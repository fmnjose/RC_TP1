package lib;

public class Stats {
	
	private long totalRtt = 0;
	private int rttMeasured = 0;
	
	private int window;
	private int countWindow = 1;
	private int timeout;
	private int countTimeout = 1;
	
	private int totalPackets = 0;
	private int totalBytes = 0;
	private long startTime = 0L;;

	/**
	 * Create a Stats object to do some statistics and print it
	 * 
	 * @param initialWindow - window size in blocks
	 * @param initialTimeout - used timeout
	 */
	public Stats(int initialWindow, int initialTimeout) {
		window = initialWindow;
		timeout = initialTimeout;
		
		startTime = System.currentTimeMillis();
	}

	/**
	 * count a new packet sent
	 * @param n - size in bytes
	 */
	public void newPacketSent(int n) {
		totalPackets++;
		totalBytes += n;
	}

	/**
	 * count a new RTT measure
	 * @param t - ms
	 */
	public void newRTTMeasure(long t) {
		rttMeasured++;
		totalRtt += t;
	}

	/**
	 * started usind a new timeout
	 * @param t - ms
	 */
	public void newTimeout(int t) {
		countTimeout++;
		timeout += t;
	}
	
	/**
	 * started using a new window size
	 * @param s - num blocks
	 */
	public void newWindowSize(int s) {
		countWindow++;
		window += s;
	}
	
	/**
	 * Print statistics report
	 */
	public void printReport() {
		// compute time spent receiving bytes
		int milliSeconds = (int) (System.currentTimeMillis() - startTime);
		float speed = (float) (totalBytes * 8.0 / milliSeconds / 1000); // M bps
		float averageRtt = (float) (rttMeasured>0 ? (float)totalRtt / rttMeasured : 0.0);
		System.out.println("\nTransfer stats: -----------------------------------------");
		System.out.println("File size (bytes):\t\t" + totalBytes);
		System.out.println("Packets sent:\t\t\t" + totalPackets);
		System.out.printf("End-to-end transfer time:\t%.3f s\n", (float) milliSeconds / 1000);
		System.out.printf("End-to-end transfer speed:\t%.3f M bps\n", speed);
		System.out.printf("Average observed rtt:\t\t%.3f ms\n", averageRtt);
		System.out.printf("Used timeout:\t\t\t%.1f ms\n", timeout/(float)countTimeout);
		System.out.printf("Sending window size:\t\t%.1f packet(s)\n", window/(float)countWindow);
		System.out.println("==========================================================\n");

	}
}