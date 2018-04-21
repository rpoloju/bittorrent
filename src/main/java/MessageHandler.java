import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

import messages.HandShake;

// Class to turn byte blobs into something useful
public class MessageHandler 
{
    ByteArrayOutputStream temp_buffer = new ByteArrayOutputStream();
    String previous_command = "INIT";
    int size_to_expect = 0;
    private ArrayList<MessageListener> message_listeners;

    public MessageHandler() 
    {
        message_listeners = new ArrayList<>();
    }

    public void register_listener(MessageListener ml)
    {
        message_listeners.add(ml);        
    }

    public HashMap<Integer, Integer> chunk_messages(ByteBuffer buffer) 
    {
        int start = 0;
        int end = 1;        
        int chunked_bytes = 0;
        HashMap<Integer, Integer> result = new HashMap<>();
        System.out.printf("length is %d\n", buffer.array().length);    

        while (chunked_bytes < buffer.array().length)
        {
            byte[] header = Arrays.copyOfRange(buffer.array(), chunked_bytes, chunked_bytes + 4);
            String header_maybe = new String(header);
            chunked_bytes += 4;
            // System.out.printf("Header is %s\n", header_maybe);    

            if (header_maybe.equalsIgnoreCase("P2PF"))
            {
                // Got handshake
                chunked_bytes += 14;
                chunked_bytes += 10;
                int peer_id = ByteBuffer.wrap(Arrays.copyOfRange(buffer.array(), chunked_bytes, chunked_bytes + 4)).getInt();
                System.out.printf("Peerid is %d\n", peer_id);    
                chunked_bytes += 4;
                HandShake hs = new HandShake(peer_id);
                result.put(Constants.RESOLVE, peer_id);
                broadcast_handshake(hs);
            }
            else
            {
                // Got another type
                // for (byte b : header) 
                // {
                //     System.out.println((int) b);
                // }
                int message_length = ByteBuffer.wrap(header).getInt();
                if (message_length == 0) return result;
                System.out.printf("Message length is %d\n", message_length);    
                byte[] message = Arrays.copyOfRange(buffer.array(), chunked_bytes, chunked_bytes + message_length);
                int type = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 1)).getInt();

                byte[] payload = Arrays.copyOfRange(message, 1, message_length);
                System.out.printf("Got type %d\n", type);    

            }

        }

        return result;
    }

    private void handle_string(String hostname, int port, String msg)
    {

        // System.out.printf("%s:%s says: %s\n", hostname, port, msg);
        System.out.println(String.format("Current state: %s", previous_command));

        String toks[] = msg.split(",");
        String plane = toks[0];
        if (plane.equalsIgnoreCase("CTRL"))
        {
            String type = toks[1];
            if (type.equalsIgnoreCase("ID")) 
            {
                int id = Integer.parseInt(toks[2]);
                System.out.printf("%s:%s sent a CTRL message: Resolve ID %d\n", hostname, port, id);         
            }
            else if (type.equalsIgnoreCase("IMG")) 
            {
                size_to_expect = Integer.parseInt(toks[2]);
                System.out.printf("%s:%s sent a CTRL message: Expect an image of %d bytes\n", hostname, port, size_to_expect);                                         
                init_state(size_to_expect, type);
            }
            else 
            {
                System.out.printf("Got an unhandled CTRL message: %s\n", msg);
            }
        } 
        else if (plane.equalsIgnoreCase("DATA"))
        {
            String part = toks[2];
            handle_image(hostname, port, part);
        }
        else 
        {
            System.out.printf("Unknown plane received \n");
        }
    }

    private void handle_image(String hostname, int port, String part)
    {
        if (previous_command.equals("IMG")) 
        {
            byte[] decoded_part = DatatypeConverter.parseBase64Binary(part);
            try {
                temp_buffer.write(decoded_part);
            } catch (IOException e) {
                System.err.println(e.getStackTrace());                      
            }

            System.out.printf("Received %d decoded bytes so far.\n", temp_buffer.size()); 
            if (temp_buffer.size() != size_to_expect) 
            {
                return;
            }
        } 
        else 
        {
            System.out.printf("Not sure what to do with this un-delimited message.\n"); 
            return;
        }

        try {
            Path path = Paths.get("/home/wgar/p2p/image2.jpg");
            Files.write(path, temp_buffer.toByteArray());    
            System.out.printf("Wrote the file to %s\n", path.getFileName()); 
            clear_state();         
        } catch (IOException e) {
            System.err.println(e.getStackTrace());                 
        }
        
    }

    private void init_state(int size, String type)
    {
        // temp_buffer = ByteBuffer.allocate(size);
        previous_command = type;
    }

    private void clear_state() 
    {
        // temp_buffer.clear();
        size_to_expect = 0;
        previous_command = "INIT";
    }

    private void broadcast_handshake(HandShake hs)
    {
        for (MessageListener mListener : message_listeners)
        {
            mListener.onHandShake(hs);
        }
    }
}