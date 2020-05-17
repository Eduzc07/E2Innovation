# -*- coding: utf-8 -*
import cv2
import numpy as np
import datetime,os
import sys
from time import clock
from PrintWin import *
from saveData import *
from sendData import *
from gps import *

def printHead():
    print('------------------------------------------------------------------------')
    print('[ Equipo ][  Fecha    Hora  ][subió][bajó][   latitud   ][  longitud  ]')
    print('------------------------------------------------------------------------')

global save
def printRect(vis):
    pi=280#280/320
    po=120#100/130
    #arriba out
    pol = np.array([[0.0,0.0],[0.0,po],[640.0,po],[640,0] ], np.int32)
            
    #abajo in
    pol = np.array([[0.0,360.0],[0,pi],[640.0,pi],[640,360] ], np.int32)
            
    #Left
    pl=200
    pol = np.array([[0.0,0.0],[pl,0.0],[pl,360.0],[0,360] ], np.int32)
            
    #Right
    pr=480
    pol = np.array([[640.0,0.0],[pr,0.0],[pr,360.0],[640,360] ], np.int32)
            
            
    pp = 200
    cv2.rectangle(vis,(pl,po),(pr,pi),(155,0,0,0.5),3)            # Se crea el rectangulo con las condiciones dadas
    return vis

def ImageProc(frame,im):
    # Erase window
    # cv2.fillPoly(frame,[pol],1)

    frame_gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY) #Cambiando imagen a escala de grises 
    vis = frame.copy()                                   #Copiamos el frame reescalado
    
    # blur = cv2.blur(im,(9,9))           # Filtrado de imagen
    
    kernel = np.ones((21,21),np.uint8)  # Se pocisiona el negro como color dominante en la imagen
    ret,thresh = cv2.threshold(im,40,255,cv2.THRESH_BINARY) ##<------------------------- Set best value
    # opening = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel) #Erosion+Dilatacion para remover ruido de imagen
    # erosion = cv2.erode(thresh,kernel,iterations = 1)
    # dilation = cv2.dilate(thresh,kernel,iterations = 1)
    closing = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel) #Dilatacion+Erosion cerrar huecos en la imagen

    #cv2.imshow("win2",opening)#Aplicamos opening en win2
    #cv2.imshow("win1",closing)#Aplicamos closing en win1

    # img2_fg = cv2.cvtColor(closing,cv2.COLOR_GRAY2RGB)
    # vis = cv2.add(vis,img2_fg)
    

    # th3 = cv2.adaptiveThreshold(im,255,cv2.ADAPTIVE_THRESH_GAUSSIAN_C,cv2.THRESH_BINARY_INV,11,2)

    pi = 280#280/320
    po = 120#100/130
    height,width,d = frame.shape
    #arriba out
    pol = np.array([[0.0,0.0],[0.0,po],[width,po],[width,0] ], np.int32)
    cv2.fillPoly(closing,[pol],(0))

    #abajo in
    pol = np.array([[0.0,height],[0,pi],[width,pi],[width,height] ], np.int32)
    cv2.fillPoly(closing,[pol],(0))

    #Left
    pl = 200
    pol = np.array([[0.0,0.0],[pl,0.0],[pl,height],[0,height] ], np.int32)
    cv2.fillPoly(closing,[pol],(0))

    #Right
    pr = 480
    pol = np.array([[width,0.0],[pr,0.0],[pr,height],[width,height] ], np.int32)
    cv2.fillPoly(closing,[pol],(0))

    # pol = np.array([[0,190],[640,190],[640.0,170.0],[0,170] ], np.int32)
    # cv2.fillPoly(closing,[pol],(0))
    
    pp = 200
    cv2.rectangle(im,(pl,po),(pr,pi),255,3)            # Se crea el rectangulo con las condiciones dadas
    cv2.line(im,(0,pp),(640,pp),255,3)
    cv2.imshow("win2",im)
    global save
    save = im

    cv2.imshow("win3",closing)
    # cv2.line(vis,(640,80),(0,360),(0,255,255),4)
    return closing

def diffImg(t0, t1, t2):            # Restar dos cuadros continuos para detectar el movimiento   
    d1 = cv2.absdiff(t2, t1)        # Diferencia entre cuadro t2(Tiempo2)-t1(Tiempo1)
    d2 = cv2.absdiff(t1, t0)        # Diferencia entre cuadro t1(Tiempo1)-t0(Tiempo0)
    diff = cv2.bitwise_or(d1, d2)   # Diferencia de los dos conjuntos de cuadros
    return diff


