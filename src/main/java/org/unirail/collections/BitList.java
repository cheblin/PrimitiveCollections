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
 * Represents a dynamic list of bits, optimized for both space (memory footprint)
 * and time (performance) efficiency.
 * <p>
 * {@code BitList} acts like a highly efficient, growable bit vector or bitset,
 * suitable for managing large sequences of boolean flags or performing complex
 * bitwise operations. It employs internal optimizations, such as tracking
 * sequences of leading '1's implicitly, to minimize memory usage.
 * <p>
 * Implementations provide methods for querying individual bits, finding runs of
 * '0's or '1's, calculating cardinality (rank/population count), and potentially
 * modifying the bit sequence.
 */
public interface BitList {
	/**
	 * An abstract base class providing a read-only view and core implementation
	 * details for a {@code BitList}.
	 * <p>
	 * <b>Bit Indexing and Representation:</b>
	 * .                 MSB                LSB
	 * .                 |                 |
	 * bits in the list [0, 0, 0, 1, 1, 1, 1] Leading 3 zeros and trailing 4 ones
	 * index in the list 6 5 4 3 2 1 0
	 * shift left <<
	 * shift right >>>
	 * <p>
	 * <b>Storage Optimization:</b>
	 * This class utilizes several optimizations:
	 * <ul>
	 *     <li><b>Trailing Ones (`trailingOnesCount`):</b> A sequence of '1' bits at the
	 *     beginning (indices 0 to {@code trailingOnesCount - 1}) are stored implicitly
	 *     by just keeping count, not using space in the {@code values} array.</li>
	 *     <li><b>Explicit Bits (`values`):</b> Bits *after* the implicit trailing ones
	 *     are packed into a {@code long[]} array. The first conceptual bit stored in
	 *     {@code values} always corresponds to the first '0' bit after the trailing ones.
	 *     The last bit stored in {@code values} corresponds to the highest-indexed '1'
	 *     bit ({@link #last1()}).</li>
	 *     <li><b>Trailing Zeros:</b> '0' bits from index {@code last1() + 1} up to
	 *     {@code size() - 1} are also implicit and not stored in {@code values}.</li>
	 *     <li><b>Used Count (`used`):</b> Tracks how many {@code long} elements in the
	 *     {@code values} array actually contain non-zero data, allowing the array to
	 *     potentially be larger than strictly needed for current bits.</li>
	 * </ul>
	 * This structure provides the foundation for concrete readable and writable
	 * {@code BitList} implementations.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		/**
		 * The logical number of bits in this list. This defines the valid range of
		 * indices [0, size-1].
		 * It includes implicitly stored trailing ones and trailing zeros, as well as
		 * explicitly stored bits in {@code values}.
		 */
		protected int size              = 0;
		/**
		 * The count of consecutive '1' bits starting from index 0. These bits are
		 * stored implicitly and are *not* represented in the {@code values} array.
		 * If {@code trailingOnesCount} is 0, the list starts with a '0' (or is empty).
		 */
		protected int trailingOnesCount = 0;
		
		/**
		 * The backing array storing the explicit bits of the {@code BitList}.
		 * <p>
		 * Contains bits from index {@code trailingOnesCount} up to {@link #last1()}.
		 * Bits are packed into {@code long}s, 64 bits per element.
		 * Within each {@code long}, bits are stored LSB-first (index 0 of the conceptual
		 * sub-array of 64 bits corresponds to the lowest index within that block).
		 * The {@code values} array element at index {@code i} stores bits corresponding
		 * to the global bit indices
		 * {@code [trailingOnesCount + i*64, trailingOnesCount + (i+1)*64 - 1]}.
		 * <p>
		 * Trailing zeros beyond {@link #last1()} up to {@link #size()} are not stored.
		 * May contain trailing {@code long} elements that are all zero.
		 * Initialized to a shared empty array for efficiency.
		 */
		protected long[] values = Array.EqualHashOf._longs.O;
		/**
		 * The number of {@code long} elements currently used in the {@code values} array.
		 * This is the index of the highest element containing a '1' bit, plus one.
		 * It can be less than {@code values.length}.
		 * A negative value (specifically, having the sign bit set via {@code used |= IO})
		 * indicates that the count might be stale (due to operations like clearing bits
		 * in the last used word) and needs recalculation via {@link #used()}.
		 */
		protected int    used   = 0;
		
		/**
		 * Returns the logical size (number of bits) of this {@code BitList}.
		 * This determines the valid range of bit indices [0, size-1].
		 *
		 * @return The number of bits in the list.
		 */
		public int size() { return size; }
		
		/**
		 * Calculates the minimum number of {@code long} elements needed to store a
		 * given number of bits.
		 *
		 * @param bits The number of bits.
		 * @return The required length of a {@code long[]} array.
		 */
		static int len4bits( int bits ) { return bits + BITS - 1 >> LEN; }
		
		/**
		 * The base-2 logarithm of {@link #BITS}, used for calculating array indices
		 * ({@code bit >> LEN}). Value is 6.
		 */
		protected static final int LEN  = 6;
		/**
		 * The number of bits in a {@code long}. Value is 64.
		 */
		protected static final int BITS = 1 << LEN; // 64
		/**
		 * A mask to extract the bit position within a {@code long} element
		 * ({@code bit & MASK}). Value is 63 (0b111111).
		 */
		protected static final int MASK = BITS - 1; // 63
		
		/**
		 * Calculates the index within the {@code values} array corresponding to a
		 * global bit index. Note: This does *not* account for {@code trailingOnesCount}.
		 * The bit index must be relative to the start of the `values` array.
		 *
		 * @param bit The bit index *relative to the start of the {@code values} array*.
		 * @return The index in the {@code values} array.
		 */
		static int index( int bit ) { return bit >> LEN; }
		
		/**
		 * Creates a {@code long} mask with the least significant {@code bits} set to '1'.
		 * For example, {@code mask(3)} returns {@code 0b111} (7).
		 * If {@code bits} is 0, returns 0. If {@code bits} is 64 or more, returns -1L (all ones).
		 *
		 * @param bits The number of low-order bits to set (0-64).
		 * @return A {@code long} with the specified number of LSBs set.
		 */
		static long mask( int bits ) { return -1L >>> 64 - bits; }
		
		/**
		 * Integer maximum value constant ({@code 0x7FFFFFFF}). Used for bit manipulation on {@code used}.
		 */
		static final int OI = Integer.MAX_VALUE;
		/**
		 * Integer minimum value constant ({@code 0x80000000}). Used to mark the {@code used} count as potentially stale.
		 */
		static final int IO = Integer.MIN_VALUE;
		
		/**
		 * Calculates or retrieves the number of {@code long} elements in the
		 * {@code values} array that are actively used (contain at least one '1' bit).
		 * <p>
		 * If the internal {@code used} field is non-negative, it's considered accurate
		 * and returned directly. If it's negative (marked stale via {@code used |= IO}),
		 * this method recalculates the count by scanning {@code values} backwards from
		 * the last known potential position to find the highest-indexed non-zero element.
		 * The internal {@code used} field is updated with the accurate count before returning.
		 *
		 * @return The number of {@code long} elements in {@code values} actively storing bits.
		 * Returns 0 if {@code values} is empty or contains only zeros.
		 */
		protected int used() {
			// Check if `used` is positive, indicating a cached and valid value. Return
			// directly if valid.
			if( -1 < used ) return used;
			
			// `used` is negative, recalculation is needed. Clear the sign bit to get the
			// last known count.
			used &= OI;
			
			// Start scanning backwards from the last known used index to find the highest
			// non-zero element.
			int u = used - 1;
			
			// Iterate backwards, skipping zeroed longs to find the actual used length.
			while( -1 < u && values[ u ] == 0 ) u--;
			
			// Update `used` with the new count (index + 1) and return it.
			return used = u + 1;
		}
		
