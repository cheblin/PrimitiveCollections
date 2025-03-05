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
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.

package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;

/**
 * Interface defines a contract for a list that stores nullable Object values.
 * Implementations are expected to efficiently manage lists of Objects that can contain null values,
 * offering functionalities for adding, removing, and accessing elements while tracking nullity.
 *
 * @param <V> The type of elements in this list.
 */
public interface ObjectNullList< V >/*CLS*/ {
	
	
	/**
	 * {@code R} is an abstract base class providing a read-only implementation of the List interface.
	 * It manages the underlying {@link BitList.RW} for null tracking and  ObjectList.RW for Object values,
	 * providing core functionalities for read operations and serving as a base for mutable implementations.
	 *
	 * @param <V> The type of elements in this list.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		// BitList to track null values. 'true' indicates a non-null value at the corresponding index, 'false' indicates null.
		BitList.RW         nulls;
		// List to store actual Object values. Only non-null values are stored here, in a packed manner.
		ObjectList.RW< V > values;
		
		/**
		 * Utility for comparing and hashing elements of type V.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		
		/**
		 * Constructor for {@code R} when the class of element type {@code V} is available.
		 *
		 * @param clazzV The class object representing the element type {@code V}.
		 */
		protected R( Class< V > clazzV ) {
			equal_hash_V = Array.get( clazzV );
		}
		
		/**
		 * Constructor for {@code R} when an {@link Array.EqualHashOf} instance is already available.
		 *
		 * @param equal_hash_V The {@link Array.EqualHashOf} instance for element type {@code V}.
		 */
		public R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		/**
		 * Copies a range of elements from this list into a destination array.
		 *
		 * @param index The starting index in this list to begin copying from.
		 * @param len   The number of elements to copy.
		 * @param dst   The destination array. If {@code null}, a new array of size {@code len} is created.
		 * @return The destination array containing the copied elements, or {@code null} if the list is empty.
		 * @throws ArrayIndexOutOfBoundsException if {@code index} or {@code len} are invalid.
		 */
		public V[] toArray( int index, int len, V[] dst ) {
			if( size() == 0 ) return null;
			if( dst == null || dst.length < len ) return Arrays.copyOfRange( values.values, index, index + len );
			System.arraycopy( values.values, index, dst, 0, len );
			return dst;
		}
		
		/**
		 * Checks if this list contains all elements of the specified list.
		 *
		 * @param src The list to check for containment.
		 * @return {@code true} if this list contains all elements of {@code src}, {@code false} otherwise.
		 */
		public boolean containsAll( ObjectList.R< V > src ) {
			
			for( int i = 0, s = src.size(); i < s; i++ )
				if( indexOf( src.get( i ) ) == -1 ) return false;
			return true;
		}
		
		
		/**
		 * Returns the total capacity (length) of the list, including slots that may or may not contain values (including nulls).
		 * This is determined by the length of the underlying {@link BitList.RW} which tracks null values.
		 *
		 * @return The length (capacity) of the list.
		 */
		public int length() {
			return nulls.length();
		}
		
		/**
		 * Returns the number of non-null elements currently in the list. This is equivalent to the number of 'true' bits in the {@link BitList.RW}.
		 *
		 * @return The number of non-null elements in the list.
		 */
		public int size() {
			return nulls.cardinality();
		}
		
