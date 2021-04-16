package org.unirail.collections;


public interface FloatNullList {
	interface Consumer {
		boolean add( float value );
		
		boolean add(  Float     value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag )       {return tag != -1;}
		
		float value( int tag );
		
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
		FloatList.RW values = new FloatList.RW( 4 );
		
		
		protected R( int length ) {
			values.length( length );
			nulls.length( length );
		}
		
		public static R of(  Float    ... values ) {
			R dst = new R( values.length );
			filL( dst, values );
			return dst;
		}
		
		protected static void filL( R dst,  Float    ... values ) {
			for ( Float     value : values)
				if (value == null) dst.size++;
				else
				{
					dst.values.add( (float) (value + 0) );
					dst.nulls.set1( dst.size );
					dst.size++;
				}
		}
		
		public static R oF( float... values ) {
			R dst = new R( values.length );
			fill( dst, values );
			return dst;
		}
		
		protected static void fill( R dst, float... values ) {
			for (float value : values) dst.values.add( (float) value );
			
			dst.size = values.length;
			
			dst.nulls.set1( 0, dst.size - 1 );
		}
		
		public int length() {return values.length();}
		
		int size = 0;
		
		public int size()                  { return size; }
		
		public boolean isEmpty()           { return size < 1; }
		
		
		public int tag( int index )        {return nulls.get( index ) ? index : (Integer.MIN_VALUE | index);}
		
		public float get( int tag ) {return  values.array[nulls.rank( tag ) - 1]; }
		
		public boolean hasValue( int tag ) { return -1 < tag; }
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? R.this.tag( 0 ) : -1; }
				
				public int tag( int tag ) { return (tag &= Integer.MAX_VALUE) < size - 1 ? R.this.tag( tag + 1 ) : -1; }
				
				public float value( int tag ) {return  values.array[nulls.rank( tag ) - 1]; }
				
			} : producer;
		}
		
		public int indexOf() { return nulls.next0( 0 ); }
		
		public int indexOf( float value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		public int lastIndexOf() { return nulls.prev0( size );}
		
		public int lastIndexOf( float value ) {
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
		
		protected static void set( R dst, int index,  Float     value ) {
			
			if (value == null)
			{
				if (dst.size <= index) dst.size = index + 1;
				if (!dst.nulls.get( index )) return;
				dst.nulls.remove( index );
				dst.values.remove( dst.nulls.rank( index ) );
			}
			else set( dst, index, (float) (value + 0) );
		}
		
		protected static void set( R dst, int index, float value ) {
			if (dst.size <= index) dst.size = index + 1;
			
			if (dst.nulls.get( index )) dst.values.set( dst.nulls.rank( index ) - 1, value );
			else
			{
				dst.nulls.set1( index );
				dst.values.add( dst.nulls.rank( index ) - 1, value );
			}
		}
	}
	
	class Rsize extends R {
		
		public Rsize( int items ) {
			super( items );
			size = items;
		}
		
		public static Rsize of(  Float    ... values ) {
			Rsize dst = new Rsize( values.length );
			filL( dst, values );
			return dst;
		}
		
		public static Rsize oF( float... values ) {
			Rsize dst = new Rsize( values.length );
			fill( dst, values );
			return dst;
		}
		
		public void set( int index,  Float     value ) { if (index < size) set( this, index, value ); }
		
		public void set( int index, float value )     { if (index < size) set( this, index, value ); }
		
		public void seT( int index, float... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
			     set( this, index + i, (float) values[i] );
		}
		
		public void set( int index,  Float    ... values ) {
			for (int i = 0, max = Math.min( values.length, size - index ); i < max; i++)
				if (values[i] == null) set( this, index + i, null );
				else set( this, index + i, (float) (values[i] + 0) );
		}
	}
	
	class RW extends R implements Consumer {
		
		public RW( int items ) { super( items ); }
		
		public static RW of(  Float    ... values ) {
			RW dst = new RW( values.length );
			filL( dst, values );
			return dst;
		}
		
		public static RW oF( float... values ) {
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
		
		public boolean add(  Float     value ) {
			if (value == null) size++;
			else add( (float) (value + 0) );
			
			return true;
		}
		
		public boolean add( float value ) {
			values.add( value );
			nulls.add( size, true );
			size++;
			return true;
		}
		
		public void add( int index,  Float     value ) {
			if (value == null)
			{
				nulls.add( index, false );
				size++;
			}
			else add( index, (float) (value + 0) );
		}
		
		public void add( int index, float value ) {
			if (index < size)
			{
				nulls.add( index, true );
				values.add( nulls.rank( index ) - 1, value );
				size++;
			}
			else set( index, value );
		}
		
		public void set(  Float     value )            { set( this, size, value ); }
		
		public void set( float value )                {set( this, size, value ); }
		
		public void set( int index,  Float     value ) { set( this, index, value ); }
		
		public void set( int index, float value )     {set( this, index, value ); }
		
		public void seT( int index, float... values ) {
			for (int i = 0, max = values.length; i < max; i++)
			     set( this, index + i, (float) values[i] );
		}
		
		public void set( int index,  Float    ... values ) {
			for (int i = 0, max = values.length; i < max; i++)
				if (values[i] == null) R.set( this, index + i, null );
				else set( this, index + i, (float) (values[i] + 0) );
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
			
			float v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
	}
}
