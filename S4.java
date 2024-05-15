import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class S4 {
    static String serverName = "S4";    // Node name of this server, which is the same as class name
    static int serverId = 4;            // Server id number
    static int nextServerId;            // Second server id number of the three servers in a group
    static int nextNextServerId;        // Third server id number of the three servers in a group

    // Buffer received operations before they can be proceeded
    static PriorityBlockingQueue<String> ReceivedMessages = new PriorityBlockingQueue<>(50, Comparator.comparingInt((String s) -> {
        String[] parts = s.split(":");
        return Integer.parseInt(parts[0]);      // Prioritize messages based on their Lamport number at the beginning of each line
    }));
    static int startIndex1;          // First start index of key ranges that belong to this server
    static int endIndex1;            // First end index of key ranges that belong to this server

    static int startIndex2;          // Second start index of key ranges that belong to this server
    static int endIndex2;            // Second end index of key ranges that belong to this server

    static int startIndex3;          // Third start index of key ranges that belong to this server
    static int endIndex3;            // Third end index of key ranges that belong to this server

    static List<Socket> otherSockets = new ArrayList<>();       // Store all other sockets, first other servers, then all clients
    static AtomicInteger lamportTimestamp = new AtomicInteger(0);    // total ordering to decide which command enters queue first

    static Boolean[] portStatus = {false, false, false, false, true, false, false};     // Port status, to simulate disabled and enabled channels
    static private HashMap<Integer, Integer> map1 = new HashMap<>();                    // First hashmap of current server to store objects in key-value pairs
    static private HashMap<Integer, Integer> map2 = new HashMap<>();                    // Second hashmap of current server to store objects in key-value pairs
    static private HashMap<Integer, Integer> map3 = new HashMap<>();                    // Third hashmap of current server to store objects in key-value pairs
    static Integer[] groupStatus = {1,1,1,1,1,1,1};
    static int groupId = groupStatus[serverId];                 // Indicate which group this server belongs to after partition, will be needed to identify which ports to be closed

    public static void main(String[] args) {
        nextServerId = ServerHelper.nextServerHash(serverId);
        nextNextServerId = ServerHelper.nextNextServerHash(serverId);
        startIndex1 = ServerHelper.hashRange().get(serverId).get(0);
        endIndex1 = ServerHelper.hashRange().get(serverId).get(1);
        startIndex2 = ServerHelper.hashRange().get(ServerHelper.previousServerHash(serverId)).get(0);
        endIndex2 = ServerHelper.hashRange().get(ServerHelper.previousServerHash(serverId)).get(1);
        startIndex3 = ServerHelper.hashRange().get(ServerHelper.previousPreviousServerHash((serverId))).get(0);
        endIndex3 = ServerHelper.hashRange().get(ServerHelper.previousPreviousServerHash((serverId))).get(1);

        try (ServerSocket S4ReceiverSocket = new ServerSocket(2004)) {
            // Get current server address, initially assign all servers to group 1 before the servers get partitioned into two components.
            String S4Address = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Server S4 online. IP address: " + S4Address + ". Assigned to group " + groupId);
            System.out.println("Key range 1: " + startIndex1 + " ~ " + endIndex1);
            System.out.println("Key range 2: " + startIndex2 + " ~ " + endIndex2);
            System.out.println("Key range 3: " + startIndex3 + " ~ " + endIndex3);

            // Record this server's name, IP address and group id in serverAddresses.txt
            ServerHelper.updateServerAddressesTXT(serverName, S4Address);

            List<String> serverAddresses = ServerHelper.readServerAddressesTXT();
            String S0Address = serverAddresses.get(0).trim().split(": ")[1];
            String S1Address = serverAddresses.get(1).trim().split(": ")[1];
            String S2Address = serverAddresses.get(2).trim().split(": ")[1];
            String S3Address = serverAddresses.get(3).trim().split(": ")[1];

            Socket S4SenderToS0 = new Socket(S0Address, ServerHelper.portNums.get(0));
            otherSockets.add(S4SenderToS0);
            ServerHelper.ReceiverThread S0receiverThread = new ServerHelper.ReceiverThread("S4SenderToS0", lamportTimestamp, S4SenderToS0, ReceivedMessages);
            S0receiverThread.start();
            portStatus[0] = true;
            System.out.println("Established connection to server S0.");

            Socket S4SenderToS1 = new Socket(S1Address, ServerHelper.portNums.get(1));
            otherSockets.add(S4SenderToS1);
            ServerHelper.ReceiverThread S1receiverThread = new ServerHelper.ReceiverThread("S4SenderToS1", lamportTimestamp, S4SenderToS1, ReceivedMessages);
            S1receiverThread.start();
            portStatus[1] = true;
            System.out.println("Established connection to server S1.");

            Socket S4SenderToS2 = new Socket(S2Address, ServerHelper.portNums.get(2));
            otherSockets.add(S4SenderToS2);
            ServerHelper.ReceiverThread S2receiverThread = new ServerHelper.ReceiverThread("S4SenderToS2", lamportTimestamp, S4SenderToS2, ReceivedMessages);
            S2receiverThread.start();
            portStatus[2] = true;
            System.out.println("Established connection to server S2.");

            Socket S4SenderToS3 = new Socket(S3Address, ServerHelper.portNums.get(3));
            otherSockets.add(S4SenderToS3);
            ServerHelper.ReceiverThread S3receiverThread = new ServerHelper.ReceiverThread("S4SenderToS3", lamportTimestamp, S4SenderToS3, ReceivedMessages);
            S3receiverThread.start();
            portStatus[3] = true;
            System.out.println("Established connection to server S3.");

            Socket S4SenderToS5 = S4ReceiverSocket.accept();
            otherSockets.add(S4SenderToS5);
            ServerHelper.ReceiverThread S5receiverThread = new ServerHelper.ReceiverThread("S4SenderToS5", lamportTimestamp, S4SenderToS5, ReceivedMessages);
            S5receiverThread.start();
            portStatus[5] = true;
            System.out.println("Established connection to server S5.");

            Socket S4SenderToS6 = S4ReceiverSocket.accept();
            otherSockets.add(S4SenderToS6);
            ServerHelper.ReceiverThread S6receiverThread = new ServerHelper.ReceiverThread("S4SenderToS6", lamportTimestamp, S4SenderToS6, ReceivedMessages);
            S6receiverThread.start();
            portStatus[6] = true;
            System.out.println("Established connection to server S6.");

            Socket C0 = S4ReceiverSocket.accept();
            otherSockets.add(C0);
            ServerHelper.ReceiverThread C0receiverThread = new ServerHelper.ReceiverThread("FromC0", lamportTimestamp, C0, ReceivedMessages);
            C0receiverThread.start();
            System.out.println("Established connection to client C0.");

            Socket C1 = S4ReceiverSocket.accept();
            otherSockets.add(C1);
            ServerHelper.ReceiverThread C1receiverThread = new ServerHelper.ReceiverThread("FromC1", lamportTimestamp, C1, ReceivedMessages);
            C1receiverThread.start();
            System.out.println("Established connection to client C1.");

            Socket C2 = S4ReceiverSocket.accept();
            otherSockets.add(C2);
            ServerHelper.ReceiverThread C2receiverThread = new ServerHelper.ReceiverThread("FromC2", lamportTimestamp, C2, ReceivedMessages);
            C2receiverThread.start();
            System.out.println("Established connection to client C2.");

            Socket C3 = S4ReceiverSocket.accept();
            otherSockets.add(C3);
            ServerHelper.ReceiverThread C3receiverThread = new ServerHelper.ReceiverThread("FromC3", lamportTimestamp, C3, ReceivedMessages);
            C3receiverThread.start();
            System.out.println("Established connection to client C3.");

            Socket C4 = S4ReceiverSocket.accept();
            otherSockets.add(C4);
            ServerHelper.ReceiverThread C4receiverThread = new ServerHelper.ReceiverThread("FromC4", lamportTimestamp, C4, ReceivedMessages);
            C4receiverThread.start();
            System.out.println("Established connection to client C4.");

            ServerHelper.AppendThread AppendThread = new ServerHelper.AppendThread(lamportTimestamp, otherSockets, serverName, serverId,
                    ReceivedMessages, startIndex1, endIndex1, startIndex2, endIndex2, startIndex3, endIndex3, portStatus, map1, map2,map3, groupStatus);
            AppendThread.start();

//            System.out.println("Other sockets: " + otherSockets);
//            System.out.println("Ports status: " + Arrays.toString(portStatus));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}