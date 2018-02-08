package com.google.android.exoplayer2.offline;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.offline.dataprovider.stream.HttpDataSourceFactoryBuilder;
import com.google.android.exoplayer2.offline.models.CacheInfo;
import com.google.android.exoplayer2.offline.models.DownloadInfo;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.manifest.AdaptationSet;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.dash.manifest.Period;
import com.google.android.exoplayer2.source.dash.manifest.Representation;
import com.google.android.exoplayer2.source.dash.manifest.RepresentationKey;
import com.google.android.exoplayer2.source.dash.offline.DashDownloader;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

import static com.google.android.exoplayer2.offline.OfflineUtil.get16ByteSecretKey;
import static com.google.android.exoplayer2.offline.OfflineUtil.storeCacheInfo;

/**
 * Created by sharish on 22/01/18.
 */

public class DashOfflineUtil {


    /**
     * Downloads the video in asynchronous way.
     *
     * @param downloadFolder         - target directory where the video should be downloaded
     * @param id                     - Unique video id, based on which sub directory will be identified.
     * @param manifestUrl            - target video manifest url
     * @param key                    - Encryption key if any.
     * @param targetVideoPixelHeight - Target video height.
     * @return - Rx Flowable reference for async downloading.
     */
    public static Flowable<DownloadInfo> downloadAsync(final File downloadFolder, final String id, final String manifestUrl, final String key, final int targetVideoPixelHeight, final HttpDataSourceFactoryBuilder factoryBuilder) {

        return Flowable.create(new FlowableOnSubscribe<DownloadInfo>() {
            @Override
            public void subscribe(final FlowableEmitter<DownloadInfo> e) throws Exception {

                try {
                    downloadSync(downloadFolder, id, manifestUrl, key, targetVideoPixelHeight, factoryBuilder, new Downloader.ProgressListener() {
                        @Override
                        public void onDownloadProgress(Downloader downloader, float downloadPercentage, long downloadedBytes) {

                            if (e.isCancelled()) return;

                            DownloadInfo downloadInfo = new DownloadInfo(downloadPercentage, downloadedBytes);
                            e.onNext(downloadInfo);

                            if (downloadPercentage == 100) {
                                e.onComplete();
                            }
                        }
                    });
                } catch (InterruptedException e1) {
                    if (e.isCancelled()) return;
                    throw e1;
                }


            }

        }, BackpressureStrategy.BUFFER);
    }


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

                    if (repHeight == pixelHeight || repHeight == -1) {
                        keys.add(new RepresentationKey(i, j, k));
                    }
                }
            }
        }

        return keys.toArray(new RepresentationKey[keys.size()]);

    }

    public static void downloadSync(final File baseFolder, final String id, final String manifestUrl, final String key, int targetVideoPixelHeight, HttpDataSourceFactoryBuilder factoryBuilder, final Downloader.ProgressListener listener) throws Exception {

        if (!baseFolder.exists()) {
            baseFolder.mkdir();
        }

        byte[] secretKey = get16ByteSecretKey(key);


        File manifestFolder = new File(baseFolder, id);

        if (!manifestFolder.exists()) {
            manifestFolder.mkdir();
        }

        Uri uri = Uri.parse(manifestUrl);

        SimpleCache cache = new SimpleCache(manifestFolder, new NoOpCacheEvictor(), secretKey);
        DefaultHttpDataSourceFactory factory = factoryBuilder.build();
        DownloaderConstructorHelper constructorHelper =
                new DownloaderConstructorHelper(cache, factory);

        DashDownloader dashDownloader = new DashDownloader(uri, constructorHelper);

        DashManifest dashManifest = DashUtil.loadManifest(factory.createDataSource(), uri);

        // Select the first representation of the first adaptation set of the first period
        dashDownloader.selectRepresentations(DashOfflineUtil.getRepresentationKeys(dashManifest, targetVideoPixelHeight));

        CacheInfo cacheInfoNonfinal = OfflineUtil.getCacheInfo(baseFolder, id, key);

        if (cacheInfoNonfinal == null) {
            cacheInfoNonfinal = new CacheInfo(id);
        }

        final CacheInfo cacheInfo = cacheInfoNonfinal;

        dashDownloader.download(new Downloader.ProgressListener() {
            @Override
            public void onDownloadProgress(Downloader downloader, float downloadPercentage, long downloadedBytes) {

                cacheInfo.setDownloadBytes(downloadedBytes);
                cacheInfo.setDownloadPercent(downloadPercentage);

                storeCacheInfo(baseFolder, id, cacheInfo, key);

                if (listener != null) {
                    listener.onDownloadProgress(downloader, downloadPercentage, downloadedBytes);
                }
            }
        });
    }

}
