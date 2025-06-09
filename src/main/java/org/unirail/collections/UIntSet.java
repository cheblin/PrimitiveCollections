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

/**
 * A specialized set for storing primitive keys .
 * <p>
 * This implementation uses a HYBRID strategy that automatically adapts based on data density:
 * <p>
 * - Starts as a hash set, optimized for sparse data.
 * - Uses a dual-region approach for keys storage to minimize memory overhead:
 * - {@code lo Region}: Stores entries involved in hash collisions. Uses an explicit {@code links} array for chaining.
 * Occupies low indices (0 to {@code _lo_Size-1}) in {@code keys}.
 * - {@code hi Region}: Stores entries that do not (initially) require {@code links} links (i.e., no collision at insertion time, or they are terminal nodes of a chain).
 * Occupies high indices (from {@code keys.length - _hi_Size} to {@code keys.length-1}) in {@code keys}.
 * - Manages collision-involved ({@code lo Region}) and initially non-collision ({@code hi Region}) entries distinctly.
 * <p>
 * This approach balances memory efficiency for sparse sets with optimal performance for dense sets.
 *
 *
 * <ul>
 * <li><b>{@code hi Region}:</b> Occupies high indices in the {@code keys} array.
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
 * <li><b>{@code lo Region}:</b> Occupies low indices in the {@code keys} array, indices {@code 0} through {@code _lo_Size - 1}. It grows upwards
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
 *                 <li>If the new primitive key matches `keys[index]`, the key is already present.</li>
 *                 <li>If the new primitive key collides with `keys[index]`:
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
 *                 <li>If the primitive key is found in the chain, it's already present.</li>
 *                 <li>If the primitive key is not found after traversing the entire chain:
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
 *         <li>If `E` was the head of its collision chain, the bucket that pointed to `removeIndex` is cleared (set to 0).</li>
 *         <li>To compact the {@code hi Region}, the entry from the lowest-addressed slot in the {@code hi Region}
 *             (i.e., at `keys[keys.length - _hi_Size]`) is moved into the `removeIndex` slot using {@code move()},
 *             unless `removeIndex` was already the lowest-addressed slot.</li>
 *         <li>`_hi_Size` is decremented.</li>
 *     </ul>
 * </li>
 * <li><b>Removing an entry `E` at `removeIndex` in the {@code lo Region} (`removeIndex < _lo_Size`):</b>
 *     <ul>
 *         <li>The collision chain starting from its bucket is traversed to find `E` and its preceding entry.</li>
 *         <li>If `E` is the head of its collision chain, the bucket `_buckets[bucketIndex]` is updated to point to the next entry in the chain (`links[removeIndex]`).</li>
 *         <li>If `E` is within or at the end of its collision chain (not the head), the `links` link of the preceding entry in the chain is updated to bypass `E`.</li>
 *         <li>After chain adjustments, the {@code lo Region} is compacted:
 *             The data from the last logical entry in {@code lo Region} (at index `_lo_Size-1`) is moved into the freed `removeIndex` slot using {@code move()}.
 *             This includes updating `keys` and the `links` link of the moved entry.
 *             Any pointers (bucket or `links` links from other entries) to the moved entry (`_lo_Size-1`) are updated to its new location (`removeIndex`).
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
 * <li>Allocate new internal arrays ({@code _buckets}, {@code keys}, {@code links}) with the new capacity.</li>
 * <li>Iterate through all valid entries in the old {@code lo Region} (indices {@code 0} to {@code old_lo_Size - 1}) and
 * old {@code hi Region} (indices {@code old_keys.length - old_hi_Size} to {@code old_keys.length - 1}).</li>
 * <li>Re-insert each key into the new structure using an internal {@code copy} operation, which efficiently re-hashes and re-inserts entries into the new structure, rebuilding the hash table and regions.</li>
 * </ul>
 */
public interface UIntSet {
	
