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
 *     <li>**Bit-Packed Values:** Utilizes {@link BitsList} for value storage, significantly reducing memory usage, ideal for small primitive values or flags.</li>
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
 *     <li>Applications with many small primitive values tied to keys.</li>
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
	 *     <li>Maintains the hash table structure (buckets, hash, links, keys, values).</li>
	 *     <li>Implements read operations such as {@link #size()}, {@link #containsKey(Object)}, {@link #tokenOf(Object)}, {@link #key(long)}, and {@link #value(long)}.</li>
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
	 *     <li>{@code hash}: Array storing hash codes for each entry.</li>
	 *     <li>{@code links}: Array storing the 'next' index in collision chains (0-based indices).</li>
	 *     <li>{@code keys}: Array of keys corresponding to entries.</li>
	 *     <li>{@code values}: A {@link BitsList.RW} instance for bit-packed value storage.</li>
	 *     <li>{@code _lo_Size}: Number of active entries in the low region (0 to {@code _lo_Size}-1).</li>
	 *     <li>{@code _hi_Size}: Number of active entries in the high region ({@code keys.length} - {@code _hi_Size} to {@code keys.length}-1).</li>
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
		protected boolean     hasNullKey;
		/**
		 * Value for the null key, stored as a byte for consistency with {@link BitsList}.
		 */
		protected byte        nullKeyValue;
		/**
		 * Hash table buckets array, where each bucket holds an entry index (1-based) or 0 if empty.
		 */
		protected int[]       _buckets;
		/**
		 * Array of hash codes for each entry.
		 */
		protected int[]       hash;
		/**
		 * Array storing the 'next' index in collision chains (0-based indices).
		 */
		protected int[]       links;
		/**
		 * Array of keys, aligned with entries.
		 */
		protected K[]         keys;
		/**
		 * Bit-packed values stored in a {@link BitsList.RW} instance.
		 */
		protected BitsList.RW values;
		
		/**
		 * Number of active entries in the low region (0 to _lo_Size-1).
		 */
		protected int _lo_Size;
		/**
		 * Number of active entries in the high region (keys.length - _hi_Size to keys.length-1).
		 */
		protected int _hi_Size;
		
		/**
		 * Version counter for detecting structural modifications during iteration.
		 */
		protected int                    _version;
		/**
		 * Strategy for key equality and hash code computation.
		 */
		protected Array.EqualHashOf< K > equal_hash_K;
		
		/**
		 * Index used to represent the null key in tokens.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * Number of bits to shift the version in a token's high bits.
		 */
		protected static final int  VERSION_SHIFT = 32;
		/**
		 * Represents an invalid token value.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Returns the total number of active non-null entries in the map.
		 * This is the sum of entries in the low and high regions.
		 *
		 * @return The current count of non-null entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
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
			return _count() + (
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
		 * Returns the current capacity of the hash table (length of {@code keys} array).
		 *
		 * @return The hash table capacity.
		 */
		public int length() {
			return keys == null ?
			       0 :
			       keys.length;
		}
		
		/**
		 * Checks if the map contains a mapping for the specified key.
		 *
		 * @param key The key to check for.
		 * @return {@code true} if the key is mapped, {@code false} otherwise.
		 */
		public boolean containsKey( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 *
		 * @param value The value to check for.
		 * @return {@code true} if the value is mapped to one or more keys, {@code false} otherwise.
		 */
		public boolean containsValue( int value ) { return hasNullKey && nullKeyValue == value || 0 < _count() && values.contains( value ); }
		
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
		 * Returns the first valid token for iteration.
		 * <p>
		 * Begins iteration from the start of the hash table. Returns {@link #INVALID_TOKEN} if the map is empty.
		 *
		 * @return The first token, or {@link #INVALID_TOKEN} if empty.
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
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 * Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #key(long)} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * set is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 * <p>
		 * Iteration order: First, entries in the {@code lo Region} (0 to {@code _lo_Size-1}),
		 * then entries in the {@code hi Region} ({@code keys.length - _hi_Size} to {@code keys.length-1}).
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #key(long) To get the key associated with a token.
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
		public int bits_per_value() { return values.bits_per_item; }
		
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
		 * Creates a token from an entry index and current version.
		 *
		 * @param index The entry index in {@code keys} or {@link #NULL_KEY_INDEX}.
		 * @return The token (version << 32 | index).
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); } // Combine version and index into a token
		
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
			values            = new BitsList.RW( bits_per_item, defaultValue, capacity ); // Default value for BitsList
			if( capacity > 0 ) initialize( Array.prime( capacity ) ); // Initialize hash table if capacity is provided
		}
		
		/**
		 * Removes all mappings from the map, leaving it empty.
		 */
		public void clear() {
			_version++;                               // Increment version for modification detection
			hasNullKey = false;                       // Remove null key flag
			if( _count() == 0 ) return; // Already empty
			Arrays.fill( _buckets, 0 ); // Reset buckets
			Arrays.fill( keys, null ); // clear refs
			// hash, links, and keys arrays are implicitly cleared by resetting _lo_Size and _hi_Size
			_lo_Size = 0;
			_hi_Size = 0;
			values.clear();                           // Clear values in BitsList
		}
		
		/**
		 * Removes the mapping for the specified key, if present.
		 *
		 * @param key The key to remove.
		 * @return true if remove entry or false no mapping was found for the key.
		 * @throws ConcurrentModificationException If concurrent modifications are detected.
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
		 * Relocates an entry's key, hash, link, and value from a source index ({@code src}) to a destination index ({@code dst})
		 * within the internal arrays. This method is crucial for compaction during removal operations.
		 * <p>
		 * After moving the data, this method updates any existing pointers (either from a hash bucket in
		 * {@code _buckets} or from a {@code links} link in a collision chain) that previously referenced
		 * {@code src} to now correctly reference {@code dst}.
		 *
		 * @param src The index of the entry to be moved (its current location).
		 * @param dst The new index where the entry's data will be placed.
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
		public void trim() { trim( _count() ); }
		
		/**
		 * Reduces the hash table capacity to at least the specified capacity.
		 * <p>
		 * Uses a prime number >= capacity. No action if current capacity is already <= specified capacity.
		 *
		 * @param capacity The desired capacity (>= current count).
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			capacity = Array.prime( Math.max( capacity, _count() ) ); // Ensure capacity is at least current count and prime
			if( currentCapacity <= capacity ) return; // No need to trim if current capacity is already smaller or equal
			
			resize( capacity, false ); // Resize to the new smaller size
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
		
		public void putAll( ObjectBitsMap.RW< ? extends K > src ) {
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
				// Bucket is not empty, 'index' points to an existing entry
				if( _lo_Size <= index ) { // Existing entry is in {@code hi Region}
					if( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) { // Key matches existing {@code hi Region} entry
						values.set1( index, value ); // Update value
						_version++;
						return false; // Key was not new
					}
				}
				else  // Existing entry is in {@code lo Region} (collision chain)
				{
					int collisions = 0;
					for( int next = index; ; ) {
						if( this.hash[ next ] == hash && equal_hash_K.equals( keys[ next ], key ) ) {
							values.set1( next, value );// Update value
							_version++; // Increment version as value was modified
							return false;// Key was not new
						}
						if( _lo_Size <= next ) break; // Reached a terminal node in hi Region
						next = links[ next ]; // Move to next in chain
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
				}
				
				
				// Key is new, and a collision occurred (bucket was not empty). Place new entry in {@code lo Region}.
				if( links.length == ( dst_index = _lo_Size++ ) ) // If links array needs resize, and assign new index
					// Resize links array, cap at keys.length to avoid unnecessary large array
					links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index; // Link new entry to the previous head of the chain
			}
			
			
			this.hash[ dst_index ] = hash; // Store hash code
			keys[ dst_index ]      = key;     // Store key
			values.set1( dst_index, ( byte ) value ); // Store value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to point to the new entry (1-based)
			_version++; // Increment version
			
			return true; // New mapping added
		}
		
		/**
		 * Resizes the hash table to the specified capacity.
		 * This method rebuilds the hash table structure based on the new size.
		 * All existing entries are rehashed and re-inserted into the new, larger structure.
		 * This operation increments the set's version.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resize.
		 */
		private int resize( int newSize, boolean forceNewHashCodes ) {
			_version++;
			
			// Store old data before re-initializing
			K[]         old_keys    = keys;
			int[]       old_hash    = hash;
			BitsList.RW old_values  = values.clone(); // Clone values to copy them over
			int         old_lo_Size = _lo_Size;
			int         old_hi_Size = _hi_Size;
			
			// Re-initialize with new capacity (this clears _buckets, resets _lo_Size, _hi_Size)
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
		 * Initializes the hash table with the specified capacity.
		 *
		 * @param capacity The initial capacity (adjusted to next prime).
		 * @return The actual capacity used.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets = new int[ capacity ];     // Initialize buckets array
			hash     = new int[ capacity ];     // Initialize hash array
			links    = new int[ Math.min( 16, capacity ) ]; // Initialize links with a small, reasonable capacity
			keys     = equal_hash_K.copyOf( null, capacity ); // Initialize keys array
			_lo_Size = 0;
			_hi_Size = 0;
			return capacity;
		}
		
		/**
		 * Internal helper method used during resizing to efficiently copy an
		 * existing key-value pair into the new hash table structure. It re-hashes the key
		 * and places it into the correct bucket and region (lo or hi) in the new arrays.
		 * This method does not check for existing keys, assuming all keys are new in the
		 * target structure during a resize operation.
		 *
		 * @param key   The key to copy.
		 * @param value The value associated with the key.
		 */
		private void copy( K key, int hash, byte value ) {
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from the bucket
			
			int dst_index; // Destination index for the key
			
			if( index == -1 ) // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				// Collision occurred. Place new entry in {@code lo Region}
				if( links.length == _lo_Size ) // If lo_Size exceeds links array capacity
					links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) ); // Resize links
				
				links[ dst_index = _lo_Size++ ] = index; // New entry points to the old head
			}
			
			keys[ dst_index ]      = key; // Store the key
			this.hash[ dst_index ] = hash; // Store the hash
			values.set1( dst_index, value ); // Store the value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to new head (1-based)
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