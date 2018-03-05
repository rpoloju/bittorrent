package cn.p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.*;
/**
 * This is a client
 * Running: java -cp target/p2p-0.0.1-SNAPSHOT.jar cn.p2p.Client <IP> <port>
 */
public class Client 
{
    Socket echo_socket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
    String MESSAGE;                //capitalized message read from the server
    
    private void start(String[] args)
    {
        String host_name = args[0];
        int port = Integer.parseInt(args[1]);

        try 
        {
            echo_socket = new Socket(host_name, port);
            System.out.println("Connected to " + args[0] + ":" + args[1]);
            out = new ObjectOutputStream(echo_socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(echo_socket.getInputStream());
            
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
			{
				System.out.print("Hello, please input a sentence: ");
				//read a sentence from the standard input
				message = bufferedReader.readLine();
				//Send the sentence to the server
				sendMessage(message);
				//Receive the upperCase sentence from the server
				// string MESSAGE = (String)in.readObject();
				//show the message to the user
				// System.out.println("Receive message: " + MESSAGE);
			}
        }
        catch (ConnectException e) 
        {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch(UnknownHostException unknownHost)
        {
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException)
        {
            ioException.printStackTrace();
        }
        catch(NullPointerException e) 
        {
            System.err.println(e.getStackTrace());
        }
        finally
        {
            //Close connections
            try
            {
                in.close();
                out.close();
                echo_socket.close();
            }
            catch(IOException ioException)
            {
                ioException.printStackTrace();
            }
        }
    }

    //send a message to the output stream
	private void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
    }
    
    public static void main( String[] args )
    {
        if (args.length < 2)
        {
            System.out.println("Not enough args: java -cp target/p2p-0.0.1-SNAPSHOT.jar cn.p2p.Client <hostIP> <port>");
            return;
        }
        Client client = new Client();
        client.start(args);
        
        //System.out.println( "Hello World!" );
    }
}