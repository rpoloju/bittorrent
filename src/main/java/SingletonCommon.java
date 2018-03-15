import java.io.*;
import java.util.*;
class SingletonCommon
{
    // static variable single_instance of type SingletonCommon
    private static SingletonCommon single_instance = null;

    // variable of type String
    public static int NumOfPrefNbrs;
    public static int UnchokingInt;
    public static int OptUnchokingInt;
    public static String FileName;
    public static int FileSize;
    public static int PieceSize;

    // private constructor restricted to this class itself
    private SingletonCommon()
    {
        File file = new File("Common.cfg");
        String arr[] = new String[12]; int i=0;
        Scanner in = null;
        try {
            in = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while(in.hasNext()) {
            arr[i] = in.next();
            i++;
        }
        NumOfPrefNbrs = Integer.parseInt(arr[1]);
        UnchokingInt = Integer.parseInt(arr[3]);
        OptUnchokingInt = Integer.parseInt(arr[5]);
        FileName = arr[7];
        FileSize = Integer.parseInt(arr[9]);
        PieceSize = Integer.parseInt(arr[11]);
    }

    //static method to create instance of SingletonCommon class
    public static SingletonCommon getInstance()
    {
        if (single_instance == null)
            single_instance = new SingletonCommon();

        return single_instance;
    }
}