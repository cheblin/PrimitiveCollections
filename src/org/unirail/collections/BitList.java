package org.unirail.collections;


import org.unirail.Hash;

import java.util.Arrays;

public interface BitList {
	
	
	abstract class R implements Cloneable, Comparable<R> {
		
		protected int size;
		
		public int size() {return size;}
		
		long[] array = Array.longs0;
		
		static int len4bits( int bits ) {return 1 + (bits >> LEN);}
		
		static final int LEN  = 6;
		static final int BITS = 1 << LEN;
		static final int MASK = BITS - 1;
		
		static int index( int item_X_bits ) {return item_X_bits >> LEN;}
		
		static long mask( int bits )        {return (1L << bits) - 1;}
		
		static final long FFFFFFFFFFFFFFFF = ~0L;
		static final int  OI               = Integer.MAX_VALUE;
		static final int  IO               = Integer.MIN_VALUE;
		
		
		int used = 0;
		
		int used() {
			if (-1 < used) return used;
			
			used &= OI;
			
			int i = used - 1;
			while (-1 < i && array[i] == 0) i--;
			
			return used = i + 1;
		}
		
		int used( int bit ) {
			if (size() <= bit) size = bit + 1;
			
			final int index = bit >> LEN;
			if (index < used()) return index;
			
			if (array.length < (used = index + 1)) array = Arrays.copyOf( array, Math.max( 2 * array.length, used ) );
			
			return index;
		}
		
		
		public boolean get( int bit ) {
			final int index = bit >> LEN;
			return index < used() && (array[index] & 1L << bit) != 0 ;
		}
		
		public int get( int bit, int FALSE, int TRUE ) {
			final int index = bit >> LEN;
			return index < used() && (array[index] & 1L << bit) != 0 ? TRUE : FALSE;
		}
		
		public int get( long[] dst, int from_bit, int to_bit ) {
			
			final int ret = (to_bit - from_bit - 1 >> LEN) + 1;
			
			int index = from_bit >> LEN;
			
			if ((from_bit & MASK) == 0) System.arraycopy( array, index, dst, 0, ret - 1 );
			else
				for (int i = 0; i < ret - 1; i++, index++)
				     dst[i] = array[index] >>> from_bit | array[index + 1] << -from_bit;
			
			
			long mask = FFFFFFFFFFFFFFFF >>> -to_bit;
			dst[ret - 1] =
					(to_bit - 1 & MASK) < (from_bit & MASK) ?
					array[index] >>> from_bit | (array[index + 1] & mask) << -from_bit
					                                        :
					(array[index] & mask) >>> from_bit;
			
			return ret;
		}
		
		
		public int next1( int bit ) {
			
			int index = bit >> LEN;
			if (used() <= index) return -1;
			
			for (long i = array[index] & FFFFFFFFFFFFFFFF << bit; ; i = array[index])
			{
				if (i != 0) return index * BITS + Long.numberOfTrailingZeros( i );
				if (++index == used) return -1;
			}
		}
		
		
		public int next0( int bit ) {
			
			int index = bit >> LEN;
			if (used() <= index) return bit;
			
			for (long i = ~array[index] & FFFFFFFFFFFFFFFF << bit; ; i = ~array[index])
			{
				if (i != 0) return index * BITS + Long.numberOfTrailingZeros( i );
				if (++index == used) return used * BITS;
			}
		}
		
