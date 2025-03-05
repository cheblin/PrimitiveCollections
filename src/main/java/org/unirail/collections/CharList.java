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

import java.util.Arrays;

/**
 * {@code List} is an interface that defines the contract for a dynamic list of integers.
 * It provides a base for creating various integer list implementations, including resizable and potentially specialized lists.
 * <p>
 * Implementations of this interface offer methods for adding, removing, accessing, and manipulating integer elements within the list.
 * The interface is designed to be generic with respect to the underlying integer type used for storage and external representation.
 */
public interface CharList {
	
	
	/**
	 * {@code R} is an abstract base class that provides a common implementation for {@code List}.
	 * It handles the underlying data storage, basic list operations, and common functionalities like cloning, equality checks, and JSON serialization.
	 * <p>
	 * This class is intended to be extended by concrete implementations of {@code List}, such as {@link RW}, to provide specific list behaviors.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		/**
		 * The default value used when the list is initialized with a default value or when expanding the list.
		 */
		public final char default_value;
		
		/**
		 * Constructor for the abstract base class {@code R}.
		 *
		 * @param default_value The default integer value to be used for uninitialized elements or when extending the list.
		 */
		protected R( char default_value ) { this.default_value = default_value; }
		
		/**
		 * The internal array used to store the integer values of the list.
		 * It is dynamically resized as needed to accommodate more elements.
		 */
		char[] values = Array.EqualHashOf.chars     .O;
		
		/**
		 * Returns a direct reference to the internal integer array used by the list.
		 * <p>
		 * Note: Modifying this array directly can lead to unexpected behavior and is generally discouraged.
		 *
		 * @return The internal integer array.
		 */
		public char[] array() { return values; }
		
		/**
		 * The current number of elements in the list.
		 * This is always less than or equal to the length of the internal {@code values} array.
		 */
		int size = 0;
		
		/**
		 * Returns the number of elements currently in the list.
		 *
		 * @return The size of the list.
		 */
		public int size() { return size; }
		
		/**
		 * Checks if the list is empty, containing no elements.
		 *
		 * @return {@code true} if the list is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size == 0; }
		
		/**
		 * Checks if the list contains the specified integer value.
		 *
		 * @param value The integer value to search for.
		 * @return {@code true} if the list contains the value, {@code false} otherwise.
		 */
		public boolean contains( char value ) { return -1 < indexOf( value ); }
		
		
		/**
		 * Copies a range of elements from the list into a new integer array.
		 *
		 * @param index The starting index in the destination array where elements should be copied.
		 * @param len   The number of elements to copy.
		 * @param dst   The destination integer array. If {@code null}, a new array of length {@code len} will be created.
		 * @return The destination array containing the copied elements, or {@code null} if the list is empty.
		 */
		public char[] toArray( int index, int len, char[] dst ) {
			if( size == 0 ) return null;
			if( dst == null ) dst = new char[ len ];
			for( int i = 0; i < len; i++ ) dst[ index + i ] = (char) values[ i ];
			
			return dst;
		}
		
		/**
		 * Checks if this list contains all of the elements in the specified list.
		 *
		 * @param src The list to be checked for containment in this list.
		 * @return {@code true} if this list contains all elements of the source list, {@code false} otherwise.
		 */
		public boolean containsAll( R src ) {
			for( int i = src.size(); -1 < --i; )
				if( !contains( src.get( i ) ) ) return false;
			
			return true;
		}
		
		/**
		 * Retrieves the integer value at the specified index in the list.
		 *
		 * @param index The index of the element to retrieve (0-based).
		 * @return The integer value at the specified index.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
		 */
		public char get( int index ) { return  (char) values[ index ]; }
		
		public char get() { return  (char) values[ size - 1 ]; }
		
		/**
		 * Copies a specified range of elements from this list to a destination integer array.
		 *
		 * @param dst       The destination integer array.
		 * @param dst_index The starting index in the destination array for copying.
		 * @param src_index The starting index in this list for copying.
		 * @param len       The maximum number of elements to copy.
		 * @return The number of elements actually copied, which might be less than {@code len} if the source or destination boundaries are reached.
		 */
		public int get(char[] dst, int dst_index, int src_index, int len ) {
			len = Math.min( Math.min( size - src_index, len ), dst.length - dst_index );
			if( len < 1 ) return 0;
			
			for( int i = 0; i < len; i++ )
			     dst[ dst_index++ ] = (char) values[ src_index++ ];
			
			return len;
		}
		
		/**
		 * Returns the index of the first occurrence of the specified integer value in the list,
		 * or -1 if the list does not contain the value.
		 *
		 * @param value The integer value to search for.
		 * @return The index of the first occurrence of the value, or -1 if not found.
		 */
		public int indexOf( char value ) {
			for( int i = 0; i < size; i++ )
				if( values[ i ] == value ) return i;
			return -1;
		}
		
