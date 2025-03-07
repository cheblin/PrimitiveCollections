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

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * {@code IntRingBuffer} implements a fixed-size, thread-safe ring buffer (circular buffer) for integer values.
 * It is optimized for efficient buffering and is suitable for producer-consumer scenarios and other applications
 * requiring bounded, circular data storage.
 */
public class LongRingBuffer {
	/**
	 * The underlying integer array that stores the ring buffer data.
	 */
	private final long[] buffer;
	
	/**
	 * Mask for efficient index wrapping using bitwise AND operations.
	 * Calculated as {@code (1 << capacityPowerOfTwo) - 1}.
	 */
	private final int mask;
	
	/**
	 * Lock used for thread-safe operations in multi-threaded methods.
	 * 0 indicates unlocked, 1 indicates locked.
	 */
	private volatile     int                                               lock        = 0;
	/**
	 * Constructs the lock update mechanism for thread-safe updates to the {@link #lock} field.
	 */
	static final         AtomicIntegerFieldUpdater< LongRingBuffer > lock_update = AtomicIntegerFieldUpdater.newUpdater( LongRingBuffer .class, "lock" );
	/**
	 * Pointer to the head of the buffer for get operations.
	 * Represents the index of the next element to be read.
	 */
	private volatile     long                                              get         = Long.MIN_VALUE;
	/**
	 * Atomic updater for the get pointer to ensure thread-safe operations
	 */
	private static final AtomicLongFieldUpdater< LongRingBuffer >    GET_UPDATER = AtomicLongFieldUpdater.newUpdater( LongRingBuffer.class, "get" );
	/**
	 * Pointer to the tail of the buffer for put operations.
	 * Represents the index of the next available slot to write to.
	 */
	private volatile     long                                              put         = Long.MIN_VALUE;
	
	/**
	 * Atomic updater for the put pointer to ensure thread-safe operations
	 */
	private static final AtomicLongFieldUpdater< LongRingBuffer > PUT_UPDATER = AtomicLongFieldUpdater.newUpdater( LongRingBuffer.class, "put" );
	
	/**
	 * Constructs a new {@code IntRingBuffer} with a capacity of 2<sup>{@code capacityPowerOfTwo}</sup>.
	 * The buffer size is fixed at creation.
	 *
	 * @param capacityPowerOfTwo The power of two that determines the buffer's capacity (e.g., 4 for a capacity of 16).
	 *                           Must be a non-negative integer.
	 * @throws IllegalArgumentException if {@code capacityPowerOfTwo} is negative.
	 */
	public LongRingBuffer( int capacityPowerOfTwo ) {
		if( capacityPowerOfTwo < 0 ) throw new IllegalArgumentException( "capacityPowerOfTwo must be non-negative" );
		buffer = new long[ ( mask = ( 1 << capacityPowerOfTwo ) - 1 ) + 1 ];
	}
	
	/**
	 * Returns the fixed capacity (maximum number of elements) of this ring buffer.
	 *
	 * @return The capacity of the ring buffer.
	 */
	public int length() {
		return buffer.length;
	}
	
	/**
	 * Returns the current number of elements in the ring buffer.
	 *
	 * @return The number of elements currently stored in the buffer.
	 */
	public int size() {
		return ( int ) ( put - get );
	}
	
	/**
	 * Retrieves and removes the next integer from the ring buffer in a thread-safe manner.
	 * This method is safe for concurrent access from multiple threads.
	 *
	 * @param defaultValueIfEmpty The value to return if the buffer is empty.
	 * @return The integer value retrieved from the buffer, or {@code defaultValueIfEmpty} if the buffer is empty.
	 */
	public long get_multithreaded( long defaultValueIfEmpty ) {
		long _get;
		long value;
		
		do {
			_get = get;
			if( _get == put ) return defaultValueIfEmpty;
			value = buffer[ ( int ) _get & mask ] & ( ~0L );
		}
		while( !GET_UPDATER.compareAndSet( this, _get, _get + 1 ) );
		
		return value;
	}
	
	/**
	 * Retrieves and removes the next integer from the ring buffer.
	 * <p>
	 * <b>Note:</b> This method is <b>NOT</b> thread-safe and should only be used in single-threaded contexts
	 * or when external synchronization is managed. For thread-safe retrieval, use {@link #get_multithreaded(long)}.
	 *
	 * @param defaultValueIfEmpty The value to return if the buffer is empty.
	 * @return The integer value retrieved from the buffer, or {@code defaultValueIfEmpty} if the buffer is empty.
	 */
	public long get( long defaultValueIfEmpty ) {
		// If get pointer equals put pointer, buffer is empty, return the specified default value.
		if( get == put ) return defaultValueIfEmpty;
		// Retrieve value, increment get pointer, and wrap index using bitmask.
		return buffer[ ( int ) ( get++ ) & mask ] & ( ~0L );
	}
	
	/**
	 * Adds an integer value to the ring buffer in a thread-safe manner.
	 * This method is safe for concurrent access from multiple threads.
	 *
	 * @param value The integer value to add to the buffer.
	 * @return {@code true} if the value was successfully added to the buffer, {@code false} if the buffer is full.
	 */
	public boolean put_multithreaded( long value ) {
		long _put;
		
		do {
			_put = put;
			if( size() + 1 == buffer.length ) return false;
		}
		while( !PUT_UPDATER.compareAndSet( this, _put, _put + 1 ) );
		
		buffer[ ( int ) _put & mask ] = ( long ) value;
		return true;
	}
	
	/**
	 * Adds an integer value to the ring buffer.
	 * <p>
	 * <b>Note:</b> This method is <b>NOT</b> thread-safe and should only be used in single-threaded contexts
	 * or when external synchronization is managed. For thread-safe insertion, use {@link #put_multithreaded}.
	 *
	 * @param value The integer value to add to the buffer.
	 * @return {@code true} if the value was successfully added to the buffer, {@code false} if the buffer is full.
	 */
	public boolean put( long value ) {
		if( size() + 1 == buffer.length ) return false; // Check if buffer is full.
		
		// Put value into buffer, increment put pointer, and wrap index using bitmask.
		buffer[ ( int ) ( put++ ) & mask ] = ( long ) value;
		return true; // Value successfully put into the buffer.
	}
	
	
	/**
	 * Clears the ring buffer, resetting it to an empty state.
	 * This operation is not thread-safe and should be used with caution in concurrent environments.
	 *
	 * @return This {@code IntRingBuffer} instance, allowing for method chaining.
	 */
	public LongRingBuffer clear() {
		get = Long.MIN_VALUE; // Reset get pointer to initial value.
		put = Long.MIN_VALUE; // Reset put pointer to initial value.
		return this; // For method chaining.
	}
}