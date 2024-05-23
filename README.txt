A Java-based distributed hash table file system, with multiple servers and clients for data storage, update and lookup. Applied socket programming to client-server communication. 

Each object has three replicas stored across three servers for easy recovery, decided by object keys and a set of hash functions. Applied Lamport timestamp algorithm to handle total ordering in the distributed system, ensuring actions from different clients can be executed in the same order on different servers.

Separate servers into two groups, servers in different cannot communicate. The servers can update stored objects after the groups are merged.





Project Description

Let there be seven data servers, S0, S1, ... S6 and five clients, C0, C1,...,C4. There exist communication channels between all servers. When a client wishes to perform a read or write, it establishes a communication channel with a server chosen as per the description below. This client-server channel lasts for the duration it takes to complete the read or write operation. All communication channels are FIFO and reliable when they are operational: implemented using sockets. Occasionally, a channel may be disrupted in which case no message can be communicated across that channel. There exists a hash function, H, such that for each object, Ok, H(Ok) yields a value in the range 0 − 6.

• When a client, Ci has to insert/update an object, Ok, this operation must be performed at three servers numbered: H(Ok), H(Ok)+2 modulo 7, and H(Ok)+4 modulo 7. Instead of contacting all three replicas directly, the client establishes a channel with server identified by H(Ok) and sends its task to that server. Then, that server, acting on behalf of the client ensures that the insert/update operation is performed at the three replicas. If server H(Ok) is not accessible from the client, the client then tries to access server H(Ok)+2 modulo 7, asking it to perform the update on the two live replicas.

• When a client, Cj has to read an object, Ok, it can read the value from any one of the three servers: H(Ok), H(Ok)+2 modulo 7, or H(Ok)+4 modulo 7. 

In this project you are required to implement the following (and be able to demonstrate the implementation): 

1. A client should be able to randomly choose any of the three replicas of an object when it wishes to read the value of the object. If a client tries to read an object that is not present (has not been inserted earlier by any client), the operation should return with an error code.

2. When a client wishes to update/insert an object into the data repository, it should be able to successfully perform the operation on at least two, and if possible all the three servers that are required to store the object.

3. If a client is unable to access at least two out of the three servers that are required to store an object, then the client does not perform updates to any replica of that object.

4. If two or more clients try to concurrently write to the same object and at least two replicas are available, the writes must be performed in the same order at all the replicas of the object.

5. You must also selectively disable some channels so that the servers get partitioned into two components, each with a subset of servers such that some updates are permitted in one partition, while other updates are permitted in the other partition.

6. After network partitions are merged and it is found that some writes have been performed at two out of the three replicas of an object, the third replica should be able to synchronize with the other two replicas before any further update to that object is performed and before any read of that object is performed from the third replica.

You will need to demonstrate that you have implemented all the requirements mentioned above. For instance, you may need to selectively disable some channel(s) for a brief period of time so that access two or one replica of an object is disrupted temporarily. If at least two replicas of the object are accessible, updates to that object should be allowed. However, if only one replica of the object is accessible, the update should be aborted and a corresponding message should be sent to the client.




Introduction:
1. To start the program, open 7 terminals on 7 different machines, and 5 more terminals for 5 clients.

2. The servers and clients should be launched one by one in order, from S0~S6 first, then C0~C4

3. After launched, the clients will continue asking for orders. First enter server id (from S0~S6) for "put" or "get" commands, or "merge" or "separate" commands. They will not ask for receiver since they are broadcasting to all servers.

4. For "put" and "get", the program will be asking for the exact command you want to execute. For "put", you will enter key and then value of the key-value pair you want to add or update. For "get", only key is required. There is a "multiput" command similar to "put", but asking for one more variable to run the "put" command multiple times to simulate concurrency.