		/**
		 * Returns the index of the last occurrence of the specified integer value in the list,
		 * or -1 if the list does not contain the value.
		 *
		 * @param value The integer value to search for.
		 * @return The index of the last occurrence of the value, or -1 if not found.
		 */
		public int lastIndexOf( char value ) {
			for( int i = size - 1; -1 < i; i-- )
				if( values[ i ] == value ) return i;
			return -1;
		}
		
		/**
		 * Compares this list to the specified object for equality.
		 * Two {@code List.R} instances are considered equal if they are of the same class and contain the same elements in the same order.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		/**
		 * Compares this list to another {@code List.R} instance for equality.
		 *
		 * @param other The {@code List.R} instance to compare with.
		 * @return {@code true} if the lists are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == null || other.size != size ) return false;
			
			for( int i = size(); -1 < --i; )
				if( values[ i ] != other.values[ i ] ) return false;
			return true;
		}
		
		/**
		 * Computes the hash code for this list. The hash code is based on the elements and their order in the list.
		 * It uses an efficient algorithm to calculate the hash, especially for lists with sequential or repeating values.
		 *
		 * @return The hash code for this list.
		 */
		public final int hashCode() {
			switch( size ) {
				case 0:
					return Array.finalizeHash( seed, 0 );
				case 1:
					return Array.finalizeHash( Array.mix( seed, Array.hash( values[ 0 ] ) ), 1 );
			}
			
			final int initial   = Array.hash( values[ 0 ] );
			int       prev      = Array.hash( values[ 1 ] );
			final int rangeDiff = prev - initial;
			int       h         = Array.mix( seed, initial );
			
			for( int i = 2; i < size; ++i ) {
				h = Array.mix( h, prev );
				final int hash = Array.hash( values[ i ] );
				if( rangeDiff != hash - prev ) {
					for( h = Array.mix( h, hash ), ++i; i < size; ++i )
					     h = Array.mix( h, Array.hash( values[ i ] ) );
					
					return Array.finalizeHash( h, size );
				}
				prev = hash;
			}
			
			return Array.avalanche( Array.mix( Array.mix( h, rangeDiff ), prev ) );
		}
		
		/**
		 * Seed value used in hash code calculation. Initialized with the hash code of the {@code R} class.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Creates and returns a shallow copy of this {@code List.R} instance.
		 * The copy includes a copy of the internal {@code values} array.
		 *
		 * @return A clone of this list.
		 */
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				
				dst.values = values.clone();
				dst.size   = size;
				
