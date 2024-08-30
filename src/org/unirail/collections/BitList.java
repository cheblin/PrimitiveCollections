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

public interface BitList {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		// Size of the BitList
		protected int size;
		
		// Returns the size of the BitList
		public int size() { return size; }
		
		// Array to store bit values
		long[] values = Array.EqualHashOf.longs.O;
		
		// Calculates the length of the array needed to store given bits
		static int len4bits( int bits ) { return 1 + (bits >> LEN); }
		
		// Constants for bit manipulation
		static final int LEN  = 6;//long has 64 bits. 6 bits in mask = 63.
		static final int BITS = 1 << LEN;//64
		static final int MASK = BITS - 1;//63
		
		// Returns the index in the array for the given bit position
		static int index( int item_X_bits ) { return item_X_bits >> LEN; }
		
		// Returns a mask for the given number of bits
		static long mask( int bits ) { return (1L << bits) - 1; }
		
		// Constant representing all bits set to 1
		static final long FFFFFFFFFFFFFFFF = ~0L;
		static final int  OI               = Integer.MAX_VALUE;
		static final int  IO               = Integer.MIN_VALUE;
		
		// Number of used entries in the values array
		int used = 0;
		
		// Returns the number of used entries, recalculates if necessary
		int used() {
			if( -1 < used ) return used;
			
			used &= OI;
			
			int i = used - 1;
			while( -1 < i && values[i] == 0 ) i--;
			
			return used = i + 1;
		}
		
		// Ensures the given bit is within the used range
		int used( int bit ) {
			if( size() <= bit ) size = bit + 1;
			
			final int index = bit >> LEN;
			if( index < used() ) return index;
			
			if( values.length < (used = index + 1) )
				values = Array.copyOf( values, Math.max( values.length + values.length / 2, used ) );
			
			return index;
		}
		
		// Returns the value of the bit at the given position
		public boolean get( int bit ) {
			final int index = bit >> LEN;
			return index < used() && (values[index] & 1L << bit) != 0;
		}
		
		// Returns either FALSE or TRUE based on the value of the bit at the given position
		public int get( int bit, int FALSE, int TRUE ) {
			final int index = bit >> LEN;
			return index < used() && (values[index] & 1L << bit) != 0 ? TRUE : FALSE;
		}
		
		// Copies bits from the BitList into the destination array
		public int get( long[] dst, int from_bit, int to_bit ) {
			
			final int ret = (to_bit - from_bit - 1 >> LEN) + 1;
			
			int index = from_bit >> LEN;
			
			if( (from_bit & MASK) == 0 ) System.arraycopy( values, index, dst, 0, ret - 1 );
			else
				for( int i = 0; i < ret - 1; i++, index++ )
				     dst[i] = values[index] >>> from_bit | values[index + 1] << -from_bit;
			
			
			final long mask = FFFFFFFFFFFFFFFF >>> -to_bit;
			dst[ret - 1] =
					(to_bit - 1 & MASK) < (from_bit & MASK) ?
					values[index] >>> from_bit | (values[index + 1] & mask) << -from_bit
					                                        :
					(values[index] & mask) >>> from_bit;
			
			return ret;
		}
		
		// Returns the next position of a 1 bit starting from the given bit position
		public int next1( int bit ) {
			
			int index = bit >> LEN;
			if( used() <= index ) return -1;
			
			for( long i = values[index] & FFFFFFFFFFFFFFFF << bit; ; i = values[index] )
			{
				if( i != 0 ) return index * BITS + Long.numberOfTrailingZeros( i );
				if( ++index == used ) return -1;
			}
		}
		
		// Returns the next position of a 0 bit starting from the given bit position
		public int next0( int bit ) {
			
			int index = bit >> LEN;
			if( used() <= index ) return bit;
			
			for( long i = ~values[index] & FFFFFFFFFFFFFFFF << bit; ; i = ~values[index] )
			{
				if( i != 0 ) return index * BITS + Long.numberOfTrailingZeros( i );
				if( ++index == used ) return used * BITS;
			}
		}
		
