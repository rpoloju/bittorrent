import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

// Class to turn byte blobs into something useful
public class MessageHandler 
{
    ByteBuffer temp_buffer;
    String previous_command = "INIT";
    int size_to_expect = 0;
    public MessageHandler() {
    }

    public ArrayList<ByteBuffer> chunk_messages(String hostname, int port, ByteBuffer buffer) 
    {
        ArrayList<ByteBuffer> chunks = new ArrayList<>();

        if (previous_command.equals("IMG"))
        {
            // We are expecting image so simply buffer
            chunks.add(buffer);
            return chunks;

        }

        int start = 0;
        int end = 1;        
        int chunked_bytes = 0;
        for (byte b : buffer.array())
        {
            if (b == (byte) '>' && buffer.array()[start] == (byte) '<')
            {                
                int length = end - start;
                chunked_bytes += length;
                byte[] chunk = Arrays.copyOfRange(buffer.array(), start, end);
                System.out.println(String.format("Got chunk : %s", new String(chunk)));
                // chunks.add();
                String msg = create_response_message(hostname, port, ByteBuffer.wrap(chunk));
                start = end;
            }   

            end += 1; 
        }

        if (chunked_bytes < buffer.array().length)
        {
            // Leftover undelimited bytes. Add to chunks
            int length = buffer.array().length - chunked_bytes;
            byte[] chunk = Arrays.copyOfRange(buffer.array(), chunked_bytes, buffer.array().length);
            System.out.println(String.format("Saw a leftover byte stream."));            
            String msg = create_response_message(hostname, port, ByteBuffer.wrap(chunk));
        }

        return chunks;
    }

    public String create_response_message(String hostname, int port, ByteBuffer buffer)
    {
        String rcv = new String(buffer.array());
        int start = rcv.indexOf("<") + 1;
        int end = rcv.indexOf(">");
        System.out.println(String.format("Current state: %s", previous_command));

        if (start != -1 && end != -1)
        {
            String control_msg = rcv.substring(start, end);
            handle_string(hostname, port, control_msg);
        }
        else if (start == -1 || end == -1) 
        {
            System.out.printf("Got an un-delimited message. Attempting to buffer\n");

            if (previous_command.equals("IMG")) 
            {
                temp_buffer = temp_buffer.put(buffer.array(), temp_buffer.position(), buffer.array().length);

                System.out.printf("Received %d bytes so far.\n", temp_buffer.array().length); 
                
                if (temp_buffer.array().length == size_to_expect) 
                {
                    handle_image(hostname, port, temp_buffer);
                    clear_state();
                }
            } 
            else 
            {
                System.out.printf("Not sure what to do with this un-delimited message.\n"); 
            }

            return "exit";
        }

        return "default";
    }

    private void handle_image(String hostname, int port, ByteBuffer buf)
    {
        try {
            Path path = Paths.get("/home/wgar/p2p/image2.jpg");
            Files.write(path, buf.array());    
            System.out.printf("Wrote the file to %s\n", path.getFileName());            
        } catch (IOException e) {
            System.err.println(e.getStackTrace());                 
        }
        
    }

    private void handle_string(String hostname, int port, String msg)
    {

        // System.out.printf("%s:%s says: %s\n", hostname, port, msg);

        int res = msg.indexOf("CTRL");
        // System.out.printf(Integer.toString(res));
        if (res != -1)
        {   
            String toks[] = msg.split(",");
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
        else
        {
            System.out.printf("Got a poorly formatted string message. \n");
        }
    }

    private void init_state(int size, String type)
    {
        temp_buffer = ByteBuffer.allocate(size_to_expect);
        previous_command = type;
    }

    private void clear_state() 
    {
        temp_buffer.clear();
        size_to_expect = 0;
        previous_command = "INIT";
    }
}