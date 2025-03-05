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
 * {@code BitList} interface defines a contract for bit manipulation collections.
 * It offers functionalities to manage and query a dynamic list of bits,
 * optimized for space and performance.
 */
public interface BitList {
	
	/**
	 * Abstract base class {@code R} provides a read-only implementation of the {@code BitList} interface.
	 * It includes functionalities for bit storage, size management, and read operations.
	 * This class is designed to be extended by concrete {@code BitList} implementations.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		/**
		 * Total number of bits in the list.
		 */
		protected int    size;
		/**
		 * Number of leading '1' bits at the beginning of the list that are not explicitly stored in {@code values}.
		 * This optimization helps in space efficiency when dealing with lists that start with a sequence of '1's.
		 */
		protected int    leadingOnesCount = 0;
		/**
		 * Array to store the bits of the list after the leading '1's.
		 * Each element is a {@code long} representing 64 bits.
		 */
		protected long[] values           = Array.EqualHashOf._longs.O;
		/**
		 * Number of {@code long} elements in the {@code values} array that are currently in use to store bits.
		 * This tracks the actual storage being used and can be less than the total length of the {@code values} array.
		 */
		protected int    used             = 0;
		
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
		static int len4bits( int bits ) { return 1 + ( bits >> LEN ); }
		
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
		static long mask( int bits ) { return ( 1L << bits ) - 1; }
		
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
		 * @return The index in the {@code values} array where the bit is located, or -1 if the bit is within the leading '1's range.
		 * Expands the {@code values} array if the index is out of bounds.
		 */
		int used( int bit ) {
			
			if( size() <= bit ) size = bit + 1;
			int index = bit - leadingOnesCount >> LEN;
			if( index < 0 ) return -1; // Within leading '1's
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
			if( bit < leadingOnesCount ) return true;
			int index = bit - leadingOnesCount >> LEN;
			return index < used() && ( values[ index ] & 1L << ( bit - leadingOnesCount & MASK ) ) != 0;
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
			if( from_bit >= to_bit || from_bit < 0 || to_bit <= 0 || size <= from_bit ) return 0;
			to_bit = Math.min( to_bit, size );
			int bits_to_copy = to_bit - from_bit;
			if( bits_to_copy <= 0 ) return 0;
			
			int dst_index      = 0;
			int dst_bit_offset = 0;
			int copied_bits    = 0;
			
			int leading_ones_to_copy = Math.min( leadingOnesCount - from_bit, bits_to_copy );
			if( leading_ones_to_copy > 0 ) {
				for( int i = 0; i < leading_ones_to_copy; i++ ) {
					if( dst_bit_offset == BITS ) {
						dst_bit_offset = 0;
						dst_index++;
						if( dst_index >= dst.length ) return copied_bits;
					}
					dst[ dst_index ] |= 1L << dst_bit_offset;
					dst_bit_offset++;
				}
				from_bit += leading_ones_to_copy;
				bits_to_copy -= leading_ones_to_copy;
				copied_bits += leading_ones_to_copy;
				if( bits_to_copy == 0 ) return copied_bits;
			}
			
			int start_bit_in_values        = from_bit - leadingOnesCount;
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
		 * @return The index of the next '1' bit, or the size of the {@code BitList} if no '1' bit is found from the specified position to the end.
		 */
		public int next1( int bit ) {
			// Adjust negative start to 0; if beyond size, return size
			if( bit < 0 ) bit = 0;
			if( bit >= size() ) return size();
			
			// If within leading ones, return the starting bit (it’s a '1')
			if( bit < leadingOnesCount ) return bit;
			
			// Adjust position relative to end of leading ones
			int bitOffset = bit - leadingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			int pos       = bitOffset & MASK;   // Bit position within the long
			
			// If beyond used values, all remaining are '0', so return size
			if( index >= used() ) return size();
			
			// Mask to consider only bits from pos onward in the first long
			long mask  = -1L << pos; // 1s from pos to end
			long value = values[ index ] & mask; // Check for '1's from pos
			
			// Check the first long
			if( value != 0 ) {
				return leadingOnesCount + ( index << LEN ) + Long.numberOfTrailingZeros( value );
			}
			
			// Search subsequent longs
			for( int i = index + 1; i < used(); i++ ) {
				value = values[ i ];
				if( value != 0 ) {
					return leadingOnesCount + ( i << LEN ) + Long.numberOfTrailingZeros( value );
				}
			}
			
			// No '1' found, return size
			return size();
		}
		
		/**
		 * Finds and returns the index of the next '0' bit starting from the specified position.
		 *
		 * @param bit The starting bit position to search from (inclusive).
		 * @return The index of the next '0' bit, or the size of the {@code BitList} if no '0' bit is found from the specified position to the end.
		 */
		/**
		 * Finds and returns the index of the next '0' bit starting from the specified position.
		 *
		 * @param bit The starting bit position to search from (inclusive).
		 * @return The index of the next '0' bit, or the size of the {@code BitList} if no '0' bit is found from the specified position to the end.
		 */
		public int next0( int bit ) {
			// If starting position is invalid (negative) or beyond size, return size
			if( bit < 0 ) bit = 0; // Adjust negative start to beginning
			if( size() <= bit ) return size(); // No '0' beyond size
			
			// Check within leading ones region (all '1's)
			if( bit < leadingOnesCount )
				return leadingOnesCount; // First '0' is after leading ones
			
			
			// Adjust bit position relative to the end of leading ones
			int bitOffset = bit - leadingOnesCount;
			int index     = bitOffset >> LEN; // Which long in values array
			int pos       = bitOffset & MASK;   // Bit position within that long
			
			// If starting beyond used values, all remaining bits are '0', so return bit
			if( index >= used() ) return bit;
			
			// Mask to consider only bits from pos onward in the first long
			long mask  = -1L << pos; // 1s from pos to end of 64 bits
			long value = ~values[ index ] & mask; // Invert to find '0's, apply mask
			
			// Search within the first long
			if( value != 0 )
				return leadingOnesCount + ( index << LEN ) + Long.numberOfTrailingZeros( value );
			
			
			// Search subsequent longs
			for( int i = index + 1; i < used(); i++ ) {
				value = ~values[ i ]; // Invert to find '0's
				if( value != 0 ) return leadingOnesCount + ( i << LEN ) + Long.numberOfTrailingZeros( value );
			}
			
			// No '0' found, return size
			return size();
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
			
			// If within leading ones, return the bit itself if valid, else adjust
			if( bit < leadingOnesCount ) {
				return bit; // All bits up to leadingOnesCount-1 are '1'
			}
			
			// Adjust position relative to end of leading ones
			int bitOffset = bit - leadingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			int pos       = bitOffset & MASK;   // Bit position within the long
			
			// If beyond used values, search up to last used bit
			if( index >= used() ) {
				return last1(); // Return last '1' in the list
			}
			
			// Mask to consider only bits up to pos (inclusive) in the first long
			long mask  = ( 1L << ( pos + 1 ) ) - 1; // 1s from 0 to pos
			long value = values[ index ] & mask; // Check for '1's up to pos
			
			// Check the current long
			if( value != 0 ) {
				return leadingOnesCount + ( index << LEN ) + ( BITS - 1 - Long.numberOfLeadingZeros( value ) );
			}
			
			// Search previous longs
			for( int i = index - 1; i >= 0; i-- ) {
				value = values[ i ];
				if( value != 0 ) {
					return leadingOnesCount + ( i << LEN ) + ( BITS - 1 - Long.numberOfLeadingZeros( value ) );
				}
			}
			
			// If no '1' in values, check leading ones
			if( leadingOnesCount > 0 ) {
				return leadingOnesCount - 1; // Last '1' in leading ones
			}
			
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
			
			// If within leading ones (all '1's), no '0' exists before
			if( bit < leadingOnesCount ) {
				return -1; // All bits up to leadingOnesCount-1 are '1'
			}
			
			// Adjust position relative to end of leading ones
			int bitOffset = bit - leadingOnesCount;
			int index     = bitOffset >> LEN; // Index in values array
			int pos       = bitOffset & MASK;   // Bit position within the long
			
			// If beyond used values, all trailing bits are '0', check up to used
			if( index >= used() ) {
				if( bit == leadingOnesCount ) {
					return leadingOnesCount > 0 ?
							leadingOnesCount - 1 :
							-1;
				}
				return bit; // Bits beyond used are '0'
			}
			
			// Mask to consider only bits up to pos (inclusive) in the current long
			long mask  = ( 1L << ( pos + 1 ) ) - 1; // 1s from 0 to pos
			long value = ~values[ index ] & mask; // Invert to find '0's up to pos
			
			// Check the current long
			if( value != 0 ) {
				return leadingOnesCount + ( index << LEN ) + ( BITS - 1 - Long.numberOfLeadingZeros( value ) );
			}
			
			// Search previous longs
			for( int i = index - 1; i >= 0; i-- ) {
				value = ~values[ i ]; // Invert to find '0's
				if( value != 0 ) {
					return leadingOnesCount + ( i << LEN ) + ( BITS - 1 - Long.numberOfLeadingZeros( value ) );
				}
			}
			
			// No '0' in values, return -1 (all bits before are leading ones)
			return -1;
		}
		
		/**
		 * Returns the index of the last '1' bit in this {@code BitList}.
		 *
		 * @return The index of the last '1' bit, or -1 if the {@code BitList} contains no '1' bits.
		 */
		public int last1() {
			if( used() == 0 )
				return 0 < leadingOnesCount ?
						leadingOnesCount - 1 :
						-1;
			return leadingOnesCount + ( used - 1 << LEN ) + Long.numberOfTrailingZeros( Long.highestOneBit( values[ used - 1 ] ) );
		}
		
		/**
		 * Checks if all bits in this {@code BitList} are '0'.
		 *
		 * @return {@code true} if all bits are '0', {@code false} otherwise.
		 */
		public boolean isAllZeros() { return leadingOnesCount == 0 && used == 0; }
		
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
			
			// Adjust bit position if it exceeds the size of the list.
			if( size <= bit ) bit = size - 1;
			
			// If the bit is within the leading ones region, the rank is simply the bit position + 1.
			if( bit < leadingOnesCount ) return bit + 1;
			if( used() == 0 ) return leadingOnesCount;
			
			// Calculate rank for bits beyond leading ones.
			int index = bit - leadingOnesCount >> LEN; // Index of the long containing the bit.
			// Count '1's in the current long up to the specified bit, and add leading ones count.
			int sum = leadingOnesCount + Long.bitCount( values[ index ] << BITS - 1 - ( bit - leadingOnesCount ) );
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
			
			// If within leading ones, return cardinality - 1 (since all are '1's)
			if( cardinality <= leadingOnesCount ) {
				return cardinality - 1; // 0-based index of the cardinality-th '1'
			}
			
			// Adjust cardinality for bits beyond leading ones
			int remainingCardinality = cardinality - leadingOnesCount;
			int totalBits            = size() - leadingOnesCount; // Bits stored in values
			
			// Scan through values array
			for( int i = 0; i < used() && remainingCardinality > 0; i++ ) {
				long value      = values[ i ];
				int  bitsInLong = Math.min( BITS, totalBits - ( i << LEN ) ); // Bits in this long
				int  count      = Long.bitCount( value & mask( bitsInLong ) ); // '1's in this long
				
				if( remainingCardinality <= count ) {
					// Find the exact bit in this long
					for( int j = 0; j < bitsInLong; j++ ) {
						if( ( value & ( 1L << j ) ) != 0 ) {
							if( --remainingCardinality == 0 ) {
								return leadingOnesCount + ( i << LEN ) + j;
							}
						}
					}
				}
				remainingCardinality -= count;
			}
			
			// Should not reach here if cardinality is valid, but return -1 for safety
			return -1;
		}
		
