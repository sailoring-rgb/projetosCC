import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class FTRapidHandlePacket extends Thread{
    private final DatagramSocket socket;
    private final FileManager fileManager;
    private final InetAddress endIP;
    private final int endPort;
    private FTRapidPacket ftPacket;

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
                String[] filesMissing = fileManager.getFilesMissing(ftPacket.getFileList());
                //If there is no files missing, do nothing.
                if (filesMissing.length == 0) break;
                //Else ask for files
                responsePacket = new FTRapidPacket("RequestFiles", null, filesMissing, null, this.endIP, this.endPort);
                sendPacket(responsePacket);
                break;
            //Received a files request
            case "RequestFiles":
                //Send requested files
                String[] requestedFileNames = ftPacket.getRequestFiles();
                try {
                    for (String fileName: requestedFileNames) {
                        (new Thread() {
                            @Override
                            public void run() {
                                try {
                                    List<FileChunk> fileChunks = fileManager.generateFileChunks(fileName);
                                    for (FileChunk chunk : fileChunks) {
                                        FTRapidPacket responsePacket = new FTRapidPacket("FileChunk", null, null, chunk, endIP, endPort);
                                        sendPacket(responsePacket);
                                    }
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
            //Received a file chunk
            case "FileChunk":
                try {
                    fileManager.addFileChunk(ftPacket.getFileChunk());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
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
            System.out.println("Sending Packet " + packet.getType() + " via UDP on Port " + this.socket.getLocalPort() + " to Port " + this.endPort);
            socket.send(out);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
