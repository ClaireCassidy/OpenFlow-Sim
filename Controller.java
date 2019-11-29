import tcdIO.Terminal;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.*;

public class Controller extends Node {

    Terminal terminal;
    private static final int SRC_PORT = 50000;
    boolean quit = false;

    //adjacency matrix (https://www.baeldung.com/java-graphs)
    // adjancencyMatrix[a][b] == "Is there a direct link between router a and router b?"
    private boolean[][] adjacencyMatrix = new boolean[8][8];

    // all port numbers here are implicitly router ports
    private ArrayList<Integer> connectedRouterPorts = new ArrayList<Integer>();
    private HashMap<Integer, ArrayList<Integer>> endpointToRouterMap = new HashMap<>();

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {
        terminal.println("Controller received packet from " + PacketHelper.getPacketSrcPort(packet));
        int msgType = PacketHelper.getMsgType(packet);
        terminal.println("Message has type: " + PacketHelper.getMsgTypeAsString(msgType));

        //if this is a hello packet from a router ...
        if (PacketHelper.getMsgType(packet) == PacketHelper.TYPE_HELLO) {
            int receivedPacketSrcPort = PacketHelper.getPacketSrcPort(packet);
            //terminal.println("Received hello message from Router @ " + receivedPacketSrcPort);
            terminal.println("Sending hello message to Router @ " + receivedPacketSrcPort);

            try {
                //send hello message back
                DatagramPacket hello = PacketHelper.createPacket(null, SRC_PORT, receivedPacketSrcPort,
                        PacketContent.TYPE_HELLO);
                hello.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, receivedPacketSrcPort));
                socket.send(hello);

                //send Feature Request
                DatagramPacket featureRequest = PacketHelper.createPacket(null, SRC_PORT, receivedPacketSrcPort,
                        PacketContent.TYPE_FEATURE_REQUEST);
                featureRequest.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, receivedPacketSrcPort));
                socket.send(featureRequest);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msgType == PacketContent.TYPE_FEATURE_RESPONSE) {
            System.out.println("Received feature response");
            String payload = PacketHelper.getPayload(packet);
            String[] args = payload.split(",");         //separate the arguments; args[0] == router port no.,
            // rest are the endpoints its directly connected to
//            for (String s:args) {
//                terminal.println(s);
//                System.out.println(s);
//            }

            Integer routerPortNo = Integer.parseInt(args[0].trim());

            //Router 1 is at 50007; row/col n of adjacency matrix maps to Router (RouterNo%50007)
            int adjacencyMatrixKey = routerPortNo % MIN_ROUTER_PORTNO;
            if (adjacencyMatrixKey > 7 || adjacencyMatrixKey < 0) { //only 8 routers
                System.out.println("ERROR");
                terminal.println("ERROR");
            }

            //add the router to the list of connected routers
            connectedRouterPorts.add(routerPortNo);

            for (int i = 1; i < args.length; i++) { // put the remaining endpoints into the hashmap
                Integer curEndpoint = Integer.parseInt(args[i].trim());
                ArrayList<Integer> directlyConnectedEndpoints;
                if (!endpointToRouterMap.containsKey(curEndpoint)) {    // if we're creating a new entry in the hashmap for this endpoint
                    System.out.println("Adding new Endpoint key " + curEndpoint + " with router no. " + routerPortNo);
                    directlyConnectedEndpoints = new ArrayList<>();
                    directlyConnectedEndpoints.add(routerPortNo);
                    endpointToRouterMap.put(curEndpoint, directlyConnectedEndpoints);
                } else {    // hashmap already has an entry for this endpoint, add router port number to the list of directly connected endpoints
                    System.out.println("Appending " + routerPortNo + " to the list of connected routers for " + curEndpoint);
                    directlyConnectedEndpoints = endpointToRouterMap.get(curEndpoint);  //get reference to the endpoint's list
                    directlyConnectedEndpoints.add(routerPortNo);                   // add the router
                }
            }

            System.out.println("Current Endpoint-to-Router(s) hashmap status: ");
            for (Integer endpoint : endpointToRouterMap.keySet()) {
                System.out.print(endpoint + ": <");
                for (Integer i : endpointToRouterMap.get(endpoint)) {
                    System.out.print(" " + i + " ");
                }
                System.out.print(">\n");
            }
        } else if (msgType == PacketContent.TYPE_FLOW_REQUEST) {
            int startPort = PacketHelper.getPacketSrcPort(packet);
            int endPort = Integer.parseInt(PacketHelper.getPayload(packet).trim());
            ArrayList<Integer> path = generatePath(startPort, endPort); // store a list of ports as the path
            if (path != null) { // if we were able to generate a valid path
                //terminal.println("GOT IT");
                //generatePath(50007, 50002);

                //send the appropriate flow mod message to each of the routers on the path
                DatagramPacket flowMod;
                for (int i = 0; i < path.size(); i++) {
                    //flow table information is given in the form "[endpointPort],[nextHopPort]".
                    //Where the router receiving the packet is the last hop on the path, [nextHopPort] will be "null"
                    //it will then send the message on to [endpointPort], which it will be directly connected to
                    int routerPortNo = path.get(i);
                    int nextHopPort = (i == path.size() - 1 ? -1 : path.get(i + 1));
                    String payload = endPort + "," + (nextHopPort == -1 ? null : nextHopPort);
                    System.out.println("Sending to router @ port " + routerPortNo + ": " + payload);

                    try {
                        flowMod = PacketHelper.createPacket(payload, SRC_PORT, routerPortNo, PacketContent.TYPE_FLOW_MOD);
                        flowMod.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, PacketHelper.getPacketDstPort(flowMod)));
                        socket.send(flowMod);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("CONTROLLER: Path returned null");


            }
        }
    }

    public synchronized void start() throws Exception {
        terminal.println("Controller @ " + SRC_PORT + " starting ... ");
        initialiseAdjacencyMatrix(adjacencyMatrix);
        System.out.println("Initialised Adjacency Matrix:");
        for (boolean[] x : adjacencyMatrix) {
            for (boolean y : x) {
                System.out.print(y + " ");
            }
            System.out.println();
        }

        while (!quit) {
            wait();
        }
        //sleep(3000);
    }

    Controller(Terminal terminal) {
        try {
            this.terminal = terminal;
            socket = new DatagramSocket(SRC_PORT);
            listener.go();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Terminal terminal = new Terminal("Controller @ " + SRC_PORT);
            (new Controller(terminal)).start();
            terminal.println("Program completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    // hard code the routing information for the routing table
    private void initialiseAdjacencyMatrix(boolean[][] adjacencyMatrix) {
        //R1 = {0 1 1 1 0 0 0 0}
        adjacencyMatrix[0][0] = false;
        adjacencyMatrix[0][1] = true;
        adjacencyMatrix[0][2] = true;
        adjacencyMatrix[0][3] = true;
        adjacencyMatrix[0][4] = false;
        adjacencyMatrix[0][5] = false;
        adjacencyMatrix[0][6] = false;
        adjacencyMatrix[0][7] = false;

        //R2 = {1 0 0 0 1 0 0 0}
        adjacencyMatrix[1][0] = true;
        adjacencyMatrix[1][1] = false;
        adjacencyMatrix[1][2] = false;
        adjacencyMatrix[1][3] = false;
        adjacencyMatrix[1][4] = true;
        adjacencyMatrix[1][5] = false;
        adjacencyMatrix[1][6] = false;
        adjacencyMatrix[1][7] = false;

        //R3 = {1 0 0 0 0 1 0 0}
        adjacencyMatrix[2][0] = true;
        adjacencyMatrix[2][1] = false;
        adjacencyMatrix[2][2] = false;
        adjacencyMatrix[2][3] = false;
        adjacencyMatrix[2][4] = false;
        adjacencyMatrix[2][5] = true;
        adjacencyMatrix[2][6] = false;
        adjacencyMatrix[2][7] = false;

        //R4 = {1 0 0 0 0 1 1 0}
        adjacencyMatrix[3][0] = true;
        adjacencyMatrix[3][1] = false;
        adjacencyMatrix[3][2] = false;
        adjacencyMatrix[3][3] = false;
        adjacencyMatrix[3][4] = false;
        adjacencyMatrix[3][5] = true;
        adjacencyMatrix[3][6] = true;
        adjacencyMatrix[3][7] = false;

        //R5 = {0 1 0 0 0 0 1 0}
        adjacencyMatrix[4][0] = false;
        adjacencyMatrix[4][1] = true;
        adjacencyMatrix[4][2] = false;
        adjacencyMatrix[4][3] = false;
        adjacencyMatrix[4][4] = false;
        adjacencyMatrix[4][5] = false;
        adjacencyMatrix[4][6] = true;
        adjacencyMatrix[4][7] = false;

        //R6 = {0 0 1 1 0 0 0 1}
        adjacencyMatrix[5][0] = false;
        adjacencyMatrix[5][1] = false;
        adjacencyMatrix[5][2] = true;
        adjacencyMatrix[5][3] = true;
        adjacencyMatrix[5][4] = false;
        adjacencyMatrix[5][5] = false;
        adjacencyMatrix[5][6] = false;
        adjacencyMatrix[5][7] = true;

        //R7 = {0 0 0 1 1 0 0 1}
        adjacencyMatrix[6][0] = false;
        adjacencyMatrix[6][1] = false;
        adjacencyMatrix[6][2] = false;
        adjacencyMatrix[6][3] = true;
        adjacencyMatrix[6][4] = true;
        adjacencyMatrix[6][5] = false;
        adjacencyMatrix[6][6] = false;
        adjacencyMatrix[6][7] = true;

        //R8 = {0 0 0 0 0 1 1 0}
        adjacencyMatrix[7][0] = false;
        adjacencyMatrix[7][1] = false;
        adjacencyMatrix[7][2] = false;
        adjacencyMatrix[7][3] = false;
        adjacencyMatrix[7][4] = false;
        adjacencyMatrix[7][5] = true;
        adjacencyMatrix[7][6] = true;
        adjacencyMatrix[7][7] = false;
    }

    ArrayList<Integer> generatePath(int startPort, int endpointPort) {
        System.out.println("CONTROLLER: Entered Generate Path");
//        int startIndex = startPort % 50007; //index corresponding to this port in the adjacency matrix
//
//        //get the router connected to the endpoint; will be the end index of our search
//        int endIndex = endpointToRouterMap.get(endpointPort).get(0) % 50007; //our initial approach just gets the first router associated with a given endpoint
//
//        ArrayList<Integer> path = new ArrayList<>();
//
//        //perform a breadth-first search on the adjacency matrix for a path.
//
//        PathNodeQueue nodesToVisit = new PathNodeQueue();
//        //record if we've already been to a node (avoid looping)
//        boolean[][] visitedNode = new boolean[8][8];
//
//        nodesToVisit.enque(new PathNode(startIndex, startIndex, null));
//        visitedNode[startIndex][startIndex] = true;
//
//        boolean nodesLeftToVisit = true;
//        while (nodesLeftToVisit) {
//
//            nodesToVisit.enque();
//        }
        try {
            int endpointRouterPort = endpointToRouterMap.get(endpointPort).get(0); //get the router port directly connected to this endpoint
            int endpointRouterCoord = endpointRouterPort % MIN_ROUTER_PORTNO; // get the corresponding col/row of this router in the adjacency matrix

            // tracks whether a router in the adjacency matrix has been visited yet (prevents loops)
            boolean[] visited = new boolean[8];

            // List of lists: List of paths to consider in the breadth first search;
            // each path is a list of cols (routers) to visit in the given order to represent a valid path to the
            // last router in the list
            // NOTE: instead of storing routers as their port numbers they are stored as their corresponding position
            //      in the adjacency matrix, to avoid constant conversion between port number and coordinates in the
            //      matrix.
            LinkedList<ArrayList<Integer>> bfsPaths = new LinkedList<>(); //breadth first search paths
            ArrayList<Integer> startPath = new ArrayList<>();
            startPath.add(startPort % MIN_ROUTER_PORTNO);
            bfsPaths.add(startPath);
            printBfsPaths(bfsPaths);

            while (!bfsPaths.isEmpty()) { // while there are still paths to consider

                ArrayList<Integer> path = bfsPaths.removeFirst(); //the path to consider adjacent edges for
                System.out.println("CONTROLLER: Removing first item in bfsPaths ... ");
                printBfsPaths(bfsPaths);
                int routerToConsider = path.get(path.size() - 1); //last router in the list is the router to consider
                visited[routerToConsider] = true;              //mark this router as visited
                System.out.println("CONTROLLER: Marking router at coord " + routerToConsider + " visited");

                for (int i = 0; i < 8; i++) {   // go through rows of adjacency matrix at this col
                    // if these two routers are connected and i hasn't been visited
                    if (adjacencyMatrix[routerToConsider][i] == true && visited[i] == false) {
                        if (i == endpointRouterCoord) { // if we've reached the desired router
                            //we're done
                            path.add(i);    //add this router to the path

                            ArrayList<Integer> portPath = new ArrayList<>();
                            //convert each router coordinate to its associated port number
                            for (int n : path) {
                                portPath.add(n + MIN_ROUTER_PORTNO);
                            }

                            System.out.print("\nGOT PATH: <");
                            for (int n : portPath) {
                                System.out.print(n + " ");
                            }
                            System.out.print(">");

                            return portPath;
                        } else { // add to path
                            ArrayList<Integer> newPath = new ArrayList<>();
                            for (int n : path) {
                                newPath.add(n);
                            }
                            newPath.add(i);

                            bfsPaths.add(newPath);
                        }
                    }
                }

            }

            try {
                DatagramPacket error = PacketHelper.createPacket("Error: No path to target endpoint",
                        SRC_PORT, startPort, PacketContent.TYPE_ERROR);
                error.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, PacketHelper.getPacketDstPort(error)));
                socket.send(error);
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (NullPointerException e) {  //will occur if the endpoint passed is not in the list of endpoints
            //send error message
            try {
                DatagramPacket error = PacketHelper.createPacket("Not a registered endpoint in network", SRC_PORT,
                        startPort, PacketContent.TYPE_ERROR);
                error.setSocketAddress(new InetSocketAddress(DEFAULT_DST_NODE, PacketHelper.getPacketDstPort(error)));
                socket.send(error);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    //Test method
    private void printBfsPaths(LinkedList<ArrayList<Integer>> bfsPaths) {

        System.out.println("\nPrinting BFS Paths: ");
        System.out.print("<");
        for (int i = 0; i < bfsPaths.size(); i++) {
            System.out.print("<");
            for (int j = 0; j < bfsPaths.get(i).size(); j++) {
                System.out.print(bfsPaths.get(i).get(j) + ((j == bfsPaths.get(i).size() - 1) ? "" : " "));
            }
            if (i != bfsPaths.size() - 1) {
                System.out.println(">");
            } else {
                System.out.print(">");
            }
        }
        System.out.println(">");


    }
}