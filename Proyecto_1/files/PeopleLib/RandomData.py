#!/usr/bin/env python
# -*- coding: utf-8 -*
'''
====================
Create Random data 
====================
RandomData.py <mes> <ultimo dia del mes>
 '''
import random
import sys
import numpy as np

from sendData import *
from saveData import *

UnitNames = np.chararray([0,5], itemsize=10)
#UnitNames = ["Bus-103","Bus-104","Bus-105","Bus-106","Bus-107","Bus-108","Bus-109","Bus-110"]
UnitNames = ["Bus-101","Bus-102","Bus-103","Bus-104","Bus-105","Bus-106","Bus-107","Bus-108","Bus-109","Bus-110"]


def printHead():
    print('------------------------------------------------------------------------')
    print('[ Equipo ][  Fecha    Hora  ][subió][bajó][   latitud   ][  longitud  ]')
    print('------------------------------------------------------------------------')


def readgpsOffline(pos):
    lat = np.array([-16.3868, -16.3800, -16.3757, -16.3560, -16.3486, -16.3238,-16.3208, -16.3168,-16.3122,-16.3073])
    lon = np.array([-71.5536, -71.5568, -71.5575, -71.5659, -71.5603, -71.5632, -71.5537,-71.5500, -71.5480,-71.5499])
    latlon = [0,0]
    latlon[0] = lat[pos]
    latlon[1] = lon[pos]
    return latlon


def main():             # App VideoSource
    print(__doc__)

    finFecha = 31
    month = 1
    try:
        month = int(sys.argv[1])
        finFecha = int(sys.argv[2])
    except:
        print("Error!!!!")
        print("Set month and last day.")
        sys.exit()

    printHead()

    numBus = 0  # Numero de Equipo
    hi = 5      # Hora de Inicio
    hf = 23     # Hora final
    m = 0       # minutos
    numPersonas = 0 # Numero de personas


    for numDia in range(1,finFecha + 1):
        numBus = 0 # Numero de Equipo
        h = hi
        m = 0
        while(True):
            val = random.randint(1, 2) # Random subio o bajo
            if val == 1:
                if numPersonas>=60:
                    val = 2
                    numPersonas-=1
                else:
                    #A partir de las 21 ya no siguen subiendo muchas personas
                    if (h >= 21 and numPersonas > 0):
                        val = 2
                        numPersonas-=1
                    else:
                        numPersonas+=1
            else:
                if numPersonas<1:
                    val = 1
                    numPersonas+=1
                else:
                    numPersonas-=1

            name = UnitNames[numBus]

            dayArray=np.array([numDia,month,17])    
            day = ("20%d-%02d-%02d"%(dayArray[2],dayArray[1],dayArray[0]))
            
            # Frecuencia con la que suben o bajan
            if (h < 7):
                jm = random.randint(0, 15) 

            if (h >= 7 and h < 9):
                jm = random.randint(0, 3) 

            if (h >= 9 and h < 12):
                jm = random.randint(0, 20) 

            if (h >= 12 and h < 16):
                jm = random.randint(0, 5) 

            if (h >= 16 and h < 21):
                jm = random.randint(0, 3) 

            if (h >= 21):
                jm = random.randint(0, 15) 


            m += jm 
            s = random.randint(0, 59)
            
            if (m >= 59):
                m = 0
                h += 1
            
            time = ("%s %02d:%02d:%02d"%(day,h,m,s))
            # print("%s-Total de personas: %d"%(time,numPersonas))

            if (h < 9):
                pos = random.randint(0, 4)

            if (h >= 9 and h < 12):
                pos = random.randint(0, 9)

            if (h >= 12 and h < 16):
                pos = random.randint(4, 9)            

            if (h >= 16 and h < 21):
                pos = random.randint(6, 9)            

            if (h >= 21):
                pos = random.randint(0, 9)            

            latlon = readgpsOffline(pos)

            WriteDataRandom(val,name,time,latlon[0],latlon[1],dayArray[0],dayArray[1])              # Save data

            if (h >= hf):
                #Todas las personas deben bajar al final
                # print("Personas en el bus: %d"%numPersonas)
                # print("%s %s: #Personas final del dia: %d"%(name,time,numPersonas))
                while(numPersonas > 0):
                    val = 2
                    numPersonas-=1
                    # print("%s-Total de personas: %d"%(time,numPersonas))
                    WriteDataRandom(val,name,time,latlon[0],latlon[1],dayArray[0],dayArray[1])              # Save data
                print("%s %s: #Personas final del dia: %d"%(name,time,numPersonas))
                numBus += 1 
                h = hi
                
            if numBus == len(UnitNames):
                break

            # if val==1:
            #     print('[ %s ][%s]   1      0      %0.4f       %0.4f' %(name,time,latlon[0],latlon[1]))
            # else:
            #     print('[ %s ][%s]   0      1      %0.4f       %0.4f' %(name,time,latlon[0],latlon[1]))
            
        # sendMail()
        
    sys.exit()

if __name__ == '__main__':
    main()
