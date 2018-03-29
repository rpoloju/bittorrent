package messages;

public class Have extends MessageType {

	public Have() {
		super.message_type = "HAVE";
	}

	public Have(byte[] pieceIndexField) {
		super.message_type = "HAVE";
		super.message_payload = pieceIndexField;
		super.message_length += super.message_payload.length;
	}

}