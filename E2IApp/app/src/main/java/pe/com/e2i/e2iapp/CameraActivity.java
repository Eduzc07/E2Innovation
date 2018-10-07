package pe.com.e2i.e2iapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class CameraActivity extends AppCompatActivity {

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
    }
}
