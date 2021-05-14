package org.unirail.collections;

import java.util.concurrent.atomic.AtomicLong;

public class IntRingBuffer {
	private final int[] buffer;
	private final long          mask;
	private final AtomicLong    read  = new AtomicLong();
	private final AtomicLong    write = new AtomicLong();
	
	public IntRingBuffer(int power_of_2) { buffer = new int[(int) ((mask = (1L << power_of_2) - 1) + 1)]; }
	
	public int length()                 {return buffer.length;}
	
	public int size()                   {return (int) ((write.get() - read.get()) & mask); }
	
	public long get()                   { return size() == 0 ? Long.MIN_VALUE :  buffer[(int) (read.getAndIncrement() & mask)]; }
	
	public synchronized long get_sync() { return get(); }
	
	public boolean put(int value) {
		if (available() < 1) return false;
		buffer[(int) (write.getAndIncrement() & mask)] = (int) value;
		return true;
	}
	
	public synchronized boolean put_sync(int value) {return put(value);}
	
	public int available()                                  { return (int) (mask + 1 - (write.get() - read.get()));}
	
	public void reset() {
		read.set(0);
		write.set(0);
	}
}
