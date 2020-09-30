import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class Client {
    private InetAddress multicastAddress;
    private MulticastSocket socket;
    private final long timeout = 5000;
    private Map<UUID, Long> aliveCopies;
    private byte[] sendData;
    private byte[] receiveData;
    private long lastSendTime;
    private long lastReceiveTime;
    private final int port;
    private UUID myUUID;
   // private UUID emptyUUID;
    private int lengthUUID;

    Client(String address, int argport) throws IOException {
        multicastAddress = InetAddress.getByName(address);
        port = argport;
        socket = new MulticastSocket(port);
        socket.joinGroup(multicastAddress);
       // socket.setSoTimeout(3000);
        socket.setSoTimeout((int) timeout);
        aliveCopies = new HashMap<>();
        lastReceiveTime = 0;
        lastSendTime = 0;
        myUUID = UUID.randomUUID();
       // emptyUUID = UUID.randomUUID();
        lengthUUID = myUUID.toString().length();
        sendData = myUUID.toString().getBytes();
    }

    public void findCopies() throws IOException {
        LinkedList<UUID> clones = new LinkedList<>();
        while (true) {
            if (System.currentTimeMillis() - lastSendTime > 1000) sendUDP();
            UUID receiveUUID = receiveUDP();
            if (receiveUUID != null)
                aliveCopies.put(receiveUUID, lastReceiveTime);
            //aliveCopies.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > timeout);
            for (Map.Entry<UUID, Long> entry : aliveCopies.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() > timeout) clones.add(entry.getKey());
                //System.out.println("Node " + entry.getKey() + " is alive");
            }
            for (UUID entry : clones)
                aliveCopies.remove(entry);
            clones.clear();
            for (Map.Entry<UUID, Long> entry : aliveCopies.entrySet())
                System.out.println("Node " + entry.getKey() + " is alive");
        }
    }

    private UUID receiveUDP() throws IOException{
        receiveData = new byte[lengthUUID];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, lengthUUID);
        try {
            socket.receive(receivePacket);
        } catch (SocketTimeoutException e) {
            return null;
        }
        lastReceiveTime = System.currentTimeMillis();
        return UUID.nameUUIDFromBytes(receiveData);
    }

    private void sendUDP() {
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, multicastAddress, port);
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        lastSendTime = System.currentTimeMillis();
    }
}
