package configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class PeerInfoConfig implements Initialization {
	public List<Peer> peersList = new ArrayList<Peer>();
	String filePath = "PeerInfo.cfg";

	@Override
	public void initialize() {
		String eachLine;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(this.filePath));
			while ((eachLine = br.readLine()) != null) {
				String[] tokens = eachLine.split("\\s+");
				Boolean hasFile = tokens[3].equals("1") ? true : false;
				Peer peer = new Peer(Integer.parseInt(tokens[0]), tokens[1], Integer.parseInt(tokens[2]), hasFile);
				peersList.add(peer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} /*
			 * finally { if (br != null) { br.close(); } }
			 */

	}

	@Override
	public void reload() {
		peersList.clear();
		initialize();

	}

}
