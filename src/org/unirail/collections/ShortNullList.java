package org.unirail.collections;


public interface ShortNullList {
	
	
	abstract class R implements Comparable<R> {
		
		BitList.RW         nulls;
		ShortList.RW values;
		
		
		public int length()                                 {return values.length();}
		
		
		public int size()                                   {return nulls.size;}
		
		public boolean isEmpty()                            {return size() < 1;}
		
		public boolean hasValue( int index )                {return nulls.get( index );}
		
		@Positive_OK public int nextValueIndex( int index ) {return nulls.next1( index );}
		
		@Positive_OK public int prevValueIndex( int index ) {return nulls.prev1( index );}
		
		@Positive_OK public int nextNullIndex( int index )  {return nulls.next0( index );}
		
		@Positive_OK public int prevNullIndex( int index )  {return nulls.prev0( index );}
		
		public short get( @Positive_ONLY int index ) {return (short) values.get( nulls.rank( index ) - 1 );}
		
		
		public int indexOf()                                {return nulls.next0( 0 );}
		
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
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public int hashCode()            {return nulls.hashCode() ^ values.hashCode();}
		
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			if (other.size() != size()) return other.size() - size();
			
			int diff;
			
			if ((diff = values.compareTo( other.values )) != 0 || (diff = nulls.compareTo( other.nulls )) != 0) return diff;
			
			return 0;
		}
		
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
		
		protected static void set( R dst, int index,  Short     value ) {
			
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
			else set( dst, index, (short) (value + 0) );
		}
		
		protected static void set( R dst, int index, short value ) {
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
			values = new ShortList.RW( length );
		}
		
		public RW(  Short     fill_value, int size ) {
			this( size );
			if (fill_value == null) nulls.size = size;
			else
			{
				values.size = size;
				short v = (short) (fill_value + 0);
				while (-1 < --size) values.array[size] = v;
			}
		}
		
		
		public RW(  Short    ... values ) {
			this( values.length );
			for ( Short     value : values)
				if (value == null) nulls.set( false );
				else
				{
					this.values.add( (short) (value + 0) );
					nulls.set( true );
				}
		}
		
		public RW( short... values ) {
			this( values.length );
			for (short value : values) this.values.add( (short) value );
			
			nulls.set1( 0, values.length - 1 );
		}
		
		public RW( R src, int fromIndex, int toIndex ) {
			
			nulls  = new BitList.RW( src.nulls, fromIndex, toIndex );
			values = nulls.array.length == 0 ?
			         new ShortList.RW( 0 ) :
			         new ShortList.RW( src.values, src.nulls.rank( fromIndex ), src.nulls.rank( toIndex ) );
			
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
		
		public void add(  Short     value ) {
			if (value == null) nulls.set( false );
			else add( (short) (value + 0) );
		}
		
		public void add( short value ) {
			values.add( value );
			nulls.set( true );
		}
		
		
		public void add( int index,  Short     value ) {
			if (value == null) nulls.set( false );
			else add( index, (short) (value + 0) );
		}
		
		public void add( int index, short value ) {
			if (index < size())
			{
				nulls.add( index, true );
				values.add( nulls.rank( index ) - 1, value );
			}
			else set( index, value );
		}
		
		public void set(  Short     value )            {set( this, size(), value );}
		
		public void set( short value )                {set( this, size(), value );}
		
		public void set( int index,  Short     value ) {set( this, index, value );}
		
		public void set( int index, short value )     {set( this, index, value );}
		
		public void set( int index, short... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, (short) values[i] );
		}
		
		public void set( int index,  Short    ... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, values[i] );
		}
		
		public void addAll( R src ) {
			
			for (int i = 0, s = src.size(); i < s; i++)
				if (src.hasValue( i )) add( src.get( i ) );
				else nulls.set( false );
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
			
			short v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
	}
}
