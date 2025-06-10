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
 * A specialized map for mapping primitive keys to primitive values.
 * <p>
 * This implementation employs a HYBRID storage strategy that combines two distinct regions
 * within the same underlying `keys` and `values` arrays to optimize memory usage and collision handling:
 *
 * <h3>Dual-Region Storage Strategy:</h3>
 * <ul>
 * <li><b>{@code lo Region}:</b> Occupies low indices (`0` to `_lo_Size - 1`) in the `keys` and `values` arrays.
 *     It stores entries that are part of collision chains.
 *     Entries in this region use the `links` array to link to other entries in the same chain.
 *     This region grows upwards from index 0. On removal, it is compacted to maintain density.</li>
 *
 * <li><b>{@code hi Region}:</b> Occupies high indices (from `keys.length - _hi_Size` to `keys.length - 1`) in the `keys` and `values` arrays.
 *     Entries are added from the top-end of the array downwards (i.e., new entries take the lowest available index in this region, such as `keys.length - 1 - old_hi_Size`).
 *     This region primarily stores entries that, at the time of insertion, mapped to an empty bucket
 *     and thus initially formed a "chain" of one. They can also serve as terminal nodes for
 *     collision chains originating in the {@code lo Region}. On removal, it is compacted to maintain density.</li>
 * </ul>
 *
 * <h4>Insertion Algorithm (`put`):</h4>
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
 * <h4>Removal Algorithm (`remove`):</h4>
 * Removal involves finding the entry, updating chain links or bucket pointers, and then compacting the
 * respective region (`lo` or `hi`) to maintain memory density.
 *
 * <ul>
 * <li><b>Removing an entry `E` at `removeIndex` in the {@code hi Region} (`_lo_Size <= removeIndex`):</b>
 *     <ul>
 *         <li>The key at `removeIndex` must match the target key.</li>
 *         <li>The bucket that pointed to `removeIndex` is cleared (set to 0).</li>
 *         <li>To compact the {@code hi Region}, the entry from the lowest-addressed slot in the {@code hi Region}
 *             (i.e., at `keys[keys.length - _hi_Size]`) is moved into the `removeIndex` slot using {@code move()}.</li>
 *         <li>`_hi_Size` is decremented.</li>
 *     </ul>
 * </li>
 *  <li><b>Removing an entry `E` at `removeIndex` in the {@code lo Region} (`removeIndex < _lo_Size`):</b>
 *     <ul>
 *         <li><b>Case 1: `E` is the head of its collision chain (i.e., `_buckets[bucketIndex] - 1 == removeIndex`):</b>
 *             <ul><li>The bucket `_buckets[bucketIndex]` is updated to point to the next entry in the chain (`links[removeIndex]`).</li></ul></li>
 *         <li><b>Case 2: `E` is within or at the end of its collision chain (not the head):</b>
 *             <ul><li>The `links` link of the preceding entry in the chain is updated to bypass `E`.</li>
 *                 <li><b>Special Sub-Case (Optimized for removing a terminal `hi` node from a `lo` chain):</b>
 *                  If the target key to remove `E` is found at an index `T` in the {@code hi Region}, and `T` is the terminal node of a collision chain originating in the {@code lo Region} (meaning a {@code lo Region} entry `P` at index `P_idx` points to `T`):
 *                      <ul>
 *                          <li>The data (key/value) from `P` (at `P_idx`) is copied into `T`'s slot in the {@code hi Region}. This effectively "moves" `P`'s entry into the {@code hi Region} and makes it the new terminal node for its chain.</li>
 *                          <li>The pointer that originally pointed to `P` (which is the main bucket `_buckets[bucketIndex]` for this chain) is updated to now
 *                          point to `T`'s slot (which now contains `P`'s data). This effectively makes `P`'s new location (at `T`'s old spot)
 *                          the new head of the chain.</li>
 *                          <li>`P`'s original slot (`P_idx` in the {@code lo Region}) is then marked for compaction by moving the last logical `lo` entry into it.</li>
 *                      </ul>
 *                      This strategy efficiently reuses `T`'s slot for `P`'s new logical position, avoiding a separate `hi` region compaction for `T`'s original slot.
 *                  </li>
 *             </ul>
 *         </li>
 *         <li>After chain adjustments (and potential `lo` to `hi` data copy as described above), the {@code lo Region} is compacted:
 *             The data from the last logical entry in {@code lo Region} (at index `_lo_Size-1`) is moved into the freed `removeIndex` slot using {@code move()}.
 *             This includes updating `keys`, `values`, and the `links` link of the moved entry.
 *             Any pointers (bucket or `links` links from other entries) to the moved entry (`_lo_Size-1`) are updated to its new location (`removeIndex`).
 *             `_lo_Size` is decremented. This ensures the {@code lo Region} remains dense.
 *         </li>
 *     </ul>
 * </li>
 * </ul>
 *
 * <h4>Iteration (via {@code unsafe_token}):</h4>
 * Iteration over non-null key entries proceeds by scanning the {@code lo Region} first, then the {@code hi Region}:
 * Due to compaction all entries in this ranges are valid and pass very fast.
 * <ul>
 *   <li>1. Iterate through indices from `token + 1` up to `_lo_Size - 1` (inclusive).</li>
 *   <li>2. If the {@code lo Region} scan is exhausted (i.e., `token + 1 >= _lo_Size`), iteration continues by scanning the {@code hi Region} from its logical start (`keys.length - _hi_Size`) up to `keys.length - 1` (inclusive).</li>
 * </ul>
 *
 * <h4>Resizing (`resize`):</h4>
 * When the map capacity needs to change (e.g., due to reaching load factor limits):
 * <ul>
 * <li>New internal arrays (`_buckets`, `keys`, `values`, `links`) are allocated with the new capacity.</li>
 * <li>All existing valid entries from the old {@code lo Region} (indices `0` to `old_lo_Size - 1`) and
 *     old {@code hi Region} (indices `old_keys.length - old_hi_Size` to `old_keys.length - 1`) are
 *     re-inserted into the new structure using a simplified `copy` operation. This rebuilds the
 *     hash table and reorganizes entries into the new `lo` and `hi` regions based on their re-calculated
 *     bucket indices.</li>
 * </ul>
 */
