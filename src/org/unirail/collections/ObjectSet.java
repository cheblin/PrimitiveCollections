package org.unirail.collections;

import org.unirail.Hash;

public interface ObjectSet {
	
	
	interface Iterator {
		
		int token = -1;
		
		static <K extends Comparable<? super K>> int token( R<K> src, int token ) {for (; ; ) if (src.keys.array[++token] != null) return token;}
		
		static <K extends Comparable<? super K>> K key( R<K> src, int token )     {return src.keys.array[token];}
	}
	
	abstract class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>> {
		
		public ObjectList.RW<K> keys = new ObjectList.RW<>( 4 );
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNullKey;
		
		protected double loadFactor;
		
		
		public boolean contains( K key ) {
			if (key == null) return hasNullKey;
			int slot = Hash.code( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return true;
			
			return false;
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (hasNullKey ? 1 : 0);}
		
		public int hashCode() {
			int hash = hasNullKey ? 107 : 109;
			K    key;
			
			for (int i = keys.array.length; -1 < --i; )
				if ((key = keys.array[i]) != null) hash = Hash.code(hash, key );
			
			return hash;
		}
		
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R<K> other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R<K> other ) {
			if (other == null) return -1;
			if (other.assigned != assigned) return other.assigned - assigned;
			if (other.hasNullKey != hasNullKey) return 1;
			
			for (K k : keys.array) if (k != null && !other.contains( k )) return 1;
			
			return 0;
		}
		
		@SuppressWarnings("unchecked")
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				dst.keys = keys.clone();
				
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
			
			if (hasNullKey)
			{
				dst.append( "null\n" );
				size--;
			}
			
			for (int token = Iterator.token, i = 0; i < size; dst.append( '\n' ), i++)
			     dst.append( Iterator.key( this, token = Iterator.token( this, token ) ) );
			
			return dst;
		}
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> {
		public RW()                    {this( 4, 0.75f );}
		
		
		public RW( double loadFactor ) {this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );}
		
		
		public RW( int expectedItems ) {this( expectedItems, 0.75f );}
		
		public RW( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			long length = (long) Math.ceil( expectedItems / loadFactor );
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys.length( size );
			keys.length( size );
		}
		
		@SafeVarargs public RW( K... items ) {
			this( items.length );
			for (K key : items) add( key );
		}
		
		public boolean add( K key ) {
			if (key == null) return !hasNullKey && (hasNullKey = true);
			
			int slot = Hash.code( key ) & mask;
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return false;
			
			keys.array[slot] = key;
			if (assigned == resizeAt) this.allocate( mask + 1 << 1 );
			
			assigned++;
			return true;
		}
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			keys.clear();
		}
		
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length( -size );
				return;
			}
			
			final K[] k = keys.array;
			keys.length( -size );
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = Hash.code( key ) & mask;
					
					while (keys.array[slot] != null) slot = slot + 1 & mask;
					
					keys.array[slot] = key;
				}
			
		}
		
		
		public boolean remove( K key ) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = Hash.code( key ) & mask;
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys.array[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - Hash.code( kk ) & mask) >= distance)
						{
							keys.array[gapSlot] = kk;
							                      gapSlot = slot1;
							                      distance = 0;
						}
					
					keys.array[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW<K> clone() {return (RW<K>) super.clone();}
		
	}
}
	