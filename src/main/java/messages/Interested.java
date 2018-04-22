package messages;

public class Interested extends MessageType {
	public Interested(int peer_id) {
        super(peer_id);
		super.message_type = "INTERESTED";
	}

}
