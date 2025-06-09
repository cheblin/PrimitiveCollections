//MIT License
//
//Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
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
import java.util.function.Function;

/**
 * A generic Map implementation for storing key-value pairs with efficient operations.
 * Implements a hash table with separate chaining for collision resolution and dynamic resizing.
 *
 * <p>Supports null keys and values, designed for high-performance and customization.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *     <li><b>Generic Keys:</b> Supports any object type as keys.</li>
 *     <li><b>Integer Values:</b> Optimized for storing primitive values.</li>
 *     <li><b>Separate Chaining:</b> Efficiently handles hash collisions.</li>
 *     <li><b>Dynamic Resizing:</b> Maintains performance as the map grows.</li>
 *     <li><b>Null Key Support:</b> Allows a single null key.</li>
 *     <li><b>Customizable Equality and Hashing:</b> Uses {@link Array.EqualHashOf} for key handling.</li>
 *     <li><b>Token-Based Iteration:</b> Safe and efficient traversal, even with concurrent reads.</li>
 *     <li><b>Cloneable:</b> Implements {@link Cloneable} for shallow copies.</li>
 * </ul>
 */
public interface ObjectUByteMap {
	
	/**
	 * Read-only base class providing core functionality and state management for the map.
	 */
	abstract class R< K > implements JsonWriter.Source, Cloneable {
		protected boolean                hasNullKey;        // Indicates if the Map contains a null key
		protected byte            nullKeyValue;      // Value for the null key
		protected int[]                  _buckets;         // Hash table buckets array
		protected int[]                  hash;             // Array of entries: hashCode
		protected int[]                  links;            // Array of entries: next index in collision chain (0-based)
		protected K[]                    keys;            // Array of keys
		protected byte[]          values;          // Array of values
		protected int                    _lo_Size;         // Number of active entries in the low region (0 to _lo_Size-1).
		protected int                    _hi_Size;         // Number of active entries in the high region (keys.length - _hi_Size to keys.length-1).
		protected int                    _version;        // Version for modification detection
		protected Array.EqualHashOf< K > equal_hash_K;    // Key equality and hash strategy
		
		protected static final int  NULL_KEY_INDEX = 0x7FFF_FFFF;
		protected static final int  VERSION_SHIFT  = 32;
		protected static final long INVALID_TOKEN  = -1L;
		
		/**
		 * Returns the total number of active non-null entries in the map.
		 * This is the sum of entries in the low and high regions.
		 *
		 * @return The current count of non-null entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return {@code true} if the map contains no key-value mappings, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value mappings in the map.
		 *
		 * @return The number of key-value mappings, including the null key if present.
		 */
		public int size() {
			return _count() + ( hasNullKey ?
			                    1 :
			                    0 );
		}
		
		/**
		 * Alias for {@link #size()}.
		 *
		 * @return The number of key-value mappings in the map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the capacity of the internal arrays.
		 *
		 * @return The length of the internal arrays, or 0 if uninitialized.
		 */
		public int length() {
			return keys == null ?
			       0 :
			       keys.length;
		}
		
