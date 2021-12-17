package org.unirail.collections;


import org.unirail.Hash;

public interface ObjectObjectMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K extends Comparable<? super K>, V extends Comparable<? super V>> int token( R<K, V> src, int token ) {
			for (; ; )
				if (++token == src.keys.array.length) return INIT;
				else if (src.keys.array[token] != null) return token;
		}
		
		static <K extends Comparable<? super K>, V extends Comparable<? super V>> K key( R<K, V> src, int token )   {return src.keys.array[token];}
		
		static <K extends Comparable<? super K>, V extends Comparable<? super V>> V value( R<K, V> src, int token ) {return src.values.get( token );}
	}
	
	abstract class R<K extends Comparable<? super K>, V extends Comparable<? super V>> implements Cloneable, Comparable<R<K, V>> {
		
		public ObjectList.RW<K> keys   = new ObjectList.RW<>( 0 );
		public ObjectList.RW<V> values = new ObjectList.RW<>( 0 );
		
		
		protected int assigned;
		
		
		protected int mask;
		
		
		protected int resizeAt;
		
		
		protected boolean hasNullKey;
		
		V NullKeyValue = null;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token( K key ) {
			if (key == null) return hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE;
			
			int slot = Hash.code( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains( K key )           {return !hasNone( token( key ) );}
		
		public boolean hasNone( int token )        {return token == Positive_Values.NONE;}
		
		public V value( @Positive_ONLY int token ) {return token == Positive_Values.VALUE ? NullKeyValue : values.array[token];}
		
		
		public int size()                          {return assigned + (hasNullKey ? 1 : 0);}
		
		
		public boolean isEmpty()                   {return size() == 0;}
		
		
		public int hashCode() {
			int hash = Hash.code( hasNullKey ? Hash.code( NullKeyValue ) : 10100011 );
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
			     hash = Hash.code( Hash.code( hash, NonNullKeysIterator.key( this, token ) ), NonNullKeysIterator.value( this, token ) );
			return hash;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R<K, V> other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R<K, V> other ) {
			if (other == null) return -1;
			if (size() != other.size()) return size() - other.size();
			
			if (hasNullKey != other.hasNullKey) return 1;
			
			int diff;
			if (hasNullKey)
				if (NullKeyValue != null && other.NullKeyValue != null)
				{if ((diff = NullKeyValue.compareTo( other.NullKeyValue )) != 0) return diff;}
				else if (NullKeyValue != other.NullKeyValue) return 243248;
			
			K key;
			
			for (int i = keys.array.length, token; -1 < --i; )
				if ((key = keys.array[i]) != null)
					if ((token = other.token( key )) == -1) return 1;
					else if (values.array[i] != null && other.value( token ) != null) {if ((diff = other.value( token ).compareTo( values.array[i] )) != 0) return diff;}
					else if (values.array[i] != other.value( token )) return 8;
			
			return 0;
		}
		
		
		@SuppressWarnings("unchecked")
		public R<K, V> clone() {
			try
			{
				R<K, V> dst = (R<K, V>) super.clone();
				
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			if (hasNullKey) dst.append( "null -> " ).append( NullKeyValue );
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
			     dst.append( NonNullKeysIterator.key( this, token ) ).append( " -> " ).append( NonNullKeysIterator.value( this, token ) );
			return dst;
		}
	}
	
	class RW<K extends Comparable<? super K>, V extends Comparable<? super V>> extends R<K, V> {
		
		public RW( double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
		}
		
		public RW( int expectedItems ) {this( expectedItems, 0.75 );}
		
		
		public RW( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			long length = (long) Math.ceil( expectedItems / this.loadFactor );
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys.length( size );
			values.length( size );
		}
		
		
		public boolean put( K key, V value ) {
			
			if (key == null)
			{
				boolean h = hasNullKey;
				hasNullKey   = true;
				NullKeyValue = value;
				return h != hasNullKey;
			}
			
			
			int slot = Hash.code( key ) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					values.array[slot] = value;
					return true;
				}
			
			keys.array[slot]   = key;
			values.array[slot] = value;
			
			if (assigned++ == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			
			keys.clear();
			values.clear();
		}
		
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length( -size );
				if (values.length() < size) values.length( -size );
				return;
			}
			
			final K[] ks = this.keys.array;
			final V[] vs = this.values.array;
			
			keys.length( -size );
			values.length( -size );
			
			K k;
			for (int i = ks.length; -1 < --i; )
				if ((k = ks[i]) != null)
				{
					int slot = Hash.code( k ) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot]   = k;
					values.array[slot] = vs[i];
				}
		}
		
		public boolean remove( K key ) {
			if (key == null)
			{
				boolean h = hasNullKey;
				hasNullKey = false;
				return h != hasNullKey;
			}
			
			int slot = Hash.code( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys.array[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - Hash.code( kk ) & mask) >= distance)
						{
							values.array[gapSlot] = values.array[slot1];
							keys.array[gapSlot] = kk;
							                      gapSlot = slot1;
							                      distance = 0;
						}
					
					keys.array[gapSlot]   = null;
					values.array[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW<K, V> clone() {return (RW<K, V>) super.clone();}
	}
}
	