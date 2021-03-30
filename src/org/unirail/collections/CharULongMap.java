package org.unirail.collections;

public interface CharULongMap {
	
	interface Consumer {
		boolean put( char key, long value );
		
		boolean put(  Character key, long value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		char key( int tag );
		
		long value( int tag );
		
		boolean hasNullKey();
		
		long nullKeyValue();
		
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
		public CharList.RW keys   = new CharList.RW( 0 );
		public ULongList.RW values = new ULongList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNull;
		long       NullValue;
		
		boolean hasO;
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
			values.length( size );
		}
		
		public boolean isEmpty()                        { return size() == 0; }
		
		public int size()                               { return assigned + (hasO ? 1 : 0) + (hasNull ? 1 : 0); }
		
		
		public @Nullable int tag(  Character key ) {return key == null ? hasNull ? Nullable.NULL : Nullable.NONE : tag( (char) key );}
		
		public int tag( char key ) {
			if (key == 0) return hasO ? Nullable.VALUE : Nullable.NONE;
			
			int slot = hashKey( key ) & mask;
			
			for (char key_ =  key , k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return -1;
		}
		
		public boolean contains( @Nullable int tag ) {return tag != Nullable.NONE;}
		
		
		public long get( @Nullable int tag ) {
			if (tag == Nullable.NULL) return NullValue;
			if (tag == Nullable.VALUE) return OValue;
			return   values.array[tag];
		}
		
		
		protected int hashKey( char key ) {return Array.hashKey( key ); }
		
		
		public int hashCode() {
			int h = hasO ? 0xDEADBEEF : 0;
			
			char k;
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
				
				public char key( int tag ) {return assigned == 0 || tag == keys.array.length ? (char) 0 : (char) keys.array[tag]; }
				
				public long value( int tag ) {return assigned == 0 || tag == keys.array.length ? OValue :  values.array[tag]; }
				
				public boolean hasNullKey() {return hasNull;}
				
				public long nullKeyValue() {return NullValue;}
			} : producer;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			
			if (hasO != other.hasO || hasO && OValue != other.OValue) return 1;
			
			int diff;
			if ((diff = size() - other.size()) != 0) return diff;
			
			
			char           k;
			for (int i = keys.array.length - 1, ii; -1 < i; i--)
				if ((k = (char) keys.array[i]) != 0)
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
			
			final char[] k = keys.array;
			final long[] v = values.array;
			
			keys.length( size + 1 );
			values.length( size + 1 );
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (k == null || assigned < 1) return;
			
			
			char key;
			for (int i = k.length - 1; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = hashKey( (char) key  ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot]   = key;
					values.array[slot] = v[i];
				}
		}
		
		public boolean put(  Character key, long value ) {
			if (key != null) return put( (char) key, value );
			
			hasNull   = true;
			NullValue = value;
			
			return true;
		}
		
		public boolean put( char key, long value ) {
			
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
			
			final char key_ =  key;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.array[slot] =(long)value;
					return true;
				}
			
			keys.array[slot]   =            key_;
			values.array[slot] = (long)value;
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return false;
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
			
			final char key_ =  key;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					char kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hashKey( (char) kk  ) & mask) >= distance)
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
