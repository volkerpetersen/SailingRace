package com.kaiserware.sailingrace;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This helper Class is used by:
 * 1) "Activity_StartSequence" (Fragment_StartSequence) to set the windward mark by dragging a marker from
 *    the current boat location to some other position and to display the Startline, Boat Heading
 *    and the Time / Distance to line and time to burn.
 * 2) "Activity_RaceInfo" (Fragment_RaceInfo) to display the current race course, best tack, and
 *    laylines
 *
 * All angles are assumed to be True North for all computations within this App.  All UI directions
 * will be displayed as Magnetic North using the GPS declination to convert from True North to
 * Magnetic North.
 *
 * Created by Volker Petersen on November 2015.
 */
public class Fragment_DisplayGoogleMap extends Fragment implements OnMapReadyCallback {
    private MapView mMapView;
    private GoogleMap mMap;
    private LatLng windwardMarker = null;
    private LatLng leewardMarker = null;
    private LatLng activeMarker = null;
    private LatLng boat;
    private LatLng start;
    private LatLng end;
    private LatLng tack_position;
    private double boatLat;
    private double boatLon;
    private double markerLat;
    private double markerLon;
    private double[] toMark = new double[2];
    private double[] Laylines = new double[5];
    private int WINDEX;
    private GlobalParameters para;
    private Context appContext;
    private TextView outputHeaderBar;
    private View view;
    private Marker wwdMarker = null;
    private Marker mark;
    private Button WWDmark = null;
    private List<Marker> markers;
    private List<Polyline> polylines;
    private Handler MapUpdate=new Handler(); // Handler to implement a Runnable for the Map Updates
    private boolean startMapUpdate;
    private boolean startSequence;           // true when called from "Activity_StartSequence", false when called from "Activity_RaceInfo"
    private boolean face_into_wind;          // set to true if Map should be facing into the wind in Startline display
    private boolean animation;
    public final int DEFAULT_ZOOM_LEVEL = 14; // larger zoom levels means more detail on the map
    public final String LOG_TAG = Fragment_DisplayGoogleMap.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.fragment_google_map, container, false);

        // initialize our Global Parameter singleton class and get Application context
        para = GlobalParameters.getInstance();
        appContext = getActivity().getApplicationContext();

        // Debugging / Testing Code
        if (Activity_Main.testing) {
            NavigationTools.TWD_shortAVG = 280.0d;
            para.setTWD(NavigationTools.TWD_shortAVG);
            para.setCOG(NavigationTools.fixAngle(NavigationTools.TWD_shortAVG-45.0d));
            para.setAvgCOG(para.getCOG());
            para.setAvgSOG(4.0);
            boatLat = 44.91475;
            boatLon = -93.61474;
            para.setBoatLat(boatLat);
            para.setBoatLon(boatLon);
            double[] toDistance = NavigationTools.withDistanceBearingToPosition(para.getBoatLat(), para.getBoatLon(), 0.4d, NavigationTools.fixAngle(para.getAvgCOG()+45.0d));
            para.setCommitteeLat(toDistance[0]);
            para.setCommitteeLon(toDistance[1]);
            para.setLeewardFlag(true);
            leewardMarker = new LatLng(toDistance[0], toDistance[1]);
            toDistance = NavigationTools.withDistanceBearingToPosition(para.getBoatLat(), para.getBoatLon(), 0.7d, NavigationTools.fixAngle(para.getAvgCOG()-25.0d));
            para.setPinLat(toDistance[0]);
            para.setPinLon(toDistance[1]);
            para.setLeewardPinFlag(true);
        }
        // end of testing code */

        try {
            mMapView = (MapView) view.findViewById(R.id.map_container);
            mMapView.onCreate(savedInstanceState);
            MapsInitializer.initialize(appContext);
            mMapView.getMapAsync(this);
            outputHeaderBar = (TextView) view.findViewById(R.id.map_header);
            WWDmark = (Button) view.findViewById(R.id.header_button);
        } catch (Exception e) {
            Log.e(LOG_TAG, "onCreateView() error initializing the Google Map: "+e.getMessage());
            OK_dialog_message("Google Map Display", "Failed to initialize the Google Map. Terminating.");
            return view;
        }

        if ( (getActivity().getClass().getSimpleName()).equals("Activity_StartSequence") ) {
            // True if this Fragment_DisplayGoogleMap was called from the "Activity_StartSequence"
            //Log.d(LOG_TAG, "DisplayGoogleMap() - launched by Activity_StartSequence");
            if (para.getWindwardFlag()) {
                startMapUpdate = false;         // if WWD Mark is not yet set, map doesn't require continuous updates
            } else {
                startMapUpdate = true;          // WWD Mark is set and we want to update the approach toward the Start Line
            }
            startSequence = true;               // display all elements belonging to the start sequence map display
            face_into_wind = true;              // rotate the map to face into the wind

            if ( para.getWindwardFlag() ) {
                //Log.d(LOG_TAG, "DisplayGoogleMap() - Enable WWD Set Button listener");
                WWDmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // if we have a Click, we want to go Set or Clear the Windward mark
                        if (para.getWindwardFlag()) {
                            // clear the Windward Mark if user confirms the deletion
                            clearMarker("Windward");
                        } else {
                            // set the Windward Mark at current boat location
                            //Log.d(LOG_TAG, "Launching the Fragment_DisplayGoogleMap");
                            para.setWindwardFlag(true);
                            para.setWindwardLat(para.getBoatLat());
                            para.setWindwardLat(para.getBoatLon());
                        }
                    }
                });
            } else {
                //Log.d(LOG_TAG, "DisplayGoogleMap() - hide WWD Set Button");
                WWDmark.setVisibility(view.GONE);
            }

            //Log.d(LOG_TAG, "onCreateView() ParentActivity()"+getActivity());
        } else {
            // This fragment was launched from the "Activity_RaceInfo".  Now we want to update the map.
            //Log.d(LOG_TAG, "DisplayGoogleMap() - launched by Activity_RaceInfo and hide WWD Set Button");
            startMapUpdate = true;
            startSequence = false;              // display all map elements belonging to the RaceInfo
            face_into_wind = false;             // rotate map to face due North
            WWDmark.setVisibility(View.GONE);   // hide the set WWD Mark button in the header bar
        }
        //Log.d(LOG_TAG, "Activity Name="+getActivity().getClass().getSimpleName()+" logic check="+(getActivity().getClass().getSimpleName()).equals("Activity_Display_GoogleMap"));

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (startMapUpdate){
            MapUpdate.removeCallbacks(updateMapElementsNow);   // stop the MapUpdate Handler Runnable
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        WINDEX = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext);

        if (startMapUpdate) {
            animation = false;
            MapUpdate.post(updateMapElementsNow);              // start the MapUpdate Handler Runnable
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        if (startMapUpdate){
            MapUpdate.removeCallbacks(updateMapElementsNow);   // stop the MapUpdate Handler Runnable
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public void onBackPressed() {
        // Log.d(LOG_TAG, "Found back button in Fragment!!!");
        para.setBestTack(NavigationTools.LaylinesString(Laylines[2]));
        onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Manipulates the map once it has been loaded from the web.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //@Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        addElementsToMap();

        // add a Listener to Marker to allow Marker to be moved.
        // Only Markers that have the attribute ".draggable(true)" are processed by this method
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker markerDragStart) {
                //Log.d(LOG_TAG, "Start Google Map Marker drag");
                if (startMapUpdate){
                    MapUpdate.removeCallbacks(updateMapElementsNow);   // stop the MapUpdate Handler Runnable
                }
            }

            @Override
            public void onMarkerDrag(Marker markerDrag) {
                markerLat = markerDrag.getPosition().latitude;
                markerLon = markerDrag.getPosition().longitude;
                toMark = NavigationTools.MarkDistanceBearing(boatLat, boatLon, markerLat, markerLon);
                outputHeaderBar.setText("Windward: " + appContext.getString(R.string.BTM_DTM, toMark[1], toMark[0]));
                //Log.d(LOG_TAG, "Dragging Google Map Marker");
            }

            @Override
            public void onMarkerDragEnd(Marker markerDragEnd) {
                markerLat = markerDragEnd.getPosition().latitude;
                markerLon = markerDragEnd.getPosition().longitude;

                toMark = NavigationTools.MarkDistanceBearing(boatLat, boatLon, markerLat, markerLon);
                outputHeaderBar.setText("Windward: " + appContext.getString(R.string.BTM_DTM, toMark[1], toMark[0]));

                // store Windward marker position in global parameters
                para.setWindwardLat(markerLat);
                para.setWindwardLon(markerLon);
                para.setWindwardFlag(true);
                para.setWindwardRace(true);
                WWDmark.setVisibility(view.VISIBLE);   // Make the WWD Mark reset button visible
                startMapUpdate = true;                 // WWD Mark is set and we want to update the approach toward the Start Line
                MapUpdate.post(updateMapElementsNow);  // start the MapUpdate Handler Runnable
                //Log.d(LOG_TAG, "TWD = "+para.getTWD()+"  "+NavigationTools.TWD_shortAVG);
                //Log.d(LOG_TAG, "End Marker drag lat: "+markerLat+"  lon: "+markerLon+"  WWD flag="+para.getWindwardFlag());
            }
        });
    }

    /**
     * Runnable to be executed every second to update the Google Map elements
     */
    Runnable updateMapElementsNow = new Runnable() {
        public void run() {
            //Log.d(LOG_TAG, "TWD = "+para.getTWD()+"  "+NavigationTools.TWD_shortAVG);
            if ((para.getWindwardRace() || para.getLeewardRace()) && para.getLeewardFlag() && para.getWindwardFlag()) {
                removeElementsFromMap();
                addElementsToMap();
            }
            //Log.d(LOG_TAG, "TWD = "+para.getTWD()+"  "+NavigationTools.TWD_shortAVG);
            MapUpdate.postDelayed(this, 1000);
        }
    };

    /**
     * Method to remove all elements from the Map
     */
    void removeElementsFromMap() {
        try {
            // loop thru all elements of the ArrayList markers
            for (Marker mark: markers) {
                mark.remove();
            }
            // loop thru all elements of the ArrayList polylines
            for (Polyline line: polylines) {
                line.remove();
            }
            animation = true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "removeElementsFromMap() error "+e.getMessage());
        }
    }

    /**
     * Method to add all elements (Markers and Polylines) to the Map
     *
     * This methods is being utilized both by "Fragment_StartSequence" and "Fragment_RaceInfo".
     * For each of these parent Fragments different elements will be added to the map.
     */
    void addElementsToMap() {
        String tmp;
        boolean showLaylines = false;
        float rotation;
        double delta;
        double favored = Double.NaN;
        double[] toDistance;
        double[] intersection_data;
        LatLng boatHeadingTo;
        LatLng intersection;
        LatLng pin = null;
        LatLng committee = null;
        DecimalFormat df0 = new DecimalFormat("#0");

        // initialize the 2 ArrayLists to store all elements being added to the Google Maps
        markers = new ArrayList<Marker>();
        polylines = new ArrayList<Polyline>();

        boatLat = para.getBoatLat();
        boatLon = para.getBoatLon();
        tack_position = new LatLng(boatLat, boatLon);
        boat = new LatLng(boatLat, boatLon);

        double TWD = para.getTWD();
        double mCourse = para.getCourseOffset();
        double TackGybe = para.getTackGybe();
        String tack = para.getTack();
        int boatResource = R.drawable.boatgreen_black;
        boolean tack_position_ok = false;
        int zoomLevel = DEFAULT_ZOOM_LEVEL;

        leewardMarker = new LatLng(para.getLeewardLat(), para.getLeewardLon());

        // if no Windward Marker is defined yet, set a default one a quarter mile upwind from boat
        if ( para.getWindwardFlag() ) {
            windwardMarker = new LatLng(para.getWindwardLat(), para.getWindwardLon());
        } else {
            toDistance = NavigationTools.withDistanceBearingToPosition(boat.latitude, boat.longitude, para.courseLength, NavigationTools.TWD_shortAVG);
            windwardMarker = new LatLng(toDistance[0], toDistance[1]);
        }

        if (face_into_wind && para.getSailtimerStatus()) {
            rotation = (float)NavigationTools.TWD_shortAVG;  // map faces into the wind
        } else {
            rotation = 0.0f;                                 // map faces straight north
        }
        //Log.d(LOG_TAG, "Map rotation: "+ rotation);

        // set Map Center (CameraPosition) a quarter mile to windward and with bearing=rotation set above
        toDistance = NavigationTools.withDistanceBearingToPosition(boat.latitude, boat.longitude, 0.25d, NavigationTools.TWD_shortAVG);
        LatLng map_center = new LatLng(toDistance[0], toDistance[1]);
        CameraPosition camera = new CameraPosition.Builder()
                .target(map_center)     // set position at current boat lat lng
                .tilt(0)                // look straight down onto the map
                .bearing(rotation)      // map rotation based on face_to_wind setting(either into the wind or due North)
                .zoom(zoomLevel)        // set zoom
                .build();               // create a Camera Position with the above parameters

        if (startSequence) {
            //
            // we are displaying this map within the "Fragment_StartSequence" context
            //

            // plot the start-line and intersection when the start-line has been defined by user
            if (para.getLeewardFlag() && para.getLeewardPinFlag()) {
                pin = new LatLng(para.getPinLat(), para.getPinLon());
                committee = new LatLng(para.getCommitteeLat(), para.getCommitteeLon());
                toDistance = NavigationTools.withDistanceBearingToPosition(boat.latitude, boat.longitude, 1.0d, para.getAvgCOG());
                boatHeadingTo = new LatLng(toDistance[0], toDistance[1]);

                favored = NavigationTools.favoredPin(committee, pin, NavigationTools.TWD_shortAVG);
                intersection_data = NavigationTools.LineIntersection(boat, committee, pin, para.getAvgCOG(), para.getAvgSOG());

                polylines.add(mMap.addPolyline(new PolylineOptions().add(boat, boatHeadingTo).width(12).color(Color.GREEN)));
                polylines.add(mMap.addPolyline(new PolylineOptions().add(committee, pin).width(12).color(Color.RED)));

                // add the Start-line Intersection Marker
                if (!Double.isNaN(intersection_data[0]) && !Double.isNaN(intersection_data[1])) {
                    intersection = new LatLng(intersection_data[2], intersection_data[3]);
                    tmp = "Time: "+df0.format(intersection_data[1])+"sec  Distance: "+df0.format(intersection_data[0]/NavigationTools.km_nm*1000.0)+"m";
                    tmp += "  Burn: "+df0.format(Fragment_StartSequence.secondsToGun - intersection_data[1])+"sec";
                    mark = mMap.addMarker(new MarkerOptions()
                            .position(intersection)
                            .draggable(false)
                            .title(tmp)
                            .anchor(0.5f, 0.5f)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.tack_marker))
                            //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)))
                            .alpha(0.7f));
                    mark.showInfoWindow();
                    markers.add(mark);
                }
            }

            // add the committee boat Marker
            if ( !Double.isNaN(para.getCommitteeLat()) && !Double.isNaN(para.getCommitteeLon())) {
                mark = mMap.addMarker(new MarkerOptions()
                        .position(committee)
                        .draggable(false)
                        .title("Committee")
                        .rotation((float)para.courseBearing)
                        .anchor(0.5f, 1.0f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.committee)));
                markers.add(mark);
            }

            // add the Pin Marker
            if (para.getLeewardPinFlag()) {
                mark = mMap.addMarker(new MarkerOptions()
                        .position(pin)
                        .draggable(false)
                        .title("Pin")
                        .rotation((float)para.courseBearing)
                        .anchor(0.2f, 1.0f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));
                markers.add(mark);
            }

            // add Leeward Marker
            if (para.getLeewardFlag()) {
                tmp = "at "+NavigationTools.PositionDegreeToString(leewardMarker.latitude, true);
                tmp += " and "+NavigationTools.PositionDegreeToString(leewardMarker.longitude, false);
                mark = mMap.addMarker(new MarkerOptions()
                        .position(leewardMarker)
                        .draggable(false)
                        .title("Leeward Mark")
                        .snippet(tmp)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                markers.add(mark);
            }

            // set boat resource base on current Heading and Wind Direction
            if (NavigationTools.HeadingDelta(para.getCOG(), NavigationTools.TWD_shortAVG) >= 0.0d) {
                boatResource = R.drawable.boatgreen_black;
            } else {
                boatResource = R.drawable.boatred_black;
            }
            // end of the "Fragment_StartSequence elements display
            //
        } else {
            //
            // we are displaying this map within the "Fragment_RaceInfo" context
            //

            // determine if we race upwind / downwind and if the necessary Marks are defined
            if ( para.getLeewardFlag() ) {
                leewardMarker = new LatLng(para.getLeewardLat(), para.getLeewardLon());
            }

            if (mCourse == 0.0f) {
                markerLat = para.getWindwardLat();
                markerLon = para.getWindwardLon();
                activeMarker = windwardMarker;
            } else if (mCourse == 180.0f) {
                markerLat = para.getLeewardLat();
                markerLon = para.getLeewardLon();
                activeMarker = leewardMarker;
            } else {
                showLaylines = false;
            }

            if (para.getWindwardFlag() && para.getLeewardFlag()) {
                showLaylines = true;
            }

            //Log.d(LOG_TAG, "Fragment_DisplayGoogleMap()  Boat lat="+boatLat+"  showLayLines="+showLaylines);
            if (showLaylines) {
                // compute and plot the laylines from the Boat to the Mark
                // @return double[] array with [Latitude, Longitude, initial tack (0=stbd) (1=port), Distance, Heading]
                Laylines = NavigationTools.optimumLaylines(boatLat, boatLon, markerLat, markerLon, TWD, mCourse, TackGybe, tack);
                if (Laylines[0] != Double.NaN && Laylines[1] != Double.NaN) {
                    tack_position = new LatLng(Laylines[0], Laylines[1]);
                    tack_position_ok = true;

                    // only add a Tack marker if the tack is more than 0.03 nm (approx. 55m) from both of the course marks.
                    toMark = NavigationTools.MarkDistanceBearing(boat.latitude, boat.longitude, Laylines[0], Laylines[1]);
                    delta = toMark[0];
                    toMark = NavigationTools.MarkDistanceBearing(activeMarker.latitude, activeMarker.longitude, Laylines[0], Laylines[1]);
                    delta = delta + toMark[0];
                    if (delta > 0.03d) {
                        tmp = "in "+appContext.getString(R.string.DF1, Laylines[3])+" nm and at "+appContext.getString(R.string.Degrees, NavigationTools.TrueToMagnetic(Laylines[4],para.getDeclination()));
                        mark = mMap.addMarker(new MarkerOptions()
                                .position(tack_position)
                                .draggable(false)
                                .title("Tack")
                                .snippet(tmp)
                                .anchor(0.5f, 0.5f)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.tack_marker))
                                .alpha(0.7f));
                        mark.showInfoWindow();
                        markers.add(mark);
                    }
                    // end of tack marker add
                }

                if (Laylines[2] == 0.0d) {
                    if (mCourse == 180.0f) {
                        boatResource = R.drawable.boatgreen_black_reach;
                    } else {
                        boatResource = R.drawable.boatgreen_black;
                    }
                    if (tack_position_ok) {
                        polylines.add(mMap.addPolyline(new PolylineOptions()
                                .add(boat, tack_position)
                                .width(12)
                                .color(Color.GREEN)));
                        polylines.add(mMap.addPolyline(new PolylineOptions()
                                .add(tack_position, activeMarker)
                                .width(12)
                                .color(Color.RED)));
                    }
                } else {
                    if (mCourse == 180.0f) {
                        boatResource = R.drawable.boatred_black_reach;
                    } else {
                        boatResource = R.drawable.boatred_black;
                    }
                    if (tack_position_ok) {
                        polylines.add(mMap.addPolyline(new PolylineOptions()
                                .add(boat, tack_position)
                                .width(12)
                                .color(Color.RED)));
                        polylines.add(mMap.addPolyline(new PolylineOptions()
                                .add(tack_position, activeMarker)
                                .width(12)
                                .color(Color.GREEN)));
                    }
                }

                // add a Wind-arrow to the Laylines
                double[] windMarker = NavigationTools.withDistanceBearingToPosition(boatLat, boatLon, 1.1, TWD);
                polylines.add(mMap.addPolyline(new PolylineOptions()
                        .add(boat, new LatLng(windMarker[0], windMarker[1]))
                        .width(8)
                        .color(Color.BLACK)));

                // compute and plot the laylines from the Leeward to the Windward Mark for windward
                // course or from Windward to Leeward Mark for downwind course using the average
                // TWD over the past "history" TWD readings
                if (mCourse == 180.0d) {
                    Laylines = NavigationTools.optimumLaylines(para.getWindwardLat(), para.getWindwardLon(), para.getLeewardLat(), para.getLeewardLon(), TWD, mCourse, TackGybe, tack);
                    start = new LatLng(para.getWindwardLat(), para.getWindwardLon());
                    end = new LatLng(para.getLeewardLat(), para.getLeewardLon());
                } else {
                    Laylines = NavigationTools.optimumLaylines(para.getLeewardLat(), para.getLeewardLon(), para.getWindwardLat(), para.getWindwardLon(), TWD, mCourse, TackGybe, tack);
                    start = new LatLng(para.getLeewardLat(), para.getLeewardLon());
                    end   = new LatLng(para.getWindwardLat(), para.getWindwardLon());
                }
                if (Laylines[0] != Double.NaN && Laylines[1] != Double.NaN) {
                    tack_position = new LatLng(Laylines[0], Laylines[1]);
                    tack_position_ok = true;
                } else {
                    tack_position_ok = false;
                }
                if (Laylines[2] == 0.0d && tack_position_ok) {
                    dashedLine(start, tack_position, Color.GREEN, zoomLevel);
                    dashedLine(tack_position, end, Color.RED, zoomLevel);
                }
                if (Laylines[2] == 1.0d && tack_position_ok) {
                    dashedLine(start, tack_position, Color.RED, zoomLevel);
                    dashedLine(tack_position, end, Color.GREEN, zoomLevel);
                }
            }

            // add Leeward Marker
            if (para.getLeewardFlag()) {
                tmp = " at "+NavigationTools.PositionDegreeToString(leewardMarker.latitude, true);
                tmp += " and "+NavigationTools.PositionDegreeToString(leewardMarker.longitude, false);
                mark = mMap.addMarker(new MarkerOptions()
                        .position(leewardMarker)
                        .draggable(false)
                        .title("Leeward Mark")
                        .snippet(tmp)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                markers.add(mark);
            }
            // end of the "Fragment_RaceInfo elements display
            //
        }

        // add Windward Mark
        tmp = " at "+NavigationTools.PositionDegreeToString(windwardMarker.latitude, true);
        tmp += " and "+NavigationTools.PositionDegreeToString(windwardMarker.longitude, false);
        mark = mMap.addMarker(new MarkerOptions()
                .position(windwardMarker)
                .draggable(true)
                .title("Windward Mark")
                .snippet(tmp)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        markers.add(mark);
        wwdMarker = mark;
        //Log.d(LOG_TAG, "DisplayGoogleMap() Boat lat="+boat.latitude+"  Lon="+boat.longitude);
        //Log.d(LOG_TAG, "DisplayGoogleMap() WWD Lat ="+windwardMarker.latitude+"  Lon="+windwardMarker.longitude);

        // add Boat with rotation and boatResource set either for StartSequence or RaceInfo context
        if (face_into_wind && para.getSailtimerStatus()) {
            // map faces into the wind. Thus we need angle between the wind and the boat
            rotation = (float)NavigationTools.HeadingDelta(NavigationTools.TWD_shortAVG, para.getAvgCOG());
        } else {
            // map faces straight north
            rotation = (float)para.getAvgCOG();
        }
        //Log.d(LOG_TAG, "Boat rotation relative to map="+rotation);

        mark = mMap.addMarker(new MarkerOptions()
                .position(boat)
                .anchor(0.5f, 0.0f)
                .rotation(rotation)
                .icon(BitmapDescriptorFactory.fromResource(boatResource))
                .alpha(1.0f));
        markers.add(mark);
        //Log.d(LOG_TAG, "Map bearing after boat addition to map="+camera.bearing);

        if (WINDEX >= 1) {
            String msg = "TWD="+appContext.getString(R.string.Degrees, para.getTWD());
            msg += "  TWS="+appContext.getString(R.string.DF1, para.getTWS());
            if ( !Double.isNaN(favored) ) {
                msg += "  Favored: "+NavigationTools.favoredPinString(favored);
            }
            outputHeaderBar.setText(msg);

            if (para.getSailtimerStatus()) {
                outputHeaderBar.setTextColor(ContextCompat.getColorStateList(appContext, R.color.WHITE));
            } else {
                outputHeaderBar.setTextColor(ContextCompat.getColorStateList(appContext, R.color.RED));
            }
        }

        if (animation) {
            // when the MapUpdater is running, animation gets set to true after this first display of
            // the map.  In that case we want to animate the camera update as the boat moves along
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(camera));

        } else {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera));
        }

    }

    void dashedLine(LatLng start, LatLng end, int color, int zoomLevel) {
        double startLAT = start.latitude;
        double startLON = start.longitude;
        double endLAT = end.latitude;
        double endLON = end.longitude;
        double delta = 0.02; // segment size in mn at zoom level 13 = DEFAULT_ZOOM_LEVEL

        double scale;
        // adjust delta by the zoom level.  Each increase in the Maps zoom level halves the area
        // shown.  Thus for smaller zoom levels than 13 => delta * 2^(zoomLevel-13)
        //              for larger zoom levels than 13  => delta * 2^(1/(13-zoomLevel))
        if (zoomLevel > DEFAULT_ZOOM_LEVEL) {
            scale = (double)(zoomLevel - DEFAULT_ZOOM_LEVEL);
            delta = delta / Math.pow(2.0d, scale);
        }
        if (zoomLevel < DEFAULT_ZOOM_LEVEL) {
            scale = (double)(DEFAULT_ZOOM_LEVEL - zoomLevel);
            delta = delta * Math.pow(2.0d, scale);
        }

        /* Get distance between current and next point */
        toMark = NavigationTools.MarkDistanceBearing(startLAT, startLON, endLAT, endLON);

        /* If distance is less than 0.001 nm */
        if (toMark[0] < delta*2.0d) {
            polylines.add(mMap.addPolyline(new PolylineOptions()
                    .add(start)
                    .add(end)
                    .color(color)));
            return;
        } else {
            /* Get how many divisions to make of this line */
            int countOfDivisions = (int) (toMark[0]/(delta*3.0d));

            /* Get difference to add per lat/lng */
            double latdiff = (end.latitude - start.latitude) / countOfDivisions;
            double lngdiff = (end.longitude - start.longitude) / countOfDivisions;

            /* initialize the starting point of the dashed line */
            LatLng lastKnowLatLng = new LatLng(start.latitude, start.longitude);
            for (int j = 0; j < countOfDivisions; j++) {
                /* line segment length is 2/3 of the total length of dashed line (line plus space) */
                LatLng segmentLatLng = new LatLng(lastKnowLatLng.latitude + latdiff*0.666667d, lastKnowLatLng.longitude + lngdiff*0.666667d);
                polylines.add(mMap.addPolyline(new PolylineOptions()
                        .add(lastKnowLatLng)
                        .add(segmentLatLng)
                        .color(color)));
                lastKnowLatLng = new LatLng(lastKnowLatLng.latitude + latdiff, lastKnowLatLng.longitude + lngdiff);
            }
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
                para.setWindwardFlag(false);
                para.setWindwardRace(false);
                para.setWindwardLat(Double.NaN);
                para.setWindwardLon(Double.NaN);
                WWDmark.setVisibility(view.GONE);
                if (wwdMarker != null) {
                    wwdMarker.remove();
                }
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
     * OK_dialog_message Method displays a messages and asks the user to
     * acknowledge this message before hiding the dialog box and returning to the UI
     */
    public void OK_dialog_message(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                this.getActivity(), android.R.style.Theme_Holo_Dialog));
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Go ahead quit this dialog after the user has seen/acknowledge the msg
                // TODO - done!
            }
        });
        builder.setTitle(title);
        builder.setIcon(R.mipmap.ic_launcher);
        AlertDialog alert=builder.create();
        alert.show();
    }

}
