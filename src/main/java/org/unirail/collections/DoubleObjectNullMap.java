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
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * {@code IntObjectNullMap} is a generic interface for a map that stores key-value pairs
 * where keys are primitive integers and values are objects.
 * <p>
 * This map implementation is designed for efficiency, providing fast key lookups and value retrievals.
 * It also supports the concept of a null key, allowing a single null key entry in the map.
 * <p>
 * Implementations should provide methods for inserting, retrieving, updating, and deleting key-value pairs,
 * as well as querying the map's state (e.g., size, emptiness, key/value existence).
 * <p>
 */
public interface DoubleObjectNullMap {
	
	/**
	 * {@code R} is an abstract, read-only base class that provides the core implementation
	 * for {@code IntObjectNullMap}. It manages the internal state and provides read-only operations.
	 * <p>
	 * This class uses a hash table for efficient key lookups. It also includes mechanisms for
	 * handling collisions, managing free entries, and versioning for concurrent modification detection.
	 * <p>
	 * Subclasses, like {@link RW}, extend this class to provide read-write capabilities.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	abstract class R< V > implements JsonWriter.Source, Cloneable {
		/**
		 * Indicates whether the map contains a null key.
		 */
		protected       boolean                hasNullKey;
		/**
		 * Stores the value associated with the null key.
		 */
		protected       V                      nullKeyValue;
		/**
		 * Array of buckets for the hash table. Each bucket is an index to the first entry in a chain.
		 */
		protected       int[]                  _buckets;
		/**
		 * Array of 'next' indices for each entry, forming linked lists for collision resolution.
		 */
		protected       int[]                  nexts;
		/**
		 * Array of integer keys.
		 */
		protected       double[]          keys = Array.EqualHashOf.doubles     .O;
		/**
		 * List to store values, allowing null values and efficient removal.
		 */
		protected       ObjectNullList.RW< V > values;
		/**
		 * The total number of entries currently in the map (including free/removed entries).
		 */
		protected       int                    _count;
		/**
		 * Index of the first free (removed) entry in the {@code nexts}, {@code keys}, and {@code values} arrays.
		 */
		protected       int                    _freeList;
		/**
		 * The number of free (removed) entries available for reuse.
		 */
		protected       int                    _freeCount;
		/**
		 * Version number, incremented on each modification, used for detecting concurrent modifications.
		 */
		protected       int                    _version;
		/**
		 * Strategy object for value equality and hash code calculation.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		/**
		 * Constant to mark the start of the free list in the {@code nexts} array.
		 */
		protected static final int  StartOfFreeList = -3;
		/**
		 * Mask to extract the index from a token.
		 */
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		/**
		 * Bit shift for the version part of a token.
		 */
		protected static final int  VERSION_SHIFT   = 32;
		/**
		 * Represents an invalid token, indicating that a key is not found or a token is invalid. Value is -1.
		 */
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Constructs a read-only map with the specified value equality and hash strategy.
		 *
		 * @param equal_hash_V The strategy for comparing and hashing values.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
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
			return _count - _freeCount + (
					hasNullKey ?
							1 :
							0 );
		}
		
		/**
		 * Returns the number of key-value mappings in this map.
		 * Alias for {@link #size()}.
		 *
		 * @return the number of key-value mappings in this map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the total capacity of the internal arrays (buckets, nexts, keys, values).
		 * This is not the same as the size of the map, but rather the allocated storage capacity.
		 *
		 * @return the length of the internal arrays, or 0 if not initialized.
		 */
		public int length() {
			return nexts == null ?
					0 :
					nexts.length;
		}
		
		/**
		 * Checks if this map contains a mapping for the specified primitive integer key.
		 *
		 * @param key The key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean containsKey( double key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this map contains a mapping for the specified boxed Integer key.
		 * Handles null keys as well.
		 *
		 * @param key The key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean containsKey(  Double    key ) {
			return key == null ?
					hasNullKey :
					tokenOf( key ) != INVALID_TOKEN;
		}
		
		/**
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		public V nullKeyValue() { return nullKeyValue; }
		
		
		/**
		 * Checks if this map contains a value equal to the given value.
		 * Note: This operation iterates through all values and may be slow for large maps.
		 *
		 * @param value The value whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a value equal to the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( Object value ) {
			if( hasNullKey && Objects.equals( nullKeyValue, value ) ) return true;
			for( int i = 0; i < _count; i++ )
				if( nexts[ i ] >= -1 && Objects.equals( values.get( i ), value ) ) return true;
			return false;
		}
		
		/**
		 * Returns a token associated with the specified boxed Integer key, if present.
		 * A token is a long value that uniquely identifies an entry in the map and includes a version.
		 * Returns {@link #INVALID_TOKEN} (-1) if the key is not found or if the map has been modified since the token was issued.
		 * Handles null keys.
		 *
		 * @param key The key for which to retrieve the token.
		 * @return The token associated with the key, or {@link #INVALID_TOKEN} (-1) if the key is not found.
		 */
		public long tokenOf(  Double    key ) {
			return key == null ?
					hasNullKey ?
							token( _count ) :
							INVALID_TOKEN :
					tokenOf( key.doubleValue     () );
		}
		
		/**
		 * Returns a token associated with the specified primitive integer key, if present.
		 * A token is a long value that uniquely identifies an entry in the map and includes a version.
		 * Returns {@link #INVALID_TOKEN} (-1) if the key is not found or if the map has been modified since the token was issued.
		 *
		 * @param key The key for which to retrieve the token.
		 * @return The token associated with the key, or {@link #INVALID_TOKEN} (-1) if the key is not found.
		 */
		public long tokenOf( double key ) {
			if( _buckets == null ) return INVALID_TOKEN; // Map is not initialized, no buckets exist
			int hash = Array.hash( key ); // Calculate hash of the key
			
			int i = _buckets[ bucketIndex( hash ) ] - 1; // Get bucket index and first entry in the chain (0-based index)
			
			// Traverse the chain in the bucket to find the key
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key )
					return token( i ); // Key found, return its token
				i = nexts[ i ]; // Move to the next entry in the chain
				if( nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." ); // Detect potential infinite loop or concurrent modification
			}
			return INVALID_TOKEN; // Key not found in the chain
		}
		
		/**
		 * Returns a token for the first entry in the map.
		 * This can be used to iterate through the map using {@link #token(long)}.
		 * Returns {@link #INVALID_TOKEN} (-1) if the map is empty.
		 *
		 * @return A token for the first entry, or {@link #INVALID_TOKEN} (-1) if the map is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i ); // Find the first valid entry
			return hasNullKey ?
					token( _count ) :
					// If no regular entry, check for null key
					INVALID_TOKEN;      // Map is empty
		}
		
		/**
		 * Returns a token for the next entry in the map after the entry represented by the given token.
		 * This is used for iteration.
		 * Returns {@link #INVALID_TOKEN} (-1) if there are no more entries after the current one or if the token is invalid due to map modification.
		 *
		 * @param token The token of the current entry.
		 * @return A token for the next entry, or {@link #INVALID_TOKEN} (-1) if no more entries exist or the token is invalid.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN || version( token ) != _version ) return INVALID_TOKEN;
			for( int i = index( token ) + 1; i < _count; i++ )
				if( nexts[ i ] >= -1 ) return token( i ); // Find the next valid entry after the current index
			return hasNullKey && index( token ) < _count ?
					token( _count ) :
					// If no more regular entries, check for null key (if current token is not for null key)
					INVALID_TOKEN;      // No more entries
		}
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use  {@link #nullKeyValue()} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #nullKeyValue() To get the null key’s value.
		 */
		public int unsafe_token( final int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return i;
			return -1;
		}
		
		/**
		 * Checks if the entry associated with the given token represents the null key entry.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token corresponds to the null key, {@code false} otherwise.
		 */
		boolean isKeyNull( long token ) { return index( token ) == _count; }
		
		/**
		 * Returns the primitive integer key associated with the given token.
		 *
		 * @param token The token for which to retrieve the key.
		 * @return The integer key associated with the token, or 0 if the token corresponds to the null key.
		 */
		public double key( long token ) {
			return   hasNullKey && index( token ) == _count ?
					0 :
					keys[ index( token ) ]; // Return the actual key from the keys array
		}
		
		/**
		 * Returns whether the entry associated with the given token has a non-null value.
		 * In a null-value context, this indicates if a value is explicitly set (not default null).
		 *
		 * @param token The token to check.
		 * @return {@code true} if the entry has a non-null value, {@code false} if it has a null value.
		 */
		public boolean hasValue( long token ) {
			return index( token ) == _count ?
					nullKeyValue != null :
					// For null key entry, check if nullKeyValue is not null
					values.hasValue( index( token ) ); // For regular entries, use ObjectNullList's hasValue
		}
		
		/**
		 * Returns the value associated with the given token.
		 *
		 * @param token The token for which to retrieve the value.
		 * @return The value associated with the token.
		 */
		public V value( long token ) {
			return hasNullKey && index( token ) == _count ?
					nullKeyValue :
					// Return nullKeyValue if it's the null key entry
					values.get( index( token ) ); // Return the value from the values list
		}
		
		/**
		 * Returns the value to which the specified boxed Integer key is mapped,
		 * or {@code null} if this map contains no mapping for the key.
		 * Handles null keys.
		 *
		 * @param key The key whose associated value is to be returned.
		 * @return The value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
		 */
		@SuppressWarnings( "unchecked" )
		public V get(  Double    key ) {
			long token = tokenOf( (  Double    ) key );
			return token == INVALID_TOKEN ?
					null :
					// Key not found, return null
					value( token ); // Key found, return the associated value
		}
		
		/**
		 * Returns the value to which the specified primitive integer key is mapped,
		 * or {@code defaultValue} if this map contains no mapping for the key.
		 *
		 * @param key          The key whose associated value is to be returned.
		 * @param defaultValue The default value to return if the map contains no mapping for the key.
		 * @return The value to which the specified key is mapped, or {@code defaultValue} if there is no mapping.
		 */
		public V getOrDefault( double key, V defaultValue ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
					defaultValue :
					// Key not found, return default value
					value( token ); // Key found, return the associated value
		}
		
		/**
		 * Returns the value to which the specified boxed Integer key is mapped,
		 * or {@code defaultValue} if this map contains no mapping for the key.
		 * Handles null keys.
		 *
		 * @param key          The key whose associated value is to be returned.
		 * @param defaultValue The default value to return if this map contains no mapping for the key.
		 * @return The value to which the specified key is mapped, or {@code defaultValue} if there is no mapping.
		 */
		public V getOrDefault(  Double    key, V defaultValue ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
					defaultValue :
					// Key not found, return default value
					value( token ); // Key found, return the associated value
		}
		
		/**
		 * Computes the hash code for this map.
		 * The hash code is based on the keys and values of all entries in the map, including the null key entry if present.
		 *
		 * @return The hash code value for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) ); // Mix seed with key hash
				h = Array.mix( h, Array.hash( value( token ) == null ?
						                              seed :
						                              // Use seed for null value hash to differentiate from hash(null)
						                              equal_hash_V.hashCode( value( token ) ) ) ); // Mix with value hash
				h = Array.finalizeHash( h, 2 ); // Finalize hash for the entry
				a += h;
				b ^= h;
				c *= h | 1; // Accumulate hash components
			}
			if( hasNullKey ) {
				int h = Array.hash( seed ); // Start hash for null key entry with seed
				h = Array.mix( h, Array.hash( nullKeyValue != null ?
						                              equal_hash_V.hashCode( nullKeyValue ) :
						                              // Hash null key value
						                              seed ) ); // Use seed if nullKeyValue is null
				h = Array.finalizeHash( h, 2 ); // Finalize hash for null key entry
				a += h;
				b ^= h;
				c *= h | 1; // Accumulate hash components
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() ); // Finalize hash for the entire map
		}
		
		/**
		 * Seed value used in hash code calculation.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map to the specified object for equality.
		 * Returns {@code true} if the given object is also a {@code R} instance and represents the same map.
		 * Equality is determined by comparing key-value mappings and handling of null keys.
		 *
		 * @param obj The object to compare with this map.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj );
		}
		
		/**
		 * Compares this map to another {@code R} map for equality.
		 *
		 * @param other The other map to compare with.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) {
			if( other == this ) return true; // Same instance
			if( other == null || hasNullKey != other.hasNullKey || // Null check, null key presence check
			    hasNullKey && !Objects.equals( nullKeyValue, other.nullKeyValue ) || // Null key value equality check
			    size() != other.size() ) return false; // Size check
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || !Objects.equals( value( token ), other.value( t ) ) ) return false;
			}
			return true; // All key-value pairs are equal
		}
		
		/**
		 * Creates and returns a deep copy of this read-only map.
		 * The clone will contain the same mappings as the original map.
		 *
		 * @return A clone of this map.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone(); // Perform shallow clone first
				if( _buckets != null ) {
					dst._buckets = _buckets.clone(); // Deep clone buckets array
					dst.nexts    = nexts.clone();    // Deep clone nexts array
					dst.keys     = keys.clone();     // Deep clone keys array
					dst.values   = values.clone();   // Deep clone ObjectNullList.RW (values)
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
				return null;
			}
		}
		
		/**
		 * Returns a string representation of this map in JSON format.
		 *
		 * @return A JSON string representing this map.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes this map to a {@link JsonWriter} in JSON format.
		 *
		 * @param json The JsonWriter to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 ); // Pre-allocate buffer for JSON string
			long token = token(); // Get the first token for iteration
			
			json.enterObject(); // Start JSON object
			if( hasNullKey ) json.name().value( nullKeyValue );
			for( ; index( token ) < _count; token = token( token ) )
			     json.name( String.valueOf( key( token ) ) ).value( value( token ) ); // Write key-value pairs as JSON properties
			json.exitObject(); // End JSON object
		}
		
		/**
		 * Calculates the bucket index for a given hash value.
		 *
		 * @param hash The hash value of the key.
		 * @return The bucket index in the {@code _buckets} array.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an index and the current version.
		 *
		 * @param index The index of the entry in the internal arrays.
		 * @return A long token representing the entry.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index & INDEX_MASK; }
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token The token.
		 * @return The index of the entry.
		 */
		protected int index( long token ) { return ( int ) ( token & INDEX_MASK ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token The token.
		 * @return The version of the map when the token was created.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * {@code RW} is a read-write implementation of {@code IntObjectNullMap}, extending the read-only base class {@link R}.
	 * It provides methods to modify the map, such as adding, updating, and removing key-value pairs.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs an empty read-write map with a default initial capacity.
		 *
		 * @param clazzV The class of the value type {@code V}, used for array creation in {@link ObjectNullList.RW}.
		 */
		public RW( Class< V > clazzV ) { this( clazzV, 0 ); }
		
		/**
		 * Constructs an empty read-write map with a specified initial capacity.
		 *
		 * @param clazzV   The class of the value type {@code V}, used for array creation in {@link ObjectNullList.RW}.
		 * @param capacity The initial capacity of the map.
		 */
		public RW( Class< V > clazzV, int capacity ) { this( Array.get( clazzV ), capacity ); }
		
		/**
		 * Constructs an empty read-write map with a default initial capacity, using a provided value equality and hash strategy.
		 *
		 * @param equal_hash_V The strategy for comparing and hashing values.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V ) { this( equal_hash_V, 0 ); }
		
		/**
		 * Constructs an empty read-write map with a specified initial capacity, using a provided value equality and hash strategy.
		 *
		 * @param equal_hash_V The strategy for comparing and hashing values.
		 * @param capacity     The initial capacity of the map.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int capacity ) {
			super( equal_hash_V );
			if( capacity > 0 ) initialize( capacity ); // Initialize internal arrays if capacity is specified
		}
		
		
		/**
		 * Removes all of the mappings from this map.
		 * The map will be empty after this call.
		 */
		public void clear() {
			hasNullKey   = false; // Reset null key flag
			nullKeyValue = null;  // Reset null key value
			if( _count == 0 ) return; // Map is already empty
			Arrays.fill( _buckets, 0 ); // Clear buckets array
			Arrays.fill( nexts, 0, _count, ( int ) 0 ); // Clear nexts array
			values.clear(); // Clear values list
			_count     = 0; // Reset entry count
			_freeList  = -1; // Reset free list
			_freeCount = 0; // Reset free entry count
			_version++; // Increment version for concurrency control
		}
		
		/**
		 * Removes the mapping for a boxed Integer key from this map if it is present.
		 * Returns the value to which this map previously associated the key, or {@code null} if the map contains no mapping for the key.
		 * Handles null keys.
		 *
		 * @param key The key whose mapping is to be removed from the map.
		 * @return The previous value associated with the key, or {@code null} if there was no mapping.
		 */
		public V remove(  Double    key ) {
			if( key == null ) {
				if( !hasNullKey ) return null; // Null key not present
				hasNullKey = false; // Mark null key as removed
				V oldValue = nullKeyValue; // Get the old value
				nullKeyValue = null;  // Clear null key value
				_version++; // Increment version
				return oldValue; // Return the old value
			}
			return remove( key.doubleValue     () ); // Remove by primitive int key
		}
		
		
		/**
		 * Removes the mapping for a primitive integer key from this map if it is present.
		 * Returns the value to which this map previously associated the key, or {@code null} if the map contains no mapping for the key.
		 *
		 * @param key The key whose mapping is to be removed from the map.
		 * @return The previous value associated with the key, or {@code null} if there was no mapping.
		 */
		public V remove( double key ) {
			// Handle edge cases: if map is uninitialized or empty, nothing to remove
			if( _buckets == null || _count == 0 )				return null; // Return invalid token indicating no removal
			
			// Compute hash and bucket index for the key to locate its chain
			int hash           = Array.hash( key );                // Hash the key using Array.hash
			int bucketIndex    = bucketIndex( hash );       // Map hash to bucket index
			int last           = -1;                             // Previous index in chain (-1 if 'i' is head)
			int i              = _buckets[ bucketIndex ] - 1;         // Head of chain (convert 1-based to 0-based)
			int collisionCount = 0;                    // Counter to detect infinite loops or concurrency issues
			
			// Traverse the linked list in the bucket to find the key
			while( -1 < i ) {
				int next = nexts[ i ];                   // Get next index in the chain
				if( keys[ i ] == key ) {                  // Found the key at index 'i'
					
					V oldValue = values.get( i ); // Get the old value
					
					// Step 1: Unlink the entry at 'i' from its chain
					if( last < 0 )
						// If 'i' is the head, update bucket to point to the next entry
						_buckets[ bucketIndex ] = next + 1;
					
					else
						// Otherwise, link the previous entry to the next, bypassing 'i'
						nexts[ last ] = ( int ) next;
					
					// Step 2: Optimize removal if value is non-null and not the last non-null
					final int     lastNonNullValue = values.nulls.last1(); // Index of last non-null value
					if( values.hasValue( i ) )
						if( i != lastNonNullValue ) {
						// Optimization applies: swap with last non-null entry
						// Step 2a: Copy key, next, and value from lastNonNullValue to i
						double   keyToMove          = keys[ lastNonNullValue ];
						int           BucketOf_KeyToMove = bucketIndex( Array.hash( keyToMove ) );
						int           _next              = nexts[ lastNonNullValue ];
						
						keys[ i ]  = keyToMove;                         // Copy the key to the entry being removed
						nexts[ i ] = _next;         // Copy the next to the entry being removed
						values.set1( i, values.get( lastNonNullValue ) ); // Copy the value to the entry being removed
						
						// Step 2b: Update the chain containing keyToMove to point to 'i'
						int prev = -1;                     // Previous index in keyToMove’s chain
						collisionCount = 0;// Reset collision counter
						
						// Start at chain head
						for( int current = _buckets[ BucketOf_KeyToMove ] - 1; current != lastNonNullValue; prev = current, current = nexts[ current ] )
							if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
						
						if( -1 < prev ) nexts[ prev ] = i; // Update next pointer of the previous entry
						else _buckets[ BucketOf_KeyToMove ] = i + 1;// If 'lastNonNullValue' the head, update bucket to the position of keyToMove
						
						
						values.set1( lastNonNullValue, null );        // Clear value (O(1) since it’s last non-null)
						i = lastNonNullValue; // Continue freeing operations at swapped position
					}
					else values.set1( i, null );                       // Clear value (may shift if not last)
					
					nexts[ i ] = ( int ) ( StartOfFreeList - _freeList ); // Mark 'i' as free and link to free list
					_freeList  = i; // Update free list head
					_count --;
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
		 * Ensures that the capacity of the map is at least equal to the specified capacity.
		 * If the current capacity is less than the requested capacity, the internal arrays are resized to the next prime number greater than or equal to capacity.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity of the map after ensuring capacity.
		 * @throws IllegalArgumentException if the capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity is less than 0." );
			int currentCapacity = length(); // Get current capacity
			if( capacity <= currentCapacity ) return currentCapacity; // No need to resize if current capacity is sufficient
			_version++; // Increment version before resize
			if( _buckets == null ) return initialize( capacity ); // Initialize if not yet initialized
			int newSize = Array.prime( capacity ); // Calculate new prime size
			resize( newSize ); // Resize the internal arrays
			return newSize; // Return the new capacity
		}
		
		/**
		 * Trims the capacity of this map to be the map's current size.
		 * If the map's capacity is larger than the current size, it is reduced to conserve space.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Trims the capacity of this map to be at least as large as the specified capacity.
		 * If the map's capacity is larger than the specified capacity, it is reduced to conserve space, but not below the specified capacity or the current size, whichever is larger.
		 *
		 * @param capacity The desired capacity to trim to.
		 * @throws IllegalArgumentException if the capacity is less than the current size of the map.
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length(); // Get current capacity
			int new_size        = Array.prime( capacity ); // Calculate new prime size
			if( currentCapacity <= new_size ) return; // No need to trim if current capacity is already smaller or equal
			
			int[]                  old_next   = nexts; // Store old arrays for copying
			double[]          old_keys   = keys;
			ObjectNullList.RW< V > old_values = values.clone(); // Clone old values list
			int                    old_count  = _count; // Store old count
			_version++; // Increment version before resize
			initialize( new_size ); // Initialize with new smaller size
			copy( old_next, old_keys, old_values, old_count ); // Copy existing data to new arrays
		}
		
		/**
		 * Associates the specified value with the specified boxed Integer key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles null keys.
		 *
		 * @param key   The key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 */
		public boolean put(  Double    key, V value ) {
			return key == null ?
					put( value ) :
					put( key, value ); // Insert or update value for null key
			
		}
		
		
		/**
		 * Associates the specified value with the null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The value to be associated with the null key.
		 */
		public boolean put( V value ) {
			boolean b = !hasNullKey;
			hasNullKey   = true; // Set null key flag
			nullKeyValue = value; // Set null key value
			_version++; // Increment version
			return true; // Insertion or update successful
		}
		
		/**
		 * Associates the specified value with the specified primitive integer key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 */
		public boolean put( double key, V value ) {
			
			if( _buckets == null ) initialize( 7 ); // Initialize if not yet initialized
			int[] _nexts         = nexts; // Get nexts array
			int   hash           = Array.hash( key ); // Hash the key
			int   collisionCount = 0; // Collision counter
			int   bucketIndex    = bucketIndex( hash ); // Get bucket index
			int   bucket         = _buckets[ bucketIndex ] - 1; // Get head of chain (0-based index)
			
			// Traverse the chain to check for existing key
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) {
					values.set1( next, value ); // Set new value
					_version++; // Increment version
					return false; // Update successful
				}
				
				next = _nexts[ next ]; // Move to next entry in chain
				if( _nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." ); // Detect concurrent modification
			}
			
			int index;
			if( 0 < _freeCount ) { // Reuse a free slot if available
				index     = _freeList; // Get index from free list
				_freeList = StartOfFreeList - _nexts[ _freeList ]; // Update free list head
				_freeCount--; // Decrement free count
			}
			else { // No free slots available, need to expand if full
				if( _count == _nexts.length ) {
					resize( Array.prime( _count * 2 ) ); // Resize to accommodate new entry
					bucket =  _buckets[ bucketIndex = bucketIndex( hash ) ] - 1; // Recalculate bucket index and chain head after resize
				}
				index = _count++; // Get the next available index and increment count
			}
			
			nexts[ index ] = ( int ) bucket; // Set next pointer for new entry
			keys[ index ]  = ( double ) key; // Set key for new entry
			values.set1( index, value ); // Set value for new entry
			_buckets[ bucketIndex ] = index + 1; // Update bucket to point to the new entry (1-based)
			_version++; // Increment version
			
			return true; // Insertion successful
		}
		
		/**
		 * Resizes the internal arrays to a new capacity.
		 * Rehashes all existing entries to distribute them in the new buckets.
		 *
		 * @param newSize The new capacity for the arrays. Should be a prime number for better distribution.
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Double   .BYTES * 8 ); // Limit max size to avoid potential issues
			_version++; // Increment version before resize
			int[]         new_next = Arrays.copyOf( nexts, newSize ); // Create new nexts array
			double[] new_keys = Arrays.copyOf( keys, newSize ); // Create new keys array
			final int     count    = _count; // Store current count
			
			_buckets = new int[ newSize ]; // Create new buckets array
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) { // Process only valid (non-free) entries
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) ); // Recalculate bucket index for the key
					new_next[ i ]           = ( int ) ( _buckets[ bucketIndex ] - 1 ); // Link current entry to the head of the new bucket chain
					_buckets[ bucketIndex ] = i + 1; // Update bucket head to point to the current entry (1-based)
				}
			
			nexts = new_next; // Replace old arrays with new ones
			keys  = new_keys;
		}
		
		/**
		 * Initializes the internal arrays with a given capacity.
		 *
		 * @param capacity The initial capacity for the arrays. Should be a prime number for better distribution.
		 * @return The initialized capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets  = new int[ capacity ]; // Initialize buckets array
			nexts     = new int[ capacity ]; // Initialize nexts array
			keys      = new double[ capacity ]; // Initialize keys array
			values    = new ObjectNullList.RW<>( equal_hash_V, capacity ); // Initialize values list
			_freeList = -1; // Initialize free list to empty
			return capacity; // Return the initialized capacity
		}
		
		/**
		 * Copies entries from old arrays to the newly resized arrays.
		 * Used during trimming to compact the map.
		 *
		 * @param old_next   The old nexts array.
		 * @param old_keys   The old keys array.
		 * @param old_values The old values list.
		 * @param old_count  The number of valid entries in the old arrays.
		 */
		private void copy( int[] old_next, double[] old_keys, ObjectNullList.RW< V > old_values, int old_count ) {
			int new_count = 0; // Counter for new entry index
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue; // Skip free/invalid entries
				keys[ new_count ] = old_keys[ i ]; // Copy key
				values.set1( new_count, old_values.get( i ) ); // Copy value
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) ); // Recalculate bucket index in new buckets array
				nexts[ new_count ]      = ( int ) ( _buckets[ bucketIndex ] - 1 ); // Link to the head of the new bucket chain
				_buckets[ bucketIndex ] = new_count + 1; // Update bucket head to point to the new entry (1-based)
				new_count++; // Increment new entry count
			}
			_count     = new_count; // Update total entry count
			_freeCount = 0; // Reset free entry count
		}
		
		/**
		 * Static instance of {@link Array.EqualHashOf} for {@code RW} class, used for default equality checks.
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Returns a default {@link Array.EqualHashOf} instance suitable for {@code RW} maps.
	 * This can be used when creating a map where value equality and hashing need to be customized.
	 *
	 * @param <V> The value type of the map.
	 * @return A default {@link Array.EqualHashOf} instance for {@code RW}.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() { return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; }
}