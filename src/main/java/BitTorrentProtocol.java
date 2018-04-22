import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import messages.BitField;
import messages.Choke;
import messages.HandShake;
import messages.Have;
import messages.Interested;
import messages.NotInterested;
import messages.Piece;
import messages.Request;
import messages.UnChoke;

public class BitTorrentProtocol implements MessageListener
{
    private SingletonCommon common_cfg;
    private SingletonPeerInfo peer_cfg;
    private RemotePeerInfo my_info;
    private int myId;
    private int hasfile;
    private BitSet have_field;
    private DuplexServer listener;
    private HashMap<Integer, BitSet> peer_to_have_field;

    
    public BitTorrentProtocol(SingletonCommon ccfg, SingletonPeerInfo pcfg, int peer_id) throws Exception
    {
        // load in peer meta data here
        common_cfg = ccfg;
        peer_cfg = pcfg;
        myId = peer_id;
        my_info = peer_cfg.peerInfoMap.get(myId);
        hasfile = my_info.getHasFile_or_not();

        // we have 306 pieces (FileSize // PieceSize)
        String pwd = System.getProperty("user.dir");
        int pieces  = MessagePreparer.get_num_pieces(pwd + "/image.jpg");

        have_field = new BitSet(pieces);
        if (hasfile == 1) 
        { 
            have_field.set(0, pieces);
        }

        peer_to_have_field = new HashMap<>();

        // start server to listen for messages. 
        listener = new DuplexServer(my_info, myId, this);
        System.out.println("Initiated listener");

        if (myId > 1001)
        {
            int peers_to_connect = myId - 1001;
            for (int i = 0; i < peers_to_connect; i++)
            {
                // start clients to talk with previous peers
                RemotePeerInfo prev_peer = peer_cfg.peerInfoMap.get(1001 + i);
                listener.init_socket(prev_peer.getPeerHostName(), prev_peer.getPeerPortNumber(), 1001 + i);
            }           
        }
    }

	@Override
	public void onHandShake(HandShake hs) {
        System.out.println("We have handshake");
        int from_id = hs.getpeerId();

        // create bitfield & send
        BitField bf = new BitField(from_id, have_field);

        if (!bf.hasNothing())
        {
            System.out.println("Sending my bitfield");
            
            try {
                // byte[] bss = new byte[] {0, 0, 0, 1};
                // System.out.println("The length is " + ByteBuffer.wrap(bss).getInt());            
                // listener.send_message(ByteBuffer.wrap(bss), from_id);            
                listener.send_message(bf.get_buffer(), from_id);
            } catch (IOException e) {
                System.err.println(e.getStackTrace());
            }
        }
        else 
        {
            System.out.println("I have nothing so skipping bitfield.");
        }

	}

	@Override
	public void onBitField(BitField bf) {
        System.out.println("We have bitfield");
		
	}

	@Override
	public void onChoke(Choke c) {
		
	}

	@Override
	public void onHave(Have h) {
		
	}

	@Override
	public void onInterested(Interested in) {
		
	}

	@Override
	public void onNotInterested(NotInterested nin) {
		
	}

	@Override
	public void onPiece(Piece p) {
		
	}

	@Override
	public void onRequest(Request r) {
		
	}

	@Override
	public void onUnChoke(UnChoke unc) {
		
	}

	@Override
	public void onPeerJoined(int peer_id) {
		System.out.println("A new friend with ID=" + peer_id + " has joined");
	}

	@Override
	public void onPeerLeft(int peer_id) {
		System.out.println("A friend left with ID=" + peer_id);
	}

}