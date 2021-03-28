package org.unirail.collections;


import java.util.Arrays;

public interface BitsList {
	
	
	class R implements Cloneable, Comparable<R> {
		
		static final int BITS = 64;
		static final int MASK = BITS - 1;
		static final int LEN  = 6;
		
		public long[] array = new long[4];
		int size = 0;
		
		
		public final int bits;
		public final int mask;
		
		public R( int bits_per_item ) {
			bits = bits_per_item;
			mask = (1 << bits_per_item) - 1;
		}
		
		public R( int bits_per_item, int items ) {
			bits  = bits_per_item;
			mask  = (1 << bits_per_item) - 1;
			array = new long[(items >>> LEN) + ((items & MASK) == 0 ? 0 : 1)];
		}
		
		public static R of( int bits_per_item, byte... values ) {
			R dst = new R( bits_per_item, values.length );
			fill( dst, values );
			return dst;
		}
		
		static void fill( R dst, byte... items ) {
			
			final int bits = dst.bits;
			for (byte b : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				long v = b & dst.mask;
				
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
			for (int i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				long v = i & dst.mask;
				
				dst.array[index] |= v  << bit;
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
			
			return (int) (BITS < bit + bits ?
			              array[index] >>> bit | (array[index + 1] & (1L << bit + bits - BITS) - 1) << BITS - bit :
			              array[index] >>> bit & mask);
		}
		
		
		public int indexOf( int value ) {
			value &= mask;
			
			for (int max = size * bits, item = 0; item < max; item += bits)
			{
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				if (value == (BITS < bit + bits ?
				              array[index] >>> bit | (array[index + 1] & (1L << bit + bits - BITS) - 1) << BITS - bit :
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
				              array[index] >>> bit | (array[index + 1] & (1L << bit + bits - BITS) - 1) << BITS - bit :
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
				if (array[i] != other.array[i]) return (int) (array[i] - other.array[i]);
			
			return 0;
		}
		
		
		public boolean containsAll( IntList.Producer src ) {
			return false;
		}
		
		
		public R subList( int fromIndex, int toIndex ) {
			return null;
		}
		
		protected static boolean set( R dst, int item, int value ) {
			
			final long v = value & dst.mask;
			
			int       index = item * dst.bits >>> LEN;
			final int bit   = item * dst.bits & MASK;
			
			if (dst.size <= item)
			{
				dst.size = item + 1;
				if (dst.array.length <= index + (bit == 0 ? 0 : 1)) dst.array = Arrays.copyOf( dst.array, dst.array.length + dst.array.length / 2 );
			}
			
			final long k = BITS - bit, i = dst.array[index];
			
			if (k < dst.bits)
			{
				dst.array[index]     = i << k | v << bit;
				dst.array[index + 1] = dst.array[index + 1] >>> dst.bits + k << dst.bits + k | v >> k;
			}
			else dst.array[index] = i << k >>> k | v << bit | i >>> bit + dst.bits << bit + dst.bits;
			
			return true;
		}
		
		protected static boolean add( R dst, int value ) {
			long v = value & dst.mask;
			
			final int item = dst.bits * dst.size++;
			if (dst.array.length * BITS < item + dst.bits) dst.array = item == 0 ? new long[4] : Arrays.copyOf( dst.array, dst.array.length + dst.array.length / 2 );
			
			final int index = item >>> LEN;
			final int bit   = item & MASK;
			
			if (bit == 0) dst.array[index] = v;
			else
			{
				dst.array[index] |= v << bit;
				if (BITS < bit + dst.bits) dst.array[index + 1] = v >> BITS - bit;
			}
			
			return true;
		}
		
		protected static void add( R dst, int item, int value ) {
			
			if (dst.size <= item)
			{
				set( dst, item, value );
				return;
			}
			
			long v = value & dst.mask;
			
			int index = item * dst.bits >>> LEN;
			dst.size++;
			
			final long[] src  = dst.array;
			long[]       dst_ = dst.array;
			
			if (dst.array.length * BITS < dst.size * dst.bits)
			{
				dst_ = dst.array = new long[dst.array.length + dst.array.length / 2];
				if (0 < index) System.arraycopy( src, 0, dst, 0, index );
			}
			
			final int bit = item * dst.bits & MASK;
			if (0 < bit)
			{
				final long i = src[index];
				final int  k = BITS - bit;
				if (bit + dst.bits < BITS)
				{
					dst_[index] = i << k >>> k | v << bit | i >>> bit << bit + dst.bits;
					v           = i >>> bit + dst.bits | src[index + 1] << k - dst.bits & dst.mask;
				}
				else
				{
					dst_[index] = i << k >>> k | v << bit;
					v           = v >> k | i >> bit << dst.bits - k;
				}
				index++;
			}
			
			for (final int max = (dst.size * dst.bits >> LEN) + ((dst.size * dst.bits & MASK) == 0 ? 0 : 1); ; )
			{
				final long i = src[index];
				dst_[index] = i << dst.bits | v;
				if (max < ++index) break;
				v = i >>> BITS - dst.bits;
			}
		}
		
		protected static void remove( R dst, int value ) {
			for (int i; -1 < (i = dst.lastIndexOf( value )); )
			     removeAt( dst, i );
		}
		
		protected static void removeAt( R dst, int item ) {
			
			
			int       index = (item *= dst.bits) >>> LEN;
			final int bit   = item & MASK;
			
			final int k = BITS - bit;
			long      i = dst.array[index];
			
			if (bit == 0) dst.array[index] = i >>>= dst.bits;
			else if (k < dst.bits)
			{
				long ii = dst.array[index + 1];
				
				dst.array[index]   = i << k >>> k | ii >>> bit + dst.bits - BITS << bit;
				dst.array[++index] = i = ii >>> dst.bits;
			}
			else if (dst.bits < k)
			{
				long ii = dst.array[index + 1];
				
				dst.array[index]   = i << k >>> k | i >>> bit + dst.bits << bit | ii << BITS - dst.bits;
				dst.array[++index] = i = ii >>> dst.bits;
			}
			
			for (final int max = dst.size * dst.bits; index * BITS < max; )
			{
				long ii = dst.array[index + 1];
				dst.array[index]   = i << dst.bits >>> dst.bits | ii << BITS - dst.bits;
				dst.array[++index] = i = ii >>> dst.bits;
			}
			
			dst.size--;
		}
		
		protected static boolean addAll( R dst, IntList.Producer src ) {
			int fix = dst.size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) add( dst, src.value( tag ) );
			return dst.size != fix;
		}
		
		protected static int addAll( R dst, int from, IntList.Producer src ) {
			int fix = dst.size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) add( dst, from++, src.value( tag ) );
			return dst.size - fix;
		}
		
		protected static int removeAll( R dst, IntList.Producer src ) {
			
			int fix = dst.size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) R.remove( dst, src.value( tag ) );
			return fix - dst.size;
		}
		
		protected static boolean retainAll( R dst, IntList.Consumer chk ) {
			final int fix = dst.size;
			
			for (int item = 0, v; item < dst.size; item++)
				if (!chk.add( v = dst.get( item ) )) R.remove( dst, v );
			
			return fix != dst.size;
		}
		
		protected static void clear( R dst ) {
			for (int i = ((dst.bits * dst.size) >> LEN) + 1; -1 < --i; )
			     dst.array[i] = 0;
			
			dst.size = 0;
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
		
		public boolean set( int value ) {
			if (array.length <= size) return false;
			return set( this, size, value );
		}
		
		public boolean set( int item, int value ) {
			if (array.length <= item) return false;
			return set( this, item, value );
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
			return add( this, value );
		}
		
		public void add( int item, int value ) {
			add( this, item, value );
		}
		
		public void remove( int value ) {
			remove( this, value );
		}
		
		public void removeAt( int item ) {
			removeAt( this, item );
		}
		
		public boolean addAll( IntList.Producer src ) {
			return addAll( this, src );
		}
		
		public int addAll( int from, IntList.Producer src ) {
			return addAll( this, from, src );
		}
		
		public int removeAll( IntList.Producer src ) {
			return removeAll( this, src );
		}
		
		public boolean set( int item, int value ) {
			return set( this, item, value );
		}
		
		public boolean retainAll( IntList.Consumer chk ) {
			return retainAll( this, chk );
		}
		
		public void clear() {
			clear( this );
		}
		
		public IntList.Consumer consumer() {return this; }
		
		public RW clone()                  { return (RW) super.clone(); }
	}
}
