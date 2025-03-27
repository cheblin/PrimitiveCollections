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
import java.util.function.Function;

/**
 * <p>
 * **ObjectBitsMap: A Memory-Efficient and Versatile Map Implementation**
 * </p>
 * <p>
 * This interface defines a generic Map specifically designed for storing key-value pairs where values are compactly stored as bits.
 * It leverages a hash table with separate chaining for efficient collision handling and dynamic resizing to adapt to varying data sizes.
 * </p>
 * <p>
 * **Core Features:**
 * </p>
 * <ul>
 *     <li>**Generic Keys:**  Accepts any object type as keys, providing flexibility in key selection.</li>
 *     <li>**Bit-Packed Values (BitsList):** Employs {@link BitsList} for value storage, dramatically reducing memory footprint, especially for integer-like values or flags.</li>
 *     <li>**Hash Table with Separate Chaining:**  Resolves hash collisions efficiently, maintaining performance even with a high load factor.</li>
 *     <li>**Dynamic Resizing:** Automatically adjusts internal capacity to maintain optimal performance as the map grows or shrinks.</li>
 *     <li>**Null Key Support:**  Allows for a single null key, useful for representing default or absent values.</li>
 *     <li>**Customizable Key Handling:**  Uses {@link Array.EqualHashOf} for key equality checks and hash code generation, enabling tailored key comparison logic.</li>
 *     <li>**Token-Based Iteration:**  Provides a robust and efficient iteration mechanism using tokens, ensuring safety even during concurrent read operations.</li>
 *     <li>**Cloneable:** Implements {@link Cloneable} for creating shallow copies of the map.</li>
 * </ul>
 * <p>
 * **Implementation Details:**
 * </p>
 * <p>
 * The {@link ObjectBitsMap} interface is implemented by the {@link RW} (Read-Write) class, which extends the abstract {@link R} (Read-Only) base class.
 * This design separates read and write operations, allowing for potential optimizations and clearer code organization.
 * </p>
 * <p>
 * **Use Cases:**
 * </p>
 * <p>
 * This Map is ideal for scenarios where memory efficiency is paramount, such as:
 * </p>
 * <ul>
 *     <li>Caching systems</li>
 *     <li>Data structures for embedded systems</li>
 *     <li>Representing sparse data</li>
 *     <li>Scenarios with a large number of small integer values associated with keys</li>
 * </ul>
 */
public interface ObjectBitsMap {
	
	/**
	 * <p>
	 * **Abstract Read-Only Base Class (R):** Provides Core Functionality and State Management
	 * </p>
	 * <p>
	 * This abstract class serves as the foundation for {@link ObjectBitsMap} implementations, offering a read-only view and managing the underlying data structures.
	 * It encapsulates the common state and read-related operations, promoting code reuse and a clear separation of concerns.
	 * </p>
	 * <p>
	 * **Key Responsibilities:**
	 * </p>
	 * <ul>
	 *     <li>Manages the hash table structure (buckets, hash_nexts, keys, values).</li>
	 *     <li>Implements read-only operations like {@link #size()}, {@link #contains(Object)}, {@link #tokenOf(Object)}, {@link #key(long)}, and {@link #value(long)}.</li>
	 *     <li>Provides token-based iteration for safe and efficient traversal.</li>
	 *     <li>Implements {@link #hashCode()}, {@link #equals(Object)}, {@link #clone()}, and {@link #toJSON(JsonWriter)}.</li>
	 * </ul>
	 * <p>
	 * **State Variables:**
	 * </p>
	 * <ul>
	 *     <li>{@code hasNullKey}:  Indicates whether the map contains a null key.</li>
	 *     <li>{@code nullKeyValue}: Stores the value associated with the null key.</li>
	 *     <li>{@code _buckets}:  The hash table's bucket array, used for initial hash lookup.</li>
	 *     <li>{@code hash_nexts}:  An array storing combined hash codes and next entry indices for collision chaining.</li>
	 *     <li>{@code keys}:  An array holding the keys of the map entries.</li>
	 *     <li>{@code values}: A {@link BitsList.RW} instance for storing bit-packed values.</li>
	 *     <li>{@code _count}: The total number of entries currently in the hash table (including free slots).</li>
	 *     <li>{@code _freeList}:  Index of the first entry in the free list, used for efficient removal and insertion.</li>
	 *     <li>{@code _freeCount}:  Number of free entries available in the hash table.</li>
	 *     <li>{@code _version}:  A version counter used for detecting concurrent modifications during iteration.</li>
	 *     <li>{@code equal_hash_K}:  The {@link Array.EqualHashOf} strategy for key equality and hashing.</li>
	 * </ul>
	 *
	 * @param <K> The type of keys.
	 */
	abstract class R< K > implements JsonWriter.Source, Cloneable {
		/**
		 * Indicates if the Map contains a null key.
		 */
		protected boolean                hasNullKey;
		/**
		 * Value for the null key (byte for consistency with BitsList).
		 */
		protected byte                   nullKeyValue;
		/**
		 * Hash table buckets array. Buckets are entry indices + 1 or 0 if empty.
		 */
		protected int[]                  _buckets;
		/**
		 * Array of entries: hashCode (32 bits) | next index (32 bits). 'next index' is an index in the same `hash_nexts` array forming a linked list for hash collisions.
		 */
		protected long[]                 hash_nexts;
		/**
		 * Array of keys corresponding to entries in `hash_nexts`.
		 */
		protected K[]                    keys;
		/**
		 * Bit-packed values stored using {@link BitsList.RW}.
		 */
		protected BitsList.RW            values;
		/**
		 * Total number of entries in `hash_nexts` (including used and free).
		 */
		protected int                    _count;
		/**
		 * Index of the first free entry in `hash_nexts`. -1 if no free entries, otherwise index to `hash_nexts`. Free entries are linked via 'next index' to form a free list.
		 */
		protected int                    _freeList;
		/**
		 * Number of free entries in `hash_nexts`.
		 */
		protected int                    _freeCount;
		/**
		 * Version counter to detect modifications during iteration. Incremented on structural changes.
		 */
		protected int                    _version;
		/**
		 * Strategy for key equality and hash code calculation.
		 */
		protected Array.EqualHashOf< K > equal_hash_K;
		