		/**
		 * Checks if the list is empty, meaning it contains no non-null elements.
		 *
		 * @return {@code true} if the list is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() {
			return size() < 1;
		}
		
		/**
		 * Checks if a value (can be null or non-null) exists at the specified index.
		 * This method checks if an index is considered 'occupied' in terms of list structure, regardless of whether the value is null or not-null.
		 * More precisely, it checks if the {@link BitList.RW} has a bit set at the given index, indicating a value was once set at this position.
		 *
		 * @param index The index to check.
		 * @return {@code true} if a value exists at the specified index (can be null or non-null), {@code false} otherwise.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		public boolean hasValue( int index ) {
			return nulls.get( index );
		}
		
		/**
		 * Finds the index of the next non-null value starting from the given index (inclusive).
		 *
		 * @param index The starting index for the search.
		 * @return The index of the next non-null value, or -1 if no non-null value is found from the given index onwards.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		public int nextValueIndex( int index ) {
			return nulls.next1( index );
		}
		
		/**
		 * Finds the index of the previous non-null value before the given index (exclusive).
		 *
		 * @param index The starting index for the backward search.
		 * @return The index of the previous non-null value, or -1 if no non-null value is found before the given index.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index > length()).
		 */
		public int prevValueIndex( int index ) {
			return nulls.prev1( index );
		}
		
		/**
		 * Finds the index of the next null value starting from the given index (inclusive).
		 *
		 * @param index The starting index for the search.
		 * @return The index of the next null value, or -1 if no null value is found from the given index onwards.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		public int nextNullIndex( int index ) {
			return nulls.next0( index );
		}
		
		/**
		 * Finds the index of the previous null value before the given index (exclusive).
		 *
		 * @param index The starting index for the backward search.
		 * @return The index of the previous null value, or -1 if no null value is found before the given index.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index > length()).
		 */
		public int prevNullIndex( int index ) {
			return nulls.prev0( index );
		}
		
		/**
		 * Retrieves the non-null Object value at the specified index.
		 * Note: This method is optimized for retrieving non-null values. For potentially null values, consider using a method that directly checks for null before retrieval.
		 *
		 * @param index The index of the element to retrieve.
		 * @return The non-null Object value at the specified index.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= size()).
		 * @throws NullPointerException      if the value at the specified index is null.
		 */
		public V get( int index ) {
			return values.get( nulls.rank( index ) - 1 );
		}
		
		/**
		 * Finds the first occurrence of the specified non-null Object value in the list and returns its index.
		 *
		 * @param value The non-null Object value to search for.
		 * @return The index of the first occurrence of the value, or -1 if not found.
		 */
		public int indexOf( V value ) {
			int i = values.indexOf( value );
			return i < 0 ?
					i :
					nulls.bit( i );
		}
		
		/**
		 * Finds the last occurrence of the specified non-null Object value in the list and returns its index.
		 *
		 * @param value The non-null Object value to search for.
		 * @return The index of the last occurrence of the value, or -1 if not found.
		 */
		public int lastIndexOf( V value ) {
			int i = values.lastIndexOf( value );
			return i < 0 ?
					i :
					nulls.bit( i );
		}
		
		/**
		 * Overrides {@link Object#equals(Object)} to provide equality comparison for {@code List} instances.
		 * Checks for null, class type, and then delegates to {@link #equals(R)} for detailed comparison.
		 *
		 * @param obj The object to compare to.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		/**
		 * Computes the hash code for this list. The hash code is based on both the null value tracking (nulls BitList) and the actual Object values (values List).
		 *
		 * @return The hash code value for this list.
		 */
		@Override
		public int hashCode() {
			return Array.finalizeHash( Array.hash( Array.hash( nulls ), values ), size() );
		}
		
