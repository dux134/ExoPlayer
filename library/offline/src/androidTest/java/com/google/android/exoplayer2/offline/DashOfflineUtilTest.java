package com.google.android.exoplayer2.offline;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.google.android.exoplayer2.offline.models.DownloadInfo;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONObject;

import java.io.File;

/**
 * Created by sharish on 30/01/18.
 */

public class DashOfflineUtilTest extends InstrumentationTestCase {

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
        DownloadInfo downloadInfo = TestUtil.downloadSampleDashVideo(baseCacheDir, videoId, new String(_16ByteEncryptionKey));

        // verify that 'Download percent is 100 '
        assertEquals(100, (int) downloadInfo.downloadPercent);

        File videoCacheFolder = new File(baseCacheDir, videoId);

        long folderSize = TestUtil.folderSize(videoCacheFolder, "exo", "0.0");

        boolean hasCache = videoCacheFolder.list().length > 0;

        // verify that 'Downloaded cache folder has files'
        assertTrue("No cache exists ", hasCache);

        // verify that 'Cumulative file size in the download folder matches with downloaded info.'
        assertEquals(folderSize, downloadInfo.downloadBytes);

    }


}
