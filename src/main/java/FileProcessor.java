import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;

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
    private String filename;

    private Logger LOGGER = LoggerFactory.getLogger(FileProcessor.class);
    
    public FileProcessor(SingletonCommon _ccfg, RemotePeerInfo _my_info) throws IOException
    {
        ccfg = _ccfg;
        my_info = _my_info;
        piece_size = ccfg.PieceSize;
        file_size = ccfg.FileSize;
        hasfile = my_info.getHasFile_or_not();
        pwd = System.getProperty("user.dir");
        filename = ccfg.FileName;

        String split_directory_path = Paths.get(pwd, "peer_" + my_info.peerId).toString();

        File folder = new File(split_directory_path);
        if (!folder.exists()) {
            folder.mkdir();
        }

        path = Paths.get(split_directory_path, filename).toString();
        num_pieces = (int) Math.ceil((double)file_size / piece_size);
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

            } else {
                // LOGGER.warn("hasfile = 1 but no file found! path=" + path);
                String from_root_path = Paths.get(pwd, filename).toString();
                File ffroot = new File(from_root_path);
                if (ffroot.exists()) {
                    copy_file(from_root_path, path);
                } else {
                    throw new IOException("hasfile = 1 but no file found! path=" + from_root_path);
                }
            }

            init_pieces(path);

        } 
    }

    private void copy_file(String from, String to) throws IOException
    {
        Path fp = Paths.get(from);
        Path tp = Paths.get(to);
        CopyOption[] options = new CopyOption[] {
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        };
        Files.copy(fp, tp, options);
    }

    public byte[] get_piece(int idx)
    {
        ByteBuffer piece_content = index_to_piece.get(idx);

        if (piece_content == null) {
            return new byte[]{};
        } else {
            return piece_content.array();
        }

    }

    public int put_piece(int idx, byte[] piece_content) throws IOException
    {
        if (index_to_piece.get(idx) != null) {
            return Constants.ALREADY_HAVE;
        } else if (piece_content.length == 0) {
            return Constants.EMPTY_PIECE_RCV;
        } else {
            index_to_piece.put(idx, ByteBuffer.wrap(piece_content));
            if (index_to_piece.size() == num_pieces) {
                // Write the file
                // LOGGER.info(String.format("TRANSFER COMPLETE! Writing to path %s", path));
                LOGGER.info("Peer [" + my_info.peerId + "] has downloaded the complete file.");
                byte[] all_pieces = cram_pieces();
                Path p = Paths.get(path);
                Files.write(p, all_pieces);
                return Constants.FILE_COMPLETE;
            } else {
                return Constants.GOOD_PIECE;
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

    public int get_progress() 
    {
        return index_to_piece.size();
    }

    // Cram together all pieces when we finish getting the file.
    private byte[] cram_pieces() 
    {
        byte[] everything = ByteBuffer.allocate(file_size).array();

        int bytes_written = 0;
        for (int idx : index_to_piece.keySet()) {
            byte[] chunk = index_to_piece.get(idx).array();
            System.arraycopy(chunk, 0, everything, bytes_written, chunk.length);
            bytes_written += chunk.length;
        }

        return everything;
    }

    private void init_pieces(String path) throws IOException 
    {
        Path p = Paths.get(path);
        byte[] raw_image = Files.readAllBytes(p);

        int idx = 0;
        int chunk_size = piece_size;
        for (int taken_bytes = 0; taken_bytes < file_size; taken_bytes += chunk_size) { 
            // Rather not copy in empty bytes, delimit actual taking on final step
            if (taken_bytes + chunk_size > file_size) {
                chunk_size = file_size - taken_bytes;
            }
            byte[] arr = Arrays.copyOfRange(raw_image, taken_bytes, taken_bytes + chunk_size);
            ByteBuffer piece = ByteBuffer.wrap(arr);
            index_to_piece.put(idx, piece);
            idx += 1;
        }
    }

}