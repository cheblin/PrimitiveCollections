package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.Arrays;
import java.util.ConcurrentModificationException;

/**
 * A specialized map for mapping primitive keys to primitive values.
 * <p>
 * This map uses an internal hash table structure optimized for performance and memory
 * efficiency, particularly when dealing with non-negative primitive keys. It distinguishes
 * between two primary regions for storing entries:
 *
 * <ul>
 * <li><b>{@code lo Region}:</b> Stores entries involved in hash collisions.
 *     Occupies low indices (0 to {@code _lo_Size-1}) in the internal key and value arrays.
 *     Uses an explicit {@code links} array for chaining collision entries.
 *     Entries in this region can be the head or part of a collision chain.</li>
 *
 * <li><b>{@code hi Region}:</b> Stores entries that do not (initially) require explicit chain links.
 *     These are typically entries inserted into an empty hash bucket, or they serve as terminal nodes of collision chains.
 *     Occupies high indices (from {@code keys.length - _hi_Size} to {@code keys.length-1}) in the internal key and value arrays.
 *     This region effectively grows downwards from {@code keys.length-1}.</li>
 * </ul>
 *
 * <h3>Insertion Algorithm (Hash Mode):</h3>
 * <ol>
 * <li>Compute the bucket index using the key's hash.</li>
 * <li><b>If the target bucket is empty:</b>
 *     <ul><li>The new entry is placed in the {@code hi Region}. The bucket is updated to point to this new entry.</li></ul></li>
 * <li><b>If the target bucket is not empty:</b>
 *     <ul>
 *         <li>The map checks if the new key matches the key of the existing entry at the head of the chain. If so, the value is updated.</li>
 *         <li>If the new key collides (does not match) with the existing entry:
 *             <ul>
 *                 <li>The new entry is placed in the {@code lo Region}.</li>
 *                 <li>This new {@code lo Region} entry becomes the new head of the chain, pointing to the previous head.</li>
 *                 <li>If the previous head was in the {@code hi Region}, it now becomes a terminal node in a {@code lo Region} chain.</li>
 *                 <li>If the previous head was already in the {@code lo Region}, the existing chain is traversed. If the key is found, its value is updated. Otherwise, the new entry is added to the {@code lo Region} and becomes the new head of the chain.</li>
 *             </ul>
 *         </li>
 *     </ul>
 * </li>
 * </ol>
 *
 * <h3>Removal Algorithm (Hash Mode):</h3>
 * Removal involves finding the entry, updating chain links or bucket pointers, and then compacting the
 * respective region ({@code lo} or {@code hi}) to maintain memory density.
 *
 * <ul>
 * <li><b>Removing an entry from the {@code hi Region}:</b>
 *     <ul>
 *         <li>If the entry is found, the bucket pointing to it is cleared.</li>
 *         <li>To compact the {@code hi Region}, the entry from the lowest-addressed slot in the {@code hi Region}
 *             is moved into the vacated slot.</li>
 *         <li>The size of the {@code hi Region} is decremented.</li>
 *     </ul>
 * </li>
 * <li><b>Removing an entry from the {@code lo Region}:</b>
 *     <ul>
 *         <li><b>Case 1: Entry is the head of its collision chain:</b> The bucket is updated to point to the next entry in the chain.</li>
 *         <li><b>Case 2: Entry is within or at the end of its collision chain (not the head):</b>
 *             <ul><li>The link of the preceding entry in the chain is updated to bypass the removed entry.</li>
 *                 <li><b>Special Optimization:</b> If a {@code lo Region} chain terminates at a {@code hi Region} entry, and that {@code hi Region} entry is being removed, the {@code lo Region} entry that pointed to it is effectively "moved" into the {@code hi Region} slot. This optimizes compaction and chain restructuring.</li>
 *             </ul>
 *         </li>
 *         <li>After chain adjustments, the {@code lo Region} is compacted:
 *             The data from the last logical entry in the {@code lo Region} is moved into the freed slot.
 *             Any pointers (bucket or links from other entries) to the moved entry's original location are updated to its new location.
 *             The size of the {@code lo Region} is decremented. This ensures the {@code lo Region} remains dense.
 *         </li>
 *     </ul>
 * </li>
 * </ul>
 *
 * <h3>Iteration ({@code unsafe_token}) (Hash Mode):</h3>
 * Iteration over non-null key entries proceeds by scanning the {@code lo Region} first, then the {@code hi Region}:
 * Due to compaction, all entries in these ranges are valid and can be accessed very quickly.
 * <ul>
 *   <li>1. Iterate through indices from `token + 1` up to `_lo_Size - 1` (inclusive).</li>
 *   <li>2. If the {@code lo Region} scan is exhausted (i.e., `token + 1 >= _lo_Size`), iteration continues by scanning the {@code hi Region} from its logical start (`keys.length - _hi_Size`) up to `keys.length - 1` (inclusive).</li>
 * </ul>
 *
 * <h3>Resizing (Hash to Hash):</h3>
 * <ul>
 * <li>Allocate new internal arrays ({@code _buckets}, {@code keys}, {@code values}, {@code links}) with the new capacity.</li>
 * <li>Iterate through all valid entries in the old {@code lo Region} and old {@code hi Region}.</li>
 * <li>Re-insert each key-value pair into the new structure, efficiently re-hashing and re-inserting entries to rebuild the hash table and regions.</li>
 * </ul>
 */
