//package cs.tcd.ie;

import java.net.DatagramPacket;

public interface PacketContent {

    int PACKET_SIZE = 65536;

    int HEADER_LENGTH =         5;      //frame header length in bytes
    int SRC_PORT_START =        2;      //index in byte array where src-port begins
    int PORT_FIELD_LENGTH =     2;      // length in bytes of port-type entries in frame
                                        // (2 bytes can represent 2^16 values = up to port 65536)
    int DST_PORT_START =        0;      // index in byte array where dst port (endpoint) no starts
    int MSG_TYPE_START =        4;      // index in byte array where msgType info starts
    int MSG_TYPE_LENGTH =       1;      // length in bytes of msg-type info in frame


    // values for the message-type byte

    int TYPE_HELLO =            0;      // 'hello' message; used to establish contact between router
                                        //      and controller
    int TYPE_FEATURE_REQUEST =  1;      // 'Feature Request' message; used to request the port number of the router
                                        //      and information about any endpoints its directly linked to
    int TYPE_FEATURE_RESPONSE = 2;      // 'Feature Response' message; sent by the router on receipt of a 'Feature
                                        //      Request' message. Contains a set of comma-separated values in the
                                        //      payload. The first value is required and is the port of the router
                                        //      sending the message (included since extended implementations may have
                                        //      routers with more than one port). All subsequent values are optional
                                        //      and list the port number(s) of endpoints directly connected to this node.
                                        //      This is used by the controller for generating flows between endpoints.
    int TYPE_MSG =              3;      //  Normal message to be routed to specified endpoint
    int TYPE_FLOW_REQUEST =     4;      //  Message sent from router to broker to ask for routing information on the
                                        //      given endpoint in payload section.
    int TYPE_FLOW_MOD =         5;      //  Send by the controller to modify the flow table of a router. Contains a
                                        //      destination endpoint and the port this router should send the packet
                                        //      to to progress the packet's path through the network. If the packet
                                        //      has a 'null' next router field, then this means that the current
                                        //      router is the last router before the endpoint, and it should send
                                        //      the packet on the the endpoint, which it will be directly connected
                                        //      to.
    int TYPE_ERROR =            6;      //  Indicates an error in one of the operations. This is sent in the following
                                        //      scenarios:
                                        //  -   The Controller receives a Flow Mod Request for an endpoint not
                                        //      registered in the network. This includes when a port number of a router,
                                        //      not an endpoint, is passed.
}