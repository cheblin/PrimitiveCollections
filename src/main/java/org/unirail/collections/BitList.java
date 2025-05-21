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

/// <summary>
/// Represents a dynamic list of bits, optimized for both space (memory footprint)
/// and time (performance) efficiency.
/// <para>
/// <see cref="BitList"/> acts like a highly efficient, growable bit vector or bitset,
/// suitable for managing large sequences of boolean flags or performing complex
/// bitwise operations. It employs internal optimizations, such as tracking
/// sequences of leading '1's implicitly, to minimize memory usage.
/// </para>
/// <para>
/// Implementations provide methods for querying individual bits, finding runs of
/// '0's or '1's, calculating cardinality (rank/population count), and potentially
/// modifying the bit sequence.
/// </para>
/// </summary>

public interface BitList {
	/// <summary>
	/// An abstract base class providing a read-only view and core implementation
	/// details for a <see cref="BitList"/>.
	/// <para>
	/// <b>Bit Indexing and Representation:</b>
	/// .                 MSB                LSB
	/// .                 |                 |
	/// bits in the list [0, 0, 0, 1, 1, 1, 1] Leading 3 zeros and trailing 4 ones
	/// index in the list 6 5 4 3 2 1 0
	/// shift left  ≪
	/// shift right  ≫
	/// </para>
	/// <para>
	/// <b>Storage Optimization:</b>
	/// This class utilizes several optimizations:
	/// <list type="bullet">
	///     <item><b>Trailing Ones (<see cref="trailingOnesCount"/>):</b> A sequence of '1' bits at the
	///     beginning (indices 0 to <see cref="trailingOnesCount"/> - 1) are stored implicitly
	///     by just keeping count, not using space in the <see cref="values"/> array.</item>
	///     <item><b>Explicit Bits (<see cref="values"/>):</b> Bits *after* the implicit trailing ones
	///     are packed into a <see cref="ulong"/>[] array. The first conceptual bit stored in
	///     <see cref="values"/> always corresponds to the first '0' bit after the trailing ones.
	///     The last bit stored in <see cref="values"/> corresponds to the highest-indexed '1'
	///     bit (<see cref="Last1"/>).</item>
	///     <item><b>Trailing Zeros:</b> '0' bits from index <see cref="Last1"/> + 1 up to
	///     <see cref="Count"/> - 1 are also implicit and not stored in <see cref="values"/>.</item>
	///     <item><b>Used Count (<see cref="used"/>):</b> Tracks how many <see cref="ulong"/> elements in the
	///     <see cref="values"/> array actually contain non-zero data, allowing the array to
	///     potentially be larger than strictly needed for current bits.</item>
	/// </list>
	/// This structure provides the foundation for concrete readable and writable
	/// <see cref="BitList"/> implementations.
	/// </para>
	/// </summary>
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
		 * provided primitive values based on the result.
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
			
			// Calculate number of full 64-bit chunks that fit in dst
			int full_longs = Math.min( bits_to_copy >> LEN, dst.length );
			
			for( int i = 0; i < full_longs; i++ )
			     dst[ i ] = get64( from_bit + i * 64 );
			
			int copied_bits    = full_longs * 64;
			int remaining_bits = bits_to_copy - copied_bits;
			if( remaining_bits == 0 ) return copied_bits;
			
			dst[ full_longs ] = get64( from_bit + copied_bits ) & mask( remaining_bits );
			
