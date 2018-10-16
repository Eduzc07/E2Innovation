package pe.com.e2i.e2iapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
//import android.app.Fragment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment {
    private static final String TAG = CameraFragment.class.getSimpleName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView mTextView;
//    private TextureView mTextureView;  //A TextureView can be used to display a content stream
    private ImageView mImageView;

//    private OnFragmentInteractionListener mListener;

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

    private Bitmap 		mBitMap; // Bitmap element to show the image in preview.
    private Mat         jpegMat; //Mat element which receive the image from buffer.
    private Mat 		rgbMat; // Decode jpegMat in rgbMat, also it contains the image after JNI (C++ and OpenCV).

    private Surface mImageSurface;

    private Handler mBackgroundHandler;

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
    }

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;
    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

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
//            Log.d(TAG, "I'm an image frame!");
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

                byte[] nv21;
                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
                ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();

                nv21 = new byte[ySize + uSize + vSize];

                //U and V are swapped
                yBuffer.get(nv21, 0, ySize);
                vBuffer.get(nv21, ySize, vSize);
                uBuffer.get(nv21, ySize + vSize, uSize);

                //Method 1 in JAVA
//                Mat mYuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
//                mYuv.put(0, 0, nv21);
//                Mat mRGB = new Mat();
//                cvtColor(mYuv, mRGB, Imgproc.COLOR_YUV2RGB_NV21, 3);
//                Core.flip(mRGB.t(), mRGB, 1);
//                Imgproc.rectangle(mRGB, new Point(50, 50), new Point(400, 200), new Scalar(0, 255, 0, 255), 5);
//                ShowImage(mRGB);

                //Method 2 with JNI
                Mat mRGB = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC3);
                YUVtoRBG(mRGB.getNativeObjAddr(), nv21, image.getWidth(), image.getHeight());
                ShowImage(mRGB);

                if (image != null)
                    image.close();
            } catch (IllegalStateException e) {
                Log.d(TAG, "mImageAvailable() Too many images acquired");
            }
        }

    };

    public Mat convertYuv420888ToMat(Image image, boolean isGreyOnly) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane yPlane = image.getPlanes()[0];
        int ySize = yPlane.getBuffer().remaining();

        if (isGreyOnly) {
            byte[] data = new byte[ySize];
            yPlane.getBuffer().get(data, 0, ySize);

            Mat greyMat = new Mat(height + height/2, width, CvType.CV_8UC1);
            greyMat.put(0, 0, data);
//            Mat rgbMat = Imgcodecs.imdecode(greyMat, Imgcodecs.IMREAD_COLOR);
//            Mat m = new Mat(height, width, CvType.CV_8UC1, Scalar.all(250));e
            Log.v(TAG,"Data:" + Integer.toString(data.length));

//            String cs = Integer.toString(greyMat.rows());
//            String rs = Integer.toString(greyMat.cols());
//            Log.v(TAG,"Matrix - Java:"+cs+"x"+rs);

            Mat rgbMat = new Mat(height, width, CvType.CV_8UC3);
            cvtColor(greyMat, rgbMat, Imgproc.COLOR_GRAY2RGB);

            return rgbMat;
        }

        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        // be aware that this size does not include the padding at the end, if there is any
        // (e.g. if pixel stride is 2 the size is ySize / 2 - 1)
        int uSize = uPlane.getBuffer().remaining();
        int vSize = vPlane.getBuffer().remaining();

        byte[] data = new byte[ySize + (ySize/2)];

        yPlane.getBuffer().get(data, 0, ySize);

        ByteBuffer ub = uPlane.getBuffer();
        ByteBuffer vb = vPlane.getBuffer();

        int uvPixelStride = uPlane.getPixelStride(); //stride guaranteed to be the same for u and v planes
        if (uvPixelStride == 1) {
            uPlane.getBuffer().get(data, ySize, uSize);
            vPlane.getBuffer().get(data, ySize + uSize, vSize);

            Mat yuvMat = new Mat(height + (height / 2), width, CvType.CV_8UC1);
            yuvMat.put(0, 0, data);
            Mat rgbMat = new Mat(height, width, CvType.CV_8UC3);
            Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_I420, 3);
            yuvMat.release();
            return rgbMat;
        }

        // if pixel stride is 2 there is padding between each pixel
        // converting it to NV21 by filling the gaps of the v plane with the u values
        vb.get(data, ySize, vSize);
        for (int i = 0; i < uSize; i += 2) {
            data[ySize + i + 1] = ub.get(i);
        }

        Mat yuvMat = new Mat(height + (height / 2), width, CvType.CV_8UC1);
        yuvMat.put(0, 0, data);
        Mat rgbMat = new Mat(height, width, CvType.CV_8UC3);
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21, 3);
        yuvMat.release();
        return rgbMat;
    }

    public static Mat imageToMat(Image image) {
        ByteBuffer buffer;
        int rowStride;
        int pixelStride;
        int width = image.getWidth();
        int height = image.getHeight();
        int offset = 0;

        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[image.getWidth() * image.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
                if (pixelStride == bytesPerPixel) {
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);

                    if (h - row != 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                    offset += length;
                } else {


                    if (h - row == 1) {
                        buffer.get(rowData, 0, width - pixelStride + 1);
                    } else {
                        buffer.get(rowData, 0, rowStride);
                    }

                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }
        }

        Mat mat = new Mat(height + height/2, width, CvType.CV_8UC1);
        mat.put(0, 0, data);

        return mat;
    }

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

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment CameraFragment.
//     */
    // TODO: Rename and change types and number of parameters