public interface DoubleShortMap {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Indicates whether the map contains a mapping for the null key.
		 */
		protected boolean           hasNullKey;
		/**
		 * The value associated with the null key, if {@code hasNullKey} is {@code true}.
		 */
		protected short       nullKeyValue;
		/**
		 * The array holding the head pointers for each hash bucket.
		 * Values are 1-based indices into the `keys` and `values` arrays.
		 * A value of 0 indicates an empty bucket.
		 */
		protected int[]             _buckets;
		/**
		 * The array used to link entries in collision chains within the {@code lo Region}.
		 * Each element `links[i]` stores the index of the next entry in the chain,
		 * or an index >= `_lo_Size` if it points to a terminal node in the {@code hi Region}.
		 */
		protected int[]             links;
		/**
		 * The array storing the keys of the map entries.
		 * Keys for {@code lo Region} entries are at indices `0` to `_lo_Size - 1`.
		 * Keys for {@code hi Region} entries are at indices `keys.length - _hi_Size` to `keys.length - 1`.
		 */
		protected double[]     keys;
		/**
		 * The array storing the values of the map entries.
		 * Values correspond to keys at the same index in the `keys` array.
		 */
		protected short[]     values;
		
		
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * The current version of the map, incremented on structural modifications.
		 * Used to detect concurrent modifications during iteration.
		 */
		protected int _version;
		
