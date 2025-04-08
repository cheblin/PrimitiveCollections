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
 * Defines interfaces and abstract classes for lists that efficiently store data using bits,
 * with support for a designated 'null' value.
 * <p>
 * This framework allows for memory-efficient storage of lists where each element can be represented
 * using a fixed number of bits, and where null values need to be explicitly handled.
 */
public interface BitsNullList {
	
	/**
	 * Abstract base class for {@link BitsList} implementations that support a specific null value.
	 * <p>
	 * This class extends {@link BitsList.R} and provides the foundation for lists where a particular
	 * integer value is used to represent null elements.
	 */
	abstract class R extends BitsList.R {
		
		/**
		 * The integer value that represents a null element in this list.
		 */
		public final int null_val;
		
		/**
		 * Constructs a new BitsNullList.R with the specified bits per item and null value.
		 *
		 * @param bits_per_item The number of bits used to store each item in the list.
		 * @param null_val      The integer value to represent null elements.
		 */
		protected R( int bits_per_item, int null_val ) {
			super( bits_per_item );
			this.null_val = null_val;
		}
		
		/**
		 * Constructs a new BitsNullList.R with the specified bits per item, null value, and initial size.
		 *
		 * @param bits_per_item The number of bits used to store each item.
		 * @param null_val      The integer value representing null.
		 * @param size          The initial size of the list.
		 */
		protected R( int bits_per_item, int null_val, int size ) {
			super( bits_per_item, null_val, size );
			this.null_val = null_val;
		}
		
		/**
		 * Constructs a new BitsNullList.R with the specified bits per item, null value, default value, and initial size.
		 *
		 * @param bits_per_item The number of bits used to store each item.
		 * @param null_val      The integer value representing null.
		 * @param default_value The default value to initialize list elements with.
		 * @param size          The initial size of the list.
		 */
		protected R( int bits_per_item, int null_val, int default_value, int size ) {
			super( bits_per_item, default_value, size );
			this.null_val = null_val;
		}
		
		/**
		 * Checks if the element at the specified index is not the null value.
		 *
		 * @param index The index of the element to check.
		 * @return {@code true} if the element at the index is not null, {@code false} otherwise.
		 */
		public boolean hasValue( int index ) { return get( index ) != null_val; }
		
		/**
		 * Checks if this list contains the specified item.
		 *
		 * @param item The item to search for.
		 * @return {@code true} if the list contains the item, {@code false} otherwise.
		 */
		public boolean contains( long item ) { return 0 < indexOf( item ); }
		
		/**
		 * Creates and returns a shallow copy of this BitsNullList.R instance.
		 *
		 * @return A clone of this instance.
		 */
		@Override public R clone() { return ( R ) super.clone(); }
		
		/**
		 * Returns a JSON string representation of this list.
		 *
		 * @return A JSON string representing the list.
		 */
		@Override public String toString() { return toJSON(); }
		
		/**
		 * Writes the list's content as a JSON array to the provided {@link JsonWriter}.
		 * <p>
		 * Null values in the list are represented as JSON null values.
		 *
		 * @param json The JsonWriter to write the JSON representation to.
		 */
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterArray();
			
