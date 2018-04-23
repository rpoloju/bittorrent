package messages;

import java.io.IOException;
import java.nio.ByteBuffer;


public class Piece extends MessageType {

	public Piece(int peer_id) {
        super(peer_id);
		super.message_type = "PIECE";
	}

    public Piece(int peer_id, int pieceIndex, byte[] pieceContent) {
        super(peer_id);
        super.message_type = "PIECE";

        super.message_payload = new byte[pieceContent.length + 4];
        byte[] pieceIndex_asArray = ByteBuffer.allocate(4).putInt(pieceIndex).array();
		System.arraycopy(pieceIndex_asArray, 0, super.message_payload, 0, pieceIndex_asArray.length);
		System.arraycopy(pieceContent, 0, super.message_payload, pieceIndex_asArray.length, pieceContent.length);

		super.message_length += super.message_payload.length;
    }

	public Piece(int peer_id, byte[] pieceIndex, byte[] pieceContent) {
        super(peer_id);
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
}
