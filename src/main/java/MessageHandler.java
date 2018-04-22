import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

import messages.BitField;
import messages.HandShake;
import messages.MessageType;

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

    public ArrayList<MessageType> chunk_messages(ByteBuffer buffer, int some_id) 
    {
        int start = 0;
        int end = 1;        
        int chunked_bytes = 0;
        int given_id = some_id;
        System.out.printf("length is %d\n", buffer.array().length);    
        ArrayList<MessageType> chunks = new ArrayList<>();

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
                chunked_bytes += 4;
                // Set local scope's id for next messages. Append to result so duplex eventually gets it. 
                given_id = peer_id;            
                HandShake hs = new HandShake(peer_id);
                chunks.add(hs);
            }
            else
            {
                // Got another type
                header[0] = (byte) 0; // Undo hack
                int message_length = ByteBuffer.wrap(header).getInt();
                if (message_length == 0) 
                    return chunks;
                    
                if (message_length > buffer.array().length || message_length < 0)
                {
                    System.err.println("Malformed message received: message_length=" + message_length);
                    return chunks;
                }

                System.out.printf("Message length is %d\n", message_length);    
                byte[] message = Arrays.copyOfRange(buffer.array(), chunked_bytes, chunked_bytes + message_length);
                // for (byte b : message) {
                //     System.out.println(new Integer((int)b));
                // }
                int type = Arrays.copyOfRange(message, 0, 1)[0];      
                byte[] payload = Arrays.copyOfRange(message, 1, message_length - 1);
                chunked_bytes += message_length;
                System.out.printf("Got type %s\n", MessageType.getTypeFromMessageValue((byte)type));    
                
                // Handle each type of message now and call broadcaster
                if (type == Constants.BITFIELD) {
                    BitSet bs = BitSet.valueOf(payload);
                    BitField bf = new BitField(given_id, bs);
                    chunks.add(bf);
                } 
            }

        }

        return chunks;
    }

    public void handle_messages(ArrayList<MessageType> messages)
    {
        for (MessageType msg : messages) {
            broadcast_X(msg);   
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

    private void broadcast_X(MessageType message)
    {
        for (MessageListener mListener : message_listeners)
        {   
            if (message instanceof HandShake) {
                mListener.onHandShake((HandShake) message);
            } else if (message instanceof BitField) {
                mListener.onBitField((BitField) message);
            } 
        }

        
    }
}