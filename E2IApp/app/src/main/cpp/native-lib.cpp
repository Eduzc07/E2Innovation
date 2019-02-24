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

int numIter = 0;
int biggestSide = 0;

//Variables
int ksize = 3;
int scale = 1;
int delta = 0;
int ddepth = CV_16S;
Mat mNV;
Mat mBgr;

Mat outImg;
Mat src_gray;
Mat blured;
Mat grad;
Mat grad_x, grad_y;
Mat abs_grad_x, abs_grad_y;
//Mat dst;
Mat mLastImage;
Mat mReferenceImage;
Rect mRefRect = Rect(0,0,0,0);
Rect mTestRect = Rect(0,0,0,0);

vector<Rect> currentBoundRect;
double moneyValue;
//Mat src_gray = Mat(480, 640, CV_8UC1);
//Mat blured = Mat(480, 640, CV_8UC1);
//Mat grad_x = Mat(480, 640, CV_8UC1);
//Mat grad_y = Mat(480, 640, CV_8UC1);
//Mat grad = Mat(480, 640, CV_8UC1);
//Mat abs_grad_x = Mat(480, 640, CV_8UC1);
//Mat abs_grad_y = Mat(480, 640, CV_8UC1);
//Mat dst = Mat(480, 640, CV_8UC1);

double factor = 1.0;

Rect getBox(Rect input);
void runImage();

extern "C"
JNIEXPORT int JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_YUVtoRBG(
        JNIEnv* env,
        jobject,
        jlong addrRgba,
        jbyteArray YUVFrameData,
        jint width,
        jint height,
        jboolean value)
{
    bool isRotated = value;

    //A new name into C++ space "mRgb" to the Matrix saved in Java which is located in "addrRgba" is assigned.
    Mat& mRgb = *(Mat*)addrRgba;

    jbyte * pYUVFrameData = env->GetByteArrayElements(YUVFrameData, 0);
    mNV = Mat(height + height/2, width, CV_8UC1, (unsigned char*)pYUVFrameData);
    mBgr = Mat(height, width, CV_8UC3);
    cv::cvtColor(mNV, mBgr, CV_YUV2RGB_NV21);
    mNV.release();

    //Rotate Image to display
    if(!isRotated)
        flip(mBgr.t(), mBgr, 1);

    mBgr.copyTo(mRgb);
    return 0;
}


