package org.unirail.collections;

import java.util.Arrays;

public interface LongObjectMap {
	
	public interface Consumer<V extends Comparable<? super V>> {
		boolean put( long key, V value );//return false to interrupt
	}
	
	
	public interface Producer<V extends Comparable<? super V>> {
		
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		default boolean isNull( int tag ) { return tag < 0; }
		
		long key( int tag );
		
		V value( int tag );
		
	}
	
	
	public static class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>> {
		
		
		public LongList.RW keys = new LongList.RW( 0 );
		
		public ObjectList.RW<V> values = new ObjectList.RW<>();
		
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		
		boolean hasOKey;
		V       OKeyValue;
		
		
		protected double loadFactor;
		
		
		public R( double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
		}
		
		public R( int expectedItems ) {
			this( expectedItems, 0.75 );
		}
		
		
		public R( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys.allocate( size );
			values.allocate( size );
		}
		
		
		public V get( long key ) {
			if (key == 0) return hasOKey ? OKeyValue : null;
			
			int slot = hashKey( key ) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return values.array[slot];
			
			return null;
		}
		
		
		public boolean contains( long key ) {
			if (key == 0) return hasOKey;
			
			int slot = hashKey( key ) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + (hasOKey ? 1 : 0); }
		
		
		public int hashCode() {
			int         h = hasOKey ? 0xDEADBEEF : 0;
			long k;
			
			for (int i = mask; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash( k ) + Array.hash( values.array[i] );
			
			return h;
		}
		
		
		protected int hashKey( long key ) { return Array.hash( key ); }
		
		
		private Producer<V> producer;
		
		public Producer<V> producer() {
			return producer == null ? producer = new Producer<>() {
				
				public int tag() {
					int i = keys.array.length;
					if (hasOKey) return OKeyValue == null ? Integer.MIN_VALUE | i : i;
					
					if (0 < assigned)
						while (-1 < --i)
							if (keys.array[i] != 0)
								return values.array[i] == null ? i | Integer.MIN_VALUE : i;
					
					return -1;
					
				}
				
				public int tag( int tag ) {
					tag &= Integer.MAX_VALUE;
					while (-1 < --tag)
						if (keys.array[tag] != 0)
							return values.array[tag] == null ? tag | Integer.MIN_VALUE : tag;
					return -1;
				}
				
				public long key( int tag ) {return (tag &= Integer.MAX_VALUE) == keys.array.length ? (long) 0 :  keys.array[tag]; }
				
				public V value( int tag ) {return tag == keys.array.length ? OKeyValue : values.array[tag]; }
				
				
			} : producer;
		}
		
		
		@SuppressWarnings("unchecked") public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R<V> other ) {
			
			if (other == null) return -1;
			if (assigned != other.assigned) return assigned - other.assigned;
			if (other.hasOKey != hasOKey) return 1;
			
			int diff;
			if (hasOKey && (diff = OKeyValue.compareTo( other.OKeyValue )) != 0) return diff;
			
			
			V           v;
			long k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					if (values.array[i] == null && other.get( k ) != null || (v = other.get( k )) == null) return -1;
					else if ((diff = v.compareTo( values.array[i] )) != 0) return diff;
			
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
			
			if (dst == null) dst = new StringBuilder( assigned * 10 );
			else dst.ensureCapacity( dst.length() + assigned * 10 );
			
			Producer<V> src = producer();
			for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
			     dst.append( src.key( tag ) )
					     .append( " -> " )
					     .append( src.value( tag ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	public static class RW<V extends Comparable<? super V>> extends R<V> implements Consumer<V> {
		
		
		public RW( double loadFactor ) {
			super( loadFactor );
		}
		
		public RW( int expectedItems ) {
			super( expectedItems );
		}
		
		public RW( int expectedItems, double loadFactor ) {
			super( expectedItems, loadFactor );
		}
		
		public Consumer<V> consumer() {return this; }
		
		
		public boolean put( long key, V value ) {
			if (key == 0)
			{
				hasOKey   = true;
				OKeyValue = value;
				return true;
			}
			
			
			int slot = hashKey( key ) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					values.array[slot] = value;
					return true;
				}
			
			keys.array[slot]   =  key;
			values.array[slot] =            value;
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public V remove( long key ) {
			
			if (key == 0)
				if (hasOKey)
				{
					hasOKey = false;
					V v = OKeyValue;
					OKeyValue = null;
					return v;
				}
				else return null;
			
			int slot = hashKey( key ) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					long kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hashKey( kk ) & mask) >= distance)
						{
							
							keys.array[gapSlot]   =  kk;
							values.array[gapSlot] = values.array[s];
							                                   gapSlot = s;
							                                   distance = 0;
						}
					
					keys.array[gapSlot]   = 0;
					values.array[gapSlot] = null;
					assigned--;
					return v;
				}
			
			return null;
		}
		
		public void clear() {
			assigned = 0;
			hasOKey  = false;
			
			Arrays.fill( keys.array,  0 );
			Arrays.fill( values.array, null );
		}
		
		void allocate( int size ) {
			
			if (assigned < 1)
			{
				resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
				mask     = size - 1;
				
				if (keys.length() < size) keys.allocate( size );
				else keys.clear();
				
				if (values.length() < size) values.allocate( size );
				else values.clear();
				
				return;
			}
			
			
			final long[] k = keys.array;
			final V[]           v = values.array;
			
			keys.allocate( size + 1 );
			values.allocate( size + 1 );
			
			mask     = size - 1;
			resizeAt = Math.min( mask, (int) Math.ceil( size * loadFactor ) );
			
			if (k == null || isEmpty()) return;
			
			
			long key;
			for (int from = k.length - 1; 0 <= --from; )
				if ((key = k[from]) != 0)
				{
					int slot = hashKey( key ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot]   =  key;
					values.array[slot] = v[from];
				}
		}
		
		
		public RW<V> clone() { return (RW<V>) super.clone(); }
	}
}
