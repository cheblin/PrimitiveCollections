package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;


/**
 * A specialized map for mapping char/short keys (65,536 distinct values) to object values of type V.
 * Implements a HYBRID strategy:
 * 1. Starts as a hash map with separate chaining, optimized for sparse data.
 * Uses short[] for next pointers, limiting this phase's capacity.
 * 2. Automatically transitions to a direct-mapped flat array when the hash map
 * reaches its capacity limit (~32,749 entries) and needs to grow further.
 * This approach balances memory efficiency for sparse maps with guaranteed O(1)
 * performance and full key range support for dense maps.
 */
public interface ShortObjectMap {
	
	/**
	 * Abstract base class providing read-only operations for the map.
	 * Handles the underlying structure which can be either a hash map or a flat array.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		protected boolean        hasNullKey;          // Indicates if the map contains a null key
		protected V              nullKeyValue;        // Value for the null key, stored separately.
		protected char[]         _buckets;            // Hash table buckets array (1-based indices to chain heads).
		protected short[]        nexts;               // Links within collision chains (-1 termination, -2 unused, <-2 free list link).
		protected short[] keys = Array.EqualHashOf.shorts     .O; // Keys array.
		protected V[]            values;              // Values array.
		protected int            _count;              // Hash mode: Total slots used (entries + free slots). Flat mode: Number of set bits (actual entries).
		protected int            _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int            _freeCount;          // Number of free entries in the free list.
		protected int            _version;            // Version counter for concurrent modification detection.
		
		/**
		 * Strategy for comparing values and calculating their hash codes.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		protected static final int  StartOfFreeList = -3; // Marks the start of the free list in 'nexts' field.
		protected static final long INDEX_MASK      = 0xFFFF_FFFFL; // Mask for index in token.
		protected static final int  VERSION_SHIFT   = 32; // Bits to shift version in token.
		// Special index used in tokens to represent the null key. Outside valid array index ranges.
		protected static final int  NULL_KEY_INDEX  = 0x1_FFFF; // 65537
		
		protected static final long INVALID_TOKEN = -1L; // Invalid token constant.
		
		
		/**
		 * Flag indicating if the map is operating in flat array mode.
		 */
		protected boolean isFlatStrategy = false;
		
		/**
		 * Bitset to track presence of keys in flat mode. Size 1024 longs = 65536 bits.
		 */
		protected long[] flat_bits; // Size: 65536 / 64 = 1024
		
		// Constants for Flat Mode
		protected static final int FLAT_ARRAY_SIZE  = 0x10000;
		protected static final int FLAT_BITSET_SIZE = FLAT_ARRAY_SIZE / 64; // 1024
		
		
		/**
		 * Constructs a new read-only map base with the specified value equality/hash strategy.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating hash codes.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return True if the map contains no key-value mappings.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value mappings in the map.
		 * Calculation depends on the current mode (hash map or flat array).
		 *
		 * @return The total number of mappings, including the conceptual null key if present.
		 */
		public int size() {
			// Hash Mode: _count includes free slots, subtract _freeCount for actual entries.
			// Flat Mode: _count is the number of set bits (actual entries).
			// Add 1 if the null key is present in either mode.
			return (
					       isFlatStrategy ?
							       _count :
							       _count - _freeCount ) + (
					       hasNullKey ?
							       1 :
							       0 );
		}
		
		
		public int count() { return size(); }
		
		/**
		 * Returns the allocated capacity of the internal structure.
		 * In hash map mode, it's the length of the internal arrays.
		 * In flat mode, it's the fixed size (65536).
		 *
		 * @return The capacity.
		 */
		public int length() {
			return isFlatStrategy ?
					FLAT_ARRAY_SIZE :
					( nexts == null ?
							0 :
							nexts.length );
		}
		
		/**
		 * Checks if the map contains a mapping for the specified key (boxed Character).
		 *
		 * @param key The key to check.
		 * @return True if the key exists in the map.
		 */
		public boolean contains(  Short     key ) {
			return key == null ?
					hasNullKey :
					contains( ( short ) ( key + 0 ) );
		}
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive char key (0 to 65535).
		 * @return True if the key exists in the map.
		 */
		public boolean contains( short key ) {
			return isFlatStrategy ?
					exists( ( char ) key ) :
					tokenOf( key ) != INVALID_TOKEN;
		}
		
