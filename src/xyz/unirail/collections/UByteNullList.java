package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

import java.util.Arrays;

public interface UByteNullList {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		protected R( boolean default_value_is_null ) { this.default_value_is_null = default_value_is_null; }
		public final boolean default_value_is_null;
		
		BitList.RW         nulls;
		UByteList.RW values;
		
		
		public int length()                                 { return nulls.length(); }
		
		public int size()                                   { return nulls.size; }
		
		public boolean isEmpty()                            { return size() < 1; }
		
		public boolean hasValue( int index )                { return nulls.get( index ); }
		
		@Positive_OK public int nextValueIndex( int index ) { return nulls.next1( index ); }
		
		@Positive_OK public int prevValueIndex( int index ) { return nulls.prev1( index ); }
		
		@Positive_OK public int nextNullIndex( int index )  { return nulls.next0( index ); }
		
		@Positive_OK public int prevNullIndex( int index )  { return nulls.prev0( index ); }
		
		public char get( @Positive_ONLY int index ) { return (char)( 0xFFFF &  values.get( nulls.rank( index ) - 1 )); }
		
		
		public int indexOf( char value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		
		public int lastIndexOf( char value ) {
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
		
		
		protected static void set( R dst, int index,  Character value ) {
			
			if( value == null )
			{
				if( dst.size() <= index ) dst.nulls.set0( index );//resize
				else if( dst.nulls.get( index ) )
				{
					dst.values.remove( dst.nulls.rank( index ) );
					dst.nulls.set0( index );
				}
			}
			else set( dst, index, value. charValue     () );
		}
		
		protected static void set( R dst, int index, char value ) {
			
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
		
		char get( @Positive_ONLY int index );
		
		void add(  Character value );
		
		void add( int index,  Character value );
		
		void add( int index, char value );
		
		void set( int index,  Character value );
		
		void set( int index, char value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length ) {
			super( false );
			
			nulls  = new BitList.RW( length );
			values = new UByteList.RW( length );
		}
		
		public RW(  Character default_value, int size ) {
			super( default_value == null );
			
			nulls  = new BitList.RW( false, size );
			values = new UByteList.RW( default_value_is_null ? (char) 0 : default_value. charValue     (), size );
		}
		
		public RW( char default_value, long size ) {
			super( false );
			
			nulls  = new BitList.RW( false, (int) size );
			values = new UByteList.RW( default_value, (int) size );
		}
		
		public RW(  Character... values ) { this( null, values ); }
		
		public RW(  Character default_value,  Character... values ) {
			super( default_value == null );
			int length = values == null ? 0 : values.length;
			
			nulls       = new BitList.RW( length );
			this.values = new UByteList.RW( length );
			if( length == 0 ) return;
			
			for(  Character value : values ) add( value );
		}
		
		
		public RW( char... values ) { this( (char)0, values ); }
		public RW( char default_value, char... values ) {
			super( false );
			if( values == null )
			{
				nulls       = new BitList.RW( 0 );
				this.values = new UByteList.RW( 0 );
				return;
			}
			
			nulls       = new BitList.RW( true, values.length );
			this.values = new UByteList.RW( default_value, values );
		}
		
		
		public RW( R src, int fromIndex, int toIndex ) {
			super( src.default_value_is_null );
			nulls  = new BitList.RW( src.nulls, fromIndex, toIndex );
			values = nulls.cardinality() == 0 ?
			         new UByteList.RW( 0 ) :
			         new UByteList.RW( src.values, src.nulls.rank( fromIndex ), src.nulls.rank( toIndex ) );
		}
		
		
		public RW clone()    { return (RW) super.clone(); }
		
		
		public void remove() { remove( size() - 1 ); }
		
		public void remove( int index ) {
			if( size() < 1 || size() <= index ) return;
			
			if( nulls.get( index ) ) values.remove( nulls.rank( index ) - 1 );
			nulls.remove( index );
		}
		
		public void add(  Character value ) {
			if( value == null ) nulls.add( false );
			else add( value. charValue     () );
		}
		
		public void add( char value ) {
			values.add( value );
			nulls.add( true );
		}
		
		
		public void add( int index,  Character value ) {
			if( value == null ) nulls.add( index, false );
			else add( index, value. charValue     () );
		}
		
		public void add( int index, char value ) {
			if( index < size() )
			{
				nulls.add( index, true );
				values.add( nulls.rank( index ) - 1, value );
			}
			else set( index, value );
		}
		
		public void set(  Character value )            { set( this, size() - 1, value ); }
		
		public void set( char value )                { set( this, size() - 1, value ); }
		
		public void set( int index,  Character value ) { set( this, index, value ); }
		
		public void set( int index, char value )     { set( this, index, value ); }
		
		public void set( int index, char... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set( this, index + i, (char) values[i] );
		}
		
		public void set( int index,  Character... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set( this, index + i, values[i] );
		}
		
		public void addAll( R src ) {
			
			for( int i = 0, s = src.size(); i < s; i++ )
				if( src.hasValue( i ) ) add( src.get( i ) );
				else nulls.add( false );
		}
		
		public RW clear() {
			values.clear();
			nulls.clear();
			return this;
		}
		
		public RW size( int size ) {
			if( size < 1 ) clear();
			else
			{
				nulls.size( size );
				values.size( nulls.cardinality() );
			}
			return this;
		}
		
		public RW length( int length ) {
			nulls.length( length );
			values.length( length );
			values.size( nulls.cardinality() );
			return this;
		}
		
		public RW fit() {
			nulls.fit();
			values.values = Arrays.copyOf( values.values, values.size );
			return this;
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
			
			char v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
	}
}
