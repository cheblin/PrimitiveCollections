// Copyright 2025 Chikirev Sirguy, Unirail Group
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.Arrays;
import java.util.ConcurrentModificationException;

/**
 * A specialized map for mapping primitive `int` keys to primitive `int` values,
 * with built-in support for mapping keys to a conceptual "null" value.
 * <p>
 * This implementation uses a memory-efficient, dual-region open-addressing hash
 * table. It distinguishes between entries based on their collision status to
 * optimize performance and memory usage. A `long[]` bitset is used to track
 * which entries hold a meaningful value versus a conceptual null.
 *
 * <ul>
 * <li><b>{@code lo Region}:</b> Stores entries that are part of a hash collision chain.
 *     It occupies the low-indexed start of the internal `keys`, `values`, and `nulls`
 *     arrays (from `0` to `_lo_Size - 1`). An explicit `links` array is used for chaining.</li>
 *
 * <li><b>{@code hi Region}:</b> Stores entries that, at insertion time, do not cause a collision.
 *     It occupies the high-indexed end of the internal arrays (from `keys.length - _hi_Size`
 *     to `keys.length - 1`). Entries in this region do not require an initial `links` slot.
 *     They can, however, become the terminal node of a collision chain originating in the `lo Region`.</li>
 * </ul>
 *
 * <h3>Insertion</h3>
 * When a new key-value pair is added:
 * <ol>
 * <li>If the key already exists, its value and null-status are updated.</li>
 * <li>If the key is new:
 *     <ul>
 *     <li><b>If its hash maps to an empty bucket:</b> The new entry is added to the {@code hi Region}.</li>
 *     <li><b>If its hash maps to an occupied bucket (a collision):</b> The new entry is added to the
 *         {@code lo Region} and becomes the new head of the collision chain.</li>
 *     </ul>
 * </li>
 * </ol>
 *
 * <h3>Removal</h3>
 * When an entry is removed:
 * <ol>
 * <li>The entry is located and its collision chain is repaired by updating pointers in `_buckets` or `links`.</li>
 * <li>To maintain density for fast iteration, the affected region (`lo` or `hi`) is compacted. The last
 *     logical entry from that region is moved into the vacated slot. All pointers to the moved entry are updated.</li>
 * <li>A special optimization handles removing a `hi Region` entry that terminates a `lo Region` chain,
 *     by efficiently moving the predecessor `lo Region` entry into the vacated `hi Region` slot.</li>
 * </ol>
 *
 * <h3>Iteration</h3>
 * Iteration is highly efficient because both regions are kept dense. The iterator first
 * scans the `lo Region` and then the `hi Region`.
 */
public interface FloatUIntNullMap {
	
	/**
	 * An abstract base class providing read-only operations for the dual-region hash map.
	 * It implements the core functionalities and state management, offering methods for
	 * querying the map, iteration, and serialization without allowing modification.
	 *
	 * @see IntIntNullMap for a detailed explanation of the internal strategy.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * `true` if the map contains a mapping for the conceptual null key.
		 */
		protected boolean                hasNullKey;
		/**
		 * `true` if the conceptual null key maps to a non-null value. This is only meaningful
		 * if {@link #hasNullKey} is `true`.
		 */
		protected boolean                nullKeyHasValue;
		/**
		 * The primitive value associated with the conceptual null key. This is only meaningful
		 * if {@link #nullKeyHasValue} is `true`.
		 */
		protected int            nullKeyValue;
		/**
		 * The hash table. Stores 1-based indices into the `keys` array, where `_buckets[i] - 1`
		 * is the 0-based index of the head of a collision chain. A value of `0` indicates an empty bucket.
		 */
		protected int[]                  _buckets;
		/**
		 * An array for collision chaining. For an entry at index `i` in the `lo Region`,
		 * `links[i]` stores the 0-based index of the next element in its collision chain.
		 */
		protected int[]                  links= Array.EqualHashOf._ints.O;
		/**
		 * Stores the primitive keys of the map. This array is logically divided into a `lo Region`
		 * (for collision-involved entries) and a `hi Region` (for non-colliding entries).
		 */
		protected float[]          keys;
		
