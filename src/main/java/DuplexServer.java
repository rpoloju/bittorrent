import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import javax.xml.bind.DatatypeConverter;

import com.jcraft.jsch.Buffer;


public class DuplexServer extends Thread implements Runnable
{   
    private ServerHandler s;
    // private ArrayList<InetSocketAddress> clientsToInit;
    // We really just want to store clients as IP:port pairs
    private HashMap<Integer, ClientHandler> id_to_client;
    private int my_id;
    private String temp_buffer = "";

    public DuplexServer(int port, int id) throws Exception
    {   
        // clientsToInit = new ArrayList<>();
        s = new ServerHandler(port, this);
        s.start();
        id_to_client = new HashMap<>();
        my_id = id;
    }

    public void broadcast_to_peers(String message) throws IOException
    {
        for(Integer id : id_to_client.keySet()) {
            ClientHandler ch = id_to_client.get(id);
            ch.write(message);
        }
    }

    public void init_socket(String host_name, int port, int id) throws IOException
    {
        InetSocketAddress address = new InetSocketAddress(host_name, port);
        // clientsToInit.add(address);
        System.out.println("Adding original client");
        SocketChannel channel = SocketChannel.open(address);        
        ClientHandler ch = new ClientHandler(channel, this);
        ch.start();
        id_to_client.put(id, ch);

        // This peer is initiation the connection as a client, server will need my ID.
        String handshake = String.format("<CTRL,ID,%s>", my_id);
        ch.write(handshake);        
    }

    public void init_socket(SocketChannel sc) throws IOException
    {
        System.out.println("Adding received client");
        ClientHandler ch = new ClientHandler(sc, this);
        ch.start();
        // Don't know id, just add to a random one
        Random r = new Random();
        int temp = r.nextInt();
        id_to_client.put(temp, ch);
        // I am receiving an unknown peer so don't have to send resolution msg. 
        // String handshake = String.format("<CTRL,ID,%s>", my_id);
        // ch.write(handshake);        
    }

    private void process_message(String hostname, int port, String msg) throws IOException
    {
        System.out.printf("%s:%s says: %s\n", hostname, port, msg);

        int res = msg.indexOf("CTRL");
        // System.out.printf(Integer.toString(res));
        if (res != -1)
        {
            int start = msg.indexOf("<") + 1;
            int end = msg.indexOf(">");
            if (start == -1 || end == -1) 
            {
                System.out.printf("Got an un-delimited message. Buffering. \n");
                temp_buffer += msg;    
                return;
            } 
            else if (start == -1 && end != -1) 
            {
                // got the last message
                System.out.printf("Attempting proc of buffer \n");                
                process_message("", 0, temp_buffer);
                temp_buffer = "";
                return;
            }

            String control_msg = msg.substring(start, end);
            String toks[] = control_msg.split(",");
            String type = toks[1];
            if (type.equalsIgnoreCase("ID")) 
            {
                int id = Integer.parseInt(toks[2]);
                System.out.printf("%s:%s sent a CTRL message: Resolve ID %d\n", hostname, port, id);         
            }
            else if (type.equalsIgnoreCase("IMG"))
            {
                String img_to_hex_str = toks[2];
                byte[] data = DatatypeConverter.parseHexBinary(img_to_hex_str);
                // byte[] data = img_as_str.getBytes();
                Path path = Paths.get("/home/wgar/p2p/image2.jpg");
                Files.write(path, data);
            }
            else 
            {
                System.out.printf("Got an unhandled CTRL message: %s\n", control_msg);
            }
        }
    }

    /////////////////////////
    // Class to collect incoming clients.
    // Accepted SocketChannel is given to parent to start a transmitter.  
    // Inspired by https://github.com/khanhhua/full-duplex-chat
    private class ServerHandler extends Thread implements Runnable
    {
        private ServerSocketChannel serverChannel;
        DuplexServer parent;
        Selector my_selector;

        public ServerHandler(int port, DuplexServer dp) throws Exception
        {
            parent = dp;
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));
            my_selector = Selector.open();
            serverChannel.register(my_selector, SelectionKey.OP_ACCEPT);
        }

        public void run() 
        {
            Iterator<SelectionKey> it;
            while (serverChannel.isOpen()) {
                try {
                    if (my_selector.select() != 0) {
                        it = my_selector.selectedKeys().iterator();
                        while (it.hasNext())
                        {                          
                            SelectionKey key = it.next();
                            procKey(key);
                            it.remove();
                        }

                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void procKey(SelectionKey key) throws IOException {
            if (key.isAcceptable()) {
                SocketChannel channelClient = serverChannel.accept();
                String hostname = channelClient.socket().getInetAddress().getHostName();
                int port = channelClient.socket().getPort();
                System.out.println(String.format("Got a new connection from %s:%s", hostname, port));
                // Send to super class to initiate a client
                parent.init_socket(channelClient);
            } else if (key.isReadable()) {


            } else if (key.isWritable()) {

            }
        }

    }
    
    /////////////////////////
    // Threaded transmitter for a single client. Send any messages to parent. 
    // Will only relay messages if peer is the client in the connection. 
    public class ClientHandler extends Thread implements Runnable {
        SocketChannel channel;
        DuplexServer parent;
        Selector my_selector;
        String hostname;
        int port;
        byte[] temp_buffer;
        SingletonCommon common_cfg;

        public ClientHandler(SocketChannel sc, DuplexServer dp) throws IOException {
            parent = dp;    
            channel = sc;
            my_selector = Selector.open();
            common_cfg = SingletonCommon.getInstance();
            channel.configureBlocking(false);
            channel.register(my_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            hostname = channel.socket().getInetAddress().getHostName();
            port = channel.socket().getPort();
        }

        @Override
        public void run() {
            Iterator<SelectionKey> it;
            while (channel.isConnected()) {
                try {
                    if (my_selector.select() != 0) {
                        it = my_selector.selectedKeys().iterator();
    
                        while (it.hasNext()) {
                            SelectionKey key = it.next();
    
                            handleKey(key);
    
                            it.remove();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleKey(SelectionKey key) throws IOException {
            if (key.isReadable()) {
                SocketChannel channelClient = (SocketChannel) key.channel();
                if (!channelClient.isOpen()) {
                    System.out.println("Channel terminated by client");
                }
                ByteBuffer buffer = ByteBuffer.allocate(common_cfg.PieceSize);
                buffer.clear();
                channelClient.read(buffer);
                if (buffer.get(0) == 0) {
                    System.out.println("Nothing to read.");
                    channelClient.close();
                    return;
                }    

                parent.process_message(hostname, port, new String(buffer.array()));

            } else if (key.isWritable()) {

            }
        }
    
        public void write(String input) throws IOException {
            channel.write(ByteBuffer.wrap(input.getBytes()));
        }
    }
}