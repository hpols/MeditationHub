package com.example.android.meditationhub.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.ui.PlayActivity;
import com.example.android.meditationhub.util.Constants;

/**
 * Foreground service based on: https://github.com/DimaKoz/Android-Foreground-Service-Example
 */
public class MediaPlayerService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = MediaPlayerService.class.getSimpleName();

    private final static String FOREGROUND_CHANNEL_ID = Constants.NOTIFICATION_CHANNEL_ID;
    static private int stateService = Constants.STATE_NOT_INIT;
    private MeditationLocal selectedMed;

    private Bitmap coverArt;
    private Uri medUri;
    private String medTitle;
    private final Object lock = new Object();
    private MediaPlayer mediaPlayer;
    private NotificationManager notMan;
    private PowerManager.WakeLock wakeLock;
    private RemoteViews remoteViews;

    PendingIntent notPender, pausePender, resumePender, stopPender;
    int position;

    boolean bindIsOngoing;

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
                initPlayer();
                play();
                break;

            case Constants.PAUSE_ACTION:
                pauseAction();
                break;

            case Constants.PLAY_ACTION:
                playAction();
                break;

            case Constants.STOP_ACTION:
                stopAction();
                break;

            default:
                stopForeground(true);
                broadcastPlayerChange();
                stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void broadcastPlayerChange() {
        Intent broadCastPlayerChange = new Intent();
        broadCastPlayerChange.setAction(Constants.PLAYER_CHANGE);
        sendBroadcast(broadCastPlayerChange);

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        destroyPlayer();
        stateService = Constants.STATE_NOT_INIT;
        super.onDestroy();
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
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {

        Log.d(TAG, "Player onError() what:" + what);
        destroyPlayer();
        //handler.postDelayed(delayedShutdown, Constants.DELAY_SHUTDOWN_FOREGROUND_SERVICE);
        notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
        stateService = Constants.STATE_PAUSE;
        return false;
    }

    private void initPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
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
                mediaPlayer.setDataSource(this, medUri);
                mediaPlayer.prepareAsync();

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
        broadcastPlayerChange();
        Log.i(TAG, "Clicked Pause");
        destroyPlayer();
    }

    public void playAction() {
        stateService = Constants.STATE_PLAY;
        notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
        broadcastPlayerChange();
        Log.i(TAG, "Clicked Play");
        destroyPlayer();
        initPlayer();
        play();
    }

    public void stopAction() {
        Log.i(TAG, "Received Stop Intent");
        setBindIsOngoing(false);
        stateService = Constants.STATE_NOT_INIT;
        position = 0;
        broadcastPlayerChange();
        destroyPlayer();
        stopForeground(true);
        stopSelf();
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
                        R.drawable.ic_meditation_hub))
                .setSmallIcon(R.drawable.ic_meditation_hub)
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
            intent = new Intent(this, PlayActivity.class);
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

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "Player onPrepared()");
        stateService = Constants.STATE_PLAY;
        notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
        try {
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (position != 0) {
            mediaPlayer.seekTo(position);
        }
        mediaPlayer.start();
        broadcastPlayerChange();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d(TAG, "Player onBufferingUpdate():" + percent);
    }

    private void lockCPU() {
        PowerManager powMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powMan == null) {
            return;
        }
        wakeLock = powMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getSimpleName());
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public MeditationLocal getSelectedMed() {
        return selectedMed;
    }

    public Bitmap getCoverArt() {
        return coverArt;
    }

    public Uri getMedUri() {
        return medUri;
    }

    public String getMedTitle() {
        return medTitle;
    }

    public void setSelectedMed(MeditationLocal selectedMed) {
        this.selectedMed = selectedMed;
    }

    public void setCoverArt(Bitmap coverArt) {
        this.coverArt = coverArt;
    }

    public void setMedUri(Uri medUri) {
        this.medUri = medUri;
    }

    public void setMedTitle(String medTitle) {
        this.medTitle = medTitle;
    }

    public boolean isBindIsOngoing() {
        return bindIsOngoing;
    }

    public void setBindIsOngoing(boolean bindIsOngoing) {
        this.bindIsOngoing = bindIsOngoing;
    }


}
