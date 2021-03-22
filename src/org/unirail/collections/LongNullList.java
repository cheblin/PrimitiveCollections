package org.unirail.collections;


public interface LongNullList {
	interface Consumer {
		boolean add( long value );
		
		boolean add(  Long      value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag )       {return tag != -1;}
		
		long value( int tag );
		
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
		
		IntBoolMap.RW      nulls  = new IntBoolMap.RW();
		LongList.RW values = new LongList.RW( 4 );
		
		public void fit() {
			values.fit();
			nulls.fit();
		}
		
		
		public R( int length ) {
			if (length < 1) return;
			
			values.allocate( length );
			nulls.allocate( length );
		}
		
		public R(  Long     ... values ) {
			
			this.values.allocate( values.length );
			nulls.allocate( values.length );
			for ( Long      value : values)
				if (value == null) ++size;
				else
				{
					this.values.add( (long) (value + 0) );
					nulls.set1( size );
					++size;
				}
		}
		
		
		int size = 0;
		
		public int size()                  { return size; }
		
		public boolean isEmpty()           { return size < 1; }
		
		
		public int tag( int index )        {return nulls.contains( index ) ? index : -1;}
		
		public long get( int tag ) {return  values.array[nulls.rank( tag ) - 1]; }
		
		public boolean hasValue( int tag ) { return -1 < tag; }
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < size ? nulls.contains( 0 ) ? 0 : Integer.MIN_VALUE : -1; }
				
				public int tag( int tag ) { return (tag &= Integer.MAX_VALUE) < size - 1 ? nulls.contains( ++tag ) ? tag : Integer.MIN_VALUE | tag : -1; }
				
				public long value( int tag ) {return  values.array[nulls.rank( tag ) - 1]; }
				
			} : producer;
		}
		
		public int indexOf() { return nulls.next0( 0 ); }
		
		public int indexOf( long value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.key( i );
		}
		
		public int lastIndexOf() { return nulls.prev0( size );}
		
		public int lastIndexOf( long value ) {
			int i = values.lastIndexOf( value );
			return i < 0 ? i : nulls.key( i );
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
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW extends R implements Consumer {
		
		public RW clone()          { return (RW) super.clone(); }
		
		public Consumer consumer() {return this; }
		
		public RW( int length ) {
			super( length );
		}
		
		public RW(  Long     ... values ) {
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
			
			if (nulls.contains( index )) values.remove( nulls.rank( index ) );
			nulls.del( index );
			
			return true;
		}
		
		public int addAll( Producer src, int count ) {
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
		
		public void swap( int bit1, int bit2 ) {
			
			int exist, empty;
			if (nulls.contains( bit1 ))
				if (nulls.contains( bit2 ))
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
			else if (nulls.contains( bit2 ))
			{
				exist = nulls.rank( bit2 ) - 1;
				empty = nulls.rank( bit1 );
				
				nulls.set1( bit1 );
				nulls.set0( bit2 );
			}
			else return;
			
			long v = values.get( exist );
			values.remove( exist );
			values.add( empty, v );
		}
		
		public boolean set( int index,  Long      value ) {
			
			if (value != null) return set( index, (long) (value + 0) );
			
			if (!nulls.contains( index )) return false;
			
			nulls.set0( index );
			
			values.remove( nulls.rank( index ) );
			
			return true;
		}
		
		public boolean set( int index, long value ) {
			
			if (nulls.contains( index ))
			{
				values.set( nulls.rank( index ) - 1, value );
				return false;
			}
			
			
			nulls.set1( index );
			values.add( nulls.rank( index ) - 1, value );
			
			return true;
		}
		
		
		public boolean add(  Long      value ) {
			if (value != null) return add( (long) (value + 0) );
			
			size++;
			return true;
		}
		
		public boolean add( long value ) {
			values.add( value );
			nulls.set1( size );
			++size;
			return true;
		}
		
		
		public boolean add( int index,  Long      value ) {
			if (value != null) return add( index, (long) (value + 0) );
			
			nulls.add( index, false );
			size++;
			return true;
		}
		
		public boolean add( int index, long value ) {
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