extern "C"
JNIEXPORT int JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_runMain(
        JNIEnv* env,
        jobject,
        jlong addrRgba,
        jbyteArray YUVFrameData,
        jint width,
        jint height,
        jboolean value)
{
    bool isRotated = value;
    double e1 = (double) getTickCount();

    //A new name into C++ space "mRgb" to the Matrix saved in Java which is located in "addrRgba" is assigned.
    Mat& mRgb = *(Mat*)addrRgba;

    jbyte * pYUVFrameData = env->GetByteArrayElements(YUVFrameData, 0);
    mNV = Mat(height + height/2, width, CV_8UC1, (unsigned char*)pYUVFrameData);
    mBgr = Mat(height, width, CV_8UC3);
    cv::cvtColor(mNV, mBgr, CV_YUV2RGB_NV21);
    mNV.release();

    //Rotate Image to display
    if(!isRotated)
        flip(mBgr.t(), mBgr, 1);

    //Save lastImage
    mBgr.copyTo(mLastImage);

    if(numIter == 5){
        numIter = 0;
        //currentBoundRect.clear();
    }

    //mBgr.copyTo(mRgb);

    if(numIter != 0){
        //Scalar color = Scalar(50, 200, 50);
        /// Draw contours
//        for( int i = 0; i< currentBoundRect.size(); i++ ) {
//            Rect newBox = getBox(currentBoundRect[i]);
//            rectangle( mBgr, newBox.tl(), newBox.br(), color, 2 );
//
//            char val[20];
//            sprintf(val, "%1.1f%%", moneyValue);
//            putText(mBgr, val, newBox.tl(), CV_FONT_HERSHEY_COMPLEX_SMALL, 1, Scalar(255,0,0));
//        }
        runImage();

        double e2 = (double) getTickCount();
        double time = (e2 - e1)/ getTickFrequency();

        char delay[20];
        sprintf(delay, "Delay: %1.2f ms   ", time*1e3);
        putText(mBgr, delay , Point(10, 30), CV_FONT_HERSHEY_PLAIN, 1, Scalar(255,0,0));

        numIter++;
        mBgr.copyTo(mRgb);
        return 0;
    }
//
//    resize(mBgr, outImg, cv::Size(), factor, factor);
//
//    /// Convert it to gray
//    cvtColor( outImg, src_gray, CV_BGR2GRAY );
//    outImg.release();
//
//    /// Reduce the noise so we avoid false circle detection
//    blur(src_gray, blured, Size(3, 3));
//    src_gray.release(); // free mem
//
//    Sobel(blured, grad_x, ddepth, 1, 0, ksize, scale, delta, BORDER_DEFAULT);
//    Sobel(blured, grad_y, ddepth, 0, 1, ksize, scale, delta, BORDER_DEFAULT);
//
//    blured.release(); // free mem
//
//    // converting back to CV_8U
//    convertScaleAbs(grad_x, abs_grad_x);
//    convertScaleAbs(grad_y, abs_grad_y);
//    addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 0, grad);
//    grad_x.release();
//    grad_y.release();
//    abs_grad_x.release();
//    abs_grad_y.release();
//
//    threshold(grad, dst, 100, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);
//    grad.release();
//
//    vector<vector<Point> > contours;
//    vector<Vec4i> hierarchy;
//
////    Mat canny_output;
//    // Detect edges using canny
////    Canny( dst, canny_output, 50, 100, 3 );
//
//    /// Find contours
//    findContours(dst, contours, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );
//
//    //Run if it s empty
//    if (contours.size() == 0){
//        double e2 = (double) getTickCount();
//        double time = (e2 - e1)/ getTickFrequency();
//
//        char delay[20];
//        sprintf(delay, "Delay: %1.2f ms", time*1e3);
//        putText(mBgr, delay , Point(10, 30),CV_FONT_HERSHEY_PLAIN ,2, Scalar(255,0,0));
//
//        mBgr.copyTo(mRgb);
//        return 0;
//    }
//
//    vector<vector<Point> > contours_poly( contours.size() );
//    vector<Rect> boundRect( contours.size() );
////    vector<Point2f>centers( contours.size() );
////    vector<float>radius( contours.size() );
//
//    double biggestArea = 0;
//    /// Draw contours
//    for( int i = 0; i< contours.size(); i++ ) {
//        double area = contourArea(contours[i]);
//        if ( area > 1e3*factor && area < 4e4*factor){
//            Scalar color = Scalar(50, 200, 50);
////            drawContours( mBgr, contours, i, color, 2, 8, hierarchy, 0, Point() );
//
//            approxPolyDP( contours[i], contours_poly[i], 3, true );
//            boundRect[i] = boundingRect( contours_poly[i] );
////            minEnclosingCircle( contours_poly[i], centers[i], radius[i] );
//
//            if (abs(boundRect[i].height - boundRect[i].width) < 20){
////                rectangle( mBgr, boundRect[i].tl()/factor, boundRect[i].br()/factor, color, 2 );
////                char name[20];
////                sprintf(name, "%1.0f", (double)boundRect[i].area());
////                putText(mBgr, name,centers[i],CV_FONT_HERSHEY_SIMPLEX ,1, color);
//                currentBoundRect.push_back(boundRect[i]);
//
//                if (biggestArea < area){
//                    biggestSide = (boundRect[i].height + boundRect[i].width)/2;
//                    biggestArea = area;
//                }
//            }
//        }
//    }
//    contours.clear();
//    hierarchy.clear();
//    contours_poly.clear();
//    boundRect.clear();
//
    numIter++;
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

//    double e2 = (double) getTickCount();
//    double time = (e2 - e1)/ getTickFrequency();
//
//    char delay[20];
//    sprintf(delay, "Delay: %1.2f ms", time*1e3);
//    putText(mBgr, delay , Point(10, 30),CV_FONT_HERSHEY_PLAIN ,2, Scalar(255,0,0));
//
//    mBgr.copyTo(mRgb);
    return 0;
}

Rect getBox(Rect input){
    int px = input.x/factor + input.width/(2*factor) - biggestSide/2;
    int py = input.y/factor + input.height/(2*factor) - biggestSide/2;
    Rect newBox = Rect(px, py, biggestSide, biggestSide);

    moneyValue = input.area() * 100.0 / newBox.area();
    return newBox;
}

