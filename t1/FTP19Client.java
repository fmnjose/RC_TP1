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
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;

import lib.Stats;

import static t1.FTP19Packet.*;

public class FTP19Client {

	static final int FTP19_PORT = 9000;

	static final int DEFAULT_TIMEOUT = 500;
	static final int DEFAULT_MAX_RETRIES = 5;
	private static final int DEFAULT_BLOCK_SIZE = 8 * 1024;

	static int windowSize = 1; // this client is a stop and wait one
	static int blockSize = DEFAULT_BLOCK_SIZE;
	static int timeout = DEFAULT_TIMEOUT;

	static Stats stats;
	static SlidingWindow window;
	static BlockingQueue<FTP19Packet> receiverQueue;
	static SocketAddress srvAddress;

	static Semaphore semaphore = new Semaphore(1);

	/**
	 * Receiver thread so that ACKs from server can be received by this at the same
	 * time the main thread is sending data to the server or reading file. ACKs are
	 * added to a shared concurrent queue. The server port is updated if it changes
	 * during upload.
	 */
	static class Receiver implements Runnable {

		DatagramSocket socket;

		Receiver(DatagramSocket sock) {
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
				System.out.println("Receiver done.");
			}
		}
	}

	static class PollingThread implements Runnable {
		PollingThread() {
		}

		public void run() {
			for (;;) {
				try {
					FTP19Packet ack = receiverQueue.poll(timeout, TimeUnit.MILLISECONDS);

					if (ack == null) {
						window.resetIndex(0L);
					} else {
						System.out.println(ack);
						;
						ack.setPosition(2);
						long cpckN = ack.getLong();
						long spckN = ack.getLong();

						if (cpckN < window.getPacketNumber() && window.getResendTrigger() != -1
								&& spckN == window.getResendTrigger())
							window.resetIndex(spckN);
						else
							for (; cpckN >= window.getPacketNumber(); window.removeHead());

					}
				} catch (InterruptedException e) {
					System.out.println(e.getStackTrace());
				}

			}
		}
	}

	/**
	 * Send a block with the filename to the server, repeating until the expected
	 * ACK is received, or the number of allowed retries is exceeded.
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
				if (ack.getShort() == ACK)
					if (expectedACK == ack.getLong()) {
						stats.newRTTMeasure(System.currentTimeMillis() - sendTime); // RTT
						return ack.getLong(); // return sseqN
					} else
						System.err.println("got wrong ack: " + ack);
				else
					System.err.println("got unexpected packet: " + ack);
			else
				System.err.println(expectedACK + " timed out waiting for " + srvAddress);
		}
		throw new IOException("sendRetry: too many retries");
	}

	static FTP19Packet buildUploadPacket(String filename) {
		return new FTP19Packet().putShort(UPLOAD).putLong(0L).putLong(0L).putString(filename);
	}

	static FTP19Packet buildDataPacket(long cseqN, long sseqN, byte[] payload, int length) {
		return new FTP19Packet().putShort(DATA).putLong(cseqN).putLong(sseqN).putBytes(payload, length);
	}

	static FTP19Packet buildFinPacket(long seqN) {
		return new FTP19Packet().putShort(FIN).putLong(seqN).putLong(seqN);
	}

	static void sendFile(String filename) throws Exception {
		try (DatagramSocket socket = new lib.RCDatagramSocket(); // new DatagramSocket(); // for testing use
																	// lib.RCDatagramSocket();
				FileInputStream f = new FileInputStream(filename)) {
			// for statistics
			stats = new Stats(windowSize, timeout);

			// create concurrent producer/consumer queue for ACKs
			receiverQueue = new ArrayBlockingQueue<>(windowSize);
			// start a receiver process to feed the queue
			window = new SlidingWindow(windowSize);

			new Thread(new Receiver(socket)).start();

			int maxbs = (int) sendFilename(socket, buildUploadPacket(filename), 0L, DEFAULT_MAX_RETRIES);
			blockSize = Math.min(maxbs, blockSize);
			reliableSend(f, socket);
			stats.printReport();

		}
	}

	// Pus em metodo a parte para ser mais digerivel (menos feio)
	/*
	 * private static void reliableSend(FileInputStream f, DatagramSocket socket){
	 * byte[] buffer = new byte[blockSize]; int n; boolean done_reading = false,
	 * done = false; FTP19Packet packet; DatagramPacket pckt; long seqN = 1L; //
	 * data block count starts at 1
	 * 
	 * //sliding window ca dentro porque nao faz sentido nao estar window = new
	 * SlidingWindow(windowSize);
	 * 
	 * //Sending the first <window_size> packets try{ for(int i = 0; i < windowSize;
	 * i++){ n = f.read(buffer); pckt = buildDataPacket(seqN, 0L, buffer,
	 * n).toDatagram(srvAddress); seqN ++; socket.send(pckt);
	 * stats.newPacketSent(n); window.addPacket(pckt); if(n < blockSize){
	 * done_reading = true; break; } }
	 * 
	 * //Loops until done for(;!done;){ try{ packet = receiverQueue.poll(timeout,
	 * TimeUnit.MILLISECONDS);
	 * 
	 * packet.setPosition(2); if(packet.getLong() < window.getPacketNumber()){
	 * dumpWindow(socket); }
	 * 
	 * packet.setPosition(2); while(packet.getLong() >= window.getPacketNumber()){
	 * packet.setPosition(2); System.out.println("PACKET NUMBER: " +
	 * packet.getLong() + "||||| WINDOW NUMBER: " + window.getPacketNumber());
	 * 
	 * if(!done_reading){ n = f.read(buffer); //se n e 0 entao tempo de enviar o
	 * packet do fim if(n == -1){ done_reading = true; pckt =
	 * buildFinPacket(seqN).toDatagram(srvAddress); stats.newPacketSent(0); }else{
	 * pckt = buildDataPacket(seqN,0L,buffer,n).toDatagram(srvAddress);
	 * stats.newPacketSent(n); }
	 * 
	 * socket.send(pckt); window.ack(pckt); //se ja acabou de ler e so fazer acks
	 * sem adicionar ficheiros //just smile and wave boys }else{ window.ack(); //se
	 * ja nao ha nada para levar ack, were done lads if(window.getNumberOfPackets()
	 * == 0) done = true;
	 * 
	 * } seqN++; packet.setPosition(2); } }catch(InterruptedException e){
	 * dumpWindow(socket); } } }catch(IOException e){
	 * System.err.println("IOException during send"); } }
	 */

	private static void reliableSend(FileInputStream f, DatagramSocket socket) {
		byte buffer[] = new byte[blockSize];
		int n;
		FTP19Packet packet;
		DatagramPacket pckt;
		long seqN = 1L;
		int maxIndex;
		boolean doneReading = false;

		new Thread(new PollingThread()).start();

		for (;;) {
			try {
				if (window.hasSpace() && !doneReading) {
					System.out.println("Ola bom dia");
					n = f.read(buffer);
					if (n != -1) {
						pckt = buildDataPacket(seqN, 0L, buffer, n).toDatagram(srvAddress);
						window.addPacket(pckt);
						socket.send(pckt);
						System.out.println("Sent new packet : " + seqN);
						seqN++;
					}

					if (n < blockSize) {
						doneReading = true;
						System.out.println("n menor que blocksize");
					}

				} else if (window.getNumberOfPackets() != 0) {

					while (window.getCurrentIndex() < window.getMaxIndex(doneReading)) {
						window.incrementIndex();
						socket.send(window.getPacket());
						System.out
								.println("Sent packet: " + (int) (window.getPacketNumber() + window.getCurrentIndex()));
					}
				} else
					break;

			} catch (IOException e) {
				System.out.println(e.getStackTrace());
			}
		}

		try {
			System.out.println("Bom dia");
			pckt = buildFinPacket(seqN).toDatagram(srvAddress);
			window.addPacket(pckt);

			while (window.getCurrentIndex() != window.getNumberOfPackets() - 1) {
				window.incrementIndex();
				socket.send(window.getPacket());
			}
		} catch (IOException e) {
			System.out.println(e.getStackTrace());
		}
	}

	/*
	 * private static void dumpWindow(DatagramSocket socket) { try {
	 * System.out.println("DUMP"); Iterator<DatagramPacket> itera =
	 * window.getPackets(); while (itera.hasNext()) { DatagramPacket p =
	 * itera.next(); System.out.println("DATAGRAM NUMBER: " + (byte)
	 * p.getData()[9]); socket.send(p); } } catch (IOException e) {
	 * System.out.println("IOException. Resending window"); dumpWindow(socket); } }
	 */

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
				if (blockSize <= 0 || blockSize > MAX_FTP19_PACKET_SIZE)
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
			System.err.printf("usage: java " + stk[stk.length - 1].getClassName()
					+ " filename server [ blocksize [ windowsize ]]\n");
			System.exit(1);
		}
	}

}
