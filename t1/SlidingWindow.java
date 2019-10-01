package t1;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue; 
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class SlidingWindow{
    int windowSize;
    Queue<DatagramPacket> sendingQueue;
    DatagramSocket socket;
    public SlidingWindow(DatagramSocket socket, int windowSize){
        this.socket = socket;
        this.windowSize = windowSize;
        sendingQueue = new ArrayBlockingQueue<>(windowSize);
    }

    public void addPacket(DatagramPacket packet){
        sendingQueue.add(packet);
        removeFromSendingQueue();
    }

    private void removeFromSendingQueue(){
        sendingQueue.remove();
    }


}