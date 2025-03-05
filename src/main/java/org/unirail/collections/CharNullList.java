//MIT License
//
//Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//GitHub Repository: https://github.com/AdHoc-Protocol
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//1. The above copyright notice and this permission notice must be included in all
//   copies or substantial portions of the Software.
//
//2. Users of the Software must provide a clear acknowledgment in their user
//   documentation or other materials that their solution includes or is based on
//   this Software. This acknowledgment should be prominent and easily visible,
//   and can be formatted as follows:
//   "This product includes software developed by Chikirev Sirguy and the Unirail Group
//   (https://github.com/AdHoc-Protocol)."
//
//3. If you modify the Software and distribute it, you must include a prominent notice
//   stating that you have changed the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package org.unirail.collections;


import org.unirail.JsonWriter;

/**
 * Defines a contract for a list that stores nullable primitive integer values.
 * Implementations manage lists of integers that can contain nulls, providing efficient
 * methods for adding, removing, and accessing elements while tracking nullity.
 */
public interface CharNullList {
	
	
	/**
	 * Abstract base class providing a read-only implementation of the IntNullList.
	 * Uses a {@link BitList.RW} to track null values and an {@link IntList.RW} to store
	 * non-null integer values, serving as the foundation for both read-only and mutable operations.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		// BitList to track null values. 'true' indicates a non-null value at the corresponding index, 'false' indicates null.
		protected BitList.RW         nulls;
		// List to store actual integer values. Only non-null values are stored here, in a packed manner.
		protected CharList.RW values;
		
		/**
		 * Returns the total number of elements in the list, including both null and non-null values.
		 *
		 * @return the total length of the list
		 */
		public int length() {
			return nulls.length();
		}
		
		/**
		 * Returns the number of elements in the list, including both null and non-null values.
		 * This is equivalent to {@code length()} in this implementation.
		 *
		 * @return the size of the list
		 */
		public int size() {
			return nulls.size;
		}
		
		/**
		 * Checks if the list contains no elements.
		 *
		 * @return {@code true} if the list is empty, {@code false} otherwise
		 */
		public boolean isEmpty() {
			return size() < 1;
		}
		
		/**
		 * Checks if there is a non-null value at the specified index.
		 *
		 * @param index the index to check
		 * @return {@code true} if the value at the index is non-null, {@code false} if it is null
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index >= length()}
		 */
		public boolean hasValue( int index ) {
			return nulls.get( index );
		}
		
		/**
		 * Finds the next index with a non-null value, starting from the specified index (inclusive).
		 *
		 * @param index the starting index to search from
		 * @return the index of the next non-null value, or -1 if none exists
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index >= length()}
		 */
		
		public @Positive_OK int nextValueIndex( int index ) {
			return nulls.next1( index );
		}
		
		/**
		 * Finds the previous index with a non-null value, before the specified index (exclusive).
		 *
		 * @param index the starting index to search before
		 * @return the index of the previous non-null value, or -1 if none exists
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index > length()}
		 */
		public @Positive_OK int prevValueIndex( int index ) {
			return nulls.prev1( index );
		}
		
		/**
		 * Finds the next index with a null value, starting from the specified index (inclusive).
		 *
		 * @param index the starting index to search from
		 * @return the index of the next null value, or -1 if none exists
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index >= length()}
		 */
		public @Positive_OK int nextNullIndex( int index ) {
			return nulls.next0( index );
		}
		
		/**
		 * Finds the previous index with a null value, before the specified index (exclusive).
		 *
		 * @param index the starting index to search before
		 * @return the index of the previous null value, or -1 if none exists
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index > length()}
		 */
		public @Positive_OK int prevNullIndex( int index ) {
			return nulls.prev0( index );
		}
		
		/**
		 * Retrieves the non-null integer value at the specified index.
		 * <p>
		 * Assumes the value at the index is non-null; callers should use {@code hasValue(index)}
		 * to verify this beforehand. If the value is null, the behavior is implementation-dependent
		 * and may result in an exception or incorrect value.
		 *
		 * @param index the index of the element to retrieve
		 * @return the non-null integer value at the specified index
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index >= size()}
		 * @throws IllegalStateException     if the value at the index is null (implementation-dependent)
		 */
		public char get( @Positive_ONLY int index ) {
			return (char) values.get( nulls.rank( index ) - 1 );
		}
		
