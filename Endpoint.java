import tcdIO.Terminal;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Endpoint extends Node {

    private Terminal terminal;
    private InetSocketAddress routerSocketAddress;

    boolean quit = false;

    //ports 50001 - 50006 incl. are endpoints
    static final int SRC_PORT = 50001;              //port this endpoint is located at
    static final int ROUTER_PORT = 50007;           //port number of router this endpoint is directly connected to

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {

    }

    public synchronized void start() throws Exception {
        terminal.println("Openflow Endpoint @ port " + SRC_PORT + " started ... ");
        String payload;
        int packetDst;

        while (!quit) {
            payload = terminal.readString("Please enter a message to send: ");
            packetDst = Integer.parseInt(terminal.readString("Please enter the end destination " +
                    "port of the packet: "));
            terminal.println("Preparing to send message \""+payload+"\" to port "+packetDst + " ... ");

            DatagramPacket toRouter = PacketHelper.createPacket(payload, SRC_PORT, packetDst, -1);
            toRouter.setSocketAddress(routerSocketAddress);
            socket.send(toRouter);

            terminal.println("Packet successfully forwarded to router\n");
        }
    }

    public static void main(String[] args) {
        try {
            Terminal terminal = new Terminal("Endpoint @ "+SRC_PORT);
            (new Endpoint(terminal, DEFAULT_DST_NODE, ROUTER_PORT, SRC_PORT)).start();
            terminal.println("Program completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    Endpoint(Terminal terminal, String dstHost, int dstPort, int srcPort) {
        try {
            this.terminal = terminal;
            routerSocketAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}
