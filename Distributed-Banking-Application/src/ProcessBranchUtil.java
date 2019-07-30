
public class ProcessBranchUtil {
	public static FileLineToken processLine(String lineIn) {

		FileLineToken fileLineToken;
		String[] splitArray= null;
		String branchName=null;
		String branchIPAddress=null;
		int branchPortNumber=0;
		splitArray = lineIn.split(" ");
		branchName=splitArray[0];
		branchIPAddress=splitArray[1];
		branchPortNumber = Integer.parseInt(splitArray[2]);

		fileLineToken = new FileLineToken(branchName,branchIPAddress,branchPortNumber);
		return fileLineToken;

	}
}