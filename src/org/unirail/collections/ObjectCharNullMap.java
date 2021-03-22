package org.unirail.collections;


import java.util.Arrays;

public interface ObjectCharNullMap {
	interface Consumer<K extends Comparable<? super K>> {
		boolean put( K key, char value );
		
		boolean put( K key,  Character value );
	}
	
	
	interface Producer<K extends Comparable<? super K>> {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag )       {return tag != -1;}
		
		K key( int tag );
		
		char  value( int tag );
		
		default boolean hasValue( int tag ) { return -1 < tag; }
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); dst.append( '\n' ), tag = tag( tag ))
			{
				dst.append( key( tag ) ).append( " -> " );
				
				if (hasValue( tag ))dst.append( value( tag ) );
				else dst.append( "null" );
			}
			
			return dst;
		}
	}
	
	
	class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>> {
		
		
		ObjectList.RW<K> keys = new ObjectList.RW<>( 0 );
		
		CharNullList.RW values = new CharNullList.RW( 0 );
		
		
		protected int assigned;
		
		
		protected int mask;
		
		
		protected int resizeAt;
		
		
		@Nullable int hasNull;
		char NullValue = 0;
		
		protected double loadFactor;
		
		
		public R()                    { this( 4 ); }
		
		
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
		
		public int size()              { return assigned + (hasNull == Nullable.NONE ? 0 : 1); }
		
		public boolean isEmpty()       { return size() == 0; }
		
		protected int hashKey( K key ) { return Array.hashKey( key.hashCode() ); }
		
		public int hashCode() {
			int h = hasNull == Nullable.VALUE ? 0xDEADBEEF : 0;
			K   k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != null)
					h += Array.hash( k ) + Array.hash( values.hashCode() );
			return h;
		}
		
		private Producer<K> producer;
		
		
		public Producer<K> producer() {
			return producer == null ? producer = new Producer<>() {
				
				public int tag() {
					int len = keys.array.length;
					switch (hasNull)
					{
						case Nullable.VALUE: return len;
						case Nullable.NULL: return Integer.MIN_VALUE | len;
					}
					return 0 < assigned ? tag( len ) : -1;
				}
				
				public int tag( int tag ) {
					tag &= Integer.MAX_VALUE;
					while (-1 < --tag)
						if (keys.array[tag] != null)
							return values.nulls.contains( tag ) ? tag : tag | Integer.MIN_VALUE;
					return -1;
				}
				
				public K key( int tag ) {return (tag &= Integer.MAX_VALUE) < keys.array.length ? keys.array[tag] : null; }
				
				public char value( int tag ) {return tag < keys.array.length ? values.get( values.tag( tag ) ) : NullValue; }
				
			} : producer;
		}
		
		
		public @Nullable int tag( K key ) {
			
			if (key == null) return hasNull;
			
			int slot = hashKey( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return (slot = values.tag( slot )) == -1 ? Nullable.NULL : slot;
			
			return Nullable.NONE;//the key is not present
		}
		
		public boolean hasValue( @Nullable int tag ) {return -1 < tag; }
		
		public char get( @Nullable int tag ) { return tag == Nullable.VALUE ? NullValue : values.get( tag ); }
		
		
		public boolean contains( int tag )           {return tag != Nullable.NONE;}
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R<K> other ) {
			if (other == null) return -1;
			
			if (hasNull != other.hasNull ||
			    hasNull == Nullable.VALUE && NullValue != other.NullValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			
			K key;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = keys.array[i]) != null)
					if (values.nulls.contains( i ))
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
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
		
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> implements Consumer<K> {
		
		public RW() {
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
			assigned = 0;
			hasNull  = Nullable.NONE;
			keys.clear();
			values.clear();
		}
		
		
		//put key -> null
		public boolean put( K key,  Character value ) {
			if (value != null) put( key, (char) value );
			
			if (key == null)
			{
				hasNull = Nullable.NULL;
				return true;
			}
			
			int slot = hashKey( key ) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					values.set( slot,null );
					return true;
				}
			
			keys.array[slot] = key;
			values.set( slot, null );
			
			if (++assigned == resizeAt) this.allocate( mask + 1 << 1 );
			
			return true;
		}
		
		public boolean put( K key, char value ) {
			
			if (key == null)
			{
				
				hasNull   = Nullable.VALUE;
				NullValue = value;
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
			values.set( slot, value );
			
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
			
			CharNullList.RW vals = values;
			values = new CharNullList.RW( size + 1 );
			
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
					if (tag < 0) values.set( slot,null );
					else values.set( slot, vals.get( tag ) );
				}
			
		}
		
		
		public boolean remove( K key ) {
			if (key == null)
			{
				hasNull = Nullable.NONE;
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
							
							if (values.nulls.contains( s ))
								values.set( gapSlot, values.get( s ) );
							else
								values.set( gapSlot,null );
							
							gapSlot  = s;
							distance = 0;
						}
					
					array[gapSlot] = null;
					values.set( gapSlot, null );
					assigned--;
					return true;
				}
			
			return false;
		}
		
		
	}
	
}
	
	