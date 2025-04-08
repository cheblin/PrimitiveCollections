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
 * {@code IntNullList} defines a contract for a list that stores nullable primitive values.
 * <p>
 * Implementations of this interface manage lists of primitives that can contain null values.
 * It provides efficient methods for adding, removing, and accessing elements, while also
 * effectively tracking nullity for each element.
 */
public interface FloatNullList {
	
	/**
	 * {@code R} is an abstract base class providing a read-only implementation of the {@link IntNullList} interface.
	 * <p>
	 * It uses a {@link BitList.RW} to efficiently track null values and an array to store
	 * the actual values. {@code R} supports two distinct storage strategies to optimize
	 * performance based on the density of null values within the list:
	 * <p>
	 * <b>1. Compressed (Rank-Based) Strategy:</b>
	 * - This strategy is space-efficient when the list contains many null values.
	 * - Only non-null values are stored contiguously in the {@code values} array.
	 * - The {@link BitList.RW} {@code nulls} tracks the positions of non-null values.
	 * - To access a value at a given index, the rank of the index in the {@code nulls} bitlist
	 * is used to find the corresponding value in the {@code values} array.
	 * <p>
	 * <b>2. Flat (One-to-One) Strategy:</b>
	 * - This strategy is optimized for performance when the list contains few null values
	 * and frequent accesses are needed.
	 * - All elements, including placeholders for null values, are stored in the {@code values} array in a one-to-one mapping with the logical indices of the list.
	 * - {@code nulls} bitlist still tracks nullity, but indexing is direct and faster.
	 * <p>
	 * The choice between these strategies is dynamically managed based on a {@code flatStrategyThreshold},
	 * aiming to provide the best balance between space and time efficiency.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * {@code nulls} is a {@link BitList.RW} that tracks null values within the list.
		 * {@code true} at a given index indicates a non-null value at that position in the logical list.
		 * {@code false} indicates a null value.
		 */
		protected BitList.RW nulls;
		
		/**
		 * {@code values} is the array used to store the values of the list.
		 * <p>
		 * - In <b>Compressed Strategy</b>, it stores only the non-null values compactly.
		 * - In <b>Flat Strategy</b>, it stores all values.
		 */
		protected float[] values;
		
		/**
		 * {@code default_value} is the default value used when the list needs to be extended
		 * or when uninitialized elements are accessed. This value is inherited from {@code IntList.R}.
		 */
		public final float default_value;
		
		/**
		 * {@code cardinality} stores the number of non-null elements in the list when using the
		 * <b>Compressed Strategy</b>. In <b>Flat Strategy</b>, this field is unused and typically set to 0.
		 */
		protected int cardinality = 0;
		
		/**
		 * {@code flatStrategyThreshold} is the threshold that determines when to switch from
		 * <b>Compressed Strategy</b> to <b>Flat Strategy</b>. The switch is typically based on the
		 * density of null values. The default value is 1024.
		 */
		protected int flatStrategyThreshold = 1024;
		
		
		/**
		 * {@code isFlatStrategy} is a boolean flag indicating the current storage strategy being used.
		 * {@code true} indicates <b>Flat Strategy</b> is active.
		 * {@code false} indicates <b>Compressed Strategy</b> is active.
		 */
		protected boolean isFlatStrategy = false;
		
		/**
		 * Constructs a new {@code R} instance.
		 *
		 * @param default_value The default value to be used for elements in this list.
		 */
		protected R( float default_value ) {
			this.default_value = default_value;
			this.values        = Array.EqualHashOf.floats     .O; // Initialize with an empty array.
		}
		
		
		public int length() { return values.length; }
		
		
		public int size()   { return nulls.size(); }
		
		/**
		 * Checks if the list is empty (contains no elements).
		 *
		 * @return {@code true} if the list is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() < 1; }
		
		/**
		 * Checks if there is a non-null value at the specified index.
		 *
		 * @param index The index to check.
		 * @return {@code true} if the value at the given {@code index} is non-null, {@code false} if it is null.
		 */
		public boolean hasValue( int index ) {
			return nulls.get( index );
		}
		
		/**
		 * Finds the index of the next non-null value starting from the given {@code index} (inclusive).
		 *
		 * @param index The starting index for the search.
		 * @return The index of the next non-null value, or -1 if no non-null value is found from {@code index} onwards.
		 */
		public @Positive_OK int nextValueIndex( int index ) {
			return nulls.next1( index );
		}
		
