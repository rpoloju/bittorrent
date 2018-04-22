package messages;

public class UnChoke extends MessageType {
	public UnChoke(int peer_id) {
        super(peer_id);
		super.message_type = "UNCHOKE";
	}

}
