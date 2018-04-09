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

import configuration.LogConfig;
import io.IOStreamReader;
import io.IOStreamWriter;

public class MessageType {

	static Map<Byte, String> messageType = new HashMap<>();

	MessageType() {
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
		case "UNINTERESTED":
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
			return new BitField(new BitSet());
		else if (type.toUpperCase().equals("REQUEST"))
			return new Request();
		else if (type.toUpperCase().equals("PIECE"))
			return new Piece();
		else if (type.toUpperCase().equals("HANDSHAKE"))
			return new HandShake(0);
		else {
			throw new ClassNotFoundException("Message of type " + type + " not found");
		}

	}

	// Method to read from OubjectInputStream
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

	// Method to write to ObjectOutputStream
	public void write(IOStreamWriter ioStreamWriter) throws IOException {
		LogConfig.getLogRecord().debugLog("writing message");
		byte buf[] = ByteBuffer.allocate(4 + message_length).putInt(message_length).array();
		buf[4] = getCodeFromMessageType(this.message_type);
		if (message_payload != null && message_payload.length > 0) {
			System.arraycopy(message_payload, 0, buf, 5, message_payload.length);
		}
		ioStreamWriter.write(buf);
	}

}
