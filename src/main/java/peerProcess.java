/* CNT5106C Spring 2018
 * 
 * This is the entry point for the p2p project. 
 */

import java.util.ArrayList;
import java.util.Collections;

public class peerProcess
{
    private int peerId;
    private int port;
    private String hostname;
    private Boolean hasfile;
    private ArrayList<Boolean> bitfield;
    // private int bitfield;
    private DuplexServer listener;
    private ArrayList<Client> senders;

    public peerProcess()
    {

    }

    private void start(String[] args) throws Exception
    {   
        // read Common.cfg
        peerId =  Integer.parseInt(args[0]);
        // load in peer meta data here
        port = 6000 + peerId;
        hostname = "localhost";
        hasfile = true;
        // we have 306 pieces (FileSize // PieceSize)
        // if (hasfile) { bitfield = 0b1111111111111111; }//{ bitfield = Integer.MAX_VALUE; }
        // else { bitfield = 0b0000000000000000; }
        if (hasfile) { bitfield = new ArrayList<Boolean>(Collections.nCopies(306, true)); }
        else { bitfield = new ArrayList<Boolean>(Collections.nCopies(306, false));}
        // start server to listen for messages. 
        listener = new DuplexServer(port);
        System.out.println("Initiated listener");

        if (peerId > 1001)
        {
            int peers_to_connect = peerId - 1001;
            for (int i = 0; i < peers_to_connect; i++)
            {
                // start clients to talk with previous peers
                listener.init_socket(hostname, 6000 + 1001 + i, 0);
                // System.out.println("Broadcasting...");
                // listener.broadcast_to_peers("I am here");
            }           
        }
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            System.out.println("Not enough args: java -cp target/p2p-0.0.1-SNAPSHOT.jar cn.p2p.Client <port>");
            return;
        }

        peerProcess proc = new peerProcess();
        proc.start(args);
        // Server server = new Server();
        // server.start(args);
    }
}
