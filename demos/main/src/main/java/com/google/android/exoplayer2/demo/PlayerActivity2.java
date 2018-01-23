/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.demo.offline.DrmUtil;
import com.google.android.exoplayer2.demo.offline.OfflineUtil;
import com.google.android.exoplayer2.demo.offline2.IVideoStreamDataSourceProvider;
import com.google.android.exoplayer2.demo.offline2.OfflineStreamProvider;
import com.google.android.exoplayer2.demo.offline2.OnlineStreamProvider;
import com.google.android.exoplayer2.demo.offline2.license.ILicenseProvider;
import com.google.android.exoplayer2.demo.offline2.license.OfflineLicenseProvider;
import com.google.android.exoplayer2.demo.offline2.license.OnlineLicenseProvider;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity2 extends Activity implements OnClickListener,
        PlaybackControlView.VisibilityListener {

    public static final String DRM_LICENSE_URL = "drm_license_url";
    public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";

    public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
    public static final String EXTENSION_EXTRA = "extension";

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    public static final String VIDEO_NAME = "video_name";

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private Handler mainHandler;
    private EventLogger eventLogger;
    private SimpleExoPlayerView simpleExoPlayerView;
    private LinearLayout debugRootView;
    private TextView debugTextView;
    private Button retryButton;

    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private TrackSelectionHelper trackSelectionHelper;
    private DebugTextViewHelper debugViewHelper;
    private boolean inErrorState;
    private TrackGroupArray lastSeenTrackGroupArray;

    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;

    // Fields used only for ad playback. The ads loader is loaded via reflection.

    private ILicenseProvider mLicenseProvider;

    private IVideoStreamDataSourceProvider mVideoStreamProvider;


    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shouldAutoPlay = true;
        clearResumePosition();


        mainHandler = new Handler();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.player_activity);
        View rootView = findViewById(R.id.root);
        rootView.setOnClickListener(this);
        debugRootView = findViewById(R.id.controls_root);
        debugTextView = findViewById(R.id.debug_text_view);
        retryButton = findViewById(R.id.retry_button);
        retryButton.setOnClickListener(this);

        simpleExoPlayerView = findViewById(R.id.player_view);
        simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.requestFocus();
    }

    @Override
    public void onNewIntent(Intent intent) {
        releasePlayer();
        shouldAutoPlay = true;
        clearResumePosition();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer();
        } else {
            showToast(R.string.storage_permission_denied);
            finish();
        }
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // See whether the player view wants to handle media or DPAD keys events.
        return simpleExoPlayerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    // OnClickListener methods

    @Override
    public void onClick(View view) {
        if (view == retryButton) {
            initializePlayer();
        } else if (view.getParent() == debugRootView) {
            MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                trackSelectionHelper.showSelectionDialog(
                        this, ((Button) view).getText(), mappedTrackInfo, (int) view.getTag());
            }
        }
    }

    // PlaybackControlView.VisibilityListener implementation

    @Override
    public void onVisibilityChange(int visibility) {
        debugRootView.setVisibility(visibility);
    }

    // Internal methods

    private void initializePlayer() {
        Intent intent = getIntent();

        Uri playingUri = intent.getData();
        boolean needNewPlayer = player == null;

        if (!needNewPlayer) {
            preparePlayer();
            return;
        }

        TrackSelection.Factory adaptiveTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
        trackSelectionHelper = new TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory);
        lastSeenTrackGroupArray = null;
        eventLogger = new EventLogger(trackSelector);

        String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
        String[] keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);
        int errorStringId = R.string.error_drm_unknown;
        if (Util.SDK_INT < 18) {
            errorStringId = R.string.error_drm_not_supported;
            showToast(errorStringId);

            return;

        }

        String videoId = getVideoId();
        File licenseFolder = new File(getExternalCacheDir(), "key_id");
        File videoCacheFolder = new File(getExternalCacheDir(), "offline_samples");

        MediaDrmCallback mediaDrmCallback = getMediaDrmCallback(drmLicenseUrl, keyRequestPropertiesArray);

        mLicenseProvider = new OnlineLicenseProvider(getHttpFactory(), mediaDrmCallback, playingUri);
        mVideoStreamProvider = new OnlineStreamProvider(mediaDrmCallback);


        if (OfflineUtil.isCacheNeeded(videoId)) {
            mLicenseProvider = new OfflineLicenseProvider(videoId, licenseFolder, mLicenseProvider);
            mVideoStreamProvider = new OfflineStreamProvider(videoId, playingUri, videoCacheFolder, mVideoStreamProvider);

        }

        buildDrmStream();


    }

    private void buildDrmStream() {

        mLicenseProvider.loadLicense()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<byte[]>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(byte[] keyId) {

                        onLicenseGenerated(keyId);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });


    }

    private void onLicenseGenerated(byte[] keyId) {

        try {
            DefaultDrmSessionManager<FrameworkMediaCrypto> sessionManager = mVideoStreamProvider.buildSessionManager(keyId);
            onDrmSessionManagerReady(sessionManager);

        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
        }
    }


    private void preparePlayer() {

        Intent intent = getIntent();
        Uri playingUri = intent.getData();
        String extn = intent.getStringExtra(EXTENSION_EXTRA);
        String action = intent.getAction();


        if (!ACTION_VIEW.equals(action)) {
            showToast(getString(R.string.unexpected_intent_action, action));
            return;
        }

        if (Util.maybeRequestReadExternalStoragePermission(this, playingUri)) {
            // The player will be reinitialized if the permission is granted.
            return;
        }

        MediaSource mediaSource = buildMediaSource(playingUri, mainHandler, eventLogger);

        boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
        if (haveResumePosition) {
            player.seekTo(resumeWindow, resumePosition);
        }

        player.prepare(mediaSource, !haveResumePosition, false);
        inErrorState = false;
        updateButtonVisibilities();
    }

    private void onDrmSessionManagerReady(DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {

        @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this,
                drmSessionManager, extensionRendererMode);

        releasePlayer();
        player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
        player.addListener(new PlayerEventListener());
        player.addListener(eventLogger);
        player.addMetadataOutput(eventLogger);
        player.addAudioDebugListener(eventLogger);
        player.addVideoDebugListener(eventLogger);

        simpleExoPlayerView.setPlayer(player);
        player.setPlayWhenReady(shouldAutoPlay);
        debugViewHelper = new DebugTextViewHelper(player, debugTextView);
        debugViewHelper.start();

        preparePlayer();
    }

    private MediaSource buildMediaSource(Uri uri, @Nullable Handler handler, @Nullable MediaSourceEventListener listener) {
        return buildDashMediaSource(uri, handler, listener);

    }

    private MediaSource buildDashMediaSource(Uri uri, @Nullable Handler handler,
                                             @Nullable MediaSourceEventListener listener) {


        return new DashMediaSource.Factory(
                new DefaultDashChunkSource.Factory(mVideoStreamProvider),
                mVideoStreamProvider)
                .createMediaSource(uri, handler, listener);

    }

    private HttpDataSource.Factory getHttpFactory() {
        return buildHttpDataSourceFactory(false);
    }


    private MediaDrmCallback getMediaDrmCallback(String licenseUrl, String[] keyRequestPropertiesArray) {
        return DrmUtil.getMediaDrmCallback(licenseUrl, getHttpFactory(), keyRequestPropertiesArray);
    }

    private String getVideoId() {
        return getIntent().getStringExtra(VIDEO_NAME);
    }


    private void releasePlayer() {
        if (player != null) {
            debugViewHelper.stop();
            debugViewHelper = null;
            shouldAutoPlay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            player = null;
            trackSelector = null;
            trackSelectionHelper = null;
            eventLogger = null;
        }
    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentWindowIndex();
        resumePosition = Math.max(0, player.getContentPosition());
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return ((DemoApplication) getApplication())
                .buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    // User controls

    private void updateButtonVisibilities() {
        debugRootView.removeAllViews();

        retryButton.setVisibility(inErrorState ? View.VISIBLE : View.GONE);
        debugRootView.addView(retryButton);

        if (player == null) {
            return;
        }

        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

        for (int i = 0; i < mappedTrackInfo.length; i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
            if (trackGroups.length != 0) {
                Button button = new Button(this);
                int label;
                switch (player.getRendererType(i)) {
                    case C.TRACK_TYPE_AUDIO:
                        label = R.string.audio;
                        break;
                    case C.TRACK_TYPE_VIDEO:
                        label = R.string.video;
                        break;
                    case C.TRACK_TYPE_TEXT:
                        label = R.string.text;
                        break;
                    default:
                        continue;
                }
                button.setText(label);
                button.setTag(i);
                button.setOnClickListener(this);
                debugRootView.addView(button, debugRootView.getChildCount() - 1);
            }
        }
    }

    private void showControls() {
        debugRootView.setVisibility(View.VISIBLE);
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private class PlayerEventListener extends Player.DefaultEventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                showControls();
            }
            updateButtonVisibilities();
        }

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            if (inErrorState) {
                // This will only occur if the user has performed a seek whilst in the error state. Update
                // the resume position so that if the user then retries, playback will resume from the
                // position to which they seeked.
                updateResumePosition();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            String errorString = null;
            e.printStackTrace();
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                if (cause instanceof DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    DecoderInitializationException decoderInitializationException =
                            (DecoderInitializationException) cause;
                    if (decoderInitializationException.decoderName == null) {
                        if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
                            errorString = getString(R.string.error_querying_decoders);
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString = getString(R.string.error_no_secure_decoder,
                                    decoderInitializationException.mimeType);
                        } else {
                            errorString = getString(R.string.error_no_decoder,
                                    decoderInitializationException.mimeType);
                        }
                    } else {
                        errorString = getString(R.string.error_instantiating_decoder,
                                decoderInitializationException.decoderName);
                    }
                }
            }
            if (errorString != null) {
                showToast(errorString);
            }
            inErrorState = true;
            if (isBehindLiveWindow(e)) {
                clearResumePosition();
                initializePlayer();
            } else {
                updateResumePosition();
                updateButtonVisibilities();
                showControls();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            updateButtonVisibilities();
            if (trackGroups != lastSeenTrackGroupArray) {
                MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_video);
                    }
                    if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_audio);
                    }
                }
                lastSeenTrackGroupArray = trackGroups;
            }
        }

    }

}
