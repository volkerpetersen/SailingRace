package com.kaiserware.sailingrace;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Activity to setup the start timer, run the start timer, and to set windward and Leeward positions
 * in preparation for the race.  User can either use the "Back" button to return to the "MainActivity"
 * or this Activity will terminate upon end of countdown timer and launch the "Activity_RaceInfo".
 *
 * This Activity manages different screen sizes utilizing a Single-Pane / Dual-Pane mode utilizing
 * the Fragments "Fragment_DisplayGoogleMap" (called from Button press) and "Fragment_StartSequence".
 * See Udacity "Developing Android Apps Lesson 5" or "developer.android.com/training/basics/fragments/fragment-ui.html"
 *
 * On large devices, we utilize a Dual-Pane display mode with these screens:
 *
 * |----------------------------|------------------------------|
 * |                            |                              |
 * |  numerical display         |  start-line map using        |
 * |  using                     |  Fragment_DisplayGoogleMap   |
 * |  Fragment_StartSequence    |                              |
 * |                            |  or                          |
 * |                            |                              |
 * |                            |  set WWD Mark using          |
 * |                            |  Fragment_DisplayGoogleMap   |
 * |                            |                              |
 * |----------------------------|------------------------------|
 *
 * On small devices (smart phone) the same fragment displays are available by swiping
 * left or right on the initial/default numerical display (Fragment_StartSequence)
 *
 * All angles used in computations are assumed to be True North.  Angles displayed on screen are 
 * measured in Magnetic North (True North + declination) except for TWA and AWA which are the difference
 * between the Boat heading and the Wind Direction!
 * 
 * All speeds are measure in Nautical Miles per Hour (kts = nm/hr)
 *
 * Created by Volker Petersen on November 2015.
 *
 */
