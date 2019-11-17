package com.example.android.meditationhub.model;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MeditationLocal.class}, version = 1, exportSchema = false)
public abstract class MeditationLocalDb extends RoomDatabase {

    private static final String TAG = MeditationLocalDb.class.getSimpleName();

    private static final Object LOCK = new Object();
    private static final String DB_NAME = "meditations";
    private static MeditationLocalDb instance;

    public static MeditationLocalDb getInstance(Context ctxt) {
        if (instance == null) {
            synchronized (LOCK) {
                Log.d(TAG,"creating new DB instance");
                instance = Room.databaseBuilder(ctxt.getApplicationContext(), MeditationLocalDb.class,
                        MeditationLocalDb.DB_NAME).build();
            }
        }
        Log.d(TAG,"getting DB instance");
        return instance;
    }

    public abstract MeditationLocalDao meditationLocalDao();
}
