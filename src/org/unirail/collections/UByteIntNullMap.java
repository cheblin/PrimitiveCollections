package org.unirail.collections;


import org.unirail.Hash;

public interface UByteIntNullMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static int token( R src, int token )        {return ByteSet.NonNullKeysIterator.token( src.keys, token );}
		
		static char key( R src, int token ) {return (char) (ByteSet.NonNullKeysIterator.key( src.keys, token ));}
		
		static boolean hasValue( R src, int token ) {return src.values.hasValue( ByteSet.NonNullKeysIterator.index( null, token ) );}
		
		static int value( R src, int token ) {return  src.values.get( ByteSet.NonNullKeysIterator.index( null, token ) );}
	}
	
	abstract class R implements Cloneable, Comparable<R> {
		
		ByteSet.RW             keys = new ByteSet.RW();
		IntNullList.RW values;
		
		
		public int size()                                     {return keys.size();}
		
		public boolean isEmpty()                              {return keys.isEmpty();}
		
		
		public boolean contains(  Character key )           {return !hasNone( token( key ) );}
		
		public boolean contains( char key )            {return !hasNone( token( key ) );}
		
		
		public @Positive_Values int token(  Character key ) {return key == null ? hasNullKey : token( (char) (key + 0) );}
		
		public @Positive_Values int token( char key )  {return keys.contains( (byte) key ) ? values.hasValue( key ) ? key : Positive_Values.NULL : Positive_Values.NONE;}
		
		public boolean hasValue( int token )                  {return -1 < token;}
		
		public boolean hasNone( int token )                   {return token == Positive_Values.NONE;}
		
		public boolean hasNull( int token )                   {return token == Positive_Values.NULL;}
		
		
		public int value( @Positive_ONLY int token ) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			return    values.get( keys.rank( (byte) token ) );
		}
		
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		int nullKeyValue = 0;
		
		public int hashCode() {
			return Hash.code( Hash.code( hasNullKey == Positive_Values.NULL ? 553735009 : hasNullKey == Positive_Values.NONE ? 10019689 : Hash.code( nullKeyValue ), keys ), values );
		}
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		@Override public int compareTo( R other ) {
			if (other == null) return -1;
			
			int diff;
			if (hasNullKey != other.nullKeyValue || hasNullKey == Positive_Values.VALUE && nullKeyValue != other.nullKeyValue) return 1;
			if ((diff = other.keys.compareTo( keys )) != 0 || (diff = other.values.compareTo( values )) != 0 || (diff = other.values.compareTo( values )) != 0) return diff;
			
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
		
		
		//endregion
		public String toString() {return toString( null ).toString();}
		
		StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			if (keys.hasNullKey)
			{
				dst.append( "null -> " ).append( nullKeyValue ).append( '\n' );
			}
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; dst.append( '\n' ))
			{
				dst.append( NonNullKeysIterator.key( this, token ) ).append( " -> " );
				
				if (NonNullKeysIterator.hasValue( this, token )) dst.append( NonNullKeysIterator.value( this, token ) );
				else dst.append( "null" );
			}
			
			return dst;
		}
	}
	
	class RW extends R {
		
		public RW( int length ) {values = new IntNullList.RW( 265 < length ? 256 : length );}
		
		
		public void clear() {
			keys.clear();
			values.clear();
		}
		
		
		public boolean put(  Character key, int value ) {
			if (key != null) return put( (char) (key + 0), value );
			
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			nullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put(  Character key,  Integer   value ) {
			if (key != null) return put( (char) (key + 0), value );
			
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
		
		
		public boolean put( char key,  Integer   value ) {
			if (value != null) return put( key, (int) value );
			
			
			if (keys.add( (byte) key ))
			{
				values.add( keys.rank( (byte) key ) - 1, ( Integer  ) null );
				return true;
			}
			values.set( keys.rank( (byte) key ) - 1, ( Integer  ) null );
			
			return false;
		}
		
		public boolean put( char key, int value ) {
			
			if (keys.add( (byte) key ))
			{
				values.add( keys.rank( (byte) key ) - 1, value );
				return true;
			}
			
			values.set( keys.rank( (byte) key ) - 1, value );
			return false;
		}
		
		public boolean remove(  Character  key ) {
			return key == null ?
			       hasNullKey != Positive_Values.NONE && (hasNullKey = Positive_Values.NONE) == Positive_Values.NONE :
			       remove( (char) (key + 0) );
		}
		
		public boolean remove( char key ) {
			final byte k = (byte) key;
			if (!keys.contains( k )) return false;
			
			values.remove( keys.rank( k ) - 1 );
			keys.remove( k );
			return true;
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
