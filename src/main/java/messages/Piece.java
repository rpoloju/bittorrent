package messages;

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
}
