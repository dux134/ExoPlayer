package com.google.android.exoplayer2.demo.offline;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

/**
 * Created by sharish on 30/01/18.
 */

@RunWith(RobolectricTestRunner.class)
public class OfflineUtilTest {

    /**
     * Test to verify the output of get16ByteSecretKey is always 16, when valid input is passed, null when empty input is passed.
     */
    @Test
    public void test16ByteKey() {
        byte[] positiveResult = OfflineUtil.get16ByteSecretKey("123");
        byte[] negResult = OfflineUtil.get16ByteSecretKey(null);
        byte[] negResult2 = OfflineUtil.get16ByteSecretKey("");


        assertEquals(16, positiveResult.length);
        assertEquals(null, negResult);
        assertEquals(null, negResult2);

    }

}
