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

import java.util.*;
import java.util.function.Function;

/**
 * A generic Set implementation providing efficient storage and operations for keys.
 * Supports null keys and provides token-based iteration for safe and unsafe traversal.
 * The implementation uses a hash table with open addressing and a dual-region strategy for efficient key storage
 * and compaction.
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
 */
public interface ObjectSet {
	
	/**
	 * Read-only base class providing core functionality and state management for the set.
	 * Implements {@link java.util.Set} and {@link JsonWriter.Source} for JSON serialization.
	 * <p>
	 * This implementation uses a HYBRID strategy that automatically adapts based on data density,
	 * dividing active entries into two regions:
	 * - {@code lo Region}: Stores entries involved in hash collisions. Uses an explicit {@code links} array for chaining.
	 * Occupies low indices (0 to {@code _lo_Size-1}) in {@code keys}.
	 * - {@code hi Region}: Stores entries that do not (initially) require {@code links} links (i.e., no collision at insertion time, or they are terminal nodes of a chain).
	 * Occupies high indices (from {@code keys.length - _hi_Size} to {@code keys.length-1}) in {@code keys}.
	 * This approach balances memory efficiency for sparse sets with optimal performance for dense sets.
	 */
	abstract class R< K > implements java.util.Set< K >, JsonWriter.Source, Cloneable {
		protected boolean hasNullKey;    // True if the set contains a null key
		protected int[]   _buckets;       // Hash table buckets (1-based indices to chain heads). Stores 0-based indices plus one.
		protected int[]   hash;           // Stores hash codes for each entry.
		protected int[]   links = Array.EqualHashOf._ints.O;          // Stores the 'next' index in collision chains (0-based indices).
		protected K[]     keys;             // Set elements (keys)
		
		protected int _lo_Size;         // Number of active entries in the low region (0 to _lo_Size-1).
		protected int _hi_Size;         // Number of active entries in the high region (keys.length - _hi_Size to keys.length-1).
		
		protected int                    _version;         // Version for modification detection
		protected Array.EqualHashOf< K > equal_hash_K; // Equality and hash provider
		
		protected static final int  NULL_KEY_INDEX = 0x7FFF_FFFF;
		protected static final int  VERSION_SHIFT  = 32;
		protected static final long INVALID_TOKEN  = -1L;
		
		/**
		 * Checks if the set contains a null key.
		 *
		 * @return {@code true} if the set contains a null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the total number of active non-null entries in the set.
		 * This is the sum of entries in the low and high regions.
		 *
		 * @return The current count of non-null entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * Returns the number of elements in the set, including the null key if present.
		 *
		 * @return The size of the set.
		 */
		@Override
		public int size() {
			return _count() + ( hasNullKey ?
			                    1 :
			                    0 );
		}
		
		/**
		 * Returns the total capacity of the internal arrays, which is the maximum number
		 * of entries the map can hold before resizing.
		 *
		 * @return The capacity of the internal data structures.
		 */
		public int length() {
			return keys == null ?
			       0 :
			       keys.length;
		}
		
		/**
		 * Returns the number of elements in the set (alias for {@link #size()}).
		 *
		 * @return The number of elements in the set.
		 */
		public int count() { return size(); }
		
