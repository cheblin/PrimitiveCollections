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
import java.util.function.Function;

/**
 * A generic, high-performance map that stores object keys and memory-efficient, bit-packed integer values.
 * Values are stored in a {@link BitsList}, allowing each value to occupy only 1 to 7 bits, making this
 * implementation ideal for applications with memory constraints or those mapping to small integer ranges or flags.
 * <p>
 * This map is built on a hash table with an optimized collision resolution strategy that separates
 * entries into a "low" region (for collision chains) and a "high" region (for non-colliding entries)
 * to enhance performance.
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *     <li><b>Generic Keys:</b> Supports any object type as keys.</li>
 *     <li><b>Bit-Packed Values:</b> Uses {@link BitsList} to store values compactly (1-7 bits per value),
 *         significantly reducing memory footprint.</li>
 *     <li><b>Optimized Collision Handling:</b> Employs a hybrid strategy with separate chaining for collisions
 *         and direct access for non-colliding entries.</li>
 *     <li><b>Dynamic Resizing:</b> The internal hash table automatically grows to maintain performance as more
 *         elements are added. Capacity can also be managed with {@link RW#ensureCapacity} and {@link RW#trim}.</li>
 *     <li><b>Null Key Support:</b> Allows a single null key to be mapped to a value.</li>
 *     <li><b>Customizable Hashing/Equality:</b> Key hashing and equality checks are delegated to a
 *         pluggable {@link Array.EqualHashOf} strategy.</li>
 *     <li><b>Fail-Fast Token-Based Iteration:</b> Provides a safe and efficient token-based mechanism for
 *         traversing the map, with an unsafe, higher-performance option available.</li>
 *     <li><b>Cloning:</b> Supports shallow cloning of the map structure.</li>
 * </ul>
 */
public interface ObjectBitsMap {
	
	/**
	 * A read-only base class that provides the core data structures and access methods for the map.
	 * It manages the internal arrays, state, and the token-based iteration system.
	 *
	 * @param <K> The type of keys maintained by this map.
	 */
	abstract class R< K > implements JsonWriter.Source, Cloneable {
		/**
		 * Flag indicating whether a mapping for a null key exists.
		 */
		protected boolean     hasNullKey;
		/**
		 * The value associated with the null key, stored as a byte. Only valid if {@link #hasNullKey} is true.
		 */
		protected byte        nullKeyValue;
		/**
		 * The hash table, storing 1-based indices into the {@link #keys}, {@link #values}, etc., arrays.
		 * An index of 0 indicates an empty bucket.
		 */
		protected int[]       _buckets;
		/**
		 * An array storing the cached hash code for each corresponding key in the {@link #keys} array.
		 */
		protected int[]       hash;
		/**
		 * An array for storing collision chain links. For an entry at index {@code i} in the "low" region,
		 * {@code links[i]} stores the 0-based index of the next entry in the same bucket's chain.
		 */
		protected int[]       links= Array.EqualHashOf._ints.O;
		/**
		 * An array storing the keys of the map.
		 */
		protected K[]         keys;
		/**
		 * A {@link BitsList} instance that stores all values in a bit-packed format.
		 */
		protected BitsList.RW values;
		
		/**
		 * The number of active entries stored in the "low" region of the internal arrays (from index 0 to {@code _lo_Size - 1}).
		 * These entries are part of collision chains.
		 */
		protected int _lo_Size;
		/**
		 * The number of active entries stored in the "high" region of the internal arrays (from index {@code keys.length - _hi_Size}
		 * to {@code keys.length - 1}). These entries are unique occupants of their hash buckets.
		 */
		protected int _hi_Size;
		
		/**
		 * A version counter, incremented on each structural modification to the map. Used for fail-fast iteration.
		 */
		protected int                    _version;
		/**
		 * The strategy object used for computing hash codes and checking equality for keys.
		 */
		protected Array.EqualHashOf< K > equal_hash_K;
		
