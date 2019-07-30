import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Handler extends Thread {

    private Socket socket;
    private int count;
    private static Map<String, Integer> requestsServed = new HashMap<String, Integer>();
    public static StringBuilder sb = new StringBuilder();
    Utility utility;

    public Handler(Socket socketIn) {

        utility = new Utility();
        socket = socketIn;
    }

    @Override
    public void run() {
        try {
            String resource = "";
            if (socket != null) {
                Scanner inputStream = new Scanner(new InputStreamReader(socket.getInputStream()));
                String resourceRequest = inputStream.nextLine();
                if (!resourceRequest.isEmpty()) {
                    if (resourceRequest.contains("GET")) {
                        resource = resourceRequest.replaceAll("GET /", "").replaceAll("HTTP/1.1", "").trim();

                        // Call getResource and check if the folder exists and get the resource if it does.

                        byte [] resourceArray = utility.getResource(resource);
                        socket.getOutputStream().write(resourceArray);

                        if (utility.fileExists) {
                            // Locking mechanism to maintain count for resources
                            Lock readWriteLock = new ReentrantLock();
                            readWriteLock.lock();
                            try {
                                if (!requestsServed.isEmpty() && requestsServed.get(resource) != null) {
                                    count = requestsServed.get(resource) + 1;
                                } else {
                                    count = 1;
                                }
                                increaseRequestsServed(resource, count);
                            } finally {
                                readWriteLock.unlock();
                            }
                        }
                    }
                }
                inputStream.close();
            }
            if (socket != null && utility.fileExists) {
                printResults(resource, socket.getInetAddress().getHostAddress(), socket.getLocalPort());
            }
        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void increaseRequestsServed(String resource, int count) {
        requestsServed.put(resource, count);
    }

    public void printResults(String resource, String clientIP, int portNumber) {
        sb.append("/" + resource);
        sb.append("|");
        sb.append(clientIP);
        sb.append("|");
        sb.append(portNumber);
        sb.append("|");
        sb.append(requestsServed.get(resource));
        System.out.println(sb.toString());
        sb.setLength(0);
    }
}
