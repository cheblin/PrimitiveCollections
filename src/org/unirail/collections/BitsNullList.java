package org.unirail.collections;


import static org.unirail.collections.BitsList.*;

public interface BitsNullList {
	
	
	abstract class R extends BitsList.R {
		
		public final int null_val;
		
		public int null_val() {return null_val;}
		
		protected R( int null_val, int bits_per_item ) {
			super( bits_per_item );
			this.null_val = null_val;
		}
		
		protected R( int null_val, int bits_per_item, int items ) {
			super( bits_per_item, items );
			this.null_val = null_val;
			
			if (0 < items && null_val != 0) nulls( this, 0, items );
		}
		
		protected static void nulls( R dst, int from, int upto ) {while (from < upto) set( dst, from++, dst.null_val );}
		
		protected R( int null_val, int bits_per_item, int fill_value, int size ) {
			super( bits_per_item, size );
			this.null_val = null_val;
			this.size     = size;
			if (fill_value == 0) return;
			while (-1 < --size) set( this, size, fill_value );
		}
		
		
		public boolean hasValue( int index ) {return get( index ) != null_val;}
		
		@Override public R clone()           {return (R) super.clone();}
		
		StringBuilder toString( StringBuilder dst ) {
			final int size = size();
			if (dst == null) dst = new StringBuilder( size * 4 );
			else dst.ensureCapacity( dst.length() + size * 4 );
			
			long src  = array[0];
			for (int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++)
			{
				final int bit   = bit( bp );
				long      value = (BITS < bit + bits ? value( src, src = array[index( bp ) + 1], bit, bits ) : value( src, bit, (int) mask ));
				
				if (value == null_val) dst.append( "null" );
				else dst.append( value );
				
				dst.append( '\t' );
				
				if (i % 10 == 0) dst.append( '\t' ).append( i / 10 * 10 ).append( '\n' );
			}
			
			return dst;
		}
	}
	
	class RW extends R {
		
		public RW( int null_val, int bits_per_item )                            {super( null_val, bits_per_item );}
		
		
		public RW( int null_val, int bits_per_item, int items )                 {super( null_val, bits_per_item, items );}
		
		public RW( int null_val, int bits_per_item, int fill_value, int items ) {super( null_val, bits_per_item, fill_value, items );}
		
		public RW( int null_val, int bits_per_item, Integer fill_value, int items ) {super( null_val, bits_per_item, fill_value == null ? null_val : fill_value, items );}
		
		
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
		
		
		public void add( Byte value )                 {add( size, value == null ? null_val : value & 0xFF );}
		
		public void add( Character value )            {add( size, value == null ? null_val : value & 0xFF );}
		
		public void add( Short value )                {add( size, value == null ? null_val : value & 0xFF );}
		
		public void add( Integer value )              {add( size, value == null ? null_val : value & 0xFF );}
		
		public void add( Long value )                 {add( size, value == null ? null_val : value & 0xFF );}
		
		public void add( int index, Byte value )      {add( index, value == null ? null_val : value & 0xFF );}
		
		public void add( int index, Character value ) {add( index, value == null ? null_val : value & 0xFF );}
		
		public void add( int index, Short value )     {add( index, value == null ? null_val : value & 0xFF );}
		
		public void add( int index, Integer value )   {add( index, value == null ? null_val : value & 0xFF );}
		
		public void add( int index, Long value )      {add( index, value == null ? null_val : value & 0xFF );}
		
		public void add( int index, long src ) {
			if (index < size) add( this, index, src );
			else set( index, src );
		}
		
		public void remove( Byte value )              {remove( this, value == null ? null_val : value & 0xFF );}
		
		public void remove( Character value )         {remove( this, value == null ? null_val : value & 0xFF );}
		
		public void remove( Short value )             {remove( this, value == null ? null_val : value & 0xFF );}
		
		public void remove( Integer value )           {remove( this, value == null ? null_val : value & 0xFF );}
		
		public void remove( Long value )              {remove( this, value == null ? null_val : value & 0xFF );}
		
		public void remove( int value )               {remove( this, value );}
		
		
		public void removeAt( int item )              {removeAt( this, item );}
		
		
		public void set( int index, Byte value )      {set( index, value == null ? null_val : value & 0xFF );}
		
		public void set( int index, Character value ) {set( index, value == null ? null_val : value & 0xFF );}
		
		public void set( int index, Short value )     {set( index, value == null ? null_val : value & 0xFF );}
		
		public void set( int index, Integer value )   {set( index, value == null ? null_val : value & 0xFF );}
		
		public void set( int index, Long value )      {set( index, value == null ? null_val : value & 0xFF );}
		
		public void set( int item, long value ) {
			final int fix = size;
			set( this, item, value );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		
		public void set( int item, Byte... values ) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, Character... values ) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, Short... values ) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, Integer... values ) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, Long... values ) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
			     set( this, item + i, values[i] == null ? null_val : (int) (values[i] & 0xFF) );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, byte... values ) {
			final int fix = size;
			set( this, item, values );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, char... values ) {
			final int fix = size;
			set( this, item, values );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, short... values ) {
			final int fix = size;
			set( this, item, values );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, int... values ) {
			final int fix = size;
			set( this, item, values );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void set( int item, long... values ) {
			final int fix = size;
			set( this, item, values );
			
			if (fix < item && null_val != 0) nulls( this, fix, item );
		}
		
		public void clear() {
			if (size < 1) return;
			nulls( this, 0, size );
			size = 0;
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