5. Then the program will execute as follows:

    5.1 The program already defined a maximum hash range of 200, from 0~199, and evenly distributed among the 7 servers.

    5.2 Each part of hash range has 3 replicas distributed among the servers. Ensuring for each object, Ok, H(Ok) yields a value in the range 0 − 6, and three replicas of the object have indexes H(Ok), H(Ok)+2 modulo 7, and H(Ok)+4 modulo 7.

    5.3 Each client connects to all servers, and each server initially connects to all clients and all other servers

    5.4 At first all servers are assigned to the same group (#1). Later by using "separate" command, you can separate them into two groups

6. To separate the servers into groups, first in any of the client terminals enter "separate". The program will ask for 7 numbers in a row, each be either 1 or 2, separated by spaces, to assign 7 group numbers to these servers to simulate separating the servers into two groups.

7. Each server keeps a list of 7 boolean values, indicating their port status to all other servers (and one for themself in contrast). And another list is 7 group numbers of the servers. The newly coming "separate" command will update port status and group numbers based on existing ones.

8. Before sending a message out to another server, the server should check the port status list for the port. If the status is false, it simulates that port is off, so message sending will fail.

9. When receiving a "merge" command, all servers updates server status into all true's, and all group numbers be 1's, simulating that all ports are on. Then each server will create three HashMap of hashmaps:

    9.1 The outer HashMap has a keys for its 3 maps, each map's key is its start Index of the range. And the inner HashMap is the actual HashMap of the range.

    9.2 Then the hashmaps will be passed to the other two replicas who holds the same map piece, in a new message type MAP.

    9.3 When the server's receiverThread receives a message of MAP type, it will parse the message and find the key-value pair, put it in one of the temp spaces and wait for another one. Upon both arrives, update HashMap based on the received maps.


ISSUE:
merge function is not working. Error messages shown on servers are like:

    Local Lamport time: 4, Handling command: 4:C1:merge
    Received message: 14:S5:MAP:29:
    Buffered message: 15:S5:MAP:29:
    Buffer status:[15:S5:MAP:29:]
    Received message: 17:S3:MAP:29:
    Buffered message: 18:S3:MAP:29:
    Buffer status:[15:S5:MAP:29:, 18:S3:MAP:29:]
    Received message: 19:S3:MAP:172:
    Buffered message: 20:S3:MAP:172:
    Buffer status:[15:S5:MAP:29:, 18:S3:MAP:29:, 20:S3:MAP:172:]
    Received message: 10:S6:MAP:172:
    Buffered message: 21:S6:MAP:172:
    Buffer status:[15:S5:MAP:29:, 18:S3:MAP:29:, 20:S3:MAP:172:, 21:S6:MAP:172:]
    Received message: 26:S6:MAP:116:
    Buffered message: 27:S6:MAP:116:
    Buffer status:[15:S5:MAP:29:, 18:S3:MAP:29:, 20:S3:MAP:172:, 21:S6:MAP:172:, 27:S6:MAP:116:]
    Forwarded message:"5:S1:MAP:29:" to server S3
    Forwarded message:"28:S1:MAP:29:" to server S5
    Forwarded message:"29:S1:MAP:172:" to server S6
    Forwarded message:"30:S1:MAP:172:" to server S3
    Forwarded message:"31:S1:MAP:116:" to server S6
    Forwarded message:"32:S1:MAP:116:" to server S4
    Local Lamport time: 32, Handling command: 15:S5:MAP:29:
    Received message: 32:S4:MAP:116:
    Buffered message: 33:S4:MAP:116:
    Buffer status:[18:S3:MAP:29:, 21:S6:MAP:172:, 20:S3:MAP:172:, 27:S6:MAP:116:, 33:S4:MAP:116:]
    Exception in thread "Thread-11" java.lang.RuntimeException: java.lang.NumberFormatException: For input string: ""
        at ServerHelper$AppendThread.run(ServerHelper.java:653)
        at java.base/java.lang.Thread.run(Thread.java:840)
    Caused by: java.lang.NumberFormatException: For input string: ""
        at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
        at java.base/java.lang.Integer.parseInt(Integer.java:678)
        at java.base/java.lang.Integer.parseInt(Integer.java:786)
        at ServerHelper.stringToHashMap(ServerHelper.java:142)
        at ServerHelper$AppendThread.run(ServerHelper.java:557)
        ... 1 more



    Received message: 6:C1:merge
    Buffered message: 8:C1:merge
    Buffer status:[]
    Local Lamport time: 8, Handling command: 8:C1:merge
    Forwarded message:"9:S5:MAP:144:" to server S0
    Forwarded message:"10:S5:MAP:144:" to server S2
    Forwarded message:"11:S5:MAP:87:" to server S3
    Forwarded message:"12:S5:MAP:87:" to server S0
    Forwarded message:"13:S5:MAP:29:" to server S3
    Forwarded message:"14:S5:MAP:29:" to server S1
    Received message: 18:S0:MAP:0:
    Buffered message: 19:S0:MAP:0:
    Buffer status:[]
    Local Lamport time: 19, Handling command: 19:S0:MAP:0:
    Received message: 6:S2:MAP:58:
    Buffered message: 20:S2:MAP:58:
    Buffer status:[20:S2:MAP:58:]
    Received message: 14:S2:MAP:0:
    Buffered message: 21:S2:MAP:0:
    Buffer status:[20:S2:MAP:58:, 21:S2:MAP:0:]
    Exception in thread "Thread-11" java.lang.RuntimeException: java.lang.NumberFormatException: For input string: ""
        at ServerHelper$AppendThread.run(ServerHelper.java:653)
        at java.base/java.lang.Thread.run(Thread.java:840)
    Caused by: java.lang.NumberFormatException: For input string: ""
        at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
        at java.base/java.lang.Integer.parseInt(Integer.java:678)
        at java.base/java.lang.Integer.parseInt(Integer.java:786)
        at ServerHelper.stringToHashMap(ServerHelper.java:142)
        at ServerHelper$AppendThread.run(ServerHelper.java:575)
        ... 1 more
    Received message: 32:S1:MAP:116:
    Buffered message: 33:S1:MAP:116:
    Buffer status:[20:S2:MAP:58:, 21:S2:MAP:0:, 33:S1:MAP:116:]


5/23/2024: get function is not working, maybe due to wrong message generated
put function needs fixing: if replica 2 is in another group different from replica 1 and 3, 1 cannot send message to 3.

GeneratedMessage: get:get:1
[Waiting]Enter command (put/multiput/get/separate/merge):
Error sending message: For input string: "e"
java.lang.NumberFormatException: For input string: "e"
	at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
	at java.base/java.lang.Integer.parseInt(Integer.java:668)
	at java.base/java.lang.Integer.parseInt(Integer.java:786)
	at ClientHelper.sendMessage(ClientHelper.java:249)
	at C1.main(C1.java:66)
