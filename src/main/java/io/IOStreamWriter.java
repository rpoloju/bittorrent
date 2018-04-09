package io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

import messages.MessageType;

public class IOStreamWriter extends DataOutputStream implements ObjectOutput {

	public IOStreamWriter(OutputStream in) throws IOException, SecurityException {
		super(in);
	}

	@Override
	public void writeObject(Object object) throws IOException {
		try {
			if (object instanceof MessageType)
				((MessageType) object).write(this);
		} catch (IOException io) {
			io.printStackTrace();
		}
	}
}
