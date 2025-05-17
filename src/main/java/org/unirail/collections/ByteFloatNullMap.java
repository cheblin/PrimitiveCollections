// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// 1. The above copyright notice and this permission notice shall be included in all
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
// FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.ConcurrentModificationException;
import java.util.HashMap;

/**
 * {@code ByteIntNullMap} is an interface defining a map that associates primitive {@code byte} keys
 * with nullable primitive {@code int} values.
 * <p>
 * Key Features:
 * <ul>
 *     <li>Supports a special {@code null} key, distinct from any {@code byte} value.</li>
 *     <li>Allows associating {@code null} values with any key (including the {@code null} key).</li>
 *     <li>Built upon {@link ByteSet.RW} for efficient management of the {@code byte} key set (excluding the null key).</li>
 *     <li>Employs two internal strategies for storing primitive values to optimize for both space and performance based on the density of non-null value entries.</li>
 * </ul>
 * <p>
 * Value Storage Optimization:
 * The map dynamically transitions between two strategies:
 * <ol>
 *     <li><b>Compressed (Rank-Based) Strategy:</b> Used initially and when the map is sparse (fewer than 128 non-null values). It stores only non-null values compactly, saving memory. Value lookup requires calculating the key's rank among non-null entries.</li>
 *     <li><b>Flat (One-to-One) Strategy:</b> Activated when the 128th non-null value is added. Uses a fixed-size array (256 elements) where the index directly maps to the byte key's unsigned value (0-255). This provides very fast O(1) lookups for non-null values, sacrificing some potential memory efficiency for speed in denser maps.</li>
 * </ol>
 * This adaptive behavior aims to provide good performance across different map densities. Concrete implementations are provided by the nested {@link R} (read-only) and {@link RW} (read-write) classes.
 */
public interface ByteFloatNullMap {
	
