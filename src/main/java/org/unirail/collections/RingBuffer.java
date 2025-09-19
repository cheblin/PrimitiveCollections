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
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Supplier;

/**
 * A generic, fixed-size, lock-free ring buffer (circular buffer).
 *
 * <p>This implementation uses a power-of-two capacity for high-performance index calculation
 * via bit masking. It is optimized for the following concurrent scenarios:
 * <ul>
 *   <li>Single-Producer, Single-Consumer (SPSC) - Use non-multithreaded methods like {@link #put(Object)} and {@link #get()}.</li>
 *   <li>Multiple-Producers, Single-Consumer (MPSC) - Use {@link #put_multithreaded(Object)}.</li>
 *   <li>Single-Producer, Multiple-Consumers (SPMC) - Use {@link #get_multithreaded(Object)}.</li>
 * </ul>
 *
 * <p><b>Warning:</b> This implementation is <b>not safe</b> for Multiple-Producers, Multiple-Consumers (MPMC)
 * scenarios without external synchronization. In an MPMC context, data races can occur, leading to incorrect behavior.
 *
 * <p>Thread safety for MPSC and SPMC scenarios is achieved using {@link AtomicLongFieldUpdater} to perform lock-free
 * compare-and-set (CAS) operations on the head ({@code get}) and tail ({@code put}) pointers.
 *
 * @param <T> The type of elements held in this collection.
 */
public class RingBuffer< T > {
	/**
	 * The final array that stores the buffer's elements. Its length is always a power of two.
	 */
	private final T[] buffer;
	
	/**
	 * Bit mask used for efficient index wrapping. Calculated as {@code capacity - 1}.
	 */
	private final int mask;
	
	/**
	 * The head pointer, tracking the index of the next element to be read (dequeued).
	 * Declared {@code volatile} to ensure visibility of writes across threads.
	 */
	private volatile long get = 0L;
	
	/**
	 * The tail pointer, tracking the index of the next available slot for writing (enqueuing).
	 * Declared {@code volatile} to ensure visibility of writes across threads.
	 */
	private volatile long put = 0L;
	
	/**
	 * Atomic updater for the {@code get} pointer, enabling lock-free CAS operations for thread-safe reads.
	 */
	@SuppressWarnings( "rawtypes" )
	private static final AtomicLongFieldUpdater< RingBuffer > GET = AtomicLongFieldUpdater.newUpdater( RingBuffer.class, "get" );
	
	/**
	 * Atomic updater for the {@code put} pointer, enabling lock-free CAS operations for thread-safe writes.
	 */
	@SuppressWarnings( "rawtypes" )
	private static final AtomicLongFieldUpdater< RingBuffer > PUT = AtomicLongFieldUpdater.newUpdater( RingBuffer.class, "put" );
	
	/**
	 * Constructs a ring buffer with a capacity equal to 2<sup>{@code powerOf2}</sup>.
	 *
	 * @param clazz    The class type of the elements. This is required to create a generic array.
	 * @param powerOf2 The exponent for the capacity calculation (e.g., 10 results in a capacity of 1024).
	 * @throws IllegalArgumentException if {@code powerOf2} is negative or greater than 30 (to prevent overflow).
	 */
	public RingBuffer( Class< T > clazz, int powerOf2 ) { this( clazz, powerOf2, false ); }
	
