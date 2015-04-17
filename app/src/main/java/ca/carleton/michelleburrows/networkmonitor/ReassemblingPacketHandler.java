package ca.carleton.michelleburrows.networkmonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.pkts.PacketHandler;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.protocol.Protocol;

/**
 * Packet handler that attempts to reassemble messages that have been split into multiple
 * fragments.
 * Created by Michelle on 4/9/2015.
 */
public class ReassemblingPacketHandler implements PacketHandler {
    List<ReassembledPacket> packets;

    public ReassemblingPacketHandler() {
        packets = new ArrayList<ReassembledPacket>();
    }

    public void nextPacket(Packet arg0) throws IOException {
        if (!arg0.hasProtocol(Protocol.TCP)) {
            System.out.println("Not TCP!");
            return;
        }
        TCPPacket pkt = (TCPPacket) arg0.getPacket(Protocol.TCP);
        int id = pkt.getIdentification();
        if (packets.size() > 0) {
            ReassembledPacket lastPacket = packets.get(packets.size() - 1);
            if (lastPacket.getNextId() == id) {
                lastPacket.addData(pkt.getPayload().getArray());
                return;
            }
        }
        byte[] data = pkt.getPayload().getArray();
        ReassembledPacket packet = new ReassembledPacket(id, pkt.getSourceIP(), pkt.getDestinationIP(), data);
        packets.add(packet);
    }
}