		/**
		 * Finds the index of the previous non-null value before the given {@code index} (exclusive).
		 *
		 * @param index The starting index for the backward search.
		 * @return The index of the previous non-null value, or -1 if no non-null value is found before {@code index}.
		 */
		public @Positive_OK int prevValueIndex( int index ) {
			return nulls.prev1( index );
		}
		
		/**
		 * Finds the index of the next null value starting from the given {@code index} (inclusive).
		 *
		 * @param index The starting index for the search.
		 * @return The index of the next null value, or -1 if no null value is found from {@code index} onwards.
		 */
		public @Positive_OK int nextNullIndex( int index ) { return nulls.next0( index ); }
		
		/**
		 * Finds the index of the previous null value before the given {@code index} (exclusive).
		 *
		 * @param index The starting index for the backward search.
		 * @return The index of the previous null value, or -1 if no null value is found before {@code index}.
		 */
		public @Positive_OK int prevNullIndex( int index ) { return nulls.prev0( index ); }
		
		/**
		 * Retrieves the value at the specified {@code index}.
		 * Callers should use {@link #hasValue(int)} to check for nullity before calling {@code get}
		 *
		 * @param index The index of the element to retrieve.
		 * @return The value at the specified {@code index}, or 0 if the value is logically null.
		 */
		public float get( @Positive_ONLY int index ) {
			return ( isFlatStrategy ?
					values[ index ] :
					values[ nulls.rank( index ) - 1 ] ); // Rank-based access in compressed strategy.
		}
		
		/**
		 * Returns the index of the first occurrence of the specified non-null primitive {@code value} in the list.
		 *
		 * @param value The non-null value to search for.
		 * @return The index of the first occurrence of the {@code value}, or -1 if the value is not found in the list or if {@code value} is null.
		 */
		public int indexOf( float value ) {
			if( isFlatStrategy ) {
				for( int i = nulls.next1( 0 ); i != -1; i = nulls.next1( i + 1 ) )
					if( values[ i ] == ( float ) value ) return i; // Linear search in flat array.
				return -1;
			}
			
			int i = Array.indexOf( values, ( float ) value, 0, cardinality ); // Search in compressed array.
			return i < 0 ?
					-1 :
					// Value not found in compressed array.
					nulls.bit( i + 1 ); // Convert rank in compressed array to logical index.
		}
		
		/**
		 * Checks if the list contains the specified non-null primitive {@code value}.
		 *
		 * @param value The non-null value to check for.
		 * @return {@code true} if the list contains the {@code value}, {@code false} otherwise.
		 */
		public boolean contains( float value ) { return indexOf( value ) != -1; }
		
		/**
		 * Checks if the list contains the specified {@code value} (which can be null).
		 *
		 * @param value The  value (can be null) to check for.
		 * @return {@code true} if the list contains the {@code value}, {@code false} otherwise.
		 */
		public boolean contains(  Float     value ) {
			return value == null ?
					nextNullIndex( 0 ) != -1 :
					// Check for null value.
					indexOf( value ) != -1;     // Check for non-null value.
		}
		
		/**
		 * Returns the index of the last occurrence of the specified non-null primitive {@code value} in the list.
		 *
		 * @param value The non-null value to search for.
		 * @return The index of the last occurrence of the {@code value}, or -1 if the value is not found.
		 */
		public int lastIndexOf( float value ) {
			if( isFlatStrategy ) {
				for( int i = nulls.last1(); i != -1; i = nulls.prev1( i - 1 ) )
					if( values[ i ] == ( float ) value ) return i; // Reverse linear search in flat array.
				return -1;
			}
			else {
				int i = Array.lastIndexOf( values, ( float ) value, 0, cardinality ); // Reverse search in compressed array.
				return i < 0 ?
						-1 :
						// Value not found in compressed array.
						nulls.bit( i + 1 ); // Convert rank to logical index.
			}
		}
		
		/**
		 * Compares this list to another object for equality.
		 * Equality is determined by the logical contents of the lists (including null values) and not by the
		 * internal storage strategy.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the given object is an instance of {@code R} and has the same logical content as this list, {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) );
		}
		
		/**
		 * Computes the hash code for this list.
		 * The hash code is based on the logical content of the list (including null values) and is consistent
		 * regardless of the current storage strategy (flat or compressed).
		 *
		 * @return The hash code value for this list.
		 */
		@Override
		public int hashCode() {
			int hash = Array.hash( nulls ); // Start with hash of the nullity bitlist.
			
			if( isFlatStrategy )
				for( int i = nulls.next1( 0 ); i != -1; i = nulls.next1( i + 1 ) )
				     hash = Array.mix( hash, Array.hash( values[ i ] ) ); // Mix in the hash of each non-null value.
			else
				hash = Array.hash( hash, values, 0, cardinality );
			
			return Array.finalizeHash( hash, size() ); // Finalize the hash with the length of the list.
		}
		
