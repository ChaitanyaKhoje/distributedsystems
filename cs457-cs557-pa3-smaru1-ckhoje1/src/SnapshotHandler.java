import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotHandler implements Runnable {

	//private StateRecorder stateRecorder;
	private Branch branch;
	private Socket snapshotSocket;
	private Bank.BranchMessage branchMessage = null;
	private Map<String, Boolean> markerCheckMap = new HashMap<String, Boolean>();
	private List<Integer> currentSnapshotIDs = new ArrayList<Integer>();
	private static boolean pauseSender = false;
	private ThreadController threadController;
	public static Map<String, List<ChannelStateRecorder>> channelStateMap = new HashMap<String, List<ChannelStateRecorder>>();
	Bank.BranchMessage.Builder branchMsg = Bank.BranchMessage.newBuilder();
	Bank.ReturnSnapshot.Builder returnSnapshotObject = Bank.ReturnSnapshot.newBuilder();
	Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshotBuilder = Bank.ReturnSnapshot.LocalSnapshot.newBuilder();

	public SnapshotHandler(StateRecorder stateRecorderIn, Branch branchIn, Socket branchSocket, ThreadController threadControllerIn) {

		//stateRecorder = stateRecorderIn;
		branch = branchIn;
		snapshotSocket = branchSocket;
		threadController = threadControllerIn;
	}

	@Override
	public void run() {
		try {
			while (true) {
				//System.out.println("Socket: " + snapshotSocket.getInetAddress().getLocalHost().getHostAddress() + "port: " + snapshotSocket.getLocalPort());
				//Socket snapshotSocket = socket.accept();
				InputStream inputStream = snapshotSocket.getInputStream(); // controller to branch between
				if (snapshotSocket != null) {
					if (snapshotSocket.getInputStream() != null) {
						branchMessage = Bank.BranchMessage.parseDelimitedFrom(inputStream);
					} else {
						continue;
					}
				}
				if (branchMessage != null) {
					if (branchMessage.hasInitSnapshot()) {
						handleInitSnapshot();
					} else if (branchMessage.hasMarker()) {
						handleMarkerMessages();
					} else if (branchMessage.hasRetrieveSnapshot()) {
						Bank.BranchMessage.Builder returnSnapshotMessage = handleSnapshotRetrieval();
						if (returnSnapshotMessage != null) {
							// Send this populated branch message builder back to controller
							OutputStream outputStream = snapshotSocket.getOutputStream();
							returnSnapshotMessage.build().writeDelimitedTo(outputStream);
							outputStream.flush();
						} else {
							System.out.println();
						}
					} else {
						System.err.println("Invalid message from the controller");
						System.exit(1);
					}
					if (Controller.sameSnapshotID) continue;
				} else {
					continue;
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * HANDLE INIT SNAPSHOT:
	 * <p>
	 * Stops sender thread.
	 * Acquires lock.
	 * Records local state.
	 * Sends Marker to other branches.
	 * Releases lock.
	 * Resumes sender thread.
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public synchronized void handleInitSnapshot() throws InterruptedException, IOException {

		//TODO: Need to stop the sender thread here!
		// No need to create a new thread to stop the Sender thread,
		// let the current thread of SnapshotHandler pause the Sender from sending
		//setPauseSender(true);
		//controlSender("STOP");
		branch.getSender().setPause(true);

		// Check if we really need a semaphore/mutex here as we have synchronized methods internally.
		// We are creating a critical section below as some data structures are shared by multiple threads.
		//Semaphore semaphore = new Semaphore(1);
		// Acquiring lock
		//semaphore.acquire();

		int snapshotID = branchMessage.getInitSnapshot().getSnapshotId();
		if (getCurrentSnapshotIDs().contains(snapshotID)) {

			Bank.BranchMessage.Builder branchMsg = Bank.BranchMessage.newBuilder();
			Bank.ReturnSnapshot.Builder returnSnapshotObject = Bank.ReturnSnapshot.newBuilder();
			Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshotBuilder = Bank.ReturnSnapshot.LocalSnapshot.newBuilder();

			localSnapshotBuilder.setSnapshotId(snapshotID);

			returnSnapshotObject.setLocalSnapshot(localSnapshotBuilder);
			branchMsg.setReturnSnapshot(returnSnapshotObject);

			if (branchMsg != null) {
				// Send this populated branch message builder back to controller
				OutputStream outputStream = snapshotSocket.getOutputStream();
				branchMsg.build().writeDelimitedTo(outputStream);
				outputStream.flush();
			}
		}
		getCurrentSnapshotIDs().add(snapshotID);

		//System.out.println("Starting snapshot number: " + snapshotID);

		// Recording local state
		branch.setSnapshotID(snapshotID);
		branch.getStateRecorder().storeStates(branch);
		System.out.println("---------------------------------------------");
		System.out.println("Init-Snapshot for snapshot ID: " + snapshotID);
		System.out.println("Local state for: " + branch.getBranchName() + " is " + branch.getInitialBalance());
		System.out.println("---------------------------------------------");
		// Send marker messages to others
		sendMarkerMessages(branch.getStateRecorder().getSnapshotID(), false);

		//branch.getRecordingState().get(branch.getBranchName()).setRecording(true);
		// Releasing lock
		//semaphore.release();

		//TODO: Need to release the sender thread here!
		//setPauseSender(false);
		//controlSender("RESUME");
		branch.getSender().setPause(false);


		// Snapshot handler thread -> send Marker msgs to all branches....
		// Receiver notices one marker ... and calls handleMarkerMessages by using its own thread.
		// Receiver --> pause sender thread ()
	}

	/**
	 * HANDLE MARKER MESSAGES
	 *
	 * @throws InterruptedException
	 */
	public synchronized void handleMarkerMessages() throws InterruptedException {

		//TODO: Need to stop the sender thread here!
		//controlSender("STOP");
		//Semaphore semaphore = new Semaphore(1);
		//semaphore.acquire();
		//System.out.println("Inside handle marker message");
		int markerSnapshotID = branchMessage.getMarker().getSnapshotId();

		if (!getCurrentSnapshotIDs().contains(markerSnapshotID)) {
			branch.getSender().setPause(true);
			//setPauseSender(true);
			// This is the first marker message

			// Saving ID in list of snapshots
			getCurrentSnapshotIDs().add(markerSnapshotID);

			// Recording local state
			branch.setSnapshotID(markerSnapshotID);
			branch.getStateRecorder().storeStates(branch);
			System.out.println("---------------------------------------------");
			System.out.println("Handling marker messages for snapshot ID: " + markerSnapshotID);
			System.out.println("Local state for: " + branch.getBranchName() + " is " + branch.getInitialBalance());
			System.out.println("---------------------------------------------");
			// Send marker messages to others
			sendMarkerMessages(branch.getStateRecorder().getSnapshotID(), true);
			//setPauseSender(false);
			//branch.getRecordingState().get(branch.getBranchName()).setRecording(true);
			branch.getSender().setPause(false);
		} else {
			// This is a returning/reply marker message
			//System.out.println("----------- RETURN MARKER AT: " + branchMessage.getMarker().getDstBranch() + " SENT FROM: " + branchMessage.getMarker().getSrcBranch());

			String branchName = branchMessage.getMarker().getDstBranch();
			//System.out.println("CHANNEL STATE for: " + branchName + " is: " + channelStateMap.get(branchName));

			List<ChannelStateRecorder> results = channelStateMap.get(branchName);

			for(int i = 0;i < (branch.getListOfBranches().size()) * 2; i++) {
				localSnapshotBuilder.addChannelState(0);
			}

			int amount = 0;
			if (results != null && !results.isEmpty()) {
				//System.err.println("OUTPUT------------"+ Arrays.toString(results.toArray()));
				for (int i = 0; i < results.size(); i++) {
					//System.out.println("TRYING TO STOP RECORDING FOR: " + results.get(i).getListeningBranch());
					if (results.get(i).getMarkerReceiverBranch().equals(branchMessage.getMarker().getSrcBranch())
							&& results.get(i).isRecording()
							&& results.get(i).getSnapshotID() == branchMessage.getMarker().getSnapshotId()) {
						amount = results.get(i).getAmountReceivedOnListeningBranch();
						//System.out.println("----------- STOPPING RECORDING FOR: LB: " + results.get(i).getListeningBranch() + " MB: " + results.get(i).getMarkerReceiverBranch());
						results.get(i).setRecording(false);
						int temp = Integer.parseInt(results.get(i).getMarkerReceiverBranch().replaceAll("\\D+", ""));
						//System.out.println(" --------------ADD CHANNEL STATE : " + temp);
						//localSnapshotBuilder.addChannelState(0);
						//System.out.println(" --------------SET CHANNEL FOR: " + temp + " amount: " + amount);
						localSnapshotBuilder.setChannelState(temp, 0);
						//		localSnapshotBuilder.setChannelState(temp, amount);
					}
				}
				channelStateMap.put(branchName, results);
			}
			//localSnapshotBuilder.addChannelState(amount);
		}

		// Channel state recording
		//branch.getRecordingState().get(branch.getBranchName()).setRecording(false);

		// Releasing lock
		//semaphore.release();

		//TODO: Need to release the sender thread here!

		//notifyAll();
		//controlSender("RESUME");
	}

	/**
	 * HANDLE SNAPSHOT RETRIEVAL:
	 * <p>
	 * Builds and returns a branch's snapshot.
	 *
	 * @return Branch Message
	 */
	public Bank.BranchMessage.Builder handleSnapshotRetrieval() {

		int snapshotId = branchMessage.getRetrieveSnapshot().getSnapshotId();

		if (getCurrentSnapshotIDs().contains(snapshotId)) {
			// Creating Return-Snapshot object

			localSnapshotBuilder.setBalance(branch.getStateRecorder().getCurrentBalance());
			localSnapshotBuilder.setSnapshotId(branch.getStateRecorder().getSnapshotID());

			List<ChannelStateRecorder> cList = channelStateMap.get(branch.getBranchName());
			for(int i = 0;i < (branch.getListOfBranches().size()) * 2; i++) {
				localSnapshotBuilder.addChannelState(0);
			}

			if (cList != null && !cList.isEmpty()) {
				//System.out.println("LIST SIZE: " + cList.size());
				for (int i = 0; i < cList.size(); i++) {
					String listedBranch = cList.get(i).getListeningBranch();
					String markerB = cList.get(i).getListeningBranch();
					if (listedBranch.equals(branch.getBranchName())) {
						//System.out.println("MARKER BRANCH: " + markerB);
						int temp = Integer.parseInt(cList.get(i).getMarkerReceiverBranch().replaceAll("\\D+",""));
						//localSnapshotBuilder.setChannelState(temp, cList.get(i).getAmountReceivedOnListeningBranch());
					}
				}
			}

			returnSnapshotObject.setLocalSnapshot(localSnapshotBuilder);
			branchMsg.setReturnSnapshot(returnSnapshotObject);
			return branchMsg;
		} else {
			System.err.println("Invalid snapshot ID; snapshot for " + snapshotId + " was not recorded");
			return null;
		}
	}

	/**
	 * Sends marker messages to all other branches.
	 *
	 * @param snapshotID
	 */
	public void sendMarkerMessages(int snapshotID, boolean recordEmptyIn) {

		for(int i = 0; i < branch.getListOfBranches().size(); i++) {
			Bank.InitBranch.Branch targetBranch = branch.getListOfBranches().get(i);
			//for (Bank.InitBranch.Branch targetBranch : branch.getListOfBranches()) {
			//System.out.println("Target branch: " + targetBranch.getName());
			//System.out.println("Branch name: " + branch.getBranchName());
			if (!branch.getBranchName().equals(targetBranch.getName())
					&& !(branch.getBranchPortNumber() == (targetBranch.getPort()))) {

				//System.out.println("Sending marker message from " + branch.getBranchName() + " to " + targetBranch.getName());
				Bank.BranchMessage.Builder branchMsg = Bank.BranchMessage.newBuilder();
				Bank.Marker.Builder markerMsg = Bank.Marker.newBuilder();

				markerMsg.setSrcBranch(branch.getBranchName());
				markerMsg.setDstBranch(targetBranch.getName());
				markerMsg.setSnapshotId(snapshotID);
				//System.out.println("----------- SENDING MARKER FROM: " + branch.getBranchName() + " TO: " + targetBranch.getName());
				Socket targetBranchSocket = branch.getSocketStore().get(targetBranch.getName());
				branchMsg.setMarker(markerMsg);
				try {
					//Sending the marker message to the target branch socket's outputstream.
					branchMsg.build().writeDelimitedTo(targetBranchSocket.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
				// INSIDE FOR LOOP BECAUSE WE HAVE TO RECORD FROM ALL CHANNELS.
				// Start recording (FOR CHANNEL STATE RECORDING)
				// If the specific branch (key) on which we are going to record already exists in the map; condition:

				// Just update listening branch name, flag = true and marker receiver branch.
				// Amount received will be updated in Receiver class.

				// CHANNEL STATE MAP:
				// MapEntry -> { key: b1 -> value: [ {listeningBranch = b1, markerReceiver: b2, amount = 10, isRecord = true},
				//								 		{listeningBranch = b1, markerReceiver: b3, amount = 15, isRecord = true}
				//										{listeningBranch = b1, markerReceiver: b4, amount = 5, isRecord = true}
				// 								    ] }
				ChannelStateRecorder channelStateRecorder = new ChannelStateRecorder();
				boolean firstMarkerFlag = false;
				if (channelStateMap != null) {
					List<ChannelStateRecorder> tempList = channelStateMap.get(targetBranch.getName());
					if (tempList != null && !tempList.isEmpty()) {
						for (ChannelStateRecorder cs1 : tempList) {
							if (cs1.isFirst()) {
								firstMarkerFlag = true;
							}
						}
					}
				}
				if (recordEmptyIn && channelStateMap != null
						&& firstMarkerFlag) {
					channelStateRecorder.setListeningBranch(branch.getBranchName());
					//System.out.println("STARTING RECORDING FOR: RECORD EMPTY FLAG SET");
					//System.out.println("LISTENING Branch: " + channelStateRecorder.getListeningBranch());
					channelStateRecorder.setMarkerReceiverBranch(targetBranch.getName());
					//System.out.println("MARKER RECEIVING Branch: " + channelStateRecorder.getMarkerReceiverBranch());
					channelStateRecorder.setAmountReceivedOnListeningBranch(0);
					channelStateRecorder.setSnapshotID(snapshotID);
					//System.out.println("BOOLEAN: " + channelStateRecorder.isRecording());
					//System.out.println("SNAP: " + snapshotID);
					if (channelStateMap.containsKey(branch.getBranchName())) {                // Branch key exists in map?
						channelStateMap.get(branch.getBranchName()).add(channelStateRecorder);
					} else {
						branch.getChannelStateRecorderList().add(channelStateRecorder);
						channelStateMap.put(branch.getBranchName(), branch.getChannelStateRecorderList());
					}
				}
				if (channelStateMap != null && !recordEmptyIn && !firstMarkerFlag) {
					channelStateRecorder.setListeningBranch(branch.getBranchName());
					//System.out.println("STARTING RECORDING FOR: ");
					//System.out.println("LISTENING Branch: " + channelStateRecorder.getListeningBranch());
					channelStateRecorder.setMarkerReceiverBranch(targetBranch.getName());
					//System.out.println("MARKER RECEIVING Branch: " + channelStateRecorder.getMarkerReceiverBranch());
					//System.out.println("VALUE OF i4: " + i);
					if (0 == i && !channelStateRecorder.isFirst()) {
						channelStateRecorder.setFirst(true);
						//System.out.println("SETTING FIRST MARKER TRUE");
					} else {
						channelStateRecorder.setFirst(false);
						//System.out.println("SETTING FIRST MARKER FALSE");
					}
					channelStateRecorder.setRecording(true);
					channelStateRecorder.setSnapshotID(snapshotID);
					//System.out.println("BOOLEAN: " + channelStateRecorder.isRecording());
					//System.out.println("SNAP: " + snapshotID);

					if (channelStateMap.containsKey(branch.getBranchName())) {                // Branch key exists in map?
						channelStateMap.get(branch.getBranchName()).add(channelStateRecorder);
						//System.out.println("INSERTING INTO CHANNEL STATE MAP: " + branch.getBranchName() + ", ChannelStateRecorder: " + channelStateRecorder.getMarkerReceiverBranch());
					} else {
						branch.getChannelStateRecorderList().add(channelStateRecorder);
						channelStateMap.put(branch.getBranchName(), branch.getChannelStateRecorderList());
						//System.out.println("INSERTING INTO CHANNEL STATE MAP: " + branch.getBranchName() + ", ChannelStateRecorder: " + channelStateRecorder.getMarkerReceiverBranch());
					}
				}
			}
		}
		//System.out.println("CURRENT ENTRIES IN MAP FOR " + branch.getBranchName());
		/*for (String branch: channelStateMap.keySet()) {
			System.out.println("ENTRY: ----" + channelStateMap.get(branch).toString());
		}*/
	}

	/**
	 * Stops or resumes Sender thread depending upon operation specified.
	 *
	 * @param operation
	 */
	public void controlSender(String operation) {

		if (operation.equals("STOP")) {
			// TODO: Need to verify if the thread controller code actually stops the thread
			threadController.stopSender();
		} else if (operation.equals("RESUME")) {
			threadController.resumeSender();
		}
	}

	public synchronized List<Integer> getCurrentSnapshotIDs() {
		return currentSnapshotIDs;
	}

	public synchronized void setCurrentSnapshotIDs(List<Integer> currentSnapshotIDsIn) {
		currentSnapshotIDs = currentSnapshotIDsIn;
	}

	public synchronized static boolean isPauseSender() {
		return pauseSender;
	}

	public synchronized static void setPauseSender(boolean pauseSenderIn) {
		pauseSender = pauseSenderIn;
	}

	public ThreadController getThreadController() {
		return threadController;
	}

	public void setThreadController(ThreadController threadControllerIn) {
		threadController = threadControllerIn;
	}

	public Branch getBranch() {
		return branch;
	}

	public void setBranch(Branch branchIn) {
		branch = branchIn;
	}

	public Bank.BranchMessage getBranchMessage() {
		return branchMessage;
	}

	public void setBranchMessage(Bank.BranchMessage branchMessageIn) {
		branchMessage = branchMessageIn;
	}
}

