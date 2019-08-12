package com.example.android.meditationhub.model;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface MeditationLocalDao {

    //––– CREATE Methods –––//

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long createEntry(MeditationLocal meditationLocal);

    //––– READ Methods –––//

    @Query("SELECT * FROM meditations")
    LiveData<List<MeditationLocal>> getAllEntries();

    @Query("SELECT * FROM meditations WHERE filename LIKE :filename")
    MeditationLocal getMeditation(String filename);


    //––– UPDATE Methods –––//

    @Update(onConflict = OnConflictStrategy.IGNORE)
    int updateEntry(MeditationLocal meditationLocal);

    // –––DELETE METHODS –––//
    @Delete
    void deleteEntry(MeditationLocal meditationLocal);
}
