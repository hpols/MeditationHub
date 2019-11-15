package com.example.android.meditationhub.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.android.meditationhub.BuildConfig;
import com.example.android.meditationhub.R;
import com.example.android.meditationhub.util.Constants;

import timber.log.Timber;

/**
 * Foreground service based on: https://github.com/DimaKoz/Android-Foreground-Service-Example
 */
public class MediaPlayerService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener{

    private final static String FOREGROUND_CHANNEL_ID = Constants.NOTIFICATION_CHANNEL_ID;
    static private int stateService = Constants.STATE_NOT_INIT;
    private Uri medUri;
    private final Object lock = new Object();
    private final Handler handler = new Handler();
    private MediaPlayer mediaPlayer;
    private NotificationManager notMan;
    private PowerManager.WakeLock wakeLock;

    private Handler timerUpdateHandler = new Handler();
    private Runnable timerUpdateRunnable = new Runnable() {

        @Override
        public void run() {
            notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE,
                    prepareNotification());
            timerUpdateHandler.postDelayed(this,
                    Constants.DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE);
        }
    };
    private Runnable delayedShutdown = new Runnable() {

        public void run() {
            unlockCPU();
            stopForeground(true);
            stopSelf();
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

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        Timber.d("onCreate()");
        stateService = Constants.STATE_NOT_INIT;
        notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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
                Timber.i("Received start Intent ");
                if(intent.getExtras() != null) {
                    medUri = (Uri) intent.getExtras().get(Constants.URI);
                }
                stateService = Constants.STATE_PREPARE;

                startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                destroyPlayer();
                initPlayer();
                play();
                break;

            case Constants.PAUSE_ACTION:
                stateService = Constants.STATE_PAUSE;
                notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                Timber.i("Clicked Pause");
                destroyPlayer();
                handler.postDelayed(delayedShutdown, Constants.DELAY_SHUTDOWN_FOREGROUND_SERVICE);
                break;

            case Constants.PLAY_ACTION:
                stateService = Constants.STATE_PREPARE;
                notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                Timber.i("Clicked Play");
                destroyPlayer();
                initPlayer();
                play();
                break;

            case Constants.STOP_ACTION:
                Timber.i("Received Stop Intent");
                destroyPlayer();
                stopForeground(true);
                stopSelf();
                break;

            default:
                stopForeground(true);
                stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy()");
        destroyPlayer();
        stateService = Constants.STATE_NOT_INIT;
        try {
            timerUpdateHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void destroyPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
                Timber.d("Player destroyed");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mediaPlayer = null;
            }
        }
        unlockCPU();

    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Timber.d("Player onError() what:" + what);
        destroyPlayer();
        handler.postDelayed(delayedShutdown, Constants.DELAY_SHUTDOWN_FOREGROUND_SERVICE);
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
                Timber.d("Player onInfo(), what:" + what + ", extra:" + extra);
                return false;
            }
        });
        lockCPU();
    }

    private void play() {
        try {
            handler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    private Notification prepareNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                notMan.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) {
            // The user-visible name of the channel.
            CharSequence name = getString(R.string.text_value_radio_notification);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL_ID, name,
                    importance);
            channel.setSound(null, null);
            channel.enableVibration(false);
            notMan.createNotificationChannel(channel);
        }

        //create the necessary pendingIntents.
        PendingIntent notPender = createPender(Constants.MAIN_ACTION, Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pausePender = createPender(Constants.PAUSE_ACTION, 0);
        PendingIntent resumePender = createPender(Constants.PLAY_ACTION, 0);
        PendingIntent stopPender = createPender(Constants.STOP_ACTION, 0);

        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.radio_notification);
        rv.setOnClickPendingIntent(R.id.ui_notification_close_button, stopPender);

        switch (stateService) {

            case Constants.STATE_PAUSE:
                updateRv(rv, View.INVISIBLE, resumePender, android.R.drawable.ic_media_play);
                break;

            case Constants.STATE_PLAY:
                updateRv(rv, View.INVISIBLE, pausePender, android.R.drawable.ic_media_pause);
                break;

            case Constants.STATE_PREPARE:
                updateRv(rv, View.VISIBLE, pausePender, android.R.drawable.ic_media_pause);
                break;
        }

        NotificationCompat.Builder notBuild;
        notBuild = new NotificationCompat.Builder(this,
                FOREGROUND_CHANNEL_ID);
        notBuild.setContent(rv)
                .setSmallIcon(R.mipmap.ic_launcher)
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

    /** Update the remoteView
     *
     * @param rv is the remoteView in question
     * @param visibility triggers visibility settings
     * @param pender is the pendingIntent to be attached to the remoteView
     * @param icon is the playback button to be set
     */
    private void updateRv(RemoteViews rv, int visibility, PendingIntent pender, int icon) {
        rv.setViewVisibility(R.id.ui_notification_progress_bar, visibility);
        rv.setOnClickPendingIntent(R.id.ui_notification_player_button, pender);
        rv.setImageViewResource(R.id.ui_notification_player_button, icon);
    }

    /** Create pendingIntent
     *
     * @param action to be linked to the intent
     * @param flag to be attached to the intent (if needed)
     * @return the pendingIntent for further use
     */
    private PendingIntent createPender(String action, int flag) {
        Intent intent = new Intent(this, MediaPlayerService.class);
        if (flag != 0) {
            intent.setFlags(flag);
        }
        intent.setAction(action);
        return PendingIntent.getService(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Timber.d("Player onPrepared()");
        stateService = Constants.STATE_PLAY;
        notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE,
                prepareNotification());
        try {
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
        timerUpdateHandler.postDelayed(timerUpdateRunnable, 0);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Timber.d("Player onBufferingUpdate():" + percent);
    }

    private void lockCPU() {
        PowerManager powMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powMan == null) {
            return;
        }
        wakeLock = powMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getSimpleName());
        wakeLock.acquire();
        Timber.d("Player lockCPU()");
    }

    private void unlockCPU() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
            Timber.d("Player unlockCPU()");
        }
    }

    public int getPosition() {
        return mediaPlayer.getCurrentPosition();
    }
}
