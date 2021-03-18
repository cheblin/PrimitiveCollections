package org.unirail.collections;


import java.util.Arrays;

public interface BitsList {
	
	
	interface Consumer {
		boolean add( int value );
	}
	
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		int value( int tag );
	}
	
	
	class R implements Cloneable, Comparable<R> {
		
		public int[] array = new int[4];
		int size = 0;
		
		
		public final int bits;
		public final int mask;
		
		public R( int bits ) {
			this.bits = bits;
			mask      = (1 << bits) - 1;
		}
		
		public R( int bits, int items ) {
			this.bits = bits;
			mask      = (1 << bits) - 1;
			array     = new int[(items >>> LEN) + ((items & MASK) == 0 ? 0 : 1)];
		}
		
		public R( int bits, byte... items ) {
			this( bits, items.length );
			
			for (byte v : items)
			{
				final int index = bits * size++;
				final int item  = index >>> LEN;
				final int bit   = index & MASK;
				
				v &= mask;
				if (bit == 0)
				{
					array[item] = v;
					return;
				}
				
				array[item] |= v << bit;
				if (BITS < bit + bits) array[item + 1] = v >> BITS - bit;
			}
		}
		
		public R( int bits, int... items ) {
			this( bits, items.length );
			
			for (int v : items)
			{
				final int index = bits * size++;
				final int item  = index >>> LEN;
				final int bit   = index & MASK;
				
				v &= mask;
				if (bit == 0)
				{
					array[item] = v;
					return;
				}
				
				array[item] |= v << bit;
				if (BITS < bit + bits) array[item + 1] = v >> BITS - bit;
			}
		}
		
		public int length()      { return array.length * BITS / bits; }
		
		public int size()        { return size; }
		
		public boolean isEmpty() { return size == 0; }
		
		public boolean contains( int value ) {
			return false;
		}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return size() - 1; }
				
				public int tag( int tag ) {return --tag;}
				
				public int value( int tag ) {return get( tag );}
				
			} : producer;
		}
		
		
		public byte[] toArray( byte[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new byte[size];
			
			for (int max = size * bits, index = 0; index < max; index += bits)
			{
				final int item = index >>> LEN;
				final int bit  = index & MASK;
				
				dst[index / bits] = (byte) (BITS < bit + bits ?
				                            array[item] >>> bit | (array[item + 1] & (1 << bit + bits - BITS) - 1) << BITS - bit :
				                            array[item] >>> bit & mask);
			}
			return dst;
		}
		
		
		public int get( int index ) {
			int       item = (index *= bits) >>> LEN;
			final int bit  = index & MASK;
			
			return BITS < bit + bits ?
			       array[item] >>> bit | (array[item + 1] & (1 << bit + bits - BITS) - 1) << BITS - bit :
			       array[item] >>> bit & mask;
		}
		
		
		public int indexOf( int value ) {
			value &= mask;
			
			for (int max = size * bits, index = 0; index < max; index += bits)
			{
				final int item = index >>> LEN;
				final int bit  = index & MASK;
				
				if (value == (BITS < bit + bits ?
				              array[item] >>> bit | (array[item + 1] & (1 << bit + bits - BITS) - 1) << BITS - bit :
				              array[item] >>> bit & mask)) return index / bits;
			}
			
			return -1;
		}
		
		public int lastIndexOf( int value ) {
			value &= mask;
			
			for (int index = (size - 1) * bits; -1 < index; index -= bits)
			{
				final int item = index >>> LEN;
				final int bit  = index & MASK;
				
				if (value == (BITS < bit + bits ?
				              array[item] >>> bit | (array[item + 1] & (1 << bit + bits - BITS) - 1) << BITS - bit :
				              array[item] >>> bit & mask)) return index / bits;
			}
			
			return -1;
			
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			for (int i = (size >>> LEN) + ((size & MASK) == 0 ? 0 : 1); -1 < i; i--)
				if (array[i] != other.array[i]) return array[i] - other.array[i];
			
			return 0;
		}
		
		
		public boolean containsAll( Producer src ) {
			return false;
		}
		
		
		public R subList( int fromIndex, int toIndex ) {
			return null;
		}
		
		
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
		
		static final int BITS = 32;
		static final int MASK = BITS - 1;
		static final int LEN  = BITS - Integer.numberOfLeadingZeros( MASK );
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( size * 2 );
			else dst.ensureCapacity( dst.length() + size * 2 );
			
			for (int i = 0; i < size; dst.append( '\n' ), i++)
			     dst.append( get( i ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW extends R implements Consumer {
		
		public RW( int bits ) {
			super( bits );
		}
		
		public RW( int bits, int items ) {
			super( bits, items );
		}
		
		public RW( int bits, byte... items ) {
			super( bits, items );
		}
		
		public RW( int bits, int... items ) {
			super( bits, items );
		}
		
		public boolean add( int value ) {
			value &= mask;
			
			final int index = bits * size++;
			if (array.length * BITS < index + bits) array = index == 0 ? new int[4] : Arrays.copyOf( array, array.length + array.length / 2 );
			
			final int item = index >>> LEN;
			final int bit  = index & MASK;
			
			if (bit == 0) array[item] = value;
			else
			{
				array[item] |= value << bit;
				if (BITS < bit + bits) array[item + 1] = value >> BITS - bit;
			}
			
			return true;
		}
		
		public void add( int index, int value ) {
			
			if (size <= index)
			{
				add( value );
				return;
			}
			
			value &= mask;
			
			int       item = (index *= bits) >>> LEN;
			final int bit  = index & MASK;
			size++;
			
			final int[] src = array;
			int[]       dst = array;
			
			if (array.length * BITS < size * bits)
			{
				dst = array = new int[array.length + array.length / 2];
				if (0 < item) System.arraycopy( src, 0, dst, 0, item );
			}
			
			if (0 < bit)
			{
				final int i = src[item];
				final int k = BITS - bit;
				if (BITS < bit + bits)
				{
					dst[item] = (i << k) >>> k | value << bit;
					value     = value >> bits - k | src[item + 1] << k & mask;
				}
				else
				{
					dst[item] = (i << k) >>> k | value << bit | i >>> bit << bit + bits;
					value     = i >>> bit + bits | src[item + 1] << k - bits & mask;
				}
				item++;
			}
			
			for (final int max = (size >> LEN) + ((size & MASK) == 0 ? 0 : 1); ; )
			{
				final int i = src[item];
				dst[item] = i << bits | value;
				if (max < ++item) break;
				value = i >>> BITS - bits;
			}
		}
		
		public int set( int index, int value ) {
			
			value &= mask;
			
			final int item = (index = bits * (index < size ? index : size)) >>> LEN;
			final int bit  = index & MASK;
			final int i    = array[item];
			
			final int k = BITS - bit;
			
			if (k < bits)
			{
				int ii = array[item + 1];
				
				int ret = i >>> bit | (ii & (1 << bits + k) - 1) << k;
				array[item]     = i << k | value << bit;
				array[item + 1] = ii >>> bits + k << bits + k | value >> k;
				return ret;
			}
			
			array[item] = (i << k) >>> k | value << bit | i >>> bit + bits << bit + bits;
			
			return i >>> bit & mask;
		}
		
		
		public boolean remove( int value ) {
			
			return false;
		}
		
		
		public int removeAt( int index ) {
			
			int ret = get( index );
			
			int       item = (index *= bits) >>> LEN;
			final int bit  = index & MASK;
			
			final int k = BITS - bit;
			int       i = array[item];
			
			if (bit == 0) array[item] = i >>>= bits;
			else if (k < bits)//на разломе
			{
				int ii = array[item + 1];
				
				array[item]   = (i << k) >>> k | ii >>> bit + bits - BITS << bit;
				array[++item] = i = ii >>> bits;
			}
			else if (bits < k)//полностью внутри
			{
				int ii = array[item + 1];
				
				array[item]   = (i << k) >>> k | i >>> bit + bits << bit | ii << BITS - bits;
				array[++item] = i = ii >>> bits;
			}
			
			for (final int max = size * bits; item * BITS < max; )
			{
				final int ii = array[item + 1];
				array[item]   = (i << bits) >>> bits | ii << BITS - bits;
				array[++item] = i = ii >>> bits;
			}
			
			size--;
			
			return ret;
		}
		
		public boolean addAll( Producer src ) {
			return false;
		}
		
		public boolean addAll( int index, Producer src ) {
			return false;
		}
		
		public boolean removeAll( Producer src ) {
			return false;
		}
		
		public boolean retainAll( Producer src ) {
			return false;
		}
		
		public void clear() {
		
		}
		
		public Consumer consumer() {return this; }
		
		public RW clone()          { return (RW) super.clone(); }
		
	}
}
