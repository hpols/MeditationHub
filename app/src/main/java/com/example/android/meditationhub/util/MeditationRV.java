package com.example.android.meditationhub.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.model.MeditationLocal;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MeditationRV extends RecyclerView.Adapter<MeditationRV.MeditationVH> {

    private Context ctxt;
    private FirebaseAuth mAuth;
    private static FirebaseUser user;

    private List<MeditationLocal> meditations;
    private List<String> keys;

    public MeditationRV(Context ctxt, List<MeditationLocal> meditations, List<String> keys) {
        this.ctxt = ctxt;
        this.meditations = meditations;
        this.keys = keys;
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
    public void onBindViewHolder(@NonNull MeditationVH medVh, int i) {
        final MeditationLocal currentMeditation = meditations.get(i);
        final boolean isStored;

        medVh.mTitle.setText(currentMeditation.getTitle());
        medVh.subtitleTv.setText(currentMeditation.getSubtitle());

        //set Action image
        int actionImage;
        if (currentMeditation.getStorage() == null) {
            actionImage = android.R.drawable.stat_sys_download;
            isStored = false;
        } else {
            actionImage = android.R.drawable.ic_media_play;
            isStored = true;
        }
        medVh.actionIb.setImageResource(actionImage);

        medVh.actionIb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStored) {
                    //start mediaplayer
                } else {
                    //download file
                }
            }
        });
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
        TextView mTitle;
        @BindView(R.id.subtitle_tv)
        TextView subtitleTv;
        @BindView(R.id.action_ib)
        ImageButton actionIb;

        private String key;

        MeditationVH(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}