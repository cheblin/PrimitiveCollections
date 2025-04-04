//MIT License
//
//Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
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

public interface UByteList {
	
	/**
	 * Read-only base class for an integer list implementation providing core functionality.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		/**
		 * Sentinel value representing uninitialized or invalid elements in the list.
		 * Since primitive ints cannot be null, this value (e.g., -1, 0, Integer.MAX_VALUE)
		 * is used to fill new slots when the list expands. Choose a value that does not
		 * conflict with valid data in your use case.
		 */
		public final char default_value;
		
		/**
		 * Constructs a read-only list with a specified default value for uninitialized elements.
		 *
		 * @param default_value The sentinel value used for new or uninitialized elements.
		 */
		protected R( char default_value ) { this.default_value = default_value; }
		
		/**
		 * Internal array storing the list's integer values, dynamically resized as needed.
		 */
		byte[] values = Array.EqualHashOf.bytes     .O;
		
		/**
		 * Provides direct access to the internal integer array.
		 * <p>
		 * Warning: Modifying this array directly may corrupt the list's state and should be avoided.
		 *
		 * @return The internal array of integer values.
		 */
		public byte[] array() { return values; }
		
		/**
		 * Current number of elements in the list, always less than or equal to the array's capacity.
		 */
		int size = 0;
		
		/**
		 * Returns the current number of acid elements in the list.
		 *
		 * @return The number of elements currently stored.
		 */
		public int size() { return size; }
		
		/**
		 * Checks if the list is empty.
		 *
		 * @return true if the list has no elements, false otherwise.
		 */
		public boolean isEmpty() { return size == 0; }
		
		/**
		 * Checks if the list contains a specific integer value.
		 *
		 * @param value The value to search for.
		 * @return true if the value is found, false otherwise.
		 */
		public boolean contains( char value ) { return -1 < indexOf( value ); }
		
		/**
		 * Copies a range of elements into a destination array, creating one if none is provided.
		 *
		 * @param index Starting index in the destination array.
		 * @param len   Number of elements to copy from the list.
		 * @param dst   Destination array; if null, a new array of size `len` is allocated.
		 * @return The populated array, or null if the list is empty.
		 */
		public char[] toArray( int index, int len, char[] dst ) {
			if( size == 0 ) return null;
			if( dst == null ) dst = new char[ len ];
			for( int i = 0; i < len; i++ ) dst[ index + i ] = (char)( 0xFF &  values[ i ]);
			
			return dst;
		}
		
		/**
		 * Verifies if all elements from another list are present in this list.
		 *
		 * @param src The list whose elements are checked for presence.
		 * @return true if all elements in `src` are contained, false otherwise.
		 */
		public boolean containsAll( R src ) {
			for( int i = src.size(); -1 < --i; )
				if( !contains( src.get( i ) ) ) return false;
			return true;
		}
		
		/**
		 * Retrieves the value at a specific index.
		 *
		 * @param index The 0-based index of the element to retrieve.
		 * @return The integer value at the specified index.
		 */
		public char get( int index ) { return  (char)( 0xFF &  values[ index ]); }
		
		/**
		 * Retrieves the last value in the list.
		 *
		 * @return The integer value at the end of the list.
		 */
		public char get() { return  (char)( 0xFF &  values[ size - 1 ]); }
		
		/**
		 * Copies a range of elements into a destination array with bounds checking.
		 *
		 * @param dst       Destination array to copy elements into.
		 * @param dst_index Starting index in the destination array.
		 * @param src_index Starting index in this list.
		 * @param len       Maximum number of elements to copy.
		 * @return Number of elements actually copied.
		 */
		public int get(char[] dst, int dst_index, int src_index, int len ) {
			len = Math.min( Math.min( size - src_index, len ), dst.length - dst_index );
			if( len < 1 ) return 0;
			
			for( int i = 0; i < len; i++ )
			     dst[ dst_index++ ] = (char)( 0xFF &  values[ src_index++ ]);
			
			return len;
		}
		
		/**
		 * Finds the first occurrence of a value in the list.
		 *
		 * @param value The value to locate.
		 * @return The 0-based index of the first occurrence, or -1 if not found.
		 */
		public int indexOf( char value ) { return Array.indexOf( values, ( byte ) value, 0, size ); }
		
		/**
		 * Finds the last occurrence of a value in the list.
		 *
		 * @param value The value to locate.
		 * @return The 0-based index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( char value ) { return Array.lastIndexOf( values, ( byte ) value, 0, size ); }
		
		/**
		 * Compares this list with another object for equality.
		 *
		 * @param obj The object to compare against.
		 * @return true if the object is an equal list of the same class, false otherwise.
		 */
		public boolean equals( Object obj ) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		/**
		 * Compares this list with another R instance for equality.
		 *
		 * @param other The R instance to compare with.
		 * @return true if both lists have identical elements in the same order, false otherwise.
		 */
		public boolean equals( R other ) { return other != null && other.size == size && Array.equals( values, other.values, 0, size ); }
		
