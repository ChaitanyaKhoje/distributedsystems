public class ThreadController {

    private Thread senderThread;

    public ThreadController(Thread senderIn) {

        senderThread = senderIn;
    }

    /**
     * Makes the current branch's Sender instance thread wait
     */
    public void stopSender() {

        if (senderThread.isAlive()) {
            synchronized (senderThread) {
                try {
                    System.err.println("----------STOPPING sender thread here!");
                    senderThread.wait();
                } catch (InterruptedException e) {
                    System.err.println("Cannot stop sender!!");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Resumes current branch's Sender instance thread
     */
    public void resumeSender() {

        if (senderThread.isAlive()) {
            //SnapshotHandler.setPauseSender(false);
            System.out.println("----------Resuming sender thread here!");
            synchronized (senderThread) {
                senderThread.notify();
            }
        }
    }
}
