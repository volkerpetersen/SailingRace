package com.kaiserware.sailingrace;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Class/Activity to manage all screen updates, location updates, and computations during
 * the race.  This class supports race to Windward and Leeward marks (one each) and a
 * manual wind mode (no Windex instrument) and a Windex instrument supported race mode.
 * This Activity can be launched from either the "Activity_Main" or the "Activity_StartSequence"
 * when the countdown timer has reached zero and the race starts.
 *
 * All angles used in computations are assumed to be True North.  Angles displayed on screen are 
 * measured in Magnetic North (True North + declination) except for TWA and AWA which are the difference
 * between the Boat heading and the Wind Direction!
 * 
 * All speeds are measure in Nautical Miles per Hour (kts = nm/hr)
 *
 * This Activity manages different screen sizes utilizing a One-Pane / Dual-Pane mode utilizing
 * the Fragments "Fragment_RaceInfo", "Fragment_DisplayGoogleMap" and "Fragment_PlotWindChart".
 * See Udacity "Developing Android Apps Lesson 5" or "developer.android.com/training/basics/fragments/fragment-ui.html"
 *
 * On large devices, we utilize a Dual-Pane display mode with these screens:
 *
 * |----------------------------|------------------------------|
 * |                            |                              |
 * |  numerical display         |  race course map using       |   swipe on numerical display
 * |  using                     |  Fragment_DisplayGoogleMap   |   will cycle thru the Fragments
 * |  Fragment_RaceInfo         |                              |   on the right-hand side.
 * |                            |  or                          |
 * |                            |                              |
 * |                            |  TWD plot using              |
 * |                            |  Fragment_PlotWindChart      |
 * |                            |                              |
 * |----------------------------|------------------------------|
 *
 * On small devices (smart phone) the same fragmnet display are available by swiping
 * left or right on the initial/default numerical display (Fragment_RaceInfo)
 *
 * Created by Volker Petersen on November 2015.
 *
 */
