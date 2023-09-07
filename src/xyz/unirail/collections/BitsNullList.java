package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

public interface BitsNullList {
	
	
	abstract class R extends BitsList.R {
		
		public final int null_val;
		
		protected R( int null_val, int bits_per_item ) {
			super( bits_per_item );
			this.null_val = null_val;
		}
		
		protected R( int null_val, int bits_per_item, int size ) {
			super( bits_per_item, null_val, size );
			this.null_val = null_val;
		}
		
		protected R( int null_val, int bits_per_item, int default_value, int size ) {
			super( bits_per_item, default_value, size );
			this.null_val = null_val;
			this.size     = size;
		}
		
		
		public boolean hasValue( int index ) { return get( index ) != null_val; }
		public boolean contains( long item ) { return 0 < indexOf( item ); }
		
		@Override public R clone()           { return (R) super.clone(); }
		
		
		public String toString()             { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterArray();
			
			final int size = size();
			if( 0 < size )
			{
				json.preallocate( size * 4 );
				
				long src = values[0];
				for( int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++ )
				{
					final int bit   = BitsList.bit( bp );
					long      value = (BitsList.BITS < bit + bits ? BitsList.value( src, src = values[BitsList.index( bp ) + 1], bit, bits, mask ) : BitsList.value( src, bit, mask ));
					
					if( value == null_val ) json.value();
					else json.value( value );
				}
			}
			json.exitArray();
		}
	}
	
	interface Interface {
		int size();
		
		int get( int item );
		
		boolean hasValue( int index );
		
		RW set( int item, long value );
		
		RW set( int item, Long value );
		
		RW add( long value );
		
		RW add( Long value );
	}
	
	class RW extends R implements Interface {
		public RW( int null_val, int bits_per_item )                                  { super( null_val, bits_per_item ); }
		
		public RW( int null_val, int bits_per_item, int size )                        { super( null_val, bits_per_item, size ); }
		
		public RW( int null_val, int bits_per_item, int default_value, int size )     { super( null_val, bits_per_item, default_value, size ); }
		
		public RW( int null_val, int bits_per_item, Integer default_value, int size ) { super( null_val, bits_per_item, default_value == null ? null_val : default_value, size ); }
		
		
		public RW add( Byte value )                                                   { return add( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add( Character value )                                              { return add( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add( Short value )                                                  { return add( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add( Integer value )                                                { return add( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add( Long value )                                                   { return add( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add( int index, Byte value )                                        { return add( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add( int index, Character value )                                   { return add( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add( int index, Short value )                                       { return add( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add( int index, Integer value )                                     { return add( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add( int index, Long value )                                        { return add( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add( long value )                                                   { return add( size, value & 0xFF ); }
		public RW add( int index, long src ) {
			if( index < size ) add( this, index, src );
			else set( index, src );
			return this;
		}
		
		public RW remove( Byte value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( Character value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( Short value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( Integer value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( Long value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( int value ) {
			remove( this, value );
			return this;
		}
		
		public RW removeAt( int item ) {
			removeAt( this, item );
			return this;
		}
		
		public RW set( int index, Byte value )      { return set( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set( int index, Character value ) { return set( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set( int index, Short value )     { return set( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set( int index, Integer value )   { return set( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set( int index, Long value ) { return set( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set( int item, long value ) {
			set( this, item, value );
			return this;
		}
		
		
		public RW set( int item, Byte... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int item, Character... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int item, Short... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int item, Integer... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int item, Long... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : (int) (values[i] & 0xFF) );
			return this;
		}
		
		public RW set( int item, byte... values ) {
			set( this, item, values );
			return this;
		}
		
		public RW set( int item, char... values ) {
			set( this, item, values );
			return this;
		}
		
		public RW set( int item, short... values ) {
			set( this, item, values );
			return this;
		}
		
		public RW set( int item, int... values ) {
			set( this, item, values );
			return this;
		}
		
		public RW set( int item, long... values ) {
			set( this, item, values );
			return this;
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
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
	
}
