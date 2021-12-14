package org.unirail.collections;


import org.unirail.Hash;

import java.util.Arrays;

public interface BitsList {
	
	
	long FFFFFFFFFFFFFFFF = ~0L;
	
	static long mask( int bits )                                 {return (1L << bits) - 1;}
	
	static int size( int src )                                   {return src >>> 3;}
	
	static int index( int item_X_bits )                          {return item_X_bits >> LEN;}
	
	static int bit( int item_X_bits )                            {return item_X_bits & MASK;}
	
	static long value( long src, int bit, int bits )             {return src >>> bit & mask( bits );}
	
	static long value( long prev, long next, int bit, int bits ) {return ((next & mask( bit + bits - BITS )) << BITS - bit | prev >>> bit) & mask( bits );}
	
	int BITS = 64;
	int MASK = BITS - 1;
	int LEN  = 6;
	
	static int len4bits( int bits ) {return (bits + BITS) >>> LEN;}
	
	
	abstract class R implements Cloneable, Comparable<R> {
		
		protected long[] array = Array.longs0;
		protected int    size  = 0;
		
		protected void length( int items ) {
			if (0 < items)
			{
				if (items < size) size = items;
				
				array = Arrays.copyOf( array, len4bits( items * bits ) );
				return;
			}
			
			size  = 0;
			array = items == 0 ? Array.longs0 : new long[len4bits( -items * bits )];
		}
		
		public int size()        {return size;}
		
		public boolean isEmpty() {return size == 0;}
		
		protected final long mask;
		public final    int  bits;
		
		public int bits()                {return bits;}
		
		
		protected R( int bits_per_item ) {mask = mask( bits = bits_per_item );}
		
		protected R( int bits_per_item, int items ) {
			mask  = mask( bits = bits_per_item );
			array = new long[len4bits( items * bits )];
		}
		
		protected R( int bits_per_item, int fill_value, int size ) {
			this( bits_per_item, size );
			this.size = size;
			if ((fill_value & 0xFF) == 0) return;
			while (-1 < --size) set( this, size, fill_value );
		}
		
		protected void clear() {
			for (int i = index( bits * size ) + 1; -1 < --i; ) array[i] = 0;
			size = 0;
		}
		
		public int hashCode() {
			int i = index( size );
			
			int hash = Hash.code( 149989999 , (array[i] & (1L << bit( size )) - 1) );
			
			while (-1 < --i) hash = Hash.code( hash, array[i] );
			
			return hash;
		}
		
		public boolean equals( Object other ) {
			return other != null &&
			       getClass() == other.getClass() &&
			       equals( getClass().cast( other ) );
		}
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		@Override public int compareTo( R other ) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			int i = index( size );
			
			long m = (1L << bit( size )) - 1;
			if ((array[i] & m) != (other.array[i] & m)) return 2;
			
			while (-1 < --i)
				if (array[i] != other.array[i]) return (int) (array[i] - other.array[i]);
			
			return 0;
		}
		
		
		public int length()                          {return array.length * BITS / bits;}
		
		
		protected static void add( R dst, long src ) {set( dst, dst.size, src );}
		
		protected static void add( R dst, int item, long value ) {
			
			if (dst.size <= item)
			{
				set( dst, item, value );
				return;
			}
			
			int p = item * dst.bits;
			
			item = index( p );
			
			final long[] src  = dst.array;
			long[]       dst_ = dst.array;
			
			if (dst.length() * BITS < p) dst.length( Math.max( dst.length() + dst.length() / 2, len4bits( p ) ) );
			
			long      v   = value & dst.mask;
			final int bit = bit( p );
			if (0 < bit)
			{
				final long i = src[item];
				final int  k = BITS - bit;
				if (k < dst.bits)
				{
					dst_[item] = i << k >>> k | v << bit;
					v          = v >> k | i >> bit << dst.bits - k;
				}
				else
				{
					dst_[item] = i << k >>> k | v << bit | i >>> bit << bit + dst.bits;
					v          = i >>> bit + dst.bits | src[item + 1] << k - dst.bits & dst.mask;
				}
				item++;
			}
			
			dst.size++;
			
			for (final int max = len4bits( dst.size * dst.bits ); ; )
			{
				final long i = src[item];
				dst_[item] = i << dst.bits | v;
				if (max < ++item) break;
				v = i >>> BITS - dst.bits;
			}
		}
		
