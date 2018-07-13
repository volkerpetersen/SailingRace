#!/usr/bin/env python
"""
---------------------------------------------------------------------------
Program Description
---------------------------------------------------------------------------
Program to compute laylines and optimum course

"""

__date__      = "Mon Nov 23 12:37:16 2015"
__author__    = "Volker Petersen"
__copyright__ = "Copyright (c) 2014 Volker Petersen"
__license__   = "Python 2.7 | GPL http://www.gnu.org/licenses/gpl.txt"
__version__   = "SailingRace_Math.py ver 1.0"

try:
    # import python system modules
    import sys
    import matplotlib.pyplot as plt
    import math
except ImportError as e:
    print "Import error:", e, "\nAborting the program", __version__
    sys.exit()

def withDistanceBearingToPosition(latFrom, lonFrom, distance, bearing):
    km_nm = 0.539706          # km to nm conversion factor 
    Radius = 6371.0*km_nm	 # Earth Radius in nm 
    
    dist = distance / Radius
    brng = math.radians(bearing)
    lat1 = math.radians(latFrom)
    lon1 = math.radians(lonFrom)

    lat2 = math.asin(math.sin(lat1) * math.cos(dist) + math.cos(lat1) * math.sin(dist) * math.cos(brng))
    a = math.atan2(math.sin(brng) * math.sin(dist) * math.cos(lat1), math.cos(dist) - math.sin(lat1) * math.sin(lat2))
    lon2 = lon1 + a
    lon2 = (lon2 + 3 * math.pi) % (2 * math.pi) - math.pi
    return (math.degrees(lat2), math.degrees(lon2))

def MarkDistanceBearing(latFrom, lonFrom, latTo, lonTo):
    #latFrom=  44.63630626204924  
    #lonFrom =-93.3544007186544
    #latTo = 44.62666894009293  
    #lonTo = -93.3626308143815

    deltaLat = math.radians(latTo)-math.radians(latFrom) 
    deltaLon = math.radians(lonTo)-math.radians(lonFrom)
    latTo = math.radians(latTo)
    latFrom = math.radians(latFrom)
    
    a = math.sin(deltaLat/2.0) * math.sin(deltaLat/2.0) + \
        math.cos(latFrom) * math.cos(latTo) * \
        math.sin(deltaLon/2.0) * math.sin(deltaLon/2.0)
    
    c = 2.0*math.atan2(math.sqrt(a), math.sqrt(1.0-a))
    RADIUS = 6371.0*0.539956803
    print "distance = ",c*RADIUS

    y = math.sin(deltaLon) * math.cos(latTo)
    x = math.cos(latFrom) * math.sin(latTo) - \
        math.sin(latFrom) * math.cos(latTo) * math.cos(deltaLon)
    
    brng = math.degrees(math.atan2(y,x))
    brng = (brng+360.0) % 360.0
    print "bearing = ", brng    
    

def testDistanceBearing():
    boatLat = 44.631
    boatLon = -93.369
    markerLat = 44.653
    markerLon = -93.372
    DTM = 1.28
    BTM = 355.0
    (lat, lon) = withDistanceBearingToPosition(boatLat, boatLon, DTM, BTM)
    print "\nTest of the calc Distance/Bearing added to Position" 
    print "Boat Lat:\t %.3f" %boatLat
    print "Boat Lon:\t %.3f" %boatLon
    print "\nDTM:\t %.3f nm \t %.3f km" %(DTM, DTM/0.539706)
    print "BTM:\t %.3f" %BTM
    print "\nMarker Lat:\t %.3f" %markerLat
    print "Marker Lon:\t %.3f" %markerLon
    print "\nCalc Lat:\t %.3f" %lat
    print "Calc Lon:\t %.3f" %lon
    
def Mod(a, b):
    #* Modulo function *
    while (a < 0):
        a += b
    return a % b
    
def fixAngle(angle):
    return Mod(angle, 360.0)

def HeadingDelta(From, To):
    # difference between two angles going from angle "From" to angle "To"
    # Clockwise => Positive values 0-180, 
    # Counter-Clockwise => Negative values 0-180
    # 
    if (From > 360 or From < 0):
        From = Mod(From, 360)
    if (To > 360 or To < 0):
        To = Mod(To, 360)

    diff = To - From;
    absDiff = abs(diff);

    if (absDiff <= 180):
        if (absDiff == 180):
            diff = absDiff
        #print "\nFrom: %s   To: %s   absDiff <= 180 => diff: %s" %(From, To, diff)
        return diff
    elif (To > From):
        return absDiff - 360;
    else:
        return 360 - absDiff;
    
    
