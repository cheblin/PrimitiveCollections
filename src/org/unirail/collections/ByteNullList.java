package org.unirail.collections;


public interface ByteNullList {
	interface Consumer {
		boolean add( byte value );
		
		boolean add(  Byte      value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag )       {return tag != -1;}
		
		byte value( int tag );
		
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
		
		BitSet.RW          nulls  = new BitSet.RW();
		ByteList.RW values = new ByteList.RW( 4 );
		
		
		R( int length ) {
			if (length < 1) return;
			
			values.length( length );
			nulls.length( length );
		}
		
		public static R of(  Byte     ... values ) {
			R dst = new R( 0 );
			fill( dst, values );
			return dst;
		}
		
		static void fill( R dst,  Byte     ... values ) {
			dst.values.length( values.length );
			dst.nulls.length( values.length );
			
			for ( Byte      value : values)
				if (value == null) ++dst.size;
				else
				{
					dst.values.add( (byte) (value + 0) );
					dst.nulls.set1( dst.size );
					++dst.size;
				}
		}
		
		public static R of( byte... values ) {
			R dst = new R( values.length );
			fill( dst, values );
			return dst;
		}
		
		static void fill( R dst, byte... values ) {
			for (byte value : values) dst.values.add( value );
			
			dst.size = values.length;
			
			dst.nulls.length( dst.size );
			dst.nulls.set1( 0, dst.size - 1 );
		}
		
		public int length() {return values.length();}
		
		int size = 0;
		
		public int size()                  { return size; }
		
		public boolean isEmpty()           { return size < 1; }
		
		
		public int tag( int index )        {return nulls.get( index ) ? index : -1;}
		
		public byte get( int tag ) {return (byte) values.array[nulls.rank( tag ) - 1]; }
		
		public boolean hasValue( int tag ) { return -1 < tag; }
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? nulls.get( 0 ) ? 0 : Integer.MIN_VALUE : -1; }
				
				public int tag( int tag ) { return (tag &= Integer.MAX_VALUE) < size - 1 ? nulls.get( ++tag ) ? tag : Integer.MIN_VALUE | tag : -1; }
				
				public byte value( int tag ) {return (byte) values.array[nulls.rank( tag ) - 1]; }
				
			} : producer;
		}
		
		public int indexOf() { return nulls.next0( 0 ); }
		
		public int indexOf( byte value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		public int lastIndexOf() { return nulls.prev0( size );}
		
		public int lastIndexOf( byte value ) {
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
	}
	
	class Rsize extends R {
		
		Rsize( int length ) {
			super( length );
		}
		
		public static Rsize of(  Byte     ... values ) {
			Rsize dst = new Rsize( 0 );
			fill( dst, values );
			return dst;
		}
		
		public static Rsize of( byte... values ) {
			Rsize dst = new Rsize( values.length );
			fill( dst, values );
			return dst;
		}
		
		public boolean set( int index,  Byte      value ) {
			if (values.length() <= index) return false;
			if (value != null) return set( index, (byte) (value + 0) );
			
			if (!nulls.get( index )) return true;
			
			nulls.remove( index );
			values.remove( nulls.rank( index ) );
			
			return true;
		}
		
		public boolean set( int index, byte value ) {
			if (values.length() <= index) return false;
			
			if (nulls.get( index )) values.set( nulls.rank( index ) - 1, value );
			else
			{
				nulls.set1( index );
				values.add( nulls.rank( index ) - 1, value );
			}
			
			
			return true;
		}
	}
	
	class RW extends Rsize implements Consumer {
		
		public RW( int length ) {
			super( length );
		}
		
		public static RW of(  Byte     ... values ) {
			RW dst = new RW( 0 );
			fill( dst, values );
			return dst;
		}
		
		public static RW of( byte... values ) {
			RW dst = new RW( 0 );
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
		
		public boolean add(  Byte      value ) {
			if (value == null) size++;
			else add( (byte) (value + 0) );
			
			return true;
		}
		
		public boolean add( byte value ) {
			values.add( value );
			nulls.add( size, true );
			++size;
			return true;
		}
		
		
		public void add( int index,  Byte      value ) {
			if (value == null)
			{
				nulls.add( index, false );
				size++;
			}
			else add( index, (byte) (value + 0) );
		}
		
		public void add( int index, byte value ) {
			if (index < size)
			{
				nulls.add( index, true );
				values.add( nulls.rank( index ) - 1, value );
				++size;
			}
			else set( index, value );
		}
		
		public boolean set( int index,  Byte      value ) {
			
			if (value != null) return set( index, (byte) (value + 0) );
			
			if (!nulls.get( index )) return true;
			
			nulls.remove( index );
			values.remove( nulls.rank( index ) );
			
			return true;
		}
		
		public boolean set( int index, byte value ) {
			
			if (index < size)
				if (nulls.get( index )) values.set( nulls.rank( index ) - 1, value );
				else
				{
					nulls.set1( index );
					values.add( nulls.rank( index ) - 1, value );
				}
			else
			{
				nulls.set1( index );
				values.add( value );
				size = index + 1;
			}
			return true;
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
			
			byte v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
	}
}
