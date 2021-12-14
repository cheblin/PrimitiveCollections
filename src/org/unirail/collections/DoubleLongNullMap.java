package org.unirail.collections;

import org.unirail.Hash;

public interface DoubleLongNullMap {
	
	interface NonNullKeysIterator {
		int END = -1;
		
		
		static int token( R src ) {return token( src, END );}
		
		static int token( R src, int token ) {
			int len = src.keys.array.length;
			for (token++, token &= Integer.MAX_VALUE; ; token++)
				if (token == len) return src.has0key == Positive_Values.NONE ? END : -2;
				else if (token == 0x7FFF_FFFF) return END;
				else if (src.keys.array[token] != 0) return src.values.hasValue( token ) ? token : token | Integer.MIN_VALUE;
		}
		
		static double key( R src, int token ) {return token == -2 ? 0 : src.keys.array[token & Integer.MAX_VALUE];}
		
		static boolean hasValue( R src, int token ) {return (token == -2 && src.has0key == Positive_Values.VALUE) || -1 < token;}
		
		static long value( R src, int token ) {return token == -2 ? src.OkeyValue :    src.values.get( token );}
	}
	
	abstract class R implements Cloneable, Comparable<R> {
		
		
		public DoubleList.RW     keys   = new DoubleList.RW( 0 );
		public LongNullList.RW values = new LongNullList.RW( 0 );
		
		int assigned;
		int mask;
		int resizeAt;
		
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		long nullKeyValue = 0;
		
		
		@Positive_Values int has0key = Positive_Values.NONE;
		long       OkeyValue;
		
		
		protected double loadFactor;
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (has0key == Positive_Values.NONE ? 0 : 1) + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		
		public int hashCode() {
			int hash = hasNullKey == Positive_Values.VALUE ? Hash.code( nullKeyValue ) : hasNullKey == Positive_Values.NONE ? 436195789 : 22121887;
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ))
			{
				hash = Hash.code( hash, NonNullKeysIterator.key( this, token ) );
				if (NonNullKeysIterator.hasValue( this, token )) hash = Hash.code( hash, Hash.code( (NonNullKeysIterator.value( this, token )) ) );
			}
			
			return hash;
		}
		
		public boolean contains(  Double    key )           {return !hasNone( token( key ) );}
		
		public boolean contains( double key )               {return !hasNone( token( key ) );}
		
		public @Positive_Values int token(  Double    key ) {return key == null ? hasNullKey : token( (double) (key + 0) );}
		
		public @Positive_Values int token( double key ) {
			
			if (key == 0) return has0key == Positive_Values.VALUE ? Positive_Values.VALUE - 1 : has0key;
			
			final double key_ =  key ;
			
			int slot = Hash.code( key ) & mask;
			
			for (double k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return (values.hasValue( slot )) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;//the key is not present
		}
		
		public boolean hasValue( int token ) {return -1 < token;}
		
		public boolean hasNone( int token )  {return token == Positive_Values.NONE;}
		
		public boolean hasNull( int token )  {return token == Positive_Values.NULL;}
		
		public long value( @Positive_ONLY int token ) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			if (token == Positive_Values.VALUE - 1) return OkeyValue;
			return values.get( token );
		}
		
		
		public @Positive_Values int hasNullKey() {return hasNullKey;}
		
		public long nullKeyValue() {return nullKeyValue;}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			
			if (hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && nullKeyValue != other.nullKeyValue) return 1;
			
			if (has0key != other.has0key || has0key == Positive_Values.VALUE && OkeyValue != other.OkeyValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			double           key;
			for (int i = keys.array.length; -1 < --i; )
				if ((key =  keys.array[i]) != 0)
					if (values.nulls.get( i ))
					{
						int ii = other.token( key );
						if (ii < 0 || values.get( i ) != other.value( ii )) return 1;
					}
					else if (-1 < other.token( key )) return 1;
			
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
		
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			switch (hasNullKey)
			{
				case Positive_Values.VALUE:
					dst.append( "null -> " ).append( nullKeyValue ).append( '\n' );
					break;
				case Positive_Values.NULL:
					dst.append( "null -> null\n" );
			}
			
			switch (has0key)
			{
				case Positive_Values.VALUE:
					dst.append( "0 -> " ).append( OkeyValue ).append( '\n' );
					break;
				case Positive_Values.NULL:
					dst.append( "0 -> null\n" );
			}
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ), dst.append( '\n' ))
			{
				dst.append( NonNullKeysIterator.key( this, token ) ).append( " -> " );
				if (NonNullKeysIterator.hasValue( this, token )) dst.append( NonNullKeysIterator.value( this, token ) );
				else dst.append( "null" );
			}
			
			return dst;
		}
	}
	
	class RW extends R {
		
		
		public RW( int expectedItems ) {this( expectedItems, 0.75 );}
		
		
		public RW( int expectedItems, double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys.length( size );
			values.nulls.length( size );
			values.values.length( size );
		}
		
		
		public boolean put(  Double    key, long value ) {
			if (key != null) return put( (double) (key + 0), value );
			
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			nullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put(  Double    key,  Long      value ) {
			if (key != null) return put( (double) (key + 0), value );
			
			int h = hasNullKey;
			
			if (value == null)
			{
				hasNullKey = Positive_Values.NULL;
				return h == Positive_Values.NULL;
			}
			
			hasNullKey   = Positive_Values.VALUE;
			nullKeyValue = value;
			return h == Positive_Values.VALUE;
		}
		
		public boolean put( double key,  Long      value ) {
			if (value != null) return put( key, (long) value );
			
			if (key == 0)
			{
				int h = has0key;
				has0key = Positive_Values.NULL;
				return h == Positive_Values.NONE;
			}
			
			int               slot = Hash.code( key ) & mask;
			final double key_ =  key ;
			
			for (double k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set( slot, ( Long     ) null );
					return false;
				}
			
			keys.array[slot] = key_;
			values.set( slot, ( Long     ) null );
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean put( double key, long value ) {
			if (key == 0)
			{
				int h = has0key;
				has0key   = Positive_Values.VALUE;
				OkeyValue = value;
				return h == Positive_Values.NONE;
			}
			
			int slot = Hash.code( key ) & mask;
			
			final double key_ =  key ;
			
			for (double k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
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
		
		
		public boolean remove() {return has0key != Positive_Values.NONE && (has0key = Positive_Values.NONE) == Positive_Values.NONE;}
		
		public boolean remove( double key ) {
			if (key == 0) return remove();
			
			int slot = Hash.code( key ) & mask;
			
			final double key_ =  key ;
			
			final double[] array = keys.array;
			for (double k; (k = array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					double kk;
					
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Hash.code( kk ) & mask) >= distance)
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
				
				if (keys.length() < size) keys.length( -size );
				if (values.nulls.length() < size) values.nulls.length( -size );
				if (values.values.length() < size) values.values.length( -size );
				
				return;
			}
			
			RW tmp = new RW( size - 1, loadFactor );
			
			double[] k = keys.array;
			double   key;
			
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
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
			has0key  = Positive_Values.NONE;
			keys.clear();
			values.clear();
		}
		
		public RW clone() {return (RW) super.clone();}
	}
}
