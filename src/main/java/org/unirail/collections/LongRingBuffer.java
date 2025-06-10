// Copyright 2025 Chikirev Sirguy, Unirail Group
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol

package org.unirail.collections;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * A thread-safe, fixed-size ring buffer (circular buffer) for storing primitive values.
 * Optimized for efficient producer-consumer scenarios and bounded circular data storage.
 * The buffer uses atomic operations for thread-safe access and a bitwise mask for efficient index wrapping.
 */
public class LongRingBuffer {
	/**
	 * The internal array storing the ring buffer's primitive values.
	 */
	private final long[] buffer;
	
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
	static final AtomicIntegerFieldUpdater< LongRingBuffer > lock_update = AtomicIntegerFieldUpdater.newUpdater( LongRingBuffer.class, "lock" );
	
	/**
	 * Head pointer indicating the next element to read (used for get operations).
	 */
	private volatile long get = Long.MIN_VALUE;
	
	/**
	 * Atomic updater for thread-safe modifications to the get pointer.
	 */
	private static final AtomicLongFieldUpdater< LongRingBuffer > GET_UPDATER = AtomicLongFieldUpdater.newUpdater( LongRingBuffer.class, "get" );
	
	/**
	 * Tail pointer indicating the next available slot to write (used for put operations).
	 */
	private volatile long put = Long.MIN_VALUE;
	
	/**
	 * Atomic updater for thread-safe modifications to the put pointer.
	 */
	private static final AtomicLongFieldUpdater< LongRingBuffer > PUT_UPDATER = AtomicLongFieldUpdater.newUpdater( LongRingBuffer.class, "put" );
	
	/**
	 * Creates a new ring buffer with a capacity of 2^capacityPowerOfTwo.
	 *
	 * @param capacityPowerOfTwo The power of two defining the buffer's capacity (e.g., 4 for 16 elements).
	 * @throws IllegalArgumentException If capacityPowerOfTwo is negative.
	 */
	public LongRingBuffer( int capacityPowerOfTwo ) {
		if( capacityPowerOfTwo < 0 ) throw new IllegalArgumentException( "capacityPowerOfTwo must be non-negative" );
		buffer = new long[ ( mask = ( 1 << capacityPowerOfTwo ) - 1 ) + 1 ];
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
	public boolean put_multithreaded( long value ) {
		long _put;
		
		do {
			_put = put;
			if( size() == buffer.length ) return false;
		}
		while( !PUT_UPDATER.compareAndSet( this, _put, _put + 1 ) );
		
		buffer[ ( int ) _put & mask ] = ( long ) value;
		return true;
	}
	
	/**
	 * Adds an integer to the buffer (non-thread-safe).
	 * Use only in single-threaded contexts or with external synchronization.
	 *
	 * @param value The primitive to add.
	 * @return True if the value was added, false if the buffer is full.
	 */
	public boolean put( long value ) {
		if( size() == buffer.length ) return false;
		buffer[ ( int ) ( put++ ) & mask ] = ( long ) value;
		return true;
	}
	
	/**
	 * Resets the buffer to an empty state (non-thread-safe).
	 * Use with caution in concurrent environments.
	 *
	 * @return This instance for method chaining.
	 */
	public LongRingBuffer clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
		return this;
	}
}