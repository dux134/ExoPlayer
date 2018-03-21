package com.google.android.exoplayer2.offline.dataprovider.cache;

import com.google.android.exoplayer2.offline.OfflineUtil;
import com.google.android.exoplayer2.offline.models.CacheInfo;

import java.io.File;

/**
 * Created by Harish on 21/03/18.
 */

public class FileCacheInfoProvider implements ICacheInfoProvider {

    private File mBaseDirectory;
    private String mEncryptionKey;

    public FileCacheInfoProvider(File mBaseDirectory) {
        this.mBaseDirectory = mBaseDirectory;
    }

    public FileCacheInfoProvider(File mBaseDirectory, String mEncryptionKey) {
        this.mBaseDirectory = mBaseDirectory;
        this.mEncryptionKey = mEncryptionKey;
    }

    @Override
    public CacheInfo getCacheInfo(String id) {
        return OfflineUtil.getCacheInfo(mBaseDirectory, id, mEncryptionKey);
    }

    @Override
    public boolean isCacheAvailable(String id) {
        return OfflineUtil.isCacheAvailable(mBaseDirectory, id, mEncryptionKey);
    }

    @Override
    public void storeCacheInfo(String id, CacheInfo cacheInfo) {
        OfflineUtil.storeCacheInfo(mBaseDirectory, id, cacheInfo, mEncryptionKey);
    }
}