		/**
		 * Performs a detailed equality comparison with another {@code R} instance.
		 * Checks if both lists have the same size, the same Object values in the same order, and the same null value placements.
		 *
		 * @param other The other {@code R} instance to compare to.
		 * @return {@code true} if the lists are equal, {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) {
			return other != null && other.size() == size() && values.equals( other.values ) && nulls.equals( other.nulls );
		}
		
		/**
		 * Creates and returns a deep copy of this {@code R} instance.
		 * Clones both the values List and the nulls BitList to ensure that modifications to the clone do not affect the original list.
		 *
		 * @return A clone of this list object.
		 */
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				dst.values = values.clone(); // Deep clone the values List.
				dst.nulls  = nulls.clone();  // Deep clone the nulls BitList.
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		/**
		 * Returns a string representation of the list in JSON format.
		 *
		 * @return JSON string representation of the list.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Writes the list's content in JSON format to the provided {@link JsonWriter}.
		 * Iterates through the list and writes each element as a JSON value, outputting "null" for null values and the Object value for non-null elements.
		 *
		 * @param json The {@link JsonWriter} to write the JSON output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			
			json.enterArray(); // Start JSON array.
			int size = size();
			if( 0 < size ) {
				json.preallocate( size * 10 ); // Pre-allocate buffer for potential performance optimization.
				for( int i = 0, ii; i < size; )
					if( ( ii = nextValueIndex( i ) ) == i ) json.value( get( i++ ) ); // If current index is non-null, write its Object value.
					else if( ii == -1 || size <= ii ) { // If no more non-null values or next non-null index is out of bounds.
						while( i++ < size ) json.value(); // Write 'null' for all remaining elements (which are null).
						break; // Exit loop as all remaining elements are null.
					}
					else
						for( ; i < ii; i++ ) json.value(); // Write 'null' for all null elements until the next non-null value index.
			}
			json.exitArray(); // End JSON array.
		}
		
		/**
		 * Sets a value at a specific index, handling null values by updating both the nulls BitList and values List accordingly.
		 * This is a protected static helper method to be used by concrete RW implementations.
		 *
		 * @param dst   The destination {@code R} instance to set the value in.
		 * @param index The index where the value should be set.
		 * @param value The value (can be null) to set.
		 * @param <V>   The type of elements in this list.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		protected static < V > void set( R< V > dst, int index, V value ) {
			
			if( value == null ) {
				if( dst.size() <= index ) dst.nulls.set0( index );//resize - If index is beyond current size, just mark it as null in BitList (resizing BitList if needed).
				else if( dst.nulls.get( index ) ) { // If index is within size and was previously non-null:
					dst.values.remove( dst.nulls.rank( index ) - 1 ); // Remove the corresponding non-null value from values List.
					dst.nulls.set0( index );                    // Mark the index as null in nulls BitList.
				}
			}
			else {
				if( dst.nulls.get( index ) ) dst.values.set1( dst.nulls.rank( index ) - 1, value ); // If index was already non-null, update the value in values List.
				else {
					dst.nulls.set1( index ); // Mark the index as non-null in nulls BitList.
					dst.values.add1( dst.nulls.rank( index ) - 1, value ); // Add the non-null value to values List at the correct position based on rank.
				}
			}
		}
	}
	
	/**
	 * {@code Interface} defines the public API for mutable List implementations.
	 * It extends the read-only {@code Interface} and adds methods for modifying the list, such as adding and setting elements, handling nullable Object values.
	 *
	 * @param <V> The type of elements in this list.
	 */
	interface Interface< V > {
		/**
		 * Returns the number of non-null elements in the list.
		 *
		 * @return The size of the list.
		 */
		int size();
		
		/**
		 * Checks if a value (can be null or non-null) exists at the specified index.
		 *
		 * @param index The index to check.
		 * @return {@code true} if a value exists at the specified index, {@code false} otherwise.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		boolean hasValue( int index );
		
		/**
		 * Retrieves the non-null Object value at the specified index.
		 *
		 * @param index The index of the element to retrieve.
		 * @return The non-null Object value at the specified index.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= size()).
		 * @throws NullPointerException      if the value at the specified index is null.
		 */
		V get( int index );
		
		/**
		 * Adds a Object value (can be null) to the end of the list.
		 *
		 * @param value The Object value to add (can be null).
		 * @return The {@code RW} instance for method chaining.
		 */
		RW< V > add1( V value );
		
