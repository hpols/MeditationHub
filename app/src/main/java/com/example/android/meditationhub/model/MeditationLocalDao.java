package com.example.android.meditationhub.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

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
