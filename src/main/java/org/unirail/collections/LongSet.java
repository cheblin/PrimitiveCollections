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
 * A specialized set for storing primitive `int` keys, using a memory-efficient,
 * dual-region open-addressing hash table. This implementation is optimized for
 * performance by separating entries based on their collision status.
 *
 * <h3>Dual-Region Hashing Strategy</h3>
 * The core of this set is a single `keys` array divided into two logical regions
 * to store entries, minimizing memory overhead, especially for the `links` array
 * used for collision chaining.
 *
 * <ul>
 * <li><b>{@code hi Region}:</b> Occupies the high-indexed end of the {@code keys} array.
 *     It stores entries that, at insertion time, map to an empty hash bucket.
 *     This region grows downwards from `keys.length - 1`. Entries in this region
 *     do not initially require a slot in the `links` array, saving memory. They can,
 *     however, become the terminal node of a collision chain originating in the `lo Region`.</li>
 *
 * <li><b>{@code lo Region}:</b> Occupies the low-indexed start of the {@code keys} array.
 *     It stores entries that are part of a collision chain. This includes newly
 *     inserted keys that hash to an already occupied bucket. Every entry in this region
 *     has a corresponding slot in the `links` array to point to the next entry in its
 *     collision chain. This region grows upwards from index `0`.</li>
 * </ul>
 *
 * <h3>Insertion</h3>
 * When a new key is added:
 * <ol>
 * <li>Its hash is computed to find a bucket in the {@code _buckets} table.</li>
 * <li><b>If the bucket is empty:</b> The new key is added to the {@code hi Region}. The bucket
 *     is updated to point to this new entry.</li>
 * <li><b>If the bucket is occupied (a collision):</b>
 *     <ul>
 *     <li>The existing chain is checked for duplicates.</li>
 *     <li>If the key is new, it is added to the {@code lo Region} and becomes the
 *         new head of the collision chain. Its link in the `links` array is set to
 *         point to the previous chain head, and the bucket is updated to point to
 *         this new entry in the `lo Region`.</li>
 *     </ul>
 * </li>
 * </ol>
 *
 * <h3>Removal</h3>
 * When a key is removed:
 * <ol>
 * <li>The entry is located in either the `lo` or `hi` region.</li>
 * <li>The collision chain is repaired by updating the bucket pointer or the predecessor's
 *     link in the `links` array to bypass the removed entry.</li>
 * <li>To maintain density and enable fast iteration, the region is compacted. The last
 *     logical entry from the affected region (`lo` or `hi`) is moved into the slot
 *     vacated by the removed entry. All pointers to the moved entry are updated to its new location.</li>
 * </ol>
 *
 * <h3>Iteration</h3>
 * Iteration is highly efficient because both regions are kept dense. The iterator first
 * scans the `lo Region` from index `0` to `_lo_Size - 1`, and then scans the `hi Region`
 * from `keys.length - _hi_Size` to `keys.length - 1`.
 */
public interface LongSet {
	
	/**
	 * A read-only abstract base class that implements the core functionalities and state management for
	 * the dual-region hash set. It provides methods for querying the set, iteration, and serialization
	 * without allowing modification.
	 *
	 * @see IntSet for a detailed explanation of the internal dual-region hashing strategy.
	 */
	abstract class R implements JsonWriter.Source, Cloneable {
		/**
		 * Returns `true` if this set contains the null key. The null key is handled
		 * separately from other keys.
		 *
		 * @return `true` if the null key is present in the set.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Tracks whether the null key is present in the set.
		 */
		protected boolean       hasNullKey;
		/**
		 * The hash table. Stores 1-based indices into the `keys` array, where `_buckets[i] - 1`
		 * is the 0-based index of the head of a collision chain. A value of `0` indicates an empty bucket.
		 */
		protected int[]         _buckets;
		/**
		 * An array used for collision chaining. For an entry at index `i` in the `lo Region` of the `keys` array,
		 * `links[i]` stores the 0-based index of the next element in the same collision chain.
		 */
		protected int[]         links;
		/**
		 * Stores the primitive keys of the set. This array is logically divided into a `lo Region`
		 * (for collision-involved entries) and a `hi Region` (for non-colliding entries).
		 */
		protected long[] keys;
		
