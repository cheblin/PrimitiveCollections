package org.unirail.collections;


import java.util.Arrays;

public interface BitsList {
	
	interface Consumer {
		default boolean add( int value ) {return add( (char) value );}
		
		boolean add( char value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return -1 < tag;}
		
		char value( int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( value( tag ) ).append( '\n' );
			return dst;
		}
	}
	
	class Base implements Cloneable, Comparable<Base> {
		
		static final int BITS = 64;
		static final int MASK = BITS - 1;
		static final int LEN  = 6;
		
		protected long[] array;
		protected int    size = 0;
		
		
		public final int bits;
		public final int mask;
		
		public Base( int bits_per_item ) {
			bits  = bits_per_item;
			mask  = (1 << bits_per_item) - 1;
			array = new long[1];
		}
		
		public Base( int bits_per_item, int items ) {
			bits  = bits_per_item;
			mask  = (1 << bits_per_item) - 1;
			array = new long[1 + (items >>> LEN)];
		}
		
		
		protected static void set( Base dst, int from, char... src ) {
			
			final int bits = dst.bits;
			for (int i = 0, max = src.length; i < max; i++)
			{
				final int item  = bits * dst.size++;
				final int index = from + (item >>> LEN);
				final int bit   = item & MASK;
				
				final long v = src[i] & dst.mask;
				
				dst.array[index] |= v << bit;
				if (BITS < bit + bits) dst.array[index + 1] = v >> BITS - bit;
			}
		}
		
		protected static void set( Base dst, int from, int... src ) {
			
			final int bits = dst.bits;
			for (int i = 0, max = src.length; i < max; i++)
			{
				final int item  = bits * dst.size++;
				final int index = from + (item >>> LEN);
				final int bit   = item & MASK;
				
				final long v = src[i] & dst.mask;
				
				dst.array[index] |= v << bit;
				if (BITS < bit + bits) dst.array[index + 1] = v >> BITS - bit;
			}
		}
		
		public int length()                   { return array.length * BITS / bits; }
		
		public int size()                     { return size; }
		
		public boolean isEmpty()              { return size == 0; }
		
		public boolean contains( int value )  { return -1 < indexOf( value ); }
		
		public boolean contains( char value ) { return -1 < indexOf( value ); }
		
		
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
		
		
		public char get( int item ) {
			int       index = (item *= bits) >>> LEN;
			final int bit   = item & MASK;
			
			return (char) (BITS < bit + bits ?
			               array[index] >>> bit | (array[index + 1] & (1L << bit + bits - BITS) - 1) << BITS - bit :
			               array[index] >>> bit & mask);
		}
		
		
		public int indexOf( char value ) { return indexOf( (int) value ); }
		
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
		
		public int lastIndexOf( char value ) {return lastIndexOf( (int) value ); }
		
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
		
		
		public int compareTo( Base other ) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			int i = size >>> LEN;
			
			int m = (1 << (size & MASK)) - 1;
			if ((array[i] & m) != (other.array[i] & m)) return 2;
			
			while (-1 < --i)
				if (array[i] != other.array[i]) return (int) (array[i] - other.array[i]);
			
			return 0;
		}
		
		
		public boolean containsAll( Producer src ) {
			return false;
		}
		
		
		public Base subList( int fromIndex, int toIndex ) {
			return null;
		}
		
		protected static boolean set( Base dst, int item, char value ) {return set( dst, item, (int) value );}
		
