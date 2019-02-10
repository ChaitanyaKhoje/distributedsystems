import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class Receiver implements Runnable {

    private Socket socket;
    private Branch branch;
    private String string;
    private int amountToBeAdded = 0;

    public Receiver(Socket socketIn, Branch branchIn, String stringIn) {

        setBranch(branchIn);
        setSocket(socketIn);
        setString(stringIn);
    }

    @Override
    public void run() {

        InputStream inputStream = null;
        //int temp = 0;
        while (true) {
            //if (temp == 100) break;
            try {
                Random r = new Random();
                int i = r.nextInt(5) + 1;
                while (i < 5) {
                    //Thread.sleep(5000);
                    i++;
                }
                inputStream = getSocket().getInputStream();
                Bank.BranchMessage branchMsg = Bank.BranchMessage.parseDelimitedFrom(inputStream);
                receiveAtomically(branchMsg);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            //temp++;
        }
    }

    public synchronized void receiveAtomically(Bank.BranchMessage branchMsg) throws InterruptedException {

        if (branchMsg != null) {
            if (branchMsg.hasTransfer()) {
                int amount = branchMsg.getTransfer().getMoney();
                setAmountToBeAdded(amount);
                branch.addMoney(getAmountToBeAdded());
                if (Branch.timeToSleep >= 1000) {
                    System.out.println("Received: " + amount + " from " + branchMsg.getTransfer().getSrcBranch());
                    System.out.println("Updated " + branch.getBranchName() + "'s balance to " + branch.getInitialBalance());
                }
                // Check flag for recording set in SnapshotHandler after sending marker msgs (recordingState map)
                // So, IF recording is active, update branch's StateRecorder object.

                String src = branchMsg.getTransfer().getSrcBranch();
                //System.out.println("Source branch: " + src);
                String des = branchMsg.getTransfer().getDstBranch();
                //System.out.println("Destination branch: " + des);

                // GET SOURCE BRANCH FROM MAP OF CHANNEL STATE RECORDER
                // Is the channel state map not empty?
                if (!SnapshotHandler.channelStateMap.isEmpty()) {
                    // Is the corresponding des branch recording?
                    if (SnapshotHandler.channelStateMap.containsKey(des)) {
                        List<ChannelStateRecorder> cList = SnapshotHandler.channelStateMap.get(des);
                        for (ChannelStateRecorder csr : cList) {
                            //System.out.println("----------TRYING TO SAVE CS: " + csr.getListeningBranch() + " MB: " + csr.getMarkerReceiverBranch() + " RECORDING?: " + csr.isRecording());
                            if (csr.isRecording() && csr.getListeningBranch().equals(des) && csr.getMarkerReceiverBranch().equals(src)) {
                                //System.out.println("---------- SAVING CHANNEL STATE: LB: " + csr.getListeningBranch() + " MB: " + csr.getMarkerReceiverBranch());
                                //System.out.println("-------- SNAPSHOT ID: " + csr.getSnapshotID());
                                //System.out.println("AMOUNT BEFORE UPDATING: " + csr.getAmountReceivedOnListeningBranch());
                                csr.setAmountReceivedOnListeningBranch(amount);
                                //System.out.println("UPDATED AMOUNT IN CHANNEL: " + csr.getAmountReceivedOnListeningBranch());
                                SnapshotHandler.channelStateMap.put(des, cList);
                                break;
                            }
                        }
                    }
                }
            }

            // BELOW CODE IS FOR HANDLING MARKER MESSAGES
            // A branch when receives Init Snapshot message from the controller, it saves its local state and sends out
            // marker messages to all other branches, these markers are caught HERE and NOT on SnapshotHandler socket,
            // hence we set the same branch's SnapshotHandler object's fields and call handleMarkerMessages to handle reply markers.
            if (branchMsg.hasMarker()) {
                branch.getSender().setPause(true);
                SnapshotHandler snapshotHandler = branch.getSnapshotHandler();
                snapshotHandler.setBranch(branch);
                snapshotHandler.setBranchMessage(branchMsg);
                snapshotHandler.handleMarkerMessages();
                branch.getSender().setPause(false);
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public int getAmountToBeAdded() {
        return amountToBeAdded;
    }

    public void setAmountToBeAdded(int amountToBeAdded) {
        this.amountToBeAdded = amountToBeAdded;
    }
}
