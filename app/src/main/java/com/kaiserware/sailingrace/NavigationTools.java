package com.kaiserware.sailingrace;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;
import static java.lang.Math.abs;

/**
 * Helper Class which various navigation tools / calculation.
 * All angles are assumed to be True North for all calculations.  For display purposes
 * this program uses Magnetic North figures.  Conversion between True and Magnetic North
 * is based on the declination figure provided by the GPS class.
 *
 * Created by Volker Petersen November 2015.
 */
public class NavigationTools {
    public static final double km_nm = 0.539956803d;      /* km to nm conversion factor - 1,852m in nm*/
    private static final double RADIUS = 6371.0*km_nm;	   /* Earth RADIUS = 3,440.065 nm */
    private static final double THREESIXTY = 360.0d;       /* 360 degrees in Compass Rose */
    private static final double ZERO = 0.0d;               /* double value zero */
    public static final int MIN_FFT_DATA_POINTS = 600;
    public static final double PRECISION = 0.00000001d;
    private static final DecimalFormat df3 = new DecimalFormat("#0.000");
    private static final DecimalFormat twoDigits = new DecimalFormat("00");
    public static long raceID;
    public static LinkedList<Double> TWD = new LinkedList<Double>();
    public static LinkedList<Double> TWD_short_AVG = new LinkedList<Double>();
    public static LinkedList<Double> TWD_long_AVG = new LinkedList<Double>();
    public static LinkedList<Double> TWD_Std_Dev = new LinkedList<Double>();
    public static boolean above_mean;            // True is the current wind oscillation is above long range mean
    public static int wind_period;               // duration of the current wind oscillation
    public static double wind_frequency;         // computed wind frequency using FFT in fft_wind_frequency()
    public static double wind_cycles;            // number of wind cycles in current dataset TWD
    public static double wind_sine_start;        // x-axis offset of the calculated wind sine wave
    public static FastFourierTransformation fft; // class object to the FastFourierTransformation
    public static double TWD_StdDev;
    public static double TWD_longAVG;
    public static double TWD_shortAVG;
    public static int longAvg;   // set from preferences in AppBackgroundServices
    public static int shortAvg;  // set from preferences in AppBackgroundServices
    static final String LOG_TAG = NavigationTools.class.getSimpleName();

    /**
     * Method to compute an Angle between 0 and 359.99 degrees (full compass rose)
     * @param angle
     * @return double
     */
    public static double fixAngle(double angle) {
        return Mod(angle, THREESIXTY);
    }

    /**
     * Method to convert a X/Y coordinate value into a Polar Coordinate System value with radius = 1.0
     * Note: Excel uses atan2(x,y) whereas Java uses atan2(y,x) !!!
     * @param x
     * @param y
     * @return double (in degrees for full compass rose (0 - 359.99)
     */
    public static double convertXYCoordinateToPolar(double x, double y) {
        // check for y == 0.0 and x == 0 so that we don't have a division by zero error
        // and correctly find due West direction
        if (abs(x) < PRECISION && abs(y) < PRECISION) {
            return ZERO;
        }
        return (THREESIXTY+Math.toDegrees(Math.atan2(y, x))) % THREESIXTY;
    }

    /**
     * Method to check if a double is zero within the PRECISION
     * @param x
     * @return boolean true if zero
     */
    public static boolean isZero(double x) {
        if (abs(x) < PRECISION)
            return true;
        else
            return false;
    }

    /**
     * Method to convert a True North Compass reading into a Magnetic North compass reading
     * Magnetic Bearing - Magnetic Declination = True Bearing
     * MB = TB + MD
     *
     * @param trueAngle True North compass angle
     * @param declination Declination at the current location
     * @return Magnetic North compass angle 0 to 359.999
     */
    public static double TrueToMagnetic(double trueAngle, double declination) {
        return Mod(Math.round(trueAngle+declination), THREESIXTY);
    }

    /**
     * Method to compute the difference between two angles going from angle "From" to angle "To"
     * Clockwise => Positive values 0-180,
     * Counter-Clockwise => Negative values -0 to -179.999999
     *
     * @param From
     * @param To
     * @return double
     */
    public static double HeadingDelta(double From, double To) {
        if (From > THREESIXTY || From < ZERO) {
            From = fixAngle(From);
        }
        if (To > THREESIXTY ||  To < ZERO) {
            To = fixAngle(To);
        }

        double diff = To - From;
        double absDiff = abs(diff);

        if (absDiff <= 180.0d) {
            if (absDiff == 180.0d) {
                diff = absDiff;
            }
            return diff;
        } else if (To > From) {
            return (absDiff - THREESIXTY);
        } else {
            return (THREESIXTY - absDiff);
        }
    }

