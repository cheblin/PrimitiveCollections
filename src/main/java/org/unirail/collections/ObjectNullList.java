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

import java.util.Arrays;

/**
 * Defines a contract for a list that efficiently manages nullable objects of type {@code V}.
 * Unlike a standard {@link ObjectList}, which uses a single array for all elements including nulls,
 * this interface introduces two strategies to optimize for different use cases:
 * <ul>
 *     <li><b>Compressed Strategy</b>: Saves memory by storing only non-null values and tracking nullity separately.</li>
 *     <li><b>Flat Strategy</b>: Prioritizes fast access by storing all elements in one array, including nulls.</li>
 * </ul>
 * Implementations dynamically switch between these strategies based on null density and a configurable threshold,
 * offering a balance between memory efficiency and performance tailored to the application's needs.
 *
 * @param <V> The type of elements stored in the list, which can be any object (including null).
 */
public interface ObjectNullList< V > {
	
	/**
	 * Abstract base class providing a read-only foundation for {@link ObjectNullList}.
	 * It implements two distinct storage strategies to handle nullable objects:
	 * <p>
	 * <b>Compressed (Rank-Based) Strategy</b>:
	 * - Ideal for lists with many nulls, minimizing memory usage.
	 * - Stores only non-null values in a compact {@code values} array.
	 * - Uses a {@link BitList.RW} ({@code nulls}) to track which indices are non-null ({@code true}) or null ({@code false}).
	 * - Access involves a rank calculation in {@code nulls}, trading some performance for significant memory savings.
	 * <p>
	 * <b>Flat (One-to-One) Strategy</b>:
	 * - Suited for lists with few nulls or where access speed is critical.
	 * - Stores all elements (nulls and non-nulls) in a single {@code values} array, matching logical indices directly.
	 * - Discards {@code nulls}, relying on {@code null} entries in {@code values} to indicate nullity, eliminating rank overhead.
	 * <p>
	 * The strategy switch is controlled by {@code flatStrategyThreshold}, which evaluates null density against bitlist usage,
	 * ensuring optimal performance and memory usage based on the list's current state.
	 *
	 * @param <V> The type of elements in this list, allowing null values.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		/**
		 * Tracks nullity in compressed strategy using a {@link BitList.RW}.
		 * Each bit corresponds to a logical index: {@code true} for non-null, {@code false} for null.
		 * In flat strategy, this is set to {@code null} since nullity is managed directly in {@code values}.
		 */
		protected BitList.RW nulls;
		
		/**
		 * Holds the list's elements in an array of type {@code V}.
		 * In compressed strategy, contains only non-null values; in flat strategy, contains all elements including nulls.
		 */
		protected V[] values;
		
		/**
		 * Utility for equality checks, hashing, and array creation for type {@code V}.
		 * Ensures type-safe operations and consistent behavior across the list.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		/**
		 * Tracks list metrics: non-null count (cardinality) in compressed strategy, total size (including nulls) in flat strategy.
		 * Updated during operations to maintain accurate state.
		 */
		protected int size_card = 0;
		
		/**
		 * Threshold for switching from compressed to flat strategy, defaulting to 1024.
		 * Compared against {@code nulls.used * 64} to determine when to switch, balancing memory and performance.
		 */
		protected int flatStrategyThreshold = 1024;
		
		/**
		 * Indicates the active storage strategy: {@code true} for flat, {@code false} for compressed.
		 */
		protected boolean isFlatStrategy = false;
		
		/**
		 * Constructs a read-only instance with a specified class for type {@code V}.
		 *
		 * @param clazzV The class of the element type {@code V}, used to initialize {@code equal_hash_V}.
		 */
		protected R( Class< V > clazzV ) {
			this.equal_hash_V = Array.get( clazzV );
			this.values       = equal_hash_V.OO; // Empty array for initial efficiency
		}
		
