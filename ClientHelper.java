import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// Some helper functions that all clients use in common
public class ClientHelper {
    static int maxRange = 200;
    static List<Integer> portNums = Arrays.asList(2000, 2001, 2002, 2003, 2004, 2005, 2006);
//    boolean closeReceived = false;
    static String messageToBeSent;
    static List<String> getServerAddresses(){
        List<String> serverAddresses = new ArrayList<>();
        // get server id and address from the file
        List<String> serverInfos = readServerAddressesTXT("serverAddresses.txt");
        for (String serverInfo: serverInfos){
            String[] parts = serverInfo.split(": ");
            serverAddresses.add(parts[1]);
        }
        return serverAddresses;
    }

    static List<String> readServerAddressesTXT(String fileName){
        List<String> lines = new ArrayList<>();
        File file = new File(fileName);
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


    static String generateMessage() throws IOException {
        String receiverOrGlobalCommand;
        String command;
        Integer key;
        Integer value;
        Integer commandRepeatedTimes;

        InputStreamReader inputReaderFromUser = new InputStreamReader(System.in);
        BufferedReader inputFromUser = new BufferedReader(inputReaderFromUser);

        while (true) {
            System.out.println("[Waiting]Enter receiver or separate/merge command:");
            receiverOrGlobalCommand = inputFromUser.readLine();

            // Validate receiver format
            if (receiverOrGlobalCommand.matches("S[0-6]") || receiverOrGlobalCommand.equals("separate") || receiverOrGlobalCommand.equals("merge")) {
                break; // Exit the loop if the format is correct
            } else {
                System.out.println("[Waiting]Invalid input. Enter a receiver in the format 'S0' to 'S6', or separate, merge.");
            }
        }

        switch(receiverOrGlobalCommand){
            case "merge":
                messageToBeSent = receiverOrGlobalCommand;     // Return "merge" as generated message
                break;

            case "separate":
                int[] numbers = new int[7];
                boolean validNumbers = false;
                while (!validNumbers) {
                    System.out.println("[Waiting]Enter 7 numbers, each should be 1 or 2, separated by spaces:");
                    String[] inputNumbers = inputFromUser.readLine().split(" ");
                    if (inputNumbers.length != 7) {
                        System.out.println("[Waiting]Please enter exactly 7 numbers.");
                        continue;
                    }

                    validNumbers = true;
                    for (int i = 0; i < 7; i++) {
                        try {
                            int num = Integer.parseInt(inputNumbers[i]);
                            if (num != 1 && num != 2) {
                                System.out.println("[Waiting]Each number should be 1 or 2. Please try again.");
                                validNumbers = false;
                                break;
                            }
                            numbers[i] = num;
                        } catch (NumberFormatException e) {
                            System.out.println("[Waiting]Invalid input. Please enter valid integers.");
                            validNumbers = false;
                            break;
                        }
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    sb.append(numbers[i]);
                    if (i < 6) {
                        sb.append(" ");
                    }
                }

                String mergedNumbers = sb.toString();
                messageToBeSent = (receiverOrGlobalCommand + ":" + mergedNumbers);   // Return "separate:" and 7 numbers, each either 1 or 2 only for group indexes, separated by space
                break;

            default:
                // All other cases: put, get, multiput, ask for command
                while (true) {
                    System.out.println("[Waiting]Enter command:");
                    command = inputFromUser.readLine();

                    // Validate the command
                    switch (command) {
                        case "put":
                            while (true) {
                                System.out.println("[Waiting]Enter key (0-" + maxRange + "):");
                                try {
                                    key = Integer.parseInt(inputFromUser.readLine());
                                    if (key < 0 || key > maxRange) {
                                        System.out.println("Key must be between 0 and " + maxRange + ". Please try again.");
                                        continue;
                                    }
                                    break; // Exit the loop if key is valid
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input.");
                                }
                            }

                            // Ask for value
                            System.out.println("[Waiting]Enter value:");
                            while (true) {
                                try {
                                    value = Integer.parseInt(inputFromUser.readLine());
                                    break; // Exit the loop if value is valid
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input.");
                                }
                            }

                            messageToBeSent =  receiverOrGlobalCommand + ":" + command + ":" + key  + ":" + value;
                            break;
                        case "multiput":
                            while (true) {
                                System.out.println("[Waiting]Enter key (0-" + maxRange + "):");
                                try {
                                    key = Integer.parseInt(inputFromUser.readLine());
                                    if (key < 0 || key > maxRange) {
                                        System.out.println("Key must be between 0 and " + maxRange + ". Please try again.");
                                        continue;
                                    }
                                    break; // Exit the loop if key is valid
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input.");
                                }
                            }

                            // Ask for value
                            System.out.println("[Waiting]Enter value:");
                            while (true) {
                                try {
                                    value = Integer.parseInt(inputFromUser.readLine());
                                    break; // Exit the loop if value is valid
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input.");
                                }
                            }

                            System.out.println("[Waiting]Enter execution times:");
                            while (true) {
                                try {
                                    commandRepeatedTimes = Integer.parseInt(inputFromUser.readLine());
                                    break; // Exit the loop if value is valid
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input.");
                                }
                            }
                            messageToBeSent =  (receiverOrGlobalCommand + ":" + command + ":" + key  + ":" + value  + ":" + commandRepeatedTimes);
                            break;
                        case "get":
                            while (true) {
                                System.out.println("[Waiting]Enter key (0-" + maxRange + "):");
                                try {
                                    key = Integer.parseInt(inputFromUser.readLine());
                                    if (key < 0 || key > maxRange) {
                                        System.out.println("Key must be between 0 and " + maxRange + ". Please try again.");
                                        continue;
                                    }
                                    break; // Exit the loop if key is valid
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input.");
                                }
                            }
                            messageToBeSent = (receiverOrGlobalCommand + ":" + command + ":" + key);
                            break; // Exit the loop if the command is valid
                        default:
                            System.out.println("[Waiting]Invalid command. Please enter one of: put, multiput, get, separate, merge.");
                            continue; // Repeat the loop to ask for input again
                    }
                    break; // Exit the loop if the command is valid
                }
        }
        return messageToBeSent; // Outgoing messages: receiverOrGlobalCommand:command:rest part(key, key-value pair, group ids).
                                // e.g. : merge; separate:1 1 2 1 2 2 1; S1:put:25:2; S3:get:6; S5:multiput:6:33:50

    }

    static void sendMessage(AtomicInteger lamportTimestamp, List<Socket> allServers, String clientName) throws IOException {
        String generatedMessage = generateMessage();
        System.out.println("GeneratedMessage: " + generatedMessage);
        String[] parts = generatedMessage.split(":");
        try {
            // These commands are sent to all servers
            if (parts[0].equals("separate") || generatedMessage.equals("merge")) {
//            if ( generatedMessage.equals("close") || parts[0].equals("separate") || generatedMessage.equals("merge")) {
                for (int i = 0; i < allServers.size(); i++) {
                    lamportTimestamp.incrementAndGet();
                    String message = lamportTimestamp + ":" + clientName + ":" + generatedMessage;
                    BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(allServers.get(i).getOutputStream()));
                    outputStream.write(message);
                    outputStream.newLine();
                    System.out.println("Local Lamport time: " + lamportTimestamp + ", Sent message " + message + " to server S" + i);
                    outputStream.flush();
                }
            } else {
                // Deal with put, multiput, and get
                String command = parts[1];
                int receiverID = Integer.parseInt(String.valueOf(parts[0].charAt(1)));
                String message = "";
                int commandRepeatedTimes = 1;
                switch (command) {
                    case "put":
                        message = lamportTimestamp + ":" + clientName + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
                        break;
                    case "multiput":
                        commandRepeatedTimes = Integer.parseInt(parts[4]);
                        message = lamportTimestamp + ":" + clientName + ":put:" + parts[2] + ":" + parts[3];
                        break;
                    case "get":
                        message = lamportTimestamp + ":" + clientName + ":" + parts[1] + ":" + parts[2];
                        break;
                }
                for (int i = 0; i < commandRepeatedTimes; i++) {
                    lamportTimestamp.incrementAndGet();
                    message = lamportTimestamp + ":" + message.split(":", 2)[1];
                    BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(allServers.get(receiverID).getOutputStream()));
                    outputStream.write(message);
                    outputStream.newLine();
                    System.out.println("Local Lamport time: " + lamportTimestamp + ", Sent message " + message + " to server S" + receiverID);
                    outputStream.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static String getLamportFromMessage(String receivedMessage){
        String[] parts = receivedMessage.split(":");
        return parts[0];
    }

    public static class ReceiverThread implements Runnable {
        private Thread t;
        String threadName;
        private Socket serverSocket;
        private AtomicInteger lamportTimestamp;
        private BufferedReader inBuffer = null;

        public ReceiverThread(Socket serverSocket, AtomicInteger lamportTimestamp, String threadName) {
            this.serverSocket = serverSocket;
            this.lamportTimestamp = lamportTimestamp;
            this.threadName = threadName;
        }

        public void run() {
            String message;
            try {
                InputStream inputFromClient = serverSocket.getInputStream();
                InputStreamReader inputReader = new InputStreamReader(inputFromClient);
                inBuffer = new BufferedReader(inputReader);
                while(true){
                    message = inBuffer.readLine();
                    int messageLamport = Integer.parseInt(getLamportFromMessage(message));
                    lamportTimestamp.set(Math.max(lamportTimestamp.get(), messageLamport) + 1);
                    System.out.println("Local Lamport time: " + lamportTimestamp + ", Received message: " + message);

                    }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Thread " + threadName + " closed");
            }
        }

        public void start() {
            System.out.println("Starting message receiver.");
            if (t == null) {
                t = new Thread(this);
                t.start();
            }
        }
    }

}
