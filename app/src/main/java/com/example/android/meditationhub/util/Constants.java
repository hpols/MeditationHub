package com.example.android.meditationhub.util;

import android.os.Environment;

import java.io.File;

public class Constants {

    //keys
    public static final String SELECTED_MED = "ThisMed";
    public static final String ACTION = "action";
    public static final String ART = "coverArt";
    public static final String PLAYER_READY = "player ready";
    public static final String PLAYER_POSITION = "player position";
    public static final String IS_PLAYING = "is playing";
    public static final String DURATION = "audio_duration";
    public static final String DELAY_TV = "delay textView";

    //start playing when activity is opened?
    public final static int AUTO_PLAY = 1;
    public final static int JUST_OPEN = 0;

    //
    public static final String FOLDER = "MeditationHub";
    public static final File APP_FOLDER = new File(Environment.getExternalStorageDirectory(), FOLDER);

    public static final String OPEN_APP = "openApp";
    public static final String DO = "notificationAction";
    public static final int NOTIFICATION_ID = 2;

    public static final int REQUEST_CODE_PLAY = 111;
    public static final int REQUEST_OPEN_APP = 222;

    public static final String NOTIFICATION_CHANNEL_ID = "MUSIC_PLAYER_12345";

    public static final String ACTION_PAUSE_PLAY = "pause/play";
    public static final String ACTION_OPEN_APP = "open app";
    public static final String URI = "uri of meditation";

    public static final int SAVED_INT = 0;
    public static final int SAVED_BOO = 1;
    public static final int SAVED_PARCEL = 2;

    public static final String PLAYBACK_POS = "playback position";
    public static final int CONVERT_POSITION = 1;
    public static final int CONVERT_DURATION = 0;

    public static final String SERVICE_ID = "playerService id";
    public static final int LOADER_ID = 10;

    public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 8466503;
    public static final long DELAY_SHUTDOWN_FOREGROUND_SERVICE = 20000;
    public static final long DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE = 10000;

    //service states and actions
    public static final int STATE_STOP = 40;
    public static final int STATE_PREPARE = 30;
    public static final int STATE_PLAY = 20;
    public static final int STATE_PAUSE = 10;
    public static final int STATE_NOT_INIT = 0;

    public static final String MAIN_ACTION = "action_main";
    public static final String PAUSE_ACTION = "action_pause";
    public static final String PLAY_ACTION = "action_play";
    public static final String START_ACTION = "action_start";
    public static final String STOP_ACTION = "action_stop";


    public static final String TITLE = "med_title";
    public static final String LOGIN_BOO = "is_login";
    public static final String PLAYER_CHANGE = "play_button_update";
    public static final String PLAYER_DELAY = "play_delay_ui";
}
