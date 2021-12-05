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

public class FTRapidProtocol {

    private DatagramSocket socket;
    private String serverHost;
    private int serverPort;

    public FTRapidProtocol(Datagram Socket socket, String serverHost, int serverPort){
        this.socket = socket;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public int start() throws IOException{
        // Packet pacote = new Packet((short) 0,1,0,0); ????????????????????????
        InetAddress address = this.socket.getLocalAddress();
        Host servidor = new Host(this.socket.getLocalPort(), address.getHostName());
        // pacote.setData(sI.toBytes()); ????????????????????????
        send(this.socket, this.serverHost, this.serverPort/*, pacote*/);

        // Definir um tempo máximo de espera do ACK
        this.socket.setSoTimeout(60000);
        // Packet ack = receive(s); ?????????????????????????????
        // ...
    }

    public Host listen(DatagramSocket)
/*
    public static void main(String[] args) {

        try {
            InetAddress ipServer = InetAddress.getByName(args[0]);                       // receives the IP of the server
            int port = Integer.parseInt(args[1]);                                        // recieves the port of the server
            System.out.println("Conecting to: " +ipServer.toString() +":" +port);      
            

            DatagramSocket clientSocket = new DatagramSocket();                         // creates a socket - port not define - system gives an available port

            // buffer to read from the console
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String lineFromConsole = reader.readLine();                                 // reading from the console

            while (!lineFromConsole.equalsIgnoreCase("quit")) {
                byte[] inBuffer = new byte[MTU];
                byte[] outBuffer = new byte[MTU];

                // from the console to the socket - sending a message
                outBuffer = lineFromConsole.getBytes();
                DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, ipServer, port);
                clientSocket.send(outPacket);

                // from the socket to the console - reading a message
                DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                clientSocket.receive(inPacket);
                System.out.println(new String(inPacket.getData()));

                lineFromConsole = reader.readLine();                                    // reading from console
            }
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    */
}