public class Activity_StartSequence extends FragmentActivity implements HelperClass_FragmentChangeListener {
    private GestureDetectorCompat swipeDetector;    // swipe detector listener in Class file swipeDetector
    public static boolean DualPane;
    public static boolean timer_terminated;
    public Context appContext;
    private GlobalParameters para;
    private Fragment_DisplayGoogleMap plot_startline_fragment;
    private Fragment_StartSequence start_fragment;
    private FragmentTransaction transaction;
    public static final String MAP_START_LINE_FRAGMENT_TAG = "mapStartLineFRAGMENT";
    public static final String STARTFRAGMENT_TAG = "startFRAGMENT";
    static final String LOG_TAG = Activity_StartSequence.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dual_pane);
        // activity_dual_pane is a generic layout xml that is used in the 2 Activities
        // "Activity_StartSequence" and "Activity_RaceInfo"

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        appContext = getApplicationContext();
        timer_terminated = false;   // gets set to true when the timer in "Fragment_StartSequence" reaches zero

        // initialize the Global Parameter singleton class
        para = GlobalParameters.getInstance();

        int WINDEX = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext);
        // boolean value to indicate if app is using / not using the Bluetooth connected Windex

        if (WINDEX >= 1) {
            // Instantiate the gesture detector with the application context and an implementation
            // of GestureDetector.OnGestureListener when we have an Wind Anemometer
            swipeDetector = new GestureDetectorCompat(appContext, new Activity_StartSequence.MySwipeListener());
        }

        if(findViewById(R.id.fragment_map_display_container) != null) {
            // if this id is present, we're in a two-pane mode on a tablet which uses the
            // layout file res/layout-sw540dp/fragment_start_sequence.xmlxml
            DualPane = true;
        } else {
            // if this id is not present, we're in a one-pane mode on a phone which uses the
            // layout file res/layout/fragment_start_sequencence.xml
            DualPane = false;
        }
        /**
        Configuration config = getResources().getConfiguration();
        if (config.smallestScreenWidthDp >= 600) {
            Log.d(LOG_TAG, "Found Tablet Device with sw = "+config.smallestScreenWidthDp+"dp");
        } else {
            Log.d(LOG_TAG, "Found a Smartphone with sw = "+config.smallestScreenWidthDp+"dp");
        }
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Log.d(LOG_TAG, "Found Two-Pane mode = "+DualPane);
        Log.d(LOG_TAG, "Found "+MAP_START_LINE_FRAGMENT_TAG+" = "+(findViewById(R.id.fragment_map_display_container) != null));
        Log.d(LOG_TAG, "Found "+STARTFRAGMENT_TAG+" = "+(findViewById(R.id.fragment_numerical_display_container) != null));
        Log.d(LOG_TAG, "savedInstanceState  = "+(savedInstanceState==null));
        Log.d(LOG_TAG, "Found Pixel Height = "+metrics.heightPixels);
        Log.d(LOG_TAG, "Found Pixel Width  = "+metrics.widthPixels);
        Log.d(LOG_TAG, "Found Pixel Width  = "+metrics.widthPixels);
        */

        if (savedInstanceState == null) {
            plot_startline_fragment = new Fragment_DisplayGoogleMap();
            plot_startline_fragment.setArguments(getIntent().getExtras());

            start_fragment = new Fragment_StartSequence();
            start_fragment.setArguments(getIntent().getExtras());

            if (DualPane) {
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_numerical_display_container, start_fragment, STARTFRAGMENT_TAG);
                transaction.addToBackStack(STARTFRAGMENT_TAG);
                transaction.replace(R.id.fragment_map_display_container, plot_startline_fragment, MAP_START_LINE_FRAGMENT_TAG);
                transaction.addToBackStack(MAP_START_LINE_FRAGMENT_TAG);
                transaction.commit();
            } else {
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_numerical_display_container, start_fragment, STARTFRAGMENT_TAG);
                transaction.addToBackStack(STARTFRAGMENT_TAG);
                transaction.commit();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // set screen time out to on
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (DualPane) {
            //Log.d(LOG_TAG, "Found Tablet Device with sw = "+config.smallestScreenWidthDp+"dp");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            //Log.d(LOG_TAG, "Found a Smartphone with sw = "+config.smallestScreenWidthDp+"dp");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // set screen time out to on
    }

    /**
     * Method to offer three different options on UI Back Key press.
     * a) when the Start Timer has run down to zero (timer_terminated = true) terminate this Activity
     *    and launch the "Activity_RaceInfo"
     * b) terminate this activity when there is only the original fragment "Fragment_StartSequence"
     *    on the fragment BackStack and the user confirms the desire to terminate this Activity
     *
     * c) if a) doesn't apply, pop the last fragment off the BackStack and display the second to last one
     */
    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        int count = fm.getBackStackEntryCount();

        /**
        for(int entry = 0; entry < count; entry++){
            Log.i(LOG_TAG, "onBackPressed() - Found fragment: " + fm.getBackStackEntryAt(entry).getName());
        }
        */

        if (count > 1) {
            // pop the current Fragment from the BackStack
            //Log.d(LOG_TAG, "onBackPressed() - popping from BackStack = " + count);
            transaction = getFragmentManager().beginTransaction();
            fm.popBackStack();
            Button WL_Course_Set = (Button) findViewById(R.id.button_set_WL_Course);
            if (WL_Course_Set!=null) {
                if (para.getWindwardFlag()) {
                    WL_Course_Set.setText("W/L CLR");
                    WL_Course_Set.setTextColor(ContextCompat.getColorStateList(appContext, R.color.RED));
                } else {
                    WL_Course_Set.setText("Set W/L");
                    WL_Course_Set.setTextColor(ContextCompat.getColorStateList(appContext, R.color.WHITE));
                }
            }
        }

        //Log.d(LOG_TAG, "onBackPressed() - timer_terminated="+timer_terminated+"  BackStackCount="+count);
        if (timer_terminated) {
            // timer_terminated is set to TRUE in "Start_Fragment" when the timer countdown reached zero
            // and the app needs to go straight from "Activity_StartSequence" to "start_raveActivity"
            startActivity(new Intent(appContext, Activity_RaceInfo.class));
            finish();
        } else {
            // if the user hit the Back-key prior to the countdown being finished, "timer_terminted" is
            // FALSE and we check if the user only wants to return from one of the other Fragments when
            // in Single Pane mode or user wants to truely terminate the "Fragment_StartSequence"
            //
            // While in Single Pane mode, user might have used the swipe action to see the "Fragment_StartLineMap"
            // or initiated the "Fragment_DisplayGoogleMap" to set the Windward Mark.  In that case the
            // fragment BackStack count is greater than one and we don't confirm if user truly wants to
            // quit this activity
            if (count <= 1) {
                confirmQuit();
            }
        }
    }

    /**
     * Utilizes the HelperClass_FragmentChangeListener to listen to the fragment "Fragment_StartSequence"
     * if the UI detects the requirement to switch between the numerical display and the Google Map display.
     *
     * This method and the associated Helper Class are being utilized by "Fragment_StartSequence" in the
     * Single Pane mode only.  To ensure that this Helper Class is not used in another context, we're
     * verifying that the Single Pane "fragment_numerical_display_container" can be found.
     *
     * @param fragment the fragment object is determined and passed in by the HelperClass_FragmentChangeListener
     */
    @Override
    public void replaceFragment(Fragment fragment) {
        int containerID = (R.id.fragment_numerical_display_container);

        if (findViewById(containerID) != null) {
            //Fragment maps_fragment = new Fragment_DisplayGoogleMap();
            Fragment maps_fragment = new Fragment_Set_WL_Course();
            maps_fragment.setArguments(getIntent().getExtras());

            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(containerID, maps_fragment, MAP_START_LINE_FRAGMENT_TAG);
            transaction.addToBackStack(MAP_START_LINE_FRAGMENT_TAG);
            transaction.commit();
        }
    }


    /**
     * Initialize Touch Events to be able to detect UI Screen Swipes
     *
     * @param "MotionEvent" event
     * @return onTouchEvent
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.swipeDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    /**
     * MySwipeListener Method to detect Swipe Gestures.  This class requires the above
     * method "onTouchEvent"
     */
    class MySwipeListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onDown(MotionEvent event) {
            //Log.d(LOG_TAG,"Found Motion Event: " + event.toString());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final int SWIPE_MIN_DISTANCE = 120;
            final int SWIPE_MAX_OFF_PATH = 200;
            final int SWIPE_THRESHOLD_VELOCITY = 200;
            int swipe = 0;
            try {
                float diffAbs = Math.abs(e1.getY() - e2.getY());
                float diff = e1.getX() - e2.getX();

                if (diffAbs > SWIPE_MAX_OFF_PATH)
                    return false;

                // Left swipe
                if (diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    swipe = -1;

                    // Right swipe
                } else if (-diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    swipe = +1;
                }
            } catch (Exception e) {
                Log.e("YourActivity", "Error on gestures");
            }
            if (swipe!=0) {
                changeActivityOnSwipe(swipe);
            }
            return false;
        }
    }

    /**
     * changeActivityOnSwipe Method executes the desired action after the Class "swipeDetector"
     * detected a left or right swipe.  In this case we'll change to the Fragment_DisplayGoogleMap on swipe detection
     */
    public void changeActivityOnSwipe(int swipe) {
        //Log.d(LOG_TAG, "DETECTED SWIPE - Need to Implement Maps display / update logic here!");
        if (DualPane) {
            // Large device two-pane mode
            // TODO update the map in the Map pane
        } else {
            // Small device single-pane mode
            // Replace whatever is in the fragment_container view within this fragment,
            // and add the transaction to the back stack so the user can navigate back

            if (swipe == -1) {
                transaction = getFragmentManager().beginTransaction();
                transaction.add(R.id.fragment_numerical_display_container, plot_startline_fragment, MAP_START_LINE_FRAGMENT_TAG);
                transaction.addToBackStack(MAP_START_LINE_FRAGMENT_TAG);
                transaction.commit();
            }
        }
    }
    /**
     * confirmQuit Method handles the "Quit" confirmation so that the user doesn't quit the start
     * timer by accidentally hitting the back button
     */
    private void confirmQuit() {
        AlertDialog alert;

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this,android.R.style.Theme_Holo_Dialog));
        builder.setMessage(R.string.QuitTimer);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Go ahead quit this App
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog. Go ahead and continue the app
                onResume();
            }
        });
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.mipmap.ic_launcher);
        alert=builder.create();
        alert.show();
    }
}

