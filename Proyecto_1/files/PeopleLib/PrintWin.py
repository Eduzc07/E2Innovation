import cv2
import numpy as np
import datetime
import os

full_path = os.path.realpath(__file__)
Filepath = os.path.dirname(full_path)

# PIO = np.loadtxt('%s/../../Data/IOArea.config'%Filepath,np.float32) 

# pin = np.zeros((3,2),dtype=np.float32)
# pin[0] = PIO[0]
# pin[1] = PIO[1]
# pin[2] = PIO[2]

# pout = np.zeros((3,2),dtype=np.float32)
# pout[0] = PIO[3]
# pout[1] = PIO[4]
# pout[2] = PIO[5]

# pin = np.array([[0.0,240.0],[300.0,240.0],[640.0,240.0]]) #Definiendo posicion y tamano del area de entrada
# pout = np.array([[0.0,140.0],[150.0,140.0],[640.0,140.0]]) #Definiendo posicion y tamano del area de salida

pi=200.0
po=pi-1
pin = np.array([[0.0,pi],[300.0,pi],[640.0,pi]]) #Definiendo posicion y tamano del area de entrada
pout = np.array([[0.0,po],[150.0,po],[640.0,po]]) #Definiendo posicion y tamano del area de salida
#Erase
pol = np.array([[0.0,0.0],[0.0,50.0],[640.0,50.0],[640,0] ], np.int32)

# pin=np.array([[400.0,360.0],[500.0,50.0],[640.0,50.0]])
# pout=np.array([[0.0,360.0],[290.0,360.0],[300.0,0.0]])
# #EraseW
# pol = np.array([[0,0], [0,260], [394,220],[410,0] ], np.int32)
# dposx=10

# pin=np.array([[0.0,360.0],[360.0,174.0],[640.0,400.0]])
# pout=np.array([[0.0,160.0],[390.0,90.0],[400.0,0.0]])
# #Erase
# pol = np.array([[400,0], [380,194], [640,400],[640,0] ], np.int32)
# dposx=420

def eq(valx):
    # Solo si todos los puntos atraviesan toda la imagen de izquierda a derecha
    if(valx<pin[1,0]):#Evaluando en que tramo se encuentra 
        mi1=(pin[1,1]-pin[0,1])/(pin[1,0]-pin[0,0])#Hallando la pendiente 
        valiy=mi1*(valx-pin[0,0])+pin[0,1]#Ecuacion de la recta uno de ingreso

    if(valx>=pin[1,0]):#Evaluando en que tramo se encuentra 
        mi2=(pin[2,1]-pin[1,1])/(pin[2,0]-pin[1,0])#Hallando la pendiente 
        valiy=mi2*(valx-pin[1,0])+pin[1,1]#Ecuacion de la recta dos de ingreso

    if(valx<pout[1,0]): #Evaluando en que tramo se encuentra 
        mo1=(pout[1,1]-pout[0,1])/(pout[1,0]-pout[0,0]) #Hallando la pendiente 
        valoy=mo1*(valx-pout[0,0])+pout[0,1] #Ecuacion de la recta uno de salida

    if(valx>=pout[1,0]): #Evaluando en que tramo se encuentra 
        mo2=(pout[2,1]-pout[1,1])/(pout[2,0]-pout[1,0]) #Hallando la pendiente
        valoy=mo2*(valx-pout[1,0])+pout[1,1] #Ecuacion de la recta dos de salida

    return valiy,valoy  