		/**
		 * Checks if the map contains a mapping for the specified key.
		 *
		 * @param key The key to check.
		 * @return {@code true} if the map contains the key, {@code false} otherwise.
		 */
		public boolean containsKey( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 *
		 * @param value The value to check.
		 * @return {@code true} if the map contains the value, {@code false} otherwise.
		 */
		public boolean containsValue( char value ) {
			
			if( hasNullKey && nullKeyValue == value ) return true;
			
			if( _count() == 0 ) return false;
			
			// Iterate active entries in the low region
			for( int i = 0; i < _lo_Size; i++ ) if( values[ i ] == value ) return true;
			// Iterate active entries in the high region
			for( int i = keys.length - _hi_Size; i < keys.length; i++ ) if( values[ i ] == value ) return true;
			return false;
		}
		
		/**
		 * Checks if the map contains a mapping for the null key.
		 *
		 * @return {@code true} if the map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the null key.
		 *
		 * @return The value associated with the null key, or undefined if no null key exists.
		 */
		public char nullKeyValue() { return (char)( 0xFF &  nullKeyValue); }
		
		/**
		 * Returns the token associated with the specified key.
		 *
		 * @param key The key to look up.
		 * @return A token for iteration, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if excessive collisions are detected.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
			                         token( NULL_KEY_INDEX ) :
			                         INVALID_TOKEN;
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			
			int hash  = equal_hash_K.hashCode( key );
			int index = _buckets[ bucketIndex( hash ) ] - 1; // 0-based index from bucket
			if( index < 0 ) return INVALID_TOKEN; // Bucket is empty
			
			// If the first entry is in the hi Region (it's the only one for this bucket)
			if( _lo_Size <= index ) return ( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) ?
			                               token( index ) :
			                               INVALID_TOKEN;
			
			// Traverse collision chain (entry is in lo Region)
			for( int collisions = 0; ; ) {
				if( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) return token( index );
				if( _lo_Size <= index ) break; // Reached a terminal node that might be in hi Region (no more links)
				index = links[ index ];
				if( _lo_Size + 1 < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the first valid token for iteration.
		 *
		 * @return A token for iteration, or {@link #INVALID_TOKEN} if the map is empty.
		 */
		public long token() {
			int index = unsafe_token( -1 ); // Get the first non-null key token
			return index == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       // If no non-null keys, return null key token if present
			       INVALID_TOKEN :
			       token( index ); // Return token for found non-null key
		}
		
		/**
		 * Returns the next valid token for iteration.
		 *
		 * @param token The current token.
		 * @return The next token, or {@link #INVALID_TOKEN} if no further entries exist or the token is invalid.
		 * @throws IllegalArgumentException        if the token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the map was modified since the token was issued.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN; // If current token is null key, no more elements
			
			index = unsafe_token( index ); // Get next unsafe token (non-null key)
			
			return index == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       // If no more non-null keys, check for null key
			       INVALID_TOKEN :
			       token( index ); // Return token for found non-null key
		}
		
		/**
		 * Returns the next token for fast, <b>unsafe</b> iteration over <b>non-null keys only</b>.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #key(long)} to handle it separately.</p>
		 *
		 * <p><b>WARNING: UNSAFE.</b> This method is faster than {@link #token(long)} but risky if the map is
		 * structurally modified during iteration. Such changes may cause skipped entries, exceptions, or undefined
		 * behavior. Use only when no modifications will occur.</p>
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 */
		public int unsafe_token( int token ) {
			if( _buckets == null || _count() == 0 ) return -1;
			
			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size; // Start of hi region (inclusive)
			
			// Check lo region first
			if( i < _lo_Size ) return i; // Return the current index in lo region
			
			// If we've exhausted lo region or started beyond it
			if( i < lowest_hi ) { // If 'i' is in the gap between lo and hi regions
				// If hi region is empty, no more entries
				if( _hi_Size == 0 ) return -1;
				return lowest_hi; // Return the start of hi region
			}
			
			// If 'i' is already in or past the start of the hi region
			// Iterate through hi region
			if( i < keys.length ) return i;
			
			return -1; // No more entries
		}
		
		/**
		 * Checks if the token corresponds to the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the key associated with the given token.
		 *
		 * @param token The token to query.
		 * @return The key, or {@code null} if the token corresponds to the null key entry.
		 */
		public K key( long token ) {
			return isKeyNull( token ) ?
			       null :
			       keys[ index( token ) ];
		}
		
		/**
		 * Returns the value associated with the given token.
		 *
		 * @param token The token to query.
		 * @return The value associated with the token.
		 */
		public char value( long token ) {
			return (char)( 0xFF & ( index( token ) == NULL_KEY_INDEX ?
			                   nullKeyValue :
			                   values[ index( token ) ] ));
		}
		
		/**
		 * Computes the hash code for the map based on its key-value pairs.
		 *
		 * @return The hash code for the map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) );
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
		
		private static final int seed = R.class.hashCode();
		
		/**
		 * Checks if this map is equal to another object.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the object is a map with the same key-value mappings, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Checks if this map is equal to another map of the same type.
		 *
		 * @param other The other map to compare with.
		 * @return {@code true} if the maps have the same key-value mappings, {@code false} otherwise.
		 */
		public boolean equals( R< K > other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    hasNullKey && nullKeyValue != other.nullKeyValue ||
			    size() != other.size() ) return false;
			
			long t;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( ( t = other.tokenOf( key( token ) ) ) == INVALID_TOKEN ||
				    value( token ) != other.value( t ) ) return false;
			return true;
		}
		
