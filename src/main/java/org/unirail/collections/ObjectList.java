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
 * Interface for a dynamically resizable list of objects, similar to {@link java.util.ArrayList}
 * but with enhanced type safety and specialized handling for object types.
 */
public interface ObjectList {
	
	/**
	 * Abstract base class providing core functionality for {@code ObjectList}.
	 * Manages storage, core operations, cloning, equality checks, and JSON serialization.
	 *
	 * @param <V> The type of elements in the list.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		/**
		 * Internal array storing the list's elements.
		 */
		protected V[] values;
		
		/**
		 * Utility for comparing and hashing elements of type V.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		/**
		 * Indicates if the element type V is String, optimizing JSON serialization.
		 */
		private final boolean V_is_string;
		
		/**
		 * Constructs an instance with the specified element type class.
		 *
		 * @param clazzV The class object representing the element type V.
		 */
		protected R( Class< V > clazzV ) {
			equal_hash_V = Array.get( clazzV );
			V_is_string  = clazzV == String.class;
		}
		
		/**
		 * Constructs an instance with a provided {@link Array.EqualHashOf} instance.
		 *
		 * @param equal_hash_V The {@link Array.EqualHashOf} for element type V.
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
		 * Returns the current number of elements in the list.
		 *
		 * @return The size of the list.
		 */
		public int size() { return size; }
		
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Copies a range of elements from this list into a destination array.
		 *
		 * @param index The starting index in this list.
		 * @param len   The number of elements to copy.
		 * @param dst   The destination array, or null to create a new array.
		 * @return The destination array with copied elements, or null if the list is empty.
		 */
		public V[] toArray( int index, int len, V[] dst ) {
			if( index < 0 ) throw new IndexOutOfBoundsException( "index cannot be negative" );
			if( size <= index  ) throw new IndexOutOfBoundsException( "index range exceeds bounds" );
			if (index + len > size)         throw new IllegalArgumentException("range exceeds size");
			if( dst == null || dst.length < len ) return Arrays.copyOfRange( values, index, index + len );
			System.arraycopy( values, index, dst, 0, len );
			return dst;
		}
		
		/**
		 * Checks if this list contains all elements of another list.
		 *
		 * @param src The list whose elements to check for.
		 * @return true if all elements of src are in this list, false otherwise.
		 */
		public boolean containsAll( R< V > src ) {
			for( int i = 0, s = src.size(); i < s; i++ )
				if( indexOf( src.get( i ) ) == -1 ) return false;
			return true;
		}
		
		/**
		 * Checks if a non-null value exists at the specified index.
		 *
		 * @param index The index to check.
		 * @return true if the index is valid and the value is non-null, false otherwise.
		 */
		public boolean hasValue( int index ) {
			return -1 < index && index < size && values[ index ] != null;
		}
		
		/**
		 * Retrieves the element at the specified index.
		 *
		 * @param index The index of the element.
		 * @return The element at the specified index.
		 */
		public V get( int index ) {
			return values[ index ];
		}
		
		/**
		 * Finds the index of the first occurrence of a value.
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
		 * Finds the index of the last occurrence of a value.
		 *
		 * @param value The value to search for.
		 * @return The index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( V value ) {
			for( int i = size; -1 < --i; )
				if( equal_hash_V.equals( values[ i ], value ) ) return i;
			return -1;
		}
		
		/**
		 * Checks if this list equals another object.
		 *
		 * @param obj The object to compare with.
		 * @return true if the object is a list with the same elements, false otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj );
		}
		
		/**
		 * Checks if this list equals another {@code R<V>} list.
		 *
		 * @param other The list to compare with.
		 * @return true if the lists have the same size and elements, false otherwise.
		 */
		public boolean equals( R< V > other ) {
			return other == this || other != null && size == other.size && equal_hash_V.equals( values, other.values, size );
		}
		
		/**
		 * Computes the hash code of this list.
		 *
		 * @return The hash code based on the list's elements and size.
		 */
		public int hashCode() {
			return Array.finalizeHash( V_is_string ?
					                           Array.hash( ( String[] ) values, size ) :
					                           equal_hash_V.hashCode( values, size ), size() );
		}
		
