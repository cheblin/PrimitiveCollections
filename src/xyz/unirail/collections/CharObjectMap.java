package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

public interface CharObjectMap {
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static < V > int token( R< V > src, int token ) {
			for( int len = src.keys.length; ; )
				if( ++token == len ) return src.has0Key ? len : INIT;
				else if( token == len + 1 ) return INIT;
				else if( src.keys[ token ] != 0 ) return token;
		}
		
		static < V > char key( R< V > src, int token ) { return token == src.keys.length ? 0 : (char) src.keys[ token ]; }
		
		static < V > V value( R< V > src, int token ) { return token == src.keys.length ? src.OKeyValue : src.values[ token ]; }
	}
	
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		
		public          char[] keys;
		public          V[]           values;
		protected final Array.Of< V > ofV;
		
		protected R( Class< V > clazz ) { ofV = Array.get( clazz ); }
		
		public R( Array.Of< V > ofV )   { this.ofV = ofV; }
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean hasNullKey;
		V       NullKeyValue;
		
		
		boolean has0Key;
		V       OKeyValue;
		
		
		protected double loadFactor;
		
		public boolean contains(  Character key )           { return !hasNone( token( key ) ); }
		
		public boolean contains( char key )               { return !hasNone( token( key ) ); }
		
		public boolean hasNone( int token )                      { return token == Positive_Values.NONE; }
		
		
		public @Positive_Values int token(  Character key ) { return key == null ? hasNullKey ? keys.length + 1 : Positive_Values.NONE : token( ( char ) ( key + 0 ) ); }
		
		public @Positive_Values int token( char key ) {
			if( key == 0 ) return has0Key ? keys.length : Positive_Values.NONE;
			
			int slot = Array.hash( key ) & mask;
			
			for( char k; ( k = keys[ slot ] ) != 0; slot = slot + 1 & mask )
				if( k == key ) return slot;
			
			return Positive_Values.NONE;
		}
		
		public V value( @Positive_ONLY int token ) {
			if( token == keys.length + 1 ) return NullKeyValue;
			if( token == keys.length ) return OKeyValue;
			
			return values[ token ];
		}
		
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + ( has0Key ? 1 : 0 ) + ( hasNullKey ? 1 : 0 ); }
		
		
		public int hashCoder() {
			int hash = hasNullKey ? Array.hash( NullKeyValue ) : 997651;
			
			for( int token = NonNullKeysIterator.INIT, h = 0xc2b2ae35; ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT; )
			     hash = ( h++ ) + Array.hash( Array.hash( hash, NonNullKeysIterator.key( this, token ) ), ofV.hashCode( NonNullKeysIterator.value( this, token ) ) );
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
				h = Array.mix( h, NonNullKeysIterator.value( this, token ) == null ? seed : Array.hash( NonNullKeysIterator.value( this, token ) ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, NullKeyValue == null ? seed : Array.hash( Array.hash( NullKeyValue ) ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		
		@SuppressWarnings( "unchecked" ) public boolean equals( Object obj ) {
			
			return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R< V > other ) {
			
			if( other == null || assigned != other.assigned ||
					has0Key != other.has0Key || hasNullKey != other.hasNullKey ||
					has0Key && !ofV.equals( OKeyValue, other.OKeyValue ) ||
					hasNullKey && NullKeyValue != other.NullKeyValue && ( NullKeyValue == null || other.NullKeyValue == null || !ofV.equals( NullKeyValue, other.NullKeyValue ) ) )
				return false;
			
			V           v;
			char key;
			
			for( int i = keys.length, c; -1 < --i; )
				if( ( key = keys[ i ] ) != 0 ) {
					if( ( c = other.token( key ) ) < 0 ) return false;
					v = other.value( c );
					
					if( values[ i ] != v )
						if( v == null || values[ i ] == null || !ofV.equals( v, values[ i ] ) ) return false;
				}
			return true;
		}
		
		
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		
		private Array.ISort.Primitives   getK = null;
		private Array.ISort.Objects< V > getV = null;
		
		public void build_K( Array.ISort.Primitives.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[ assigned ];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = index -> (char) keys[ index ] : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != 0 ) dst.dst[ k++ ] = ( char ) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_V( Array.ISort.Anything.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[ assigned ];
			dst.size = assigned;
			dst.src  = getV == null ? getV = new Array.ISort.Objects< V >() {
				@Override V get( int index ) { return values[ index ]; }
			} : getV;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != 0 ) dst.dst[ k++ ] = ( char ) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_K( Array.ISort.Primitives.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[ assigned ];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = index -> (char) keys[ index ] : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != 0 ) dst.dst[ k++ ] = i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		
		public void build_V( Array.ISort.Anything.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[ assigned ];
			dst.size = assigned;
			dst.src  = getV == null ? getV = new Array.ISort.Objects< V >() {
				@Override V get( int index ) { return values[ index ]; }
			} : getV;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != 0 ) dst.dst[ k++ ] = ( char ) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		
		public String toString() { return toJSON(); }
		
		@Override public void toJSON( JsonWriter json ) {
			json.enterObject();
			
			if( hasNullKey ) json.name().value( NullKeyValue );
			
			int size = size(), token = NonNullKeysIterator.INIT;
			if( 0 < size ) {
				json.preallocate( size * 10 );
				
				while( ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT )
					json.
							name( NonNullKeysIterator.key( this, token ) ).
							value( NonNullKeysIterator.value( this, token ) );
			}
			
			json.exitObject();
		}
	}
	
	interface Interface< V > {
		int size();
		
		boolean contains(  Character key );
		
		boolean contains( char key );
		
		boolean hasNone( int token );
		
		@Positive_Values int token(  Character key );
		
		@Positive_Values int token( char key );
		
		V value( @Positive_ONLY int token );
		
		boolean put(  Character key, V value );
		
		boolean put( char key, V value );
	}
	
	class RW< V > extends R< V > implements Interface< V > {
		
		public RW( Class< V > clazz, int expectedItems )                    { this( clazz, expectedItems, 0.75 ); }
		
		
		public RW( Class< V > clazz, int expectedItems, double loadFactor ) { this( Array.get( clazz ), expectedItems, loadFactor ); }
		
		public RW( Array.Of< V > ofV, int expectedItems, double loadFactor ) {
			super( ofV );
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			final long length = ( long ) Math.ceil( expectedItems / loadFactor );
			int        size   = ( int ) ( length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ) );
			
			resizeAt = Math.min( mask = size - 1, ( int ) Math.ceil( size * loadFactor ) );
			
			keys   = new char[ size ];
			values = ofV.copyOf( null, size );
		}
		
		
		public boolean put(  Character key, V value ) {
			if( key != null ) return put( ( char ) key, value );
			
			hasNullKey   = true;
			NullKeyValue = value;
			
			return true;
		}
		
		public boolean put( char key, V value ) {
			if( key == 0 ) {
				has0Key   = true;
				OKeyValue = value;
				return true;
			}
			
			
			int slot = Array.hash( key ) & mask;
			
			for( char k; ( k = keys[ slot ] ) != 0; slot = slot + 1 & mask )
				if( k == key ) {
					values[ slot ] = value;
					return true;
				}
			
			keys[ slot ]   =  key;
			values[ slot ] =            value;
			
			if( ++assigned == resizeAt ) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean remove(  Character key ) {
			if( key == null ) return hasNullKey && !( hasNullKey = false );
			
			return remove( ( char ) key );
		}
		
		public boolean remove( char key ) {
			
			if( key == 0 ) return has0Key && !( has0Key = false );
			
			int slot = Array.hash( key ) & mask;
			
			for( char k; ( k = keys[ slot ] ) != 0; slot = slot + 1 & mask )
				if( k == key ) {
					final V v       = values[ slot ];
					int     gapSlot = slot;
					
					char kk;
					for( int distance = 0, s; ( kk = keys[ s = gapSlot + ++distance & mask ] ) != 0; )
						if( ( s - Array.hash( kk ) & mask ) >= distance ) {
							
							keys[ gapSlot ]   =  kk;
							values[ gapSlot ] = values[ s ];
							                               gapSlot = s;
							                               distance = 0;
						}
					
					keys[ gapSlot ]   = 0;
					values[ gapSlot ] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			
			has0Key   = false;
			OKeyValue = null;
			
			hasNullKey   = false;
			NullKeyValue = null;
			
			for( int i = keys.length - 1; i >= 0; i-- ) values[ i ] = null;
		}
		
		
		void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, ( int ) Math.ceil( size * loadFactor ) );
			
			if( assigned < 1 ) {
				if( keys.length < size ) {
					keys   = new char[ size ];
					values = ofV.copyOf( null, size );
				}
				return;
			}
			
			
			final char[] k = keys;
			final V[]           v = values;
			
			keys   = new char[ size ];
			values = ofV.copyOf( null, size );
			
			char key;
			for( int i = k.length; -1 < --i; )
				if( ( key = k[ i ] ) != 0 ) {
					int slot = Array.hash( key ) & mask;
					while( keys[ slot ] != 0 ) slot = slot + 1 & mask;
					keys[ slot ]   =  key;
					values[ slot ] = v[ i ];
				}
		}
		
		
		public RW< V > clone() { return ( RW< V > ) super.clone(); }
		
		private static final Object OBJECT = new Array.Of<>( RW.class );
	}
	
	@SuppressWarnings( "unchecked" )
	static < V > Array.Of< RW< V > > of() { return ( Array.Of< RW< V > > ) RW.OBJECT; }
}