public interface FloatUShortNullMap {
	
	// Inner abstract class for read-only map operations and common fields.
	// This design allows sharing common logic and state between read-only and read-write implementations.
	abstract class R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Flag indicating if the conceptual null key is currently present in the map.
		 */
		protected boolean                hasNullKey;
		/**
		 * Flag indicating if the conceptual null key maps to a non-null value.
		 * If `false`, the null key maps to a conceptual null value.
		 */
		protected boolean                nullKeyHasValue;
		/**
		 * Stores the primitive value associated with the conceptual null key.
		 * This value is only meaningful if {@link #nullKeyHasValue} is {@code true}.
		 */
		protected char            nullKeyValue;
		/**
		 * Hash table buckets. Each element stores a 1-based index (or 0 if empty)
		 * pointing to the entry in the hi region or  head of a collision chain in the lo region.
		 * An index of 0 means the bucket is empty.
		 */
		protected int[]                  _buckets;
		/**
		 * Stores collision chain links for entries in the {@code lo Region}.
		 * {@code links[i]} contains the index of the next entry in the chain that can be in the {@code hi Region} if it is a terminal entry
		 */
		protected int[]                  links;
		/**
		 * Array storing the actual primitive keys of the map entries.
		 * Keys for the  lo Region are stored from index 0 up to {@code _lo_Size-1}.
		 * Keys for the  hi Region are stored from {@code keys.length - _hi_Size} up to {@code keys.length-1}.
		 */
		protected float[]          keys;
		
		/**
		 * Array storing the actual primitive values of the map entries.
		 * Values are stored at the same indices as their corresponding keys.
		 */
		protected char[] values;
		/**
		 * Bitset array indicating whether a value at a given index is non-null.
		 * Each bit corresponds to an entry index in `keys` and `values` arrays.
		 * If `nulls[i / 64]` has the `(i % 64)`-th bit set, the value at `values[i]` is non-null.
		 * Otherwise, it is conceptually null (i.e., values[i] holds a default primitive value).
		 */
		protected long[]        nulls;
		
		
		/**
		 * Returns the total number of non-null key-value mappings in the map's internal data structures.
		 * This count excludes the conceptual null key, if present.
		 *
		 * @return The number of entries for non-null keys.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * Version counter for detecting concurrent structural modifications to the map.
		 * Incremented on operations that change the map's structure (e.g., put, remove, resize, clear).
		 * Used by iterators to ensure consistency.
		 */
		protected int _version;
		
		/**
		 * The current number of entries stored in the {@code lo Region} (collision-involved entries).
		 * These entries are stored at indices from 0 up to {@code _lo_Size-1}.
		 */
		protected int _lo_Size;
		/**
		 * The current number of entries stored in the {@code hi Region} (non-collision entries or terminal chain nodes).
		 * These entries are stored at indices from {@code keys.length - _hi_Size} up to {@code keys.length-1}.
		 */
		protected int _hi_Size;
		
		/**
		 * Shift amount for encoding the version into the higher bits of a {@code long} token.
		 * This allows packing both the version and the index into a single long.
		 */
		protected static final int VERSION_SHIFT = 32;
		
