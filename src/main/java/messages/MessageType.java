/* CNT 5106c Spring 2018
 * Ravi Teja Poloju
 * 
 * This is the super class of all message types.
 * Choke = 0, Unchoke = 1, Interested = 2, Not Interested = 3,
 * Have = 4, Bitfield = 5, Request = 6, Piece = 7
 */

package messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;


public class MessageType {

	static Map<Byte, String> messageType = new HashMap<>();
    int peer_id;
    
	MessageType(int peer_id) {
        this.peer_id = peer_id;
		messageType.put((byte) 0, "CHOKE");
		messageType.put((byte) 1, "UNCHOKE");
		messageType.put((byte) 2, "INTERESTED");
		messageType.put((byte) 3, "NOTINTERESTED");
		messageType.put((byte) 4, "HAVE");
		messageType.put((byte) 5, "BITFIELD");
		messageType.put((byte) 6, "REQUEST");
		messageType.put((byte) 7, "PIECE");
		messageType.put((byte) 8, "HANDSHAKE");
	}

	public static String getTypeFromMessageValue(byte messageValue) {
		return messageType.get(messageValue);
	}

	public static Byte getCodeFromMessageType(String messageType) {
		switch (messageType.toUpperCase()) {
		case "CHOKE":
			return 0;
		case "UNCHOKE":
			return 1;
		case "INTERESTED":
			return 2;
		case "NOTINTERESTED":
			return 3;
		case "HAVE":
			return 4;
		case "BITFIELD":
			return 5;
		case "REQUEST":
			return 6;
		case "PIECE":
			return 7;
		}
		return null;
	}

	/*
	 * An actual message consists of 4-byte message length field, 1-byte message
	 * type field, and a message payload with variable size. So the default message
	 * length would be 1 which is occupied by message type
	 */
	public int message_length = 1; // can be in the range from 1 to 5 inclusive
	public String message_type = "";
	public byte[] message_payload = {};

	public int getLength() {
		return this.message_length;
	}

	public String getType() {
		return this.message_type;
	}

	public byte[] getPayload() {
		return this.message_payload;
    }

    public int getpeerId() {
        // internal peer id for DuplexServer
        return this.peer_id;
    }

    public ByteBuffer get_buffer() {
        byte[] message_length = ByteBuffer.allocate(4).putInt(1 + this.message_payload.length).array();
        // message_length[0] = (byte) 127;
        // Maybe flip the bytes around instead. Or add/subtract bytes rather than ints
        byte[] type = ByteBuffer.allocate(1).put(getCodeFromMessageType(this.message_type)).array();

        byte[] message = ByteBuffer.allocate(message_length.length + type.length + this.message_payload.length).array();
        System.arraycopy(message_length, 0, message, 0, message_length.length);
        System.arraycopy(type, 0, message, message_length.length, type.length);
        System.arraycopy(message_payload, 0, message, message_length.length + type.length, message_payload.length);

        // for (byte b : message) {
        //     System.out.println(new Integer((int)b));
        // }

        return ByteBuffer.wrap(message);
    }

}
