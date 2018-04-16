/* CNT5106C Spring 2018
 * 
 * This is the entry point for the p2p project. 
 */

import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import javax.xml.bind.DatatypeConverter;

public class peerProcess
{
    private int peerId;
    private int port;
    private String hostname;
    private int hasfile;
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
        SingletonCommon common_cfg = SingletonCommon.getInstance();

        peerId =  Integer.parseInt(args[0]);
        
        // read PeerInfo.cfg
        SingletonPeerInfo peer_cfg = SingletonPeerInfo.getInstance();
        RemotePeerInfo my_info = peer_cfg.peerInfoMap.get(peerId);

        // load in peer meta data here
        port = my_info.getPeerPortNumber();
        hostname = my_info.getPeerHostName();
        hasfile = my_info.getHasFile_or_not();
        System.out.println("CONFIG: " + hostname + ":" + port + " hasfile: " + hasfile);
        // we have 306 pieces (FileSize // PieceSize)
        // if (hasfile) { bitfield = 0b1111111111111111; }//{ bitfield = Integer.MAX_VALUE; }
        // else { bitfield = 0b0000000000000000; }
        if (hasfile > 0) { bitfield = new ArrayList<Boolean>(Collections.nCopies(306, true)); }
        else { bitfield = new ArrayList<Boolean>(Collections.nCopies(306, false));}
        // start server to listen for messages. 
        listener = new DuplexServer(port, peerId);
        System.out.println("Initiated listener");

        if (peerId > 1001)
        {
            int peers_to_connect = peerId - 1001;
            for (int i = 0; i < peers_to_connect; i++)
            {
                // start clients to talk with previous peers
                RemotePeerInfo prev_peer = peer_cfg.peerInfoMap.get(1001 + i);
                listener.init_socket(prev_peer.getPeerHostName(), prev_peer.getPeerPortNumber(), 1001 + i);
                // System.out.println("Broadcasting...");
                // listener.broadcast_to_peers("<CTRL,HELLO," + Integer.toString(peerId) + ">");
                
                MessagePreparer.broadcast_file(listener, "/home/wgar/p2p/image.jpg");
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
