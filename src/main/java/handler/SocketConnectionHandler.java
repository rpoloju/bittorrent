package handler;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import configuration.LogConfig;
import filemanagement.FileManager;
import io.IOStreamReader;
import io.IOStreamWriter;
import messages.HandShake;
import messages.MessageType;

/***
 * Take care of a single socket connection between client and server
 */

public class SocketConnectionHandler implements Runnable {
	int peerId;
	boolean isconnection = false;
	final public Socket socket;
	public ConnectionState state;
	public int remotepeerId;
	private IOStreamReader in; // stream read from the socket
	private boolean isunchoked;
	private IOStreamWriter out; // stream write to the socket
	public LinkedBlockingQueue<MessageType> msgQueue = new LinkedBlockingQueue<MessageType>();
	private PeerHandler phandler;
	private MessageHandler msgHandler;
	private FileManager fmgr;

	public SocketConnectionHandler(int peerId, Socket socket, PeerHandler p, FileManager f) {
		this.socket = socket;
		this.peerId = peerId;
		this.remotepeerId = -1;
		this.isunchoked = false;
		this.state = ConnectionState.initiated;
		this.msgHandler = null;
		this.fmgr = f;
		phandler = p;

		try {
			out = new IOStreamWriter(socket.getOutputStream());
			out.flush();
			in = new IOStreamReader(socket.getInputStream());
		} catch (IOException e) {
			out = null;
			in = null;
			e.printStackTrace();
		}

	}

	public SocketConnectionHandler(int peerId, int remotePeer, Socket socket, PeerHandler p, FileManager fmgr) {
		this(peerId, socket, p, fmgr);
		this.remotepeerId = remotePeer;

	}

	@Override
	public void run() {
		new Thread() {

			@Override
			public void run() {
				MessageType msg = null;
				// listen to incoming message
				while (!socket.isClosed() || state != ConnectionState.close) {
					try {
						msg = msgQueue.poll();
						if (msg != null) {
							SendMessage(msg);
						}
					} catch (Exception e) {
						LogConfig.getLogRecord().debugLog("error here" + e);
					}
				}
			}
		}.start();

		if (socket == null)
			LogConfig.getLogRecord().debugLog("socket is null ");
		if (!socket.isClosed()) {
			MessageType message = null;
			send(new HandShake(this.peerId));
			try {
				while (!socket.isClosed() || state != ConnectionState.close) {
					// receive the message sent from the client
					try {
						message = (MessageType) in.readObject();

						if (message == null)
							continue;
						if (message instanceof HandShake) {
							HandShake h = (HandShake) message;
							if (this.remotepeerId == -1 || this.remotepeerId == h.peerId) {
								state = ConnectionState.connected;
								this.remotepeerId = h.peerId;
								LogConfig.getLogRecord().isConnected(this.remotepeerId);
								if (phandler.ConnectionTable.get(this.remotepeerId) != null) {
									// do something with the old connection
								}
								phandler.ConnectionTable.put(this.remotepeerId, this); // add the new socket connection
																						// for the remot peer
								msgHandler = new MessageHandler(this.peerId, this.remotepeerId, this.fmgr);
							}
						}
						if (msgHandler != null) {
							send(msgHandler.handleRequest(message));
						}

					} catch (IOException e) {
						socket.isClosed();
						state = ConnectionState.close;
					}

				}
			} catch (ClassNotFoundException classnot) {
				socket.isClosed();
				state = ConnectionState.close;
			}

		} else {
			LogConfig.getLogRecord().debugLog("Connection is closed");
			state = ConnectionState.disconnected;
			try {
				socket.close();
				state = ConnectionState.close;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void send(MessageType msg) {
		if (msg == null)
			return;
		msgQueue.add(msg);
	}

	// send a message to the output stream
	public void SendMessage(MessageType message) {
		if (message == null)
			return;
		try {
			out.writeObject(message);
			out.flush();
		} catch (IOException ioException) {
		}
	}

	public void terminate() {
		state = ConnectionState.close;
	}

}
