import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Handler implements FileStore.Iface {

    private Utility utility = new Utility();
    private List<NodeID> fingerTable;
    private String ip;
    private int port;
    private String key;
    private NodeID currentNode;

    private Map<String, RFileMetadata> fileMetadata;
    private Map<String, FileData> fileContent;

    public static boolean checker = false;

    public Handler() {}

    public Handler(String ipAddr, int portNumber) {

        ip = ipAddr;
        //System.out.println("DEBUG: IP: " + ipAddr);
        port = portNumber;
        //System.out.println("DEBUG: IP: Port:" + portNumber);
        key = Utility.calculateHashValue(ipAddr + ":" + portNumber);
        System.out.println("-------------------------------");
        System.out.println("Server with PORT: " + port + " and HASH: "  + key);
        System.out.println("-------------------------------");
        currentNode = new NodeID(key, ip, port);
        fileMetadata = new HashMap<String, RFileMetadata>();
        fileContent = new HashMap<String, FileData>();
    }

    @Override
    public void writeFile(RFile rFile) throws SystemException, TException {

        // Check if the file exists on the current server
        // if it does, overwrite file contents and increment version number.

        // File does not exist code;
        //System.out.println("WRITE FILE CALLED!");
        //System.out.println("DEBUG: (Inside writeFile) Filename: " + rFile.getMeta().getFilename());
        String contentHash = Utility.calculateHashValue(rFile.getContent());  // Get hash of content
        rFile.getMeta().setContentHash(contentHash);

        String fileNameHash = Utility.calculateHashValue(rFile.getMeta().getFilename());
        System.out.println("Filename hash: " + fileNameHash);
        NodeID node = findSucc(fileNameHash);
        System.out.println("DEBUG: Found successor with hash: " + node.getId());
        // We have to write the file on current node.
        if (node.getId().equals(currentNode.getId())) {
            System.out.println("WRITING FILE ON: " + node.getId());
            if (!fileMetadata.containsKey(rFile.getMeta().getFilename()) && !fileContent.containsKey(rFile.getMeta().getFilename())) {
                fileMetadata.put(rFile.getMeta().getFilename(), rFile.getMeta());
                fileContent.put(rFile.getMeta().getFilename(), new FileData(rFile.getContent(), contentHash));
            } else {
                RFileMetadata rFileMetadata = fileMetadata.get(rFile.getMeta().getFilename());
                rFileMetadata.setVersion(rFile.getMeta().getVersion() + 1);
                rFileMetadata.setContentHash(rFile.getMeta().getContentHash());
                rFile.setContent(rFile.getContent());
                rFile.setMeta(rFileMetadata);
                fileMetadata.get(rFile.getMeta().getFilename()).setContentHash(rFile.getMeta().getContentHash());
                fileMetadata.get(rFile.getMeta().getFilename()).setVersion(rFile.getMeta().getVersion());
                fileContent.get(rFile.getMeta().getFilename()).setContent(rFile.getContent());
                fileContent.get(rFile.getMeta().getFilename()).setContentHash(rFile.getMeta().getContentHash());
            }
        } else {
            SystemException systemException = new SystemException();
            systemException.setMessage("The server is not the file's successor!");
            throw systemException;
        }
    }

    @Override
    public RFile readFile(String filename) throws SystemException, TException {

        //System.out.println("READ FILE CALLED!");
        //System.out.println("DEBUG: (Inside readFile) Filename: " + filename);
        String fileNameHash = Utility.calculateHashValue(filename);  // Get hash of content
        System.out.println("Filename hash: " + fileNameHash);
        NodeID node = findSucc(fileNameHash);
        RFile rFile = null;
        //if(fileContent.containsKey(filename) && fileMetadata.containsKey(filename)) {
        if(node.getId().equals(currentNode.getId())) {
            if (fileContent.containsKey(filename)) {
                System.out.println("DEBUG: File found on: " + node.getId());
                rFile = new RFile();
                RFileMetadata metadata = new RFileMetadata();
                metadata.setFilename(filename);
                metadata.setVersion(fileMetadata.get(filename).getVersion());
                metadata.setContentHash(fileMetadata.get(filename).getContentHash());

                rFile.setMeta(metadata);
                rFile.setContent(fileContent.get(filename).getContent());
            } else {
                SystemException systemException = new SystemException();
                systemException.setMessage("The file doesn't exist on the server.");
                throw systemException;
            }
        } else {
            SystemException systemException = new SystemException();
            systemException.setMessage("The server is not the file's successor.");
            throw systemException;
        }
        return rFile;
    }

    @Override
    public void setFingertable(List<NodeID> node_list) throws TException {

        this.fingerTable = node_list;
    }

    @Override
    public NodeID findSucc(String key) throws SystemException, TException {

        NodeID predecessor = findPred(key);
        NodeID node = null;
        FileStore.Client client = null;
        if (!currentNode.getId().equals(predecessor.getId())) {
            TTransport tTransport = new TSocket(predecessor.getIp(), predecessor.getPort());
            tTransport.open();
            TProtocol tProtocol = new TBinaryProtocol(tTransport);
            client = new FileStore.Client(tProtocol);
            node = client.getNodeSucc();
            tTransport.close();
        } else {
            node = getNodeSucc();
        }
        return node;
    }

    @Override
    public NodeID findPred(String key) throws SystemException, TException {

        NodeID nodeID = currentNode;
        if (fingerTable != null) {
            boolean existsInFT;
            NodeID currNodeSuccessor = getNodeSucc();
            //System.out.println("DEBUG: (Inside findPred) Found current node successor: " + currNodeSuccessor.getId());

            if (!utility.isElementOf(key, currentNode.getId(), currNodeSuccessor.getId(), true)) {
                existsInFT = false;
                for (int i = fingerTable.size() - 1; i >= 0; i--) {
                    NodeID fingerTableEntry = fingerTable.get(i);
                    if (utility.isElementOf(fingerTableEntry.getId(), currentNode.getId(), key, false)) {
                        nodeID = fingerTableEntry;
                        existsInFT = true;
                        break;
                    }
                }
                if (!existsInFT) {
                    SystemException systemException = new SystemException();
                    systemException.setMessage("The key was not found on fingertable for the node: " + nodeID);
                    throw systemException;
                }
                TTransport tTransport = new TSocket(nodeID.getIp(), nodeID.port);
                tTransport.open();
                TProtocol tProtocol = new TBinaryProtocol(tTransport);
                FileStore.Client client = new FileStore.Client(tProtocol);
                nodeID = new NodeID();
                nodeID = client.findPred(key);
                tTransport.close();
            }
        } else {
            SystemException systemException = new SystemException();
            systemException.setMessage("No fingertable for the current node.");
            throw systemException;
        }
        return nodeID;
    }

    @Override
    public NodeID getNodeSucc() throws SystemException, TException {

        NodeID nodeID = null;
        if(null != fingerTable && !fingerTable.isEmpty()) {
            nodeID = new NodeID(fingerTable.get(0));
        } else {
            SystemException systemException = new SystemException();
            String message = "Fingertable doesn't exist for the current node.";
            systemException.setMessage(message);
            throw systemException;
        }
        return nodeID;
    }
}
