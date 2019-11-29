import tcdIO.Terminal;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class EndpointGeneric extends Node {

    private Terminal terminal;
    private InetSocketAddress routerSocketAddress;

    boolean quit = false;

    //ports 50001 - 50006 incl. are endpoints
    private int srcPort;              //port this endpoint is located at
    private int routerPort;           //port number of router this endpoint is directly connected to

    @Override
    public void onReceipt(DatagramPacket packet) {
        System.out.println("["+srcPort+"] Received msg");
        int msgType = PacketHelper.getMsgType(packet);
        int packetSrcPort = PacketHelper.getPacketSrcPort(packet);
        terminal.println();
        terminal.println("[Received packet from router @ "+packetSrcPort+"]");

        if (msgType == PacketContent.TYPE_MSG) {
            String payload = PacketHelper.getPayload(packet).trim();
            terminal.println("Message: "+payload);
        } else if (msgType == PacketContent.TYPE_ERROR) {
            String payload = PacketHelper.getPayload(packet).trim();
            terminal.println("NOTE: There was an error sending your message.");
            terminal.println(" Router says: \""+payload+"\"");
            terminal.println(" >>> Please enter a message to send: ");
        }

        //notify();
    }

    public synchronized void start() throws Exception {
        terminal.println("Openflow Endpoint @ port " + srcPort + " started ... ");
        terminal.println("Router @ port "+routerPort);
        String payload;
        int packetDst;

        while (!quit) {
            payload = terminal.readString(" >>> Please enter a message to send: ");
            packetDst = Integer.parseInt(terminal.readString("Please enter the end destination " +
                    "port of the packet: "));
            //wait();
            terminal.println("Preparing to send message \""+payload+"\" to port "+packetDst + " ... ");

            DatagramPacket toRouter = PacketHelper.createPacket(payload, srcPort, packetDst, PacketContent.TYPE_MSG);
            toRouter.setSocketAddress(routerSocketAddress);
            socket.send(toRouter);

            terminal.println("Packet successfully forwarded to router\n");
        }
    }

    public static void main(String[] args) {
        int srcPort = Integer.parseInt(args[0]);
        int routerPort = Integer.parseInt(args[1]);
        try {
            Terminal terminal = new Terminal("Endpoint @ "+srcPort);
            (new EndpointGeneric(terminal, DEFAULT_DST_NODE, routerPort, srcPort)).start();
            terminal.println("Program completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    EndpointGeneric(Terminal terminal, String dstHost, int routerPort, int srcPort) {
        try {
            this.srcPort = srcPort;
            this.routerPort = routerPort;
            this.terminal = terminal;
            routerSocketAddress = new InetSocketAddress(dstHost, routerPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}
