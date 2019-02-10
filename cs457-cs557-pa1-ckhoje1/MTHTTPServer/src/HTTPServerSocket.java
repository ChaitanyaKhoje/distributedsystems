import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServerSocket {

    public HTTPServerSocket() { }

    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(0);
            System.out.println("Server created on port: " + serverSocket.getLocalPort() + " and hostname: " + serverSocket.getInetAddress().getLocalHost().getHostName());
        } catch (IOException e) {
            System.err.println("Server creation on a port number failed, please try again.");
        }

        while(true) {
            Socket clientSocket = null;
            try {
                if (serverSocket != null) {
                    clientSocket = serverSocket.accept();
                }
                Handler h = new Handler(clientSocket);
                h.start();
                //new Thread(new Handler(clientSocket)).start();
            } catch (IOException e) {
                System.err.println("Cannot accept client request!");
                return;
            } finally {
                /*try {
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }
        }
    }
}
