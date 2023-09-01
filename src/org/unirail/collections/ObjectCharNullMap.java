package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;


public interface ObjectCharNullMap {
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K> int token( R<K> src, int token ) {
			for( token++; ; token++ )
				if( token == src.keys.length ) return INIT;
				else if( src.keys[token] != null ) return token;
		}
		
		static <K> K key( R<K> src, int token )            { return src.keys[token]; }
		
		static <K> boolean hasValue( R<K> src, int token ) { return src.values.hasValue( token ); }
		
		static <K> char value( R<K> src, int token ) { return src.values.get( token ); }
	}
	
	abstract class R<K> implements Cloneable, JsonWriter.Source {
		
		protected final Array.Of<K> array;
		private final   boolean     K_is_string;
		
		protected R( Class<K> clazzK ) {
			array       = Array.get( clazzK );
			K_is_string = clazzK == String.class;
		}
		
		K[]                    keys;
		CharNullList.RW values;
		
		protected int    assigned;
		protected int    mask;
		protected int    resizeAt;
		protected double loadFactor;
		
		
		public int size()                { return assigned + (hasNullKey == Positive_Values.NONE ? 0 : 1); }
		
		public boolean isEmpty()         { return size() == 0; }
		
		
		public boolean contains( K key ) { return !hasNone( token( key ) ); }
		
		public @Positive_Values int token( K key ) {
			
			if( key == null ) return hasNullKey == Positive_Values.VALUE ? keys.length : hasNullKey;
			
			int slot = array.hashCode( key ) & mask;
			
			for( K k; (k = keys[slot]) != null; slot = slot + 1 & mask )
				if( array.equals( k, key ) ) return (values.hasValue( slot )) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;
		}
		
		public boolean hasValue( int token ) { return -1 < token; }
		
		public boolean hasNone( int token )  { return token == Positive_Values.NONE; }
		
		public boolean hasNull( int token )  { return token == keys.length; }
		
		public char value( @Positive_ONLY int token ) { return token == keys.length ? NullKeyValue : values.get( token ); }
		
		
		//Compute a hash that is symmetric in its arguments - that is a hash
		//where the order of appearance of elements does not matter.
		//This is useful for hashing sets, for example.
		public int hashCode() {
			int a = 0;
			int b = 0;
			int c = 1;
			
			for( int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
			{
				int h = Array.mix( seed, Array.hash( NonNullKeysIterator.key( this, token ) ) );
				h = Array.mix( h, Array.hash( NonNullKeysIterator.hasValue( this, token ) ? NonNullKeysIterator.value( this, token ) : seed ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey != Positive_Values.NONE )
			{
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( hasNullKey == Positive_Values.VALUE ? Array.hash( NullKeyValue ) : seed ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R<K> other ) {
			if( other == null || hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && NullKeyValue != other.NullKeyValue || size() != other.size() ) return false;
			
			
			K key;
			for( int i = keys.length, token; -1 < --i; )
				if( (key = keys[i]) != null )
					if( values.nulls.get( i ) )
					{
						if( (token = other.token( key )) == -1 || values.get( i ) != other.value( token ) ) return false;
					}
					else if( -1 < other.token( key ) ) return false;
			
			return true;
		}
		
		@Override @SuppressWarnings( "unchecked" )
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e )
			{
				e.printStackTrace();
			}
			return null;
		}
		
		protected @Positive_Values int         hasNullKey   = Positive_Values.NONE;
		protected                  char NullKeyValue = 0;
		
		
		public Array.ISort.Objects<K> getK = null;
		public Array.ISort.Primitives getV = null;
		
		public void build_K( Array.ISort.Anything.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get( int index ) { return keys[index]; }
			} : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != null ) dst.dst[k++] = (char) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_V( Array.ISort.Primitives.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = index -> values.hasValue( index ) ?  value( index ) : 0 : getV;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != null ) dst.dst[k++] = (char) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_K( Array.ISort.Anything.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get( int index ) { return keys[index]; }
			} : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != null ) dst.dst[k++] = (char) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_V( Array.ISort.Primitives.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = index -> values.hasValue( index ) ?  value( index ) : 0 : getV;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != null ) dst.dst[k++] = (char) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			
			
			if( 0 < assigned )
			{
				json.preallocate( assigned * 10 );
				int token = NonNullKeysIterator.INIT;
				
				if( K_is_string )
				{
					json.enterObject();
					
					switch( hasNullKey )
					{
						case Positive_Values.NULL:
							json.name().value();
							break;
						case Positive_Values.VALUE:
							json.name().value( NullKeyValue );
					}
					
					while( (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT )
					{
						json.name( NonNullKeysIterator.key( this, token ).toString() );
						if( NonNullKeysIterator.hasValue( this, token ) ) json.value( NonNullKeysIterator.value( this, token ) );
						else json.value();
					}
					
					json.exitObject();
				}
				else
				{
					json.enterArray();
					
					switch( hasNullKey )
					{
						case Positive_Values.NULL:
							json.name().value();
							json.
									enterObject()
									.name( "Key" ).value()
									.name( "Value" ).value().
									exitObject();
							
							break;
						case Positive_Values.VALUE:
							json.
									enterObject()
									.name( "Key" ).value()
									.name( "Value" ).value( NullKeyValue ).
									exitObject();
					}
					
					while( (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT )
					{
						json.
								enterObject()
								.name( "Key" ).value( NonNullKeysIterator.key( this, token ) )
								.name( "Value" );
						
						if( NonNullKeysIterator.hasValue( this, token ) ) json.value( NonNullKeysIterator.value( this, token ) );
						else json.value();
						
						json.exitObject();
					}
					json.exitArray();
				}
			}
			else
			{
				json.enterObject();
				switch( hasNullKey )
				{
					case Positive_Values.NULL:
						json.name().value();
						break;
					case Positive_Values.VALUE:
						json.name().value( NullKeyValue );
				}
				json.exitObject();
			}
		}
	}
	
	interface Interface<K> {
		int size();
		
		boolean contains( K key );
		
		@Positive_Values int token( K key );
		
		boolean hasValue( int token );
		
		boolean hasNone( int token );
		
		boolean hasNull( int token );
		
		char value( @Positive_ONLY int token );
		
		boolean put( K key,  Character value );
		
		boolean put( K key, char value );
	}
	
	class RW<K> extends R<K> implements Interface<K> {
		
		public RW( Class<K> clazz, int expectedItems ) { this( clazz, expectedItems, 0.75 ); }
		
		public RW( Class<K> clazz, int expectedItems, double loadFactor ) {
			super( clazz );
			
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			long length = (long) Math.ceil( expectedItems / loadFactor );
			
			int size = (int) (length == expectedItems ? length + 1 : Math.max( 4, org.unirail.collections.Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys   = array.copyOf( null, size );
			values = new CharNullList.RW( size );
		}
		
		@Override public RW<K> clone() { return (RW<K>) super.clone(); }
		
		
		public boolean put( K key,  Character value ) {
			if( value != null ) put( key, (char) value );
			
			if( key == null )
			{
				hasNullKey = Positive_Values.NULL;
				return true;
			}
			
			int slot = array.hashCode( key ) & mask;
			
			
			for( K k; (k = keys[slot]) != null; slot = slot + 1 & mask )
				if( array.equals( k, key ) )
				{
					values.set( slot, ( Character) null );
					return true;
				}
			
			keys[slot] = key;
			values.set( slot, ( Character) null );
			
			if( ++assigned == resizeAt ) this.allocate( mask + 1 << 1 );
			
			return true;
		}
		
		public boolean put( K key, char value ) {
			
			if( key == null )
			{
				int h = hasNullKey;
				hasNullKey   = Positive_Values.VALUE;
				NullKeyValue = value;
				return h != Positive_Values.VALUE;
			}
			
			int slot = array.hashCode( key ) & mask;
			
			
			for( K k; (k = keys[slot]) != null; slot = slot + 1 & mask )
				if( array.equals( k, key ) )
				{
					values.set( slot, value );
					return true;
				}
			
			keys[slot] = key;
			values.set( slot, value );
			
			if( ++assigned == resizeAt ) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		public void clear() {
			assigned   = 0;
			hasNullKey = Positive_Values.NONE;
			for( int i = keys.length - 1; i >= 0; i-- ) keys[i] = null;
			values.clear();
		}
		
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if( assigned < 1 )
			{
				if( keys.length < size ) keys = array.copyOf( null, size );
				
				if( values.nulls.length() < size ) values.nulls.length( -size );
				if( values.values.values.length < size ) values.values.values = Arrays.copyOf( values.values.values, size );
				
				return;
			}
			
			final K[]              k    = keys;
			CharNullList.RW vals = values;
			
			keys   = array.copyOf( null, size );
			values = new CharNullList.RW( -size );
			
			K key;
			for( int i = k.length; -1 < --i; )
				if( (key = k[i]) != null )
				{
					int slot = array.hashCode( key ) & mask;
					while( !(keys[slot] == null) ) slot = slot + 1 & mask;
					
					keys[slot] = key;
					
					if( vals.hasValue( i ) ) values.set( slot, vals.get( i ) );
					else values.set( slot, ( Character) null );
				}
		}
		
		
		public boolean remove( K key ) {
			if( key == null )
			{
				int h = hasNullKey;
				hasNullKey = Positive_Values.NONE;
				return h != Positive_Values.NONE;
			}
			
			int slot = array.hashCode( key ) & mask;
			
			for( K k; (k = keys[slot]) != null; slot = slot + 1 & mask )
				if( array.equals( k, key ) )
				{
					int gapSlot = slot;
					
					K kk;
					for( int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != null; )
						if( (s - array.hashCode( kk ) & mask) >= distance )
						{
							keys[gapSlot] = kk;
							
							if( values.nulls.get( s ) )
								values.set( gapSlot, values.get( s ) );
							else
								values.set( gapSlot, ( Character) null );
							
							gapSlot  = s;
							distance = 0;
						}
					
					keys[gapSlot] = null;
					values.set( gapSlot, ( Character) null );
					assigned--;
					return true;
				}
			
			return false;
		}
	}
}
	
	