		/**
		 * Adds a Object value (can be null) at a specific index in the list.
		 *
		 * @param index The index at which the value should be inserted.
		 * @param value The Object value to add (can be null).
		 * @return The {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index > size()).
		 */
		RW< V > add1( int index, V value );
		
		
		/**
		 * Sets the value at a specific index in the list, allowing a Object value (can be null).
		 *
		 * @param index The index where the value should be set.
		 * @param value The Object value to set (can be null).
		 * @return The {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		RW< V > set1( int index, V value );
	}
	
	/**
	 * {@code RW} class provides a read-write implementation of the List interface.
	 * It extends the abstract {@code R} class, inheriting read-only functionalities, and implements the {@code Interface} to provide methods for modifying the list, handling nullable Object values.
	 *
	 * @param <V> The type of elements in this list.
	 */
	class RW< V > extends R< V > implements Interface< V > {
		
		/**
		 * Default value used when initializing or extending the list.
		 */
		public final V default_value;
		
		/**
		 * Constructs an empty {@code RW} list with a specified initial capacity.
		 *
		 * @param clazz  The class of the element type V.
		 * @param length The initial capacity of the list.
		 */
		@SuppressWarnings( "unchecked" )
		public RW( Class< V > clazz, int length ) { this( Array.get( clazz ), length ); }
		
		/**
		 * Constructs an empty {@code RW} list with a specified initial capacity and {@link Array.EqualHashOf}.
		 *
		 * @param equal_hash_V The EqualHashOf instance for element type V.
		 * @param length       The initial capacity of the list.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int length ) {
			super( equal_hash_V );
			default_value = null;
			
			nulls  = new BitList.RW( length );      // Initialize BitList for null tracking with specified length.
			values = new ObjectList.RW<>( equal_hash_V, length ); // Initialize List for Object values with specified length.
		}
		
		/**
		 * Constructs a {@code RW} list with a specified default value and initial size.
		 *
		 * @param clazz         The Class object representing the type of elements.
		 * @param default_value The default value to initialize elements with.
		 * @param size          The initial size of the array.
		 */
		public RW( Class< V > clazz, V default_value, int size ) { this( Array.get( clazz ), default_value, size ); }
		
		/**
		 * Constructs a {@code RW} list with a specified default value, initial size, and {@link Array.EqualHashOf}.
		 *
		 * @param equal_hash_V  The EqualHashOf object for type V.
		 * @param default_value The default value to initialize elements with.
		 * @param size          The initial size of the array.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, V default_value, int size ) {
			super( equal_hash_V );
			this.default_value = default_value;
			// Initialize nulls BitList: true if default_value is not null, indicating non-null default values.
			nulls = new BitList.RW( default_value != null, size );
			
			// Initialize values List: use null as default if default_value is null, otherwise use the given default value.
			values = new ObjectList.RW<>( equal_hash_V, default_value, size );
			if( size < 0 ) values.clear(); // Clear values if size is negative (interpreting negative size as empty list).
		}
		
		
		/**
		 * Creates and returns a deep copy of this {@code RW} list.
		 *
		 * @return A clone of this list object.
		 */
		@Override
		public RW< V > clone() {
			return ( RW< V > ) super.clone(); // Delegate to superclass clone method for creating a copy.
		}
		
		/**
		 * Removes the last element from the list, decreasing its size by one.
		 *
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the list is empty.
		 */
		public RW< V > remove() {
			return remove( size() - 1 ); // Delegate to remove(int index) to remove the last element.
		}
		
