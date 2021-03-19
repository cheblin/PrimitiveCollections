package org.unirail.collections;

public interface UIntIntNullMap {
	public interface Consumer {
		boolean put( long key, int value );
		
		boolean put( long key );
	}
	
	
	public interface Producer {
		int tag();
		
		int tag( int tag );
		
		long key( int tag );
		
		int value( int tag );
		
		default boolean isNull( int tag ) { return tag < 0; }
	}
	
	public static class R implements Cloneable, Comparable<R> {
		public UIntList.RW     keys   = new UIntList.RW( 0 );
		public IntNullList.RW values = new IntNullList.RW( 0 );
		
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		
		@Nullable int hasOKey;
		int       OKeyValue;
		
		
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
			values.nulls.allocate( size );
			values.values.allocate( size );
		}
		
		
		public int tag( long key ) {
			
			if (key == 0) return  hasOKey == Nullable.VALUE ? Integer.MAX_VALUE : -1;
			
			final int key_ = (int) key ;
			
			int slot = hashKey( key ) & mask;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return values.tag( slot ) ;
			
			return -1;//the key is not present
		}
		
		public boolean contains( long key ) {return tag( key ) != -1;}
		
		
		public int get( int tag ) { return tag == Nullable.VALUE ? OKeyValue : values.get( tag ); }
		
		
		public boolean isEmpty()                   { return size() == 0; }
		
		public int size()                          { return assigned + (hasOKey == Nullable.NONE ? 0 : 1); }
		
		protected int hashKey( long key )   {return Array.hashKey( key ); }
		
		
		public int hashCode() {
			int h = hasOKey == Nullable.NONE ? 0xDEADBEEF : 0;
			
			int k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash(  k  ) + Array.hash( values.hashCode() );
			
			return h;
		}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				public int tag() {
					int i = keys.array.length;
					switch (hasOKey)
					{
						case Nullable.VALUE: return i;
						case Nullable.NULL: return Integer.MIN_VALUE | i;
						default:
							if (0 < assigned)
								while ( -1 < --i )
									if (keys.array[i] != 0)
										return values.nulls.get( i ) ? i : i | Integer.MIN_VALUE;
							
							return -1;
					}
				}
				
				public int tag( int tag ) {
					tag &= Integer.MAX_VALUE;
					while (-1 < --tag)
						if (keys.array[tag] != 0)
							return values.nulls.get( tag ) ? tag : tag | Integer.MIN_VALUE;
					return -1;
				}
				
				public long key( int tag ) {return (tag &= Integer.MAX_VALUE) == keys.array.length ? (long) 0 :   keys.array[tag]; }
				
				public int value( int tag ) {return tag == keys.array.length ? OKeyValue :  values.get( values.tag( tag ) ) ; }
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
			    hasOKey == Nullable.VALUE && OKeyValue != other.OKeyValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			
			long           key;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key =  keys.array[i]) != 0)
					if (values.nulls.get( i ))
					{
						int tag = other.tag( key );
						if (tag == -1 || values.get( i ) != other.get( tag )) return 1;
					}
					else if (-1 < other.tag( key )) return 1;
			
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
			if (dst == null) dst = new StringBuilder( assigned * 10 );
			else dst.ensureCapacity( dst.length() + assigned * 10 );
			
			Producer src = producer();
			for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
			{
				dst.append( src.key( tag ) ).append( " -> " );
				
				if (src.isNull( tag )) dst.append( "null" );
				else dst.append( src.value( tag ) );
			}
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	public static class RW extends R implements Consumer {
		
		public RW() {
		}
		
		public RW( int expectedItems ) {
			super( expectedItems );
		}
		
		public RW( int expectedItems, double loadFactor ) {
			super( expectedItems, loadFactor );
		}
		
		public Consumer consumer() {return this; }
		
		
		//put key -> null
		public boolean put( long key ) {
			if (key == 0)
			{
				hasOKey = Nullable.NULL;
				return true;
			}
			
			int               slot = hashKey( key ) & mask;
			final int key_ = (int) key ;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set( slot );
					return true;
				}
			
			keys.array[slot] = key_;
			values.set( slot );
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		//put key -> value
		public boolean put( long key, int value ) {
			if (key == 0)
			{
				hasOKey   = Nullable.VALUE;
				OKeyValue = value;
				return true;
			}
			
			int slot = hashKey( key ) & mask;
			
			final int key_ = (int) key ;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set( slot, value );
					return true;
				}
			
			keys.array[slot] = key_;
			values.set( slot, value );
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean remove() { return hasOKey != Nullable.NONE && (hasOKey = Nullable.NONE) == Nullable.NONE; }
		
		public boolean remove( long key ) {
			if (key == 0) return remove();
			
			int slot = hashKey( key ) & mask;
			
			final int key_ = (int) key ;
			
			final int[] array = keys.array;
			for (int k; (k = array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					int kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hashKey(   kk  ) & mask) >= distance)
						{
							
							array[gapSlot] = kk;
							
							if (values.nulls.get( s ))
								values.set( gapSlot, values.get( s ) );
							else
								values.set( gapSlot );
							
							gapSlot  = s;
							distance = 0;
						}
					
					array[gapSlot] = 0;
					values.set( gapSlot );
					assigned--;
					return true;
				}
			return false;
		}
		
		void allocate( int size ) {
			
			if (assigned < 1)
			{
				resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
				mask     = size - 1;
				
				if (keys.length() < size) keys.allocate( size );
				else keys.clear();
				
				if (values.nulls.length() < size) values.nulls.allocate( size );
				else values.nulls.clear();
				
				if (values.values.length() < size) values.values.allocate( size );
				else values.values.clear();
				
				return;
			}
			
			RW tmp = new RW( size + 1, loadFactor );
			
			int[] array = keys.array;
			int   key;
			
			for (int i = array.length - 1; -1 < --i; )
				if ((key = array[i]) != 0)
					if (values.nulls.get( i )) tmp.put( key, values.get( i ) );
					else tmp.put( key );
			
			keys   = tmp.keys;
			values = tmp.values;
			
			assigned = tmp.assigned;
			mask     = tmp.mask;
			resizeAt = tmp.resizeAt;
		}
		
		public void clear() {
			assigned = 0;
			hasOKey  = Nullable.NONE;
			keys.clear();
			values.clear();
		}
		
		public RW clone() { return (RW) super.clone(); }
	}
}
