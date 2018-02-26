package com.example.android.popularmoviesstage2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

/**
 * Fragment which will display the settings of the app
 * the change listener needs to be implemented so the summary of the listviews is correct
 */

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        //load preferences from xml
        addPreferencesFromResource(R.xml.movie_preferences);

        //all actions with the preferences here are needed only to set the pref summary for listprefs
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        //needed to get the total count of preferences
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int preferenceCount = preferenceScreen.getPreferenceCount();
        //loop through all the preferences
        for (int i = 0; i < preferenceCount; i++) {
            //Get the preference from the preference screen
            Preference preference = preferenceScreen.getPreference(i);
            //if the preference is list preference set the summary
            if (preference instanceof ListPreference) {
                //get the value of the preference
                String value = sharedPreferences.getString(preference.getKey(), "");//key and def val
                setPreferenceSummary(preference, value);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    //sets the correct preference summary
    //A preference summary shows the current setting in the settings fragment
    //checkboxes have this automatically, this is needed for the list preference
    private void setPreferenceSummary(Preference preference, String value) {
        //check if this is a list preference
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            //get the index of the list preference
            int preferenceIndex = listPreference.findIndexOfValue(value);
            //check if the index is valid
            if (preferenceIndex >= 0) {
                listPreference.setSummary(listPreference.getEntries()[preferenceIndex]);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Preference preference = findPreference(s);
        if (preference != null) {
            if (preference instanceof ListPreference) {
                String value = sharedPreferences.getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            }
        }
    }
}
