/* CNT5106C Spring 2018
 * 
 * Ravi Teja Poloju
 * 
 * This class defines the structure of peer info
 * Eg.:	1001 lin114-00.cise.ufl.edu 6008 1
 *		[peer ID] [host name] [listening port] [has file or not]
 */

public class RemotePeerInfo {

	public int peerId;
	public String peerHostName;
	public int peerPortNumber;
	public int hasFile_or_not;

	public RemotePeerInfo(int peerId, String peerHostName, int peerPortNumber, int hasFile_or_not) {
		this.peerId = peerId;
		this.peerHostName = peerHostName;
		this.peerPortNumber = peerPortNumber;
		this.hasFile_or_not = hasFile_or_not;
	}

	public int getPeerId() {
		return peerId;
	}

	public String getPeerHostName() {
		return peerHostName;
	}

	public int getPeerPortNumber() {
		return peerPortNumber;
	}

	public int getHasFile_or_not() {
		return hasFile_or_not;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}

	public void setPeerHostName(String peerHostName) {
		this.peerHostName = peerHostName;
	}

	public void setPeerPortNumber(int peerPortNumber) {
		this.peerPortNumber = peerPortNumber;
	}

	public void setHasFile_or_not(int hasFile_or_not) {
		this.hasFile_or_not = hasFile_or_not;
	}
}