		/**
		 * Stores the primitive values corresponding to the keys at the same indices. If an entry's
		 * value is conceptually null, the content of this array at that index is undefined.
		 */
		protected int[] values;
		/**
		 * A bitset used to track whether an entry has a non-null value. For an entry at index `i`,
		 * if the `i`-th bit is set in this bitset, `values[i]` holds a meaningful value. Otherwise,
		 * the entry's value is conceptually null.
		 */
		protected long[]        nulls;
		
		
		/**
		 * Returns the total number of non-null key entries currently stored in the map.
		 * This is the sum of entries in the `lo` and `hi` regions.
		 *
		 * @return The current count of non-null key entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * A version counter used for fail-fast iteration. It is incremented upon any
		 * structural modification to the map (put, remove, clear, resize).
		 */
		protected int _version;
		
		/**
		 * The number of active entries in the `lo Region`, which occupies indices from `0` to `_lo_Size - 1`.
		 */
		protected int _lo_Size;
		/**
		 * The number of active entries in the `hi Region`, which occupies indices from `keys.length - _hi_Size`
		 * to `keys.length - 1`.
		 */
		protected int _hi_Size;
		
		/**
		 * The number of bits to shift the version value when packing it into a `long` token.
		 */
		protected static final int VERSION_SHIFT = 32;
		
