import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.DatatypeConverter;

import com.google.common.base.Splitter;

public class MessagePreparer 
{
    
    public MessagePreparer()
    {

    }

    public static int get_num_pieces(String path) throws IOException
    {
        SingletonCommon common_cfg = SingletonCommon.getInstance();
        
        Path p = Paths.get(path);
        byte[] raw_image = Files.readAllBytes(p);
        int pieces = (int) Math.ceil((double)raw_image.length / common_cfg.PieceSize);
        System.out.printf("Got %d pieces for path %s\n", pieces, path);

        return pieces;
    }

    public static void broadcast_file(DuplexServer listener, String path) throws IOException
    {
        // Split up file into a million pieces and spread it.
        Path p = Paths.get(path);
        byte[] raw_image = Files.readAllBytes(p);
        String sending = String.format("<CTRL,IMG,%d>", raw_image.length);
        listener.broadcast_to_peers(sending);
                
        String encoded_image = DatatypeConverter.printBase64Binary(raw_image);
        
        
        for (String substring : Splitter.fixedLength(80).split(encoded_image))
        {
            sending = String.format("<DATA,IMG,%s>", substring);
            listener.broadcast_to_peers(sending);
        }
    }

}