def test_headingDelta():
    #      HeadingDelta(Fron To)
    print "1.) testing 10, 50 == ", HeadingDelta(10, 50),
    assert HeadingDelta(10, 50) == 40
    print "..... passed!"

    print "2.) testing 340, 20 == ", HeadingDelta(340, 20),
    assert HeadingDelta(340, 20) == 40
    print "..... passed!"

    print "3.) testing 300, 340 == ", HeadingDelta(300, 340),
    assert HeadingDelta(300, 340) == 40
    print "..... passed!"
    
    print "4.) testing 50, 10 == ", HeadingDelta(50, 10),
    assert HeadingDelta(50, 10) == -40
    print "..... passed!"

    print "5.) testing 20, 340 == ", HeadingDelta(20, 340),
    assert HeadingDelta(20, 340) == -40
    print "..... passed!"

    print "6.) testing 340, 300 == ", HeadingDelta(340, 300),
    assert HeadingDelta(340, 300) == -40
    print "..... passed!"

    print "7.) testing 340, 160 == ", HeadingDelta(340, 160),
    assert HeadingDelta(340, 160) == 180
    print "..... passed!"

    print "8.) testing 700, 300 == ", HeadingDelta(700, 300),
    assert HeadingDelta(700, 300) == -40
    print "..... passed!"

    print "9.) testing 340, 660 == ", HeadingDelta(340, 660),
    assert HeadingDelta(340, 660) == -40
    print "..... passed!"

    print "10.)testing 360, 1 == ", HeadingDelta(360, 1),
    assert HeadingDelta(360, 1) == 1
    print "..... passed!"

    print "11.)testing 1, 360 == ", HeadingDelta(1, 360),
    assert HeadingDelta(1, 360) == -1
    print "..... passed!"
     

def plot_course(data, scale):
    fig, ax = plt.subplots()
    ax.plot(*data)
    plt.ylim([0, scale])
    plt.xlim([-scale/2, scale/2])
    ax.margins(0.1)
    ax.set_autoscaley_on(False)
    fig.show()

