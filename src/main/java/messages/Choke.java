package messages;

public class Choke extends MessageType {
	public Choke(int peer_id) {
        super(peer_id);
		super.message_type = "CHOKE";
	}


}
