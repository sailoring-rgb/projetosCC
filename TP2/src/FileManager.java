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

    //Get list of files in the folder (name, length and lastModified)
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

    //Get list of files that are missing/different from the list and are not already being received
    public List<FileInfo> getFilesMissing(List<FileInfo> fileList) {
        List<FileInfo> filesMissing = new ArrayList<>();
        List<FileInfo> currentFileList = this.getFileList();

        //Request files we don't have or files that have a 'lastModified' date greater than ours
        for(FileInfo file : fileList) {
            boolean requestFile = true;
            String fileName = file.getName();
            for(FileInfo myFile: currentFileList) {
                String myFileName = myFile.getName();
                if(fileName.equals(myFileName) && file.getLastModified() <= myFile.getLastModified()) {
                    requestFile = false;
                    break;
                }
            }
            //Only request the files that are not already being received, or that are being received but no chunk has arrived yet
            if(requestFile && (!this.filesBeingReceived.containsKey(fileName) || filesBeingReceived.get(fileName).size() == 0)) filesMissing.add(file);
        }

        String string = "-> Compare file lists\n" +
                "My Files: " + Arrays.toString(currentFileList.toArray()) + "\n" +
                "Peer Files: " + Arrays.toString(fileList.toArray()) + "\n" +
                "Files Missing: " + Arrays.toString(filesMissing.toArray()) + "\n";
        System.out.println(string);

        return filesMissing;
    }

    //Add files to the 'filesBeingReceived' Map
    public void addFilesBeingReceived(List<FileInfo> files) {
        for(FileInfo file : files) {
            String fileName = file.getName();
            if(!this.filesBeingReceived.containsKey(fileName)){
                Map<Integer, FileChunk> chunkList = new HashMap<>();
                this.filesBeingReceived.put(fileName, chunkList);
            }
        }
    }

    //Check if file is being sent, if file is in the 'filesBeingSent' Map
    public boolean isFileBeingSent(String fileName) {
        return this.filesBeingSent.containsKey(fileName);
    }

    //Generate a list of chunks from a given file and put them in the 'filesBeingSent' Map
    public List<FileChunk> generateFileChunks(FileInfo fileInfo) throws Exception {
        String fileName = fileInfo.getName();
        File file = new File(this.folder.getAbsolutePath() + "/" + fileName);

        if(!file.exists() || file.isDirectory()) throw new Exception("File not found!");

        int CHUNK_SIZE = 512;
        byte[] fileByteArray = readFileToByteArray(file);
        List<FileChunk> dataChunks = new ArrayList<>();
        int numChunks = (int) Math.ceil(fileByteArray.length / 512.0);
        int chunkSequenceNumber = 1;
        for (int i = 0; i < fileByteArray.length; i = i + CHUNK_SIZE) {
            //byte[] chunk = new byte[CHUNK_SIZE];
            int length = CHUNK_SIZE;
            if((i + CHUNK_SIZE) >= fileByteArray.length) length = fileByteArray.length - i;
            byte[] data = new byte[length];
            System.arraycopy(fileByteArray, i, data, 0, length);
            FileChunk fileChunk = new FileChunk(data, fileInfo, chunkSequenceNumber, numChunks);
            dataChunks.add(fileChunk);
            chunkSequenceNumber++;
        }

        Map<Integer,FileChunk> chunks = new HashMap<>();
        for(FileChunk chunk: dataChunks) {
            chunks.put(chunk.getChunkSequenceNumber(), chunk);
        }
        this.filesBeingSent.put(fileName, chunks);

        String string = "-> Generated File Chunks:\n" +
                "File Name: " + fileName + "\n" +
                "Size: " + fileByteArray.length + " B\n" +
                "Num Chunks: " + numChunks + "\n";
        System.out.println(string);

        return dataChunks;
    }

    //Transforms a file in an array of bytes
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

    //Add file chunk to the 'filesBeingReceived' Map
    public synchronized void addFileChunk(FileChunk fileChunk) throws Exception {
        String fileName = fileChunk.getFileInfo().getName();
        if(!filesBeingReceived.containsKey(fileName)) return;

        filesBeingReceived.get(fileName).put(fileChunk.getChunkSequenceNumber(), fileChunk);
        System.out.println(filesBeingReceived.get(fileName).size() + " of " + fileChunk.getNumChunks());

        //If all the chunks have arrived, create/rewrite file
        if(filesBeingReceived.get(fileName).size() == fileChunk.getNumChunks()) {
            this.createFile(fileChunk.getFileInfo());
        }
    }

    //Set a file chunk as acknowledged from a file being sent
    public void acknowledgeFileChunk(FileChunk chunk) {
        String fileName = chunk.getFileInfo().getName();
        if(filesBeingSent.containsKey(fileName)) {
            filesBeingSent.get(fileName).get(chunk.getChunkSequenceNumber()).acknowledge();
        }
    }

    //Check if all the chunks from file being sent are acknowledged
    public boolean allFileChunksAcknowledged(String fileName) {
        if(!filesBeingSent.containsKey(fileName)) return true;

        for(FileChunk chunk : filesBeingSent.get(fileName).values()) {
            if(!chunk.isAcknowledged()) return false;
        }

        return true;
    }

    //Get the list of chunks from a file being sent that are not acknowledged
    public List<FileChunk> getFileChunksNotAcknowledged(String fileName) {
        List<FileChunk> chunks = new ArrayList<>();

        if(!filesBeingSent.containsKey(fileName)) return chunks;

        for(FileChunk chunk : filesBeingSent.get(fileName).values()) {
            if(!chunk.isAcknowledged()) chunks.add(chunk);
        }

        return chunks;
    }

    //Create/rewrite file received
    public synchronized void createFile(FileInfo fileInfo) throws Exception{
        String fileName = fileInfo.getName();
        File file = new File(this.folder.getAbsolutePath() + "/" + fileName);

        //If file exists, use 'overwrite' in FileOutputStream, else use 'append'
        boolean append = !file.exists();
        FileOutputStream outToFile = new FileOutputStream(file, append);

        //Order chunks by 'sequenceNumber' and write them to the file
        SortedSet<Integer> keys = new TreeSet<>(filesBeingReceived.get(fileName).keySet());
        for (Integer sequenceNumber : keys) {
            outToFile.write(filesBeingReceived.get(fileName).get(sequenceNumber).getData());
        }
        outToFile.close();
        file.setLastModified(fileInfo.getLastModified());
        filesBeingReceived.remove(fileName);

        String string = "-> File Received: \n" +
                "Name: " + fileName + "\n" +
                "Size: " + fileInfo.getSize() + " B\n";
        System.out.println(string);
    }

    public static void main(String[] args) throws Exception{
        File folder = new File("folder1");
        FileManager fileManager = new FileManager(folder);
        FileInfo fileInfo = new FileInfo("Twitter-Logo.png", 20000, 1887326);
        List<FileChunk> fileChunks = fileManager.generateFileChunks(fileInfo);
        for(FileChunk chunk: fileChunks) {
            System.out.println(chunk.getData().length + " B");
            System.out.println(chunk.getNumChunks());
        }
    }
}
