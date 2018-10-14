#include <jni.h>
#include <string>
#include "opencv2/opencv.hpp"
#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>

using namespace cv;

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
    //A new name into C++ space "mRgb" to the Matrix saved in Java which is located in "addrRgba" is assigned.
    Mat& mRgb = *(Mat*)addrRgba;

    jbyte * pYUVFrameData = env->GetByteArrayElements(YUVFrameData, 0);
    Mat mNV(height + height/2, width, CV_8UC1, (unsigned char*)pYUVFrameData);
    Mat mBgr(height, width, CV_8UC3);
    cv::cvtColor(mNV, mBgr, CV_YUV2RGB_NV21);

    flip(mBgr.t(), mBgr, 1);
    rectangle(mBgr, Point(50, 50), Point(400, 200), Scalar(0, 255, 0), 5);
    rectangle(mBgr, Point(0, 0), Point(height, width), Scalar(0, 255, 255), 5);

    mBgr.copyTo(mRgb);
}