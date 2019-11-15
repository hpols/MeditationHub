package com.example.android.meditationhub;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.example.android.meditationhub.model.Header;
import com.example.android.meditationhub.model.ItemList;
import com.example.android.meditationhub.model.MeditationLocal;
import com.example.android.meditationhub.model.MeditationLocalDb;
import com.example.android.meditationhub.ui.PlayActivity;
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

import timber.log.Timber;

/**
 * The {@link MeditationAdapter} ensures that the {@link RecyclerView} can display all information,
 * updating itself after any changes.
 */
public class MeditationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context ctxt;
    private static FirebaseUser user;

    private List<MeditationLocal> meditations;
    private List<ItemList> items;
    private MeditationLocalDb meditationLocalDb;

    //alert the MainActivity of download related information
    private final AdapterInterface adapterInterface;

    // This object helps you save/restore the open/close state of each view
    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();

    public interface AdapterInterface {
        void download(Uri uri, MeditationLocal filename, int medPos);

        void remove(int medPos, MeditationLocal selectedMed);
    }

    public MeditationAdapter(Context ctxt, FirebaseAuth mAuth, List<ItemList> items,
                             AdapterInterface adapterInterface) {
        this.ctxt = ctxt;
        this.items = items;
        this.adapterInterface = adapterInterface;

        user = mAuth.getCurrentUser();
        meditationLocalDb = MeditationLocalDb.getInstance(ctxt);
        viewBinderHelper.setOpenOnlyOne(true); //only allow one viewholder to be swiped at a time
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        if(viewType == ItemList.TYPE_HEADER) {

            View root = LayoutInflater.from(ctxt).inflate(R.layout.header_item, viewGroup,
                    false);
            return new HeaderVH(root);
        } else if (viewType == ItemList.TYPE_ITEM){
            View root = LayoutInflater.from(ctxt).inflate(R.layout.swiping_recycler, viewGroup,
                    false);

            root.setFocusable(true);

            return new MeditationVH(root);
        }
        throw new RuntimeException("there is no type that matches the type " + viewType +
                " make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder Vh, int position) {

        if (Vh instanceof HeaderVH) {
            final HeaderVH headVh = (HeaderVH) Vh;

            Header selectedHeader = (Header) items.get(position);
            headVh.headerTv.setText(selectedHeader.getName());
        } else {
            final MeditationVH medVh = (MeditationVH) Vh;

            //check whether Meditation or header
            final int medPos = position;

            final MeditationLocal selectedMed = (MeditationLocal)items.get(position);

            // Save/restore the open/close state.
            // You need to provide a String id which uniquely defines the data object.
            viewBinderHelper.bind(medVh.swipeRevealLayout, selectedMed.getId());

            Timber.d("this meditation = " + selectedMed.toString());

            String removeText = ctxt.getString((R.string.alert_title), selectedMed.getTitle());
            medVh.alertTv.setText(removeText);

            medVh.titleTv.setText(selectedMed.getTitle());
            medVh.subtitleTv.setText(selectedMed.getSubtitle());

            //set Action image and its responses to clicks
            int actionImage;
            if (selectedMed.getStorage() == null) {
                actionImage = android.R.drawable.stat_sys_download;
                viewBinderHelper.lockSwipe(String.valueOf(medPos));
            } else {
                actionImage = android.R.drawable.ic_media_play;
                medVh.thumbIv.setVisibility(View.VISIBLE);
                medVh.medUri = getUri(selectedMed);
                viewBinderHelper.unlockSwipe(String.valueOf(medPos));

                medVh.coverArt = MedUtils.getCoverArt(medVh.medUri, ctxt);
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
                            goToPlayer(selectedMed, medVh.medUri, medVh.thumbIv);
                        }
                    }
                }
            });
            medVh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToPlayer(selectedMed, medVh.medUri, medVh.thumbIv);
                }
            });
        }
    }

    private Uri getUri(MeditationLocal selectedMed) {
        File medPath = new File(Environment.getExternalStorageDirectory(),
                ctxt.getString(R.string.app_name));
        File medFile = new File(medPath, selectedMed.getFilename());
        Uri medUri = FileProvider.getUriForFile(ctxt,
                ctxt.getApplicationContext().getPackageName() + ".file_provider", medFile);
        ctxt.grantUriPermission(ctxt.getApplicationContext().getPackageName(), medUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Timber.v("accessing content via uri: " + medUri);
        return Uri.parse(selectedMed.getStorage());
    }

    private void goToPlayer(MeditationLocal selectedMed, Uri medUri, ImageView thumbIv) {
        Intent openPlayer = new Intent(ctxt, PlayActivity.class);
        openPlayer.putExtra(Constants.SELECTED_MED, selectedMed);
        openPlayer.putExtra(Constants.URI, medUri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle transitionBundle = ActivityOptions.makeSceneTransitionAnimation((Activity) ctxt,
                    thumbIv, thumbIv.getTransitionName()).toBundle();
            ctxt.startActivity(openPlayer, transitionBundle);
        } else {
            ctxt.startActivity(openPlayer);
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

    public void removeAudio(final int position) {
        MeditationLocal selectedMed = meditations.get(position);
        if (selectedMed.getStorage().isEmpty()) {
            Toast.makeText(ctxt, "Swiping left or right will remove the audio of a meditation from your device. However, this one is not on yur device.", Toast.LENGTH_SHORT).show();
        } else {
            adapterInterface.remove(position, selectedMed);
        }
    }

    public void saveStates(Bundle outState) {
        viewBinderHelper.saveStates(outState);
    }

    public void restoreStates(Bundle inState) {
        viewBinderHelper.restoreStates(inState);
    }

    class HeaderVH extends RecyclerView.ViewHolder {
        TextView headerTv;

        public HeaderVH(@NonNull View itemView) {
            super(itemView);
            headerTv = itemView.findViewById(R.id.header_tv);
        }
    }

    class MeditationVH extends RecyclerView.ViewHolder {

        //@BindView(R.id.title_tv)
        TextView titleTv;
        //@BindView(R.id.subtitle_tv)
        TextView subtitleTv;
        //@BindView(R.id.action_ib)
        ImageButton actionIb;
        //@BindView(R.id.thumb_iv)
        ImageView thumbIv;

        //@BindView(R.id.swipe_layout)
        SwipeRevealLayout swipeRevealLayout;
        //@BindView(R.id.swipe_reveal)
        ConstraintLayout swipeReveal;
        //@BindView(R.id.swipe_main)
        CardView swipeMain;
        //@BindView(R.id.alert_ok_bt)
        Button okBt;
        //@BindView(R.id.alert_cancel_bt)
        Button cancelBt;
        //@BindView((R.id.alert_cb))
        CheckBox alertCb;
        TextView alertTv;

        Bitmap coverArt = null;
        Uri medUri;

        MeditationVH(final View view) {
            super(view);
            //ButterKnife.bind(this, view);

            swipeRevealLayout = itemView.findViewById(R.id.swipe_layout);
            swipeReveal = itemView.findViewById(R.id.swipe_reveal);
            swipeMain = itemView.findViewById(R.id.swipe_main);

            titleTv = itemView.findViewById(R.id.title_tv);
            subtitleTv = itemView.findViewById(R.id.subtitle_tv);
            actionIb = itemView.findViewById(R.id.action_ib);
            thumbIv = itemView.findViewById(R.id.thumb_iv);

            okBt = itemView.findViewById(R.id.alert_ok_bt);
            cancelBt = itemView.findViewById(R.id.alert_cancel_bt);
            alertCb = itemView.findViewById(R.id.alert_cb);
            alertTv = itemView.findViewById(R.id.alert_tv);

            okBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeAudio(getAdapterPosition());
                }
            });
            cancelBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    swipeRevealLayout.close(true);
                }
            });
            alertCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctxt);
                    final SharedPreferences.Editor sharedPrefEd = sharedPref.edit();
                    final String ALERT_NEEDED = "alert needed";
                    if (isChecked) {//show alert again next time
                        sharedPrefEd.putBoolean(ALERT_NEEDED, true);
                    } else { //no longer show alert
                        sharedPrefEd.putBoolean(ALERT_NEEDED, false);
                    }
                    sharedPrefEd.apply();
                }
            });
        }

    }
}