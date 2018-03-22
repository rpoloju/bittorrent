import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class DuplexServer
{   
    private ServerSocketHandler s;
    private ArrayList<ClientSocketHandler> clients = new ArrayList<>(); // change this to map of peerID -> ClientSocketHandler
    
    public DuplexServer(int port) throws Exception
    {   
        s = new ServerSocketHandler(port);
        s.start();
    }

    public void broadcast_to_peers(String message)
    {
        for (ClientSocketHandler client : clients)
        {
            client.sendMessage(message);
        }
    }

    public void init_socket(String host_name, int port, int id) throws UnknownHostException, IOException
    {
        Socket s = new Socket(host_name, port);
        ClientSocketHandler h = new ClientSocketHandler(s, id);
        clients.add(h);
        h.start();
    }

    /////////////////////////
    // Class to collect incoming client connections and save into clients List. 
    private class ServerSocketHandler extends Thread 
    {
        private ServerSocket serverSocket;
        private int port;

        public ServerSocketHandler(int port) throws Exception
        {
            this.port = port;
            this.serverSocket = new ServerSocket(port);
        }

        public void run() 
        {
            try 
            {
                int count = 0;
                while (true) 
                {
                    // getting connections and saving to list
                    ClientSocketHandler h = new ClientSocketHandler(serverSocket.accept(), count);
                    clients.add(h);
                    h.start();
                    count += 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally 
            {
                try {
                    serverSocket.close();                    
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /////////////////////////
    // Transmitter and threaded receiver for clients
    private class ClientSocketHandler extends Thread {
        private Socket clientSocket;
        private int clientID;
        private ReceptionThread rc;
        
        private class ReceptionThread extends Thread
        {
            public ReceptionThread() {
                // System.out.println(this.toString());
            }
    
            public void run()
            {
                try {
                    while (true) {
                        ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());                
                        String message = (String) in.readObject();
                        System.out.println("RECEIVE: " + message + " from " + Integer.toString(clientID));
                        // deliver to class
                        // ---- 
                        // send response
                        // sendMessage(response)
    
                    }
                } catch (ClassNotFoundException classnot) {
                    System.err.println("Data received in unknown format");
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        public ClientSocketHandler(Socket connection, int no) {
            this.clientSocket = connection;
            this.clientID = no;
        }

        // send a message to the output stream
        public void sendMessage(String msg) {
            try {
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                // stream write the message
                out.writeObject(msg);
                out.flush();
            } catch (IOException ioException) {
                System.out.println(ioException.getMessage());
            }
        }

        public void run() 
        {
            try {
                // Wait until a single client requesting
                System.out.println(
                        "Got a connection from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                rc = new ReceptionThread();
                // rc.start();

                // PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                // BufferedReader in = new BufferedReader(
                // new InputStreamReader(clientSocket.getInputStream()));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } finally {
                // Close connections
                try {
                    if (clientSocket != null) { clientSocket.close(); }
                } catch (IOException ioException) {
                    System.out.println(ioException.getMessage());
                }
            }
        }
    }

}