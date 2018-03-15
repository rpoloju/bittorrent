import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

//import init.LogConfig;

public class StartRemotePeers {

	private static final String scriptPrefix = "peerProcess ";

	public static void main(String[] args) {

		ArrayList<RemotePeerInfo> peerList = new ArrayList<>();

		String ciseUser = "Ravi Poloju"; // change with your CISE username
		String st;
		/**
		 * Make sure the below peer hostnames and peerIDs match those in PeerInfo.cfg in
		 * the remote CISE machines. Also make sure that the peers which have the file
		 * initially have it under the 'peer_[peerID]' folder.
		 */

		try {
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while ((st = in.readLine()) != null) {

				String[] tokens = st.split("\\s+");
				peerList.add(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
			}
			in.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		for (RemotePeerInfo remotePeer : peerList) {
			try {
				JSch jsch = new JSch();
				/*
				 * Give the path to your private key. Make sure your public key is already
				 * within your remote CISE machine to ssh into it without a password. Or you can
				 * use the corressponding method of JSch which accepts a password.
				 */
				//jsch.addIdentity("C:\\Users\\vazra\\.ssh\\private", "");
				
				Session session = jsch.getSession(ciseUser, remotePeer.getPeerHostName(), 22);
				session.setPassword("Rpoloju");
				Properties config = new Properties();
				config.put("StrictHostKeyChecking", "no");
				session.setConfig(config);

				session.connect();

				System.out
						.println("Session to peer# " + remotePeer.getPeerId() + " at " + remotePeer.getPeerHostName());

				Channel channel = session.openChannel("exec");
				System.out.println("remotePeerID" + remotePeer.getPeerId());
				((ChannelExec) channel).setCommand(scriptPrefix + remotePeer.getPeerId());

				channel.setInputStream(null);
				((ChannelExec) channel).setErrStream(System.err);

				InputStream input = channel.getInputStream();
				channel.connect();

				System.out.println("Channel Connected to peer# " + remotePeer.getPeerId() + " at "
						+ remotePeer.getPeerHostName() + " server with commands");

				(new Thread() {
					@Override
					public void run() {

						InputStreamReader inputReader = new InputStreamReader(input);
						BufferedReader bufferedReader = new BufferedReader(inputReader);
						String line = null;

						try {

							while ((line = bufferedReader.readLine()) != null) {
								System.out.println(remotePeer.getPeerId() + ">:" + line);
							}
							bufferedReader.close();
							inputReader.close();
						} catch (Exception ex) {
							System.out.println(remotePeer.getPeerId() + " Exception >:");
							ex.printStackTrace();
						}

						channel.disconnect();
						session.disconnect();
					}
				}).start();

			} catch (JSchException e) {
				System.out.println(remotePeer.getPeerId() + " JSchException >:");
				e.printStackTrace();
			} catch (IOException ex) {
				System.out.println(remotePeer.getPeerId() + " Exception >:");
				ex.printStackTrace();
			}

		}
	}

}