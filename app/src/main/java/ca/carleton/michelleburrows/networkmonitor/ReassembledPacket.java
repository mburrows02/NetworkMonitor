package ca.carleton.michelleburrows.networkmonitor;

/**
 * Created by Michelle on 4/9/2015.
 */
public class ReassembledPacket {
    int firstId;
    int nextId;
    String src;
    String dst;
    byte[] data;

    public ReassembledPacket(int firstId, String src, String dst, byte[] firstSegment) {
        this.firstId = firstId;
        this.nextId = firstId+1;
        this.src = src;
        this.dst = dst;
        this.data = firstSegment.clone();
    }

    public void addData(byte[] nextSegment) {
        byte[] newData = new byte[data.length + nextSegment.length];
        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(nextSegment, 0, newData, data.length, nextSegment.length);
        data = newData;
        ++nextId;
    }

    public int getFirstId() {
        return firstId;
    }

    public int getNextId() {
        return nextId;
    }

    public String getSrc() {
        return src;
    }

    public String getDst() {
        return dst;
    }

    public byte[] getData() {
        return data;
    }

}

