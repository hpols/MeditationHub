package com.example.android.meditationhub.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import androidx.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;

import com.example.android.meditationhub.BuildConfig;
import com.example.android.meditationhub.NotificationPanel;
import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityPlayerBinding;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.player.MediaPlayerService;
import com.example.android.meditationhub.player.PlaybackInfoListener;
import com.example.android.meditationhub.player.PlayerAdapter;
import com.example.android.meditationhub.player.PlayerHolder;
import com.example.android.meditationhub.player.PlayerService;
import com.example.android.meditationhub.util.Constants;
import com.example.android.meditationhub.util.MedUtils;

import timber.log.Timber;

public class PlayerActivity extends AppCompatActivity {

    public static boolean displayHours;
    private ActivityPlayerBinding playerBinding;
    private MeditationLocal selectedMed;
    private Bitmap coverArt;
    private Uri medUri;

    private int position;
    private MediaPlayerService mediaPlayerService;
    private boolean servcieIsBound;

    private PlayerAdapter playerAdapter;
    private boolean userIsSeeking = false;

    private PlayerHolder playerHolder;
    private NotificationPanel notificationPanel;
    private NotificationPanel.NotificationReceiver notificationReceiver;

    //saving instances
    public static boolean isPlaying;
    private static final String IS_PLAYING = "is playing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playerBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        //retrieve information from the SaveInstance or Intent depending on the flow
        if (savedInstanceState != null) {
            position = mediaPlayerService.getPosition();
            isPlaying = (boolean) getSavedInstanceState(savedInstanceState, IS_PLAYING,
                    Constants.SAVED_BOO);
            selectedMed = (MeditationLocal) getSavedInstanceState(savedInstanceState,
                    Constants.SELECTED_MED, Constants.SAVED_PARCEL);
            coverArt = (Bitmap) getSavedInstanceState(savedInstanceState, Constants.ART,
                    Constants.SAVED_PARCEL);
            medUri = (Uri) getSavedInstanceState(savedInstanceState, Constants.URI,
                    Constants.SAVED_PARCEL);
        } else {
            //retrieve information passed with the intent
            selectedMed = getIntent().getParcelableExtra(Constants.SELECTED_MED);
            medUri = getIntent().getParcelableExtra(Constants.URI);

            coverArt = MedUtils.getCoverArt(medUri, this);
        }

        //setup the coverArt
        MedUtils.displayCoverArt(coverArt, playerBinding.thumbIv);

        //setup the player
        initializeUI();
        //initializePlaybackController();
        //initializeSeekbar();
        //initializeSession();
        Timber.d("onCreate: finished");
    }

    /**
     * retrieve information saved before activity was destroyed.
     *
     * @param savedInstanceState is the bundle holding the saved information
     * @param key                indicates what information was saved
     * @param type               is the type of variable to be retrieved
     */
    private Object getSavedInstanceState(Bundle savedInstanceState, String key, int type) {
        if (savedInstanceState.containsKey(key)) {
            switch (type) {
                case Constants.SAVED_INT:
                    return savedInstanceState.getInt(key);
                case Constants.SAVED_BOO:
                    return savedInstanceState.getBoolean(key);
                case Constants.SAVED_PARCEL:
                    return savedInstanceState.getParcelable(key);
            }
        }
        return null;
    }

    /**
     * Initializes the Media Session to be enabled with media buttons, transport controls, callbacks
     * and media controller.
     */
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
        outState.putInt(Constants.PLAYBACK_POS, mediaPlayerService.getPosition());
        outState.putBoolean(IS_PLAYING, isPlaying);
        outState.putParcelable(Constants.SELECTED_MED, selectedMed);
        outState.putParcelable(Constants.ART, coverArt);
        outState.putParcelable(Constants.URI, medUri);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        final int delay = MedUtils.getDelay(this);
