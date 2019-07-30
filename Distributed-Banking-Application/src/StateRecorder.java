import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class StateRecorder {

    private int snapshotID;
    private int currentBalance;
    private int counter = 0;
    private boolean isRecording = false;
    private Map<String, Integer> channelStateMap = new HashMap<String, Integer>();

    public void storeStates(Branch branch) {

        //Semaphore semaphore = new Semaphore(1);
        //try {
            //semaphore.acquire();
            System.out.println("Recording snapshot data for: " + branch.getBranchName());
            setSnapshotID(branch.getSnapshotID());
            setCurrentBalance(branch.getInitialBalance());
            //semaphore.release();
        /*} catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //TODO: Release semaphore/mutex in finally block or in try block itself?
        }*/
    }

    public void listen(Branch branchIn) {

        Semaphore semaphore = new Semaphore(1);

        try {
            semaphore.acquire();
            System.out.println("Listening on incoming channel for: " + branchIn.getBranchName());
            setCurrentBalance(branchIn.getInitialBalance());
            setCounter(getCounter() + 1);
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized int getSnapshotID() {
        return snapshotID;
    }

    public synchronized void setSnapshotID(int snapshotIDIn) {
        snapshotID = snapshotIDIn;
    }

    public synchronized int getCurrentBalance() {
        return currentBalance;
    }

    public synchronized void setCurrentBalance(int currentBalanceIn) {
        currentBalance = currentBalanceIn;
    }

    public synchronized boolean isRecording() { return isRecording; }

    public synchronized void setRecording(boolean recording) { isRecording = recording; }

    public synchronized int getCounter() { return counter; }

    public synchronized void setCounter(int counterIn) { counter = counterIn; }

    public Map<String, Integer> getChannelStateMap() {
        return channelStateMap;
    }

    public void setChannelStateMap(Map<String, Integer> channelStateIn) {
        channelStateMap = channelStateIn;
    }
}
