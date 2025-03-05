//  MIT License
//
//  Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//  For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//  GitHub Repository: https://github.com/AdHoc-Protocol
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to use,
//  copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
//  the Software, and to permit others to do so, under the following conditions:
//
//  1. The above copyright notice and this permission notice must be included in all
//     copies or substantial portions of the Software.
//
//  2. Users of the Software must provide a clear acknowledgment in their user
//     documentation or other materials that their solution includes or is based on
//     this Software. This acknowledgment should be prominent and easily visible,
//     and can be formatted as follows:
//     "This product includes software developed by Chikirev Sirguy and the Unirail Group
//     (https://github.com/AdHoc-Protocol)."
//
//  3. If you modify the Software and distribute it, you must include a prominent notice
//     stating that you have changed the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
//  OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.


package org.unirail.collections;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

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
		T    value;
		
		do {
			_get = get;
			if( _get == put ) return return_this_value_if_no_items;
			value = buffer[ ( int ) _get & mask ];
		}
		while( !( ( AtomicLongFieldUpdater< RingBuffer< T > > ) GET_UPDATER ).compareAndSet( this, _get, _get + 1 ) );
		
		return value;
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
			if( size() + 1 == buffer.length ) return false;
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
		if( size() + 1 == buffer.length ) return false;
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