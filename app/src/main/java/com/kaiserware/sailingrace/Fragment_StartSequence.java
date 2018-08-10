package com.kaiserware.sailingrace;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment to display the numerical start timer information, run the start timer, and to set
 * Windward and Leeward (Committee and Pin) positions in preparation for the race.  User can either use 
 * the "Back" button to return to the "Activity_Main" or this Fragment (and its parent Activity 
 * "start_timer_Activity" will terminate upon end of countdown timer and launch the "Activity_RaceInfo".
 *
 * All angles used in computations are assumed to be True North.  Angles displayed on screen are 
 * measured in Magnetic North (True North + declination) except for TWA and AWA which are the difference
 * between the Boat heading and the Wind Direction!
 * 
 * All speeds are measure in Nautical Miles per Hour (kts)
 *
 * Created by Volker Petersen on November 2015.
 *
 */
public class Fragment_StartSequence extends Fragment {
    private final int MILLISECONDS = 1000;      // conversion seconds to miliseconds
    private boolean timerHasStarted = false;    // flag that indicates if our countdown timer has started
    private boolean startline = false;          // flag that indicates to display the start-line stats
    private int WINDEX;                         // flag = 1 if the SailTimerAPI Wind Anemometer is utilized
    private int minutes;                        // minute duration of the each start sequence segment
    private int seconds;                        // seconds duration of the each start sequence segment
    private int countdown;                      //
    public static int secondsToGun;             // seconds to Start Signal of the Class we are racing in
    private int ourClass;                       // id of the Class in which we are racing
    private int currentClass = 0;               // id of the current Class getting ready to start
    private boolean canTalk = false;            // flag that indicates if we use text-to-speech function
    private int warning = 1;  // warning = 1 if a warning period is at the beginning of the the start sequence
    private int remainingClock;
    private String classNames[];
    private String tmp;
    private DecimalFormat twoDigits = new DecimalFormat("00");
    private NumberPicker[] nps=new NumberPicker[3];
    private TextView ct;
    private TextView classSequence;
    private TextView TWDvalue;
    private TextView TWSvalue;
    private TextView TTLcommittee;
    private TextView TTLpin;
    private TextView TTLintersect;
    private TextView favored_degrees;
    private TextView favored_string;
    private CountDownTimer countDownTimer = null;
    private ToneGenerator sound;
    private TextToSpeech talk;
    private Context appContext;
    private Button btnCommitteeMarkSet;
    private Button btnLeewardMarkPinSet;
    //private Button btnWindwardMarkSetManual;
    private Button btn_set_WL_Course;
    private Button btnWindwardMarkSetAuto;
    private Button btnCountdownStart;
    private Button countdownPlus;
    private Button countdownMinus;
    private GlobalParameters para;
    private GPSLocation gps;
    private ColorStateList GRAY;
    private ColorStateList WHITE;
    private ColorStateList RED;
    private ColorStateList GREEN;
    private View view;
    static final String LOG_TAG = Activity_StartSequence.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        appContext = getActivity().getApplicationContext();

        view = inflater.inflate(R.layout.fragment_start_sequence, container, false);

        // initialize the Global Parameter singleton class
        para = GlobalParameters.getInstance();

        // fetch the Shared Preferences from the class FetchPreferenceValues in file SailingRacePreferences
        // Shared Preferences key names are defined in SailingRacePreferences.OnCreate()
        seconds = 0;
        minutes = SailingRacePreferences.FetchPreferenceValue("key_StartSequence", appContext);
        warning = SailingRacePreferences.FetchPreferenceValue("key_Warning", appContext);
        ourClass = SailingRacePreferences.FetchPreferenceValue("key_RaceClass", appContext)+warning+1;
        WINDEX = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // boolean value to indicate if app is using / not using the Bluetooth connected Windex
        if (warning == 1) {
            classNames = new String[] {"Warning", "Preparatory", "S2", "J/24", "J/22", "Sonar", "Capri25", "Ensign", "MORC"};
        } else {
            classNames = new String[] {"Preparatory", "S2", "J/24", "J/22", "Sonar", "Capri25", "Ensign", "MORC"};
        }

        // initialize the location services in class in file GPSlocation.java
        /*
        COGfifo = new fifoQueueDouble(smooth_GPS);
        SOGfifo = new fifoQueueDouble(smooth_GPS);
        gps = new GPSLocation(this, gpsUpdates, smooth_GPS, COGfifo, SOGfifo, REQUEST_CODE_FINE_LOCATION);
        */

        // initialize the TextView output fields
        TWDvalue = (TextView) view.findViewById(R.id.TWD);
        TWSvalue = (TextView) view.findViewById(R.id.TWS);
        TTLcommittee = (TextView) view.findViewById(R.id.TTLcommittee);
        TTLpin = (TextView) view.findViewById(R.id.TTLpin);
        TTLintersect = (TextView) view.findViewById(R.id.TTLintersect);
        favored_degrees = (TextView) view.findViewById(R.id.favoredDegrees);
        favored_string = (TextView) view.findViewById(R.id.favoredString);
        ct = (TextView) view.findViewById(R.id.countdownString);
        classSequence = (TextView) view.findViewById(R.id.classSequence);

        // initialize the color variables (type ColorStateList)
        GRAY = ContextCompat.getColorStateList(appContext, R.color.GRAY);
        WHITE = ContextCompat.getColorStateList(appContext, R.color.WHITE);
        RED = ContextCompat.getColorStateList(appContext, R.color.RED);
        GREEN = ContextCompat.getColorStateList(appContext, R.color.GREEN);

        // Initialize the Start, set Windward / Leeward Map Position buttons and register btn listeners
        btnCommitteeMarkSet = (Button) view.findViewById(R.id.button_setLeeward);
        btnLeewardMarkPinSet = (Button) view.findViewById(R.id.button_setLeewardPin);
        btn_set_WL_Course = (Button) view.findViewById(R.id.button_set_WL_Course);
        //btnWindwardMarkSetManual = (Button) view.findViewById(R.id.button_setWindwardManual);
        btnWindwardMarkSetAuto = (Button) view.findViewById(R.id.button_setWindwardAuto);
        btnCountdownStart = (Button) view.findViewById(R.id.button_countdownStart);

        btnCountdownStart.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 countdown = (minutes*60 + seconds) * MILLISECONDS;
                 countdown_start( countdown*(ourClass-currentClass+1) );
             }
        });

        btnCommitteeMarkSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if we have a Click, we want to go Set or Clear the Committee Boat mark
                if (para.getLeewardFlag()) {
                    // clear the Committee Boat Mark if user confirms the deletion
                    clearMarker("Leeward");
                } else {
                    // set the Committee Boat Mark
                    //Log.d(LOG_TAG, "setting Committee Boat Mark.  Location changed: "+gps.LOCATION_CHANGED);
                    if (!Double.isNaN(para.getBoatLat())) {
                        startline = true;
                        para.setLeewardFlag(true);
                        para.setCommitteeLat(para.getBoatLat());
                        para.setCommitteeLon(para.getBoatLon());
                    }
                    display_update(minutes, seconds);
                }
                updateButtons();
            }
        });

        btnLeewardMarkPinSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if we have a Click, we want to go Set or Clear the Pin mark
                if (para.getLeewardPinFlag()) {
                    // clear the Leeward Pin Mark if user confirms the deletion
                    clearMarker("LeewardPin");
                } else {
                    // set the Leeward Mark
                    //Log.d(LOG_TAG, "setting Leeward Pin Mark.  Location changed: "+!Double.isNaN(para.getBoatLat()));
                    if (!Double.isNaN(para.getBoatLat())) {
                        startline = true;
                        para.setLeewardPinFlag(true);
                        para.setPinLat(para.getBoatLat());
                        para.setPinLon(para.getBoatLon());
                    }
                    display_update(minutes, seconds);
                }
                updateButtons();
            }
        });

        if (Activity_StartSequence.DualPane) {
            // In Dual Pane mode hide the WWD Set button in "Fragment_StartSequence"
            //btnWindwardMarkSetManual.setVisibility(View.GONE);
            btn_set_WL_Course.setVisibility(View.GONE);
        } else {
            // In Single Pane mode initialize the listener and replace current "Fragment_StartSequence"
            // with the "Fragment_DisplayGoogleMap" using the "HelperClass_FragmentChangeListener"
            //btnWindwardMarkSetManual.setOnClickListener(new View.OnClickListener() {
            btn_set_WL_Course.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go Set or Clear the Windward mark

                    if (para.getWindwardFlag()) {
                        // clear the Windward Mark if user confirms the deletion
                        clearMarker("Windward");
                    } else {
                        // start the Google Maps Fragment (MapsActivity) to set the Windward Mark
                        //Log.d(LOG_TAG, "Launching the Fragment_DisplayGoogleMap");
                        Fragment fr=new Fragment();
                        HelperClass_FragmentChangeListener fc=(HelperClass_FragmentChangeListener)getActivity();
                        fc.replaceFragment(fr);
                    }

                    updateButtons();
                }
            });
        }


        // and we add the button listener to set the Windward Mark if we have the start-line set
        btnWindwardMarkSetAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if we have a Click, user wants to go set the Windward mark 1 nm upwind
                // compute the center of the start-line and then go 1nm to windward
                double lat = (para.getCommitteeLat()+para.getPinLat()) / 2.0;
                double lon = (para.getCommitteeLon()+para.getPinLon()) / 2.0;
                double[] LatLon = NavigationTools.withDistanceBearingToPosition(lat, lon, 1.0, para.getTWD());

                // store Windward marker position in global parameters
                para.setWindwardLat(LatLon[0]);
                para.setWindwardLon(LatLon[1]);
                para.setWindwardFlag(true);
                para.setWindwardRace(true);
                updateButtons();
            }
        });

        // Initialize the countdown Plus/Minus buttons and register btn listeners
        countdownPlus = (Button) view.findViewById(R.id.countdownPlus);
        countdownPlus.setEnabled(false);
        countdownPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustCountdownTimer(+1);
            }
        });

        countdownMinus = (Button) view.findViewById(R.id.countdownMinus);
        countdownMinus.setEnabled(false);
        countdownMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustCountdownTimer(-1);
            }
        });


        // Initialize the three Number Pickers.  Listener is registered in the layout file
        nps[0]= (NumberPicker) view.findViewById(R.id.numberPickerMinutes);
        nps[1]= (NumberPicker) view.findViewById(R.id.numberPickerSeconds);
        nps[2]= (NumberPicker) view.findViewById(R.id.numberPickerClass);

        NumberPicker.OnValueChangeListener onValueChangedMinutes=new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                minutes = newVal;
                display_update(minutes, seconds);
            }
        };
        NumberPicker.OnValueChangeListener onValueChangedSeconds=new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                seconds = newVal;
                display_update(minutes, seconds);
            }
        };
        NumberPicker.OnValueChangeListener onValueChangedClass=new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                ourClass = newVal+warning+1;
                display_update(minutes, seconds);
            }
        };

        String[] values = new String[60];
        for (int i = 0; i < values.length; i++){
            values[i]=Integer.toString(i);
        }

        // initialize the values for the three Number Pickers
        for(int i=0;i<3;i++){
            nps[i].setMinValue(0);
            if (i == 2) {
                // race class number picker initialization.  First strip out Warning and Preparatory periods
                String[] raceClasses = Arrays.copyOfRange(classNames, warning+1, classNames.length);
                nps[i].setDisplayedValues(raceClasses);
                nps[i].setMaxValue(raceClasses.length - 1);
            } else {
                // minutes and seconds number picker initialization
                nps[i].setMaxValue(values.length - 1);
                nps[i].setDisplayedValues(values);
            }
        }
        nps[0].setOnValueChangedListener(onValueChangedMinutes);
        nps[1].setOnValueChangedListener(onValueChangedSeconds);
        nps[2].setOnValueChangedListener(onValueChangedClass);

        nps[0].setValue(minutes);
        nps[1].setValue(seconds);
        nps[2].setValue(ourClass-warning-1);
        nps[2].setWrapSelectorWheel(false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        AppBackgroundServices.writeToSQLite = true;   // start the SQLite DB updates

        Log.d(LOG_TAG, "StartSequence onResume() WWDFlag="+para.getWindwardFlag());
        // initialize the Tome Generator
        sound = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        // initialize the Text-to-Speech app
        talk=new TextToSpeech(appContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    talk.setLanguage(Locale.US);
                    canTalk = true;
                } else {
                    canTalk = false;
                }
            }
        });
        updateButtons();
        display_update(minutes, seconds);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (canTalk) {
            talk.shutdown();
        }
        AppBackgroundServices.writeToSQLite = false;
        //Log.d(LOG_TAG, "onDestroy() after super.onDestroy()");
    }

    /**
     * Update the UI buttons in the Fragment_StartSequence
     */
    public void updateButtons() {
        if (para.getLeewardFlag()) {
            btnCommitteeMarkSet.setText("LWD CLR");
            btnCommitteeMarkSet.setTextColor(RED);
        } else {
            btnCommitteeMarkSet.setText("LWD Set");
            btnCommitteeMarkSet.setTextColor(WHITE);
        }
        if (para.getLeewardPinFlag()) {
            btnLeewardMarkPinSet.setText("PIN CLR");
            btnLeewardMarkPinSet.setTextColor(RED);
        } else {
            btnLeewardMarkPinSet.setText("PIN Set");
            btnLeewardMarkPinSet.setTextColor(WHITE);
        }
        if (para.getWindwardFlag()) {
            btn_set_WL_Course.setText("W/L CLR");
            btn_set_WL_Course.setTextColor(RED);
        } else {
            btn_set_WL_Course.setText("W/L Set");
            btn_set_WL_Course.setTextColor(WHITE);
        }
        if (para.getLeewardFlag() && para.getWindwardFlag()) {
            para.setCenterLat( (para.getWindwardLat()+para.getCommitteeLat()) / 2.0d );
            para.setCenterLon( (para.getWindwardLon()+para.getCommitteeLon()) / 2.0d );
            if (storeRaceLocations()) {
                // TODO nothing to do here other than debugging
            } else {
                Log.d(LOG_TAG, "storeRaceLocations() - Error in storing race location data");
            }
        }

        // only enable the automatic Windward Marker set button when we have the start-line defined
        // and the Windward Marker has not been set yet
        if (para.getLeewardFlag() && para.getLeewardPinFlag() && !para.getWindwardFlag()) {
            btnWindwardMarkSetAuto.setEnabled(true);
            btnWindwardMarkSetAuto.setTextColor(WHITE);
        } else {
            btnWindwardMarkSetAuto.setEnabled(false);
            btnWindwardMarkSetAuto.setTextColor(GRAY);
        }

        if (para.getLeewardFlag() && para.getLeewardPinFlag() && startline) {
            startline = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                    this.getActivity(),android.R.style.Theme_Holo_Dialog));
            double[] results = NavigationTools.MarkDistanceBearing(para.getCommitteeLat(),
                    para.getCommitteeLon(), para.getPinLat(), para.getPinLon());
            if (Double.isNaN(results[0])) {
                tmp = "Starting Line Stats:\n  Length = no GPS signal";
            } else {
                tmp = "Starting Line Stats:\n  Length = "+getString(R.string.DF1, results[0]*1852.0d)+"m";
            }
            if (Double.isNaN(results[1])) {
                tmp += "\n  Direction = no GPS signal";
            } else {
                tmp += "\n  Direction = "+getString(R.string.Degrees, results[1]);
                double favored = NavigationTools.HeadingDelta(NavigationTools.fixAngle(results[1]+90.0d), NavigationTools.TWD_shortAVG);
                if (favored >= 0.0d) {
                    tmp += "\n  Committee-end favored by "+getString(R.string.Degrees, favored);
                } else {
                    tmp += "\n  Pin-end favored by "+getString(R.string.Degrees, -favored);
                }
            }

            builder.setMessage(tmp);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User OKs the dialog. Nothing to do.
                }
            });
            builder.setTitle(R.string.app_name);
            builder.setIcon(R.mipmap.ic_launcher);
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    /**
     * Method to update the SQLite Location database with the Lat/Lon of the Leeward Mark, the
     * Windward Mark, and the Center of the race course.
     *
     * @return true if successful, false if nothing was written to the DB (error or not the WIND number of reading
     */
    private boolean storeRaceLocations() {
        SQLiteDatabase db;

        // if we don't have a valid sqlite db connection, return false
        sqlite_WindDataHelper dbhelper = new sqlite_WindDataHelper(appContext);
        db = dbhelper.getWritableDatabase();
        if (db==null) {
            return false;
        }

        // Create a new map of values, where the DB column names are the keys
        ContentValues values = new ContentValues();
        values.put(sqlite_WindDataHelper.LocationEntry.COLUMN_RACE, para.getRaceID());
        values.put(sqlite_WindDataHelper.LocationEntry.COLUMN_LWD_LAT, noNaN(para.getCommitteeLat()));
        values.put(sqlite_WindDataHelper.LocationEntry.COLUMN_LWD_LON, noNaN(para.getCommitteeLon()));
        values.put(sqlite_WindDataHelper.LocationEntry.COLUMN_WWD_LAT, noNaN(para.getWindwardLat()));
        values.put(sqlite_WindDataHelper.LocationEntry.COLUMN_WWD_LON, noNaN(para.getWindwardLon()));
        values.put(sqlite_WindDataHelper.LocationEntry.COLUMN_CTR_LAT, noNaN(para.getCenterLat()));
        values.put(sqlite_WindDataHelper.LocationEntry.COLUMN_CTR_LON, noNaN(para.getCenterLon()));

        // Insert or Update the new row, returning the primary key value of the new row
        long newRowID = db.insertWithOnConflict(sqlite_WindDataHelper.LocationEntry.TABLE_NAME, null, values, db.CONFLICT_REPLACE);

        dbhelper.closeDB(null, db);
        if (newRowID == -1) {
            Log.d(LOG_TAG, "storeRaceLocations() - failed to write values '"+values.toString()+" to the DB.");
            return false;
        } else {
            Log.d(LOG_TAG, "storeRaceLocations() - wrote values '"+values.toString()+" to the DB record "+newRowID);
            return true;
        }
    }

    /**
     * Method to replace a NaN value with a 0.0
     * @param value to be checked if NaN.  If NaN, replace with 0.0
     * @return
     */
    double noNaN(double value) {
        if (Double.isNaN(value)) {
            return 0.0d;
        } else {
            return value;
        }
    }


    /**
     * Method to create a UI Dialog to get the user's confirmation to delete a race
     * course marker.  The marker name is specified by the @param name.
     * @param marker name of the race course marker to be deleted
     */
    public void clearMarker(final String marker) {
        // Alert Dialog to verify Marker deletion
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this.getActivity(),android.R.style.Theme_Holo_Dialog));
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
                    startline = false;
                    para.setLeewardFlag(false);
                    para.setLeewardRace(false);
                    para.setCommitteeLat(Double.NaN);
                    para.setCommitteeLon(Double.NaN);
                }
                if (marker.equals("LeewardPin")) {
                    startline = false;
                    para.setLeewardPinFlag(false);
                    para.setPinLat(Double.NaN);
                    para.setPinLon(Double.NaN);
                }
                updateButtons();
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
     * Method to update all the UI elements each time a UI interface Button or Numberpicker
     * has changed their value and each time the clock elapsed one second (one interval)
     *
     * @param min the minutes display value
     * @param sec the seconds display value
     */
    public void display_update(int min, int sec) {
        double TTL, favored;
        double[] results, intersection;
        LatLng Boat, Pin, Committee;

        tmp = twoDigits.format(min) + ":" + twoDigits.format(sec);
        ct.setText(tmp);

        if (timerHasStarted) {
            if (classNames[currentClass].equals("Warning") || classNames[currentClass].equals("Preparatory")) {
                tmp = classNames[currentClass]+" Period";
            } else {
                tmp = classNames[currentClass] + " Start";
            }
        } else {
            tmp = "Racing with "+classNames[ourClass];
        }
        classSequence.setText(tmp);
        favored_string.setTextColor(WHITE);

        if (WINDEX >= 1) {
            TWDvalue.setText(appContext.getString(R.string.Degrees, para.getTWD()));
            TWSvalue.setText(appContext.getString(R.string.DF1, para.getTWS()));

            // compute the favored side of the starting line and display that data
            // compute the time to Committee, Pin, and Intersection of current course and the
            // starting line (i.e., line between Committee Boat and Pin)
            if (para.getLeewardPinFlag() && para.getLeewardFlag() && para.getAvgSOG() > 0.0d) {
                Pin = new LatLng(para.getPinLat(), para.getPinLon());
                Committee = new LatLng(para.getCommitteeLat(), para.getCommitteeLon());

                favored = NavigationTools.favoredPin(Committee, Pin, NavigationTools.TWD_shortAVG);
                favored_degrees.setText(appContext.getString(R.string.Degrees, favored));
                favored_string.setText(NavigationTools.favoredPinString(favored));
                if (favored <= 0.0d) {
                    favored_string.setTextColor(RED);
                } else {
                    favored_string.setTextColor(GREEN);
                }

                // get distance/bearing to Committee Boat end of starting line
                results = NavigationTools.MarkDistanceBearing(para.getBoatLat(), para.getBoatLon(),
                        para.getCommitteeLat(), para.getCommitteeLon());
                TTL = results[0] / para.getAvgSOG() * 3600.0d;
                TTLcommittee.setText(NavigationTools.min_sec(TTL));

                // get distance/bearing to Pin end of starting line
                results = NavigationTools.MarkDistanceBearing(para.getBoatLat(), para.getBoatLon(),
                        para.getPinLat(), para.getPinLon());
                TTL = results[0] / para.getAvgSOG() * 3600.0d;
                TTLpin.setText(NavigationTools.min_sec(TTL));

                // get distance/bearing to the intersection of current course and starting line
                Boat = new LatLng(para.getBoatLat(), para.getBoatLon());
                intersection = NavigationTools.LineIntersection(Boat, Committee, Pin, para.getAvgCOG(), para.getAvgSOG());
                if (!Double.isNaN(intersection[0])) {
                    TTLintersect.setText(NavigationTools.min_sec(intersection[0]));
                } else {
                    TTLintersect.setText("- -");
                }
            }
        }
    }

    /**
     * Method to generate the countdown timer voice output
     * @param txt
     */
    public void textToSpeech(String txt) {
        if (canTalk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String utteranceId=this.hashCode() + "";
                talk.speak(txt, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                HashMap<String, String> map = new HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
                talk.speak(txt, TextToSpeech.QUEUE_FLUSH, map);
            }
        }
    }

    /**
     * Class to create the countdown timer using the build-in Android class "CountDownTimer"
     */
    public class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            if (canTalk) {
                talk.shutdown();
            }
            tmp = twoDigits.format(0) + ":" + twoDigits.format(0);
            ct.setText(tmp);

            // set timer_terminated to true so that the function onBackPressed() in the calling
            // Activity ("Activity_StartSequence") of this Fragment can see that this Fragment should
            // be terminated w/o asking for user confirmation.  We only want the user confirmation
            // if the Bakkey is pressed prior to the timer countdown being finished.
            Activity_StartSequence.timer_terminated = true;
            getActivity().onBackPressed();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            secondsToGun = (int)(millisUntilFinished / MILLISECONDS);
            remainingClock = secondsToGun % (int)(countdown / MILLISECONDS);
            int min = (int)(remainingClock / 60.0d);
            int sec = remainingClock - min * 60;
            display_update(min, sec);

            if (remainingClock < 61 && remainingClock > 29){
                if ((remainingClock % 10) == 0) {
                    tmp = Integer.toString(remainingClock);
                    textToSpeech(tmp);
                }
            }
            if (remainingClock < 29 && remainingClock >= 10){
                if ((remainingClock % 5) == 0) {
                    tmp = Integer.toString(remainingClock);
                    textToSpeech(tmp);
                }
            }
            if (remainingClock < 10) {
                sound.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150); // 2nd param is duration in
                tmp = "";
            }
            if (remainingClock == 0) {
                sound.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300); // 2nd param is duration in ms
                ct.setText(twoDigits.format(0) + ":" + twoDigits.format(0));
                currentClass += 1;
                //Log.d(LOG_TAG, "======== remainingClock=0: CurrentClass="+currentClass+"  OurClass="+ourClass+"  name="+classNames[currentClass]);
            }
        }
    }

    /**
     * Method is called when clicking on Button "button_countdownStart" defined in "fragment_start_sequencence.xml"
     * this button is cycled thru this sequence: Start->Stop->Reset->Start.
     * if Reset is pressed with more than 50% left in current countdown, reset and start from top in current class
     * if Reset is pressed with less than 50% left in current countdown, reset and start from top in next class
     * current class is being counted up from 0 to <= ourClass.   ourClass contains the class number of our race class
     */
    public void countdown_start(Integer countdownTime) {
        if(!timerHasStarted) {
            timerHasStarted = true;
            countDownTimer = new MyCountDownTimer(countdownTime, MILLISECONDS);
            //Log.d(LOG_TAG, "\n++++++C_S+++++++ ourClass = "+ourClass+" current class="+currentClass+" time="+countdownTime/MILLISECONDS/(ourClass-currentClass+1));
            //Log.d(LOG_TAG, "++++++C_S+++++++ Countdown Time  = "+countdownTime/MILLISECONDS);
            //Log.d(LOG_TAG, "++++++C_S+++++++ Class Multiplier= "+(ourClass-currentClass+1));
            countDownTimer.start();

            btnCountdownStart.setText("STOP");
            nps[0].setEnabled(false);
            nps[1].setEnabled(false);
            nps[2].setEnabled(false);
            countdownPlus.setEnabled(true);
            countdownMinus.setEnabled(true);
        } else {
            countDownTimer.cancel();
            timerHasStarted = false;
            btnCountdownStart.setText("RESET");
            countdownPlus.setEnabled(false);
            countdownMinus.setEnabled(false);
            //Log.d(LOG_TAG, "Resetting the RaceClass from current="+classNames[ourClass]);
            dialog_Reset_RaceClass();
        }
    }

    /**
     * Method to adjust the countdown timer by the increment amount.  This method is called when the
     * user presses the + or - Button (countdownPlus / countdownMinus)
     *
     * @param increment
     */
    public void adjustCountdownTimer(Integer increment) {
        if (timerHasStarted) {
            int min, sec;
            countDownTimer.cancel();
            min = (int)Math.floor(remainingClock / 60.0d);
            sec = remainingClock - min * 60;
            //Log.d(LOG_TAG,"\n======ACT====== before: remainingClock = "+remainingClock+"  min="+min+"  sec="+sec);
            timerHasStarted = false;
            remainingClock += increment;
            min = (int)Math.floor(remainingClock / 60.0d);
            sec = remainingClock - min * 60 + 1;
            if ( (min*60+sec) > (minutes*60+seconds) ) {
                min = minutes;
                sec = seconds;
            }
            if (sec < 0) {
                sec = 0;
            }
            //Log.d(LOG_TAG,"======ACT====== after: remainingClock = "+remainingClock+"  min="+min+"  sec="+sec);
            //Log.d(LOG_TAG,"======ACT====== Our Class="+ourClass+"  current Class="+currentClass);
            //Log.d(LOG_TAG,"======ACT====== Countdown Time = "+((minutes*60 + seconds)*(ourClass-currentClass) + (min*60+sec)));
            countdown_start( ((minutes*60 + seconds)*(ourClass-currentClass) + (min*60+sec)) * MILLISECONDS );
        }
    }

    /**
     * dialog_Reset_RaceClass Method handles the reset of the Race Class during an already started
     * race start sequence.
     */
    private void dialog_Reset_RaceClass() {
        NumberPicker.OnValueChangeListener onValueChangedRaceClass=new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                currentClass = newVal;
            }
        };

        final Dialog dialog = new Dialog(this.getActivity());
        dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        dialog.setContentView(R.layout.update_raceclass_dialog);
        dialog.setTitle("Correct Start Sequence");
        dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.mipmap.ic_launcher);
        final NumberPicker np = (NumberPicker) dialog.findViewById(R.id.npRaceClass);
        final TextView tv = (TextView) dialog.findViewById((R.id.RaceClassTitle));
        Button reset = (Button) dialog.findViewById(R.id.buttonReset);
        Button cancel= (Button) dialog.findViewById(R.id.buttonCancel);
        tv.setText("Please select the upcoming Start Class and reset the Start Timer");

        //Log.d(LOG_TAG, "Current Class Name="+classNames[currentClass]+" length="+classNames.length);

        np.setDisplayedValues(classNames);
        np.setMaxValue(classNames.length - 1);
        np.setValue(currentClass);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener(onValueChangedRaceClass);
        minutes = nps[0].getValue();
        seconds = nps[1].getValue();

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentClass = np.getValue();
                dialog.dismiss();
                //Log.d(LOG_TAG, "to new class="+classNames[currentClass]+" class number="+currentClass);
                //Log.d(LOG_TAG, "new minutes="+minutes+" & seconds="+seconds);
                btnCountdownStart.performClick();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                currentClass += 1;
                btnCountdownStart.performClick();
            }
        });
        dialog.show();
    }
}
