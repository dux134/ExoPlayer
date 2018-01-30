package com.google.android.exoplayer2.demo.offline2.license;

import android.util.Log;

import com.google.android.exoplayer2.demo.models.CacheInfo;
import com.google.android.exoplayer2.demo.offline.OfflineUtil;

import java.io.File;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

/**
 * Created by sharish on 22/01/18.
 */

public class OfflineLicenseProvider implements ILicenseProvider {

    private String mId;
    private File mBaseFolder;
    private ILicenseProvider mBaseProvider;
    private String mKey;

    public OfflineLicenseProvider(String mId, File baseFolder, ILicenseProvider mBaseProvider, String key) {
        this.mId = mId;
        this.mBaseFolder = baseFolder;
        this.mBaseProvider = mBaseProvider;
        this.mKey = key;
    }

    @Override
    public Single<byte[]> loadLicense() {

        long t1 = System.currentTimeMillis();
        CacheInfo cacheInfo = OfflineUtil.getCacheInfo(mBaseFolder, mId, mKey);
        long t2 = System.currentTimeMillis();

        Log.d("ExecutionCheck", "CacheLoad:"+(t2-t1));


        byte[] keyId = null;
        if (cacheInfo != null) {
            keyId = cacheInfo.getLicenseKey();
        }

        // if cache exists, check if good amount of time expiry left for the license and return the license key.
        if (keyId != null && keyId.length > 0) {

            long t3 = System.currentTimeMillis();

            long licensePeriodLeft = getLicensePeriodLeft(keyId);

            long t4 = System.currentTimeMillis();

            Log.d("ExecutionCheck", "LicExpiryCheck:"+(t4-t3));

            if (licensePeriodLeft > 60) {
                return Single.just(keyId);
            }
        }

        // otherwise download from base provider and store it in cache if valid.
        return mBaseProvider.loadLicense().map(new Function<byte[], byte[]>() {
            @Override
            public byte[] apply(byte[] newKeyId) throws Exception {

                if (newKeyId.length > 0) {

                    CacheInfo cacheInfo = OfflineUtil.getCacheInfo(mBaseFolder, mId, mKey);

                    if (cacheInfo == null) {
                        cacheInfo = new CacheInfo(mId);
                    }

                    cacheInfo.setLicenseKey(newKeyId);

                    OfflineUtil.storeCacheInfo(mBaseFolder, mId, cacheInfo, mKey);
                }

                return newKeyId;
            }
        });
    }

    @Override
    public Flowable<byte[]> loadLicense2() {
        CacheInfo cacheInfo = OfflineUtil.getCacheInfo(mBaseFolder, mId, mKey);

        byte[] keyId = null;
        if (cacheInfo != null) {
            keyId = cacheInfo.getLicenseKey();
        }

        // if cache exists, check if good amount of time expiry left for the license and return the license key.
        if (keyId != null && keyId.length > 0) {
            long licensePeriodLeft = getLicensePeriodLeft(keyId);

            if (licensePeriodLeft > 60) {
                return Flowable.just(keyId);
            }
        }

        // otherwise download from base provider and store it in cache if valid.
        return mBaseProvider.loadLicense2().map(new Function<byte[], byte[]>() {
            @Override
            public byte[] apply(byte[] newKeyId) throws Exception {

                if (newKeyId.length > 0) {

                    CacheInfo cacheInfo = OfflineUtil.getCacheInfo(mBaseFolder, mId, mKey);

                    if (cacheInfo == null) {
                        cacheInfo = new CacheInfo(mId);
                    }

                    cacheInfo.setLicenseKey(newKeyId);

                    OfflineUtil.storeCacheInfo(mBaseFolder, mId, cacheInfo, mKey);
                }

                return newKeyId;
            }
        });
    }

    @Override
    public long getLicensePeriodLeft(byte[] keyId) {
        return mBaseProvider.getLicensePeriodLeft(keyId);
    }
}
