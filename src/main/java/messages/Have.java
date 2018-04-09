package messages;

import java.nio.ByteBuffer;

public class Have extends MessageType {

	public Have() {
		super.message_type = "HAVE";
	}

	public Have(byte[] pieceIndexField) {
		super.message_type = "HAVE";
		super.message_payload = pieceIndexField;
		super.message_length += super.message_payload.length;
	}

	public Have(int index) {
		super.message_type = "HAVE";
		super.message_payload = ByteBuffer.allocate(4).putInt(index).array();
		super.message_length += super.message_payload.length;
	}

	public int getpieceIndex() {
		int x = java.nio.ByteBuffer.wrap(super.message_payload).getInt();
		return x;
	}

}