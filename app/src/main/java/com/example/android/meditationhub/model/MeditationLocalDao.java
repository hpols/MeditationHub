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
    long createMed(MeditationLocal meditationLocal);

    //––– READ Methods –––//

    @Query("SELECT * FROM meditations")
    LiveData<List<MeditationLocal>> getAll();

    @Query("SELECT * FROM meditations WHERE id LIKE :id")
    MeditationLocal getMedById(String id);


    //––– UPDATE Methods –––//

    @Update(onConflict = OnConflictStrategy.IGNORE)
    int updateMed(MeditationLocal meditationLocal);

    // –––DELETE METHODS –––//
    @Delete
    void deleteMed(MeditationLocal meditationLocal);
}
