package com.example.android.meditationhub.ui;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityPlayerBinding;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.player.MediaPlayerService;
import com.example.android.meditationhub.util.Constants;
import com.example.android.meditationhub.util.MedUtils;
import com.swifty.animateplaybutton.AnimatePlayButton;

public class PlayActivity extends AppCompatActivity {

    private ActivityPlayerBinding playBinding;
    private static final String TAG = PlayActivity.class.getSimpleName();

    private MeditationLocal selectedMed;
    private Bitmap coverArt;
    private Uri medUri;

    private int position;
    public static boolean isPlaying;

    private Intent mediaPlayerServiceInt;
    private MediaPlayerService mediaPlayerService;
    private boolean serviceIsBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);

        //retrieve information from the Service or Intent depending on the flow
        if (MediaPlayerService.getState() != Constants.STATE_NOT_INIT) {
            position = mediaPlayerService.getPosition();
            selectedMed = mediaPlayerService.getSelectedMed();
            coverArt = mediaPlayerService.getCoverArt();
            //we don't need to retrieve URI as it is only handled in the Service
        } else {
            //retrieve information passed with the intent
            selectedMed = getIntent().getParcelableExtra(Constants.SELECTED_MED);
            medUri = getIntent().getParcelableExtra(Constants.URI);
            coverArt = MedUtils.getCoverArt(medUri, this);
        }
        initializeUI();
    }

    private void initializeUI() {
        //setup the coverArt and titles
        MedUtils.displayCoverArt(coverArt, playBinding.thumbIv);
        playBinding.titleTv.setText(selectedMed.getTitle());
        playBinding.subtitleTv.setText(selectedMed.getSubtitle());

        playBinding.playbackControlBt.setPlayListener(new AnimatePlayButton.OnButtonsListener() {
            @Override
            public boolean onPlayClick(View view) {
                mediaPlayerServiceInt = new Intent(PlayActivity.this, MediaPlayerService.class);
                mediaPlayerServiceInt.setAction(Constants.START_ACTION);
                mediaPlayerServiceInt.putExtra(Constants.URI, medUri);
                mediaPlayerServiceInt.putExtra(Constants.TITLE, selectedMed.getTitle());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(mediaPlayerServiceInt);
                } else {
                    startService(mediaPlayerServiceInt);
                }
                bindMediaPlayerService();
                return true;
            }

            @Override
            public boolean onPauseClick(View view) {
                Intent pausePlayback = new Intent(PlayActivity.this, MediaPlayerService.class);
                pausePlayback.setAction(Constants.PAUSE_ACTION);
                PendingIntent pendingPausePlayback = PendingIntent.getService(PlayActivity.this,
                        0, pausePlayback, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    pendingPausePlayback.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean onResumeClick(View view) {
                Intent resumePlayback = new Intent(PlayActivity.this, MediaPlayerService.class);
                resumePlayback.setAction(Constants.PLAY_ACTION);
                PendingIntent pendingResumePlayback = PendingIntent.getService(PlayActivity.this,
                        0, resumePlayback, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    pendingResumePlayback.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean onStopClick(View view) {
                stopService(mediaPlayerServiceInt);
                return true;
            }
        });
    }

    //monitor state of the service
    private ServiceConnection mediaPlayerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaPlayerService = ((MediaPlayerService.MyBinder) service).getService();
            serviceIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mediaPlayerService = null;
            serviceIsBound = false;
        }
    };

    //unbind service so the audio continues to play
    private void unbindMediaPlayerService() {
        mediaPlayerService.setCoverArt(coverArt);
        mediaPlayerService.setSelectedMed(selectedMed);

        unbindService(mediaPlayerConnection);
        serviceIsBound = false;
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaPlayerService.getState() != Constants.STATE_NOT_INIT) {
            unbindMediaPlayerService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