			final int size = size();
			if( 0 < size ) {
				json.preallocate( size * 4 ); // Estimate buffer size for JSON output
				
				long src = values[ 0 ];
				for( int bp = 0, max = size * bits_per_item, i = 1; bp < max; bp += bits_per_item, i++ ) {
					final int bit = BitsList.R.bit( bp );
					long value = ( BitsList.R.BITS < bit + bits_per_item ?
							BitsList.R.value( src, src = values[ BitsList.R.index( bp ) + 1 ], bit, bits_per_item, mask ) :
							BitsList.R.value( src, bit, mask ) );
					
					if( value == null_val ) json.value(); // Write JSON null for null_val
					else json.value( value );            // Write the actual value
				}
			}
			json.exitArray();
		}
	}
	
	
	/**
	 * Concrete implementation of {@link BitsNullList} that allows read and write operations.
	 * <p>
	 * This class extends {@link BitsNullList.R}
	 * providing a fully functional, mutable list with null value support.
	 */
	class RW extends R {
		/**
		 * Constructs a new RW list with the specified bits per item and null value.
		 *
		 * @param bits_per_item Number of bits used to represent each item.
		 * @param null_val      Value used to represent null.
		 */
		public RW( int bits_per_item, int null_val ) { super( bits_per_item, null_val ); }
		
		/**
		 * Constructs a new RW list with the specified bits per item, null value, and initial size.
		 * <p>
		 * If {@code size} is positive, the list is initialized with default values.
		 * If {@code size} is negative, the absolute value is used for size, and the list is not initialized with default values.
		 *
		 * @param bits_per_item Number of bits used to represent each item.
		 * @param null_val      Value used to represent null.
		 * @param size          The initial size of the list.
		 */
		public RW( int bits_per_item, int null_val, int size ) { super( bits_per_item, null_val, size ); }
		
		/**
		 * Constructs a new RW list with the specified bits per item, null value, default value, and initial size.
		 * <p>
		 * If {@code size} is positive, the list is initialized with the provided {@code default_value}.
		 * If {@code size} is negative, the absolute value is used for size, and the list is not initialized with default values.
		 *
		 * @param bits_per_item Number of bits used to represent each item.
		 * @param null_val      Value used to represent null.
		 * @param default_value The default value to initialize elements with.
		 * @param size          The initial size of the list.
		 */
		public RW( int bits_per_item, int null_val, int default_value, int size ) { super( bits_per_item, null_val, default_value, size ); }
		
		/**
		 * Constructs a new RW list with the specified bits per item, null value, nullable default value, and initial size.
		 * <p>
		 * If {@code default_value} is null, {@code null_val} is used as the default value.
		 * If {@code size} is positive, the list is initialized with the provided (or null-substituted) {@code default_value}.
		 * If {@code size} is negative, the absolute value is used for size, and the list is not initialized with default values.
		 *
		 * @param bits_per_item Number of bits used to represent each item.
		 * @param null_val      Value used to represent null.
		 * @param default_value The default value to initialize elements with. If null, null_val is used.
		 * @param size          The initial size of the list.
		 */
		public RW( int bits_per_item, int null_val, Integer default_value, int size ) {
			super( bits_per_item, null_val, default_value == null ?
					null_val :
					default_value, size );
		}
		
		/**
		 * Adds a Byte value to the end of the list. Null Byte values are converted to the null representation.
		 *
		 * @param value The Byte value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( Byte value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds a Character value to the end of the list. Null Character values are converted to the null representation.
		 *
		 * @param value The Character value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( Character value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds a Short value to the end of the list. Null Short values are converted to the null representation.
		 *
		 * @param value The Short value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( Short value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds an Integer value to the end of the list. Null Integer values are converted to the null representation.
		 *
		 * @param value The Integer value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( Integer value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds a Long value to the end of the list. Null Long values are converted to the null representation.
		 *
		 * @param value The Long value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( Long value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds a Byte value at the specified index in the list. Null Byte values are converted to the null representation.
		 *
		 * @param index The index at which to add the value.
		 * @param value The Byte value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( int index, Byte value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds a Character value at the specified index in the list. Null Character values are converted to the null representation.
		 *
		 * @param index The index at which to add the value.
		 * @param value The Character value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( int index, Character value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds a Short value at the specified index in the list. Null Short values are converted to the null representation.
		 *
		 * @param index The index at which to add the value.
		 * @param value The Short value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( int index, Short value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds an Integer value at the specified index in the list. Null Integer values are converted to the null representation.
		 *
		 * @param index The index at which to add the value.
		 * @param value The Integer value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( int index, Integer value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds a Long value at the specified index in the list. Null Long values are converted to the null representation.
		 *
		 * @param index The index at which to add the value.
		 * @param value The Long value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( int index, Long value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Adds a long value to the end of the list.
		 *
		 * @param value The long value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( long value ) { return add1( size, value & 0xFF ); }
		
		/**
		 * Adds a long value at the specified index. If the index is within the current size, it inserts/replaces at that index.
		 * If the index is beyond the current size, it effectively sets the value at that index, potentially increasing the size.
		 *
		 * @param index The index at which to add or set the value.
		 * @param src   The long value to add.
		 * @return This RW instance for method chaining.
		 */
		public RW add1( int index, long src ) {
			if( index < size ) add( this, index, src ); // Insert if within bounds
			else set1( index, src );                    // Set if out of bounds, potentially extending list
			return this;
		}
		
		/**
		 * Removes the first occurrence of the given Byte value from the list. Null Byte values are treated as the null representation.
		 *
		 * @param value The Byte value to remove.
		 * @return This RW instance for method chaining.
		 */
		public RW remove( Byte value ) {
			remove( this, value == null ?
					null_val :
					value & 0xFF );
			return this;
		}
		
		/**
		 * Removes the first occurrence of the given Character value from the list. Null Character values are treated as the null representation.
		 *
		 * @param value The Character value to remove.
		 * @return This RW instance for method chaining.
		 */
		public RW remove( Character value ) {
			remove( this, value == null ?
					null_val :
					value & 0xFF );
			return this;
		}
		
		/**
		 * Removes the first occurrence of the given Short value from the list. Null Short values are treated as the null representation.
		 *
		 * @param value The Short value to remove.
		 * @return This RW instance for method chaining.
		 */
		public RW remove( Short value ) {
			remove( this, value == null ?
					null_val :
					value & 0xFF );
			return this;
		}
		
		/**
		 * Removes the first occurrence of the given Integer value from the list. Null Integer values are treated as the null representation.
		 *
		 * @param value The Integer value to remove.
		 * @return This RW instance for method chaining.
		 */
		public RW remove( Integer value ) {
			remove( this, value == null ?
					null_val :
					value & 0xFF );
			return this;
		}
		
		/**
		 * Removes the first occurrence of the given Long value from the list. Null Long values are treated as the null representation.
		 *
		 * @param value The Long value to remove.
		 * @return This RW instance for method chaining.
		 */
		public RW remove( Long value ) {
			remove( this, value == null ?
					null_val :
					value & 0xFF );
			return this;
		}
		
		/**
		 * Removes the first occurrence of the given int value from the list.
		 *
		 * @param value The int value to remove.
		 * @return This RW instance for method chaining.
		 */
		public RW remove( int value ) {
			remove( this, value );
			return this;
		}
		
		/**
		 * Removes the element at the specified index from the list.
		 *
		 * @param item The index of the element to remove.
		 * @return This RW instance for method chaining.
		 */
		public RW removeAt( int item ) {
			removeAt( this, item );
			return this;
		}
		
		/**
		 * Sets the element at the specified index to the given Byte value. Null Byte values are converted to the null representation.
		 *
		 * @param index The index of the element to set.
		 * @param value The Byte value to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set1( int index, Byte value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to the given Character value. Null Character values are converted to the null representation.
		 *
		 * @param index The index of the element to set.
		 * @param value The Character value to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set1( int index, Character value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to the given Short value. Null Short values are converted to the null representation.
		 *
		 * @param index The index of the element to set.
		 * @param value The Short value to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set1( int index, Short value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to the given Integer value. Null Integer values are converted to the null representation.
		 *
		 * @param index The index of the element to set.
		 * @param value The Integer value to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set1( int index, Integer value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to the given Long value. Null Long values are converted to the null representation.
		 *
		 * @param index The index of the element to set.
		 * @param value The Long value to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set1( int index, Long value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to the given long value.
		 *
		 * @param index The index of the element to set.
		 * @param value The long value to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set1( int index, long value ) {
			set1( this, index, value );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given Byte values.
		 * Null Byte values are converted to the null representation.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of Byte values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, Byte... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[ i ] == null ?
					     null_val :
					     values[ i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with values from a source Byte array.
		 * Null Byte values are converted to the null representation.
		 *
		 * @param index     The starting index to set values in this list.
		 * @param values    The source array of Byte values.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of values to copy from the source array.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, Byte[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, values[ src_index + i ] == null ?
					     null_val :
					     values[ src_index + i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given Character values.
		 * Null Character values are converted to the null representation.
		 *
		 * @param item   The starting index to set values.
		 * @param values An array of Character values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int item, Character... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, item + i, values[ i ] == null ?
					     null_val :
					     values[ i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given Short values.
		 * Null Short values are converted to the null representation.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of Short values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, Short... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[ i ] == null ?
					     null_val :
					     values[ i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given Integer values.
		 * Null Integer values are converted to the null representation.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of Integer values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, Integer... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[ i ] == null ?
					     null_val :
					     values[ i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given Long values.
		 * Null Long values are converted to the null representation.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of Long values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, Long... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[ i ] == null ?
					     null_val :
					     ( int ) ( values[ i ] & 0xFF ) ); // Casting to int due to set1 signature
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given byte values.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of byte values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, byte... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with values from a source byte array.
		 *
		 * @param index     The starting index to set values in this list.
		 * @param src       The source array of byte values.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of values to copy from the source array.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, byte[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given char values.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of char values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, char... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given short values.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of short values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, short... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given int values.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of int values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, int... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets a range of elements starting at the specified index with the given long values.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of long values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, long... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Adjusts the internal array length to match the current size, potentially saving memory if the list's capacity is larger than its size.
		 *
		 * @return This RW instance for method chaining.
		 */
		public RW fit() { return length( size ); }
		
		/**
		 * Sets the internal array length to accommodate the specified number of items.
		 * If {@code items} is less than 1, the internal array is cleared.
		 *
		 * @param items The desired number of items the internal array should accommodate.
		 * @return This RW instance for method chaining.
		 */
		public RW length( int items ) {
			if( items < 1 ) {
				values = Array.EqualHashOf._longs.O; // Clear the array if items is less than 1
				size   = 0;
			}
			else length_( items );                    // Adjust length if items is positive
			return this;
		}
		
		/**
		 * Sets the size of the list.
		 * <p>
		 * If the new {@code size} is smaller than the current size, elements at the end are effectively removed.
		 * If the new {@code size} is larger, new elements are added and initialized with the default value.
		 * If {@code size} is less than 1, the list is cleared.
		 *
		 * @param size The new size of the list.
		 * @return This RW instance for method chaining.
		 */
		public RW size( int size ) {
			if( size < 1 ) clear();                        // Clear if size is less than 1
			else if( this.size < size ) set1( size - 1, default_value ); // Extend with default value if size increases
			else this.size = size;                     // Just update size if it's within bounds or decreasing
			return this;
		}
		
		/**
		 * Creates and returns a shallow copy of this RW instance.
		 *
		 * @return A clone of this instance.
		 */
		@Override public RW clone() { return ( RW ) super.clone(); }
	}
}