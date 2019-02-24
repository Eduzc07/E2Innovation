package pe.com.e2i.e2iapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import java.io.File;

public class MainActivity extends AppCompatActivity implements MainFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary( "native-lib" );
//    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "---->> onStop <<---- ");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int width = prefs.getInt("refWidth", 0);
        int height = prefs.getInt("refHeight", 0);
        float penDim =  prefs.getFloat("mPencilDim", 0);

        Log.v(LOG_TAG, "---->> width <<---- " + width);
        Log.v(LOG_TAG, "---->> height <<---- " + height);
        Log.v(LOG_TAG, "---->> pencilDim <<---- " + penDim);

        String data = Integer.toString( width );
        data += ";" + Integer.toString( height );
        if(penDim!=0) {
            data += ";" + penDim;
            if (Utility.isMetric( getApplicationContext() ))
                data += ";mm";
            else
                data += ";in";
        }else{
            data += ";0;0";
        }

        Utility.writeToFile(data , getApplicationContext() );
    }

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
//        WindowManager.LayoutParams lp = getWindow().getAttributes();
//        //Increasing Brightness while the App is running.
//        //float brightness=WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
//        float brightness=0.8f;
//        lp.screenBrightness = brightness;
//        getWindow().setAttributes(lp);

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById( R.id.sample_text );
//        tv.setText( stringFromJNI() );

        String readFile = Utility.readFromFile( getApplicationContext() );

        if (readFile.matches("")) {
            readFile = "0;0;0;0";
        }

        String[] parts = readFile.split(";");
        int width = Integer.parseInt(parts[0]);
        int height = Integer.parseInt(parts[1]);
        float pencilDim = Float.parseFloat(parts[2]);

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/DCIM/E2IApp");
        File inFile = new File(dir, "procRefImage.jpeg");

        Log.v(LOG_TAG, "---->> width <<---- " + parts[0]);
        Log.v(LOG_TAG, "---->> height <<---- " + parts[1]);
        Log.v(LOG_TAG, "---->> pencilDim <<---- " + parts[2]);

        Boolean bCheck = false;
        if(inFile.exists() && width != 0 && height != 0)
            bCheck = true;

        //Create share boolean
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("eCheck", bCheck);
        editor.putInt("refWidth", width);
        editor.putInt("refHeight", height);
        editor.putInt("mCameraState", 0);
        editor.putFloat("mPencilDim", pencilDim);
        editor.apply();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about_us) {
            Intent intent = new Intent(this, AboutUsActivity.class);
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
