package org.unirail.collections;

import org.unirail.Hash;

public interface IntDoubleNullMap {
	
	interface Iterator {
		int token = -1;
		
		static int key( R src, int token ) {return   src.keys.array[token & Integer.MAX_VALUE];}
		
		@Positive_YES static int token( R src, int token ) {
			for (token++, token &= Integer.MAX_VALUE; src.keys.array[token] == 0; token++) ;
			return src.values.hasValue( token ) ? token : token | Integer.MIN_VALUE;
		}
		
		static double value( R src, @Positive_ONLY int token ) {return   src.values.get( token );}
	}
	
	abstract class R implements Cloneable, Comparable<R> {
		
		
		public IntList.RW     keys   = new IntList.RW( 0 );
		public DoubleNullList.RW values = new DoubleNullList.RW( 0 );
		
		int assigned;
		int mask;
		int resizeAt;
		
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		double nullKeyValue = 0;
		
		
		@Positive_Values int hasOkey = Positive_Values.NONE;
		double       OkeyValue;
		
		
		protected double loadFactor;
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (hasOkey == Positive_Values.NONE ? 0 : 1) + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		
		public int hashCode() {
			int hash = Hash.code( 15618090, hasOkey == Positive_Values.VALUE ? OkeyValue : hasOkey == Positive_Values.NONE ? 13011091 : 136101521  );
			hash = Hash.code( hash, hasNullKey == Positive_Values.VALUE ? nullKeyValue : hasNullKey == Positive_Values.NONE ? 436195789 : 22121887 );
			
			return Hash.code( Hash.code( hash, keys ), values );
		}
		
		public boolean contains(  Integer   key )           {return !hasNone( token( key ) );}
		
		public boolean contains( int key )               {return !hasNone( token( key ) );}
		
		public @Positive_Values int token(  Integer   key ) {return key == null ? hasNullKey : token( (int) (key + 0) );}
		
		public @Positive_Values int token( int key ) {
			
			if (key == 0) return hasOkey == Positive_Values.VALUE ? Positive_Values.VALUE - 1 : hasOkey;
			
			final int key_ =  key ;
			
			int slot = Hash.code( key ) & mask;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return (values.hasValue( slot )) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;//the key is not present
		}
		
		public boolean hasValue( int token ) {return -1 < token;}
		
		public boolean hasNone( int token )  {return token == Positive_Values.NONE;}
		
		public boolean hasNull( int token )  {return token == Positive_Values.NULL;}
		
		public double value( @Positive_ONLY int token ) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			if (token == Positive_Values.VALUE - 1) return OkeyValue;
			return values.get( token );
		}
		
		
		public @Positive_Values int hasNullKey() {return hasNullKey;}
		
		public double nullKeyValue() {return nullKeyValue;}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			
			if (hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && nullKeyValue != other.nullKeyValue) return 1;
			
			if (hasOkey != other.hasOkey || hasOkey == Positive_Values.VALUE && OkeyValue != other.OkeyValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			int           key;
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
					size--;
					break;
				case Positive_Values.NULL:
					dst.append( "null -> null\n" );
					size--;
			}
			
			switch (hasOkey)
			{
				case Positive_Values.VALUE:
					dst.append( "0 -> " ).append( OkeyValue ).append( '\n' );
					size--;
					break;
				case Positive_Values.NULL:
					dst.append( "0 -> null\n" );
					size--;
			}
			
			for (int token = Iterator.token, i = 0; i < size; i++, dst.append( '\n' ))
			{
				dst.append( Iterator.key( this, token = Iterator.token( this, token ) ) ).append( " -> " );
				
				if (token < 0) dst.append( "null" );
				else dst.append( Iterator.value( this, token ) );
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
		
		
		public boolean put(  Integer   key, double value ) {
			if (key != null) return put( (int) (key + 0), value );
			
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			nullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put(  Integer   key,  Double    value ) {
			if (key != null) return put( (int) (key + 0), value );
			
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
		
		public boolean put( int key,  Double    value ) {
			if (value != null) return put( key, (double) value );
			
			if (key == 0)
			{
				int h = hasOkey;
				hasOkey = Positive_Values.NULL;
				return h == Positive_Values.NONE;
			}
			
			int               slot = Hash.code( key ) & mask;
			final int key_ =  key ;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set( slot, ( Double   ) null );
					return false;
				}
			
			keys.array[slot] = key_;
			values.set( slot, ( Double   ) null );
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean put( int key, double value ) {
			if (key == 0)
			{
				int h = hasOkey;
				hasOkey   = Positive_Values.VALUE;
				OkeyValue = value;
				return h == Positive_Values.NONE;
			}
			
			int slot = Hash.code( key ) & mask;
			
			final int key_ =  key ;
			
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
		
		
		public boolean remove() {return hasOkey != Positive_Values.NONE && (hasOkey = Positive_Values.NONE) == Positive_Values.NONE;}
		
		public boolean remove( int key ) {
			if (key == 0) return remove();
			
			int slot = Hash.code( key ) & mask;
			
			final int key_ =  key ;
			
			final int[] array = keys.array;
			for (int k; (k = array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					int kk;
					
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Hash.code( kk ) & mask) >= distance)
						{
							
							array[gapSlot] = kk;
							
							if (values.nulls.get( s ))
								values.set( gapSlot, values.get( s ) );
							else
								values.set( gapSlot, ( Double   ) null );
							
							gapSlot  = s;
							distance = 0;
						}
					
					array[gapSlot] = 0;
					values.set( gapSlot, ( Double   ) null );
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
			
			int[] k = keys.array;
			int   key;
			
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
			hasOkey  = Positive_Values.NONE;
			keys.clear();
			values.clear();
		}
		
		public RW clone() {return (RW) super.clone();}
	}
}