//        if (delay != 0 && position == 0 && isPlaying) { //add delay audio if requested in settings
//            Uri delayAudio = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
//                    File.pathSeparator + File.separator + getPackageName() + R.raw.forest_murmur);
//            playerAdapter.loadMedia(delayAudio);
//
//            //start randomly so that the background sound is not always the same
//            int randomStart = new Random().nextInt(playerAdapter.getDuration() - delay);
//            playerAdapter.seekTo(randomStart);
//
//            //play for the set time and then move on to the meditation
//            new Handler().postDelayed(new Runnable() {
//                public void run() {
//                    playerAdapter.release();
//                    playerAdapter.loadMedia(medUri);
//                    startService(new Intent(PlayerActivity.this, PlayerService.class));
//                    //playerAdapter.play();
//                    setupReceiver();
//                }
//            }, delay * 1000);   //delay * 1000 (for mili)
//        } else {
//            Intent mediaLoader = new Intent(this, PlayerService.class);
//            mediaLoader.putExtra(Constants.URI, medUri);
//            mediaLoader.putExtra(Constants.SERVICE_ID, Constants.LOADER_ID);
//            startService(mediaLoader);
//            playerAdapter.loadMedia(medUri);
//            if (isPlaying) {
//                startService(new Intent(this, PlayerService.class));
//                setupReceiver();
//            }
//        }
        Timber.d("onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
//        playerHolder.setPlaybackInfoListener(null);
//        playerHolder.handleAlerts(PlayerHolder.TURN_ON_ALL_ALERTS);
//        position = playerHolder.getPosition();
//        stopService(new Intent(this, PlayerService.class));
//        //playerAdapter.release();
//        //unregisterReceiver(notificationReceiver);
//        Timber.d("onStop: release MediaPlayer");
    }

    private void initializeUI() {
        playerBinding.titleTv.setText(selectedMed.getTitle());
        playerBinding.subtitleTv.setText(selectedMed.getSubtitle());

//        playerBinding.playPauseBt.setImageResource(MedUtils.getPlaybackControl());
//        playerBinding.stopBt.setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        resetPlayback();
//                    }
//                });
//        playerBinding.playPauseBt.setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        final int lState = MediaPlayerService.getState();
//                        if (lState == Constants.STATE_SERVICE.STATE_NOT_INIT) {
//
//                            Intent startIntent = new Intent(view.getContext(), MediaPlayerService.class);
//                            startIntent.setAction(Constants.START_ACTION);
//                            startIntent.putExtra(Constants.URI, medUri);
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                startForegroundService(startIntent);
//                            } else {
//                                startService(startIntent);
//                            }
//
//
//                            Intent serviceBindIntent =  new Intent(PlayerActivity.this, MediaPlayerService.class);
//                            bindService(serviceBindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//                        } else if (lState == Constants.STATE_SERVICE.STATE_PREPARE ||
//                                lState == Constants.STATE_SERVICE.STATE_PLAY) {
//                            Intent lPauseIntent = new Intent(view.getContext(), MediaPlayerService.class);
//                            lPauseIntent.setAction(Constants.PAUSE_ACTION);
//                            PendingIntent lPendingPauseIntent = PendingIntent.getService(view.getContext(),
//                                    0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//                            try {
//                                lPendingPauseIntent.send();
//                            } catch (PendingIntent.CanceledException e) {
//                                e.printStackTrace();
//                            }
//                        } else if (lState == Constants.STATE_SERVICE.STATE_PAUSE) {
//
//                            Intent lPauseIntent = new Intent(view.getContext(), MediaPlayerService.class);
//                            lPauseIntent.setAction(Constants.PLAY_ACTION);
//                            PendingIntent lPendingPauseIntent = PendingIntent.getService(view.getContext(),
//                                    0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//                            try {
//                                lPendingPauseIntent.send();
//                            } catch (PendingIntent.CanceledException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        playerBinding.playPauseBt.setImageResource(MedUtils.getPlaybackControl());
//
////                        if (isPlaying) {
////                            pausePlayback();
////                        } else {
////                            startPlayback();
////                            setupReceiver();
////                        }
//                    }
//                });
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder iBinder) {
            Log.d("PlayerActivity", "ServiceConnection: connected to service.");
            // We've bound to MyService, cast the IBinder and get MyBinder instance
            MediaPlayerService.MyBinder binder = (MediaPlayerService.MyBinder) iBinder;
            mediaPlayerService = binder.getService();
            servcieIsBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("PlayerActivity", "ServiceConnection: disconnected from service.");
            servcieIsBound = false;
        }
    };

    private void setupReceiver() {
        notificationPanel = new NotificationPanel(PlayerActivity.this,
                selectedMed.getTitle(), medUri);
        notificationReceiver = new NotificationPanel.
                NotificationReceiver(PlayerActivity.this);
        registerReceiver(notificationReceiver,
                notificationReceiver.getIntentFilter());
    }

    public void startPlayback() {
        startService(new Intent(this, PlayerService.class));
        //playerAdapter.play();
        isPlaying = true;
    }

    private void resetPlayback() {
        stopService(new Intent(this, PlayerService.class));
        //playerAdapter.reset();
        position = 0;
        notificationPanel.notificationCancel();

    }

    public void pausePlayback() {
        playerAdapter.pause();
        isPlaying = false;
    }

    private void initializePlaybackController() {
        playerHolder = new PlayerHolder(this, this);
        Timber.d("initializePlaybackController: created PlayerHolder");
        playerHolder.setPlaybackInfoListener(new PlaybackListener());
        playerAdapter = playerHolder;
        if (position != 0) {
            playerHolder.setPosition(position);
        }
        Timber.d("initializePlaybackController: PlayerHolder progress callback set");
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

            String displayDuration = MedUtils.getDisplayTime(duration, false, Constants.CONVERT_DURATION);
            playerBinding.durationTv.setText(displayDuration);
            Timber.d("Playback duration: " + displayDuration);
        }

        @Override
        public void onPositionChanged(int position) {
            if (!userIsSeeking) {
                playerBinding.progressSb.setProgress(position);
                String displayPosition = MedUtils.getDisplayTime(position, displayHours, Constants.CONVERT_POSITION);
                playerBinding.positionTv.setText(displayPosition);
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
            playerHolder.setPosition(position);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopService(new Intent(this, PlayerService.class));
        playerAdapter.release();
        finish();
    }
}
