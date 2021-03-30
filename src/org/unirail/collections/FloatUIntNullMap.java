package org.unirail.collections;

public interface FloatUIntNullMap {
	interface Consumer {
		boolean put( float key, long value );
		
		boolean put( float key,  Long      value );
		
		boolean put(  Float     key,  Long      value );
		
		boolean put(  Float     key, long value );
	}
	
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		float key( int tag );
		
		boolean hasValue( int tag );
		
		long value( int tag );
		
		
		@Nullable int hasNullKey();
		
		long nullKeyValue();
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			switch (hasNullKey())
			{
				case Nullable.VALUE: dst.append( "null -> " ).append( nullKeyValue() ).append( '\n' );
					break;
				case Nullable.NULL: dst.append( "null -> null\n" );
			}
			
			for (int tag = tag(); ok( tag ); dst.append( '\n' ), tag = tag( tag ))
			{
				dst.append( key( tag ) ).append( " -> " );
				
				if (hasValue( tag )) dst.append( value( tag ) );
				else dst.append( "null" );
			}
			
			return dst;
		}
	}
	
	class R implements Cloneable, Comparable<R> {
		public FloatList.RW     keys   = new FloatList.RW( 0 );
		public UIntNullList.RW values = new UIntNullList.RW( 0 );
		
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		
		@Nullable int hasNull;
		long NullValue = 0;
		
		
		@Nullable int hasO;
		long       OValue;
		
		
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
			values.nulls.length( size );
			values.values.length( size );
		}
		
		public boolean isEmpty()                 { return size() == 0; }
		
		public int size()                        { return assigned + (hasO == Nullable.NONE ? 0 : 1) + (hasNull == Nullable.NONE ? 0 : 1); }
		
		protected int hashKey( float key ) {return Array.hashKey( key ); }
		
		
		public int hashCode() {
			int h = hasO == Nullable.NONE ? 0xDEADBEEF : 0;
			
			float k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash(  k  ) + Array.hash( values.hashCode() );
			
			return h;
		}
		
		public @Nullable int tag(  Float     key ) {return key == null ? hasNull : tag( (float) (key + 0) );}
		
		public @Nullable int tag( float key ) {
			
			if (key == 0)
				if (hasO == Nullable.NONE) return Nullable.NONE;
				else return hasO - 1;
			
			final float key_ = Float.floatToIntBits( key );
			
			int slot = hashKey( key ) & mask;
			
			for (float k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return (slot = values.tag( slot )) == -1 ? Integer.MIN_VALUE | slot : slot;
			
			return Nullable.NONE;//the key is not present
		}
		
		public boolean contains( int tag )           {return tag != Nullable.NONE;}
		
		public boolean hasValue( @Nullable int tag ) { return -1 < tag; }
		
		public long get( @Nullable int tag ) {
			switch (tag)
			{
				case Nullable.VALUE: return NullValue;
				case Nullable.VALUE - 1: return OValue;
			}
			return values.get( tag & Integer.MAX_VALUE );
		}
		
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				
				public int tag() {
					int i = keys.array.length;
					switch (hasO)
					{
						case Nullable.VALUE: return i;
						case Nullable.NULL: return Integer.MIN_VALUE | i;
						default:
							if (0 < assigned)
								while (-1 < --i)
									if (keys.array[i] != 0)
										return values.nulls.get( i ) ? i : Integer.MIN_VALUE | i;
							
							return -1;
					}
				}
				
				public int tag( int tag ) {
					tag &= Integer.MAX_VALUE;
					while (-1 < --tag)
						if (keys.array[tag] != 0)
							return values.nulls.get( tag ) ? tag : Integer.MIN_VALUE | tag;
					return -1;
				}
				
				public float key( int tag ) {return (tag &= Integer.MAX_VALUE) < keys.array.length ?  keys.array[tag] : (float) 0; }
				
				public boolean hasValue( int tag ) { return -1 < tag; }
				
				public long value( int tag ) {return tag < keys.array.length ? values.get( values.tag( tag ) ) : OValue; }
				
				public @Nullable int hasNullKey() { return hasNull; }
				
				public long nullKeyValue() { return NullValue; }
			} : producer;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			
			if (hasNull != other.hasNull ||
			    hasNull == Nullable.VALUE && NullValue != other.NullValue) return 1;
			
			if (hasO != other.hasO ||
			    hasO == Nullable.VALUE && OValue != other.OValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			
			float           key;
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
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW extends R implements Consumer {
		
		public RW() {
		}
		
		public RW( int expectedItems ) {
			super( expectedItems );
		}
		
		public RW( int expectedItems, double loadFactor ) {
			super( expectedItems, loadFactor );
		}
		
		
		public Consumer consumer() {return this; }
		
		public boolean put(  Float     key, long value ) {
			if (key != null) return put( (float) (key + 0), value );
			
			hasNull   = Nullable.VALUE;
			NullValue = value;
			return true;
		}
		
		public boolean put(  Float     key,  Long      value ) {
			if (key != null) return put( (float) (key + 0), value );
			
			if (value == null)
				hasNull = Nullable.NULL;
			else
			{
				hasNull   = Nullable.VALUE;
				NullValue = value;
			}
			
			return true;
		}
		
		public boolean put( float key,  Long      value ) {
			if (value != null) return put( key, (long) value );
			
			if (key == 0)
			{
				hasO = Nullable.NULL;
				return true;
			}
			
			int               slot = hashKey( key ) & mask;
			final float key_ = Float.floatToIntBits( key );
			
			for (float k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set( slot, ( Long     ) null );
					return true;
				}
			
			keys.array[slot] = key_;
			values.set( slot, ( Long     ) null );
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean put( float key, long value ) {
			if (key == 0)
			{
				hasO   = Nullable.VALUE;
				OValue = value;
				return true;
			}
			
			int slot = hashKey( key ) & mask;
			
			final float key_ = Float.floatToIntBits( key );
			
			for (float k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
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
		
		
		public boolean remove() { return hasO != Nullable.NONE && (hasO = Nullable.NONE) == Nullable.NONE; }
		
		public boolean remove( float key ) {
			if (key == 0) return remove();
			
			int slot = hashKey( key ) & mask;
			
			final float key_ = Float.floatToIntBits( key );
			
			final float[] array = keys.array;
			for (float k; (k = array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					float kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hashKey(   kk  ) & mask) >= distance)
						{
							
							array[gapSlot] = kk;
							
							if (values.nulls.get( s ))
								values.set( gapSlot, values.get( s ) );
							else
								values.set( gapSlot, ( Long     ) null );
							
							gapSlot  = s;
							distance = 0;
						}
					
					array[gapSlot] = 0;
					values.set( gapSlot, ( Long     ) null );
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
				
				if (keys.length() < size) keys.length( size );
				else keys.clear();
				
				if (values.nulls.length() < size) values.nulls.length( size );
				else values.nulls.clear();
				
				if (values.values.length() < size) values.values.length( size );
				else values.values.clear();
				
				return;
			}
			
			RW tmp = new RW( size - 1, loadFactor );
			
			float[] array = keys.array;
			float   key;
			
			for (int i = array.length - 1; -1 < --i; )
				if ((key = array[i]) != 0)
					if (values.nulls.get( i )) tmp.put( key, values.get( i ) );
					else tmp.put( key, null );
			
			keys   = tmp.keys;
			values = tmp.values;
			
			assigned = tmp.assigned;
			mask     = tmp.mask;
			resizeAt = tmp.resizeAt;
		}
		
		public void clear() {
			assigned = 0;
			hasO     = Nullable.NONE;
			keys.clear();
			values.clear();
		}
		
		public RW clone() { return (RW) super.clone(); }
	}
}
