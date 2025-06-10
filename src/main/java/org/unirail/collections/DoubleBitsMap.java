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
import java.util.NoSuchElementException;

/**
 * Defines a contract for a specialized map designed for efficient storage and retrieval of
 * mappings from 2-byte primitive keys to primitive values (1-7 bits, determined by {@code bits_per_item}).
 * <p>
 * This interface and its implementations employ a unique dual-region storage strategy to optimize
 * memory usage and performance.
 * <p>
 * For a detailed explanation of the internal mechanisms, including the dual-region strategy,
 * insertion, removal, and iteration algorithms, refer to the documentation of the
 * nested abstract class R and its concrete implementation RW.
 */
public interface DoubleBitsMap {
	
	
	/**
	 * An abstract base class for a specialized map designed for mapping 2-byte primitive keys
	 * to primitive values (1-7 bits, determined by {@code bits_per_item}).
	 * <p>
	 * - Uses a dual-region approach for keys/values storage to minimize memory overhead:
	 * - {@code lo Region}: Stores entries involved in hash collisions. Uses an explicit links array for chaining.
	 * Occupies low indices in the keys/values arrays.
	 * - {@code hi Region}: Stores entries that do not (initially) require explicit links (i.e., no collision at insertion time, or they are terminal nodes of a chain).
	 * Occupies high indices in the keys/values arrays.
	 * - Manages collision-involved ({@code lo Region}) and initially non-collision ({@code hi Region}) entries distinctly.
	 * <p>
	 *
	 *
	 * <ul>
	 * <li><b>{@code hi Region}:</b> Occupies high indices in the keys (and values) arrays.
	 *     Entries are added at an index that effectively makes this region grow downwards from the highest available index.
	 *     Active entries are in indices from `keys.length - _hi_Size` to `keys.length - 1`.
	 *     <ul>
	 *         <li>Stores entries that, at the time of insertion, map to an empty bucket in the {@code _buckets} hash table.</li>
	 *         <li>These entries can also become the terminal entries of collision chains originating in the {@code lo Region}.</li>
	 *         <li>Managed stack-like: new entries are added to what becomes the lowest index of this region if it were viewed as growing from the highest available index downwards.</li>
	 *     </ul>
	 * </li>
	 *
	 * <li><b>{@code lo Region}:</b> Occupies low indices in the keys (and values) arrays, indices {@code 0} through {@code _lo_Size - 1}. It grows upwards
	 *     (e.g., from {@code 0} towards higher indices, up to a maximum of {@code _lo_Size} elements).
	 *     <ul>
	 *         <li>Stores entries that are part of a collision chain (i.e., multiple keys hash to the same bucket, or an entry that initially was in {@code hi Region} caused a collision upon a new insertion).</li>
	 *         <li>Uses the links array for managing collision chains. This array is sized dynamically and stores the index of the next element in the chain.</li>
	 *         <li>Only entries in this region can have their links array slot utilized for chaining to another entry in either region.</li>
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
	 *         <li><b>If the index is in the {@code hi Region} (`_lo_Size <= index`):</b>
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
	 *         <li><b>If the index is in the {@code lo Region} (part of an existing collision chain):</b>
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
	 * <li><b>Removing an entry at an index in the {@code hi Region} (`_lo_Size <= removeIndex`):</b>
	 *     <ul>
	 *         <li>If the key at the index matches the target key:</li>
	 *         <li>The bucket that pointed to the index (if any) is cleared (set to 0). Note: If the index is not the bucket head, no direct bucket update is needed.</li>
	 *         <li>To compact the {@code hi Region}, the entry from the lowest-addressed slot in the {@code hi Region}
	 *             is moved into the `removeIndex` slot using the internal `move` operation.</li>
	 *         <li>`_hi_Size` is decremented.</li>
	 *     </ul>
	 * </li>
	 * <li><b>Removing an entry at an index in the {@code lo Region} (`removeIndex < _lo_Size`):</b>
	 *     <ul>
	 *         <li><b>Case 1: The entry is the head of its collision chain (i.e., `_buckets[bucketIndex] - 1 == removeIndex`):</b>
	 *             <ul><li>The bucket `_buckets[bucketIndex]` is updated to point to the next entry in the chain (`links[removeIndex]`).</li></ul></li>
	 *         <li><b>Case 2: The entry is within or at the end of its collision chain (not the head):</b>
	 *             <ul><li>The link of the preceding entry in the chain is updated to bypass the entry to be removed.</li>
	 *                 <li><b>Special Optimization (removing a terminal entry from the {@code hi Region} that is part of a {@code lo Region} chain):</b>
	 *                  If the target key to remove (`T`) is found at an index `T_idx` in the {@code hi Region}, and `T` is the terminal node of a collision chain originating in the {@code lo Region} (meaning a {@code lo Region} entry `P` at index `P_idx` points to `T`):
	 *                      <ul>
	 *                          <li>The data (key/value) from the preceding entry `P` (at `P_idx`) is copied into `T_idx`'s slot in the {@code hi Region}. This effectively "moves" `P`'s entry into the {@code hi Region} and makes it the new terminal node for its chain.</li>
	 *                          <li>The pointer that originally pointed to the preceding entry `P` (which is the main bucket `_buckets[bucketIndex]` for this chain, or a `links` entry) is updated to now
	 *                          point to `T_idx` (which now contains `P`'s data). This effectively makes `P`'s new location (at `T_idx`'s old spot)
	 *                          the new head of the chain (if it was the head) or a node within the chain.</li>
	 *                          <li>The preceding entry's original slot (`P_idx` in the {@code lo Region}) is then marked for compaction by moving the last logical `lo` entry into it.</li>
	 *                      </ul>
	 *                      This strategy efficiently reuses `T_idx`'s slot for `P`'s new logical position, avoiding a separate `hi` region compaction for `T`'s original slot.
	 *                  </li>
	 *             </ul>
	 *         </li>
	 *         <li>After chain adjustments (and potential `lo` to `hi` data copy as described above), the {@code lo Region} is compacted:
	 *             The data from the last logical entry in {@code lo Region} (at index `_lo_Size-1`) is moved into the freed `removeIndex` slot using the internal `move` operation.
	 *             This includes updating the keys, values, and the link of the moved entry.
	 *             Any pointers (bucket or links from other entries) to the moved entry (`_lo_Size-1`) are updated to its new location (`removeIndex`).
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
	 *   <li>2. If the {@code lo Region} scan is exhausted (i.e., {@code token + 1 >= _lo_Size}), iteration continues by scanning the {@code hi Region} from its logical start up to {@code keys.length - 1} (inclusive).</li>
	 * </ul>
	 *
	 * <h3>Resizing (Hash to Hash):</h3>
	 * <ul>
	 * <li>Allocate new internal arrays (buckets, keys, values, links) with the new capacity.</li>
	 * <li>Iterate through all valid entries in the old {@code lo Region} and
	 * old {@code hi Region}.</li>
	 * <li>Re-insert each key-value pair into the new structure using an internal copy operation, which efficiently re-hashes and re-inserts entries into the new structure, rebuilding the hash table and regions.</li>
	 * </ul>
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Indicates whether a special mapping for the null key exists in the map.
		 */
		protected boolean       hasNullKey;
		/**
		 * The value associated with the null key, if {@link #hasNullKey} is true.
		 */
		protected byte          nullKeyValue;
		/**
		 * The hash table buckets. Each entry stores a 1-based index into the internal storage arrays
		 * representing the head of a collision chain, or 0 if the bucket is empty.
		 */
		protected int[]         _buckets;
		/**
		 * Stores links for collision chains. An entry in the {@code lo Region} uses its corresponding links array slot
		 * to point to the next entry in its chain. {@code hi Region} entries do not use their links slots.
		 */
		protected int[]         links;
		/**
		 * Stores the 2-byte primitive keys. Shared by both {@code lo Region} and {@code hi Region}.
		 */
		protected double[] keys;
		/**
		 * Stores the primitive values (1-7 bits). Shared by both {@code lo Region} and {@code hi Region}.
		 * Values are managed by an efficient list structure to optimize memory usage.
		 */
		protected BitsList.RW   values;
		
		
		/**
		 * Returns the total number of non-null key-value mappings currently stored in the map's
		 * {@code lo Region} and {@code hi Region} arrays.
		 *
		 * @return The combined count of entries in the {@code lo Region} and {@code hi Region}.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * A version counter that increments on every structural modification to the map.
		 * Used to detect concurrent modifications during iteration or token-based access.
		 */
		protected int _version;
		
