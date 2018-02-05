package com.google.android.exoplayer2.demo.offline2;

import android.os.Handler;

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

/**
 * Created by sharish on 22/01/18.
 */

public class OnlineStreamProvider implements IVideoStreamDataSourceProvider {

    private Handler mMainHandler;
    private MediaDrmCallback mMediaDrmCallback;
    private DefaultDrmSessionManager.EventListener mEventListener;

    public OnlineStreamProvider(MediaDrmCallback mMediaDrmCallback) {
        this.mMediaDrmCallback = mMediaDrmCallback;
    }

    public OnlineStreamProvider(MediaDrmCallback mMediaDrmCallback, Handler mMainHandler, DefaultDrmSessionManager.EventListener mEventListener) {
        this.mMainHandler = mMainHandler;
        this.mMediaDrmCallback = mMediaDrmCallback;
        this.mEventListener = mEventListener;
    }

    @Override
    public DataSource createDataSource() {
        return new DefaultHttpDataSourceFactory("ExoPlayer", null).createDataSource();
    }

    @Override
    public DefaultDrmSessionManager<FrameworkMediaCrypto> buildSessionManager(byte[] keyId) throws UnsupportedDrmException {
        return DefaultDrmSessionManager.newWidevineInstance(mMediaDrmCallback, null, mMainHandler, mEventListener);
    }
}
