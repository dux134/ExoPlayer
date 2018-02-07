package com.google.android.exoplayer2.offline;

import android.support.annotation.Nullable;

import com.google.android.exoplayer2.offline.models.CacheInfo;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Created by sharish on 22/01/18.
 */

public class OfflineUtil {

    public static final int VIDEO_HEIGHT_WILDCARD = -1;

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

}