		/**
		 * The current number of entries in the {@code lo Region}.
		 * This region grows from index 0 upwards.
		 */
		protected int _lo_Size;
		/**
		 * The current number of entries in the {@code hi Region}.
		 * This region grows from `keys.length - 1` downwards.
		 */
		protected int _hi_Size;
		
		/**
		 * The bit shift used to encode the map's version into the higher bits of a token.
		 */
		protected static final int VERSION_SHIFT = 32;
		
		/**
		 * A special index value used within a token to represent the null key.
		 * It is chosen to be distinct from any valid array index.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * A special token value indicating that a key was not found or that iteration has ended.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return {@code true} if the map contains no key-value mappings (including the null key), {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the total number of key-value mappings in this map.
		 * This includes the null key if present.
		 *
		 * @return The number of entries in the map.
		 */
		public int size() {
			return _count() + (
					hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the number of bits allocated for each value item in the map.
		 * This value is set during construction of the map.
		 *
		 * @return The number of bits per value item (1-7).
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
		 * This represents the allocated size of the keys array.
		 *
		 * @return The current capacity of the map's internal arrays.
		 */
		public int length() {
			return
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
		 * @see #tokenOf(Integer)
		 */
		public boolean containsKey(  Double    key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 * @see #tokenOf(int)
		 */
		public boolean containsKey( double key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Checks if the map contains the specified primitive value.
		 * This method iterates through all map entries.
		 *
		 * @param value The primitive value to search for.
		 * @return {@code true} if the value exists in the map, {@code false} otherwise.
		 */
		public boolean containsValue( long value ) {
			if( size() == 0 ) return false;
			
			if( hasNullKey && nullKeyValue == value ) return true;
			
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
		 * @return A token for the key, or the invalid token value if the key is not found.
		 */
		public long tokenOf(  Double    key ) {
			return key == null ?
			       ( hasNullKey ?
			         token( NULL_KEY_INDEX ) :
			         INVALID_TOKEN ) :
			       tokenOf( ( double ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a "token" representing the internal location of the specified primitive key.
		 * This token can be used for fast access to the key and its value via {@link #key(long)} and {@link #value(long)}.
		 * It also includes a version stamp to detect concurrent modifications.
		 *
		 * @param key The primitive key to get the token for.
		 * @return A token for the key, or the invalid token value if the key is not found.
		 * @throws ConcurrentModificationException if a concurrent structural modification is detected while traversing
		 *                                         a collision chain, indicating potential data corruption or unexpected behavior.
		 */
		public long tokenOf( double key ) {
			
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			int index = (_buckets[ bucketIndex( Array.hash( key ) ) ] ) - 1;
			if( index < 0 ) return INVALID_TOKEN;
			
			
			for( int collisions = 0; ; ) { // Traverse lo Region collision chain
				if( keys[ index ] == key ) return token( index );
				if( _lo_Size <= index ) return INVALID_TOKEN; // Reached a terminal node in hi Region
				index = links[ index ];
				if( _lo_Size + 1 < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
		}
		
		
		/**
		 * Returns the first valid token for iterating over map entries (excluding the null key initially, it's last).
		 * Call the next token method subsequently to get the next token.
		 * The returned token includes the map's current version for modification detection.
		 *
		 * @return The token for the first entry, or the invalid token value if the map is empty.
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
		 * Example usage: `for (long token = map.firstToken(); token != invalid; token = map.nextToken(token))`
		 * <p>
		 * Iteration order: First, entries in the {@code lo Region} (0 to {@code _lo_Size-1}),
		 * then entries in the {@code hi Region} ({@code keys.length - _hi_Size} to {@code keys.length-1}),
		 * and finally the null key if it exists.
		 *
		 * @param token The current token obtained from a previous call to {@link #token()} or {@link #token(long)}.
		 *              Must not be the invalid token value.
		 * @return The token for the next entry, or the invalid token value if no more entries exist.
		 * @throws IllegalArgumentException        if the input token is the invalid token value.
		 * @throws ConcurrentModificationException if the map has been structurally modified since the token was issued.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN; // Null key was the last iterated
			index = unsafe_token( index ); // Get next internal index
			
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
			if( _count() == 0 ) return -1;
			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size;
			return i < _lo_Size ?
			       // Still in lo Region?
			       i :
			       i < lowest_hi ?
			       // Transition from lo to hi Region
			       _hi_Size == 0 ?
			       // If hi Region is empty, no more non-null keys
			       -1 :
			       lowest_hi :
			       // Start of hi Region
			       i < keys.length ?
			       // Still in hi Region?
			       i :
			       -1; // All non-null keys iterated
		}
		
		
		/**
		 * Checks if the map contains a special mapping for the null key.
		 *
		 * @return {@code true} if a null key mapping exists, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		/**
		 * Returns the primitive value associated with the null key.
		 *
		 * @return The primitive value associated with the null key.
		 * @throws NoSuchElementException if {@link #hasNullKey()} is {@code false}.
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
		 * Returns the key associated with the given token.
		 *
		 * @param token The token obtained from token retrieval methods or iteration methods.
		 *              Must not be the invalid token value or represent the null key.
		 * @return The key value.
		 * @throws IllegalArgumentException        if the token is invalid or represents the null key.
		 * @throws ConcurrentModificationException if the map has been structurally modified since the token was issued.
		 */
		public double key( long token ) { return ( double )  ( keys[ index( token ) ] ); }
		
		
		/**
		 * Returns the value associated with the given token.
		 * The value is returned as a long to accommodate the internal list structure's return type,
		 * even though the map stores values as 1-7 bits.
		 *
		 * @param token The token obtained from token retrieval methods or iteration methods.
		 *              Must not be the invalid token value.
		 * @return The primitive value.
		 * @throws IllegalArgumentException        if the token is invalid.
		 * @throws ConcurrentModificationException if the map has been structurally modified since the token was issued.
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
				int h = Array.mix( seed, Array.hash( keys[ token ] ) );
				h = Array.mix( h, Array.hash( value( token ) ) );
				h = Array.finalizeHash( h, 2 ); // For 2 components (key and value)
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey ) {
				// Hash for the null key-value pair
				int h = Array.hash( seed ); // Use seed for null key's hash
				h = Array.mix( h, Array.hash( nullKeyValue ) );
				h = Array.finalizeHash( h, 2 ); // For 2 components (implicit null key and its value)
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		/**
		 * A seed used for hash code calculations to prevent collisions with other types of objects
		 * and to ensure a unique hash for this specific map implementation.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map with the specified object for equality.
		 * Returns {@code true} if the given object is of the same concrete class, and the two maps
		 * contain the same number of mappings, and each mapping in this map is also present in the
		 * other map (i.e., the same key maps to the same value).
		 *
		 * @param obj The object to be compared for equality with this map.
		 * @return {@code true} if the specified object is equal to this map.
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		
		/**
		 * Compares this map with another R instance for equality.
		 * This method assumes the {@code other} object is already cast to R.
		 *
		 * @param other The other map instance to compare with.
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
		 * All internal arrays are cloned,
		 * ensuring the cloned map is independent of the original.
		 *
		 * @return A cloned instance of this map.
		 * @throws InternalError if cloning fails (should not happen as Cloneable is supported and arrays are clonable).
		 * @see java.lang.Cloneable
		 */
		@Override
		public R clone() {
			try {
				R cloned = ( R ) super.clone();
				
				if( _buckets != null ) cloned._buckets = _buckets.clone();
				if( links != null ) cloned.links = links.clone();
				if( keys != null ) cloned.keys = keys.clone();
				if( values != null ) cloned.values = values.clone();
				
				return cloned;
			} catch( CloneNotSupportedException e ) {
				// This should technically not happen as R implements Cloneable
				throw new InternalError( e );
			}
		}
		
		/**
		 * Returns a string representation of this map in JSON format.
		 *
		 * @return A JSON string representing the map.
		 * @see #toJSON(JsonWriter)
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the content of this map to a JsonWriter in JSON object format.
		 * The null key, if present, is represented as a JSON "null" key.
		 * Primitive keys are represented as their numeric value.
		 *
		 * @param json The JsonWriter to write to.
		 * @see JsonWriter.Source
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 ); // Preallocate space for efficiency
			json.enterObject();
			
			if( hasNullKey ) json.name( "null" ).value( nullKeyValue ); // Represent null key as "null" string
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     json.name( keys[ token ] ).value( values.get( token ) ); // Write non-null key-value pairs
			
			json.exitObject();
		}
		
		/**
		 * Calculates the bucket index for a given hash value within the buckets array.
		 * Uses bitwise AND with 0x7FFF_FFFF to handle potential negative hash values (e.g., from `Integer.hashCode()`)
		 * before applying the modulo operator, ensuring a non-negative result.
		 *
		 * @param hash The hash value of a key.
		 * @return The calculated bucket index (a non-negative integer within the bounds of the buckets array).
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		
		/**
		 * Packs an internal array index and the current map version into a single token.
		 * The version is stored in the higher 32 bits, and the index in the lower 32 bits.
		 *
		 * @param index The 0-based index of the entry in the internal arrays (or {@link #NULL_KEY_INDEX} for the null key).
		 * @return A token representing the entry and its associated version.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index ); }
		
		
		/**
		 * Extracts the 0-based internal array index from a given token.
		 *
		 * @param token The token, typically obtained from token retrieval methods or iteration methods.
		 * @return The 0-based index of the entry, or {@link #NULL_KEY_INDEX} if the token represents the null key.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		
		/**
		 * Extracts the version stamp from a given token.
		 * This version stamp is used to detect structural modifications to the map
		 * since the token was issued.
		 *
		 * @param token The token.
		 * @return The version stamp.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
	}
	
	
	/**
	 * Concrete implementation of a specialized map providing read-write functionalities.
	 * This class manages key-value mappings for 2-byte keys to 1-7 bit values.
	 *
	 * @see IntBitsMap
	 * @see R
	 */
	class RW extends R {
		/**
		 * Constructs an empty map with default initial capacity and a specified number of bits per value item.
		 * The default capacity is chosen to be a small prime number.
		 *
		 * @param bits_per_item The number of bits to allocate for each item's value; must be between 1 and 7 (inclusive).
		 * @throws IllegalArgumentException if {@code bits_per_item} is not within the valid range (1-7).
		 */
		public RW( int bits_per_item ) {
			this( bits_per_item, 0 );
		}
		
		/**
		 * Constructs an empty map with a specified initial capacity and bits per value item.
		 * The provided capacity will be rounded up to the nearest prime number to optimize hash distribution.
		 *
		 * @param bits_per_item The number of bits to allocate for each item's value; must be between 1 and 7 (inclusive).
		 * @param capacity      The initial capacity of the map. It will be rounded up to the nearest prime number.
		 * @throws IllegalArgumentException   if {@code bits_per_item} is not within the valid range (1-7).
		 * @throws NegativeArraySizeException if the provided capacity is negative.
		 */
		public RW( int bits_per_item, int capacity ) { this( bits_per_item, capacity, 0 ); }
		
		/**
		 * Constructs an empty map with a specified initial capacity, bits per value item, and a default value.
		 * The provided capacity will be rounded up to the nearest prime number.
		 * The default value is used when initializing the underlying list structure for value storage.
		 *
		 * @param bits_per_item The number of bits to allocate for each item's value; must be between 1 and 7 (inclusive).
		 * @param capacity      The initial capacity of the map. Rounded up to the nearest prime number.
		 * @param defaultValue  The default value used when initializing the internal value list and for reset operations.
		 * @throws IllegalArgumentException   if {@code bits_per_item} is not within the valid range (1-7).
		 * @throws NegativeArraySizeException if the provided capacity is negative.
		 */
		public RW( int bits_per_item, int capacity, int defaultValue ) {
			if( bits_per_item < 1 || bits_per_item > 7 ) throw new IllegalArgumentException( "bits_per_item must be between 1 and 7." );
			if( capacity < 0 ) throw new NegativeArraySizeException( "capacity cannot be negative." );
			initialize( Array.prime( capacity ) );
			values = new BitsList.RW( bits_per_item, defaultValue, capacity );
		}
		
		
		private int initialize( int capacity ) {
			_version++; // Increment version to invalidate old tokens
			
			_buckets = new int[ capacity ];
			// links array starts smaller, grows on demand for lo Region
			links    = new int[ Math.min( 16, capacity ) ];
			_lo_Size = 0; // Reset lo Region size
			keys     = new double[ capacity ];
			// If values object exists, reinitialize it with new capacity, preserving bits_per_item
			if( values != null ) values = new BitsList.RW( values.bits_per_item, values.default_value, capacity );
			_hi_Size = 0; // Reset hi Region size
			return length();
		}
		
		
		/**
		 * Associates the specified primitive value with the specified boxed key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * This method handles {@code null} boxed keys by delegating to the other put method for null keys.
		 *
		 * @param key   The boxed key with which the specified value is to be associated.
		 * @param value The primitive value to be associated with the specified key.
		 * @return {@code true} if a new key-value pair was added (the key was not previously present), {@code false} if an existing value was updated.
		 */
		public boolean put(  Double    key, long value ) {
			return key == null ?
			       put( value ) :
			       put( ( double ) ( key + 0 ), value );
		}
		
		
		/**
		 * Associates the specified primitive value with the null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The primitive value to be associated with the null key. The value will be truncated to fit the value's bit size.
		 * @return {@code true} if the null key was not previously present, {@code false} if its value was updated.
		 */
		public boolean put( long value ) {
			boolean ret = !hasNullKey; // True if null key is new
			hasNullKey   = true;
			nullKeyValue = ( byte ) ( value & 0x7F ); // Store value as byte, possibly truncated
			_version++; // Increment version due to structural modification
			return ret;
		}
		
		
		/**
		 * Associates the specified primitive value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * <p>
		 * This method implements the detailed Hash Map Insertion Logic described in the base class documentation.
		 * It handles both cases: adding a new entry to the {@code hi Region} (no initial collision)
		 * and adding to the {@code lo Region} (collision management).
		 *
		 * @param key   The primitive key with which the specified value is to be associated.
		 * @param value The primitive value to be associated with the specified key.
		 * @return {@code true} if a new key-value pair was added (the key was not previously present), {@code false} if an existing value was updated.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal,
		 *                                         suggesting external structural modification.
		 * @see R for detailed insertion algorithm
		 */
		public boolean put( double key, long value ) {
			
			if( _buckets == null ) initialize( 7 ); // Initialize with a small prime capacity if first put
			else if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ) ); // Resize if capacity reached
			
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from bucket (or -1 if empty)
			int dst_index;
			
			// Bucket is empty: place new entry in {@code hi Region}
			if( index == -1 ) dst_index = keys.length - 1 - _hi_Size++; // Add to the "bottom" of {@code hi Region} and increment counter
			else {
				for( int next = index, collisions = 0; ; ) {
					if( keys[ next ] == ( double ) key ) {
						values.set1( next, value );// Update value
						_version++; // Increment version as value was modified
						return false;// Key was not new
					}
					if( _lo_Size <= next ) break; // Reached a terminal node in hi Region
					next = links[ next ]; // Move to next in chain
					// Safeguard against excessively long or circular chains (corrupt state)
					if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
				
				// Key is new, and a collision occurred (bucket was not empty). Place new entry in {@code lo Region}.
				if( links.length == ( dst_index = _lo_Size++ ) ) links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index; // Link new entry to the previous head of the chain
			}
			
			keys[ dst_index ] = ( double ) key;
			values.set1( dst_index, value );
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
			_version++;
			return true;
		}
		
		
		/**
		 * Removes the mapping for the specified boxed key from this map if present.
		 * Handles {@code null} boxed keys by delegating to the method for removing the null key.
		 *
		 * @param key The boxed key whose mapping is to be removed.
		 * @return {@code true} if the map contained a mapping for the specified key and it was removed, {@code false} otherwise.
		 */
		public boolean remove(  Double    key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( double ) ( key + 0 ) );
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
		
		
		private void move( int src, int dst ) {
			
			if( src == dst ) return; // No move needed
			
			// Find and update the pointer that points to src
			int bucketIndex = bucketIndex( Array.hash( keys[ src ] ) );
			int index       = _buckets[ bucketIndex ] - 1;
			
			// src is the head of its chain in a bucket
			if( index == src ) _buckets[ bucketIndex ] = ( int ) ( dst + 1 ); // Update bucket to point to dst
			else { // src is somewhere within a collision chain
				// Traverse the chain to find the link pointing to src
				while( links[ index ] != src )
					index = links[ index ];
				links[ index ] = ( int ) dst; // Update the link to bypass src and point to dst
			}
			
			// If the source was in the lo Region, copy its link to the destination's link slot
			// hi Region entries do not have meaningful links in their slots.
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			// Move key and value data
			keys[ dst ] = keys[ src ];
			values.set1( dst, values.get( src ) );
		}
		
		
		/**
		 * Removes the mapping for the specified primitive key from this map if present.
		 * <p>
		 * This method implements the detailed Hash Map Removal Logic described in the base class documentation,
		 * including handling of {@code hi Region} and {@code lo Region} removals, chain adjustments,
		 * and the special optimization for removing terminal {@code hi} nodes from {@code lo} chains.
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return {@code true} if the map contained a mapping for the specified key and it was removed, {@code false} otherwise.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal,
		 *                                         suggesting external structural modification.
		 * @see R for detailed removal algorithm
		 */
		public boolean remove( double key ) {
			
			if( _count() == 0 ) return false;
			int removeBucketIndex = bucketIndex( Array.hash( key ) );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1; // 0-based index from bucket (or -1 if empty)
			if( removeIndex < 0 ) return false; // Key not found in this bucket/chain
			
			if( _lo_Size <= removeIndex ) {// Entry is in {@code hi Region}
				
				if( keys[ removeIndex ] != key ) return false; // Key at index does not match
				
				// Move the last element of the hi-region to the removed spot for compaction
				move( keys.length - _hi_Size, removeIndex );
				_hi_Size--; // Decrement hi-region size
				_buckets[ removeBucketIndex ] = 0; // Clear the bucket, as the removed hi-region entry was its sole occupant
				_version++; // Structural modification
				return true;
			}
			
			int next = links[ removeIndex ];
			if( keys[ removeIndex ] == key ) _buckets[ removeBucketIndex ] = ( int ) ( next + 1 );
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
						_buckets[ removeBucketIndex ] = ( int ) ( removeIndex + 1 );
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
		 * Removes all mappings from this map.
		 * The map will be empty after this call returns.
		 * The internal arrays are reset to their initial state or cleared, but not deallocated,
		 * allowing for reuse without new memory allocations.
		 */
		public void clear() {
			_version++; // Structural modification
			hasNullKey = false; // Clear null key presence
			
			if( _count() == 0 ) return; // Already empty (no non-null keys)
			if( _buckets != null ) Arrays.fill( _buckets, ( int ) 0 ); // Clear all bucket pointers
			_lo_Size = 0; // Reset lo region size
			_hi_Size = 0; // Reset hi region size
			// keys and values arrays are not explicitly cleared as their content will be overwritten on new insertions
		}
		
		
		/**
		 * Ensures that this map can hold at least the specified number of entries without resizing.
		 * If the current capacity is less than the specified capacity, the map is resized to
		 * accommodate the new capacity. The new capacity will be a prime number.
		 * If the map is uninitialized (e.g., after construction with 0 capacity), it will be initialized.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new actual capacity of the map's internal arrays.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length(); // Already has enough capacity
			return _buckets == null ?
			       initialize( Array.prime( capacity ) ) :
			       // Initialize if not already
			       resize( Array.prime( capacity ) ); // Resize to new prime capacity
		}
		
		
		/**
		 * Trims the capacity of the map's internal arrays to reduce memory usage.
		 * The capacity will be reduced to the current size of the map, or a prime number
		 * slightly larger than the size, ensuring sufficient space for future additions.
		 */
		public void trim() { trim( size() ); }
		
		/**
		 * Trims the capacity of the map's internal arrays to a specified minimum capacity.
		 * If the current capacity is greater than the specified capacity, the map is resized.
		 * The actual new capacity will be a prime number at least {@code capacity} and
		 * also at least the current size of the map.
		 *
		 * @param capacity The minimum desired capacity after trimming.
		 */
		public void trim( int capacity ) {
			// Only trim if current length is greater than the target prime capacity
			if( length() <= ( capacity = Array.prime( Math.max( capacity, size() ) ) ) ) return;
			
			resize( capacity ); // Perform the resize operation
		}
		
		
		/**
		 * Resizes the map's internal arrays to a new specified size. This is a structural modification.
		 * All existing entries are re-hashed and re-inserted into the new, larger or smaller, structure.
		 * This method is called internally by {@link #put(int, long)} when the map is full, or by {@link #ensureCapacity(int)}
		 * and {@link #trim(int)}.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resize.
		 */
		private int resize( int newSize ) {
			_version++; // Structural modification, increment version
			
			// Store old array references and sizes before re-initialization
			double[] old_keys    = keys;
			BitsList.RW   old_values  = values;
			int           old_lo_Size = _lo_Size;
			int           old_hi_Size = _hi_Size;
			
			initialize( newSize ); // Re-initialize map with new capacity, resetting counters and buckets
			
			// Copy existing entries from old structure to new structure
			for( int i = 0; i < old_lo_Size; i++ )
			     copy( old_keys[ i ], old_values.get( i ) ); // Re-hash and insert lo-region entries
			
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
			     copy( old_keys[ i ], old_values.get( i ) ); // Re-hash and insert hi-region entries
			
			return length();
		}
		
		
		private void copy( double key, long value ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from bucket (or -1 if empty)
			int dst_index;
			
			if( index == -1 ) // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++;
			else { // Collision: place new entry in {@code lo Region}
				// Resize links array if needed, cap at keys.length
				if( links.length == _lo_Size ) links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) );
				links[ dst_index = _lo_Size++ ] = ( int ) ( index ); // Link to previous head and increment lo_Size
			}
			
			keys[ dst_index ] = key; // Store key
			values.set1( dst_index, value ); // Store value
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 ); // Update bucket to point to new entry
		}
		
		/**
		 * Creates and returns a deep copy of this RW map.
		 * Delegates to the base class's clone method to handle the core cloning logic.
		 *
		 * @return A cloned instance of this map.
		 * @throws InternalError if cloning fails (should not happen as Cloneable is supported).
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
	}
}