		/**
		 * Returns the index of the first occurrence of the specified non-null integer value.
		 *
		 * @param value the non-null integer value to search for
		 * @return the index of the first occurrence, or -1 if not found
		 */
		public int indexOf( char value ) {
			int i = values.indexOf( value );
			return i < 0 ?
					-1 :
					nulls.bit( i );
		}
		
		public boolean contains( char value ) { return indexOf( value ) != -1; }
		
		public boolean contains(  Character value ) {
			return value == null ?
					nextNullIndex( 0 ) < size() :
					indexOf( value ) != -1;
		}
		
		/**
		 * Returns the index of the last occurrence of the specified non-null integer value.
		 *
		 * @param value the non-null integer value to search for
		 * @return the index of the last occurrence, or -1 if not found
		 */
		public int lastIndexOf( char value ) {
			int i = values.lastIndexOf( value );
			return i < 0 ?
					i :
					nulls.bit( i );
		}
		
		/**
		 * Checks if this list is equal to another object.
		 *
		 * @param obj the object to compare with
		 * @return {@code true} if the objects are equal, {@code false} otherwise
		 */
		public boolean equals( Object obj ) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		/**
		 * Computes a hash code for this list based on its contents.
		 *
		 * @return the hash code value
		 */
		public int hashCode() {
			return Array.finalizeHash( Array.hash( Array.hash( nulls ), values ), size() );
		}
		
		/**
		 * Performs a detailed equality check with another {@code R} instance.
		 *
		 * @param other the other {@code R} instance to compare with
		 * @return {@code true} if the lists are equal, {@code false} otherwise
		 */
		public boolean equals( R other ) {
			return other != null && other.size() == size() && values.equals( other.values ) && nulls.equals( other.nulls );
		}
		
		/**
		 * Creates a deep copy of this list.
		 *
		 * @return a new {@code R} instance that is a clone of this list
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				dst.values = values.clone();
				dst.nulls  = nulls.clone();
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		/**
		 * Returns a JSON string representation of the list.
		 *
		 * @return a string in JSON format representing the list's contents
		 */
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Writes the list's contents in JSON format to the provided {@code JsonWriter}.
		 *
		 * @param json the {@code JsonWriter} to write the JSON data to
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			
			json.enterArray(); // Start JSON array.
			int size = size();
			if( 0 < size ) {
				json.preallocate( size * 10 ); // Pre-allocate buffer for potential performance optimization.
				for( int i = 0, ii; i < size; )
					if( ( ii = nextValueIndex( i ) ) == i ) json.value( get( i++ ) ); // If current index is non-null, write its integer value.
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
		 * Sets a value at the specified index, supporting null values (helper method for {@code RW}).
		 * <p>
		 * If the value is null, marks the index as null in {@code nulls}, removing any existing
		 * non-null value. If the value is non-null, delegates to the primitive {@code set} method.
		 *
		 * @param dst   the destination {@code R} instance to modify
		 * @param index the index at which to set the value
		 * @param value the value to set, which may be null
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index >= dst.length()}
		 */
		protected static void set( R dst, int index,  Character value ) {
			
			if( value == null ) {
				if( dst.size() <= index ) dst.nulls.set0( index );//resize - If index is beyond current size, just mark it as null in BitList (resizing BitList if needed).
				else if( dst.nulls.get( index ) ) { // If index is within size and was previously non-null:
					dst.values.remove( dst.nulls.rank( index ) - 1 ); // Remove the corresponding non-null value from values List.
					dst.nulls.set0( index );                    // Mark the index as null in nulls BitList.
				}
			}
			else set( dst, index, value. charValue     () ); // If value is not null, delegate to the primitive int version of set.
		}
		
