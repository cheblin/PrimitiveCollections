package org.unirail.collections;


public interface ObjectByteMap {
	
	interface Consumer<K extends Comparable<? super K>> {
		boolean put( K key, byte value );
	}
	
	
	interface Producer<K extends Comparable<? super K>> {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		K key( int tag );
		
		byte  value( int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( key( tag ) )
					     .append( " -> " )
					     .append( value( tag ) ).append( '\n' );
			return dst;
		}
	}
	
	class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>> {
		
		public ObjectList.RW<K> keys;
		
		public ByteList.RW values = new ByteList.RW( 0 );
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNull;
		
		byte NullValue = 0;
		
		protected double loadFactor;
		
		
		public R( ObjectList.RW<K> keys ) {
			this( keys, 4 );
		}
		
		
		public R( ObjectList.RW<K> keys, double loadFactor ) {
			
			this.keys       = keys;
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			
		}
		
		public R( ObjectList.RW<K> keys, int expectedItems ) {
			this( keys, expectedItems, 0.75 );
		}
		
		
		public R( ObjectList.RW<K> keys, int expectedItems, double loadFactor ) {
			this( keys, loadFactor );
			
			long length = (long) Math.ceil( expectedItems / loadFactor );
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys.length( size );
			values.length( size );
		}
		
		
		public int tag( K key ) {
			if (key == null) return hasNull ? Integer.MAX_VALUE : -1;
			
			int slot = hashKey( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return slot;
			
			return -1;
		}
		
		public byte get( int tag ) {return tag == Integer.MAX_VALUE ? NullValue :  (byte) values.array[tag]; }
		
		public boolean contains( int tag ) {return -1 < tag;}
		
		
		public int size()                  { return assigned + (hasNull ? 1 : 0); }
		
		public boolean isEmpty()           { return size() == 0; }
		
		
		protected int hashKey( K key )     { return Array.hashKey( key.hashCode() ); }
		
		public int hashCode() {
			int h = hasNull ? 0xDEADBEEF : 0;
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
		
		public int compareTo( R<K> other ) {
			if (other == null) return -1;
			
			if (hasNull != other.hasNull ||
			    hasNull && NullValue != other.NullValue) return 1;
			
			int diff;
			if ((diff = size() - other.size()) != 0) return diff;
			
			
			K key;
			for (int i = keys.array.length - 1, tag; -1 < i; i--)
				if ((key = keys.array[i]) != null)
					if ((tag = other.tag( key )) == -1 || values.array[i] != other.get( tag )) return 1;
			
			return 0;
		}
		
		@SuppressWarnings("unchecked")
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
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
				
				public K key( int tag ) {return assigned == 0 || tag == keys.array.length ? null : keys.array[tag]; }
				
				
				public byte value( int tag ) {return assigned == 0 || tag == keys.array.length ? NullValue : (byte) values.array[tag]; }
				
			} : producer;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( assigned * 10 );
			else dst.ensureCapacity( dst.length() + assigned * 10 );
			
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	abstract class RW<K extends Comparable<? super K>> extends R<K> implements Consumer<K> {
		
		public RW( ObjectList.RW<K> keys ) {
			super( keys );
		}
		
		public RW( ObjectList.RW<K> keys, double loadFactor ) {
			super( keys, loadFactor );
		}
		
		public RW( ObjectList.RW<K> keys, int expectedItems ) {
			super( keys, expectedItems );
		}
		
		public RW( ObjectList.RW<K> keys, int expectedItems, double loadFactor ) {
			super( keys, expectedItems, loadFactor );
		}
		
		public void clear() {
			assigned = 0;
			hasNull  = false;
			keys.clear(); ;
			values.clear();
		}
		
		public boolean put( K key, byte value ) {
			
			if (key == null)
			{
				hasNull   = true;
				NullValue = value;
				return true;
			}
			
			int slot = hashKey( key ) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					values.array[slot] =  value;
					return true;
				}
			
			keys.array[slot]   =            key;
			values.array[slot] =  value;
			
			if (assigned++ == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		protected void allocate( int size ) {
			
			
			if (assigned < 1)
			{
				resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
				mask     = size - 1;
				
				if (keys.length() < size) keys.length( size );
				else keys.clear();
				
				if (values.length() < size) values.length( size );
				else values.clear();
				return;
			}
			
			final K[]           k = keys.array;
			final byte[] v = values.array;
			
			keys.length( size + 1 );
			values.length( size + 1 );
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			K kk;
			for (int i = k.length - 1; 0 <= --i; )
				if ((kk = k[i]) != null)
				{
					int slot = hashKey( kk ) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot]   = kk;
					values.array[slot] = v[i];
				}
		}
		
		public boolean remove( K key ) {
			if (key == null) return hasNull && !(hasNull = false);
			
			int slot = hashKey( key ) & mask;
			
			final K[] array = keys.array;
			
			for (K k; (k = array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					byte[] vals = values.array;
					
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != null; )
						if ((s - hashKey( kk ) & mask) >= distance)
						{
							vals[gapSlot] = vals[s];
							array[gapSlot] = kk;
							                 gapSlot = s;
							                 distance = 0;
						}
					
					array[gapSlot] = null;
					vals[gapSlot]  = (byte) 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW<K> clone()          { return (RW<K>) super.clone(); }
		
		public Consumer<K> consumer() {return this; }
	}
	
}
	