		/**
		 * Checks if the set is empty.
		 *
		 * @return {@code true} if the set contains no elements, {@code false} otherwise.
		 */
		@Override
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Checks if the set contains the specified key.
		 *
		 * @param key The key to check for.
		 * @return {@code true} if the set contains the key, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean contains( Object key ) { return tokenOf( ( K ) key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the set contains the specified key (type-safe version).
		 *
		 * @param key The key to check for.
		 * @return {@code true} if the set contains the key, {@code false} otherwise.
		 */
		public boolean contains_( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Returns the iteration token for the specified key, or {@link #INVALID_TOKEN} if not found.
		 * The token can be used for iteration or to retrieve the key.
		 *
		 * @param key The key to look up.
		 * @return The token for the key, or {@link #INVALID_TOKEN} if the key is not in the set.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
			                         token( NULL_KEY_INDEX ) :
			                         INVALID_TOKEN;
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			
			int hash  = equal_hash_K.hashCode( key );
			int index = ( _buckets[ bucketIndex( hash ) ] ) - 1; // 0-based index from bucket
			if( index < 0 ) return INVALID_TOKEN; // Bucket is empty
			
			// Traverse collision chain in lo Region
			for( int collisions = 0; ; ) {
				if( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) return token( index );
				if( _lo_Size <= index ) return INVALID_TOKEN; // Reached a terminal node (which is in hi Region, or end of chain)
				index = links[ index ];
				if( _lo_Size < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
		}
		
		/**
		 * Returns the first valid iteration token for traversing the set.
		 * The token points to the first valid element or the null key if present and no other elements exist.
		 * Returns {@link #INVALID_TOKEN} if the set is empty.
		 *
		 * @return The first iteration token, or {@link #INVALID_TOKEN} if the set is empty.
		 */
		public long token() {
			int index = unsafe_token( -1 ); // Start unsafe iteration from -1
			return index == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       INVALID_TOKEN :
			       token( index );
		}
		
		/**
		 * Returns the next iteration token in the sequence, starting from the given token.
		 * Used to iterate through the elements of the set, including the null key if present.
		 * Returns {@link #INVALID_TOKEN} if there are no more elements or if the token is invalid.
		 *
		 * @param token The current iteration token.
		 * @return The next iteration token, or {@link #INVALID_TOKEN} if no more elements or the token is invalid.
		 * @throws IllegalArgumentException        If the token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException If the set is modified during iteration.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version )
				throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN; // If current token is null key, no more elements
			
			index = unsafe_token( index ); // Get next unsafe token (non-null key)
			
			return index == -1 ?
			       hasNullKey ?
			       // If no more non-null keys, check for null key
			       token( NULL_KEY_INDEX ) :
			       INVALID_TOKEN :
			       token( index );
		}
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 * Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #key(long)} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * set is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 * <p>
		 * Iteration order: First, entries in the {@code lo Region} (0 to {@code _lo_Size-1}),
		 * then entries in the {@code hi Region} ({@code keys.length - _hi_Size} to {@code keys.length-1}).
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #key(long) To get the key associated with a token.
		 */
		public int unsafe_token( int token ) {
			if( _buckets == null || _count() == 0 ) return -1;
			
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
		 * Checks if the given token corresponds to the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the key associated with the given token.
		 *
		 * @param token The token for the key.
		 * @return The key associated with the token, or {@code null} if the token represents the null key.
		 */
		public K key( long token ) {
			return index( token ) == NULL_KEY_INDEX ?
			       null :
			       keys[ index( token ) ];
		}
		
		/**
		 * Computes the hash code for the set based on its elements.
		 *
		 * @return The hash code for the set.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				final int h = Array.hash( key( token ) );
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
		
		private static final int seed = R.class.hashCode();
		
		/**
		 * Checks if this set is equal to another object.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the sets are equal, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< K > ) obj ); }
		
		/**
		 * Checks if this set is equal to another set of the same type.
		 *
		 * @param other The other set to compare with.
		 * @return {@code true} if the sets are equal, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean equals( R< K > other ) {
			if( other == this ) return true;
			if( other == null || other.size() != size() || other.hasNullKey != hasNullKey ) return false;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( !other.contains_( key( token ) ) ) return false;
			return true;
		}
		
		/**
		 * Creates a shallow copy of the set.
		 *
		 * @return A cloned instance of the set.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public R< K > clone() {
			try {
				R< K > dst = ( R< K > ) super.clone();
				if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.hash     = hash.clone();
					dst.links    = links.clone();
					dst.keys     = keys.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e ); // Propagate as InternalError if cloning fails
			}
		}
		
		/**
		 * Returns a JSON string representation of the set.
		 *
		 * @return The JSON string representation.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		
		/**
		 * Serializes the set to JSON format.
		 *
		 * @param json The {@link JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			int size = size();
			if( equal_hash_K == Array.string ) {
				json.enterObject();
				if( hasNullKey ) json.name().value();
				if( size > 0 ) {
					json.preallocate( size * 10 ); // Heuristic pre-allocation
					for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
					     json.name( key( token ).toString() ).value();
				}
				json.exitObject();
			}
			else {
				json.enterArray();
				if( hasNullKey ) json.value();
				if( size > 0 ) {
					json.preallocate( size * 10 ); // Heuristic pre-allocation
					for( long t = token(); t != INVALID_TOKEN; t = token( t ) ) json.value( key( t ) );
				}
				json.exitArray();
			}
		}
		
		/**
		 * Computes the bucket index for a given hash code.
		 *
		 * @param hash The hash code.
		 * @return The bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an index.
		 *
		 * @param index The index.
		 * @return The token combining the version and index.
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); }
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token The token.
		 * @return The index.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token The token.
		 * @return The version.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * Read-write implementation extending the read-only base class.
	 * Provides methods to add, remove, and modify elements in the set.
	 */
	class RW< K > extends R< K > {
		/**
		 * Threshold for hash collisions in a bucket before considering rehashing.
		 */
		private static final int HashCollisionThreshold = 100;
		
		public Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes = null;
		
		/**
		 * Constructs an empty set for the specified key class.
		 *
		 * @param clazzK The class of the keys.
		 */
		public RW( Class< K > clazzK ) { this( clazzK, 0 ); }
		
		/**
		 * Constructs an empty set with the specified initial capacity for the key class.
		 *
		 * @param clazzK   The class of the keys.
		 * @param capacity The initial capacity.
		 */
		public RW( Class< K > clazzK, int capacity ) { this( Array.get( clazzK ), capacity ); }
		
		/**
		 * Constructs an empty set with the specified equality and hash provider.
		 *
		 * @param equal_hash_K The equality and hash provider.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K ) { this( equal_hash_K, 0 ); }
		
		/**
		 * Constructs an empty set with the specified equality and hash provider and initial capacity.
		 *
		 * @param equal_hash_K The equality and hash provider.
		 * @param capacity     The initial capacity.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Constructs a set containing the elements of the specified collection.
		 *
		 * @param equal_hash_K The equality and hash provider.
		 * @param collection   The collection whose elements are to be added to the set.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Collection< ? extends K > collection ) {
			this.equal_hash_K = equal_hash_K;
			addAll( collection );
		}
		
		/**
		 * Adds a key to the set if it is not already present.
		 *
		 * @param key The key to add.
		 * @return {@code true} if the key was added, {@code false} if it was already present.
		 */
		@Override
		public boolean add( K key ) {
			if( key == null ) {
				if( hasNullKey ) return false;
				hasNullKey = true;
				_version++;
				return true;
			}
			
			if( _buckets == null ) initialize( 7 ); // Initial capacity for hash table
			else if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ), false ); // Resize if backing array is full
			
			int hash        = equal_hash_K.hashCode( key );
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from bucket
			int dst_index;
			
			// Bucket is empty: place new entry in hi Region
			if( index == -1 ) dst_index = keys.length - 1 - _hi_Size++; // Add to the "bottom" of hi Region
			else {
				// Bucket is not empty, 'index' points to an existing entry
				int collisions = 0;
				for( int i = index; ; ) {
					if( this.hash[ i ] == hash && equal_hash_K.equals( keys[ i ], key ) ) return false; // Key was not new
					
					if( _lo_Size <= i ) break; // Reached a terminal node (which could be in hi Region)
					i = links[ i ];
					
					// Safety check for endless loop / corrupted state
					if( _lo_Size < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
				// Check for high collision and potential rehashing for string keys, adapted from original put()
				if( HashCollisionThreshold < collisions && this.forceNewHashCodes != null && key instanceof String ) // Check for high collision and potential rehashing for string keys
				{
					resize( keys.length, true ); // Resize to potentially trigger new hash codes
					hash        = equal_hash_K.hashCode( key );
					bucketIndex = bucketIndex( hash );
					index       = _buckets[ bucketIndex ] - 1;
				}
				
				( links.length == ( dst_index = _lo_Size++ ) ?
				  links = Arrays.copyOf( links, Math.max( 16, Math.min( _lo_Size * 2, keys.length ) ) ) :
				  links )[ dst_index ] = index; // New entry points to the old head
			}
			
			this.hash[ dst_index ]  = hash; // Store hash code
			keys[ dst_index ]       = key;   // Store key
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to new head (1-based)
			_version++;
			return true;
		}
		
		/**
		 * Removes the specified key from the set if it is present.
		 *
		 * @param key The key to remove.
		 * @return {@code true} if the key was removed, {@code false} if it was not present.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean remove( Object key ) { return remove_( ( K ) key ); }
		
		/**
		 * Removes the specified key from the set if it is present (type-safe version).
		 *
		 * @param key The key to remove.
		 * @return {@code true} if the key was removed, {@code false} if it was not present.
		 */
		public boolean remove_( K key ) {
			if( key == null ) { // Handle null key removal
				if( !hasNullKey ) return false; // Null key not present
				hasNullKey = false;                     // Remove null key flag
				_version++;                             // Increment version
				return true;
			}
			
			if( _count() == 0 ) return false; // Map is empty
			
			int hash              = equal_hash_K.hashCode( key );
			int removeBucketIndex = bucketIndex( hash );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1; // 0-based index from bucket
			if( removeIndex < 0 ) return false; // Key not in this bucket
			
			// Case 1: Entry to be removed is in the hi Region (cannot be part of a chain from _lo_Size)
			if( _lo_Size <= removeIndex ) {
				if( this.hash[ removeIndex ] != hash || !equal_hash_K.equals( keys[ removeIndex ], key ) ) return false;
				
				// Move the last element of hi region to the removed slot
				move( keys.length - _hi_Size, removeIndex );
				
				_hi_Size--; // Decrement hi_Size
				_buckets[ removeBucketIndex ] = 0; // Clear the bucket reference (it was the only one)
				_version++;
				return true;
			}
			
			
			int next = links[ removeIndex ];
			if( this.hash[ removeIndex ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) _buckets[ removeBucketIndex ] = ( int ) ( next + 1 );
			else {
				int last = removeIndex;
				if( this.hash[ removeIndex = next ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) )// The key is found at 'SecondNode'
					if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];// 'SecondNode' is in 'lo Region', relink to bypasses 'SecondNode'
					else {  // 'SecondNode' is in the hi Region (it's a terminal node)
						
						keys[ removeIndex ]      = keys[ last ]; //  Copies `keys[last]` to `keys[removeIndex]`
						this.hash[ removeIndex ] = this.hash[ last ];
						
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
						
						if( this.hash[ removeIndex = links[ last = removeIndex ] ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) {
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else {
								
								keys[ removeIndex ]      = keys[ last ];
								this.hash[ removeIndex ] = this.hash[ last ];
								
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
		 * Relocates an entry's key, hash, and link from a source index ({@code src}) to a destination index ({@code dst})
		 * within the internal arrays. This method is crucial for compaction during removal operations.
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
			
			hash[ dst ] = hash[ src ];
			keys[ dst ] = keys[ src ];
			keys[ src ] = null;// Clear source slot for memory management and to prevent stale references
		}
		
		/**
		 * Adds all elements from the specified collection to the set.
		 *
		 * @param keys The collection of keys to add.
		 * @return {@code true} if the set was modified, {@code false} otherwise.
		 */
		@Override
		public boolean addAll( Collection< ? extends K > keys ) {
			boolean modified = false;
			for( K key : keys ) if( add( key ) ) modified = true;
			return modified;
		}
		
		/**
		 * Removes all elements from the set that are contained in the specified collection.
		 *
		 * @param keys The collection of keys to remove.
		 * @return {@code true} if the set was modified, {@code false} otherwise.
		 */
		@Override
		public boolean removeAll( Collection< ? > keys ) {
			Objects.requireNonNull( keys );
			int v = _version;
			
			// Restart iteration after each removal, as indices might have shifted
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) {
				K currentKey = key( token( t ) );
				if( keys.contains( currentKey ) ) {
					remove_( currentKey );
					t = -1; // Reset iteration after removal
				}
			}
			
			// Handle null key if present
			if( hasNullKey && keys.contains( null ) ) remove_( null );
			
			return v != _version;
		}
		
		/**
		 * Retains only the elements in the set that are contained in the specified collection.
		 *
		 * @param keys The collection of keys to retain.
		 * @return {@code true} if the set was modified, {@code false} otherwise.
		 */
		@Override
		public boolean retainAll( Collection< ? > keys ) {
			Objects.requireNonNull( keys );
			int v = _version;
			
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) {
				K currentKey = key( token( t ) );
				if( !keys.contains( currentKey ) ) {
					remove_( currentKey );
					t = -1; // Reset iteration after removal
				}
			}
			
			// Handle null key separately
			if( hasNullKey && !keys.contains( null ) ) remove_( null );
			
			return v != _version;
		}
		
		/**
		 * Removes all elements from the set.
		 */
		@Override
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count() == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( keys, null );
			// No explicit clearing of hash/links array elements as they are overwritten on add,
			// and resetting _lo_Size, _hi_Size effectively marks them as empty.
			_lo_Size = 0;
			_hi_Size = 0;
		}
		
		/**
		 * Returns an iterator over the elements in the set.
		 *
		 * @return An iterator over the set's elements.
		 */
		@Override
		public java.util.Iterator< K > iterator() {
			return new Iterator( this );
		}
		
		/**
		 * Checks if the set contains all elements from the specified collection.
		 *
		 * @param src The collection to check.
		 * @return {@code true} if the set contains all elements, {@code false} otherwise.
		 */
		@Override
		public boolean containsAll( Collection< ? > src ) {
			for( Object element : src )
				if( !contains( element ) ) return false;
			return true;
		}
		
		/**
		 * Returns an array containing all elements in the set.
		 *
		 * @return An array of the set's elements.
		 */
		@Override
		public Object[] toArray() {
			Object[] array = new Object[ size() ];
			int      index = 0;
			for( long token = token(); token != INVALID_TOKEN; token = token( token ) )
			     array[ index++ ] = key( token );
			return array;
		}
		
		/**
		 * Returns an array containing all elements in the set, using the provided array if possible.
		 *
		 * @param a The array to fill, if large enough.
		 * @return An array of the set's elements.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public < T > T[] toArray( T[] a ) {
			int size = size();
			if( a.length < size ) return ( T[] ) Arrays.copyOf( toArray(), size, a.getClass() );
			System.arraycopy( toArray(), 0, a, 0, size );
			if( a.length > size ) a[ size ] = null;
			return a;
		}
		
		/**
		 * Trims the set's capacity to its current size.
		 */
		public void trim() {
			trim( count() );
		}
		
		/**
		 * Trims the set's capacity to the specified capacity, which must be at least the current size.
		 * The actual new capacity will be a prime number at least {@code capacity} and
		 * at least the current {@link #_count()}.
		 *
		 * @param capacity The desired capacity.
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			if( length() <= ( capacity = Array.prime( Math.max( capacity, size() ) ) ) ) return;
			
			resize( capacity, false );
			
			if( _lo_Size < links.length )
				links = _lo_Size == 0 ?
				        Array.EqualHashOf._ints.O :
				        Array.copyOf( links, _lo_Size );
		}
		
		/**
		 * Initializes the set's internal arrays with the specified capacity.
		 *
		 * @param capacity The capacity to initialize.
		 * @return The initialized capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets = new int[ capacity ];
			hash     = new int[ capacity ];
			keys     = equal_hash_K.copyOf( null, capacity );
			_lo_Size = 0;
			_hi_Size = 0;
			return capacity;
		}
		
		/**
		 * Resizes the set's internal arrays to a new specified size.
		 * This method rebuilds the hash table structure based on the new size.
		 * All existing entries are rehashed and re-inserted into the new, larger structure.
		 * This operation increments the set's version.
		 *
		 * @param newSize The desired new capacity for the internal arrays.
		 * @return The actual allocated capacity after resize.
		 */
		private int resize( int newSize, boolean forceNewHashCodes ) {
			_version++;
			
			// Store old data before re-initializing
			K[]   old_keys = keys;
			int[] old_hash = hash;
			
			int old_lo_Size = _lo_Size;
			int old_hi_Size = _hi_Size;
			if( links.length < 0xFF && links.length < _buckets.length ) links = _buckets;//reuse buckets as links
			initialize( newSize );
			
			
			// If forceNewHashCodes is set, apply it to the equal_hash_K provider BEFORE re-hashing elements.
			if( forceNewHashCodes ) {
				
				equal_hash_K = this.forceNewHashCodes.apply( equal_hash_K ); // Apply new hashing strategy
				
				// Copy elements from old structure to new structure by re-inserting
				K key;
				// Iterate through old lo region
				for( int i = 0; i < old_lo_Size; i++ ) copy( key = old_keys[ i ], equal_hash_K.hashCode( key ) );
				
				// Iterate through old hi region
				for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) copy( key = old_keys[ i ], equal_hash_K.hashCode( key ) );
				
				return keys.length; // Return actual new capacity
			}
			
			// Copy elements from old structure to new structure by re-inserting
			
			// Iterate through old lo region
			for( int i = 0; i < old_lo_Size; i++ ) copy( old_keys[ i ], old_hash[ i ] );
			
			// Iterate through old hi region
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) copy( old_keys[ i ], old_hash[ i ] );
			
			return keys.length; // Return actual new capacity
		}
		
		/**
		 * Internal helper method used during resizing to efficiently copy an
		 * existing key into the new hash table structure. It re-hashes the key
		 * and places it into the correct bucket and region (lo or hi) in the new arrays.
		 * This method does not check for existing keys, assuming all keys are new in the
		 * target structure during a resize operation.
		 *
		 * @param key The key to copy.
		 */
		private void copy( K key, int hash ) {
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from the bucket
			
			int dst_index; // Destination index for the key
			
			// Bucket is empty: place new entry in hi Region
			if( index == -1 ) dst_index = keys.length - 1 - _hi_Size++;
			else ( links.length == _lo_Size ?
			       links = Arrays.copyOf( links, Math.max( 16, Math.min( _lo_Size * 2, keys.length ) ) ) :
			       links )[ dst_index = _lo_Size++ ] = ( char ) ( index );
			
			keys[ dst_index ]       = key; // Store the key
			this.hash[ dst_index ]  = hash; // Store the hash
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to new head (1-based)
		}
		
		/**
		 * Iterator implementation for the set.
		 */
		public class Iterator implements java.util.Iterator< K > {
			private final RW< K > _set;
			private       long    _currentToken;
			private       int     _version;
			private       K       _currentKey;
			
			Iterator( RW< K > set ) {
				_set          = set;
				_version      = set._version;
				_currentToken = INVALID_TOKEN; // Start in an invalid state, hasNext() will fetch the first
				_currentKey   = null;
			}
			
			/**
			 * Checks if there are more elements to iterate.
			 *
			 * @return {@code true} if there are more elements, {@code false} otherwise.
			 * @throws ConcurrentModificationException If the set is modified during iteration.
			 */
			@Override
			public boolean hasNext() {
				if( _version != _set._version )
					throw new ConcurrentModificationException( "Collection was modified; enumeration operation may not execute." );
				
				// Check if _currentToken is INVALID_TOKEN (first call or after remove)
				if( _currentToken == INVALID_TOKEN ) _currentToken = _set.token(); // Get first token
				else _currentToken = _set.token( _currentToken ); // Get next token
				return _currentToken != INVALID_TOKEN;
			}
			
			/**
			 * Returns the next element in the iteration.
			 *
			 * @return The next element.
			 * @throws ConcurrentModificationException If the set is modified during iteration.
			 * @throws NoSuchElementException          If there are no more elements.
			 */
			@Override
			public K next() {
				if( _version != _set._version )
					throw new ConcurrentModificationException( "Collection was modified; enumeration operation may not execute." );
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentKey = _set.key( _currentToken ); // Get the key for the current token
				return _currentKey;
			}
			
			/**
			 * Removes the last element returned by the iterator from the set.
			 *
			 * @throws ConcurrentModificationException If the set is modified during iteration.
			 * @throws IllegalStateException           If no element has been returned by the iterator.
			 */
			@Override
			public void remove() {
				if( _version != _set._version )
					throw new ConcurrentModificationException( "Collection was modified; enumeration operation may not execute." );
				if( _currentKey == null && !_set.hasNullKey && _currentToken == INVALID_TOKEN )
					throw new IllegalStateException( "No element to remove." );
				
				_set.remove_( _currentKey );
				_currentKey   = null; // Clear current key after removal
				_version      = _set._version; // Update iterator's version to match set's version
				_currentToken = INVALID_TOKEN; // Invalidate current token to force hasNext() to re-evaluate from start
			}
		}
		
		@Override
		public RW< K > clone() {
			return ( RW< K > ) super.clone();
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Returns the equality and hash provider for the RW class.
	 *
	 * @param <K> The type of keys.
	 * @return The equality and hash provider.
	 */
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() {
		return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT;
	}
}