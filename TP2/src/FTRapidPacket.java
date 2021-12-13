import java.io.*;
import java.net.InetAddress;
import java.util.Arrays;

public class FTRapidPacket implements Serializable {
    private String type;//Package type
    private String[] fileList;// If type = 'FileList'
    private String[] requestFiles;// If type = 'RequestFiles'
    private FileChunk fileChunk;//If type = 'FileChunk'
    private InetAddress endIP;
    private int endPort;

    public FTRapidPacket(String type,
                         String[] fileList,
                         String[] requestFiles,
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
        Object o = null;
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
        ObjectOutput out = null;
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
        System.out.println("##### FTPacket #####");
        System.out.println("Type: " + this.type);
        switch (this.type) {
            case "FileList":
                System.out.println("Files: " + Arrays.toString(this.fileList));
                break;
            case "RequestFiles":
                System.out.println("Files: " + Arrays.toString(this.requestFiles));
                break;
            case "FileChunk":
                System.out.println("Filename: " + this.fileChunk.getFileName());
                System.out.println("Chunk Sequence Number: " + this.fileChunk.getChunkSequenceNumber());
                break;
        }
        System.out.println("####################");
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getFileList() {
        return this.fileList;
    }

    public void setFileList(String[] fileList) {
        this.fileList = fileList;
    }

    public String[] getRequestFiles() {
        return this.requestFiles;
    }

    public void setRequestFiles(String[] requestFiles) {
        this.requestFiles = requestFiles;
    }

    public FileChunk getFileChunk() {
        return this.fileChunk;
    }

    public void setFileChunk(FileChunk fileChunk) {
        this.fileChunk = fileChunk;
    }

    public InetAddress getEndIP() {
        return this.endIP;
    }

    public void setEndIP(InetAddress endIP) {
        this.endIP = endIP;
    }

    public int getEndPort() {
        return this.endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }
}
