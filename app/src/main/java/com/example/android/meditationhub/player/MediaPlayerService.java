package com.example.android.meditationhub.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.model.MeditationLocal;
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
    //private final Handler handler = new Handler();
    private MediaPlayer mediaPlayer;
    private NotificationManager notMan;
    private NotificationCompat.Builder notBuilder;
    private Notification not;
    private PowerManager.WakeLock wakeLock;
    private RemoteViews remoteViews;

    PendingIntent notPender, pausePender, resumePender, stopPender;

    //    private Handler timerUpdateHandler = new Handler();
//    private Runnable timerUpdateRunnable = new Runnable() {
//
//        @Override
//        public void run() {
//            notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE,
//                    prepareNotification());
//            timerUpdateHandler.postDelayed(this,
//                    Constants.DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE);
//        }
//    };
//    private Runnable delayedShutdown = new Runnable() {
//
//        public void run() {
//            unlockCPU();
//            stopForeground(true);
//            stopSelf();
//        }
//
//    };
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
                setUpNotification();
                //startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                destroyPlayer();
                initPlayer();
                play();
                break;

            case Constants.PAUSE_ACTION:
                stateService = Constants.STATE_PAUSE;
                updateNotification();
                //notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                Log.i(TAG, "Clicked Pause");
                destroyPlayer();
                //handler.postDelayed(delayedShutdown, Constants.DELAY_SHUTDOWN_FOREGROUND_SERVICE);
                break;

            case Constants.PLAY_ACTION:
                stateService = Constants.STATE_PREPARE;
                updateNotification();
                //notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                Log.i(TAG, "Clicked Play");
                destroyPlayer();
                initPlayer();
                play();
                break;

            case Constants.STOP_ACTION:
                Log.i(TAG, "Received Stop Intent");
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
        Log.d(TAG, "onDestroy()");
        destroyPlayer();
        stateService = Constants.STATE_NOT_INIT;
        try {
            //timerUpdateHandler.removeCallbacksAndMessages(null);
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
        updateNotification();
        //notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
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
        try {
            //handler.removeCallbacksAndMessages(null);
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

    // call this method to setup notification for the first time
    private void setUpNotification() {

        notMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notPender = createPender(Constants.MAIN_ACTION, Intent.FLAG_ACTIVITY_NEW_TASK);
        pausePender = createPender(Constants.PAUSE_ACTION, 0);
        resumePender = createPender(Constants.PLAY_ACTION, 0);
        stopPender = createPender(Constants.STOP_ACTION, 0);

        // we need to build a basic notification first, then update it
        Intent notIntent = new Intent(this, MediaPlayerService.class);
        notIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, notIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // notification's layout
        remoteViews = new RemoteViews(getPackageName(), R.layout.radio_notification);
        // notification's title
        remoteViews.setTextViewText(R.id.not_med_title_tv, medTitle);
        remoteViews.setOnClickPendingIntent(R.id.not_close_bt, stopPender);

        notBuilder = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID);

        int apiVersion = Build.VERSION.SDK_INT;

        if (apiVersion < Build.VERSION_CODES.HONEYCOMB) {
            not = new Notification(R.drawable.ic_launcher_foreground, medTitle, System.currentTimeMillis());
            not.contentView = remoteViews;
            not.contentIntent = pendIntent;

            not.flags |= Notification.FLAG_NO_CLEAR; //Do not clear the notification
            not.defaults |= Notification.DEFAULT_LIGHTS;

            // starting service with notification in foreground mode
            startForeground(Constants.NOTIFICATION_ID, not);

        } else {
            notBuilder.setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pendIntent)
                    .setContent(remoteViews)
                    .setTicker(medTitle);

            // starting service with notification in foreground mode
            startForeground(Constants.NOTIFICATION_ID, notBuilder.build());
        }
    }

    // use this method to update the Notification's UI
    private void updateNotification() {

        int api = Build.VERSION.SDK_INT;
        // update the icon

        switch (stateService) {
            case Constants.STATE_PAUSE:
                updateRv(View.INVISIBLE, resumePender, android.R.drawable.ic_media_play);
                break;
            case Constants.STATE_PLAY:
                updateRv(View.INVISIBLE, pausePender, android.R.drawable.ic_media_pause);
                break;
        }


        // update the notification
        if (api < Build.VERSION_CODES.HONEYCOMB) {
            notMan.notify(Constants.NOTIFICATION_ID, not);
        } else {
            notMan.notify(Constants.NOTIFICATION_ID, notBuilder.build());
        }
    }

    private Notification prepareNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                notMan.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) {
            // The user-visible name of the channel.
            CharSequence name = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL_ID, name,
                    importance);
            channel.setSound(null, null);
            channel.enableVibration(false);
            notMan.createNotificationChannel(channel);
        }

        //create the necessary pendingIntents.
        notPender = createPender(Constants.MAIN_ACTION, Intent.FLAG_ACTIVITY_NEW_TASK);
        pausePender = createPender(Constants.PAUSE_ACTION, 0);
        resumePender = createPender(Constants.PLAY_ACTION, 0);
        stopPender = createPender(Constants.STOP_ACTION, 0);

        remoteViews = new RemoteViews(getPackageName(), R.layout.radio_notification);
        remoteViews.setTextViewText(R.id.not_med_title_tv, medTitle);
        remoteViews.setOnClickPendingIntent(R.id.not_close_bt, stopPender);

        updateRv(View.VISIBLE, pausePender, android.R.drawable.ic_media_pause);

        NotificationCompat.Builder notBuild;
        notBuild = new NotificationCompat.Builder(this,
                FOREGROUND_CHANNEL_ID);
        notBuild.setContent(remoteViews)
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

    /**
     * Update the remoteView
     *
     * @param visibility triggers visibility settings
     * @param pender     is the pendingIntent to be attached to the remoteView
     * @param icon       is the playback button to be set
     */
    private void updateRv(int visibility, PendingIntent pender, int icon) {
        remoteViews.setViewVisibility(R.id.not_pb, visibility);
        remoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, pender);
        remoteViews.setImageViewResource(R.id.ui_notification_player_button, icon);
    }

    /**
     * Create pendingIntent
     *
     * @param action to be linked to the intent
     * @param flag   to be attached to the intent (if needed)
     * @return the pendingIntent for further use
     */
    private PendingIntent createPender(String action, int flag) {
        Intent intent = new Intent(this, MediaPlayerService.class);
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
        setUpNotification();
        //notMan.notify(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
        try {
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
        //timerUpdateHandler.postDelayed(timerUpdateRunnable, 0);
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
        wakeLock.acquire();
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
        return mediaPlayer.getCurrentPosition();
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
}
