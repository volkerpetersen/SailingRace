package com.kaiserware.sailingrace;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Helper Class to fetch the GPS location, heading, and speed from the build-in location services.
 * Potentially to be replace by GPSlocation (developed Feb 2016) to take advantage of newer
 * LocationManager FuseLocationAPI. Initial testing prefers this older Class GPSTracker.
 *
 * Both classes (GPSTracker and GPSlocation) are compatible for use in the SailingRace app.
 *
 * Created by Volker Petersen November 2015.
 */
public class GPSTracker extends Service implements LocationListener {
    private GeomagneticField geoField;
    private Activity activity;
    private float gpsDistance;
    private long gpsTime;
    private boolean DECLINATION;
    public boolean LOCATION_CHANGED;
    public boolean CONNECTION;
    public boolean PERMISSION;
    private int MY_PERMISSION_ACCESS_FINE_LOCATION;
    public long UTCtime;
    public int CTR;
    private final double toKTS = 1.9438445;         // m/s to knots conversation factor
    private fifoQueueDouble COGfifo;                // fifo Queue with COG values
    private fifoQueueDouble SOGfifo;                // fifo Queue with SOG values
    private GlobalParameters para;                  // class object for the global parameters
    public Location location;
    private LocationManager locationManager;
    static final String LOG_TAG = GPSTracker.class.getSimpleName();

    /**
     * GPS location, heading, and speed service
     *
     * @param act                   - Activity of the Calling Class
     * @param gpsUpdates            - long minimum time (in milliseconds) between GPS updates
     * @param COG                   - fifQueueDouble holding Course Over Ground readings
     * @param SOG                   - fifQueueDouble holding Speed Over Ground readings 
     * @param permit                - int USER PERMIT CODE used in calling Class and this Class
     */
    public GPSTracker(Activity act, long gpsUpdates, int storeNumberLocations,
                       fifoQueueDouble COG, fifoQueueDouble SOG, int permit) {
        activity = act;
        CONNECTION = false;
        PERMISSION = false;
        LOCATION_CHANGED = false;
        COGfifo = COG;
        SOGfifo = SOG;
        gpsDistance = 0.0f;     // user can specify the min time between location updates.
        gpsTime = gpsUpdates;   // if gpsDistance > 0, then both gpsTime AND gpsDistance criteria
                                // before a new location point is generated.  Problem when standing still.

        // initialize our Global Parameter singleton class
        para = GlobalParameters.getInstance();
        CTR = 0;
    }

    /**
     * Initializes the LocationManager GPS Services
     * @param minGPSUpdateTime in milliseconds
     * @param minGPSDistance in meter
     * @return void
     */
    public void initializeGPS(long minGPSUpdateTime, float minGPSDistance) {
        Location loc=null;

        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ requires runtime permissions for location services
            int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
            Log.d(LOG_TAG, "Location permission = "+permissionCheck+" Granted="+PackageManager.PERMISSION_GRANTED);
            PERMISSION = (permissionCheck !=  PackageManager.PERMISSION_GRANTED);
        } else {
            PERMISSION = true;
            // Pre-Marshmallow do NOT require runtime permission requests for location services.
            // The permissions are set via the Manifest and don't require user confirmation.
        }
        try {
            PERMISSION = true;
            locationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);
            CONNECTION = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.d(LOG_TAG, "Location CONNECTION = "+CONNECTION+" with provider="+LocationManager.GPS_PROVIDER);

            if (CONNECTION && PERMISSION) {
                LOCATION_CHANGED = true;
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minGPSUpdateTime, minGPSDistance, this);
                } catch (SecurityException e) {
                    Log.e(LOG_TAG, "initializeGPS()-(1) PERMISSION_NOT_GRANTED by user: "+e);
                }

                if (locationManager != null) {
                    try {
                        loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        para.setBoatLat(loc.getLatitude());
                        para.setBoatLon(loc.getLongitude());
                        UTCtime = loc.getTime();
                    } catch (SecurityException e) {
                        Log.e(LOG_TAG,"initializeGPS()-(2) PERMISSION_NOT_GRANTED by user: "+e);
                    }
                }
            } else {
                // error msg
                Log.e(LOG_TAG, "Failed GPS PERMISSION: "+PERMISSION+"  CONNECTION: "+CONNECTION+" with provider="+LocationManager.GPS_PROVIDER);
                return;
            }
            if (loc != null) {
                location = loc;
                getDeclination();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "initializeGPS()-Error in Try/Catch: "+e);
        }
        return;
    }

    @Override
    public void onLocationChanged(Location locUpdate) {
        location = locUpdate;
        if (location != null) {
            // update the variables utilized in the Classes "Fragment_RaceInfo" and "Activity_StartSequence"
            para.setBoatLat(location.getLatitude());
            para.setBoatLon(location.getLongitude());

            para.setCOG(location.getBearing());
            COGfifo.add(para.getCOG());
            para.setAvgCOG(COGfifo.averageCompassDirection());

            para.setSOG(location.getSpeed()*toKTS);
            SOGfifo.add(para.getSOG());
            para.setAvgSOG(SOGfifo.average());

            LOCATION_CHANGED = true;
            UTCtime = location.getTime();
            CTR = CTR + 1;
            if (!DECLINATION) {
                getDeclination();
            }
            //Log.d(LOG_TAG, "New Lat="+location.getLatitude()+"  lng="+location.getLongitude());
        } else {
            Log.d(LOG_TAG, "onLocationChanged() is null - "+locUpdate.toString());
        }

    }

    @Override
    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Method to return the current Latitude
     * @return double latitude or NaN if no location is available
     */
    public double getLatitude() {
        if (CONNECTION && location != null) {
            return location.getLatitude();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Method to return the current Longitude
     * @return double longitude or NaN if no location is available
     */
    public double getLongitude() {
        if (CONNECTION && location != null) {
            return location.getLongitude();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Method to start the current GPS location thread
     */
    public void startGPS() {
        initializeGPS(gpsTime, gpsDistance);
    }

    /**
     * Method to stop the current GPS location thread
     */
    public void stopGPS() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(GPSTracker.this);
            } catch (SecurityException e) {
                Log.e(LOG_TAG,"3) PERMISSION_NOT_GRANTED by user");
            }
        }
    }

    public String getBestProvider() {
        String provider = "???";
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        if (locationManager != null) {
            provider = locationManager.getBestProvider(criteria, true);
            if (provider != null) {
                provider = provider.toUpperCase();
            } else {
                provider = "???";
            }
        }
        return provider;
    }

    public void getDeclination() {
        if (location != null) {
            geoField = new GeomagneticField(
                    Double.valueOf(location.getLatitude()).floatValue(),
                    Double.valueOf(location.getLongitude()).floatValue(),
                    Double.valueOf(location.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );
            para.setDeclination(geoField.getDeclination());
            DECLINATION = true;
        } else {
            para.setDeclination(0.0d);
            DECLINATION = false;
        }
    }
}
