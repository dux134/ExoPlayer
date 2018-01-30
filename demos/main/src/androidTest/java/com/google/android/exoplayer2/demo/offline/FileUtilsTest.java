package com.google.android.exoplayer2.demo.offline;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.google.android.exoplayer2.util.Util;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

/**
 * Created by sharish on 30/01/18.
 */

public class FileUtilsTest extends InstrumentationTestCase {

    private File cacheDir;
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
        cacheDir = Util.createTempDirectory(context, "DemoTest");

        sampleJsonKey = "Hello";

        sampleJsonObject = new JSONObject();
        sampleJsonObject.put(sampleJsonKey, "world");
    }

    @Override
    protected void tearDown() throws Exception {
        Util.recursiveDelete(cacheDir);
        super.tearDown();
    }

    public void testJsonReadWriteEncryption() throws Exception {

        FileUtils.writeEncryptedJson(cacheDir, "testJsonReadWriteEncryption", _16ByteEncryptionKey, sampleJsonObject);
        JSONObject outputObject = FileUtils.readEncryptedJson(cacheDir, "testJsonReadWriteEncryption", _16ByteEncryptionKey);

        assertEquals(sampleJsonObject.length(), outputObject.length());
        assertEquals(sampleJsonObject.getString(sampleJsonKey), outputObject.getString(sampleJsonKey));
    }

    public void testJsonReadWrite() throws Exception {

        FileUtils.writeJson(cacheDir, "testJsonReadWrite", sampleJsonObject);
        JSONObject outputObject = FileUtils.readJson(cacheDir, "testJsonReadWrite");

        assertEquals(sampleJsonObject.length(), outputObject.length());
        assertEquals(sampleJsonObject.getString(sampleJsonKey), outputObject.getString(sampleJsonKey));
    }

    public void testByteReadWrite() throws Exception {

        FileUtils.writeBytes(cacheDir, "testByteReadWrite", _16ByteEncryptionKey);
        byte[] readBytes = FileUtils.readBytes(cacheDir, "testByteReadWrite");

        assertTrue(Arrays.equals(_16ByteEncryptionKey, readBytes));
    }
}
