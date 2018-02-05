package com.google.android.exoplayer2.demo.offline2.license;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.OfflineLicenseHelper;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import java.io.IOException;
import java.util.UUID;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

/**
 * Created by sharish on 22/01/18.
 */

public class OnlineLicenseProvider implements ILicenseProvider {

    private HttpDataSource.Factory httpDataSourceFactory;
    private MediaDrmCallback mMediaDrmCallback;
    private Uri mPlayingUri;

    public OnlineLicenseProvider(HttpDataSource.Factory httpDataSourceFactory, MediaDrmCallback mMediaDrmCallback, Uri mPlayingUri) {
        this.httpDataSourceFactory = httpDataSourceFactory;
        this.mMediaDrmCallback = mMediaDrmCallback;
        this.mPlayingUri = mPlayingUri;

    }

    @Override
    public Single<byte[]> loadLicense() {


        return Single.create(new SingleOnSubscribe<byte[]>() {
            @Override
            public void subscribe(SingleEmitter<byte[]> e) throws Exception {

                byte[] keyId = loadLicenseSync();

                if (keyId != null) {
                    e.onSuccess(keyId);
                } else {
                    e.onError(new RuntimeException("Failed to download key id"));
                }
            }
        });

    }

    @Override
    public Flowable<byte[]> loadLicense2() {
        return Flowable.create(new FlowableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(FlowableEmitter<byte[]> e) throws Exception {
                byte[] keyId = loadLicenseSync();

                if (keyId != null) {
                    e.onNext(keyId);
                } else {
                    e.onError(new RuntimeException("Failed to download key id"));
                }
            }
        }, BackpressureStrategy.BUFFER);
    }

    @Override
    public long getLicensePeriodLeft(byte[] keyId) {

        long t1 = System.currentTimeMillis();

        UUID widevineUuid = C.WIDEVINE_UUID;
        FrameworkMediaDrm frameworkMediaDrm;
        try {
            frameworkMediaDrm = FrameworkMediaDrm.newInstance(widevineUuid);

            long t2 = System.currentTimeMillis();

            OfflineLicenseHelper<FrameworkMediaCrypto> offlineLicenseHelper = new OfflineLicenseHelper<>(widevineUuid, frameworkMediaDrm, mMediaDrmCallback, null);

            long t3 = System.currentTimeMillis();

            long left = offlineLicenseHelper.getLicenseDurationRemainingSec(keyId).first;

            long t4 = System.currentTimeMillis();


            Log.d("ExecutionCheck", "getLicensePeriodLeft: t4-t3:" + (t4 - t3));
            Log.d("ExecutionCheck", "getLicensePeriodLeft: t3-t2:" + (t3 - t2));
            Log.d("ExecutionCheck", "getLicensePeriodLeft: t2-t1:" + (t2 - t1));

            return left;

        } catch (Exception e) {
            e.printStackTrace();
        }


        return 0;
    }


    byte[] loadLicenseSync() throws IOException, InterruptedException, DrmSession.DrmSessionException, UnsupportedDrmException {

        UUID widevineUuid = C.WIDEVINE_UUID;
        FrameworkMediaDrm frameworkMediaDrm = FrameworkMediaDrm.newInstance(widevineUuid);

        OfflineLicenseHelper<FrameworkMediaCrypto> offlineLicenseHelper = new OfflineLicenseHelper<>(widevineUuid, frameworkMediaDrm, mMediaDrmCallback, null);


        DataSource dataSource = httpDataSourceFactory.createDataSource();
        DashManifest dashManifest = DashUtil.loadManifest(dataSource, mPlayingUri);
        DrmInitData drmInitData = DashUtil.loadDrmInitData(dataSource, dashManifest.getPeriod(0));

        return offlineLicenseHelper.downloadLicense(drmInitData);
    }
}
