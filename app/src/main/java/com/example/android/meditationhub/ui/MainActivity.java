package com.example.android.meditationhub.ui;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.android.meditationhub.BuildConfig;
import com.example.android.meditationhub.MeditationAdapter;
import com.example.android.meditationhub.R;
import com.example.android.meditationhub.SettingsActivity;
import com.example.android.meditationhub.databinding.ActivityMainBinding;
import com.example.android.meditationhub.model.Header;
import com.example.android.meditationhub.model.ItemList;
import com.example.android.meditationhub.model.MeditationFireBase;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.model.MeditationLocalDb;
import com.example.android.meditationhub.model.MeditationLocalViewModel;
import com.example.android.meditationhub.util.EntryExecutor;
import com.example.android.meditationhub.util.MedUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements MeditationAdapter.AdapterInterface {

    private FirebaseAuth fireAuth;
    private FirebaseDatabase fireDb;
    private MeditationLocalDb meditationLocalDb;
    private DatabaseReference dbRefMed;

    private MeditationAdapter medAdapter;

    private ActivityMainBinding mainBinding;
    List<String> keys = new ArrayList<>();
    List<ItemList> items = new ArrayList<>();

    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEd;
    final static public String ALERT_NEEDED = "alert needed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefEd = sharedPref.edit();
        sharedPrefEd.apply();

        fireDb = FirebaseDatabase.getInstance();
        dbRefMed = fireDb.getReference("meditations");

        meditationLocalDb = MeditationLocalDb.getInstance(this);

        fireAuth = FirebaseAuth.getInstance();

        dbRefMed.addValueEventListener(new ValueEventListener() {
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
                fireAuth = FirebaseAuth.getInstance();

                mainBinding.meditationListPb.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final boolean showAlert = sharedPref.getBoolean(ALERT_NEEDED, true);

        //setup viewModel
        MeditationLocalViewModel viewModel
                = ViewModelProviders.of(this).get(MeditationLocalViewModel.class);
        viewModel.getMeditationLocalEntries().observe(this, new Observer<List<MeditationLocal>>() {
            @Override
            public void onChanged(@Nullable List<MeditationLocal> meditationLocals) {
                Timber.d("Updating entries from LiveData in ViewModel");
                createOrderedItemList(meditationLocals);

                medAdapter = new MeditationAdapter(MainActivity.this, fireAuth, items,
                        MainActivity.this);

                final int numOfCol = MedUtils.noOfCols(MainActivity.this);
                GridLayoutManager layoutMan = new GridLayoutManager(MainActivity.this,
                        numOfCol);
                layoutMan.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        switch(medAdapter.getItemViewType(position)){
                            case ItemList.TYPE_HEADER:
                                return numOfCol;
                            case ItemList.TYPE_ITEM:
                            default:
                                return 1;
                        }
                    }
                });

                mainBinding.meditationRv.setLayoutManager(layoutMan);
                mainBinding.meditationRv.setAdapter(medAdapter);
            }
        });
    }

    private void createOrderedItemList(List<MeditationLocal> meditationLocals) {

        Collections.sort(meditationLocals);
        String prevCategory = null;
        for (int i = 0; i < meditationLocals.size(); i++) {
            String currCategory = meditationLocals.get(i).getCategory();
            if (prevCategory != null && prevCategory.equals(currCategory)) {
                items.add(meditationLocals.get(i));
            } else {
                items.add(new Header(currCategory));
                items.add(meditationLocals.get(i));
                prevCategory = currCategory;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (medAdapter != null) {
            medAdapter.saveStates(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (medAdapter != null) {
            medAdapter.restoreStates(savedInstanceState);
        }
    }

    private void addToLocalDb(final MeditationFireBase meditations, final String key) {
        EntryExecutor.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                String receivedMeditationFilename = meditations.getFilename();
                MeditationLocal storedMeditation = meditationLocalDb.meditationLocalDao()
                        .getMeditation(receivedMeditationFilename);
                if (storedMeditation == null) {
                    final MeditationLocal receivedMeditation = new MeditationLocal();
                    receivedMeditation.setTitle(meditations.getTitle());
                    receivedMeditation.setSubtitle(meditations.getSubtitle());
                    receivedMeditation.setFilename(meditations.getFilename());
                    receivedMeditation.setCategory(meditations.getCategory());
                    receivedMeditation.setId(key);
                    receivedMeditation.setStorage(null);

                    meditationLocalDb.meditationLocalDao().createEntry(receivedMeditation);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        FirebaseUser user = fireAuth.getCurrentUser();
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
        FirebaseUser user = fireAuth.getCurrentUser();

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
                fireAuth.signOut();
                invalidateOptionsMenu();
                MeditationAdapter.logout();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * download the meditation. In part based on: https://gist.github.com/emaillenin/9a0fea5a6924ddb23b8dd620392e745f
     *
     * @param uri         of the audio file to be downloaded
     * @param selectedMed the pojo of the meditation in question
     * @param medPos      the position of the meditation in the adapter
     */
    @Override
    public void download(Uri uri, final MeditationLocal selectedMed, final int medPos) {

        //setup the snackbar to track the download
        final Snackbar bar = Snackbar.make(mainBinding.getRoot(), "", Snackbar.LENGTH_INDEFINITE);

        // Get the view object.
        Snackbar.SnackbarLayout snackbarView = (Snackbar.SnackbarLayout) bar.getView();

        // Get custom view from external layout xml file.
        // see https://www.dev2qa.com/android-snackbar-example/
        View customView = getLayoutInflater().inflate(R.layout.download_prog, null);
        TextView snackMes = customView.findViewById(R.id.snackbar_text);
        snackMes.setText("Downloading: " + selectedMed.getTitle());
        final ProgressBar snackProg = customView.findViewById(R.id.circularProgressbar);
        snackProg.setMax(0);
        snackProg.setMax(100);

        snackbarView.addView(customView, 0);

        bar.show();

        //set up the download manager and the file destination
        final DownloadManager dlManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        String destination = Environment.getExternalStorageDirectory() + "/MeditationHub";
        Timber.v("destination: " + destination);

        //ensure the folder exists before continuing
        File file = new File(destination);
        if (!file.exists())
            file.mkdirs();

        destination += "/" + selectedMed.getFilename();
        final Uri destinationUri = Uri.parse("file://" + destination);

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

                    final double bytesTotal = csr.getInt(csr.getColumnIndex(DownloadManager
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
                            final double bytesLoaded = csr.getInt(csr
                                    .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            if (bytesLoaded != 0) {
                                final int dlProgress = (int) ((bytesLoaded / bytesTotal) * 100);

                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        snackProg.setProgress(dlProgress);
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
                            Timber.v("content Uri of downloaded audio: " + contentUri);

                            EntryExecutor.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    selectedMed.setStorage(String.valueOf(contentUri));
                                    meditationLocalDb.meditationLocalDao().updateEntry(selectedMed);
                                    Timber.v("Updated meditation: " + selectedMed.toString());
                                }
                            });
                            bar.dismiss();
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

    }

    @Override
    public void remove(final int medPos, final MeditationLocal selectedMed) {
//        //access preferences to note whether the alert is required/wished
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        final SharedPreferences.Editor sharedPrefEd = sharedPref.edit();
//        final String ALERT_NEEDED = "alert needed";
//
//        //show alert along with the opt-out checkbox
//        if (sharedPref.getBoolean(ALERT_NEEDED, true)) {
//            AlertDialog.Builder alertBuild = new AlertDialog.Builder(this, R.style.dialog);
//            alertBuild.setTitle(R.string.alert_title);
//
//            final boolean[] checked = new boolean[]{true};
//            alertBuild.setMultiChoiceItems(new String[]{getString(R.string.alert_cb)}, checked,
//                    new DialogInterface.OnMultiChoiceClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i, boolean b) {
//                            checked[i] = b;
//                        }
//                    });
//            alertBuild.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    removeFile(medPos, selectedMed);
//                    if (checked[0]) {//show alert again next time
//                        sharedPrefEd.putBoolean(ALERT_NEEDED, true);
//                    } else { //no longer show alert
//                        sharedPrefEd.putBoolean(ALERT_NEEDED, false);
//                    }
//                    sharedPrefEd.apply();
//                }
//            });
//            alertBuild.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    medAdapter.notifyItemChanged(medPos);
//                }
//            });
//
//            AlertDialog alertDialog = alertBuild.create();
//            alertDialog.show();
//        } else {//directly call the move method, alert is no longer needed.
        removeFile(medPos, selectedMed);
//        }
    }

    /**
     * remove the audio file from the device
     *
     * @param medPos      of the meditation in the adapter.
     * @param selectedMed is the selected meditation
     */
    private void removeFile(final int medPos, final MeditationLocal selectedMed) {

        Uri uriToBeRemoved = Uri.parse(selectedMed.getStorage());
        int i = getContentResolver().delete(uriToBeRemoved, null, null);

        if (i > -1) {
            EntryExecutor.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    selectedMed.setStorage(null);
                    int u = meditationLocalDb.meditationLocalDao().updateEntry(selectedMed);
                }

                @Override
                protected void finalize() throws Throwable {
                    super.finalize();
                    medAdapter.notifyItemChanged(medPos);
                }
            });

        }
    }
}