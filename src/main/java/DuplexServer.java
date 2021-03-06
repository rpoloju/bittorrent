import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import messages.MessageType;

import messages.HandShake;

/**
 * @author Washington Garcia
 * https://github.com/w-garcia
 */

public class DuplexServer extends Thread implements Runnable
{   
    private ServerHandler s;
    private MessageHandler mh;
    // private ArrayList<InetSocketAddress> clientsToInit;
    // We really just want to store clients as IP:port pairs
    private HashMap<Integer, ClientHandler> id_to_client;
    private int my_id;
    private Logger LOGGER = LoggerFactory.getLogger(DuplexServer.class);

    public DuplexServer(RemotePeerInfo my_info, int id, BitTorrentProtocol btp) throws Exception
    {   
        int port = my_info.getPeerPortNumber();
        String hostname = my_info.getPeerHostName();
        int hasfile = my_info.getHasFile_or_not();
        LOGGER.debug("CONFIG: " + hostname + ":" + port + " hasfile: " + hasfile);

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
        LOGGER.info("Peer [" + my_id + "] makes a connection to Peer [" + id + "].");
        
        InetSocketAddress address = new InetSocketAddress(host_name, port);
        // clientsToInit.add(address);
        SocketChannel channel = SocketChannel.open(address);        
        ClientHandler ch = new ClientHandler(channel, this);
        ch.start();
        id_to_client.put(id, ch);

        // This peer is initiation the connection as a client, server will need my ID.
        // String handshake = String.format("<CTRL,ID,%s>", my_id);
        HandShake hs = new HandShake(my_id);
        ch.write(ByteBuffer.wrap(hs.getPayload()));        
    }

    // Receiving incoming peer connection
    public void init_socket(SocketChannel sc) throws IOException
    {       
        // Don't log until we have the resolved ID.

        ClientHandler ch = new ClientHandler(sc, this);
        ch.start();
        // Don't know id, just add to a random one
        Random r = new Random();
        int temp = r.nextInt();
        id_to_client.put(temp, ch);

        // Peer already knew my id, but send anyway to update BtP
        HandShake hs = new HandShake(my_id);
        ch.write(ByteBuffer.wrap(hs.getPayload()));    
    }

    public void resolve_socket(String hostname, int port, int id_to_resolve)
    {
        int old_key = get_peer_id(hostname, port);

        if (old_key == -1) {
            LOGGER.debug(String.format("The requested resolution for ID=%d could not be found!", id_to_resolve));
            return;
        }

        if (old_key == id_to_resolve) {
            // Got an id we already knew.
            LOGGER.debug(String.format("The ID matched existing ID=%d and is OK", id_to_resolve));
        }
        else {
            ClientHandler get = id_to_client.remove(old_key);
            id_to_client.put(id_to_resolve, get);

            LOGGER.info("Peer [" + my_id + "] is connected from Peer [" + id_to_resolve + "].");    
        }

        // Notify BtP
        mh.peer_joined(id_to_resolve);
    }

    public void clean_socket(String hostname, int port)
    {
        int peer_gone = get_peer_id(hostname, port);

        if (peer_gone == -1) {
            LOGGER.debug(String.format("Peer with ID=%d left but was not registered!", peer_gone));
            return;
        }

        id_to_client.remove(peer_gone);
        LOGGER.debug(String.format("ClientHandler for Peer %d successfully removed.", peer_gone));

        // Notify BtP
        mh.peer_left(peer_gone);
    }

    public void broadcast_to_peers(ByteBuffer message) throws IOException
    {
        for(Integer id : id_to_client.keySet()) {
            ClientHandler ch = id_to_client.get(id);
            ch.write(message);
        }
    }

    public void send_message(ByteBuffer buffer, int peer_id) throws IOException
    {
        ClientHandler ch  = id_to_client.get(peer_id);
        if (ch != null)
        {
            ch.write(buffer);
        }
        else
        {
            LOGGER.debug("Tried to send to a non-existant peer ID.");
        }
    }

    // private void process_handshake(String hostname, int port, byte[] handshake) {
    //     int peer_id = ByteBuffer.wrap(Arrays.copyOfRange(handshake, 0, 4)).getInt();
    //     HandShake hs = new HandShake(peer_id);
    //     resolve_socket(hostname, port, peer_id);
    // }

    // Handle the stream of one peer.
    private void process_message(String hostname, int port, ByteBuffer buffer) throws IOException
    {
        int peer_id = get_peer_id(hostname, port);

        ArrayList<MessageType> result = mh.chunk_messages(buffer, peer_id);

        for (MessageType msg : result)
        {
            if (msg instanceof HandShake)
            {
                HandShake hs = (HandShake) msg;
                peer_id = msg.getpeerId();
                resolve_socket(hostname, port, peer_id);
            }
        }

        mh.handle_messages(result);
    }