		public int get( int item ) {
			final int index = index( item *= bits );
			final int bit   = bit( item );
			
			return (int) (BITS < bit + bits ? value( array[index], array[index + 1], bit, bits ) : value( array[index], bit, bits ));
		}
		
		protected static void set( R dst, int from, byte... src )  {for (int i = src.length; -1 < --i; ) set( dst, from + i, src[i] );}
		
		protected static void set( R dst, int from, char... src )  {for (int i = src.length; -1 < --i; ) set( dst, from + i, src[i] );}
		
		protected static void set( R dst, int from, short... src ) {for (int i = src.length; -1 < --i; ) set( dst, from + i, src[i] );}
		
		protected static void set( R dst, int from, int... src )   {for (int i = src.length; -1 < --i; ) set( dst, from + i, src[i] );}
		
		protected static void set( R dst, int from, long... src )  {for (int i = src.length; -1 < --i; ) set( dst, from + i, src[i] );}
		
		protected static void set( R dst, int item, long src ) {
			final long v = src & dst.mask;
			final int
					p = item * dst.bits,
					index = index( p ),
					bit = bit( p );
			
			final int  k = BITS - bit;
			final long i = dst.array[index];
			
			if (item < dst.size)
			{
				if (k < dst.bits)
				{
					dst.array[index]     = i << k >>> k | v << bit;
					dst.array[index + 1] = dst.array[index + 1] >>> dst.bits - k << dst.bits - k | v >> k;
				}
				else dst.array[index] = ~(~0L >>> BITS - dst.bits << bit) & i | v << bit;
				return;
			}
			
			if (dst.length() <= item) dst.length( Math.max( dst.length() + dst.length() / 2, len4bits( p + dst.bits ) ) );
			
			if (k < dst.bits)
			{
				dst.array[index]     = i << k >>> k | v << bit;
				dst.array[index + 1] = v >> k;
			}
			else dst.array[index] = ~(~0L << bit) & i | v << bit;
			
			dst.size = item + 1;
		}
		
		
		protected static void removeAt( R dst, int item ) {
			
			if (item + 1 == dst.size)
			{
				dst.size--;
				return;
			}
			
			int       index = index( item *= dst.bits );
			final int bit   = bit( item );
			
			final int k = BITS - bit;
			long      i = dst.array[index];
			
			if (index + 1 == dst.length())
			{
				if (bit == 0) dst.array[index] = i >>> dst.bits;
				else if (k < dst.bits) dst.array[index] = i << k >>> k;
				else if (dst.bits < k) dst.array[index] = i << k >>> k | i >>> bit + dst.bits << bit;
				
				dst.size--;
				return;
			}
			
			if (bit == 0) dst.array[index] = i >>>= dst.bits;
			else if (k < dst.bits)
			{
				long ii = dst.array[index + 1];
				
				dst.array[index]   = i << k >>> k | ii >>> bit + dst.bits - BITS << bit;
				dst.array[++index] = i = ii >>> dst.bits;
			}
			else if (dst.bits < k)
				if (index + 1 == dst.array.length)
				{
					dst.array[index] = i << k >>> k | i >>> bit + dst.bits << bit;
					dst.size--;
					return;
				}
				else
				{
					long ii = dst.array[index + 1];
					
					dst.array[index]   = i << k >>> k | i >>> bit + dst.bits << bit | ii << BITS - dst.bits;
					dst.array[++index] = i = ii >>> dst.bits;
				}
			
			int f = index;
			for (final int max = dst.size * dst.bits >>> LEN; index < max; )
			{
				long ii = dst.array[index + 1];
				dst.array[index]   = i << dst.bits >>> dst.bits | ii << BITS - dst.bits;
				dst.array[++index] = i = ii >>> dst.bits;
			}
			
			
			dst.size--;
		}
		
		
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				if (0 < dst.length()) dst.array = array.clone();
				return dst;
				
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			return null;
		}
		
		public String toString() {return toString( null ).toString();}
		
		StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( size * 4 );
			else dst.ensureCapacity( dst.length() + size * 4 );
			
			long src = array[0];
			for (int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++)
			{
				final int bit   = bit( bp );
				long      value = (BITS < bit + bits ? value( src, src = array[index( bp ) + 1], bit, bits ) : src >>> bit & mask);
				
				dst.append( value ).append( '\t' );
				
				if (i % 10 == 0) dst.append( '\t' ).append( i / 10 * 10 ).append( '\n' );
			}
			
			return dst;
		}
		
		public int indexOf( long value ) {
			for (int item = 0, max = size * bits; item < max; item += bits)
				if (value == get( item )) return item / bits;
			
			return -1;
		}
		
		public int lastIndexOf( long value ) {return lastIndexOf( size, value );}
		
		public int lastIndexOf( int from, long value ) {
			for (int i = Math.max( from, size ); -1 < --i; )
				if (value == get( i )) return i;
			
			return -1;
		}
		
		protected static void remove( R dst, long value ) {
			for (int i = dst.size; -1 < (i = dst.lastIndexOf( i, value )); )
			     removeAt( dst, i );
		}
		
		public boolean contains( long value ) {return -1 < indexOf( value );}
		
		public byte[] toArray( byte[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new byte[size];
			
			for (int item = 0, max = size * bits; item < max; item += bits)
			     dst[item / bits] = (byte) get( item );
			
			return dst;
		}
	}
	
	
	class RW extends R {
		
		public RW( int bits_per_item )                            {super( bits_per_item );}
		
		
		public RW( int bits_per_item, int items )                 {super( bits_per_item, items );}
		
		
		public RW( int bits_per_item, int fill_value, int items ) {super( bits_per_item, fill_value, items );}
		
		public RW( int bits_per_item, byte... values ) {
			super( bits_per_item, values.length );
			set( this, 0, values );
		}
		
		
		public RW( int bits_per_item, char... values ) {
			super( bits_per_item, values.length );
			set( this, 0, values );
		}
		
		
		public RW( int bits_per_item, short... values ) {
			super( bits_per_item, values.length );
			set( this, 0, values );
		}
		
		
		public RW( int bits_per_item, int... values ) {
			super( bits_per_item, values.length );
			set( this, 0, values );
		}
		
		
		public RW( int bits_per_item, long... values ) {
			super( bits_per_item, values.length );
			set( this, 0, values );
		}
		
		
		public void add( long value )                {add( this, value );}
		
		public void add( int index, long src )       {add( this, index, src );}
		
		public void remove( long value )             {remove( this, value );}
		
		public void removeAt( int item )             {removeAt( this, item );}
		
		public void set( int value )                 {set( this, size, value );}
		
		public void set( char value )                {set( this, size, value );}
		
		public void set( int item, int value )       {set( this, item, value );}
		
		public void set( int item, char value )      {set( this, item, value );}
		
		public void set( int index, char... values ) {set( this, index, values );}
		
		public boolean retainAll( R chk ) {
			final int fix = size;
			
			int v;
			for (int item = 0; item < size; item++)
				if (!chk.contains( v = get( item ) )) remove( this, v );
			
			return fix != size;
		}
		
		public void clear()         {super.clear();}
		
		public void fit()           {length( -size );}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
	
}
