import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Client {
    private SocketAddress address;
    private MulticastSocket socket;

    private Map<InetAddress, Long> aliveCopies = new HashMap<>();
    byte[] sendData = new byte[512];
    byte[] receiveData = new byte[512];

    public void findCopies(){}


    private InetAddress receiveUDP() {
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            socket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return receivePacket.getAddress();
    }

    private void sendUDP(){

    }
/*import java.net.Socket

        fun main(args: Array<String>) {
        val app = App("Hello", "224.0.0.0", 4441)
        }
        import java.net.*

class App constructor(msg: String, address: String, port: Int) {

private val timeout = 5000

private val address: SocketAddress //For multicasting
private val socket: MulticastSocket = MulticastSocket(port) //Default port
private var receiveLastTime: Long
private var sendLastTime: Long

private var copies = HashMap<String, Long>()

        init {
        socket.soTimeout = timeout
        this.address = InetSocketAddress(address, port)
        socket.joinGroup(this.address, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()))
        receiveLastTime = 5000
        sendLastTime = 4000
        copies.clear()
        findCopies()
        }

private fun findCopies() {
        sendLastTime = System.currentTimeMillis()
        while (true) {
//sending message to users in multicast group with timeout
        if (System.currentTimeMillis() - sendLastTime > timeout) sendMessage("DDOS")
//receive a message and remember user in map
        val ip = receiveMessage()
        if (ip.isNotEmpty()) copies.put(ip, receiveLastTime)
//if someone don't send a message for a long time then delete him from map
        for (entry in copies)
        if (System.currentTimeMillis() - entry.value > timeout) copies.remove(entry.key)

        println("Number of live devices: ${copies.size}")
        }
        }

private fun receiveMessage(): String {
        val requestPacket = DatagramPacket(ByteArray(256), ByteArray(256).size)
        try {
        socket.receive(requestPacket)
        } catch (ex : SocketTimeoutException) {
        return ""
        }
        receiveLastTime = System.currentTimeMillis()
        return requestPacket.address.toString()
        }

private fun sendMessage(mes: String) {
        socket.send(DatagramPacket(mes.toByteArray(), mes.toByteArray().size, address))
        sendLastTime = System.currentTimeMillis()
        }
        }
        */
}
