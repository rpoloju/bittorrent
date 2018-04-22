import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import messages.BitField;
import messages.Choke;
import messages.HandShake;
import messages.Have;
import messages.Interested;
import messages.MessageType;
import messages.NotInterested;
import messages.Piece;
import messages.Request;
import messages.UnChoke;

/**
 * @author Washington Garcia
 */

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
    private int pieces;
    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;

    private ArrayList<Integer> choked_peers; // a 
    private ArrayList<Integer> preferred_peers; // b
    private int optimistic_unchoked_peer; // c
    private ArrayList<Integer> interested_peers; // = a + b + c

    private Logger LOGGER = LoggerFactory.getLogger(BitTorrentProtocol.class);
    
    private Timer unchoke_timer;
    private Timer optim_unchoke_timer;

    public BitTorrentProtocol(SingletonCommon ccfg, SingletonPeerInfo pcfg, int peer_id) throws Exception
    {
        // load in peer meta data here
        common_cfg = ccfg;
        peer_cfg = pcfg;
        myId = peer_id;
        my_info = peer_cfg.peerInfoMap.get(myId);
        hasfile = my_info.getHasFile_or_not();
        numberOfPreferredNeighbors = ccfg.NumOfPrefNbrs; // k
        unchokingInterval = ccfg.UnchokingInt; // p
        optimisticUnchokingInterval = ccfg.OptUnchokingInt; // m

        // we have 306 pieces (FileSize // PieceSize)
        String pwd = System.getProperty("user.dir");
        pieces  = MessagePreparer.get_num_pieces(pwd + "/image.jpg");

        have_field = new BitSet(pieces);
        if (hasfile == 1) 
        { 
            have_field.set(0, pieces);
        }

        peer_to_have_field = new HashMap<>();
        interested_peers = new ArrayList<>();
        optimistic_unchoked_peer = -1;
        preferred_peers = new ArrayList<>();
        choked_peers = new ArrayList<>();

        LOGGER.debug("Starting P2P Protocol for Peer" + myId + ".");
        
        // Start timers and let network latency add randomness
        init_timers();

        // start server to listen for messages. 
        listener = new DuplexServer(my_info, myId, this);

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

    // Set up the protocol timers    
    private void init_timers()
    {
        unchoke_timer = new Timer(unchokingInterval * 1000, new ActionListener(){
        
            @Override
            public void actionPerformed(ActionEvent e) {
                recalculate_preferred_peers();
            }
        });
        unchoke_timer.setRepeats(true);
        unchoke_timer.start();

        optim_unchoke_timer = new Timer(optimisticUnchokingInterval * 1000, new ActionListener(){
        
            @Override
            public void actionPerformed(ActionEvent e) {
                recalculate_optim_unchoke();
            }
        });
        optim_unchoke_timer.setRepeats(true);
        optim_unchoke_timer.start();
    }

    /////////// Timer data handlers

    private void recalculate_preferred_peers() {
        LOGGER.info("Peer ["+ myId + "] has the preffered neighbors []");
    }

    private void recalculate_optim_unchoke() {
        LOGGER.info("Peer ["+ myId + "] has the optimistically unchoked neighbor [" + optimistic_unchoked_peer + "]");
    }

    /////////// Meta data handlers

    private MessageType check_interest(BitField bf) {
        // Determine if send interested or not interested
        int from_id = bf.getpeerId();                
        BitSet peer_set = bf.getBitSet();

        for (int i = 0; i < bf.getLength(); i++) {
            boolean mine = have_field.get(i);
            boolean theirs = peer_set.get(i);

            if (mine == false && theirs == true) {
                LOGGER.debug("Sending INTERESTED to Peer " + from_id);
                return new Interested(myId);
            }
        }

        LOGGER.debug("Sending NOTINTERESTED to Peer " + from_id);        
        return new NotInterested(myId);
    }

    private void update_peer_map(BitField bf) {
       int from_id = bf.getpeerId();
       
       peer_to_have_field.put(from_id, bf.getBitSet());
    }

    private void update_peer_map(Have h) {
        int from_id = h.getpeerId();

        BitSet map_get = peer_to_have_field.get(from_id);
        if (map_get != null) {
            map_get.set(h.getpieceIndex());
        } else {
            LOGGER.debug("Got a HAVE for an unitialized Peer, ID=" + from_id);
        }
    }

    ////////////// BTP Event handlers

	@Override
	public void onHandShake(HandShake hs) {
        // System.out.println("We have handshake");
        
        int from_id = hs.getpeerId();

        // create bitfield & send
        BitField bf = new BitField(from_id, have_field);

        if (!bf.hasNothing())
        {
            LOGGER.debug("Sending my bitfield to Peer " + from_id);
            
            try {
                // byte[] bss = new byte[] {0, 0, 0, 1};
                // System.out.println("The length is " + ByteBuffer.wrap(bss).getInt());            
                // listener.send_message(ByteBuffer.wrap(bss), from_id);            
                listener.send_message(bf.get_buffer(), from_id);
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                System.out.println(e.getStackTrace());
            }
        }
        else 
        {
            LOGGER.debug("I have nothing so skipping bitfield.");
        }

	}

	@Override
	public void onBitField(BitField bf) {
        int from_id = bf.getpeerId();
        LOGGER.debug("We have bitfield from Peer [" + from_id + "].");
        
        update_peer_map(bf);
        MessageType response = check_interest(bf);

        try {       
            listener.send_message(response.get_buffer(), from_id);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            System.out.println(e.getStackTrace());
        }
	}

	@Override
	public void onHave(Have h) {
        int idx = h.getpieceIndex();
        int from_id = h.getpeerId();
        LOGGER.info("Peer [" + myId + "] received the 'have' message from [" + from_id + "] for the piece [" + idx + "].");
        update_peer_map(h);

        BitSet bs = new BitSet(pieces);
        bs.set(idx);
        MessageType response = check_interest(new BitField(from_id, bs));

        try {       
            listener.send_message(response.get_buffer(), from_id);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            System.out.println(e.getStackTrace());
        }
	}

	@Override
	public void onInterested(Interested in) {
        int from_id = in.getpeerId();
        LOGGER.info("Peer [" + myId + "] received the the 'interested' message from [" + from_id + "].");

        if (!interested_peers.contains(Integer.valueOf(from_id))) {
            interested_peers.add(Integer.valueOf(from_id));
        }
	}

	@Override
	public void onNotInterested(NotInterested nin) {
		int from_id = nin.getpeerId();
        LOGGER.info("Peer [" + myId + "] received the the 'not interested' message from [" + from_id + "].");

        if (interested_peers.contains(Integer.valueOf(from_id))) {
            interested_peers.remove(Integer.valueOf(from_id));
        }
	}

	@Override
	public void onPiece(Piece p) {
        int from_id = p.getpeerId();
        LOGGER.info("Peer [" + myId + "] received piece from [" + from_id + "].");
	}

	@Override
	public void onRequest(Request r) {
        int from_id = r.getpeerId();
        LOGGER.info("Peer [" + myId + "] received request from [" + from_id + "].");
		
	}

    @Override
	public void onChoke(Choke c) {
        int from_id = c.getpeerId();
        LOGGER.info("Peer [" + myId + "] is choked by [" + from_id + "].");
		
    }
    
	@Override
	public void onUnChoke(UnChoke unc) {
        // Send a request message. A reply is not guaranteed.
        int from_id = unc.getpeerId();
        LOGGER.info("Peer [" + myId + "] is unchoked by [" + from_id + "].");
	}

	@Override
	public void onPeerJoined(int peer_id) {
        LOGGER.debug("A new friend with ID=" + peer_id + " has joined");
	}

	@Override
	public void onPeerLeft(int peer_id) {
		LOGGER.debug("A friend left with ID=" + peer_id);
	}

}