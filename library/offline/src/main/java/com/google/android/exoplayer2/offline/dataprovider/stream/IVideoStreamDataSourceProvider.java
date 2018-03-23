package com.google.android.exoplayer2.offline.dataprovider.stream;

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.DataSource;

/**
 * Created by sharish on 22/01/18.
 */

public interface IVideoStreamDataSourceProvider extends DataSource.Factory {

    DefaultDrmSessionManager<FrameworkMediaCrypto> buildSessionManager(byte[] licenseKey) throws UnsupportedDrmException;
}
