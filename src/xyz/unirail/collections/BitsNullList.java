package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

public interface BitsNullList {
	
	
	abstract class R extends BitsList.R {
		
		public final int null_val;
		
		protected R( int null_val, int bits_per_item ) {
			super( bits_per_item );
			this.null_val = null_val;
		}
		
		protected R( int null_val, int bits_per_item, int items ) {
			super( bits_per_item, null_val, items );
			this.null_val = null_val;
		}
		
		protected R( int null_val, int bits_per_item, int default_value, int items ) {
			super( bits_per_item, default_value, items );
			this.null_val = null_val;
			this.size     = items;
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
		
		void set( int item, long value );
		
		void set( int item, Long value );
		
		void add( long value );
		
		void add( Long value );
	}
	
	class RW extends R implements Interface {
		public RW( int null_val, int bits_per_item )                                   { super( null_val, bits_per_item ); }
		
		
		public RW( int null_val, int bits_per_item, int items )                        { super( null_val, bits_per_item, items ); }
		
		public RW( int null_val, int bits_per_item, int default_value, int items )     { super( null_val, bits_per_item, default_value, items ); }
		
		public RW( int null_val, int bits_per_item, Integer default_value, int items ) { super( null_val, bits_per_item, default_value == null ? null_val : default_value, items ); }
		
		
		public RW( int null_val, int bits_per_item, byte... values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, Byte... values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, char... values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, Character... values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, short... values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, Short... values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, int[] values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, Integer[] values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, long... values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		public RW( int null_val, int bits_per_item, Long... values ) {
			super( null_val, bits_per_item, values.length );
			set( 0, values );
		}
		
		
		public void add( Byte value )                 { add( size, value == null ? null_val : value & 0xFF ); }
		
		public void add( Character value )            { add( size, value == null ? null_val : value & 0xFF ); }
		
		public void add( Short value )                { add( size, value == null ? null_val : value & 0xFF ); }
		
		public void add( Integer value )              { add( size, value == null ? null_val : value & 0xFF ); }
		
		public void add( Long value )                 { add( size, value == null ? null_val : value & 0xFF ); }
		
		public void add( int index, Byte value )      { add( index, value == null ? null_val : value & 0xFF ); }
		
		public void add( int index, Character value ) { add( index, value == null ? null_val : value & 0xFF ); }
		
		public void add( int index, Short value )     { add( index, value == null ? null_val : value & 0xFF ); }
		
		public void add( int index, Integer value )   { add( index, value == null ? null_val : value & 0xFF ); }
		
		public void add( int index, Long value )      { add( index, value == null ? null_val : value & 0xFF ); }
		
		public void add( long value )                 { add( size, value & 0xFF ); }
		public void add( int index, long src ) {
			if( index < size ) add( this, index, src );
			else set( index, src );
		}
		
		public void remove( Byte value )              { remove( this, value == null ? null_val : value & 0xFF ); }
		
		public void remove( Character value )         { remove( this, value == null ? null_val : value & 0xFF ); }
		
		public void remove( Short value )             { remove( this, value == null ? null_val : value & 0xFF ); }
		
		public void remove( Integer value )           { remove( this, value == null ? null_val : value & 0xFF ); }
		
		public void remove( Long value )              { remove( this, value == null ? null_val : value & 0xFF ); }
		
		public void remove( int value )               { remove( this, value ); }
		
		
		public void removeAt( int item )              { removeAt( this, item ); }
		
		
		public void set( int index, Byte value )      { set( index, value == null ? null_val : value & 0xFF ); }
		
		public void set( int index, Character value ) { set( index, value == null ? null_val : value & 0xFF ); }
		
		public void set( int index, Short value )     { set( index, value == null ? null_val : value & 0xFF ); }
		
		public void set( int index, Integer value )   { set( index, value == null ? null_val : value & 0xFF ); }
		
		public void set( int index, Long value )      { set( index, value == null ? null_val : value & 0xFF ); }
		
		public void set( int item, long value ) {
			set( this, item, value );
		}
		
		
		public void set( int item, Byte... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
		}
		
		public void set( int item, Character... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
		}
		
		public void set( int item, Short... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
		}
		
		public void set( int item, Integer... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
		}
		
		public void set( int item, Long... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : (int) (values[i] & 0xFF) );
		}
		
		public void set( int item, byte... values ) {
			set( this, item, values );
		}
		
		public void set( int item, char... values ) {
			set( this, item, values );
		}
		
		public void set( int item, short... values ) {
			set( this, item, values );
		}
		
		public void set( int item, int... values ) {
			set( this, item, values );
		}
		
		public void set( int item, long... values ) {
			set( this, item, values );
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
	
}
