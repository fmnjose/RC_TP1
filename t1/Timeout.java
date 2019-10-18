package t1;

import java.util.List;
import java.util.LinkedList;

public class Timeout{
    long cTimeout, maxSize, defaultTimeout;
    LinkedList<Long> rtts;


    public Timeout(long defaultTimeout, int windowSize){
        this.defaultTimeout = defaultTimeout;
        this.cTimeout = 0;
        this.rtts = new LinkedList<>();
        this.maxSize = windowSize;
    }

    public void packetReceived(long rtt){
        if (this.rtts.size() == this.maxSize)
            this.cTimeout -= this.rtts.poll();

        this.cTimeout += rtt;
        this.rtts.push(rtt);
    }

    public long getTimeout(){
        if(this.rtts.size() != this.maxSize)
            return this.defaultTimeout;
        else{
            return (this.cTimeout / this.maxSize);
        }
    }
}