	/**
	 * Constructs a ring buffer with a capacity of 2<sup>{@code powerOf2}</sup>, optionally pre-filling it with new instances.
	 * <p>Pre-filling is useful when the buffer will be used for recycling objects, allowing consumers to retrieve
	 * an object and producers to replace it without causing a null pointer exception on the first pass.
	 *
	 * @param clazz    The class type of the elements. This is required to create a generic array.
	 * @param powerOf2 The exponent for the capacity calculation (e.g., 10 results in a capacity of 1024).
	 * @param fill     If {@code true}, the buffer is pre-filled with new instances created via the type's
	 *                 default (no-argument) constructor.
	 * @throws IllegalArgumentException if {@code powerOf2} is negative or greater than 30 (to prevent overflow).
	 * @throws RuntimeException         if {@code fill} is {@code true} but the class {@code T} does not have an accessible
	 *                                  no-argument constructor, or if instantiation fails for any other reason.
	 */
	@SuppressWarnings( "unchecked" )
	public RingBuffer( Class< T > clazz, int powerOf2, boolean fill ) {
		if( powerOf2 < 0 ) throw new IllegalArgumentException( "powerOf2 must be non-negative" );
		if( powerOf2 > 30 ) throw new IllegalArgumentException( "powerOf2 must not exceed 30 to avoid integer overflow" );
		int capacity = 1 << powerOf2;
		this.mask   = capacity - 1;
		this.buffer = ( T[] ) Array.newInstance( clazz, capacity );
		
		if( fill ) try {
			Constructor< T > constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible( true ); // Allow access to non-public constructors
			for( int i = 0; i < buffer.length; i++ ) buffer[ i ] = constructor.newInstance();
		} catch( NoSuchMethodException e ) {
			throw new RuntimeException( "Class " + clazz.getName() + " requires a no-arg constructor for filling", e );
		} catch( InstantiationException | IllegalAccessException | InvocationTargetException e ) {
			throw new RuntimeException( "Failed to initialize elements in RingBuffer", e );
		}
	}
	
	/**
	 * Constructs a ring buffer and pre-fills it using the provided factory.
	 * <p>This is the most flexible and performant way to create a pre-filled buffer. The factory
	 * is called once for each slot in the buffer.
	 * <p>Example usage with a method reference:
	 * <pre>{@code RingBuffer<MyObject> rb = new RingBuffer<>(MyObject.class, 10, MyObject::new);}</pre>
	 * <p>Example usage with a lambda for a constructor with arguments:
	 * <pre>{@code RingBuffer<User> rb = new RingBuffer<>(User.class, 8, () -> new User("default"));}</pre>
	 *
	 * @param clazz    The class type of the elements. This is required to create a generic array.
	 * @param powerOf2 The exponent for the capacity calculation (e.g., 10 results in a capacity of 1024).
	 * @param factory  A {@link Supplier} that provides new instances of {@code T}. If null, the buffer is not filled.
	 * @throws IllegalArgumentException if {@code powerOf2} is negative or greater than 30.
	 */
	@SuppressWarnings( "unchecked" )
	public RingBuffer( Class< T > clazz, int powerOf2, Supplier< T > factory ) {
		if( powerOf2 < 0 ) throw new IllegalArgumentException( "powerOf2 must be non-negative" );
		if( powerOf2 > 30 ) throw new IllegalArgumentException( "powerOf2 must not exceed 30 to avoid integer overflow" );
		int capacity = 1 << powerOf2;
		this.mask   = capacity - 1;
		this.buffer = ( T[] ) Array.newInstance( clazz, capacity );
		
		if( factory != null ) for( int i = 0; i < buffer.length; i++ ) buffer[ i ] = factory.get();
	}
	
	/**
	 * Returns the fixed capacity of the ring buffer.
	 *
	 * @return the buffer's capacity, which is always a power of 2.
	 */
	public int capacity() { return buffer.length; }
	
	/**
	 * Returns the approximate number of elements currently in the buffer.
	 * <p><b>Note:</b> In a concurrent environment, this value can be stale as soon as it is returned,
	 * as other threads may be adding or removing elements. It should be used as an estimate.
	 *
	 * @return The approximate count of elements in the buffer.
	 */
	public int size() {
		// A snapshot of the size. Can be stale in a concurrent environment.
		long size = put - get;
		return size < 0 ?
				0 :
				( int ) Math.min( size, buffer.length );
	}
	
