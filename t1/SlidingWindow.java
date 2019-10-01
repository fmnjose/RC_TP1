package t1;

import java.util.LinkedList;
import java.util.List;
import java.net.DatagramPacket;

public class SlidingWindow{
    int windowSize;
    long packetNumber;
    List<DatagramPacket> sendingQueue;
    List<DatagramPacket> sentQueue;

    public SlidingWindow(int windowSize){
        packetNumber = 1;
        this.windowSize = windowSize;
        sendingQueue = new LinkedList<>();
        sentQueue = new LinkedList<>();
    }

    public void addPacket(DatagramPacket packet){
        sendingQueue.add(packet);
        sendingQueue.remove(0);
    }

    public DatagramPacket sendPacket(int index){
        return sendingQueue.get(0);
    }

    public long getPacketNumber(){
        return packetNumber;
    }

    public void incrementWindow(){
        packetNumber++;
    }

    public void restoreWindow(){
        
    }
}