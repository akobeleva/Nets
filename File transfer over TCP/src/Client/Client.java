package Client;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Client {
    private final Socket socket;
    private static final String FOLDER = "C:\\Users\\Анастасия\\IdeaProjects\\client-server\\resourсes\\";
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    public Client(String address, int port) throws IOException {
        socket = new Socket(address, port);
    }

    public void sendFile(String name) throws IOException {
        File file = new File(FOLDER + name);
        if (file.length() != 0) {
            //send fileName
            dataOutputStream.writeUTF(name);

            //send fileSize
            dataOutputStream.writeLong(file.length());

            //send file
            FileInputStream in = new FileInputStream(file);
            byte[] data = new byte[1024];
            int size;
            while ((size = in.read(data)) != -1){
                dataOutputStream.write(data, 0, size);
                dataOutputStream.flush();
            }
            in.close();
            System.out.println("File is uploaded");
        }
    }

    public void getResponse() throws IOException {
        System.out.println("Response: " + dataInputStream.readUTF());
    }

    public void startClient(){
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3){
            System.err.println("Incorrect count of arguments");
            return;
        }
        Client client = new Client(args[0], Integer.parseInt(args[1]));
        if (args[2].length() > 4096){
            System.err.println("Incorrect filename length");
            return;
        }
        client.startClient();
        client.sendFile(args[2]);
        client.getResponse();
        client.closeConnection();
    }
}
