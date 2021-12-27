package org.unirail.collections;


import static org.unirail.collections.Array.HashEqual.hash;

public interface DoubleNullList {
	
	
	abstract class R  {
		
		BitList.RW         nulls;
		DoubleList.RW values;
		
		
		public int length()                                 {return values.length();}
		
		
		public int size()                                   {return nulls.size;}
		
		public boolean isEmpty()                            {return size() < 1;}
		
		public boolean hasValue( int index )                {return nulls.get( index );}
		
		@Positive_OK public int nextValueIndex( int index ) {return nulls.next1( index );}
		
		@Positive_OK public int prevValueIndex( int index ) {return nulls.prev1( index );}
		
		@Positive_OK public int nextNullIndex( int index )  {return nulls.next0( index );}
		
		@Positive_OK public int prevNullIndex( int index )  {return nulls.prev0( index );}
		
		public double get( @Positive_ONLY int index ) {return  values.get( nulls.rank( index ) - 1 );}
		
		
		public int indexOf()                                {return nulls.next0( 0 );}
		
		public int indexOf( double value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		
		public int lastIndexOf( double value ) {
			int i = values.lastIndexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		
		public boolean equals( Object obj ) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public int hashCode()            {return hash( hash( nulls ), values );}
		
		
		public boolean equals( R other )  {return other != null && other.size() == size() && values.equals(other.values) && nulls.equals(other.nulls);}
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				dst.values = values.clone();
				dst.nulls  = nulls.clone();
				return dst;
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 4 );
			else dst.ensureCapacity( dst.length() + size * 64 );
			
			for (int i = 0, ii; i < size; )
				if ((ii = nextValueIndex( i )) == i) dst.append( get( i++ ) ).append( '\n' );
				else if (ii == -1 || size <= ii)
				{
					while (i++ < size) dst.append( "null\n" );
					break;
				}
				else for (; i < ii; i++) dst.append( "null\n" );
			
			return dst;
		}
		
		protected static void set( R dst, int index,  Double    value ) {
			
			if (value == null)
			{
				if (dst.size() <= index)
				{
					dst.nulls.set0( index );
					return;
				}
				
				if (!dst.nulls.get( index )) return;
				
				dst.values.remove( dst.nulls.rank( index ) );
				dst.nulls.remove( index );
			}
			else set( dst, index, (double) (value + 0) );
		}
		
		protected static void set( R dst, int index, double value ) {
			if (dst.nulls.get( index ))
			{
				dst.values.set( dst.nulls.rank( index ) - 1, value );
				return;
			}
			
			dst.nulls.set1( index );
			dst.values.add( dst.nulls.rank( index ) - 1, value );
		}
	}
	
	
	class RW extends R {
		
		public RW( int length ) {
			nulls  = new BitList.RW( length );
			values = new DoubleList.RW( length );
		}
		
		public RW(  Double    fill_value, int size ) {
			this( size );
			if (fill_value == null) nulls.size = size;
			else
			{
				values.size = size;
				double v = (double) (fill_value + 0);
				while (-1 < --size) values.array[size] = v;
			}
		}
		
		
		public RW(  Double   ... values ) {
			this( values.length );
			for ( Double    value : values)
				if (value == null) nulls.add( false );
				else
				{
					this.values.add( (double) (value + 0) );
					nulls.add( true );
				}
		}
		
		public RW( double... values ) {
			this( values.length );
			for (double value : values) this.values.add( (double) value );
			
			nulls.set1( 0, values.length - 1 );
		}
		
		public RW( R src, int fromIndex, int toIndex ) {
			
			nulls  = new BitList.RW( src.nulls, fromIndex, toIndex );
			values = nulls.array.length == 0 ?
			         new DoubleList.RW( 0 ) :
			         new DoubleList.RW( src.values, src.nulls.rank( fromIndex ), src.nulls.rank( toIndex ) );
			
		}
		
		public void length( int length ) {
			values.length( length );
			nulls.length( length );
		}
		
		public void fit() {
			values.length( values.size );
			nulls.fit();
		}
		
		public RW clone()    {return (RW) super.clone();}
		
		
		public void remove() {remove( size() - 1 );}
		
		public void remove( int index ) {
			if (size() < 1 || size() <= index) return;
			
			if (nulls.get( index )) values.remove( nulls.rank( index ) - 1 );
			nulls.remove( index );
		}
		
		public void add(  Double    value ) {
			if (value == null) nulls.add( false );
			else add( (double) (value + 0) );
		}
		
		public void add( double value ) {
			values.add( value );
			nulls.add( true );
		}
		
		
		public void add( int index,  Double    value ) {
			if (value == null) nulls.add(index, false );
			else add( index, (double) (value + 0) );
		}
		
		public void add( int index, double value ) {
			if (index < size())
			{
				nulls.add( index, true );
				values.add( nulls.rank( index ) - 1, value );
			}
			else set( index, value );
		}
		
		public void set(  Double    value )            {set( this, size(), value );}
		
		public void set( double value )                {set( this, size(), value );}
		
		public void set( int index,  Double    value ) {set( this, index, value );}
		
		public void set( int index, double value )     {set( this, index, value );}
		
		public void set( int index, double... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, (double) values[i] );
		}
		
		public void set( int index,  Double   ... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, values[i] );
		}
		
		public void addAll( R src ) {
			
			for (int i = 0, s = src.size(); i < s; i++)
				if (src.hasValue( i )) add( src.get( i ) );
				else nulls.add( false );
		}
		
		public void clear() {
			values.clear();
			nulls.clear();
		}
		
		public void swap( int index1, int index2 ) {
			
			int exist, empty;
			if (nulls.get( index1 ))
				if (nulls.get( index2 ))
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
			else if (nulls.get( index2 ))
			{
				exist = nulls.rank( index2 ) - 1;
				empty = nulls.rank( index1 );
				
				nulls.set1( index1 );
				nulls.set0( index2 );
			}
			else return;
			
			double v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
	}
}