	/**
	 * {@code R} is a read-only abstract base class that implements the core functionalities and state management for
	 * a set of primitive keys. It handles the underlying structure which can be either a hash set or a flat bitset.
	 * It serves as a foundation for read-write implementations and provides methods for querying the set without modification.
	 * This class is designed to be lightweight and efficient for read-heavy operations.
	 * <p>
	 * It implements {@link JsonWriter.Source} to support JSON serialization and {@link Cloneable} for creating copies of the set.
	 */
	abstract class R implements JsonWriter.Source, Cloneable {
		/**
		 * Indicates whether the Set contains a null key. Null keys are handled separately.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		protected boolean       hasNullKey;          // Indicates if the set contains a null key.
		protected int[]         _buckets;            // Hash table buckets array (1-based indices to chain heads). Stores 0-based indices plus one.
		protected int[]         links;               // Links within collision chains. Stores 0-based indices.
		protected int[] keys;                // Stores the primitive keys in the set.
		
		protected int _lo_Size;                       // Number of active entries in the low region (0 to _lo_Size-1).
		protected int _hi_Size;                       // Number of active entries in the high region (keys.length - _hi_Size to keys.length-1).
		
		/**
		 * Returns the total number of active entries in the set when operating in hash mode.
		 * This is the sum of entries in the low and high regions.
		 *
		 * @return The current count of entries in hash mode.
		 */
		protected int _count() { return _lo_Size + _hi_Size; } // Total number of active entries in hash mode
		
		protected int _version;                       // Version counter for concurrent modification detection. Incremented on structural changes.
		
