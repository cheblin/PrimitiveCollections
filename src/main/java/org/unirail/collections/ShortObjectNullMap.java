//MIT License
//
//Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//GitHub Repository: https://github.com/AdHoc-Protocol
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//1. The above copyright notice and this permission notice must be included in all
//   copies or substantial portions of the Software.
//
//2. Users of the Software must provide a clear acknowledgment in their user
//   documentation or other materials that their solution includes or is based on
//   this Software. This acknowledgment should be prominent and easily visible,
//   and can be formatted as follows:
//   "This product includes software developed by Chikirev Sirguy and the Unirail Group
//   (https://github.com/AdHoc-Protocol)."
//
//3. If you modify the Software and distribute it, you must include a prominent notice
//   stating that you have changed the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;


/**
 * A specialized map for mapping 2 bytes primitive keys (65,536 distinct values) to a nullable object values.
 * Implements a HYBRID strategy:
 * 1. Starts as a hash map with separate chaining, optimized for sparse data.
 * 2. Automatically transitions to a direct-mapped flat array when the hash map is full and reaches its capacity 0x7FFF entries (exclude nullKey entity) and needs to grow further.
 * This approach balances memory efficiency for sparse maps with guaranteed O(1) performance and full key range support for dense maps.
 */
public interface ShortObjectNullMap {
	
	/**
	 * Abstract base class providing read-only operations for the map.
	 * Handles the underlying structure which can be either a hash map or a flat array.
	 * Uses ObjectNullList to store values, allowing nulls efficiently.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		protected boolean                hasNullKey;          // Indicates if the map contains a null key
		protected V                      nullKeyValue;        // Value for the null key, stored separately.
		protected char[]                 _buckets;            // Hash table buckets array (1-based indices to chain heads).
		protected short[]                nexts;               // Links within collision chains (-1 termination, -2 unused, <-2 free list link).
		protected short[]         keys; // Keys array.
		protected ObjectNullList.RW< V > values;              // Values list, allowing nulls.
		protected int                    _count;              // Hash mode: Total slots used (entries + free slots). Flat mode: Number of set bits (actual entries).
		protected int                    _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int                    _freeCount;          // Number of free entries in the free list.
		protected int                    _version;            // Version counter for concurrent modification detection.
		
		/**
		 * Strategy for comparing values and calculating their hash codes.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		protected static final int StartOfFreeList = -3; // Marks the start of the free list in 'nexts' field.
		
		protected static final int VERSION_SHIFT  = 32; // Bits to shift version in token.
		// Special index used in tokens to represent the null key. Outside valid array index ranges.
		protected static final int NULL_KEY_INDEX = 0x1_FFFF; // 65537
		
		protected static final long INVALID_TOKEN = -1L; // Invalid token constant.
		
		
		/**
		 * Flag indicating if the map is operating in flat array mode.
		 */
		protected boolean isFlatStrategy() { return nulls != null; }
		
		/**
		 * Bitset to track presence of keys in flat mode. Size 1024 longs = 65536 bits.
		 */
		protected long[] nulls; // Size: 65536 / 64 = 1024
		
