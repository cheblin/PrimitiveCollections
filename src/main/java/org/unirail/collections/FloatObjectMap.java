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
 * A specialized map for mapping primitive `int` keys to object values, using a memory-efficient,
 * dual-region open-addressing hash table. This implementation is optimized for
 * performance by separating entries based on their collision status.
 *
 * <h3>Dual-Region Hashing Strategy</h3>
 * The core of this map is a single `keys` array (and a parallel `values` array)
 * divided into two logical regions to store entries. This minimizes memory overhead,
 * especially for the `links` array used for collision chaining.
 *
 * <ul>
 * <li><b>{@code hi Region}:</b> Occupies the high-indexed end of the `keys` and `values` arrays.
 *     It stores entries that, at insertion time, map to an empty hash bucket.
 *     This region grows downwards from `keys.length - 1`. Entries in this region
 *     do not initially require a slot in the `links` array, saving memory. They can,
 *     however, become the terminal node of a collision chain originating in the `lo Region`.</li>
 *
 * <li><b>{@code lo Region}:</b> Occupies the low-indexed start of the `keys` and `values` arrays.
 *     It stores entries that are part of a collision chain. This includes newly
 *     inserted keys that hash to an already occupied bucket. Every entry in this region
 *     has a corresponding slot in the `links` array to point to the next entry in its
 *     collision chain. This region grows upwards from index `0`.</li>
 * </ul>
 *
 * <h3>Insertion</h3>
 * When a new key-value pair is added:
 * <ol>
 * <li>If the key already exists, its associated value is updated.</li>
 * <li>If the key is new, its hash is computed to find a bucket in the {@code _buckets} table.
 *     <ul>
 *     <li><b>If the bucket is empty:</b> The new entry is added to the {@code hi Region}. The bucket
 *         is updated to point to this new entry.</li>
 *     <li><b>If the bucket is occupied (a collision):</b> The new entry is added to the
 *         {@code lo Region} and becomes the new head of the collision chain. Its link in the
 *         `links` array points to the previous chain head, and the bucket is updated to point
 *         to this new entry in the `lo Region`.</li>
 *     </ul>
 * </li>
 * </ol>
 *
 * <h3>Removal</h3>
 * When a key is removed:
 * <ol>
 * <li>The entry is located in either the `lo` or `hi` region.</li>
 * <li>The collision chain is repaired by updating the bucket pointer or the predecessor's
 *     link in the `links` array to bypass the removed entry.</li>
 * <li>To maintain density and enable fast iteration, the region is compacted. The last
 *     logical entry from the affected region (`lo` or `hi`) is moved into the slot
 *     vacated by the removed entry. All pointers to the moved entry are updated to its new location.</li>
 * </ol>
 *
 * <h3>Iteration</h3>
 * Iteration is highly efficient because both regions are kept dense. The iterator first
 * scans the `lo Region` from index `0` to `_lo_Size - 1`, and then scans the `hi Region`
 * from `keys.length - _hi_Size` to `keys.length - 1`.
 */
public interface FloatObjectMap {
	
