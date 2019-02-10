import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class FileProcessor {
	private String filePath;
	private Scanner scanner ;

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePathIn) {
		filePath = filePathIn;
	}
	public Scanner getScanner() {
		return scanner;
	}

	public void setScanner(Scanner scannerIn) {
		this.scanner = scannerIn;
	}

	/**
	 * @param filePathIn
	 * @throws FileNotFoundException
	 */
	public FileProcessor(String filePathIn) {
	
	try {
		filePath = filePathIn;
		File file = new File(filePath);
		setScanner(new Scanner(file));
	}
	catch(FileNotFoundException e) {
	System.err.println("File not found at specified location.");
		System.exit(0);
	}
	}
	
	public String readFileLine() {
		if(scanner.hasNextLine())
			return scanner.nextLine();
		else
			return null;
	}
	
	public boolean hasNextLine() {
		return scanner.hasNextLine();

	}
	public String toString() {
		String printMessage;
		printMessage = "FilePath:"+filePath;
		return printMessage;	
	}

}
