import messages.BitField;
import messages.Choke;
import messages.HandShake;
import messages.Have;
import messages.Interested;
import messages.NotInterested;
import messages.Piece;
import messages.Request;
import messages.UnChoke;

public interface MessageListener {
    void onHandShake(HandShake hs);
    void onBitField(BitField bf);
    void onHave(Have h);
    void onInterested(Interested in);
    void onNotInterested(NotInterested nin);
    void onPiece(Piece p);
    void onRequest(Request r);
    void onChoke(Choke c);
    void onUnChoke(UnChoke unc);
    void onPeerJoined(int peer_id);
    void onPeerLeft(int peer_id);
}