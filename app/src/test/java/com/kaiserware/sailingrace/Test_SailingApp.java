package com.kaiserware.sailingrace;


import com.google.android.gms.maps.model.LatLng;
import org.junit.Test;
import java.util.TimeZone;
import static junit.framework.Assert.assertEquals;

/**
 * Created by Volker Petersen on 5/3/2016.
 * Testsuite for the SailingRace App
 */
public class Test_SailingApp {
    @Test
    public void test() throws Exception {
        double x, y, expected, result;
        double precision = 0.001d;
        final String LOG_TAG = Test_SailingApp.class.getSimpleName();

        GlobalParameters para = GlobalParameters.getInstance();

        // Testing of TWD LinkList
        while (!NavigationTools.TWD.isEmpty()) {
            NavigationTools.TWD.removeFirst();
        }
        NavigationTools.TWD.add(300.0d);
        assertEquals(NavigationTools.TWD.size(), 1);
        NavigationTools.TWD.add(330.0d);
        assertEquals("TWD size s/b 2", NavigationTools.TWD.size(), 2);
        assertEquals("TWD s/b 300", (double)NavigationTools.TWD.getFirst(), 300.0d);
        assertEquals(NavigationTools.TWD.get(0), 300.0d);
        assertEquals(NavigationTools.TWD.getLast(), 330.0d);
        NavigationTools.TWD.removeFirst();
        assertEquals(NavigationTools.TWD.getFirst(), 330.0d);
        assertEquals(NavigationTools.TWD.size(), 1);
        NavigationTools.TWD.add(310.0d);
        NavigationTools.TWD.add(320.0d);
        assertEquals(NavigationTools.TWD.size(), 3);
        NavigationTools.TWD.clear();
        assertEquals(NavigationTools.TWD.size(), 0);


        // Testing of NavigationTools.convertXYCoordinateToPolar()
        x = 0.70710678;
        y = 0.70710678;
        expected = 45.0d;
        result = NavigationTools.convertXYCoordinateToPolar(x, y);
        assertEquals(expected, result, precision);

        result = NavigationTools.convertXYCoordinateToPolar(0.0d, 0.0d);
        assertEquals(0.0d, result, precision);

        x = 0.342020;  // cos(rad(290))
        y = -0.939693; // sin(rad(290))
        expected = 290.0d;
        result = NavigationTools.convertXYCoordinateToPolar(x, y);
        assertEquals(expected, result, precision);

        result = NavigationTools.convertXYCoordinateToPolar(0.0d, y);
        assertEquals(270.0d, result, precision);

        x = -0.707107;  // cos(rad 225))
        y = -0.707107;  // sin(rad(225))
        expected = 225.0d;
        result = NavigationTools.convertXYCoordinateToPolar(x, y);
        assertEquals(expected, result, precision);

        result = NavigationTools.convertXYCoordinateToPolar(0.0d, -y);
        assertEquals(90.0d, result, precision);

        x = -0.060516;
        y = 0.998167;
        expected = 93.4694;
        result = NavigationTools.convertXYCoordinateToPolar(x, y);
        assertEquals(expected, result, precision);

        x = -0.844795d;
        y = -0.535090d;
        expected = 212.350d;
        result = NavigationTools.convertXYCoordinateToPolar(x, y);
        assertEquals(expected, result, precision);

        result = NavigationTools.convertXYCoordinateToPolar(0.00199d, 0.0020d);
        assertEquals(45.14359814423119d, result, precision);

        result = NavigationTools.convertXYCoordinateToPolar(-0.0019d, 1.0d);
        assertEquals(90.10885d, result, precision);

        result = NavigationTools.convertXYCoordinateToPolar(-1.0d, 0.00001d);
        assertEquals(180.0d, result, precision);

        result = NavigationTools.convertXYCoordinateToPolar(-0.000019d, -1.0d);
        assertEquals(269.9989d, result, precision);

        // Testing of the True Wind calculations
        //
        double[] results = new double[3];
        results = NavigationTools.calc_TWA_TWD_TWS(7.0d, 27.0d, 207.0d, 20.9d);
        assertEquals(39.22871d, results[0], precision);
        assertEquals(219.2287d, results[1], precision);
        assertEquals(15.0034d, results[2], precision);
        double awa = NavigationTools.TWAtoAWA(7.0d, results[0], results[2]);
        assertEquals(27.d, awa, precision);

        results = NavigationTools.calc_TWA_TWD_TWS(5.0d, 30.0d, 150.0d, 10.0d);
        assertEquals(53.79398d, results[0], precision);
        assertEquals(173.7940d, results[1], precision);
        assertEquals(6.1966d, results[2], precision);
        awa = NavigationTools.TWAtoAWA(5.0d, results[0], results[2]);
        assertEquals(30.0d, awa, precision);

        results = NavigationTools.calc_TWA_TWD_TWS(5.0d, -50.0d, 50.0d, 10.0d);
        assertEquals(-79.44146d, results[0], precision);
        assertEquals(20.5586d, results[1], precision);
        assertEquals(7.7924d, results[2], precision);
        awa = NavigationTools.TWAtoAWA(5.0d, results[0], results[2]);
        assertEquals(-50.0d, awa, precision);

        results = NavigationTools.calc_TWA_TWD_TWS(5.5d, 150.0d, 270.0d, 10.0d);
        assertEquals(160.55182d, results[0], precision);
        assertEquals(280.5518d, results[1], precision);
        assertEquals(15.01708d, results[2], precision);
        awa = NavigationTools.TWAtoAWA(5.5d, results[0], results[2]);
        assertEquals(150.0d, awa, precision);

        results = NavigationTools.calc_TWA_TWD_TWS(5.5d, -150.0d, 120.0d, 10.0d);
        assertEquals(-160.55182d, results[0], precision);
        assertEquals(109.4482d, results[1], precision);
        assertEquals(15.01708d, results[2], precision);
        awa = NavigationTools.TWAtoAWA(5.5d, results[0], results[2]);
        assertEquals(-150.0d, awa, precision);

        // Testing of the determination of the race course quadrant
        //
        para.setWindwardLat(55.0d);
        para.setWindwardLon(-93.4d);
        para.setCommitteeLat(54.5d);
        para.setCommitteeLon(-93.0d);
        double[] h = NavigationTools.MarkDistanceBearing(para.getWindwardLat(), para.getWindwardLon(), para.getCommitteeLat(), para.getCommitteeLon());
        h = NavigationTools.withDistanceBearingToPosition(para.getWindwardLat(), para.getWindwardLon(), h[0]/2.0d, h[1]);
        assertEquals((para.getWindwardLat()+para.getCommitteeLat())/2.0d, h[0], precision);
        assertEquals((para.getWindwardLon()+para.getCommitteeLon())/2.0d+0.0003d, h[1], precision);

        // let's find out which Quadrant we're are currently sailing in
        // Q1
        double[] q = NavigationTools.MarkDistanceBearing(h[0], h[1], 54.8d, -93.1d);
        //assertEquals(45.0d, q[1], precision);
        assertEquals( (q[1]>=0.0d && q[1]<=90.0d), true);

        // Q2
        q = NavigationTools.MarkDistanceBearing(h[0], h[1], 54.6d, -93.1d);
        //assertEquals(135.0d, q[1], precision);
        assertEquals( (q[1]>90.0d && q[1]<=180.0d), true);

        // Q3
        q = NavigationTools.MarkDistanceBearing(h[0], h[1], 54.6d, -93.5d);
        //assertEquals(205.0d, q[1], precision);
        assertEquals( (q[1]>180.0d && q[1]<=270.0d), true);

        // Q4
        q = NavigationTools.MarkDistanceBearing(h[0], h[1], 54.8d, -93.5d);
        //assertEquals(300.0d, q[1], precision);
        assertEquals( (q[1]>270.0d && q[1]<360.0d), true);

        // Testing of the fifoQueue
        //
        fifoQueueDouble COGfifo = new fifoQueueDouble(3);
        COGfifo.add(10.d);
        COGfifo.add(350.d);
        COGfifo.add(340.d);
        COGfifo.add(20.d);
        assertEquals(350.0d, COGfifo.getFirst(), precision);
        assertEquals(20.0d, COGfifo.getLast(), precision);
        assertEquals(340.0d, COGfifo.getElement(1), precision);
        assertEquals(356.53056d, COGfifo.averageCompassDirection(), precision);

        // test is division by zero is caught correctly
        COGfifo.add(80.d);
        COGfifo.add(90.d);
        COGfifo.add(100.d);
        assertEquals(90.0d, COGfifo.averageCompassDirection(), precision);

        COGfifo.add(260.d);
        COGfifo.add(270.d);
        COGfifo.add(280.d);
        assertEquals(270.0d, COGfifo.averageCompassDirection(), precision);

        COGfifo.add(320.d);
        COGfifo.add(330.d);
        COGfifo.add(310.d);
        assertEquals(320.0d, COGfifo.averageCompassDirection(), precision);

        // Testing of Race_ID conversions
        TimeZone TZ = TimeZone.getTimeZone("UTC");  // set UTC as the default timezone

        String raceID = "1463616002";
        Long race = Long.parseLong(raceID) * (long)1000;
        String name = NavigationTools.getDateString(race, "yyyy-MM-dd ss");
        assertEquals("2016-05-18 02", name);

        // Testing of the Navigation Tools conversion from True to Magnetic compass readings
        double declination = -13.0d;
        double trueAngle = 357.0d;
        assertEquals(10.0d, NavigationTools.TrueToMagnetic(trueAngle, declination), precision);
        declination = -13.0d;
        trueAngle = 3.5d;  // rounding to full degrees
        assertEquals(17.0d, NavigationTools.TrueToMagnetic(trueAngle, declination), precision);
        declination = 10.0d;
        trueAngle = 5.0d;
        assertEquals(355.0d, NavigationTools.TrueToMagnetic(trueAngle, declination), precision);
        declination = 15.0d;
        trueAngle = 194.0d;
        assertEquals(179.0d, NavigationTools.TrueToMagnetic(trueAngle, declination), precision);

        // Testing of the Navigation Tools heading/distance calculations
        results = NavigationTools.MarkDistanceBearing(30.4d, -81.5d, 29.1d, -80.2d);
        assertEquals(103.36289d, results[0], precision);
        assertEquals("Great Circle Bearing", 138.711126199, results[1], precision);
        results = NavigationTools.MarkDistanceBearing(30.4d, -81.5d, 29.1d, -82.0d);
        assertEquals(82.28886d, results[0], precision);
        assertEquals("Great Circle Bearing", 198.5898719, results[1], precision);
        results = NavigationTools.MarkDistanceBearing(3.0d, -10.0d, 6.0d, -10.0d);
        assertEquals(180.12137d, results[0], precision);
        assertEquals("Great Circle Bearing", 0.0d, results[1], precision);
        results = NavigationTools.MarkDistanceBearing(44.63630626204924d, -93.3544007186544d, 44.62666894009293d, -93.3626308143815d);
        assertEquals(0.67710318105d, results[0], precision);
        assertEquals("Great Circle Bearing", 211.291092905d, results[1], precision);

        results = NavigationTools.withDistanceBearingToPosition(30.4d, -81.5d, 103.36289d, 139.03653d);
        assertEquals(29.09365d, results[0], precision);
        assertEquals(-80.20851d, results[1], precision);
        results = NavigationTools.withDistanceBearingToPosition(30.4d, -81.5d, 82.28886d, 198.46472d);
        assertEquals(29.1d, results[0], precision);
        assertEquals(-81.9967d, results[1], precision);
        results = NavigationTools.withDistanceBearingToPosition(7.0d, 10.0d, 240.0d, 180.0d);
        assertEquals(3.0027, results[0], precision);
        assertEquals(10.0d, results[1], precision);


        // Testing of the Navigation Tools time (sec) to min:sec string
        double time = 91.2;
        assertEquals("1:31", NavigationTools.min_sec(time));
        time = 91.9;
        assertEquals("1:32", NavigationTools.min_sec(time));
        time = 1.9;
        assertEquals("0:02", NavigationTools.min_sec(time));
        time = 1.43;
        assertEquals("0:01", NavigationTools.min_sec(time));

        // testing compass angle rounding for the Compass Angle output fuction
        assertEquals(0.0d, NavigationTools.TrueToMagnetic(360.0, 0.0));
        assertEquals(359.0d, NavigationTools.TrueToMagnetic(359.499, 0.0));
        assertEquals(359.d, NavigationTools.TrueToMagnetic(359.444, 0.0));
        assertEquals(0.0d, NavigationTools.TrueToMagnetic(359.9000, 0.0));
        assertEquals(0.0d, NavigationTools.TrueToMagnetic(359.9900, 0.0));
        assertEquals(0.0d, NavigationTools.TrueToMagnetic(359.9990, 0.0));
        assertEquals(0.0d, NavigationTools.TrueToMagnetic(359.9999, 0.0));

        // this assertion needs today's data in line 125
        /*
        Calendar now = Calendar.getInstance(TZ);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 10);
        long today = (long)(now.getTimeInMillis());
        name = NavigationTools.getDateString(today, "yyyy-MM-dd ss");
        assertEquals("2016-05-19 00", name);
        */

        LatLng Boat = new LatLng(44.63138d, -93.36925d);
        NavigationTools.TWD_shortAVG = 135.0d;
        para.setCOG(NavigationTools.fixAngle(NavigationTools.TWD_shortAVG-45.0d));
        para.setAvgCOG(para.getCOG());
        para.setAvgSOG(4.0);
        double[] toDistance = NavigationTools.withDistanceBearingToPosition(Boat.latitude, Boat.longitude, 0.7d, NavigationTools.fixAngle(para.getAvgCOG()-25.0d));
        LatLng Committee = new LatLng(toDistance[0], toDistance[1]);

        toDistance = NavigationTools.withDistanceBearingToPosition(Boat.latitude, Boat.longitude, 0.4d, NavigationTools.fixAngle(para.getAvgCOG()+45.0d));
        LatLng Pin = new LatLng(toDistance[0], toDistance[1]);

        //toDistance = NavigationTools.withDistanceBearingToPosition(Boat.latitude, Boat.longitude, 1.0d, para.getAvgCOG());
        //LatLng headingTo = new LatLng(toDistance[0], toDistance[1]);

        double[] intersection_data = NavigationTools.LineIntersection(Boat, Committee, Pin, para.getAvgCOG(), para.getAvgSOG());
        assertEquals(0.45463908862733077d, intersection_data[0], precision);
        assertEquals(409.1751797645977d, intersection_data[1], precision);
        assertEquals(44.631377826959614d, intersection_data[2], precision);
        assertEquals(-93.35860951159047d, intersection_data[3], precision);

        Fragment_RaceInfo.screenUpdates = 100;
        Fragment_RaceInfo.above_mean = true;
        double sample_rate = (double) Fragment_RaceInfo.screenUpdates;
        int size = 2000;
        FastFourierTransformation fft = new FastFourierTransformation(size);
        fft.create_dataset(sample_rate, 5.0d, size);
        double[][] xy = fft.fetch_data(NavigationTools.TWD, NavigationTools.TWD_longAVG);
        fft.fft(xy[0], xy[1]);
        double freq = fft.frequency(xy[0], sample_rate);

        assertEquals("size after FFT", 2048, fft.nfft);
        assertEquals("size after FFT", size, NavigationTools.TWD.size());

        NavigationTools.fft_wind_frequency(sample_rate);

        int hrs = (int)(NavigationTools.wind_period / 3600.0d);
        int min = (int)(NavigationTools.wind_period / 60.0d) - hrs*60;

        assertEquals("FFT frequency", 4.98046875d, freq, precision);
        // at 1024 datapoints we have 51 cycles at 5HZ and 100 samples/sec.  Thus with more or
        // less datapoints the number of cycles changes proportionally.
        assertEquals("FFT # of cycles", 51.0d*fft.nfft/1024, NavigationTools.wind_cycles, precision);

        //assertEquals("Last test fails", 5.0d, 0.0d, PRECISION);
    }
}
