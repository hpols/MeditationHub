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

        //retrieve information from the SaveInstance or Intent depending on the flow
        if (savedInstanceState != null) {
            position =  mediaPlayerService.getPosition();
            isPlaying = (boolean) getSavedInstanceState(savedInstanceState, Constants.IS_PLAYING,
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
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    stopService(mediaPlayerServiceInt);
                }
                return true;
            }
        });
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

            Log.v(TAG, key + "retrieved instant state");
        }
        return null;
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
        unbindService(mediaPlayerConnection);
        serviceIsBound = false;
        Log.v(TAG,"Service unbound");
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
            Log.v(TAG,"no Service to bind");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Constants.IS_PLAYING, isPlaying);
        outState.putParcelable(Constants.SELECTED_MED, selectedMed);
        outState.putParcelable(Constants.ART, coverArt);
        outState.putParcelable(Constants.URI, medUri);

        Log.v(TAG,"all outstates saved");
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
