import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Classe que implementa o protocolo UDP

/*public class FTRapidProtocol implements Runnable{
    private final DatagramSocket socket;
    private final ClientHandler client;

    public FTRapidProtocol(ClientHandler client) throws IOException{
        System.out.println("Listening in UDP " + serverPort);
        
        while(true){
            try{
                // wait for UDP connection
                DatagramPacket packet = new DatagramPacket(new byte[Packet.Max_Size],Packet.Max_Size);
                socket.receive(packet);
                byte[] buffer = new byte[packet.getLength()];
                System.arraycopy(packet.getData(),0,buffer,0,packet.getLength());

                // parse datagram packet info
                InetAddress address = packet.getAddress();
                int serverPort = packet.getPort();

                // parse packet data
                // CRIAR CLASSE PACKET !!!!!!!!!!!!!!
                Packet packetData = Serializer.Deserialize_Packet(buffer);
                PacketType packetType = packet.getType();

                if(packetType == PacketType.CONNECTION){
                    System.out.println("UDP connection received");

                    // establish UDP connection with server
                    //if(...)
                }
            }
        }
    }
}


 */