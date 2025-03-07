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
 * Interface for a bit-packed list implementation that stores integers in a space-efficient manner
 * using an array of longs. Each item occupies a fixed number of bits, specified at creation.
 */
public interface BitsList {
	
	
	/**
	 * Abstract base class for {@code BitsList}, providing core functionality for a list of bit-packed integers.
	 * Each item occupies a fixed number of bits, defined at construction, stored in an array of {@code long}s.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Array storing the bit-packed data.
		 */
		protected long[] values = Array.EqualHashOf._longs.O;
		
		/**
		 * Number of items currently stored in the list.
		 */
		protected int size = 0;
		
		/**
		 * Mask to isolate the bits of each item.
		 */
		protected final long mask;
		
		/**
		 * Number of bits per item (1 to BITS).
		 */
		public final int bits;
		
		/**
		 * Default value for new items, masked to fit within {@code bits} per item.
		 */
		public final int default_value;
		
		/**
		 * Constructs a {@code BitsList} with the specified number of bits per item.
		 * Initializes with an empty list and a default value of 0.
		 *
		 * @param bits_per_item Number of bits per item (1 to BITS).
		 */
		protected R( int bits_per_item ) {
			mask          = mask( bits = bits_per_item );
			default_value = 0;
		}
		
		/**
		 * Constructs a {@code BitsList} with the specified bits per item and initial capacity.
		 * Initializes with zero size and a default value of 0.
		 *
		 * @param bits_per_item Number of bits per item (1 to BITS).
		 * @param length        Initial capacity in items.
		 */
		protected R( int bits_per_item, int length ) {
			mask          = mask( bits = bits_per_item );
			values        = new long[ len4bits( length * bits ) ];
			default_value = 0;
		}
		
		/**
		 * Constructs a {@code BitsList} with specified bits per item, default value, and size.
		 * If {@code size} is negative, its absolute value is used. Populates the list with the default value if non-zero.
		 *
		 * @param bits_per_item Number of bits per item (1 to BITS).
		 * @param default_value Default value for items, masked to fit within {@code bits_per_item}.
		 * @param size          Initial size in items (if negative, uses absolute value).
		 */
		protected R( int bits_per_item, int default_value, int size ) {
			mask               = mask( bits = bits_per_item );
			this.size          = size < 0 ?
					-size :
					size;
			values             = new long[ len4bits( this.size * bits ) ];
			this.default_value = ( int ) ( default_value & mask );
			if( this.default_value != 0 && this.size > 0 )
				for( int i = 0; i < this.size; i++ ) append( this, i, this.default_value );
		}
		
		/**
		 * Returns the number of items in the list.
		 *
		 * @return The current size of the list.
		 */
		public int size() {
			return size;
		}
		
		/**
		 * Returns the current capacity of the list in items.
		 *
		 * @return The capacity, calculated as the number of items that can fit in the {@code values} array.
		 */
		public int length() {
			return values.length * BITS / bits;
		}
		
		/**
		 * Adjusts the storage capacity of the list.
		 * If {@code items > 0}, sets the capacity to at least {@code items}, trimming excess if necessary.
		 * If {@code items <= 0}, clears the list and allocates capacity for {@code -items} items.
		 *
		 * @param items Desired capacity in items.
		 */
		protected void length_( int items ) {
			if( 0 < items ) {
				if( items < size ) size = items;
				int new_length = len4bits( items * bits );
				if( values.length != new_length ) values = Arrays.copyOf( values, new_length );
			}
			else {
				int new_length = len4bits( -items * bits );
				if( values.length != new_length ) {
					values = new_length == 0 ?
							Array.EqualHashOf._longs.O :
							new long[ new_length ];
					size   = 0;
				}
				else clear();
			}
		}
		
		/**
		 * Clears all items in the list by setting their bits to zero.
		 * Resets the size to 0.
		 */
		protected void clear() {
			BitList.RW.clearBits( values, 0, size * bits );
			size = 0;
		}
		
		/**
		 * Checks if the list is empty.
		 *
		 * @return {@code true} if the list has no items, {@code false} otherwise.
		 */
		public boolean isEmpty() {
			return size == 0;
		}
		
