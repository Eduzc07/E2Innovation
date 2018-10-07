package pe.com.e2i.e2iapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements MainFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary( "native-lib" );
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        if (findViewById(R.id.camera_container) != null) {
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.camera_container, new CameraFragment(), "CTAG")
                        .commit();
            }
        } else {
            getSupportActionBar().setElevation(0f);
        }

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById( R.id.sample_text );
//        tv.setText( stringFromJNI() );

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    public native String stringFromJNI();
    public void onViewCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }
}
