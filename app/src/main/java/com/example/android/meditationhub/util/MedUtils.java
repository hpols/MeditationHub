package com.example.android.meditationhub.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.widget.ImageView;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.ui.PlayerActivity;

import java.util.Locale;

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

    public static String getDisplayTime(int millis, boolean displayHours, int convert) {
        StringBuffer buf = new StringBuffer();

        int hours = millis / (1000*60*60);
        int minutes = ( millis % (1000*60*60) ) / (1000*60);
        int seconds = ( ( millis % (1000*60*60) ) % (1000*60) ) / 1000;

        buf
                .append(String.format(Locale.getDefault(), "%02d", hours))
                .append(":")
                .append(String.format(Locale.getDefault(), "%02d", minutes))
                .append(":")
                .append(String.format(Locale.getDefault(), "%02d", seconds));

        return buf.toString();
    }

    /**
     * get the cover art of the meditation (as available)
     * see: //https://stackoverflow.com/a/21549403/7601437
     *
     * @param medUri is the meditation audio in question
     */
    static public Bitmap getCoverArt(Uri medUri, Context ctxt) {

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(ctxt, medUri);

        byte[] data = mmr.getEmbeddedPicture();

        if (data != null) {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } else {
            return BitmapFactory.decodeResource(ctxt.getResources(), R.drawable.ic_meditation_hub);
        }
    }

    public static int getPlaybackControl() {
        int image;
        if (PlayerActivity.isPlaying) {
            image = android.R.drawable.ic_media_pause;
        } else {
            image =  android.R.drawable.ic_media_play;
        }
        return image;
    }
}
