package com.kaiserware.sailingrace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import android.support.v7.view.ContextThemeWrapper;
import javax.net.ssl.HttpsURLConnection;

/**
 * Main Activity from which to launch the "SailingRacePreference", "Activity_StartSequence", or
 * "Activity_RaceInfo".
 *
 * Make sure to update the version number in the local build.gradle file
 *
 * All angles used in computations are assumed to be True North.  Angles displayed on screen are
 * measured in Magnetic North (True North + declination) except for TWA and AWA which are the difference
 * between the Boat heading and the Wind Direction!
 *
 * All speeds are measure in Nautical Miles per Hour (kts = nm/hr)
 *
 * Created by Volker Petersen - November 2015
 *
 * Github: https://github.com/volkerpetersen/SailingRace - implemented July 2018
 *
 */
public class Activity_Main extends Activity {
    public static volatile boolean runRaceBackgroundServices = false;
    public static final String LOG_TAG = Activity_Main.class.getSimpleName();
    public static final TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
    public static boolean completedRaceFlag;    // set when a race has been completed
    private Activity mainActivity;              // variable to store the Activity object for Activity_Main
    private Switch windexSwitch;                // Switch for the Anemometer status
    private Context appContext;                 // object holding the App Context
    private GlobalParameters para;              // Class object for the global parameters of this App
    private sqlite_WindDataHelper dbhelper;     // Class object for the sqlite DB helper class
    private AlertDialog alert;                  // variable to store an AlertDialog object
    private long today;                         // today's date
    private long seconds;                       // seconds
    private boolean sailtimerMenuActive = true; // SailTimer Connection MenuItem Active/Inactive
    private int WINDEX;                         // store the SailTimer Anemometer Settings Preference
    public static boolean testing=false;        // enable/disable test mode for app running in Virtual Device
    private SQLiteDatabase db = null;           // SQLite Database handlecd
    private Cursor cursor = null;               // SQLite Database cursor
    public int httpTimeout = 5000;	 		    // timeout in milliseconds for HTTP requests
    private Handler WindUpdate=new Handler();   // Handler to implement the Runnable for the Windex Data Updates
    private int httpRequestQueueCtr = 0;        // counts the number of active Async HTTP Requests
    private int sqliteRecordsToCloud = 0;       // number of records that have been send via HTTP Request to Cloud
    private int sqliteRecordsToUpdate;          // variable with # of Records to be stored in Cloud
    private int sqliteRacesToUpdate;            // variable with # of Race records to be stored in Cloud
    private TextView outputAWDlabel;            // output Textfield for the AWD label
    private TextView outputAWDvalue;            // output Textfield for the AWD value
    private TextView outputAWSlabel;            // output Textfield for the AWS label
    private TextView outputAWSvalue;            // output Textfield for the AWS value
    private ProgressDialog spinner;             // spinner Dialog utilized in the AsyncTask HTTP Request

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name_TM);
        appContext = getApplicationContext();
        mainActivity = this;
        spinner = new ProgressDialog(this);

        // initialize our Global Parameter singleton class
        para = GlobalParameters.getInstance();
        para.mainActivity = mainActivity;       // is used in AppBackgroundServices.java

        windexSwitch = (Switch) findViewById(R.id.windexSwitch);
        windexSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if (WINDEX >= 1 && !para.getSailtimerStatus()) {
                        connect_SailTimerAPI();
                    }
                }
            }
        });

        // to switch between BuildConfig.DEBUG = True / False click on the BuildVariants
        // on the left edge of Android Studio Window and select either FreeDebug or FreeRelease
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "APP Variant - Debug");
            Log.d(LOG_TAG, "Race Courses: ");
            for (Map.Entry<String, HashMap>entry : para.raceCourses.entrySet()) {
                String key = entry.getKey();
                HashMap wp = entry.getValue();
                Log.d(LOG_TAG, "Race '"+key+"'  contains "+wp.size()+" waypoints.");
            }
        } else {
            Log.i(LOG_TAG, "APP Variant - Production");
        }

        completedRaceFlag = false;

        // Update the SailTimerAPI hashCode and store it in the Global Parameters
        // Alternatively use "OfflineWebApp/SailTimerAPI_Password.php" to fetch the hash code
        //new getHashCodeOnline().execute("fetch hash code");

        para.setWindwardFlag(false);
        para.setWindwardRace(false);
        para.setWindwardLat(Double.NaN);
        para.setWindwardLon(Double.NaN);
        para.setLeewardFlag(false);
        para.setLeewardRace(false);
        para.setCommitteeLat(Double.NaN);
        para.setCommitteeLon(Double.NaN);
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "initial setting of 'WindwardLat': " + para.getWindwardLat());

        // code to hack the Menu Bar into the Action Bar for SDK19 devices
        // see answer 315 here: http://stackoverflow.com/questions/9286822/how-to-force-use-of-overflow-menu-on-devices-with-menu-button/11438245#11438245
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Force Menu Display error="+ex.getMessage());
        }
        // end of Menu Bar hack
        TextView outputVersionID = (TextView) findViewById(R.id.versionID);
        outputVersionID.setText(BuildConfig.VERSION_NAME);

        Button startSettingsActivity =  (Button) findViewById(R.id.button_settings);
        startSettingsActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppBackgroundServices.writeToSQLite = false;   // stop the SQLite DB updates
                startActivity(new Intent(appContext, SailingRacePreferences.class));
                if (BuildConfig.DEBUG)
                    Log.d(LOG_TAG, "XXX Button startSettingsActivity after startActivity() with runRaceBackgroundServices=" + runRaceBackgroundServices);
            }
        });

        // establish a test connection to the SQLite database to store wind data for this
        // App and make sure the DB is created if it doesn't exists
        sqlite_WindDataHelper dbhelper = new sqlite_WindDataHelper(appContext);
        db = dbhelper.getWritableDatabase();
        dbhelper.onCreate(db);
        dbhelper.closeDB(cursor, db);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Configuration config = getResources().getConfiguration();
        if (config.smallestScreenWidthDp >= 540) {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "Found Tablet Device with sw = "+config.smallestScreenWidthDp+"dp");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "Found a Smartphone with sw = "+config.smallestScreenWidthDp+"dp");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // launch the App Background Services to get the data from the Wind Anemometer
        if (!runRaceBackgroundServices) {
            runRaceBackgroundServices = true;
            launchAppBackgroundServices();  // launch the AppBackgroundServices IntentService
        }

        // establish a RaceID if we're not arriving here from Activity_RaceInfo() after completing a race
        if ( !completedRaceFlag ) {
            determineRaceID();
        } else {
            if (windexSwitch.isChecked() || para.getSailtimerStatus()) {
                record_race_summary(para.getRaceID(), false);
            }
        }

        // wait for 2.0 sec and then check if the SailTimer Anemometer is connected and up and running
        Handler SailTimerHandler = new Handler();
        SailTimerHandler.postDelayed(runnableSailTimer, 2000);

        // forces the operating system to call onPrepareOptionsMenu() to update the Menu Items
        invalidateOptionsMenu();
    }

    Runnable runnableSailTimer = new Runnable() {
        @Override
        public void run() {
            try {
                //Log.e(LOG_TAG, "runnableSailTimer executed");
                if (WINDEX >= 1) {
                    // Windex installed, as indicated by user thru preference settings
                    windexSwitch.setVisibility(View.VISIBLE);
                    if (para.getSailtimerStatus()) {
                        // Windex API is up and running
                        //windexSwitch.setChecked(true);
                        //windexSwitch.setEnabled(false);
                        //windexSwitch.setTypeface(Typeface.DEFAULT_BOLD);
                        windexSwitch.setVisibility(View.GONE);
                        sailtimerMenuActive = false;
                        outputAWDlabel = (TextView) findViewById(R.id.awdLabel);
                        outputAWDvalue = (TextView) findViewById(R.id.awdValue);
                        outputAWSlabel = (TextView) findViewById(R.id.awsLabel);
                        outputAWSvalue = (TextView) findViewById(R.id.awsValue);
                        outputAWDlabel.setText("AWD: ");
                        outputAWSlabel.setText("AWS: ");
                        WindUpdate.post(updateWindexData);
                    } else {
                        // Windex API is NOT running
                        windexSwitch.setChecked(false);
                        windexSwitch.setTypeface(Typeface.DEFAULT_BOLD);
                        sailtimerMenuActive = true;
                    }
                } else {
                    // NO Windex installed, as indicated by user thru preference settings
                    windexSwitch.setVisibility(View.GONE);
                    sailtimerMenuActive = false;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "runnableSailTimer() Caught Exception "+e.getMessage());
            }
        }
    };

    Runnable updateWindexData = new Runnable() {
        // display the current apparent wind data every second
        @Override
        public void run() {
            outputAWDvalue.setText(appContext.getString(R.string.Degrees, NavigationTools.TrueToMagnetic(para.getAWD(), para.getDeclination())));
            outputAWSvalue.setText(appContext.getString(R.string.DF1, para.getAWS()));

            WindUpdate.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        if (WINDEX >= 1) {
            // stop the WindUpdate Handler Runnable
            WindUpdate.removeCallbacks(updateWindexData);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (WINDEX >= 1) {
            // stop the WindUpdate Handler Runnable
            WindUpdate.removeCallbacks(updateWindexData);
        }

        // stop the runRaceBackgroundServices IntentService
        runRaceBackgroundServices = false;

        para.setWindwardFlag(false);
        para.setWindwardRace(false);
        para.setLeewardFlag(false);
        para.setLeewardRace(false);
        para.setWindwardLat(Double.NaN);
        para.setWindwardLon(Double.NaN);
        para.setCommitteeLat(Double.NaN);
        para.setCommitteeLon(Double.NaN);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "Back pressed");
            confirmQuit();
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        // enable / disable the Database reset Menu Option based on if the
        // Wind Instrument is enabled or not.

        WINDEX = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // boolean value to indicate if app is using / not using the Bluetooth connected Windex

        MenuItem resetDB = menu.findItem(R.id.sqlite_reset);
        MenuItem syncDB = menu.findItem(R.id.sqlite_sync);
        MenuItem sailtimerMenuItem = menu.findItem(R.id.connect_sailtimer_anemometer);
        if (WINDEX >= 1) {
            syncDB.setEnabled(true);
            resetDB.setEnabled(true);
        } else {
            syncDB.setEnabled(false);
            resetDB.setEnabled(false);
        }
        sailtimerMenuItem.setEnabled(sailtimerMenuActive);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        runRaceBackgroundServices=false;  // stop Service
        AppBackgroundServices.writeToSQLite=runRaceBackgroundServices;
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(appContext, SailingRacePreferences.class));
                break;
            case R.id.race_mode:
                startActivity(new Intent(appContext, Activity_RaceInfo.class));
                break;
            case R.id.timer_mode:
                startActivity(new Intent(appContext, Activity_StartSequence.class));
                break;
            case R.id.sqlite_sync:
                new sync_sqlite_DB_to_cloud().execute("toCloud");
                break;
            case R.id.sqlite_show_data:
                startActivity(new Intent(appContext, sqlite_show_wind_race_summary.class));
                break;
            case R.id.sqlite_reset:
                reset_sqlite_db();
                break;
            case R.id.connect_sailtimer_anemometer:
                connect_SailTimerAPI();
                break;
            case R.id.about:
                aboutSailingRace();
                break;
            case R.id.quit:
                confirmQuit();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Method to launch the external App "SailTimerAPI"
     * This method is either called from the Main Menu or on Switch Button Press
     */
    private void connect_SailTimerAPI() {
        String msg;
        String app_name = "SailTimerAPI\u2122";
        String title = app_name+" Connection";
        try {
            PackageManager manager = appContext.getPackageManager();
            // com.windinstrument.api.DeviceControlActivity
            Intent i = manager.getLaunchIntentForPackage("com.windinstrument.api");
            if (i == null) {
                msg = "Could not launch the "+app_name+" App. Please make sure this App is installed on your device";
                OK_dialog_message(title, msg);
                windexSwitch.setChecked(false);
            } else {
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                appContext.startActivity(i);
                windexSwitch.setChecked(true);
                windexSwitch.setEnabled(false);
            }
        } catch (Exception e) {
            msg = app_name+" App launch attempt failed with error:\n"+e.getMessage();
            OK_dialog_message(title, msg);
            windexSwitch.setChecked(false);
        }
    }

    /*
     *  Method launches the AppBackgroundServices IntentService which performs functions outside
     *  of our main UI.  Pass the time between data updates into this intent.  Value comes
     *  from the Preference Value "key_ScreenUpdates".
     *  Hence, we're updating our data at the same frequency as the screen update.
     */
    private void launchAppBackgroundServices() {
        runRaceBackgroundServices = true;
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "Launching the AppBackgroundServices");
        int dataUpdates = SailingRacePreferences.FetchPreferenceValue("key_ScreenUpdates", appContext);
        Intent mServiceIntent = new Intent(appContext, AppBackgroundServices.class);
        mServiceIntent.putExtra("dataUpdateFrequency", String.valueOf(dataUpdates));
        appContext.startService(mServiceIntent);
    }

    /**
     * Method to handle the Timer button to start the Timer activity.
     * Method is called via the onClick() definition in activity_main.xml
     *
     * @param view required by the button onClick() definition in "activity_main.xml"
     */
    public void start_timerActivity(View view) {
        startActivity(new Intent(appContext, Activity_StartSequence.class));
    }

    /**
     * Method to handle the Race button and start the race activity
     * Method is called via the onClick() definition in activity_main.xml
     *
     * @param view required by the button onClick() definition in "activity_main.xml"
     */
    public void start_raceActivity(View view) {
        startActivity(new Intent(appContext, Activity_RaceInfo.class));
    }

    /**
     * Method to handle the Settings button to allow user to customize the program parameters
     * Method is called via the onClick() definition in activity_main.xml
     *
     * @param view required by the button onClick() definition in "activity_main.xml"
     */
    public void start_settingsActivity(View view) {
        startActivity(new Intent(appContext, SailingRacePreferences.class));
    }

    /**
     * Method to create the RaceID for this race.  This method is called
     * from onResume() if neither the WindwardFlag nor the LeewardFlags are set
     */
    private void determineRaceID() {
        final String ds = "yyyy-MM-dd ss Z";

        // create a Race ID using today's date
        Cursor cursor = null;
        SQLiteDatabase db = null;
        Calendar now;
        now = Calendar.getInstance(TimeZone.getDefault());
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 10);
        today = now.getTimeInMillis()/1000L;

        // check if we've already recorded a race today in the sqlite db
        String sql = "SELECT * FROM "+ sqlite_WindDataHelper.WindEntry.TABLE_NAME;
        sql += " WHERE "+ sqlite_WindDataHelper.WindEntry.COLUMN_RACE+">='"+today+"'";
        sql += " GROUP BY "+ sqlite_WindDataHelper.WindEntry.COLUMN_RACE;
        //sql += " ORDER BY "+sqlite_WindDataHelper.WindEntry.COLUMN_RACE+" DESC";
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "determineRaceID() - millis= "+now.getTimeInMillis()+"  sec="+today);
            Log.d(LOG_TAG, "determineRaceID() - Race_ID initialization query: "+sql);
        }

        try {
            sqlite_WindDataHelper dbhelper = new sqlite_WindDataHelper(appContext);
            db = dbhelper.getWritableDatabase();
            cursor = db.rawQuery(sql, null);
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "determineRaceID() - query cursor.moveToFirst: "+cursor.moveToFirst());
            if (cursor.moveToFirst()) {
                List<CharSequence>  listItems = new ArrayList<>();
                do {
                    seconds = cursor.getLong(sqlite_WindDataHelper.COL_WIND_RACE);
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, "determineRaceID() - seconds ="+seconds);
                        Log.d(LOG_TAG, "determineRaceID() - date    ="+NavigationTools.getDateString(seconds*1000L, ds));
                    }
                    listItems.add(NavigationTools.getDateString(seconds*1000L, ds));
                } while (cursor.moveToNext());

                if (seconds >= today) {
                    listItems.add(NavigationTools.getDateString((seconds+1)*1000, ds));
                }

            } else {
                // the default value for the Race_ID is today's date at midnight.
                if (BuildConfig.DEBUG)
                    Log.d(LOG_TAG, "determineRaceID() - else choice ="+NavigationTools.getDateString(today*1000L, ds));
                para.setRaceID(today);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "determineRaceID() generated error "+e.getMessage());
        } finally {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "cursor="+(cursor!=null)+"  db="+(db!=null));
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
            //dbhelper.closeDB(cursor, db);
        }
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "determineRaceID() ="+para.getRaceID()+"  "+NavigationTools.getDateString(para.getRaceID()*1000L, ds));
    }

    /**
     * Method to save the race summary into the SQLite database (local and Cloud).
     * @param raceID - Long ID of the race for which we want to store a race summary
     * @param writeToCloud - if true, will write this data also to the Cloud MySQL DB
	 * @return true (success) or false (failed) to save the race summary
     */
    public boolean record_race_summary(Long raceID, boolean writeToCloud) {
        boolean status = true;
        double avgTWS=0.0d;
        double TWD;
        double avgTWD;
        double TWDx=0.0d;
        double TWDy=0.0d;
        int ctr = 0;
        Cursor cursorWind = null;
        SQLiteDatabase db = null;
        String name, msg;
        final JSONObject jsonValues = new JSONObject();

        // check if we have any wind records for this raceID.
        sqlite_WindDataHelper dbhelper = new sqlite_WindDataHelper(appContext);
        db = dbhelper.getWritableDatabase();

        // convert raceID into human readable date string
        name = NavigationTools.getDateString(raceID*1000L, "yyyy-MM-dd ss");

        if (db.isOpen()) {
            String sql = "SELECT * FROM " + sqlite_WindDataHelper.WindEntry.TABLE_NAME + " WHERE ";
            sql += sqlite_WindDataHelper.WindEntry.COLUMN_RACE + "='" + raceID + "'";
            cursorWind = db.rawQuery(sql, null);
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "record_race_summary(): Query for current Race:" + sql+"  Result:"+cursorWind.toString() + "bool: "+cursorWind.moveToFirst());

            if (cursorWind.moveToFirst()) {
                do {
                    avgTWS += (double) cursorWind.getFloat(sqlite_WindDataHelper.COL_WIND_TWS);
                    TWD = Math.toRadians((double) cursorWind.getInt(sqlite_WindDataHelper.COL_WIND_TWD));
                    TWDx += Math.cos(TWD);
                    TWDy += Math.sin(TWD);
                    ctr += 1;
                } while (cursorWind.moveToNext());
                avgTWS = avgTWS / (double) ctr;
                avgTWD = NavigationTools.convertXYCoordinateToPolar( (TWDx / (double) ctr), (TWDy / (double) ctr) );

                // fetch the geo data for this raceID from the SqLite DB and store in global parameters
                if (!fetchRaceLocations(raceID)) {
                    para.setWindwardLat(0.0d);
                    para.setWindwardLon(0.0d);
                    para.setCommitteeLat(0.0d);
                    para.setCommitteeLon(0.0d);
                    para.setCenterLat(0.0d);
                    para.setCenterLon(0.0d);
                }

                // Create a new map of values, where the DB column names are the keys
                ContentValues values = new ContentValues();
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_ID, raceID);
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_RECORDS, ctr);
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_AVG_TWD, avgTWD);
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_AVG_TWS, avgTWS);
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_WWD_LAT, para.getWindwardLat());
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_WWD_LON, para.getWindwardLon());
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_LWD_LAT, para.getCommitteeLat());
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_LWD_LON, para.getCommitteeLon());
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_CTR_LAT, para.getCenterLat());
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_CTR_LON, para.getCenterLon());
                values.put(sqlite_WindDataHelper.RaceEntry.COLUMN_NAME, name);

                // Insert/replace the SQLite row, returning the primary key value of the new row
                long newRowId = db.insertWithOnConflict(
                        sqlite_WindDataHelper.RaceEntry.TABLE_NAME,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE);

                if (BuildConfig.DEBUG)
                    Log.d(LOG_TAG, "db.insert() Race Table newRowID = "+newRowId+"  race name: "+name);

                if (writeToCloud) {
                    // let's also write this data to the Cloud database
					try {
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_ID, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_ID));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_AVG_TWD, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_AVG_TWD));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_AVG_TWS, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_AVG_TWS));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_WWD_LAT, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_WWD_LAT));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_WWD_LON, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_WWD_LON));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_LWD_LAT, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_LWD_LAT));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_LWD_LON, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_LWD_LON));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_CTR_LAT, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_CTR_LAT));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_CTR_LON, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_CTR_LON));
			            jsonValues.put(sqlite_WindDataHelper.RaceEntry.COLUMN_NAME, values.get(sqlite_WindDataHelper.RaceEntry.COLUMN_NAME));
					} catch (Exception e) {
			            Log.e(LOG_TAG, "record_race_summary() json error: "+e.getMessage());
			        }
					JSONObject jsonResponse = send_json_to_web(jsonValues, "raceJSON");
					if (BuildConfig.DEBUG)
					    Log.d(LOG_TAG, "send_json_to_web() return: "+jsonResponse.toString());
                }
            } else {
                status = false;
                msg = "No wind records found for "+name+".  Did not create a race summary.";
                if (BuildConfig.DEBUG)
                    Log.d(LOG_TAG, msg+" Query="+sql);
                OK_dialog_message(getString(R.string.title_sqlite_wind_data), msg);
            }
            dbhelper.closeDB(cursorWind, db);
        } else {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "record_race_summary() unable to open the SQLite DB");
            status = false;
        }
        return status;
    }

    /**
     * The Method 'fetch_all_races()' retrieves all records from the WIND SqLite DB which
     * have a COLUMN_STATUS=0 and stores the data in an ArrayList which will be returned.
     *
     * @return  ArrayList<String> with all the unique race_id
     */
    public ArrayList<String> fetch_all_races() {
        ArrayList<String> races = new ArrayList<String>();
        String query, raceName;
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            dbhelper = sqlite_WindDataHelper.getsInstance(appContext);
            db = dbhelper.getWritableDatabase();

            query = "SELECT * FROM " + sqlite_WindDataHelper.WindEntry.TABLE_NAME;
            query+= " WHERE "+ sqlite_WindDataHelper.WindEntry.COLUMN_STATUS + " ='0'";
            query+= " GROUP BY "+ sqlite_WindDataHelper.WindEntry.COLUMN_STATUS + ";";

            cursor = db.rawQuery(query, null);
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "fetch_all_races() query = " + query);
                Log.d(LOG_TAG, "fetch_all_races() race count: " + cursor.getCount());
            }
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        raceName = cursor.getString(sqlite_WindDataHelper.COL_WIND_RACE);
                        if (!races.contains(raceName)) {
                            races.add(raceName);
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "fetch_all_races() sqlite error " + e.getMessage());
        } finally {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "fetch_all_races() finally {} record count: " + cursor.getCount());
            dbhelper.closeDB(cursor, db);
        }
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "fetch_all_races() races: " + races.toString());

        return races;
    }

    /**
     * Method to synchronize all local SQLite Wind records with status=0 for one specified race
     * to the Cloud based MySQL DB
     * @param raceID - ID of the race for which we want to store the wind data records
     * @return int with number of records to update
     */
    public int record_sqlite_wind_to_web(final String raceID) {
        String query;
        int toUpdate = 0;
        int toUpdateErrors = 0;
        int ctr = 0;
        try {
            dbhelper = sqlite_WindDataHelper.getsInstance(appContext);
            db = dbhelper.getWritableDatabase();
            JSONObject jsonResponse;

            query = "SELECT * FROM "+ sqlite_WindDataHelper.WindEntry.TABLE_NAME+" WHERE ";
            query += sqlite_WindDataHelper.WindEntry.COLUMN_STATUS + " ='0' AND ";
            query += sqlite_WindDataHelper.WindEntry.COLUMN_RACE + "='"+raceID+"';";
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "record_sqlite_wind_to_web(): Race Wind data Query = "+query);

            cursor = db.rawQuery(query, null);
            cursor.moveToNext();
            toUpdate = cursor.getCount();
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Query: " + query);
                Log.d(LOG_TAG, "record_sqlite_wind_to_web() found "+toUpdate+" records to update for race "+raceID);
            }
            if (toUpdate == 0) {
                OK_dialog_message("Sync Wind DB", "Nothing to sync.  All records of the Wind database had been synced to the web.");
            } else {
                // need to sync the SQLite database to the web
                final JSONObject windData = new JSONObject();
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        do {
                            if (BuildConfig.DEBUG) {
                                //Log.d(LOG_TAG, "record_sqlite_wind_to_web: RaceID=" + cursor.getString(sqlite_WindDataHelper.COL_WIND_RACE) + "  AWD=" + cursor.getString(sqlite_WindDataHelper.COL_WIND_AWD) + "  AWS=" + cursor.getString(sqlite_WindDataHelper.COL_WIND_AWS) + "  TWD=" + cursor.getString(sqlite_WindDataHelper.COL_WIND_TWD) + "  TWS=" + cursor.getString(sqlite_WindDataHelper.COL_WIND_TWS));
                            }
							JSONObject windValues = new JSONObject();
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_AWD, cursor.getString(sqlite_WindDataHelper.COL_WIND_AWD));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_AWS, cursor.getString(sqlite_WindDataHelper.COL_WIND_AWS));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_TWD, cursor.getString(sqlite_WindDataHelper.COL_WIND_TWD));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_TWS, cursor.getString(sqlite_WindDataHelper.COL_WIND_TWS));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_SOG, cursor.getString(sqlite_WindDataHelper.COL_WIND_SOG));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_LAT, cursor.getString(sqlite_WindDataHelper.COL_WIND_LAT));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_LON, cursor.getString(sqlite_WindDataHelper.COL_WIND_LON));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_QUADRANT, cursor.getString(sqlite_WindDataHelper.COL_WIND_QUADRANT));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_STATUS, cursor.getString(sqlite_WindDataHelper.COL_WIND_STATUS));
                            windValues.put(sqlite_WindDataHelper.WindEntry.COLUMN_RACE, cursor.getString(sqlite_WindDataHelper.COL_WIND_RACE));
                            windData.put(cursor.getString(sqlite_WindDataHelper.COL_WIND_DATE), windValues);
                        } while (cursor.moveToNext());
                    } catch (JSONException e) {
                        errorMsgDialog("record_sqlite_wind_to_web(): error:" + e.getMessage());
                    }
					jsonResponse = send_json_to_web(windData, "windJSON");
                    if (BuildConfig.DEBUG)
                        Log.d(LOG_TAG, "record_sqlite_wind_to_web: status=" + jsonResponse.getString("status"));

                    if (jsonResponse.getString("status").equals("success")) {
                        // set all wind records to "updated to cloud"
						JSONArray jsonArray = jsonResponse.getJSONArray("post");
						ContentValues args = new ContentValues();
						args.put(sqlite_WindDataHelper.WindEntry.COLUMN_STATUS, "1");
						ctr = db.update(sqlite_WindDataHelper.WindEntry.TABLE_NAME,
								args,
								sqlite_WindDataHelper.WindEntry.COLUMN_RACE + "='" + raceID + "'",
								null);
						if (BuildConfig.DEBUG)
							Log.d(LOG_TAG, "record_sqlite_wind_to_web() set = " + ctr + " records to '1' in wind data set race: " + raceID);

						// now deal with possible error exceptions that were returned from php script
						toUpdateErrors = jsonArray.length();

                        if (BuildConfig.DEBUG)
    						Log.d(LOG_TAG, "record_sqlite_wind_to_web() set = " + toUpdateErrors + " records to '0' in wind data set race: " + raceID);

						for (int i = 0; i < toUpdateErrors; i++) {
							// Update the status field to "not updated" in the sqlite DB
							args.put(sqlite_WindDataHelper.WindEntry.COLUMN_STATUS, "0");
							ctr = db.update(sqlite_WindDataHelper.WindEntry.TABLE_NAME,
									args,
									sqlite_WindDataHelper.WindEntry.COLUMN_DATE + "='" + jsonArray.getString(i) + "'",
									null);
							if (BuildConfig.DEBUG)
								Log.d(LOG_TAG, "record_sqlite_wind_to_web() Updated = " + ctr + " record to '0' for for wind data set record: " + jsonArray.getString(i));
						}
                        sqliteRecordsToCloud += (toUpdate - toUpdateErrors); // variable holding cumulative successful update count
					} else {
						throw new JSONException("record_sqlite_wind_to_web(): HTTP request failed:  '" + jsonResponse.toString() + "'.");
					}
                } else {
					// no records in sqlite DB to be saved to web
				}
            //Log.d(LOG_TAG, "ASYNC urlConnection return value: "+sb.toString());
            return toUpdate;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "SQLite table 'wind' sync error: "+e.getMessage());
        } finally {
            dbhelper.closeDB(cursor, db);
        }
        return toUpdate;
    }


    /**
     * Async Task class to execute the cloud-data update.
     * @param - first parameter is a String Array passed into "doInBackground"
     * @param - second parameter is an Integer array passed into "onProgressUpdate"
     * @param - third parameter is a String passed into "onPostExecute"
     */
    public class sync_sqlite_DB_to_cloud extends AsyncTask<String, Integer, String> {
        public final String LOG_TAG = sync_sqlite_DB_to_cloud.class.getSimpleName();

        // Runs in UI before background thread is called
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // initialize Spinner Progress Dialog
            if (spinner != null) {
                spinner.setTitle("Wind Data to Cloud");
                spinner.setIcon(R.mipmap.ic_launcher);
                spinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                spinner.setMessage("Syncing Wind Data to Cloud...");
                spinner.show();
            }
        }

        // this is our Background thread
        @Override
        protected String doInBackground(String... msg1) {
            int index = 0;

            ArrayList<String> allRaces = fetch_all_races();

            sqliteRacesToUpdate = allRaces.size();  // global variable with # of races to be send to cloud
            sqliteRecordsToUpdate = 0;              // global variable with cumulative # of records to be send to cloud
            sqliteRecordsToCloud = 0;               // global variable with cumulative # of records sent to cloud
            if (spinner != null) {
                spinner.setMax(sqliteRacesToUpdate);
                publishProgress(index);
            }

            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "Activity_Main doInBackground(): fetch_all_races() record count="+sqliteRacesToUpdate);

            while (index < sqliteRacesToUpdate) {
                sqliteRecordsToUpdate += record_sqlite_wind_to_web(allRaces.get(index));
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "Activity_Main doInBackground(): working on race #"+(index+1)+" with ID="+allRaces.get(index));
                    Log.d(LOG_TAG, "Activity_Main doInBackground(): done with wind data, doing race data next");
                }
                record_race_summary(Long.parseLong(allRaces.get(index)), true);
                index++;
                publishProgress(index);
            }

            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "Activity_Main doInBackground(): while-loop complete with index="+index+" httpRequestQueueCtr="+httpRequestQueueCtr+" and sqliteRecordsToUpdate="+sqliteRecordsToUpdate);

            // Wait for the AsyncTask to complete execution of all HTTP Request Queues ('httpRequestQueueCtr')
            // added a 50 seconds (500 x 100 milliseconds) timeout safety measure into this
            // while loop to prevent a potential freeze-up of this program section
            index = 0;
            try {
                while (httpRequestQueueCtr > 0 && index < 500) {
                    // 500 x 100 = 50,000 milliseconds = 50 seconds
                    Thread.sleep(100);
                    index++;
                }
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Activity_Main doInBackground() error: interrupted execution = "+e.getMessage());
            }

            return "this string is passed to onPostExecute";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (spinner != null) {
                spinner.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Activity_Main onPostExecute() Done with Async Task: "+result);
                Log.d(LOG_TAG, "Activity_Main onPostExecute() sqliteRecordsToCloud: " +sqliteRecordsToCloud);
                Log.d(LOG_TAG, "Activity_Main onPostExecute() sqliteRecordsToUpdate: "+sqliteRecordsToUpdate);
            }
            if (spinner != null) {
                spinner.dismiss();
            }

            String msg;
            if (sqliteRecordsToCloud > 0) {
                msg = "Moved "+sqliteRecordsToCloud+" out of "+sqliteRecordsToUpdate;
                msg += " wind records to the Cloud.";
            } else if(sqliteRecordsToCloud==0 && sqliteRecordsToUpdate > 0) {
                msg = "All of the "+sqliteRecordsToUpdate +" records still need to be pushed to the Cloud.";
                msg += " The current attempt failed to write any wind records to the Cloud.";
            } else   {
                msg = "All wind data is already moved to the Cloud.";
            }
            OK_dialog_message("Sync Wind DB", msg);
        }

    }

    /**
     * Method to add a local SQLite records (race or wind) to the Cloud based MySQL DB
     * @param jsonValues (JSONObject) contains the json values compiled in
	 *                   "record_race_summary()" or "record_wind_data()"
	 * @param post (String) with the post keyword ("raceJSON" or "windJSON")
	 * @return JSONObject with the jsonResponse data returned by the HTTP request
     */
    public JSONObject send_json_to_web(JSONObject jsonValues, String post) {
        JSONObject jsonObject;
        JSONObject jsonResponse = new JSONObject();
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "ContentValues "+jsonValues.toString());
            Log.d(LOG_TAG, "Post          "+post);
        }

		// catch JSON Exceptions
        try {
            jsonResponse.put("status", "failure");
            jsonResponse.put("error", "send_json_to_web(): default error message");

            httpRequestQueueCtr++;

            // catch HttpsURLConnection Exceptions
            try {
				URL url = new URL(sqlite_WindDataHelper.DATABASE_URL);

				HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setReadTimeout(httpTimeout);        // milliseconds
                urlConnection.setConnectTimeout(httpTimeout);     // milliseconds
                urlConnection.setRequestMethod("POST");			  // designate a POST request
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                OutputStream out = urlConnection.getOutputStream();
                out.write((post+"="+jsonValues.toString()).getBytes("UTF-8"));
                out.close();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream())
                    );

                    String line = "";
                    StringBuffer result = new StringBuffer("");
                    while((line = in.readLine()) != null) {
                        result.append(line);
                    }
                    in.close();

					int start = result.indexOf("{\"status\"");
					if (start >= 0) {
                        if (BuildConfig.DEBUG)
					        Log.d(LOG_TAG, "send_json_to_web(): php return => "+result);
						jsonObject = new JSONObject(result.toString());
						jsonResponse = jsonObject.getJSONObject("results");
						if (!jsonResponse.getString("status").equals("success")) {
		                    jsonResponse.put("status", "failure");
							jsonResponse.put("error", "no 'status' variable set in php return json");
                        }
                    } else {
						jsonResponse.put("status", "failure");
						jsonResponse.put("error", "send_json_to_web(): HTTP response: "+responseCode);
                    }
                } else {
                    jsonResponse.put("status", "failure");
                    jsonResponse.put("error", "send_json_to_web(): HTTP response: "+responseCode);
                }
            } catch (Exception e) {
                // caught a HttpsURLConnection Exceptions
                jsonResponse.put("status", "failure");
                jsonResponse.put("error", "send_json_to_web(): urlConnection error: "+e.getMessage());
            }
            httpRequestQueueCtr--;
        } catch (JSONException j) {
            // caught a JSON Exceptions
            Log.d(LOG_TAG, "send_json_to_web(): json exception "+j.getMessage());
        }
        return jsonResponse;
    }

    /**
     * Method to reset the local SQLite DB tables
     */
    public void reset_sqlite_db() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this,android.R.style.Theme_Holo_Dialog));
        builder.setMessage("Do you really want to reset all Wind records on this device?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    dbhelper = sqlite_WindDataHelper.getsInstance(appContext);
                    db = dbhelper.getWritableDatabase();
                    dbhelper.onUpgrade(db, dbhelper.DATABASE_VERSION, dbhelper.DATABASE_VERSION);
                    OK_dialog_message("Sync Wind DB", "All Wind records on this device have been reset.");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Sqlite Reset error: "+e.getMessage());
                } finally {
                    dbhelper.closeDB(cursor, db);
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // quit this SQLite reset task since the user changed his mind
            }
        });
        builder.setTitle(R.string.sqlite_reset);
        builder.setIcon(R.mipmap.ic_launcher);
        alert=builder.create();
        alert.show();
    }

    /**
     * OK_dialog_message Method displays a messages and asks the user to
     * acknowledge this message before hiding the dialog box and returning to the UI
     */
    public void OK_dialog_message(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this, android.R.style.Theme_Holo_Dialog));
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Go ahead quit this dialog after the user has seen/acknowledge the msg
                return;
            }
        });
        builder.setTitle(title);
        builder.setIcon(R.mipmap.ic_launcher);
        alert=builder.create();
        alert.show();
    }

    /**
     * errorMsgDialog Method handles any error message that should be displayed to
     * the user.
     */
    private void errorMsgDialog(String prompt) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(
                this, android.R.style.Theme_Holo_Dialog));
        //AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Activity_Main.this);
        alertDialogBuilder.setMessage(prompt);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // user gave a positive affirmation
                return;
            }
        });
        alertDialogBuilder.setTitle("Warning");
        alertDialogBuilder.setIcon(R.mipmap.ic_launcher);
        AlertDialog alert=alertDialogBuilder.create();
        alert.show();
    }

    /**
     * "About" app dialog box
     */
    private void aboutSailingRace() {
        String temp;
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        String versionName = BuildConfig.VERSION_NAME;

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this,android.R.style.Theme_Holo_Dialog));
        temp = getString(R.string.AboutApp) + year + "\n\n" + versionName + "\n";
        builder.setMessage(temp);
        builder.setPositiveButton("Help", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Launch the Browser to review the App Documentation
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("http://southmetrochorale.org/OfflineWebApp"));
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog. Go ahead and continue the app
                onResume();
            }
        });
        builder.setTitle(R.string.app_name_TM);
        builder.setIcon(R.mipmap.ic_launcher);
        alert=builder.create();
        alert.show();
    }

    /**
     * confirmQuit Method handles the "Quit" confirmation so that the user doesn't quit this App
     * by accidentially hitting the back button.  It also allows the user to restart a new race.
     */
    private void confirmQuit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this,android.R.style.Theme_Holo_Dialog));
        builder.setMessage(R.string.QuitApp);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Go ahead quit this App
                finish();
            }
        });
        builder.setNeutralButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog. Go ahead and continue the app
                completedRaceFlag = true;
                onResume();
            }
        });
        builder.setNegativeButton("Start new Race", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Go ahead quit this App
                completedRaceFlag = false;
                onResume();
            }
        });
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.mipmap.ic_launcher);
        alert=builder.create();
        alert.show();
    }

    /**
     * fetchRaceLocations Method retrieves geo data from SqLite DB table for a specified race_id.
     * Store the results in the para global parameters.
     *
     * @param raceID Long
     * @return  boolean Success (true) or Error (false)
     */
     boolean fetchRaceLocations(Long raceID) {
        String query;
        Cursor cursor = null;
        SQLiteDatabase db = null;
        boolean status = false;

        try {
            dbhelper = sqlite_WindDataHelper.getsInstance(appContext);
            db = dbhelper.getWritableDatabase();

            query = "SELECT * FROM " + sqlite_WindDataHelper.LocationEntry.TABLE_NAME + " WHERE ";
            query += sqlite_WindDataHelper.LocationEntry.COLUMN_RACE + "='"+raceID+"';";

            cursor = db.rawQuery(query, null);
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "fetchRaceLocations() query = " + query);
                Log.d(LOG_TAG, "fetchRaceLocations() record count: " + cursor.getCount());
            }
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    para.setCommitteeLat(Double.parseDouble(cursor.getString(sqlite_WindDataHelper.COL_LOC_LWD_LAT)));
                    para.setCommitteeLon(Double.parseDouble(cursor.getString(sqlite_WindDataHelper.COL_LOC_LWD_LON)));
                    para.setWindwardLat(Double.parseDouble(cursor.getString(sqlite_WindDataHelper.COL_LOC_WWD_LAT)));
                    para.setWindwardLon(Double.parseDouble(cursor.getString(sqlite_WindDataHelper.COL_LOC_WWD_LON)));
                    para.setCenterLat(Double.parseDouble(cursor.getString(sqlite_WindDataHelper.COL_LOC_CTR_LAT)));
                    para.setCenterLon(Double.parseDouble(cursor.getString(sqlite_WindDataHelper.COL_LOC_CTR_LON)));
                    status = true;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "fetch_geoData() sqlite error " + e.getMessage());
        } finally {
            dbhelper.closeDB(cursor, db);
        }
        return status;
     }
}
