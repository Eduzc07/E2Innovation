package pe.com.e2i.e2iapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment {

    private static final String TAG = CameraFragment.class.getSimpleName();
    private TextView mTextView;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private Button mFlashButton;
    private Button mPhotoButton;

    private Size mPreviewSize; //Size of the preview Image
    private CameraDevice 				mCameraDevice; //The CameraDevice class is a representation of a single camera connected to an Android device
    private CaptureRequest 		        mPreviewRequest; //A builder for capture requests.
    private CaptureRequest.Builder 		mPreviewBuilder; //A builder for capture requests.
    private CameraCaptureSession 		mPreviewSession;  /*A configured capture session for a CameraDevice,
    									used for capturing images from the camera or reprocessing images captured
    									from the camera in the same session previously.*/

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Boolean mIsRotated = false;
    private Boolean mTakeImage = false;
    private int mRefImage = 0;
    private Boolean mGrabbing = false;
    private Boolean m_bDisplayRef = false;
    private int m_iCount = 0;

    private Mat 	mRGB; // Decode jpegMat in rgbMat, also it contains the image after JNI (C++ and OpenCV).
    private Boolean mIsProgressBarVisible = false;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    /*========================    4   ======================== */
    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        stopBackgroundThread();
        super.onPause();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
    /*========================  End 4 ======================== */

    @Override
    public void onStop() {
        super.onStop();
        closeCamera();
        Log.e(TAG, "Rotation onStop");
    }

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;
    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /*========================    3   ======================== */
    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
    }
    /*========================  End 3 ======================== */

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ImageReader mImageReader;
    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));


            //Hide ProgressBar
            if(mIsProgressBarVisible){
                mProgressBar.setVisibility(View.GONE);
                mIsProgressBarVisible = false;
            }

            try {
                //------------------------------------------------------
                //Here the buffer is read.
                Image image =  reader.acquireNextImage();
                if (image == null)
                    return;
                //------------------------------------------------------
//                String cs = Integer.toString(image.getHeight());
//                String rs = Integer.toString(image.getWidth());
//                Log.v(TAG,"Image - Java:"+cs+"x"+rs);

                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
                ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();

                byte[] nv21;
                nv21 = new byte[ySize + uSize + vSize];

                //U and V are swapped
                yBuffer.get(nv21, 0, ySize);
                vBuffer.get(nv21, ySize, vSize);
                uBuffer.get(nv21, ySize + vSize, uSize);

                if (mTakeImage){
                    Mat rgb  = new Mat();
                    YUVtoRBG(rgb.getNativeObjAddr(), nv21, image.getWidth(), image.getHeight(), mIsRotated);
                    save(rgb);
                    mTakeImage = false;
                }

                //Method 1 in JAVA
//                Mat mYuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
//                mYuv.put(0, 0, nv21);
//                Mat mRGB = new Mat();
//                cvtColor(mYuv, mRGB, Imgproc.COLOR_YUV2RGB_NV21, 3);
//                Core.flip(mRGB.t(), mRGB, 1);
//                Imgproc.rectangle(mRGB, new Point(50, 50), new Point(400, 200), new Scalar(0, 255, 0, 255), 5);
//                ShowImage(mRGB);

                //Method 2 with JNI
//                mRGB = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC3);

                if (mGrabbing && !m_bDisplayRef){
                    runMain(mRGB.getNativeObjAddr(), nv21, image.getWidth(), image.getHeight(), mIsRotated);
                    ShowImage(mRGB);
                }

                if (m_bDisplayRef) {
                    m_iCount++;
                    if (m_iCount > 50 ){
                        m_bDisplayRef = false;
                        m_iCount = 0;
                    }
                }

                if (image != null)
                    image.close();
            } catch (IllegalStateException e) {
                Log.d(TAG, "mImageAvailable() Too many images acquired");
            }
        }

        //Save in SD Card!.
        private void save(Mat imageMat) {

            Bitmap imgMap;
            imgMap = Bitmap.createBitmap(imageMat.cols(),
                    imageMat.rows(),
                    Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(imageMat, imgMap);

            Log.d(TAG, "Dir:  " + Environment.getExternalStorageDirectory());
            //File where the image will be saved.

            FileOutputStream outStream = null;
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/DCIM/E2IApp");
            dir.mkdirs();
            String fileName = String.format("%d.jpg", System.currentTimeMillis());
            File outFile = new File(dir, fileName);

            //Display message
            CharSequence text = fileName + " saved.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getActivity(), text, duration);
            toast.setGravity( Gravity.CENTER, 0, 0);
            toast.show();

            try {
                outStream = new FileOutputStream(outFile);
                imgMap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.flush();
                outStream.close();

                //Add the picture to the gallery
                Intent galleryIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri picUri = Uri.fromFile(outFile);
                galleryIntent.setData(picUri);
                getActivity().sendBroadcast(galleryIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };

    static {
        System.loadLibrary( "native-lib" );
    }

    static {
        if(OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV successfully loaded");
        }else{
            Log.d(TAG, "OpenCV not loaded");
        }
    }

    public CameraFragment()  {
        // Required empty public constructor
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        mTextView = (TextView) rootView.findViewById(R.id.camera_textView);
        mImageView = (ImageView)  rootView.findViewById(R.id.test_image);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.loadingPanel);

        mFlashButton = (Button) rootView.findViewById(R.id.flash_button);
        mFlashButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.isActivated()){
                    setFlash(false);
                    view.setActivated(false);
                } else {
                    view.setActivated(true);
                    setFlash(true);
                }
            }
        });

        mPhotoButton = (Button) rootView.findViewById(R.id.takePhoto_button);
        mPhotoButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });


        //Display ProgressBar
        mIsProgressBarVisible = true;

        openCamera();
        mTextView.setText( stringFromJNI() );

        Log.e(TAG, "Rotation onCreateView");

        // Orientation of image.
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape
            mIsRotated = true; //JNI C++
        } else {
            // Portrait
            mIsRotated = false; //JNI C++
        }
        return rootView;
    }

    //Here the Preview Mode is set on the Screen.
    private void openCamera() {

        setUpCameraOutputs();

        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "openCamera E");

        try {
            String cameraId = manager.getCameraIdList()[0];//1: Front Camera 0: Back Camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            for (int j =0; j<9;j++){
                Size currentSize = map.getOutputSizes(SurfaceTexture.class)[j];
                Log.v(TAG, j + ": >> mPreviewSize << Height: "+ Integer.toString(currentSize.getHeight()));
                Log.v(TAG, "   >> mPreviewSize << Width:"+ Integer.toString(currentSize.getWidth()));
            }

            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[4];
            //Sony Experia, it could change between devices.
            //0. 2160x3840
            //1. 1536x2048
            //2. 1080x1920
            //3. 720x1280
            //4. 480x720
            //5. 480x640
            //6. 288x352
            //7. 240x320
            //8. 144x176

            //Here it requests which capabilities are allow to change in the device in use.
            int[] Cap = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);//Run ok
            Log.v(TAG, ">> Available Capabilities in this Device << \n");
            Log.v(TAG, Utility.integerArrayToString(Cap));

            //Show the Focal lengths Available
            float[] lensDistances = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS); //Run ok
            Log.v(TAG, "This device supported focal lengths of "+ Utility.floatArrayToString(lensDistances)); //tag:Device to see in Log.Cat

            boolean Flash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (Flash){
                Log.v(TAG, "Flash Available."); //tag:Device to see in Log.Cat
            } else {
                Log.v(TAG, "Flash Disabled.");  //tag:Device to see in Log.Cat
            }

            //Here is asked which  Level of hardware is supported.
            int val = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

            float minimumLens;
            //FULL > LIMITED > LEGACY.
            switch (val) {
                //LIMITED
                case 0:
                    Log.v(TAG, "Hardware Level:"+ Integer.toString(val)+"-> LIMITED");
                    minimumLens = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                    Log.v(TAG, "Minimum Focus Distance: "+ Float.toString(minimumLens));
                    break;
                //FULL
                case 1:
                    Log.v(TAG, "Hardware Level:"+ Integer.toString(val)+"-> FULL");
                    minimumLens = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                    Log.v(TAG, "Minimum Focus Distance: "+ Float.toString(minimumLens));
                    int minimumSensitive = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE).getLower();;
                    Log.v(TAG, "Minimum sensitive: "+ Integer.toString(minimumSensitive));
                    long minimumExposure = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE).getLower();;
                    Log.v(TAG, "Minimum Exposure time: "+ Long.toString(minimumExposure));
                    float[] lensApertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                    Log.v(TAG, "This device supported focal lengths of "+ Utility.floatArrayToString(lensApertures));
                    break;
                //LEGACY
                case 2:
                    Log.v(TAG, "Hardware Level:"+ Integer.toString(val)+"-> LEGACY");
                    long ssaa = characteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);;
                    Log.v(TAG, "Max Frame Duration: "+ Long.toString(ssaa/1000000)+" ms");
                    break;
            }

            if (ActivityCompat.checkSelfPermission( getContext(),
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                Log.e(TAG, "openCamera X");  //sending log output. .e : Send a Error Message
                manager.openCamera(cameraId, mStateCallback, null);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
//        Log.e(TAG, "openCamera X");  //sending log output. .e : Send a Error Message
    }

    //A callback objects for receiving updates about the state of a camera device.
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        //The next three methods should be implemented.
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            //***************************************************************
            startPreview();
            //***************************************************************
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            //Cleaning Matrix created previously.
            mRGB.release();
            Log.e(TAG, "onDisconnected");
            mCameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
            Log.e(TAG, "onError");
        }
    };

    protected void startPreview() {
        if(null == mCameraDevice || null == mPreviewSize) {
            Log.e(TAG,"startPreview fail, return");
            return;
        }

        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // This is the output Surface we need to start preview.
            Surface mImageSurface = mImageReader.getSurface();
            mPreviewBuilder.addTarget(mImageSurface);

            mGrabbing = true;

            // also for preview callbacks
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mPreviewSession = cameraCaptureSession;

                            //***************************************************************
//                            updatePreview();
                            //***************************************************************

                            try {
                                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//                                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);

                                // Auto focus should be continuous for camera preview.
                                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                //Update Flash
//                                mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewBuilder.build();
                                mPreviewSession.setRepeatingRequest(mPreviewRequest,
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed( @NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getContext(), "onConfigureFailed", Toast.LENGTH_LONG).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if(null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
//        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
//        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

        //Update Flash
        if (false){
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        }else{
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
        }
//        update_WB(viewModeAWB);

        //mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
        //mPreviewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f);

        //CapureRequest: settings and outputs needed to capture a single image from the camera device.
        //CameraData: The base class for camera controls and information.
        //Update preview Lines
        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(),
                                                null,
                                                mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();  //printStackTrace() helps the programmer to understand where the actual problem occurred, lines in console.
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     */
    private void setUpCameraOutputs() {
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
//                Size largest = Collections.max(
//                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
//                        new CompareSizesByArea());

//                for (int j =0; j<9;j++){
//                    Size currentSize = map.getOutputSizes(SurfaceTexture.class)[j];
//                    Log.v(TAG, j + ": >> mPreviewSize << Height: "+ Integer.toString(currentSize.getHeight()));
//                    Log.v(TAG, "   >> mPreviewSize << Width:"+ Integer.toString(currentSize.getWidth()));
//                }

                Size imageSize = map.getOutputSizes(SurfaceTexture.class)[5];
                //0. 2160x3840
                //1. 1536x2048
                //2. 1080x1920
                //3. 720x1280
                //4. 480x720
                //5. 480x640
                //6. 288x352
                //7. 240x320
                //8. 144x176

                Log.v(TAG, "   >> largest << Height: "+ Integer.toString(imageSize.getHeight()));
                Log.v(TAG, "   >> largest << Width:"+ Integer.toString(imageSize.getWidth()));

//                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
//                        ImageFormat.JPEG, /*maxImages*/2);

                mImageReader = ImageReader.newInstance(imageSize.getWidth(),
                        imageSize.getHeight(),
                                                        ImageFormat.YUV_420_888, 2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);


                mRGB = new Mat(imageSize.getWidth(), imageSize.getHeight(), CvType.CV_8UC3);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
//                int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
//                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//                boolean swappedDimensions = false;
//                switch (displayRotation) {
//                    case Surface.ROTATION_0:
//                    case Surface.ROTATION_180:
//                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
//                            swappedDimensions = true;
//                        }
//                        break;
//                    case Surface.ROTATION_90:
//                    case Surface.ROTATION_270:
//                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
//                            swappedDimensions = true;
//                        }
//                        break;
//                    default:
//                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
//                }

//                Point displaySize = new Point();
//                getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
//                int rotatedPreviewWidth = width;
//                int rotatedPreviewHeight = height;
//                int maxPreviewWidth = displaySize.x;
//                int maxPreviewHeight = displaySize.y;
//
//                if (swappedDimensions) {
//                    rotatedPreviewWidth = height;
//                    rotatedPreviewHeight = width;
//                    maxPreviewWidth = displaySize.y;
//                    maxPreviewHeight = displaySize.x;
//                }
//
//                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
//                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
//                }
//
//                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
//                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
//                }
//
//                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
//                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
//                // garbage capture data.
//                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
//                        maxPreviewHeight, largest);
//
//                // We fit the aspect ratio of TextureView to the size of preview we picked.
//                int orientation = getResources().getConfiguration().orientation;
//                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                    mTextureView.setAspectRatio(
//                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
//                } else {
//                    mTextureView.setAspectRatio(
//                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
//                }
//
//                // Check if the flash is supported.
//                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
//                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        if (null != mPreviewSession) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    //To show the Image.
    public void ShowImage(Mat matToBit){
        Bitmap imgMap;
        imgMap = Bitmap.createBitmap(matToBit.cols(),
                                     matToBit.rows(),
                                     Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matToBit, imgMap);

        if (imgMap!=null) {
            mImageView.setImageBitmap(imgMap);
        }else{
            Log.d(TAG, "null frame!");
        }
    }

    private void setFlash(boolean value){
        if (value){
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        } else{
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
        }

        //Update preview Lines
        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();  //printStackTrace() helps the programmer to understand where the actual problem occurred, lines in console.
        }
    }

    private void takePhoto(){
        Log.d(TAG, "take photo!");
        //mTakeImage = true;
        //mImageReader.close();
        if (mRefImage == 0){
            m_bDisplayRef = true;
            Mat rgb  = new Mat();
            getRef(rgb.getNativeObjAddr());
            ShowImage(rgb);

            //Display message
            CharSequence text = "Reference has been taken.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getActivity(), text, duration);
            toast.setGravity( Gravity.CENTER, 0, 0);
            toast.show();
        }

        if (mRefImage == 1){
            //mImageReader.close();
            //mCameraDevice.close();
            //closeCamera();
            mGrabbing = false;
            Mat image  = new Mat();
            checkPencil(image.getNativeObjAddr());
            ShowImage(image);
        }

        if (mRefImage >= 2){
            mGrabbing = true;
            mRefImage = -1;
        }

        mRefImage++;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    /*=====================   JNI Part ======================= */
    /*matAddrRgba: Pointer from the Mat.
     * vT: Times we ask for a photo.
     * vV: Size of the image to put into the Matrix with OpenCV commands.
     */
    public native int YUVtoRBG(long matAddrRgba, byte[] data, int width, int height, boolean rotation);

    public native int runMain(long matAddrRgba, byte[] data, int width, int height, boolean rotation);

    public native int getRef(long matAddrRgba);

    public native int checkPencil(long matAddrRgba);
    /*=====================   JNI Part ======================= */

}
