package org.unirail.collections;


public interface IntNullList {
	interface Consumer {
		boolean add( int value );
		
		boolean add(  Integer   value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag )       {return tag != -1;}
		
		int value( int tag );
		
		default boolean hasValue( int tag ) { return -1 < tag; }
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); dst.append( '\n' ), tag = tag( tag ))
				if (hasValue( tag )) dst.append( value( tag ) );
				else dst.append( "null" );
			return dst;
		}
	}
	
	
	class R implements Comparable<R> {
		
		BitList.RW         nulls  = new BitList.RW( 4 );
		IntList.RW values = new IntList.RW( 4 );
		
		
		R( int length ) {
			if (length < 1) return;
			
			values.length( length );
			nulls.length( length );
		}
		
		public static R oF(  Integer  ... values ) {
			R dst = new R( values.length );
			filL( dst, values );
			return dst;
		}
		
		protected static void filL( R dst,  Integer  ... values ) {
			for ( Integer   value : values)
				if (value == null) dst.size++;
				else
				{
					dst.values.add( (int) (value + 0) );
					dst.nulls.set1( dst.size );
					dst.size++;
				}
		}
		
		public static R of( int... values ) {
			R dst = new R( values.length );
			fill( dst, values );
			return dst;
		}
		
		protected static void fill( R dst, int... values ) {
			for (int value : values) dst.values.add( value );
			
			dst.size = values.length;
			
			dst.nulls.set1( 0, dst.size - 1 );
		}
		
		public int length() {return values.length();}
		
		int size = 0;
		
		public int size()                  { return size; }
		
		public boolean isEmpty()           { return size < 1; }
		
		
		public int tag( int index )        {return nulls.get( index ) ? index : -1;}
		
		public int get( int tag ) {return  values.array[nulls.rank( tag ) - 1]; }
		
		public boolean hasValue( int tag ) { return -1 < tag; }
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? nulls.get( 0 ) ? 0 : Integer.MIN_VALUE : -1; }
				
				public int tag( int tag ) { return (tag &= Integer.MAX_VALUE) < size - 1 ? nulls.get( ++tag ) ? tag : Integer.MIN_VALUE | tag : -1; }
				
				public int value( int tag ) {return  values.array[nulls.rank( tag ) - 1]; }
				
			} : producer;
		}
		
		public int indexOf() { return nulls.next0( 0 ); }
		
		public int indexOf( int value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		public int lastIndexOf() { return nulls.prev0( size );}
		
		public int lastIndexOf( int value ) {
			int i = values.lastIndexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		public R subList( int fromIndex, int toIndex ) {
			if (size <= fromIndex) return null;
			if (size - 1 < toIndex) toIndex = size - 1;
			
			if (!nulls.get( fromIndex )) fromIndex = nulls.next1( fromIndex );
			if (toIndex <= fromIndex) return null;
			
			if (!nulls.get( toIndex )) toIndex = nulls.prev1( toIndex );
			if (toIndex <= fromIndex) return null;
			
			long[] n = nulls.subList( fromIndex, toIndex );
			
			R ret = new R( 0 );
			ret.nulls.set( n );
			
			ret.size = size = toIndex - fromIndex;
			
			int from = nulls.rank( fromIndex );
			int to   = nulls.rank( toIndex );
			
			values.subList( from, to, ret.values );
			return ret;
		}
		
		
		public boolean equals( Object obj ) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
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
		
		public StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( size * 4 );
			else dst.ensureCapacity( dst.length() + size * 64 );
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
		
		protected static void set( R dst, int index, int value ) {
			if (dst.size <= index) dst.size = index + 1;
			
			if (dst.nulls.get( index )) dst.values.set( dst.nulls.rank( index ) - 1, value );
			else
			{
				dst.nulls.set1( index );
				dst.values.add( dst.nulls.rank( index ) - 1, value );
			}
		}
		
		protected static void set( Rsize dst, int index,  Integer   value ) {
			
			if (value == null)
			{
				if (dst.size <= index || !dst.nulls.get( index )) return;
				
				dst.nulls.remove( index );
				dst.values.remove( dst.nulls.rank( index ) );
				if (index + 1 == dst.size) dst.size--;
			}
			else set( dst, index, (int) (value + 0) );
		}
	}
	
	class Rsize extends R {
		
		public Rsize( int items ) {
			super( items );
			size = items;
		}
		
		public static Rsize oF(  Integer  ... values ) {
			Rsize dst = new Rsize( values.length );
			filL( dst, values );
			return dst;
		}
		
		public static Rsize of( int... values ) {
			Rsize dst = new Rsize( values.length );
			fill( dst, values );
			return dst;
		}
		
		public void set( int index,  Integer   value ) {
			if (size <= index) return;
			set( this, index, value );
		}
		
		public void set( int index, int value ) {
			if (size <= index) return;
			set( this, index, value );
		}
		
		public void set( int index, int... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, values[i] );
		}
		
		public void seT( int index,  Integer  ... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, values[i] );
		}
	}
	
	class RW extends Rsize implements Consumer {
		
		public RW( int length ) {
			super( length );
			size = 0;
		}
		
		public static RW oF(  Integer  ... values ) {
			RW dst = new RW( values.length );
			filL( dst, values );
			return dst;
		}
		
		public static RW of( int... values ) {
			RW dst = new RW( values.length );
			fill( dst, values );
			return dst;
		}
		
		public void fit() {
			values.fit();
			nulls.fit();
		}
		
		public RW clone()          { return (RW) super.clone(); }
		
		public Consumer consumer() {return this; }
		
		public void remove()       { remove( size - 1 ); }
		
		public void remove( int index ) {
			if (size < 1 || size <= index) return;
			
			size--;
			
			if (nulls.get( index )) values.remove( nulls.rank( index ) - 1 );
			nulls.remove( index );
		}
		
		public boolean add(  Integer   value ) {
			if (value == null) size++;
			else add( (int) (value + 0) );
			
			return true;
		}
		
		public boolean add( int value ) {
			values.add( value );
			nulls.add( size, true );
			++size;
			return true;
		}
		
		
		public void add( int index,  Integer   value ) {
			if (value == null)
			{
				nulls.add( index, false );
				size++;
			}
			else add( index, (int) (value + 0) );
		}
		
		public void add( int index, int value ) {
			if (index < size)
			{
				nulls.add( index, true );
				values.add( nulls.rank( index ) - 1, value );
				++size;
			}
			else set( index, value );
		}
		
		public void set(  Integer   value )            { set( this, size, value ); }
		
		public void set( int value )                {set( this, size, value ); }
		
		
		public void set( int index,  Integer   value ) { set( this, index, value ); }
		
		public void set( int index, int value )     {set( this, index, value ); }
		
		
		public void set( int index, int... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, values[i] );
		}
		
		public void seT( int index,  Integer  ... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, values[i] );
		}
		
		
		public int addAll( Producer src ) {
			int fix = size;
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
				if (src.hasValue( tag )) size++;
				else
					add( src.value( tag ) );
			return size - fix;
		}
		
		public void clear() {
			values.clear();
			nulls.clear();
			size = 0;
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
			
			int v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
	}
}