				return dst;
				
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
			
		}
		
		/**
		 * Returns a string representation of this list, which is a JSON array representation of the list's elements.
		 *
		 * @return A JSON string representation of the list.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the JSON array representation of this list to the provided {@link JsonWriter}.
		 *
		 * @param json The {@link JsonWriter} to write the JSON output to.
		 */
		@Override public void toJSON( JsonWriter json ) {
			json.enterArray();
			int size = size();
			if( 0 < size ) {
				json.preallocate( size * 10 );
				for( int i = 0; i < size; i++ ) json.value( get( i ) );
			}
			json.exitArray();
		}
	}
	
	/**
	 * {@code Interface} defines the public API for interacting with {@code List} implementations.
	 * It specifies the methods that concrete implementations must provide for basic list operations.
	 * <p>
	 * This interface is implemented by classes like {@link RW}.
	 */
	interface Interface {
		/**
		 * Returns the number of elements in the list.
		 *
		 * @return The size of the list.
		 */
		int size();
		
		/**
		 * Retrieves the integer value at the specified index in the list.
		 *
		 * @param index The index of the element to retrieve (0-based).
		 * @return The integer value at the specified index.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
		 */
		char get( int index );
		
		/**
		 * Adds a new integer value to the end of the list.
		 *
		 * @param value The integer value to add.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 */
		RW add1( char value );
		
		/**
		 * Sets the integer value at the specified index in the list.
		 *
		 * @param index The index where the value should be set (0-based).
		 * @param value The new integer value to set.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
		 */
		RW set1( int index, char value );
	}
	
	/**
	 * {@code RW} (Read-Write) is a concrete implementation of {@code List} that allows modification
	 * of the list after creation. It extends {@link R} to inherit the core list functionality and implements
	 * the {@link Interface} to provide the public API.
	 * <p>
	 * This class provides constructors to customize the initial capacity and default value for the list.
	 */
	class RW extends R implements Interface {
		
		/**
		 * Constructor for {@code RW} that initializes the list with a specified initial capacity (length).
		 * The list will be initially filled with default integer values (typically 0).
		 *
		 * @param length The initial capacity of the list (number of elements the internal array can hold).
		 */
		public RW( int length ) {
			super( ( char ) 0 );
			// Create a new array if length > 0, otherwise use an empty array
			values = 0 < length ?
					new char[ length ] :
					Array.EqualHashOf.chars     .O;
		}
		
		/**
		 * Constructor for {@code RW} that initializes the list with a default value and a specified size.
		 * If size is positive, the list will be initialized with the given default value. If size is negative, the absolute value is used for initial capacity but elements are not initialized.
		 *
		 * @param default_value The default integer value to initialize elements with.
		 * @param size          The initial size of the list. If negative, the absolute value is used as capacity, but no initialization is performed.
		 */
		public RW( char default_value, int size ) {
			super( default_value );
			
			values = size == 0 ?
					Array.EqualHashOf.chars     .O :
					new char[ this.size = size < 0 ?
							-size :
							size ];// Use absolute value of size
			
			if( size < 1 || default_value == 0 )
				return;// Skip initialization if size was negative or if default_value is 0
			while( -1 < --size ) values[ size ] = ( char ) default_value;// Initialize all elements with the default value
		}
		
		/**
		 * Adds an integer value to the end of the list.
		 *
		 * @param value The integer value to add.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 */
		public RW add1( char value ) { return add1( size, value ); }
		
		/**
		 * Adds an integer value at the specified index in the list, shifting subsequent elements to the right.
		 *
		 * @param index The index at which the value should be inserted (0-based).
		 * @param value The integer value to add.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index > size()}).
		 */
		public RW add1( int index, char value ) {
			
			int max = Math.max( index, size + 1 );
			
			size            = Array.resize( values, values.length <= max ?
					values = new char[ max + max / 2 ] :
					values, index, size, 1 );
			values[ index ] = ( char ) value;
			return this;
		}
		
		/**
		 * Adds multiple integer values from an array to the end of the list.
		 *
		 * @param src The array of integer values to add.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 */
		public RW add( char... src ) { return add( size(), src, 0, src.length ); }
		
		/**
		 * Adds a range of integer values from a source array to the list, starting at a specified index.
		 * Elements in the list from the index onwards are shifted to the right to accommodate the new elements.
		 *
		 * @param index     The index in this list where the insertion should begin (0-based).
		 * @param src       The source array of integer values to add.
		 * @param src_index The starting index in the source array from where to begin copying.
		 * @param len       The number of elements to copy from the source array.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index > size()}) or if source array range is invalid.
		 */
		public RW add( int index, char[] src, int src_index, int len ) {
			int max = Math.max( index, size ) + len;
			
			size = Array.resize( values, values.length < max ?
					values = new char[ max + max / 2 ] :
					values, index, size, len );
			
			for( int i = 0; i < len; i++ ) values[ index + i ] = ( char ) src[ src_index + i ];
			return this;
		}
		
		/**
		 * Removes the last element from the list.
		 *
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the list is empty.
		 */
		public RW remove() { return remove( size - 1 ); }
		
		/**
		 * Removes the element at the specified index in the list, shifting subsequent elements to the left.
		 *
		 * @param index The index of the element to remove (0-based).
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
		 */
		public RW remove( int index ) {
			if( size < 1 || size < index ) return this;
			if( index == size - 1 ) size--;
			else size = Array.resize( values, values, index, size, -1 );
			
			return this;
		}
		
		/**
		 * Removes a range of elements from the list, starting at a specified index and removing a given number of elements.
		 * Elements after the removed range are shifted to the left.
		 *
		 * @param index The starting index of the range to remove (0-based).
		 * @param len   The number of elements to remove.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}) or if the range extends beyond the list bounds.
		 */
		public RW remove( int index, int len ) {
			if( size < 1 || size < index ) return this;
			if( index == size - 1 ) size--;
			else size = Array.resize( values, values, index, size, -len );
			
			return this;
		}
		
		/**
		 * Sets the integer value at the end of the list. If the list is not large enough, it will be expanded to accommodate the new element.
		 *
		 * @param value The integer value to set at the end of the list.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 */
		public RW set1( char value ) { return set1( size, value ); }
		
		/**
		 * Sets the integer value at the specified index in the list. If the index is beyond the current size, the list is expanded, and any intervening indices are filled with the default value.
		 *
		 * @param index The index where the value should be set (0-based).
		 * @param value The new integer value to set.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is negative ({@code index < 0}).
		 */
		public RW set1( int index, char value ) {
			
			if( size <= index ) {
				if( values.length <= index ) values = Arrays.copyOf( values, index + index / 2 );
				
				if( default_value != 0 ) Arrays.fill( values, size, index, ( char ) default_value );
				
				size = index + 1;
			}
			
			values[ index ] = ( char ) value;
			return this;
		}
		
		/**
		 * Sets multiple integer values from an array into the list, starting at a specified index. If the index is beyond the current size, the list is expanded as needed.
		 *
		 * @param index The starting index in this list where values should be set (0-based).
		 * @param src   The array of integer values to set.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is negative ({@code index < 0}).
		 */
		public RW set( int index, char... src ) { return set( index, src, 0, src.length ); }
		
		/**
		 * Sets a range of integer values from a source array into this list, starting at a specified index. If the index is beyond the current size, the list is expanded, and intervening indices are filled with the default value as needed.
		 *
		 * @param index     The starting index in this list where values should be set (0-based).
		 * @param src       The source array of integer values.
		 * @param src_index The starting index in the source array from where to begin copying.
		 * @param len       The number of elements to copy from the source array.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is negative ({@code index < 0}) or if source array range is invalid.
		 */
		public RW set( int index, char[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( index + i, src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Swaps the elements at two specified indices in the list.
		 *
		 * @param index1 The index of the first element to swap (0-based).
		 * @param index2 The index of the second element to swap (0-based).
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if either index is out of range ({@code index < 0 || index >= size()}).
		 */
		public RW swap( int index1, int index2 ) {
			final char tmp = values[ index1 ];
			values[ index1 ] = values[ index2 ];
			values[ index2 ] = tmp;
			return this;
		}
		
		/**
		 * Removes all elements from this list that are also present in the specified source list.
		 *
		 * @param src The source list containing elements to be removed from this list.
		 * @return The number of elements removed from this list.
		 */
		public int removeAll( R src ) {
			int fix = size;
			
			for( int i = 0, k, src_size = src.size(); i < src_size; i++ )
				if( -1 < ( k = indexOf( src.get( i ) ) ) ) remove( k );
			return fix - size;
		}
		
		/**
		 * Removes all occurrences of a specified integer value from the list.
		 *
		 * @param src The integer value to be removed from the list.
		 * @return The number of elements removed from this list.
		 */
		public int removeAll( char src ) {
			int fix = size;
			
			for( int k; -1 < ( k = indexOf( src ) ); ) remove( k );
			return fix - size;
		}
		
		/**
		 * Removes all occurrences of a specified integer value from the list using a fast removal method that may alter the order of remaining elements.
		 *
		 * @param src The integer value to be removed.
		 * @return The number of elements removed from this list.
		 */
		public int removeAll_fast( char src ) {
			int fix = size;
			
			for( int k; -1 < ( k = indexOf( src ) ); ) remove_fast( k );
			return fix - size;
		}
		
		/**
		 * Removes the element at the specified index using a fast removal method that replaces the removed element with the last element of the list, potentially changing the order of elements.
		 *
		 * @param index The index of the element to remove (0-based).
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
		 */
		public RW remove_fast( int index ) {
			if( size < 1 || size <= index ) return this;
			values[ index ] = values[ --size ];
			return this;
		}
		
		/**
		 * Retains only the elements in this list that are present in the specified check list. All other elements are removed.
		 *
		 * @param chk The list containing elements to be retained in this list.
		 * @return {@code true} if this list was modified as a result of the operation, {@code false} otherwise.
		 */
		public boolean retainAll( R chk ) {
			
			final int fix = size;
			
			for( int index = 0; index < size; index++ )
				if( !chk.contains( get( index ) ) )
					remove( index );
			
			return fix != size;
		}
		
		/**
		 * Clears all elements from the list, making it empty.
		 *
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 */
		public RW clear() {
			size = 0;
			return this;
		}
		
		/**
		 * Trims the capacity of the list to be equal to its current size. This reduces the memory footprint if the list's capacity is much larger than its size.
		 *
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 */
		public RW fit() { return length( size() ); }
		
		/**
		 * Sets the length of the internal array used by the list. If the new length is less than the current size, the list is truncated. If it's greater, the array is expanded, but no new elements are initialized beyond the current size.
		 *
		 * @param length The new length of the internal array. If less than 1, the list is cleared, and an empty array is used.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 */
		public RW length( int length ) {
			if( values.length != length )
				if( length < 1 ) {
					values = Array.EqualHashOf.chars     .O;
					size   = 0;
					return this;
				}
			
			Arrays.copyOf( values, length );
			if( length < size ) size = length;
			
			return this;
		}
		
		/**
		 * Sets the size of the list. If the new size is less than the current size, the list is truncated. If it's greater, the list is expanded, and new elements are initialized with the default value.
		 *
		 * @param size The new size of the list. If less than 1, the list is cleared.
		 * @return The modified {@code RW} list instance to allow for method chaining.
		 */
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( size() < size ) set1( size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code RW} instance.
		 *
		 * @return A clone of this list.
		 */
		public RW clone() { return ( RW ) super.clone(); }
		
	}
}