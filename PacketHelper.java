// helper class for constructing and deconstructing packet information from frames

import java.net.DatagramPacket;

public class PacketHelper implements PacketContent {

    //creates a datagram packet whose buffer contains a frame containing the routing information and payload
    // of the message to be sent
    public static DatagramPacket createPacket(String payload, int srcPort, int dstPort, int msgType) {

        byte[] header = new byte[HEADER_LENGTH];

        // port addresses are in the range >50000, so we need 2 bytes to represent them
        // 16bits = 2 bytes can represent 2^16 values = up to port 65536
        // buf[0] = MSByte of dst address
        // buf[1] = LSByte of dst address
        // 00000000 00000000 xxxxxxxx xxxxxxxx == port num as int
        header[DST_PORT_START+1] = (byte) dstPort;     //truncates to the 8 LSbits of dst Port
        header[DST_PORT_START] = (byte) (dstPort >>> 8); //Right-Shift dstPort by 8 bits; again truncate.

        //buf[2] = MSByte of src address
        //buf[3] = MSByte of dst address
        header[SRC_PORT_START+1] = (byte) srcPort;
        header[SRC_PORT_START] = (byte) (srcPort >>> 8);

        //buf[4] = msgType
        header[MSG_TYPE_START] = (byte) msgType;

        //now insert the payload and header into the packet

        byte[] payloadBytes;
        if (payload != null) {
            payloadBytes = payload.getBytes();
        } else {
            payloadBytes = new byte[0];
        }

        //size of buf = size of packet = header + payload
        byte[] buf = new byte[header.length + payloadBytes.length];

        //copy the header in
        System.arraycopy(header, 0, buf, 0, header.length);
        //copy the payload in
        System.arraycopy(payloadBytes, 0, buf, header.length, payloadBytes.length);

        return new DatagramPacket(buf, buf.length);
    }

    public static int getPacketSrcPort(DatagramPacket packet) {
        byte[] buffer = packet.getData();
        byte[] srcPortBytes = new byte[PORT_FIELD_LENGTH];

        //copy the src port bytes from the datagram buffer
        System.arraycopy(buffer, SRC_PORT_START, srcPortBytes, 0, PORT_FIELD_LENGTH);

        // convert to an int
        int mostSigByte = (srcPortBytes[0]);
        mostSigByte = (mostSigByte << 8) & 0xFFFF;           //assign correct place-value; convert to unsigned
        int leastSigByte = srcPortBytes[1] & 0xFF;          //convert to unsigned
        return mostSigByte + leastSigByte;

    }

    public static int getPacketDstPort(DatagramPacket packet) {
        byte[] buffer = packet.getData();
        byte[] dstPortBytes = new byte[PORT_FIELD_LENGTH];

        //copy the dst port bytes from the datagram buffer
        System.arraycopy(buffer, DST_PORT_START, dstPortBytes, 0, PORT_FIELD_LENGTH);

        // convert to an int
        int mostSigByte = (dstPortBytes[0]);
        mostSigByte = (mostSigByte << 8) & 0xFFFF;           //assign correct place-value; convert to unsigned
        int leastSigByte = dstPortBytes[1] & 0xFF;          //convert to unsigned
        return mostSigByte + leastSigByte;
    }

    public static String getPayload(DatagramPacket packet) {
        byte[] frame = packet.getData();
        byte[] payload = new byte[frame.length - PacketContent.HEADER_LENGTH]; //any bytes not allocated to the frame
        // will be used to store the payload
        //System.out.println(payload.length);
        System.arraycopy(frame, PacketContent.HEADER_LENGTH, payload, 0, payload.length);
        return new String(payload);

    }

    public static int getMsgType(DatagramPacket packet) {
        byte[] data = packet.getData();
        return (data[MSG_TYPE_START] & 0xFF);               //convert to unsigned
    }

    public static String getMsgTypeAsString(int msgType) {
        String result;
        switch (msgType) {
            case (TYPE_HELLO):
                result = "Hello Message";
                break;
            case(TYPE_FEATURE_REQUEST):
                result = "Feature Request";
                break;
            case(TYPE_FEATURE_RESPONSE):
                result = "Feature Response";
                break;
            case (TYPE_MSG):
                result = "Endpoint-to-Endpoint Transmission";
                break;
            case (TYPE_FLOW_REQUEST):
                result = "Flow Table Modification Request";
                break;
            case (TYPE_FLOW_MOD):
                result = "Modification to Flow Table";
                break;
            case (TYPE_ERROR):
                result = "Error!";
                break;
            default:
                result = "Unknown Message Type :(";
        }
        return result;
    }

    //test methods
    public static void main(String[] args) {
        DatagramPacket packet = createPacket("Test", 50001, 50002, TYPE_HELLO);
        System.out.println("Message type: " + getMsgType(packet) + ", to String: " + getMsgTypeAsString(getMsgType(packet)));
        System.out.println("Dst Port: " + getPacketDstPort(packet));
        System.out.println("Src Port: " +getPacketSrcPort(packet));
        System.out.println("Payload: " + getPayload(packet));
    }
}