    /**
     * Method to compute the short and long average value and Std Dev of all the values in the
     * LinkedList TWD of length 'longQueue'.  We use the previous TWD_longAVG for the calculation
     * of the Std Dev.
     *
     * Test implementation in Excel 'SailingRace_Math' Tab 'Avg_Wind_Direction' and 'StdDev'
     *
     * @param twd    - latest TWD reading
     *
     * @return flag  - true if the twd is within +/- 3.0*StdDev (99% of all observations), false otherwise
     */
    public static boolean calc_AVG_StdDev(double twd) {
        boolean flag = true;
        int n, size, short_ctr;
        double x_long, y_long, x_short, y_short, error, delta;
        DecimalFormat df4 = new DecimalFormat("#0.0000");

        //long startTime = System.currentTimeMillis();

        size = TWD.size();
        if(size < 2) {
            TWD_longAVG = twd;
            TWD_shortAVG = twd;
            TWD_StdDev = 5.0d;   // default value to make sure we don't error out in the beginning
        } else {
            x_long = 0.0d;
            y_long = 0.0d;
            x_short = 0.0d;
            y_short = 0.0d;
            error = 0.0d;

            if (size-shortAvg > 0) {
                short_ctr = size-shortAvg;
            } else {
                short_ctr = 0;
            }

            for(n=0; n<size; n++) {
                x_long += Math.cos(Math.toRadians(NavigationTools.TWD.get(n)));
                y_long += Math.sin(Math.toRadians(NavigationTools.TWD.get(n)));
                delta = HeadingDelta(NavigationTools.TWD.get(n), TWD_longAVG);
                error += delta*delta;
                if (n >= short_ctr) {
                    x_short += Math.cos(Math.toRadians(NavigationTools.TWD.get(n)));
                    y_short += Math.sin(Math.toRadians(NavigationTools.TWD.get(n)));
                }
            }
            x_long = x_long/((double) size);
            y_long = y_long/((double) size);
            TWD_longAVG = NavigationTools.convertXYCoordinateToPolar(x_long, y_long);
            TWD_StdDev = Math.sqrt(error / (double)size);

            if (short_ctr > 0) {
                x_short = x_short/((double) shortAvg);
                y_short = y_short/((double) shortAvg);
                TWD_shortAVG = NavigationTools.convertXYCoordinateToPolar(x_short, y_short);
            } else {
                TWD_shortAVG = TWD_longAVG;
            }

            // true for 99.0% of all data points (+/- 3 Std Dev)
            flag = abs(HeadingDelta(twd, TWD_longAVG)) < (3.0d * TWD_StdDev);
        }

        /*
        long elapsedTime = System.currentTimeMillis()-startTime;
        if (size < 5) {
            Log.d(LOG_TAG, "AVG_StdDev() 1 pass-elapsed millis=" +elapsedTime + "   for " + size + " data points.   Flag=" + flag);
            Log.d(LOG_TAG, "AVG_StdDev() 1 TWD=" + df4.format(twd) + "  Avg=" + df4.format(TWD_longAVG) + "  StdDev=" + df4.format(TWD_StdDev));
        }
        // 'longQueue=900 requires an avg of 15 milli-seconds to compute
        */

        return flag;
    }

