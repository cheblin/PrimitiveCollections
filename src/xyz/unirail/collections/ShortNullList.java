package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

import java.util.Arrays;

public interface ShortNullList {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		BitList.RW         nulls;
		ShortList.RW values;
		
		
		public int length()                                 { return nulls.length(); }
		
		public int size()                                   { return nulls.size; }
		
		public boolean isEmpty()                            { return size() < 1; }
		
		public boolean hasValue( int index )                { return nulls.get( index ); }
		
		@Positive_OK public int nextValueIndex( int index ) { return nulls.next1( index ); }
		
		@Positive_OK public int prevValueIndex( int index ) { return nulls.prev1( index ); }
		
		@Positive_OK public int nextNullIndex( int index )  { return nulls.next0( index ); }
		
		@Positive_OK public int prevNullIndex( int index )  { return nulls.prev0( index ); }
		
		public short get( @Positive_ONLY int index ) { return (short) values.get( nulls.rank( index ) - 1 ); }
		
		
		public int indexOf( short value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		
		public int lastIndexOf( short value ) {
			int i = values.lastIndexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		
		public boolean equals( Object obj ) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public int hashCode()            { return Array.finalizeHash( Array.hash( Array.hash( nulls ), values ), size() ); }
		
		
		public boolean equals( R other ) { return other != null && other.size() == size() && values.equals( other.values ) && nulls.equals( other.nulls ); }
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				dst.values = values.clone();
				dst.nulls  = nulls.clone();
				return dst;
			} catch( CloneNotSupportedException e )
			{
				e.printStackTrace();
			}
			return null;
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterArray();
			int size = size();
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				for( int i = 0, ii; i < size; )
					if( (ii = nextValueIndex( i )) == i ) json.value( get( i++ ) );
					else if( ii == -1 || size <= ii )
					{
						while( i++ < size ) json.value();
						break;
					}
					else for( ; i < ii; i++ ) json.value();
			}
			json.exitArray();
		}
		
		
		protected static void set( R dst, int index,  Short     value ) {
			
			if( value == null )
			{
				if( dst.size() <= index ) dst.nulls.set0( index );//resize
				else if( dst.nulls.get( index ) )
				{
					dst.values.remove( dst.nulls.rank( index ) );
					dst.nulls.set0( index );
				}
			}
			else set( dst, index, (short) (value + 0) );
		}
		
		protected static void set( R dst, int index, short value ) {
			
			if( dst.nulls.get( index ) ) dst.values.set( dst.nulls.rank( index ) - 1, value );
			else
			{
				dst.nulls.set1( index );
				dst.values.add( dst.nulls.rank( index ) - 1, value );
			}
		}
	}
	
	interface Interface {
		int size();
		
		boolean hasValue( int index );
		
		short get( @Positive_ONLY int index );
		
		void add(  Short     value );
		
		void add( int index,  Short     value );
		
		void add( int index, short value );
		
		void set( int index,  Short     value );
		
		void set( int index, short value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int size ) { this( size, size ); }
		
		public RW( int length, int size ) {
			nulls  = new BitList.RW( length );
			values = new ShortList.RW( length );
			
			if( length < size ) size = length;
			
			if( 0 < size ) nulls.set0( size - 1 );
		}
		public RW(  Short     default_value, int size ) {
			this( size );
			if( default_value == null ) nulls.size = size;
			else
			{
				values.size = size;
				short v = (short) (default_value + 0);
				while( -1 < --size ) values.values[size] = v;
			}
		}
		
		
		public RW(  Short    ... values ) {
			this( values.length );
			for(  Short     value : values )
				if( value == null ) nulls.add( false );
				else
				{
					this.values.add( (short) (value + 0) );
					nulls.add( true );
				}
		}
		
		public RW( short... values ) {
			this( values.length );
			for( short value : values ) this.values.add( (short) value );
			
			nulls.set1( 0, values.length - 1 );
		}
		
		public RW( R src, int fromIndex, int toIndex ) {
			
			nulls  = new BitList.RW( src.nulls, fromIndex, toIndex );
			values = nulls.values.length == 0 ?
			         new ShortList.RW( 0 ) :
			         new ShortList.RW( src.values, src.nulls.rank( fromIndex ), src.nulls.rank( toIndex ) );
			
		}
		
		public void length( int length ) {
			boolean f = false;
			if( this.nulls.length() < length )
				this.nulls.length( length );
			else if( length < this.nulls.length() )
			{
				this.nulls.length( length );
				f = true;
			}
			if( values.values.length == length )
			{
				if( f )
				{
					int c = nulls.cardinality();
					if( c < values.size() ) for( int i = c; i < this.values.size; i++ ) values.values[i] = 0;
					values.size = c;
				}
				return;
			}
			
			if( length < values.values.length && f )
			{
				int c = nulls.cardinality();
				if( c < values.size() )
				{
					short[] tmp = values.values;
					
					values.values = new short[length];
					System.arraycopy( tmp, 0, values.values, 0, values.size = c );
				}
			}
			values.values = Arrays.copyOf( values.values, length );
			
		}
		
		public void fit() {
			nulls.fit();
			values.values = Arrays.copyOf( values.values, values.size );
		}
		
		public RW clone()    { return (RW) super.clone(); }
		
		
		public void remove() { remove( size() - 1 ); }
		
		public void remove( int index ) {
			if( size() < 1 || size() <= index ) return;
			
			if( nulls.get( index ) ) values.remove( nulls.rank( index ) - 1 );
			nulls.remove( index );
		}
		
		public void add(  Short     value ) {
			if( value == null ) nulls.add( false );
			else add( (short) (value + 0) );
		}
		
		public void add( short value ) {
			values.add( value );
			nulls.add( true );
		}
		
		
		public void add( int index,  Short     value ) {
			if( value == null ) nulls.add( index, false );
			else add( index, (short) (value + 0) );
		}
		
		public void add( int index, short value ) {
			if( index < size() )
			{
				nulls.add( index, true );
				values.add( nulls.rank( index ) - 1, value );
			}
			else set( index, value );
		}
		
		public void set(  Short     value )            { set( this, size() - 1, value ); }
		
		public void set( short value )                { set( this, size() - 1, value ); }
		
		public void set( int index,  Short     value ) { set( this, index, value ); }
		
		public void set( int index, short value )     { set( this, index, value ); }
		
		public void set( int index, short... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set( this, index + i, (short) values[i] );
		}
		
		public void set( int index,  Short    ... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set( this, index + i, values[i] );
		}
		
		public void addAll( R src ) {
			
			for( int i = 0, s = src.size(); i < s; i++ )
				if( src.hasValue( i ) ) add( src.get( i ) );
				else nulls.add( false );
		}
		
		public void clear() {
			values.clear();
			nulls.clear();
		}
		
		public void swap( int index1, int index2 ) {
			
			int exist, empty;
			if( nulls.get( index1 ) )
				if( nulls.get( index2 ) )
				{
					values.swap( nulls.rank( index1 ) - 1, nulls.rank( index2 ) - 1 );
					return;
				}
				else
				{
					exist = nulls.rank( index1 ) - 1;
					empty = nulls.rank( index2 );
					nulls.set0( index1 );
					nulls.set1( index2 );
				}
			else if( nulls.get( index2 ) )
			{
				exist = nulls.rank( index2 ) - 1;
				empty = nulls.rank( index1 );
				
				nulls.set1( index1 );
				nulls.set0( index2 );
			}
			else return;
			
			short v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
	}
}
