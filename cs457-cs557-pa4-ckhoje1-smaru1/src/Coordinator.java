import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Coordinator {

	private ServerContext serverContext;
	private Socket socket;
	private KeyValStore.KeyValMessage inKeyValMsgClient;
	private KeyValStore.KeyValMessage.Builder keyValueStoreBuilder = null;
	private StringBuilder writeLog = new StringBuilder();
	private int failCount = 0;
	private int successfulWrites = 0;
	private int successfulReads = 0;
	private static long putToTime=0;
	private Map<String, KeyValStore.Response> readValues  = new HashMap<String, KeyValStore.Response>();

	public Coordinator(ServerContext serverContextIn, KeyValStore.KeyValMessage keyValMessageIn, Socket socketIn) {

		serverContext = serverContextIn;
		inKeyValMsgClient = keyValMessageIn;
		socket = socketIn;
	}

	public void handle() {

		if (inKeyValMsgClient.hasGet()) {
			get();
		} else if (inKeyValMsgClient.hasPut()) {
			put();
		}
	}

	private void get() {

		System.out.println("Processing GET...");
		KeyValStore.Get getMsg = inKeyValMsgClient.getGet();

		Date date = new Date();
		int time = (int) date.getTime();
		putToTime = (int) date.getTime();
		String consistency_level = getMsg.getConsistencyLevel();
		KeyValStore.Get.Builder getToServer = KeyValStore.Get.newBuilder();

		System.out.println(getMsg.getKey());

		getToServer.setKey(getMsg.getKey());
		getToServer.setConsistencyLevel(consistency_level);

		keyValueStoreBuilder = KeyValStore.KeyValMessage.newBuilder();
		keyValueStoreBuilder.setGet(getToServer.build());
		keyValueStoreBuilder.setConnectionWith(0);
		keyValueStoreBuilder.setCoordinatorName(serverContext.getNodeName());
		keyValueStoreBuilder.setCoordinatorIp(serverContext.getNodeIP());
		keyValueStoreBuilder.setCoordinatorPort(serverContext.getNodePort());

		if (false) { //check consistency and if not met write an exception if ( 1 >= count)
		} else {

			System.out.println("Server Info:" + serverContext.getReplicaServers().get(0));
			int primaryindex = getToServer.getKey() / 64;
			int firstSecondaryIndex = 99;
			int secondSecondaryIndex = 99;
			List<Integer> indexForStoringToReplicas = new ArrayList<Integer>();
			indexForStoringToReplicas.add(primaryindex);
			firstSecondaryIndex = (primaryindex + 1) % 4;
			secondSecondaryIndex = (primaryindex + 2) % 4;
			indexForStoringToReplicas.add(firstSecondaryIndex);
			indexForStoringToReplicas.add(secondSecondaryIndex);
			connectToServers(indexForStoringToReplicas, keyValueStoreBuilder, consistency_level);
		}
	}


	private void put() {
		// key present for put.
		System.out.println("Processing PUT...");

		KeyValStore.Put putMsg = inKeyValMsgClient.getPut();

		Date date = new Date();
		long time = date.getTime();
		putToTime =  date.getTime();
		//System.out.println("putToTime"+putToTime+ " And date.gettime() "+ date.getTime());
		String consistency_level = putMsg.getConsistencyLevel();

		KeyValStore.Put.Builder putToServer = KeyValStore.Put.newBuilder();
		System.out.println(putMsg.getKey());
		putToServer.setKey(putMsg.getKey());
		putToServer.setValue(putMsg.getValue());
		putToServer.setTime(time);
		putToServer.setConsistencyLevel(consistency_level);
		keyValueStoreBuilder = KeyValStore.KeyValMessage.newBuilder();
		keyValueStoreBuilder.setPut(putToServer.build());
		keyValueStoreBuilder.setConnectionWith(0);
		keyValueStoreBuilder.setPutTime(putToTime);
		keyValueStoreBuilder.setCoordinatorName(serverContext.getNodeName());
		keyValueStoreBuilder.setCoordinatorIp(serverContext.getNodeIP());
		keyValueStoreBuilder.setCoordinatorPort(serverContext.getNodePort());

		if (false) { //check consistency and if not met write an exception if ( 1 >= count)

		} else {
			int primaryindex = putToServer.getKey() / 64;
			int firstSecondaryIndex = 99;
			int secondSecondaryIndex = 99;
			List<Integer> indexForStoringToReplicas = new ArrayList<Integer>();
			indexForStoringToReplicas.add(primaryindex);
			firstSecondaryIndex = (primaryindex + 1) % 4;
			secondSecondaryIndex = (primaryindex + 2) % 4;
			indexForStoringToReplicas.add(firstSecondaryIndex);
			indexForStoringToReplicas.add(secondSecondaryIndex);
			//System.out.println("After partitioning"+primaryindex +" "+firstSecondaryIndex+" "+ secondSecondaryIndex);

			connectToServers(indexForStoringToReplicas, keyValueStoreBuilder, consistency_level);
		}
	}

	/**
	 * This method will connect to the servers to put or get the keys.
	 *
	 * @param indexForStoringToReplicasIn
	 * @param keyValueStoreBuilderIn
	 */
	private void connectToServers(List<Integer> indexForStoringToReplicasIn, KeyValStore.KeyValMessage.Builder keyValueStoreBuilderIn, String consistency_level) {

		KeyValStore.KeyValMessage.Builder keyValueStoreToServerBuilder = keyValueStoreBuilder;
		//System.out.println("Size: " + indexForStoringToReplicasIn.size());
		//System.out.println("Size of servercontent array: " + serverContext.getReplicaServers());
		String targetServer = "";
		for (int i = 0; i < 3; i++) {
			//System.out.println("KeyvalStoreBuilder: "+keyValueStoreBuilder+" With i value: "+i);

			try {
				if (serverContext.getReplicaServers().get(indexForStoringToReplicasIn.get(i)).getReplicaServerName() != null) {
					targetServer = serverContext.getReplicaServers().get(indexForStoringToReplicasIn.get(i)).getReplicaServerName();
				}

				if (!serverContext.getNodeName().equalsIgnoreCase(targetServer)
                        && !targetServer.equals("")) {
					Socket socket = new Socket(serverContext.getReplicaServers().get(indexForStoringToReplicasIn.get(i)).getReplicaServerId(), serverContext.getReplicaServers().get(indexForStoringToReplicasIn.get(i)).getReplicaServerPortNumber());

					/*
					    Check if any hints are stored and if the servers that were down, are now up;
                        If a server is up, update its failed state to false.
                    */
					if (!serverContext.getFailedServers().isEmpty()) {
                        if (serverContext.getFailedServers().containsKey(targetServer)) serverContext.getFailedServers().put(targetServer, false);
                    }

					/*
						HINTED-HANDOFF HINT SENDING CODE SEGMENT (Repair-Category = 1)
					    Get the hints and start sending to up servers
					    Note: Same socket is used for sending hints and sending GET/PUT messages
					*/
					int key = 0;
					long time1 = 0;
					String value = "";
					String fail_server = "";
					if (serverContext.getRepairCategory() == 1) {
						//System.out.println(" HINTS: ------" +serverContext.getHints());
						if (!serverContext.getHints().isEmpty()) {  // Does hint map have any entries?
							//System.out.println("TARGET SERVER HINT: " + targetServer);
							if (serverContext.getHints().containsKey("server2")) {   // Does it contain our target server?
								KeyValStore.Hint storedHint = serverContext.getHints().get("server2");   // Fetch its hint
								//System.out.println("Stored hint: " + storedHint);
								if (storedHint != null) {   // Is hint valid?

									key = storedHint.getKey();
									time1 = storedHint.getTime();
									value = storedHint.getValue();
									fail_server = storedHint.getFailedServer();

									KeyValStore.KeyValMessage.Builder hintBuilder = KeyValStore.KeyValMessage.newBuilder();
									hintBuilder.setHint(storedHint);    // Save hint in builder
									// Store coordinator details
									hintBuilder.setCoordinatorPort(serverContext.getNodePort());
									hintBuilder.setCoordinatorIp(serverContext.getNodeIP());
									hintBuilder.setCoordinatorName(serverContext.getNodeName());
									hintBuilder.setConnectionWith(0);   // Tells the target server that this is a message from another server, and not the client.
									// Send hint message to the target server
									// (this is handled by a server in its hasHint method for its kvMessage)
									//System.out.println("Hint builder: " + hintBuilder);
									hintBuilder.build().writeDelimitedTo(socket.getOutputStream());
									socket.getOutputStream().flush();
									// Removing sent hint from the stored hints map.
									serverContext.getHints().remove(targetServer);
								}
							}
						}
					}

					System.out.println(socket);
					// Send a request (GET/PUT)
					KeyValStore.KeyValMessage.Builder individualKVStoreBuilder = keyValueStoreToServerBuilder;
					if (serverContext.getRepairCategory() == 1) {
						//System.out.println("SETTING HINT VARIABLES IN INDIVIDUALKVBUILDER");
						
						individualKVStoreBuilder.setHintKey(key);
						individualKVStoreBuilder.setHintValue(value);
						individualKVStoreBuilder.setHintTime(time1);
						individualKVStoreBuilder.setHintFailedServer(fail_server);
					}
					individualKVStoreBuilder.build().writeDelimitedTo(socket.getOutputStream());

					//System.out.println("Before listening to server");
					//System.out.println("Socket: " + socket.getLocalPort());

					//System.out.println("Inside socket connections.");
					socket.getOutputStream().flush();

					// Handle a response
					KeyValStore.KeyValMessage responseMsg = KeyValStore.KeyValMessage.parseDelimitedFrom(socket.getInputStream());
				
					// Check hint for self (coordinator)
                    if (serverContext.getRepairCategory() == 1) {
                        KVStore kvStore = new KVStore();
                        kvStore.setTime(responseMsg.getHintTime());
                        kvStore.setValue(responseMsg.getHintValue());
                        kvStore.setKey(responseMsg.getHintKey());
                        if (serverContext.getStorageForData().containsKey(responseMsg.getHintKey())) {
                            if (responseMsg.getHintTime() > serverContext.getStorageForData().get(responseMsg.getHintKey()).getTime()) {
                                serverContext.getStorageForData().put(responseMsg.getHintKey(), kvStore);
                            }
                        }
                        serverContext.getStorageForData().put(responseMsg.getHintKey(),kvStore);
						//System.out.println("Inside a method which is responsible to store in failed server hint map.");
                        
                        
                        /*Changed at 12/5/2018 at 6pm
                         *     KeyValStore.Hint coordinatorHint = responseMsg.getHint();
                         *  if (coordinatorHint != null) {
                            KVStore kvStore = new KVStore();
                            kvStore.setTime(coordinatorHint.getTime());
                            kvStore.setValue(coordinatorHint.getValue());
                            kvStore.setKey(coordinatorHint.getKey());
                            if (serverContext.getStorageForData().containsKey(coordinatorHint.getKey())) {
                                if (coordinatorHint.getTime() > serverContext.getStorageForData().get(coordinatorHint.getKey()).getTime()) {
                                    serverContext.getStorageForData().put(coordinatorHint.getKey(), kvStore);
                                }
                            }
                            serverContext.getStorageForData().put(coordinatorHint.getKey(),kvStore);
                        }
*/                    }

					/*
						READ-REPAIR (Repair-Category 2)
						Store the GET operation's fetched values into a map and iteratively check if they are same.
						If they are not same, send the value with the latest timestamp to all replicas
					*/
					if (serverContext.getRepairCategory() == 2) {		// Is the repair category READ REPAIR?
						readValues.put(targetServer, responseMsg.getResponse());
					}

					if (responseMsg.getResponse().getIsReadOrWrite()  && successfulWrites >= 0) successfulWrites++;
					if ((!responseMsg.getResponse().getIsReadOrWrite()) && successfulReads >=0) successfulReads++;

					//System.out.println("Value of successfulWrites: "+ successfulWrites);
					//System.out.println("Value of successfulReads: "+ successfulReads);
					if (responseMsg != null) {
						if (successfulWrites >= 0) {
							if ((consistency_level.equalsIgnoreCase("ONE") && successfulWrites == 1)
									|| (consistency_level.equalsIgnoreCase("QUORUM") && successfulWrites == 2)) {
								// The targetServer responded and is alive

								//System.out.println("Value of successfulWrites insdie if conditions: "+ successfulWrites);

								handleResponse(responseMsg);
								successfulWrites = -1;
							}
						}
						if (successfulReads>=0 ) {
						    if((consistency_level.equalsIgnoreCase("ONE") && successfulReads ==1)
						            || (consistency_level.equalsIgnoreCase("QUORUM") && successfulReads ==2)){

								//System.out.println("Value of successfulReads insdie if conditions: : "+ successfulReads);
						        handleResponse(responseMsg);
						        successfulReads = -1;
                            }
                        }
					}

					//System.out.println("INSIDE CONNECT TO SERVERS RESPONSE: " + responseMsg);
					socket.close();
				} else {
					updateSelf(keyValueStoreBuilderIn);
				}
			} catch (Exception e) {
				/*
					STORE HINT:

					The "instanceOf" check is to handle hard coded number of servers/replicas.
				  	If the list of replicas doesn't have sufficient servers as per the forloop iterations,
				  	it will throw an IndexOutOfBoundsException, which is caught here.
				  	We do not need to save a hint in this case.
				  	Hints are saved when the ConnectionRefused exception is thrown.
			    */

				failCount++;
				//System.out.println("Value of failCOunt: "+failCount);
                //TODO: Helper method for exception handling

				if (!(e instanceof IndexOutOfBoundsException)) {
                    if (e instanceof ConnectException) {
                        System.err.println("Operation failed as the server is not alive!!!");
						System.out.println("Storing hint...");
                        //TODO: Hint being saved for server 2 in test case 2, which should not happen.
                        serverContext.getFailedServers().put(targetServer, true);
                        // Prepare hint
                        KeyValStore.Hint.Builder hintBuilder = KeyValStore.Hint.newBuilder();
                        KeyValStore.KeyValMessage inKeyValMsgClientUpdate = inKeyValMsgClient;
                        KeyValStore.Put put = inKeyValMsgClientUpdate.getPut();
                        hintBuilder.setKey(put.getKey());
                        hintBuilder.setValue(put.getValue());
                        Date date = new Date();
                        int time = (int) date.getTime();
                        hintBuilder.setTime(time);
                        hintBuilder.setFailedServer(targetServer);

                        KeyValStore.Hint hintElement;
                        hintElement = hintBuilder.build();
						//System.out.println("SAVING HINT: " + hintElement);
                        if (serverContext.getHints().get(targetServer) == null) {
                            serverContext.getHints().put(targetServer, hintElement);
                            System.out.println("Hint saved for: " + targetServer);
                        } else {
                            serverContext.getHints().replace(targetServer, hintElement);
                            System.out.println("Hint saved for: " + targetServer);
                        }
                    }
                }

				if((successfulWrites <2 && consistency_level.equalsIgnoreCase("QUORUM") ||
                        successfulReads<2 && consistency_level.equalsIgnoreCase("QUORUM")) && failCount==2){
                    //Create an exception stating that consistency not met.
                    KeyValStore.KeyValMessage.Builder exceptionMsgBuilder1 = KeyValStore.KeyValMessage.newBuilder();
                    KeyValStore.Exception.Builder exceptionBuilder1 =  KeyValStore.Exception.newBuilder();
                    KeyValStore.KeyValMessage inKeyValMsgClientUpdate = inKeyValMsgClient;
                    if (inKeyValMsgClientUpdate.hasPut()) {
						KeyValStore.Put put = inKeyValMsgClientUpdate.getPut();
						//System.out.println("PUT KEY: " + put.getKey());
						exceptionBuilder1.setKey(put.getKey());
						exceptionBuilder1.setExceptionMessage("Only 1 server replicas, QUORUM Consistency not match.");
						exceptionMsgBuilder1.setException(exceptionBuilder1.build());
						//System.out.println("INSIDE FIRST IF OF CATCH.");
					} else if (inKeyValMsgClientUpdate.hasGet()) {
						KeyValStore.Get get = inKeyValMsgClientUpdate.getGet();
						//System.out.println("GET KEY: " + get.getKey());
						exceptionBuilder1.setKey(get.getKey());
						exceptionBuilder1.setExceptionMessage("Only 1 server replicas, QUORUM Consistency not match.");
						exceptionMsgBuilder1.setException(exceptionBuilder1.build());
						//System.out.println("INSIDE FIRST IF OF CATCH.");
					}
            		try {
            			exceptionMsgBuilder1.build().writeDelimitedTo(socket.getOutputStream());
						System.out.println("Response of exception sent to client!!");
						socket.getOutputStream().flush();
					} catch (IOException e1) {
						System.out.println();
					}
                }

				// Condition for all replicas failing
				if (failCount == 3) {
					KeyValStore.KeyValMessage.Builder exceptionMsgBuilder = KeyValStore.KeyValMessage.newBuilder();
					KeyValStore.Exception.Builder exceptionBuilder = KeyValStore.Exception.newBuilder();
					KeyValStore.KeyValMessage inKeyValMsgClientUpdate = inKeyValMsgClient;
					KeyValStore.Put put = inKeyValMsgClientUpdate.getPut();
					exceptionBuilder.setKey(put.getKey());
					exceptionBuilder.setExceptionMessage("All servers are down except the coordinator!");
					exceptionMsgBuilder.setException(exceptionBuilder.build());
					try {
						exceptionMsgBuilder.build().writeDelimitedTo(socket.getOutputStream());
						System.out.println("Response of exception sent to client!!");
						socket.getOutputStream().flush();
					} catch (IOException e1) {
						System.out.println();
					}
				}
			}
		}
		/*
		 	Iterate over saved replica data for read-repair
		 	If there's a mismatch, get the latest value based on timestamp.
		*/

		if (!readValues.isEmpty() && serverContext.getRepairCategory() == 2) {
			long time = 0;
			List<Long> times = new ArrayList<Long>();
			String value = "";
			for (Map.Entry<String, KeyValStore.Response> response : readValues.entrySet()) {
				KeyValStore.Response resp = response.getValue();    // replica response object
				if (resp.getTime() > time) {
					time = resp.getTime();
					times.add(time);
				}
			}
			// We have an array of all replica's times, check max time.
			long maxTime = 0;
			String maxValue = "";
			if (!times.isEmpty()) {
				maxTime = times.get(0);
				for (int i = 1; i < times.size(); i++) {
					if (times.get(i) > maxTime) {
						maxTime = times.get(i);
					}
				}
				//System.out.println("Latest timestamp: " + maxTime);
				for (Map.Entry<String, KeyValStore.Response> rep : readValues.entrySet()) {
					if (rep.getValue().getTime() == maxTime) {
						maxValue = rep.getValue().getValue();
					}
				}
				//System.out.println("Server with latest value: " + maxValue);
			}

			// Send the latest value to all replicas
			if (maxTime != 0 && !maxValue.equals("")) {		// If maxValue and maxTime are not empty
				for (Map.Entry<String, KeyValStore.Response> response : readValues.entrySet()) {
					String replicaName = response.getKey();
					//System.out.println("Sending repair message to: " + replicaName);
					KeyValStore.Response resp = response.getValue();    // replica response object
					KeyValStore.ReadRepair.Builder readRepairBuilder = KeyValStore.ReadRepair.newBuilder();
					readRepairBuilder.setKey(resp.getKey());
					readRepairBuilder.setValue(maxValue);
					readRepairBuilder.setTime(maxTime);
					readRepairBuilder.setDestinationIp(resp.getResponderIp());
					readRepairBuilder.setDestinationPort(resp.getResponderPort());
					KeyValStore.ReadRepair readRepair = readRepairBuilder.build();
					try {
						Socket repairSocket = new Socket(resp.getResponderIp(), resp.getResponderPort());
						KeyValStore.KeyValMessage.Builder readRepairMessage = KeyValStore.KeyValMessage.newBuilder();
						readRepairMessage.setReadRepair(readRepair);
						readRepairMessage.setCoordinatorName(serverContext.getNodeName());
						readRepairMessage.setCoordinatorIp(serverContext.getNodeIP());
						readRepairMessage.setCoordinatorPort(serverContext.getNodePort());
						readRepairMessage.setConnectionWith(0);

						readRepairMessage.build().writeDelimitedTo(repairSocket.getOutputStream());
						repairSocket.getOutputStream().flush();
						repairSocket.close();
						//System.out.println("Repair message sent successfully!!");
					} catch (IOException e) {
						System.err.println("Error while sending repair message to a replica!!");
					}
				}
			}
		}
	}

	public void handleResponse(KeyValStore.KeyValMessage responseMsg) {

		try {
			if (responseMsg != null) {
				if (responseMsg.hasResponse()) {
					//System.out.println("inside has response with socket as: " + socket);
					//System.out.println("Inside Listening: " + responseMsg);
					if (responseMsg.hasResponse()) {
						KeyValStore.Response responseFromServers = responseMsg.getResponse();
						//System.out.println("RESPONSE FROM SERVER boolean check: " + responseFromServers.getIsReadOrWrite());
						//for put/ write
						if (responseFromServers.getIsReadOrWrite()) {
							//System.out.println("PUT RESPONSE TO CLIENT: " + responseFromServers);
							KeyValStore.KeyValMessage.Builder responseToClient = KeyValStore.KeyValMessage.newBuilder();
							if (responseFromServers.getStatus()) {
								// Update server state.
								responseToClient.setResponse(responseFromServers);
								//System.out.println("PUT RESPONSE TO SOCKET: " + socket);
								responseToClient.build().writeDelimitedTo(socket.getOutputStream());
								//System.out.println("Sending response back to the client.");
								socket.getOutputStream().flush();
							} else {
								// Issues with writing to server, and call READ REPAIR ANd other.
							}
						}
						//for get / read response
						else {
							//System.out.println("GET RESPONSE TO CLIENT: " + responseFromServers);
							//System.out.println("Status of read is: "+responseFromServers.getStatus());
							KeyValStore.KeyValMessage.Builder responseToClient = KeyValStore.KeyValMessage.newBuilder();

							if (responseFromServers.getStatus()) {
								//Need to check consistency here :TODO

								responseToClient.setResponse(responseFromServers);
								//System.out.println("GET RESPONSE TO SOCKET: " + socket);
								responseToClient.build().writeDelimitedTo(socket.getOutputStream());
								//System.out.println("Sending response back to the client with responseFromServers.: " + responseFromServers);
								socket.getOutputStream().flush();
							} else {
								//System.out.println("Inside else part of get response when ");
								responseToClient.setResponse(responseFromServers);
								responseToClient.build().writeDelimitedTo(socket.getOutputStream());
								socket.getOutputStream().flush();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error in handle response");
		}
	}

	public void updateSelf(KeyValStore.KeyValMessage.Builder keyValueStoreBuilderIn) throws UnknownHostException, IOException {

        KeyValStore.KeyValMessage.Builder kvBuilder = keyValueStoreBuilderIn;
        KeyValStore.KeyValMessage inKeyValMsgClientUpdate = inKeyValMsgClient;
	    if(inKeyValMsgClientUpdate.hasPut()) {
			KeyValStore.Put put = inKeyValMsgClientUpdate.getPut();
			// Different builder used for TIME update, the one that was built in put() above.
			KeyValStore.Put put1 = kvBuilder.getPut();
			long time = put1.getTime();
			
			System.out.println(put);
			writeLog.append(put.getKey()); // 1 shashwat 10
			writeLog.append(" ");
			writeLog.append(put.getValue());
			writeLog.append(" ");
			writeLog.append(putToTime);
			writeLog.append("\n");
			//System.out.println("Value at updateself for time: "+putToTime);
			serverContext.write(put, putToTime);

			/*KVStore kvStore = new KVStore();
			kvStore.setKey(put.getKey());
			kvStore.setValue(put.getValue());
			kvStore.setTime(time);*/

            KVStore kvStore;
            if (serverContext.getStorageForData().containsKey(put.getKey())) {
                // Existing kvStore in a server
                kvStore = serverContext.getStorageForData().get(put.getKey());  // Get existing kvStore
                kvStore.setValue(put.getValue());
                if (kvStore.getTime() < putToTime) {     // If present time is less than the time in update
                    kvStore.setTime(putToTime);
                }
            } else {
                // Server doesn't have an entry for the key that we are putting, create new kvStore.
                kvStore = new KVStore();
                kvStore.setKey(put.getKey());
                kvStore.setValue(put.getValue());
                kvStore.setTime(putToTime);
            }

			if(serverContext.getStorageForData().containsKey(put.getKey())) {
				serverContext.getStorageForData().replace(put.getKey(), kvStore);
			}

			serverContext.getStorageForData().put(put.getKey(), kvStore); // This is the main storage (in memory).
			//System.out.println("Successfully written to the server message:"+ serverContext.getStorageForData().get(put.getKey()).getKey());
			//System.out.println("From Update self method, with succwrite val: "+successfulWrites);


			//Creating Response for the client.
			if (successfulWrites >= 0) {
				//System.out.println("Inside if condition Selfupdate of PUT.");
				if (inKeyValMsgClient.getPut().getConsistencyLevel().equalsIgnoreCase("ONE")) {
					//for ONE no condition, but QUORUM, dont send response from here, just increment suc++
					KeyValStore.Response.Builder writeResponse = KeyValStore.Response.newBuilder();
					writeResponse.setIsReadOrWrite(true); // true for write or put.
					writeResponse.setStatus(true);
					writeResponse.setKey(put.getKey());
					writeResponse.setValue(put.getValue());
					writeResponse.setTime(putToTime);
					kvBuilder.setResponse(writeResponse.build());

					kvBuilder.build().writeDelimitedTo(socket.getOutputStream());
					socket.getOutputStream().flush();
					successfulWrites=-1;

					//System.out.println("Inside update self sending responding directly from here when Consistency_level is ONE."+successfulWrites);

				}
				if(inKeyValMsgClient.getPut().getConsistencyLevel().equalsIgnoreCase("QUORUM")){
					successfulWrites++;
					if(successfulWrites == 2) {
						//Response send from here
						KeyValStore.Response.Builder writeResponse = KeyValStore.Response.newBuilder();
						writeResponse.setIsReadOrWrite(true); // true for write or put.
						writeResponse.setStatus(true);
						writeResponse.setKey(put.getKey());
						writeResponse.setValue(put.getValue());
						writeResponse.setTime(putToTime);
						kvBuilder.setResponse(writeResponse.build());

						kvBuilder.build().writeDelimitedTo(socket.getOutputStream());
						socket.getOutputStream().flush();
						successfulWrites=-1;
					}
					//System.out.println("Inside if condition Selfupdate of PUT with update succwrites val.: "+successfulWrites);
				}
				// Here

				KeyValStore.Put putMsg = inKeyValMsgClient.getPut();
				KeyValStore.Put.Builder putToServer = KeyValStore.Put.newBuilder();
				System.out.println(putMsg.getKey());
				putToServer.setKey(putMsg.getKey());
				putToServer.setValue(putMsg.getValue());
				putToServer.setTime(putToTime);
				putToServer.setConsistencyLevel(putMsg.getConsistencyLevel());
				keyValueStoreBuilder.setPut(putToServer.build());
				keyValueStoreBuilder.setConnectionWith(0);
				keyValueStoreBuilder.setCoordinatorName(serverContext.getNodeName());
				keyValueStoreBuilder.setCoordinatorIp(serverContext.getNodeIP());
				keyValueStoreBuilder.setCoordinatorPort(serverContext.getNodePort());
				keyValueStoreBuilder.build();
			}
		}
		if(inKeyValMsgClientUpdate.hasGet()) {
			KeyValStore.Get get = inKeyValMsgClientUpdate.getGet();
			System.out.println(get);

			int key = get.getKey();
			String value;
			long time;
			//Creating Response to the coordinator.
			KeyValStore.Response.Builder readResponse = KeyValStore.Response.newBuilder();
			readResponse.setIsReadOrWrite(false); // true for write or put.
			if(serverContext.getStorageForData().containsKey(key)) {
                if (successfulReads >= 0) {
                    if (inKeyValMsgClient.getGet().getConsistencyLevel().equalsIgnoreCase("ONE")) {
                        value = serverContext.getStorageForData().get(key).getValue();
                        time = serverContext.getStorageForData().get(key).getTime();
                        readResponse.setStatus(true);
                        readResponse.setKey(get.getKey());
                        readResponse.setValue(value);
                        readResponse.setTime(time);
                        successfulReads=-1;
						//System.out.println("Successfully read to the server message from selfupdate for ONE :" + serverContext.getStorageForData().get(get.getKey()));
						kvBuilder.setResponse(readResponse.build());
						kvBuilder.setConnectionWith(0);
						kvBuilder.build().writeDelimitedTo(socket.getOutputStream());
						socket.getOutputStream().flush();
                    }
                    if (inKeyValMsgClient.getGet().getConsistencyLevel().equalsIgnoreCase("QUORUM")){
                        successfulReads++;
						if (successfulReads == 2) {
							value = serverContext.getStorageForData().get(key).getValue();
							time = serverContext.getStorageForData().get(key).getTime();
							readResponse.setStatus(true);
							readResponse.setKey(get.getKey());
							readResponse.setValue(value);
							readResponse.setTime(time);
							successfulReads=-1;
							//System.out.println("Successfully read to the server message from selfupdate for ONE :" + serverContext.getStorageForData().get(get.getKey()));
							kvBuilder.setResponse(readResponse.build());
							kvBuilder.setConnectionWith(0);
							kvBuilder.build().writeDelimitedTo(socket.getOutputStream());
							socket.getOutputStream().flush();
						}
                    }
                }
            } else {
			    if(successfulReads>=0) {
                    if(inKeyValMsgClient.getGet().getConsistencyLevel().equalsIgnoreCase("ONE")){
                        readResponse.setStatus(false);
                        readResponse.setKey(get.getKey());

						//System.out.println("Failure while reading for the server");
						kvBuilder.setResponse(readResponse.build());
						kvBuilder.setConnectionWith(0);
						kvBuilder.build().writeDelimitedTo(socket.getOutputStream());
						socket.getOutputStream().flush();
						successfulReads=-1;
                    }
                    if(inKeyValMsgClient.getGet().getConsistencyLevel().equalsIgnoreCase("QUORUM")){
                        successfulReads++;
                        if(successfulReads==2){
							readResponse.setStatus(false);
							readResponse.setKey(get.getKey());
							//System.out.println("Failure while reading for the server");
							kvBuilder.setResponse(readResponse.build());
							kvBuilder.setConnectionWith(0);
							kvBuilder.build().writeDelimitedTo(socket.getOutputStream());
							socket.getOutputStream().flush();
							successfulReads=-1;
						}
                    }
                }
			}

            KeyValStore.Get getMsg = inKeyValMsgClient.getGet();
    		KeyValStore.Get.Builder getToServer = KeyValStore.Get.newBuilder();
    		getToServer.setKey(getMsg.getKey());
    		getToServer.setConsistencyLevel(getMsg.getConsistencyLevel());
    		keyValueStoreBuilder.setGet(getToServer.build());
    		keyValueStoreBuilder.setConnectionWith(0);
    		keyValueStoreBuilder.setCoordinatorName(serverContext.getNodeName());
    		keyValueStoreBuilder.setCoordinatorIp(serverContext.getNodeIP());
    		keyValueStoreBuilder.setCoordinatorPort(serverContext.getNodePort());
    		keyValueStoreBuilder.build();
			//System.out.println("Overriding the message here in update self after responding to client: "+keyValueStoreBuilder);
		}
	}
}