    /**
     * Method to compute the intersection of the current course with the starting line.
     * The starting line is defined as the line between the Committee Boat and Pin locations.
     * Our boat is at location "Boat" traveling with Course "cog" and Speed "sog".
     * With the given intersection and speed the method than computes and returns the Time to Line
     * at the current course/speed (in seconds).  It returns NaN if there is no line intersection found.
     *
     * see: http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect
     *
     * @param Boat LatLng of the race boat
     * @param Committee LatLng of the Committe boat end of the start line
     * @param Pin LatLng of the Pin end of the start line
     * @param cog heading of the boat
     * @param sog speed of the boat
     * @return array a with distance, time (sec), lat, lon to/of intersection or
     *         NaN if the two line (course of the boat and start line) don't intersect
     */
    public static double[] LineIntersection(LatLng Boat, LatLng Committee, LatLng Pin, double cog, double sog) {
        double[] BoatEnd;
        double[] s = new double[2];
        double[] r = new double[2];
        double[] a = new double[2];
        double[] intersection = new double[4];
        double a0, a1, a2, t, u, lat, lon;

        // compute the other end of the vector from the boat to "somewhere" far enough away to make
        // sure this vector will cross the starting line (when the current cog aims at the starting)
        // line). Vector length of 2nm should be more than sufficient for the application.
        BoatEnd = withDistanceBearingToPosition(Boat.latitude, Boat.longitude, 2.0d, cog);
        s[0] = Pin.latitude - Committee.latitude;
        s[1] = Pin.longitude - Committee.longitude;
        r[0] = BoatEnd[0] - Boat.latitude;
        r[1] = BoatEnd[1] - Boat.longitude;

        // a = (q - p)
        // q is Committee Boat location, p is current Boat location
        a[0] = Committee.latitude-Boat.latitude;
        a[1] = Committee.longitude-Boat.longitude;

        // compute the vector cross products (a x s),  (r x s), and (a x r)
        a0 = a[0]*s[1]-a[1]*s[0];   // (a x s)
        a1 = r[0]*s[1]-r[1]*s[0];   // (r x s)
        a2 = a[0]*r[1]-a[1]*r[0];   // (a x r)

        intersection[0] = Double.NaN;
        intersection[1] = Double.NaN;
        intersection[2] = Double.NaN;
        intersection[3] = Double.NaN;
        t = Double.NaN;
        u = Double.NaN;

        if (abs(a1)>1e-10d) {
            //t = (q − p) × s / (r × s) where (q-p) = a and (q-p) x s = a0, and r*s = a1
            t = a0 / a1;

            // u = (q − p) × r / (r × s) where (q-p) = a and (q-p) x r = a2, and r*s = a1
            u = a2 / a1;

            // If r × s = 0 and (q − p) × r = 0, then the two lines are collinear.
            // If r × s = 0 and (q − p) × r ≠ 0, then the two lines are parallel and non-intersecting.
            // If r × s ≠ 0 and 0 ≤ t ≤ 1 and 0 ≤ u ≤ 1, the two line segments meet at the point p + t r = q + u s.
            // Otherwise, the two line segments are not parallel, but do not intersect.
            if ((t >= 0.0d && t <= 1.0d) && (u >= 0.0d && u <= 1.0d)) {
                lat = Boat.latitude + t * r[0];
                lon = Boat.longitude + t * r[1];
                a = MarkDistanceBearing(Boat.latitude, Boat.longitude, lat, lon);

                //Log.d(LOG_TAG, "Intercept x       =" + lat);
                //Log.d(LOG_TAG, "Intercept y       =" + lon);
                //Log.d(LOG_TAG, "Intercept distance=" + a[0]);
                //Log.d(LOG_TAG, "Intercept sog=" + sog);

                intersection[0] = a[0];                 // distance to intersection in nm
                intersection[1] = a[0] / sog * 3600.0d; // time to intersection in sec
                intersection[2] = lat;                  // lat of intersection point
                intersection[3] = lon;                  // lon of intersection point
            }
        }
        //Log.d(LOG_TAG, "Intercept t="+t);
        //Log.d(LOG_TAG, "Intercept u="+u);
        //Log.d(LOG_TAG, "Intercept a0="+a0);
        //Log.d(LOG_TAG, "Intercept a1="+a1);
        //Log.d(LOG_TAG, "Intercept a2="+a2);

        return intersection;
    }
    /**
     * Method to compute the favored Pin of the starting line based on the current wind.
     * @param committee LatLng of the committee boat end of the startline
     * @param pin LatLng of the Pin end of the startline
     * @param twd current True Wind Direction in Degrees True North.
     * @return double degrees favored (positive Committee, negative Pin)
     */
    public static double favoredPin(LatLng committee, LatLng pin, double twd) {
        double startline_rightangle;
        double[] DistanceBearing;

        DistanceBearing = MarkDistanceBearing(committee.latitude, committee.longitude, pin.latitude, pin.longitude);
        startline_rightangle = fixAngle(DistanceBearing[1] + 90.0d);
        //Log.d(LOG_TAG,"Start Line length/Direction: "+DistanceBearing[0]+"  "+DistanceBearing[1]+"  right angle: "+startline_rightangle);

        return HeadingDelta(startline_rightangle, twd);
    }

    public static String favoredPinString(double favored) {
        if (favored <= 0.0d) {
            return "Pin";
        } else {
            return "Com.";
        }
    }

    /**
     * Method to compute the Velocity Made Good (VMG) toward the current active mark.
     * @param btm  Bearing-To-Mark (in Degrees True North)
     * @param sog  Speed-Over-Ground (in kts)
     * @return double vmg (in kts)
     */
    public static double calcVMG(double btm, double sog) {
        return Math.cos(Math.toRadians(btm)) * sog;
    }

