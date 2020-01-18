package com.example.android.meditationhub.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.ui.MainActivity;
import com.example.android.meditationhub.ui.PlayerActivity;
import com.example.android.meditationhub.util.Constants;
import com.example.android.meditationhub.util.MedUtils;

import java.util.concurrent.TimeUnit;

/**
 * Foreground service based on: https://github.com/DimaKoz/Android-Foreground-Service-Example
 */
public class MediaPlayerService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = MediaPlayerService.class.getSimpleName();

    private final static String FOREGROUND_CHANNEL_ID = Constants.NOTIFICATION_CHANNEL_ID;
    static private int stateService = Constants.STATE_NOT_INIT;
    private MeditationLocal selectedMed;

    private Bitmap coverArt;
    private Uri medUri;
    private String medTitle;
    private final Object lock = new Object();
    private MediaPlayer mediaPlayer;
    private AudioManager audioMan;
    private NotificationManager notMan;
    private PowerManager.WakeLock wakeLock;
    private RemoteViews remoteViews;

    private PendingIntent notPender, pausePender, resumePender, stopPender;
    private int position, delay;
    private long duration;
    private boolean bindIsOngoing, activityIsDestroyed, playbackIsDelayed;

    private SeekBar seekBar;
    private TextView currentPositionTv, totalDurationTv;
    private final int interval = 1000;
    private boolean isLongerThanHour;

    private static final boolean TURN_OFF_ALL_ALERTS = true;
    private static final boolean TURN_ON_ALL_ALERTS = false;

    // Async thread to update progress bar every second
    private final Runnable progressRunner = new Runnable() {
        @Override
        public void run() {
            if (seekBar != null) {
                if (activityIsDestroyed) {
                    seekBar.setMax((int) duration);
                    totalDurationTv.setText(MedUtils.getDisplayTime(duration, false));
                }
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                currentPositionTv.setText(MedUtils.getDisplayTime(mediaPlayer.getCurrentPosition(), isLongerThanHour));

                if (mediaPlayer.isPlaying()) {
                    seekBar.postDelayed(progressRunner, interval);
                }
            }
        }
    };

    public MediaPlayerService() {
    }

    public static int getState() {
        return stateService;
    }

    // Binder given to Activity
    private final IBinder binder = new MyBinder();

    /**
     * Class used for the client Binder. The Binder object is responsible for returning an instance
     * of {@link MediaPlayerService} to the client.
     */
    public class MyBinder extends Binder {
        public MediaPlayerService getService() {
            // Return this instance of MyService so clients can call public methods
            return MediaPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate()");
        stateService = Constants.STATE_NOT_INIT;
        notMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //create the necessary pendingIntents.
        notPender = createPender(Constants.MAIN_ACTION, Intent.FLAG_ACTIVITY_NEW_TASK);
        pausePender = createPender(Constants.PAUSE_ACTION, 0);
        resumePender = createPender(Constants.PLAY_ACTION, 0);
        stopPender = createPender(Constants.STOP_ACTION, 0);

        remoteViews = new RemoteViews(getPackageName(), R.layout.play_notification);

        delay = MedUtils.getDelay(this);
        if (delay != 0) {
            playbackIsDelayed = true;
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (intent == null || intent.getAction() == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        switch (intent.getAction()) {
            case Constants.START_ACTION:
                Log.i(TAG, "Received start Intent ");
                if (intent.getExtras() != null) {
                    selectedMed = (MeditationLocal) intent.getExtras().get(Constants.SELECTED_MED);
                    medUri = (Uri) intent.getExtras().get(Constants.URI);
                    medTitle = (String) intent.getExtras().get(Constants.TITLE);
                    coverArt = (Bitmap) intent.getExtras().get(Constants.ART);
                }
                stateService = Constants.STATE_PREPARE;
                startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                destroyPlayer();
                //Request audio focus
                if (!requestAudioFocus()) {
                    //Could not gain focus
                    stopSelf();
                    Toast.makeText(this, "Could not gain audio focus. Please try again",
                            Toast.LENGTH_SHORT).show();
                } else {
                    initMediaSession();
                    initPlayer();
                    play();
                    handleAlerts(TURN_OFF_ALL_ALERTS);
                }
                break;

            case Constants.PAUSE_ACTION:
                pauseAction();
                break;

            case Constants.PLAY_ACTION:
                playAction();
                break;

            case Constants.STOP_ACTION:
            default:
                stopAction();
                if (delay != 0) {
                    playbackIsDelayed = true;
                }
                break;
        }
        return START_NOT_STICKY;
    }

    private void broadcastPlayerChange(String action) {
        Intent broadCastPlayerChange = new Intent();
        switch (action) {
            case Constants.PLAYER_CHANGE:
                broadCastPlayerChange.setAction(Constants.PLAYER_CHANGE);
                sendBroadcast(broadCastPlayerChange);
                break;
            case Constants.PLAYER_DELAY:
                broadCastPlayerChange.setAction(Constants.PLAYER_DELAY);
                break;
        }
        sendBroadcast(broadCastPlayerChange);
    }

    public void setUIControls(SeekBar progressSb, TextView positionTv, TextView durationTv) {
        seekBar = progressSb;
        currentPositionTv = positionTv;
        totalDurationTv = durationTv;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Change current position of the song playback
                    mediaPlayer.seekTo(progress);
                }

                // Update our textView to display the correct number of second in format 0:00
                currentPositionTv.setText(MedUtils.getDisplayTime(mediaPlayer.getCurrentPosition(), isLongerThanHour));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void resetUI() {
        seekBar.setProgress(0);
        currentPositionTv.setText(MedUtils.getDisplayTime(0, isLongerThanHour));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        destroyPlayer();
        stateService = Constants.STATE_NOT_INIT;
        removeAudioFocus();
    }

    private void destroyPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
                Log.d(TAG, "Player destroyed");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mediaPlayer = null;
            }
        }
        unlockCPU();
        if (seekBar != null) {
            seekBar.removeCallbacks(progressRunner);
        }

    }

    public boolean onError(MediaPlayer mp, int what, int extra) {

        Log.d(TAG, "Player onError() what:" + what);
        destroyPlayer();
        notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
        stateService = Constants.STATE_PAUSE;
        return false;
    }

    private void initPlayer() {
        mediaPlayer = new MediaPlayer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            builder.setUsage(AudioAttributes.USAGE_MEDIA);
            builder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
            AudioAttributes attributes = builder.build();
            mediaPlayer.setAudioAttributes(attributes);
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "Player onInfo(), what:" + what + ", extra:" + extra);
                return false;
            }
        });
        lockCPU();
    }

    private void play() {
        synchronized (lock) {
            try {
                if (mediaPlayer == null) {
                    initPlayer();
                }
                mediaPlayer.reset();
                mediaPlayer.setVolume(1.0f, 1.0f);

                if (position == 0 && playbackIsDelayed) { //add delay audio if requested in settings

                    AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.gong);
                    if (afd == null) return;
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                            afd.getLength());
                    afd.close();

                    mediaPlayer.prepareAsync();
                } else {
                    mediaPlayer.setDataSource(this, medUri);
                    mediaPlayer.prepareAsync();
                }
            } catch (Exception e) {
                destroyPlayer();
                e.printStackTrace();
            }
        }
    }

    public void pauseAction() {
        setPosition(mediaPlayer.getCurrentPosition());
        stateService = Constants.STATE_PAUSE;
        notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
        broadcastPlayerChange(Constants.PLAYER_CHANGE);
        seekBar.removeCallbacks(progressRunner);
        Log.i(TAG, "Clicked Pause");
        destroyPlayer();
        handleAlerts(TURN_ON_ALL_ALERTS);
    }

    public void playAction() {
        stateService = Constants.STATE_PLAY;
        notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
        broadcastPlayerChange(Constants.PLAYER_CHANGE);
        Log.i(TAG, "Clicked Play");
        destroyPlayer();
        if (requestAudioFocus()) {
            initPlayer();
            play();
        } else { //Could not gain focus
            stopSelf();
            Toast.makeText(this, "Could not gain audio focus. Please try again.",
                    Toast.LENGTH_SHORT).show();
        }
        handleAlerts(TURN_OFF_ALL_ALERTS);
    }

    public void stopAction() {
        Log.i(TAG, "Received Stop Intent");
        resetUI();
        setBindIsOngoing(false);
        stateService = Constants.STATE_NOT_INIT;
        position = 0;
        broadcastPlayerChange(Constants.PLAYER_CHANGE);
        destroyPlayer();
        stopForeground(true);
        stopSelf();
        handleAlerts(TURN_ON_ALL_ALERTS);
    }

    private Notification prepareNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                notMan.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) {
            // The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL_ID,
                    getString(R.string.app_name), importance);
            channel.setSound(null, null);
            channel.enableVibration(false);
            notMan.createNotificationChannel(channel);
        }

        remoteViews.setOnClickPendingIntent(R.id.not_close_bt, stopPender);
        remoteViews.setTextViewText(R.id.not_title_tv, medTitle);

        switch (stateService) {
            case Constants.STATE_PAUSE:
                updateRv(View.INVISIBLE, resumePender, android.R.drawable.ic_media_play);
                break;
            case Constants.STATE_PLAY:
                updateRv(View.INVISIBLE, pausePender, android.R.drawable.ic_media_pause);
                break;
            case Constants.STATE_PREPARE:
                updateRv(View.VISIBLE, pausePender, android.R.drawable.ic_media_pause);
                break;
        }

        NotificationCompat.Builder notBuild;
        notBuild = new NotificationCompat.Builder(this,
                FOREGROUND_CHANNEL_ID);
        notBuild.setContent(remoteViews)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher_foreground))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(notPender);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notBuild.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        return notBuild.build();
    }

    /**
     * Update the remoteView
     *
     * @param visibility triggers visibility settings
     * @param pender     is the pendingIntent to be attached to the remoteView
     * @param icon       is the playback button to be set
     */
    private void updateRv(int visibility, PendingIntent pender, int icon) {
        remoteViews.setViewVisibility(R.id.not_pb, visibility);
        remoteViews.setOnClickPendingIntent(R.id.not_player_bt, pender);
        remoteViews.setImageViewResource(R.id.not_player_bt, icon);
    }

    /**
     * Create pendingIntent
     *
     * @param action to be linked to the intent
     * @param flag   to be attached to the intent (if needed)
     * @return the pendingIntent for further use
     */
    private PendingIntent createPender(String action, int flag) {

        Intent intent;

        if (action.equals(Constants.MAIN_ACTION)) {
            intent = new Intent(this, PlayerActivity.class);
        } else {
            intent = new Intent(this, MediaPlayerService.class);
        }

        if (flag != 0) {
            intent.setFlags(flag);
        }
        if (action.equals(Constants.MAIN_ACTION)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        intent.setAction(action);
        return PendingIntent.getService(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void handleAlerts(boolean activation) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean Dnd = sharedPref.getBoolean(getString(R.string.pref_dnd_switch_key), false);

        if (Dnd) {
            AudioManager auMan;
            auMan = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            assert auMan != null;
            String logText;
            if (activation == TURN_OFF_ALL_ALERTS) {
                logText = "turned off";
                //turn ringer silent
                auMan.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else {
                logText = "turned on";
                auMan.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
            Log.i(TAG, "RINGER is " + logText);

            //turn off sound, disable notifications
            auMan.setStreamMute(AudioManager.STREAM_SYSTEM, activation);
            Log.i(TAG, "STREAM_SYSTEM" + logText);
            //notifications
            auMan.setStreamMute(AudioManager.STREAM_NOTIFICATION, activation);
            Log.i(TAG, "STREAM_NOTIFICATION" + logText);
            //alarm
            auMan.setStreamMute(AudioManager.STREAM_ALARM, activation);
            Log.i(TAG, "STREAM_ALARM" + logText);
            //ringer
            auMan.setStreamMute(AudioManager.STREAM_RING, activation);
            Log.i(TAG, "STREAM_RING" + logText);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "Player onPrepared() & duration is: " + duration);
        stateService = Constants.STATE_PLAY;
        notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());

        try {
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (playbackIsDelayed) {
            mediaPlayer.start();
            broadcastPlayerChange(Constants.PLAYER_DELAY);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (mediaPlayer == null) return;
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    //reset mediaPlayer
                    mediaPlayer.reset();
                    playbackIsDelayed = false; //delay is done

                    initPlayer();
                    play();
                }
            }, delay); //set specified delay
        } else {
            if (position != 0) {
                mediaPlayer.seekTo(position);
            }
            mediaPlayer.start();
            setDuration(mediaPlayer.getDuration());

            broadcastPlayerChange(Constants.PLAYER_CHANGE);

            seekBar.setMax((int) duration);
            seekBar.postDelayed(progressRunner, interval);

            // Set our duration text view to display total duration in format 0:00
            totalDurationTv.setText(MedUtils.getDisplayTime(duration, false));
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        //Invoked when playback of a media source has completed.
        if (!playbackIsDelayed) {
            stopAction();
            Intent returnToMain = new Intent(this, MainActivity.class);
            startActivity(returnToMain);
        }
    }

    @Override
    public void onAudioFocusChange(int i) {
        //Invoked when the audio focus of the system is updated.
        switch (i) { //get focus
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioMan = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert audioMan != null;
        int result = audioMan.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        //Focus gained
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        //Could not gain focus
    }

    private void removeAudioFocus() {
        audioMan.abandonAudioFocus(this);
    }

    /**
     * MediaSession and Notification actions
     */
    private void initMediaSession() {
        MediaSessionCompat mediaSession = new MediaSessionCompat(this,
                MediaPlayerService.class.getSimpleName());

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

    /**
     * Media Session Callbacks, enabling all external clients to control the player.
     */
    class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            stateService = Constants.STATE_PLAY;
        }

        @Override
        public void onPause() {
            stateService = Constants.STATE_PAUSE;
            setPosition(mediaPlayer.getCurrentPosition());
        }
    }

    private void lockCPU() {
        PowerManager powMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powMan == null) {
            return;
        }
        wakeLock = powMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                this.getClass().getSimpleName());
        wakeLock.acquire(1000);
        Log.d(TAG, "Player lockCPU()");
    }

    private void unlockCPU() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
            Log.d(TAG, "Player unlockCPU()");
        }
    }

    private void setPosition(int position) {
        this.position = position;
    }

    public MeditationLocal getSelectedMed() {
        return selectedMed;
    }

    public Bitmap getCoverArt() {
        return coverArt;
    }

    public void setSelectedMed(MeditationLocal selectedMed) {
        this.selectedMed = selectedMed;
    }

    public void setCoverArt(Bitmap coverArt) {
        this.coverArt = coverArt;
    }

    public boolean isBindIsOngoing() {
        return bindIsOngoing;
    }

    public void setBindIsOngoing(boolean bindIsOngoing) {
        this.bindIsOngoing = bindIsOngoing;
    }

    private void setDuration(long duration) {
        this.duration = duration;
        isLongerThanHour = duration > TimeUnit.HOURS.toMillis(1);
    }

    public void setActivityIsDestroyed(boolean activityIsDestroyed) {
        this.activityIsDestroyed = activityIsDestroyed;
    }
}