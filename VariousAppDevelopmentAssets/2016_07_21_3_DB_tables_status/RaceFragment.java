package com.example.volkerpetersen.sailingrace;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import java.util.Calendar;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * Fragment to manage all screen updates, location updates, and computations during
 * the race.  This class supports race to Windward and Leeward marks (one each) and a
 * manual wind mode (no Windex instrument) and a Windex instrument supported race mode.
 * This Activity can be launched from either the "MainActivity" or the "start_timerActivity"
 * when the countdown timer has reached zero and the race starts.
 *
 * Integrates the wind data from the SailTimerWind Bluetooth Windex when the SharedPreference
 * key_Windex = windex = 1
 *
 * all angles used in computations are assumed to be True North.
 * all angles output to screen are Magnetic North (True North + declination) except for
 * TWA and AWA which are always True North!
 *
 * Created by Volker Petersen on November 2015.
 *
 */
public class RaceFragment extends Fragment {
    private TextView outputStatus;              // output Textfield for the GPS status
    private TextView outputLatitude;            // output Textfield for the current Latitude
    private TextView outputLongitude;           // output Textfield for the current Longitude
    private TextView outputCOG;                 // Course Over Ground output field
    private TextView outputSOG;                 // Spped Over Ground output field
    private TextView outputVMG;                 // Velocity Made Good toward the currently active mark
    private TextView outputMWD;                 // Mean Wind Direction output field
    private TextView outputMeanHeadingTarget;   // output field top of screen with MeanHeadingTarget
    private TextView outputHeaderLift;          // output field showing if historically we have been LIFTED or HEADED
    private TextView outputAvgVariance;         // output field for the historical average amount Lifted or Headed
    private TextView outputVarDuration;         // output field for the duration of the current average
    private TextView outputDTM;                 // output field for DTM (Distance-To-Mark)
    private TextView outputBTM;                 // output field for BTM (Bearing-To-Mark)
    private TextView outputTBS;                 // output field for the Target Boat Speed (TBS)
    private TextView outputTBStext;             // output field for the Target Boat Speed (TBS) text
    private TextView outputTWS;                 // output field for TWS (True Wind Speed)
    private TextView outputTWD;                 // output field for TWD (True Wind Direction)
    private TextView outputTWDlabel;            // output field for TWD label
    private TextView outputGoalVMG;             // output field for Goal VMG to the current Mark
    private TextView outputGoalVMGtext;         // output field for the text of the Goal VMG
    private TextView outputLeewardMark;         // output field for the text of the Leeward Mark status
    private TextView outputWindwardMark;        // output field for the text of the Windward Mark status
    private Button btnSetLeewardMark;
    private Button btnGoLeewardMark;
    private Button btnSetWindwardMark;
    private Button btnGoWindwardMark;
    private Button btnMinusTen;
    private Button btnMinusOne;
    private Button btnPlusTen;
    private Button btnPlusOne;
    private RadioButton radioTRUE;
    private RadioButton radioAPPARENT;
    private CheckBox checkboxTACK;
    private ImageView boat;
    private ImageView outputWindGaugeNeedle;    // ImageView of the Wind Gauge Needle
    private ImageView outputWindGaugeArrow;     // ImageView of the Wind Gauge Tack/Gybe angle indicator arrow
    private GPSTracker gps;                     // gps class object to fetch the location data
    private GlobalParameters para;              // class object for the global parameters
    private boolean TRUE_APPARENT = true;       // true = TRUE, false = APPARENT wind values (angle, direction, speed) to be displayed
    private double AWA = 10.0d;                 // Apparent Wind Angle computed from the GPS and Windex data. Range 0 to +/-180
    private double AWD = 0.0d;                  // Apparent Wind Direction received from the Windex Data
    private double AWS = 0.0d;                  // Apparent Wind Speed received from the Windex Data
    private double TWA = 0.0d;                  // True Wind Angle computed from the Windex and GPS data. Range 0 to +/-180
    private double TWD = 0.0d;                  // True Wind Direction computed from the Windex and GPS data
    private double smoothedTWD = 0.0;           // TWD after applying a low pass filter
    private double lastTWD = 0.0;               // previous TWD reading
    private double avgSmoothedTWD;              // running average of the smoothed TWD; calc in windOscillations()
    private double minTWD;                      // minimum TWD reading (initialized in onStart() )
    private double maxTWD;                      // maximum TWD reading (initialized in onStart() )
    private double lastSmoothedTWD = 0.0;       // previous avg. TWD calculated value
    private double frequency = 0.0;             // wind oscillation frequency
    private double amplitude = 0.0;             // wind oscillation amplitude
    private double alpha;                       // low-pass filter parameter (from SailingRacePreferences)
    private double TWS = 0.0d;                  // True Wind Speed (kts) compute from the Windex and GPS data
    private double meanHeadingTarget;           // Mean Heading Target either manual goal or current avg COG
    private double meanWindDirection;           // calculated by adding tackAngle / gybeAngle to COG
    private double timeCounter = 0.0d;          // keeps duration of current header / lift sequence
    private double sumVariances = 0.0d;         // sum of the heading variances while in a Header or Lift sequence
    private double lastAvgVariance = 0.0d;      // keeps track of the last avg variance between meanHeadingTarget and COG
    private double halfwayLAT=Double.NaN;       // halfway Latitude point between Windward and Leeward mark
    private double halfwayLON=Double.NaN;       // halfway Longitude point between Windward and Leeward mark
    private double vmgu = 0.0;                  // Velocity Mage Good upwind
    private int screenUpdates;                  // screen update frequency in sec.  Set in preferences.
    private String tack = "stbd";               // "stbd" or "port" depending on current active board we're sailing on
    private double CourseOffset = 0.0d;         // Upwind leg=0.0  |  Downwind leg=180.0
    private double TackGybe;                    // contains tackAngle Upwind and gybeAngle Downwind
    private double tackAngle;                   // upwind leg tack angle set in preferences
    private double gybeAngle;                   // downwind leg gybe angle set in preferences
    private long gpsUpdates;                    // minimum time between GPS updates (set in Shared Preferences)
    private long datetime;                      // UTC date/time timestamp updated in the Runnable loop
    private int polars;                         // SharedPreference to indicate if Capri-25 polars are used (1) or not (0)
    private int windex;                         // SharedPreference to indicate if Windex is used (1) or not (0)
    private int smooth_GPS;                     // GPS readings to be stored in FIFO queue
    private int smooth_WIND;                    // Wind readings to be stored in FIFO queue
    private boolean autotack = true;            // with Windex enabled, program will auto tack / gybe when set to true
    private SQLiteDatabase db;                  // SQLite database to store the wind data when the windex is active
    private Cursor cursor;                      // SQLite cursor
    TimeZone TZ = TimeZone.getTimeZone("UTC");  // set UTC as the default timezone
    private Handler ScreenUpdate=new Handler(); // Handler to implement the Runnable for the Screen Updates
    private ContentValues windRecord;             // stores the tack data to be written to the SQLite DB
    private Context appContext;
    private ColorStateList WHITE;
    private ColorStateList RED;
    private ColorStateList GREEN;
    private ColorStateList YELLOW;
    private fifoQueueDouble AWDfifo;
    private fifoQueueDouble AWSfifo;
    private fifoQueueDouble COGfifo;
    private fifoQueueDouble SOGfifo;
    private final int WIND = 4;                 // Wind reading average to be stored every WIND seconds in the SQLite DB
    private int windCTR;                        // counter of wind readings (reset in onStart)
    private final int WIND_DELAY = 60;          // delay for WIND_DELAY readings before evaluating Wind Oscillations
    private int crossingsCTR;                   // counter of the avgSmoothedTWD crossings from above or below
    private View view;
    private double[] DistanceBearing = new double[2];
    static final int REQUEST_CODE_FINE_LOCATION = 1234;
    static final String LOG_TAG = RaceFragment.class.getSimpleName();
    private BroadcastReceiver WindexBroadcastReceiver;
    private double[] data;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        appContext = getActivity().getApplicationContext();

