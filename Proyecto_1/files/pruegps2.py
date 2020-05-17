import serial
import MySQLdb
import time

#gps = serial.Serial("/dev/ttyUSB0", baudrate=9600, timeout=1)
#gps.close()
global fecha, hora, lat, lon, horo

def readgps():
    gps = serial.Serial("/dev/ttyUSB0", baudrate=9600)
    while True:
        fecha=""
        hora=""
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
                fecha = data[9]
                print type(fecha)
                hora = data[1]
                print "fecha: ",fecha
                print "hora: ", hora
                latgps = float(data[3])
                if data[4] == "S":
                    latgps = -latgps
                latdeg = int(latgps/100)
                latmin = latgps - latdeg*100
                lat = latdeg + (latmin/60)
                print "Latitud", lat

                longps = float(data[5])
                if data[6] == "W":
                    longps = -longps
                londeg = int(longps/100)
                lonmin = longps - londeg*100
                lon = londeg + (lonmin/60)
                print "Longitud", lon
                gps.close()
                return fecha, hora, lat, lon

def sendgpsdata():
    db = MySQLdb.connect("www.e2i.com.pe","olcqsxdf_usuario","Usuario10x","olcqsxdf_luminarias")
    cursor = db.cursor()
    sql = "INSERT INTO DATAONLINE (EQUIPO, FECHA, HORA, LATITUD, LONGITUD, HOROMETRO)\
           VALUES ('%s','%s','%s','%f','%f','%f')" % \
           ('LG-023', fecha, hora, lat, lon, 244.3)
    cursor.execute(sql)
    db.commit()
    db.close()
    print "send data"
    return

while True:
    (fecha, hora, lat, lon)=readgps()
    sendgpsdata()
    time.sleep(2)
    
print "fin"
