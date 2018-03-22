import java.io.*;
import java.util.*;
class SingletonPeerInfo
{
    // static variable single_instance of type SingletonPeerInfo
    private static SingletonPeerInfo single_instance = null;

    // variable of type String
    public static int peerID;
    public static String host;
    public static int portNumber;
    public static int fileStatus;

    // private constructor restricted to this class itself
    private SingletonPeerInfo()
    {
        File file = new File("PeerInfo.cfg");
        String arr[] = new String[4]; int i=0;
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
        peerID = Integer.parseInt(arr[0]);
        host = arr[1];
        portNumber = Integer.parseInt(arr[2]);
        fileStatus = Integer.parseInt(arr[3]);
    }

    //static method to create instance of SingletonPeerInfo class
    public static SingletonPeerInfo getInstance()
    {
        if (single_instance == null)
            single_instance = new SingletonPeerInfo();

        return single_instance;
    }
}