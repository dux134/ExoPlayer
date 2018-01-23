package com.google.android.exoplayer2.demo.offline2;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.demo.offline.OfflineUtil;
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

    public OfflineStreamProvider(String mId, Uri mManifestUri, File mBaseFolder, IVideoStreamDataSourceProvider streamProvider) {
        this.mId = mId;
        this.mBaseFolder = mBaseFolder;
        this.mManifestUri = mManifestUri;
        this.mBaseStreamProvider = streamProvider;

    }

    @Override
    public DataSource createDataSource() {

        if (!mBaseFolder.exists()) {
            mBaseFolder.mkdir();
        }

        if (OfflineUtil.hasCache(mBaseFolder, mId)) {

            Log.d("Offline", "Playing from cache");
            return OfflineUtil.loadCache(mBaseFolder, mId);
        } else {
            try {
                Log.d("Offline", "Creating cache");

                return OfflineUtil.downloadAndLoad(mBaseFolder, mId, mManifestUri);
            } catch (Exception e) {
                e.printStackTrace();

                Log.d("Offline", "Creating cache failed. Streaming directly");
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
