package messages;

import java.nio.ByteBuffer;

public class Request extends MessageType {

	public Request(int peer_id) {
        super(peer_id);
		super.message_type = "REQUEST";
	}

	public Request(int peer_id, int requestingPieceIndexField) {
        super(peer_id);
		super.message_type = "REQUEST";
		super.message_payload = ByteBuffer.allocate(4).putInt(requestingPieceIndexField).array();
		super.message_length += super.message_payload.length;
	}

	public int getRequestIndex() {
		int x = java.nio.ByteBuffer.wrap(super.message_payload).getInt();
		return x;
	}

}
