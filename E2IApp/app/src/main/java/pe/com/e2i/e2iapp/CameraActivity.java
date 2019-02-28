package pe.com.e2i.e2iapp;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_RESULT = 200;
    private static final String LOG_TAG = CameraActivity.class.getSimpleName();

    private Boolean mCheckPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_camera );

        if (savedInstanceState == null) {
            if (!Utility.isCameraAllow( this ) && !Utility.isSDAllow( this )){
                // Create the detail fragment and add it to the activity
                // using a fragment transaction.
                CameraFragment fragment = new CameraFragment();
                getSupportFragmentManager().beginTransaction()
                        .add( R.id.camera_container, fragment )
                        .commit();
            }
        }

        //Set Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        //Increasing Brightness while the App is running.
        //float brightness=WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        float brightness=0.9f;
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);

        getSupportActionBar().setDisplayHomeAsUpEnabled( false );

        //Keep the Screen On
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Version over Mashmellow have another way to check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Add permission for camera and let user grant the permission
            if (Utility.isCameraAllow(this) && Utility.isSDAllow(this)) {
                Log.v(LOG_TAG, "---->>0 <<---- ");
                mCheckPermission = true;

                if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
                    Toast.makeText(this,"No Permission to use the Camera services", Toast.LENGTH_SHORT).show();
                }

                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_RESULT);


                Log.v(LOG_TAG, "---->>1555 <<---- ");
                return;
            } else {
                Log.v(LOG_TAG, "---->>2 <<---- ");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.detail, menu);
//        return super.onCreateOptionsMenu( menu );
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (mCheckPermission){

            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/DCIM/E2IApp");
            File inFile = new File(dir, "procRefImage.jpeg");
            inFile.delete();

            deleteFile( "E2Iconfig.txt" );

            CameraFragment fragment = new CameraFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.camera_container, fragment )
                    .commit();
        }


        switch (requestCode){
            case  REQUEST_CAMERA_RESULT:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED){
                    // close the app
                    Toast.makeText(CameraActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}