def shortest_distance_to_mark(TackAngle, DTM, BTM, TWD, CourseOffset, plot=True):
    # see Evernote for drawing with all angle definitions    
    if (CourseOffset == 180.0):
        sign = -1.0
    else:
        sign = 1.0
    delta = HeadingDelta(BTM, TWD)*sign
    if (abs(delta) > TackAngle):
        theta_stbd = 0.0
        theta_port = 0.0
        if delta < 0:
            fav = "Port"
            d1_stbd = 0.0
            d2_stbd = 0.0
            d1_port = 0.0
            d2_stbd = DTM
            TlessD = 0.0
            TplusD = 0.0
            dist_port = DTM
            dist_stbd = 0.0
        else:
            fav = "Starboard"
            d1_stbd = 0.0
            d2_stbd = DTM
            d1_port = 0.0
            d2_port = 0.0
            TlessD = 0.0
            TplusD = 0.0
            dist_port = 0.0
            dist_stbd = DTM
        alpha_stbd = 0.0
        beta_stbd  = 0.0
        theta_stbd = 0.0
        alpha_port = 0.0
        beta_port  = 0.0
        theta_port = 0.0
        delta = math.radians(delta)
           
    else:
        TackAngle  = abs(TackAngle)
        print "delta:", delta
        TlessD     = math.radians(TackAngle - delta)
        TplusD     = math.radians(TackAngle + delta)
        alpha_stbd = math.radians(90.0-TackAngle+delta)
        beta_stbd  = math.radians(90.0-TackAngle-delta)
        theta_stbd = math.radians(TackAngle+delta)
        alpha_port = math.radians(90.0-TackAngle-delta)
        beta_port  = math.radians(90.0-TackAngle+delta)
        theta_port = math.radians(TackAngle-delta)
        delta = math.radians(delta)
                
        d1_stbd = DTM * math.tan(theta_stbd) / (math.tan(TlessD)+math.tan(theta_stbd))
        d2_stbd = DTM - d1_stbd
        dist_stbd = d1_stbd/math.cos(TlessD) + d2_stbd/math.sin(beta_stbd)
    
        d1_port = DTM * math.tan(theta_port) / (math.tan(TplusD)+math.tan(theta_port)) 
        d2_port = DTM - d1_port
        h1_port = d1_port/math.cos(TplusD) 
        h2_port = d2_port/math.sin(beta_port)
        dist_port = h1_port + h2_port
        fav = ""
        if (h1_port * 1.5 < h2_port):
            fav = "Starboard"
        if (h2_port * 1.5 < h1_port):
            fav = "Port"
        
    x1 = 0.0
    y1 = 0.0
    x2 = x1 - d1_stbd*math.tan(TlessD)
    y2 = y1 + d1_stbd
    x3 = x1 + d1_port*math.tan(TplusD)
    y3 = y1 + d1_port
    x4 = x2 + d2_stbd*math.tan(theta_stbd)
    y4 = y2 +d2_stbd
    x5 = x3 - d2_port*math.tan(theta_port)
    y5 = y3 + d2_port
    xw = x1 + DTM*1.2*math.sin(delta)
    yw = y1 + DTM*1.2*math.cos(delta)
    lines = [(x1, x2), (y1, y2), 'g',
             (x2, x4), (y2, y4), 'r',
             (x1, x3), (y1, y3), 'r',
             (x3, x5), (y3, y5), 'g',
             (x1, xw), (y1, yw), 'b',]
             #(x1, x1), (y1, y1+DTM), 'k']    
    if (plot):
        print "alpha stbd:\t %.1f" %math.degrees(alpha_stbd)
        print "beta stbd:\t %.1f" %math.degrees(beta_stbd)
        print "theta stdb:\t %.1f" %math.degrees(theta_stbd)
        print "Delta:\t %.1f" %math.degrees(delta)
        print "checksum:\t %.2f" %(180.0-math.degrees(alpha_stbd)-math.degrees(beta_stbd)-math.degrees(theta_stbd)-math.degrees(TlessD))
        print "\nalpha port:\t %.1f" %math.degrees(alpha_port)
        print "beta port:\t %.1f" %math.degrees(beta_port)
        print "theta port:\t %.1f" %math.degrees(theta_port)
        print "TWD:\t %.1f" %(TWD)
        print "BTM:\t %.1f" %(BTM)
        print "checksum:\t %.2f" %(180.0-math.degrees(alpha_port)-math.degrees(beta_port)-math.degrees(theta_port)-math.degrees(TplusD))
        print "\nTack Angle:\t %.1f" %TackAngle
        print "Tack-Delta:\t %.1f" %math.degrees(TlessD)
        print "Tack+Delta:\t %.1f" %math.degrees(TplusD)
        print "h1_port   :\t %.1f" %(h1_port)
        print "h2_port   :\t %.1f" %(h2_port)
        if (beta_stbd != 0.0):
            print "stbd x:\t %.4f \t %.4f" %(d1_stbd*math.tan(TlessD), d2_stbd/math.tan(beta_stbd))
            print "port x:\t %.4f \t %.4f" %(d1_port*math.tan(TplusD), d2_port/math.tan(beta_port))
        plot_course(lines, int(DTM*1.2))

    return (d1_port, d2_port, d1_stbd, d2_stbd, dist_port, dist_stbd, fav)
    
def main(CourseOffset):
    TackAngle = 40
    tack = "port"
    COG = 40
    BTM = 61.42
    DTM = 10.0    
    TWD = 42.5
    
    (d1_port, d2_port, d1_stbd, d2_stbd, sail_port, sail_stbd, fav) = shortest_distance_to_mark(TackAngle, DTM, BTM, TWD, CourseOffset, True)
    print "\nPort tack first:"  
    print "d1:\t\t %.3f" %d1_port
    print "d2:\t\t %.3f" %d2_port
    print "distance port-stbd:\t %.3f" %sail_port
    print "\nStarboard tack first:"    
    print "d1:\t\t %.3f" %d1_stbd
    print "d2:\t\t %.3f" %d2_stbd
    print "distance stbd-port:\t %.3f" %sail_stbd
    if (fav == ""):
        print "\nNo clearly favored track"
    else:
        print "\nFavorite Tack is the lifted %s tack" %fav
    return
    
"""
|------------------------------------------------------------------------------------------
| main
|------------------------------------------------------------------------------------------
"""
if __name__ == "__main__":
    print __doc__  
    #test_headingDelta()
    CourseOffset = 0.0  # Upwind Course
    CourseOffset = 180.0  # Downwind Course
    main(CourseOffset)
    testDistanceBearing()
    MarkDistanceBearing(44.63630626204924, -93.3544007186544, 44.62666894009293, -93.3626308143815)