		/**
		 * Generates a hash code for this {@code BitList}.
		 * The hash code is based on the size, leading ones count, and the content of the {@code values} array.
		 *
		 * @return The hash code value for this {@code BitList}.
		 */
		@Override
		public int hashCode() {
			int hash = 197;
			for( int i = used(); -1 < --i; ) hash = Array.hash( hash, values[ i ] );
			hash = Array.hash( hash, leadingOnesCount );
			return Array.finalizeHash( hash, size() );
		}
		
		/**
		 * Returns the total potential bit capacity of the underlying storage, including leading ones and allocated {@code values}.
		 * This value represents the maximum bit index that could be addressed without resizing the {@code values} array,
		 * plus the bits represented by {@code leadingOnesCount}.
		 *
		 * @return The length of the {@code BitList} in bits, considering allocated storage.
		 */
		public int length() { return leadingOnesCount + ( values.length << LEN ); }
		
		/**
		 * Creates and returns a deep copy of this {@code R} instance.
		 * The cloned object will have the same size, leading ones count, and bit values as the original.
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
		 * {@code BitLists} are considered equal if they have the same size, leading ones count, and bit values.
		 */
		public boolean equals( R other ) {
			if( size() != other.size() || leadingOnesCount != other.leadingOnesCount ) return false;
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
				json.preallocate( ( used + ( leadingOnesCount >> LEN ) + 1 ) * 68 );
				for( int i = 0; i < leadingOnesCount; i++ ) json.value( 1 );
				for( int i = 0; i < used; i++ ) {
					long v = values[ i ];
					int limit = i == used - 1 ?
							size - leadingOnesCount - ( i << LEN ) :
							BITS;
					for( int s = 0; s < limit; s++ )
					     json.value( ( v & 1L << s ) == 0 ?
							                 0 :
							                 1 );
				}
			}
			json.exitArray();
		}
		
		/**
		 * Creates a new read-only {@code BitList} (RW) that is a view of a portion of this {@code BitList}.
		 * The new {@code BitList} reflects the bits from {@code fromBit} (inclusive) to {@code toBit} (exclusive).
		 *
		 * @param fromBit The starting bit index of the sublist (inclusive).
		 * @param toBit   The ending bit index of the sublist (exclusive).
		 * @return A new {@code RW} instance representing the specified sublist.
		 */
		public R getBits( int fromBit, int toBit ) { return new RW( this, fromBit, toBit ); }
		
		/**
		 * Converts this {@code BitList} to a byte array.
		 * The bits are packed into bytes in little-endian order.
		 *
		 * @return A byte array representing the bits in this {@code BitList}.
		 */
		public byte[] toByteArray() {
			int    byteLength   = size() + 7 >> 3;
			byte[] dst          = new byte[ byteLength ];
			int    dst_i        = 0;
			int    leadingBytes = leadingOnesCount >> 3;
			for( int i = 0; i < leadingBytes; i++ ) dst[ dst_i++ ] = ( byte ) 0xFF;
			int remainingLeadingBits = leadingOnesCount & 7;
			if( remainingLeadingBits > 0 ) dst[ dst_i++ ] = ( byte ) mask( remainingLeadingBits );
			int usedLongs = used();
			for( int i = 0; i < usedLongs; i++ ) {
				long data = values[ i ];
				int bytesToWrite = i == usedLongs - 1 ?
						size - leadingOnesCount - ( i << LEN ) + 7 >> 3 :
						8;
				for( int j = 0; j < bytesToWrite; j++ ) dst[ dst_i++ ] = ( byte ) ( data >>> j * 8 );
			}
			return dst;
		}
		
		/**
		 * Counts the number of leading zero bits in this {@code BitList}.
		 * If the {@code BitList} starts with a sequence of zeros, this method returns the length of that sequence.
		 * If the {@code BitList} starts with a '1' or is all ones, it returns 0.
		 *
		 * @return The number of leading zero bits.
		 */
		public int countLeadingZeros() {
			if( leadingOnesCount == 0 ) {
				if( isAllZeros() ) return size();
				int leadingZeros = 0;
				for( int i = 0; i < size(); i++ )
					if( !get( i ) ) return leadingZeros;
					else leadingZeros++;
			}
			return 0;
		}
		
		/**
		 * Counts the number of trailing zero bits in this {@code BitList}.
		 * If the {@code BitList} ends with a sequence of zeros, this method returns the length of that sequence.
		 * If the {@code BitList} ends with a '1' or is all ones, it returns 0.
		 *
		 * @return The number of trailing zero bits.
		 */
		public int countTrailingZeros() {
			if( isAllZeros() ) return size();
			int trailingZeros = 0;
			for( int i = size - 1; i >= 0; i-- )
				if( !get( i ) ) trailingZeros++;
				else break;
			return trailingZeros;
		}
	}
	
	/**
	 * {@code RW} class extends {@code R} and provides a read-write implementation of the {@code BitList} interface.
	 * It inherits read operations from {@code R} and adds functionalities for modifying bits,
	 * such as setting, clearing, flipping bits, and performing bitwise operations.
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
				if( default_value ) leadingOnesCount = this.size = size; // All bits are 1, stored efficiently as leading ones
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
			if( from_bit < src.leadingOnesCount ) {
				int onesInRange = Math.min( src.leadingOnesCount - from_bit, size );
				leadingOnesCount = onesInRange;
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
			leadingOnesCount = Math.min( leadingOnesCount, and.leadingOnesCount );
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
			if( or.used() < 1 && or.leadingOnesCount == 0 ) return this;
			leadingOnesCount = Math.max( leadingOnesCount, or.leadingOnesCount );
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
			leadingOnesCount = Math.min( leadingOnesCount, xor.leadingOnesCount ); // XOR cancels leading '1's up to min
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
			leadingOnesCount = Math.max( 0, leadingOnesCount - not.leadingOnesCount );
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
			if( leadingOnesCount > 0 && set.leadingOnesCount > 0 ) return true;
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
		 * This method handles the leading ones optimization and array expansion if needed.
		 *
		 * @param bit The bit position to set to '1' (0-indexed).
		 * @return This {@code RW} instance after setting the bit to '1'.
		 */
		public RW set1( int bit ) {
			// Skip if bit is already set within the leading ones region - no change needed
			if( bit < leadingOnesCount ) return this;
			
			// Extend size to include the bit if it’s beyond current bounds - new bits default to 0
			if( size <= bit ) size = bit + 1;
			
			// Case: Setting the bit immediately following leading ones - extend contiguous 1s
			if( bit == leadingOnesCount ) {
				// If values array is unused, simply increment leadingOnesCount to include the new 1
				if( used == 0 ) {
					leadingOnesCount++;
					return this;
				}
				
				// Examine values[0] to determine if this bit extends a span of contiguous 1s
				long O     = values[ 0 ];    // First long in values, representing bits after leadingOnesCount
				int  span1 = 1;         // Initial span includes only the bit being set
				
				// Check if values contains bits and has any 1s to extend the span
				// Note: Bit 0 in values[0] corresponds to leadingOnesCount, assumed 0 unless set
				if( 0 < used() && O != 0 ) {
					// Special case: O = -2L means 63 bits are 1 (0b...10, bit 0 = 0)
					// Count contiguous 1s in values[0] starting from bit 1 (after bit 0)
					if( O == -2L ) {
						span1 = BITS;  // Full 64-bit span minus bit 0
						// Scan subsequent longs for additional contiguous 1s
						for( int i = 1; i < used(); i++ ) {
							long v = values[ i ];
							if( v == -1L ) {
								span1 += BITS; // Add full 64 bits for each all-1s long
								continue;
							}
							// Count leading 1s in the first non-all-1s long, starting from bit 1
							while( ( ( v >>>= 1 ) & 1L ) != 0 ) span1++;
							break;
						}
					}
					else while( ( ( O >>>= 1 ) & 1L ) != 0 ) span1++;
					
					// If the span of 1s reaches the last set bit, absorb all values into leadingOnesCount
					// span1 - 1 adjusts for the new bit being counted in span1
					if( last1() - leadingOnesCount == span1 - 1 ) {
						Arrays.fill( values, 0, used, 0 ); // Clear values as all 1s are now in leadingOnesCount
						used = 0;
						leadingOnesCount += span1;
						return this;
					}
				}
				
				// Extend leadingOnesCount by the span and shift values right to make room
				// Unlike set0, we shift right here to prepend the new 1s, adjusting remaining bits
				leadingOnesCount += span1;
				
				int array_max_bit = ( used() << LEN );
				shiftRight( values, 0, array_max_bit, 0, array_max_bit, span1, true );
				used |= IO;
				return this;
			}
			
			// Case: Setting a bit beyond leading ones - directly modify values array
			int bitOffset = bit - leadingOnesCount; // Position relative to end of leading ones
			int index     = bitOffset >> LEN;           // Index of the target long in values
			int pos       = bitOffset & MASK;             // Bit position within that long
			
			// Resize values array if the index exceeds current capacity
			if( index >= values.length )
				values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), index + 1 ) );
			// Update used count if the index extends beyond current usage
			if( index >= used ) used = index + 1;
			
			// Set the specified bit to 1 in the values array
			values[ index ] |= 1L << pos;
			return this;
		}
		
		/**
		 * Sets a range of bits within the {@code values} array to '1'.
		 * This is a helper method for setting bits in the {@code values} array, handling bit offsets and array boundaries.
		 *
		 * @param from_bit The starting bit position within the {@code values} array to set to '1' (inclusive).
		 * @param to_bit   The ending bit position within the {@code values} array to set to '1' (exclusive).
		 */
		private void valuesSet1( int from_bit, int to_bit ) {
			
			int from_index    = from_bit >> LEN; // Calculate starting index in `values` array
			int to_index      = used( leadingOnesCount + to_bit - 1 ); // Ensure `values` array is large enough and get ending index
			int bitFromOffset = from_bit & MASK;
			int bitToOffset   = to_bit & MASK;
			
			// If range is within a single long element
			if( from_index == to_index ) values[ from_index ] |= -1L << bitFromOffset & ( bitToOffset == 0 ?
					-1L :
					-1L >>> BITS - bitToOffset );
			else { // If range spans multiple long elements
				values[ from_index ] |= -1L << bitFromOffset; // Set bits from `from_bit` to end of `from_index` long to '1'
				for( int i = from_index + 1; i < to_index; i++ ) values[ i ] = -1L; // Set all bits in intermediate long elements to '1'
				if( bitToOffset > 0 ) values[ to_index ] |= -1L >>> BITS - bitToOffset; // Set bits from beginning of `to_index` long to `to_bit` to '1'
			}
		}
		
		/**
		 * Sets a range of bits from {@code from_bit} (inclusive) to {@code to_bit} (exclusive) to '1'.
		 * Modifies this BitList in place.
		 *
		 * @param from_bit The starting bit position of the range to set to '1' (inclusive).
		 * @param to_bit   The ending bit position of the range to set to '1' (exclusive).
		 * @return This BitList after setting the range of bits to '1'.
		 */
		public RW set1( int from_bit, int to_bit ) {
			// Validate range: no-op if invalid or empty
			if( from_bit < 0 || from_bit >= to_bit ) return this;
			
			// Extend size to accommodate the range if necessary
			if( size < to_bit ) size = to_bit;
			
			// Case 1: Entire range within leading ones, already set
			if( to_bit <= leadingOnesCount ) return this;
			
			// Case 2: Range starts within leading ones and extends beyond
			if( from_bit <= leadingOnesCount ) {
				int start = leadingOnesCount; // Begin setting beyond current leading ones
				if( start == from_bit && used > 0 && values[ 0 ] != 0 ) {
					// Optimize: Extend leadingOnesCount if contiguous with values
					long O     = values[ 0 ];
					int  span1 = to_bit - from_bit; // Initial span to set
					if( O == -1L ) {
						span1 = BITS;
						for( int i = 1; i < used(); i++ ) {
							long v = values[ i ];
							if( v == -1L ) { span1 += BITS; continue; }
							span1 += Long.numberOfTrailingZeros( ~v );
							break;
						}
					}
					else {
						long temp = O >>> 1;
						while( ( temp & 1L ) != 0 ) { span1++; temp >>>= 1; }
					}
					if( last1() - leadingOnesCount == span1 - ( to_bit - from_bit ) ) {
						Arrays.fill( values, 0, used, 0 );
						used             = 0;
						leadingOnesCount = from_bit + span1;
						return this;
					}
					leadingOnesCount = to_bit;
					
					shiftLeft( values, 0, size(), 0, size(), to_bit - from_bit, true );
					used |= IO;
				}
				else {
					// Set remaining bits beyond leadingOnesCount
					leadingOnesCount = from_bit; // Align with start
					int bitOffsetStart = start - leadingOnesCount;
					int bitOffsetEnd   = to_bit - 1 - leadingOnesCount;
					int fromIndex      = bitOffsetStart >> LEN;
					int toIndex        = bitOffsetEnd >> LEN;
					int fromBit        = bitOffsetStart & MASK;
					int toBit          = bitOffsetEnd & MASK;
					
					if( toIndex >= values.length )
						values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), toIndex + 1 ) );
					if( toIndex >= used ) used = toIndex + 1;
					
					if( fromIndex == toIndex ) values[ fromIndex ] |= mask( toBit + 1 - fromBit ) << fromBit;
					else {
						values[ fromIndex ] |= -1L << fromBit;
						for( int i = fromIndex + 1; i < toIndex; i++ ) values[ i ] = -1L;
						values[ toIndex ] |= mask( toBit + 1 );
					}
				}
				return this;
			}
			
			// Case 3: Entire range beyond leading ones
			int bitOffsetStart = from_bit - leadingOnesCount;
			int bitOffsetEnd   = to_bit - 1 - leadingOnesCount;
			int fromIndex      = bitOffsetStart >> LEN;
			int toIndex        = bitOffsetEnd >> LEN;
			int fromBit        = bitOffsetStart & MASK;
			int toBit          = bitOffsetEnd & MASK;
			
			if( toIndex >= values.length )
				values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), toIndex + 1 ) );
			if( toIndex >= used ) used = toIndex + 1;
			
			if( fromIndex == toIndex ) values[ fromIndex ] |= mask( toBit + 1 - fromBit ) << fromBit;
			else {
				values[ fromIndex ] |= -1L << fromBit;
				for( int i = fromIndex + 1; i < toIndex; i++ ) values[ i ] = -1L;
				values[ toIndex ] |= mask( toBit + 1 );
			}
			return this;
		}
		
		/**
		 * Sets the bit at the specified position to '0'.
		 * This may involve adjusting the leading ones count and shifting bits in the values array.
		 *
		 * @param bit The bit position to set to '0' (0-indexed).
		 * @return This {@code RW} instance after setting the bit to '0'.
		 */
		public RW set0( int bit ) {
			// No action needed for negative indices; they’re invalid
			if( bit < 0 ) return this;
			
			// Extend size if bit is beyond current bounds, initializing new bits as 0
			if( size <= bit ) {
				size = bit + 1;
				return this;
			}
			
			// Case: Clearing a bit within the leading ones region
			if( bit < leadingOnesCount ) {
				// Calculate bits already stored in values array beyond leading ones
				// last1() gives the highest 1 bit’s index; subtract leadingOnesCount to get value-stored bits
				int items_in_values = last1() - leadingOnesCount;
				
				// Number of bits from 'bit' to the end of leading ones (including bit itself)
				int preserve = leadingOnesCount - bit;
				// Reduce leading ones to exclude bits from 'bit' onward
				leadingOnesCount = bit;
				
				// If there are bits in values, shift them left to align with new leadingOnesCount
				// This preserves any existing bits beyond the original leading ones
				if( 0 < items_in_values )
					shiftLeft( values, 0, size(), 0, size(), preserve, true );
				
				// Preserve the 1s from bit+1 to the original end of leading ones in values
				// Starts at position 1 (bit after 'bit'), covering 'preserve' bits total
				// Note: Position 0 in values becomes 0, representing the cleared bit
				
				valuesSet1( 1, preserve - 1 );
				used |= IO;
				return this;
			}
			
			// Case: Clearing a bit in the values array beyond leading ones
			int bitOffset = bit - leadingOnesCount; // Offset from end of leading ones
			int index     = bitOffset >> LEN;           // Which long in values array
			int pos       = bitOffset & MASK;             // Bit position within that long
			
			// If bit is beyond used values, it’s already 0, so no action needed
			if( used() <= index ) return this;
			
			// Clear the specified bit by flipping it to 0
			values[ index ] &= ~( 1L << pos );
			
			// If clearing the last used long and it becomes 0, mark used for recalculation
			// IO flag triggers used() to recompute, potentially reducing used count
			if( index + 1 == used && values[ index ] == 0 ) used |= IO;
			
			return this;
		}
		
		/**
		 * Sets a range of bits from {@code from_bit} (inclusive) to {@code to_bit} (exclusive) to '0'.
		 * This operation can affect the leading ones count and shift bits within the values array.
		 *
		 * @param from_bit The starting bit position of the range to set to '0' (inclusive).
		 * @param to_bit   The ending bit position of the range to set to '0' (exclusive).
		 * @return This {@code RW} instance after setting the range of bits to '0'.
		 */
		public RW set0( int from_bit, int to_bit ) {
			// Validate range: skip if invalid (negative start) or empty/inverted
			if( from_bit < 0 || to_bit <= from_bit ) return this;
			
			// Ensure size accommodates the range, new bits default to 0
			if( size < to_bit ) size = to_bit;
			
			// Case 1: Entire range within leading ones - truncate and preserve trailing bits
			if( to_bit <= leadingOnesCount ) {
				// Number of bits already in values beyond leading ones (may be negative if all 1s are in leadingOnesCount)
				int items_in_values = last1() - leadingOnesCount;
				
				// Bits to preserve after to_bit (trailing 1s beyond the cleared range)
				int preserve1 = leadingOnesCount - to_bit;
				// Total bits to move into values, including the cleared range and preserved bits
				int move_into_values = leadingOnesCount - from_bit;
				// Truncate leading ones to exclude the range starting at from_bit
				leadingOnesCount = from_bit;
				
				// Pre-allocate space in values for existing bits plus those moved from leadingOnesCount
				// Ensures array capacity for items_in_values + move_into_values
				used( leadingOnesCount + items_in_values + move_into_values );
				
				// If values contains bits, shift them left to make room for moved bits
				if( items_in_values > 0 ) {
					
					shiftLeft( values, 0, size(), 0, size(), move_into_values, true );
					used |= IO;
				}
				
				// Preserve trailing 1s (if any) after the cleared range, starting at the end of the cleared section
				// Range: from move_into_values - preserve1 (start of preserved bits) to move_into_values (end)
				if( move_into_values > 1 ) valuesSet1( move_into_values - preserve1, move_into_values );
				
				return this;
			}
			
			// Case 2: Range spans leading ones and values - truncate leading ones and clear into values
			if( from_bit < leadingOnesCount ) {
				// Bits from from_bit to end of leading ones to potentially preserve
				int preserve = leadingOnesCount - from_bit;
				// Truncate leading ones to exclude bits from from_bit onward
				leadingOnesCount = from_bit;
				
				// Calculate end position in values array for the range
				int bitOffsetEnd = to_bit - 1 - leadingOnesCount;
				int toIndex      = bitOffsetEnd >> LEN;
				int toBit        = bitOffsetEnd & MASK;
				
				// Expand values array if needed to cover the range
				if( toIndex >= values.length )
					values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), toIndex + 1 ) );
				if( toIndex >= used ) used = toIndex + 1;
				
				// Preserve any leading ones beyond from_bit into values
				if( preserve > 0 ) {
					if( used > 0 ) {// Shift existing values to align with new leadingOnesCount
						
						shiftLeft( values, 0, size(), 0, size(), preserve, true );
						used |= IO;
					}
					if( preserve > 1 ) valuesSet1( 1, preserve ); // Set preserved bits from position 1
					else if( preserve == 1 ) values[ 0 ] |= 1L; // Single bit case
					used = Math.max( used, 1 ); // Ensure at least one long is used
				}
				
				// Clear bits from leadingOnesCount to to_bit in values
				// Single long: mask preserves bits before from_bit and after to_bit
				if( toIndex == 0 ) values[ 0 ] &= ~( mask( toBit + 1 ) & -1L >>> BITS - ( preserve > 0 ?
						preserve :
						0 ) );
				else {
					// Multiple longs: clear from start to end, preserving initial bits if any
					values[ 0 ] &= mask( preserve > 0 ?
							                     preserve :
							                     0 );
					for( int i = 1; i < toIndex; i++ ) values[ i ] = 0;
					values[ toIndex ] &= ~mask( toBit + 1 );
				}
				// Flag used for recalculation if last long becomes 0
				if( toIndex + 1 == used && values[ toIndex ] == 0 ) used |= IO;
				return this;
			}
			
			// Case 3: Entire range beyond leading ones - clear bits directly in values
			int bitOffsetStart = from_bit - leadingOnesCount;
			int bitOffsetEnd   = to_bit - leadingOnesCount;
			if( bitOffsetStart < ( used() << LEN ) ) {
				clearBits( values, bitOffsetStart, bitOffsetEnd );
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
			if( _size < leadingOnesCount ) {
				leadingOnesCount += bits;
				return this;
			}
			int bitOffset = _size - leadingOnesCount;
			int index     = bitOffset >> LEN;
			int pos       = bitOffset & MASK;
			if( index >= values.length ) values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), index + 2 ) );
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
		 * Sets a bit at a specific key (index) with a boolean value.
		 * If the key is out of bounds, the {@code BitList} is resized to accommodate it.
		 *
		 * @param key   The index (bit position) to set.
		 * @param value The boolean value to set ({@code true} for '1', {@code false} for '0').
		 * @return This {@code RW} instance after setting the bit.
		 */
		public RW add( int key, boolean value ) {
			if( key < 0 ) return this;
			if( size <= key ) size = key + 1;
			return set( key, value );
		}
		
		/**
		 * Removes the bit at the specified position, shifting subsequent bits to the left.
		 *
		 * @param bit The bit position to remove (0-indexed).
		 * @return This {@code RW} instance after removing the bit.
		 */
		public RW remove( int bit ) {
			if( bit < 0 || size <= bit ) return this;
			size--;
			if( bit < leadingOnesCount ) {
				leadingOnesCount--;
				return this;
			}
			int index = bit - leadingOnesCount >> LEN;
			if( index >= used() ) return this;
			int  pos  = bit - leadingOnesCount & MASK;
			long mask = ~( 1L << pos );
			values[ index ] &= mask;
			for( int i = index + 1; i < used; i++ ) {
				values[ i - 1 ] = values[ i - 1 ] & mask | ( values[ i ] & 1L ) << BITS - 1;
				values[ i ] >>>= 1;
			}
			if( used > 0 && values[ used - 1 ] == 0 ) used |= IO;
			return this;
		}
		
		/**
		 * Trims any trailing zero bits from the end of the {@code BitList}, reducing its size to the position of the last '1' bit.
		 *
		 * @return This {@code RW} instance after trimming trailing zero bits.
		 */
		public RW trim() {
			int lastBit = last1();
			if( lastBit + 1 < size ) length( lastBit + 1 );
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
		public RW fromByteArray( byte[] bytes ) {
			clear();
			if( bytes == null || bytes.length == 0 ) return this;
			size = bytes.length * 8;
			int i = 0;
			while( i < bytes.length && bytes[ i ] == ( byte ) 0xFF ) i++;
			leadingOnesCount = i * 8;
			if( leadingOnesCount == size ) return this;
			int remainingBits    = size - leadingOnesCount;
			int longLengthNeeded = len4bits( remainingBits );
			if( values.length < longLengthNeeded ) values = Array.copyOf( values, longLengthNeeded );
			int longIndex = 0;
			int bitOffset = 0;
			for( ; i < bytes.length; i++ ) {
				long currentByte = bytes[ i ] & 0xFFL;
				if( bitOffset <= BITS - 8 ) {
					values[ longIndex ] |= currentByte << bitOffset;
					bitOffset += 8;
					if( bitOffset == BITS ) {
						bitOffset = 0;
						longIndex++;
					}
				}
				else {
					int remainingBitsInLong = BITS - bitOffset;
					values[ longIndex ] |= currentByte << bitOffset;
					longIndex++;
					values[ longIndex ] |= currentByte >>> remainingBitsInLong;
					bitOffset = 8 - remainingBitsInLong;
				}
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
			if( bits <= leadingOnesCount ) {
				leadingOnesCount = bits;
				values           = Array.EqualHashOf._longs.O;
				used             = 0;
			}
			else {
				int newLength = index( bits - leadingOnesCount ) + 1;
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
		 * This operation resets size, leading ones count, and clears the values array.
		 *
		 * @return This {@code RW} instance after clearing all bits.
		 */
		public RW clear() {
			java.util.Arrays.fill( values, 0, used, 0L );
			used             = 0;
			size             = 0;
			leadingOnesCount = 0;
			return this;
		}
		
		/**
		 * Creates and returns a deep copy of this {@code RW} instance.
		 * The cloned object will have the same size, leading ones count, and bit values as the original.
		 *
		 * @return A clone of this {@code RW} instance.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		
		/**
		 * Shifts all bits in this {@code BitList} to the left by the specified number of positions.
		 * Bits shifted out of the leftmost positions are discarded, and vacated positions on the right are filled with '0's.
		 *
		 * @param n The number of positions to shift left. Must be non-negative.
		 * @return This {@code RW} instance after the left shift operation.
		 * @throws IllegalArgumentException if {@code n} is negative.
		 */
		public RW shiftLeft( int n ) {
			if( n < 0 ) throw new IllegalArgumentException( "Shift value must be non-negative" );
			if( n == 0 || isAllZeros() ) return this;
			if( n >= size() ) {
				clear();
				return this;
			}
			if( n < leadingOnesCount ) leadingOnesCount -= n;
			else {
				int shiftInValues = n - leadingOnesCount;
				leadingOnesCount = 0;
				
				shiftLeft( values, 0, size(), 0, size(), shiftInValues, true );
				used |= IO;
			}
			size -= n;
			return this;
		}
		
		/**
		 * Shifts all bits in this {@code BitList} to the right by the specified number of positions.
		 * Bits shifted out of the rightmost positions are discarded, and vacated positions on the left are filled with '0's.
		 *
		 * @param n The number of positions to shift right. Must be non-negative.
		 * @return This {@code RW} instance after the right shift operation.
		 * @throws IllegalArgumentException if {@code n} is negative.
		 */
		public RW shiftRight( int n ) {
			if( n < 0 ) throw new IllegalArgumentException( "Shift value must be non-negative" );
			if( n == 0 || isAllZeros() ) return this;
			if( n >= size() ) {
				clear();
				return this;
			}
			int shiftLongs = n >> LEN;
			int shiftBits  = n & MASK;
			int newUsed = used() + shiftLongs + ( shiftBits > 0 ?
					1 :
					0 );
			if( values.length < newUsed ) values = Array.copyOf( values, Math.max( values.length + ( values.length >> 1 ), newUsed ) );
			if( leadingOnesCount > n ) leadingOnesCount -= n;
			else {
				int bitsFromValues = n - leadingOnesCount;
				leadingOnesCount = 0;
				
				int array_max_bit = ( used() << LEN );
				shiftRight( values, 0, array_max_bit, 0, array_max_bit, bitsFromValues, true );
				used |= IO;
			}
			size -= n;
			return this;
		}
		
		
		/**
		 * Shifts bits within a specified range of a {@code long} array to the left by the given count.
		 * Bits shifted beyond the range {@code [from_bit, to_bit)} are discarded, and bits outside this range remain unchanged.
		 * Vacated lower bits are filled with zeros if {@code clear} is true. The array is modified in-place.
		 *
		 * @param array         The {@code long} array to modify.
		 * @param array_min_bit Minimum valid bit index in the array (inclusive, >= 0).
		 * @param array_max_bit Maximum valid bit index in the array (exclusive, <= array.length * BITS).
		 * @param from_bit      Starting bit index of the shift range (adjusted to {@code array_min_bit} if smaller).
		 * @param to_bit        Ending bit index of the shift range (exclusive, adjusted to {@code array_max_bit} if larger).
		 * @param shift_bits    Number of positions to shift left (>= 0).
		 * @param clear         If true, vacated bits are cleared to zero; otherwise, they retain their original values.
		 * @throws IllegalArgumentException If {@code shift_bits} is negative, {@code array} is null, or bounds are invalid.
		 */
		static void shiftLeft( long[] array, int array_min_bit, int array_max_bit, int from_bit, int to_bit, int shift_bits, boolean clear ) {
			if( shift_bits < 0 ) throw new IllegalArgumentException( "Shift count must be non-negative" );
			if( array == null ) throw new IllegalArgumentException( "Array cannot be null" );
			if( array_min_bit < 0 || array_max_bit <= array_min_bit || array_max_bit > array.length * BITS )
				throw new IllegalArgumentException( "Invalid array bounds: array_min_bit=" + array_min_bit + ", array_max_bit=" + array_max_bit );
			
			from_bit = Math.max( from_bit, array_min_bit );
			to_bit   = Math.min( to_bit, array_max_bit );
			if( from_bit >= to_bit || shift_bits == 0 ) return;
			
			int rangeBits = to_bit - from_bit;
			if( shift_bits >= rangeBits ) {
				if( clear ) clearBits( array, from_bit, to_bit );
				return;
			}
			
			int fromIndex  = from_bit >> 6;
			int toIndex    = to_bit - 1 >> 6;
			int fromBit    = from_bit & 63;
			int toBit      = to_bit - 1 & 63;
			int shiftLongs = shift_bits >> 6;
			int shiftBits  = shift_bits & 63;
			
			if( shiftLongs >= toIndex - fromIndex + 1 ) {
				if( clear ) clearBits( array, from_bit, to_bit );
				return;
			}
			
			long firstOriginal = array[ fromIndex ];
			long lastOriginal = toIndex < array.length ?
					array[ toIndex ] :
					0;
			
			if( shiftBits == 0 && shiftLongs > 0 )
				System.arraycopy( array, fromIndex + shiftLongs, array, fromIndex, toIndex - fromIndex - shiftLongs + 1 );
			else {
				int destIndex = toIndex;
				int srcIndex  = toIndex - shiftLongs;
				while( srcIndex > fromIndex ) {
					long left  = array[ srcIndex ] << shiftBits;
					long right = array[ srcIndex - 1 ] >>> BITS - shiftBits;
					array[ destIndex-- ] = left | right;
					srcIndex--;
				}
				if( srcIndex == fromIndex ) array[ destIndex ] = array[ srcIndex ] << shiftBits;
			}
			
			if( fromBit != 0 ) {
				long mask = mask( fromBit );
				array[ fromIndex ] = array[ fromIndex ] & ~mask | firstOriginal & mask;
			}
			if( toBit != 63 && toIndex < array.length ) {
				long mask = mask( toBit + 1 );
				array[ toIndex ] = array[ toIndex ] & mask | lastOriginal & ~mask;
			}
			
			if( clear ) clearBits( array, from_bit, from_bit + shift_bits );
		}
		
		/**
		 * Shifts bits within a specified range of a {@code long} array to the right by the given count.
		 * Bits shifted beyond the range {@code [from_bit, to_bit)} are discarded, and bits outside this range remain unchanged.
		 * Vacated higher bits are filled with zeros if {@code clear} is true. The array is modified in-place.
		 *
		 * @param array         The {@code long} array to modify.
		 * @param array_min_bit Minimum valid bit index in the array (inclusive, >= 0).
		 * @param array_max_bit Maximum valid bit index in the array (exclusive, <= array.length * BITS).
		 * @param from_bit      Starting bit index of the shift range (adjusted to {@code array_min_bit} if smaller).
		 * @param to_bit        Ending bit index of the shift range (exclusive, adjusted to {@code array_max_bit} if larger).
		 * @param shift_bits    Number of positions to shift right (>= 0).
		 * @param clear         If true, vacated bits are cleared to zero; otherwise, they retain their original values.
		 * @throws IllegalArgumentException If {@code shift_bits} is negative, {@code array} is null, or bounds are invalid.
		 */
		static void shiftRight( long[] array, int array_min_bit, int array_max_bit, int from_bit, int to_bit, int shift_bits, boolean clear ) {
			if( shift_bits < 0 ) throw new IllegalArgumentException( "Shift count must be non-negative" );
			if( array == null ) throw new IllegalArgumentException( "Array cannot be null" );
			if( array_min_bit < 0 || array_max_bit <= array_min_bit || array_max_bit > array.length * BITS )
				throw new IllegalArgumentException( "Invalid array bounds: array_min_bit=" + array_min_bit + ", array_max_bit=" + array_max_bit );
			
			from_bit = Math.max( from_bit, array_min_bit );
			to_bit   = Math.min( to_bit, array_max_bit );
			if( from_bit >= to_bit || shift_bits == 0 ) return;
			
			int rangeBits = to_bit - from_bit;
			if( shift_bits >= rangeBits ) {
				if( clear ) clearBits( array, from_bit, to_bit );
				return;
			}
			
			int fromIndex  = from_bit >> 6;
			int toIndex    = to_bit - 1 >> 6;
			int fromBit    = from_bit & 63;
			int toBit      = to_bit - 1 & 63;
			int shiftLongs = shift_bits >> 6;
			int shiftBits  = shift_bits & 63;
			
			if( shiftLongs >= toIndex - fromIndex + 1 ) {
				if( clear ) clearBits( array, from_bit, to_bit );
				return;
			}
			
			long firstOriginal = fromIndex + shiftLongs < array.length ?
					array[ fromIndex + shiftLongs ] :
					0;
			long lastOriginal = toIndex < array.length ?
					array[ toIndex ] :
					0;
			
			if( shiftBits == 0 && shiftLongs > 0 )
				System.arraycopy( array, fromIndex, array, fromIndex + shiftLongs, toIndex - fromIndex - shiftLongs + 1 );
			else {
				int destIndex = fromIndex + shiftLongs;
				int srcIndex  = fromIndex;
				while( srcIndex < toIndex ) {
					long right = array[ srcIndex ] >>> shiftBits;
					long left  = array[ srcIndex + 1 ] << BITS - shiftBits;
					array[ destIndex++ ] = left | right;
					srcIndex++;
				}
				if( srcIndex == toIndex ) array[ destIndex ] = array[ srcIndex ] >>> shiftBits;
			}
			
			if( fromBit != 0 && fromIndex + shiftLongs < array.length ) {
				long mask = mask( fromBit );
				array[ fromIndex + shiftLongs ] = array[ fromIndex + shiftLongs ] & ~mask | firstOriginal & mask;
			}
			if( toBit != 63 && toIndex < array.length ) {
				long mask = ~mask( toBit + 1 );
				array[ toIndex ] = array[ toIndex ] & mask | lastOriginal & ~mask;
			}
			
			if( clear ) clearBits( array, to_bit - shift_bits, to_bit );
		}
		
		/**
		 * Sets all bits in the specified range of a {@code long} array to zero.
		 * Bits outside the range remain unchanged. The array is modified in-place.
		 *
		 * @param array    The {@code long} array to modify.
		 * @param from_bit Starting bit index of the range to clear (inclusive).
		 * @param to_bit   Ending bit index of the range to clear (exclusive).
		 */
		static void clearBits( long[] array, int from_bit, int to_bit ) {
			if( from_bit >= to_bit ) return;
			int fromIndex = from_bit >> 6;
			int toIndex   = to_bit - 1 >> 6;
			int fromBit   = from_bit & 63;
			int toBit     = to_bit - 1 & 63;
			
			if( fromIndex >= array.length ) return;
			if( toIndex >= array.length ) toIndex = array.length - 1;
			
			if( fromIndex == toIndex ) {
				long mask = mask( toBit + 1 - fromBit ) << fromBit;
				array[ fromIndex ] &= ~mask;
			}
			else {
				if( fromBit != 0 ) {
					array[ fromIndex ] &= mask( fromBit );
					fromIndex++;
				}
				for( int i = fromIndex; i < toIndex; i++ ) array[ i ] = 0;
				if( toBit != 63 ) array[ toIndex ] &= ~mask( toBit + 1 );
			}
		}
		
		
	}
	
	
}