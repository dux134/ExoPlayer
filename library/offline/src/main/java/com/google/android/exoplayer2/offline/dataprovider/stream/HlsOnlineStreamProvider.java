package com.google.android.exoplayer2.offline.dataprovider.stream;

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

/**
 * Created by sharish on 22/01/18.
 */

public class HlsOnlineStreamProvider implements IVideoStreamDataSourceProvider {

    private DefaultBandwidthMeter mBandwidthMeter;

    public HlsOnlineStreamProvider(DefaultBandwidthMeter mBandwidthMeter) {
        this.mBandwidthMeter = mBandwidthMeter;
    }

    @Override
    public DataSource createDataSource() {
        return new DefaultHttpDataSourceFactory("ExoPlayer", mBandwidthMeter).createDataSource();
    }

    @Override
    public DefaultDrmSessionManager<FrameworkMediaCrypto> buildSessionManager(byte[] keyId) throws UnsupportedDrmException {
        return null;
    }
}
