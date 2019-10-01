package lib;
/**
 * RCDatagramSocket - DatagramSocket replacement for testing - RC FCT/UNL 2019
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;


public class RCDatagramSocket extends DatagramSocket {
	private static final int PACK_LOSS = 1; // PACK_LOSS/200
	private static final int MAXRTT = 30; 	// ms
	
	private static Random rand = null;
	private static final int seed = 0; // random seed
	
	private long startTime = 0;
	private long psent = 0;
	private long prec = 0;
	private long plost = 0;

	/**
	 * Unreliable DatagramSocket even in local communications
	 * @throws SocketException
	 */
	public RCDatagramSocket() throws SocketException {
		super();
		if ( rand==null) rand = new Random(seed);
		startTime = System.currentTimeMillis();
	}
	
	/**
	 * Unreliable DatagramSocket even in local communications
	 * @throws SocketException
	 */
	public RCDatagramSocket( int port ) throws SocketException {
		super( port );
		if ( rand==null) rand = new Random(seed);
		startTime = System.currentTimeMillis();
	}
	
	
	/**
	 * send DatagraPacket, but not always
	 */
	public void send( DatagramPacket p ) throws IOException {
		if ( rand.nextInt( 200 ) > PACK_LOSS ) {
			super.send( p );
			psent++;
		}
		else plost++;
		//else System.out.println("lost packet");
	}
	
	/**
	 * receive DatagramPacket with delay
	 */
	public void receive( DatagramPacket p ) throws IOException {
		rttdelay();
		super.receive(p);
		prec++;
	}


	/**
	 * 
	 */
	private void rttdelay() {
		try {
			long t = System.currentTimeMillis()%60000;
			t = (long)(((1.0+Math.sin(t/10000.0))/2.0) * MAXRTT);
			Thread.sleep(1+t);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * close socket and print stats report
	 */
	public void close() {
		super.close();
		printReport();
	}
	
	
	private void printReport() {
		// compute time spent 
		int milliSeconds = (int) (System.currentTimeMillis() - startTime);
		//float speed = (float) (totalBytes * 8.0 / milliSeconds / 1000); // M bps
		System.out.println("\nRCDatagramSocket stats: ------");
		System.out.printf("Transfer time:\t %.3f s\n", (float) milliSeconds / 1000);
		System.out.println("Total packets sent:\t " + psent);
		System.out.println("Packets sent but lost:\t " + plost);
		System.out.println("Packets received:\t " + prec);
		System.out.println("-------------------------------\n");
	}
}
