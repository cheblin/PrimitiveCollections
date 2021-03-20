package org.unirail.collections;

public interface ByteUByteMap {
	
	interface Consumer {
		boolean put( byte key, char value );//return false to interrupt
	}
	
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		byte key( int tag );
		
		char value( int tag );
	}
	
	
	class R implements Cloneable, Comparable<R> {
		
		ByteSet.RW         keys = new ByteSet.RW();
		ByteList.RW values;
		
		
		public int size()        { return keys.size; }
		
		public boolean isEmpty() { return keys.isEmpty();}
		
		public R()               {this( 8 );}
		
		public R( int length ) {
			values = new ByteList.RW( 265 < length ? 256 : length );
		}
		
		
		public int tag( byte key )       {return keys.contains( key ) ? key : key | Integer.MIN_VALUE;}
		
		public boolean exists( int tag ) { return -1 < tag; }
		
		public char  get( int tag ) {return  (char)( 0xFFFF &   values.array[keys.rank( (byte) tag )]);}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			int diff;
			if ((diff = other.keys.compareTo( keys )) != 0 ||
			    (diff = other.values.compareTo( values )) != 0) return diff;
			
			return 0;
		}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return 0 < values.size() ? 0 : -1; }
				
				public int tag( int tag ) {
					int key = (tag >> 16) + 1, index = (tag & 0xFFFF) + 1;
					
					long l;
					if (key < 128)
					{
						if (key < 64)
						{
							if ((l = keys._1 >>> key) != 0) return Long.numberOfTrailingZeros( l ) << 16 | index;
							key = 0;
						}
						else key -= 64;
						
						if ((l = keys._2 >>> key) != 0) return (Long.numberOfTrailingZeros( l ) + 64) << 16 | index;
						key = 128;
					}
					
					if (key < 192)
					{
						if ((l = keys._3 >>> (key -= 128)) != 0) return (Long.numberOfTrailingZeros( l ) + 128) << 16 | index;
						
						key = 0;
					}
					else key -= 192;
					
					if ((l = keys._4 >>> key) != 0) return (Long.numberOfTrailingZeros( l ) + 192) | index;
					
					return -1;
				}
				
				public byte key( int tag ) { return (byte) (tag >>> 16); }
				
				public char value( int tag ) { return (char)( 0xFFFF &  values.array[tag & 0xFFFF]); }
				
			} : producer;
		}
		
		
		public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				dst.keys   = keys.clone();
				dst.values = values.clone();
				
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( keys.size() * 10 );
			else dst.ensureCapacity( dst.length() + keys.size() * 10 );
			
			Producer src = producer();
			for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
			     dst.append( src.key( tag ) )
					     .append( " -> " )
					     .append( src.value( tag ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW extends R implements Consumer {
		
		public Consumer consumer() {return this; }
		
		public boolean put( byte key, char value ) {
			
			if (keys.size < 1)
			{
				values.resize( 0, 1, false );
				values.array[0] = (byte)value;
				keys.add( key );
				return false;
			}
			
			final int rank = keys.rank( key );
			
			if (-1 < rank)
			{
				values.array[rank] = (byte)value;
				return true;
			}
			
			values.resize( ~rank, 1, false );
			values.array[~rank] = (byte)value;
			
			return false;
		}
		
		public boolean remove( byte key ) {
			if (!keys.contains( key )) return false;
			
			int rank = keys.rank( key );
			
			values.resize( rank, -1, false );
			values.array[keys.size] = 0;
			
			keys.remove( key );
			
			return true;
		}
		
		
		public void clear() {
			if (keys.size < 1) return;
			keys.clear();
			values.clear();
		}
		
		public RW clone() { return (RW) super.clone(); }
		
	}
}
