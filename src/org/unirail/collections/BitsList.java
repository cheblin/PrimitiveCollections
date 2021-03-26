package org.unirail.collections;


import java.util.Arrays;

public interface BitsList {
	
	
	class R implements Cloneable, Comparable<R> {
		
		public int[] array = new int[4];
		int size = 0;
		
		
		public final int bits;
		public final int mask;
		
		public R( int bits_per_item ) {
			bits = bits_per_item;
			mask      = (1 << bits_per_item) - 1;
		}
		
		public R( int bits_per_item, int items ) {
			bits = bits_per_item;
			mask      = (1 << bits_per_item) - 1;
			array     = new int[(items >>> LEN) + ((items & MASK) == 0 ? 0 : 1)];
		}
		
		public static R of( int bits_per_item, byte... values ) {
			R dst = new R( bits_per_item, values.length );
			fill( dst, values );
			return dst;
		}
		
		static void fill( R dst, byte... items ) {
			
			final int bits = dst.bits;
			for (byte v : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				v &= dst.mask;
				
				dst.array[index] |= v << bit;
				if (BITS < bit + bits) dst.array[index + 1] = v >> BITS - bit;
			}
		}
		
		public static R of( int bits_per_item, int... values ) {
			R dst = new R( bits_per_item, values.length );
			fill( dst, values );
			return dst;
		}
		
		static void fill( R dst, int... items ) {
			
			final int bits = dst.bits;
			for (int v : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				v &= dst.mask;
				
				dst.array[index] |= v << bit;
				if (BITS < bit + bits) dst.array[index + 1] = v >> BITS - bit;
			}
		}
		
		public int length()                  { return array.length * BITS / bits; }
		
		public int size()                    { return size; }
		
		public boolean isEmpty()             { return size == 0; }
		
		public boolean contains( int value ) { return -1 < indexOf( value ); }
		
		private IntList.Producer producer;
		
