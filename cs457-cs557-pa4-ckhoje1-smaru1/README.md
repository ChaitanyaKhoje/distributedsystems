Distributed Systems [CS557]: Programming Assignment 4
Key-Value Store with Configurable Consistency

Group members:

Chaitanya Khoje
Shashwat Maru

Programming language used: 
Java

How to compile and run the program:

Pre-req:

bash
export PATH=/home/vchaska1/protobuf/bin:$PATH
protoc --java_out=./src KeyValStore.proto


Example server.txt:

server1 128.226.114.201 8031
server2 128.226.114.202 8032
server3 128.226.114.203 8033
server4 128.226.114.204 8034
 

1. make
2. chmod +x client.sh
3. chmod +x server.sh
4. Run all servers; eg. 

FOR EACH SERVER:
./server.sh <servername> <port> <servers_file> <hinted_handoff[1]/read_repair[2]>
./server.sh server1 8031 server.txt 1

Client calling server1 to become coordinator;
5. ./client.sh server1 server.txt

Tasks by both group members:
Most of the tasks were performed together as segregating the tasks individually was not possible due to linked logics throughout. 

Chaitanya Khoje [CK]
Shashwat Maru [SM]

1. Client code [SM]
2. KeyValStore proto file [SM/CK]
3. Server connections [SM]
4. GET/PUT implementations [SM/CK]
5. Server-side GET/PUT/HINT protobuf message handling [SM/CK]
6. Hinted-Handoff [SM/CK]
7. Read-Repair [CK]
8. ServerContext for storing all details regarding what a replica/server holds [SM]
9. Consistency levels; ONE/QUORUM [SM/CK]
10. Building protobuf messages throughout [SM/CK]
11. Error handling [CK]
12. Socket connections [SM/CK]
13. File writing and reading [SM]
 



