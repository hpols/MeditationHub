package com.example.android.meditationhub;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.widget.RemoteViews;

import com.example.android.meditationhub.ui.PlayerActivity;
import com.example.android.meditationhub.util.Constants;


/**
 * Based on https://github.com/gokulnathperiasamy/MusicPlayer
 * The {@link NotificationPanel} handles the creations and interaction with the Mediaplayer
 * Notifications
 */
public class NotificationPanel {

    private static RemoteViews remoteView;
    private final Context ctxt;
    private final String title;
    private static NotificationManager nManager;
    private static Notification.Builder nBuilder;

    public NotificationPanel(Context ctxt, String title) {
        this.ctxt = ctxt;
        this.title = title;

        createNotification();
    }

    public static void updateButton() {
        int image;
        if (PlayerActivity.isPlaying) {
            image = android.R.drawable.ic_media_pause;
        } else {
            image =  android.R.drawable.ic_media_play;
        }
        remoteView.setImageViewResource(R.id.play_pause_bt, image);
    }

    private void createNotification() {
        nBuilder = new Notification.Builder(ctxt)
                .setContentTitle(ctxt.getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_meditation_hub)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVibrate(null);

        remoteView = new RemoteViews(ctxt.getPackageName(), R.layout.messageview);
        remoteView.setTextViewText(R.id.title_tv, title);
        updateButton();

        //setListeners(remoteView); --> This will open application on click!
        setBroadcastListeners(remoteView);
        nManager = (NotificationManager) ctxt.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nBuilder.setChannelId(Constants.NOTIFICATION_CHANNEL_ID);
            NotificationChannel nChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    ctxt.getString(R.string.app_name), //name
                    NotificationManager.IMPORTANCE_LOW); //importance
            nManager.createNotificationChannel(nChannel);
        }
        updateNotification();

    }

    public static void updateNotification() {
        nBuilder.setContent(remoteView);
        nManager.notify(Constants.NOTIFICATION_ID, nBuilder.build());
    }

    public void notificationCancel() {
        nManager.cancel(Constants.NOTIFICATION_ID);
    }

    private void setBroadcastListeners(RemoteViews view) {
        setBroadcastIntentListeners(view, Constants.ACTION_PAUSE_PLAY, Constants.REQUEST_CODE_PLAY, R.id.play_pause_bt);
        setBroadcastIntentListeners(view, Constants.OPEN_APP, Constants.REQUEST_OPEN_APP, R.id.app_btn);
    }


    private void setBroadcastIntentListeners(RemoteViews view, String actionType, int requestCode, int controlID) {
        Intent intent = new Intent(actionType);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctxt, requestCode, intent, 0);
        view.setOnClickPendingIntent(controlID, pendingIntent);
    }

    /**
     * Based on https://github.com/gokulnathperiasamy/MusicPlayer
     * The {@link NotificationReceiver} is a {@link BroadcastReceiver} catching all user clicks from the
     * {@link NotificationPanel}.
     */
    public static class NotificationReceiver extends BroadcastReceiver {

        private final PlayerActivity playerActivity;
        private IntentFilter intentFilter;

        public NotificationReceiver(PlayerActivity playerActivity) {
            this.playerActivity = playerActivity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                assert action != null;
                switch (action) {
                    case Constants.OPEN_APP:
                        break;
                    case Constants.ACTION_PAUSE_PLAY:
                        if (PlayerActivity.isPlaying) {
                            playerActivity.pausePlayback();
                        } else {
                            playerActivity.startPlayback();
                        }
                        updateButton();
                        updateNotification();
                }
            }
        }

        public IntentFilter getIntentFilter() {
            if (intentFilter == null) {
                createIntentFilter();
            }
            return intentFilter;
        }

        private void createIntentFilter() {
            if (intentFilter == null) {
                intentFilter = new IntentFilter();
                intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
                intentFilter.addAction(Constants.ACTION_PAUSE_PLAY);
            }
        }
    }
}