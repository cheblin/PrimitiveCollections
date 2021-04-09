package org.unirail.collections;


public interface BoolNullList {
	
	interface Consumer {
		boolean add( Boolean value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		Boolean value( int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); dst.append( '\n' ), tag = tag( tag ))
			     dst.append( value( tag ) );
			
			return dst;
		}
	}
	
	class R extends BitsList.Base {
		
		public R() {
			super( 2 );
		}
		
		public R( int items ) {
			super( 2, items );
		}
		
		public static R oF( boolean... values ) {
			R dst = new R( values.length );
			fill( dst, values );
			return dst;
		}
		
		protected static void fill( R dst, boolean... items ) {
			
			final int bits = dst.bits;
			for (boolean i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (i ? 1L : 2L) << bit;
			}
		}
		
		public static R of( Boolean... values ) {
			R dst = new R( values.length );
			filL( dst, values );
			return dst;
		}
		
		protected static void filL( R dst, Boolean... items ) {
			
			final int bits = dst.bits;
			for (Boolean b : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (b == null ? 0L : b ? 1L : 2L) << bit;
			}
		}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? 0 : -1; }
				
				public int tag( int tag ) { return tag != -1 && tag < size - 1 ? ++tag : -1; }
				
				public Boolean value( int tag ) {return getBoolean( tag );}
			} : producer;
		}
		
		public Boolean getBoolean( int index ) {
			final int i = get( index );
			return i == 0 ? null : i == 1 ? Boolean.TRUE : Boolean.FALSE;
		}
		
		public R clone() {return (R) super.clone();}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( size * 2 );
			else dst.ensureCapacity( dst.length() + size * 2 );
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class Rsize extends R {
		
		public Rsize( int items ) {
			super( items );
			size = items;
		}
		
		public void set( int item, int value )     { if (item < size) set( this, item, value ); }
		
		public void set( int item, boolean value ) { if (item < size) set( this, item, value ? 1 : 2 ); }
		
		public void set( int item, Boolean value ) { if (item < size) set( this, item, value == null ? 0 : value ? 1 : 2 ); }
		
		public void seT( int index, boolean... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, values[i] ? 1 : 2 );
		}
		
		public void set( int index, Boolean... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, values[i] == null ? 0 : values[i] ? 1 : 2 );
		}
		
		public static Rsize oF( boolean... values ) {
			Rsize dst = new Rsize( values.length );
			fill( dst, values );
			return dst;
		}
		
		public static Rsize of( Boolean... values ) {
			Rsize dst = new Rsize( values.length );
			filL( dst, values );
			return dst;
		}
	}
	
	class RW extends Rsize implements Consumer {
		public RW() {super( 1 ); size = 0; }
		
		public RW( int items ) {
			super( items );
			size = 0;
		}
		
		public static RW of( boolean... values ) {
			RW dst = new RW( values.length );
			fill( dst, values );
			return dst;
		}
		
		public static RW of( Boolean... values ) {
			RW dst = new RW( values.length );
			filL( dst, values );
			return dst;
		}
		
		public boolean add( boolean value ) {
			add( this, value ? 1 : 2 );
			return true;
		}
		
		public boolean add( Boolean value ) {
			add( this, value == null ? 0 : value ? 1 : 2 );
			return false;
		}
		
		public void remove( Boolean value ) {
			remove( this, value == null ? 0 : value ? 1 : 2 );
		}
		
		public void remove( boolean value )        { remove( this, value ? 1 : 2 ); }
		
		public void removeAt( int item )           { removeAt( this, item ); }
		
		
		public void set( boolean value )           {set( this, size, value ? 1 : 2 ); }
		
		public void set( Boolean value )           { set( this, size, value == null ? 0 : value ? 1 : 2 ); }
		
		
		public void set( int item, int value )     {set( this, item, value ); }
		
		public void set( int item, boolean value ) {set( this, item, value ? 1 : 2 ); }
		
		public void set( int item, Boolean value ) { set( this, item, value == null ? 0 : value ? 1 : 2 ); }
		
		
		public void seT( int index, boolean... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, values[i] ? 1 : 2 );
		}
		
		public void set( int index, Boolean... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, values[i] == null ? 0 : values[i] ? 1 : 2 );
		}
		
		public void clear() {
			clear( this );
		}
		
		public Consumer consumer() {return this; }
		
		public RW clone()          { return (RW) super.clone(); }
	}
}
