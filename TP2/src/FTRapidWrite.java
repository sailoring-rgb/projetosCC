import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FTRapidWrite extends Thread{
    private final DatagramSocket socket;
    private final FileManager fileManager;
    private final InetAddress endIP;
    private final int endPort;

    public FTRapidWrite(DatagramSocket socket, FileManager fileManager, InetAddress endIP, int endPort) {
        this.socket = socket;
        this.fileManager = fileManager;
        this.endIP = endIP;
        this.endPort = endPort;
    }

    public void run() {
        try {
            while (true) {
                FTRapidPacket ftRapidPacket = new FTRapidPacket(PacketType.FILE_LIST, fileManager.getFileList(), null, null, endIP, endPort, fileManager.getSecret());
                byte[] buffer = ftRapidPacket.convertToBytes();
                DatagramPacket out = new DatagramPacket(buffer, buffer.length, ftRapidPacket.getEndIP(), ftRapidPacket.getEndPort());

                System.out.println(ftRapidPacket.toString(false));
                socket.send(out);

                Thread.sleep(2*1000);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