    /**
     * Method to compute the Velocity Made Good Upwind (VMGu) toward the current active mark.
     * This is the preferred VMG calc since it measures your upwind progress and avoids the
     * disadvantage of the conventional VMG that declines as you approach the mark layline.
     * (because the angle to the mark approaches 90 as you approach the layline, at which point vmg=0).
     *
     * @param twa  True Wind Angle (True North)
     * @param CourseOffset  0.0 for Upwind legs, 180.0 for Downwind legs
     * @param sog  Speed-Over-Ground (in kts)
     * @return vmgu = (sog * cos(twa) (in kts)
     */
    public static double calcVMGu(double twa, double CourseOffset, double sog) {
        double delta = HeadingDelta(CourseOffset, abs(twa));
        /*
        Log.d(LOG_TAG, "twa         : "+df3.format(twa));
        Log.d(LOG_TAG, "CourseOffset: "+df3.format(CourseOffset));
        Log.d(LOG_TAG, "delta       : "+df3.format(delta));
        Log.d(LOG_TAG, "sog         : "+df3.format(sog));
        Log.d(LOG_TAG, "VMGu        : "+df3.format(sog * Math.cos(Math.toRadians(delta))));
        */
        return abs(Math.round(sog*Math.cos(Math.toRadians(delta))*100.0)/100.0);
    }