		/**
		 * The number of active entries in the `lo Region`, which occupies indices from `0` to `_lo_Size - 1`
		 * in the `keys` and `links` arrays.
		 */
		protected int _lo_Size;
		/**
		 * The number of active entries in the `hi Region`, which occupies indices from `keys.length - _hi_Size`
		 * to `keys.length - 1` in the `keys` array.
		 */
		protected int _hi_Size;
		
		/**
		 * Returns the total number of non-null entries currently stored in the set.
		 * This is the sum of entries in the low and high regions.
		 *
		 * @return The current count of non-null entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * A version counter used for fail-fast iteration. It is incremented upon any
		 * structural modification to the set (add, remove, clear, resize).
		 */
		protected int _version;
		
		/**
		 * The number of bits to shift the version value when packing it into a `long` token.
		 */
		protected static final int VERSION_SHIFT  = 32;
		/**
		 * A special index value used in tokens to represent the null key. This value is outside
		 * the valid range of array indices.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * A constant representing an invalid or non-existent token, returned when a key is not found
		 * or at the end of an iteration.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		
		/**
		 * Returns the number of elements in this set (its cardinality), including the null key if present.
		 *
		 * @return the number of elements in this set.
		 */
		public int size() {
			return ( _count() ) + (
					hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the number of elements in this set. This is an alias for {@link #size()}.
		 *
		 * @return the number of elements in this set.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns {@code true} if this set contains no elements.
		 *
		 * @return {@code true} if this set contains no elements.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the total allocated capacity of the internal {@code keys} array.
		 * This represents the maximum number of non-null elements the set can hold
		 * before a resize is triggered.
		 *
		 * @return The current capacity.
		 */
		public int length() {
			return ( keys == null ?
			         0 :
			         keys.length );
		}
		
		
		/**
		 * Returns {@code true} if this set contains the specified boxed key.
		 *
		 * @param key the boxed key to check for (can be null).
		 * @return {@code true} if this set contains the specified key.
		 */
		public boolean contains(  Long      key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Returns {@code true} if this set contains the specified primitive key.
		 *
		 * @param key the primitive key to check for.
		 * @return {@code true} if this set contains the specified key.
		 */
		public boolean contains( long key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Returns a token for the specified boxed key if it exists in the set.
		 * If the key is not found, returns {@link #INVALID_TOKEN}. The null key is handled correctly.
		 *
		 * @param key the boxed key to find (can be null).
		 * @return a valid token if the key is in the set, otherwise {@link #INVALID_TOKEN}.
		 */
		public long tokenOf(  Long      key ) {
			return key == null ?
			       ( hasNullKey ?
			         token( NULL_KEY_INDEX ) :
			         INVALID_TOKEN ) :
			       tokenOf( ( long ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a "token" representing the location of the specified primitive key.
		 * This token can be used for fast key retrieval via {@link #key(long)} and includes a
		 * version stamp to detect concurrent modifications.
		 *
		 * @param key The primitive key to find.
		 * @return A {@code long} token for the key, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected while
		 *                                         traversing a collision chain, which may indicate
		 *                                         a concurrent modification.
		 */
		public long tokenOf( long key ) {
			
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			
			int index = ( _buckets[ bucketIndex( Array.hash( key ) ) ] ) - 1;
			if( index < 0 ) return INVALID_TOKEN;
			
			if( _lo_Size <= index )
				return keys[ index ] == key ?
				       token( index ) :
				       INVALID_TOKEN;
			
			for( int collisions = 0; ; ) {
				if( keys[ index ] == key ) return token( index );
				if( _lo_Size <= index ) break;
				index = links[ index ];
				if( _lo_Size < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		
		/**
		 * Returns the token for the first element in the iteration order.
		 * Subsequent elements can be accessed by passing the returned token to {@link #token(long)}.
		 *
		 * @return The token for the first element, or {@link #INVALID_TOKEN} if the set is empty.
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
		 * Returns the token for the element that follows the one specified by the given token.
		 * This method is used to iterate through the set.
		 * <p>
		 * Iteration order is: all elements in the `lo Region`, then all elements in the `hi Region`,
		 * and finally the null key, if present.
		 *
		 * @param token The current token from a previous call to {@link #token()} or {@link #token(long)}.
		 * @return The token for the next element, or {@link #INVALID_TOKEN} if there are no more elements.
		 * @throws IllegalArgumentException        if the input token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the set was structurally modified after the token was issued.
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
		 * Returns the next valid internal array index for iteration, starting from the given index.
		 * This low-level method implements the core iteration logic, scanning first the dense `lo Region`
		 * and then the dense `hi Region`. It does not handle the null key.
		 *
		 * @param token The current internal index, or -1 to start from the beginning.
		 * @return The next valid internal array index, or -1 if iteration is complete.
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
		 * Returns {@code true} if the given token represents the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the primitive key associated with the given token.
		 *
		 * @param token The token obtained from iteration or {@link #tokenOf}. Must not represent the null key.
		 * @return The primitive key at the token's location.
		 * @throws IllegalArgumentException if the token represents the null key.
		 */
		public long key( long token ) {
			if( index( token ) == NULL_KEY_INDEX ) throw new IllegalArgumentException( "Token represents a null key." );
			return ( long )  ( keys[ index( token ) ] );
		}
		
		/**
		 * Computes the hash code for this set. The hash code is derived from the hash codes
		 * of all keys in the set.
		 *
		 * @return the hash code value for this set.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				final int h = Array.mix( seed, Array.hash( keys[ token ] ) );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			if( hasNullKey ) {
				final int h = Array.hash( seed );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		/**
		 * A static seed used in `hashCode` calculation to improve hash distribution.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this set with the specified object for equality. Returns `true` if the
		 * object is also an `IntSet.R`, the two sets have the same size, and every key in this
		 * set is contained in the other set.
		 *
		 * @param obj the object to be compared for equality with this set.
		 * @return `true` if the specified object is equal to this set.
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		/**
		 * Compares this set with another {@code IntSet.R} for equality.
		 *
		 * @param other the other set to compare against.
		 * @return `true` if the sets contain the exact same keys.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null || other.size() != size() || other.hasNullKey != hasNullKey ) return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( !other.contains(
						( long ) ( keys[ token ] ) ) ) return false;
			return true;
		}
		
		/**
		 * Creates and returns a deep copy of this set. The internal arrays are cloned,
		 * making the new set independent of the original.
		 *
		 * @return A deep copy of this set.
		 * @throws InternalError if cloning fails, which should not happen.
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
		 * Returns a string representation of the set in JSON array format (e.g., `[1, 42, -5, null]`).
		 *
		 * @return A JSON string representing the set.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the contents of this set to a {@link JsonWriter} as a JSON array.
		 * The null key is written as JSON `null`.
		 *
		 * @param json the {@link JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 5 );
			json.enterArray();
			
			if( hasNullKey ) json.value();
			
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) json.value( keys[ token ] );
			json.exitArray();
		}
		
		
		/**
		 * Computes the bucket index for a given key's hash code.
		 *
		 * @param hash The hash code of a key.
		 * @return The index in the {@code _buckets} array.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Packs an internal array index and the current set version into a single `long` token.
		 * The version is stored in the high 32 bits and the index in the low 32 bits.
		 *
		 * @param index The 0-based index of the entry (or {@link #NULL_KEY_INDEX} for the null key).
		 * @return A versioned `long` token representing the entry.
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); }
		
		/**
		 * Extracts the 0-based internal array index from a token.
		 *
		 * @param token The `long` token.
		 * @return The 0-based index.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts the version stamp from a token.
		 *
		 * @param token The `long` token.
		 * @return The version stamp.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * A read-write implementation of the dual-region hash set, extending the read-only base class {@link R}.
	 * It provides methods for modifying the set, such as adding and removing elements, clearing the set,
	 * and managing its capacity.
	 */
	class RW extends R {
		/**
		 * A threshold related to capacity management. In this implementation, it does not trigger
		 * a strategy change, as only the dual-region hash set is implemented.
		 */
		protected static int flatStrategyThreshold = 0x7FFF;
		
		/**
		 * Constructs an empty set with a default initial capacity.
		 */
		public RW() { this( 0 ); }
		
		/**
		 * Constructs an empty set with a specified initial capacity hint.
		 *
		 * @param capacity The initial capacity hint. The set will be able to hold at least
		 *                 this many elements before needing to resize.
		 */
		public RW( int capacity ) { if( capacity > 0 ) initialize( Array.prime( capacity ) ); }
		
		
		/**
		 * Initializes or re-initializes the internal hash set arrays with the given capacity.
		 * This is a structural modification and increments the version counter.
		 *
		 * @param capacity The desired capacity for the new internal arrays.
		 * @return The actual allocated capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			
			_buckets = new int[ capacity ];
			links    = new int[ Math.min( 16, capacity ) ];
			_lo_Size = 0;
			keys     = new long[ capacity ];
			_hi_Size = 0;
			return length();
		}
		
		
		/**
		 * Adds the specified boxed key to this set if it is not already present.
		 *
		 * @param key the boxed key to add (can be null).
		 * @return {@code true} if the set did not already contain the key.
		 */
		public boolean add(  Long      key ) {
			return key == null ?
			       addNullKey() :
			       add( ( long ) ( key + 0 ) );
		}
		
		
		/**
		 * Adds the specified primitive key to this set if it is not already present.
		 * <p>
		 * If the key's hash maps to an empty bucket, the key is added to the `hi Region`.
		 * If it maps to an occupied bucket (a collision), the key is added to the `lo Region`
		 * and becomes the new head of that bucket's collision chain. The set is resized
		 * if its capacity is exceeded.
		 *
		 * @param key the primitive key to add.
		 * @return {@code true} if the set did not already contain the key.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected during
		 *                                         collision chain traversal.
		 */
		public boolean add( long key ) {
			if( _buckets == null ) initialize( 7 );
			else if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ) );
			
			int hash        = Array.hash( key );
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1;
			int dst_index;
			
			if( index == -1 )
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				if( _lo_Size <= index ) {
					if( keys[ index ] == ( long ) key ) {
						_version++;
						return false;
					}
				}
				else
					for( int next = index, collisions = 0; ; ) {
						if( keys[ next ] == key ) {
							_version++;
							return false;
						}
						if( _lo_Size <= next ) break;
						next = links[ next ];
						
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
				
				if( links.length == ( dst_index = _lo_Size++ ) ) links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index;
			}
			
			keys[ dst_index ]       = ( long ) key;
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
			_version++;
			return true;
		}
		
		
		/**
		 * Adds the null key to the set if it is not already present.
		 *
		 * @return {@code true} if the null key was added, {@code false} if it was already present.
		 */
		public boolean addNullKey() {
			if( hasNullKey ) return false;
			hasNullKey = true;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes the specified boxed key from this set if it is present.
		 *
		 * @param key the boxed key to remove (can be null).
		 * @return {@code true} if the set contained the specified key.
		 */
		public boolean remove(  Long      key ) {
			return key == null ?
			       removeNullKey() :
			       remove( ( long ) ( key + 0 ) );
		}
		
		
		/**
		 * Moves an entry from a source index to a destination index within the internal arrays.
		 * This is a key part of the compaction process during removal. After moving the key,
		 * it finds the pointer (either in `_buckets` or `links`) that referenced the source
		 * index and updates it to point to the new destination index, maintaining the
		 * integrity of the hash table and its collision chains.
		 *
		 * @param src The source index of the entry to move.
		 * @param dst The destination index for the entry.
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
		 * After unlinking the entry from its collision chain (if any), the data region
		 * (`lo` or `hi`) is compacted by moving the last logical element from that region
		 * into the slot vacated by the removed key. The `move` helper method ensures
		 * all pointers to the moved element are updated correctly.
		 *
		 * @param key the primitive key to remove.
		 * @return {@code true} if the set contained the specified key.
		 * @throws ConcurrentModificationException if a potential infinite loop is detected during
		 *                                         collision chain traversal.
		 */
		public boolean remove( long key ) {
			if( _count() == 0 ) return false;
			int removeBucketIndex = bucketIndex( Array.hash( key ) );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1;
			if( removeIndex < 0 ) return false;
			
			if( _lo_Size <= removeIndex ) {
				
				if( keys[ removeIndex ] != key ) return false;
				
				move( keys.length - _hi_Size, removeIndex );
				_hi_Size--;
				_buckets[ removeBucketIndex ] = 0;
				_version++;
				return true;
			}
			
			int next = links[ removeIndex ];
			if( keys[ removeIndex ] == key ) _buckets[ removeBucketIndex ] = ( int ) ( next + 1 );
			else {
				int last = removeIndex;
				if( keys[ removeIndex = next ] == key )
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
					else {
						keys[ removeIndex ] = keys[ last ];
						
						_buckets[ removeBucketIndex ] = ( int ) ( removeIndex + 1 );
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
			
			move( _lo_Size - 1, removeIndex );
			_lo_Size--;
			_version++;
			return true;
		}
		
		/**
		 * Removes the null key from this set if it is present.
		 *
		 * @return {@code true} if the null key was removed, {@code false} if it was not present.
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey = false;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes all elements from this set. The set will be empty after this call, but its
		 * allocated capacity will remain unchanged. This is a structural modification.
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
		 * Returns an array containing all of the primitive keys in this set.
		 * If the set contains the null key, it will be represented by `null_substitute`.
		 * The order of keys in the returned array is not guaranteed.
		 *
		 * @param dst             An array to store the keys in. If it is too small, a new array is allocated.
		 * @param null_substitute The primitive value to use in place of a null key.
		 * @return An array containing all the keys in this set.
		 */
		public long[] toArray( long[] dst, long null_substitute ) {
			int s = size();
			if( dst.length < s ) dst = new long[ s ];
			
			int index = 0;
			if( hasNullKey ) dst[ index++ ] = null_substitute;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     dst[ index++ ] = ( long )  ( keys[ token ] );
			
			return dst;
		}
		
		
		/**
		 * Ensures that this set can hold at least the specified number of non-null elements
		 * without needing to resize. If necessary, the internal arrays are resized.
		 * This is a structural modification if a resize occurs.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new capacity of the set.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			return _buckets == null ?
			       initialize( capacity ) :
			       resize( Array.prime( capacity ) );
		}
		
		/**
		 * Trims the capacity of this set's internal arrays to be the set's current size.
		 * This can be used to minimize the memory footprint of the set. This is a structural
		 * modification if the capacity is reduced.
		 */
		public void trim() { trim( size() ); }
		
		
		/**
		 * Trims the capacity of this set's internal arrays to the specified capacity,
		 * provided it is not less than the current size. This is a structural modification
		 * if the capacity is reduced.
		 *
		 * @param capacity The desired capacity, which must be at least the current size.
		 */
		public void trim( int capacity ) {
			capacity = Array.prime( Math.max( capacity, size() ) );
			if( length() <= capacity ) return;
			
			
			resize( capacity );
		}
		
		
		/**
		 * Resizes the internal hash set arrays to a new capacity. All existing elements are
		 * rehashed and inserted into the new arrays, rebuilding the `lo` and `hi` regions.
		 * This is a structural modification and increments the version counter.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resizing.
		 */
		private int resize( int newSize ) {
			_version++;
			
			
			long[] old_keys    = keys;
			int           old_lo_Size = _lo_Size;
			int           old_hi_Size = _hi_Size;
			initialize( newSize );
			
			for( int i = 0; i < old_lo_Size; i++ )
			     copy( ( long ) old_keys[ i ] );
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ )
			     copy( ( long ) old_keys[ i ] );
			
			return length();
		}
		
		/**
		 * An internal helper method for efficiently copying a key into the new hash table
		 * structure during a resize operation. It performs the insertion logic without
		 * checking for duplicates, as it assumes all keys from the old set are unique.
		 *
		 * @param key The primitive key to copy.
		 */
		private void copy( long key ) {
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int index       = _buckets[ bucketIndex ] - 1;
			
			int dst_index;
			
			
			if( index == -1 )
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				if( links.length == _lo_Size )
					links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) );
				links[ dst_index = _lo_Size++ ] = ( int ) index;
			}
			
			keys[ dst_index ]       = ( long ) key;
			_buckets[ bucketIndex ] = ( int ) ( dst_index + 1 );
		}
		
		/**
		 * Creates and returns a deep copy of this read-write set.
		 *
		 * @return A cloned instance of this RW set.
		 * @throws InternalError if cloning fails.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
	}
}