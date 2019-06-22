package com.example.android.meditationhub.ui;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityPlayerBinding;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.util.Constants;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding playerBinding;
    private MeditationLocal thisMed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playerBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);

        thisMed = getIntent().getParcelableExtra(Constants.THIS_MED);

    }
}
