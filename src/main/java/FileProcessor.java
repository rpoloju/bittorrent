import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

import com.google.common.base.Splitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**Consume the file or offer services to BTP protocol to save the file.
 * @author Washington Garcia
 * https://github.com/w-garcia
 */
public class FileProcessor 
{
    private HashMap<Integer, ByteBuffer> index_to_piece;
    private String pwd;
    private int piece_size;
    private int file_size;
    private SingletonCommon ccfg;
    private RemotePeerInfo my_info;
    private String path;
    private int num_pieces;
    private int hasfile;

    private Logger LOGGER = LoggerFactory.getLogger(FileProcessor.class);
    
    public FileProcessor(String file_name, SingletonCommon _ccfg, RemotePeerInfo _my_info) throws IOException
    {
        ccfg = _ccfg;
        my_info = _my_info;
        piece_size = ccfg.PieceSize;
        file_size = ccfg.FileSize;
        hasfile = my_info.getHasFile_or_not();
        pwd = System.getProperty("user.dir");

        path = pwd + "/" + file_name;
        num_pieces = (int) Math.ceil((double)file_size / ccfg.PieceSize);
        LOGGER.debug("I am expecting [" + num_pieces + "] pieces to save in [" + path + "].");
        index_to_piece = new HashMap<>();

        if (hasfile != 0) {
            File f = new File(path);
            if (f.exists() && !f.isDirectory()) {                
                // normally have 306 pieces (FileSize // PieceSize)
                int real_pieces  = get_file_pieces(path);
                if (real_pieces != num_pieces) {
                    LOGGER.error("File found but tracker's file size is set wrong!");
                    throw new IOException("real_pieces != given num_pieces )");
                }
                init_pieces(path);

            } else {
                throw new IOException("hasfile = 1 but no file found! path=" + path);
            }

        } 
    }

    public int get_file_pieces(String path) throws IOException
    {
        Path p = Paths.get(path);
        byte[] raw_image = Files.readAllBytes(p);
        int pieces = (int) Math.ceil((double)raw_image.length / piece_size);
        LOGGER.debug(String.format("Got %d pieces from path %s", pieces, path));

        return pieces;
    }

    public int get_num_pieces() 
    {
        return num_pieces;
    }

    private void init_pieces(String path) throws IOException 
    {
        Path p = Paths.get(path);
        byte[] raw_image = Files.readAllBytes(p);

        for (int taken_bytes = 0; taken_bytes < file_size; taken_bytes += piece_size) {
            
        }
    }

    public static void broadcast_file(DuplexServer listener, String path) throws IOException
    {
        // Split up file into a million pieces and spread it.
        Path p = Paths.get(path);
        byte[] raw_image = Files.readAllBytes(p);
        String sending = String.format("<CTRL,IMG,%d>", raw_image.length);
        // listener.broadcast_to_peers(sending);
                
        String encoded_image = DatatypeConverter.printBase64Binary(raw_image);
        
        
        for (String substring : Splitter.fixedLength(80).split(encoded_image))
        {
            sending = String.format("<DATA,IMG,%s>", substring);
            // listener.broadcast_to_peers(sending);
        }
    }

}