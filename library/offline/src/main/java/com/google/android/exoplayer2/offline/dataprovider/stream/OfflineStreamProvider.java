package com.google.android.exoplayer2.offline.dataprovider.stream;

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.offline.OfflineUtil;
import com.google.android.exoplayer2.offline.dataprovider.source.IDataSourceProvider;
import com.google.android.exoplayer2.upstream.DataSource;

import java.io.File;

/**
 * Created by sharish on 22/01/18.
 */

public class OfflineStreamProvider implements IVideoStreamDataSourceProvider {

    private IVideoStreamDataSourceProvider mBaseStreamProvider;

    private String mId;
    private File mBaseFolder;
    private String mManifestUrl;
    private String mEncryptionKey;
    private int mVideoHeight = OfflineUtil.VIDEO_HEIGHT_WILDCARD;
    private IDataSourceProvider mDataSourceProvider;

    public OfflineStreamProvider(String mId, String mManifestUrl, File mBaseFolder, IVideoStreamDataSourceProvider streamProvider, IDataSourceProvider dataSourceProvider) {
        this(mId, mManifestUrl, mBaseFolder, null, OfflineUtil.VIDEO_HEIGHT_WILDCARD, streamProvider, dataSourceProvider);
    }

    public OfflineStreamProvider(String mId, String mManifestUrl, File mBaseFolder, String encryptionKey, int videoHeight, IVideoStreamDataSourceProvider streamProvider, IDataSourceProvider dataSourceProvider) {
        this.mId = mId;
        this.mBaseFolder = mBaseFolder;
        this.mManifestUrl = mManifestUrl;
        this.mBaseStreamProvider = streamProvider;
        mEncryptionKey = encryptionKey;
        mVideoHeight = videoHeight;
        mDataSourceProvider = dataSourceProvider;
    }

    @Override
    public DataSource createDataSource() {

        if (!mBaseFolder.exists()) {
            mBaseFolder.mkdir();
        }

        if (OfflineUtil.isCacheAvailable(mBaseFolder, mId, mEncryptionKey)) {
            return OfflineUtil.loadCache(mBaseFolder, mId, mEncryptionKey);
        } else {
            try {
                return mDataSourceProvider.downloadAndLoad(mBaseFolder, mId, mManifestUrl, mEncryptionKey, mVideoHeight);
            } catch (Exception e) {
                e.printStackTrace();
                return mBaseStreamProvider.createDataSource();

            }
        }
    }

    @Override
    public DefaultDrmSessionManager<FrameworkMediaCrypto> buildSessionManager(byte[] licenseKey) throws UnsupportedDrmException {

        DefaultDrmSessionManager<FrameworkMediaCrypto> sessionManager = mBaseStreamProvider.buildSessionManager(licenseKey);

        if (licenseKey != null) {
            sessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, licenseKey);
        }

        return sessionManager;
    }
}
