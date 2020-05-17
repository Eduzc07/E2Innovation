#!/usr/bin/env python
'''
Running Camera Using MultiThreading
=========

Usage
-----
check_camera.py [Cameraimage filename]

Keys
----
  q     - exit
  ESC   - exit

'''

# Python 2/3 compatibility
from __future__ import print_function
import cv2

from multiprocessing.pool import ThreadPool
from collections import deque

class DummyTask:
    def __init__(self, data):
        self.data = data
    def ready(self):
        return True
    def get(self):
        return self.data

class App:
    def __init__(self, video_src):
        if (video_src.isdigit()):
            self.cap = cv2.VideoCapture(int(video_src))
        else:
            self.cap = cv2.VideoCapture(video_src)

    def run(self):
        threadn = cv2.getNumberOfCPUs()
        pool = ThreadPool(processes = threadn)
        pending = deque()

        threaded_mode = False
        
        frame_interval = 0
        last_frame_time = cv2.getTickCount()

        while True:
            while len(pending) > 0 and pending[0].ready():
                res,t0 = pending.popleft().get()
                t2 = cv2.getTickCount()
                time = (t2 - t0)/ cv2.getTickFrequency()
                frame_interval = frame_interval/cv2.getTickFrequency()
                cv2.putText(res,"threaded: " + str(threaded_mode), (20,20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
                cv2.putText(res,"latency: %0.1f ms" %  (time*1000), (20,40), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
                cv2.putText(res,"frame interval: %0.1f ms" % (frame_interval*1000), (20,60), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
                cv2.imshow('threaded video', res)
            
            if len(pending) < threadn:
                # Capture frame-by-frame
                ret, frame = self.cap.read()
                t = cv2.getTickCount()
                frame_interval=t-last_frame_time
                last_frame_time = cv2.getTickCount()

                if threaded_mode:
                    task = pool.apply_async(process_frame, (frame.copy(),t))
                else:
                    task = DummyTask(process_frame(frame,t))
                pending.append(task)
                    
            ch = cv2.waitKey(1)
            if ch == ord(' '):
                threaded_mode = not threaded_mode
            if ch==27 or ch == ord('q'):
                break

        # When everything done, release the capture
        self.cap.release()
        cv2.destroyAllWindows()


def process_frame(frame,t0):
    # some intensive computation...
    #frame = cv2.medianBlur(frame, 19)
    return frame,t0

def main():
    import sys
    try:
        video_src = sys.argv[1]
    except:
        video_src = '0'

    print(__doc__)

    App(video_src).run()
    

if __name__ == '__main__':
    main()