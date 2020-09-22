import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Client {
    private InetAddress multicastAddress;
    private MulticastSocket socket;
    private final long timeout = 5000;
    private Map<UUID, Long> aliveCopies;
    byte[] sendData = new byte[512];
    byte[] receiveData = new byte[512];
    private long lastSendTime;
    private long lastReceiveTime;
    private final int port = 4446;
    private UUID myUUID;

    Client(String address) throws IOException {
        multicastAddress = InetAddress.getByName(address);
        socket = new MulticastSocket(port);
        socket.joinGroup(multicastAddress);
        socket.setSoTimeout((int) timeout);
        aliveCopies = new HashMap<>();
        lastReceiveTime = 4500;
        lastSendTime = 5200;
        myUUID = UUID.randomUUID();
        sendData = myUUID.toString().getBytes();
        System.out.println("Your UUID: " + myUUID);
    }

    public void findCopies() {
        while (true) {
            sendUDP();
            UUID receiveUUID = receiveUDP();
            aliveCopies.put(receiveUUID, lastReceiveTime);
            for (Map.Entry<UUID, Long> entry : aliveCopies.entrySet()) {
                System.out.println("Node " + entry.getKey() + " is alive");
            }
            aliveCopies.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > timeout);
        }
        }

    private UUID receiveUDP() {
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            socket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
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
        lastReceiveTime = System.currentTimeMillis();
    }
}