		// Returns the previous position of a 1 bit starting from the given bit position
		public int prev1( int bit ) {
			
			int index = bit >> LEN;
			if( used() <= index ) return last1() - 1;
			
			
			for( long i = values[index] & FFFFFFFFFFFFFFFF >>> -(bit + 1); ; i = values[index] )
			{
				if( i != 0 ) return (index + 1) * BITS - 1 - Long.numberOfLeadingZeros( i );
				if( index-- == 0 ) return -1;
			}
		}
		
		// Returns the previous position of a 0 bit starting from the given bit position
		public int prev0( int bit ) {
			int index = bit >> LEN;
			if( used() <= index ) return bit;
			
			for( long i = ~values[index] & FFFFFFFFFFFFFFFF >>> -(bit + 1); ; i = ~values[index] )
			{
				if( i != 0 ) return (index + 1) * BITS - 1 - Long.numberOfLeadingZeros( i );
				if( index-- == 0 ) return -1;
			}
		}
		
		// Returns the last position of a 1 bit
		public int last1() { return used() == 0 ? 0 : BITS * (used - 1) + BITS - Long.numberOfLeadingZeros( values[used - 1] ); }
		
		// Checks if the BitList is empty
		public boolean isEmpty() { return used == 0; }
		
		// Returns the rank (number of 1 bits up to the given bit position)
		public int rank( int bit ) {
			final int max = bit >> LEN;
			
			if( max < used() )
				for( int i = 0, sum = 0; ; i++ )
					if( i < max ) sum += Long.bitCount( values[i] );
					else return sum + Long.bitCount( values[i] & FFFFFFFFFFFFFFFF >>> BITS - (bit + 1) );
			
			return cardinality();
		}
		
		// Returns the total number of 1 bits in the BitList
		public int cardinality() {
			int sum = 0;
			for( int i = 0, max = used(); i < max; i++ )
			     sum += Long.bitCount( values[i] );
			
			return sum;
		}
		
		// Returns the bit position of the given cardinality
		public int bit( int cardinality ) {
			
			int i = 0, c = 0;
			while( (c += Long.bitCount( values[i] )) < cardinality ) i++;
			
			long v = values[i];
			int  z = Long.numberOfLeadingZeros( v );
			
			for( long p = 1L << BITS - 1; cardinality < c; z++ ) if( (v & p >>> z) != 0 ) c--;
			
			return i * 32 + BITS - z;
		}
		
		// Returns the hash code of the BitList
		public int hashCode() {
			int hash = 197;
			for( int i = used(); -1 < --i; )
			     hash = Array.hash( hash, values[i] );
			
			
			return Array.finalizeHash( hash, size() );
		}
		
		// Returns the length of the BitList in bits
		public int length() { return values.length * BITS; }
		
