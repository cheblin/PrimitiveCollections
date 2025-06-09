package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.Arrays;
import java.util.ConcurrentModificationException;

/**
 * A specialized map for mapping primitive keys to primitive values.
 * <p>
 * This implementation uses a HYBRID strategy that automatically adapts based on data density:
 * <p>
 * 1. SPARSE MODE (Hash Map Phase):
 * - Starts as a hash map, optimized for sparse data.
 * - Uses a dual-region approach for key/value storage to minimize memory overhead:
 * - {@code lo Region}: Stores entries involved in hash collisions. Uses an explicit {@code links} array for chaining.
 * Occupies low indices (0 to {@code _lo_Size-1}) in {@code keys}/{@code values}.
 * - {@code hi Region}: Stores entries that do not (initially) require {@code links} links (i.e., no collision at insertion time, or they are terminal nodes of a chain).
 * Occupies high indices (from {@code keys.length - _hi_Size} to {@code keys.length-1}) in {@code keys}/{@code values}.
 * - Manages collision-involved ({@code lo Region}) and initially non-collision ({@code hi Region}) entries distinctly.
 * <p>
 * 2. DENSE MODE (Flat Array Phase):
 * - Automatically transitions to a direct-mapped flat array when a resize operation targets a capacity beyond
 * {@link RW#flatStrategyThreshold} (typically 0x7FFF = 32,767 entries).
 * - Provides guaranteed O(1) performance with full key range support (0-65535).
 * <p>
 * This approach balances memory efficiency for sparse maps with optimal performance for dense maps.
 *
 * <h3>Hash Map Phase Optimization:</h3>
 * The hash map phase uses a sophisticated dual-region strategy for storing keys and values to minimize memory usage:
 *
 * <ul>
 * <li><b>{@code hi Region}:</b> Occupies high indices in the key and value storage arrays.
 *     Entries are added at {@code keys[keys.length - 1 - _hi_Size]}, and {@code _hi_Size} is incremented.
 *     Thus, this region effectively grows downwards from {@code keys.length-1}. Active entries are in indices
 *     {@code [keys.length - _hi_Size, keys.length - 1]}.
 *     <ul>
 *         <li>Stores entries that, at the time of insertion, map to an empty bucket in the hash table.</li>
 *         <li>These entries can also become the terminal entries of collision chains originating in the {@code lo Region}.</li>
 *         <li>Managed stack-like: new entries are added to what becomes the lowest index of this region if it were viewed as growing from {@code keys.length-1} downwards.</li>
 *     </ul>
 * </li>
 *
 * <li><b>{@code lo Region}:</b> Occupies low indices in the key and value storage arrays, indices {@code 0} through {@code _lo_Size - 1}. It grows upwards
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
 * <h3>Removal Algorithm (Hash Mode):</h3>
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
 * <li><b>Removing an entry `E` at `removeIndex` in the {@code lo Region} (`removeIndex < _lo_Size`):</b>
 *     <ul>
 *         <li><b>Case 1: `E` is the head of its collision chain (i.e., `_buckets[bucketIndex] - 1 == removeIndex`):</b>
 *             <ul><li>The bucket `_buckets[bucketIndex]` is updated to point to the next entry in the chain (`links[removeIndex]`).</li></ul></li>
 *         <li><b>Case 2: `E` is within or at the end of its collision chain (not the head):</b>
 *             <ul><li>The chaining link of the preceding entry in the chain is updated to bypass `E`.</li>
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
 *             Any pointers (bucket or chaining links from other entries) to the moved entry (`_lo_Size-1`) are updated to its new location (`removeIndex`).
 *             `_lo_Size` is decremented. This ensures the {@code lo Region} remains dense.
 *         </li>
 *     </ul>
 * </li>
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
 * <li>Allocate new internal storage arrays (hash table, key storage, value storage, and chaining links) with the new capacity.</li>
 * <li>Iterate through all valid entries in the old {@code lo Region} (indices {@code 0} to {@code old_lo_Size - 1}) and
 * old {@code hi Region} (indices {@code old_keys.length - old_hi_Size} to {@code old_keys.length - 1}).</li>
 * <li>Re-insert each key-value pair into the new structure using an internal {@code copy} operation, which efficiently re-hashes and re-inserts entries into the new structure, rebuilding the hash table and regions.</li>
 * </ul>
 *
 * <h3>Flat Array Phase (Dense Mode):</h3>
 * <ul>
 * <li><b>Trigger:</b> Switches to this mode if a resize operation targets a capacity greater than {@link RW#flatStrategyThreshold} (0x7FFF = 32,767 entries),
 *     or if the initial capacity is set above this threshold.</li>
 * <li><b>Storage:</b> Uses a {@code nulls} bitset (1 bit per possible key) for presence
 * tracking and a {@code values} array of size 65536 for direct key-to-value mapping.
 *     The key itself serves as the index into the {@code values} array.</li>
 * <li><b>Memory:</b> Hash-mode specific arrays like the hash table, chaining links, and the dual-region key array are discarded (set to null).
 * <li><b>Performance:</b> Guaranteed O(1) access for all operations (put, get, remove) as there are no hash collisions.</li>
 * </ul>
 */
