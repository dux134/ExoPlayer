package com.google.android.exoplayer2.offline;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

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
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

/**
 * Created by sharish on 22/01/18.
 */

public class OfflineUtil {

    public static final int VIDEO_HEIGHT_WILDCARD = -1;

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
    public static Flowable<DownloadInfo> downloadAsync(final File downloadFolder, final String id, final Uri manifestUrl, final String key, final int targetVideoPixelHeight) {

        return Flowable.create(new FlowableOnSubscribe<DownloadInfo>() {
            @Override
            public void subscribe(final FlowableEmitter<DownloadInfo> e) throws Exception {

                downloadSync(downloadFolder, id, manifestUrl, key, targetVideoPixelHeight, new Downloader.ProgressListener() {
                    @Override
                    public void onDownloadProgress(Downloader downloader, float downloadPercentage, long downloadedBytes) {

                        if(e.isCancelled()) return;
                        
                        DownloadInfo downloadInfo = new DownloadInfo(downloadPercentage, downloadedBytes);
                        e.onNext(downloadInfo);

                        if (downloadPercentage == 100) {
                            e.onComplete();
                        }
                    }
                });


            }

        }, BackpressureStrategy.BUFFER);
    }


    /**
     * Reads the Cache info.
     * @param baseDownloadDirectory - base download directory where the info is stored.
     * @param id - Unique id
     * @param key - Encryption key if any.
     * @return - Cache info of the video if found, null otherwise.
     */
    public static CacheInfo getCacheInfo(File baseDownloadDirectory, String id, String key) {

        File contentFolder = new File(baseDownloadDirectory, "info");

        if (!contentFolder.exists()) return null;

        byte[] secretKey = get16ByteSecretKey(key);

        JSONObject object;

        if (key == null || key.length() == 0) {
            object = FileUtil.readJson(contentFolder, id);
        } else {
            object = FileUtil.readEncryptedJson(contentFolder, id, secretKey);
        }

        CacheInfo cacheInfo = new CacheInfo(id);
        cacheInfo.fromJson(object);

        return cacheInfo;

    }

    /**
     * Tells whether cache is available for the video or not.
     * @param baseDownloadDirectory - base download directory where the info is stored.
     * @param id - Unique id
     * @param key - Encryption key if any.
     * @return - true when cache download percent is greater than zero, false otherwise.
     */
    public static boolean isCacheAvailable(File baseDownloadDirectory, String id, String key) {
        CacheInfo cacheInfo = getCacheInfo(baseDownloadDirectory, id, key);
        return cacheInfo != null && cacheInfo.getDownloadPercent() > 0;
    }

    /**
     * Loads the cache data source for the given video id.
     *
     * @param baseDirectory - Directory where all the cache resides
     * @param id            - unique id of the video to pick the video
     * @param key           - decryption key.
     * @return - cache data source.
     */
    public static CacheDataSource loadCache(File baseDirectory, String id, @Nullable String key) {

        File manifestFolder = new File(baseDirectory, id);

        byte[] secretKey = get16ByteSecretKey(key);
        SimpleCache cache = new SimpleCache(manifestFolder, new NoOpCacheEvictor(), secretKey);
        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("ExoPlayer", null);
        return new CacheDataSource(cache, factory.createDataSource(), CacheDataSource.FLAG_BLOCK_ON_CACHE);
    }

    /**
     * Provides Offline Video Data Source. If not offline exists, makes a synchronous cache download and provide it.
     *
     * @param baseFolder         - target directory where the video should be downloaded
     * @param id                     - Unique video id, based on which sub directory will be identified.
     * @param manifestUrl            - target video manifest url
     * @param key                    - Encryption key if any.
     * @param targetVideoPixelHeight - Target video height.
     * @return - Video stream as data source.
     */
    public static DataSource downloadAndLoad(File baseFolder, String id, Uri manifestUrl, String key, int targetVideoPixelHeight) throws Exception {

        if (!isCacheAvailable(baseFolder, id, key)) {
            downloadSync(baseFolder, id, manifestUrl, key, targetVideoPixelHeight, null);
        }

        return loadCache(baseFolder, id, key);
    }

    /**
     * Stores the cache data source for the given video id.
     *
     * @param baseDirectory - Directory where all the cache resides
     * @param id            - unique id of the video to pick the video
     * @param key           - decryption key.
     */
    public static void storeCacheInfo(File baseDirectory, String id, CacheInfo cacheInfo, String key) {

        File contentFolder = new File(baseDirectory, "info");

        if (!contentFolder.exists()) {
            contentFolder.mkdir();
        }

        byte[] secretKey = get16ByteSecretKey(key);

        if (key == null || key.length() == 0) {
            FileUtil.writeJson(contentFolder, id, cacheInfo.toJson());
        } else {
            FileUtil.writeEncryptedJson(contentFolder, id, secretKey, cacheInfo.toJson());
        }

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

    static byte[] get16ByteSecretKey(String key) {

        if (key == null || key.length() == 0) return null;

        byte[] secretKey = new byte[16];

        try {
            byte[] actualBytes = key.getBytes("UTF-8");
            System.arraycopy(actualBytes, 0, secretKey, 0, Math.min(16, actualBytes.length));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return secretKey;
    }


    private static void downloadSync(final File baseFolder, final String id, final Uri manifestUrl, final String key, int targetVideoPixelHeight, final Downloader.ProgressListener listener) throws Exception {

        if (!baseFolder.exists()) {
            baseFolder.mkdir();
        }

        byte[] secretKey = OfflineUtil.get16ByteSecretKey(key);


        File manifestFolder = new File(baseFolder, id);

        if (!manifestFolder.exists()) {
            manifestFolder.mkdir();
        }

        SimpleCache cache = new SimpleCache(manifestFolder, new NoOpCacheEvictor(), secretKey);
        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("ExoPlayer", null);
        DownloaderConstructorHelper constructorHelper =
                new DownloaderConstructorHelper(cache, factory);

        DashDownloader dashDownloader = new DashDownloader(manifestUrl, constructorHelper);

        DashManifest dashManifest = DashUtil.loadManifest(factory.createDataSource(), manifestUrl);

        // Select the first representation of the first adaptation set of the first period
        dashDownloader.selectRepresentations(OfflineUtil.getRepresentationKeys(dashManifest, targetVideoPixelHeight));

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
