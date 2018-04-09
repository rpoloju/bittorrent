package filemanagement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import configuration.Initialization;
import configuration.LogConfig;

/* FileMerge takes name of the file to while all the split files should be merged
 * initProperties() - takes output file name and create cfg object
 * mergeFiles() - looks into splitParts folder reads each file and writes into output file
 */
public class FileMerge implements Initialization {
	public File outFile = null;
	public String outFileName = null;
	private int noOfSplits;
	private int peerId;

	public FileMerge(int peerId, int noOfSplits, String outputFileName) {
		this.peerId = peerId;
		this.noOfSplits = noOfSplits;
		this.outFileName = outputFileName;
		outFile = new File(System.getProperty("user.dir") + "/peer_" + peerId + "/" + outFileName);
	}

	/*
	 * here cfg object can be used to check the size of combined file whether it is
	 * equal to all split files combined whether the size of each piece is equal to
	 * given value or not etc
	 */

	public FileMerge(String string) {
		outFileName = string;
		outFile = new File(System.getProperty("user.dir") + "/peer_" + peerId + "/" + outFileName);
	}

	@Override
	public void initialize() {

	}

	@Override
	public void reload() {

	}

	public void mergeFiles() {
		try {
			FileOutputStream outFileStream = new FileOutputStream(outFile);
			FileInputStream inpFile = null;

			String splitDirectoryPath = System.getProperty("user.dir") + "/peer_" + peerId;
			for (int i = 0; i < noOfSplits; i++) {
				File f = new File(splitDirectoryPath + "/" + i + "_" + outFileName);
				inpFile = new FileInputStream(f);
				int fileSize = (int) f.length();
				byte[] buffer = new byte[fileSize];
				int lengthRead = inpFile.read(buffer, 0, fileSize);
				if (lengthRead > 0) {
					outFileStream.write(buffer);
				}
				inpFile.close();
			}
			outFileStream.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteFiles() {
		try {
			String splitDirectoryPath = System.getProperty("user.dir") + "/peer_" + peerId;
			for (int i = 0; i < noOfSplits; i++) {
				LogConfig.getLogRecord().debugLog("Printing file : " + i + "_" + outFileName);
				File f = new File(splitDirectoryPath + "/" + i + "_" + outFileName);
				f.delete();
			}

			LogConfig.getLogRecord().debugLog("File Merge is sucessful");
		} catch (Exception e) {
			LogConfig.getLogRecord().debugLog("Error occured while merging file");
		}
	}

}
