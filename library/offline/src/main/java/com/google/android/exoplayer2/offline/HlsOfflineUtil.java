package com.google.android.exoplayer2.offline;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.offline.models.CacheInfo;
import com.google.android.exoplayer2.offline.models.DownloadInfo;
import com.google.android.exoplayer2.source.hls.offline.HlsDownloader;
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistParser;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.ParsingLoadable;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

import static com.google.android.exoplayer2.offline.OfflineUtil.get16ByteSecretKey;
import static com.google.android.exoplayer2.offline.OfflineUtil.storeCacheInfo;

/**
 * Created by sharish on 22/01/18.
 */

public class HlsOfflineUtil {

    /**
     * Downloads the video in asynchronous way.
     *
     * @param downloadFolder         - target directory where the video should be downloaded
     * @param id                     - Unique video id, based on which sub directory will be identified.
     * @param manifestUrl            - target video manifest url
     * @param key                    - Encryption key if any.
     * @param targetVideoPixelHeight - Target video height.
     * @return - Rx Flowable reference for async downloading.
     */
    public static Flowable<DownloadInfo> downloadAsync(final File downloadFolder, final String id, final String manifestUrl, final String key, final int targetVideoPixelHeight) {

        return Flowable.create(new FlowableOnSubscribe<DownloadInfo>() {
            @Override
            public void subscribe(final FlowableEmitter<DownloadInfo> e) throws Exception {

                try {

                    downloadSync(downloadFolder, id, manifestUrl, key, targetVideoPixelHeight, new Downloader.ProgressListener() {
                        @Override
                        public void onDownloadProgress(Downloader downloader, float downloadPercentage, long downloadedBytes) {

                            DownloadInfo downloadInfo = new DownloadInfo(downloadPercentage, downloadedBytes);
                            e.onNext(downloadInfo);

                            if (downloadPercentage == 100) {
                                e.onComplete();
                            }
                        }
                    });

                } catch (InterruptedException e1) {
                    if (e.isCancelled()) return;
                    throw e1;
                }


            }

        }, BackpressureStrategy.BUFFER);
    }


    private static String[] getRepresentationKeys(HlsMasterPlaylist manifest, int pixelHeight) {

        List<HlsMasterPlaylist.HlsUrl> variants = manifest.variants;

        int periodCount = variants.size();

        ArrayList<String> keys = new ArrayList<>();

        for (int i = 0; i < periodCount; i++) {

            HlsMasterPlaylist.HlsUrl hlsUrl = variants.get(i);
            if (hlsUrl.format.height == pixelHeight) {
                keys.add(hlsUrl.url);
            }

        }

        List<HlsMasterPlaylist.HlsUrl> audios = manifest.audios;

        for (HlsMasterPlaylist.HlsUrl audioUrl : audios) {
            keys.add(audioUrl.url);

        }


        return keys.toArray(new String[keys.size()]);

    }


    public static void downloadSync(final File baseFolder, final String id, final String manifestUrl, final String key, int targetVideoPixelHeight, final Downloader.ProgressListener listener) throws Exception {

        if (!baseFolder.exists()) {
            baseFolder.mkdir();
        }

        byte[] secretKey = get16ByteSecretKey(key);


        File manifestFolder = new File(baseFolder, id);

        if (!manifestFolder.exists()) {
            manifestFolder.mkdir();
        }

        Uri uri = Uri.parse(manifestUrl);

        SimpleCache cache = new SimpleCache(manifestFolder, new NoOpCacheEvictor(), secretKey);
        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("ExoPlayer", null);
        DownloaderConstructorHelper constructorHelper =
                new DownloaderConstructorHelper(cache, factory);


        HlsDownloader hlsDownloader = new HlsDownloader(uri, constructorHelper);

        HlsMasterPlaylist hlsPlaylist = (HlsMasterPlaylist) loadManifest(factory.createDataSource(), uri);

        // Select the first representation of the first adaptation set of the first period
        hlsDownloader.selectRepresentations(getRepresentationKeys(hlsPlaylist, targetVideoPixelHeight));

        CacheInfo cacheInfoNonfinal = OfflineUtil.getCacheInfo(baseFolder, id, key);

        if (cacheInfoNonfinal == null) {
            cacheInfoNonfinal = new CacheInfo(id);
        }

        final CacheInfo cacheInfo = cacheInfoNonfinal;

        hlsDownloader.download(new Downloader.ProgressListener() {
            @Override
            public void onDownloadProgress(Downloader downloader, float downloadPercentage, long downloadedBytes) {

                cacheInfo.setDownloadBytes(downloadedBytes);
                cacheInfo.setDownloadPercent(downloadPercentage);

                storeCacheInfo(baseFolder, id, cacheInfo, key);

                if (listener != null) {
                    listener.onDownloadProgress(downloader, downloadPercentage, downloadedBytes);
                }
            }
        });
    }

    private static HlsPlaylist loadManifest(DataSource dataSource, Uri uri) throws IOException {
        DataSpec dataSpec = new DataSpec(uri,
                DataSpec.FLAG_ALLOW_CACHING_UNKNOWN_LENGTH | DataSpec.FLAG_ALLOW_GZIP);
        ParsingLoadable<HlsPlaylist> loadable = new ParsingLoadable<>(dataSource, dataSpec,
                C.DATA_TYPE_MANIFEST, new HlsPlaylistParser());
        loadable.load();
        return loadable.getResult();
    }

}
