package com.kaiserware.sailingrace;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Helper Class which defines the sqlite database that is utilized to record wind and
 * course data during the race.  This data can be uploaded to a Cloud-based MySQL DB
 * for further processing / analysis.
 *
 * Created by Volker Petersen in December 2015.
 */
public class sqlite_WindDataHelper extends SQLiteOpenHelper {
    public static final String LOG_TAG = sqlite_WindDataHelper.class.getSimpleName();
    private static sqlite_WindDataHelper sInstance;

    public static synchronized sqlite_WindDataHelper getsInstance(Context context) {
        // the application context ensures that we don't leak this Activities content
        if (sInstance == null) {
            sInstance = new sqlite_WindDataHelper(context);
        }
        return sInstance;
    }

    public sqlite_WindDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // If you change the database schema below, you must increment the database version.
    public static final int DATABASE_VERSION = 14;
    public static final String DATABASE_URL = "https://www.southmetrochorale.org/OfflineWebApp/insert_wind_record.php";
    public static final String DATABASE_NAME = "OfflineWind.db";
    private static final String INT_TYPE =" INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public static final String SQL_DELETE_WIND =
            "DROP TABLE IF EXISTS " + WindEntry.TABLE_NAME;

    public static final String SQL_DELETE_RACE =
            "DROP TABLE IF EXISTS " + RaceEntry.TABLE_NAME;

    public static final String SQL_DELETE_LOCATION =
            "DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME;