		/**
		 * The current number of entries stored in the {@code lo Region} of the map.
		 * This region stores entries involved in collision chains.
		 */
		protected int _lo_Size;
		/**
		 * The current number of entries stored in the {@code hi Region} of the map.
		 * This region primarily stores entries that are heads of chains of length one,
		 * or terminal nodes of chains.
		 */
		protected int _hi_Size;
		
		/**
		 * The bit shift used to encode the map's version into a token.
		 */
		protected static final int VERSION_SHIFT = 32;
		
		/**
		 * A special index used within a token to represent the null key mapping.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * A special token value indicating that a key was not found or no more elements exist.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Returns {@code true} if this map contains no key-value mappings.
		 *
		 * @return {@code true} if this map contains no key-value mappings
		 */
		public boolean isEmpty() { return size() == 0; }
		
		
		/**
		 * Returns the number of key-value mappings in this map.
		 * If this map contains more than {@code Integer.MAX_VALUE} elements, returns {@code Integer.MAX_VALUE}.
		 *
		 * @return the number of key-value mappings in this map
		 */
		public int size() {
			return
					_count() + (
							hasNullKey ?
							1 :
							0 );
		}
		
		/**
		 * Returns the number of key-value mappings in this map, including the null key mapping if present.
		 * This is equivalent to {@link #size()}.
		 *
		 * @return the number of key-value mappings in this map
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the total capacity of the underlying arrays for non-null keys.
		 * This indicates the maximum number of non-null key-value pairs that can be stored
		 * before a resize operation becomes necessary.
		 *
		 * @return the current capacity of the internal arrays
		 */
		public int length() {
			return
					( keys == null ?
					  0 :
					  keys.length );
		}
		
		
		/**
		 * Returns {@code true} if this map contains a mapping for the specified key.
		 * This method handles both null and non-null keys.
		 *
		 * @param key The key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key.
		 */
		public boolean containsKey(  Double    key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Returns {@code true} if this map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key.
		 */
		public boolean containsKey( double key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Returns {@code true} if this map maps one or more keys to the specified value.
		 *
		 * @param value The value whose presence in this map is to be tested.
		 * @return {@code true} if this map maps one or more keys to the specified value.
		 */
		public boolean containsValue( short value ) {
			
			if( size() == 0 ) return false;
			
			if( hasNullKey && nullKeyValue == value ) return true;
			
			for( int i = 0; i < _lo_Size; i++ )
				if( values[ i ] == value ) return true;
			
			for( int i = keys.length - _hi_Size; i < keys.length; i++ )
				if( values[ i ] == value ) return true;
			
			return false;
		}
		
		
		/**
		 * Returns a token representing the mapping for the specified boxed key, or {@code INVALID_TOKEN}
		 * if this map contains no mapping for the key. The token can be used to retrieve
		 * the key or value efficiently.
		 *
		 * @param key The key to search for.
		 * @return A token for the mapping, or {@code INVALID_TOKEN} if not found.
		 */
		public long tokenOf(  Double    key ) {
			return key == null ?
			       ( hasNullKey ?
			         token( NULL_KEY_INDEX ) :
			         INVALID_TOKEN ) :
			       tokenOf( ( double ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a token representing the mapping for the specified primitive key, or {@code INVALID_TOKEN}
		 * if this map contains no mapping for the key. The token can be used to retrieve
		 * the key or value efficiently.
		 *
		 * @param key The primitive key to search for.
		 * @return A token for the mapping, or {@code INVALID_TOKEN} if not found.
		 */
		public long tokenOf( double key ) {
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
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
		 * Returns the token for the first non-null key-value mapping in this map according to
		 * the internal iteration order. If only the null key is present, a token for the null key is returned.
		 * If the map is empty, {@code INVALID_TOKEN} is returned.
		 *
		 * @return The token for the first entry, or {@code INVALID_TOKEN}.
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
		 * Returns the token for the next non-null key-value mapping after the given token
		 * according to the internal iteration order. If the given token corresponds to the null key,
		 * it returns the token for the first non-null key.
		 * If there are no more non-null entries, it returns a token for the null key if present,
		 * otherwise {@code INVALID_TOKEN}.
		 *
		 * @param token The current token. Must not be {@code INVALID_TOKEN}.
		 * @return The token for the next entry, or {@code INVALID_TOKEN} if no more entries.
		 * @throws IllegalArgumentException        If the provided token is {@code INVALID_TOKEN}.
		 * @throws ConcurrentModificationException If the map has been structurally modified since the token was acquired.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN;
			
			return ( index = unsafe_token( index ) ) == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       INVALID_TOKEN :
			       token( index );
		}
		
		
		/**
		 * Returns the internal array index for the next non-null key-value mapping after the given index
		 * according to the internal iteration order. This method does not perform version checks
		 * and is intended for internal use or highly optimized iteration where version consistency is managed externally.
		 * <p>
		 * Iteration proceeds by first scanning the {@code lo Region} (from `token + 1` up to `_lo_Size - 1`),
		 * then the {@code hi Region} (from `keys.length - _hi_Size` up to `keys.length - 1`).
		 *
		 * @param token The current internal index (or -1 to start from the beginning).
		 * @return The internal index of the next entry, or -1 if no more non-null entries.
		 */
		public int unsafe_token( final int token ) {
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
		 * Returns {@code true} if this map contains a mapping for the {@code null} key.
		 *
		 * @return {@code true} if the null key is present.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		/**
		 * Returns the value to which the {@code null} key is mapped.
		 * It is recommended to call {@link #hasNullKey()} first to ensure the null key is present.
		 *
		 * @return The value associated with the null key.
		 */
		public short nullKeyValue() { return (short) nullKeyValue; }
		
		
		/**
		 * Checks if the given token represents the mapping for the {@code null} key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key mapping.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		
		/**
		 * Returns the primitive key associated with the given token.
		 * It is recommended to check {@link #isKeyNull(long)} before calling this method,
		 * as calling it with a token for the null key will result in an {@code ArrayIndexOutOfBoundsException}.
		 *
		 * @param token The token representing a non-null key-value mapping.
		 * @return The primitive key.
		 */
		public double key( long token ) { return ( double )  ( keys[ index( token ) ] ); }
		
		
		/**
		 * Returns the primitive value associated with the given token.
		 *
		 * @param token The token representing a key-value mapping (can be for the null key).
		 * @return The primitive value.
		 */
		public short value( long token ) {
			return (short)(
					isKeyNull( token ) ?
					nullKeyValue :
					values[ index( token ) ] );
		}
		
		
		@Override
		/**
		 * Computes a hash code for this map.
		 * The hash code is derived from the hash codes of all key-value pairs in the map.
		 *
		 * @return A hash code for this map.
		 */
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( keys[ token ] ) );
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
		 * A seed value used for hash code calculations.
		 */
		private static final int seed = R.class.hashCode();
		
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		
		/**
		 * Compares the specified object with this map for equality. Returns {@code true} if the given object is an instance of this map type
		 * and the two maps represent the same key-value mappings.
		 *
		 * @param other The map to be compared for equality with this map.
		 * @return {@code true} if the specified object is equal to this map.
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
		 * Creates and returns a shallow copy of this map. The internal arrays (`_buckets`, `links`, `keys`, `values`) are cloned,
		 * but the primitive elements within those arrays are copied by value.
		 *
		 * @return A shallow copy of this map.
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
				throw new InternalError( e );
			}
		}
		
		/**
		 * Returns a string representation of this map. This method delegates to {@link #toJSON()}
		 * for a JSON-formatted output.
		 *
		 * @return a string representation of this map
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the JSON representation of this map to the given {@link JsonWriter}.
		 * Keys are output as JSON names (or 'null' for the null key) and values as their corresponding JSON values.
		 *
		 * @param json The JsonWriter to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     json.name( keys[ token ] ).value( values[ token ] );
			
			json.exitObject();
		}
		
		/**
		 * Computes the bucket index for a given hash value.
		 * The hash value is masked to be non-negative before applying the modulo operation.
		 *
		 * @param hash The hash value of a key.
		 * @return The computed bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an internal array index. The token encodes the map's current version
		 * and the index.
		 *
		 * @param index The internal array index.
		 * @return A long token combining version and index.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index ); }
		
		/**
		 * Extracts the internal array index from a token.
		 *
		 * @param token The long token.
		 * @return The internal array index.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token The long token.
		 * @return The version encoded in the token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
	}
	
	/**
	 * A concrete mutable implementation of {@link IntIntMap.R} that allows adding,
	 * removing, and updating key-value mappings for primitive keys and values.
	 * This class manages the internal array resizing and collision handling logic.
	 */
	class RW extends R {
		
		/**
		 * Constructs an empty {@code IntIntMap.RW} instance with a default initial capacity (7).
		 */
		public RW() { this( 0 ); }
		
		/**
		 * Constructs an empty map instance with the specified initial capacity.
		 * The actual capacity will be the smallest prime number greater than or equal to the requested capacity.
		 *
		 * @param capacity The initial capacity.
		 */
		public RW( int capacity ) { if( capacity > 0 ) initialize( Array.prime( capacity ) ); }
		
		
		/**
		 * Initializes or re-initializes the internal arrays of the map with a given capacity.
		 * This method increments the map's version, effectively invalidating all existing tokens.
		 *
		 * @param capacity The desired capacity for the new arrays.
		 * @return The actual length of the initialized key/value arrays.
		 */
		private int initialize( int capacity ) {
			_version++;
			
			_buckets = new int[ capacity ];
			links    = new int[ Math.min( 16, capacity ) ];
			_lo_Size = 0;
			keys     = new double[ capacity ];
			values   = new short[ capacity ];
			_hi_Size = 0;
			return length();
		}
		
		
		/**
		 * Associates the specified value with the specified boxed key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * If the key is {@code null}, the null key mapping is updated or added.
		 *
		 * @param key   The key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 * @return {@code true} if a new key was added, {@code false} if an existing key's value was updated.
		 */
		public boolean put(  Double    key, short value ) {
			return key == null ?
			       put( value ) :
			       put( ( double ) ( key + 0 ), value );
		}
		
		
		/**
		 * Associates the specified value with the {@code null} key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The value to be associated with the null key.
		 * @return {@code true} if the null key mapping was added, {@code false} if its value was updated.
		 */
		public boolean put( short value ) {
			boolean ret = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = ( short ) value;
			_version++;
			return ret;
		}
		
		
		/**
		 * Associates the specified value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 * @return {@code true} if a new key was added, {@code false} if an existing key's value was updated.
		 */
		public boolean put( double key, short value ) {
			if( _buckets == null ) initialize( 7 );
			else if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ) );
			
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 )  // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++; // Add to the "bottom" of {@code hi Region}
			else {
				for( int i = index, collisions = 0; ; ) {
					if( keys[ i ] == ( double ) key ) {
						values[ i ] = ( short ) value;// Update value
						_version++;
						return false;// Key was not new
					}
					if( _lo_Size <= i ) break;
					i = links[ i ];
					
					if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
				
				if( links.length == ( dst_index = _lo_Size++ ) ) links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index;
			}
			
			keys[ dst_index ]       = ( double ) key;
			values[ dst_index ]     = ( short ) value;
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
			_version++;
			return true;
		}
		
		
		/**
		 * Removes the mapping for the specified boxed key from this map if present.
		 * If the key is {@code null}, the null key mapping is removed.
		 *
		 * @param key The key whose mapping is to be removed from the map.
		 * @return {@code true} if the map contained a mapping for the specified key.
		 */
		public boolean remove(  Double    key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( double ) ( key + 0 ) );
		}
		
		
		/**
		 * Removes the mapping for the {@code null} key from this map if present.
		 *
		 * @return {@code true} if the null key mapping was present and removed.
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey = false;
			_version++;
			return true;
		}
		
		/**
		 * Relocates an entry's key and value from a source index (`src`) to a destination index (`dst`)
		 * within the internal `keys` and `values` arrays. This method is crucial for compaction
		 * during removal operations.
		 * <p>
		 * This method also updates any existing pointers (either from a hash bucket in `_buckets`
		 * or from a `links` link in a collision chain) that previously referenced `src` to now
		 * correctly reference `dst`.
		 * <p>
		 * A critical assumption for this method is that {@code src} and {@code dst} must both belong
		 * to the same region (either both in {@code lo Region} or both in {@code hi Region}).
		 * (e.g., moving the last `lo_Region` entry to a vacated `lo_Region` slot, or the lowest `hi_Region` entry to a vacated `hi_Region` slot).
		 * It correctly handles updating `links` links for {@code lo Region} entries. The `remove` method handles more complex
		 * cross-region re-alignments by strategically copying data before calling this `move` method for compaction.
		 *
		 * @param src The index of the entry to be moved (its current location).
		 * @param dst The new index where the entry's data will be placed.
		 */
		private void move( int src, int dst ) {
			
			if( src == dst ) return;
			int bucketIndex = bucketIndex( Array.hash( keys[ src ] ) );
			int index       = _buckets[ bucketIndex ] - 1;
			
			if( index == src ) _buckets[ bucketIndex ] = ( int ) ( dst + 1 );
			else {
				while( links[ index ] != src )
					index = links[ index ];
				
				links[ index ] = ( int ) dst;
			}
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			keys[ dst ]   = keys[ src ];
			values[ dst ] = values[ src ];
		}
		
		
		/**
		 * Removes the mapping for the specified primitive key from this map if present.
		 *
		 * @param key The key whose mapping is to be removed from the map.
		 * @return {@code true} if the map contained a mapping for the specified key.
		 */
		public boolean remove( double key ) {
			if( _count() == 0 ) return false;
			int removeBucketIndex = bucketIndex( Array.hash( key ) );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1;
			if( removeIndex < 0 ) return false;
			
			if( _lo_Size <= removeIndex ) {// Entry is in {@code hi Region}
				
				if( keys[ removeIndex ] != key ) return false;
				
				// Compacting the hi Region by moving the last hi entry to the vacated slot
				// and clearing the bucket that pointed to the removed entry.
				move( keys.length - _hi_Size, removeIndex );
				_hi_Size--;
				_buckets[ removeBucketIndex ] = 0; // Clear the bucket
				_version++;
				return true;
			}
			
			// Entry is in {@code lo Region} (collision chain)
			int next = links[ removeIndex ];
			if( keys[ removeIndex ] == key ) _buckets[ removeBucketIndex ] = ( int ) ( next + 1 );
			else {
				int last = removeIndex;
				if( keys[ removeIndex = next ] == key )// The key is found at 'SecondNode'
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];// 'SecondNode' is in 'lo Region', relink to bypasses 'SecondNode'
					else {
						// 'SecondNode' is in the hi Region (it's a terminal node)
						keys[ removeIndex ]   = keys[ last ]; //  Copies `keys[last]` to `keys[removeIndex]`
						values[ removeIndex ] = values[ last ]; // Copies `values[last]` to `values[removeIndex]`
						
						// Update the bucket for this chain.
						// 'removeBucketIndex' is the hash bucket for the original 'key'.
						// Since keys[last] and the original key (keys[removeIndex]) shared the same bucket,
						// this bucket is also relevant for keys[last].
						// By pointing it to 'removeIndex' (which now contains keys[last]'s data),
						// we effectively make keys[last] (now at removeIndex) the new sole head of the chain from the bucket.
						_buckets[ removeBucketIndex ] = ( int ) ( removeIndex + 1 );
						// Now 'last' (the original lo-region slot) is the one to be compacted.
						removeIndex = last;
					}
				else if( _lo_Size <= removeIndex ) return false;
				else
					for( int collisions = 0; ; ) {
						int prev = last;
						
						if( keys[ removeIndex = links[ last = removeIndex ] ] == key ) {
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else {
								keys[ removeIndex ]   = keys[ last ];
								values[ removeIndex ] = values[ last ];
								links[ prev ]         = ( int ) removeIndex;
								removeIndex           = last;
							}
							break;
						}
						if( _lo_Size <= removeIndex ) return false;
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
			}
			
			// After chain adjustments, compact the lo Region by moving the last logical lo entry
			// into the freed 'removeIndex' slot.
			move( _lo_Size - 1, removeIndex );
			_lo_Size--;
			_version++;
			return true;
		}
		
		/**
		 * Removes all of the mappings from this map.
		 * The map will be empty after this call returns.
		 */
		public void clear() {
			_version++;
			
			hasNullKey = false;
			
			if( _count() == 0 ) return;
			if( _buckets != null ) Arrays.fill( _buckets, ( int ) 0 );
			_lo_Size = 0;
			_hi_Size = 0;
		}
		
		
		/**
		 * Ensures that this map has enough capacity to store at least the specified number of entries
		 * without performing a resize operation. If the current capacity is less than the requested capacity,
		 * the map's internal arrays are resized.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new actual capacity of the internal arrays.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			return _buckets == null ?
			       initialize( capacity ) :
			       resize( Array.prime( capacity ) );
		}
		
		
		/**
		 * Reduces the capacity of the map to its current size, or to a minimum recommended capacity
		 * if the map is small. This helps reclaim unused memory.
		 */
		public void trim() { trim( size() ); }
		
		
		/**
		 * Reduces the capacity of the map to the specified capacity, if the current capacity is larger.
		 * The new capacity will be the smallest prime number greater than or equal to the specified capacity,
		 * and also large enough to hold all current entries.
		 *
		 * @param capacity The target capacity.
		 */
		public void trim( int capacity ) {
			if( length() <= ( capacity = Array.prime( Math.max( capacity, size() ) ) ) ) return;
			
			resize( capacity );
		}
		
		
		/**
		 * Resizes the internal arrays of the map to a new, specified size.
		 * All existing entries are re-hashed and re-inserted into the new structure.
		 * This method increments the map's version.
		 *
		 * @param newSize The new desired capacity for the internal arrays.
		 * @return The actual length of the new key/value arrays.
		 */
		private int resize( int newSize ) {
			_version++;
			
			double[] old_keys    = keys;
			short[] old_values  = values;
			int           old_lo_Size = _lo_Size;
			int           old_hi_Size = _hi_Size;
			initialize( newSize );
			
			
			for( int i = 0; i < old_lo_Size; i++ )
			     copy( old_keys[ i ], old_values[ i ] );
			
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
			     copy( old_keys[ i ], old_values[ i ] );
			
			return length();
		}
		
		/**
		 * Helper method to copy a key-value pair into the new map structure during resize.
		 * This method re-hashes the key and places it correctly into the `lo` or `hi` region
		 * of the *new* map, forming new collision chains as needed.
		 * It does NOT handle key existence checks for updates; it always adds or forms a new chain.
		 *
		 * @param key   The key to copy.
		 * @param value The value to copy.
		 */
		private void copy( double key, short value ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 )
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				if( links.length == _lo_Size ) links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) );
				links[ dst_index = _lo_Size++ ] = ( int ) ( index );
			}
			
			keys[ dst_index ]       = key;
			values[ dst_index ]     = value;
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
		}
		
		/**
		 * Creates and returns a shallow copy of this map. The internal arrays are cloned,
		 * but the primitive elements within those arrays are copied by value.
		 *
		 * @return A shallow copy of this map.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
	}
}