/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.offline.DashOfflineUtil;
import com.google.android.exoplayer2.offline.OfflineUtil;
import com.google.android.exoplayer2.offline.dataprovider.license.OfflineLicenseProvider;
import com.google.android.exoplayer2.offline.dataprovider.license.OnlineLicenseProvider;
import com.google.android.exoplayer2.offline.dataprovider.stream.HttpDataSourceFactoryBuilder;
import com.google.android.exoplayer2.offline.models.CacheInfo;
import com.google.android.exoplayer2.offline.models.DownloadInfo;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * An activity for selecting from a list of samples.
 */
public class SampleChooserActivity2 extends Activity {

    private static final String TAG = "SampleChooserActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_chooser_activity);
        Intent intent = getIntent();
        String dataUri = intent.getDataString();
        String[] uris;
        if (dataUri != null) {
            uris = new String[]{dataUri};
        } else {
            ArrayList<String> uriList = new ArrayList<>();
            AssetManager assetManager = getAssets();
            try {
                for (String asset : assetManager.list("")) {
                    if (asset.endsWith(".exolist.json")) {
                        uriList.add("asset:///" + asset);
                    }
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
                        .show();
            }
            uris = new String[uriList.size()];
            uriList.toArray(uris);
            Arrays.sort(uris);
        }
        SampleListLoader loaderTask = new SampleListLoader();
        loaderTask.execute(uris);
    }

    private void onSampleGroups(final List<SampleGroup> groups, boolean sawError) {
        if (sawError) {
            Toast.makeText(getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
                    .show();
        }
        ExpandableListView sampleList = findViewById(R.id.sample_list);
        sampleList.setAdapter(new SampleAdapter(this, groups));
        sampleList.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
                                        int childPosition, long id) {
                onSampleSelected(groups.get(groupPosition).samples.get(childPosition));
                return true;
            }
        });
    }

    private void onSampleSelected(Sample sample) {
        startActivity(sample.buildIntent(this));
    }

    private final class SampleListLoader extends AsyncTask<String, Void, List<SampleGroup>> {

        private boolean sawError;

        @Override
        protected List<SampleGroup> doInBackground(String... uris) {
            List<SampleGroup> result = new ArrayList<>();
            Context context = getApplicationContext();
            String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
            DataSource dataSource = new DefaultDataSource(context, null, userAgent, false);
            for (String uri : uris) {
                DataSpec dataSpec = new DataSpec(Uri.parse(uri));
                InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
                try {
                    readSampleGroups(new JsonReader(new InputStreamReader(inputStream, "UTF-8")), result);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading sample list: " + uri, e);
                    sawError = true;
                } finally {
                    Util.closeQuietly(dataSource);
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<SampleGroup> result) {
            onSampleGroups(result, sawError);
        }

        private void readSampleGroups(JsonReader reader, List<SampleGroup> groups) throws IOException {
            reader.beginArray();
            while (reader.hasNext()) {
                readSampleGroup(reader, groups);
            }
            reader.endArray();
        }

        private void readSampleGroup(JsonReader reader, List<SampleGroup> groups) throws IOException {
            String groupName = "";
            ArrayList<Sample> samples = new ArrayList<>();

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        groupName = reader.nextString();
                        break;
                    case "samples":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            samples.add(readEntry(reader));
                        }
                        reader.endArray();
                        break;
                    case "_comment":
                        reader.nextString(); // Ignore.
                        break;
                    default:
                        throw new ParserException("Unsupported name: " + name);
                }
            }
            reader.endObject();

            SampleGroup group = getGroup(groupName, groups);
            group.samples.addAll(samples);
        }

        private Sample readEntry(JsonReader reader) throws IOException {
            String sampleName = null;
            String uri = null;
            String extension = null;
            String drmLicenseUrl = null;
            String[] drmKeyRequestProperties = null;

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        sampleName = reader.nextString();
                        break;
                    case "uri":
                        uri = reader.nextString();
                        break;
                    case "extension":
                        extension = reader.nextString();
                        break;
                    case "drm_scheme":
                        String scheme = reader.nextString();

                        if (!"widevine".equals(scheme)) {
                            throw new ParserException("Only Widevine scheme supported: " + scheme);

                        }
                        break;
                    case "drm_license_url":
                        drmLicenseUrl = reader.nextString();
                        break;
                    case "drm_key_request_properties":
                        ArrayList<String> drmKeyRequestPropertiesList = new ArrayList<>();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            drmKeyRequestPropertiesList.add(reader.nextName());
                            drmKeyRequestPropertiesList.add(reader.nextString());
                        }
                        reader.endObject();
                        drmKeyRequestProperties = drmKeyRequestPropertiesList.toArray(new String[0]);
                        break;
                    default:
                        throw new ParserException("Unsupported attribute name: " + name);
                }
            }
            reader.endObject();
            DrmInfo drmInfo = new DrmInfo(drmLicenseUrl,
                    drmKeyRequestProperties);


            return new UriSample(sampleName, drmInfo, uri, extension);
        }

        private SampleGroup getGroup(String groupName, List<SampleGroup> groups) {
            for (int i = 0; i < groups.size(); i++) {
                if (Util.areEqual(groupName, groups.get(i).title)) {
                    return groups.get(i);
                }
            }
            SampleGroup group = new SampleGroup(groupName);
            groups.add(group);
            return group;
        }

    }

    private static final class SampleAdapter extends BaseExpandableListAdapter {

        private final Context context;
        private final List<SampleGroup> sampleGroups;

        public SampleAdapter(Context context, List<SampleGroup> sampleGroups) {
            this.context = context;
            this.sampleGroups = sampleGroups;
        }

        @Override
        public Sample getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).samples.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.inner_list_sample, parent, false);
            }

