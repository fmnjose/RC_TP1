package t1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;


public class FTP19Packet {
//
	// packet layout:
	//      | opcode | cum seq num | seq num | optional payload (java string, data, ...) |
	
	
	// Op Codes (shorts):
	public static final short UPLOAD	= 1;
	public static final short ERROR 	= 2;
	public static final short DATA 	= 3;
	public static final short ACK 	= 4;
	public static final short FIN 	= 6;

	public static final int MAX_FTP19_PACKET_SIZE = (1<<16);// 64K
	private static final int FTP19_FIXED_HEADER = Short.BYTES + Long.BYTES + Long.BYTES;

	protected ByteBuffer bb;

	/**
	 * Constructor for creating a new, initially, empty FTP19Packet
	 * 
	 **/
	public FTP19Packet() {
		bb = ByteBuffer.allocate(MAX_FTP19_PACKET_SIZE);
	}

	/**
	 * Constructor for decoding a byte array as a Ftp19Packet
	 * 
	 **/
	public FTP19Packet(byte[] packet, int length) {
		bb = ByteBuffer.wrap(packet, 0, length);
	}
	

	/**
	 * Puts a FTP19Packet in a DatagramPacket 
	 * (FTP19Packet current position marks its end)
	 * @throws IOException
	 */
	public DatagramPacket toDatagram() throws IOException {
		return new DatagramPacket(bb.array(), bb.position());
	}
	
	/**
	 * Puts a Ftp19Packet in a DatagramPacket with destination filled
	 * (FTP19Packet current position marks its end)
	 * @param dst destination SocketAddress
	 * @throws IOException
	 */
	public DatagramPacket toDatagram(SocketAddress dst) throws IOException {
		return new DatagramPacket(bb.array(), bb.position(), dst);
	}

	/********************  PUTS - adds info to packet current position (starts at 0) **********************/

	/**
	 * Appends a short (2 bytes, in net byte order) to the FTP19Packet
	 * @param	s int from where to get the short (2 bytes) to append 
	 * @return	this packet
	 */
	public FTP19Packet putShort(int s) {
		bb.putShort((short) s);
		return this;
	}
	
	/**
	 * Appends a long (8 bytes, in net byte order) to the FTP19Packet
	 * @param	l long to append 
	 * @return	this packet
	 */
	public FTP19Packet putLong(long l) {
		bb.putLong( l );
		return this;
	}

	/**
	 * Appends a byte to the FTP19Packet
	 * @param	b int from were to get the LSB to append 
	 * @return	this packet
	 */
	public FTP19Packet putByte(int b) {
		bb.put((byte) b);
		return this;
	}

	/**
	 * Appends a Java String to the FTP19Packet 
	 * [does not include any terminator!]
	 * @param	s string to append 
	 * @return	this packet
	 */
	public FTP19Packet putString(String s) {
		bb.put(s.getBytes());
		return this;
	}

	/**
	 * Appends the given byte array to the FTP19Packet
	 * @param	block byte array to append 
	 * @return	this packet 
	 */
	public FTP19Packet putBytes(byte[] block) {
		return this.putBytes( block, block.length);
	}
	
	/**
	 * Appends length bytes from the byte array to the FTP19Packet
	 * @param	block byte array from were to copy 
	 * @param	length numb of bytes to append
	 * @return	this packet 
	 */
	public FTP19Packet putBytes(byte[] block, int length) {
		bb.put(block, 0, length);
		return this;
	}
	
	
	/********************  GETS - gets info from packet current position (starts at 0)**********************/
	
	/**
	 * Gets current position (you probably don't need this)
	 * @return the current used size of the FTP19Packet in bytes
	 */
	protected int getPosition() {
		return bb.position();
	}
	
	/**
	 * Sets current position (you probably don't need this)
	 */
	protected void setPosition(int pos) {
		bb.position(pos);
	}
	
	/**
	 * Gets a short (2 bytes)  stored in net byte order (Big Endian)
	 * @return int with value
	 */
	public int getShort() {
		return bb.getShort();
	}
	
	/**
	 * Gets a long (8 bytes) stored in net byte order (Big Endian)
	 * @return long with value
	 */
	public long getLong() {
		return bb.getLong();
	}
	
	/**
	 * Gets a Java String from FTP19Packet (from current position until end)
	 * @return String
	 */
	public String getString() {
		byte []b = new byte[bb.remaining()];
		bb.get(b);
		return new String( b );
	}
	
	/**
	 * Gets a byte array from FTP19Packet (from current position until end)
	 * @return byte[]
	 */
	public byte[] getBytes() {
		byte []b = new byte[bb.remaining()];
		bb.get(b);
		return b;
	}
	
	
	
	/**
	 * Gets a String representation of this FTP19Packet
	 */
	public String toString() {
		int idx = bb.position();
		bb.position(0);
		StringBuilder sb = new StringBuilder();
		
		try {
			switch (getShort()) {
			case UPLOAD:
				sb.append("UPLOAD<").append(getLong()).append(",")
					.append(getLong()).append(",")
					.append(getString());
				break;
			case ERROR:
				sb.append("ERROR<").append(getLong()).append(",")
					.append(getLong()).append(",")
					.append(getString());
				break;
			case DATA:
				sb.append("DATA<").append(getLong()).append(",")
					.append(getLong()).append(",")
					.append(" : ").append( this.getBytes().length );
				break;
			case ACK:
				sb.append("ACK<").append(getLong()).append(",")
					.append(getLong());
				break;
			case FIN:
				sb.append("FIN<").append(getLong()).append(",")
					.append(getLong());
				break;
			}
		} catch (Exception e) {
			System.err.println(e);
		}
		bb.position(idx);
		return sb.append('>').toString();
	}
}
