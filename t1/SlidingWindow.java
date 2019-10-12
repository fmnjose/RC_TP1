package t1;

import java.util.LinkedList;
import java.util.List;
import java.net.DatagramPacket;
import java.util.Iterator;

public class SlidingWindow{
    private int windowSize;
    private long packetNumber;
    private List<DatagramPacket> sendingQueue;
    private int index;
    private long resendTrigger; //sseq do pacote que despoletou o resend


    public SlidingWindow(int windowSize){
        this.packetNumber = 1;
        this.index = -1;
        this.windowSize = windowSize;
        sendingQueue = new LinkedList<>();
        this.resendTrigger = -1L;
    }

    public synchronized void addPacket(DatagramPacket packet){
        this.sendingQueue.add(packet);
        if(index < windowSize - 1)
            index++;
        
    }

    public synchronized DatagramPacket getPacket(){
        return sendingQueue.get(index);
    }

    public synchronized void resetIndex(long sseq){
        index = -1;

        if(resendTrigger == -1)
            resendTrigger = sseq;
    }

    public synchronized void removeHead(){
        sendingQueue.remove(0);
        packetNumber++;
        index--;
        resendTrigger = -1L;
    }

    public synchronized void incrementIndex(){
        index++;
    }

    public synchronized int getCurrentIndex(){
        return index;
    }

    public synchronized int getCurrentWindowSize(){
        return windowSize;
    }
    /**
     * 
     * @return Lowest packet number
     */
    public synchronized long getPacketNumber(){
        return packetNumber;
    }

    /**
     *
     * @return Number of packets in the window
     */
    public synchronized int getNumberOfPackets(){
        return this.sendingQueue.size();
    }

    public synchronized boolean hasSpace(){
        return this.sendingQueue.size() < windowSize;
    }

    public synchronized long getResendTrigger(){
        return resendTrigger;
    }
}