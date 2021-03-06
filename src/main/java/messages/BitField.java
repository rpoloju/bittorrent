package messages;

import java.util.BitSet;

public class BitField extends MessageType {
	public BitField(int peer_id, BitSet bits) {
        super(peer_id);
		super.message_type = "BITFIELD";
		super.message_payload = bits.toByteArray();
        super.message_length += super.message_payload.length; 
        super.message_length += 1;
	}
	
	public BitSet getBitSet() {
		return BitSet.valueOf(super.message_payload);
    }
    
    public boolean hasNothing() {
        BitSet bs = BitSet.valueOf(super.message_payload);
        for (byte b : super.message_payload)
        {
            if ((int) b != 0)
            {
                return false;
            }
        }
        return true;        
    }
}
