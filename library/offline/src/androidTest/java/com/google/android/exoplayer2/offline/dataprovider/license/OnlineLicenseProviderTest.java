package com.google.android.exoplayer2.offline.dataprovider.license;

import android.content.Context;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.TestScheduler;

/**
 * Created by sharish on 30/01/18.
 */

public class OnlineLicenseProviderTest extends InstrumentationTestCase {

    private static final String SAMPLE_VIDEO_MANIFEST_URL = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd";
    private static final String SAMPLE_VIDEO_LICENSE_URL = "https://proxy.uat.widevine.com/proxy?video_id=d286538032258a1c&provider=widevine_test";


    private OnlineLicenseProvider mLicenseProvider;


    @Override
    public void setUp() throws Exception {
        super.setUp();

        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("Exo", null);
        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(SAMPLE_VIDEO_LICENSE_URL, factory);

        mLicenseProvider = new OnlineLicenseProvider(factory, drmCallback, Uri.parse(SAMPLE_VIDEO_MANIFEST_URL));
    }


    /**
     * Tests whether a proper license is generated and the following are verified
     * <p>
     * 1. Key always starts with 'ksid'
     * 2. Key is always 12-byte length.
     */
    public void testLoadLicense() throws Throwable {


        byte[] keyId = downloadLicense();

        String keyIdString = new String(keyId);
        assertTrue("Key was " + keyIdString, keyIdString.startsWith("ksid"));
        assertEquals(keyId.length, 12);
    }


    /**
     * Tests whether a newly generated google license is generated has infinite expiry
     */
    public void testLicenseExpiry() throws Throwable {

        byte[] keyId = downloadLicense();
        long licensePeriodLeft1 = mLicenseProvider.getLicensePeriodLeft(keyId);

        assertEquals(Math.abs(C.TIME_UNSET), licensePeriodLeft1);

    }

    private byte[] downloadLicense() throws Throwable {

        final Object[] result = {null};

        TestScheduler scheduler = new TestScheduler();
        mLicenseProvider.loadLicense2()
                .subscribeOn(scheduler)
                .observeOn(scheduler)
                .subscribe(new Consumer<byte[]>() {
                    @Override
                    public void accept(byte[] bytes) throws Exception {
                        result[0] = bytes;
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        result[0] = throwable;
                    }
                });


        scheduler.triggerActions();

        if (result[0] instanceof Throwable) {
            throw (Throwable) result[0];
        }

        return (byte[]) result[0];

    }


}
