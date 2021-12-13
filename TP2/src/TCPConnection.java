import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPConnection extends Thread{

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            while (true) {
                System.out.println("Waiting for HTTP Request...");
                Socket client = serverSocket.accept();
                handleRequest(client);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void handleRequest(Socket socket) {
        System.out.println("HTTP Request Received!");
        try {
            BufferedReader bufRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String outStr = bufRead.readLine();

            //Check if the request is an HTTP GET
            System.out.println(outStr);
            if(!outStr.equals("GET / HTTP/1.1")) return;

            //Return an HTML page with the app state
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println("\r\n");
            out.println("<p> OK </p>");
            out.flush();
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