		/**
		 * Constructs a read-only instance with a pre-provided {@link Array.EqualHashOf}.
		 *
		 * @param equal_hash_V The utility for comparing and creating arrays of type {@code V}.
		 */
		public R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
			this.values       = equal_hash_V.OO;
		}
		
		/**
		 * Switches to flat strategy, reallocating {@code values} to include all elements with nulls in place.
		 */
		protected void switchToFlatStrategy() {
			if( size() == 0 )//the collection is empty
			{
				if( values.length == 0 ) values = equal_hash_V.copyOf( null, 16 );
				isFlatStrategy = true;
				return;
			}
			V[] compressed = values;
			values = equal_hash_V.copyOf( null, Math.max( 16, nulls.last1() + 1 ) );
			for( int i = nulls.next1( 0 ), ii = 0; ii < size_card; i = nulls.next1( i + 1 ) )
			     values[ i ] = compressed[ ii++ ];
			
			size_card      = nulls.size();
			nulls          = null;
			isFlatStrategy = true;
		}
		
		/**
		 * Switches to compressed strategy, compacting non-null values and rebuilding {@code nulls}.
		 * Optimized to count and populate in one pass for efficiency.
		 */
		protected void switchToCompressedStrategy() {
			nulls = new BitList.RW( size_card );
			for( int i = 0, ii = 0; i < size_card; i++ )
				if( values[ i ] != null ) { // packing
					nulls.set1( i );
					values[ ii++ ] = values[ i ];
				}
			isFlatStrategy = false;
		}
		
		/**
		 * Copies a range of elements into a destination array, preserving nulls.
		 *
		 * @param index Starting logical index.
		 * @param len   Number of elements to copy.
		 * @param dst   Destination array; resized if null or too small.
		 * @return Array with copied elements, or {@code null} if empty.
		 */
		public V[] toArray( int index, int len, V[] dst ) {
			if( size() == 0 ) return dst;
			// Ensure valid range.
			index = Math.max( 0, index );
			len   = Math.min( len, size() - index );
			if( len <= 0 ) return dst;
			
			if( dst == null || dst.length < len ) dst = equal_hash_V.copyOf( null, len );
			
			if( isFlatStrategy ) System.arraycopy( values, index, dst, 0, Math.min( len, size_card - index ) );
			else for( int i = 0, srcIndex = index; i < len && srcIndex < size(); i++, srcIndex++ )
			          dst[ i ] = hasValue( srcIndex ) ?
					          values[ nulls.rank( srcIndex ) - 1 ] :
					          null;
			return dst;
		}
		
		/**
		 * Checks if this list contains all non-null elements from another {@link ObjectList.R}.
		 *
		 * @param src Source list to check against.
		 * @return {@code true} if all non-null elements are present, {@code false} otherwise.
		 */
		public boolean containsAll( ObjectNullList.R< V > src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				if( indexOf( src.get( i ) ) == -1 ) return false;
			return true;
		}
		
		/**
		 * Returns the physical capacity of the internal {@code values} array.
		 *
		 * @return The length of the {@code values} array.
		 */
		public int length() {
			return values.length;
		}
		
		/**
		 * Returns the logical size of the list (total elements, including nulls).
		 *
		 * @return The logical size of the list.
		 */
		public int size() {
			return isFlatStrategy ?
					size_card :
					nulls.size();
		}
		
		/**
		 * Checks if the list is logically empty.
		 *
		 * @return {@code true} if the list has no elements, {@code false} otherwise.
		 */
		public boolean isEmpty() {
			return size() < 1;
		}
		
		/**
		 * Checks if a non-null value exists at the specified index.
		 *
		 * @param index The index to check.
		 * @return {@code true} if the value is non-null, {@code false} if null or out of bounds.
		 */
		public boolean hasValue( int index ) {
			return index >= 0 && index < size() && (
					isFlatStrategy ?
							values[ index ] != null :
							nulls.get( index ) );
		}
		
		/**
		 * Finds the next index with a non-null value, starting from {@code index} (inclusive).
		 *
		 * @param index The starting point for the search.
		 * @return The next non-null index, or -1 if none found.
		 */
		public int nextValueIndex( int index ) {
			if( !isFlatStrategy ) { return nulls.next1( index ); }
			
			for( int i = index; i < size_card; i++ )
				if( values[ i ] != null ) return i;
			return -1;
		}
		
		/**
		 * Finds the previous index with a non-null value, before {@code index} (exclusive).
		 *
		 * @param index The starting point for the backward search.
		 * @return The previous non-null index, or -1 if none found.
		 */
		public int prevValueIndex( int index ) {
			if( !isFlatStrategy ) { return nulls.prev1( index ); }
			
			for( int i = index - 1; i >= 0; i-- )
				if( values[ i ] != null ) return i;
			return -1;
		}
		
		/**
		 * Finds the next index with a null value, starting from {@code index} (inclusive).
		 *
		 * @param index The starting point for the search.
		 * @return The next null index, or -1 if none found.
		 */
		public int nextNullIndex( int index ) {
			if( !isFlatStrategy ) { return nulls.next0( index ); }
			
			for( int i = index; i < size_card; i++ )
				if( values[ i ] == null ) return i;
			return -1;
		}
		
		/**
		 * Finds the previous index with a null value, before {@code index} (exclusive).
		 *
		 * @param index The starting point for the backward search.
		 * @return The previous null index, or -1 if none found.
		 */
		public int prevNullIndex( int index ) {
			if( !isFlatStrategy ) { return nulls.prev0( index ); }
			
			for( int i = index - 1; i >= 0; i-- )
				if( values[ i ] == null ) return i;
			return -1;
		}
		
		/**
		 * Retrieves the value at the specified index, which may be null.
		 *
		 * @param index The index to retrieve from.
		 * @return The value at {@code index}, or {@code null} if the index is null or out of bounds.
		 */
		public V get( int index ) {
			if( index < 0 || index >= size() ) return null;
			return isFlatStrategy ?
					values[ index ] :
					( nulls.get( index ) ?
							values[ nulls.rank( index ) - 1 ] :
							null );
		}
		
		/**
		 * Finds the first occurrence of a specified non-null value.
		 *
		 * @param value The value to locate (null returns -1).
		 * @return The index of the first occurrence, or -1 if not found.
		 */
		public int indexOf( V value ) {
			if( value == null ) return nextNullIndex( 0 );
			
			if( isFlatStrategy ) {
				for( int i = 0; i < size_card; i++ )
					if( equal_hash_V.equals( values[ i ], value ) ) return i;
				return -1;
			}
			
			int i = Array.indexOf( values, value, 0, size_card );
			return i < 0 ?
					-1 :
					nulls.bit( i );
		}
		
		/**
		 * Checks if the list contains a specified value.
		 *
		 * @param value The value to check for.
		 * @return {@code true} if the value exists, {@code false} otherwise.
		 */
		public boolean contains( V value ) { return indexOf( value ) != -1; }
		
		/**
		 * Finds the last occurrence of a specified value.
		 *
		 * @param value The value to locate (null returns the last null index).
		 * @return The index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( V value ) {
			if( isFlatStrategy ) {
				for( int i = size_card - 1; 0 <= i; i-- )
					if( equal_hash_V.equals( values[ i ], value ) ) return i;
				return -1;
			}
			if( value == null ) return prevNullIndex( size() );
			for( int i = nulls.prev1( size() - 1 ); i != -1; i = nulls.prev1( i ) )
				if( equal_hash_V.equals( values[ i ], value ) ) return i;
			return -1;
		}
		
		
		/**
		 * Computes a hash code based on the list's logical content.
		 *
		 * @return The hash code for the list.
		 */
		@Override
		public int hashCode() {
			return Array.finalizeHash( Array.hash( Array.hash( nulls ), values, 0, isFlatStrategy ?
					size() :
					size_card ), size() );
		}
		
		/**
		 * Compares this list with another object for equality.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if equal and of the same type, {@code false} otherwise.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj );
		}
		
		/**
		 * Compares this list to another {@code R} instance for logical equality.
		 *
		 * @param other The other {@code R} instance to compare with.
		 * @return {@code true} if logically equal, {@code false} otherwise.
		 */
		
		public boolean equals( R< V > other ) {
			int size;
			
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
		 *
		 * @return A new {@code R} instance identical to this one.
		 * @throws RuntimeException if cloning fails unexpectedly.
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
		 *
		 * @return The JSON representation.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Serializes the list to JSON format.
		 *
		 * @param json The {@link JsonWriter} to output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray();
			int size = size();
			if( size > 0 ) {
				json.preallocate( size * 10 );
				for( int i = 0, ii; i < size; )
					if( ( ii = nextValueIndex( i ) ) == i ) json.value( get( i++ ) );
					else if( ii == -1 || size <= ii ) {
						while( i++ < size ) json.value();
						break;
					}
					else for( ; i < ii; i++ ) json.value();
			}
			json.exitArray();
		}
		
		/**
		 * Sets a value at a specific index, handling both strategies.
		 *
		 * @param dst   The target {@code R} instance to modify.
		 * @param index The index to set.
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
				if( dst.nulls.last1() < index ) dst.nulls.size( index + 1 );//extend nulls explicitly
				else if( dst.nulls.get( index ) ) dst.size_card = Array.resize( dst.values, dst.values, dst.nulls.rank( index ) - 1, dst.size_card, -1 );//as cardinality
				dst.nulls.set0( index );
			}
			else if( dst.nulls.get( index ) ) dst.values[ dst.nulls.rank( index ) - 1 ] = value;
			else {
				int rank = dst.nulls.rank( index );
				
				if( dst.values.length <= rank && dst.flatStrategyThreshold < dst.nulls.used * 64 ) {
					dst.nulls.set1( index ); // before call switchToFlatStrategy() must be applied
					dst.switchToFlatStrategy();
					dst.values[ index ] = value;
				}
				else {
					try {
						dst.size_card = Array.resize( dst.values,
						                              dst.values.length == rank ?
								                              ( dst.values = dst.equal_hash_V.copyOf( null, Math.max( 16, rank * 3 / 2 ) ) ) :
								                              dst.values, rank, dst.size_card, 1 );
					} catch( Exception e ) {
						throw new RuntimeException( e );
					}
					dst.values[ rank ] = value;
					dst.nulls.set1( index );
				}
			}
		}
	}
	
	/**
	 * Read-write implementation of {@link ObjectNullList}, extending {@code R} with modification capabilities.
	 *
	 * @param <V> The type of elements in this list, allowing nulls.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs an empty list with a specified initial capacity.
		 *
		 * @param clazz  The class of type {@code V}.
		 * @param length The initial capacity of the list.
		 */
		public RW( Class< V > clazz, int length ) {
			this( Array.get( clazz ), length );
		}
		
		/**
		 * Constructs an empty list with a specified initial capacity and equality utility.
		 *
		 * @param equal_hash_V The utility for type {@code V} operations.
		 * @param length       The initial capacity of the list.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int length ) {
			super( equal_hash_V );
			nulls  = new BitList.RW( length );
			values = length > 0 ?
					equal_hash_V.copyOf( null, Math.max( 16, length ) ) :
					equal_hash_V.OO;
		}
		
		/**
		 * Constructs a list with a default value and initial size.
		 *
		 * @param clazz         The class of type {@code V}.
		 * @param default_value The default value for initialization (can be null).
		 * @param size          The initial logical size (negative means capacity only).
		 */
		public RW( Class< V > clazz, V default_value, int size ) {
			this( Array.get( clazz ), default_value, size );
		}
		
		/**
		 * Constructs a list with a default value, initial size, and equality utility.
		 *
		 * @param equal_hash_V  The utility for type {@code V}.
		 * @param default_value The default value (can be null).
		 * @param size          The initial logical size (negative for capacity only).
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, V default_value, int size ) {
			super( equal_hash_V );
			nulls     = new BitList.RW( default_value != null, size );
			size_card = default_value == null ?
					0 :
					size;
			values    = size == 0 ?
					equal_hash_V.OO :
					equal_hash_V.copyOf( null,
					                     Math.max( 16, size < 0 ?
							                     -size :
							                     size ) );
			if( 0 < size && default_value != null )
				Arrays.fill( values, 0, size, default_value ); // Fill with default if specified.
		}
		
		/**
		 * Sets the threshold for switching to flat strategy.
		 *
		 * @param interleavedBits The new threshold value (higher delays switch, lower accelerates it).
		 */
		public void flatStrategyThreshold( int interleavedBits ) {
			flatStrategyThreshold = interleavedBits;
		}
		
		/**
		 * Creates a deep copy of this list.
		 *
		 * @return A new {@code RW} instance identical to this one.
		 */
		public RW< V > clone() {
			return ( RW< V > ) super.clone();
		}
		
		/**
		 * Removes the last element from the list.
		 *
		 * @return This instance for chaining.
		 */
		public RW< V > remove() {
			return remove( size() - 1 );
		}
		
		/**
		 * Removes the element at the specified index.
		 *
		 * @param index The index to remove.
		 * @return This instance for chaining.
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
		 * Sets a value at the last index.
		 *
		 * @param value The value to set.
		 * @return This instance for chaining.
		 */
		public RW< V > set1( V value ) {
			set( this, size() - 1, value );
			return this;
		}
		
		/**
		 * Sets a value at a specific index.
		 *
		 * @param index The index to set.
		 * @param value The value to set.
		 * @return This instance for chaining.
		 */
		public RW< V > set1( int index, V value ) {
			set( this, index, value );
			return this;
		}
		
		/**
		 * Sets multiple values starting from an index.
		 *
		 * @param index  The starting index.
		 * @param values The values to set.
		 * @return This instance for chaining.
		 */
		@SafeVarargs
		public final RW< V > set( int index, V... values ) {
			for( int i = 0; i < values.length; i++ ) set( this, index + i, values[ i ] );
			return this;
		}
		
		/**
		 * Sets a range of values from an array.
		 *
		 * @param index     The starting index.
		 * @param values    The source array.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to copy.
		 * @return This instance for chaining.
		 */
		public RW< V > set( int index, V[] values, int src_index, int len ) {
			for( int i = 0; i < len; i++ ) set( this, index + i, values[ src_index + i ] );
			return this;
		}
		
		/**
		 * Adds a value to the end of the list.
		 *
		 * @param value The value to append.
		 * @return This instance for chaining.
		 */
		public RW< V > add1( V value ) {
			set( this, size(), value );
			return this;
		}
		
		/**
		 * Inserts a value at a specific index.
		 *
		 * @param index The insertion point.
		 * @param value The value to insert.
		 * @return This instance for chaining.
		 */
		public RW< V > add1( int index, V value ) {
			if( isFlatStrategy )
				if( index < size_card ) {
					// Insert within bounds: shift elements right, update total size
					size_card       = Array.resize( values,
					                                values.length <= size_card ?
							                                equal_hash_V.copyOf( null, Math.max( 16, size_card * 3 / 2 ) ) :
							                                values, index, size_card, 1 );
					values[ index ] = value;
				}
				else set1( index, value ); // Extend list if index is beyond current size
			else if( index < size() ) {
				// Insert into bitlist: true for non-null, false for null
				nulls.add( index, value != null );
				if( value != null ) {
					int i   = nulls.rank( index ) - 1; // Position in values array
					int max = Math.max( i + 1, size_card + 1 ); // Ensure capacity
					if( values.length <= max && flatStrategyThreshold <= nulls.used * 64 ) {
						// Switch to flat if threshold exceeded
						switchToFlatStrategy();
						values[ index ] = value;
					}
					else {
						// Insert non-null value, update non-null count
						size_card   = Array.resize( values,
						                            values.length <= max ?
								                            equal_hash_V.copyOf( null, Math.max( 16, max * 3 / 2 ) ) :
								                            values, i, size_card, 1 );
						values[ i ] = value;
					}
				}
				// No size_card adjustment for null: it tracks non-null count only
			}
			else set1( index, value ); // Extend list via set1 for out-of-bounds
			return this;
		}
		
		/**
		 * Adds multiple values to the end of the list.
		 *
		 * @param items The values to add.
		 * @return This instance for chaining.
		 */
		@SafeVarargs
		public final RW< V > add( V... items ) {
			return set( size(), items );
		}
		
		/**
		 * Adds all elements from another {@code R} instance.
		 *
		 * @param src The source list.
		 * @return This instance for chaining.
		 */
		public RW< V > addAll( R< V > src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
			     add1( src.get( i ) );
			return this;
		}
		
		/**
		 * Clears the list, reverting to compressed strategy.
		 *
		 * @return This instance for chaining.
		 */
		public RW< V > clear() {
			Array.fill( values, 0, size_card, null );
			
			isFlatStrategy = false;
			size_card      = 0;
			if( nulls == null ) nulls = new BitList.RW( 0 );
			else nulls.clear();
			return this;
		}
		
		/**
		 * Sets the physical capacity of the list.
		 *
		 * @param length The new capacity.
		 * @return This instance for chaining.
		 */
		public RW< V > length( int length ) {
			if( length < 1 ) return clear();
			
			if( isFlatStrategy ) {
				if( values.length != length ) values = equal_hash_V.copyOf( values, length );
				size_card = Math.min( size_card, length );
			}
			else {
				nulls.length( length );
				if( length < size_card ) size_card = nulls.cardinality();
			}
			return this;
		}
		
		/**
		 * Sets the logical size, extending or truncating as needed.
		 *
		 * @param size The new logical size.
		 * @return This instance for chaining.
		 */
		public RW< V > size( int size ) {
			if( size < 1 ) clear();
			if( size() < size ) set1( size - 1, null );
			else {
				nulls.size( size );
				if( !isFlatStrategy ) size_card = nulls.cardinality();
			}
			if( !isFlatStrategy && flatStrategyThreshold <= nulls.used * 64 ) switchToFlatStrategy();
			
			return this;
		}
		
		/**
		 * Trims the list’s capacity to its logical size.
		 *
		 * @return This instance for chaining.
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
		 * Swaps elements at two indices.
		 *
		 * @param index1 First index to swap.
		 * @param index2 Second index to swap.
		 * @return This instance for chaining.
		 */
		public RW< V > swap( int index1, int index2 ) {
			if( index1 == index2 || index1 < 0 || index2 < 0 || index1 >= size() || index2 >= size() ) return this;
			if( isFlatStrategy ) {
				V tmp = values[ index1 ];
				values[ index1 ] = values[ index2 ];
				values[ index2 ] = tmp;
				return this;
			}
			
			boolean e1 = nulls.get( index1 ); // True if index1 has a non-null value
			boolean e2 = nulls.get( index2 ); // True if index2 has a non-null value
			
			if( !e1 && !e2 ) return this; // Both indices are null, no change needed
			
			int i1 = nulls.rank( index1 ) - 1; // Rank in values array for index1
			int i2 = nulls.rank( index2 ) - 1; // Rank in values array for index2
			
			if( e1 && e2 ) { // Both indices have non-null values
				
				V tmp = values[ i1 ];
				values[ i1 ] = values[ i2 ];
				values[ i2 ] = tmp;
				return this;
			}
			
			
			int exist = e1 ?
					i1 :
					i2;
			
			int empty = e1 ?
					i2 + 1 :
					i1 + 1;
			
			
			V v = values[ exist ];// Store the non-null value to move
			Array.resize( values, values, exist, size_card, -1 );// Remove the value from its old position
			Array.resize( values, values,
			              exist < empty ?
					              empty - 1 :
					              empty, size_card - 1, 1 );//expand empty
			
			values[ empty ] = v;
			
			return this;
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Returns a type-safe {@link Array.EqualHashOf} for {@code RW} instances.
	 *
	 * @param <V> The element type of the list.
	 * @return An {@link Array.EqualHashOf} instance for {@code RW<V>}.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() {
		return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT;
	}
}