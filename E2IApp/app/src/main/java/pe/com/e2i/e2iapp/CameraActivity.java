package pe.com.e2i.e2iapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_RESULT = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_camera );

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            CameraFragment fragment = new CameraFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.camera_container, fragment )
                    .commit();
        }

        //Version over Mashmellow have another way to check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {

                if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
                    Toast.makeText(this,"No Permission to use the Camera services", Toast.LENGTH_SHORT).show();
                }
                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_RESULT);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
