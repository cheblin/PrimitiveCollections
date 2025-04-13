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
 * Defines interfaces and abstract classes for bit-packed lists that efficiently store integers with support for a designated null value.
 * <p>
 * This framework extends {@link BitsList} to provide memory-efficient storage of lists where each element occupies a fixed number of bits (1 to 7),
 * and a specific integer value is reserved to represent null. The bit-packed storage uses an array of {@code long}s, with each item’s bit size
 * determined at construction. The null value feature allows explicit handling of missing or undefined elements.
 * <p>
 * <b>Restrictions:</b>
 * <ul>
 *   <li>{@code bits_per_item} must be between 1 and 7 (inclusive), as inherited from {@link BitsList.R}. Values outside this range will throw an
 *       {@code IllegalArgumentException}.</li>
 *   <li>The maximum value per item is limited by the number of bits (e.g., 2^bits_per_item - 1), and all values (including {@code null_val} and
 *       {@code default_value}) are masked to fit within this range.</li>
 *   <li>Negative indices are not allowed and will throw an {@code IndexOutOfBoundsException} in methods that access or modify the list.</li>
 * </ul>
 */
public interface BitsNullList {
	
	/**
	 * Abstract base class for {@link BitsList} implementations that support a designated null value.
	 * <p>
	 * Extends {@link BitsList.R} to add support for a specific integer value representing null elements. This class provides the core functionality
	 * for reading list contents, with bit-packed storage in an array of {@code long}s. Each item occupies a fixed number of bits (1 to 7), and the
	 * null value is used to distinguish missing or undefined elements.
	 * <p>
	 * <b>Restrictions:</b>
	 * <ul>
	 *   <li>{@code bits_per_item} must be between 1 and 7 (inclusive). Values outside this range will throw an {@code IllegalArgumentException}
	 *       during construction.</li>
	 *   <li>{@code null_val} must fit within the range defined by {@code bits_per_item} (0 to 2^bits_per_item - 1). It is masked to ensure compliance.</li>
	 *   <li>{@code default_value}, if specified, must also fit within the same range and is masked accordingly.</li>
	 *   <li>Indices must be non-negative and within the current size (0 to {@code size-1}) for methods like {@code get}. Otherwise, an
	 *       {@code IndexOutOfBoundsException} is thrown.</li>
	 * </ul>
	 */
	abstract class R extends BitsList.R {
		
		/**
		 * The integer value that represents a null element in this list.
		 * <p>
		 * This value is masked to fit within {@code bits_per_item} bits (i.e., 0 to 2^bits_per_item - 1).
		 */
		public final int null_val;
		
		/**
		 * Constructs an empty {@code BitsNullList.R} with the specified bits per item and null value.
		 *
		 * @param bits_per_item The number of bits used to store each item (must be 1 to 7).
		 * @param null_val      The integer value to represent null elements, masked to fit within {@code bits_per_item}.
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		protected R( int bits_per_item, int null_val ) {
			super( bits_per_item );
			this.null_val = ( int ) ( null_val & mask );
		}
		
		/**
		 * Constructs a {@code BitsNullList.R} with the specified bits per item, null value, and initial size.
		 * <p>
		 * The list is initialized with the null value as the default if size is positive.
		 *
		 * @param bits_per_item The number of bits used to store each item (must be 1 to 7).
		 * @param null_val      The integer value representing null, masked to fit within {@code bits_per_item}.
		 * @param size          If positive, sets the initial number of items to this value and fills the
		 *                      list with the effective `default_value`. If negative or zero, sets the initial number of items to `abs(size)` (no filling occurs).
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		protected R( int bits_per_item, int null_val, int size ) {
			super( bits_per_item, null_val, size );
			this.null_val = ( int ) ( null_val & mask );
		}
		
		/**
		 * Constructs a {@code BitsNullList.R} with the specified bits per item, null value, default value, and initial size.
		 * <p>
		 * If size is positive, the list is populated with the default value; if negative, the absolute value is used, and the list remains empty.
		 *
		 * @param bits_per_item The number of bits used to store each item (must be 1 to 7).
		 * @param null_val      The integer value representing null, masked to fit within {@code bits_per_item}.
		 * @param default_value The default value for initializing list elements, masked to fit within {@code bits_per_item}.
		 * @param size          If positive, sets the initial number of items to this value and fills the
		 *                      list with the effective `default_value`. If negative or zero, sets the initial number of items to `abs(size)` (no filling occurs).
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		protected R( int bits_per_item, int null_val, int default_value, int size ) {
			super( bits_per_item, default_value, size );
			this.null_val = ( int ) ( null_val & mask );
		}
		
		/**
		 * Checks if the element at the specified index is not the null value.
		 *
		 * @param index The index to check (0 to {@code size-1}).
		 * @return {@code true} if the element at the index is not {@code null_val}, {@code false} otherwise.
		 * @throws IndexOutOfBoundsException if {@code index} is negative or exceeds {@code size-1}.
		 */
		public boolean hasValue( int index ) { return get( index ) != null_val; }
		
