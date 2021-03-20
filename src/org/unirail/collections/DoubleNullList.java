package org.unirail.collections;


public interface DoubleNullList {
	interface Consumer {
		boolean add( double value );
		
		boolean addNull();
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		double value( int tag );
		
		default boolean exists( int tag ) { return -1 < tag; }
	}
	
	
	class R implements Comparable<R> {
		
		BitSet.RW          nulls  = new BitSet.RW();
		DoubleList.RW values = new DoubleList.RW( 4 );
		
		public void fit() {
			values.fit();
			nulls.fit();
		}
		
		
		public R( int length ) {
			if (length < 1) return;
			
			values.allocate( length );
			nulls.allocate( length );
		}
		
		public R(  Double   ... values ) {
			
			this.values.allocate( values.length );
			nulls.allocate( values.length );
			for ( Double    value : values)
				if (value == null) ++size;
				else
				{
					this.values.add( value );
					nulls.set1( size );
					++size;
				}
		}
		
		
		int size = 0;
		
		public int size()                { return size; }
		
		public boolean isEmpty()         { return size < 1; }
		
		
		public int tag( int index )      {return nulls.get( index ) ? index : -1;}
		
		public double get( int tag ) {return  values.array[nulls.rank( tag ) - 1]; }
		
		public boolean exists( int tag ) { return -1 < tag; }
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return nulls.get( size - 1 ) ? size - 1 : size - 1 & Integer.MIN_VALUE; }
				
				public int tag( int tag ) { return (tag &= Integer.MAX_VALUE) == 0 ? -1 : nulls.get( --tag ) ? tag : tag | Integer.MIN_VALUE; }
				
				public double value( int tag ) {return  values.array[nulls.rank( tag ) - 1]; }
				
			} : producer;
		}
		
		public int indexOf() { return nulls.next0( 0 ); }
		
		public int indexOf( double value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		public int lastIndexOf() { return nulls.prev0( size );}
		
		public int lastIndexOf( double value ) {
			int i = values.lastIndexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		public R subList( int fromIndex, int toIndex ) {
			if (size <= fromIndex) return null;
			if (size - 1 < toIndex) toIndex = size - 1;
			if (toIndex == fromIndex) return null;
			
			long[] data = nulls.subList( fromIndex, toIndex );
			R      ret  = new R( 0 );
			ret.size = toIndex - fromIndex;
			
			if (data.length == 0) return ret;
			
			ret.nulls.set( data );
			
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
			
			for (int i = 0; i < size; dst.append( '\n' ), i++)
				if (nulls.get( i )) dst.append( get( i ) );
				else dst.append( "null" );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW extends R implements Consumer {
		
		public RW clone()          { return (RW) super.clone(); }
		
		public Consumer consumer() {return this; }
		
		public RW( int length ) {
			super( length );
		}
		
		public RW(  Double   ... values ) {
			super( values );
		}
		
		public boolean remove() {
			if (size < 1) return false;
			
			size--;
			if (nulls.size() <= size) return true;
			
			nulls.set0( size );
			values.remove();
			return true;
		}
		
		public boolean remove( int index ) {
			if (size - 1 < index) index = size - 1;
			
			size--;
			
			if (nulls.size() <= index) return false;
			
			if (nulls.get( index )) values.remove( nulls.rank( index ) );
			nulls.del( index );
			
			return true;
		}
		
		public int addAll( Producer src, int count ) {
			int fix = size;
			
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag ))
				if (src.exists( tag )) size++;
				else
					add( src.value( tag ) );
			return size - fix;
		}
		
		public void clear() {
			values.clear();
			nulls.clear();
			size = 0;
		}
		
		public void swap( int bit1, int bit2 ) {
			
			int exist, empty;
			if (nulls.get( bit1 ))
				if (nulls.get( bit2 ))
				{
					values.swap( nulls.rank( bit1 ) - 1, nulls.rank( bit2 ) - 1 );
					return;
				}
				else
				{
					exist = nulls.rank( bit1 ) - 1;
					empty = nulls.rank( bit2 );
					nulls.set0( bit1 );
					nulls.set1( bit2 );
				}
			else if (nulls.get( bit2 ))
			{
				exist = nulls.rank( bit2 ) - 1;
				empty = nulls.rank( bit1 );
				
				nulls.set1( bit1 );
				nulls.set0( bit2 );
			}
			else return;
			
			double v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
		
		
		public boolean set( int index, double value ) {
			
			if (nulls.get( index ))
			{
				values.set( nulls.rank( index ) - 1, value );
				return false;
			}
			
			
			nulls.set1(index);
			values.add( nulls.rank( index )-1, value );
			
			return true;
		}
		
		
		public boolean set( int index ) {
			
			if (!nulls.get( index )) return false;
			
			nulls.set0( index );
			
			values.remove( nulls.rank( index ) );
			
			return true;
		}
		
		
		public boolean addNull() {
			++size;
			return true;
		}
		
		//insert null in index
		public void addNull( int index ) {
			nulls.add( index, false );
			size++;
		}
		
		
		public boolean add( double value ) {
			values.add( value );
			nulls.set1( size );
			++size;
			return true;
		}
		
		
		public boolean add( int index, double value ) {
			if (index < size - 1)
			{
				nulls.add( index, true );
				values.add( nulls.rank( index ) - 1, value );
			}
			else add( value );
			
			return true;
		}
		
	}
}
