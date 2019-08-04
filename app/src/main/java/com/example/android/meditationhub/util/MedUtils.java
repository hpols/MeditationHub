package com.example.android.meditationhub.util;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.ui.PlayerActivity;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MedUtils {

    public static final int CONVERT_DURATION = 0;
    public static final int CONVERT_POSITION = 1;

    public static void displayCoverArt(Bitmap coverArt, ImageView imageView) {
        if (coverArt != null) {
            imageView.setImageBitmap(coverArt);
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }

    public static String getDisplayTime(int duration, boolean displayHours, int convert) {
        long h = TimeUnit.MILLISECONDS.toHours(duration);
        long m = TimeUnit.MILLISECONDS.toMinutes(duration)- TimeUnit.MILLISECONDS.toMinutes(h);
        long s = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(m);

        String format_ms = "%d:%d";
        String format_hms = "%d:%d:%d";
        String output = "00:00:00";

        switch (convert) {
            case CONVERT_DURATION:
                if(h == 0) {
                    output = String.format(Locale.getDefault(), format_ms, m, s);
                    PlayerActivity.displayHours = false;
                } else {
                    output = String.format(Locale.getDefault(), format_hms, h, m, s);
                    PlayerActivity.displayHours = true;
                }
                break;
            case CONVERT_POSITION:
                if(displayHours) {
                    output = String.format(Locale.getDefault(), format_hms, h, m, s);
                } else {
                    output = String.format(Locale.getDefault(), format_ms, m, s);
                }
                break;
        }


        return output;
    }
}