	/**
	 * {@code R} is an abstract base class providing read-only access to a {@link ByteIntNullMap}.
	 * <p>
	 * It extends {@link ByteSet.R} to manage the set of non-null {@code byte} keys.
	 * Values associated with keys are stored, potentially using one of two internal strategies (Compressed or Flat)
	 * detailed below, allowing for nullable primitive {@code int} values. It also handles the special {@code null} key separately.
	 * This class is designed for efficient read operations.
	 * <p>
	 * <b>Value Storage Strategies:</b>
	 * This class utilizes two primary strategies to manage the mapping between byte keys and their associated {@code int} values, balancing memory usage and access speed:
	 * <ol>
	 *     <li><b>Compressed (Rank-Based) Strategy (Default for Sparse Maps):</b>
	 *         <ul>
	 *             <li>Active when the number of keys associated with <i>non-null</i> values is less than 128.</li>
	 *             <li>The {@link #values} array stores only the non-null {@code int} values contiguously.</li>
	 *             <li>The {@link #nulls} bitset tracks which byte keys (0-255) have a non-null value associated with them.</li>
	 *             <li>To find the value for a key `k`, the map first checks `nulls.get(k)`. If true, it calculates the rank `r = nulls.rank(k)` (the number of non-null entries up to and including `k`). The value is then found at `values[r - 1]`.</li>
	 *             <li>Memory efficient for sparse maps as it avoids allocating space for keys with null values in the {@code values} array.</li>
	 *             <li>Lookup time includes the cost of the rank calculation, which can increase slightly with the number of non-null entries.</li>
	 *         </ul>
	 *     </li>
	 *     <li><b>Flat (One-to-One) Strategy (For Denser Maps):</b>
	 *         <ul>
	 *             <li>Activated automatically by the {@link RW} class when the 128th non-null value is added. The map remains in this state thereafter.</li>
	 *             <li>The {@link #values} array is resized to a fixed length of 256.</li>
	 *             <li>The {@link #nulls} bitset continues to track which keys have non-null values.</li>
	 *             <li>To find the value for a key `k`, the map checks `nulls.get(k)`. If true, the value is directly accessed at `values[k & 0xFF]`. The rank calculation is no longer needed.</li>
	 *             <li>Provides very fast, constant-time O(1) lookups for non-null values once the presence is confirmed via {@link #nulls}.</li>
	 *             <li>Uses a fixed amount of memory (256 integers) for the {@code values} array, regardless of the actual number of non-null entries, trading potential memory savings for consistent high-speed access.</li>
	 *         </ul>
	 *     </li>
	 * </ol>
	 * The map manages the transition transparently. Read operations correctly retrieve values regardless of the current internal strategy.
	 * The special {@code null} key's value is handled separately using {@link #nullKeyValue}, {@link #nullKeyHasValue}.
	 */
	abstract class R extends ByteSet.R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Bitset tracking non-null value association for byte keys (0-255).
		 * <p>
		 * In <b>both Compressed and Flat strategies</b>:
		 * If {@code nulls.get(key)} is {@code true}, it indicates that the given byte key has a non-null {@code int} value stored.
		 * If {@code false}, the key either is not present in the map or is associated with a {@code null} value.
		 * <p>
		 * In <b>Compressed (Rank-Based) Strategy</b>:
		 * This bitset is also used to calculate the rank via {@code nulls.rank(key)}, which determines the index in the compact {@link #values} array where the non-null value is stored.
		 * <p>
		 * In <b>Flat (One-to-One) Strategy</b>:
		 * This bitset serves as a fast check for non-null presence before accessing the {@link #values} array at the direct index {@code key & 0xFF}.
		 * Its {@code cardinality} reflects the total count of byte keys associated with non-null values.
		 */
		protected Array.FF nulls = new Array.FF() { };
		
		/**
		 * Array storing the primitive {@code int} values associated with byte keys.
		 * The interpretation of this array depends on the current storage strategy:
		 * <p>
		 * <b>Compressed (Rank-Based) Strategy ({@code values.length < 256}):</b>
		 * <ul>
		 *     <li>Stores only the non-null values contiguously.</li>
		 *     <li>The size of this array typically corresponds to {@code nulls.cardinality}.</li>
		 *     <li>The value for a key `k` (where `nulls.get(k)` is true) is located at index `nulls.rank(k) - 1`.</li>
		 * </ul>
		 * <p>
		 * <b>Flat (One-to-One) Strategy ({@code values.length == 256}):</b>
		 * <ul>
		 *     <li>Has a fixed size of 256.</li>
		 *     <li>The value for a key `k` (where `nulls.get(k)` is true) is located directly at index `k & 0xFF`.</li>
		 *     <li>Entries corresponding to keys with null values or keys not present in the map are unused or hold default values (0), but are only considered valid if `nulls.get(key)` is true.</li>
		 * </ul>
		 */
		protected float[]          values;
		/**
		 * Stores the primitive {@code int} value associated with the special {@code null} key.
		 * This field is only relevant if {@link #hasNullKey} is true and {@link #nullKeyHasValue} is true.
		 */
		protected float            nullKeyValue = 0;
		
		/**
		 * Flag indicating if the special {@code null} key is present in the map and is associated with a non-null value.
		 * If true, {@link #nullKeyValue} holds the associated primitive value. else a null
		 */
		protected boolean nullKeyHasValue; // Indicates if the null key has a non-null value
		
		/**
		 * Checks if the special {@code null} key is present and associated with a non-null value.
		 *
		 * @return {@code true} if the {@code null} key exists and has a non-null value, {@code false} otherwise.
		 */
		public boolean nullKeyHasValue() { return nullKeyHasValue; }
		
		/**
		 * Returns the primitive {@code int} value associated with the special {@code null} key.
		 * Call this method only if {@link #hasNullKey()} and {@link #nullKeyHasValue()} both return true.
		 * If the null key is associated with null, the behavior is undefined (might return 0).
		 *
		 * @return The primitive {@code int} value associated with the null key.
		 */
		public float nullKeyValue() { return  nullKeyValue; }
		
		/**
		 * Checks if this map contains the specified value (boxed Integer). This includes checking the value
		 * associated with the null key, if present, and values associated with byte keys.
		 * Handles checking for {@code null} values as well.
		 *
		 * @param value The  value to search for. Can be {@code null}.
		 * @return {@code true} if the map maps one or more keys to the specified value, {@code false} otherwise.
		 */
		public boolean containsValue(  Float     value ) { return tokenOfValue( value ) != -1; }
		/**
		 * Checks if the map contains the specified key as a {@code Byte} object.
		 *
		 * @param key the key to search for
		 * @return {@code true} if the key is found, {@code false} otherwise
		 */
		public boolean containsKey(  Byte      key ) { return super.contains( key ); }
		
		/**
		 * Checks if the map contains the specified primitive {@code byte} key.
		 *
		 * @param key the primitive key to search for
		 * @return {@code true} if the key is found, {@code false} otherwise
		 */
		public boolean containsKey( byte key ) { return super.contains( key ); }
		
		/**
		 * Checks if this map contains the specified primitive {@code int} value. This includes checking the value
		 * associated with the null key, if present, and values associated with byte keys.
		 * This method cannot find entries explicitly set to {@code null}. Use {@link #containsValue} with a {@code null} argument for that.
		 *
		 * @param value The primitive {@code int} value to search for.
		 * @return {@code true} if the map maps one or more keys to the specified non-null value, {@code false} otherwise.
		 */
		public boolean containsValue( float value ) { return tokenOfValue( value ) != -1; }
		
		/**
		 * Finds the first token associated with the given value (boxed Integer).
		 * Searches both the null key's value and all byte key values. Handles {@code null} search value.
		 *
		 * @param value The  value to find. Can be {@code null}.
		 * @return The token of the first entry found with the specified value, or -1 if not found.
		 * The token can represent the null key (see {@link #isKeyNull(long)}) or a byte key.
		 */
		public long tokenOfValue(  Float     value ) {
			if( value == null ) {
				if( hasNullKey && !nullKeyHasValue ) return token( KEY_MASK );
				for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) if( !nulls.get_( ( byte ) ( t & KEY_MASK ) ) ) return t;
				return -1;
			}
			
			return tokenOfValue( value. floatValue     () );
		}
		
		/**
		 * Finds the first token associated with the given primitive {@code int} value.
		 * Searches both the null key's value (if non-null) and all byte keys with non-null values.
		 *
		 * @param value The primitive {@code int} value to find.
		 * @return The token of the first entry found with the specified value, or -1 if not found.
		 * The token can represent the null key or a byte key.
		 */
		public long tokenOfValue( float value ) {
			if( hasNullKey && nullKeyHasValue && nullKeyValue == value ) return token( KEY_MASK );
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; )
				if( nulls.get_( ( byte ) ( t & KEY_MASK ) ) && values[ t >>> KEY_LEN ] == value ) return t;
			return -1;
		}
		
