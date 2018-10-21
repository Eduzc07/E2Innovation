#include <jni.h>
#include <string>
#include "opencv2/opencv.hpp"
#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>

using namespace cv;
using namespace std;

extern "C" JNIEXPORT jstring JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_stringFromJNI(
        JNIEnv *env,
        jobject /* this */)
{
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_YUVtoRBGTest(
        JNIEnv* env,
        jobject obj,
        jint width,
        jint height,
        jbyteArray YUVFrameData)
{
    jbyte * pYUVFrameData = env->GetByteArrayElements(YUVFrameData, 0);

    double alpha;
    alpha = (double) 1.0;

    Mat mNV(height + height/2, width, CV_8UC1, (unsigned char*)pYUVFrameData);
    Mat mBgr(height, width, CV_8UC3);

    cv::cvtColor(mNV, mBgr, CV_YUV2BGR_I420);

    env->ReleaseByteArrayElements(YUVFrameData, pYUVFrameData, 0);

    return;
}



bool mIsRotated = false;

extern "C"
JNIEXPORT void JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_setRotation(
        JNIEnv* env,
        jobject,
        jboolean value)
{
    mIsRotated = value;
    return;
}

extern "C"
JNIEXPORT void JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_YUVtoRBG(
        JNIEnv* env,
        jobject,
        jlong addrRgba,
        jbyteArray YUVFrameData,
        jint width,
        jint height)
{

    double e1 = (double) getTickCount();

    //A new name into C++ space "mRgb" to the Matrix saved in Java which is located in "addrRgba" is assigned.
    Mat& mRgb = *(Mat*)addrRgba;

    jbyte * pYUVFrameData = env->GetByteArrayElements(YUVFrameData, 0);
    Mat mNV(height + height/2, width, CV_8UC1, (unsigned char*)pYUVFrameData);
    Mat mBgr(height, width, CV_8UC3);
    cv::cvtColor(mNV, mBgr, CV_YUV2RGB_NV21);

    if(!mIsRotated)
        flip(mBgr.t(), mBgr, 1);

    Mat src_gray;
//
    /// Convert it to gray
    cvtColor( mBgr, src_gray, CV_BGR2GRAY );

    /// Reduce the noise so we avoid false circle detection
    blur( src_gray, src_gray, Size(5, 5));

    int ksize = 3;
    int scale = 1;
    int delta = 0;
    int ddepth = CV_16S;

    Mat grad;
    Mat grad_x, grad_y;
    Mat abs_grad_x, abs_grad_y;
    Sobel(src_gray, grad_x, ddepth, 1, 0, ksize, scale, delta, BORDER_DEFAULT);
    Sobel(src_gray, grad_y, ddepth, 0, 1, ksize, scale, delta, BORDER_DEFAULT);
    // converting back to CV_8U
    convertScaleAbs(grad_x, abs_grad_x);
    convertScaleAbs(grad_y, abs_grad_y);
    addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 0, grad);

    Mat dst;
    threshold( grad, dst, 100, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);

    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;

//    Mat canny_output;
    /// Detect edges using canny
//    Canny( dst, canny_output, 50, 100, 3 );

    /// Find contours
    findContours( dst, contours, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );


    vector<vector<Point> > contours_poly( contours.size() );
    vector<Rect> boundRect( contours.size() );
    vector<Point2f>centers( contours.size() );
    vector<float>radius( contours.size() );
    /// Draw contours
    Mat drawing = Mat::zeros( mBgr.size(), CV_8UC3 );
    for( int i = 0; i< contours.size(); i++ ) {
        double area = contourArea(contours[i]);
        if ( area > 1e3 && area < 4e4){
            Scalar color = Scalar(50, 200, 50);
//            drawContours( mBgr, contours, i, color, 2, 8, hierarchy, 0, Point() );

            approxPolyDP( contours[i], contours_poly[i], 3, true );
            boundRect[i] = boundingRect( contours_poly[i] );
            minEnclosingCircle( contours_poly[i], centers[i], radius[i] );

            if (abs(boundRect[i].height - boundRect[i].width) < 20){
                rectangle( mBgr, boundRect[i].tl(), boundRect[i].br(), color, 2 );
//                char name[20];
//                sprintf(name, "%1.0f", (double)boundRect[i].area());
//                putText(mBgr, name,centers[i],CV_FONT_HERSHEY_SIMPLEX ,1, color);
            }

        }
    }


    //------------------------------------------------------------------------------
//    vector<Vec3f> circles;
//
//    /// Apply the Hough Transform to find the circles
//    HoughCircles( src_gray, circles, CV_HOUGH_GRADIENT, 1, src_gray.rows/15, 50, 100, 0, 0 );
//
//    /// Draw the circles detected
//    for( size_t i = 0; i < circles.size(); i++ )
//    {
//        Point center(cvRound(circles[i][0]), cvRound(circles[i][1]));
//        int radius = cvRound(circles[i][2]);
//        // circle center
//        circle( mBgr, center, 3, Scalar(0,255,0), -1, 8, 0 );
//        // circle outline
//        circle( mBgr, center, radius, Scalar(0,0,255), 3, 8, 0 );
//    }
//
//    rectangle(mBgr, Point(50, 50), Point(400, 200), Scalar(0, 255, 0), 5);
//    rectangle(mBgr, Point(0, 0), Point(height, width), Scalar(255, 255, 255), 5);

//    cvtColor( dst, mBgr, CV_GRAY2BGR );
    //------------------------------------------------------------------------------

    double e2 = (double) getTickCount();
    double time = (e2 - e1)/ getTickFrequency();

    char delay[20];
    sprintf(delay, "Delay: %1.2f ms", time*1e3);
    putText(mBgr, delay , Point(10, 30),CV_FONT_HERSHEY_PLAIN ,2, Scalar(255,0,0));

    mBgr.copyTo(mRgb);
}