			return copied_bits + remaining_bits;
		}
		
		
		/**
		 * Finds the index of the next '1' bit in this {@link BitList} after the specified bit index.
		 * <p>
		 * This method searches for the first bit set to '1' starting from the position immediately
		 * following the given {@code bit} index. If no '1' bit is found, or if the input is invalid,
		 * it returns -1.
		 * </p>
		 *
		 * @param bit The starting bit index (exclusive) for the search. A value of -1 starts the search from index 0.
		 * @return The index of the next '1' bit, or -1 if no '1' bit is found or if the list is empty or the input is less than -1.
		 */
		public int next1( int bit ) {
			if( size == 0 || bit++ < -1 || size <= bit ) return -1;
			if( bit < trailingOnesCount ) return bit;
			
			int last1 = last1();
			if( bit == last1 ) return last1;
			if( last1 < bit ) return -1;
			
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >>> LEN;
			
			for( long value = values[ index ] & ~0L << ( bitOffset & MASK ); ; value = values[ ++index ] )
				if( value != 0 )
					return trailingOnesCount + ( index << LEN ) + Long.numberOfTrailingZeros( value );
		}
		
		/**
		 * Finds the index of the next '0' bit in this {@link BitList} after the specified bit index.
		 * <p>
		 * This method searches for the first bit set to '0' starting from the position immediately
		 * following the given {@code bit} index. If no '0' bit is found, or if the input is invalid,
		 * it returns -1.
		 * </p>
		 *
		 * @param bit The starting bit index (exclusive) for the search. A value of -1 starts the search from index 0.
		 * @return The index of the next '0' bit, or -1 if no '0' bit is found or if the list is empty.
		 */
		public int next0( int bit ) {
			if( size == 0 ) return -1;
			
			if( ++bit < trailingOnesCount )
				return trailingOnesCount == size ?
						-1 :
						trailingOnesCount;
			
			if( size <= bit ) return -1;
			
			int last1 = last1();
			
			if( bit == last1 )
				return last1 + 1 < size ?
						bit + 1 :
						-1;
			
			if( last1 < bit )
				return last1 + 1 < size ?
						bit :
						-1;
			
			
			// Adjust bit position relative to the end of trailing ones
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN; // Which long in values array
			
			for( long value = ~values[ index ] & ~0L << ( bitOffset & MASK ); ; value = ~values[ ++index ] )
				if( value != 0 )
					return trailingOnesCount + ( index << LEN ) + Long.numberOfTrailingZeros( value );
		}
		
		/**
		 * Finds the index of the previous '1' bit in this {@link BitList} before the specified bit index.
		 * <p>
		 * This method searches backward for the first bit set to '1' starting from the position
		 * immediately preceding the given {@code bit} index. If no '1' bit is found, or if the input
		 * is invalid, it returns -1.
		 * </p>
		 *
		 * @param bit The starting bit index (exclusive) for the backward search. If -1 or greater than or equal to the list size, the search starts from the last index.
		 * @return The index of the previous '1' bit, or -1 if no '1' bit is found or if the list is empty or the input is less than -1.
		 */
		public int prev1( int bit ) {
			if( size == 0 || bit < -1 ) return -1;
			
			bit = size <= bit || bit == -1 ?
					size - 1 :
					bit - 1;
			
			if( bit < trailingOnesCount ) return bit;
			int last1 = last1();
			if( last1 < bit ) return last1;
			
			int bitOffset = bit - trailingOnesCount;
			int index     = bitOffset >> LEN;
			
			for( long value = values[ index ] & mask( ( bitOffset & MASK ) + 1 ); ; value = values[ --index ] )
				if( value == 0 ) { if( index == 0 ) return trailingOnesCount - 1; }
				else
					return trailingOnesCount + ( index << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
		}
		
		/**
		 * Finds the index of the previous '0' bit in this {@link BitList} before the specified bit index.
		 * <p>
		 * This method searches backward for the first bit set to '0' starting from the position
		 * immediately preceding the given {@code bit} index. If no '0' bit is found, or if the input
		 * is invalid, it returns -1.
		 * </p>
		 *
		 * @param bit The starting bit index (exclusive) for the backward search. If -1 or greater than or equal to the list size, the search starts from the last index.
		 * @return The index of the previous '0' bit, or -1 if no '0' bit is found or if the list is empty or the input is less than -1.
		 */
		public int prev0( int bit ) {
			if( size == 0 || bit < -1 ) return -1;
			
			bit = size <= bit || bit == -1 ?
					size - 1 :
					bit - 1;
			
			if( bit < trailingOnesCount ) return -1;
			
			if( last1() < bit ) return bit;
			
			int bitInValues = bit - trailingOnesCount;
			int index       = bitInValues >> LEN; // Index in values array
			
			for( long value = ~values[ index ] & mask( ( bitInValues & MASK ) + 1 ); ; value = ~values[ --index ] )
				if( value != 0 )
					return trailingOnesCount + ( index << LEN ) + BITS - 1 - Long.numberOfLeadingZeros( value );
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
		public boolean isAllZeros() { return trailingOnesCount == 0 && used() == 0; }
		
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
			if( cardinality() < cardinality ) return -1; // Exceeds total '1's in list
			
			// If within trailing ones, return cardinality - 1 (since all are '1's)
			if( cardinality <= trailingOnesCount ) return cardinality - 1; // 0-based index of the cardinality-th '1'
			
			// Adjust cardinality for bits beyond trailing ones
			int remainingCardinality = cardinality - trailingOnesCount;
			int totalBits            = last1() - trailingOnesCount; // Bits stored in values
			
			// Scan through values array
			for( int i = 0; i < used() && remainingCardinality > 0; i++ ) {
				long value = values[ i ];
				int  bits  = Math.min( BITS, totalBits - ( i << LEN ) ); // Bits in this long
				int  count = Long.bitCount( value & mask( bits ) ); // '1's in this long
				
				// Find the exact bit in this long
				if( remainingCardinality <= count )
					for( int j = 0; j < bits; j++ )
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
		 * as a JSON array of primitives (0 or 1).
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
		 * Counts the number of trailing '0' bits (zeros at the least significant end, lowest indices) in this {@code BitList}.
		 * This is equivalent to the index of the first '1' bit, or {@code size()} if the list contains only '0's.
		 *
		 * @return The number of trailing zero bits. Returns {@code size()} if the list
		 * is all zeros or empty. Returns 0 if the first bit (index 0) is '1'.
		 */
		public int numberOfTrailing0() {
			if( size == 0 || 0 < trailingOnesCount ) return 0;
			int i = next1( -1 );
			return i == -1 ?
					size :
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
						last1 - prev0( last1 ) :
						0;
			}
			return 0;
		}
		
		public long get64( int bit ) {
			if( bit + BITS <= trailingOnesCount ) return -1L;
			if( last1() < bit ) return 0L;
			
			long ret          = 0L;
			int  bits_fetched = 0;
			
			if( bit < trailingOnesCount ) {
				bits_fetched = trailingOnesCount - bit;
				ret          = mask( bits_fetched );
				bit += bits_fetched - trailingOnesCount;
			}
			else bit -= trailingOnesCount;
			
			int index = bit >> LEN;
			if( used() <= index ) return ret;
			
			int pos = bit & MASK;
			
			int  bits_needed = BITS - bits_fetched;
			long bits        = values[ index ] >>> pos;
			
			if( pos != 0 && index + 1 < used ) bits |= values[ index + 1 ] << BITS - pos;
			
			if( bits_needed < BITS ) bits &= mask( bits_needed );
			
			ret |= bits << bits_fetched;
			
			return ret;
		}
		
		/**
		 * Find the index of the first bit where two BitLists differ
		 *
		 * @param other First BitList.
		 * @return The index of the first differing bit, or min(other.size, r2.size) if
		 * they are identical up to the shorter length.
		 */
		public int findFirstDifference( R other ) {
			int checkLimit = Math.min( other.size(), size() );
			int toc1       = other.trailingOnesCount;
			int toc2       = trailingOnesCount;
			int commonTOC  = Math.min( toc1, toc2 );
			
			if( toc1 != toc2 ) return commonTOC; // First difference is where one runs out of 1s
			
			// 2. Check word by word after the common TOC
			int bit = commonTOC;
			while( bit < checkLimit ) {
				long word1 = other.get64( bit );
				long word2 = get64( bit );
				if( word1 != word2 ) {
					int diffOffset = Long.numberOfTrailingZeros( word1 ^ word2 );
					int diffBit    = bit + diffOffset;
					return Math.min( diffBit, checkLimit ); // Return the difference capped by size
				}
				// Avoid overflow on adding BITS
				if( bit > checkLimit - BITS ) bit = checkLimit; // Processed the last partial word
				else bit += BITS;
			}
			
			return checkLimit; // No difference found up to checkLimit
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
		public RW( int bits ) {
			if( bits < 0 ) throw new IllegalArgumentException( "bits cannot be negative" );
			if( 0 < bits ) values = new long[ len4bits( bits ) ];
		}
		
		/**
		 * Constructs a new {@code RW} BitList of a specified initial size, with all
		 * bits initialized to a specified default value.
		 *
		 * @param default_value The boolean value to initialize all bits to
		 *                      ({@code true} for '1', {@code false} for '0').
		 * @param size          The initial number of bits in the list. Must be non-negative.
		 */
		public RW( boolean default_value, int size ) {
			if( 0 < size )
				if( default_value )
					trailingOnesCount = this.size = size; // All bits are 1, stored efficiently as trailing ones
				else
					this.size = size;
		}
		
		/**
		 * Performs a bitwise AND operation between this {@code BitList} and another
		 * read-only {@code BitList}.
		 * The result is stored in this {@code BitList}, modifying it in place.
		 *
		 * @param and The other {@code BitList} to perform AND with.
		 * @return This {@code RW} instance after the AND operation.
		 */
		public RW and( R and ) {
			// --- 1. Handle Trivial Cases ---
			// If either BitList is null or effectively empty (size 0), the result of the
			// AND is empty.
			// Clear this BitList and return.
			if( and == null || and.size() == 0 ) {
				int s = size;
				clear(); // Result of AND with empty set is empty set.
				size = s;
				return this;
			}
			// Optimization: If 'this' BitList is entirely contained within the trailing
			// ones region of 'and',
			// then 'this AND and' is simply 'this'. No modification is needed.
			// Example: this = [1,1], and = [1,1,1,0]. this.size (2) <= and.toc (3). Result
			// is [1,1].
			if( size <= and.trailingOnesCount )
				return this;
			
			// Optimization: If the 'and' BitList has no explicitly stored bits (`and.used()
			// == 0`),
			// it means 'and' consists solely of implicit trailing ones up to
			// `and.trailingOnesCount`,
			// followed by implicit zeros up to `and.size()`.
			// In this case, for the AND operation:
			// - Bits in `this` from index 0 up to `and.trailingOnesCount - 1` are ANDed
			// with '1's, so they remain unchanged.
			// - Bits in `this` from index `and.trailingOnesCount` onwards are ANDed with
			// '0's (implicit zeros of 'and'), so they must become '0'.
			// The `set0(from, to)` method clears bits in the specified range.
			if( and.used() == 0 ) {
				set0( and.trailingOnesCount, size() );// Truncate 'this' to the resulting minimum size.
				return this;
			}
			
			// --- 2. Calculate Result Dimensions ---
			// The result's trailing ones can only exist where *both* operands had trailing
			// ones.
			final int min_toc = Math.min( this.trailingOnesCount, and.trailingOnesCount );
			// The result's size is limited by the shorter of the two operands.
			final int min_size = Math.min( this.size, and.size() );
			
			// --- 3. Optimization: Result is entirely trailing ones ---
			// If the effective size of the result is completely covered by the new trailing
			// ones count,
			// then no explicit bits need to be stored in the 'values' array.
			if( min_size <= min_toc ) {
				// Clear any existing data in the values array.
				if( 0 < used() )
					Arrays.fill( this.values, 0, this.used(), 0L ); // Use used() for safety
				// Update the state of 'this' to reflect the result.
				this.trailingOnesCount = min_size; // TOC is the full size
				this.size              = min_size;
				this.used              = 0; // No explicit bits are used.
				return this;
			}
			// --- 4. Adjust Size and Prepare for Main Logic ---
			// If the final size (min_size) is smaller than the current size of 'this',
			// truncate 'this' to min_size. This effectively sets bits beyond min_size to 0.
			if( min_size < size() )
				size( min_size );
			
			// --- 5. Handle Discrepancies in TrailingOnesCount ---
			// This section attempts to align the 'values' array of 'this' conceptually
			// to start right after the final min_toc, before performing the word-wise AND.
			
			int bit = min_toc; // 'bit' tracks the absolute bit index being processed, starts after common TOC.
			int i   = 0; // 'i' tracks the index within the 'this.values' array (destination).
			
			// Case 5a: 'this' originally had more trailing ones than the final 'min_toc'.
			// The bits in 'this' between `and.trailingOnesCount` (which equals `min_toc`
			// here)
			// and the original `this.trailingOnesCount` were '1's.
			// In the 'and' list, bits in this range are effectively '0' (either explicitly
			// or implicitly).
			// So, `1 AND 0` results in '0'. These bits in 'this' must become '0'.
			if( and.trailingOnesCount < trailingOnesCount ) { // Equivalent to: min_toc < this.trailingOnesCount
				// This sets the bit at 'and.trailingOnesCount' (which is the first bit *after*
				// min_toc) to '0'.
				// Crucially, the set0(bit) method, when called with a bit less than the current
				// trailingOnesCount, restructures the BitList. It reduces
				// `this.trailingOnesCount`
				// to `and.trailingOnesCount` and shifts the content of the `values` array,
				// potentially allocating a new array.
				// This is a potentially very expensive operation involving array
				// copying/shifting.
				set0( and.trailingOnesCount ); // This modifies this.trailingOnesCount, this.values, this.used.
				// After this call, this.trailingOnesCount should now equal min_toc.
				// The loop variables 'i' and 'bit' remain 0 and min_toc, respectively, which is
				// correct
				// for starting the subsequent loop right after the new (reduced) TOC.
			}
			// Case 5b: if 'and' had more trailing ones than 'this' (i.e., min_toc <
			// and.trailingOnesCount).
			// The bits at start of 'this.values' on length `and.trailingOnesCount` -
			// `this.trailingOnesCount` are not changed.
			
			else if( trailingOnesCount < and.trailingOnesCount && ( trailingOnesCount & MASK ) != 0 ) { // Alignment needed
				// if 'this'
				// starts
				// mid-word and
				// 'and' has
				// more TOC.
				
				if( used() == 0 )
					// If no explicit bits in 'this', and 'this' ends before 'and's TOC, the AND
					// operation keeps 'this' as is up to its size. Size already adjusted.
					return this;
				
				// Calculate the difference in trailing ones counts. This is the number of bits
				// at the beginning of `this.values` that correspond to implicit '1's in `and`.
				
				int dif = and.trailingOnesCount - trailingOnesCount; // Number of bits where 'this' is 0 and 'and' is 1.
				
				// Calculate the index and position within this.values corresponding to the end
				// of this range.
				int index = dif >> LEN; // Word index in this.values
				int pos   = dif & MASK; // Bit position within that word
				
				// <111111--- and.trailingOnesCount ----111><and.values[0]>< and.values[1]>...
				// | ^ |
				// pos & |
				// | v |
				// dif | |
				// <- trailingOnesCount -><values[0]>....<values[index]>...>
				// not changed part | | |
				// align till here | end of values[index]
				
				// Mask for lower bits up to 'pos' (exclusive of pos, i.e., bits 0 to pos-1).
				long mask = mask( pos ); // Mask to keep bits that are ANDed with implicit 1s.
				long v    = values[ index ]; // Get the value from 'this.values' at the boundary index.
				
				// Perform the partial AND operation on this boundary word `values[index]`.
				// `v & mask`: Preserves the lower `pos` bits of `v`. These bits are ANDed with
				// '1's from `and`, so they remain unchanged.
				// `v & (and.values[0] << pos)`: Handles the upper `64-pos` bits of `v`.
				// - `and.values[0] << pos`: Takes the first word of `and`'s explicit bits and
				// shifts it left by `pos`.
				// This aligns the beginning of `and.values[0]` with the `pos`-th bit of `v`.
				// - `v & (...)`: Performs the AND operation between the original upper bits of
				// `v` and the aligned lower bits of `and.values[0]`.
				// The result combines the preserved lower bits and the ANDed upper bits.
				
				values[ index ] = v & mask | v & and.values[ 0 ] << pos;
				
				// Update the loop counters to start the main word-wise loop from the *next*
				// word.
				i = index + 1; // Start `this.values` index from the next word.
				// Recalculate the absolute starting bit for the next iteration. It corresponds
				// to the start of `this.values[i]`.
				// The absolute position is the end of the common TOC (`trailingOnesCount` =
				// `min_toc`) plus the offset based on the word index `i`.
				bit = trailingOnesCount + ( i << LEN ); // `trailingOnesCount` here is `min_toc`.
			}
			
			// --- 6. Perform Word-Level AND for Remaining Bits ---
			
			for( ; bit < min_size && i < used(); i++, bit += BITS ) // Loop until the absolute bit index reaches the
				// final size.
				
				// Perform the AND operation for the current 64-bit chunk.
				// `and.get_(bit)`: Fetches 64 bits from the `and` list starting at absolute
				// index `bit`.
				// This correctly handles `and`'s internal structure (TOC + values + implicit
				// zeros).
				// `values[i] &= ...`: ANDs the fetched chunk from `and` with the corresponding
				// word in `this.values`
				// and stores the result back into `this.values[i]`.
				 values[ i ] &= and.get64( bit );
			
			// --- 7. Final State Update ---
			// After the loop, `this.values` holds the result of the AND operation for the
			// explicit bits.
			// The `trailingOnesCount` and `size` fields were already set correctly earlier.
			// The `used` count might now be inaccurate if the AND operation zeroed out
			// higher-order words
			// that were previously in use. Mark `used` as dirty to force recalculation on
			// the next `used()` call.
			this.used |= IO; // Mark used count as potentially invalid (needs recalculation).
			
			return this; // Return this instance for method chaining.
		}
		
		public RW or( R or ) {
			// --- 1. Handle Trivial Cases ---
			if( or == null || or.size() == 0 || or.isAllZeros() )
				return this;
			
			if( isAllZeros() ) { /* copy 'or' state */
				size              = or.size;
				trailingOnesCount = or.trailingOnesCount;
				if( 0 < or.used() )
					if( values == null || values.length < or.used )
						values = or.values.clone();
					else
						System.arraycopy( or.values, 0, values, 0, or.used() );
				
				this.used = or.used;
				return this;
			}
			
			final int max_size = Math.max( this.size, or.size() );// The size of the result is the maximum of the sizes of
			// the two operands.
			// Calculate a *candidate* for the resulting trailingOnesCount (TOC).
			// The result MUST have at least as many trailing ones as the maximum of the
			// input TOCs.
			// This is because if either operand has a '1' at a position < max(toc1, toc2),
			// the result will have a '1'.
			int max_toc = Math.max( trailingOnesCount, or.trailingOnesCount );
			
			// If neither list uses its 'values' array (i.e., they consist entirely of
			// implicit ones),
			// the OR result is also entirely implicit ones, up to the maximum TOC.
			if( used() == 0 && or.used() == 0 ) {
				trailingOnesCount = max_toc;// The resulting TOC is simply the larger of the two original TOCs.
				size              = max_size;// The resulting size is the larger of the two original sizes.
				return this;
			}
			int last1 = last1();
a:
			for( int i = next0( max_toc - 1 ), ii = or.next0( max_toc - 1 ); ; ) {
				while( i < ii ) {
					if( i == -1 ) {
						max_toc = last1 + 1;
						break a;
					}
					
					i = next0( i );
				}
				while( ii < i ) {
					if( ii == -1 ) {
						max_toc = or.last1() + 1;
						break a;
					}
					ii = or.next0( ii );
				}
				
				if( i == ii ) {
					max_toc = i == -1 ?
							Math.max( last1, or.last1() ) + 1 :
							i;
					break;
				}
			}
			
			// --- 3. Handle All-Ones Case ---
			// If the first common zero is at or after the result size, the result is all
			// ones.
			if( max_size <= max_toc ) {
				if( 0 < this.used() ) Arrays.fill( this.values, 0, this.used(), 0L );
				this.trailingOnesCount = max_size; // All ones up to size
				this.size              = max_size;
				this.used              = 0;
				return this;
			}
			
			int bit = trailingOnesCount;// tracks the absolute bit index corresponding to the start of the current word
			// in `this.values`. Initialize with original TOC.
			
			if( trailingOnesCount < max_toc ) // max_toc can only be bigger !
			{
				// If the true `max_toc` is greater than the original `trailingOnesCount` of
				// `this`,
				// it means the result has more leading implicit '1's than 'this' originally
				// did.
				// The existing explicit bits in `this.values` must be shifted right
				// conceptually
				// to make space for these newly gained implicit '1's.
				
				int max = last1 + 1 - trailingOnesCount;
				shiftRight( values, values, 0, max, max_toc - trailingOnesCount, true );
				bit = trailingOnesCount = max_toc;
				used |= IO;
			}
			
			for( int i = 0; bit < max_size && i < used(); i++, bit += BITS )
			     values[ i ] |= or.get64( bit );
			
			this.size = max_size;
			
			return this;
		}
		
		/**
		 * Performs a bitwise XOR operation between this {@code BitList} and another
		 * read-only {@code BitList} (`xor`).
		 * The result is stored in this {@code BitList}, modifying it in place.
		 * The size of the resulting BitList will be the maximum of the sizes of the two
		 * operands.
		 * The trailing ones count and the explicit bit values are updated based on the
		 * XOR operation.
		 * <p>
		 * Formula: this = this XOR xor
		 * <p>
		 * Truth Table for XOR:
		 * A | B | A XOR B
		 * --|---|--------
		 * 0 | 0 | 0
		 * 0 | 1 | 1
		 * 1 | 0 | 1
		 * 1 | 1 | 0
		 *
		 * @param xor The other {@code R} (read-only) BitList to perform the XOR
		 *            operation with.
		 * @return This {@code RW} (read-write) instance after the XOR operation,
		 * allowing for method chaining.
		 */
		public RW xor( R xor ) {
			// --- 1. Handle Trivial Cases ---
			
			// Case 1.1: XOR with null or empty BitList.
			// XORing with nothing or an empty set results in no change to 'this',
			// except potentially needing to match the size if 'xor' was non-null but size
			// 0.
			// However, the size adjustment happens later. If xor is null or size 0, just
			// return.
			if( xor == null || xor.size() == 0 )
				return this;
			
			if( this.isAllZeros() ) {
				
				// Case 1.2: 'this' BitList is currently all zeros.
				// The result of `0 XOR xor` is simply `xor`.
				// Therefore, copy the state (size, trailingOnesCount, values, used) from `xor`
				// into `this`.
				
				// If this is empty, the result is a copy of xor.
				this.size              = xor.size;
				this.trailingOnesCount = xor.trailingOnesCount;
				// Deep copy needed as 'values' might be shared or modified later in 'xor'.
				if( 0 < xor.used() )
					if( values.length < xor.used() )
						values = xor.values.clone();
					else
						System.arraycopy( xor.values, 0, values, 0, xor.used() );
				used = xor.used(); // Use used() to get potentially recalculated value
				return this;
			}
			
			// Case 1.3: The 'xor' BitList is all zeros.
			// The result of `this XOR 0` is `this`.
			// No change to bits is needed. Only the size might need adjustment if
			// `xor.size` was larger.
			if( xor.isAllZeros() ) {
				// XOR with all zeros changes nothing except possibly size.
				if( this.size < xor.size )
					this.size = xor.size;
				return this;
			}
			
			// --- 2. Calculate Result Dimensions & Check for All-Zeros Result ---
			final int max_size = Math.max( this.size, xor.size() );// The final size of the BitList after XOR is the
			// maximum of the two operand sizes.
			
			int first_1 = findFirstDifference( xor );// find first 1 in result
			
			// If the first difference occurs at or after the maximum size required for the
			// result,
			// it means 'this' and 'xor' are identical up to `max_size`.
			// Since `A XOR A = 0`, the result of the XOR operation up to `max_size` is all
			// zeros.
			if( max_size <= first_1 ) {
				clear(); // Reset 'this' to an empty state (size=0, toc=0, used=0, values=O).
				// Set the size of the all-zero result correctly.
				this.size = max_size;
				return this; // Result is all zeros.
			}
			
			
			// --- Calculate the new TrailingOnesCount (new_toc) for the result ---
			
			int new_toc = 0;
			
			
			if( first_1 == 0 )
				for( long x = -1; x == -1; ) {
					x = xor.get64( new_toc ) ^ get64( new_toc );
					if( x == -1 ) new_toc += 64;
					else
						new_toc += Long.numberOfTrailingZeros( ~x );
					
					if( max_size <= new_toc ) {
						trailingOnesCount = new_toc;
						used              = 0;
						size              = max_size;
						return this;
					}
				}
			
			int last1 = last1();
			
			// Edge case: If all original '1's fall within the new TOC, the explicit part is
			// empty.
			if( last1 < new_toc ) {
				if( 0 < used() )
					Arrays.fill( this.values, 0, used(), 0 );// Clear the values array as it should contain only zeros.
				this.trailingOnesCount = new_toc;
				this.size              = max_size;
				this.used              = 0;
				return this;
			}
			
			int max_last1 = Math.max( last1, xor.last1() );// Highest '1' in either operand.
			
			// Calculate the number of longs needed for the result's explicit bits (`values`
			// array).
			// The explicit bits start conceptually *after* `new_toc`.
			// The highest bit index relative to the start of `values` would be `max_last1 -
			// new_toc`.
			// We need `len4bits` based on the *count* of bits, which is `(max_last1 -
			// new_toc) + 1`.
			// Handle the case where `new_toc` might be greater than `max_last1`.
			int new_values_len = len4bits( new_toc <= max_last1 ?
					                               max_last1 - new_toc + 1 :
					                               0 );
			
			// Initialize loop variables for iterating through words.
			int i = 0; // Start writing to index 0 of the result's `values` array.
			
			// --- Handle structural changes due to TOC differences ---
			
			// Case 4.1: The new TOC (`new_toc`) is shorter than `this`'s original TOC.
			// This implies that bits in `this` from `new_toc` up to `this.trailingOnesCount
			// - 1` were originally '1'.
			// For the result's TOC to end at `new_toc`, the `xor` operand must *also* have
			// had '1's in this same range.
			// Therefore, in this range, the operation is `1 XOR 1 = 0`.
			// Additionally, the explicit bits originally stored in `this.values` need to be
			// conceptually shifted left
			// relative to the new, shorter TOC.
			
			if( new_toc < trailingOnesCount ) {
				// Implication: Result has fewer initial implicit '1's than 'this' had.
				// Bits in 'this' from `new_toc` to `original_this_toc - 1` were originally '1'.
				// For the result's TOC to stop at `new_toc`, `xor` must also have had '1's in
				// this range (1 XOR 1 = 0).
				// Action:
				// 1. Shift the explicit bits originally in `this.values` conceptually *left*
				// relative to the new, shorter TOC.
				// 2. Fill the vacated space (corresponding to `new_toc` to `original_this_toc -
				// 1`) with the XOR result (which is `1 XOR xor = ~xor`).
				
				// Calculate the shift_bits distance required for the existing `values` data.
				int shift_bits = trailingOnesCount - new_toc;// Calculate the amount by which `this.values` needs to be
				// shifted left.
				
				// Call shiftLeft helper. Source is `this.values`, destination `dst` is either
				// `this.values` (if resized) or a new array.
				// The range `from_bit` to `to_bit` covers the original explicit bits (`0` to
				// `original_this_last1 - original_this_toc + 1`).
				// The `shift_bits` parameter moves these bits left relative to the start of the
				// array.
				// `clear=false`: The vacated bits on the right (low indices) are *not* cleared
				// by `shiftLeft` itself,
				// because they will be explicitly filled by the subsequent loop using
				// `~xor.get_()`.
				int sb = len4bits( shift_bits );
				values = 0 < used() ?
						shiftLeft( values, 0, last1 - trailingOnesCount + 1, shift_bits, false ) :
						values.length < sb ?
								new long[ sb ] :
								values;
				
				
				int index = shift_bits >>> LEN;
				
				// Fill the space created by the shift_bits (from relative index 0 up to `shift_bits`)
				// with the XOR result for the absolute range `new_toc` to `original_this_toc -
				// 1`.
				// In this range, `this` was '1' and `xor` was also '1', so `this XOR xor` is
				// `0`.
				// The loop iterates using `i` over the destination `values` indices (0 to
				// shift_bits/64)
				// and `bit` over absolute bit indices (`new_toc` to `original_toc - 1`).
				for( int bit = new_toc; i < index; i++, bit += BITS )
					// Fetch 64 bits from `xor` starting at `bit`. We expect these to be all '1's.
					// Invert them (`~`) to get all '0's. Store these '0's in `values[i]`.
					// This correctly sets the result bits in this segment to 0.
					// Note: If xor.get_() wasn't all 1s, new_toc would have been different.
					 values[ i ] = ~xor.get64( bit );
				
				// Handle the boundary word at `index` (if `shift_bits` wasn't a multiple of 64).
				int pos = shift_bits & MASK; // Position within the boundary word.
				if( pos != 0 ) { //process on edge bits
					long mask_lower = mask( pos ); // Mask for lower `pos` bits (range [new_toc, original_toc)).
					long val        = values[ i ]; // Value already shifted into upper bits of index i.
					long xor_val    = xor.get64( new_toc + ( i << LEN ) ); // Corresponding word from xor.
					
					// Calculate result for the boundary word:
					// Lower `pos` bits: Calculate `1 XOR xor_val` which equals `NOT xor_val`.
					// Upper `64-pos` bits: XOR the shifted original bits with corresponding xor bits.
					values[ i ] =
							( val ^ xor_val ) & ~mask_lower | // preserve Upper bits
							~xor_val & mask_lower;   // Lower bits result = NOT xor_val (since original this was 1)
				}
				trailingOnesCount = new_toc;
				this.size         = max_size; // Set final size
				
				used += sb;
				used |= IO;// need totally recount
				
				return this;
			}
			
			
			if( trailingOnesCount < new_toc ) {// Case 4.2: The new TOC (`new_toc`) is longer than `this`'s
				
				// Calculate the shift_bits distance (number of bits to shift_bits right conceptually).
				int shift_bits = new_toc - trailingOnesCount;
				// Shift original explicit bits right by shift_dist.
				// The source range covers the original explicit bits.
				// Destination starts at relative index 0.
				// `clear=true` ensures vacated higher-index bits (left side) are zeroed.
				shiftRight( values, values, 0, last1 - trailingOnesCount + 1, shift_bits, true );
			}
			else if( values.length < new_values_len ) // Case 4.3: `trailingOnesCount == new_toc`. No structural shift  needed.
				values = Arrays.copyOf( values, new_values_len );// Only resize the `values` array if needed.
			
			int bit = new_toc; // Start processing from the absolute bit index where the new_toc ends.
			
			trailingOnesCount = new_toc;// Update the trailingOnesCount of 'this'.
			
			// --- 5. Perform Word-Level XOR for Remaining Bits ---
			// Iterate through the longs required for the result's explicit part (`new_values_len`).
			// `i` tracks the index in `this.values` (starting potentially non-zero if Case 4.1 occurred).
			// `bit` tracks the corresponding absolute bit index.
			for( int max = len4bits( max_size ); i < max; i++, bit += BITS )
			     this.values[ i ] ^= xor.get64( bit );
			
			
			this.size = max_size; // Set final size
			this.used |= IO;// need totally recount
			
			return this;
		}
		
		/**
		 * Performs a bitwise AND NOT operation:
		 * {@code thisBitList AND NOT otherBitList}.
		 * Clears bits in this {@code BitList} where the corresponding bit in the
		 * {@code not} {@code BitList} is set.
		 *
		 * @param not The {@code BitList} to perform NOT and AND with.
		 * @return This {@code RW} instance after the AND NOT operation.
		 */
		public RW andNot( R not ) {
			// --- 1. Handle Trivial Cases ---
			if( not == null || not.isAllZeros() || this.size == 0 )
				return this; // ANDNOT with empty/all-zero set changes nothing in 'this'.
			// If 'this' is empty, result is empty.
			if( this.isAllZeros() ) return this;
			
			// --- 2. Calculate Result Dimensions ---
			// Resulting trailing ones exist where 'this' has '1' and 'not' has '0'.
			// This means they survive up to the point where 'not' has its first '1'.
			int first_1_in_not = not.next1( -1 );
			if( first_1_in_not == -1 )
				first_1_in_not = not.size(); // Treat all-zeros 'not' as having '1' beyond its size
			final int res_toc = Math.min( this.trailingOnesCount, first_1_in_not );
			// The logical size is determined by 'this'. Bits in 'this' beyond 'not.size()'
			// are ANDed with ~0 (i.e., 1), so they survive.
			final int res_size = this.size; // Keep original size
			
			// --- 3. Optimization: Result is entirely trailing ones ---
			// This happens if res_toc covers the whole res_size.
			if( res_size <= res_toc ) {
				if( this.used() > 0 ) Arrays.fill( this.values, 0, this.used(), 0L );
				this.trailingOnesCount = res_size;
				this.size              = res_size;
				this.used              = 0;
				
				return this;
			}
			
			// --- 4. Calculate Dimensions for the Result's 'values' Part ---
			final int bits_in_result_values = res_size - res_toc;
			if( bits_in_result_values <= 0 ) { // Safety check
				if( this.used() > 0 )
					Arrays.fill( this.values, 0, this.used(), 0L );
				this.trailingOnesCount = res_toc; // Use res_toc, not res_size here
				this.size              = res_size;
				this.used              = 0;
				
				return this;
			}
			final int result_values_len = len4bits( bits_in_result_values );
			
			// --- 5. Determine Destination Array: Reuse 'this.values' or Allocate New ---
			final long[]  result_values;
			final boolean in_place;
			int           original_used_cached = -1; // Cache original used count if in_place
			
			// In-place is possible if res_toc matches this.toc AND capacity is sufficient.
			if( this.trailingOnesCount == res_toc && this.values.length >= result_values_len ) {
				original_used_cached = this.used(); // Cache before potential modification
				result_values        = this.values;
				in_place             = true;
			}
			else {
				result_values = new long[ result_values_len ];
				in_place      = false;
			}
			
			// --- 6. Perform Word-Level AND NOT Operation using get_() ---
			for( int i = 0; i < result_values_len; i++ ) {
				int  current_abs_bit_start = res_toc + ( i << LEN );
				long this_word             = this.get64( current_abs_bit_start );
				// Optimization: if this_word is 0, the result is 0, skip fetching not_word
				if( this_word == 0L ) {
					result_values[ i ] = 0L;
					continue;
				}
				long not_word = not.get64( current_abs_bit_start );
				
				result_values[ i ] = this_word & ~not_word;
			}
			
			// --- 7. Clean Up If Modified In-Place ---
			// If the result uses fewer longs than originally, clear the trailing ones.
			if( in_place )
				if( result_values_len < original_used_cached )
					Arrays.fill( this.values, result_values_len, original_used_cached, 0L );
			
			// --- 8. Update the State of 'this' BitList ---
			this.trailingOnesCount = res_toc;
			this.size              = res_size;
			this.values            = result_values;
			this.used              = result_values_len | IO; // Mark used dirty
			
			return this;
		}
		
		/**
		 * Checks if this {@code BitList} intersects with another {@code BitList} (i.e.,
		 * if there is at least one bit position where both are '1').
		 *
		 * @param other The other {@code BitList} to check for intersection.
		 * @return {@code true} if there is an intersection, {@code false} otherwise.
		 */
		public boolean intersects( R other ) {
			// Trivial cases: cannot intersect with null or if either list is empty
			if( other == null || this.size == 0 || other.size == 0 )
				return false;
			
			// Determine the maximum bit index to check (up to the end of the shorter list)
			int checkLimit = Math.min( this.size, other.size() );
			
			// --- Fast structural checks first ---
			
			// 1. Check overlap in the shared trailing ones region: [0, min(this.toc,
			// other.toc))
			// If both lists have trailing ones, they intersect immediately in this common
			// range.
			int commonTOC = Math.min( this.trailingOnesCount, other.trailingOnesCount );
			if( commonTOC > 0 )
				return true; // Intersection guaranteed in the range [0, commonTOC)
			// At this point, we know that at least one of the lists has trailingOnesCount =
			// 0,
			// otherwise, commonTOC would be > 0 and we would have returned true.
			
			// 2. Check overlap between this.trailingOnes and other.values
			// This checks the absolute bit range [commonTOC, min(checkLimit, this.toc))
			// If this list has trailing ones extending beyond the common range...
			// Start checking from end of common TOC (or 0 if none)
			int range1End = Math.min( checkLimit, this.trailingOnesCount ); // End at limit or end of this's TOC
			if( commonTOC < range1End ) {
				// ...we need to see if 'other' has any '1's stored in its 'values' array
				// within this absolute bit range.
				// We can efficiently check this by finding the next '1' in 'other' starting
				// from range1Start.
				int next1InOther = other.next1( commonTOC - 1 );
				// If a '1' exists in 'other' within this range where 'this' has implicit
				// '1's...
				if( next1InOther != -1 && next1InOther < range1End )
					return true; // ...then they intersect.
			}
			
			// 3. Check overlap between other.trailingOnes and this.values (Symmetric to
			// check 2)
			// This checks the absolute bit range [commonTOC, min(checkLimit, other.toc))
			// If the other list has trailing ones extending beyond the common range...
			int range2End = Math.min( checkLimit, other.trailingOnesCount );
			if( commonTOC < range2End ) {
				// ...we need to see if 'this' has any '1's stored in its 'values' array
				// within this absolute bit range.
				int next1InThis = this.next1( commonTOC - 1 );
				// If a '1' exists in 'this' within this range where 'other' has implicit
				// '1's...
				if( next1InThis != -1 && next1InThis < range2End )
					return true; // ...then they intersect.
			}
			
			// --- Check the region where both *might* have explicit bits in 'values' ---
			// This region starts where the *longer* trailingOnesCount ends (or from bit 0
			// if both TOC=0).
			int valuesCheckStart = Math.max( this.trailingOnesCount, other.trailingOnesCount );
			
			// We only need to check from valuesCheckStart up to checkLimit.
			// Use word-level checks for efficiency.
			
			// Calculate the starting bit offset relative to the beginning of each 'values'
			// array.
			// Ensure the offset is non-negative.
			int thisRelBitStart  = Math.max( 0, valuesCheckStart - this.trailingOnesCount );
			int otherRelBitStart = Math.max( 0, valuesCheckStart - other.trailingOnesCount );
			
			// Calculate the starting word index for each 'values' array.
			int thisWordStartIndex  = thisRelBitStart >> LEN;
			int otherWordStartIndex = otherRelBitStart >> LEN;
			
			// Calculate the ending bit index (inclusive) to check relative to each 'values'
			// array start.
			int endBitInclusive = checkLimit - 1;
			int thisRelBitEnd   = endBitInclusive - this.trailingOnesCount;
			int otherRelBitEnd  = endBitInclusive - other.trailingOnesCount;
			
			// Determine the highest word index we need to potentially check in each
			// 'values' array.
			// This depends on the relative end bit and the actual used words.
			int thisUsed  = this.used(); // Cache used() result
			int otherUsed = other.used();
			int thisWordEndIndex = thisRelBitEnd < 0 ?
					-1 :
					Math.min( thisUsed - 1, thisRelBitEnd >> LEN );
			int otherWordEndIndex = otherRelBitEnd < 0 ?
					-1 :
					Math.min( otherUsed - 1, otherRelBitEnd >> LEN );
			
			// The loop needs to cover all word indices relevant to *both* lists within the
			// check range.
			int loopStartIndex = Math.max( thisWordStartIndex, otherWordStartIndex );
			int loopEndIndex   = Math.max( thisWordEndIndex, otherWordEndIndex ); // Iterate up to the highest relevant
			// index
			
			// Iterate through the relevant words where both lists might have explicit bits.
			for( int wordIndex = loopStartIndex; wordIndex <= loopEndIndex; wordIndex++ ) {
				
				// Get the word value from 'this', default to 0 if outside its relevant range or
				// used words.
				long thisWord = wordIndex >= thisWordStartIndex && wordIndex <= thisWordEndIndex
						?
						this.values[ wordIndex ]
						:
						0L;
				
				// Get the word value from 'other', default to 0 if outside its relevant range
				// or used words.
				long otherWord = wordIndex >= otherWordStartIndex && wordIndex <= otherWordEndIndex
						?
						other.values[ wordIndex ]
						:
						0L;
				
				// If both words are 0, they can't intersect in this word.
				if( thisWord == 0L && otherWord == 0L )
					continue;
				
				// Create masks to isolate the bits *within this word* that fall into the
				// absolute check range [valuesCheckStart, checkLimit).
				
				long commonMask = -1L; // Start with all bits relevant
				
				// Mask off bits *below* valuesCheckStart if this word overlaps the start
				// boundary.
				int wordAbsStartBit = this.trailingOnesCount + ( wordIndex << LEN ); // Approx absolute start
				// Determine which word starts first (relative to absolute bits)
				// other.values starts earlier
				if( wordAbsStartBit < valuesCheckStart )
					if( this.trailingOnesCount <= other.trailingOnesCount ) { // this.values starts earlier or same
						if( wordIndex == thisWordStartIndex )
							commonMask &= -1L << ( thisRelBitStart & MASK );
					}
					else if( wordIndex == otherWordStartIndex )
						commonMask &= -1L << ( otherRelBitStart & MASK );
				
				// Mask off bits *at or above* checkLimit if this word overlaps the end
				// boundary.
				int wordAbsEndBit = wordAbsStartBit + BITS; // Approx absolute end (exclusive)
				// Determine which word ends later (relative to absolute bits)
				// other.values ends later
				if( wordAbsEndBit > checkLimit )
					if( this.trailingOnesCount >= other.trailingOnesCount ) { // this.values ends later or same
						if( wordIndex == thisWordEndIndex )
							commonMask &= mask( ( thisRelBitEnd & MASK ) + 1 );
					}
					else if( wordIndex == otherWordEndIndex )
						commonMask &= mask( ( otherRelBitEnd & MASK ) + 1 );
				
				// Check for intersection within the masked portion of the words.
				if( ( thisWord & otherWord & commonMask ) != 0 )
					return true; // Intersection found in the 'values' arrays.
			}
			
			// No intersection found after all checks
			return false;
		}
		
		/**
		 * Flips the bit at the specified position. If the bit is '0', it becomes '1',
		 * and vice versa.
		 *
		 * @param bit The bit position to flip (0-indexed).
		 * @return This {@code RW} instance after flipping the bit.
		 */
		public RW flip( int bit ) {
			if( bit < 0 ) return this;
			return get( bit ) ?
					set0( bit ) :
					set1( bit );
		}
		
		
		/**
		 * Flips a range of bits from {@code from_bit} (inclusive) to {@code to_bit}
		 * (exclusive).
		 * For each bit in the range, if it's '0', it becomes '1', and if it's '1', it
		 * becomes '0'.
		 * This implementation performs the flip in-place, efficiently handling
		 * interactions with {@code trailingOnesCount} and the explicit bits stored
		 * in the {@code values} array without creating temporary BitList objects.
		 *
		 * @param from_bit The starting bit position of the range to flip (inclusive,
		 *                 0-indexed).
		 * @param to_bit   The ending bit position of the range to flip (exclusive,
		 *                 0-indexed).
		 * @return This {@code RW} instance after flipping the bits in the specified
		 * range.
		 */
		public RW flip( int from_bit, int to_bit ) {
			
			// 1. Validate and normalize inputs
			if( from_bit < 0 ) from_bit = 0;
			if( to_bit <= from_bit ) return this; // Empty or invalid range, no change needed
			
			int last1 = last1();
			
			if( from_bit == trailingOnesCount ) {
				if( used() == 0 ) {
					trailingOnesCount = to_bit;
					size              = Math.max( size, to_bit );
					return this;
				}
				
				fill( 3, values, 0, Math.min( last1 + 1, to_bit ) - trailingOnesCount );//flip bits in the values
				int shift_bits = 0;
				int i          = 0;
				for( long v; i < used; )
					if( ( v = values[ i ] ) == -1 ) shift_bits += 64;
					else {
						shift_bits += Long.numberOfTrailingZeros( ~v );
						break;
					}
				
				if( shift_bits == last1 + 1 - trailingOnesCount ) {
					trailingOnesCount += shift_bits + ( ( size = Math.max( size, to_bit ) ) - ( last1 + 1 ) );
					Arrays.fill( values, 0, used, 0 );
					used = 0;
					return this;
				}
				
				int len = len4bits( ( last1 - trailingOnesCount ) - shift_bits + ( ( size = Math.max( size, to_bit ) ) - last1 ) );
				
				values = shiftRight( values,
				                     values.length < len ?
						                     new long[ len ] :
						                     values, 0, last1 - trailingOnesCount + 1, shift_bits, true );
				
				trailingOnesCount += shift_bits;
				used = len | IO;
				return this;
			}
			
			
			if( from_bit < trailingOnesCount ) {
				
				int shift_bits = trailingOnesCount - from_bit;
				int zeros      = Math.min( trailingOnesCount, to_bit ) - from_bit;// len of flipped to 0's  trailing Ones
				
				if( 0 < used() ) {
					int len = len4bits( Math.max( last1 - trailingOnesCount + shift_bits, to_bit - trailingOnesCount ) );
					
					values = shiftLeft( values, 0, last1 - trailingOnesCount + 1 + shift_bits, shift_bits, false );
					used   = len;
					
					fill( 0, values, 0, zeros );
				}
				else if( values.length < ( used = len4bits( Math.max( shift_bits, to_bit - trailingOnesCount ) ) ) )
					values = new long[ used * 3 / 2 ];
				
				
				fill( 1, values, zeros, zeros + trailingOnesCount - from_bit );
				trailingOnesCount = from_bit;
				size              = Math.max( size, to_bit );
				used |= IO;
				return this;
			}
			else if( last1 < to_bit ) {
				int u = Math.max( used(), len4bits( to_bit - trailingOnesCount ) );
				
				if( values.length < u )
					values = used == 0 ?
							new long[ u ] :
							Arrays.copyOf( values, u );
				used = u;
				
				if( last1 < from_bit ) {
					fill( 1, values, from_bit - trailingOnesCount, to_bit - trailingOnesCount );
					size = Math.max( size, to_bit );
					return this;
				}
				
				fill( 1, values, last1 + 1 - trailingOnesCount, to_bit + 1 - trailingOnesCount );
				to_bit = last1;
			}
			
			size = Math.max( size, to_bit );
			
			int pos   = from_bit - trailingOnesCount & MASK;
			int index = from_bit - trailingOnesCount >>> LEN;
			if( 0 < pos ) {
				long mask = mask( pos );
				long v    = values[ index ];
				values[ index ] = v & mask | ~v & ~mask;
				index++;
			}
			
			int index2 = to_bit - trailingOnesCount >>> LEN;
			while( index < index2 ) values[ index ] = ~values[ index++ ];
			
			int pos2 = to_bit - trailingOnesCount >>> LEN;
			if( 0 < pos2 ) {
				long mask = mask( pos2 );
				long v    = values[ index2 ];
				values[ index2 ] = ~( v & mask ) | v & ~mask;
			}
			
			
			return this; // Return instance for method chaining
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
		 * Sets the bit at the specified index based on a primitive value.
		 * The bit is set to '1' if the value is non-zero, and '0' if the value is zero.
		 * The {@code BitList} size will be increased if the index is outside the current range.
		 *
		 * @param bit   The global bit index (0-indexed) to set. Must be non-negative.
		 * @param value The primitive value. If {@code value != 0}, sets the bit to '1',
		 *              otherwise sets it to '0'.
		 * @return This {@code RW} instance for method chaining.
		 */
		public RW set( int bit, int value ) { return set( bit, value != 0 ); }
		
		/**
		 * Sets the bit at the specified index based on comparing a primitive value to a
		 * reference 'TRUE' value.
		 * The bit is set to '1' if {@code value == TRUE}, and '0' otherwise.
		 * The {@code BitList} size will be increased if the index is outside the current range.
		 *
		 * @param bit   The global bit index (0-indexed) to set. Must be non-negative.
		 * @param value The primitive value to compare.
		 * @param TRUE  The primitive value representing the 'true' state.
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
		 * Sets the bit at the specified bit to '1'.
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
				
				int next1       = next1( bit ); // Find the index of the next '1' starting from bit + 1.
				int last1       = last1(); // Absolute index of the last '1' in the list.
				int valuesLast1 = last1 - trailingOnesCount; // Index of the last '1' relative to the start of 'values'.
				
				
				if( bit + 1 == next1 ) {
					
					// Find the end of this contiguous run of '1's by looking for the next '0'.
					int next0After = next0( next1 - 1 ); // Find the first '0' at or after the start of the run.
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
		 */
		public RW set1( int from_bit, int to_bit ) {
			
			if( from_bit < 0 || to_bit <= from_bit ) return this;
			if( size < to_bit ) size = to_bit;
			if( to_bit <= trailingOnesCount ) return this;
			
			int last1 = last1();
			
			if( from_bit <= trailingOnesCount ) {
				int next_zero = next0( to_bit - 1 );
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
				
				if( 0 < bitsInValues ) values = shiftLeft( values, 0, bitsInValues, shift, trailingOnesCount == 1 );
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
		 * Removes the bit at the specified index, shifting all subsequent bits one
		 * position to the left (towards lower indices). Decreases the size by one.
		 * Handles adjustments to {@code trailingOnesCount} and the {@code values} array.
		 *
		 * @param bit The global bit index (0-indexed) to remove. Must be non-negative
		 *            and less than the current size.
		 * @return This {@code RW} instance for method chaining.
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
				int next0 = next0( bit ); // Find the next '0' after the bit to detect '1' spans
				
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
					trailingOnesCount += shift - 1; // Extend trailingOnesCount by the span length
					shiftRight( values, values, 1, last1InValues + 1, shift, true );
				}
			}
			
			used |= IO;
			return this;
		}
		
		public RW add( boolean value ) {
			return value ?
					add1( size ) :
					add0( size );
		}
		
		
		public RW add( int bit, boolean value ) {
			return value ?
					add1( bit ) :
					add0( bit );
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
		 */
		public RW add0( int bit ) {
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
		 */
		public RW add1( int bit ) {
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
			if( bits < 0 ) throw new IllegalArgumentException( "length cannot be negative" );
			
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