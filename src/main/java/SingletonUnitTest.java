public class SingletonUnitTest {
    public static void main(String args[]) {
        SingletonPeerInfo peerInfo_cfg = SingletonPeerInfo.getInstance();
        for(int i =1001; i <= 1006; i++) {
            System.out.print((peerInfo_cfg.peerInfoMap.get(i).peerId + " " +
                    peerInfo_cfg.peerInfoMap.get(i).peerHostName + " " +
                    peerInfo_cfg.peerInfoMap.get(i).peerPortNumber + " " +
                    peerInfo_cfg.peerInfoMap.get(i).hasFile_or_not));
            System.out.println();
        }
    }
}
