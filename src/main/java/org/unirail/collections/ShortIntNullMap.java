package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;
import java.util.ConcurrentModificationException;


/**
 * A specialized map for mapping 2 bytes primitive keys (65,536 distinct values) to a nullable primitive values.
 * Implements a HYBRID strategy:
 * 1. Starts as a hash map with separate chaining, optimized for sparse data.
 * 2. Automatically transitions to a direct-mapped flat array when the hash map is full and reaches its capacity 0x7FFF entries (exclude nullKey entity) and needs to grow further.
 * This approach balances memory efficiency for sparse maps with guaranteed O(1) performance and full key range support for dense maps.
 */
public interface ShortIntNullMap {
	
	/**
	 * Abstract base class providing common structure and read-only operations for the {@link CharIntNullMap}.
	 * It encapsulates the core logic for both the hash map and flat array strategies, managing the underlying data structures
	 * and providing methods for querying size, checking containment, retrieving values, and iterating using tokens.
	 * This class is not meant to be instantiated directly but serves as the foundation for the read-write implementation (`RW`).
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Stores whether a mapping for the conceptual null key exists.
		 */
		protected boolean     hasNullKey;
		/**
		 * Stores whether the conceptual null key maps to a non-null value.
		 */
		protected boolean     nullKeyHasValue;
		/**
		 * Stores the primitive int value associated with the conceptual null key. Only meaningful if {@link #nullKeyHasValue} is true.
		 */
		protected int nullKeyValue;
		
		/**
		 * Hash table buckets (hash map mode only). Stores 1-based indices into `nexts`/`keys`/`values` arrays,
		 * representing the head of the collision chain for each bucket. A value of 0 indicates an empty bucket.
		 */
		protected char[]                 _buckets;
		/**
		 * Stores collision chain links and free list information (hash map mode only).
		 * Values meaning:
		 * <ul>
		 *     <li>{@code >= 0}: Index of the next entry in the collision chain within `keys`/`values`.</li>
		 *     <li>{@code -1}: Terminator for a collision chain.</li>
		 *     <li>{@code -2}: Slot has never been used or is currently occupied by a key-value pair.</li>
		 *     <li>{@code <= StartOfFreeList (-3)}: Index is part of the free list. `StartOfFreeList - value` gives the index of the next free slot.</li>
		 * </ul>
		 * Because it's a `short[]`, its maximum positive index is 32767, limiting the hash map phase capacity.
		 */
		protected short[]                nexts;
		/**
		 * Stores the char keys (hash map mode only). Indexed parallel to `nexts` and `values`.
		 */
		protected short[]         keys; // Initialized to empty array.
		/**
		 * Stores the int values, managing nullability. Indexed parallel to `keys` and `nexts` in hash map mode, or directly by key in flat mode.
		 */
		protected IntNullList.RW values;
		/**
		 * Represents the number of active elements.
		 * <p>In hash map mode: Total number of slots used in the `keys`/`nexts`/`values` arrays (including free slots linked in the free list).
		 * Actual entry count is `_count - _freeCount`.
		 * <p>In flat array mode: The number of set bits in the `nulls` bitset, representing the exact count of non-null keys present.
		 */
		protected int                    _count;
		/**
		 * Head index of the free list in `nexts` (hash map mode only). -1 if the free list is empty.
		 */
		protected int                    _freeList = -1;
		/**
		 * Number of slots currently in the free list (hash map mode only).
		 */
		protected int                    _freeCount;
		/**
		 * Version counter incremented on structural modifications. Used for fail-fast iteration.
		 */
		protected int                    _version;
		
		/**
		 * Internal constant used in `nexts` to mark entries as part of the free list. See {@link #nexts}.
		 */
		protected static final int StartOfFreeList = -3;
		/**
		 * Mask to extract the index part from a token.
		 */
		
		/**
		 * Number of bits to shift the version part within a token.
		 */
		protected static final int VERSION_SHIFT  = 32;
		/**
		 * Special index value used in tokens to represent the conceptual null key.
		 * It's intentionally outside the valid range of array indices (0 to 65535).
		 * Value: {@value #NULL_KEY_INDEX}.
		 */
		protected static final int NULL_KEY_INDEX = 0x1_FFFF; // 65536 + 1
		
		/**
		 * Represents an invalid or expired token. Value: {@value #INVALID_TOKEN}.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		
		/**
		 * Flag indicating if the map is currently operating in the flat array mode (true) or hash map mode (false).
		 */
		protected boolean isFlatStrategy() { return nulls != null; }
		
		/**
		 * Bitset used in flat array mode to track the presence of keys. Each bit corresponds to a char key (0-65535).
		 * A set bit indicates the key is present in the map. Size is {@value #NULLS_SIZE} longs (1024).
		 * Null if not in flat mode.
		 */
		protected long[] nulls;
		
