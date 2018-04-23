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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import messages.BitField;
import messages.Choke;
import messages.HandShake;
import messages.Have;
import messages.Interested;
import messages.MessageType;
import messages.NotInterested;
import messages.Piece;
import messages.Request;
import messages.UnChoke;

/** Class to turn byte blobs into something useful
 * @author Washington Garcia
 * https://github.com/w-garcia
 */
public class MessageHandler 
{
    ByteArrayOutputStream temp_buffer = new ByteArrayOutputStream();
    String previous_command = "INIT";
    int size_to_expect = 0;
    private ArrayList<MessageListener> message_listeners;

    private Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);

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
        // System.out.printf("length is %d\n", buffer.array().length);    
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
                    LOGGER.error("Malformed message received: message_length=" + message_length);
                    return chunks;
                }

                LOGGER.debug(String.format("Message length is %d", message_length));    
                byte[] message = Arrays.copyOfRange(buffer.array(), chunked_bytes, chunked_bytes + message_length);
                // for (byte b : message) {
                //     System.out.println(new Integer((int)b));
                // }
                int type = Arrays.copyOfRange(message, 0, 1)[0];      
                chunked_bytes += message_length;
                LOGGER.debug(String.format("Got type %s", MessageType.getTypeFromMessageValue((byte)type)));    
                
                // Handle each type of message now and call broadcaster
                if (type == Constants.BITFIELD) {
                    byte[] payload = Arrays.copyOfRange(message, 1, message_length);                
                    BitSet bs = BitSet.valueOf(payload);
                    BitField bf = new BitField(given_id, bs);
                    chunks.add(bf);
                } else if (type == Constants.INTERESTED) {
                    Interested in = new Interested(given_id);
                    chunks.add(in);
                } else if (type == Constants.NOTINTERESTED) {
                    NotInterested unin = new NotInterested(given_id);
                    chunks.add(unin);
                } else if (type == Constants.HAVE) {
                    byte[] payload = Arrays.copyOfRange(message, 1, message_length);                
                    int idx = ByteBuffer.wrap(payload).getInt();
                    Have h = new Have(given_id, idx);
                    chunks.add(h);                    
                } else if (type == Constants.CHOKE) {
                    Choke c = new Choke(given_id);
                    chunks.add(c);
                } else if (type == Constants.UNCHOKE) {
                    UnChoke unc = new UnChoke(given_id);
                    chunks.add(unc);
                } else if (type == Constants.REQUEST) {
                    byte[] payload = Arrays.copyOfRange(message, 1, message_length);                    
                    int idx = ByteBuffer.wrap(payload).getInt();                    
                    Request req = new Request(given_id, idx);
                    chunks.add(req);
                } else if (type == Constants.PIECE) {
                    byte[] payload = Arrays.copyOfRange(message, 1, message_length);                    
                    byte[] piece_index = Arrays.copyOfRange(payload, 0, 4);
                    byte[] piece_data = Arrays.copyOfRange(payload, 4, payload.length);
                    Piece p = new Piece(given_id, piece_index, piece_data);
                    chunks.add(p);
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
            } else if (message instanceof NotInterested) {
                mListener.onNotInterested((NotInterested) message);
            } else if (message instanceof Interested) {
                mListener.onInterested((Interested) message);
            } else if (message instanceof Have) {
                mListener.onHave((Have) message);
            } else if (message instanceof Choke) {
                mListener.onChoke((Choke) message);
            } else if (message instanceof UnChoke) {
                mListener.onUnChoke((UnChoke) message);
            } else if (message instanceof Request) {
                mListener.onRequest((Request) message);
            }
        }

        
    }

    public void peer_joined(int peer_id)
    {
        for (MessageListener mListener : message_listeners)
        {
            mListener.onPeerJoined(peer_id);
        }
    }

    public void peer_left(int peer_id)
    {
        for (MessageListener mListener : message_listeners)
        {
            mListener.onPeerLeft(peer_id);
        }
    }
}