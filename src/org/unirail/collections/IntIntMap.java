package org.unirail.collections;

public interface IntIntMap {
	
	interface Consumer {
		boolean put( int key, int value );
		
		boolean put(  Integer   key, int value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		int key( int tag );
		
		int value( int tag );
		
		boolean hasNullKey();
		
		int nullKeyValue();
		
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
	
	
	class R implements Cloneable, Comparable<R> {
		public IntList.RW keys   = new IntList.RW( 0 );
		public IntList.RW values = new IntList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNull;
		int       NullValue;
		
		boolean hasO;
		int       OValue;
		
		protected double loadFactor;
		
		public R() {
			this( 4 );
		}
		
		
		public R( int expectedItems ) {
			this( expectedItems, 0.75 );
		}
		
		
		public R( int expectedItems, double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys.length( size );
			values.length( size );
		}
		
		public boolean isEmpty()                        { return size() == 0; }
		
		public int size()                               { return assigned + (hasO ? 1 : 0) + (hasNull ? 1 : 0); }
		
		
		public @Nullable int tag(  Integer   key ) {return key == null ? hasNull ? Nullable.NULL : Nullable.NONE : tag( (int) key );}
		
		public int tag( int key ) {
			if (key == 0) return hasO ? Nullable.VALUE : Nullable.NONE;
			
			int slot = hashKey( key ) & mask;
			
			for (int key_ =  key , k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return -1;
		}
		
		public boolean contains( @Nullable int tag ) {return tag != Nullable.NONE;}
		
		
		public int get( @Nullable int tag ) {
			if (tag == Nullable.NULL) return NullValue;
			if (tag == Nullable.VALUE) return OValue;
			return   values.array[tag];
		}
		
		
		protected int hashKey( int key ) {return Array.hashKey( key ); }
		
		
		public int hashCode() {
			int h = hasO ? 0xDEADBEEF : 0;
			
			int k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash( k ) + Array.hash( values.array[i] );
			
			return h;
		}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return (0 < assigned ? keys.array.length : 0) - (hasO ? 0 : 1); }
				
				public int tag( int tag ) { while (-1 < --tag) if (keys.array[tag] != 0) return tag; return -1; }
				
				public int key( int tag ) {return assigned == 0 || tag == keys.array.length ? (int) 0 :  keys.array[tag]; }
				
				public int value( int tag ) {return assigned == 0 || tag == keys.array.length ? OValue :  values.array[tag]; }
				
				public boolean hasNullKey() {return hasNull;}
				
				public int nullKeyValue() {return NullValue;}
			} : producer;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		public int compareTo( R other ) {
			if (other == null) return -1;
			
			if (hasO != other.hasO || hasO && OValue != other.OValue) return 1;
			
			int diff;
			if ((diff = size() - other.size()) != 0) return diff;
			
			
			int           k;
			for (int i = keys.array.length - 1, ii; -1 < i; i--)
				if ((k =  keys.array[i]) != 0)
					if ((ii = other.tag( k )) < 0 || other.values.array[ii] != values.array[i]) return 1;
			
			return 0;
		}
		
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				
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
			
			if (dst == null) dst = new StringBuilder( keys.size() * 10 );
			else dst.ensureCapacity( dst.length() + keys.size() * 10 );
			return producer().toString( dst );
			
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	
	class RW extends R implements Consumer {
		
		public Consumer consumer() {return this; }
		
		void allocate( int size ) {
			
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
			
			final int[] k = keys.array;
			final int[] v = values.array;
			
			keys.length( size + 1 );
			values.length( size + 1 );
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (k == null || assigned < 1) return;
			
			
			int key;
			for (int i = k.length - 1; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = hashKey(  key  ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot]   = key;
					values.array[slot] = v[i];
				}
		}
		
		public boolean put(  Integer   key, int value ) {
			if (key != null) return put( (int) key, value );
			
			hasNull   = true;
			NullValue = value;
			
			return true;
		}
		
		public boolean put( int key, int value ) {
			
			if (key == 0)
			{
				if (hasO)
				{
					OValue = value;
					return true;
				}
				hasO   = true;
				OValue = value;
				return false;
			}
			
			
			int slot = hashKey( key ) & mask;
			
			final int key_ =  key;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.array[slot] =(int)value;
					return true;
				}
			
			keys.array[slot]   =            key_;
			values.array[slot] = (int)value;
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return false;
		}
		
		
		public boolean remove(  Integer   key ) {
			if (key == null)
				if (hasNull)
				{
					hasNull = false;
					return true;
				}
				else return false;
			
			return remove( (int) key );
		}
		
		public boolean remove( int key ) {
			
			if (key == 0) return hasO && !(hasO = false);
			
			int slot = hashKey( key ) & mask;
			
			final int key_ =  key;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					int kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hashKey(  kk  ) & mask) >= distance)
						{
							
							keys.array[gapSlot]   = kk;
							values.array[gapSlot] = values.array[s];
							                        gapSlot = s;
							                        distance = 0;
						}
					
					keys.array[gapSlot]   = 0;
					values.array[gapSlot] = 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			hasO     = false;
			hasNull  = false;
			
			keys.clear();
			values.clear();
		}
		
		public RW clone() { return (RW) super.clone(); }
		
		
	}
}
