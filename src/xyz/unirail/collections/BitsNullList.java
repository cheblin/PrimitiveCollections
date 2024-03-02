package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

public interface BitsNullList {
	
	
	abstract class R extends BitsList.R {
		
		public final int null_val;
		
		protected R( int bits_per_item, int null_val ) {
			super( bits_per_item );
			this.null_val = null_val;
		}
		
		protected R( int bits_per_item, int null_val, int size ) {
			super( bits_per_item, null_val, size );
			this.null_val = null_val;
		}
		
		protected R( int bits_per_item, int null_val, int default_value, int size ) {
			super( bits_per_item, default_value, size );
			this.null_val = null_val;
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
		
		byte get( int item );
		
		boolean hasValue( int index );
		
		RW set1( int item, long value );
		
		RW set1( int item, Long value );
		
		RW add1( long value );
		
		RW add1( Long value );
	}
	
	class RW extends R implements Interface {
		public RW( int bits_per_item, int null_val ) { super( null_val, bits_per_item ); }
		
		public RW( int bits_per_item, int null_val, int size ) { super( bits_per_item, null_val, size ); }
		
		public RW( int bits_per_item, int null_val, int default_value, int size ) { super( bits_per_item, null_val, default_value, size ); }
		
		public RW( int bits_per_item, int null_val, Integer default_value, int size ) { super( bits_per_item, null_val, default_value == null ? null_val : default_value, size ); }
		
		
		public RW add1( Byte value )                 { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( Character value )            { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( Short value )                { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( Integer value )              { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( Long value ) { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Byte value )      { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Character value ) { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Short value )     { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Integer value )   { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Long value )      { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( long value ) { return add1( size, value & 0xFF ); }
		public RW add1( int index, long src ) {
			if( index < size ) add( this, index, src );
			else set1( index, src );
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
		
		public RW set1( int index, Byte value )      { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, Character value ) { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, Short value )     { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, Integer value )   { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, Long value )      { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, long value ) {
			set1( this, index, value );
			return this;
		}
		
		
		public RW set( int index, Byte... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int index, Byte[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, values[src_index + i] == null ? null_val : values[src_index + i] & 0xFF );
			return this;
		}
		
		public RW set( int item, Character... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int index, Short... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int index, Integer... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int index, Long... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[i] == null ? null_val : (int) (values[i] & 0xFF) );
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
