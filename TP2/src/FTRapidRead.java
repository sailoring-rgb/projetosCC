import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class FTRapidRead extends Thread{
    private final DatagramSocket socket;
    private final byte[] buffer = new byte[2024];
    private final FileManager fileManager;

    public FTRapidRead(DatagramSocket socket, FileManager fileManager) {
        this.socket = socket;
        this.fileManager = fileManager;
    }

    public void run() {
        System.out.println("Listening for UDP connections on port " + socket.getLocalPort() + " ...");
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(this.buffer,this.buffer.length);
                this.socket.receive(packet);

                System.out.println("UDP connection received.\n");
                FTRapidPacket ftPacket = new FTRapidPacket(packet.getData());
                FTRapidHandlePacket handlePacket = new FTRapidHandlePacket(this.socket, fileManager, packet.getAddress(), packet.getPort(), ftPacket);
                handlePacket.start();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
