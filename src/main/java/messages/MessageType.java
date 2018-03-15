/* CNT 5106c Spring 2018
 * Ravi Teja Poloju
 * 
 * This is the super class of all message types.
 * Choke = 0, Unchoke = 1, Interested = 2, Not Interested = 3,
 * Have = 4, Bitfield = 5, Request = 6, Piece = 7
 */

package messages;

import java.util.HashMap;
import java.util.Map;

public class MessageType {

	static Map<Integer, String> messageType = new HashMap<>();

	MessageType() {
		messageType.put(0, "CHOKE");
		messageType.put(1, "UNCHOKE");
		messageType.put(2, "INTERESTED");
		messageType.put(3, "NOTINTERESTED");
		messageType.put(4, "HAVE");
		messageType.put(5, "BITFIELD");
		messageType.put(6, "REQUEST");
		messageType.put(7, "PIECE");
	}

	public static String getTypeFromMessageValue(int messageValue) {
		return messageType.get(messageValue);
	}

	/*
	 * An actual message consists of 4-byte message length field, 1-byte message
	 * type field, and a message payload with variable size. So the default message
	 * length would be 1 which is occupied by message type
	 */
	public int message_length = 1;
	public String message_type = null;
	public byte[] message_payload = null;

	public int getLength() {
		return this.message_length;
	}

	public String getType() {
		return this.message_type;
	}

	public byte[] getPayload() {
		return this.message_payload;
	}

	public static MessageType getMessage(String type) throws ClassNotFoundException {
		if (type.toUpperCase().equals("CHOKE"))
			return new Choke();
		else if (type.toUpperCase().equals("UNCHOKE"))
			return new UnChoke();
		else if (type.toUpperCase().equals("INTERESTED"))
			return new Interested();
		else if (type.toUpperCase().equals("NOTINTERESTED"))
			return new NotInterested();
		else if (type.toUpperCase().equals("HAVE"))
			return new Have();
		else if (type.toUpperCase().equals("BITFIELD"))
			return new BitField();
		else if (type.toUpperCase().equals("REQUEST"))
			return new Request();
		else if (type.toUpperCase().equals("PIECE"))
			return new Piece();
		else {
			throw new ClassNotFoundException("Message of type " + type + " not found");
		}

	}

}
