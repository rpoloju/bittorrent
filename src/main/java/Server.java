
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Running: java -cp target/p2p-0.0.1-SNAPSHOT.jar cn.p2p.Server <port>
 */
public class Server {
	private ServerSocket serverSocket;

	private void start(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);

		try {
			serverSocket = new ServerSocket(port);
			int count = 0;
			while (true) {
				ClientSocketHandler h = new ClientSocketHandler(serverSocket.accept(), count);
				h.start();
				count += 1;
			}
		} finally {
			serverSocket.close();
		}
	}

	private static class ClientSocketHandler extends Thread {
		private Socket clientSocket;
		private ObjectInputStream in; // stream read from the socket
		private ObjectOutputStream out; // stream write to the socket
		private String message; // message received from the client
		private String MESSAGE; // uppercase message send to the client
		private int clientID;

		public ClientSocketHandler(Socket connection, int no) {
			this.clientSocket = connection;
			this.clientID = no;
		}

		public void run() {
			try {
				// Wait until a single client requesting
				System.out.println(
						"Got a connection from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(clientSocket.getInputStream());
				// PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				// BufferedReader in = new BufferedReader(
				// new InputStreamReader(clientSocket.getInputStream()));
				try {
					while (true) {
						String message = (String) in.readObject();
						System.out.println("RECEIVE: " + message + " from " + Integer.toString(clientID));

					}
				} catch (ClassNotFoundException classnot) {
					System.err.println("Data received in unknown format");
				}

			} catch (IOException ioException) {
				System.out.println("Disconnect with Client");
			} finally {
				// Close connections
				try {
					in.close();
					out.close();
					clientSocket.close();
				} catch (IOException ioException) {
					System.out.println("Disconnect with Client ");
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("Not enough args: java -cp target/p2p-0.0.1-SNAPSHOT.jar cn.p2p.Client <port>");
			return;
		}
		Server server = new Server();
		server.start(args);

		// System.out.println( "Hello World!" );
	}
}