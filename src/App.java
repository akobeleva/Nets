import java.net.InetAddress;
import java.net.UnknownHostException;

public class App {
    public static void main(String[] args) throws UnknownHostException {
        InetAddress myIP = null;
        myIP = InetAddress.getLocalHost();
        System.out.println(myIP);
        Client client = new Client();
        client.findCopies();
    }
}