		protected static final int VERSION_SHIFT  = 32; // Number of bits to shift the version value when packing into a token.
		/**
		 * Special index used in tokens to represent the null key. This value is outside
		 * the valid range of array indices (0-65535).
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * A constant representing an invalid or non-existent token.
		 */
		protected static final long INVALID_TOKEN = -1L; // Invalid token constant.
		
		
		/**
		 * Returns the number of elements in this set (its cardinality).
		 * Corresponds to {@code Set.size()}.
		 *
		 * @return the number of elements in this set
		 */
		public int size() {
			return ( _count() ) + ( // Hash mode: _count() (lo_Size + hi_Size) is actual entries
					hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the number of elements in this set. This is an alias for {@link #size()}.
		 *
		 * @return the number of elements in this set
		 */
		public int count() { return size(); }
		
		/**
		 * Returns {@code true} if this set contains no elements.
		 *
		 * @return {@code true} if this set contains no elements
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the allocated capacity of the internal structure.
		 * In hash set mode, it's the length of the internal {@code keys} array.
		 * In flat mode, it's the fixed maximum size (65536).
		 *
		 * @return The capacity.
		 */
		public int length() {
			return ( keys == null ?
			         0 :
			         keys.length );
		}
		
		
		/**
		 * Checks if this set contains the specified boxed key. Handles null keys.
		 *
		 * @param key the boxed key to check for in this set.
		 * @return {@code true} if this set contains the specified key.
		 */
		public boolean contains(  Long      key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this set contains the specified primitive key.
		 *
		 * @param key the primitive key to check for in this set.
		 * @return {@code true} if this set contains the specified key.
		 */
		public boolean contains( long key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Returns a token for the specified boxed key if it exists in the set, otherwise returns {@link #INVALID_TOKEN}.
		 * Tokens are used for efficient iteration and element access. Handles null keys.
		 *
		 * @param key the boxed key to get the token for (can be null).
		 * @return a valid token if the key is in the set, -1 ({@link #INVALID_TOKEN}) if not found.
		 */
		public long tokenOf(  Long      key ) {
			return key == null ?
			       ( hasNullKey ?
			         token( NULL_KEY_INDEX ) :
			         INVALID_TOKEN ) :
			       tokenOf( ( long ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a "token" representing the internal location of the specified primitive key.
		 * This token can be used for fast access to the key via {@link #key(long)}.
		 * It also includes a version stamp to detect concurrent modifications.
		 *
		 * @param key The primitive key to get the token for.
		 * @return A {@code long} token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if a concurrent structural modification is detected while traversing a collision chain.
		 */
		public long tokenOf( long key ) {
			
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN; // Use _count()
			
			int index = ( _buckets[ bucketIndex( Array.hash( key ) ) ] ) - 1; // 0-based index
			if( index < 0 ) return INVALID_TOKEN; // Bucket is empty
			
			if( _lo_Size <= index ) // If the first entry is in the hi Region
				return keys[ index ] == key ?
				       token( index ) :
				       INVALID_TOKEN; // If key doesn't match, it's not here as hi entries are non-colliding heads unless moved
			
			// Traverse collision chain in lo Region
			for( int collisions = 0; ; ) {
				if( keys[ index ] == key ) return token( index );
				if( _lo_Size <= index ) break; // Reached a terminal node (which is in hi Region, or end of chain)
				index = links[ index ];
				if( _lo_Size < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." ); // Max _lo_Size collisions
			}
			return INVALID_TOKEN;
		}
		
		
		/**
		 * Returns the first valid token for iterating over set entries (excluding the null key initially).
		 * Call {@link #token(long)} subsequently to get the next token.
		 *
		 * @return The token for the first entry, or {@link #INVALID_TOKEN} if the set is empty.
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
		 * Returns the next valid token for iterating over set entries.
		 * This method is designed to be used in a loop:
		 * {@code for (long token = set.token(); token != INVALID_TOKEN; token = set.token(token))}
		 * <p>
		 * Iteration order: First, entries in the {@code lo Region} (0 to {@code _lo_Size-1}),
		 * then entries in the {@code hi Region} ({@code keys.length - _hi_Size} to {@code keys.length-1}),
		 * and finally the null key if it exists.
		 *
		 * @param token The current token obtained from a previous call to {@link #token()} or {@link #token(long)}.
		 *              Must not be {@link #INVALID_TOKEN}.
		 * @return The token for the next entry, or {@link #INVALID_TOKEN} if no more entries exist.
		 * @throws IllegalArgumentException        if the input token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the set has been structurally modified since the token was issued.
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
		 * Returns the next valid internal array index for iteration, excluding the null key.
		 * This is a low-level method primarily used internally for iteration logic.
		 * In hash mode, it iterates {@code lo Region} then {@code hi Region}.
		 * In flat mode, it iterates through set bits in the `nulls` array.
		 *
		 * @param token The current internal index, or -1 to start from the beginning.
		 * @return The next valid internal array index, or -1 if no more entries exist in the main set arrays.
		 */
		public int unsafe_token( final int token ) {
			
			if( _count() == 0 ) return -1; // Use _count()
			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size; // Start of hi region
			
			return i < _lo_Size ?
			       // Check lo region first
			       i :
			       i < lowest_hi ?
			       // If lo region exhausted, check if we're past its end
			       _hi_Size == 0 ?
			       // If hi region is empty, no more entries
			       -1 :
			       lowest_hi :
			       // Otherwise, return start of hi region
			       i < keys.length ?
			       // Iterate through hi region
			       i :
			       -1; // No more entries
		}
		
		
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
		public long key( long token ) {
			if( index( token ) == NULL_KEY_INDEX ) throw new IllegalArgumentException( "Token represents a null key." );
			return ( long )  ( keys[ index( token ) ] ); // In hash mode, retrieve from the keys array
		}
		
		/**
		 * Computes the hash code for this set.
		 * The hash code is calculated based on the keys in the set and the set's size.
		 * It iterates through the set using tokens and accumulates hash values using a mixing function.
		 *
		 * @return the hash code value for this set
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			// Use unsafe iteration for potentially better performance, assuming no concurrent modification during hashCode calculation.
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				final int h = Array.mix( seed, Array.hash( keys[ token ] ) ); // Key from array in hash mode
				a += h;
				b ^= h;
				c *= h | 1;
			}
			if( hasNullKey ) {
				final int h = Array.hash( seed ); // Hash the seed for the null key presence
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() ); // Finalize the hash with size and seed
		}
		
		/**
		 * Seed value used in hashCode calculation to add randomness and improve hash distribution.
		 * Initialized with the hashCode of the class.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this set with the specified object for equality.
		 * Returns {@code true} if the given object is of the same type, and the two sets
		 * contain the same number of elements, and each element in this set is also present in the
		 * other set.
		 *
		 * @param obj the object to compare with
		 * @return {@code true} if the specified object is an {@code R} instance with the same keys
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		/**
		 * Compares this set to another {@code R} instance for equality.
		 *
		 * @param other the {@code R} instance to compare with
		 * @return {@code true} if the sets contain the same keys
		 */
		public boolean equals( R other ) {
			if( other == this ) return true; // Same instance check
			if( other == null || other.size() != size() || other.hasNullKey != hasNullKey ) return false; // Size and null key presence check
			
			// Use unsafe iteration for potentially better performance
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( !other.contains(
						( long ) ( keys[ token ] ) ) ) return false; // Check if each key in this set is present in the other set
			return true; // All keys match
		}
		
		/**
		 * Creates and returns a deep copy of this set.
		 * All internal arrays are cloned, ensuring the cloned set is independent of the original.
		 *
		 * @return A cloned instance of this set.
		 * @throws InternalError if cloning fails (should not happen as Cloneable is supported).
		 */
		@Override
		public R clone() {
			try {
				R cloned = ( R ) super.clone();
				
				if( _buckets != null ) cloned._buckets = _buckets.clone();
				if( links != null ) cloned.links = links.clone();
				if( keys != null ) cloned.keys = keys.clone();
				
				return cloned;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e );
			}
		}
		
		/**
		 * Returns a string representation of this set in JSON array format.
		 * The null key is represented as JSON null. Primitive keys are written as numbers.
		 *
		 * @return A JSON string representing the set.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the set's content to a {@link JsonWriter} as a JSON array.
		 * The null key is represented as JSON null. Primitive keys are written as numbers.
		 *
		 * @param json the JsonWriter to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 5 ); // Pre-allocate buffer (estimate size based on average key length)
			json.enterArray(); // Start JSON array
			
			if( hasNullKey ) json.value(); // Write null value if null key is present
			
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) json.value( keys[ token ] ); // Key from the keys array
			json.exitArray(); // End JSON array
		}
		
		
		/**
		 * Calculates the bucket index for a given hash value within the {@code _buckets} array.
		 *
		 * @param hash The hash value of a key.
		 * @return The calculated bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Packs an internal array index and the current set version into a single {@code long} token.
		 * The version is stored in the higher 32 bits, and the index in the lower 32 bits.
		 *
		 * @param index The 0-based index of the entry in the internal arrays (or {@link #NULL_KEY_INDEX} for the null key).
		 * @return A {@code long} token representing the entry.
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); }
		
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
	}
	
	/**
	 * {@code RW} is a read-write implementation, extending the read-only base class {@link R}.
	 * It provides methods for modifying the set, such as adding and removing elements, as well as clearing and managing the set's capacity (including switching between hash and flat modes).
	 * This class is suitable for scenarios where both read and write operations are frequently performed on the set.
	 */
	class RW extends R {
		/**
		 * The threshold at which the set switches from the sparse (hash set) strategy to the
		 * dense (flat array) strategy. If the target capacity during a resize operation
		 * exceeds this value, or if the initial capacity is set above this threshold,
		 * the set transitions to flat mode.
		 * Current value: 0x7FFF (32,767 entries). This value was chosen to ensure that a
		 * hash table would likely require more memory than a flat bitset for the same
		 * number of elements, and to allow for O(1) operations in dense scenarios.
		 */
		protected static int flatStrategyThreshold = 0x7FFF;
		
		/**
		 * Constructs an empty {@code RW} set with a default initial capacity.
		 * The initial capacity is chosen to be small and will expand as needed.
		 */
		public RW() { this( 0 ); }
		
		/**
		 * Constructs an empty {@code RW} set with the specified initial capacity hint.
		 * The set will use the sparse (hash set) strategy unless the initial capacity
		 * exceeds {@link #flatStrategyThreshold}, in which case it starts in dense (flat array) mode.
		 *
		 * @param capacity the initial capacity hint. The set will be initialized to hold at least this many elements.
		 */
		public RW( int capacity ) { if( capacity > 0 ) initialize( Array.prime( capacity ) ); }
		
		
		/**
		 * Initializes or re-initializes the internal arrays based on the specified capacity
		 * and the current strategy (sparse or dense). This method updates the set's version.
		 * When transitioning to flat strategy, hash-specific arrays are nulled, and vice-versa.
		 *
		 * @param capacity The desired capacity for the new internal arrays.
		 * @return The actual allocated capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			
			_buckets = new int[ capacity ];
			links    = new int[ Math.min( 16, capacity ) ];
			_lo_Size = 0;
			keys     = new int[ capacity ];
			_hi_Size = 0;
			return length();
		}
		
		
		/**
		 * Adds the specified boxed key to this set if it is not already present. Handles null keys.
		 *
		 * @param key the boxed key to add to this set.
		 * @return {@code true} if this set did not already contain the specified key.
		 */
		public boolean add(  Long      key ) {
			return key == null ?
			       addNullKey() :
			       add( ( long ) ( key + 0 ) );
		}
		
		
		/**
		 * Adds the specified primitive key to this set if it is not already present.
		 * <p>
		 * <h3>Hash Set Insertion Logic (Sparse Mode):</h3>
		 * <ol>
		 * <li><b>Check for existing key:</b> If the key already exists, {@code false} is returned.
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
		 * <li><b>Update structures:</b> The new key is stored. The {@code _buckets} array is updated
		 *     to point to the new entry's index (1-based). The set's version is incremented.</li>
		 * </ol>
		 *
		 * @param key the primitive key to add to this set.
		 * @return {@code true} if this set did not already contain the specified key.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal.
		 */
		public boolean add( long key ) {
			if( _buckets == null ) initialize( 7 );
			else if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ) );
			
			int hash        = Array.hash( key );
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 )  // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++; // Add to the "bottom" of {@code hi Region}
			else {
				// Bucket is not empty, 'index' points to an existing entry
				if( _lo_Size <= index ) { // Entry is in {@code hi Region}
					if( keys[ index ] == ( int ) key ) { // Key matches existing {@code hi Region} entry
						_version++;
						return false; // Key was not new
					}
				}
				else // Entry is in {@code lo Region} (collision chain)
					for( int next = index, collisions = 0; ; ) {
						if( keys[ next ] == key ) {
							_version++;
							return false;// Key was not new
						}
						if( _lo_Size <= next ) break;
						next = links[ next ];
						
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
				
				if( links.length == ( dst_index = _lo_Size++ ) ) links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index;
			}
			
			keys[ dst_index ]       = ( int ) key;
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
			_version++;
			return true;
		}
		
		
		/**
		 * Adds a null key to the set if it is not already present.
		 *
		 * @return {@code true} if the null key was added, {@code false} if already present.
		 */
		public boolean addNullKey() {
			if( hasNullKey ) return false;
			hasNullKey = true;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes the mapping for the specified boxed key from this set if present.
		 * Handles {@code null} keys.
		 *
		 * @param key The boxed key whose mapping is to be removed.
		 * @return {@code true} if the set contained a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean remove(  Long      key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( long ) ( key + 0 ) );
		}
		
		
		/**
		 * Relocates an entry's key from a source index ({@code src}) to a destination index ({@code dst})
		 * within the internal {@code keys} array. This method is crucial for compaction
		 * during removal operations in sparse mode.
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
			int bucketIndex = bucketIndex( Array.hash( keys[ src ] ) );
			int index       = _buckets[ bucketIndex ] - 1;
			
			if( index == src ) _buckets[ bucketIndex ] = ( int ) ( dst + 1 );
			else {
				while( links[ index ] != src )
					index = links[ index ];
				
				links[ index ] = ( int ) dst;
			}
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			keys[ dst ] = keys[ src ];
		}
		
		/**
		 * Removes the specified primitive key from this set if it is present.
		 * <p>
		 * <h3>Hash Set Removal Logic (Sparse Mode):</h3>
		 * Removal involves finding the entry, updating chain links or bucket pointers, and then
		 * compacting the respective region ({@code lo} or {@code hi}) to maintain memory density.
		 * <ol>
		 * <li><b>Find entry:</b> Locate the entry corresponding to the key by traversing the hash bucket's chain.</li>
		 * <li><b>Handle 'hi Region' removal:</b>
		 *     <ul>
		 *         <li>If the found entry is in the {@code hi Region} and is the head of its bucket, the bucket pointer is cleared (set to 0).</li>
		 *         <li>To compact the {@code hi Region}, the entry from the "bottom" of the {@code hi Region}
		 *             (at {@code keys[keys.length - _hi_Size]}) is moved into the freed {@code removeIndex} slot using {@link #move(int, int)},
		 *             unless {@code removeIndex} was already that last slot.</li>
		 *         <li>{@code _hi_Size} is then decremented.</li>
		 *     </ul>
		 * </li>
		 * <li><b>Handle 'lo Region' removal:</b>
		 *     <ul>
		 *         <li>The collision chain starting from its bucket is traversed. If the key is found:</li>
		 *         <li>If the key is the head of its chain, the bucket pointer is updated to the next entry in the chain.</li>
		 *         <li>If the key is within or at the end of its chain, the {@code links} link of the preceding entry is updated to bypass it.</li>
		 *         <li>After chain adjustments, the {@code lo Region} is compacted:
		 *             The data from the last logical entry in {@code lo Region} (at index {@code _lo_Size-1})
		 *             is moved into the freed {@code removeIndex} slot using {@link #move(int, int)}.
		 *             All pointers (bucket or {@code links} links) to the moved entry's original position
		 *             are updated to its new location. {@code _lo_Size} is decremented.</li>
		 *     </ul>
		 * </li>
		 * </ol>
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return {@code true} if the set contained a mapping for the specified key, {@code false} otherwise.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal.
		 */
		public boolean remove( long key ) {
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
						keys[ removeIndex ] = keys[ last ]; //  Copies `keys[last]` to `keys[removeIndex]`
						
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
								keys[ removeIndex ] = keys[ last ];
								links[ prev ]       = ( int ) removeIndex;
								removeIndex         = last;
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
		 * Removes the null key from this set if it is present.
		 *
		 * @return {@code true} if the null key was removed, {@code false} if not present.
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey = false;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes all elements from this set. The set will be empty after this call returns.
		 * The internal arrays are reset to their initial state or cleared, but not deallocated.
		 * This operation increments the set's version.
		 */
		public void clear() {
			_version++;
			
			hasNullKey = false;
			
			
			if( _count() == 0 ) return; // Already empty in hash mode
			if( _buckets != null ) Arrays.fill( _buckets, ( int ) 0 ); // Clear bucket pointers
			_lo_Size = 0; // Reset lo/hi sizes
			_hi_Size = 0;
		}
		
		
		/**
		 * Returns an array containing all of the primitive keys in this set.
		 * The order of keys is not guaranteed.
		 *
		 * @param dst             The array into which the elements of this set are to be stored, if it is big enough;
		 *                        otherwise, a new array of the same runtime type is allocated for this purpose.
		 * @param null_substitute The primitive value to use if the null key is present in the set.
		 * @return An array containing all the keys in this set.
		 */
		public long[] toArray( long[] dst, long null_substitute ) {
			int s = size();
			if( dst.length < s ) dst = new long[ s ];
			
			int index = 0;
			if( hasNullKey ) dst[ index++ ] = null_substitute;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     dst[ index++ ] = ( long )  ( keys[ token ] ); // Key from the keys array
			
			return dst;
		}
		
		
		/**
		 * Ensures that this set can hold at least the specified number of entries without resizing.
		 * If the current capacity is less than the specified capacity, the set is resized.
		 * If in sparse mode and the set is uninitialized, it will be initialized.
		 * This operation increments the set's version if a structural change occurs.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new capacity of the set's internal arrays.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length(); // No change needed if capacity is sufficient
			return _buckets == null ?
			       initialize( capacity ) :
			       // Initialize if not already done
			       resize( Array.prime( capacity ) ); // Resize hash table
		}
		
		/**
		 * Trims the capacity of the set's internal arrays to reduce memory usage.
		 * The capacity will be reduced to the current size of the set, or a prime number
		 * slightly larger than the size, ensuring sufficient space for future additions.
		 * This method maintains the current strategy (sparse or dense) unless trimming
		 * a flat set below the flat strategy threshold.
		 * This operation increments the set's version if a structural change occurs.
		 */
		public void trim() { trim( size() ); }
		
		
		/**
		 * Trims the capacity of the set's internal arrays to a specified minimum capacity.
		 * If the current capacity is greater than the specified capacity, the set is resized.
		 * The actual new capacity will be a prime number at least {@code capacity} and
		 * at least the current {@link #size()}. This operation increments the set's version
		 * if a structural change occurs.
		 *
		 * @param capacity The minimum desired capacity after trimming.
		 */
		public void trim( int capacity ) {
			capacity = Array.prime( Math.max( capacity, size() ) ); // Ensure capacity is at least current size and a prime
			if( length() <= capacity ) return; // No trim needed if already small enough or larger capacity requested
			
			
			// If current strategy is hash, just resize if capacity is smaller than current length
			resize( capacity );
		}
		
		
		/**
		 * Resizes the set's internal arrays to a new specified size.
		 * This method handles transitions between sparse and dense strategies based on {@code newSize}
		 * relative to {@link #flatStrategyThreshold}. When resizing in sparse mode, all existing
		 * entries are rehashed and re-inserted into the new, larger structure. When transitioning
		 * to flat mode, data is copied directly.
		 * This operation increments the set's version.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resize.
		 */
		private int resize( int newSize ) {
			_version++;
			
			
			// Hash to Hash resize (remain in hash mode)
			int[] old_keys    = keys;
			int           old_lo_Size = _lo_Size;
			int           old_hi_Size = _hi_Size;
			initialize( newSize ); // Re-initialize with new hash capacity
			
			// Copy elements from old hash structure to new hash structure by re-inserting
			for( int i = 0; i < old_lo_Size; i++ )
			     copy( ( int ) old_keys[ i ] );
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
			     copy( ( int ) old_keys[ i ] );
			
			return length();
		}
		
		/**
		 * Internal helper method used during resizing in sparse mode to efficiently copy an
		 * existing key into the new hash table structure. It re-hashes the key
		 * and places it into the correct bucket and region (lo or hi) in the new arrays.
		 * This method does not check for existing keys, assuming all keys are new in the
		 * target structure during a resize operation.
		 *
		 * @param key The primitive key to copy.
		 */
		private void copy( int key ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from the bucket
			
			int dst_index; // Destination index for the key
			
			
			if( index == -1 ) // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				// Collision occurred. Place new entry in {@code lo Region}
				if( links.length == _lo_Size ) // If lo_Size exceeds links array capacity
					links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) ); // Resize links
				links[ dst_index = _lo_Size++ ] = ( int ) index; // New entry points to the old head
			}
			
			keys[ dst_index ]       = ( int ) key; // Store the key
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 ); // Update bucket to new head (1-based)
		}
		
		/**
		 * Creates and returns a deep copy of this read-write set.
		 * Overrides the base class clone to ensure the correct runtime type is returned.
		 *
		 * @return A cloned instance of this RW set.
		 * @throws InternalError if cloning fails.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		
	}
}