def printAreas(vis):
    height,width,d=vis.shape

    overlay = np.zeros((height,width,3), np.uint8)
    cv2.polylines(vis, [np.int32(pin)], False, (0, 255, 0),2)       # Dibuja los limites del area de ingreso
    ppin=np.array([[0,480],pin[0],pin[1],pin[2],[640,480]],np.int)  # Define los contornos del area de ingreso
    cv2.fillPoly(overlay,[ppin],(0,255,0))                          # Colorea el area de ingreso (0,255,0) color verde bgr

    cv2.polylines(vis, [np.int32(pout)], False, (0, 0, 255),2)      # Dibuja los limites del area de salida
    ppout=np.array([[0,0],pout[0],pout[1],pout[2],[640,0]],np.int)  # Define los contornos del area de salida
    cv2.fillPoly(overlay,[ppout],(0,0,255))                         # Colorea el area de salida(0,0,255) color rojo bgr

    vis = cv2.addWeighted(vis, 0.9, overlay, 0.1, 0.0, dtype=cv2.CV_8UC3)  #Calcula la suma de vis+overlay y la coloca como nuevo vis
    return vis

def printTracking(vis,blank_image,puntos,puntos2):
    if(len(puntos)>0):#Si longitud de puntos mayor que cero
        npuntos=np.int32(puntos)#Velocidad de integracion de puntos
        pi=(npuntos[0,0],npuntos[0,1])#Posicion punto inicial 
        pf=(npuntos[len(npuntos)-1,0],npuntos[len(npuntos)-1,1])#Pocision punto Final
        cv2.circle(vis, pi, 8, (0,0,255), -1)#Crea un circulo color rojo en el punto inicial
        # cv2.polylines(vis, [npuntos], False, (255, 255, 255),1)
        cv2.polylines(blank_image, [npuntos], False, (255, 255, 255),1)#Traza una linea color blanco en blank_image
        cv2.circle(vis, pf, 8, (0,255,0), -1)#Crea un circulo color Verde en el punto final 
        cv2.line(vis,pi,pf,(0,255,255),4)#Crea una linea color Aqua del pi al pf

    if(len(puntos2)>0):#Si longitud de puntos2 mayor que cero
        npuntos=np.int32(puntos2)#Velocidad de integracion de puntos2
        pi=(npuntos[0,0],npuntos[0,1])
        pf=(npuntos[len(npuntos)-1,0],npuntos[len(npuntos)-1,1])
        cv2.circle(vis, pi, 8, (255,0,0), -1)
        # cv2.polylines(vis, [npuntos], False, (255, 255, 255),1)
        # cv2.polylines(blank_image, [npuntos], False, (255, 255, 255),1)                
        cv2.circle(vis, pf, 8, (0,255,0), -1)
        cv2.line(vis,pi,pf,(0,255,255),4)#Crea una linea color Aqua del pi al pf
        
    ##Add a tracking
    vis = cv2.addWeighted(vis, 0.9, blank_image, 0.6, 0.0, dtype=cv2.CV_8UC3)#Crea una nueva imagen vis a partir de vis anterior + blank_image
    return vis


