package org.unirail.collections;

public interface FloatLongMap{
	
	interface Consumer {
		boolean put( float key, long value );//return false to interrupt
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		float key( int tag );
		
		long value( int tag );
	}
	
	
	class R implements Cloneable, Comparable<R> {
		public FloatList.RW keys   = new FloatList.RW( 0 );
		public LongList.RW values = new LongList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean hasOKey;
		long       OKeyValue;
		
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
			
			keys.allocate( size );
			values.allocate( size );
		}
		
		
		public long get( int tag ) {
			
			if (tag == assigned) return OKeyValue;
			return   values.array[tag];
		}
		
		
		public int tag( float key ) {
			if (key == 0) return hasOKey ? assigned : -1;
			
			int slot = hashKey( key ) & mask;
			
			for (float key_ = Float.floatToIntBits( key ), k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return -1;
		}
		
		
		public boolean isEmpty()                 { return size() == 0; }
		
		public int size()                        { return assigned + (hasOKey ? 1 : 0); }
		
		protected int hashKey( float key ) {return Array.hashKey( key ); }
		
		
		public int hashCode() {
			int h = hasOKey ? 0xDEADBEEF : 0;
			
			float k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash( k ) + Array.hash( values.array[i] );
			
			return h;
		}
		
		
	
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() { return (0 < assigned ? keys.array.length : 0) - (hasOKey ? 0 : 1); }
				
				public int tag( int tag ) { while (-1 < --tag) if (keys.array[tag] != 0) return tag; return -1; }
				
				public float key( int tag ) {return assigned == 0 || tag == keys.array.length ? (float) 0 :  keys.array[tag]; }
				
				public long value( int tag ) {return assigned == 0 || tag == keys.array.length ? OKeyValue :  values.array[tag]; }
			} : producer;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			
			if (hasOKey != other.hasOKey ||
			    hasOKey && OKeyValue != other.OKeyValue) return 1;
			
			int diff;
			if ((diff = size() - other.size()) != 0) return diff;
			
			
			float           k;
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
	}
	
	
	class RW extends R implements Consumer {
		
		public Consumer consumer() {return this; }
		
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
			
			final float[] k = keys.array;
			final long[] v = values.array;
			
			keys.allocate( size + 1 );
			values.allocate( size + 1 );
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (k == null || assigned < 1) return;
			
			
			float key;
			for (int i = k.length - 1; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = hashKey(  key  ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot]   = key;
					values.array[slot] = v[i];
				}
		}
		
		public boolean put( float key, long value ) {
			
			if (key == 0)
			{
				if (hasOKey)
				{
					OKeyValue = value;
					return true;
				}
				hasOKey   = true;
				OKeyValue = value;
				return false;
			}
			
			
			int slot = hashKey( key ) & mask;
			
			final float key_ = Float.floatToIntBits( key);
			
			for (float k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.array[slot] = value;
					return true;
				}
			
			keys.array[slot]   =            key_;
			values.array[slot] =  value;
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return false;
		}
		
		
		public boolean remove( float key ) {
			
			if (key == 0)
				if (hasOKey)
				{
					hasOKey = false;
					return true;
				}
				else return false;
			
			int slot = hashKey( key ) & mask;
			
			final float key_ = Float.floatToIntBits( key);
			
			for (float k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					float kk;
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
			hasOKey  = false;
			
			keys.clear();
			values.clear();
		}
		
		public RW clone() { return (RW) super.clone(); }
		
		
	}
}
