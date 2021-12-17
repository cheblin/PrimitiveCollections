package org.unirail.collections;


import org.unirail.Hash;

public interface UByteObjectMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static <V extends Comparable<? super V>> int token( R<V> src, int token ) {return  ByteSet.NonNullKeysIterator.token( src.keys, token );}
		
		static <V extends Comparable<? super V>> char key( R<V> src, int token ) {return (char) ByteSet.NonNullKeysIterator.key( src.keys, token );}
		
		static <V extends Comparable<? super V>> V value( R<V> src, int token ) {return src.values.array[ByteSet.NonNullKeysIterator.index( src.keys, token )];}
	}
	
	abstract class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>> {
		
		ByteSet.RW       keys = new ByteSet.RW();
		ObjectList.RW<V> values;
		
		public int size()                           {return keys.size();}
		
		public boolean isEmpty()                    {return keys.isEmpty();}
		
		
		public boolean contains(  Character key ) {return key == null ? keys.contains( null ) : keys.contains( (byte) (key + 0) );}
		
		public boolean contains( int key )          {return keys.contains( (byte) key );}
		
		public V value(  Character key )          {return key == null ? NullKeyValue : value( key + 0 );}
		
		public V value( int key )                   {return values.array[keys.rank( (byte) key )];}
		
		V NullKeyValue = null;
		
		public int hashCode() {return Hash.code( Hash.code( Hash.code( keys.contains( null ) ? Hash.code( NullKeyValue ) : 29399999 ), keys ), values );}
		
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
			
			if (keys.hasNullKey) dst.append( "null -> " ).append( NullKeyValue ).append( '\n' );
			
			
			for (int token = NonNullKeysIterator.INIT; ( token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT;)
			     dst.append( NonNullKeysIterator.key(this, token ) )
					     .append( " -> " )
					     .append( NonNullKeysIterator.value( this, token ) )
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
		
		public boolean put(  Character key, V value ) {
			if (key == null)
			{
				NullKeyValue = value;
				return keys.add( null );
			}
			
			return put( (char) (key + 0), value );
		}
		
		public boolean put( char key, V value ) {
			boolean ret = keys.add( (byte) key );
			values.array[keys.rank( (byte) key ) - 1] = value;
			return ret;
		}
		
		public boolean remove(  Character  key ) {
			if (key == null)
			{
				NullKeyValue = null;
				return keys.remove( null );
			}
			return remove( (char) (key + 0) );
		}
		
		public boolean remove( char key ) {
			final byte k = (byte) key;
			if (!keys.contains( k )) return false;
			
			values.remove( keys.rank( k ) - 1 );
			keys.remove( k );
			
			return true;
		}
		
		
		public RW<V> clone() {return (RW<V>) super.clone();}
	}
}
