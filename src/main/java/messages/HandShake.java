package messages;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HandShake extends MessageType {
	public String handshakeHeader;

	public HandShake(int peer_id) {
        super(peer_id);
		this.handshakeHeader = "P2PFILESHARINGPROJ";
		super.message_type = "HANDSHAKE";
		super.message_length = 32;

		super.message_payload = new byte[32];

		/*
		 * The handshake header is 18-byte string ‘P2PFILESHARINGPROJ’, which is
		 * followed by 10-byte zero bits, which is followed by 4-byte peer ID which is
		 * the integer representation of the peer ID.
		 */
		System.arraycopy(this.handshakeHeader.getBytes(),0,super.message_payload,0,18);
		System.arraycopy(ByteBuffer.allocate(4).putInt(this.peer_id).array(), 0, super.message_payload, 28, 4);// last 4 bytes

	}
}
