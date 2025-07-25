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

import org.unirail.JsonWriter;

import java.util.Arrays;

/**
 * An interface defining a contract for a list that stores primitive values,
 * allowing elements to represent a {@code null} state.
 * <p>
 * This list is optimized for balancing memory usage and performance by employing two internal
 * storage strategies for the primitive data, managed by a backing {@link BitList.RW}
 * which tracks the nullity status of each logical element.
 * </p>
 * <p>
 * The two strategies are:
 * </p>
 * <ul>
 *     <li><b>Compressed Strategy:</b> Space-efficient for sparse lists (many nulls). Only
 *         non-null values are stored contiguously in the internal primitive array. Access
 *         requires calculating the rank (position among non-nulls) using the nullity bitlist.</li>
 *     <li><b>Flat Strategy:</b> Time-efficient for dense lists (few nulls). The internal primitive
 *         array has the same size as the logical list, providing direct indexing after
 *         checking nullity with the bitlist.</li>
 * </ul>
 * <p>
 * The writable implementation {@link RW} automatically switches between these strategies
 * based on the density of non-null elements and a configurable threshold to optimize
 * performance for common operations.
 * </p>
 */
public interface CharNullList {
	
	/**
	 * {@code R} is an abstract base class providing a read-only view of a list that
	 * can store primitive values and represent a {@code null} state for elements.
	 * <p>
	 * It encapsulates the core data structures and logic for accessing elements
	 * while being aware of the nullity status and the underlying storage strategy.
	 * </p>
	 *
	 * @implSpec This class utilizes a {@link BitList.RW} instance (`nulls`) to track
	 * which logical positions contain a non-null value (bit is true) or a
	 * null placeholder (bit is false). The actual primitive values
	 * are stored in a primitive array (`values`), whose organization depends
	 * on the current storage strategy (`isFlatStrategy`). The number of
	 * non-null elements is tracked in `cardinality`.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * The backing bitlist that tracks the nullity status of each element.
		 * The size of this bitlist is the logical size of the list.
		 * A bit set to {@code true} at index {@code i} indicates that the element
		 * at logical index {@code i} in this list is non-null and has a corresponding
		 * value in the internal values array. A bit set to {@code false} indicates a null element.
		 */
		protected BitList.RW nulls;
		
		/**
		 * The array storing the primitive values of the non-null elements.
		 * <p>
		 *
		 * @implSpec The interpretation and size of this array depend on the
		 * current storage strategy (`isFlatStrategy`):
		 * <ul>
		 *               <li>In <b>Compressed Strategy:</b> (`isFlatStrategy = false`), this array
		 *                   stores only the non-null values contiguously. Its size is
		 *                   typically equal to `cardinality` (the count of non-nulls).</li>
		 *               <li>In <b>Flat Strategy:</b> (`isFlatStrategy = true`), this array
		 *                   mirrors the logical structure of the list. Its size is
		 *                   equal to `nulls.size()`. The element at `values[i]` conceptually stores the
		 *                   value for logical index `i`, but this value is only valid
		 *                   if the corresponding bit in `nulls` at index `i` is true.</li>
		 * </ul>
		 */
		protected char[] values = Array.EqualHashOf.chars     .O;
		
		
		/**
		 * The number of non-null elements currently stored in the list.
		 * This is equal to the cardinality of the nulls bit list.
		 * <p>
		 *
		 * @implSpec In the Compressed Strategy, this value directly corresponds to the number of meaningful elements in the `values` array.
		 * In the Flat Strategy, it is still maintained for consistency and potential
		 * strategy switching decisions, but the Flat Strategy primarily relies
		 * on the cardinality of the `nulls` bitlist for dynamic count.
		 */
		protected int cardinality = 0;
		
		/**
		 * Gets the number of non-null elements currently stored in the list.
		 *
		 * @return the count of non-null elements
		 */
		public int cardinality() {
			return isFlatStrategy ?
			       nulls.cardinality() :
			       cardinality;
		}
		
		/**
		 * The threshold used by the {@link RW} subclass to decide when to switch
		 * to the Flat Strategy. If the number of non-null elements (`cardinality`)
		 * exceeds this threshold, the list switches to the Flat Strategy during
		 * modification operations to potentially improve access time. A higher threshold
		 * favors the Compressed Strategy (space efficiency). A lower threshold favors
		 * the Flat Strategy (time efficiency).
		 * Default is 64.
		 *
		 * @see RW#flatStrategyThreshold(int)
		 */
		protected int flatStrategyThreshold = 64;
		
		public int flatStrategyThreshold() { return flatStrategyThreshold; }
		
		/**
		 * Indicates the current storage strategy for the `values` array.
		 * {@code true} if the Flat Strategy is active (direct indexing),
		 * {@code false} if the Compressed Strategy is active (rank-based indexing).
		 *
		 * @see #values
		 */
		protected boolean isFlatStrategy = false;
		
		/**
		 * Returns the physical capacity of the underlying `values` array.
		 * This is the number of primitive slots currently allocated for storing values.
		 * <p>
		 *
		 * @return The allocated length of the internal `values` array.
		 * @implSpec In the Flat Strategy, this is usually at least the logical size
		 * and potentially larger to accommodate future growth without
		 * immediate reallocation. In the Compressed Strategy, this is
		 * the allocated size of the array holding only the non-null values,
		 * and it might be larger than `cardinality`.
		 * @see #size()
		 */
		public int length() { return values.length; }
		
		
		/**
		 * Returns the logical size of the list, which is the total number of elements
		 * including both null and non-null values.
		 *
		 * @return The total number of logical elements in the list.
		 */
		public int size() { return nulls.size(); }
		
		/**
		 * Checks if the list is logically empty.
		 *
		 * @return {@code true} if the logical size is 0, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() < 1; }
		
		/**
		 * Checks if the element at the specified logical index is a non-null value.
		 *
		 * @param index The logical index to check.
		 * @return {@code true} if the element at this index is non-null and has a
		 * corresponding value, {@code false} if it is null or the index
		 * is out of the valid range [0, size() - 1].
		 */
		public boolean hasValue( int index ) {
			return nulls.get( index );
		}
		
