import java.io.Serializable;

public class FileChunk implements Serializable {
    private final byte[] data;
    private final String fileName;
    private final int chunkSequenceNumber;
    private final int numChunks;

    public FileChunk(byte[] data, String fileName, int chunkSequenceNumber, int numChunks) {
        this.data = data;
        this.fileName = fileName;
        this.chunkSequenceNumber = chunkSequenceNumber;
        this.numChunks = numChunks;
    }

    public byte[] getData() {
        return this.data;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getChunkSequenceNumber() {
        return this.chunkSequenceNumber;
    }

    public int getNumChunks() {
        return this.numChunks;
    }
}
