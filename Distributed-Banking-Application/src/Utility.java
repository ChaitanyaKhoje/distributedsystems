import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Utility {

    private Branch branch;
    private static List<Bank.InitBranch.Branch> branchListTemp = new ArrayList<Bank.InitBranch.Branch>();

    public Utility() { }

    public Utility(Branch branchIn) {

        branch = branchIn;
    }

    public Utility(List<Bank.InitBranch.Branch> bList) {

        branchListTemp = bList;
    }

    public synchronized Bank.InitBranch.Branch getRandomBranch() {

        Random randomizer = new Random();
        Bank.InitBranch.Branch targetBranch;

        while(true) {
            if (branch != null) {
                targetBranch = branch.getListOfBranches().get(randomizer.nextInt(branch.getListOfBranches().size()));
                if (!branch.getBranchName().equals(targetBranch.getName())) {
                    break;
                }
            } else {
                targetBranch = branchListTemp.get(randomizer.nextInt(branchListTemp.size()));
                if (targetBranch != null) {
                    break;
                }
            }
        }
        return targetBranch;
    }

    public synchronized int getNewSnapshotID(int oldID) {

        return oldID + 1;
    }
}
