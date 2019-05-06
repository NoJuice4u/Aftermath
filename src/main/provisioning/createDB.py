import mysql.connector 
import argparse

DATABASE = "aftermath_data"
TABLES = {
            "map_vertex": {"vertexId": "UNSIGNED INT", "lat": "DECIMAL(10, 8)", "lon": "DECIMAL(11, 8)"},
            "map_edge": {""}}
 
parser = argparse.ArgumentParser(description="Lunch Bot.  Need I say more?")
parser.add_argument('--host', required=True, help='MySQL Host')
parser.add_argument('--user', required=True, help='MySQL Root User Login (For DB Creation)')
parser.add_argument('--password', required=True, help='MySQL Password')
args = parser.parse_args()

db = mysql.connector.connect(host=args.host, user=args.user, passwd=args.password, auth_plugin='mysql_native_password')
cursor = db.cursor()

cursor.execute("SHOW DATABASES")

found = False
for x in cursor:
    if(x[0] == DATABASE):
        print(DATABASE + " table found.")
        found = True

if(found == False):
    print(DATABASE + " table not found.  Creating database")
    cursor.execute("CREATE DATABASE " + DATABASE)

cursor.execute("USE " + DATABASE)
cursor.execute("SHOW tables")

# CREATE TABLE pet (name VARCHAR(20), owner VARCHAR(20), species VARCHAR(20), sex CHAR(1), birth DATE, death DATE);

print("TABLES")
for x in cursor:
    print(x)
