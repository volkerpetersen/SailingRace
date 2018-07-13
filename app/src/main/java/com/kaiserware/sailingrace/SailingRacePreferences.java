package com.kaiserware.sailingrace;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.Hashtable;

/**
 * Activity to update and display the current user "Setting Preferences" for this App
 *
 * Created by Volker Petersen - November 2015.
 */
public class SailingRacePreferences extends PreferenceActivity {
    public static Hashtable<String, String[]> prefs = new Hashtable<String, String[]>();

    public static int FetchPreferenceValue(String key, Context who) {
        final String LOG_TAG = SailingRacePreferences.class.getSimpleName();
        boolean def;

        // the HashTable prefs contains for each key (which equals the Shared Preferences key)
        // a String[] with the 2 items [Preference Summary, Preference Default Value]
        String[] item;
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(who);
        if (prefs.isEmpty()) {
            initializePreferencesHashTable();
        }
        item = prefs.get(key);
        // key type definitions:
        //      boolean, string, int
        // set via the preference.xml file in xml folder
        if (item != null) {
            if (key.equals("key_Warning") || key.equals("key_Polars") || key.equals("key_Windex")) {
                //Log.d(LOG_TAG, "if BOOLEAN key = "+key);
                def = (item[1].equals("1"));
                boolean b = SP.getBoolean(key, def);
                //Log.d(LOG_TAG, "FetchPreferenceValue() - key=" + key+"  item[1]="+item[1]+"  b="+(b ? 1 : 0));
                return (b) ? 1 : 0;
            } else {
                //Log.d(LOG_TAG, "if NON-BOOLEAN key = "+key);
                String value="1";
                try {
                    value = SP.getString(key, item[1]);
                    //Log.d(LOG_TAG, "FetchPreferenceValue() - key=" + key + "  item[1]=" + item[1] + "  value=" + value);
                } catch (Exception e) {
                    Log.d(LOG_TAG, "FetchPreference exception=" + key + "  item[1]=" + item[1] + "  error="+e.getMessage());
                    value = "1";
                }
                return Integer.parseInt(value);
            }
        } else {
            Log.d(LOG_TAG, "FetchPreferenceValue() - Key error, empty array on " + key);
            return 0;
        }
    }

    public static void initializePreferencesHashTable() {
        // Hashtable with values for the TextPreferences
        // make sure that the data in the file preferences.xml jibes with these data definitions
        String[] preference_keys = new String[] {
                "key_StartSequence",
                "key_CourseDistance",
                "key_LeftRightDistance",
                "key_NumberOfLegs",
                "key_ScreenUpdates",
                "key_GPSUpdateTime",
                "key_TackAngle",
                "key_GybeAngle",
                "key_longAvg",
                "key_avgWIND",
                "key_avgGPS",
                "key_Windex",
                "key_Warning",
                "key_RaceClass",
                "key_Polars"};
        String[] pref_summary = new String[] {
                "Subsequent Class starts every INT minutes",  // key_StartSequence
                "W/L distance INT m (1852m = 1nm)",           // key_CourseDistance
                "Course width equals INT% of W/L distance",   // key_LeftRightDistance
                "INT legs on this W/L Course",                // key_NumberOfLegs
                "Screen Updates every INT seconds",           // key_ScreenUpdates
                "New location every INT milli-seconds",       // key_GPSUpdateTime
                "Target Tack Angle INT°",                     // key_TackAngle
                "Target Gybe Angle INT°",                     // key_GybeAngle
                "",                                           // key_longAvg - ListPreference
                "",                                           // key_avgWIND - ListPreference
                "",                                           // key_avgGPS - ListPreference
                "",                                           // key_Windex - ListPreference
                "",                                           // key_Warning - ListPreference
                "",                                           // key_Class - ListPreference
                ""};                                          // key_Polars - ListPreference
        String[] pref_defaults = new String[] {
                "3",    // key_StartSequence
                "1852", // key_CourseDistance
                "20",   // key_LeftRightDistance
                "5",    // key_NumberOfLegs
                "1",    // key_ScreenUpdates (sec)
                "450",  // key_GPSUpdateTime  (milli seconds)
                "40",   // key_TackAngle
                "30",   // key_GybeAngle
                "30",   // key_longAvg
                "1",    // key_avgWIND
                "3",    // key_avgGPS
                "1",    // key_Windex (0, 1, or 2)
                "0",    // key_Warning
                "4",    // key_Class
                "1"};   // key_Polars

        // initialize the HashTable prefs containing for each key (which equals the Shared Preferences key)
        // a String[] with the 2 items [Preference Summary, Preference Default Value]
        for (int i=0; i<preference_keys.length; i++) {
            String[] item = {pref_summary[i], pref_defaults[i]};
            prefs.put(preference_keys[i], item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializePreferencesHashTable();
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        public final String LOG_TAG = MyPreferenceFragment.class.getSimpleName();

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(com.kaiserware.sailingrace.R.xml.preferences);

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

            // initialize the default values for the first time this app runs.
            PreferenceManager.setDefaultValues(getActivity().getApplicationContext(), R.xml.preferences, false);

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            UpdatePreferenceValues(SP);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            UpdatePreferenceValues(sharedPreferences);
        }

        public void UpdatePreferenceValues(SharedPreferences SP) {
            String[] item;
            EditTextPreference summaryValue;
            final boolean polarsEnabled = SP.getBoolean("key_Polars", true);
            final boolean windexEnabled = SP.getBoolean("key_Windex", true);

            getPreferenceScreen().findPreference("key_Polars").setEnabled(windexEnabled);
            getPreferenceScreen().findPreference("key_TackAngle").setEnabled(!windexEnabled);
            getPreferenceScreen().findPreference("key_GybeAngle").setEnabled(!windexEnabled);

            if (windexEnabled && !polarsEnabled) {
                getPreferenceScreen().findPreference("key_TackAngle").setEnabled(true);
                getPreferenceScreen().findPreference("key_GybeAngle").setEnabled(true);
            }

            for(String key: prefs.keySet()){
                item = prefs.get(key);
                if (!item[0].equals("")) {
                    String summary = "";
                    summaryValue = (EditTextPreference) findPreference(key);
                    if (item[0].contains("INT")) {
                        summary = item[0].replace("INT", SP.getString(key, item[1]));
                    }
                    summaryValue.setSummary(summary);
                }
            }
        }

        @Override
        public void onResume() {
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            super.onResume();
        }

        @Override
        public void onPause() {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }
    }
}
