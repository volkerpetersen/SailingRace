package com.kaiserware.sailingrace;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.res.ColorStateList;
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
import java.util.TimeZone;

/**
 * Fragment to manage all screen updates, location updates, and computations during
 * the race.  This class supports race to Windward and Leeward marks (one each) and a
 * manual wind mode (no Windex instrument) and a Windex instrument supported race mode.
 * This Activity can be launched from either the "Activity_Main" or the "Activity_StartSequence"
 * when the countdown timer has reached zero and the race starts.
 *
 * Integrates the wind data from the SailTimerWind Bluetooth Windex when the SharedPreference
 * key_Windex = windex == 1 (via the our own AppBackgroundServices class)
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
public class Fragment_RaceInfo extends Fragment {
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
    private TextView outputWindPeriod;          // output field for 1/2 of the FFT wind period duration
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
    private ImageView boat;                     // ImageView of the Boat
    private ImageView outputWindGaugeNeedle;    // ImageView of the Wind Gauge Needle
    private ImageView outputWindGaugeArrow;     // ImageView of the Wind Gauge Tack/Gybe angle indicator arrow
    private GlobalParameters para;              // class object for the global app parameters
    private boolean TRUE_APPARENT = false;      // true = TRUE, false = APPARENT wind values (angle, direction, speed) to be displayed
    private double execution_ctr;               // count of loops thru the main loop
    private double avg_execution_time;          // avg computation time for the main loop
    private double AWA = 10.0d;                 // Apparent Wind Angle computed from the GPS and Windex data. Range 0 to +/-180
    private double AWD = 0.0d;                  // Apparent Wind Direction received from the Windex Data
    private double AWS = 0.0d;                  // Apparent Wind Speed received from the Windex Data
    private double TWA = 0.0d;                  // True Wind Angle computed from the Windex and GPS data. Range 0 to +/-180
    private double TWD = 0.0d;                  // True Wind Direction computed from the Windex and GPS data
    private double TWS = 0.0d;                  // True Wind Speed (kts) compute from the Windex and GPS data
    private double meanHeadingTarget;           // Mean Heading Target either manual goal or current avg COG
    private double meanWindDirection;           // calculated by adding tackAngle / gybeAngle to COG
    private double timeCounter = 0.0d;          // keeps duration of current header / lift sequence
    private double lastDelta = 0.0d;            // keeps track of the last differential between the NavigationTools.TWD_longAvg and TWD_shortAVG
    private double lastDuration = 0.0d;         // keeps track of the duration of the last wind oscillation cycle
    private double sumVariances = 0.0d;         // sum of the heading variances while in a Header or Lift sequence
    private double vmgu = 0.0;                  // Velocity Made Good upwind
    public static int screenUpdates;            // screen update frequency in sec.  Set in preferences.
    private String tack = "stbd";               // "stbd" or "port" depending on current active board we're sailing on
    private double CourseOffset = 0.0d;         // Upwind leg=0.0  |  Downwind leg=180.0
    private double TackGybe;                    // contains tackAngle Upwind and gybeAngle Downwind
    private double tackAngle;                   // upwind leg tack angle set in preferences
    private double gybeAngle;                   // downwind leg gybe angle set in preferences
    private int polars;                         // SharedPreference to indicate if Capri-25 polars are used (1) or not (0)
    private int WINDEX;                         // SharedPreference to indicate if Windex is used (1) or not (0)
    private boolean autotack = true;            // with Windex enabled, program will auto tack / gybe when set to true
    private Handler ScreenUpdate=new Handler(); // Handler to implement the Runnable for the Screen Updates
    public static long current_duration=0;      // duration of the current wind Oscillation
    public static boolean above_mean=true;      // is the current oscillation above mean (true) or below (false)
    private Context appContext;
    private ColorStateList WHITE;
    private ColorStateList RED;
    private ColorStateList GREEN;
    private ColorStateList YELLOW;
    private View view;
    private double[] DistanceBearing = new double[2];
    private static final String LOG_TAG = Fragment_RaceInfo.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        appContext = getActivity().getApplicationContext();

        // initialize our Global Parameter singleton class
        para = GlobalParameters.getInstance();
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        para.current_cycle_start = ( c.getTimeInMillis() )/1000L;       // get time in seconds
        para.previous_cycle_start = para.current_cycle_start;
        para.last_cycle_swap = para.current_cycle_start;

        // fetch Sailing Preferences to determine if we need the Windex layout or the non-Windex one
        WINDEX = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // boolean value to indicate if app is using / not using the Bluetooth connected Windex
        if (WINDEX >= 1) {
            // Inflate the layout used in the Windex Anemometer mode
            view = inflater.inflate(R.layout.fragment_race_info_numeric_windex, container, false);
        } else {
            // Inflate the layout used in the non-Windex mode
            view = inflater.inflate(R.layout.fragment_race_info_numeric_no_windex, container, false);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

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
        polars = SailingRacePreferences.FetchPreferenceValue("key_Polars", appContext); // =1 if we use Capri-25 build-in polars
        tackAngle = (double) SailingRacePreferences.FetchPreferenceValue("key_TackAngle", appContext);  // tack angle
        gybeAngle = (double) SailingRacePreferences.FetchPreferenceValue("key_GybeAngle", appContext); // gybe angle
        WINDEX = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // boolean value to indicate if app is using / not using the Bluetooth connected Windex

        para.setBestTack("- -");

        execution_ctr = 0.0d;
        avg_execution_time = 0.0d;

        if (WINDEX == 0) {
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
                    startActivity(new Intent(appContext, Fragment_DisplayGoogleMap.class));
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
            // the Broadcast Receiver to receive the Anemometer data is set up in AppBackgroundServices
            //--------------------------------------------------------------------------------------
            outputTBStext = (TextView) view.findViewById(R.id.TBStext);
            outputTBS = (TextView) view.findViewById(R.id.TBS);
            outputTWS = (TextView) view.findViewById(R.id.WindGaugeTWS);
            outputWindPeriod = (TextView) view.findViewById(R.id.wind_period);
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
            ScreenUpdate.removeCallbacks(updateScreenNow);   // stop the ScreenUpdate Handler Runnable
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error on Destroy: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        para.setBestTack("- -");

        updateMarkerButtons();
        ScreenUpdate.post(updateScreenNow);

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
        ScreenUpdate.removeCallbacks(updateScreenNow);   // stop the ScreenUpdate Handler Runnable
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
            String tmp;
            double delta;
            double[] Laylines = new double[5];
            double[] goalPolars = new double[2];
            double goalVMGu;
            double elapsed;

            //long startTime = System.currentTimeMillis();

            // if we have an active Leeward or Windward mark, compute distance and Bearing to mark and the optimum Laylines
            if (para.getLeewardRace()) {
                DistanceBearing = NavigationTools.MarkDistanceBearing(para.getBoatLat(), para.getBoatLon(), para.getCommitteeLat(), para.getCommitteeLon());
                Laylines = NavigationTools.optimumLaylines(para.getBoatLat(), para.getBoatLon(), para.getCommitteeLat(), para.getCommitteeLon(), TWD, para.getCourseOffset(), TackGybe, tack);
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

            if (WINDEX >= 1) {
                //--------------------------------------------------------------------------------------
                // Windex screen updates
                //--------------------------------------------------------------------------------------
                AWD = para.getAWD();
                AWS = para.getAWS();
                AWA = para.getAWA();
                TWA = para.getTWA();
                TWD = para.getTWD();
                TWS = para.getTWS();

                if (radioTRUE.isChecked() ) {
                    TRUE_APPARENT = true;
                } else {
                    TRUE_APPARENT = false;
                }

                // compute the wind oscillation and update the UI accordingly
                windOscillations();

                checkForCurrentCourse();    // detect current tack and update the display (boat icon, Mark buttons)
                updateWindGauge();          // also compute TackGybe angle!
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
                lastDelta = updateVarianceSumAvg(delta, lastDelta);
            }


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

            outputStatus.setText(AppBackgroundServices.gpsStatus);
            tmp = "Lat:" + NavigationTools.PositionDegreeToString(para.getBoatLat(), true);
            outputLatitude.setText(tmp);

            tmp = "Lon:" + NavigationTools.PositionDegreeToString(para.getBoatLon(), false)
                    + "  δ:" + appContext.getString(R.string.Deg1, para.getDeclination());
            outputLongitude.setText(tmp);

            /**
            execution_ctr += 1.0d;
            elapsed = (double)(System.currentTimeMillis()-startTime);
            avg_execution_time += elapsed;
            tmp ="time = "+appContext.getString(R.string.DF2, elapsed)+"    avg execution time (milli sec) = "+appContext.getString(R.string.DF2, avg_execution_time/execution_ctr);
            Log.d(LOG_TAG, tmp);
            */

            // set the screen display frequency
            ScreenUpdate.postDelayed(this, (long) (screenUpdates * 1000));
        }
    };

    /**
     * Method to compute the frequency and amplitude of the wind oscillation.
     * This algorithm uses the NavigationTools.TWD_longAVG and NaviagtionTools.TWD_shortAVG
     * values to determine the position of the short term average wind direction to the long-term average.
     *
     * Based on that determination we can determine if we have a header of lift, the strength of it,
     * and its duration.
     *
     * @param - none
     * @return - none
     */
    public void windOscillations() {
        double delta, sign, half_period;
        long current_time;
        long last_duration;
        String tmp;

        // compute the Fast Fourier Transformation of current TWD to get the predominant frequency
        NavigationTools.fft_wind_frequency(screenUpdates);
        tmp = "wind() frequency="+NavigationTools.wind_frequency+"  period="+appContext.getString(R.string.DF0, (double)NavigationTools.wind_period)+"  TWD size="+NavigationTools.TWD.size();
        //Log.d(LOG_TAG, tmp);
        //Log.d(LOG_TAG, "windCTR="+AppBackgroundServices.windCTR+"  longAVG="+NavigationTools.longAvg);
        if (!Double.isNaN(NavigationTools.wind_frequency)) {
            half_period = NavigationTools.wind_period / 2.0d;
            tmp = appContext.getString(R.string.MinSec, Math.floor(half_period / 60.0d), (half_period%60.0d));
            outputWindPeriod.setText(tmp);
        } else {
            outputWindPeriod.setText("--");
        }

        delta = NavigationTools.HeadingDelta(NavigationTools.TWD_longAVG, NavigationTools.TWD_shortAVG);
        //Log.d(LOG_TAG, "delta="+delta+"  short_avg="+NavigationTools.TWD_shortAVG+"  long_avg="+NavigationTools.TWD_longAVG);
        //Log.d(LOG_TAG, "number of datapoints="+NavigationTools.TWD.size());


        Calendar c = Calendar.getInstance(Activity_Main.TZ_UTC);
        current_time = c.getTimeInMillis() / 1000L;
        if (lastDelta*delta < 0.0d) {
            // when lastDelta and delta are of opposite sign we crossed the long-term average.
            // We need to reset the current_cylce and previous_cycle timers for the current Lift /
            // Header and store the start time of the last wind oscillation.
            // We do so only when the last oscillation lasted at least 45 sec!
            // we set above_mean to true if delta is positive, otherwise false
            //
            if (current_time-para.last_cycle_swap > 45L) {
                para.previous_cycle_start = para.current_cycle_start;
                para.current_cycle_start = current_time - 45L;
                above_mean = (delta >= 0.0d);
            }
            para.last_cycle_swap = current_time;
        }
        current_duration = current_time-para.current_cycle_start;
        last_duration = para.current_cycle_start-para.previous_cycle_start;
        //Log.d(LOG_TAG, "last_delta="+lastDelta+"  delta="+delta);
        //Log.d(LOG_TAG, "cur="+current_time+"  c_cycle="+para.current_cycle_start+"  p_cycle="+para.previous_cycle_start);

        if (CourseOffset == 0.0d) {
            // Upwind leg (CourseOffset = 0.0)
            sign = 1.0d;
        } else {
            // Downwind leg (CourseOffset = 180.0)
            sign = -1.0d;
        }
        if (tack.equals("port")) {
            sign = -sign;
        }
        sign = delta * sign;

        if (sign < 0.0d) {
            outputHeaderLift.setText("Header");
            outputHeaderLift.setTextColor(RED);
        } else {
            outputHeaderLift.setText("Lift");
            outputHeaderLift.setTextColor(GREEN);
        }
        tmp = appContext.getString(R.string.Degrees, Math.abs(sign))+"/"+appContext.getString(R.string.Degrees, NavigationTools.TWD_longAVG);
        outputAvgVariance.setText(tmp);
        tmp = "C: "+appContext.getString(R.string.MinSec, Math.floor(current_duration / 60.0d), (current_duration%60.0d));
        tmp += "\nP: "+appContext.getString(R.string.MinSec, Math.floor(last_duration / 60.0d), (last_duration%60.0d));
        outputVarDuration.setText(tmp);

        lastDelta = delta;

        return;
    }

    /**
     * Method to update the display of the WindGauge and the complementing text output
     * The windRecord contains {TWD, smoothedTWD, avgSmoothedTWD, amplitude, frequency, COG, SOG, and tack}.
     */
    public void updateWindGauge() {
        double angle;
        if (para.getSailtimerStatus()) {
            outputTWS.setTextColor(YELLOW);
        } else {
            outputTWS.setTextColor(RED);
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
        TackGybe = NavigationTools.getTackAngle(WINDEX, polars, tack, tackAngle, gybeAngle, CourseOffset, TWS);
        //if (CourseOffset == 0.0d) {
        angle = CourseOffset-TackGybe;
        //} else {
        //    angle = CourseOffset-TackGybe;
        //}
        if (!TRUE_APPARENT) {
            // if we're displaying Apparent wind data, convert the polar TWA (angle) to an AWA
            angle = NavigationTools.TWAtoAWA(para.getSOG(),angle, TWS);
        }
        outputWindGaugeArrow.setRotation((float)NavigationTools.HeadingDelta(0.0d, angle));
        //Log.d(LOG_TAG, "angle="+angle+"  CourseOffset="+CourseOffset+" TackGybe="+TackGybe+"  Rot="+NavigationTools.HeadingDelta(0.0d, angle));
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
     * initiated by UI buttons defined in activity_dual_pane and the onClick listener in onCreate
     *
     * @param mapMarkerSet  boolean that is true when a position has been set. In that case this
     *                      method will delete the existing mark
     */
    public void setWindwardMark(boolean mapMarkerSet) {
        if (!mapMarkerSet) {
            para.setWindwardLat(para.getBoatLat());
            para.setWindwardLon(para.getBoatLon());
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
     * initiated by UI buttons defined in activity_dual_pane and the onClick listener in onCreate
     *
     */
    public void setLeewardMark() {
        para.setCommitteeLat(para.getBoatLat());
        para.setCommitteeLon(para.getBoatLon());
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
                    para.setCommitteeLat(Double.NaN);
                    para.setCommitteeLon(Double.NaN);
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
     * initiated by UI buttons defined in activity_dual_pane
     */
    public void goWindwardMark() {
        if (para.getWindwardFlag()) {
            para.setWindwardRace(true);
            para.setLeewardRace(false);
            CourseOffset = 0.0d;            // on Upwind leg we sail to the wind, no correct required
            para.setCourseOffset(CourseOffset);
            TackGybe = NavigationTools.getTackAngle(WINDEX, polars, tack, tackAngle, gybeAngle, CourseOffset, TWS);
            para.setTackGybe(TackGybe);
        } else {
            para.setWindwardRace(false);
        }
        updateMarkerButtons();
    }

    /**
     * Class method to start racing toward Leeward mark
     * initiated by UI buttons defined in activity_dual_pane
     */
    public void goLeewardMark() {
        if (para.getLeewardFlag()) {
            para.setLeewardRace(true);
            para.setWindwardRace(false);
            CourseOffset = 180.0d;          // on Downwind leg we sail away from the wind
            para.setCourseOffset(CourseOffset);
            TackGybe = NavigationTools.getTackAngle(WINDEX, polars, tack, tackAngle, gybeAngle, CourseOffset, TWS);
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
        if (WINDEX == 0) {
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

        TackGybe = NavigationTools.getTackAngle(WINDEX, polars, tack, tackAngle, gybeAngle, CourseOffset, TWS);
        para.setTackGybe(TackGybe);
        para.setTack(tack);

        if (CourseOffset == 0.0d) {
            // Upwind leg - we need to change meanHeadingTaget from downwind to upwind leg
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - gybeAngle - 180.0 - tackAngle);
                if (WINDEX >= 1) {
                    boat.setImageResource(R.drawable.windward_stbd);
                } else {
                    boat.setImageResource(R.drawable.boatgreen);
                }
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + gybeAngle + 180.0 + tackAngle);
                if (WINDEX >= 1) {
                    boat.setImageResource(R.drawable.windward_port);
                } else {
                    boat.setImageResource(R.drawable.boatred);
                }
            }
        } else {
            // Downwind leg - we need to change meanHeadingTaget from upwind to downwind leg
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + tackAngle + 180.0 + gybeAngle);
                if (WINDEX >= 1) {
                    boat.setImageResource(R.drawable.leeward_stbd);
                } else {
                    boat.setImageResource(R.drawable.boatgreen_reach);
                }
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - tackAngle - 180.0 - gybeAngle);
                if (WINDEX >= 1) {
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
        if (WINDEX >= 1 && autotack) {
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
                //Log.d(LOG_TAG, "Go to Windward Mark!");
                goLeewardMark();
                TRUE_APPARENT = false;
            }
            if (Math.abs(TWA) <= 90.0d && para.getLeewardRace()) {
                //Log.d(LOG_TAG, "Go to Leeward Mark!");
                goWindwardMark();
                TRUE_APPARENT = true;
            }
        }
    }
}
