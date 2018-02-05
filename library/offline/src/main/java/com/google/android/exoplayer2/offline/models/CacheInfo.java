package com.google.android.exoplayer2.offline.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by sharish on 29/01/18.
 */

public class CacheInfo {

    private static final String KEY_ID = "_id";
    private static final String KEY_DOWNLOAD_PERCENT = "download_percent";
    private static final String KEY_LICENSE_KEY = "license_key";
    private static final String KEY_DOWNLOADED_BYTES = "download_bytes";


    private String mId;
    private float mDownloadPercent;
    private byte[] mLicenseKey;
    private long mDownloadBytes;


    public CacheInfo(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public float getDownloadPercent() {
        return mDownloadPercent;
    }

    public void setDownloadPercent(float mDownloadPercent) {
        this.mDownloadPercent = mDownloadPercent;
    }

    public byte[] getLicenseKey() {
        return mLicenseKey;
    }

    public void setLicenseKey(byte[] mLicenseKey) {
        this.mLicenseKey = mLicenseKey;
    }

    public long getDownloadBytes() {
        return mDownloadBytes;
    }

    public void setDownloadBytes(long mDownloadBytes) {
        this.mDownloadBytes = mDownloadBytes;
    }


    public void fromJson(JSONObject object) {

        if (object == null) return;
        try {

            mId = object.optString(KEY_ID);
            mDownloadPercent = (float) object.optDouble(KEY_DOWNLOAD_PERCENT);
            mLicenseKey = object.optString(KEY_LICENSE_KEY).getBytes("UTF-8");
            mDownloadBytes = object.optLong(KEY_DOWNLOADED_BYTES);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public JSONObject toJson() {

        JSONObject object = new JSONObject();

        try {

            object.put(KEY_ID, mId);
            object.put(KEY_DOWNLOAD_PERCENT, mDownloadPercent);
            if (mLicenseKey != null) {
                object.put(KEY_LICENSE_KEY, new String(mLicenseKey));
            }
            object.put(KEY_DOWNLOADED_BYTES, mDownloadBytes);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }
}