		// Constants for Flat Mode
		protected static final int FLAT_ARRAY_SIZE = 0x10000;
		protected static final int NULLS_SIZE      = FLAT_ARRAY_SIZE / 64; // 1024
		
		
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
					       isFlatStrategy() ?
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
			return isFlatStrategy() ?
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
		public boolean contains(  Short     key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive char key (0 to 65535).
		 * @return True if the key exists in the map.
		 */
		public boolean contains( short key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 * This operation can be slow (O(N)).
		 *
		 * @param value The value to search for. Can be null.
		 * @return True if the value exists in the map.
		 */
		public boolean containsValue( Object value ) {
			if( hasNullKey && Objects.equals( nullKeyValue, value ) ) return true;
			if( size() == ( hasNullKey ?
					1 :
					0 ) ) return false; // Adjusted check: only null key?
			
			if( isFlatStrategy() ) {
				for( int i = -1; ( i = next1( i ) ) != -1; ) {
					if( Objects.equals( values.get( i ), value ) ) return true; // Use values.get()
				}
			}
			else if( nexts != null ) {
				for( int i = 0; i < _count; i++ ) // Iterate up to _count, not nexts.length
					if( -2 < nexts[ i ] && Objects.equals( values.get( i ), value ) ) return true;
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
			if( isFlatStrategy() )
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
			if( isFlatStrategy() )
				return next1( index( token ) );
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
		
		
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		
		/**
		 * Checks if the entry associated with the given token has a non-null value.
		 * In a null-value context, this indicates if a value is explicitly set (not default null).
		 *
		 * @param token The token to check.
		 * @return {@code true} if the entry has a non-null value, {@code false} if it has a null value.
		 */
		public boolean hasValue( long token ) {
			final int idx = index( token );
			return idx == NULL_KEY_INDEX ?
					nullKeyValue != null :
					values.hasValue( idx ); // Check values != null before calling hasValue
		}
		
		
		/**
		 * Retrieves the key associated with a token.  Before calling this method ensure that this token is not point to the isKeyNull
		 *
		 * @param token The token representing a key-value pair.
		 * @return The char key associated with the token, or 0 for the null key token.
		 * @throws ArrayIndexOutOfBoundsException if the token index is invalid/out of bounds (excluding null key case).
		 */
		public short key( long token ) {
			return ( short ) (short) (
					isFlatStrategy() ?
							index( token ) :
							keys[ index( token ) ] );
		}
		
		
		/**
		 * Retrieves the value associated with a token.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The value associated with the token, or {@code null} if the token is invalid or represents the null key which holds a null value.
		 * @throws IndexOutOfBoundsException if the token index is invalid for the current mode/state (and not the null key token).
		 */
		public V value( long token ) {
			final int idx = index( token );
			return
					idx == NULL_KEY_INDEX ?
							nullKeyValue :
							values.get( idx );
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
			for( int token = unsafe_token( -1 ); token != INVALID_TOKEN; token = unsafe_token( token ) ) {
				if( isKeyNull( token ) ) continue; // Skip null key, handled below
				
				int keyHash = Array.hash( key( token ) ); // Use safe key() method
				V   val     = value( token );              // Use safe value() method
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
			    ( hasNullKey && !Objects.equals( nullKeyValue, other.nullKeyValue ) ) || size() != other.size() )
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
			
			if( isFlatStrategy() )
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( token ).value( values.get( token ) );
			else if( nexts != null ) // Ensure initialized
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( keys[ token ] ).value( values.get( token ) );
			
			
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
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index ); }
		
		/**
		 * Extracts index from a token.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts version from a token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
		/**
		 * Checks if a key is present in flat mode using the bitset. Assumes nulls is not null.
		 */
		protected final boolean exists( char key ) { return ( nulls[ key >>> 6 ] & 1L << key ) != 0; }
		
		
		/**
		 * Finds the next set bit >= 'bit' in the bitset
		 */
		public int next1( int bit ) { return next1( bit, nulls ); }
		
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
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
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
			_count = 0; // Flat mode _count tracks actual entries
			
			// Determine initial mode based on capacity hint
			if( capacity > flatStrategyThreshold ) {
				
				nulls    = new long[ NULLS_SIZE ]; // 1024 longs
				values   = new ObjectNullList.RW<>( equal_hash_V, FLAT_ARRAY_SIZE ); // Use ObjectNullList
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
			values     = new ObjectNullList.RW<>( equal_hash_V, capacity );// Use strategy for V[] creation
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
					put( ( short ) ( key + 0 ), value );
		}
		
		
		/**
		 * Associates the specified value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Uses ObjectNullList.set1().
		 *
		 * @param key   The primitive char key with which the specified value is to be associated.
		 * @param value The value (type V, can be null) to be associated with the specified key.
		 * @return True if the key was added, false if it already existed (and value was updated).
		 * @throws ConcurrentModificationException if excessive collisions are detected in hash mode.
		 */
		public boolean put( short key, V value ) {
			if( isFlatStrategy() ) {
				boolean b;
				if( b = !exists( ( char ) key ) ) {
					exists1( ( char ) key );
					_count++;
				}
				values.set1( ( char ) key, value );
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
					values.set1( next, value );
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
					if( isFlatStrategy() ) return put( key, value );
					
					bucket = ( ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1 );
				}
				index = _count++;
			}
			
			nexts[ index ] = ( short ) bucket;
			keys[ index ]  = ( short ) key;
			values.set1( index, value );
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
			if( isFlatStrategy() ) {
				if( exists( ( char ) key ) ) {
					V oldValue = values.get( key );
					if( oldValue != null ) values.set1( key, null );
					exists0( ( char ) key );
					_count--;
					_version++;
					return oldValue;
				}
				return null;
			}
			// Handle edge cases: if map is uninitialized or empty, nothing to remove
			if( _buckets == null || _count == 0 )
				return null; // Return invalid token indicating no removal
			
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
					
					V oldValue = values.get( i ); // Get the old value
					
					// Step 1: Unlink the entry at 'i' from its chain
					if( last < 0 )
						// If 'i' is the head, update bucket to point to the next entry
						_buckets[ bucketIndex ] = ( char ) ( next + 1 );
					
					else
						// Otherwise, link the previous entry to the next, bypassing 'i'
						nexts[ last ] = next;
					
					// Step 2: Optimize removal if value is non-null and not the last non-null
					final int lastNonNullValue = values.nulls.last1(); // Index of last non-null value
					
					// Update free list head
					if( values.hasValue( i ) )
						if( i != lastNonNullValue ) {
							// Optimization applies: swap with last non-null entry
							// Step 2a: Copy key, next, and value from lastNonNullValue to i
							short   keyToMove          = keys[ lastNonNullValue ];
							int            BucketOf_KeyToMove = bucketIndex( Array.hash( keyToMove ) );
							short          _next              = nexts[ lastNonNullValue ];
							
							keys[ i ]  = keyToMove;                         // Copy the key to the entry being removed
							nexts[ i ] = _next;         // Copy the next to the entry being removed
							values.set1( i, values.get( lastNonNullValue ) ); // Copy the value to the entry being removed
							
							// Step 2b: Update the chain containing keyToMove to point to 'i'
							int prev = -1;                     // Previous index in keyToMove’s chain
							collisionCount = 0;// Reset collision counter
							
							// Start at chain head
							for( int current = _buckets[ BucketOf_KeyToMove ] - 1; current != lastNonNullValue; prev = current, current = nexts[ current ] )
								if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
							
							if( -1 < prev ) nexts[ prev ] = ( short ) i; // Update next pointer of the previous entry
							else _buckets[ BucketOf_KeyToMove ] = ( char ) ( i + 1 );// If 'lastNonNullValue' the head, update bucket to the position of keyToMove
							
							
							values.set1( lastNonNullValue, null );        // Clear value (O(1) since it’s last non-null)
							i = lastNonNullValue; // Continue freeing operations at swapped position
						}
						else values.set1( i, null );                       // Clear value (may shift if not last)
					
					nexts[ i ] = ( short ) ( StartOfFreeList - _freeList ); // Mark 'i' as free and link to free list
					_freeList  = i; // Update free list head
					_freeCount++;       // Increment count of free entries
					_version++;         // Increment version for concurrency control
					return oldValue;    // Return the removed value
				}
				
				// Move to next entry in chain
				last = i;
				i    = next;
				if( collisionCount++ > nexts.length ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				
			}
			
			return null; // Key not found
		}
		
		/**
		 * Clears all mappings from the map. Resets to initial state (hash or flat depending on initial setup).
		 */
		public void clear() {
			_version++;
			
			hasNullKey = false;
			if( _count == 0 ) return;
			if( isFlatStrategy() )
				Array.fill( nulls, 0 );
			else {
				Arrays.fill( _buckets, ( char ) 0 );
				Arrays.fill( nexts, ( short ) 0 );
				
				_freeList  = -1;
				_freeCount = 0;
			}
			values.clear(); // Clear values list
			_count = 0;
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
			return !isFlatStrategy() && _buckets == null ?
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
			if( isFlatStrategy() ) {
				if( capacity <= flatStrategyThreshold ) resize( capacity );
				return;
			}
			
			short[]                old_next   = nexts;
			short[]         old_keys   = keys;
			ObjectNullList.RW< V > old_values = values.clone();
			int                    old_count  = _count;
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
			
			if( isFlatStrategy() ) {
				if( newSize > flatStrategyThreshold ) return length();
				
				_version++;
				ObjectNullList.RW< V > _values = values;
				long[]                 _nulls  = nulls;
				
				
				initialize( newSize );
				for( int token = -1; ( token = next1( token, _nulls ) ) != -1; )
				     put( ( short ) token, _values.get( token ) );
				
				return length();
			}
			
			_version++;
			if( flatStrategyThreshold < newSize ) {
				
				ObjectNullList.RW< V > _values = values;
				values = new ObjectNullList.RW<>( equal_hash_V, FLAT_ARRAY_SIZE );
				long[] nulls = new long[ NULLS_SIZE ];
				
				for( int i = -1; ( i = unsafe_token( i ) ) != -1; ) {
					char key = ( char ) keys[ i ];
					exists1( key , nulls);
					values.set1( key, _values.get( i ) );
				}
				
				this.nulls = nulls;
				
				_buckets = null;
				nexts    = null;
				keys     = null;
				
				_count -= _freeCount;
				
				_freeList  = -1;
				_freeCount = 0;
				return FLAT_ARRAY_SIZE;
			}
			
			
			short[]        new_next = Arrays.copyOf( nexts, newSize );
			short[] new_keys = Arrays.copyOf( keys, newSize );
			
			final int count = _count;
			
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
		 * Copies entries during trimming.
		 *
		 * @param old_nexts  Old hash_nexts array.
		 * @param old_keys   Old keys array.
		 * @param old_values Old values array.
		 * @param old_count  Old count.
		 */
		private void copy( short[] old_nexts, short[] old_keys, ObjectNullList.RW< V > old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_nexts[ i ] < -1 ) continue;
				
				keys[ new_count ] = old_keys[ i ];
				values.set1( new_count, old_values.get( i ) );
				;
				
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
		protected final void exists1( char key ) { nulls[ key >>> 6 ] |= 1L << key; }
		protected static void exists1( char key, long[] nulls ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		/**
		 * Clears a key's presence in flat mode using the bitset.
		 */
		protected final void exists0( char key ) { nulls[ key >>> 6 ] &= ~( 1L << key ); }
		
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