		/**
		 * Finds the next logical index after the specified index that contains a non-null value.
		 *
		 * @param index The starting logical index (exclusive) for the search. The search begins
		 *              at {@code index + 1}. Pass -1 to start the search from the beginning.
		 * @return The logical index of the next non-null element after {@code index}, or -1 if no more
		 * non-null elements are found in the list.
		 */
		public @Positive_OK int nextValueIndex( int index ) { return nulls.next1( index ); }
		
		
		/**
		 * Finds the previous logical index before the specified index that contains a non-null value.
		 *
		 * @param index The starting logical index (exclusive) for the backward search. The search begins
		 *              at {@code index - 1}. Pass -1 or a value greater than or equal to the list's size
		 *              to start the search from the end (i.e., {@code size()}).
		 * @return The logical index of the previous non-null element before {@code index}, or -1 if no more
		 * non-null elements are found in the list.
		 */
		public @Positive_OK int prevValueIndex( int index ) { return nulls.prev1( index ); }
		
		/**
		 * Finds the next logical index after the specified index that contains a null value.
		 *
		 * @param index The starting logical index (exclusive) for the search. The search begins
		 *              at {@code index + 1}. Pass -1 to start the search from the beginning.
		 * @return The logical index of the next null element after {@code index}, or -1 if no more
		 * null elements are found in the list.
		 */
		public @Positive_OK int nextNullIndex( int index ) { return nulls.next0( index ); }
		
		
		/**
		 * Finds the previous logical index before the specified index that contains a null value.
		 *
		 * @param index The starting logical index (exclusive) for the backward search. The search begins
		 *              at {@code index - 1}. Pass -1 or a value greater than or equal to the list's size
		 *              to start the search from the last logical index ({@code size() - 1}).
		 * @return The logical index of the previous null element before {@code index}, or -1 if no more
		 * null elements are found in the list.
		 */
		public @Positive_OK int prevNullIndex( int index ) { return nulls.prev0( index ); }
		
		/**
		 * Retrieves the primitive value at the specified logical index.
		 * <p>
		 *
		 * @param index The logical index of the element to retrieve. Must be non-negative
		 *              and less than {@code size()}.
		 * @return The primitive value at the specified index.
		 * @throws IndexOutOfBoundsException If the index is negative or greater than or equal to {@code size()}.
		 * @apiNote This method should only be called if {@link #hasValue(int)} returns {@code true}
		 * for the given index. Calling this method on an index that represents a null element
		 * may return an undefined default value (e.g., 0) or lead to unexpected behavior,
		 * depending on the internal storage strategy. Always check {@link #hasValue(int)} first
		 * to ensure the index contains a valid non-null value.
		 */
		public char get( @Positive_ONLY int index ) {
			if( index < 0 ) throw new IndexOutOfBoundsException( "Index cannot be negative" );
			if( size() <= index ) throw new IndexOutOfBoundsException( "Index is out of bounds" );
			
			return (char)( isFlatStrategy ?
			                   values[ index ] :
			                   values[ nulls.rank( index ) - 1 ] ); // Rank-based access in compressed strategy.
		}
		
		/**
		 * Returns the first logical index where a non-null element with the specified
		 * primitive value is found.
		 * <p>
		 * This method only searches among the non-null elements. To find indices of
		 * nulls, use {@link #nextNullIndex(int)}.
		 *
		 * @param value The primitive value to search for.
		 * @return The logical index of the first occurrence of the specified value, or -1 if
		 * the value is not found among the non-null elements.
		 */
		public int indexOf( char value ) {
			if( isFlatStrategy ) {
				for( int i = nulls.next1( -1 ); i != -1; i = nulls.next1( i ) )
					if( values[ i ] == ( char ) value ) return i; // Linear search in flat array.
				return -1;
			}
			
			int i = Array.indexOf( values, ( char ) value, 0, cardinality ); // Search in compressed array.
			return i < 0 ?
			       -1 :
			       // Value not found in compressed array.
			       nulls.bit( i + 1 ); // Convert rank in compressed array to logical index.
		}
		
		/**
		 * Checks if this list contains at least one non-null element with the specified
		 * primitive value.
		 *
		 * @param value The primitive value to check for.
		 * @return {@code true} if the value is found among the non-null elements, {@code false} otherwise.
		 */
		public boolean containsKey( char value ) { return indexOf( value ) != -1; }
		
		/**
		 * Checks if this list contains at least one element equal to the specified
		 * boxed primitive value.
		 * <p>
		 * If the input {@code value} is {@code null}, this checks if the list contains
		 * at least one null element. If the input {@code value} is non-null, this checks
		 * if the list contains at least one non-null element with that primitive value.
		 *
		 * @param value The boxed value (or {@code null}) to check for.
		 * @return {@code true} if the list contains the specified value (or a null if
		 * input is null), {@code false} otherwise.
		 */
		public boolean containsKey(  Character value ) {
			return value == null ?
			       nextNullIndex( -1 ) != -1 :
			       // Check for null value.
			       indexOf( value ) != -1;     // Check for non-null value.
		}
		
		/**
		 * Returns the last logical index where a non-null element with the specified
		 * primitive value is found.
		 * <p>
		 * This method only searches among the non-null elements.
		 *
		 * @param value The primitive value to search for.
		 * @return The logical index of the last occurrence of the specified value, or -1 if
		 * the value is not found among the non-null elements.
		 */
		public int lastIndexOf( char value ) {
			if( isFlatStrategy ) {
				for( int i = nulls.last1(); i != -1; i = nulls.prev1( i - 1 ) )
					if( values[ i ] == ( char ) value ) return i; // Reverse linear search in flat array.
				return -1;
			}
			else {
				int i = Array.lastIndexOf( values, ( char ) value, 0, cardinality ); // Reverse search in compressed array.
				return i < 0 ?
				       -1 :
				       // Value not found in compressed array.
				       nulls.bit( i + 1 ); // Convert rank to logical index.
			}
		}
		
