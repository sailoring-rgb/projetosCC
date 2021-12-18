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
        System.out.println(ftPacket.toString(true));

        FTRapidPacket responsePacket;

        //Check packet type
        String packetType = ftPacket.getType();
        switch (packetType) {
            //Received the list of files in the other system
            case "FileList":
                //Compare the list with the current list on out folder
                List<FileInfo> filesMissing = fileManager.getFilesMissing(ftPacket.getFileList());
                //If there is no files missing, do nothing
                if (filesMissing.size() == 0) break;
                //Else ask for files
                responsePacket = new FTRapidPacket("RequestFiles", null, filesMissing, null, this.endIP, this.endPort);
                sendPacket(responsePacket);
                //Add files to the list of files being received
                fileManager.addFilesBeingReceived(filesMissing);
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
                                    //If file is already being sent, do nothing
                                    if(fileManager.isFileBeingSent(fileName)) return;

                                    //Else generate chunks and send them
                                    List<FileChunk> fileChunks = fileManager.generateFileChunks(file);

                                    //Start timer
                                    long startTime = System.nanoTime();

                                    for (FileChunk chunk : fileChunks) {
                                        FTRapidPacket responsePacket = new FTRapidPacket("FileChunk", null, null, chunk, endIP, endPort);
                                        sendPacket(responsePacket);
                                    }

                                    //Check if all acks were received, if not, resend the chunks not acknowledged
                                    Thread.sleep(50);
                                    while(!fileManager.allFileChunksAcknowledged(fileName)) {
                                        System.out.println("WAITING FOR FILE: " + fileName);
                                        List<FileChunk> notAckChunks = fileManager.getFileChunksNotAcknowledged(fileName);
                                        for(FileChunk chunk : notAckChunks) {
                                            FTRapidPacket resendPacket = new FTRapidPacket("FileChunk", null, null, chunk, endIP, endPort);
                                            sendPacket(resendPacket);
                                        }
                                        Thread.sleep(50);
                                    }

                                    //Stop timer now or when we receive the last ack
                                    long elapsedNanos = System.nanoTime() - startTime;
                                    double elapsedSeconds = elapsedNanos / 1_000_000_000.0;

                                    long fileSize = file.getSize();
                                    double throughput = fileSize / elapsedSeconds;

                                    String string = "-> File Sent\n" +
                                            "Name: " + fileName + "\n" +
                                            "Size: " + fileSize + " B\n" +
                                            "Transfer time: " + elapsedSeconds + " seconds\n" +
                                            "Throughput: " + throughput + " bits/second\n";
                                    System.out.println(string);
                                } catch (Exception e) {
                                    System.out.println("Error sending file chunk");
                                }
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            //Received a file chunk, send ack
            case "FileChunk":
                try {
                    FileChunk chunk = ftPacket.getFileChunk();
                    fileManager.addFileChunk(chunk);
                    //Create chunk without the data
                    FileChunk ackChunk = new FileChunk(null, chunk.getFileInfo(), chunk.getChunkSequenceNumber(), chunk.getNumChunks());
                    responsePacket = new FTRapidPacket("Ack", null, null, ackChunk, endIP, endPort);
                    sendPacket(responsePacket);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            //Received an ack
            case "Ack":
                fileManager.acknowledgeFileChunk(ftPacket.getFileChunk());
                break;
            //Unknown request
            default:
                System.out.println("Type Unknown");
        }
    }

    public void sendPacket(FTRapidPacket packet) {
        byte[] buffer = packet.convertToBytes();
        DatagramPacket out = new DatagramPacket(buffer, buffer.length, packet.getEndIP(), packet.getEndPort());

        try {
            //System.out.println(packet.toString(false));
            //System.out.println("Sending Packet " + packet.getType() + " via UDP on Port " + this.socket.getLocalPort() + " to Port " + this.endPort);
            int random = ThreadLocalRandom.current().nextInt(1, 11);
            if(random <= 8) socket.send(out);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