		/**
		 * Computes a hash code based on the list's contents.
		 *
		 * @return The hash code value for this list.
		 */
		@Override
		public int hashCode() {
			int last_long = index( size * bits );
			int hash      = Array.hash( 149989999, values[ last_long ] & mask( bit( size * bits ) ) );
			for( int i = last_long - 1; i >= 0; i-- ) hash = Array.hash( hash, values[ i ] );
			return Array.finalizeHash( hash, size() );
		}
		
		/**
		 * Checks if this list is equal to another object.
		 *
		 * @param other The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object other ) {
			return other != null && getClass() == other.getClass() && equals( getClass().cast( other ) );
		}
		
		/**
		 * Checks if this list is equal to another {@code R} instance.
		 * Compares size and all bit-packed values.
		 *
		 * @param other The {@code R} instance to compare with.
		 * @return {@code true} if the lists are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == null || other.size != size ) return false;
			int  last_long = index( size * bits );
			long last_mask = mask( bit( size * bits ) );
			if( ( values[ last_long ] & last_mask ) != ( other.values[ last_long ] & last_mask ) ) return false;
			for( int i = last_long - 1; i >= 0; i-- ) if( values[ i ] != other.values[ i ] ) return false;
			return true;
		}
		
		/**
		 * Retrieves the value of the last item in the list.
		 *
		 * @return The value of the last item as a {@code long}.
		 * @throws IndexOutOfBoundsException If the list is empty.
		 */
		public byte get() {
			if( size == 0 ) throw new IndexOutOfBoundsException( "List is empty" );
			return get( size - 1 );
		}
		
		/**
		 * Retrieves the value at the specified index.
		 * Extracts the value from the bit-packed array, handling cases where it spans two {@code long}s.
		 *
		 * @param item The index of the item (0 to {@code size-1}).
		 * @return The value at the index as a {@code long}.
		 * @throws IndexOutOfBoundsException If {@code item} is out of bounds.
		 */
		public byte get( int item ) {
			if( item < 0 || item >= size ) throw new IndexOutOfBoundsException( "Index: " + item + ", Size: " + size );
			int bit_pos = item * bits;
			int index   = index( bit_pos );
			int bit     = bit( bit_pos );
			return bit + bits > BITS ?
					value( values[ index ], values[ index + 1 ], bit, bits, mask ) :
					value( values[ index ], bit, mask );
		}
		
		/**
		 * Adds a value to the end of the list.
		 * Delegates to {@code set1} to append the value.
		 *
		 * @param dst The {@code BitsList} instance to modify.
		 * @param src The value to add, masked to fit within {@code bits} per item.
		 */
		protected static void add( R dst, long src ) {
			set1( dst, dst.size, src );
		}
		
		/**
		 * Adds a value at the specified index, shifting subsequent elements to the right.
		 * Extends the list if the index equals the current size; otherwise, inserts within the list.
		 *
		 * @param dst   The {@code BitsList} instance to modify.
		 * @param item  The index to insert at (0 to {@code size}).
		 * @param value The value to insert, masked to fit within {@code bits} per item.
		 */
		protected static void add( R dst, int item, long value ) {
			if( item < 0 ) throw new IndexOutOfBoundsException( "Index: " + item );
			if( item >= dst.size ) {
				set1( dst, item, value );
				return;
			}
			int total_bits      = dst.size * dst.bits;
			int insert_bit      = item * dst.bits;
			int required_length = len4bits( total_bits + dst.bits );
			
			
			dst.values = BitList.RW.shiftLeft( dst.values,
			                                   dst.values.length < required_length ?
					                                   new long[ Math.max( dst.values.length + dst.values.length / 2, required_length ) ] :
					                                   dst.values,
			                                   0, dst.values.length * BITS, insert_bit, total_bits, dst.bits, true );
			dst.size++;
			set1( dst, item, value );
		}
		