def printWindow(vis,pup,pdown,count,time):
    # dposx=10
    # cv2.putText(vis,"counts: %d" % count, (dposx,30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
    # cv2.putText(vis,"#Subieron: %d" % pup, (dposx,60), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
    # cv2.putText(vis,"#Bajaron: %d" % pdown, (dposx,90), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
    # cv2.putText(vis,"#Total: %d" % (pup-pdown), (dposx,120), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
    # cv2.putText(vis,'Fecha: %s' % datetime.datetime.now(), (dposx,150), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (255,255,255),1)
    # cv2.putText(vis,"delay: %0.3f ms" % (time*1000), (dposx,180), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1) #Se muestra el delay

    # # Colorea el area de ingreso (0,255,0) color verde bgr
    # cv2.fillPoly(vis,[pol],(0,0,0))
    # # En la ventana PeopleCounting incluye el texto definiendo posicion, tipo de letra y tamano
    # cv2.putText(vis,"#Subieron: %d" % pup, (dposx,45), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255,255,255),1) 
    # # En la ventana PeopleCounting incluye el texto definiendo posicion, tipo de letra y tamano
    # cv2.putText(vis,"#Bajaron: %d" % pdown, (130,45), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255,255,255),1)
    # # En la ventana PeopleCounting incluye el texto definiendo posicion, tipo de letra y tamano
    # cv2.putText(vis,"#Total: %d" % (pup-pdown), (270,45), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255,255,255),1)
    # # En la ventana PeopleCounting muestra la fecha del ordenador definiendo posicion, tipo de letra y tamano 
    # cv2.putText(vis,' %s' % datetime.datetime.now().strftime("%H:%M:%S Fecha: %d-%m-%y"), (400,45), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255,255,255),1)
    # cv2.putText(vis,"E2 Innovation SRL - Conteo de Pasajeros", (160,20), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255,255,255),2)


    ##Background
    background = cv2.imread('%s/../sources/background.png'%Filepath) 

    posx = 210
    if(pup < 0 or pup >9):
        posx = 196

    # En la ventana PeopleCounting incluye el texto definiendo posicion, tipo de letra y tamano
    cv2.putText(background,"%d" % pup, (posx,45), cv2.FONT_HERSHEY_SIMPLEX, 1.25, (255,255,255),2) 

    posx = 210
    if(pdown < 0 or pdown >9):
        posx = 196

    # En la ventana PeopleCounting incluye el texto definiendo posicion, tipo de letra y tamano
    cv2.putText(background,"%d" % pdown, (posx,97), cv2.FONT_HERSHEY_SIMPLEX, 1.25, (255,255,255),2)

    posx = 210
    if(pup-pdown < 0 or pup-pdown >9):
        posx = 196

    # En la ventana PeopleCounting incluye el texto definiendo posicion, tipo de letra y tamano
    cv2.putText(background,"%d" % (pup-pdown), (posx,147), cv2.FONT_HERSHEY_SIMPLEX, 1.25, (255,255,255),2)
    # En la ventana PeopleCounting muestra la fecha del ordenador definiendo posicion, tipo de letra y tamano 
    cv2.putText(background,'%s' % datetime.datetime.now().strftime("%d/%m/20%y"), (280,210), cv2.FONT_HERSHEY_SIMPLEX, 0.75, (255,255,255),2)
    cv2.putText(background,'%s' % datetime.datetime.now().strftime("%H:%M:%S"), (285,248), cv2.FONT_HERSHEY_SIMPLEX, 0.75, (255,255,255),2)


    factor = 0.5
    height,width,d = background.shape
    height,width = [height/2,width/2]
    frame = cv2.resize(background, (width,height), fx=factor, fy=factor, interpolation=cv2.INTER_AREA) #Reescalamiento de imagen
    
    blank_image = np.zeros(vis.shape, np.uint8)
    
    height,width,d = frame.shape

    #Posiciones del contador
    # [py,px]=[0,vis.shape[1]-width] #Arriba derecha
    # [py,px]=[0,0] #arriba izquierda
    [py,px]=[vis.shape[0]-height,0] #Abajo izquierda

    # blank_image[py:py+height,px:px+width] = frame
    # vis = cv2.addWeighted(vis,1.0,blank_image,2.0,0)
    
    vis[py:py+height,px:px+width] = frame

    return vis


def win(winName):
    cv2.namedWindow("Counting People"); #Crea la Ventana Counting People
    cv2.moveWindow("Counting People", 1425,20); #Mueve Counting People a la posicion deseada en la pantalla

    # cv2.namedWindow("win1");#Crea la Ventana win1
    # cv2.moveWindow("win1", 1425,490);#Mueve win1 a la posicion deseada en la pantalla

    cv2.namedWindow("win2");#Crea la Ventana win2
    cv2.moveWindow("win2", 700,20);#Mueve win2 a la posicion deseada en la pantalla

    cv2.namedWindow("win3");#Crea la Ventana win3
    cv2.moveWindow("win3", 0,20);#Mueve win3 a la posicion deseada en la pantalla
    
    cv2.namedWindow(winName, cv2.WINDOW_AUTOSIZE)     # Ajusta la ventana para cuadrar con las imagenes mostradas
    #Crea la Ventana Movement Indicator
    cv2.moveWindow(winName, 2065,490);#Mueve Movement Indicator a la posicion deseada en la pantalla

