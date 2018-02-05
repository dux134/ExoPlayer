package com.google.android.exoplayer2.offline.dataprovider.stream;

import android.net.Uri;

import com.google.android.exoplayer2.offline.OfflineUtil;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.DataSource;

import java.io.File;

/**
 * Created by sharish on 22/01/18.
 */

public class OfflineStreamProvider implements IVideoStreamDataSourceProvider {

    private IVideoStreamDataSourceProvider mBaseStreamProvider;

    private String mId;
    private File mBaseFolder;
    private Uri mManifestUri;
    private String mEncryptionKey;
    private int mVideoHeight = OfflineUtil.VIDEO_HEIGHT_WILDCARD;

    public OfflineStreamProvider(String mId, Uri mManifestUri, File mBaseFolder, IVideoStreamDataSourceProvider streamProvider) {
        this(mId, mManifestUri, mBaseFolder, null, OfflineUtil.VIDEO_HEIGHT_WILDCARD, streamProvider);
    }

    public OfflineStreamProvider(String mId, Uri mManifestUri, File mBaseFolder, String encryptionKey, int videoHeight, IVideoStreamDataSourceProvider streamProvider) {
        this.mId = mId;
        this.mBaseFolder = mBaseFolder;
        this.mManifestUri = mManifestUri;
        this.mBaseStreamProvider = streamProvider;
        mEncryptionKey = encryptionKey;
        mVideoHeight = videoHeight;
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
                return OfflineUtil.downloadAndLoad(mBaseFolder, mId, mManifestUri, mEncryptionKey, mVideoHeight);
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
