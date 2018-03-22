package com.google.android.exoplayer2.offline.dataprovider.license;

import android.net.Uri;

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

/**
 * Created by sharish on 22/01/18.
 */

public class OnlineLicenseProvider implements ILicenseProvider {

    private HttpDataSource.Factory httpDataSourceFactory;
    private MediaDrmCallback mMediaDrmCallback;
    private String mPlayingUri;

    public OnlineLicenseProvider(HttpDataSource.Factory httpDataSourceFactory, MediaDrmCallback mMediaDrmCallback, String mPlayingUri) {
        this.httpDataSourceFactory = httpDataSourceFactory;
        this.mMediaDrmCallback = mMediaDrmCallback;
        this.mPlayingUri = mPlayingUri;

    }

    @Override
    public Flowable<byte[]> loadLicense2() {
        return Flowable.create(new FlowableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(FlowableEmitter<byte[]> e) throws Exception {

                try {
                    byte[] keyId = loadLicenseSync();

                    if (keyId != null) {
                        e.onNext(keyId);
                    } else {
                        e.onError(new RuntimeException("Failed to download key id"));
                    }
                }catch (Throwable t) {
                    if(e.isCancelled()) {
                        return;
                        // do nothing.
                    }
                    throw t;
                }

            }
        }, BackpressureStrategy.BUFFER);
    }

    @Override
    public long getLicensePeriodLeft(byte[] keyId) {


        UUID widevineUuid = C.WIDEVINE_UUID;
        FrameworkMediaDrm frameworkMediaDrm;
        try {
            frameworkMediaDrm = FrameworkMediaDrm.newInstance(widevineUuid);

            OfflineLicenseHelper<FrameworkMediaCrypto> offlineLicenseHelper = new OfflineLicenseHelper<>(widevineUuid, frameworkMediaDrm, mMediaDrmCallback, null);

            return offlineLicenseHelper.getLicenseDurationRemainingSec(keyId).first;

        } catch (Exception e) {
            e.printStackTrace();
        }


        return 0;
    }


    private byte[] loadLicenseSync() throws IOException, InterruptedException, DrmSession.DrmSessionException, UnsupportedDrmException {

        UUID widevineUuid = C.WIDEVINE_UUID;
        FrameworkMediaDrm frameworkMediaDrm = FrameworkMediaDrm.newInstance(widevineUuid);

        OfflineLicenseHelper<FrameworkMediaCrypto> offlineLicenseHelper = new OfflineLicenseHelper<>(widevineUuid, frameworkMediaDrm, mMediaDrmCallback, null);


        DataSource dataSource = httpDataSourceFactory.createDataSource();
        DashManifest dashManifest = DashUtil.loadManifest(dataSource, Uri.parse(mPlayingUri));
        DrmInitData drmInitData = DashUtil.loadDrmInitData(dataSource, dashManifest.getPeriod(0));

        return offlineLicenseHelper.downloadLicense(drmInitData);
    }
}
