package pe.com.e2i.e2iapp;

public class Utility {

    //Method to print an floatArray.
    public static String floatArrayToString(float[] f){
        String output = "";
        String delimiter = "\n" ;// Can be new line \n tab \t etc...
        for (int i=0; i<f.length; i++){
            output = output + f[i] + " mm."+ delimiter;
        }
        return output;
    };

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
    };
}
