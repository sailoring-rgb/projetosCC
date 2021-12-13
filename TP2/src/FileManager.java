import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class FileManager {
    File folder;
    //Map<String, FileOutputStream> filesBeingReceived;
    Map<String, Map<Integer,FileChunk>> filesBeingReceived;
    Map<String, List<FileChunk>> filesBeingSent;

    public FileManager(File folder) {
        this.folder = folder;
        this.filesBeingReceived = new HashMap<>();
        this.filesBeingSent = new HashMap<>();
    }

    public String[] getFileList() {
        return this.folder.list();
    }

    //Get list of files that are missing/differ in size, and are not already being received
    public String[] getFilesMissing(String[] fileList) {
        List<String> filesMissing = new ArrayList<>();
        String[] currentFileList = this.getFileList();

        System.out.println("-> Compare file lists");
        System.out.println("My Files: " + Arrays.toString(currentFileList));
        System.out.println("Peer Files: " + Arrays.toString(fileList));

        for(String fileName : fileList) {
            boolean exist = false;
            for(String myFile: currentFileList) {
                if(fileName.equals(myFile)) {exist = true; break;}
            }
            if(!exist && !this.filesBeingReceived.containsKey(fileName)) filesMissing.add(fileName);
        }

        String[] missing = new String[filesMissing.size()];
        System.out.println("Files Missing: " + Arrays.toString(missing));
        return filesMissing.toArray(missing);
    }

    public List<FileChunk> generateFileChunks(String filename) throws Exception {
        File file = new File(this.folder.getAbsolutePath() + "/" + filename);

        if(!file.exists() || file.isDirectory()) throw new Exception("File not found!");

        int CHUNK_SIZE = 20;
        byte[] fileByteArray = readFileToByteArray(file);
        System.out.println(fileByteArray.length);
        List<FileChunk> dataChunks = new ArrayList<>();
        int numChunks = (int) Math.ceil(fileByteArray.length / 20.0);
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
        if(!this.filesBeingSent.containsKey(filename)) this.filesBeingSent.put(filename, dataChunks);
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

    public void addFileChunk(FileChunk fileChunk) throws Exception {
        if(!filesBeingReceived.containsKey(fileChunk.getFileName())) {
            Map<Integer, FileChunk> chunkList = new HashMap<>();
            chunkList.put(fileChunk.getChunkSequenceNumber(), fileChunk);
            filesBeingReceived.put(fileChunk.getFileName(), chunkList);
        } else {
            //check duplicate
            filesBeingReceived.get(fileChunk.getFileName()).put(fileChunk.getChunkSequenceNumber(), fileChunk);
            System.out.println(filesBeingReceived.get(fileChunk.getFileName()).size() + " of " + fileChunk.getNumChunks());
            if(filesBeingReceived.get(fileChunk.getFileName()).size() == fileChunk.getNumChunks()) {
                System.out.println("FILE RECEIVED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                File file = new File(this.folder.getAbsolutePath() + "/" + fileChunk.getFileName());
                //if(file.exists()) throw new Exception("File already exists!");
                FileOutputStream outToFile = new FileOutputStream(file);

                SortedSet<Integer> keys = new TreeSet<>(filesBeingReceived.get(fileChunk.getFileName()).keySet());
                for (Integer sequenceNumber : keys) {
                    outToFile.write(filesBeingReceived.get(fileChunk.getFileName()).get(sequenceNumber).getData());
                }
                outToFile.close();
            }
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