    /**
     * Method to compute the optimum Laylines from current boat location to a mark
     * returns the waypoint at which to tack to sail the course Boat->Tack->Mark.
     *
     * check drawing in Evernote "SailingRace App - Android"  - angle BTM renamed to Stbd
     * @param boatLat
     * @param boatLon
     * @param markerLat
     * @param markerLon
     * @param TWD  True Wind Direction (True North)
     * @param CourseOffset 0.0 (Upwind course), 180.0 (Downwind course)
     * @param TackAngle optimum Upwind / Downwind Tack or Gybe angle
     * @param tack current tack (stbd or port)
     * @return double[] array with [Latitude, Longitude, initial tack (0=stbd) (1=port), Distance, Heading]
     *                             [   0         1                   2                      3          4    ]
     */
    public static double[] optimumLaylines(double boatLat, double boatLon, double markerLat, double markerLon,
                                           double TWD, double CourseOffset, double TackAngle, String tack) {
        double sign;
        if (CourseOffset == 180.0d) {
            // Downwind course
            sign = -1.0d;
        } else {
            // Upwind course
            sign = 1.0d;
        }

        TackAngle = abs(TackAngle);
        double[] results = new double[5];
        double[] DistanceBearing = MarkDistanceBearing(boatLat, boatLon, markerLat, markerLon);
        double DTM        = DistanceBearing[0];
        double BTM        = DistanceBearing[1];
        double adjTWD     = fixAngle(TWD + CourseOffset);
        double delta      = HeadingDelta(BTM, adjTWD) * sign;
        double TlessD     = Math.toRadians(TackAngle - delta);  // equals theta_port
        double TplusD     = Math.toRadians(TackAngle + delta);  // equals theta_stbd
        double alpha_port = Math.toRadians(90.0 - TackAngle - delta);
        double beta_port  = Math.toRadians(90.0 - TackAngle + delta);
        double theta_stbd = Math.toRadians(TackAngle + delta);  // equals TplusD
        double theta_port = Math.toRadians(TackAngle - delta);

        results[0] = Double.NaN;
        results[1] = Double.NaN;
        /*
        Log.d(LOG_TAG, "TWD        "+df3.format(TWD));
        Log.d(LOG_TAG, "adjTWD     "+df3.format(adjTWD));
        Log.d(LOG_TAG, "BTM        "+df3.format(BTM));
        Log.d(LOG_TAG, "delta      "+df3.format(delta));
        Log.d(LOG_TAG, "TackAngle  "+df3.format(TackAngle));
        Log.d(LOG_TAG, "TA - delta "+df3.format(Math.abs(TackAngle) - Math.abs(delta)));
        */
        if (abs(TackAngle) <= abs(delta)) {
            // we can sail straight to the mark
            results[0] = markerLat;
            results[1] = markerLon;
            if (delta > ZERO) {
                // sail on starboard tack to mark
                results[2] = ZERO;
            } else {
                // sail on port tack to mark
                results[2] = 1.0d;
            }
            results[3] = DTM;
            results[4] = BTM;
        } else {
            // we have to put in a tack to reach the mark
            double d1_stbd = DTM * Math.tan(theta_stbd) / (Math.tan(TlessD)+Math.tan(theta_stbd));
            double d1_port = DTM * Math.tan(theta_port) / (Math.tan(TplusD)+Math.tan(theta_port));
            double d2_port = DTM - d1_port;
            double h1_port = d1_port/Math.cos(TplusD);
            double h2_port = d2_port/Math.sin(beta_port);
            double distance;
            double heading;
            double fav;
            if (h1_port > 1.5d * h2_port) {
                fav = -1.0d;  // Port
            } else if (h2_port > 1.5d * h1_port) {
                fav = 1.0d;   //Starboard
            } else {
                if (tack.equals("stbd")) {
                    fav = 2.0d;  // Starboard_neutral
                } else {
                    fav = -2.0d; // Port_neutral
                }
            }
            if (fav > ZERO) {
                distance = d1_stbd/Math.cos(TlessD);
                heading = fixAngle(BTM - Math.toDegrees(TlessD)*sign);
                results[2] = ZERO;
            } else {
                distance = d1_port/Math.cos(TplusD);
                heading = fixAngle(BTM + Math.toDegrees(TplusD)*sign);
                results[2] = 1.0d;
            }
            double [] latlon = withDistanceBearingToPosition(boatLat, boatLon, distance, heading);

            /* debug logs
            Log.d(LOG_TAG, "DTM       = "+df3.format(DTM));
            Log.d(LOG_TAG, "BTM       = "+df3.format(BTM));
            Log.d(LOG_TAG, "TWD       ="+df3.format(TWD));
            Log.d(LOG_TAG, "Delta     ="+df3.format(delta));
            Log.d(LOG_TAG, "TackAngle ="+df3.format(TackAngle));
            Log.d(LOG_TAG, "TlessD    ="+df3.format(Math.toDegrees(TlessD)));
            Log.d(LOG_TAG, "TplusD    ="+df3.format(Math.toDegrees(TplusD)));
            Log.d(LOG_TAG, "beta_port ="+df3.format(Math.toDegrees(beta_port)));
            Log.d(LOG_TAG, "theta_port="+df3.format(Math.toDegrees(theta_port)));
            Log.d(LOG_TAG, "Distance  ="+df3.format((distance)));
            Log.d(LOG_TAG, "Heading   ="+df3.format(Math.toDegrees(heading));
            Log.d(LOG_TAG, "d1_stbd   ="+df3.format(Math.toDegrees(d1_stbd)));
            Log.d(LOG_TAG, "d1_port   ="+df3.format(Math.toDegrees(d1_port)));
            Log.d(LOG_TAG, "h1_port   ="+df3.format(Math.toDegrees(h1_port)));
            Log.d(LOG_TAG, "h2_port   ="+df3.format(Math.toDegrees(h2_port)));
            double checksum = theta_port + theta_port + beta_port + (Math.toRadians(90.0)-theta_port);
            Log.d(LOG_TAG, "Checksum  ="+df3.format(Math.toDegrees(checksum)));
            */

            results[0] = latlon[0];
            results[1] = latlon[1];
            results[3] = distance;
            results[4] = heading;
        }
        return results;
    }

    /**
     * Method to convert a Double variable time (in sec) into a string with Format "min:sec"
     * @param time
     * @return results
     */
    public static String min_sec(double time) {
        String results;
        long roundedTime = Math.round(time);
        results = Integer.toString((int)Math.floor(roundedTime / 60.0d));
        results += ":"+twoDigits.format((int)(roundedTime % 60));
        return results;
    }

    /**
     * Method to compute the True Wind Direction from the AWD and the SOG.  See the Evernote Sailing
     * note for the various angle definitions and Wikipedia https://en.wikipedia.org/wiki/Apparent_wind
     * Sample implementation in Excel in folder "VariousAppDevelopmentAssets".
     *
     * @param SOG Speed over Ground in Kts
     * @param AWA Apparent Wind Angle (in Degrees True North) = AWD - COG
     * @param AWD Apparent Wind Direction from the Windex Anemometer (in Degrees True North)
     * @param AWS Apparent Wind Speed from the Windex Anemometer (in kts)
     * @return [TWA, TWD, TWS] (True Wind Angle and Direction in degrees True North and True Wind Speed in kts)
     */
    public static double[] calc_TWA_TWD_TWS(double SOG, double AWA, double AWD, double AWS) {
        double sign;
        double[] values = new double[3];

        // initialize the default values for the TWA, TWD, and TWS
        values[0] = AWA;  // TWA
        values[1] = AWD;  // TWD
        values[2] = AWS;  // TWS

        if (SOG <= ZERO || AWS <= ZERO) {
            return values;
        }

        if (AWA < ZERO) {
            sign = -1.0d;
        } else {
            sign = 1.0d;
        }

        values[2] = Math.sqrt(AWS * AWS + SOG * SOG - 2 * AWS * SOG * Math.cos(Math.toRadians(AWA)));
        if (values[2] == ZERO) {
            values[2] = 0.000001d;
        }
        values[0] = Math.toDegrees(Math.acos((AWS*Math.cos(Math.toRadians(AWA))-SOG)/values[2]))*sign;
        values[1] = fixAngle(AWD-(AWA-values[0]));

        return values;
    }

