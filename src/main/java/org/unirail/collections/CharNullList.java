// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
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
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.Arrays;

/**
 * {@code IntNullList} defines a contract for a list that stores primitive integer values,
 * allowing elements to represent a {@code null} state.
 * <p>
 * Implementations manage lists of primitive integers where each position can either hold an
 * integer value or be considered {@code null}. This interface ensures efficient methods
 * for adding, removing, and accessing elements while tracking the nullity status of each
 * element. Implementations use optimized storage strategies to balance memory usage and
 * access performance.
 */
public interface CharNullList {
	
	/**
     * {@code R} is an abstract base class providing a read-only view of an {@link IntNullList}.
     * <p>
     * It uses a {@link BitList.RW} to track nullity (where {@code true} indicates a non-null
     * value and {@code false} indicates a null value) and a primitive integer array to store
     * values. Two storage strategies optimize performance based on null density:
     * <ul>
     *     <li><b>Compressed (Rank-Based) Strategy:</b> Space-efficient for sparse lists with many nulls.
     *         Stores only non-null values contiguously in the {@code values} array. Access uses
     *         rank calculations via {@code nulls.rank(i)}.</li>
     *     <li><b>Flat (One-to-One) Strategy:</b> Optimized for dense lists with few nulls. The
     *         {@code values} array mirrors the logical list, with direct indexing after nullity checks.</li>
     * </ul>
     * The writable subclass {@link RW} dynamically switches strategies based on a threshold.
     */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
         * Tracks nullity of elements. A bit set to {@code true} at index {@code i} indicates a
         * non-null value; {@code false} indicates null.
		 */
		protected BitList.RW nulls;
		
		/**
         * Stores primitive integer values.
         * <ul>
         *     <li>In <b>Compressed Strategy</b>, contains only non-null values; size equals {@code cardinality}.</li>
         *     <li>In <b>Flat Strategy</b>, mirrors logical list; size matches {@code nulls.size()}.</li>
         * </ul>
		 */
		protected char[] values;
		
		/**
         * Default integer value used in some operations (e.g., initialization). Distinct from
         * logical nullity, which is tracked by {@code nulls}.
		 */
		public final char default_value;
		
		/**
         * Number of non-null elements. Used for indexing in <b>Compressed Strategy</b> and consistency
         * in <b>Flat Strategy</b>.
		 */
		protected int cardinality = 0;
		
		/**
         * Threshold for switching between storage strategies in {@link RW}. Compared against
         * {@code cardinality} or internal metrics to balance space and speed. Default is 1024.
		 *
		 * @see RW#flatStrategyThreshold(int)
		 */
		protected int flatStrategyThreshold = 1024;
		
		
		/**
         * Indicates current storage strategy: {@code true} for <b>Flat</b>, {@code false} for <b>Compressed</b>.
		 */
		protected boolean isFlatStrategy = false;
		
		/**
         * Constructs a read-only base list with a default value.
		 *
         * @param default_value Default integer value for the list.
		 */
		protected R( char default_value ) {
			this.default_value = default_value;
			this.values        = Array.EqualHashOf.chars     .O; // Initialize with an empty array.
		}
		
		
		/**
         * Returns the physical capacity of the {@code values} array, not the logical size.
         * <ul>
         *     <li>In <b>Flat Strategy</b>, typically matches logical size.</li>
         *     <li>In <b>Compressed Strategy</b>, reflects capacity for non-null elements.</li>
         * </ul>
		 *
         * @return Length of the {@code values} array.
         * @see #size()
		 */
		public int length() { return values.length; }
		
		
		/**
         * Returns the logical size of the list, including both null and non-null elements.
		 *
         * @return Total number of logical elements.
		 */
		public int size() { return nulls.size(); }
		
		/**
         * Checks if the list is logically empty.
		 *
         * @return {@code true} if size is 0, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() < 1; }
		
		/**
         * Checks if the element at the specified index is non-null.
		 *
         * @param index Logical index to check.
         * @return {@code true} if non-null, {@code false} if null or out of bounds.
		 */
		public boolean hasValue( int index ) {
			return nulls.get( index );
		}
		
		/**
         * Finds the next non-null value's index at or after the given index.
		 *
         * @param index Starting index (inclusive).
         * @return Index of next non-null value, or -1 if none found.
		 */
		public @Positive_OK int nextValueIndex( int index ) { return nulls.next1( index ); }
		
