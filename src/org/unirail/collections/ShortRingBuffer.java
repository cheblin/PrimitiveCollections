package org.unirail.collections;

import java.util.concurrent.atomic.AtomicBoolean;

public class ShortRingBuffer {
	private final    short[] buffer;
	private final    int           mask;
	private final    AtomicBoolean lock = new AtomicBoolean();
	private volatile long          get  = Long.MIN_VALUE;
	private volatile long          put  = Long.MIN_VALUE;
	
	public ShortRingBuffer( int power_of_2 ) { buffer = new short[(mask = (1 << power_of_2) - 1) + 1]; }
	
	public int length() { return buffer.length; }
	
	public int size()   { return (int) (put - get); }
	
	public long get_multithreaded( long return_this_value_if_no_items ) {
		
		while( !lock.compareAndSet( false, true ) ) Thread.yield();
		
		long ret = get( return_this_value_if_no_items );
		
		lock.set( false );
		return ret;
	}
	public long get( long return_this_value_if_no_items ) { return get == put ? return_this_value_if_no_items : buffer[(int) (get++) & mask] & (~0L); }
	
	public boolean put_multithreaded( short value ) {
		if( size() + 1 == buffer.length ) return false;
		
		while( !lock.compareAndSet( false, true ) ) Thread.yield();
		boolean ret = put( value );
		lock.set( false );
		return ret;
	}
	public boolean put( short value ) {
		if( size() + 1 == buffer.length ) return false;
		
		buffer[(int) (put++) & mask] = (short) value;
		
		return true;
	}
	
	public void clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
	}
}
