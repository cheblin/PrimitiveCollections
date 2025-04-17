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
 * This interface defines a generic map designed for storing key-value pairs where values are compactly stored as bits.
 * It uses a hash table with separate chaining for efficient collision handling and dynamic resizing to adapt to varying data sizes.
 * </p>
 * <p>
 * **Core Features:**
 * </p>
 * <ul>
 *     <li>**Generic Keys:** Supports any object type as keys, offering flexibility in key selection.</li>
 *     <li>**Bit-Packed Values:** Utilizes {@link BitsList} for value storage, significantly reducing memory usage, ideal for small integer values or flags.</li>
 *     <li>**Hash Table with Separate Chaining:** Efficiently resolves hash collisions, maintaining performance under high load factors.</li>
 *     <li>**Dynamic Resizing:** Automatically adjusts internal capacity to ensure optimal performance as the map grows or shrinks.</li>
 *     <li>**Null Key Support:** Allows a single null key for representing default or absent values.</li>
 *     <li>**Customizable Key Handling:** Employs {@link Array.EqualHashOf} for key equality and hash code generation, enabling custom comparison logic.</li>
 *     <li>**Token-Based Iteration:** Provides safe and efficient iteration using tokens, protecting against concurrent modification issues.</li>
 *     <li>**Cloneable:** Implements {@link Cloneable} for creating shallow copies of the map.</li>
 * </ul>
 * <p>
 * **Implementation Details:**
 * </p>
 * <p>
 * The {@link ObjectBitsMap} interface is implemented by the {@link RW} (Read-Write) class, which extends the abstract {@link R} (Read-Only) base class.
 * This separation of read and write operations enhances code clarity and enables potential optimizations.
 * </p>
 * <p>
 * **Use Cases:**
 * </p>
 * <p>
 * This map is well-suited for memory-constrained environments, including:
 * </p>
 * <ul>
 *     <li>Caching systems requiring compact storage.</li>
 *     <li>Data structures in embedded systems.</li>
 *     <li>Representation of sparse data.</li>
 *     <li>Applications with many small integer values tied to keys.</li>
 * </ul>
 */
public interface ObjectBitsMap {
	
