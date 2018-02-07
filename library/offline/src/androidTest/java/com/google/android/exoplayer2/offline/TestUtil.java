package com.google.android.exoplayer2.offline;

import android.util.Log;

import com.google.android.exoplayer2.offline.models.DownloadInfo;

import java.io.File;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.TestScheduler;

/**
 * Created by sharish on 07/02/18.
 */

public class TestUtil {

    private static final String SAMPLE_DASH_VIDEO_MANIFEST_URL = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd";
    private static final String SAMPLE_HLS_VIDEO_MANIFEST_URL = "https://tungsten.aaplimg.com/VOD/bipbop_adv_example_v2/master.m3u8";


    static long folderSize(File directory, String matchExtn, String discardNamePrefix) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                String name = file.getName();
                if (name.endsWith(matchExtn) && !name.startsWith(discardNamePrefix)) {
                    length += file.length();
                }
            } else
                length += folderSize(file, matchExtn, discardNamePrefix);
        }
        return length;
    }

    static DownloadInfo downloadSampleDashVideo(File baseCacheDir, String id, String encKey) {

        Flowable<DownloadInfo> asyncDownloader = DashOfflineUtil.downloadAsync(baseCacheDir, id, SAMPLE_DASH_VIDEO_MANIFEST_URL, encKey, 180);

        final DownloadInfo[] result = {null};
        final Throwable[] err = {null};

        TestScheduler scheduler1 = new TestScheduler();
        TestScheduler scheduler2 = new TestScheduler();

        asyncDownloader.subscribeOn(scheduler1)
                .observeOn(scheduler2)
                .subscribe(new Consumer<DownloadInfo>() {
                    @Override
                    public void accept(DownloadInfo integer) throws Exception {
                        result[0] = integer;

                        int downloadPercent = (int) result[0].downloadPercent;
                        Log.d("TestLog", "Download Percent:" + downloadPercent);


                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                        err[0] = throwable;
                        result[0] = new DownloadInfo(-2, 0);
                    }
                });

        scheduler1.triggerActions();
        scheduler2.triggerActions();

        float lastKnownValue = result[0].downloadPercent;

        while (lastKnownValue <= result[0].downloadPercent) {

            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (lastKnownValue == result[0].downloadPercent) {
                break;
            }

            if (result[0].downloadPercent == 100) {
                break;
            }

        }

        if (result[0].downloadPercent == -2) {
            throw new RuntimeException(err[0]);
        }

        return result[0];

    }


    static DownloadInfo downloadSampleHlsVideo(File baseCacheDir, String id, String encKey) {

        Flowable<DownloadInfo> asyncDownloader = HlsOfflineUtil.downloadAsync(baseCacheDir, id, SAMPLE_HLS_VIDEO_MANIFEST_URL, encKey, 180);

        final DownloadInfo[] result = {null};
        final Throwable[] err = {null};

        TestScheduler scheduler1 = new TestScheduler();
        TestScheduler scheduler2 = new TestScheduler();

        asyncDownloader.subscribeOn(scheduler1)
                .observeOn(scheduler2)
                .subscribe(new Consumer<DownloadInfo>() {
                    @Override
                    public void accept(DownloadInfo integer) throws Exception {
                        result[0] = integer;

                        int downloadPercent = (int) result[0].downloadPercent;
                        Log.d("TestLog", "Download Percent:" + downloadPercent);


                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                        err[0] = throwable;
                        result[0] = new DownloadInfo(-2, 0);
                    }
                });

        scheduler1.triggerActions();
        scheduler2.triggerActions();

        float lastKnownValue = result[0].downloadPercent;

        while (lastKnownValue <= result[0].downloadPercent) {

            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (lastKnownValue == result[0].downloadPercent) {
                break;
            }

            if (result[0].downloadPercent == 100) {
                break;
            }

        }

        if (result[0].downloadPercent == -2) {
            throw new RuntimeException(err[0]);
        }

        return result[0];

    }


}
