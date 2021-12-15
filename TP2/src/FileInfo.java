import java.io.Serializable;

public class FileInfo implements Serializable {
    private final String name;
    private final long size;
    private final long lastModified;

    public FileInfo(String name, long size, long lastModified) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getName() {
        return this.name;
    }

    public long getSize() {
        return this.size;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    public String toString() {
        return "{name: " + this.name + ", size: " + this.size + ", lastModified: " + this.lastModified + "}";
    }
}
