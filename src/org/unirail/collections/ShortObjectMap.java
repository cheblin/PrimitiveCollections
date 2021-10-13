package org.unirail.collections;

public interface ShortObjectMap {
	
	interface Iterator {
		
		int token = -1;
		
		static <V extends Comparable<? super V>> int token( R<V> src, int token ) {for (; ; ) if (src.keys.array[++token] != 0) return token;}
		
		static <V extends Comparable<? super V>> short key( R<V> src, int token ) {return  (short) src.keys.array[token];}
		
		static <V extends Comparable<? super V>> V value( R<V> src, int token )   {return src.values.array[token];}
	}
	
	abstract class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>> {
		
		
		public ShortList.RW keys = new ShortList.RW( 0 );
		
		public ObjectList.RW<V> values = new ObjectList.RW<>( 0 );
		
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		boolean hasNullKey;
		V       nullKeyValue;
		
		
		boolean hasO;
		V       OkeyValue;
		
		
		protected double loadFactor;
		
		public boolean contains(  Short     key )           {return !hasNone( token( key ) );}
		
		public boolean contains( short key )               {return !hasNone( token( key ) );}
		
		public boolean hasNone( int token )                      {return token == Positive_Values.NONE;}
		
		
		public @Positive_Values int token(  Short     key ) {return key == null ? hasNullKey ? Integer.MAX_VALUE : Positive_Values.NONE : token( (short) (key + 0) );}
		
		public @Positive_Values int token( short key ) {
			if (key == 0) return hasO ? Positive_Values.VALUE - 1 : Positive_Values.NONE;
			
			int slot = Array.hash( key ) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return slot;
			
			return Positive_Values.NONE;
		}
		
		public V value( @Positive_ONLY int token ) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			if (token == Positive_Values.VALUE - 1) return OkeyValue;
			
			return values.array[token];
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (hasO ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		
		public int hashCode() {
			int h = 575551;
			h ^= hasO ? Array.hash( OkeyValue ) : 131111;
			h ^= hasNullKey ? Array.hash( nullKeyValue ) : 997651;
			
			short key;
			
			for (int i = mask; 0 <= i; i--)
				if ((key = keys.array[i]) != 0)
					h = h ^ Array.hash( key ) + h ^ Array.hash( values.array[i] );
			
			return (int) h;
		}
		
		
		@SuppressWarnings("unchecked") public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R<V> other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R<V> other ) {
			
			if (other == null) return -1;
			if (assigned != other.assigned) return assigned - other.assigned;
			if (hasO != other.hasO || hasNullKey != other.hasNullKey) return 1;
			
			int diff;
			if (hasO && (diff = OkeyValue.compareTo( other.OkeyValue )) != 0) return diff;
			if (hasNullKey)
				if (nullKeyValue != null && other.nullKeyValue != null)
				{if ((diff = nullKeyValue.compareTo( other.nullKeyValue )) != 0) return diff;}
				else if (nullKeyValue != other.nullKeyValue) return -2334;
			
			
			V           v;
			short key;
			
			for (int i = keys.array.length, c; -1 < --i; )
				if ((key = keys.array[i]) != 0)
				{
					if ((c = other.token( key )) < 0) return 3;
					v = other.value( c );
					
					if (values.array[i] != null && v != null) {if ((diff = v.compareTo( values.array[i] )) != 0) return diff;}
					else if (values.array[i] != v) return 8;
				}
			return 0;
		}
		
		
		@SuppressWarnings("unchecked")
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
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
			
			
			if (hasNullKey)
			{
				dst.append( "null -> " ).append( nullKeyValue ).append( '\n' );
				size--;
			}
			
			if (hasO)
			{
				dst.append( "0 -> " ).append( OkeyValue ).append( '\n' );
				size--;
			}
			
			for (int token = Iterator.token, i = 0; i < size; i++)
			     dst.append( Iterator.key( this, token = Iterator.token( this, token ) ) )
					     .append( " -> " )
					     .append( Iterator.value( this, token ) )
					     .append( '\n' );
			
			return dst;
		}
	}
	
	class RW<V extends Comparable<? super V>> extends R<V> {
		
		public RW( double loadFactor ) {this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );}
		
		public RW( int expectedItems ) {this( expectedItems, 0.75 );}
		
		
		public RW( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys.length( size );
			values.length( size );
		}
		
		
		public boolean put(  Short     key, V value ) {
			if (key != null) return put( (short) key, value );
			
			hasNullKey   = true;
			nullKeyValue = value;
			
			return true;
		}
		
		public boolean put( short key, V value ) {
			if (key == 0)
			{
				hasO      = true;
				OkeyValue = value;
				return true;
			}
			
			
			int slot = Array.hash( key ) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					values.array[slot] = value;
					return true;
				}
			
			keys.array[slot]   =  key;
			values.array[slot] =            value;
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean remove(  Short     key ) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			return remove( (short) key );
		}
		
		public boolean remove( short key ) {
			
			if (key == 0) return hasO && !(hasO = false);
			
			int slot = Array.hash( key ) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					short kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Array.hash( kk ) & mask) >= distance)
						{
							
							keys.array[gapSlot]   =  kk;
							values.array[gapSlot] = values.array[s];
							                                   gapSlot = s;
							                                   distance = 0;
						}
					
					keys.array[gapSlot]   = 0;
					values.array[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			
			hasO      = false;
			OkeyValue = null;
			
			hasNullKey   = false;
			nullKeyValue = null;
			
			keys.clear();
			values.clear();
		}
		
		
		void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length( -size );
				
				if (values.length() < size) values.length( -size );
				return;
			}
			
			
			final short[] k = keys.array;
			final V[]           v = values.array;
			
			keys.length( -size );
			values.length( -size );
			
			short key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = Array.hash( key ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot]   =  key;
					values.array[slot] = v[i];
				}
		}
		
		
		public RW<V> clone() {return (RW<V>) super.clone();}
	}
}
