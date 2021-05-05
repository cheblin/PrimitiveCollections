package org.unirail.collections;

public interface UByteIntNullMap {
	interface Consumer {
		boolean put( char key, int value );
		
		boolean put( char key,  Integer   value );
		
		boolean put(  Byte      key,  Integer   value );
		
		boolean put(  Byte      key, int value );
	}
	
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		char key( int tag );
		
		boolean hasValue( int tag );
		
		int value( int tag );
		
		
		@Nullable int hasNullKey();
		
		int nullKeyValue();
		
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
		public UByteList.RW     keys   = new UByteList.RW( 0 );
		public IntNullList.RW values = new IntNullList.RW( 0 );
		
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		
		@Nullable int hasNull;
		int NullValue = 0;
		
		
		@Nullable int hasO;
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
			values.nulls.length( size );
			values.values.length( size );
		}
		
		public boolean isEmpty()                 { return size() == 0; }
		
		public int size()                        { return assigned + (hasO == Nullable.NONE ? 0 : 1) + (hasNull == Nullable.NONE ? 0 : 1); }
		
		protected int hashKey( char key ) {return Array.hashKey( key ); }
		
		
		public int hashCode() {
			int h = hasO == Nullable.NONE ? 0xDEADBEEF : 0;
			
			byte k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash( (char)( 0xFFFF &  k ) ) + Array.hash( values.hashCode() );
			
			return h;
		}
		
		public @Nullable int tag(  Byte      key ) {return key == null ? hasNull : tag( (char) (key + 0) );}
		
		public @Nullable int tag( char key ) {
			
			if (key == 0)
				if (hasO == Nullable.NONE) return Nullable.NONE;
				else return hasO - 1;
			
			final byte key_ = (byte) key ;
			
			int slot = hashKey( key ) & mask;
			
			for (byte k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return (slot = values.tag( slot )) == -1 ? Integer.MIN_VALUE | slot : slot;
			
			return Nullable.NONE;//the key is not present
		}
		
		public boolean contains( int tag )           {return tag != Nullable.NONE;}
		
		public boolean hasValue( @Nullable int tag ) { return -1 < tag; }
		
		public int get( @Nullable int tag ) {
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
				
				public char key( int tag ) {return (tag &= Integer.MAX_VALUE) < keys.array.length ? (char)( 0xFFFF &  keys.array[tag]) : (char) 0; }
				
				public boolean hasValue( int tag ) { return -1 < tag; }
				
				public int value( int tag ) {return tag < keys.array.length ? values.get( values.tag( tag ) ) : OValue; }
				
				public @Nullable int hasNullKey() { return hasNull; }
				
				public int nullKeyValue() { return NullValue; }
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
			
			if (hasNull != other.hasNull ||
			    hasNull == Nullable.VALUE && NullValue != other.NullValue) return 1;
			
			if (hasO != other.hasO ||
			    hasO == Nullable.VALUE && OValue != other.OValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			
			char           key;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = (char)( 0xFFFF &  keys.array[i])) != 0)
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
		
		public boolean put(  Byte      key, int value ) {
			if (key != null) return put( (char) (key + 0), value );
			
			hasNull   = Nullable.VALUE;
			NullValue = value;
			return true;
		}
		
		public boolean put(  Byte      key,  Integer   value ) {
			if (key != null) return put( (char) (key + 0), value );
			
			if (value == null)
				hasNull = Nullable.NULL;
			else
			{
				hasNull   = Nullable.VALUE;
				NullValue = value;
			}
			
			return true;
		}
		
		public boolean put( char key,  Integer   value ) {
			if (value != null) return put( key, (int) value );
			
			if (key == 0)
			{
				hasO = Nullable.NULL;
				return true;
			}
			
			int               slot = hashKey( key ) & mask;
			final byte key_ = (byte) key ;
			
			for (byte k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set( slot, ( Integer  ) null );
					return true;
				}
			
			keys.array[slot] = key_;
			values.set( slot, ( Integer  ) null );
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean put( char key, int value ) {
			if (key == 0)
			{
				hasO   = Nullable.VALUE;
				OValue = value;
				return true;
			}
			
			int slot = hashKey( key ) & mask;
			
			final byte key_ = (byte) key ;
			
			for (byte k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
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
		
		public boolean remove( char key ) {
			if (key == 0) return remove();
			
			int slot = hashKey( key ) & mask;
			
			final byte key_ = (byte) key ;
			
			final byte[] array = keys.array;
			for (byte k; (k = array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					byte kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hashKey(  (char)( 0xFFFF &  kk ) ) & mask) >= distance)
						{
							
							array[gapSlot] = kk;
							
							if (values.nulls.get( s ))
								values.set( gapSlot, values.get( s ) );
							else
								values.set( gapSlot, ( Integer  ) null );
							
							gapSlot  = s;
							distance = 0;
						}
					
					array[gapSlot] = 0;
					values.set( gapSlot, ( Integer  ) null );
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
			
			byte[] array = keys.array;
			byte   key;
			
			for (int i = array.length - 1; -1 < --i; )
				if ((key = array[i]) != 0)
					if (values.nulls.get( i )) tmp.put((char)( 0xFFFF &  key), values.get( i ) );
					else tmp.put((char)( 0xFFFF &  key), null );
			
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