	/**
	 * <p>
	 * **Abstract Read-Only Base Class (R):** Core Functionality and State Management
	 * </p>
	 * <p>
	 * This abstract class provides the foundation for {@link ObjectBitsMap} implementations, offering read-only operations and managing internal data structures.
	 * It encapsulates shared state and read-related functionality, promoting code reuse and separation of concerns.
	 * </p>
	 * <p>
	 * **Key Responsibilities:**
	 * </p>
	 * <ul>
	 *     <li>Maintains the hash table structure (buckets, hash_nexts, keys, values).</li>
	 *     <li>Implements read operations such as {@link #size()}, {@link #contains(Object)}, {@link #tokenOf(Object)}, {@link #key(long)}, and {@link #value(long)}.</li>
	 *     <li>Supports token-based iteration for safe and efficient traversal.</li>
	 *     <li>Provides {@link #hashCode()}, {@link #equals(Object)}, {@link #clone()}, and {@link #toJSON(JsonWriter)} for standard operations.</li>
	 * </ul>
	 * <p>
	 * **State Variables:**
	 * </p>
	 * <ul>
	 *     <li>{@code hasNullKey}: Indicates whether a null key is present.</li>
	 *     <li>{@code nullKeyValue}: Stores the value associated with the null key (as a byte).</li>
	 *     <li>{@code _buckets}: Hash table bucket array, holding entry indices (1-based) or 0 if empty.</li>
	 *     <li>{@code hash_nexts}: Array storing packed entries (hash code and next index) for collision chaining.</li>
	 *     <li>{@code keys}: Array of keys corresponding to entries in {@code hash_nexts}.</li>
	 *     <li>{@code values}: A {@link BitsList.RW} instance for bit-packed value storage.</li>
	 *     <li>{@code _count}: Total number of entries in {@code hash_nexts}, including used and free slots.</li>
	 *     <li>{@code _freeList}: Index of the first free entry in {@code hash_nexts}, forming a linked list for free slots.</li>
	 *     <li>{@code _freeCount}: Number of free entries in {@code hash_nexts}.</li>
	 *     <li>{@code _version}: Version counter for detecting structural modifications during iteration.</li>
	 *     <li>{@code equal_hash_K}: {@link Array.EqualHashOf} strategy for key equality and hashing.</li>
	 * </ul>
	 *
	 * @param <K> The type of keys.
	 */
	abstract class R< K > implements JsonWriter.Source, Cloneable {
		/**
		 * Indicates if the map contains a null key.
		 */
		protected boolean                hasNullKey;
		/**
		 * Value for the null key, stored as a byte for consistency with {@link BitsList}.
		 */
		protected byte                   nullKeyValue;
		/**
		 * Hash table buckets array, where each bucket holds an entry index (1-based) or 0 if empty.
		 */
		protected int[]                  _buckets;
		/**
		 * Array of packed entries: high 32 bits for hash code, low 32 bits for next index, forming a linked list for collision resolution.
		 */
		protected long[]                 hash_nexts;
		/**
		 * Array of keys, aligned with entries in {@code hash_nexts}.
		 */
		protected K[]                    keys;
		/**
		 * Bit-packed values stored in a {@link BitsList.RW} instance.
		 */
		protected BitsList.RW            values;
		/**
		 * Total number of entries in {@code hash_nexts}, including used and free slots.
		 */
		protected int                    _count;
		/**
		 * Index of the first free entry in {@code hash_nexts}, or -1 if none. Free entries are linked via next indices.
		 */
		protected int                    _freeList;
		/**
		 * Number of free entries in {@code hash_nexts}.
		 */
		protected int                    _freeCount;
		/**
		 * Version counter for detecting structural modifications during iteration.
		 */
		protected int                    _version;
		/**
		 * Strategy for key equality and hash code computation.
		 */
		protected Array.EqualHashOf< K > equal_hash_K;
		
		/**
		 * Marker for the start of the free list in 'next index' fields.
		 */
		protected static final int  StartOfFreeList = -3;
		/**
		 * Mask to extract the hash code from packed {@code hash_nexts} entries.
		 */
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		/**
		 * Mask to extract the next index from packed {@code hash_nexts} entries.
		 */
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		/**
		 * Index used to represent the null key in tokens.
		 */
		protected static final int  NULL_KEY_INDEX  = 0x7FFF_FFFF;
		
		/**
		 * Number of bits to shift the version in a token's high bits.
		 */
		protected static final int  VERSION_SHIFT = 32;
		/**
		 * Represents an invalid token value.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return {@code true} if the map has no key-value mappings, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value mappings in the map.
		 *
		 * @return The number of key-value mappings.
		 */
		public int size() {
			return _count - _freeCount + (
					hasNullKey ?
							1 :
							0 );
		}
		
		/**
		 * Alias for {@link #size()}.
		 *
		 * @return The number of key-value mappings.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the hash table (length of {@code hash_nexts}).
		 *
		 * @return The hash table capacity.
		 */
		public int length() {
			return hash_nexts == null ?
					0 :
					hash_nexts.length;
		}
		
