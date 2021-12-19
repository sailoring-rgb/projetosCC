import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Monitor extends Thread{
    private final FileManager fileManager;

    public Monitor(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            System.out.println("Listening for TCP connections on port " + serverSocket.getLocalPort() + " ...");
            while (true) {
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

            //Check if the request is an HTTP GET on /
            System.out.println(outStr);
            if(!outStr.equals("GET / HTTP/1.1")) return;

            List<FileInfo> fileInfoList = fileManager.getFileList();

            //Return an HTML page with the app state
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println("\r\n");
            out.println("<div style='display: flex; flex-direction:column; align-items: center;'>");
            out.println("<h1>FT-Rapid</h1>");
            out.println("<h3>Status: <span style='color: green'>Running</span></h3>");
            out.println("<h2>Files</h2>");
            if(fileInfoList.size() == 0) {
                out.println("No files.");
            } else {
                out.println("<table>");
                out.println("<tr><th>File Name</th><th>Size(Bytes)</th><th>Last Modified</th></tr>");
                for (FileInfo fileInfo : fileInfoList) {
                    String fileInfoString = "<tr><td>" + fileInfo.getName() + "</td>" +
                            "<td>" + fileInfo.getSize() + "</td>" +
                            "<td>" + fileInfo.getLastModified() + "</td></tr>";
                    out.println(fileInfoString);
                }
                out.println("</table>");
            }
            out.println("</div>");
            out.flush();
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
