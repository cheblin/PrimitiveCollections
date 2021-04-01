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
		
		protected static void fill( R dst, boolean... items ) {
			
			final int bits = dst.bits;
			for (boolean i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (i ? 2L : 1L) << bit;
			}
		}
		
		public static R oF( Boolean... values ) {
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
				
				dst.array[index] |= (b == null ? 0L : b ? 2L : 1L) << bit;
			}
		}
		
		private Producer producer;
		
		public Producer producer_bool() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? 0 : -1; }
				
				public int tag( int tag ) { return tag != -1 && tag < size - 1 ? ++tag : -1; }
				
				public Boolean value( int tag ) {return get_bool( tag );}
			} : producer;
		}
		
		public int tag( int item ) {return super.get( item ) == 0 ? Integer.MIN_VALUE | item : item;}
		
		
		public Boolean get_bool( int tag ) {
			switch (super.get( tag ))
			{
				case 1: return Boolean.TRUE;
				case 2: return Boolean.FALSE;
			} return null;
		}
		
		public R clone() {return (R) super.clone();}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( size * 2 );
			else dst.ensureCapacity( dst.length() + size * 2 );
			return producer_bool().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class Rsize extends R {
		
		
		public Rsize( int items ) {
			super( items );
			size = items;
		}
		
		public void set( int item, boolean value ) { if (item < size) set( this, item, value ? 1 : 2 ); }
		
		public void set( int item, Boolean value ) { if (item < size) set( this, item, value == null ? 0 : value ? 1 : 2 ); }
		
		public void set( int index, boolean... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, values[i] ? 1 : 2 );
		}
		
		public void seT( int index, Boolean... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, values[i] == null ? 0 : values[i] ? 1 : 2 );
		}
		
		public static Rsize of( boolean... values ) {
			Rsize dst = new Rsize( values.length );
			fill( dst, values );
			return dst;
		}
		
		public static Rsize oF( Boolean... values ) {
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
		
		
		public void set( int item, boolean value ) {set( this, item, value ? 1 : 2 ); }
		
		public void set( int item, Boolean value ) { set( this, item, value == null ? 0 : value ? 1 : 2 ); }
		
		
		public void set( int index, boolean... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, values[i] ? 1 : 2 );
		}
		
		public void seT( int index, Boolean... values ) {
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
