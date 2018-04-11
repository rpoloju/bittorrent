package configuration;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import messages.BitField;
import messages.Have;
import messages.Interested;
import messages.MessageType;
import messages.NotInterested;
import messages.Piece;
import messages.Request;

public class MessageHandler {
	private int remotepeerID;
	private int selfpeerID;
	private FileManager fmgr;
	public PeerHandler phandler;

	public MessageHandler(int self, int remote, FileManager fmgr) {
		this.selfpeerID = self;
		this.fmgr = fmgr;
		this.remotepeerID = remote;
		this.phandler = PeerHandler.getInstance();
	}

	public MessageType handleRequest(MessageType msg) {
		MessageType MESSAGE = null;
		Peer remotepeer = this.phandler.getPeer(remotepeerID);
		Peer selfpeer = this.phandler.getPeer(selfpeerID);

		BitSet required = selfpeer.getRequiredPart(remotepeer.availableParts);

		if (msg.message_type.toUpperCase().equals("UNCHOKE")) {
			// send request message for a piece
			this.phandler.getPeer(remotepeerID).RemoteUnchoke();
			LogConfig.getLogRecord().unchoked(remotepeerID);
			if (required.isEmpty()) {
				// send Not interested message
				this.phandler.insertChokedPeer(remotepeer);
				return new NotInterested();
			} else {
				// send reuqest message of random piece index
				int index = pickRandomIndex(required);
				this.phandler.getPeer(selfpeerID).setAvailablePartsIndex(index);
				byte[] b = ByteBuffer.allocate(4).putInt(index).array();
				return new Request(b);
			}
			
		} else if (msg.message_type.toUpperCase().equals("CHOKE")) {
			this.phandler.getPeer(remotepeerID).RemoteChoke();
			LogConfig.getLogRecord().choked(remotepeerID);
			
		} else if (msg.message_type.toUpperCase().equals("INTERESTED")) {
			// should add remotepeer in the list of preferred peers
			LogConfig.getLogRecord().receivedInterested(remotepeerID);
			this.phandler.addPreferredPeer(remotepeerID);
			LogConfig.getLogRecord().changeOfPrefereedNeighbors(this.phandler.getPreferredPeers());
			return null;
			
		} else if (msg.message_type.toUpperCase().equals("NOTINTERESTED")) {
			// should remove remotepeer in the list of preferred peers
			LogConfig.getLogRecord().receivedNotInterested(remotepeerID);
			this.phandler.removePreferredPeer(remotepeerID);
			LogConfig.getLogRecord().changeOfPrefereedNeighbors(this.phandler.getPreferredPeers());
			return null;
			
		} else if (msg.message_type.toUpperCase().equals("HAVE")) {
			Have h = (Have) msg;
			int index = h.getpieceIndex();
			LogConfig.getLogRecord().receivedHave(remotepeerID, index);
			remotepeer.setAvailablePartsIndex(index);
			this.phandler.getPeer(remotepeerID).setAvailablePartsIndex(index);
			required = selfpeer.getRequiredPart(remotepeer.availableParts);
			if (required.get(index)) {
				return new Interested(); // interesting piece
				
			} else {
				return new NotInterested(); // already have the piece
			}
			
		} else if (msg.message_type.toUpperCase().equals("BITFIELD")) {
			BitField bf = (BitField) msg;
			this.phandler.getPeer(remotepeerID).setparts(bf.getBitSet());
			LogConfig.getLogRecord().debugLog("Bitfield recieved :" + bf.getBitSet() + "from peer:" + remotepeerID);
			if (selfpeer.getRequiredPart(bf.getBitSet()).isEmpty()) {
				return new NotInterested();
				
			} else {
				return new Interested();
			}
			
		} else if (msg.message_type.toUpperCase().equals("REQUEST")) {
			Request r = (Request) msg;
			return new Piece(r.message_payload, fmgr.getBytefromtheIndex(r.getRequestIndex()));
			
		} else if (msg.message_type.toUpperCase().equals("PIECE")) {
			Piece p = (Piece) msg;
			int i = p.getpieceIndex();
			fmgr.savePart(i, p.getPieceContent());
			LogConfig.getLogRecord().pieceDownloaded(this.remotepeerID, i,
					this.phandler.getPeer(selfpeerID).availableParts.cardinality());
			sendHave(i);
			this.phandler.getPeer(remotepeerID).set_downloadrate(p.getPieceContent().length);
			required = this.phandler.getPeer(selfpeerID).getRequiredPart(remotepeer.availableParts);
			int inx = pickRandomIndex(required);
			if (!remotepeer.isRemoteChoke() && inx >= 0) {
				this.phandler.getPeer(selfpeerID).setAvailablePartsIndex(inx);
				byte[] b = ByteBuffer.allocate(4).putInt(inx).array();
				return new Request(b);
			}
			
		} else if (msg.message_type.toUpperCase().equals("HANDSHAKE")) {
			// after handshake get the bitfield of selfpeer and send it to remote
			LogConfig.getLogRecord()
					.debugLog("Sending bitfield of parts: " + selfpeer.availableParts + "to peers:" + remotepeerID);
			MESSAGE = new BitField(selfpeer.availableParts);
			
		} else {
			LogConfig.getLogRecord().debugLog("Illegal Type of message recieved");
		}

		return MESSAGE;
	}

	public int pickRandomIndex(BitSet required) {
		LogConfig.getLogRecord().debugLog("required: " + required);
		List<Integer> indexes = new ArrayList<Integer>();
		Random randomGenerator = new Random();
		for (int i = required.nextSetBit(0); i != -1; i = required.nextSetBit(i + 1)) {
			indexes.add(i);
		}
		if (indexes.size() > 0) {
			int index = randomGenerator.nextInt(indexes.size());
			LogConfig.getLogRecord().debugLog("Random generated index:" + indexes.get(index));
			return indexes.get(index);
		} else {
			return -1;
		}
	}

	public void sendHave(int index) {
		for (int key : this.phandler.getPeersList()) {
			if (key == this.selfpeerID)
				continue;
			if (this.selfpeerID != key) {
				SocketConnectionHandler con = this.phandler.ConnectionTable.get(key);
				if (con != null) {
					LogConfig.getLogRecord().debugLog("sending have for index:" + index + "to peer:" + key);
					con.send(new Have(index));
					if (this.phandler.getPeer(selfpeerID).hasFile) {
						con.send(new NotInterested());
					}
				}
			}
		}
	}

}
