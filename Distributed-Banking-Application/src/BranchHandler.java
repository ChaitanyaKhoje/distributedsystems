import java.net.Socket;

public class BranchHandler implements Runnable {

    private Socket branchSocket = null;
    private Branch branch;
    private Bank.BranchMessage branchMsg = null;

    public BranchHandler(Socket branchSocketIn, Branch branchIn, Bank.BranchMessage branchMsgIn) {

        setBranchSocket(branchSocketIn);
        setBranch(branchIn);
        setBranchMsg(branchMsgIn);
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        System.out.println("DEBUG: Inside Handler for branch: " + branch.getBranchName());
        System.out.println("Before parsing the input.");

        if (branchMsg.hasInitBranch()) {
            branch.setInitialBalance(branchMsg.getInitBranch().getBalance());
            branch.setListOfBranches(branchMsg.getInitBranch().getAllBranchesList());
            for (int i = 0; i < branchMsg.getInitBranch().getAllBranchesCount(); i++) {
                if (branch.getBranchName().equalsIgnoreCase(branchMsg.getInitBranch().getAllBranches(i).getName()) && branch.getBranchIPAddress().equalsIgnoreCase(branchMsg.getInitBranch().getAllBranches(i).getIp())
                        && branch.getBranchPortNumber() == branchMsg.getInitBranch().getAllBranches(i).getPort()) {
                    System.out.println("Inside the if condition");
                    break; // for now only.
                }
            }
            System.out.println("Check whether it is running correctly or not, initial balance " + branch.getInitialBalance() + "List of branches" + branch.getListOfBranches());
        }
    }

    public Socket getBranchSocket() {
        return branchSocket;
    }

    public void setBranchSocket(Socket branchSocket) {
        this.branchSocket = branchSocket;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Bank.BranchMessage getBranchMsg() {
        return branchMsg;
    }

    public void setBranchMsg(Bank.BranchMessage branchMsg) {
        this.branchMsg = branchMsg;
    }
}
