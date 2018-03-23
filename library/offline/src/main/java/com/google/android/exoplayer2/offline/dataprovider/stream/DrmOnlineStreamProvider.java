package com.google.android.exoplayer2.offline.dataprovider.stream;

import android.os.Handler;

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

/**
 * Created by sharish on 22/01/18.
 */

public class DrmOnlineStreamProvider implements IVideoStreamDataSourceProvider {

    private Handler mMainHandler;
    private MediaDrmCallback mMediaDrmCallback;
    private DefaultDrmSessionManager.EventListener mEventListener;
    private HttpDataSourceFactoryBuilder mFactoryBuilder;


    public DrmOnlineStreamProvider(MediaDrmCallback mMediaDrmCallback, HttpDataSourceFactoryBuilder factoryBuilder) {
        this.mMediaDrmCallback = mMediaDrmCallback;
        this.mFactoryBuilder = factoryBuilder;
    }

    public DrmOnlineStreamProvider(MediaDrmCallback mMediaDrmCallback, Handler mMainHandler, DefaultDrmSessionManager.EventListener mEventListener, HttpDataSourceFactoryBuilder factoryBuilder) {
        this.mMainHandler = mMainHandler;
        this.mMediaDrmCallback = mMediaDrmCallback;
        this.mEventListener = mEventListener;
        this.mFactoryBuilder = factoryBuilder;

    }

    @Override
    public DataSource createDataSource() {
        return mFactoryBuilder.build().createDataSource();
    }

    @Override
    public DefaultDrmSessionManager<FrameworkMediaCrypto> buildSessionManager(byte[] keyId) throws UnsupportedDrmException {
        return DefaultDrmSessionManager.newWidevineInstance(mMediaDrmCallback, null, mMainHandler, mEventListener);
    }
}
