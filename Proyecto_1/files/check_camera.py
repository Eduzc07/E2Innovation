#!/usr/bin/env python
'''
Running Camera Online!
=========

Usage
-----
check_camera.py [Camera]

Keys
----
  q     - exit
  ESC   - exit

'''

# Python 2/3 compatibility
from __future__ import print_function
import cv2

from multiprocessing.pool import ThreadPool

class App:
	def __init__(self, video_src):
		if (video_src.isdigit()):
			self.cap = cv2.VideoCapture(int(video_src))
		else:
			self.cap = cv2.VideoCapture(video_src)
		

	def run(self):
		fps = 0
		e0 = cv2.getTickCount()
		while(True):
			e1 = cv2.getTickCount()
			# Capture frame-by-frame
			ret, frame = self.cap.read()

			# Our operations on the frame come here
			# gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

			# Display the resulting frame
			e2 = cv2.getTickCount()
			time = (e2 - e1)/cv2.getTickFrequency()
			cv2.putText(frame,"delay: %0.3f ms" % (time*1000), (10,20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255),1)
			cv2.imshow('frame',frame)
						
			#=======================================================================
			#FPS
			e3 = cv2.getTickCount()
			time = (e3 - e0)/cv2.getTickFrequency()
			fps+=1
			if(time>1.0):
				e0 = cv2.getTickCount()
				print("fps:%d"%fps)
				fps = 0
			#=======================================================================

			ch = cv2.waitKey(1)
			if ch==27 or ch == ord('q'):
				break

		# When everything done, release the capture
		self.cap.release()
		cv2.destroyAllWindows()


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