		/**
		 * Creates a shallow copy of this map.
		 *
		 * @return A new map with the same key-value mappings, or {@code null} if cloning fails.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public R< K > clone() {
			try {
				R dst = ( R ) super.clone();
				if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.hash     = hash.clone();
					dst.links    = links.clone();
					dst.keys     = keys.clone();
					dst.values   = values.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e );
			}
		}
		
		/**
		 * Returns a JSON string representation of this map.
		 *
		 * @return A JSON string representing the map's contents.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Serializes the map's contents to JSON.
		 *
		 * @param json The {@link JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			
			if( equal_hash_K == Array.string ) {
				json.enterObject();
				if( hasNullKey ) json.name().value( nullKeyValue );
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( key( token ) == null ?
				                null :
				                key( token ).toString() )
				         .value( value( token ) );
				json.exitObject();
			}
			else {
				json.enterArray();
				
				if( hasNullKey )
					json.enterObject()
					    .name( "Key" ).value()
					    .name( "Value" ).value( nullKeyValue )
					    .exitObject();
				
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.enterObject()
				         .name( "Key" ).value( key( token ) )
				         .name( "Value" ).value( value( token ) )
				         .exitObject();
				json.exitArray();
			}
		}
		
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		protected long token( int index )     { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); }
		
		protected int index( long token ) {
			return ( int ) token;
		}
		
		protected int version( long token ) {
			return ( int ) ( token >>> VERSION_SHIFT );
		}
	}
	
	/**
	 * Read-write implementation extending the read-only base class, providing methods to modify the map.
	 */
	class RW< K > extends R< K > {
		private static final int                                                        HashCollisionThreshold = 100;
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs an empty map for the specified key class.
		 *
		 * @param clazzK The class of the keys.
		 */
		public RW( Class< K > clazzK ) { this( Array.get( clazzK ), 0 ); }
		
		/**
		 * Constructs an empty map for the specified key class with the given initial capacity.
		 *
		 * @param clazzK   The class of the keys.
		 * @param capacity The initial capacity.
		 */
		public RW( Class< K > clazzK, int capacity ) { this( Array.get( clazzK ), capacity ); }
		
		/**
		 * Constructs an empty map with the specified equality and hash strategy.
		 *
		 * @param equal_hash_K The equality and hash strategy for keys.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K ) { this( equal_hash_K, 0 ); }
		
		/**
		 * Constructs an empty map with the specified equality and hash strategy and initial capacity.
		 *
		 * @param equal_hash_K The equality and hash strategy for keys.
		 * @param capacity     The initial capacity.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Removes all mappings from the map.
		 */
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count() == 0 ) return; // Uses _count() for current entries
			Arrays.fill( _buckets, 0 );
			Arrays.fill( keys, null ); // Clear object references
			// The values, hash, and links arrays are implicitly cleared as _lo_Size and _hi_Size are reset.
			_lo_Size = 0;
			_hi_Size = 0;
		}
		
