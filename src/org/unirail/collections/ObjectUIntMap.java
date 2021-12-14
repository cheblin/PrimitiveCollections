package org.unirail.collections;


import org.unirail.Hash;

public interface ObjectUIntMap {
	
	interface NonNullKeysIterator {
		int END = -1;
		
		static <K extends Comparable<? super K>> int token( R<K> src ) {return token( src, END );}
		
		static <K extends Comparable<? super K>> int token( R<K> src, int token ) {
			for (; ; )
				if (++token == src.keys.array.length) return END;
				else if (src.keys.array[token] != null) return token;
		}
		
		static <K extends Comparable<? super K>> K key( R<K> src, int token ) {return src.keys.array[token];}
		
		static <K extends Comparable<? super K>> long value( R<K> src, int token ) {return src.values.get( token );}
	}
	
	abstract class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>> {
		
		public ObjectList.RW<K> keys = new ObjectList.RW<>( 0 );
		
		public UIntList.RW values = new UIntList.RW( 0 );
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNullKey;
		
		long NullKeyValue = 0;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token( K key ) {
			if (key == null) return hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE;
			
			int slot = Hash.code( key ) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains( K key )    {return !hasNone( token( key ) );}
		
		public long value( @Positive_ONLY int token ) {return token == Positive_Values.VALUE ? NullKeyValue :  (0xFFFFFFFFL &  values.array[token]);}
		
		public boolean hasNone( int token ) {return token == Positive_Values.NONE;}
		
		
		public int size()                   {return assigned + (hasNullKey ? 1 : 0);}
		
		public boolean isEmpty()            {return size() == 0;}
		
		
		public int hashCode() {
			int hash = Hash.code( hasNullKey ? Hash.code( NullKeyValue ) : 719717, keys );
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ))
			     hash = Hash.code( hash, NonNullKeysIterator.key( this, token ) );
			
			return hash;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R<K> other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R<K> other ) {
			if (other == null) return -1;
			
			if (hasNullKey != other.hasNullKey ||
			    hasNullKey && NullKeyValue != other.NullKeyValue) return 1;
			
			int diff;
			if ((diff = size() - other.size()) != 0) return diff;
			
			
			K key;
			for (int i = keys.array.length, token; -1 < --i; )
				if ((key = keys.array[i]) != null)
					if ((token = other.token( key )) == Positive_Values.NONE || values.array[i] != other.value( token )) return 1;
			
			return 0;
		}
		
		@SuppressWarnings("unchecked")
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
		
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			if (hasNullKey) dst.append( "null -> " ).append( NullKeyValue ).append( '\n' );
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ))
			     dst.append( NonNullKeysIterator.key( this, token ) )
					     .append( " -> " )
					     .append( NonNullKeysIterator.value( this, token ) )
					     .append( '\n' );
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
			values.length( size );
		}
		
		public boolean put( K key, long value ) {
			
			if (key == null)
			{
				NullKeyValue = value;
				return !hasNullKey && (hasNullKey = true);
			}
			
			int slot = Hash.code( key ) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					values.array[slot] = (int) value;
					return false;
				}
			
			keys.array[slot]   = key;
			values.array[slot] = (int) value;
			
			if (assigned++ == resizeAt) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean remove( K key ) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = Hash.code( key ) & mask;
			
			final K[] array = keys.array;
			
			for (K k; (k = array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo( key ) == 0)
				{
					int[] vals = values.array;
					
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != null; )
						if ((s - Hash.code( kk ) & mask) >= distance)
						{
							vals[gapSlot] = vals[s];
							array[gapSlot] = kk;
							                 gapSlot = s;
							                 distance = 0;
						}
					
					array[gapSlot] = null;
					vals[gapSlot]  = (int) 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			keys.clear();
			values.clear();
		}
		
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length( -size );
				if (values.length() < size) values.length( -size );
				return;
			}
			
			final K[]           k = keys.array;
			final int[] v = values.array;
			
			keys.length( -size );
			values.length( -size );
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = Hash.code( key ) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot]   = key;
					values.array[slot] = v[i];
				}
		}
		
		public RW<K> clone() {return (RW<K>) super.clone();}
	}
}
	