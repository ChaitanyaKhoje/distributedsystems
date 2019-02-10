import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Branch {

    private int initialBalance;
    private String branchName;
    private String branchIPAddress;
    private int branchPortNumber;
    static int timeToSleep;
    public static List<Bank.InitBranch.Branch> listOfBranches = new ArrayList<Bank.InitBranch.Branch>();
    private ServerSocket branchSocket = null;

    public static Map<String, Socket> socketStore = new HashMap<String, Socket>();
    public static Bank.BranchMessage branchMsg = null;
    private int connections = 0;
    private int outGoingConnections = 0;
    private int snapshotID;
    private StateRecorder stateRecorder;
    private List<ChannelStateRecorder> channelStateRecorderList = new ArrayList<ChannelStateRecorder>();
    private Map<String, StateRecorder> recordingState = new HashMap<String, StateRecorder>();
    private ThreadController threadController;
    private Sender sender;
    private SnapshotHandler snapshotHandler;    // Declared on class level so that it can be used in Receiver for handling marker msgs

    public Branch() { }

    public static void main(String[] args) throws IOException, Exception {

        Branch branch = new Branch();
        Socket socket = null;
        branch.branchName = args[0];
        branch.branchPortNumber = Integer.parseInt(args[1]);

/*        branch.branchName = "branch4";
        branch.branchPortNumber = 9098;*/

        branch.branchIPAddress = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Branch Name: " + branch.branchName + "\r\n" + "Branch's IP address: " + branch.branchIPAddress);
        System.out.println("Branch's port: " + branch.branchPortNumber);
        timeToSleep = Integer.parseInt(args[2]);
        /*if (timeToSleep > 1000) {
            System.err.println("Command line argument for transaction frequency is very less, kindly provide value between 0-1000 ms.");
            System.exit(0);
        }*/
        try {
            branch.branchSocket = new ServerSocket(branch.branchPortNumber);
            //System.out.println("Branch created on port: " + branch.branchSocket.getLocalPort() + " and hostname: " + branch.branchSocket.getInetAddress().getLocalHost().getHostAddress());
        } catch (IOException e) {
            System.err.println("Branch creation on a port number failed, please try again.");
        } finally { }
        try {
            if (branch.branchSocket != null) socket = branch.branchSocket.accept();
            branchMsg = Bank.BranchMessage.parseDelimitedFrom(socket.getInputStream());
            if (branchMsg != null) {
                if (branchMsg.hasInitBranch()) {
                    branch.setInitialBalance(branchMsg.getInitBranch().getBalance());
                    branch.setListOfBranches(branchMsg.getInitBranch().getAllBranchesList());
                    branch.setConnections(branchMsg.getInitBranch().getAllBranchesCount() - 1);
                    //System.out.println("Check whether it is running correctly or not, initial balance " + branch.getInitialBalance() + "List of branches" + branch.getListOfBranches());
                }
                /*else if (branchMsg.hasInitSnapshot()) {
                    System.out.println("Controller wants to initialize snapshot");

                    int snapshotID = branchMsg.getInitSnapshot().getSnapshotId();
                    branch.setSnapshotID(snapshotID);
                    System.out.println("Starting snapshot number: " + snapshotID);

                    stateRecorder = new StateRecorder();

                    SnapshotHandler snapshotHandler = new SnapshotHandler(stateRecorder, branch);
                    Thread snapshotThread = new Thread(snapshotHandler);
                    snapshotThread.start();
                }*/
            }
        } catch (IOException e) {
            System.err.println("Cannot accept request!");
        } finally { }

        Thread[] threadForCommunication = new Thread[branchMsg.getInitBranch().getAllBranchesCount() - 1];

        try {
            connectToOtherBranches(branchMsg, branch, threadForCommunication);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //  Call for this anonymous method is made below this code.
        Thread incomingConnectionFromOtherBranchesThread = new Thread(new Runnable() {

            Socket receiveSocket = null;

            @Override
            public void run() {
                while (true) {
                    try {
                        //	incomingConnectionFromOtherBranches(branchMsg, branch,receiveSocket);
                        System.out.flush();
                        InputStreamReader inputReader = null;
                        receiveSocket = branch.branchSocket.accept();
                        //System.out.println("Welcome to incomingConnectionFromOtherBranches method");
                        //				for(int i=0; i<branchMsg.getInitBranch().getAllBranchesCount();i++) {
                        //	if((branch.getBranchName().compareTo(branchMsg.getInitBranch().getAllBranches(i).getName())<0)) { // iterating all and connecting to other branches only, expect itself.
                        if (branch.getConnections() != 0) {
                            //System.out.println("Just before socket accept");

                            inputReader = new InputStreamReader(receiveSocket.getInputStream());
                            //System.out.println("After input reader" + inputReader);
                            BufferedReader bufferedReader = new BufferedReader(inputReader);
                            String str = null;
                            try {
                                //System.out.println("Before read");
                                str = bufferedReader.readLine();
                                //System.out.println("After read" + str);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Throwable th) {
                                th.printStackTrace();
                            }

                            // Put in new map of String, Socket: str, receiveSocket
                            branch.socketStore.put(str, receiveSocket);
                            branch.popAConnection();
                            //System.out.println("Connected with branch: " + str + "With branch count " + branchMsg.getInitBranch().getAllBranchesCount());
                            System.out.flush();
                            int j = branch.getOutGoingConnections();
                            Receiver receiver = new Receiver(receiveSocket, branch, str);
                            threadForCommunication[j] = new Thread(receiver);
                            threadForCommunication[j].start();
                            branch.setOutGoingConnections(branch.getOutGoingConnections() + 1);
                        }
                        //			}

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        /**
         *  Takes in incoming connections being made through connectToOtherBranches().
         *  A new thread is created for this process.
         */
        incomingConnectionFromOtherBranchesThread.start();
        //System.out.println("End of receiving.");

        //  Sending money starts here.
        startSending(branch);
        Thread.sleep(500);

        // Initialize object of State Recorder class for this branch
        branch.setStateRecorder(new StateRecorder());
        // Initialize recordingStateMap for this branch
        branch.getRecordingState().put(branch.getBranchName(), branch.getStateRecorder());
        // Channel state list, if this works we dont need recording state map probably

        branch.setSnapshotHandler(new SnapshotHandler(branch.getStateRecorder(), branch, socket, branch.getThreadController()));
        Thread snapshotThread = new Thread(branch.getSnapshotHandler());
        snapshotThread.start();
    }

    /**
     * Makes connections to other branches depending on lexicographical ordering (branch 2 connects to branch 1).
     * This method works in the main thread.
     *
     * @param branchMsg
     * @param branch
     * @param threadForCommunication
     * @throws UnknownHostException
     * @throws IOException
     */
    public static void connectToOtherBranches(Bank.BranchMessage branchMsg, Branch branch, Thread[] threadForCommunication) throws UnknownHostException, IOException {
        //System.out.println("Welcome to connect method");
        for (int i = 0; i < branchMsg.getInitBranch().getAllBranchesCount(); i++) {
            if ((branch.getBranchName().compareTo(branchMsg.getInitBranch().getAllBranches(i).getName()) < 0)) { // iterating all and connecting to other branches only, expect itself.
                Socket socket = new Socket(branchMsg.getInitBranch().getAllBranches(i).getIp(), branchMsg.getInitBranch().getAllBranches(i).getPort());
                String senderBranchName = branch.getBranchName() + "\n";

                socket.setKeepAlive(true);

                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                dataOutputStream.writeBytes(senderBranchName);

                branch.popAConnection();
                dataOutputStream.flush();
                branch.socketStore.put(branchMsg.getInitBranch().getAllBranches(i).getName(), socket);
                //System.out.println("Inside connectMethod & count of all branches" + branchMsg.getInitBranch().getAllBranchesCount() + " Name of the other with whome we are connecting " + branchMsg.getInitBranch().getAllBranches(i) + " And socket is " + socket);
                int j = branch.getOutGoingConnections();
                Receiver receiver = new Receiver(socket, branch, branchMsg.getInitBranch().getAllBranches(i).getName());
                threadForCommunication[j] = new Thread(receiver);
                threadForCommunication[j].start();
                branch.setOutGoingConnections(branch.getOutGoingConnections() + 1);
                //System.out.println("End of the method.");
            }
        }
    }

    /**
     * This method deals with sending money to other branches on a random-money(in between 1% and 5%)
     * and random-branch-selection basis.
     *
     * @param branch
     */
    public static void startSending(Branch branch) {

        Sender sender = new Sender(branch);
        // We need this Sender class object so that we can pause it later while taking a snapshot
        // So saving it for later use on this particular instance of the branch class
        // Each branch instance will have its own instance of Sender.
        branch.setSender(sender);
        Thread sendingThread = new Thread(sender);
        branch.setThreadController(new ThreadController(sendingThread));
        if (branch.getConnections() == 0) {
            //System.out.println("Connection's Exhausted");
            System.out.flush();
        }
        sendingThread.start();
    }

    public Map<String, Socket> getSocketStore() {
        return socketStore;
    }

    public void setSocketStore(Map<String, Socket> socketStoreIn) {
        socketStore = socketStoreIn;
    }

    public synchronized void deduct(int removeBalanceAmount) { initialBalance = initialBalance - removeBalanceAmount; }

    public synchronized int getInitialBalance() {
        return initialBalance;
    }

    public synchronized void setInitialBalance(int initialBalanceIn) {
        initialBalance = initialBalanceIn;
    }

    public synchronized void addMoney(int amountToBeAdded) {
        setInitialBalance(getInitialBalance() + amountToBeAdded);
    }

    public int getConnections() {
        return connections;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    public synchronized void popAConnection() {
        this.setConnections(getConnections() - 1);
    }

    public synchronized int getOutGoingConnections() {
        return outGoingConnections;
    }

    public synchronized void setOutGoingConnections(int outGoingConnections) {
        this.outGoingConnections = outGoingConnections;
    }

    public synchronized String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchNameIn) {
        branchName = branchNameIn;
    }

    public int getBranchPortNumber() {
        return branchPortNumber;
    }

    public void setBranchPortNumber(int branchPortNumberIn) {
        branchPortNumber = branchPortNumberIn;
    }

    public String getBranchIPAddress() {
        return branchIPAddress;
    }

    public void setBranchIPAddress(String branchIPAddressIn) {
        branchIPAddress = branchIPAddressIn;
    }

    public List<Bank.InitBranch.Branch> getListOfBranches() {
        return listOfBranches;
    }

    public void setListOfBranches(List<Bank.InitBranch.Branch> listOfBranchesIn) {
        listOfBranches = listOfBranchesIn;
    }

    public synchronized int getSnapshotID() {
        return snapshotID;
    }

    public synchronized void setSnapshotID(int snapshotIDIn) {
        snapshotID = snapshotIDIn;
    }

    public synchronized StateRecorder getStateRecorder() {
        return stateRecorder;
    }

    public synchronized void setStateRecorder(StateRecorder snapshotDataIn) {
        stateRecorder = snapshotDataIn;
    }


    public synchronized Map<String, StateRecorder> getRecordingState() {
        return recordingState;
    }

    public synchronized void setRecordingState(Map<String, StateRecorder> recordingStateIn) {
        recordingState = recordingStateIn;
    }

    public synchronized Sender getSender() {
        return sender;
    }

    public synchronized void setSender(Sender senderIn) {
        sender = senderIn;
    }

    public SnapshotHandler getSnapshotHandler() {
        return snapshotHandler;
    }

    public void setSnapshotHandler(SnapshotHandler snapshotHandlerIn) {
        snapshotHandler = snapshotHandlerIn;
    }

    public synchronized List<ChannelStateRecorder> getChannelStateRecorderList() {
        return channelStateRecorderList;
    }

    public synchronized void setChannelStateRecorderList(List<ChannelStateRecorder> channelStateRecorderIn) {
        channelStateRecorderList = channelStateRecorderIn;
    }

    public synchronized ThreadController getThreadController() {
        return threadController;
    }

    public synchronized void setThreadController(ThreadController threadControllerIn) {
        threadController = threadControllerIn;
    }
}