		/**
		 * Performs a detailed equality check with another {@code R} instance.
		 * This method efficiently compares the logical contents of two {@code R} lists, handling both
		 * flat and compressed strategies and ensuring that null values are also compared correctly.
		 *
		 * @param other The other {@code R} instance to compare with.
		 * @return {@code true} if the two lists are logically equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			int size;
			if( other == this ) return true;
			if( other == null || ( size = size() ) != other.size() || !nulls.equals( other.nulls ) || cardinality != other.cardinality ) return false; // Quick checks for inequality.
			
			boolean b;
			for( int i = 0; i < size; i++ )
				if( ( b = hasValue( i ) ) != other.hasValue( i ) || b && get( 1 ) != other.get( i ) ) return false;
			
			return true; // Lists are equal if all checks pass.
		}
		
		/**
		 * Creates and returns a deep copy of this {@code R} instance.
		 * The clone will have the same logical content and internal strategy as the original.
		 *
		 * @return A new {@code R} instance that is a clone of this list.
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
		 * Returns a JSON string representation of this list.
		 * Delegates to {@link #toJSON()}.
		 *
		 * @return A JSON formatted string representing the list's content.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the content of this list in JSON format to the provided {@link JsonWriter}.
		 * Null values are represented as JSON null, and non-null values are represented as JSON numbers.
		 *
		 * @param json The {@link JsonWriter} to write the JSON output to.
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
			json.exitArray(); // End JSON array.
		}
		
		/**
		 * Copies a range of elements into a destination array, preserving nulls.
		 *
		 * @param index Starting logical index.
		 * @param len   Number of elements to copy.
		 * @param dst   Destination array; resized if null or too small.
		 * @return Array with copied elements, or {@code null} if empty.
		 */
		public  Float    [] toArray( int index, int len,  Float    [] dst ) {
			if( size() == 0 ) return null;
			if( dst == null || dst.length < len ) dst = new  Float    [ len ];
			
			else for( int i = 0, srcIndex = index; i < len && srcIndex < size(); i++, srcIndex++ )
			          dst[ i ] = hasValue( srcIndex ) ?
					          
					          ( values[ nulls.rank( srcIndex ) - 1 ] ) :
					          null;
			return dst;
		}
		