def dist(a,b):
    x1 = a[0]
    y1 = a[1]
    x2 = b[0]
    y2 = b[1]
    return np.sqrt(np.sum((x1-x2)**2) + np.sum((y1-y2)**2))

#Add the point only if the point is not too far
def isOk(points,nPoint,distance):
    CurrentDistance = 0
    if len(points) == 0:
        return 0,True

    if len(points)==1:
        CurrentDistance = dist(nPoint,points[len(points)-1])
        return CurrentDistance,True
    
    NumSe = 3
    if len(points)>11:
        NumSe = 10

    for i in range(1,NumSe):
        CurrentDistance = dist(nPoint,points[len(points)-i])
        error = abs(CurrentDistance-distance)
        # print("%d: %0.4f"%(i,error)) 
        if (len(points)-i <=0):
            break
            
        if (error<35):
            distance = (CurrentDistance + distance)/(len(points)-i)
            return distance,True
    
    return distance,False
    
# Ver si la direccion del vector va de salida o entrada
def checkMov(pi,pf):                # Pi primer punto, Po punto final
    iviy,ivoy = eq(pi[0]) # y int value input / y int value output
    # print("values yi:")
    # print(pi[1],iviy)
    # print(pi[1],ivoy)
    fviy,fvoy = eq(pf[0])
    # print("values yf:")
    # print(pf[1],fviy)
    # print(pf[1],fvoy)

    mov = 0
    if(pi[1] < ivoy and pf[1] > fviy): # Saber si el objeto se encuentra dentro del area de entrada
        mov=1
    
    if(pi[1] > iviy and pf[1] < fvoy): # Saber si el objeto se encuentra dentro del area de salida
        mov=2
    
    return mov


