import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessServerUtil {

	public static ProcessServerUtil singletonProcessServerUtil;
	public Map<String, ServerContext> savedContexts = new HashMap<String, ServerContext>();

	public ProcessServerUtil() {}

	public ProcessServerUtil(String first) {

		singletonProcessServerUtil = new ProcessServerUtil();
	}

	public static FileLineToken processLine(String lineIn) {

		FileLineToken fileLineToken;
		String[] splitArray;
		String serverName;
		String serverIPAddress;
		int serverPortNumber;
		splitArray = lineIn.split(" ");
		serverName = splitArray[0];
		serverIPAddress = splitArray[1];
		serverPortNumber = Integer.parseInt(splitArray[2]);

		fileLineToken = new FileLineToken(serverName, serverIPAddress, serverPortNumber);
		return fileLineToken;
	}

	public Map<String, ServerContext> getSavedContexts() {

		ProcessServerUtil psu = getSingletonProcessServerUtil();
		return psu.savedContexts;
	}

	public void setSavedContexts(Map<String, ServerContext> savedContexts) {
		getSingletonProcessServerUtil().setSavedContexts(savedContexts);
	}

	public static ProcessServerUtil getSingletonProcessServerUtil() {
		return singletonProcessServerUtil;
	}
}