		/**
		 * Copies a range of elements into a destination array, preserving nulls.
		 *
		 * @param index Starting logical index.
		 * @param len   Number of elements to copy.
		 * @param dst   Destination array; resized if null or too small.
		 * @return Array with copied elements, or {@code null} if empty.
		 */
		public float[] toArray( int index, int len, float[] dst, float null_substitute ) {
			if( size() == 0 ) return null;
			if( dst == null || dst.length < len ) dst = new float[ len ];
			
			else for( int i = 0, srcIndex = index; i < len && srcIndex < size(); i++, srcIndex++ )
			          dst[ i ] =  ( hasValue( srcIndex ) ?
					          values[ nulls.rank( srcIndex ) - 1 ] :
					          null_substitute );
			return dst;
		}
		
		
		/**
		 * Checks if this list contains all non-null elements from another {@link ObjectList.R}.
		 *
		 * @param src Source list to check against.
		 * @return {@code true} if all non-null elements are present, {@code false} otherwise.
		 */
		public boolean containsAll( R src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				if( src.hasValue( i ) ) { if( indexOf( src.get( i ) ) == -1 ) return false; }
				else if( nextNullIndex( 0 ) == -1 ) return false;
			
			return true;
		}
		
		
	}
	
	/**
	 * {@code RW} is a read-write implementation of the {@link IntNullList} interface, extending {@link R}.
	 * <p>
	 * It provides methods to modify the list, such as adding, removing, and setting elements, while
	 * automatically managing null values and switching between the flat and compressed storage strategies
	 * based on the configured {@code flatStrategyThreshold}.
	 */
	class RW extends R {
		
		/**
		 * Constructs a new {@code RW} list with the specified initial capacity and a default value of 0.
		 *
		 * @param length The initial capacity of the list.
		 */
		public RW( int length ) {
			super( ( float ) 0 ); // Call super constructor with default value 0.
			nulls  = new BitList.RW( length ); // Initialize nulls bitlist with given length.
			values = length > 0 ?
					new float[ length ] :
					// Allocate value array if length > 0.
					Array.EqualHashOf.floats     .O; // Use empty array if length is 0.
		}
		
		/**
		 * Constructs a new {@code RW} list with a specified default boxed  value and initial size.
		 *
		 * @param default_value The default value for the list. Can be null.
		 * @param size          The initial size of the list.
		 */
		public RW(  Float     default_value, int size ) {
			super( default_value == null ?
					       0 :
					       // Use 0 as default primitive value if default_value is null.
					       default_value.floatValue     () ); // Otherwise, use the int value of default_value.
			nulls       = new BitList.RW( default_value != null, size ); // Initialize nulls bitlist, pre-set bits if default_value is not null.
			cardinality = nulls.cardinality();
			
			values = size == 0 ?
					Array.EqualHashOf.floats     .O :
					// Use empty array if size is 0.
					new float[ cardinality = size < 0 ?
							-size :
							// If size is negative, use its absolute value for initial capacity but set cardinality to 0.
							size ]; // Otherwise, use size for both capacity and cardinality.
			
			if( 0 < size && default_value != null && default_value != 0 ) {
				isFlatStrategy = flatStrategyThreshold < size * 64;
				Arrays.fill( values, 0, size, ( float ) ( default_value + 0 ) ); // Fill array with default value if size > 0 and default_value is not null or 0.
			}
			
		}
		
		/**
		 * Constructs a new {@code RW} list with a specified primitive default value and initial size.
		 *
		 * @param default_value The default primitive value for the list.
		 * @param size          The initial size of the list.
		 */
		public RW( float default_value, int size ) {
			super( default_value ); // Call super constructor with the given default value.
			nulls       = new BitList.RW( false, size ); // Initialize nulls bitlist, initially all nulls.
			cardinality = nulls.cardinality();
			
			values = size == 0 ?
					Array.EqualHashOf.floats     .O :
					// Use empty array if size is 0.
					new float[ this.cardinality = size < 0 ?
							-size :
							// If size is negative, use its absolute value for initial capacity but set cardinality to 0.
							size ]; // Otherwise, use size for both capacity and cardinality.
			
			if( 0 < size && default_value != 0 ) {
				isFlatStrategy = flatStrategyThreshold < size * 64;
				Arrays.fill( values, 0, size, ( float ) ( default_value + 0 ) ); // Fill array with default value if size > 0 and default_value is not null or 0.
			}
		}
		
		/**
		 * Sets the threshold for switching to the flat storage strategy.
		 * <p>
		 * The class employs two storage strategies: compressed and flat.
		 * The compressed strategy is more space-efficient, especially when the list contains many null values.
		 * However, accessing elements in the compressed strategy involves calculating the rank of indices in the {@link BitList.RW} ({@code nulls}),
		 * which can become less performant as the size of the {@code BitList.RW} grows.
		 * The flat strategy, on the other hand, prioritizes access speed by storing values in a one-to-one mapping,
		 * but it consumes more memory as it allocates space for all logical elements, including placeholders for nulls.
		 * <p>
		 * This method, {@code flatStrategyThreshold}, allows you to adjust the point at which the list automatically switches
		 * from the space-efficient compressed strategy to the performance-optimized flat strategy.
		 * The switch is triggered based on the usage of the {@link BitList.RW} instance (`nulls`) that tracks null values.
		 * <p>
		 * Specifically, the threshold is compared against a value derived from the {@code nulls} bitlist's internal state,
		 * which is approximately the number of bits currently used in the {@code nulls} bitlist's storage array (`nulls.used * 64`).
		 * When this usage value is greater than or equal to the {@code flatStrategyThreshold}, and a resize of the value array is needed
		 * (e.g., when adding a new non-null element in compressed strategy requires array expansion), the list will switch to the flat strategy.
		 * <p>
		 * Increasing {@code interleavedBits} (i.e., setting a higher {@code flatStrategyThreshold}) delays the switch to the flat strategy,
		 * making the list stay in the compressed strategy for longer, even as the {@code nulls} bitlist grows. This is beneficial when
		 * space efficiency is paramount and the performance cost of rank calculations in the compressed strategy is still acceptable.
		 * <p>
		 * Conversely, decreasing {@code interleavedBits} (i.e., setting a lower {@code flatStrategyThreshold}) makes the list switch to the flat
		 * strategy sooner. This prioritizes access performance, as the flat strategy avoids rank calculations, at the expense of potentially
		 * using more memory. Setting {@code interleavedBits} to 0 would effectively force an immediate switch to the flat strategy as soon as
		 * the condition is met, likely on the first resize after initialization if the initial size and threshold allow.
		 * <p>
		 * The default value of {@code flatStrategyThreshold} is 1024. You can adjust this value based on your application's specific needs,
		 * balancing between memory usage and access performance. Consider profiling your application with different threshold values
		 * to find the optimal setting.
		 *
		 * @param interleavedBits The new threshold value for switching to the flat strategy.
		 *                        This value represents a point related to the usage of the {@code nulls} bitlist.
		 *                        A higher value delays the switch to flat strategy, favoring space efficiency.
		 *                        A lower value promotes an earlier switch to flat strategy, favoring access performance.
		 */
		public void flatStrategyThreshold( int interleavedBits ) {
			flatStrategyThreshold = interleavedBits;
		}
		
		/**
		 * Creates and returns a deep copy of this {@code RW} instance.
		 *
		 * @return A new {@code RW} instance that is a clone of this list.
		 */
		@Override
		public RW clone() {
			return ( RW ) super.clone(); // Simply cast the superclass clone result to RW.
		}
		
		/**
		 * Removes the last element from the list.
		 *
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW remove() {
			return remove( size() - 1 ); // Remove element at the last valid index.
		}
		
		/**
		 * Removes the element at the specified {@code index}.
		 *
		 * @param index The index of the element to remove.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW remove( int index ) {
			if( size() < 1 || size() <= index ) return this; // No-op if list is empty or index is out of bounds.
			if( isFlatStrategy ) nulls.remove( index ); // Remove the nullity bit at the index.
			else { // Compressed strategy.
				if( nulls.get( index ) ) cardinality = Array.resize( values, values, nulls.rank( index ) - 1, cardinality, -1 ); // Shrink compressed array by removing the value.
				nulls.remove( index ); // Remove the nullity bit at the index.
			}
			return this;
		}
		
		/**
		 * Sets a boxed  value at the last index of the list.
		 * This is equivalent to modifying the last element if the list is not empty.
		 *
		 * @param value The  value to set (can be null).
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW set1(  Float     value ) {
			set( this, size() - 1, value ); // Delegate to static set method for Integer.
			return this;
		}
		
		/**
		 * Sets a primitive value at the last index of the list.
		 * This is equivalent to modifying the last element if the list is not empty.
		 *
		 * @param value The primitive value to set.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW set1( float value ) {
			set( this, size() - 1, value ); // Delegate to static set method for primitive int.
			return this;
		}
		
		/**
		 * Sets a boxed  value (can be null) at a specific {@code index} in the list.
		 *
		 * @param index The index where the value should be set.
		 * @param value The boxed  value to set (can be null).
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW set1( int index,  Float     value ) {
			set( this, index, value ); // Delegate to static set method for Integer at index.
			return this;
		}
		
		/**
		 * Sets a primitive value at a specific {@code index} in the list.
		 *
		 * @param index The index where the value should be set.
		 * @param value The primitive value to set.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW set1( int index, float value ) {
			set( this, index, value ); // Delegate to static set method for primitive int at index.
			return this;
		}
		
		/**
		 * Sets multiple boxed  values starting from the specified {@code index}.
		 *
		 * @param index  The starting index for setting values.
		 * @param values An array of boxed  values (can be null) to set.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW set( int index,  Float    ... values ) {
			for( int i = 0; i < values.length; i++ ) set( this, index + i, values[ i ] ); // Iterate backwards and set each value.
			return this;
		}
		
		/**
		 * Sets multiple primitive values starting from the specified {@code index}.
		 *
		 * @param index  The starting index for setting values.
		 * @param values An array of primitive values to set.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW set( int index, float... values ) {
			return set( index, values, 0, values.length ); // Delegate to array range set method.
		}
		
		/**
		 * Sets a range of values from a source array of primitives into this list, starting at a specific index.
		 *
		 * @param index     The starting index in this list where the values should be set.
		 * @param values    The source array containing the primitive values to set.
		 * @param src_index The starting index in the source array to copy from.
		 * @param len       The number of elements to copy from the source array.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW set( int index, float[] values, int src_index, int len ) {
			for( int i = 0; i < values.length; i++ ) set( this, index + i, ( float ) values[ src_index + i ] ); // Iterate backwards and set each value from source array.
			return this;
		}
		
		/**
		 * Sets a range of values from a source array of boxed s (can be null) into this list, starting at a specific index.
		 *
		 * @param index     The starting index in this list where the values should be set.
		 * @param values    The source array containing the boxed  values (can be null) to set.
		 * @param src_index The starting index in the source array to copy from.
		 * @param len       The number of elements to copy from the source array.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW set( int index,  Float    [] values, int src_index, int len ) {
			for( int i = 0; i < values.length; i++ ) set( this, index + i, values[ src_index + i ] ); // Iterate backwards and set each value from source array.
			return this;
		}
		
		/**
		 * Adds a boxed  value (can be null) to the end of the list.
		 *
		 * @param value The boxed  value to add (can be null).
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW add1(  Float     value ) {
			if( value == null ) {
				nulls.add( false ); // Add null entry to bitlist.
				if( isFlatStrategy && values.length <= nulls.size() - 1 )
					values = Arrays.copyOf( values, nulls.size() + nulls.size() / 2 ); // Ensure flat array capacity.
			}
			else add1( value.floatValue     () ); // Delegate to primitive add for non-null value.
			
			return this;
		}
		
		/**
		 * Adds a primitive value to the end of the list.
		 *
		 * @param value The primitive value to add.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW add1( float value ) {
			
			int i;
			if( isFlatStrategy ) {
				if( values.length <= ( i = nulls.size() ) ) values = Arrays.copyOf( values, i + i / 2 + 1 ); // Ensure flat array capacity.
			}
			else  // Compressed strategy.
				if( values.length <= ( i = cardinality + 1 ) && flatStrategyThreshold <= nulls.used * 64 ) {
					nulls.add( true ); // Mark as non-null.
					
					switchToFlatStrategy(); // Switch before modification if threshold is met and resize is needed.
					
					
					values[ nulls.size() - 1 ] = ( float ) value; // Add value to flat array.
					return this;
				}
				else i = ( cardinality = Array.resize( values, values.length <= i ?
						( values = new float[ i * 3 / 2 ] ) :
						values, cardinality, cardinality, 1 ) ) - 1;
			
			values[ i ] = ( float ) value; // Add value to flat array.
			nulls.add( true ); // Mark as non-null.
			
			return this;
		}
		
		/**
		 * Adds a boxed  value (can be null) at a specific {@code index} in the list.
		 *
		 * @param index The index at which the value should be inserted.
		 * @param value The boxed  value to add (can be null).
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW add1( int index,  Float     value ) {
			if( value == null ) {
				nulls.add( index, false ); // Insert null bit at index.
				
				if( isFlatStrategy && values.length <= nulls.size() - 1 ) values = Arrays.copyOf( values, nulls.size() + nulls.size() / 2 ); // Ensure flat array capacity.
			}
			else add1( index, value.floatValue     () ); // Delegate to primitive add at index for non-null value.
			return this;
		}
		
		/**
		 * Adds a primitive value at a specific {@code index} in the list.
		 *
		 * @param index The index at which the value should be inserted.
		 * @param value The primitive value to add.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW add1( int index, float value ) {
			// Inserting within bounds.
			if( index < size() )
				if( isFlatStrategy ) {
					nulls.add( index, true ); // Insert non-null bit at index.
					Array.resize( values, values.length <= nulls.size() - 1 ?
							values = new float[ nulls.size() + nulls.size() / 2 ] :
							values, index, size(), 1 );
					
					values[ index ] = ( float ) value; // Insert value in flat array.
				}
				else { // Compressed strategy.
					
					nulls.add( index, true ); // Insert non-null bit at index.
					cardinality++;
					int rank = nulls.rank( index ) - 1; // Calculate rank for insertion.
					
					if( values.length <= cardinality && flatStrategyThreshold <= nulls.used * 64 ) {
						switchToFlatStrategy();
						values[ index ] = ( float ) value; // Insert value in flat array.
						return this;
					}
					
					
					Array.resize( values, values.length <= cardinality ?
							values = new float[ cardinality + cardinality / 2 ] :
							values, rank, cardinality - 1, 1 ); // Make space for new value in compressed array.
					values[ rank ] = ( float ) value; // Insert value in compressed array.
				}
			else set1( index, value ); // If index is beyond current size, treat as set operation.
			return this;
		}
		
		/**
		 * Adds multiple primitive values to the end of the list.
		 *
		 * @param items An array of primitive values to add.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW add( float... items ) {
			return set( size(), items ); // Set the new items starting from the original size.
		}
		
		/**
		 * Adds multiple boxed  values (can be null) to the end of the list.
		 *
		 * @param items An array of boxed  values (can be null) to add.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW add(  Float    ... items ) { return set( size(), items ); }
		
		/**
		 * Adds all elements from another {@link R} list to the end of this list.
		 *
		 * @param src The source {@link R} list to add elements from.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW addAll( R src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				if( src.hasValue( i ) ) add1( src.get( i ) ); // Add non-null value from source.
				else nulls.add( false ); // Add null if source has null at this position.
			return this;
		}
		
		/**
		 * Clears all elements from this list, making it empty.
		 *
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW clear() {
			cardinality = 0; // Reset cardinality.
			nulls.clear(); // Clear the nulls bitlist.
			values         = Array.EqualHashOf.floats     .O; // Reset value array to empty.
			isFlatStrategy = false; // Revert to compressed strategy for empty list.
			return this;
		}
		
		/**
		 * Sets the total capacity (length) of the list.
		 * If the new length is less than the current length, the list is truncated.
		 * If the new length is greater, the list is extended with default values (nulls).
		 *
		 * @param length The new length (capacity) of the list.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW length( int length ) {
			if( length < 1 ) {
				clear(); // If length is less than 1, clear the list.
				return this;
			}
			
			int oldLength = nulls.size();
			
			nulls.length( length ); // Set new length for nulls bitlist.
			
			if( isFlatStrategy ) {
				if( values.length != length )
					values = Arrays.copyOf( values, length ); // Resize flat array if needed.
			}
			else if( length < cardinality ) cardinality = length; // In compressed strategy, truncate cardinality if length is reduced.
			
			if( !isFlatStrategy && oldLength < length && nulls.used * 64 >= flatStrategyThreshold ) switchToFlatStrategy(); // Check strategy switch on extension.
			else if( isFlatStrategy && length < oldLength && nulls.used * 64 < flatStrategyThreshold ) switchToCompressedStrategy(); // Check strategy switch on reduction.
			return this;
		}
		
		/**
		 * Sets the number of non-null elements (size) of the list.
		 * If the new size is less than the current size, the list is truncated, potentially removing non-null values.
		 * If the new size is greater, the list is extended with null values.
		 *
		 * @param size The new size (number of non-null elements) of the list.
		 * @return This {@code RW} instance (for method chaining).
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
		 * Adjusts the internal array lengths to match the current size, freeing any excess allocated memory.
		 * This is useful to optimize memory usage after a series of removals or size reductions.
		 *
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW fit() {
			length( size() );
			
			if( !isFlatStrategy ) return this;
			int i = nulls.cardinality();
			if( flatStrategyThreshold < i ) return this;
			
			switchToCompressedStrategy();
			values = Arrays.copyOf( values, i );
			return this;
		}
		
		/**
		 * Trims the list to remove any trailing null values, effectively resizing it to end at the last non-null element.
		 *
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW trim() {
			int last = nulls.last1() + 1; // Find index of last non-null element + 1.
			length( last ); // Set length to trim trailing nulls.
			if( isFlatStrategy && nulls.used * 64 < flatStrategyThreshold ) switchToCompressedStrategy(); // Potentially switch back to compressed if null density is low enough.
			return this;
		}
		
		/**
		 * Swaps the elements at two specified indices in the list.
		 * This operation correctly handles both null and non-null values and works efficiently
		 * regardless of whether the list is in flat or compressed strategy.
		 *
		 * @param index1 The index of the first element to swap.
		 * @param index2 The index of the second element to swap.
		 * @return This {@code RW} instance (for method chaining).
		 */
		public RW swap( int index1, int index2 ) {
			if( index1 == index2 ) return this; // No need to swap if indices are the same.
			
			boolean e1 = nulls.get( index1 ); // Get nullity status of element at index1.
			boolean e2 = nulls.get( index2 ); // Get nullity status of element at index2.
			
			if( isFlatStrategy )
				if( e1 && e2 ) { // Both elements are non-null in flat strategy.
					float value1 = values[ index1 ];
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
					
					float tmp = values[ i1 ];
					values[ i1 ] = values[ i2 ];
					values[ i2 ] = tmp; // Swap values in compressed array using ranks.
				}
				else {
					// One is null, one is non-null in compressed strategy.
					int exist = e1 ?
							i1 :
							i2;
					int empty = e1 ?
							i2 + 1 :
							i1 + 1;
					
					nulls.set( index1, e2 );// Swap nullity: index1 gets index2's state
					nulls.set( index2, e1 );// Swap nullity: index2 gets index1's state
					
					float v = values[ exist ]; // Get the value to be moved.
					
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
		 * Switches the storage strategy to <b>Flat (One-to-One) Strategy</b>.
		 * before call this method, nulls must be in the final, updated state.
		 */
		protected void switchToFlatStrategy() {
			if( size() == 0 )//the collection is empty
			{
				if( values.length == 0 ) values = new float[ 16 ];
				isFlatStrategy = true;
				return;
			}
			
			float[] flat = new float[ nulls.last1() + 1 ]; // Allocate flat array with some extra capacity for growth.
			for( int i = nulls.next1( 0 ), ii = 0; ii < cardinality; i = nulls.next1( i + 1 ) )
			     flat[ i ] = values[ ii++ ];
			
			this.values    = flat;
			isFlatStrategy = true;
		}
		
		
		/**
		 * Switches the storage strategy to <b>Compressed (Rank-Based) Strategy</b>.
		 * <p>
		 * If the list is already in compressed strategy, this method does nothing. Otherwise, it converts
		 * the internal data representation from flat to compressed. This involves creating a new array
		 * that is just large enough to hold the non-null values and copying only the non-null values
		 * from the flat array into this new compressed array.
		 */
		protected void switchToCompressedStrategy() {
			
			cardinality = nulls.cardinality(); // Count of non-null elements.
			for( int i = nulls.next1( 0 ), ii = 0; i != -1; i = nulls.next1( i + 1 ) )
				if( i != ii ) values[ ii++ ] = values[ i ];// Pack non-null values sequentially in the same array.
			
			isFlatStrategy = false;
		}
		
		/**
		 * Sets a value at a specific index in the list. This helper method is used by {@link RW}.
		 * It handles both null and non-null {@code value} inputs.
		 * <p>
		 * If {@code value} is null:
		 * - Sets the corresponding bit in {@code nulls} to {@code false} (marking it as null).
		 * - If the index was previously non-null, it removes the associated value
		 * <p>
		 *
		 * @param dst   The target {@code R} instance to modify.
		 * @param index The index at which to set the value.
		 * @param value The  value to set (can be null).
		 */
		protected static void set( R dst, int index,  Float     value ) {
			if( value == null ) {
				if( dst.nulls.last1() < index ) dst.nulls.set0( index ); // If index is beyond current length, extend and set as null.
				else if( dst.nulls.get( index ) ) { // If index was previously non-null, convert to null.
					if( !dst.isFlatStrategy )
						dst.cardinality = Array.resize( dst.values, dst.values, dst.nulls.rank( index ) - 1, dst.cardinality, -1 ); // Remove value from compressed array.
					dst.nulls.set0( index );
				}
			}
			else set( dst, index, value.floatValue     () ); // Delegate to primitive int setter for non-null value.
		}
		
		/**
		 * Sets a non-null value at a specific index in the list. This helper method is used by {@link RW}.
		 * <p>
		 * - If the index already contains a non-null value, it updates the value at that index.
		 * - If the index is currently null, it marks it as non-null in {@code nulls} and adds the
		 * given {@code value} to the {@code values} array at the appropriate position (either directly
		 * in flat strategy or at the correct rank in compressed strategy).
		 *
		 * @param dst   The destination {@code R} instance to modify.
		 * @param index The index at which to set the value.
		 * @param value The non-null value to set.
		 */
		protected static void set( RW dst, int index, float value ) {
			
			if( dst.isFlatStrategy ) {
				if( dst.values.length <= index )
					dst.values = Arrays.copyOf( dst.values, Math.max( index + 1, dst.values.length + dst.values.length / 2 ) ); // Ensure array capacity.
				dst.values[ index ] = ( float ) value; // Set value in flat array.
				dst.nulls.set1( index ); // Mark as non-null.
			}
			else // Index already non-null in compressed strategy.
				if( dst.nulls.get( index ) ) dst.values[ dst.nulls.rank( index ) - 1 ] = ( float ) value; // Update existing value.
				else { // Index was null, need to insert value in compressed strategy.
					int rank = dst.nulls.rank( index ); // Calculate index for insertion.
					int max  = Math.max( rank, dst.cardinality );
					
					if( dst.values.length <= max && dst.flatStrategyThreshold < dst.nulls.used * 64 ) {
						dst.switchToFlatStrategy();
						dst.nulls.set1( index ); // Mark as non-null.
						dst.values[ index ] = ( float ) value; // Set value in flat array.
					}
					else {
						dst.cardinality    = Array.resize( dst.values,
						                                   dst.values.length <= max ?
								                                   ( dst.values = new float[ 2 + max * 3 / 2 ] ) :
								                                   dst.values,
						                                   rank, dst.cardinality, 1 ); // Insert a slot for the new value.
						dst.values[ rank ] = ( float ) value; // Set the new value in compressed array.
						dst.nulls.set1( index ); // Mark as non-null.
					}
				}
		}
	}
}