		/**
		 * Fixed size of the effective array in flat mode (number of possible char keys). Value: {@value #FLAT_ARRAY_SIZE}.
		 */
		protected static final int FLAT_ARRAY_SIZE = 0x10000; // 65536
		/**
		 * Size of the `nulls` array in longs. Value: {@value #NULLS_SIZE}.
		 */
		protected static final int NULLS_SIZE      = FLAT_ARRAY_SIZE / 64; // 1024
		
		
		/**
		 * Checks if this map contains no key-value mappings.
		 *
		 * @return {@code true} if the map is empty (size is 0), {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the total number of key-value mappings currently in this map.
		 * This includes the conceptual null key if it is present.
		 * The calculation depends on the current strategy (hash map or flat array).
		 *
		 * @return The number of key-value mappings.
		 */
		public int size() {
			// Hash Mode: _count includes used slots and free slots. Subtract free slots for actual entries.
			// Flat Mode: _count directly tracks the number of set bits (actual entries).
			// Add 1 if the null key mapping exists in either mode.
			return (
					       isFlatStrategy() ?
							       _count :
							       _count - _freeCount ) + (
					       hasNullKey ?
							       1 :
							       0 );
		}
		
		
		/**
		 * Returns the number of key-value mappings in the map. Alias for {@link #size()}.
		 *
		 * @return The number of mappings.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the internal storage.
		 * <p>In hash map mode, this is the allocated size of the `keys`, `nexts`, and `values` arrays.
		 * <p>In flat array mode, this is the fixed size {@value #FLAT_ARRAY_SIZE} (65536).
		 * Returns 0 if the map is uninitialized (in hash mode).
		 *
		 * @return The current capacity.
		 */
		public int length() {
			return isFlatStrategy() ?
					FLAT_ARRAY_SIZE :
					nexts == null ?
							0 :
							nexts.length;
		}
		