		/**
		 * Checks if the entry associated with the given token has a non-null value.
		 *
		 * @param token A valid token obtained from iteration or {@link #tokenOfValue} / {@link #tokenOfValue}.
		 * @return {@code true} if the entry corresponding to the token has a non-null value,
		 * {@code false} if the value is null or the token represents the null key which has a null value.
		 * @throws ConcurrentModificationException if the map was modified since the token was obtained.
		 * @throws IndexOutOfBoundsException       if the token is invalid (e.g., corrupted or from a different map).
		 */
		public boolean hasValue( long token ) {
			return isKeyNull( token ) ?
					nullKeyHasValue :
					-1 < ( int ) token;
		}
		
		/**
		 * Retrieves the primitive {@code int} value associated with the given token.
		 * <p>
		 * <b>Precondition:</b> This method should only be called if {@link #hasValue(long)} returns {@code true} for the given token.
		 * Calling it for a token associated with a {@code null} value results in undefined behavior (likely returning 0 or a stale value depending on the strategy).
		 * <p>
		 * The retrieval mechanism depends on the map's current internal storage strategy:
		 * <ul>
		 *     <li>If the token represents the null key, returns {@link #nullKeyValue}.</li>
		 *     <li>If the token represents a byte key and the map is in <b>Flat Strategy</b> ({@code values.length == 256}), it returns {@code values[key & 0xFF]}.</li>
		 *     <li>If the token represents a byte key and the map is in <b>Compressed Strategy</b> ({@code values.length < 256}), it calculates the rank and returns {@code values[nulls.rank(key) - 1]}.</li>
		 * </ul>
		 *
		 * @param token A valid token for an entry that has a non-null value.
		 * @return The primitive {@code int} value associated with the token.
		 */
		public float value( long token ) {
			return  ( isKeyNull( token ) ?
					nullKeyValue :
					values[ ( int ) token >> KEY_LEN ] );
		}
		
