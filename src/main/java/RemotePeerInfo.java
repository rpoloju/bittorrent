/* CNT5106C Spring 2018
 * 
 * Ravi Teja Poloju
 * 
 * This class defines the structure of peer info
 * Eg.:	1001 lin114-00.cise.ufl.edu 6008 1
 *		[peer ID] [host name] [listening port] [has file or not]
 */

public class RemotePeerInfo {

	public String peerId;
	public String peerHostName;
	public String peerPortNumber;

	public RemotePeerInfo(String peerId, String peerHostName, String peerPortNumber) {
		this.peerId = peerId;
		this.peerHostName = peerHostName;
		this.peerPortNumber = peerPortNumber;
	}

	public String getPeerId() {
		return peerId;
	}

	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}

	public String getPeerHostName() {
		return peerHostName;
	}

	public void setPeerHostName(String peerHostName) {
		this.peerHostName = peerHostName;
	}

	public String getPeerPortNumber() {
		return peerPortNumber;
	}

	public void setPeerPortNumber(String peerPortNumber) {
		this.peerPortNumber = peerPortNumber;
	}
	
}
