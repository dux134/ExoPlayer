package com.google.android.exoplayer2.offline.models;

/**
 * Created by sharish on 05/02/18.
 */

public class DownloadInfo {

    public float downloadPercent;
    public long downloadBytes;

    public DownloadInfo(float downloadPercent, long downloadBytes) {
        this.downloadPercent = downloadPercent;
        this.downloadBytes = downloadBytes;
    }
}