		public R get( HashMap<  Byte     ,  Float     > dst ) {
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; )
				if( hasValue( t ) ) dst.put( key( t ), value( t ) );
				else dst.put( key( t ), null );
			
			if( hasNullKey )
				if( nullKeyHasValue ) dst.put( null,  nullKeyValue );
				else dst.put( null, null );
			
			return this;
		}
		
		
		/**
		 * Computes the hash code for this map.
		 * The hash code is derived from the hash codes of the underlying key set (from {@link ByteSet.R}),
		 * the non-null values, the state of the {@link #nulls} bitset, and the state of the null key mapping (if present).
		 * It adheres to the general contract for {@link Object#hashCode()}.
		 *
		 * @return The hash code value for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			int h = seed;
			
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; )
				if( hasValue( t ) ) {
					h = Array.mix( h, Array.hash( values[ t >>> KEY_LEN ] ) );
					
					h = Array.finalizeHash( h, 2 );
					a += h;
					b ^= h;
					c *= h | 1;
				}
			
			if( hasNullKey ) {
				h = nullKeyHasValue ?
						Array.mix( seed, nullKeyHasValue ?
								Array.hash( nullKeyValue ) :
								22633363 ) :
						Array.hash( seed );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares the specified object with this map for equality.
		 * Returns {@code true} if the given object is also a {@code ByteIntNullMap.R}, the two maps have the same size,
		 * contain the same keys, and map each key to the same value (including handling of {@code null} keys and {@code null} values).
		 * The comparison is independent of the internal storage strategy (Compressed or Flat) used by either map.
		 *
		 * @param obj The object to be compared for equality with this map.
		 * @return {@code true} if the specified object is equal to this map.
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		/**
		 * Compares this read-only map to another {@code ByteIntNullMap.R} for equality.
		 * <p>
		 * Equality is determined by comparing the presence of a null key, the associated value for the null key (if any),
		 * the size of the maps, and the key-value pairs for non-null keys.
		 *
		 * @param other The {@code ByteIntNullMap.R} to compare to.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    hasNullKey && ( nullKeyHasValue != other.nullKeyHasValue || nullKeyHasValue && nullKeyValue != other.nullKeyValue ) ||
			    size() != other.size() ||
			    !super.equals( other ) || !nulls.equals( other.nulls ) )
				return false;
			
			int t1 = -1, t2 = -1;
			for( t1 = unsafe_token( t1 ), t2 = other.unsafe_token( t2 ); t1 != -1 && t2 != -1; t1 = unsafe_token( t1 ), t2 = other.unsafe_token( t2 ) )
				if( hasValue( t1 ) && ( !other.hasValue( t2 ) || value( t1 ) != other.value( t2 ) ) ) return false;
			
			return t1 == t2;
		}
		
		// Internal helper for token generation/update, possibly accounting for strategy
		protected long token( long token, int key ) {
			return ( long ) _version << VERSION_SHIFT | (
					nulls.get_( ( byte ) key ) ?
							values.length == 256 ?
									( long ) key << KEY_LEN :
									( ( int ) token & ~KEY_MASK ) + ( 1 << KEY_LEN ) & 0x7FFF_FFFFL :
							token & ( long ) ~KEY_MASK | 0x8000_0000L
			) | key;
		}
		
		protected long tokenOf( int key ) {
			return ( long ) _version << VERSION_SHIFT |
			       (
					       nulls.get_( ( byte ) key ) ?
							       ( values.length == 256 ?
									       ( long ) key :
									       nulls.rank( ( byte ) key ) - 1
							       ) << KEY_LEN :
							       ~KEY_MASK & 0xFFFF_FFFFL
			       ) | key;
		}
		
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over the byte keys present in the map's underlying {@link ByteSet}.
		 * This iterator yields tokens for <strong>all present byte keys</strong>, regardless of whether their associated value is null or non-null.
		 * It does NOT iterate over the special null key. Use {@link #hasNullKey()} separately.
		 * <p>
		 * Start iteration by passing {@code -1} to this method. In subsequent calls, pass the previously returned token to get the next one.
		 * Iteration ends when {@code -1} is returned.
		 * <p>
		 * <strong>Warning:</strong> This method bypasses concurrency checks (version checking). If the map's structure
		 * (the set of keys) is modified concurrently (e.g., by adding or removing keys in another thread or even the same thread
		 * via {@link RW} methods), the behavior of the iteration is undefined. It may lead to missed keys, repeated keys,
		 * {@link IndexOutOfBoundsException}, or other errors. Use this method only when you are certain that the map's key set
		 * will not be modified during the iteration. For safe iteration, use standard iterators or re-check map state frequently.
		 * <p>
		 * The returned token incorporates the current modification version and the key. Use {@link #key(long)} to extract the key.
		 * Use {@link #hasValue(long)} and {@link #value(long)} to access the value safely based on the token.
		 *
		 * @param token The previous token returned by this method, or {@code -1} to start the iteration.
		 *              The upper bits of the token are used internally to track iteration progress and are not simply the value index.
		 * @return The next token representing a byte key present in the map, or {@code -1} if iteration is complete.
		 * @see #token(long) For safe iteration (if applicable, though typically not provided for raw tokens).
		 * @see #key(long) To get the byte key from a token.
		 * @see #hasValue(long) To check if the token corresponds to a non-null value.
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
		 * Creates and returns a deep copy of this read-only map view.
		 * The clone will have the same key-value mappings, including the null key state,
		 * and the same internal storage strategy (Compressed or Flat) as the original at the time of cloning.
		 * The cloned map's state is independent of the original.
		 *
		 * @return A deep clone of this {@code ByteIntNullMap.R} instance.
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				dst.values = values.clone(); // Shallow copy; deep copy may be needed for mutable V
				dst.nulls  = ( Array.FF ) nulls.clone();
				return dst;
			} catch( CloneNotSupportedException ignored ) { }
			return null;
		}
		
		/**
		 * Returns a string representation of this map in JSON format.
		 * Example: `{"1":10, "null":null, "5":-50}`
		 * Keys are represented as strings. The special null key is represented as "null".
		 * Values are represented as JSON numbers or the JSON literal `null`.
		 *
		 * @return A JSON string representation of the map.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the content of this map to the provided {@link JsonWriter}.
		 * The map is written as a JSON object. Keys are converted to strings (byte keys as decimal strings, the null key as "null").
		 * Values are written as JSON numbers for non-null {@code int} values, and as JSON `null` for null values.
		 * The order of entries in the JSON output is not guaranteed but typically follows key iteration order.
		 *
		 * @param json The {@link JsonWriter} instance to write the JSON output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey )
				json.name().value( nullKeyHasValue ?
						                   nullKeyValue :
						                   null );
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				json.name( key( token ) );
				if( hasValue( token ) )
					json.value( value( token ) ); // Compressed (Rank-Based) Strategy - Rank calculation for sparse access
				else
					json.value(); // Output null for null value
			}
			json.exitObject();
		}
	}
	
	/**
	 * {@code RW} (Read-Write) class provides a mutable implementation of {@link ByteIntNullMap}.
	 * <p>
	 * It extends {@link ByteIntNullMap.R} and allows modification of the map, including adding, updating, and removing key-value pairs.
	 * This class is suitable for scenarios where the map needs to be dynamically changed.
	 * <p>
	 * It inherits and utilizes the two value storage strategies from {@link R}: Compressed (Rank-Based) and Flat (One-to-One),
	 * transitioning between them automatically as the map's density of non-null values increases and the overhead of rank calculations in the Compressed strategy grows.
	 */
	class RW extends R {
		
		/**
		 * Constructs a new {@code RW} map with an initial capacity.
		 * <p>
		 * Initially, the map starts with the **Compressed (Rank-Based) Strategy**, optimized for sparse maps.
		 * The {@link #values} array is initialized with a size that is at least 16 and at most 256, based on the provided {@code length} hint.
		 *
		 * @param length The initial capacity hint for the map. The actual capacity will be at least 16 and at most 256.
		 */
		public RW( int length ) { values = new float[ ( int ) Math.max( Math.min( Array.nextPowerOf2( length ), 0x100 ), 16 ) ]; }
		
		/**
		 * Clears all mappings from this map.
		 * <p>
		 * This operation removes all key-value pairs, including the null key mapping if present, and resets the map to an empty state.
		 * The map reverts to its initial state using the **Compressed (Rank-Based) Strategy** with an empty (or initial size) {@link #values} array, ready to efficiently store sparse data again.
		 *
		 * @return This {@code RW} instance to allow for method chaining.
		 */
		public RW clear() {
			_clear();
			nulls._clear();
			hasNullKey      = false;
			nullKeyHasValue = false;
			return this;
		}
		
		/**
		 * Associates the specified primitive value with the specified boxed Byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * Supports null keys.
		 *
		 * @param key   The boxed {@link Byte} key with which the specified value is to be associated.
		 *              Null is a valid key.
		 * @param value The primitive {@code int} value to be associated with the specified key.
		 * @return {@code true} if the key was newly added to the map, {@code false} if the key already existed.
		 */
		public boolean put(  Byte      key, float value ) {
			if( key != null ) return put( ( byte ) ( key + 0 ), value );
			nullKeyValue    = ( float ) value;
			nullKeyHasValue = true;
			return _add();
		}
		
		/**
		 * Associates the specified boxed primitive value with the specified boxed Byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * Supports null keys and null values.
		 *
		 * @param key   The boxed {@link Byte} key with which the specified value is to be associated.
		 *              Null is a valid key.
		 * @param value The boxed  value to be associated with the specified key.
		 *              Null is a valid value.
		 * @return {@code true} if the key was newly added to the map, {@code false} if the key already existed.
		 */
		public boolean put(  Byte      key,  Float     value ) {
			if( key != null ) return put( ( byte ) ( key + 0 ), value );
			
			if( nullKeyHasValue = value != null ) nullKeyValue = ( float ) ( value + 0 );
			
			return _add();
		}
		
		/**
		 * Associates the specified boxed value with the specified primitive {@code byte} key.
		 * Handles {@code null} values correctly by marking the key as present but having a null value.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The primitive {@code byte} key.
		 * @param value The boxed  value. Can be {@code null}.
		 * @return {@code true} if the key was not present in the map before this call (a new mapping was added),
		 * {@code false} if the key was already present (an existing mapping was updated).
		 */
		public boolean put( byte key,  Float     value ) {
			if( value != null ) return put( key, ( float ) ( value + 0 ) );
			
			if( nulls.set0( ( byte ) key ) && values.length < 256 )
				Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.cardinality + 1, -1 );
			
			
			return set1( ( byte ) key ); // return true if key was added (as a key with null value), false otherwise (key already present, value changed to null)
		}
		
		/**
		 * Associates the specified primitive {@code int} value with the specified primitive {@code byte} key.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * <p>
		 * This method handles the core logic for adding/updating non-null values and manages the
		 * transition from the <b>Compressed (Rank-Based) Strategy</b> to the <b>Flat (One-to-One) Strategy</b>.
		 * The transition occurs if this operation adds the 128th non-null value mapping.
		 *
		 * @param key   The primitive {@code byte} key.
		 * @param value The primitive {@code int} value to associate with the key.
		 * @return {@code true} if the key was not present in the map before this call (a new mapping was added),
		 * {@code false} if the key was already present (an existing mapping was updated).
		 */
		public boolean put( byte key, float value ) {
			
			if( nulls.get_( ( byte ) key ) ) {
				values[ values.length == 256 ?
						key & 0xFF :
						nulls.rank( ( byte ) key ) - 1 ] = ( float ) value; // Set or update the value at the calculated index
				return false;
			}
			
			if( values.length == 256 ) values[ key & 0xFF ] = ( float ) value;
			else if( nulls.cardinality == 128 ) {//switch to flat mode
				float[] values_ = new float[ 256 ];
				for( int token = -1, ii = 0; ( token = unsafe_token( token ) ) != -1; )
					if( hasValue( token ) )
						values_[ token & KEY_MASK ] = values[ ii++ ];
				
				( values = values_ )[ key & 0xFF ] = ( float ) value;
			}
			else {
				int r = nulls.rank( ( byte ) key ); // Get the rank for the key
				
				Array.resize( values,
				              nulls.cardinality < values.length ?
						              values :
						              ( values = new float[ values.length * 2 ] ), r, nulls.cardinality, 1 ); // Resize the 'values' array to accommodate the new value at index 'i'
				values[ r ] = ( float ) value;
			}
			
			nulls.set1( ( byte ) key );
			
			return set1( ( byte ) key );
		}
		
		/**
		 * Removes the mapping for the specified boxed Byte key from this map if present.
		 * <p>
		 * If a mapping exists for the key, it is removed. Null keys are supported.
		 *
		 * @param key The boxed {@link Byte} key whose mapping is to be removed from the map.
		 *            Null is a valid key.
		 * @return {@code true} if a mapping was removed for the key, {@code false} otherwise.
		 */
		public boolean remove(  Byte      key ) {
			if( key != null ) return remove( ( byte ) ( key + 0 ) );
			
			nullKeyHasValue = false;
			return _remove();
		}
		
		
		/**
		 * Removes the mapping for the specified primitive {@code byte} key from this map if present.
		 * This involves removing the key from the underlying {@link ByteSet} and updating the
		 * value storage ({@link #nulls} bitset and potentially the {@link #values} array if in compressed mode).
		 *
		 * @param key The primitive {@code byte} key whose mapping is to be removed.
		 * @return {@code true} if a mapping was removed (the key was present), {@code false} otherwise.
		 */
		public boolean remove( byte key ) {
			if( !set0( ( byte ) key ) ) return false; // Remove key from base ByteSet (handling key presence).
			
			if( nulls.set0( ( byte ) key ) && values.length < 256 )
				Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.cardinality + 1, -1 ); // Resize to remove the value, shifting elements to fill the gap.
			
			return true; // Mapping removed successfully (or was not present initially).
		}
		
		/**
		 * Creates and returns a deep copy of this read-write map.
		 * The clone is also a fully independent mutable {@code RW} instance with the same
		 * current mappings and internal state (including storage strategy) as the original.
		 * Modifications to the clone will not affect the original map, and vice versa.
		 *
		 * @return A deep clone of this {@code ByteIntNullMap.RW} instance.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
	}
}