import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class Client {

	private static String coordinatorName = null;
	private static String coordinatorIP = null;
	private static int coordinatorPort = 0;
	private static Socket coordinatorSocket = null;
    private static ServerSocket responseHandlerSocket = null;
    public static KeyValStore.KeyValMessage.Builder keyValMessage = KeyValStore.KeyValMessage.newBuilder();
	private static KeyValStore.KeyValMessage keyValMsg = null;
    public static boolean invalidCommand = false;
    public static boolean tryConnect = false;
    private static Socket socket = null;

	public static void main(String[] args) throws UnknownHostException, IOException {

		if (args.length != 0) {
			System.out.println("Kindly enter GET or PUT as a Operation");
			connectToCoordinator(args);
			while (true) {
				sendRequest();
				System.out.println("Request successfully sent");
				handleResponse();
			}
		} else {
			System.err.println("Looks like incorrect arguments were provided to the client; Expected: <server-name> <server.txt>");
		}
	}

	/**
	 * Sends client requests
	 *
	 * @throws IOException
	 */
	private static void sendRequest() throws IOException {
		// For sending get or put operation from client to the coordinatorName.
		Scanner sc;
		int key;
		String value;
		String consistency_level;

        String input = "";
        sc = new Scanner(System.in);

        System.out.println("Available commands: ");
        System.out.println("GET <key> <consistency_level> <blank-space>(Ex. GET 12 <ONE/QUORUM> \" \")");
        System.out.println("PUT <key> <value> <consistency_level>	(Ex. PUT 12 'Hey there' <ONE/QUORUM>)");
        System.out.println("Note: The GET command needs a blank space after consistency_level for the <value> part.");
        System.out.println("-------------------------------------");
        input = sc.nextLine();

		System.out.println("Input: " + input);
		String[] split = input.split(" ");

		// Check if the input is empty.
		if (split.length != 0) {
			// Check if the input is a GET/PUT command, does it satisfy requirements?
			if (split[0].equalsIgnoreCase("get") && split.length != 3) {
				System.err.println("Invalid GET command!!");
                invalidCommand = true;
			} else if (split[0].equalsIgnoreCase("put") && split.length != 4) {
				System.err.println("Invalid PUT command!!");
                invalidCommand = true;
			} else {
                invalidCommand = false;
				// The input for either GET or PUT is valid, and we can proceed.
				if (split[0].equalsIgnoreCase("get") || split[0].equalsIgnoreCase("put")) {
					if (split[0].equalsIgnoreCase("put")) {
						String temp = split[1];
						key = Integer.parseInt(temp);
						value = split[2];
						consistency_level = split[3].toUpperCase();
						KeyValStore.Put.Builder put = KeyValStore.Put.newBuilder();
						put.setKey(key);
						put.setValue(value);
						put.setConsistencyLevel(consistency_level);
						keyValMessage.setPut(put.build());
					} else {
						key = Integer.parseInt(split[1]);
						consistency_level = split[2];
						KeyValStore.Get.Builder get = KeyValStore.Get.newBuilder();
						get.setKey(key);
						get.setConsistencyLevel(consistency_level);
						keyValMessage.setGet(get.build());
					}
				} else {
					System.err.println("Invalid operation!!");
					System.exit(-1);
				}
				keyValMessage.setConnectionWith(1);
				if (!tryConnect) {
					/* ignore */
					keyValMessage.build().writeDelimitedTo(coordinatorSocket.getOutputStream());
					coordinatorSocket.getOutputStream().flush();
					//System.out.println("COORDINATOR SOCKET (when client starts): " + coordinatorSocket);
					tryConnect = true;
				} else {
					if (coordinatorSocket != null) coordinatorSocket = null;
					socket = new Socket(coordinatorIP, coordinatorPort);
					//System.out.println("SOCKET (when client sends new message): " + socket);
					keyValMessage.build().writeDelimitedTo(socket.getOutputStream());
					socket.getOutputStream().flush();
				}
			}
		}
	}

	/**
	 * Handles coordinator responses.
	 */
	private static void handleResponse() {

		try {
			KeyValStore.KeyValMessage rMsg = null;
			KeyValStore.KeyValMessage responseMsg = null;
			KeyValStore.KeyValMessage responseMsg1 = null;
			
			if (coordinatorSocket != null) responseMsg = KeyValStore.KeyValMessage.parseDelimitedFrom(coordinatorSocket.getInputStream());
			if (socket != null) responseMsg1 = KeyValStore.KeyValMessage.parseDelimitedFrom(socket.getInputStream());
			if (responseMsg != null) rMsg = responseMsg;
			if (responseMsg1 != null) rMsg = responseMsg1;
			if (rMsg != null) {
				if (rMsg.hasResponse()) { // then how come it come ok wait
					KeyValStore.Response response = rMsg.getResponse();
                    //System.out.println("Response msg: " + rMsg);
                    //System.out.println("Response: " + response);
                    // blank response for get on quorum
					// TRUE FOR WRITE
					if (response.getIsReadOrWrite()) {
						if (response.getStatus()) {
							System.out.println("Key: " + response.getKey() + " status is: " + response.getStatus() + " ,while writing on the system with value: " + response.getValue());
						} else {
							System.out.println("FAILED: Key: " + response.getKey() + " status is: " + response.getStatus() + " ,while writing on the system with value: " + response.getValue());
						}
					}
					//FALSE FOR GET/READ
					else {
						if (response.getStatus()) {
							System.out.println("SUCCEEDED: Key: " + response.getKey() + " status is: " + response.getStatus() + " ,while reading from the system and the value is: " + response.getValue());
							System.out.println("Time while value was read: " + response.getTime());
						} else {
							System.out.println("FAILED: Key: " + response.getKey() + " status is: " + response.getStatus() + " ,while reading from the system and the value is: " + response.getValue());
							System.out.println("Time while value was read: " + response.getTime());
						}
					}
				}
				if (rMsg.hasException()) {
					KeyValStore.Exception exception = rMsg.getException();
					System.out.println("Exception by Key: " + exception.getKey() + " and the exception message is: " + exception.getExceptionMessage());
				}
			} else {
				System.err.println("Kindly check the connection and parsing.");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Client makes connection to the coordinator
	 *
	 * @param args
	 */
	private static void connectToCoordinator(String[] args) {

		// Argument0 will be the coordinatorName name
		coordinatorName = args[0];
		String filePath = args[1];

		//getting ip and port number of selected coordinatorName using the file given in the command line.
		getCoordinatorDetails(coordinatorName, filePath);

		// This is to create socket to connect to coordinatorName, if it is not connect and if it is already created, then simply using it.
		try {
			coordinatorSocket = new Socket(coordinatorIP, coordinatorPort);
			System.out.println(coordinatorSocket);
		} catch (Exception ex) {
            System.err.println("ERROR! Cannot start client as no servers running for coordinator assignment!!");
            System.exit(-1);
		}
	}

	/**
	 * This method is to get the details about the coordinatorName from coordinatorName name and the file.
	 *
	 * @param coordinator
	 * @param filePathIn
	 */
	private static void getCoordinatorDetails(String coordinator, String filePathIn) {

		FileProcessor fileProcessor = new FileProcessor(filePathIn);
		String coordinatorName;
		System.out.println(coordinator);
		while (fileProcessor.hasNextLine()) {
			FileLineToken fileLineToken = ProcessServerUtil.processLine(fileProcessor.readFileLine());
			if (fileLineToken != null) {
				coordinatorName = fileLineToken.getServerName();
				if (coordinator.equalsIgnoreCase(coordinatorName)) {
					coordinatorIP = fileLineToken.getServerIPAddress();
					coordinatorPort = fileLineToken.getServerPortNumber();
				}
			}
		}
		System.out.println("CoordIP: "+ coordinatorIP);
		System.out.println("CoordPort: "+coordinatorPort);
	}
}