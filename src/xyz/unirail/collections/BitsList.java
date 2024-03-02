package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

import java.util.Arrays;

public interface BitsList {
	
	static long mask( int bits )                                            { return (1L << bits) - 1; }
	
	static int size( int src )                                              { return src >>> 3; }
	
	static int index( int item_X_bits )                                     { return item_X_bits >> LEN; }
	
	static int bit( int item_X_bits )                                       { return item_X_bits & MASK; }
	
	static byte value( long src, int bit, long mask )                       { return (byte) (src >>> bit & mask); }
	
	static byte value( long prev, long next, int bit, int bits, long mask ) { return (byte) (((next & mask( bit + bits - BITS )) << BITS - bit | prev >>> bit) & mask); }
	
	int BITS = 64;
	int MASK = BITS - 1;
	int LEN  = 6;
	
	static int len4bits( int bits ) { return (bits + BITS) >>> LEN; }
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		protected long[] values = Array.EqualHashOf.longs.O;
		protected int    size   = 0;
		public int size() { return size; }
		
		protected final long mask;
		public final    int  bits;
		public final    int  default_value;
		
		protected R( int bits_per_item ) {
			mask          = mask( bits = bits_per_item );
			default_value = 0;
		}
		
		protected R( int bits_per_item, int length ) {
			mask          = mask( bits = bits_per_item );
			values        = new long[len4bits( length * bits )];
			default_value = 0;
		}
		protected R( int bits_per_item, int default_value, int size ) {
			mask               = mask( bits = bits_per_item );
			values             = new long[len4bits( size * bits )];
			this.size = size;
			if( (this.default_value = default_value & 0xFF) != 0 )
				for( int i = 0; i < size; i++ ) append( this, i, default_value );
		}
		
		public int length() { return values.length * BITS / bits; }
		
		//if 0 < items - fit storage space according `items` param
		//if items < 0 - cleanup and allocate spase
		protected void length_( int items ) {
			if( 0 < items )
			{
				if( items < size ) size = items;
				final int new_values_length = len4bits( items * bits );
				
				values = new_values_length == 0 ? Array.EqualHashOf.longs.O : Array.copyOf( values, new_values_length );
				return;
			}
			
			
			final int new_values_length = len4bits( -items * bits );
			
			if( values.length != new_values_length )
			{
				values = new_values_length == 0 ? Array.EqualHashOf.longs.O : new long[new_values_length];
				if( default_value == 0 )
				{
					size = 0;
					return;
				}
			}
			
			clear();
		}
		
		protected void clear() {
			if( default_value == 0 )//can do it fast
				Arrays.fill( values, 0, Math.min( index( bits * size ), values.length - 1 ), 0 );
			size = 0;
		}
		
		
		public boolean isEmpty() { return size == 0; }
		
		
		public int hashCode() {
			int i = index( size );
			
			int hash = Array.hash( 149989999, (values[i] & (1L << bit( size )) - 1) );
			
			while( -1 < --i ) hash = Array.hash( hash, values[i] );
			
			return Array.finalizeHash( hash, size() );
		}
		
		public boolean equals( Object other ) {
			return other != null &&
			       getClass() == other.getClass() &&
			       equals( getClass().cast( other ) );
		}
		
		public boolean equals( R other ) {
			if( other == null || other.size != size ) return false;
			
			int i = index( size );
			
			long mask = (1L << bit( size )) - 1;
			if( (values[i] & mask) != (other.values[i] & mask) ) return false;
			
			while( -1 < --i )
				if( values[i] != other.values[i] ) return false;
			
			return true;
		}
		
		
		public byte get() { return get( size - 1 ); }
		public byte get( int item ) {
			final int index = index( item *= bits );
			final int bit   = bit( item );
			
			return BITS < bit + bits ? value( values[index], values[index + 1], bit, bits, mask ) : value( values[index], bit, mask );
		}
		
