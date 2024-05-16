package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

import java.util.Arrays;

public interface CharNullList {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		
		BitList.RW         nulls;
		CharList.RW values;
		
		
		public int length()                                 { return nulls.length(); }
		
		public int size()                                   { return nulls.size; }
		
		public boolean isEmpty()                            { return size() < 1; }
		
		public boolean hasValue( int index )                { return nulls.get( index ); }
		
		@Positive_OK public int nextValueIndex( int index ) { return nulls.next1( index ); }
		
		@Positive_OK public int prevValueIndex( int index ) { return nulls.prev1( index ); }
		
		@Positive_OK public int nextNullIndex( int index )  { return nulls.next0( index ); }
		
		@Positive_OK public int prevNullIndex( int index )  { return nulls.prev0( index ); }
		
		public char get( @Positive_ONLY int index ) { return (char) values.get( nulls.rank( index ) - 1 ); }

		
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
			
			if( dst.nulls.get( index ) ) dst.values.set1( dst.nulls.rank( index ) - 1, value );
			else
			{
				dst.nulls.set1( index );
				dst.values.add1( dst.nulls.rank( index ) - 1, value );
			}
		}
	}
	
	interface Interface {
		int size();
		
		boolean hasValue( int index );
		
		char get( @Positive_ONLY int index );
		
		RW add1(  Character value );
		
		RW add1( int index,  Character value );
		
		RW add1( int index, char value );
		
		RW set1( int index,  Character value );
		
		RW set1( int index, char value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length ) {
			super(  );
			
			nulls  = new BitList.RW( length );
			values = new CharList.RW( length );
		}
		
		public RW(  Character default_value, int size ) {
			super( );
			
			nulls  = new BitList.RW( default_value != null, size );
			values = default_value == null ?
			         new CharList.RW( 0 ) :
			         new CharList.RW( default_value. charValue     (), size );
		}
		
		public RW( char default_value, int size ) {
			super(  );
			
			nulls  = new BitList.RW( false, size );
			values = new CharList.RW( default_value, size );
		}
		
		public RW clone()  { return (RW) super.clone(); }
		
		public RW remove() { return remove( size() - 1 ); }
		
		public RW remove( int index ) {
			if( size() < 1 || size() <= index ) return this;
			
			if( nulls.get( index ) ) values.remove( nulls.rank( index ) - 1 );
			nulls.remove( index );
			return this;
		}
		
		public RW set1(  Character value ) {
			set( this, size() - 1, value );
			return this;
		}
		
		public RW set1( char value ) {
			set( this, size() - 1, value );
			return this;
		}
		
		public RW set1( int index,  Character value ) {
			set( this, index, value );
			return this;
		}
		
		public RW set1( int index, char value ) {
			set( this, index, value );
			return this;
		}
		public RW set( int index,  Character... values ) {
			for( int i = values.length; -1 < --i; )
			     set( this, index + i, values[i] );
			return this;
		}
		
		public RW set( int index, char... values ) { return set( index, values, 0, values.length ); }
		
		public RW set( int index, char[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set( this, index + i, (char) values[src_index + i] );
			return this;
		}
		
		public RW set( int index,  Character[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set( this, index + i, values[src_index + i] );
			return this;
		}
		
		
		public RW add1(  Character value ) {
			if( value == null ) nulls.add( false );
			else add1( value. charValue     () );
			return this;
		}
		
		public RW add1( char value ) {
			values.add1( value );
			nulls.add( true );
			return this;
		}
		
		
		public RW add1( int index,  Character value ) {
			if( value == null ) nulls.add( index, false );
			else add1( index, value. charValue     () );
			return this;
		}
		
		public RW add1( int index, char value ) {
			if( index < size() )
			{
				nulls.add( index, true );
				values.add1( nulls.rank( index ) - 1, value );
			}
			else set1( index, value );
			return this;
		}
		
		public RW add( char... items ) {
			int size = size();
			set1( size() + items.length - 1, values.default_value );
			return set( size, items );
		}
		
		
		public RW add(  Character... items ) {
			int size = size();
			set1( size() + items.length - 1, values.default_value );
			return set( size, items );
		}
		
		public RW addAll( R src ) {
			
			for( int i = 0, s = src.size(); i < s; i++ )
				if( src.hasValue( i ) ) add1( src.get( i ) );
				else nulls.add( false );
			return this;
		}
		
		public RW clear() {
			values.clear();
			nulls.clear();
			return this;
		}
		
		public RW length( int length ) {
			nulls.length( length );
			values.length( length );
			values.size( nulls.cardinality() );
			return this;
		}
		
		public RW size( int size ) {
			nulls.size( size );
			values.size( nulls.cardinality() );
			return this;
		}
		
		public RW fit() {
			nulls.fit();
			values.values = Arrays.copyOf( values.values, values.size );
			return this;
		}
		
		public RW swap( int index1, int index2 ) {
			
			int exist, empty;
			if( nulls.get( index1 ) )
				if( nulls.get( index2 ) )
				{
					values.swap( nulls.rank( index1 ) - 1, nulls.rank( index2 ) - 1 );
					return this;
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
			else return this;
			
			char v = values.get( exist );
			values.remove( exist );
			values.add1( empty, v );
			return this;
		}
	}
}
