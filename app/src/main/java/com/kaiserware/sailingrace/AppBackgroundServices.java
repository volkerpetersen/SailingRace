package com.kaiserware.sailingrace;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Calendar;

/**
 * IntentService Class to handle these tasks independent of the UI:
 * 1) receive the apparent wind data from the SailTimerAPI app (if Windex is enabled)
 * 2) convert Apparent wind data to True wind data
 * 3) write wind data to the sqlite DB
 * 4) update COG/SOG/Wind values in global parameters
 *
 * This IntentService will continue to run in the background even if user hits either the
 * Home key to work on some other App else on the devise or hits the power button.
 *
 * This class is initiated by Activity_Main onCreate(0 and it is stopped by Activity_Main on Destroy()
 * by setting the variable 'Activity_Main.runRaceBackgroundServices' to false. In addition, this IntentService
 * is stopped prior to launching SailingRacePreferences and relaunched upon exit of that
 * activity.  This ensures that potential changes of parameters get updated in this IntentService
 *
 * Created by Volker Petersen on 8/29/2016.
 */
public class AppBackgroundServices extends IntentService {
    public static volatile boolean writeToSQLite = false;    // variable is updated in Activity_RaceInfo()
                                                             // & Activity_StartSequence() onResume()
    public static volatile String gpsStatus = "";

    private static final String LOG_TAG = AppBackgroundServices.class.getSimpleName();
    private Handler updateData = new Handler();
    private int dataUpdateFrequency;
    private int WINDEX;
    private BroadcastReceiver WindexBroadcastReceiver;
    private Context appContext;
    private fifoQueueDouble AWDfifo;
    private fifoQueueDouble AWSfifo;
    private static final int WIND = 4;          // store every 4th value in SqLite DB
    private GPSLocation gps = null;             // gps class object to fetch the location data
    public static int windCTR = 0;              // windCTR keeps track of the number loops thru this runnable
    private GlobalParameters para;              // global parameters Singleton
    private Activity hostActivity;              // activity object of the host Activity that initiates this IntentService
    private double[] testData;
    private int testDataSize;
    private boolean testing = false;
    private static DecimalFormat df2 = new DecimalFormat("#0.00");

    public AppBackgroundServices() {
        super("RaceBackgroundServices");
    }

