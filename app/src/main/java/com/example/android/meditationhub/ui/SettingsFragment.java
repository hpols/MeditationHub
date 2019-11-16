package com.example.android.meditationhub.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.android.meditationhub.R;

public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private static final int ON_DO_NOT_DISTURB_CALLBACK_CODE = 888;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEd;

    /**
     * create the Fragment, set the preference summaries and ensure the sound preference is only
     * active when the sound callback is selected
     *
     * @param savedInstanceState retaining information as needed
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.menu_settings);

        sharedPref = getPreferenceScreen().getSharedPreferences();
        sharedPrefEd = sharedPref.edit();

        PreferenceScreen prefScreen = getPreferenceScreen();
        int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = prefScreen.getPreference(i);
            if (!(pref instanceof SwitchPreference)) {
                setPrefSummary(pref, sharedPref.getString(pref.getKey(), ""));
            }
        }
    }

    /**
     * set the preference summary
     *
     * @param pref  is the preference in question
     * @param value is the value to be displayed in the summary
     */
    private void setPrefSummary(Preference pref, Object value) {
        String stringValue = value.toString();

        if (pref instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPref = (ListPreference) pref;
            int prefIndex = listPref.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                pref.setSummary(listPref.getEntries()[prefIndex]);
            }
        } else if (pref instanceof EditTextPreference) {
            // For other preferences, set the summary to the value's simple string representation.
            pref.setSummary("seconds delayed: " + stringValue);
        }
    }

    /**
     * Update summaries and activation of the sound preference
     *
     * @param sharedPref holds the information of the preference just changed
     * @param key        is the key of the preference just changed
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {

        final Preference preference = findPreference(key);

        if (key.equals(getString(R.string.pref_key_time_delay))) {
            preference.setSummary(sharedPref.getString(key,
                    getString(R.string.pref_key_time_delay)));
        }
        if (key.equals(getString(R.string.pref_dnd_switch_key))) {

            try {
                if (Build.VERSION.SDK_INT < 23) {
                    AudioManager audioManager = (AudioManager) getActivity().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                } else {
                    this.requestDNDForApi23AndUp();
                }
            } catch (SecurityException e) {
                Log.e(TAG, String.valueOf(e));
            }
        }
    }


    private void requestDNDForApi23AndUp() {

        NotificationManager notificationManager = (NotificationManager)
                getActivity().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // if user granted access else ask for permission
        if (Build.VERSION.SDK_INT >23 && notificationManager.isNotificationPolicyAccessGranted()) {

        } else {
            // Open Setting screen to ask for permisssion
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivityForResult(intent, ON_DO_NOT_DISTURB_CALLBACK_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ON_DO_NOT_DISTURB_CALLBACK_CODE) {
            this.requestDNDForApi23AndUp();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
