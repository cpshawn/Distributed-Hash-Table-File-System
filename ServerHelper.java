import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// Some helper functions that all servers use in common
public class ServerHelper {
    static int maxRange = 200;
    // All port number information of servers
    static List<Integer> portNums = Arrays.asList(2000, 2001, 2002, 2003, 2004, 2005, 2006);
    // Hash function to calculate the second server in one group for a client command
    static int nextServerHash(int serverID){
        return (serverID + 2) % 7;
    }

    // Hash function to calculate the third server in one group for a client command
    static int nextNextServerHash(int serverID){
        return (serverID + 4) % 7;
    }

    // Hash function to calculate the previous server in one group for a client command
    static int previousServerHash(int serverID){
        return (serverID + 5) % 7;
    }

    // Hash function to calculate the previous of previous server in one group for a client command
    static int previousPreviousServerHash(int serverID){
        return (serverID + 3) % 7;
    }
    
    static boolean fromReplicaServer(int serverID, int senderID){
        return (serverID == nextServerHash(senderID)) || (serverID == nextNextServerHash(senderID)) || (serverID == previousServerHash(senderID)) || (serverID == previousPreviousServerHash(senderID));
    }

    // Return a list of lists indicating index range for each server.
    // server Sn has range ranges[n]
    static List<List<Integer>> hashRange() {
        int unitRange = maxRange / 7;
        int remainder = maxRange % 7;
        List<List<Integer>> ranges = new ArrayList<>();

        int start = 0;
        int end = 0;
        for (int i = 0; i < 7; i++) {
            end = start + unitRange - 1;
            if (i < remainder) {
                end++; // Evenly distribute remainder to first (remainder) number of ranges
            }
            List<Integer> range = new ArrayList<>();
            range.add(start);
            range.add(end);
            ranges.add(range);
            start = end + 1;
        }
//        System.out.println("Hash ranges: " + ranges);
        return ranges;
    }
    // Read serverAddresses line by line and record each line as one server's information as one element in an array.
    static List<String> readServerAddressesTXT(){
        List<String> lines = new ArrayList<>();
        File file = new File("serverAddresses.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return lines;
    }

    // Update server IP name, address and group ID from a server
    static void updateServerAddressesTXT(String serverName, String ServerAddress) throws IOException {
        List<String> lines = new ArrayList<>();
        File file = new File("serverAddresses.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(serverName)) {
                lines.set(i, serverName + ": " + ServerAddress);
                found = true;
                break;
            }
        }

        if (!found) {
            lines.add(serverName + ": " + ServerAddress);
        }
        Collections.sort(lines);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.println(line);
            }
        }
    }


    static void forwardMessage(Socket target, String message, String targetName) throws IOException {
        BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(target.getOutputStream()));
        outputStream.write(message);
        outputStream.newLine();
        System.out.println("Forwarded message: " + message + " to " + ((targetName.charAt(0) == 'C')? "client ":"server ")  + targetName);
        outputStream.flush();
    }


    static String getLamportFromMessage(String receivedMessage){
        String[] parts = receivedMessage.split(":");
        return parts[0];
    }


    public static String hashMapToString(HashMap<Integer, Integer> map) {
        return map.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    public static HashMap<Integer, Integer> stringToHashMap(String str) {
        HashMap<Integer, Integer> map = new HashMap<>();
        String[] keyValuePairs = str.split(",");
        for (String pair : keyValuePairs) {
            String[] entry = pair.split(":");
            map.put(Integer.parseInt(entry[0]), Integer.parseInt(entry[1]));
        }
        return map;
    }

    // Receive message from port and buffer incoming commands in a priority blocking queue to avoid loss due to concurrency.
    // Commands may come from client or server, but contains the sender client's information so that the command can be returned to the correct client.
    static class ReceiverThread implements Runnable {
        private Thread t;
        String threadName;
        private Socket receiverSocket;
        private PriorityBlockingQueue<String> queue;
        private AtomicInteger lamportTimestamp;
        private BufferedReader inBuffer = null;

        public ReceiverThread(String threadName, AtomicInteger lamportTimestamp, Socket receiverSocket, PriorityBlockingQueue<String> queue) {
            this.threadName = threadName;
            this.lamportTimestamp = lamportTimestamp;
            this.receiverSocket = receiverSocket;
            this.queue = queue;
        }

        public void run() {
            String message;
            try {
                InputStream inputFromClient = receiverSocket.getInputStream();
                InputStreamReader inputReader = new InputStreamReader(inputFromClient);
                inBuffer = new BufferedReader(inputReader);
//                do {
                while (true) {
                    message = inBuffer.readLine();
                    int messageLamport = Integer.parseInt(getLamportFromMessage(message));
                    lamportTimestamp.set(Math.max(lamportTimestamp.get(), messageLamport) + 1);
                    System.out.println("Received message: " + message);
                    String messageWithUpdatedLamport = lamportTimestamp.get() + ":" + message.substring(message.indexOf(":") + 1);

                    queue.add(messageWithUpdatedLamport);
                    System.out.println("Buffered message: " + messageWithUpdatedLamport);
                    System.out.println("Buffer status:" + queue);
                }

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }finally {
                System.out.println("Thread "+ threadName + " closed");
            }
//                }while(!(message.split(":")[2].equals("close")));

        }

        public void start() {
            System.out.println("Starting message receiver " + threadName);
            if (t == null) {
                t = new Thread(this);
                t.start();
            }
        }
    }



    // Continuously read first command from the buffer and take corresponding action indicated in the command
    static class AppendThread implements Runnable{
        private Thread t;
        String threadName;
        private List<Socket> receiverSockets;
        private PriorityBlockingQueue<String> queue;
        int startIndex1;
        int endIndex1;
        HashMap<Integer, Integer> map1;
        int startIndex2;
        int endIndex2;
        HashMap<Integer, Integer> map2;
        int startIndex3;
        int endIndex3;
        HashMap<Integer, Integer> map3;
        Boolean[] portStatus;
        String serverName;
        int serverID;
        private AtomicInteger lamportTimestamp;
        Integer[] groupStatus;
        HashMap<Integer, Integer> onHoldMap1FirstCopy = new HashMap<>();
        HashMap<Integer, Integer> onHoldMap1SecondCopy = new HashMap<>();
        HashMap<Integer, Integer> onHoldMap2FirstCopy = new HashMap<>();
        HashMap<Integer, Integer> onHoldMap2SecondCopy = new HashMap<>();
        HashMap<Integer, Integer> onHoldMap3FirstCopy = new HashMap<>();
        HashMap<Integer, Integer> onHoldMap3SecondCopy = new HashMap<>();

        public AppendThread(AtomicInteger lamportTimestamp, List<Socket> receiverSockets, String serverName, int serverID, PriorityBlockingQueue<String> queue,
                            int startIndex1, int endIndex1, int startIndex2, int endIndex2, int startIndex3, int endIndex3, Boolean[] portStatus,
                            HashMap<Integer, Integer> map1, HashMap<Integer, Integer> map2, HashMap<Integer, Integer> map3, Integer[] groupStatus){
            this.lamportTimestamp = lamportTimestamp;
            this.serverName = serverName;
            this.serverID = serverID;
            this.receiverSockets = receiverSockets;
            this.queue = queue;
            this.startIndex1 = startIndex1;
            this.endIndex1 = endIndex1;
            this.startIndex2 = startIndex2;
            this.endIndex2 = endIndex2;
            this.startIndex3 = startIndex3;
            this.endIndex3 = endIndex3;
            this.portStatus = portStatus;
            this.threadName = "AppendThread";
            this.map1 = map1;
            this.map2 = map2;
            this.map3 = map3;
            this.groupStatus = groupStatus;
        }

        @Override
        public void run() {
            try{
                while (true){
                    String message = queue.poll();
                    if (message != null){
                        System.out.println("Local Lamport time: " + lamportTimestamp + ", Handling command: " + message);
                        String[] parts = message.split(":", 3);
                        int key;
                        int value;
                        String sender = parts[1];
                        int senderID = Character.getNumericValue(sender.charAt(1));
                        String action = parts[2].split(":")[0];
                        // message = lamport:sender:action(put):key:value
                        // parts = [lamport, sender, action(put):key:value]
                        switch (action) {
                            case "put":
                                String[] keyValToBeUpdated = parts[2].split(":");
                                // keyValToBeUpdated = [action, key, value]
                                key = Integer.parseInt(keyValToBeUpdated[1]);
                                value = Integer.parseInt(keyValToBeUpdated[2]);

                                // If this message is from a server in replica, do not forward it
                                boolean fromReplicaServer = ((sender.charAt(0) == 'S') && fromReplicaServer(serverID, senderID));
                                if (startIndex1 <= key & endIndex1 >= key) {           // Key in the first map
                                    if (!fromReplicaServer) {
                                        // From client, find the next two hash nodes and forward
                                        int nextServerId = nextServerHash(serverID);
                                        int nextNextServerId = nextNextServerHash(serverID);

                                        // Check if at least one of them is in the same group
                                        List<Integer> openedPortsID = new ArrayList<>();
                                        if (portStatus[serverID] == portStatus[nextServerId]) {
                                            openedPortsID.add(nextServerId);
                                        }
                                        if (portStatus[serverID] == portStatus[nextNextServerId]) {
                                            openedPortsID.add(nextNextServerId);
                                        }

                                        if (openedPortsID.size() == 0) {
                                            // Return fail message to client sender
                                            String errorMessage = lamportTimestamp.incrementAndGet() + ":Error: failed to update[" + key + "]: " + value + ", cannot perform operation on enough servers";
                                            int socketIndex = Character.getNumericValue(sender.charAt(1)) + 6;      // receiverSockets list contains 6 servers and then the 5 clients. For example, client C3's index in the socket list is 10
                                            System.out.println("Local Lamport time: " + lamportTimestamp + "Error: failed to update[\" + key + \"]: \" + value + \", cannot perform operation on enough servers. Returned error message to sender " + sender);
                                            forwardMessage(receiverSockets.get(socketIndex), errorMessage, sender);
                                        } else {
                                            // Forward this message with an updated lamport number to the connected servers, then update map
                                            for (Integer id : openedPortsID) {
                                                String updatedMessage = lamportTimestamp.incrementAndGet() + ":" + serverName + ":"+ message.split(":", 3)[2];
                                                String targetName = "S" + id;
                                                forwardMessage(receiverSockets.get(id), updatedMessage, targetName);
                                                System.out.println("Local Lamport time: " + lamportTimestamp + ", Sent message " + message + " to server " + targetName);
                                            }
                                            map1.put(key, value);
                                            System.out.println("Local Lamport time: " + lamportTimestamp + ",Updated map: ");
                                            System.out.println("Map1: " + map1);
                                        }
                                    }
                                    else {
                                        map1.put(key, value);
                                        System.out.println("Local Lamport time: " + lamportTimestamp + ",Updated map: ");
                                        System.out.println("Map1: " + map1);
                                    }

                                } else if (startIndex2 <= key & endIndex2 >= key) {            // Key in the second map
                                    if (!fromReplicaServer) {
                                        // From client, find previous and next hash nodes and forward first
                                        int previousServerId = previousServerHash(serverID);
                                        int nextServerId = nextServerHash(serverID);

                                        // Check if at least one of them is in the same group
                                        List<Integer> openedPortsID = new ArrayList<>();
                                        if (portStatus[serverID] == portStatus[previousServerId]) {
                                            openedPortsID.add(previousServerId);
                                        }
                                        if (portStatus[serverID] == portStatus[nextServerId]) {
                                            openedPortsID.add(nextServerId);
                                        }

                                        if (openedPortsID.size() == 0) {
                                            // Return fail message to client sender
                                            String errorMessage = lamportTimestamp.incrementAndGet() + ":Error: failed to update[" + key + "]: " + value + ", cannot perform operation on enough servers";
                                            int socketIndex = Character.getNumericValue(sender.charAt(1)) + 6;      // receiverSockets list contains 6 servers and then the 5 clients. For example, client C3's index in the socket list is 10
                                            System.out.println("Local Lamport time: " + lamportTimestamp + "Error: failed to update[\" + key + \"]: \" + value + \", cannot perform operation on enough servers. Returned error message to sender " + sender);
                                            forwardMessage(receiverSockets.get(socketIndex), errorMessage, sender);
                                        } else {
                                            // Forward this message with an updated lamport number to the connected servers, then update map
                                            for (Integer id : openedPortsID) {
                                                String updatedMessage = lamportTimestamp.incrementAndGet() + ":" + serverName + ":"+ message.split(":", 3)[2];
                                                String targetName = "S" + id;
                                                forwardMessage(receiverSockets.get(id), updatedMessage, targetName);
                                                System.out.println("Local Lamport time: " + lamportTimestamp + ", Sent message " + message + " to server " + targetName);
                                            }
                                            map2.put(key, value);
                                            System.out.println("Local Lamport time: " + lamportTimestamp + ",Updated map: ");
                                            System.out.println("Map2: " + map2);
                                        }
                                    }
                                    else {
                                        map2.put(key, value);
                                        System.out.println("Local Lamport time: " + lamportTimestamp + ",Updated map: ");
                                        System.out.println("Map2: " + map2);
                                    }

                                } else if (startIndex3 <= key & endIndex3 >= key) {            // Key in the third map
                                    if (!fromReplicaServer) {
                                        // From client, find previous two hash nodes and forward
                                        int previousServerId = previousServerHash(serverID);
                                        int previousPreviousServerId = previousPreviousServerHash(serverID);

                                        // Check if at least one of them is in the same group
                                        List<Integer> openedPortsID = new ArrayList<>();
                                        if (portStatus[serverID] == portStatus[previousPreviousServerId]) {
                                            openedPortsID.add(previousPreviousServerId);
                                        }
                                        if (portStatus[serverID] == portStatus[previousServerId]) {
                                            openedPortsID.add(previousServerId);
                                        }

                                        if (openedPortsID.size() == 0) {
                                            // Return fail message to client sender
                                            String errorMessage = lamportTimestamp.incrementAndGet() + ":Error: failed to update[" + key + "]: " + value + ", cannot perform operation on enough servers";
                                            int socketIndex = Character.getNumericValue(sender.charAt(1)) + 6;      // receiverSockets list contains 6 servers and then the 5 clients. For example, client C3's index in the socket list is 10
                                            System.out.println("Local Lamport time: " + lamportTimestamp + " Error: failed to update[\" + key + \"]: \" + value + \", cannot perform operation on enough servers. Returned error message to sender " + sender);
                                            forwardMessage(receiverSockets.get(socketIndex), errorMessage, sender);
                                        } else {
                                            // Forward this message with an updated lamport number to the connected servers, then update map
                                            for (Integer id : openedPortsID) {
                                                String updatedMessage = lamportTimestamp.incrementAndGet() + ":" + serverName + ":"+ message.split(":", 3)[2];
                                                String targetName = "S" + id;
                                                forwardMessage(receiverSockets.get(id), updatedMessage, targetName);
                                                System.out.println("Local Lamport time: " + lamportTimestamp + ", Sent message " + message + " to server " + targetName);
                                            }
                                            map3.put(key, value);
                                            System.out.println("Local Lamport time: " + lamportTimestamp + ",Updated maps: ");
                                            System.out.println("Map3: " + map3);
                                        }
                                    }
                                    else{
                                        map3.put(key, value);
                                        System.out.println("Local Lamport time: " + lamportTimestamp + ",Updated maps: ");
                                        System.out.println("Map3: " + map3);
                                    }
                                } else {
                                    // Key is not in any of three maps, message is from client
                                    // Find the three correct indexes of range the key belongs to
                                    List<List<Integer>> ranges = hashRange();
                                    int actualRangeIndex = 0;
                                    for (List<Integer> range : ranges) {
                                        int startIndex = range.get(0);
                                        int endIndex = range.get(1);
                                        if (startIndex <= key && key <= endIndex) {
                                            // Key is in this range
                                            break;
                                        }
                                        actualRangeIndex += 1;
                                    }
                                    int nextActualRangeIndex = nextServerHash(actualRangeIndex);
                                    int nextNextActualRangeIndex = nextNextServerHash(actualRangeIndex);

                                    // Check if at least two of them is in the same group
                                    List<Integer> openedPortsID = new ArrayList<>();
                                    if ((portStatus[serverID] == portStatus[actualRangeIndex]) & (serverID != actualRangeIndex)) {
                                        openedPortsID.add(actualRangeIndex);
                                    }
                                    if ((portStatus[serverID] == portStatus[nextActualRangeIndex]) & (serverID != nextActualRangeIndex)) {
                                        openedPortsID.add(nextActualRangeIndex);
                                    }
                                    if ((portStatus[serverID] == portStatus[nextNextActualRangeIndex]) & (serverID != nextNextActualRangeIndex)) {
                                        openedPortsID.add(nextNextActualRangeIndex);
                                    }

                                    if (openedPortsID.size() < 2) {
                                        // Return fail message to client sender
                                        String errorMessage = lamportTimestamp.incrementAndGet() + ":Error: failed to update[" + key + "]: " + value + ", cannot perform operation on enough servers";
                                        int socketIndex = Character.getNumericValue(sender.charAt(1)) + 6;      // receiverSockets list contains 6 servers and then the 5 clients. For example, client C3's index in the socket list is 10
                                        System.out.println("Local Lamport time: " + lamportTimestamp + "Error: failed to update[" + key + "]: " + value + ", cannot perform operation on enough servers. Returned error message to sender " + sender);
                                        forwardMessage(receiverSockets.get(socketIndex), errorMessage, sender);
                                    } else {
                                        // Forward this message with an updated lamport number to the connected servers
                                        for (Integer id : openedPortsID) {
                                            String updatedMessage = lamportTimestamp.incrementAndGet() + ":" + serverName + ":"+ message.split(":", 3)[2];
                                            String targetName = "S" + id;
                                            forwardMessage(receiverSockets.get(id), updatedMessage, targetName);
                                            System.out.println("Local Lamport time: " + lamportTimestamp + ", Sent message " + message + " to server " + targetName);
                                        }
                                    }
                                }
                                break;

                            case "get":
                                key = Integer.parseInt(parts[2].split(":")[1]);
                                boolean isKeyInMap1 = map1.containsKey(key);
                                boolean isKeyInMap2 = map2.containsKey(key);
                                boolean isKeyInMap3 = map3.containsKey(key);
                                String returnMessage;
                                lamportTimestamp.incrementAndGet();
                                if (isKeyInMap1 || isKeyInMap2 || isKeyInMap3) {
                                    // The key is in one of the maps
                                    if (map1.containsKey(key)) {
                                        value = map1.get(key);
                                    }
                                    else if (map2.containsKey(key)) {
                                        value = map2.get(key);
                                    }
                                    else{
                                        value = map3.get(key);
                                    }
                                    returnMessage = lamportTimestamp + ":From server " + serverName + ": [get](key = " + key + ") = " + value;
                                } else {
                                    // The key is not in any of the maps
                                    returnMessage = lamportTimestamp + ":Error: does not find value of [get](key = " + key + ") ";
                                }
                                forwardMessage(receiverSockets.get(senderID + 6), returnMessage, sender);

                                break;

                            case "merge":
                                groupStatus = new Integer[]{1, 1, 1, 1, 1, 1, 1};
                                portStatus = new Boolean[]{true, true, true, true, true, true, true};

                                // MAP: Hashmap of hashmaps: key(startIndex)-value(map)

                                // map1, going to next hash and next next hash:
                                String map1String = serverName+":MAP:"+ startIndex1 + ":" + hashMapToString(map1);
                                int map1TargetID1 = nextServerHash(serverID);
                                int map1TargetID2 = nextNextServerHash(serverID);
                                String map1StringWithLamport;

                                Socket map1TargetID1Socket = receiverSockets.get(map1TargetID1);
                                // Generated messages to be send are in the form: LamportNumber:sender:MAP:key:(map values)
                                map1StringWithLamport = lamportTimestamp.incrementAndGet() + ":" + map1String;
                                forwardMessage(map1TargetID1Socket, map1StringWithLamport, "S" + map1TargetID1);

                                Socket map1TargetID2Socket = receiverSockets.get(map1TargetID2);
                                map1StringWithLamport = lamportTimestamp.incrementAndGet() + ":" + map1String;
                                forwardMessage(map1TargetID2Socket, map1StringWithLamport, "S" + map1TargetID2);


                                // map2, going to previous hash and next hash:
                                String map2String = serverName+":MAP:"+ startIndex2 + ":"+ hashMapToString(map2);
                                int map2TargetID1 = previousServerHash(serverID);
                                int map2TargetID2 = nextServerHash(serverID);
                                String map2StringWithLamport;

                                Socket map2TargetID1Socket = receiverSockets.get(map2TargetID1);
                                map2StringWithLamport = lamportTimestamp.incrementAndGet() + ":" + map2String;
                                forwardMessage(map2TargetID1Socket, map2StringWithLamport, "S" + map2TargetID1);

                                Socket map2TargetID2Socket = receiverSockets.get(map2TargetID2);
                                map2StringWithLamport = lamportTimestamp.incrementAndGet() + ":" + map2String;
                                forwardMessage(map2TargetID2Socket, map2StringWithLamport, "S" + map2TargetID2);


                                // map3, going to previous hash and previous previous hash:
                                String map3String = serverName+":MAP:"+ startIndex3 + ":" + hashMapToString(map3);
                                int map3TargetID1 = previousServerHash(serverID);
                                int map3TargetID2 = previousPreviousServerHash(serverID);
                                String map3StringWithLamport;

                                Socket map3TargetID1Socket = receiverSockets.get(map3TargetID1);
                                map3StringWithLamport = lamportTimestamp.incrementAndGet() + ":" + map3String;
                                forwardMessage(map3TargetID1Socket, map3StringWithLamport, "S" + map3TargetID1);

                                Socket map3TargetID2Socket = receiverSockets.get(map3TargetID2);
                                map3StringWithLamport = lamportTimestamp.incrementAndGet() + ":" + map3String;
                                forwardMessage(map3TargetID2Socket, map3StringWithLamport, "S" + map3TargetID2);

                                break;

                            case "separate":
                                String groupStat = parts[2].split(":")[1];
                                String[] groupStatParts = groupStat.split(" ");

                                // Update the grouping information from input
                                for (int i = 0; i < groupStatParts.length; i++) {
                                    groupStatus[i] = Integer.parseInt(groupStatParts[i]);
                                }
                                // Use new groupStatus to update portStatus
                                int currGroupId = groupStatus[serverID];
                                for (int i = 0; i < portStatus.length; i++){
                                    if (groupStatus[i] != currGroupId){
                                        portStatus[i] = false;
                                    }
                                    else{
                                        portStatus[i] = true;
                                    }
                                }
                                System.out.println("Local Lamport time: " + lamportTimestamp + ",Received grouping info: " + groupStat);
                                System.out.println("Local Lamport time: " + lamportTimestamp + ",Updated grouping info: " + Arrays.toString(groupStatus));
                                System.out.println("Local Lamport time: " + lamportTimestamp + ",Updated port status: " + Arrays.toString(portStatus));
                                break;

                            // Received a piece of map, wait until two of the same key received, then use them to update that part of map.
                            case "MAP":
                                // Message format:
                                // Lamport:sender:MAP:key:(map values)
                                // map values----key1:value1,key2:value2,...
                                // Convert 3 maps into 3 hashmaps with startindex as key

                                int incomingMapKey = Integer.parseInt(parts[2].split(":", 3)[1]);
                                String incomingMapVal = parts[2].split(":", 4)[2];
                                // Belongs to map1
                                if (incomingMapKey >= startIndex1 & incomingMapKey <= endIndex1){
                                    if (onHoldMap1FirstCopy.isEmpty()){
                                        onHoldMap1FirstCopy = stringToHashMap(incomingMapVal);
                                    }
                                    else{
                                        onHoldMap1SecondCopy = stringToHashMap(incomingMapVal);
                                    }
                                }
                                // Belongs to map2
                                else if (incomingMapKey >= startIndex2 & incomingMapKey <= endIndex2){
                                    if (onHoldMap2FirstCopy.isEmpty()){
                                        onHoldMap2FirstCopy = stringToHashMap(incomingMapVal);
                                    }
                                    else{
                                        onHoldMap2SecondCopy = stringToHashMap(incomingMapVal);
                                    }
                                }
                                // Belongs to map3
                                else{
                                    if (onHoldMap3FirstCopy.isEmpty()){
                                        onHoldMap3FirstCopy = stringToHashMap(incomingMapVal);
                                    }
                                    else{
                                        onHoldMap3SecondCopy = stringToHashMap(incomingMapVal);
                                    }
                                }

                                if (!(onHoldMap1FirstCopy.isEmpty() & onHoldMap1FirstCopy.isEmpty())){
                                    Set<Integer> allKeys = new HashSet<>();
                                    allKeys.addAll(onHoldMap1FirstCopy.keySet());
                                    allKeys.addAll(onHoldMap1SecondCopy.keySet());

                                    for (Integer mapKey : allKeys) {
                                        Integer firstCopyValue = onHoldMap1FirstCopy.get(mapKey);
                                        Integer secondCopyValue = onHoldMap1SecondCopy.get(mapKey);

                                        if (firstCopyValue != null && secondCopyValue != null && firstCopyValue.equals(secondCopyValue)) {
                                            // If the key only exists in one on-hold map, it won't be updated in map1.
                                            Integer map1Value = map1.get(mapKey);

                                            // If map1 has a different value than the two on-hold maps, or doesn't contain the key
                                            if (!firstCopyValue.equals(map1Value)) {
                                                map1.put(mapKey, firstCopyValue); // Update map1 to reflect the value from the on-hold maps
                                            }
                                        }
                                    }
                                    System.out.println("Local Lamport time: " + lamportTimestamp + ",Map 1 updated: " + map1);
                                }

                                if (!(onHoldMap2FirstCopy.isEmpty() & onHoldMap2FirstCopy.isEmpty())){
                                    Set<Integer> allKeys = new HashSet<>();
                                    allKeys.addAll(onHoldMap2FirstCopy.keySet());
                                    allKeys.addAll(onHoldMap2SecondCopy.keySet());

                                    for (Integer mapKey : allKeys) {
                                        Integer firstCopyValue = onHoldMap2FirstCopy.get(mapKey);
                                        Integer secondCopyValue = onHoldMap2SecondCopy.get(mapKey);

                                        if (firstCopyValue != null && secondCopyValue != null && firstCopyValue.equals(secondCopyValue)) {
                                            // If the key only exists in one on-hold map, it won't be updated in map1.
                                            Integer map1Value = map2.get(mapKey);

                                            // If map1 has a different value than the two on-hold maps, or doesn't contain the key
                                            if (!firstCopyValue.equals(map1Value)) {
                                                map2.put(mapKey, firstCopyValue); // Update map1 to reflect the value from the on-hold maps
                                            }
                                        }
                                    }
                                    System.out.println("Local Lamport time: " + lamportTimestamp + ",Map 2 updated: " + map2);
                                }

                                if (!(onHoldMap3FirstCopy.isEmpty() & onHoldMap3FirstCopy.isEmpty())){
                                    Set<Integer> allKeys = new HashSet<>();
                                    allKeys.addAll(onHoldMap3FirstCopy.keySet());
                                    allKeys.addAll(onHoldMap3SecondCopy.keySet());

                                    for (Integer mapKey : allKeys) {
                                        Integer firstCopyValue = onHoldMap3FirstCopy.get(mapKey);
                                        Integer secondCopyValue = onHoldMap3SecondCopy.get(mapKey);

                                        if (firstCopyValue != null && secondCopyValue != null && firstCopyValue.equals(secondCopyValue)) {
                                            // If the key only exists in one on-hold map, it won't be updated in map1.
                                            Integer map1Value = map3.get(mapKey);

                                            // If map1 has a different value than the two on-hold maps, or doesn't contain the key
                                            if (!firstCopyValue.equals(map1Value)) {
                                                map3.put(mapKey, firstCopyValue); // Update map1 to reflect the value from the on-hold maps
                                            }
                                        }
                                    }
                                    System.out.println("Local Lamport time: " + lamportTimestamp + ",Map 3 updated: " + map3);
                                }

                                break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        public void start() {
            System.out.println("Starting buffer handler " + threadName);
            if (t == null) {
                t = new Thread(this);
                t.start();
            }
        }
    }
}
