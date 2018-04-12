package messages;

import java.io.IOException;
import java.nio.ByteBuffer;

import configuration.LogConfig;
import io.IOStreamReader;

public class Piece extends MessageType {

	public Piece() {
		super.message_type = "PIECE";
	}

	public Piece(byte[] pieceIndex, byte[] pieceContent) {
		super.message_type = "PIECE";
		
		super.message_payload = new byte[pieceContent.length + 4];
		System.arraycopy(pieceIndex, 0, super.message_payload, 0, pieceIndex.length);
		System.arraycopy(pieceContent, 0, super.message_payload, pieceIndex.length, pieceContent.length);

		super.message_length += super.message_payload.length;
	}

	public int getpieceIndex() {
		byte[] pieceIndex = new byte[4];
		for (int i = 0; i < 4; i++) {
			pieceIndex[i] = super.message_payload[i];
		}
		ByteBuffer buf = ByteBuffer.wrap(pieceIndex);
		return buf.getInt();
	}

	public byte[] getPieceContent() {
		byte[] pieceContent = new byte[super.message_payload.length - 4]; // excluding msg_type length and pieceIndex
																			// length
		System.arraycopy(super.message_payload, 4, pieceContent, 0, pieceContent.length);
		return pieceContent;
	}

	@Override
	public void read(IOStreamReader ioStreamReader, int length) throws IOException {
		if (length > 0) {
			message_payload = new byte[length];
			if (message_payload != null && message_payload.length > 0)
				ioStreamReader.readFully(message_payload, 0, length);
			else {
				LogConfig.getLogRecord().debugLog("Payload is empty");
				throw new IOException("Payload is empty");
			}
		}
	}
}
