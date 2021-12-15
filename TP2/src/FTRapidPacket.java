import java.io.*;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public class FTRapidPacket implements Serializable {
    private String type;//Package type
    private List<FileInfo> fileList;// If type = 'FileList'
    private List<FileInfo> requestFiles;// If type = 'RequestFiles'
    private FileChunk fileChunk;//If type = 'FileChunk'
    private InetAddress endIP;
    private int endPort;

    public FTRapidPacket(String type,
                         List<FileInfo> fileList,
                         List<FileInfo> requestFiles,
                         FileChunk fileChunk,
                         InetAddress endIP,
                         int endPort)
    {
        this.type = type;
        this.fileList = fileList;
        this.requestFiles = requestFiles;
        this.fileChunk = fileChunk;
        this.endIP = endIP;
        this.endPort = endPort;
    }

    public FTRapidPacket(byte[] data) {
        ByteArrayInputStream byteArray = new ByteArrayInputStream(data);
        ObjectInput in = null;
        Object o;
        try {
            in = new ObjectInputStream(byteArray);
            o = in.readObject();

            FTRapidPacket ftRapidPacket = (FTRapidPacket) o;

            this.type = ftRapidPacket.getType();
            this.fileList = ftRapidPacket.getFileList();
            this.requestFiles = ftRapidPacket.getRequestFiles();
            this.fileChunk = ftRapidPacket.getFileChunk();
            this.endIP = ftRapidPacket.getEndIP();
            this.endPort = ftRapidPacket.getEndPort();
        } catch(Exception ex){
            System.out.println(ex.getClass());
            System.out.println("Bytes Error");
        } finally {
            try {
                if(in != null) in.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public byte[] convertToBytes() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] data = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            data = bos.toByteArray();
        } catch (Exception ex) {
            System.out.println("Error Converting to Bytes");
        } finally {
            try {
                bos.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return data;
    }

    public void print() {
        StringBuilder packetInfo = new StringBuilder();
        packetInfo.append("\n##### FTPacket #####\n");
        packetInfo.append("Type: ").append(this.type).append("\n");
        switch (this.type) {
            case "FileList":
                packetInfo.append("Files: ").append(Arrays.toString(this.fileList.toArray())).append("\n");
                break;
            case "RequestFiles":
                packetInfo.append("Files: ").append(Arrays.toString(this.requestFiles.toArray())).append("\n");
                break;
            case "FileChunk":
                packetInfo.append("Filename: ").append(this.fileChunk.getFileName()).append("\n");
                packetInfo.append("Chunk Sequence Number: ").append(this.fileChunk.getChunkSequenceNumber()).append("\n");
                break;
        }
        packetInfo.append("####################\n");
        System.out.println(packetInfo);
    }

    public String getType() {
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
}
