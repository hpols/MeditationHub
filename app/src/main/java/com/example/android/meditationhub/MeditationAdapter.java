package com.example.android.meditationhub;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.model.MeditationLocalDb;
import com.example.android.meditationhub.ui.PlayerActivity;
import com.example.android.meditationhub.util.Constants;
import com.example.android.meditationhub.util.MedUtils;
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
 * updating itself after any changes.
 */
public class MeditationAdapter extends RecyclerView.Adapter<MeditationAdapter.MeditationVH> {

    private Context ctxt;
    private static FirebaseUser user;

    private List<MeditationLocal> meditations;

    private MeditationLocalDb meditationLocalDb;

    //alert the MainActivity of download related information
    private final AdapterInterface adapterInterface;

    public interface AdapterInterface {
        void download(Uri uri, MeditationLocal filename, int medPos);
    }

    public MeditationAdapter(Context ctxt, FirebaseAuth mAuth, List<MeditationLocal> meditations,
                             AdapterInterface adapterInterface) {
        this.ctxt = ctxt;
        this.meditations = meditations;
        this.adapterInterface = adapterInterface;

        user = mAuth.getCurrentUser();
        meditationLocalDb = MeditationLocalDb.getInstance(ctxt);
    }

    @NonNull
    @Override
    public MeditationVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        View root = LayoutInflater.from(ctxt).inflate(R.layout.meditation_item, viewGroup,
                false);

        root.setFocusable(true);

        return new MeditationVH(root);
    }

    @Override
    public void onBindViewHolder(@NonNull final MeditationVH medVh, int position) {
        final int medPos = position;
        final MeditationLocal selectedMed = meditations.get(position);
        Timber.d("this meditation = " + selectedMed.toString());

        medVh.titleTv.setText(selectedMed.getTitle());
        medVh.subtitleTv.setText(selectedMed.getSubtitle());

        //set Action image and its responses to clicks
        int actionImage;
        if (selectedMed.getStorage() == null) {
            actionImage = android.R.drawable.stat_sys_download;
        } else {
            actionImage = android.R.drawable.ic_media_play;
            medVh.thumbIv.setVisibility(View.VISIBLE);
            medVh.medUri =getUri(selectedMed);

            medVh.coverArt = getCoverArt(medVh.medUri);
            MedUtils.displayCoverArt(medVh.coverArt, medVh.thumbIv);
        }
        medVh.actionIb.setImageResource(actionImage);

        medVh.actionIb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.v("button clicked in recyclerView");
                if (user == null) {
                    Toast.makeText(ctxt, "Please login to use this feature",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (selectedMed.getStorage() == null) {
                        Timber.v("downloaded started");
                        //download file
                        Permissions.check(ctxt, Manifest.permission.WRITE_EXTERNAL_STORAGE, 
                                null, new PermissionHandler() {
                            @Override
                            public void onGranted() {
                                // Create a storage reference from our app
                                StorageReference ref = FirebaseStorage.getInstance().getReference()
                                        .child(selectedMed.getFilename());

                                ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        adapterInterface.download(uri, selectedMed, medPos);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Timber.e(ctxt.getString(R.string.download_error)
                                                + exception.toString());
                                        Toast.makeText(ctxt, ctxt.getString(R.string.download_error),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            @Override
                            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                                super.onDenied(context, deniedPermissions);
                                Toast.makeText(ctxt, ctxt.getString(R.string.download_denied),
                                        Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public boolean onBlocked(Context context, ArrayList<String> blockedList) {
                                Toast.makeText(ctxt, ctxt.getString(R.string.download_blocked),
                                        Toast.LENGTH_LONG).show();
                                return super.onBlocked(context, blockedList);
                            }
                        });

                    } else {
                        //start mediaPlayer
                        goToPlayer(selectedMed, medVh.coverArt, medVh.medUri, medVh.thumbIv, Constants.AUTO_PLAY);
                    }
                }
            }
        });
        medVh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToPlayer(selectedMed, medVh.coverArt, medVh.medUri, medVh.thumbIv, Constants.JUST_OPEN);
            }
        });
    }

    private Uri getUri(MeditationLocal selectedMed) {
        File medPath = new File(Environment.getExternalStorageDirectory(),
                ctxt.getString(R.string.app_name));
        File medFile = new File(medPath, selectedMed.getFilename());
        Uri medUri = FileProvider.getUriForFile(ctxt,
                ctxt.getApplicationContext().getPackageName() + ".file_provider", medFile);
        ctxt.grantUriPermission(ctxt.getApplicationContext().getPackageName(), medUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return medUri;
    }

    private void goToPlayer(MeditationLocal selectedMed, Bitmap coverArt, Uri medUri, ImageView thumbIv, int action) {
        Intent openPlayer = new Intent(ctxt, PlayerActivity.class);
        openPlayer.putExtra(Constants.SELECTED_MED, selectedMed);
        openPlayer.putExtra(Constants.ACTION, action);
        openPlayer.putExtra(Constants.ART, coverArt);
        openPlayer.putExtra(Constants.URI, medUri);

        Bundle transitionBundle = ActivityOptions.makeSceneTransitionAnimation((Activity) ctxt,
                thumbIv, thumbIv.getTransitionName()).toBundle();
        ctxt.startActivity(openPlayer, transitionBundle);
    }

    @Override
    public int getItemCount() {
        return meditations.size();
    }

    /**
     * get the cover art of the meditation (as available)
     * see: //https://stackoverflow.com/a/21549403/7601437
     *
     * @param medUri is the meditation audio in question
     */
    private Bitmap getCoverArt(Uri medUri) {


        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(ctxt, medUri);

        byte[] data = mmr.getEmbeddedPicture();

        if (data != null) {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } else {
            return BitmapFactory.decodeResource(ctxt.getResources(), R.drawable.ic_meditation_hub);
        }
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
        @BindView(R.id.thumb_iv)
        ImageView thumbIv;

        Bitmap coverArt = null;
        Uri medUri;

        MeditationVH(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}