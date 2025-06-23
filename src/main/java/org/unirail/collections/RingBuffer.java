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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * A generic, fixed-size ring buffer (circular buffer) implementation.
 * Supports thread-safe operations for Single-Producer, Single-Consumer (SPSC),
 * Multiple-Producer, Single-Consumer (MPSC), or Single-Producer, Multiple-Consumer
 * (SPMC) scenarios. Not safe for Multiple-Producer, Multiple-Consumer (MPMC) without
 * external synchronization.
 *
 * <p>Uses {@link AtomicLongFieldUpdater} for atomic operations, ensuring lock-free
 * access in supported scenarios.</p>
 *
 * @param <T> The type of elements stored in the buffer
 */
public class RingBuffer< T > {
	/**
	 * The underlying array storing buffer elements.
	 */
	private final T[] buffer;
	
	/**
	 * Bit mask for efficient index wrapping (capacity - 1).
	 */
	private final int mask;
	
	/**
	 * Pointer to the head of the buffer for get operations.
	 */
	private volatile long get = 0L;
	
	/**
	 * Pointer to the tail of the buffer for put operations.
	 */
	private volatile long put = 0L;
	
	/**
	 * Atomic updater for the get pointer.
	 */
	private static final AtomicLongFieldUpdater< RingBuffer > GET = AtomicLongFieldUpdater.newUpdater( RingBuffer.class, "get" );
	
	/**
	 * Atomic updater for the put pointer.
	 */
	private static final AtomicLongFieldUpdater< RingBuffer > PUT = AtomicLongFieldUpdater.newUpdater( RingBuffer.class, "put" );
	
	/**
	 * Constructs a new RingBuffer with a capacity of 2^{@code power_of_2}.
	 *
	 * @param clazz      The class type of the elements
	 * @param power_of_2 The power of two determining buffer capacity (e.g., 3 for capacity 8)
	 * @throws IllegalArgumentException if power_of_2 is negative or exceeds 30
	 */
	@SuppressWarnings( "unchecked" )
	public RingBuffer( Class< T > clazz, int power_of_2 ) { this( clazz, power_of_2, false ); }
	
	/**
	 * Constructs a new RingBuffer with a capacity of 2^{@code power_of_2}.
	 *
	 * @param clazz      The class type of the elements
	 * @param power_of_2 The power of two determining buffer capacity (e.g., 3 for capacity 8)
	 * @param fill       If true, initializes buffer elements using the class's no-arg constructor
	 * @throws IllegalArgumentException if power_of_2 is negative or exceeds 30
	 * @throws RuntimeException         if fill is true and element initialization fails
	 */
	@SuppressWarnings( "unchecked" )
	public RingBuffer( Class< T > clazz, int power_of_2, boolean fill ) {
		
		if( power_of_2 < 0 ) throw new IllegalArgumentException( "power_of_2 must be non-negative" );
		if( 30 < power_of_2 ) throw new IllegalArgumentException( "power_of_2 must not exceed 30 to avoid integer overflow" );
		int capacity = 1 << power_of_2;
		mask   = capacity - 1;
		buffer = ( T[] ) Array.newInstance( clazz, capacity );
		if( fill )
			try {
				Constructor< T > ctor = clazz.getDeclaredConstructor();
				ctor.setAccessible( true ); // Handle non-public constructors
				for( int i = 0; i < buffer.length; i++ )
				     buffer[ i ] = ctor.newInstance();
			} catch( NoSuchMethodException e ) {
				// Better error message if no default constructor exists
				throw new RuntimeException( "Class " + clazz.getName() + " requires a no-arg constructor for filling.", e );
			} catch( InstantiationException | IllegalAccessException | InvocationTargetException e ) {
				// Catch other reflection-related or constructor-thrown exceptions
				throw new RuntimeException( "Failed to initialize element in RingBuffer using no-arg constructor.", e );
			} catch( Exception e ) {
				// Catch any other unexpected exceptions during fill
				throw new RuntimeException( "An unexpected error occurred during RingBuffer fill.", e );
			}
		
	}
	
	/**
	 * Returns the fixed capacity of the ring buffer (always a power of 2).
	 *
	 * @return Buffer capacity
	 */
	public int capacity() {
		return buffer.length;
	}
	