		/**
		 * Checks if this list contains the specified value, excluding the null value unless explicitly searched for.
		 *
		 * @param value The value to search for (masked to fit within {@code bits_per_item}).
		 * @return {@code true} if the list contains the value, {@code false} otherwise.
		 */
		public boolean contains( long value ) { return -1 != indexOf( value ); }
		
		public boolean contains( Integer value ) { return -1 != indexOf( value ); }
		
		public int indexOf( Integer value ) {
			return indexOf( value == null ?
					         null_val :
					         value);
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code BitsNullList.R} instance.
		 * <p>
		 * The cloned instance shares the same {@code bits_per_item} and {@code null_val} constraints.
		 *
		 * @return A new {@code R} instance identical to this one.
		 */
		@Override public R clone() { return ( R ) super.clone(); }
		
		/**
		 * Returns a JSON-formatted string representation of the list.
		 * <p>
		 * Elements equal to {@code null_val} are represented as JSON {@code null}.
		 *
		 * @return A JSON string representing the list contents.
		 */
		@Override public String toString() { return toJSON(); }
		
		/**
		 * Serializes the list’s contents as a JSON array to the provided {@link JsonWriter}.
		 * <p>
		 * Elements equal to {@code null_val} are written as JSON {@code null}, while other values are written as integers.
		 *
		 * @param json The {@code JsonWriter} to write the JSON representation to.
		 */
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterArray();
			
