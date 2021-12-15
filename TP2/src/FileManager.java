import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class FileManager {
    File folder;
    Map<String, Map<Integer,FileChunk>> filesBeingReceived;
    Map<String, Map<Integer,FileChunk>> filesBeingSent;

    public FileManager(File folder) {
        this.folder = folder;
        this.filesBeingReceived = new HashMap<>();
        this.filesBeingSent = new HashMap<>();
    }

    public List<FileInfo> getFileList() {
        List<FileInfo> fileInfoList = new ArrayList<>();
        for(File f : folder.listFiles()) {
            if(f.isFile()) {
                FileInfo fileInfo = new FileInfo(f.getName(), f.length(), f.lastModified());
                fileInfoList.add(fileInfo);
            }
        }
        return fileInfoList;
    }

    //Get list of files that are missing/differ in size, and are not already being received
    public List<FileInfo> getFilesMissing(List<FileInfo> fileList) {
        List<FileInfo> filesMissing = new ArrayList<>();
        List<FileInfo> currentFileList = this.getFileList();

        System.out.println("-> Compare file lists");
        System.out.println("My Files: " + Arrays.toString(currentFileList.toArray()));
        System.out.println("Peer Files: " + Arrays.toString(fileList.toArray()));

        for(FileInfo file : fileList) {
            boolean exist = false;
            String fileName = file.getName();
            for(FileInfo myFile: currentFileList) {
                String myFileName = myFile.getName();
                if(fileName.equals(myFileName) && file.getSize() == myFile.getSize() && file.getLastModified() == file.getLastModified()) {
                    exist = true;
                    break;
                }
            }
            if(!exist && !this.filesBeingReceived.containsKey(fileName)) filesMissing.add(file);
        }

        System.out.println("Files Missing: " + Arrays.toString(filesMissing.toArray()));
        return filesMissing;
    }

    public List<FileChunk> generateFileChunks(String filename) throws Exception {
        File file = new File(this.folder.getAbsolutePath() + "/" + filename);

        if(!file.exists() || file.isDirectory()) throw new Exception("File not found!");

        int CHUNK_SIZE = 512;
        byte[] fileByteArray = readFileToByteArray(file);
        System.out.println(fileByteArray.length);
        List<FileChunk> dataChunks = new ArrayList<>();
        int numChunks = (int) Math.ceil(fileByteArray.length / 512.0);
        int chunkSequenceNumber = 1;
        for (int i = 0; i < fileByteArray.length; i = i + CHUNK_SIZE) {
            //byte[] chunk = new byte[CHUNK_SIZE];
            int length = CHUNK_SIZE;
            if((i + CHUNK_SIZE) >= fileByteArray.length) length = fileByteArray.length - i;
            byte[] data = new byte[length];
            System.arraycopy(fileByteArray, i, data, 0, length);
            FileChunk fileChunk = new FileChunk(data, filename, chunkSequenceNumber, numChunks);
            dataChunks.add(fileChunk);
            chunkSequenceNumber++;
        }
        if(!this.filesBeingSent.containsKey(filename)) {
            Map<Integer,FileChunk> chunks = new HashMap<>();
            for(FileChunk chunk: dataChunks) {
                chunks.put(chunk.getChunkSequenceNumber(), chunk);
            }
            this.filesBeingSent.put(filename, chunks);
        }
        return dataChunks;
    }

    private static byte[] readFileToByteArray(File file) {
        FileInputStream fis;
        // Creating a byte array using the length of the
        // file.length returns long which is cast to int
        byte[] bArray = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return bArray;
    }

    public synchronized void addFileChunk(FileChunk fileChunk) throws Exception {
        String fileName = fileChunk.getFileName();
        if(!filesBeingReceived.containsKey(fileName)) {
            Map<Integer, FileChunk> chunkList = new HashMap<>();
            chunkList.put(fileChunk.getChunkSequenceNumber(), fileChunk);
            filesBeingReceived.put(fileName, chunkList);
        } else {
            //check duplicate
            filesBeingReceived.get(fileName).put(fileChunk.getChunkSequenceNumber(), fileChunk);
            System.out.println(filesBeingReceived.get(fileName).size() + " of " + fileChunk.getNumChunks());
        }
        this.createFile(fileName, fileChunk.getNumChunks());
    }

    public void acknowledgeFileChunk(FileChunk chunk) {
        String fileName = chunk.getFileName();
        if(filesBeingSent.containsKey(fileName)) {
            filesBeingSent.get(fileName).get(chunk.getChunkSequenceNumber()).acknowledge();
        }
    }

    public boolean allFileChunksAcknowledged(String fileName) {
        if(!filesBeingSent.containsKey(fileName)) return true;

        for(FileChunk chunk : filesBeingSent.get(fileName).values()) {
            if(!chunk.isAcknowledged()) return false;
        }

        return true;
    }

    public List<FileChunk> getFileChunksNotAcknowledged(String fileName) {
        List<FileChunk> chunks = new ArrayList<>();

        if(!filesBeingSent.containsKey(fileName)) return chunks;

        for(FileChunk chunk : filesBeingSent.get(fileName).values()) {
            if(!chunk.isAcknowledged()) chunks.add(chunk);
        }

        return chunks;
    }

    public synchronized void createFile(String fileName, int numChunks) throws Exception{
        if(filesBeingReceived.get(fileName).size() == numChunks) {
            System.out.println("-> FILE CREATED: " + fileName);
            File file = new File(this.folder.getAbsolutePath() + "/" + fileName);
            //if(file.exists()) throw new Exception("File already exists!");
            FileOutputStream outToFile = new FileOutputStream(file);

            SortedSet<Integer> keys = new TreeSet<>(filesBeingReceived.get(fileName).keySet());
            for (Integer sequenceNumber : keys) {
                outToFile.write(filesBeingReceived.get(fileName).get(sequenceNumber).getData());
            }
            outToFile.close();
            filesBeingReceived.remove(fileName);
        }
    }

    public static void main(String[] args) throws Exception{
        File folder = new File("folder1");
        FileManager fileManager = new FileManager(folder);
        List<FileChunk> fileChunks = fileManager.generateFileChunks("file1.txt");
        for(FileChunk chunk: fileChunks) {
            System.out.println(chunk.getData().length);
            System.out.println(chunk.getNumChunks());
        }
    }
}
