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
 * Defines a bit-packed list that efficiently stores primitives with value in range 0..0xEF using an array of {@code long}s.
 * Each item occupies a fixed number of bits, specified during instantiation, enabling compact storage.
 */
public interface BitsList {
	
	/**
	 * Abstract base class providing core functionality for a bit-packed list of primitives.
	 * Items are stored in an array of {@code long}s, with each item occupying a fixed number of bits.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * The underlying array storing bit-packed data.
		 */
		protected long[] values = Array.EqualHashOf._longs.O;
		
		/**
		 * The current number of items in the list.
		 */
		protected int size = 0;
		
		/**
		 * A mask used to isolate the bits of each item based on {@code bits_per_item}.
		 */
		protected final long mask;
		
		/**
		 * The number of bits allocated per item, ranging from 1 to 7 (inclusive).
		 */
		public final int bits_per_item;
		
		/**
		 * Returns the number of bits per item, calculated from the mask.
		 *
		 * @return The number of bits per item.
		 */
		protected int bits_per_item() { return 32 - Integer.numberOfLeadingZeros( ( int ) mask ); }
		
		/**
		 * Sentinel value representing uninitialized elements in the list.
		 * Since primitive ints cannot be null, this value is used to fill new slots when the list expands.
		 * Choose a value that does not conflict with valid data in your use case.
		 */
		public final int default_value;
		
		/**
		 * Constructs an empty {@code BitsList} with the specified number of bits per item and a default value of 0.
		 *
		 * @param bits_per_item The number of bits per item, must be between 1 and 7 (inclusive).
		 */
		protected R( int bits_per_item ) {
			if( bits_per_item < 1 || 7 < bits_per_item ) throw new IllegalArgumentException( "bits_per_item must be in the range 1 to 7" );
			
			mask          = mask( this.bits_per_item = bits_per_item );
			default_value = 0;
		}
		
		/**
		 * Constructs a {@code BitsList} with the specified bits per item and initial capacity, starting with zero size.
		 *
		 * @param bits_per_item The number of bits per item, must be between 1 and 7 (inclusive).
		 * @param length        The initial capacity in items.
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		protected R( int bits_per_item, int length ) {
			if( bits_per_item < 1 || 7 < bits_per_item ) throw new IllegalArgumentException( "bits_per_item must be in the range 1 to 7" );
			
			mask          = mask( this.bits_per_item = bits_per_item );
			values        = new long[ len4bits( length * this.bits_per_item ) ];
			default_value = 0;
		}
		
		/**
		 * Constructs a {@code BitsList} with specified bits per item, default value, and initial size.
		 * Populates the list with the default value if it is non-zero and size is positive.
		 *
		 * @param bits_per_item The number of bits per item, must be between 1 and 7 (inclusive).
		 * @param default_value The default value for items, masked to fit within {@code bits_per_item}.
		 * @param size          If positive, sets the initial number of items to this value and fills the list with the effective `default_value`.
		 *                      If negative, sets the initial number of items to `abs(size)` no filling occurs.
		 */
		protected R( int bits_per_item, int default_value, int size ) {
			if( bits_per_item < 1 || 7 < bits_per_item ) throw new IllegalArgumentException();
			mask               = mask( this.bits_per_item = bits_per_item );
			this.size          =
					size < 0 ?
							-size :
							size;
			values             = new long[ len4bits( this.size * this.bits_per_item ) ];
			this.default_value = ( int ) ( default_value & mask );
			
			if( this.default_value != 0 && 0 < size )
				for( int i = 0; i < this.size; i++ ) set_( this, i, this.default_value );
		}
		
		/**
		 * Returns the current number of items in the list.
		 *
		 * @return The size of the list.
		 */
		public int size() { return size; }
		
		/**
		 * Returns the current capacity of the list in terms of items.
		 *
		 * @return The maximum number of items that can be stored without resizing.
		 */
		public int length() { return ( values.length << LEN ) / bits_per_item; }
		
