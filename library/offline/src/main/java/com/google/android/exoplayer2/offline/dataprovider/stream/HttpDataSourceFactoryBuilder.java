package com.google.android.exoplayer2.offline.dataprovider.stream;

import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.util.ArrayList;

import static com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS;
import static com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS;

/**
 * Created by sharish on 20/09/17.
 */

public class HttpDataSourceFactoryBuilder {

    private String mUserAgent;
    private TransferListener mTransferListener;
    private ArrayList<String[]> mRequestProperties = new ArrayList<>();
    private int mConnectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
    private int mReadTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;


    public HttpDataSourceFactoryBuilder(String userAgent) {
        this.mUserAgent = userAgent;
    }

    public HttpDataSourceFactoryBuilder addRequestProperties(String key, String value) {
        mRequestProperties.add(new String[]{key, value});
        return this;
    }

    public HttpDataSourceFactoryBuilder transferListener(TransferListener transferListener) {
        mTransferListener = transferListener;
        return this;
    }

    public HttpDataSourceFactoryBuilder connectTimeoutMillis(int timeoutMillis) {
        mConnectTimeoutMillis = timeoutMillis;
        return this;
    }

    public HttpDataSourceFactoryBuilder readTimeoutMillis(int timeoutMillis) {
        mReadTimeoutMillis = timeoutMillis;
        return this;
    }


    public DefaultHttpDataSourceFactory build() {


        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory(mUserAgent,
                mTransferListener, mConnectTimeoutMillis,
                mReadTimeoutMillis, false);

        if (mRequestProperties.size() > 0) {
            HttpDataSource.RequestProperties defaultRequestProperties = factory.getDefaultRequestProperties();
            for (String[] requestPropertyPair : mRequestProperties) {
                defaultRequestProperties.set(requestPropertyPair[0], requestPropertyPair[1]);
            }
        }

        return factory;
    }
}