		/**
		 * Removes the element at the specified index in the list.
		 *
		 * @param index The index of the element to remove.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= size()).
		 */
		public RW< V > remove( int index ) {
			if( size() < 1 || size() <= index ) return this; // Do nothing if list is empty or index is out of bounds.
			
			if( nulls.get( index ) ) values.remove( nulls.rank( index ) - 1 ); // If element is non-null, remove its value from values List.
			nulls.remove( index ); // Always remove the null-tracking bit at the given index from nulls BitList.
			return this; // For method chaining.
		}
		
		
		/**
		 * Sets a Object value (can be null) at the end of the list.
		 *
		 * @param value The Object value to set (can be null).
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW< V > set1( V value ) {
			R.set( this, size() - 1, value ); // Delegate to static set method to handle null values.
			return this; // For method chaining.
		}
		
		
		/**
		 * Sets a Object value (can be null) at a specific index in the list.
		 *
		 * @param index The index where the value should be set.
		 * @param value The Object value to set (can be null).
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		@Override
		public RW< V > set1( int index, V value ) {
			R.set( this, index, value ); // Delegate to static set method to handle null values.
			return this; // For method chaining.
		}
		
		/**
		 * Sets multiple Object values (can be null) in the list, starting at a specific index.
		 *
		 * @param index  The starting index in this list where the values should be set.
		 * @param values An array of Object values (can be null) to set.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 * @throws IndexOutOfBoundsException if the range extends beyond the list's capacity after potential resizing.
		 */
		@SafeVarargs
		public final RW< V > set( int index, V... values ) {
			for( int i = values.length; -1 < --i; ) // Iterate through values array backwards.
			     R.set( this, index + i, values[ i ] ); // Set each Object value using the static set method.
			return this; // For method chaining.
		}
		
		
		/**
		 * Sets a range of values from a source array of Objects (can be null) into this list, starting at a specific index.
		 *
		 * @param index     The starting index in this list where the values should be set.
		 * @param values    The source array containing the Object values (can be null) to set.
		 * @param src_index The starting index in the source array to copy from.
		 * @param len       The number of elements to copy from the source array.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 * @throws IndexOutOfBoundsException if {@code src_index} or {@code len} are invalid for the source array.
		 * @throws IndexOutOfBoundsException if the range extends beyond the list's capacity after potential resizing.
		 */
		public RW< V > set( int index, V[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; ) // Iterate backwards for potential resizing efficiency.
			     R.set( this, index + i, values[ src_index + i ] ); // Set each Object value using the static set method.
			return this; // For method chaining.
		}
		
		/**
		 * Adds a Object value (can be null) to the end of the list.
		 *
		 * @param value The Object value to add (can be null).
		 * @return This {@code RW} instance for method chaining.
		 */
		@Override
		public RW< V > add1( V value ) {
			if( value == null ) nulls.add( false ); // Add null bit to nulls BitList.
			else {
				values.add1( value ); // Add non-null value to values List.
				nulls.add( true );  // Add non-null bit to nulls BitList.
			}
			return this; // For method chaining.
		}
		
		
		/**
		 * Adds a Object value (can be null) at a specific index in the list.
		 *
		 * @param index The index at which the value should be inserted.
		 * @param value The Object value to add (can be null).
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index > size()).
		 */
		@Override
		public RW< V > add1( int index, V value ) {
			if( value == null ) nulls.add( index, false ); // Add null bit to nulls BitList at the specified index.
			else {
				if( index < size() ) // If index is within the current size, insert in the middle.
				{
					nulls.add( index, true ); // Add non-null bit to nulls BitList at the specified index.
					values.add1( nulls.rank( index ) - 1, value ); // Add non-null value to values List at the correct position based on rank.
				}
				else set1( index, value ); // If index is at or beyond the current size, delegate to set1 to append or extend.
			}
			return this; // For method chaining.
		}
		
		/**
		 * Adds multiple Object values (can be null) to the end of the list.
		 *
		 * @param items An array of Object values (can be null) to add.
		 * @return This {@code RW} instance for method chaining.
		 */
		@SafeVarargs
		public final RW< V > add( V... items ) {
			int size = size(); // Get current size before adding.
			set1( size() + items.length - 1, values.default_value ); // Extend list to accommodate new items (setting default value at the new end).
			return set( size, items ); // Delegate to set method to efficiently add the items.
		}
		
