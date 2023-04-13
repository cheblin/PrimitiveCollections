package xyz.unirail.collections;

import java.util.concurrent.atomic.AtomicBoolean;

public class CharRingBuffer {
	private final    char[] buffer;
	private final    int           mask;
	private final    AtomicBoolean lock = new AtomicBoolean();
	private volatile long          get  = Long.MIN_VALUE;
	private volatile long          put  = Long.MIN_VALUE;
	
	public CharRingBuffer( int power_of_2 ) { buffer = new char[(mask = (1 << power_of_2) - 1) + 1]; }
	
	public int length() { return buffer.length; }
	
	public int size()   { return (int) (put - get); }
	
	public long get_multithreaded( long return_this_value_if_no_items ) {
		
		while( !lock.compareAndSet( false, true ) ) Thread.yield();
		
		long ret = get( return_this_value_if_no_items );
		
		lock.set( false );
		return ret;
	}
	public long get( long return_this_value_if_no_items ) { return get == put ? return_this_value_if_no_items : buffer[(int) (get++) & mask] & (~0L); }
	
	public boolean put_multithreaded( char value ) {
		if( size() + 1 == buffer.length ) return false;
		
		while( !lock.compareAndSet( false, true ) ) Thread.yield();
		boolean ret = put( value );
		lock.set( false );
		return ret;
	}
	public boolean put( char value ) {
		if( size() + 1 == buffer.length ) return false;
		
		buffer[(int) (put++) & mask] = (char) value;
		
		return true;
	}
	
	public void clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
	}
}