		/**
		 * Checks if the map contains a mapping for the specified key.
		 *
		 * @param key The key to check for.
		 * @return {@code true} if the key is mapped, {@code false} otherwise.
		 */
		public boolean contains( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 *
		 * @param value The value to check for.
		 * @return {@code true} if the value is mapped to one or more keys, {@code false} otherwise.
		 */
		public boolean containsValue( int value ) { return hasNullKey && nullKeyValue == value || 0 < _count && values.contains( value ); }
		
		/**
		 * Checks if the map contains a null key.
		 *
		 * @return {@code true} if a null key is present, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the null key.
		 *
		 * @return The value for the null key, or undefined if no null key exists.
		 */
		public int nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Returns a token for the specified key, or {@link #INVALID_TOKEN} if not found.
		 * <p>
		 * Tokens combine an entry index and map version for safe iteration. Tokens may become invalid after structural modifications.
		 *
		 * @param key The key to find.
		 * @return A token for the key, or {@link #INVALID_TOKEN} if absent.
		 * @throws ConcurrentModificationException If concurrent modifications are detected.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
					token( NULL_KEY_INDEX ) :
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
		 * Returns the first valid token for iteration.
		 * <p>
		 * Begins iteration from the start of the hash table. Returns {@link #INVALID_TOKEN} if the map is empty.
		 *
		 * @return The first token, or {@link #INVALID_TOKEN} if empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return token( i ); // Find the first non-free entry
			return hasNullKey ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN; // If no entry, check for null key
		}
		
		/**
		 * Returns the next valid token for iteration.
		 * <p>
		 * Continues iteration from the given token. Returns {@link #INVALID_TOKEN} if no further entries exist or if the token is invalid.
		 *
		 * @param token The current token.
		 * @return The next token, or {@link #INVALID_TOKEN} if none remain or token is invalid.
		 * @throws ConcurrentModificationException If the map was modified since the token was issued.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			int i = index( token );
			if( i == NULL_KEY_INDEX ) return INVALID_TOKEN;
			
			if( 0 < _count - _freeCount )
				for( i++; i < _count; i++ )
					if( next( hash_nexts[ i ] ) >= -1 ) return token( i ); // Find the next non-free entry after the current token
			
			return hasNullKey && index( token ) < _count ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN; // Check for null key after iterating through all entries
		}
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over non-null keys.
		 * <p>
		 * Starts with {@code unsafe_token(-1)} and passes returned tokens back to continue. Returns -1 when iteration ends.
		 * Excludes the null key; check {@link #hasNullKey()} separately. <strong>WARNING:</strong> Unsafe for concurrent modifications,
		 * which may cause skipped entries or undefined behavior. Use only when modifications are guaranteed not to occur.
		 *
		 * @param token The previous token, or -1 to start.
		 * @return The next token (index) for a non-null key, or -1 if none remain.
		 */
		public int unsafe_token( int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return i;
			return -1;
		}
		
		/**
		 * Checks if the token represents the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token is for the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Retrieves the key for the given token.
		 *
		 * @param token The token representing an entry.
		 * @return The associated key, or null if the token is for the null key.
		 */
		public K key( long token ) {
			return isKeyNull( token ) ?
					null :
					keys[ index( token ) ];
		}
		
		/**
		 * Returns the number of bits used per value in the {@link BitsList}.
		 *
		 * @return The number of bits per value (1 to 7).
		 */
		public int bits_per_value() { return values.bits_per_item(); }
		
		/**
		 * Retrieves the value for the given token.
		 *
		 * @param token The token representing an entry.
		 * @return The associated value.
		 */
		public byte value( long token ) {
			return isKeyNull( token ) ?
					nullKeyValue :
					values.get( index( token ) ); // Handle null key value
		}
		
		/**
		 * Computes a hash code for the map based on its key-value mappings.
		 * <p>
		 * Consistent with {@link #equals(Object)}.
		 *
		 * @return The map's hash code.
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
		 * Checks if this map equals another object.
		 * <p>
		 * Returns {@code true} if the object is a {@link R} instance with identical key-value mappings.
		 *
		 * @param obj The object to compare.
		 * @return {@code true} if equal, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Checks if this map equals another {@link R} map.
		 *
		 * @param other The map to compare.
		 * @return {@code true} if equal, {@code false} otherwise.
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
		 * Creates a shallow copy of the map.
		 * <p>
		 * Keys and values are not cloned.
		 *
		 * @return A shallow copy of the map.
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
		 * Returns a JSON string representation of the map.
		 *
		 * @return A JSON string of the map's contents.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the map's contents to a {@link JsonWriter}.
		 * <p>
		 * Outputs a JSON object for string keys or an array of key-value objects for other key types.
		 *
		 * @param json The {@link JsonWriter} to write to.
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
		 * Computes the bucket index for a hash code.
		 *
		 * @param hash The hash code.
		 * @return The bucket index in {@code _buckets}.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; } // Ensure positive index within bucket array bounds
		
		/**
		 * Extracts the hash code from a packed hash_next entry.
		 *
		 * @param packedEntry The packed entry (hash code | next index).
		 * @return The hash code.
		 */
		protected static int hash( long packedEntry ) { return ( int ) ( packedEntry >> 32 ); }
		
		/**
		 * Extracts the next index from a packed hash_next entry.
		 *
		 * @param hash_next The packed entry (hash code | next index).
		 * @return The next index.
		 */
		protected static int next( long hash_next ) { return ( int ) ( hash_next & NEXT_MASK ); }
		
		/**
		 * Creates a token from an entry index and current version.
		 *
		 * @param index The entry index in {@code hash_nexts}.
		 * @return The token (version << 32 | index).
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index; } // Combine version and index into a token
		
		/**
		 * Extracts the entry index from a token.
		 *
		 * @param token the token value
		 * @return the extracted entry index.
		 */
		protected int index( long token ) { return ( int ) token; }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token The token.
		 * @return The version.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * <p>
	 * **Read-Write Implementation (RW):** Mutable Extension of Read-Only Base Class
	 * </p>
	 * <p>
	 * This class implements a mutable {@link ObjectBitsMap}, extending {@link R} to add modification operations.
	 * It supports adding, updating, and removing key-value pairs, with dynamic resizing to maintain performance.
	 * </p>
	 * <p>
	 * **Key Features:**
	 * </p>
	 * <ul>
	 *     <li>**Mutable Operations:** Supports {@link #put(Object, long)}, {@link #remove(Object)}, {@link #clear()}, and capacity management.</li>
	 *     <li>**Dynamic Resizing:** Adjusts hash table size based on load, using prime numbers for optimal bucket counts.</li>
	 *     <li>**Hash Collision Handling:** Monitors collisions and supports rehashing with updated hash functions for string keys via {@code forceNewHashCodes}.</li>
	 *     <li>**Null Key Support:** Allows setting a value for the null key with a dedicated {@link #put(long)} method.</li>
	 * </ul>
	 *
	 * @param <K> The type of keys.
	 */
	class RW< K > extends R< K > {
		/**
		 * Threshold for hash collisions in a bucket before considering rehashing.
		 */
		private static final int                                                        HashCollisionThreshold = 100;
		/**
		 * Optional function to update the hashing strategy, used to mitigate collisions for string keys.
		 */
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs an empty map with the specified key class, bits per value, and default capacity.
		 *
		 * @param clazzK        The key class.
		 * @param bits_per_item Number of bits per value (1 to 7).
		 */
		public RW( Class< K > clazzK, int bits_per_item ) { this( clazzK, bits_per_item, 0 ); }
		
		/**
		 * Constructs an empty map with the specified key class, bits per value, and initial capacity.
		 *
		 * @param clazzK        The key class.
		 * @param bits_per_item Number of bits per value (1 to 7).
		 * @param capacity      Initial hash table capacity.
		 */
		public RW( Class< K > clazzK, int bits_per_item, int capacity ) { this( Array.get( clazzK ), bits_per_item, capacity ); }
		
		/**
		 * Constructs an empty map with the specified key equality/hashing strategy, bits per value, and default capacity.
		 *
		 * @param equal_hash_K  The key equality and hashing strategy.
		 * @param bits_per_item Number of bits per value (1 to 7).
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item ) { this( equal_hash_K, bits_per_item, 0 ); }
		
		/**
		 * Constructs an empty map with the specified key equality/hashing strategy, bits per value, and initial capacity.
		 *
		 * @param equal_hash_K  The key equality and hashing strategy.
		 * @param bits_per_item Number of bits per value (1 to 7).
		 * @param capacity      Initial hash table capacity.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			values            = new BitsList.RW( bits_per_item, ( byte ) 0, capacity ); // Default value 0 for BitsList
			if( capacity > 0 ) initialize( Array.prime( capacity ) ); // Initialize hash table if capacity is provided
		}
		
		/**
		 * Constructs an empty map with the specified key equality/hashing strategy, bits per value, initial capacity, and default value.
		 *
		 * @param equal_hash_K  The key equality and hashing strategy.
		 * @param bits_per_item Number of bits per value (1 to 7).
		 * @param capacity      Initial hash table capacity.
		 * @param defaultValue  Default value for new entries in {@link BitsList}.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int bits_per_item, int capacity, byte defaultValue ) {
			this.equal_hash_K = equal_hash_K;
			values            = new BitsList.RW( bits_per_item, defaultValue, capacity ); // Use provided default value for BitsList
			if( capacity > 0 ) initialize( Array.prime( capacity ) ); // Initialize hash table if capacity is provided
		}
		
		/**
		 * Removes all mappings from the map, leaving it empty.
		 */
		public void clear() {
			_version++;                               // Increment version for modification detection
			hasNullKey = false;                       // Remove null key flag
			if( _count == 0 ) return; // Already empty
			Arrays.fill( _buckets, 0 ); // Reset buckets
			Arrays.fill( hash_nexts, 0, _count, 0L ); // Clear hash_nexts entries
			values.clear();                           // Clear values in BitsList
			_count     = 0;                               // Reset entry count
			_freeList  = -1;                            // Reset free list
			_freeCount = 0;                            // Reset free entry count
		}
		
		/**
		 * Removes the mapping for the specified key, if present.
		 *
		 * @param key The key to remove.
		 * @return A token for the removed entry, or {@link #INVALID_TOKEN} if not found. Valid until the next modification.
		 * @throws ConcurrentModificationException If concurrent modifications are detected.
		 */
		public long remove( K key ) {
			if( key == null ) { // Handle null key removal
				if( !hasNullKey ) return INVALID_TOKEN; // Null key not present
				hasNullKey = false;                     // Remove null key flag
				_version++;                             // Increment version
				return token( NULL_KEY_INDEX );                    // Return token for null key (using _count as index, which is conceptually after the last valid index)
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
		 * Ensures the hash table can hold at least the specified capacity without resizing.
		 * <p>
		 * Resizes to the next prime number >= capacity if needed.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity after resizing, or current capacity if sufficient.
		 * @throws IllegalArgumentException If capacity is negative.
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
		 * Reduces the hash table capacity to the current number of mappings.
		 * <p>
		 * Uses a prime number >= current count.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Reduces the hash table capacity to at least the specified capacity.
		 * <p>
		 * Uses a prime number >= capacity. No action if current capacity is already <= specified capacity.
		 *
		 * @param capacity The desired capacity (>= current count).
		 */
		public void trim( int capacity ) {
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
		 * Sets or updates the value for the null key.
		 *
		 * @param value The value for the null key (masked to fit {@link BitsList} bits).
		 * @return {@code true} if the null key was newly set or updated, {@code false} if unchanged.
		 */
		public boolean put( long value ) {
			boolean b = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = ( byte ) ( value & values.mask );
			_version++;
			return b;
		}
		
		public void putAll( RW< ? extends K > src ) {
			for( int token = -1; ( token = src.unsafe_token( token ) ) != -1; )
			     put( src.key( token ), src.value( token ) );
		}
		
		/**
		 * Associates the specified value with the specified key.
		 * <p>
		 * Replaces the old value if the key exists; otherwise, adds a new mapping.
		 *
		 * @param key   The key to map.
		 * @param value The value to associate (masked to fit {@link BitsList} bits).
		 * @return {@code true} if a new mapping was added, {@code false} if an existing value was updated.
		 * @throws ConcurrentModificationException If concurrent modifications are detected.
		 */
		public boolean put( K key, long value ) {
			if( key == null ) return put( value );
			
			if( _buckets == null ) initialize( 7 ); // Initialize hash table if not yet initialized
			long[] _hash_nexts    = hash_nexts;
			int    hash           = equal_hash_K.hashCode( key );
			int    collisionCount = 0;
			int    bucketIndex    = bucketIndex( hash );
			int    bucket         = _buckets[ bucketIndex ] - 1; // Start at the head of the collision chain for the bucket
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _hash_nexts.length; ) { // Traverse collision chain
				if( hash( _hash_nexts[ next ] ) == hash && equal_hash_K.equals( keys[ next ], key ) ) // Key already exists
				{
					values.set1( next, ( byte ) value ); // Update existing value
					_version++;                     // Increment version
					return false;                     // Value updated
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
		 * Resizes the hash table to the specified capacity.
		 *
		 * @param new_size          The new capacity (must be prime).
		 * @param forceNewHashCodes If {@code true}, applies {@link #forceNewHashCodes} to update hash codes (for string keys).
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
		 * Initializes the hash table with the specified capacity.
		 *
		 * @param capacity The initial capacity (adjusted to next prime).
		 * @return The actual capacity used.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets   = new int[ capacity ];     // Initialize buckets array
			hash_nexts = new long[ capacity ];  // Initialize hash_nexts array
			keys       = equal_hash_K.copyOf( null, capacity ); // Initialize keys array
			_freeList  = -1;
			_count     = 0;
			_freeCount = 0;
			return capacity;
		}
		
		/**
		 * Copies data from old arrays to a newly resized table.
		 *
		 * @param old_hash_next Old hash_nexts array.
		 * @param old_keys      Old keys array.
		 * @param old_values    Old values ({@link BitsList}).
		 * @param old_count     Number of valid entries in old arrays.
		 */
		private void copy( long[] old_hash_next, K[] old_keys, BitsList.RW old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				final long hn = old_hash_next[ i ];
				if( next( hn ) < -1 ) continue; // Skip free entries in old table
				
				keys[ new_count ] = old_keys[ i ];             // Copy key
				values.set1( new_count, old_values.get( i ) ); // Copy value from BitsList
				
				int bucketIndex = bucketIndex( hash( hn ) ); // Calculate new bucket index
				hash_nexts[ new_count ] = hn & 0xFFFF_FFFF_0000_0000L | _buckets[ bucketIndex ] - 1; // Chain to previous bucket head
				
				_buckets[ bucketIndex ] = new_count + 1;                  // Set new bucket head
				new_count++;                                            // Increment new entry count
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		/**
		 * Creates a packed hash_next entry from hash code and next index.
		 *
		 * @param hash The hash code.
		 * @param next The next index.
		 * @return The packed entry (hash << 32 | next).
		 */
		private static long hash_next( int hash, int next ) {
			return ( long ) hash << 32 | next & NEXT_MASK;
		}
		
		/**
		 * Sets the next index in a hash_next entry.
		 *
		 * @param dst   The hash_nexts array.
		 * @param index The entry index.
		 * @param next  The new next index.
		 */
		private static void next( long[] dst, int index, int next ) {
			dst[ index ] = dst[ index ] & HASH_CODE_MASK | next & NEXT_MASK;
		}
		
		/**
		 * Sets the hash code in a hash_next entry.
		 *
		 * @param dst   The hash_nexts array.
		 * @param index The entry index.
		 * @param hash  The new hash code.
		 */
		private static void hash( long[] dst, int index, int hash ) {
			dst[ index ] = dst[ index ] & NEXT_MASK | ( long ) hash << 32;
		}
		
		/**
		 * Default {@link Array.EqualHashOf} instance for {@link RW}.
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
		@Override public RW< K > clone() { return ( RW< K > ) super.clone(); }
	}
	
	/**
	 * Returns a default {@link Array.EqualHashOf} for {@link RW}.
	 *
	 * @param <K> The key type.
	 * @return The default {@link Array.EqualHashOf} for {@link RW}.
	 */
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() { return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT; }
}