		/**
		 * Special marker in 'next index' to indicate the start of the free list.
		 */
		protected static final int  StartOfFreeList = -3;
		/**
		 * Mask to extract the hash code part from `hash_nexts` entries.
		 */
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		/**
		 * Mask to extract the 'next index' part from `hash_nexts` entries.
		 */
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		/**
		 * Mask to extract the index from a token.
		 */
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		/**
		 * Number of bits to shift the version to the higher part of the token.
		 */
		protected static final int  VERSION_SHIFT   = 32;
		/**
		 * Represents an invalid token value (-1).
		 */
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return {@code true} if the map contains no key-value mappings, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value mappings in this map.
		 *
		 * @return the number of key-value mappings in this map.
		 */
		public int size() {
			return _count - _freeCount + ( hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the number of key-value mappings in this map. Alias for {@link #size()}.
		 *
		 * @return the number of key-value mappings in this map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the underlying hash table (the length of `hash_nexts` array).
		 *
		 * @return the capacity of the hash table.
		 */
		public int length() {
			return hash_nexts == null ?
					0 :
					hash_nexts.length;
		}
		
		/**
		 * Checks if the map contains a mapping for the specified key.
		 *
		 * @param key key whose presence in this map is to be tested
		 * @return {@code true} if this map contains a mapping for the specified key.
		 */
		public boolean contains( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this map maps one or more keys to the specified value.
		 *
		 * @param value value whose presence in this map is to be tested
		 * @return {@code true} if this map maps one or more keys to the specified value.
		 */
		public boolean containsValue( int value ) {
			if( _count == 0 && !hasNullKey ) return false;
			return values.contains( value );
		}
		
		/**
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		public int nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Returns a token representing the entry associated with the specified key, or {@link #INVALID_TOKEN} (-1) if the key is not found.
		 * <p>
		 * Tokens are used for efficient and safe iteration. Note that a returned token may become invalid if the map is structurally modified after retrieval.
		 *
		 * @param key the key to find the token for
		 * @return a token for the key, or {@link #INVALID_TOKEN} (-1) if the key is not present.
		 * @throws ConcurrentModificationException if concurrent modifications are detected during hash traversal.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
			if( _buckets == null ) return INVALID_TOKEN;
			int hash = equal_hash_K.hashCode( key );
			int i    = _buckets[ bucketIndex( hash ) ] - 1; // Adjust bucket index to 0-based
			
			// Traverse the collision chain for the bucket
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < hash_nexts.length; ) {
				final long hash_next = hash_nexts[ i ];
				if( hash( hash_next ) == hash && equal_hash_K.equals( keys[ i ], key ) )
					return token( i ); // Key found, return its token
				i = next( hash_next ); // Move to the next entry in the collision chain
				if( hash_nexts.length <= collisionCount++ ) // Detect potential infinite loop due to concurrent modification
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN; // Key not found
		}
		
		/**
		 * Returns the first valid token in the map for iteration.
		 * <p>
		 * Starts iteration from the beginning of the hash table. Returns {@link #INVALID_TOKEN} (-1) if the map is empty.
		 *
		 * @return the first valid token, or {@link #INVALID_TOKEN} (-1) if the map is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return token( i ); // Find the first non-free entry
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN; // If no entry, check for null key
		}
		
		/**
		 * Returns the next valid token in the map, starting from the given token.
		 * <p>
		 * Used for iterating through the map entries. Returns {@link #INVALID_TOKEN} (-1) if no more entries exist or if the provided token is invalid due to structural modification.
		 *
		 * @param token the current token
		 * @return the next valid token, or {@link #INVALID_TOKEN} (-1) if no more entries exist or token is invalid.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN || version( token ) != _version ) return INVALID_TOKEN;
			for( int i = index( token ) + 1; i < _count; i++ )
				if( next( hash_nexts[ i ] ) >= -1 ) return token( i ); // Find the next non-free entry after the current token
			return hasNullKey && index( token ) < _count ?
					token( _count ) :
					INVALID_TOKEN; // Check for null key after iterating through all entries
		}
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #key(long)} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #key(long) To get the key associated with a token.
		 */
		public int unsafe_token( int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return i;
			return -1;
		}
		
		/**
		 * Retrieves the key associated with the given token.
		 *
		 * @param token the token representing the entry
		 * @return the key associated with the token, or null if token represents the null key.
		 */
		public K key( long token ) {
			return hasNullKey && index( token ) == _count ?
					null :
					keys[ index( token ) ]; // Handle null key case
		}
		
		/**
		 * Retrieves the value associated with the given token.
		 *
		 * @param token the token representing the entry
		 * @return the value associated with the token.
		 */
		public byte value( long token ) {
			return index( token ) == _count ?
					nullKeyValue :
					values.get( index( token ) ); // Handle null key value
		}
		
		/**
		 * Generates a hash code for this map based on its key-value mappings.
		 * <p>
		 * Consistent with {@link #equals(Object)}.
		 *
		 * @return the hash code of this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			// Iterate through all key-value pairs and mix their hash codes
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) ); // Mix seed with key hash
				h = Array.mix( h, Array.hash( value( token ) ) );     // Mix with value hash
				h = Array.finalizeHash( h, 2 );                 // Finalize the pair hash
				a += h; b ^= h; c *= h | 1;                  // Accumulate hash components
			}
			if( hasNullKey ) { // Include null key entry in hash calculation
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue ) );
				h = Array.finalizeHash( h, 2 );
				a += h; b ^= h; c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() ); // Finalize the map hash
		}
		
		/**
		 * Seed value for hash code generation.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map with the specified object for equality.
		 * <p>
		 * Returns {@code true} if the given object is also a {@link R} instance (or subclass) and represents the same key-value mappings.
		 *
		 * @param obj the object to compare with
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Compares this map with another {@link R} map for equality.
		 *
		 * @param other the other map to compare with
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R< K > other ) {
			if( other == this ) return true; // Same instance check
			if( other == null || hasNullKey != other.hasNullKey || // Null check and null key consistency check
			    ( hasNullKey && nullKeyValue != other.nullKeyValue ) || size() != other.size() ) return false; // Null key value and size check
			
			// Compare key-value pairs using tokens and equality checks
			for( long token = token(), t; index( token ) < _count; token = token( token ) )
				if( ( t = other.tokenOf( key( token ) ) ) == INVALID_TOKEN || value( token ) != other.value( t ) ) return false; // Check if keys and values match in both maps
			return true; // All key-value pairs are equal
		}
		
		/**
		 * Creates and returns a shallow copy of this map.
		 * <p>
		 * The keys and values themselves are not cloned.
		 *
		 * @return a shallow copy of this map.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public R< K > clone() {
			try {
				R dst = ( R ) super.clone(); // Perform shallow clone
				if( _buckets != null ) { // Clone internal arrays if initialized
					dst._buckets   = _buckets.clone();
					dst.hash_nexts = hash_nexts.clone();
					dst.keys       = keys.clone();
					dst.values     = values.clone(); // BitsList.RW is also cloneable and performs a shallow copy
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace(); // Should not happen as R implements Cloneable
				return null;
			}
		}
		
		/**
		 * Returns a JSON string representation of this map.
		 * <p>
		 * Uses the {@link #toJSON(JsonWriter)} method to generate the JSON output.
		 *
		 * @return A JSON string representing the map's contents.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the map's content to a {@link JsonWriter}.
		 * <p>
		 * Outputs either a JSON object (if keys are strings) or a JSON array of key-value objects.
		 *
		 * @param json the JsonWriter to write to
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 ); // Preallocate buffer for better performance
			long token = token();
			
			if( equal_hash_K == Array.string ) { // Optimize for string keys: output as JSON object
				json.enterObject();
				if( hasNullKey ) json.name().value( nullKeyValue );
				for( ; index( token ) < _count; token = token( token ) )
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
				
				for( ; index( token ) < _count; token = token( token ) )
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
		 * @param hash the hash code
		 * @return the bucket index within the {@code _buckets} array.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; } // Ensure positive index within bucket array bounds
		
		/**
		 * Extracts the hash code from a packed hash_next entry.
		 *
		 * @param packedEntry the packed hash_next value (hashCode | nextIndex)
		 * @return the extracted hash code.
		 */
		protected static int hash( long packedEntry ) { return ( int ) ( packedEntry >> 32 ); }
		
		/**
		 * Extracts the 'next index' from a packed hash_next entry.
		 *
		 * @param hash_next the packed hash_next value (hashCode | nextIndex)
		 * @return the extracted 'next index'.
		 */
		protected static int next( long hash_next ) { return ( int ) ( hash_next & NEXT_MASK ); }
		
		/**
		 * Creates a token from an entry index and the current version.
		 *
		 * @param index the entry index in the `hash_nexts` array
		 * @return the generated token.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index & INDEX_MASK; } // Combine version and index into a token
		
		/**
		 * Extracts the entry index from a token.
		 *
		 * @param token the token value
		 * @return the extracted entry index.
		 */
		protected int index( long token ) { return ( int ) ( token & INDEX_MASK ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token the token value
		 * @return the extracted version.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * <p>
	 * **Read-Write Implementation (RW):** Extends Read-Only Base Class for Mutability
	 * </p>
	 * <p>
	 * This class provides a read-write implementation of {@link ObjectBitsMap}, inheriting from the {@link R} base class.
	 * It adds methods for modifying the map, such as {@link #put(Object, int)}, {@link #remove(Object)}, {@link #clear()}, and resizing operations.
	 * </p>
	 * <p>
	 * **Key Features and Extensions:**
	 * </p>
	 * <ul>
	 *     <li>**Mutable Operations:** Implements methods to add, update, and remove key-value pairs.</li>
	 *     <li>**Dynamic Resizing:** Automatically resizes the hash table when necessary to maintain performance.</li>
	 *     <li>**Hash Collision Handling:**  Monitors hash collision frequency and can optionally trigger rehashing with potentially better hash functions for string keys.</li>
	 *     <li>{@code forceNewHashCodes}:  A function to dynamically update the hashing strategy, especially useful for mitigating hash collisions in string keys.</li>
	 * </ul>
	 *
	 * @param <K> The type of keys.
	 */
	class RW< K > extends R< K > {
		/**
		 * Threshold for hash collision count in a bucket before considering rehashing.
		 */
		private static final int                                                        HashCollisionThreshold = 100;
		/**
		 * Optional function to force recalculation of hash codes for keys (e.g., for string keys to improve distribution after many collisions).
		 */
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs a new empty {@link RW} map with the specified key class and bits per value item, and a default initial capacity.
		 *
		 * @param clazzK        The class of the keys.
		 * @param bits_per_item The number of bits to allocate for each value in the {@link BitsList}.
		 */
		public RW( Class< K > clazzK, int bits_per_item ) { this( clazzK, bits_per_item, 0 ); }
		
		/**
		 * Constructs a new empty {@link RW} map with the specified key class, bits per value item, and initial capacity.
		 *
		 * @param clazzK        The class of the keys.
		 * @param bits_per_item The number of bits to allocate for each value in the {@link BitsList}.
		 * @param capacity      The initial capacity of the hash table.
		 */
		public RW( Class< K > clazzK, int bits_per_item, int capacity ) { this( Array.get( clazzK ), bits_per_item, capacity ); }
		
		/**
		 * Constructs a new empty {@link RW} map with the specified key equality and hashing strategy, bits per value item, and a default initial capacity.
		 *
		 * @param equal_hash_K  The {@link Array.EqualHashOf} strategy for key handling.
		 * @param bits_per_item The number of bits to allocate for each value in the {@link BitsList}.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item ) { this( equal_hash_K, bits_per_item, 0 ); }
		
		/**
		 * Constructs a new empty {@link RW} map with the specified key equality and hashing strategy, bits per value item, and initial capacity.
		 *
		 * @param equal_hash_K  The {@link Array.EqualHashOf} strategy for key handling.
		 * @param bits_per_item The number of bits to allocate for each value in the {@link BitsList}.
		 * @param capacity      The initial capacity of the hash table.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			values            = new BitsList.RW( bits_per_item, ( byte ) 0, capacity ); // Default value 0 for BitsList
			if( capacity > 0 ) initialize( capacity ); // Initialize hash table if capacity is provided
		}
		
		/**
		 * Constructs a new empty {@link RW} map with the specified key equality and hashing strategy, bits per value item, initial capacity, and default value for {@link BitsList}.
		 *
		 * @param equal_hash_K  The {@link Array.EqualHashOf} strategy for key handling.
		 * @param bits_per_item The number of bits to allocate for each value in the {@link BitsList}.
		 * @param capacity      The initial capacity of the hash table.
		 * @param defaultValue  The default value for new entries in the {@link BitsList}.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item, int capacity, byte defaultValue ) {
			this.equal_hash_K = equal_hash_K;
			values            = new BitsList.RW( bits_per_item, defaultValue, capacity ); // Use provided default value for BitsList
			if( capacity > 0 ) initialize( capacity ); // Initialize hash table if capacity is provided
		}
		
		/**
		 * Associates the specified value with the specified key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   key with which the specified value is to be associated
		 * @param value value to be associated with the specified key
		 * @return {@code true} if a new key-value mapping was added, {@code false} if the value for an existing key was updated.
		 * @throws IllegalArgumentException        if trying to add a duplicate key using {@link #putTry(Object, int)}.
		 * @throws ConcurrentModificationException if concurrent modifications are detected during hash traversal.
		 */
		public boolean put( K key, int value ) { return tryInsert( key, value, 1 ); }
		
		/**
		 * Associates the specified value with the specified key in this map only if the key is not already present.
		 *
		 * @param key   key with which the specified value is to be associated
		 * @param value value to be associated with the specified key
		 * @return {@code true} if a new key-value mapping was added, {@code false} if the key was already present.
		 * @throws ConcurrentModificationException if concurrent modifications are detected during hash traversal.
		 */
		public boolean putNotExist( K key, int value ) { return tryInsert( key, value, 2 ); }
		
		/**
		 * Associates the specified value with the specified key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * <p>
		 * Throws {@link IllegalArgumentException} if a key already exists, unlike {@link #put(Object, int)} which overwrites.
		 *
		 * @param key   key with which the specified value is to be associated
		 * @param value value to be associated with the specified key
		 * @throws IllegalArgumentException        if an item with the same key already exists.
		 * @throws ConcurrentModificationException if concurrent modifications are detected during hash traversal.
		 */
		public void putTry( K key, int value ) { tryInsert( key, value, 0 ); }
		
		/**
		 * Removes all of the mappings from this map. The map will be empty after this call.
		 */
		public void clear() {
			if( _count == 0 && !hasNullKey ) return; // Already empty
			Arrays.fill( _buckets, 0 ); // Reset buckets
			Arrays.fill( hash_nexts, 0, _count, 0L ); // Clear hash_nexts entries
			Arrays.fill( keys, 0, _count, null );      // Clear keys
			values.clear();                           // Clear values in BitsList
			_count     = 0;                               // Reset entry count
			_freeList  = -1;                            // Reset free list
			_freeCount = 0;                            // Reset free entry count
			hasNullKey = false;                       // Remove null key flag
			_version++;                               // Increment version for modification detection
		}
		
		/**
		 * Removes the mapping for a key from this map if it is present.
		 *
		 * @param key key whose mapping is to be removed from the map
		 * @return a token representing the removed entry, or {@link #INVALID_TOKEN} (-1) if the key was not found. The token remains valid until the next modification.
		 * @throws ConcurrentModificationException if concurrent modifications are detected during hash traversal.
		 */
		public long remove( K key ) {
			if( key == null ) { // Handle null key removal
				if( !hasNullKey ) return INVALID_TOKEN; // Null key not present
				hasNullKey = false;                     // Remove null key flag
				_version++;                             // Increment version
				return token( _count );                    // Return token for null key (using _count as index, which is conceptually after the last valid index)
			}
			
			if( _buckets == null || _count == 0 ) return INVALID_TOKEN; // Map is empty
			
			int collisionCount = 0;
			int last           = -1; // Index of the last entry in the collision chain (for updating 'next' pointer)
			int hash           = equal_hash_K.hashCode( key );
			int bucketIndex    = bucketIndex( hash );
			int i              = _buckets[ bucketIndex ] - 1; // Start at the head of the collision chain for the bucket
			
			while( -1 < i ) { // Traverse the collision chain
				long hash_next = hash_nexts[ i ];
				if( hash( hash_next ) == hash && equal_hash_K.equals( keys[ i ], key ) ) { // Key found
					if( last < 0 ) _buckets[ bucketIndex ] = next( hash_next ) + 1; // If it's the head of the chain, update the bucket
					else next( hash_nexts, last, next( hash_next ) ); // Otherwise, update the 'next' pointer of the previous entry
					
					next( hash_nexts, i, StartOfFreeList - _freeList ); // Add the removed entry to the free list
					keys[ i ] = null;                                    // Clear the key reference (for GC)
					values.set1( i, values.default_value );             // Reset value in BitsList to default
					_freeList = i;                                     // Update free list head
					_freeCount++;                                     // Increment free entry count
					_version++;                                         // Increment version
					return token( i );                                    // Return token of the removed entry
				}
				last = i;                                            // Update 'last' index for chaining
				i    = next( hash_next );                                   // Move to the next entry in the collision chain
				if( hash_nexts.length < collisionCount++ )             // Detect potential infinite loop due to concurrent modification
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN; // Key not found
		}
		
		/**
		 * Ensures that the hash table can hold at least the specified number of entries without resizing.
		 * If the current capacity is less than the requested capacity, the table is resized to the next prime number greater than or equal to capacity.
		 *
		 * @param capacity the desired minimum capacity
		 * @return the new capacity of the hash table after ensuring capacity.
		 * @throws IllegalArgumentException if capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity is less than 0." );
			int currentCapacity = length();
			if( capacity <= currentCapacity ) return currentCapacity; // Already sufficient capacity
			_version++; // Increment version as structural change is about to occur
			if( _buckets == null ) return initialize( capacity ); // Initialize if buckets are null
			int newSize = Array.prime( capacity ); // Get next prime size
			resize( newSize, false );                // Resize to the new size
			return newSize;
		}
		
		/**
		 * Reduces the capacity of the hash table to the current number of key-value mappings (or a prime number greater than or equal to the count).
		 * <p>
		 * If the current capacity is already less than or equal to the count, no resizing occurs.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Reduces the capacity of the hash table to at least the specified capacity (or a prime number greater than or equal to capacity).
		 * <p>
		 * If the current capacity is already less than or equal to the specified capacity, no resizing occurs.
		 *
		 * @param capacity the desired capacity after trimming. Must be at least the current count of elements.
		 * @throws IllegalArgumentException if capacity is less than the current count.
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			int new_size        = Array.prime( capacity ); // Get next prime size for capacity
			if( currentCapacity <= new_size ) return; // No need to trim if current capacity is already smaller or equal
			
			long[]      old_hash_next = hash_nexts; // Store old arrays for copying
			K[]         old_keys      = keys;
			BitsList.RW old_values    = values.clone(); // Clone BitsList to avoid modification during copy
			int         old_count     = _count;
			_version++; // Increment version
			initialize( new_size ); // Initialize with new smaller size
			copy( old_hash_next, old_keys, old_values, old_count ); // Copy existing data to the new smaller table
		}
		
		/**
		 * Attempts to insert a key-value pair into the map.
		 * Handles different insertion behaviors:
		 * 0: Throw exception if key exists.
		 * 1: Update value if key exists, insert if not.
		 * 2: Insert only if key does not exist.
		 *
		 * @param key      the key to insert
		 * @param value    the value to associate with the key
		 * @param behavior insertion behavior code (0, 1, or 2)
		 * @return {@code true} if insertion was successful (or value updated), {@code false} if insertion was skipped due to behavior 2 and existing key.
		 * @throws IllegalArgumentException        if behavior is 0 and key already exists.
		 * @throws ConcurrentModificationException if concurrent modifications are detected during hash traversal.
		 */
		private boolean tryInsert( K key, int value, int behavior ) {
			if( key == null ) { // Handle null key insertion
				if( hasNullKey ) // Null key already exists
					switch( behavior ) {
						case 1:
							break; // Update existing null key value
						case 0:
							throw new IllegalArgumentException( "An item with the same key has already been added. Key: " + key ); // Throw exception as requested
						default:
							return false; // Do not insert (behavior 2)
					}
				hasNullKey   = true;              // Set null key flag
				nullKeyValue = ( byte ) value; // Store null key value
				_version++;                     // Increment version
				return true;                     // Insertion successful (or update)
			}
			
			if( _buckets == null ) initialize( 0 ); // Initialize hash table if not yet initialized
			long[] _hash_nexts    = hash_nexts;
			int    hash           = equal_hash_K.hashCode( key );
			int    collisionCount = 0;
			int    bucketIndex    = bucketIndex( hash );
			int    bucket         = _buckets[ bucketIndex ] - 1; // Start at the head of the collision chain for the bucket
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _hash_nexts.length; ) { // Traverse collision chain
				if( hash( _hash_nexts[ next ] ) == hash && equal_hash_K.equals( keys[ next ], key ) ) // Key already exists
					switch( behavior ) {
						case 1:
							values.set1( next, ( byte ) value ); // Update existing value
							_version++;                     // Increment version
							return true;                     // Value updated
						case 0:
							throw new IllegalArgumentException( "An item with the same key has already been added. Key: " + key ); // Throw exception as requested
						default:
							return false; // Do not insert (behavior 2)
					}
				next = next( _hash_nexts[ next ] ); // Move to the next entry in the chain
				if( _hash_nexts.length < collisionCount++ ) // Detect potential infinite loop due to concurrent modification
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			
			int index;
			if( 0 < _freeCount ) { // Use a free slot if available
				index     = _freeList;           // Get index of free slot
				_freeList = StartOfFreeList - next( _hash_nexts[ _freeList ] ); // Update free list head
				_freeCount--;                 // Decrement free entry count
			}
			else { // No free slots, need to allocate a new one
				if( _count == _hash_nexts.length ) { // Need to resize if full
					resize( Array.prime( _count * 2 ), false ); // Resize to double capacity
					bucket = _buckets[ bucketIndex = bucketIndex( hash ) ] - 1; // Recalculate bucket and chain head after resize
				}
				index = _count++; // Allocate new slot at the end
			}
			
			hash_nexts[ index ] = hash_next( hash, bucket ); // Create hash_next entry with hash and previous chain head
			keys[ index ]       = key;                     // Store the key
			values.set1( index, ( byte ) value );      // Store the value
			_buckets[ bucketIndex ] = index + 1;      // Update bucket to point to the new entry
			_version++;                             // Increment version
			
			if( HashCollisionThreshold < collisionCount && this.forceNewHashCodes != null && key instanceof String ) // Check for high collision and potential rehashing for string keys
				resize( hash_nexts.length, true ); // Resize to potentially trigger new hash codes
			return true; // Insertion successful
		}
		
		/**
		 * Resizes the hash table to a new capacity.
		 *
		 * @param new_size          the new capacity of the hash table
		 * @param forceNewHashCodes if {@code true}, force recalculation of hash codes using {@link #forceNewHashCodes} function (if provided).
		 */
		private void resize( int new_size, boolean forceNewHashCodes ) {
			_version++; // Increment version as structural change
			long[]    new_hash_next = Arrays.copyOf( hash_nexts, new_size ); // Create new arrays with new size
			K[]       new_keys      = Arrays.copyOf( keys, new_size );
			final int count         = _count;
			
			if( forceNewHashCodes && this.forceNewHashCodes != null ) { // Optionally recalculate hash codes
				equal_hash_K = this.forceNewHashCodes.apply( equal_hash_K ); // Apply new hashing strategy
				for( int i = 0; i < count; i++ )
					if( next( new_hash_next[ i ] ) >= -2 ) // Process only non-free entries
						hash( new_hash_next, i, equal_hash_K.hashCode( keys[ i ] ) ); // Update hash code in hash_next entry
			}
			
			_buckets = new int[ new_size ]; // Create new bucket array
			for( int i = 0; i < count; i++ )
				if( next( new_hash_next[ i ] ) > -2 ) { // Rehash only non-free entries
					int bucketIndex = bucketIndex( hash( new_hash_next[ i ] ) ); // Calculate new bucket index
					next( new_hash_next, i, _buckets[ bucketIndex ] - 1 );   // Update 'next' pointer to previous bucket head
					_buckets[ bucketIndex ] = i + 1;                        // Set bucket head to current entry
				}
			
			hash_nexts = new_hash_next; // Replace old arrays with new ones
			keys       = new_keys;
			// No need to reassign values as it's a reference to BitsList.RW
		}
		
		/**
		 * Initializes the hash table with a given capacity.
		 *
		 * @param capacity the initial capacity of the hash table.
		 * @return the prime number capacity that was actually used for initialization.
		 */
		private int initialize( int capacity ) {
			capacity   = Array.prime( capacity ); // Get the next prime number for capacity
			_buckets   = new int[ capacity ];     // Initialize buckets array
			hash_nexts = new long[ capacity ];  // Initialize hash_nexts array
			keys       = equal_hash_K.copyOf( null, capacity ); // Initialize keys array
			_freeList  = -1;                  // Initialize free list to empty
			return capacity;
		}
		
		/**
		 * Copies data from old hash table arrays to the newly resized arrays.
		 *
		 * @param old_hash_next old hash_nexts array
		 * @param old_keys      old keys array
		 * @param old_values    old values (BitsList)
		 * @param old_count     number of valid entries in old arrays
		 */
		private void copy( long[] old_hash_next, K[] old_keys, BitsList.RW old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( next( old_hash_next[ i ] ) < -1 ) continue; // Skip free entries in old table
				hash_nexts[ new_count ] = old_hash_next[ i ];   // Copy hash_next entry
				keys[ new_count ]       = old_keys[ i ];             // Copy key
				values.set1( new_count, old_values.get( i ) ); // Copy value from BitsList
				int bucketIndex = bucketIndex( hash( old_hash_next[ i ] ) ); // Calculate new bucket index
				next( hash_nexts, new_count, _buckets[ bucketIndex ] - 1 ); // Chain to previous bucket head
				_buckets[ bucketIndex ] = new_count + 1;                  // Set new bucket head
				new_count++;                                            // Increment new entry count
			}
			_count     = new_count;     // Update total entry count
			_freeCount = 0;         // Reset free entry count as table is compacted
		}
		
		/**
		 * Creates a packed hash_next entry (hashCode | nextIndex).
		 *
		 * @param hash the hash code
		 * @param next the next index
		 * @return the packed hash_next value.
		 */
		private static long hash_next( int hash, int next ) {
			return ( long ) hash << 32 | next & NEXT_MASK;
		}
		
		/**
		 * Sets the 'next index' part of a hash_next entry in the destination array.
		 *
		 * @param dst   the destination hash_nexts array
		 * @param index the index in the array to modify
		 * @param next  the new 'next index' value
		 */
		private static void next( long[] dst, int index, int next ) {
			dst[ index ] = dst[ index ] & HASH_CODE_MASK | next & NEXT_MASK;
		}
		
		/**
		 * Sets the hash code part of a hash_next entry in the destination array.
		 *
		 * @param dst   the destination hash_nexts array
		 * @param index the index in the array to modify
		 * @param hash  the new hash code value
		 */
		private static void hash( long[] dst, int index, int hash ) {
			dst[ index ] = dst[ index ] & NEXT_MASK | ( long ) hash << 32;
		}
		
		/**
		 * Static instance of EqualHashOf for RW class itself (likely for internal use or extensions).
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Returns a default {@link Array.EqualHashOf} instance for {@link RW} class.
	 *
	 * @param <K> The key type.
	 * @return a default {@link Array.EqualHashOf} for {@link RW}.
	 */
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() { return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT; }
}