package com.example.volkerpetersen.sailingrace;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by Volker Petersen in December 2015.
 */
public class sqlWindDataHelper extends SQLiteOpenHelper {
    public static final String LOG_TAG = sqlWindDataHelper.class.getSimpleName();
    private static sqlWindDataHelper sInstance;
    public final static int WIND_RECORDS = 150;  // SQLite DB FIFO Queue size (600 records = 10 min)

    public static synchronized sqlWindDataHelper getsInstance(Context context) {
        // the application context ensures that we don't leak this Activities content
        if (sInstance == null) {
            sInstance = new sqlWindDataHelper(context);
        }
        return sInstance;
    }

    public sqlWindDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // If you change the database schema below, you must increment the database version.
    public static final String DATABASE_URL = "http://www.southmetrochorale.org/OfflineWebApp/insert_wind_record.php";
    public static final int DATABASE_VERSION = 6;
    public static final String DATABASE_NAME = "OfflineWind.db";
    private static final String INT_TYPE =" INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public static final String SQL_DELETE_WIND =
            "DROP TABLE IF EXISTS " + WindEntry.TABLE_NAME;

    public static final String SQL_DELETE_RACE =
            "DROP TABLE IF EXISTS " + RaceEntry.TABLE_NAME;

    public static final String SQL_DELETE_windFIFO =
            "DROP TABLE IF EXISTS " + windFIFO.TABLE_NAME;

    // Inner class that defines the table content for the table WIND
    public static abstract class WindEntry implements BaseColumns {
        public static final String TABLE_NAME = "Wind";
        public static final String COLUMN_DATE = "Date";
        public static final String COLUMN_TWD = "TWD";
        public static final String COLUMN_TWS = "TWS";
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
        public static final String COLUMN_AVG_TWS = "avgTWS";
        public static final String COLUMN_AVG_TWD = "avgTWD";
        public static final String COLUMN_NAME = "Name";
    }

    // Inner class that defines the table content for the table PERFORMANCE
    public static abstract class windFIFO implements BaseColumns {
        public static final String TABLE_NAME = "WindFIFO";
        public static final String COLUMN_ID = "ID";
        public static final String COLUMN_DATE = "Date";
        public static final String COLUMN_TWD = "TWD";
        public static final String COLUMN_smoothedTWD = "smoothedTWD";
        public static final String COLUMN_avgSmoothedTWD = "avgSmoothedTWD";
        public static final String COLUMN_amplitude = "Amplitude";
        public static final String COLUMN_frequency = "Frequency";
        public static final String COLUMN_COG = "COG";
        public static final String COLUMN_SOG = "SOG";
        public static final String COLUMN_tack = "Tack";
    }

    public static final String[] WIND_COLUMNS = {
            WindEntry.COLUMN_DATE,
            WindEntry.COLUMN_TWD,
            WindEntry.COLUMN_TWS,
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
            RaceEntry.COLUMN_AVG_TWS,
            RaceEntry.COLUMN_AVG_TWD,
            RaceEntry.COLUMN_NAME
    };

    public static final String[] windFIFO_COLUMNS = {
            windFIFO.COLUMN_ID,
            windFIFO.COLUMN_DATE,
            windFIFO.COLUMN_TWD,
            windFIFO.COLUMN_smoothedTWD,
            windFIFO.COLUMN_avgSmoothedTWD,
            windFIFO.COLUMN_amplitude,
            windFIFO.COLUMN_frequency,
            windFIFO.COLUMN_COG,
            windFIFO.COLUMN_SOG,
            windFIFO.COLUMN_tack,
    };

    // These indices are tied to WIND_COLUMNS.  If WIND_COLUMNS changes, these must change.
    public static final int COL_WIND_DATE = 0;
    public static final int COL_WIND_TWD = 1;
    public static final int COL_WIND_TWS = 2;
    public static final int COL_WIND_QUADRANT = 3;
    public static final int COL_WIND_STATUS = 4;
    public static final int COL_WIND_RACE = 5;

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
    public static final int COL_NAME = 9;

