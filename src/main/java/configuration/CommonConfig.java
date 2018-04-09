package configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;


public class CommonConfig {

	public int numOfPrefNeighbours;
	public int unchokeInterval;
	public int optUnchokeInterval;
	public String fileName;
	public int fileSize;
	public int pieceSize;
	HashMap<String, String> cfgValues = new HashMap<String, String>();

	String filePath = "Common.cfg";

	public void initialize() {
		String st;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			while ((st = br.readLine()) != null) {
				String[] tokens = st.split("\\s+");
				cfgValues.put(tokens[0], tokens[1]);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		numOfPrefNeighbours = Integer.parseInt(cfgValues.get("NumberOfPreferredNeighbors"));
		unchokeInterval = Integer.parseInt(cfgValues.get("UnchokingInterval"));
		optUnchokeInterval = Integer.parseInt(cfgValues.get("OptimisticUnchokingInterval"));
		fileName = cfgValues.get("FileName");
		fileSize = Integer.parseInt(cfgValues.get("FileSize"));
		pieceSize = Integer.parseInt(cfgValues.get("PieceSize"));
		
	}

	public void reload() {
		cfgValues.clear();
		initialize();
	}

}