		/**
		 * Checks if the map contains a mapping for the specified key (boxed {@code Character}).
		 * Handles the conceptual null key.
		 *
		 * @param key The key to check (can be null).
		 * @return {@code true} if a mapping for the key exists, {@code false} otherwise.
		 */
		public boolean containsKey(  Short     key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive key (0 to 65535).
		 * @return {@code true} if a mapping for the key exists, {@code false} otherwise.
		 */
		public boolean containsKey( short key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this map contains one or more keys mapped to the specified value (boxed {@code Integer}).
		 * This involves iterating through the values, which can be inefficient (O(N) in hash mode, potentially O(capacity) in flat mode if sparse).
		 * Checks both non-null key mappings and the null key mapping (if present).
		 *
		 * @param value The value to search for (can be null, representing a conceptual null value).
		 * @return {@code true} if the value is found, {@code false} otherwise.
		 */
		public boolean containsValue(  Integer   value ) {
			int i;
			if( value == null )
				if( hasNullKey && !nullKeyHasValue ) return true;
				else if( isFlatStrategy() ) {
					for( int t = -1; ( t = unsafe_token( t ) ) != -1; )
						if( !hasValue( t ) )
							return true;
					return false;
				}
				else return ( i = values.nextNullIndex( -1 ) ) != -1 && i < _count - _freeCount;
			return hasNullKey && nullKeyHasValue && nullKeyValue == value || values.indexOf( value ) != -1;
		}
		
		/**
		 * Checks if the map contains one or more keys mapped to the specified primitive value.
		 * This involves iterating through the values, which can be inefficient (O(N) in hash mode, potentially O(capacity) in flat mode if sparse).
		 * Checks both non-null key mappings and the null key mapping (if present and has a non-null value).
		 *
		 * @param value The primitive value to search for.
		 * @return {@code true} if the value is found, {@code false} otherwise.
		 */
		public boolean containsValue( int value ) { return hasNullKey && nullKeyHasValue && nullKeyValue == value || values.indexOf( value ) != -1; }
		
		
		/**
		 * Returns a token associated with the specified key (boxed {@code Character}).
		 * A token can be used with {@link #key(long)}, {@link #value(long)}, etc., for efficient access.
		 * Handles the conceptual null key, returning a special token if present.
		 *
		 * @param key The key whose token is requested (can be null).
		 * @return A valid token if the key is found, or {@link #INVALID_TOKEN} (-1) otherwise.
		 */
		public long tokenOf(  Short     key ) {
			return key == null ?
					hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN :
					tokenOf( ( short ) ( key + 0 ) );
		}
		
		/**
		 * Returns a token associated with the specified primitive key.
		 * A token can be used with {@link #key(long)}, {@link #value(long)}, etc., for efficient access.
		 *
		 * @param key The primitive key.
		 * @return A valid token if the key is found, or {@link #INVALID_TOKEN} (-1) otherwise.
		 */
		public long tokenOf( short key ) {
			// Flat Mode: Check bitset. If present, the key itself is the index.
			if( isFlatStrategy() )
				return exists( ( char ) key ) ?
						token( ( char ) key ) :
						INVALID_TOKEN;
			
			// Hash Mode: Perform hash lookup.
			// Map must be initialized and not effectively empty (size > 0 or just null key exists)
			if( _buckets == null || size() == 0 ) return INVALID_TOKEN; // Check size() to account for only null key present
			
			int hash = Array.hash( key );
			
			int i = _buckets[ bucketIndex( hash ) ] - 1;
			
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key ) return token( i );
				i = nexts[ i ];
				if( nexts.length < ++collisionCount ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		
		/**
		 * Returns the token for the first entry in the map for iteration purposes.
		 * If the map contains only the null key, its token ({@link #token} with {@link #NULL_KEY_INDEX}) is returned.
		 * If the map contains non-null keys, the token of the first non-null key according to internal order is returned.
		 * Internal order depends on the current strategy (bit order in flat mode, slot order in hash mode).
		 *
		 * @return The token of the first entry, or {@link #INVALID_TOKEN} if the map is empty.
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
		 * Returns the token for the next entry in the map during iteration.
		 * Takes the current token and finds the subsequent entry according to the internal order.
		 * If the last non-null key's token is passed, and the null key exists, the null key's token is returned next.
		 *
		 * @param token The current token obtained from {@link #token()} or a previous call to this method.
		 * @return The token of the next entry, or {@link #INVALID_TOKEN} if there are no more entries or if the provided token is invalid/expired (due to concurrent modification).
		 */
		public long token( final long token ) {
			// Validate token: must not be invalid and version must match current map version
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
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
		 * Returns the index of the next non-null key for fast, **unsafe** iteration, bypassing concurrency checks.
		 *
		 * <p><b>Usage:</b> Start iteration by calling {@code unsafe_token(-1)}. Subsequent calls should pass the previously returned index
		 * to get the next index. Iteration stops when this method returns {@code -1}.
		 *
		 * <p><b>Excludes Null Key:</b> This iteration method **only** covers non-null keys. Check for the null key separately using
		 * {@link #hasNullKey()} and access its value via {@link #nullKeyValue()}.
		 *
		 * <p><strong>⚠️ WARNING: UNSAFE ⚠️</strong> This method offers higher performance than {@link #token(long)} by omitting version checks.
		 * However, if the map is structurally modified (e.g., keys added or removed, resizing) during an iteration using this method,
		 * the behavior is undefined. It may lead to missed entries, incorrect results, infinite loops, or runtime exceptions
		 * (like {@code ArrayIndexOutOfBoundsException}). **Only use this method when you can guarantee that no modifications
		 * will occur to the map during the iteration.**
		 *
		 * @param token The index returned by the previous call to this method, or {@code -1} to start iteration.
		 * @return The index of the next non-null key, or {@code -1} if no more non-null keys exist.
		 * @see #token(long) For safe iteration that includes the null key and checks for concurrent modifications.
		 */
		public int unsafe_token( final int token ) {
			if( isFlatStrategy() )
				return next1( index( token ) );
			else
				for( int i = token + 1; i < _count; i++ )
					if( -2 < nexts[ i ] ) return i;
			
			return -1; // No more entries
		}
		
		/**
		 * Checks if the map contains a mapping for the conceptual null key.
		 *
		 * @return {@code true} if the null key is present, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Checks if the conceptual null key currently maps to a non-null value.
		 * Returns {@code false} if the null key is not present or if it maps to a conceptual null value.
		 *
		 * @return {@code true} if the null key exists and has a non-null value, {@code false} otherwise.
		 */
		public boolean nullKeyHasValue() { return hasNullKey && nullKeyHasValue; }
		
		/**
		 * Returns the primitive value associated with the conceptual null key.
		 * Behavior is undefined if the null key is not present ({@link #hasNullKey()} is false).
		 * Returns 0 if the null key is present but maps to a conceptual null value ({@link #nullKeyHasValue} is false).
		 *
		 * @return The value associated with the null key, or 0 if it maps to null.
		 */
		public int nullKeyValue() { return  nullKeyValue; }
		
		
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Retrieves the primitive key associated with the given token.  Before calling this method ensure that this token is not point to the isKeyNull
		 *
		 * @param token The token representing a non-null key-value pair (obtained from {@link #tokenOf}, {@link #token()}, or {@link #token(long)}).
		 * @return The primitive key associated with the token.
		 * @throws ArrayIndexOutOfBoundsException if the token represents the null key (index {@link #NULL_KEY_INDEX}), is invalid, or expired.
		 *                                        In flat mode, the index is the key itself, so this exception is less likely unless the token is corrupt.
		 */
		public short key( long token ) {
			return ( short ) (short) (
					isFlatStrategy() ?
							index( token ) :
							keys[ index( token ) ] );
		}
		
		/**
		 * Checks if the entry associated with the given token has a non-null value.
		 * Handles both regular tokens and the special null key token.
		 *
		 * @param token The token to check (can be for the null key).
		 * @return {@code true} if the token is valid and the corresponding entry maps to a non-null value, {@code false} otherwise (including if the value is conceptually null or the token is invalid/expired).
		 */
		public boolean hasValue( long token ) {
			return isKeyNull( token ) ?
					nullKeyHasValue :
					values.hasValue( index( token ) );
		}
		
		/**
		 * Returns the primitive value associated with the specified token.
		 * <p>
		 * <b>Precondition:</b> This method should only be called if {@link #hasValue(long)} returns {@code true} for the given token.
		 * Calling it for a token associated with a {@code null} value results in undefined behavior (likely returning 0 or a stale value depending on the strategy).
		 * <p>
		 * The result is undefined if the token is {@link #INVALID_TOKEN} or has expired due to structural modification.
		 *
		 * @param token The token for which to retrieve the value.
		 * @return The primitive value associated with the token, or 0 if the value is conceptually null.
		 */
		public int value( long token ) {
			return (
					isKeyNull( token ) ?
							nullKeyValue :
							values.get( index( token ) ) );
		}
		
		/**
		 * Computes an order-independent hash code for this map.
		 * The hash code is based on the hash codes of the keys and values (or a seed for null values)
		 * for all entries, including the null key if present.
		 *
		 * @return The computed hash code.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) );
				h = Array.mix( h, hasValue( token ) ?
						Array.hash( value( token ) ) :
						seed );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			// Include null key entry if present
			if( hasNullKey ) {
				int h = nullKeyHasValue ?
						Array.mix( seed, Array.hash( nullKeyValue ) ) :
						Array.hash( seed );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			// Final mixing step based on overall size
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode(); // Seed for hashing
		
		/**
		 * Compares this map with the specified object for equality.
		 * Returns true only if the object is also a {@code CharIntNullMap.R}, has the same size,
		 * and contains the same key-value mappings (including null key and null values).
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		/**
		 * Compares this map with another {@code CharIntNullMap.R} instance for equality.
		 * Checks for size, null key presence/value, and then iterates through all non-null key mappings
		 * ensuring the other map contains the same keys with equal values (or both null).
		 *
		 * @param other The other {@code R} map instance to compare with.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    hasNullKey && ( nullKeyHasValue != other.nullKeyHasValue || nullKeyHasValue && nullKeyValue != other.nullKeyValue ) ||
			    size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || hasValue( token ) != other.hasValue( t ) ||
				    hasValue( token ) && value( token ) != other.value( t ) )
					return false;
			}
			return true;
		}
		
		
		/**
		 * Creates and returns a shallow copy of this map instance.
		 * The keys and values themselves are not cloned, but the internal structures (arrays, bitset)
		 * holding them are copied. The clone will have its own version counter.
		 *
		 * @return A clone of this map instance.
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				if( isFlatStrategy() ) {
					dst.nulls  = nulls.clone();
					dst.values = values.clone();
				}
				else if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.nexts    = nexts.clone();
					dst.keys     = keys.clone();
					dst.values   = values.clone();
				}
				// Primitive fields (hasNullKey, nullKeyValue, _count, etc.) are copied by super.clone()
				// Reset version? Typically clones start at version 0 or copy the version. Here it seems version is just copied.
				// Let's assume the copied version is intended. dst._version = 0; might be safer if iterators on clones are expected.
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		/**
		 * Returns a string representation of the map in JSON format.
		 * Delegates to {@link #toJSON()}.
		 *
		 * @return JSON string representation.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		
		/**
		 * Serializes the map content into the provided {@link JsonWriter}.
		 * Outputs keys as JSON strings and values as JSON numbers or `null`.
		 * The null key is represented by the JSON string `"null"`.
		 *
		 * @param json The {@link JsonWriter} instance to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey )
				json.name().value( nullKeyHasValue ?
						                   nullKeyValue :
						                   null );
			
			if( isFlatStrategy() )
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( token ).value( hasValue( token ) ?
						                               value( token ) :
						                               null );
			else
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json
						     .name( String.valueOf( keys[ index( token ) ] ) )
						     .value( hasValue( token ) ?
								             value( token ) :
								             null );
			
			
			json.exitObject();
		}
		
		// --- Helper methods ---
		
		/**
		 * Calculates the bucket index for a given hash code in hash map mode.
		 * Ensures the index is non-negative and within the bounds of the `_buckets` array.
		 *
		 * @param hash The hash code of the key.
		 * @return The calculated bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a `long` token by combining the current map version and the entry index.
		 * The version is stored in the upper 32 bits, the index in the lower 32 bits.
		 *
		 * @param index The entry index (or {@link #NULL_KEY_INDEX} for the null key).
		 * @return The combined token.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index; }
		
		/**
		 * Extracts the index part from a `long` token.
		 *
		 * @param token The token.
		 * @return The index encoded in the token.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts the version part from a `long` token.
		 *
		 * @param token The token.
		 * @return The map version encoded in the token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
		/**
		 * Checks if a key exists in flat mode by querying the `nulls` bitset.
		 * Assumes {@link #isFlatStrategy} is true and `nulls` is not null.
		 *
		 * @param key The primitive key.
		 * @return {@code true} if the bit corresponding to the key is set, {@code false} otherwise.
		 */
		protected final boolean exists( char key ) { return ( nulls[ key >>> 6 ] & 1L << key ) != 0; }
		
		
		/**
		 * Finds the index of the next set bit (1) in the `nulls` array, starting from or after the specified bit index `key`.
		 * Used for iteration in flat mode.
		 *
		 * @param key The bit index (char value) to start searching from (exclusive).
		 * @return The index of the next set bit, or -1 if no more set bits are found.
		 */
		public int next1( int key ) { return next1( key, nulls ); }
		
		public static int next1( int bit, long[] nulls ) {
			
			if( 0xFFFF < ++bit ) return -1;
			int  index = bit >>> 6;
			long value = nulls[ index ] & -1L << ( bit & 63 );
			
			while( value == 0 )
				if( ++index == NULLS_SIZE ) return -1;
				else value = nulls[ index ];
			
			return ( index << 6 ) + Long.numberOfTrailingZeros( value );
		}
		
	}
	
	/**
	 * Read-write implementation of the {@link CharIntNullMap}.
	 * Extends the read-only functionality of {@link R} with methods for adding, removing, and modifying entries,
	 * handling resizing, and managing the transition between hash map and flat array strategies.
	 */
	class RW extends R {
		/**
		 * The capacity threshold that triggers the switch *to* the flat array strategy during a resize.
		 * If a resize operation in hash map mode requires a capacity greater than this value,
		 * the map converts to the flat array strategy. Set to {@value #flatStrategyThreshold} (32767),
		 * which is the maximum number of entries addressable by `short` indices used in `nexts`.
		 */
		protected static final int flatStrategyThreshold = 0x7FFF; // 32767
		
		/**
		 * Constructs an empty {@code RW} map with a default initial capacity (which is 0, meaning initialization happens on first insert).
		 */
		public RW() {
			this( 0 ); // Start with 0 capacity, initialize on first put
		}
		
		/**
		 * Constructs an empty {@code RW} map with the specified initial capacity.
		 * If the requested capacity exceeds {@link #flatStrategyThreshold}, the map will initialize directly into flat array mode.
		 * Otherwise, it initializes in hash map mode.
		 *
		 * @param capacity The desired initial capacity. If 0 or less, initialization is deferred until the first insertion.
		 */
		public RW( int capacity ) {
			values = new IntNullList.RW( 0 );
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Initializes the internal data structures for the map with a specified target capacity.
		 * Selects the appropriate strategy (hash map or flat array) based on the capacity.
		 * If hash map mode is chosen, allocates `_buckets`, `nexts`, `keys` arrays.
		 * If flat array mode is chosen, allocates `nulls`.
		 * The `values` list should be initialized beforehand.
		 *
		 * @param capacity The desired capacity.
		 * @return The actual initialized capacity (might be adjusted, e.g., to a prime number or {@value #FLAT_ARRAY_SIZE}).
		 */
		private int initialize( int capacity ) {
			_version++;
			_count = 0; // Flat mode _count tracks actual entries
			if( flatStrategyThreshold < capacity ) {
				
				nulls    = new long[ NULLS_SIZE ]; // 1024 longs
				_buckets = null;
				nexts    = null;
				keys     = null;
				return FLAT_ARRAY_SIZE;
			}
			nulls      = null;
			_buckets   = new char[ capacity ];
			nexts      = new short[ capacity ];
			keys       = new short[ capacity ];
			_freeList  = -1;
			_freeCount = 0;
			return length();
		}
		
		/**
		 * Associates the specified value (boxed {@code Integer}) with the specified key (boxed {@code Character}) in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles null key and null value.
		 *
		 * @param key   The key ( can be null).
		 * @param value The value ( can be null).
		 * @return {@code true} always, as this method performs an insert or update (consistent with Map.put semantics.
		 * Consider if returning modification status (new vs update) is needed.
		 */
		public boolean put(  Short     key,  Integer   value ) {
			return key == null ?
					value == null ?
							put() :
							putNullKey( value ) :
					value == null ?
							putNullValue( ( short ) ( key + 0 ) ) :
							put( ( short ) ( key + 0 ), ( int ) ( value + 0 ), true );
		}
		
		/**
		 * Associates the specified value (boxed {@code Integer}) with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced. Handles null value.
		 *
		 * @param key   The primitive key.
		 * @param value The value ( can be null).
		 * @return {@code true} if the map state was changed (insert or update).
		 */
		public boolean put( short key,  Integer   value ) {
			return value == null ?
					putNullValue( ( short ) ( key + 0 ) ) :
					put( ( short ) ( key + 0 ), ( int ) ( value + 0 ), true );
		}
		
		/**
		 * Associates the specified primitive value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The primitive key.
		 * @param value The primitive value.
		 * @return {@code true} if the map state was changed (insert or update).
		 */
		public boolean put( short key, int value ) { return put( key, value, true ); }
		
		
		public boolean putNullKey( int  value ) { return put( value, true ); }
		
		public boolean putNullValue( short key ) { return put( key, ( int ) 0, false ); }
		
		/**
		 * Core insertion logic for non-null keys, handling both hash map and flat array strategies.
		 *
		 * @param key      The primitive key to insert or update.
		 * @param value    The primitive value to associate with the key.
		 * @param hasValue {@code true} if `value` represents a non-null value, {@code false} if it represents a conceptual null.
		 * @return {@code true} if the map was structurally modified (new key added) or an existing key was updated (only if behavior is 1).
		 * {@code false} if the key already existed and behavior was 2 (skip).
		 * @throws IllegalArgumentException        If behavior is 0 and the key already exists.
		 * @throws ConcurrentModificationException If potential hash chain corruption is detected.
		 */
		private boolean put( short key, int value, boolean hasValue ) {
			if( isFlatStrategy() ) {
				boolean b;
				if( b = !exists( ( char ) key ) ) {
					exists1( ( char ) key );
					_count++;
				}
				
				if( hasValue ) values.set1( ( char ) key, value );
				else values.set1( ( char ) key, null );
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
					if( hasValue ) values.set1( next, value );
					else values.set1( next, null );
					_version++;
					return false;
				}
				
				next = _nexts[ next ];
				if( _nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
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
					if( _count < flatStrategyThreshold ) { if( flatStrategyThreshold < i ) i = flatStrategyThreshold; }
					
					
					resize( i );
					if( isFlatStrategy() ) return put( key, value, hasValue );
					
					bucket = _buckets[ bucketIndex = bucketIndex( hash ) ] - 1;
				}
				index = _count++;
			}
			
			nexts[ index ] = ( short ) bucket;
			keys[ index ]  = ( short ) key;
			
			if( hasValue ) values.set1( index, value );
			else values.set1( index, null );
			
			_buckets[ bucketIndex ] = ( char ) ( index + 1 );
			_version++;
			
			return true;
		}
		
		/**
		 * Associates the specified value (boxed {@code Integer}) with the conceptual null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced. Handles null value.
		 *
		 * @param value The value ( can be null).
		 * @return {@code true} if the map state was changed (insert or update).
		 */
		public boolean put(  Integer   value ) {
			return value == null ?
					put() :
					put( value );
		}
		
		/**
		 * Associates the specified primitive value with the conceptual null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The primitive value.
		 * @return {@code true} if the map state was changed (insert or update).
		 */
		public boolean put( int value ) {
			_version++;
			nullKeyHasValue = true;
			
			if( hasNullKey ) {
				nullKeyValue = ( int ) value;
				return false;
			}
			
			hasNullKey   = true;
			nullKeyValue = ( int ) value;
			return true;
		}
		
		/**
		 * Associates the specified value with the null key in this map (boxed value).
		 * <p>
		 * If the map previously contained a mapping for the null key, the old value is replaced by the specified value.
		 *
		 * @param value the value to be associated with the null key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 */
		private boolean put( int value, boolean hasValue ) {
			boolean b = !hasNullKey;
			
			hasNullKey = true;
			if( nullKeyHasValue = hasValue ) nullKeyValue = ( int ) value;
			_version++;
			return b;
		}
		
		/**
		 * Core insertion/update logic for the conceptual null key, and  null value .
		 */
		public boolean put() {
			
			if( !hasNullKey ) {
				hasNullKey = true;
				_version++;
				
				return true;
			}
			
			if( nullKeyHasValue ) {
				nullKeyHasValue = false;
				_version++;
			}
			
			return false;
			
		}
		
		/**
		 * Removes the mapping for the specified key (boxed {@code Character}) from this map, if present.
		 * Handles the conceptual null key.
		 *
		 * @param key The key whose mapping is to be removed (can be null).
		 * @return The token of the removed entry if found and removed, or {@link #INVALID_TOKEN} (-1) if the key was not found.
		 * Note: The returned token is based on the state *before* removal but with an incremented version, making it technically invalid for subsequent access, but useful for identifying *which* entry was removed.
		 */
		public boolean remove(  Short     key ) {
			return key == null ?
					removeNullKey() :
					remove( ( short ) ( key + 0 ) );
		}
		
		/**
		 * Removes the mapping for the conceptual null key, if present.
		 *
		 * @return A token representing the null key (using {@link #NULL_KEY_INDEX}) if it was present and removed,
		 * or {@link #INVALID_TOKEN} if the null key was not present.
		 */
		private boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey = false;
			_version++;
			// Return a token representing the null key conceptually
			return true;
		}
		
		/**
		 * Removes the mapping for the specified primitive key from this map, if present.
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return true if remove entry or false no mapping was found for the  key.
		 */
		public boolean remove( short key ) {
			if( _count == 0 ) return false;
			
			if( isFlatStrategy() ) {
				if( exists( ( char ) key ) ) {
					exists0( ( char ) key );
					_count--;
					_version++;
					return true;
				}
				return false;
			}
			// Handle edge cases: if map is uninitialized or empty, nothing to remove
			if( _buckets == null || _count == 0 )
				return false; // Return invalid token indicating no removal
			
			// Compute hash and bucket index for the key to locate its chain
			int hash           = Array.hash( key );                // Hash the key using Array.hash
			int bucketIndex    = bucketIndex( hash );       // Map hash to bucket index
			int last           = -1;                             // Previous index in chain (-1 if 'i' is head)
			int i              = _buckets[ bucketIndex ] - 1;         // Head of chain (convert 1-based to 0-based)
			int collisionCount = 0;                    // Counter to detect infinite loops or concurrency issues
			
			// Traverse the linked list in the bucket to find the key
			
			while( -1 < i ) {
				short next = nexts[ i ];                   // Get next index in the chain
				if( keys[ i ] == key ) {                  // Found the key at index 'i'
					
					if( values.isFlatStrategy ) {
						values.nulls.set0( i );//optional
						
						if( last < 0 ) _buckets[ bucketIndex ] = ( char ) ( next + 1 );
						else nexts[ last ] = next;
					}
					else // values used compressedStrategy
					{
						// Step 1: Unlink the entry at 'i' from its chain
						if( last < 0 )
							// If 'i' is the head, update bucket to point to the next entry
							_buckets[ bucketIndex ] = ( char ) ( next + 1 );
						
						else
							// Otherwise, link the previous entry to the next, bypassing 'i'
							nexts[ last ] = next;
						
						// Step 2: Optimize removal if value is non-null and not the last non-null
						final int lastNonNullValue = values.nulls.last1(); // Index of last non-null value
						if( values.hasValue( i ) )
							if( i != lastNonNullValue ) {
								// Optimization applies: swap with last non-null entry
								// Step 2a: Copy key, next, and value from lastNonNullValue to i
								short   keyToMove = keys[ lastNonNullValue ];
								int            bucket    = bucketIndex( Array.hash( keyToMove ) );
								short          _next     = nexts[ lastNonNullValue ];
								
								keys[ i ]  = keyToMove;                         // Copy the key to the entry being removed
								nexts[ i ] = _next;         // Copy the next to the entry being removed
								values.set1( i, values.get( lastNonNullValue ) ); // Copy the value to the entry being removed
								
								// Step 2b: Update the chain containing keyToMove to point to 'i'
								int prev = -1;                     // Previous index in keyToMove’s chain
								collisionCount = 0;// Reset collision counter
								
								// Start at chain head
								for( int current = _buckets[ bucket ] - 1; current != lastNonNullValue; prev = current, current = nexts[ current ] )
									if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
								
								if( -1 < prev ) nexts[ prev ] = ( short ) i;
								else _buckets[ bucket ] = ( char ) ( i + 1 );// If 'lastNonNullValue' the head, update bucket to the position of keyToMove
								
								
								values.set1( lastNonNullValue, null );        // Clear value (O(1) since it’s last non-null)
								i = lastNonNullValue;
							}
							else values.set1( i, null );                       // Clear value (may shift if not last)
					}
					
					
					nexts[ i ] = ( short ) ( StartOfFreeList - _freeList ); // Mark 'i' as free
					
					_freeList = i;
					_freeCount++;       // Increment count of free entries
					_version++;         // Increment version for concurrency control
					return true;    // Return token for removed/overwritten entry
				}
				
				// Move to next entry in chain
				last = i;
				i    = next;
				if( collisionCount++ > nexts.length ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				
			}
			
			return false; // Key not found
		}
		
		/**
		 * Removes all mappings from this map. The map will be empty after this call returns.
		 * Resets internal structures according to the current strategy.
		 */
		public void clear() {
			_version++;
			
			hasNullKey      = false;
			nullKeyHasValue = false;
			
			if( _count == 0 ) return;
			if( isFlatStrategy() )
				Array.fill( nulls, 0 );
			else {
				Arrays.fill( _buckets, ( char ) 0 );
				Arrays.fill( nexts, ( short ) 0 );
				_freeList  = -1;
				_freeCount = 0;
			}
			values.clear();
			_count = 0;
		}
		
		/**
		 * Ensures the map’s capacity is at least the specified value.
		 * May trigger resize or switch to flat mode.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The actual capacity after ensuring.
		 */
		public int ensureCapacity( int capacity ) {
			return capacity <= length() ?
					length() :
					!isFlatStrategy() && _buckets == null ?
							initialize( capacity ) :
							resize( Array.prime( capacity ) );
		}
		
		/**
		 * Trims the capacity of the map (in hash mode) to be the smallest prime number
		 * greater than or equal to the current {@link #size()}.
		 * If the map is in flat mode, this operation might switch it back to hash mode
		 * if the current size is less than or equal to {@link #flatStrategyThreshold}.
		 * If the map is already in hash mode, this can reduce memory usage if the map is sparse.
		 * Has no effect if the current capacity is already minimal.
		 */
		public void trim() { trim( size() ); }
		
		/**
		 * Trims the capacity of the map to the specified target capacity, if possible.
		 * <p><b>Hash Mode:</b> If currently in hash mode, the capacity is reduced to the smallest
		 * prime number greater than or equal to `max(capacity, size())`. No effect if the
		 * current capacity is already this small or smaller.
		 * <p><b>Flat Mode:</b> If currently in flat mode, and the target `capacity` (clamped to at least `size()`)
		 * is less than or equal to {@link #flatStrategyThreshold}, the map will attempt to switch
		 * back to hash map mode with the appropriately calculated prime capacity.
		 * If the target capacity still exceeds the threshold, the map remains in flat mode, and this operation has no effect.
		 *
		 * @param capacity The desired target capacity. Must not be less than the current {@link #size()}.
		 * @throws IllegalArgumentException if `capacity` is less than `size()`.
		 */
		public void trim( int capacity ) {
			int newSize = Array.prime( capacity );
			if( length() <= newSize ) return;
			
			if( isFlatStrategy() ) {
				if( capacity < flatStrategyThreshold ) resize( newSize );
				return;
			}
			
			short[]                old_next   = nexts;
			short[]         old_keys   = keys;
			IntNullList.RW old_values = values;
			int                    old_count  = _count;
			initialize( newSize );
			
			for( int i = 0; i < old_count; i++ )
				if( -2 < old_next[ i ] )
					if( old_values.hasValue( i ) )
						put( (short) old_keys[ i ],  old_values.get( i )  );
					else
						put( (short) old_keys[ i ], null );
		}
		
		/**
		 * Resizes the internal hash map arrays to a new capacity or handles strategy transitions.
		 * This method orchestrates rehashing, switching to flat mode, or switching back to hash mode.
		 *
		 * @param newSize The target capacity. For hash mode, this should ideally be prime.
		 *                Determines whether to switch strategies based on {@link #flatStrategyThreshold}.
		 * @return The new capacity after resizing or strategy change.
		 */
		private int resize( int newSize ) {
			
			newSize = Math.min( newSize, FLAT_ARRAY_SIZE ); ;
			
			if( isFlatStrategy() ) {
				if( newSize > flatStrategyThreshold ) return length();
				
				_version++;
				IntNullList.RW _values = values;
				values = new IntNullList.RW( newSize );
				long[] _nulls = nulls;
				
				initialize( newSize );
				for( int token = -1; ( token = next1( token, _nulls ) ) != -1; )
					if( _values.hasValue( token ) )
						put( ( short ) token, _values.get( token ), true );
					else
						put( ( short ) token, ( int ) 0, false );
				
				return length();
			}
			
			_version++;
			if( flatStrategyThreshold < newSize ) {
				
				IntNullList.RW _values = values;
				values = new IntNullList.RW( Math.max( _count * 3 / 2, FLAT_ARRAY_SIZE ) );//_count - optimistic
				long[] nulls = new long[ NULLS_SIZE ];
				
				for( int i = -1; ( i = unsafe_token( i ) ) != -1; ) {
					char key = ( char ) keys[ i ];
					exists1( key, nulls );// Mark the key as present in the NEW nulls bitset.
					if( _values.hasValue( i ) )
						values.set1( key, _values.get( i ) );
					else
						values.set1( key, null );
				}
				
				this.nulls = nulls;
				_buckets   = null;
				nexts      = null;
				keys       = null;
				
				_count -= _freeCount;
				
				_freeList  = -1;
				_freeCount = 0;
				return FLAT_ARRAY_SIZE;
			}
			
			
			short[]        new_next = Arrays.copyOf( nexts, newSize );
			short[] new_keys = Arrays.copyOf( keys, newSize );
			final int      count    = _count;
			
			_buckets = new char[ newSize ];
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = ( short ) ( _buckets[ bucketIndex ] - 1 ); //relink chain
					_buckets[ bucketIndex ] = ( char ) ( i + 1 );
				}
			
			nexts = new_next;
			keys  = new_keys;
			return length();
		}
		
		
		/**
		 * Creates and returns a shallow copy of this read-write map instance.
		 * Clones the internal structures like the superclass `R` does.
		 *
		 * @return A clone of this RW map instance.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		/**
		 * Sets the bit corresponding to the key in the `nulls` array (flat mode only).
		 * Marks the key as present. Assumes {@link #isFlatStrategy} is true.
		 *
		 * @param key The primitive key.
		 */
		protected final void exists1( char key ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		protected static void exists1( char key, long[] nulls ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		/**
		 * Clears the bit corresponding to the key in the `nulls` array (flat mode only).
		 * Marks the key as not present. Assumes {@link #isFlatStrategy} is true.
		 *
		 * @param key The primitive key.
		 */
		protected final void exists0( char key ) { nulls[ key >>> 6 ] &= ~( 1L << key ); }
	}
}