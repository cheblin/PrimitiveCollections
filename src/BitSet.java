package org.unirail.collections;


import java.util.Arrays;

public interface BitSet {
	
	
	interface Consumer {
		boolean set( int bit, boolean value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		boolean value( int tag );
	}
	
	
	class R implements Cloneable, Comparable<R> {
		
		static final int LEN  = 6;
		static final int BITS = 1 << LEN;
		static final int MASK = BITS - 1;
		
		static final long FFFFFFFFFFFFFFFF = ~0L;
		static final int  OI               = Integer.MAX_VALUE;
		static final int  IO               = ~Integer.MAX_VALUE;
		
		long[] array;
		
		int used = 0;
		
		int used() {
			if (-1 < used) return used;
			
			used &= OI;
			
			int i = used - 1;
			while (i >= 0 && array[i] == 0) i--;
			
			return used = i + 1;
		}
		
		int used( int bit ) {
			
			final int index = bit >> LEN;
			if (index < used()) return index;
			
			if (array.length < (used = index + 1)) array = Arrays.copyOf( array, Math.max( 2 * array.length, used ) );
			
			return index;
		}
		
		public R()           { this( 1 ); }
		
		public R( int bits ) { array = new long[(bits - 1 >> LEN) + 1]; }
		
		public R( long[] array ) {
			this.array = array;
			used       = array.length | IO;
		}
		
		
		public boolean get( int bit ) {
			final int index = bit >> LEN;
			return index < used() && (array[index] & 1L << bit) != 0;
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
					(to_bit - 1 & MASK) < (from_bit & MASK)
					?
					array[index] >>> from_bit |
					(array[index + 1] & mask) << -from_bit
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
			if (used() <= index) return size() - 1;
			
			
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
		
		
		public int size()        { return used() == 0 ? 0 : BITS * (used - 1) + BITS - Long.numberOfLeadingZeros( array[used - 1] );}
		
		
		public boolean isEmpty() { return used == 0; }
		
		
		public int rank( int bit ) {
			final int max = bit >> LEN;
			
			if (max < used())
				for (int i = 0, sum = 0; ; i++)
					if (i < max) sum += Long.bitCount( array[i] );
					else return sum + Long.bitCount( array[i] & ((FFFFFFFFFFFFFFFF >>> BITS - (bit + 1))) );
			
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
		
		public /*CLS*/ long[] subList( int from_bit, int to_bit ) {
			to_bit = prev1( to_bit );
			
			if (to_bit <= from_bit) return new long[0];
			
			long[] dst = new long[(to_bit - from_bit - 1 >> LEN) + 1];
			
			int
					index = from_bit >>> LEN,
					max = (to_bit >>> LEN) + 1,
					i = 0;
			
			for (long v = array[index] >>> from_bit; ; v >>>= from_bit, i++)
				if (index + 1 < max)
					dst[i] = v | (v = array[index + i]) << BITS - from_bit;
				else
				{
					dst[i] = v;
					return dst;
				}
		}
		
		
		public int hashCode() {
			long h = 1234;
			for (int i = used; --i >= 0; )
			     h ^= array[i] * (i + 1);
			
			return (int) (h >> 32 ^ h);
		}
		
		public int length() { return array.length * BITS; }
		
		
		public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				dst.array = array.clone();
				return dst;
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			
			return null;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public int compareTo( R other ) {
			if (other.size() != size()) return other.size() - size();
			
			for (int i = used(); -1 < --i; )
				if (array[i] != other.array[i]) return (int) (array[i] - other.array[i]);
			
			return 0;
		}
		
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				
				public int tag() { return size() - 1; }
				
				public int tag( int tag ) {return --tag;}
				
				public boolean value( int tag ) {return get( tag );}
			} : producer;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( used * 64 );
			else dst.ensureCapacity( dst.length() + used * 64 );
			
			
			for (int i = 0; i < used; dst.append( '\n' ), i++)
			{
				long l = array[i];
				for (int b = 63; -1 < b; b--)
				     dst.append( (1L << b & l) == 0 ? '_' : '*' );
			}
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	
	class RW extends R implements Consumer {
		
		public void fit() {if (used() < array.length) array = Arrays.copyOf( array, used );}
		
		
		public void and( R and ) {
			if (this == and) return;
			
			if (and.used() < used())
				while (used > and.used) array[--used] = 0;
			
			for (int i = 0; i < used; i++) array[i] &= and.array[i];
			
			used |= IO;
		}
		
		
		public void or( R or ) {
			if (this == or) return;
			
			int min = Math.min( used, or.used );
			
			if (used() < or.used())
			{
				if (array.length < or.used) array = Arrays.copyOf( array, Math.max( 2 * array.length, or.used ) );
				used = or.used;
			}
			
			for (int i = 0; i < min; i++)
			     array[i] |= or.array[i];
			
			if (min < or.used) System.arraycopy( or.array, min, array, min, used - min );
		}
		
		
		public void xor( R xor ) {
			
			final int min = Math.min( used, xor.used );
			
			if (used() < xor.used())
			{
				if (array.length < xor.used) array = Arrays.copyOf( array, Math.max( 2 * array.length, xor.used ) );
				used = xor.used;
			}
			
			for (int i = 0; i < min; i++)
			     array[i] ^= xor.array[i];
			
			if (min < xor.used) System.arraycopy( xor.array, min, array, min, xor.used - min );
			
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
		
		
		void allocate( int size ) {array = new long[(size >> 6) + ((size & 63) == 0 ? 0 : 1)]; }
		
		
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
		
		public void set( long[] array ) {
			this.array = array;
			used       = array.length | IO;
		}
		
		public void set1()          { set1( size() ); }
		
		public void set1( int bit ) {array[used( bit )] |= 1L << bit; }
		
		
		public boolean set( int bit, boolean value ) {
			if (value)
				set1( bit );
			else
				set0( bit );
			return true;
		}
		
		
		public void set1( int from_bit, int to_bit ) {
			
			if (from_bit == to_bit) return;
			
			int from_index = from_bit >> LEN;
			int to_index   = used( to_bit - 1 );
			
			long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			if (from_index == to_index)
			{
				array[from_index] |= from_mask & to_mask;
			}
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
		
		
		public void add( int bit, boolean value ) {
			if (bit < size())
			{
				int index = bit >> LEN;
				
				long i = array[index];
				long m = FFFFFFFFFFFFFFFF << bit;
				
				m = (i & m) << 1 | i & ~m;
				
				if (value) m |= 1L << bit;
				
				array[index] = m;
				
				if (array[used - 1] < 0) used++;
				
				while (++index < used)
				{
					m            = array[index];
					array[index] = m << 1 | i >> BITS - 1;
					i            = m;
				}
			}
			else if (value) set1( bit );
		}
		
		public void del() { set0( size() - 1 ); }
		
		public void del( int bit ) {
			final int size = size();
			
			if (bit < size)
				if (size == 1) array[0] = 0;
				else
				{
					int index = bit >> LEN;
					
					long i = array[index];
					
					long m = FFFFFFFFFFFFFFFF << bit;
					
					m = i >>> 1 & m | i & ~m;
					
					array[index] = m;
					
					
					while (++index < used)
					{
						m            = array[index];
						array[index] = m >> 1 | i << BITS - 1;
						i            = m;
					}
					
					if (array[used - 1] == 0) used--;
				}
		}
		
		public void set0( int bit ) {
			final int index = bit >> LEN;
			
			if (index < used())
				if (index + 1 == used && (array[index] &= ~(1L << bit)) == 0) used |= IO;
				else
					array[index] &= ~(1L << bit);
		}
		
		
		public void set0( int from_bit, int to_bit ) {
			
			if (from_bit == to_bit) return;
			
			int from_index = from_bit >> LEN;
			if (used() <= from_index) return;
			
			int to_index = to_bit - 1 >> LEN;
			if (used <= to_index)
			{
				to_bit   = size();
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
		
		public void clear()        {for (used(); used > 0; ) array[--used] = 0;}
		
		public Consumer consumer() {return this; }
		
		public RW clone()          { return (RW) super.clone(); }
		
		
	}
}

