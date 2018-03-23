package com.google.android.exoplayer2.offline.dataprovider.license;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.google.android.exoplayer2.offline.OfflineUtil;
import com.google.android.exoplayer2.offline.models.CacheInfo;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.Arrays;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.TestScheduler;

/**
 * Created by sharish on 30/01/18.
 */
public class OfflineLicenseProviderTest extends InstrumentationTestCase {

    private File cacheDir;
    private byte[] _16ByteEncryptionKey = {
            0, 1, 2, 3,
            4, 5, 6, 7,
            8, 9, 10, 11,
            12, 13, 14, 15
    };

    private OfflineLicenseProvider mLicenseProvider;
    private String videoId = "123";


    @Override
    public void setUp() throws Exception {
        super.setUp();

        Context context = getInstrumentation().getTargetContext();
        cacheDir = Util.createTempDirectory(context, "DemoTest");

    }

    @Override
    protected void tearDown() throws Exception {
        Util.recursiveDelete(cacheDir);
        super.tearDown();
    }

    /**
     * Tests whether a proper license is generated and the same is stored in cache.
     */
    public void testLoadLicense() throws Throwable {

        byte[] key1 = {1, 2, 3, 4};

        initLicenseProvider(key1, 100);
        byte[] keyId = downloadLicense();

        assertTrue(Arrays.equals(key1, keyId)); // verify mock key is retrieved.

        CacheInfo cacheInfo = OfflineUtil.getCacheInfo(cacheDir, videoId, new String(_16ByteEncryptionKey));
        assertTrue(Arrays.equals(cacheInfo.getLicenseKey(), keyId)); // verify that cache info also holds the same key.
    }


    /**
     * Create a new license and verify the following
     *
     * 1. Verify that base license provider is hit when no cache exists and stores such value in cache.
     * 2. Verify that base provider is not hit when cache exists
     * 3. Verify that cache source is hit when cache exists
     * 4. Verify that base license provider is hit when the license expiry is too small.
     */
    public void testLoadLicense2() throws Throwable {


        byte[] mockKey1 = {1, 2, 3, 4};
        byte[] mockKey2 = {5, 6, 7, 8};
        byte[] mockKey3 = {9, 10, 11, 12};


        initLicenseProvider(mockKey1, 100);
        byte[] actualKey1 = downloadLicense();

        CacheInfo cacheInfo = OfflineUtil.getCacheInfo(cacheDir, videoId, new String(_16ByteEncryptionKey));

        assertTrue(Arrays.equals(cacheInfo.getLicenseKey(), mockKey1)); // verify that cache info also holds the same key.


        initLicenseProvider(mockKey2, 100);
        byte[] actualKey2 = downloadLicense();


        assertTrue(Arrays.equals(mockKey1, actualKey1)); //1.  Verify that base license provider is hit when no cache exists and stores such value in cache.
        assertFalse(Arrays.equals(mockKey2, actualKey2));//2.  Verify that base provider is not hit when cache exists
        assertTrue(Arrays.equals(mockKey1, actualKey2)); //3. Verify that cache source is hit when cache exists


        initLicenseProvider(mockKey3, 10);
        byte[] actualKey3 = downloadLicense();

        assertTrue(Arrays.equals(mockKey3, actualKey3)); // 4. Verify that base license provider is hit when the expiry is too small.

    }


    private void initLicenseProvider(final byte[] key, final int lifetime) {


        ILicenseProvider dummyLicenseProvider = new ILicenseProvider() {
            @Override
            public Flowable<byte[]> loadLicense2() {
                return Flowable.just(key);
            }

            @Override
            public long getLicensePeriodLeft(byte[] keyId) {
                return lifetime;
            }
        };


        mLicenseProvider = new OfflineLicenseProvider(videoId, cacheDir, dummyLicenseProvider, new String(_16ByteEncryptionKey));
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
