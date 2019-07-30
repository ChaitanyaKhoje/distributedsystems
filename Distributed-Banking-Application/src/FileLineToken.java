
public class FileLineToken {
	private String branchName;
	private String branchIPAddress;
	private int branchPortNumber;
	
	public FileLineToken(String branchNameIn, String branchIPAddressIn, int branchPortNumberIn) {
		
		setBranchName(branchNameIn);
		setBranchIPAddress(branchIPAddressIn);
		setBranchPortNumber(branchPortNumberIn);
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public String getBranchIPAddress() {
		return branchIPAddress;
	}

	public void setBranchIPAddress(String branchIPAddress) {
		this.branchIPAddress = branchIPAddress;
	}

	public int getBranchPortNumber() {
		return branchPortNumber;
	}

	public void setBranchPortNumber(int branchPortNumber) {
		this.branchPortNumber = branchPortNumber;
	}

}
