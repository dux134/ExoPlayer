package com.google.android.exoplayer2.offline;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.google.android.exoplayer2.offline.models.CacheInfo;
import com.google.android.exoplayer2.offline.models.DownloadInfo;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

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
     * Downloads a sample video and checks for the following.
     * <p>
     * 1. Downloaded video percent matches with cache info
     * 2. Downloaded bytes matches with cache info.
     * 3. Verifies whether cache available is true
     */
    public void testGetCacheInfoAndIsCacheAvailable() {

        String videoId = "123";

        String encKey = new String(_16ByteEncryptionKey);
        DownloadInfo downloadInfo = TestUtil.downloadSampleDashVideo(baseCacheDir, videoId, encKey);

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

}
