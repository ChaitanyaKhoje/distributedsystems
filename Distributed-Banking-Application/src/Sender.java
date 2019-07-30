import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class Sender implements Runnable{

	private Branch branch;
	private boolean pause = false;

	public Sender() {}

	public Sender(Branch branchIn) {

		branch = branchIn;
	}

	/**
	 *  A new thread to send money to a random branch
	 */
	@Override
    public void run() {

        while (true) {
            try {
                Thread.sleep(Branch.timeToSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendMoney();
        }
    }

	public synchronized void sendMoney() {

		int money = calculateMoney();
        // If balance in a branch is 0, we can stop sending, but the outer loop inside run() remains alive.
		if (money != 0) {
            branch.deduct(money);
            if (Branch.timeToSleep >= 1000) {
                System.out.println("Deducted: " + money + " from " + branch.getBranchName());
                System.out.println("Updated " + branch.getBranchName() + "'s balance to " + branch.getInitialBalance());
            }
            Utility utility = new Utility(branch);
            Bank.InitBranch.Branch targetBranch = utility.getRandomBranch();
            Bank.Transfer.Builder transfer = Bank.Transfer.newBuilder();
            Bank.BranchMessage.Builder branchMsg = Bank.BranchMessage.newBuilder();

            transfer.setMoney(money);
            transfer.setSrcBranch(branch.getBranchName());
            transfer.setDstBranch(targetBranch.getName());
            branchMsg.setTransfer(transfer);

            // Connect to the random branch we just fetched.
            Socket targetSocket = branch.getSocketStore().get(targetBranch.getName());
            //System.out.println("Target socket: " + targetSocket);
            //System.out.println("Target branch: " + targetBranch.getName());
            OutputStream outputStream;
            if (targetSocket != null) {
                try {
                    outputStream = targetSocket.getOutputStream();
                    branchMsg.build().writeDelimitedTo(outputStream);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}

	public synchronized int calculateMoney() {

		int balance = branch.getInitialBalance();
        //System.out.println("DEBUG: Balance: " + balance);
		int money = getRandomNumberInRange(balance);
        //System.out.println("DEBUG: Money: " + money);
		return money;
	}

	private synchronized int getRandomNumberInRange(int balance) {

        int temp=0;
        Random r = new Random();
        temp = 1 + r.nextInt(5);
        temp = (temp*balance)/100;
        return temp;
	}

    public boolean isPause() {
        return pause;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }
}
