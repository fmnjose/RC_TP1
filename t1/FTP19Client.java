package t1;


/**
 * FTP19Client Stop&Wait - File transfer protocol 2019 edition - RC FCT/UNL
 **/

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import lib.Stats;
import sun.nio.cs.ext.PCK;

import static t1.FTP19Packet.*;


public class FTP19Client {

	static final int FTP19_PORT = 9000;

	static final int DEFAULT_TIMEOUT = 500;
	static final int DEFAULT_MAX_RETRIES = 5;
	private static final int DEFAULT_BLOCK_SIZE = 8*1024;

	static int windowSize = 1; // this client is a stop and wait one
	static int blockSize = DEFAULT_BLOCK_SIZE;
	static int timeout = DEFAULT_TIMEOUT;

	static Stats stats;
	static Queue<DatagramPacket> sendQueue;
	static BlockingQueue<FTP19Packet> receiverQueue;
	static SocketAddress srvAddress;

	/**
	 * Receiver thread so that ACKs from server can be received by this
	 * at the same time the main thread is sending data to the server or
	 * reading file. ACKs are added to a shared concurrent queue.
	 * The server port is updated if it changes during upload.
	 */
	static class Receiver implements Runnable {

		DatagramSocket socket;

		Receiver( DatagramSocket sock ) {
			socket = sock;
		}

		public void run() {
			try {
				for (;;) {
					byte[] buffer = new byte[MAX_FTP19_PACKET_SIZE];
					DatagramPacket msg = new DatagramPacket(buffer, buffer.length);
					socket.receive(msg);
					// update server address (it changes when the reply to UPLOAD 
					// comes from a different port)
					srvAddress = msg.getSocketAddress();
					// make the packet available to sender process
					FTP19Packet pkt = new FTP19Packet(msg.getData(), msg.getLength());
					receiverQueue.put(pkt);
				}
			} catch (Exception e) {
				System.out.println("Receiver done.");			}
		}
	}


	/**
	 * Send a block with the filename to the server, repeating until the expected ACK is received, 
	 * or the number of allowed retries is exceeded.
	 * 
	 * @return Server's block size
	 */
	static long sendFilename(DatagramSocket socket, FTP19Packet pkt, long expectedACK, int retries) throws Exception {
		DatagramPacket dgpkt = pkt.toDatagram(srvAddress);
		for (int i = 0; i < retries; i++) {
			long sendTime = System.currentTimeMillis();

			socket.send(dgpkt);

			FTP19Packet ack = receiverQueue.poll(timeout, TimeUnit.MILLISECONDS);
			if (ack != null)
				if (ack.getShort() == ACK )
					if (expectedACK == ack.getLong()) {
						stats.newRTTMeasure(System.currentTimeMillis() - sendTime); // RTT
						return ack.getLong(); // return sseqN
					} else 
						System.err.println("got wrong ack: "+ack);
				else
					System.err.println("got unexpected packet: "+ack);
			else
				System.err.println(expectedACK+" timed out waiting for "+srvAddress);
		}
		throw new IOException("sendRetry: too many retries");
	}

	static FTP19Packet buildUploadPacket(String filename) {
		return new FTP19Packet()
				.putShort(UPLOAD).putLong(0L).putLong(0L).putString(filename);
	}

	static FTP19Packet buildDataPacket(long cseqN, long sseqN, byte[] payload, int length) {
		return new FTP19Packet()
				.putShort(DATA).putLong(cseqN).putLong(sseqN).putBytes(payload, length);
	}

	static FTP19Packet buildFinPacket(long seqN) {
		return new FTP19Packet()
				.putShort(FIN).putLong(seqN).putLong(seqN);
	}	 


	static void sendFile(String filename) throws Exception {
		try (DatagramSocket socket = new DatagramSocket();    // for testing use lib.RCDatagramSocket();
				FileInputStream f = new FileInputStream(filename)) {
			// for statistics
			stats = new Stats(windowSize, timeout);

			// create concurrent producer/consumer queue for ACKs
			receiverQueue = new ArrayBlockingQueue<>(1);
			// start a receiver process to feed the queue
			new Thread(new Receiver( socket )).start();

			System.out.println("sending file: \"" + filename + "\" to server: " + srvAddress);

			int maxbs = (int)sendFilename(socket, buildUploadPacket(filename), 0L, DEFAULT_MAX_RETRIES);
			blockSize = Math.min(maxbs,  blockSize);
			System.out.println("continuing to server: "+srvAddress+" with blocksize: "+blockSize);

			long seqN = 1L; // data block count starts at 1
			byte[] buffer = new byte[blockSize];
			int n;
			boolean done = false;
			FTP19Packet pckt;
			// read and send blocks
			for(int i = 0; i < windowSize; i++){
				n = f.read(buffer);
				pckt = buildDataPacket(seqN, 0L, buffer, n);
				socket.send(pckt.toDatagram(srvAddress));
				sendQueue.add(pckt.toDatagram(srvAddress));
				if(n < blockSize){
					done = true;
					break;
				}
			}

			for(;;){
				try{
				pckt = receiverQueue.poll(timeout, TimeUnit.MILLISECONDS);
				}catch(InterruptedException e){
					for(int j = 0; j < sendQueue.size(); j++){
					pckt = sendQueue.poll();
					socket.send(pckt.to);
					sendQueue.add(pckt);
					}

				}
			}
			// send the FIN packet
			FTP19Packet pk = buildFinPacket(seqN);
			System.out.println("sending: " + pk);
			sendFilename(socket, pk, seqN, DEFAULT_MAX_RETRIES);

			System.out.println("transfer finished.");
			stats.printReport();

		}
	}



	/**** MAIN ****/

	public static void main(String[] args) throws Exception {
		try {
			switch (args.length) {
			case 4:
				windowSize = Integer.parseInt(args[3]);
				// windowSize must be at least 1 
				if (windowSize <= 0)
					throw new Exception("wrong window size");
			case 3:
				blockSize = Integer.parseInt(args[2]);
				// blockSize must be at least 1 and less than MAX_FTP19_PACKET_SIZE
				if (blockSize <= 0 || blockSize>MAX_FTP19_PACKET_SIZE)
					throw new Exception("wrong block size");
			case 2:
				break;
				
			default:
				throw new Exception("bad parameters");
			}

			String filename = args[0];
			srvAddress = new InetSocketAddress(InetAddress.getByName(args[1]), FTP19_PORT);
			sendFile(filename);

		} catch (IOException x) {
			System.err.println(x);
			System.exit(1);
		} catch (Exception x) {
			System.err.println(x);
			StackTraceElement[] stk = x.getStackTrace();
			System.err.printf("usage: java "+stk[stk.length-1].getClassName()+" filename server [ blocksize [ windowsize ]]\n");
			System.exit(1);
		}
	}

}


