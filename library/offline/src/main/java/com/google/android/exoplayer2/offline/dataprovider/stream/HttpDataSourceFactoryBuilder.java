package com.google.android.exoplayer2.offline.dataprovider.stream;

import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import java.util.ArrayList;

/**
 * Created by sharish on 20/09/17.
 */

public class HttpDataSourceFactoryBuilder {

    private String mUserAgent;
    private DefaultBandwidthMeter mBandwidthMeter;
    private ArrayList<String[]> mRequestProperties = new ArrayList<>();


    public HttpDataSourceFactoryBuilder(String userAgent) {
        this.mUserAgent = userAgent;
    }

    public HttpDataSourceFactoryBuilder addRequestProperties(String key, String value) {
        mRequestProperties.add(new String[]{key, value});
        return this;
    }

    public HttpDataSourceFactoryBuilder bandwidthMeter(DefaultBandwidthMeter defaultBandwidthMeter) {
        mBandwidthMeter = defaultBandwidthMeter;
        return this;
    }


    public DefaultHttpDataSourceFactory build() {


        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory(mUserAgent, mBandwidthMeter);

        if (mRequestProperties.size() > 0) {
            HttpDataSource.RequestProperties defaultRequestProperties = factory.getDefaultRequestProperties();
            for (String[] requestPropertyPair : mRequestProperties) {
                defaultRequestProperties.set(requestPropertyPair[0], requestPropertyPair[1]);
            }
        }

        return factory;
    }
}
