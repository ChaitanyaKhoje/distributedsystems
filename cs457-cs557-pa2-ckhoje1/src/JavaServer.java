import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

import java.net.InetAddress;

public class JavaServer {

    public static Handler handler;

    public static FileStore.Processor processor;

    public static int port;
    public static String ip;

    public static void main(String [] args) {
        try {
            port = Integer.valueOf(args[0]);
            ip = InetAddress.getLocalHost().getHostAddress();
            handler = new Handler(ip, port);
            processor = new FileStore.Processor(handler);

            Runnable r = new Runnable() {
                public void run() {
                    System.out.println("Thread started");
                    JavaServer.simple(processor);
                }
            };
            new Thread(r).start();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    public static void simple(FileStore.Processor processor) {
        try {
            System.out.println("THREAD");
            TServerTransport serverTransport = new TServerSocket(port);
            TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));
            System.out.println("Starting the simple server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}