public class ChannelStateRecorder {

    private String listeningBranch;                     // Branch for which we are recording
    private boolean isRecording = false;
    private int amountReceivedOnListeningBranch;
    private String markerReceiverBranch;                // From which our listeningBranch is waiting for a reply marker
    private boolean isFirst = false;
    private int snapshotID;

    // Example: two branches;
    // B1 init marker to B2 (start recording for b1)
    // B2 handle marker and back to B1 (stop recording for b1)

    public ChannelStateRecorder() { }

    public ChannelStateRecorder(String listeningBranchIn, boolean isRecordingIn, int amountReceivedOnListeningBranchIn, String markerReceiverBranchIn) {

        listeningBranch = listeningBranchIn;
        isRecording = isRecordingIn;
        amountReceivedOnListeningBranch = amountReceivedOnListeningBranchIn;
        markerReceiverBranch = markerReceiverBranchIn;
    }

    public synchronized String getListeningBranch() {
        return listeningBranch;
    }

    public synchronized void setListeningBranch(String listeningBranch) {
        this.listeningBranch = listeningBranch;
    }

    public synchronized boolean isRecording() {
        return isRecording;
    }

    public synchronized void setRecording(boolean recording) {
        isRecording = recording;
    }

    public synchronized int getAmountReceivedOnListeningBranch() {
        return amountReceivedOnListeningBranch;
    }

    public synchronized void setAmountReceivedOnListeningBranch(int amountReceivedOnListeningBranch) {
        this.amountReceivedOnListeningBranch = amountReceivedOnListeningBranch;
    }

    public synchronized String getMarkerReceiverBranch() {
        return markerReceiverBranch;
    }

    public synchronized void setMarkerReceiverBranch(String markerReceiverBranch) {
        this.markerReceiverBranch = markerReceiverBranch;
    }

    public synchronized int getSnapshotID() {
        return snapshotID;
    }

    public synchronized void setSnapshotID(int snapshotIDIn) {
        snapshotID = snapshotIDIn;
    }

    public synchronized boolean isFirst() {
        return isFirst;
    }

    public synchronized void setFirst(boolean first) {
        isFirst = first;
    }

    @Override
    public String toString() {
        return "ChannelStateRecorder{" +
                "listeningBranch='" + listeningBranch + '\'' +
                ", isRecording=" + isRecording +
                ", amountReceivedOnListeningBranch=" + amountReceivedOnListeningBranch +
                ", markerReceiverBranch='" + markerReceiverBranch + '\'' +
                ", snapshotID=" + snapshotID +
                '}';
    }
}
