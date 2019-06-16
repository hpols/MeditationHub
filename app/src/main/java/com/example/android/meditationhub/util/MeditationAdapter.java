package com.example.android.meditationhub.util;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.Guideline;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.model.MeditationLocalDb;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * The {@link MeditationAdapter} ensures that the {@link RecyclerView} can display all information,
 * updating itself after any changes
 */
public class MeditationAdapter extends RecyclerView.Adapter<MeditationAdapter.MeditationVH> {

    private Context ctxt;
    private static FirebaseUser user;

    private List<MeditationLocal> meditations;
    private List<String> keys;

    private MeditationLocalDb meditationLocalDb;

    public MeditationAdapter(Context ctxt, FirebaseAuth mAuth, List<MeditationLocal> meditations,
                             List<String> keys) {
        this.ctxt = ctxt;
        this.meditations = meditations;
        this.keys = keys;

        user = mAuth.getCurrentUser();
        meditationLocalDb = MeditationLocalDb.getInstance(ctxt);
    }

    @NonNull
    @Override
    public MeditationVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View root = LayoutInflater.from(ctxt).inflate(R.layout.meditation_item, viewGroup,
                false);
        root.setFocusable(true);

        return new MeditationVH(root);
    }

    @Override
    public void onBindViewHolder(@NonNull final MeditationVH medVh, int i) {
        final MeditationLocal thisMed = meditations.get(i);

        medVh.titleTv.setText(thisMed.getTitle());
        medVh.subtitleTv.setText(thisMed.getSubtitle());

        //set Action image
        int actionImage;
        if (thisMed.getStorage() == null) {
            actionImage = android.R.drawable.stat_sys_download;
        } else {
            actionImage = android.R.drawable.ic_media_play;
        }
        medVh.actionIb.setImageResource(actionImage);

        medVh.actionIb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.v("button clicked in recyclerview");
                if (user == null) {
                    Toast.makeText(ctxt, "Please login to use this feature",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (thisMed.getStorage() == null) {
                        Timber.v("downloaded started");
                        //download file
                        downloadFile(thisMed, medVh);
                        medVh.progressBgV.setVisibility(View.VISIBLE);

                    } else {
                        //start mediaplayer
                    }
                }
            }
        });
    }

    /**
     * download the Meditation to the App-own folder in the External(??) Storage
     *
     * @param thisMed is the current Meditation being downloaded
     * @param medVh             viewholder to pass on to the download manager > updates
     */
    private void downloadFile(final MeditationLocal thisMed, final MeditationVH medVh) {
        //ensure permissions are granted. If not ask user to grant them.
        Permissions.check(ctxt, Manifest.permission.WRITE_EXTERNAL_STORAGE, null, new PermissionHandler() {
            @Override
            public void onGranted() {
                // Create a storage reference from our app
                StorageReference ref = FirebaseStorage.getInstance().getReference().child(thisMed.getFilename());

                final File rootPath = new File(Environment.getExternalStorageDirectory(), "MeditationHub");
                if (!rootPath.exists()) {
                    rootPath.mkdirs();
                }
                ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        downloader(uri, rootPath, thisMed, medVh);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Timber.e("The file could not be retrieved: "
                                + exception.toString());
                        Toast.makeText(ctxt, "An error occurred creating the file",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                super.onDenied(context, deniedPermissions);
                Toast.makeText(ctxt, ctxt.getString(R.string.download_denied), Toast.LENGTH_LONG).show();
            }

            @Override
            public boolean onBlocked(Context context, ArrayList<String> blockedList) {
                Toast.makeText(ctxt, ctxt.getString(R.string.download_blocked), Toast.LENGTH_LONG).show();
                return super.onBlocked(context, blockedList);
            }
        });
    }

    /**
     * setup the download manager
     *
     * @param uri               the location on Firebase of the file
     * @param rootPath          where the file will be stored on the device
     * @param thisMed is the Meditation being downloaded
     * @param medVh             is the viewholder to be passed on for the Ui updating
     */
    private void downloader(Uri uri, File rootPath, MeditationLocal thisMed, final MeditationVH medVh) {
        final DownloadManager dlManager = (DownloadManager)
                ctxt.getSystemService(Context.DOWNLOAD_SERVICE);
        final DownloadManager.Request dlRequest = new DownloadManager.Request(uri);
        dlRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        dlRequest.setDestinationInExternalFilesDir(ctxt, String.valueOf(rootPath), thisMed.getFilename());
        final long dlId = dlManager.enqueue(dlRequest);

        updateProgressToUi(medVh, dlManager, dlId, thisMed);
    }

    /**
     * Keep the user updated on the status of the download on screen.
     * See also: https://stackoverflow.com/questions/15795872/show-download-progress-inside-activity-using-downloadmanager
     *
     * @param medVh             is the viewholder so as to access the progressbar displaying the
     *                          progress of the download in realtime.
     * @param dlManager         the Manager handling the download
     * @param dlId              the id of the download currently being handled.
     * @param thisMed is the Meditation being downloadedr
     */
    private void updateProgressToUi(final MeditationVH medVh, final DownloadManager dlManager,
                                    final long dlId, final MeditationLocal thisMed) {

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
                            msg = "Download failed!";
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
                                final int dlProgress = (int) ((bytesLoaded * 100L) / bytesTotal);

                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        medVh.progressGuide.setGuidelinePercent((float) dlProgress / 100L);
                                        Timber.v("Progress: downloaded: " + bytesLoaded +
                                                " from total: " + bytesTotal + "=" + dlProgress);

                                        //TODO: continue working on UI display in July.
                                    }
                                });
                            }
                            break;

                        case DownloadManager.STATUS_SUCCESSFUL:
                            msg = "Download complete!";
                            final Uri location = dlManager.getUriForDownloadedFile(dlId);
                            EntryExecutor.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    thisMed.setStorage(String.valueOf(location));
                                    meditationLocalDb.meditationLocalDao().updateEntry(thisMed);
                                }
                            });

                            notifyDataSetChanged();
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
    }

    @Override
    public int getItemCount() {
        return meditations.size();
    }

    public static void logout() {
        user = null;
    }

    class MeditationVH extends RecyclerView.ViewHolder {

        @BindView(R.id.title_tv)
        TextView titleTv;
        @BindView(R.id.subtitle_tv)
        TextView subtitleTv;
        @BindView(R.id.action_ib)
        ImageButton actionIb;
        @BindView(R.id.progress_bg_v)
        View progressBgV;
        @BindView(R.id.progress_guide)
        Guideline progressGuide;

        private String key;

        MeditationVH(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}