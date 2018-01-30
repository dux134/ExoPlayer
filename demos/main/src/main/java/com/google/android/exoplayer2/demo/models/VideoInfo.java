package com.google.android.exoplayer2.demo.models;

/**
 * Created by sharish on 25/01/18.
 */

public class VideoInfo {

    private String mVideoId;
    private String mManifestUrl;
    private String mLicenseUrl;
    private String[] mKeyReqProperties;
    private boolean mIsCacheNeeded;

    public String getVideoId() {
        return mVideoId;
    }

    public void setVideoId(String mVideoId) {
        this.mVideoId = mVideoId;
    }

    public String getManifestUrl() {
        return mManifestUrl;
    }

    public void setManifestUrl(String mManifestUrl) {
        this.mManifestUrl = mManifestUrl;
    }

    public String getLicenseUrl() {
        return mLicenseUrl;
    }

    public void setLicenseUrl(String mLicenseUrl) {
        this.mLicenseUrl = mLicenseUrl;
    }

    public String[] getKeyReqProperties() {
        return mKeyReqProperties;
    }

    public void setKeyReqProperties(String[] mKeyReqProperties) {
        this.mKeyReqProperties = mKeyReqProperties;
    }

    public boolean isCacheNeeded() {
        return mIsCacheNeeded;
    }

    public void setCacheNeeded(boolean mIsCacheNeeded) {
        this.mIsCacheNeeded = mIsCacheNeeded;
    }
}
