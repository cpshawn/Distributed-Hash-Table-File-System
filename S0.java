import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class S0 {
    static String serverName = "S0";    // Node name of this server, which is the same as class name
    static int serverId = 0;            // Server id number
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

    static List<Socket> otherSockets = new ArrayList<>();     // Store all other sockets, first other servers, then all clients
    static AtomicInteger lamportTimestamp = new AtomicInteger(0);    // total ordering to decide which command enters queue first
    static Boolean[] portStatus = {true, false, false, false, false, false, false};     // Port status, to simulate disabled and enabled channels
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

        try (ServerSocket S0ReceiverSocket = new ServerSocket(2000)) {
            // Get current server address, initially assign all servers to group 1 before the servers get partitioned into two components.
            String S0Address = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Server S0 online. IP address: " + S0Address + ". Assigned to group " + groupId);
            System.out.println("Key range 1: " + startIndex1 + " ~ " + endIndex1);
            System.out.println("Key range 2: " + startIndex2 + " ~ " + endIndex2);
            System.out.println("Key range 3: " + startIndex3 + " ~ " + endIndex3);

            // Record this server's name, IP address and group id in serverAddresses.txt
            ServerHelper.updateServerAddressesTXT(serverName, S0Address);

            // Establishing connections to all other servers and clients
            Socket S0SenderToS1 = S0ReceiverSocket.accept();
            otherSockets.add(S0SenderToS1);
            ServerHelper.ReceiverThread S1receiverThread = new ServerHelper.ReceiverThread("S0SenderToS1", lamportTimestamp, S0SenderToS1, ReceivedMessages);
            S1receiverThread.start();
            portStatus[1] = true;
            System.out.println("Established connection to server S1.");

            Socket S0SenderToS2 = S0ReceiverSocket.accept();
            otherSockets.add(S0SenderToS2);
            ServerHelper.ReceiverThread S2receiverThread = new ServerHelper.ReceiverThread("S0SenderToS2", lamportTimestamp, S0SenderToS2, ReceivedMessages);
            S2receiverThread.start();
            portStatus[2] = true;
            System.out.println("Established connection to server S2.");

            Socket S0SenderToS3 = S0ReceiverSocket.accept();
            otherSockets.add(S0SenderToS3);
            ServerHelper.ReceiverThread S3receiverThread = new ServerHelper.ReceiverThread("S0SenderToS3", lamportTimestamp, S0SenderToS3, ReceivedMessages);
            S3receiverThread.start();
            portStatus[3] = true;
            System.out.println("Established connection to server S3.");

            Socket S0SenderToS4 = S0ReceiverSocket.accept();
            otherSockets.add(S0SenderToS4);
            ServerHelper.ReceiverThread S4receiverThread = new ServerHelper.ReceiverThread("S0SenderToS4", lamportTimestamp, S0SenderToS4, ReceivedMessages);
            S4receiverThread.start();
            portStatus[4] = true;
            System.out.println("Established connection to server S4.");

            Socket S0SenderToS5 = S0ReceiverSocket.accept();
            otherSockets.add(S0SenderToS5);
            ServerHelper.ReceiverThread S5receiverThread = new ServerHelper.ReceiverThread("S0SenderToS5", lamportTimestamp, S0SenderToS5, ReceivedMessages);
            S5receiverThread.start();
            portStatus[5] = true;
            System.out.println("Established connection to server S5.");

            Socket S0SenderToS6 = S0ReceiverSocket.accept();
            otherSockets.add(S0SenderToS6);
            ServerHelper.ReceiverThread S6receiverThread = new ServerHelper.ReceiverThread("S0SenderToS6", lamportTimestamp, S0SenderToS6, ReceivedMessages);
            S6receiverThread.start();
            portStatus[6] = true;
            System.out.println("Established connection to server S6.");

            Socket C0 = S0ReceiverSocket.accept();
            otherSockets.add(C0);
            ServerHelper.ReceiverThread C0receiverThread = new ServerHelper.ReceiverThread("FromC0", lamportTimestamp, C0, ReceivedMessages);
            C0receiverThread.start();
            System.out.println("Established connection to client C0.");

            Socket C1 = S0ReceiverSocket.accept();
            otherSockets.add(C1);
            ServerHelper.ReceiverThread C1receiverThread = new ServerHelper.ReceiverThread("FromC1", lamportTimestamp, C1, ReceivedMessages);
            C1receiverThread.start();
            System.out.println("Established connection to client C1.");

            Socket C2 = S0ReceiverSocket.accept();
            otherSockets.add(C2);
            ServerHelper.ReceiverThread C2receiverThread = new ServerHelper.ReceiverThread("FromC2", lamportTimestamp, C2, ReceivedMessages);
            C2receiverThread.start();
            System.out.println("Established connection to client C2.");

            Socket C3 = S0ReceiverSocket.accept();
            otherSockets.add(C3);
            ServerHelper.ReceiverThread C3receiverThread = new ServerHelper.ReceiverThread("FromC3", lamportTimestamp, C3, ReceivedMessages);
            C3receiverThread.start();
            System.out.println("Established connection to client C3.");

            Socket C4 = S0ReceiverSocket.accept();
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