import java.io.ByteArrayOutputStream;
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
    ByteArrayOutputStream temp_buffer = new ByteArrayOutputStream();
    String previous_command = "INIT";
    int size_to_expect = 0;
    public MessageHandler() {
    }

    public ArrayList<ByteBuffer> chunk_messages(String hostname, int port, ByteBuffer buffer) 
    {
        ArrayList<ByteBuffer> chunks = new ArrayList<>();

        int start = 0;
        int end = 1;        
        int chunked_bytes = 0;
        for (byte b : buffer.array())
        {
            if (b == (byte) '>' && buffer.array()[start] == (byte) '<')
            {                
                int length = end - start;
                chunked_bytes += length;
                byte[] chunk = Arrays.copyOfRange(buffer.array(), start + 1, end - 1);
                String ctrl_msg = new String(chunk);
                System.out.println(String.format("Got chunk : %s", ctrl_msg));
                handle_string(hostname, port, ctrl_msg);
                // chunks.add();
                start = end;
            }   

            end += 1; 
        }

        if (chunked_bytes < buffer.array().length)
        {
            // Leftover undelimited bytes. DROP . This is incredibly idealistic but yeah just keep message lengths short for now.
            // int length = buffer.array().length - chunked_bytes;
            // byte[] chunk = Arrays.copyOfRange(buffer.array(), chunked_bytes, buffer.array().length);
            // System.out.println(String.format("Leftover byte stream : %s", new String(chunk)));
            // handle_wierd_string(hostname, port, ByteBuffer.wrap(chunk));
            // System.out.println(String.format("Left over byte stream of length %d DROPPED.", buffer.array().length - chunked_bytes));
        }

        return chunks;
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
}