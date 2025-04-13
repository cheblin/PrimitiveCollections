// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit others to do so, subject to the following conditions:
//
// 1. The above copyright notice and this permission notice must be included in all
//    copies or substantial portions of the Software.
//
// 2. Users of the Software must provide a clear acknowledgment in their user
//    documentation or other materials that their solution includes or is based on
//    this Software. This acknowledgment should be prominent and easily visible,
//    and can be formatted as follows:
//    "This product includes software developed by Chikirev Sirguy and the Unirail Group
//    (https://github.com/AdHoc-Protocol)."
//
// 3. If you modify the Software and distribute it, you must include a prominent notice
//    stating that you have changed the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.ConcurrentModificationException;

/**
 * Defines the contract for a specialized map that uses primitive {@code byte} values as keys
 * and {@code Object} references as values.
 * <p>
 * This interface supports mapping a {@code null} key and storing {@code null} values.
 * Implementations aim for efficient storage and retrieval, particularly optimized for
 * primitive byte keys.
 */
public interface UByteObjectMap {
	
	/**
	 * Provides a base implementation for read-only operations on a {@code ByteObjectMap}.
	 * <p>
	 * This abstract class leverages the key management functionality from {@link ByteSet.R}
	 * and adds mechanisms for storing and retrieving associated values of type {@code V}.
	 * It employs a sophisticated storage strategy for values:
	 * <ul>
	 *     <li><b>Sparse Mode:</b> For maps with fewer than 128 keys, values are stored in an array
	 *         indexed by the key's rank (obtained via {@link ByteSet.R#rank(byte)}) minus 1. The array grows dynamically.</li>
	 *     <li><b>Flat Mode:</b> When the map reaches 128 keys, it switches to a flat structure where the
	 *         values array has a fixed size of 256. Values are stored at the index corresponding to the
	 *         unsigned byte value of the key ({@code key & 0xFF}).</li>
	 * </ul>
	 * This dual-mode approach optimizes memory usage for smaller maps while providing constant-time
	 * lookups for larger maps.
	 * <p>
	 * Iteration is managed using tokens, which incorporate versioning to detect concurrent modifications
	 * and ensure safe access during iteration.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	abstract class R< V > extends UByteSet.R implements Cloneable, JsonWriter.Source {
		/**
		 * Stores the values associated with the byte keys. The interpretation of indices
		 * depends on the map's mode (sparse or flat), as described in the {@link ByteObjectMap.R} documentation.
		 * <p>
		 * <ul>
		 *     <li>Sparse Mode (< 128 keys): Index = {@code rank(key) - 1}. Array size is dynamic.</li>
		 *     <li>Flat Mode (>= 128 keys): Index = {@code key & 0xFF}. Array size is fixed at 256.</li>
		 * </ul>
		 * Direct access to this array is generally discouraged; use the provided methods like {@link #value(byte)}
		 * or {@link #value(long)} instead.
		 */
		public          V[]                    values;
		/**
		 * Strategy object for comparing values of type {@code V} for equality and calculating their hash codes.
		 * Allows customization of value semantics if the default {@code equals}/{@code hashCode} behavior is not desired.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		/**
		 * Holds the value associated with the {@code null} key, if present.
		 * If no {@code null} key is mapped, this field is typically {@code null}.
		 */
		protected       V                      nullKeyValue;
		