		protected static boolean set( Base dst, int item, int value ) {
			
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
		
		protected static boolean add( Base dst, char value ) {return add( dst, (int) value );}
		
		protected static boolean add( Base dst, int value ) {
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
		
		protected static void add( Base dst, int item, char value ) { add( dst, item, (int) value );}
		
		protected static void add( Base dst, int item, int value ) {
			
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
		
		protected static void remove( Base dst, char value ) {remove( dst, (int) value );}
		
		protected static void remove( Base dst, int value ) {
			for (int i; -1 < (i = dst.lastIndexOf( value )); )
			     removeAt( dst, i );
		}
		
		protected static void removeAt( Base dst, int item ) {
			
			
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
		
		protected static boolean addAll( Base dst, Producer src ) {
			int fix = dst.size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) add( dst, src.value( tag ) );
			return dst.size != fix;
		}
		
		protected static int addAll( Base dst, int from, Producer src ) {
			int fix = dst.size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) add( dst, from++, src.value( tag ) );
			return dst.size - fix;
		}
		
		protected static int removeAll( Base dst, Producer src ) {
			
			int fix = dst.size;
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) Base.remove( dst, src.value( tag ) );
			return fix - dst.size;
		}
		
		protected static boolean retainAll( Base dst, Consumer chk ) {
			final int fix = dst.size;
			
			for (int item = 0, v; item < dst.size; item++)
				if (!chk.add( v = dst.get( item ) )) Base.remove( dst, (char) v );
			
			return fix != dst.size;
		}
		
		protected static void clear( Base dst ) {
			for (int i = ((dst.bits * dst.size) >> LEN) + 1; -1 < --i; )
			     dst.array[i] = 0;
			
			dst.size = 0;
		}
		
		
		public Base clone() {
			try
			{
				Base dst = (Base) super.clone();
				dst.array = array.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		
	}
	
	class R extends Base {
		
		public R( int bits_per_item ) {
			super( bits_per_item );
		}
		
		public R( int bits_per_item, int items ) {
			super( bits_per_item, items );
		}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? 0 : -1; }
				
				public int tag( int tag ) { return ++tag < size ? tag : -1; }
				
				public char value( int tag ) {return R.this.get( tag );}
				
			} : producer;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( size * 2 );
			else dst.ensureCapacity( dst.length() + size * 2 );
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class Rsize extends R {
		public Rsize( int bits_per_item, int items ) {
			super( bits_per_item, items );
			size = items;
		}
		
		public void set( int index, char... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, (char) values[i] );
		}
		
		public void set( int index, int... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, (char) values[i] );
		}
		
		public void set( int item, int value )  { if (item < size) set( this, item, value ); }
		
		public void set( int item, char value ) {if (item < size) set( this, item, value ); }
		
		public static Rsize of( int bits_per_item, char... values ) {
			Rsize dst = new Rsize( bits_per_item, values.length );
			set( dst, 0, values );
			return dst;
		}
		
		public static Rsize of( int bits_per_item, int... values ) {
			Rsize dst = new Rsize( bits_per_item, values.length );
			set( dst, 0, values );
			return dst;
		}
	}
	
	class RW extends R implements Consumer {
		
		public RW( int bits_per_item )            { super( bits_per_item, 1 ); }
		
		public RW( int bits_per_item, int items ) { super( bits_per_item, items ); }
		
		public static RW of( int bits_per_item, char... values ) {
			RW dst = new RW( bits_per_item, values.length );
			set( dst, 0, values );
			return dst;
		}
		
		public static RW of( int bits_per_item, int... values ) {
			RW dst = new RW( bits_per_item, values.length );
			dst.set( 0, values );
			return dst;
		}
		
		
		public boolean add( int value )              { return add( this, value ); }
		
		public boolean add( char value )             { return add( this, value ); }
		
		public void add( int item, int value )       { add( this, item, value ); }
		
		public void add( int item, char value )      { add( this, item, value ); }
		
		public void remove( int value )              { remove( this, value ); }
		
		public void remove( char value )             { remove( this, value ); }
		
		public void removeAt( int item )             { removeAt( this, item ); }
		
		public boolean addAll( Producer src )        { return addAll( this, src ); }
		
		public int addAll( int from, Producer src )  { return addAll( this, from, src ); }
		
		public int removeAll( Producer src )         { return removeAll( this, src ); }
		
		public void set( int value )                 { set( this, size, value ); }
		
		public void set( char value )                { set( this, size, value ); }
		
		public void set( int item, int value )       { set( this, item, value ); }
		
		public void set( int item, char value )      { set( this, item, value ); }
		
		public void set( int index, int... values )  { set( this, index, values ); }
		
		public void set( int index, char... values ) { set( this, index, values ); }
		
		public boolean retainAll( Consumer chk )     { return retainAll( this, chk ); }
		
		public void clear()                          { clear( this ); }
		
		public Consumer consumer()                   {return this; }
		
		public RW clone()                            { return (RW) super.clone(); }
	}
}
