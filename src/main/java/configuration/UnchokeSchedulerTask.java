package configuration;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.TimerTask;

import messages.Choke;
import messages.UnChoke;

public class UnchokeSchedulerTask extends TimerTask {

	public PeerHandler pHandle;
	public int preferredN;
	private Random _rand;
	private int peerId;
	Queue<Peer> peerMinQueue;

	public UnchokeSchedulerTask(int pId, PeerHandler pHandle, int k) {
		this.pHandle = pHandle;
		this._rand = new Random();
		this.preferredN = k;
		this.peerId = pId;
		this.peerMinQueue = new PriorityQueue<>(downloadrateComparator);
	}

	@Override
	public void run() {
		// find the K max downloadable peers
		List<Integer> peers = this.pHandle.getPreferredPeers();
		if (this.pHandle.getPeer(this.peerId).hasFile) {
			findKprefferedpeerondwnRate(peers, true);
		} else {
			findKprefferedpeerondwnRate(peers, false);
		}

		HashSet<Integer> unchokedlist = new HashSet<Integer>();
		for (Peer p : peerMinQueue) {
			if (this.pHandle.isChoked(p.id)) { // if the is choked
				SocketConnectionHandler sc = this.pHandle.ConnectionTable.get(p.id);
				if (sc != null)
					sc.send(new UnChoke()); // if the new peer is the new unchoked peer send unchoke message
			}

			unchokedlist.add(p.id); // add the pid to the unchoke list

		}
		this.pHandle.resetandaddunChokePeers(unchokedlist); // reset the current unchoked peer and add new one
		if (this.pHandle.getOptunChokedPeer() > 0) {
			this.pHandle.addunChokedPeer(this.pHandle.getOptunChokedPeer()); // add the unchoked peer into the list
		}
		// send choke msg to all choked neighbours
		for (int pId : this.pHandle.getChokedPeers()) {
			SocketConnectionHandler sc = this.pHandle.ConnectionTable.get(pId);
			if (sc != null)
				sc.send(new Choke()); // peer send choke msg
		}
		// set download rate to 0

	}

	public static Comparator<Peer> downloadrateComparator = new Comparator<Peer>() {

		@Override
		public int compare(Peer p1, Peer p2) {
			return (int) (p1.rate - p2.rate);
		}
	};

	public void findKprefferedpeerondwnRate(List<Integer> peers, boolean random) {
		this.peerMinQueue.clear();
		if (random && peers.size() > this.preferredN) {
			while (this.peerMinQueue.size() < this.preferredN) {
				int i = this._rand.nextInt(peers.size());
				Peer p = this.pHandle.getPeer(peers.get(i));
				this.pHandle.getPeer(peers.get(i)).get_downloadrate();// setting the download rate to zero and saving it
																		// in rate variable
				this.peerMinQueue.offer(p);
				peers.remove(i);
			}
		} else {
			for (int i = 0; i < peers.size(); i++) {
				Peer p = this.pHandle.getPeer(peers.get(i));
				this.pHandle.getPeer(peers.get(i)).get_downloadrate();// setting the download rate to zero and saving it
																		// in rate variable
				if (this.peerMinQueue.size() >= this.preferredN) {
					Peer top = this.peerMinQueue.peek();
					if (top.rate == p.rate) {
						if (this._rand.nextBoolean()) {
							this.peerMinQueue.poll();
							this.peerMinQueue.offer(p);
						}
					} else if (top.rate < p.rate) {
						this.peerMinQueue.poll();
						this.peerMinQueue.offer(p);
					}
				} else {
					this.peerMinQueue.offer(p);
				}
			}
		}

	}

}
