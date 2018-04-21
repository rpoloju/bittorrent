import java.util.ArrayList;
import java.util.BitSet;

import messages.HandShake;

public class Peer
{
    private SingletonCommon common_cfg;
    private SingletonPeerInfo peer_cfg;
    private BitTorrentProtocol btp;

    public Peer(int peer_id) throws Exception
    {
        // read Common.cfg
        common_cfg = SingletonCommon.getInstance();
                
        // read PeerInfo.cfg
        peer_cfg = SingletonPeerInfo.getInstance();

        btp = new BitTorrentProtocol(common_cfg, peer_cfg, peer_id);
    }
}