		/**
		 * A special internal index used to represent the conceptual null key within tokens.
		 * This index is outside the valid range of `keys` array indices.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * A constant representing an invalid or non-existent token.
		 * Used as a sentinel value for methods returning tokens.
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
		 * This includes the conceptual null key if present.
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
		 * Returns the total number of key-value mappings in this map.
		 * This is an alias for {@link #size()}.
		 *
		 * @return The number of entries in the map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the internal storage arrays for keys and values.
		 *
		 * @return The current capacity of the map's internal arrays. Returns 0 if the map is uninitialized.
		 */
		public int length() {
			return
					keys == null ?
					0 :
					keys.length;
		}
		
		
		/**
		 * Checks if the map contains a mapping for the specified boxed key.
		 * This method handles {@code null} keys.
		 *
		 * @param key The boxed key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean containsKey(  Float     key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean containsKey( float key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Checks if this map contains one or more keys mapped to the specified boxed value.
		 * This method handles searching for both non-null values and conceptual null values.
		 *
		 * @param value The boxed value to search for (can be null, representing a conceptual null value).
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
						if( ( nulls[ nulls_idx ] | -1L << remaining ) == -1 ) break;  // Set bits beyond _lo_Size to 1
						else return true;
					
					if( nulls[ nulls_idx ] != -1L ) return true; // Found an unset bit (null value)
				}
			
			// Skip hi region if empty
			if( _hi_Size == 0 ) return false;
			
			// Scan hi region (indices keys.length - _hi_Size to keys.length-1)
			int hi_start  = keys.length - _hi_Size;
			int nulls_idx = hi_start >> 6;
			long bits = nulls[ nulls_idx ]; // Get the first 64-bit segment
			
			int bit = hi_start & 63; // Starting bit in the first segment
			if( 0 < bit ) bits &= -1L << bit; // Clear bits before hi_start
			
			// Process all hi region segments, including the first
			for( int remaining; ; bits = nulls[ ++nulls_idx ] ) {
				// Mask bits beyond keys.length in the last partial segment
				if( nulls_idx == nulls.length )
					if( 0 < ( remaining = keys.length & 63 ) ) // Bits in the last segment (0 if keys.length is 64-bit aligned)
						return ( nulls[ nulls_idx ] | -1L << remaining ) != -1;  // Set bits beyond _lo_Size to 1
				
				if( bits != -1L ) return true; // Found an unset bit (null value)
			}
		}
		
		/**
		 * Checks if the map contains one or more keys mapped to the specified primitive value.
		 * Checks both non-null key mappings and the null key mapping (if present and has a non-null value).
		 *
		 * @param value The primitive value to search for.
		 * @return {@code true} if the value is found, {@code false} otherwise.
		 */
		public boolean containsValue( char value ) {
			if( hasNullKey && nullKeyHasValue && nullKeyValue == value ) return true;
			
			if( _count() == 0 ) return false;
			
			int  nulls_idx = 0;
			long bits      = 0;
			
			// Scan lo region (0 to _lo_Size-1)
			for( int i = 0; i < _lo_Size; i++ ) {
				if( nulls_idx != i >> 6 ) bits = nulls[ nulls_idx = i >> 6 ];
				if( ( bits & 1L << i ) != 0 && values[ i ] == value ) return true;
			}
			
			// Scan hi region (keys.length - _hi_Size to keys.length-1)
			for( int i = keys.length - _hi_Size; i < keys.length; i++ ) {
				if( nulls_idx != i >> 6 ) bits = nulls[ nulls_idx = i >> 6 ];
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
		 * @return A {@code long} token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 */
		public long tokenOf(  Float     key ) {
			return key == null ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       INVALID_TOKEN :
			       tokenOf( ( float ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a "token" representing the internal location of the specified primitive key.
		 * This token can be used for fast access to the key and its value via {@link #key(long)} and {@link #value(long)}.
		 * It also includes a version stamp to detect concurrent modifications.
		 *
		 * @param key The primitive key to get the token for.
		 * @return A {@code long} token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if a concurrent structural modification is detected while traversing a collision chain, leading to an unexpectedly long chain traversal.
		 */
		public long tokenOf( float key ) {
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			int index = _buckets[ bucketIndex( Array.hash( key ) ) ] - 1;
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
		 * Returns the first valid token for iterating over map entries.
		 * <p>
		 * Iteration order prioritizes non-null keys (from {@code lo Region} then {@code hi Region}),
		 * followed by the conceptual null key if present.
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
			if( _count() == 0 || keys == null || keys.length - 1 <= token ) return -1;
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
		 * Checks if the map contains a special mapping for the conceptual null key.
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
		 * Returns the primitive value associated with the conceptual null key.
		 * <p>
		 * <b>Precondition:</b> This method should only be called if {@link #hasNullKey()} returns {@code true}
		 * and preferably {@link #nullKeyHasValue()} returns {@code true}.
		 * If {@link #nullKeyHasValue()} is {@code false}, the returned value is conceptually null
		 * (often 0 for primitive values) and should be interpreted as such.
		 *
		 * @return The primitive value associated with the null key.
		 */
		public char nullKeyValue() { return  nullKeyValue; }
		
		
		/**
		 * Checks if the given token represents the conceptual null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the primitive key associated with the given token.
		 *
		 * @param token The token obtained from {@link #tokenOf} or iteration methods.
		 *              Must not be {@link #INVALID_TOKEN} or represent the conceptual null key.
		 * @return The primitive key.
		 * @throws IllegalArgumentException if the token is invalid or represents the null key.
		 */
		public float key( long token ) {
			return ( float )  keys[ index( token ) ];
		}
		
		/**
		 * Checks if the entry associated with the given token has a non-null value.
		 * Handles both regular tokens and the special null key token.
		 *
		 * @param token The token to check (can be for the conceptual null key).
		 * @return {@code true} if the token is valid and the corresponding entry maps to a non-null value, {@code false} otherwise (including if the value is conceptually null or the token is invalid/expired).
		 */
		public boolean hasValue( long token ) {
			return isKeyNull( token ) ?
			       nullKeyHasValue :
			       ( nulls[ index( token ) >> 6 ] & 1L << index( token ) ) != 0;
		}
		
		/**
		 * Returns the primitive value associated with the specified token.
		 * <p>
		 * <b>Precondition:</b> This method should only be called if {@link #hasValue(long)} returns {@code true} for the given token.
		 * <p>
		 * The result is undefined if the token is {@link #INVALID_TOKEN} or has expired due to structural modification.
		 *
		 * @param token The token for which to retrieve the value.
		 * @return The primitive value associated with the token.
		 */
		public char value( long token ) {
			return ( isKeyNull( token ) ?
			                 nullKeyValue :
			                   values[ index( token ) ] );
		}
		
		
		/**
		 * Computes a hash code for this map.
		 * The hash code is calculated based on all key-value pairs, including the conceptual null key if present.
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
			    hasNullKey && nullKeyValue != other.nullKeyValue || size() != other.size() )
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
				
				if( _buckets != null ) cloned._buckets = _buckets.clone();
				if( links != null ) cloned.links = links.clone();
				if( keys != null ) cloned.keys = keys.clone();
				if( values != null ) cloned.values = values.clone();
				if( nulls != null ) cloned.nulls = nulls.clone();
				
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
		 * The conceptual null key is represented as "null" key.
		 * Primitive keys are represented as their direct value.
		 *
		 * @param json The {@code JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     json.name( keys[ token ] ).value( hasValue( token ) ?
			                                       value( token ) :
			                                       null );
			
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
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index; }
		
		
		/**
		 * Extracts the 0-based internal array index from a given {@code long} token.
		 *
		 * @param token The {@code long} token.
		 * @return The 0-based index.
		 */
		protected int index( long token ) { return ( int ) token; }
		
		
		/**
		 * Extracts the version stamp from a given {@code long} token.
		 *
		 * @param token The {@code long} token.
		 * @return The version stamp.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
	}
	
	
	/**
	 * Concrete implementation providing read-write functionalities for the map.
	 * This class allows modification of the map's contents.
	 */
	class RW extends R {
		
		
		/**
		 * Constructs an empty RW map with a default initial capacity.
		 */
		public RW() { this( 0 ); }
		
		
		/**
		 * Constructs an empty RW map with the specified initial capacity.
		 *
		 * @param capacity The initial capacity of the map. If 0, the map starts uninitialized.
		 */
		public RW( int capacity ) {
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		
		/**
		 * Initializes or re-initializes the internal arrays based on the specified capacity
		 *
		 * @param capacity The desired capacity for the new internal arrays.
		 * @return The actual allocated capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			
			_buckets = new int[ capacity ];
			links    = new int[ Math.min( 16, capacity ) ];
			_lo_Size = 0;
			keys     = new float[ capacity ];
			values   = new char[ capacity ];
			nulls    = new long[ capacity + 63 >> 6 ];
			
			_hi_Size = 0;
			return length();
		}
		
		/**
		 * Associates the conceptual null key with a conceptual null value.
		 * If the conceptual null key was not previously present, it is added.
		 * If it was present and had a non-null value, that value is cleared (set to conceptual null).
		 *
		 * @return {@code true} if the map was structurally modified (e.g., null key added, or its value status changed), {@code false} otherwise.
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
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 * Handles null boxed values gracefully (associates the null key with a conceptual null value if input `value` is null).
		 *
		 * @param value The boxed value to associate with the conceptual null key (can be null).
		 * @return {@code true} if the map state was changed (insert or update), {@code false} otherwise.
		 */
		public boolean put(  Character value ) {
			return value == null ?
			       put() :
			       put( value );
		}
		
		/**
		 * Associates the specified boxed value with the specified boxed key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles {@code null} keys and {@code null} values.
		 *
		 * @param key   The boxed key (can be null).
		 * @param value The boxed value (can be null).
		 * @return {@code true} if the map state was changed (new key added or existing key's value updated to a different value status).
		 */
		public boolean put(  Float     key,  Character value ) {
			return key == null ?
			       value == null ?
			       put() :
			       putNullKey( value ) :
			       value == null ?
			       putNullValue( ( float ) ( key + 0 ) ) :
			       put( ( float ) ( key + 0 ), ( char ) ( value + 0 ), true );
		}
		
		/**
		 * Associates the specified boxed value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles null boxed values.
		 *
		 * @param key   The primitive key.
		 * @param value The boxed value to associate with the key (can be null).
		 * @return {@code true} if the map state was changed (new key added or existing key's value updated to a different value status).
		 */
		public boolean put( float key,  Character value ) {
			return value == null ?
			       putNullValue( ( float ) ( key + 0 ) ) :
			       put( ( float ) ( key + 0 ), ( char ) ( value + 0 ), true );
		}
		
		
		/**
		 * Associates the specified primitive value with the conceptual null key in this map.
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value The primitive value to associate.
		 * @return {@code true} if the map was structurally modified (e.g., null key added), {@code false} otherwise.
		 */
		public boolean put( char value ) {
			_version++;
			nullKeyHasValue = true;
			
			if( hasNullKey ) {
				nullKeyValue = ( char ) value;
				return false; // Key was not new, only value updated.
			}
			
			nullKeyValue = ( char ) value;
			return hasNullKey = true; // Null key was added.
		}
		
		
		/**
		 * Associates a value with the conceptual null key in this map.
		 * <p>
		 * If the map previously contained a mapping for the null key, the old value is replaced.
		 *
		 * @param value    The primitive value to be associated with the null key.
		 * @param hasValue {@code true} if `value` is a non-null value, {@code false} if it represents a conceptual null value.
		 * @return {@code true} if the map was structurally modified as a result of this operation (e.g., null key added), {@code false} otherwise.
		 */
		private boolean put( char value, boolean hasValue ) {
			boolean b = !hasNullKey; // Check if the null key is being added for the first time.
			
			hasNullKey = true;
			if( nullKeyHasValue = hasValue ) nullKeyValue = ( char ) value;
			_version++;
			return b;
		}
		
		/**
		 * Associates the specified primitive value with the conceptual null key in this map.
		 *
		 * @param value The primitive value to associate.
		 * @return {@code true} if the map state was changed (insert or update).
		 */
		public boolean putNullKey( char  value ) { return put( value, true ); }
		
		/**
		 * Associates a conceptual null value with the specified primitive key.
		 * If the key already exists, its value is set to conceptual null. If not, the key is added with a conceptual null value.
		 *
		 * @param key The primitive key.
		 * @return {@code true} if the map state was changed (new key added or existing key's value status changed).
		 */
		public boolean putNullValue( float key ) { return put( key, ( char ) 0, false ); }
		
		/**
		 * Associates the specified primitive value with the specified primitive key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The primitive key.
		 * @param value The primitive value to associate.
		 * @return {@code true} if the map state was changed (new key added or existing key's value updated to a different value).
		 */
		public boolean put( float key, char value ) { return put( key, value, true ); }
		
		/**
		 * Core insertion/update logic for non-null keys.
		 *
		 * @param key      The primitive key to insert or update.
		 * @param value    The primitive value to associate with the key.
		 * @param hasValue {@code true} if `value` represents a non-null value, {@code false} if it represents a conceptual null value.
		 * @return {@code true} if the map was structurally modified (new key added), {@code false} if an existing key was updated.
		 * @throws ConcurrentModificationException If potential hash chain corruption is detected.
		 */
		private boolean put( float key, char value, boolean hasValue ) {
			
			
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
					if( keys[ index ] == ( float ) key ) { // Key matches existing {@code hi Region} entry
						if( hasValue ) {// Update value
							values[ index ] = ( char ) value;
							nulls[ index >> 6 ] |= 1L << index;
						}
						else
							nulls[ index >> 6 ] &= ~( 1L << index );
						
						
						_version++;
						return false; // Key was not new
					}
				}
				else // Entry is in {@code lo Region} (collision chain)
					for( int next = index, collisions = 0; ; ) {
						if( keys[ next ] == key ) {
							if( hasValue ) {// Update value
								values[ next ] = ( char ) value;
								nulls[ next >> 6 ] |= 1L << next;
							}
							else
								nulls[ next >> 6 ] &= ~( 1L << next );
							
							_version++;
							return false;// Key was not new
						}
						if( _lo_Size <= next ) break; // Found terminal node (might be in hi or lo region)
						next = links[ next ];
						
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
				
				if( links.length == ( dst_index = _lo_Size++ ) )
					links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index; // Link the new entry to the previous head of the chain
			}
			
			keys[ dst_index ] = ( float ) key;
			if( hasValue ) {// Update value
				values[ dst_index ] = ( char ) value;
				nulls[ dst_index >> 6 ] |= 1L << dst_index;
			}
			else
				nulls[ dst_index >> 6 ] &= ~( 1L << dst_index );
			
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 ); // Update bucket to point to the new head (1-based index)
			_version++;
			return true; // Key was new
		}
		
		
		/**
		 * Removes the mapping for the specified boxed key from this map if present.
		 * Handles {@code null} boxed keys by delegating to {@link #removeNullKey()}.
		 *
		 * @param key The boxed key whose mapping is to be removed.
		 * @return {@code true} if the map contained a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean remove(  Float     key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( float ) ( key + 0 ) );
		}
		
		
		/**
		 * Removes the mapping for the conceptual null key from this map if present.
		 *
		 * @return {@code true} if the null key was present and removed, {@code false} otherwise.
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey      = false;
			nullKeyHasValue = false; // Also reset nullKeyHasValue as the null key is no longer present.
			_version++;
			return true;
		}
		
		/**
		 * Relocates an entry's data (key and value) from a source index ({@code src}) to a destination index ({@code dst})
		 * within the internal {@code keys} and {@code values} arrays.
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
			
			if( index == src ) _buckets[ bucketIndex ] = ( int ) ( dst + 1 ); // If src was the head of a chain
			else { // Find the preceding entry in the chain and update its link
				while( links[ index ] != src )
					index = links[ index ];
				
				links[ index ] = ( int ) dst;
			}
			
			// If the source was in the lo region, its link might be important for the new position.
			// Copy the link from `src` to `dst`.
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			// Copy the key and value data from `src` to `dst`.
			keys[ dst ] = keys[ src ];
			
			if( ( nulls[ src >> 6 ] & 1L << src ) != 0 ) {
				values[ dst ] = values[ src ];
				nulls[ dst >> 6 ] |= 1L << dst;
			}
			else
				nulls[ dst >> 6 ] &= ~( 1L << dst );
		}
		
		
		/**
		 * Removes the mapping for the specified primitive key from this map if present.
		 * <p>
		 * <h3>Hash Map Removal Logic (Sparse Mode):</h3>
		 * Removal involves finding the entry, updating chain links or bucket pointers, and then
		 * compacting the respective region ({@code lo} or {@code hi}) to maintain memory density.
		 * <ol>
		 * <li><b>Find entry:</b> Locate the entry corresponding to the key by traversing the hash bucket's chain.</li>
		 * <li><b>Handle 'hi region' removal:</b>
		 *     <ul>
		 *         <li>If the found entry is in the {@code hi Region} and is the head of its bucket (meaning no collision chain),
		 *             the bucket pointer is cleared (set to 0).</li>
		 *         <li>To compact the {@code hi Region}, the entry from the "bottom" of the {@code hi Region}
		 *             (at {@code keys[keys.length - _hi_Size]}) is moved into the freed {@code removeIndex} slot using {@link #move(int, int)}.
		 *             {@code _hi_Size} is then decremented.</li>
		 *     </ul>
		 * </li>
		 * <li><b>Handle 'lo region' removal (and associated 'hi region' entries):</b>
		 *     <ul>
		 *         <li><b>Case A: Entry is the head of its chain:</b> The bucket pointer {@code _buckets[removeBucketIndex]}
		 *             is updated to point to the next entry in the chain ({@code links[removeIndex]}).</li>
		 *         <li><b>Case B: Entry is within or at the end of its chain:</b> The {@code links} link of the
		 *             preceding entry in the chain is updated to bypass the removed entry.</li>
		 *         <li><b>Special Optimization:</b>
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
		 *             All pointers (bucket or {@code links} links) to the moved entry's original position
		 *             are updated to its new location. {@code _lo_Size} is decremented.</li>
		 *     </ul>
		 * </li>
		 * </ol>
		 *
		 * @param key The primitive key whose mapping is to be removed.
		 * @return {@code true} if the map contained a mapping for the specified key, {@code false} otherwise.
		 * @throws ConcurrentModificationException if an internal state inconsistency is detected during collision chain traversal (e.g., an unexpectedly long chain or malformed links).
		 */
		public boolean remove( float key ) {
			
			
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
			
			int next = links[ removeIndex ];
			if( keys[ removeIndex ] == key ) _buckets[ removeBucketIndex ] = ( int ) ( next + 1 ); // If removed entry was head, update bucket
			else {
				int last = removeIndex;
				if( keys[ removeIndex = next ] == key )// The key is found at 'SecondNode'
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];// 'SecondNode' is in 'lo Region', relink to bypass 'SecondNode'
					else {  // 'SecondNode' is in the hi Region (it's a terminal node)
						// This is the special optimization case: moving a lo entry to a hi slot
						keys[ removeIndex ] = keys[ last ]; // Copies the key from `last` to `removeIndex`
						
						if( ( nulls[ last >> 6 ] & 1L << last ) != 0 ) { // If `last` has a non-null value.
							values[ removeIndex ] = values[ last ];
							nulls[ removeIndex >> 6 ] |= 1L << removeIndex; // Set bit for new location.
						}
						else
							nulls[ removeIndex >> 6 ] &= ~( 1L << removeIndex ); // Clear bit for new location.
						
						
						// Update the bucket for this chain.
						// 'removeBucketIndex' is the hash bucket for the original 'key' (which was keys[T]).
						// Since keys[P] and keys[T] share the same bucket index, this is also the bucket for keys[P].
						// By pointing it to 'removeIndex' (which now contains keys[P]), we make keys[P] the new sole head.
						_buckets[ removeBucketIndex ] = ( int ) ( removeIndex + 1 );
						removeIndex                   = last; // Mark `last` (the original lo entry's position) for compaction
					}
				else if( _lo_Size <= removeIndex ) return false; // Not found, and reached end of hi-region chain
				else
					for( int collisions = 0; ; ) {
						int prev = last;
						
						if( keys[ removeIndex = links[ last = removeIndex ] ] == key ) {
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else {
								// Another special optimization case: moving a lo entry to a hi slot, not head of chain
								keys[ removeIndex ] = keys[ last ];
								if( ( nulls[ last >> 6 ] & 1L << last ) != 0 ) { // If `last` has a non-null value.
									values[ removeIndex ] = values[ last ];
									nulls[ removeIndex >> 6 ] |= 1L << removeIndex; // Set bit for new location.
								}
								else
									nulls[ removeIndex >> 6 ] &= ~( 1L << removeIndex ); // Clear bit for new location.
								
								links[ prev ] = ( int ) removeIndex;
								removeIndex   = last; // Mark original lo entry's position for compaction
							}
							break;
						}
						if( _lo_Size <= removeIndex ) return false; // Not found, reached end of hi-region chain
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
			}
			
			// Compact the lo region by moving the last entry into the freed slot
			move( _lo_Size - 1, removeIndex );
			_lo_Size--;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes all mappings from this map.
		 * The map will be empty after this call returns.
		 * The internal arrays are reset to their initial state or cleared, but not deallocated,
		 * maintaining their allocated capacity.
		 */
		public void clear() {
			_version++;
			
			hasNullKey      = false;
			nullKeyHasValue = false; // Ensure null key status is reset
			
			if( _count() == 0 ) return; // If already empty, no structural change needed
			if( _buckets != null ) Arrays.fill( _buckets, ( int ) 0 ); // Clear buckets
			_lo_Size = 0;
			_hi_Size = 0;
			Arrays.fill( nulls, 0 );
		}
		
		
		/**
		 * Ensures that this map can hold at least the specified number of entries without immediate resizing.
		 * If the current capacity is less than the specified capacity, the map is resized to accommodate it.
		 * If the map is uninitialized (i.e., `_buckets` is null), it will be initialized.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new actual capacity of the map's internal arrays.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			return _buckets == null ?
			       initialize( capacity ) :
			       resize( Array.prime( capacity ) );
		}
		
		
		/**
		 * Trims the capacity of the map's internal arrays to reduce memory usage.
		 * The capacity will be reduced to the current size of the map, or a prime number
		 * slightly larger than the size, ensuring sufficient space for future additions.
		 */
		public void trim() { trim( size() ); }
		
		/**
		 * Trims the capacity of the map's internal arrays to a specified minimum capacity.
		 * If the current capacity is greater than the specified capacity (after accounting for actual size),
		 * the map is resized. The actual new capacity will be a prime number at least {@code capacity} and
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
		 * This involves creating new arrays and re-hashing all existing entries into the new structure.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resize.
		 */
		private int resize( int newSize ) {
			_version++;
			
			float[] old_keys    = keys;
			long[]        old_nulls   = nulls;
			char[] old_values  = values;
			int           old_lo_Size = _lo_Size;
			int           old_hi_Size = _hi_Size;
			initialize( newSize ); // Initialize new arrays with the new size
			
			long bits = 0;
			int  b    = 0;
			
			
			// Copy entries from the old lo Region to the new structure
			for( int i = 0; i < old_lo_Size; i++ )
				if( ( ( i < b << 6 ?
				        bits :
				        ( bits = old_nulls[ b++ >> 6 ] ) ) & 1L << i ) != 0 )
					copy( ( float ) old_keys[ i ], ( char ) old_values[ i ], true );
				else
					copy( ( float ) old_keys[ i ], ( char ) 0, false );
			
			
			// Copy entries from the old hi Region to the new structure
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
				if( ( ( i < b << 6 ?
				        bits :
				        ( bits = old_nulls[ b++ >> 6 ] ) ) & 1L << i ) != 0 )
					copy( ( float ) old_keys[ i ], ( char ) old_values[ i ], true );
				else
					copy( ( float ) old_keys[ i ], ( char ) 0, false );
			
			return length();
		}
		
		/**
		 * Copies a key-value pair into the new hash table structure during a resize operation.
		 * This method handles the re-hashing and placement of the entry into the appropriate bucket
		 * and region ({@code hi Region} or {@code lo Region} with linking) within the newly sized arrays.
		 *
		 * @param key      The key to copy.
		 * @param value    The value to copy.
		 * @param hasValue {@code true} if `value` represents a non-null value, {@code false} if it represents a conceptual null value.
		 */
		private void copy( float key, char value, boolean hasValue ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1; // Get the current head of the chain (1-based to 0-based)
			int dst_index;
			
			if( index == -1 ) // Bucket is empty in the new structure
				dst_index = keys.length - 1 - _hi_Size++; // Place in hi Region
			else { // Collision in the new structure
				if( links.length == _lo_Size ) links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) );
				links[ dst_index = _lo_Size++ ] = ( int ) index; // Place in lo Region and link to previous head
			}
			
			keys[ dst_index ] = key;
			if( hasValue ) {
				values[ dst_index ] = value;
				nulls[ dst_index >> 6 ] |= 1L << ( dst_index & 63 ); // Set bit for non-null value
			}
			
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 ); // Update bucket to point to the new head
		}
		
		/**
		 * Creates and returns a deep copy of this {@code RW} map.
		 *
		 * @return A cloned instance of this map.
		 * @throws InternalError if cloning fails (should not happen as Cloneable is supported).
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		
	}
}