public interface CharCharNullMap {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		protected boolean                hasNullKey;
		/**
		 * Stores whether the conceptual null key maps to a non-null value.
		 */
		protected boolean                nullKeyHasValue;
		/**
		 * Stores the primitive value associated with the conceptual null key. Only meaningful if {@link #nullKeyHasValue} is true.
		 */
		protected char            nullKeyValue;
		/**
		 * Internal array serving as the primary hash table. Stores 1-based indices into the {@code keys} array,
		 * indicating the head of a collision chain for each bucket, or 0 if the bucket is empty.
		 */
		protected char[]                 _buckets;
		/**
		 * Internal array used to manage collision chains in hash map mode. Each entry stores the 0-based index
		 * of the next entry in its chain.
		 */
		protected char[]                 links;
		/**
		 * Internal array storing the primitive keys of the map entries.
		 */
		protected char[]         keys;
		/**
		 * Array storing the actual primitive values of the map entries.
		 * Values are stored at the same indices as their corresponding keys.
		 */
		protected char[]          values;
		/**
		 * Bitset array indicating whether a value at a given index is non-null.
		 * Each bit corresponds to an entry index in `keys` and `values` arrays.
		 * If `nulls[i / 64]` has the `(i % 64)`-th bit set, the value at `values[i]` is non-null.
		 * Otherwise, it is conceptually null (i.e., values[i] holds a default primitive value).
		 */
		protected long[]                 nullsVal;
		
		
		/**
		 * Number of active entries when the map is operating in dense (flat array) strategy.
		 */
		protected int flat_count;
		
		/**
		 * Returns the number of non-null key mappings in the map when in hash map strategy.
		 *
		 * @return The number of non-null key mappings.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * A counter that increments with every structural modification to the map, used for concurrency control and token validation.
		 */
		protected int _version;
		
		protected int _lo_Size;
		protected int _hi_Size;
		
		protected static final int VERSION_SHIFT = 32;
		
		protected static final int NULL_KEY_INDEX = 0x1_FFFF;
		
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Checks if the map is currently operating in the dense (flat array) strategy.
		 * This is determined by whether the presence bitset array has been allocated.
		 *
		 * @return {@code true} if in flat strategy, {@code false} if in hash map strategy.
		 */
		protected boolean isFlatStrategy() { return nullsKey != null; }
		
