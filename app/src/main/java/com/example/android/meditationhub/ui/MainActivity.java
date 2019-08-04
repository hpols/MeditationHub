package com.example.android.meditationhub.ui;

import android.app.DownloadManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.meditationhub.BuildConfig;
import com.example.android.meditationhub.MeditationAdapter;
import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityMainBinding;
import com.example.android.meditationhub.model.MeditationFireBase;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.model.MeditationLocalDb;
import com.example.android.meditationhub.model.MeditationLocalViewModel;
import com.example.android.meditationhub.util.EntryExecutor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements MeditationAdapter.AdapterInterface {

    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDb;
    private MeditationLocalDb meditationLocalDb;
    private DatabaseReference refMeditation;

    private MeditationAdapter medAdapter;

    private ActivityMainBinding mainBinding;
    List<String> keys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        firebaseDb = FirebaseDatabase.getInstance();
        refMeditation = firebaseDb.getReference("meditations");

        meditationLocalDb = MeditationLocalDb.getInstance(this);

        mAuth = FirebaseAuth.getInstance();

        refMeditation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mainBinding.meditationListPb.setVisibility(View.VISIBLE);
                for (DataSnapshot keyNodes : dataSnapshot.getChildren()) {
                    keys.add(keyNodes.getKey());
                    MeditationFireBase meditation = keyNodes.getValue(MeditationFireBase.class);
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
                medAdapter = new MeditationAdapter(MainActivity.this, mAuth, meditationLocals, MainActivity.this);
                mainBinding.meditationListRv.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                mainBinding.meditationListRv.setAdapter(medAdapter);
            }
        });
    }

    private void addToLocalDb(final MeditationFireBase meditations, final String key) {
        EntryExecutor.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                String receivedMeditationFilename = meditations.getFilename();
                MeditationLocal storedMeditation = meditationLocalDb.meditationLocalDao().getMeditation(receivedMeditationFilename);
                if (storedMeditation == null) {
                    final MeditationLocal receivedMeditation = new MeditationLocal();
                    receivedMeditation.setTitle(meditations.getTitle());
                    receivedMeditation.setSubtitle(meditations.getSubtitle());
                    receivedMeditation.setFilename(meditations.getFilename());
                    receivedMeditation.setLocation(meditations.getLocation());
                    receivedMeditation.setId(key);
                    receivedMeditation.setStorage(null);

                    long id = meditationLocalDb.meditationLocalDao().createEntry(receivedMeditation);
                }
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
                MeditationAdapter.logout();
                return true;
            case R.id.uri_add:
                addUri();
        }
        return super.onOptionsItemSelected(item);
    }

    private void addUri() {
        EntryExecutor.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                MeditationLocal selectedMed = meditationLocalDb.meditationLocalDao().getMeditation("WalkingMed4.mp3");
                selectedMed.setStorage("/sdcard/Android/data/com.example.android.meditationhub/files/mnt/sdcard/MeditationHub/WalkingMed4.mp3");
                meditationLocalDb.meditationLocalDao().updateEntry(selectedMed);
                Timber.v("Updated meditation: " + selectedMed.toString());
            }
        });
        medAdapter.notifyDataSetChanged();
    }

    /**
     * download the meditation. In part based on: https://gist.github.com/emaillenin/9a0fea5a6924ddb23b8dd620392e745f
     *
     * @param uri     of the audio file to be downloaded
     * @param selectedMed the pojo of the meditation in question
     * @param medPos  the position of the meditation in the adapter
     */
    @Override
    public void download(Uri uri, final MeditationLocal selectedMed, final int medPos) {

        //show views to track the download
        mainBinding.downloadInfoCv.setVisibility(View.VISIBLE);
        mainBinding.downloadMsgTv.setText("Downloading: " + selectedMed.getTitle());

        //set up the download manager and the file destination
        final DownloadManager dlManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        String destination = Environment.getExternalStorageDirectory() + "/MeditationHub";
        Timber.v("destination: "+ destination);

        //ensure the folder exists before continuing
        File file = new File(destination);
        if (!file.exists())
            file.mkdirs();

        destination += "/" + selectedMed.getFilename();
        final Uri destinationUri = Uri.parse("file://" + destination);
        Timber.v("destinationUri: "+ destinationUri);

        //create the download request
        DownloadManager.Request dlRequest = new DownloadManager.Request(uri);
        dlRequest.setDestinationUri(destinationUri);
        final long dlId = dlManager.enqueue(dlRequest);

        final String finalDestination = destination;

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean downloading = true;
                while (downloading) {

                    DownloadManager.Query dlQuery = new DownloadManager.Query();
                    dlQuery.setFilterById(dlId);

                    Cursor csr = dlManager.query(dlQuery);
                    csr.moveToFirst();

                    final int bytesTotal = csr.getInt(csr.getColumnIndex(DownloadManager
                            .COLUMN_TOTAL_SIZE_BYTES));

                    if (csr.getInt(csr.getColumnIndex(DownloadManager.COLUMN_STATUS))
                            == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }

                    String msg;

                    switch (csr.getInt(csr.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                        case DownloadManager.STATUS_FAILED:
                            int reason = csr.getInt(csr.getColumnIndex(DownloadManager.COLUMN_REASON));
                            msg = "Download failed!" + reason;
                            dlManager.remove(dlId);
                            downloading = false;
                            break;

                        case DownloadManager.STATUS_PAUSED:
                            msg = "Download paused!";
                            break;

                        case DownloadManager.STATUS_PENDING:
                            msg = "Download pending!";
                            break;

                        case DownloadManager.STATUS_RUNNING:
                            msg = "Download in progress!";
                            final int bytesLoaded = csr.getInt(csr
                                    .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            if (bytesLoaded != 0) {
                                final int dlProgress = (int) ((bytesLoaded * 100.0f)/ bytesTotal);

                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainBinding.downloadPb.setProgress(dlProgress);
                                        mainBinding.downloadPercentTv.setText(dlProgress + " %");
                                        Timber.v("Progress: downloaded: " + bytesLoaded +
                                                " from total: " + bytesTotal + "=" + dlProgress);
                                    }
                                });
                            }
                            break;

                        case DownloadManager.STATUS_SUCCESSFUL:
                            msg = "Download complete!";

                            final Uri contentUri = FileProvider.getUriForFile(MainActivity.this,
                                    BuildConfig.APPLICATION_ID + ".file_provider", new File(finalDestination));

                            EntryExecutor.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    selectedMed.setStorage(String.valueOf(contentUri));
                                    meditationLocalDb.meditationLocalDao().updateEntry(selectedMed);
                                    Timber.v("Updated meditation: " + selectedMed.toString());
                                }
                            });
                            break;

                        default:
                            msg = "Download is nowhere in sight";
                            break;
                    }

                    Timber.d(msg);
                    csr.close();
                }
            }
        }).start();

        //whether successful or not, hide views again
        medAdapter.notifyItemChanged(medPos);
        mainBinding.downloadInfoCv.setVisibility(View.GONE);
    }
}