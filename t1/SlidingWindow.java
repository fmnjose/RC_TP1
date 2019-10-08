package t1;

import java.util.LinkedList;
import java.util.List;
import java.net.DatagramPacket;
import java.util.Iterator;

public class SlidingWindow{
    int windowSize;
    long packetNumber;
    List<DatagramPacket> sendingQueue;

    public SlidingWindow(int windowSize){
        packetNumber = 1;
        this.windowSize = windowSize;
        sendingQueue = new LinkedList<>();
    }

    public void addPacket(DatagramPacket packet){
        this.sendingQueue.add(packet);
    }

    public DatagramPacket getPacket(int index){
        return sendingQueue.get(index);
    }


    /**
     * 
     * @return Lowest packet number
     */
    public long getPacketNumber(){
        return packetNumber;
    }

    /**
     *
     * @return Number of packets in the window
     */
    public int getNumberOfPackets(){
        return this.sendingQueue.size();
    }

    // para ser usado quando e ack e ha cenas para enviar
    public void ack(DatagramPacket newPacket){
        this.sendingQueue.remove(0);
        this.packetNumber++;
        this.addPacket(newPacket);
    }

    //ack quando ja se leu tudo
    public void ack(){
        this.sendingQueue.remove(0);
        this.packetNumber++;
    }

    //iteradores sao mel
    public Iterator<DatagramPacket> getPackets(){
        return this.sendingQueue.iterator();
    }
}