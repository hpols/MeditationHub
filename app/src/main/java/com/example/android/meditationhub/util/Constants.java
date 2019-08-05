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
}
