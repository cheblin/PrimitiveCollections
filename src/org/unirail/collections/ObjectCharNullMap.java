package org.unirail.collections;


import org.unirail.Hash;

public interface ObjectCharNullMap {
	interface NonNullKeysIterator {
		int END = -1;
		
		static <K extends Comparable<? super K>> int token( R<K> src ) {return token( src, END );}
		
		static <K extends Comparable<? super K>> int token( R<K> src, int token ) {
			for (token++, token &= Integer.MAX_VALUE; ; token++)
				if (token == src.keys.array.length) return END;
				else if (src.keys.array[token] != null) return src.values.hasValue( token ) ? token : token | Integer.MIN_VALUE;
		}
		
		static <K extends Comparable<? super K>> K key( R<K> src, int token )            {return src.keys.array[token & Integer.MAX_VALUE];}
		
		static <K extends Comparable<? super K>> boolean hasValue( R<K> src, int token ) {return -1 < token;}
		
		static <K extends Comparable<? super K>> char value( R<K> src, int token ) {return src.values.get( token );}
	}
	
	abstract class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>> {
		
		
		ObjectList.RW<K>       keys   = new ObjectList.RW<>( 0 );
		CharNullList.RW values = new CharNullList.RW( 0 );
		
		protected int    assigned;
		protected int    mask;
		protected int    resizeAt;
		protected double loadFactor;
		
		
		public int size()        {return assigned + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		public boolean isEmpty() {return size() == 0;}
		
		public int hashCode() {
			int hash = Hash.code( hasNullKey == Positive_Values.NONE ? 719281 : hasNullKey == Positive_Values.NULL ? 401101 : Hash.code( NullKeyValue ) );
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ))
			{
				hash = Hash.code( hash, NonNullKeysIterator.key( this, token ) );
				if (NonNullKeysIterator.hasValue( this, token )) hash = Hash.code( hash, NonNullKeysIterator.value( this, token ) );
			}
			
			return hash;
		}
		
		
		public boolean contains( K key ) {return !hasNone( token( key ) );}
		
		public @Positive_Values int token( K key ) {
			
			if (key == null) return hasNullKey;
			
			int slot = Hash.code( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return (values.hasValue( slot )) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;
		}
		
		public boolean hasValue( int token ) {return -1 < token;}
		
		public boolean hasNone( int token )  {return token == Positive_Values.NONE;}
		
		public boolean hasNull( int token )  {return token == Positive_Values.NULL;}
		
		public char value( @Positive_ONLY int token ) {return token == Positive_Values.VALUE ? NullKeyValue : values.get( token );}
		
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R<K> other ) {return other != null && compareTo( other ) == 0;}
		
		@Override public int compareTo( R<K> other ) {
			if (other == null) return -1;
			
			if (hasNullKey != other.hasNullKey ||
			    hasNullKey == Positive_Values.VALUE && NullKeyValue != other.NullKeyValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			
			K key;
			for (int i = keys.array.length; -1 < --i; )
				if ((key = keys.array[i]) != null)
					if (values.nulls.get( i ))
					{
						int tag = other.token( key );
						if (tag == -1 || values.get( i ) != other.value( tag )) return 1;
					}
					else if (-1 < other.token( key )) return 1;
			
			return 0;
		}
		
		@Override @SuppressWarnings("unchecked")
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		protected @Positive_Values int         hasNullKey   = Positive_Values.NONE;
		protected                  char NullKeyValue = 0;
		
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			switch (hasNullKey)
			{
				case Positive_Values.NULL:
					dst.append( "null -> null\n" );
					break;
				case Positive_Values.VALUE:
					dst.append( "null -> " ).append( NullKeyValue ).append( '\n' );
			}
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; dst.append( '\n' ), token = NonNullKeysIterator.token( this, token ))
			{
				dst.append( NonNullKeysIterator.key( this, token ) ).append( " -> " );
				
				if (NonNullKeysIterator.hasValue( this, token )) dst.append( NonNullKeysIterator.value( this, token ) );
				else dst.append( "null" );
			}
			
			return dst;
		}
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> {
		
		public RW( double loadFactor ) {this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );}
		
		public RW( int expectedItems ) {this( expectedItems, 0.75 );}
		
		public RW( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			long length = (long) Math.ceil( expectedItems / loadFactor );
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys.length( size );
			values.nulls.length( size );
			values.values.length( size );
		}
		
		@Override public RW<K> clone() {return (RW<K>) super.clone();}
		
		
		public boolean put( K key,  Character value ) {
			if (value != null) put( key, (char) value );
			
			if (key == null)
			{
				hasNullKey = Positive_Values.NULL;
				return true;
			}
			
			int slot = Hash.code( key ) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					values.set( slot, ( Character) null );
					return true;
				}
			
			keys.array[slot] = key;
			values.set( slot, ( Character) null );
			
			if (++assigned == resizeAt) this.allocate( mask + 1 << 1 );
			
			return true;
		}
		
		public boolean put( K key, char value ) {
			
			if (key == null)
			{
				int h = hasNullKey;
				hasNullKey   = Positive_Values.VALUE;
				NullKeyValue = value;
				return h != Positive_Values.VALUE;
			}
			
			int slot = Hash.code( key ) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					values.set( slot, value );
					return true;
				}
			
			keys.array[slot] = key;
			values.set( slot, value );
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		public void clear() {
			assigned   = 0;
			hasNullKey = Positive_Values.NONE;
			keys.clear();
			values.clear();
		}
		
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length( -size );
				
				if (values.nulls.length() < size) values.nulls.length( -size );
				
				if (values.values.length() < size) values.values.length( -size );
				
				return;
			}
			
			final K[]              k    = keys.array;
			CharNullList.RW vals = values;
			
			keys.length( -size );
			values = new CharNullList.RW( -size );
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = Hash.code( key ) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot] = key;
					
					if (vals.hasValue( i )) values.set( slot, vals.get( i ) );
					else values.set( slot, ( Character) null );
				}
			
		}
		
		
		public boolean remove( K key ) {
			if (key == null)
			{
				int h = hasNullKey;
				hasNullKey = Positive_Values.NONE;
				return h != Positive_Values.NONE;
			}
			
			int slot = Hash.code( key ) & mask;
			
			final K[] ks = keys.array;
			for (K k; (k = ks[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = ks[s = gapSlot + ++distance & mask]) != null; )
						if ((s - Hash.code( kk ) & mask) >= distance)
						{
							ks[gapSlot] = kk;
							
							if (values.nulls.get( s ))
								values.set( gapSlot, values.get( s ) );
							else
								values.set( gapSlot, ( Character) null );
							
							gapSlot  = s;
							distance = 0;
						}
					
					ks[gapSlot] = null;
					values.set( gapSlot, ( Character) null );
					assigned--;
					return true;
				}
			
			return false;
		}
		
		
	}
	
}
	
	