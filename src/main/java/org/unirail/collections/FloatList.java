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
 * Defines a contract for a list specifically designed for primitive values.
 */
public interface FloatList {
	
	/**
	 * Read-only base class for a primitive value list implementation providing core functionality.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		/**
		 * Sentinel value representing uninitialized elements in the list.
		 * Since primitive values cannot be null, this value is used to fill new slots when the list expands.
		 * Choose a value that does not conflict with valid data in your use case.
		 */
		public final float default_value;
		
		/**
		 * Constructs a read-only list with a specified default value for uninitialized elements.
		 *
		 * @param default_value The sentinel value used for new or uninitialized elements.
		 */
		protected R( float default_value ) { this.default_value = default_value; }
		
		/**
		 * Internal array storing the list's primitive values, dynamically resized as needed.
		 */
		float[] values = Array.EqualHashOf.floats     .O;
		
		/**
		 * Provides direct access to the internal array.
		 * <p>
		 * Warning: Modifying this array directly may corrupt the list's state and should be avoided.
		 *
		 * @return The internal array of primitive values.
		 */
		public float[] array() { return values; }
		
		/**
		 * Current number of elements in the list, always less than or equal to the array's capacity.
		 */
		int size = 0;
		
		/**
		 * Returns the current number of elements in the list.
		 *
		 * @return The number of elements currently stored.
		 */
		public int size() { return size; }
		
		/**
		 * Returns the total capacity of the internal array backing this list.
		 *
		 * @return The current allocated capacity.
		 */
		public int length() { return values.length; }
		
		/**
		 * Checks if the list is empty.
		 *
		 * @return true if the list has no elements, false otherwise.
		 */
		public boolean isEmpty() { return size == 0; }
		
		/**
		 * Checks if the list contains a specific primitive value.
		 *
		 * @param value The value to search for.
		 * @return true if the value is found, false otherwise.
		 */
		public boolean contains( float value ) { return -1 < indexOf( value ); }
		
