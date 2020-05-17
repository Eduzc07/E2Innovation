#!/usr/bin/env python
# -*- coding: utf-8 -*
'''
====================
Send Data
====================

'''
import random
import sys
import numpy as np

from sendData import *
from saveData import *

report = "[Mail Service]:"

def main():             # App VideoSource
    print(__doc__)

    iniFecha = 1 
    finFecha = 4 
    month = 9 
    try:
        month = int(sys.argv[1])
        iniFecha = int(sys.argv[2])
        finFecha = int(sys.argv[3])
        
    except:
        print("Error!!!!")
        print("Set month, first  day and last day if it is needed.")
        sys.exit()

    print("Preparing to send data from %d/%d - %d/%d"%(iniFecha,month,finFecha,month))
    
    for numDia in range(iniFecha,finFecha + 1):
        print("%s The data will be uploaded."%report)
        sendServerDataMonth(month,numDia)
        sendMailMonth(month,numDia,"edu.zc07@gmail.com")
        #sendMailMonth(month,numDia,"erodriguez@e2i.com.pe")
    sys.exit()

if __name__ == '__main__':
    main()
