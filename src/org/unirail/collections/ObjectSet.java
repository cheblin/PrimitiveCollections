package org.unirail.collections;

public interface ObjectSet {
	
	interface Producer<K extends Comparable<? super K>> {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		K key( int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( key( tag ) ).append( '\n' );
			
			return dst;
		}
	}
	
	interface Consumer<V extends Comparable<? super V>> {
		boolean add( V key );
	}
	
	class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>> {
		
		public ObjectList.RW<K> keys = new ObjectList.RW<>( 4 );
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNull;
		
		protected double loadFactor;
		
		public R() {
			this( 4, 0.75f );
		}
		
		
		public R( double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
		}
		
		
		public R( int expectedItems ) {
			this( expectedItems, 0.75f );
		}
		
		public R( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			long length = (long) Math.ceil( expectedItems / loadFactor );
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys.length( size );
			
			keys.length( size );
		}
		
		
		public boolean contains( K key ) {
			if (key == null) return hasNull;
			int slot = hashKey( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return true;
			
			return false;
		}
		
		private Producer<K> producer;
		
		public Producer<K> producer() {
			return producer == null ? producer = new Producer<>() {
				
				public int tag() {
					int len = 0 < assigned ? keys.array.length : 0;
					return hasNull ? len : (tag( len ));
				}
				
				public int tag( int tag ) {
					while (-1 < --tag)
						if (keys.array[tag] != null)
							return tag;
					return -1;
				}
				
				public K key( int tag ) {return assigned < 1 || tag == keys.array.length ? null : keys.array[tag]; }
				
			} : producer;
		}
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + (hasNull ? 1 : 0); }
		
		public int hashCode() {
			int h = hasNull ? 0xDEADBEEF : 0;
			K   key;
			
			for (int slot = mask; 0 <= slot; slot--)
				if ((key = keys.array[slot]) != null) h += Array.hash( key.hashCode() );
			
			return h;
		}
		
		
		protected int hashKey( K key ) {return Array.hashKey( key ); }
		
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R<K> other ) {
			if (other == null) return -1;
			if (other.assigned != assigned) return other.assigned - assigned;
			if (other.hasNull != hasNull) return 1;
			
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
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( assigned * 10 );
			else dst.ensureCapacity( dst.length() + assigned * 10 );
			
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> implements Consumer<K> {
		public RW() {
			super();
		}
		
		public RW( double loadFactor ) {
			super( loadFactor );
		}
		
		public RW( int expectedItems ) {
			super( expectedItems );
		}
		
		public RW( int expectedItems, double loadFactor ) {
			super( expectedItems, loadFactor );
		}
		
		public boolean add( K key ) {
			if (key == null) return !hasNull && (hasNull = true);
			
			int slot = hashKey( key ) & mask;
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return false;
			
			keys.array[slot] = key;
			if (assigned == resizeAt) this.allocate( mask + 1 << 1 );
			
			assigned++;
			return true;
		}
		
		private void allocate( int size ) {
			
			final K[] k = keys.array;
			
			mask = size - 1;
			keys.length( size + 1 );
			resizeAt = Math.min( mask, (int) Math.ceil( size * loadFactor ) );
			
			if (k == null || isEmpty()) return;
			
			K kk;
			for (int i = k.length - 1; --i >= 0; )
				if ((kk = k[i]) != null)
				{
					int slot = hashKey( kk ) & mask;
					
					while (keys.array[slot] != null)
						slot = slot + 1 & mask;
					
					keys.array[slot] = kk;
				}
			
		}
		
		public void clear() {
			assigned = 0;
			hasNull  = false;
			keys.clear();
		}
		
		
		public boolean remove( K key ) {
			if (key == null) return hasNull && !(hasNull = false);
			
			int slot = hashKey( key ) & mask;
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys.array[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - hashKey( kk ) & mask) >= distance)
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
		
		public RW<K> clone()          { return (RW<K>) super.clone(); }
		
		public Consumer<K> consumer() {return this; }
	}
}
	