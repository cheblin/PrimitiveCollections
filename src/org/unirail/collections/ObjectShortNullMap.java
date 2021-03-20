package org.unirail.collections;


import java.util.Arrays;

public interface ObjectShortNullMap {
	interface Consumer<K extends Comparable<? super K>> {
		boolean put( K key, short value );
		
		boolean put( K key );
	}
	
	
	interface Producer<K extends Comparable<? super K>> {
		int tag();
		
		int tag( int tag );
		
		K key( int tag );
		
		short  value( int tag );
		
		default boolean isNull( int tag ) { return tag < 0; }
	}
	
	class Entry<K extends Comparable<? super K>> {
		final K               key;
		final  Short     value;
		
		public Entry( K key,  Short     value ) {
			this.key   = key;
			this.value = value;
		}
	}
	
	class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>> {
		
		
		public ObjectList.RW<K> keys = new ObjectList.RW<>();
		
		public ShortNullList.RW values = new ShortNullList.RW( 0 );
		
		
		protected int assigned;
		
		
		protected int mask;
		
		
		protected int resizeAt;
		
		
		@Nullable int hasNullKey;
		short NullKeyValue = 0;
		
		protected double loadFactor;
		
		
		public R() { this( 4 ); }
		
		public R( Entry<K>... items ) {
			this( items.length );
			for (Entry<K> item : items)
				put( item.key, item.value );
		}
		
		
		public R( double loadFactor ) {this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );}
		
		public R( int expectedItems ) {this( expectedItems, 0.75 );}
		
		
		public R( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			long length = (long) Math.ceil( expectedItems / loadFactor );
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys.allocate( size );
			values.nulls.allocate( size );
			values.values.allocate( size );
		}
		
		private Producer<K> producer;
		
		protected boolean put( K key, short value ) {
			
			if (key == null)
			{
				
				hasNullKey   = Nullable.VALUE;
				NullKeyValue = value;
				return true;
			}
			
			int slot = hashKey( key ) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					values.set( slot, value );
					return true;
				}
			
			keys.array[slot] = key;
			values.add( slot, value );
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		protected void allocate( int size ) {
			
			if (assigned < 1)
			{
				resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
				mask     = size - 1;
				
				if (keys.length() < size) keys.allocate( size );
				else Arrays.fill( keys.array, null );
				
				if (values.nulls.length() < size) values.nulls.allocate( size );
				else values.nulls.clear();
				
				if (values.values.length() < size) values.values.allocate( size );
				else values.values.clear();
				
				return;
			}
			
			ShortNullList.RW vals = values;
			values = new ShortNullList.RW( size + 1 );
			
			final K[] k = keys.array;
			
			keys.allocate( size + 1 );
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			K kk;
			for (int i = k.length - 1; 0 <= --i; )
				if ((kk = k[i]) != null)
				{
					int slot = hashKey( kk ) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot] = kk;
					int tag = vals.tag( slot );
					if (tag < 0) values.set( slot );
					else values.set( slot, vals.get( tag ) );
				}
			
		}
		
		
		public Producer<K> producer() {
			return producer == null ? producer = new Producer<>() {
				
				public int tag() {
					int i = keys.array.length;
					switch (hasNullKey)
					{
						case Nullable.VALUE: return i;
						case Nullable.NULL: return Integer.MIN_VALUE | i;
						default:
							if (0 < assigned)
								while (-1 < --i)
									if (keys.array[i] != null)
										return values.nulls.get( i ) ? i : i | Integer.MIN_VALUE;
							
							return -1;
					}
				}
				
				public int tag( int tag ) {
					tag &= Integer.MAX_VALUE;
					while (-1 < --tag)
						if (keys.array[tag] != null)
							return values.nulls.get( tag ) ? tag : tag | Integer.MIN_VALUE;
					return -1;
				}
				
				public K key( int tag ) {return (tag &= Integer.MAX_VALUE) == keys.array.length ? null :  keys.array[tag]; }
				
				public short value( int tag ) {return tag == keys.array.length ? NullKeyValue :  values.get( values.tag( tag ) ) ; }
				
			} : producer;
		}
		
		
		public int tag() { return hasNullKey == Nullable.VALUE ? Integer.MAX_VALUE : -1; }
		
		public int tag( K key ) {
			
			if (key == null) return tag();
			
			
			int slot = hashKey( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return slot;
			
			return -1;
		}
		
		public short get( int tag ) { return tag == Nullable.VALUE ? NullKeyValue : values.get( tag ); }
		
		
		public boolean contains( K key ) {return -1 < tag( key );}
		
		public int size()                { return assigned + (hasNullKey == Nullable.NONE ? 0 : 1); }
		
		
		public boolean isEmpty()         { return size() == 0; }
		
		
		protected int hashKey( K key )   { return Array.hashKey( key.hashCode() ); }
		
		public int hashCode() {
			int h = hasNullKey == Nullable.VALUE ? 0xDEADBEEF : 0;
			K   k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != null)
					h += Array.hash( k ) + Array.hash( values.hashCode() );
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
			
			if (hasNullKey != other.hasNullKey ||
			    hasNullKey == Nullable.VALUE && NullKeyValue != other.NullKeyValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			
			K key;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = keys.array[i]) != null)
					if (values.nulls.get( i ))
					{
						int tag = other.tag( key );
						if (tag == -1 || values.get( i ) != other.get( tag )) return 1;
					}
					else if (-1 < other.tag( key )) return 1;
			
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
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( assigned * 10 );
			else dst.ensureCapacity( dst.length() + assigned * 10 );
			
			Producer<K> src = producer();
			for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
			     dst.append( src.key( tag ) )
					     .append( " -> " )
					     .append( src.value( tag ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
		
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> implements Consumer<K> {
		
		public RW() {
		}
		
		public RW( Entry<K>... items ) {
			super( items );
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
		
		public RW<K> clone()          { return (RW<K>) super.clone(); }
		
		public Consumer<K> consumer() {return this; }
		
		public void clear() {
			assigned   = 0;
			hasNullKey = Nullable.NONE;
			keys.clear();
			values.clear();
		}
		
		
		//put key -> null
		public boolean put( K key ) {
			
			if (key == null)
			{
				hasNullKey = Nullable.NULL;
				return true;
			}
			
			int slot = hashKey( key ) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					values.set( slot );
					return true;
				}
			
			keys.array[slot] = key;
			values.addNull( slot );
			
			if (++assigned == resizeAt) this.allocate( mask + 1 << 1 );
			
			return true;
		}
		
		public boolean put( K key, short value ) { return super.put( key, value ); }
		
		
		public void remove()                   {hasNullKey = Nullable.NONE;}
		
		public boolean remove( K key ) {
			if (key == null)
			{
				hasNullKey = Nullable.NONE;
				return true;
			}
			
			int slot = hashKey( key ) & mask;
			
			final K[] array = keys.array;
			for (K k; (k = array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != null; )
						if ((s - hashKey( kk ) & mask) >= distance)
						{
							array[gapSlot] = kk;
							
							if (values.nulls.get( s ))
								values.set( gapSlot, values.get( s ) );
							else
								values.set( gapSlot );
							
							gapSlot  = s;
							distance = 0;
						}
					
					array[gapSlot] = null;
					values.set( gapSlot );
					assigned--;
					return true;
				}
			
			return false;
		}
	}
	
}
	
	