	/**
	 * Checks if the buffer is empty.
	 * <p><b>Note:</b> In a concurrent environment, the buffer's state can change after this method returns.
	 * The result is a snapshot in time and may be stale.
	 *
	 * @return {@code true} if the head and tail pointers were equal at the time of the check.
	 */
	public boolean isEmpty() { return get == put; }
	
	/**
	 * Checks if the buffer is full.
	 * <p><b>Note:</b> In a concurrent environment, the buffer's state can change after this method returns.
	 * The result is a snapshot in time and may be stale.
	 *
	 * @return {@code true} if the number of elements equals the capacity at the time of the check.
	 */
	public boolean isFull() { return put - get == buffer.length; }
	
	/**
	 * Atomically retrieves and removes an element from the buffer.
	 * This method is thread-safe for multiple consumers (SPMC).
	 *
	 * @param returnIfEmpty The value to return if the buffer is empty.
	 * @return The retrieved element, or {@code returnIfEmpty} if the buffer was empty.
	 */
	public T get_multithreaded( T returnIfEmpty ) { return get_multithreaded( returnIfEmpty, null ); }
	
	/**
	 * Atomically retrieves and removes an element from the buffer, replacing it with a specified value.
	 * This method is thread-safe for multiple consumers (SPMC).
	 * <p>
	 * It uses a compare-and-set (CAS) loop to atomically increment the 'get' pointer,
	 * ensuring that each consumer retrieves a unique element.
	 *
	 * @param returnIfEmpty The value to return if the buffer is empty.
	 * @param replacement   The value to place in the buffer at the retrieved element's slot.
	 *                      This is useful for 'nulling out' the reference to aid garbage collection
	 *                      or for recycling pooled objects.
	 * @return The retrieved element, or {@code returnIfEmpty} if the buffer was empty.
	 */
	public T get_multithreaded( T returnIfEmpty, T replacement ) {
		long currentGet;
		do {
			currentGet = GET.get( this ); // Volatile read of the head pointer
			if( currentGet == PUT.get( this ) ) return returnIfEmpty; // Buffer is empty, checked with a volatile read of the tail
		}
		while( !GET.compareAndSet( this, currentGet, currentGet + 1L ) ); // Atomically claim the item
		
		int index  = ( int ) currentGet & mask;
		T   result = buffer[ index ];
		buffer[ index ] = replacement;
		return result;
	}
	
	/**
	 * Retrieves and removes an element from the buffer.
	 * <p><b>Warning: This method is not thread-safe.</b> It should only be used in a single-consumer
	 * context or when external synchronization is in place.
	 * <p>
	 * <b>Precondition:</b> The caller is responsible for ensuring the buffer is <b>not empty</b>
	 * before calling this method (e.g., by checking {@link #isEmpty()}). Invoking this on an empty buffer
	 * will corrupt the buffer's state by advancing the read pointer past the write pointer,
	 * and will return a stale, invalid element.
	 *
	 * @return The retrieved element.
	 */
	public T get() { return get( null ); }
	
	/**
	 * Retrieves and removes an element from the buffer, replacing it with the specified value.
	 * Call isEmpty before call this method
	 * <p><b>Warning: This method is not thread-safe.</b> It should only be used in a single-consumer
	 * context or when external synchronization is in place. It performs a non-atomic update.
	 * <p>
	 * <b>Precondition:</b> The caller is responsible for ensuring the buffer is <b>not empty</b>
	 * before calling this method (e.g., by checking {@link #isEmpty()}). Invoking this on an empty buffer
	 * will corrupt the buffer's state by advancing the read pointer past the write pointer,
	 * and will return a stale, invalid element.
	 *
	 * @param replacement The value to place in the buffer at the retrieved element's slot.
	 * @return The retrieved element.
	 */
	public T get( T replacement ) {
		int index = ( int ) get++ & mask;
		T   item  = buffer[ index ];
		buffer[ index ] = replacement;
		return item;
	}
	
