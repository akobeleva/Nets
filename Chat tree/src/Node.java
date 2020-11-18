import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {
    private final String name;
    private final int loss_percentage;
    private InetSocketAddress parentAddress = null;
    private InetSocketAddress alternateNode = null;
    private DatagramSocket socket;
    private DatagramChannel channel;
    Selector selector;

    Set<SocketAddress> neighbours = new HashSet<>();

    Map<UUID, DatagramPacket> receivedMessages = new HashMap<>();
    Map<UUID, List<SocketAddress>> sentMessages = new HashMap<>();

    private final BlockingQueue<String> strings = new LinkedBlockingQueue<>();

    Set<SocketAddress> blackList = new HashSet<>();

    int resendDelay = 100;
    ActionListener resendMsg = e -> sentMessages.forEach((key, value) -> value.forEach(socketAddress -> {
        byte[] sub = receivedMessages.get(key).getData();
        send(socketAddress, ByteBuffer.wrap(sub).position(sub.length));
    }));

    int checkAliveDelay = 100;
    ActionListener checkIfAlive = e -> sendToNeighbours(null, ByteBuffer.allocate(1).put(MessageType.PING_MESSAGE));

    int killDiedDelay = 3000;
    ActionListener killDiedNodes = e ->{
        blackList.forEach(socketAddress -> {
            neighbours.remove(socketAddress);
            System.out.println("Died: " + socketAddress);
            if (socketAddress.equals(parentAddress)){
                rebuildTree();
            }
            sentMessages.entrySet().removeIf(entry -> entry.getValue().contains(socketAddress));
        });
        blackList.clear();
        blackList.addAll(neighbours);
    };

    public Node(String _name, int _port, int _loss_percentage) throws SocketException {
        this.name = _name;
        this.loss_percentage = _loss_percentage;
        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(InetAddress.getByName("localhost"), _port));
            socket = channel.socket();
            System.out.println(socket.getLocalSocketAddress());
            selector = Selector.open();
            channel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        startReader(selector);
        try {
            channel.register(selector, SelectionKey.OP_READ);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }

        Timer resendTimer = new Timer(resendDelay, resendMsg);
        resendTimer.start();

        Timer killDiedTimer = new Timer(killDiedDelay, killDiedNodes);
        killDiedTimer.start();

        Timer checkAliveTimer = new Timer(checkAliveDelay, checkIfAlive);
        checkAliveTimer.start();
    }

    public Node(String _name, int _port, int _loss_percentage, InetSocketAddress _parentAddress) throws SocketException {
        this.name = _name;
        this.loss_percentage = _loss_percentage;
        this.parentAddress = _parentAddress;
        try {
            channel = DatagramChannel.open();
            socket = channel.socket();
            channel.bind(new InetSocketAddress(InetAddress.getByName("localhost"), _port));
            System.out.println(socket.getLocalSocketAddress());
            selector = Selector.open();
            channel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        startReader(selector);
        try {
            channel.register(selector, SelectionKey.OP_READ);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
        sendHello(parentAddress);

        Timer resendTimer = new Timer(resendDelay, resendMsg);
        resendTimer.start();

        Timer killDiedTimer = new Timer(killDiedDelay, killDiedNodes);
        killDiedTimer.start();

        Timer checkAliveTimer = new Timer(checkAliveDelay, checkIfAlive);
        checkAliveTimer.start();
    }

    void run(){
        while (true){
            if (!strings.isEmpty()){
                sendMessage(strings.poll());
            }
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Set<SelectionKey> channels = selector.selectedKeys();
            for (SelectionKey key : channels){
                if (key.isValid() && key.isReadable()) {
                    receive();
                }
            }
        }
    }

    void startReader(Selector selector){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        Thread backgroundReaderThread = new Thread(()-> {
            while(!Thread.interrupted()){
                try {
                    String string = bufferedReader.readLine();
                    if (string == null){
                        break;
                    }
                    strings.add(string);
                    selector.wakeup();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        backgroundReaderThread.start();
    }

    Random random = new Random();
    boolean isItFaultPacket(){
        if (random.nextInt(99) < loss_percentage){
            System.out.println("Packet has lost");
            return true;
        }
        return false;
    }

    void send(SocketAddress socketAddress, ByteBuffer buffer){
        buffer.flip();
        try {
            channel.send(buffer, socketAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void receive(){
        ByteBuffer sub = ByteBuffer.allocate(4096);
        byte type = MessageType.ERROR_MESSAGE;
        DatagramPacket datagramPacket = null;
        try {
            SocketAddress address = channel.receive(sub);
            if (address == null) return;
            int lim = sub.flip().limit();
            byte[] buffer = new byte[lim];
            sub.get(buffer, 0, lim);
            type = buffer[0];
            datagramPacket = new DatagramPacket(buffer, lim, address);
        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (type){
            case MessageType.CHAT_MESSAGE: {
                if (!isItFaultPacket()) receiveMessage(datagramPacket);
                break;
            }
            case MessageType.HELLO_MESSAGE: {
                receiveHello(datagramPacket);
                break;
            }
            case MessageType.HANDSHAKE_MESSAGE: {
                receiveHandshake(datagramPacket);
                break;
            }
            case MessageType.PING_MESSAGE: {
                receivePing(datagramPacket);
                break;
            }
            case MessageType.CONFIRM_MESSAGE: {
                receiveConfirmation(datagramPacket);
                break;
            }
            default: {
                System.out.println("Receive error message");
                break;
            }
        }
    }

    void sendHello(InetSocketAddress address){
        System.out.println("Send Hello to " + address);
        neighbours.add(address);
        send(address, ByteBuffer.allocate(1).put(MessageType.HELLO_MESSAGE));
    }

    void receiveHello(DatagramPacket packet){
        System.out.println("Receive Hello from");
        SocketAddress address = packet.getSocketAddress();
        System.out.println(address);
        neighbours.add(address);
        sendHandshakeMsg(address);
    }

    void sendToNeighbours(SocketAddress notSend, ByteBuffer buffer){
        neighbours.forEach((e) -> {
            if (!e.equals(notSend)) send(e, buffer);
        });
    }

    void sendMessage(String msg){
        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length);
        buffer.put(MessageType.CHAT_MESSAGE)
                .putInt(uuid.toString().getBytes().length)
                .put(uuid.toString().getBytes())
                .putInt(name.getBytes().length)
                .put(name.getBytes())
                .putInt(msg.getBytes().length)
                .put(msg.getBytes());
        receivedMessages.put(uuid, packet);
        List<SocketAddress> list = new ArrayList<>(neighbours);
        sentMessages.put(uuid, list);
        sendToNeighbours(null, buffer);
    }

    void receiveMessage(DatagramPacket packet){
        SocketAddress from = packet.getSocketAddress();
        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        buffer.get();
        int lengthUUID = buffer.getInt();
        byte[] bufferUUID = new byte[lengthUUID];
        buffer.get(bufferUUID, 0, lengthUUID);
        int lengthName = buffer.getInt();
        byte[] bufferName = new byte[lengthName];
        buffer.get(bufferName, 0, lengthName);
        int lengthMsg = buffer.getInt();
        byte[] bufferMsg = new byte[lengthMsg];
        buffer.get(bufferMsg, 0, lengthMsg);
        UUID uuid = UUID.fromString(new String(bufferUUID, 0, lengthUUID));
        String nameSrc = new String(bufferName, 0, lengthName);
        String msg = new String(bufferMsg, 0, lengthMsg);

        if (receivedMessages.containsKey(uuid)){
            return;
        }

        receivedMessages.put(uuid, packet);

        System.out.println("UUID: " + uuid);
        System.out.println("Source name: " + nameSrc);
        System.out.println("Message: " + msg);

        List<SocketAddress> list = new ArrayList<>();
        list.remove(from);
        sentMessages.put(uuid, list);

        sendConfirmation(from, uuid);
        sendToNeighbours(from, buffer);
    }

    void rebuildTree(){
        System.out.println("Start rebuilding");
        if (alternateNode != null & !alternateNode.equals((parentAddress))) {
            sendHello(alternateNode);
            return;
        }
        Optional<SocketAddress> new_addr = neighbours.stream().filter(e -> !e.equals(parentAddress)).findFirst();
        if (new_addr.isPresent()){
            alternateNode = (InetSocketAddress) new_addr.get();
            parentAddress = (InetSocketAddress) new_addr.get();
            sendUpdateAlternateNodeForChild();
        } else {
            parentAddress = null;
            alternateNode = null;
            System.out.println("I'm alone");
        }
    }

    void sendUpdateAlternateNodeForChild(){
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        buffer.put(MessageType.HANDSHAKE_MESSAGE)
                .putInt(alternateNode.getPort())
                .putInt(alternateNode.getHostName().length())
                .put(alternateNode.getHostName().getBytes());
        sendToNeighbours(null, buffer);
    }

    void sendHandshakeMsg(SocketAddress address){
        System.out.println("Send handshake");
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        if (parentAddress == null){
            alternateNode = (InetSocketAddress) socket.getLocalSocketAddress();
            buffer.put(MessageType.HANDSHAKE_MESSAGE)
                    .putInt(alternateNode.getPort())
                    .putInt(alternateNode.getHostName().length())
                    .put(alternateNode.getHostName().getBytes());
            alternateNode = (InetSocketAddress) address;
            parentAddress = (InetSocketAddress) address;
        } else {
            buffer.put(MessageType.HANDSHAKE_MESSAGE)
                    .putInt(parentAddress.getPort())
                    .putInt(parentAddress.getHostName().length())
                    .put(parentAddress.getHostName().getBytes());
        }
        send(address, buffer);
    }

    void receiveHandshake(DatagramPacket packet){
        System.out.println("Receive handshake");
        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        buffer.get();
        int port = buffer.getInt();
        int hostLentgh = buffer.getInt();
        byte[] bufferHost = new byte[hostLentgh];
        buffer.get(bufferHost, 0, hostLentgh);
        String host = new String(bufferHost);

        try {
            System.out.println("Update alternate node");
            InetSocketAddress newAlternateNode = new InetSocketAddress(InetAddress.getByName(host), port);
            if (socket.getLocalSocketAddress().equals(newAlternateNode)){
                alternateNode = (InetSocketAddress) packet.getSocketAddress();
                parentAddress = (InetSocketAddress) packet.getSocketAddress();
            } else alternateNode = newAlternateNode;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    void receivePing(DatagramPacket packet){
        blackList.remove(packet.getSocketAddress());
    }

    void sendConfirmation(SocketAddress socketAddress, UUID msgUUID){
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        buffer.put(MessageType.CONFIRM_MESSAGE)
                .putInt(msgUUID.toString().getBytes().length)
                .put(msgUUID.toString().getBytes());
        send(socketAddress, buffer);
    }

    void receiveConfirmation(DatagramPacket packet){
        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        buffer.get();
        int lengthUUID = buffer.getInt();
        byte[] bufferUUID = new byte[4096];
        buffer.get(bufferUUID, 0, lengthUUID);
        String uuidString = new String(bufferUUID, 0, lengthUUID);
        System.out.println("Receive confirmation of: " + uuidString);
        UUID uuid = UUID.fromString(uuidString);

        sentMessages.get(uuid).remove(packet.getSocketAddress());
    }
}
