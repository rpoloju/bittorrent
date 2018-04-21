/* CNT5106C Spring 2018
 * 
 * This is the entry point for the p2p project. 
 */

import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import javax.xml.bind.DatatypeConverter;

import messages.Have;

public class peerProcess
{
    

    public peerProcess()
    {

    }

    private void start(String[] args) throws Exception
    {   
        int peerId =  Integer.parseInt(args[0]);
        
        Peer me = new Peer(peerId);
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
    }
}
