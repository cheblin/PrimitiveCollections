package xyz.unirail.collections;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class ByteRingBuffer {
	private final    byte[] buffer;
	private final    int           mask;
	private volatile int    lock = 0;
	private volatile long          get  = Long.MIN_VALUE;
	private volatile long          put  = Long.MIN_VALUE;
	
	public ByteRingBuffer( int power_of_2 ) { buffer = new byte[(mask = (1 << power_of_2) - 1) + 1]; }
	
	public int length() { return buffer.length; }
	
	public int size()   { return (int) (put - get); }
	
	public long get_multithreaded( long return_this_value_if_no_items ) {
		
		while( !lock_update.compareAndSet( this, 0, 1 ) ) Thread.onSpinWait();
		
		long ret = get( return_this_value_if_no_items );
		
		lock_update.set( this, 0 );
		return ret;
	}
	public long get( long return_this_value_if_no_items ) { return get == put ? return_this_value_if_no_items : buffer[(int) (get++) & mask] & (~0L); }
	
	public boolean put_multithreaded( byte value ) {
		if( size() + 1 == buffer.length ) return false;
		
		while( !lock_update.compareAndSet( this, 0, 1 ) ) Thread.onSpinWait();
		boolean ret = put( value );
		lock_update.set( this, 0 );
		return ret;
	}
	public boolean put( byte value ) {
		if( size() + 1 == buffer.length ) return false;
		
		buffer[(int) (put++) & mask] = (byte) value;
		
		return true;
	}
	static final AtomicIntegerFieldUpdater<ByteRingBuffer > lock_update = AtomicIntegerFieldUpdater.newUpdater( ByteRingBuffer .class, "lock" );
	public void clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
	}
}