			final int size = size();
			if( 0 < size ) {
				json.preallocate( size * 4 ); // Estimate buffer size for JSON output
				
				long src = values[ 0 ];
				for( int bp = 0, max = size * bits_per_item, i = 1; bp < max; bp += bits_per_item, i++ ) {
					final int bit = BitsList.R.bit( bp );
					long value =
							( BitsList.R.BITS < bit + bits_per_item ?
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
	 * Concrete implementation of {@link BitsNullList} that supports both read and write operations with null value handling.
	 * <p>
	 * Extends {@link BitsNullList.R} to provide a fully mutable list where each element occupies a fixed number of bits (1 to 7),
	 * and a designated {@code null_val} represents null elements. Methods support chaining for fluent usage and handle null inputs
	 * by converting them to {@code null_val}.
	 * <p>
	 * <b>Restrictions:</b>
	 * <ul>
	 *   <li>{@code bits_per_item} must be between 1 and 7 (inclusive). Values outside this range will throw an {@code IllegalArgumentException}
	 *       during construction.</li>
	 *   <li>{@code null_val} and {@code default_value} must fit within the range defined by {@code bits_per_item} (0 to 2^bits_per_item - 1).
	 *       They are masked to ensure compliance.</li>
	 *   <li>Indices must be non-negative in all modification methods (e.g., {@code add1}, {@code set1}). Negative indices throw an
	 *       {@code IndexOutOfBoundsException}.</li>
	 *   <li>Array-based setters (e.g., {@code set(int, byte...)}) must not exceed source array bounds, or an {@code IndexOutOfBoundsException}
	 *       is thrown.</li>
	 * </ul>
	 */
	class RW extends R {
		/**
		 * Constructs an empty {@code RW} list with the specified bits per item and null value.
		 *
		 * @param bits_per_item Number of bits per item (must be 1 to 7).
		 * @param null_val      Value used to represent null, masked to fit within {@code bits_per_item}.
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		public RW( int bits_per_item, int null_val ) { super( bits_per_item, null_val ); }
		
		/**
		 * Constructs an {@code RW} list with the specified bits per item, null value, and initial size.
		 * <p>
		 * If {@code size} is positive, the list is initialized with {@code null_val} as the default value. If negative,
		 * the absolute value is used, and the list remains empty.
		 *
		 * @param bits_per_item Number of bits per item (must be 1 to 7).
		 * @param null_val      Value used to represent null, masked to fit within {@code bits_per_item}.
		 * @param size          If positive, sets the initial number of items to this value and fills the
		 *                      list with the effective `default_value`. If negative or zero, sets the initial number of items to `abs(size)` (no filling occurs).
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		public RW( int bits_per_item, int null_val, int size ) { super( bits_per_item, null_val, size ); }
		
		/**
		 * Constructs an {@code RW} list with the specified bits per item, null value, default value, and initial size.
		 * <p>
		 * If {@code size} is positive, the list is initialized with {@code default_value}. If negative, the absolute value is used,
		 * and the list remains empty.
		 *
		 * @param bits_per_item Number of bits per item (must be 1 to 7).
		 * @param null_val      Value used to represent null, masked to fit within {@code bits_per_item}.
		 * @param default_value Default value for elements, masked to fit within {@code bits_per_item}.
		 * @param size          If positive, sets the initial number of items to this value and fills the
		 *                      list with the effective `default_value`. If negative or zero, sets the initial number of items to `abs(size)` (no filling occurs).
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		public RW( int bits_per_item, int null_val, int default_value, int size ) { super( bits_per_item, null_val, default_value, size ); }
		
		/**
		 * Constructs an {@code RW} list with the specified bits per item, null value, nullable default value, and initial size.
		 * <p>
		 * If {@code default_value} is null, {@code null_val} is used as the default. If {@code size} is positive, the list is initialized
		 * with the chosen default value. If negative, the absolute value is used, and the list remains empty.
		 *
		 * @param bits_per_item Number of bits per item (must be 1 to 7).
		 * @param null_val      Value used to represent null, masked to fit within {@code bits_per_item}.
		 * @param default_value Default value for elements (if null, uses {@code null_val}), masked to fit within {@code bits_per_item}.
		 * @param size          If positive, sets the initial number of items to this value and fills the
		 *                      list with the effective `default_value`. If negative or zero, sets the initial number of items to `abs(size)` (no filling occurs).
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		public RW( int bits_per_item, int null_val, Integer default_value, int size ) {
			super( bits_per_item, null_val, default_value == null ?
					null_val :
					default_value, size );
		}
		
		/**
		 * Appends a {@code Byte} value to the end of the list. Null values are stored as {@code null_val}.
		 *
		 * @param value The {@code Byte} value to append (or null).
		 * @return This {@code RW} instance for chaining.
		 */
		public RW add1( Byte value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Appends a {@code Character} value to the end of the list. Null values are stored as {@code null_val}.
		 *
		 * @param value The {@code Character} value to append (or null).
		 * @return This {@code RW} instance for chaining.
		 */
		public RW add1( Character value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Appends a {@code Short} value to the end of the list. Null values are stored as {@code null_val}.
		 *
		 * @param value The {@code Short} value to append (or null).
		 * @return This {@code RW} instance for chaining.
		 */
		public RW add1( Short value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Appends an {@code Integer} value to the end of the list. Null values are stored as {@code null_val}.
		 *
		 * @param value The {@code Integer} value to append (or null).
		 * @return This {@code RW} instance for chaining.
		 */
		public RW add1( Integer value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Appends a {@code Long} value to the end of the list. Null values are stored as {@code null_val}.
		 *
		 * @param value The {@code Long} value to append (or null).
		 * @return This {@code RW} instance for chaining.
		 */
		public RW add1( Long value ) {
			return add1( size, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Inserts a {@code Byte} value at the specified index. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to insert at (0 to {@code size}).
		 * @param value The {@code Byte} value to insert (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW add1( int index, Byte value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Inserts a {@code Character} value at the specified index. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to insert at (0 to {@code size}).
		 * @param value The {@code Character} value to insert (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW add1( int index, Character value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Inserts a {@code Short} value at the specified index. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to insert at (0 to {@code size}).
		 * @param value The {@code Short} value to insert (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW add1( int index, Short value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Inserts an {@code Integer} value at the specified index. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to insert at (0 to {@code size}).
		 * @param value The {@code Integer} value to insert (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW add1( int index, Integer value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Inserts a {@code Long} value at the specified index. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to insert at (0 to {@code size}).
		 * @param value The {@code Long} value to insert (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW add1( int index, Long value ) {
			return add1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Appends a {@code long} value to the end of the list.
		 *
		 * @param value The {@code long} value to append, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW add1( long value ) { return add1( size, value & 0xFF ); }
		
		/**
		 * Adds or sets a {@code long} value at the specified index. If within current size, inserts; if beyond, sets and extends.
		 *
		 * @param index The index to add/set at (0 or greater).
		 * @param src   The {@code long} value to add/set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW add1( int index, long src ) {
			if( index < size ) add( this, index, src ); // Insert if within bounds
			else set1( index, src );                    // Set if out of bounds, potentially extending list
			return this;
		}
		
		
		/**
		 * Removes the first occurrence of an {@code Integer} value. Null values are treated as {@code null_val}.
		 *
		 * @param value The {@code Integer} value to remove (or null).
		 * @return This {@code RW} instance for chaining.
		 */
		public RW remove( Integer value ) {
			removeAt( indexOf( value == null ?
					                   null_val :
					                   value & 0xFF ) );
			return this;
		}
		
		
		/**
		 * Removes the first occurrence of an {@code int} value.
		 *
		 * @param value The {@code int} value to remove, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW remove( long value ) {
			removeAt( indexOf( value ) );
			return this;
		}
		
		/**
		 * Removes the all occurrence of an {@code Integer} value. Null values are treated as {@code null_val}.
		 *
		 * @param value The {@code Integer} value to remove (or null).
		 * @return This {@code RW} instance for chaining.
		 */
		public RW removeAll( Integer value ) {
			remove( value == null ?
					        null_val :
					        value & 0xFF );
			return this;
		}
		
		/**
		 * Removes the all occurrence of an {@code int} value.
		 *
		 * @param value The {@code int} value to remove, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance for chaining.
		 */
		public RW removeAll( long value ) {
			remove( this, value );
			return this;
		}
		
		/**
		 * Removes the element at the specified index.
		 *
		 * @param item The index to remove (0 to {@code size-1}).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code item} is negative or exceeds {@code size-1}.
		 */
		public RW removeAt( int item ) {
			removeAt( this, item );
			return this;
		}
		
		/**
		 * Sets the element at the specified index to a {@code Byte} value. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to set (0 or greater).
		 * @param value The {@code Byte} value to set (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set1( int index, Byte value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to a {@code Character} value. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to set (0 or greater).
		 * @param value The {@code Character} value to set (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set1( int index, Character value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to a {@code Short} value. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to set (0 or greater).
		 * @param value The {@code Short} value to set (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set1( int index, Short value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to an {@code Integer} value. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to set (0 or greater).
		 * @param value The {@code Integer} value to set (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set1( int index, Integer value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to a {@code Long} value. Null values are stored as {@code null_val}.
		 *
		 * @param index The index to set (0 or greater).
		 * @param value The {@code Long} value to set (or null).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set1( int index, Long value ) {
			return set1( index, value == null ?
					null_val :
					value & 0xFF );
		}
		
		/**
		 * Sets the element at the specified index to a {@code long} value.
		 *
		 * @param index The index to set (0 or greater).
		 * @param value The {@code long} value to set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set1( int index, long value ) {
			set1( this, index, value );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code Byte} values. Null values are stored as {@code null_val}.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code Byte} values to set (may include nulls).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set_( int index, Byte... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[ i ] == null ?
					     null_val :
					     values[ i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets multiple elements from a {@code Byte} array starting at the specified index. Null values are stored as {@code null_val}.
		 *
		 * @param index     The starting index in the list (0 or greater).
		 * @param values    The source {@code Byte} array (may include nulls).
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative or source bounds are exceeded.
		 */
		public RW set__( int index, Byte[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, values[ src_index + i ] == null ?
					     null_val :
					     values[ src_index + i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code Character} values. Null values are stored as {@code null_val}.
		 *
		 * @param item   The starting index (0 or greater).
		 * @param values The {@code Character} values to set (may include nulls).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code item} is negative.
		 */
		public RW set_( int item, Character... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, item + i, values[ i ] == null ?
					     null_val :
					     values[ i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code Short} values. Null values are stored as {@code null_val}.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code Short} values to set (may include nulls).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set_( int index, Short... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[ i ] == null ?
					     null_val :
					     values[ i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code Integer} values. Null values are stored as {@code null_val}.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code Integer} values to set (may include nulls).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set_( int index, Integer... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[ i ] == null ?
					     null_val :
					     values[ i ] & 0xFF );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code Long} values. Null values are stored as {@code null_val}.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code Long} values to set (may include nulls).
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set_( int index, Long... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[ i ] == null ?
					     null_val :
					     ( int ) ( values[ i ] & 0xFF ) ); // Casting to int due to set1 signature
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code byte} values.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code byte} values to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set( int index, byte... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple elements from a {@code byte} array starting at the specified index.
		 *
		 * @param index     The starting index in the list (0 or greater).
		 * @param src       The source {@code byte} array.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative or source bounds are exceeded.
		 */
		public RW set( int index, byte[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code char} values.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code char} values to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set( int index, char... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code short} values.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code short} values to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set( int index, short... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code int} values.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code int} values to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set( int index, int... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple elements starting at the specified index with {@code long} values.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The {@code long} values to set.
		 * @return This {@code RW} instance for chaining.
		 * @throws IndexOutOfBoundsException if {@code index} is negative.
		 */
		public RW set( int index, long... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Trims the internal array capacity to match the current size, potentially reducing memory usage.
		 *
		 * @return This {@code RW} instance for chaining.
		 */
		public RW fit() { return length( size ); }
		
		/**
		 * Adjusts the internal array capacity to accommodate the specified number of items.
		 * <p>
		 * If {@code items} is less than 1, the list is cleared and capacity is set to zero.
		 *
		 * @param items The desired capacity in items (if less than 1, clears the list).
		 * @return This {@code RW} instance for chaining.
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
		 * Sets the size of the list, extending with {@code default_value} or truncating as needed.
		 * <p>
		 * If {@code size} is less than 1, the list is cleared. If greater than the current size, new elements are added with
		 * {@code default_value}.
		 *
		 * @param size The desired size (if less than 1, clears the list).
		 * @return This {@code RW} instance for chaining.
		 */
		public RW size( int size ) {
			if( size < 1 ) clear();                        // Clear if size is less than 1
			else if( this.size < size ) set1( size - 1, default_value ); // Extend with default value if size increases
			else this.size = size;                     // Just update size if it's within bounds or decreasing
			return this;
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code RW} instance.
		 * <p>
		 * The cloned instance retains the same {@code bits_per_item} and {@code null_val} constraints.
		 *
		 * @return A new {@code RW} instance identical to this one.
		 */
		@Override public RW clone() { return ( RW ) super.clone(); }
	}
}