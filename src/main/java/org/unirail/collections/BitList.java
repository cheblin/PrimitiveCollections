// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// 1. The above copyright notice and this permission notice must be included in all
//    copies or substantial portions of the Software.
//
// 2. Users of the Software must provide a clear acknowledgment in their user
//    documentation or other materials that their solution includes or is based on
//    this Software. This acknowledgment should be prominent and easily visible,
//    and can be formatted as follows:
//    "This product includes software developed by Chikirev Sirguy and the Unirail Group
//    (https://github.com/AdHoc-Protocol)."
//
// 3. If you modify the Software and distribute it, you must include a prominent notice
//    stating that you have changed the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.Arrays;

/**
 * {@code BitList} is a powerful abstraction that behaves like a massive Java primitive for bit manipulation.
 * It provides an efficient way to manage and query a dynamic list of bits, optimized for both space and performance.
 * Designed to handle large-scale bit operations, it supports a variety of functionalities while maintaining a compact memory footprint.
 */
public interface BitList {
	/**
	 * Abstract base class {@code R} provides a read-only foundation for the {@code BitList} interface.
	 * MSB                LSB
	 * |                  |
	 * bits  in the list   [0, 0, 0, 1, 1, 1, 1]      Leading 3 zeros and trailing 4 ones
	 * index in the list    6  5  4  3  2  1  0
	 * shift left                 <<
	 * shift right                >>>
	 * <p>
	 * It encapsulates core functionalities for bit storage, size management, and querying, serving as a blueprint
	 * for concrete {@code BitList} implementations. Optimized for space efficiency, it uses techniques like
	 * implicit trailing ones and trailing zeros to minimize memory usage while ensuring fast read operations.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		/**
		 * The total number of bits in the BitList, encompassing:
		 * - trailing '1's represented by trailingOnesCount (implicitly stored),
		 * - Explicit bits stored in the values array (following trailingOnesCount),
		 * - Trailing '0's up to the size (implicitly assumed, not stored in values).
		 */
		protected int size;
		/**
		 * Number of trailing '1' bits at the beginning of the list that are not explicitly stored in {@code values}.
		 * This optimization helps in space efficiency when dealing with lists that start with a sequence of '1's.
		 */
		protected int trailingOnesCount = 0;
		
		/**
		 * Stores the interleaving bits of the BitList *after* any trailing ones.
		 * <p>
		 * The first bit in `values` is always '0' (representing the first '0' in the BitList).
		 * The last bit in `values` is always '1' (representing the final '1' in the BitList before trailing zeros).
		 * Trailing zeros, up to the BitList's logical size, are not explicitly stored.
		 * <p>
		 * Bits are packed into `long` values, with lower array indices representing earlier bits in the BitList.
		 * Within each `long`, bits are stored least-significant-bit first.
		 */
		protected long[] values = Array.EqualHashOf._longs.O;
		/**
		 * Number of {@code long} elements in the {@code values} array that are currently in use to store bits.
		 * This tracks the actual storage being used and can be less than the total length of the {@code values} array.
		 */
		protected int    used   = 0;
		
		/**
		 * Returns the total number of bits in this {@code BitList}.
		 *
		 * @return The size of the {@code BitList}.
		 */
		public int size() { return size; }
		
		/**
		 * Calculates the number of {@code long} elements required to store a given number of bits.
		 *
		 * @param bits The number of bits to store.
		 * @return The number of {@code long} elements needed.
		 */
		static int len4bits( int bits ) { return bits + BITS - 1 >> LEN; }
		
		/**
		 * Bit length for indexing within {@code long} array.
		 */
		protected static final int LEN  = 6;
		/**
		 * Number of bits in a {@code long} (64).
		 */
		protected static final int BITS = 1 << LEN; // 64
		/**
		 * Mask for bitwise operations within a {@code long} (63, i.e., {@code BITS - 1}).
		 */
		protected static final int MASK = BITS - 1; // 63
		
		/**
		 * Calculates the index of the {@code long} element in the {@code values} array that contains the specified bit.
		 *
		 * @param bit The bit position (0-indexed).
		 * @return The index of the {@code long} element in the {@code values} array.
		 */
		static int index( int bit ) { return bit >> LEN; }
		
		/**
		 * Creates a mask with the specified number of least significant bits set to '1'.
		 *
		 * @param bits The number of bits to set to '1' in the mask.
		 * @return A {@code long} mask.
		 */
		static long mask( int bits ) { return -1L >>> 64 - bits; }
		
		/**
		 * Integer maximum value constant.
		 */
		static final int OI = Integer.MAX_VALUE;
		/**
		 * Integer minimum value constant.
		 */
		static final int IO = Integer.MIN_VALUE;
		
		/**
		 * Calculates and returns the number of {@code long} elements in the {@code values} array that are actively used.
		 * "Used" is defined as the count of {@code long} elements from index 0 up to the highest index containing a non-zero value.
		 * If {@code used} is negative, it recalculates by scanning for trailing zeros; otherwise, it returns the cached value.
		 * This method efficiently determines the actual storage used, excluding trailing zeroed {@code long} elements.
		 *
		 * @return The number of {@code long} elements in {@code values} with at least one non-zero bit.
		 */
		protected int used() {
			// Check if `used` is positive, indicating a cached and valid value. Return directly if valid.
			if( -1 < used ) return used;
			
			// `used` is negative, recalculation is needed. Clear the sign bit to get the last known count.
			used &= OI;
			
			// Start scanning backwards from the last known used index to find the highest non-zero element.
			int u = used - 1;
			
			// Iterate backwards, skipping zeroed longs to find the actual used length.
			while( -1 < u && values[ u ] == 0 ) u--;
			
			// Update `used` with the new count (index + 1) and return it.
			return used = u + 1;
		}
		
