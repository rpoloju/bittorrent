package cn.p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.*;

/**
 * Running: java -cp target/p2p-0.0.1-SNAPSHOT.jar cn.p2p.Server <port>
 */
public class Server 
{
    private String message;    //message received from the client
    private String MESSAGE;    //uppercase message send to the client
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket

    private void start(String[] args)
    {
        int port = Integer.parseInt(args[0]);

        try 
        {
            // TODO: Put outside
            serverSocket = new ServerSocket(port);
            // Wait until a single client requesting
            // TODO: put multiple
            clientSocket = serverSocket.accept();
            System.out.println("Got a connection from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());
            // PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            // BufferedReader in = new BufferedReader(
            //     new InputStreamReader(clientSocket.getInputStream()));
            try
            {
                while(true)
                {
                    String message = (String) in.readObject();
                    System.out.println("RECEIVE: " + message);

                }
            }
            catch(ClassNotFoundException classnot)
            {
                System.err.println("Data received in unknown format");
            }
            
        }
        catch(IOException ioException)
        {
			System.out.println("Disconnect with Client");
		}
        finally
        {
			//Close connections
            try
            {
				in.close();
				out.close();
                serverSocket.close();
                clientSocket.close();
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client ");
			}
		}
    }

    public static void main( String[] args )
    {
        if (args.length < 1)
        {
            System.out.println("Not enough args: java -cp target/p2p-0.0.1-SNAPSHOT.jar cn.p2p.Client <port>");
            return;
        }
        Server server = new Server();
        server.start(args);
        
        //System.out.println( "Hello World!" );
    }
}