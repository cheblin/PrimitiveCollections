package org.unirail.collections;


import java.util.Arrays;

public interface ByteObjectMap {
	interface Consumer<V extends Comparable<? super V>> {
		boolean put( byte key, V value );
		
		boolean put( Byte key, V value );
	}
	
	
	interface Producer<V extends Comparable<? super V>> {
		
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		byte key( int tag );
		
		V value( int tag );
		
		boolean hasNullKey();
		
		V nullKeyValue();
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			return dst;
		}
	}
	
	class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>> {
		
		ByteSet.RW       keys = new ByteSet.RW();
		ObjectList.RW<V> values;
		
		public R() {this( 8 );}
		
		public R( int length ) {
			values = new ObjectList.RW<>( 265 < length ? 256 : length );
		}
		
		boolean hasNull   = false;
		V       NullValue = null;
		
		public int size()                           { return keys.size(); }
		
		public boolean isEmpty()                    { return keys.isEmpty();}
		
		public boolean contains( byte key ) { return keys.contains(  key ); }
		
		public V get( byte key )                    {return keys.contains(  key ) ? values.array[keys.rank(   key ) - 1] : null;}
		
		private Producer<V> producer;
		
		public Producer<V> producer() {
			return producer == null ? producer = new Producer<V>() {
				public int tag() { return keys.tag(); }
				
				public int tag( int tag ) {return keys.tag( tag );}
				
				public byte key( int tag ) { return (byte) (tag >>> 8); }
				
				public V value( int tag ) { return values.array[tag & 0xFF]; }
				
				public boolean hasNullKey() {return hasNull;}
				
				public V nullKeyValue() {return NullValue;}
				
			} : producer;
		}
		
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R<V> other ) {
			if (other == null) return -1;
			int diff;
			if ((diff = keys.compareTo( other.keys )) != 0 || (diff = values.compareTo( other.values )) != 0) return diff;
			if (keys.hasNull && NullValue != other.NullValue) return 1;
			
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
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( keys.size() * 10 );
			else dst.ensureCapacity( dst.length() + keys.size() * 10 );
			
			
			final Producer<V> src = producer();
			if (src.hasNullKey()) dst.append( "null -> " ).append( src.nullKeyValue() ).append( '\n' );
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
			     dst.append( src.key( tag ) )
					     .append( " -> " )
					     .append( src.value( tag ) ).append( '\n' );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW<V extends Comparable<? super V>> extends R<V> implements Consumer<V> {
		
		public RW() {
		}
		
		public RW( int length ) {
			super( length );
		}
		
		
		public boolean put(  Byte      key, V value ) {
			if (key != null) return put( (byte) (key + 0), value );
			
			keys.add( null );
			NullValue = value;
			return true;
		}
		
		public boolean put( byte key, V value ) {
			keys.add( key );
			values.array[keys.rank( (byte) key ) - 1] = value;
			return true;
		}
		
		public boolean remove(  Byte       key ) { return key == null ? keys.remove( null ) : remove( (byte) (key + 0) ); }
		
		public boolean remove( byte key ) {
			final byte k = (byte) key;
			if (!keys.contains( k )) return false;
			
			values.remove( keys.rank( k ) - 1 );
			keys.remove( k );
			
			return true;
		}
		
		
		public void clear() {
			if (keys.size < 1) return;
			Arrays.fill( values.array, null );
			keys.clear();
		}
		
		public Consumer<V> consumer() {return this; }
		
		public RW<V> clone()          { return (RW<V>) super.clone(); }
	}
}