		/**
		 * Returns the value associated with the {@code null} key.
		 *
		 * @return The value mapped to the {@code null} key, or {@code null} if no such mapping exists.
		 */
		public V nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Initializes the read-only map using the default equality and hashing strategy for the value type.
		 * The strategy is obtained via {@link Array#get(Class)}.
		 *
		 * @param clazz The {@code Class} object representing the type {@code V} of the values.
		 *              Used to determine the appropriate {@link Array.EqualHashOf} strategy. Must not be null.
		 */
		protected R( Class< V > clazz ) {
			this.equal_hash_V = Array.get( clazz );
		}
		
		/**
		 * Initializes the read-only map with a custom strategy for value equality and hashing.
		 *
		 * @param equal_hash_V A custom {@link Array.EqualHashOf} implementation for handling values of type {@code V}. Must not be null.
		 */
		public R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		
		/**
		 * Retrieves the value associated with the key identified by the given iteration token.
		 * <p>
		 * Tokens provide a safe and efficient way to access map entries during iteration.
		 * This method decodes the token to locate the corresponding value, checking for
		 * concurrent modifications.
		 *
		 * @param token A valid iteration token obtained from methods like {@link #token()} or {@link #token(long)}.
		 * @return The value associated with the key represented by the token. If the token corresponds
		 * to the {@code null} key, {@link #nullKeyValue()} is returned.
		 * @throws ConcurrentModificationException If the token's version does not match the map's current version,
		 *                                         indicating a structural modification since the token was obtained.
		 * @throws IllegalArgumentException        if the token is invalid (e.g., -1).
		 * @see #token()
		 * @see #token(long)
		 * @see #key(long)
		 */
		public V value( long token ) {
			return isKeyNull( token ) ?
					nullKeyValue :
					values[ ( int ) token >>> KEY_LEN ];
		}
		
		/**
		 * Retrieves the value associated with the specified boxed {@code Byte} key.
		 * <p>
		 * If the provided {@code key} is {@code null}, this method returns the value
		 * currently mapped to the {@code null} key (which might be {@code null}).
		 *
		 * @param key The boxed {@code Byte} key whose associated value is sought. Can be {@code null}.
		 * @return The value mapped to the specified key, or {@code null} if the key is not found in the map
		 * (or if {@code null} is explicitly mapped to the key).
		 */
		public V value(  Character key ) {
			return key == null ?
					nullKeyValue :
					value( ( char ) ( key + 0 ) );
		}
		
		/**
		 * Retrieves the value associated with the specified primitive {@code byte} key.
		 *
		 * @param key The primitive {@code byte} key whose associated value is sought.
		 * @return The value mapped to the specified key, or {@code null} if the key is not found in the map
		 * (or if {@code null} is explicitly mapped to the key).
		 */
		public V value( char key ) {
			if( !get_( ( byte ) key ) ) return null;
			
			return values[
					values.length == 256 ?
							key & 0xFF :
							rank( ( byte ) key ) - 1 ];
		}
		
		
		
		/**
		 * Checks if this map contains at least one mapping to the specified value.
		 * <p>
		 * Value equality is determined using the {@link #equal_hash_V} strategy provided during construction.
		 * This involves iterating through all mapped values.
		 *
		 * @param value The value whose presence is being tested. Can be {@code null}.
		 * @return {@code true} if at least one key maps to a value {@code v} such that
		 * {@code equal_hash_V.equals(v, value)} is {@code true}, {@code false} otherwise.
		 */
		public boolean containsValue( V value ) { return tokenOfValue( value ) != -1; }
		
		/**
		 * Computes the hash code for this map.
		 * <p>
		 * The hash code is derived from the hash codes of all key-value pairs in the map,
		 * including the mapping for the {@code null} key if present. The calculation uses
		 * the map's {@code seed} and the {@link Array#hash(Object)}, {@link Array#mix(int, int)},
		 * {@link Array#mixLast(int, int)}, and {@link Array#finalizeHash(int, int)} methods
		 * for mixing and finalization. Value hash codes are obtained using {@link #equal_hash_V}.
		 *
		 * @return The hash code value for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			int h =  seed;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				h = Array.mix( h, equal_hash_V.hashCode( values[ token >>> KEY_LEN ] ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue != null ?
						                              equal_hash_V.hashCode( nullKeyValue ) :
						                              seed ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = IntObjectMap.R.class.hashCode();
		
		/**
		 * Compares the specified object with this map for equality.
		 * <p>
		 * Returns {@code true} if the given object is also a {@code ByteObjectMap.R},
		 * has the same size, contains the same key-value mappings, and handles the {@code null} key identically.
		 * Value equality is determined using the {@link #equal_hash_V} strategy. Key equality uses standard
		 * byte comparison (and special handling for the null key).
		 *
		 * @param obj The object to compare with this map.
		 * @return {@code true} if the specified object is equal to this map, {@code false} otherwise.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj ); }
		
		/**
		 * Type-safe equality comparison with another {@code ByteObjectMap.R}.
		 * <p>
		 * Checks for equal size, identical handling of the {@code null} key and its associated value,
		 * and ensures that for every key in this map, the other map contains the same key mapped
		 * to an equal value. Value equality is determined by {@link #equal_hash_V}. Key set equality
		 * is checked via the superclass {@link ByteSet.R#equals(ByteSet.R)}.
		 *
		 * @param other The {@code ByteObjectMap.R} to compare against.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) {
			if( other == this ) return true;
			if( other == null ||
			    !super.equals( other ) || // Compare ByteSet part
			    hasNullKey && !equal_hash_V.equals( nullKeyValue, other.nullKeyValue ) || // Compare null key value
			    size() != other.size() ) // Compare sizes. Important to check size before array iteration
				return false;
			long t;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( ( t = other.tokenOf( key( token ) ) ) == -1 || !equal_hash_V.equals( other.value( t ), values[ token >>> KEY_LEN ] ) ) return false;
			return true;
		}
		
		
		/**
		 * Internal helper to construct a token, embedding version, key, and value index.
		 * This version seems specific to sparse mode, where rank determines the index.
		 *
		 * @param token The previous token (used to derive the index part, likely related to rank).
		 * @param key   The byte key (0-255).
		 * @return A new token embedding version, key, and derived index.
		 */
		@Override
		protected long token( long token, int key ) {
			return ( long ) _version << VERSION_SHIFT | (
					values.length == 256 ?
							( long ) key << KEY_LEN :
							( token & ( long ) ~KEY_MASK ) + ( 1 << KEY_LEN )
			) | key;
		}
		
		/**
		 * Internal helper to construct a token directly from a key.
		 * Calculates the appropriate value index based on flat or sparse mode.
		 *
		 * @param key The byte key (0-255).
		 * @return A token embedding version, key, and calculated value index.
		 */
		@Override
		protected long tokenOf( int key ) {
			return ( long ) _version << VERSION_SHIFT | (
					                                            values.length == 256 ?
							                                            ( long ) key :
							                                            rank( ( byte ) key ) - 1
			                                            ) << KEY_LEN | key;
		}
		
		
		/**
		 * Finds the first token associated with the specified  value.
		 * <p>
		 * Searches the map, checking the value associated with the {@code null} key first (if present),
		 * then iterating through all non-null key mappings. Value equality is determined using the
		 * {@link #equal_hash_V} strategy.
		 *
		 * @param value The value to search for. Can be {@code null}.
		 * @return The first token encountered whose associated key maps to the specified value,
		 * or {@code -1} if the value is not found in the map.
		 */
		public long tokenOfValue( V value ) {
			if( hasNullKey && equal_hash_V.equals( nullKeyValue, value ) ) return token_nullKey();
			
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) if( equal_hash_V.equals( values[ t >>> KEY_LEN ], value ) ) return t;
			return -1;
		}
		
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * bypassing concurrency checks.
		 *
		 * <p><b>Usage:</b> Start iteration by calling {@code unsafe_token(-1)}. Pass the returned token
		 * back to this method repeatedly to get subsequent tokens. Iteration ends when {@code -1} is returned.
		 * The mapping for the {@code null} key (if present) is <em>not</em> included in this iteration;
		 * check {@link #hasNullKey()} and access {@link #nullKeyValue()} separately if needed.
		 *
		 * <p><strong><span style="color:red; font-weight:bold;">WARNING: UNSAFE.</span></strong> This method provides
		 * direct access to the underlying iteration mechanism without version checks. If the map is structurally
		 * modified (e.g., keys added or removed, resizing) concurrently or during the iteration (except through
		 * the iterator's own remove method, if available), the behavior is undefined. This may lead to
		 * {@link ArrayIndexOutOfBoundsException}, incorrect results, infinite loops, or other errors.
		 * Use this method <em>only</em> in performance-critical sections where you can guarantee that
		 * no concurrent modifications will occur. For safe iteration, use {@link #token()} and {@link #token(long)}.
		 *
		 * <p>The returned integer token encodes both the key (lower bits) and the value's index
		 * (middle bits) for direct access, but does <em>not</em> include the version stamp.
		 * Use {@code token & KEY_MASK} to get the key and {@code token >>> KEY_LEN} to get the value index
		 * for {@link #values}.
		 *
		 * @param token The previous token returned by this method, or {@code -1} to start iteration.
		 * @return The next integer token representing a non-null key mapping, or {@code -1} if iteration is complete.
		 * @see #token()
		 * @see #token(long)
		 * @see #hasNullKey()
		 * @see #nullKeyValue()
		 */
		@Override public int unsafe_token( int token ) {
			if( token == -1 ) {
				
				int ret = next1( -1 );
				
				return ret == -1 ?
						-1 :
						( int ) tokenOf( ret );
			}
			
			int next = next1( token & KEY_MASK );
			
			return next == -1 ?
					-1 :
					( int ) token( token, next );
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code ByteObjectMap.R} instance.
		 * <p>
		 * The internal key set structures and the {@code values} array are cloned.
		 * However, the value objects {@code V} themselves are <em>not</em> duplicated; the new map
		 * will contain references to the same value objects as the original map. Changes to mutable
		 * value objects will be reflected in both maps. The {@link #equal_hash_V} strategy and
		 * {@link #nullKeyValue} reference are also copied.
		 *
		 * @return A shallow clone of this instance.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			
			R< V > dst = ( R< V > ) super.clone();
			dst.values = values.clone(); // Shallow copy: value references are copied, not the value objects themselves.
			return dst;
		}
		
		/**
		 * Returns a string representation of the map, typically in JSON format.
		 * Delegates to {@link #toJSON()}.
		 *
		 * @return A string representation of this map.
		 * @see #toJSON(JsonWriter)
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Serializes the content of this map into a JSON object format using the provided {@link JsonWriter}.
		 * <p>
		 * Writes an opening brace {@code '{'}, followed by key-value pairs, and a closing brace {@code '}'}.
		 * The {@code null} key, if present, is represented as the JSON string {@code "null"}.
		 * Primitive {@code byte} keys are converted to their string representations (e.g., {@code "10"}, {@code "-1"}).
		 * Values are written using the {@link JsonWriter#value(Object)} method, which handles various types
		 * including {@code null}, primitives, strings, and objects implementing {@link JsonWriter.Source}.
		 *
		 * @param json The {@link JsonWriter} instance to write the JSON output to. Must not be null.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			if( hasNullKey ) json.name().value( nullKeyValue ); // Represent null key as "null" in JSON
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     json.name( String.valueOf( key( token ) ) ).value( values[ ( int ) token >>> KEY_LEN ] );
			json.exitObject();
		}
	}
	
	/**
	 * A concrete, modifiable implementation of {@code ByteObjectMap}.
	 * <p>
	 * Extends the read-only functionality of {@link ByteObjectMap.R} with methods
	 * for adding, updating, and removing key-value mappings ({@code put}, {@code remove}, {@code clear}).
	 * It manages the internal transition between sparse and flat storage modes automatically
	 * as elements are added.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs a new, empty, modifiable map with a specified initial capacity,
		 * using the default equality/hashing strategy for values of type {@code V}.
		 *
		 * @param clazz  The {@code Class} object for the value type {@code V}, used to get the default {@link Array.EqualHashOf}. Must not be null.
		 * @param length The desired initial capacity. The actual capacity will be at least 16 and at most 256.
		 *               The map grows automatically if needed. A capacity hint helps optimize initial memory allocation.
		 */
		public RW( Class< V > clazz, int length ) {
			this( Array.get( clazz ), length );
		}
		
		/**
		 * Constructs a new, empty, modifiable map with a specified initial capacity and a custom
		 * equality/hashing strategy for values.
		 *
		 * @param equal_hash_V The custom {@link Array.EqualHashOf} strategy for comparing and hashing values. Must not be null.
		 * @param length       The desired initial capacity. The actual capacity will be at least 16 and at most 256.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int length ) {
			super( equal_hash_V );
			values = this.equal_hash_V.copyOf( null, Math.max( Math.min( length, 0x100 ), 16 ) );
		}
		
		/**
		 * Removes all mappings from this map.
		 * <p>
		 * The map will be empty after this call returns. Resets size, clears key structures,
		 * nullifies the {@code nullKeyValue}, and clears the {@code values} array references.
		 * The internal capacity may remain unchanged. Increments the modification version.
		 *
		 * @return This {@code RW} instance, allowing for method chaining.
		 */
		public RW< V > clear() {
			java.util.Arrays.fill( values, 0, cardinality, null ); // Clear value array
			_clear();
			nullKeyValue = null;                              // Clear null key value
			return this;
		}
		
		/**
		 * Associates the specified value with the specified boxed {@code Byte} key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * Handles {@code null} keys.
		 *
		 * @param key   The boxed {@code Byte} key with which the specified value is to be associated. {@code null} key is permitted.
		 * @param value The value to be associated with the specified key.
		 * @return {@code true} if a new key-value mapping was added, {@code false} if the key already existed and the value was updated.
		 */
		public boolean put(  Character key, V value ) {
			if( key == null ) {
				nullKeyValue = value;
				return _add();
			}
			return put( ( char ) ( key + 0 ), value );
		}
		
		/**
		 * Associates the specified value with the specified primitive {@code byte} key in this map.
		 * <p>
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles the transition from sparse to flat mode when the 128th key is added.
		 * May resize the internal {@code values} array in sparse mode.
		 * Increments the modification version if a new key is added.
		 *
		 * @param key   The primitive {@code byte} key.
		 * @param value The value to be associated with the key (can be {@code null}).
		 * @return {@code true} if the key was not previously present (new mapping added),
		 * {@code false} if the key existed and its value was updated.
		 */
		public boolean put( char key, V value ) {
			if( get_( ( byte ) key ) ) {
				values[ values.length == 256 ?
						key & 0xFF :
						rank( ( byte ) key ) - 1 ] = value; // Set or update the value at the calculated index
				return false;
			}
			
			if( values.length == 256 ) values[ key & 0xFF ] = value;
			else if( cardinality == 127 ) {//switch to flat mode
				
				V[] values_ = equal_hash_V.copyOf( null, 256 );
				for( int token = -1, ii = 0; ( token = unsafe_token( token ) ) != -1; )
				     values_[ token & KEY_MASK ] = values[ ii++ ];
				
				( values = values_ )[ key & 0xFF ] = value;
			}
			else {
				
				int r = rank( ( byte ) key ); // Get the rank for the key
				
				Array.resize( values,
				              cardinality < values.length ?
						              values :
						              ( values = equal_hash_V.copyOf( null, values.length * 2 ) ), r, cardinality, 1 ); // Resize the 'values' array to accommodate the new value at index 'i'
				values[ r ] = value;
			}
			
			return set1( ( byte ) key );
		}
		
		/**
		 * Removes the mapping for the specified boxed {@code Byte} key from this map if present.
		 * Handles the {@code null} key case.
		 * Increments the modification version if a mapping is removed.
		 *
		 * @param key The boxed {@code Byte} key whose mapping is to be removed (can be {@code null}).
		 * @return {@code true} if a mapping was removed (the key was present), {@code false} otherwise.
		 */
		public boolean remove(  Character key ) {
			if( key == null ) {
				nullKeyValue = null;
				return _remove();
			}
			return remove( ( char ) ( key + 0 ) );
		}
		
		/**
		 * Removes the mapping for the specified primitive {@code byte} key from this map if present.
		 * May resize or shift elements in the {@code values} array if in sparse mode.
		 * Increments the modification version if a mapping is removed.
		 *
		 * @param key The primitive {@code byte} key whose mapping is to be removed.
		 * @return {@code true} if a mapping was removed (the key was present), {@code false} otherwise.
		 */
		public boolean remove( char key ) {
			if( !set0( ( byte ) key ) ) return false;
			
			if( values.length == 256 ) {
				values[ key & 0xFF ] = null;
				return true;
			}
			int r = rank( ( byte ) key );
			if( r == cardinality ) values[ r ] = null;
			Array.resize( values, values, r, cardinality + 1, -1 ); // Resize values array to remove the value at the index of removed key
			values[ cardinality ] = null;
			return true; // Return true if key was successfully removed
		}
		
		/**
		 * Creates and returns a deep copy of this modifiable map instance.
		 * <p>
		 * Produces a new {@code RW} map with the same key-value mappings. Like {@link R#clone()},
		 * this is a shallow copy with respect to the values themselves; references are copied,
		 * not the value objects.
		 *
		 * @return A clone of this {@code RW} instance.
		 */
		public RW< V > clone() { return ( RW< V > ) super.clone(); }
		
		// Static helper for equality/hashing of RW instances. Used by equal_hash() method.
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Returns a specialized {@link Array.EqualHashOf} strategy instance suitable for comparing
	 * {@code ByteObjectMap.RW<V>} maps themselves for equality and generating hash codes.
	 * <p>
	 * This allows instances of {@code ByteObjectMap.RW<V>} to be used as elements in other
	 * collections or algorithms that rely on consistent equality and hashing behavior, based on the
	 * map's content (keys, values, null key mapping) as defined by the {@link R#equals(R)}
	 * and {@link R#hashCode()} methods.
	 *
	 * @param <V> The value type parameter of the {@code RW} map. The returned strategy is generic
	 *            and works for any {@code V}.
	 * @return An {@link Array.EqualHashOf} instance that operates on {@code ByteObjectMap.RW<V>} objects.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() {
		return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT;
	}
}