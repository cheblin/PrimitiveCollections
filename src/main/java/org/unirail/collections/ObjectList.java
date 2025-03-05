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
//  OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.
package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;

/**
 * {@code ObjectList} interface and implementations for a dynamically resizable list of objects.
 * Provides functionalities similar to {@link java.util.ArrayList} but with a focus on type safety and
 * potentially specialized for object types.
 *
 * <V> The type of elements in this list.
 */
public interface ObjectList {
	
	/**
	 * Abstract base class {@code R} providing common implementations for {@code ObjectList}.
	 * Handles core list operations, data storage, cloning, equality, and JSON serialization.
	 *
	 * @param <V> The type of elements in the list.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		/**
		 * Internal array to store list elements.
		 */
		protected       V[]                    values;
		/**
		 * Utility for comparing and hashing elements of type V.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		/**
		 * Flag indicating if the element type V is String, used for JSON serialization optimization.
		 */
		private final   boolean                V_is_string;
		
		/**
		 * Constructor for {@code R} when the class of element type {@code V} is available.
		 *
		 * @param clazzV The class object representing the element type {@code V}.
		 */
		protected R( Class< V > clazzV ) {
			equal_hash_V = Array.get( clazzV );
			V_is_string  = clazzV == String.class;
		}
		
		/**
		 * Constructor for {@code R} when an {@link Array.EqualHashOf} instance is already available.
		 *
		 * @param equal_hash_V The {@link Array.EqualHashOf} instance for element type {@code V}.
		 */
		public R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
			V_is_string       = false;
		}
		
		/**
		 * Current number of elements in the list.
		 */
		int size = 0;
		
		/**
		 * Returns the number of elements in this list.
		 *
		 * @return The size of the list.
		 */
		public int size() { return size; }
		
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
			if( size == 0 ) return null;
			if( dst == null || dst.length < len ) return Arrays.copyOfRange( values, index, index + len );
			System.arraycopy( values, index, dst, 0, len );
			return dst;
		}
		
		/**
		 * Checks if this list contains all elements of the specified list.
		 *
		 * @param src The list to check for containment.
		 * @return {@code true} if this list contains all elements of {@code src}, {@code false} otherwise.
		 */
		public boolean containsAll( R< V > src ) {
			
			for( int i = 0, s = src.size(); i < s; i++ )
				if( indexOf( src.get( i ) ) == -1 ) return false;
			return true;
		}
		
		/**
		 * Checks if a value exists at the specified index (and is not null).
		 *
		 * @param index The index to check.
		 * @return {@code true} if the index is within bounds and the value is not null, {@code false} otherwise.
		 */
		public boolean hasValue( int index ) {
			return -1 < index && index < size && values[ index ] != null;
		}
		
		/**
		 * Returns the element at the specified index in this list.
		 *
		 * @param index The index of the element to return.
		 * @return The element at the specified index.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
		 */
		public V get( int index ) { return values[ index ]; }
		
		
		/**
		 * Returns the index of the first occurrence of the specified value in this list,
		 * or -1 if this list does not contain the value.
		 *
		 * @param value The value to search for.
		 * @return The index of the first occurrence, or -1 if not found.
		 */
		public int indexOf( V value ) {
			for( int i = 0; i < size; i++ )
				if( equal_hash_V.equals( values[ i ], value ) ) return i;
			return -1;
		}
		
		/**
		 * Returns the index of the last occurrence of the specified value in this list,
		 * or -1 if this list does not contain the value.
		 *
		 * @param value The value to search for.
		 * @return The index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( V value ) {
			for( int i = size; -1 < --i; )
				if( equal_hash_V.equals( values[ i ], value ) ) return i;
			return -1;
		}
		
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj ); }
		
		/**
		 * Compares this list to another {@code R<V>} list for equality.
		 *
		 * @param other The list to compare with.
		 * @return {@code true} if the lists are equal, {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) { return other != null && size == other.size && equal_hash_V.equals( values, other.values, size ); }
		
		public int hashCode() {
			return Array.finalizeHash( V_is_string ?
					                           Array.hash( ( String[] ) values, size ) :
					                           equal_hash_V.hashCode( values, size ), size() );
		}
		
		
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				if( dst.values != null ) dst.values = values.clone();
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		
		/**
		 * Returns a JSON string representation of this list.
		 *
		 * @return JSON string of the list.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the JSON representation of this list to the provided {@link JsonWriter}.
		 *
		 * @param json The JsonWriter to write to.
		 */
		@Override public void toJSON( JsonWriter json ) {
			json.enterArray();
			
			int size = size();
			
			if( 0 < size ) {
				json.preallocate( size * 10 );
				if( V_is_string ) {
					String[] strs = ( String[] ) values;
					for( int i = 0; i < size; i++ ) json.value( strs[ i ] );
				}
				else {
					for( int i = 0; i < size; i++ ) json.value( values[ i ] );
				}
			}
			json.exitArray();
		}
	}
	
	/**
	 * {@code RW} (Read-Write) is a concrete implementation of {@code ObjectList} that allows modifications.
	 *
	 * @param <V> The type of elements in the list.
	 */
	class RW< V > extends R< V > {
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
			
			values = length == 0 ?
					equal_hash_V.OO :
					equal_hash_V.copyOf( null, length );
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
			
			values = size == 0 ?
					equal_hash_V.OO :
					// If size is 0, use an empty array
					equal_hash_V.copyOf( null, this.size = size < 0 ?
							-size :
							size );// Create a new array with the specified size
			
			if( default_value == null || size < 1 ) return;
			for( int i = 0; i < size; i++ ) values[ i ] = default_value; // Corrected initialization loop
		}
		
		
		/**
		 * Adds an element to the end of the list.
		 *
		 * @param value The element to add.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW< V > add1( V value ) { return add1( size, value ); }
		
		/**
		 * Adds an element at a specific index, shifting subsequent elements to the right.
		 *
		 * @param index The index at which to insert the element.
		 * @param value The element to add.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index > size()}).
		 */
		public RW< V > add1( int index, V value ) {
			int max = Math.max( index, size + 1 );
			
			size = Array.resize( values, values.length <= max ?
					values = equal_hash_V.copyOf( null, max + max / 2 ) :
					values, index, size, 1 );
			
			values[ index ] = value;
			return this;
		}
		
		/**
		 * Adds multiple elements to the end of the list.
		 *
		 * @param items The elements to add.
		 * @return This {@code RW} instance for chaining.
		 */
		@SafeVarargs public final RW< V > add( V... items ) { return add( size(), items, 0, items.length ); }
		
		/**
		 * Adds a range of elements from a source array to the list, starting at a specific index.
		 *
		 * @param index     The index at which to insert the elements.
		 * @param src       The source array.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to add from the source array.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} or source array range is invalid.
		 */
		public RW< V > add( int index, V[] src, int src_index, int len ) {
			int max = Math.max( index, size ) + len;
			
			size = Array.resize( values, values.length < max ?
					values = equal_hash_V.copyOf( null, max + max / 2 ) :
					values, index, size, len );
			
			System.arraycopy( src, src_index, values, index, len );
			return this;
		}
		
		/**
		 * Removes the last element from the list.
		 *
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if the list is empty.
		 */
		public RW< V > remove() { return remove( size - 1 ); }
		
		/**
		 * Removes an element at a specific index, shifting subsequent elements to the left.
		 *
		 * @param index The index of the element to remove.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
		 */
		public RW< V > remove( int index ) {
			size = Array.resize( values, values, index, size, -1 );
			return this;
		}
		
		/**
		 * Removes the element at a specific index, but faster by replacing it with the last element.
		 * Order of elements may not be preserved.
		 *
		 * @param index The index of the element to remove.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
		 */
		public RW< V > remove_fast( int index ) {
			if( size < 1 || size <= index ) return this;
			if( size < 1 ) return this;
			size--;
			values[ index ] = values[ size ];
			values[ size ]  = null;
			return this;
		}
		
		/**
		 * Sets a value at the end of the list. If the list is not large enough, it will be expanded.
		 *
		 * @param value The value to set.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW< V > set1( V value ) { return set1( size, value ); }
		
		/**
		 * Sets a value at a specific index. If the index is beyond the current size, the list is expanded.
		 *
		 * @param index The index to set the value at.
		 * @param value The value to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if the index is negative ({@code index < 0}).
		 */
		public RW< V > set1( int index, V value ) {
			if( size <= index ) {
				if( values.length <= index ) values = equal_hash_V.copyOf( values, index + index / 2 );
				size = index + 1; // Update size after resizing
			}
			values[ index ] = value;
			return this;
		}
		
		/**
		 * Sets multiple values starting at a specific index.
		 *
		 * @param index The starting index to set values.
		 * @param src   The values to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		@SafeVarargs
		public final RW< V > set( int index, V... src ) { return set( index, src, 0, src.length ); }
		
		
		/**
		 * Sets a range of values from a source array into this list, starting at a specified index.
		 *
		 * @param index     The starting index in this list.
		 * @param src       The source array.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to set from the source array.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} or source array range is invalid.
		 */
		public RW< V > set( int index, V[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( index + i, src[ src_index + i ] );
			return this;
		}
		
		
		/**
		 * Removes all elements that are present in the specified list.
		 *
		 * @param src The list of elements to remove.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW< V > removeAll( R< V > src ) {
			for( int i = 0, max = src.size(); i < max; i++ )
			     removeAll( src.get( i ) );
			return this;
		}
		
		/**
		 * Removes all occurrences of a specific element from the list.
		 *
		 * @param src The element to remove.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW< V > removeAll( V src ) {
			for( int k; ( k = indexOf( src ) ) != -1; ) remove( k );
			return this;
		}
		
		/**
		 * Removes all occurrences of a specific element from the list using the faster remove method.
		 * Order of elements may not be preserved.
		 *
		 * @param src The element to remove.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW< V > removeAll_fast( V src ) {
			for( int k; ( k = indexOf( src ) ) != -1; ) remove_fast( k );
			return this;
		}
		
		/**
		 * Retains only the elements that are present in the specified list.
		 *
		 * @param chk The list of elements to retain.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW< V > retainAll( R< V > chk ) {
			for( int i = 0; i < size; i++ ) {
				final V val = values[ i ];
				if( chk.indexOf( val ) == -1 ) removeAll( val );
			}
			return this;
		}
		
		public RW< V > clone() { return ( RW< V > ) super.clone(); }
		
		/**
		 * Clears the list, removing all elements.
		 *
		 * @return This {@code RW} instance for chaining.
		 */
		public RW< V > clear() {
			if( size < 1 ) return this;
			Arrays.fill( values, 0, size - 1, default_value );//release objects
			size = 0;
			return this;
		}
		
		/**
		 * Sets the internal array length, truncating or padding with default values if necessary.
		 *
		 * @param length The new length of the internal array.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW< V > length( int length ) {
			if( length < 1 ) {
				values = equal_hash_V.OO;
				size   = 0;
				return this;
			}
			int old_length = values.length;
			values = equal_hash_V.copyOf( values, length );
			
			if( length < size ) size = length;
			else if( default_value != null && old_length < length ) { // Fill new space with default value only if needed
				Arrays.fill( values, old_length, length, default_value ); // Fill the newly allocated space
				size = length; // Update size to reflect the new length
			}
			
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
		public RW< V > swap( int index1, int index2 ) {
			final V tmp = values[ index1 ];
			values[ index1 ] = values[ index2 ];
			values[ index2 ] = tmp;
			return this;
		}
		
		/**
		 * Sets the size of the list, adding default values if increasing size, or truncating if decreasing.
		 *
		 * @param size The new size of the list.
		 * @return This {@code RW} instance for chaining.
		 * @throws IllegalArgumentException if size is negative.
		 */
		public RW< V > size( int size ) {
			if( size < 1 ) {
				clear();
				return this;
			}
			
			if( values.length < size ) {
				values = equal_hash_V.copyOf( values, size );
				if( default_value != null ) Arrays.fill( values, this.size, ( this.size = size ) - 1, default_value );
				return this;
			}
			
			if( this.size < size )
				if( default_value != null ) Arrays.fill( values, this.size, size - 1, default_value );
			
			this.size = size;
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
	static < V > Array.EqualHashOf< RW< V > > equal_hash() { return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; }
}
	