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
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

/**
 * {@code ByteObjectNullMap} is an interface defining a map that uses primitive {@code byte} values as keys
 * and stores objects of type {@code V} as values.
 * <p>
 * It extends the functionality of a standard map by explicitly supporting a {@code null} key (distinct from byte keys)
 * and allowing {@code null} values to be associated with both byte keys and the null key.
 * Implementations provide efficient storage and retrieval of object values based on byte keys,
 * with specific mechanisms for handling the null key and null values.
 */
public interface UByteObjectNullMap {
	
	/**
	 * {@code R} (Read-only) is an abstract base class providing the foundation for read operations on a byte-keyed object map
	 * supporting a null key.
	 * <p>
	 * It extends {@link ByteSet.R} to manage the set of present byte keys and inherits its token-based mechanism with versioning
	 * for safe iteration over byte keys. This class manages the storage for object values associated with byte keys
	 * and the separate value associated with the null key.
	 * <p>
	 * It supports two internal storage strategies for byte key values:
	 * <ul>
	 *     <li><b>Compressed (Rank-Based):</b> Used when the number of non-null values is relatively small. Values are stored contiguously,
	 *     and their positions are determined by the rank of the key in the {@link #nulls} bitset.</li>
	 *     <li><b>Flat (One-to-One):</b> Used when the map becomes denser. More then 127 items with none-null values, Values are stored in a fixed-size array (256 elements),
	 *     where the index directly corresponds to the byte key (0-255).</li>
	 * </ul>
	 * This class is designed to be extended by concrete implementations like {@link RW} that add modification capabilities.
	 *
	 * @param <V> The type of values stored in this map.
	 */
	abstract class R< V > extends UByteSet.R implements Cloneable, JsonWriter.Source {
		/**
		 * Bitset tracking which byte keys (0-255) are associated with non-null values.
		 * <p>
		 * In <b>both Compressed and Flat strategies</b>:
		 * If {@code nulls.get_(key)} is {@code true}, it indicates that the given byte key has a non-null value stored.
		 * If {@code false}, the key either is not present in the map (according to the underlying {@link ByteSet.R})
		 * or it is present but associated with a {@code null} value.
		 * <p>
		 * In <b>Compressed (Rank-Based) Strategy</b> (when {@code values.length < 256}):
		 * This bitset is also used to calculate the rank via {@code nulls.rank(key)}. The rank (minus 1) determines the index
		 * in the compact {@link #values} array where the non-null value for that key is stored.
		 * <p>
		 * In <b>Flat (One-to-One) Strategy</b> when more then 127 items with none-null values:
		 * This bitset is not used for indexing (direct key-to-index mapping is used), but it is still maintained
		 * by modification methods (like {@link RW#put}) to track non-null value presence, although read methods like {@link #value(byte)}
		 * might rely directly on the {@code values} array content in this mode.
		 * It might become {@code null} when switching to flat mode.
		 */
		protected Array.FF nulls = new Array.FF() { };
		
		/**
		 * Array storing the object values associated with byte keys.
		 * The interpretation and size of this array depend on the current storage strategy:
		 * <p>
		 * <b>Compressed (Rank-Based) Strategy ({@code values.length < 256}):</b>
		 * <ul>
		 *     <li>Stores only the non-null values contiguously.</li>
		 *     <li>The size of this array is related to {@code nulls.cardinality()} (the count of non-null values).</li>
		 *     <li>The non-null value for a key `k` (where `nulls.get_(k)` is true) is located at index `nulls.rank(k) - 1`.</li>
		 * </ul>
		 * <p>
		 * <b>Flat (One-to-One) Strategy - when more then 127 items with none-null values ({@code values.length == 256}):</b>
		 * <ul>
		 *     <li>Has a fixed size of 256.</li>
		 *     <li>The value for a key `k` is located directly at index `k & 0xFF`.</li>
		 *     <li>Entries corresponding to keys not present in the map or keys associated with {@code null} will hold {@code null}.</li>
		 * </ul>
		 */
		public          V[]                    values;
		/**
		 * Helper object for determining equality and generating hash codes for values of type {@code V}.
		 * An instance of {@link Array.EqualHashOf} provides strategies suitable for the value type.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		/**
		 * Stores the value associated with the special {@code null} key.
		 * This field holds the value mapped to the null key, which can itself be {@code null}.
		 * Use {@link #hasNullKey()} from the parent {@link ByteSet.R} to check if a null key mapping exists.
		 */
		protected       V                      nullKeyValue;
		