    /**
     * Method to convert the TWA from the Capri-25 polars into an AWA
     *
     * @param SOG in kts
     * @param TWA in degrees True North (range: -180.0 - 0 - +180.0)
     * @param TWS in kts
     * @return double AWA
     */
    public static double TWAtoAWA(double SOG, double TWA, double TWS) {
        double x, sign, AWA;
        boolean upwind = Math.abs(TWA) <= 90.0d;

        if (TWA < ZERO) {
            sign = -1.0;
        } else {
            sign = +1.0;
        }

        TWA = Math.toRadians(Math.abs(TWA));

        if (upwind) {
            x = SOG + TWS * Math.cos(TWA);
        } else {
            x = SOG - TWS * Math.cos(Math.PI-TWA);
        }

        if (Math.abs(x) < PRECISION) {
            AWA = sign * 90.0;
        } else {
            AWA = sign * Math.toDegrees(Math.atan(TWS*Math.sin(TWA)/x));
            if (!upwind) {
                AWA = sign*180.0d + AWA;
            }
        }
        return AWA;
    }
    /**
     * Method to compute Distance (in nm) and Bearing (in degrees True North) from the lat/lon FROM to
     * the lat/lon TO using the Great Circle calculation (distance) and Rhumb line formula (heading).
     *
     * @param latFrom
     * @param lonFrom
     * @param latTo
     * @param lonTo
     * @return double[] array with [Distance (nm), Bearing (True North)] from location FROM to location TO
     */
    public static double [] MarkDistanceBearing(double latFrom, double lonFrom, double latTo, double lonTo) {
        double[] values = new double[2];
        double dLat = Math.toRadians(latTo - latFrom);
        double dLon = Math.toRadians(lonTo - lonFrom);
        double lat1 = Math.toRadians(latFrom);
        double lat2 = Math.toRadians(latTo);

        double a = Math.sin(dLat / 2.0d) * Math.sin(dLat/2.0d) + Math.sin(dLon/2.0d) * Math.sin(dLon/2.0d) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        values[0] = RADIUS * c;          			     // Great Circle distance in mn

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        values[1] = convertXYCoordinateToPolar(x, y);    // Great Circle bearing in True North

        //double x = Math.log(Math.tan(lat2/2.0d+Math.PI/4.0d)/Math.tan(lat1/2.0d+Math.PI/4.0d));
        //values[1] = convertXYCoordinateToPolar(x, dLon);    // Rhumb Line bearing in True North
        //Log.d(LOG_TAG, "Distance: "+values[0]+"  Direction:"+values[1]);

        return values;
    }