		/**
		 * Generates a hash code based on the list's elements and their order.
		 *
		 * @return A hash code for this list.
		 */
		public final int hashCode() { return Array.avalanche( Array.hash( Array.mix( seed, size ), values, 0, size ) ); }
		
		/**
		 * Seed value for hash code calculation, based on the class's identity.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Creates a shallow copy of this list, including its internal array.
		 *
		 * @return A cloned instance of this list.
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
		 * Returns a JSON string representation of the list's elements.
		 *
		 * @return A string in JSON array format.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Serializes the list as a JSON array into the provided writer.
		 *
		 * @param json The JsonWriter to output the JSON representation.
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
	 * Read-write extension of the R class, adding methods to modify the list.
	 */
	class RW extends R {
		
		/**
		 * Initializes an empty list with a specified initial capacity.
		 *
		 * @param length Initial capacity of the internal array; if 0 or less, uses an empty array.
		 */
		public RW( int length ) {
			super( ( char ) 0 );
			values = 0 < length ?
					new byte[ length ] :
					Array.EqualHashOf.bytes     .O;
		}
		
		/**
		 * Initializes the list with a default value and size. Negative size sets capacity without initialization.
		 *
		 * @param default_value Value used for uninitialized or newly added elements.
		 * @param size          Initial size (if positive) or capacity (if negative, using absolute value).
		 */
		public RW( char default_value, int size ) {
			super( default_value );
			values = size == 0 ?
					Array.EqualHashOf.bytes     .O :
					new byte[ this.size = size < 0 ?
							-size :
							size ];
			if( size < 1 || default_value == 0 ) return;
			while( -1 < --size ) values[ size ] = ( byte ) default_value;
		}
		
		/**
		 * Appends a value to the end of the list, expanding if necessary.
		 *
		 * @param value The integer to add.
		 * @return This instance for method chaining.
		 */
		public RW add1( char value ) { return add1( size, value ); }
		
		/**
		 * Inserts a value at the specified index, shifting elements rightward.
		 *
		 * @param index 0-based position for insertion.
		 * @param value The integer to insert.
		 * @return This instance for method chaining.
		 */
		public RW add1( int index, char value ) {
			int max = Math.max( index, size + 1 );
			size            = Array.resize( values, values.length <= max ?
					values = new byte[ max + max / 2 ] :
					values, index, size, 1 );
			values[ index ] = ( byte ) value;
			return this;
		}
		
		/**
		 * Appends multiple values from an array to the end of the list.
		 *
		 * @param src Array of integers to add.
		 * @return This instance for method chaining.
		 */
		public RW add( byte... src ) { return add( size(), src, 0, src.length ); }
		
		/**
		 * Inserts a range of values from an array at a specified index, shifting elements rightward.
		 *
		 * @param index     Starting position in this list.
		 * @param src       Source array of integers.
		 * @param src_index Starting index in the source array.
		 * @param len       Number of elements to insert.
		 * @return This instance for method chaining.
		 */
		public RW add( int index, byte[] src, int src_index, int len ) {
			int max = Math.max( index, size ) + len;
			size = Array.resize( values, values.length < max ?
					values = new byte[ max + max / 2 ] :
					values, index, size, len );
			for( int i = 0; i < len; i++ ) values[ index + i ] = ( byte ) src[ src_index + i ];
			return this;
		}
		
		/**
		 * Removes the last element from the list.
		 *
		 * @return This instance for method chaining.
		 */
		public RW remove() { return remove( size - 1 ); }
		
		/**
		 * Removes the element at a specified index, shifting subsequent elements leftward.
		 *
		 * @param index 0-based index of the element to remove.
		 * @return This instance for method chaining.
		 */
		public RW remove( int index ) {
			if( size < 1 || size < index ) return this;
			if( index == size - 1 ) size--;
			else size = Array.resize( values, values, index, size, -1 );
			return this;
		}
		
		/**
		 * Removes a range of elements starting at the specified index.
		 *
		 * @param index Starting 0-based index of the range to remove.
		 * @param len   Number of elements to remove.
		 * @return This instance for method chaining.
		 */
		public RW remove( int index, int len ) {
			if( size < 1 || size < index ) return this;
			if( index == size - 1 ) size--;
			else size = Array.resize( values, values, index, size, -len );
			return this;
		}
		
		/**
		 * Sets the value at the end of the list, expanding if necessary with default_value.
		 *
		 * @param value The integer to set.
		 * @return This instance for method chaining.
		 */
		public RW set1( char value ) { return set1( size, value ); }
		