		/**
		 * Compares this list to another object for logical content equality.
		 * Two lists are considered equal if they have the same logical size
		 * and the same elements (including nulls and their values) at each corresponding index.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the specified object is a list of the
		 * exact same class with the same logical content, {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) );
		}
		
		/**
		 * Computes a hash code for this list based on its logical content.
		 * The hash code is independent of the internal storage strategy. It considers
		 * both the presence/absence of values (nullity) and the values themselves.
		 *
		 * @return A hash code value for this list.
		 */
		@Override
		public int hashCode() {
			int hash = 133;
			
			for( int i = 0, size = size(); i < size; i++ )
			     hash = Array.mix( hash, Array.hash( hasValue( i ) ?
			                                         get( i ) :
			                                         17 ) );
			return Array.finalizeHash( hash, size() ); // Finalize the hash with the length of the list.
		}
		
		/**
		 * Checks logical equality between this list and another
		 * {@link R} instance.
		 *
		 * @param other The {@link R} instance to compare with.
		 * @return {@code true} if both lists have the same logical size and the same
		 * elements (including nulls and their values) at each corresponding logical index,
		 * {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			int size;
			if( other == this ) return true;
			if( other == null || ( size = size() ) != other.size() ) return false; // Quick checks for inequality.
			
			boolean b;
			for( int i = 0; i < size; i++ )
				if( ( b = hasValue( i ) ) != other.hasValue( i ) || b && get( i ) != other.get( i ) ) return false;
			
			return true; // Lists are equal if all checks pass.
		}
		
		/**
		 * Creates a deep copy of this list, preserving content and strategy.
		 *
		 * @return Cloned {@code R} instance.
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				dst.values         = values.clone(); // Deep clone the value array.
				dst.nulls          = nulls.clone();   // Deep clone the nullity bitlist.
				dst.cardinality    = cardinality;
				dst.isFlatStrategy = isFlatStrategy;
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace(); // Handle clone exception (should not typically occur).
			}
			return null; // Return null if cloning fails unexpectedly.
		}
		
		/**
		 * Returns a JSON string of the list, with nulls represented as JSON {@code null}.
		 *
		 * @return JSON representation, e.g., {@code [1, null, 3]}.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the list content to a JSON writer, representing null elements as JSON {@code null}.
		 *
		 * @param json Writer to output JSON.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray(); // Start JSON array.
			int size = size();
			if( 0 < size ) {
				json.preallocate( size * 10 ); // Pre-allocate buffer to improve writing performance, assuming ~10 bytes per element in JSON.
				
				for( int i = -1, ii = -1; i < size; ) {
					
					if( ( i = nextValueIndex( i ) ) == -1 ) { // No more non-null values or reached end of list.
						while( ++ii < size ) json.value(); // Write remaining nulls as JSON null.
						break;
					}
					while( ++ii < i ) json.value(); // Write nulls up to the next non-null value.
					json.value( get( i ) );
				}
			}
			json.exitArray();
		}
		
		/**
		 * Copies a range of elements from this list into a new or provided boxed primitive array.
		 * Null elements in the list are represented as {@code null} in the destination array.
		 *
		 * @param index Starting logical index (inclusive, 0-indexed) from which to start copying.
		 * @param len   The number of elements to copy.
		 * @param dst   The destination array. If {@code null} or too small to hold {@code len} elements,
		 *              a new array of size {@code len} is created.
		 * @return The populated boxed primitive array containing the copied elements.
		 * Returns {@code dst} if the list is empty and {@code dst} is provided and not too small.
		 */
		public  Character[] toArray( int index, int len,  Character[] dst ) {
			if( size() == 0 ) return dst;
			if( dst == null || dst.length < len ) dst = new  Character[ len ];
			
			for( int i = 0, srcIndex = index; i < len && srcIndex < size(); i++, srcIndex++ )
			     dst[ i ] =
			     hasValue( srcIndex ) ?
			     get( srcIndex ) :
			     null;
			return dst;
		}
		