    private int get_peer_id(String hostname, int port) 
    {
        int id = -1;
        for (int key : id_to_client.keySet()) {
            ClientHandler ch = id_to_client.get(key);
            if (ch.hostname == hostname && ch.port == port) {
                id = key;
                break;
            }
        }

        return id;
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
                LOGGER.debug(String.format("Got a new connection from %s:%s", hostname, port));
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
        int bytes_left;
        byte[] read_queue;

        public ClientHandler(SocketChannel sc, DuplexServer dp) throws IOException {
            parent = dp;    
            channel = sc;
            my_selector = Selector.open();
            common_cfg = SingletonCommon.getInstance();
            channel.configureBlocking(false);
            channel.register(my_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            hostname = channel.socket().getInetAddress().getHostName();
            port = channel.socket().getPort();
            // write_queue = new ArrayList<>();
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
                            
                            try {
                                handleKey(key);
                            } catch (IOException e) {
                                LOGGER.warn(e.getMessage());
                            }
    
                            it.remove();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void read_queue(SocketChannel sc) throws IOException {
            LOGGER.debug("Peer is queueing message payload.");
            
            ByteBuffer temp = ByteBuffer.allocate(bytes_left);
            int bytes_read = sc.read(temp);
            System.arraycopy(temp.array(), 0, read_queue, read_queue.length - bytes_left, bytes_read);
            bytes_left -= bytes_read;
            if (bytes_left == 0) {
                LOGGER.debug("Peer is firing the message payload.");
                parent.process_message(hostname, port, ByteBuffer.wrap(read_queue));
            }
        }

        private void test_queue(SocketChannel sc) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.clear();
            int res = sc.read(buf);
            if (res == -1) {
                // Hit EOF, send kill signal. 
                sc.close();
                parent.clean_socket(hostname, port);
                return;
            }
            byte[] header = buf.array();
            String header_maybe = new String(header);
            if (header_maybe.equalsIgnoreCase("P2PF")) {
                // Expecting handshake
                ByteBuffer body = ByteBuffer.allocate(28);
                int bytes_read = sc.read(body);
                if (bytes_read < 28) {
                    bytes_left = 28 - bytes_read;
                    read_queue = ByteBuffer.allocate(32).array();
                    System.arraycopy(header, 0, read_queue, 0, header.length);   
                    System.arraycopy(body.array(), 0, read_queue, 4, bytes_read);                 

                } else {
                    byte[] handshake = ByteBuffer.allocate(32).array();
                    System.arraycopy(header, 0, handshake, 0, header.length);
                    System.arraycopy(body.array(), 0, handshake, 4, bytes_read);
                    parent.process_message(hostname, port, ByteBuffer.wrap(handshake));
                }
            } else {
                // Got another type
                // header[0] = (byte) 0; // Undo hack
                int message_length = ByteBuffer.wrap(header).getInt();
                if (message_length == 0) 
                    return;
                    
                if (message_length > Math.pow(2, 32) || message_length < 0)
                {
                    LOGGER.error("Malformed message received: message_length=" + message_length);
                    return;
                }
                ByteBuffer body = ByteBuffer.allocate(message_length);
                int bytes_read = sc.read(body);
                if (bytes_read < message_length) {
                    bytes_left = message_length - bytes_read;
                    read_queue = ByteBuffer.allocate(message_length + 4).array();
                    System.arraycopy(header, 0, read_queue, 0, header.length);
                    System.arraycopy(body.array(), 0, read_queue, 4, bytes_read);
                } else {
                    byte[] message = ByteBuffer.allocate(message_length + 4).array();
                    System.arraycopy(header, 0, message, 0, header.length);
                    System.arraycopy(body.array(), 0, message, 4, bytes_read);
                    parent.process_message(hostname, port, ByteBuffer.wrap(message));
                }
            }
        }

        private void handleKey(SelectionKey key) throws IOException {
            if (key.isReadable()) {
                SocketChannel channelClient = (SocketChannel) key.channel();
                if (!channelClient.isOpen()) {
                    LOGGER.debug("Channel terminated by client");
                    parent.clean_socket(hostname, port);                    
                }
                // ByteBuffer buffer = ByteBuffer.allocate(common_cfg.PieceSize * 2);
                // First get the header
                if (bytes_left == 0) {
                    test_queue(channelClient);
                } else {
                    read_queue(channelClient);
                }

            } else if (key.isWritable()) {
                // SocketChannel channelClient = (SocketChannel) key.channel();
                // for (ByteBuffer bb : write_queue){
                    // LOGGER.debug(String.format("Sending %d bytes", bb.array().length));
                    
                    // channelClient.write(bb);
                // }
                // write_queue.clear();
            }
        }
    
        // public void write(String input) throws IOException {
            // channel.write(ByteBuffer.wrap(input.getBytes()));
        // }

        public void write(ByteBuffer buffer) throws IOException {
            channel.write(buffer);
            // write_queue.add(buffer);
        }
    }
}