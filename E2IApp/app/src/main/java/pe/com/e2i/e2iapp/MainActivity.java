package pe.com.e2i.e2iapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

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

        //Set Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        //Increasing Brightness while the App is running.
        //float brightness=WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        float brightness=0.8f;
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById( R.id.sample_text );
//        tv.setText( stringFromJNI() );

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