		/**
		 * Returns the value associated with the {@code null} key.
		 * Use {@link #hasNullKey()} to check if a mapping for the null key actually exists.
		 *
		 * @return The value mapped to the null key, which might be {@code null}.
		 */
		public V nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Protected constructor for initializing the read-only base map.
		 * Sets up the value equality and hashing strategy using {@link Array#get(Class)}.
		 * Intended for use by subclasses.
		 *
		 * @param clazz The {@link Class} object for the value type {@code V}. Used to obtain the default {@link Array.EqualHashOf} strategy.
		 */
		protected R( Class< V > clazz ) { this.equal_hash_V = Array.get( clazz ); }
		
		/**
		 * Protected constructor for initializing the read-only base map with a custom value handling strategy.
		 * Allows specifying a custom {@link Array.EqualHashOf} for comparing and hashing values.
		 * Intended for use by subclasses.
		 *
		 * @param equal_hash_V A custom {@link Array.EqualHashOf} instance for handling values of type {@code V}.
		 */
		public R( Array.EqualHashOf< V > equal_hash_V ) { this.equal_hash_V = equal_hash_V; }
		
		
		/**
		 * Checks if this map contains the specified value among its mappings.
		 * This includes checking the value associated with the null key (if present)
		 * and all values associated with byte keys. Handles checking for {@code null} values correctly.
		 *
		 * @param value The  value to search for. Can be {@code null}.
		 * @return {@code true} if the map maps one or more keys (null or byte) to the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( V value ) { return tokenOfValue( value ) != -1; }
		
		/**
		 * Finds the token associated with the *first* occurrence of the given value in the map.
		 * Searches the null key's value first (if the null key is present), then iterates through the byte key mappings.
		 * Handles comparison correctly if the search `value` is {@code null}.
		 *
		 * @param value The value to search for. Can be {@code null}.
		 * @return The token of the first entry found with the specified value, or `-1` if the value is not found in the map.
		 * The token can represent the null key (check with {@link #isKeyNull(long)}) or a byte key.
		 */
		public long tokenOfValue( V value ) {
			if( hasNullKey && equal_hash_V.equals( value, nullKeyValue ) ) return super.tokenOf( KEY_MASK );
			
			if( values.length == 256 ) {
				for( int t = -1; ( t = unsafe_token( t ) ) != -1; )
					if( equal_hash_V.equals( value, values[ t & KEY_MASK ] ) ) return t;
			}
			else
				for( int t = -1; ( t = unsafe_token( t ) ) != -1; )
					if( nulls.get_( ( byte ) ( t & KEY_MASK ) ) ) { if( equal_hash_V.equals( value, values[ t >>> KEY_LEN ] ) ) return t; }
					else if( value == null ) return t;
			
			return -1;
		}
		
		/**
		 * Checks if the key represented by the given token exists in the map AND is associated with a non-null value.
		 *
		 * @param token The token representing a potential key-value pair.
		 * @return {@code true} if the token represents a key present in the map
		 * with an associated non-null value,
		 * {@code false} otherwise (key not present,
		 * xkey present but value is null, or token invalid).
		 */
		public boolean hasValue( long token ) {
			return isKeyNull( token ) ?
					hasNullKey && nullKeyValue != null :
					-1 < ( int ) token;
		}
		
		/**
		 * Retrieves the non-null value associated with the key represented by a given token.
		 * <p>
		 * <b>Precondition:</b> {@link #hasValue(long)} must return {@code true} for the given
		 * {@code token} before calling this method.
		 *
		 * @param token The token representing a key-value pair with a non-null value.
		 * @return The non-null value associated with the key represented by the token.
		 */
		public V value( long token ) {
			return isKeyNull( token ) ?
					nullKeyValue :
					values[ ( int ) token >> KEY_LEN ];
		}
		
		/**
		 * Gets the value associated with a boxed {@link Byte} key.
		 * Allows using a {@code Byte} object (including {@code null}) as the key.
		 *
		 * @param key The boxed {@link Byte} key to look up. Can be {@code null}.
		 * @return The value associated with the key. Returns {@link #nullKeyValue()} if the key is {@code null}.
		 * Returns the value associated with the byte key if the key is non-null and present.
		 * Returns {@code null} if the key is non-null but not found, or if the key is found but its associated value is {@code null}.
		 */
		public V value(  Character key ) {
			return key == null ?
					nullKeyValue :
					// Handle null key case
					value( ( byte ) ( key + 0 ) ); // Key not found
		}
		
		/**
		 * Gets the value associated with a primitive {@code byte} key.
		 *
		 * @param key The primitive {@code byte} key to look up.
		 * @return The value associated with the key. Returns {@code null} if the key is not found in the map,
		 * or if the key is found but its associated value is {@code null}.
		 */
		public V value( char key ) {
			return
					values.length == 256 ?
							values[ key & 0xFF ] :
							nulls.get_( ( byte ) ( key + 0 ) ) ?
									values[ nulls.rank( ( byte ) ( key + 0 ) ) - 1 ] :
									null;
		}
		
		
		/**
		 * Computes the hash code for this map.
		 * The hash code is based on the hash codes of the key-value mappings (including the null key mapping, if present)
		 * and incorporates the structural state and version information from the parent {@link ByteSet.R}.
		 * It uses the {@link #equal_hash_V} strategy to compute hash codes for values.
		 *
		 * @return The hash code of this map.
		 */
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			int h = Array.mix( seed, super.hashCode() ); // Mix seed with superclass hashCode (ByteSet.R)
			
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; )
				if( hasValue( t ) ) {
					h = Array.mix( h, Array.hash( value( t ) ) );
					
					h = Array.finalizeHash( h, 2 ); // Finalize hash for this part
					a += h;
					b ^= h;
					c *= h | 1; // Accumulate hash components
				}
			if( hasNullKey ) {
				h = Array.hash( seed ); // Start hash for null key
				h = Array.mix( h, Array.hash( nullKeyValue != null ?
						                              // Mix with hash of nullKeyValue (or seed if nullKeyValue is null)
						                              equal_hash_V.hashCode( nullKeyValue ) :
						                              seed ) );
				h = Array.finalizeHash( h, 2 ); // Finalize hash for null key part
				a += h;
				b ^= h;
				c *= h | 1; // Accumulate hash components
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() ); // Finalize overall hash with size and accumulated components
		}
		
