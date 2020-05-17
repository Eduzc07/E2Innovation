#!/usr/bin/env python
# -*- coding: utf-8 -*
'''
People counting
====================

Keys
----
ESC - exit
 s  - Save Image
'''

# Python 2/3 compatibility
from __future__ import print_function

import numpy as np
import cv2
import video
import datetime
from common import anorm2, draw_str
from time import clock

pin=np.array([[0.0,360.0],[380.0,194.0],[640.0,400.0]])
pout=np.array([[0.0,160.0],[390.0,90.0],[400.0,0.0]])
#Erase
pol = np.array([[400,0], [380,194], [640,400],[640,0] ], np.int32)



def eq(valx):
    if(valx<pin[1,0]):
        mi1=(pin[1,1]-pin[0,1])/(pin[1,0]-pin[0,0])
        valiy=mi1*(valx-pin[0,0])+pin[0,1]

    if(valx>=pin[1,0]):
        mi2=(pin[2,1]-pin[1,1])/(pin[2,0]-pin[1,0])
        valiy=mi2*(valx-pin[1,0])+pin[1,1]

    if (valx<400):    
        if(valx<pout[1,0]):
            mo1=(pout[1,1]-pout[0,1])/(pout[1,0]-pout[0,0])
            valoy=mo1*(valx-pout[0,0])+pout[0,1]

        if(valx>=pout[1,0]):
            mo2=(pout[2,1]-pout[1,1])/(pout[2,0]-pout[1,0])
            valoy=mo2*(valx-pout[1,0])+pout[1,1]
    else:
        valoy=0
    return valiy,valoy    


def diffImg(t0, t1, t2):
    d1 = cv2.absdiff(t2, t1)
    d2 = cv2.absdiff(t1, t0)
    diff=cv2.bitwise_and(d1, d2)
    return diff

def checkMov(pi,pf):
    iviy,ivoy=eq(pi[0])
    # print("values yi:")
    # print(pi[1],iviy)
    # print(pi[1],ivoy)
    fviy,fvoy=eq(pf[0])
    # print("values yf:")
    # print(pf[1],fviy)
    # print(pf[1],fvoy)

    mov=0
    if(pi[1] < ivoy and pf[1] > fviy):
        mov=1
    
    if(pi[1] > iviy and pf[1] < fvoy):
        mov=2
    

    return mov


