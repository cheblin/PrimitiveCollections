package org.unirail.collections;


public interface ByteObjectMap {
	
	
	interface Iterator {
		
		int token = -1;
		
		static <V extends Comparable<? super V>> int token( R<V> src, int token ) {
			int i = (token & ~0xFF) + (1 << 8);
			return i | ByteSet.Iterator.token( src.keys, token );
		}
		
		static byte key( int token ) {return (byte) (token & 0xFF);}
		
		static <V extends Comparable<? super V>> V value( R<V> src, int token ) {return src.values.array[token >> 8];}
	}
	
	abstract class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>> {
		
		ByteSet.RW       keys = new ByteSet.RW();
		ObjectList.RW<V> values;
		
		public int size()                           {return keys.size();}
		
		public boolean isEmpty()                    {return keys.isEmpty();}
		
		
		public boolean contains(  Byte      key ) {return key == null ? keys.contains( null ) : keys.contains( (byte) (key + 0) );}
		
		public boolean contains( int key )          {return keys.contains( (byte) key );}
		
		public V value(  Byte      key )          {return key == null ? NullKeyValue : value( key + 0 );}
		
		public V value( int key )                   {return values.array[keys.rank( (byte) key )];}
		
		V NullKeyValue = null;
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R<V> other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R<V> other ) {
			if (other == null) return -1;
			int diff;
			if ((diff = keys.compareTo( other.keys )) != 0 || (diff = values.compareTo( other.values )) != 0) return diff;
			if (keys.hasNullKey && NullKeyValue != other.NullKeyValue) return 1;
			
			return 0;
		}
		
		
		@SuppressWarnings("unchecked")
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			return null;
		}
		
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			if (keys.hasNullKey)
			{
				dst.append( "null -> " ).append( NullKeyValue ).append( '\n' );
				size--;
			}
			
			for (int token = Iterator.token, i = 0; i < size; i++)
			     dst.append( Iterator.key( token = Iterator.token( this, token ) ) )
					     .append( " -> " )
					     .append( Iterator.value( this, token ) )
					     .append( '\n' );
			
			return dst;
		}
	}
	
	class RW<V extends Comparable<? super V>> extends R<V> {
		
		public RW( int length ) {values = new ObjectList.RW<>( 265 < length ? 256 : length );}
		
		public void clear() {
			NullKeyValue = null;
			values.clear();
			keys.clear();
		}
		
		public boolean put(  Byte      key, V value ) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains( null );
				keys.add( null );
				return !ret;
			}
			
			return put( (byte) (key + 0), value );
		}
		
		public boolean put( byte key, V value ) {
			boolean ret = keys.add( (byte) key );
			values.array[keys.rank( (byte) key ) - 1] = value;
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
		
		
		public RW<V> clone() {return (RW<V>) super.clone();}
	}
}
