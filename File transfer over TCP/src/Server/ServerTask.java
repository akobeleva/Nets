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
    FileOutputStream outFile;
    File file;
    private static final String FOLDER = ".\\uploads\\";
    boolean isClientAlive = true;
    long speedSize = 0;
    Timer timer1;
    Timer timer2;
    long receivedFileSize = 0;
    long realFileSize;
    long startTime, endTime;

    public ServerTask(Socket _client, int _id) throws IOException {
        client = _client;
        taskID = _id;
        dataInputStream = new DataInputStream(client.getInputStream());
        dataOutputStream = new DataOutputStream(client.getOutputStream());
    }

    ActionListener speed = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (speedSize != 0)
                System.out.println("Downloading speed: " + Math.round(speedSize / 3 / 1024) + " кб/c" + "| File: "
                        + file.getName()+ "| from Client: " + taskID);
            speedSize = 0;
        }
    };

    ActionListener checkIfClientAlive = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (speedSize == 0) isClientAlive = false;
            if(!isClientAlive){
                checkMetaInfo();
            }
        }
    };

    public void receiveFile(){
        try {
            byte[] data = new byte[4096];
            int count;
            startTime = System.currentTimeMillis();
            while ((count = dataInputStream.read(data)) != -1){
                receivedFileSize += count;
                speedSize += count;
                outFile.write(data, 0, count);
                outFile.flush();
            }
            endTime = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void end(){
        try {
            timer2.stop();
            timer1.stop();
            outFile.close();
            dataInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private void printTotalSpeed(){
        System.out.println("Total downloading speed: " + Math.round(receivedFileSize / (((endTime - startTime)
                * 1.0) / 1000) / 1024) + " кб/c" + "| File: " + file.getName()+ "| from Client: " + taskID);
    }

    @Override
    public void run() {
        System.out.println("Connected with host " + client.getInetAddress() + "\nPort: " + client.getPort()
                                + "\nClientID: " + taskID);
        try {
            String fileName = dataInputStream.readUTF();
            realFileSize = dataInputStream.readLong();
            file = new File(FOLDER + fileName);
            if (file.exists())
                file = new File(FOLDER + taskID + fileName);
            file.createNewFile();
            outFile = new FileOutputStream(file);
            timer1 = new Timer(1000, checkIfClientAlive);
            timer2 = new Timer(3000, speed);
            timer1.start();
            timer2.start();
            receiveFile();
            printTotalSpeed();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //end();
            System.out.println("Client " + taskID + " is disconnected");
        }
    }
}
