import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerContext {

    private String nodeName;
    private int nodePort;
    private String nodeIP;
    private boolean isCoordinator = false;
    private int repairCategory = 0;         // 1: Hinted-Handoff, 2: ReadRepair
    private Socket selfSocket = null;
    boolean isFilePresent = false;
    private ReplicaServer replicas = null;
    private List<ReplicaServer> replicaServers = new ArrayList<ReplicaServer>(); // This will have replicas for the current server, which we are going to populate from the input file.
    
    private KVStore kvStore = new KVStore();
    private Map<Integer, KVStore> StorageForData = new HashMap<Integer, KVStore>();
    private Map<String, Boolean> failedServers = new HashMap<String, Boolean>();    // Used to store the failure states of servers
    private Map<String, KeyValStore.Hint> hints = new HashMap<String, KeyValStore.Hint>();

    private Map<ReplicaServer, List<ReplicaServer>> clusterOfServer = new HashMap<ReplicaServer, List<ReplicaServer>>();

    public ServerContext() { }

    public Map<Integer, KVStore> getStorageForData() {
		return StorageForData;
	}

	public void setStorageForData(Map<Integer, KVStore> storageForData) {
		StorageForData = storageForData;
	}

    public String getNodeName() { return nodeName; }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public String getNodeIP() {
        return nodeIP;
    }

    public void setNodeIP(String nodeIP) {
        this.nodeIP = nodeIP;
    }

    public ReplicaServer getReplicas() {
        return replicas;
    }

    public void setReplicas(ReplicaServer replicas) {
        this.replicas = replicas;
    }

    public List<ReplicaServer> getReplicaServers() {
        return replicaServers;
    }

    public void setReplicaServers(List<ReplicaServer> replicaServers) {
        this.replicaServers = replicaServers;
    }

    public KVStore getKVStore() {
        return kvStore;
    }

    public void setKVStore(KVStore keyValueStore) {
        this.kvStore = keyValueStore;
    }

	public Map<ReplicaServer, List<ReplicaServer>> getClusterOfServer() {
		return clusterOfServer;
	}

	public void setClusterOfServer(Map<ReplicaServer, List<ReplicaServer>> clusterOfServer) {
		this.clusterOfServer = clusterOfServer;
	}

    public boolean isCoordinator() {
        return isCoordinator;
    }

    public void setCoordinator(boolean coordinator) {
        isCoordinator = coordinator;
    }

    public Socket getSelfSocket() {
        return selfSocket;
    }

    public void setSelfSocket(Socket selfSocket) {
        this.selfSocket = selfSocket;
    }

    public Map<String, Boolean> getFailedServers() {
        return failedServers;
    }

    public void setFailedServers(Map<String, Boolean> failedServers) {
        this.failedServers = failedServers;
    }

    public Map<String, KeyValStore.Hint> getHints() {
        return hints;
    }

    public void setHints(Map<String, KeyValStore.Hint> hints) {
        this.hints = hints;
    }

    public int getRepairCategory() {
        return repairCategory;
    }

    public void setRepairCategory(int repairCategory) {
        this.repairCategory = repairCategory;
    }

	public void write(KeyValStore.Put put, long time) {
		System.out.println("Top of write and time value is : "+time);
		StringBuilder writeLog1 = new StringBuilder();
		writeLog1.append(put.getKey());
		writeLog1.append(" ");
		writeLog1.append(put.getValue());
		writeLog1.append(" ");
		writeLog1.append(time);
		writeLog1.append(System.getProperty("line.separator"));
		System.out.println(writeLog1);

		String string= writeLog1.toString();
		BufferedWriter bufferedWriter = null;
		FileWriter fileWrite = null;
		String absolutePath ="Log/".concat(nodeName).concat(".log");
		System.out.println("Log file will be create with name: "+absolutePath);
		try {
			File file = new File(absolutePath);
			if(!file.exists()) {
				if (file.createNewFile()) {
					System.out.println("Log written!");
				}
			}
			fileWrite = new FileWriter(file, true);
			bufferedWriter = new BufferedWriter(fileWrite);
			bufferedWriter.write(string);

		} catch (IOException ioe) {
			System.out.println("One or more output files were not found!");
		} finally {
			try {

				if (bufferedWriter != null) {
					bufferedWriter.flush();
					bufferedWriter.close();
				}
				if (fileWrite != null) {
					fileWrite.flush();
					fileWrite.close();
				}
			} catch (IOException e) {
				System.out.println();
			}
		}
	}
}
