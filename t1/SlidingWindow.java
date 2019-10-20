package t1;

import java.util.LinkedList;
import java.util.List;
import java.net.DatagramPacket;

public class SlidingWindow{
    private List<DatagramPacket> sendingQueue;
    private List<Long> sendingTimes;
    private List<Long> resentTimes;
    private int index;
    private int windowSize;
    private long lastCSeq;
    private long lastSSeq;
    private boolean dumped;


    public SlidingWindow(int windowSize){
        this.lastCSeq = 1;
        this.lastSSeq = -1;
        this.index = 0;
        this.windowSize = windowSize;
        this.dumped = false;
        this.sendingQueue = new LinkedList<>();
        this.sendingTimes = new LinkedList<>();
        this.resentTimes = new LinkedList<>();
    }

    public void addPacket(DatagramPacket packet){
        this.sendingQueue.add(packet);        
        this.sendingTimes.add(System.currentTimeMillis());
    }

    public DatagramPacket getPacket(){
        sendingTimes.remove(0);
        sendingTimes.add(System.currentTimeMillis());
        return sendingQueue.get(index);
    }

    public long getSendTime(){
        return this.dumped ? this.resentTimes.get(0) : this.sendingTimes.get(0);
    }

    public void setSSeq(long sseq){
       if (sseq == 0){
            this.index = 0;
            this.lastSSeq = this.lastCSeq + this.sendingQueue.size();
            this.dumped = true;
            this.resentTimes = new LinkedList<>(this.sendingTimes);
        }
        else{
            if(this.lastSSeq == -1L)
                this.index = 0;
            else if(sseq > this.lastSSeq + 1)
                this.index = (int)(this.lastSSeq + 1 - this.lastCSeq);
            else 
                this.index++;

                this.lastSSeq = sseq;
        }
    }

    public void removeHead(){
        this.sendingQueue.remove(0);
        this.sendingTimes.remove(0);
        
        if(!this.resentTimes.isEmpty())
            this.resentTimes.remove(0);
        
        if(resentTimes.isEmpty())
            this.dumped = false;

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
 
    public long getLastCSeq(){
        return this.lastCSeq;
    }

    public long getLastSSeq(){
        return this.lastSSeq;
    }
 
    public int getNumberOfPackets(){
        return this.sendingQueue.size();
    }

    public boolean hasSpace(){
        return this.sendingQueue.size() < windowSize;
    }

    public int getMaxIndex(){
		return this.sendingQueue.size() - 1;
    }
}