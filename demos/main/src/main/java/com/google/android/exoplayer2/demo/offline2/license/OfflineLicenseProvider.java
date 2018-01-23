package com.google.android.exoplayer2.demo.offline2.license;

import com.google.android.exoplayer2.demo.offline.OfflineUtil;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.functions.Function;

/**
 * Created by sharish on 22/01/18.
 */

public class OfflineLicenseProvider implements ILicenseProvider {

    private String mId;
    private File mCacheFolder;
    private ILicenseProvider mBaseProvider;

    public OfflineLicenseProvider(String mId, File mCacheFolder, ILicenseProvider mBaseProvider) {
        this.mId = mId;
        this.mCacheFolder = mCacheFolder;
        this.mBaseProvider = mBaseProvider;
    }

    @Override
    public Single<byte[]> loadLicense() {

        byte[] keyId = OfflineUtil.readOfflineKeyId(mCacheFolder, mId);

        // if cache exists, return cache.
        if (keyId != null && keyId.length > 0) {
            return Single.just(keyId);
        }

        // otherwise download from base provider and store it in cache if valid.
        return mBaseProvider.loadLicense().map(new Function<byte[], byte[]>() {
            @Override
            public byte[] apply(byte[] newKeyId) throws Exception {

                if (newKeyId.length > 0) {
                    OfflineUtil.storeOfflineKeyId(mCacheFolder, mId, newKeyId);
                }

                return newKeyId;
            }
        });
    }
}
