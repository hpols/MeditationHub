package com.example.android.meditationhub.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.example.android.meditationhub.R;
import com.example.android.meditationhub.model.MeditationLocal;

import java.io.File;

public class MedUtils {

    private static final String TAG = MedUtils.class.getSimpleName();

    static SharedPreferences sharedPref;
    static SharedPreferences.Editor sharedPrefEd;

    public static int noOfCols(Context ctxt) {
        int screenWidth = ctxt.getResources().getConfiguration().screenWidthDp;

        return Math.round(screenWidth / 180);
    }

    public static boolean isInternetAvailable(Context pContext) {
        if (pContext == null) {
            return false;
        }
        ConnectivityManager cm =
                (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static void displayMedInfo(Bitmap coverArt, ImageView image, TextView title,
                                      TextView subtitle, MeditationLocal selectedMed) {

        if (coverArt != null) {
            image.setImageBitmap(coverArt);
            image.setAlpha((float) 1.0);
            title.setVisibility(View.GONE);
            subtitle.setVisibility(View.GONE);
        } else {
            image.setAlpha((float) 0.2);
            image.setImageResource(R.mipmap.ic_launcher);
            title.setVisibility(View.VISIBLE);
            subtitle.setVisibility(View.VISIBLE);
            title.setText(selectedMed.getTitle());
            subtitle.setText(selectedMed.getSubtitle());
        }
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
            return BitmapFactory.decodeResource(ctxt.getResources(), R.drawable.ic_launcher_foreground);
        }

        //TODO: rework so that placeholder images is not set as bitmap (loses transparent bg & gets pixelated
    }


    //https://stackoverflow.com/a/42250183/7601437
    static public long getDuration(Uri medUri, Context ctxt) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(ctxt, medUri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(durationStr);
    }

    /**
     * Function to convert milliseconds time to
     * Timer Format
     * Hours:Minutes:Seconds
     */
    public static String getDisplayTime(long milliseconds, boolean isLongerThanHour) {
        String finalTimerString = "";
        String secondsString, minuteString;

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        // Add hours if there
        if (hours > 0 || isLongerThanHour) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        secondsString = setDoubleDigit(seconds);
        minuteString = setDoubleDigit(minutes);

        finalTimerString = finalTimerString + minuteString + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    private static String setDoubleDigit(int time) {
        String timeString;
        if (time < 10) {
            timeString = "0" + time;
        } else {
            timeString = "" + time;
        }
        return timeString;
    }

    public static int getDelay(Context ctxt) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(ctxt);
        String delayString =
                sharedPref.getString(ctxt.getResources().getString(R.string.pref_key_time_delay),
                        ctxt.getResources().getString(R.string.pref_default_time_delay));
        return Integer.parseInt(delayString) * 1000;
    }

    public static Uri getUri(MeditationLocal selectedMed, Context ctxt) {
        File medPath = new File(Environment.getExternalStorageDirectory(),
                ctxt.getString(R.string.app_name));
        File medFile = new File(medPath, selectedMed.getFilename());
        Uri medUri = FileProvider.getUriForFile(ctxt,
                ctxt.getApplicationContext().getPackageName() + ".file_provider", medFile);
        ctxt.grantUriPermission(ctxt.getApplicationContext().getPackageName(), medUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.v(TAG, "accessing content via uri: " + medUri);
        return Uri.parse(selectedMed.getStorage());
    }
}