    // Inner class that defines the table content for the table LOCATION
    public static abstract class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "Location";
        public static final String COLUMN_RACE = "Race_ID";
        public static final String COLUMN_LWD_LAT = "LWD_LAT";
        public static final String COLUMN_LWD_LON = "LWD_LON";
        public static final String COLUMN_WWD_LAT = "WWD_LAT";
        public static final String COLUMN_WWD_LON = "WWD_LON";
        public static final String COLUMN_CTR_LAT = "CTR_LAT";
        public static final String COLUMN_CTR_LON = "CTR_LON";
    }

    // Inner class that defines the table content for the table WIND
    public static abstract class WindEntry implements BaseColumns {
        public static final String TABLE_NAME = "Wind";
        public static final String COLUMN_DATE = "Date";
        public static final String COLUMN_AWD = "AWD";
        public static final String COLUMN_AWS = "AWS";
        public static final String COLUMN_TWD = "TWD";
        public static final String COLUMN_TWS = "TWS";
        public static final String COLUMN_SOG = "SOG";
        public static final String COLUMN_LAT = "LAT";
        public static final String COLUMN_LON = "LON";
        public static final String COLUMN_QUADRANT = "Quadrant";
        public static final String COLUMN_STATUS = "Status";
        public static final String COLUMN_RACE = "Race_ID";
    }

    // Inner class that defines the table content for the table Races
    public static abstract class RaceEntry implements BaseColumns {
        public static final String TABLE_NAME = "Races";
        public static final String COLUMN_ID = "Race_ID";
        public static final String COLUMN_WWD_LAT = "WWD_LAT";
        public static final String COLUMN_WWD_LON = "WWD_LON";
        public static final String COLUMN_LWD_LAT = "LWD_LAT";
        public static final String COLUMN_LWD_LON = "LWD_LON";
        public static final String COLUMN_CTR_LAT = "CTR_LAT";
        public static final String COLUMN_CTR_LON = "CTR_LON";
        public static final String COLUMN_AVG_TWD = "avgTWD";
        public static final String COLUMN_AVG_TWS = "avgTWS";
        public static final String COLUMN_RECORDS = "records";
        public static final String COLUMN_NAME = "Name";
    }

    public static final String[] LOCATION_COLUMNS = {
            LocationEntry.COLUMN_RACE,
            LocationEntry.COLUMN_LWD_LAT,
            LocationEntry.COLUMN_LWD_LON,
            LocationEntry.COLUMN_WWD_LAT,
            LocationEntry.COLUMN_WWD_LON,
            LocationEntry.COLUMN_CTR_LAT,
            LocationEntry.COLUMN_CTR_LON
    };
    public static final String[] WIND_COLUMNS = {
            WindEntry.COLUMN_DATE,
            WindEntry.COLUMN_AWD,
            WindEntry.COLUMN_AWS,
            WindEntry.COLUMN_TWD,
            WindEntry.COLUMN_TWS,
            WindEntry.COLUMN_SOG,
            WindEntry.COLUMN_QUADRANT,
            WindEntry.COLUMN_STATUS,
            WindEntry.COLUMN_RACE
    };

    public static final String[] RACE_COLUMNS = {
            RaceEntry.COLUMN_ID,
            RaceEntry.COLUMN_WWD_LAT,
            RaceEntry.COLUMN_WWD_LON,
            RaceEntry.COLUMN_LWD_LAT,
            RaceEntry.COLUMN_LWD_LON,
            RaceEntry.COLUMN_CTR_LAT,
            RaceEntry.COLUMN_CTR_LON,
            RaceEntry.COLUMN_AVG_TWD,
            RaceEntry.COLUMN_AVG_TWS,
            RaceEntry.COLUMN_RECORDS,
            RaceEntry.COLUMN_NAME
    };

    // These indices are tied to LOCATION_COLUMNS.  If LOCATION_COLUMNS changes, these must change.
    public static final int COL_LOC_RACE = 0;
    public static final int COL_LOC_LWD_LAT = 1;
    public static final int COL_LOC_LWD_LON = 2;
    public static final int COL_LOC_WWD_LAT = 3;
    public static final int COL_LOC_WWD_LON = 4;
    public static final int COL_LOC_CTR_LAT = 5;
    public static final int COL_LOC_CTR_LON = 6;

    // These indices are tied to WIND_COLUMNS.  If WIND_COLUMNS changes, these must change.
    public static final int COL_WIND_DATE = 0;
    public static final int COL_WIND_AWD = 1;
    public static final int COL_WIND_AWS = 2;
    public static final int COL_WIND_TWD = 3;
    public static final int COL_WIND_TWS = 4;
    public static final int COL_WIND_SOG = 5;
    public static final int COL_WIND_LAT = 6;
    public static final int COL_WIND_LON = 7;
    public static final int COL_WIND_QUADRANT = 8;
    public static final int COL_WIND_STATUS = 9;
    public static final int COL_WIND_RACE = 10;

    // These indices are tied to RACE_COLUMNS.  If RACE_COLUMNS changes, these must change.
    public static final int COL_RACE_ID = 0;
    public static final int COL_WWD_LAT = 1;
    public static final int COL_WWD_LON = 2;
    public static final int COL_LWD_LAT = 3;
    public static final int COL_LWD_LON = 4;
    public static final int COL_CTR_LAT = 5;
    public static final int COL_CTR_LON = 6;
    public static final int COL_AVG_TWS = 7;
    public static final int COL_AVG_TWD = 8;
    public static final int COL_RECORDS = 9;
    public static final int COL_NAME = 10;

    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_LOCATION =
                "CREATE TABLE IF NOT EXISTS " + LocationEntry.TABLE_NAME + " (" +
                        LocationEntry.COLUMN_RACE + INT_TYPE + " PRIMARY KEY" + COMMA_SEP +
                        LocationEntry.COLUMN_LWD_LAT + REAL_TYPE + COMMA_SEP +
                        LocationEntry.COLUMN_LWD_LON + REAL_TYPE + COMMA_SEP +
                        LocationEntry.COLUMN_WWD_LAT + REAL_TYPE + COMMA_SEP +
                        LocationEntry.COLUMN_WWD_LON + REAL_TYPE + COMMA_SEP +
                        LocationEntry.COLUMN_CTR_LAT + REAL_TYPE + COMMA_SEP +
                        LocationEntry.COLUMN_CTR_LON + REAL_TYPE +
                        " );";
        db.execSQL(SQL_CREATE_LOCATION);

        final String SQL_CREATE_WIND =
                "CREATE TABLE IF NOT EXISTS " + WindEntry.TABLE_NAME + " (" +
                        WindEntry.COLUMN_DATE + INT_TYPE + " PRIMARY KEY" + COMMA_SEP +
                        WindEntry.COLUMN_AWD + INT_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_AWS + REAL_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_TWD + INT_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_TWS + REAL_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_SOG + REAL_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_LAT + REAL_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_LON + REAL_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_QUADRANT + INT_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_STATUS + INT_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_RACE + INT_TYPE +
                        " );";
        db.execSQL(SQL_CREATE_WIND);

        final String SQL_CREATE_RACE =
                "CREATE TABLE IF NOT EXISTS " + RaceEntry.TABLE_NAME + " (" +
                        RaceEntry.COLUMN_ID + INT_TYPE + " PRIMARY KEY" + COMMA_SEP +
                        RaceEntry.COLUMN_WWD_LAT + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_WWD_LON + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_LWD_LAT + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_LWD_LON + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_CTR_LAT + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_CTR_LON + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_AVG_TWS + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_AVG_TWD + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_RECORDS + INT_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_NAME + TEXT_TYPE +
                        " )";
        db.execSQL(SQL_CREATE_RACE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_WIND);
        db.execSQL(SQL_DELETE_RACE);
        db.execSQL(SQL_DELETE_LOCATION);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Method to close the SQLiteDatabase
     * @param cursor current cursor of the DB to be close
     * @param db handle of the DB to be closed
     */
    public void closeDB(Cursor cursor, SQLiteDatabase db) {
        if (cursor!=null && !cursor.isClosed()) {
            cursor.close();
        }
        if (db!=null && db.isOpen()) {
            db.close();
        }
    }

}
