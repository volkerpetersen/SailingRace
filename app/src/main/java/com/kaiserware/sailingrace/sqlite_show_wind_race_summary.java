package com.kaiserware.sailingrace;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Created by Volker Petersen on 9/23/2016.
 */

public class sqlite_show_wind_race_summary extends Activity {
    private static final String LOG_TAG = sqlite_show_wind_race_summary.class.getSimpleName();
    private Cursor cursor = null;
    private SQLiteDatabase db = null;
    private TextView numberOfRaces;
    private Button numberOfRecords;
    private sqlite_WindDataHelper dbhelper = new sqlite_WindDataHelper(this);
    private Context appContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_sqlite_wind_data);
        numberOfRaces = (TextView) findViewById(R.id.NumberOfRaces);
        numberOfRecords = (Button) findViewById(R.id.NumberOfRecords);
        appContext = getApplicationContext();

        numberOfRecords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch_to_ListWindTable(view);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String query;
        int ctr;
        NavigationTools.raceID = 0L;

        // check if we have any wind records for this raceID.
        db = dbhelper.getWritableDatabase();
        if (db.isOpen()) {
            // fetch the number of Wind records in the SQLIte DB
            query = "SELECT rowid _id,* FROM " + sqlite_WindDataHelper.WindEntry.TABLE_NAME;
            cursor = db.rawQuery(query, null);
            if (cursor != null) {
                ctr = cursor.getCount();
                cursor.close();
            } else {
                ctr = 0;
            }
            numberOfRecords.setText(String.valueOf(ctr));

            // fetch all the records from Races table in the SQLite DB
            // see http://stackoverflow.com/questions/3359414/android-column-id-does-not-exist
            // answer 91:
            // the rowid _id, * selects all columns plus add a column _id containing the rowid
            // the field _id is required for the ListView class
            query = "SELECT rowid _id,* FROM " + sqlite_WindDataHelper.RaceEntry.TABLE_NAME;
            query += " ORDER BY "+ sqlite_WindDataHelper.RaceEntry.COLUMN_ID+" DESC";
            cursor = db.rawQuery(query, null);
            if (cursor != null) {
                ctr = cursor.getCount();
                cursor.moveToFirst();

                //Log.d(LOG_TAG, "onResume() "+cursor.getCount()+" records found in query="+query);

                String[] fromColumns = new String[] {
                    sqlite_WindDataHelper.RaceEntry.COLUMN_ID,
                    sqlite_WindDataHelper.RaceEntry.COLUMN_RECORDS,
                    sqlite_WindDataHelper.RaceEntry.COLUMN_AVG_TWD,
                    sqlite_WindDataHelper.RaceEntry.COLUMN_AVG_TWS
                };
                int[] toViews = new int[] {R.id.Races_Date, R.id.Races_Records, R.id.Races_TWD, R.id.Races_TWS};

                // create the adapter using the cursor pointing to the desired data
                // as well as the row layout information stored in row_wind.xml file
                SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                        R.layout.row_races, cursor, fromColumns, toViews, 1);

                ListView RacesListView = (ListView) findViewById(R.id.RaceTableListView);
                if (RacesListView == null) {
                    Toast toast = Toast.makeText(this, "Could not initialize the Simple Cursor Adapter to display all the Races.", Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    // attach a setViewBinder to the ListView to format the Date data
                    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                        @Override
                        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                            // columnIndex-1 because we added one column _id to this query
                            if (columnIndex-1 == sqlite_WindDataHelper.COL_RACE_ID) {
                                TextView textView = (TextView) view;
                                // fetch the current date in seconds from the SQLite DB and convert
                                // it back to milliseconds.  We stored it in seconds to ensure
                                // compatibility with the web-based MySQL DB
                                // convert it into human readable date string
                                long millis = cursor.getLong(sqlite_WindDataHelper.COL_RACE_ID) * 1000L;
                                textView.setText(NavigationTools.getDateString(millis, "yyyy-MM-dd ss"));

                                return true;
                            }
                            return false;
                        }
                    });

                    // attach an onClickListener to the RacesListView
                    RacesListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                         public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                             //Log.d(LOG_TAG, "ClickListener - position="+position+"  id="+id);
                             NavigationTools.raceID = id;
                             switch_to_ListWindTable(view);
                         }
                    });

                    RacesListView.setAdapter(adapter);
                }
            } else {
                ctr = 0;
            }
            numberOfRaces.setText(String.valueOf(ctr));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // close the Cursor and DBhandle outside of onResume(0 to ensure that the Cursor is
        // active as long as the ListView in onResume() can access the Cursor
        dbhelper.closeDB(cursor, db);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
    Method to switch this activity to the "list_wind_table" activity.  This method is
    either called when the layout button "NumberOfRecords" get pressed or if one of the
    races listed in the ListView get selected by the user.
     */
    private void switch_to_ListWindTable(View view) {
        Intent myIntent = new Intent(view.getContext(), sqlite_show_wind_records.class);
        startActivityForResult(myIntent, 0);
    }

}