		// Clones the BitList
		public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				dst.values = values.length == 0 ? values : values.clone();
				return dst;
			} catch( CloneNotSupportedException e ) { e.printStackTrace(); }
			
			return null;
		}
		
		// Checks if the BitList is equal to another object
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) ); }
		
		// Checks if the BitList is equal to another BitList
		public boolean equals( R other ) {
			if( size() != other.size() ) return false;
			
			for( int i = used(); -1 < --i; ) if( values[i] != other.values[i] ) return false;
			return true;
		}
		
		// Returns the JSON representation of the BitList
		public String toString() { return toJSON(); }
		
		// Writes the JSON representation of the BitList
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterArray();
			
			int size = size();
			if( 0 < size )
			{
				int used = used();
				if( 0 < used )
				{
					json.preallocate( (used + 1) * 68 );
					
					for( int i = 0; i < used - 1; i++ )
					{
						final long v = values[i];
						for( int s = 0; s < BITS; s++ )
						     json.value( (v & 1L << s) == 0 ? 0 : 1 );
					}
					
					for( int i = (used - 1) * 64; i < size; i++ )
					     json.value( get( i ) ? 1 : 0 );
				}
			}
			
			json.exitArray();
		}
	}
	
	// Non-abstract class implementing R with modifications for mutable BitList
	class RW extends R {
		
		/**
		 Constructor initializing with specified length.
		 
		 @param length The number of boolean values to be stored.
		 */
		public RW( int length ) { if( 0 < length ) values = new long[len4bits( length )]; }// Create a new long array if length > 0
		
		/**
		 Constructor initializing with default value and size.
		 
		 @param default_value The default boolean value to initialize elements with.
		 @param size          The initial size of the data structure. If negative, the absolute value is used.
		 If size is positive, initialize with the default value.
		 */
		public RW( boolean default_value, int size ) {
			
			// Calculate the number of longs needed to store 'size' bits
			int len = len4bits( this.size = size < 0 ? -size :  size );
			values = new long[len];
			
			// Mark all bits are dirty
			used = len | IO;
			// If size is positive and default_value is true, set all bits to 1
			if( 0 < size && default_value ) set1( 0, size - 1 );
		}
		
		// Constructor initializing with a range from another BitList
		public RW( R src, int from_bit, int to_bit ) {
			
			if( src.size() <= from_bit ) return;
			size = Math.min( to_bit, src.size() - 1 ) - from_bit;
			
			int i2 = src.get( to_bit ) ? to_bit : src.prev1( to_bit );
			
			if( i2 == -1 ) return;
			
			values = new long[(i2 - 1 >> LEN) + 1];
			used   = values.length | IO;
			
			int
					i1 = src.get( from_bit ) ? from_bit : src.next1( from_bit ),
					index = i1 >>> LEN,
					max = (i2 >>> LEN) + 1,
					i = 0;
			
			for( long v = src.values[index] >>> i1; ; v >>>= i1, i++ )
				if( index + 1 < max )
					values[i] = v | (v = src.values[index + i]) << BITS - i1;
				else
				{
					values[i] = v;
					return;
				}
		}
		
		// AND operation on BitList
		public RW and( R and ) {
			if( this == and ) return this;
			
			if( and.used() < used() )
				while( used > and.used ) values[--used] = 0;
			
			for( int i = 0; i < used; i++ ) values[i] &= and.values[i];
			
			used |= IO;
			return this;
		}
		
		// OR operation on BitList
		public RW or( R or ) {
			if( or.used() < 1 || this == or ) return this;
			
			int u = used;
			if( used() < or.used() )
			{
				if( values.length < or.used ) values = Array.copyOf( values, Math.max( 2 * values.length, or.used ) );
				used = or.used;
			}
			
			int min = Math.min( u, or.used );
			
			for( int i = 0; i < min; i++ )
			     values[i] |= or.values[i];
			
			if( min < or.used ) System.arraycopy( or.values, min, values, min, or.used - min );
			else if( min < u ) System.arraycopy( values, min, or.values, min, u - min );
			return this;
		}
		
		// XOR operation on BitList
		public RW xor( R xor ) {
			if( xor.used() < 1 || xor == this ) return this;
			
			int u = used;
			if( used() < xor.used() )
			{
				if( values.length < xor.used ) values = Array.copyOf( values, Math.max( 2 * values.length, xor.used ) );
				used = xor.used;
			}
			
			final int min = Math.min( u, xor.used );
			for( int i = 0; i < min; i++ )
			     values[i] ^= xor.values[i];
			
			if( min < xor.used ) System.arraycopy( xor.values, min, values, min, xor.used - min );
			else if( min < u ) System.arraycopy( values, min, xor.values, min, u - min );
			
			used |= IO;
			return this;
		}
		
		// AND NOT operation on BitList
		public RW andNot( R not ) {
			for( int i = Math.min( used(), not.used() ) - 1; -1 < i; i-- ) values[i] &= ~not.values[i];
			
			used |= IO;
			return this;
		}
		
		// Checks if BitList intersects with another BitList
		public boolean intersects( R set ) {
			for( int i = Math.min( used, set.used ) - 1; i >= 0; i-- )
				if( (values[i] & set.values[i]) != 0 ) return true;
			
			return false;
		}
		
		// Flips the bit at the specified position
		public RW flip( int bit ) {
			final int index = used( bit );
			if( (values[index] ^= 1L << bit) == 0 && index + 1 == used ) used |= IO;
			return this;
		}
		
		// Flips a range of bits
		public RW flip( int from_bit, int to_bit ) {
			if( from_bit == to_bit ) return this;
			
			int from_index = from_bit >> LEN;
			int to_index   = used( to_bit - 1 );
			
			final long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			final long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if( from_index == to_index )
			{
				if( (values[from_index] ^= from_mask & to_mask) == 0 && from_index + 1 == used ) used |= IO;
			}
			else
			{
				values[from_index] ^= from_mask;
				
				for( int i = from_index + 1; i < to_index; i++ ) values[i] ^= FFFFFFFFFFFFFFFF;
				
				values[to_index] ^= to_mask;
				                    used |= IO;
			}
			return this;
		}
		
		// Sets bits starting from index to the specified boolean values
		public RW set( int index, boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
				if( values[i] ) set1( index + i );
				else set0( index + i );
			return this;
		}
		
		// Sets the bit at the specified position to the given boolean value
		public RW set( int bit, boolean value ) {
			if( value )
				set1( bit );
			else
				set0( bit );
			return this;
		}
		
		// Sets the bit at the specified position to the given integer value
		public RW set( int bit, int value ) {
			if( value == 0 )
				set0( bit );
			else
				set1( bit );
			return this;
		}
		
		// Sets the bit at the specified position to the given integer value, compared to TRUE
		public RW set( int bit, int value, int TRUE ) {
			if( value == TRUE )
				set1( bit );
			else
				set0( bit );
			return this;
		}
		
		// Sets a range of bits to the specified boolean value
		public RW set( int from_bit, int to_bit, boolean value ) {
			if( value )
				set1( from_bit, to_bit );
			else
				set0( from_bit, to_bit );
			return this;
		}
		
		// Sets the bit at the specified position to 1
		public RW set1( int bit ) {
			final int index = used( bit );// Ensure the bit index is within range
			values[index] |= 1L << bit;
			return this;
		}
		
		// Sets a range of bits to 1
		public RW set1( int from_bit, int to_bit ) {
			
			if( from_bit == to_bit ) return this;
			
			int from_index = from_bit >> LEN;
			int to_index   = used( to_bit - 1 );
			
			long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if( from_index == to_index ) values[from_index] |= from_mask & to_mask;
			else
			{
				values[from_index] |= from_mask;
				
				for( int i = from_index + 1; i < to_index; i++ )
				     values[i] = FFFFFFFFFFFFFFFF;
				
				values[to_index] |= to_mask;
			}
			return this;
		}
		
		// Sets the bit at the specified position to 0
		public RW set0( int bit ) {
			if( size() <= bit ) size = bit + 1;
			
			final int index = bit >> LEN;
			
			if( index < used() )
				if( index + 1 == used && (values[index] &= ~(1L << bit)) == 0 ) used |= IO;
				else
					values[index] &= ~(1L << bit);
			return this;
		}
		
		// Sets a range of bits to 0
		public RW set0( int from_bit, int to_bit ) {
			if( size() <= to_bit ) size = to_bit + 1;
			
			if( from_bit == to_bit ) return this;
			
			int from_index = from_bit >> LEN;
			if( used() <= from_index ) return this;
			
			int to_index = to_bit - 1 >> LEN;
			if( used <= to_index )
			{
				to_bit   = last1();
				to_index = used - 1;
			}
			
			long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if( from_index == to_index )
			{
				if( (values[from_index] &= ~(from_mask & to_mask)) == 0 && from_index + 1 == used ) used |= IO;
			}
			else
			{
				values[from_index] &= ~from_mask;
				
				for( int i = from_index + 1; i < to_index; i++ ) values[i] = 0;
				
				values[to_index] &= ~to_mask;
				
				used |= IO;
			}
			return this;
		}
		
		// Adds a boolean value to the BitList
		public RW add( boolean value ) { return set( size, value ); }
		
		// Adds a long value to the BitList
		public RW add( long src ) { return add( src, BITS ); }
		
		// Adds a long value to the BitList with a specified number of bits
		public RW add( long src, int bits ) {
			if( BITS < bits ) bits = BITS;
			
			int _size = size;
			size += bits;
			
			if( (src &= ~(1L << bits - 1)) == 0 ) return this;
			
			used( _size + BITS - Long.numberOfLeadingZeros( src ) );
			
			int bit = _size & MASK;
			
			if( bit == 0 ) values[index( size )] = src;
			else
			{
				values[index( _size )] &= src << bit | mask( bit );
				if( index( _size ) < index( size ) ) values[index( size )] = src >> bit;
			}
			return this;
		}
		
		// Adds a boolean value at a specified key
		public RW add( int key, boolean value ) {
			if( key < last1() )
			{
				int index = key >> LEN;
				
				long m = FFFFFFFFFFFFFFFF << key, v = values[index];
				
				m = (v & m) << 1 | v & ~m;
				
				if( value ) m |= 1L << key;
				
				while( ++index < used )
				{
					values[index - 1] = m;
					final int t = (int) (v >>> BITS - 1);
					v = values[index];
					m = v << 1 | t;
				}
				values[index - 1] = m;
				                    used |= IO;
			}
			else if( value )
			{
				final int index = used( key );  // Ensure the key is within range
				values[index] |= 1L << key;
			}
			
			size++;
			return this;
		}
		
		// Removes a bit at the specified position
		public RW remove( int bit ) {
			if( size <= bit ) return this;
			
			size--;
			
			int index = bit >> LEN;
			if( used() <= index ) return this;
			
			
			final int last = last1();
			if( bit == last ) set0( bit );
			else if( bit < last )
			{
				long m = FFFFFFFFFFFFFFFF << bit, v = values[index];
				
				v = v >>> 1 & m | v & ~m;
				
				while( ++index < used )
				{
					m = values[index];
					
					values[index - 1] = (m & 1) << BITS - 1 | v;
					v                 = m >>> 1;
				}
				values[index - 1] = v;
				                    used |= IO;
			}
			return this;
		}
		
		// Method to adjust the internal length to fit the current size.
		public RW fit() { return length( size() ); }
		
		// Adjusts the length of the BitList
		public RW length( int bits ) {
			if( 0 < bits )
			{
				if( bits < size )
				{
					set0( bits, size + 1 );
					size = bits;
				}
				values = Array.copyOf( values, index( bits ) + 1 );
				
				used |= IO;
				return this;
			}
			
			size   = 0;
			used   = 0;
			values = Array.EqualHashOf.longs.O;
			return this;
		}
		
		// Sets the size of the BitList
		public RW size( int size ) {
			
			if( size < size() )
				if( size < 1 ) clear();
				else
				{
					set0( size - 1, size() );
					this.size = size;
				}
			else if( size() < size ) set0( size - 1 );
			
			return this;
		}
		
		// Clears the BitList
		public RW clear() {
			for( used(); used > 0; ) values[--used] = 0;
			size = 0;
			return this;
		}
		// Clones the BitList
		public RW clone() { return (RW) super.clone(); }
	}
}

