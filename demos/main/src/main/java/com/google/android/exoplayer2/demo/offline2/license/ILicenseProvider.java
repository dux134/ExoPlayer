package com.google.android.exoplayer2.demo.offline2.license;

import io.reactivex.Flowable;
import io.reactivex.Single;

/**
 * Created by sharish on 22/01/18.
 */

public interface ILicenseProvider {

    Single<byte[]> loadLicense();

    Flowable<byte[]> loadLicense2();


    long getLicensePeriodLeft(byte[] keyId);
}
