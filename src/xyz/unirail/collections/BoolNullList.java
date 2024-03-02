package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

public interface BoolNullList {
	
	abstract class R extends BitsList.R {
		
		protected R( int length )                        { super( 2, length ); }
		
		protected R( Boolean default_value, int size ) { super( 2, default_value == null ? 2 : default_value ? 1 : 0, size ); }
		
		public boolean hasValue( int index )            { return get( index ) != 2; }
		public Boolean get_Boolean( int index ) {
			switch( get( index ) )
			{
				case 1:
					return Boolean.TRUE;
				case 0:
					return Boolean.FALSE;
			}
			return null;
		}
		
		@Override public void toJSON( JsonWriter json ) {
			json.enterArray();
			
			json.preallocate( size * 4 );
			if( 0 < size )
			{
				long src = values[0];
				
				for( int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++ )
				{
					final int bit   = BitsList.bit( bp );
					long      value = (BitsList.BITS < bit + bits ? BitsList.value( src, src = values[BitsList.index( bp ) + 1], bit, bits, mask ) : BitsList.value( src, bit, mask ));
					
					if( (value & 2) == 2 ) json.value();
					else json.value( value == 1 );
				}
			}
			json.exitArray();
		}
		
		public R clone() { return (R) super.clone(); }
	}
	
	interface Interface {
		int size();
		Boolean get_Boolean( int index );
		
		boolean hasValue( int index );
		
		RW set1( boolean value );
		
		RW set1( Boolean value );
		
		RW add( boolean value );
		
		RW add( Boolean value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length )                        { super( length ); }
		
		public RW( Boolean default_value, int size ) { super( default_value, size ); }
		
		
		public RW add( boolean value )           { add( this, value ? 1 : 0 ); return this; }
		
		public RW add( Boolean value )           { add( this, value == null ? 2 : value ? 1 : 0 ); return this;}
		
		
		public RW remove( Boolean value )        { remove( this, value == null ? 2 : value ? 1 : 0 );return this; }
		
		public RW remove( boolean value )        { remove( this, value ? 1 : 0 );return this; }
		
		
		public RW removeAt( int item )           { removeAt( this, item );return this; }
		
		
		public RW set1( boolean value )           { set1( this, size, value ? 1 : 0 ); return this;}
		
		public RW set1( Boolean value )           { set1( this, size, value == null ? 2 : value ? 1 : 0 );return this; }
		
		
		public RW set1( int item, boolean value ) { set1( this, item, value ? 1 : 0 ); return this;}
		
		public RW set1( int item, Boolean value ) { set1( this, item, value == null ? 2 : value ? 1 : 0 );return this; }
		
		
		public RW set( int index, boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set1( index + i, values[i] );
			return this;
		}
		
		public RW set( int index, Boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set1( index + i, values[i] );
			return this;
		}
		
		
		public RW fit() {
			length_( -size );
			return this;
		}
		
		public RW length( int items ) {
			if( items < 0 ) values = Array.EqualHashOf.longs.O;
			else length_( -items );
			return this;
		}
		
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( this.size < size ) set1(this, size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		
		public RW clone()   { return (RW) super.clone(); }
	}
}

