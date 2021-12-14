package org.unirail.collections;

import org.unirail.Hash;

public interface ShortSet {
	
	interface NonNullZeroKeysIterator {
		int END = -1;
		
		static int token( R src ) {return token( src, END );}
		
		static int token( R src, int token ) {
			for (; ; )
				if (++token == src.keys.array.length) return END;
				else if (src.keys.array[token] != 0) return token;
		}
		
		static short key( R src, int token ) {return (short)  src.keys.array[token];}
	}
	
	abstract class R implements Cloneable, Comparable<R> {
		
		public ShortList.RW keys = new ShortList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean hasOkey;
		boolean hasNullKey;
		
		protected double loadFactor;
		
		
		public boolean contains(  Short     key ) {return key == null ? hasNullKey : contains( (short) (key + 0) );}
		
		public boolean contains( short key ) {
			if (key == 0) return hasOkey;
			
			int slot = Hash.code( key ) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		
		public int size()        {return assigned + (hasOkey ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		
		public int hashCode() {
			int hash = Hash.code( hasOkey ? 20831323 : 535199981, hasNullKey ? 10910099 : 97654321 );
			
			for (int token = NonNullZeroKeysIterator.token( this ); token != NonNullZeroKeysIterator.END; token = NonNullZeroKeysIterator.token( this, token ))
			     hash = Hash.code( hash, NonNullZeroKeysIterator.key( this, token ) );
			
			return Hash.code( hash, keys );
		}
		
		
		public short[] toArray( short[] dst ) {
			final int size = size();
			if (dst == null || dst.length < size) dst = new short[size];
			
			for (int i = keys.array.length, ii = 0; -1 < --i; )
				if (keys.array[i] != 0) dst[ii++] = keys.array[i];
			
			return dst;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		@Override public int compareTo( R other ) {
			if (other == null) return -1;
			if (other.assigned != assigned) return other.assigned - assigned;
			
			for (short k : keys.array) if (k != 0 && !other.contains( k )) return 1;
			
			return 0;
		}
		
		
		@Override public R clone() {
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
		
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			if (hasNullKey) dst.append( "null\n" );
			
			if (hasOkey) dst.append( "0\n" );
			
			for (int token = NonNullZeroKeysIterator.token( this ); token != NonNullZeroKeysIterator.END; token = NonNullZeroKeysIterator.token( this, token ))
			     dst.append( NonNullZeroKeysIterator.key( this, token ) ).append( '\n' );
			return dst;
		}
	}
	
	
	class RW extends R {
		
		public RW( int expectedItems ) {this( expectedItems, 0.75 );}
		
		public RW( double loadFactor ) {this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );}
		
		
		public RW( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys.length( size );
		}
		
		public RW( short... items ) {
			this( items.length );
			for (short key : items) add( key );
		}
		
		public RW(  Short    ... items ) {
			this( items.length );
			for ( Short     key : items) add( key );
		}
		
		public boolean add(  Short     key ) {return key == null ? !hasNullKey && (hasNullKey = true) : add( (short) key );}
		
		public boolean add( short  key ) {
			
			if (key == 0) return !hasOkey && (hasOkey = true);
			
			int slot = Hash.code( key ) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return false;
			
			keys.array[slot] =  key;
			
			if (assigned++ == resizeAt) allocate( mask + 1 << 1 );
			return true;
		}
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length( -size );
				return;
			}
			
			final short[] k = keys.array;
			
			keys.length( -size );
			
			short key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = Hash.code( key ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] = key;
				}
		}
		
		
		public boolean remove(  Short     key ) {return key == null ? hasNullKey && !(hasNullKey = false) : remove( (short) key );}
		
		public boolean remove( short key ) {
			
			if (key == 0) return hasOkey && !(hasOkey = false);
			
			int slot = Hash.code( key ) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					short kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Hash.code( kk ) & mask) >= distance)
						{
							keys.array[gapSlot] =  kk;
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
			assigned   = 0;
			hasOkey    = false;
			hasNullKey = false;
			
			keys.clear();
		}
		
		
		public void retainAll( RW chk ) {
			short key;
			
			for (int i = keys.array.length; -1 < --i; )
				if ((key = keys.array[i]) != 0 && !chk.add( key )) remove( key );
			
			if (hasOkey && !chk.add( (short) 0 )) hasOkey = false;
		}
		
		public int removeAll( R src ) {
			int fix = size();
			
			for (int i = 0, s = src.size(), token = NonNullZeroKeysIterator.END; i < s; i++) remove( NonNullZeroKeysIterator.key( src, token = NonNullZeroKeysIterator.token( src, token ) ) );
			
			return fix - size();
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}