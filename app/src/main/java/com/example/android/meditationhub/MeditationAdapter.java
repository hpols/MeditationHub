package com.example.android.meditationhub;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.meditationhub.model.Header;
import com.example.android.meditationhub.model.ItemList;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.util.MedUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link MeditationAdapter} ensures that the {@link RecyclerView} can display all information,
 * updating itself after any changes.
 */
public class MeditationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = MeditationAdapter.class.getSimpleName();

    private Context ctxt;
    private static FirebaseUser user;

    private List<MeditationLocal> meditations;
    private List<ItemList> items;

    //keep intouch with MainActivity through this interface (clicks, downloads, removes)
    private final AdapterInterface adapterInterface;

    public interface AdapterInterface {
        void goToPlayer(MeditationLocal selectedMed, Uri medUri, ImageView thumbIv, boolean play);

        void download(Uri uri, MeditationLocal selevtedMed, int medPos);

        void remove(int medPos, MeditationLocal selectedMed);
    }

    public MeditationAdapter(Context ctxt, FirebaseAuth mAuth, List<ItemList> items,
                             AdapterInterface adapterInterface) {
        this.ctxt = ctxt;
        this.items = items;
        this.adapterInterface = adapterInterface;

        user = mAuth.getCurrentUser();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        if (viewType == ItemList.TYPE_HEADER) {

            View root = LayoutInflater.from(ctxt).inflate(R.layout.header_item, viewGroup,
                    false);
            return new HeaderVH(root);
        } else if (viewType == ItemList.TYPE_ITEM) {

            View root = LayoutInflater.from(ctxt).inflate(R.layout.meditation_item, viewGroup,
                    false);

            root.setFocusable(true);

            return new MeditationVH(root);
        }
        throw new RuntimeException("there is no type that matches the type " + viewType +
                " make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder Vh, final int position) {

        if (Vh instanceof HeaderVH) {
            final HeaderVH headVh = (HeaderVH) Vh;

            Header selectedHeader = (Header) items.get(position);
            headVh.headerTv.setText(selectedHeader.getName());
        } else {
            final MeditationVH medVh = (MeditationVH) Vh;

            //check whether Meditation or header
            final int medPos = position;

            final MeditationLocal selectedMed = (MeditationLocal) items.get(position);

            Log.d(TAG, "this meditation = " + selectedMed.toString());

            final String removeText = ctxt.getString((R.string.alert_title),
                    selectedMed.getTitle());
            //medVh.alertTv.setText(removeText);

            //set Action image and its responses to clicks
            int actionImage = android.R.drawable.stat_sys_download;
            if (selectedMed.getStorage() == null) {
                medVh.thumbIv.setAlpha((float) 0.2);
                medVh.thumbIv.setImageResource(R.drawable.ic_meditation_hub);

                medVh.durationTv.setVisibility(View.INVISIBLE);
                medVh.titleTv.setVisibility(View.VISIBLE);
                medVh.subtitleTv.setVisibility(View.VISIBLE);
                medVh.titleTv.setText(selectedMed.getTitle());
                medVh.subtitleTv.setText(selectedMed.getSubtitle());

            } else {
                actionImage = android.R.drawable.ic_media_play;
                medVh.thumbIv.setAlpha((float) 1.0);
                medVh.medUri = MedUtils.getUri(selectedMed, ctxt);

                medVh.coverArt = MedUtils.getCoverArt(medVh.medUri, ctxt);
                MedUtils.displayMedInfo(medVh.coverArt, medVh.thumbIv, medVh.titleTv,
                        medVh.subtitleTv, selectedMed);

                medVh.duration = MedUtils.getDuration(medVh.medUri, ctxt);
                medVh.durationTv.setText(MedUtils.getDisplayTime(medVh.duration, false));
            }
            medVh.actionIb.setImageResource(actionImage);

            medVh.actionIb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "button clicked in recyclerView");
                    if (user == null) {
                        Toast.makeText(ctxt, "Please login to use this feature",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        if (selectedMed.getStorage() == null) {
                            if (!MedUtils.isInternetAvailable(ctxt)) {
                                Toast.makeText(ctxt, "You are currently offline. Go online to download.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            Log.v(TAG, "downloaded started");
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
                                                    Log.e(TAG, ctxt.getString(R.string.download_error)
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
                            adapterInterface.goToPlayer(selectedMed, medVh.medUri, medVh.thumbIv, true);
                        }
                    }
                }
            });

            medVh.thumbIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (selectedMed.getStorage() != null) {
                        adapterInterface.goToPlayer(selectedMed, medVh.medUri, medVh.thumbIv, false);
                    } else {
                        Snackbar downloadSnack = Snackbar
                                .make(medVh.durationTv, "This meditation has not been downloaded.", Snackbar.LENGTH_SHORT)
                                .setAction("Download Now", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        adapterInterface.download(medVh.medUri, selectedMed, medPos);
                                    }
                                });
                        downloadSnack.show();
                    }
                }
            });

            medVh.thumbIv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    removeAudio(position, selectedMed);
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getItemType();
    }

    public static void logout() {
        user = null;
    }

    public void removeAudio(final int position, MeditationLocal selectedMed) {
        if (selectedMed.getStorage().isEmpty()) {
            Toast.makeText(ctxt, "A long click will remove the audio of a meditation from your device. However, this one is not on yur device.", Toast.LENGTH_SHORT).show();
        } else {
            adapterInterface.remove(position, selectedMed);
        }
    }

    class HeaderVH extends RecyclerView.ViewHolder {
        TextView headerTv;

        public HeaderVH(@NonNull View itemView) {
            super(itemView);
            headerTv = itemView.findViewById(R.id.header_tv);
        }
    }

    class MeditationVH extends RecyclerView.ViewHolder {

        TextView titleTv, subtitleTv, durationTv;
        ImageButton actionIb;
        ImageView thumbIv;

        Bitmap coverArt = null;
        Uri medUri;
        long duration;

        MeditationVH(final View view) {
            super(view);

            DisplayMetrics displaymetrics = new DisplayMetrics();
            ((Activity) ctxt).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

            view.getLayoutParams().width = displaymetrics.widthPixels / MedUtils.noOfCols(ctxt);

            titleTv = itemView.findViewById(R.id.title_tv);
            subtitleTv = itemView.findViewById(R.id.subtitle_tv);
            durationTv = itemView.findViewById(R.id.duration_tv);
            actionIb = itemView.findViewById(R.id.action_ib);
            thumbIv = itemView.findViewById(R.id.thumb_iv);

        }

    }
}