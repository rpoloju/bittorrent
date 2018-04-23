import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

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
 * https://github.com/w-garcia
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
    // Download rate: keep a counter of each peer when we get a piece from there. Reset on timer.
    // Pieces are constant size so count of pieces received during each interval is accurate. 
    private FileProcessor file_processor;
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

        String file_name = "image.jpg";
        file_processor = new FileProcessor(file_name, ccfg, my_info);
        pieces = file_processor.get_num_pieces();
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
        int k = numberOfPreferredNeighbors;
        ArrayList<Integer> temp = new ArrayList<>(interested_peers);
        ArrayList<Integer> old_choke = choked_peers;
        ArrayList<Integer> old_preferrs = preferred_peers;
        ArrayList<Integer> new_choke;
        Collections.shuffle(temp);
        
        // Create new choked_peers (k to n)

        if (interested_peers.size() >= k) {
            preferred_peers = new ArrayList<>(temp.subList(0, k));
            new_choke = new ArrayList<>(temp.subList(k, temp.size()));
            
        } else {
            preferred_peers = temp;
            new_choke = new ArrayList<>();
        }

        LOGGER.info("Peer ["+ myId + "] has the preffered neighbors " + preferred_peers.toString() + ".");

        for (int peer_id : interested_peers) {
            if (peer_id == optimistic_unchoked_peer) {
                continue;
            }
            if (!new_choke.contains(peer_id) && old_choke.contains(peer_id)) { // State change choked->unchoked
                send_message(new UnChoke(peer_id), peer_id);
            } else if (new_choke.contains(peer_id) && !old_choke.contains(peer_id)) { // State change unchoked/idle->choked
                send_message(new Choke(peer_id), peer_id);
            } else if (preferred_peers.contains(peer_id) && old_preferrs.contains(peer_id)) {
                // Already unchoked
            } else if (!new_choke.contains(peer_id) && !old_choke.contains(peer_id)) { // either new or already unchoked
                send_message(new UnChoke(peer_id), peer_id);
            } 
        }
        choked_peers = new_choke;
    }

    private void recalculate_optim_unchoke() {
        if (choked_peers.size() == 0) 
        {
            return;
        }

        Random r = new Random();        
        int rand_idx = r.nextInt(choked_peers.size());
        int new_optim_peer = choked_peers.get(rand_idx);

        if (optimistic_unchoked_peer == new_optim_peer) {

        } else {
            if (!preferred_peers.contains(optimistic_unchoked_peer)) // Only choke if not in preferred (prefferd + optim edge case)
                send_message(new Choke(optimistic_unchoked_peer), optimistic_unchoked_peer);

            send_message(new UnChoke(new_optim_peer), new_optim_peer); 
        }

        optimistic_unchoked_peer = new_optim_peer;
        LOGGER.info("Peer ["+ myId + "] has the optimistically unchoked neighbor [" + optimistic_unchoked_peer + "]");
    }

    /////////// Meta data handlers

    private MessageType check_interest(BitField bf) {
        // Determine if send interested or not interested
        int from_id = bf.getpeerId();                
        BitSet peer_set = bf.getBitSet();

        for (int i = 0; i < pieces; i++) {
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

    private int check_interested_index() {
        ArrayList<Integer> idx_to_get = new ArrayList<>();
        for (int i = 0; i < pieces; i++) {
            boolean mine = have_field.get(i);
            if (mine == false) idx_to_get.add(i);
        }

        Random r = new Random();
        return idx_to_get.get(r.nextInt(idx_to_get.size()));
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
            peer_to_have_field.put(from_id, map_get);
        } else {
            LOGGER.debug("Got a HAVE for an unitialized Peer, ID=" + from_id);
        }
    }

    private boolean am_done() {
        // Go through all peers and make sure they are finished
        for (int peer_id : peer_to_have_field.keySet()) {
            BitSet bs = peer_to_have_field.get(peer_id);
            for (int i = 0; i < pieces; i++) {
                boolean theirs = bs.get(i);
                if (!theirs) {
                    return false;
                }
            }
        }

        return true;
    }

    private void exit() {
        for (int peer_id : peer_to_have_field.keySet()) {
            try {
                listener.send_message(ByteBuffer.wrap(new byte[]{0}), peer_id);
            } catch (IOException e) {
                LOGGER.warn("Some friends left unexpectedly!");
                LOGGER.error(e.getMessage());
                System.out.println(e.getStackTrace());
            }
        }

        unchoke_timer.stop();
        optim_unchoke_timer.stop();
    }

    void send_message(MessageType msg, int peer_id) {
        try {
            // byte[] bss = new byte[] {0, 0, 0, 1};
            // System.out.println("The length is " + ByteBuffer.wrap(bss).getInt());            
            // listener.send_message(ByteBuffer.wrap(bss), from_id);            
            listener.send_message(msg.get_buffer(), peer_id);
        } catch (IOException e) {
            LOGGER.warn("Friend left unexpectedly!");
            LOGGER.error(e.getMessage());
            System.out.println(e.getStackTrace());
        }
    }

    void broadcast(MessageType msg) {
        try {
            listener.broadcast_to_peers(msg.get_buffer());
        } catch (IOException e) {
            LOGGER.warn("Some friends left unexpectedly!");
            LOGGER.error(e.getMessage());
            System.out.println(e.getStackTrace());
        }
    }

    ////////////// BTP Event handlers

	@Override
	public void onHandShake(HandShake hs) {
        // System.out.println("We have handshake");
        
        int from_id = hs.getpeerId();
        
        // Add peer's bitfield assuming it is empty, since empty peers won't send me anything.
        BitField theirs = new BitField(from_id, new BitSet(pieces));
        update_peer_map(theirs);

        // create my bitfield & send
        BitField bf = new BitField(from_id, have_field);

        if (!bf.hasNothing())
        {
            LOGGER.debug("Sending my bitfield to Peer " + from_id);
            
            send_message(bf, from_id);
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

        send_message(response, from_id);
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

        send_message(response, from_id);
	}

	@Override
	public void onInterested(Interested in) {
        int from_id = in.getpeerId();
        LOGGER.info("Peer [" + myId + "] received the 'interested' message from [" + from_id + "].");

        if (!interested_peers.contains(Integer.valueOf(from_id))) {
            interested_peers.add(Integer.valueOf(from_id));
        }
	}

	@Override
	public void onNotInterested(NotInterested nin) {
		int from_id = nin.getpeerId();
        LOGGER.info("Peer [" + myId + "] received the 'not interested' message from [" + from_id + "].");

        // Not sure about this
        // if (interested_peers.contains(Integer.valueOf(from_id))) {
        //     interested_peers.remove(Integer.valueOf(from_id));
        // }
	}

	@Override
	public void onPiece(Piece p) {
        // Update my map, send Have, and try to request another piece. 
        int from_id = p.getpeerId();
        LOGGER.info("Peer [" + myId + "] received piece from [" + from_id + "].");
        int idx = p.getpieceIndex();

        byte[] piece_content = p.getPieceContent();
        int result = file_processor.put_piece(idx, piece_content);
        if (result == Constants.ALREADY_HAVE) {
            LOGGER.debug("Peer [" + myId + "] received piece from [" + from_id + "] but already had it.");
        } else if (result == Constants.EMPTY_PIECE_RCV) {
            LOGGER.debug("Peer [" + myId + "] received piece from [" + from_id + "] but it was empty.");        
        } else {
            // FILE_COMPLETE or GOOD_PIECE
            have_field.set(idx);
            Have h = new Have(myId, idx);
            broadcast(h);
        }

        // Exit or check for another piece I'll want
        if (result == Constants.FILE_COMPLETE) {
            if (am_done()) {
                // exit(); // Needs to be tested some more.
            }
        } else {
            int req_idx = check_interested_index();
            Request r = new Request(from_id, req_idx);
            send_message(r, from_id);
        }
	}

	@Override
	public void onRequest(Request r) {
        int from_id = r.getpeerId();
        int idx = r.getRequestIndex();
        LOGGER.info("Peer [" + myId + "] received request from [" + from_id + "] for piece [" + idx + "].");
        
        // Retreive the piece corresponding to index, create msg and send. 
        byte[] piece_content = file_processor.get_piece(idx);
        if (piece_content.length == 0) {
            LOGGER.warn("Peer [" + myId + "] requested from [" + from_id + "] non-existant piece [" + idx + "].");
        } else {
            Piece p = new Piece(from_id, idx, piece_content);
            send_message(p, from_id);
        }
	}

    @Override
	public void onChoke(Choke c) {
        int from_id = c.getpeerId();
        LOGGER.info("Peer [" + myId + "] is choked by [" + from_id + "].");
		// This could be kept track of, but not really necessary. Peer can just wait until unchoke event to reinitiate transfer. 
    }
    
	@Override
	public void onUnChoke(UnChoke unc) {
        // Send a request message. A reply is not guaranteed.
        int from_id = unc.getpeerId();
        LOGGER.info("Peer [" + myId + "] is unchoked by [" + from_id + "].");

        BitSet peer_set = peer_to_have_field.get(from_id);
        ArrayList<Integer> possible_idx = new ArrayList<>();

        for (int i = 0; i < pieces; i++) {
            boolean mine = have_field.get(i);
            boolean theirs = peer_set.get(i);

            if (mine == false && theirs == true) {
                possible_idx.add(i);
            }
        }

        Random r = new Random();
        int idx = possible_idx.get(r.nextInt(possible_idx.size()));

        Request req = new Request(from_id, idx);
        send_message(req, from_id);
	}

	@Override
	public void onPeerJoined(int peer_id) {
        LOGGER.debug("A new friend with ID=" + peer_id + " has joined");
	}

	@Override
	public void onPeerLeft(int peer_id) {
        LOGGER.debug("A friend left with ID=" + peer_id);

        if (interested_peers.contains(Integer.valueOf(peer_id))) {
            interested_peers.remove(Integer.valueOf(peer_id));
        }

        if (peer_to_have_field.get(peer_id) != null) {
            peer_to_have_field.remove(peer_id);
        }
	}

}