	/**
	 * An abstract base class providing read-only operations for the dual-region hash map.
	 * It implements the core functionalities and state management, offering methods for
	 * querying the map, iteration, and serialization without allowing modification.
	 *
	 * @param <V> The type of values stored in the map.
	 * @see IntObjectMap for a detailed explanation of the internal dual-region hashing strategy.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		/**
		 * Tracks whether the map contains a mapping for the null key.
		 */
		protected boolean       hasNullKey;
		/**
		 * The value associated with the null key, stored separately.
		 */
		protected V             nullKeyValue;
		/**
		 * The hash table. Stores 1-based indices into the `keys` array, where `_buckets[i] - 1`
		 * is the 0-based index of the head of a collision chain. A value of `0` indicates an empty bucket.
		 */
		protected int[]         _buckets;
		/**
		 * An array used for collision chaining. For an entry at index `i` in the `lo Region`,
		 * `links[i]` stores the 0-based index of the next element in the same collision chain.
		 */
		protected int[]         links;
		/**
		 * Stores the primitive keys of the map. This array is logically divided into a `lo Region`
		 * (for collision-involved entries) and a `hi Region` (for non-colliding entries).
		 */
		protected float[] keys;
		/**
		 * Stores the object values corresponding to the keys at the same indices.
		 */
		protected V[]           values;
		
		
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
		 * The number of bits to shift the version value when packing it into a `long` token.
		 */
		protected static final int VERSION_SHIFT  = 32;
		/**
		 * A special index value used in tokens to represent the null key. This value is outside
		 * the valid range of array indices.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * A constant representing an invalid or non-existent token, returned when a key is not found
		 * or at the end of an iteration.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * A strategy object for comparing and hashing the map's values. Allows for custom
		 * equality logic (e.g., identity vs. `equals`).
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		
		/**
		 * Constructs a new read-only map base with the specified strategy for value equality and hashing.
		 *
		 * @param equal_hash_V The strategy for comparing and hashing values. Must not be null.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		
		/**
		 * Returns `true` if this map contains no key-value mappings.
		 *
		 * @return `true` if this map is empty.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the total number of key-value mappings in this map, including the null key if present.
		 *
		 * @return The number of mappings in this map.
		 */
		public int size() {
			return (
					       
					       _count() ) + (
					       hasNullKey ?
					       1 :
					       0 );
		}
		
		
		/**
		 * Returns the number of key-value mappings in this map. Alias for {@link #size()}.
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
			return ( keys == null ?
			         0 :
			         keys.length );
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
		 * Returns `true` if this map maps one or more keys to the specified value.
		 * This operation requires a full scan of the map and is O(N) in the number of entries.
		 *
		 * @param value The value to search for. Comparison uses the `equal_hash_V` strategy.
		 * @return `true` if the value is found in the map.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean containsValue( Object value ) {
			V v;
			try { v = ( V ) value; } catch( Exception e ) { return false; }
			if( hasNullKey && equal_hash_V.equals( nullKeyValue, v ) ) return true;
			if( size() == 0 ) return false;
			for( int i = 0; i < _lo_Size; i++ )
				if( equal_hash_V.equals( values[ i ], v ) ) return true;
			
			for( int i = keys.length - _hi_Size; i < keys.length; i++ )
				if( equal_hash_V.equals( values[ i ], v ) ) return true;
			
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
			       ( hasNullKey ?
			         token( NULL_KEY_INDEX ) :
			         INVALID_TOKEN ) :
			       tokenOf( ( float ) ( key + 0 ) );
		}
		
		/**
		 * Returns a "token" representing the location of the specified primitive key.
		 * This token can be used for fast access to the key and value via {@link #key(long)} and
		 * {@link #value(long)}, and includes a version stamp to detect concurrent modifications.
		 *
		 * @param key The primitive key to find.
		 * @return A `long` token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected while
		 *                                         traversing a collision chain, which may indicate
		 *                                         a concurrent modification.
		 */
		public long tokenOf( float key ) {
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			int index = ( _buckets[ bucketIndex( Array.hash( key ) ) ] ) - 1;
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
		 * and finally the null key mapping, if present.
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
		 * Returns the next valid internal array index for iteration, excluding the null key.
		 * This low-level method implements the core iteration logic, scanning first the dense `lo Region`
		 * and then the dense `hi Region`. It does not perform version checks.
		 *
		 * @param token The current internal index, or -1 to start from the beginning.
		 * @return The next valid internal array index, or -1 if iteration is complete.
		 */
		public int unsafe_token( final int token ) {
			if( _count() == 0 ) return -1;
			
			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size;
			
			if( i < _lo_Size ) return i;
			
			if( i < lowest_hi ) return _hi_Size == 0 ?
			                           -1 :
			                           lowest_hi;
			
			if( i < keys.length ) return i;
			
			return -1;
		}
		
		/**
		 * Returns `true` if this map contains a mapping for the null key.
		 *
		 * @return `true` if the null key is present.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value to which the null key is mapped, or `null` if this map contains
		 * no mapping for the null key.
		 *
		 * @return The value associated with the null key.
		 */
		public V nullKeyValue() {
			return hasNullKey ?
			       nullKeyValue :
			       null;
		}
		
		
		/**
		 * Returns `true` if the given token represents the null key.
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
		public float key( long token ) { return ( float )  ( keys[ index( token ) ] ); }
		
		
		/**
		 * Returns the value associated with the given token.
		 *
		 * @param token A valid token obtained from iteration or `tokenOf`.
		 * @return The value at the token's location. If the token represents the null key,
		 * returns the value mapped to the null key.
		 */
		public V value( long token ) {
			return
					isKeyNull( token ) ?
					nullKeyValue :
					values[ index( token ) ];
		}
		
		/**
		 * Returns the value to which the specified boxed key is mapped,
		 * or `null` if this map contains no mapping for the key.
		 *
		 * @param key The key whose associated value is to be returned (can be null).
		 * @return The value associated with the key, or `null` if the key is not found.
		 */
		public V get(  Float     key ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
			       null :
			       value( token );
		}
		
		/**
		 * Returns the value to which the specified primitive key is mapped,
		 * or `null` if this map contains no mapping for the key.
		 *
		 * @param key The key whose associated value is to be returned.
		 * @return The value associated with the key, or `null` if the key is not found.
		 */
		public V get( float key ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
			       null :
			       value( token );
		}
		
		/**
		 * Returns the value to which the specified boxed key is mapped,
		 * or `defaultValue` if this map contains no mapping for the key.
		 *
		 * @param key          The key whose associated value is to be returned (can be null).
		 * @param defaultValue The default value to return if no mapping is found.
		 * @return The value for the key, or `defaultValue` if not found.
		 */
		public V get(  Float     key, V defaultValue ) {
			long token = tokenOf( key );
			return ( token == INVALID_TOKEN || version( token ) != _version ) ?
			       defaultValue :
			       value( token );
		}
		
		/**
		 * Returns the value to which the specified primitive key is mapped,
		 * or `defaultValue` if this map contains no mapping for the key.
		 *
		 * @param key          The key whose associated value is to be returned.
		 * @param defaultValue The default value to return if no mapping is found.
		 * @return The value for the key, or `defaultValue` if not found.
		 */
		public V get( float key, V defaultValue ) {
			long token = tokenOf( key );
			return ( token == INVALID_TOKEN || version( token ) != _version ) ?
			       defaultValue :
			       value( token );
		}
		
		
		/**
		 * Computes the hash code for this map. The hash code is the sum of the hash codes
		 * of each key-value entry in the map.
		 *
		 * @return The hash code value for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				
				int keyHash = Array.hash( key( token ) );
				V   val     = value( token );
				int valueHash = val == null ?
				                seed :
				                equal_hash_V.hashCode( val );
				
				int h = Array.mix( seed, keyHash );
				h = Array.mix( h, valueHash );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, nullKeyValue == null ?
				                  seed :
				                  equal_hash_V.hashCode( nullKeyValue ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map with the specified object for equality. Returns `true` if the
		 * object is also an `IntObjectMap.R`, the two maps have the same size, and every
		 * mapping in this map is equal to a mapping in the other map.
		 *
		 * @param obj The object to be compared for equality with this map.
		 * @return `true` if the specified object is equal to this map.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj ); }
		
		/**
		 * Compares this map with another `IntObjectMap.R` for equality.
		 *
		 * @param other The other map to compare against.
		 * @return `true` if the maps contain the exact same key-value mappings.
		 */
		public boolean equals( R< V > other ) {
			
			
			if( other == this ) return true;
			if( other == null ||
			    hasNullKey != other.hasNullKey || ( hasNullKey && !equal_hash_V.equals( nullKeyValue, other.nullKeyValue ) ) ||
			    size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || !equal_hash_V.equals( value( token ), other.value( t ) ) ) return false;
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
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				if( _buckets != null ) dst._buckets = _buckets.clone();
				if( links != null ) dst.links = links.clone();
				if( keys != null ) dst.keys = keys.clone();
				if( values != null ) dst.values = values.clone();
				
				return dst;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e );
			}
		}
		
		/**
		 * Returns a string representation of the map in JSON object format (e.g., `{"1":val1, "42":val2, "null":val3}`).
		 *
		 * @return A JSON string representing the map.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		
		/**
		 * Writes the contents of this map to a {@link JsonWriter} as a JSON object.
		 * Keys are written as JSON numbers, and the null key is written as the JSON string `"null"`.
		 *
		 * @param json The {@link JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 15 );
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     json.name( keys[ token ] ).value( values[ token ] );
			
			
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
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index ); }
		
		/**
		 * Extracts the 0-based internal array index from a token.
		 *
		 * @param token The `long` token.
		 * @return The 0-based index.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
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
	 *
	 * @param <V> The type of values stored in the map.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs an empty map with a default initial capacity and value strategy.
		 *
		 * @param clazzV A `Class` object representing the value type, used for array creation.
		 */
		public RW( Class< V > clazzV ) { this( Array.get( clazzV ), 0 ); }
		
		/**
		 * Constructs an empty map with a specified initial capacity and default value strategy.
		 *
		 * @param clazzV   A `Class` object representing the value type.
		 * @param capacity The initial capacity hint.
		 */
		public RW( Class< V > clazzV, int capacity ) { this( Array.get( clazzV ), capacity ); }
		
		/**
		 * Constructs an empty map with a default capacity and a custom value strategy.
		 *
		 * @param equal_hash_V The strategy for comparing and hashing values.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V ) { this( equal_hash_V, 0 ); }
		
		/**
		 * Constructs an empty map with a specified initial capacity and a custom value strategy.
		 *
		 * @param equal_hash_V The strategy for comparing and hashing values.
		 * @param capacity     The initial capacity hint.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int capacity ) {
			super( equal_hash_V );
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
			links    = new int[ Math.min( 16, capacity ) ];
			keys     = new float[ capacity ];
			values   = equal_hash_V.copyOf( null, capacity );
			_lo_Size = 0;
			_hi_Size = 0;
			return length();
		}
		
		
		/**
		 * Associates the specified value with the specified boxed key. If the map previously
		 * contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The key with which the value is to be associated (can be null).
		 * @param value The value to be associated with the key.
		 * @return `true` if a new mapping was created, `false` if an existing value was updated.
		 */
		public boolean put(  Float     key, V value ) {
			return key == null ?
			       putNullKey( value ) :
			       put( ( float ) ( key + 0 ), value );
		}
		
		
		/**
		 * Associates the specified value with the null key. If a mapping for the null key
		 * already exists, the old value is replaced.
		 *
		 * @param value The value to be associated with the null key.
		 * @return `true` if a new mapping was created, `false` if the null key's value was updated.
		 */
		public boolean putNullKey( V value ) {
			boolean ret = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = value;
			_version++;
			return ret;
		}
		
		
		/**
		 * Associates the specified value with the specified primitive key. If the map previously
		 * contained a mapping for the key, the old value is replaced.
		 * <p>
		 * If the key's hash maps to an empty bucket, the entry is added to the `hi Region`.
		 * If it maps to an occupied bucket (a collision), the entry is added to the `lo Region`
		 * and becomes the new head of that bucket's collision chain. The map is resized
		 * if its capacity is exceeded.
		 *
		 * @param key   The primitive key to associate the value with.
		 * @param value The value to be associated with the key.
		 * @return `true` if a new mapping was created, `false` if an existing value was updated.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected during
		 *                                         collision chain traversal.
		 */
		public boolean put( float key, V value ) {
			
			
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
						values[ i ] = value;
						_version++;
						return false;
					}
					if( _lo_Size <= i ) break;
					i = links[ i ];
					
					if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
				
				if( links.length == ( dst_index = _lo_Size++ ) ) links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index;
			}
			
			keys[ dst_index ]       = ( float ) key;
			values[ dst_index ]     = value;
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
			_version++;
			return true;
		}
		
		/**
		 * Removes the mapping for the specified boxed key from this map if present.
		 *
		 * @param key The key whose mapping is to be removed (can be null).
		 * @return The previous value associated with the key, or `null` if there was no mapping.
		 */
		public V remove(  Float     key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( float ) ( key + 0 ) );
		}
		
		/**
		 * Removes the mapping for the null key from this map if present.
		 *
		 * @return The previous value associated with the null key, or `null` if there was no mapping.
		 */
		private V removeNullKey() {
			if( !hasNullKey ) return null;
			V oldValue = nullKeyValue;
			hasNullKey   = false;
			nullKeyValue = null;
			_version++;
			return oldValue;
		}
		
		/**
		 * Moves an entry (key, value, and link) from a source index to a destination index.
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
			
			keys[ dst ]   = keys[ src ];
			values[ dst ] = values[ src ];
		}
		
		
		/**
		 * Removes the mapping for the specified primitive key from this map if it is present.
		 * <p>
		 * After unlinking the entry from its collision chain, the data region (`lo` or `hi`)
		 * is compacted by moving the last logical element from that region into the vacated slot.
		 * The {@link #move(int, int)} helper ensures all pointers to the moved element are updated correctly.
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return The previous value associated with the key, or `null` if there was no mapping.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected during
		 *                                         collision chain traversal.
		 */
		public V remove( float key ) {
			if( _count() == 0 ) return null;
			int removeBucketIndex = bucketIndex( Array.hash( key ) );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1;
			if( removeIndex < 0 ) return null;
			
			if( _lo_Size <= removeIndex ) {
				
				if( keys[ removeIndex ] != key ) return null;
				V oldValue = values[ removeIndex ];
				move( keys.length - _hi_Size, removeIndex );
				_hi_Size--;
				_buckets[ removeBucketIndex ] = 0;
				_version++;
				return oldValue;
			}
			
			V   oldValue;
			int next = links[ removeIndex ];
			if( keys[ removeIndex ] == key ) {
				_buckets[ removeBucketIndex ] = ( int ) ( next + 1 );
				oldValue                      = values[ removeIndex ];
			}
			else {
				int last = removeIndex;
				if( keys[ removeIndex = next ] == key ) {
					oldValue = values[ removeIndex ];
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
					else {
						
						keys[ removeIndex ]   = keys[ last ];
						values[ removeIndex ] = values[ last ];
						
						_buckets[ removeBucketIndex ] = ( int ) ( removeIndex + 1 );
						removeIndex                   = last;
					}
				}
				else if( _lo_Size <= removeIndex ) return null;
				else
					for( int collisions = 0; ; ) {
						int prev = last;
						if( keys[ removeIndex = links[ last = removeIndex ] ] == key ) {
							oldValue = values[ removeIndex ];
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else {
								
								keys[ removeIndex ]   = keys[ last ];
								values[ removeIndex ] = values[ last ];
								links[ prev ]         = ( int ) removeIndex;
								removeIndex           = last;
							}
							break;
						}
						if( _lo_Size <= removeIndex ) return null;
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
			}
			
			move( _lo_Size - 1, removeIndex );
			_lo_Size--;
			_version++;
			return oldValue;
		}
		
		/**
		 * Removes all mappings from this map. The map will be empty after this call, but its
		 * allocated capacity will remain unchanged. This is a structural modification.
		 */
		public void clear() {
			_version++;
			
			hasNullKey   = false;
			nullKeyValue = null;
			
			
			if( _count() == 0 ) return;
			if( _buckets != null ) Arrays.fill( _buckets, ( int ) 0 );
			_lo_Size = 0;
			_hi_Size = 0;
		}
		
		/**
		 * Ensures that this map can hold at least the specified number of non-null entries
		 * without needing to resize. If necessary, the internal arrays are resized.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new capacity of the map.
		 * @throws IllegalArgumentException if the specified capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity is less than 0." );
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
		 * @throws IllegalArgumentException if the specified capacity is less than the current number of entries.
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than the number of non-null key entries." );
			
			capacity = Array.prime( Math.max( capacity, _count() ) );
			
			if( length() <= capacity ) return;
			
			resize( capacity );
		}
		
		/**
		 * Resizes the internal hash map arrays to a new capacity. All existing entries are
		 * rehashed and inserted into the new arrays, rebuilding the `lo` and `hi` regions.
		 * This is a structural modification and increments the version counter.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resizing.
		 */
		private int resize( int newSize ) {
			_version++;
			
			
			float[] old_keys    = keys;
			V[]           old_values  = values;
			int           old_lo_Size = _lo_Size;
			int           old_hi_Size = _hi_Size;
			
			initialize( newSize );
			
			for( int i = 0; i < old_lo_Size; i++ )
			     copy( old_keys[ i ], old_values[ i ] );
			
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
			     copy( old_keys[ i ], old_values[ i ] );
			
			return length();
		}
		
		
		/**
		 * An internal helper for efficiently copying an entry into the new hash table
		 * structure during a resize operation. It re-hashes the key and places the entry
		 * without checking for duplicates, as all keys from the old map are unique.
		 *
		 * @param key   The primitive key to copy.
		 * @param value The value to copy.
		 */
		private void copy( float key, V value ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 ) dst_index = keys.length - 1 - _hi_Size++;
			else {
				if( links.length == _lo_Size ) links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) );
				links[ dst_index = _lo_Size++ ] = ( int ) ( index );
			}
			
			keys[ dst_index ]       = key;
			values[ dst_index ]     = value;
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
		}
		
		
		/**
		 * Creates and returns a deep copy of this read-write map.
		 *
		 * @return A cloned instance of this map.
		 * @throws InternalError if cloning fails.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public RW< V > clone() { return ( RW< V > ) super.clone(); }
		
		
		/**
		 * A static instance providing a default strategy for value equality and hashing.
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
	}
	
	/**
	 * Returns an {@link Array.EqualHashOf} implementation for this map type. This allows
	 * instances of `IntObjectMap` to be used as values in other generic collections
	 * that require a custom equality and hashing strategy (e.g., for nesting maps).
	 *
	 * @param <V> The type of values in the map.
	 * @return A singleton {@link Array.EqualHashOf} instance for this map type.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() { return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; }
}