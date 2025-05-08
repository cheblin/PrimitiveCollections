//MIT License
//
//Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//GitHub Repository: https://github.com/AdHoc-Protocol
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//1. The above copyright notice and this permission notice must be included in all
//   copies or substantial portions of the Software.
//
//2. Users of the Software must provide a clear acknowledgment in their user
//   documentation or other materials that their solution includes or is based on
//   this Software. This acknowledgment should be prominent and easily visible,
//   and can be formatted as follows:
//   "This product includes software developed by Chikirev Sirguy and the Unirail Group
//   (https://github.com/AdHoc-Protocol)."
//
//3. If you modify the Software and distribute it, you must include a prominent notice
//   stating that you have changed the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package org.unirail.collections;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * A thread-safe, fixed-size ring buffer (circular buffer) for storing primitive values.
 * Optimized for efficient producer-consumer scenarios and bounded circular data storage.
 * The buffer uses atomic operations for thread-safe access and a bitwise mask for efficient index wrapping.
 */
public class ByteRingBuffer {
	/**
	 * The internal array storing the ring buffer's primitive values.
	 */
	private final byte[] buffer;
	
	/**
	 * Bitmask for efficient index wrapping, computed as (1 << capacityPowerOfTwo) - 1.
	 */
	private final int mask;
	
	/**
	 * Lock for thread-safe operations in multi-threaded methods (0 = unlocked, 1 = locked).
	 */
	private volatile int lock = 0;
	
	/**
	 * Atomic updater for thread-safe modifications to the lock field.
	 */
	static final AtomicIntegerFieldUpdater< ByteRingBuffer > lock_update = AtomicIntegerFieldUpdater.newUpdater( ByteRingBuffer.class, "lock" );
	
	/**
	 * Head pointer indicating the next element to read (used for get operations).
	 */
	private volatile long get = Long.MIN_VALUE;
	
	/**
	 * Atomic updater for thread-safe modifications to the get pointer.
	 */
	private static final AtomicLongFieldUpdater< ByteRingBuffer > GET_UPDATER = AtomicLongFieldUpdater.newUpdater( ByteRingBuffer.class, "get" );
	
	/**
	 * Tail pointer indicating the next available slot to write (used for put operations).
	 */
	private volatile long put = Long.MIN_VALUE;
	
	/**
	 * Atomic updater for thread-safe modifications to the put pointer.
	 */
	private static final AtomicLongFieldUpdater< ByteRingBuffer > PUT_UPDATER = AtomicLongFieldUpdater.newUpdater( ByteRingBuffer.class, "put" );
	
	/**
	 * Creates a new ring buffer with a capacity of 2^capacityPowerOfTwo.
	 *
	 * @param capacityPowerOfTwo The power of two defining the buffer's capacity (e.g., 4 for 16 elements).
	 * @throws IllegalArgumentException If capacityPowerOfTwo is negative.
	 */
	public ByteRingBuffer( int capacityPowerOfTwo ) {
		if( capacityPowerOfTwo < 0 ) throw new IllegalArgumentException( "capacityPowerOfTwo must be non-negative" );
		buffer = new byte[ ( mask = ( 1 << capacityPowerOfTwo ) - 1 ) + 1 ];
	}
	
	/**
	 * Returns the fixed capacity of the ring buffer.
	 *
	 * @return The maximum number of elements the buffer can hold.
	 */
	public int length() {
		return buffer.length;
	}
	
	/**
	 * Returns the current number of elements in the ring buffer.
	 *
	 * @return The number of elements currently stored.
	 */
	public int size() {
		return ( int ) ( put - get );
	}
	
	/**
	 * Thread-safely retrieves and removes the next integer from the buffer.
	 *
	 * @param defaultValueIfEmpty Value to return if the buffer is empty.
	 * @return The retrieved integer, or defaultValueIfEmpty if the buffer is empty.
	 */
	public long get_multithreaded( long defaultValueIfEmpty ) {
		long _get;
		
		do
			if( ( _get = get ) == put ) return defaultValueIfEmpty;
		while( !GET_UPDATER.compareAndSet( this, _get, _get + 1 ) );
		
		return buffer[ ( int ) _get & mask ] & ( ~0L );
	}
	
	/**
	 * Retrieves and removes the next integer from the buffer (non-thread-safe).
	 * Use only in single-threaded contexts or with external synchronization.
	 *
	 * @param defaultValueIfEmpty Value to return if the buffer is empty.
	 * @return The retrieved integer, or defaultValueIfEmpty if the buffer is empty.
	 */
	public long get( long defaultValueIfEmpty ) {
		if( get == put ) return defaultValueIfEmpty;
		return buffer[ ( int ) ( get++ ) & mask ] & ( ~0L );
	}
	
	/**
	 * Thread-safely adds an integer to the buffer.
	 *
	 * @param value The primitive to add.
	 * @return True if the value was added, false if the buffer is full.
	 */
	public boolean put_multithreaded( byte value ) {
		long _put;
		
		do {
			_put = put;
			if( size() == buffer.length ) return false;
		}
		while( !PUT_UPDATER.compareAndSet( this, _put, _put + 1 ) );
		
		buffer[ ( int ) _put & mask ] = ( byte ) value;
		return true;
	}
	
	/**
	 * Adds an integer to the buffer (non-thread-safe).
	 * Use only in single-threaded contexts or with external synchronization.
	 *
	 * @param value The primitive to add.
	 * @return True if the value was added, false if the buffer is full.
	 */
	public boolean put( byte value ) {
		if( size() == buffer.length ) return false;
		buffer[ ( int ) ( put++ ) & mask ] = ( byte ) value;
		return true;
	}
	
	/**
	 * Resets the buffer to an empty state (non-thread-safe).
	 * Use with caution in concurrent environments.
	 *
	 * @return This instance for method chaining.
	 */
	public ByteRingBuffer clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
		return this;
	}
}