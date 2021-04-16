package org.unirail.collections;


public interface BitsNullList {
	
	interface Consumer {
		default boolean add( int value ) {return add( (char) value );}
		
		boolean add( char value );
		
		boolean add( Integer value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag )       {return tag != -1;}
		
		char value( int tag );
		
		default boolean hasValue( int tag ) { return -1 < tag; }
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); dst.append( '\n' ), tag = tag( tag ))
				if (hasValue( tag )) dst.append( value( tag ) );
				else dst.append( "null" );
			return dst;
		}
	}
	
	class R extends BitsList.Base {
		
		protected final char null_val;
		
		public R( char null_val, int bits_per_item ) {
			super( bits_per_item );
			this.null_val = null_val;
		}
		
		public R( char null_val, int bits_per_item, int items ) {
			super( bits_per_item, Math.abs( items ) );
			this.null_val = null_val;
			
			if (0 < items && null_val != 0) nulls( this, 0, items );
		}
		
		
		public static R oF( char null_val, int bits_per_item, int... values ) {
			R dst = new R( null_val, bits_per_item, -values.length );
			fill( dst, values );
			return dst;
		}
		
		protected static void fill( R dst, int... items ) {
			
			final int bits = dst.bits;
			for (int i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (long) i << bit;
			}
		}
		
		public static R of( char null_val, int bits_per_item, Integer... values ) {
			R dst = new R( null_val, bits_per_item, -values.length );
			filL( dst, values );
			return dst;
		}
		
		protected static void filL( R dst, Integer... items ) {
			
			final int bits = dst.bits;
			for (Integer i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (long) (i == null ? dst.null_val : i) << bit;
			}
		}
		
		protected static void nulls( R dst, int from, int upto ) {
			
			final long null_val = dst.null_val;
			
			for (int bits = dst.bits; from < upto; )
			{
				final int item  = bits * from++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= null_val << bit;
			}
		}
		
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? R.this.tag( 0 ) : -1; }
				
				public int tag( int tag ) { return (tag &= Integer.MAX_VALUE) < size - 1 ? R.this.tag( tag + 1 ) : -1; }
				
				public char value( int tag ) {return get( tag ); }
				
			} : producer;
		}
		
		public int tag( int index )        {return super.get( index ) == null_val ? (Integer.MIN_VALUE | index) : index;}
		
		public char get( int tag )         {return super.get( tag ); }
		
		public boolean hasValue( int tag ) { return -1 < tag; }
		
		public R clone()                   {return (R) super.clone();}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( size * 2 );
			else dst.ensureCapacity( dst.length() + size * 2 );
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class Rsize extends R {
		
		public Rsize( char null_val, int bits_per_item, int items ) {
			super( null_val, bits_per_item, items );
			size = Math.abs( items );
		}
		
		public void set( int item, int value )     { if (item < size) set( this, item, value ); }
		
		public void set( int item, char value )    { if (item < size) set( this, item, value ); }
		
		public void set( int item, Integer value ) { if (item < size) set( this, item, value == null ? null_val : (char) (value & 0xFFFF) ); }
		
		public void seT( int index, int... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, (char) values[i] );
		}
		
		public void set( int index, Integer... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, values[i] == null ? null_val : (char) (values[i] & 0xFFFF) );
		}
		
		public static Rsize oF( char null_val, int bits_per_item, int... values ) {
			Rsize dst = new Rsize( null_val, bits_per_item, -values.length );
			fill( dst, values );
			return dst;
		}
		
		public static Rsize of( char null_val, int bits_per_item, Integer... values ) {
			Rsize dst = new Rsize( null_val, bits_per_item, -values.length );
			filL( dst, values );
			return dst;
		}
	}
	
	class RW extends R implements Consumer {
		
		public RW( char null_val, int bits_per_item, int items ) { super( null_val, bits_per_item, items ); }
		
		public static RW oF( char null_val, int bits_per_item, int... values ) {
			RW dst = new RW( null_val, bits_per_item, -values.length );
			fill( dst, values );
			return dst;
		}
		
		public static RW of( char null_val, int bits_per_item, Integer... values ) {
			RW dst = new RW( null_val, bits_per_item, -values.length );
			filL( dst, values );
			return dst;
		}
		
		public boolean add( char value ) {
			add( this, value );
			return true;
		}
		
		public boolean add( Integer value ) {
			add( this, value == null ? null_val : (char) (value & 0xFFFF) );
			return true;
		}
		
		public void remove( Integer value )     { remove( this, value == null ? null_val : (char) (value & 0xFFFF) ); }
		
		public void remove( int value )         { remove( this, value ); }
		
		public void remove( char value )        { remove( this, value ); }
		
		public void removeAt( int item )        { removeAt( this, item ); }
		
		
		public void set( int value )            {set( this, size, value ); }
		
		public void set( char value )           {set( this, size, value ); }
		
		public void set( Integer value )        { set( this, size, value == null ? null_val : (char) (value & 0xFFFF) ); }
		
		
		public void set( int item, char value ) {set( item, (int) value );}
		
		public void set( int item, int value ) {
			final int fix = size;
			set( this, item, value );
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void seT( int item, int... values ) {
			final int fix = size;
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, item + i, (char) values[i] );
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, Integer value ) {
			final int fix = size;
			set( this, item, value == null ? null_val : (char) (value & 0xFFFF) );
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, Integer... values ) {
			final int fix = size;
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, item + i, values[i] == null ? null_val : (char) (values[i] & 0xFFFF) );
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void clear()        { clear( this ); }
		
		public Consumer consumer() {return this; }
		
		public RW clone()          { return (RW) super.clone(); }
	}
}
