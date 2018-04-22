package messages;

public class NotInterested extends MessageType {
	public NotInterested(int peer_id) {
        super(peer_id);
		super.message_type = "NOTINTERESTED";
	}
}
