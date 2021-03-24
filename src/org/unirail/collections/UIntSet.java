package org.unirail.collections;

public interface UIntSet {
	
	interface Consumer {
		boolean add( long value );
		
		boolean add(  Integer   key );
	}
	
	interface Producer {
		@Nullable int tag();
		
		@Nullable int tag( int tag );
		
		default boolean ok( @Nullable int tag ) {return tag != Nullable.NONE;}
		
		boolean hasNullKey();
		
		long  key( @Nullable int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			if (hasNullKey()) dst.append( "null\n" );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( key( tag ) ).append( '\n' );
			
			return dst;
		}
	}
	
	class R implements Cloneable, Comparable<R> {
		
		public UIntList.RW keys = new UIntList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean hasO;
		boolean hasNull;
		
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
			
			keys.length( size );
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
				
				if (src.keys.length() < size) src.keys.length( size );
				else src.keys.clear();
				
				return;
			}
			
			
			final int[] k = src.keys.array;
			
			src.keys.length( size + 1 );
			
			src.mask     = size - 1;
			src.resizeAt = Math.min( src.mask, (int) Math.ceil( size * src.loadFactor ) );
			
			if (k == null || src.isEmpty()) return;
			
			
			long key;
			for (int from = k.length - 1; 0 <= --from; )
				if ((key = k[from]) != 0)
				{
					int slot = src.hashKey( key ) & src.mask;
					while (src.keys.array[slot] != 0) slot = slot + 1 & src.mask;
					src.keys.array[slot] =(int) key;
				}
		}
		
		private static void add( R src, long key ) {
			
			if (key == 0)
			{
				src.hasO = true;
				return;
			}
			
			int slot = src.hashKey( key ) & src.mask;
			
			
			for (long k; (k = src.keys.array[slot]) != 0; slot = slot + 1 & src.mask)
				if (k == key) return;
			
			src.keys.array[slot] = (int) key;
			
			if (src.assigned++ == src.resizeAt) allocate( src, src.mask + 1 << 1 );
		}
		
		public boolean contains(  Integer   key ) { return key == null ? hasNull : contains( (long) (key + 0) ); }
		
		public boolean contains( long key ) {
			if (key == 0) return hasO;
			
			int slot = hashKey( key ) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty() { return size() == 0; }
		
		
		public int size()        { return assigned + (hasO ? 1 : 0); }
		
		
		public int hashCode() {
			int         h = hasO ? 0xDEADBEEF : 0;
			long k;
			
			for (int slot = mask; slot >= 0; slot--)
				if ((k = keys.array[slot]) != 0)
					h += Array.hash( k );
			
			return h;
		}
		
		protected int hashKey( long key ) {return Array.hashKey( key );}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				
				public int tag() { return (0 < assigned ? keys.array.length : 0) - (hasO ? 0 : 1); }
				
				public int tag( int tag ) { while (-1 < --tag) if (keys.array[tag] != 0) return tag; return -1; }
				
				public boolean hasNullKey() { return hasNull; }
				
				public long key( int tag ) {return assigned == 0 || tag == keys.array.length ? (long) 0 :   keys.array[tag]; }
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
			
			for (int k : keys.array) if (k != 0 && !other.contains( k )) return 1;
			
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
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	
	class RW extends R implements Consumer {
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
		
		public boolean add(  Integer   key ) {
			if (key == null) hasNull = true;
			else add( (long) key );
			return true;
		}
		
		public boolean add( long  value ) {
			R.add( this, value );
			return true;
		}
		
		
		public boolean remove(  Integer   key ) {
			if (key == null)
				if (hasNull)
				{
					hasNull = false;
					return true;
				}
				else return false;
			
			return remove( (long) key );
		}
		
		
		public boolean remove( long key ) {
			
			if (key == 0) return hasO && !(hasO = false);
			
			int slot = hashKey( key ) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					long kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hashKey( kk ) & mask) >= distance)
						{
							keys.array[gapSlot] = (int) kk;
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
			hasO     = false;
			keys.clear();
		}
		
		public void retainAll( Consumer chk ) {
			
			long key;
			
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = keys.array[i]) != 0 && !chk.add( key )) remove( key );
			
			
			if (hasO && !chk.add( (long) 0 )) hasO = false;
			
		}
		
		public int removeAll( UIntList.Producer src ) {
			int fix = size();
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) remove( src.value( tag ) );
			
			return fix - size();
		}
		
		public Consumer consumer() {return this; }
		
		public RW clone()          { return (RW) super.clone(); }
	}
}