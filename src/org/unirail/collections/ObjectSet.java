package org.unirail.collections;

public interface ObjectSet {
	
	
	class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>> {
		
		
		public ObjectList.RW<K> keys = new ObjectList.RW<>();
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNullKey;
		
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
			
			keys.allocate( size );
			
			keys.allocate( size );
		}
		
		public R( K... items ) { this(  items.length ); for (K k : items) add( this, k ); }
		
		private static <K extends Comparable<? super K>> boolean add( R<K> src, K key ) {
			if (key == null) return !src.hasNullKey && (src.hasNullKey = true);
			
			int slot = src.hashKey( key ) & src.mask;
			for (K k; (k = src.keys.array[slot]) != null; slot = slot + 1 & src.mask)
				if (k.compareTo( key ) == 0) return false;
			
			src.keys.array[slot] = key;
			if (src.assigned == src.resizeAt) allocate( src, src.mask + 1 << 1 );
			
			src.assigned++;
			return true;
		}
		
		private static <K extends Comparable<? super K>> void allocate( R<K> src, int size ) {
			
			final K[] k = src.keys.array;
			
			src.mask = size - 1;
			src.keys.allocate( size + 1 );
			src.resizeAt = Math.min( src.mask, (int) Math.ceil( size * src.loadFactor ) );
			
			if (k == null || src.isEmpty()) return;
			
			K kk;
			for (int i = k.length - 1; --i >= 0; )
				if ((kk = k[i]) != null)
				{
					int slot = src.hashKey( kk ) & src.mask;
					
					while (src.keys.array[slot] != null)
						slot = slot + 1 & src.mask;
					
					src.keys.array[slot] = kk;
				}
			
		}
		
		
		public boolean contains( K key ) {
			if (key == null) return hasNullKey;
			int slot = hashKey( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return true;
			
			return false;
		}
		
		private ObjectList.Producer<K> producer;
		
		public ObjectList.Producer<K> producer() {
			return producer == null ? producer = new ObjectList.Producer<>() {
				
				public int tag() { return (0 < assigned ? keys.array.length : 0) - (hasNullKey ? 0 : 1); }
				
				public int tag( int tag ) { while (-1 < --tag) if (keys.array[tag] != null) return tag; return -1; }
				
				public K value( int tag ) {return assigned == 0 || tag == keys.array.length ? null : keys.array[tag]; }
				
			} : producer;
		}
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + (hasNullKey ? 1 : 0); }
		
		public int hashCode() {
			int h = hasNullKey ? 0xDEADBEEF : 0;
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
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( assigned * 10 );
			else dst.ensureCapacity( dst.length() + assigned * 10 );
			
			ObjectList.Producer<K> src = producer();
			for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
			     dst.append( src.value( tag ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> implements ObjectList.Consumer<K> {
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
		
		public RW( K... items ) {
			super( items );
		}
		
		public boolean add( K value ) { return R.add( this, value ); }
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			keys.clear();
		}
		
		
		public boolean remove( K key ) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
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
		
		public RW<K> clone()                     { return (RW<K>) super.clone(); }
		
		public ObjectList.Consumer<K> consumer() {return this; }
	}
}
	