Programming Assignment 3: The Snapshot Algorithm

Java Programming Language is used for this assignment along with makefile for compiling. Following are the steps require to compile and run the code:

==============================================================================

HOW TO COMPILE AND RUN THE CODE

Git clone from the repository and then please follow below steps and run below step from the clone repo current folder.

Step1:  bash 
export PATH=/home/vchaska1/protobuf/bin:$PATH
protoc --java_out=src/ bank.proto
Step2: chmod +x branch.sh
Step3: chmod +x controller.sh
Step4: chmod +x branches.txt
Step5: make
Step6: ./branch.sh  <branchname> <Port#> <Interval time in ms>  // run this command in different terminal for n times to create n branches.  
Step7: ./controller.sh <Amount> branch.txt  // here amount  is the total amount needs to be distributed among branches and branches.txt  is the file which needs to be put by you, in this file mention all the branches which are created using step 5, please provide correct branch name and port # otherwise code will not work.

Output will be seen in the controller terminal.

==============================================================================

TASK Performed some divided and some individually by both group members.

Shashwat Maru: SM
Chaitanya Khoje: CK

Following are the task performed for the implementation of the project:

1. File reading, - SM
2. Controller code, - SM
3. socket programming, - SM
4. full duplex channel, - SM
5. simple bank transaction setup, - SM 
6. Multithreading in socket connections - SM, 
7. protobuf messages between controller and branches and also between branches, - SM 
8. capturing local state of all the branches, - CK
9. channel states for the branches etc. - CK
10. Javadocs and utility classes. - CK
11. chandy lamport implementation for initSnapshot,  - CK
12. Thread controller deals with pausing and resuming the sender thread upon request by the snapshot handler. - SM
13. Optimizations in the branch code - CK
14. Retrieve and Return Snapshot - CK

The name taggings for the above points is just a brief way to say which implementations were majorly done by a person,
although almost everything was done by both the group members. 

==============================================================================

Status of the project: Every feature is working properly except channel state(pending).

==============================================================================

Note: This code is working almost perfect above 100ms, though few of the snapshots may not match for the local balances if ran on 60 ms or lower.

==============================================================================
SAMPLE CONTROLLER OUTPUT:

snapshot_id: 1
branch1: 939, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 994, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 1165, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 892, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 2
branch1: 885, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 1190, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 964, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 961, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 3
branch1: 1016, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 1038, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 1033, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 913, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 4
branch1: 1064, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 783, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 1118, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 1035, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 5
branch1: 1009, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 888, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 1077, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 1026, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 6
branch1: 1107, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 918, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 963, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 1012, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 7
branch1: 932, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 1026, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 1041, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 1001, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 8
branch1: 1104, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 883, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 957, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 1056, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 9
branch1: 827, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 1218, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 767, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 1188, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
snapshot_id: 10
branch1: 1048, branch2->branch1: 0, branch3->branch1: 0, branch4->branch1: 0
branch2: 946, branch1->branch2: 0, branch3->branch2: 0, branch4->branch2: 0
branch3: 1190, branch1->branch3: 0, branch2->branch3: 0, branch4->branch3: 0
branch4: 816, branch1->branch4: 0, branch2->branch4: 0, branch3->branch4: 0
