package org.unirail.collections;

public interface LongSet {
	
	class R implements Cloneable, Comparable<R> {
		
		public LongList.RW keys = new LongList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasOKey;
		
		
		protected double loadFactor;
		
		
		public R()                    { this( 4 ); }
		
		
		public R( int expectedItems ) { this( expectedItems, 0.75 ); }
		
		public R( double loadFactor ) { this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D ); }
		
		
		public R( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys.allocate( size );
		}
		
		public R( long... items ) {
			this( items.length );
			for (long i : items) add( this, i );
		}
		
		private static void allocate( R src, int size ) {
			if (src.assigned < 1)
			{
				src.resizeAt = Math.min( size - 1, (int) Math.ceil( size * src.loadFactor ) );
				src.mask     = size - 1;
				
				if (src.keys.length() < size) src.keys.allocate( size );
				else src.keys.clear();
				
				return;
			}
			
			
			final long[] k = src.keys.array;
			
			src.keys.allocate( size + 1 );
			
			src.mask     = size - 1;
			src.resizeAt = Math.min( src.mask, (int) Math.ceil( size * src.loadFactor ) );
			
			if (k == null || src.isEmpty()) return;
			
			
			long key;
			for (int from = k.length - 1; 0 <= --from; )
				if ((key = k[from]) != 0)
				{
					int slot = src.hashKey( key ) & src.mask;
					while (src.keys.array[slot] != 0) slot = slot + 1 & src.mask;
					src.keys.array[slot] = key;
				}
		}
		
		private static void add( R src, long key ) {
			
			if (key == 0)
			{
				src.hasOKey = true;
				return;
			}
			
			int slot = src.hashKey( key ) & src.mask;
			
			for (long k; (k = src.keys.array[slot]) != 0; slot = slot + 1 & src.mask)
				if (k == key) return;
			
			src.keys.array[slot] =  key;
			
			if (src.assigned++ == src.resizeAt) allocate( src, src.mask + 1 << 1 );
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
			
			for (int slot = mask; slot >= 0; slot--)
				if ((k = keys.array[slot]) != 0)
					h += Array.hash( k );
			
			return h;
		}
		
		protected int hashKey( long key ) {return Array.hashKey( key );}
		
		private LongList.Producer producer;
		
		public LongList.Producer producer() {
			return producer == null ? producer = new LongList.Producer() {
				
				public int tag() { return (0 < assigned ? keys.array.length : 0) - (hasOKey ? 0 : 1); }
				
				public int tag( int tag ) { while (-1 < --tag) if (keys.array[tag] != 0) return tag; return -1; }
				
				public long value( int tag ) {return assigned == 0 || tag == keys.array.length ? (long) 0 :   keys.array[tag]; }
			} : producer;
		}
		
		
		public long[] toArray( long[] dst ) {
			final int size = size();
			if (dst == null || dst.length < size) dst = new long[size];
			
			for (int i = keys.array.length - 1, ii = 0; 0 <= i; i--)
				if (keys.array[i] != 0) dst[ii++] = keys.array[i];
			
			return dst;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			if (other.assigned != assigned) return other.assigned - assigned;
			
			for (long k : keys.array) if (k != 0 && !other.contains( k )) return 1;
			
			return 0;
		}
		
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				
				dst.keys = keys.clone();
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
			
			LongList.Producer src = producer();
			for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
			     dst.append( src.value( tag ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	
	class RW extends R implements LongList.Consumer {
		public RW() {
			super();
		}
		
		public RW( int expectedItems ) {
			super( expectedItems );
		}
		
		public RW( double loadFactor ) {
			super( loadFactor );
		}
		
		public RW( int expectedItems, double loadFactor ) {
			super( expectedItems, loadFactor );
		}
		
		public RW( long... items ) {
			super( items );
		}
		
		public boolean add( long  value ) {
			R.add( this, value );
			return true;
		}
		
		public boolean remove( long key ) {
			
			if (key == 0) return hasOKey && !(hasOKey = false);
			
			int slot = hashKey( key ) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					long kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hashKey( kk ) & mask) >= distance)
						{
							keys.array[gapSlot] =  kk;
							                                 gapSlot = s;
							                                 distance = 0;
						}
					
					keys.array[gapSlot] = 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			hasOKey  = false;
			keys.clear();
		}
		
		public void retainAll( LongList.Consumer chk ) {
			
			long key;
			
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = keys.array[i]) != 0 && !chk.add( key )) remove( key );
			
			
			if (hasOKey && !chk.add( (long) 0 )) hasOKey = false;
			
		}
		
		public int removeAll( LongList.Producer src ) {
			int fix = size();
			
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag )) remove( src.value( tag ) );
			
			return fix - size();
		}
		
		public LongList.Consumer consumer() {return this; }
		
		public RW clone()                          { return (RW) super.clone(); }
		
		
	}
	
	
}