		/**
		 * Sets a non-null integer value at the specified index (helper method for {@code RW}).
		 * <p>
		 * If the index already has a non-null value, updates it; otherwise, marks it as non-null
		 * and adds the value to the appropriate position in {@code values}.
		 *
		 * @param dst   the destination {@code R} instance to modify
		 * @param index the index at which to set the value
		 * @param value the non-null integer value to set
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index >= dst.length()}
		 */
		protected static void set( R dst, int index, char value ) {
			
			if( dst.nulls.get( index ) ) dst.values.set1( dst.nulls.rank( index ) - 1, value ); // If index was already non-null, update the value in values List.
			else {
				dst.nulls.set1( index ); // Mark the index as non-null in nulls BitList.
				dst.values.add1( dst.nulls.rank( index ) - 1, value ); // Add the non-null value to values List at the correct position based on rank.
			}
		}
	}
	
	/**
	 * Defines the public API for mutable {@code IntNullList} implementations.
	 */
	interface Interface {
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
		 * Retrieves the non-null integer value at the specified index.
		 *
		 * @param index the index to retrieve
		 * @return the non-null integer value
		 * @throws IndexOutOfBoundsException if {@code index < 0} or {@code index >= size()}
		 * @throws IllegalStateException     if the value is null (implementation-dependent)
		 */
		char get( @Positive_ONLY int index );
		
		/**
		 * Adds a boxed Integer value (can be null) to the end of the list.
		 *
		 * @param value The boxed Integer value to add (can be null).
		 * @return The {@code RW} instance for method chaining.
		 */
		RW add1(  Character value );
		
		/**
		 * Adds a boxed Integer value (can be null) at a specific index in the list.
		 *
		 * @param index The index at which the value should be inserted.
		 * @param value The boxed Integer value to add (can be null).
		 * @return The {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index > size()).
		 */
		RW add1( int index,  Character value );
		
		/**
		 * Adds a primitive integer value at a specific index in the list.
		 *
		 * @param index The index at which the value should be inserted.
		 * @param value The primitive integer value to add.
		 * @return The {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index > size()).
		 */
		RW add1( int index, char value );
		
		/**
		 * Sets the value at a specific index in the list, allowing a boxed Integer value (can be null).
		 *
		 * @param index The index where the value should be set.
		 * @param value The boxed Integer value to set (can be null).
		 * @return The {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		RW set1( int index,  Character value );
		
		/**
		 * Sets the value at a specific index in the list, using a primitive integer value.
		 *
		 * @param index The index where the value should be set.
		 * @param value The primitive integer value to set.
		 * @return The {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		RW set1( int index, char value );
	}
	
	/**
	 * Read-write implementation of IntNullList, extending R and implementing Interface.
	 */
	class RW extends R implements Interface {
		
		/**
		 * Constructs a list with the specified initial capacity.
		 *
		 * @param length The initial capacity.
		 */
		public RW( int length ) {
			super(); // Call superclass constructor.
			
			nulls  = new BitList.RW( length );      // Initialize BitList for null tracking with specified length.
			values = new CharList.RW( length ); // Initialize List for integer values with specified length.
		}
		
		/**
		 * Constructs a list with a boxed Integer default value and size.
		 *
		 * @param default_value The default value (can be null).
		 * @param size          The initial size.
		 */
		public RW(  Character default_value, int size ) {
			super(); // Call superclass constructor.
			
			// Initialize nulls BitList: true if default_value is not null, indicating non-null default values.
			nulls = new BitList.RW( default_value != null, size );
			
			// Initialize values List: use 0 as default if default_value is null, otherwise use its integer value.
			values = new CharList.RW( default_value == null ?
					                                 0 :
					                                 default_value. charValue     (), size );
			if( size < 0 ) values.clear(); // Clear values if size is negative (interpreting negative size as empty list).
		}
		
		/**
		 * Constructs a list with a primitive integer default value and size.
		 *
		 * @param default_value The default value.
		 * @param size          The initial size.
		 */
		public RW( char default_value, int size ) {
			super(); // Call superclass constructor.
			
			// Initialize nulls BitList: always false as primitive ints cannot be null.
			nulls = new BitList.RW( false, size );
			
			// Initialize values List with the given primitive integer default value.
			values = new CharList.RW( default_value, size );
		}
		
		/**
		 * Creates a deep copy of this list.
		 *
		 * @return A clone of this list.
		 */
		public RW clone() {
			return ( RW ) super.clone(); // Delegate to superclass clone method for creating a copy.
		}
		
		/**
		 * Removes the last element from the list.
		 *
		 * @return This instance.
		 * @throws IndexOutOfBoundsException if the list is empty.
		 */
		public RW remove() {
			return remove( size() - 1 ); // Delegate to remove(int index) to remove the last element.
		}
		
		/**
		 * Removes the element at the specified index.
		 *
		 * @param index The index to remove.
		 * @return This instance.
		 * @throws IndexOutOfBoundsException if index < 0 or index >= size().
		 */
		public RW remove( int index ) {
			if( size() < 1 || size() <= index ) return this; // Do nothing if list is empty or index is out of bounds.
			
			if( nulls.get( index ) ) values.remove( nulls.rank( index ) - 1 ); // If element is non-null, remove its value from values List.
			nulls.remove( index ); // Always remove the null-tracking bit at the given index from nulls BitList.
			return this; // For method chaining.
		}
		
		/**
		 * Sets a boxed Integer value at the end of the list.
		 *
		 * @param value The value (can be null).
		 * @return This instance.
		 */
		public RW set1(  Character value ) {
			set( this, size() - 1, value ); // Delegate to static set method to handle null values.
			return this; // For method chaining.
		}
		
		/**
		 * Sets a primitive integer value at the end of the list.
		 *
		 * @param value The primitive integer value to set.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set1( char value ) {
			set( this, size() - 1, value ); // Delegate to static set method.
			return this; // For method chaining.
		}
		
		/**
		 * Sets a boxed Integer value (can be null) at a specific index in the list.
		 *
		 * @param index The index where the value should be set.
		 * @param value The boxed Integer value to set (can be null).
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		public RW set1( int index,  Character value ) {
			set( this, index, value ); // Delegate to static set method to handle null values.
			return this; // For method chaining.
		}
		
		/**
		 * Sets a primitive integer value at a specific index in the list.
		 *
		 * @param index The index where the value should be set.
		 * @param value The primitive integer value to set.
		 * @return The {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index >= length()).
		 */
		public RW set1( int index, char value ) {
			set( this, index, value ); // Delegate to static set method.
			return this; // For method chaining.
		}
		
		/**
		 * Sets multiple boxed Integer values starting at the specified index.
		 *
		 * @param index  The starting index.
		 * @param values The values to set.
		 * @return This instance.
		 */
		public RW set( int index,  Character... values ) {
			for( int i = values.length; -1 < --i; ) // Iterate through values array backwards.
			     set( this, index + i, values[ i ] ); // Set each boxed Integer value using the static set method.
			return this; // For method chaining.
		}
		
		/**
		 * Sets multiple primitive integer values starting at the specified index.
		 *
		 * @param index  The starting index.
		 * @param values The values to set.
		 * @return This instance.
		 */
		public RW set( int index, char... values ) {
			return set( index, values, 0, values.length ); // Delegate to set(int index, int[] values, int src_index, int len) to set from an array range.
		}
		
		/**
		 * Sets a range of values from a source array of primitive integers into this list, starting at a specific index.
		 *
		 * @param index     The starting index in this list where the values should be set.
		 * @param values    The source array containing the primitive integer values to set.
		 * @param src_index The starting index in the source array to copy from.
		 * @param len       The number of elements to copy from the source array.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set( int index, char[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; ) // Iterate backwards for potential resizing efficiency.
			     set( this, index + i, ( char ) values[ src_index + i ] ); // Set each primitive integer value using the static set method.
			return this; // For method chaining.
		}
		
		/**
		 * Sets a range of values from a source array of boxed Integers (can be null) into this list, starting at a specific index.
		 *
		 * @param index     The starting index in this list where the values should be set.
		 * @param values    The source array containing the boxed Integer values (can be null) to set.
		 * @param src_index The starting index in the source array to copy from.
		 * @param len       The number of elements to copy from the source array.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set( int index,  Character[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; ) // Iterate backwards for potential resizing efficiency.
			     set( this, index + i, values[ src_index + i ] ); // Set each boxed Integer value using the static set method.
			return this; // For method chaining.
		}
		
		/**
		 * Adds a boxed Integer value (can be null) to the end of the list.
		 *
		 * @param value The boxed Integer value to add (can be null).
		 * @return This {@code RW} instance for method chaining.
		 */
		@Override
		public RW add1(  Character value ) {
			if( value == null ) nulls.add( false ); // Add null bit to nulls BitList.
			else add1( value. charValue     () ); // If value is not null, delegate to add1(int).
			return this; // For method chaining.
		}
		
		/**
		 * Adds a primitive integer value to the end of the list.
		 *
		 * @param value The value to add.
		 * @return This instance.
		 */
		public RW add1( char value ) {
			values.add1( value ); // Add non-null value to values List.
			nulls.add( true );  // Add non-null bit to nulls BitList.
			return this; // For method chaining.
		}
		
		/**
		 * Adds a boxed Integer value (can be null) at a specific index in the list.
		 *
		 * @param index The index at which the value should be inserted.
		 * @param value The boxed Integer value to add (can be null).
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index > size()).
		 */
		@Override
		public RW add1( int index,  Character value ) {
			if( value == null ) nulls.add( index, false ); // Add null bit to nulls BitList at the specified index.
			else add1( index, value. charValue     () ); // If value is not null, delegate to add1(int, int).
			return this; // For method chaining.
		}
		
		/**
		 * Adds a primitive integer value at a specific index in the list.
		 *
		 * @param index The index at which the value should be inserted.
		 * @param value The primitive integer value to add.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 or index > size()).
		 */
		@Override
		public RW add1( int index, char value ) {
			if( index < size() ) // If index is within the current size, insert in the middle.
			{
				nulls.add( index, true ); // Add non-null bit to nulls BitList at the specified index.
				values.add1( nulls.rank( index ) - 1, value ); // Add non-null value to values List at the correct position based on rank.
			}
			else set1( index, value ); // If index is at or beyond the current size, delegate to set1 to append or extend.
			return this; // For method chaining.
		}
		
		/**
		 * Adds multiple primitive integer values to the end of the list.
		 *
		 * @param items An array of primitive integer values to add.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW add( char... items ) {
			int size = size(); // Get current size before adding.
			set1( size() + items.length - 1, values.default_value ); // Extend list to accommodate new items (setting default value at the new end).
			return set( size, items ); // Delegate to set method to efficiently add the items.
		}
		
		/**
		 * Adds multiple boxed Integer values (can be null) to the end of the list.
		 *
		 * @param items An array of boxed Integer values (can be null) to add.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW add(  Character... items ) {
			int size = size(); // Get current size before adding.
			set1( size() + items.length - 1, values.default_value ); // Extend list (setting default value at the new end).
			return set( size, items ); // Delegate to set method to efficiently add the items.
		}
		
		/**
		 * Adds all elements from another {@code R} (List.R) list to the end of this list.
		 *
		 * @param src The source {@code R} list to add elements from.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW addAll( R src ) {
			
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
		public RW clear() {
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
		public RW length( int length ) {
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
		public RW size( int size ) {
			nulls.size( size ); // Set the size of the nulls BitList, which in turn adjusts the size of the List.
			values.size( nulls.cardinality() ); // Adjust the size of the values List based on the new cardinality of nulls BitList.
			return this; // For method chaining.
		}
		
		/**
		 * Adjusts the internal array lengths to be exactly the same as the current size of the list, potentially freeing up memory.
		 *
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW fit() {
			return length( size() ); // Call length method to resize to current size.
		}
		
		/**
		 * Swaps the elements at two specified indices in the list, maintaining nullity correctly.
		 *
		 * @param index1 the index of the first element
		 * @param index2 the index of the second element
		 * @return this {@code RW} instance for method chaining
		 * @throws IndexOutOfBoundsException if either index is out of range
		 */
		public RW swap( int index1, int index2 ) {
			
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
			
			char v = values.get( exist ); // Get the non-null value from values List.
			values.remove( exist );               // Remove the value from its original position.
			values.add1( empty, v );             // Add the value to the new position in values List (using 'empty' rank, though rank is not strictly necessary here).
			return this; // For method chaining.
		}
	}
}