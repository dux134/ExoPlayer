package com.google.android.exoplayer2.offline;

import android.content.Context;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.android.exoplayer2.offline.models.CacheInfo;
import com.google.android.exoplayer2.offline.models.DownloadInfo;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.TestScheduler;

/**
 * Created by sharish on 30/01/18.
 */

public class OfflineUtilTest extends InstrumentationTestCase {

    private static final String SAMPLE_VIDEO_MANIFEST_URL = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd";

    private File baseCacheDir;
    private byte[] _16ByteEncryptionKey = {
            0, 1, 2, 3,
            4, 5, 6, 7,
            8, 9, 10, 11,
            12, 13, 14, 15
    };

    private JSONObject sampleJsonObject;
    private String sampleJsonKey;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Context context = getInstrumentation().getTargetContext();
        File externalCacheDir = context.getExternalCacheDir();
        File testFolder = new File(externalCacheDir, "tests");

        if (!testFolder.exists()) {
            testFolder.mkdir();
        }

        baseCacheDir = testFolder; //Util.createTempDirectory(context, "DemoTest");

        sampleJsonKey = "Hello";

        sampleJsonObject = new JSONObject();
        sampleJsonObject.put(sampleJsonKey, "world");
    }

    @Override
    protected void tearDown() throws Exception {
        Util.recursiveDelete(baseCacheDir);
        super.tearDown();
    }

    /**
     * Downloads a sample video and checks for the following
     * <p>
     * 1. Download percent is 100
     * 2. Downloaded cache folder has files
     * 3. Cumulative file size in the download folder matches with downloaded info.
     */
    public void testDownloadAsync() throws InterruptedException {

        String videoId = "123";
        DownloadInfo downloadInfo = downloadSampleVideo(videoId, new String(_16ByteEncryptionKey));

        // verify that 'Download percent is 100 '
        assertEquals(100, (int) downloadInfo.downloadPercent);

        File videoCacheFolder = new File(baseCacheDir, videoId);

        long folderSize = folderSize(videoCacheFolder, "exo", "0.0");

        boolean hasCache = videoCacheFolder.list().length > 0;

        // verify that 'Downloaded cache folder has files'
        assertTrue("No cache exists ", hasCache);

        // verify that 'Cumulative file size in the download folder matches with downloaded info.'
        assertEquals(folderSize, downloadInfo.downloadBytes);

    }

    /**
     * Downloads a sample video and checks for the following.
     * <p>
     * 1. Downloaded video percent matches with cache info
     * 2. Downloaded bytes matches with cache info.
     * 3. Verifies whether cache available is true
     */
    public void testGetCacheInfoAndIsCacheAvailable() {

        String videoId = "123";

        String encKey = new String(_16ByteEncryptionKey);
        DownloadInfo downloadInfo = downloadSampleVideo(videoId, encKey);

        CacheInfo cacheInfo = OfflineUtil.getCacheInfo(baseCacheDir, videoId, encKey);

        assertEquals(downloadInfo.downloadPercent, cacheInfo.getDownloadPercent());
        assertEquals(downloadInfo.downloadBytes, cacheInfo.getDownloadBytes());
        assertTrue(OfflineUtil.isCacheAvailable(baseCacheDir, videoId, encKey));

    }

    /**
     * Insert a dummy cache info and reads back to verify the data saved are correct.
     */
    public void testStoreCacheInfo() {

        String videoId = "123";
        String encKey = new String(_16ByteEncryptionKey);

        CacheInfo writeCacheInfo = new CacheInfo(videoId);
        writeCacheInfo.setDownloadBytes(100);
        writeCacheInfo.setDownloadPercent(100);
        writeCacheInfo.setLicenseKey(_16ByteEncryptionKey);

        OfflineUtil.storeCacheInfo(baseCacheDir, videoId, writeCacheInfo, encKey);


        CacheInfo readCacheInfo = OfflineUtil.getCacheInfo(baseCacheDir, videoId, encKey);

        assertEquals(writeCacheInfo.getDownloadBytes(), readCacheInfo.getDownloadBytes());
        assertEquals(writeCacheInfo.getDownloadPercent(), readCacheInfo.getDownloadPercent());
        assertTrue(Arrays.equals(writeCacheInfo.getLicenseKey(), readCacheInfo.getLicenseKey()));
        assertTrue(writeCacheInfo.getId().equals(readCacheInfo.getId()));

    }


    private DownloadInfo downloadSampleVideo(String id, String encKey) {


        Uri uri = Uri.parse(SAMPLE_VIDEO_MANIFEST_URL);

        Flowable<DownloadInfo> asyncDownloader = OfflineUtil.downloadAsync(baseCacheDir, id, uri, encKey, 180);

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


    private static long folderSize(File directory, String matchExtn, String discardNamePrefix) {
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
}
