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

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * A generic, thread-safe, fixed-size ring buffer (circular buffer) implementation.
 * Supports both single-threaded and multi-threaded operations with efficient buffering.
 *
 * @param <T> The type of elements stored in the buffer
 */
public class RingBuffer< T > {
	/**
	 * The underlying array storing buffer elements
	 */
	private final T[] buffer;
	
	/**
	 * Bit mask for efficient index wrapping
	 */
	private final int mask;
	
	/**
	 * Lock used for thread-safe operations
	 */
	private volatile int                                     lock        = 0;
	/**
	 * Atomic updater for the lock field
	 */
	static final     AtomicIntegerFieldUpdater< RingBuffer > lock_update = AtomicIntegerFieldUpdater.newUpdater( RingBuffer.class, "lock" );
	/**
	 * Pointer to the head of the buffer for get operations
	 */
	private volatile long                                    get         = Long.MIN_VALUE;
	
	/**
	 * Atomic updater for get pointer to ensure thread-safe operations
	 */
	private static final AtomicLongFieldUpdater< ? > GET_UPDATER = AtomicLongFieldUpdater.newUpdater( RingBuffer.class, "get" );
	
	/**
	 * Pointer to the tail of the buffer for put operations
	 */
	private volatile long put = Long.MIN_VALUE;
	
	/**
	 * Atomic updater for put pointer to ensure thread-safe operations
	 */
	private static final AtomicLongFieldUpdater< ? > PUT_UPDATER = AtomicLongFieldUpdater.newUpdater( RingBuffer.class, "put" );
	
	/**
	 * Constructs a new RingBuffer with a capacity of 2^power_of_2.
	 *
	 * @param clazz      The class type of the elements
	 * @param power_of_2 The power of two determining buffer capacity
	 * @throws IllegalArgumentException if power_of_2 is negative
	 */
	@SuppressWarnings( "unchecked" )
	public RingBuffer( Class< T > clazz, int power_of_2 ) {
		if( power_of_2 < 0 ) throw new IllegalArgumentException( "power_of_2 must be a non-negative integer" );
		// Calculate buffer size and create array using reflection
		buffer = ( T[] ) Array.newInstance( clazz, ( mask = ( 1 << power_of_2 ) - 1 ) + 1 );
	}
	
	/**
	 * Returns the fixed capacity of the ring buffer.
	 *
	 * @return Total buffer capacity
	 */
	public int length() {
		return buffer.length;
	}
	
	/**
	 * Returns the current number of elements in the buffer.
	 *
	 * @return Current buffer size
	 */
	public int size() {
		return ( int ) ( put - get );
	}
	
	/**
	 * Retrieves and removes an element from the buffer in a thread-safe manner.
	 *
	 * @param return_this_value_if_no_items Value to return if buffer is empty
	 * @return Retrieved value or default
	 */
	@SuppressWarnings( "unchecked" )
	public T get_multithreaded( T return_this_value_if_no_items ) {
		long _get;
		
		do
			if( ( _get = get ) == put ) return return_this_value_if_no_items;
		while( !( ( AtomicLongFieldUpdater< RingBuffer< T > > ) GET_UPDATER ).compareAndSet( this, _get, _get + 1 ) );
		
		return buffer[ ( int ) _get & mask ];
	}
	
	/**
	 * Retrieves and removes an element from the buffer (non-thread-safe).
	 *
	 * @param return_this_value_if_no_items Value to return if buffer is empty
	 * @return Retrieved value or default
	 */
	public T get( T return_this_value_if_no_items ) {
		return get == put ?
				return_this_value_if_no_items :
				buffer[ ( int ) ( get++ ) & mask ];
	}
	
	/**
	 * Adds an element to the buffer in a thread-safe manner.
	 *
	 * @param value Element to add
	 * @return true if element was added, false if buffer is full
	 */
	@SuppressWarnings( "unchecked" )
	public boolean put_multithreaded( T value ) {
		long _put;
		
		do {
			_put = put;
			if( size() == buffer.length ) return false;
		}
		while( !( ( AtomicLongFieldUpdater< RingBuffer< T > > ) PUT_UPDATER ).compareAndSet( this, _put, _put + 1 ) );
		
		buffer[ ( int ) _put & mask ] = value;
		return true;
	}
	
	/**
	 * Adds an element to the buffer (non-thread-safe).
	 *
	 * @param value Element to add
	 * @return true if element was added, false if buffer is full
	 */
	public boolean put( T value ) {
		if( size() == buffer.length ) return false;
		buffer[ ( int ) ( put++ ) & mask ] = value;
		return true;
	}
	
	
	/**
	 * Clears the buffer, resetting it to an empty state.
	 *
	 * @return This buffer instance for method chaining
	 */
	public RingBuffer< T > clear() {
		get = Long.MIN_VALUE;
		put = Long.MIN_VALUE;
		return this;
	}
}