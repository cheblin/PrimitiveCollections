package org.unirail.collections;

import java.util.concurrent.atomic.AtomicLong;

public class UByteRingBuffer {
	private final byte[] buffer;
	private final long          mask;
	private final AtomicLong    read  = new AtomicLong();
	private final AtomicLong    write = new AtomicLong();
	
	public UByteRingBuffer( int power_of_2 ) { buffer = new byte[(int) ((mask = (1L << power_of_2) - 1) + 1)]; }
	
	public int length()                 {return buffer.length;}
	
	public int size()                   {return (int) ((write.get() - read.get()) & mask); }
	
	public long get()                   { return size() == 0 ? Long.MIN_VALUE : (char)( 0xFFFF &  buffer[(int) (read.getAndIncrement() & mask)]); }
	
	public synchronized long get_sync() { return get(); }
	
	public boolean put( char value ) {
		if (available() < 1) return false;
		buffer[(int) (write.getAndIncrement() & mask)] = (byte)value;
		return true;
	}
	
	public synchronized boolean put_sync( char value ) {return put( value );}
	
	public int available()                                    { return (int) (mask + 1 - (write.get() - read.get()));}
	
	public void reset() {
		read.set( 0 );
		write.set( 0 );
	}
}
