import random
import datetime
from time import clock
import numpy as np
import time

la=np.array([16.3868,16.3800,16.3757,16.3560,16.3486])
lo=np.array([71.5536,71.5568,71.5575,71.5659,71.5603])

def escribir():
	archivo = open('../Data/datos.txt','w')
	archivo.write('   Fecha     Hora     Subio     Bajo          Latitud            Longitud\n')
	for i in range (0,3):
		laa=la[i+1]
		loo=lo[i+1]
		s=random.randint(0,20)
		b=random.randint(0,20)
		archivo.write('  %s' %datetime.datetime.now().strftime("%y-%m-%d  %H:%M:%S")):q
		archivo.write('   %d'%s)
		archivo.write('        %d'%b)
		archivo.write('         %0.4f' %laa)
		archivo.write('                 %0.4f\n' %loo)
		time.sleep(5)
	for j in range (0,4):
		laa=la[-(j+1)]
		loo=lo[-(j+1)]
		s=random.randint(0,20)
		b=random.randint(0,20)
		archivo.write('  %s' %datetime.datetime.now().strftime("%y-%m-%d  %H:%M:%S"))
		archivo.write('   %d'%s)
		archivo.write('        %d'%b)
		archivo.write('         %0.4f' %laa)
		archivo.write('                 %0.4f\n' %loo)
		time.sleep(5)

print("procesando")
escribir()