		public IntList.Producer producer() {
			return producer == null ? producer = new IntList.Producer() {
				public int tag() { return 0 < size ? 0 : -1; }
				
				public int tag( int tag ) { return ++tag < size ? tag : -1; }
				
				public int value( int tag ) {return get( tag );}
				
			} : producer;
		}
		
		
		public byte[] toArray( byte[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new byte[size];
			
			for (int max = size * bits, item = 0; item < max; item += bits)
			{
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst[item / bits] = (byte) (BITS < bit + bits ?
				                           array[index] >>> bit | (array[index + 1] & (1 << bit + bits - BITS) - 1) << BITS - bit :
				                           array[index] >>> bit & mask);
			}
			return dst;
		}
		
		
		public int get( int item ) {
			int       index = (item *= bits) >>> LEN;
			final int bit   = item & MASK;
			
			return BITS < bit + bits ?
			       array[index] >>> bit | (array[index + 1] & (1 << bit + bits - BITS) - 1) << BITS - bit :
			       array[index] >>> bit & mask;
		}
		
		
		public int indexOf( int value ) {
			value &= mask;
			
			for (int max = size * bits, item = 0; item < max; item += bits)
			{
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				if (value == (BITS < bit + bits ?
				              array[index] >>> bit | (array[index + 1] & (1 << bit + bits - BITS) - 1) << BITS - bit :
				              array[index] >>> bit & mask)) return item / bits;
			}
			
			return -1;
		}
		
		public int lastIndexOf( int value ) {
			value &= mask;
			
			for (int item = (size - 1) * bits; -1 < item; item -= bits)
			{
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				if (value == (BITS < bit + bits ?
				              array[index] >>> bit | (array[index + 1] & (1 << bit + bits - BITS) - 1) << BITS - bit :
				              array[index] >>> bit & mask)) return item / bits;
			}
			
			return -1;
			
		}
		
		
		public boolean equals( Object other ) {
			
			return other != null &&
			       getClass() == other.getClass() &&
			       compareTo( getClass().cast( other ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			for (int i = (size >>> LEN) + ((size & MASK) == 0 ? 0 : 1); -1 < i; i--)
				if (array[i] != other.array[i]) return array[i] - other.array[i];
			
			return 0;
		}
		
		
		public boolean containsAll( IntList.Producer src ) {
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
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class Rsize extends R {
		
		public Rsize( int bits_per_item ) {
			super( bits_per_item );
		}
		
		public Rsize( int bits_per_item, int items ) {
			super( bits_per_item, items );
		}
		
		public boolean set( int item, int value ) {
			if (array.length <= item) return false;
			
			value &= mask;
			
			int       index = item * bits >>> LEN;
			final int bit   = item * bits & MASK;
			
			final int k = BITS - bit, i = array[index];
			
			if (k < bits)
			{
				array[index]     = i << k | value << bit;
				array[index + 1] = array[index + 1] >>> bits + k << bits + k | value >> k;
			}
			else array[index] = i << k >>> k | value << bit | i >>> bit + bits << bit + bits;
			return true;
		}
		
		public static Rsize of( int bits_per_item, byte... values ) {
			Rsize dst = new Rsize( bits_per_item, values.length );
			fill( dst, values );
			return dst;
		}
		
		public static Rsize of( int bits_per_item, int... values ) {
			Rsize dst = new Rsize( bits_per_item, values.length );
			fill( dst, values );
			return dst;
		}
	}
	
	class RW extends Rsize implements IntList.Consumer {
		
		public RW( int bits_per_item ) {
			super( bits_per_item );
		}
		
		public RW( int bits_per_item, int items ) {
			super( bits_per_item, items );
		}
		
		public static RW of( int bits_per_item, byte... values ) {
			RW dst = new RW( bits_per_item, values.length );
			fill( dst, values );
			return dst;
		}
		
		public static RW of( int bits_per_item, int... values ) {
			RW dst = new RW( bits_per_item, values.length );
			fill( dst, values );
			return dst;
		}
		
		public boolean add( int value ) {
			value &= mask;
			
			final int item = bits * size++;
			if (array.length * BITS < item + bits) array = item == 0 ? new int[4] : Arrays.copyOf( array, array.length + array.length / 2 );
			
			final int index = item >>> LEN;
			final int bit   = item & MASK;
			
			if (bit == 0) array[index] = value;
			else
			{
				array[index] |= value << bit;
				if (BITS < bit + bits) array[index + 1] = value >> BITS - bit;
			}
			
			return true;
		}
		
		public void add( int item, int value ) {
			
			if (size <= item)
			{
				set( item, value );
				return;
			}
			
			value &= mask;
			
			int index = item * bits >>> LEN;
			size++;
			
			final int[] src = array;
			int[]       dst = array;
			
			if (array.length * BITS < size * bits)
			{
				dst = array = new int[array.length + array.length / 2];
				if (0 < index) System.arraycopy( src, 0, dst, 0, index );
			}
			
			final int bit = item * bits & MASK;
			if (0 < bit)
			{
				final int i = src[index];
				final int k = BITS - bit;
				if (bit + bits < BITS)
				{
					dst[index] = i << k >>> k | value << bit | i >>> bit << bit + bits;
					value      = i >>> bit + bits | src[index + 1] << k - bits & mask;
				}
				else
				{
					dst[index] = i << k >>> k | value << bit;
					value      = value >> k | i >> bit << bits - k;
				}
				index++;
			}
			
			for (final int max = (size * bits >> LEN) + ((size * bits & MASK) == 0 ? 0 : 1); ; )
			{
				final int i = src[index];
				dst[index] = i << bits | value;
				if (max < ++index) break;
				value = i >>> BITS - bits;
			}
		}
		
		public boolean set( int item, int value ) {
			
			value &= mask;
			
			int       index = item * bits >>> LEN;
			final int bit   = item * bits & MASK;
			
			if (size <= item)
			{
				size = item + 1;
				if (array.length <= index + (bit == 0 ? 0 : 1)) array = Arrays.copyOf( array, array.length + array.length / 2 );
			}
			
			final int k = BITS - bit, i = array[index];
			
			if (k < bits)
			{
				array[index]     = i << k | value << bit;
				array[index + 1] = array[index + 1] >>> bits + k << bits + k | value >> k;
			}
			else array[index] = i << k >>> k | value << bit | i >>> bit + bits << bit + bits;
			
			return true;
		}
		
		
		public void remove( int value ) {
			for (int i; -1 < (i = lastIndexOf( value )); )
			     removeAt( i );
		}
		
		
		public void removeAt( int item ) {
			
			
			int       index = (item *= bits) >>> LEN;
			final int bit   = item & MASK;
			
			final int k = BITS - bit;
			int       i = array[index];
			
			if (bit == 0) array[index] = i >>>= bits;
			else if (k < bits)
			{
				int ii = array[index + 1];
				
				array[index]   = i << k >>> k | ii >>> bit + bits - BITS << bit;
				array[++index] = i = ii >>> bits;
			}
			else if (bits < k)
			{
				int ii = array[index + 1];
				
				array[index]   = i << k >>> k | i >>> bit + bits << bit | ii << BITS - bits;
				array[++index] = i = ii >>> bits;
			}
			
			for (final int max = size * bits; index * BITS < max; )
			{
				final int ii = array[index + 1];
				array[index]   = i << bits >>> bits | ii << BITS - bits;
				array[++index] = i = ii >>> bits;
			}
			
			size--;
			
		}
		
		public boolean addAll( IntList.Producer src ) {
			int fix = size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) add( src.value( tag ) );
			return size != fix;
		}
		
		public int addAll( int from, IntList.Producer src ) {
			int fix = size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) add( from++, src.value( tag ) );
			return size - fix;
		}
		
		public int removeAll( IntList.Producer src ) {
			
			int fix = size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) remove( src.value( tag ) );
			return fix - size;
		}
		
		public boolean retainAll( IntList.Consumer chk ) {
			final int fix = size;
			
			for (int item = 0, v; item < size; item++)
				if (!chk.add( v = get( item ) )) remove( v );
			
			return fix != size;
		}
		
		public void clear()                { size = 0; }
		
		public IntList.Consumer consumer() {return this; }
		
		public RW clone()                  { return (RW) super.clone(); }
		
	}
}
