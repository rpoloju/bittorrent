import java.util.BitSet;

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
    private int port;
    private String hostname;
    private int hasfile;
    private BitSet have_field;
    private DuplexServer listener;

    
    public BitTorrentProtocol(SingletonCommon ccfg, SingletonPeerInfo pcfg, int peer_id) throws Exception
    {
        // load in peer meta data here
        common_cfg = ccfg;
        peer_cfg = pcfg;
        myId = peer_id;

        my_info = peer_cfg.peerInfoMap.get(myId);
        port = my_info.getPeerPortNumber();
        hostname = my_info.getPeerHostName();
        hasfile = my_info.getHasFile_or_not();
        System.out.println("CONFIG: " + hostname + ":" + port + " hasfile: " + hasfile);

        // we have 306 pieces (FileSize // PieceSize)
        String pwd = System.getProperty("user.dir");
        int pieces  = MessagePreparer.get_num_pieces(pwd + "/image.jpg");

        have_field = new BitSet(pieces);
        if (hasfile == 1) 
        { 
            have_field.set(0, pieces);
        }

        // start server to listen for messages. 
        listener = new DuplexServer(port, myId, this);
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
	}

	@Override
	public void onBitField(BitField bf) {
		
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

}