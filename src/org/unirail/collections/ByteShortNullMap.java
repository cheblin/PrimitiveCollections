package org.unirail.collections;


import org.unirail.JsonWriter;

public interface ByteShortNullMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static int token( R src, int token )        { return ByteSet.NonNullKeysIterator.token( src.keys, token ); }
		
		static byte key( R src, int token ) { return (byte) (ByteSet.NonNullKeysIterator.key( src.keys, token )); }
		
		static boolean hasValue( R src, int token ) { return src.values.hasValue( ByteSet.NonNullKeysIterator.index( null, token ) ); }
		
		static short value( R src, int token ) { return  src.values.get( ByteSet.NonNullKeysIterator.index( null, token ) ); }
	}
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		ByteSet.RW             keys = new ByteSet.RW();
		ShortNullList.RW values;
		
		
		public int size()                                     { return keys.size(); }
		
		public boolean isEmpty()                              { return keys.isEmpty(); }
		
		
		public boolean contains(  Byte      key )           { return !hasNone( token( key ) ); }
		
		public boolean contains( byte key )           { return !hasNone( token( key ) ); }
		
		
		public @Positive_Values int token(  Byte      key ) { return key == null ? hasNullKey == Positive_Values.VALUE ? 256 : hasNullKey : token( (byte) (key + 0) ); }
		
		public @Positive_Values int token( byte key ) {
			if( keys.contains( (byte) key ) )
			{
				int i = keys.rank( (byte) key ) - 1;
				return values.hasValue( i ) ? i : Positive_Values.NULL;
			}
			return Positive_Values.NONE;
		}
		
		public boolean hasValue( int token ) { return -1 < token; }
		
		public boolean hasNone( int token )  { return token == Positive_Values.NONE; }
		
		public boolean hasNull( int token )  { return token == Positive_Values.NULL; }
		
		public short value( @Positive_ONLY int token ) { return token == 256 ? NullKeyValue :    values.get( (byte) token ); }
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		short NullKeyValue = 0;
		
		public int hashCode() {
			return Array.finalizeHash( Array.hash( Array.hash( hasNullKey == Positive_Values.NULL ? 553735009 : hasNullKey == Positive_Values.NONE ? 10019689 : Array.hash( NullKeyValue ), keys ), values ), size() );
		}
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			return other != null && hasNullKey == other.hasNullKey &&
			       (hasNullKey != Positive_Values.VALUE || NullKeyValue == other.NullKeyValue)
			       && other.keys.equals( keys ) && other.values.equals( values );
		}
		
		@Override public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e )
			{
				e.printStackTrace();
			}
			return null;
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			json.enterObject();
			
			switch( hasNullKey )
			{
				case Positive_Values.NULL:
					json.name().value();
					break;
				case Positive_Values.VALUE:
					if( keys.hasNullKey ) json.name().value( NullKeyValue );
			}
			
			int size = keys.size;
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				for( int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
				{
					json.name( NonNullKeysIterator.key( this, token ) );
					if( NonNullKeysIterator.hasValue( this, token ) ) json.value();
					else json.value( NonNullKeysIterator.value( this, token ) );
				}
			}
			
			json.exitObject();
		}
	}
	interface Interface {
		int size();
		boolean contains(  Byte      key );
		boolean contains( byte key );
		@Positive_Values int token(  Byte      key );
		@Positive_Values int token( byte key );
		default boolean hasValue( int token ) { return -1 < token; }
		boolean put(  Byte      key, short value );
		boolean put(  Byte      key,  Short     value );
		boolean put( byte key,  Short     value );
		boolean put( byte key, short value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length ) { values = new ShortNullList.RW( 265 < length ? 256 : length ); }
		
		
		public void clear() {
			keys.clear();
			values.clear();
		}
		
		
		public boolean put(  Byte      key, short value ) {
			if( key != null ) return put( (byte) (key + 0), value );
			
			keys.add( null );
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			NullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put(  Byte      key,  Short     value ) {
			if( key != null ) return put( (byte) (key + 0), value );
			
			keys.add( null );
			int h = hasNullKey;
			
			if( value == null )
			{
				hasNullKey = Positive_Values.NULL;
				return h == Positive_Values.NULL;
			}
			
			hasNullKey   = Positive_Values.VALUE;
			NullKeyValue = value;
			return h == Positive_Values.VALUE;
		}
		
		
		public boolean put( byte key,  Short     value ) {
			if( value != null ) return put( key, (short) value );
			
			
			if( keys.add( (byte) key ) )
			{
				values.add( keys.rank( (byte) key ) - 1, ( Short    ) null );
				return true;
			}
			values.set( keys.rank( (byte) key ) - 1, ( Short    ) null );
			
			return false;
		}
		
		public boolean put( byte key, short value ) {
			
			if( keys.add( (byte) key ) )
			{
				values.add( keys.rank( (byte) key ) - 1, value );
				return true;
			}
			
			values.set( keys.rank( (byte) key ) - 1, value );
			return false;
		}
		
		public boolean remove(  Byte       key ) {
			if( key == null )
			{
				hasNullKey = Positive_Values.NONE;
				keys.remove( null );
			}
			
			return remove( (byte) (key + 0) );
		}
		
		public boolean remove( byte key ) {
			final byte k = (byte) key;
			if( !keys.contains( k ) ) return false;
			
			values.remove( keys.rank( k ) - 1 );
			keys.remove( k );
			return true;
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