		public int prev1( int bit ) {
			
			int index = bit >> LEN;
			if (used() <= index) return last1() - 1;
			
			
			for (long i = array[index] & FFFFFFFFFFFFFFFF >>> -(bit + 1); ; i = array[index])
			{
				if (i != 0) return (index + 1) * BITS - 1 - Long.numberOfLeadingZeros( i );
				if (index-- == 0) return -1;
			}
		}
		
		
		public int prev0( int bit ) {
			int index = bit >> LEN;
			if (used() <= index) return bit;
			
			for (long i = ~array[index] & FFFFFFFFFFFFFFFF >>> -(bit + 1); ; i = ~array[index])
			{
				if (i != 0) return (index + 1) * BITS - 1 - Long.numberOfLeadingZeros( i );
				if (index-- == 0) return -1;
			}
		}
		
		
		public int last1()       {return used() == 0 ? 0 : BITS * (used - 1) + BITS - Long.numberOfLeadingZeros( array[used - 1] );}
		
		
		public boolean isEmpty() {return used == 0;}
		
		
		public int rank( int bit ) {
			final int max = bit >> LEN;
			
			if (max < used())
				for (int i = 0, sum = 0; ; i++)
					if (i < max) sum += Long.bitCount( array[i] );
					else return sum + Long.bitCount( array[i] & FFFFFFFFFFFFFFFF >>> BITS - (bit + 1) );
			
			return cardinality();
		}
		
		
		public int cardinality() {
			for (int i = 0, sum = 0; ; i++)
				if (i < used()) sum += Long.bitCount( array[i] );
				else return sum;
		}
		
		public int bit( int cardinality ) {
			
			int i = 0, c = 0;
			while ((c += Long.bitCount( array[i] )) < cardinality) i++;
			
			long v = array[i];
			int  z = Long.numberOfLeadingZeros( v );
			
			for (long p = 1L << BITS - 1; cardinality < c; z++) if ((v & p >>> z) != 0) c--;
			
			return i * 32 + BITS - z;
		}
		
		
		public int hashCode() {
			int hash = 197;
			for (int i = used; --i >= 0; )
			     hash = Hash.code( hash , array[i] );
			
			return hash;
		}
		
		public int length() {return array.length * BITS;}
		
		
		public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				dst.array = array.length == 0 ? array : array.clone();
				return dst;
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			
			return null;
		}
		
