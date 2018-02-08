package com.google.android.exoplayer2.offline.dataprovider.source;

import com.google.android.exoplayer2.offline.dataprovider.stream.HttpDataSourceFactoryBuilder;
import com.google.android.exoplayer2.upstream.DataSource;

import java.io.File;

/**
 * Created by sharish on 07/02/18.
 */

public interface IDataSourceProvider {

    /**
     * Provides Offline Video Data Source. If not offline exists, makes a synchronous cache download and provide it.
     *
     * @param baseFolder         - target directory where the video should be downloaded
     * @param id                     - Unique video id, based on which sub directory will be identified.
     * @param manifestUrl            - target video manifest url
     * @param key                    - Encryption key if any.
     * @param targetVideoPixelHeight - Target video height.
     * @param factoryBuilder - Main source builder.
     * @return - Video stream as data source.
     */
    DataSource downloadAndLoad(File baseFolder, String id, String manifestUrl, String key, HttpDataSourceFactoryBuilder factoryBuilder, int targetVideoPixelHeight) throws Exception;
}
