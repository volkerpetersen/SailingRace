package com.example.volkerpetersen.sailingrace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Main Activity from which to launch the "SailingRacePreference", "start_timerActivity", or
 * "start_raceActivity".
 *
 * Created by Volker Petersen - November 2015
 */
public class MainActivity extends Activity {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    public Context appContext;
    public GlobalParameters para;
    private SQLiteDatabase db = null;
    private sqlWindDataHelper dbhelper;
    private int toUpdate;
    private int windex;
    private int sqliteRecordsToCloud;
    private int sqliteRecordsToUpdate;
    private ProgressDialog spinner;
    private int volleyQueueCtr = 0;
    private AlertDialog alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name_TM);
        appContext = getApplicationContext();
        spinner = new ProgressDialog(MainActivity.this);

        // initialize our Global Parameter singleton class
        para = GlobalParameters.getInstance();

        // Update the SailTimerAPI hashCode and store it in the Global Parameters
        //SailTimerAPI.getHashCodeOnline(appContext, para);

        para.setWindwardFlag(false);
        para.setWindwardRace(false);
        para.setWindwardLat(Double.NaN);
        para.setWindwardLon(Double.NaN);
        para.setLeewardFlag(false);
        para.setLeewardRace(false);
        para.setLeewardLat(Double.NaN);
        para.setLeewardLon(Double.NaN);
        //Log.d(LOG_TAG, "initial setting of 'WindwardLat': "+para.getWindwardLat());

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
            Log.d(LOG_TAG, "Force Menu Display error="+ex.getMessage());
        }
        // end of Menu Bar hack

    }

    @Override
    protected void onResume() {
        super.onResume();

        Configuration config = getResources().getConfiguration();
        if (config.smallestScreenWidthDp >= 540) {
            //Log.d(LOG_TAG, "Found Tablet Device with sw = "+config.smallestScreenWidthDp+"dp");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            //Log.d(LOG_TAG, "Found a Smartphone with sw = "+config.smallestScreenWidthDp+"dp");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        windex = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // boolean value to indicate if app is using / not using the Bluetooth connected Windex
        //Log.d(LOG_TAG, "getRaceID = "+para.getRaceID());

        invalidateOptionsMenu();   // forces the operating system to call onPrepareOptionsMenu()
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
        para.setWindwardFlag(false);
        para.setWindwardRace(false);
        para.setLeewardFlag(false);
        para.setLeewardRace(false);
        para.setWindwardLat(Double.NaN);
        para.setWindwardLon(Double.NaN);
        para.setLeewardLat(Double.NaN);
        para.setLeewardLon(Double.NaN);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //Log.d(LOG_TAG, "Back pressed");
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
        MenuItem resetDB = menu.findItem(R.id.sqlite_reset);
        MenuItem syncDB = menu.findItem(R.id.sqlite_sync);
        if (windex == 1) {
            syncDB.setEnabled(true);
            resetDB.setEnabled(true);
        } else {
            syncDB.setEnabled(false);
            resetDB.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(appContext, SailingRacePreferences.class));
                return true;
            case R.id.race_mode:
                startActivity(new Intent(appContext, start_raceActivity.class));
                return true;
            case R.id.timer_mode:
                startActivity(new Intent(appContext, start_timerActivity.class));
                return true;
            case R.id.sqlite_sync:
                new sync_sqlite_DB_to_cloud().execute("toCould");
                return true;
            case R.id.sqlite_reset:
                reset_sqlite_db();
                return true;
            case R.id.about:
                aboutSailingRace();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method to handle the Timer button to start the Timer activity.
     * Method is called via the onClick() definition in activity_main.xml
     *
     * @param view required by the button onClick() definition in "activity_main.xml"
     */
    public void start_timerActivity(View view) {
        startActivity(new Intent(appContext, start_timerActivity.class));
    }

    /**
     * Method to handle the Race button and start the race activity
     * Method is called via the onClick() definition in activity_main.xml
     *
     * @param view required by the button onClick() definition in "activity_main.xml"
     */
    public void start_raceActivity(View view) {
        startActivity(new Intent(appContext, start_raceActivity.class));
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
     * Method to safe the race summary into the SQLite database (local and Cloud).
     * @param raceID - ID of the race for which we want to store a race summary
     */
    public boolean record_race_summary(String raceID) {
        double avgTWS=0.0d;
        double TWD;
        double avgTWD;
        double TWDx=0.0d;
        double TWDy=0.0d;
        int ctr = 0;
        Cursor cursorWind = null;

        // check if we have any wind records for this raceID.
        sqlWindDataHelper dbhelper = new sqlWindDataHelper(appContext);
        SQLiteDatabase db = dbhelper.getWritableDatabase();
        dbhelper.onCreate(db);

        String sql = "SELECT * FROM "+sqlWindDataHelper.WindEntry.TABLE_NAME+" WHERE ";
        sql += sqlWindDataHelper.WindEntry.COLUMN_RACE+"='"+raceID+"'";
        cursorWind = db.rawQuery(sql, null);
        Log.d(LOG_TAG, "record_race_summary()-Query for current Race:" + sql+"  Result:"+cursorWind.toString() + "bool: "+!cursorWind.moveToFirst());

        if (cursorWind.moveToFirst()) {
            do {
                avgTWS += (double)cursorWind.getFloat(sqlWindDataHelper.COL_WIND_TWS);
                TWD = Math.toRadians( (double)cursorWind.getInt(sqlWindDataHelper.COL_WIND_TWD) );
                TWDx += Math.sin(TWD);
                TWDy += Math.cos(TWD);
                ctr += 1;
            } while(cursorWind.moveToNext());
            avgTWS = avgTWS / (double)ctr;
            avgTWD = NavigationTools.convertXYCoordinateToPolar( TWDx/(double)ctr, TWDy/(double)ctr );

            // convert raceID into human readable date string
            String name = NavigationTools.getDate( (Long.parseLong(raceID)*(long)1000), "yyyy-MM-dd ss" );

            // Create a new map of values, where the DB column names are the keys
            ContentValues values = new ContentValues();
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_ID, raceID);
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_AVG_TWD, avgTWD);
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_AVG_TWS, avgTWS);
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_WWD_LAT, para.getWindwardLat());
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_WWD_LON, para.getWindwardLon());
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_LWD_LAT, para.getLeewardLat());
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_LWD_LON, para.getLeewardLon());
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_CTR_LAT, para.getCenterLat());
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_CTR_LON, para.getCenterLon());
            values.put(sqlWindDataHelper.RaceEntry.COLUMN_NAME, name);

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(sqlWindDataHelper.RaceEntry.TABLE_NAME, null, values);
            Log.d(LOG_TAG, "Race Table newRowID = "+newRowId+"  race name: "+name);

            // let's also write this data to the Could database
            sync_sqlite_race_to_web(values);
        } else {
            Log.d(LOG_TAG, "No wind records found.  Didn't create a race summary.  Query="+sql);
        }

        if (cursorWind != null) {
            cursorWind.close();
        }
        return false;
    }

    /**
     * Method to synchronize all SQLite windFIFO records to the Cloud based MySQL DB
     */
    public void sync_sqlite_windFIFO() {
        Cursor cursor;
        String query;
        try {
            dbhelper = sqlWindDataHelper.getsInstance(appContext);
            db = dbhelper.getWritableDatabase();
            dbhelper.onCreate(db);

            query = "SELECT * FROM " + sqlWindDataHelper.windFIFO.TABLE_NAME + ";";
            Log.d(LOG_TAG, "windFIFO data Query = " + query);

            cursor = db.rawQuery(query, null);
            cursor.moveToNext();
            toUpdate = cursor.getCount();
            //Log.d(LOG_TAG, "Query: " + query);
            //Log.d(LOG_TAG, "Count: "+cursor.getCount());
            if (toUpdate == 0) {
                sqliteMessages("Nothing to sync.  All records of the Wind database had been synced to the web.");
            } else {
                Log.d(LOG_TAG, "sync_sqlite_windFIFO() found " + toUpdate + " records to update.");

                // need to sync the SQLite database to the web
                final JSONObject windData = new JSONObject();
                if (cursor != null && cursor.moveToFirst()) {
                    int ctr = 0;
                    do {
                        // convert date from milliseconds to seconds
                        JSONObject windValues = new JSONObject();
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_ID, cursor.getString(sqlWindDataHelper.COL_windFIFO_ID));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_DATE, cursor.getString(sqlWindDataHelper.COL_windFIFO_DATE));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_TWD, cursor.getString(sqlWindDataHelper.COL_windFIFO_TWD));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_smoothedTWD, cursor.getString(sqlWindDataHelper.COL_windFIFO_smoothedTWD));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_avgSmoothedTWD, cursor.getString(sqlWindDataHelper.COL_windFIFO_avgSmoothedTWD));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_amplitude, cursor.getString(sqlWindDataHelper.COL_windFIFO_AMPLITUDE));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_frequency, cursor.getString(sqlWindDataHelper.COL_windFIFO_FREQUENCY));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_COG, cursor.getString(sqlWindDataHelper.COL_windFIFO_COG));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_SOG, cursor.getString(sqlWindDataHelper.COL_windFIFO_SOG));
                        windValues.put(sqlWindDataHelper.windFIFO.COLUMN_tack, cursor.getString(sqlWindDataHelper.COL_windFIFO_TACK));
                        ctr += 1;
                    } while (cursor.moveToNext());

                    // Volley request to write to the web cloud
                }
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "SQLite table 'windFIFO' sync error: "+e.getMessage());
        }
    }
    /**
     * Method to synchronize all local SQLite Wind records with status=0 for one specified race
     * to the Cloud based MySQL DB
     * @param raceID - ID of the race for which we want to store the wind data records
     */
    public void sync_sqlite_wind_to_web(String raceID) {
        Cursor cursor;
        String query;
        try {
            dbhelper = sqlWindDataHelper.getsInstance(appContext);
            db = dbhelper.getWritableDatabase();
            dbhelper.onCreate(db);

            query = "SELECT * FROM "+sqlWindDataHelper.WindEntry.TABLE_NAME+" WHERE ";
            query += sqlWindDataHelper.WindEntry.COLUMN_STATUS + "='0' AND ";
            query += sqlWindDataHelper.WindEntry.COLUMN_RACE + "='"+raceID+"';";
            Log.d(LOG_TAG, "Race Wind data Query = "+query);

            cursor = db.rawQuery(query, null);
            cursor.moveToNext();
            toUpdate = cursor.getCount();
            //Log.d(LOG_TAG, "Query: " + query);
            //Log.d(LOG_TAG, "Count: "+cursor.getCount());
            if (toUpdate == 0) {
                sqliteMessages("Nothing to sync.  All records of the Wind database had been synced to the web.");
            } else {
                Log.d(LOG_TAG, "sync_sqlite_wind_to_web() found "+toUpdate+" records to update for race "+raceID);

                // need to sync the SQLite database to the web
                final JSONObject windData = new JSONObject();
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        // convert date from milliseconds to seconds
                        JSONObject windValues = new JSONObject();
                        windValues.put(sqlWindDataHelper.WindEntry.COLUMN_TWD, cursor.getString(sqlWindDataHelper.COL_WIND_TWD));
                        windValues.put(sqlWindDataHelper.WindEntry.COLUMN_TWS, cursor.getString(sqlWindDataHelper.COL_WIND_TWS));
                        windValues.put(sqlWindDataHelper.WindEntry.COLUMN_QUADRANT, cursor.getString(sqlWindDataHelper.COL_WIND_QUADRANT));
                        windValues.put(sqlWindDataHelper.WindEntry.COLUMN_STATUS, cursor.getString(sqlWindDataHelper.COL_WIND_STATUS));
                        windValues.put(sqlWindDataHelper.WindEntry.COLUMN_RACE, cursor.getString(sqlWindDataHelper.COL_WIND_RACE));
                        windData.put(cursor.getString(sqlWindDataHelper.COL_WIND_DATE), windValues);
                    } while (cursor.moveToNext());
                }

                // Set up the Volley network library to make asynchronous httpRequests to send the
                // JSON data with a POST request to the web server php script running on the
                // South Metro Chorale website (sqlWindDataHelper.DATABASE_URL).
                //Log.d(LOG_TAG, "sync_sqlite_wind_to_web() json="+windData.toString());
                StringRequest postRequest = new StringRequest(Request.Method.POST, sqlWindDataHelper.DATABASE_URL,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Cursor cursor=null;
                                int ctr;
                                volleyQueueCtr--;
                                try {
                                    //Log.d(LOG_TAG, "http response: "+response);
                                    JSONObject jsonObject1 = new JSONObject(response);
                                    JSONObject jsonObject2 = jsonObject1.getJSONObject("results");
                                    JSONArray jsonArray = jsonObject2.getJSONArray("post");
                                    if (jsonObject2.getString("status").equals("success")) {
                                        //Log.d(LOG_TAG,"sync_sqlite_wind_to_web() post: "+jsonArray.toString());
                                        dbhelper = sqlWindDataHelper.getsInstance(appContext);
                                        db = dbhelper.getWritableDatabase();
                                        dbhelper.onCreate(db);

                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            // Update the status field in the sqlite DB

                                            // is working, converted to db.update() below
                                            //String sql = "UPDATE "+sqlWindDataHelper.WindEntry.TABLE_NAME;
                                            //sql += " SET "+sqlWindDataHelper.WindEntry.COLUMN_STATUS+"='1' WHERE ";
                                            //sql += sqlWindDataHelper.WindEntry.COLUMN_DATE+"='"+jsonArray.getString(i)+"';";
                                            //cursor = db.rawQuery(sql, null);
                                            ContentValues args = new ContentValues();
                                            args.put(sqlWindDataHelper.WindEntry.COLUMN_STATUS, "1");
                                            ctr = db.update(sqlWindDataHelper.WindEntry.TABLE_NAME, 
                                                    args,
                                                    sqlWindDataHelper.WindEntry.COLUMN_DATE+"='"+jsonArray.getString(i)+"'",
                                                    null);
                                            if (ctr > 0) {
                                                sqliteRecordsToCloud += ctr;
                                            }
                                            Log.d(LOG_TAG, "Updated = "+ctr+" record for for wind data set record: "+jsonArray.getString(i));

                                        }
                                        if (cursor != null) {
                                            cursor.close();
                                        }

                                    } else {
                                        Log.d(LOG_TAG,"sync_sqlite_wind_to_web() http error: "+jsonArray.toString());
                                    }
                                } catch (JSONException e) {
                                    Log.d(LOG_TAG, "sync_sqlite_wind_to_web() http error (catch):"+e.getMessage());
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                volleyQueueCtr--;
                                Log.d(LOG_TAG, "sync_sqlite_wind_to_web() Volley http error: " + error.getMessage());
                            }
                        }
                ) {
                    @Override
                    protected Map<String, String> getParams() {
                        // create the httpRequest POST parameters:
                        Map<String, String> params = new HashMap<>();
                        params.put("windJSON", windData.toString());
                        return params;
                    }
                };
                Volley.newRequestQueue(this).add(postRequest);
                volleyQueueCtr++;
            }

            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "SQLite table 'wind' sync error: "+e.getMessage());
        }
    }

    /**
     * Method to add a local SQLite Race record to the Cloud based MySQL DB
     * @param race (ContentValues) contains the race parameters computed in "record_race_summary()
     */
    public void sync_sqlite_race_to_web(ContentValues race) {
        Log.d(LOG_TAG, "ContentValues "+race.toString());
        Log.d(LOG_TAG, "Race ID "+race.get(sqlWindDataHelper.RaceEntry.COLUMN_ID));
        try {
            final JSONObject raceValues = new JSONObject();
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_ID, race.get(sqlWindDataHelper.RaceEntry.COLUMN_ID));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_AVG_TWD, race.get(sqlWindDataHelper.RaceEntry.COLUMN_AVG_TWD));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_AVG_TWS, race.get(sqlWindDataHelper.RaceEntry.COLUMN_AVG_TWS));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_WWD_LAT, race.get(sqlWindDataHelper.RaceEntry.COLUMN_WWD_LAT));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_WWD_LON, race.get(sqlWindDataHelper.RaceEntry.COLUMN_WWD_LON));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_LWD_LAT, race.get(sqlWindDataHelper.RaceEntry.COLUMN_LWD_LAT));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_LWD_LON, race.get(sqlWindDataHelper.RaceEntry.COLUMN_LWD_LON));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_CTR_LAT, race.get(sqlWindDataHelper.RaceEntry.COLUMN_CTR_LAT));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_CTR_LON, race.get(sqlWindDataHelper.RaceEntry.COLUMN_CTR_LON));
            raceValues.put(sqlWindDataHelper.RaceEntry.COLUMN_NAME, race.get(sqlWindDataHelper.RaceEntry.COLUMN_NAME));

            Log.d(LOG_TAG, "sync_sqlite_race_to_web() json="+raceValues.toString());
            // Set up the asynchronous HTTP client to send the JSON data with a POST request
            // to the web server php script running on the South Metro Chorale website.
            StringRequest postRequest = new StringRequest(Request.Method.POST, sqlWindDataHelper.DATABASE_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                volleyQueueCtr--;
                                Log.d(LOG_TAG, "http response: "+response);
                                JSONObject jsonObject = new JSONObject(response);
                                if (jsonObject.getString("status").equals("success")) {
                                    // TODO nothing to implement here
                                } else {
                                    Log.d(LOG_TAG,"sync_sqlite_race_to_web() http error: "+jsonObject.getString("post"));
                                }
                            } catch (JSONException e) {
                                Log.d(LOG_TAG, "sync_sqlite_race_to_web() http error (catch):"+e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            volleyQueueCtr--;
                            Log.d(LOG_TAG, "sync_sqlite_race_to_web() Volley http error: "+error.getMessage());
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    // create the httpRequest POST parameters:
                    Map<String, String> params = new HashMap<>();
                    params.put("raceJSON", raceValues.toString());
                    return params;
                }
            };
            Volley.newRequestQueue(this).add(postRequest);
            volleyQueueCtr++;
        } catch (Exception e) {
            Log.d(LOG_TAG, "SQLite table 'races' sync error: "+e.getMessage());
        }
    }

    /**
     * Method to add a the local wind oscillation FIFO queue to the Cloud based MySQL DB
     */
    public void sync_sqlite_wind_FIFO() {
        try {
            dbhelper = sqlWindDataHelper.getsInstance(appContext);
            db = dbhelper.getWritableDatabase();
            dbhelper.onCreate(db);

            String query = "SELECT * FROM "+sqlWindDataHelper.windFIFO.TABLE_NAME;

            Cursor cursor = db.rawQuery(query, null);
            cursor.moveToNext();
            toUpdate = cursor.getCount();
            final JSONObject windFIFOqueue = new JSONObject();
            if (toUpdate == 0) {
                sqliteMessages("Nothing to sync.  All records of the windFIFO database table have been synced to the web.");
            } else {
                Log.d(LOG_TAG, "sync_sqlite_wind_FIFO() found "+toUpdate+" records in FIFO queue to update");

                if (cursor != null && cursor.moveToFirst()) {
                    JSONObject windFIFOrecord = new JSONObject();
                    do {
                        //windFIFOrecord.put(sqlWindDataHelper.windFIFO.COLUMN_DATE, cursor.getString(sqlWindDataHelper.COL_windFIFO_DATE));
                        windFIFOrecord.put(sqlWindDataHelper.windFIFO.COLUMN_TWD, cursor.getString(sqlWindDataHelper.COL_windFIFO_TWD));
                        windFIFOrecord.put(sqlWindDataHelper.windFIFO.COLUMN_smoothedTWD, cursor.getString(sqlWindDataHelper.COL_windFIFO_smoothedTWD));
                        windFIFOrecord.put(sqlWindDataHelper.windFIFO.COLUMN_avgSmoothedTWD, cursor.getString(sqlWindDataHelper.COL_windFIFO_avgSmoothedTWD));
                        windFIFOrecord.put(sqlWindDataHelper.windFIFO.COLUMN_COG, cursor.getString(sqlWindDataHelper.COL_windFIFO_COG));
                        windFIFOrecord.put(sqlWindDataHelper.windFIFO.COLUMN_SOG, cursor.getString(sqlWindDataHelper.COL_windFIFO_SOG));
                        windFIFOqueue.put(cursor.getString(sqlWindDataHelper.COL_windFIFO_DATE), windFIFOrecord);
                    } while (cursor.moveToNext());
                }
            }

            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
            // Set up the asynchronous HTTP client to send the JSON data with a POST request
            // to the web server php script running on the South Metro Chorale website (DATABASE_URL).
            StringRequest postRequest = new StringRequest(Request.Method.POST, sqlWindDataHelper.DATABASE_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                volleyQueueCtr--;
                                Log.d(LOG_TAG, "http response: "+response);
                                JSONObject jsonObject = new JSONObject(response);
                                //Log.d(LOG_TAG, "jsonObject result="+jsonObject.getString("results"));
                                //Log.d(LOG_TAG, "jsonObject status="+jsonObject.getJSONObject("results").getString("status"));
                                if (jsonObject.getJSONObject("results").getString("status").equals("success")) {
                                    // TODO nothing to implement here
                                } else {
                                    Log.d(LOG_TAG,"sync_sqlite_wind_FIFO() http error: "+jsonObject.getString("post"));
                                }
                            } catch (JSONException e) {
                                Log.d(LOG_TAG, "sync_sqlite_wind_FIFO() http error (catch):"+e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            volleyQueueCtr--;
                            Log.d(LOG_TAG, "sync_sqlite_wind_FIFO() Volley http error: "+error.getMessage());
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    // create the httpRequest POST parameters:
                    Map<String, String> params = new HashMap<>();
                    params.put("fifoJSON", windFIFOqueue.toString());
                    //Log.d(LOG_TAG,"windFIFO params="+params.toString());
                    return params;
                }
            };
            Volley.newRequestQueue(this).add(postRequest);
            volleyQueueCtr++;
        } catch (Exception e) {
            Log.d(LOG_TAG, "SQLite table 'wind_FIFO' sync error: "+e.getMessage());
        }
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
                try {
                    sqlWindDataHelper dbhelper = sqlWindDataHelper.getsInstance(appContext);
                    SQLiteDatabase db = dbhelper.getWritableDatabase();
                    db.execSQL(sqlWindDataHelper.SQL_DELETE_WIND);
                    db.execSQL(sqlWindDataHelper.SQL_DELETE_RACE);
                    db.execSQL(sqlWindDataHelper.SQL_DELETE_windFIFO);
                    sqliteMessages("All Wind records on this device have been reset.");
                } catch (Exception e) {
                    Log.d(LOG_TAG, "Sqlite Reset error: "+e.getMessage());
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
     * sqliteMessages Method displays the messages from the SqLite DB updates and asks
     * the user to acknowledge the message before hiding the dialog box.
     */
    public void sqliteMessages(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this, android.R.style.Theme_Holo_Dialog));
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Go ahead quit this dialog after the user has seen/acknowledge the msg
                // TODO - done!
            }
        });
        builder.setTitle(R.string.sqlite_sync);
        builder.setIcon(R.mipmap.ic_launcher);
        alert=builder.create();
        alert.show();
    }

    /**
     * "About" app dialog box
     */
    private void aboutSailingRace() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this,android.R.style.Theme_Holo_Dialog));
        builder.setMessage(R.string.AboutApp);
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
     * confirmQuit Method handles the "Quit" confirmation so that the user doesn't quit the race
     * activity by accidentially hitting the back button
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

    ArrayList<String> fetch_all_races() {
        ArrayList<String> races = new ArrayList<String>();
        String query, raceName;
        Cursor cursor;
        try {
            sqliteRecordsToUpdate = 0;
            dbhelper = sqlWindDataHelper.getsInstance(appContext);
            db = dbhelper.getWritableDatabase();
            dbhelper.onCreate(db);

            query = "SELECT * FROM " + sqlWindDataHelper.WindEntry.TABLE_NAME + " WHERE ";
            query += sqlWindDataHelper.WindEntry.COLUMN_STATUS + "='0';";

            cursor = db.rawQuery(query, null);
            toUpdate = cursor.getCount();
            //Log.d(LOG_TAG, "fetch_all_races() query = " + query);
            //Log.d(LOG_TAG, "fetch_all_races() record count: " + cursor.getCount());
            if (toUpdate > 0) {
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            sqliteRecordsToUpdate++;
                            raceName = cursor.getString(sqlWindDataHelper.COL_WIND_RACE);
                            if (!races.contains(raceName)) {
                                races.add(raceName);
                            }
                        } while (cursor.moveToNext());
                    }
                } finally {
                    try { cursor.close(); } catch (Exception ignore) {}
                }
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "fetch_all_races() sqlite error " + e.getMessage());
        }
        return races;
    }

    /**
     * Async Task class to execute the cloud-data update.
     * @param - first parameter is a String Array passed into "doInBackground"
     * @param - second parameter is an Integer array passed into "onProgressUpdate"
     * @param - third parameter is a String passed into "onPostExecute"
     */
    private class sync_sqlite_DB_to_cloud extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... msg1) {
            String raceID;
            ArrayList<String> allRaces = fetch_all_races();
            int i = 0;
            int records = 0;
            sqliteRecordsToCloud = 0;
            spinner.setMax(allRaces.size());
            while (allRaces.size() > i) {
                raceID = allRaces.get(i);
                Log.d(LOG_TAG, "working on race #"+(i+1)+" with ID="+raceID);
                sync_sqlite_wind_to_web(raceID);
                Log.d(LOG_TAG, "done with wind data, doing race data next");
                record_race_summary(raceID);
                i++;
            }
            sync_sqlite_wind_FIFO();

            // Wait for Volley to complete execution of all Volley Request Queues ('volleyQueueCtr')
            // added a 30 seconds (300 x 100 milliseconds) timeout safety measure into this
            // while loop to prevent a potential freeze-up of this program section
            i = 0;
            try {
                while (volleyQueueCtr > 0 && i < 300) {
                    // 300 x 100 = 30,000 milliseconds = 30 seconds
                    Thread.sleep(100);
                    i++;
                }
            } catch (InterruptedException e) {
                Log.d(LOG_TAG, "doInBackground() error: interrupted execution = "+e.getMessage());
            }

            return "this string is passed to onPostExecute";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // lock the screen orientation to prevent this AsyncTask to crash upon screen rotation
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation((ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            }

            // initialize Spinner Progress Bar
            spinner.setTitle("Wind Data to Cloud");
            spinner.setIcon(R.mipmap.ic_launcher);
            spinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            spinner.setMessage("Syncing Wind Data to Cloud...");
            spinner.show();
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
            Log.d(LOG_TAG, "Done with Async Task "+result);
            spinner.dismiss();

            String msg;
            if (sqliteRecordsToCloud > 0) {
                msg = "Moved "+sqliteRecordsToCloud+" out of "+sqliteRecordsToUpdate;
                msg += " wind records to the Cloud.";
            } else if(sqliteRecordsToCloud==0 && sqliteRecordsToUpdate > 0) {
                msg = sqliteRecordsToUpdate +" still need to be stored in the Cloud.";
                msg += " The current attempt failed to write any wind records to the Cloud.";
            } else   {
                    msg = "All wind data is already moved to the Cloud.";
            }
            sqliteMessages(msg);

            // unlock the screen orientation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

    }
}

