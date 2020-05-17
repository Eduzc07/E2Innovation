# -*- coding: utf-8 -*
import datetime
import os

full_path = os.path.realpath(__file__)
Filepath = os.path.dirname(full_path)

def WriteData(val,name,lat,lon):
	filename = "%s/../../Data/data_%s.csv"%(Filepath,datetime.datetime.now().strftime("%d_%m"))
	if os.path.isfile(filename):
		file = open(filename,'ab')
	else:
		file = open(filename,'wb')
		file.write('equipo,fechahora,subio,bajo,latitud,longitud\n')
	if val==1: 
		file.write('%s,%s,1,0,%0.4f,%0.4f\n' %(name,datetime.datetime.now().strftime("20%y-%m-%d  %H:%M:%S"),lat,lon)) #subio
	elif val==2:
		file.write('%s,%s,0,1,%0.4f,%0.4f\n' %(name,datetime.datetime.now().strftime("20%y-%m-%d  %H:%M:%S"),lat,lon)) #bajo
	file.close()



def WriteDataRandom(val,name,time,lat,lon,day,month):
	# filename = "%s/../../Data/data_%s.csv"%(Filepath,datetime.datetime.now().strftime("%d_%m"))
	filename = "%s/../../Data/data_%02d_%02d.csv"%(Filepath,day,month)
	if os.path.isfile(filename):
		file = open(filename,'ab')
	else:
		file = open(filename,'wb')
		file.write('equipo,fechahora,subio,bajo,latitud,longitud\n')
	if val==1: 
		file.write('%s,%s,1,0,%0.4f,%0.4f\n' %(name,time,lat,lon)) #subio
	elif val==2:
		file.write('%s,%s,0,1,%0.4f,%0.4f\n' %(name,time,lat,lon)) #bajo
	file.close()