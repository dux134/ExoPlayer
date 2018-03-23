package com.google.android.exoplayer2.offline.dataprovider.cache;

import com.google.android.exoplayer2.offline.models.CacheInfo;

/**
 * Created by Harish on 21/03/18.
 */

public interface ICacheInfoProvider {

    CacheInfo getCacheInfo(String id);

    boolean isCacheAvailable(String id);

    void storeCacheInfo(String id, CacheInfo cacheInfo);
}
