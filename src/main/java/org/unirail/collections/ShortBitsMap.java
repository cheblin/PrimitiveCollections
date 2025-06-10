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
 * A specialized map for mapping 2-byte primitive keys to primitive byte values (1-7 bits, determined by {@code bits_per_item}).
 * <p>
 * This implementation uses a HYBRID strategy that automatically adapts based on data density:
 * <p>
 * 1. SPARSE MODE (Hash Map Phase):
 * - Starts as a hash map, optimized for sparse data.
 * - Uses a dual-region approach for keys/values storage to minimize memory overhead:
 * - {@code lo Region}: Stores entries involved in hash collisions. Uses an explicit {@code links} array for chaining.
 * Occupies low indices (0 to {@code _lo_Size-1}) in key and value storage.
 * - {@code hi Region}: Stores entries that do not (initially) require explicit {@code links} (i.e., no collision at insertion time, or they are terminal nodes of a chain).
 * Occupies high indices (from {@code keys.length - _hi_Size} to {@code keys.length-1}) in key and value storage.
 * - Manages collision-involved ({@code lo Region}) and initially non-collision ({@code hi Region}) entries distinctly.
 * <p>
 * 2. DENSE MODE (Flat Array Phase):
 * - Automatically transitions to a direct-mapped flat array when a resize operation targets a capacity beyond
 * {@link RW#flatStrategyThreshold} (typically 0x7FFF = 32,767 entries).
 * - Provides guaranteed O(1) performance with full key range support .
 * <p>
 * This approach balances memory efficiency for sparse maps with optimal performance for dense maps.
 *
 * <h3>Hash Map Phase Optimization:</h3>
 * The hash map phase uses a sophisticated dual-region strategy for storing keys and values to minimize memory usage:
 *
 * <ul>
 * <li><b>{@code hi Region}:</b> Occupies high indices in the key (and value) arrays.
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
 * <li><b>{@code lo Region}:</b> Occupies low indices in the key (and value) arrays, indices {@code 0} through {@code _lo_Size - 1}. It grows upwards
 *     (e.g., from {@code 0} towards higher indices, up to a maximum of {@code _lo_Size} elements).
 *     <ul>
 *         <li>Stores entries that are part of a collision chain (i.e., multiple keys hash to the same bucket, or an entry that initially was in {@code hi Region} caused a collision upon a new insertion).</li>
 *         <li>Uses the {@code links} array for managing collision chains. This array is sized dynamically and stores the index of the next element in the chain.</li>
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
 *                 <li>If the new key matches the key at `keys[index]`, the value at `values[index]` is updated.</li>
 *                 <li>If the new key collides with the key at `keys[index]`:
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
 * <h3>Removal Algorithm (Hash Mode):</h3>
 * Removal involves finding the entry, updating chain links or bucket pointers, and then compacting the
 * respective region (`lo` or `hi`) to maintain memory density.
 *
 * <ul>
 * <li><b>Removing an entry `E` at `removeIndex` in the {@code hi Region} (`_lo_Size <= removeIndex`):</b>
 *     <ul>
 *         <li>If the key at `removeIndex` matches the target key:</li>
 *         <li>The bucket that pointed to `removeIndex` (if any) is cleared (set to 0). Note: If `removeIndex` is not the bucket head, no direct bucket update is needed.</li>
 *         <li>To compact the {@code hi Region}, the entry from the lowest-addressed slot in the {@code hi Region}
 *             (i.e., at `keys[keys.length - _hi_Size]`) is moved into the `removeIndex` slot using {@code move()}.</li>
 *         <li>`_hi_Size` is decremented.</li>
 *     </ul>
 * </li>
 * <li><b>Removing an entry `E` at `removeIndex` in the {@code lo Region} (`removeIndex < _lo_Size`):</b>
 *     <ul>
 *         <li><b>Case 1: `E` is the head of its collision chain (i.e., `_buckets[bucketIndex] - 1 == removeIndex`):</b>
 *             <ul><li>The bucket `_buckets[bucketIndex]` is updated to point to the next entry in the chain (`links[removeIndex]`).</li></ul></li>
 *         <li><b>Case 2: `E` is within or at the end of its collision chain (not the head):</b>
 *             <ul><li>The `links` link of the preceding entry in the chain is updated to bypass `E`.</li>
 *                 <li><b>Special Optimization (removing a terminal `hi` node from a `lo` chain):</b>
 *                  If the target key to remove (`T`) is found at an index `T_idx` in the {@code hi Region}, and `T` is the terminal node of a collision chain originating in the {@code lo Region} (meaning a {@code lo Region} entry `P` at index `P_idx` points to `T`):
 *                      <ul>
 *                          <li>The data (key/value) from `P` (at `P_idx`) is copied into `T_idx`'s slot in the {@code hi Region}. This effectively "moves" `P`'s entry into the {@code hi Region} and makes it the new terminal node for its chain.</li>
 *                          <li>The pointer that originally pointed to `P` (which is the main bucket `_buckets[bucketIndex]` for this chain, or a `links` entry) is updated to now
 *                          point to `T_idx` (which now contains `P`'s data). This effectively makes `P`'s new location (at `T_idx`'s old spot)
 *                          the new head of the chain (if it was the head) or a node within the chain.</li>
 *                          <li>`P`'s original slot (`P_idx` in the {@code lo Region}) is then marked for compaction by moving the last logical `lo` entry into it.</li>
 *                      </ul>
 *                      This strategy efficiently reuses `T_idx`'s slot for `P`'s new logical position, avoiding a separate `hi` region compaction for `T`'s original slot.
 *                  </li>
 *             </ul>
 *         </li>
 *         <li>After chain adjustments (and potential `lo` to `hi` data copy as described above), the {@code lo Region} is compacted:
 *             The data from the last logical entry in {@code lo Region} (at index `_lo_Size-1`) is moved into the freed `removeIndex` slot using {@code move()}.
 *             This includes updating keys, values, and the `links` link of the moved entry.
 *             Any pointers (bucket or `links` links from other entries) to the moved entry (`_lo_Size-1`) are updated to its new location (`removeIndex`).
 *             `_lo_Size` is decremented. This ensures the {@code lo Region} remains dense.
 *         </li>
 *     </ul>
 * </li>
 * </ul>
 *
 * <h3>Iteration ({@code unsafe_token}) (Hash Mode):</h3>
 * Iteration over non-null key entries proceeds by scanning the {@code lo Region} first, then the {@code hi Region}:
 * Due to compaction, all entries within these calculated ranges are valid and can be iterated very fast.
 * <ul>
 *   <li>1. Iterate through indices from {@code token + 1} up to {@code _lo_Size - 1} (inclusive).</li>
 *   <li>2. If the {@code lo Region} scan is exhausted (i.e., {@code token + 1 >= _lo_Size}), iteration continues by scanning the {@code hi Region} from its logical start ({@code keys.length - _hi_Size}) up to {@code keys.length - 1} (inclusive).</li>
 * </ul>
 *
 * <h3>Resizing (Hash to Hash):</h3>
 * <ul>
 * <li>Allocate new internal arrays ({@code _buckets}, keys, values, {@code links}) with the new capacity.</li>
 * <li>Iterate through all valid entries in the old {@code lo Region} (indices {@code 0} to {@code old_lo_Size - 1}) and
 * old {@code hi Region} (indices {@code old_keys.length - old_hi_Size} to {@code old_keys.length - 1}).</li>
 * <li>Re-insert each key-value pair into the new structure using an internal {@code copy} operation, which efficiently re-hashes and re-inserts entries into the new structure, rebuilding the hash table and regions.</li>
 * </ul>
 */
public interface ShortBitsMap {
	
	/**
	 * Provides a read-only view and base functionality for the map.
	 * It encapsulates common fields and read operations used by both read-only and read-write map implementations.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Indicates whether a mapping for the null key exists in this map.
		 */
		protected boolean        hasNullKey;
		/**
		 * The value associated with the null key, if {@link #hasNullKey} is true.
		 */
		protected byte           nullKeyValue;
		/**
		 * The hash table array where each element points to the 1-based index of the head of a collision chain,
		 * or 0 if the bucket is empty.
		 */
		protected char[]         _buckets;
		/**
		 * An array used to form collision chains in the 'lo Region'. Each element at index `i`
		 * stores the index of the next entry in the chain, or an index into the {@code hi Region} for terminal nodes.
		 */
		protected char[]         links;
		/**
		 * The array storing the keys of the map entries.
		 */
		protected short[] keys;
		/**
		 * The underlying storage for the map's values, optimized for compact bit storage.
		 */
		protected BitsList.RW    values;
		
		/**
		 * The count of entries when the map is in dense (flat array) strategy.
		 */
		protected int flat_count;
		
		/**
		 * Returns the current number of non-null key entries stored in the {@code lo Region} and {@code hi Region}.
		 * This is used for the sparse (hash map) strategy.
		 *
		 * @return The total count of non-null key entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * A version counter that is incremented on every structural modification to the map.
		 * Used to detect concurrent modifications during iteration.
		 */
		protected int _version;
		
		/**
		 * The current number of entries in the 'lo Region' (collision-involved entries).
		 */
		protected int _lo_Size;
		/**
		 * The current number of entries in the 'hi Region' (initially non-collision entries).
		 */
		protected int _hi_Size;
		
		/**
		 * The number of bits to shift to extract the version from a combined token.
		 */
		protected static final int VERSION_SHIFT = 32;
		
		/**
		 * A special index used within a token to represent the null key.
		 */
		protected static final int NULL_KEY_INDEX = 0x1_FFFF;
		
		/**
		 * A special token value indicating that a key was not found or no more elements exist during iteration.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Checks if the map is currently operating in the dense (flat array) strategy.
		 * This is determined by whether the {@code nulls} bitset array has been allocated.
		 *
		 * @return {@code true} if in flat strategy, {@code false} if in hash map strategy.
		 */
		protected boolean isFlatStrategy() { return nulls != null; }
		
		/**
		 * In dense (flat array) mode, this bitset tracks the presence of primitive keys.
		 * Each bit corresponds to a key value. A set bit indicates the key is present.
		 */
		protected long[] nulls;
		
		/**
		 * The fixed size of the flat array used in dense mode (65,536 entries, covering the full range of 2-byte keys).
		 */
		protected static final int FLAT_ARRAY_SIZE = 0x10000;
		/**
		 * The size of the {@code nulls} bitset array, calculated as {@code FLAT_ARRAY_SIZE / 64}.
		 */
		protected static final int NULLS_SIZE      = FLAT_ARRAY_SIZE / 64;
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return {@code true} if the map contains no key-value mappings, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the total number of key-value mappings in this map.
		 * This includes the null key if present.
		 *
		 * @return The number of entries in the map.
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
		 * Returns the number of bits allocated per value item in the underlying {@link BitsList}.
		 *
		 * @return The number of bits per value.
		 */
		public int bits_per_value() { return values.bits_per_item; }
		
		/**
		 * Returns the total number of key-value mappings in this map.
		 * This is an alias for {@link #size()}.
		 *
		 * @return The number of entries in the map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the internal storage arrays.
		 * In flat strategy, this is fixed at {@link #FLAT_ARRAY_SIZE}.
		 * In sparse strategy, it's the allocated size of the key array.
		 *
		 * @return The current capacity of the map's internal arrays.
		 */
		public int length() {
			return isFlatStrategy() ?
			       FLAT_ARRAY_SIZE :
			       ( keys == null ?
			         0 :
			         keys.length );
		}
		
		
		/**
		 * Checks if the map contains a mapping for the specified boxed key.
		 * This method handles {@code null} keys.
		 *
		 * @param key The boxed key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean containsKey(  Short     key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean containsKey( short key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Checks if the map contains the specified value.
		 * This operation can be slow (O(N) in hash mode, O(N) in flat mode, where N is map size).
		 *
		 * @param value The value to search for.
		 * @return True if the value exists in the map.
		 */
		public boolean containsValue( long value ) {
			if( size() == 0 ) return false;
			
			if( hasNullKey && nullKeyValue == value ) return true;
			
			if( isFlatStrategy() ) {
				for( int i = -1; ( i = next( i ) ) != -1; )
					if( values.get( i ) == value ) return true;
				return false;
			}
			
			// Iterate lo region
			for( int i = 0; i < _lo_Size; i++ )
				if( values.get( i ) == value ) return true;
			
			// Iterate hi region
			for( int i = keys.length - _hi_Size; i < keys.length; i++ )
				if( values.get( i ) == value ) return true;
			
			return false;
		}
		
		
		/**
		 * Returns a "token" representing the internal location of the specified boxed key.
		 * This token can be used for fast access to the key and its value via {@link #key(long)} and {@link #value(long)}.
		 * It also includes a version stamp to detect concurrent modifications.
		 *
		 * @param key The boxed key to get the token for. Handles {@code null} keys.
		 * @return A {@code long} token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 */
		public long tokenOf(  Short     key ) {
			return key == null ?
			       ( hasNullKey ?
			         token( NULL_KEY_INDEX ) :
			         INVALID_TOKEN ) :
			       tokenOf( ( short ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a "token" representing the internal location of the specified primitive key.
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
			
			if( _count() == 0 ) return INVALID_TOKEN;
			int index = (_buckets[ bucketIndex( Array.hash( key ) ) ] ) - 1;
			if( index < 0 ) return INVALID_TOKEN;
			
			for( int collisions = 0; ; ) {
				if( keys[ index ] == key ) return token( index );
				if( _lo_Size <= index ) return INVALID_TOKEN; //terminal node
				index = links[ index ];
				if( _lo_Size < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
		}
		
		
		/**
		 * Returns the first valid token for iterating over map entries (excluding the null key).
		 * Call {@link #token(long)} subsequently to get the next token.
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
		 * Returns the next valid token for iterating over map entries.
		 * This method is designed to be used in a loop:
		 * {@code for (long token = map.token(); token != INVALID_TOKEN; token = map.token(token))}
		 * <p>
		 * Iteration order: First, entries in the {@code lo Region} (0 to {@code _lo_Size-1}),
		 * then entries in the {@code hi Region} ({@code keys.length - _hi_Size} to {@code keys.length-1}),
		 * and finally the null key if it exists.
		 *
		 * @param token The current token obtained from a previous call to {@link #token()} or {@link #token(long)}.
		 *              Must not be {@link #INVALID_TOKEN}.
		 * @return The token for the next entry, or {@link #INVALID_TOKEN} if no more entries exist.
		 * @throws IllegalArgumentException        if the input token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the map has been structurally modified since the token was issued.
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
		 * Returns the next valid internal array index for iteration over non-null keys.
		 * This method is faster than {@link #token(long)} as it bypasses version checks and is designed for internal use.
		 * <p>
		 * Iteration order:
		 * 1. First, it iterates through entries in the {@code lo Region} (indices from 0 up to {@code _lo_Size - 1}).
		 * 2. If the {@code lo Region} is exhausted, it then proceeds to iterate through entries in the {@code hi Region}
		 * (indices from {@code keys.length - _hi_Size} up to {@code keys.length - 1}).
		 *
		 * @param token The current internal index. Use -1 to begin iteration.
		 * @return The next valid internal array index for a non-null key, or -1 if no more entries exist in the main map arrays.
		 */
		public int unsafe_token( final int token ) {
			if( isFlatStrategy() ) return next( index( token ) );
			if( _count() == 0 ) return -1;
			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size;
			return i < _lo_Size ?
			       i :
			       i < lowest_hi ?
			       _hi_Size == 0 ?
			       -1 :
			       lowest_hi :
			       i < keys.length ?
			       i :
			       -1;
			
		}
		
		
		/**
		 * Checks if the map contains a special mapping for the null key.
		 *
		 * @return {@code true} if a null key mapping exists, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		/**
		 * Returns the value associated with the null key.
		 *
		 * @return The value associated with the null key.
		 * @throws java.util.NoSuchElementException if {@link #hasNullKey()} is {@code false}.
		 */
		public long nullKeyValue() { return nullKeyValue; }
		
		
		/**
		 * Checks if the given token represents the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the primitive key associated with the given token.
		 *
		 * @param token The token obtained from {@link #tokenOf} or iteration methods.
		 *              Must not be {@link #INVALID_TOKEN} or represent the null key.
		 * @return The primitive key.
		 * @throws IllegalArgumentException if the token is invalid or represents the null key.
		 */
		public short key( long token ) {
			return ( short ) (short) (
					isFlatStrategy() ?
					index( token ) :
					keys[ index( token ) ] );
		}
		
		
		/**
		 * Returns the primitive value associated with the given token.
		 *
		 * @param token The token obtained from {@link #tokenOf} or iteration methods.
		 *              Must not be {@link #INVALID_TOKEN}.
		 * @return The primitive value.
		 * @throws IllegalArgumentException if the token is invalid.
		 */
		public byte value( long token ) {
			return
					isKeyNull( token ) ?
					nullKeyValue :
					values.get( index( token ) );
		}
		
		/**
		 * Computes a hash code for this map.
		 * The hash code is calculated based on all key-value pairs, including the null key if present.
		 * The order of iteration for hash code calculation is consistent, ensuring that
		 * two equal maps will have the same hash code.
		 *
		 * @return A hash code for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( isFlatStrategy() ?
				                                     token :
				                                     keys[ token ] ) );
				h = Array.mix( h, Array.hash( value( token ) ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		/**
		 * A seed used for hash code calculations to prevent collisions with other types of objects.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map with the specified object for equality.
		 * Returns {@code true} if the given object has same type, and the two maps
		 * contain the same number of mappings, and each mapping in this map is also present in the
		 * other map (i.e., the same key maps to the same value).
		 *
		 * @param obj The object to be compared for equality with this map.
		 * @return {@code true} if the specified object is equal to this map.
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		
		/**
		 * Compares this map with another {@code R} instance for equality.
		 *
		 * @param other The other map to compare with.
		 * @return {@code true} if this map is equal to the specified map.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null ||
			    hasNullKey != other.hasNullKey ||
			    ( hasNullKey && nullKeyValue != other.nullKeyValue ) || size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || value( token ) != other.value( t ) ) return false;
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
		public R clone() {
			try {
				R cloned = ( R ) super.clone();
				
				
				if( isFlatStrategy() ) {
					if( nulls != null ) cloned.nulls = nulls.clone();
					if( values != null ) cloned.values = values.clone();
				}
				else {
					if( _buckets != null ) cloned._buckets = _buckets.clone();
					if( links != null ) cloned.links = links.clone();
					if( keys != null ) cloned.keys = keys.clone();
					if( values != null ) cloned.values = values.clone();
				}
				return cloned;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e );
			}
		}
		
		/**
		 * Returns a string representation of this map in JSON format.
		 *
		 * @return A JSON string representing the map.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the content of this map to a {@link JsonWriter} in JSON object format.
		 * Null key is represented as "null" key.
		 * Primitive keys are represented as their primitive value.
		 *
		 * @param json The {@code JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			
			if( isFlatStrategy() )
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( token ).value( values.get( token ) );
			else
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( keys[ token ] ).value( values.get( token ) );
			
			json.exitObject();
		}
		
		/**
		 * Calculates the bucket index for a given hash value within the {@code _buckets} array.
		 *
		 * @param hash The hash value of a key.
		 * @return The calculated bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		
		/**
		 * Packs an internal array index and the current map version into a single {@code long} token.
		 * The version is stored in the higher 32 bits, and the index in the lower 32 bits.
		 *
		 * @param index The 0-based index of the entry in the internal arrays (or {@link #NULL_KEY_INDEX} for the null key).
		 * @return A {@code long} token representing the entry.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index ); }
		
		
		/**
		 * Extracts the 0-based internal array index from a given {@code long} token.
		 *
		 * @param token The {@code long} token.
		 * @return The 0-based index.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		
		/**
		 * Extracts the version stamp from a given {@code long} token.
		 *
		 * @param token The {@code long} token.
		 * @return The version stamp.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
		/**
		 * In dense (flat array) mode, checks if a given primitive key exists by checking its
		 * corresponding bit in the {@code nulls} bitset.
		 *
		 * @param key The primitive key to check.
		 * @return {@code true} if the key exists, {@code false} otherwise.
		 */
		protected final boolean exists( char key ) { return ( nulls[ key >>> 6 ] & 1L << key ) != 0; }
		
		
		/**
		 * In dense (flat array) mode, finds the next existing key index in the values storage
		 * after the given starting key index. Uses the {@code nulls} bitset for efficient lookup.
		 *
		 * @param key The starting primitive key index (inclusive). If -1, starts from 0.
		 * @return The next existing key index, or -1 if no more keys are found.
		 */
		protected int next( int key ) { return next( key, nulls ); }
		
		
		/**
		 * Static helper method to find the next existing key index in a bitset.
		 * Used for iterating over keys in flat strategy.
		 *
		 * @param bit   The current bit index (inclusive start point).
		 * @param nulls The bitset array.
		 * @return The next set bit index, or -1 if no more bits are set.
		 */
		protected static int next( int bit, long[] nulls ) {
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
	 * Concrete implementation providing read-write functionalities for the map.
	 * It manages the transitions between sparse (hash map) and dense (flat array) strategies,
	 * and implements complex insertion and removal logic.
	 */
	class RW extends R {
		
		
		/**
		 * The threshold at which the map switches from the sparse (hash map) strategy to the
		 * dense (flat array) strategy. If the target capacity during a resize operation
		 * exceeds this value, the map transitions to flat mode.
		 * Current value: 0x7FFF (32,767 entries).
		 */
		protected static final int flatStrategyThreshold = 0x7FFF;
		
		
		/**
		 * Constructs an empty map with default capacity and a specified number of bits per value item.
		 *
		 * @param bits_per_item The number of bits to allocate for each item's value must be between 1 and 7 (inclusive).
		 */
		public RW( int bits_per_item ) {
			this( bits_per_item, 0 );
		}
		
		/**
		 * Constructs an empty map with a specified initial capacity and bits per value item.
		 *
		 * @param bits_per_item The number of bits to allocate for each item's value must be between 1 and 7 (inclusive).
		 * @param capacity      The initial capacity of the map. It will be rounded up to the nearest prime number.
		 */
		public RW( int bits_per_item, int capacity ) { this( bits_per_item, capacity, 0 ); }
		
		/**
		 * Constructs an empty map with a specified initial capacity, bits per value item, and default value.
		 *
		 * @param bits_per_item The number of bits to allocate for each item's value must be between 1 and 7 (inclusive).
		 * @param capacity      The initial capacity of the map. Rounded up to the nearest prime number.
		 * @param defaultValue  The default value used when initializing the {@link BitsList} and for reset operations.
		 */
		public RW( int bits_per_item, int capacity, int defaultValue ) {
			if( capacity < 0 ) throw new NegativeArraySizeException( "capacity" );
			initialize( Array.prime( capacity ) );
			values = new BitsList.RW( bits_per_item, defaultValue, capacity );
		}
		
		
		/**
		 * Initializes or re-initializes the internal arrays based on the specified capacity
		 * and the current strategy (sparse or dense). This method updates the map's version.
		 * <p>
		 * In sparse mode, it allocates bucket, key, and value arrays to the given `capacity`.
		 * The `links` array is initialized with a smaller capacity (minimum of 16 or `capacity`),
		 * and it will grow dynamically as needed by the {@code lo Region}.
		 *
		 * @param capacity The desired capacity for the new internal arrays.
		 * @return The actual allocated capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			flat_count = 0;
			if( flatStrategyThreshold < capacity ) {
				nulls = new long[ NULLS_SIZE ];
				if( values != null ) values = new BitsList.RW( values.bits_per_item, FLAT_ARRAY_SIZE );
				_buckets = null;
				links    = null;
				keys     = null;
				_hi_Size = 0;
				_lo_Size = 0;
				return FLAT_ARRAY_SIZE;
			}
			nulls    = null;
			_buckets = new char[ capacity ];
			links    = new char[ Math.min( 16, capacity ) ];
			_lo_Size = 0;
			keys     = new short[ capacity ];
			if( values != null ) values = new BitsList.RW( values.bits_per_item, capacity );
			_hi_Size = 0;
			return length();
		}
		
		
		/**
		 * Associates the specified value with the specified boxed key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles {@code null} keys.
		 *
		 * @param key   The boxed key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 * @return {@code true} if a new key-value pair was added (the key was not previously present), {@code false} if an existing value was updated.
		 */
		public boolean put(  Short     key, long value ) {
			return key == null ?
			       put( value ) :
			       put( ( short ) ( key + 0 ), value );
		}
		
		
		/**
		 * Associates the specified value with the null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The value to be associated with the null key.
		 * @return {@code true} if the null key was not previously present, {@code false} if its value was updated.
		 */
		public boolean put( long value ) {
			boolean ret = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = ( byte ) ( value & 0x7F );
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
		 *             The {@code links} link of this new entry points to the *previous* head of the chain (which was
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
		public boolean put( short key, long value ) {
			
			if( isFlatStrategy() ) {
				boolean ret = !exists( ( char ) key );
				if( ret ) {
					exists1( ( char ) key );
					flat_count++;
				}
				values.set1( key, value );
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
			
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 )  // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++; // Add to the "bottom" of {@code hi Region} 
			else {
				for( int i = index, collisions = 0; ; ) {
					if( keys[ i ] == key ) {
						values.set1( i, value );// Update value
						_version++;
						return false;// Key was not new
					}
					if( _lo_Size <= i ) break;
					i = links[ i ];
					
					if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
				
				if( links.length == ( dst_index = _lo_Size++ ) ) links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( char ) index;
			}
			
			keys[ dst_index ] = ( short ) key;
			values.set1( dst_index, value );
			_buckets[ bucketIndex ] = ( char ) ( dst_index + 1 );
			_version++;
			return true;
		}
		
		
		/**
		 * Removes the mapping for the specified boxed key from this map if present.
		 * Handles {@code null} keys.
		 *
		 * @param key The boxed key whose mapping is to be removed.
		 * @return {@code true} if the map contained a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean remove(  Short     key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( short ) ( key + 0 ) );
		}
		
		
		/**
		 * Removes the mapping for the null key from this map if present.
		 *
		 * @return {@code true} if the null key was present and removed, {@code false} otherwise.
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey = false;
			_version++;
			return true;
		}
		
		/**
		 * Relocates an entry's key and value from a source index ({@code src}) to a destination index ({@code dst})
		 * within the internal key and value arrays. This method is crucial for compaction
		 * during removal operations in sparse mode.
		 * <p>
		 * After moving the data, this method updates any existing pointers (either from a hash bucket in
		 * {@code _buckets} or from a {@code links} link in a collision chain) that previously referenced
		 * {@code src} to now correctly reference {@code dst}.
		 * <p>
		 * A critical assumption for this method is that {@code src} and {@code dst} must both belong
		 * to the same region (either both in {@code lo Region} or both in {@code hi Region}).
		 * This constraint simplifies pointer updates, as {@code hi Region} entries do not participate
		 * in {@code links} chains themselves (though they can be terminal nodes of {@code lo Region} chains).
		 *
		 * @param src The index of the entry to be moved (its current location).
		 * @param dst The new index where the entry's data will be placed.
		 */
		private void move( int src, int dst ) {
			
			if( src == dst ) return;
			int bucketIndex = bucketIndex( Array.hash( keys[ src ] ) );
			int index       = _buckets[ bucketIndex ] - 1;
			
			if( index == src ) _buckets[ bucketIndex ] = ( char ) ( dst + 1 );
			else {
				while( links[ index ] != src )
					index = links[ index ];
				
				links[ index ] = ( char ) dst;
			}
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			keys[ dst ] = keys[ src ];
			values.set1( dst, values.get( src ) );
		}
		
		
		/**
		 * Removes the mapping for the specified primitive key from this map if present.
		 * <p>
		 * <h3>Hash Map Removal Logic (Sparse Mode):</h3>
		 * Removal involves finding the entry, updating chain links or bucket pointers, and then
		 * compacting the respective region ({@code lo} or {@code hi}) to maintain memory density.
		 * <ol>
		 * <li><b>Find entry:</b> Locate the entry corresponding to the key by traversing the hash bucket's chain.</li>
		 * <li><b>Handle 'hi Region' removal:</b>
		 *     <ul>
		 *         <li>If the found entry is in the {@code hi Region} and is the head of its bucket (meaning no collision chain),
		 *             the bucket pointer is cleared (set to 0).</li>
		 *         <li>To compact the {@code hi Region}, the entry from the "bottom" of the {@code hi Region}
		 *             (at {@code keys[keys.length - _hi_Size]}) is moved into the freed {@code removeIndex} slot using {@link #move(int, int)}.
		 *             {@code _hi_Size} is then decremented.</li>
		 *     </ul>
		 * </li>
		 * <li><b>Handle 'lo Region' removal (and associated 'hi Region' entries):</b>
		 *     <ul>
		 *         <li><b>Case A: Entry is the head of its chain:</b> The bucket pointer {@code _buckets[removeBucketIndex]}
		 *             is updated to point to the next entry in the chain ({@code links[removeIndex]}).</li>
		 *         <li><b>Case B: Entry is within or at the end of its chain:</b> The {@code links} link of the
		 *             preceding entry in the chain is updated to bypass the removed entry.</li>
		 *         <li><b>Special Optimization (removing a terminal 'hi' node from a 'lo' chain):</b>
		 *             If the key to be removed (`T`) is found at an index `T_idx` in the {@code hi Region}, and `T` is
		 *             a terminal node of a collision chain whose preceding entry `P` is in the {@code lo Region} (i.e., `links[P_idx] == T_idx`):
		 *             <ul>
		 *                 <li>The data (key/value) from `P` (at `P_idx`) is copied into `T_idx`'s slot in the {@code hi Region}.
		 *                     This effectively "moves" `P`'s entry into the {@code hi Region} and makes it the new terminal node for its chain.</li>
		 *                 <li>The main bucket pointer for this chain (which is for `P`) is updated to now point to `T_idx`
		 *                     (which now contains `P`'s data). This makes `P`'s new location the head of the chain.</li>
		 *                 <li>`P`'s original slot (`P_idx` in the {@code lo Region}) is then treated as the index to be
		 *                     compacted, and the last logical `lo` entry is moved into it.</li>
		 *             </ul>
		 *             This avoids a separate {@code hi} region compaction for the removed {@code hi} entry and
		 *             optimizes the movement of the {@code lo} entry into the freed {@code hi} slot.
		 *         </li>
		 *         <li>After chain adjustments (and potential `lo` to `hi` data copy as described above), the {@code lo Region} is compacted:
		 *             The data from the last logical entry in {@code lo Region} (at index {@code _lo_Size-1})
		 *             is moved into the freed {@code removeIndex} slot using {@link #move(int, int)}.
		 *             This includes updating keys, values, and the `links` link of the moved entry.
		 *             Any pointers (bucket or {@code links} links from other entries) to the moved entry's original position
		 *             are updated to its new location. {@code _lo_Size} is decremented.</li>
		 *     </ul>
		 * </li>
		 * </ol>
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return {@code true} if the map contained a mapping for the specified key, {@code false} otherwise.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal.
		 */
		public boolean remove( short key ) {
			
			if( isFlatStrategy() ) {
				
				if( flat_count == 0 || !exists( ( char ) key ) ) return false;
				exists0( ( char ) key );
				flat_count--;
				_version++;
				return true;
			}
			
			if( _count() == 0 ) return false;
			int removeBucketIndex = bucketIndex( Array.hash( key ) );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1;
			if( removeIndex < 0 ) return false;
			
			if( _lo_Size <= removeIndex ) {// Entry is in {@code hi Region}
				
				if( keys[ removeIndex ] != key ) return false;
				
				move( keys.length - _hi_Size, removeIndex );
				_hi_Size--;
				_buckets[ removeBucketIndex ] = 0;
				_version++;
				return true;
			}
			
			char next = links[ removeIndex ];
			if( keys[ removeIndex ] == key ) _buckets[ removeBucketIndex ] = ( char ) ( next + 1 );
			else {
				int last = removeIndex;
				if( keys[ removeIndex = next ] == key )// The key is found at 'SecondNode'
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];// 'SecondNode' is in 'lo Region', relink to bypasses 'SecondNode'
					else {  // 'SecondNode' is in the hi Region (it's a terminal node)
						
						keys[ removeIndex ] = keys[ last ]; //  Copies `keys[last]` to `keys[removeIndex]`
						values.set1( removeIndex, values.get( last ) ); // Copies `values[last]` to `values[removeIndex]`
						
						// Update the bucket for this chain.
						// 'removeBucketIndex' is the hash bucket for the original 'key' (which was keys[T]).
						// Since keys[P] and keys[T] share the same bucket index, this is also the bucket for keys[P].
						// By pointing it to 'removeIndex' (which now contains keys[P]), we make keys[P] the new sole head.
						_buckets[ removeBucketIndex ] = ( char ) ( removeIndex + 1 );
						removeIndex                   = last;
					}
				else if( _lo_Size <= removeIndex ) return false;
				else
					for( int collisions = 0; ; ) {
						int prev = last;
						
						if( keys[ removeIndex = links[ last = removeIndex ] ] == key ) {
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else {
								
								keys[ removeIndex ] = keys[ last ];
								values.set1( removeIndex, values.get( last ) ); // Copies `values[last]` to `values[removeIndex]`
								
								links[ prev ] = ( char ) removeIndex;
								removeIndex   = last;
							}
							break;
						}
						if( _lo_Size <= removeIndex ) return false;
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
			}
			
			move( _lo_Size - 1, removeIndex );
			_lo_Size--;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes all mappings from this map.
		 * The map will be empty after this call returns.
		 * The internal arrays are reset to their initial state or cleared, but not deallocated.
		 */
		public void clear() {
			_version++;
			
			hasNullKey = false;
			
			
			if( isFlatStrategy() ) {
				if( flat_count == 0 ) return;
				flat_count = 0;
				Array.fill( nulls, 0 );
				return;
			}
			
			if( _count() == 0 ) return;
			if( _buckets != null ) Arrays.fill( _buckets, ( char ) 0 );
			_lo_Size = 0;
			_hi_Size = 0;
		}
		
		
		/**
		 * Ensures that this map can hold at least the specified number of entries without resizing.
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
		 * Trims the capacity of the map's internal arrays to reduce memory usage.
		 * The capacity will be reduced to the current size of the map, or a prime number
		 * slightly larger than the size, ensuring sufficient space for future additions.
		 * This method maintains the current strategy (sparse or dense).
		 */
		public void trim() { trim( size() ); }
		
		/**
		 * Trims the capacity of the map's internal arrays to a specified minimum capacity.
		 * If the current capacity is greater than the specified capacity, the map is resized.
		 * The actual new capacity will be a prime number at least {@code capacity} and
		 * at least the current {@link #size()}.
		 *
		 * @param capacity The minimum desired capacity after trimming.
		 */
		public void trim( int capacity ) {
			if( length() <= ( capacity = Array.prime( Math.max( capacity, size() ) ) ) ) return;
			
			resize( capacity );
		}
		
		
		/**
		 * Resizes the map's internal arrays to a new specified size.
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
				if( flatStrategyThreshold < newSize ) return length();
				
				BitsList.RW _values = values;
				long[]      _nulls  = nulls;
				
				initialize( newSize );
				for( int token = -1; ( token = next( token, _nulls ) ) != -1; )
				     copy( ( short ) token, _values.get( token ) );
				
				
				return length();
			}
			
			if( flatStrategyThreshold < newSize ) {
				
				BitsList.RW _values = values;
				
				values = new BitsList.RW( values.bits_per_item, FLAT_ARRAY_SIZE );
				nulls  = new long[ NULLS_SIZE ];
				
				// Iterate old hash entries and set bits in new flat nulls bitset
				for( int i = 0; i < _lo_Size; i++ ) {
					char key = ( char ) keys[ i ];
					exists1( key );
					values.set1( key, _values.get( i ) );
				}
				for( int i = keys.length - _hi_Size; i < keys.length; i++ ) {
					char key = ( char ) keys[ i ];
					exists1( key );
					values.set1( key, _values.get( i ) );
				}
				
				flat_count = _count(); // Total count of non-null keys (lo + hi) before clearing hash arrays
				// Clear hash-specific fields to free memory
				_buckets = null;
				links    = null;
				keys     = null;
				_lo_Size = 0;
				_hi_Size = 0;
				
				return FLAT_ARRAY_SIZE;
			}
			
			
			short[] old_keys    = keys;
			BitsList.RW    old_values  = values;
			int            old_lo_Size = _lo_Size;
			int            old_hi_Size = _hi_Size;
			initialize( newSize );
			
			
			for( int i = 0; i < old_lo_Size; i++ )
			     copy( old_keys[ i ], old_values.get( i ) );
			
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
			     copy( old_keys[ i ], old_values.get( i ) );
			
			return length();
		}
		
		/**
		 * Internal helper method used during resizing in sparse mode to efficiently copy an
		 * existing key-value pair into the new hash table structure. It re-hashes the key
		 * and places it into the correct bucket and region (lo or hi) in the new arrays.
		 * This method does not check for existing keys, assuming all keys are new in the
		 * target structure during a resize operation.
		 *
		 * @param key   The key to copy.
		 * @param value The value to copy.
		 */
		private void copy( short key, long value ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 )
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				if( links.length == _lo_Size ) links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) );
				links[ dst_index = _lo_Size++ ] = ( char ) ( index );
			}
			
			
			keys[ dst_index ] = key;
			values.set1( dst_index, value );
			_buckets[ bucketIndex ] = ( char ) ( dst_index + 1 );
		}
		
		/**
		 * Creates and returns a deep copy of this {@code RW} map.
		 *
		 * @return A cloned instance of this map.
		 * @throws InternalError if cloning fails (should not happen as Cloneable is supported).
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		
		/**
		 * Internal helper method for dense (flat array) strategy: marks a bit in the map's own {@code nulls} bitset
		 * at the position corresponding to the given primitive key, indicating the key's presence.
		 *
		 * @param key The primitive key to mark as present.
		 */
		protected final void exists1( char key ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		/**
		 * Internal helper method for dense (flat array) strategy: clears a bit in the map's own {@code nulls} bitset
		 * at the position corresponding to the given primitive key, indicating the key's absence.
		 *
		 * @param key The primitive key to mark as absent.
		 */
		protected final void exists0( char key ) { nulls[ key >>> 6 ] &= ~( 1L << key ); }
	}
}