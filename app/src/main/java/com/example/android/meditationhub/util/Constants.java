package com.example.android.meditationhub.util;

import android.os.Environment;

import java.io.File;

public class Constants {
    //keys
    public static final String THIS_MED = "ThisMed";
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


}
