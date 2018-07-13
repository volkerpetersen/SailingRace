# -*- coding: utf-8 -*-
"""
Created on Mon Jul 25 14:10:47 2016

@author: Volker Petersen
"""

import numpy as np
import math

#--------------------------------------------------------------------------------------------
# Function to compute the distance between two Lat/Lon coordinates
#--------------------------------------------------------------------------------------------        
def calc_distance(latFrom, lonFrom, latTo, lonTo):
    km_nm = 0.539706            # km to nm conversion factor
    RADIUS = 6371.0*km_nm       # Earth RADIUS in nm 
    dLat = math.radians(latTo - latFrom)
    dLon = math.radians(lonTo - lonFrom)
    lat1 = math.radians(latFrom)
    lat2 = math.radians(latTo)

    a = math.sin(dLat / 2.0) * math.sin(dLat/2.0) + math.sin(dLon/2.0) * math.sin(dLon/2.0) * math.cos(lat1) * math.cos(lat2)
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    return RADIUS * c


# Run the program
if __name__ == "__main__":
    q = [3.0, 6.0]      # current loat location
    s = [3.0, 7.0]      # distance from boat to future boat position at [6, 13]
    p = [6.0, 7.0]      # committee
    r = [-2.0, 4.0]     # distance from committee to pin at [4, 11]
    
    
    print np.add(q, s)
    print np.add(p, r)    
    
    a0 = np.cross(np.subtract(q, p), s) 
    print "a =",np.subtract(q,p)
    print "a =", [q[0]-p[0], q[1]-p[1]]
    
    # cross product long hand
    a1 = (r[0]*s[1]-r[1]*s[0])
    print "a1 =", a1, np.cross(r, s), "(long, short)"
    
    print ("\n(q-p) x s ="), a0
    print ("r x s    ="), a1
    t = a0/a1
    print "t =",t
    
    a2 = np.cross(np.subtract(q, p), r) 
    print ("\n(q-p) x r ="), a2
    print ("r x s    ="), a1
    u = a2/a1
    print "u =",u
    
    intersection1 = np.add(q, np.multiply(u, s))
    intersection2 = np.add(p, np.multiply(t, r))
    
    print "\nx =", intersection1[0], q[0]+u*s[0]
    print "y =", intersection1[1], q[1]+u*s[1]
    
    print "x =", intersection2[0],
    print "y =", intersection2[1]
    
    distance = math.sqrt((intersection1[0]-p[0])*(intersection1[0]-p[0])+(intersection1[1]-p[1])*(intersection1[1]-p[1]))
    print "Distance =", distance, "sqrt()"
    distance = calc_distance(p[0], p[1], intersection1[0], intersection1[1])
    print "Distance =", distance
    print "Time     =", distance / 5.0 * 3600.0
    print "a0       =", a0
    print "a1       =", a1
    print "a2       =", a2
    
    
    