		protected static void add( R dst, long src ) { set1( dst, dst.size, src ); }
		protected static void add( R dst, int item, long value ) {
			
			if( dst.size == item )
			{
				append( dst, item, value );
				return;
			}
			if( dst.size < item )
			{
				set1( dst, item, value );
				return;
			}
			
			int p = item * dst.bits;
			item = index( p );
			
			final long[] src  = dst.values;
			long[]       dst_ = dst.values;
			if( dst.length() * BITS < p ) dst.length_( Math.max( dst.length() + dst.length() / 2, len4bits( p ) ) );
			long      v   = value & dst.mask;
			final int bit = bit( p );
			if( 0 < bit )
			{
				final long i = src[item];
				final int  k = BITS - bit;
				if( k < dst.bits )
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
			
			for( final int max = len4bits( dst.size * dst.bits ); ; )
			{
				final long i = src[item];
				dst_[item] = i << dst.bits | v;
				if( max < ++item ) break;
				v = i >>> BITS - dst.bits;
			}
		}
		
		
		protected static void set( R dst, int from, byte... src )  { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		
		protected static void set( R dst, int from, char... src )  { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		
		protected static void set( R dst, int from, short... src ) { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		
		protected static void set( R dst, int from, int... src )   { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		
		protected static void set( R dst, int from, long... src )  { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		
		protected static void set1( R dst, int item, long src ) {
			final int total_bits = item * dst.bits;
			
			if( item < dst.size )
			{
				final int
						index = index( total_bits ),
						bit = bit( total_bits ),
						k = BITS - bit;
				
				final long i = dst.values[index], v = src & dst.mask;
				if( k < dst.bits )
				{
					dst.values[index]     = i << k >>> k | v << bit;
					dst.values[index + 1] = dst.values[index + 1] >>> dst.bits - k << dst.bits - k | v >> k;
				}
				else dst.values[index] = ~(~0L >>> BITS - dst.bits << bit) & i | v << bit;
				return;
			}
			
			if( dst.length() <= item ) dst.length_( Math.max( dst.length() + dst.length() / 2, len4bits( total_bits + dst.bits ) ) );
			
			if( dst.default_value != 0 )
				for( int i = dst.size; i < item; i++ ) append( dst, i, dst.default_value );
			
			append( dst, item, src );
			
			dst.size = item + 1;
		}
		
		private static void append( R dst, int item, long src ) {
			final long v = src & dst.mask;
			final int
					p = item * dst.bits,
					index = index( p ),
					bit = bit( p );
			
			final int  k = BITS - bit;
			final long i = dst.values[index];
			
			if( k < dst.bits )
			{
				dst.values[index]     = i << k >>> k | v << bit;
				dst.values[index + 1] = v >> k;
			}
			else
				dst.values[index] = ~(~0L << bit) & i | v << bit;
		}
		
		
		protected static void removeAt( R dst, int item ) {
			
			if( item + 1 == dst.size )
			{
				if( dst.default_value == 0 ) append( dst, item, 0 );//zeroed place
				dst.size--;
				return;
			}
			
			int       index = index( item *= dst.bits );
			final int bit   = bit( item );
			
			final int k = BITS - bit;
			long      i = dst.values[index];
			
			if( index + 1 == dst.length() )
			{
				if( bit == 0 ) dst.values[index] = i >>> dst.bits;
				else if( k < dst.bits ) dst.values[index] = i << k >>> k;
				else if( dst.bits < k ) dst.values[index] = i << k >>> k | i >>> bit + dst.bits << bit;
				
				dst.size--;
				return;
			}
			
			if( bit == 0 ) dst.values[index] = i >>>= dst.bits;
			else if( k < dst.bits )
			{
				long ii = dst.values[index + 1];
				
				dst.values[index]   = i << k >>> k | ii >>> bit + dst.bits - BITS << bit;
				dst.values[++index] = i = ii >>> dst.bits;
			}
			else if( dst.bits < k )
				if( index + 1 == dst.values.length )
				{
					dst.values[index] = i << k >>> k | i >>> bit + dst.bits << bit;
					dst.size--;
					return;
				}
				else
				{
					long ii = dst.values[index + 1];
					
					dst.values[index]   = i << k >>> k | i >>> bit + dst.bits << bit | ii << BITS - dst.bits;
					dst.values[++index] = i = ii >>> dst.bits;
				}
			
			for( final int max = dst.size * dst.bits >>> LEN; index < max; )
			{
				long ii = dst.values[index + 1];
				dst.values[index]   = i << dst.bits >>> dst.bits | ii << BITS - dst.bits;
				dst.values[++index] = i = ii >>> dst.bits;
			}
			
			dst.size--;
		}
		
		
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				if( 0 < dst.length() ) dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e ) { e.printStackTrace(); }
			return null;
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterArray();
			
			if( 0 < size )
			{
				json.preallocate( size * 4 );
				long src = values[0];
				
				for( int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++ )
				{
					final int bit   = bit( bp );
					long      value = (BITS < bit + bits ? value( src, src = values[index( bp ) + 1], bit, bits, mask ) : value( src, bit, mask ));
					json.value( value );
				}
			}
			json.exitArray();
		}
		
		
		public int indexOf( long value ) {
			for( int item = 0, max = size * bits; item < max; item += bits )
				if( value == get( item ) ) return item / bits;
			
			return -1;
		}
		
		public int lastIndexOf( long value ) { return lastIndexOf( size, value ); }
		
		public int lastIndexOf( int from, long value ) {
			for( int i = Math.min( from, size ); -1 < --i; )
				if( value == get( i ) ) return i;
			
			return -1;
		}
		
		protected static void remove( R dst, long value ) {
			for( int i = dst.size; -1 < (i = dst.lastIndexOf( i, value )); )
			     removeAt( dst, i );
		}
		
		public boolean contains( long value ) { return -1 < indexOf( value ); }
		
		public byte[] toArray( byte[] dst ) {
			if( size == 0 ) return null;
			if( dst == null || dst.length < size ) dst = new byte[size];
			
			for( int item = 0, max = size * bits; item < max; item += bits )
			     dst[item / bits] = (byte) get( item );
			
			return dst;
		}
	}
	
	interface Interface {
		int size();
		
		byte get( int item );
		
		RW set1( int item, int value );
		
		RW add1( long value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int bits_per_item )                               { super( bits_per_item ); }
		
		public RW( int bits_per_item, int length )                   { super( bits_per_item, length ); }
		
		public RW( int bits_per_item, int defaultValue, int size ) { super( bits_per_item, defaultValue, size ); }
		
		
		public RW add1( long value ) {
			add( this, value );
			return this;
		}
		
		public RW add1( int index, long src ) {
			add( this, index, src );
			return this;
		}
		
		public RW remove( long value ) {
			remove( this, value );
			return this;
		}
		
		public RW removeAt( int item ) {
			removeAt( this, item );
			return this;
		}
		public RW remove() {
			removeAt( this, size - 1 );
			return this;
		}
		
		public RW set1( int value ) {
			set1( this, size - 1, value );
			return this;
		}
		
		public RW set1( char value ) {
			set1( this, size - 1, value );
			return this;
		}
		
		public RW set1( int item, int value ) {
			set1( this, item, value );
			return this;
		}
		
		public RW set1( int item, char value ) {
			set1( this, item, value );
			return this;
		}
		
		public RW set( int index, byte... values ) {
			set( this, index, values );
			return this;
		}
		
		public RW set( int index, byte[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, src[src_index + i] );
			return this;
		}

		public RW set( int index, char... values ) {
			set( this, index, values );
			return this;
		}
		public RW set( int index, short... values ) {
			set( this, index, values );
			return this;
		}
		public RW set( int index, int... values ) {
			set( this, index, values );
			return this;
		}
		public RW set( int index, long... values ) {
			set( this, index, values );
			return this;
		}
		
		public boolean retainAll( R chk ) {
			final int fix = size;
			
			int v;
			for( int item = 0; item < size; item++ )
				if( !chk.contains( v = get( item ) ) ) remove( this, v );
			
			return fix != size;
		}
		
		public RW fit() {
			length_( -size );
			return this;
		}
		
		public RW length( int items ) {
			if( items < 1 ) values = Array.EqualHashOf.longs.O;
			else length_( -items );
			return this;
		}
		
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( this.size < size ) set1( size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
	
}
