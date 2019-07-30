Distributed Systems [CS557]: Programming Assignment 1

Name: Chaitanya Khoje
BNumber: B00714581

How to run the program:

"make" command to run the makefile which compiles and runs the files and that's all there is to it.

Implementation details:

There are a total of 3 java files.
1. HTTPServerSocket; deals with creating the server side socket and waiting on accept() for client requests.
    When a request comes in, the server accepts the request and then comes in picture the "Handler".
2. Handler; deals with the core part of the program. It has the run() method which is called upon successful call of start()
    on handler's object in "HTTPServerSocket". The resource is first parsed from the client's request header, and then sent
    to the third file, "Utility". A byte array of the available resource including the response header is fetched back into the
    "Handler" and a "write()"" is called on the client socket's output stream.
    After this, there is a hashmap which maintains a key value pair for every resource's request count. MAP{resource, requestCount}.
    There is a critical section maintained for updating this map using locks.
3. Utility; deals with preparing the response, writing content & header into byte array.