		/**
		 * Checks if the map contains the specified value.
		 * This operation can be slow (O(N)).
		 *
		 * @param value The value to search for. Can be null.
		 * @return True if the value exists in the map.
		 */
		public boolean containsValue( Object value ) {
			if( hasNullKey && Objects.equals( nullKeyValue, value ) ) return true;
			if( size() == 0 ) return false;
			if( isFlatStrategy ) {
				for( int i = -1; ( i = next1( i + 1 ) ) != -1; ) {
					if( Objects.equals( values[ i ], value ) ) return true;
				}
			}
			else if( nexts != null ) {
				for( int i = 0; i < _count; i++ ) // Iterate up to _count, not nexts.length
					if( -2 < nexts[ i ] && Objects.equals( values[ i ], value ) ) return true;
			}
			
			return false;
		}
		
		/**
		 * Returns a token for the specified key (boxed Character).
		 *
		 * @param key The key (can be null).
		 * @return A token representing the key's location if found, or INVALID_TOKEN if not found.
		 */
		public long tokenOf(  Short     key ) {
			return key == null ?
					( hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN ) :
					tokenOf( ( short ) ( key + 0 ) );
		}
		
		/**
		 * Returns a token for the specified primitive key.
		 *
		 * @param key The primitive char key.
		 * @return A token representing the key's location if found, or INVALID_TOKEN if not found.
		 */
		public long tokenOf( short key ) {
			if( isFlatStrategy )
				return exists( ( char ) key ) ?
						token( ( char ) key ) :
						INVALID_TOKEN;
			
			if( _buckets == null || size() == 0 ) return INVALID_TOKEN; // Check size() to account for only null key present
			
			int hash = Array.hash( key );
			int i    = ( _buckets[ bucketIndex( hash ) ] ) - 1;
			
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key ) return token( i );
				i = nexts[ i ];
				if( nexts.length < ++collisionCount ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		
		/**
		 * Returns the initial token for iteration. Starts with the first non-null key.
		 * If only the null key exists, returns the null key token.
		 *
		 * @return The first valid token to begin iteration, or INVALID_TOKEN if the map is empty.
		 */
		public long token() {
			int index = unsafe_token( -1 );
			
			return index == -1 ?
					hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN :
					token( index );
		}
		
		/**
		 * Returns the next token in iteration.
		 *
		 * @param token The current token.
		 * @return The next valid token for iteration, or -1 (INVALID_TOKEN) if there are no more entries or the token is invalid due to structural modification.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN || version( token ) != _version ) return INVALID_TOKEN;
			
			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN;
			index = unsafe_token( index );
			
			return index == -1 ?
					hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN :
					token( index );
		}
		
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #nullKeyValue()} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token (index), or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #nullKeyValue() To get the null key’s value.
		 */
		public int unsafe_token( final int token ) {
			if( isFlatStrategy )
				return next1( index( token ) + 1 );
			else
				for( int i = token + 1; i < _count; i++ )
					if( -2 < nexts[ i ] ) return i;
			
			return -1; // No more entries
		}
		
		/**
		 * Checks if the map contains the conceptual null key.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the conceptual null key. Returns null if null key doesn't exist.
		 */
		public V nullKeyValue() {
			return hasNullKey ?
					nullKeyValue :
					null;
		}
		
		/**
		 * Checks if the token corresponds to a non-null key.
		 */
		public boolean hasKey( long token ) { return index( token ) != NULL_KEY_INDEX; }
		
		/**
		 * Retrieves the key associated with a token.
		 * Throws if the token represents the null key or is invalid.
		 *
		 * @param token The token representing a non-null key-value pair.
		 * @return The char key associated with the token.
		 * @throws ArrayIndexOutOfBoundsException if the token is for the null key or invalid/out of bounds.
		 */
		public short key( long token ) {
			return ( short ) (short) (
					isFlatStrategy ?
							index( token ) :
							keys[ index( token ) ] );
		}
		
		
		/**
		 * Retrieves the value associated with a token.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The value associated with the token, or {@code null} if the token is invalid or represents the null key which holds a null value.
		 * @throws IndexOutOfBoundsException if the token index is invalid for the current mode/state.
		 */
		public V value( long token ) {
			return
					index( token ) == NULL_KEY_INDEX ?
							nullKeyValue :
							values[ index( token ) ];
		}
		
		/**
		 * Retrieves the value associated with the specified key (boxed Character).
		 *
		 * @param key The key (can be null).
		 * @return The associated value, or null if the key is not found.
		 */
		public V get(  Short     key ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
					null :
					value( token );
		}
		
		/**
		 * Retrieves the value associated with the specified primitive key.
		 *
		 * @param key The primitive char key.
		 * @return The associated value, or null if the key is not found.
		 */
		public V get( short key ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
					null :
					value( token );
		}
		
		/**
		 * Retrieves the value associated with the specified key (boxed Character), or returns defaultValue if the key is not found.
		 *
		 * @param key          The key (can be null).
		 * @param defaultValue The value to return if the key is not found.
		 * @return The associated value, or defaultValue if the key is not found.
		 */
		public V getOrDefault(  Short     key, V defaultValue ) {
			long token = tokenOf( key );
			// Must also check version in case entry was removed and re-added at same index
			return ( token == INVALID_TOKEN || version( token ) != _version ) ?
					defaultValue :
					value( token );
		}
		
		/**
		 * Retrieves the value associated with the specified primitive key, or returns defaultValue if the key is not found.
		 *
		 * @param key          The primitive char key.
		 * @param defaultValue The value to return if the key is not found.
		 * @return The associated value, or defaultValue if the key is not found.
		 */
		public V getOrDefault( short key, V defaultValue ) {
			long token = tokenOf( key );
			// Must also check version in case entry was removed and re-added at same index
			return ( token == INVALID_TOKEN || version( token ) != _version ) ?
					defaultValue :
					value( token );
		}
		
		
		/**
		 * Computes an order-independent hash code.
		 *
		 * @return The hash code of the map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			// Iterate using safe tokens to handle potential modifications during iteration by other threads (though ideally avoid)
			for( long token = token(); token != INVALID_TOKEN; token = token( token ) ) {
				if( index( token ) == NULL_KEY_INDEX ) continue; // Skip null key here, handle below
				
				int keyHash = Array.hash( key( token ) ); // Get key using safe method
				V   val     = value( token ); // Get value using safe method
				int valueHash = Array.hash( val == null ?
						                            seed :
						                            equal_hash_V.hashCode( val ) );
				
				int h = Array.mix( seed, keyHash );
				h = Array.mix( h, valueHash );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				int h = Array.hash( seed ); // Hash for null key representation
				h = Array.mix( h, Array.hash( nullKeyValue == null ?
						                              seed :
						                              equal_hash_V.hashCode( nullKeyValue ) ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj ); }
		
		/**
		 * Compares this map with another R instance for equality.
		 * Requires that the other map has the same value type V.
		 *
		 * @param other The other map to compare.
		 * @return True if the maps are equal.
		 */
		public boolean equals( R< V > other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    ( hasNullKey && Objects.equals( nullKeyValue, other.nullKeyValue ) ) || size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || !Objects.equals( value( token ), other.value( t ) ) ) return false;
			}
			return true;
		}
		
		
		@Override
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				// Ensure equal_hash_V is copied (it's final, so reference is copied, which is ok)
				if( isFlatStrategy ) {
					dst.flat_bits = flat_bits.clone();
					dst.values    = values.clone();
				}
				else if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.nexts    = nexts.clone();
					dst.keys     = keys.clone();
					dst.values   = values.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		public String toString() { return toJSON(); }
		
		
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 15 ); // Guestimate size increase for object values
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			
			if( isFlatStrategy )
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( token ).value( values[ token ] );
			else
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( keys[ token ] ).value( values[ token ] );
			
			
			json.exitObject();
		}
		
		// --- Helper methods ---
		
		/**
		 * Calculates bucket index in hash map mode. Ensures non-negative index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token combining version and index.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index & INDEX_MASK ); }
		
		/**
		 * Extracts index from a token.
		 */
		protected int index( long token ) { return ( int ) ( token & INDEX_MASK ); }
		
		/**
		 * Extracts version from a token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
		/**
		 * Checks if a key is present in flat mode using the bitset. Assumes flat_bits is not null.
		 */
		protected final boolean exists( char key ) { return ( flat_bits[ key >>> 6 ] & 1L << key ) != 0; }
		
		
		/**
		 * Finds the next set bit >= 'bit' in the bitset
		 */
		public int next1( int bit ) { return next1( bit, flat_bits ); }
		
		/**
		 * Static helper to find the next set bit >= 'bit' in a given bitset
		 */
		public static int next1( int bit, long[] bits ) {
			
			int index = bit >>> 6; // Index in bits array (word index)
			if( bits.length <= index ) return -1;
			
			int pos = bit & 63;   // Bit position within the long (0-63)
			
			// Mask to consider only bits from pos onward in the first long
			long mask  = -1L << pos; // 1s from pos to end
			long value = bits[ index ] & mask; // Check for '1's from pos
			
			// Check the first long
			if( value != 0 ) return ( index << 6 ) + Long.numberOfTrailingZeros( value );
			
			// Search subsequent longs
			for( int i = index + 1; i < 1024; i++ ) {
				value = bits[ i ];
				if( value != 0 ) return ( i << 6 ) + Long.numberOfTrailingZeros( value );
			}
			
			// No '1' found, return -1
			return -1;
		}
		
	}
	
	/**
	 * Read-write implementation of the map, extending read-only functionality.
	 */
	class RW< V > extends R< V > {
		// The threshold capacity determining the switch to flat strategy.
		// Set to the max capacity of the hash phase (due to short[] nexts).
		protected static final int flatStrategyThreshold = 0x7FFF; // ~32k
		
		/**
		 * Constructs an empty map with default initial capacity for hash map mode,
		 * using the default equality/hash strategy for V.
		 *
		 * @param clazzV Class object for value type V, used for array creation.
		 */
		public RW( Class< V > clazzV ) { this( Array.get( clazzV ), 0 ); }
		
		/**
		 * Constructs an empty map with specified initial capacity.
		 * If capacity exceeds the threshold, starts in flat mode.
		 * Uses the default equality/hash strategy for V.
		 *
		 * @param clazzV   Class object for value type V, used for array creation.
		 * @param capacity The initial capacity hint.
		 */
		public RW( Class< V > clazzV, int capacity ) { this( Array.get( clazzV ), capacity ); }
		
		/**
		 * Constructs an empty map with default initial capacity for hash map mode,
		 * using the specified equality/hash strategy for V.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating hash codes.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V ) { this( equal_hash_V, 0 ); }
		
		/**
		 * Constructs an empty map with specified initial capacity.
		 * If capacity exceeds the threshold, starts in flat mode.
		 * Uses the specified equality/hash strategy for V.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating hash codes.
		 * @param capacity     The initial capacity hint.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int capacity ) {
			super( equal_hash_V );
			if( capacity > 0 ) initialize( capacity );
		}
		
		
		/**
		 * Initializes the internal arrays with a specified capacity.
		 * This is called internally when needed.
		 *
		 * @param capacity The desired capacity (will be adjusted to a prime if in hash mode).
		 * @return The initialized capacity.
		 */
		private int initialize( int capacity ) {
			_version++; // Increment version on initialization
			
			// Determine initial mode based on capacity hint
			if( capacity > flatStrategyThreshold ) {
				isFlatStrategy = true;
				flat_bits      = new long[ FLAT_BITSET_SIZE ]; // 1024 longs
				values         = equal_hash_V.copyOf( null, FLAT_ARRAY_SIZE ); // Use strategy for V[] creation
				_count         = 0; // Flat mode _count tracks actual entries
				return FLAT_ARRAY_SIZE;
			}
			_buckets  = new char[ capacity ];
			nexts     = new short[ capacity ];
			keys      = new short[ capacity ];
			_freeList = -1;
			_count    = 0;
			values    = equal_hash_V.copyOf( null, capacity ); // Use strategy for V[] creation
			_freeList = -1;
			return length();
		}
		
		
		/**
		 * Associates a value with a key (boxed Character). Replaces existing value.
		 *
		 * @param key   The key (can be null).
		 * @param value The value (can be null).
		 * @return The previous value associated with the key, or null if none.
		 */
		public boolean put(  Short     key, V value ) {
			return key == null ?
					put( value ) :
					put( key, value );
		}
		
		
		/**
		 * Associates the specified value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the behavior is determined by the {@code onExists} predicate.
		 *
		 * @param key   The primitive char key with which the specified value is to be associated.
		 * @param value The value (type V, can be null) to be associated with the specified key.
		 * @throws ConcurrentModificationException if excessive collisions are detected in hash mode.
		 */
		public boolean put( short key, V value ) {
			if( isFlatStrategy ) {
				boolean b;
				if( b = !exists( ( char ) key ) ) {
					exists1( ( char ) key );
					_count++;
				}
				
				values[ ( char ) key ] = value;
				_version++;
				return b;
			}
			
			if( _buckets == null ) initialize( 7 );
			short[] _nexts         = nexts;
			int     hash           = Array.hash( key );
			int     collisionCount = 0;
			int     bucketIndex    = bucketIndex( hash );
			int     bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) {
					values[ next ] = value;
					_version++;
					return false;
				}
				
				next = _nexts[ next ];
				if( _nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			
			int index;
			if( 0 < _freeCount ) {
				index     = _freeList;
				_freeList = StartOfFreeList - _nexts[ _freeList ];
				_freeCount--;
			}
			else {
				if( _count == _nexts.length ) {
					int i = Array.prime( _count * 2 );
					if( flatStrategyThreshold < i && _count < flatStrategyThreshold ) i = flatStrategyThreshold;
					
					resize( i );
					if( isFlatStrategy ) return put( key, value );
					
					
					bucket = ( ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1 );
				}
				index = _count++;
			}
			
			nexts[ index ]          = ( short ) bucket;
			keys[ index ]           = ( short ) key;
			values[ index ]         = value;
			_buckets[ bucketIndex ] = ( char ) ( index + 1 );
			_version++;
			
			return true;
		}
		
		/**
		 * Inserts or updates the value for the conceptual null key.
		 *
		 * @param value The value (can be null).
		 * @return The previous value associated with the null key, or null if none.
		 */
		public boolean put( V value ) {
			boolean b = !hasNullKey;
			
			hasNullKey   = true;
			nullKeyValue = value;
			_version++;
			return b;
			
		}
		
		/**
		 * Removes a key-value pair (boxed Character key).
		 *
		 * @param key The key to remove (can be null).
		 * @return The value of the removed entry if found and removed, or null if not found.
		 */
		public V remove(  Short     key ) {
			return key == null ?
					removeNullKey() :
					remove( ( short ) ( key + 0 ) );
		}
		
		/**
		 * Removes the conceptual null key mapping.
		 *
		 * @return The value associated with the null key if it was present and removed, or null otherwise.
		 */
		private V removeNullKey() {
			if( !hasNullKey ) return null;
			V oldValue = nullKeyValue;
			hasNullKey   = false;
			nullKeyValue = null; // Clear the value reference
			_version++;
			return oldValue;
		}
		
		/**
		 * Removes a key-value pair (primitive key).
		 *
		 * @param key The primitive char key to remove.
		 * @return The value of the removed entry if found and removed, or null if not found.
		 */
		public V remove( short key ) {
			V oldValue;
			if( isFlatStrategy ) {
				if( exists( ( char ) key ) ) {
					oldValue = values[ key ];
					if( oldValue != null ) values[ key ] = null;
					exists0( ( char ) key );
					_version++;
					return oldValue;
				}
				return null;
			}
			if( _buckets == null ) return null;
			
			int collisionCount = 0;
			int last           = -1;
			int hash           = Array.hash( key );
			int bucketIndex    = bucketIndex( hash );
			int i              = _buckets[ bucketIndex ] - 1;
			
			while( -1 < i ) {
				short next = nexts[ i ];
				if( keys[ i ] == key ) {
					oldValue = values[ i ];
					if( oldValue != null ) values[ key ] = null;
					
					if( last < 0 ) _buckets[ bucketIndex ] = ( char ) ( next + 1 );
					else nexts[ last ] = next;
					nexts[ i ] = ( short ) ( StartOfFreeList - _freeList );
					_freeList  = i;
					_freeCount++;
					_version++;
					return oldValue;
				}
				last = i;
				i    = next;
				if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return null;
		}
		
		/**
		 * Clears all mappings from the map. Resets to initial state (hash or flat depending on initial setup).
		 */
		public void clear() {
			hasNullKey = false;
			if( _count == 0 ) return;
			if( isFlatStrategy )
				if( isFlatStrategy = flatStrategyThreshold < 1 ) Array.fill( flat_bits, 0 );
				else flat_bits = null;
			else {
				Arrays.fill( _buckets, ( char ) 0 );
				Arrays.fill( nexts, ( short ) 0 );
				Arrays.fill( values, 0, _count, null ); // Clear value references
				_freeList  = -1;
				_freeCount = 0;
			}
			_count = 0;
			_version++;
		}
		
		/**
		 * Ensures the map’s capacity is at least the specified value.
		 * May trigger resize or switch to flat mode. Does nothing if current capacity is sufficient.
		 *
		 * @param capacity The minimum desired capacity. Must be non-negative.
		 * @return The actual capacity after ensuring (might be larger than requested).
		 * @throws IllegalArgumentException if capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			return !isFlatStrategy && _buckets == null ?
					initialize( capacity ) :
					resize( Array.prime( capacity ) );
		}
		
		/**
		 * Trims the capacity to the current number of entries.
		 */
		public void trim() { trim( size() ); }
		
		/**
		 * Trims the capacity to the specified value (at least the current size).
		 * Only applicable in hash map mode. Does nothing in flat mode.
		 * The actual capacity will be the smallest prime number >= requested capacity.
		 *
		 * @param capacity The desired capacity. Must be >= size() and <= flatStrategyThreshold.
		 * @throws IllegalArgumentException if capacity is less than current size or greater than threshold.
		 */
		public void trim( int capacity ) {
			capacity = Array.prime( capacity );
			if( length() <= capacity ) return;
			if( isFlatStrategy ) {
				if( capacity <= flatStrategyThreshold ) resize( capacity );
				return;
			}
			
			short[]        old_next   = nexts;
			short[] old_keys   = keys;
			V[]            old_values = values;
			int            old_count  = _count;
			_version++;
			initialize( capacity );
			copy( old_next, old_keys, old_values, old_count );
		}
		
		/**
		 * Resizes the map's internal structures or switches between hash and flat modes.
		 *
		 * @param newSize The target capacity hint (will be adjusted).
		 * @return The new actual capacity after resizing/switching.
		 */
		private int resize( int newSize ) {
			newSize = Math.min( newSize, FLAT_ARRAY_SIZE ); ; ;
			
			if( isFlatStrategy ) {
				if( newSize <= flatStrategyThreshold )//switch to hash map strategy
				{
					V[] _values = values;
					isFlatStrategy = false;
					
					initialize( newSize );
					for( int token = -1; ( token = next1( token + 1, flat_bits ) ) != -1; )
					     put( ( short ) token, _values[ token ] );
					flat_bits = null;
				}
				return length();
			}
			else if( flatStrategyThreshold < newSize ) {
				
				V[] _values = values;
				values    = equal_hash_V.copyOf( null, FLAT_ARRAY_SIZE );
				flat_bits = new long[ FLAT_ARRAY_SIZE / 64 ];
				
				for( int i = -1; ( i = unsafe_token( i ) ) != -1; ) {
					char key = ( char ) keys[ i ];
					exists1( key );
					values[ key ] = _values[ i ];
				}
				
				isFlatStrategy = true;
				
				_buckets = null;
				nexts    = null;
				keys     = null;
				
				_count -= _freeCount;
				
				_freeList  = -1;
				_freeCount = 0;
				return length();
			}
			
			
			_version++;
			short[]        new_next   = Arrays.copyOf( nexts, newSize );
			short[] new_keys   = Arrays.copyOf( keys, newSize );
			V[]            new_values = Arrays.copyOf( values, newSize );
			final int      count      = _count;
			
			_buckets = new char[ newSize ];
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = ( short ) ( _buckets[ bucketIndex ] - 1 ); //relink chain
					_buckets[ bucketIndex ] = ( char ) ( i + 1 );
				}
			
			nexts  = new_next;
			keys   = new_keys;
			values = new_values;
			return length();
		}
		
		/**
		 * Copies entries during trimming.
		 *
		 * @param old_nexts  Old hash_nexts array.
		 * @param old_keys   Old keys array.
		 * @param old_values Old values array.
		 * @param old_count  Old count.
		 */
		private void copy( short[] old_nexts, short[] old_keys, V[] old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_nexts[ i ] < -1 ) continue;
				
				keys[ new_count ]   = old_keys[ i ];
				values[ new_count ] = old_values[ i ];
				
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) );
				nexts[ new_count ]      = ( short ) ( _buckets[ bucketIndex ] - 1 );
				_buckets[ bucketIndex ] = ( char ) ( new_count + 1 );
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
			_freeList  = -1; // Reset free list
		}
		
		
		@Override
		@SuppressWarnings( "unchecked" )
		public RW< V > clone() { return ( RW< V > ) super.clone(); }
		
		/**
		 * Sets a key as present in flat mode using the bitset.
		 */
		protected final void exists1( char key ) { flat_bits[ key >>> 6 ] |= 1L << key; }
		
		/**
		 * Clears a key's presence in flat mode using the bitset.
		 */
		protected final void exists0( char key ) { flat_bits[ key >>> 6 ] &= ~( 1L << key ); }
		
		/**
		 * Static instance used for obtaining the EqualHashOf strategy
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
	}
	
	/**
	 * Returns the {@link Array.EqualHashOf} implementation for {@code RW<V>}.
	 * This is typically used for nesting maps.
	 *
	 * @param <V> The type of values in the map.
	 * @return The {@link Array.EqualHashOf} instance for {@code RW<V>}.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() { return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; }
}