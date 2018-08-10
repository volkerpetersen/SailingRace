#!/usr/bin/env python
__doc__ = """
--------------------------------------------------------------------------------------------
Offline Sailing Races - Wind Analysis
This source code is available under the Python 3.5 | GPL license
(see: http://www.gnu.org/licenses/gpl.txt for more info).

@author: Volker Petersen <volker.petersen01@gmail.com>
--------------------------------------------------------------------------------------------
"""
__author__      = "Volker Petersen <volker.petersen01@gmail.com>"
__version__     = "WindOscillations mysql_tools.py Revision: 1.1"
__date__        = "Date: 2017/03/18"
__copyright__   = "Copyright (c) 2017 Volker Petersen"
__license__     = "Python 3.5 | GPL http://www.gnu.org/licenses/gpl.txt"

try:
    import os, sys, platform, inspect
    import pymysql
    import pandas.io.sql as psql
    import pandas as pd

except ImportError as e:
    print ("Import error. %s\nAborting the program" %(str(e), __version__))
    sys.exit()


#==============================================================================================
class MYSQL(object):
    """
    ------------------------------------------------------------------------------------------
     MYSQL Class with the various mysql tools
    ------------------------------------------------------------------------------------------
    """
    def __init__(self):
        """ initialize the column names at our own mysql datastore
            in the tables 'races' and 'wind'.                                    """

        self.races = ['Race_ID',      # 0
            'Name',                   # 1
            'WWD_LAT',                # 2
            'WWD_LON',                # 3
            'LWD_LAT',                # 4
            'LWD_LON',                # 5
            'CTR_LAT',                # 6
            'CTR_LON',                # 7
            'avg_TWS',                # 8
            'avg_TWD']                # 9

        self.wind = ['Date',          # 0
                    'AWD',            # 1
                    'AWS',            # 2
                    'TWD',            # 3
                    'TWS',            # 4
                    'SOG',            # 5
                    'LAT',            # 6
                    'LON',            # 7
                    'Quadrant',       # 8
                    'Status',         # 9
                    'Race_ID']        #10


#==============================================================================================
    def __str__(self):
        """ generate a printout of the class initialization """
        print ("\nData columns in mysql table 'races':")
        print (self.races)
        print ("\nData columns in mysql table 'wind':")
        print (self.wind)
        return "\n"
        

#==============================================================================================
    def list_races(self):
        """ return a list of all data columns in a specified data structure """
        return self.races

#==============================================================================================
    def list_wind(self):
        """ return a list of all data columns in a specified data structure """
        return self.wind

#==============================================================================================
    def connect_db(self):
        """
        |------------------------------------------------------------------------------------------
        | function to connect to the mysql DB 'securities_master' at Hostmonster SouthMetroChorale
        | @ret        - mysql connector or False if connection failed
        |------------------------------------------------------------------------------------------
        """
        if "linux" in platform.system().lower():
            db_host = 'localhost'
        else:
            db_host = 'a2ss54.a2hosting.com'
        db_user = 'southme1_sudo'
        db_pass = 'Vesret7713'
        db_name = 'southme1_offline_wind_records'
        try:
            self.con = pymysql.connect(db_host, db_user, db_pass, db_name)
        except:
            print ("Failed to connect to DB % on host %s" %(db_host, db_name))
            self.con = False
        return self.con


#==============================================================================================
    def fetch_races(self):
        """
        ------------------------------------------------------------------------------------------
         fetch all the races from the mysql database
         @ret    df - pandas df with the data retrieved from the mysql DB.  [] if no data found.
        ------------------------------------------------------------------------------------------
        """

        sql_query = "SELECT * FROM races"
        df = self.fetch_query(sql_query, self.races[0])

        return df

#==============================================================================================
    def fetch_wind(self, raceID):
        """
        ------------------------------------------------------------------------------------------
         fetch the requested wind records from the mysql database
         @param  raceID - the Race_ID of the wind records to be retrieved from the mysql DB
         @ret    df     - pandas df with the data retrieved from the mysql DB.  [] if no data found.
        ------------------------------------------------------------------------------------------
        """

        sql_query = "SELECT * FROM wind WHERE %s='%s'" %(self.wind[10], raceID)
        df = self.fetch_query(sql_query, self.wind[0])

        return df

#==============================================================================================
    def fetch_query(self, query, index=None):
        """
        ------------------------------------------------------------------------------------------
         fetch records from the mysql database based on a custom query
         @param  query - the query to be used to fetch selected records from the mysql DB
         @param  index - the column to be used as df index
         @ret    df    - pandas df with the data retrieved from the mysql DB.  [] if no data found.
        ------------------------------------------------------------------------------------------
        """

        con = self.connect_db()
        if con == False:
            exit("\nTerminating Class module", __version__)

        if index:        
            df = psql.read_sql(query, con=con, index_col=index)
        else:
            df = psql.read_sql(query, con=con)
            
        try:
            con.close()
        except:
            print ("couldn't close mysql DB")

        return df.sort_index(axis=0)   # return DF sorted with ascending index


"""
|------------------------------------------------------------------------------------------
| main
|------------------------------------------------------------------------------------------
"""
if __name__ == "__main__":
    print (__doc__)

    cwd = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
    
    mysql = MYSQL()
    #print (mysql)
    
    races = mysql.fetch_races()
    print (races)
    
    wind = mysql.fetch_wind(1491627600)  # 2017-04-08
    filename = os.path.join(cwd, "wind_2017_04_08")
    wind.to_pickle(filename)
    #wind = pd.read_pickle(file_name)
    print ("fetched %d records from date 2017-04-08" %wind['AWD'].count())
    
    wind = mysql.fetch_wind(1494478800)  # 2017-05-11
    filename = os.path.join(cwd, "wind_2017_05_11")
    wind.to_pickle(filename)
    print ("fetched %d records from date 2017-05-11" %wind['AWD'].count())
    
    #query = "SELECT %s, %s FROM wind WHERE %s='%s'" %(mysql.wind[0], mysql.wind[3], mysql.wind[7], 1488866400)
    #print (query)
    #wind = mysql.fetch_query(query, mysql.wind[0])
    #print (wind.tail())
    
    print ('\nDone!\n')

