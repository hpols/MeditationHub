package com.example.android.meditationhub.model;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

import timber.log.Timber;

public class MeditationLocalViewModel extends AndroidViewModel {

    private final LiveData<List<MeditationLocal>> meditationLocalEntries;

    public MeditationLocalViewModel(@NonNull Application application) {
        super(application);
        MeditationLocalDb db = MeditationLocalDb.getInstance(this.getApplication());
        Timber.d("ViewModel is retrieving the entries from the db");
        meditationLocalEntries = db.meditationLocalDao().getAllEntries();
    }

    public LiveData<List<MeditationLocal>> getMeditationLocalEntries() {
        return meditationLocalEntries;
    }
}
