package org.unirail.collections;

import org.unirail.Hash;

public interface CharSet {
	
	interface NonNullKeysIterator {
		int END = -1;
		
		static int token( R src ) {return token( src, END );}
		
		static int token( R src, int token ) {
			for (int len = src.keys.array.length; ; )
				if (++token == len) return src.has0Key ? -2 : END;
				else if (token == 0x7FFF_FFFF) return END;
				else if (src.keys.array[token] != 0) return token;
		}
		
		static char key( R src, int token ) {return token == -2 ? 0 : (char)  src.keys.array[token];}
	}
	
	abstract class R implements Cloneable, Comparable<R> {
		
		public CharList.RW keys = new CharList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean has0Key;
		boolean hasNullKey;
		
		protected double loadFactor;
		
		
		public boolean contains(  Character key ) {return key == null ? hasNullKey : contains( (char) (key + 0) );}
		
		public boolean contains( char key ) {
			if (key == 0) return has0Key;
			
			int slot = Hash.code( key ) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		
		public int size()        {return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		
		public int hashCode() {
			int hash = hasNullKey ? 10910099 : 97654321;
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ))
			     hash = Hash.code( hash, NonNullKeysIterator.key( this, token ) );
			
			return Hash.code( hash, keys );
		}
		
		
		public char[] toArray( char[] dst ) {
			final int size = size();
			if (dst == null || dst.length < size) dst = new char[size];
			
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
			
			for (char k : keys.array) if (k != 0 && !other.contains( k )) return 1;
			
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
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ))
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
		
		public RW( char... items ) {
			this( items.length );
			for (char key : items) add( key );
		}
		
		public RW(  Character... items ) {
			this( items.length );
			for ( Character key : items) add( key );
		}
		
		public boolean add(  Character key ) {return key == null ? !hasNullKey && (hasNullKey = true) : add( (char) key );}
		
		public boolean add( char  key ) {
			
			if (key == 0) return !has0Key && (has0Key = true);
			
			int slot = Hash.code( key ) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
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
			
			final char[] k = keys.array;
			
			keys.length( -size );
			
			char key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = Hash.code( key ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] = key;
				}
		}
		
		
		public boolean remove(  Character key ) {return key == null ? hasNullKey && !(hasNullKey = false) : remove( (char) key );}
		
		public boolean remove( char key ) {
			
			if (key == 0) return has0Key && !(has0Key = false);
			
			int slot = Hash.code( key ) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					char kk;
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
			has0Key    = false;
			hasNullKey = false;
			
			keys.clear();
		}
		
		
		public void retainAll( RW chk ) {
			char key;
			
			for (int i = keys.array.length; -1 < --i; )
				if ((key = keys.array[i]) != 0 && !chk.add( key )) remove( key );
			
			if (has0Key && !chk.add( (char) 0 )) has0Key = false;
		}
		
		public int removeAll( R src ) {
			int fix = size();
			
			for (int i = 0, s = src.size(), token = NonNullKeysIterator.END; i < s; i++) remove( NonNullKeysIterator.key( src, token = NonNullKeysIterator.token( src, token ) ) );
			
			return fix - size();
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}