		/**
		 * Removes the mapping for the specified key, if present.
		 *
		 * @param key The key to remove.
		 * @return true if remove entry or false no mapping was found for the key.
		 * @throws ConcurrentModificationException if excessive collisions are detected.
		 */
		public boolean remove( K key ) {
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
			
			// Case 2: Entry to be removed is in the lo Region
			// Finding the key in the chain and updating links
			int next = links[ removeIndex ]; // Get the link from the first element in the chain
			
			// Key found at the head of the chain (removeIndex)
			if( this.hash[ removeIndex ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) _buckets[ removeBucketIndex ] = ( next + 1 ); // Update bucket to point to the next element
			else {
				// Key is NOT at the head of the chain. Traverse.
				int last = removeIndex; // 'last' tracks the node *before* 'removeIndex' (the element we are currently checking)
				
				// This block is from ObjectBitsMap, it handles the 2nd element in the chain.
				// 'removeIndex' is reassigned here to be the 'next' element.
				// The key is found at the 'SecondNode'
				// 'SecondNode' is in 'lo Region', relink 'last' to bypass 'SecondNode'
				if( this.hash[ removeIndex = next ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
				else {  // 'SecondNode' is in the hi Region (it's a terminal node)
					// This block performs a specific compaction for hi-region terminal nodes.
					keys[ removeIndex ]      = keys[ last ]; // Copies `keys[last]` to `keys[removeIndex]`
					this.hash[ removeIndex ] = this.hash[ last ];
					values[ removeIndex ]    = values[ last ]; // Copies `values[last]` to `values[removeIndex]`
					
					// 'removeBucketIndex' is the hash bucket for the original 'key'.
					// By pointing it to 'removeIndex' (which now contains data from 'last'),
					// we make 'last' (now at 'removeIndex') the new sole head of the bucket.
					_buckets[ removeBucketIndex ] = ( removeIndex + 1 );
					removeIndex                   = last; // Mark original 'last' (lo-region entry) for removal/compaction
				}
				else // Loop for 3rd+ elements in the chain
					// If 'removeIndex' (now the second element) is in hi-region and didn't match
					if( _lo_Size <= removeIndex ) return false; // Key not found in this chain
					else for( int collisionCount = 0; ; ) {
						int prev = last; // This 'prev' is the element *before* 'last' in this inner loop context
						
						// Advance 'last' and 'removeIndex' (current element being checked)
						if( this.hash[ removeIndex = links[ last = removeIndex ] ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) {
							// Found in lo-region: relink 'last' to bypass 'removeIndex'
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else { // Found in hi-region (terminal node): Special compaction
								// Copy data from 'removeIndex' (hi-region node) to 'last' (lo-region node)
								keys[ removeIndex ]      = keys[ last ];
								this.hash[ removeIndex ] = this.hash[ last ];
								values[ removeIndex ]    = values[ last ]; // Adapted type
								
								// Relink 'prev' (the element before 'last') to 'removeIndex'
								links[ prev ] = removeIndex;
								                removeIndex = last; // Mark original 'last' (lo-region entry) for removal/compaction
							}
							break; // Key found and handled, break from loop.
						}
						if( _lo_Size <= removeIndex ) return false; // Reached hi-region terminal node, key not found
						// Safeguard against excessively long or circular chains (corrupt state)
						if( _lo_Size + 1 < ++collisionCount ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
			}
			
			// At this point, 'removeIndex' holds the final index of the element to be removed.
			// This 'removeIndex' might have been reassigned multiple times within the if-else blocks.
			move( _lo_Size - 1, removeIndex ); // Move the last lo-region element to the spot of the removed entry
			_lo_Size--; // Decrement lo-region size
			_version++; // Structural modification
			return true;
		}
		
		/**
		 * Relocates an entry's key, hash, link, and value from a source index ({@code src}) to a destination index ({@code dst})
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
			
			int bucketIndex = bucketIndex( this.hash[ src ] );
			int index       = _buckets[ bucketIndex ] - 1;
			
			if( index == src ) _buckets[ bucketIndex ] = ( dst + 1 ); // Update bucket head if src was the head
			else {
				// Find the link pointing to src
				// This loop iterates through the chain for the bucket to find the predecessor of `src`
				while( links[ index ] != src ) {
					index = links[ index ];
					// Defensive break for corrupted chain or if `src` is somehow not in this chain (shouldn't happen if logic is correct)
					if( index == -1 || index >= keys.length ) break;
				}
				if( links[ index ] == src ) // Ensure we found it before updating
					links[ index ] = dst; // Update the link to point to dst
			}
			
			// Only copy the link if the source was in the low region (as hi-region elements don't link further)
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			this.hash[ dst ] = this.hash[ src ]; // Copy hash
			keys[ dst ]      = keys[ src ];   // Copy key
			keys[ src ]      = null;          // Clear source slot for memory management and to prevent stale references
			values[ dst ]    = values[ src ]; // Copy value
		}
		
		/**
		 * Ensures the hash table can hold at least the specified capacity without resizing.
		 * <p>
		 * Resizes to the next prime number >= capacity if needed.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity after resizing, or current capacity if sufficient.
		 * @throws IllegalArgumentException If capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity < 0" );
			int currentCapacity = length();
			if( capacity <= currentCapacity ) return currentCapacity; // Already sufficient capacity
			_version++; // Increment version as structural change is about to occur
			if( _buckets == null ) return initialize( Array.prime( capacity ) ); // Initialize if buckets are null
			int newSize = Array.prime( capacity ); // Get next prime size
			resize( newSize, false );                // Resize to the new size
			return newSize;
		}
		
		/**
		 * Reduces the hash table capacity to the current number of mappings.
		 * <p>
		 * Uses a prime number >= current count.
		 */
		public void trim() { trim( _count() ); } // Uses _count()
		
		/**
		 * Reduces the hash table capacity to at least the specified capacity.
		 * <p>
		 * Uses a prime number >= capacity. No action if current capacity is already <= specified capacity.
		 *
		 * @param capacity The desired capacity (>= current count).
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			capacity = Array.prime( Math.max( capacity, _count() ) ); // Ensure capacity is at least current count and prime
			if( currentCapacity <= capacity ) return; // No need to trim if current capacity is already smaller or equal
			
			resize( capacity, false ); // Resize to the new smaller size
		}
		
		/**
		 * Sets or updates the value for the null key.
		 *
		 * @param value The value for the null key.
		 * @return {@code true} if the null key was newly set or updated, {@code false} if unchanged.
		 */
		public boolean putNullKeyValue( char value ) {
			boolean b = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = ( byte ) value;
			_version++;
			return b;
		}
		
		/**
		 * Puts all key-value mappings from the given source map into this map.
		 * Existing keys will have their values updated.
		 *
		 * @param src The source map whose mappings are to be put into this map.
		 */
		public void putAll( RW< ? extends K > src ) {
			for( int token = -1; ( token = src.unsafe_token( token ) ) != -1; )
			     put( src.key( token ), src.value( token ) );
		}
		
		/**
		 * Associates the specified value with the specified key.
		 * <p>
		 * Replaces the old value if the key exists; otherwise, adds a new mapping.
		 *
		 * @param key   The key to map.
		 * @param value The value to associate.
		 * @return {@code true} if a new mapping was added, {@code false} if an existing value was updated.
		 * @throws ConcurrentModificationException If concurrent modifications are detected.
		 */
		public boolean put( K key, char value ) {
			if( key == null ) return putNullKeyValue( value );
			
			
			if( _buckets == null ) initialize( 7 ); // Initial capacity if not yet initialized
			else // If backing array is full, resize
				if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ), false ); // Resize to double capacity
			
			int hash        = equal_hash_K.hashCode( key );
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from bucket
			int dst_index; // Destination index for the new/updated entry
			
			// If bucket is empty, new entry goes into hi Region
			if( index == -1 ) dst_index = keys.length - 1 - _hi_Size++; // Calculate new position in hi region
			else {
				// Bucket is not empty, 'index' points to an existing entry
				if( _lo_Size <= index ) { // Existing entry is in {@code hi Region}
					if( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) { // Key matches existing {@code hi Region} entry
						values[ index ] = ( byte ) value; // Update value
						_version++;
						return false; // Key was not new
					}
				}
				else  // Existing entry is in {@code lo Region} (collision chain)
				{
					int collisions = 0;
					for( int next = index; ; ) {
						if( this.hash[ next ] == hash && equal_hash_K.equals( keys[ next ], key ) ) {
							values[ next ] = ( byte ) value; // Update value
							return false;// Key was not new
						}
						if( _lo_Size <= next ) break; // Reached a terminal node in hi Region
						next = links[ next ]; // Move to next in chain
						// Safeguard against excessively long or circular chains (corrupt state)
						if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
					// Check for high collision and potential rehashing for string keys, adapted from original put()
					if( HashCollisionThreshold < collisions && this.forceNewHashCodes != null && key instanceof String ) // Check for high collision and potential rehashing for string keys
					{
						resize( keys.length, true ); // Resize to potentially trigger new hash codes
						hash        = equal_hash_K.hashCode( key );
						bucketIndex = bucketIndex( hash );
						index       = _buckets[ bucketIndex ] - 1;
					}
				}
				
				
				// Key is new, and a collision occurred (bucket was not empty). Place new entry in {@code lo Region}.
				if( links.length == ( dst_index = _lo_Size++ ) ) // If links array needs resize, and assign new index
					// Resize links array, cap at keys.length to avoid unnecessary large array
					links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index; // Link new entry to the previous head of the chain
			}
			
			
			this.hash[ dst_index ]  = hash; // Store hash code
			keys[ dst_index ]       = key;     // Store key
			values[ dst_index ]     = ( byte ) value; // Store value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to point to the new entry (1-based)
			_version++; // Increment version
			
			
			return true; // New mapping added
		}
		
		/**
		 * Resizes the hash table to the specified capacity.
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
			K[]           old_keys    = keys;
			int[]         old_hash    = hash;
			byte[] old_values  = values.clone(); // Clone values to copy them over
			int           old_lo_Size = _lo_Size;
			int           old_hi_Size = _hi_Size;
			
			// Re-initialize with new capacity (this clears _buckets, resets _lo_Size, _hi_Size)
			initialize( newSize );
			
			
			// If forceNewHashCodes is set, apply it to the equal_hash_K provider BEFORE re-hashing elements.
			if( forceNewHashCodes ) {
				
				equal_hash_K = this.forceNewHashCodes.apply( equal_hash_K ); // Apply new hashing strategy
				
				// Copy elements from old structure to new structure by re-inserting
				K key;
				// Iterate through old lo region
				for( int i = 0; i < old_lo_Size; i++ ) copy( key = old_keys[ i ], equal_hash_K.hashCode( key ), old_values[ i ] );
				
				// Iterate through old hi region
				for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) copy( key = old_keys[ i ], equal_hash_K.hashCode( key ), old_values[ i ] );
				
				return keys.length; // Return actual new capacity
			}
			
			// Copy elements from old structure to new structure by re-inserting
			
			// Iterate through old lo region
			for( int i = 0; i < old_lo_Size; i++ ) copy( old_keys[ i ], old_hash[ i ], old_values[ i ] );
			
			// Iterate through old hi region
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) copy( old_keys[ i ], old_hash[ i ], old_values[ i ] );
			
			return keys.length; // Return actual new capacity
		}
		
		/**
		 * Initializes the hash table with the specified capacity.
		 *
		 * @param capacity The initial capacity (adjusted to next prime).
		 * @return The actual capacity used.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets = new int[ capacity ];     // Initialize buckets array
			hash     = new int[ capacity ];     // Initialize hash array
			links    = new int[ Math.min( 16, capacity ) ]; // Initialize links with a small, reasonable capacity
			keys     = equal_hash_K.copyOf( null, capacity ); // Initialize keys array
			values   = new byte[ capacity ]; // Initialize values array
			_lo_Size = 0;
			_hi_Size = 0;
			return capacity;
		}
		
		/**
		 * Internal helper method used during resizing to efficiently copy an
		 * existing key-value pair into the new hash table structure. It re-hashes the key
		 * and places it into the correct bucket and region (lo or hi) in the new arrays.
		 * This method does not check for existing keys, assuming all keys are new in the
		 * target structure during a resize operation.
		 *
		 * @param key   The key to copy.
		 * @param value The value associated with the key.
		 */
		private void copy( K key, int hash, byte value ) {
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from the bucket
			
			int dst_index; // Destination index for the key
			
			if( index == -1 ) // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				// Collision occurred. Place new entry in {@code lo Region}
				if( links.length == _lo_Size ) // If lo_Size exceeds links array capacity
					links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) ); // Resize links
				
				links[ dst_index = _lo_Size++ ] = index; // New entry points to the old head
			}
			
			keys[ dst_index ]       = key; // Store the key
			this.hash[ dst_index ]  = hash; // Store the hash
			values[ dst_index ]     = (byte)value; // Store the value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to new head (1-based)
		}
		
		/**
		 * Default {@link Array.EqualHashOf} instance for {@link RW}.
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
		@Override public RW< K > clone() { return ( RW< K > ) super.clone(); }
	}
	
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() { return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT; }
}