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
    private Server listener;
    private ArrayList<Client> senders;

    public peerProcess()
    {

    }

    private void start(String[] args) throws Exception
    {   
        // read Common.cfg
        peerId =  Integer.parseInt(args[0]);
        // load in peer meta data here
        port = 6008;
        hostname = "localhost";
        hasfile = true;
        // we have 306 pieces (FileSize // PieceSize)
        if (hasfile) { bitfield = new ArrayList<>(Collections.nCopies(306, true)); }
        else { bitfield = new ArrayList<>(Collections.nCopies(306, false));}

        // start server to listen for messages. 
        listener = new Server();
        listener.start(port);
        
        if (peerId > 1001)
        {
            // start clients to talk with previous peers
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
