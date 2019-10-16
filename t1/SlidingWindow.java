package t1;

import java.util.LinkedList;
import java.util.List;
import java.net.DatagramPacket;
import java.util.Iterator;

public class SlidingWindow{
    private List<DatagramPacket> sendingQueue;
    private int index;
    private int windowSize;
    private long lastCSeq;
    private long lastSSeq;


    public SlidingWindow(int windowSize){
        this.lastCSeq = 1;
        this.index = -1;
        this.windowSize = windowSize;
        sendingQueue = new LinkedList<>();
    }

    public synchronized void addPacket(DatagramPacket packet){
        this.sendingQueue.add(packet);        
    }

    public synchronized DatagramPacket getPacket(){
        return sendingQueue.get(index);
    }

    public synchronized void resetIndex(long initialIndex,long sseq){
        if(sseq == -1)
            lastSSeq = this.lastCSeq + sendingQueue.size() + 1;
        else
            lastSSeq = sseq;

        this.index = (int)(initialIndex % windowSize);

    }

    public synchronized void removeHead(){
        sendingQueue.remove(0);
        lastCSeq++;
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
    public synchronized long getLastCSeq(){
        return this.lastCSeq;
    }

    public synchronized long getLastSSeq(){
        return this.lastSSeq;
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

    public synchronized int getMaxIndex(){
		return this.sendingQueue.size() - 1;
    }
}