import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {

    private static List<Bank.InitBranch.Branch> branchList = new ArrayList<Bank.InitBranch.Branch>();
    private static List<Bank.InitBranch.Branch.Builder> branchBuilderList = new ArrayList<Bank.InitBranch.Branch.Builder>();
    public static Map<String, Socket> socketList = new HashMap<String, Socket>();
    private static int totalBranches = 0;
    private static int initialAmountForBranches = 0;
    private static List<Integer> ChannelsOfEachBranch = null;
    public static boolean sameSnapshotID = false;
    static StringBuilder globalSnapShot = new StringBuilder();
    
    static Bank.BranchMessage.Builder branchMsg = Bank.BranchMessage.newBuilder();
    static Bank.InitBranch.Builder initBranch = Bank.InitBranch.newBuilder();
    static Bank.InitBranch.Branch.Builder branch = Bank.InitBranch.Branch.newBuilder();
    static Bank.ReturnSnapshot.Builder returnSnapshot = Bank.ReturnSnapshot.newBuilder();

    /**
     *  CONTROLLER MAIN METHOD:
     *
     *  Initializes all branches.
     *  Takes Snapshots of the system.
     *
     * @param args
     */
    public static void main(String[] args) {

        processBranchFile(args);

        try {
            initializeBranches();
            initializeSnapshot();
        } catch (IOException | InterruptedException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Reads the branches.txt file and adds it to InitBranch Builder.
     *
     * @param args
     */
    public static void processBranchFile(String[] args) {

        int totalAmount = Integer.parseInt(args[0]); // Need to make sure that this amount gets divided based on # of branches in branches.txt
        //System.out.println("total Branches " + totalBranches + " & InitialAmountForEachBranch" + initialAmountForBranches);

        FileProcessor fileProcessor = new FileProcessor(args[1]);
        String branchName;
        String branchIPAddress;
        int branchPortNumber;

        while (fileProcessor.hasNextLine()) {
            FileLineToken fileLineToken = ProcessBranchUtil.processLine(fileProcessor.readFileLine());
            if (fileLineToken != null) {
                branchName = fileLineToken.getBranchName();
                branchIPAddress = fileLineToken.getBranchIPAddress();
                branchPortNumber = fileLineToken.getBranchPortNumber();
                //	Bank.InitBranch.Branch.Builder branch = Bank.InitBranch.Branch.newBuilder();

                branch.setName(branchName);
                branch.setIp(branchIPAddress);
                branch.setPort(branchPortNumber);
                //System.out.println(branch);

                branchBuilderList.add(branch);

                initBranch.addAllBranches(branch);
            }
        }

        totalBranches = branchBuilderList.size();
        initialAmountForBranches = totalAmount / totalBranches;
    }

    /**
     * Initializes all the branches by sending them the initBranch message.
     *
     * @throws IOException
     */
    private static void initializeBranches() throws IOException, InterruptedException {

        //Sending init msg to all the branches.
        for (int i = 0; i < initBranch.getAllBranchesCount(); i++) {
            initBranch.setBalance(initialAmountForBranches);
            branchMsg.setInitBranch(initBranch);
            //System.out.println("NAME: " + initBranch.getAllBranches(i).getName() + " IP " + initBranch.getAllBranches(i).getIp() + " PORT " + initBranch.getAllBranches(i).getPort());
            Socket socket = new Socket(initBranch.getAllBranches(i).getIp(), initBranch.getAllBranches(i).getPort());
            socketList.put(initBranch.getAllBranches(i).getName(), socket);
            OutputStream outputStream = socket.getOutputStream();
            branchMsg.build().writeDelimitedTo(outputStream);
            outputStream.flush();
        }
    }

    /**
     * Sends initSnapshot message to an arbitrarily chosen branch.
     *
     * @throws IOException
     */
    private static void initializeSnapshot() throws IOException, InterruptedException {

        Thread.sleep(10000);
        setBranchList(branchMsg.getInitBranch().getAllBranchesList());
        Utility utility = new Utility(branchList);
        int newSnapshotID = 0;

        while (true) {
            Thread.sleep(2000);
            globalSnapShot.setLength(0);

            // This flag is used in SnapshotHandler
            sameSnapshotID = false;
            // Generating new snapshot ID
            newSnapshotID = utility.getNewSnapshotID(newSnapshotID);
            //if (newSnapshotID == 15) break;
            // Sending Init Snapshot
            //System.out.println("------------Sending for snapshot ID: " + newSnapshotID);
            sendInitSnapshotMessage(newSnapshotID, utility);

            //TODO: Hypothetically what if the marker messages take more time than the thread sleep time below.
            Thread.sleep(3000);

            // Sending Retrieve-Snapshot message before sending the next Init-Snapshot message
            retrieveSnapshots(newSnapshotID);

            //System.out.println("After retrieving, snapshot id: "+ newSnapshotID);
            System.getProperty("line.separator");
            globalSnapShot.setLength(globalSnapShot.length() - 3);
            System.out.println(globalSnapShot);
        }
    }

    /**
     * Iterates and retrieves snapshots from all the branches depending upon the snapshot ID specified
     * and combines them into a global snapshot.
     *
     * @param snapshotID
     * @throws IOException
     * @throws InterruptedException
     */
    private static void retrieveSnapshots(int snapshotID) throws IOException, InterruptedException {

        int balance = 0;
        int returnSnapshotId = 0;

        globalSnapShot.append("snapshot_id: " + snapshotID + "\n");
        int totalbal = 0;
        // Sending Retrieve-Snapshot message to all branches

        for (int i = 0; i < branchList.size(); i++) {
            Bank.InitBranch.Branch targetBranch = branchList.get(i);
            Bank.RetrieveSnapshot.Builder retrieveSnapshotBuilder = Bank.RetrieveSnapshot.newBuilder();
            retrieveSnapshotBuilder.setSnapshotId(snapshotID);
            branchMsg.setRetrieveSnapshot(retrieveSnapshotBuilder);
            Socket socket = socketList.get(targetBranch.getName());
            OutputStream outputStream = socket.getOutputStream();
            branchMsg.build().writeDelimitedTo(outputStream);
            outputStream.flush();

            //Collecting local states from all the branches.
            Thread.sleep(1000);
            InputStream inputStream = socket.getInputStream();
            Bank.BranchMessage branchMessageForReturnSnapshot = Bank.BranchMessage.parseDelimitedFrom(inputStream);
            Bank.ReturnSnapshot returnSnapshot = branchMessageForReturnSnapshot.getReturnSnapshot();
            Bank.ReturnSnapshot.LocalSnapshot localSnapshot = returnSnapshot.getLocalSnapshot();
            balance = branchMessageForReturnSnapshot.getReturnSnapshot().getLocalSnapshot().getBalance();
            returnSnapshotId = branchMessageForReturnSnapshot.getReturnSnapshot().getLocalSnapshot().getSnapshotId();

            if (branchMessageForReturnSnapshot != null) {
                globalSnapShot.append(targetBranch.getName() + ": " + balance + ", ");
                totalbal = totalbal + balance;
                int count = localSnapshot.getChannelStateCount();

                List<Integer> channelStateList = localSnapshot.getChannelStateList();
                //System.out.println("Channel state count: " + localSnapshot.getChannelStateList());
                for (int j = 0; j < branchList.size(); j++) {
                    Bank.InitBranch.Branch branch = branchList.get(j);
                    if (!branch.getName().equals(targetBranch.getName())) {
                        int temp = Integer.parseInt(branch.getName().replaceAll("\\D+", ""));
                        globalSnapShot.append(branch.getName() + "->" + targetBranch.getName() + ": " + channelStateList.get(temp));
                        if (j + 1 < branchList.size()) globalSnapShot.append(", ");
                    }
                }
                globalSnapShot.append("\n");
            }
        }
        //globalSnapShot.append(totalbal);
    }

    /**
     * Helper method for initSnapshot
     *
     * @param newSnapshotID
     * @param utility
     * @throws IOException
     */
    private static void sendInitSnapshotMessage(int newSnapshotID, Utility utility) throws IOException {

        // Sending Init-Snapshot message
        Bank.InitBranch.Branch randomBranch = utility.getRandomBranch();
        Bank.InitSnapshot.Builder initSnapshotBuilder = Bank.InitSnapshot.newBuilder();
        initSnapshotBuilder.setSnapshotId(newSnapshotID);
        branchMsg.setInitSnapshot(initSnapshotBuilder);
        Socket socket = socketList.get(randomBranch.getName());
        OutputStream outputStream = socket.getOutputStream();
        branchMsg.build().writeDelimitedTo(outputStream);
        outputStream.flush();
    }

    public static List<Bank.InitBranch.Branch> getBranchList() {

        return branchList;
    }

    public static void setBranchList(List<Bank.InitBranch.Branch> branchList) {

        Controller.branchList = branchList;
    }

    public static int getTotalBranches() {

        return totalBranches;
    }

    public static void setTotalBranches(int totalBranches) {

        Controller.totalBranches = totalBranches;
    }

    public static int getInitialAmountForBranches() {

        return initialAmountForBranches;
    }

    public static void setInitialAmountForBranches(int initialAmountForBranches) {

        Controller.initialAmountForBranches = initialAmountForBranches;
    }
}