		/**
		 * A special index value used in tokens to represent the conceptual null key. This value is outside
		 * the valid range of array indices.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * A constant representing an invalid or non-existent token, returned when a key is not found
		 * or at the end of an iteration.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		
		/**
		 * Returns `true` if this map contains no key-value mappings.
		 *
		 * @return `true` if this map is empty.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the total number of key-value mappings in this map, including the conceptual null key if present.
		 *
		 * @return The number of mappings in this map.
		 */
		public int size() {
			return _count() + (
					hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the total number of key-value mappings in this map. Alias for {@link #size()}.
		 *
		 * @return The number of mappings.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the total allocated capacity of the map's internal arrays.
		 * This represents the maximum number of non-null key entries the map can hold
		 * before a resize is triggered.
		 *
		 * @return The current capacity.
		 */
		public int length() {
			return
					keys == null ?
					0 :
					keys.length;
		}
		
		
		/**
		 * Returns `true` if this map contains a mapping for the specified boxed key.
		 *
		 * @param key The key to check for (can be null).
		 * @return `true` if a mapping for the key exists.
		 */
		public boolean containsKey(  Float     key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Returns `true` if this map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive key to check for.
		 * @return `true` if a mapping for the key exists.
		 */
		public boolean containsKey( float key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Returns `true` if this map maps one or more keys to the specified boxed value.
		 * A `null` argument will search for keys mapped to a conceptual null value.
		 *
		 * @param value The value to search for (can be null).
		 * @return `true` if the value is found in the map.
		 */
		public boolean containsValue(  Long      value ) {
			if( value != null ) return containsValue( ( long ) ( value + 0 ) );
			
			if( hasNullKey && !nullKeyHasValue ) return true;
			
			if( 0 < _lo_Size )
				for( int nulls_idx = 0; ; nulls_idx++ ) {
					int remaining = _lo_Size - ( nulls_idx << 6 );
					if( remaining < 64 )
						if( ( nulls[ nulls_idx ] | -1L << remaining ) == -1 ) break;
						else return true;
					
					if( nulls[ nulls_idx ] != -1L ) return true;
				}
			
			if( _hi_Size == 0 ) return false;
			
			int  hi_start  = keys.length - _hi_Size;
			int  nulls_idx = hi_start >> 6;
			long bits      = nulls[ nulls_idx ];
			
			int bit = hi_start & 63;
			if( 0 < bit ) bits &= -1L << bit;
			
			for( int remaining; ; bits = nulls[ ++nulls_idx ] ) {
				if( nulls_idx == nulls.length )
					if( 0 < ( remaining = keys.length & 63 ) )
						return ( nulls[ nulls_idx ] | -1L << remaining ) != -1;
				
				if( bits != -1L ) return true;
			}
		}
		
		/**
		 * Returns `true` if this map maps one or more keys to the specified primitive value.
		 * This method only searches for non-null values.
		 *
		 * @param value The primitive value to search for.
		 * @return `true` if the value is found.
		 */
		public boolean containsValue( long value ) {
			if( hasNullKey && nullKeyHasValue && nullKeyValue == value ) return true;
			
			if( _count() == 0 ) return false;
			
			int  nulls_idx = 0;
			long bits      = 0;
			
			for( int i = 0; i < _lo_Size; i++ ) {
				if( nulls_idx != i >> 6 ) bits = nulls[ nulls_idx = i >> 6 ];
				if( ( bits & 1L << i ) != 0 && values[ i ] == value ) return true;
			}
			
			for( int i = keys.length - _hi_Size; i < keys.length; i++ ) {
				if( nulls_idx != i >> 6 ) bits = nulls[ nulls_idx = i >> 6 ];
				if( ( bits & 1L << i ) != 0 && values[ i ] == value ) return true;
			}
			
			return false;
		}
		
		
		/**
		 * Returns a token representing the location of the specified boxed key.
		 * If the key is not found, returns {@link #INVALID_TOKEN}. The null key is handled correctly.
		 *
		 * @param key The boxed key to find (can be null).
		 * @return A valid token if the key is in the map, otherwise {@link #INVALID_TOKEN}.
		 */
		public long tokenOf(  Float     key ) {
			return key == null ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       INVALID_TOKEN :
			       tokenOf( ( float ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a "token" representing the location of the specified primitive key.
		 * This token can be used for fast access to the key and value, and includes a
		 * version stamp to detect concurrent modifications.
		 *
		 * @param key The primitive key to find.
		 * @return A `long` token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected while
		 *                                         traversing a collision chain.
		 */
		public long tokenOf( float key ) {
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			int index = _buckets[ bucketIndex( Array.hash( key ) ) ] - 1;
			if( index < 0 ) return INVALID_TOKEN;
			
			for( int collisions = 0; ; ) {
				if( keys[ index ] == key ) return token( index );
				if( _lo_Size <= index ) return INVALID_TOKEN;
				index = links[ index ];
				if( _lo_Size < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
		}
		
		
		/**
		 * Returns the token for the first entry in the iteration order.
		 * Subsequent entries can be accessed by passing the returned token to {@link #token(long)}.
		 *
		 * @return The token for the first entry, or {@link #INVALID_TOKEN} if the map is empty.
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
		 * Returns the token for the entry that follows the one specified by the given token.
		 * This method is used to iterate through the map.
		 * <p>
		 * Iteration order is: all entries in the `lo Region`, then all entries in the `hi Region`,
		 * and finally the conceptual null key mapping, if present.
		 *
		 * @param token The current token from a previous call to {@link #token()} or {@link #token(long)}.
		 * @return The token for the next entry, or {@link #INVALID_TOKEN} if there are no more entries.
		 * @throws IllegalArgumentException        if the input token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the map was structurally modified after the token was issued.
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
		 * Returns the next valid internal array index for fast, unsafe iteration.
		 * This method scans the dense `lo` and `hi` regions, bypassing concurrency checks.
		 * It **excludes** the conceptual null key.
		 *
		 * <p><strong>⚠️ WARNING: UNSAFE ⚠️</strong>
		 * This method is for performance-critical use where no concurrent modifications are guaranteed.
		 * Structural changes to the map during iteration can lead to undefined behavior. For safe
		 * iteration, use the standard `token()` and `token(long)` methods.
		 *
		 * @param token The index from the previous call, or `-1` to start iteration.
		 * @return The index of the next entry, or `-1` if iteration is complete.
		 */
		public int unsafe_token( final int token ) {
			if( _count() == 0 || keys == null || keys.length - 1 <= token ) return -1;
			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size;
			return i < _lo_Size ?
			       i :
			       i < lowest_hi ?
			       _hi_Size == 0 ?
			       -1 :
			       lowest_hi :
			       i < keys.length ?
			       i :
			       -1;
		}
		
		
		/**
		 * Returns `true` if this map contains a mapping for the conceptual null key.
		 *
		 * @return `true` if the null key is present.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns `true` if the conceptual null key is present and maps to a non-null value.
		 *
		 * @return `true` if the null key maps to a non-null value.
		 */
		public boolean nullKeyHasValue() { return hasNullKey && nullKeyHasValue; }
		
		/**
		 * Returns the primitive value to which the conceptual null key is mapped.
		 * <p>
		 * <b>Precondition:</b> Callers should first check {@link #nullKeyHasValue()} to ensure
		 * this method returns a meaningful value. If the null key maps to a conceptual null,
		 * the returned primitive value is undefined (typically `0`).
		 *
		 * @return The primitive value for the null key.
		 */
		public long nullKeyValue() { return (0xFFFFFFFFL &  nullKeyValue); }
		
		
		/**
		 * Returns `true` if the given token represents the conceptual null key.
		 *
		 * @param token The token to check.
		 * @return `true` if the token represents the null key.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the primitive key associated with the given token.
		 *
		 * @param token The token for a non-null key entry. Must be valid.
		 * @return The primitive key at the token's location.
		 */
		public float key( long token ) {
			return ( float )  keys[ index( token ) ];
		}
		
		/**
		 * Returns `true` if the entry associated with the given token has a non-null value.
		 *
		 * @param token The token to check, which can represent a non-null key or the conceptual null key.
		 * @return `true` if the entry's value is not conceptually null.
		 */
		public boolean hasValue( long token ) {
			return isKeyNull( token ) ?
			       nullKeyHasValue :
			       ( nulls[ index( token ) >> 6 ] & 1L << index( token ) ) != 0;
		}
		
		/**
		 * Returns the primitive value associated with the specified token.
		 * <p>
		 * <b>Precondition:</b> Callers should first check {@link #hasValue(long)} for the given
		 * token to ensure this method returns a meaningful value. If the entry's value is
		 * conceptually null, the returned primitive value is undefined (typically `0`).
		 *
		 * @param token The token for which to retrieve the value.
		 * @return The primitive value associated with the token.
		 */
		public long value( long token ) {
			return (0xFFFFFFFFL & ( isKeyNull( token ) ?
			                   nullKeyValue :
			                   values[ index( token ) ] ));
		}
		
		
		/**
		 * Computes the hash code for this map. The hash code is the sum of the hash codes
		 * of each key-value entry in the map, including the conceptual null key if present.
		 *
		 * @return The hash code value for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( keys[ token ] ) );
				h = Array.mix( h, Array.hash( value( token ) ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		/**
		 * A static seed used in `hashCode` calculation to improve hash distribution.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map with the specified object for equality. Returns `true` if the
		 * object is also an `IntIntNullMap.R`, the two maps have the same size, and every
		 * mapping in this map is equal to a mapping in the other map.
		 *
		 * @param obj the object to be compared for equality with this map.
		 * @return `true` if the specified object is equal to this map.
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		
		/**
		 * Compares this map with another `IntIntNullMap.R` for equality.
		 *
		 * @param other the other map to compare against.
		 * @return `true` if the maps contain the exact same key-value mappings, respecting null values.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null ||
			    hasNullKey != other.hasNullKey ||
			    hasNullKey && nullKeyValue != other.nullKeyValue || size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || value( token ) != other.value( t ) ) return false;
			}
			return true;
		}
		
		
		/**
		 * Creates and returns a deep copy of this map. The internal arrays are cloned,
		 * making the new map independent of the original.
		 *
		 * @return A deep copy of this map.
		 * @throws InternalError if cloning fails, which should not happen.
		 */
		@Override
		public R clone() {
			try {
				R cloned = ( R ) super.clone();
				
				if( _buckets != null ) cloned._buckets = _buckets.clone();
				if( links != null ) cloned.links = links.clone();
				if( keys != null ) cloned.keys = keys.clone();
				if( values != null ) cloned.values = values.clone();
				if( nulls != null ) cloned.nulls = nulls.clone();
				
				return cloned;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e );
			}
		}
		
		/**
		 * Returns a string representation of the map in JSON object format (e.g., `{"1":10, "42":null}`).
		 *
		 * @return A JSON string representing the map.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the contents of this map to a {@link JsonWriter} as a JSON object.
		 * The conceptual null key is written as the JSON string `"null"`. Keys mapped to
		 * a conceptual null value are written as JSON `null`.
		 *
		 * @param json The {@link JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     json.name( keys[ token ] ).value( hasValue( token ) ?
			                                       value( token ) :
			                                       null );
			
			json.exitObject();
		}
		
		/**
		 * Computes the bucket index for a given key's hash code.
		 *
		 * @param hash The hash code of a key.
		 * @return The index in the `_buckets` array.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		
		/**
		 * Packs an internal array index and the current map version into a single `long` token.
		 * The version is stored in the high 32 bits and the index in the low 32 bits.
		 *
		 * @param index The 0-based index of the entry (or {@link #NULL_KEY_INDEX} for the null key).
		 * @return A versioned `long` token representing the entry.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index; }
		
		
		/**
		 * Extracts the 0-based internal array index from a token.
		 *
		 * @param token The `long` token.
		 * @return The 0-based index.
		 */
		protected int index( long token ) { return ( int ) token; }
		
		
		/**
		 * Extracts the version stamp from a token.
		 *
		 * @param token The `long` token.
		 * @return The version stamp.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
	}
	
	
	/**
	 * A read-write implementation of the dual-region hash map, extending the read-only base class {@link R}.
	 * It provides methods for modifying the map, such as adding and removing entries, clearing the map,
	 * and managing its capacity.
	 */
	class RW extends R {
		
		
		/**
		 * Constructs an empty map with a default initial capacity.
		 */
		public RW() { this( 0 ); }
		
		
		/**
		 * Constructs an empty map with a specified initial capacity hint.
		 *
		 * @param capacity The initial capacity. The map will be able to hold at least this many
		 *                 elements before needing to resize.
		 */
		public RW( int capacity ) {
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		
		/**
		 * Initializes or re-initializes the internal hash map arrays with the given capacity.
		 * This is a structural modification and increments the version counter.
		 *
		 * @param capacity The desired capacity for the new internal arrays.
		 * @return The actual allocated capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			
			_buckets = new int[ capacity ];
			keys     = new float[ capacity ];
			values   = new int[ capacity ];
			nulls    = new long[ capacity + 63 >> 6 ];
			_lo_Size = 0;
			_hi_Size = 0;
			return length();
		}
		
		/**
		 * Associates the conceptual null key with a conceptual null value.
		 * If the null key was not present, it is added. If it was present with a non-null
		 * value, its value is changed to be conceptually null.
		 *
		 * @return `true` if the map's state changed (key added or value status changed).
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
		 * Associates the conceptual null key with the specified boxed value.
		 * A `null` value will map the key to a conceptual null.
		 *
		 * @param value The value to associate with the null key (can be null).
		 * @return `true` if the map's state changed.
		 */
		public boolean put(  Long      value ) {
			return value == null ?
			       put() :
			       put( value );
		}
		
		/**
		 * Associates the specified boxed value with the specified boxed key.
		 * Handles `null` for both key and value.
		 *
		 * @param key   The key to map (can be null).
		 * @param value The value to map (can be null).
		 * @return `true` if the map's state changed (key added or value updated).
		 */
		public boolean put(  Float     key,  Long      value ) {
			return key == null ?
			       value == null ?
			       put() :
			       putNullKey( value ) :
			       value == null ?
			       putNullValue( ( float ) ( key + 0 ) ) :
			       put( ( float ) ( key + 0 ), ( long ) ( value + 0 ), true );
		}
		
		/**
		 * Associates the specified boxed value with the specified primitive key.
		 *
		 * @param key   The primitive key to map.
		 * @param value The value to map (can be null).
		 * @return `true` if the map's state changed.
		 */
		public boolean put( float key,  Long      value ) {
			return value == null ?
			       putNullValue( ( float ) ( key + 0 ) ) :
			       put( ( float ) ( key + 0 ), ( long ) ( value + 0 ), true );
		}
		
		
		/**
		 * Associates the specified primitive value with the conceptual null key.
		 *
		 * @param value The value to associate.
		 * @return `true` if a new mapping for the null key was created.
		 */
		public boolean put( long value ) {
			_version++;
			nullKeyHasValue = true;
			
			if( hasNullKey ) {
				nullKeyValue = ( int ) value;
				return false;
			}
			
			nullKeyValue = ( int ) value;
			return hasNullKey = true;
		}
		
		
		/**
		 * Private helper to associate a value with the conceptual null key.
		 *
		 * @param value    The primitive value.
		 * @param hasValue `true` to map to a non-null value, `false` for conceptual null.
		 * @return `true` if the null key was newly added.
		 */
		private boolean put( long value, boolean hasValue ) {
			boolean b = !hasNullKey;
			
			hasNullKey = true;
			if( nullKeyHasValue = hasValue ) nullKeyValue = ( int ) value;
			_version++;
			return b;
		}
		
		/**
		 * Associates the specified primitive value with the conceptual null key.
		 *
		 * @param value The primitive value to associate.
		 * @return `true` if the map's state changed.
		 */
		public boolean putNullKey( long  value ) { return put( value, true ); }
		
		/**
		 * Associates a conceptual null value with the specified primitive key.
		 *
		 * @param key The primitive key.
		 * @return `true` if the map's state changed (key added or value status changed).
		 */
		public boolean putNullValue( float key ) { return put( key, ( long ) 0, false ); }
		
		/**
		 * Associates the specified primitive value with the specified primitive key.
		 *
		 * @param key   The primitive key.
		 * @param value The primitive value.
		 * @return `true` if a new mapping was created.
		 */
		public boolean put( float key, long value ) { return put( key, value, true ); }
		
		/**
		 * The core method for adding or updating a key-value pair. It handles the
		 * dual-region hashing logic and updates the `keys`, `values`, and `nulls` arrays.
		 *
		 * @param key      The primitive key.
		 * @param value    The primitive value.
		 * @param hasValue `true` if mapping to a non-null value, `false` for conceptual null.
		 * @return `true` if a new key was added, `false` if an existing key's value was updated.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected during
		 *                                         collision chain traversal.
		 */
		private boolean put( float key, long value, boolean hasValue ) {
			
			
			if( _buckets == null ) initialize( 7 );
			else if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ) );
			
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 )
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				for( int i = index, collisions = 0; ; ) {
					if( keys[ i ] == ( float ) key ) {
						if( hasValue ) {
							values[ i ] = ( int ) value;
							nulls[ i >> 6 ] |= 1L << i;
						}
						else
							nulls[ i >> 6 ] &= ~( 1L << i );
						
						_version++;
						return false;
					}
					if( _lo_Size <= i ) break;
					i = links[ i ];
					
					if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
				
				( links.length == ( dst_index = _lo_Size++ ) ?
				  links = Arrays.copyOf( links, Math.max( 16, Math.min( _lo_Size * 2, keys.length ) ) ) :
				  links )[ dst_index ] = index; // New entry points to the old head
			}
			
			keys[ dst_index ] = ( float ) key;
			if( hasValue ) {
				values[ dst_index ] = ( int ) value;
				nulls[ dst_index >> 6 ] |= 1L << dst_index;
			}
			else
				nulls[ dst_index >> 6 ] &= ~( 1L << dst_index );
			
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
			_version++;
			return true;
		}
		
		
		/**
		 * Removes the mapping for the specified boxed key from this map if present.
		 *
		 * @param key The key whose mapping is to be removed (can be null).
		 * @return `true` if a mapping was removed.
		 */
		public boolean remove(  Float     key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( float ) ( key + 0 ) );
		}
		
		
		/**
		 * Removes the mapping for the conceptual null key from this map if present.
		 *
		 * @return `true` if the null key mapping was removed.
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey      = false;
			nullKeyHasValue = false;
			_version++;
			return true;
		}
		
		/**
		 * Moves an entry (key, value, and null-status bit) from a source index to a destination index.
		 * This is a core part of the compaction process during removal. After moving the data,
		 * it finds the pointer (in `_buckets` or `links`) that referenced `src` and updates
		 * it to point to `dst`, maintaining hash chain integrity.
		 *
		 * @param src The source index of the entry to move.
		 * @param dst The destination index for the entry.
		 */
		private void move( int src, int dst ) {
			
			if( src == dst ) return;
			int bucketIndex = bucketIndex( Array.hash( keys[ src ] ) );
			int index       = _buckets[ bucketIndex ] - 1;
			
			if( index == src ) _buckets[ bucketIndex ] = ( int ) ( dst + 1 );
			else {
				while( links[ index ] != src )
					index = links[ index ];
				
				links[ index ] = ( int ) dst;
			}
			
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			keys[ dst ] = keys[ src ];
			
			if( ( nulls[ src >> 6 ] & 1L << src ) != 0 ) {
				values[ dst ] = values[ src ];
				nulls[ dst >> 6 ] |= 1L << dst;
			}
			else
				nulls[ dst >> 6 ] &= ~( 1L << dst );
		}
		
		
		/**
		 * Removes the mapping for the specified primitive key from this map if it is present.
		 * <p>
		 * After unlinking the entry from its collision chain, the data region (`lo` or `hi`)
		 * is compacted by moving the last logical element from that region into the vacated slot.
		 * The {@link #move(int, int)} helper ensures all pointers to the moved element are updated correctly.
		 * This method also handles a special optimization for removing a `hi Region` entry that
		 * terminates a `lo Region` chain.
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return `true` if a mapping was removed.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected during
		 *                                         collision chain traversal.
		 */
		public boolean remove( float key ) {
			
			
			if( _count() == 0 ) return false;
			int removeBucketIndex = bucketIndex( Array.hash( key ) );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1;
			if( removeIndex < 0 ) return false;
			
			if( _lo_Size <= removeIndex ) {
				
				if( keys[ removeIndex ] != key ) return false;
				
				move( keys.length - _hi_Size, removeIndex );
				_hi_Size--;
				_buckets[ removeBucketIndex ] = 0;
				_version++;
				return true;
			}
			
			int next = links[ removeIndex ];
			if( keys[ removeIndex ] == key ) _buckets[ removeBucketIndex ] = ( int ) ( next + 1 );
			else {
				int last = removeIndex;
				if( keys[ removeIndex = next ] == key )
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
					else {
						keys[ removeIndex ] = keys[ last ];
						
						if( ( nulls[ last >> 6 ] & 1L << last ) != 0 ) {
							values[ removeIndex ] = values[ last ];
							nulls[ removeIndex >> 6 ] |= 1L << removeIndex;
						}
						else
							nulls[ removeIndex >> 6 ] &= ~( 1L << removeIndex );
						
						
						_buckets[ removeBucketIndex ] = ( int ) ( removeIndex + 1 );
						removeIndex                   = last;
					}
				else if( _lo_Size <= removeIndex ) return false;
				else
					for( int collisions = 0; ; ) {
						int prev = last;
						
						if( keys[ removeIndex = links[ last = removeIndex ] ] == key ) {
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else {
								keys[ removeIndex ] = keys[ last ];
								if( ( nulls[ last >> 6 ] & 1L << last ) != 0 ) {
									values[ removeIndex ] = values[ last ];
									nulls[ removeIndex >> 6 ] |= 1L << removeIndex;
								}
								else
									nulls[ removeIndex >> 6 ] &= ~( 1L << removeIndex );
								
								links[ prev ] = ( int ) removeIndex;
								removeIndex   = last;
							}
							break;
						}
						if( _lo_Size <= removeIndex ) return false;
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
			}
			
			move( _lo_Size - 1, removeIndex );
			_lo_Size--;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes all mappings from this map. The map will be empty after this call, but its
		 * allocated capacity will remain unchanged. This is a structural modification.
		 */
		public void clear() {
			_version++;
			
			hasNullKey      = false;
			nullKeyHasValue = false;
			
			if( _count() == 0 ) return;
			if( _buckets != null ) Arrays.fill( _buckets, ( int ) 0 );
			_lo_Size = 0;
			_hi_Size = 0;
			Arrays.fill( nulls, 0 );
		}
		
		
		/**
		 * Ensures that this map can hold at least the specified number of non-null entries
		 * without needing to resize. If necessary, the internal arrays are resized.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new capacity of the map.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			return _buckets == null ?
			       initialize( capacity ) :
			       resize( Array.prime( capacity ) );
		}
		
		
		/**
		 * Trims the capacity of this map's internal arrays to be the map's current size.
		 * This can be used to minimize the memory footprint of the map. This is a structural
		 * modification if the capacity is reduced.
		 */
		public void trim() { trim( size() ); }
		
		/**
		 * Trims the capacity of this map's internal arrays to the specified capacity,
		 * provided it is not less than the current size.
		 *
		 * @param capacity The desired capacity, which must be at least the current size.
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			if( length() <= ( capacity = Array.prime( Math.max( capacity, size() ) ) ) ) return;
			
			resize( capacity );
			
			if( _lo_Size < links.length )
				links = _lo_Size == 0 ?
				        Array.EqualHashOf._ints.O :
				        Array.copyOf( links, _lo_Size );
		}
		
		
		/**
		 * Resizes the internal hash map arrays to a new capacity. All existing entries are
		 * rehashed and inserted into the new arrays, rebuilding the `lo` and `hi` regions.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resizing.
		 */
		private int resize( int newSize ) {
			_version++;
			
			float[] old_keys    = keys;
			long[]        old_nulls   = nulls;
			int[] old_values  = values;
			int           old_lo_Size = _lo_Size;
			int           old_hi_Size = _hi_Size;
			if( links.length < 0xFF && links.length < _buckets.length ) links = _buckets;//reuse buckets as links
			initialize( newSize );
			
			long bits = 0;
			int  b    = 0;
			
			
			for( int i = 0; i < old_lo_Size; i++ )
				if( ( ( i < b << 6 ?
				        bits :
				        ( bits = old_nulls[ b++ >> 6 ] ) ) & 1L << i ) != 0 )
					copy( ( float ) old_keys[ i ], ( int ) old_values[ i ], true );
				else
					copy( ( float ) old_keys[ i ], ( int ) 0, false );
			
			
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
				if( ( ( i < b << 6 ?
				        bits :
				        ( bits = old_nulls[ b++ >> 6 ] ) ) & 1L << i ) != 0 )
					copy( ( float ) old_keys[ i ], ( int ) old_values[ i ], true );
				else
					copy( ( float ) old_keys[ i ], ( int ) 0, false );
			
			return length();
		}
		
		/**
		 * An internal helper for efficiently copying an entry into the new hash table
		 * structure during a resize operation. It performs the insertion logic without
		 * checking for duplicates, as it assumes all keys from the old map are unique.
		 *
		 * @param key      The primitive key to copy.
		 * @param value    The primitive value to copy.
		 * @param hasValue `true` if mapping to a non-null value, `false` for conceptual null.
		 */
		private void copy( float key, int value, boolean hasValue ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 )
				dst_index = keys.length - 1 - _hi_Size++;
			else ( links.length == _lo_Size ?
				  links = Arrays.copyOf( links, Math.max( 16, Math.min( _lo_Size * 2, keys.length ) ) ) :
				  links )[ dst_index = _lo_Size++ ] = ( char ) ( index );
			
			keys[ dst_index ] = key;
			if( hasValue ) {
				values[ dst_index ] = value;
				nulls[ dst_index >> 6 ] |= 1L << ( dst_index & 63 );
			}
			
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
		}
		
		/**
		 * Creates and returns a deep copy of this read-write map.
		 *
		 * @return A cloned instance of this map.
		 * @throws InternalError if cloning fails.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		
	}
}