        windex = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // boolean value to indicate if app is using / not using the Bluetooth connected Windex
        if (windex == 1) {
            // Inflate the layout used in the Windex Anemometer mode
            view = inflater.inflate(R.layout.fragment_race_windex, container, false);
        } else {
            // Inflate the layout used in the non-Windex mode
            view = inflater.inflate(R.layout.fragment_race, container, false);
        }

        //* test dataset.  Used in windOscillations()
        data = new double[] {294.0, 288.5, 293.8, 295.8, 299.4, 298.5, 300.0, 301.8, 303.0,
                294.7, 296.9, 289.7, 293.3, 284.7, 288.3, 283.1, 284.3, 285.0,
                281.2, 283.0, 277.5, 278.6, 279.2, 285.2, 285.5, 292.0, 296.5,
                291.8, 297.8, 295.4, 302.5, 297.0, 296.8, 298.0, 300.7, 298.9,
                290.7, 287.3, 285.7, 288.3, 288.1, 280.3, 277.0, 280.2, 280.0,
                276.5, 280.6, 283.2, 284.2, 287.5, 288.0, 296.5, 293.8, 296.8,
                301.4, 298.5, 298.0, 298.8, 302.0, 297.7, 292.9, 296.7, 290.3,
                285.7, 284.3, 284.1, 280.3, 277.0, 284.2, 282.0, 278.5, 282.6,
                286.2, 287.2, 283.5, 289.0, 296.5, 293.8, 293.8, 302.4, 295.5,
                298.0, 295.8, 301.0, 295.7, 291.9, 289.7, 289.3, 284.7, 283.3,
                285.1, 283.3, 279.0, 281.2, 280.0, 284.5, 281.6, 282.2, 288.2,
                283.5, 286.0, 289.5, 297.8, 296.8, 301.4, 296.5, 304.0, 301.8,
                302.0, 293.7, 298.9, 289.7, 287.3, 288.7, 282.3, 285.1, 282.3,
                280.0, 283.2, 283.0, 280.5, 282.6, 287.2, 287.2, 291.5, 288.0,
                294.5, 295.8, 296.8, 299.4, 297.5, 298.0, 303.8, 303.0, 293.7,
                292.9, 290.7, 295.3, 292.7, 290.3, 281.1, 279.3, 280.0, 279.2,
                281.0, 283.5, 278.6, 281.2, 288.2, 286.5, 291.0, 292.5, 291.8,
                294.8, 301.4, 298.5, 298.0, 303.8, 298.0, 298.7, 294.9, 292.7,
                288.3, 284.7, 285.3, 285.1, 286.3, 281.0, 278.2, 282.0, 276.5,
                281.6, 281.2, 283.2, 290.5, 294.0, 296.5, 292.8, 300.8, 295.4,
                300.5, 297.0, 297.8, 295.0, 301.7, 292.9, 294.7, 289.3, 287.7,
                288.3, 283.1, 284.3, 285.0, 282.2, 280.0, 282.5, 278.6, 285.2,
                281.2, 290.5, 293.0};
        //*/

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // initialize our Global Parameter singleton class
        para = GlobalParameters.getInstance();

        // fetch all the display elements from the xml file
        outputStatus = (TextView) view.findViewById(R.id.Status);
        outputLatitude = (TextView) view.findViewById(R.id.Latitude);
        outputLongitude = (TextView) view.findViewById(R.id.Longitude);
        outputCOG = (TextView) view.findViewById(R.id.COG);
        outputSOG = (TextView) view.findViewById(R.id.SOG);
        outputVMG = (TextView) view.findViewById(R.id.VMG);
        outputMWD = (TextView) view.findViewById(R.id.MWD);
        outputBTM = (TextView) view.findViewById(R.id.BTM);
        outputDTM = (TextView) view.findViewById(R.id.DTM);
        outputHeaderLift = (TextView) view.findViewById(R.id.HeaderLift);
        outputAvgVariance = (TextView) view.findViewById(R.id.avgVariance);
        outputVarDuration = (TextView) view.findViewById(R.id.varDuration);

        // fetch the Shared Preferences from the class FetchPreferenceValues in file SailingRacePreferences
        // Shared Preferences key names are defined in SailingRacePreferences.OnCreate()
        screenUpdates = SailingRacePreferences.FetchPreferenceValue("key_ScreenUpdates", appContext); // Time interval for screen update in seconds.
        smooth_GPS = SailingRacePreferences.FetchPreferenceValue("key_avgGPS", appContext); // max number of GPS positions stored in LinkedList.
        smooth_WIND = SailingRacePreferences.FetchPreferenceValue("key_avgWIND", appContext); // max number of WIND values stored in LinkedList.
        gpsUpdates = (long) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateTime", appContext); // Time Interval for GPS position updates in milliseconds
        polars = SailingRacePreferences.FetchPreferenceValue("key_Polars", appContext); // =1 if we use Capri-25 build-in polars
        tackAngle = (double) SailingRacePreferences.FetchPreferenceValue("key_TackAngle", appContext);  // tack angle
        gybeAngle = (double) SailingRacePreferences.FetchPreferenceValue("key_GybeAngle", appContext); // gybe angle
        windex = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // boolean value to indicate if app is using / not using the Bluetooth connected Windex
        alpha = (SailingRacePreferences.FetchPreferenceValue("key_Alpha", appContext)/100.0d); // low-pass filter parameter Alpha
        windCTR = 0;
        crossingsCTR = 0;
        minTWD = 500.0d;
        maxTWD = 0.0d;

