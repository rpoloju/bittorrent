package messages;

public class Request extends MessageType {

	public Request() {
		super.message_type = "REQUEST";
	}

	public Request(byte[] requestingPieceIndexField) {
		super.message_type = "REQUEST";
		super.message_payload = requestingPieceIndexField;
		super.message_length += super.message_payload.length;
	}

}