    /**
     * Override the default method onHandleIntent that gets called when this IntentService gets
     * initiated.  Activity_Main onCreate() initiates this IntentService.
     *
     * Once initiated we create a runnable that will run until the public boolean runRaceBackgroundServices
     * is set to false in Activity_Main onDestroy()
     *
     * The runnable performs these four tasks:
     *      a) launch the GPS location services
     *      b) fetch the apparent wind data from the SailTimerAPI Broadcast receiver
     *      c) compute the True wind values from the Apparent Wind readings and the COG/SOG
     *      d) write the wind data to the SQLite db
     *
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // initialize our Global Parameter singleton class
        para = GlobalParameters.getInstance();
        hostActivity = para.mainActivity;

        // fetch the desired dataUpdateFrequency from the Intent Extra
        dataUpdateFrequency = Integer.parseInt(intent.getStringExtra("dataUpdateFrequency"));
        writeToSQLite = false;

        if (dataUpdateFrequency <=0)
            dataUpdateFrequency = 1000; // default value 1 second in milliseconds

        runServices();                  // launch the activities to be handled by this IntentService
    }

    /*
     *  Method to perform all activities of this IntentService
     */
    private void runServices() {
        final int REQUEST_CODE_FINE_LOCATION = 1234;
        fifoQueueDouble COGfifo;
        fifoQueueDouble SOGfifo;

        appContext = getApplicationContext();

        // fetch the avgWIND parameter from the App Preference Values
        WINDEX = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // int value to indicate if app is using / not using the Bluetooth connected Windex
        int smooth_WIND = SailingRacePreferences.FetchPreferenceValue("key_avgWIND", appContext); // max number of WIND values stored in LinkedList.
        int smooth_GPS = SailingRacePreferences.FetchPreferenceValue("key_avgGPS", appContext); // max number of GPS positions stored in LinkedList.
        long gpsUpdates = (long) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateTime", appContext); // Time Interval for GPS position updates in milliseconds

        // initialize the FIFO queues to keep smooth_GPS COG & SOG and smooth_WIND for AWD & AWS
        AWDfifo = new fifoQueueDouble(smooth_WIND);
        AWSfifo = new fifoQueueDouble(smooth_WIND);
        COGfifo = new fifoQueueDouble(smooth_GPS);
        SOGfifo = new fifoQueueDouble(smooth_GPS);

        // initialize the GPSTracker services, buttons, and switch screen on (disable sleep)
        gps = new GPSLocation(hostActivity, gpsUpdates, smooth_GPS, COGfifo, SOGfifo, REQUEST_CODE_FINE_LOCATION);
        if (gps != null)
            gps.startGPS();


        if (WINDEX >= 1) {
            // initialize the NavigationTools Linked Lists
            NavigationTools.TWD.clear();
            NavigationTools.TWD_long_AVG.clear();
            NavigationTools.TWD_short_AVG.clear();
            NavigationTools.TWD_Std_Dev.clear();
            windCTR = 0;
            int longAvg = SailingRacePreferences.FetchPreferenceValue("key_longAvg", appContext); // duration (in min) of long-term TWD average

            if (longAvg>30) {
                longAvg = 30;
            }
            if (longAvg <15) {
                longAvg = 15;
            }
            NavigationTools.longAvg = longAvg*60;     // convert from minutes to seconds
            NavigationTools.shortAvg = 60;            // 60 sec/min duration of the short-term TWD average

            if (WINDEX >= 1) {
                // register the 'SailTimerAPI' broadcast receiver for the SailTimer Anemometer
                // This Broadcast Receiver utilizes the data being send from the 'SailTimer API' app
                try {
                    WindexBroadcastReceiver = SailTimerAPI.getInstance(AWDfifo, AWSfifo);
                    appContext.registerReceiver(WindexBroadcastReceiver, SailTimerAPI.WindexBroadcastReceiverIntentFilter());
                } catch (Exception e) {
                    String msg = "Error initializing the SailTimer WindexBroadcastReceiver.";
                    msg += "\nMake sure the SailTimer API is running and connected to the Anemometer.";
                    Toast.makeText(appContext, msg, Toast.LENGTH_LONG);
                    Log.e(LOG_TAG, "Error registering the SailTimer API WindexBroadcastReceiver: " + e.getMessage());
                }
            }

            if (Activity_Main.testing) {
                // test data-set.  Used in windOscillations()
                testData = new double[] {
                        294.0, 288.5, 293.8, 295.8, 299.4, 298.5, 300.0, 301.8, 303.0,
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
                        281.2, 290.5};

                testDataSize = testData.length;
            }

        }
        /*
        Log.d(LOG_TAG, "AppBackgroundServices started with:");
        Log.d(LOG_TAG, "  writeToSQLite = :"+writeToSQLite);
        Log.d(LOG_TAG, "     WINDEX     = :"+WINDEX);
        Log.d(LOG_TAG, "     RaceID     = :"+para.getRaceID());
        Log.d(LOG_TAG, "     halfwayLAT = :"+para.getCenterLat()));
        Log.d(LOG_TAG, "     halfwayLON = :"+para.getCenterLon());
        Log.d(LOG_TAG, "runServices() after gps start gps="+gps.toString());
        */

        updateData.post(fetchDataContinuously);             // start the Runnable to fetch the data
        while (Activity_Main.runRaceBackgroundServices) {
            // this service runs continuously until the runRaceBackgroundServices boolean
            // is set to false in the initiating activity (in this case Activity_Main)
            // nothing to do in this loop.  It's only purpose is to keep this IntentService running
        }
        updateData.removeCallbacks(fetchDataContinuously);   // stop the Runnable to fetch the data

        try {
            appContext.unregisterReceiver(WindexBroadcastReceiver);
            if (gps != null) {
                gps.stopGPS();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error on 'unregisterReceiver(WindexBroadcastReceiver)': " + e.getMessage());
        }

        stopSelf();                                         // stop the IntentService
        return;
    }


    Runnable fetchDataContinuously = new Runnable() {
        public void run() {
            double AWD=0.0d, AWS=0.0d, AWA=0.0d, TWA=0.0d, TWD=0.0d, TWS=0.0d;
            double[] values;
            boolean goodWindData;
            String[] dots =new String[]{".", "..", "...", "...."};

            // create UTC timestamp in seconds to maintain compatibility with MYSQL DB
            Calendar now = Calendar.getInstance(Activity_Main.TZ_UTC);
            long datetime = now.getTimeInMillis()/1000L;
            //Log.d(LOG_TAG, "fDC() time ="+NavigationTools.getDateString(datetime*1000, "yy-MM-dd HH:mm:ss Z")+"  SQL="+(windCTR%WIND == 0 && writeToSQLite)+"  windex="+WINDEX+"  windCTR="+windCTR);

            gpsStatus = gps.getBestProvider()+dots[gps.CTR%4];
            if (WINDEX >= 1) {
                // fetch the values stored by the SailTimerAPI and compute the True wind values

                if (Activity_Main.testing) {
                    AWD = testData[windCTR%testDataSize]+(0.9+(Math.random()*(1.1d-0.9d)));
                    AWS = 8.0d + (Math.random() * (14.0d - 8.0d));
                } else {
                    if (para.getSailtimerStatus()) {
                        AWD = AWDfifo.averageCompassDirection();
                        AWS = AWSfifo.average();
                    } else {
                        AWD = 0.0d;
                        AWS = 0.0d;
                    }
                }
                AWA = NavigationTools.HeadingDelta(para.getAvgCOG(), AWD);

                values = NavigationTools.calc_TWA_TWD_TWS(para.getAvgSOG(), AWA, AWD, AWS);
                TWA = values[0];
                TWD = values[1];
                TWS = values[2];

                para.setAWA(AWA);
                para.setAWD(AWD);
                para.setAWS(AWS);

                para.setTWA(TWA);
                para.setTWD(TWD);
                para.setTWS(TWS);

                // the windCTR>4 && windCTR<8 removes the first 4 values which might be erroneous
                if (NavigationTools.TWD.size() >= NavigationTools.longAvg || (windCTR > 4 && windCTR < 9)) {
                    NavigationTools.TWD.removeFirst();
                    NavigationTools.TWD_short_AVG.removeFirst();
                    NavigationTools.TWD_long_AVG.removeFirst();
                    NavigationTools.TWD_Std_Dev.removeFirst();
                }

                goodWindData = NavigationTools.calc_AVG_StdDev(TWD);

                NavigationTools.TWD.add(TWD);
                NavigationTools.TWD_short_AVG.add(NavigationTools.TWD_shortAVG);
                NavigationTools.TWD_long_AVG.add(NavigationTools.TWD_longAVG);
                NavigationTools.TWD_Std_Dev.add(NavigationTools.TWD_StdDev);

                windCTR++;
                // write wind data to the SQLite database when we
                // encountered a 'WIND'th value and writeToSQLite & goodWindData are 'true'
                if (windCTR%WIND == 0 && writeToSQLite && goodWindData) {
                    // windCTR gets increased by 1 every time we loop thru this runnable.  windCTR
                    // is used to determine when we have a multiple of WIND readings so that we
                    // store that one in the SQLite DB table WIND

                    if( updateWindDB(AWD, AWS, TWD, TWS, para.getRaceID(), datetime) ){
                        // nothing to add here other than keeping the debug lines
                        //Log.d(LOG_TAG, "updateWindDB() - wrote record to sqlite DB with windCTR = "+windCTR);
                    } else {
                        // nothing to add here.  Just for debugging
                        //Log.d(LOG_TAG, "updateWindDB() - ERROR writing record to sqlite DB");
                    }
                }
            }
            // set the data update frequency
            updateData.postDelayed(this, (long) (dataUpdateFrequency * 1000));
        }
    };


    /**
     * Method to update the SQLite wind database with the average TWD and TWS over the past WIND number of readings.
     * In addition to the wind data, we'll also store our location on the course as defined by the 4 quadrants (e.g.
     * left Windward = quadrant 4, right Windward = quadrant 1, right Leeward = quadrant 2, and left Leeward = quadrant 3
     * WIND is a final int defined in the top section of this Class.
     *
     * @param AWD (Apparent Wind Direction - true north)
     * @param AWS (Apparent Wind Speed - kts)
     * @param TWD (True Wind Direction - true north)
     * @param TWS (True Wind Speed - kts)
     * @param raceID ID of this race.  If raceID = 0, return false
     * @param datetime long datetime variable for this record
     * @return true if successful, false if nothing was written to the DB (error or not the WIND number of readings)
     */
    public boolean updateWindDB(double AWD, double AWS, double TWD, double TWS, long raceID, long datetime) {
        int quadrant;
        SQLiteDatabase db;
        boolean status = true;

        // if we don't have a valid raceID, return false
        if (raceID < 0) {
            return false;
        }

        // if we don't have a valid sqlite db connection, return false
        sqlite_WindDataHelper dbhelper = new sqlite_WindDataHelper(appContext);
        db = dbhelper.getWritableDatabase();
        if (db==null) {
            return false;
        }

        // let's find out which Quadrant we're are currently sailing in by computing
        // the distance (q[0]) and heading (q[1]) from the race course center to the boat.
        double[] q = NavigationTools.MarkDistanceBearing(para.getCenterLat(), para.getCenterLon(), para.getBoatLat(), para.getBoatLon());

        if (q[1]>90.0d && q[1]<=180.0d) {
            quadrant = 2;
        } else if (q[1]>180.0d && q[1]<=270.0d) {
            quadrant = 3;
        } else if (q[1]>270.0d && q[1]<360.0d){
            quadrant = 4;
        } else {
            quadrant = 1;
        }

        // Create a new map of values using the sqlite DB column names as the keys
        ContentValues values = new ContentValues();
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_DATE, datetime);
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_AWD, AWD);
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_AWS, AWS);
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_TWD, TWD);
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_TWS, TWS);
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_SOG, para.getAvgSOG());
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_LAT, para.getBoatLat());
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_LON, para.getBoatLon());
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_QUADRANT, quadrant);
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_STATUS, 0);
        values.put(sqlite_WindDataHelper.WindEntry.COLUMN_RACE, raceID);

        try {
            long newRowId = db.insert(sqlite_WindDataHelper.WindEntry.TABLE_NAME, null, values);
            if (newRowId == -1) {
                status = false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "updateWindDB() time="+datetime+"  raceID="+raceID+" SQLite error= "+e.getMessage());
            status = false;
        }

        if (db!=null && db.isOpen())
            db.close();

        return status;
    }
}
