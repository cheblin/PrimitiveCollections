package org.unirail.collections;


public interface BoolNullList {
	
	interface Consumer {
		boolean add( boolean value );
		
		boolean add( Boolean value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag )       {return tag != -1;}
		
		boolean value( int tag );
		
		default boolean hasValue( int tag ) { return -1 < tag; }
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); dst.append( '\n' ), tag = tag( tag ))
				if (hasValue( tag )) dst.append( value( tag ) );
				else dst.append( "null" );
			return dst;
		}
	}
	
	class R extends BitsList.R {
		
		public R() {
			super( 2 );
		}
		
		public R( int items ) {
			super( 2, items );
		}
		
		public static R of( boolean... values ) {
			R dst = new R( values.length );
			fill( dst, values );
			return dst;
		}
		
		static void fill( R dst, boolean... items ) {
			
			final int bits = dst.bits;
			for (boolean i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (i ? 2L : 1L) << bit;
			}
		}
		
		public static R of( Boolean... values ) {
			R dst = new R( values.length );
			fill( dst, values );
			return dst;
		}
		
		static void fill( R dst, Boolean... items ) {
			
			final int bits = dst.bits;
			for (Boolean b : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (b == null ? 0L : b ? 2L : 1L) << bit;
			}
		}
		
		private Producer producer;
		
		public Producer producer_bool() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? 0 : -1; }
				
				public int tag( int tag ) { return (tag &= Integer.MAX_VALUE) < size - 1 ? get( ++tag ) == 0 ? Integer.MIN_VALUE | tag : tag : -1; }
				
				public boolean value( int tag ) {return get( tag ) == 2;}
			} : producer;
		}
		
		public long tag( int item )         {return get( item ) == 0 ? Long.MIN_VALUE | item : item;}
		
		public boolean hasValue( long tag ) { return -1 < tag; }
		
		public boolean get( long tag )      { return get( (int) tag ) == 2; }
		
		public R clone()                    {return (R) super.clone();}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( size * 2 );
			else dst.ensureCapacity( dst.length() + size * 2 );
			return producer_bool().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class Rsize extends R {
		
		public Rsize() { }
		
		public Rsize( int items ) {
			super( items );
		}
		
		public boolean set( int value ) {
			if (array.length <= size) return false;
			return set( this, size, value );
		}
		
		public boolean set( int item, int value ) {
			if (array.length <= item) return false;
			return set( this, item, value );
		}
		
		public static Rsize of( boolean... values ) {
			Rsize dst = new Rsize( values.length );
			fill( dst, values );
			return dst;
		}
		
		public static Rsize of( Boolean... values ) {
			Rsize dst = new Rsize( values.length );
			fill( dst, values );
			return dst;
		}
	}
	
	class RW extends Rsize implements Consumer {
		public RW() {
		}
		
		public RW( int items ) {
			super( items );
		}
		
		public static RW of( boolean... values ) {
			RW dst = new RW( values.length );
			fill( dst, values );
			return dst;
		}
		
		public static RW of( Boolean... values ) {
			RW dst = new RW( values.length );
			fill( dst, values );
			return dst;
		}
		
		public boolean add( boolean value ) {
			add( this, value ? 2 : 1 );
			return true;
		}
		
		public boolean add( Boolean value ) {
			add( this, value == null ? 0 : value ? 2 : 1 );
			return false;
		}
		
		public void remove( Boolean value ) {
			remove( this, value == null ? 0 : value ? 2 : 1 );
		}
		
		public void remove( boolean value ) { remove( this, value ? 2 : 1 ); }
		
		public void removeAt( int item )    { removeAt( this, item ); }
		
		
		public boolean set( int item, int value ) {
			return set( this, item, value );
		}
		
		
		public void clear() {
			clear( this );
		}
		
		public Consumer consumer() {return this; }
		
		public RW clone()          { return (RW) super.clone(); }
	}
}
