package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

public interface BoolNullList {
	
	abstract class R extends BitsList.R {
		
		protected R( int length )                        { super( 2, length ); }
		
		protected R( Boolean default_value, int size ) { super( 2, default_value == null ? 0 : default_value ? 1 : 2, size ); }
		
		public boolean hasValue( int index )            { return get( index ) != 0; }
		public Boolean get_Boolean( int index ) {
			switch( get( index ) )
			{
				case 3:
					return Boolean.TRUE;
				case 2:
					return Boolean.FALSE;
			}
			return null;
		}
		
		public String toString() { return toJSON(); }
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
					if( (value & 2) == 0 ) json.value();
					else json.value( value == 3 );
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
		
		void set( boolean value );
		
		void set( Boolean value );
		
		void add( boolean value );
		
		void add( Boolean value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length )                        { super( length ); }
		
		public RW( Boolean default_value, int size ) { super( default_value, size ); }
		
		public RW( boolean... values ) {
			super( values.length );
			set( 0, values );
		}
		
		public RW( Boolean... values ) {
			super( values.length );
			set( 0, values );
		}
		
		public void add( boolean value )           { add( this, value ? 1 : 2 ); }
		
		public void add( Boolean value )           { add( this, value == null ? 0 : value ? 1 : 2 ); }
		
		
		public void remove( Boolean value )        { remove( this, value == null ? 0 : value ? 1 : 2 ); }
		
		public void remove( boolean value )        { remove( this, value ? 1 : 2 ); }
		
		
		public void removeAt( int item )           { removeAt( this, item ); }
		
		
		public void set( boolean value )           { set( this, size, value ? 1 : 2 ); }
		
		public void set( Boolean value )           { set( this, size, value == null ? 0 : value ? 1 : 2 ); }
		
		public void set( int item, int value )     { set( this, item, value ); }
		
		
		public void set( int item, boolean value ) { set( this, item, value ? 1 : 2 ); }
		
		public void set( int item, Boolean value ) { set( this, item, value == null ? 0 : value ? 1 : 2 ); }
		
		
		public void set( int index, boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set( index + i, values[i] );
		}
		
		public void set( int index, Boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set( index + i, values[i] );
		}
		
		
		public RW fit() {
			length_( -size );
			return this;
		}
		
		public RW length( int items ) {
			if( items < 0 ) values = Array.Of.longs.O;
			else length_( -items );
			return this;
		}
		
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( this.size < size ) set( size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		
		public RW clone()   { return (RW) super.clone(); }
	}
}

