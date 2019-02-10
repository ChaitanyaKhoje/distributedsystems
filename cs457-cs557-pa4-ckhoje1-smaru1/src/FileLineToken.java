
public class FileLineToken {
	private String serverName;
	private String serverIPAddress;
	private int serverPortNumber;
	
	public FileLineToken(String branchNameIn, String branchIPAddressIn, int branchPortNumberIn) {
		
		setServerName(branchNameIn);
		setServerIPAddress(branchIPAddressIn);
		setServerPortNumber(branchPortNumberIn);
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerIPAddress() {
		return serverIPAddress;
	}

	public void setServerIPAddress(String serverIPAddress) {
		this.serverIPAddress = serverIPAddress;
	}

	public int getServerPortNumber() {
		return serverPortNumber;
	}

	public void setServerPortNumber(int serverPortNumber) {
		this.serverPortNumber = serverPortNumber;
	}

}