    // These indices are tied to windFIFO_COLUMNS.  If windFIFO_COLUMNS changes, these must change.
    public static final int COL_windFIFO_ID = 0;
    public static final int COL_windFIFO_DATE = 1;
    public static final int COL_windFIFO_TWD = 2;
    public static final int COL_windFIFO_smoothedTWD = 3;
    public static final int COL_windFIFO_avgSmoothedTWD = 4;
    public static final int COL_windFIFO_AMPLITUDE = 5;
    public static final int COL_windFIFO_FREQUENCY = 6;
    public static final int COL_windFIFO_COG = 7;
    public static final int COL_windFIFO_SOG = 8;
    public static final int COL_windFIFO_TACK = 9;

    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_WIND =
                "CREATE TABLE IF NOT EXISTS " + WindEntry.TABLE_NAME + " (" +
                        WindEntry.COLUMN_DATE + INT_TYPE + " PRIMARY KEY," +
                        WindEntry.COLUMN_TWD + INT_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_TWS + REAL_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_QUADRANT + INT_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_STATUS + INT_TYPE + COMMA_SEP +
                        WindEntry.COLUMN_RACE + INT_TYPE +
                        " );";
        db.execSQL(SQL_CREATE_WIND);

        final String SQL_CREATE_RACE =
                "CREATE TABLE IF NOT EXISTS " + RaceEntry.TABLE_NAME + " (" +
                        RaceEntry.COLUMN_ID + INT_TYPE + " PRIMARY KEY," +
                        RaceEntry.COLUMN_WWD_LAT + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_WWD_LON + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_LWD_LAT + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_LWD_LON + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_CTR_LAT + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_CTR_LON + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_AVG_TWS + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_AVG_TWD + REAL_TYPE + COMMA_SEP +
                        RaceEntry.COLUMN_NAME + TEXT_TYPE +
                        " )";
        db.execSQL(SQL_CREATE_RACE);

        final String SQL_CREATE_windFIFO =
                "CREATE TABLE IF NOT EXISTS " + windFIFO.TABLE_NAME + " (" +
                        windFIFO.COLUMN_ID + INT_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                        windFIFO.COLUMN_DATE + INT_TYPE + COMMA_SEP +
                        windFIFO.COLUMN_TWD + REAL_TYPE + COMMA_SEP +
                        windFIFO.COLUMN_smoothedTWD + REAL_TYPE + COMMA_SEP +
                        windFIFO.COLUMN_avgSmoothedTWD + REAL_TYPE + COMMA_SEP +
                        windFIFO.COLUMN_amplitude + REAL_TYPE + COMMA_SEP +
                        windFIFO.COLUMN_frequency + REAL_TYPE + COMMA_SEP +
                        windFIFO.COLUMN_COG + REAL_TYPE + COMMA_SEP +
                        windFIFO.COLUMN_SOG + REAL_TYPE + COMMA_SEP +
                        windFIFO.COLUMN_tack + TEXT_TYPE + ");" +
                        " CREATE TRIGGER delete_tail AFTER INSERT ON "+ windFIFO.TABLE_NAME +
                        " BEGIN DELETE FROM "+ windFIFO.TABLE_NAME + " WHERE "+
                        windFIFO.COLUMN_ID+"%"+WIND_RECORDS+"=NEW."+windFIFO.COLUMN_ID+
                        "%"+WIND_RECORDS+" AND "+windFIFO.COLUMN_ID+"!=NEW."+windFIFO.COLUMN_ID+
                        " END";
        //Log.d(LOG_TAG, "SQL_CREATE_RACE create string="+SQL_CREATE_RACE);
        //Log.d(LOG_TAG, "windFIFO create string="+SQL_CREATE_windFIFO);
        db.execSQL(SQL_CREATE_windFIFO);
        /* sample SQLite statement from this web source: https://gist.github.com/elyezer/6450054
        CREATE TABLE ring_buffer (id INTEGER PRIMARY KEY AUTOINCREMENT, data TEXT);

        -- Number 10 on where statement defines the ring buffer's size
        CREATE TRIGGER delete_tail AFTER INSERT ON ring_buffer
                BEGIN
        DELETE FROM ring_buffer WHERE id%10=NEW.id%10 AND id!=NEW.id;
        END;
        */
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_WIND);
        db.execSQL(SQL_DELETE_RACE);
        db.execSQL(SQL_DELETE_windFIFO);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
