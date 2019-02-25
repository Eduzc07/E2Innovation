package pe.com.e2i.e2iapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment {

    private static final String TAG = CameraFragment.class.getSimpleName();

    private static final String APP_SHARE_HASHTAG = "#E2IApp";

    private ShareActionProvider mShareActionProvider;

    private TextView mTextView;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private Button mFlashButton;
    private Button mPhotoButton;
    private Button mRefreshButton;
    private EditText mCameraPlainText;
    private TextView mCameraUnitTextView;
    private TextView mCameraHTextView;

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
    private int m_iRefWidth = 0;
    private int m_iRefHeight = 0;
    private int m_iCameraState = 0;
    private String m_sCurrentName;
    private Boolean mShowShare = false;
    private String m_sUnits = " mm";
    private Boolean m_bIsDataChanged = false;

    private String mMessage;
    private Bitmap mBitmap;

    private Mat 	mRGB; // Decode jpegMat in rgbMat, also it contains the image after JNI (C++ and OpenCV).
    private Boolean mIsProgressBarVisible = false;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    /*========================    4   ======================== */
    @Override
    public void onPause() {
        Log.e(TAG, "onPause <----");
        stopBackgroundThread();
        super.onPause();

        if (!m_bIsDataChanged)
            return;

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        //Save the Current Option selected
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();

        String dimFloat = mCameraPlainText.getText().toString();
        if (dimFloat.matches("")) {
            editor.putFloat("mPencilDim", 0);
            return;
        }

        float f = Float.parseFloat(dimFloat);
        editor.putFloat("mPencilDim", f);
        editor.apply();
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
                    save(rgb, m_sCurrentName);
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
        private void save(Mat imageMat, String fileName) {

            Bitmap imgMap;
            imgMap = Bitmap.createBitmap(imageMat.cols(),
                    imageMat.rows(),
                    Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(imageMat, imgMap);

//            Log.d(TAG, "Dir:  " + Environment.getExternalStorageDirectory());
            //File where the image will be saved.

            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/DCIM/E2IApp");
            dir.mkdirs();
//            String fileName = String.format("%d.jpg", System.currentTimeMillis());
            File outFile = new File(dir, fileName);

            //Image processed
            File fileProc = new File(dir, "procRefImage.jpeg");

            if (m_iCameraState == 1){
                //Display message
                CharSequence text = "Imagen de Referencia guardada.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(getActivity(), text, duration);
                toast.setGravity( Gravity.CENTER, 0, 0);
                toast.show();
            }

            try {

                if (m_iCameraState == 1) {
                    FileOutputStream fOut = new FileOutputStream( fileProc );
                    mBitmap.compress( Bitmap.CompressFormat.JPEG, 100, fOut );
                    fOut.flush();
                    fOut.close();
                }

                fileProc.setReadable(true, false);

                FileOutputStream outStream = new FileOutputStream(outFile);
                imgMap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.flush();
                outStream.close();

                outFile.setReadable(true, false);

                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

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
        //To use Options added in this fragment
        setHasOptionsMenu(true);
        // Required empty public constructor
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        mTextView = (TextView) rootView.findViewById(R.id.camera_textView);
        mCameraUnitTextView = (TextView) rootView.findViewById(R.id.camera_unit_textView);
        mCameraHTextView  = (TextView) rootView.findViewById(R.id.camera_h_textView);
        mCameraPlainText  = (EditText) rootView.findViewById(R.id.camera_plain_text);

        mCameraPlainText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                // We still need this for the share intent
                mMessage = APP_SHARE_HASHTAG + "\n";
                mMessage += "*Imagen de Referencia*";
                mMessage += "\n" + "Dimensiones: " + Integer.toString( m_iRefWidth ) + " x " +
                        Integer.toString( m_iRefHeight ) + " pixeles\n";

                String dimFloat = s.toString();
                if (!dimFloat.matches("")) {
                    float f = Float.parseFloat( dimFloat );

                    if (Utility.isMetric(getContext())) {
                        mMessage += "Este lápiz es de *" + Integer.toString( (int) f ) + m_sUnits + "*.\n";
                    } else {
                        mMessage += "Este lápiz es de *" + Float.toString( f ) + m_sUnits + "*.\n";
                    }
                }
                mMessage += "Visitanos en ";
                mMessage += getResources().getString( R.string.pref_link);
                mShareActionProvider.setShareIntent(createShareE2IIntent());
            }
        });

        mTextView.setVisibility(View.GONE);
        mCameraUnitTextView.setVisibility(View.GONE);
        mCameraHTextView.setVisibility(View.GONE);
        mCameraPlainText.setVisibility(View.GONE);

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
                updatePhotoButton();
            }
        });

        mRefreshButton = (Button) rootView.findViewById(R.id.refresh_button);
        mRefreshButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateRefreshButton();
            }
        });

        mRefreshButton.setVisibility( View.GONE );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        m_iCameraState = prefs.getInt("mCameraState", 0);

        int[] size  = Utility.getSize( getContext() );
        m_iRefWidth = size[0];
        m_iRefHeight = size[1];

        if (Utility.isMetric(getContext())) {
            m_sUnits = " mm";
            mCameraPlainText.setHint( "0" );
        } else {
            m_sUnits = " in";
            mCameraPlainText.setHint( "0.0" );
        }

        mGrabbing = true;
        openCamera();