class App: #Creacion de la clase App
    def __init__(self, video_src): #Inicializacion de clase con conjunto de variables video_src
        self.winName = "Movement Indicator"
        win(self.winName)
        # read argv input
        if (video_src.isdigit()):
            self.cap = cv2.VideoCapture(int(video_src))
        else:
            self.cap = cv2.VideoCapture(video_src)

        # cap = cv2.VideoCapture('test1.mp4')
        # cap = cv2.VideoCapture('IMG_5346.MOV')
        # cap = cv2.VideoCapture('IMG_5741.MOV')
        # cap = cv2.VideoCapture('../Videos/IMG_6051.MOV')
        #self.cap = cv2.VideoCapture('../Videos/IMG_7522.MOV')
        #set the width and height, and UNSUCCESSFULLY set the exposure time
        #self.cap.set(3,1280)
        #self.cap.set(4,1024)
        #self.cap.set(15, 0.1)

        
        if (not  self.cap.isOpened()):
            print ("[Error]: Camera is not connected")
            print ("[Info]: Se puede cargar un video")
            print ("[Info]: Ejemplo: ./init ../Videos/IMG_6051.MOV")
            sys.exit()

        ret, img = self.cap.read()
        height,width,d = img.shape
        # height,width=[height/4,width/4]
        
        # Reduce if the video is too big
        if(width > 800):
            self.height,self.width = [height/3,width/3]
        else:
            self.height,self.width = height , width

        print("Image: %d x %d"%(self.width,self.height))
        
        #=======================================================================
        full_path = os.path.realpath(__file__)
        self.Filepath = os.path.dirname(full_path)
        # save
        # Define the codec and create VideoWriter object
        fourcc = cv2.VideoWriter_fourcc(*'XVID') #fourcc se usa para especificar el tipo de video (XVID da videos pequenos)
        self.out = cv2.VideoWriter('%s/../../Videos/output.avi'%(self.Filepath),
                                    fourcc, 27.0, (self.width,self.height)) #Reproduce el video con VideoWriter
        #=======================================================================
        self.frame = cv2.resize(img, (self.width,self.height), fx=0.25, fy=0.25, interpolation=cv2.INTER_AREA) #Escala la imagen (img) a 25% en ambos ejes
        self.distance = 0
        configFile = "%s/../../Data/CustomerData.csv"%self.Filepath
        csv_data = csv.reader(file(configFile))
        for row in csv_data:
            if(row[0] =="Equipo"):
                self.name = row[1] # ID-name from the bus

        printHead()
                
    def run(self): #Corremos la clase
        # Read three images first:
        ret, img = self.cap.read()
        #-------------------------------------------------------------------------
        #Variables 
        #-------------------------------------------------------------------------
        t_minus = cv2.cvtColor(self.frame, cv2.COLOR_RGB2GRAY)  # Cambia el color a escala de grises (cuadro anterior)
        t = cv2.cvtColor(self.frame, cv2.COLOR_RGB2GRAY)        # Cambia el color a escala de grises (cuadro presente)
        t_plus = cv2.cvtColor(self.frame, cv2.COLOR_RGB2GRAY)   # Cambia el color a escala de grises (cuadro posterior)

        blank_image = np.zeros((self.height,self.width,3), np.uint8)  #Imagen no definida en color negro

        color = 0xFF    # To draw the colors in the image
        puntos = []     # Points to follow the movemente
        puntos2 = []    # Points to follow the movemente
        count = 0       # Numero de iteraciones para evaluar la direccion del vector
        nframes = 0     # Numero de cuadros de diferencia a mayor numero, se detectan movimientos lentos

        pup = 0         # Numero de personas subiendo
        pdown = 0       # Numero de personas bajando
        id = 0          # Numero de imagen almacenada, Para contar el numero de accion
        fps = 0         # Numero de cuadros por segundo
        e0 = cv2.getTickCount() # Calcula el numero de Ticks hasta el momento 
        op = 0
        #-------------------------------------------------------------------------
        # Main Loop
        #-------------------------------------------------------------------------
        while True:
            e1 = cv2.getTickCount() #Calcula el numero de Ticks hasta el momento 
            ret, cam = self.cap.read()

            if cam is None:
                print("Image empty, Video has finished.")
                break

            frame = cv2.resize(cam, (self.width,self.height), fx=0.25, fy=0.25, interpolation=cv2.INTER_AREA) #Reescalamiento de imagen
            vis = frame.copy() 

            im = diffImg(t_minus, t, t_plus)    # Diferencia entre los tres cuadros definidos antes
            #All the pre-image processing is here
            dst = ImageProc(frame,im)
                        
            im2, contours, hierarchy = cv2.findContours(dst, cv2.RETR_CCOMP , cv2.CHAIN_APPROX_NONE)#CHAIN_APPROX_NONE,CHAIN_APPROX_SIMPLE ,CHAIN_APPROX_TC89_L1
            # cv2.drawContours(vis, contours, -1, (0,255,0), -1) #Dibuja todos los contornos de la imagen

            aream = 0               # Area Previa
            ni = 0                  # Numero de Iteraciones
            xp,yp,wp,hp= 0,0,0,0    # cordenadas Previas

            for cnt in contours:
                area = cv2.contourArea(cnt)   # El contorno del area (area actual)                
                # print (area)
                if (area > 8000): ##<------------------------- Set best value

                    M = cv2.moments(cnt)            # moments da una lista de valores calculados de todos los momentos (Ejmp: Centro, masa o area del
                    cx = int(M['m10']/M['m00'])     # Calcula el centroide en X del objeto
                    cy = int(M['m01']/M['m00'])     # Calcula el centroide en Y del objeto 

                    x,y,w,h = cv2.boundingRect(cnt) # Se crea un rectangulo delimitador, x,y:coordenadas superior e izquierda - w,h:ancho y altura del rectangulo.
                  
                    cv2.putText(vis,'%0.2f - %d'  %(area,ni), (cx,cy-10), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (255,255,255),1)#Muestra el area del rectangulo (con dos decimales)-valor ni en el centroide del rectangulo, desplazado en Y-10
                    
                    color+=2
                    if (area>aream and ni==0):                              # Si area actual es menor al area previa
                        # cv2.rectangle(vis,(x,y),(x+w,y+h),(255,255,0),3)    # Se crea el rectangulo con las condiciones dadas

                        cv2.circle(vis, (cx, cy), 8, (255,0,0), 3)         # Dibuja un circulo de color Azul
                        self.distance, isPointOk = isOk(puntos,[cx, cy],self.distance)
                        if isPointOk:
                            puntos.append([cx, cy])                         # anexa los puntos de seguimiento en el centroide del rectangulo
                            count = 0               # reinicia el conteo break
                            aream = area            # Convierte el area previa en la actual

                            cv2.circle(vis, (cx, cy), 5, (0,0,255), -1)         # Dibuja un circulo de color Rojo
                            if color <255:
                                cv2.circle(blank_image, (cx, cy), 5, (0,255-color,255), -1) # Dibuja circulos que degradaran de rojo a amarillo.
                            else:
                                cv2.circle(blank_image, (cx, cy), 5, (0,color-510,510-color), -1)

                    if (area>aream and ni==1):# and (y<140 or y>260): # and h<hp and w<wp
                        cv2.rectangle(vis,(x,y),(x+w,y+h),(255,0,255),3)            # Se crea el rectangulo con las condiciones dadas
                        puntos2.append([cx, cy])                                    # anexa los puntos2 de seguimiento en el centroide del rectangulo
                        cv2.circle(vis, (cx, cy), 5, (255,0,0), -1)                 # Dibuja un circulo de color Azul
                        cv2.circle(blank_image, (cx, cy), 5, (255-color,0,0), -1)   # Dibuja circulos que degradaran en tonos de azul    

                    xp,yp,wp,hp = x,y,w,h   # Convierta las coordenadas previas en las actuales
                    ni =+ 1 # aumenta el numero de iteraciones en 1

            count += 1       # aumenta el conteo 

            #Print the input/output areas
            vis = printAreas(vis)
            vis = printTracking(vis,blank_image,puntos,puntos2)

            # Read next image
            nframes+=1
            # demorar 10 frames para percibir movimientos lentos
            if (nframes == 1):
                nframes = 0
                t_minus = t # t[n-1]
                t = t_plus  # t[n]
                #Current frame
                gray = cv2.cvtColor(frame, cv2.COLOR_RGB2GRAY)
                t_plus = cv2.equalizeHist(gray) # t[n+1]
            
            # Numero de iteraciones que se esperan antes de evaluar si el vector es de entrada o salida
            # Mientras menor sea el numero es mas sensible al movimiento
            # a mayor numero, puede confundir el movimiento de 2 personas
            if(count > 10): ##<------------------------- Set best value
                if(len(puntos)>0):
                    npuntos = np.int32(puntos)    
                    pi = (npuntos[0,0],npuntos[0,1])                            # Definiendo el punto inicial del vector de seguimiento
                    pf = (npuntos[len(npuntos)-1,0],npuntos[len(npuntos)-1,1])  # Definiendo el punto final del vector de seguimiento
                    val = checkMov(pi,pf)                                       # Punto Inicial Punto final entran al checkmov
                    if val != 0:
                        id+=1 # Add only if the person move up or down

                    latlon = readgpsOffline()
                    WriteData(val,self.name,latlon[0],latlon[1])              # Save data
                    if val==1:
                        pup += 1                                                  # Aumenta el contador de pasajeros que ingresan
                        #cv2.imwrite('../Images/image%d.png'%id,vis)             # Realizar screenshot de la ventana 
                        print('[ %s ][%s]   1      0      %0.4f       %0.4f' %(self.name,datetime.datetime.now().strftime("%d-%m-%y %H:%M:%S"),latlon[0],latlon[1]))
                    elif val==2:
                        pdown += 1                                                # Aumenta el contador de pasajeros que egresan
                        #cv2.imwrite('../Images/image%d.png'%id,vis)             # Realizar screenshot de la ventana 
                        print('[ %s ][%s]   0      1      %0.4f       %0.4f' %(self.name,datetime.datetime.now().strftime("%d-%m-%y %H:%M:%S"),latlon[0],latlon[1]))
                        
                # Reinicio de valores
                blank_image = np.zeros((self.height,self.width,3), np.uint8) 
                puntos = []
                puntos2 = []
                count = 0
                color = 0

            #=======================================================================
            #FPS
            # e3 = cv2.getTickCount()
            # time = (e3 - e0)/cv2.getTickFrequency()
            # fps+=1
            # if(time>1.0):
            #     e0 = cv2.getTickCount()
            #     print("fps:%d"%fps)
            #     fps=0
            #=======================================================================
            
            e2 = cv2.getTickCount()
            time = (e2 - e1)/cv2.getTickFrequency() #Calculamos el delay entre la reproduccion del video y el procesamiento en si 

            # Print Rectangle Area in window
            vis = printRect(vis)
            # Print in window
            vis = printWindow(vis,pup,pdown,count,time)

            #=======================================================================
            # write the flipped frame
            self.out.write(vis)
            #=======================================================================
            if sendMail():                              # Send a mil each day at 00:00
                printHead()

            cv2.imshow('Counting People', vis)      # Mostramos la sucesion de imagenes vis en la ventana CountingPeople
            cv2.imshow(self.winName, blank_image)   # Muestra la ventana con blank_image (Background estatico:color negro, Imagenes Dinamicas:color blanco)
            global save
            ch = 0xFF & cv2.waitKey(1) #Comando para terminar programaps
            if ch == 27:
                break
            elif ch == ord('s'):                    # wait for 's' key to save and exit
                cv2.imwrite('%s/../../Images/image%d.png'%(self.Filepath,op),vis)   # Guardar imagen 
                cv2.imwrite('%s/../../Images/win%d.png'%(self.Filepath,op),save)   # Guardar imagen 
                print("Image Saved!")               # Notificacion de guardado
                op+=1
        cv2.destroyAllWindows()            