		/**
		 * Ensures the internal state (`size` and `values` array capacity) can accommodate
		 * the specified bit index, expanding if necessary. It also returns the calculated
		 * index within the `values` array for the given bit.
		 * <p>
		 * If {@code bit} is greater than or equal to the current {@link #size()},
		 * {@code size} is updated to {@code bit + 1}.
		 * If the calculated {@code values} index is outside the current bounds of used elements
		 * or the allocated length of {@code values}, the `values` array is resized (typically
		 * grows by 50%) and the `used` count is updated.
		 *
		 * @param bit The global bit position (0-indexed) to ensure accommodation for.
		 * @return The index in the {@code values} array where the bit resides,
		 * or -1 if the bit falls within the implicit {@code trailingOnesCount} range.
		 */
		int used( int bit ) {
			
			if( size() <= bit ) size = bit + 1;
			int index = bit - trailingOnesCount >> LEN;
			if( index < 0 ) return -1; // Within trailing '1's
			if( index < used() ) return index;
			if( values.length < ( used = index + 1 ) ) values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), used ) );
			return index;
		}
		
		/**
		 * Retrieves the value of the bit at the specified global index.
		 *
		 * @param bit The global bit index (0-indexed) to retrieve.
		 * @return {@code true} if the bit at the specified index is '1', {@code false}
		 * if it is '0'. Returns {@code false} if the index is negative or
		 * greater than or equal to {@link #size()}.
		 */
		public boolean get( int bit ) {
			if( bit < 0 || bit >= size ) return false;
			if( bit < trailingOnesCount ) return true;
			int index = bit - trailingOnesCount >> LEN;
			return index < used() && ( values[ index ] & 1L << ( bit - trailingOnesCount & MASK ) ) != 0;
		}
		
		/**
		 * Retrieves the value of the bit at the specified index and returns one of two
		 * provided integer values based on the result.
		 * This is a convenience method equivalent to {@code get(bit) ? TRUE : FALSE}.
		 *
		 * @param bit   The global bit index (0-indexed) to check.
		 * @param FALSE The value to return if the bit at {@code bit} is '0' or out of bounds.
		 * @param TRUE  The value to return if the bit at {@code bit} is '1'.
		 * @return {@code TRUE} if {@code get(bit)} is true, otherwise {@code FALSE}.
		 */
		public int get( int bit, int FALSE, int TRUE ) {
			return get( bit ) ?
					TRUE :
					FALSE;
		}
		
		/**
		 * Copies a range of bits from this {@code BitList} into a destination
		 * {@code long} array, starting at the beginning of the destination array.
		 * <p>
		 * Bits are copied starting from {@code from_bit} (inclusive) up to
		 * {@code to_bit} (exclusive). The destination array {@code dst} is assumed
		 * to be zero-initialized or the caller handles merging. Bits are packed into
		 * {@code dst} starting at index 0, bit 0.
		 *
		 * @param dst      The destination {@code long} array to copy bits into.
		 * @param from_bit The starting global bit index in this {@code BitList} (inclusive).
		 * @param to_bit   The ending global bit index in this {@code BitList} (exclusive).
		 * @return The number of bits actually copied. This may be less than
		 * {@code to_bit - from_bit} if the range exceeds the list's size or
		 * the destination array's capacity. Returns 0 if the range is invalid
		 * or out of bounds.
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
						if( dst_index >= dst.length )
							return copied_bits;
					}
					dst[ dst_index ] |= 1L << dst_bit_offset;
					dst_bit_offset++;
				}
				from_bit += trailing_ones_to_copy;
				bits_to_copy -= trailing_ones_to_copy;
				copied_bits += trailing_ones_to_copy;
				if( bits_to_copy == 0 )
					return copied_bits;
			}
			
			int start_bit_offset_in_values = from_bit - trailingOnesCount & MASK;
			
			for( int i = from_bit - trailingOnesCount >> LEN; i < used() && bits_to_copy > 0; i++ ) {
				long current_value          = values[ i ] >>> start_bit_offset_in_values;
				int  bits_from_current_long = Math.min( BITS - start_bit_offset_in_values, bits_to_copy );
				
				for( int j = 0; j < bits_from_current_long; j++ ) {
					if( dst_bit_offset == BITS ) {
						dst_bit_offset = 0;
						dst_index++;
						if( dst_index >= dst.length )
							return copied_bits;
					}
					if( ( current_value & 1L << j ) != 0 )
						dst[ dst_index ] |= 1L << dst_bit_offset;
					dst_bit_offset++;
				}
				copied_bits += bits_from_current_long;
				bits_to_copy -= bits_from_current_long;
				start_bit_offset_in_values = 0;
			}
			return copied_bits;
		}
		
		
		/**
		 * Finds the index of the first '1' bit occurring at or after a specified index.
		 *
		 * @param bit The starting global bit index (inclusive) to search from.
		 *            If negative, the search starts from index 0.
		 * @return The index of the next '1' bit at or after {@code bit}, or -1 if
		 * no '1' bit exists in that range up to {@code size() - 1}.
		 */
		public int next1( int bit ) {
			if( size() == 0 ) return -1;
			if( bit < 0 ) bit = 0;
			int last1 = last1();
			if( last1 < bit ) return -1;
			if( bit == last1 ) return last1;
			
			if( bit < trailingOnesCount ) return bit;
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			
			long value = values[ index ] & -1L << ( bitOffset & MASK ); // Check for '1's from pos
			
			if( value != 0 ) return trailingOnesCount + ( index << LEN ) + Long.numberOfTrailingZeros( value );
			
			for( int i = index + 1; i < used(); i++ )
				if( ( value = values[ i ] ) != 0 )
					return trailingOnesCount + ( i << LEN ) + Long.numberOfTrailingZeros( value );
			
			return -1;
		}
		
		/**
		 * Finds the index of the first '0' bit occurring at or after a specified index.
		 *
		 * @param bit The starting global bit index (inclusive) to search from.
		 *            If negative, the search starts from index 0.
		 * @return The index of the next '0' bit at or after {@code bit}, or -1 if
		 * no '0' bit exists in that range up to {@code size() - 1}.
		 */
		public int next0( int bit ) {
			if( size() == 0 || size() <= bit ) return -1;
			
			if( bit < 0 ) bit = 0;
			if( last1() < bit ) return bit;
			
			if( bit < trailingOnesCount ) return trailingOnesCount == size ?
					-1 :
					trailingOnesCount;
			
			// Adjust bit position relative to the end of trailing ones
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Which long in values array
			
			long value = ~values[ index ] & -1L << ( bitOffset & MASK );
			
			// Search within the first long
			if( value != 0 ) return trailingOnesCount + ( index << LEN ) + Long.numberOfTrailingZeros( value );
			
			// Search subsequent longs
			for( int i = index + 1; i < used(); i++ )
				if( ( value = ~values[ i ] ) != 0 ) return trailingOnesCount + ( i << LEN ) + Long.numberOfTrailingZeros( value );
			
			// No '0' found, return -1
			return -1;
		}
		
		/**
		 * Finds the index of the last '1' bit occurring at or before a specified index.
		 *
		 * @param bit The ending global bit index (inclusive) to search backwards from.
		 *            If negative, returns -1. If greater than or equal to {@code size()},
		 *            the search starts from {@code size() - 1}.
		 * @return The index of the previous '1' bit at or before {@code bit}, or -1 if
		 * no '1' bit exists in that range down to index 0.
		 */
		public int prev1( int bit ) {
			if( size() == 0 ) return -1;
			
			if( bit < 0 ) return -1; // Nothing before 0
			if( size() <= bit ) bit = size() - 1; // Adjust to last valid bit
			
			if( bit < trailingOnesCount ) return bit; // All bits up to trailingOnesCount-1 are '1'
			
			// Adjust position relative to end of trailing ones
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			
			// If beyond used values, search up to last used bit
			if( used() <= index ) return last1(); // Return last '1' in the list
			
			long value = values[ index ] & mask( ( bitOffset & MASK ) + 1 ); // Check for '1's up to pos
			
			if( value != 0 ) return trailingOnesCount + ( index << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
			
			for( int i = index - 1; -1 < i; i-- )
				if( ( value = values[ i ] ) != 0 ) return trailingOnesCount + ( i << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
			
			if( trailingOnesCount > 0 ) return trailingOnesCount - 1; // Last '1' in trailing ones
			
			return -1;
		}
		
		/**
		 * Finds the index of the last '0' bit occurring at or before a specified index.
		 *
		 * @param bit The ending global bit index (inclusive) to search backwards from.
		 *            If negative, returns -1. If greater than or equal to {@code size()},
		 *            the search starts from {@code size() - 1}.
		 * @return The index of the previous '0' bit at or before {@code bit}, or -1 if
		 * no '0' bit exists in that range down to index 0.
		 */
		public int prev0( int bit ) {
			if( size() == 0 ) return -1;
			
			// Handle invalid or out-of-bounds cases
			if( bit < 0 ) return -1; // Nothing before 0
			if( size() <= bit ) bit = size() - 1; // Adjust to last valid bit
			
			// If within trailing ones (all '1's), no '0' exists before
			if( bit < trailingOnesCount ) return -1; // All bits up to trailingOnesCount-1 are '1'
			
			// Adjust position relative to end of trailing ones
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			
			// If beyond used values, all trailing bits are '0', check up to used
			if( last1() < bit ) return bit; // Bits beyond used are '0'
			
			long value = ~values[ index ] & mask( ( bitOffset & MASK ) + 1 ); // Invert to find '0's up to pos
			
			if( value != 0 ) return trailingOnesCount + ( index << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
			
			// Search previous longs
			for( int i = index - 1; -1 < i; i-- )
				if( ( value = ~values[ i ] ) != 0 )
					return trailingOnesCount + ( i << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
			
			return -1;
		}
		
		/**
		 * Returns the index of the highest-numbered ('leftmost' or most significant)
		 * bit that is set to '1'.
		 *
		 * @return The index of the highest set bit, or -1 if the {@code BitList}
		 * contains no '1' bits (i.e., it's empty or all zeros).
		 */
		public int last1() {
			return used() == 0 ?
					trailingOnesCount - 1 :
					trailingOnesCount + ( used - 1 << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( values[ used - 1 ] );
		}
		
		/**
		 * Checks if this {@code BitList} contains only '0' bits (or is empty).
		 *
		 * @return {@code true} if the list has size 0, or if {@code trailingOnesCount}
		 * is 0 and the {@code values} array contains no set bits;
		 * {@code false} otherwise.
		 */
		public boolean isAllZeros() { return trailingOnesCount == 0 && used == 0; }
		
		/**
		 * Calculates the number of '1' bits from index 0 up to and including the
		 * specified bit index (also known as rank or population count).
		 *
		 * @param bit The global bit index (inclusive) up to which to count set bits.
		 *            If negative, returns 0. If greater than or equal to {@code size()},
		 *            counts up to {@code size() - 1}.
		 * @return The total number of '1' bits in the range [0, bit].
		 */
		public int rank( int bit ) {
			if( bit < 0 || size == 0 ) return 0;
			if( size <= bit ) bit = size - 1;
			if( bit < trailingOnesCount ) return bit + 1;
			if( used() == 0 ) return trailingOnesCount;
			
			int last1 = last1();
			if( last1 < bit ) bit = last1;
			
			// Calculate rank for bits beyond trailing ones.
			int index = bit - trailingOnesCount >> LEN; // Index of the long containing the bit.
			int sum   = trailingOnesCount + Long.bitCount( values[ index ] << BITS - 1 - ( bit - trailingOnesCount ) );
			// Add '1' counts from all preceding longs in the values array.
			for( int i = 0; i < index; i++ )
			     sum += Long.bitCount( values[ i ] );
			
			return sum; // Total count of '1's up to the specified bit.
		}
		
		/**
		 * Returns the total number of bits set to '1' in this {@code BitList}.
		 * This is equivalent to calling {@code rank(size() - 1)}.
		 *
		 * @return The total number of '1' bits (cardinality).
		 */
		public int cardinality() { return rank( size - 1 ); }
		
		/**
		 * Finds the global bit index of the Nth set bit ('1'). If the Nth '1' exists,
		 * {@code rank(result) == cardinality}.
		 *
		 * @param cardinality The rank (1-based count) of the '1' bit to find. For example,
		 *                    {@code cardinality = 1} finds the first '1', {@code cardinality = 2}
		 *                    finds the second '1', etc.
		 * @return The 0-based global index of the bit with the specified cardinality,
		 * or -1 if the cardinality is less than 1 or greater than the total
		 * number of '1's in the list ({@link #cardinality()}).
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
		 * Computes a hash code for this {@code BitList}.
		 * The hash code depends on the size, the number of trailing ones, and the
		 * content of the actively used part of the {@code values} array.
		 *
		 * @return A hash code value for this {@code BitList}.
		 */
		@Override
		public int hashCode() {
			int hash = 197;
			for( int i = used(); -1 < --i; )
			     hash = Array.hash( hash, values[ i ] );
			hash = Array.hash( hash, trailingOnesCount );
			return Array.finalizeHash( hash, size() );
		}
		
		/**
		 * Returns the total potential bit capacity of the underlying storage,
		 * considering the current length of the {@code values} array.
		 * This is {@code trailingOnesCount + values.length * 64}. It represents the
		 * maximum bit index that could theoretically be accessed without needing to
		 * reallocate the {@code values} array, not the logical {@link #size()}.
		 *
		 * @return The current storage capacity in bits.
		 */
		public int length() {
			return trailingOnesCount + ( values.length << LEN );
		}
		
		/**
		 * Creates and returns a deep copy of this {@code R} instance.
		 * The clone will have the same {@code size}, {@code trailingOnesCount},
		 * and a separate copy of the {@code values} array with the same bit content.
		 * The {@code used} count is also copied.
		 *
		 * @return A new {@code R} instance identical to this one.
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
		 * Returns true if the object is a non-null {@code BitList} of the exact
		 * same class with the same size and identical bit sequence.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are identical {@code BitList} instances,
		 * {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		/**
		 * Compares this {@code BitList} to another {@code BitList} (specifically, an
		 * instance of {@code R} or its subclasses) for content equality.
		 *
		 * @param other The {@code BitList} (as an {@code R}) to compare with.
		 * @return {@code true} if both lists have the same {@link #size()},
		 * the same {@code trailingOnesCount}, and the same bit values
		 * in their respective {@code values} arrays up to their {@code used} limits;
		 * {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null || size() != other.size() || trailingOnesCount != other.trailingOnesCount ) return false;
			for( int i = used(); -1 < --i; )
				if( values[ i ] != other.values[ i ] )
					return false;
			return true;
		}
		
		/**
		 * Returns a string representation of this {@code BitList} in JSON format.
		 * The format is an array of 0s and 1s, e.g., {@code [1, 1, 0, 1, 0]}.
		 *
		 * @return A JSON array string representing the bits.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Writes the content of this {@code BitList} to the provided {@code JsonWriter}
		 * as a JSON array of integers (0 or 1).
		 * <p>
		 * Iterates through the logical bits, including implicit trailing ones,
		 * explicit bits from {@code values}, and implicit trailing zeros up to {@code size}.
		 *
		 * @param json The {@code JsonWriter} instance to write the JSON array to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterArray();
			int size = size();
			if( 0 < size ) {
				json.preallocate( ( used() + ( trailingOnesCount >> LEN ) + 1 ) * 68 );
				for( int i = 0; i < trailingOnesCount; i++ )
				     json.value( 1 );
				
				int last1 = last1();
				
				for( int l = 0, k = last1 + 1 - trailingOnesCount; l < used; l++, k -= BITS ) {
					long v = values[ l ];
					
					for( int s = 0, max = Math.min( BITS, k ); s < max; s++ )
					     json.value( ( v & 1L << s ) == 0 ?
							                 0 :
							                 1 );
				}
				
				for( int i = last1; ++i < size; )
				     json.value( 0 );// trailing 0's
				
			}
			json.exitArray();
		}
		
		
		/**
		 * Counts the number of leading '0' bits (zeros at the most significant end,
		 * highest indices) in this {@code BitList}.
		 * Equivalent to {@code size() - 1 - last1()} for non-empty lists.
		 *
		 * @return The number of leading zero bits. Returns {@code size()} if the list
		 * is all zeros or empty. Returns 0 if the highest bit (at {@code size()-1})
		 * is '1'.
		 */
		public int numberOfLeading0() {
			return size == 0 ?
					0 :
					size - 1 - last1();
		}
		
		/**
		 * Counts the number of trailing '0' bits (zeros at the least significant end,
		 * lowest indices) in this {@code BitList}.
		 * This is equivalent to the index of the first '1' bit, or {@code size()} if
		 * the list contains only '0's.
		 *
		 * @return The number of trailing zero bits. Returns {@code size()} if the list
		 * is all zeros or empty. Returns 0 if the first bit (index 0) is '1'.
		 */
		public int numberOfTrailing0() {
			int i = next1( 0 );
			return i == -1 ?
					0 :
					i;
		}
		
		/**
		 * Counts the number of trailing '1' bits (ones at the least significant end,
		 * lowest indices) in this {@code BitList}.
		 * This directly corresponds to the {@code trailingOnesCount} optimization field.
		 *
		 * @return The number of implicitly stored trailing '1' bits. Returns 0 if the
		 * list starts with '0' or is empty.
		 */
		public int numberOfTrailing1() { return trailingOnesCount; }
		
		/**
		 * Counts the number of leading '1' bits (ones at the most significant end,
		 * highest indices) in this {@code BitList}.
		 *
		 * @return The number of leading '1' bits. Returns 0 if the list ends in '0',
		 * is empty, or contains only '0's. Returns {@code size()} if the list
		 * contains only '1's.
		 */
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
	 * A concrete, mutable implementation of {@code BitList} extending the read-only
	 * base {@link R}.
	 * <p>
	 * This class provides methods to set, clear, flip, add, insert, and remove bits,
	 * as well as perform bulk operations and manage the list's size and capacity.
	 * It inherits the optimized storage mechanism from {@code R} (using
	 * {@code trailingOnesCount} and a {@code values} array) and updates this
	 * structure efficiently during modifications.
	 */
	class RW extends R {
		/**
		 * Constructs an empty {@code RW} BitList with an initial capacity hint.
		 * The underlying storage (`values` array) will be allocated to hold at least
		 * `bits`, potentially reducing reallocations if the approximate
		 * final size is known. The logical size remains 0.
		 *
		 * @param bits The initial capacity hint in bits. If non-positive,
		 *             a default initial capacity might be used or allocation
		 *             deferred.
		 */
		public RW( int bits ) { if( 0 < bits ) values = new long[ len4bits( bits ) ]; }
		
		/**
		 * Constructs a new {@code RW} BitList of a specified initial size, with all
		 * bits initialized to a specified default value.
		 *
		 * @param default_value The boolean value to initialize all bits to
		 *                      ({@code true} for '1', {@code false} for '0').
		 * @param size          The initial number of bits in the list. Must be non-negative.
		 * @throws IllegalArgumentException if size is negative.
		 */
		public RW( boolean default_value, int size ) {
			if( 0 < size )
				if( default_value )
					trailingOnesCount = this.size = size; // All bits are 1, stored efficiently as trailing ones
				else
					this.size = size;
		}
		
		
		/**
		 * Sets a sequence of bits starting at a specified index, using values from a
		 * boolean array.
		 * The {@code BitList} size will be increased if necessary to accommodate the
		 * sequence.
		 *
		 * @param index  The starting global bit index (0-indexed, inclusive) to begin setting.
		 *               Must be non-negative.
		 * @param values An array of boolean values. {@code values[i]} determines the
		 *               state of the bit at {@code index + i} ({@code true} for '1',
		 *               {@code false} for '0').
		 * @return This {@code RW} instance for method chaining.
		 * @throws IllegalArgumentException if index is negative.
		 * @throws NullPointerException     if values is null.
		 */
		public RW set( int index, boolean... values ) {
			if( index < 0 ) return this;
			int end = index + values.length;
			if( size < end )
				size = end;
			
			for( int i = 0; i < values.length; i++ )
				if( values[ i ] ) set1( index + i );
				else set0( index + i );
			return this;
		}
		
		/**
		 * Sets the bit at the specified index to the given boolean value.
		 * The {@code BitList} size will be increased if the index is outside the current range.
		 *
		 * @param bit   The global bit index (0-indexed) to set. Must be non-negative.
		 * @param value The boolean value to set the bit to ({@code true} for '1',
		 *              {@code false} for '0').
		 * @return This {@code RW} instance for method chaining.
		 * @throws IllegalArgumentException if bit is negative.
		 */
		public RW set( int bit, boolean value ) {
			if( bit < 0 )
				return this;
			if( size <= bit )
				size = bit + 1;
			return value ?
					set1( bit ) :
					set0( bit );
		}
		
		/**
		 * Sets the bit at the specified index based on an integer value.
		 * The bit is set to '1' if the value is non-zero, and '0' if the value is zero.
		 * The {@code BitList} size will be increased if the index is outside the current range.
		 *
		 * @param bit   The global bit index (0-indexed) to set. Must be non-negative.
		 * @param value The integer value. If {@code value != 0}, sets the bit to '1',
		 *              otherwise sets it to '0'.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IllegalArgumentException if bit is negative.
		 */
		public RW set( int bit, int value ) { return set( bit, value != 0 ); }
		
		/**
		 * Sets the bit at the specified index based on comparing an integer value to a
		 * reference 'TRUE' value.
		 * The bit is set to '1' if {@code value == TRUE}, and '0' otherwise.
		 * The {@code BitList} size will be increased if the index is outside the current range.
		 *
		 * @param bit   The global bit index (0-indexed) to set. Must be non-negative.
		 * @param value The integer value to compare.
		 * @param TRUE  The integer value representing the 'true' state.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set( int bit, int value, int TRUE ) { return set( bit, value == TRUE ); }
		
		/**
		 * Sets all bits within a specified range to a given boolean value.
		 * The range is defined from {@code from_bit} (inclusive) to {@code to_bit} (exclusive).
		 * The {@code BitList} size will be increased if {@code to_bit} is beyond the current size.
		 *
		 * @param from_bit The starting global bit index of the range (inclusive, 0-indexed).
		 *                 Must be non-negative.
		 * @param to_bit   The ending global bit index of the range (exclusive, 0-indexed).
		 *                 Must not be less than {@code from_bit}.
		 * @param value    The boolean value to set all bits in the range to
		 *                 ({@code true} for '1', {@code false} for '0').
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set( int from_bit, int to_bit, boolean value ) {
			if( from_bit >= to_bit || from_bit < 0 ) return this;
			if( size < to_bit ) size = to_bit;
			return value ?
					set1( from_bit, to_bit ) :
					set0( from_bit, to_bit );
		}
		
		/**
		 * Sets the bit at the specified index to '1'.
		 * Handles adjustments to {@code trailingOnesCount} and the {@code values} array,
		 * including potential merging of adjacent '1' sequences and shifting bits if
		 * a '0' within the {@code values} array (conceptually, the first '0' after
		 * trailing ones) is changed to '1'. Expands storage if necessary.
		 * Increases list size if {@code bit >= size}.
		 *
		 * @param bit The global bit index (0-indexed) to set to '1'. Must be non-negative.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set1( int bit ) {
			if( bit < trailingOnesCount ) return this;
			
			if( size <= bit ) size = bit + 1;
			
			if( bit == trailingOnesCount ) {
				
				if( used() == 0 ) {
					trailingOnesCount++;
					return this;
				}
				
				int next1 = next1( bit + 1 ); // Find the index of the next '1' starting from bit + 1.
				
				int last1       = last1(); // Absolute index of the last '1' in the list.
				int valuesLast1 = last1 - trailingOnesCount; // Index of the last '1' relative to the start of 'values'.
				
				
				if( bit + 1 == next1 ) {
					
					// Find the end of this contiguous run of '1's by looking for the next '0'.
					int next0After = next0( next1 ); // Find the first '0' at or after the start of the run.
					
					int span_of_1_end =
							next0After == -1 ?
									last1 + 1 :
									next0After;
					if( last1 < span_of_1_end ) {
						Arrays.fill( values, 0, used, 0 );// Cleanup the values array.
						used = 0;
					}
					else {
						values = shiftRight( values, values, next1 - trailingOnesCount, valuesLast1 + 1, span_of_1_end - trailingOnesCount, true );
						used |= IO;
					}
					
					trailingOnesCount = span_of_1_end;// Update the trailing ones count to the end of the merged span.
					return this;
				}
				
				values = shiftRight( values, values, 0, valuesLast1 + 1, 1, true );
				
				trailingOnesCount++;
				used |= IO;
				return this;
			}
			
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN;
			
			if( values.length < index + 1 ) values = Arrays.copyOf( values, Math.max( values.length * 3 / 2, index + 1 ) );
			
			// Step 6: Update used count
			if( used <= index ) used = index + 1;
			
			
			values[ index ] |= 1L << ( bitOffset & MASK );
			
			return this;
		}
		
		/**
		 * Sets all bits within a specified range to '1'.
		 * The range is {@code [from_bit, to_bit)}. Handles adjustments to
		 * {@code trailingOnesCount} and the {@code values} array, potentially merging
		 * runs of '1's and shifting bits. Expands storage if needed.
		 * Increases list size if {@code to_bit > size}.
		 *
		 * @param from_bit The starting global bit index (inclusive, 0-indexed). Must be non-negative.
		 * @param to_bit   The ending global bit index (exclusive, 0-indexed). Must not be less than {@code from_bit}.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IllegalArgumentException if {@code from_bit} is negative or {@code to_bit < from_bit}.
		 */
		public RW set1( int from_bit, int to_bit ) {
			
			if( from_bit < 0 || to_bit <= from_bit ) return this;
			if( size < to_bit ) size = to_bit;
			if( to_bit <= trailingOnesCount ) return this;
			
			int last1 = last1();
			
			if( from_bit <= trailingOnesCount ) {
				int next_zero = next0( to_bit );
				to_bit = next_zero == -1 ?
						size :
						next_zero; // Corrected line: if no '0' found, extend to size
				
				if( last1 < to_bit ) {
					Arrays.fill( values, 0, used, 0 ); // Clear the values array as all bits become trailing ones
					used              = 0; // No bits remain in values array
					trailingOnesCount = to_bit; // All bits up to to_bit are now '1's
					return this;
				}
				
				if( 0 < used ) values = shiftRight( values, values, 0, last1 - trailingOnesCount + 1, to_bit - trailingOnesCount, true );
				
				trailingOnesCount = to_bit; // Update trailingOnesCount to encompass the new range of '1's
				used |= IO; // Mark used for recalculation due to potential trailing zeros
				return this;
			}
			
			int max = to_bit - trailingOnesCount >> LEN;
			
			if( values.length < max + 1 ) values = Array.copyOf( values, Math.max( values.length * 3 / 2, max + 1 ) );
			if( used < max + 1 ) used = max + 1;
			
			fill( 1, values, from_bit - trailingOnesCount, to_bit - trailingOnesCount );
			
			return this;
		}
		
		/**
		 * Sets the bit at the specified index to '0'.
		 * Handles adjustments to {@code trailingOnesCount} and the {@code values} array.
		 * If a bit within the {@code trailingOnesCount} region is cleared, it splits
		 * the implicit '1's, potentially creating new explicit entries in the {@code values}
		 * array and shifting existing ones. Expands storage if necessary.
		 * Increases list size if {@code bit >= size}.
		 *
		 * @param bit The global bit index (0-indexed) to set to '0'. Must be non-negative.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IllegalArgumentException if bit is negative.
		 */
		public RW set0( int bit ) {
			// Step 1: Validate input
			if( bit < 0 ) return this;
			
			if( size <= bit ) {
				size = bit + 1;
				return this;
			}
			
			int last1 = last1(); // Original last set bit index (e.g., 4)
			
			if( last1 < bit ) {
				size = Math.max( size, bit + 1 );
				return this;
			}
			
			if( bit < trailingOnesCount ) {
				if( bit + 1 == trailingOnesCount && used() == 0 ) {
					trailingOnesCount--;
					return this;
				}
				int bitsInValues = last1 - trailingOnesCount + 1;
				
				int shift = trailingOnesCount - bit;
				
				used = len4bits( shift + bitsInValues );
				
				if( 0 < bitsInValues ) values = shiftLeft( values, 1, bitsInValues, shift, trailingOnesCount == 1 );
				else if( values.length < used ) values = new long[ used ];
				
				
				if( 1 < shift ) fill( 1, values, 1, shift );
				
				trailingOnesCount = bit; // Set the new trailing ones count (e.g., 1)
				used |= IO; // Mark dirty for safety/simplicity
				return this;
			}
			
			
			int bitInValues = bit - trailingOnesCount; // Relative offset in values
			values[ bitInValues >> LEN ] &= ~( 1L << ( bitInValues & MASK ) ); // Clear the bit
			
			if( bit == last1 ) used |= IO; // Mark dirty for recalculation by used()
			
			return this;
		}
		
		/**
		 * Sets all bits within a specified range to '0'.
		 * The range is {@code [from_bit, to_bit)}. Handles adjustments to
		 * {@code trailingOnesCount} and the {@code values} array, potentially splitting
		 * implicit '1's runs and shifting bits. Expands storage if needed.
		 * Increases list size if {@code to_bit > size}.
		 *
		 * @param from_bit The starting global bit index (inclusive, 0-indexed). Must be non-negative.
		 * @param to_bit   The ending global bit index (exclusive, 0-indexed). Must not be less than {@code from_bit}.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set0( int from_bit, int to_bit ) {
			if( from_bit < 0 || to_bit <= from_bit ) return this; // Invalid range
			if( size < to_bit ) size = to_bit;
			
			// Optimization: If the entire range is beyond the last '1', it's already '0's.
			int last1 = last1();
			if( last1 < from_bit ) {
				size = Math.max( size, to_bit );
				return this;
			}
			
			to_bit = Math.min( to_bit, size );
			if( to_bit <= from_bit ) return this; // Range became invalid after clamping
			
			int last1InValue = last1 - trailingOnesCount;
			int bitsInValues = last1InValue < 0 ?
					0 :
					last1InValue + 1;
			
			if( to_bit <= trailingOnesCount ) {
				
				int shift = trailingOnesCount - to_bit; // '1's after the cleared range
				
				trailingOnesCount = from_bit;
				
				used = len4bits( shift + bitsInValues );
				
				if( 0 < bitsInValues ) values = shiftLeft( values, 0, bitsInValues, to_bit - from_bit + shift, true );
				else if( values.length < used ) values = new long[ Math.max( values.length + ( values.length >> 1 ), used ) ];
				
				if( 0 < shift ) fill( 1, values, to_bit - from_bit, to_bit - from_bit + shift );
				
			}
			else if( from_bit < trailingOnesCount ) {
				
				int shift = trailingOnesCount - from_bit;
				
				trailingOnesCount = from_bit;
				used              = len4bits( Math.max( shift + bitsInValues, to_bit - trailingOnesCount ) );
				
				if( 0 < bitsInValues )
					values = shiftLeft( values, 0, bitsInValues, shift, true );
				else if( values.length < used )
					values = new long[ Math.max( values.length + ( values.length >> 1 ), used ) ];
				
				fill( 0, values, 0, to_bit - trailingOnesCount );
			}
			else fill( 0, values, from_bit - trailingOnesCount, to_bit - trailingOnesCount );
			
			used |= IO;
			return this;
		}
		
		/**
		 * Appends a single bit with the specified boolean value to the end of this
		 * {@code BitList}. Equivalent to {@code set(size(), value)}.
		 * Increases the size of the list by one.
		 *
		 * @param value The boolean value of the bit to append ({@code true} for '1',
		 *              {@code false} for '0').
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW add( boolean value ) { return set( size, value ); }
		
		
		/**
		 * Sets or adds a bit at a specific index with a boolean value.
		 * If {@code bit} is within the current size, it sets the bit.
		 * If {@code bit} is beyond the current size, it expands the list (padding
		 * with '0's if necessary) and then sets the bit at that index.
		 * Effectively combines {@code set(bit, value)} with ensuring size.
		 *
		 * @param bit   The index (0-indexed) where the bit should be placed or set. Must be non-negative.
		 * @param value The boolean value to set ({@code true} for '1', {@code false} for '0').
		 * @return This {@code RW} instance for method chaining.
		 * @throws IllegalArgumentException if bit is negative.
		 */
		public RW add( int bit, boolean value ) {
			if( bit < 0 ) return this;
			if( size <= bit ) size = bit + 1;
			return set( bit, value );
		}
		
		/**
		 * Removes the bit at the specified index, shifting all subsequent bits one
		 * position to the left (towards lower indices). Decreases the size by one.
		 * Handles adjustments to {@code trailingOnesCount} and the {@code values} array.
		 *
		 * @param bit The global bit index (0-indexed) to remove. Must be non-negative
		 *            and less than the current size.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IndexOutOfBoundsException if bit is negative or {@code bit >= size}.
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
			if( last1 < bit ) return this;
			
			// Case 3: Removing the last '1' bit, no subsequent bits to shift
			if( bit == last1 ) {
				bit -= trailingOnesCount;
				values[ bit >> LEN ] &= ~( 1L << ( bit & MASK ) ); // Clear the bit
				used |= IO; // Invalidate used to force recalculation (set0 may not update it fully)
				return this;
			}
			
			int last1InValues = last1 - trailingOnesCount;
			
			// Case 4: Removing the first bit after trailingOnesCount (always '0' in values)
			if( bit != trailingOnesCount ) shiftRight( values, values, bit - trailingOnesCount, last1InValues + 1, 1, true );
			else {
				int next0 = next0( bit + 1 ); // Find the next '0' after the bit to detect '1' spans
				
				// Subcase 4a: Next bit is '0', no '1' span to merge
				if( next0 == bit + 1 ) {
					shiftRight( values, values, 1, last1InValues + 1, 1, true );
					return this;
				}
				
				if( next0 == -1 || last1 < next0 ) {
					trailingOnesCount += last1InValues; // Merge all bits in values into trailingOnesCount
					Arrays.fill( values, 0, used, 0 ); // Clear the values array
				}
				else {
					int shift = next0 - bit; // Length of '1' span including the removed position
					trailingOnesCount += shift; // Extend trailingOnesCount by the span length
					shiftRight( values, values, 1, last1InValues + 1, shift, true );
				}
			}
			
			used |= IO;
			return this;
		}
		
		/**
		 * Inserts a '0' bit at the specified index, shifting all existing bits at
		 * and after that index one position to the right (towards higher indices).
		 * Increases the size by one. Handles adjustments to {@code trailingOnesCount}
		 * and the {@code values} array.
		 *
		 * @param bit The global bit index (0-indexed) at which to insert the '0'.
		 *            Must be non-negative. If {@code bit >= size}, acts like appending a '0'.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IllegalArgumentException if bit is negative.
		 */
		public RW insert0( int bit ) {
			if( bit < 0 ) return this;
			
			if( bit < size ) size++;
			else size = bit + 1;
			
			int last1 = last1();
			if( last1 < bit ) return this;
			int last1InValues = last1 - trailingOnesCount;
			
			if( bit < trailingOnesCount ) {
				// Number of '1's to preserve after the insertion point
				int shiftBits = trailingOnesCount - bit;
				
				used              = len4bits( last1 - trailingOnesCount + 1 + shiftBits );
				trailingOnesCount = bit;
				
				if( 0 < last1InValues ) values = shiftLeft( values, 0, last1InValues + 1, shiftBits, true );
				else if( values.length < used ) values = new long[ Math.max( values.length * 3 / 2, used ) ];
				
				fill( 1, values, 1, 1 + shiftBits );
			}
			else // bit <= last1
			{
				used   = len4bits( last1InValues + 1 + 1 ) | IO;
				values = shiftLeft( values, Math.abs( bit - trailingOnesCount ), last1InValues + 1, 1, true );
			}
			
			return this;
		}
		
		/**
		 * Inserts a '1' bit at the specified index, shifting all existing bits at
		 * and after that index one position to the right (towards higher indices).
		 * Increases the size by one. Handles adjustments to {@code trailingOnesCount}
		 * and the {@code values} array, potentially merging with adjacent '1's.
		 *
		 * @param bit The global bit index (0-indexed) at which to insert the '1'.
		 *            Must be non-negative. If {@code bit >= size}, acts like appending a '1'.
		 * @return This {@code RW} instance for method chaining.
		 * @throws IllegalArgumentException if bit is negative.
		 */
		public RW insert1( int bit ) {
			// Case 0: Handle invalid input.
			if( bit < 0 ) return this; // No insertion for negative indices.
			
			if( bit <= trailingOnesCount ) {
				trailingOnesCount++;
				size++;
				return this;
			}
			
			int bitInValues = bit - trailingOnesCount;
			int index       = bitInValues >> LEN;
			int last1       = last1();
			int valuesLast1 = last1 - trailingOnesCount;
			
			
			if( used() == 0 ) { if( values.length < index + 1 ) values = new long[ Math.max( values.length + ( values.length >> 1 ), index + 2 ) ]; }
			else if( bit <= last1 ) values = shiftLeft( values, bit - trailingOnesCount, valuesLast1 + 1, 1, false );
			
			
			used = len4bits( Math.max( bitInValues + 1, valuesLast1 + 1 + 1 ) );
			
			values[ index ] |= 1L << ( bitInValues & MASK ); // Set the bit at the calculated index and position to '1'.
			
			if( size < bit ) size = bit + 1;
			else size++;
			
			return this;
		}
		
		/**
		 * Removes any trailing zero bits from the end of this {@code BitList} by
		 * adjusting the size down to the index of the last '1' bit plus one.
		 * If the list is all zeros or empty, the size becomes 0.
		 *
		 * @return This {@code RW} instance after trimming.
		 */
		public RW trim() {
			length( last1() + 1 );
			return this;
		}
		
		
		/**
		 * Adjusts the capacity of the underlying {@code values} array to be the
		 * minimum size required to hold the current logical bits (up to {@link #size()}).
		 * This can reduce memory usage if the list was previously larger or if
		 * operations caused overallocation. It potentially clears bits between the
		 * new size and the old size if shrinking.
		 *
		 * @return This {@code RW} instance after adjusting capacity.
		 */
		public RW fit() { return length( size() ); }
		
		/**
		 * Sets the logical length (size) of this {@code BitList} to the specified
		 * number of bits.
		 * <p>
		 * If the new length {@code bits} is less than the current {@link #size()}, the list
		 * is truncated. Bits at indices {@code bits} and higher are discarded.
		 * This may involve adjusting {@code trailingOnesCount} and clearing bits
		 * within the {@code values} array. The underlying {@code values} array capacity
		 * is also reduced to match the new requirement.
		 * <p>
		 * If {@code bits} is greater than the current size, the list is conceptually
		 * padded with '0' bits at the end to reach the new length. The underlying
		 * {@code values} array capacity might be increased, but no new '1' bits are set.
		 * <p>
		 * If {@code bits} is non-positive, the list is effectively cleared.
		 *
		 * @param bits The desired new length (size) of the {@code BitList} in bits.
		 * @return This {@code RW} instance after setting the length.
		 */
		public RW length( int bits ) {
			if( bits < 0 ) {
				clear();
				return this;
			}
			
			
			if( bits <= trailingOnesCount ) {
				trailingOnesCount = bits;
				values            = Array.EqualHashOf._longs.O;
				used              = 0;
				size              = bits;
			}
			else if( bits < size ) {
				int last1 = last1();
				
				if( last1 < bits ) {
					size = bits;
					return this;
				}
				
				int len = len4bits( bits - trailingOnesCount );
				if( len < values.length ) values = Array.copyOf( values, len );
				used = Math.min( used(), len ) | IO;
				
				set0( bits, last1 + 1 );//zeroed
				size = bits;
			}
			return this;
		}
		
		/**
		 * Sets the logical size of this {@code BitList}.
		 * If the new size is smaller than the current size, the list is truncated,
		 * discarding bits at indices {@code newSize} and above. This is similar to
		 * {@link #length} but might not shrink the underlying array capacity.
		 * If the new size is larger, the list is expanded, conceptually padding with
		 * '0' bits.
		 *
		 * @param size The desired new size of the {@code BitList}. Must be non-negative.
		 * @return This {@code RW} instance after resizing.
		 */
		public RW size( int size ) {
			if( size < this.size )
				if( size < 1 ) clear();
				else {
					set0( size, size() );
					this.size = size;
				}
			else if( size() < size ) this.size = size;
			return this;
		}
		
		/**
		 * Resets this {@code BitList} to an empty state.
		 * Sets size and trailingOnesCount to 0, clears the {@code values} array
		 * (sets elements to 0), and resets the {@code used} count to 0.
		 * The capacity of the {@code values} array may be retained.
		 *
		 * @return This {@code RW} instance after clearing.
		 */
		public RW clear() {
			java.util.Arrays.fill( values, 0, used(), 0L );
			used              = 0;
			size              = 0;
			trailingOnesCount = 0;
			return this;
		}
		
		/**
		 * Creates and returns a deep copy of this {@code RW} instance.
		 * The clone will have the same size, trailing ones count, and bit values
		 * as the original, with its own independent copy of the underlying data.
		 *
		 * @return A new {@code RW} instance identical to this one.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		
		/**
		 * Extracts a 64-bit word (long) from a {@code long} array, starting at a
		 * specified bit offset within the array's conceptual bitstream.
		 * Handles words that span across two {@code long} elements.
		 *
		 * @param src      The source {@code long} array.
		 * @param bit      The starting bit position (0-based index relative to the start of src).
		 * @param src_bits The total number of valid bits in the conceptual bitstream represented by src (used for boundary checks).
		 * @return The 64-bit word starting at the specified bit position. Bits beyond {@code src_bits} are treated as 0.
		 */
		protected static long get_( long[] src, int bit, int src_bits ) {
			int  index  = bit >>> LEN;
			int  offset = bit & MASK;
			long result = src[ index ] >>> offset;
			if( 0 < offset && bit + BITS - offset < src_bits ) result |= src[ index + 1 ] << BITS - offset;
			return result;
		}
		
		/**
		 * Sets (writes) a 64-bit word (long) into a destination {@code long} array
		 * at a specified bit offset within the array's conceptual bitstream.
		 * Handles words that span across two {@code long} elements. Assumes destination
		 * array is large enough.
		 *
		 * @param src      The 64-bit word to write.
		 * @param dst      The destination {@code long} array.
		 * @param bit      The starting bit position (0-based index relative to start of dst) to write to.
		 * @param dst_bits The total number of valid bits in the destination conceptual bitstream (used for boundary checks/masking).
		 */
		private static void set( long src, long[] dst, int bit, int dst_bits ) {
			int index  = bit >>> LEN;
			int offset = bit & MASK;
			
			if( offset == 0 ) dst[ index ] = src;
			else {
				dst[ index ] = dst[ index ] & mask( offset ) | src << offset;
				int next = index + 1;
				
				if( next < dst.length && next < len4bits( dst_bits ) ) dst[ next ] = dst[ next ] & ~0L << offset | src >>> BITS - offset;
			}
		}
		
		/**
		 * Copies a specified number of bits from a source {@code long} array region
		 * to a destination {@code long} array region. Handles overlapping regions correctly.
		 * Uses {@link #get_(long[], int, int)} and {@link #set(long, long[], int, int)} internally.
		 *
		 * @param src     The source {@code long} array.
		 * @param src_bit The starting bit position in the source (relative index).
		 * @param dst     The destination {@code long} array (can be the same as src).
		 * @param dst_bit The starting bit position in the destination (relative index).
		 * @param bits    The number of bits to copy.
		 */
		private static void bitcpy( long[] src, int src_bit, long[] dst, int dst_bit, int bits ) {
			int src_bits = src_bit + bits;
			int dst_bits = dst_bit + bits;
			
			int last      = bits >>> LEN;
			int last_bits = ( bits - 1 & MASK ) + 1;
			
			if( dst == src && dst_bit < src_bit ) {
				//  <<<
				for( int i = 0; i < last; i++ )
				     set( get_( src, src_bit + ( i << LEN ), src_bits ), dst, dst_bit + ( i << LEN ), dst_bits );
				
				if( last_bits > 0 ) {
					long s = get_( src, src_bit + ( last << LEN ), src_bits );
					long d = get_( dst, dst_bit + ( last << LEN ), dst_bits );
					set( d ^ ( s ^ d ) & mask( last_bits ), dst, dst_bit + ( last << LEN ), dst_bits );
				}
			}
			else {    // >>>
				for( int i = 0; i < last; i++ )
				     set( get_( src, src_bit + bits - ( i + 1 << LEN ), src_bits ), dst, dst_bit + bits - ( i + 1 << LEN ), dst_bits );
				
				if( last_bits > 0 ) {
					long s = get_( src, src_bit, src_bits );
					long d = get_( dst, dst_bit, dst_bits );
					set( d ^ ( s ^ d ) & mask( last_bits ), dst, dst_bit, dst_bits );
				}
			}
			
			dst[ len4bits( dst_bits ) - 1 ] &= mask( ( dst_bits - 1 & MASK ) + 1 );
		}
		
		
		/**
		 * Shifts a range of bits within a {@code long} array to the right (towards
		 * lower bit indices, LSB). Equivalent to {@code >>>} operation on the conceptual bitstream.
		 * Optionally clears the bits vacated at the high end of the range.
		 * <p>
		 * works like right bit-shift >>> on primitives.
		 * .                 MSB               LSB
		 * .                 |                 |
		 * bits in the list [0, 0, 0, 1, 1, 1, 1] Leading 3 zeros and trailing 4 ones
		 * index in the list 6 5 4 3 2 1 0
		 * shift left <<
		 * shift right >>>
		 *
		 * @param src        The source {@code long} array.
		 * @param dst        The destination  {@code long} array. May be the same as src.
		 * @param lo_bit     The starting bit index (inclusive, relative) of the range to shift.
		 * @param hi_bit     The ending bit index (exclusive, relative) of the range to shift.
		 * @param shift_bits The number of positions to shift right (must be positive).
		 * @param clear      If true, the vacated bits at the high end (indices {@code [hi_bit - shift_bits, hi_bit)}) are set to 0.
		 * @return The modified {@code dst} array
		 */
		static long[] shiftRight( long[] src, long[] dst, int lo_bit, int hi_bit, int shift_bits, boolean clear ) {
			if( hi_bit <= lo_bit || shift_bits < 1 ) return src;
			
			if( src != dst && 0 < lo_bit ) System.arraycopy( src, 0, dst, 0, len4bits( lo_bit ) );
			if( shift_bits < hi_bit - lo_bit ) bitcpy( src, lo_bit + shift_bits, dst, lo_bit, hi_bit - lo_bit - shift_bits );
			
			if( clear ) fill( 0, dst, hi_bit - shift_bits, hi_bit );
			return dst;
		}
		
		/**
		 * Shifts a range of bits within a {@code long} array to the left (towards
		 * higher bit indices, MSB). Equivalent to {@code <<} operation on the conceptual bitstream.
		 * Handles potential reallocation if the shift requires expanding the array.
		 * Optionally clears the bits vacated at the low end of the range.
		 * <p>
		 * works like left bit-shift << on primitives.
		 * .                 MSB               LSB
		 * .                 |                 |
		 * bits in the list [0, 0, 0, 1, 1, 1, 1] Leading 3 zeros and trailing 4 ones
		 * index in the list 6 5 4 3 2 1 0
		 * shift left <<
		 * shift right >>>
		 *
		 * @param src        The source {@code long} array.
		 * @param lo_bit     The starting bit index (inclusive, relative) of the range to shift.
		 * @param hi_bit     The ending bit index (exclusive, relative) of the range to shift.
		 * @param shift_bits The number of positions to shift left (must be positive).
		 * @param clear      If true, the vacated bits at the low end (indices {@code [lo_bit, lo_bit + shift_bits)}) are set to 0.
		 * @return The modified {@code src} array, or a new, larger array if reallocation occurred.
		 */
		static long[] shiftLeft( long[] src, int lo_bit, int hi_bit, int shift_bits, boolean clear ) {
			
			if( hi_bit <= lo_bit || shift_bits < 1 ) return src;
			
			
			int    max = len4bits( hi_bit + shift_bits );
			long[] dst = src;
			
			if( src.length < max ) {
				dst = new long[ max * 3 / 2 ];
				if( 0 < lo_bit >> LEN ) System.arraycopy( src, 0, dst, 0, lo_bit >> LEN );
			}
			
			bitcpy( src, lo_bit, dst, lo_bit + shift_bits, hi_bit - lo_bit );
			
			if( clear ) fill( 0, dst, lo_bit, lo_bit + shift_bits );
			
			return dst;
		}
		
		/**
		 * Fills a range of bits within a {@code long} array with a specified value (0, 1, or 2 for toggle/flip).
		 * Operates on the conceptual bitstream represented by the array.
		 *
		 * @param src    The value to fill with: 0 (clear), 1 (set), any other (flip).
		 * @param dst    The destination {@code long} array.
		 * @param lo_bit The starting bit index (inclusive, relative) of the range to fill.
		 * @param hi_bit The ending bit index (exclusive, relative) of the range to fill.
		 */
		private static void fill( int src, long[] dst, int lo_bit, int hi_bit ) {
			
			int lo_index  = lo_bit >> LEN;
			int hi_index  = hi_bit - 1 >> LEN;
			int lo_offset = lo_bit & MASK;
			int hi_offset = hi_bit - 1 & MASK;
			
			if( lo_index == hi_index ) switch( src ) {
				case 0:
					dst[ lo_index ] &= ~( mask( hi_bit - lo_bit ) << lo_offset );
					return;
				case 1:
					dst[ lo_index ] |= mask( hi_bit - lo_bit ) << lo_offset;
					return;
				default:
					dst[ lo_index ] ^= mask( hi_bit - lo_bit ) << lo_offset;
					return;
			}
			
			switch( src ) {
				case 0:
					dst[ lo_index ] &= mask( lo_offset );
					for( int i = lo_index + 1; i < hi_index; i++ ) dst[ i ] = 0L;
					dst[ hi_index ] &= ~mask( hi_offset + 1 );
					return;
				case 1:
					dst[ lo_index ] |= ~mask( lo_offset );
					for( int i = lo_index + 1; i < hi_index; i++ ) dst[ i ] = -1L;
					dst[ hi_index ] |= mask( hi_offset + 1 );
					return;
				default:
					dst[ lo_index ] ^= ~mask( lo_offset );
					for( int i = lo_index + 1; i < hi_index; i++ ) dst[ i ] ^= -1L;
					dst[ hi_index ] ^= mask( hi_offset + 1 );
			}
		}
	}
}