		/**
         * Finds the previous non-null value's index at or before the given index.
		 *
         * @param index Starting index (inclusive).
         * @return Index of previous non-null value, or -1 if none found.
		 */
		public @Positive_OK int prevValueIndex( int index ) { return nulls.prev1( Math.min( index, size() - 1 ) ); }
		
		/**
         * Finds the next null value's index at or after the given index.
		 *
         * @param index Starting index (inclusive).
         * @return Index of next null value, or -1 if none found.
		 */
		public @Positive_OK int nextNullIndex( int index ) { return nulls.next0( index ); }
		
		/**
         * Finds the previous null value's index at or before the given index.
		 *
         * @param index Starting index (inclusive).
         * @return Index of previous null value, or -1 if none found.
		 */
		public @Positive_OK int prevNullIndex( int index ) { return nulls.prev0( Math.min( index, size() - 1 ) ); }
		
		/**
         * Retrieves the integer value at the specified index. Caller must verify non-nullity
         * with {@link #hasValue(int)} first, or behavior is undefined (e.g., may throw exception
         * or return garbage).
		 *
         * @param index Logical index of a non-null value.
         * @return Integer value at the index.
         * @throws IndexOutOfBoundsException If index is invalid or null.
		 */
		public char get( @Positive_ONLY int index ) {
			return (char)( isFlatStrategy ?
					values[ index ] :
					values[ nulls.rank( index ) - 1 ] ); // Rank-based access in compressed strategy.
		}
		
