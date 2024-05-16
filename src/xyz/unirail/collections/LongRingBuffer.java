package xyz.unirail.collections;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A class that implements a ring buffer for integers.
 */
public class LongRingBuffer {
	// Buffer to hold the integers
	private final    long[] buffer;
	// Mask for bitwise operations
	private final    int           mask;
	// Lock for synchronization
	private volatile int    lock = 0;
	// Pointer for get operations
	private volatile long          get  = Long.MIN_VALUE;
	// Pointer for put operations
	private volatile long          put  = Long.MIN_VALUE;
	
	/**
	 * Constructor that initializes the buffer and mask.
	 * @param power_of_2 The size of the buffer as a power of 2.
	 */
	public LongRingBuffer( int power_of_2 ) { buffer = new long[(mask = (1 << power_of_2) - 1) + 1]; }
	
	/**
	 * Method to get the length of the buffer.
	 * @return The length of the buffer.
	 */
	public int length() { return buffer.length; }
	
	/**
	 * Method to get the size of the buffer.
	 * @return The size of the buffer.
	 */
	public int size()   { return (int) (put - get); }
	
	/**
	 * Method to get an item from the buffer in a multithreaded environment.
	 * @param return_this_value_if_no_items The value to return if there are no items in the buffer.
	 * @return The item from the buffer, or the specified value if there are no items.
	 */
	public long get_multithreaded( long return_this_value_if_no_items ) {
		
		while( !lock_update.compareAndSet( this, 0, 1 ) ) Thread.onSpinWait();
		
		long ret = get( return_this_value_if_no_items );
		
		lock_update.set( this, 0 );
		return ret;
	}
	/**
	 * Method to get an item from the buffer.
	 * @param return_this_value_if_no_items The value to return if there are no items in the buffer.
	 * @return The item from the buffer, or the specified value if there are no items.
	 */
	public long get( long return_this_value_if_no_items ) { return get == put ? return_this_value_if_no_items : buffer[(int) (get++) & mask] & (~0L); }
	
	/**
	 * Method to put an item in the buffer in a multithreaded environment.
	 * @param value The value to put in the buffer.
	 * @return True if the value was put in the buffer, false if the buffer is full.
	 */
	public boolean put_multithreaded( long value ) {
		if( size() + 1 == buffer.length ) return false;
		
		while( !lock_update.compareAndSet( this, 0, 1 ) ) Thread.onSpinWait();
		boolean ret = put( value );
		lock_update.set( this, 0 );
		return ret;
	}
	/**
	 * Method to put an item in the buffer.
	 * @param value The value to put in the buffer.
	 * @return True if the value was put in the buffer, false if the buffer is full.
	 */
	public boolean put( long value ) {
		if( size() + 1 == buffer.length ) return false;
		
		buffer[(int) (put++) & mask] = (long) value;
		
		return true;
	}
	// AtomicIntegerFieldUpdater for the lock
	static final AtomicIntegerFieldUpdater<LongRingBuffer > lock_update = AtomicIntegerFieldUpdater.newUpdater( LongRingBuffer .class, "lock" );
	/**
	 * Method to clear the buffer.
	 * @return The cleared buffer.
	 */
	public LongRingBuffer  clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
		return this;
	}
}
