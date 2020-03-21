#!/usr/bin/env python3

#pip3 install gspread oauth2client requests

HEIZUNG_IP = "192.168.2.58"
ROOMS = ['Jan', 'Tobi', 'Esszimmer', 'WohnzimmerMitte']
SHEET_NAME = 'Temperaturverlauf'
CRED_FILE = '/home/jan/heizung-263720-d8d64f9bb924.json'

import requests
import math
import time
import datetime
import gspread
from oauth2client.service_account import ServiceAccountCredentials

myTime = time.time()
outsidetemp = math.nan
try:
    r = requests.get('http://api.openweathermap.org/data/2.5/weather?lat=49.5010611&lon=8.6588786&units=metric&APPID=76767cf8c05205f646b3c4699ea2030e')
    outsidetemp = r.json()['main']['temp']
except:
    print('Error fetching outside temp')

print("Outside: " + str(outsidetemp))

now = datetime.datetime.fromtimestamp(myTime)
row = [now.strftime("%d.%m.%Y %H:%M:%S"),outsidetemp]

try:
    r = requests.get('http://' + HEIZUNG_IP + '/state')
    heatingstate = r.json()

    for room in ROOMS:
        temp = math.nan
        on = 0
        try:
            roomJson = heatingstate[room]
            print("Room %s: %s" % (room, str(roomJson)))
            on = int(roomJson['on'])
            lastTempUpdate = roomJson['lastTempUpdate']
            if lastTempUpdate != 0:
                updateAgo = myTime - lastTempUpdate // 1000
                print("Last temprature Update %d seconds ago" % updateAgo)
                if updateAgo < 15 * 60:
                    temp = roomJson['currentTemprature']
                else:
                    print('Last temprature Update more than 15 min ago')

        except Exception as e:
            print('Error getting state for ' + room)
            print(e)

        row.append(temp)
        row.append(on)
except:
    print('Error getting state from heating')

print(row)

# use creds to create a client to interact with the Google Drive API
scopes = ['https://www.googleapis.com/auth/spreadsheets', "https://www.googleapis.com/auth/drive.file",
          "https://www.googleapis.com/auth/drive"]

print("Authorizing")
creds = ServiceAccountCredentials.from_json_keyfile_name(CRED_FILE, scopes)
client = gspread.authorize(creds)
sheet = client.open(SHEET_NAME).get_worksheet(0)
print("Writing to sheet")
sheet.insert_row(row, 2, value_input_option='USER_ENTERED')
print('finished')