		/**
		 * In dense (flat array) mode, this bitset tracks the presence of primitive keys.
		 * Each bit corresponds to a primitive value (0-65535). A set bit indicates the key is present.
		 */
		protected long[] nullsKey;
		
		
		protected static final int FLAT_ARRAY_SIZE = 0x10000;
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
		 * Returns the total number of key-value mappings in this map.
		 * This is an alias for {@link #size()}.
		 *
		 * @return The number of entries in the map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the internal storage arrays.
		 * In flat strategy, this is fixed at {@link #FLAT_ARRAY_SIZE}.
		 * In sparse strategy, it's the allocated size of the {@code keys} array.
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
		public boolean containsKey(  Character key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean containsKey( char key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Checks if this map contains one or more keys mapped to the specified boxed value.
		 * This involves iterating through the values, which can be inefficient (O(N) in hash mode, potentially O(capacity) in flat mode if sparse).
		 * Checks both non-null key mappings and the null key mapping (if present).
		 *
		 * @param value The value to search for (can be null, representing a conceptual null value).
		 * @return {@code true} if the value is found, {@code false} otherwise.
		 */
		public boolean containsValue(  Character value ) {
			// If `value` is not null, perform a direct search for the primitive value
			// Check the conceptual null key first if it has a non-null value
			if( value != null ) return containsValue( ( char ) ( value + 0 ) );
			
			// Special check for conceptual null key mapping to conceptual null value
			if( hasNullKey && !nullKeyHasValue ) return true;
			
			// Scan lo region (indices 0 to _lo_Size-1) for unset bits (null values)
			if( 0 < _lo_Size )
				for( int nulls_idx = 0; ; nulls_idx++ ) {
					int remaining = _lo_Size - ( nulls_idx << 6 );
					if( remaining < 64 )
						if( ( nullsVal[ nulls_idx ] | -1L << remaining ) == -1 ) break;  // Set bits beyond _lo_Size to 1
						else return true;
					
					if( nullsVal[ nulls_idx ] != -1L ) return true; // Found an unset bit (null value)
				}
			
			// Skip hi region if empty
			if( _hi_Size == 0 ) return false;
			
			// Scan hi region (indices keys.length - _hi_Size to keys.length-1)
			int  hi_start  = keys.length - _hi_Size;
			int  nulls_idx = hi_start >> 6;
			long bits      = nullsVal[ nulls_idx ]; // Get the first 64-bit segment
			
			int bit = hi_start & 63; // Starting bit in the first segment
			if( 0 < bit ) bits &= -1L << bit; // Clear bits before hi_start
			
			// Process all hi region segments, including the first
			for( int remaining; ; bits = nullsVal[ ++nulls_idx ] ) {
				// Mask bits beyond keys.length in the last partial segment
				if( nulls_idx == nullsVal.length )
					if( 0 < ( remaining = keys.length & 63 ) ) // Bits in the last segment (0 if keys.length is 64-bit aligned)
						return ( nullsVal[ nulls_idx ] | -1L << remaining ) != -1;  // Set bits beyond _lo_Size to 1
				
				if( bits != -1L ) return true; // Found an unset bit (null value)
			}
		}
		
		/**
		 * Checks if the map contains one or more keys mapped to the specified primitive value.
		 * This involves iterating through the values, which can be inefficient (O(N) in hash mode, potentially O(capacity) in flat mode if sparse).
		 * Checks both non-null key mappings and the null key mapping (if present and has a non-null value).
		 *
		 * @param value The primitive value to search for.
		 * @return {@code true} if the value is found, {@code false} otherwise.
		 */
		public boolean containsValue( char value ) {
			if( hasNullKey && nullKeyHasValue && nullKeyValue == value ) return true;
			
			if( _count() == 0 ) return false;
			
			int  nulls_idx = -1;
			long bits      = 0;
			
			// Scan lo region (0 to _lo_Size-1)
			for( int i = 0; i < _lo_Size; i++ ) {
				if( nulls_idx != i >> 6 ) bits = nullsVal[ nulls_idx = i >> 6 ];
				if( ( bits & 1L << i ) != 0 && values[ i ] == value ) return true;
			}
			
			nulls_idx = -1;
			// Scan hi region (keys.length - _hi_Size to keys.length-1)
			for( int i = keys.length - _hi_Size; i < keys.length; i++ ) {
				if( nulls_idx != i >> 6 ) bits = nullsVal[ nulls_idx = i >> 6 ];
				if( ( bits & 1L << i ) != 0 && values[ i ] == value ) return true;
			}
			
			return false;
		}
		
		
		/**
		 * Returns a "token" representing the internal location of the specified boxed key.
		 * This token can be used for fast access to the key and its value via {@link #key(long)} and {@link #value(long)}.
		 * It also includes a version stamp to detect concurrent modifications.
		 *
		 * @param key The boxed key to get the token for. Handles {@code null} keys.
		 * @return A numeric token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 */
		public long tokenOf(  Character key ) {
			return key == null ?
			       ( hasNullKey ?
			         token( NULL_KEY_INDEX ) :
			         INVALID_TOKEN ) :
			       tokenOf( ( char ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a "token" representing the internal location of the specified primitive key.
		 * This token can be used for fast access to the key and its value via {@link #key(long)} and {@link #value(long)}.
		 * It also includes a version stamp to detect concurrent modifications.
		 *
		 * @param key The primitive key to get the token for.
		 * @return A numeric token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if a concurrent structural modification is detected while traversing a collision chain.
		 */
		public long tokenOf( char key ) {
			if( isFlatStrategy() )
				return exists( ( char ) key ) ?
				       token( ( char ) key ) :
				       INVALID_TOKEN;
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			int index = (_buckets[ bucketIndex( Array.hash( key ) ) ] ) - 1;
			if( index < 0 ) return INVALID_TOKEN;
			
			if( _lo_Size <= index )
				return keys[ index ] == key ?
				       token( index ) :
				       INVALID_TOKEN;
			
			
			for( int collisions = 0; ; ) {
				if( keys[ index ] == key ) return token( index );
				if( _lo_Size <= index ) break; //terminal node
				index = links[ index ];
				if( _lo_Size < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
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
		 * Returns the index of the next non-null key for fast, **unsafe** iteration, bypassing concurrency checks.
		 *
		 * <p><b>Usage:</b> Start iteration by calling {@code unsafe_token(-1)}. Subsequent calls should pass the previously returned index
		 * to get the next index. Iteration stops when this method returns {@code -1}.
		 *
		 * <p><b>Excludes Null Key:</b> This iteration method **only** covers non-null keys. Check for the null key separately using
		 * {@link #hasNullKey()} and access its value via {@link #nullKeyValue()}.
		 *
		 * <p><strong>⚠️ WARNING: UNSAFE ⚠️</strong> This method offers higher performance than {@link #token(long)} by omitting version checks.
		 * However, if the map is structurally modified (e.g., keys added or removed, resizing) during an iteration using this method,
		 * the behavior is undefined. It may lead to missed entries, incorrect results, infinite loops, or runtime exceptions
		 * (like {@code ArrayIndexOutOfBoundsException}). **Only use this method when you can guarantee that no modifications
		 * will occur to the map during the iteration.**
		 *
		 * @param token The index returned by the previous call to this method, or {@code -1} to start iteration.
		 * @return The index of the next non-null key, or {@code -1} if no more non-null keys exist.
		 * @see #token(long) For safe iteration that includes the null key and checks for concurrent modifications.
		 */
		public int unsafe_token( final int token ) {
			if( isFlatStrategy() ) {
				int bit = index( token ) + 1;
				if( 0xFFFF < bit ) return -1;
				int  nullsKey_idx = bit >>> 6;
				long value        = nullsKey[ nullsKey_idx ];
				if( value == -1L ) return bit;
				
				if( value != 0 ) value &= -1L << bit;
				
				while( value == 0 )
					if( ++nullsKey_idx == NULLS_SIZE ) return -1;
					else value = nullsKey[ nullsKey_idx ];
				
				return ( nullsKey_idx << 6 ) + Long.numberOfTrailingZeros( value );
			}
			
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
		 * Checks if the conceptual null key currently maps to a non-null value.
		 * Returns {@code false} if the null key is not present or if it maps to a conceptual null value.
		 *
		 * @return {@code true} if the null key exists and has a non-null value, {@code false} otherwise.
		 */
		public boolean nullKeyHasValue() { return hasNullKey && nullKeyHasValue; }
		
		/**
		 * Returns the primitive value associated with the null key.
		 *
		 * @return The primitive value associated with the null key.
		 * @throws java.util.NoSuchElementException if {@link #hasNullKey()} is {@code false}.
		 */
		public char nullKeyValue() { return (char) nullKeyValue; }
		
		
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
		public char key( long token ) {
			return ( char ) (char) (
					isFlatStrategy() ?
					index( token ) :
					keys[ index( token ) ] );
		}
		
		/**
		 * Checks if the entry associated with the given token has a non-null value.
		 * Handles both regular tokens and the special null key token.
		 *
		 * @param token The token to check (can be for the null key).
		 * @return {@code true} if the token is valid and the corresponding entry maps to a non-null value, {@code false} otherwise (including if the value is conceptually null or the token is invalid/expired).
		 */
		public boolean hasValue( long token ) {
			return isKeyNull( token ) ?
			       nullKeyHasValue :
			       ( nullsVal[ index( token ) >> 6 ] & 1L << index( token ) ) != 0;
		}
		
		/**
		 * Returns the primitive value associated with the specified token.
		 * <p>
		 * <b>Precondition:</b> This method should only be called if {@link #hasValue(long)} returns {@code true} for the given token.
		 * Calling it for a token associated with a {@code null} value results in undefined behavior (likely returning 0 or a stale value depending on the strategy).
		 * <p>
		 * The result is undefined if the token is {@link #INVALID_TOKEN} or has expired due to structural modification.
		 *
		 * @param token The token for which to retrieve the value.
		 * @return The primitive value associated with the token, or 0 if the value is conceptually null.
		 */
		public char value( long token ) {
			return (char)(
					isKeyNull( token ) ?
					nullKeyValue :
					values[ index( token ) ] );
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
		 * Returns {@code true} if the given object has the same type, and the two maps
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
					if( nullsKey != null ) cloned.nullsKey = nullsKey.clone();
				}
				else {
					if( _buckets != null ) cloned._buckets = _buckets.clone();
					if( links != null ) cloned.links = links.clone();
					if( keys != null ) cloned.keys = keys.clone();
				}
				
				if( values != null ) cloned.values = values.clone();
				if( nullsVal != null ) cloned.nullsVal = nullsVal.clone();
				
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
		 * Primitive keys are represented as their numeric value.
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
				     json.name( token ).value( hasValue( token ) ?
				                               value( token ) :
				                               null );
			else
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( keys[ token ] ).value( hasValue( token ) ?
				                                       value( token ) :
				                                       null );
			
			json.exitObject();
		}
		
		/**
		 * Calculates the bucket index for a given hash value within the internal hash table.
		 *
		 * @param hash The hash value of a key.
		 * @return The calculated bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		
		/**
		 * Packs an internal array index and the current map version into a single numeric token.
		 * The version is stored in the higher 32 bits, and the index in the lower 32 bits.
		 *
		 * @param index The 0-based index of the entry in the internal arrays (or {@link #NULL_KEY_INDEX} for the null key).
		 * @return A numeric token representing the entry.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index ); }
		
		
		/**
		 * Extracts the 0-based internal array index from a given numeric token.
		 *
		 * @param token The numeric token.
		 * @return The 0-based index.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		
		/**
		 * Extracts the version stamp from a given numeric token.
		 *
		 * @param token The numeric token.
		 * @return The version stamp.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
		/**
		 * In dense (flat array) mode, checks if a given primitive key exists by checking its
		 * corresponding bit in the presence bitset.
		 *
		 * @param key The primitive key to check.
		 * @return {@code true} if the key exists, {@code false} otherwise.
		 */
		protected final boolean exists( char key ) { return ( nullsKey[ key >>> 6 ] & 1L << key ) != 0; }
		
		
	}
	
	
	/**
	 * Concrete implementation providing read-write functionalities.
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
		 * Constructs an empty RW with a default initial capacity.
		 */
		public RW() { this( 0 ); }
		
		
		/**
		 * Constructs an empty RW with the specified initial capacity.
		 * The map will use the sparse (hash map) strategy unless the initial capacity
		 * exceeds {@link #flatStrategyThreshold}, in which case it starts in dense (flat array) mode.
		 *
		 * @param capacity The initial capacity of the map.
		 */
		public RW( int capacity ) {
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Initializes or re-initializes the internal arrays based on the specified capacity
		 * and the current strategy (sparse or dense). This method updates the map's version.
		 *
		 * @param capacity The desired capacity for the new internal arrays.
		 * @return The actual allocated capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			flat_count = 0;
			if( flatStrategyThreshold < capacity ) {
				nullsKey = new long[ NULLS_SIZE ];
				values   = new char[ capacity ];
				nullsVal = new long[ capacity + 63 >> 6 ];
				_buckets = null;
				links    = null;
				keys     = null;
				_hi_Size = 0;
				_lo_Size = 0;
				return FLAT_ARRAY_SIZE;
			}
			nullsKey = null;
			_buckets = new char[ capacity ];
			links    = new char[ Math.min( 16, capacity ) ];
			_lo_Size = 0;
			keys     = new char[ capacity ];
			values   = new char[ capacity ];
			nullsVal = new long[ capacity + 63 >> 6 ];
			_hi_Size = 0;
			return length();
		}
		
		/**
		 * Core insertion/update logic for the conceptual null key, setting its associated value to conceptual null.
		 *
		 * @return {@code true} if the map state was changed (null key was added).
		 */
		public boolean put() {
			
			if( !hasNullKey ) {
				hasNullKey = true;
				_version++;
				
				return true;
			}
			
			if( nullKeyHasValue ) {
				nullKeyHasValue = false;
				_version++;
			}
			
			return false;
			
		}
		
		/**
		 * Associates the specified boxed value with the conceptual null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced. Handles null value.
		 *
		 * @param value The value (can be null).
		 * @return {@code true} if the map state was changed (insert or update).
		 */
		public boolean put(  Character value ) {
			return value == null ?
			       put() :
			       put( value );
		}
		
		/**
		 * Associates the specified boxed value with the specified boxed key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles null key and null value.
		 *
		 * @param key   The key (can be null).
		 * @param value The value (can be null).
		 * @return {@code true} always, as this method performs an insert or update.
		 */
		public boolean put(  Character key,  Character value ) {
			return key == null ?
			       value == null ?
			       put() :
			       putNullKey( value ) :
			       value == null ?
			       putNullValue( ( char ) ( key + 0 ) ) :
			       put( ( char ) ( key + 0 ), ( char ) ( value + 0 ), true );
		}
		
		/**
		 * Associates the specified boxed value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced. Handles null value.
		 *
		 * @param key   The primitive key.
		 * @param value The value (can be null).
		 * @return {@code true} if the map state was changed (insert or update).
		 */
		public boolean put( char key,  Character value ) {
			return value == null ?
			       putNullValue( ( char ) ( key + 0 ) ) :
			       put( ( char ) ( key + 0 ), ( char ) ( value + 0 ), true );
		}
		
		
		/**
		 * Associates the specified primitive value with the conceptual null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The primitive value.
		 * @return {@code true} if the map state was changed (insert or update).
		 */
		public boolean put( char value ) {
			_version++;
			nullKeyHasValue = true;
			
			if( hasNullKey ) {
				nullKeyValue = ( char ) value;
				return false;
			}
			
			hasNullKey   = true;
			nullKeyValue = ( char ) value;
			return true;
		}
		
		
		/**
		 * Associates the specified value with the null key in this map.
		 * <p>
		 * If the map previously contained a mapping for the null key, the old value is replaced by the specified value.
		 *
		 * @param value    the value to be associated with the null key.
		 * @param hasValue {@code true} if the value is non-null, {@code false} if it is conceptually null.
		 * @return {@code true} if the map was structurally modified as a result of this operation (null key was added), {@code false} otherwise.
		 */
		private boolean put( char value, boolean hasValue ) {
			boolean b = !hasNullKey;
			
			hasNullKey = true;
			if( nullKeyHasValue = hasValue ) nullKeyValue = ( char ) value;
			_version++;
			return b;
		}
		
		/**
		 * Associates the specified primitive value with the conceptual null key in this map.
		 *
		 * @param value The primitive value to associate with the null key.
		 * @return {@code true} if the map was structurally modified (null key was added), {@code false} otherwise.
		 */
		public boolean putNullKey( char  value ) { return put( value, true ); }
		
		/**
		 * Associates a conceptual null value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, its value is set to conceptual null.
		 *
		 * @param key The primitive key.
		 * @return {@code true} if the map was structurally modified (key was added), {@code false} otherwise.
		 */
		public boolean putNullValue( char key ) { return put( key, ( char ) 0, false ); }
		
		/**
		 * Associates the specified primitive value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The primitive key.
		 * @param value The primitive value.
		 * @return {@code true} if the map was structurally modified (key was added), {@code false} otherwise.
		 */
		public boolean put( char key, char value ) { return put( key, value, true ); }
		
		/**
		 * Core insertion logic for non-null keys, handling both hash map and flat array strategies.
		 *
		 * @param key      The primitive key to insert or update.
		 * @param value    The primitive value to associate with the key.
		 * @param hasValue {@code true} if `value` represents a non-null value, {@code false} if it represents a conceptual null.
		 * @return {@code true} if the map was structurally modified (new key added); {@code false} if an existing key's value was updated.
		 * @throws ConcurrentModificationException If potential hash chain corruption is detected.
		 */
		private boolean put( char key, char value, boolean hasValue ) {
			
			if( isFlatStrategy() ) {
				boolean b;
				if( b = !exists( ( char ) key ) ) {
					exists1( ( char ) key );
					flat_count++;
				}
				
				if( hasValue ) {// Update value
					values[ ( char ) key ] = ( char ) value;
					nullsVal[ ( char ) key >> 6 ] |= 1L << ( char ) key;
				}
				else
					nullsVal[ ( char ) key >> 6 ] &= ~( 1L << ( char ) key );
				_version++;
				return b;
			}
			
			if( _buckets == null ) initialize( 7 );
			else if( _count() == keys.length ) {
				int i = Array.prime( keys.length * 2 );
				if( flatStrategyThreshold < i && keys.length < flatStrategyThreshold ) i = flatStrategyThreshold;
				
				resize( i );
				if( isFlatStrategy() ) return put( key, value, hasValue );
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
					if( keys[ index ] == ( char ) key ) { // Key matches existing {@code hi Region} entry
						if( hasValue ) {// Update value
							values[ index ] = ( char ) value;
							nullsVal[ index >> 6 ] |= 1L << index;
						}
						else
							nullsVal[ index >> 6 ] &= ~( 1L << index );
						
						_version++;
						return false; // Key was not new
					}
				}
				else // Entry is in {@code lo Region} (collision chain)
					for( int next = index, collisions = 0; ; ) {
						if( keys[ next ] == key ) {
							if( hasValue ) {// Update value
								values[ next ] = ( char ) value;
								nullsVal[ next >> 6 ] |= 1L << next;
							}
							else
								nullsVal[ next >> 6 ] &= ~( 1L << next );
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
			
			keys[ dst_index ] = ( char ) key;
			if( hasValue ) {// Update value
				values[ dst_index ] = ( char ) value;
				nullsVal[ dst_index >> 6 ] |= 1L << dst_index;
			}
			else
				nullsVal[ dst_index >> 6 ] &= ~( 1L << dst_index );
			
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
		public boolean remove(  Character key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( char ) ( key + 0 ) );
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
		 * within the internal key and value storage arrays. This method is crucial for compaction
		 * during removal operations in sparse mode.
		 * <p>
		 * After moving the data, this method updates any existing pointers (either from a hash bucket in
		 * the hash table or from a chaining link in a collision chain) that previously referenced
		 * {@code src} to now correctly reference {@code dst}.
		 * <p>
		 * A critical assumption for this method is that {@code src} and {@code dst} must both belong
		 * to the same region (either both in {@code lo Region} or both in {@code hi Region}).
		 * This constraint simplifies pointer updates, as {@code hi Region} entries do not participate
		 * in chaining links themselves (though they can be terminal nodes of collision chains originating in the {@code lo Region}).
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
			if( ( nullsVal[ src >> 6 ] & 1L << src ) != 0 ) {
				values[ dst ] = values[ src ];
				nullsVal[ dst >> 6 ] |= 1L << dst;
			}
			else
				nullsVal[ dst >> 6 ] &= ~( 1L << dst );
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
		 *         <li><b>Case B: Entry is within or at the end of its chain:</b> The chaining link of the
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
		 *         <li><b>Compaction:</b> After chain adjustments, the {@code lo Region} is compacted:
		 *             The data from the last logical entry in {@code lo Region} (at index {@code _lo_Size-1})
		 *             is moved into the freed {@code removeIndex} slot using {@link #move(int, int)}.
		 *             All pointers (bucket or chaining links) to the moved entry's original position
		 *             are updated to its new location. {@code _lo_Size} is decremented.</li>
		 *     </ul>
		 * </li>
		 * </ol>
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return {@code true} if the map contained a mapping for the specified key, {@code false} otherwise.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal.
		 */
		public boolean remove( char key ) {
			
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
						
						if( ( nullsVal[ last >> 6 ] & 1L << last ) != 0 ) { // If `last` has a non-null value.
							values[ removeIndex ] = values[ last ];
							nullsVal[ removeIndex >> 6 ] |= 1L << removeIndex; // Set bit for new location.
						}
						else
							nullsVal[ removeIndex >> 6 ] &= ~( 1L << removeIndex ); // Clear bit for new location.
						
						
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
								
								if( ( nullsVal[ last >> 6 ] & 1L << last ) != 0 ) { // If `last` has a non-null value.
									values[ removeIndex ] = values[ last ];
									nullsVal[ removeIndex >> 6 ] |= 1L << removeIndex; // Set bit for new location.
								}
								else
									nullsVal[ removeIndex >> 6 ] &= ~( 1L << removeIndex ); // Clear bit for new location.
								
								
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
				Array.fill( nullsKey, 0 );
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
				if( flatStrategyThreshold < newSize ) return length();// Dense mode: Stay in dense mode
				
				// Transition from dense to sparse mode
				char[] old_values   = values;
				long[]        old_nullsVal = nullsVal;
				long[]        old_nullsKey = nullsKey;
				int           old_count    = flat_count;
				
				initialize( newSize );
				
				
				for( int nulls_index = 0; 0 < old_count; nulls_index++ ) {
					long bitsKey = old_nullsKey[ nulls_index ];
					if( bitsKey == 0 ) continue;
					
					long bitsVal = old_nullsVal[ nulls_index ];
					// in previous FlatStrategy, FLAT_ARRAY_SIZE and NULLS_SIZE are fit to each other
					int key = nulls_index << 6, max = key + 64;
					
					if( bitsKey == -1 ) {
						old_count -= 64;
						for( ; key < max; key++ )
							if( ( bitsVal & 1L << key ) != 0 )
								copy( ( char ) key, ( char ) old_values[ key ], true );
							else
								copy( ( char ) key, ( char ) 0, false );
					}
					else
						for( long bit; key < max; key++ )
							if( ( bitsKey & ( bit = 1L << key ) ) != 0 ) {
								old_count--;
								if( ( bitsVal & bit ) != 0 )
									copy( ( char ) key, ( char ) old_values[ key ], true );
								else
									copy( ( char ) key, ( char ) 0, false );
							}
				}
				
				
				return length();
			}
			
			
			if( flatStrategyThreshold < newSize ) {// Sparse mode: Transition to dense mode if newSize > flatStrategyThreshold
				long[]        old_nullsVal = nullsVal;
				char[] old_values   = values;
				
				
				values   = new char[ FLAT_ARRAY_SIZE ];
				nullsVal = new long[ FLAT_ARRAY_SIZE + 63 >> 6 ];
				long[] nullsKey = new long[ NULLS_SIZE ];
				
				for( int i = -1; ( i = unsafe_token( i ) ) != -1; ) {
					char key = ( char ) keys[ i ];
					exists1( key, nullsKey );
					
					if( ( old_nullsVal[ i >> 6 ] & 1L << i ) != 0 ) { // If `last` has a non-null value.
						values[ key ] = old_values[ i ];
						nullsVal[ key >> 6 ] |= 1L << key; // Set bit for new location.
					}
					else
						nullsVal[ key >> 6 ] &= ~( 1L << key ); // Clear bit for new location.
					
					
				}
				flat_count    = _count();
				this.nullsKey = nullsKey;
				_buckets      = null;
				links         = null;
				keys          = null;
				_lo_Size      = 0;
				_hi_Size      = 0;
				return FLAT_ARRAY_SIZE;
			}
			
			
			char[] old_keys     = keys;
			long[]         old_nullsVal = nullsVal;
			char[]  old_values   = values;
			int            old_lo_Size  = _lo_Size;
			int            old_hi_Size  = _hi_Size;
			initialize( newSize );
			
			int  nulls_idx = 0;
			long bits      = old_nullsVal[ nulls_idx ];
			// Copy entries from the old lo Region to the new structure
			for( int i = 0; i < old_lo_Size; i++ )
				if( ( ( i >> 6 == nulls_idx ?
				        bits :
				        ( bits = old_nullsVal[ ++nulls_idx >> 6 ] ) ) & 1L << i ) != 0 )
					copy( ( char ) old_keys[ i ], ( char ) old_values[ i ], true );
				else
					copy( ( char ) old_keys[ i ], ( char ) 0, false );
			
			
			// Copy entries from the old hi Region to the new structure
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
				if( ( ( i >> 6 == nulls_idx ?
				        bits :
				        ( bits = old_nullsVal[ ++nulls_idx >> 6 ] ) ) & 1L << i ) != 0 )
					copy( ( char ) old_keys[ i ], ( char ) old_values[ i ], true );
				else
					copy( ( char ) old_keys[ i ], ( char ) 0, false );
			
			return length();
		}
		
		/**
		 * Internal helper method used during resizing in sparse mode to efficiently copy an
		 * existing key-value pair into the new hash table structure. It re-hashes the key
		 * and places it into the correct bucket and region (lo or hi) in the new arrays.
		 * This method does not check for existing keys, assuming all keys are new in the
		 * target structure during a resize operation.
		 *
		 * @param key      The primitive key to copy.
		 * @param value    The primitive value to copy.
		 * @param hasValue {@code true} if `value` represents a non-null value, {@code false} if it represents a conceptual null.
		 */
		private void copy( char key, char value, boolean hasValue ) {
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
			if( hasValue ) {
				values[ dst_index ] = value;
				nullsVal[ dst_index >> 6 ] |= 1L << ( dst_index & 63 ); // Set bit for non-null value
			}
			
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
		 * Internal helper method for dense (flat array) strategy: marks a bit in the provided bitset
		 * at the position corresponding to the given primitive key, indicating the key's presence.
		 *
		 * @param key   The primitive key to mark as present.
		 * @param nulls The bitset array to modify.
		 */
		protected final void exists1( char key, long[] nulls ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		/**
		 * Internal helper method for dense (flat array) strategy: marks a bit in the map's own presence bitset
		 * at the position corresponding to the given primitive key, indicating the key's presence.
		 *
		 * @param key The primitive key to mark as present.
		 */
		protected final void exists1( char key ) { nullsKey[ key >>> 6 ] |= 1L << key; }
		
		/**
		 * Internal helper method for dense (flat array) strategy: clears a bit in the map's own presence bitset
		 * at the position corresponding to the given primitive key, indicating the key's absence.
		 *
		 * @param key The primitive key to mark as absent.
		 */
		protected final void exists0( char key ) { nullsKey[ key >>> 6 ] &= ~( 1L << key ); }
	}
}