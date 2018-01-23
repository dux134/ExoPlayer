package com.google.android.exoplayer2.demo.offline;

import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;

/**
 * Created by sharish on 22/01/18.
 */

public class DrmUtil {


    public static HttpDataSource.Factory buildHttpDataSourceFactory(String userAgent, DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }

    public static MediaDrmCallback getMediaDrmCallback(String licenseUrl, HttpDataSource.Factory httpFactory, String token) {

        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl, httpFactory);
        drmCallback.setKeyRequestProperty("Authorization", "Bearer " + token);
        return drmCallback;
    }

    public static MediaDrmCallback getMediaDrmCallback(String licenseUrl, HttpDataSource.Factory httpFactory, String[] keyRequestPropertiesArray) {

        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl, httpFactory);
        if (keyRequestPropertiesArray != null) {
            for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                        keyRequestPropertiesArray[i + 1]);
            }
        }
        return drmCallback;
    }
}
