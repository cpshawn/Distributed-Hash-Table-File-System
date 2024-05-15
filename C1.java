import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class C1 {
    static String clientName = "C1";
    static int clientId = 1;
    // total ordering to decide which command enters queue first
    static AtomicInteger lamportTimestamp = new AtomicInteger(0);

    // Record all server sockets
    static List<Socket> allServers = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        List<String> serverAddresses = ClientHelper.getServerAddresses();
//        System.out.println("serverAddresses:"+serverAddresses);

        Socket C1SenderToS0 = new Socket(serverAddresses.get(0), ClientHelper.portNums.get(0));
        allServers.add(C1SenderToS0);
        ClientHelper.ReceiverThread S0ReceiverThread = new ClientHelper.ReceiverThread(C1SenderToS0, lamportTimestamp, "S0ReceiverThread");
        S0ReceiverThread.start();
        System.out.println("Established connection to server S0.");

        Socket C1SenderToS1 = new Socket(serverAddresses.get(1), ClientHelper.portNums.get(1));
        allServers.add(C1SenderToS1);
        ClientHelper.ReceiverThread S1ReceiverThread = new ClientHelper.ReceiverThread(C1SenderToS1, lamportTimestamp, "S1ReceiverThread");
        S1ReceiverThread.start();
        System.out.println("Established connection to server S1.");

        Socket C1SenderToS2 = new Socket(serverAddresses.get(2), ClientHelper.portNums.get(2));
        allServers.add(C1SenderToS2);
        ClientHelper.ReceiverThread S2ReceiverThread = new ClientHelper.ReceiverThread(C1SenderToS2, lamportTimestamp, "S2ReceiverThread");
        S2ReceiverThread.start();
        System.out.println("Established connection to server S2.");

        Socket C1SenderToS3 = new Socket(serverAddresses.get(3), ClientHelper.portNums.get(3));
        allServers.add(C1SenderToS3);
        ClientHelper.ReceiverThread S3ReceiverThread = new ClientHelper.ReceiverThread(C1SenderToS3, lamportTimestamp, "S3ReceiverThread");
        S3ReceiverThread.start();
        System.out.println("Established connection to server S3.");

        Socket C1SenderToS4 = new Socket(serverAddresses.get(4), ClientHelper.portNums.get(4));
        allServers.add(C1SenderToS4);
        ClientHelper.ReceiverThread S4ReceiverThread = new ClientHelper.ReceiverThread(C1SenderToS4, lamportTimestamp, "S4ReceiverThread");
        S4ReceiverThread.start();
        System.out.println("Established connection to server S4.");

        Socket C1SenderToS5 = new Socket(serverAddresses.get(5), ClientHelper.portNums.get(5));
        allServers.add(C1SenderToS5);
        ClientHelper.ReceiverThread S5ReceiverThread = new ClientHelper.ReceiverThread(C1SenderToS5, lamportTimestamp, "S5ReceiverThread");
        S5ReceiverThread.start();
        System.out.println("Established connection to server S5.");

        Socket C1SenderToS6 = new Socket(serverAddresses.get(6), ClientHelper.portNums.get(6));
        allServers.add(C1SenderToS6);
        ClientHelper.ReceiverThread S6ReceiverThread = new ClientHelper.ReceiverThread(C1SenderToS6, lamportTimestamp, "S6ReceiverThread");
        S6ReceiverThread.start();
        System.out.println("Established connection to server S6.");

        while (true) {
            ClientHelper.sendMessage(lamportTimestamp, allServers, clientName);
        }
    }
}