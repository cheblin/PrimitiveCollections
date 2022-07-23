package org.unirail.collections;

import java.util.concurrent.atomic.AtomicBoolean;

public class UByteRingBuffer {
	private final    byte[] buffer;
	private final    int           mask;
	private final    AtomicBoolean lock = new AtomicBoolean();
	private volatile long          get  = Long.MIN_VALUE;
	private volatile long          put  = Long.MIN_VALUE;
	
	public UByteRingBuffer(int power_of_2) {buffer = new byte[(mask = (1 << power_of_2) - 1) + 1];}
	
	public int length() {return buffer.length;}
	
	public int size()   {return (int) (put - get);}
	
	public long get_multithreaded(long if_empty_then_return_value) {
		
		while (!lock.compareAndSet(false, true)) Thread.yield();
		
		long ret = get(if_empty_then_return_value);
		
		lock.set(false);
		return ret;
	}
	public long get(long if_empty_then_return_value) {return get == put ? if_empty_then_return_value : buffer[(int) (get++) & mask] & (~0L);}
	
	public boolean put_multithreaded(char value) {
		if (size() + 1 == buffer.length) return false;
		
		while (!lock.compareAndSet(false, true)) Thread.yield();
		boolean ret = put(value);
		lock.set(false);
		return ret;
	}
	public boolean put(char value) {
		if (size() + 1 == buffer.length) return false;
		
		buffer[(int) (put++) & mask] = (byte) value;
		
		return true;
	}
	
	public void clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
	}
}
