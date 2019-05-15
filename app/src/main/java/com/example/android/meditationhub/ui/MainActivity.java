package com.example.android.meditationhub.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityMainBinding;
import com.example.android.meditationhub.model.MeditationFireBase;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.model.MeditationLocalDb;
import com.example.android.meditationhub.model.MeditationLocalViewModel;
import com.example.android.meditationhub.util.EntryExecutor;
import com.example.android.meditationhub.util.RvConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDb;
    private MeditationLocalDb meditationLocalDb;
    private DatabaseReference refMeditation;

    private ActivityMainBinding mainBinding;
    private List<MeditationFireBase> meditationsFb = new ArrayList<>();
    List<String> keys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        firebaseDb = FirebaseDatabase.getInstance();
        firebaseDb.setPersistenceEnabled(true);
        refMeditation = firebaseDb.getReference("meditations");
        refMeditation.keepSynced(true);

        meditationLocalDb = MeditationLocalDb.getInstance(this);

        mAuth = FirebaseAuth.getInstance();

        refMeditation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                meditationsFb.clear();

                for (DataSnapshot keyNodes : dataSnapshot.getChildren()) {
                    keys.add(keyNodes.getKey());
                    MeditationFireBase meditation = keyNodes.getValue(MeditationFireBase.class);
                    meditationsFb.add(meditation);
                    assert meditation != null;
                    addToLocalDb(meditation, keyNodes.getKey());
                    Log.v(getClass().getSimpleName(), "meditation added");
                }
                mAuth = FirebaseAuth.getInstance();

                mainBinding.meditationListPb.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //setup viewModel
        MeditationLocalViewModel viewModel = ViewModelProviders.of(this).get(MeditationLocalViewModel.class);
        viewModel.getMeditationLocalEntries().observe(this, new Observer<List<MeditationLocal>>() {
            @Override
            public void onChanged(@Nullable List<MeditationLocal> meditationLocals) {
                Timber.d("Updating entries from LiveData in ViewModel");
                new RvConfig().setConfig(mainBinding.meditationListRv, MainActivity.this, meditationLocals, keys);
            }
        });
    }

    private void addToLocalDb(MeditationFireBase meditations, String key) {
        final MeditationLocal receivedMeditation = new MeditationLocal();
        receivedMeditation.setTitle(meditations.getTitle());
        receivedMeditation.setSubtitle(meditations.getSubtitle());
        receivedMeditation.setFilename(meditations.getFilename());
        receivedMeditation.setLocation(meditations.getLocation());
        receivedMeditation.setId(Integer.parseInt(key));
        receivedMeditation.setStorage(null);
        EntryExecutor.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                meditationLocalDb.meditationLocalDao().createEntry(receivedMeditation);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        FirebaseUser user = mAuth.getCurrentUser();
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);

        if (user != null) {
            menu.getItem(0).setVisible(false); //login
            menu.getItem(1).setVisible(true); //logout
        } else {
            menu.getItem(0).setVisible(true); //login
            menu.getItem(1).setVisible(false); //logout
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            menu.getItem(0).setVisible(false); //login
            menu.getItem(1).setVisible(true); //logout
        } else {
            menu.getItem(0).setVisible(true); //login
            menu.getItem(1).setVisible(false); //logout
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.login_menu:
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            case R.id.logout_menu:
                mAuth.signOut();
                invalidateOptionsMenu();
                RvConfig.logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
