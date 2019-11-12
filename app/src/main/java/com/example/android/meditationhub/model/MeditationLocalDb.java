package com.example.android.meditationhub.model;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import timber.log.Timber;

@Database(entities = {MeditationLocal.class}, version = 1, exportSchema = false)
public abstract class MeditationLocalDb extends RoomDatabase {

    private static final Object LOCK = new Object();
    private static final String DB_NAME = "meditations";
    private static MeditationLocalDb instance;

    public static MeditationLocalDb getInstance(Context ctxt) {
        if (instance == null) {
            synchronized (LOCK) {
                Timber.d("creating new DB instance");
                instance = Room.databaseBuilder(ctxt.getApplicationContext(), MeditationLocalDb.class,
                        MeditationLocalDb.DB_NAME).build();
            }
        }
        Timber.d("getting DB instance");
        return instance;
    }

    public abstract MeditationLocalDao meditationLocalDao();
}
