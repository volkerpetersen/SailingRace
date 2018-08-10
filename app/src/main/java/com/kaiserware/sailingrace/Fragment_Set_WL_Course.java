package com.kaiserware.sailingrace;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class Fragment_Set_WL_Course extends Fragment {
    private Context appContext;
    private View view;
    private GlobalParameters para;
    private TextView TWD;
    private TextView StartLine;
    private TextView CourseBearing;
    private TextView CourseLength;
    private TextView CourseOffset;
    private TextView NumberOfLegs;
    private RadioButton RadioLeft;
    private RadioButton RadioRight;
    private Button WL_Course_Set;
    public static final String LOG_TAG = Fragment_Set_WL_Course.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        appContext = getActivity().getApplicationContext();

        view = inflater.inflate(R.layout.fragment_wl_setup, container, false);

        // initialize the Global Parameter singleton class
        para = GlobalParameters.getInstance();

        TWD = (TextView) view.findViewById(R.id.TWD);
        StartLine = (TextView) view.findViewById(R.id.StartLine);
        CourseBearing = (TextView) view.findViewById(R.id.CourseBearing);
        CourseLength = (TextView) view.findViewById(R.id.CourseLength);
        CourseOffset = (TextView) view.findViewById(R.id.CourseOffset);
        NumberOfLegs = (TextView) view.findViewById(R.id.numberOfLegs);
        RadioLeft = (RadioButton) view.findViewById(R.id.radioLEFT);
        RadioRight = (RadioButton) view.findViewById(R.id.radioRIGHT);
        WL_Course_Set = (Button) view.findViewById(R.id.wl_OK);
        WL_Course_Set.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                raceCourseCompute();
                getActivity().onBackPressed();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeRaceCourseSetup();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    void initializeRaceCourseSetup() {
        // initialize the race course parameters and allow the user to update the race course parameters
        String tmp;
        String cd;
        double miles_to_m = 1852.0;
        double defaultStartlineLength = 0.10799; // default length of starting line in nm = 200 meters
        double courseBearing;
        double[] results = new double[2];
        TWD.setText(getString(R.string.Degrees, para.getTWD()));

        if (Double.isNaN(para.getCommitteeLat()) || Double.isNaN(para.getCommitteeLon()) || Double.isNaN(para.getPinLat()) || Double.isNaN(para.getPinLon()) ) {
            results[0] = defaultStartlineLength;
            results[1] = NavigationTools.fixAngle(para.getTWD()-90.0);
        } else {
            results = NavigationTools.MarkDistanceBearing(para.getCommitteeLat(),
                    para.getCommitteeLon(), para.getPinLat(), para.getPinLon());
            if (NavigationTools.isZero(results[0]) && NavigationTools.isZero(results[1])) {
                results[0] = defaultStartlineLength;
                results[1] = NavigationTools.fixAngle(para.getTWD()-90.0);
            }
        }

        if (Double.isNaN(results[0])) {
            tmp = "L=n/a  ";
        } else {
            tmp = "L="+getString(R.string.DF0, results[0]*miles_to_m)+"m  ";
        }

        if (Double.isNaN(results[1])) {
            tmp += "B=n/a";
            cd = "0";
        } else {
            tmp += "B=" + getString(R.string.Degrees, results[1]);
            courseBearing = NavigationTools.fixAngle(results[1]+90.0);
            para.courseBearing = courseBearing;
            cd = getString(R.string.DF0, courseBearing);
        }

        StartLine.setText(tmp);
        CourseBearing.setText(cd);

        if (Double.isNaN(para.courseLength)) {
            para.courseLength = (double) SailingRacePreferences.FetchPreferenceValue("key_CourseDistance", appContext);
            para.courseLength = para.courseLength / miles_to_m;
        }
        CourseLength.setText(getString(R.string.DF2, para.courseLength));

        if (Double.isNaN(para.courseOffset)) {
            para.courseOffset = (double) SailingRacePreferences.FetchPreferenceValue("key_LeftRightDistance", appContext);
            para.courseOffset = para.courseLength * para.courseOffset / 100.0;
        }
        //Log.d(LOG_TAG, "courseOffset Pref: "+para.courseOffset);
        CourseOffset.setText(getString(R.string.DF2, para.courseOffset));

        if (para.numberOfLegs == 0) {
            para.numberOfLegs = SailingRacePreferences.FetchPreferenceValue("key_NumberOfLegs", appContext);
        }
        //Log.d(LOG_TAG, "# of Legs Pref: "+para.numberOfLegs);
        NumberOfLegs.setText(String.valueOf(para.numberOfLegs));

        return;
    }

    void raceCourseCompute() {
        // fetch the user input and compute the final race course parameters
        double[] position = new double[2];
        double offset;

        // fetch the latest values from the W/L Course setup fragment display
        try {
            para.courseBearing = Double.parseDouble(CourseBearing.getText().toString());
            para.courseLength = Double.parseDouble(CourseLength.getText().toString());
            para.courseOffset = Double.parseDouble(CourseOffset.getText().toString());
        } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "error converting string to double - "+e.getMessage());
        }
        offset = NavigationTools.fixAngle(para.courseBearing-90.0);

        position = NavigationTools.withDistanceBearingToPosition(para.getCommitteeLat(), para.getCommitteeLon(), para.courseLength, para.courseBearing);
        if (RadioLeft.isChecked()) {
            position = NavigationTools.withDistanceBearingToPosition(position[0], position[1], para.courseOffset, offset);
        }

        para.setWindwardLat(position[0]);
        para.setWindwardLon(position[1]);
        para.setWindwardFlag(true);

        if (RadioLeft.isChecked()) {
            position = NavigationTools.withDistanceBearingToPosition(para.getPinLat(), para.getPinLon(), para.courseOffset, offset);
        } else {
            position[0] = para.getCommitteeLat();
            position[1] = para.getCommitteeLon();
        }
        para.setLeewardLat(position[0]);
        para.setLeewardLon(position[1]);
        para.setLeewardFlag(true);

        position = NavigationTools.withDistanceBearingToPosition(para.getPinLat(), para.getPinLon(), para.courseLength/2.0, para.courseBearing);
        para.setCenterLat(position[0]);
        para.setCenterLon(position[1]);

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Windward  Lat:"+getString(R.string.DF4, para.getWindwardLat())+" Lon: "+getString(R.string.DF4, para.getWindwardLon()));
            Log.d(LOG_TAG, "Leeward   Lat:"+getString(R.string.DF4, para.getLeewardLat())+" Lon: "+getString(R.string.DF4, para.getLeewardLon()));
            Log.d(LOG_TAG, "Committee Lat:"+getString(R.string.DF4, para.getCommitteeLat())+" Lon: "+getString(R.string.DF4, para.getCommitteeLon()));
            Log.d(LOG_TAG, "Pin       Lat:"+getString(R.string.DF4, para.getPinLat())+" Lon: "+getString(R.string.DF4, para.getPinLon()));
            Log.d(LOG_TAG, "Course Bearing:"+getString(R.string.Degrees, para.courseBearing)+" Offset: "+getString(R.string.Degrees, offset));
        }

        return;
    }

}
