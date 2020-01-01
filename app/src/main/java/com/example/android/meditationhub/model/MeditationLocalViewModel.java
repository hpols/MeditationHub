package com.example.android.meditationhub.model;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MeditationLocalViewModel extends AndroidViewModel {

    private static final String TAG = MeditationLocalViewModel.class.getSimpleName();

    private final LiveData<List<MeditationLocal>> meditationLocalEntries;

    public MeditationLocalViewModel(@NonNull Application application) {
        super(application);
        MeditationLocalDb db = MeditationLocalDb.getInstance(this.getApplication());
        Log.d(TAG, "ViewModel is retrieving the entries from the db");
        meditationLocalEntries = db.meditationLocalDao().getAll();
    }

    public LiveData<List<MeditationLocal>> getMeditationLocalEntries() {
        return meditationLocalEntries;
    }
}
