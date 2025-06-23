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

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * A thread-safe, fixed-size ring buffer (circular buffer) for storing primitive values.
 * Optimized for Single-Producer, Single-Consumer (SPSC), Multiple-Producer, Single-Consumer (MPSC),
 * or Single-Producer, Multiple-Consumer (SPMC) scenarios. Not safe for Multiple-Producer,
 * Multiple-Consumer (MPMC) without external synchronization.
 *
 * <p>Uses {@link AtomicLongFieldUpdater} for lock-free atomic operations and a bitwise mask
 * for efficient index wrapping.</p>
 */
public class IntRingBuffer {
	/**
	 * The internal array storing the ring buffer's primitive values.
	 */
	private final int[] buffer;
	
	/**
	 * Bitmask for efficient index wrapping, computed as (1 << capacityPowerOfTwo) - 1.
	 */
	private final int mask;
	
	/**
	 * Head pointer indicating the next element to read (used for get operations).
	 */
	private volatile long get = 0L;
	
	/**
	 * Tail pointer indicating the next available slot to write (used for put operations).
	 */
	private volatile long put = 0L;
	
	/**
	 * Atomic updater for thread-safe modifications to the get pointer.
	 */
	private static final AtomicLongFieldUpdater< IntRingBuffer > GET = AtomicLongFieldUpdater.newUpdater( IntRingBuffer.class, "get" );
	
	/**
	 * Atomic updater for thread-safe modifications to the put pointer.
	 */
	private static final AtomicLongFieldUpdater< IntRingBuffer > PUT = AtomicLongFieldUpdater.newUpdater( IntRingBuffer.class, "put" );
	
	/**
	 * Creates a new ring buffer with a capacity of 2^capacityPowerOfTwo.
	 *
	 * @param capacityPowerOfTwo The power of two defining the buffer's capacity (e.g., 4 for 16 elements).
	 * @throws IllegalArgumentException If capacityPowerOfTwo is negative or exceeds 30.
	 */
	public IntRingBuffer( int capacityPowerOfTwo ) {
		if( capacityPowerOfTwo < 0 ) throw new IllegalArgumentException( "capacityPowerOfTwo must be non-negative" );
		if( 30 < capacityPowerOfTwo ) throw new IllegalArgumentException( "capacityPowerOfTwo must not exceed 30 to avoid integer overflow" );
		int capacity = 1 << capacityPowerOfTwo;
		mask   = capacity - 1;
		buffer = new int[ capacity ];
	}
	
	/**
	 * Returns the fixed capacity of the ring buffer.
	 *
	 * @return The maximum number of elements the buffer can hold.
	 */
	public int length() { return buffer.length; }
	
	/**
	 * Returns the approximate number of elements in the ring buffer.
	 * <p>In concurrent scenarios, the result may be stale due to non-atomic reads.</p>
	 *
	 * @return The approximate number of elements currently stored.
	 */
	public int size() {
		long size = put - get;
		return size < 0 ?
		       0 :
		       ( int ) Math.min( size, buffer.length );
	}
	
	/**
	 * Checks if the buffer is approximately empty.
	 * <p>In concurrent scenarios, the result may be stale.</p>
	 *
	 * @return true if the buffer appears empty, false otherwise
	 */
	public boolean isEmpty() {
		return get == put;
	}
	
	/**
	 * Checks if the buffer is approximately full.
	 * <p>In concurrent scenarios, the result may be stale.</p>
	 *
	 * @return true if the buffer appears full, false otherwise
	 */
	public boolean isFull() {
		return put - get == buffer.length;
	}
	
	/**
	 * Thread-safely retrieves the next integer from the buffer without removing it.
	 * Suitable for SPSC or SPMC scenarios.
	 *
	 * @param defaultValueIfEmpty Value to return if the buffer is empty.
	 * @return The retrieved integer, or defaultValueIfEmpty if the buffer is empty.
	 */
	public int get_multithreaded( int defaultValueIfEmpty ) {
		long get;
		do {
			get = GET.get( this ); // Volatile read
			if( get == PUT.get( this ) ) return defaultValueIfEmpty;// Volatile read
		}
		while( !GET.compareAndSet( this, get, get + 1L ) );
		return buffer[ ( int ) get & mask ];
	}
	
	/**
	 * Thread-safely retrieves and removes the next integer from the buffer.
	 * Suitable for SPSC or SPMC scenarios.
	 *
	 * @param defaultValueIfEmpty Value to return if the buffer is empty.
	 * @return The retrieved integer, or defaultValueIfEmpty if the buffer is empty.
	 */
	public int remove_multithreaded( int defaultValueIfEmpty ) {
		long currentGet;
		do {
			currentGet = GET.get( this ); // Volatile read
			if( currentGet == PUT.get( this ) ) return defaultValueIfEmpty;// Volatile read
		}
		while( !GET.compareAndSet( this, currentGet, currentGet + 1L ) );
		return buffer[ ( int ) currentGet & mask ];
	}
	
	/**
	 * Retrieves the next integer from the buffer without removing it (non-thread-safe).
	 * Use only in single-threaded contexts or with external synchronization.
	 *
	 * @param defaultValueIfEmpty Value to return if the buffer is empty.
	 * @return The retrieved integer, or defaultValueIfEmpty if the buffer is empty.
	 */
	public int get( int defaultValueIfEmpty ) {
		return get == put ?
		       defaultValueIfEmpty :
		       buffer[ ( int ) get++ & mask ];
	}
	
	/**
	 * Retrieves and removes the next integer from the buffer (non-thread-safe).
	 * Use only in single-threaded contexts or with external synchronization.
	 *
	 * @param defaultValueIfEmpty Value to return if the buffer is empty.
	 * @return The retrieved integer, or defaultValueIfEmpty if the buffer is empty.
	 */
	public int remove( int defaultValueIfEmpty ) {
		return get == put ?
		       defaultValueIfEmpty :
		       buffer[ ( int ) get++ & mask ];
	}
	
	/**
	 * Thread-safely adds an integer to the buffer.
	 *
	 * @param value The primitive to add.
	 * @return True if the value was added, false if the buffer is full.
	 */
	public boolean put_multithreaded( int value ) {
		long currentPut;
		do {
			currentPut = PUT.get( this ); // Volatile read
			// Volatile read
			if( currentPut - GET.get( this ) >= buffer.length ) return false;
		}
		while( !PUT.compareAndSet( this, currentPut, currentPut + 1L ) );
		buffer[ ( int ) currentPut & mask ] = ( int ) value;
		return true;
	}
	
	/**
	 * Adds an integer to the buffer (non-thread-safe).
	 * Use only in single-threaded contexts or with external synchronization.
	 *
	 * @param value The primitive to add.
	 * @return True if the value was added, false if the buffer is full.
	 */
	public boolean put( int value ) {
		if( put - get >= buffer.length ) return false;
		buffer[ ( int ) put++ & mask ] = ( int ) value;
		return true;
	}
	
	/**
	 * Resets the buffer to an empty state (non-thread-safe).
	 * Use with caution in concurrent environments; ensure no concurrent access.
	 *
	 * @return This instance for method chaining.
	 */
	public IntRingBuffer clear() {
		get = 0L;
		put = 0L;
		return this;
	}
	
	/**
	 * Returns a string representation of the buffer's state.
	 * <p>For debugging purposes; not thread-safe due to non-atomic state access.</p>
	 *
	 * @return String representation of the buffer
	 */
	@Override
	public String toString() {
		return String.format( "IntRingBuffer{capacity=%d, size=%d, get=%d, put=%d}",
		                      length(), size(), get, put );
	}
}