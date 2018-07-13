#!/usr/bin/env python
"""
---------------------------------------------------------------------------
Program Description
---------------------------------------------------------------------------
"""

__date__      = "Thu Dec 31 10:32:29 2015"
__author__    = "Volker Petersen"
__copyright__ = "Copyright (c) 2014 Volker Petersen"
__license__   = "Python 2.7 | GPL http://www.gnu.org/licenses/gpl.txt"
__version__   = "ProgramName.py ver 1.0"

try:
    # import python system modules
    import sys
    import sqlite3
    import datetime
    import time
    from random import randint
except ImportError as e:
    print "Import error:", e, "\nAborting the program", __version__
    sys.exit()

        
def createSQLITE(db, table_name, fields):
    # Creating a new SQLite table with 1 column
    field_string = ""   
    for f in fields:
        field_string += f[0]+f[1] + ","
    field_string = field_string[:-1]
    print field_string    
    db.execute('CREATE TABLE if not exists {tn} ({nf})'.format(tn=table_name, nf=field_string))
    print "Created table", table_name
 
def fetch_datetime(duration):
    print "Date / time:", datetime.datetime.now()
    
def add_ten_entries(db):
    i = 0
    while (i < 10):
        i += 1
        now = datetime.datetime.now()
        print "Run",i,"at", now, randint(250, 285)
        time.sleep(2)
        
"""
|------------------------------------------------------------------------------------------
| main
|------------------------------------------------------------------------------------------
"""
if __name__ == "__main__":
    sqlite_file = 'WindData.sqlite'  # name of the sqlite database file
    table_name  = 'WindData'            # name of the table to be created
    fields = [['Date', ' INTEGER PRIMARY KEY NOT NULL'],
              ['TWD', ' INTEGER'],
              ['TWS', ' REAL'],
              ['Quadrant', ' INTEGER']] 

    # Connecting to the database file
    try:    
        conn = sqlite3.connect(sqlite_file)
        db = conn.cursor()
        createSQLITE(db, table_name, fields)
        add_ten_entries(db)
        
    except Exception as e:        
        print "Error %s:" % e.args[0]
        sys.exit(1)
    finally:
        # Committing changes and closing the connection to the database file
        conn.commit()
        conn.close()