//        mTextView.setText( stringFromJNI() );

        String title;
        if (m_iCameraState == 1)
            title = getResources().getString(R.string.tittle_activity_Ref);
        else
            title = getResources().getString(R.string.tittle_activity_Test);

        getActivity().setTitle(title);

        //Display ProgressBar
        mIsProgressBarVisible = true;

        Log.e(TAG, "Rotation onCreateView");

        // Orientation of image.
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape
            mIsRotated = true;
        } else {
            // Portrait
            mIsRotated = false;
        }

        switch(m_iCameraState) {
            case 1:
                break;
            case 2:
                //Load Ref in c++
                setRefRec(m_iRefWidth, m_iRefHeight);
                break;
        }

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/DCIM/E2IApp");
        File inFile = new File(dir, "procRefImage.jpeg");

        if (inFile.exists() && m_iRefWidth != 0 && m_iRefHeight != 0 && m_iCameraState == 1){
            String sizePix = "Dimensiones: \n" + Integer.toString( m_iRefWidth ) + " x " +
                    Integer.toString( m_iRefHeight ) + " pixeles";
            mTextView.setText(sizePix);

            float dim = prefs.getFloat("mPencilDim", 0);
            mCameraHTextView.setText(Utility.getFormattedMeasure(getContext(), dim));

            mTextView.setVisibility(View.VISIBLE);
            if (dim != 0) {
                mCameraHTextView.setVisibility(View.VISIBLE);
            } else {
                mCameraHTextView.setVisibility(View.GONE);
            }

            mShowShare = true;
            getActivity().invalidateOptionsMenu();
            // We still need this for the share intent
            mMessage = APP_SHARE_HASHTAG + "\n";
            mMessage += "*Imagen de Referencia*";
            mMessage += "\n" + "Dimensiones: " + Integer.toString( m_iRefWidth ) + " x " +
                    Integer.toString( m_iRefHeight ) + " pixeles\n";

            if (dim != 0) {
                if (Utility.isMetric(getContext())) {
                    mMessage += "Este lápiz es de *" + Integer.toString( (int) dim ) + m_sUnits + "*.\n";
                } else {
                    mMessage += "Este lápiz es de *" + Float.toString( dim ) + m_sUnits + "*.\n";
                }
            }
            mMessage += "Visitanos en ";
            mMessage += getResources().getString( R.string.pref_link);
        }

        try {
            if(inFile.exists() && m_iCameraState == 1){
                mFlashButton.setVisibility( View.GONE );
                mPhotoButton.setVisibility( View.GONE );
                mRefreshButton.setVisibility( View.VISIBLE );
                mProgressBar.setVisibility(View.GONE);
                mGrabbing = false;

                Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(inFile));
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                if (bmp!=null) {
                    mBitmap = bmp;
                    mImageView.setImageBitmap(bmp);
                } else {
                    Log.d(TAG, "null frame!");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Error--- >");  //sending log output. .e : Send a Error Message
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

            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[5];
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
                Log.v(TAG, "Flash Available.");
            } else {
                Log.v(TAG, "Flash Disabled.");
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        super.onCreateOptionsMenu( menu, inflater );
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.camerafragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        menuItem.setVisible(mShowShare);//

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (m_iRefWidth != 0 && m_iRefHeight != 0 && m_iCameraState == 1)
            mShareActionProvider.setShareIntent(createShareE2IIntent());

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
        } else {
            Log.d(TAG, "null frame!");
        }

        //Use only processed Image
        if (!mGrabbing)
            mBitmap = imgMap;
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
        Mat rgb = new Mat();
        switch(m_iCameraState) {
            case 1:
                mGrabbing = false;
//            m_bDisplayRef = true;
                getRef(rgb.getNativeObjAddr());

                m_iRefWidth = getRefWidth();
                m_iRefHeight = getRefHeight();

                m_bIsDataChanged = true;
                break;
            case 2:
                mGrabbing = false;
                int val = checkPencil(rgb.getNativeObjAddr());
                Log.d(TAG, "take photo! --" + Integer.toString(val));
                break;
        }


        ShowImage(rgb);
    }

    private void updatePhotoButton() {
        // We still need this for the share intent
        mMessage = APP_SHARE_HASHTAG + "\n";
        //Save the Current Option selected
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        switch(m_iCameraState) {
            case 1:
                SharedPreferences.Editor editor = prefs.edit();

                if (m_iRefWidth != 0 && m_iRefHeight != 0){
                    mShowShare = true;
                    getActivity().invalidateOptionsMenu();

                    editor.putBoolean("eCheck", true);
                    editor.putInt("refWidth", m_iRefWidth);
                    editor.putInt("refHeight", m_iRefHeight);
                    mTextView.setVisibility(View.VISIBLE);
                    mCameraUnitTextView.setVisibility(View.VISIBLE);
                    mCameraUnitTextView.setText(m_sUnits);
                    mCameraHTextView.setVisibility(View.VISIBLE);
                    mCameraPlainText.setVisibility(View.VISIBLE);

                    mCameraHTextView.setText( "h: " );

                    String sizePix = "Dimensiones: \n" + Integer.toString( m_iRefWidth ) + " x " +
                            Integer.toString( m_iRefHeight ) + " pixeles";
                    mTextView.setText(sizePix);

                    mFlashButton.setVisibility( View.GONE );
                    mPhotoButton.setVisibility( View.GONE );
                    mRefreshButton.setVisibility( View.VISIBLE );

                    //Save Image
                    m_sCurrentName = "RefImage.jpg";
                    mTakeImage = true;

                    mMessage += "*Imagen de Referencia*";
                    mMessage += "\n" + "Dimensiones: " + Integer.toString( m_iRefWidth ) + " x " +
                            Integer.toString( m_iRefHeight ) + " pixeles\n";

                    String dimFloat = mCameraPlainText.getText().toString();
                    if (!dimFloat.matches("")) {
                        float f = Float.parseFloat( dimFloat );
                        if (Utility.isMetric(getContext())) {
                            mMessage += "Este lápiz es de *" + Integer.toString( (int) f ) + m_sUnits + "*.\n";
                        } else {
                            mMessage += "Este lápiz es de *" + Float.toString( f ) + m_sUnits + "*.\n";
                        }
                    }

                } else {
                    editor.putBoolean("eCheck", false);
                    mTextView.setVisibility(View.GONE);
                    mCameraUnitTextView.setVisibility(View.GONE);
                    mCameraHTextView.setVisibility(View.GONE);
                    mCameraPlainText.setVisibility(View.GONE);

                    //Display message
                    CharSequence text = "No se ha encontrado un lápiz.\n    Debe tomar otra imagen.";
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(getActivity(), text, duration);
                    toast.setGravity( Gravity.CENTER, 0, 0);
                    toast.show();

                    //Restart Grabbing
                    m_bDisplayRef = true;
                    mGrabbing = true;
                }
                editor.apply();
                break;
            case 2:
                int width = getTestWidth();
                int height = getTestHeight();
                if (width != 0 && height != 0) {
                    mShowShare = true;
                    getActivity().invalidateOptionsMenu();
                    String sizePix = "Dimensiones: \n" + Integer.toString( width ) + " x " +
                            Integer.toString( height ) + " pixeles";
                    mTextView.setText(sizePix);

                    float dimRef = prefs.getFloat("mPencilDim", 0);
                    float factor = (float) m_iRefWidth/ (float) width;
                    float dim = factor*(float)height*dimRef/ (float)m_iRefHeight;

                    dim = dim > dimRef? dimRef:dim;
                    mCameraHTextView.setText(Utility.getFormattedMeasure(getContext(), dim));

                    mTextView.setVisibility(View.VISIBLE);
                    if (dimRef != 0) {
                        mCameraHTextView.setVisibility(View.VISIBLE);
                    } else {
                        mCameraHTextView.setVisibility(View.GONE);
                    }

                    mFlashButton.setVisibility( View.GONE );
                    mPhotoButton.setVisibility( View.GONE );
                    mRefreshButton.setVisibility( View.VISIBLE );

                    //Save Image
                    m_sCurrentName = "TestImage.jpg";
                    mTakeImage = true;

                    mMessage += "*Medición Actual*";
                    mMessage += "\n" + "Dimensiones: " + Integer.toString( width ) + " x " +
                            Integer.toString( height ) + " pixeles\n";

                    if (dim != 0) {
                        if (Utility.isMetric(getContext())) {
                            mMessage += "Este lápiz es de aproximádamente *" + Integer.toString( (int) dim ) + m_sUnits + "*.\n";
                        } else {
                            mMessage += "Este lápiz es de aproximádamente *" + Float.toString( dim ) + m_sUnits + "*.\n";
                        }
                    }
                } else {
                    mTextView.setVisibility(View.GONE);
                    mCameraUnitTextView.setVisibility(View.GONE);
                    mCameraHTextView.setVisibility(View.GONE);
                    mCameraPlainText.setVisibility(View.GONE);

                    //Display message
                    CharSequence text = "No se ha encontrado ningún lápiz.";
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(getActivity(), text, duration);
                    toast.setGravity( Gravity.CENTER, 0, 0);
                    toast.show();

                    //Restart Grabbing
                    m_bDisplayRef = true;
                    mGrabbing = true;
                }
                break;
        }

        mMessage += "Visitanos en ";
        mMessage += getResources().getString( R.string.pref_link);
        mShareActionProvider.setShareIntent(createShareE2IIntent());
    }

    private void updateRefreshButton() {
        mShowShare = false;
        getActivity().invalidateOptionsMenu();



        switch(m_iCameraState) {
            case 1:
                mFlashButton.setVisibility( View.VISIBLE );
                mPhotoButton.setVisibility( View.VISIBLE );
                mRefreshButton.setVisibility( View.GONE );
                mGrabbing = true;
                m_iRefWidth = 0;
                m_iRefHeight = 0;
                //Save the Current Option selected
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = prefs.edit();

                editor.putBoolean("eCheck", false);
                mTextView.setVisibility(View.GONE);
                mCameraUnitTextView.setVisibility(View.GONE);
                mCameraHTextView.setVisibility(View.GONE);
                mCameraPlainText.setVisibility(View.GONE);
                editor.apply();

                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/DCIM/E2IApp");
                //Image processed
                File fileProc = new File(dir, "procRefImage.jpeg");
                fileProc.delete();

                getContext().deleteFile( "E2Iconfig.txt" );
                break;
            case 2:
                mFlashButton.setVisibility( View.VISIBLE );
                mPhotoButton.setVisibility( View.VISIBLE );
                mRefreshButton.setVisibility( View.GONE );
                mGrabbing = true;
                mTextView.setVisibility(View.GONE);
                mCameraUnitTextView.setVisibility(View.GONE);
                mCameraHTextView.setVisibility(View.GONE);
                break;
        }
    }

    private Intent createShareE2IIntent(){

        if (mBitmap == null){
            Log.d(TAG, "mBitmap null frame!");
            return null;
        }

        //Add Logo
        Bitmap result = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());
        Canvas canvas = new Canvas(result);

        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);

        float aspectRatio = logo.getWidth() /
                (float) logo.getHeight();
        int width = 180;
        int height = Math.round(width / aspectRatio);

        Bitmap b = Bitmap.createScaledBitmap(logo, width, height, false);
        canvas.drawBitmap(mBitmap, 0f, 0f, null);
        canvas.drawBitmap(logo, 10f, 40f, null);

        try {
//            File sdCard = Environment.getExternalStorageDirectory();
//            File dir = new File(sdCard.getAbsolutePath() + "/DCIM/E2IApp");
//            File file = new File(dir, m_sCurrentName);

            File file = new File(getContext().getExternalCacheDir(), "procImage.jpeg");
            FileOutputStream fOut = new FileOutputStream(file);
            result.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, false);

            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

            final Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_TEXT, mMessage);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            shareIntent.setType("image/jpeg");
            return shareIntent;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
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

    public native void setRefRec(int width, int height);

    public native int getRefWidth();

    public native int getRefHeight();

    public native int getTestWidth();

    public native int getTestHeight();
    /*=====================   JNI Part ======================= */
}