double getWear(Rect refSize, Rect testSize)
{
    if (refSize.area()== 0 || testSize.area()== 0)
        return 0;

    double widthRef;
    double heightRef;

    if (refSize.height > refSize.width){
        widthRef = refSize.width;
        heightRef = refSize.height;
    } else {
        widthRef = refSize.height;
        heightRef = refSize.width;
        mRefRect.width = widthRef;
        mRefRect.height = heightRef;
    }

    double widthTest;
    double heightTest;

    if (testSize.height > testSize.width){
        widthTest = testSize.width;
        heightTest = testSize.height;
    } else {
        widthTest = testSize.height;
        heightTest = testSize.width;
        mTestRect.width = widthTest;
        mTestRect.height = heightTest;
    }

    double f = widthRef / widthTest;

    double wear = f*heightTest*100./heightRef;
    return wear>100?100.:wear;
}

Rect getSize(Mat& srcOrig){

    //Pre ROI
    Rect newBox = Rect(srcOrig.cols / 2 - 60, srcOrig.rows / 2 - 280, 120, 560);
    Mat image_roi = srcOrig.clone()(newBox);

    Mat src;
    image_roi.copyTo(src);

    // Create a kernel that we will use to sharpen our image
    Mat kernel = (Mat_<float>(3,3) <<
            1,  1, 1,
            1, -8, 1,
            1,  1, 1);
    // an approximation of second derivative, a quite strong kernel
    // do the laplacian filtering as it is
    // well, we need to convert everything in something more deeper then CV_8U
    // because the kernel has some negative values,
    // and we can expect in general to have a Laplacian image with negative values
    // BUT a 8bits unsigned int (the one we are working with) can contain values from 0 to 255
    // so the possible negative number will be truncated
    Mat imgLaplacian;
    filter2D(src, imgLaplacian, CV_32F, kernel);
    Mat sharp;
    src.convertTo(sharp, CV_32F);
    Mat imgResult = sharp - imgLaplacian;
    // convert back to 8bits gray scale
    imgResult.convertTo(imgResult, CV_8UC3);

    // Create binary image from source image
    Mat bw;
    cvtColor(imgResult, bw, COLOR_BGR2GRAY);
    threshold(bw, bw, 40, 255, THRESH_BINARY_INV | THRESH_OTSU);

    // Dilate a bit the dist image
    Mat kernel1 = Mat::ones(3, 3, CV_8U);
    dilate(bw, bw, kernel1);

    Mat element = getStructuringElement(MORPH_RECT, Size(5, 5));
    /// Apply the specified morphology operation
    morphologyEx( bw, bw, MORPH_OPEN, element );

    // Create the CV_8U version of the distance image
    // It is needed for findContours()
    Mat dist_8u;
    //dist.convertTo(dist_8u, CV_8U);
    bw.copyTo(dist_8u);
    // Find total markers
    vector<vector<Point> > contours;

    findContours(dist_8u, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    // Create the marker image for the watershed algorithm
    Mat markers = Mat::zeros(bw.size(), CV_32S);
    // Draw the foreground markers
    for (size_t i = 0; i < contours.size(); i++) {
        drawContours(markers, contours, static_cast<int>(i), Scalar(static_cast<int>(i)+1), -1);
    }

    // Draw the background marker
    Rect boxWater = Rect(20, 20, 80, 520);
    rectangle( markers, boxWater.tl(), boxWater.br(), Scalar(255), 3 );

    Mat newMat;
    markers.convertTo(newMat, CV_8U);

    // Perform the watershed algorithm
    watershed(imgResult, markers);
    Mat mark;
    markers.convertTo(mark, CV_8U);
    bitwise_not(mark, mark);

    // image looks like at that point
    // Generate random colors
    vector<Vec3b> colors;
    for (size_t i = 0; i < contours.size(); i++) {
        int b = theRNG().uniform(0, 256);
        int g = theRNG().uniform(0, 256);
        int r = theRNG().uniform(0, 256);
        colors.push_back(Vec3b((uchar)b, (uchar)g, (uchar)r));
    }

    // Create the result image
    Mat dst = Mat::zeros(markers.size(), CV_8UC3);
    // Fill labeled objects with random colors
    for (int i = 0; i < markers.rows; i++) {
        for (int j = 0; j < markers.cols; j++) {
            int index = markers.at<int>(i,j);
            if (index > 0 && index <= static_cast<int>(contours.size())) {
                dst.at<Vec3b>(i,j) = colors[index-1];
            }
        }
    }

    contours.clear();
    findContours(dist_8u, contours,  RETR_TREE, CHAIN_APPROX_SIMPLE);
    vector<vector<Point> > contours_poly( contours.size() );
    vector<Rect> boundRect( contours.size() );

    int pos = 0;
    //Enclose Rectangle
    for( size_t i = 0; i < contours.size(); i++ ) {
        double area = contourArea(contours[i]);
        if ( area > 2e3 && area < 2e4) {
            approxPolyDP(contours[i], contours_poly[i], 3, true);
            boundRect[i] = boundingRect(contours_poly[i]);
//            pos = i;
        }
    }

    Mat destMat = Mat::zeros( srcOrig.size(), CV_8UC3 );
    dst.copyTo( destMat(newBox) );

    dst = dst*0.4 + imgResult*0.6;

    rectangle( dst, boundRect[pos].tl(), boundRect[pos].br(), Scalar(0, 200,0), 2 );
    // Visualize the final image
    Point init = Point(newBox.x, newBox.y);

    srcOrig = destMat*0.4 + srcOrig*1.0;
    rectangle(srcOrig, init + boundRect[pos].tl(), init + boundRect[pos].br(), Scalar(0, 200,0), 2 );

    return boundRect[pos];
}

void runImage()
{
    Scalar color = Scalar(0, 200, 200);
    /// Draw contours
    Rect newBox = Rect(mBgr.cols / 2 - 30, mBgr.rows / 2 - 250, 60, 500);
    rectangle( mBgr, newBox.tl(), newBox.br(), color, 2 );

//    Rect eraser = Rect(mBgr.cols / 2 - 10, mBgr.rows / 2 - 240, 20, 50);
//    rectangle( mBgr, eraser.tl(), eraser.br(), Scalar(50, 255, 50), 2 );
//
//    Rect pen = Rect(mBgr.cols / 2 - 10, mBgr.rows / 2 - 188, 20, 420);
//    Mat image_roi = mBgr.clone()(pen);
//    rectangle( mBgr, pen.tl(), pen.br(), Scalar(50, 255, 250), 2 );

//    Mat src_gray;
//    cvtColor(image_roi, src_gray, CV_BGR2GRAY );


    //image_roi.copyTo(mBgr(cv::Rect(0, 0, image_roi.cols, image_roi.rows)));
}

extern "C"
JNIEXPORT int JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_getRef(
        JNIEnv* env,
        jobject,
        jlong addrRgba)
{
    //A new name into C++ space "mRgb" to the Matrix saved in Java which is located in "addrRgba" is assigned.
    Mat& mRgb = *(Mat*)addrRgba;

    mLastImage.copyTo(mReferenceImage);
    mRefRect = getSize(mLastImage);

    mLastImage.copyTo(mRgb);
    return 0;
}

extern "C"
JNIEXPORT int JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_checkPencil(
        JNIEnv* env,
        jobject,
        jlong addrRgba)
{
    //A new name into C++ space "mRgb" to the Matrix saved in Java which is located in "addrRgba" is assigned.
    Mat& mRgb = *(Mat*)addrRgba;

    mTestRect = getSize(mLastImage);

    double wear = getWear(mRefRect, mTestRect);

    if (wear != 0){
        char name[10];
        sprintf (name, "%0.1f%%", wear);

        cv::putText(mLastImage,
                    name,
                    cv::Point(mLastImage.cols / 2 - 30, mLastImage.rows / 2 - 250), // Coordinates
                    cv::FONT_HERSHEY_DUPLEX, // Font
                    0.8, // Scale. 2.0 = 2x bigger
                    cv::Scalar(255,255,255), // BGR Color
                    1); // Line Thickness (Optional)
    }
    mLastImage.copyTo(mRgb);
    return mRefRect.width;
}

extern "C"
JNIEXPORT void JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_setRefRec(
        JNIEnv* env,
        jobject,
        jint width,
        jint height)
{
    mRefRect.width = width;
    mRefRect.height = height;
}

extern "C"
JNIEXPORT int JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_getRefWidth(
        JNIEnv* env,
        jobject)
{
    return mRefRect.width;
}

extern "C"
JNIEXPORT int JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_getRefHeight(
        JNIEnv* env,
        jobject)
{
    return mRefRect.height;
}

extern "C"
JNIEXPORT int JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_getTestWidth(
        JNIEnv* env,
        jobject)
{
    return mTestRect.width;
}

extern "C"
JNIEXPORT int JNICALL
Java_pe_com_e2i_e2iapp_CameraFragment_getTestHeight(
        JNIEnv* env,
        jobject)
{
    return mTestRect.height;
}