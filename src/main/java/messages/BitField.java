package messages;

import java.util.BitSet;

public class BitField extends MessageType {
	public BitField(BitSet bits) {
		super.message_type = "BITFIELD";
		super.message_payload = bits.toByteArray();
		super.message_length += super.message_payload.length; //Variable
	}
	
	public BitSet getBitSet(){
		return BitSet.valueOf(super.message_payload);
	}

}
