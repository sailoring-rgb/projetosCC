import java.io.*;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

enum PacketType {
    FILE_LIST,
    REQUEST_FILES,
    FILE_CHUNK,
    CHUNK_ACK
}

public class FTRapidPacket implements Serializable {
    private final PacketType type;//Packet type
    private final List<FileInfo> fileList;// If type = FILE_LIST
    private final List<FileInfo> requestFiles;// If type = REQUEST_FILES
    private final FileChunk fileChunk;//If type = FILE_CHUNK or CHUNK_ACK
    private final InetAddress endIP;//Peer destination IP
    private final int endPort;//Peer destination port
    private final String secret;//Shared secret

    public FTRapidPacket(PacketType type,
                         List<FileInfo> fileList,
                         List<FileInfo> requestFiles,
                         FileChunk fileChunk,
                         InetAddress endIP,
                         int endPort,
                         String secret)
    {
        this.type = type;
        this.fileList = fileList;
        this.requestFiles = requestFiles;
        this.fileChunk = fileChunk;
        this.endIP = endIP;
        this.endPort = endPort;
        this.secret = secret;
    }

    public FTRapidPacket(byte[] data) throws Exception{
        ByteArrayInputStream byteArray = new ByteArrayInputStream(data);
        ObjectInput in = null;
        Object o;
        try {
            in = new ObjectInputStream(byteArray);
            o = in.readObject();

            FTRapidPacket ftRapidPacket = (FTRapidPacket) o;

            this.secret = ftRapidPacket.getSecret();
            this.type = ftRapidPacket.getType();
            this.fileList = ftRapidPacket.getFileList();
            this.requestFiles = ftRapidPacket.getRequestFiles();
            this.fileChunk = ftRapidPacket.getFileChunk();
            this.endIP = ftRapidPacket.getEndIP();
            this.endPort = ftRapidPacket.getEndPort();
        } catch(Exception ex){
            throw new Exception("Error creating packet from bytes.");
        } finally {
            try {
                if(in != null) in.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public byte[] convertToBytes() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] data;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            data = bos.toByteArray();
        } catch (Exception ex) {
            throw new Exception("Error converting packet to bytes.");
        } finally {
            try {
                bos.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return data;
    }

    public String getSecret() {
        return this.secret;
    }

    public PacketType getType() {
        return this.type;
    }

    public List<FileInfo> getFileList() {
        return this.fileList;
    }

    public List<FileInfo> getRequestFiles() {
        return this.requestFiles;
    }

    public FileChunk getFileChunk() {
        return this.fileChunk;
    }

    public InetAddress getEndIP() {
        return this.endIP;
    }

    public int getEndPort() {
        return this.endPort;
    }

    public String toString(boolean in) {
        StringBuilder packetInfo = new StringBuilder();
        packetInfo.append("##### FTPacket #####");
        if(in) packetInfo.append(" <-\n");
        else packetInfo.append(" ->\n");
        packetInfo.append("Type: ").append(this.type).append("\n");
        switch (this.type) {
            case FILE_LIST:
                packetInfo.append("Files: ").append(Arrays.toString(this.fileList.toArray())).append("\n");
                break;
            case REQUEST_FILES:
                packetInfo.append("Files: ").append(Arrays.toString(this.requestFiles.toArray())).append("\n");
                break;
            case FILE_CHUNK:
            case CHUNK_ACK:
                packetInfo.append("Filename: ").append(this.fileChunk.getFileInfo().getName()).append("\n");
                packetInfo.append("Chunk Sequence Number: ").append(this.fileChunk.getChunkSequenceNumber()).append("\n");
                break;
        }
        packetInfo.append("####################\n");
        return packetInfo.toString();
    }
}
