import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FTRapidHandlePacket extends Thread{
    private final DatagramSocket socket;
    private final FileManager fileManager;
    private final InetAddress endIP;
    private final int endPort;
    private final FTRapidPacket ftPacket;

    public FTRapidHandlePacket(DatagramSocket socket, FileManager fileManager, InetAddress endIP, int endPort, FTRapidPacket ftPacket) {
        this.socket = socket;
        this.fileManager = fileManager;
        this.endIP = endIP;
        this.endPort = endPort;
        this.ftPacket = ftPacket;
    }

    public void run() {
        if(ftPacket == null) {
            System.out.println("Wrong Packet Format!");
            return;
        }
        String packetType = ftPacket.getType();
        ftPacket.print();

        FTRapidPacket responsePacket;
        //Check packet action type
        switch (packetType) {
            //Received the list of files in the other system
            case "FileList":
                //Compare the list with the current list on this system.
                List<FileInfo> filesMissing = fileManager.getFilesMissing(ftPacket.getFileList());
                //If there is no files missing, do nothing.
                if (filesMissing.size() == 0) break;
                //Else ask for files
                responsePacket = new FTRapidPacket("RequestFiles", null, filesMissing, null, this.endIP, this.endPort);
                sendPacket(responsePacket);
                break;
            //Received a files request
            case "RequestFiles":
                //Send requested files
                List<FileInfo> requestedFileNames = ftPacket.getRequestFiles();
                try {
                    for (FileInfo file: requestedFileNames) {
                        (new Thread() {
                            @Override
                            public void run() {
                                String fileName = file.getName();
                                try {
                                    List<FileChunk> fileChunks = fileManager.generateFileChunks(fileName);
                                    //Start timer
                                    for (FileChunk chunk : fileChunks) {
                                        FTRapidPacket responsePacket = new FTRapidPacket("FileChunk", null, null, chunk, endIP, endPort);
                                        int random = ThreadLocalRandom.current().nextInt(1, 11);
                                        if(random <= 8) sendPacket(responsePacket);
                                    }
                                    //TIMEOUT and CHECK IF RECEIVED ALL AKS, IF NOT, RESEND
                                    while(!fileManager.allFileChunksAcknowledged(fileName)) {
                                        Thread.sleep(50);
                                        System.out.println("WAITING FOR FILE: " + fileName);
                                        List<FileChunk> notAckChunks = fileManager.getFileChunksNotAcknowledged(fileName);
                                        for(FileChunk chunk : notAckChunks) {
                                            System.out.println(chunk.getChunkSequenceNumber());
                                            FTRapidPacket resendPacket = new FTRapidPacket("FileChunk", null, null, chunk, endIP, endPort);
                                            sendPacket(resendPacket);
                                        }
                                    }
                                    //Stop timer now or when we receive the last ack

                                } catch (Exception e) {
                                    System.out.println("Error sending chunk");
                                }
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            //Received a file chunk, send ACK
            case "FileChunk":
                try {
                    FileChunk chunk = ftPacket.getFileChunk();
                    fileManager.addFileChunk(chunk);
                    FileChunk responseChunk = new FileChunk(null, chunk.getFileName(), chunk.getChunkSequenceNumber(), chunk.getNumChunks());
                    responsePacket = new FTRapidPacket("Ack", null, null, responseChunk, endIP, endPort);
                    sendPacket(responsePacket);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            //Unknown request
            case "Ack":
                fileManager.acknowledgeFileChunk(ftPacket.getFileChunk());
            default:
                System.out.println("Type Unknown");
        }
    }

    public void sendPacket(FTRapidPacket packet) {
        byte[] buffer = packet.convertToBytes();
        DatagramPacket out = new DatagramPacket(buffer, buffer.length, packet.getEndIP(), packet.getEndPort());

        try {
            System.out.println("Sending Packet " + packet.getType() + " via UDP on Port " + this.socket.getLocalPort() + " to Port " + this.endPort);
            socket.send(out);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