		private static final int seed = IntObjectMap.R.class.hashCode(); // Seed for hash calculations
		
		/**
		 * Checks if this map is equal to another object.
		 * Equality requires the other object to be of the same class (or a compatible {@code R})
		 * and to have the same key-value mappings (including null key status and value)
		 * and the same underlying {@link ByteSet.R} state.
		 * Value comparison uses the {@link #equal_hash_V} strategy.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj ); }
		
		/**
		 * Compares this map to another {@code R} instance for equality.
		 *
		 * @param other The other {@code R} instance to compare with.
		 * @return {@code true} if the maps represent the same mappings, {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) {
			if( other == this ) return true;
			if( other == null ||
			    hasNullKey != other.hasNullKey || hasNullKey && !equal_hash_V.equals( nullKeyValue, other.nullKeyValue ) ||
			    size() != other.size() ||
			    !super.equals( other ) || !nulls.equals( other.nulls ) )
				return false;
			
			if( values.length == 256 && other.values.length == 256 || values.length != 256 && other.values.length != 256 )
				return Array.equals( values, other.values, 0, nulls.cardinality );
			
			int t1 = -1, t2 = -1;
			for( t1 = unsafe_token( t1 ), t2 = other.unsafe_token( t2 ); t1 != -1 && t2 != -1; t1 = unsafe_token( t1 ), t2 = other.unsafe_token( t2 ) )
				if( hasValue( t1 ) && ( !other.hasValue( t2 ) || !equal_hash_V.equals( value( t1 ), other.value( t2 ) ) ) ) return false;
			
			return t1 == t2;
		}
		
		/**
		 * Internal helper to potentially update a token structure.
		 * Seems intended to adjust the index part based on strategy, but implementation details are unclear
		 * and might be outdated or specific to a particular use case not fully shown.
		 * The current implementation seems complex and potentially incorrect as it mixes adding offsets
		 * with bitwise operations in a non-standard way.
		 * It's generally safer to rely on `tokenOf(key)` for fresh tokens.
		 *
		 * @param token The previous token.
		 * @param key   The current key being processed.
		 * @return An updated token representation (structure might depend on strategy).
		 */
		protected long token( long token, int key ) {
			return ( long ) _version << VERSION_SHIFT | (
					
					values.length == 256 ?
							values[ key ] == null ?
									-1 :
									( long ) key << KEY_LEN :
							nulls.get_( ( byte ) key ) ?
									( ( int ) token & ~KEY_MASK ) + ( 1 << KEY_LEN ) & 0x7FFF_FFFFL :
									token & ( long ) ~KEY_MASK | 0x8000_0000L
			) | key;
		}
		
