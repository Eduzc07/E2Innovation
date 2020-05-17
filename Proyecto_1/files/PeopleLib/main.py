#!/usr/bin/env python
# -*- coding: utf-8 -*
'''
====================
   People counting
====================

Keys
----
ESC - exit
 s  - Save Image

 ------------------------------------
'''
from __future__ import print_function
from people_counting_v2 import App

import sys,os
import numpy as np
import csv

full_path = os.path.realpath(__file__)
Filepath = os.path.dirname(full_path)

def createCustomerData():
    filename = "%s/../../Data/CustomerData.csv"%Filepath
    Log = "[Loggin] "
    if not os.path.isfile(filename):
        print("%sNo existen Datos de Usuario."%Log)
        print("%sLos datos seran creados a continuación: "%Log)
        print("%sEjemplo:\n"%Log)
        print("          Nombre: Mario Eduardo ")
        print("          Apellidos: Zarate Caceres ")
        print("          Correo: ezarate@e2i.com.pe")
        print("          Equipo: Bus-001\n\n\n")

        fileInfo = open(filename,'wb')
        name = raw_input("%sNombre: "%Log)
        Lastname = raw_input("%sApellidos: "%Log)
        email = raw_input("%sCorreo: "%Log)
        equipo = raw_input("%sNombre de equipo: "%Log)

        #Saving data
        fileInfo.write('Nombres,%s\n'%(name))
        fileInfo.write('Apellidos,%s\n'%(Lastname))
        fileInfo.write('Correo,%s\n'%(email))
        fileInfo.write('Equipo,%s\n'%(equipo))
        fileInfo.close()
        print("%sLos datos de usuario fueron creados."%Log)
    else:
        print("%sDatos de usuario encontrados."%Log)
        csv_data = csv.reader(file(filename))
        for row in csv_data:
            print("%s%s: %s"%(Log,row[0],row[1]))


def createFolders(Filepath):
    import os
    if not os.path.exists("%s/../../Videos"%Filepath):
        os.makedirs("%s/../../Videos"%Filepath)
        print("\"Videos\" Folder was created.")
    if not os.path.exists("%s/../../Images"%Filepath):
        os.makedirs("%s/../../Images"%Filepath)
        print("\"Images\" Folder was created.")
    if not os.path.exists("%s/../../Data"%Filepath):
        os.makedirs("%s/../../Data"%Filepath)
        print("\"Data\" Folder was created.")

    if not os.path.isfile("%s/../../Data/IOArea.config"%Filepath):
        print("IOArea.config does not exist")
        print("Creating default values.")
        pin = np.array([[0.0,240.0],[300.0,240.0],[640.0,240.0]])
        pout = np.array([[0.0,140.0],[150.0,140.0],[640.0,140.0]]) #Definiendo posicion y tamano del area de salida
        pdat =  np.append(pin, pout,axis=0)
        np.savetxt('%s/../../Data/IOArea.config'%Filepath, pdat)   # X is an array
        
def main():             # App VideoSource
    try:
        video_src = sys.argv[1]
    except:
        video_src = '0'

    print(__doc__)

    createFolders(Filepath)
    createCustomerData()
    
    App(video_src).run()
    sys.exit()

if __name__ == '__main__':
    main()

