package org.unirail.collections;


public interface ObjectObjectMap {
	
	interface Consumer<K extends Comparable<? super K>, V extends Comparable<? super V>> {
		boolean put( K key, V value );//return false to interrupt
	}
	
	interface Producer<K extends Comparable<? super K>, V extends Comparable<? super V>> {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		K key( int tag );
		
		V value( int tag );
	}
	
	class R<K extends Comparable<? super K>, V extends Comparable<? super V>> implements Cloneable, Comparable<R<K, V>> {
		
		public ObjectList.RW<K> keys   = new ObjectList.RW<>();
		public ObjectList.RW<V> values = new ObjectList.RW<>();
		
		
		protected int assigned;
		
		
		protected int mask;
		
		
		protected int resizeAt;
		
		
		protected boolean hasNullKey;
		
		V NullKeyValue = null;
		
		protected double loadFactor;
		
		
		public R( double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
		}
		
		public R( int expectedItems ) {
			this( expectedItems, 0.75 );
		}
		
		
		public R( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			long length = (long) Math.ceil( expectedItems / this.loadFactor );
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys.allocate( size );
			values.allocate( size );
		}
		
		public int tag( K key ) {
			if (key == null) return hasNullKey ? Integer.MAX_VALUE : -1;
			
			int slot = hashKey( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return slot;
			
			return -1;
		}
		
		public V get( int tag )           {return tag == Integer.MAX_VALUE ? NullKeyValue : values.array[tag]; }
		
		public boolean contains( K key ) {return -1 < tag( key );}
		
		
		public int size()                { return assigned + (hasNullKey ? 1 : 0); }
		
		
		public boolean isEmpty()         { return size() == 0; }
		
		
		protected int hashKey( K key )   { return Array.hashKey( key.hashCode() ); }
		
		public int hashCode() {
			int h = hasNullKey ? 0xDEADBEEF : 0;
			K   k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != null)
					h += Array.hash( k ) + Array.hash( values.array[i] );
			return h;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R<K, V> other ) {
			if (other == null) return -1;
			if (size() != other.size()) return size() - other.size();
			
			if (hasNullKey != other.hasNullKey) return 1;
			
			int diff;
			if (hasNullKey)
				if (NullKeyValue == null) {if (other.NullKeyValue != null) return -1;}
				else if ((diff = NullKeyValue.compareTo( other.NullKeyValue )) != 0) return diff;
			
			K key;
			
			for (int i = keys.array.length - 1, tag; -1 < i; i--)
				if ((key = keys.array[i]) != null)
					if ((tag = other.tag( key )) == -1 || values.array[i] != other.get( tag )) return 1;
			
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
		
		private Producer<K, V> producer;
		
		public Producer<K, V> producer() {
			return producer == null ? producer = new Producer<>() {
				
				public int tag() { return (0 < assigned ? keys.array.length : 0) - (hasNullKey ? 0 : 1); }
				
				public int tag( int tag ) { while (-1 < --tag) if (keys.array[tag] != null) return tag; return -1; }
				
				public K key( int tag ) {return assigned == 0 || tag == keys.array.length ? null : keys.array[tag]; }
				
				public V value( int tag ) {return assigned == 0 || tag == keys.array.length ? (hasNullKey ? NullKeyValue : null) : values.array[tag]; }
				
			} : producer;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( assigned * 10 );
			else dst.ensureCapacity( dst.length() + assigned * 10 );
			
			Producer<K, V> src = producer();
			for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
			     dst.append( src.key( tag ) )
					     .append( " -> " )
					     .append( src.value( tag ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW<K extends Comparable<? super K>, V extends Comparable<? super V>> extends R<K, V> implements Consumer<K, V> {
		
		
		public RW( double loadFactor ) {
			super( loadFactor );
		}
		
		public RW( int expectedItems ) {
			super( expectedItems );
		}
		
		public RW( int expectedItems, double loadFactor ) {
			super( expectedItems, loadFactor );
		}
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			
			keys.clear();
			values.clear();
		}
		
		public boolean put( V value ) {
			hasNullKey   = true;
			NullKeyValue = value;
			return true;
		}
		
		public boolean put( K key, V value ) {
			
			if (key == null) return put( value );
			
			
			int slot = hashKey( key ) & mask;
			
			
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
		
		protected void allocate( int arraySize ) {
			
			final K[] k = this.keys.array;
			final V[] v = this.values.array;
			
			keys.allocate( arraySize + 1 );
			values.allocate( arraySize + 1 );
			
			resizeAt = Math.min( arraySize - 1, (int) Math.ceil( arraySize * loadFactor ) );
			mask     = arraySize - 1;
			
			if (k == null || isEmpty()) return;
			
			K kk;
			for (int from = k.length - 1; 0 <= --from; )
				if ((kk = k[from]) != null)
				{
					int slot = hashKey( kk ) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot]   = kk;
					values.array[slot] = v[from];
				}
		}
		
		public V remove( K key ) {
			if (key == null)
			{
				hasNullKey = false;
				return NullKeyValue;
			}
			
			int slot = hashKey( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys.array[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - hashKey( kk ) & mask) >= distance)
						{
							values.array[gapSlot] = values.array[slot1];
							keys.array[gapSlot] = kk;
							                      gapSlot = slot1;
							                      distance = 0;
						}
					
					keys.array[gapSlot]   = null;
					values.array[gapSlot] = null;
					assigned--;
					return v;
				}
			
			return null;
		}
		
		public RW<K, V> clone()          { return (RW<K, V>) super.clone(); }
		
		public Consumer<K, V> consumer() {return this; }
	}
}
	