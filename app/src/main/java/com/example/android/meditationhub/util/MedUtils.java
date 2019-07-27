package com.example.android.meditationhub.util;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.example.android.meditationhub.R;

public class MedUtils {

    public static void displayCoverArt(Bitmap coverArt, ImageView imageView) {
        if (coverArt != null) {
            imageView.setImageBitmap(coverArt);
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }
}
