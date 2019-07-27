package com.example.android.meditationhub.ui;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityPlayerBinding;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.util.Constants;
import com.example.android.meditationhub.util.MedUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import butterknife.BindView;

public class PlayerActivity extends AppCompatActivity implements Player.EventListener {

    private ActivityPlayerBinding playerBinding;
    private MeditationLocal thisMed;
    private Bitmap coverArt;


    private static final String TAG = PlayerActivity.class.getSimpleName();

    //for the player
    private SimpleExoPlayer player;
    private MediaSource mediaSource;

    //keep track of playback state and position
    private boolean playWhenReady;
    private long playerCurrentPosition;

    //For the Video player
    @BindView(R.id.exo_play)
    PlayerView exoPlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playerBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);

        //retrieve information passed with the intent
        thisMed = getIntent().getParcelableExtra(Constants.THIS_MED);
        coverArt = getIntent().getParcelableExtra(Constants.ART);

        playerCurrentPosition = C.TIME_UNSET;
        //any player information stored before orientation change or similar?
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.PLAYER_READY)) {
                playWhenReady = savedInstanceState.getBoolean(Constants.PLAYER_READY);
            }
            if (savedInstanceState.containsKey(Constants.PLAYER_POSITION)) {
                playerCurrentPosition = savedInstanceState.getLong(Constants.PLAYER_POSITION);
            }

        }
        //setup the coverArt
        MedUtils.displayCoverArt(coverArt, playerBinding.thumbIv);

        // Initialize the Media Session to display the video.
        initializeSession();

        // Initialize the player.
        setupPlayer();

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(Constants.PLAYER_READY, playWhenReady);
        outState.putLong(Constants.PLAYER_POSITION, playerCurrentPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playerCurrentPosition = player.getCurrentPosition();
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.setPlayWhenReady(playWhenReady);
            player.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(playWhenReady);
            setupPlayer();
        }
    }

    //Initializes the Media Session to be enabled with media buttons, transport controls, callbacks
    //and media controller.
    private void initializeSession() {

        MediaSessionCompat mediaSession = new MediaSessionCompat(this, getLocalClassName());

        // Enable mediaButton ~ and transportControls callbacks
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player.
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback has methods that handle callbacks from a media controller.
        mediaSession.setCallback(new PlayerActivity.MySessionCallback());

        // Start the Media Session since the activity is active.
        mediaSession.setActive(true);
    }

    private void setupPlayer() {
        if (player == null) {

            //setup the media source
            mediaSource = new ExtractorMediaSource.Factory(new DefaultDataSourceFactory(
                    this, Util.getUserAgent(this, "ExoPlay"),
                    new DefaultBandwidthMeter())).createMediaSource(Uri.parse(thisMed.getStorage()));

            //setup the player
            player = ExoPlayerFactory.newSimpleInstance(this,
                    new DefaultTrackSelector(new AdaptiveTrackSelection.
                            Factory(new DefaultBandwidthMeter())));

            player.addListener(this);
            player.prepare(mediaSource);
            if (playerCurrentPosition != C.TIME_UNSET) {
                player.seekTo(playerCurrentPosition);
            }
            player.setPlayWhenReady(playWhenReady);

            Log.v("TEST", "playing state : " + player.getPlaybackState());
        }

//        playerBinding.exoPlay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                player.setPlayWhenReady(true);
//            }
//        });
//
//        playerBinding.exoStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                player.setPlayWhenReady(false);
//            }
//        });
    }

    //Release ExoPlayer when it is no longer needed.
    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    //Media Session Callbacks, where all external clients control the player.
    class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            player.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            player.setPlayWhenReady(false);
            playerCurrentPosition = player.getCurrentPosition();
        }

        @Override
        public void onSkipToPrevious() {
            player.seekTo(0);
        }
    }

    // ExoPlayer Event Listeners

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    /**
     * Method that is called when the ExoPlayer state changes. Used to update the MediaSession
     * PlayBackState to keep in sync, and post the media notification.
     *
     * @param playWhenReady true if ExoPlayer is playing, false if it's paused.
     * @param playbackState int describing the state of ExoPlayer. Can be STATE_READY, STATE_IDLE,
     *                      STATE_BUFFERING, or STATE_ENDED.
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        if (playbackState == Player.STATE_READY) {
            Log.i("TEST", "ExoPlayer State is: READY");
        } else if (playbackState == Player.STATE_BUFFERING) {
            Log.i("TEST", "ExoPlayer State is: BUFFERING");
        } else if (playbackState == Player.STATE_ENDED) {
            Log.i("TEST", "ExoPlayer State is: ENDED");
        } else if (playbackState == Player.STATE_IDLE) {
            Log.i("TEST", "ExoPlayer State is: IDLE");
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }
}
