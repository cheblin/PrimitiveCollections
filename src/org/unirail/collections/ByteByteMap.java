package org.unirail.collections;

public interface ByteByteMap {
	
	interface Consumer {
		boolean put( byte key, byte value );
		
		boolean put(  Byte      key, byte value );
	}
	
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		byte key( int tag );
		
		byte value( int tag );
		
		boolean hasNullKey();
		
		byte nullKeyValue();
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			if (hasNullKey()) dst.append( "null -> " ).append( nullKeyValue() ).append( '\n' );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( key( tag ) )
					     .append( " -> " )
					     .append( value( tag ) )
					     .append( '\n' );
			return dst;
		}
	}
	
	
	class R implements Cloneable, Comparable<R> {
		
		ByteSet.RW         keys = new ByteSet.RW();
		ByteList.RW values;
		
		byte NullValue = 0;
		
		
		public R() {this( 8 );}
		
		public R( int length ) {
			values = new ByteList.RW( 265 < length ? 256 : length );
		}
		
		public int size()                            { return keys.size(); }
		
		public boolean isEmpty()                     { return keys.isEmpty();}
		
		
		public @Nullable int tag(  Byte      key ) {return key == null ? keys.contains( null ) ? Nullable.NULL : Nullable.NONE : tag( (byte) (key + 0) );}
		
		public @Nullable int tag( byte key ) {return keys.contains( (byte) (key + 0) ) ? key : Nullable.NONE;}
		
		public boolean contains( int tag )           { return tag != -1; }
		
		public byte  get( int tag ) {return tag == Nullable.NULL ? NullValue :  (byte)  values.array[keys.rank( (byte) tag )];}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				
				public int tag() { return keys.tag(); }
				
				public int tag( int tag ) {return keys.tag( tag );}
				
				public byte key( int tag ) { return (byte) (tag >>> 8); }
				
				public byte value( int tag ) { return (byte) values.array[tag & 0xFF]; }
				
				public boolean hasNullKey() { return keys.hasNull; }
				
				public byte nullKeyValue() { return NullValue; }
				
			} : producer;
		}
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			
			int diff;
			if ((diff = other.keys.compareTo( keys )) != 0 || (diff = other.values.compareTo( values )) != 0) return diff;
			if (keys.hasNull && NullValue != other.NullValue) return 1;
			
			return 0;
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
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW extends R implements Consumer {
		
		public RW() {
		}
		
		public RW( int length ) {
			super( length );
		}
		
		
		public boolean put(  Byte      key, byte value ) {
			if (key != null) return put( (byte) (key + 0), value );
			
			keys.add( null );
			NullValue = value;
			return true;
		}
		
		public boolean put( byte key, byte value ) {
			keys.add( key + 0 );
			values.array[keys.rank( (byte) key ) - 1] =  value;
			return true;
		}
		
		public boolean remove(  Byte       key ) { return key == null ? keys.remove( null ) : remove( (byte) (key + 0) ); }
		
		public boolean remove( byte key ) {
			if (!keys.contains( (byte) key )) return false;
			
			values.remove( keys.rank( (byte) key ) - 1 );
			keys.remove( (byte) key );
			
			return true;
		}
		
		public void clear() {
			if (keys.size < 1) return;
			keys.clear();
			values.clear();
		}
		
		public Consumer consumer() {return this; }
		
		public RW clone()          { return (RW) super.clone(); }
	}
}
