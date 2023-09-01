package xyz.unirail.collections;


import xyz.unirail.JsonWriter;


public interface ObjectObjectMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static < K, V > int token( R< K, V > src, int token ) {
			for( ; ; )
				if( ++token == src.keys.length ) return INIT;
				else if( src.keys[ token ] != null ) return token;
		}
		
		static < K, V > K key( R< K, V > src, int token )   { return src.keys[ token ]; }
		
		static < K, V > V value( R< K, V > src, int token ) { return src.values[ token ]; }
	}
	
	abstract class R< K, V > implements Cloneable, JsonWriter.Source {
		
		public          K[]           keys;
		public          V[]           values;
		protected final Array.Of< K > ofK;
		protected final Array.Of< V > ofV;
		private final   boolean       K_is_string;
		
		protected R( Class< K > clazzK, Class< V > clazzV ) {
			
			ofK         = Array.get( clazzK );
			ofV         = Array.get( clazzV );
			K_is_string = clazzK == String.class;
		}
		
		public R( Array.Of< K > ofK, Array.Of< V > ofV ) {
			this.ofK    = ofK;
			this.ofV    = ofV;
			K_is_string = false;
		}
		
		protected int assigned;
		
		protected int mask;
		
		
		protected int resizeAt;
		
		
		protected boolean hasNullKey;
		
		V NullKeyValue = null;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token( K key ) {
			if( key == null ) return hasNullKey ? keys.length : Positive_Values.NONE;
			
			int slot = ofK.hashCode( key ) & mask;
			
			for( K k; ( k = keys[ slot ] ) != null; slot = slot + 1 & mask )
				if( k.equals( key ) ) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains( K key )           { return !hasNone( token( key ) ); }
		
		public boolean hasNone( int token )        { return token == Positive_Values.NONE; }
		
		public V value( @Positive_ONLY int token ) { return token == keys.length ? NullKeyValue : values[ token ]; }
		
		
		public int size()                          { return assigned + ( hasNullKey ? 1 : 0 ); }
		
		
		public boolean isEmpty()                   { return size() == 0; }
		
		//Compute a hash that is symmetric in its arguments - that is a hash
		//where the order of appearance of elements does not matter.
		//This is useful for hashing sets, for example.
		public int hashCode() {
			int a = 0;
			int b = 0;
			int c = 1;
			
			for( int token = NonNullKeysIterator.INIT; ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT; ) {
				int h = Array.mix( seed, ofK.hashCode( NonNullKeysIterator.key( this, token ) ) );
				h = Array.mix( h, NonNullKeysIterator.value( this, token ) == null ? seed : ofV.hashCode( NonNullKeysIterator.value( this, token ) ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, NullKeyValue == null ? seed : ofV.hashCode( NullKeyValue ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) ); }
		
		public boolean equals( R< K, V > other ) {
			if( other == null || size() != other.size() || hasNullKey != other.hasNullKey ||
					( hasNullKey && NullKeyValue != other.NullKeyValue &&
							( NullKeyValue == null || other.NullKeyValue == null || !ofV.equals( NullKeyValue, other.NullKeyValue ) ) ) )
				return false;
			
			
			K key;
			for( int i = keys.length, token = 0; -1 < --i; )
				if( ( key = keys[ i ] ) != null &&
						( ( token = other.token( key ) ) == -1 || !ofV.equals( values[ i ], other.value( token ) ) ) )
					return false;
			
			return true;
		}
		
		
		@SuppressWarnings( "unchecked" )
		public R< K, V > clone() {
			
			try {
				R< K, V > dst = ( R< K, V > ) super.clone();
				
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		public Array.ISort.Objects< K > getK = null;
		public Array.ISort.Objects< V > getV = null;
		
		
		public void build_K( Array.ISort.Anything.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[ assigned ];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects< K >() {
				@Override K get( int index ) { return keys[ index ]; }
			} : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[ i ] != null ) dst.dst[ k++ ] = ( char ) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build_V( Array.ISort.Anything.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[ assigned ];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = new Array.ISort.Objects< V >() {
				@Override V get( int index ) { return values[ index ]; }
			} : getV;
			
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
		
		public void build_V( Array.ISort.Anything.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[ assigned ];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = new Array.ISort.Objects< V >() {
				@Override V get( int index ) { return values[ index ]; }
			} : getV;
			
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
								.name( "Key" ).value()
								.name( "Value" ).value( NullKeyValue ).
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
	
	interface Interface< K, V > {
		int size();
		
		boolean contains( K key );
		
		boolean hasNone( int token );
		
		V value( @Positive_ONLY int token );
		
		@Positive_Values int token( K key );
		
		boolean put( K key, V value );
	}
	
	class RW< K, V > extends R< K, V > implements Interface< K, V > {
		
		
		public RW( Class< K > clazzK, Class< V > clazzV, int expectedItems )                    { this( clazzK, clazzV, expectedItems, 0.75 ); }
		
		public RW( Class< K > clazzK, Array.Of< V > ofV, int expectedItems )                    { this( Array.get( clazzK ), ofV, expectedItems, 0.75 ); }
		
		public RW( Array.Of< K > ofK, Class< V > clazzV, int expectedItems )                    { this( ofK, Array.get( clazzV ), expectedItems, 0.75 ); }
		
		public RW( Array.Of< K > ofK, Array.Of< V > ofV, int expectedItems )                    { this( ofK, ofV, expectedItems, 0.75 ); }
		
		
		public RW( Class< K > clazzK, Class< V > clazzV, int expectedItems, double loadFactor ) { this( Array.get( clazzK ), Array.get( clazzV ), expectedItems, loadFactor ); }
		
		public RW( Class< K > clazzK, Array.Of< V > ofV, int expectedItems, double loadFactor ) { this( Array.get( clazzK ), ofV, expectedItems, loadFactor ); }
		
		public RW( Array.Of< K > ofK, Class< V > clazzV, int expectedItems, double loadFactor ) { this( ofK, Array.get( clazzV ), expectedItems, loadFactor ); }
		
		public RW( Array.Of< K > ofK, Array.Of< V > ofV, int expectedItems, double loadFactor ) {
			super( ofK, ofV );
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			long length = ( long ) Math.ceil( expectedItems / this.loadFactor );
			int  size   = ( int ) ( length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ) );
			
			resizeAt = Math.min( mask = size - 1, ( int ) Math.ceil( size * loadFactor ) );
			
			keys   = ofK.copyOf( null, size );
			values = ofV.copyOf( null, size );
		}
		
		
		public boolean put( K key, V value ) {
			
			if( key == null ) {
				boolean h = hasNullKey;
				hasNullKey   = true;
				NullKeyValue = value;
				return h != hasNullKey;
			}
			
			
			int slot = ofK.hashCode( key ) & mask;
			
			
			for( K k; ( k = keys[ slot ] ) != null; slot = slot + 1 & mask )
				if( ofK.equals( k, key ) ) {
					values[ slot ] = value;
					return true;
				}
			
			keys[ slot ]   = key;
			values[ slot ] = value;
			
			if( assigned++ == resizeAt ) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			
			for( int i = keys.length - 1; i >= 0; i--, keys[ i ] = null, values[ i ] = null ) ;
		}
		
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, ( int ) Math.ceil( size * loadFactor ) );
			
			if( assigned < 1 ) {
				if( keys.length < size ) keys = ofK.copyOf( null, size );
				if( values.length < size ) values = ofV.copyOf( null, size );
				return;
			}
			
			final K[] ks = this.keys;
			final V[] vs = this.values;
			
			keys   = ofK.copyOf( null, size );
			values = ofV.copyOf( null, size );
			
			K k;
			for( int i = ks.length; -1 < --i; )
				if( ( k = ks[ i ] ) != null ) {
					int slot = ofK.hashCode( k ) & mask;
					while( !( keys[ slot ] == null ) ) slot = slot + 1 & mask;
					
					keys[ slot ]   = k;
					values[ slot ] = vs[ i ];
				}
		}
		
		public boolean remove( K key ) {
			if( key == null ) {
				boolean h = hasNullKey;
				hasNullKey = false;
				return h != hasNullKey;
			}
			
			int slot = ofK.hashCode( key ) & mask;
			
			for( K k; ( k = keys[ slot ] ) != null; slot = slot + 1 & mask )
				if( ofK.equals( k, key ) ) {
					final V v       = values[ slot ];
					int     gapSlot = slot;
					
					K kk;
					for( int distance = 0, slot1; ( kk = keys[ slot1 = gapSlot + ++distance & mask ] ) != null; )
						if( ( slot1 - ofK.hashCode( kk ) & mask ) >= distance ) {
							values[ gapSlot ] = values[ slot1 ];
							keys[ gapSlot ] = kk;
							                  gapSlot = slot1;
							                  distance = 0;
						}
					
					keys[ gapSlot ]   = null;
					values[ gapSlot ] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW< K, V > clone() { return ( RW< K, V > ) super.clone(); }
		
		private static final Object OBJECT = new Array.Of<>( RW.class );
	}
	
	@SuppressWarnings( "unchecked" )
	static < K, V > Array.Of< RW< K, V > > of() { return ( Array.Of< RW< K, V > > ) RW.OBJECT; }
}
	