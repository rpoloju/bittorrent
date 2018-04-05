import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;


public class DuplexServer
{   
    private ServerHandler s;
    private ArrayList<ClientHandler> c;
    
    public DuplexServer(int port) throws Exception
    {   
        s = new ServerHandler(port);
        s.start();
        c = new ArrayList<>();
    }

    public void broadcast_to_peers(String message) throws IOException
    {
        for(ClientHandler ch : c) {
            ch.write(message);
        }
    }

    public void init_socket(String host_name, int port) throws IOException
    {
        System.out.println("Creating a new client channel");
        
        ClientHandler ch = new ClientHandler();
        ch.connect(host_name, port);
        ch.start();
        c.add(ch);
    }

    /////////////////////////
    // Class to collect incoming client connections and save into clients List. 
    // Inspired by https://github.com/khanhhua/full-duplex-chat
    private class ServerHandler extends Thread 
    {
        private ServerSocketChannel serverChannel;
        ArrayList<SocketChannel> clients;
        Selector selector;

        public ServerHandler(int port) throws Exception
        {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        }

        public void run() 
        {
            clients = new ArrayList<>();

            Iterator<SelectionKey> it;
            while (serverChannel.isOpen()) {
                try {
                    if (selector.select() != 0) {
                        it = selector.selectedKeys().iterator();
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
                channelClient.configureBlocking(false);
                channelClient.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    
                System.out.println("Client is accepted");
    
                clients.add(channelClient);
            } else if (key.isReadable()) {
                SocketChannel channelClient = (SocketChannel) key.channel();
                if (!channelClient.isOpen()) {
                    System.out.println("Channel terminated by client");
                }
                ByteBuffer buffer = ByteBuffer.allocate(80);
                buffer.clear();
                channelClient.read(buffer);
                if (buffer.get(0) == 0) {
                    System.out.println("Nothing to read.");
                    channelClient.close();
    
                    clients.remove(channelClient);
                    return;
                }
    
                System.out.printf("Client says: %s\n", new String(buffer.array()));
            } else if (key.isWritable()) {

            }
        }
    }
    
    /////////////////////////
    // Transmitter and threaded receiver for a client
    public class ClientHandler extends Thread {
        SocketChannel channel;
        Selector selector;

        public void connect(String host_name, int port) throws IOException {
            InetSocketAddress address = new InetSocketAddress(host_name, port);
    
            channel = SocketChannel.open(address);
            channel.configureBlocking(false);
            selector = Selector.open();
    
            channel.register(selector, SelectionKey.OP_READ);
        }
    
        @Override
        public void run() {
            Iterator<SelectionKey> it;
            while (channel.isConnected()) {
                try {
                    if (selector.select() != 0) {
                        it = selector.selectedKeys().iterator();
    
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
                ByteBuffer buffer = ByteBuffer.allocate(80);
                buffer.clear();
                channel.read(buffer);
                if (buffer.get(0) == 0) {
                    return;
                }
    
                System.out.printf("Server says: %s \n", new String(buffer.array()));
            }
        }
    
        public void write(String input) throws IOException {
            channel.write(ByteBuffer.wrap(input.getBytes()));
        }
    }
}