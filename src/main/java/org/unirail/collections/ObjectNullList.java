// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit others to do so, under the following conditions:
//
// 1. The above copyright notice and this permission notice must be included in all
//    copies or substantial portions of the Software.
//
// 2. Users of the Software must provide a clear acknowledgment in their user
//    documentation or other materials that their solution includes or is based on
//    this Software. This acknowledgment should be prominent and easily visible,
//    and can be formatted as follows:
//    "This product includes software developed by Chikirev Sirguy and the Unirail Group
//    (https://github.com/AdHoc-Protocol)."
//
// 3. If you modify the Software and distribute it, you must include a prominent notice
//    stating that you have changed the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// Acknowledgment: This class is derived from and builds upon the design of IntNullList
// by Chikirev Sirguy and the Unirail Group, adapted for generic nullable objects.

package org.unirail.collections;

import org.unirail.JsonWriter;

/**
 * A list interface for efficiently managing nullable objects of type {@code V}.
 * Unlike a standard list, which stores all elements including nulls in a single array,
 * this interface uses two strategies to optimize memory and performance:
 * <ul>
 *     <li><b>Compressed Strategy</b>: Minimizes memory by storing only non-null values in a compact array,
 *         using a bit list to track nullity.</li>
 *     <li><b>Flat Strategy</b>: Prioritizes fast access by storing all elements, including nulls, in a single array.</li>
 * </ul>
 * Implementations dynamically switch between these strategies based on null density and a configurable threshold,
 * balancing memory efficiency and access speed for specific use cases.
 *
 * @param <V> The type of elements stored in the list, which may include null.
 */
public interface ObjectNullList< V > {
	
	/**
	 * Abstract base class providing read-only functionality for lists of nullable objects.
	 * It implements two storage strategies for nullable objects:
	 * <p>
	 * <b>Compressed Strategy</b>:
	 * - Optimizes memory for lists with many nulls by storing only non-null values in an array.
	 * - Uses a bit list ({@code nulls}) to track nullity, where {@code true} indicates a non-null value
	 * and {@code false} indicates null.
	 * - Access requires rank calculations on the bit list, trading some performance for memory savings.
	 * <p>
	 * <b>Flat Strategy</b>:
	 * - Optimizes access speed for lists with few nulls by storing all elements, including nulls, in a single
	 * array.
	 * - Discards the bit list, using {@code null} entries in the values array to indicate nullity, eliminating rank
	 * calculation overhead.
	 * <p>
	 * The strategy is chosen based on null density compared to {@code flatStrategyThreshold}, ensuring an optimal
	 * balance between memory usage and performance.
	 *
	 * @param <V> The type of elements in the list, allowing null values.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		/**
		 * Used in the compressed strategy only
		 * Using to tracks nullity. Each bit corresponds to a logical index: {@code true} for non-null, {@code false} for null.
		 * In flat strategy, is null and not used, as nullity is tracked directly in the values array V[].
		 */
		protected BitList.RW nulls;
		
		/**
		 * Stores the list's elements.
		 * In compressed strategy, contains only non-null values.
		 * In flat strategy, contains all elements, including nulls.
		 */
		protected V[] values;
		
		/**
		 * Provides type-safe equality checks, hashing, and array creation for elements of type {@code V}.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		/**
		 * In compressed strategy: the number of non-null elements (cardinality).
		 * In flat strategy:       total size (including nulls).
		 */
		protected int size_card = 0;
		
		/**
		 * Returns the number of non-null elements currently in the list.
		 * This count reflects the actual number of stored values, excluding explicit null entries.
		 *
		 * @return The count of non-null elements (cardinality).
		 */
		public int cardinality() {
			if( !isFlatStrategy ) return size_card;                     // In compressed mode, size_card is the cardinality.
			
			
			int count = 0;// Need to count in flat mode.
			for( int i = 0; i < size_card; i++ )
				if( values[ i ] != null )
					count++;
			
			return count;
		}
		
		/**
		 * Threshold for switching from compressed to flat strategy, defaulting to {@code 64}.
		 * When the number of non-null elements exceeds this threshold, the list may switch to the flat strategy
		 * to optimize access speed. This value balances memory efficiency and performance.
		 */
		protected int flatStrategyThreshold = 64;
		
