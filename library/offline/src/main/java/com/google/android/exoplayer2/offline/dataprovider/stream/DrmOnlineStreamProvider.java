package com.google.android.exoplayer2.offline.dataprovider.stream;

import android.os.Handler;

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

/**
 * Created by sharish on 22/01/18.
 */

public class DrmOnlineStreamProvider implements IVideoStreamDataSourceProvider {

    private Handler mMainHandler;
    private MediaDrmCallback mMediaDrmCallback;
    private DefaultDrmSessionManager.EventListener mEventListener;
    private DefaultBandwidthMeter mBandwidthMeter;


    public DrmOnlineStreamProvider(MediaDrmCallback mMediaDrmCallback, DefaultBandwidthMeter bandwidthMeter) {
        this.mMediaDrmCallback = mMediaDrmCallback;
        this.mBandwidthMeter = bandwidthMeter;
    }

    public DrmOnlineStreamProvider(MediaDrmCallback mMediaDrmCallback, Handler mMainHandler, DefaultDrmSessionManager.EventListener mEventListener, DefaultBandwidthMeter bandwidthMeter) {
        this.mMainHandler = mMainHandler;
        this.mMediaDrmCallback = mMediaDrmCallback;
        this.mEventListener = mEventListener;
        this.mBandwidthMeter = bandwidthMeter;

    }

    @Override
    public DataSource createDataSource() {
        return new DefaultHttpDataSourceFactory("ExoPlayer", mBandwidthMeter).createDataSource();
    }

    @Override
    public DefaultDrmSessionManager<FrameworkMediaCrypto> buildSessionManager(byte[] keyId) throws UnsupportedDrmException {
        return DefaultDrmSessionManager.newWidevineInstance(mMediaDrmCallback, null, mMainHandler, mEventListener);
    }
}