	/**
	 * Returns the approximate number of elements in the buffer.
	 * <p>In concurrent scenarios, the result may be stale due to non-atomic reads.</p>
	 *
	 * @return Approximate number of elements
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
	 * Retrieves and removes an element from the buffer in a thread-safe manner.
	 * Suitable for SPSC or SPMC scenarios.
	 *
	 * @param return_if_empty Value to return if the buffer is empty
	 * @return Retrieved element or return_if_empty if empty
	 */
	public T get_multithreaded( T return_if_empty ) { return get_multithreaded( return_if_empty, null ); }
	
	
	/**
	 * Retrieves an element from the buffer in a thread-safe manner and replacing it with the specified value.
	 *
	 * @param return_if_empty Value to return if the buffer is empty
	 * @param replacement     Value to place in the buffer at the retrieved index
	 * @return Retrieved element or return_if_empty if empty
	 */
	public T get_multithreaded( T return_if_empty, T replacement ) {
		long get;
		do {
			get = GET.get( this ); // Volatile read
			if( get == PUT.get( this ) ) return return_if_empty;// Volatile read
		}
		while( !GET.compareAndSet( this, get, get + 1L ) );
		int index = ( int ) get & mask;
		T   ret   = buffer[ index ];
		buffer[ index ] = replacement;
		return ret;
	}
	
	/**
	 * Retrieves and removes an element from the buffer (non-thread-safe).
	 * For single-threaded or externally synchronized use only.
	 *
	 * @param return_if_empty Value to return if the buffer is empty
	 * @return Retrieved element or return_if_empty if empty
	 */
	public T get( T return_if_empty ) { return get( return_if_empty, null ); }
	
	/**
	 * Retrieves an element from the buffer (non-thread-safe) and replacing it with the specified value.
	 *
	 * @param defaultValue Value to return if the buffer is empty
	 * @param replacement  Value to place in the buffer at the retrieved index
	 * @return Retrieved element or defaultValue if empty
	 */
	public T get( T defaultValue, T replacement ) {
		if( get == put ) return defaultValue;
		int index = ( int ) get++ & mask;
		T   item  = buffer[ index ];
		buffer[ index ] = replacement;
		return item;
	}
	
	/**
	 * Adds an element to the buffer in a thread-safe manner.
	 * Suitable for SPSC or MPSC scenarios.
	 *
	 * @param value Element to add
	 * @return true if added, false if buffer is full
	 */
	public boolean put_multithreaded( T value ) {
		long put;
		do {
			put = PUT.get( this ); // Volatile read
			if( buffer.length <= put - GET.get( this ) ) return false;// Volatile read
		}
		while( !PUT.compareAndSet( this, put, put + 1L ) );
		buffer[ ( int ) put & mask ] = value;
		return true;
	}
	
	/**
	 * Adds an element to the buffer in a thread-safe manner, returning the element
	 * at the insertion index, or return_if_full if the buffer is full.
	 *
	 * @param return_if_full Value to return if the buffer is full
	 * @param value          Element to add
	 * @return Previous element at the index or return_if_full if buffer is full
	 */
	public T put_multithreaded( T return_if_full, T value ) {
		long put;
		do {
			put = PUT.get( this ); // Volatile read
			if( buffer.length <= put - GET.get( this ) ) return return_if_full;// Volatile read
		}
		while( !PUT.compareAndSet( this, put, put + 1L ) );
		int index = ( int ) put & mask;
		T   ret   = buffer[ index ];
		buffer[ index ] = value;
		return ret;
	}
	
	/**
	 * Adds an element to the buffer (non-thread-safe).
	 * For single-threaded or externally synchronized use only.
	 *
	 * @param value Element to add
	 * @return true if added, false if buffer is full
	 */
	public boolean put( T value ) {
		if( buffer.length <= put - get ) return false;
		buffer[ ( int ) put++ & mask ] = value;
		return true;
	}
	
	public T put( T return_if_full, T value ) {
		if( buffer.length <= put - get ) return return_if_full;
		int index = ( int ) put++ & mask;
		T   ret   = buffer[ index ];
		buffer[ index ] = value;
		return ret;
	}
	
	
	/**
	 * Clears the buffer, resetting it to an empty state.
	 * <p>Not thread-safe; use only when the buffer is quiescent or externally synchronized.
	 * Nulls out all elements to prevent memory leaks.</p>
	 *
	 * @return This buffer instance for method chaining
	 */
	public RingBuffer< T > clear( boolean nullify ) {
		get = 0L;
		put = 0L;
		if( nullify ) Arrays.fill( buffer, null ); // Prevent memory leaks
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
		return String.format( "RingBuffer{capacity=%d, size=%d, get=%d, put=%d}",
		                      capacity(), size(), get, put );
	}
}