	/**
	 * Atomically adds an element to the buffer if space is available.
	 * This method is thread-safe for multiple producers (MPSC).
	 * <p>
	 * It uses a compare-and-set (CAS) loop to atomically increment the 'put' pointer,
	 * ensuring that each producer claims a unique slot.
	 *
	 * @param value The element to add.
	 * @return {@code true} if the element was successfully added, {@code false} if the buffer was full.
	 */
	public boolean put_multithreaded( T value ) {
		long currentPut;
		do {
			currentPut = PUT.get( this ); // Volatile read of the tail pointer
			if( buffer.length <= currentPut - GET.get( this ) ) return false; // Buffer is full, checked with a volatile read of the head
		}
		while( !PUT.compareAndSet( this, currentPut, currentPut + 1L ) ); // Atomically claim the slot
		
		buffer[ ( int ) currentPut & mask ] = value;
		return true;
	}
	
	/**
	 * Atomically adds an element to the buffer, returning the element that was overwritten at the insertion index.
	 * This method is thread-safe for multiple producers (MPSC).
	 * <p>
	 * This is useful in object pooling scenarios where the overwritten object needs to be handled (e.g., returned to a pool).
	 *
	 * @param returnIfFull The value to return if the buffer is full and the new element cannot be added.
	 * @param value        The element to add.
	 * @return The element that was previously at the insertion index, or {@code returnIfFull} if the buffer was full.
	 */
	public T put_multithreaded( T returnIfFull, T value ) {
		long currentPut;
		do {
			currentPut = PUT.get( this ); // Volatile read
			if( buffer.length <= currentPut - GET.get( this ) ) return returnIfFull; // Buffer is full
		}
		while( !PUT.compareAndSet( this, currentPut, currentPut + 1L ) );
		
		int index  = ( int ) currentPut & mask;
		T   result = buffer[ index ];
		buffer[ index ] = value;
		return result;
	}
	
	/**
	 * Adds an element to the buffer, returning the element that was overwritten.
	 * <p><b>Warning: This method is not thread-safe.</b> It should only be used in a single-producer
	 * context or when external synchronization is in place.
	 * <p>The caller is responsible for ensuring the buffer is not full before calling this method.
	 *
	 * @param value The element to add.
	 * @return The element that was previously at the insertion index.
	 */
	public T put( T value ) {
		int index  = ( int ) put++ & mask;
		T   result = buffer[ index ];
		buffer[ index ] = value;
		return result;
	}
	
	/**
	 * Adds an element to the buffer.
	 * This is a slightly more performant version of {@link #put(Object)} for cases where the
	 * overwritten value is not needed.
	 * <p><b>Warning: This method is not thread-safe.</b> It should only be used in a single-producer context.
	 * <p>The caller is responsible for ensuring the buffer is not full before calling this method.
	 *
	 * @param value The element to add.
	 */
	public void put_( T value ) { buffer[ ( int ) put++ & mask ] = value; }
	
	/**
	 * Resets the buffer to an empty state by setting the head and tail pointers to zero.
	 * <p><b>Warning: This method is not thread-safe.</b> It should only be called when no other threads
	 * are accessing the buffer, or when access is controlled by external synchronization.
	 *
	 * @return Internal array for advanced cleaning.
	 */
	public T[] clear() {
		// Not thread-safe. Should be called only when no other operations are in progress.
		get = 0L;
		put = 0L;
		return buffer;
	}
	
	/**
	 * Returns a string representation of the buffer's current state.
	 * <p><b>Warning: This method is not thread-safe.</b> The reported values of size, get, and put
	 * are snapshots and may be inconsistent if the buffer is being modified concurrently.
	 * It is intended for debugging and logging in controlled scenarios.
	 *
	 * @return A string summarizing the buffer's capacity, size, and pointer positions.
	 */
	@Override
	public String toString() {
		return String.format( "RingBuffer{capacity=%d, size=%d, get=%d, put=%d}",
		                      capacity(), size(), get, put );
	}
}