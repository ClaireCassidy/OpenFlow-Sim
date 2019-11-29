import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import tcdIO.Terminal;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Thread.sleep;

public class RouterGeneric extends Node {

    private Terminal terminal;
    private HashMap<Integer, Integer> flowTable;        //contains the routing information for particular endpoints
    private int curDstPort;                             //dst port to send the next packet to; updated based on
                                                        // received packet dst port and associated entry in flow table

    private boolean quit = false;                       // has the user expressed desire to quit?
    private boolean awaitingFlowMod = false;            // used to check if execution should continue or if we're
                                                        //  awaiting a flow modification message
    private DatagramPacket toSend = null;               // Keeps a copy of the datagram
    private int toSendSrcEndpoint = -1;                 //  Used in the case of sending an error back to the endpoint
                                                        // that wanted to send

    private int srcPort;                                // src port of router instance
    private ArrayList<Integer> connectedEndpoints;       // endpoints directly connected to this router
    private final int CONTROLLER_PORT = 50000;          // known to all routers in the network

    RouterGeneric(Terminal terminal, int port) {
        try {
            srcPort = port;
            this.terminal = terminal;
            //routerSocketAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            flowTable = new HashMap<Integer, Integer>();
            listener.go();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);   //main called from RouterGenerator class and passed port no.
        //System.out.println("Router @ "+port+" started");
        try {
            Terminal terminal = new Terminal("Router @ "+port);
            (new RouterGeneric(terminal, port)).start();
            terminal.println("Program completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void start() throws Exception {
        sleep(1000);    //Ensure Controller has started up before the routers start
        terminal.println("OpenFlow Router @ port "+srcPort+" started");
        initialiseHashmap();        //test method
        connectedEndpoints = new ArrayList<>();
        initialiseConnectedEndpoints();

        //send hello message to controller
        DatagramPacket hello = PacketHelper.createPacket(null, srcPort,
                CONTROLLER_PORT, PacketContent.TYPE_HELLO);
        hello.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, PacketHelper.getPacketDstPort(hello)));
        socket.send(hello);

        while (!quit) {
            terminal.println("\n\nAwaiting contact ... ");
            wait();
            if (awaitingFlowMod) {
                System.out.println("["+srcPort+"] Awaiting flow mod");
                wait();
            }

            if (toSend != null) {
                //we've received the flow mod; send the packet

                int endpointPort = PacketHelper.getPacketDstPort(toSend);
                int nextHop = flowTable.get(endpointPort);
                try {
                    if (nextHop == -1) {
                        toSend.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, endpointPort));
                        socket.send(toSend);
                    } else {
                        toSend.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, nextHop));
                        socket.send(toSend);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                toSend = null;
            }
        }
    }

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {

        terminal.println("Received transmission from port " + PacketHelper.getPacketSrcPort(packet));
        int msgType = PacketHelper.getMsgType(packet);
        terminal.println("Received message type: "+PacketHelper.getMsgTypeAsString(msgType));


        if (msgType == PacketContent.TYPE_HELLO) {
            //none
        } else if (msgType == PacketContent.TYPE_FEATURE_REQUEST) {
            // create feature response information; will be stored in the payload
            String payload = ""+srcPort;
            terminal.println("Endpoint count for "+srcPort+": "+connectedEndpoints.size());
            for (Integer endpointPort:connectedEndpoints) {
                payload=payload+","+endpointPort;   //append each payload port
            }

            try {
                DatagramPacket featureResponse = PacketHelper.createPacket(payload, srcPort, CONTROLLER_PORT,
                        PacketContent.TYPE_FEATURE_RESPONSE);
                featureResponse.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE,
                        PacketHelper.getPacketDstPort(featureResponse)));
                socket.send(featureResponse);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msgType == PacketContent.TYPE_MSG) {
            //check the endpoint
            int endpointPort = PacketHelper.getPacketDstPort(packet);
            toSendSrcEndpoint = PacketHelper.getPacketSrcPort(packet);

            //check flow table for route
            if (flowTable.containsKey(endpointPort)) {
                //route based on flow table entry
                System.out.println("["+srcPort+"] Have flow info; routing");

                int nextHop = flowTable.get(endpointPort);
                String payload = PacketHelper.getPayload(packet).trim();

                if (nextHop == -1) { //-1 means that this router is the last router on path; send directly
                    System.out.println("["+srcPort+"] This is the last hop; routing to "+endpointPort);
                    try {
                        DatagramPacket toEndpoint = PacketHelper.createPacket(payload, srcPort, endpointPort, PacketContent.TYPE_MSG);
                        toEndpoint.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, endpointPort));
                        socket.send(toEndpoint);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        DatagramPacket toNextHop = PacketHelper.createPacket(payload, srcPort, endpointPort, PacketContent.TYPE_MSG);
                        toNextHop.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, nextHop));
                        socket.send(toNextHop);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                //send flow req to controller
                System.out.println("["+srcPort+"] Sending flow req to controller");
                try {
                    DatagramPacket flowRequest = PacketHelper.createPacket("" + PacketHelper.getPacketDstPort(packet), srcPort,
                            CONTROLLER_PORT, PacketContent.TYPE_FLOW_REQUEST);
                    flowRequest.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, PacketHelper.getPacketDstPort(flowRequest)));
                    socket.send(flowRequest);

                } catch (Exception e){
                    e.printStackTrace();
                }

                //create the packet and save it until flow mod received and can send
                System.out.println("["+srcPort+"] Saving packet");
                String payload = PacketHelper.getPayload(packet).trim();
                toSend = PacketHelper.createPacket(payload, srcPort, endpointPort, PacketContent.TYPE_MSG);

                System.out.println("["+srcPort+"] Setting awaiting flow mod flag to true");
                awaitingFlowMod = true;
                //returns to start method
            }
        } else if (msgType == PacketContent.TYPE_FLOW_MOD) {
            System.out.println("["+srcPort+"] Received Flow Mod");
            awaitingFlowMod = false;
            //add the information to the flow table
            String payload = PacketHelper.getPayload(packet).trim();
            String[] args = payload.split(",");

            int endpointPort = Integer.parseInt(args[0]);
            int nextHop = 0;
            if (args[1].equalsIgnoreCase("null")) {
                nextHop = -1;
            } else {
                nextHop = Integer.parseInt(args[1]);
            }

            flowTable.put(endpointPort, nextHop);
            System.out.println("["+srcPort+"] Added new entry to flow table: <"+endpointPort+":"+nextHop+">");
            notify();
        } else if (msgType == PacketContent.TYPE_ERROR) {
            if (awaitingFlowMod) {
                String payload = PacketHelper.getPayload(packet).trim();
                terminal.println("Send Failed. Controller says:");
                terminal.println("Notifying endpoint ... ");

                try {
                    DatagramPacket error = PacketHelper.createPacket(payload, srcPort, toSendSrcEndpoint,
                            PacketContent.TYPE_ERROR);
                    error.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, PacketHelper.getPacketDstPort(error)));
                    socket.send(error);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                awaitingFlowMod = false;
                toSend = null;
            }
        }
    }

    private void initialiseHashmap() {
        // Endpoint port (int) | Next Hop port (int)
//        if (srcPort!=50009) {
//            flowTable.put(new Integer(50009), new Integer(srcPort + 1));
//        }
    }

    private void initialiseConnectedEndpoints() {
        //Hardcoded as per the diagram for the assignment
        if (srcPort == 50007) {     //R1 @ 50007
            connectedEndpoints.add(new Integer(50001));
        } else if (srcPort == 50014) {
            connectedEndpoints.add(new Integer(50002));
        }
    }
}