		public int flatStrategyThreshold() { return flatStrategyThreshold; }
		
		/**
		 * Indicates the current storage strategy: {@code true} for flat, {@code false} for compressed.
		 */
		protected boolean isFlatStrategy = false;
		
		/**
		 * Constructs a read-only instance for elements of type {@code V}.
		 *
		 * @param clazzV The class of the element type {@code V}, used to initialize the utility for type-safe operations.
		 */
		protected R( Class< V > clazzV ) {
			this.equal_hash_V = Array.get( clazzV );
			this.values       = equal_hash_V.OO; // Empty array for initial efficiency
		}
		
		/**
		 * Constructs a read-only instance with a provided equality and hashing utility.
		 *
		 * @param equal_hash_V The utility for type-safe operations on elements of type {@code V}.
		 */
		public R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
			this.values       = equal_hash_V.OO;
		}
		
		/**
		 * Switches to the flat strategy, reallocating the underlying values array to include all elements, including nulls.
		 */
		protected void switchToFlatStrategy() { switchToFlatStrategy( nulls.size ); }
		
		/**
		 * Switches the internal storage to the flat strategy, ensuring enough capacity.
		 * All elements, including nulls, will be stored directly in the values array.
		 * If the list is empty, it initializes with a default capacity.
		 *
		 * @param capacity The desired minimum capacity for the underlying array.
		 */
		protected void switchToFlatStrategy( int capacity ) {
			if( size() == 0 )//the collection is empty
			{
				if( values.length == 0 ) values = equal_hash_V.copyOf( null, 16 );
				isFlatStrategy = true;
				size_card      = 0; // Size_card represents logical size in flat strategy
				return;
			}
			
			V[] compressed = values;
			values = equal_hash_V.copyOf( null, Math.max( 16, capacity ) );
			for( int i = -1, ii = 0; ( i = nulls.next1( i ) ) != -1; )
			     values[ i ] = compressed[ ii++ ];
			
			size_card      = nulls.size();
			nulls          = null;
			isFlatStrategy = true;
		}
		
		/**
		 * Switches to the compressed strategy, compacting non-null values and rebuilding the bit list.
		 * Performs a single pass to count and populate for efficiency.
		 */
		protected void switchToCompressedStrategy() {
			nulls = new BitList.RW( size_card );
			int cardinality = 0;
			for( int i = 0; i < size_card; i++ )
				if( values[ i ] != null ) { // packing
					nulls.set1( i );
					values[ cardinality++ ] = values[ i ];
				}
			size_card      = cardinality;// Update size_card to actual cardinality
			isFlatStrategy = false;
		}
		
		/**
		 * Copies a range of elements from this list into a destination array, preserving nulls.
		 * The destination array will be resized if it is {@code null} or too small to hold the specified range.
		 *
		 * @param index The starting logical index in this list from which to begin copying.
		 * @param len   The number of elements to copy.
		 * @param dst   The destination array where elements will be copied. If {@code null} or insufficient in size,
		 *              a new array of the appropriate type and size will be allocated.
		 * @return The destination array (either the provided {@code dst} or a newly allocated one)
		 * containing the copied elements. If the specified range is empty, the original {@code dst} is returned.
		 */
		public V[] toArray( int index, int len, V[] dst ) {
			if( size() == 0 ) return dst;
			// Ensure valid range.
			index = Math.max( 0, index );
			len   = Math.min( len, size() - index );
			if( len <= 0 ) return dst;
			
			if( dst == null || dst.length < len ) dst = equal_hash_V.copyOf( null, len );
			
			if( isFlatStrategy ) System.arraycopy( values, index, dst, 0, Math.min( len, size_card - index ) );
			else for( int i = 0, ii = nulls.rank( index ) - 1; i < len && index < size(); i++, index++ )
			          dst[ i ] = hasValue( index ) ?
			                     values[ ii++ ] :
			                     null;
			return dst;
		}
		
		/**
		 * Checks if this list contains all non-null elements from another list of nullable objects.
		 *
		 * @param src The source list to check against.
		 * @return {@code true} if all non-null elements are present, {@code false} otherwise.
		 */
		public boolean containsAll( ObjectNullList.R< V > src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				if( indexOf( src.get( i ) ) == -1 ) return false;
			return true;
		}
		
		/**
		 * Returns the physical capacity of the internal values array.
		 * This is the maximum number of elements the list can hold without resizing its internal storage.
		 *
		 * @return The current capacity of the underlying values array.
		 */
		public int length() {
			return values.length;
		}
		
		/**
		 * Returns the logical size of the list, including nulls.
		 * This represents the total number of elements, whether they are non-null values or explicit null entries.
		 *
		 * @return The total number of elements in the list.
		 */
		public int size() {
			return isFlatStrategy ?
			       size_card :
			       nulls.size();
		}
		
		/**
		 * Checks if the list is empty.
		 *
		 * @return {@code true} if the list has no elements (logical size is zero), {@code false} otherwise.
		 */
		public boolean isEmpty() {
			return size() < 1;
		}
		
		/**
		 * Checks if the specified logical index contains a non-null value.
		 *
		 * @param index The logical index to check.
		 * @return {@code true} if the index has a non-null value, {@code false} if the element at that index is null or the index is out of bounds.
		 */
		public boolean hasValue( int index ) {
			return 0 <= index && index < size() && (
					isFlatStrategy ?
					values[ index ] != null :
					nulls.get( index ) );
		}
		
		/**
		 * Finds the next logical index after the specified index that contains a non-null value.
		 *
		 * @param index The starting logical index (exclusive) for the search. The search begins
		 *              at `index + 1`. Pass -1 to start the search from the beginning.
		 * @return The logical index of the next non-null element after `index`, or -1 if no more
		 * non-null elements are found in the list.
		 */
		public int nextValueIndex( int index ) {
			if( !isFlatStrategy ) return nulls.next1( index );
			
			for( int i = index; ++i < size_card; i++ )
				if( values[ i ] != null ) return i;
			return -1;
		}
		
		/**
		 * Finds the previous logical index before the specified index that contains a non-null value.
		 *
		 * @param index The starting logical index (exclusive) for the backward search. The search begins
		 *              at `index - 1`. Pass -1 or a value greater than or equal to the list's size
		 *              to start the search from the end.
		 * @return The logical index of the previous non-null element before `index`, or -1 if no more
		 * non-null elements are found in the list.
		 */
		public int prevValueIndex( int index ) {
			if( !isFlatStrategy ) return nulls.prev1( index );
			
			for( int i = index; -1 < --i; )
				if( values[ i ] != null ) return i;
			return -1;
		}
		
		/**
		 * Finds the next logical index after the specified index that contains a null value.
		 *
		 * @param index The starting logical index (exclusive) for the search. The search begins
		 *              at `index + 1`. Pass -1 to start the search from the beginning.
		 * @return The logical index of the next null element after `index`, or -1 if no more
		 * null elements are found in the list.
		 */
		public int nextNullIndex( int index ) {
			if( !isFlatStrategy ) return nulls.next0( index );
			
			for( int i = index; ++i < size_card; i++ )
				if( values[ i ] == null ) return i;
			return -1;
		}
		
		/**
		 * Finds the previous logical index before the specified index that contains a null value.
		 *
		 * @param index The starting logical index (exclusive) for the backward search. The search begins
		 *              at `index - 1`. Pass -1 or a value greater than or equal to the list's size
		 *              to start the search from the last logical index (`size() - 1`).
		 * @return The logical index of the previous null element before `index`, or -1 if no more
		 * null elements are found in the list.
		 */
		public int prevNullIndex( int index ) {
			if( !isFlatStrategy ) return nulls.prev0( index );
			
			for( int i = index; -1 < --i; )
				if( values[ i ] == null ) return i;
			return -1;
		}
		
		/**
		 * Retrieves the element at the specified logical index.
		 *
		 * @param index The logical index of the element to retrieve.
		 * @return The element at the specified index, or {@code null} if the element at that index is null,
		 * or if the index is out of the list's bounds.
		 */
		public V get( int index ) {
			return index < 0 || index >= size() ?
			       null :
			       isFlatStrategy ?
			       values[ index ] :
			       nulls.get( index ) ?
			       values[ nulls.rank( index ) - 1 ] :
			       null;
		}
		
		/**
		 * Finds the first occurrence of a specified value.
		 *
		 * @param value The value to locate (can be null).
		 * @return The logical index of the first occurrence, or -1 if not found.
		 */
		public int indexOf( V value ) {
			if( value == null ) return nextNullIndex( -1 );
			
			if( isFlatStrategy ) {
				for( int i = 0; i < size_card; i++ ) {
					V v = values[ i ];
					if( v != null && equal_hash_V.equals( v, value ) ) return i;
				}
				return -1;
			}
			
			
			for( int i = 0; i < size_card; i++ )
				if( equal_hash_V.equals( values[ i ], value ) ) return nulls.bit( i + 1 );
			
			return -1;
		}
		
		/**
		 * Checks if the list contains a specified value.
		 *
		 * @param value The value to check for.
		 * @return {@code true} if the value is present in the list, {@code false} otherwise.
		 */
		public boolean contains( V value ) { return indexOf( value ) != -1; }
		
		/**
		 * Finds the last occurrence of a specified value.
		 *
		 * @param value The value to locate (can be null).
		 * @return The logical index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( V value ) {
			if( isFlatStrategy ) {
				for( int i = size_card; -1 < --i; )
					if( equal_hash_V.equals( values[ i ], value ) ) return i;
				return -1;
			}
			
			if( value == null ) return prevNullIndex( size() );
			
			for( int i = size_card; -1 < --i; )
				if( equal_hash_V.equals( values[ i ], value ) ) return nulls.bit( i + 1 );
			return -1;
		}
		
		/**
		 * Computes a hash code based on the list's logical content.
		 * The hash code is derived from the values of all elements, considering nulls.
		 *
		 * @return The hash code for the list.
		 */
		@Override
		public int hashCode() {
			
			int hash = 17;
			if( isFlatStrategy )
				for( int i = 0; i < size_card; i++ )
				     hash = Array.hash( hash, equal_hash_V.hashCode( values[ i ] ) );
			else
				for( int i = 0, s = nulls.size; i < s; i++ )
				     hash = Array.hash( hash, equal_hash_V.hashCode( nulls.get( i ) ?
				                                                     values[ i ] :
				                                                     null ) );
			return Array.finalizeHash( hash, size() );
		}
		
		/**
		 * Compares this list with another object for equality.
		 * Returns {@code true} if the object is an instance of {@code R} (or a subclass)
		 * and contains the same elements in the same order, considering nulls.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the object is an equivalent list instance, {@code false} otherwise.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj );
		}
		
		/**
		 * Compares this list to another read-only list instance for logical equality.
		 * Two lists are considered equal if they have the same logical size and
		 * all corresponding elements are equal (including null-equality).
		 *
		 * @param other The other read-only list instance to compare with.
		 * @return {@code true} if the lists are logically equal, {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) {
			int size;
			
			if( other == this ) return true;
			if( other == null || ( size = size() ) != other.size() ) return false;
			
			for( int i = 0; i < size; i++ ) {
				V value1 = get( i );
				V value2 = other.get( i );
				
				if( value1 != value2 && ( value1 == null || !equal_hash_V.equals( value1, value2 ) ) ) return false;
			}
			return true;
		}
		
		/**
		 * Creates a deep copy of this list.
		 * The returned list will have independent copies of internal data structures.
		 *
		 * @return A new read-only list instance with the same content.
		 * @throws RuntimeException if cloning fails.
		 */
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				dst.nulls  = nulls != null ?
				             nulls.clone() :
				             null;
				dst.values = values.clone();
				return dst;
			} catch( CloneNotSupportedException e ) {
				throw new RuntimeException( "Failed to clone ObjectNullList.R", e );
			}
		}
		
		/**
		 * Returns a JSON string representation of the list.
		 * This method internally calls {@link #toJSON(JsonWriter)} to format the output.
		 *
		 * @return The JSON string representation of the list.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Serializes the list to JSON format.
		 * Elements are written in their logical order. Null elements are represented as JSON 'null'.
		 *
		 * @param json The {@link JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray();
			int size = size();
			if( size > 0 ) {
				json.preallocate( size * 10 );
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
		 * Sets a value at a specific logical index within a target list instance, handling both compressed and flat strategies.
		 * This is a protected static helper method used by read-write implementations.
		 *
		 * @param dst   The target list instance to modify.
		 * @param index The logical index at which to set the value.
		 * @param value The value to set (can be null).
		 */
		protected static < V > void set( R< V > dst, int index, V value ) {
			if( dst.isFlatStrategy ) {
				if( dst.values.length <= index ) {
					dst.values    = dst.equal_hash_V.copyOf( dst.values, Math.max( 16, index * 3 / 2 ) );
					dst.size_card = index + 1; // Total size including nulls
				}
				else if( dst.size_card <= index ) dst.size_card = index + 1;// Extend total size
				
				dst.values[ index ] = value;
			}
			else if( value == null ) {
				if( index <= dst.nulls.last1() && dst.nulls.get( index ) )
					dst.size_card = Array.resize( dst.values, dst.values, dst.nulls.rank( index ) - 1, dst.size_card, -1 );//as cardinality
				dst.nulls.set0( index );
			}
			else if( dst.nulls.get( index ) ) dst.values[ dst.nulls.rank( index ) - 1 ] = value;
			else {
				
				if( dst.flatStrategyThreshold < dst.size_card ) {
					dst.switchToFlatStrategy( Math.max( index + 1, dst.nulls.size() * 3 / 2 ) );
					dst.values[ index ] = value;
				}
				else {
					int rank = dst.nulls.rank( index );
					int max  = Math.max( rank, dst.size_card + 1 );
					
					dst.size_card = Array.resize( dst.values,
					                              dst.values.length == max ?
					                              ( dst.values = dst.equal_hash_V.copyOf( null, Math.max( 16, max * 3 / 2 ) ) ) :
					                              dst.values, rank, dst.size_card, 1 );
					
					dst.values[ rank ] = value;
					dst.nulls.set1( index );
				}
			}
		}
	}
	
	/**
	 * Read-write implementation of {@link ObjectNullList}, extending its read-only counterpart {@code R}
	 * with methods to modify the list's content and structure. This class manages elements
	 * using either a compressed or flat storage strategy to optimize memory or performance.
	 *
	 * @param <V> The type of elements in the list, allowing null values.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs a new list with a specified initial logical capacity, using a default flat strategy threshold of {@code 64}.
		 *
		 * @param clazz  The class of the element type {@code V}.
		 * @param length The initial logical capacity of the list. If {@code 0}, the list is empty.
		 *               If negative, the list is initialized with a logical size of {@code -length} and filled with nulls.
		 */
		public RW( Class< V > clazz, int length ) { this( clazz, length, 64 ); }
		
		/**
		 * Constructs a new list with a specified initial logical capacity and a configurable flat strategy threshold.
		 *
		 * @param clazz                 The class of the element type {@code V}.
		 * @param length                The initial logical capacity for the list. If {@code 0}, the list is empty.
		 *                              If negative, the list is initialized with a logical size of {@code -length} and filled with null elements.
		 * @param flatStrategyThreshold The threshold for switching between compressed and flat storage strategies.
		 *                              A higher value favors the compressed strategy (memory efficiency), while a lower value favors the flat strategy (access speed).
		 * @throws IllegalArgumentException if {@code flatStrategyThreshold} is negative.
		 */
		public RW( Class< V > clazz, int length, int flatStrategyThreshold ) {
			this( Array.get( clazz ), length, flatStrategyThreshold );
		}
		
		/**
		 * Constructs a new list with a specified initial capacity, using a provided equality utility and a default flat strategy threshold of {@code 64}.
		 *
		 * @param equal_hash_V The utility for type-safe operations on elements of type {@code V}.
		 * @param items        The initial logical capacity for the list.
		 *                     If positive, it sets the initial capacity for the internal arrays.
		 *                     If negative, the list is initialized with a logical size of {@code -items}
		 *                     and is pre-filled with null elements up to that size.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int items ) { this( equal_hash_V, items, 64 ); }
		
		/**
		 * Constructs a new list with a specified initial capacity, equality utility, and flat strategy threshold.
		 *
		 * @param equal_hash_V          The utility for type-safe operations on elements of type {@code V}.
		 * @param items                 The initial logical capacity for the list.
		 *                              If positive, it sets the initial capacity for the internal arrays.
		 *                              If negative, the list is initialized with a logical size of {@code -items}
		 *                              and is pre-filled with null elements up to that size.
		 * @param flatStrategyThreshold The threshold for switching between compressed and flat storage strategies.
		 *                              A higher value favors the compressed strategy (memory efficiency), while a lower value favors the flat strategy (access speed).
		 * @throws IllegalArgumentException if {@code flatStrategyThreshold} is negative.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int items, int flatStrategyThreshold ) {
			super( equal_hash_V );
			if( flatStrategyThreshold < 0 ) throw new IllegalArgumentException( "flatStrategyThreshold cannot be negative" );
			this.flatStrategyThreshold = flatStrategyThreshold;
			int length = Math.abs( items );
			
			nulls  = new BitList.RW( length );
			values = length == 0 ?
			         equal_hash_V.OO :
			         equal_hash_V.copyOf( null, length );
			if( items < 0 ) set1( -items - 1, null );// + set size
		}
		
		/**
		 * Sets the threshold for strategy switching and may trigger an immediate strategy switch.
		 * <ul>
		 *     <li>If {@code threshold} is less than or equal to the current cardinality (number of non-null elements)
		 *         and the list is currently using the compressed strategy, it will switch to the flat strategy.</li>
		 *     <li>If {@code threshold} is greater than the current cardinality and the list is currently
		 *         using the flat strategy, it will switch to the compressed strategy.</li>
		 * </ul>
		 * This method allows fine-tuning the balance between memory efficiency and access performance.
		 * Operations like {@code add} or {@code set} may also trigger switches based on resizing needs.
		 *
		 * @param threshold New threshold value (a higher value favors compressed, a lower value favors flat).
		 * @see #switchToFlatStrategy()
		 * @see #switchToCompressedStrategy()
		 */
		public void flatStrategyThreshold( int threshold ) {
			
			if( isFlatStrategy ) {
				if( cardinality() < ( flatStrategyThreshold = threshold ) ) switchToCompressedStrategy();
			}
			else if( ( flatStrategyThreshold = threshold ) <= nulls.cardinality() ) switchToFlatStrategy();
		}
		
		
		/**
		 * Creates a deep copy of this list.
		 * The returned list will be an independent {@code RW} instance with the same content
		 * and internal state (e.g., strategy, thresholds).
		 *
		 * @return A new {@code RW} instance with the same content.
		 * @throws RuntimeException if cloning fails (e.g., if the underlying element type is not cloneable).
		 */
		public RW< V > clone() {
			return ( RW< V > ) super.clone();
		}
		
		/**
		 * Removes the last element from the list.
		 * If the list is empty, no operation is performed.
		 *
		 * @return This instance for method chaining.
		 */
		public RW< V > remove() {
			return remove( size() - 1 );
		}
		
		/**
		 * Removes the element at the specified logical index.
		 * If the index is out of the list's bounds, no operation is performed.
		 *
		 * @param index The logical index of the element to remove.
		 * @return This instance for method chaining.
		 */
		public RW< V > remove( int index ) {
			if( size() < 1 || size() <= index ) return this;
			if( isFlatStrategy )
				size_card = Array.resize( values, values, index, size_card, -1 );
			else {
				if( nulls.get( index ) ) size_card = Array.resize( values, values, nulls.rank( index ) - 1, size_card, -1 );
				nulls.remove( index );
			}
			return this;
		}
		
		/**
		 * Sets the value at the last index.
		 *
		 * @param value The value to set (can be null).
		 * @return This instance for method chaining.
		 */
		public RW< V > set1( V value ) {
			set( this, size() - 1, value );
			return this;
		}
		
		/**
		 * Sets the value at a specific logical index.
		 * If the index is beyond the current logical size, the list will be extended with nulls
		 * up to the specified index before setting the value.
		 *
		 * @param index The logical index to set.
		 * @param value The value to set (can be null).
		 * @return This instance for method chaining.
		 */
		public RW< V > set1( int index, V value ) {
			set( this, index, value );
			return this;
		}
		
		/**
		 * Sets multiple values starting from a specified logical index.
		 * The elements from the provided array are copied into the list.
		 *
		 * @param index  The starting logical index in this list.
		 * @param values The values to set (can include nulls).
		 * @return This instance for method chaining.
		 */
		@SafeVarargs
		public final RW< V > set( int index, V... values ) {
			for( int i = values.length; -1 < --i; ) set( this, index + i, values[ i ] );
			return this;
		}
		
		/**
		 * Sets a range of values from a source array into this list, starting at a specified logical index.
		 *
		 * @param index     The starting logical index in this list where the values will be placed.
		 * @param values    The source array containing the elements to set.
		 * @param src_index The starting index within the source array from which elements will be read.
		 * @param len       The number of elements to copy from the source array.
		 * @return This instance for method chaining.
		 */
		public RW< V > set( int index, V[] values, int src_index, int len ) {
			for( int i = values.length; -1 < --i; ) set( this, index + i, values[ src_index + i ] );
			return this;
		}
		
		/**
		 * Appends a value to the end of the list.
		 * The new element will be placed at the current logical size of the list.
		 *
		 * @param value The value to append (can be null).
		 * @return This instance for method chaining.
		 */
		public RW< V > add1( V value ) {
			set( this, size(), value );
			return this;
		}
		
		/**
		 * Inserts a value at a specific logical index, shifting existing elements at and after that index to the right.
		 * If the index is greater than or equal to the current logical size, the element is appended, and any
		 * intermediate indices are filled with nulls.
		 *
		 * @param index The logical index at which to insert the value.
		 * @param value The value to insert (can be null).
		 * @return This instance for method chaining.
		 */
		public RW< V > add1( int index, V value ) {
			if( size() <= index ) {
				set1( index, value ); // Extend list via set1 for out-of-bounds
				return this;
			}
			
			if( isFlatStrategy ) {
				size_card       = Array.resize( values,
				                                values.length <= size_card ?
				                                equal_hash_V.copyOf( null, Math.max( 16, size_card * 3 / 2 ) ) :
				                                values, index, size_card, 1 );
				values[ index ] = value;
				return this;
			}
			
			if( value == null ) {
				nulls.add( index, false );
				return this;
			}
			
			int i   = nulls.rank( index ) - 1;
			int max = Math.max( i + 2, size_card + 1 ); // Ensure capacity
			if( values.length <= max && flatStrategyThreshold <= size_card ) {
				switchToFlatStrategy( max );
				nulls.set1( index );
				size_card++;
				values[ index ] = value;
			}
			else {
				size_card = Array.resize( values,
				                          values.length <= max ?
				                          equal_hash_V.copyOf( null, max == 0 ?
				                                                     16 :
				                                                     max * 3 / 2 ) :
				                          values, i, size_card, 1 );
				nulls.add( index, true );
				values[ i ] = value;
			}
			
			return this;
		}
		
		/**
		 * Appends multiple values to the end of the list.
		 * Each value from the provided array is added sequentially.
		 *
		 * @param items The values to append (can include nulls).
		 * @return This instance for method chaining.
		 */
		@SafeVarargs
		public final RW< V > add( V... items ) {
			return items == null ?
			       this :
			       set( size(), items );
		}
		
		/**
		 * Appends all elements from another read-only list instance to this list.
		 * The elements are appended in their original order.
		 *
		 * @param src The source list to append.
		 * @return This instance for method chaining.
		 */
		public RW< V > addAll( R< V > src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
			     add1( src.get( i ) );
			return this;
		}
		
		/**
		 * Clears all elements from the list, making it empty.
		 * The internal storage strategy (compressed or flat) remains the same.
		 * The underlying values array is filled with nulls, and the logical size is set to zero.
		 *
		 * @return This instance for method chaining.
		 */
		public RW< V > clear() {
			if( size() < 1 ) return this;
			Array.fill( values, 0, size_card, null );
			
			size_card = 0;
			if( !isFlatStrategy ) nulls.clear();
			return this;
		}
		
		/**
		 * Sets the physical capacity of the underlying storage, truncating or expanding as needed.
		 * This affects the allocated memory but does not change the logical size of the list directly.
		 * If the new capacity is less than the current logical size, the list will be truncated to the new capacity,
		 * effectively removing elements beyond that point.
		 *
		 * @param length The new desired physical capacity.
		 * @return This instance for method chaining.
		 * @throws IllegalArgumentException if {@code length} is negative.
		 */
		public RW< V > length( int length ) {
			if( length < 0 ) throw new IllegalArgumentException( "length cannot be negative" );
			
			if( length == 0 ) {
				size_card = 0;
				values    = equal_hash_V.copyOf( null, 0 );
				if( !isFlatStrategy ) nulls.length( 0 );
				return this;
			}
			
			if( isFlatStrategy ) {
				if( values.length != length ) values = equal_hash_V.copyOf( values, length );
				size_card = Math.min( size_card, length );
			}
			else {
				nulls.length( length );
				size_card = nulls.cardinality();
			}
			return this;
		}
		
		/**
		 * Sets the logical size of the list.
		 * <ul>
		 *     <li>If the new size is less than the current size, the list is truncated, and elements beyond
		 *         the new size are removed.</li>
		 *     <li>If the new size is greater than the current size, the list is extended with null elements
		 *         up to the new size.</li>
		 *     <li>If the new size is {@code 0} or less, the list is cleared.</li>
		 * </ul>
		 * This operation may also trigger a strategy switch if the cardinality threshold is met.
		 *
		 * @param size The new desired logical size for the list.
		 * @return This instance for method chaining.
		 */
		public RW< V > size( int size ) {
			if( size < 1 ) clear();
			if( size() < size ) set1( size - 1, null );
			else {
				nulls.size( size );
				if( !isFlatStrategy ) size_card = nulls.cardinality();
			}
			if( !isFlatStrategy && flatStrategyThreshold <= size_card ) switchToFlatStrategy();
			
			return this;
		}
		
		/**
		 * Trims the list's capacity to match its logical size, potentially switching to the compressed strategy.
		 * This reduces the memory footprint by reallocating the internal array to exactly fit the current elements.
		 * If currently in flat strategy and the cardinality (number of non-null elements) falls below
		 * the {@code flatStrategyThreshold}, it will switch to the compressed strategy.
		 *
		 * @return This instance for method chaining.
		 */
		public RW< V > fit() {
			length( size() );
			
			if( !isFlatStrategy ) return this;
			int i = 0;
			for( V v : values )
				if( v != null ) i++;
			if( flatStrategyThreshold < i ) return this;
			
			switchToCompressedStrategy();
			values = equal_hash_V.copyOf( values, i );
			return this;
		}
		
		/**
		 * Swaps the elements located at the specified logical indices within the list.
		 * If the indices are the same, no operation is performed. If the elements at the indices
		 * are identical (considering nulls and object equality), no modification occurs.
		 *
		 * @param index1 The first logical index of an element to swap.
		 * @param index2 The second logical index of an element to swap.
		 * @return This instance for method chaining.
		 * @throws IndexOutOfBoundsException if either {@code index1} or {@code index2} is negative
		 *                                   or greater than or equal to the current logical size of the list.
		 */
		public RW< V > swap( int index1, int index2 ) {
			if( index1 < 0 || index1 >= size() ) throw new IndexOutOfBoundsException( "Index1 must be non-negative and less than the list's size: " + index1 );
			if( index2 < 0 || index2 >= size() ) throw new IndexOutOfBoundsException( "Index2 must be non-negative and less than the list's size: " + index2 );
			if( index1 == index2 ) return this;
			V value1 = get( index1 );
			V value2 = get( index2 );
			if( value1 == value2 ) return this;
			
			set( index1, value2 );
			set( index2, value1 );
			
			return this;
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Provides a type-safe {@link Array.EqualHashOf} instance for {@code RW} lists.
	 * This utility can be used for operations like comparing or hashing {@code RW} instances themselves.
	 *
	 * @param <V> The element type of the list.
	 * @return A type-safe {@link Array.EqualHashOf} for {@code RW<V>} instances.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() {
		return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT;
	}
}