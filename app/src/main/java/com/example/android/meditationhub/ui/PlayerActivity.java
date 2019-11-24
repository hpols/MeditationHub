package com.example.android.meditationhub.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityPlayerBinding;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.player.MediaPlayerService;
import com.example.android.meditationhub.util.Constants;
import com.example.android.meditationhub.util.MedUtils;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding playBinding;
    private static final String TAG = PlayerActivity.class.getSimpleName();

    private MeditationLocal selectedMed;
    private Bitmap coverArt;
    private Uri medUri;

    private MediaPlayerService mediaPlayerService;
    private boolean serviceIsBound;
    private int delay;

    private IntentFilter intentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);

        if (MediaPlayerService.getState() == Constants.STATE_NOT_INIT) {
            if (savedInstanceState != null) {
                if (savedInstanceState.containsKey(Constants.SELECTED_MED)) {
                    selectedMed = savedInstanceState.getParcelable(Constants.SELECTED_MED);
                }
                if (savedInstanceState.containsKey(Constants.ART)) {
                    coverArt = savedInstanceState.getParcelable(Constants.ART);
                }
            } else {
                //retrieve information passed with the intent
                selectedMed = getIntent().getParcelableExtra(Constants.SELECTED_MED);
                medUri = getIntent().getParcelableExtra(Constants.URI);
                coverArt = MedUtils.getCoverArt(medUri, this);
                //if user has clicked play button to transition
                if (getIntent().getBooleanExtra(Constants.IS_PLAYING, false)) {
                    startMediaPlayerService();
                }
            }

            delay = MedUtils.getDelay(PlayerActivity.this);
        }

        initializeUI(); //setup control buttons

        intentFilter = new IntentFilter(); //setup intents for broadcastreceiver
        intentFilter.addAction(Constants.PLAYER_CHANGE);
        intentFilter.addAction(Constants.PLAYER_DELAY);
    }

    private void initializeUI() {
        //setup the coverArt and titles
        MedUtils.displayCoverArt(coverArt, playBinding.thumbIv);
        if (coverArt != null) {
            playBinding.titleTv.setVisibility(View.INVISIBLE);
            playBinding.subtitleTv.setVisibility(View.INVISIBLE);
        } else {
            playBinding.titleTv.setVisibility(View.VISIBLE);
            playBinding.subtitleTv.setVisibility(View.VISIBLE);
            playBinding.titleTv.setText(selectedMed.getTitle());
            playBinding.subtitleTv.setText(selectedMed.getSubtitle());
        }

        playBinding.playPauseBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (MediaPlayerService.getState()) {
                    case Constants.STATE_PLAY:
                        mediaPlayerService.pauseAction();
                        break;
                    case Constants.STATE_PAUSE:
                        mediaPlayerService.playAction();
                        break;
                    case Constants.STATE_NOT_INIT:
                        startMediaPlayerService();
                        break;
                }
            }
        });

        playBinding.stopBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerService.stopAction();
            }
        });
    }

    private void setupDelayUI(boolean show) {
        if (show) {
            playBinding.delayTv.setVisibility(View.VISIBLE);
            playBinding.playPauseBt.setVisibility(View.INVISIBLE);
            playBinding.durationTv.setVisibility(View.INVISIBLE);
            playBinding.positionTv.setVisibility(View.INVISIBLE);
            playBinding.progressSb.setVisibility(View.INVISIBLE);
        } else {
            playBinding.delayTv.setVisibility(View.GONE);
            playBinding.durationTv.setVisibility(View.VISIBLE);
            playBinding.positionTv.setVisibility(View.VISIBLE);
            playBinding.progressSb.setVisibility(View.VISIBLE);
            playBinding.playPauseBt.setVisibility(View.VISIBLE);
        }
    }

    private void updatePlayerControls() {
        switch (MediaPlayerService.getState()) {
            case Constants.STATE_PAUSE:
                playBinding.playPauseBt.setImageResource(R.drawable.ic_play);
                playBinding.stopBt.setVisibility(View.VISIBLE);
                break;
            case Constants.STATE_PLAY:
            case Constants.STATE_PREPARE:
                playBinding.playPauseBt.setImageResource(R.drawable.ic_pause);
                playBinding.stopBt.setVisibility(View.VISIBLE);
                break;
            case Constants.STATE_NOT_INIT:
                playBinding.playPauseBt.setImageResource(R.drawable.ic_play);
                playBinding.stopBt.setVisibility(View.GONE);
                break;
        }
    }

    private void startMediaPlayerService() {
        Intent mediaPlayerServiceInt = new Intent(this, MediaPlayerService.class);
        mediaPlayerServiceInt.setAction(Constants.START_ACTION);
        mediaPlayerServiceInt.putExtra(Constants.URI, medUri);
        mediaPlayerServiceInt.putExtra(Constants.TITLE, selectedMed.getTitle());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mediaPlayerServiceInt);
        } else {
            startService(mediaPlayerServiceInt);
        }
        bindMediaPlayerService();

    }

    //stay up to date with playback controls in the UI
    private BroadcastReceiver updatePlayBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Constants.PLAYER_CHANGE)) {
                updatePlayerControls();
            }
            if (intent.getAction() != null && intent.getAction().equals(Constants.PLAYER_DELAY)) {
                setupDelayUI(true);
                new CountDownTimer(delay, 1000) {

                    public void onTick(long millisUntilFinished) {
                        final String delayText = getString((R.string.delay_message),
                                String.valueOf(millisUntilFinished / 1000));
                        playBinding.delayTv.setText(delayText);
                    }

                    public void onFinish() {
                        setupDelayUI(false);
                    }

                }.start();
            }
        }
    };

    //monitor state of the service
    private ServiceConnection mediaPlayerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaPlayerService = ((MediaPlayerService.MyBinder) service).getService();

            setControlsVisible(true);
            mediaPlayerService.setUIControls(playBinding.progressSb, playBinding.positionTv,
                    playBinding.durationTv);
            serviceIsBound = true;

            if (mediaPlayerService.isBindIsOngoing()) {
                selectedMed = mediaPlayerService.getSelectedMed();
                coverArt = mediaPlayerService.getCoverArt();

                updatePlayerControls();

                initializeUI(); //setup control buttons
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mediaPlayerService = null;
            serviceIsBound = false;
        }
    };

    private void setControlsVisible(boolean setVisible) {
        if (setVisible) {
            playBinding.progressSb.setVisibility(View.VISIBLE);
            playBinding.positionTv.setVisibility(View.VISIBLE);
            playBinding.durationTv.setVisibility(View.VISIBLE);
        } else {
            playBinding.progressSb.setVisibility(View.INVISIBLE);
            playBinding.positionTv.setVisibility(View.INVISIBLE);
            playBinding.durationTv.setVisibility(View.INVISIBLE);
        }

    }

    //unbind service so the audio continues to play
    private void unbindMediaPlayerService() {
        mediaPlayerService.setCoverArt(coverArt);
        mediaPlayerService.setSelectedMed(selectedMed);
        mediaPlayerService.setBindIsOngoing(true);

        unbindService(mediaPlayerConnection);
        serviceIsBound = false;
        setControlsVisible(false);

        Log.v(TAG, "Service unbound");
    }

    /**
     * bind service so the display can follow the audio playback
     */
    private void bindMediaPlayerService() {
        if (!serviceIsBound) {
            Intent bindInt = new Intent(this, MediaPlayerService.class);
            serviceIsBound = bindService(bindInt, mediaPlayerConnection, Context.BIND_AUTO_CREATE);

            Log.v(TAG, "Service bound");
        } else {
            Log.v(TAG, "no Service to bind");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (MediaPlayerService.getState() != Constants.STATE_NOT_INIT) {
            bindMediaPlayerService();
        }

        registerReceiver(updatePlayBtReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaPlayerService.getState() != Constants.STATE_NOT_INIT) {
            unbindMediaPlayerService();
            mediaPlayerService.setActivityIsDestroyed(true);
        }
        unregisterReceiver(updatePlayBtReceiver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (MediaPlayerService.getState() == Constants.STATE_NOT_INIT) {
            outState.putParcelable(Constants.SELECTED_MED, selectedMed);
            outState.putParcelable(Constants.ART, coverArt);
        } else {
            outState.clear(); //making sure nothing hangs around we don't need from previous saves.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}