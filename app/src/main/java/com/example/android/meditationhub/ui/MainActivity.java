package com.example.android.meditationhub.ui;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.android.meditationhub.BuildConfig;
import com.example.android.meditationhub.MeditationAdapter;
import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivityMainBinding;
import com.example.android.meditationhub.model.Header;
import com.example.android.meditationhub.model.ItemList;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.model.MeditationLocalDb;
import com.example.android.meditationhub.model.MeditationLocalViewModel;
import com.example.android.meditationhub.util.Constants;
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

public class MainActivity extends AppCompatActivity implements MeditationAdapter.AdapterInterface {

    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseAuth fireAuth;
    private FirebaseDatabase fireDb;
    private MeditationLocalDb medDb;
    private DatabaseReference dbRefMed;

    private MeditationAdapter medAdapter;
    private NetworkReceiver networkReceiver = new NetworkReceiver();

    private ActivityMainBinding mainBinder;
    List<String> keys = new ArrayList<>();
    List<MeditationLocal> meds = new ArrayList<>();
    List<ItemList> itemsOnline = new ArrayList<>();
    List<ItemList> itemsOffline = new ArrayList<>();

    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEd;
    private IntentFilter intentFilter;

    boolean isOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinder = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mainBinder.swipeRefresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (isOnline) {
                    checkFirebaseForUpdates();
                    createOrderedItemList();
                    medAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Go online to check for updates",
                            Toast.LENGTH_SHORT).show();
                    mainBinder.swipeRefresher.setRefreshing(false);
                }
            }
        });

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefEd = sharedPref.edit();
        sharedPrefEd.apply();

        isOnline = MedUtils.isInternetAvailable(this);

        fireDb = FirebaseDatabase.getInstance();
        dbRefMed = fireDb.getReference("meditations");
        medDb = MeditationLocalDb.getInstance(this);
        fireAuth = FirebaseAuth.getInstance();

        if (isOnline) {
            checkFirebaseForUpdates();
        } else {
            setupViewModel();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkReceiver);
    }

    class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Network Receiver has been fired");
            isOnline = MedUtils.isInternetAvailable(MainActivity.this);

            checkFirebaseForUpdates();
            createOrderedItemList();
            if (medAdapter != null) {
                setItems();
                medAdapter.notifyDataSetChanged();
            }

        }
    }

    // ––– GENERAL METHODS ––– //

    private void setupMedAdapter() {
        medAdapter = new MeditationAdapter(this, fireAuth, this);
        setItems();

        final int numOfCol = MedUtils.noOfCols(MainActivity.this);

        GridLayoutManager layoutMan = new GridLayoutManager(MainActivity.this, numOfCol);
        layoutMan.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (medAdapter.getItemViewType(position)) {
                    case ItemList.TYPE_HEADER:
                        return numOfCol;
                    case ItemList.TYPE_ITEM:
                    default:
                        return 1;
                }
            }
        });

        mainBinder.meditationRv.setLayoutManager(layoutMan);
        mainBinder.meditationRv.setHasFixedSize(true);
        mainBinder.meditationRv.setAdapter(medAdapter);
    }

    private void setItems() {
        if (isOnline) {
            medAdapter.setItems(itemsOnline);
        } else {
            medAdapter.setItems(itemsOffline);
        }
    }

    private void checkFirebaseForUpdates() {
        mainBinder.swipeRefresher.setRefreshing(true);
        dbRefMed.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot fbKeys : dataSnapshot.getChildren()) {
                    MainActivity.this.keys.add(fbKeys.getKey());
                    MeditationLocal meditation = fbKeys.getValue(MeditationLocal.class);
                    assert meditation != null;
                    meditation.setId(fbKeys.getKey());
                    updateLocalDb(meditation);
                }

                mainBinder.swipeRefresher.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mainBinder.swipeRefresher.setRefreshing(false);
            }
        });

        setupViewModel();
    }

    //setup the viewModel
    private void setupViewModel() {
        Log.v(TAG, "Steup ViewModel called");
        MeditationLocalViewModel viewModel
                = ViewModelProviders.of(this).get(MeditationLocalViewModel.class);
        viewModel.getMeditationLocalEntries().observe(this, new Observer<List<MeditationLocal>>() {
            @Override
            public void onChanged(@Nullable List<MeditationLocal> meditationLocals) {
                meds = meditationLocals;
                Log.d(TAG, "Updating entries from LiveData in ViewModel. Current count: " + meds.size());
                createOrderedItemList();
                setupMedAdapter();
            }
        });
    }

    private void createOrderedItemList() {
        Log.v(TAG, "Create ordered List called");
        itemsOnline.clear();
        itemsOffline.clear();
        Collections.sort(meds);
        String prevCatOnline = null, prevCatOffline = null;
        for (int i = 0; i < meds.size(); i++) {
            String currCatOnline = meds.get(i).getCategory();
            if (prevCatOnline != null && prevCatOnline.equals(currCatOnline)) {
                itemsOnline.add(meds.get(i));
            } else {
                itemsOnline.add(new Header(currCatOnline));
                itemsOnline.add(meds.get(i));
                prevCatOnline = currCatOnline;
            }

            //also create an offline list, for easy switching between the two states
            if (meds.get(i).getStorage() != null) {
                String currCatOffline = meds.get(i).getCategory();
                if (prevCatOffline != null && prevCatOffline.equals(currCatOffline)) {
                    itemsOffline.add(meds.get(i));
                } else {
                    itemsOffline.add(new Header(currCatOffline));
                    itemsOffline.add(meds.get(i));
                    prevCatOffline = currCatOffline;
                }
            }
        }
        if (itemsOnline.size() != itemsOffline.size()) {
            itemsOffline.add(new Header("~ Go online to view all available meditations ~"));
        }

    }

    private void updateLocalDb(final MeditationLocal meditations) {
        EntryExecutor.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final boolean[] toBeUpdated = new boolean[1];
                String receivedMedId = meditations.getId();
                MeditationLocal storedMed = medDb.meditationLocalDao().getMedById(receivedMedId);
                if (storedMed == null) {
                    medDb.meditationLocalDao().createMed(meditations);
                } else { //check for differences and update as necessary
                    String logT = null, logS = null, logF = null, logC = null;

                    if (!meditations.getTitle().equals(storedMed.getTitle())) {
                        storedMed.setTitle(meditations.getTitle());
                        logT = "Title has been changed to: " + meditations.getTitle() + ", ";
                        toBeUpdated[0] = true;
                    }
                    if (!meditations.getSubtitle().equals(storedMed.getSubtitle())) {
                        storedMed.setSubtitle(meditations.getSubtitle());
                        logS = "Subtitle has been changed to: " + meditations.getSubtitle() + ", ";
                        toBeUpdated[0] = true;
                    }
                    if (!meditations.getFilename().equals(storedMed.getFilename())) {
                        storedMed.setFilename(meditations.getFilename());
                        logF = "Filename has been changed to: " + meditations.getFilename() + ", ";
                        toBeUpdated[0] = true;
                    }
                    if (!meditations.getCategory().equals(storedMed.getCategory())) {
                        storedMed.setCategory(meditations.getCategory());
                        logC = "Category has been changed to: " + meditations.getCategory() + ", ";
                        toBeUpdated[0] = true;
                    }
                    //don't touch Id or Storage
                    if (toBeUpdated[0]) {
                        medDb.meditationLocalDao().updateMed(storedMed);
                        Log.v(TAG, "Meditation no. " + meditations.getId() + ": " + logT + logS + logF + logC);
                    }

                }
            }
        });
    }

    // ––– MENU METHODS ––– //

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

    // ––– INTERFACE METHODS ––– //

    /**
     * start playermethod passing all necessary information to it
     *
     * @param selectedMed is the meditation to be played
     * @param medUri      is the uri of the meditation
     * @param thumbIv     is the thumbnail
     * @param play        indicates whether the player should start automatically once loaded
     */
    @Override
    public void goToPlayer(MeditationLocal selectedMed, Uri medUri, ImageView thumbIv,
                           boolean play) {
        Intent openPlayer = new Intent(this, PlayerActivity.class);
        openPlayer.putExtra(Constants.SELECTED_MED, selectedMed);
        openPlayer.putExtra(Constants.URI, medUri);
        openPlayer.putExtra(Constants.IS_PLAYING, play);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle transitionBundle = ActivityOptions.makeSceneTransitionAnimation(this,
                    thumbIv, thumbIv.getTransitionName()).toBundle();
            startActivity(openPlayer, transitionBundle);
        } else {
            startActivity(openPlayer);
        }
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
        final Snackbar bar = Snackbar.make(mainBinder.getRoot(), "", Snackbar.LENGTH_INDEFINITE);

        // Get the view object.
        Snackbar.SnackbarLayout snackbarView = (Snackbar.SnackbarLayout) bar.getView();

        // Get custom view from external layout xml file.
        // see https://www.dev2qa.com/android-snackbar-example/
        View customView = getLayoutInflater().inflate(R.layout.download_prog, null);
        TextView snackMes = customView.findViewById(R.id.snackbar_text);
        String snackText = getString(R.string.download_snack) + selectedMed.getTitle();
        snackMes.setText(snackText);
        final ProgressBar snackProg = customView.findViewById(R.id.circularProgressbar);
        snackProg.setMax(0);
        snackProg.setMax(100);
        snackbarView.addView(customView, 0);
        bar.show();

        //set up the download manager and the file destination
        final DownloadManager dlManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        String destination = Environment.getExternalStorageDirectory() + "/MeditationHub";

        //ensure the folder exists before continuing
        File file = new File(destination);
        if (!file.exists())
            file.mkdirs();

        destination += "/" + selectedMed.getFilename();
        final Uri destinationUri = Uri.parse("file://" + destination);

        //create the download request
        DownloadManager.Request dlRequest = new DownloadManager.Request(uri);
        dlRequest.setDestinationUri(destinationUri);
        assert dlManager != null;
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
                                    }
                                });
                            }
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            msg = "Download complete!";

                            final Uri contentUri = FileProvider.getUriForFile(MainActivity.this,
                                    BuildConfig.APPLICATION_ID + ".file_provider", new File(finalDestination));
                            Log.v(TAG, "content Uri of downloaded audio: " + contentUri);

                            EntryExecutor.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    selectedMed.setStorage(String.valueOf(contentUri));
                                    medDb.meditationLocalDao().updateMed(selectedMed);
                                    Log.v(TAG, "Updated meditation: " + selectedMed.toString());
                                }
                            });
                            bar.dismiss();
                            break;
                        default:
                            msg = "Download is nowhere in sight";
                            break;
                    }

                    Log.d(TAG, msg);
                    csr.close();
                }
            }
        }).start();

        //whether successful or not, hide views again
        medAdapter.notifyItemChanged(medPos);

    }

    /**
     * remove the audio file from the device
     *
     * @param medPos      of the meditation in the adapter.
     * @param selectedMed is the selected meditation
     */
    @Override
    public void remove(final int medPos, final MeditationLocal selectedMed) {

        final String removeText = getString((R.string.alert_title), selectedMed.getTitle());

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.dialog);

        // set dialog message
        alertDialogBuilder
                .setTitle(removeText)
                .setMessage(R.string.alert_text)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked proceed with removing file
                        Uri uriToBeRemoved = Uri.parse(selectedMed.getStorage());
                        int i = getContentResolver().delete(uriToBeRemoved, null, null);

                        if (i > -1) {
                            EntryExecutor.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    selectedMed.setStorage(null);
                                    medDb.meditationLocalDao().updateMed(selectedMed);
                                }

                                @Override
                                protected void finalize() throws Throwable {
                                    super.finalize();
                                    medAdapter.notifyItemChanged(medPos);
                                }
                            });
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}