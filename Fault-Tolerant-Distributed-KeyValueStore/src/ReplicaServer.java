
public class ReplicaServer {
	private String replicaServerName;
	private String replicaServerId;
	private int replicaServerPortNumber;
	private int max = 1000;
	private int min = 999;
//getter and setter

	public ReplicaServer() {
	}

	public ReplicaServer(String replicaServerNameIn, String replicaServerIdIn, int replicaServerPortNumberIn) {
		setReplicaServerName(replicaServerNameIn);
		setReplicaServerId(replicaServerIdIn);
		setReplicaServerPortNumber(replicaServerPortNumberIn);
	}

	public int getMax() { return max; }

	public void setMax(int max) { this.max = max; }

	public int getMin() { return min; }

	public void setMin(int min) { this.min = min; }

	public String getReplicaServerName() {
		return replicaServerName;
	}

	public void setReplicaServerName(String replicaServerName) {
		this.replicaServerName = replicaServerName;
	}

	public String getReplicaServerId() {
		return replicaServerId;
	}

	public void setReplicaServerId(String replicaServerId) {
		this.replicaServerId = replicaServerId;
	}

	public int getReplicaServerPortNumber() {
		return replicaServerPortNumber;
	}

	public void setReplicaServerPortNumber(int replicaServerPortNumber) {
		this.replicaServerPortNumber = replicaServerPortNumber;
	}
}
