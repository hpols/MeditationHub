package com.example.android.meditationhub.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.example.android.meditationhub.MeditationAdapter;
import com.example.android.meditationhub.R;

/**
 * based on: https://gist.github.com/keinix/b1aa2417dbea9311a1207eddf8b9d47b
 */
public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private MeditationAdapter medAdapter;

    private Drawable deleteIc;
    private final ColorDrawable bg;


    public SwipeToDeleteCallback(MeditationAdapter medAdapter, Context ctxt) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.medAdapter = medAdapter;
        deleteIc = ContextCompat.getDrawable(ctxt, android.R.drawable.stat_sys_upload_done);
        deleteIc.setColorFilter(ctxt.getResources().getColor(R.color.colorAccent), PorterDuff.Mode.ADD);
        bg = new ColorDrawable(ctxt.getResources().getColor(R.color.colorPrimary));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recView, @NonNull RecyclerView.ViewHolder vH,
                          @NonNull RecyclerView.ViewHolder target) {
        // used for up and down movements. Not needed here
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder vH, int direction) {
        int position = vH.getAdapterPosition();
        medAdapter.deleteTask(position);
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recView,
                            @NonNull RecyclerView.ViewHolder vH, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(canvas, recView, vH, dX, dY, actionState, isCurrentlyActive);

        View itemView = vH.itemView;
        int backgroundCornerOffset = 20; //so bg is behind the rounded corners of itemView

        int iconMargin = (itemView.getHeight() - deleteIc.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIc.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + deleteIc.getIntrinsicHeight();

        if (dX > 0) { // Swiping to the right
            int iconLeft = itemView.getLeft() + iconMargin + deleteIc.getIntrinsicWidth();
            int iconRight = itemView.getLeft() + iconMargin;
            deleteIc.setBounds(iconRight, iconTop, iconLeft, iconBottom);

            bg.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + ((int) dX) + backgroundCornerOffset, itemView.getBottom());
        } else if (dX < 0) { // Swiping to the left
            int iconLeft = itemView.getRight() - iconMargin - deleteIc.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            deleteIc.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            bg.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                    itemView.getTop(), itemView.getRight(), itemView.getBottom());
        } else { // view is unSwiped
            bg.setBounds(0, 0, 0, 0);
        }

        bg.draw(canvas);
        deleteIc.draw(canvas);
    }
}