		/**
		 * Sets multiple values starting at the specified index.
		 * Values are set in reverse order to handle overlapping indices correctly.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param from The starting index (0 or greater).
		 * @param src  The array of values to set.
		 */
		protected static void set( R dst, int from, byte... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length - 1; i >= 0; i-- ) set1( dst, from + i, src[ i ] );
		}
		
		protected static void set( R dst, int from, char... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length - 1; i >= 0; i-- ) set1( dst, from + i, src[ i ] );
		}
		
		protected static void set( R dst, int from, short... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length - 1; i >= 0; i-- ) set1( dst, from + i, src[ i ] );
		}
		
		protected static void set( R dst, int from, int... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length - 1; i >= 0; i-- ) set1( dst, from + i, src[ i ] );
		}
		
		protected static void set( R dst, int from, long... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length - 1; i >= 0; i-- ) set1( dst, from + i, src[ i ] );
		}
		
		/**
		 * Sets a value at the specified index, extending the list if necessary.
		 * If the index is beyond the current size, fills intervening positions with the default value.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param item The index to set (0 or greater).
		 * @param src  The value to set, masked to fit within {@code bits} per item.
		 */
		protected static void set1( R dst, int item, long src ) {
			if( item < 0 ) throw new IndexOutOfBoundsException( "Index: " + item );
			long v       = src & dst.mask;
			int  bit_pos = item * dst.bits;
			
			
			if( item < dst.size ) {
				int index = index( bit_pos );
				int bit   = bit( bit_pos );
				int k     = BITS - bit;
				
				if( k < dst.bits ) {// Value spans two longs
					
					long mask = ~0L << bit;
					dst.values[ index ] = dst.values[ index ] & ~mask | v << bit;
					int bits_in_next = dst.bits - k;
					dst.values[ index + 1 ] = dst.values[ index + 1 ] & ~mask( bits_in_next ) | v >>> k;
				}
				else {
					// Value fits within one long
					long mask = dst.mask << bit;
					dst.values[ index ] = dst.values[ index ] & ~mask | v << bit;
				}
				return;
			}
			
			if( dst.length() <= item ) dst.length_( Math.max( dst.length() + dst.length() / 2, len4bits( bit_pos + dst.bits ) ) );
			if( dst.default_value != 0 )
				for( int i = dst.size; i < item; i++ )
				     append( dst, i, dst.default_value );
			
			append( dst, item, v );
			dst.size = item + 1;
		}
		
		/**
		 * Appends a value at the specified bit position within the array.
		 * Handles cases where the value spans two {@code long}s.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param item The index to append at.
		 * @param src  The value to append, masked to fit within {@code bits} per item.
		 */
		private static void append( R dst, int item, long src ) {
			long v       = src & dst.mask;
			int  bit_pos = item * dst.bits;
			int  index   = index( bit_pos );
			int  bit     = bit( bit_pos );
			int  k       = BITS - bit;
			if( k < dst.bits ) {
				// Value spans two longs
				long mask = ~0L << bit;
				dst.values[ index ] = dst.values[ index ] & ~mask | v << bit;
				int bits_in_next = dst.bits - k;
				dst.values[ index + 1 ] = dst.values[ index + 1 ] & ~mask( bits_in_next ) | v >>> k;
			}
			else {
				// Value fits within one long
				long mask = dst.mask << bit;
				dst.values[ index ] = dst.values[ index ] & ~mask | v << bit;
			}
		}
		
		/**
		 * Removes an item at the specified index, shifting subsequent elements to the left.
		 * If the index is the last item, simply reduces the size unless the default value requires clearing.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param item The index to remove (0 to {@code size-1}).
		 */
		protected static void removeAt( R dst, int item ) {
			if( item == dst.size - 1 ) {
				if( dst.default_value == 0 ) append( dst, item, 0 );
				dst.size--;
				return;
			}
			int from_bit = ( item + 1 ) * dst.bits;
			int to_bit   = dst.size * dst.bits;
			BitList.RW.shiftRight( dst.values, dst.values, 0, dst.values.length * BITS, from_bit, to_bit, dst.bits, true );
			dst.size--;
		}
		
		/**
		 * Creates a clone of this list.
		 *
		 * @return A new {@code R} instance that is a deep copy of this list.
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				if( dst.length() > 0 ) dst.values = values.clone();
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
				return null;
			}
		}
		
		/**
		 * Returns a JSON string representation of the list.
		 *
		 * @return The list as a JSON-formatted string.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Writes the list contents to a JSON writer.
		 * Outputs the list as an array of values.
		 *
		 * @param json The {@code JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray();
			if( size > 0 ) {
				json.preallocate( size * 4 );
				long src = values[ 0 ];
				for( int bp = 0, max = size * bits, i = 0; bp < max; bp += bits, i++ ) {
					int bit = bit( bp );
					long value = bit + bits > BITS ?
							value( src, values[ index( bp ) + 1 ], bit, bits, mask ) :
							value( src, bit, mask );
					json.value( value );
					if( bit + bits > BITS ) src = values[ index( bp ) + 1 ];
				}
			}
			json.exitArray();
		}
		
		/**
		 * Finds the first occurrence of the specified value in the list.
		 *
		 * @param value The value to search for.
		 * @return The index of the first occurrence, or -1 if not found.
		 */
		public int indexOf( long value ) {
			for( int i = 0; i < size; i++ )
				if( value == get( i ) ) return i;
			return -1;
		}
		
		/**
		 * Finds the last occurrence of the specified value in the list.
		 *
		 * @param value The value to search for.
		 * @return The index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( long value ) {
			return lastIndexOf( size, value );
		}
		
		/**
		 * Finds the last occurrence of the specified value up to the given index.
		 * Searches backward from the specified index.
		 *
		 * @param from  The starting index for the backward search (clamped to {@code size}).
		 * @param value The value to search for.
		 * @return The index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( int from, long value ) {
			for( int i = Math.min( from, size ) - 1; i >= 0; i-- ) if( value == get( i ) ) return i;
			return -1;
		}
		
		/**
		 * Removes all occurrences of the specified value from the list.
		 * Repeatedly finds and removes the last occurrence until none remain.
		 *
		 * @param dst   The {@code BitsList} instance to modify.
		 * @param value The value to remove.
		 */
		protected static void remove( R dst, long value ) {
			for( int i = dst.size - 1; i >= 0; i = dst.lastIndexOf( i, value ) ) removeAt( dst, i );
		}
		
		/**
		 * Checks if the list contains the specified value.
		 *
		 * @param value The value to check for.
		 * @return {@code true} if the value is present, {@code false} otherwise.
		 */
		public boolean contains( long value ) {
			return indexOf( value ) != -1;
		}
		
		/**
		 * Converts the list to a long array.
		 * Allocates a new array if the provided one is null or too small.
		 *
		 * @param dst The destination array; if null or insufficient, a new array is created.
		 * @return The long array containing all values, or {@code null} if the list is empty.
		 */
		public byte[] toArray( byte[] dst ) {
			if( size == 0 ) return null;
			if( dst == null || dst.length < size ) dst = new byte[ size ];
			for( int i = 0; i < size; i++ ) dst[ i ] = get( i );
			return dst;
		}
		
		/**
		 * Creates a mask with the specified number of least significant bits set to 1.
		 * Example: {@code mask(3)} returns {@code 0b111} (7 in decimal).
		 *
		 * @param bits Number of bits for the mask, ranging from 0 to BITS.
		 * @return A {@code long} mask with the specified bits set.
		 */
		static long mask( int bits ) {
			return ( 1L << bits ) - 1;
		}
		
		
		/**
		 * Computes the array index for a given bit position.
		 * Since each {@code long} holds BITS bits, the index is calculated as {@code bit_position / BITS}.
		 *
		 * @param bit_position The bit position in the array.
		 * @return The index of the {@code long} containing the bit.
		 */
		protected static int index( int bit_position ) {
			return bit_position >> 6; // 6 because 2^6 = BITS
		}
		
		/**
		 * Computes the bit offset within a {@code long} for a given bit position.
		 * This is the remainder when {@code bit_position} is divided by BITS.
		 *
		 * @param bit_position The bit position in the array.
		 * @return The bit offset (0 to 63) within the {@code long}.
		 */
		protected static int bit( int bit_position ) {
			return bit_position & 63; // 63 is 0b111111
		}
		
		/**
		 * Extracts a value from a {@code long} starting at a specified bit position.
		 * The value is isolated using the provided mask.
		 *
		 * @param src  Source {@code long} value containing the bits.
		 * @param bit  Starting bit position within the {@code long} (0 to 63).
		 * @param mask Mask to isolate the desired bits.
		 * @return The extracted value as a {@code long}.
		 */
		protected static byte value( long src, int bit, long mask ) {
			return ( byte ) ( src >>> bit & mask );
		}
		
		/**
		 * Extracts a value spanning two {@code long}s starting at a specified bit position.
		 * Used when the value crosses the boundary between two {@code long}s.
		 *
		 * @param prev Previous {@code long} value.
		 * @param next Next {@code long} value.
		 * @param bit  Starting bit position in {@code prev} (0 to 63).
		 * @param bits Number of bits to extract.
		 * @param mask Mask to isolate the desired bits.
		 * @return The extracted value as a {@code long}.
		 */
		protected static byte value( long prev, long next, int bit, int bits, long mask ) {
			return ( byte ) ( ( ( next & mask( bit + bits - BITS ) ) << BITS - bit | prev >>> bit ) & mask );
		}
		
		/**
		 * Calculates the number of {@code long}s needed to store a given number of bits.
		 * Uses ceiling division by BITS.
		 *
		 * @param bits Total number of bits to store.
		 * @return Number of {@code long}s required.
		 */
		static int len4bits( int bits ) {
			return bits + 63 >>> 6; // 63 ensures ceiling division
		}
		
		/**
		 * Number of bits in a {@code long}.
		 */
		protected static final int BITS = 64;
		
		/**
		 * Mask for bit operations, equal to 63 (0b111111).
		 */
		static final int MASK = BITS - 1;
		
		/**
		 * Number of bits to shift for indexing, equal to log2(BITS) = 6.
		 */
		static final int LEN = 6;
		
	}
	
	
	/**
	 * Read-write extension of {@code BitsList}, adding methods that modify the list and return the instance for chaining.
	 * Extends {@code R} with additional functionality for dynamic manipulation.
	 */
	class RW extends R {
		
		/**
		 * Constructs with specified bits per item; inherits from {@code R}.
		 */
		public RW( int bits_per_item ) {
			super( bits_per_item );
		}
		
		/**
		 * Constructs with specified bits per item and capacity; inherits from {@code R}.
		 */
		public RW( int bits_per_item, int length ) {
			super( bits_per_item, length );
		}
		
		/**
		 * Constructs with bits per item, default value, and size; inherits from {@code R}.
		 */
		public RW( int bits_per_item, int defaultValue, int size ) {
			super( bits_per_item, defaultValue, size );
		}
		
		/**
		 * Adds a value to the end of the list and returns this instance for chaining.
		 *
		 * @param value The value to add.
		 * @return This {@code RW} instance.
		 */
		public RW add1( long value ) {
			add( this, value );
			return this;
		}
		
		/**
		 * Adds a value at the specified index and returns this instance for chaining.
		 *
		 * @param index The index to insert at (0 to {@code size}).
		 * @param src   The value to insert.
		 * @return This {@code RW} instance.
		 */
		public RW add1( int index, long src ) {
			add( this, index, src );
			return this;
		}
		
		/**
		 * Removes all occurrences of the specified value and returns this instance for chaining.
		 *
		 * @param value The value to remove.
		 * @return This {@code RW} instance.
		 */
		public RW remove( long value ) {
			remove( this, value );
			return this;
		}
		
		/**
		 * Removes the item at the specified index and returns this instance for chaining.
		 *
		 * @param item The index to remove (0 to {@code size-1}).
		 * @return This {@code RW} instance.
		 */
		public RW removeAt( int item ) {
			removeAt( this, item );
			return this;
		}
		
		/**
		 * Removes the last item and returns this instance for chaining.
		 *
		 * @return This {@code RW} instance.
		 * @throws IndexOutOfBoundsException If the list is empty.
		 */
		public RW remove() {
			if( size == 0 ) throw new IndexOutOfBoundsException( "List is empty" );
			removeAt( this, size - 1 );
			return this;
		}
		
		/**
		 * Sets the value of the last item and returns this instance for chaining.
		 *
		 * @param value The value to set.
		 * @return This {@code RW} instance.
		 * @throws IndexOutOfBoundsException If the list is empty.
		 */
		public RW set1( long value ) {
			if( size == 0 ) throw new IndexOutOfBoundsException( "List is empty" );
			set1( this, size - 1, value );
			return this;
		}
		
		/**
		 * Sets the value at the specified index and returns this instance for chaining.
		 *
		 * @param item  The index to set (0 or greater).
		 * @param value The value to set.
		 * @return This {@code RW} instance.
		 */
		public RW set1( int item, long value ) {
			set1( this, item, value );
			return this;
		}
		
		/**
		 * Sets multiple values starting at the specified index and returns this instance for chaining.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The values to set.
		 * @return This {@code RW} instance.
		 */
		public RW set( int index, byte... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple values from a source array starting at the specified index and returns this instance.
		 *
		 * @param index     The starting index in the list (0 or greater).
		 * @param src       The source array containing the values.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to set.
		 * @return This {@code RW} instance.
		 */
		public RW set( int index, byte[] src, int src_index, int len ) {
			if( index < 0 ) throw new IndexOutOfBoundsException( "Index: " + index );
			if( src_index < 0 || src_index + len > src.length ) throw new IndexOutOfBoundsException( "Source bounds exceeded" );
			for( int i = len - 1; i >= 0; i-- ) set1( this, index + i, src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Overload for {@code char} values.
		 */
		public RW set( int index, char... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Overload for {@code short} values.
		 */
		public RW set( int index, short... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Overload for {@code int} values.
		 */
		public RW set( int index, int... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Overload for {@code long} values.
		 */
		public RW set( int index, long... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Retains only the items present in the specified list and indicates if the list changed.
		 * Removes all items not found in {@code chk}.
		 *
		 * @param chk The {@code R} instance containing values to retain.
		 * @return {@code true} if this list was modified, {@code false} otherwise.
		 */
		public boolean retainAll( R chk ) {
			if( chk == null ) return false;
			int initial_size = size;
			for( int i = 0; i < size; ) {
				int v = get( i );
				if( !chk.contains( v ) ) removeAt( this, i );
				else i++;
			}
			return initial_size != size;
		}
		
		/**
		 * Adjusts the capacity to match the current size and returns this instance for chaining.
		 * Trims excess capacity to optimize memory usage.
		 *
		 * @return This {@code RW} instance.
		 */
		public RW fit() {
			return length( size );
		}
		
		/**
		 * Sets the capacity of the list and returns this instance for chaining.
		 * If {@code items < 1}, clears the list and sets capacity to 0; otherwise, adjusts to at least {@code items}.
		 *
		 * @param items The desired capacity in items.
		 * @return This {@code RW} instance.
		 */
		public RW length( int items ) {
			if( items < 1 ) {
				values = Array.EqualHashOf._longs.O;
				size   = 0;
			}
			else length_( items );
			return this;
		}
		
		/**
		 * Clears the list, resetting size to 0.
		 * Overrides {@code R.clear()} to ensure proper clearing in the read-write context.
		 */
		@Override
		public void clear() {
			super.clear();
		}
		
		/**
		 * Sets the size of the list, extending with the default value if necessary, and returns this instance.
		 * If {@code size < 1}, clears the list; if greater than current size, extends with {@code default_value}.
		 *
		 * @param size The desired size in items.
		 * @return This {@code RW} instance.
		 */
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( this.size < size ) set1( size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		/**
		 * Creates a clone of this list.
		 *
		 * @return A new {@code RW} instance that is a deep copy of this list.
		 */
		@Override
		public RW clone() {
			return ( RW ) super.clone();
		}
	}
}