		/**
		 * Internal helper to create a token for a given byte key.
		 * The token encodes the map version, the key, and information about the value's location
		 * (index in `values` array if non-null and compressed, or just the key if flat)
		 * or an indicator if the value is null.
		 *
		 * @param key The byte key (0-255).
		 * @return A token representing the key and its value state/location.
		 */
		protected long tokenOf( int key ) {
			return ( long ) _version << VERSION_SHIFT |
			       (
					       values.length == 256 ?
							       values[ key ] == null ?
									       -1 :
									       ( long ) key << KEY_LEN :
							       nulls.get_( ( byte ) key ) ?
									       nulls.rank( ( byte ) key ) - 1L << KEY_LEN :
									       ~KEY_MASK & 0xFFFF_FFFFL
			       ) | key;
		}
		
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over the byte keys present in the map's underlying {@link ByteSet}.
		 * This iterator yields tokens for <strong>all present byte keys</strong> (as managed by {@link ByteSet.R}),
		 * regardless of whether their associated value in this map is null or non-null.
		 * It does NOT iterate over the special null key. Use {@link #hasNullKey()} and {@link #nullKeyValue()} separately for the null key mapping.
		 * <p>
		 * Start iteration by passing {@code -1} to this method. In subsequent calls, pass the previously returned *integer* token
		 * (lower 32 bits of the long token if using that type elsewhere) to get the next one.
		 * Iteration ends when {@code -1} is returned.
		 * <p>
		 * <strong>Warning:</strong> This method bypasses concurrency checks (version checking is NOT performed during iteration).
		 * If the map's structure (keys added/removed) or value associations (affecting compressed mode ranks) are modified concurrently
		 * (even in the same thread via {@link RW} methods), the behavior of the iteration is undefined and may lead to incorrect results,
		 * missed/repeated elements, {@link IndexOutOfBoundsException}, or other errors. Use this method only when certain that the map
		 * will not be modified during the iteration loop. For safe iteration, consider collecting keys/tokens first or using higher-level iteration mechanisms if available.
		 * <p>
		 * The returned {@code int} token represents the lower 32 bits of the internal token structure, primarily containing the key.
		 * To get the full {@code long} token (including version and potential index info), you might need internal helpers like {@link #tokenOf},
		 * but using the returned {@code int} token directly with {@link #key(long)} (after casting) is typical.
		 * Use {@link #value(byte)} with {@code key(token)} to get the value associated with the iterated key safely (though null checks might be needed).
		 *
		 * @param token The previous integer token returned by this method, or {@code -1} to start the iteration.
		 *              The internal state of the iterator uses the key part of the token.
		 * @return The next integer token representing a byte key present in the map's key set, or {@code -1} if iteration is complete.
		 * @see #key(long) To get the byte key from a token (cast int token to long if needed, or use internal structure knowledge).
		 * @see #value(byte) To get the value for the key obtained from the token.
		 * @see #hasNullKey() To check for the presence of the null key separately.
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
		 * Creates and returns a shallow copy of this {@code R} instance.
		 * The clone will have its own copies of the internal arrays (e.g., {@code values}) and the {@code nulls} bitset.
		 * However, the values themselves (if they are objects) are not deep-copied; both the original and the clone will reference the same value objects.
		 * The clone will share the same {@link #equal_hash_V} strategy instance.
		 *
		 * @return A shallow copy of this map.
		 * @throws RuntimeException wrapping the {@link CloneNotSupportedException} if cloning fails (should not happen as {@code R} implements {@link Cloneable}).
		 */
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			
			try {
				R< V > dst = ( R< V > ) super.clone(); // Clone the ByteSet.R part
				dst.values = values.clone(); // Shallow copy of values array
				dst.nulls  = ( Array.FF ) nulls.clone(); // Shallow copy of nulls array
				return dst;
			} catch( CloneNotSupportedException e ) { }
			return null; // Cloning failed
		}
		
		/**
		 * Returns a JSON string representation of this map.
		 * Delegates to {@link #toJSON(JsonWriter)}.
		 *
		 * @return A JSON string representing the map's contents.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the contents of this map in JSON format to the provided {@link JsonWriter}.
		 * Outputs a JSON object where keys are string representations of the byte keys (e.g., "10", "-1")
		 * or the literal string "null" for the null key. Values are the JSON representations of the corresponding
		 * object values (or JSON `null` if the value is {@code null}).
		 * Iteration uses the potentially unsafe {@link #unsafe_token} for performance; ensure no concurrent modification.
		 *
		 * @param json The {@link JsonWriter} to write the JSON output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 ); // Pre-allocate buffer space for JSON output (heuristic size)
			json.enterObject(); // Begin JSON object
			if( hasNullKey )
				json.name().value( nullKeyValue );
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				json.name( key( token ) );
				if( hasValue( token ) )
					json.value( value( token ) ); // Compressed (Rank-Based) Strategy - Rank calculation for sparse access
				else
					json.value(); // Output null for null value
			}
			
			json.exitObject(); // End JSON object
		}
	}
	
	/**
	 * {@code RW} (Read-Write) is a concrete implementation of {@link ByteObjectNullMap} that extends the read-only base class {@link R}
	 * and provides methods for modifying the map (adding, updating, removing entries).
	 * <p>
	 * It supports both compressed and flat storage strategies, automatically transitioning from compressed to flat
	 * when the map becomes dense enough to benefit from direct indexing when more then 127 items with none-null values. All modification operations update the
	 * map's version for concurrency control.
	 *
	 * @param <V> The type of values stored in this map.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs a new, empty read-write map with an estimated initial capacity,
		 * using the default equality/hashing strategy for the specified value type.
		 *
		 * @param clazz  The {@link Class} of the value type {@code V}. Used to obtain the default {@link Array.EqualHashOf} strategy.
		 * @param length A hint for the initial capacity (number of expected entries). The actual internal storage size might differ.
		 *               If length >= 128 (a threshold suggesting potential density), it might start in Flat mode directly.
		 */
		public RW( Class< V > clazz, int length ) {
			this( Array.get( clazz ), length );
		}
		
		/**
		 * Constructs a new, empty read-write map with an estimated initial capacity and a custom equality/hashing strategy
		 * for the value type.
		 *
		 * @param equal_hash_V The custom {@link Array.EqualHashOf} instance to use for value comparisons and hashing.
		 * @param length       A hint for the initial capacity. If length suggests high density (e.g., >= 128),
		 *                     it might initialize directly into the Flat storage strategy.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int length ) {
			super( equal_hash_V );
			values = this.equal_hash_V.copyOf( null, Math.max( Math.min( length, 0x100 ), 16 ) ); // Initialize values array with a minimum size and up to max byte value range
		}
		
		/**
		 * Removes all mappings from this map. The map will be empty after this call returns.
		 * Resets the null key mapping and clears all byte key mappings. Resets internal structures and increments the version.
		 *
		 * @return This {@code RW} instance, allowing for method chaining.
		 */
		public RW< V > clear() {
			if( 0 < cardinality ) {
				if( values.length < 256 ) nulls._clear();
				java.util.Arrays.fill( values, null ); // Clear values array
			}
			_clear(); // Clear ByteSet.R state
			nullKeyValue = null; // Reset null key value
			return this;
		}
		
		/**
		 * Associates the specified value with the null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The value to be associated with the null key. Can be {@code null}.
		 * @return {@code true} if the null key was not present before this call (a new mapping was added),
		 * {@code false} if the null key was already present (the value was updated).
		 * The return value reflects the change in the presence of the *key*.
		 */
		public boolean put(  Character key, V value ) {
			if( key != null ) return put( ( char ) ( key + 0 ), value ); // Delegate to primitive byte key put method
			
			nullKeyValue = value; // Set null key value
			return _add(); // Add null key entry (ByteSet.R logic)
		}
		
		/**
		 * Associates the specified value with the specified primitive {@code byte} key in this map.
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * Handles the transition from compressed to flat storage strategy when more then 127 items with none-null values.
		 * If the provided `value` is {@code null}, this method effectively ensures the key exists but is mapped to `null`.
		 * If the intent is to *remove* the key entirely when setting the value to null, use {@link #remove(byte)} instead.
		 *
		 * @param key   The primitive {@code byte} key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key. Can be {@code null}.
		 * @return {@code true} if the key was not present before this call (a new mapping was added),
		 * {@code false} if the key was already present (the value was updated or set to null).
		 * The return value reflects the change in the presence of the *key*.
		 */
		public boolean put( char key, V value ) {
			
			if( value == null ) {
				
				if( values.length == 256 ) values[ key & 0xFF ] = null;
				else if( nulls.set0( ( byte ) key ) ) {
					Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.cardinality + 1, -1 ); // Remove key from nulls array and resize values array if needed
					values[ nulls.cardinality ] = null;
				}
			}
			else if( values.length == 256 ) values[ key & 0xFF ] = value;
			else if( nulls.cardinality == 127 && !nulls.get_( ( byte ) key ) ) {//switch to flat mode
				V[] values_ = equal_hash_V.copyOf( null, 256 );
				for( int token = -1, ii = 0; ( token = unsafe_token( token ) ) != -1; )
					if( hasValue( token ) )
						values_[ token & KEY_MASK ] = values[ ii++ ];
				
				nulls                              = null; // Discard nulls bitset (no longer used for indexing)
				( values = values_ )[ key & 0xFF ] = value;
			}
			else {
				
				int r = nulls.rank( ( byte ) key ); // Get the rank for the key
				if( nulls.set1( ( byte ) key ) )
					Array.resize( values,
					              nulls.cardinality < values.length ?
							              values :
							              ( values = equal_hash_V.copyOf( null, values.length * 2 ) ), r, nulls.cardinality - 1, 1 );
				else r--;
				
				values[ r ] = value;
			}
			
			
			return set1( ( byte ) key ); // return true if key was added, false otherwise
		}
		
		/**
		 * Removes the mapping for the specified key from this map if present.
		 * Handles both the null key and byte keys.
		 *
		 * @param key The key whose mapping is to be removed. Can be {@code null} to remove the null key mapping,
		 *            or a boxed {@link Byte} for a byte key.
		 * @return {@code true} if a mapping was removed as a result of this call (i.e., the key was present),
		 * {@code false} if the key was not found in the map.
		 */
		public boolean remove(  Character key ) {
			if( key != null ) return remove( ( char ) ( key + 0 ) ); // Delegate to primitive byte key remove if key is not null
			
			nullKeyValue = null; // Reset null key value
			return _remove(); // Remove null key entry (ByteSet.R logic)
		}
		
		/**
		 * Removes the mapping for the specified primitive {@code byte} key from this map if present.
		 *
		 * @param key The primitive {@code byte} key whose mapping is to be removed.
		 * @return {@code true} if a mapping was removed as a result of this call (i.e., the key was present),
		 * {@code false} if the key was not found in the map.
		 */
		public boolean remove( char key ) {
			if( !set0( ( byte ) key ) ) return false; // Remove key from ByteSet.R, return false if key wasn't present
			
			if( values.length == 256 ) values[ key & 0xFF ] = null;
			else if( nulls.set0( ( byte ) key ) ) {
				Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.cardinality + 1, -1 ); // Resize values array to remove the gap
				values[ nulls.cardinality ] = null;
			}
			
			return true; // Mapping removed
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code RW} instance.
		 * This is equivalent to calling {@link R#clone()} but returns the type {@code RW<V>}.
		 * The clone is a new {@code RW} map with the same initial mappings, but modifications
		 * to one map (e.g., adding/removing keys) will not affect the other. The referenced values
		 * themselves are not deep-copied.
		 *
		 * @return A shallow copy (clone) of this map instance.
		 */
		public RW< V > clone() { return ( RW< V > ) super.clone(); } // Simply cast the shallow clone from superclass to RW<V>
		
		// Static helper for equality/hashing of RW instances
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class ); // Static instance used for default EqualHashOf<RW<V>>
	}
	
	/**
	 * Provides a static factory method to obtain an {@link Array.EqualHashOf} instance suitable for
	 * comparing {@link ByteObjectNullMap.RW RW<V>} map instances themselves based on their content.
	 * Note: This compares the maps, not the values *within* the maps (which use `equal_hash_V`).
	 *
	 * @param <V> The value type parameter of the {@code RW} maps to be compared.
	 * @return A shared {@link Array.EqualHashOf} instance capable of comparing {@code RW<V>} maps.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() {
		return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; // Return the static OBJECT instance
	}
}