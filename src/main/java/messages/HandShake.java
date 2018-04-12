package messages;

import java.io.IOException;
import java.nio.ByteBuffer;

import configuration.LogConfig;
import io.IOStreamWriter;

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
		System.arraycopy(this.handshakeHeader.getBytes(),0,super.message_payload,0,18);
		System.arraycopy(ByteBuffer.allocate(4).putInt(peerId).array(), 0, super.message_payload, 28, 4);// last 4 bytes

	}

	public int getpeerId() {
		return this.peerId;
	}
	
	@Override
	public void write(IOStreamWriter out) throws IOException {
		LogConfig.getLogRecord().debugLog("writing handshake message");
		
		if (message_payload != null && message_payload.length > 0)
			out.write(message_payload, 0, message_payload.length);

		LogConfig.getLogRecord().debugLog("done writing");
	}

}