		/**
		 * Adjusts the storage capacity of the list.
		 * If {@code items > 0}, ensures capacity for at least {@code items}, trimming excess if needed.
		 * If {@code items <= 0}, clears the list and allocates capacity for {@code -items} items.
		 *
		 * @param items The desired capacity in items; negative values indicate clearing and pre-allocation.
		 */
		protected void length_( int items ) {
			if( 0 < items ) {
				if( items < size ) size = items;
				int new_length = len4bits( items * bits_per_item );
				if( values.length != new_length ) values = Arrays.copyOf( values, new_length );
			}
			else {
				int new_length = len4bits( -items * bits_per_item );
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
		 * Clears all items in the list by resetting their bits to zero and setting size to 0.
		 */
		protected void clear() {
			if( size() == 0 ) return;
			java.util.Arrays.fill( values, 0, len4bits( size * bits_per_item ), 0L );
			size = 0;
		}
		
		/**
		 * Checks if the list contains no items.
		 *
		 * @return {@code true} if the list is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size == 0; }
		
		/**
		 * Computes a hash code based on the list's bit-packed contents.
		 *
		 * @return A hash code representing the list's current state.
		 */
		@Override
		public int hashCode() {
			if( size == 0 ) return 149989999;
			int last_long = index( size * bits_per_item );
			int hash      = Array.hash( 149989999, values[ last_long ] & mask( bit( size * bits_per_item ) ) );
			for( int i = last_long - 1; i >= 0; i-- ) hash = Array.hash( hash, values[ i ] );
			return Array.finalizeHash( hash, size() );
		}
		
		/**
		 * Compares this list to another object for equality.
		 *
		 * @param other The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object other ) {
			return other != null && getClass() == other.getClass() && equals( ( R ) other );
		}
		
		/**
		 * Compares this list to another {@code R} instance for equality.
		 * Checks size and all bit-packed values for an exact match.
		 *
		 * @param other The {@code R} instance to compare with.
		 * @return {@code true} if the lists are identical, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null || other.size != size ) return false;
			
			for( int i = 0; i < size; i++ )
				if( get( i ) != other.get( i ) ) return false;
			
			return true;
		}
		
		/**
		 * Retrieves the value of the last item in the list.
		 *
		 * @return The value of the last item as a {@code byte}.
		 */
		public byte get() {
			if( size == 0 ) throw new IndexOutOfBoundsException( "List is empty" );
			return get( size - 1 );
		}
		
		/**
		 * Retrieves the value at the specified index, handling bit extraction across {@code long} boundaries.
		 *
		 * @param item The index of the item (0 to {@code size-1}).
		 * @return The value at the specified index as a {@code byte}.
		 */
		public byte get( int item ) {
			if( item < 0 || size <= item ) throw new IndexOutOfBoundsException( "Index: " + item + ", Size: " + size );
			int bit_pos = item * bits_per_item;
			int index   = index( bit_pos );
			int bit     = bit( bit_pos );
			return bit + bits_per_item > BITS ?
					value( values[ index ], values[ index + 1 ], bit, bits_per_item, mask ) :
					value( values[ index ], bit, mask );
		}
		
		/**
		 * Appends a value to the end of the list.
		 *
		 * @param dst The {@code BitsList} instance to modify.
		 * @param src The value to append, masked to fit within {@code bits_per_item}.
		 */
		protected static void add( R dst, long src ) {
			set1( dst, dst.size, src );
		}
		
		/**
		 * Inserts a value at the specified index, shifting subsequent elements right.
		 * Extends the list if the index equals the current size.
		 *
		 * @param dst   The {@code BitsList} instance to modify.
		 * @param item  The index to insert at (0 to {@code size}).
		 * @param value The value to insert, masked to fit within {@code bits_per_item}.
		 */
		protected static void add( R dst, int item, long value ) {
			if( item < 0 ) throw new IndexOutOfBoundsException( "Index: " + item );
			if( item >= dst.size ) {
				set1( dst, item, value );
				return;
			}
			
			dst.values = BitList.RW.shiftLeft( dst.values, item * dst.bits_per_item, dst.size * dst.bits_per_item, dst.bits_per_item, true );
			dst.size++;
			set1( dst, item, value );
		}
		
		/**
		 * Sets multiple values starting at the specified index from a byte array.
		 * Values are set in reverse order to ensure correct placement.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param from The starting index (0 or greater).
		 * @param src  The array of values to set.
		 */
		protected static void set( R dst, int from, byte... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[ i ] );
		}
		
		/**
		 * Sets multiple values starting at the specified index from a char array.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param from The starting index (0 or greater).
		 * @param src  The array of values to set.
		 */
		protected static void set( R dst, int from, char... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[ i ] );
		}
		