		/**
		 * Sets a value at a specific index, expanding the list with default_value if needed.
		 *
		 * @param index 0-based index to set the value.
		 * @param value The integer to set.
		 * @return This instance for method chaining.
		 */
		public RW set1( int index, char value ) {
			if( size <= index ) {
				if( values.length <= index ) values = Arrays.copyOf( values, index + index / 2 );
				if( default_value != 0 ) Arrays.fill( values, size, index, ( byte ) default_value );
				size = index + 1;
			}
			values[ index ] = ( byte ) value;
			return this;
		}
		
		/**
		 * Sets multiple values from an array starting at a specified index.
		 *
		 * @param index Starting 0-based index in this list.
		 * @param src   Array of integers to set.
		 * @return This instance for method chaining.
		 */
		public RW set( int index, char... src ) { return set( index, src, 0, src.length ); }
		
		/**
		 * Sets a range of values from an array starting at a specified index, expanding if necessary.
		 *
		 * @param index     Starting 0-based index in this list.
		 * @param src       Source array of integers.
		 * @param src_index Starting index in the source array.
		 * @param len       Number of elements to set.
		 * @return This instance for method chaining.
		 */
		public RW set( int index, char[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( index + i, src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Swaps the elements at two specified indices.
		 *
		 * @param index1 First 0-based index.
		 * @param index2 Second 0-based index.
		 * @return This instance for method chaining.
		 */
		public RW swap( int index1, int index2 ) {
			final byte tmp = values[ index1 ];
			values[ index1 ] = values[ index2 ];
			values[ index2 ] = tmp;
			return this;
		}
		
		/**
		 * Removes all elements present in another list.
		 *
		 * @param src List of elements to remove.
		 * @return Number of elements removed.
		 */
		public int removeAll( R src ) {
			int fix = size;
			for( int i = 0, k, src_size = src.size(); i < src_size; i++ )
				if( -1 < ( k = indexOf( src.get( i ) ) ) ) remove( k );
			return fix - size;
		}
		
		/**
		 * Removes all occurrences of a specific value.
		 *
		 * @param src Value to remove.
		 * @return Number of elements removed.
		 */
		public int removeAll( char src ) {
			int fix = size;
			for( int k; -1 < ( k = indexOf( src ) ); ) remove( k );
			return fix - size;
		}
		
		/**
		 * Quickly removes all occurrences of a value, possibly reordering elements.
		 *
		 * @param src Value to remove.
		 * @return Number of elements removed.
		 */
		public int removeAll_fast( char src ) {
			int fix = size;
			for( int k; -1 < ( k = indexOf( src ) ); ) remove_fast( k );
			return fix - size;
		}
		
		/**
		 * Quickly removes an element by replacing it with the last element.
		 *
		 * @param index 0-based index of the element to remove.
		 * @return This instance for method chaining.
		 */
		public RW remove_fast( int index ) {
			if( size < 1 || size <= index ) return this;
			values[ index ] = values[ --size ];
			return this;
		}
		
		/**
		 * Retains only elements present in another list, removing others.
		 *
		 * @param chk List of elements to retain.
		 * @return true if the list was modified, false otherwise.
		 */
		public boolean retainAll( R chk ) {
			final int fix = size;
			for( int index = 0; index < size; index++ )
				if( !chk.contains( get( index ) ) )
					remove( index );
			return fix != size;
		}
		
		/**
		 * Empties the list without changing its capacity.
		 *
		 * @return This instance for method chaining.
		 */
		public RW clear() {
			size = 0;
			return this;
		}
		
		/**
		 * Trims the internal array's capacity to match the current size.
		 *
		 * @return This instance for method chaining.
		 */
		public RW fit() { return length( size() ); }
		
		/**
		 * Adjusts the internal array's length, truncating or expanding as needed.
		 *
		 * @param length New capacity; if less than 1, clears the list.
		 * @return This instance for method chaining.
		 */
		public RW length( int length ) {
			if( values.length != length )
				if( length < 1 ) {
					values = Array.EqualHashOf.bytes     .O;
					size   = 0;
					return this;
				}
			values = Arrays.copyOf( values, length ); // Fixed missing assignment
			if( length < size ) size = length;
			return this;
		}
		
		/**
		 * Sets the list's size, expanding with default_value or truncating as needed.
		 *
		 * @param size New size; if less than 1, clears the list.
		 * @return This instance for method chaining.
		 */
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( size() < size ) set1( size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		/**
		 * Creates a shallow copy of this RW instance.
		 *
		 * @return A cloned RW instance.
		 */
		public RW clone() { return ( RW ) super.clone(); }
	}
}