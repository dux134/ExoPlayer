package com.google.android.exoplayer2.offline.dataprovider.source;

import com.google.android.exoplayer2.offline.HlsOfflineUtil;
import com.google.android.exoplayer2.offline.dataprovider.stream.HttpDataSourceFactoryBuilder;
import com.google.android.exoplayer2.upstream.DataSource;

import java.io.File;

import static com.google.android.exoplayer2.offline.OfflineUtil.isCacheAvailable;
import static com.google.android.exoplayer2.offline.OfflineUtil.loadCache;

/**
 * Created by sharish on 07/02/18.
 */

public class HlsDataSourceProvider implements IDataSourceProvider {

    @Override
    public DataSource downloadAndLoad(File baseFolder, String id, String manifestUrl, String key, HttpDataSourceFactoryBuilder factoryBuilder, int targetVideoPixelHeight) throws Exception {

        if (!isCacheAvailable(baseFolder, id, key)) {
            HlsOfflineUtil.downloadSync(baseFolder, id, manifestUrl, key, targetVideoPixelHeight, factoryBuilder, null);
        }

        return loadCache(baseFolder, id, key, factoryBuilder);

    }
}
