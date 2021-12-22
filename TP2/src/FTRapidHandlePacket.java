import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

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
        if(!fileManager.checkSecret(ftPacket)) {
            System.out.println("SECURITY WARNING: Shared secret not matching!\n");
            return;
        }
        System.out.println(ftPacket.toString(true));

        FTRapidPacket responsePacket;

        //Check packet type
        PacketType packetType = ftPacket.getType();
        switch (packetType) {
            //Received the list of files in the other system
            case FILE_LIST:
                //Compare the list with the current list on out folder
                List<FileInfo> filesMissing = fileManager.getFilesMissing(ftPacket.getFileList());
                //If there is no files missing, do nothing
                if (filesMissing.size() == 0) break;
                //Else ask for files
                responsePacket = new FTRapidPacket(PacketType.REQUEST_FILES, null, filesMissing, null, endIP, endPort, fileManager.getSecret());
                sendPacket(responsePacket);
                //Add files to the list of files being received
                fileManager.addFilesBeingReceived(filesMissing);
                break;
            //Received a files request
            case REQUEST_FILES:
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
                                        FTRapidPacket responsePacket = new FTRapidPacket(PacketType.FILE_CHUNK, null, null, chunk, endIP, endPort, fileManager.getSecret());
                                        sendPacket(responsePacket);
                                    }

                                    //Check if all the chunks were acknowledged, if not, resend the chunks not acknowledged
                                    Thread.sleep(100);
                                    while(!fileManager.allFileChunksAcknowledged(fileName)) {
                                        System.out.println("-> Waiting for chunk acks: " + fileName + "\n");
                                        List<FileChunk> notAckChunks = fileManager.getFileChunksNotAcknowledged(fileName);
                                        for(FileChunk chunk : notAckChunks) {
                                            FTRapidPacket resendPacket = new FTRapidPacket(PacketType.FILE_CHUNK, null, null, chunk, endIP, endPort, fileManager.getSecret());
                                            sendPacket(resendPacket);
                                        }
                                        Thread.sleep(100);
                                    }

                                    //Stop timer now or when we receive the last ack
                                    long elapsedNanos = System.nanoTime() - startTime;
                                    double elapsedSeconds = elapsedNanos / 1_000_000_000.0;

                                    long fileSize = file.getSize();
                                    double throughput = (fileSize * 8) / elapsedSeconds;

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
            case FILE_CHUNK:
                try {
                    FileChunk chunk = ftPacket.getFileChunk();
                    //Save chunk
                    fileManager.addFileChunk(chunk);
                    //Create chunk without the data
                    FileChunk ackChunk = new FileChunk(null, chunk.getFileInfo(), chunk.getChunkSequenceNumber(), chunk.getNumChunks());
                    responsePacket = new FTRapidPacket(PacketType.CHUNK_ACK, null, null, ackChunk, endIP, endPort, fileManager.getSecret());
                    sendPacket(responsePacket);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            //Received an ack
            case CHUNK_ACK:
                fileManager.acknowledgeFileChunk(ftPacket.getFileChunk());
                break;
            //Unknown request
            default:
                System.out.println("Type Unknown");
        }
    }

    public void sendPacket(FTRapidPacket packet) {
        try {
            byte[] buffer = packet.convertToBytes();
            DatagramPacket out = new DatagramPacket(buffer, buffer.length, packet.getEndIP(), packet.getEndPort());

            socket.send(out);
            System.out.println(packet.toString(false));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