		/**
		 * Sets multiple values starting at the specified index from a short array.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param from The starting index (0 or greater).
		 * @param src  The array of values to set.
		 */
		protected static void set( R dst, int from, short... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[ i ] );
		}
		
		/**
		 * Sets multiple values starting at the specified index from an int array.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param from The starting index (0 or greater).
		 * @param src  The array of values to set.
		 */
		protected static void set( R dst, int from, int... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[ i ] );
		}
		
		/**
		 * Sets multiple values starting at the specified index from a long array.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param from The starting index (0 or greater).
		 * @param src  The array of values to set.
		 */
		protected static void set( R dst, int from, long... src ) {
			if( from < 0 ) throw new IndexOutOfBoundsException( "Index: " + from );
			for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[ i ] );
		}
		
		/**
		 * Sets a value at the specified index, extending the list if necessary with the default value.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param item The index to set (0 or greater).
		 * @param src  The value to set, masked to fit within {@code bits_per_item}.
		 */
		protected static void set1( R dst, int item, long src ) {
			if( item < 0 ) throw new IndexOutOfBoundsException( "Index: " + item );
			long v       = src & dst.mask;
			int  bit_pos = item * dst.bits_per_item;
			
			if( item < dst.size ) {
				int index = index( bit_pos );
				int bit   = bit( bit_pos );
				int k     = BITS - bit;
				
				if( k < dst.bits_per_item ) { // Value spans two longs
					long mask = ~0L << bit;
					dst.values[ index ] = dst.values[ index ] & ~mask | v << bit;
					int bits_in_next = dst.bits_per_item - k;
					dst.values[ index + 1 ] = dst.values[ index + 1 ] & ~mask( bits_in_next ) | v >>> k;
				}
				else {
					// Value fits within one long
					long mask = dst.mask << bit;
					dst.values[ index ] = dst.values[ index ] & ~mask | v << bit;
				}
				return;
			}
			
			if( dst.length() <= item ) dst.length_( Math.max( dst.length() * 3 / 2, item + 1 ) );
			
			if( dst.default_value != 0 )
				for( int i = dst.size; i < item; i++ )
				     set_( dst, i, dst.default_value );
			
			set_( dst, item, v );
			dst.size = item + 1;
		}
		
		/**
		 * Sets a value at the specified bit position, handling spans across two {@code long}s.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param item The index to set.
		 * @param src  The value to set, masked to fit within {@code bits_per_item}.
		 */
		private static void set_( R dst, int item, long src ) {
			long v       = src & dst.mask;
			int  bit_pos = item * dst.bits_per_item;
			int  index   = index( bit_pos );
			int  bit     = bit( bit_pos );
			int  k       = BITS - bit;
			if( k < dst.bits_per_item ) {
				// Value spans two longs
				long mask = ~0L << bit;
				dst.values[ index ] = dst.values[ index ] & ~mask | v << bit;
				int bits_in_next = dst.bits_per_item - k;
				dst.values[ index + 1 ] = dst.values[ index + 1 ] & ~mask( bits_in_next ) | v >>> k;
			}
			else {
				// Value fits within one long
				long mask = dst.mask << bit;
				dst.values[ index ] = dst.values[ index ] & ~mask | v << bit;
			}
		}
		
		/**
		 * Removes the item at the specified index, shifting subsequent elements left.
		 *
		 * @param dst  The {@code BitsList} instance to modify.
		 * @param item The index to remove (0 to {@code size-1}).
		 */
		protected static void removeAt( R dst, int item ) {
			if( dst.size == 0 || item < 0 || dst.size <= item ) return;
			if( item == dst.size - 1 ) {
				if( dst.default_value == 0 ) set_( dst, item, 0 ); // Ensure trailing bits are zeroed
				dst.size--;
				return;
			}
			BitList.RW.shiftRight( dst.values, dst.values, item * dst.bits_per_item, dst.size * dst.bits_per_item, dst.bits_per_item, true );
			dst.size--;
		}
		
		/**
		 * Creates a deep copy of this list.
		 *
		 * @return A new {@code R} instance identical to this list.
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
		 * Returns a JSON-formatted string representation of the list.
		 *
		 * @return The list as a JSON string.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Serializes the list contents into a JSON array.
		 *
		 * @param json The {@code JsonWriter} to write the list to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray();
			if( size > 0 ) {
				json.preallocate( size * 4 );
				long src = values[ 0 ];
				for( int bp = 0, max = size * bits_per_item, i = 0; bp < max; bp += bits_per_item, i++ ) {
					int bit = bit( bp );
					long value = bit + bits_per_item > BITS ?
							value( src, values[ index( bp ) + 1 ], bit, bits_per_item, mask ) :
							value( src, bit, mask );
					json.value( value );
					if( bit + bits_per_item > BITS ) src = values[ index( bp ) + 1 ];
				}
			}
			json.exitArray();
		}
		
		/**
		 * Returns the index of the first occurrence of the specified value.
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
		 * Returns the index of the last occurrence of the specified value.
		 *
		 * @param value The value to search for.
		 * @return The index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( long value ) {
			return lastIndexOf( size, value );
		}
		
		/**
		 * Returns the index of the last occurrence of the specified value up to a given index.
		 *
		 * @param from  The index to start searching backward from (clamped to {@code size}).
		 * @param value The value to search for.
		 * @return The index of the last occurrence, or -1 if not found.
		 */
		public int lastIndexOf( int from, long value ) {
			for( int i = Math.min( from, size - 1 ); -1 < i; i-- ) if( value == get( i ) ) return i;
			return -1;
		}
		
		/**
		 * Removes all occurrences of the specified value from the list.
		 *
		 * @param dst   The {@code BitsList} instance to modify.
		 * @param value The value to remove.
		 */
		protected static void remove( R dst, long value ) {
			for( int i = dst.size; -1 < ( i = dst.lastIndexOf( i, value ) ); ) removeAt( dst, i );
		}
		
		/**
		 * Checks if the list contains the specified value.
		 *
		 * @param value The value to check for.
		 * @return {@code true} if the value exists in the list, {@code false} otherwise.
		 */
		public boolean contains( long value ) { return indexOf( value ) != -1; }
		
		/**
		 * Converts the list to a byte array, creating a new array if necessary.
		 *
		 * @param dst The destination array; if null or too small, a new array is allocated.
		 * @return The byte array of list values, or {@code null} if the list is empty.
		 */
		public byte[] toArray( byte[] dst ) {
			if( size == 0 ) return dst;
			if( dst == null || dst.length < size ) dst = new byte[ size ];
			for( int i = 0; i < size; i++ ) dst[ i ] = get( i );
			return dst;
		}
		
		public byte[] toArray() { return toArray( new byte[ size ] ); }
		
		
		/**
		 * Creates a mask with the specified number of least significant bits set to 1.
		 *
		 * @param bits The number of bits for the mask (0 to 7).
		 * @return A {@code long} with the specified bits set (e.g., {@code mask(3)} returns 0b111).
		 */
		static long mask( int bits ) {
			return ( 1L << bits ) - 1;
		}
		
		/**
		 * Calculates the array index for a given bit position.
		 *
		 * @param bit_position The bit position in the bit-packed array.
		 * @return The index of the {@code long} containing the bit.
		 */
		protected static int index( int bit_position ) {
			return bit_position >> LEN;
		}
		
		/**
		 * Calculates the bit offset within a {@code long} for a given bit position.
		 *
		 * @param bit_position The bit position in the bit-packed array.
		 * @return The bit offset within the {@code long} (0 to 63).
		 */
		protected static int bit( int bit_position ) {
			return bit_position & MASK;
		}
		
		/**
		 * Extracts a value from a {@code long} at a specified bit position.
		 *
		 * @param src  The source {@code long} containing the value.
		 * @param bit  The starting bit position (0 to 63).
		 * @param mask The mask to isolate the value.
		 * @return The extracted value as a {@code byte}.
		 */
		protected static byte value( long src, int bit, long mask ) {
			return ( byte ) ( src >>> bit & mask );
		}
		
		/**
		 * Extracts a value spanning two {@code long}s at a specified bit position.
		 *
		 * @param prev The previous {@code long} containing the lower bits.
		 * @param next The next {@code long} containing the upper bits.
		 * @param bit  The starting bit position in {@code prev} (0 to 63).
		 * @param bits The number of bits to extract.
		 * @param mask The mask to isolate the value.
		 * @return The extracted value as a {@code byte}.
		 */
		protected static byte value( long prev, long next, int bit, int bits, long mask ) {
			return ( byte ) ( ( ( next & mask( bit + bits - BITS ) ) << BITS - bit | prev >>> bit ) & mask );
		}
		
		/**
		 * Calculates the number of {@code long}s required to store a given number of bits.
		 *
		 * @param bits The total number of bits to store.
		 * @return The number of {@code long}s needed (ceiling division by 64).
		 */
		static int len4bits( int bits ) {
			return bits + MASK >>> LEN;
		}
		
		/**
		 * The number of bits in a {@code long} (64).
		 */
		protected static final int BITS = 64;
		
		/**
		 * A mask for bit operations (63, or 0b111111).
		 */
		protected static final int MASK = BITS - 1;
		
		/**
		 * The number of bits to shift for indexing (log2(64) = 6).
		 */
		protected static final int LEN = 6;
	}
	
	/**
	 * A read-write extension of {@code BitsList.R}, adding modification methods with chaining support.
	 */
	class RW extends R {
		
		/**
		 * Constructs an empty list with the specified bits per item.
		 *
		 * @param bits_per_item The number of bits per item (1 to 7).
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		public RW( int bits_per_item ) {
			super( bits_per_item );
		}
		
		/**
		 * Constructs a list with the specified bits per item and initial capacity.
		 *
		 * @param bits_per_item The number of bits per item (1 to 7).
		 * @param length        The initial capacity in items.
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		public RW( int bits_per_item, int length ) {
			super( bits_per_item, length );
		}
		
		/**
		 * Constructs a list with specified bits per item, default value, and initial size.
		 *
		 * @param bits_per_item The number of bits per item (1 to 7).
		 * @param defaultValue  The default value for items, masked to fit within {@code bits_per_item}.
		 * @param size          If positive, sets the initial number of items to this value and fills the list with the effective `default_value`.
		 *                      If negative, sets the initial number of items to `abs(size)` no filling occurs.
		 * @throws IllegalArgumentException if {@code bits_per_item} is not between 1 and 7.
		 */
		public RW( int bits_per_item, int defaultValue, int size ) {
			super( bits_per_item, defaultValue, size );
		}
		
		/**
		 * Appends a value to the end of the list and returns this instance for chaining.
		 *
		 * @param value The value to append, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance.
		 */
		public RW add1( long value ) {
			add( this, value );
			return this;
		}
		
		/**
		 * Inserts a value at the specified index and returns this instance for chaining.
		 *
		 * @param index The index to insert at (0 to {@code size}).
		 * @param src   The value to insert, masked to fit within {@code bits_per_item}.
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
		 */
		public RW remove() {
			if( size == 0 ) throw new IndexOutOfBoundsException( "List is empty" );
			removeAt( this, size - 1 );
			return this;
		}
		
		/**
		 * Sets the value of the last item and returns this instance for chaining.
		 *
		 * @param value The value to set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance.
		 */
		public RW set1( long value ) {
			set1( this, size - 1, value );
			return this;
		}
		
		/**
		 * Sets the value at the specified index and returns this instance for chaining.
		 *
		 * @param item  The index to set (0 or greater).
		 * @param value The value to set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance.
		 */
		public RW set1( int item, long value ) {
			set1( this, item, value );
			return this;
		}
		
		/**
		 * Sets multiple values starting at the specified index from a byte array and returns this instance.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The values to set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance.
		 */
		public RW set( int index, byte... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple values from a byte array starting at the specified index and returns this instance.
		 *
		 * @param index     The starting index in the list (0 or greater).
		 * @param src       The source array of values, masked to fit within {@code bits_per_item}.
		 * @param src_index The starting index in the source array.
		 * @param len       The number of elements to set.
		 * @return This {@code RW} instance.
		 */
		public RW set( int index, byte[] src, int src_index, int len ) {
			if( index < 0 ) throw new IndexOutOfBoundsException( "Index: " + index );
			if( src_index < 0 || src_index + len > src.length )
				throw new IndexOutOfBoundsException( "Source bounds exceeded" );
			for( int i = len - 1; i >= 0; i-- ) set1( this, index + i, src[ src_index + i ] );
			return this;
		}
		
		/**
		 * Sets multiple values starting at the specified index from a char array and returns this instance.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The values to set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance.
		 */
		public RW set( int index, char... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple values starting at the specified index from a short array and returns this instance.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The values to set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance.
		 */
		public RW set( int index, short... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple values starting at the specified index from an int array and returns this instance.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The values to set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance.
		 */
		public RW set( int index, int... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Sets multiple values starting at the specified index from a long array and returns this instance.
		 *
		 * @param index  The starting index (0 or greater).
		 * @param values The values to set, masked to fit within {@code bits_per_item}.
		 * @return This {@code RW} instance.
		 */
		public RW set( int index, long... values ) {
			set( this, index, values );
			return this;
		}
		
		/**
		 * Retains only the items present in the specified list, removing others, and indicates if modified.
		 *
		 * @param chk The {@code R} instance containing values to retain.
		 * @return {@code true} if the list was modified, {@code false} otherwise.
		 */
		public boolean retainAll( R chk ) {
			if( chk == null ) return false;
			boolean ret = false;
			for( int i = size; -1 < --i; ) {
				long v = get( i );
				if( chk.contains( v ) ) continue;
				removeAll( v );
				ret = true;
				if( size < i ) i = size;
			}
			return ret;
		}
		
		public int removeAll( long src ) {
			int fix = size;
			for( int k; -1 < ( k = indexOf( src ) ); ) removeAt( k );
			return fix - size;
		}
		
		/**
		 * Trims the capacity to match the current size and returns this instance for chaining.
		 *
		 * @return This {@code RW} instance.
		 */
		public RW fit() {
			return length( size );
		}
		
		/**
		 * Sets the capacity of the list and returns this instance for chaining.
		 * If {@code items < 1}, clears the list; otherwise, adjusts capacity to at least {@code items}.
		 *
		 * @param length The desired capacity in items.
		 * @return This {@code RW} instance.
		 */
		public RW length( int length ) {
			if( length < 0 ) throw new IllegalArgumentException( "length cannot be negative" );
			
			if( length ==0 ) {
				values = Array.EqualHashOf._longs.O;
				size   = 0;
			}
			else length_( length );
			return this;
		}
		
		/**
		 * Sets the size of the list, extending with the default value if needed, and returns this instance.
		 *
		 * @param size The desired size; if less than 1, clears the list; if greater than current size, extends.
		 * @return This {@code RW} instance.
		 */
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( this.size < size ) set1( size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		/**
		 * Creates a deep copy of this list.
		 *
		 * @return A new {@code RW} instance identical to this list.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		/**
		 * Clears all items in the list by resetting their bits to zero and setting size to 0.
		 */
		public void clear() { super.clear(); }
	}
}