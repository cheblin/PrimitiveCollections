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
 * A specialized map for mapping primitive keys to object values.
 * <p>
 * This implementation uses a HYBRID strategy that automatically adapts based on data density:
 * <p>
 * 1. SPARSE MODE (Hash Map Phase):
 * - Starts as a hash map, optimized for sparse data.
 * - Uses a dual-region approach for key-value storage to minimize memory overhead:
 * - {@code lo Region}: Stores entries involved in hash collisions. Uses an explicit {@code links} array for chaining.
 * Occupies low indices (0 to {@code _lo_Size-1}) in {@code keys}/{@code values}.
 * - {@code hi Region}: Stores entries that do not (initially) require collision links (i.e., no collision at insertion time, or they are terminal nodes of a chain).
 * Occupies high indices (from {@code keys.length - _hi_Size} to {@code keys.length-1}) in {@code keys}/{@code values}.
 * - Manages collision-involved ({@code lo Region}) and initially non-collision ({@code hi Region}) entries distinctly.
 * <p>
 * 2. DENSE MODE (Flat Array Phase):
 * - Automatically transitions to a direct-mapped flat array when a resize operation targets a capacity beyond
 * a predefined threshold (e.g., {@link RW#flatStrategyThreshold}).
 * - Provides guaranteed O(1) performance with full key range support.
 * <p>
 * This approach balances memory efficiency for sparse maps with optimal performance for dense maps.
 *
 * <h3>Hash Map Phase Optimization:</h3>
 * The hash map phase uses a sophisticated dual-region strategy for storing keys and values to minimize memory usage:
 *
 * <ul>
 * <li><b>{@code hi Region}:</b> Occupies high indices in the {@code keys} (and {@code values}) array.
 *     Entries are added at {@code keys[keys.length - 1 - _hi_Size]}, and {@code _hi_Size} is incremented.
 *     Thus, this region effectively grows downwards from {@code keys.length-1}. Active entries are in indices
 *     {@code [keys.length - _hi_Size, keys.length - 1]}.
 *     <ul>
 *         <li>Stores entries that, at the time of insertion, map to an empty bucket in the {@code _buckets} hash table.</li>
 *         <li>These entries can also become the terminal entries of collision chains originating in the {@code lo Region}.</li>
 *         <li>Managed stack-like: new entries are added to what becomes the lowest index of this region if it were viewed as growing from {@code keys.length-1} downwards.</li>
 *     </ul>
 * </li>
 *
 * <li><b>{@code lo Region}:</b> Occupies low indices in the {@code keys} (and {@code values}) array, indices {@code 0} through {@code _lo_Size - 1}. It grows upwards
 *     (e.g., from {@code 0} towards higher indices, up to a maximum of {@code _lo_Size} elements).
 *     <ul>
 *         <li>Stores entries that are part of a collision chain (i.e., multiple keys hash to the same bucket, or an entry that initially was in {@code hi Region} caused a collision upon a new insertion).</li>
 *         <li>Uses the {@code links} array for managing collision chains. This array is sized dynamically.</li>
 *         <li>Only entries in this region can have their {@code links} array slot utilized for chaining to another entry in either region.</li>
 *     </ul>
 * </li>
 * </ul>
 *
 * <h3>Insertion Algorithm (Hash Mode):</h3>
 * <ol>
 * <li>Compute the bucket index using {@code bucketIndex(hash)}.</li>
 * <li><b>If the bucket is empty ({@code _buckets[bucketIndex] == 0}):</b>
 *     <ul><li>The new entry is placed in the {@code hi Region} (at index `keys.length - 1 - _hi_Size`, then `_hi_Size` is incremented).
 *      The bucket {@code _buckets[bucketIndex]} is updated to point to this new entry (using 1-based index: `dst_index + 1`).</li></ul></li>
 * <li><b>If the bucket is not empty:</b>
 *     <ul>
 *         <li>Let `index = _buckets[bucketIndex]-1` be the 0-based index of the current head of the chain.</li>
 *         <li><b>If `index` is in the {@code hi Region} (`_lo_Size <= index`):</b>
 *             <ul>
 *                 <li>If the new key matches `keys[index]`, the value at `values[index]` is updated.</li>
 *                 <li>If the new key collides with `keys[index]`:
 *                     <ul>
 *                         <li>The new entry is placed in the {@code lo Region} (at index `n_idx = _lo_Size`, then `_lo_Size` is incremented).</li>
 *                         <li>The `links[n_idx]` field of this new {@code lo Region} entry is set to `index` (making it point to the existing {@code hi Region} entry).</li>
 *                         <li>The bucket {@code _buckets[bucketIndex]} is updated to point to this new {@code lo Region} entry `n_idx` (using 1-based index: `n_idx + 1`), effectively making the new entry the head of the chain.</li>
 *                     </ul>
 *                 </li>
 *             </ul>
 *         </li>
 *         <li><b>If `index` is in the {@code lo Region} (part of an existing collision chain):</b>
 *             <ul>
 *                 <li>The existing collision chain starting from `index` is traversed.</li>
 *                 <li>If the key is found in the chain, its value is updated.</li>
 *                 <li>If the key is not found after traversing the entire chain:
 *                     <ul>
 *                         <li>The new entry is placed in the {@code lo Region} (at index `n_idx = _lo_Size`, then `_lo_Size` is incremented).</li>
 *                         <li>The `links` array is resized if necessary.</li>
 *                         <li>The `links[n_idx]` field of this new {@code lo Region} entry is set to `index` (pointing to the old head).</li>
 *                         <li>The bucket {@code _buckets[bucketIndex]} is updated to point to this new {@code lo Region} entry `n_idx` (using 1-based index: `n_idx + 1`), making the new entry the new head.</li>
 *                     </ul>
 *                 </li>
 *             </ul>
 *         </li>
 *     </ul>
 * </li>
 * </ol>
 *
 * <h3>Removal Algorithm (Hash Mode - Simplified):</h3>
 * Removal involves finding the entry, updating chain links or bucket pointers, and then compacting the
 * respective region (`lo` or `hi`) to maintain memory density.
 *
 * <ul>
 * <li>Locate the entry for the target key.</li>
 * <li>Adjust pointers: If the removed entry was the head of its chain, the bucket pointer is updated. Otherwise,
 *     the {@code links} pointer of its predecessor in the chain is updated to bypass it.</li>
 * <li>Clear the value reference at the removed index.</li>
 * <li>Compact the region:
 *     <ul>
 *         <li>If the entry was in the {@code lo Region} (`removeIndex < _lo_Size`), the last logical entry in the
 *             {@code lo Region} (at index {@code _lo_Size-1}) is moved into the freed slot. {@code _lo_Size} is decremented.</li>
 *         <li>If the entry was in the {@code hi Region} (`_lo_Size <= removeIndex`), the last logical entry in the
 *             {@code hi Region} (at index {@code keys.length - _hi_Size - 1}) is moved into the freed slot.
 *             {@code _hi_Size} is decremented.</li>
 *     </ul>
 * </li>
 * <li>Any bucket or {@code links} references that pointed to the moved entry's *original* position are updated
 *     to point to its *new* position.</li>
 * </ul>
 *
 * <h3>Iteration ({@code unsafe_token}) (Hash Mode):</h3>
 * Iteration over non-null key entries proceeds by scanning the {@code lo Region} first, then the {@code hi Region}:
 * Due to compaction all entries in this ranges are valid and pass very fast.
 * <ul>
 *   <li>1. Iterate through indices from `token + 1` up to `_lo_Size - 1` (inclusive).</li>
 *   <li>2. If the {@code lo Region} scan is exhausted (i.e., `token + 1 >= _lo_Size`), iteration continues by scanning the {@code hi Region} from its logical start (`keys.length - _hi_Size`) up to `keys.length - 1` (inclusive).</li>
 * </ul>
 *
 * <h3>Resizing (Hash to Hash):</h3>
 * <ul>
 * <li>Allocate new internal arrays ({@code _buckets}, {@code keys}, {@code values}, {@code links}) with the new capacity.</li>
 * <li>Iterate through all valid entries in the old {@code lo Region} (indices {@code 0} to {@code old_lo_Size - 1}) and
 * old {@code hi Region} (indices {@code old_keys.length - old_hi_Size} to {@code old_keys.length - 1}).</li>
 * <li>Re-insert each key-value pair into the new structure using an internal {@code copy} operation, which efficiently re-hashes and re-inserts entries into the new structure, rebuilding the hash table and regions.</li>
 * </ul>
 *
 * <h3>Flat Array Phase (Dense Mode):</h3>
 * <ul>
 * <li><b>Trigger:</b> Switches to this mode if a resize operation targets a capacity greater than a predefined threshold (e.g., {@link RW#flatStrategyThreshold}),
 *     or if the initial capacity is set above this threshold.</li>
 * <li><b>Storage:</b> Uses a bitset array for presence tracking and a value array for direct key-to-value mapping.
 *     The key itself serves as the index into the value array.</li>
 * <li><b>Memory:</b> Hash-mode specific arrays like {@code _buckets}, {@code links}, and the dual-region {@code keys} array are discarded (set to null).
 * <li><b>Performance:</b> Guaranteed O(1) access for all operations (put, get, remove) as there are no hash collisions.</li>
 * </ul>
 */
public interface ShortObjectMap {

	/**
	 * Abstract base class providing read-only operations for the map.
	 * Handles the underlying structure which can be either a hash map or a flat array,
	 * determined by the {@link #isFlatStrategy()} flag.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	abstract class R< V > implements Cloneable, JsonWriter.Source {

		protected boolean        hasNullKey;          // Indicates if the map contains a null key
		protected V              nullKeyValue;        // Value for the null key, stored separately.
		protected char[]         _buckets;            // Hash table buckets array (indices to collision chain heads).
		protected char[]         links;               // Links within collision chains for entries in the low region.
		protected short[] keys;                // Array for storing keys.
		protected V[]            values;              // Array for storing values.

		protected int flat_count;          // Number of entries when operating in flat array mode.
		protected int _lo_Size;            // Number of entries in the map's low region (hash mode).
		protected int _hi_Size;            // Number of entries in the map's high region (hash mode).

		protected int _count() { return _lo_Size + _hi_Size; } // Actual entries in hash mode.

		protected int _version;            // Internal version counter for modification detection.

		protected static final int VERSION_SHIFT  = 32; // Number of bits to shift the version component within a token.
		// Special index used in tokens to represent the null key. Outside valid array index ranges.
		protected static final int NULL_KEY_INDEX = 0x1_FFFF; // 65537

		protected static final long INVALID_TOKEN = -1L; // Constant representing an invalid or absent token.

		/**
		 * Strategy for comparing object values and calculating their hash codes.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;

		/**
		 * Indicates if the map is currently operating in the dense (flat array) strategy.
		 *
		 * @return {@code true} if in flat strategy, {@code false} if in hash map strategy.
		 */
		protected boolean isFlatStrategy() { return nulls != null; }

		// --- Flat Array Mode Fields & Constants ---
		/**
		 * Bitset used in flat array mode to track the presence of primitive keys. Null in hash map mode.
		 */
		protected              long[] nulls;
		/**
		 * Fixed size of the value array and the conceptual key space in flat array mode.
		 */
		protected static final int    FLAT_ARRAY_SIZE = 0x10000;
		/**
		 * Size of the {@link #nulls} array.
		 */
		protected static final int    NULLS_SIZE      = FLAT_ARRAY_SIZE / 64; // 1024


		/**
		 * Constructs a new read-only map base with the specified value comparison and hashing strategy.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating hash codes. Must not be null.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}


		/**
		 * Checks if the map is empty (contains no key-value mappings).
		 *
		 * @return True if the map contains no mappings.
		 */
		public boolean isEmpty() { return size() == 0; }

		/**
		 * Returns the total number of key-value mappings in this map.
		 *
		 * @return The total number of mappings.
		 */
		public int size() {
			return (
					       isFlatStrategy() ?
					       flat_count :
					       _count() ) + (
					       hasNullKey ?
					       1 :
					       0 );
		}


		/**
		 * Returns the total number of key-value mappings in this map. Alias for {@link #size()}.
		 */
		public int count() { return size(); }

		/**
		 * Returns the total allocated capacity of the map's internal storage.
		 *
		 * @return The capacity.
		 */
		public int length() {
			return isFlatStrategy() ?
			       FLAT_ARRAY_SIZE :
			       ( keys == null ?
			         // Use keys.length as that's the total allocated size.
			         0 :
			         keys.length );
		}

		/**
		 * Checks if the map contains a mapping for the specified object key.
		 *
		 * @param key The key to check (can be null).
		 * @return True if a mapping for the key exists.
		 */
		public boolean containsKey(  Short     key ) { return tokenOf( key ) != INVALID_TOKEN; }

		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive key.
		 * @return True if a mapping for the key exists.
		 */
		public boolean containsKey( short key ) { return tokenOf( key ) != INVALID_TOKEN; }

		/**
		 * Checks if the map contains one or more mappings to the specified value.
		 * This operation iterates through all entries and can be relatively slow (O(N) where N is the size).
		 *
		 * @param value The value to search for (comparison uses {@code equal_hash_V.equals}). Can be null.
		 * @return True if the value exists in the map.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean containsValue( Object value ) {
			V v;
			try { v = ( V ) value; } catch( Exception e ) { return false; }
			if( hasNullKey && equal_hash_V.equals( nullKeyValue, v ) ) return true;
			if( size() == 0 ) return false;
			if( isFlatStrategy() ) {
				for( int i = -1; ( i = next1( i ) ) != -1; ) {
					if( equal_hash_V.equals( values[ i ], v ) ) return true;
				}
			}
			else { // Hash map strategy: iterate lo region then hi region
				for( int i = 0; i < _lo_Size; i++ )
					if( equal_hash_V.equals( values[ i ], v ) ) return true;

				for( int i = keys.length - _hi_Size; i < keys.length; i++ )
					if( equal_hash_V.equals( values[ i ], v ) ) return true;
			}
			return false;
		}

		/**
		 * Returns a token representing the location of the specified object key.
		 * A token combines the map's version and the entry's index.
		 *
		 * @param key The key (can be null).
		 * @return A long token if the key is found, or {@link #INVALID_TOKEN} (-1L) if not found.
		 */
		public long tokenOf(  Short     key ) {
			return key == null ?
			       ( hasNullKey ?
			         token( NULL_KEY_INDEX ) :
			         INVALID_TOKEN ) :
			       tokenOf( ( short ) ( key + 0 ) );
		}

		/**
		 * Returns a token representing the internal location of the specified primitive key.
		 * This token can be used for fast access to the key and its value via {@link #key(long)} and {@link #value(long)}.
		 * It also includes a version stamp to detect concurrent modifications.
		 *
		 * @param key The primitive key to get the token for.
		 * @return A {@code long} token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if a concurrent structural modification is detected while traversing a collision chain.
		 */
		public long tokenOf( short key ) {
			if( isFlatStrategy() )
				return exists( ( char ) key ) ?
				       token( ( char ) key ) :
				       INVALID_TOKEN;

			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			int index = ( _buckets[ bucketIndex( Array.hash( key ) ) ] ) - 1;
			if( index < 0 ) return INVALID_TOKEN; // Bucket is empty

			if( _lo_Size <= index ) // Current head of chain is in hi-region (must be the key)
				return keys[ index ] == key ?
				       token( index ) :
				       INVALID_TOKEN;

			// Current head of chain is in lo-region, traverse collision chain
			for( int collisions = 0; ; ) {
				if( keys[ index ] == key ) return token( index );
				if( _lo_Size <= index ) break; // Reached terminal node (in hi region), key not found
				index = links[ index ];
				if( _lo_Size < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." ); // Safety break
			}
			return INVALID_TOKEN; // Key not found in chain
		}


		/**
		 * Returns an initial token for iterating through the map's entries.
		 * If the map is empty, {@link #INVALID_TOKEN} is returned.
		 *
		 * @return The first valid token, or {@link #INVALID_TOKEN} if the map is empty.
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
		 * Returns the token for the next entry in the iteration sequence.
		 *
		 * @param token The current token obtained from {@link #token()} or a previous call to this method.
		 * @return The next valid token, or {@link #INVALID_TOKEN} (-1L) if there are no more entries or if a concurrent modification occurred.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );

			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN; // Null key was last
			index = unsafe_token( index ); // Get next non-null key token

			return index == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       // If no more non-null keys, return null key token if present
			       INVALID_TOKEN :
			       token( index ); // Return token for next non-null key

		}


		/**
		 * Returns the next valid internal index for iteration, excluding the null key.
		 * This is a low-level method primarily used internally for iteration logic.
		 *
		 * @param token The current internal index, or -1 to start from the beginning.
		 * @return The next valid internal array index, or -1 if no more entries exist in the main map arrays.
		 */
		public int unsafe_token( final int token ) {
			if( isFlatStrategy() ) return next1( token );
			if( _count() == 0 ) return -1; // No entries in hash map arrays

			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size; // Start of hi region

			// 1. Iterate through lo Region
			if( i < _lo_Size ) return i;

			// 2. If lo Region exhausted, start/continue hi Region
			if( i < lowest_hi ) { // Token was in lo region or before hi region start
				return _hi_Size == 0 ?
				       -1 :
				       // No hi region entries
				       lowest_hi; // Start of hi region
			}

			// 3. Continue hi Region iteration
			if( i < keys.length ) return i;

			return -1; // No more entries
		}

		/**
		 * Checks if the map contains a mapping for the null key.
		 *
		 * @return True if the null key is present.
		 */
		public boolean hasNullKey() { return hasNullKey; }

		/**
		 * Returns the value associated with the null key.
		 *
		 * @return The value mapped to the null key, or {@code null} if the null key is not present in the map.
		 */
		public V nullKeyValue() {
			return hasNullKey ?
			       nullKeyValue :
			       null;
		}


		/**
		 * Checks if the token represents the null key.
		 *
		 * @param token The token to check.
		 * @return True if the token represents the null key.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }

		/**
		 * Retrieves the primitive key associated with a given token.
		 *
		 * @param token The token representing a non-null key-value pair. Must be valid and not for the null key.
		 * @return The primitive key associated with the token.
		 */
		public short key( long token ) {
			return ( short ) (short) (
					isFlatStrategy() ?
					index( token ) :
					keys[ index( token ) ] );
		}


		/**
		 * Retrieves the value associated with a given token.
		 * Handles both regular tokens and the special token for the null key.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The value associated with the token, or {@code null} if the token is invalid or represents the null key which holds a null value.
		 * @throws IndexOutOfBoundsException if the token index is invalid for the current mode/state.
		 */
		public V value( long token ) {
			return
					isKeyNull( token ) ?
					nullKeyValue :
					values[ index( token ) ];
		}

		/**
		 * Retrieves the value associated with the specified object key.
		 * Returns {@code null} if the key is not found or if the key maps to {@code null}.
		 *
		 * @param key The key whose associated value is to be returned (can be null).
		 * @return The associated value, or {@code null} if the key is not found.
		 */
		public V get(  Short     key ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
			       null :
			       value( token );
		}

		/**
		 * Retrieves the value associated with the specified primitive key.
		 * Returns {@code null} if the key is not found or if the key maps to {@code null}.
		 *
		 * @param key The primitive key whose associated value is to be returned.
		 * @return The associated value, or {@code null} if the key is not found.
		 */
		public V get( short key ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
			       null :
			       value( token );
		}

		/**
		 * Retrieves the value associated with the specified object key,
		 * or returns {@code defaultValue} if the key is not found.
		 *
		 * @param key          The key whose associated value is to be returned (can be null).
		 * @param defaultValue The value to return if the key is not found.
		 * @return The associated value, or {@code defaultValue} if the key is not found.
		 */
		public V get(  Short     key, V defaultValue ) {
			long token = tokenOf( key );
			// Must also check version in case entry was removed and re-added at same index
			return ( token == INVALID_TOKEN || version( token ) != _version ) ?
			       defaultValue :
			       value( token );
		}

		/**
		 * Retrieves the value associated with the specified primitive key,
		 * or returns {@code defaultValue} if the key is not found.
		 *
		 * @param key          The primitive key whose associated value is to be returned.
		 * @param defaultValue The value to return if the key is not found.
		 * @return The associated value, or {@code defaultValue} if the key is not found.
		 */
		public V get( short key, V defaultValue ) {
			long token = tokenOf( key );
			// Must also check version in case entry was removed and re-added at same index
			return ( token == INVALID_TOKEN || version( token ) != _version ) ?
			       defaultValue :
			       value( token );
		}


		/**
		 * Computes an order-independent hash code for the map.
		 *
		 * @return The hash code of the map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			// Iterate using unsafe_token as it's safe for hashcode calc since no modifications will occur.
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
				int h = Array.hash( seed ); // Hash for null key representation
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
		 * Compares this map with the specified object for equality.
		 * Returns true if the object is also a R, has the same size,
		 * contains the same key-value mappings, and uses the same value equality strategy implicitly.
		 *
		 * @param obj The object to compare with.
		 * @return True if the objects are equal.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj ); }

		/**
		 * Compares this map with another map instance for equality.
		 * Requires that the other map has the same value type V.
		 *
		 * @param other The other map to compare.
		 * @return True if the maps are equal.
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
		 * Creates and returns a deep copy of this map.
		 * All internal arrays are cloned, ensuring the cloned map is independent of the original.
		 *
		 * @return A cloned instance of this map.
		 * @throws InternalError if cloning fails (should not happen as Cloneable is supported).
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				// Ensure equal_hash_V is copied (it's final, so reference is copied, which is ok)
				if( isFlatStrategy() ) {
					if( nulls != null ) dst.nulls = nulls.clone();
					if( values != null ) dst.values = values.clone();
				}
				else { // Hash mode
					if( _buckets != null ) dst._buckets = _buckets.clone();
					if( links != null ) dst.links = links.clone();
					if( keys != null ) dst.keys = keys.clone();
					if( values != null ) dst.values = values.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e );
			}
		}

		@Override
		public String toString() { return toJSON(); }


		/**
		 * Appends a JSON representation of this map to the given JsonWriter.
		 * Outputs a JSON object where keys are codes (represented as numbers or potentially strings depending on JsonWriter)
		 * and values are the JSON representation of the mapped values.
		 * The null key is represented by the JSON key "null".
		 *
		 * @param json The JsonWriter to append to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 15 ); // Guestimate size increase for object values
			json.enterObject();

			if( hasNullKey ) json.name().value( nullKeyValue );

			if( isFlatStrategy() )
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( token ).value( values[ token ] );
			else
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( keys[ token ] ).value( values[ token ] );


			json.exitObject();
		}

		// --- Helper methods ---

		/**
		 * Calculates the bucket index for a given hash code in hash map mode.
		 * Ensures a non-negative index within the bounds of the {@code _buckets} array.
		 *
		 * @param hash The hash code of the key.
		 * @return The bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }

		/**
		 * Creates a token combining the current map version and an entry index.
		 *
		 * @param index The index of the entry (or {@link #NULL_KEY_INDEX} for the null key).
		 * @return The combined token.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index ); }

		/**
		 * Extracts the index component from a token.
		 *
		 * @param token The token.
		 * @return The index encoded in the token.
		 */
		protected int index( long token ) { return ( int ) ( token ); }

		/**
		 * Extracts the version component from a token.
		 *
		 * @param token The token.
		 * @return The map version encoded in the token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }


		/**
		 * Checks if a primitive key is present in flat array mode.
		 *
		 * @param key The primitive key.
		 * @return True if the bit corresponding to the key is set.
		 */
		protected final boolean exists( char key ) { return ( nulls[ key >>> 6 ] & 1L << key ) != 0; }


		/**
		 * Finds the index of the next set bit representing an existing key in flat array mode.
		 *
		 * @param bit The bit index (inclusive) to start searching from (0 to 65535).
		 * @return The index of the next set bit, or -1 if no set bit is found at or after {@code fromBitIndex}.
		 */
		protected int next1( int bit ) { return next1( bit, nulls ); }

		protected static int next1( int bit, long[] nulls ) {

			if( 0xFFFF < ++bit ) return -1;
			int  index = bit >>> 6;
			long value = nulls[ index ] & -1L << ( bit & 63 );

			while( value == 0 )
				if( ++index == NULLS_SIZE ) return -1;
				else value = nulls[ index ];

			return ( index << 6 ) + Long.numberOfTrailingZeros( value );
		}

	}

	/**
	 * Provides read-write functionalities for the map, including management of sparse and dense storage strategies.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	class RW< V > extends R< V > {
		/**
		 * The threshold capacity at which the map switches from the sparse (hash map) strategy to the
		 * dense (flat array) strategy. If the target capacity during a resize operation
		 * exceeds this value, the map transitions to flat mode.
		 */
		protected static final int flatStrategyThreshold = 0x7FFF; // ~32k

		/**
		 * Constructs an empty RW with a default initial capacity,
		 * using the default equality/hash strategy for V.
		 *
		 * @param clazzV Class object for value type V, used for array creation.
		 */
		public RW( Class< V > clazzV ) { this( Array.get( clazzV ), 0 ); }

		/**
		 * Constructs an empty RW with the specified initial capacity.
		 * If capacity exceeds the threshold, starts in flat mode.
		 * Uses the default equality/hash strategy for V.
		 *
		 * @param clazzV   Class object for value type V, used for array creation.
		 * @param capacity The initial capacity of the map.
		 */
		public RW( Class< V > clazzV, int capacity ) { this( Array.get( clazzV ), capacity ); }

		/**
		 * Constructs an empty RW with a default initial capacity,
		 * using the specified equality/hash strategy for V.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating hash codes.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V ) { this( equal_hash_V, 0 ); }

		/**
		 * Constructs an empty RW with specified initial capacity.
		 * If capacity exceeds the threshold, starts in flat mode.
		 * Uses the specified equality/hash strategy for V.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating hash codes.
		 * @param capacity     The initial capacity hint.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int capacity ) {
			super( equal_hash_V );
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}


		/**
		 * Initializes or re-initializes the internal arrays based on the specified capacity
		 * and the current storage strategy. This method updates the map's version.
		 *
		 * @param capacity The desired capacity for the new internal arrays.
		 * @return The actual allocated capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			flat_count = 0; // Reset flat count for new initialization

			if( flatStrategyThreshold < capacity ) {
				nulls    = new long[ NULLS_SIZE ];
				values   = equal_hash_V.copyOf( null, FLAT_ARRAY_SIZE );
				_buckets = null;
				links    = null;
				keys     = null;
				_hi_Size = 0;
				_lo_Size = 0;
				return FLAT_ARRAY_SIZE;
			}
			// Hash map strategy
			nulls    = null;
			_buckets = new char[ capacity ];
			links    = new char[ Math.min( 16, capacity ) ]; // links array grows as needed for lo_Size
			keys     = new short[ capacity ];
			values   = equal_hash_V.copyOf( null, capacity );
			_lo_Size = 0;
			_hi_Size = 0;
			return length();
		}


		/**
		 * Associates the specified value with the specified object key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles {@code null} keys.
		 *
		 * @param key   The object key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 * @return {@code true} if a new key-value pair was added (the key was not previously present), {@code false} if an existing value was updated.
		 */
		public boolean put(  Short     key, V value ) {
			return key == null ?
			       putNullKey( value ) :
			       put( ( short ) ( key + 0 ), value );
		}


		/**
		 * Associates the specified value with the null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The value to be associated with the null key.
		 * @return {@code true} if the null key was not previously present, {@code false} if its value was updated.
		 */
		public boolean putNullKey( V value ) {
			boolean ret = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = value;
			_version++;
			return ret;
		}


		/**
		 * Associates the specified value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * <p>
		 * <h3>Hash Map Insertion Logic (Sparse Mode):</h3>
		 * <ol>
		 * <li><b>Check for existing key:</b> If the key already exists, its value is updated and {@code false} is returned.
		 *     This involves traversing the collision chain if one exists.</li>
		 * <li><b>Determine insertion point:</b> If the key is new:
		 *     <ul>
		 *         <li>If the target bucket is empty ({@code _buckets[bucketIndex] == 0}), the new entry is placed in the
		 *             {@code hi Region} (at {@code keys.length - 1 - _hi_Size++}).</li>
		 *         <li>If the target bucket is not empty (a collision occurs), the new entry is placed in the
		 *             {@code lo Region} (at {@code _lo_Size++}). The `links` array is resized if necessary.
		 *             The `links` link of this new entry points to the *previous* head of the chain (which was
		 *             pointed to by {@code _buckets[bucketIndex]-1}), effectively making the new entry the new head of the chain.</li>
		 *     </ul>
		 * </li>
		 * <li><b>Update structures:</b> The new key and value are stored. The {@code _buckets} array is updated
		 *     to point to the new entry's index (1-based). The map's version is incremented.</li>
		 * </ol>
		 *
		 * @param key   The primitive key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 * @return {@code true} if a new key-value pair was added (the key was not previously present), {@code false} if an existing value was updated.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal.
		 */
		public boolean put( short key, V value ) {

			if( isFlatStrategy() ) {
				boolean ret = !exists( ( char ) key );
				if( ret ) {
					exists1( ( char ) key );
					flat_count++;
				}
				values[ ( char ) key ] = value;
				_version++;
				return ret;
			}

			if( _buckets == null ) initialize( 7 );
			else if( _count() == keys.length ) {
				int i = Array.prime( keys.length * 2 );
				if( flatStrategyThreshold < i && keys.length < flatStrategyThreshold ) i = flatStrategyThreshold;

				resize( i );
				if( isFlatStrategy() ) return put( key, value );
			}

			int hash        = Array.hash( key );
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;

			if( index == -1 )  // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++; // Add to the "bottom" of {@code hi Region}
			else {
				// Bucket is not empty, 'index' points to an existing entry
				if( _lo_Size <= index ) { // Entry is in {@code hi Region}
					if( keys[ index ] == ( short ) key ) { // Key matches existing {@code hi Region} entry
						values[ index ] = value; // Update value
						_version++;
						return false; // Key was not new
					}
				}
				else // Entry is in {@code lo Region} (collision chain)
					for( int next = index, collisions = 0; ; ) {
						if( keys[ next ] == key ) {
							values[ next ] = value;// Update value
							_version++;
							return false;// Key was not new
						}
						if( _lo_Size <= next ) break;
						next = links[ next ];

						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}

				if( links.length == ( dst_index = _lo_Size++ ) )
					links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );

				links[ dst_index ] = ( char ) index;
			}

			keys[ dst_index ]       = ( short ) key;
			values[ dst_index ]     = value;
			_buckets[ bucketIndex ] = ( char ) ( dst_index + 1 );
			_version++;
			return true;
		}

		/**
		 * Removes the mapping for the specified object key from this map if present.
		 * Handles {@code null} keys.
		 *
		 * @param key The object key whose mapping is to be removed.
		 * @return The previous value associated with the key, or null if none.
		 */
		public V remove(  Short     key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( short ) ( key + 0 ) );
		}

		/**
		 * Removes the mapping for the null key from this map if present.
		 *
		 * @return The value associated with the null key if it was present and removed, or null otherwise.
		 */
		private V removeNullKey() {
			if( !hasNullKey ) return null;
			V oldValue = nullKeyValue;
			hasNullKey   = false;
			nullKeyValue = null; // Clear the value reference
			_version++;
			return oldValue;
		}

		/**
		 * Relocates an entry's key and value within the internal arrays.
		 * This method is crucial for compaction during removal operations in sparse mode.
		 * After moving the data, this method updates any existing pointers (either from a hash bucket in
		 * {@code _buckets} or from a {@code links} link in a collision chain) that previously referenced
		 * {@code src} to now correctly reference {@code dst}.
		 *
		 * @param src The index of the entry to be moved (its current location).
		 * @param dst The new index where the entry's data will be placed.
		 */
		private void move( int src, int dst ) {
			if( src == dst ) return;
			int bucketIndex = bucketIndex( Array.hash( keys[ src ] ) );
			int index       = _buckets[ bucketIndex ] - 1;

			if( index == src ) _buckets[ bucketIndex ] = ( char ) ( dst + 1);
			else {
				while( links[ index ] != src )
					index = links[ index ];

				links[ index ] = ( char ) dst;
			}
			if( src < _lo_Size ) links[ dst ] = links[ src ];

			keys[ dst ]   = keys[ src ];
			values[ dst ] = values[ src ];
		}


		/**
		 * Removes the mapping for the specified primitive key from this map if present.
		 * <p>
		 * <h3>Hash Map Removal Logic (Sparse Mode - Simplified):</h3>
		 * Removal involves finding the entry, updating chain links or bucket pointers, and then
		 * compacting the respective region ({@code lo} or {@code hi}) to maintain memory density.
		 * <ol>
		 * <li><b>Find entry:</b> Locate the entry corresponding to the key by traversing the hash bucket's chain.</li>
		 * <li><b>Adjust Pointers:</b> Update the bucket pointer (if the removed entry was the chain head) or
		 *     the {@code links} pointer of the preceding entry in the chain to bypass the removed entry.</li>
		 * <li><b>Clear Value:</b> Set the {@code values} array slot to {@code null} for the removed entry.</li>
		 * <li><b>Compaction:</b>
		 *     <ul>
		 *         <li>If the removed entry was in the {@code lo Region}, the last logical entry in the
		 *             {@code lo Region} is moved into the freed slot using {@link #move(int, int)}.
		 *             {@code _lo_Size} is decremented.</li>
		 *         <li>If the removed entry was in the {@code hi Region}, the last logical entry in the
		 *             {@code hi Region} is moved into the freed slot using {@link #move(int, int)}.
		 *             {@code _hi_Size} is decremented.</li>
		 *     </ul>
		 * </li>
		 * </ol>
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return The previous value associated with the key if found and removed, or {@code null} otherwise.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal.
		 */
		public V remove( short key ) {

			if( isFlatStrategy() ) {

				if( flat_count == 0 || !exists( ( char ) key ) ) return null;
				V oldValue = values[ key ];
				values[ key ] = null; // Clear value reference
				exists0( ( char ) key );
				flat_count--;
				_version++;
				return oldValue;
			}

			if( _count() == 0 ) return null;
			int removeBucketIndex = bucketIndex( Array.hash( key ) );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1;
			if( removeIndex < 0 ) return null;

			if( _lo_Size <= removeIndex ) {// Entry is in {@code hi Region}

				if( keys[ removeIndex ] != key ) return null;
				V oldValue = values[ removeIndex ];
				move( keys.length - _hi_Size, removeIndex );
				_hi_Size--;
				_buckets[ removeBucketIndex ] = 0;
				_version++;
				return oldValue;
			}

			V    oldValue;
			char next = links[ removeIndex ];
			if( keys[ removeIndex ] == key ) {
				_buckets[ removeBucketIndex ] = ( char ) ( next + 1 );
				oldValue                      = values[ removeIndex ];
			}
			else {
				int last = removeIndex;
				if( keys[ removeIndex = next ] == key )// The key is found at 'SecondNode'
				{
					oldValue = values[ removeIndex ];
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];// 'SecondNode' is in 'lo Region', relink to bypasses 'SecondNode'
					else {  // 'SecondNode' is in the hi Region (it's a terminal node)

						keys[ removeIndex ]   = keys[ last ]; //  Copies `keys[last]` to `keys[removeIndex]`
						values[ removeIndex ] = values[ last ]; // Copies `values[last]` to `values[removeIndex]`

						// Update the bucket for this chain.
						// 'removeBucketIndex' is the hash bucket for the original 'key' (which was keys[T]).
						// Since keys[P] and keys[T] share the same bucket index, this is also the bucket for keys[P].
						// By pointing it to 'removeIndex' (which now contains keys[P]), we make keys[P] the new sole head.
						_buckets[ removeBucketIndex ] = ( char ) ( removeIndex + 1 );
						removeIndex                   = last;
					}
				}
				else if( _lo_Size <= removeIndex ) return null;
				else
					for( int collisions = 0; ; ) {
						int prev = last;
						if( keys[ removeIndex = links[ last = removeIndex ] ] == key ) {
							{
								oldValue = values[ removeIndex ];
								if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
								else {

									keys[ removeIndex ]   = keys[ last ];
									values[ removeIndex ] = values[ last ];
									links[ prev ]         = ( char ) removeIndex;
									removeIndex           = last;
								}
								break;
							}
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
		 * Removes all key-value mappings from this map.
		 * The map will be empty after this call returns.
		 * The internal arrays are reset to their initial state or cleared, but not deallocated.
		 */
		public void clear() {
			_version++;

			hasNullKey   = false;
			nullKeyValue = null; // Clear null key value reference

			if( isFlatStrategy() ) {
				if( flat_count == 0 ) return; // Use flat_count
				flat_count = 0; // Use flat_count
				Array.fill( nulls, 0 );
				// Values array is not cleared explicitly, will be overwritten on new puts
				return;
			}

			// Hash map strategy
			if( _count() == 0 ) return; // Use _count()
			if( _buckets != null ) Arrays.fill( _buckets, ( char ) 0 );
			// links, keys, values arrays are not filled/cleared for performance,
			// they will be overwritten or compacted.
			_lo_Size = 0;
			_hi_Size = 0;
		}

		/**
		 * Ensures that this map can hold at least the specified number of entries without excessive resizing.
		 * If the current capacity is less than the specified capacity, the map is resized.
		 * If in sparse mode and the map is uninitialized, it will be initialized.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new capacity of the map's internal arrays.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			return !isFlatStrategy() && _buckets == null ?
			       initialize( capacity ) :
			       resize( Array.prime( capacity ) );
		}

		/**
		 * Trims the capacity of the map's internal storage to reduce memory usage.
		 * The capacity will be reduced to the current size of the map, or a prime number
		 * slightly larger than the size, ensuring sufficient space for future additions.
		 * This method maintains the current strategy (sparse or dense).
		 */
		public void trim() { trim( size() ); }

		/**
		 * Trims the capacity of the map's internal storage to a specified minimum capacity.
		 * If the current capacity is greater than the specified capacity, the map is resized.
		 * The actual new capacity will be a prime number at least {@code capacity} and
		 * at least the current {@link #size()}.
		 *
		 * @param capacity The minimum desired capacity after trimming.
		 */
		public void trim( int capacity ) {
			// Ensure requested capacity is at least the current number of elements
			capacity = Array.prime( Math.max( capacity, size() - ( hasNullKey ?
			                                                       1 :
			                                                       0 ) ) );

			// If current length is already optimal or smaller than desired capacity, do nothing
			if( length() <= capacity ) return;

			resize( capacity );
		}

		/**
		 * Resizes the map's internal storage to a new specified size.
		 * This method handles transitions between sparse and dense strategies based on {@code newSize}
		 * relative to {@link #flatStrategyThreshold}. When resizing in sparse mode, all existing
		 * entries are rehashed and re-inserted into the new, larger structure. When transitioning
		 * to flat mode, data is copied directly.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resize.
		 */
		private int resize( int newSize ) {
			newSize = Math.min( newSize, FLAT_ARRAY_SIZE );
			_version++;

			if( isFlatStrategy() ) {
				// Current strategy is Flat.
				if( newSize > flatStrategyThreshold ) return length(); // Still in Flat mode, no change to length.

				// Transition from Flat to Hash
				V[]    _old_values = values;
				long[] _old_nulls  = nulls;

				// Initialize as hash map (this clears current arrays and sets _lo_Size, _hi_Size to 0)
				initialize( newSize );

				// Re-insert existing flat entries into the new hash map
				for( int token = -1; ( token = next1( token, _old_nulls ) ) != -1; )
				     copy( ( short ) token, _old_values[ token ] );

				return length(); // Return new hash map length
			}

			// Current strategy is Hash.
			if( flatStrategyThreshold < newSize ) {
				// Transition from Hash to Flat
				V[]    _old_values  = values; // Old values array (hash mode)
				short [] _old_keys    = keys;   // Old keys array (hash mode)
				int    _old_lo_Size = _lo_Size;
				int    _old_hi_Size = _hi_Size;

				// Initialize as flat array (this clears current arrays and sets up nulls, values for flat mode)
				initialize( newSize );

				// Copy entries from old hash structure to new flat structure
				for( int i = 0; i < _old_lo_Size; i++ ) {
					char key = (char)_old_keys[ i ];
					exists1( key, nulls );
					values[ key ] = _old_values[ i ];
				}
				for( int i = _old_keys.length - _old_hi_Size; i < _old_keys.length; i++ ) {
					char key = (char)_old_keys[ i ];
					exists1( key, nulls );
					values[ key ] = _old_values[ i ];
				}

				flat_count = _old_lo_Size + _old_hi_Size; // Set flat_count based on total entries copied

				return FLAT_ARRAY_SIZE; // Return fixed flat array size
			}

			// Current strategy is Hash, new strategy is also Hash (resize hash map to new capacity)
			short[] old_keys    = keys;
			V[]            old_values  = values;
			int            old_lo_Size = _lo_Size;
			int            old_hi_Size = _hi_Size;

			// Initialize new hash map arrays (this resets _lo_Size, _hi_Size to 0)
			initialize( newSize );

			// Re-insert entries from old hash structure into new hash structure
			for( int i = 0; i < old_lo_Size; i++ )
			     copy( old_keys[ i ], old_values[ i ] );

			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
			     copy( old_keys[ i ], old_values[ i ] );

			return length(); // Return new hash map length
		}

		/**
		 * Internal helper method used during resizing in sparse mode to efficiently copy an
		 * existing key-value pair. It re-hashes the key and places it into the correct bucket and region (lo or hi)
		 * in the new arrays. This method does not check for existing keys, assuming all keys are new in the
		 * target structure during a resize operation.
		 *
		 * @param key   The key to copy.
		 * @param value The value to copy.
		 */
		private void copy( short key, V value ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1; // Current head in the NEW _buckets
			int dst_index;

			if( index == -1 ) { // Bucket is empty in the new map: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++;
			}
			else {
				// Collision in the new map: place new entry in {@code lo Region}
				// Ensure links array has enough capacity for the new lo_Size entry
				if( links.length == _lo_Size ) {
					links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) );
				}
				links[ dst_index = _lo_Size++ ] = ( char ) ( index ); // New entry links to the old head
			}

			keys[ dst_index ]       = key;
			values[ dst_index ]     = value;
			_buckets[ bucketIndex ] = ( char ) ( dst_index + 1 ); // New entry becomes the head
		}


		@Override
		@SuppressWarnings( "unchecked" )
		public RW< V > clone() { return ( RW< V > ) super.clone(); }

		/**
		 * Internal helper method for flat array mode: marks a primitive key as present in the map's bitset.
		 *
		 * @param key The primitive key to mark as present.
		 */
		protected final void exists1( char key ) { nulls[ key >>> 6 ] |= 1L << key; }

		/**
		 * Internal helper method for flat array mode: marks a primitive key as present in a provided bitset.
		 * Used during transitions.
		 *
		 * @param key   The primitive key to mark as present.
		 * @param nulls The {@code long[]} bitset array to modify.
		 */
		protected static void exists1( char key, long[] nulls ) { nulls[ key >>> 6 ] |= 1L << key; }

		/**
		 * Internal helper method for flat array mode: marks a primitive key as absent in the map's bitset.
		 *
		 * @param key The primitive key to mark as absent.
		 */
		protected final void exists0( char key ) { nulls[ key >>> 6 ] &= ~( 1L << key ); }

		/**
		 * Static instance for obtaining the `EqualHashOf` strategy for this map type.
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );

	}

	/**
	 * Returns the `EqualHashOf` implementation for this map type.
	 * This is typically used for nesting maps.
	 *
	 * @param <V> The type of values in the map.
	 * @return The {@link Array.EqualHashOf} instance for {@code RW<V>}.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() { return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; }
}