import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utility {

    public boolean fileExists = false;

    /**
     * The HTTP response
     *
     * @return
     */
    public String prepareResponse(String filePathIn, boolean fileExists) {

        String responsePart = "";

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        if (fileExists) {
            File file = new File(filePathIn);
            Path path = new File(filePathIn).toPath();

            String status = "HTTP/1.1 200 OK";
            String dateTime = "Date: " + dateFormat.format(new Date());
            String server = "Server: Apache/2.2.16 (Debian)";
            String lastModified = "Last-Modified: " + dateFormat.format(file.lastModified());
            String contentType = "";

            try {
                contentType = "Content-Type: " + Files.probeContentType(path);
            } catch (IOException e) {
                System.err.println("An I/O error occurred");
            } finally {
            }

            String contentLength = "Content-Length: " + file.length();
            responsePart = status + "\n" + dateTime + "\n" + server + "\n" + lastModified + "\n" + contentType + "\n" + contentLength + "\n\n";
        } else {
            // The file does not exist. Return a 404 error header.
            String status = "HTTP/1.1 404 Not Found";
            String dateTime = "Date: " + dateFormat.format(new Date());
            String server = "Server: Apache/2.2.16 (Debian)";
            String lastModified = "Last-Modified: ";
            String contentType = "Content-Type: ";
            String contentLength = "Content-Length: ";

            responsePart = status + "\n" + dateTime + "\n" + server + "\n" + lastModified + "\n" + contentType + "\n" + contentLength + "\n\n";
        }
        return responsePart;
    }

    public byte[] getResource(String resource) {

        File file = new File(System.getProperty("user.dir") + "" + File.separator + "www/" + resource);
        byte[] resourceArray = null;

        if (file.exists()) {
            fileExists = true;
            String responsePart = prepareResponse(file.getPath(), fileExists);
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                resourceArray = new byte[responsePart.length() + content.length + 1];
                System.arraycopy(responsePart.getBytes(), 0, resourceArray, 0, responsePart.getBytes().length);
                System.arraycopy(content, 0, resourceArray, responsePart.getBytes().length, content.length);
            } catch (FileNotFoundException e) {
                System.err.println("The file was not found!");
            } catch (IOException e) {
                System.err.println("An I/O exception occurred while writing content to the response!");
            } finally { }
        } else {
            fileExists = false;
            resourceArray = prepareResponse(file.getPath(), fileExists).getBytes();
        }
        return resourceArray;
    }
}