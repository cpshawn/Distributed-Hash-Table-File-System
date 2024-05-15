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