		/**
         * Returns the first index of a non-null integer value. Does not search for nulls; use
         * {@link #nextNullIndex(int)} for nulls.
		 *
         * @param value Non-null integer to find.
         * @return First index of the value, or -1 if not found.
		 */
		public int indexOf( char value ) {
			if( isFlatStrategy ) {
				for( int i = nulls.next1( 0 ); i != -1; i = nulls.next1( i + 1 ) )
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
         * Checks if the list contains a non-null integer value. For null checks, use
         * {@link #contains(Integer)}.
		 *
         * @param value Non-null integer to check.
         * @return {@code true} if found, {@code false} otherwise.
		 */
		public boolean contains( char value ) { return indexOf( value ) != -1; }
		
		/**
         * Checks if the list contains a boxed value, which may be {@code null}.
		 *
         * @param value Boxed integer (or {@code null}) to check.
         * @return {@code true} if value (or a null) is present, {@code false} otherwise.
		 */
		public boolean contains(  Character value ) {
			int i;
			return value == null ?
					nextNullIndex( 0 ) != -1 :
					// Check for null value.
					indexOf( value ) != -1;     // Check for non-null value.
		}
		
		/**
         * Returns the last index of a non-null integer value. Does not search for nulls.
		 *
         * @param value Non-null integer to find.
         * @return Last index of the value, or -1 if not found.
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
         * Compares this list to another object for equality based on logical content.
		 *
         * @param obj Object to compare.
         * @return {@code true} if equal, {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) );
		}
		
		/**
         * Computes a hash code based on logical content, independent of storage strategy.
		 *
         * @return Hash code of the list.
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
         * Checks logical equality with another {@code R} instance.
		 *
         * @param other List to compare.
         * @return {@code true} if logically equal, {@code false} otherwise.
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
			return null; // Return null if cloning fails.
		}
		
		/**
         * Returns a JSON string of the list, with nulls as {@code null}.
		 *
         * @return JSON representation, e.g., {@code [1, null, 3]}.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
         * Writes the list to a JSON writer, representing nulls as {@code null}.
		 *
         * @param json Writer to output JSON.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray(); // Start JSON array.
			int size = size();
			if( 0 < size ) {
				json.preallocate( size * 10 ); // Pre-allocate buffer to improve writing performance, assuming ~10 bytes per element in JSON.
				for( int i = 0, ii; i < size; )
					if( ( ii = nextValueIndex( i ) ) == i ) json.value( get( i++ ) ); // Write non-null value to JSON and move to next index.
					else if( ii == -1 || size <= ii ) { // No more non-null values or reached end of list.
						while( i++ < size ) json.value(); // Write remaining nulls as JSON null.
						break;
					}
					else for( ; i < ii; i++ ) json.value(); // Write nulls up to the next non-null value.
			}
			json.exitArray();
		}
		
		/**
         * Copies a range of elements to a boxed array, preserving nulls.
		 *
         * @param index Starting index.
         * @param len Number of elements to copy.
         * @param dst Destination array (or new if null/too small).
         * @return Populated array with nulls as {@code null}.
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
         * Copies a range of elements to a primitive array, substituting nulls.
		 *
         * @param index Starting index.
         * @param len Number of elements to copy.
         * @param dst Destination array (or new if null/too small).
         * @param null_substitute Value for null elements.
         * @return Populated array with nulls substituted.
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
         * Checks if this list contains all elements from another list, including nulls.
         * For nulls, requires at least one null if the source has any.
		 *
         * @param src Source list to check.
         * @return {@code true} if all elements are present, {@code false} otherwise.
		 */
		public boolean containsAll( R src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				if( src.hasValue( i ) ) { if( indexOf( src.get( i ) ) == -1 ) return false; }
				else if( nextNullIndex( -1 ) == -1 ) return false;
			
			return true;
		}
		
		
	}
	
	/**
     * {@code RW} extends {@link R} to provide read-write functionality for an {@link IntNullList}.
	 * <p>
     * It supports adding, removing, and setting elements (including nulls), and manages storage
     * strategies dynamically based on {@code flatStrategyThreshold} and operational needs.
	 */
	class RW extends R {
		
		/**
         * Constructs an empty list with specified capacity and default value 0.
		 *
         * @param length Initial capacity.
		 */
		public RW( int length ) {
			super( ( char ) 0 ); // Call super constructor with default value 0.
			nulls  = new BitList.RW( length ); // Initialize nulls bitlist with given length.
			values = length > 0 ?
					new char[ length ] :
					// Allocate value array if length > 0.
					Array.EqualHashOf.chars     .O; // Use empty array if length is 0.
		}
		
		/**
         * Constructs a list of given size with a boxed default value.
         * <ul>
         *     <li>If {@code default_value} is {@code null}, all elements are null.</li>
         *     <li>If non-null, all elements are set to that value and marked non-null.</li>
         * </ul>
         * May start in flat strategy if size exceeds threshold.
		 *
         * @param default_value Boxed default value (or {@code null}).
         * @param size Initial logical size (negative sets capacity, size 0).
		 */
		public RW(  Character default_value, int size ) {
			super( default_value == null ?
					       0 :
					       // Use 0 as default primitive value if default_value is null.
					       default_value.charValue     () ); // Otherwise, use the int value of default_value.
			nulls       = new BitList.RW( default_value != null, size ); // Initialize nulls bitlist, pre-set bits if default_value is not null.
			cardinality = nulls.cardinality();
			
			values = size == 0 ?
					Array.EqualHashOf.chars     .O :
					// Use empty array if size is 0.
					new char[ size < 0 ?
							-size :
							// If size is negative, use its absolute value for initial capacity but set cardinality to 0.
							size ]; // Otherwise, use size for both capacity and cardinality.
			
			if( 0 < size && default_value != null && default_value != 0 ) {
				isFlatStrategy = flatStrategyThreshold <= cardinality;
				Arrays.fill( values, 0, size, ( char ) ( default_value + 0 ) ); // Fill array with default value if size > 0 and default_value is not null or 0.
			}
			
		}
		
		/**
         * Constructs a list of given size with a primitive default value.
         * All elements are set to {@code default_value} and marked non-null.
         * May start in flat strategy if size exceeds threshold.
		 *
         * @param default_value Primitive default value.
         * #param size Initial logical size (negative sets capacity, size 0).
		 */
		public RW( char default_value, int size ) {
			super( default_value ); // Call super constructor with the given default value.
			nulls       = new BitList.RW( true, size ); // Initialize nulls bitlist, initially all nulls.
			cardinality = nulls.cardinality();
			
			values = size == 0 ?
					Array.EqualHashOf.chars     .O :
					// Use empty array if size is 0.
					new char[ size < 0 ?
							-size :
							// If size is negative, use its absolute value for initial capacity but set cardinality to 0.
							size ]; // Otherwise, use size for both capacity and cardinality.
			
			if( 0 < size && default_value != 0 ) {
				isFlatStrategy = flatStrategyThreshold <= cardinality;
				Arrays.fill( values, 0, size, ( char ) ( default_value + 0 ) ); // Fill array with default value if size > 0 and default_value is not null or 0.
			}
		}
		
		/**
         * Sets the threshold for strategy switching and may trigger an immediate switch.
		 * <ul>
         *     <li>If {@code threshold} ≤ {@code cardinality} and currently compressed, switches to flat.</li>
         *     <li>If {@code threshold} > {@code cardinality} and currently flat, switches to compressed.</li>
		 * </ul>
         * Operations like {@code add} or {@code set} may also trigger switches based on resizing needs.
		 *
         * @param threshold New threshold value (higher favors compressed, lower favors flat).
		 * @see #switchToFlatStrategy()
		 * @see #switchToCompressedStrategy()
		 */
		public void flatStrategyThreshold( int threshold ) {
			
			if( ( flatStrategyThreshold = threshold ) <= nulls.cardinality() ) { if( !isFlatStrategy ) switchToFlatStrategy(); }
			else if( isFlatStrategy ) switchToCompressedStrategy();
		}
		
		
		/**
         * Creates a deep copy of this list.
		 *
         * @return Cloned {@code RW} instance.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		/**
		 * Removes the last element from the list.
		 *
         * @return This instance for chaining.
		 */
		public RW remove() { return remove( size() - 1 ); }
		
		/**
         * Removes the element at the specified index, shifting subsequent elements left.
         * No effect if index is out of bounds.
		 *
         * @param index Logical index to remove.
         * @return This instance for chaining.
		 */
		public RW remove( int index ) {
			if( index < 0 || size() <= index ) return this;
			if( isFlatStrategy ) {
				nulls.remove( index ); // Remove the nullity bit at the index.
				Array.resize( values, values, index, nulls.size + 1, -1 );
			}
			else { // Compressed strategy.
				if( nulls.get( index ) ) cardinality = Array.resize( values, values, nulls.rank( index ) - 1, cardinality, -1 ); // Shrink compressed array by removing the value.
				nulls.remove( index ); // Remove the nullity bit at the index.
			}
			return this;
		}
		
		/**
         * Sets the last element (or first if empty) to a boxed value.
		 *
         * @param value Boxed value (may be {@code null}).
         * @return This instance for chaining.
		 */
		public RW set1(  Character value ) {
			set( this, Math.max( 0, size() - 1 ), value ); // Delegate to static set method for Integer.
			return this;
		}
		
		/**
         * Sets the last element (or first if empty) to a primitive value, marking it non-null.
		 *
         * @param value Primitive value.
         * @return This instance for chaining.
		 */
		public RW set1( char value ) {
			set( this, Math.max( 0, size() - 1 ), value ); // Delegate to static set method for primitive int.
			return this;
		}
		
		/**
         * Sets an element at the specified index to a boxed value. Extends list with nulls if needed.
		 *
         * @param index Logical index.
         * @param value Boxed value (may be {@code null}).
         * @return This instance for chaining.
		 */
		public RW set1( int index,  Character value ) {
			set( this, index, value ); // Delegate to static set method for Integer at index.
			return this;
		}
		
		/**
         * Sets an element at the specified index to a primitive value, marking it non-null.
         * Extends list with nulls if needed.
		 *
         * @param index Logical index.
         * @param value Primitive value.
         * @return This instance for chaining.
		 */
		public RW set1( int index, char value ) {
			set( this, index, value ); // Delegate to static set method for primitive int at index.
			return this;
		}
		
		/**
         * Sets multiple elements from an index using boxed values. Extends list if needed.
		 *
         * @param index Starting index.
         * @param values Boxed values (may include {@code null}).
         * @return This instance for chaining.
		 */
		public RW set( int index,  Character... values ) {
			for( int i = 0; i < values.length; i++ ) set( this, index + i, values[ i ] ); // Iterate backwards and set each value.
			return this;
		}
		
		/**
         * Sets multiple elements from an index using primitive values, marking them non-null.
         * Extends list if needed.
		 *
         * @param index Starting index.
         * @param values Primitive values.
         * @return This instance for chaining.
		 */
		public RW set( int index, char... values ) {
			return set( index, values, 0, values.length ); // Delegate to array range set method.
		}
		
		/**
         * Sets a range of elements from a primitive array, marking them non-null.
         * Extends list if needed.
		 *
         * @param index Starting index.
         * @param src Source array.
         * @param src_index Source starting index.
         * @param len Number of elements to set.
         * @return This instance for chaining.
		 */
		public RW set( int index, char[] src, int src_index, int len ) {
			for( int i = 0; i < src.length; i++ ) set( this, index + i, ( char ) src[ src_index + i ] ); // Iterate backwards and set each value from source array.
			return this;
		}
		
		/**
         * Sets a range of elements from a boxed array. Extends list if needed.
		 *
         * @param index Starting index.
         * @param src Source array (may include {@code null}).
         * @param src_index Source starting index.
         * @param len Number of elements to set.
         * @return This instance for chaining.
		 */
		public RW set( int index,  Character[] src, int src_index, int len ) {
			for( int i = 0; i < src.length; i++ ) set( this, index + i, src[ src_index + i ] ); // Iterate backwards and set each value from source array.
			return this;
		}
		
		/**
         * Adds a boxed value to the end of the list. May trigger strategy switch.
		 *
         * @param value Boxed value (may be {@code null}).
         * @return This instance for chaining.
		 */
		public RW add1(  Character value ) {
			set( this, size(), value );
			return this;
		}
		
		/**
         * Adds a primitive value to the end of the list, marking it non-null.
         * May trigger strategy switch.
		 *
         * @param value Primitive value.
         * @return This instance for chaining.
		 */
		public RW add1( char value ) {
			set( this, size(), value );
			return this;
		}
		
		/**
         * Adds multiple primitive values to the end of the list, marking them non-null.
         * May trigger strategy switch.
		 *
         * @param items Primitive values.
         * @return This instance for chaining.
		 */
		public RW add( char... items ) { // VEXT -> int
			if( items == null || items.length == 0 ) return this;
			// Optimize: Could potentially pre-calculate final size/cardinality,
			// resize/switch strategy once, then copy data.
			// Current impl uses set() which calls single-element set repeatedly.
			return set( size(), items, 0, items.length ); // Set the new items starting from the current end
		}
		
		/**
         * Adds multiple boxed values to the end of the list. May trigger strategy switch.
		 *
         * @param items Boxed values (may include {@code null}).
         * @return This instance for chaining.
		 */
		public RW add(  Character[] items ) { return set( size(), items ); }
		
		
		/**
         * Inserts a boxed value at an index, shifting elements right. Extends list if needed.
         * May trigger strategy switch.
		 *
         * @param index Logical index.
         * @param value Boxed value (may be {@code null}).
         * @return This instance for chaining.
		 */
		public RW add1( int index,  Character value ) {
			if( value == null ) {
				int s = size();
				nulls.add( index, false );
				
				if( isFlatStrategy )
					Array.resize( values,
					              values.length <= nulls.size() - 1 ?
							              values = new char[ ( nulls.size() - 1 ) * 2 / 3 ] :
							              values, index, s, 1 );
			}
			else add1( index, value.charValue     () ); // Delegate to primitive add at index for non-null value.
			return this;
		}
		
		/**
         * Inserts a primitive value at an index, marking it non-null and shifting elements right.
         * Extends list if needed. May trigger strategy switch.
		 *
         * @param index Logical index.
         * @param value Primitive value.
         * @return This instance for chaining.
		 */
		public RW add1( int index, char value ) {
			
			if( index < size() )// Inserting within bounds.
				if( isFlatStrategy ) {
					// Insert non-null bit at index.
					
					nulls.add( index, true );
					Array.resize( values,
					              values.length <= nulls.size() - 1 ?
							              values = new char[ nulls.size() + nulls.size() / 2 ] :
							              values, index, size(), 1 );
					
					values[ index ] = ( char ) value; // Insert value in flat array.
				}
				else { // Compressed strategy.
					
					// Insert non-null bit at index.
					
					nulls.add( index, true );
					cardinality++;
					int rank = nulls.rank( index ) - 1; // Calculate rank for insertion.
					
					if( values.length <= cardinality && flatStrategyThreshold <= nulls.used * 64 ) {
						switchToFlatStrategy();
						values[ index ] = ( char ) value; // Insert value in flat array.
						return this;
					}
					
					
					Array.resize( values, values.length <= cardinality ?
							values = new char[ cardinality + cardinality / 2 ] :
							values, rank, cardinality - 1, 1 ); // Make space for new value in compressed array.
					values[ rank ] = ( char ) value; // Insert value in compressed array.
				}
			else set1( index, value ); // If index is beyond current size, treat as set operation.
			return this;
		}
		
		
		/**
         * Adds all elements from another list to the end of this list.
		 *
         * @param src Source list.
         * @return This instance for chaining.
		 */
		public RW addAll( R src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				// Add null if source has null at this position.
				if( src.hasValue( i ) ) add1( src.get( i ) ); // Add non-null value from source.
				else nulls.set( nulls.size, false );
			return this;
		}
		
		/**
         * Clears all elements, resetting the list to empty.
		 *
         * @return This instance for chaining.
		 */
		public RW clear() {
			cardinality = 0; // Reset cardinality.
			nulls.clear(); // Clear the nulls bitlist.
			return this;
		}
		
		/**
         * Sets the physical capacity of the list. Truncates if smaller than current length.
         * May trigger strategy switch.
		 *
         * @param length New capacity (non-negative).
         * @return This instance for chaining.
		 */
		public RW length( int length ) {
			if( length < 1 ) {
				clear(); // If length is less than 1, clear the list.
				return this;
			}
			
			nulls.length( length ); // Set new length for nulls bitlist.
			
			if( isFlatStrategy ) {
				if( values.length != length )
					if( nulls.cardinality() < flatStrategyThreshold ) switchToCompressedStrategy();
					else values = Arrays.copyOf( values, length ); // Resize flat array if needed.
			}
			else if( length < cardinality )
				if( flatStrategyThreshold < ( cardinality = nulls.cardinality() ) ) switchToFlatStrategy();
				else values = Arrays.copyOf( values, cardinality ); // Resize flat array if needed.
			
			return this;
		}
		
		/**
         * Sets the logical size of the list. If increased, extends with nulls, setting the last
         * element to {@code default_value} if non-null. If decreased, truncates.
		 *
         * @param size New size (non-negative).
         * @return This instance for chaining.
		 */
		public RW size( int size ) {
			if( size < 1 ) clear(); // If size is less than 1, clear the list.
			if( this.size() < size ) set1( size - 1, default_value ); // If increasing size, ensure last element is set (though it might be null).
			else {
				nulls.size( size ); // Set new size for nulls bitlist.
				if( !isFlatStrategy ) cardinality = nulls.cardinality();
			}
			return this;
		}
		
		/**
         * Minimizes memory usage by setting capacity to current size. May switch to compressed
         * strategy if density is low.
		 *
         * @return This instance for chaining.
		 */
		public RW fit() {
			length( size() );
			return this;
		}
		
		/**
         * Trims trailing nulls by adjusting size to the last non-null element + 1.
         * May switch to compressed strategy if density is low.
		 *
         * @return This instance for chaining.
		 */
		public RW trim() {
			int last = nulls.last1() + 1; // Find index of last non-null element + 1.
			length( last ); // Set length to trim trailing nulls.
			if( isFlatStrategy && nulls.used * 64 < flatStrategyThreshold ) switchToCompressedStrategy(); // Potentially switch back to compressed if null density is low enough.
			return this;
		}
		
		/**
         * Swaps elements (and nullity) at two indices. Efficient for both strategies.
		 *
         * @param index1 First index.
         * @param index2 Second index.
         * @return This instance for chaining.
		 */
		public RW swap( int index1, int index2 ) {
			if( index1 == index2 || index1 < 0 || index2 < 0 || size() <= index1 || size() <= index2 ) return this; // No need to swap if indices are the same.
			
			boolean e1 = nulls.get( index1 ); // Get nullity status of element at index1.
			boolean e2 = nulls.get( index2 ); // Get nullity status of element at index2.
			
			if( isFlatStrategy )
				if( e1 && e2 ) { // Both elements are non-null in flat strategy.
					char value1 = values[ index1 ];
					values[ index1 ] = values[ index2 ];
					values[ index2 ] = value1; // Swap values directly.
				}
				else if( !e1 && !e2 ) return this; // Both are null, no action needed.
				else if( e1 ) { // index1 is non-null, index2 is null.
					values[ index2 ] = values[ index1 ]; // Move value from index1 to index2.
					nulls.set0( index1 ); // Update nullity at index1 to null.
					nulls.set1( index2 ); // Update nullity at index2 to non-null.
				}
				else { // index1 is null, index2 is non-null.
					values[ index1 ] = values[ index2 ]; // Move value from index2 to index1.
					nulls.set1( index1 ); // Update nullity at index1 to non-null.
					nulls.set0( index2 ); // Update nullity at index2 to null.
				}
			else  // Compressed strategy.
			{
				if( !e1 && !e2 ) return this; // Both are null, no action needed.
				int i1 = nulls.rank( index1 ) - 1; // Get rank of index1.
				int i2 = nulls.rank( index2 ) - 1; // Get rank of index2.
				
				if( e1 && e2 ) { // Both elements are non-null in compressed strategy.
					
					char tmp = values[ i1 ];
					values[ i1 ] = values[ i2 ];
					values[ i2 ] = tmp; // Swap values in compressed array using ranks.
				}
				else {
					nulls.set( index1, e2 );// Swap nullity: index1 gets index2's state
					nulls.set( index2, e1 );// Swap nullity: index2 gets index1's state
					
					if( Math.abs( index1 - index2 ) < 2 ) return this;
					
					int exist = e1 ?
							i1 :
							i2;
					int empty = e1 ?
							i2 + 1 :
							i1 + 1;
					
					
					char v = values[ exist ]; // Get the value to be moved.
					
					Array.resize( values, values, exist, cardinality, -1 );// Remove the value from its old position,
					Array.resize( values, values,
					              exist < empty ?
							              empty - 1 :
							              empty, cardinality - 1, 1 );//expand empty
					
					values[ empty ] = v; // Insert the moved value at the new rank.
				}
			}
			
			return this;
		}
		
		/**
         * Switches to flat strategy, reallocating {@code values} for direct indexing.
         * Assumes {@code nulls} and {@code cardinality} are up-to-date.
		 */
		protected void switchToFlatStrategy() {
			if( size() == 0 )//the collection is empty
			{
				if( values.length == 0 ) values = new char[ 16 ];
				isFlatStrategy = true;
				return;
			}
			
			char[] flat = new char[ nulls.last1() * 3 / 2 ]; // Allocate flat array with some extra capacity for growth.
			for( int i = nulls.next1( 0 ), ii = 0; ii < cardinality; i = nulls.next1( i + 1 ) )
			     flat[ i ] = values[ ii++ ];
			
			this.values    = flat;
			isFlatStrategy = true;
		}
		
		
		/**
         * Switches to compressed strategy, packing non-null values into {@code values}.
		 */
		protected void switchToCompressedStrategy() {
			
			cardinality = nulls.cardinality(); // Count of non-null elements.
			for( int i = nulls.next1( -1 ), ii = 0; i != -1; i = nulls.next1( i + 1 ), ii++ )
				if( i != ii ) values[ ii ] = values[ i ];// Pack non-null values sequentially in the same array.
			
			isFlatStrategy = false;
		}
		
		/**
         * Sets a boxed value at an index, handling nulls and extensions.
		 *
         * @param dst Target list.
         * @param index Logical index.
         * @param value Boxed value (may be {@code null}).
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
			else set( dst, index, value.charValue     () ); // Delegate to primitive int setter for non-null value.
		}
		
		/**
         * Sets a primitive value at an index, marking it non-null. May trigger strategy switch.
		 *
         * @param dst Target list.
         * @param index Logical index.
         * @param value Primitive value.
		 */
		protected static void set( RW dst, int index, char value ) {
			
			if( dst.isFlatStrategy ) {
				if( dst.values.length <= index ) dst.values = Arrays.copyOf( dst.values, Math.max( index + 1, dst.values.length * 3 / 2 ) ); // Ensure array capacity.
				dst.values[ index ] = ( char ) value; // Set value in flat array.
				dst.nulls.set1( index ); // Mark as non-null.
			}
			else if( dst.nulls.get( index ) ) dst.values[ dst.nulls.rank( index ) - 1 ] = ( char ) value; // Update existing value.
			else {
				int rank = dst.nulls.rank( index );
				int max  = Math.max( rank, dst.cardinality );
				
				if( dst.values.length <= max && dst.flatStrategyThreshold <= dst.cardinality ) {
					dst.switchToFlatStrategy();
					dst.nulls.set1( index ); // Mark as non-null.
					dst.values[ index ] = ( char ) value; // Set value in flat array.
				}
				else {
					dst.cardinality    = Array.resize( dst.values,
					                                   dst.values.length <= max ?
							                                   ( dst.values = new char[ 2 + max * 3 / 2 ] ) :
							                                   dst.values,
					                                   rank, dst.cardinality, 1 ); // Insert a slot for the new value.
					dst.values[ rank ] = ( char ) value; // Set the new value in compressed array.
					dst.nulls.set1( index ); // Mark as non-null.
				}
			}
		}
	}
}