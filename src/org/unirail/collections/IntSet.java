package org.unirail.collections;


import static org.unirail.collections.Array.HashEqual.hash;

public interface IntSet {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token( R src, int token ) {
			for (int len = src.keys.array.length; ; )
				if (++token == len) return src.has0Key ? -2 : INIT;
				else if (token == 0x7FFF_FFFF) return INIT;
				else if (src.keys.array[token] != 0) return token;
		}
		
		static int key( R src, int token ) {return token == -2 ? 0 :   src.keys.array[token];}
	}
	
	abstract class R implements Cloneable {
		
		public IntList.RW keys = new IntList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean has0Key;
		boolean hasNullKey;
		
		protected double loadFactor;
		
		
		public boolean contains(  Integer   key ) {return key == null ? hasNullKey : contains( (int) (key + 0) );}
		
		public boolean contains( int key ) {
			if (key == 0) return has0Key;
			
			int slot = hash( key ) & mask;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		
		public int size()        {return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		
		public int hashCode() {
			int hash = hasNullKey ? 10910099 : 97654321;
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
			     hash = hash( hash, NonNullKeysIterator.key( this, token ) );
			
			return hash( hash, keys );
		}
		
		
		public int[] toArray( int[] dst ) {
			final int size = size();
			if (dst == null || dst.length < size) dst = new int[size];
			
			for (int i = keys.array.length, ii = 0; -1 < --i; )
				if (keys.array[i] != 0) dst[ii++] = keys.array[i];
			
			return dst;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			if (other == null || other.assigned != assigned) return false;
			
			for (int k : keys.array) if (k != 0 && !other.contains( k )) return false;
			
			return true;
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
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
			     dst.append( NonNullKeysIterator.key( this, token ) ).append( '\n' );
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
		
		public RW( int... items ) {
			this( items.length );
			for (int key : items) add( key );
		}
		
		public RW(  Integer  ... items ) {
			this( items.length );
			for ( Integer   key : items) add( key );
		}
		
		public boolean add(  Integer   key ) {return key == null ? !hasNullKey && (hasNullKey = true) : add( (int) key );}
		
		public boolean add( int  key ) {
			
			if (key == 0) return !has0Key && (has0Key = true);
			
			int slot = hash( key ) & mask;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
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
			
			final int[] k = keys.array;
			
			keys.length( -size );
			
			int key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = hash( key ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] = key;
				}
		}
		
		
		public boolean remove(  Integer   key ) {return key == null ? hasNullKey && !(hasNullKey = false) : remove( (int) key );}
		
		public boolean remove( int key ) {
			
			if (key == 0) return has0Key && !(has0Key = false);
			
			int slot = hash( key ) & mask;
			
			for (int k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					int kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hash( kk ) & mask) >= distance)
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
			has0Key    = false;
			hasNullKey = false;
			
			keys.clear();
		}
		
		
		public void retainAll( RW chk ) {
			int key;
			
			for (int i = keys.array.length; -1 < --i; )
				if ((key = keys.array[i]) != 0 && !chk.add( key )) remove( key );
			
			if (has0Key && !chk.add( (int) 0 )) has0Key = false;
		}
		
		public int removeAll( R src ) {
			int fix = size();
			
			for (int i = 0, s = src.size(), token = NonNullKeysIterator.INIT; i < s; i++) remove( NonNullKeysIterator.key( src, token = NonNullKeysIterator.token( src, token ) ) );
			
			return fix - size();
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}