		/**
		 * A special index used in tokens to represent the null key.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * The number of bits to shift the version number when creating a token.
		 */
		protected static final int  VERSION_SHIFT = 32;
		/**
		 * A constant representing an invalid or non-existent token, typically returned when a key is not found.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Returns the total number of non-null key-value mappings in the map.
		 * This is the sum of entries in both the low and high regions.
		 *
		 * @return The current count of non-null entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return {@code true} if the map contains no key-value mappings, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the total number of key-value mappings in this map.
		 * This count includes the mapping for the null key, if it exists.
		 *
		 * @return The number of key-value mappings in the map.
		 */
		public int size() {
			return _count() + (
					hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Alias for {@link #size()}.
		 *
		 * @return The number of key-value mappings in the map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the internal arrays. This is the maximum number of
		 * entries the map can hold without resizing.
		 *
		 * @return The length of the internal arrays, or 0 if the map is uninitialized.
		 */
		public int length() {
			return keys == null ?
			       0 :
			       keys.length;
		}
		
		/**
		 * Checks if the map contains a mapping for the specified key.
		 *
		 * @param key The key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean containsKey( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this map maps one or more keys to the specified value.
		 * This operation can be efficient as it delegates the check to the underlying {@link BitsList}.
		 *
		 * @param value The value whose presence in this map is to be tested.
		 * @return {@code true} if this map maps one or more keys to the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( int value ) { return hasNullKey && nullKeyValue == value || 0 < _count() && values.contains( value ); }
		
		/**
		 * Checks if the map contains a mapping for the null key.
		 *
		 * @return {@code true} if a mapping for the null key exists, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value to which the null key is mapped. This method should only be
		 * called if {@link #hasNullKey()} returns {@code true}.
		 *
		 * @return The value associated with the null key.
		 */
		public int nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Returns a token representing the mapping for the specified key. A token is a
		 * long value that encodes the entry's internal index and the map's current version,
		 * allowing for safe, fail-fast access and iteration.
		 *
		 * @param key The key to find.
		 * @return A valid token if the key is found, or {@link #INVALID_TOKEN} if the key is not in the map.
		 * @throws ConcurrentModificationException if the hash chain for the key's bucket is excessively long,
		 *                                         suggesting a potential concurrent modification issue or poor hash distribution.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
			                         token( NULL_KEY_INDEX ) :
			                         INVALID_TOKEN;
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			
			int hash  = equal_hash_K.hashCode( key );
			int index = _buckets[ bucketIndex( hash ) ] - 1; // 0-based index from bucket
			if( index < 0 ) return INVALID_TOKEN; // Bucket is empty
			
			// If the first entry is in the hi Region (it's the only one for this bucket)
			if( _lo_Size <= index ) return ( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) ?
			                               token( index ) :
			                               INVALID_TOKEN;
			
			// Traverse collision chain (entry is in lo Region)
			for( int collisions = 0; ; ) {
				if( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) return token( index );
				if( _lo_Size <= index ) break; // Reached a terminal node that might be in hi Region (no more links)
				index = links[ index ];
				if( _lo_Size + 1 < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the first token for starting an iteration over the map.
		 * The iteration order is not guaranteed. If the map contains non-null keys, a token for
		 * the first non-null key is returned. Otherwise, if the map contains only a null key,
		 * its token is returned.
		 *
		 * @return The first token for iteration, or {@link #INVALID_TOKEN} if the map is empty.
		 */
		public long token() {
			int index = unsafe_token( -1 ); // Get the first non-null key token
			return index == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       // If no non-null keys, return null key token if present
			       INVALID_TOKEN :
			       token( index ); // Return token for found non-null key
		}
		
		/**
		 * Returns the next token in the iteration sequence.
		 * The iteration order is not guaranteed. It will traverse all non-null keys first,
		 * and if a null key exists, its token will be returned last.
		 *
		 * @param token The current token from a previous call to {@link #token()} or {@link #token(long)}.
		 * @return The next token in the sequence, or {@link #INVALID_TOKEN} if the iteration is complete.
		 * @throws IllegalArgumentException        if the provided {@code token} is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the map has been structurally modified since the token was issued.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN; // If current token is null key, no more elements
			
			index = unsafe_token( index ); // Get next unsafe token (non-null key)
			
			return index == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       // If no more non-null keys, check for null key
			       INVALID_TOKEN :
			       token( index ); // Return token for found non-null key
		}
		
		/**
		 * Returns the next token for fast, <b>unsafe</b> iteration over <b>non-null keys only</b>.
		 * <p>
		 * This method provides direct access to the internal indices of the map's entries.
		 * To start an iteration, call with {@code -1}. Subsequent calls should pass the previously
		 * returned value. The iteration is finished when this method returns {@code -1}.
		 * </p>
		 * <p>The null key is <b>not</b> included in this iteration. Check for it separately using {@link #hasNullKey()}.</p>
		 * <p><b>WARNING:</b> This method is "unsafe" because it does not perform version checks. If the map
		 * is structurally modified (e.g., by adding or removing elements) during iteration, the behavior is
		 * undefined and may lead to missed entries, repeated entries, or exceptions. Use only in single-threaded
		 * contexts where no modifications occur during the loop.</p>
		 *
		 * @param token The previous token (internal index), or {@code -1} to start the iteration.
		 * @return The next token (internal index), or {@code -1} if there are no more non-null entries.
		 */
		public int unsafe_token( int token ) {
			if( _buckets == null || _count() == 0 ) return -1;
			
			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size; // Start of hi region (inclusive)
			
			// Check lo region first
			if( i < _lo_Size ) return i; // Return the current index in lo region
			
			// If we've exhausted lo region or started beyond it
			if( i < lowest_hi ) { // If 'i' is in the gap between lo and hi regions
				// If hi region is empty, no more entries
				if( _hi_Size == 0 ) return -1;
				return lowest_hi; // Return the start of hi region
			}
			
			// If 'i' is already in or past the start of the hi region
			// Iterate through hi region
			if( i < keys.length ) return i;
			
			return -1; // No more entries
		}
		
		/**
		 * Checks if the given token represents the null key mapping.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token corresponds to the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the key corresponding to the given token.
		 *
		 * @param token The token representing the entry.
		 * @return The key for the entry. Returns {@code null} if the token represents the null key mapping.
		 */
		public K key( long token ) {
			return isKeyNull( token ) ?
			       null :
			       keys[ index( token ) ];
		}
		
		/**
		 * Returns the number of bits used to store each value in the underlying {@link BitsList}.
		 *
		 * @return The number of bits per value, typically between 1 and 7.
		 */
		public int bits_per_value() { return values.bits_per_item; }
		
		/**
		 * Returns the value corresponding to the given token.
		 *
		 * @param token The token representing the entry.
		 * @return The value for the entry, as a {@code byte}.
		 */
		public byte value( long token ) {
			return isKeyNull( token ) ?
			       nullKeyValue :
			       values.get( index( token ) ); // Handle null key value
		}
		
		/**
		 * Computes the hash code for this map. The hash code is derived from the hash codes
		 * of its key-value pairs.
		 *
		 * @return The hash code value for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			// Iterate through all key-value pairs and mix their hash codes
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) ); // Mix seed with key hash
				h = Array.mix( h, Array.hash( value( token ) ) );     // Mix with value hash
				h = Array.finalizeHash( h, 2 );                 // Finalize the pair hash
				a += h;
				b ^= h;
				c *= h | 1;                  // Accumulate hash components
			}
			if( hasNullKey ) { // Include null key entry in hash calculation
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() ); // Finalize the map hash
		}
		
		/**
		 * Seed for hash code computation.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares the specified object with this map for equality.
		 *
		 * @param obj The object to be compared for equality with this map.
		 * @return {@code true} if the specified object is also a map and the two maps represent
		 * the same mappings, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Compares the specified map with this map for equality. Two maps are equal if they
		 * have the same size and contain the same key-value mappings.
		 *
		 * @param other The map to be compared for equality with this map.
		 * @return {@code true} if the specified map is equal to this map, {@code false} otherwise.
		 */
		public boolean equals( R< K > other ) {
			if( other == this ) return true; // Same instance check
			if( other == null || hasNullKey != other.hasNullKey || // Null check and null key consistency check
			    hasNullKey && nullKeyValue != other.nullKeyValue || size() != other.size() ) return false; // Null key value and size check
			
			// Compare key-value pairs using tokens and equality checks
			long t;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( ( t = other.tokenOf( key( token ) ) ) == INVALID_TOKEN || value( token ) != other.value( t ) ) return false; // Check if keys and values match in both maps
			return true; // All key-value pairs are equal
		}
		
		/**
		 * Creates and returns a shallow copy of this map. The keys are not cloned, and the
		 * internal {@link BitsList} is also shallow-copied.
		 *
		 * @return A shallow copy of this map instance.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public R< K > clone() {
			try {
				R dst = ( R ) super.clone(); // Perform shallow clone
				if( _buckets != null ) { // Clone internal arrays if initialized
					dst._buckets = _buckets.clone();
					dst.hash     = hash.clone(); // Clone hash array
					dst.links    = links.clone(); // Clone links array
					dst.keys     = keys.clone();
					dst.values   = values.clone(); // BitsList.RW is also cloneable and performs a shallow copy
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e ); // Should not happen as R implements Cloneable
			}
		}
		
		/**
		 * Returns a JSON string representation of this map.
		 *
		 * @return A string containing the JSON representation of the map.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Serializes the contents of this map into a {@link JsonWriter}.
		 * If the keys are strings, the map is written as a JSON object.
		 * Otherwise, it is written as a JSON array of key-value objects.
		 *
		 * @param json The {@link JsonWriter} to write the JSON output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 ); // Preallocate buffer for better performance
			
			if( equal_hash_K == Array.string ) { // Optimize for string keys: output as JSON object
				json.enterObject();
				if( hasNullKey ) json.name().value( nullKeyValue );
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( key( token ) == null ?
				                null :
				                key( token ).toString() ).value( value( token ) ); // Write as key-value pairs in object
				json.exitObject();
			}
			else { // For non-string keys: output as JSON array of key-value objects
				json.enterArray();
				if( hasNullKey )
					json
							.enterObject()
							.name( "Key" ).value()
							.name( "Value" ).value( nullKeyValue )
							.exitObject();
				
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.enterObject()
				         .name( "Key" ).value( key( token ) )
				         .name( "Value" ).value( value( token ) )
				         .exitObject(); // Write each entry as a { "Key": ..., "Value": ... } object
				json.exitArray();
			}
		}
		
		/**
		 * Calculates the bucket index for a given hash code.
		 *
		 * @param hash The hash code.
		 * @return The index in the {@link #_buckets} array.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; } // Ensure positive index within bucket array bounds
		
		/**
		 * Creates a fail-fast token from an internal index and the current version.
		 *
		 * @param index The internal index of the entry.
		 * @return A long token combining the version and index.
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); } // Combine version and index into a token
		
		/**
		 * Extracts the internal index from a token.
		 *
		 * @param token the token value
		 * @return the extracted entry index.
		 */
		protected int index( long token ) { return ( int ) token; }
		
		/**
		 * Extracts the version number from a token.
		 *
		 * @param token The token.
		 * @return The version number encoded in the token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * A read-write implementation of the map, providing methods to add, remove, and modify mappings.
	 *
	 * @param <K> The type of keys maintained by this map.
	 */
	class RW< K > extends R< K > {
		/**
		 * The threshold for the number of collisions in a single bucket chain that, for String keys,
		 * can trigger a resize with a new hash function to mitigate hash-flooding attacks.
		 */
		private static final int                                                        HashCollisionThreshold = 100;
		/**
		 * An optional function that, when set, provides a new hashing strategy during a resize operation.
		 * This is primarily used to counter high-collision scenarios (e.g., hash-flooding attacks on String keys).
		 * The function receives the current {@link Array.EqualHashOf} instance and should return a new one.
		 */
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs an empty map with a specified key class and number of bits per value.
		 *
		 * @param clazzK        The runtime class of the keys.
		 * @param bits_per_item The number of bits (1-7) to use for storing each value.
		 */
		public RW( Class< K > clazzK, int bits_per_item ) { this( clazzK, bits_per_item, 0 ); }
		
		/**
		 * Constructs an empty map with a specified key class, bits per value, and initial capacity.
		 *
		 * @param clazzK        The runtime class of the keys.
		 * @param bits_per_item The number of bits (1-7) to use for storing each value.
		 * @param capacity      The initial capacity of the map.
		 */
		public RW( Class< K > clazzK, int bits_per_item, int capacity ) { this( Array.get( clazzK ), bits_per_item, capacity ); }
		
		/**
		 * Constructs an empty map with a custom key strategy and number of bits per value.
		 *
		 * @param equal_hash_K  The strategy for comparing and hashing keys.
		 * @param bits_per_item The number of bits (1-7) to use for storing each value.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item ) { this( equal_hash_K, bits_per_item, 0 ); }
		
		/**
		 * Constructs an empty map with a custom key strategy, bits per value, and initial capacity.
		 *
		 * @param equal_hash_K  The strategy for comparing and hashing keys.
		 * @param bits_per_item The number of bits (1-7) to use for storing each value.
		 * @param capacity      The initial capacity of the map.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			values            = new BitsList.RW( bits_per_item, ( byte ) 0, capacity ); // Default value 0 for BitsList
			if( capacity > 0 ) initialize( Array.prime( capacity ) ); // Initialize hash table if capacity is provided
		}
		
		/**
		 * Constructs an empty map with a custom key strategy, bits per value, initial capacity, and a default value.
		 *
		 * @param equal_hash_K  The strategy for comparing and hashing keys.
		 * @param bits_per_item The number of bits (1-7) to use for storing each value.
		 * @param capacity      The initial capacity of the map.
		 * @param defaultValue  The default value to use for new entries in the underlying {@link BitsList}.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item, int capacity, byte defaultValue ) {
			this.equal_hash_K = equal_hash_K;
			values            = new BitsList.RW( bits_per_item, defaultValue, capacity ); // Default value for BitsList
			if( capacity > 0 ) initialize( Array.prime( capacity ) ); // Initialize hash table if capacity is provided
		}
		
		/**
		 * Removes all mappings from this map. The map will be empty after this call returns,
		 * but the internal arrays will retain their current capacity.
		 */
		public void clear() {
			_version++;                               // Increment version for modification detection
			hasNullKey = false;                       // Remove null key flag
			if( _count() == 0 ) return; // Already empty
			Arrays.fill( _buckets, 0 ); // Reset buckets
			Arrays.fill( keys, null ); // clear refs
			// hash and links arrays are implicitly cleared by resetting _lo_Size and _hi_Size
			_lo_Size = 0;
			_hi_Size = 0;
			values.clear();                           // Clear values in BitsList
		}
		
		/**
		 * Removes the mapping for a key from this map if it is present.
		 *
		 * @param key The key whose mapping is to be removed from the map.
		 * @return {@code true} if a mapping was removed, {@code false} if the key was not found.
		 * @throws ConcurrentModificationException if an excessively long collision chain is encountered,
		 *                                         which may indicate a data corruption issue.
		 */
		public boolean remove( K key ) {
			if( key == null ) { // Handle null key removal
				if( !hasNullKey ) return false; // Null key not present
				hasNullKey = false;                     // Remove null key flag
				_version++;                             // Increment version
				return true;
			}
			
			if( _count() == 0 ) return false; // Map is empty
			
			int hash              = equal_hash_K.hashCode( key );
			int removeBucketIndex = bucketIndex( hash );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1; // 0-based index from bucket
			if( removeIndex < 0 ) return false; // Key not in this bucket
			
			// Case 1: Entry to be removed is in the hi Region (cannot be part of a chain from _lo_Size)
			if( _lo_Size <= removeIndex ) {
				if( this.hash[ removeIndex ] != hash || !equal_hash_K.equals( keys[ removeIndex ], key ) ) return false;
				
				// Move the last element of hi region to the removed slot
				move( keys.length - _hi_Size, removeIndex );
				
				_hi_Size--; // Decrement hi_Size
				_buckets[ removeBucketIndex ] = 0; // Clear the bucket reference (it was the only one)
				_version++;
				return true;
			}
			
			
			int next = links[ removeIndex ];
			if( this.hash[ removeIndex ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) _buckets[ removeBucketIndex ] = ( int ) ( next + 1 );
			else {
				int last = removeIndex;
				if( this.hash[ removeIndex = next ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) )// The key is found at 'SecondNode'
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];// 'SecondNode' is in 'lo Region', relink to bypasses 'SecondNode'
					else {  // 'SecondNode' is in the hi Region (it's a terminal node)
						
						keys[ removeIndex ]      = keys[ last ]; //  Copies `keys[last]` to `keys[removeIndex]`
						this.hash[ removeIndex ] = this.hash[ last ];
						values.set1( removeIndex, values.get( last ) ); // Copies `values[last]` to `values[removeIndex]`
						
						// Update the bucket for this chain.
						// 'removeBucketIndex' is the hash bucket for the original 'key' (which was keys[T]).
						// Since keys[P] and keys[T] share the same bucket index, this is also the bucket for keys[P].
						// By pointing it to 'removeIndex' (which now contains keys[P]), we make keys[P] the new sole head.
						_buckets[ removeBucketIndex ] = ( int ) ( removeIndex + 1 );
						removeIndex                   = last;
					}
				else if( _lo_Size <= removeIndex ) return false;
				else
					for( int collisions = 0; ; ) {
						int prev = last;
						
						if( this.hash[ removeIndex = links[ last = removeIndex ] ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) {
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else {
								
								keys[ removeIndex ]      = keys[ last ];
								this.hash[ removeIndex ] = this.hash[ last ];
								values.set1( removeIndex, values.get( last ) ); // Copies `values[last]` to `values[removeIndex]`
								
								links[ prev ] = ( int ) removeIndex;
								removeIndex   = last; // Mark original 'last' (lo-region entry) for removal/compaction
							}
							break; // Key found and handled
						}
						if( _lo_Size <= removeIndex ) return false; // Reached hi-region terminal node, key not found
						// Safeguard against excessively long or circular chains (corrupt state)
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
			}
			
			move( _lo_Size - 1, removeIndex );
			_lo_Size--; // Decrement lo-region size
			_version++; // Structural modification
			return true;
			
		}
		
		/**
		 * Relocates an entry's data from a source index to a destination index and updates all internal
		 * pointers (from buckets or chain links) to reflect the move. This is a key part of the compaction
		 * process during element removal.
		 *
		 * @param src The source index of the entry to move.
		 * @param dst The destination index for the entry.
		 */
		private void move( int src, int dst ) {
			if( src == dst ) return;
			
			int bucketIndex = bucketIndex( this.hash[ src ] );
			int index       = _buckets[ bucketIndex ] - 1;
			
			if( index == src ) _buckets[ bucketIndex ] = ( int ) ( dst + 1 ); // Update bucket head
			else {
				while( links[ index ] != src ) { // Find the link pointing to src
					index = links[ index ];
					if( index < 0 || index >= keys.length ) break; // Defensive break for corrupted chain
				}
				if( links[ index ] == src ) // Ensure we found it before updating
					links[ index ] = ( int ) dst; // Update the link to point to dst
			}
			
			if( src < _lo_Size ) links[ dst ] = links[ src ]; // If src was in lo region, copy its link
			
			this.hash[ dst ] = this.hash[ src ]; // Copy hash
			keys[ dst ]      = keys[ src ];   // Copy key
			keys[ src ]      = null;          // Clear source slot for memory management and to prevent stale references
			values.set1( dst, values.get( src ) ); // Copy value
		}
		
		/**
		 * Ensures that the map has enough capacity to hold at least the specified number of elements
		 * without needing to resize. If the current capacity is insufficient, the map is resized to a
		 * larger, prime-based capacity.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity of the map (which may be larger than the requested capacity).
		 * @throws IllegalArgumentException if the specified capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity < 0" );
			int currentCapacity = length();
			if( capacity <= currentCapacity ) return currentCapacity; // Already sufficient capacity
			_version++; // Increment version as structural change is about to occur
			if( _buckets == null ) return initialize( Array.prime( capacity ) ); // Initialize if buckets are null
			int newSize = Array.prime( capacity ); // Get next prime size
			resize( newSize, false );                // Resize to the new size
			return newSize;
		}
		
		/**
		 * Reduces the capacity of the map to be closer to its actual size. This can be used to
		 * minimize the memory footprint of the map after a large number of elements have been removed.
		 */
		public void trim() { trim( _count() ); }
		
		/**
		 * Reduces the capacity of the map to the specified capacity, if it's smaller than the current capacity.
		 * The final capacity will be a prime number at least as large as the map's current size.
		 *
		 * @param capacity The target capacity. Must not be less than the current number of elements.
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			if( length() <= ( capacity = Array.prime( Math.max( capacity, size() ) ) ) ) return;
			
			resize( capacity,false );
			
			if( _lo_Size < links.length )
				links = _lo_Size == 0 ?
				        Array.EqualHashOf._ints.O :
				        Array.copyOf( links, _lo_Size );
		}
		
		/**
		 * Associates the specified value with the <b>null key</b>. If the map previously contained a
		 * mapping for the null key, the old value is replaced.
		 *
		 * @param value The value to be associated with the null key. The value will be masked to fit the configured bit size.
		 * @return {@code true} if a new mapping for the null key was created, {@code false} if an existing mapping was updated.
		 */
		public boolean put( long value ) {
			boolean b = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = ( byte ) ( value & values.mask );
			_version++;
			return b;
		}
		
		/**
		 * Copies all of the mappings from the specified source map to this map.
		 * These mappings will replace any mappings that this map had for any of the
		 * keys currently in the specified map.
		 *
		 * @param src The map whose mappings are to be placed in this map.
		 */
		public void putAll( ObjectBitsMap.RW< ? extends K > src ) {
			for( int token = -1; ( token = src.unsafe_token( token ) ) != -1; )
			     put( src.key( token ), src.value( token ) );
		}
		
		/**
		 * Associates the specified value with the specified key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced by the specified value.
		 * If the key is new, it's added to either the "high" region (if no hash collision) or the "low" region (if a collision occurs).
		 *
		 * @param key   The key with which the specified value is to be associated.
		 * @param value The value to be associated with the key. The value will be masked to fit the configured bit size.
		 * @return {@code true} if the key was not already in the map, {@code false} if the key existed and its value was updated.
		 * @throws ConcurrentModificationException if a very high number of collisions is detected, which might indicate concurrent modification.
		 */
		public boolean put( K key, long value ) {
			if( key == null ) return put( value );
			
			if( _buckets == null ) initialize( 7 ); // Initial capacity if not yet initialized
			else // If backing array is full, resize
				if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ), false ); // Resize to double capacity
			
			int hash        = equal_hash_K.hashCode( key );
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from bucket
			int dst_index; // Destination index for the new/updated entry
			
			// If bucket is empty, new entry goes into hi Region
			if( index == -1 ) dst_index = keys.length - 1 - _hi_Size++; // Calculate new position in hi region
			else {
				
				int collisions = 0;
				for( int i = index; ; ) {
					if( this.hash[ i ] == hash && equal_hash_K.equals( keys[ i ], key ) ) {
						values.set1( i, value );// Update value
						_version++; // Increment version as value was modified
						return false;// Key was not new
					}
					if( _lo_Size <= i ) break; // Reached a terminal node in hi Region
					i = links[ i ]; // Move to next in chain
					// Safeguard against excessively long or circular chains (corrupt state)
					if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
				// Check for high collision and potential rehashing for string keys, adapted from original put()
				if( HashCollisionThreshold < collisions && this.forceNewHashCodes != null && key instanceof String ) // Check for high collision and potential rehashing for string keys
				{
					resize( keys.length, true ); // Resize to potentially trigger new hash codes
					hash        = equal_hash_K.hashCode( key );
					bucketIndex = bucketIndex( hash );
					index       = _buckets[ bucketIndex ] - 1;
				}
				
				( links.length == ( dst_index = _lo_Size++ ) ?
				  links = Arrays.copyOf( links, Math.max( 16, Math.min( _lo_Size * 2, keys.length ) ) ) :
				  links )[ dst_index ] = index; // New entry points to the old head
			}
			
			
			this.hash[ dst_index ] = hash; // Store hash code
			keys[ dst_index ]      = key;     // Store key
			values.set1( dst_index, ( byte ) value ); // Store value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to point to the new entry (1-based)
			_version++; // Increment version
			
			return true; // New mapping added
		}
		
		/**
		 * Resizes the internal data structures to a new capacity. All existing entries are re-hashed
		 * and re-inserted into the new arrays. This is a structural modification.
		 *
		 * @param newSize           The new capacity for the internal arrays.
		 * @param forceNewHashCodes If true, and {@link #forceNewHashCodes} is set, a new hashing strategy
		 *                          will be applied during the re-hashing process.
		 * @return The new capacity of the map.
		 */
		private int resize( int newSize, boolean forceNewHashCodes ) {
			_version++;
			
			// Store old data before re-initializing
			K[]         old_keys    = keys;
			int[]       old_hash    = hash;
			BitsList.RW old_values  = values.clone(); // Clone values to copy them over
			int         old_lo_Size = _lo_Size;
			int         old_hi_Size = _hi_Size;
			
			if( links.length < 0xFF && links.length < _buckets.length ) links = _buckets;//reuse buckets as links
			initialize( newSize );
			
			
			// If forceNewHashCodes is set, apply it to the equal_hash_K provider BEFORE re-hashing elements.
			if( forceNewHashCodes ) {
				
				equal_hash_K = this.forceNewHashCodes.apply( equal_hash_K ); // Apply new hashing strategy
				
				// Copy elements from old structure to new structure by re-inserting
				K key;
				// Iterate through old lo region
				for( int i = 0; i < old_lo_Size; i++ ) copy( key = old_keys[ i ], equal_hash_K.hashCode( key ), old_values.get( i ) );
				
				// Iterate through old hi region
				for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) copy( key = old_keys[ i ], equal_hash_K.hashCode( key ), old_values.get( i ) );
				
				return keys.length; // Return actual new capacity
			}
			
			// Copy elements from old structure to new structure by re-inserting
			
			// Iterate through old lo region
			for( int i = 0; i < old_lo_Size; i++ ) copy( old_keys[ i ], old_hash[ i ], old_values.get( i ) );
			
			// Iterate through old hi region
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) copy( old_keys[ i ], old_hash[ i ], old_values.get( i ) );
			
			return keys.length; // Return actual new capacity
		}
		
		/**
		 * Initializes the internal data structures with a given capacity.
		 * This is a structural modification.
		 *
		 * @param capacity The initial capacity for the map.
		 * @return The allocated capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets = new int[ capacity ];     // Initialize buckets array
			hash     = new int[ capacity ];     // Initialize hash array
			_lo_Size = 0;
			_hi_Size = 0;
			return capacity;
		}
		
		/**
		 * An internal helper method for inserting an element during a resize operation.
		 * It performs a simplified `put` operation, assuming the key is new to the
		 * target arrays and does not check for duplicates.
		 *
		 * @param key   The key to insert.
		 * @param hash  The pre-computed hash of the key.
		 * @param value The value to insert.
		 */
		private void copy( K key, int hash, byte value ) {
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from the bucket
			
			int dst_index; // Destination index for the key
			
			if( index == -1 ) // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++;
			else ( links.length == _lo_Size ?
				  links = Arrays.copyOf( links, Math.max( 16, Math.min( _lo_Size * 2, keys.length ) ) ) :
				  links )[ dst_index = _lo_Size++ ] = ( char ) ( index );
			
			keys[ dst_index ]      = key; // Store the key
			this.hash[ dst_index ] = hash; // Store the hash
			values.set1( dst_index, value ); // Store the value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to new head (1-based)
		}
		
		/**
		 * A default, shared {@link Array.EqualHashOf} instance for {@link RW} map types.
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
		@Override public RW< K > clone() { return ( RW< K > ) super.clone(); }
	}
	
	/**
	 * Returns a default {@link Array.EqualHashOf} strategy for maps of this type.
	 *
	 * @param <K> The key type.
	 * @return A singleton {@link Array.EqualHashOf} instance for the map.
	 */
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() { return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT; }
}