		/**
		 * Copies a range of elements from this list into a new or provided primitive array.
		 * Null elements in the list are substituted with the specified {@code null_substitute} value.
		 *
		 * @param index           Starting logical index (inclusive, 0-indexed) from which to start copying.
		 * @param len             The number of elements to copy.
		 * @param dst             The destination array. If {@code null} or too small to hold {@code len} elements,
		 *                        a new array of size {@code len} is created.
		 * @param null_substitute The primitive value to use for null elements in the list.
		 * @return The populated array of primitive values. Returns {@code dst} if the list is empty
		 * and {@code dst} is provided and not too small.
		 */
		public char[] toArray( int index, int len, char[] dst, char null_substitute ) {
			if( size() == 0 ) return dst;
			
			if( dst == null || dst.length < len ) dst = new char[ len ];
			
			for( int i = 0, srcIndex = index; i < len && srcIndex < size(); i++, srcIndex++ )
			     dst[ i ] =
			     hasValue( srcIndex ) ?
			     get( srcIndex ) :
			     null_substitute;
			return dst;
		}
		
		
		/**
		 * Checks if this list contains all elements from another list.
		 * <p>
		 *
		 * @param src The source list whose elements are to be checked for containment within this list.
		 * @return {@code true} if this list contains all non-null values present in {@code src}
		 * and if {@code src} contains any null elements, this list also contains at least one null element;
		 * {@code false} otherwise.
		 * @implSpec This check is based on value presence, not position. It verifies:
		 * <ul>
		 *     <li>For every non-null value present in the source list ({@code src}),
		 *         that same value must be present at least once as a non-null
		 *         element in this list.</li>
		 *     <li>If the source list ({@code src}) contains *any* null elements, this
		 *         list must also contain at least one null element.</li>
		 * </ul>
		 * Note: This method does *not* check if the size or positions match.
		 */
		public boolean containsAll( R src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				if( src.hasValue( i ) ) {
					if( indexOf( src.get( i ) ) == -1 )
						return false;
				}
				else if( nextNullIndex( -1 ) == -1 ) return false;
			
			return true;
		}
		
		
	}
	
	/**
	 * {@code RW} extends {@link R} to provide read-write functionality for a list that
	 * can store primitive values and represent nulls.
	 * <p>
	 * This class allows modification of the list's content and structure, including
	 * setting, adding, and removing elements (both null and non-null). It manages the
	 * internal storage strategy dynamically, switching between Compressed and Flat
	 * strategies based on the density of non-null elements and a configurable threshold
	 * to maintain a balance between memory usage and performance.
	 * </p>
	 *
	 * @implSpec This class implements the mutation methods by modifying the underlying
	 * `nulls` {@link BitList.RW} and the `values` primitive array. It
	 * contains logic to resize and shift the `values` array as needed
	 * and to trigger strategy switches ({@link #switchToFlatStrategy()},
	 * {@link #switchToCompressedStrategy()}) when necessary.
	 */
	class RW extends R {
		
		/**
		 * Constructs a new {@code RW} list with a specified initial capacity for internal storage
		 * and a default flat strategy threshold of 64.
		 * <p>
		 * The {@code items} parameter controls the initial allocation and logical size:
		 * <ul>
		 *     <li>If positive, it sets the initial capacity of the underlying internal array.
		 *         The logical size of the list remains 0.</li>
		 *     <li>If negative, the list is initialized with a capacity and logical size equal to
		 *         {@code -items}. All elements from logical index 0 up to {@code -items - 1}
		 *         are initially set to null.</li>
		 *     <li>If zero, the list starts with a small default capacity (e.g., 16) and a logical size of 0.</li>
		 * </ul>
		 *
		 * @param items The initial capacity or a negative value to set initial size and capacity.
		 * @see #RW(int, int)
		 */
		public RW( int items ) { this( items, 64 ); }
		
		/**
		 * Constructs a new {@code RW} list with a specified initial capacity for internal storage
		 * and a custom flat strategy threshold.
		 * <p>
		 * The {@code items} parameter controls the initial allocation and logical size:
		 * <ul>
		 *     <li>If positive, it sets the initial capacity of the underlying internal array.
		 *         The logical size of the list remains 0.</li>
		 *     <li>If negative, the list is initialized with a capacity and logical size equal to
		 *         {@code -items}. All elements from logical index 0 up to {@code -items - 1}
		 *         are initially set to null.</li>
		 *     <li>If zero, the list starts with a small default capacity (e.g., 16) and a logical size of 0.</li>
		 * </ul>
		 *
		 * @param items                 The initial capacity or a negative value to set initial size and capacity.
		 * @param flatStrategyThreshold The threshold used to decide when to switch to the Flat Strategy.
		 *                              If the number of non-null elements exceeds this value,
		 *                              the list may switch to the Flat Strategy during modifications.
		 *                              Must be non-negative.
		 * @throws IllegalArgumentException if {@code flatStrategyThreshold} is negative.
		 */
		public RW( int items, int flatStrategyThreshold ) {
			if( flatStrategyThreshold < 0 ) throw new IllegalArgumentException( "flatStrategyThreshold cannot be negative" );
			this.flatStrategyThreshold = flatStrategyThreshold;
			int length = Math.abs( items );
			nulls  = new BitList.RW( length );
			values = length == 0 ?
			         Array.EqualHashOf.chars     .O :
			         new char[ length ];
			if( items < 0 ) set1( -items - 1, null );// + set size
		}
		
		
		/**
		 * Sets the threshold for automatically switching to the Flat Strategy.
		 * If the number of non-null elements (`cardinality`) exceeds this threshold,
		 * mutation operations might trigger a switch to the Flat Strategy.
		 * Lowering the threshold while in Compressed Strategy might trigger an
		 * immediate switch to Flat Strategy if the current `cardinality` meets
		 * the new threshold. Raising the threshold while in Flat Strategy might
		 * trigger an immediate switch back to Compressed Strategy if the current
		 * `cardinality` falls below the new threshold.
		 *
		 * @param threshold The new threshold value (must be non-negative).
		 *                  A higher value keeps the list in Compressed Strategy longer
		 *                  (favors space). A lower value encourages switching to Flat
		 *                  Strategy sooner (favors time).
		 * @throws IllegalArgumentException if {@code threshold} is negative.
		 * @see #switchToFlatStrategy()
		 * @see #switchToCompressedStrategy()
		 */
		public void flatStrategyThreshold( int threshold ) {
			
			if( ( flatStrategyThreshold = threshold ) < 0 ) throw new IllegalArgumentException( "Threshold cannot be negative" );
			
			if( isFlatStrategy ) { if( cardinality() <= threshold ) switchToCompressedStrategy(); }
			else if( threshold <= cardinality() ) switchToFlatStrategy();
		}
		
		
		/**
		 * Creates a deep copy of this {@code RW} instance.
		 *
		 * @return A new {@code RW} instance identical in content and strategy to this one.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		/**
		 * Removes the last logical element from the list. Decreases the logical size by one.
		 * If the list is empty, this method has no effect.
		 *
		 * @return This instance for method chaining.
		 */
		public RW remove() { return remove( size() - 1 ); }
		
		/**
		 * Removes the logical element at the specified index, shifting all subsequent
		 * elements one position to the left (towards lower indices). Decreases the
		 * logical size by one.
		 * If the index is out of the valid range [0, size() - 1], this method has no effect.
		 *
		 * @param index The logical index of the element to remove.
		 * @return This instance for method chaining.
		 */
		public RW remove( int index ) {
			if( index < 0 || size() <= index ) return this;
			
			if( isFlatStrategy ) Array.resize( values, values, index, nulls.size, -1 );
			else if( nulls.get( index ) ) cardinality = Array.resize( values, values, nulls.rank( index ) - 1, cardinality, -1 ); // Shrink compressed array by removing the value.
			
			
			nulls.remove( index ); // Remove the nullity bit at the index.
			return this;
		}
		
		/**
		 * Sets the logical element at the end of the list (or the first element if the list
		 * is empty) to the specified boxed primitive value.
		 * If the list is empty, this adds a new element at index 0. Otherwise, it modifies
		 * the existing last element. Extends the list size if setting beyond the current end.
		 *
		 * @param value The boxed primitive value to set (may be {@code null}).
		 * @return This instance for method chaining.
		 */
		public RW set1(  Character value ) {
			set( this, Math.max( 0, size() - 1 ), value );
			return this;
		}
		
		/**
		 * Sets the logical element at the end of the list (or the first element if the list
		 * is empty) to the specified primitive value, marking it as non-null.
		 * If the list is empty, this adds a new element at index 0. Otherwise, it modifies
		 * the existing last element. Extends the list size if setting beyond the current end.
		 *
		 * @param value The primitive value to set.
		 * @return This instance for method chaining.
		 */
		public RW set1( char value ) {
			set( this, Math.max( 0, size() - 1 ), value );
			return this;
		}
		
		/**
		 * Sets the logical element at the specified index to the given boxed primitive value.
		 * If the index is beyond the current logical size, the list is extended with null elements
		 * up to the specified index before setting the value at that index.
		 *
		 * @param index The logical index at which to set the value. Must be non-negative.
		 * @param value The boxed primitive value to set (may be {@code null}).
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code index} is negative.
		 */
		public RW set1( int index,  Character value ) {
			set( this, index, value );
			return this;
		}
		
		/**
		 * Sets the logical element at the specified index to the given primitive value,
		 * marking it as non-null.
		 * If the index is beyond the current logical size, the list is extended with null elements
		 * up to the specified index before setting the value at that index.
		 *
		 * @param index The logical index at which to set the value. Must be non-negative.
		 * @param value The primitive value to set.
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code index} is negative.
		 */
		public RW set1( int index, char value ) {
			set( this, index, value );
			return this;
		}
		
		/**
		 * Sets a sequence of elements in the list starting from the specified index,
		 * using values from a variable-length array of boxed primitives.
		 * If the range extends beyond the current logical size, the list is extended
		 * with null elements as needed before setting the specified values.
		 *
		 * @param index  The starting logical index (inclusive, 0-indexed) at which to begin setting. Must be non-negative.
		 * @param values The array of boxed primitive values (may include {@code null}).
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code index} is negative.
		 * @throws NullPointerException     if {@code values} array is null.
		 */
		public RW set( int index,  Character[] values ) {
			if( index < 0 ) throw new IllegalArgumentException( "Index cannot be negative" );
			if( values == null ) throw new NullPointerException( "Values array cannot be null" );
			for( int i = values.length; -1 < --i; ) set( this, index + i, values[ i ] ); // Iterate backwards and set each value.
			return this;
		}
		
		/**
		 * Sets a sequence of elements in the list starting from the specified index,
		 * using values from a variable-length array of primitives.
		 * All elements in the specified range will be marked as non-null.
		 * If the range extends beyond the current logical size, the list is extended
		 * with null elements as needed before setting the specified values.
		 *
		 * @param index  The starting logical index (inclusive, 0-indexed) at which to begin setting. Must be non-negative.
		 * @param values The array of primitive values.
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code index} is negative.
		 * @throws NullPointerException     if {@code values} array is null.
		 */
		public RW set( int index, char... values ) {
			if( index < 0 ) throw new IllegalArgumentException( "Index cannot be negative" );
			if( values == null ) throw new NullPointerException( "Values array cannot be null" );
			return set( index, values, 0, values.length );
		}
		
		/**
		 * Sets a range of elements in the list starting from the specified index,
		 * using a portion of a primitive array.
		 * All elements in the specified range will be marked as non-null.
		 * If the range extends beyond the current logical size, the list is extended
		 * with null elements as needed before setting the specified values.
		 *
		 * @param index     The starting logical index (inclusive, 0-indexed) at which to begin setting. Must be non-negative.
		 * @param src       The source primitive array.
		 * @param src_index The starting index (inclusive, 0-indexed) within the source array.
		 * @param len       The number of elements to copy from the source array.
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code index}, {@code src_index}, or {@code len} are negative,
		 *                                  or if the source range exceeds array bounds.
		 * @throws NullPointerException     if {@code src} array is null.
		 */
		public RW set( int index, char[] src, int src_index, int len ) {
			if( index < 0 ) throw new IllegalArgumentException( "Index cannot be negative" );
			if( src_index < 0 ) throw new IllegalArgumentException( "Source index cannot be negative" );
			if( len < 0 ) throw new IllegalArgumentException( "Length cannot be negative" );
			if( src == null ) throw new NullPointerException( "Source array cannot be null" );
			if( src.length < src_index + len ) throw new IllegalArgumentException( "Source range exceeds array bounds" );
			
			for( int i = len; -1 < --i; )
			     set( this, index + i, ( char ) src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Sets a range of elements in the list starting from the specified index,
		 * using a portion of a boxed primitive array.
		 * If the range extends beyond the current logical size, the list is extended
		 * with null elements as needed before setting the specified values.
		 *
		 * @param index     The starting logical index (inclusive, 0-indexed) at which to begin setting. Must be non-negative.
		 * @param src       The source boxed primitive array (may include {@code null}).
		 * @param src_index The starting index (inclusive, 0-indexed) within the source array.
		 * @param len       The number of elements to copy from the source array.
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code index}, {@code src_index}, or {@code len} are negative,
		 *                                  or if the source range exceeds array bounds.
		 * @throws NullPointerException     if {@code src} array is null.
		 */
		public RW set( int index,  Character[] src, int src_index, int len ) {
			if( index < 0 ) throw new IllegalArgumentException( "Index cannot be negative" );
			if( src_index < 0 ) throw new IllegalArgumentException( "Source index cannot be negative" );
			if( len < 0 ) throw new IllegalArgumentException( "Length cannot be negative" );
			if( src == null ) throw new NullPointerException( "Source array cannot be null" );
			if( src.length < src_index + len ) throw new IllegalArgumentException( "Source range exceeds array bounds" );
			
			for( int i = len; -1 < --i; ) set( this, index + i, src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Adds a boxed primitive value to the end of the list. Increases the logical size by one.
		 * May trigger a strategy switch if adding a non-null value causes the cardinality
		 * to exceed the {@code flatStrategyThreshold}.
		 *
		 * @param value The boxed primitive value to add (may be {@code null}).
		 * @return This instance for method chaining.
		 */
		public RW add1(  Character value ) {
			set( this, size(), value );
			return this;
		}
		
		/**
		 * Adds a primitive value to the end of the list, marking it as non-null.
		 * Increases the logical size by one. May trigger a strategy switch if adding
		 * the value causes the cardinality to exceed the {@code flatStrategyThreshold}.
		 *
		 * @param value The primitive value to add.
		 * @return This instance for method chaining.
		 */
		public RW add1( char value ) {
			set( this, size(), value );
			return this;
		}
		
		/**
		 * Adds multiple primitive values to the end of the list, marking them as non-null.
		 * Increases the logical size by the number of items added. May trigger a strategy
		 * switch if the resulting cardinality exceeds the {@code flatStrategyThreshold}.
		 *
		 * @param items The variable-length array of primitive values to add.
		 * @return This instance for method chaining.
		 */
		public RW add( char... items ) { // VEXT -> int
			return items == null || items.length == 0 ?
			       this :
			       set( size(), items, 0, items.length );
		}
		
		/**
		 * Adds multiple boxed primitive values to the end of the list. Increases the
		 * logical size by the number of items added. May trigger a strategy switch.
		 *
		 * @param items The array of boxed primitive values (may include {@code null}).
		 * @return This instance for method chaining.
		 */
		public RW add(  Character[] items ) { return set( size(), items ); }
		
		
		/**
		 * Inserts a boxed primitive value at the specified index, shifting all existing
		 * elements at and after that index one position to the right (towards higher indices).
		 * Increases the logical size by one. If the index is beyond the current logical size,
		 * this acts like adding elements (nulls plus the specified value) to the end.
		 * May trigger a strategy switch.
		 *
		 * @param index The logical index (0-indexed) at which to insert the value. Must be non-negative.
		 * @param value The boxed primitive value to insert (may be {@code null}).
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code index} is negative.
		 */
		public RW add1( int index,  Character value ) {
			if( index < 0 ) throw new IllegalArgumentException( "Index cannot be negative" );
			if( value == null ) {
				int s = size();
				nulls.add( index, false );
				
				if( isFlatStrategy )
					Array.resize( values,
					              values.length <= nulls.size() - 1 ?
					              values = new char[ ( nulls.size() - 1 ) * 2 / 3 ] :
					              values, index, s, 1 );
			}
			else add1( index, value.charValue     () );
			return this;
		}
		
		/**
		 * Inserts a primitive value at the specified index, marking it as non-null and
		 * shifting all existing elements at and after that index one position to the right
		 * (towards higher indices). Increases the logical size by one. If the index is beyond
		 * the current logical size, this acts like setting the value at that index (which
		 * extends the list with nulls). May trigger a strategy switch if the resulting cardinality
		 * exceeds the {@code flatStrategyThreshold}.
		 *
		 * @param index The logical index (0-indexed) at which to insert the value. Must be non-negative.
		 * @param value The primitive value to insert.
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code index} is negative.
		 */
		public RW add1( int index, char value ) {
			if( index < 0 ) throw new IllegalArgumentException( "Index cannot be negative" );
			if( index < size() )// Inserting within bounds.
				if( isFlatStrategy ) {
					// Insert non-null bit at index.
					
					nulls.add( index, true );
					Array.resize( values,
					              values.length <= nulls.size() - 1 ?
					              values = new char[ nulls.size() * 3 / 2 ] :
					              values, index, size(), 1 );
					
					values[ index ] = ( char ) value; // Insert value in flat array.
				}
				else { // Compressed strategy.
					
					// Insert non-null bit at index.
					
					nulls.add( index, true );
					cardinality++;
					int rank = nulls.rank( index ) - 1; // Calculate rank for insertion.
					
					if( values.length <= cardinality && flatStrategyThreshold <= cardinality ) {
						switchToFlatStrategy();
						values[ index ] = ( char ) value; // Insert value in flat array.
						return this;
					}
					
					
					Array.resize( values, values.length <= cardinality ?
					                      values = new char[ cardinality * 3 / 2 ] :
					                      values, rank, cardinality - 1, 1 ); // Make space for new value in compressed array.
					values[ rank ] = ( char ) value; // Insert value in compressed array.
				}
			else set1( index, value ); // If index is beyond current size, treat as set operation.
			return this;
		}
		
		
		/**
		 * Adds all elements from another list to the end of this list.
		 * The logical size of this list is increased by the size of the source list.
		 * Handles both null and non-null elements from the source list. May trigger
		 * strategy switches.
		 *
		 * @param src The source {@code R} list whose elements are to be added.
		 * @return This instance for method chaining.
		 * @throws NullPointerException if {@code src} is null.
		 */
		public RW addAll( R src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				// Add null if source has null at this position.
				if( src.hasValue( i ) ) add1( src.get( i ) ); // Add non-null value from source.
				else nulls.set( nulls.size, false );
			return this;
		}
		
		/**
		 * Clears all elements from the list, setting the logical size to 0.
		 * Resets `cardinality` to 0, clears the underlying nullity bitlist,
		 * and clears (zeros out) the `values` array up to its `used` limit (which becomes 0 after clear).
		 * The capacity of the `values` array may be retained.
		 *
		 * @return This instance for method chaining.
		 */
		public RW clear() {
			cardinality = 0; // Reset cardinality.
			nulls.clear(); // Clear the nulls bitlist.
			return this;
		}
		
		/**
		 * Sets the physical capacity of the underlying storage (`values` array) and
		 * the logical size of the nullity bitlist.
		 * <p>
		 * If the new length {@code length} is less than the current logical size,
		 * the list is truncated to {@code length}. Elements at indices {@code length}
		 * and higher are discarded. The underlying {@code values} array is also
		 * resized to match the new requirement, potentially triggering a strategy switch.
		 * If the new length is greater than the current capacity, the capacity is increased.
		 * If {@code length} is less than 1, the list is cleared.
		 *
		 * @param length The desired new physical capacity and maximum logical length hint.
		 *               Must be non-negative.
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code length} is negative.
		 */
		public RW length( int length ) {
			if( length < 0 ) throw new IllegalArgumentException( "length cannot be negative" );
			
			boolean shrink = length < nulls.size();
			nulls.length( length ); // Set the length of the nullity bitlist.
			
			if( length == 0 ) {
				cardinality = 0; // Reset cardinality.
				values      = Array.EqualHashOf.chars     .O;
				return this;
			}
			
			if( isFlatStrategy ) {
				if( shrink && nulls.cardinality() <= flatStrategyThreshold ) switchToCompressedStrategy();
				else if( values.length != length ) values = Arrays.copyOf( values, length );
			}
			else {
				if( shrink ) cardinality = nulls.cardinality();
				if( flatStrategyThreshold <= cardinality ) switchToFlatStrategy( length );
				else if( values.length != length ) values = Arrays.copyOf( values, length );
			}
			
			return this;
		}
		
		/**
		 * Sets the logical size of the list.
		 * <p>
		 * If the new size is smaller than the current size, the list is truncated,
		 * discarding elements (both null and non-null) at logical indices {@code size} and above.
		 * This is done by setting the size of the backing nullity bitlist and adjusting
		 * cardinality if needed in Compressed strategy.
		 * If the new size is larger than the current size, the list is expanded,
		 * conceptually padding with null elements at the end to reach the new size.
		 * This is achieved by setting the nullity bit of the last element (new_size - 1)
		 * to false, which extends the bitlist with zeros if needed.
		 *
		 * @param size The desired new logical size of the list. Must be non-negative.
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code size} is negative.
		 */
		public RW size( int size ) {
			if( size < 0 ) throw new IllegalArgumentException( "size cannot be negative" );
			if( size == 0 ) return clear(); // If size is 0, clear the list.
			
			if( this.size() < size ) set1( size - 1, null ); // If increasing size, ensure last element is set (though it might be null).
			else {
				nulls.size( size ); // Set new size for nulls bitlist.
				if( !isFlatStrategy ) cardinality = nulls.cardinality();
			}
			return this;
		}
		
		/**
		 * Minimizes memory usage by adjusting the physical capacity of the underlying
		 * `values` array to be just large enough to hold the current logical content.
		 * This is equivalent to calling {@code length(size())}.
		 * May trigger a strategy switch if the current density warrants it.
		 *
		 * @return This instance for method chaining.
		 */
		public RW fit() {
			length( size() );
			return this;
		}
		
		/**
		 * Trims trailing null elements by adjusting the logical size down to the index
		 * of the last non-null element plus one. If the list contains only nulls or is
		 * empty, the size becomes 0.
		 * <p>
		 * This is equivalent to calling {@code length(lastNonNullableIndex + 1)}.
		 * May trigger a strategy switch back to the Compressed Strategy if trimming
		 * results in a low density of non-nulls (cardinality falls below the threshold).
		 *
		 * @return This instance for method chaining.
		 */
		public RW trim() {
			
			length( nulls.last1() + 1 ); // Set length to trim trailing nulls.
			if( isFlatStrategy && cardinality() < flatStrategyThreshold ) switchToCompressedStrategy();
			return this;
		}
		
		/**
		 * Swaps the logical elements (including nullity and value if non-null) at the
		 * two specified indices.
		 *
		 * @param index1 The logical index of the first element. Must be within [0, size() - 1].
		 * @param index2 The logical index of the second element. Must be within [0, size() - 1].
		 * @return This instance for method chaining.
		 * @throws IndexOutOfBoundsException If either index is negative or greater than or equal to `size()`.
		 */
		public RW swap( int index1, int index2 ) {
			if( index1 < 0 || index1 >= size() ) throw new IndexOutOfBoundsException( "Index1 must be non-negative and less than the list's size: " + index1 );
			if( index2 < 0 || index2 >= size() ) throw new IndexOutOfBoundsException( "Index2 must be non-negative and less than the list's size: " + index2 );
			
			if( index1 == index2 ) return this;
			
			boolean e1 = hasValue( index1 ); // Get nullity status of element at index1.
			boolean e2 = hasValue( index2 ); // Get nullity status of element at index2.
			if( !e1 && !e2 ) return this;
			char v1 = e1 ?
			                 get( index1 ) :
			                 0;
			char v2 = e2 ?
			                 get( index2 ) :
			                 0;
			
			if( e1 && e2 )
				if( v1 == v2 ) return this;
				else {
					set1( index1, v2 );
					set1( index2, v1 );
					return this;
				}
			
			if( e1 ) {
				set1( index1, null );
				set1( index2, v1 );
			}
			else {
				set1( index1, v2 );
				set1( index2, null );
			}
			return this;
		}
		
		/**
		 * Switches the storage strategy to Flat.
		 * The `values` array is reallocated to the size of the logical list (`size()`),
		 * and non-null values are copied from their potentially compressed locations
		 * to their direct logical indices in the new flat array.
		 *
		 * @implSpec This method assumes `nulls` and `cardinality` are up-to-date.
		 * If currently in Compressed mode, it iterates through the logical
		 * indices of non-nulls using `nulls.next1` and copies the corresponding
		 * values from the compressed `values` array into the correct logical
		 * position in the new flat array. If already in Flat mode, it simply
		 * reallocates and copies to a potentially larger flat array.
		 */
		protected void switchToFlatStrategy() { switchToFlatStrategy( nulls.size() ); }
		
		/**
		 * Switches the storage strategy to Flat with a specified minimum capacity.
		 * The `values` array is reallocated to at least the specified capacity (and
		 * at least the current logical size), and non-null values are copied to their
		 * direct logical indices.
		 *
		 * @param capacity The minimum desired capacity for the new flat `values` array.
		 *                 The actual capacity will be at least `Math.max(size(), capacity)`.
		 * @implSpec If currently Compressed, this method iterates through the packed
		 * `values` array and maps each value to its correct logical index in the new
		 * flat array, using the `nulls` bitlist to determine logical positions.
		 * If already Flat, it copies values from the current flat array to the new flat array.
		 */
		protected void switchToFlatStrategy( int capacity ) {
			isFlatStrategy = true;
			cardinality    = 0; // Flat strategy uses nulls.Cardinality() dynamically, so reset internal counter.
			if( size() == 0 )//the collection is empty
			{
				if( values.length != capacity ) values = new char[ Math.max( 16, capacity ) ];
				return;
			}
			
			char[] compressed = values;
			values = new char[ Math.max( size(), capacity ) ]; // Allocate flat array with sufficient capacity.
			for( int i = -1, ii = 0; ( i = nulls.next1( i ) ) != -1; )
			     values[ i ] = compressed[ ii++ ];
		}
		
		
		/**
		 * Switches the storage strategy to Compressed.
		 * The non-null values are packed contiguously into the front of the `values` array.
		 * The size of the `values` array is adjusted to the current `cardinality`.
		 *
		 * @implSpec Iterates through the logical indices of non-null elements using
		 * `nulls.next1` and copies their values into the `values` array,
		 * packing them into the front. If the current strategy was Flat,
		 * this involves copying from `values[i]` to `values[ii]` where `i`
		 * is the logical index and `ii` is the compressed index (rank-1).
		 * The array is then resized to `cardinality`.
		 */
		protected void switchToCompressedStrategy() {
			
			cardinality = nulls.cardinality(); // Count of non-null elements.
			int ii = 0;
			for( int i = -1; ( i = nulls.next1( i ) ) != -1; )
				if( i != ii ) values[ ii++ ] = values[ i ];// Pack non-null values sequentially in the same array.
			
			values         = Arrays.copyOf( values, ii );
			isFlatStrategy = false;
		}
		
		/**
		 * Static helper method to set a boxed primitive value at a specific logical index.
		 * This method handles determining if the value is null or non-null, updating the
		 * nullity bitlist, modifying the `values` array based on the current strategy,
		 * and potentially triggering strategy switches or array resizing.
		 *
		 * @param dst   The target {@code RW} list.
		 * @param index The logical index at which to set the value. Must be non-negative.
		 * @param value The boxed primitive value to set (may be {@code null}).
		 * @implSpec This method encapsulates the core logic for handling nullity and strategy
		 * when setting individual elements via the public `set` and `add` methods.
		 * If `value` is null, it marks the element as null in `nulls` and removes
		 * the corresponding value from `values` if in Compressed strategy.
		 * If `value` is non-null, it delegates to the primitive-value `set` helper.
		 */
		protected static void set( RW dst, int index,  Character value ) {
			if( value == null ) {
				if( dst.nulls.last1() < index ) dst.nulls.set0( index ); // If index is beyond current length, extend and set as null.
				else if( dst.nulls.get( index ) ) { // If index was previously non-null, convert to null.
					if( !dst.isFlatStrategy )
						dst.cardinality = Array.resize( dst.values, dst.values, dst.nulls.rank( index ) - 1, dst.cardinality, -1 ); // Remove value from compressed array.
					dst.nulls.set0( index );
				}
			}
			else set( dst, index, value.charValue     () );
		}
		
		/**
		 * Static helper method to set a primitive value at a specific logical index,
		 * marking it as non-null.
		 * This method handles updating the nullity bitlist, modifying the `values` array
		 * based on the current strategy, and potentially triggering strategy switches
		 * or array resizing.
		 *
		 * @param dst   The target {@code RW} list.
		 * @param index The logical index at which to set the value. Must be non-negative.
		 * @param value The primitive value to set.
		 * @implSpec This method is the core logic for handling non-null values when
		 * setting individual elements via the public `set` and `add` methods.
		 * It manages inserting or updating values in the `values` array based
		 * on the strategy. For Flat Strategy, it ensures array capacity and sets the value directly.
		 * For Compressed Strategy, it calculates the rank, potentially resizes the array
		 * to insert a new value or updates an existing one, and checks the `flatStrategyThreshold`
		 * to initiate a strategy switch if necessary and beneficial.
		 */
		protected static void set( RW dst, int index, char value ) {
			if( index < 0 ) throw new IllegalArgumentException( "Index cannot be negative" );
			
			if( dst.isFlatStrategy ) {
				if( dst.values.length <= index ) dst.values = Arrays.copyOf( dst.values,
				                                                             index == 0 ?
				                                                             16 :
				                                                             index * 3 / 2 ); // Ensure array capacity.
				dst.values[ index ] = ( char ) value; // Set value in flat array.
				dst.nulls.set1( index ); // Mark as non-null.
			}
			else if( dst.nulls.get( index ) ) dst.values[ dst.nulls.rank( index ) - 1 ] = ( char ) value; // Update existing value.
			else {
				if( dst.flatStrategyThreshold <= dst.cardinality ) {
					dst.switchToFlatStrategy( Math.max( index + 1, dst.nulls.size() * 3 / 2 ) );
					dst.nulls.set1( index ); // Mark as non-null.
					dst.values[ index ] = ( char ) value; // Set value in flat array.
				}
				else {
					int rank = dst.nulls.rank( index );
					int max  = Math.max( rank, dst.cardinality );
					
					dst.cardinality    = Array.resize( dst.values,
					                                   dst.values.length <= max ?
					                                   ( dst.values = new char[ max == 0 ?
					                                                                   16 :
					                                                                   max * 3 / 2 ] ) :
					                                   dst.values,
					                                   rank, dst.cardinality, 1 ); // Insert a slot for the new value.
					dst.values[ rank ] = ( char ) value; // Set the new value in compressed array.
					dst.nulls.set1( index ); // Mark as non-null.
				}
			}
		}
	}
}