#!/usr/bin/env python
# -*- coding: utf-8 -*-

__date__ = "Sun Jan 04 12:51:48 2015"
__author__ = "Volker Petersen"
__version__ = "TackAnalysis.py ver 1.0"
__copyright__ = "Copyright (c) 2014 Volker Petersen"
__license__ = "Python 2.7 | GPL http://www.gnu.org/licenses/gpl.txt"
__doc__ = """
---------------------------------------------------------------------------
Program Description
---------------------------------------------------------------------------
"""
try:
    # import python system modules
    import sys
    import numpy as np
    from matplotlib import pyplot as plt
    import pandas as pd
    import math
    from mysql_tools import MYSQL 
except ImportError as e:
    print "Import error:", e, "\nAborting the program", __version__
    sys.exit()
    
fig = 1

def get_rolling_mean(df, col, window=2, name='moving avg'):
    """Return rolling mean of values in df[col], using specified window size."""
    #temp = pd.rolling_mean(df, window=window, min_periods=1)  # old version
    temp = df.rolling(min_periods=1, window=window, center=False).mean()
    temp = temp.rename(columns={col:name})
    df = df.join(temp[name])
    return df

def get_rolling_std(df, col, window=2, name='std dev'):
    """Return rolling standard deviation of given values, using specified window size."""
    #temp = pd.rolling_std(df, window=window, min_periods=1)  # old version
    temp = df.rolling(min_periods=1, window=window, center=False).std()
    temp = temp.rename(columns={col:name})
    df = df.join(temp[name])
    return df

def plot_data(df):
    global fig    
    fig = fig + 1
    plt.figure(fig)
    ind = df.index.values
    plt.plot(ind, df['TWD'].values, 'b', lw=1.0, label='TWD')
    plt.plot(ind, df[avg_names[0]].values, 'r', lw=3, label=avg_names[0])
    plt.plot(ind, df[avg_names[1]].values, 'g', lw=3, label=avg_names[1])
    plt.plot(ind, df['FFT'].values, 'k', lw=2.0, label='FTT')
    plt.title('TWD Oscillation Analysis')
    plt.grid()
    plt.xlabel('Time (sec)')
    plt.ylabel('TWD')
    plt.legend()
    plt.show()
    

def tack_analysis(raceID=1488866400, plot=True):
    global fig
    
    if raceID < 0:
        print "Invalid Race ID (%d). Terminating....." %raceID
    else:
        mysql = MYSQL()
        query = "SELECT * FROM wind WHERE %s='%s'" %(mysql.wind[10], raceID)
        #print query
        
        wind = mysql.fetch_query(query, mysql.wind[0])
        #print wind.tail()

        if len(wind) == 0:
            print "Invalid Race ID (%d). Terminating....." %raceID
            return 
            
        if plot:    
            fig += 1
            plt.figure(fig)
            plt.plot(wind.index.values, wind['TWD'].values, 'b', lw=1.0, label='TWD')
            plt.show()
    
"""
|------------------------------------------------------------------------------------------
|------------------------------------------------------------------------------------------
"""
if __name__ == "__main__":
    print "\n", __version__
    
    tack_analysis(raceID=1492318800, plot=True)

    print "\nDone!"    