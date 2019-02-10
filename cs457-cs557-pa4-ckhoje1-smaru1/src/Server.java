import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

public class Server {

	private static ServerSocket serverSocket = null;
	private static boolean afterHintMessage = false;
	private static ServerContext serverContext = new ServerContext();
	private static StringBuilder writeLog = new StringBuilder();
	private static Socket coordinatorSocket = null;

	public Server() { }

	public static void main(String[] args) throws UnknownHostException {

		String replicaFileName = processArguments(args);
		System.out.println("===============SERVER STARTED====================");
		System.out.println(serverContext.getNodeIP());
		System.out.println(serverContext.getNodeName());
		System.out.println(serverContext.getNodePort());
		if (replicaFileName.equals("")) {
			System.err.println("Replica file name argument not passeds!");
			System.exit(0);
		}
		populateReplicaServer(replicaFileName);

		//TODO: Arguments are processed in the above method, remove testing code below as per requirement.
		//TODO: We dont know if this is a replica.
		// We just start the server here, when coordinator decides the replicas,
		// we handle it somewhere else and populate thru log file in KVStore class

		String logfileName = serverContext.getNodeName();
		String filePath = "Log/".concat(logfileName).concat(".log");
		//System.out.println("Outside and before findfile : " + filePath);

		//Check if log file is present or not.
		if (findFile(filePath)) {
			System.out.println("File is present and reaading from it and file name is: " + filePath);
			processBranchFile(filePath);
		}
		try {
			serverSocket = new ServerSocket(serverContext.getNodePort());

		} catch (Exception e) {
			System.out.println("Error while creating serverSocket for a server." + e);
		}
		KeyValStore.KeyValMessage kVMessage = null;
		Socket socket = null;
		while (true) {
			// Start accepting requests on this server
			if (!afterHintMessage) {
				socket = null;
			}
			afterHintMessage = false;

			try {

				// Before accepting incoming sockets, we store the current Server in a group for later use (in ByteOrderPartitioner)
				//TODO: TO HANDLE BYTE ORDERED PARTITIONER
				// It is handle in the coordinator.
				/*
				 * While sending the key-value pair to a replica via PUT message;
				 * Eg. Check if the key resides in the range 0-63, if it does, send it to server1,server2,server3
				 */
				if (socket == null) {
					System.out.println("Server is waiting on: " + serverSocket);
					socket = serverSocket.accept();
					System.out.println("Accepted for: " + socket);
				}
				kVMessage = KeyValStore.KeyValMessage.parseDelimitedFrom(socket.getInputStream());
				System.out.println(kVMessage);

				if (kVMessage != null) {
					/*
					 * Client communication
					 */
					if (kVMessage.getConnectionWith() == 1) { /*connectWith.getConnectionWith() == 1*/
						// Start the coordinator
						System.out.println("Coordinator initializing..");
						Coordinator coordinator = new Coordinator(serverContext, kVMessage, socket);
						coordinatorSocket = socket;    /* Save coordinator's socket; */
						coordinator.handle();
						socket.getOutputStream().flush();
					}
					/*
					 * Server/replica to server/replica communication
					 */
					if (kVMessage.getConnectionWith() == 0) {
						// Coordinator wants to pass on hint to the recovered server
						if (kVMessage.hasHint()) {
							KeyValStore.Hint receivedHint = kVMessage.getHint();
							serverContext.getHints().put(kVMessage.getHint().getFailedServer(), receivedHint);
							KVStore kvStore = new KVStore();
							// Write to log file
							writeHint(serverContext.getNodeName(), receivedHint, receivedHint.getTime());
							if (serverContext.getStorageForData().get(receivedHint.getKey()) != null) {
								if (receivedHint.getTime() >= serverContext.getStorageForData().get(receivedHint.getKey()).getTime()) {
									kvStore.setTime(receivedHint.getTime());
								}
								kvStore.setKey(receivedHint.getKey());
								kvStore.setValue(receivedHint.getValue());
								serverContext.getStorageForData().replace(receivedHint.getKey(), kvStore);
							} else {
								kvStore.setTime(receivedHint.getTime());
								kvStore.setKey(receivedHint.getKey());
								kvStore.setValue(receivedHint.getValue());
								serverContext.getStorageForData().put(receivedHint.getKey(), kvStore);
							}
							System.out.println("Hint received from coordinator");
							afterHintMessage = true;
						}

						// Coordinator wants to pass on repair message
						if (kVMessage.hasReadRepair()) {
							KeyValStore.ReadRepair receivedRepair = kVMessage.getReadRepair();
							writeRepair(serverContext.getNodeName(), receivedRepair, receivedRepair.getTime());
							KVStore kvStore;
							if (serverContext.getStorageForData().containsKey(receivedRepair.getKey())) {
								kvStore = serverContext.getStorageForData().get(receivedRepair.getKey());
								kvStore.setTime(receivedRepair.getTime());
								kvStore.setValue(receivedRepair.getValue());
								//System.out.println("Value successfully updated by repair message on server: " + serverContext.getNodeName());
							}
						}

						// Coordinator wants to PUT
						if (kVMessage.hasPut()) {
							//Date date = new Date();
							//int time = (int) date.getTime();
							
							KeyValStore.Put put = kVMessage.getPut();
							//System.out.println("Inside serverside checking time.: "+kVMessage.getPutTime());
							long time = kVMessage.getPutTime();
							// Write to log
							write(serverContext.getNodeName(), put, time);

							KVStore kvStore;
							if (serverContext.getStorageForData().containsKey(put.getKey())) {
								// Existing kvStore in a server
								kvStore = serverContext.getStorageForData().get(put.getKey());  // Get existing kvStore
								kvStore.setValue(put.getValue());
								if (kvStore.getTime() < time) {     // If present time is less than the time in update
									kvStore.setTime(time);
								}
							} else {
								// Server doesn't have an entry for the key that we are putting, create new kvStore.
								kvStore = new KVStore();
								kvStore.setKey(put.getKey());
								kvStore.setValue(put.getValue());
								kvStore.setTime(time);
							}
							serverContext.getStorageForData().put(put.getKey(), kvStore);

							/*if (serverContext.getStorageForData().containsKey(put.getKey())) {
								serverContext.getStorageForData().replace(put.getKey(), kvStore);
							}*/

							//serverContext.getStorageForData().put(put.getKey(), kvStore); // This is the main storage (in memory).
							System.out.println("Successfully written to the server message:" + serverContext.getStorageForData().get(put.getKey()).getKey());
							//Creating Response to the coordinator.
							System.out.println("Preparing response...");
							KeyValStore.Response.Builder writeResponse = KeyValStore.Response.newBuilder();
							writeResponse.setIsReadOrWrite(true); // true for write or put.
							writeResponse.setStatus(true);
							writeResponse.setKey(put.getKey());
							writeResponse.setValue(put.getValue());
							writeResponse.setTime(time);
							writeResponse.setResponderIp(serverContext.getNodeIP());
							writeResponse.setResponderPort(serverContext.getNodePort());
							writeResponse.setResponderName(serverContext.getNodeName());

							//System.out.println("COORDINATOR NAME IN hasPut: " + kVMessage.getCoordinatorName());
							KeyValStore.Hint.Builder hintBuilder = KeyValStore.Hint.newBuilder();
							hintBuilder.setKey(kVMessage.getHintKey());
							hintBuilder.setValue(kVMessage.getHintValue());
							hintBuilder.setTime(kVMessage.getHintTime());
							hintBuilder.setFailedServer(kVMessage.getHintFailedServer());
							KeyValStore.Hint hint1 = hintBuilder.build();

							//System.out.println("HINT-----" + hint1);
							/*
							KVStore storeForHint = new KVStore();
							storeForHint.setKey(kVMessage.getHintKey());
							storeForHint.setValue(kVMessage.getHintValue());
							storeForHint.setTime(kVMessage.getHintTime());
							*/

							serverContext.getHints().put(kVMessage.getHintFailedServer(), hint1);

							//System.out.println("KEY EXISTS FOR COORDINATOR MENTIONED ABOVE!!");
							writeResponse.setHintKey(kVMessage.getHintKey());
							writeResponse.setHintServerName(hint1.getFailedServer());
							writeResponse.setHintTime(hint1.getTime());
							writeResponse.setHintValue(hint1.getValue());


							Client.keyValMessage.setResponse(writeResponse.build());
							Client.keyValMessage.setConnectionWith(0);
							Client.keyValMessage.build().writeDelimitedTo(socket.getOutputStream());

							//System.out.println("Client.keyValMessage" + Client.keyValMessage);
							//System.out.println("KVMESSAGE" + kVMessage);
							//System.out.println("RESPONSE SENT!");
							//System.out.println("SOcker info from put of servers::::::" + socket);
							socket.getOutputStream().flush();
						}
						// Coordinator wants to GET
						if (kVMessage.hasGet()) {
							//TODO: Implement hintedhandoff

							KeyValStore.Get get = kVMessage.getGet();
							int key = kVMessage.getGet().getKey();
							String value = null;
							long time = 0;
							//Creating Response to the coordinator.
							KeyValStore.Response.Builder readResponse = KeyValStore.Response.newBuilder();
							readResponse.setIsReadOrWrite(false); // true for write or put.
							if (serverContext.getStorageForData().containsKey(key)) {
								value = serverContext.getStorageForData().get(key).getValue();
								time = serverContext.getStorageForData().get(key).getTime();
								readResponse.setStatus(true);
								readResponse.setKey(get.getKey());
								readResponse.setValue(value);
								readResponse.setTime(time);
								readResponse.setResponderName(serverContext.getNodeName());
								readResponse.setResponderPort(serverContext.getNodePort());
								readResponse.setResponderIp(serverContext.getNodeIP());

								//System.out.println("Successfully read to the server message:" + serverContext.getStorageForData().get(key));
							} else {
								readResponse.setStatus(false);
								readResponse.setKey(get.getKey());
								//System.out.println("Failure while reading for the server");
							}
							if (serverContext.getHints().containsKey(kVMessage.getCoordinatorName())) {
								KeyValStore.Hint hint = serverContext.getHints().get(kVMessage.getCoordinatorName());
								readResponse.setHintKey(hint.getKey());
								readResponse.setHintServerName(hint.getFailedServer());
								readResponse.setHintTime(hint.getTime());
								readResponse.setHintValue(hint.getValue());
							}

							Client.keyValMessage.setResponse(readResponse.build());
							Client.keyValMessage.setConnectionWith(0);
							Client.keyValMessage.build().writeDelimitedTo(socket.getOutputStream());

							System.out.println(socket);
							socket.getOutputStream().flush();
						}
						if (!kVMessage.hasHint()) {
							socket.close();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * All program arguments are processed in this helper method.
	 *
	 * @param args
	 * @return
	 * @throws UnknownHostException
	 */
	private static String processArguments(String[] args) throws UnknownHostException {

		if (args.length != 4) {
			System.err.println("Kindly run the server.sh along with 4 variables,like ./server.sh <server unique name> <port number> <configuration file> <Read Repair or HINTed Handoff>.");
			System.exit(-1);
		} else {
			serverContext.setNodeName(args[0]);
			serverContext.setNodePort(Integer.parseInt(args[1]));

			//TODO: Remove this code segment after testing;

			// FOR TESTING PURPOSES:
			/*String serverName = "server4";
			int serverPort = 8084;*/
/*			serverContext.setNodeName(args[0]);
			serverContext.setNodePort(args[1]);*/
			// ---------
			// If the 3rd argument is 1; use hinted-handoff, else if its 2; use read repair

			serverContext.setRepairCategory(Integer.parseInt(args[3]));
			serverContext.setNodeIP(InetAddress.getLocalHost().getHostAddress());
			return args[2];
		}
		return "";
	}

	/**
	 * This method populates replica server list.
	 *
	 * @param fileName
	 */
	private static void populateReplicaServer(String fileName) {

		FileProcessor fileProcessor = new FileProcessor(fileName);
		while (fileProcessor.hasNextLine()) {
			FileLineToken fileLineToken = ProcessServerUtil.processLine(fileProcessor.readFileLine());
			if (fileLineToken != null) {
				serverContext.setReplicas(new ReplicaServer());
				serverContext.getReplicas().setReplicaServerName(fileLineToken.getServerName());
				serverContext.getReplicas().setReplicaServerId(fileLineToken.getServerIPAddress());
				serverContext.getReplicas().setReplicaServerPortNumber(fileLineToken.getServerPortNumber());
				serverContext.getReplicaServers().add(serverContext.getReplicas());
				
			
			}
		}
		for (ReplicaServer s : serverContext.getReplicaServers()) {
			System.out.println("Servers: " + s.getReplicaServerName());
		}
	}

	/**
	 * Reads the LOG file and adds it KVStore.
	 *
	 * @param filePathIn
	 */
	public static void processBranchFile(String filePathIn) {
		String[] splitArray = null;
		int key = 0;
		String value = null;
		long time = 0;
		KVStore kvStore = null;
		//System.out.println("Inside proceesBranchFIle");
		FileProcessor fileProcessor = new FileProcessor(filePathIn);
		while (fileProcessor.hasNextLine()) {
			splitArray = fileProcessor.readFileLine().split(" ");
			if(splitArray.length==3) {

				key = Integer.parseInt(splitArray[0]);
				value = splitArray[1];
				time = Long.parseLong(splitArray[2]);
				kvStore = new KVStore();
				kvStore.setKey(key);
				kvStore.setValue(value);
				kvStore.setTime(time);
				if(serverContext.getStorageForData().isEmpty()) {
					serverContext.getStorageForData().put(key, kvStore);
					//System.out.println("Inside isempty and value is: "+ serverContext.getStorageForData().get(key).getValue());
				}
				else {
					if(kvStore.getTime() >= serverContext.getStorageForData().get(key).getTime())
					serverContext.getStorageForData().replace(key, kvStore);
					//System.out.println("Inside else of processBranchFile and value is: "+ serverContext.getStorageForData().get(key).getValue());
				}
			}	
		}
	}

	/**
	 * To Check if file is present or not in the LOG folder.
	 *
	 * @param path
	 * @return true or false.
	 */
	public static boolean findFile(String path) {
		if (path.equals(null) || path.equals("")) {
			return false;
		}
		File file = new File(path);
		if (!file.exists()) {
			return false;
		}
		return true;
	}

	/**
	 * This writes the log file.
	 */
	public static void write(String nodeName, KeyValStore.Put put, long time) {

		StringBuilder writeLog1 = new StringBuilder();
		writeLog1.append(put.getKey());
		writeLog1.append(" ");
		writeLog1.append(put.getValue());
		writeLog1.append(" ");
		writeLog1.append(put.getTime());
		writeLog1.append(System.getProperty("line.separator"));
		System.out.println(writeLog1);

		String string= writeLog1.toString();
		BufferedWriter bufferedWriter = null;
		FileWriter fileWrite = null;
		String absolutePath ="Log/".concat(nodeName).concat(".log");
		System.out.println("Log file will be create with name: "+absolutePath);
		try {
			File file = new File(absolutePath);
			if(!file.exists()) {
				if (file.createNewFile()) {
					System.out.println("Log written!");
				}
			}
			fileWrite = new FileWriter(file, true);
			bufferedWriter = new BufferedWriter(fileWrite);
			bufferedWriter.write(string);

		} catch (IOException ioe) {
			System.out.println("One or more output files were not found!");
		} finally {
			try {

				if (bufferedWriter != null) {
					bufferedWriter.flush();
					bufferedWriter.close();
				}
				if (fileWrite != null) {
					fileWrite.flush();
					fileWrite.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}
	}

	public static void writeHint(String nodeName, KeyValStore.Hint hint, long time) {

		StringBuilder writeLog1 = new StringBuilder();
		writeLog1.append(hint.getKey());
		writeLog1.append(" ");
		writeLog1.append(hint.getValue());
		writeLog1.append(" ");
		writeLog1.append(hint.getTime());
		writeLog1.append(System.getProperty("line.separator"));
		System.out.println(writeLog1);

		String string= writeLog1.toString();
		BufferedWriter bufferedWriter = null;
		FileWriter fileWrite = null;
		String absolutePath ="Log/".concat(nodeName).concat(".log");
		System.out.println("Log file will be create with name: "+absolutePath);
		try {
			File file = new File(absolutePath);
			if(!file.exists()) {
				if (file.createNewFile()) {
					System.out.println("Log written!");
				}
			}
			fileWrite = new FileWriter(file, true);
			bufferedWriter = new BufferedWriter(fileWrite);
			bufferedWriter.write(string);

		} catch (IOException ioe) {
			System.out.println("One or more output files were not found!");
		} finally {
			try {

				if (bufferedWriter != null) {
					bufferedWriter.flush();
					bufferedWriter.close();
				}
				if (fileWrite != null) {
					fileWrite.flush();
					fileWrite.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}
	}

	public static void writeRepair(String nodeName, KeyValStore.ReadRepair repair, long time) {

		StringBuilder writeLog1 = new StringBuilder();
		writeLog1.append(repair.getKey());
		writeLog1.append(" ");
		writeLog1.append(repair.getValue());
		writeLog1.append(" ");
		writeLog1.append(repair.getTime());
		writeLog1.append(System.getProperty("line.separator"));
		System.out.println(writeLog1);

		String string= writeLog1.toString();
		BufferedWriter bufferedWriter = null;
		FileWriter fileWrite = null;
		String absolutePath ="Log/".concat(nodeName).concat(".log");
		System.out.println("Log file will be create with name: "+absolutePath);
		try {
			File file = new File(absolutePath);
			if(!file.exists()) {
				if (file.createNewFile()) {
					System.out.println("Log written!");
				}
			}
			fileWrite = new FileWriter(file, true);
			bufferedWriter = new BufferedWriter(fileWrite);
			bufferedWriter.write(string);

		} catch (IOException ioe) {
			System.out.println("One or more output files were not found!");
		} finally {
			try {
				if (bufferedWriter != null) {
					bufferedWriter.flush();
					bufferedWriter.close();
				}
				if (fileWrite != null) {
					fileWrite.flush();
					fileWrite.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}
	}
}
