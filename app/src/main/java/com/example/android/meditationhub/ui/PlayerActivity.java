package com.example.android.meditationhub.ui;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;

import com.example.android.meditationhub.BuildConfig;
import com.example.android.meditationhub.NotificationPanel;
import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityPlayerBinding;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.player.MediaPlayerHolder;
import com.example.android.meditationhub.player.PlaybackInfoListener;
import com.example.android.meditationhub.player.PlayerAdapter;
import com.example.android.meditationhub.util.Constants;
import com.example.android.meditationhub.util.MedUtils;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class PlayerActivity extends AppCompatActivity {

    public static boolean displayHours;
    private ActivityPlayerBinding playerBinding;
    private MeditationLocal selectedMed;
    private Bitmap coverArt;
    private Uri medUri;

    private PlayerAdapter playerAdapter;
    private boolean userIsSeeking = false;

    private MediaPlayerHolder mMediaPlayerHolder;
    private NotificationPanel notificationPanel;
    private NotificationPanel.NotificationReceiver notificationReceiver;

    //saving instances
    private int position;
    public static boolean isPlaying;
    private static final String PLAYBACK_POS = "playback position";
    private static final String IS_PLAYING = "is playing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playerBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(PLAYBACK_POS)) {
                position = savedInstanceState.getInt(PLAYBACK_POS);
            }
            if (savedInstanceState.containsKey(IS_PLAYING)) {
                isPlaying = savedInstanceState.getBoolean(IS_PLAYING);
            }
        }

        //retrieve information passed with the intent
        selectedMed = getIntent().getParcelableExtra(Constants.SELECTED_MED);
        coverArt = getIntent().getParcelableExtra(Constants.ART);
        medUri = getIntent().getParcelableExtra(Constants.URI);
        Timber.v("Audio file: " + medUri);

        //setup the coverArt
        MedUtils.displayCoverArt(coverArt, playerBinding.thumbIv);

        initializeUI();
        initializePlaybackController();
        initializeSeekbar();
        initializeSession();
        Timber.d("onCreate: finished");
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
        mediaSession.setCallback(new MySessionCallback());

        // Start the Media Session since the activity is active.
        mediaSession.setActive(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PLAYBACK_POS, mMediaPlayerHolder.getPosition());
        outState.putBoolean(IS_PLAYING, isPlaying);
    }

    @Override
    protected void onStart() {
        super.onStart();
        playerAdapter.loadMedia(medUri);
        if (isPlaying) {
            playerAdapter.play();
        }
        Timber.d("onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaPlayerHolder.setPlaybackInfoListener(null);
        position = mMediaPlayerHolder.getPosition();
        playerAdapter.release();
        Timber.d("onStop: release MediaPlayer");
    }

    private void initializeUI() {

        playerBinding.titleTv.setText(selectedMed.getTitle());
        playerBinding.subtitleTv.setText(selectedMed.getSubtitle());

        playerBinding.pauseBt.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pausePlayback();
                    }
                });
        playerBinding.playBt.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startPlayback();
                        notificationPanel = new NotificationPanel(PlayerActivity.this, selectedMed.getTitle());
                        notificationReceiver = new NotificationPanel.NotificationReceiver(PlayerActivity.this);
                        registerReceiver(notificationReceiver, notificationReceiver.getIntentFilter());

                    }
                });

    }

    public void startPlayback() {
        playerAdapter.play();
        isPlaying = true;
    }

    private void restPlayback() {
        playerAdapter.reset();
        position = 0;
        notificationPanel.notificationCancel();

    }

    public void pausePlayback() {
        playerAdapter.pause();
        isPlaying = false;
    }

    private void initializePlaybackController() {
        mMediaPlayerHolder = new MediaPlayerHolder(this);
        Timber.d("initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        playerAdapter = mMediaPlayerHolder;
        if (position != 0) {
            mMediaPlayerHolder.setPosition(position);
        }
        Timber.d("initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        playerBinding.progressSb.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        userIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        userIsSeeking = false;
                        playerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            playerBinding.progressSb.setMax(duration);

            String displayDuration = String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
            playerBinding.durationTv.setText(displayDuration);
            Timber.d("Playback duration: " + displayDuration);
        }

        @Override
        public void onPositionChanged(int position) {
            if (!userIsSeeking) {
                playerBinding.progressSb.setProgress(position);

                String displayPosition = MedUtils.getDisplayTime(position, displayHours, MedUtils.CONVERT_POSITION);
                playerBinding.positionTv.setText(String.valueOf(displayPosition));
                Timber.d("setPlaybackPosition: setProgress " + displayPosition);
            }
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            Timber.v("onStateChanged " + stateToString);
        }

        @Override
        public void onPlaybackCompleted() {
        }

    }

    /**
     * Media Session Callbacks, enabling all external clients to control the player.
     */
    class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            isPlaying = true;
        }

        @Override
        public void onPause() {
            isPlaying = false;
            mMediaPlayerHolder.setPosition(position);
        }

        @Override
        public void onSkipToPrevious() {
            playerAdapter.seekTo(0);
        }
    }
}