		/**
		 * Adds all elements from another {@code R} (ObjectList.R) list to the end of this list.
		 *
		 * @param src The source {@code R} list to add elements from.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW< V > addAll( R< V > src ) {
			
			for( int i = 0, s = src.size(); i < s; i++ ) // Iterate through the source list.
				if( src.hasValue( i ) ) add1( src.get( i ) ); // If source element is non-null, add its value.
				else nulls.add( false ); // If source element is null, add a null placeholder.
			return this; // For method chaining.
		}
		
		/**
		 * Clears all elements from this list, making it empty.
		 *
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW< V > clear() {
			values.clear(); // Clear the values List.
			nulls.clear();  // Clear the nulls BitList.
			return this; // For method chaining.
		}
		
		/**
		 * Sets the total capacity (length) of the list, potentially reallocating internal arrays if needed.
		 *
		 * @param length The new length (capacity) of the list.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW< V > length( int length ) {
			nulls.length( length );  // Set the length of the nulls BitList.
			values.length( length ); // Set the length of the values List.
			values.size( nulls.cardinality() ); // Adjust the size of the values List to match non-null count in nulls BitList.
			return this; // For method chaining.
		}
		
		/**
		 * Sets the number of non-null elements (size) of the list.
		 * If the new size is larger than the current size, new slots are added (initialized to default values).
		 * If the new size is smaller, elements beyond the new size are truncated.
		 *
		 * @param size The new size (number of non-null elements) of the list.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW< V > size( int size ) {
			nulls.size( size ); // Set the size of the nulls BitList, which in turn adjusts the size of the List.
			values.size( nulls.cardinality() ); // Adjust the size of the values List based on the new cardinality of nulls BitList.
			return this; // For method chaining.
		}
		
		/**
		 * Adjusts the internal array lengths to be exactly the same as the current size of the list, potentially freeing up memory.
		 *
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW< V > fit() {
			return length( size() ); // Call length method to resize to current size.
		}
		
		/**
		 * Swaps the elements at two specified indices in the list, maintaining nullity correctly.
		 *
		 * @param index1 The index of the first element to swap.
		 * @param index2 The index of the second element to swap.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if either index is out of range (index < 0 or index >= size()).
		 */
		public RW< V > swap( int index1, int index2 ) {
			
			int exist, empty;
			if( nulls.get( index1 ) ) // If element at index1 is non-null.
				if( nulls.get( index2 ) ) // If element at index2 is also non-null.
				{
					values.swap( nulls.rank( index1 ) - 1, nulls.rank( index2 ) - 1 ); // Swap non-null values in values List using their ranks.
					return this; // For method chaining.
				}
				else { // index1 is non-null, index2 is null.
					exist = nulls.rank( index1 ) - 1; // Rank of non-null element at index1.
					empty = nulls.rank( index2 );      // Rank of null element at index2 (rank is not really relevant for nulls, but using it for consistency).
					nulls.set0( index1 );             // Set index1 to null in nulls BitList.
					nulls.set1( index2 );             // Set index2 to non-null in nulls BitList.
				}
			else if( nulls.get( index2 ) ) // If index1 is null and index2 is non-null.
			{
				exist = nulls.rank( index2 ) - 1; // Rank of non-null element at index2.
				empty = nulls.rank( index1 );      // Rank of null element at index1.
				
				nulls.set1( index1 );             // Set index1 to non-null in nulls BitList.
				nulls.set0( index2 );             // Set index2 to null in nulls BitList.
			}
			else return this; // If both indices are null, no swap needed, return.
			
			V v = values.get( exist ); // Get the non-null value from values List.
			values.remove( exist );               // Remove the value from its original position.
			values.add1( empty, v );             // Add the value to the new position in values List (using 'empty' rank, though rank is not strictly necessary here).
			return this; // For method chaining.
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
	static < V > Array.EqualHashOf< RW< V > > equal_hash() { return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; }
}