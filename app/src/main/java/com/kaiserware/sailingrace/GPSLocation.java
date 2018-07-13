package com.kaiserware.sailingrace;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.location.Location;
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.util.LinkedList;

/**
 * Helper Class to fetch the GPS location, heading, and speed from the build-in location services
 * utilizing the updated LocationManager's FusedLocationAPI which depends on GooglePlay Services.
 * Both classes (GPSTracker and GPSlocation) are compatible for use in the SailingRace app.
 *
 * Created by Volker Petersen February 2016.
 */
public class GPSLocation implements
                            GoogleApiClient.ConnectionCallbacks,
                            GoogleApiClient.OnConnectionFailedListener,
                            LocationListener {
    private final String LOG_TAG = GPSLocation.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private GeomagneticField mGeoField;
    private int REQUEST_CODE_FINE_LOCATION;
    private int keepNumPositions;
    private long gpsTime;
    private float gpsDistance;
    public long UTCtime;
    private fifoQueueDouble COGfifo;
    private fifoQueueDouble SOGfifo;
    private Activity activity;
    public LinkedList<Location> LocationList = new LinkedList<Location>();
    private GlobalParameters para;
    public static final double toKTS = 1.94384;   // m/s to nm/hrs=kts conversion factor
    public int CTR;
    public Location location;
    public boolean PERMISSION;
    public boolean CONNECTION;
    public boolean LOCATION_CHANGED;
    public boolean DECLINATION;

    /**
     * GPS location, heading, and speed service
     *
     * @param act                   - Activity of the Calling Class
     * @param gpsUpdates            - long minimum time (in milliseconds) between GPS updates
     * @param storeNumberLocations  - int keepNumPositions: max GPS location history to compute average speed / heading
     * @param COG                   - fifQueueDouble holding Course Over Ground readings 
     * @param SOG                   - fifQueueDouble holding Speed Over Ground readings 
     * @param permit                - int USER PERMIT CODE used in calling Class and this Class
     */
    public GPSLocation(Activity act, long gpsUpdates, int storeNumberLocations,
                       fifoQueueDouble COG, fifoQueueDouble SOG, int permit) {
        activity = act;
        keepNumPositions = storeNumberLocations;
        REQUEST_CODE_FINE_LOCATION = permit;
        CONNECTION = false;
        LOCATION_CHANGED = false;
        COGfifo = COG;
        SOGfifo = SOG;
        para = GlobalParameters.getInstance();
        //Log.d(LOG_TAG, "Global Parameters = "+para);

        gpsDistance = 0.0f;     // user can specify the minimum time between location updates.
        gpsTime = gpsUpdates;   // if gpsDistance > 0, then both gpsTime AND gpsDistance criteria must be met
                                // before a new location point is generated.  Problem when standing still.

        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ requires runtime permissions for location services
            int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
            //Log.d(LOG_TAG, "Location permission = " + permissionCheck + " Granted=" + PackageManager.PERMISSION_GRANTED);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                //Log.d(LOG_TAG, "No sufficient permission to run Fine Location services!");
                // Should we show an explanation why we request a runtime permission?
                PERMISSION = false;
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //Log.d(LOG_TAG, "shouldShowRequestPermissionRationale = true");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    String msg = "In order for the Sailing Race App to have full functionality ";
                    msg += "the Location services must be enabled.  Do you want to do so now?";
                    showMessagePositiveNegative(msg, "Yes", "No");
                } else {
                    //Log.d(LOG_TAG, "shouldShowRequestPermissionRationale = false");
                    // No explanation needed, we can request the permission.
                    grantPermission();
                }
            } else {
                PERMISSION = true;
            }
        } else {
            // Pre-Marshmallow do NOT require runtime permission requests for location services
            // the permissions set in the Manifest file will be used.
            PERMISSION = true;
        }
        //Log.d(LOG_TAG, "PERMISSION = " + PERMISSION);

        if (PERMISSION) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        } else {
            mGoogleApiClient = null;
        }
    }

    public void grantPermission() {
        // REQUEST_CODE_FINE_LOCATION is an app-defined int constant.
        // The callback method (onRequestPermissionResult) in MainActivity
        // gets the result of this request.
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE_FINE_LOCATION);
    }

    public void startGPS() {
        // Connect the client and fetch the last known location so that we have an immediate location fix
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        CTR = 0;
        //Log.d(LOG_TAG, "startGPS() - has mGoogleApiClient = " + mGoogleApiClient);
    }

    public void stopGPS() {
        // Disconnecting the client invalidates it.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        //Log.d(LOG_TAG, "stopGPS() - stopped the mGoogleApiClient");
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(gpsDistance);
        mLocationRequest.setInterval(gpsTime);  // Update location approx every gpsTime milliseconds
        CONNECTION = true;
        if (Build.VERSION.SDK_INT >= 23) {
            CONNECTION = (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            //Log.d(LOG_TAG, "onConnected() permit check="+CONNECTION);
        }
        if (CONNECTION) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            para.setBoatLat(getLatitude());
            para.setBoatLon(getLongitude());
            if (location != null) {
                UTCtime = location.getTime();
            }

            //Log.d(LOG_TAG, "onConnected() Boat lat=" + para.getBoatLat() + "  lng=" + para.getBoatLon());
            //Log.d(LOG_TAG, "onConnected() CONNECTION=" + CONNECTION + "  PERMISSION=" + PERMISSION);
        } else {
            // user revoked earlier Location Request Permission.  We need to request it again.
            //Log.d(LOG_TAG, "onConnected() - Couldn't establish Location Connection. Setting CONNECTION=false");
            CONNECTION = false;
            grantPermission();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "GoogleApiClient connection has been suspend");
        CONNECTION = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "GoogleApiClient connection has failed: "+connectionResult.toString());
        CONNECTION = false;
    }

    @Override
    public void onLocationChanged(Location current_location) {
        location = current_location;
        try {
            if (LocationList.size() >= keepNumPositions && keepNumPositions > 0) {
                LocationList.removeFirst();
            }
            if ( !DECLINATION && (location != null) )   {
                mGeoField = new GeomagneticField(
                        Double.valueOf(location.getLatitude()).floatValue(),
                        Double.valueOf(location.getLongitude()).floatValue(),
                        Double.valueOf(location.getAltitude()).floatValue(),
                        System.currentTimeMillis()
                );
                para.setDeclination( mGeoField.getDeclination());
            }
            LocationList.add(location);

            // update the variables utilized in the Classes "Fragment_RaceInfo" and "Activity_StartSequence"
            para.setBoatLat(location.getLatitude());
            para.setBoatLon(location.getLongitude());
            para.setCOG(location.getBearing());
            para.setSOG(location.getSpeed()*toKTS);
            COGfifo.add(para.getCOG());
            SOGfifo.add(para.getSOG());
            para.setAvgSOG(SOGfifo.average());
            para.setAvgCOG(COGfifo.averageCompassDirection());
            UTCtime = location.getTime();
            CONNECTION = true;
            LOCATION_CHANGED = true;
            CTR = CTR + 1;
            //Log.d(LOG_TAG, "New Lat="+location.getLatitude()+"  lng="+location.getLongitude());
        } catch (Exception e) {
            LOCATION_CHANGED = false;
            Log.e(LOG_TAG, "onLocationChanged() - Error msg = "+e);
        }
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

    public String getBestProvider() {
        if (CONNECTION) {
            return "gps";
        } else {
            return ("???");
        }
    }

    public void showMessagePositiveNegative(String message, String positive, String negative) {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                activity, android.R.style.Theme_Holo_Dialog));

        builder.setTitle(R.string.app_name);
        //builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage(message);

        builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                grantPermission();
            }
        });

        builder.setNegativeButton(negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // NOTHING we can do if user doesn't grant location service permission
            }
        });
        alert = builder.create();
        alert.show();
    }

}
