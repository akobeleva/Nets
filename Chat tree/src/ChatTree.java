import java.net.InetSocketAddress;
import java.net.SocketException;

public class ChatTree {
    public static void main(String[] args) throws SocketException {
        if (args.length != 3 && args.length != 5) {
            System.err.println("Invalid arguments");
        }

        String nodeName = args[0];
        int nodePort = Integer.parseInt(args[1]);
        int nodeLossPercantage = Integer.parseInt(args[2]);
        Node node;
        if (args.length == 3) {
            node = new Node(nodeName, nodePort, nodeLossPercantage);
        }
        else {
            InetSocketAddress parentAddress = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
            node = new Node(nodeName,nodePort, nodeLossPercantage, parentAddress);
        }
        node.run();
    }
}
