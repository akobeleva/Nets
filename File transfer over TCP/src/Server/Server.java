package Server;

import java.io.*;
import java.net.ServerSocket;

public class Server {
    private int countClients;
    private final ServerSocket socket;
    boolean serverOn;

    public void startServer(){
        serverOn = true;
        System.out.println("Server is started!");
    }

    public Server(int listenPort) throws IOException {
        socket = new ServerSocket(listenPort);
        countClients = 0;
        serverOn = false;
    }

    public void awaitConnections() throws IOException {
        while(serverOn){
            countClients++;
            new Thread(new ServerTask(socket.accept(), countClients)).start();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Incorrect count of arguments");
            return;
        }
        Server server = new Server(Integer.parseInt(args[0]));
        server.startServer();
        server.awaitConnections();
    }
}