//    public static CameraFragment newInstance(String param1, String param2) {
//        CameraFragment fragment = new CameraFragment();
//        Bundle args = new Bundle();
//        args.putString( ARG_PARAM1, param1 );
//        args.putString( ARG_PARAM2, param2 );
//        fragment.setArguments( args );
//        return fragment;
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate( savedInstanceState );
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString( ARG_PARAM1 );
//            mParam2 = getArguments().getString( ARG_PARAM2 );
//        }
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        mTextView = (TextView) rootView.findViewById(R.id.camera_textView);
//        mTextureView = (TextureView) rootView.findViewById(R.id.camera_texture);
        mImageView = (ImageView)  rootView.findViewById(R.id.test_image);

//        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);//Call mSurfaceTextureListener

        openCamera();
        mTextView.setText( stringFromJNI() );

        return rootView;
    }

    //mSurfaceTextureListener is called after OnCreate.
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener(){
        //it creates a space to preview display.
        //Invoked when a TextureView's SurfaceTexture is ready for use.
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable, width="+width+",height="+height);
            //***************************************************************
            openCamera();
            //***************************************************************
        }

        //Invoked when the SurfaceTexture's buffers size changed.
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        //Invoked when the specified SurfaceTexture is about to be destroyed.
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        //Invoked when the specified SurfaceTexture is updated through updateTexImage().
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

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
            //Sony Experia,it could change between devices.
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
            Log.v(TAG, "This deviced supported focal lengths of "+ Utility.floatArrayToString(lensDistances)); //tag:Device to see in Log.Cat

            boolean Flash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (Flash){
                Log.v(TAG, "Flash Available."); //tag:Device to see in Log.Cat
            }
            else{
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
            jpegMat.release();
            rgbMat.release();
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
//|| !mTextureView.isAvailable()
        if(null == mCameraDevice || null == mPreviewSize) {
            Log.e(TAG,"startPreview fail, return");
            return;
        }

        try {
//            SurfaceTexture texture = mTextureView.getSurfaceTexture();
//            if(null == texture) {
//                Log.e(TAG,"texture is null, return");
//                return;
//            }
//            // We configure the size of default buffer to be the size of camera preview we want.
//            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // This is the output Surface we need to start preview.
//            Surface surface = new Surface(texture);
//            mPreviewBuilder.addTarget(surface);

            Surface mImageSurface = mImageReader.getSurface();
            mPreviewBuilder.addTarget(mImageSurface);

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

                            try {

//                                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
//                                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
//
//                                // Auto focus should be continuous for camera preview.
                                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                //Update Flash
//                                mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewBuilder.build();
                                mPreviewSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed( @NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getContext(), "onConfigureFailed", Toast.LENGTH_LONG).show();
                        }
                    }, null);

//            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
//
//                @Override
//                public void onConfigured(CameraCaptureSession session) {
//                    //The camera is already closed
//                    if (null == mCameraDevice) {
//                        return;
//                    }
//                    // When the session is ready, we start displaying the preview.
//                    mPreviewSession = session;
//                    //***************************************************************
//                    updatePreview();
//                    //***************************************************************
//                }
//
//                @Override
//                public void onConfigureFailed(CameraCaptureSession session) {
//                    Toast.makeText(getContext(), "onConfigureFailed", Toast.LENGTH_LONG).show();
//                }
//            }, null);
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
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

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
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();  //printStackTrace() helps the programmer to understand where the actual problem occurred, lines in console.
        }
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {


        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {

        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {

        }

    };

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

                Size largest = map.getOutputSizes(SurfaceTexture.class)[5];
                //0. 2160x3840
                //1. 1536x2048
                //2. 1080x1920
                //3. 720x1280
                //4. 480x720
                //5. 480x640
                //6. 288x352
                //7. 240x320
                //8. 144x176

                Log.v(TAG, "   >> largest << Height: "+ Integer.toString(largest.getHeight()));
                Log.v(TAG, "   >> largest << Width:"+ Integer.toString(largest.getWidth()));

//                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
//                        ImageFormat.JPEG, /*maxImages*/2);



                mImageReader = ImageReader.newInstance(largest.getWidth(),
                                                        largest.getHeight(),
                                                        ImageFormat.YUV_420_888, 2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

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

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction( uri );
//        }
//    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach( context );
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException( context.toString()
//                    + " must implement OnFragmentInteractionListener" );
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }
//
//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     * <p>
//     * See the Android Training lesson <a href=
//     * "http://developer.android.com/training/basics/fragments/communicating.html"
//     * >Communicating with Other Fragments</a> for more information.
//     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }

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
    public native void YUVtoRBG(long matAddrRgba, byte[] data, int width, int height);
    /*=====================   JNI Part ======================= */

}