		/**
		 * Creates a deep copy of this list.
		 *
		 * @return A cloned instance of this list.
		 */
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
		 * @return The JSON string.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Serializes this list to JSON using the provided {@link JsonWriter}.
		 *
		 * @param json The JsonWriter to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray();
			int size = size();
			if( 0 < size ) {
				json.preallocate( size * 10 );
				if( V_is_string ) {
					String[] strs = ( String[] ) values;
					for( int i = 0; i < size; i++ ) json.value( strs[ i ] );
				}
				else for( int i = 0; i < size; i++ ) json.value( values[ i ] );
			}
			json.exitArray();
		}
	}
	
	/**
	 * Concrete implementation of {@code ObjectList} supporting read and write operations.
	 *
	 * @param <V> The type of elements in the list.
	 */
	class RW< V > extends R< V > {
		
		
		/**
		 * Constructs an empty list with the specified initial capacity.
		 *
		 * @param clazz  The class of the element type V.
		 * @param length The initial capacity of the list.
		 */
		@SuppressWarnings( "unchecked" )
		public RW( Class< V > clazz, int length ) {
			this( Array.get( clazz ), length );
		}
		
		/**
		 * Constructs an empty list with the specified initial capacity and equality handler.
		 *
		 * @param equal_hash_V The equality and hash code handler for type V.
		 * @param length       The initial capacity of the list.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int length ) {
			super( equal_hash_V );
			values = length == 0 ?
					equal_hash_V.OO :
					equal_hash_V.copyOf( null, length );
		}
		
		
		/**
		 * Appends an element to the end of the list.
		 *
		 * @param value The element to append.
		 * @return This list for method chaining.
		 */
		public RW< V > add1( V value ) {
			return add1( size, value );
		}
		
		/**
		 * Inserts an element at the specified index, shifting elements as needed.
		 *
		 * @param index The index for insertion.
		 * @param value The element to insert.
		 * @return This list for method chaining.
		 */
		public RW< V > add1( int index, V value ) {
			int max = Math.max( index, size + 1 );
			size            = Array.resize( values, values.length <= max ?
					values = equal_hash_V.copyOf( null, max + max / 2 ) :
					values, index, size, 1 );
			values[ index ] = value;
			return this;
		}
		
		/**
		 * Appends multiple elements to the end of the list.
		 *
		 * @param items The elements to append.
		 * @return This list for method chaining.
		 */
		@SafeVarargs
		public final RW< V > add( V... items ) {
			return add( size(), items, 0, items.length );
		}
		
		/**
		 * Inserts a range of elements from an array at the specified index.
		 *
		 * @param index     The insertion index in this list.
		 * @param src       The source array.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to insert.
		 * @return This list for method chaining.
		 */
		public RW< V > add( int index, V[] src, int src_index, int len ) {
			if( index < 0 ) throw new IllegalArgumentException( "Index cannot be negative" );
			if( src_index < 0 ) throw new IllegalArgumentException( "Source index cannot be negative" );
			if( len < 0 ) throw new IllegalArgumentException( "Length cannot be negative" );
			if( src == null ) throw new NullPointerException( "Source array cannot be null" );
			if( src.length < src_index + len ) throw new IllegalArgumentException( "Source range exceeds array bounds" );
			
			int max = Math.max( index, size ) + len;
			size = Array.resize( values,
			                     values.length < max ?
					                     values = equal_hash_V.copyOf( null, max * 3 / 2 ) :
					                     values, index, size, len );
			System.arraycopy( src, src_index, values, index, len );
			return this;
		}
		
		/**
		 * Removes the last element from the list.
		 *
		 * @return This list for method chaining.
		 */
		public RW< V > remove() {
			return size == 0 ?
					this :
					remove( size - 1 );
		}
		
		/**
		 * Removes the element at the specified index, shifting elements to fill the gap.
		 *
		 * @param index The index of the element to remove.
		 * @return This list for method chaining.
		 */
		public RW< V > remove( int index ) {
			if( size <= index ) throw new IndexOutOfBoundsException( " index cannot be negative" );
			
			size           = Array.resize( values, values, index, size, -1 );
			values[ size ] = null;
			return this;
		}
		
		/**
		 * Removes the element at the specified index by replacing it with the last element.
		 * This is faster but does not preserve element order.
		 *
		 * @param index The index of the element to remove.
		 * @return This list for method chaining.
		 */
		public RW< V > remove_fast( int index ) {
			if( index < 0 ) throw new IllegalArgumentException( " index cannot be negative" );
			
			if( size < 1 || size <= index ) return this;
			size--;
			values[ index ] = values[ size ];
			values[ size ]  = null;
			return this;
		}
		
		/**
		 * Sets the element at the end of the list, expanding if necessary.
		 *
		 * @param value The value to set.
		 * @return This list for method chaining.
		 */
		public RW< V > set1( V value ) {
			return set1( size, value );
		}
		
		/**
		 * Sets the element at the specified index, expanding the list if needed.
		 *
		 * @param index The index to set.
		 * @param value The value to set.
		 * @return This list for method chaining.
		 */
		public RW< V > set1( int index, V value ) {
			if( index < 0 ) throw new IndexOutOfBoundsException();
			if( size <= index ) {
				if( values.length <= index ) values = equal_hash_V.copyOf( values, 2 + index * 3 / 2 );
				size = index + 1;
			}
			values[ index ] = value;
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index.
		 *
		 * @param index The starting index.
		 * @param src   The elements to set.
		 * @return This list for method chaining.
		 */
		@SafeVarargs
		public final RW< V > set( int index, V... src ) {
			return set( index, src, 0, src.length );
		}
		
		/**
		 * Sets a range of elements from a source array starting at the specified index.
		 *
		 * @param index     The starting index in this list.
		 * @param src       The source array.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to set.
		 * @return This list for method chaining.
		 */
		public RW< V > set( int index, V[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( index + i, src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Removes all elements present in the specified list.
		 *
		 * @param src The list of elements to remove.
		 * @return This list for method chaining.
		 */
		public RW< V > removeAll( R< V > src ) {
			for( int i = 0, max = src.size(); i < max; i++ )
			     removeAll( src.get( i ) );
			return this;
		}
		
		/**
		 * Removes all occurrences of the specified element.
		 *
		 * @param src The element to remove.
		 * @return This list for method chaining.
		 */
		public RW< V > removeAll( V src ) {
			for( int k; ( k = indexOf( src ) ) != -1; ) remove( k );
			return this;
		}
		
		/**
		 * Removes all occurrences of the specified element using the faster removal method.
		 * This does not preserve element order.
		 *
		 * @param src The element to remove.
		 * @return This list for method chaining.
		 */
		public RW< V > removeAll_fast( V src ) {
			for( int k; ( k = indexOf( src ) ) != -1; ) remove_fast( k );
			return this;
		}
		
		/**
		 * Retains only the elements present in the specified list, removing others.
		 *
		 * @param chk The list of elements to retain.
		 * @return This list for method chaining.
		 */
		public boolean retainAll( R< V > chk ) {
			if( chk == null ) return false;
			boolean ret = false;
			for( int i = size; -1 < --i; ) {
				V v = get( i );
				if( chk.indexOf( v ) != -1 ) continue;
				removeAll( v );
				ret = true;
				if( size < i ) i = size;
			}
			return ret;
		}
		
		/**
		 * Creates a deep copy of this list.
		 *
		 * @return A cloned instance of this list.
		 */
		public RW< V > clone() {
			return ( RW< V > ) super.clone();
		}
		
		/**
		 * Removes all elements from the list, resetting it to empty.
		 *
		 * @return This list for method chaining.
		 */
		public RW< V > clear() {
			if( size < 1 ) return this;
			Arrays.fill( values, 0, size, null );
			size = 0;
			return this;
		}
		
		/**
		 * Resizes the internal array to the specified length, truncating or padding with default values.
		 *
		 * @param length The new length of the internal array.
		 * @return This list for method chaining.
		 */
		public RW< V > length( int length ) {
			if( length < 0 ) throw new IllegalArgumentException( "length cannot be negative" );
			
			if( length < 1 ) {
				values = equal_hash_V.OO;
				size   = 0;
				return this;
			}
			int old_length = values.length;
			values = equal_hash_V.copyOf( values, length );
			if( length < size ) size = length;
			
			return this;
		}
		
		/**
		 * Swaps the elements at two specified indices.
		 *
		 * @param index1 The index of the first element.
		 * @param index2 The index of the second element.
		 * @return This list for method chaining.
		 */
		public RW< V > swap( int index1, int index2 ) {
				if( index1 < 0 || index1 >= size() ) throw new IndexOutOfBoundsException( "Index1 must be non-negative and less than the list's size: " + index1 );
			if( index2 < 0 || index2 >= size() ) throw new IndexOutOfBoundsException( "Index2 must be non-negative and less than the list's size: " + index2 );
		
			final V tmp = values[ index1 ];
			values[ index1 ] = values[ index2 ];
			values[ index2 ] = tmp;
			return this;
		}
		
		/**
		 * Sets the size of the list, expanding with default values or truncating as needed.
		 *
		 * @param size The new size of the list.
		 * @return This list for method chaining.
		 */
		public RW< V > size( int size ) {
			if( size < 0 ) throw new IllegalArgumentException( "size cannot be negative" );
			
			if( size == 0 ) {
				clear();
				return this;
			}
			
			if( values.length < size ) values = equal_hash_V.copyOf( values, size );
			else if( size < this.size ) Arrays.fill( values, size, this.size, null );
			
			this.size = size;
			return this;
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Provides a type-safe {@link Array.EqualHashOf} for {@code RW} instances.
	 *
	 * @param <V> The element type of the list.
	 * @return An {@link Array.EqualHashOf} for {@code RW<V>}.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() {
		return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT;
	}
}