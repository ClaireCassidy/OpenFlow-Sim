import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import tcdIO.Terminal;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class Router extends Node {

    private Terminal terminal;
    private HashMap<Integer, Integer> flowTable;        //contains the routing information for particular endpoints
    private int curDstPort;                             //dst port to send the next packet to; updated based on
                                                        // received packet dst port and associated entry in flow table

    boolean quit = false;

    //Routers @ ports 50007-50020
    static final int SRC_PORT = 50007;          //port number of this router

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {
        terminal.println("Received transmission from port " + PacketHelper.getPacketSrcPort(packet));

        //route the packet
        int finalDstPort = PacketHelper.getPacketDstPort(packet);
        terminal.println("Final dst port: " + finalDstPort + (finalDstPort == SRC_PORT? " (Here)":""));
        if (finalDstPort == SRC_PORT) { //end of the line
            terminal.println("Payload: " + PacketHelper.getPayload(packet)+"\n\n");
        } else {
            if (flowTable.containsKey(finalDstPort)) {
                System.out.println("Match");
                curDstPort = flowTable.get(finalDstPort);   //set curDstPort to the next hop in the route
                terminal.println("Forwarding packet to next hop ("+curDstPort+")");
                packet.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, curDstPort));
                try {
                    socket.send(packet);
                    System.out.println("SENT");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                terminal.println("Couldn't find final destination port "+finalDstPort+" in flow table");
                //contact controller...
            }
        }
        notify();
    }

    public synchronized void start() throws Exception {
        terminal.println("OpenFlow Router @ port "+SRC_PORT+" started");
        initialiseHashmap();        //test method

        while (!quit) {
            terminal.println("\n\nAwaiting contact ... ");
            wait();
        }
    }

    Router(Terminal terminal) {
        try {
            this.terminal = terminal;
            //routerSocketAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(SRC_PORT);
            flowTable = new HashMap<Integer, Integer>();
            listener.go();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Terminal terminal = new Terminal("Router @ "+SRC_PORT);
            (new Router(terminal)).start();
            terminal.println("Program completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    private void initialiseHashmap() {
        // Dest port | next Hop
        flowTable.put(new Integer(50009), new Integer(50008));
    }
}
