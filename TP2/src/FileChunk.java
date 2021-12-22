import java.io.Serializable;

public class FileChunk implements Serializable {
    private final byte[] data;
    private final FileInfo fileInfo;
    private final int chunkSequenceNumber;
    private final int numChunks;
    private final long created;
    private boolean acknowledged;

    public FileChunk(byte[] data, FileInfo fileInfo, int chunkSequenceNumber, int numChunks, Long created) {
        this.data = data;
        this.fileInfo = fileInfo;
        this.chunkSequenceNumber = chunkSequenceNumber;
        this.numChunks = numChunks;
        this.created = created;
    }

    public byte[] getData() {
        return this.data;
    }

    public FileInfo getFileInfo() {
        return this.fileInfo;
    }

    public int getChunkSequenceNumber() {
        return this.chunkSequenceNumber;
    }

    public int getNumChunks() {
        return this.numChunks;
    }

    public long getCreated() {
        return this.created;
    }

    public void acknowledge() {
        this.acknowledged = true;
    }

    public boolean isAcknowledged() {
        return this.acknowledged;
    }
}
