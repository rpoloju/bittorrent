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

import messages.HandShake;


public class DuplexServer extends Thread implements Runnable
{   
    private ServerHandler s;
    private MessageHandler mh;
    // private ArrayList<InetSocketAddress> clientsToInit;
    // We really just want to store clients as IP:port pairs
    private HashMap<Integer, ClientHandler> id_to_client;
    private int my_id;


    public DuplexServer(int port, int id, BitTorrentProtocol btp) throws Exception
    {   
        // clientsToInit = new ArrayList<>();
        mh = new MessageHandler();
        mh.register_listener(btp);
        s = new ServerHandler(port, this);
        s.start();
        id_to_client = new HashMap<>();
        my_id = id;
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
        // String handshake = String.format("<CTRL,ID,%s>", my_id);
        HandShake hs = new HandShake(my_id);
        ch.write(ByteBuffer.wrap(hs.getPayload()));        
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
    }

    public void resolve_socket(String hostname, int port, int id_to_resolve)
    {
        int old_key = -1;
        for (int key : id_to_client.keySet()) {
            ClientHandler ch = id_to_client.get(key);
            if (ch.hostname == hostname && ch.port == port) {
                old_key = key;
                break;
            }
        }

        if (old_key == -1) {
            System.out.printf("The requested resolution for ID=%d could not be found!\n", id_to_resolve);
            return;
        }

        ClientHandler get = id_to_client.remove(old_key);
        id_to_client.put(id_to_resolve, get);

        System.out.printf("ClientHandler successfully resolved to %d\n", id_to_resolve);
    }

    public void broadcast_to_peers(String message) throws IOException
    {
        for(Integer id : id_to_client.keySet()) {
            ClientHandler ch = id_to_client.get(id);
            ch.write(message);
        }
    }

    public void broadcast_to_peers(ByteBuffer message) throws IOException
    {
        for(Integer id : id_to_client.keySet()) {
            ClientHandler ch = id_to_client.get(id);
            ch.write(message);
        }
    }

    private void process_message(String hostname, int port, ByteBuffer buffer) throws IOException
    {
        HashMap<Integer, Integer> result = mh.chunk_messages(buffer);

        for (int key : result.keySet())
        {
            if (key == Constants.RESOLVE)
            {
                int peer_id = result.get(key);
                resolve_socket(hostname, port, peer_id);
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
                ByteBuffer buffer = ByteBuffer.allocate(common_cfg.PieceSize + 5);
                buffer.clear();
                channelClient.read(buffer);
                if (buffer.get(0) == 0) {
                    System.out.println("Nothing to read.");
                    channelClient.close();
                    return;
                }    

                // parent.process_message(hostname, port, new String(buffer.array()));
                parent.process_message(hostname, port, buffer);

            } else if (key.isWritable()) {

            }
        }
    
        public void write(String input) throws IOException {
            channel.write(ByteBuffer.wrap(input.getBytes()));
        }

        public void write(ByteBuffer buffer) throws IOException {
            String s = new String(buffer.array());
            System.out.printf("Sending: %s\n", s);

            channel.write(buffer);
        }
    }
}