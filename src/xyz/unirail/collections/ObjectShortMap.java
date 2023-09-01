package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

public interface ObjectShortMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static < K > int token( R< K > src, int token ) {
			for( ; ; )
				if( ++token == src.keys.length ) return INIT;
				else if( src.keys[ token ] != null ) return token;
		}
		
		static < K > K key( R< K > src, int token ) { return src.keys[ token ]; }
		
		static < K > short value( R< K > src, int token ) { return  (short) src.values[ token ]; }
	}
	
	abstract class R< K > implements Cloneable, JsonWriter.Source {
		
		protected final Array.Of< K > ofK;
		private final   boolean       K_is_string;
		
		protected R( Class< K > clazz ) {
			ofK         = Array.get( clazz );
			K_is_string = clazz == String.class;
		}
		
		protected R( Array.Of< K > ofK ) {
			this.ofK    = ofK;
			K_is_string = false;
		}
		
		K[] keys;
		public short[] values;
		
		protected int     assigned;
		protected int     mask;
		protected int     resizeAt;
		protected boolean hasNullKey;
		
		short NullKeyValue = 0;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token( K key ) {
			if( key == null ) return hasNullKey ? keys.length + 1 : Positive_Values.NONE;
			
			int slot = ofK.hashCode( key ) & mask;
			
			for( K k; ( k = keys[ slot ] ) != null; slot = slot + 1 & mask )
				if( ofK.equals( k, key ) ) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains( K key )    { return !hasNone( token( key ) ); }
		
		public short value( @Positive_ONLY int token ) { return token == keys.length + 1 ? NullKeyValue :  (short) values[ token ]; }
		
		public boolean hasNone( int token ) { return token == Positive_Values.NONE; }
		
		
		public int size()                   { return assigned + ( hasNullKey ? 1 : 0 ); }
		
		public boolean isEmpty()            { return size() == 0; }
		
		
		public int hashCodew() {
			int hash = Array.hash( hasNullKey ? Array.hash( NullKeyValue ) : 719717, keys );
			for( int token = NonNullKeysIterator.INIT, h = 719717; ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT; )
			     hash = ( h++ ) + Array.hash( hash, ofK.hashCode( NonNullKeysIterator.key( this, token ) ) );
			
			return hash;
		}
		
		//Compute a hash that is symmetric in its arguments - that is a hash
		//where the order of appearance of elements does not matter.
		//This is useful for hashing sets, for example.
		public int hashCode() {
			int a = 0;
			int b = 0;
			int c = 1;
			
			for( int token = NonNullKeysIterator.INIT; ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT; ) {
				int h = Array.mix( seed, Array.hash( NonNullKeysIterator.key( this, token ) ) );
				h = Array.mix( h, Array.hash( NonNullKeysIterator.value( this, token ) ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				final int h = Array.finalizeHash( Array.mix( Array.hash( seed ), Array.hash( NullKeyValue ) ), 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) ); }
		
		public boolean equals( R< K > other ) {
			if( other == null || hasNullKey != other.hasNullKey || hasNullKey && NullKeyValue != other.NullKeyValue || size() != other.size() )
				return false;
			
			K key;
			for( int i = keys.length, token; -1 < --i; )
				if( ( key = keys[ i ] ) != null && ( ( token = other.token( key ) ) == Positive_Values.NONE || values[ i ] != other.value( token ) ) )
					return false;
			
			return true;
		}
		
		@SuppressWarnings( "unchecked" )
		public R< K > clone() {
			try {
				R< K > dst = ( R< K > ) super.clone();
				
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		public Array.ISort.Objects< K > getK = null;
		public Array.ISort.Primitives   getV = null;
		
		public void build_K( Array.ISort.Anything.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[ assigned ];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects< K >() {
				@Override K get( int index ) { return keys[ index ]; }
			} : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != null ) dst.dst[ k++ ] = ( char ) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_V( Array.ISort.Primitives.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[ assigned ];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = index -> (short) values[ index ] : getV;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != null ) dst.dst[ k++ ] = ( char ) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_K( Array.ISort.Anything.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[ assigned ];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects< K >() {
				@Override K get( int index ) { return keys[ index ]; }
			} : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != null ) dst.dst[ k++ ] = ( char ) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_V( Array.ISort.Primitives.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[ assigned ];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = index -> (short) values[ index ] : getV;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != null ) dst.dst[ k++ ] = ( char ) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		
		public String toString() { return toJSON(); }
		
		@Override public void toJSON( JsonWriter json ) {
			
			
			if( 0 < assigned ) {
				json.preallocate( assigned * 10 );
				int token = NonNullKeysIterator.INIT;
				
				if( K_is_string ) {
					json.enterObject();
					
					if( hasNullKey ) json.name().value( NullKeyValue );
					
					while( ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT )
						json.
								name( NonNullKeysIterator.key( this, token ).toString() ).
								value( NonNullKeysIterator.value( this, token ) );
					json.exitObject();
				} else {
					json.enterArray();
					if( hasNullKey )
						json.
								enterObject()
								.name( "Key" ).value( NonNullKeysIterator.key( this, token ) )
								.name( "Value" ).value( NonNullKeysIterator.value( this, token ) ).
								exitObject();
					
					while( ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT )
						json.
								enterObject()
								.name( "Key" ).value( NonNullKeysIterator.key( this, token ) )
								.name( "Value" ).value( NonNullKeysIterator.value( this, token ) ).
								exitObject();
					json.exitArray();
				}
			} else {
				json.enterObject();
				if( hasNullKey ) json.name().value( NullKeyValue );
				json.exitObject();
			}
			
		}
	}
	
	interface Interface< K > {
		int size();
		
		boolean contains( K key );
		
		short value( @Positive_ONLY int token );
		
		@Positive_Values int token( K key );
		
		boolean put( K key, short value );
	}
	
	class RW< K > extends R< K > implements Interface< K > {
		
		public RW( Class< K > clazz, int expectedItems )                    { this( clazz, expectedItems, 0.75 ); }
		
		public RW( Array.Of< K > ofK, int expectedItems )                   { this( ofK, expectedItems, 0.75 ); }
		
		public RW( Class< K > clazz, int expectedItems, double loadFactor ) { this( Array.get( clazz ), expectedItems, loadFactor ); }
		
		public RW( Array.Of< K > ofK, int expectedItems, double loadFactor ) {
			super( ofK );
			
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			long length = ( long ) Math.ceil( expectedItems / loadFactor );
			
			int size = ( int ) ( length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ) );
			
			resizeAt = Math.min( mask = size - 1, ( int ) Math.ceil( size * loadFactor ) );
			
			keys   = this.ofK.copyOf( null, size );
			values = new short[ size ];
		}
		
		public boolean put( K key, short value ) {
			
			if( key == null ) {
				NullKeyValue = value;
				return !hasNullKey && ( hasNullKey = true );
			}
			
			int slot = ofK.hashCode( key ) & mask;
			
			
			for( K k; ( k = keys[ slot ] ) != null; slot = slot + 1 & mask )
				if( ofK.equals( k, key ) ) {
					values[ slot ] = ( short ) value;
					return false;
				}
			
			keys[ slot ]   = key;
			values[ slot ] = ( short ) value;
			
			if( assigned++ == resizeAt ) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean remove( K key ) {
			if( key == null ) return hasNullKey && !( hasNullKey = false );
			
			int slot = ofK.hashCode( key ) & mask;
			
			for( K k; ( k = keys[ slot ] ) != null; slot = slot + 1 & mask )
				if( ofK.equals( k, key ) ) {
					short[] vals = values;
					
					
					int gapSlot = slot;
					
					K kk;
					for( int distance = 0, s; ( kk = keys[ s = gapSlot + ++distance & mask ] ) != null; )
						if( ( s - ofK.hashCode( kk ) & mask ) >= distance ) {
							vals[ gapSlot ] = vals[ s ];
							keys[ gapSlot ] = kk;
							                  gapSlot = s;
							                  distance = 0;
						}
					
					keys[ gapSlot ] = null;
					vals[ gapSlot ] = ( short ) 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			for( int i = keys.length - 1; i >= 0; i-- ) keys[ i ] = null;
		}
		
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, ( int ) Math.ceil( size * loadFactor ) );
			
			if( assigned < 1 ) {
				if( keys.length < size ) keys = ofK.copyOf( null, size );
				if( values.length < size ) values = new short[ size ];
				return;
			}
			
			final K[]           k = keys;
			final short[] v = values;
			
			keys   = ofK.copyOf( null, size );
			values = new short[ size ];
			
			K key;
			for( int i = k.length; -1 < --i; )
				if( ( key = k[ i ] ) != null ) {
					int slot = ofK.hashCode( key ) & mask;
					while( !( keys[ slot ] == null ) ) slot = slot + 1 & mask;
					
					keys[ slot ]   = key;
					values[ slot ] = v[ i ];
				}
		}
		
		public RW< K > clone() { return ( RW< K > ) super.clone(); }
		private static final Object OBJECT = new Array.Of<>( RW.class );
	}
	@SuppressWarnings( "unchecked" )
	static < K > Array.Of< RW< K > > of() { return ( Array.Of< RW< K > > ) RW.OBJECT; }
}
	