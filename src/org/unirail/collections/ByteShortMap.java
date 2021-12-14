package org.unirail.collections;


import org.unirail.Hash;

public interface ByteShortMap {
	
	
	interface NonNullKeysIterator {
		int END = -1;
		
		static int token( R src ) {return token( src, END );}
		
		static int token( R src, int token ) {
			int i = (token & ~0xFF) + (1 << 8);
			return i | ByteSet.NonNullKeysIterator.token( src.keys, token );
		}
		
		static byte key( int token ) {return (byte) (token & 0xFF);}
		
		static short value( R src, int token ) {return (short) src.values.array[token >> 8];}
	}
	
	
	abstract class R implements Cloneable, Comparable<R> {
		
		ByteSet.RW         keys = new ByteSet.RW();
		ShortList.RW values;
		
		
		public int size()                           {return keys.size();}
		
		public boolean isEmpty()                    {return keys.isEmpty();}
		
		public boolean contains(  Byte      key ) {return key == null ? keys.contains( null ) : keys.contains( (byte) (key + 0) );}
		
		public boolean contains( int key )          {return keys.contains( (byte) key );}
		
		public short value(  Byte      key ) {return key == null ? NullKeyValue : value( key + 0 );}
		
		public short  value( int key ) {return  (short)  values.array[keys.rank( (byte) key )];}
		
		short NullKeyValue = 0;
		
		public int hashCode() {return Hash.code( Hash.code( contains( null ) ? Hash.code( NullKeyValue ) : 77415193, keys ), values );}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		@Override public int compareTo( R other ) {
			if (other == null) return -1;
			
			int diff;
			if ((diff = other.keys.compareTo( keys )) != 0 || (diff = other.values.compareTo( values )) != 0) return diff;
			if (keys.hasNullKey && NullKeyValue != other.NullKeyValue) return 1;
			
			return 0;
		}
		
		@Override public R clone() {
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
		
		
		//endregion
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = keys.size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			if (keys.hasNullKey) dst.append( "null -> " ).append( NullKeyValue ).append( '\n' );
			
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ))
			     dst.append( NonNullKeysIterator.key( token ) )
					     .append( " -> " )
					     .append( NonNullKeysIterator.value( this, token ) )
					     .append( '\n' );
			return dst;
		}
	}
	
	class RW extends R {
		
		public RW( int length ) {values = new ShortList.RW( 265 < length ? 256 : length );}
		
		public void clear() {
			keys.clear();
			values.clear();
		}
		
		
		public boolean put(  Byte      key, short value ) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains( null );
				keys.add( null );
				return !ret;
			}
			
			return put( (byte) (key + 0), value );
		}
		
		public boolean put( byte key, short value ) {
			boolean ret = keys.add( (byte) key );
			values.array[keys.rank( (byte) key ) - 1] = (short) value;
			return ret;
		}
		
		public boolean remove(  Byte       key ) {return key == null ? keys.remove( null ) : remove( (byte) (key + 0) );}
		
		public boolean remove( byte key ) {
			final byte k = (byte) key;
			if (!keys.contains( k )) return false;
			
			values.remove( keys.rank( k ) - 1 );
			keys.remove( k );
			
			return true;
		}
		
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
