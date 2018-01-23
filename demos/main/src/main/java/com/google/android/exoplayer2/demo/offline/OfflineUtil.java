package com.google.android.exoplayer2.demo.offline;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.offline.Downloader;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.manifest.AdaptationSet;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.dash.manifest.Period;
import com.google.android.exoplayer2.source.dash.manifest.Representation;
import com.google.android.exoplayer2.source.dash.manifest.RepresentationKey;
import com.google.android.exoplayer2.source.dash.offline.DashDownloader;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by sharish on 22/01/18.
 */

public class OfflineUtil {


    private static RepresentationKey[] getRepresentationKeys(DashManifest dashManifest, int pixelHeight) {

        int periodCount = dashManifest.getPeriodCount();

        ArrayList<RepresentationKey> keys = new ArrayList<>();

        for (int i = 0; i < periodCount; i++) {
            Period period = dashManifest.getPeriod(i);
            int size = period.adaptationSets.size();

            for (int j = 0; j < size; j++) {
                AdaptationSet adaptationSet = period.adaptationSets.get(j);
                int adpSize = adaptationSet.representations.size();

                for (int k = 0; k < adpSize; k++) {
                    Representation representation = adaptationSet.representations.get(k);

                    int repHeight = representation.format.height;
                    Log.d("Offline", String.format(Locale.getDefault(), "Period: %d, Adp : %d, Rep : %d, Format:%dp", i, j, k, repHeight));

                    if(repHeight == pixelHeight || repHeight == -1) {
                        keys.add(new RepresentationKey(i, j, k));
                    }
                }
            }
        }

        return keys.toArray(new RepresentationKey[keys.size()]);

    }

    public static void download(File downloadFolder, final String id, Uri manifestUrl) throws IOException, InterruptedException {

        if (!downloadFolder.exists()) {
            downloadFolder.mkdir();
        }

        File manifestFolder = new File(downloadFolder, id);

        if (!manifestFolder.exists()) {
            manifestFolder.mkdir();
        }


        SimpleCache cache = new SimpleCache(manifestFolder, new NoOpCacheEvictor());
        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("ExoPlayer", null);
        DownloaderConstructorHelper constructorHelper =
                new DownloaderConstructorHelper(cache, factory);

        DashManifest dashManifest = DashUtil.loadManifest(factory.createDataSource(), manifestUrl);
        DashDownloader dashDownloader = new DashDownloader(manifestUrl, constructorHelper);

        // Select the first representation of the first adaptation set of the first period
        dashDownloader.selectRepresentations(getRepresentationKeys(dashManifest, 1080));

        dashDownloader.download(new Downloader.ProgressListener() {
            @Override
            public void onDownloadProgress(Downloader downloader, float downloadPercentage,
                                           long downloadedBytes) {

                Log.d("OfflineUtil", id + ":" + downloadPercentage + ", " + downloadedBytes / 1024 + "KBs");
            }
        });
    }

    public static boolean hasCache(File downloadFolder, String id) {

        File manifestFolder = new File(downloadFolder, id);

        if (manifestFolder.exists()) {
            return manifestFolder.list().length > 10;
        }

        return false;

    }

    public static void storeOfflineKeyId(File mainFolder, String id, byte[] keyId) {

        FileOutputStream fos = null;

        if( ! mainFolder.exists()) {
            mainFolder.mkdir();
        }
        try {
            fos = new FileOutputStream(new File(mainFolder, id));
            fos.write(keyId, 0, keyId.length);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    public static byte[] readOfflineKeyId(File mainFolder, String id) {

        FileInputStream fis = null;
        if( ! mainFolder.exists()) {
            mainFolder.mkdir();
        }

        File file = new File(mainFolder, id);

        if( ! file.exists()) return null;
        try {
            fis = new FileInputStream(file);
            int length = (int) file.length();
            byte[] buffer = new byte[length];
            fis.read(buffer, 0, length);
            fis.close();

            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return null;
    }



    public static CacheDataSource loadCache(File downloadFolder, String id) {


        File manifestFolder = new File(downloadFolder, id);

        SimpleCache cache = new SimpleCache(manifestFolder, new NoOpCacheEvictor());
        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("ExoPlayer", null);
        return new CacheDataSource(cache, factory.createDataSource(), CacheDataSource.FLAG_BLOCK_ON_CACHE);
    }

    public static DataSource streamDirectly() {
        return new DefaultHttpDataSourceFactory("ExoPlayer", null).createDataSource();

    }


    public static DataSource downloadAndLoad(File downloadFolder, String id, Uri manifestUrl) throws IOException, InterruptedException {

        if (!hasCache(downloadFolder, id)) {
            download(downloadFolder, id, manifestUrl);
        }

        return loadCache(downloadFolder, id);
    }

    public static boolean isCacheNeeded(String id) {
        return id != null && id.startsWith("C_");
    }
}
