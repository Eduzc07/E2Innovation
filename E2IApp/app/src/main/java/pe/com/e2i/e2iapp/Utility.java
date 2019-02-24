package pe.com.e2i.e2iapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Utility {

    //Method to print an floatArray.
    public static String floatArrayToString(float[] f){
        String output = "";
        String delimiter = "\n" ;// Can be new line \n tab \t etc...
        for (int i=0; i<f.length; i++){
            output = output + f[i] + " mm."+ delimiter;
        }
        return output;
    }

    //Method to show an integerArray.
    //This case has all the characteristics that device allows changes.
    public static String integerArrayToString(int[] f){
        String output = "";
        String delimiter= " ";
        for (int i=0; i<f.length; i++)
        {
            switch(f[i]){
                case 0:
                    delimiter = ": Backward Compatible. \n" ;// Can be new line \n tab \t etc...
                    break;
                case 1:
                    delimiter = ": Manual Sensor. \n" ;
                    break;
                case 2:
                    delimiter = ": Manual Post Processing. \n" ;
                    break;
                case 3:
                    delimiter = ": Raw Image. \n" ;
                    break;
                case 4:
                    delimiter = ": Private Processing. \n" ;
                    break;
                case 5:
                    delimiter = ": Read Sensor Settings. \n" ;
                    break;
                case 6:
                    delimiter = ": Burst Capture. \n" ;
                    break;
                case 7:
                    delimiter = ": YUV Reprocessing. \n" ;
                    break;
                case 8:
                    delimiter = ": Depth Output. \n" ;
                    break;
                case 9:
                    delimiter = ": Constrained High Speed Video. \n" ;
                    break;
            }
            output = output + f[i] + delimiter;
        }
        return output;
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    public static String getFormattedMeasure(Context context, float height) {
        int heightFormat = R.string.format_measure;

        String unit;
        if (Utility.isMetric(context)) {
            unit = "mm";
        } else {
            unit = "in";
        }

        return String.format(context.getString(heightFormat), height, unit);
    }

    public static int[] getSize(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int width = prefs.getInt("refWidth", 0);
        int height = prefs.getInt("refHeight", 0);

        int[] size = {width, height};
        return size;
    }

    public static  void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("E2Iconfig.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static  String readFromFile(Context context) {
        String ret = "";

//        File file = new File("E2Iconfig.txt");
//        if(!file.exists()) {
//            ret = "0;0;0;0";
//            return ret;
//        }

        try {
            InputStream inputStream = context.openFileInput("E2Iconfig.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }
}
