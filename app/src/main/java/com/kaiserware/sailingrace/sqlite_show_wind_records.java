package com.kaiserware.sailingrace;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Created by Volker Petersen on 9/9/2016.
 */
public class sqlite_show_wind_records extends Activity {
    private static final String LOG_TAG = sqlite_show_wind_records.class.getSimpleName();
    private Cursor cursor = null;
    private SQLiteDatabase db = null;
    private sqlite_WindDataHelper dbhelper = new sqlite_WindDataHelper(this);
    private long millis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long raceID=0;
        String sql, date;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_wind_data);

        // check if we have any wind records for this raceID.
        db = dbhelper.getWritableDatabase();

        if (db.isOpen()) {
            if (NavigationTools.raceID == 0L) {
                // get the last race in the SQLite DB if no raceID was set in NavigationTools.raceID
                // by the show_sqlite_race_data ListView
                //
                // see http://stackoverflow.com/questions/3359414/android-column-id-does-not-exist
                // answer 91:
                // the rowid _id, * selects all columns plus add a column _id containing the rowid
                // the field _id is required for the ListView class
                sql = "SELECT rowid _id,* FROM " + sqlite_WindDataHelper.RaceEntry.TABLE_NAME;
                sql += " ORDER BY " + sqlite_WindDataHelper.RaceEntry.COLUMN_ID + " DESC";
                cursor = db.rawQuery(sql, null);
                //Log.d(LOG_TAG, cursor.getCount()+" records found in query="+sql);
                if (cursor.moveToFirst()) {
                    raceID = (long) cursor.getLong(sqlite_WindDataHelper.COL_RACE_ID);
                }
            } else {
                raceID = NavigationTools.raceID;
            }

            if (raceID > 0L) {
                sql = "SELECT rowid _id,* FROM " + sqlite_WindDataHelper.WindEntry.TABLE_NAME;
                sql += " WHERE " + sqlite_WindDataHelper.WindEntry.COLUMN_RACE + "='" + raceID + "'";
                sql += " ORDER BY " + sqlite_WindDataHelper.WindEntry.COLUMN_DATE + " DESC";
                millis = (long) raceID * 1000;
                date = NavigationTools.getDateString(millis, "yyyy-MM-dd ss");
            } else {
                sql = "SELECT rowid _id,* FROM " + sqlite_WindDataHelper.WindEntry.TABLE_NAME;
                sql += " ORDER BY " + sqlite_WindDataHelper.WindEntry.COLUMN_DATE + " DESC";
                date = "most recent data";
            }
            setTitle(getString(R.string.title_list_wind_table)+" "+date);

            // fetch all wind records using the SQL query stored in sql
            cursor = db.rawQuery(sql, null);
            if (cursor!=null && cursor.getCount()>0) {
                cursor.moveToFirst();
                String[] fromColumns = new String[] {
                    sqlite_WindDataHelper.WindEntry.COLUMN_DATE,
                    sqlite_WindDataHelper.WindEntry.COLUMN_TWD,
                    sqlite_WindDataHelper.WindEntry.COLUMN_TWS,
                    sqlite_WindDataHelper.WindEntry.COLUMN_SOG,
                    sqlite_WindDataHelper.WindEntry.COLUMN_QUADRANT
                };
                int[] toViews = new int[] {R.id.LV_date, R.id.LV_twd, R.id.LV_tws, R.id.LV_sog, R.id.LV_quad};

                // create the adapter using the cursor pointing to the desired data
                // as well as the row layout information stored in row_wind.xml file
                SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                        R.layout.row_wind, cursor, fromColumns, toViews, 1);
                
                ListView WindListView = (ListView) findViewById(R.id.WindTableListView);
                if (WindListView == null) {
                    Toast toast = Toast.makeText(this, "Could not initialize the Simple Cursor Adapter", Toast.LENGTH_LONG);
                    toast.show();
                } else {

                    // attach a setViewBinder to format the Date data
                    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                        @Override
                        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                            TextView textView = (TextView) view;
                            // columnIndex-1 because we added one column _id to this query
                            if (columnIndex-1 == sqlite_WindDataHelper.COL_WIND_DATE) {
                                // fetch the current date in seconds from the SQLite DB and convert
                                // it back to milliseconds.  We stored it in seconds to ensure
                                // compatibility with the web-based MySQL DB.
                                // Finally covert it into a human readable string
                                millis = cursor.getLong(sqlite_WindDataHelper.COL_WIND_DATE)*1000L;
                                textView.setText(NavigationTools.getDateString(millis, "HH:mm:ss"));
                                return true;
                            }
                            if (columnIndex-1 == sqlite_WindDataHelper.COL_WIND_TWD) {
                                //Log.d(LOG_TAG, "TWD="+cursor.getDouble(sqlite_WindDataHelper.COL_WIND_TWD)+1);
                                textView.setText(getString(R.string.INT3, cursor.getDouble(sqlite_WindDataHelper.COL_WIND_TWD+1)));
                                return true;
                            }
                            if (columnIndex-1 == sqlite_WindDataHelper.COL_WIND_TWS) {
                                textView.setText(getString(R.string.DF1, cursor.getDouble(sqlite_WindDataHelper.COL_WIND_TWS+1)));
                                return true;
                            }
                            if (columnIndex-1 == sqlite_WindDataHelper.COL_WIND_SOG) {
                                textView.setText(getString(R.string.DF1, cursor.getDouble(sqlite_WindDataHelper.COL_WIND_SOG+1)));
                                return true;
                            }
                            return false;
                        }
                    });

                    WindListView.setAdapter(adapter);
                }
            } else {
                Toast toast = Toast.makeText(this, "No wind records found for Race ID "+raceID, Toast.LENGTH_LONG);
                toast.show();
            }
         } else {
            Toast toast = Toast.makeText(this, "Could not open the SQLite database", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbhelper.closeDB(cursor, db);
    }
}

