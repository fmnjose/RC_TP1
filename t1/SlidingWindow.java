package t1;

import java.util.LinkedList;
import java.util.List;
import java.net.DatagramPacket;
import java.util.Iterator;

public class SlidingWindow{
    private List<DatagramPacket> sendingQueue;
    private List<Long> sendingTimes;
    private int index;
    private int windowSize;
    private long lastCSeq;
    private long lastSSeq;


    public SlidingWindow(int windowSize){
        this.lastCSeq = 1;
        this.lastSSeq = 0;
        this.index = 0;
        this.windowSize = windowSize;
        this.sendingQueue = new LinkedList<>();
        this.sendingTimes = new LinkedList<>();
    }

    public void addPacket(DatagramPacket packet){
        this.sendingQueue.add(packet);        
        this.sendingTimes.add(System.currentTimeMillis());
    }

    public DatagramPacket getPacket(){
        return sendingQueue.get(index);
    }

    public long getSendTime(){
        return this.sendingTimes.get(0);
    }

    public void setSSeq(long sseq){
        if(this.lastSSeq == -1L)
            this.index = 0;
        else
            this.index = (int)(this.lastSSeq - this.lastCSeq);
        
        this.lastSSeq = sseq;
        
        
        if (sseq == 0){
            this.lastSSeq = this.lastCSeq + this.windowSize;
            this.index = 0;  
        }
    }

    public void removeHead(){
        System.out.println("Removed Head. LAST CSEQ: " + this.lastCSeq);
        this.sendingQueue.remove(0);
        this.sendingTimes.remove(0);
        lastCSeq++;
        if(lastCSeq == lastSSeq)
            lastSSeq = -1L;
    }

    public void incrementIndex(){
        index++;
    }

    public int getCurrentIndex(){
        return index;
    }

    public int getCurrentWindowSize(){
        return windowSize;
    }
    /**
     * 
     * @return Lowest packet number
     */
    public long getLastCSeq(){
        return this.lastCSeq;
    }

    public long getLastSSeq(){
        return this.lastSSeq;
    }
    /**
     *
     * @return Number of packets in the window
     */
    public int getNumberOfPackets(){
        return this.sendingQueue.size();
    }

    public boolean hasSpace(){
        return this.sendingQueue.size() < windowSize;
    }

    public int getMaxIndex(){
		return this.sendingQueue.size() - 1;
    }


    public void printWindow(){
        Iterator it = sendingQueue.iterator();

        int i = 0;

        while(it.hasNext()){
          System.out.println("WINDOW PACKET NUMBER: " + (lastCSeq + i++));
            it.next();
        }
    }
}