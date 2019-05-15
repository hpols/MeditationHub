package com.example.android.meditationhub.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.model.Meditation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RvConfig {
    private Context ctxt;
    private MeditationRV meditationRV;
    private FirebaseAuth mAuth;
    private static FirebaseUser user;

    public void setConfig(RecyclerView recyclerView, Context ctxt, List<Meditation> meditations, List<String> keys) {
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        this.ctxt = ctxt;
        meditationRV = new MeditationRV(meditations, keys);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctxt));
        recyclerView.setAdapter(meditationRV);
    }


    class MeditationRV extends RecyclerView.Adapter<BookItemView> {

        private List<Meditation> meditations;
        private List<String> keys;

        public MeditationRV(List<Meditation> meditations, List<String> keys) {
            this.meditations = meditations;
            this.keys = keys;
        }

        @NonNull
        @Override
        public BookItemView onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View root = LayoutInflater.from(ctxt).inflate(R.layout.meditation_item, viewGroup,
                    false);
            root.setFocusable(true);

            return new BookItemView(root);
        }

        @Override
        public void onBindViewHolder(@NonNull BookItemView bookItemView, int i) {
            final Meditation currentMeditation = meditations.get(i);

            bookItemView.mTitle.setText(currentMeditation.getTitle());
            bookItemView.subtitleTv.setText(currentMeditation.getSubtitle());

            //set Action image
            int actionImage;
            if(currentMeditation.getLocation().contains("https://")) {
                actionImage = android.R.drawable.stat_sys_download;
            } else {
                actionImage = android.R.drawable.ic_media_play;
            }
            bookItemView.actionIb.setImageResource(actionImage);
        }

        @Override
        public int getItemCount() {
            return meditations.size();
        }
    }

    public static void logout() {
        user = null;
    }

    class BookItemView extends RecyclerView.ViewHolder {

        @BindView(R.id.title_tv)
        TextView mTitle;
        @BindView(R.id.subtitle_tv)
        TextView subtitleTv;
        @BindView(R.id.action_ib)
        ImageButton actionIb;

        private String key;

        BookItemView(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}