class App:
    def __init__(self, video_src):
        self.track_len = 10
        self.detect_interval = 5
        self.tracks = []
        self.cam = video.create_capture(video_src)
        self.frame_idx = 0

    def run(self):

        winName = "Movement Indicator"
        cv2.namedWindow(winName, cv2.WINDOW_AUTOSIZE)   

        # cap = cv2.VideoCapture('test1.mp4')
        cap = cv2.VideoCapture('IMG_4578.MOV')

        #=======================================================================
        #save
        # Define the codec and create VideoWriter object
        # fourcc = cv2.VideoWriter_fourcc(*'XVID')
        # out = cv2.VideoWriter('output.avi',fourcc, 20.0, (640,480))
        #=======================================================================
        
        # Read three images first:
        ret, camv = cap.read()
        framev=cv2.resize(camv, (640,480), fx=0.25, fy=0.25, interpolation=cv2.INTER_AREA)

        t_minus = cv2.cvtColor(framev, cv2.COLOR_RGB2GRAY)
        t = cv2.cvtColor(framev, cv2.COLOR_RGB2GRAY)
        t_plus = cv2.cvtColor(framev, cv2.COLOR_RGB2GRAY)

        blank_image = np.zeros((480,640,3), np.uint8)

        color=0
        puntos=[]
        count=0
        nframes=0

        pup=0
        pdown=0
        op=0
        
        while True:
            e1 = cv2.getTickCount()
            # ret, frame = self.cam.read()
            ret, camf = cap.read()
            frame=cv2.resize(camf, (640,480), fx=0.25, fy=0.25, interpolation=cv2.INTER_AREA)

            #Erase window
            cv2.fillPoly(frame,[pol],1)

            frame_gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            vis = frame.copy()

            im=diffImg(t_minus, t, t_plus)

            blur = cv2.blur(im,(5,5))

            ret,thresh = cv2.threshold(blur,60,255,0) ##<------------------------- Set best value
            im2, contours, hierarchy = cv2.findContours(thresh, 1, 2)
            cv2.drawContours(vis, contours, -1, (0,255,0), 3)

            # cv2.line(vis,(640,80),(0,360),(0,255,255),4)
            
            cv2.polylines(vis, [np.int32(pin)], False, (0, 255, 0),2)
            ppin=np.array([[0,480],pin[0],pin[1],pin[2],[640,480]],np.int)

            overlay = np.zeros((480,640,3), np.uint8)
            cv2.fillPoly(overlay,[ppin],(0,255,0))

            cv2.polylines(vis, [np.int32(pout)], False, (0, 0, 255),2)
            ppout=np.array([[0,0],pout[0],pout[1],pout[2],[640,0]],np.int)
            cv2.fillPoly(overlay,[ppout],(0,0,255))

            vis = cv2.addWeighted(vis, 0.9, overlay, 0.1, 0.0, dtype=cv2.CV_8UC3)

            aream=0
            for cnt in contours:
                area = cv2.contourArea(cnt)                  
                if (area>0): ##<------------------------- Set best value
                    M = cv2.moments(cnt)
                    cx = int(M['m10']/M['m00'])
                    cy = int(M['m01']/M['m00'])
                    cv2.circle(vis, (cx, cy), 5, (0,0,255), -1)
                    cv2.circle(blank_image, (cx, cy), 5, (0,255-color,255), -1)
                    color+=2
                    if area>aream:
                        puntos.append([cx, cy])

                    aream=area    
                    count=0
                    break


            count+=1   

            if(len(puntos)>0):
                npuntos=np.int32(puntos)
                pi=(npuntos[0,0],npuntos[0,1])
                pf=(npuntos[len(npuntos)-1,0],npuntos[len(npuntos)-1,1])
                cv2.circle(vis, pi, 8, (0,0,255), -1)
                # cv2.polylines(vis, [npuntos], False, (255, 255, 255),4)
                cv2.circle(vis, pf, 8, (0,255,0), -1)
                cv2.line(vis,pi,pf,(0,255,255),4)
            

            # Read next image
            nframes+=1
            #demorar 10 frames para percibir movimientos lentos
            if nframes>10:
                nframes=0
                t_minus = t
                t = t_plus
                gray = cv2.cvtColor(frame, cv2.COLOR_RGB2GRAY)
                t_plus = cv2.equalizeHist(gray)
            draw_str(vis, (20, 20), 'counts: %d' % count)


            cv2.putText(vis,"#Subieron: %d" % pup, (410,30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
            cv2.putText(vis,"#Bajaron: %d" % pdown, (410,60), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
            cv2.putText(vis,"#Total: %d" % (pup-pdown), (410,90), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
            cv2.putText(vis,'Fecha: %s' % datetime.datetime.now(), (410,150), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (255,255,255),1)

            if(count>40):
                if(len(puntos)>0):
                    npuntos=np.int32(puntos)    
                    pi=(npuntos[0,0],npuntos[0,1])
                    pf=(npuntos[len(npuntos)-1,0],npuntos[len(npuntos)-1,1])
                    val=checkMov(pi,pf)
                    if val==1:
                        print('Una persona subio (%s)' % datetime.datetime.now())
                        pup+=1
                        cv2.imwrite('image%d.png'%op,vis)
                        print("Image Saved!")
                        op+=1
                    elif val==2:
                        print('Una persona bajo (%s)' % datetime.datetime.now())
                        pdown+=1
                        cv2.imwrite('image%d.png'%op,vis)
                        print("Image Saved!")
                        op+=1

                blank_image = np.zeros((480,640,3), np.uint8) 
                puntos=[]
                count=0
                color=0



            cv2.imshow(winName, blank_image)
            cv2.imshow("winName", thresh)
            

            e2 = cv2.getTickCount()
            time = (e2 - e1)/cv2.getTickFrequency()
            cv2.putText(vis,"delay: %0.3f ms" % (time), (410,120), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)

            #=======================================================================
            # write the flipped frame
            # out.write(vis)
            #=======================================================================
            cv2.imshow('lk_track', vis)


            ch = 0xFF & cv2.waitKey(1)
            if ch == 27:
                break
            elif ch == ord('s'): # wait for 's' key to save and exit
                cv2.imwrite('image%d.png'%op,vis)
                print("Image Saved!")
                op+=1

def main():
    import sys
    try:
        video_src = sys.argv[1]
    except:
        video_src = 0

    print(__doc__)
    App(video_src).run()
    cv2.destroyAllWindows()

if __name__ == '__main__':
    main()
