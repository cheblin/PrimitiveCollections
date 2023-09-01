package xyz.unirail.collections;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class RingBuffer<T> {
	private final    T[]           buffer;
	private final    int           mask;
	private volatile int    lock = 0;
	private volatile long          get  = Long.MIN_VALUE;
	private volatile long          put  = Long.MIN_VALUE;
	
	public RingBuffer( Class<T> clazz, int power_of_2 ) {
		buffer = Array.get( clazz ).copyOf( null, (mask = (1 << power_of_2) - 1) + 1 );
	}
	
	public int length() { return buffer.length; }
	
	public int size()   { return (int) (put - get); }
	
	public T get_multithreaded( T return_this_value_if_no_items ) {
		
		while( !lock_update.compareAndSet( this, 0, 1 ) ) Thread.onSpinWait();
		
		T ret = get( return_this_value_if_no_items );
		
		lock_update.set( this, 0 );
		return ret;
	}
	public T get( T return_this_value_if_no_items ) { return get == put ? return_this_value_if_no_items : buffer[(int) (get++) & mask]; }
	
	public boolean put_multithreaded( T value ) {
		if( size() + 1 == buffer.length ) return false;
		
		while( !lock_update.compareAndSet( this, 0, 1 ) ) Thread.onSpinWait();
		boolean ret = put( value );
		lock_update.set( this, 0 );
		return ret;
	}
	public boolean put( T value ) {
		if( size() + 1 == buffer.length ) return false;
		
		buffer[(int) (put++) & mask] = value;
		return true;
	}
	static final AtomicIntegerFieldUpdater<RingBuffer > lock_update = AtomicIntegerFieldUpdater.newUpdater( RingBuffer .class, "lock" );
	public void clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
	}
}