		/**
		 * Copies the first `len` elements from the list into the destination array `dst` starting at index `index`.
		 * If `dst` is null, a new array of size `len` is allocated. If `len > size`, elements beyond the list's current size
		 * will be copied from the internal array, which may contain uninitialized or invalid data.
		 *
		 * @param index Starting index in the destination array.
		 * @param len   Number of elements to copy from the list.
		 * @param dst   Destination array for primitive values; if null, a new array of size `len` is allocated.
		 * @return The populated array, or null if the list is empty.
		 */
		public float[] toArray( int index, int len, float[] dst ) {
			if( size == 0 ) return null;
			if( dst == null ) dst = new float[ len ];
			for( int i = 0; i < len; i++ ) dst[ index + i ] =  values[ i ];
			
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
		 * Retrieves the primitive value at a specific index.
		 *
		 * @param index The 0-based index of the element to retrieve.
		 * @return The primitive value at the specified index.
		 */
		public float get( int index ) { return   values[ index ]; }
		
		/**
		 * Retrieves the last primitive value in the list.
		 *
		 * @return The primitive value at the end of the list.
		 */
		public float get() { return   values[ size - 1 ]; }
		
		/**
		 * Copies a range of elements into a destination array of primitive values with bounds checking.
		 *
		 * @param dst       Destination array to copy elements into.
		 * @param dst_index Starting index in the destination array.
		 * @param src_index Starting index in this list.
		 * @param len       Maximum number of elements to copy.
		 * @return Number of elements actually copied.
		 */
		public int get(float[] dst, int dst_index, int src_index, int len ) {
			len = Math.min( Math.min( size - src_index, len ), dst.length - dst_index );
			if( len < 1 ) return 0;
			
			for( int i = 0; i < len; i++ )
			     dst[ dst_index++ ] =  values[ src_index++ ];
			
			return len;
		}
		
		/**
		 * Finds the first occurrence of a primitive value in the list.
		 *
		 * @param value The primitive value to locate.
		 * @return The 0-based index of the first occurrence, or -1 if not found.
		 */
		public int indexOf( float value ) { return Array.indexOf( values, ( float ) value, 0, size ); }
		
		/**
		 * Finds the last occurrence of a primitive value in the list.
		 *
		 * @param value The primitive value to locate.
		 * @return The 0-based index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( float value ) { return Array.lastIndexOf( values, ( float ) value, 0, size ); }
		
		/**
		 * Compares this list with another object for equality.
		 *
		 * @param other The object to compare against.
		 * @return true if the object is an equal list of the same class, false otherwise.
		 */
		public boolean equals( Object other ) {
			if( other == this ) return true;
			return other != null &&
			       getClass() == other.getClass() &&
			       equals( getClass().cast( other ) );
		}
		
		/**
		 * Compares this list with another R instance for equality.
		 *
		 * @param other The R instance to compare with.
		 * @return true if both lists have identical elements in the same order, false otherwise.
		 */
		public boolean equals( R other ) { return other == this || other != null && other.size == size && Array.equals( values, other.values, 0, size ); }
		
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
	 * Read-write extension of the R class, adding methods to modify the list of primitive values.
	 */
	class RW extends R {
		
		/**
		 * Initializes an empty list of primitive values with a specified initial capacity.
		 *
		 * @param length Initial capacity of the internal array; if less than 1, uses an empty array.
		 */
		public RW( int length ) {
			super( ( float ) 0 );
			values = 0 < length ?
					new float[ length ] :
					Array.EqualHashOf.floats     .O;
		}
		
		/**
		 * Initializes the list with a default value and size. If size is positive, creates a list with `size` elements
		 * initialized to `default_value`. If size is zero, creates an empty list. If size is negative, creates an array with
		 * capacity equal to the absolute value of `size` and sets the list size to this capacity, with all elements
		 * initialized to the default value.
		 *
		 * @param default_value Value used for uninitialized or newly added elements.
		 * @param size          Initial size (if positive) or capacity (if negative, using absolute value).
		 */
		public RW( float default_value, int size ) {
			super( default_value );
			values = size == 0 ?
					Array.EqualHashOf.floats     .O :
					new float[ this.size = size < 0 ?
							-size :
							size ];
			if( size < 1 || default_value == 0 ) return;
			while( -1 < --size ) values[ size ] = ( float ) default_value;
		}
		
		/**
		 * Appends a primitive value to the end of the list, expanding if necessary.
		 *
		 * @param value The primitive value to add.
		 * @return This instance for method chaining.
		 */
		public RW add1( float value ) { return add1( size, value ); }
		
		/**
		 * Inserts a primitive value at the specified index, shifting elements rightward.
		 *
		 * @param index 0-based position for insertion.
		 * @param value The primitive value to insert.
		 * @return This instance for method chaining.
		 */
		public RW add1( int index, float value ) {
			int max = Math.max( index, size + 1 );
			size            = Array.resize( values, values.length <= max ?
					values = new float[ max + max / 2 ] :
					values, index, size, 1 );
			values[ index ] = ( float ) value;
			return this;
		}
		
		/**
		 * Appends multiple primitive values from an array to the end of the list.
		 *
		 * @param src Array of primitive values to add.
		 * @return This instance for method chaining.
		 */
		public RW add( float... src ) { return add( size(), src, 0, src.length ); }
		
		/**
		 * Inserts a range of primitive values from an array at a specified index, shifting elements rightward.
		 *
		 * @param index     Starting position in this list.
		 * @param src       Source array of primitive values.
		 * @param src_index Starting index in the source array.
		 * @param len       Number of elements to insert.
		 * @return This instance for method chaining.
		 */
		public RW add( int index, float[] src, int src_index, int len ) {
			int max = Math.max( index, size ) + len;
			size = Array.resize( values, values.length < max ?
					values = new float[ max + max / 2 ] :
					values, index, size, len );
			for( int i = 0; i < len; i++ ) values[ index + i ] = ( float ) src[ src_index + i ];
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
		 * Sets the primitive value at the end of the list, expanding if necessary with default_value.
		 *
		 * @param value The primitive value to set.
		 * @return This instance for method chaining.
		 */
		public RW set1( float value ) { return set1( size, value ); }
		
		/**
		 * Sets a primitive value at a specific index, expanding the list with default_value if needed.
		 *
		 * @param index 0-based index to set the value.
		 * @param value The primitive value to set.
		 * @return This instance for method chaining.
		 */
		public RW set1( int index, float value ) {
			if( size <= index ) {
				if( values.length <= index ) values = Arrays.copyOf( values, index == 0 ?
						16 :
						index * 3 / 2 );
				if( default_value != 0 ) Arrays.fill( values, size, index, ( float ) default_value );
				size = index + 1;
			}
			values[ index ] = ( float ) value;
			return this;
		}
		
		/**
		 * Sets multiple primitive values from an array starting at a specified index.
		 *
		 * @param index Starting 0-based index in this list.
		 * @param src   Array of primitive values to set.
		 * @return This instance for method chaining.
		 */
		public RW set( int index, float... src ) { return set( index, src, 0, src.length ); }
		
		/**
		 * Sets a range of primitive values from an array starting at a specified index, expanding if necessary.
		 *
		 * @param index     Starting 0-based index in this list.
		 * @param src       Source array of primitive values.
		 * @param src_index Starting index in the source array.
		 * @param len       Number of elements to set.
		 * @return This instance for method chaining.
		 */
		public RW set( int index, float[] src, int src_index, int len ) {
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
			if( index1 < 0 || index1 >= size() ) throw new IndexOutOfBoundsException( "Index1 must be non-negative and less than the list's size: " + index1 );
			if( index2 < 0 || index2 >= size() ) throw new IndexOutOfBoundsException( "Index2 must be non-negative and less than the list's size: " + index2 );
			
			final float tmp = values[ index1 ];
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
		 * Removes all occurrences of a specific primitive value.
		 *
		 * @param src Primitive value to remove.
		 * @return Number of elements removed.
		 */
		public int removeAll( float src ) {
			int fix = size;
			for( int k; -1 < ( k = indexOf( src ) ); ) remove( k );
			return fix - size;
		}
		
		/**
		 * Quickly removes all occurrences of a primitive value, possibly reordering elements.
		 *
		 * @param src Primitive value to remove.
		 * @return Number of elements removed.
		 */
		public int removeAll_fast( float src ) {
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
			if( chk == null ) return false;
			boolean ret = false;
			for( int i = size; -1 < --i; ) {
				float v = get( i );
				if( chk.contains( v ) ) continue;
				removeAll( v );
				ret = true;
				if( size < i ) i = size;
			}
			return ret;
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
		 * Adjusts the internal array's capacity, truncating or expanding as needed.
		 * If the new length is less than the current size, the list will be truncated.
		 *
		 * @param length New capacity; if less than 1, clears the list.
		 * @return This instance for method chaining.
		 */
		public RW length( int length ) {
			if( length < 0 ) throw new IllegalArgumentException( "length cannot be negative" );
			if( values.length != length )
				if( length < 1 ) {
					values = Array.EqualHashOf.floats     .O;
					size   = 0;
					return this;
				}
			values = Arrays.copyOf( values, length );
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