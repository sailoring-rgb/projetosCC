import java.io.File;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FFSync {
    private static File folder;
    private static String peerIP;
    private static String secret = "secret";

    public static void main(String[] args) {
        System.out.println("Started FFSync!");

        if(!checkParams(args)) return;

        try {
            //FileManager responsible for all the file related operations
            FileManager fileManager = new FileManager(folder, secret);

            //TCP connection to handle HTTP requests
            Monitor monitor = new Monitor(fileManager);
            monitor.start();

            //UDP connections to handle FTRapid Protocol
            DatagramSocket ds = new DatagramSocket(8888);
            FTRapidRead ftRapidRead = new FTRapidRead(ds, fileManager);
            FTRapidWrite ftRapidWrite = new FTRapidWrite(ds, fileManager, InetAddress.getByName(peerIP), 8888);
            ftRapidRead.start();
            ftRapidWrite.start();

            monitor.join();
            ftRapidRead.join();
            ftRapidWrite.join();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    //Validate the params received for the correct work of the application
    public static boolean checkParams(String[] args) {
        System.out.println("-> Checking params");

        int numArgs = args.length;

        if(numArgs == 0 || args[0] == null || args[0].trim().isEmpty()) {
            System.out.println("ERROR: Folder directory is missing.");
            return false;
        }
        File f = new File(args[0]);
        if(!f.exists()) {
            try {
                f.mkdirs();
                System.out.println("Folder Created.");
            } catch (Exception e) {
                System.out.println("ERROR: Folder directory invalid.");
                return false;
            }
        } else if(!f.isDirectory()) {
            System.out.println("ERROR: Folder directory invalid.");
            return false;
        }
        folder = f;

        if(numArgs < 2 || args[1] == null || args[1].trim().isEmpty()) {
            System.out.println("ERROR: Peer IP is missing.");
            return false;
        }

        try {
            boolean isIpReachable = InetAddress.getByName(args[1]).isReachable(50);
            if(!isIpReachable) {
                System.out.println("ERROR: Peer IP is unreachable.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("ERROR: Peer IP is invalid.");
            return false;
        }
        peerIP = args[1];

        if(numArgs > 2 && args[2] != null && !args[2].trim().isEmpty()) {
            secret = args[2];
        }

        System.out.println("Folder Directory: " + folder.getAbsolutePath());
        System.out.println("Peer IP: " + peerIP);

        return true;
    }
}
