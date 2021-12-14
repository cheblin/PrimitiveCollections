package org.unirail.collections;

import org.unirail.Hash;

public interface CharUShortMap {
	
	
	interface NonNullZeroKeysIterator {
		int END = -1;
		
		static int token( R src ) {return token( src, END );}
		
		static int token( R src, int token ) {
			for (; ; )
				if (++token == src.keys.array.length) return END;
				else if (src.keys.array[token] != 0) return token;
		}
		
		static char key( R src, int token ) {return  (char) src.keys.array[token];}
		
		static char value( R src, int token ) {return   src.values.array[token];}
	}
	
	
	abstract class R implements Cloneable, Comparable<R> {
		public CharList.RW keys   = new CharList.RW( 0 );
		public UShortList.RW values = new UShortList.RW( 0 );
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNullKey;
		char       nullKeyValue;
		
		boolean hasO;
		char       OkeyValue;
		
		protected double loadFactor;
		
		
		public boolean isEmpty()                                 {return size() == 0;}
		
		public int size()                                        {return assigned + (hasO ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		public boolean contains(  Character key )           {return !hasNone( token( key ) );}
		
		public boolean contains( char key )               {return !hasNone( token( key ) );}
		
		public @Positive_Values int token(  Character key ) {return key == null ? hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE : token( (char) (key + 0) );}
		
		public @Positive_Values int token( char key ) {
			if (key == 0) return hasO ? Positive_Values.VALUE - 1 : Positive_Values.NONE;
			
			int slot = Hash.code( key ) & mask;
			
			for (char key_ =  key , k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return Positive_Values.NONE;
		}
		
		
		public boolean hasValue( int token ) {return -1 < token;}
		
		public boolean hasNone( int token )  {return token == Positive_Values.NONE;}
		
		public char value( @Positive_ONLY int token ) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			if (token == Positive_Values.VALUE - 1) return OkeyValue;
			
			return  values.get( token );
		}
		
		public int hashCode() {
			int hash = Hash.code( hasO ? Hash.code(OkeyValue) : 616079, hasNullKey ? Hash.code(nullKeyValue) : 331997 );
			
			for (int token = NonNullZeroKeysIterator.token( this ); token != NonNullZeroKeysIterator.END; token = NonNullZeroKeysIterator.token( this, token ))
			     hash = Hash.code( Hash.code( hash, NonNullZeroKeysIterator.key( this, token ) ), NonNullZeroKeysIterator.value( this, token ) );
			return hash;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		@Override public int compareTo( R other ) {
			if (other == null) return -1;
			
			if (hasO != other.hasO || hasNullKey != other.hasNullKey) return 1;
			
			int diff;
			if (hasO && (OkeyValue - other.OkeyValue) != 0) return 7;
			if (hasNullKey && nullKeyValue != other.nullKeyValue) return 8;
			
			if ((diff = size() - other.size()) != 0) return diff;
			
			char           key;
			for (int i = keys.array.length, c; -1 < --i; )
				if ((key = (char) keys.array[i]) != 0)
				{
					if ((c = other.token( key )) < 0) return 3;
					if (other.value( c ) !=   values.array[i]) return 1;
				}
			
			return 0;
		}
		
		@Override public R clone() {
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
			
			if (hasNullKey) dst.append( "null -> " ).append( nullKeyValue ).append( '\n' );
			
			if (hasO) dst.append( "0 -> " ).append( OkeyValue ).append( '\n' );
			
			for (int token = NonNullZeroKeysIterator.token( this ); token != NonNullZeroKeysIterator.END; token = NonNullZeroKeysIterator.token( this, token ))
			     dst.append( NonNullZeroKeysIterator.key( this, token ) )
					     .append( " -> " )
					     .append( NonNullZeroKeysIterator.value( this, token ) )
					     .append( '\n' );
			return dst;
		}
	}
	
	
	class RW extends R {
		
		public RW( int expectedItems ) {this( expectedItems, 0.75 );}
		
		
		public RW( int expectedItems, double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys.length( size );
			values.length( size );
		}
		
		
		public boolean put(  Character key, char value ) {
			if (key != null) return put( (char) key, value );
			
			hasNullKey   = true;
			nullKeyValue = value;
			
			return true;
		}
		
		public boolean put( char key, char value ) {
			
			if (key == 0)
			{
				if (hasO)
				{
					OkeyValue = value;
					return true;
				}
				hasO      = true;
				OkeyValue = value;
				return false;
			}
			
			
			int slot = Hash.code( key ) & mask;
			
			final char key_ =  key;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.array[slot] = (char) value;
					return true;
				}
			
			keys.array[slot]   = key_;
			values.array[slot] = (char) value;
			
			if (++assigned == resizeAt) allocate( mask + 1 << 1 );
			
			return false;
		}
		
		
		public boolean remove(  Character key ) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			return remove( (char) key );
		}
		
		public boolean remove( char key ) {
			
			if (key == 0) return hasO && !(hasO = false);
			
			int slot = Hash.code( key ) & mask;
			
			final char key_ =  key;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					char kk;
					
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Hash.code( kk ) & mask) >= distance)
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
			assigned   = 0;
			hasO       = false;
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
			
			final char[] k = keys.array;
			final char[] v = values.array;
			
			keys.length( -size );
			values.length( -size );
			
			char key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = Hash.code( key ) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot]   = key;
					values.array[slot] = v[i];
				}
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
