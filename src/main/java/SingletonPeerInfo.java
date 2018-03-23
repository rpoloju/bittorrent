import java.io.*;
import java.util.*;
class SingletonPeerInfo
{
    // static variable single_instance of type SingletonPeerInfo
    private static SingletonPeerInfo single_instance = null;

    HashMap<Integer, RemotePeerInfo> peerInfoMap = new HashMap<>();
    // variable of type String
    public static int peerID[] = new int[6];
    public static String host[] = new String[6];
    public static int portNumber[] = new int[6];
    public static int fileStatus[] = new int[6];

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
            peerID[i] = Integer.parseInt(in.next());
            host[i] = in.next();
            portNumber[i] = Integer.parseInt(in.next());
            fileStatus[i] = Integer.parseInt(in.next());
            peerInfoMap.put(peerID[i], new RemotePeerInfo(peerID[i], host[i], portNumber[i], fileStatus[i]));
            i++;
        }
    }

    //static method to create instance of SingletonPeerInfo class
    public static SingletonPeerInfo getInstance()
    {
        if (single_instance == null)
            single_instance = new SingletonPeerInfo();

        return single_instance;
    }
}