#import serial
import random
#gps = serial.Serial("/dev/ttyUSB0", baudrate=9600, timeout=1)
#gps.close()

def readgps():
    gps = serial.Serial("/dev/ttyUSB0", baudrate=9600)
    latlon = [0,0]
    lat = 0
    lon = 0
    data = []
    #print data
    line = gps.readline()
    #print line
    data = line.split(",")
    #print data
    if data[0] == "$GPRMC":
        if data[2] == "A":
            #Latitud
            latgps = float(data[3])
            if data[4] == "S":
                latgps = -latgps
            latdeg = int(latgps/100)
            latmin = latgps - latdeg*100
            lat = latdeg + (latmin/60)
                
            #Longitud
            longps = float(data[5])
            if data[6] == "W":
                longps = -longps
            londeg = int(longps/100)
            lonmin = longps - londeg*100
            lon = londeg + (lonmin/60)
            
            latlon[0]=lat    
            latlon[1]=lon    

    return latlon

def readgpsOffline():
    lat = random.uniform(-16.37, -16.42)
    lon = random.uniform(-71.47, -71.54)
    latlon = [0,0]
    latlon[0] = lat
    latlon[1] = lon
    return latlon
    