		/**
		 * Determines the index in the {@code values} array for a given bit position, expanding the array if necessary.
		 * It also updates the {@code size} of the {@code BitList} if the given bit position is beyond the current size.
		 *
		 * @param bit The bit position (0-indexed).
		 * @return The index in the {@code values} array where the bit is located, or -1 if the bit is within the trailing '1's range.
		 * Expands the {@code values} array if the index is out of bounds.
		 */
		int used( int bit ) {
			
			if( size() <= bit ) size = bit + 1;
			int index = bit - trailingOnesCount >> LEN;
			if( index < 0 ) return -1; // Within trailing '1's
			if( index < used() ) return index;
			if( values.length < ( used = index + 1 ) )
				values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), used ) );
			return index;
		}
		
		/**
		 * Retrieves the value of the bit at the specified position.
		 *
		 * @param bit The bit position to get (0-indexed).
		 * @return {@code true} if the bit at the specified position is '1', {@code false} otherwise.
		 * Returns {@code false} if the index is out of bounds.
		 */
		public boolean get( int bit ) {
			if( bit < 0 || bit >= size ) return false;
			if( bit < trailingOnesCount ) return true;
			int index = bit - trailingOnesCount >> LEN;
			return index < used() && ( values[ index ] & 1L << ( bit - trailingOnesCount & MASK ) ) != 0;
		}
		
		/**
		 * Retrieves the value of the bit at the specified position and returns one of two provided integers based on the bit's value.
		 *
		 * @param bit   The bit position to check (0-indexed).
		 * @param FALSE The integer value to return if the bit is '0' (false).
		 * @param TRUE  The integer value to return if the bit is '1' (true).
		 * @return {@code TRUE} if the bit at the specified position is '1', {@code FALSE} otherwise.
		 */
		public int get( int bit, int FALSE, int TRUE ) {
			return get( bit ) ?
					TRUE :
					FALSE;
		}
		
		/**
		 * Copies a range of bits from this {@code BitList} to a destination {@code long} array.
		 *
		 * @param dst      The destination {@code long} array.
		 * @param from_bit The starting bit position to copy from (inclusive).
		 * @param to_bit   The ending bit position to copy to (exclusive).
		 * @return The number of bits actually copied to the destination array.
		 */
		public int get( long[] dst, int from_bit, int to_bit ) {
			if( to_bit <= from_bit || from_bit < 0 || size <= from_bit ) return 0;
			to_bit = Math.min( to_bit, size );
			int bits_to_copy = to_bit - from_bit;
			
			int dst_index      = 0;
			int dst_bit_offset = 0;
			int copied_bits    = 0;
			
			int trailing_ones_to_copy = Math.min( trailingOnesCount - from_bit, bits_to_copy );
			if( trailing_ones_to_copy > 0 ) {
				for( int i = 0; i < trailing_ones_to_copy; i++ ) {
					if( dst_bit_offset == BITS ) {
						dst_bit_offset = 0;
						dst_index++;
						if( dst_index >= dst.length ) return copied_bits;
					}
					dst[ dst_index ] |= 1L << dst_bit_offset;
					dst_bit_offset++;
				}
				from_bit += trailing_ones_to_copy;
				bits_to_copy -= trailing_ones_to_copy;
				copied_bits += trailing_ones_to_copy;
				if( bits_to_copy == 0 ) return copied_bits;
			}
			
			int start_bit_in_values        = from_bit - trailingOnesCount;
			int start_index_in_values      = start_bit_in_values >> LEN;
			int start_bit_offset_in_values = start_bit_in_values & MASK;
			
			for( int i = start_index_in_values; i < used() && bits_to_copy > 0; i++ ) {
				long current_value          = values[ i ] >>> start_bit_offset_in_values;
				int  bits_from_current_long = Math.min( BITS - start_bit_offset_in_values, bits_to_copy );
				
				for( int j = 0; j < bits_from_current_long; j++ ) {
					if( dst_bit_offset == BITS ) {
						dst_bit_offset = 0;
						dst_index++;
						if( dst_index >= dst.length ) return copied_bits;
					}
					if( ( current_value & 1L << j ) != 0 ) dst[ dst_index ] |= 1L << dst_bit_offset;
					dst_bit_offset++;
				}
				copied_bits += bits_from_current_long;
				bits_to_copy -= bits_from_current_long;
				start_bit_offset_in_values = 0;
			}
			return copied_bits;
		}
		
		/**
		 * Finds and returns the index of the next '1' bit starting from the specified position.
		 *
		 * @param bit The starting bit position to search from (inclusive).
		 * @return The index of the next '1' bit, or -1 of the {@code BitList} if no '1' bit is found from the specified position to the end.
		 */
		public int next1( int bit ) {
			// Adjust negative start to 0; if beyond size, return size
			if( bit < 0 ) bit = 0;
			int last1 = last1();
			if( last1 < bit ) return -1;// If beyond  last1() values, all remaining are '0', so return size
			if( bit == last1 ) return last1;
			
			if( bit < trailingOnesCount ) return bit;// If within trailing ones, return the starting bit (it’s a '1')
			
			// Adjust position relative to end of trailing ones
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			int pos       = bitOffset & MASK;   // Bit position within the long
			
			// Mask to consider only bits from pos onward in the first long
			long mask  = -1L << pos; // 1s from pos to end
			long value = values[ index ] & mask; // Check for '1's from pos
			
			// Check the first long
			if( value != 0 ) return trailingOnesCount + ( index << LEN ) + Long.numberOfTrailingZeros( value );
			
			// Search subsequent longs
			for( int i = index + 1; i < used(); i++ ) {
				value = values[ i ];
				if( value != 0 ) return trailingOnesCount + ( i << LEN ) + Long.numberOfTrailingZeros( value );
			}
			
			// No '1' found, return size
			return -1;
		}
		
		
		/**
		 * Finds and returns the index of the next '0' bit starting from the specified position.
		 *
		 * @param bit The starting bit position to search from (inclusive).
		 * @return The index of the next '0' bit, or -1 if no '0' bit is found from the specified position to the end.
		 */
		public int next0( int bit ) {
			// If starting position is invalid (negative) or beyond size, return size
			if( bit < 0 ) bit = 0; // Adjust negative start to beginning
			if( size() <= bit ) return -1; // No '0' beyond size
			if( last1() < bit ) return bit;
			
			// Check within trailing ones region (all '1's)
			if( bit < trailingOnesCount )
				return trailingOnesCount; // First '0' is after trailing ones
			
			
			// Adjust bit position relative to the end of trailing ones
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Which long in values array
			int pos       = bitOffset & MASK;   // Bit position within that long
			
			// If starting beyond used values, all remaining bits are '0', so return bit
			if( index >= used() ) return bit;
			
			// Mask to consider only bits from pos onward in the first long
			long mask  = -1L << pos; // 1s from pos to end of 64 bits
			long value = ~values[ index ] & mask; // Invert to find '0's, apply mask
			
			// Search within the first long
			if( value != 0 )
				return trailingOnesCount + ( index << LEN ) + Long.numberOfTrailingZeros( value );
			
			
			// Search subsequent longs
			for( int i = index + 1; i < used(); i++ ) {
				value = ~values[ i ]; // Invert to find '0's
				if( value != 0 ) return trailingOnesCount + ( i << LEN ) + Long.numberOfTrailingZeros( value );
			}
			
			// No '0' found, return size
			return -1;
		}
		
		/**
		 * Finds and returns the index of the previous '1' bit ending at the specified position.
		 *
		 * @param bit The ending bit position to search towards the beginning (inclusive).
		 * @return The index of the previous '1' bit, or -1 if no '1' bit is found before the specified position.
		 */
		public int prev1( int bit ) {
			// Handle invalid or out-of-bounds cases
			if( bit < 0 ) return -1; // Nothing before 0
			if( bit >= size() ) bit = size() - 1; // Adjust to last valid bit
			
			// If within trailing ones, return the bit itself if valid, else adjust
			if( bit < trailingOnesCount ) return bit; // All bits up to trailingOnesCount-1 are '1'
			
			// Adjust position relative to end of trailing ones
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			int pos       = bitOffset & MASK;   // Bit position within the long
			
			// If beyond used values, search up to last used bit
			if( used() <= index ) return last1(); // Return last '1' in the list
			
			// Mask to consider only bits up to pos (inclusive) in the first long
			long mask  = ( 1L << pos + 1 ) - 1; // 1s from 0 to pos
			long value = values[ index ] & mask; // Check for '1's up to pos
			
			// Check the current long
			if( value != 0 ) return trailingOnesCount + ( index << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
			
			// Search previous longs
			for( int i = index - 1; i >= 0; i-- ) {
				value = values[ i ];
				if( value != 0 ) return trailingOnesCount + ( i << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
			}
			
			// If no '1' in values, check trailing ones
			if( trailingOnesCount > 0 ) return trailingOnesCount - 1; // Last '1' in trailing ones
			
			// No '1' found anywhere
			return -1;
		}
		
		/**
		 * Finds and returns the index of the previous '0' bit ending at the specified position.
		 *
		 * @param bit The ending bit position to search towards the beginning (inclusive).
		 * @return The index of the previous '0' bit, or -1 if no '0' bit is found before the specified position.
		 */
		public int prev0( int bit ) {
			// Handle invalid or out-of-bounds cases
			if( bit < 0 ) return -1; // Nothing before 0
			if( bit >= size() ) bit = size() - 1; // Adjust to last valid bit
			
			// If within trailing ones (all '1's), no '0' exists before
			if( bit < trailingOnesCount ) return -1; // All bits up to trailingOnesCount-1 are '1'
			
			// Adjust position relative to end of trailing ones
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			int pos       = bitOffset & MASK;   // Bit position within the long
			
			// If beyond used values, all trailing bits are '0', check up to used
			if( index >= used() ) {
				if( bit == trailingOnesCount ) return trailingOnesCount > 0 ?
						trailingOnesCount - 1 :
						-1;
				return bit; // Bits beyond used are '0'
			}
			
			// Mask to consider only bits up to pos (inclusive) in the current long
			long mask  = ( 1L << pos + 1 ) - 1; // 1s from 0 to pos
			long value = ~values[ index ] & mask; // Invert to find '0's up to pos
			
			// Check the current long
			if( value != 0 ) return trailingOnesCount + ( index << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
			
			// Search previous longs
			for( int i = index - 1; i >= 0; i-- ) {
				value = ~values[ i ]; // Invert to find '0's
				if( value != 0 ) return trailingOnesCount + ( i << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
			}
			
			// No '0' in values, return -1 (all bits before are trailing ones)
			return -1;
		}
		
		/**
		 * Retrieves the 0-based index of the leftmost '1' bit in this BitList.
		 * If the list contains no '1' bits (all zeros), returns -1. Trailing bits after this index up to size are implicit '0's.
		 *
		 * @return Index of the highest '1' bit, or -1 if no '1' bits exist.
		 */
		public int last1() {
			return used() == 0 ?
					trailingOnesCount - 1 :
					trailingOnesCount + ( used - 1 << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( values[ used - 1 ] );
		}
		
		/**
		 * Checks if all bits in this {@code BitList} are '0'.
		 *
		 * @return {@code true} if all bits are '0', {@code false} otherwise.
		 */
		public boolean isAllZeros() { return trailingOnesCount == 0 && used == 0; }
		
		/**
		 * Calculates the number of set bits ('1's) in the {@code BitList} up to the specified bit position (inclusive).
		 * This operation is also known as "rank" in bit vector terminology.
		 *
		 * @param bit The bit position up to which the count of '1's is calculated (inclusive).
		 * @return The number of '1' bits from the beginning of the {@code BitList} up to the specified bit position.
		 */
		public int rank( int bit ) {
			// Handle invalid bit position or empty list.
			if( bit < 0 || size == 0 ) return 0;
			
			// If the bit is within the trailing ones region, the rank is simply the bit position + 1.
			if( bit < trailingOnesCount ) return bit + 1;
			if( used() == 0 ) return trailingOnesCount;
			
			int last1 = last1();
			if( last1 < bit ) bit = last1;
			
			// Calculate rank for bits beyond trailing ones.
			int index = bit - trailingOnesCount >> LEN; // Index of the long containing the bit.
			// Count '1's in the current long up to the specified bit, and add trailing ones count.
			int sum = trailingOnesCount + Long.bitCount( values[ index ] << BITS - 1 - ( bit - trailingOnesCount ) );
			// Add '1' counts from all preceding longs in the values array.
			for( int i = 0; i < index; i++ ) sum += Long.bitCount( values[ i ] );
			
			return sum; // Total count of '1's up to the specified bit.
		}
		
		/**
		 * Returns the total number of set bits ('1's) in this {@code BitList}.
		 *
		 * @return The cardinality of the {@code BitList}.
		 */
		public int cardinality() { return rank( size ); }
		
		/**
		 * Finds the bit position with the given cardinality (rank).
		 * The cardinality is the number of '1's up to and including the returned bit position.
		 *
		 * @param cardinality The rank (number of '1's up to the desired bit) to search for.
		 * @return The bit position with the given cardinality, or -1 if not found (i.e., cardinality is invalid or exceeds total '1's).
		 */
		public int bit( int cardinality ) {
			// Handle invalid cardinality
			if( cardinality <= 0 ) return -1; // No position has zero or negative '1's
			if( cardinality > rank( size() - 1 ) ) return -1; // Exceeds total '1's in list
			
			// If within trailing ones, return cardinality - 1 (since all are '1's)
			if( cardinality <= trailingOnesCount ) return cardinality - 1; // 0-based index of the cardinality-th '1'
			
			// Adjust cardinality for bits beyond trailing ones
			int remainingCardinality = cardinality - trailingOnesCount;
			int totalBits            = size() - trailingOnesCount; // Bits stored in values
			
			// Scan through values array
			for( int i = 0; i < used() && remainingCardinality > 0; i++ ) {
				long value      = values[ i ];
				int  bitsInLong = Math.min( BITS, totalBits - ( i << LEN ) ); // Bits in this long
				int  count      = Long.bitCount( value & mask( bitsInLong ) ); // '1's in this long
				
				// Find the exact bit in this long
				if( remainingCardinality <= count )
					for( int j = 0; j < bitsInLong; j++ )
						if( ( value & 1L << j ) != 0 )
							if( --remainingCardinality == 0 )
								return trailingOnesCount + ( i << LEN ) + j;
				remainingCardinality -= count;
			}
			
			// Should not reach here if cardinality is valid, but return -1 for safety
			return -1;
		}
		
		/**
		 * Generates a hash code for this {@code BitList}.
		 * The hash code is based on the size, trailing ones count, and the content of the {@code values} array.
		 *
		 * @return The hash code value for this {@code BitList}.
		 */
		@Override
		public int hashCode() {
			int hash = 197;
			for( int i = used(); -1 < --i; ) hash = Array.hash( hash, values[ i ] );
			hash = Array.hash( hash, trailingOnesCount );
			return Array.finalizeHash( hash, size() );
		}
		
		/**
		 * Returns the total potential bit capacity of the underlying storage, including trailing ones and allocated {@code values}.
		 * This value represents the maximum bit index that could be addressed without resizing the {@code values} array,
		 * plus the bits represented by {@code trailingOnesCount}.
		 *
		 * @return The length of the {@code BitList} in bits, considering allocated storage.
		 */
		public int length() { return trailingOnesCount + ( values.length << LEN ); }
		
		/**
		 * Creates and returns a deep copy of this {@code R} instance.
		 * The cloned object will have the same size, trailing ones count, and bit values as the original.
		 *
		 * @return A clone of this {@code R} instance.
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				dst.values = values.length == 0 ?
						values :
						values.clone();
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		/**
		 * Compares this {@code BitList} to another object for equality.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 * Objects are considered equal if they are both {@code BitList} instances of the same class and have the same content.
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) );
		}
		
		/**
		 * Compares this {@code BitList} to another {@code BitList} for equality.
		 *
		 * @param other The {@code BitList} to compare with.
		 * @return {@code true} if the {@code BitLists} are equal, {@code false} otherwise.
		 * {@code BitLists} are considered equal if they have the same size, trailing ones count, and bit values.
		 */
		public boolean equals( R other ) {
			if( size() != other.size() || trailingOnesCount != other.trailingOnesCount ) return false;
			for( int i = used(); -1 < --i; ) if( values[ i ] != other.values[ i ] ) return false;
			return true;
		}
		
		/**
		 * Returns a string representation of this {@code BitList} in JSON format.
		 *
		 * @return A JSON string representation of the {@code BitList}.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the content of this {@code BitList} to a {@code JsonWriter}.
		 * The {@code BitList} is represented as a JSON array of 0s and 1s.
		 *
		 * @param json The {@code JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray();
			int size = size();
			if( 0 < size ) {
				json.preallocate( ( used() + ( trailingOnesCount >> LEN ) + 1 ) * 68 );
				for( int i = 0; i < trailingOnesCount; i++ ) json.value( 1 );
				
				int last1 = last1();
				
				for( int i = 0, k = last1 + 1 - trailingOnesCount; i < used; i++, k -= BITS ) {
					long v = values[ i ];
					
					for( int s = 0, limit = Math.min( BITS, k ); s < limit; s++ )
					     json.value( ( v & 1L << s ) == 0 ?
							                 0 :
							                 1 );
				}
				
				for( int i = last1; ++i < size; ) json.value( 0 );//trailing 0's
				
				
			}
			json.exitArray();
		}
		
		
		/**
		 * Converts this {@code BitList} to a byte array.
		 * The bits are packed into bytes in little-endian order.
		 *
		 * @return A byte array representing the bits in this {@code BitList}.
		 */
		public byte[] toByteArray() {
			int    byteLength    = size() + 7 >> 3;
			byte[] dst           = new byte[ byteLength ];
			int    dst_i         = 0;
			int    trailingBytes = trailingOnesCount >> 3;
			for( int i = 0; i < trailingBytes; i++ ) dst[ dst_i++ ] = ( byte ) 0xFF;
			int remainingTrailingBits = trailingOnesCount & 7;
			if( remainingTrailingBits > 0 ) dst[ dst_i++ ] = ( byte ) mask( remainingTrailingBits );
			int usedLongs = used();
			for( int i = 0; i < usedLongs; i++ ) {
				long data = values[ i ];
				int bytesToWrite = i == usedLongs - 1 ?
						size - trailingOnesCount - ( i << LEN ) + 7 >> 3 :
						8;
				for( int j = 0; j < bytesToWrite; j++ ) dst[ dst_i++ ] = ( byte ) ( data >>> j * 8 );
			}
			return dst;
		}
		
		/**
		 * Counts the number of leading zero bits in this {@code BitList}. Similar tp Long.numberOfLeadingZeros
		 * If the {@code BitList} starts with a sequence of zeros, this method returns the length of that sequence.
		 * If the {@code BitList} starts with a '1' or is all ones, it returns 0.
		 *
		 * @return The number of leading zero bits.
		 */
		public int numberOfLeading0() { return size - 1 - last1(); }
		
		/**
		 * Counts the number of trailing zero bits in this {@code BitList}. Similar tp Long.numberOfTrailingZeros
		 * If the {@code BitList} ends with a sequence of zeros, this method returns the length of that sequence.
		 * If the {@code BitList} ends with a '1' or is all ones, it returns 0.
		 *
		 * @return The number of trailing zero bits.
		 */
		public int numberOfTrailing0() {
			int i = next1( 0 );
			return i == -1 ?
					0 :
					i;
		}
		
		public int numberOfTrailing1() { return trailingOnesCount; }
		
		public int numberOfLeading1() {
			if( 0 < size ) {
				int last1 = last1();
				return last1 + 1 == size ?
						last1 - prev1( last1 - 1 ) :
						0;
			}
			return 0;
		}
	}
	
	/**
	 * {@code RW} class extends {@code R} to deliver a fully mutable implementation of the {@code BitList} interface.
	 * Building on the read-only capabilities of {@code R}, it adds robust methods for modifying bits, including
	 * setting, clearing, flipping, and performing bitwise operations. This class is designed for scenarios requiring
	 * dynamic bit manipulation with high performance and memory efficiency.
	 */
	class RW extends R {
		/**
		 * Constructs an empty {@code RW} BitList with a specified initial capacity.
		 *
		 * @param bits The initial capacity in bits.
		 */
		public RW( int bits ) {
			if( 0 < bits ) values = new long[ len4bits( bits ) ]; // If bits > 0, allocate an array of longs to hold the bits. The size of the array is calculated using len4bits.
		}
		
		
		/**
		 * Constructs a new {@code RW} BitList of a specified size, initialized with a default bit value.
		 *
		 * @param default_value The default bit value for all bits in the list ({@code true} for '1', {@code false} for '0').
		 * @param size          The number of bits in the {@code BitList}.
		 */
		public RW( boolean default_value, int size ) {
			if( 0 < size )
				if( default_value ) trailingOnesCount = this.size = size; // All bits are 1, stored efficiently as trailing ones
				else this.size = size;
		}
		
		/**
		 * Constructs a new {@code RW} BitList as a view of a section of another {@code BitList} (R).
		 * The new {@code BitList} contains bits from {@code from_bit} (inclusive) to {@code to_bit} (exclusive) of the source {@code BitList}.
		 *
		 * @param src      The source {@code BitList} to create a view from.
		 * @param from_bit The starting bit index in the source {@code BitList} (inclusive).
		 * @param to_bit   The ending bit index in the source {@code BitList} (exclusive).
		 */
		public RW( R src, int from_bit, int to_bit ) {
			if( src.size() <= from_bit || from_bit >= to_bit ) {
				size   = 0;
				values = Array.EqualHashOf._longs.O;
				used   = 0;
				return;
			}
			size = Math.min( to_bit, src.size() ) - from_bit;
			if( from_bit < src.trailingOnesCount ) {
				int onesInRange = Math.min( src.trailingOnesCount - from_bit, size );
				trailingOnesCount = onesInRange;
				if( size > onesInRange ) {
					values = new long[ len4bits( size - onesInRange ) ];
					used   = values.length | IO;
					src.get( values, from_bit + onesInRange, to_bit );
				}
			}
			else {
				values = new long[ len4bits( size ) ];
				used   = values.length | IO;
				src.get( values, from_bit, to_bit );
			}
		}
		
		/**
		 * Performs a bitwise AND operation between this {@code BitList} and another read-only {@code BitList}.
		 * The result is stored in this {@code BitList}, modifying it in place.
		 *
		 * @param and The other {@code BitList} to perform AND with.
		 * @return This {@code RW} instance after the AND operation.
		 */
		public RW and( R and ) {
			int minUsed = Math.min( used(), and.used() );
			for( int i = 0; i < minUsed; i++ ) values[ i ] &= and.values[ i ];
			for( int i = minUsed; i < used; i++ ) values[ i ] = 0;
			trailingOnesCount = Math.min( trailingOnesCount, and.trailingOnesCount );
			used |= IO;
			return this;
		}
		
		/**
		 * Performs a bitwise OR operation between this {@code BitList} and another read-only {@code BitList}.
		 * The result is stored in this {@code BitList}, modifying it in place.
		 *
		 * @param or The other {@code BitList} to perform OR with.
		 * @return This {@code RW} instance after the OR operation.
		 */
		public RW or( R or ) {
			if( or.used() < 1 && or.trailingOnesCount == 0 ) return this;
			trailingOnesCount = Math.max( trailingOnesCount, or.trailingOnesCount );
			int orUsed = or.used();
			if( values.length < orUsed ) values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), orUsed ) );
			int minUsed = Math.min( used(), orUsed );
			for( int i = 0; i < minUsed; i++ ) values[ i ] |= or.values[ i ];
			if( used < orUsed ) {
				System.arraycopy( or.values, minUsed, values, minUsed, orUsed - minUsed );
				used = orUsed;
			}
			else used |= IO;
			return this;
		}
		
		/**
		 * Performs a bitwise XOR operation between this {@code BitList} and another read-only {@code BitList}.
		 * The result is stored in this {@code BitList}, modifying it in place.
		 *
		 * @param xor The other {@code BitList} to perform XOR with.
		 * @return This {@code RW} instance after the XOR operation.
		 */
		public RW xor( R xor ) {
			trailingOnesCount = Math.min( trailingOnesCount, xor.trailingOnesCount ); // XOR cancels trailing '1's up to min
			int xorUsed = xor.used();
			if( values.length < xorUsed ) values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), xorUsed ) );
			int minUsed = Math.min( used(), xorUsed );
			for( int i = 0; i < minUsed; i++ ) values[ i ] ^= xor.values[ i ];
			if( used < xorUsed ) {
				System.arraycopy( xor.values, minUsed, values, minUsed, xorUsed - minUsed );
				used = xorUsed;
			}
			else used |= IO;
			return this;
		}
		
		/**
		 * Performs a bitwise AND NOT operation: {@code thisBitList AND NOT otherBitList}.
		 * Clears bits in this {@code BitList} where the corresponding bit in the {@code not} {@code BitList} is set.
		 *
		 * @param not The {@code BitList} to perform NOT and AND with.
		 * @return This {@code RW} instance after the AND NOT operation.
		 */
		public RW andNot( R not ) {
			trailingOnesCount = Math.max( 0, trailingOnesCount - not.trailingOnesCount );
			int notUsed = not.used();
			for( int i = 0; i < Math.min( used(), notUsed ); i++ ) values[ i ] &= ~not.values[ i ];
			used |= IO;
			return this;
		}
		
		/**
		 * Checks if this {@code BitList} intersects with another {@code BitList} (i.e., if there is at least one bit position where both are '1').
		 *
		 * @param set The other {@code BitList} to check for intersection.
		 * @return {@code true} if there is an intersection, {@code false} otherwise.
		 */
		public boolean intersects( R set ) {
			if( trailingOnesCount > 0 && set.trailingOnesCount > 0 ) return true;
			int minUsed = Math.min( used(), set.used() );
			for( int i = 0; i < minUsed; i++ ) if( ( values[ i ] & set.values[ i ] ) != 0 ) return true;
			return false;
		}
		
		/**
		 * Flips the bit at the specified position. If the bit is '0', it becomes '1', and vice versa.
		 *
		 * @param bit The bit position to flip (0-indexed).
		 * @return This {@code RW} instance after flipping the bit.
		 */
		public RW flip( int bit ) {
			if( bit < 0 || size <= bit ) return this;
			return get( bit ) ?
					set0( bit ) :
					set1( bit );
		}
		
		/**
		 * Flips a range of bits from {@code from_bit} (inclusive) to {@code to_bit} (exclusive).
		 * For each bit in the range, if it's '0', it becomes '1', and if it's '1', it becomes '0'.
		 *
		 * @param from_bit The starting bit position of the range to flip (inclusive).
		 * @param to_bit   The ending bit position of the range to flip (exclusive).
		 * @return This {@code RW} instance after flipping the bits in the specified range.
		 */
		public RW flip( int from_bit, int to_bit ) {
			if( from_bit >= to_bit || from_bit < 0 || size <= from_bit ) return this;
			if( size < to_bit ) size = to_bit;
			for( int i = from_bit; i < to_bit; i++ ) flip( i );
			return this;
		}
		
		/**
		 * Sets a sequence of bits starting at the specified index using boolean values.
		 *
		 * @param index  The starting bit index to set.
		 * @param values An array of boolean values representing the bits to set.
		 *               {@code true} sets the bit to '1', {@code false} sets it to '0'.
		 * @return This {@code RW} instance after setting the bits.
		 */
		public RW set( int index, boolean... values ) {
			if( index < 0 ) return this;
			int end = index + values.length;
			if( size < end ) size = end;
			
			for( int i = 0; i < values.length; i++ )
				if( values[ i ] ) set1( index + i );
				else set0( index + i );
			return this;
		}
		
		/**
		 * Sets the bit at the specified position to the given boolean value.
		 *
		 * @param bit   The bit position to set (0-indexed).
		 * @param value The boolean value to set the bit to ({@code true} for '1', {@code false} for '0').
		 * @return This {@code RW} instance after setting the bit.
		 */
		public RW set( int bit, boolean value ) {
			if( bit < 0 ) return this;
			if( size <= bit ) size = bit + 1;
			return value ?
					set1( bit ) :
					set0( bit );
		}
		
		/**
		 * Sets the bit at the specified position based on an integer value (non-zero for '1', zero for '0').
		 *
		 * @param bit   The bit position to set (0-indexed).
		 * @param value The integer value. If non-zero, the bit is set to '1'; otherwise, to '0'.
		 * @return This {@code RW} instance after setting the bit.
		 */
		public RW set( int bit, int value ) {
			return set( bit, value != 0 );
		}
		
		/**
		 * Sets the bit at the specified position based on an integer value, comparing it to a 'TRUE' value.
		 *
		 * @param bit   The bit position to set (0-indexed).
		 * @param value The integer value to check.
		 * @param TRUE  The integer value that represents 'true'. If {@code value} equals {@code TRUE}, the bit is set to '1'; otherwise, to '0'.
		 * @return This {@code RW} instance after setting the bit.
		 */
		public RW set( int bit, int value, int TRUE ) {
			return set( bit, value == TRUE );
		}
		
		/**
		 * Sets a range of bits from {@code from_bit} (inclusive) to {@code to_bit} (exclusive) to a specified boolean value.
		 *
		 * @param from_bit The starting bit position of the range to set (inclusive).
		 * @param to_bit   The ending bit position of the range to set (exclusive).
		 * @param value    The boolean value to set the bits to ({@code true} for '1', {@code false} for '0').
		 * @return This {@code RW} instance after setting the range of bits.
		 */
		public RW set( int from_bit, int to_bit, boolean value ) {
			if( from_bit >= to_bit || from_bit < 0 ) return this;
			if( size < to_bit ) size = to_bit;
			return value ?
					set1( from_bit, to_bit ) :
					set0( from_bit, to_bit );
		}
		
		/**
		 * Sets the bit at the specified position to '1'.
		 * This method efficiently manages the underlying storage, handling trailing ones optimization
		 * and dynamically expanding the storage array if necessary.
		 * It optimizes for cases where setting '1' extends a sequence of trailing '1's or existing '1's in the storage.
		 *
		 * <p><b>Trailing Ones Optimization:</b> If the bit to be set is immediately after the sequence of trailing '1's,
		 * this method attempts to extend the trailing '1's count to avoid explicitly storing these '1's in the {@code values} array,
		 * thus saving space. If extending trailing '1's involves shifting bits in the {@code values} array, it handles this efficiently.</p>
		 *
		 * <p><b>Array Expansion:</b> If the bit position is beyond the current allocated size of the {@code values} array,
		 * the array is automatically resized to accommodate the new bit.</p>
		 *
		 * @param bit The bit position to set to '1' (0-indexed). Must be non-negative.
		 * @return This {@code RW} instance after setting the bit to '1', enabling method chaining.
		 * @throws IllegalArgumentException if {@code bit} is negative (though the current implementation implicitly handles negative input by returning without action, best practice is to document intended behavior).
		 */
		public RW set1( int bit ) {
			// Step 1: If bit is already within trailing ones, no change needed
			if( bit < trailingOnesCount ) return this;
			
			// Step 2: Extend size if necessary
			if( size <= bit ) size = bit + 1;
			
			// Step 3: Handle setting bit immediately after trailing ones
			if( bit == trailingOnesCount ) {
				if( used() == 0 ) {
					trailingOnesCount++; // No explicit bits, just extend trailing ones
					return this;
				}
				
				int next1      = next1( trailingOnesCount + 1 ); // Next '1' after trailingOnesCount + 1
				int next0After = next0( next1 );           // Next '0' after next1
				int last1      = last1();                     // Last '1' in the list
				
				// Case 3.1: No more '1's after trailingOnesCount, just extend it
				if( size <= next1 ) {
					trailingOnesCount++;
					return this;
				}
				
				// Case 3.2: Merge with a contiguous run of '1's
				if( next1 <= last1 && next1 < next0After ) {
					int spanEnd         = Math.min( next0After, last1 + 1 ); // End of '1's span, capped at last1 + 1
					int newTrailingOnes = spanEnd; // New trailingOnesCount includes bit and span
					
					if( newTrailingOnes > last1 ) {
						// All bits are '1's, collapse into trailingOnesCount
						trailingOnesCount = newTrailingOnes;
						Arrays.fill( values, 0, used, 0 );
						used = 0;
					}
					else {
						// Shift remaining bits right
						int shift       = newTrailingOnes - trailingOnesCount;
						int valuesLast1 = last1 - trailingOnesCount;
						values            = shiftRight( values, values, 0, valuesLast1 + 1,
						                                next1 - trailingOnesCount, valuesLast1 + 1, shift, true );
						trailingOnesCount = newTrailingOnes;
						used |= IO; // Recalculate used
					}
					return this;
				}
				
				// Case 3.3: No contiguous span, extend trailingOnesCount and adjust values
				int bitOffset = bit - trailingOnesCount;
				int index     = bitOffset >> LEN;
				if( index >= values.length ) values = Arrays.copyOf( values, Math.max( values.length + ( values.length >> 1 ), index + 1 ) );
				trailingOnesCount++;
				used = Math.max( used, index + 1 );
				return this;
			}
			
			// Step 4: Set bit in values array
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN;
			int pos       = bitOffset & MASK;
			
			// Step 5: Expand values array if needed
			if( index >= values.length ) {
				int newLength = Math.max( values.length + ( values.length >> 1 ), index + 1 );
				values = Arrays.copyOf( values, newLength );
			}
			
			// Step 6: Update used count
			if( used <= index ) used = index + 1;
			
			// Step 7: Set the bit to '1'
			long current = values[ index ];
			long mask    = 1L << pos;
			if( ( current & mask ) == 0 ) values[ index ] = current | mask;
			
			return this;
		}
		
		/**
		 * Sets a range of bits from {@code from_bit} (inclusive) to {@code to_bit} (exclusive) to '1'.
		 * Modifies this BitList in place. This method efficiently handles the trailing ones optimization
		 * and expands the underlying storage as needed. If the range overlaps with or extends beyond
		 * the current trailing ones, it adjusts {@code trailingOnesCount} and shifts or sets bits in the
		 * {@code values} array accordingly.
		 *
		 * @param from_bit The starting bit position of the range to set to '1' (inclusive, 0-indexed).
		 * @param to_bit   The ending bit position of the range to set to '1' (exclusive, 0-indexed).
		 * @return This {@code RW} instance after setting the range of bits to '1', enabling method chaining.
		 */
		public RW set1( int from_bit, int to_bit ) {
			// Validate input range: return unchanged if invalid (negative start, empty, or inverted range)
			if( from_bit < 0 || to_bit <= from_bit ) return this;
			
			// Extend size to accommodate the range if necessary; new bits beyond current size default to '0'
			if( size < to_bit ) size = to_bit;
			
			// Case 1: Entire range is within or before trailing ones - no change needed if already '1's
			if( to_bit <= trailingOnesCount ) return this;
			
			// Get the index of the last '1' bit to determine how the range interacts with existing bits
			int last1 = last1();
			
			// Case 2: Range starts within or before trailing ones and extends beyond
			if( from_bit <= trailingOnesCount ) {
				// Extend the range to include any contiguous '1's following to_bit to optimize trailingOnesCount
				to_bit = next0( to_bit ); // Move to_bit to the next '0' to merge with any trailing '1's
				
				// If the range covers or exceeds the last '1', convert all bits up to to_bit to trailing ones
				if( last1 < to_bit ) {
					Arrays.fill( values, 0, used, 0 ); // Clear the values array as all bits become trailing ones
					used              = 0;                        // No bits remain in values array
					trailingOnesCount = to_bit;       // All bits up to to_bit are now '1's
					return this;
				}
				
				// Range partially overlaps trailing ones and extends into values; shift existing bits right
				int shiftAmount = to_bit - trailingOnesCount; // Number of bits to shift values right
				if( 0 < used ) {
					int last1InValues = last1 - trailingOnesCount; // Last '1' position in values array
					values = shiftRight( values, values, 0, last1InValues, 0, last1InValues, shiftAmount, true );
				}
				trailingOnesCount = to_bit; // Update trailingOnesCount to encompass the new range of '1's
				used |= IO;                // Mark used for recalculation due to potential trailing zeros
				return this;
			}
			
			// Case 3: Range is entirely beyond trailing ones - set bits directly in the values array
			int bitOffsetStart = from_bit - trailingOnesCount; // Start of range relative to end of trailing ones
			int bitOffsetEnd   = to_bit - 1 - trailingOnesCount; // End of range (inclusive) in values
			int fromIndex      = bitOffsetStart >> LEN;            // Starting long index in values array
			int toIndex        = bitOffsetEnd >> LEN;                // Ending long index in values array
			int fromBit        = bitOffsetStart & MASK;              // Bit offset within starting long
			int toBit          = bitOffsetEnd & MASK;                  // Bit offset within ending long
			
			// Expand values array if necessary to accommodate the range
			if( values.length <= toIndex ) values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), toIndex + 1 ) );
			if( used <= toIndex ) used = toIndex + 1; // Update used count if range extends beyond current usage
			
			// Set bits to '1' within the specified range in the values array
			if( fromIndex == toIndex ) {
				// Range within a single long: create a mask for the bits between fromBit and toBit
				long mask = mask( toBit + 1 - fromBit ) << fromBit;
				values[ fromIndex ] |= mask;
			}
			else {
				// Range spans multiple longs
				values[ fromIndex ] |= -1L << fromBit;              // Set all bits from fromBit to end of first long
				for( int i = fromIndex + 1; i < toIndex; i++ ) values[ i ] = -1L;                              // Set all bits in intermediate longs to '1'
				// If to_bit doesn't end at long boundary
				if( toBit != MASK ) values[ toIndex ] |= mask( toBit + 1 );           // Set bits from start of last long to toBit
			}
			
			return this; // Return this instance for method chaining
		}
		
		/**
		 * Sets the bit at the specified position to '0'.
		 * This method adjusts the internal representation of the BitList, which may involve modifying the
		 * trailingOnesCount (for trailing '1's) or the values array (for explicitly stored bits), and handles
		 * resizing if the bit position exceeds the current size. Bits beyond the current size are implicitly
		 * '0', so setting them to '0' only requires size adjustment.
		 *
		 * @param bit The bit position to set to '0' (0-indexed). Must be non-negative.
		 * @return This {@code RW} instance after setting the bit to '0', enabling method chaining.
		 */
		public RW set0( int bit ) {
			// Step 1: Validate input - negative indices are invalid
			if( bit < 0 ) return this;
			
			// Step 2: Handle out-of-bounds - extend size if bit exceeds current size (new bits are '0')
			if( size <= bit ) {
				size = bit + 1;
				return this;
			}
			
			// Step 3: Handle bit within trailingOnesCount region (all '1's)
			if( bit < trailingOnesCount ) {
				// Determine the last '1' in values (if any) for shifting
				int last1InValues = last1() - trailingOnesCount; // Adjust for new trailingOnesCount
				
				// Number of '1's to preserve after the cleared bit (bits from bit+1 to end of trailing ones)
				int preserve = trailingOnesCount - bit - 1;
				// New trailing ones count ends just before the cleared bit
				trailingOnesCount = bit;
				
				// If there are bits in values, shift them to make room
				if( 0 < last1InValues ) {
					int newIndex = index( last1InValues + 1 + preserve ); // Space for shifted + preserved bits
					long[] dst = newIndex < values.length ?
							values :
							Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), newIndex + 1 ) );
					
					values = shiftLeft( values, dst, 1, last1InValues + 1, 1, last1InValues + 1, preserve + 1, true );
					used   = Math.max( used, index( last1InValues + 1 + preserve ) + 1 );
				}
				else if( 0 < preserve ) {
					// Allocate space for preserved bits if values is empty
					int newIndex = index( 1 + preserve );
					if( values.length <= newIndex ) values = new long[ Math.max( values.length + ( values.length >> 1 ), newIndex + 1 ) ];
					used = newIndex + 1;
				}
				
				// Step 4: Set preserved '1's starting at position 1 in values (position 0 is the '0')
				if( 0 < preserve ) setBits( values, 1, 1 + preserve ); // From bit 1 to preserve count (exclusive)
				
				used |= IO; // Mark for recalculation
				return this;
			}
			
			// Step 5: Handle bit in values array (beyond trailingOnesCount)
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN;
			int pos       = bitOffset & MASK;
			
			// If bit is beyond used region, it's already '0'
			if( index >= used() ) return this;
			
			// Clear the bit
			values[ index ] &= ~( 1L << pos );
			
			// Update used if the last long becomes zero
			if( index == used() - 1 && values[ index ] == 0 ) used |= IO;
			
			return this;
		}
		
		/**
		 * Sets a range of bits from {@code from_bit} (inclusive) to {@code to_bit} (exclusive) to '0'.
		 * Modifies this BitList in place, handling trailing ones optimization and shifting preserved bits as needed.
		 * Invalid ranges (negative start, empty, or inverted) are ignored. If the range extends beyond the current size,
		 * the BitList is expanded with trailing '0's.
		 *
		 * @param from_bit Starting bit position to clear (inclusive, 0-indexed).
		 * @param to_bit   Ending bit position to clear (exclusive, 0-indexed).
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set0( int from_bit, int to_bit ) {
			// Validate input: skip if range is invalid (negative start, empty, or inverted)
			if( from_bit < 0 || to_bit <= from_bit ) return this;
			
			// Extend size to accommodate the range if necessary; new bits default to '0'
			if( size < to_bit ) size = to_bit;
			
			// Case 1: Entire range is within trailing ones - truncate and shift preserved bits into values
			if( to_bit < trailingOnesCount ) {
				// Calculate bits to preserve after to_bit (remaining trailing '1's, e.g.)
				int preserve = trailingOnesCount - to_bit;
				// Number of bits to move from trailingOnesCount into values (from_bit to end of trailing ones)
				int move_into_values = trailingOnesCount - from_bit;
				
				// Get the index of the last '1' in values relative to current trailingOnesCount
				int last1InValues = last1() - trailingOnesCount;
				
				// Truncate trailing ones to exclude bits starting from from_bit (e.g., keep only bit 0 as '1')
				trailingOnesCount = from_bit;
				
				if( last1InValues < 0 ) { // No bits in values initially (common case when all are trailing ones)
					// Ensure values array can hold the cleared range plus preserved bits
					int index = index( to_bit - from_bit + preserve ); // Total bits: cleared range + preserved
					if( values.length <= index ) values = new long[ Math.max( values.length + ( values.length >> 1 ), index + 1 ) ];
					used = index + 1;
					// Position preserved 1s after the cleared range (which remains 0 by default)
					int first1 = to_bit - from_bit - 1;
					setBits( values, 1 + first1, 1 + first1 + preserve ); // Set preserved '1's at correct offset
					return this;
				}
				
				// Resize values array if needed to hold existing bits plus moved bits
				int newIndex = last1InValues + move_into_values + 1;
				long[] dst = ( used = newIndex ) < values.length ?
						values :
						new long[ Math.max( values.length + ( values.length >> 1 ), newIndex ) ];
				
				// Shift existing values right to make room for bits moved from trailingOnesCount
				if( 0 <= last1InValues )
					values = shiftLeft( values, dst, 0, last1InValues, 0, last1InValues, move_into_values, true );
				else
					values = dst;
				
				// Set preserved '1's (if any) after the cleared range in values
				setBits( values, move_into_values - preserve, move_into_values );
				
				return this;
			}
			
			// Case 2: Range starts within trailing ones and extends into values
			if( from_bit < trailingOnesCount ) {
				// Number of '1's to preserve from from_bit to end of trailing ones
				int preserve = trailingOnesCount - from_bit;
				// Truncate trailing ones to exclude bits from from_bit onward
				trailingOnesCount = from_bit;
				
				// Calculate the end position in values array (relative to new trailingOnesCount)
				int bitOffsetEnd = to_bit - 1 - trailingOnesCount;
				int toIndex      = bitOffsetEnd >> LEN;
				int toBit        = bitOffsetEnd & MASK;
				
				// Get the last '1' position in values for shifting
				int last1InValues = last1() - from_bit;
				
				// Resize values array if needed
				long[] dst = toIndex < values.length ?
						values :
						new long[ Math.max( values.length + ( values.length >> 1 ), toIndex + 1 ) ];
				
				// Shift existing values right to align with new trailingOnesCount
				if( last1InValues >= 0 ) values = shiftLeft( values, dst, 0, last1InValues, 0, last1InValues, preserve, true );
				else values = dst;
				
				// Ensure used reflects the new extent
				if( toIndex >= used ) used = toIndex + 1;
				
				// Preserve any '1's from from_bit to original trailingOnesCount
				setBits( values, 1, 1 + preserve );
				
				// Clear bits from the end of preserved region to to_bit
				// Single long case: clear bits from preserve to toBit
				if( toIndex == 0 ) values[ 0 ] &= ~( mask( toBit + 1 ) & ~mask( preserve ) );
				else {
					// Multiple longs: clear from preserve in first long to toBit in last long
					values[ 0 ] &= mask( preserve );
					for( int i = 1; i < toIndex; i++ ) values[ i ] = 0;
					if( toBit != MASK ) values[ toIndex ] &= ~mask( toBit + 1 );
				}
				
				// Mark used for recalculation if the last long becomes 0
				if( toIndex + 1 == used && values[ toIndex ] == 0 ) used |= IO;
				
				return this;
			}
			
			// Case 3: Entire range is beyond trailing ones - clear bits directly in values
			int bitOffsetStart = from_bit - trailingOnesCount;
			int bitOffsetEnd   = to_bit - trailingOnesCount;
			
			// Only clear if the range overlaps with used bits
			if( bitOffsetStart < used() << LEN ) {
				unsetBits( values, bitOffsetStart, bitOffsetEnd );
				// Mark used for recalculation if the last long becomes 0
				if( used > 0 && values[ used - 1 ] == 0 ) used |= IO;
			}
			
			return this;
		}
		
		
		/**
		 * Appends a bit with the specified boolean value to the end of this {@code BitList}.
		 *
		 * @param value The boolean value of the bit to add ({@code true} for '1', {@code false} for '0').
		 * @return This {@code RW} instance after adding the bit.
		 */
		public RW add( boolean value ) { return set( size, value ); }
		
		/**
		 * Appends a sequence of bits from a {@code long} value to the end of this {@code BitList}.
		 * Appends up to 64 bits from the {@code long} value.
		 *
		 * @param src The {@code long} value source of bits to add.
		 * @return This {@code RW} instance after adding the bits.
		 */
		public RW add( long src ) { return add( src, BITS ); }
		
		/**
		 * Appends a specified number of bits from a {@code long} value to the end of this {@code BitList}.
		 *
		 * @param src  The {@code long} value source of bits to add.
		 * @param bits The number of bits to append from the {@code long} value (up to 64).
		 * @return This {@code RW} instance after adding the bits.
		 */
		public RW add( long src, int bits ) {
			if( bits > BITS ) bits = BITS;
			int _size = size;
			size += bits;
			if( ( src &= mask( bits ) ) == 0 ) return this;
			if( _size < trailingOnesCount ) {
				trailingOnesCount += bits;
				return this;
			}
			int bitOffset = _size - trailingOnesCount;
			int index     = bitOffset >> LEN;
			int pos       = bitOffset & MASK;
			if( values.length <= index ) values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), index + 2 ) );
			if( pos == 0 ) values[ index ] = src;
			else {
				values[ index ] |= src << pos;
				if( pos + bits > BITS ) values[ index + 1 ] = src >>> BITS - pos;
			}
			used = Math.max( used, index + ( pos + bits > BITS ?
					2 :
					1 ) );
			return this;
		}
		
		/**
		 * Sets a bit at a specific bit (index) with a boolean value.
		 * If the bit is out of bounds, the {@code BitList} is resized to accommodate it.
		 *
		 * @param bit   The index (bit position) to set.
		 * @param value The boolean value to set ({@code true} for '1', {@code false} for '0').
		 * @return This {@code RW} instance after setting the bit.
		 */
		public RW add( int bit, boolean value ) {
			if( bit < 0 ) return this;
			if( size <= bit ) size = bit + 1;
			return set( bit, value );
		}
		
		/**
		 * Removes the bit at the specified position, shifting subsequent bits to the left.
		 *
		 * @param bit The bit position to remove (0-indexed).
		 * @return This {@code RW} instance after removing the bit.
		 */
		public RW remove( int bit ) {
			// Validate input: return unchanged if bit is negative or beyond current size
			if( bit < 0 || size <= bit ) return this;
			
			// Reduce size since one bit is being removed, applies to all cases
			size--;
			
			// Case 1: Removing a bit within trailingOnesCount (all '1's)
			if( bit < trailingOnesCount ) {
				trailingOnesCount--; // Decrease the count of implicit leading '1's
				return this;
			}
			
			// Determine the position of the last '1' bit in the BitList
			int last1 = last1();
			// Case 2: Bit is beyond the last '1', in implicit trailing '0's, size already adjusted
			if( last1 < bit ) return this;
			
			// Case 3: Removing the last '1' bit, no subsequent bits to shift
			if( bit == last1 ) {
				set0( bit );          // Clear the bit at the last '1' position
				used |= IO;         // Invalidate used to force recalculation (set0 may not update it fully)
				return this;
			}
			
			// Calculate the last '1' position relative to the values array (after trailingOnesCount)
			int valuesLast1 = last1 - trailingOnesCount;
			
			// Case 4: Removing the first bit after trailingOnesCount (always '0' in values)
			if( bit == trailingOnesCount ) {
				int next0 = next0( bit + 1 ); // Find the next '0' after the bit to detect '1' spans
				
				// Subcase 4a: Next bit is '0', no '1' span to merge
				if( next0 == bit + 1 ) {
					// Shift all bits in values left by 1 to fill the gap
					// Note: from_bit = 1 assumes bitOffset = 0, correct only here
					shiftRight( values, values, 0, valuesLast1 + 1, 1, valuesLast1 + 1, 1, true );
					return this;
				}
				
				// Subcase 4b: No '0' after or next '0' beyond last '1', all remaining bits are '1's
				if( next0 == -1 || last1 < next0 ) {
					trailingOnesCount += valuesLast1; // Merge all bits in values into trailingOnesCount
					Arrays.fill( values, 0, used, 0 );  // Clear the values array
					used |= IO;                       // Invalidate used for recalculation
					return this;
				}
				
				// Subcase 4c: Partial span of '1's follows until next0
				int shift = next0 - bit;              // Length of '1' span including the removed position
				trailingOnesCount += shift;           // Extend trailingOnesCount by the span length
				// Shift remaining bits left by the span length
				// Note: from_bit = 1 assumes starting at values[0] bit 1, correct only if span aligns
				shiftRight( values, values, 0, valuesLast1 + 1, 1, valuesLast1 + 1, shift, true );
				return this;
			}
			
			// Default Case: Remove bit within values and shift subsequent bits left by 1
			shiftRight( values, values, 0, valuesLast1 + 1, 1, valuesLast1 + 1, 1, true );
			
			return this;
		}
		
		/**
		 * Inserts a '0' bit at the specified position, shifting existing bits to the right.
		 *
		 * @param bit The bit position to insert the '0' at (0-indexed).
		 * @return This {@code RW} instance after inserting the '0' bit.
		 */
		public RW insert0( int bit ) {
			// Prevent insertion at negative indices; no change needed
			if( bit < 0 ) return this;
			
			// Adjust size: if bit is beyond current size, set size to bit + 1 (implicitly padding with 0s);
			// otherwise, increment size to account for the inserted bit
			if( size <= bit ) size = bit + 1;
			else size++;
			
			// Get the index of the last '1' bit; if inserting after this, no bit shifting is needed
			// since trailing bits are implicitly 0
			int last1 = last1();
			if( last1 < bit ) return this;
			
			// Calculate the last '1' position relative to the values array (after trailingOnesCount)
			int valuesLast1 = last1 - trailingOnesCount;
			
			// Compute the bit position in the values array; use Math.abs to handle negative offsets
			// when bit < trailingOnesCount (e.g., bit = 0, trailingOnesCount = 4 → bitInValues = 4)
			int bitInValues = Math.abs( bit - trailingOnesCount );
			int index       = bitInValues >> LEN; // Index in values array (shift by 6 for 64-bit longs)
			
			// Ensure values array is large enough; if not, allocate with growth factor
			long[] dst = index < values.length ?
					values :
					new long[ Math.max( values.length + ( values.length >> 1 ), index + 2 ) ];
			
			// Case 1: Inserting within trailing ones (e.g., bit = 0, trailingOnesCount = 4)
			if( bit < trailingOnesCount ) {
				// Number of '1's to preserve after the insertion point
				int preserveTrailingOnes = trailingOnesCount - bit; // e.g., 4 - 0 = 4
				
				// Truncate trailingOnesCount to insertion point (the inserted 0 takes this position)
				trailingOnesCount = bit; // e.g., 0
				
				// Subcase 1a: If there are bits in values, shift them to accommodate preserved '1's
				if( 0 < used() ) {
					values = shiftLeft( values, dst, 0, valuesLast1 + 1, 0, valuesLast1 + 1, preserveTrailingOnes, true );
					used   = Math.max( used, index( preserveTrailingOnes + valuesLast1 + 1 ) + 1 );
				}
				// Subcase 1b: No bits in values, but need to preserve trailing '1's
				else {
					values = dst; // Use the potentially resized array
					used   = index + 1; // Set used to the full length of the new array
				}
				
				setBits( values, 1, 1 + preserveTrailingOnes ); // e.g., set bits 1-4
			}
			// Case 2: Inserting beyond trailingOnesCount (in values array)
			else {
				// Ensure used reflects the new index if insertion extends beyond current usage
				if( used() <= index ) used = index + 1;
				
				values = shiftLeft( values, dst, bitInValues, valuesLast1 + 1 + 1, bitInValues, valuesLast1 + 1, 1, true );
			}
			
			
			return this;
		}
		
		/**
		 * Inserts a '1' bit at the specified position, shifting existing bits to the right.
		 *
		 * @param bit The bit position to insert the '1' at (0-indexed).
		 * @return This {@code RW} instance after inserting the '1' bit.
		 */
		public RW insert1( int bit ) {
			// Case 0: Handle invalid input.
			if( bit < 0 ) return this; // No insertion for negative indices.
			
			// Case 1: Insertion within or at the boundary of the trailing '1's region.
			if( bit <= trailingOnesCount ) {
				trailingOnesCount++;// Inserting a '1' among trailing '1's simply extends the count of trailing '1's.
				size++; // Increment the size of the BitList as we are inserting a bit.
				return this;
			}
			
			// Case 2: Insertion within the values array (bits stored explicitly) or beyond.
			int bitInValues = bit - trailingOnesCount; // Calculate the bit position relative to the end of trailing '1's.
			int index       = bitInValues >> LEN;           // Determine the index of the long in the values array.
			
			// Ensure that the 'values' array is large enough to accommodate the index where we want to insert the bit.
			long[] dst = index < values.length ?
					values :
					new long[ Math.max( values.length + ( values.length >> 1 ), index + 2 ) ];
			
			// Optimization: Shift bits only if there are explicitly stored bits (used() > 0)
			// and if the insertion point is within or before the last '1' bit.
			if( 0 < used() ) {
				int last1       = last1(); // Get the index of the last '1' bit in the BitList.
				int valuesLast1 = last1 - trailingOnesCount; // Calculate the index of the last '1' bit relative to values array.
				
				if( bit <= last1 ) { // Check if the insertion bit is within the range of existing '1's or '0's in 'values'.
					// Shift bits to the right starting from the insertion point to make space for the inserted '1'.
					// This is necessary to maintain the correct order of bits after insertion.
					values = shiftLeft( values, dst, bit - trailingOnesCount, valuesLast1 + 1, bit - trailingOnesCount, valuesLast1, 1, false );
				}
			}
			else
				values = dst; // If no bits are used yet, just use the potentially resized 'dst' array.
			
			
			// Ensure 'used' count is updated to reflect the potentially new index.
			if( used <= index ) used = index + 1;
			
			values[ index ] |= 1L << ( bitInValues & MASK ); // Set the bit at the calculated index and position to '1'.
			
			// Ensure the size of the BitList is correctly updated.
			if( size < bit ) size = bit + 1; // If 'bit' was beyond the current size, extend size to 'bit + 1'.
			else size++; // Otherwise, just increment size by 1 for the inserted bit.
			
			return this; // Return the instance for method chaining.
		}
		
		
		/**
		 * Trims any trailing zero bits from the end of the {@code BitList}, reducing its size to the position of the last '1' bit.
		 *
		 * @return This {@code RW} instance after trimming trailing zero bits.
		 */
		public RW trim() {
			length( last1() + 1 );
			return this;
		}
		
		/**
		 * Reverses the order of all bits in this BitList in-place.
		 * The bit at index 0 swaps with the bit at index size-1, the bit at index 1 swaps with size-2, and so on.
		 * If the BitList has 0 or 1 bits, no changes are made as there’s nothing to reverse.
		 * <p>
		 * For example:
		 * - Input:  [1, 0, 1, 1] (size = 4)
		 * - Output: [1, 1, 0, 1]
		 *
		 * @return This BitList instance after reversing its bits, enabling method chaining.
		 */
		public RW reverse() {
			// Early return if the BitList is empty or has only one bit, as no reversal is needed
			if( size <= 1 ) return this;
			
			// Calculate the midpoint of the BitList (size / 2), since we only need to iterate halfway
			// Bits beyond this point are swapped with earlier bits, avoiding redundant operations
			int halfSize = size >> 1; // Equivalent to size / 2, using bitwise right shift for efficiency
			
			// Iterate from the start to the midpoint, swapping each bit with its mirror counterpart
			for( int i = 0; i < halfSize; i++ ) {
				// Get the bit at the current index (left side of the list)
				boolean bit1 = get( i );
				// Get the bit at the mirrored index from the end (right side of the list)
				boolean bit2 = get( size - 1 - i );
				
				// Swap the bits: set the left bit to the right bit’s value and vice versa
				set( i, bit2 );
				set( size - 1 - i, bit1 );
			}
			
			// Return this instance to support method chaining (e.g., bitList.reverse().trim())
			return this;
		}
		
		
		/**
		 * Initializes this {@code BitList} from a byte array.
		 * The byte array is interpreted as a little-endian representation of the bits.
		 *
		 * @param bytes The byte array to initialize from.
		 * @return This {@code RW} instance after initialization from the byte array.
		 */
		public RW fromArray( byte[] bytes ) {
			clear();
			if( bytes == null || bytes.length == 0 ) return this;
			size = bytes.length * 8;
			int i = 0;
			while( i < bytes.length && bytes[ i ] == ( byte ) 0xFF ) i++;
			trailingOnesCount = i * 8;
			if( trailingOnesCount == size ) return this;
			int longLengthNeeded = len4bits( size - trailingOnesCount );
			if( values.length < longLengthNeeded ) values = Array.copyOf( values, longLengthNeeded );
			int longIndex = 0;
			int bitOffset = 0;
			for( long b = bytes[ i ] & 0xFFL; i < bytes.length; b = bytes[ ++i ] & 0xFFL )
				if( bitOffset <= BITS - 8 ) {
					values[ longIndex ] |= b << bitOffset;
					bitOffset += 8;
					if( bitOffset == BITS ) {
						bitOffset = 0;
						longIndex++;
					}
				}
				else {
					int remainingBitsInLong = BITS - bitOffset;
					values[ longIndex ] |= b << bitOffset;
					longIndex++;
					values[ longIndex ] |= b >>> remainingBitsInLong;
					bitOffset = 8 - remainingBitsInLong;
				}
			
			used = longIndex + ( bitOffset > 0 ?
					1 :
					0 );
			return this;
		}
		
		/**
		 * Resizes the underlying storage to exactly fit the current size of the {@code BitList}, potentially reducing memory usage.
		 *
		 * @return This {@code RW} instance after fitting the storage size.
		 */
		public RW fit() { return length( size() ); }
		
		/**
		 * Sets the length of this {@code BitList} to the specified number of bits.
		 * If the new length is less than the current size, the {@code BitList} is truncated.
		 * If the new length is greater, it is padded with zero bits at the end.
		 *
		 * @param bits The new length of the {@code BitList} in bits.
		 * @return This {@code RW} instance after setting the length.
		 */
		public RW length( int bits ) {
			if( bits < 0 ) {
				clear();
				return this;
			}
			if( bits < size ) {
				set0( bits, size );
				size = bits;
			}
			if( bits <= trailingOnesCount ) {
				trailingOnesCount = bits;
				values            = Array.EqualHashOf._longs.O;
				used              = 0;
			}
			else {
				int newLength = index( bits - trailingOnesCount ) + 1;
				if( values.length > newLength ) values = Array.copyOf( values, newLength );
				used = Math.min( used, newLength );
			}
			return this;
		}
		
		/**
		 * Sets the size of the {@code BitList}.
		 * If the new size is smaller than the current size, the {@code BitList} is truncated, and bits beyond the new size are discarded.
		 * If the new size is larger, the {@code BitList} is expanded, and new bits are initialized to '0'.
		 *
		 * @param size The new size of the {@code BitList}.
		 * @return This {@code RW} instance after resizing.
		 */
		public RW size( int size ) {
			if( size < this.size ) if( size < 1 ) clear();
			else {
				set0( size, size() );
				this.size = size;
			}
			else if( size() < size ) this.size = size;
			return this;
		}
		
		/**
		 * Clears all bits in this {@code BitList}, resetting it to an empty state.
		 * This operation resets size, trailing ones count, and clears the values array.
		 *
		 * @return This {@code RW} instance after clearing all bits.
		 */
		public RW clear() {
			java.util.Arrays.fill( values, 0, used, 0L );
			used              = 0;
			size              = 0;
			trailingOnesCount = 0;
			return this;
		}
		
		/**
		 * Creates and returns a deep copy of this {@code RW} instance.
		 * The cloned object will have the same size, trailing ones count, and bit values as the original.
		 *
		 * @return A clone of this {@code RW} instance.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		
		/**
		 * Shifts all bits in this BitList to the left by the specified number of positions.
		 * Bits shifted out of the leftmost positions are discarded, and vacated positions on the right are filled with '0's.
		 * The size of the BitList increases by the shift amount, reflecting the new bit positions.
		 *
		 * @param n Number of positions to shift left; must be non-negative.
		 * @return This RW instance after the left shift operation.
		 * @throws IllegalArgumentException if n is negative.
		 */
		public RW shiftLeft( int n ) {
			if( n < 0 ) throw new IllegalArgumentException( "Shift value must be non-negative" );
			if( n == 0 || isAllZeros() ) return this;
			if( size <= n ) {
				clear();
				return this;
			}
			int last1InValues = last1() - trailingOnesCount; // Highest '1' bit index in values array
			int index         = last1InValues + n;                  // New highest index after shift
			
			// Resize values array if needed, using Math.max for growth factor
			long[] dst = ( used = index + 1 ) < values.length ?
					values :
					new long[ Math.max( values.length + ( values.length >> 1 ), used ) ];
			
			// Perform the left shift on the values array, clearing vacated bits
			values = shiftLeft( values, dst, 0, last1InValues, 0, last1InValues, n, true );
			
			if( 0 < trailingOnesCount ) setBits( values, n, n + trailingOnesCount ); // If there are trailing ones, shift them into values at position n
			
			trailingOnesCount = 0; // Trailing ones are shifted out or absorbed into values
			size += n;            // Increase size to account for new zero-filled bits on the right
			
			return this;
		}
		
		/**
		 * Shifts all bits in this BitList to the right by the specified number of positions.
		 * Bits shifted out of the rightmost positions are discarded, and vacated positions on the left
		 * are filled with zeros. The size of the BitList is reduced by the shift amount.
		 * <p>
		 * If the shift amount is zero or the BitList is all zeros, no changes are made. If the shift
		 * amount equals or exceeds the current size, the BitList is cleared.
		 *
		 * @param n Number of positions to shift right, must be non-negative.
		 * @return This RW instance after the right shift operation.
		 * @throws IllegalArgumentException if n is negative.
		 */
		public RW shiftRight( int n ) {
			if( n < 0 ) throw new IllegalArgumentException( "Shift value must be non-negative" );
			if( n == 0 || isAllZeros() ) return this;
			if( n >= size ) {
				clear();
				return this;
			}
			
			if( n < trailingOnesCount ) trailingOnesCount -= n;
			else {
				int shiftInValues = n - trailingOnesCount; // Bits to shift in values array
				trailingOnesCount = 0;
				int last1InValues = last1();//trailingOnesCount == 0 !!!
				
				if( shiftInValues < last1InValues - 2 )
					shiftRight( values, values, 1, last1InValues, 0, last1InValues, shiftInValues, true );
				else {
					if( shiftInValues == last1InValues - 1 ) trailingOnesCount = 1; // Single '1' remains in values, move into the trailingOnesCount
					Arrays.fill( values, 0, used, 0 );
					used = 0;
				}
				
				used |= IO;
			}
			size -= n;
			return this;
		}
		
		
		/**
		 * Shifts a range of bits from a source {@code long} array to a destination {@code long} array to the right
		 * (towards lower bit indices, like right bit-shift >>> on primitives) by a specified number of positions.
		 *
		 * <p>Within the given range, bits are moved from higher bit positions to lower bit positions. Bits shifted out
		 * from the lower end of the range are discarded. Vacated bit positions created at the higher end of the range are
		 * filled with zeros if {@code clear} is true; otherwise, they retain their original values (though in a right-shift,
		 * zero-filling is the typical and often intended behavior).</p>
		 *
		 * <p>This method operates at the bit level and correctly manages bit transitions across 64-bit {@code long} boundaries.
		 * The shift operation is performed out-of-place. The source {@code src} array remains unchanged, and the shifted bits
		 * are placed in the {@code dst} array. It's assumed that {@code dst} array has enough capacity to hold the shifted
		 * result within the specified ranges. Bits outside the specified {@code from_bit} to {@code to_bit} range in {@code dst}
		 * are not guaranteed to be preserved from {@code src} and might be zeroed or undefined depending on array initialization.</p>
		 *
		 * <p>If the shift distance ({@code shift_bits}) is equal to or greater than the range size ({@code to_bit} - {@code from_bit}),
		 * and {@code clear} is {@code true}, then all bits within the specified range in {@code dst} will be set to zero.</p>
		 *
		 * @param src           The source {@code long} array from which to perform the bit shift operation. This array is not modified.
		 * @param dst           The destination {@code long} array where the shifted bits will be placed. It's assumed to be of sufficient size.
		 * @param array_min_bit The starting bit index (inclusive) of the valid operational range within both {@code src} and {@code dst} arrays.
		 *                      Bits before this index are not considered by this method.
		 * @param array_max_bit The ending bit index (exclusive) of the valid operational range within both {@code src} and {@code dst} arrays.
		 *                      Bits at or after this index are not considered by this method.
		 * @param from_bit      The starting bit index (inclusive) of the specific range of bits to be shifted *within* the valid operational range.
		 * @param to_bit        The ending bit index (exclusive) of the specific range of bits to be shifted *within* the valid operational range.
		 * @param shift_bits    The number of bit positions to shift to the right. Must be a non-negative integer.
		 * @param clear         If {@code true}, the vacated bit positions at the higher end of the shifted range in {@code dst} are filled with '0' bits.
		 *                      If {@code false}, the vacated bits may retain their original values (though zero-filling is generally the expected
		 *                      behavior for right shifts in most contexts).
		 * @return The destination array {@code dst} with the shifted bits.
		 */
		static long[] shiftRight( long[] src, long[] dst, int array_min_bit, int array_max_bit, int from_bit, int to_bit, int shift_bits, boolean clear ) {
			// Validate input range and shift amount
			if( to_bit <= from_bit || shift_bits < 1 ) {
				if( src != dst ) {
					System.arraycopy( src, from_bit >> LEN, dst, from_bit >> LEN, ( to_bit - 1 >> LEN ) - ( from_bit >> LEN ) + 1 );
				}
				return dst;
			}
			
			// Normalize range boundaries to stay within array_min_bit and array_max_bit
			from_bit = Math.max( from_bit, array_min_bit );
			to_bit   = Math.min( to_bit, array_max_bit );
			
			// If the shift moves all bits out of the valid range, clear the range if specified
			if( to_bit - shift_bits <= array_min_bit ) {
				if( clear ) {
					unsetBits( dst, from_bit, to_bit );
				}
				return dst;
			}
			
			// Compute indices for the long array and the shift amounts
			int fromIndex  = from_bit >> LEN;      // Starting word index
			int toIndex    = to_bit - 1 >> LEN;    // Ending word index
			int shiftLongs = shift_bits >> LEN;   // Number of whole words to shift
			int shiftBits  = shift_bits & MASK;    // Remaining bits to shift within a word
			
			// Store original boundary values for masking (to preserve bits outside the range)
			long firstOriginal = fromIndex < src.length ?
					src[ fromIndex ] :
					0;
			long lastOriginal = toIndex < src.length ?
					src[ toIndex ] :
					0;
			
			// Perform the shift operation
			if( shiftBits == 0 && shiftLongs > 0 ) {
				// If shifting by whole words, use arraycopy
				System.arraycopy( src, fromIndex + shiftLongs, dst, fromIndex, toIndex - fromIndex - shiftLongs + 1 );
			}
			else {
				// Shift bits across words, handling bit-level shifts
				for( int srcIndex = fromIndex, destIndex = fromIndex; srcIndex <= toIndex; srcIndex++, destIndex++ ) {
					dst[ destIndex ] =
							( srcIndex - shiftLongs >= 0 && srcIndex - shiftLongs < src.length ?
									src[ srcIndex - shiftLongs ] >>> shiftBits :
									0 ) |
							( srcIndex - shiftLongs + 1 >= 0 && srcIndex - shiftLongs + 1 < src.length ?
									src[ srcIndex - shiftLongs + 1 ] << BITS - shiftBits :
									0 );
				}
			}
			
			// Compute bit positions within the words for boundary adjustments
			int fromBit = from_bit & MASK;
			int toBit   = to_bit - 1 & MASK;
			
			// Preserve bits before from_bit in the first word
			if( fromBit != 0 ) {
				long mask = mask( fromBit );
				dst[ fromIndex ] = dst[ fromIndex ] & ~mask | firstOriginal & mask;
			}
			
			// Preserve bits after to_bit - 1 in the last word, but only if to_bit - 1 is not the last bit in the operational range
			if( toBit != MASK && to_bit - 1 < array_max_bit - 1 ) {
				long mask = mask( toBit + 1 );
				dst[ toIndex ] = dst[ toIndex ] & mask | lastOriginal & ~mask;
			}
			
			// Clear vacated bits at the higher end of the range after shifting
			if( clear && from_bit + shift_bits < to_bit ) {
				// Clear the bits that were vacated at the higher end (from to_bit - shift_bits to to_bit)
				int clearFrom = Math.max( from_bit, to_bit - shift_bits );
				unsetBits( dst, clearFrom, to_bit );
			}
			
			return dst;
		}
		
		
		/**
		 * Shifts a range of bits from a source {@code long} array to a destination {@code long} array to the left (towards higher bit indices, like left bit-shift << on primitives) by a specified number of positions.
		 *
		 * <p>Within the given range, bits are moved from lower bit positions to higher bit positions. Bits shifted out from the higher end of the range are discarded.
		 * Vacated bit positions created at the lower end of the range are filled with zeros if {@code clear} is true; otherwise, their original values are retained (though this is generally not relevant in a left-shift scenario where vacated bits are typically intended to be zeroed or filled by subsequent operations).</p>
		 *
		 * <p>This method operates at the bit level and correctly manages bit transitions across 64-bit {@code long} boundaries.
		 * The shift operation is performed out-of-place. The source {@code src} array remains unchanged, and the shifted bits are placed in the {@code dst} array.
		 * It's assumed that {@code dst} array has enough capacity to hold the shifted result within the specified ranges.
		 * Bits outside the specified {@code from_bit} to {@code to_bit} range in {@code dst} are not guaranteed to be preserved from {@code src} and might be zeroed or undefined depending on array initialization.</p>
		 *
		 * <p>If the shift distance ({@code shift_bits}) is equal to or greater than the range size ({@code to_bit} - {@code from_bit}),
		 * and {@code clear} is {@code true}, then all bits within the specified range in {@code dst} will be set to zero.</p>
		 *
		 * @param src           The source {@code long} array from which to perform the bit shift operation. This array is not modified.
		 * @param dst           The destination {@code long} array where the shifted bits will be placed. It's assumed to be of sufficient size.
		 * @param array_min_bit The starting bit index (inclusive) of the valid operational range within both {@code src} and {@code dst} arrays. Bits before this index are considered outside the scope of this method.
		 * @param array_max_bit The ending bit index (exclusive) of the valid operational range within both {@code src} and {@code dst} arrays. Bits at or after this index are considered outside the scope of this method.
		 * @param from_bit      The starting bit index (inclusive) of the specific range of bits to be shifted *within* the valid operational range.
		 * @param to_bit        The ending bit index (exclusive) of the specific range of bits to be shifted *within* the valid operational range.
		 * @param shift_bits    The number of bit positions to shift to the left. Must be a non-negative integer.
		 * @param clear         If {@code true}, the vacated bit positions at the lower end of the shifted range in {@code dst} are filled with '0' bits.
		 *                      If {@code false}, the vacated bits may retain their original values (though behavior in left shift usually implies zero-filling is expected).
		 */
		static long[] shiftLeft( long[] src, long[] dst, int array_min_bit, int array_max_bit, int from_bit, int to_bit, int shift_bits, boolean clear ) {
			if( to_bit <= from_bit || shift_bits < 1 ) { // Invalid range or negative shift
				if( src != dst ) System.arraycopy( src, from_bit >> LEN, dst, from_bit >> LEN, ( to_bit - 1 >> LEN ) - ( from_bit >> LEN ) + 1 );
				return dst;
			}
			
			// Clamp range to valid boundaries
			from_bit = Math.max( from_bit, array_min_bit );
			to_bit   = Math.min( to_bit, array_max_bit );
			
			// Early exit if shift moves all bits out of range
			if( array_max_bit <= from_bit + shift_bits ) {
				if( clear ) unsetBits( dst, from_bit, to_bit );
				return dst;
			}
			
			int fromIndex = from_bit >> LEN;   // Starting long index (LEN = 6, so >> 6 is divide by 64)
			int toIndex   = to_bit - 1 >> LEN;  // Ending long index (to_bit is exclusive)
			
			int shiftLongs = shift_bits >> LEN; // Whole long words to shift
			int shiftBits  = shift_bits & MASK;  // Remaining bits to shift
			
			// Check if shift exceeds range
			if( toIndex - fromIndex + 1 <= shiftLongs ) {
				if( clear ) unsetBits( dst, from_bit, to_bit ); // Operate on dst now
				return dst;
			}
			
			// Save boundary values to preserve bits outside the range after shifting
			// firstOriginal stores the long at the start position where shifting begins in the destination
			long firstOriginal = fromIndex + shiftLongs < src.length ?
					src[ fromIndex + shiftLongs ] :
					0;
			// lastOriginal stores the long at the end position in the source
			long lastOriginal = toIndex < src.length ?
					src[ toIndex ] :
					0;
			
			// If shifting by whole longs only (no partial bits)
			if( shiftBits == 0 && shiftLongs > 0 )
				System.arraycopy( src, fromIndex, dst, fromIndex + shiftLongs, toIndex - fromIndex + 1 - shiftLongs ); // src to dst
			else {
				int dstIndex = toIndex;
				int srcIndex = toIndex - shiftLongs;
				while( srcIndex >= fromIndex ) {
					long hi = src[ srcIndex ] << shiftBits;
					long lo = srcIndex > fromIndex ?
							src[ srcIndex - 1 ] >>> BITS - shiftBits :
							0L;
					dst[ dstIndex ] = hi | lo;
					dstIndex--;
					srcIndex--;
				}
			}
			
			
			int fromBit = from_bit & MASK;
			int toBit   = to_bit - 1 & MASK;
			
			
			if( fromBit != 0 ) { //the shift starts partway through a long. We need to preserve the bits below fromBit in the destination.
				long mask = mask( fromBit );
				dst[ fromIndex + shiftLongs ] = dst[ fromIndex + shiftLongs ] & ~mask |// Inverts the mask to select bits below fromBit. Keeps the lower bits unchanged after the shift.
				                                firstOriginal & mask;//Extracts the lower bits from the original value.
			}
			
			if( toBit != 63 && array_max_bit < to_bit - 1 ) { //We need to preserve bits above toBit.
				long mask = mask( toBit + 1 );//Mask with 1s above toBit (e.g., if toBit = 3, mask(4) = 0b1111, ~mask = 0b...11110000).
				dst[ toIndex ] = dst[ toIndex ] & ~mask | //Keeps the upper bits after shifting.
				                 lastOriginal & mask; // Extracts the upper bits from the original value.
			}
			
			if( clear ) unsetBits( dst, to_bit - shift_bits, to_bit );
			return dst;
		}
		
		
		/**
		 * Sets all bits in the specified range of a {@code long} array to zero.
		 * Bits outside the range remain unchanged. The array is modified in-place.
		 *
		 * @param array    The {@code long} array to modify.
		 * @param from_bit Starting bit index of the range to clear (inclusive).
		 * @param to_bit   Ending bit index of the range to clear (exclusive).
		 */
		static void unsetBits( long[] array, int from_bit, int to_bit ) {
			if( to_bit <= from_bit ) return;
			int fromIndex = from_bit >> LEN;
			int toIndex   = to_bit - 1 >> LEN;
			int fromBit   = from_bit & MASK;
			int toBit     = to_bit - 1 & MASK;
			
			if( array.length <= fromIndex ) return;
			if( array.length <= toIndex ) toIndex = array.length - 1;
			
			if( fromIndex == toIndex ) array[ fromIndex ] &= ~( mask( toBit + 1 - fromBit ) << fromBit );
			else {
				if( fromBit != 0 ) {
					array[ fromIndex ] &= mask( fromBit );
					fromIndex++;
				}
				for( int i = fromIndex; i < toIndex; i++ ) array[ i ] = 0;
				if( toBit != MASK ) array[ toIndex ] &= ~mask( toBit + 1 );
			}
		}
		
		/**
		 * Sets bits from from_bit to to_bit (exclusive of to_bit) in the array.
		 *
		 * @param array    The long array representing the bitset.
		 * @param from_bit The starting bit index (inclusive).
		 * @param to_bit   The ending bit index (exclusive).
		 */
		static void setBits( long[] array, int from_bit, int to_bit ) {
			if( to_bit <= from_bit ) return; // Nothing to set if range is invalid
			
			int fromIndex = from_bit >> LEN; // Calculate index of the long containing the from_bit
			int toIndex   = to_bit - 1 >> LEN; // Calculate index of the long containing the to_bit (exclusive, hence to_bit - 1)
			int fromBit   = from_bit & MASK;     // Bit position within the long for from_bit
			int toBit     = to_bit - 1 & MASK;   // Bit position within the long for to_bit (exclusive, hence to_bit - 1)
			
			if( array.length <= fromIndex ) return; // from_bit is out of array bounds
			if( array.length <= toIndex ) toIndex = array.length - 1; // to_bit is out of array bounds, clamp to the last index
			
			// All bits to set are within the same long
			if( fromIndex == toIndex ) array[ fromIndex ] |= mask( toBit + 1 - fromBit ) << fromBit;
			else {
				// Bits to set span across multiple longs
				if( fromBit != 0 ) {
					// Set bits from fromBit to the end of the first long
					array[ fromIndex ] |= ~mask( fromBit );
					fromIndex++; // Move to the next long
				}
				// Set all bits to 1 for the longs in between
				for( int i = fromIndex; i < toIndex; i++ ) array[ i ] = -1; // -1L is all bits set to 1 in long
				// Set bits from the beginning of the last long up to toBit
				if( toBit != MASK ) array[ toIndex ] |= mask( toBit + 1 );
			}
		}
	}
}