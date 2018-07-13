package com.kaiserware.sailingrace;

import android.app.Fragment;

/**
 * Helper Class to facilitate the changing of a Fragment from within another Fragment.
 *
 * This App is using this Helper Class in the Fragment_StartSequence to allow the listening to
 * the button "WWD Mark" to switch to the Fragment_DisplayGoogleMap to set the Windward Mark.
 *
 * See replaceFragment() in Activity_StartSequence.java
 *
 * Created by Volker Petersen on 4/4/2017.
 */
public interface HelperClass_FragmentChangeListener {
    public void replaceFragment(Fragment fragment);
}
