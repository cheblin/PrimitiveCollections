package org.unirail.collections;


public interface UByteCharNullMap {
	
	
	interface Iterator {
		
		int token = -1;
		
		static int token( R src, int token ) {
			int key  = ByteSet.Iterator.token( src.keys, token );
			int item = token >> 8 & 0xFF;
			
			return (src.values.hasValue( key ) ? Integer.MIN_VALUE | item << 8 : (item + 1 & 0xFF) << 8) | key;
		}
		
		static char key( int token ) {return (char) (token & 0xFF);}
		
		static char value( R src, int token ) {return  src.values.get( token >> 8 );}
	}
	
	abstract class R implements Cloneable, Comparable<R> {
		
		ByteSet.RW             keys = new ByteSet.RW();
		CharNullList.RW values;
		
		
		public int size()                                     {return keys.size();}
		
		public boolean isEmpty()                              {return keys.isEmpty();}
		
		
		public boolean contains(  Character key )           {return !hasNone( token( key ) );}
		
		public boolean contains( char key )            {return !hasNone( token( key ));}
		
		
		public @Positive_Values int token(  Character key ) {return key == null ? hasNullKey : token( (char) (key + 0) );}
		
		public @Positive_Values int token( char key )  {return keys.contains( (byte) key ) ? values.hasValue( key ) ? key : Positive_Values.NULL : Positive_Values.NONE;}
		
		public boolean hasValue( int token )                  {return -1 < token;}
		
		public boolean hasNone( int token )                   {return token == Positive_Values.NONE;}
		
		public boolean hasNull( int token )                   {return token == Positive_Values.NULL;}
		
		
		public char value( @Positive_ONLY int token ) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			return    values.get( keys.rank( (byte) token ) );
		}
		
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		char nullKeyValue = 0;
		
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
				size--;
			}
			
			for (int token = Iterator.token, i = 0; i < size; i++, dst.append( '\n' ))
			{
				dst.append( Iterator.key( token = Iterator.token( this, token ) ) ).append( " -> " );
				
				if (token < 0) dst.append( "null" );
				else dst.append( Iterator.value( this, token ) );
			}
			
			return dst;
		}
	}
	
	class RW extends R {
		
		public RW( int length ) {values = new CharNullList.RW( 265 < length ? 256 : length );}
		
		
		public void clear() {
			keys.clear();
			values.clear();
		}
		
		
		public boolean put(  Character key, char value ) {
			if (key != null) return put( (char) (key + 0), value );
			
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			nullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put(  Character key,  Character value ) {
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
		
		
		public boolean put( char key,  Character value ) {
			if (value != null) return put( key, (char) value );
			
			
			if (keys.add( (byte) key ))
			{
				values.add( keys.rank( (byte) key ) - 1, ( Character) null );
				return true;
			}
			values.set( keys.rank( (byte) key ) - 1, ( Character) null );
			
			return false;
		}
		
		public boolean put( char key, char value ) {
			
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
