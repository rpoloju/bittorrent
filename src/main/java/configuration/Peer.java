package configuration;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

import configuration.LogConfig;

/*
 * This file contains details of the Peer such as id, port, host
 */
public class Peer {

	public int port;
	public String host;
	public int id;
	public boolean hasFile;
	public BitSet availableParts;
	public int noOfParts;
	private AtomicInteger _downloadrate;
	private boolean _isChoke;
	private boolean _isOptunchoke;
	public int rate;
	public boolean remotechoke;

	public Peer(int id, String host, int port, boolean hasfile) {
		this.id = id;
		this.host = host;
		this.port = port;
		this.hasFile = hasfile;
		this._downloadrate = new AtomicInteger(0);
		this.availableParts = new BitSet();
		this.Choke();
		this.OptChoke();
		this.noOfParts = Integer.MAX_VALUE;
		this.remotechoke = true;
	}

	public void setSaveParts(BitSet b) {
		if (b.cardinality() == this.noOfParts) {
			hasFile = true;
		} else {
			hasFile = false;
		}
		availableParts = b;
	}

	public int get_downloadrate() {
		this.rate = _downloadrate.getAndSet(0);
		return this.rate;
	}

	public void setparts(BitSet b) {
		availableParts.or(b);
		if (availableParts.cardinality() == this.noOfParts) {
			hasFile = true;
		} else {
			hasFile = false;
		}

	}

	public void setsaveparts(BitSet b) {
		if (b.cardinality() == this.noOfParts) {
			hasFile = true;
		} else {
			hasFile = false;
		}
		availableParts = b;
	}

	public void settotalParts(int parts) {
		this.noOfParts = parts;
		this.availableParts = new BitSet(this.noOfParts);
	}

	public void setAvailablePartsIndex(int index) {
		if (index > availableParts.size())
			return;
		availableParts.set(index);
		LogConfig.getLogRecord()
				.debugLog("available parts size= " + this.noOfParts + " cardinallity=" + availableParts.cardinality());
		if (availableParts.cardinality() == this.noOfParts) {
			hasFile = true;
		} else {
			hasFile = false;
		}
	}

	public BitSet getRequiredPart(BitSet b) {
		BitSet r = new BitSet(availableParts.size());
		r.or(availableParts);
		r.flip(0, r.size());
		r.and(b); // should flip the available bits and do and with the bitfield received
		return r;
	}

	public int get_rate() {
		return this.rate;
	}

	public int set_downloadrate(int bytelength) {
		this.rate = this._downloadrate.addAndGet(bytelength);
		return this.rate;
	}

	public void Choke() {
		this._isChoke = true;
	}

	public void Unchoke() {
		this._isChoke = false;
	}

	public boolean isRemoteChoke() {
		return this.remotechoke;
	}

	public void RemoteChoke() {
		this.remotechoke = true;
	}

	public void RemoteUnchoke() {
		this.remotechoke = false;
	}

	public void OptChoke() {
		this._isOptunchoke = false;
		this._isChoke = true;
	}

	public void OptunChoke() {
		this._isOptunchoke = true;
		this._isChoke = false;
	}
	/*
	 * public boolean ischoke(){ return this._isChoke; }
	 */
}
