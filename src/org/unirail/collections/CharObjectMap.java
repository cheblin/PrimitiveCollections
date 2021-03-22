package org.unirail.collections;

import java.util.Arrays;

public interface CharObjectMap {
	
	public interface Consumer<V extends Comparable<? super V>> {
		boolean put( char key, V value );//return false to interrupt
		
		boolean put(  Character key, V value );
		
	}
	
	
	public interface Producer<V extends Comparable<? super V>> {
		
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		
		boolean hasNullKey();
		
		V nullKeyValue();
		
		char key( int tag );
		
		V value( int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			if (hasNullKey()) dst.append( "null -> " ).append( nullKeyValue() ).append( '\n' );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( key( tag ) )
					     .append( " -> " )
					     .append( value( tag ) )
					     .append( '\n' );
			
			return dst;
		}
	}
	
	
	public static class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>> {
		
		
		public CharList.RW keys = new CharList.RW( 0 );
		
		public ObjectList.RW<V> values = new ObjectList.RW<>( 0 );
		
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		boolean hasNull;
		V       NullValue;
		
		
		boolean hasO;
		V       OValue;
		
		
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
		
		
		public V get( char key ) {
			if (key == 0) return hasO ? OValue : null;
			
			int slot = hashKey( key ) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return values.array[slot];
			
			return null;
		}
		
		
		public boolean contains( char key ) {
			if (key == 0) return hasO;
			
			int slot = hashKey( key ) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + (hasO ? 1 : 0) + (hasNull ? 1 : 0); }
		
		
		public int hashCode() {
			int         h = hasO ? 0xDEADBEEF : 0;
			char k;
			
			for (int i = mask; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash( k ) + Array.hash( values.array[i] );
			
			return h;
		}
		
		
		protected int hashKey( char key ) { return Array.hash( key ); }
		
		
		private Producer<V> producer;
		
		public Producer<V> producer() {
			return producer == null ? producer = new Producer<>() {
				
				public int tag() {
					int i = keys.array.length;
					if (hasO) return i;
					
					if (0 < assigned)
						while (-1 < --i)
							if (keys.array[i] != 0)
								return i;
					return -1;
				}
				
				public int tag( int tag ) {
					
					while (-1 < --tag)
						if (keys.array[tag] != 0)
							return tag;
					return -1;
				}
				
				public boolean hasNullKey() {return hasNull;}
				
				public V nullKeyValue() {return NullValue;}
				
				public char key( int tag ) {return tag < keys.array.length ?(char) keys.array[tag] : (char) 0; }
				
				public V value( int tag ) {return tag < keys.array.length ? values.array[tag] : OValue; }
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
			if (other.hasO != hasO) return 1;
			
			int diff;
			if (hasO && (diff = OValue.compareTo( other.OValue )) != 0) return diff;
			
			
			V           v;
			char k;
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
			return producer().toString( dst );
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
		
		
		public boolean put(  Character key, V value ) {
			if (key != null) return put( (char) key, value );
			
			hasNull   = true;
			NullValue = value;
			
			return true;
		}
		
		public boolean put( char key, V value ) {
			if (key == 0)
			{
				hasO   = true;
				OValue = value;
				return true;
			}
			
			
			int slot = hashKey( key ) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
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
		
		
		public boolean remove(  Character key ) {
			if (key == null)
				if (hasNull)
				{
					hasNull = false;
					return true;
				}
				else return false;
			
			return remove( (char) key );
		}
		
		public boolean remove( char key ) {
			
			if (key == 0) return hasO && !(hasO = false);
			
			int slot = hashKey( key ) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					char kk;
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
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned  = 0;
			hasO      = false;
			
			hasNull   = false;
			NullValue = null;
			
			keys.clear();
			values.clear();
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
			
			
			final char[] k = keys.array;
			final V[]           v = values.array;
			
			keys.allocate( size + 1 );
			values.allocate( size + 1 );
			
			mask     = size - 1;
			resizeAt = Math.min( mask, (int) Math.ceil( size * loadFactor ) );
			
			if (k == null || isEmpty()) return;
			
			
			char key;
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
