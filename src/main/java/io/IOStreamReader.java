package io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

import messages.HandShake;
import messages.MessageType;

public class IOStreamReader extends DataInputStream implements ObjectInput {
	private boolean hanshk_recvd = false;

	public IOStreamReader(InputStream in) throws IOException, SecurityException {
		super(in);
	}

	public MessageType readInstanceOf() throws ClassNotFoundException, IOException {
		byte[] b = (byte[]) readObject();
		if (b.length >= 32) {
			String s = new String(b, 0, 17);
			if (s.equals("P2PFILESHARINGPROJ")) {
				return new HandShake(byteArrayToInt(b, 28));
			}
		}

		if (b.length > 4) {
			MessageType msg;
			msg = MessageType.getMessage(MessageType.getTypeFromMessageValue(b[4]));
			// Passing message length - type of 1 byte
			msg.read(this, b.length - 1);
			return msg;
		}

		return null;

	}

	public static int byteArrayToInt(byte[] b, int offset) {

		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}

		return value;
	}

	@Override
	public Object readObject() throws ClassNotFoundException, IOException {
		if (!hanshk_recvd) {
			byte[] b = new byte[available()];
			read(b);
			if (b.length >= 32) {
				String s = new String(b, 0, 18);
				if (s.equals("P2PFILESHARINGPROJ")) {
					hanshk_recvd = true;
					return new HandShake(byteArrayToInt(b, 28));
				}
			}

		} else {
			int len = readInt();
			MessageType msg;
			msg = MessageType.getMessage(MessageType.getTypeFromMessageValue(readByte()));
			// Passing message length - type of 1 byte
			msg.read(this, len - 1);
			return msg;
		}

		return null;
	}

	int fromByteArray(byte[] bytes) {
		return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
	}

}
