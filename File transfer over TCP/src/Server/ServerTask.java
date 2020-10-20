package Server;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ServerTask implements Runnable{
    private final Socket client;
    int taskID;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;
    private static final String FOLDER = "C:\\Users\\Анастасия\\IdeaProjects\\client-server\\uploads\\";

    public ServerTask(Socket _client, int _id) throws IOException {
        client = _client;
        taskID = _id;
        dataInputStream = new DataInputStream(client.getInputStream());
        dataOutputStream = new DataOutputStream(client.getOutputStream());
    }

    boolean isClientAlive = true;
    long speedSize = 0;
    ActionListener speed = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            System.out.println(speedSize);
            speedSize = 0;
        }
    };

    ActionListener checkIfClientAlive = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if(!isClientAlive){
                checkMetaInfo();
            }
            isClientAlive = false;
        }
    };

    FileOutputStream out;


    public void receiveFile(){
        try {
            byte[] data = new byte[4096];
            int count ;
            while ((count = dataInputStream.read(data)) != -1 ){
                //System.out.println(count);
                ServerTask.this.receivedFileSize += count;
                isClientAlive = true;
                speedSize += count;
                out.write(data, 0, count);
                out.flush();
            }
            System.out.println(receivedFileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void end(){
        try {
            timer2.stop();
            timer1.stop();
            out.close();
            dataInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    Timer timer1;
    Timer timer2;

    long receivedFileSize = 0;
    long realFileSize;

    public void checkMetaInfo(){
        try {
            if(receivedFileSize == realFileSize) {
                dataOutputStream.writeUTF("SUCCESS");
                System.out.println("well");
            }
            else {
                System.out.println("bad");
                dataOutputStream.writeUTF("FAILURE");
            }
            dataOutputStream.flush();
            end();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Connected with host " + client.getInetAddress() + "\nPort: " + client.getPort()
                                + "\nClientID: " + taskID);
        try {
            String fileName = dataInputStream.readUTF();
            receivedFileSize = dataInputStream.readLong();
            out = new FileOutputStream(new File(FOLDER + fileName));
            timer1 = new Timer(1000, checkIfClientAlive);
            timer2 = new Timer(3000, speed);
            timer1.start();
            timer2.start();
            receiveFile();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            end();
            System.out.println("Client " + taskID + " is disconnected");
        }
    }
}