//            final String key = "123";
            final String key = null;


            UriSample child = (UriSample) getChild(groupPosition, childPosition);

            final String name = child.name;
            String lice = child.drmInfo.drmLicenseUrl;
            final String[] req = child.drmInfo.drmKeyRequestProperties;
            final String uriString = child.uri;
            final Uri playUri = Uri.parse(uriString);

            File externalCacheDir = view.getContext().getExternalCacheDir();
            final File baseFolder = new File(externalCacheDir, "offline_samples");

            TextView textView = view.findViewById(android.R.id.text1);
            final ProgressBar progressBar = view.findViewById(R.id.progress);

            DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("ExoPlayer", null);

            MediaDrmCallback drmCallback = DemoUtil.getMediaDrmCallback(lice, factory, req);


            OnlineLicenseProvider onlineLicenseProvider = new OnlineLicenseProvider(factory, drmCallback, playUri);
            final OfflineLicenseProvider offlineLicenseProvider = new OfflineLicenseProvider(name, baseFolder, onlineLicenseProvider, key);

            textView.setText(child.name);
            progressBar.setProgress(0);

            CacheInfo cacheInfo = OfflineUtil.getCacheInfo(baseFolder, name, key);

            float cachePercent = cacheInfo != null ? cacheInfo.getDownloadPercent() : 0;

            if (cachePercent >= 100) {
                progressBar.setProgress((int) cachePercent);
            } else {
                progressBar.setProgress((int) cachePercent);

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(final View view) {

                        Flowable<byte[]> licenseFlowable = offlineLicenseProvider.loadLicense2();

                        HttpDataSourceFactoryBuilder factoryBuilder = new HttpDataSourceFactoryBuilder("Exo");

                        if(req != null) {

                            for (int i = 0; i < req.length; i+=2) {
                                factoryBuilder.addRequestProperties(req[i], req[i+1]);
                            }

                        }

                        final Flowable<DownloadInfo> contentFlowable = DashOfflineUtil.downloadAsync(baseFolder, name, uriString, key, 180, factoryBuilder);

                        Flowable<DownloadInfo> finalFlowable = licenseFlowable.flatMap(new Function<byte[], Flowable<DownloadInfo>>() {
                            @Override
                            public Flowable<DownloadInfo> apply(byte[] bytes) throws Exception {
                                return contentFlowable;
                            }
                        })
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread());


                        finalFlowable.subscribe(new Consumer<DownloadInfo>() {
                            @Override
                            public void accept(DownloadInfo downloadInfo) throws Exception {
                                int downloadPercent = (int) downloadInfo.downloadPercent;
                                progressBar.setProgress(downloadPercent);

                                if (downloadPercent >= 100) {
                                    view.setOnLongClickListener(null);
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {

                            }
                        });


                        return false;
                    }
                });

            }
            return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return getGroup(groupPosition).samples.size();
        }

        @Override
        public SampleGroup getGroup(int groupPosition) {
            return sampleGroups.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                 ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1,
                        parent, false);
            }
            ((TextView) view).setText(getGroup(groupPosition).title);
            return view;
        }

        @Override
        public int getGroupCount() {
            return sampleGroups.size();
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

    private static final class SampleGroup {

        public final String title;
        public final List<Sample> samples;

        public SampleGroup(String title) {
            this.title = title;
            this.samples = new ArrayList<>();
        }

    }

    private static final class DrmInfo {
        public final String drmLicenseUrl;
        public final String[] drmKeyRequestProperties;

        public DrmInfo(String drmLicenseUrl,
                       String[] drmKeyRequestProperties) {
            this.drmLicenseUrl = drmLicenseUrl;
            this.drmKeyRequestProperties = drmKeyRequestProperties;
        }

        public void updateIntent(Intent intent) {
            Assertions.checkNotNull(intent);
            intent.putExtra(PlayerActivity2.DRM_LICENSE_URL, drmLicenseUrl);
            intent.putExtra(PlayerActivity2.DRM_KEY_REQUEST_PROPERTIES, drmKeyRequestProperties);
        }
    }

    private abstract static class Sample {
        public final String name;
        public final DrmInfo drmInfo;

        public Sample(String name, DrmInfo drmInfo) {
            this.name = name;
            this.drmInfo = drmInfo;
        }

        public Intent buildIntent(Context context) {
            Intent intent = new Intent(context, PlayerActivity2.class);
            intent.putExtra(PlayerActivity2.VIDEO_NAME, name);

            if (drmInfo != null) {
                drmInfo.updateIntent(intent);
            }

            return intent;
        }

    }

    private static final class UriSample extends Sample {

        public final String uri;
        public final String extension;

        public UriSample(String name, DrmInfo drmInfo, String uri,
                         String extension) {
            super(name, drmInfo);
            this.uri = uri;
            this.extension = extension;
        }

        @Override
        public Intent buildIntent(Context context) {
            return super.buildIntent(context)
                    .setData(Uri.parse(uri))
                    .setAction(PlayerActivity2.ACTION_VIEW);
        }

    }
}