        Log.d(LOG_TAG, "Low-Pass Filter Parameter = "+alpha*100.0d+"%");

        // initialize the GPSTracker services, ScreenUpdater, buttons, and switch screen on (disable sleep)
        COGfifo = new fifoQueueDouble(smooth_GPS);
        SOGfifo = new fifoQueueDouble(smooth_GPS);
        gps = new GPSTracker(getActivity(), gpsUpdates, smooth_GPS, COGfifo, SOGfifo, REQUEST_CODE_FINE_LOCATION);

        if (windex == 0) {
            //--------------------------------------------------------------------------------------
            // SailtimerWind instrument NOT enabled in SharedPreferences
            //--------------------------------------------------------------------------------------
            boat = (ImageView) view.findViewById(R.id.boat);
            outputMeanHeadingTarget = (TextView) view.findViewById(R.id.MeanHeadingTarget);
            btnMinusTen = (Button) view.findViewById(R.id.buttonMinus10);
            btnMinusOne = (Button) view.findViewById(R.id.buttonMinus);
            btnPlusTen = (Button) view.findViewById(R.id.buttonPlus10);
            btnPlusOne = (Button) view.findViewById(R.id.buttonPlus);
            btnSetLeewardMark = (Button) view.findViewById(R.id.buttonSetLeewardMark);
            btnGoLeewardMark = (Button) view.findViewById(R.id.buttonGoLeewardMark);
            btnSetWindwardMark = (Button) view.findViewById(R.id.buttonSetWindwardMark);
            btnGoWindwardMark = (Button) view.findViewById(R.id.buttonGoWindwardMark);
            meanHeadingTarget = Integer.parseInt(outputMeanHeadingTarget.getText().toString());
            btnGoLeewardMark.setEnabled(false);
            btnGoWindwardMark.setEnabled(false);

            /**
             * initialize buttons only for the No-Windex option
             */
            btnSetLeewardMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go Set or Clear the Leeward mark
                    setLeewardMark();
                }
            });

            btnSetWindwardMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go Set or Clear the Windward mark
                    setWindwardMark(false);
                }
            });

            btnSetWindwardMark.setOnLongClickListener(new View.OnLongClickListener() {
                // if we have a long Click, we want to go to Google Maps to set the mark on the map
                @Override
                public boolean onLongClick(View view) {
                    // check if the Windward Mark has been set (WindwardFlag=true)
                    if (para.getWindwardFlag()) {
                        return true;
                    }
                    startActivity(new Intent(appContext, MapsFragment.class));
                    return true;
                }
            });

            btnGoWindwardMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go set the Leeward mark
                    goWindwardMark();
                }
            });

            btnGoLeewardMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go set the Leeward mark
                    goLeewardMark();
                }
            });
        } else {
            //--------------------------------------------------------------------------------------
            // Sailtimer Inc. Wind Anemometer is enabled in SharedPreferences
            //--------------------------------------------------------------------------------------

            // initialize the FIFO queues to keep smooth_GPS COG & SOG and smooth_WIND for AWD & AWS
            AWDfifo = new fifoQueueDouble(smooth_WIND);
            AWSfifo = new fifoQueueDouble(smooth_WIND);

            WindexBroadcastReceiver = new SailTimerAPI(appContext, AWDfifo, AWSfifo);

            outputTBStext = (TextView) view.findViewById(R.id.TBStext);
            outputTBS = (TextView) view.findViewById(R.id.TBS);
            outputTWS = (TextView) view.findViewById(R.id.WindGaugeTWS);
            boat = (ImageView) view.findViewById(R.id.WindGaugeBoat);
            outputWindGaugeNeedle = (ImageView) view.findViewById(R.id.WindGaugeNeedle);
            outputWindGaugeArrow  = (ImageView) view.findViewById(R.id.WindGaugeArrow);
            outputTWD = (TextView) view.findViewById(R.id.TWD);
            outputTWDlabel = (TextView) view.findViewById(R.id.TWDlabel);
            outputGoalVMG = (TextView) view.findViewById(R.id.GoalVMG);
            outputGoalVMGtext = (TextView) view.findViewById(R.id.GoalVMGtext);
            outputLeewardMark = (TextView) view.findViewById(R.id.LeewardMark);
            outputWindwardMark = (TextView) view.findViewById(R.id.WindwardMark);

            // check for a Click on the Radio Group to update TRUE / APPARENT wind data display
            radioAPPARENT = (RadioButton) view.findViewById(R.id.radioAPPARENT);
            radioAPPARENT.setOnClickListener(new RadioButton.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Check which Radio Button clicked
                    if (radioAPPARENT.isChecked() ) {
                        TRUE_APPARENT = false;
                    } else {
                        TRUE_APPARENT = true;
                    }
                    updateWindGauge();  // update the screen accordingly
                }
            });
            radioTRUE = (RadioButton) view.findViewById(R.id.radioTRUE);
            radioTRUE.setOnClickListener(new RadioButton.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Check which Radio Button clicked
                    if (radioTRUE.isChecked() ) {
                        TRUE_APPARENT = true;
                    } else {
                        TRUE_APPARENT = false;
                    }
                    updateWindGauge();  // update the screen accordingly
                }
            });

            // check for a Click on the Checkbox to update the status of the autotack flag
            checkboxTACK = (CheckBox) view.findViewById(R.id.autotackStatus);
            checkboxTACK.setOnClickListener(new CheckBox.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ( checkboxTACK.isChecked() ) {
                        autotack = true;
                    } else {
                        autotack = false;
                    }
                    updateWindGauge();
                    //Log.d(LOG_TAG, "Found CheckBox Click =" + autotack);
                }
            });

            if (para.getWindwardFlag() && para.getLeewardFlag()) {
                double[] h = NavigationTools.MarkDistanceBearing(para.getLeewardLat(), para.getWindwardLon(), para.getLeewardLat(), para.getLeewardLon());
                h = NavigationTools.withDistanceBearingToPosition(para.getLeewardLat(), para.getLeewardLon(), h[0]/2.0d, h[1]);
                halfwayLAT = h[0];
                halfwayLON = h[1];
                para.setCenterLat(h[0]);
                para.setCenterLon(h[1]);

                // create a Race ID using today's date
                para.setRaceID(0);
                Calendar now = Calendar.getInstance(TZ);
                now.set(Calendar.HOUR_OF_DAY, 0);
                now.set(Calendar.MINUTE, 0);
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 10);
                long today = (long)(now.getTimeInMillis()/1000.0d);

                // check if we've already recorded a race today in the sqlite db.
                String sql = "SELECT * FROM "+sqlWindDataHelper.WindEntry.TABLE_NAME+" WHERE ";
                sql += sqlWindDataHelper.WindEntry.COLUMN_RACE+">='"+today+"'";
                sql += " ORDER BY "+sqlWindDataHelper.WindEntry.COLUMN_RACE+" DESC";
                //Log.d(LOG_TAG, "onStart() - Race_ID initialization query: "+sql);

                sqlWindDataHelper dbhelper = new sqlWindDataHelper(appContext);
                db = dbhelper.getWritableDatabase();
                dbhelper.onCreate(db);
                cursor = db.rawQuery(sql, null);
                if (cursor.moveToFirst()) {
                    // add an incremental count to the latest race of the day
                    today = cursor.getLong(sqlWindDataHelper.COL_WIND_RACE) + 1;
                }
                para.setRaceID(today);
                //Log.d(LOG_TAG, "onStart() - Initialized Race_ID: "+today);

                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        // initialize the color variables (type ColorStateList)
        YELLOW = ContextCompat.getColorStateList(appContext, R.color.YELLOW);
        WHITE = ContextCompat.getColorStateList(appContext, R.color.WHITE);
        RED = ContextCompat.getColorStateList(appContext, R.color.RED);
        GREEN = ContextCompat.getColorStateList(appContext, R.color.GREEN);

        // check for a Click on Boat to see if we want to Tack or Gybe
        boat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view == view.findViewById(R.id.boat) || view == view.findViewById(R.id.WindGaugeBoat)) {
                    if (tack.equals("stbd")) {
                        tack = "port";
                    } else {
                        tack = "stbd";
                    }
                    updateMarkerButtons();
                }
            }
        });

        // check for a long Click on Boat to see if we want to change from Upwind to Downwind or visa versa
        boat.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (view == view.findViewById(R.id.boat) || view == view.findViewById(R.id.WindGaugeBoat)) {
                    CourseOffset = 180.0d - CourseOffset;
                    para.setCourseOffset(CourseOffset);
                    if (CourseOffset == 0.0d) {
                        goWindwardMark();
                    } else {
                        goLeewardMark();
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            gps.stopGPS();                                   // stop the GPS thread
            ScreenUpdate.removeCallbacks(updateScreenNow);   // stop the ScreenUpdate Handler Runnable
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error on Destroy: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        para.setBestTack("- -");

        if (gps != null) gps.startGPS();
        Log.d(LOG_TAG, "onResume() after gps start gps="+gps.toString());
        updateMarkerButtons();
        ScreenUpdate.post(updateScreenNow);
        if (windex == 1) {
            sqlWindDataHelper dbhelper = new sqlWindDataHelper(appContext);
            db = dbhelper.getWritableDatabase();
            dbhelper.onCreate(db);
            try {
                appContext.registerReceiver(WindexBroadcastReceiver, SailTimerAPI.WindexBroadcastReceiverIntentFilter());
            } catch (Exception e) {
                Log.d(LOG_TAG, "Error on registering the WindexBroadcastReceiver: " + e.getMessage());
            }
        }

        if (para.getWindwardFlag() && para.getWindwardRace()) {
            goWindwardMark();
        }
        if (para.getLeewardFlag() && para.getLeewardRace()) {
            goLeewardMark();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        gps.stopGPS();                                   // stop the GPS thread
        ScreenUpdate.removeCallbacks(updateScreenNow);   // stop the ScreenUpdate Handler Runnable
        if (windex == 1) {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
            try {
                appContext.unregisterReceiver(WindexBroadcastReceiver);
            } catch (Exception e) {
                Log.d(LOG_TAG, "Error on 'unregisterReceiver(WindexBroadcastReceiver)': " + e.getMessage());
            }
        }
    }

    /**
     * Runnable to update the display every "screenUpdates" seconds.  The screen update delay
     * can be specified using the Setting parameter Screen Updates.
     *
     * Within this runnable we obtain the latest GPS position, perform all computations, and
     * update the screen display with the results.
     */
    Runnable updateScreenNow = new Runnable() {
        public void run() {
            String[] dots =new String[]{".", "..", "...", "...."};
            String tmp;
            double delta;
            double[] Laylines = new double[5];
            double[] goalPolars = new double[2];
            double goalVMGu;

            // if we have an active Leeward or Windward mark, compute distance and Bearing to mark and the optimum Laylines
            if (para.getLeewardRace()) {
                DistanceBearing = NavigationTools.MarkDistanceBearing(para.getBoatLat(), para.getBoatLon(), para.getLeewardLat(), para.getLeewardLon());
                Laylines = NavigationTools.optimumLaylines(para.getBoatLat(), para.getBoatLon(), para.getLeewardLat(), para.getLeewardLon(), TWD, para.getCourseOffset(), TackGybe, tack);
                para.setBestTack(NavigationTools.LaylinesString(Laylines[2]));
            }
            if (para.getWindwardRace()) {
                DistanceBearing = NavigationTools.MarkDistanceBearing(para.getBoatLat(), para.getBoatLon(), para.getWindwardLat(), para.getWindwardLon());
                Laylines = NavigationTools.optimumLaylines(para.getBoatLat(), para.getBoatLon(), para.getWindwardLat(), para.getWindwardLon(), TWD, para.getCourseOffset(), TackGybe, tack);
                para.setBestTack(NavigationTools.LaylinesString(Laylines[2]));
            }

            if (para.getLeewardRace() || para.getWindwardRace()) {
                outputDTM.setText(appContext.getString(R.string.DF2, DistanceBearing[0]));     // update DTM (Distance-To-Mark) in nm
                tmp = appContext.getString(R.string.Degrees, NavigationTools.TrueToMagnetic(DistanceBearing[1], para.getDeclination()));
                outputBTM.setText(tmp);  // update BTM (Bearing-To-Mark)
                // computing meanHeadingTarget which now is theoretical BTM plus TackGybe angle
                if (tack.equals("stbd")) {
                    meanHeadingTarget = NavigationTools.TrueToMagnetic(DistanceBearing[1] - TackGybe, para.getDeclination());
                } else {
                    meanHeadingTarget = NavigationTools.TrueToMagnetic(DistanceBearing[1] + TackGybe, para.getDeclination());
                }
            }

            // create UTC timestamp in seconds to maintain compatibility with MYSQL DB
            Calendar now = Calendar.getInstance(TZ);
            datetime = (long)(now.getTimeInMillis()/1000.0d);

            if (windex == 1) {
                //--------------------------------------------------------------------------------------
                // Windex screen updates
                //--------------------------------------------------------------------------------------
                AWD = AWDfifo.averageCompassDirection();
                AWS = AWSfifo.average();
                AWA = NavigationTools.HeadingDelta(para.getAvgCOG(), AWD);
                double[] values = NavigationTools.calc_TWA_TWD_TWS(para.getAvgSOG(), AWA, AWD, AWS);
                TWA = values[0];
                TWD = values[1];
                TWS = values[2];

                // compute the wind oscillation frequency and amplitude
                windCTR += 1;
                // testing next 2 lines
                int i = (windCTR-1) % (data.length-1);
                values = windOscillations(data[i]);
                //values = windOscillations(TWD);

                amplitude = values[0];
                frequency = values[1];

                //*
                double safe = lastSmoothedTWD;
                if (windCTR > WIND_DELAY) {
                    Log.d(LOG_TAG, " ");
                    Log.d(LOG_TAG, "windCTR = "+windCTR+"  crossingCTR = "+crossingsCTR);
                    Log.d(LOG_TAG, "lastSmoothedTWD = "+safe+"  avgSmoothedTWD = "+avgSmoothedTWD);
                    amplitude = values[0];
                    frequency = values[1];
                    Log.d(LOG_TAG, "  smoothedTWD = "+smoothedTWD);
                    Log.d(LOG_TAG, "minTWD = "+minTWD+"  maxTWD = "+maxTWD);
                    Log.d(LOG_TAG, "Frequency = "+frequency);
                    Log.d(LOG_TAG, "amplitude = "+amplitude);
                }
                //*/

                /*
                Log.d(LOG_TAG, "TWA = " + appContext.getString(R.string.DF1, TWA) + "  AWA = " + appContext.getString(R.string.DF1, AWA));
                Log.d(LOG_TAG, "TWD = " + appContext.getString(R.string.DF1, TWD) + "  AWD = " + appContext.getString(R.string.DF1, AWD) + "  avgAWD = " + appContext.getString(R.string.DF1, avgAWD));
                Log.d(LOG_TAG, "TWS = " + appContext.getString(R.string.DF1, TWS) + "  AWS = " + appContext.getString(R.string.DF1, AWS) + "  avgAWS = " + appContext.getString(R.string.DF1, avgAWS));
                Log.d(LOG_TAG, "COG = " + appContext.getString(R.string.DF1, para.getAvgCOG()));
                Log.d(LOG_TAG, "SOG = " + appContext.getString(R.string.DF1, para.getAvgSOG()));
                */

                //Log.d(LOG_TAG, "WindwardRace: "+para.getWindwardRace()+"  WindwardFlag: "+para.getWindwardFlag());
                //Log.d(LOG_TAG, "LeewardRace : "+ para.getLeewardRace()+"  LeewardFlag : " +para.getLeewardFlag());
                if (para.getWindwardRace() || para.getLeewardRace()) {
                    delta = NavigationTools.HeadingDelta(DistanceBearing[1], (CourseOffset - TWA));
                    if( updateWindDB(TWD, TWS, para.getRaceID()) ){
                        // TODO nothing to add here
                        //Log.d(LOG_TAG, "wrote record to sqlite DB with windCTR = "+windCTR);
                    } else {
                        // TODO nothing to add here
                        //Log.d(LOG_TAG, "ERROR writing record to sqlite DB");
                    }

                } else {
                    delta = NavigationTools.HeadingDelta((CourseOffset-TWA), TackGybe);
                }

                updateWindGauge();  // also compute TackGybe angle!
                goalPolars = NavigationTools.getPolars(TWS, CourseOffset);
                goalVMGu = NavigationTools.calcVMGu(TackGybe, CourseOffset, goalPolars[0]);
                //Log.d(LOG_TAG, "    TWS="+appContext.getString(R.string.DF2, TWS)+"  |  TBS+"+ appContext.getString(R.string.DF3, goalPolars[0]));
                //Log.d(LOG_TAG, "goalVMGu="+appContext.getString(R.string.DF2, goalVMGu)+"  |  TackGybe="+appContext.getString(R.string.DF1, TackGybe);

                outputTBS.setText(appContext.getString(R.string.DF1, goalPolars[0]));  // Target Boat Speed based from Polars for this TWS/TWA
                outputGoalVMG.setText(appContext.getString(R.string.DF1, goalVMGu));
                tmp = para.getBestTack();

                if (tmp.equals("Stbd")) {
                    outputGoalVMG.setTextColor(GREEN);
                    outputGoalVMGtext.setTextColor(GREEN);
                    outputTBS.setTextColor(GREEN);
                    outputTBStext.setTextColor(GREEN);
                } else if (tmp.equals("Port")) {
                    outputGoalVMG.setTextColor(RED);
                    outputGoalVMGtext.setTextColor(RED);
                    outputTBS.setTextColor(RED);
                    outputTBStext.setTextColor(RED);
                } else {
                    outputGoalVMG.setTextColor(WHITE);
                    outputGoalVMGtext.setTextColor(WHITE);
                    outputTBS.setTextColor(WHITE);
                    outputTBStext.setTextColor(WHITE);
                }
            } else {
                //--------------------------------------------------------------------------------------
                // Non-Windex screen updates
                //--------------------------------------------------------------------------------------
                // compute the deviation from our "meanHeadingTarget"
                delta = NavigationTools.HeadingDelta(meanHeadingTarget, para.getAvgCOG());
                outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
                tmp = appContext.getString(R.string.Degrees, NavigationTools.TrueToMagnetic(meanWindDirection, para.getDeclination()));
                outputMWD.setText(tmp);
            }

            checkForCurrentCourse();
            lastAvgVariance = updateVarianceSumAvg(delta, lastAvgVariance);

            tmp = appContext.getString(R.string.Degrees, NavigationTools.TrueToMagnetic(para.getAvgCOG(), para.getDeclination()));
            outputCOG.setText(tmp);

            if (para.getAvgSOG() < 10.0d) {
                tmp = appContext.getString(R.string.DF2, para.getAvgSOG());
            } else {
                tmp = appContext.getString(R.string.DF1, para.getAvgSOG());
            }
            outputSOG.setText(tmp);

            vmgu = NavigationTools.calcVMGu(TWA, CourseOffset, para.getAvgSOG());
            if (vmgu < 10.0d) {
                tmp = appContext.getString(R.string.DF2, vmgu);
            } else {
                tmp = appContext.getString(R.string.DF1, vmgu);
            }
            outputVMG.setText(tmp);

            outputStatus.setText(gps.getBestProvider()+dots[gps.CTR%4]);
            tmp = "Lat:" + NavigationTools.PositionDegreeToString(para.getBoatLat(), true);
            outputLatitude.setText(tmp);

            tmp = "Lon:" + NavigationTools.PositionDegreeToString(para.getBoatLon(), false)
                    + "  δ:" + appContext.getString(R.string.Deg1, para.getDeclination());
            outputLongitude.setText(tmp);

            // set the screen display frequency
            ScreenUpdate.postDelayed(this, (long) (screenUpdates * 1000));
        }
    };

    /**
     * Method to compute the frequency and amplitude of the wind oscillation.
     * This algorithm first applies a low-pass filter with the parameter "alpha"
     * (set in the SailingRacePreferences).
     * Then the average TWD will be calculated from the smoothed TWD.
     * Amplitude = (max(smoothed TWD) - min(smoothed TWD) ) / 2
     * Frequency = # of TWD readings / count of avgSmoothedTWD crossings * 2
     *
     * see Excel file "SailingRace_Math" Tab "Frequency" for a sample calculation
     *
     * @param twd
     * @return [amplitude, frequency]
     */
    public double[] windOscillations(double twd) {
        double[] results = new double[2];
        double avgSmoothedTWD_Rad, smoothedTWD_Rad, x, y;

        // first apply a low-pass filter to smooth out the wind direction (TWD) readings
        // then compute the average of the smoothed wind directions (TWD) readings
        // and determine the number of crossings of the avgSmoothedTWD
        if (windCTR > 1) {
            smoothedTWD = alpha * lastTWD + (1.0d - alpha) * smoothedTWD;
            avgSmoothedTWD_Rad = Math.toRadians( avgSmoothedTWD );
            smoothedTWD_Rad = Math.toRadians( smoothedTWD );
            x = (Math.sin(smoothedTWD_Rad) + Math.sin(avgSmoothedTWD_Rad)*(windCTR-1)) / (double)windCTR;
            y = (Math.cos(smoothedTWD_Rad) + Math.cos(avgSmoothedTWD_Rad)*(windCTR-1)) / (double)windCTR;
            avgSmoothedTWD = NavigationTools.convertXYCoordinateToPolar(x, y);

            if (smoothedTWD < minTWD) {
                minTWD = smoothedTWD;
            }
            if (smoothedTWD > maxTWD) {
                maxTWD = smoothedTWD;
            }
        } else {
            smoothedTWD = twd;
            avgSmoothedTWD = twd;
        }

        // estimate the amplitude and frequency of the smoothed TWD values
        // after we observed the wind for WIND_DELAY seconds (set to 60 seconds)
        if (windCTR > WIND_DELAY) {
            if ((lastSmoothedTWD > avgSmoothedTWD && smoothedTWD < avgSmoothedTWD) || (lastSmoothedTWD < avgSmoothedTWD && smoothedTWD > avgSmoothedTWD)) {
                crossingsCTR += 1;
            }
            if (crossingsCTR > 0) {
                // amplitude estimate
                results[0] = (maxTWD - minTWD) / 2.0d;
                // frequency estimate - division by screenUpdates to convert into Hz (1/sec)
                results[1] = (windCTR-WIND_DELAY) / (double) crossingsCTR * 2.0d / screenUpdates;
            } else {
                results[0] = 0.0;
                results[1] = 0.0;
            }
            // safe the tack data into ContentValues structure and add this record to the SQLite FIFO queue
            windRecord = new ContentValues();
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_DATE, datetime);
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_TWD, TWD);
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_smoothedTWD, smoothedTWD);
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_avgSmoothedTWD, avgSmoothedTWD);
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_amplitude, results[0]);
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_frequency, results[1]);
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_COG, para.getAvgCOG());
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_SOG, para.getAvgSOG());
            windRecord.put(sqlWindDataHelper.windFIFO.COLUMN_tack, tack);
            if (!updateWindFIFO(windRecord)) {
                Log.d(LOG_TAG, "Failed to write data to the Wind FIFO queue.");
            }
        }

        lastTWD = twd;
        lastSmoothedTWD = smoothedTWD;

        return results;
    }

    /**
     * Method to update the display of the WindGauge and the complementing text output
     * The windRecord contains {TWD, smoothedTWD, avgSmoothedTWD, amplitude, frequency, COG, SOG, and tack}.
     */
    public void updateWindGauge() {
        if (para.getSailtimerStatus()) {
            outputTWS.setTextColor(YELLOW);
        } else {
            outputTWS.setTextColor(RED);
            AWS = 0.0d;
            AWA = 0.0d;
        }
        if (TRUE_APPARENT) {
            // true: display TRUE wind data in the Wind Gauge and the data display
            outputWindGaugeNeedle.setRotation((float) TWA);
            outputTWS.setText(appContext.getString(R.string.DF1, TWS));
            outputTWD.setText(appContext.getString(R.string.Degrees, NavigationTools.TrueToMagnetic(TWD, para.getDeclination())));
            outputTWDlabel.setText("TWD:");
        } else {
            // false: display APPARENT wind data in Wind Gauge and the data display
            outputWindGaugeNeedle.setRotation((float) AWA);
            outputTWS.setText(appContext.getString(R.string.DF1, AWS));
            outputTWD.setText(appContext.getString(R.string.Degrees, NavigationTools.TrueToMagnetic(AWD, para.getDeclination())));
            outputTWDlabel.setText("AWD:");
        }


        // display the Wind Gauge arrow to indicate the optimum Tack/Gybe angle
        TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, TWS);
        outputWindGaugeArrow.setRotation((float)NavigationTools.HeadingDelta(CourseOffset - TackGybe, 0.0d));
    }

    /**
     * Method to add one record to the SQLite DB FIFO queue which stores the wind data records.
     * The FIFO queue size is defined in variable WIND_RECORDS in class "sqlWindDataHelper.
     * @param windRecord
     * @return boolean flag for success or failure
     */
    public boolean updateWindFIFO(ContentValues windRecord) {
        // if we don't have a valid sqlite db connection, return false
        if (db == null) {
            return false;
        }

        // Update the SQLite DB FIFO queue
        long rowId = db.insert(sqlWindDataHelper.windFIFO.TABLE_NAME, null, windRecord);
        Log.d(LOG_TAG,"inserted row "+rowId+" in table "+sqlWindDataHelper.windFIFO.TABLE_NAME);
        if (rowId == -1) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Method to update the SQLite wind database with the average TWD and TWS over the past WIND number of readings.
     * In addition to the wind data, we'll also store our location on the course as defined by the 4 quadrants (e.g.
     * left Windward = quadrant 4, right Windward = quadrant 1, right Leeward = quadrant 2, and left Leeward = quadrant 3
     * WIND is a final int defined in the top section of this Class.
     *
     * @param TWD (True Wind Direction - true north)
     * @param TWS (True Wind Speed - kts)
     * @param raceID ID of this race.  If raceID = 0, return false
     * @return true if successful, false if nothing was written to the DB (error or not the WIND number of reading
     */
    public boolean updateWindDB(double TWD, double TWS, long raceID) {
        int quadrant;

        // windCTR gets increased by 1 everytime we add a reading to the AWDfifo queue.  We use
        // windCTR to determine when we have WIND readings so that we store that one in the SQLite DB
        //Log.d(LOG_TAG, "windCTR: "+windCTR+"  WIND: "+WIND+"  mod: "+(windCTR%WIND));
        if ((windCTR % WIND) != 0) {
            return false;
        }

        // if we don't have a valid raceID, return false
        if (raceID == 0) {
            return false;
        }

        // if we don't have a valid sqlite db connection, return false
        if (db == null) {
            return false;
        }

        // let's find out which Quadrant we're are currently sailing in
        double[] h = NavigationTools.MarkDistanceBearing(para.getBoatLat(), para.getBoatLon(), halfwayLAT, halfwayLON);
        if (h[1]>=0.0d && h[1]<90.0d) {
            quadrant = 1;
        } else if (h[1]>=90.0d && h[1]<180.0d) {
            quadrant = 2;
        } else if (h[1]>=180.0d && h[1]<270.0d) {
            quadrant = 3;
        } else if (h[1]>=270.0d && h[1]<360.0d){
            quadrant = 4;
        } else {
            quadrant = 0;
        }

        // Create a new map of values, where the DB column names are the keys
        ContentValues values = new ContentValues();
        values.put(sqlWindDataHelper.WindEntry.COLUMN_DATE, datetime);
        values.put(sqlWindDataHelper.WindEntry.COLUMN_TWD, TWD);
        values.put(sqlWindDataHelper.WindEntry.COLUMN_TWS, TWS);
        values.put(sqlWindDataHelper.WindEntry.COLUMN_QUADRANT, quadrant);
        values.put(sqlWindDataHelper.WindEntry.COLUMN_STATUS, 0);
        values.put(sqlWindDataHelper.WindEntry.COLUMN_RACE, raceID);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(sqlWindDataHelper.WindEntry.TABLE_NAME, null, values);
        //Log.d(LOG_TAG, "newRowID = "+newRowId);
        if (newRowId == -1) {
            return false;
        }
        return true;
    }

    /**
     * Class methods to compute the heading variances, the sum totals, and averages.
     * Calculate the correct sign for the course / board we are currently sailing on.
     * We want a Header be negative values and Lift positive values based on the current course.
     *
     *           stbd     port
     *           H   L    H   L
     *          (-) (+)  (-) (+)
     * Upwind    -   +    +   -    necessary value of sign in order to yield a negative values
     * Downwind  +   -    -   +    for Header and positive values for Lift situations
     *
     * @param delta heading variance as computed in ScreenUpdateNow base on which app mode we're using
     * @param last  value of the avgVariance from the previous calculation of this value.  This value
     *              is used to determine if we still have the same variance (Header or Lift).
     * @return avgVariance
     *
     */
    public double updateVarianceSumAvg(double delta, double last) {
        double avgVariance;
        double sign;
        String tmp;

        if (CourseOffset == 0.0d) {
            // Upwind leg (CourseOffset = 0.0)
            sign = 1.0;
        } else {
            // Downwind leg (CourseOffset = 180.0)
            sign = -1.0;
        }
        if (tack.equals("port")) {
            sign = -sign;
        }
        delta = delta * sign;
        
        // check if we still have the same deviation (<0 or >0) as last time
        if ( (last >= 0.0d && delta >= 0.0d) || (last <= 0.0d && delta <= 0.0d) ) {
            timeCounter = timeCounter + (double)screenUpdates;
            sumVariances = sumVariances + delta;
            avgVariance = sumVariances / (timeCounter/(double)screenUpdates);
        } else {
            // if not, reset timer and sum
            timeCounter = 0.0d;
            sumVariances = 0.0d;
            avgVariance = 0.0d;
        }

        if (avgVariance < 0.0) {
            outputHeaderLift.setText("Header");
            outputHeaderLift.setTextColor(RED);
        } else {
            outputHeaderLift.setText("Lift");
            outputHeaderLift.setTextColor(GREEN);
        }
        tmp = appContext.getString(R.string.Degrees, Math.abs(avgVariance));
        outputAvgVariance.setText(tmp);
        tmp = appContext.getString(R.string.MinSec, Math.floor(timeCounter / 60.0d), (timeCounter%60.0d));
        outputVarDuration.setText(tmp);

        return avgVariance;
    }

    /**
     * Class methods to increase/decrease the Mean Heading target values.
     * These Methods get initiated by UI buttons
     */
    public void decreaseMeanHeading(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - 1.0);
        outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void increaseMeanHeading(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 1.0);
        outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void decreaseMeanHeading10(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - 10.0);
        outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void increaseMeanHeading10(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 10.0);
        outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
        meanWindDirection = MWD();
    }

    /**
     * Class methods to set the Windward mark at the current boat location.
     * initiated by UI buttons defined in activity_start_race and the onClick listener in onCreate
     *
     * @param mapMarkerSet  boolean that is true when a position has been set. In that case this
     *                      method will delete the existing mark
     */
    public void setWindwardMark(boolean mapMarkerSet) {
        if (!mapMarkerSet) {
            para.setWindwardLat(gps.getLatitude());
            para.setWindwardLon(gps.getLongitude());
        }
        if (para.getWindwardFlag()) {
            // display a confirmation dialog to make sure user wants to delete the mark position
            clearPositionMark("Windward");
        } else {
            para.setWindwardFlag(true);
        }
        updateMarkerButtons();
    }

    /**
     * Class methods to set the Leeward mark at the current boat location.
     * initiated by UI buttons defined in activity_start_race and the onClick listener in onCreate
     *
     */
    public void setLeewardMark() {
        para.setLeewardLat(gps.getLatitude());
        para.setLeewardLon(gps.getLongitude());
        if (para.getLeewardFlag()) {
            // display a confirmation dialog to make sure user wants to delete the mark position
            clearPositionMark("Leeward");
        } else {
            para.setLeewardFlag(true);
        }
        updateMarkerButtons();
    }

    /**
     * The "clearPositionMark" method is called from the setLeewardMark() or setWindwardMark()
     * methods to delete the designed mark upon user confirmation.
     */
    public void clearPositionMark(final String marker) {
        // Alert Dialog to verify Marker deletion
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this.getActivity(), android.R.style.Theme_Holo_Dialog));
        builder.setMessage("Delete the current "+marker+" race marker?");
        builder.setPositiveButton("Delete marker", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User confirmed the requested action
                if (marker.equals("Windward")) {
                    para.setWindwardFlag(false);
                    para.setWindwardRace(false);
                    para.setWindwardLat(Double.NaN);
                    para.setWindwardLon(Double.NaN);
                }
                if (marker.equals("Leeward")) {
                    para.setLeewardFlag(false);
                    para.setLeewardRace(false);
                    para.setLeewardLat(Double.NaN);
                    para.setLeewardLon(Double.NaN);
                }
                updateMarkerButtons();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog. Nothing to do.
            }
        });
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.mipmap.ic_launcher);
        AlertDialog alert = builder.create();
        alert.show();
    }
    /**
     * Class method to start racing toward Windward mark
     * initiated by UI buttons defined in activity_start_race
     */
    public void goWindwardMark() {
        if (para.getWindwardFlag()) {
            para.setWindwardRace(true);
            para.setLeewardRace(false);
            CourseOffset = 0.0d;            // on Upwind leg we sail to the wind, no correct required
            para.setCourseOffset(CourseOffset);
            TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, TWS);
            para.setTackGybe(TackGybe);
        } else {
            para.setWindwardRace(false);
        }
        updateMarkerButtons();
    }

    /**
     * Class method to start racing toward Leeward mark
     * initiated by UI buttons defined in activity_start_race
     */
    public void goLeewardMark() {
        if (para.getLeewardFlag()) {
            para.setLeewardRace(true);
            para.setWindwardRace(false);
            CourseOffset = 180.0d;          // on Downwind leg we sail away from the wind
            para.setCourseOffset(CourseOffset);
            TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, TWS);
            para.setTackGybe(TackGybe);
        } else {
            para.setLeewardRace(false);
        }
        updateMarkerButtons();
    }

    /**
     * Class method to update the color/text in the Marker Buttons
     */
    public void updateMarkerButtons() {
        if (windex == 0) {
            // only need to worry about these button when we're in the non-Windex mode
            if (para.getWindwardFlag() || para.getLeewardFlag()) {
                // we update the button status to false (inactive) when the "markerIsSet" is set to true
                btnMinusTen.setEnabled(false);
                btnMinusOne.setEnabled(false);
                btnPlusTen.setEnabled(false);
                btnPlusOne.setEnabled(false);
            } else {
                // we update the button status to true (active) when the "markerIsSet" is set to false
                btnMinusTen.setEnabled(true);
                btnMinusOne.setEnabled(true);
                btnPlusTen.setEnabled(true);
                btnPlusOne.setEnabled(true);
            }
            if (para.getLeewardFlag() && para.getLeewardRace()) {
                btnSetLeewardMark.setText("CLR");
                btnGoLeewardMark.setTextColor(GREEN);
            } else if (para.getLeewardFlag()) {
                btnSetLeewardMark.setText("CLR");
                btnGoLeewardMark.setTextColor(RED);
            } else {
                btnSetLeewardMark.setText("SET");
                btnGoLeewardMark.setTextColor(WHITE);
            }
            if (para.getWindwardFlag() && para.getWindwardRace()) {
                btnSetWindwardMark.setText("CLR");
                btnGoWindwardMark.setTextColor(GREEN);
            } else if (para.getWindwardFlag()) {
                btnSetWindwardMark.setText("CLR");
                btnGoWindwardMark.setTextColor(RED);
            } else {
                btnSetWindwardMark.setText("SET");
                btnGoWindwardMark.setTextColor(WHITE);
            }
        } else {
            if (para.getLeewardFlag() && para.getLeewardRace()) {
                outputLeewardMark.setText("LWD Mark: GO");
                outputLeewardMark.setTextColor(GREEN);
            } else if (para.getLeewardFlag()) {
                outputLeewardMark.setText("LWD Mark: SET");
                outputLeewardMark.setTextColor(RED);
            } else {
                outputLeewardMark.setText("LWD Mark: Not Set");
                outputLeewardMark.setTextColor(WHITE);
            }
            if (para.getWindwardFlag() && para.getWindwardRace()) {
                outputWindwardMark.setText("WWD Mark: GO");
                outputWindwardMark.setTextColor(GREEN);
            } else if (para.getWindwardFlag()) {
                outputWindwardMark.setText("WWD Mark: SET");
                outputWindwardMark.setTextColor(RED);
            } else {
                outputWindwardMark.setText("WWD Mark: Not Set");
                outputWindwardMark.setTextColor(WHITE);
            }
        }

        // reset variable upon tack / course change
        timeCounter = 0.0d;
        sumVariances = 0.0d;
        COGfifo = new fifoQueueDouble(smooth_GPS);

        TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, TWS);
        para.setTackGybe(TackGybe);
        para.setTack(tack);

        if (CourseOffset == 0.0d) {
            // Upwind leg - we need to change meanHeadingTaget from downwind to upwind leg
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - gybeAngle - 180.0 - tackAngle);
                if (windex == 1) {
                    boat.setImageResource(R.drawable.windward_stbd);
                } else {
                    boat.setImageResource(R.drawable.boatgreen);
                }
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + gybeAngle + 180.0 + tackAngle);
                if (windex == 1) {
                    boat.setImageResource(R.drawable.windward_port);
                } else {
                    boat.setImageResource(R.drawable.boatred);
                }
            }
        } else {
            // Downwind leg - we need to change meanHeadingTaget from upwind to downwind leg
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + tackAngle + 180.0 + gybeAngle);
                if (windex == 1) {
                    boat.setImageResource(R.drawable.leeward_stbd);
                } else {
                    boat.setImageResource(R.drawable.boatgreen_reach);
                }
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - tackAngle - 180.0 - gybeAngle);
                if (windex == 1) {
                    boat.setImageResource(R.drawable.leeward_port);
                } else {
                    boat.setImageResource(R.drawable.boatred_reach);
                }
            }
        }
    }

    /**
     * Class method to compute a theoretical Mean Wind Direction (MWD) from the meanHeadingTarget
     * and the assumed optimum tack angle
     */
    public double MWD() {
        if (tack.equals("stbd")) {
            return (NavigationTools.fixAngle(meanHeadingTarget + TackGybe + CourseOffset));
        }
        return (NavigationTools.fixAngle(meanHeadingTarget - TackGybe + CourseOffset));
    }

    /**
     * Class method to detect course (starboard / port) and (Windward / Leeward)
     * we are sailing on.  Once we know where we are going, we update the displays.
     */
    void checkForCurrentCourse() {
        if (windex == 1 && autotack) {
            // with the Windex on we can use the current TWA to determine our current course

            if (TWA < 0.0d && tack.equals("stbd")) {
                tack = "port";
                updateMarkerButtons();
            }
            if ( TWA >= 0.0d && tack.equals("port")) {
                tack = "stbd";
                updateMarkerButtons();
            }

            if (Math.abs(TWA) > 90.0d && para.getWindwardRace()) {
                //Log.d(LOG_TAG, "Go to Leeward Mark!");
                goLeewardMark();
            }
            if (Math.abs(TWA) <= 90.0d && para.getLeewardRace()) {
                //Log.d(LOG_TAG, "Go to Windward Mark!");
                goWindwardMark();
            }
        }
    }
}