		public boolean equals( Object obj ) {return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) );}
		
		public int compareTo( R other )     {return equals( other ) ? 0 : 1;}
		
		public boolean equals( R other ) {
			int i = size();
			if (i != other.size()) return false;
			for (i >>>= 6; -1 < i; i--) if (array[i] != other.array[i]) return false;
			return true;
		}
		
		
		public String toString() {return toString( null ).toString();}
		
		StringBuilder toString( StringBuilder dst ) {
			int size = size();
			int max  = size >> LEN;
			
			if (dst == null) dst = new StringBuilder( (max + 1) * 68 );
			else dst.ensureCapacity( dst.length() + (max + 1) * 68 );
			dst.append( String.format( "%-8s%-8s%-8s%-8s%-8s%-8s%-8s%-7s%s", "0", "7", "15", "23", "31", "39", "47", "55", "63" ) );
			dst.append( '\n' );
			dst.append( String.format( "%-8s%-8s%-8s%-8s%-8s%-8s%-8s%-7s%s", "|", "|", "|", "|", "|", "|", "|", "|", "|" ) );
			dst.append( '\n' );
			
			for (int i = 0; i < max; i++)
			{
				final long v = array[i];
				for (int s = 0; s < 64; s++)
				     dst.append( (v & 1L << s) == 0 ? '.' : '*' );
				dst.append( i * 64 );
				dst.append( '\n' );
			}
			
			if (0 < (size &= 63))
			{
				final long v = array[max];
				for (int s = 0; s < size; s++)
				     dst.append( (v & 1L << s) == 0 ? '.' : '*' );
			}
			
			return dst;
		}
	}
	
	
	class RW extends R {
		
		public RW( int length ) {if (0 < length) array = new long[len4bits( length )];}
		
		public RW( boolean fill_value, int size ) {
			
			int len = len4bits( this.size = size );
			array = new long[len];
			
			used = len | IO;
			
			if (fill_value) set1( 0, size - 1 );
		}
		
		public RW( R src, int from_bit, int to_bit ) {
			
			if (src.size() <= from_bit) return;
			size = Math.min( to_bit, src.size() - 1 ) - from_bit;
			
			int i2 = src.get( to_bit )  ? to_bit : src.prev1( to_bit );
			
			if (i2 == -1) return;
			
			array = new long[(i2 - 1 >> LEN) + 1];
			used  = array.length | IO;
			
			int
					i1 = src.get( from_bit ) ? from_bit : src.next1( from_bit ),
					index = i1 >>> LEN,
					max = (i2 >>> LEN) + 1,
					i = 0;
			
			for (long v = src.array[index] >>> i1; ; v >>>= i1, i++)
				if (index + 1 < max)
					array[i] = v | (v = src.array[index + i]) << BITS - i1;
				else
				{
					array[i] = v;
					return;
				}
		}
		
		
		public void and( R and ) {
			if (this == and) return;
			
			if (and.used() < used())
				while (used > and.used) array[--used] = 0;
			
			for (int i = 0; i < used; i++) array[i] &= and.array[i];
			
			used |= IO;
		}
		
		
		public void or( R or ) {
			if (or.used() < 1 || this == or) return;
			
			int u = used;
			if (used() < or.used())
			{
				if (array.length < or.used) array = Arrays.copyOf( array, Math.max( 2 * array.length, or.used ) );
				used = or.used;
			}
			
			int min = Math.min( u, or.used );
			
			for (int i = 0; i < min; i++)
			     array[i] |= or.array[i];
			
			if (min < or.used) System.arraycopy( or.array, min, array, min, or.used - min );
			else if (min < u) System.arraycopy( array, min, or.array, min, u - min );
		}
		
		
		public void xor( R xor ) {
			if (xor.used() < 1 || xor == this) return;
			
			int u = used;
			if (used() < xor.used())
			{
				if (array.length < xor.used) array = Arrays.copyOf( array, Math.max( 2 * array.length, xor.used ) );
				used = xor.used;
			}
			
			final int min = Math.min( u, xor.used );
			for (int i = 0; i < min; i++)
			     array[i] ^= xor.array[i];
			
			if (min < xor.used) System.arraycopy( xor.array, min, array, min, xor.used - min );
			else if (min < u) System.arraycopy( array, min, xor.array, min, u - min );
			
			used |= IO;
		}
		
		public void andNot( R not ) {
			for (int i = Math.min( used(), not.used() ) - 1; -1 < i; i--) array[i] &= ~not.array[i];
			
			used |= IO;
		}
		
		public boolean intersects( R set ) {
			for (int i = Math.min( used, set.used ) - 1; i >= 0; i--)
				if ((array[i] & set.array[i]) != 0) return true;
			
			return false;
		}
		
		public void fit() {length( size() );}
		
		void length( int bits ) {
			if (0 < bits)
			{
				if (bits < size)
				{
					set0( bits, size + 1 );
					size = bits;
				}
				array = Arrays.copyOf( array, index( bits ) + 1 );
				
				used |= IO;
				return;
			}
			
			size  = 0;
			used  = 0;
			array = bits == 0 ? Array.longs0 : new long[index( -bits ) + 1];
		}
		
		public void flip( int bit ) {
			final int index = used( bit );
			if ((array[index] ^= 1L << bit) == 0 && index + 1 == used) used |= IO;
		}
		
		
		public void flip( int from_bit, int to_bit ) {
			if (from_bit == to_bit) return;
			
			int from_index = from_bit >> LEN;
			int to_index   = used( to_bit - 1 );
			
			final long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			final long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if (from_index == to_index)
			{
				if ((array[from_index] ^= from_mask & to_mask) == 0 && from_index + 1 == used) used |= IO;
			}
			else
			{
				array[from_index] ^= from_mask;
				
				for (int i = from_index + 1; i < to_index; i++) array[i] ^= FFFFFFFFFFFFFFFF;
				
				array[to_index] ^= to_mask;
				                   used |= IO;
			}
		}
		
		public void set( int index, boolean... values ) {
			for (int i = 0, max = values.length; i < max; i++)
				if (values[i]) set1( index + i );
				else set0( index + i );
		}
		
		
		public void set1( int bit ) {
			final int index = used( bit );//!!!
			array[index] |= 1L << bit;
		}
		
		
		public void set( boolean value ) {set( size, value );}
		
		public void set( int bit, boolean value ) {
			if (value)
				set1( bit );
			else
				set0( bit );
		}
		
		public void set( int bit, int value ) {
			if (value == 0)
				set0( bit );
			else
				set1( bit );
		}
		
		public void set( int bit, int value, int TRUE ) {
			if (value == TRUE)
				set1( bit );
			else
				set0( bit );
		}
		
		
		public void set1( int from_bit, int to_bit ) {
			
			if (from_bit == to_bit) return;
			
			int from_index = from_bit >> LEN;
			int to_index   = used( to_bit - 1 );
			
			long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if (from_index == to_index) array[from_index] |= from_mask & to_mask;
			else
			{
				array[from_index] |= from_mask;
				
				for (int i = from_index + 1; i < to_index; i++)
				     array[i] = FFFFFFFFFFFFFFFF;
				
				array[to_index] |= to_mask;
			}
		}
		
		
		public void set( int from_bit, int to_bit, boolean value ) {
			if (value)
				set1( from_bit, to_bit );
			else
				set0( from_bit, to_bit );
		}
		
		
		public void set0( int bit ) {
			if (size() <= bit) size = bit + 1;
			
			final int index = bit >> LEN;
			
			if (index < used())
				if (index + 1 == used && (array[index] &= ~(1L << bit)) == 0) used |= IO;
				else
					array[index] &= ~(1L << bit);
		}
		
		
		public void set0( int from_bit, int to_bit ) {
			if (size() <= to_bit) size = to_bit + 1;
			
			if (from_bit == to_bit) return;
			
			int from_index = from_bit >> LEN;
			if (used() <= from_index) return;
			
			int to_index = to_bit - 1 >> LEN;
			if (used <= to_index)
			{
				to_bit   = last1();
				to_index = used - 1;
			}
			
			long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if (from_index == to_index)
			{
				if ((array[from_index] &= ~(from_mask & to_mask)) == 0) if (from_index + 1 == used) used |= IO;
			}
			else
			{
				array[from_index] &= ~from_mask;
				
				for (int i = from_index + 1; i < to_index; i++) array[i] = 0;
				
				array[to_index] &= ~to_mask;
				
				used |= IO;
			}
		}
		
		public void add( long src ) {add( src, 64 );}
		
		public void add( long src, int bits ) {
			if (64 < bits) bits = 64;
			
			int _size = size;
			size += bits;
			
			if ((src &= ~(1L << bits - 1)) == 0) return;
			
			used( _size + BITS - Long.numberOfLeadingZeros( src ) );
			
			int bit = _size & 63;
			
			if (bit == 0) array[index( size )] = src;
			else
			{
				array[index( _size )] &= src << bit | mask( bit );
				if (index( _size ) < index( size )) array[index( size )] = src >> bit;
			}
		}
		
		public void add( int key, boolean value ) {
			if (key < last1())
			{
				int index = key >> LEN;
				
				long m = FFFFFFFFFFFFFFFF << key, v = array[index];
				
				m = (v & m) << 1 | v & ~m;
				
				if (value) m |= 1L << key;
				
				while (++index < used)
				{
					array[index - 1] = m;
					final int t = (int) (v >>> BITS - 1);
					v = array[index];
					m = v << 1 | t;
				}
				array[index - 1] = m;
				                   used |= IO;
				size++;
			}
			else if (value) set1( key );
		}
		
		public void clear() {
			for (used(); used > 0; ) array[--used] = 0;
			size = 0;
		}
		
		
		public void remove( int bit ) {
			if (size <= bit) return;
			
			size--;
			
			int index = bit >> LEN;
			if (used() <= index) return;
			
			
			final int last = last1();
			if (bit == last) set0( bit );
			else if (bit < last)
			{
				long m = FFFFFFFFFFFFFFFF << bit, v = array[index];
				
				v = v >>> 1 & m | v & ~m;
				
				while (++index < used)
				{
					m = array[index];
					
					array[index - 1] = (m & 1) << BITS - 1 | v;
					v                = m >>> 1;
				}
				array[index - 1] = v;
				                   used |= IO;
			}
		}
		
		
		public RW clone() {return (RW) super.clone();}
	}
}