public class Activity_RaceInfo extends FragmentActivity {
    public static final String LOG_TAG = Activity_RaceInfo.class.getSimpleName();
    public static final String MAPSFRAGMENT_TAG = "mapsFRAGMENT";
    public static final String PLOTFRAGMENT_TAG = "plotFRAGMENT";
    public static final String RACEFRAGMENT_TAG = "raceFRAGMENT";
    private GestureDetectorCompat swipeDetector;// swipe detector listener in Class file swipeDetector
    public Context appContext;
    private Configuration config;
    private boolean TwoPane;
    private GlobalParameters para;
    private Fragment_DisplayGoogleMap maps_fragment;
    private Fragment_PlotWindChart plot_fragment;
    private Fragment_RaceInfo race_fragment;
    private FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dual_pane);
        // activity_dual_pane is a generic layout xml that is used in the 2 Activities
        // "Activity_StartSequence" and "Activity_RaceInfo"

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        appContext = getApplicationContext();

        // initialize our Global Parameter singleton class
        para = GlobalParameters.getInstance();

        // Instantiate the gesture detector with the application context and
        // an implementation of GestureDetector.OnGestureListener
        swipeDetector = new GestureDetectorCompat(appContext, new MySwipeListener());

        if(findViewById(R.id.fragment_map_display_container) != null) {
            // if this id is present, we're in a two-pane mode on a tablet which uses the
            // layout file res/layout-sw540dp/activity_dual_pane.xmlxml
            TwoPane = true;
        } else {
            // if this id is not present, we're in a one-pane mode on a phone which uses the
            // layout file res/layout/activity_dual_pane.xmlxml
            TwoPane = false;
        }

        config = getResources().getConfiguration();
        /**
         if (config.smallestScreenWidthDp >= 600) {
             if (BuildConfig.DEBUG)
                 Log.d(LOG_TAG, "Found Tablet Device with sw = "+config.smallestScreenWidthDp+"dp");
         } else {
             if (BuildConfig.DEBUG)
                 Log.d(LOG_TAG, "Found a Smartphone with sw = "+config.smallestScreenWidthDp+"dp");
         }
         DisplayMetrics metrics = new DisplayMetrics();
         getWindowManager().getDefaultDisplay().getMetrics(metrics);
         if (BuildConfig.DEBUG) {
             Log.d(LOG_TAG, "Found Two-Pane mode = "+DualPane);
             Log.d(LOG_TAG, "Found "+MAP_START_LINE_FRAGMENT_TAG+" = "+(findViewById(R.id.fragment_map_display_container) != null));
             Log.d(LOG_TAG, "Found "+RACEFRAGMENT_TAG+" = "+(findViewById(R.id.fragment_numerical_display_container) != null));
             Log.d(LOG_TAG, "savedInstanceState  = "+(savedInstanceState==null));
             Log.d(LOG_TAG, "Found Pixel Height = "+metrics.heightPixels);
             Log.d(LOG_TAG, "Found Pixel Width  = "+metrics.widthPixels);
             Log.d(LOG_TAG, "Found Pixel Width  = "+metrics.widthPixels);
         }
         */

        if (savedInstanceState == null) {
            maps_fragment = new Fragment_DisplayGoogleMap();
            maps_fragment.setArguments(getIntent().getExtras());

            plot_fragment = new Fragment_PlotWindChart();
            plot_fragment.setArguments(getIntent().getExtras());

            race_fragment = new Fragment_RaceInfo();
            race_fragment.setArguments(getIntent().getExtras());

            if (TwoPane) {
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_numerical_display_container, race_fragment, RACEFRAGMENT_TAG);
                transaction.addToBackStack(RACEFRAGMENT_TAG);
                transaction.replace(R.id.fragment_map_display_container, maps_fragment, MAPSFRAGMENT_TAG);
                transaction.addToBackStack(MAPSFRAGMENT_TAG);
                transaction.commit();
            } else {
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_numerical_display_container, race_fragment, RACEFRAGMENT_TAG);
                transaction.addToBackStack(RACEFRAGMENT_TAG);
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

        AppBackgroundServices.writeToSQLite = true;   // start the SQLite DB updates
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (TwoPane) {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "Found Tablet Device with sw = "+config.smallestScreenWidthDp+"dp");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "Found a Smartphone with sw = "+config.smallestScreenWidthDp+"dp");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppBackgroundServices.writeToSQLite = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // set screen time out to on
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        int count = fm.getBackStackEntryCount();
        /*
        Log.d(LOG_TAG, "BACK KEY BackStack STATUS = " + count);
        for(int entry = 0; entry < count; entry++){
            Log.i(LOG_TAG, "Found fragment: " + fm.getBackStackEntryAt(entry).getName());
        }
        */
        if (count > 1) {
            transaction = getFragmentManager().beginTransaction();
            fm.popBackStack();
        } else {
            confirmQuit();
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
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG,"Found Motion Event: " + event.toString());
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
     * detected a left or right swipe.
     *
     * On a large device with Two Pane display each swipe will cycle between the "Fragment_DisplayGoogleMap"
     * and the "Fragment_PlotWindChart".  The Back-key will terminate this Activity
     *
     * On a small device with Single Pane display a left swipe will replace the numerical display
     * (Fragment_StartSequence) with "Fragment_PlotWindChart".  A right swipe will replace the
     * numerical display with "Fragment_DisplayGoogleMap".  The Back-key will replace either one of the
     * graphical displays with the numerical display.
     *
     */
    public void changeActivityOnSwipe(int swipe) {
        if (TwoPane) {
            // Large device two-pane mode
            int containerID = (R.id.chart_container);

            if (findViewById(containerID) != null) {
                // current fragment displays the wind plot using "Fragment_PlotWindChart"
                // so we need to switch the the "Fragment_DisplayGoogleMap"
                if (maps_fragment == null) {
                    maps_fragment = new Fragment_DisplayGoogleMap();
                }
                maps_fragment.setArguments(getIntent().getExtras());
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_map_display_container, maps_fragment, MAPSFRAGMENT_TAG);
                transaction.commit();
            } else {
                // current fragment displays the race course on a Google Map using "Fragment_DisplayGoogleMap"
                // so we need to switch the the "Fragment_PlotWindChart"
                if (plot_fragment == null) {
                    plot_fragment = new Fragment_PlotWindChart();
                }
                plot_fragment.setArguments(getIntent().getExtras());
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_map_display_container, plot_fragment, PLOTFRAGMENT_TAG);
                transaction.commit();
            }
        } else {
            // Small device single-pane mode
            // Replace whatever is in the fragment_container view within this fragment,
            // and add the transaction to the back stack so the user can navigate back

            if (swipe == 1) {
                transaction = getFragmentManager().beginTransaction();
                transaction.add(R.id.fragment_numerical_display_container, maps_fragment, MAPSFRAGMENT_TAG);
                transaction.addToBackStack(MAPSFRAGMENT_TAG);
                transaction.commit();
            }
            if (swipe == -1) {
                transaction = getFragmentManager().beginTransaction();
                transaction.add(R.id.fragment_numerical_display_container, plot_fragment, PLOTFRAGMENT_TAG);
                transaction.addToBackStack(PLOTFRAGMENT_TAG);
                transaction.commit();
            }
        }
    }

    /**
     * confirmQuit Method handles the "Quit" confirmation so that the user doesn't quit the race
     * activity by accidentally hitting the back button
     */
    private void confirmQuit() {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this, android.R.style.Theme_Holo_Dialog));

        builder.setMessage(R.string.QuitRace);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Go ahead quit this race
                Activity_Main.completedRaceFlag = true;
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
        // Create the AlertDialog object and return it
        alert = builder.create();
        alert.show();
    }
}
