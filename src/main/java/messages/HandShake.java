package messages;

import java.nio.ByteBuffer;

public class HandShake extends MessageType {
	public String handshakeHeader;
	public int peerId;

	public HandShake(int peerId) {
		this.handshakeHeader = "P2PFILESHARINGPROJ";
		this.peerId = peerId;
		super.message_type = "HANDSHAKE";
		super.message_length = 32;

		super.message_payload = new byte[32];

		/*
		 * The handshake header is 18-byte string ‘P2PFILESHARINGPROJ’, which is
		 * followed by 10-byte zero bits, which is followed by 4-byte peer ID which is
		 * the integer representation of the peer ID.
		 */
		super.message_payload = this.handshakeHeader.getBytes(); // first 18 bytes
		System.arraycopy(ByteBuffer.allocate(4).putInt(peerId).array(), 0, super.message_payload, 28, 4);// last 4 bytes

	}
	
}