    /**
     * Method to compute the latitude and longitude when moving a set Distance (in nm) and
     * Bearing (in degrees) from a given location (latFrom, lonFrom)
     *
     * @param latFrom
     * @param lonFrom
     * @param distance (in nm)
     * @param bearing (in True North)
     * @return double[] array values = [Latitude, Longitude]
     */
    public static double [] withDistanceBearingToPosition(double latFrom, double lonFrom, double distance, double bearing) {
        double[] results = new double[2];
        double dist = distance / RADIUS;
        double brng = Math.toRadians(bearing);
        double lat1 = Math.toRadians(latFrom);
        double lon1 = Math.toRadians(lonFrom);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist) + Math.cos(lat1) * Math.sin(dist) * Math.cos(brng));
        double a = Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat1), Math.cos(dist) - Math.sin(lat1) * Math.sin(lat2));
        double lon2 = lon1 + a;
        lon2 = (lon2 + 3.0d * Math.PI) % (2.0d * Math.PI) - Math.PI;
        results[0] = Math.toDegrees(lat2);
        results[1] = Math.toDegrees(lon2);
        return results;
    }

    /**
     * Compute the Tack/Gybe Angles based on our current Upwind/Downwind course and tack.
     * If we have Windex data we use the polars to compute the tack angles otherwise we
     * utilize the default values from the app SharedPreferences
     *
     * @param windex
     * @param polars
     * @param tack
     * @param TackAngle
     * @param GybeAngle
     * @param CourseOffset
     * @param TWS
     *
     * @return double TackAngle
     */
    public static double getTackAngle(int windex, int polars, String tack, double TackAngle, double GybeAngle, double CourseOffset, double TWS) {
        double angle;
        if (windex >= 1 && polars == 1) {
            double[] tmp = getPolars(TWS, CourseOffset);
            if (CourseOffset == ZERO) {
                angle = -tmp[1];
            } else {
                angle = tmp[1];
            }
        } else {
            if (CourseOffset == ZERO) {
                angle = TackAngle;
            } else {
                angle = GybeAngle;
            }
        }
        if (tack.equals("port")) {
            return -angle;
        }
        return angle;
    }

    /**
     * Method to convert the output (double) from the optimumLaylines method into a string
     * @param layline
     * @return
     */
    public static String LaylinesString(double layline) {
        try {
            if (layline == ZERO) {
                return "Stbd";
            } else if (layline == 1.0d) {
                return "Port";
            } else {
                return "- -";
            }
        } catch(Exception e) {
            return "- -";
        }
    }

    /**
     * Method to compute the wind frequency from the data in the LinkList TWD
     * using a Fast Fourier Transformation.  This method requires a minimum of
     * MIN_FFT_DATA_POINTS (currently set to 600 = 10 mins)
     *
     * @param sample_rate rate at which data points are sampled per second.
     *                    in our case this is the rate of the screen_updates.
     */
    public static void fft_wind_frequency(double sample_rate) {
        int size = TWD.size();

        if (size > MIN_FFT_DATA_POINTS) {
            fft = new FastFourierTransformation(size);
            double[][] xy = fft.fetch_data(TWD, TWD_longAVG);
            fft.fft(xy[0], xy[1]);
            //Log.d(LOG_TAG, "FFT nfft="+fft.nfft+"  xy="+xy.length);

            wind_frequency = fft.frequency(xy[0], sample_rate);
            wind_period = (int)(1.0d / wind_frequency);
            wind_cycles = fft.nfft / sample_rate * wind_frequency;

            if (Fragment_RaceInfo.above_mean) {
                // Wind is above (larger) than avg wind.  Thus we're in first half of sine wave
                wind_sine_start = (1.0d - (wind_cycles - (int)wind_cycles)) * size/wind_cycles + Fragment_RaceInfo.current_duration;
            } else {
                // Wind is below (smaller) than avg wind.  Thus we're in second half of sine wave
                // and add Math.PI (or one half of a full oscillation of 2 PI) to the start point
                wind_sine_start = (0.5d - (wind_cycles - (int)wind_cycles)) * size/wind_cycles + Fragment_RaceInfo.current_duration;
            }

            /**
            Log.d(LOG_TAG, "fft_wind_frequency() Wind Period (hrs / min): "+hrs+" / "+min);
            Log.d(LOG_TAG, "fft_wind_frequency() Wind above mean: "+Fragment_RaceInfo.above_mean);
            Log.d(LOG_TAG, "fft_wind_frequency() Wind Period cycles: "+wind_cycles);
            */

        } else {
            wind_frequency = Double.NaN;
        }

    }

    /**
     * Compute the Capri 25 polars.  See Excel file "Capri25_Polars.xlxs" for the details.
     * Method returns the Target Boat Speed and optimum Tack Angle for each input True Wind Speed (TWS).
     *
     * @param TWS True Wind Speed (in kts)
     * @param CourseOffset 0 = Upwind, 180 = Downwind Course
     * @results   double[] array with [Target Boat Speed (TBS in nm), tack angle (in True North)]
     */
    public static double[] getPolars(double TWS, double CourseOffset) {
        double[] results = new double[2];
        double angle;   // target Tack / Gybe angle
        double TBS;     // Target Boat Speed

        if (CourseOffset == ZERO) {
            //--------------------------------------------------------------------------------------
            // compute the Upwind Tack Angle and Target Boat Speed
            //--------------------------------------------------------------------------------------
            if (TWS > 15.0d) {
                angle = 38.0d;
            } else {
                angle = 0.0086d*Math.pow(TWS, 3.0d) - 0.2721d*Math.pow(TWS, 2.0d) + 1.7571d*TWS + 44.038d;
                if (angle > 47.0d) {
                    angle = 47.0d;
                }
                if (angle < 38.0d) {
                    angle = 38.0d;
                }
            }

            if (TWS > 14.0d) {
                TBS = 5.6d;
            } else {
                // original polar TBS formula
                //TBS = 0.0013d*Math.pow(TWS, 3.0d) - 0.0595d*Math.pow(TWS, 2.0d) + 0.9333d*TWS + 0.6353d;

                //revised TBS formula
                TBS = -0.0227*Math.pow(TWS, 2.0d)+0.709d*TWS+0.093d;
            }
            if (TBS < ZERO || TWS == ZERO) {
                TBS = ZERO;
            }
            if (TBS > 5.6d) {
                TBS = 5.6d;
            }
        } else {
            //--------------------------------------------------------------------------------------
            // compute the Downwind Gybe Angle and Target Boat Speed
            //--------------------------------------------------------------------------------------
            if (TWS > 12.0d) {
                angle = 173.0d;
            } else {
                angle = 0.0076d*Math.pow(TWS, 4.0d) - 0.3149d*Math.pow(TWS, 3.0d) + 4.1961d*Math.pow(TWS, 2.0d) - 16.652d*TWS + 155.56d;
                if (angle > 173.0d) {
                    angle = 173.0d;
                }
                if (angle < 135.0d) {
                    angle = 135.0d;
                }
            }
            angle = 180.0d - angle;

            TBS = -0.0003d*Math.pow(TWS, 4.0d) + 0.012d*Math.pow(TWS, 3.0d) - 0.1653d*Math.pow(TWS, 2.0d) + 1.2019d*TWS + 0.207d;
            if (TBS < ZERO || TWS == ZERO) {
                TBS = ZERO;
            }
            if (TBS > 6.1d) {
                TBS = 6.1d;
            }
        }
        results[0] = TBS;
        results[1] = angle;
        return results;
    }

    /**
     * Method to compute the Modulo of a by param b
     * @param a
     * @param b
     * @return modulo
     */
    protected static double Mod(double a, double b) {
        /** Modulo function */
        while (a < 0) {
            a += b;
        }
        return a % b;
    }

    /**
     * Method to convert a double position into a degree and decimal minutes string
     *
     * @param wp waypoint position as double value (in degrees)
     * @param flag True: Latitude value; False: Longitude value
     * @return string
     */
    public static String PositionDegreeToString(double wp, boolean flag) {
        DecimalFormat dfThree = new DecimalFormat("000");
        DecimalFormat dfTwo = new DecimalFormat("00");
        DecimalFormat df3 = new DecimalFormat("#0.000");
        double wpabs = abs(wp);
        int d = (int) (wpabs);
        double m = (wpabs - d) * 60.0d;
        String strg = df3.format(m);
        if (flag) {           // we convert a latitude
            if (wp > 0) {
                strg = dfTwo.format(d) + "° " + strg + "' N";
            } else {
                strg = dfTwo.format(d) + "° " + strg + "' S";
            }
        } else {              // we convert a longitude
            if (wp > 0) {
                strg = dfThree.format(d) + "° " + strg + "' E";
            } else {
                strg = dfThree.format(d) + "° " + strg + "' W";
            }
        }
        return (strg);
    }
    /**
     * Convert date in milliseconds UTC to a date string in local timezone
     * @param milliSeconds Date in milliseconds UTC
     * @param dateFormat Date format
     * @return String formatted to the local TZ in specified format
     */
    public static String getDateString(long milliSeconds, String dateFormat) {

        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(TimeZone.getDefault());     // the the default local TZ

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance(Activity_Main.TZ_UTC);
        calendar.setTimeInMillis(milliSeconds);
        return sdf.format(calendar.getTime());
    }

    /**
     * Convert a date string in local timezone to a date in milliseconds UTC
     * @param DateString in local TZ
     * @param dateFormat Date format
     * @return long integer representing date in milliseconds UTC
     */
    public static long convertDateString(String DateString, String dateFormat) {

        Calendar calendar = Calendar.getInstance(Activity_Main.TZ_UTC);
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(TimeZone.getDefault());     // the the default local TZ

        try {
            Date date = sdf.parse(DateString);
            calendar.setTime(date);
            return calendar.getTimeInMillis();
        } catch (ParseException e) {
            Log.d(LOG_TAG, "Could not parse date string "+DateString);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 10);
            return calendar.getTimeInMillis();
        }
    }
}
