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

import java.util.*;

/**
 * {@code IntSet} is a specialized Set interface designed for efficient storage and manipulation of integer keys.
 * It provides optimized operations for adding, removing, checking for existence, and iterating over integer elements.
 * Implementations of this interface offer performance advantages over general-purpose Set implementations when dealing specifically with integers.
 */
public interface FloatSet {
	
	/**
	 * {@code R} is a read-only abstract base class that implements the core functionalities and state management for {@code IntSet}.
	 * It serves as a foundation for read-write implementations and provides methods for querying the set without modification.
	 * This class is designed to be lightweight and efficient for read-heavy operations.
	 * <p>
	 * It implements {@link JsonWriter.Source} to support JSON serialization and {@link Cloneable} for creating copies of the set.
	 */
	abstract class R implements JsonWriter.Source, Cloneable {
		/**
		 * Indicates whether the Set contains a null key. Null keys are handled separately.
		 */
		protected boolean       hasNullKey;
		/**
		 * Array of buckets for the hash table. Each bucket stores the index of the first entry in its chain.
		 */
		protected int[] _buckets;
		/**
		 * Array storing the 'next' index for each entry in the hash table, used for collision resolution (chaining).
		 */
		protected int[] nexts;
		/**
		 * Array storing the integer keys of the Set.
		 */
		protected float[] keys = Array.EqualHashOf.floats     .O;
		/**
		 * The current number of elements in the Set, excluding elements in the free list.
		 */
		protected int           _count;
		/**
		 * Index of the first element in the free list. -1 indicates an empty free list.
		 */
		protected int           _freeList;
		/**
		 * The number of elements currently in the free list (available for reuse).
		 */
		protected int           _freeCount;
		/**
		 * Version number for detecting modifications during iteration, ensuring fail-fast behavior.
		 */
		protected int           _version;
		
		/**
		 * Constant representing the start of the free list chain.
		 */
		protected static final int  StartOfFreeList = -3;
		/**
		 * Mask for extracting the index from a token.
		 */
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		/**
		 * Bit shift for extracting the version from a token.
		 */
		protected static final int  VERSION_SHIFT   = 32;
		/**
		 * Represents an invalid token, typically returned when a key is not found.
		 */
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Returns the number of elements in this set (excluding any elements in the free list, but including a potential null key).
		 *
		 * @return the number of elements in this set
		 */
		public int size() {
			return _count - _freeCount + ( hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the number of elements in this set. This is an alias for {@link #size()}.
		 *
		 * @return the number of elements in this set
		 * @see #size()
		 */
		public int count() { return size(); }
		
		/**
		 * Returns {@code true} if this set contains no elements.
		 *
		 * @return {@code true} if this set contains no elements
		 */
		public boolean isEmpty() { return size() == 0; }
		
		
		/**
		 * Checks if this set contains the specified key. Handles null keys.
		 *
		 * @param key the key to check for in this set
		 * @return {@code true} if this set contains the specified key
		 */
		public boolean contains(  Float     key ) {
			return key == null ?
					hasNullKey :
					contains( key. floatValue     () );
		}
		
		/**
		 * Checks if this set contains the specified integer key.
		 *
		 * @param key the integer key to check for in this set
		 * @return {@code true} if this set contains the specified integer key
		 */
		public boolean contains( float key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Returns a token for the specified key if it exists in the set, otherwise returns {@link #INVALID_TOKEN}.
		 * Tokens are used for efficient iteration and element access. Handles null keys.
		 *
		 * @param key the key to get the token for
		 * @return a valid token if the key is in the set, {@link #INVALID_TOKEN} otherwise
		 */
		public long tokenOf(  Float     key ) {
			return key == null ?
					( hasNullKey ?
							token( _count ) :
							INVALID_TOKEN ) :
					tokenOf( key. floatValue     () );
		}
		
		/**
		 * Returns a token for the specified integer key if it exists in the set, otherwise returns {@link #INVALID_TOKEN}.
		 * Tokens are used for efficient iteration and element access.
		 *
		 * @param key the integer key to get the token for
		 * @return a valid token if the integer key is in the set, {@link #INVALID_TOKEN} otherwise
		 */
		public long tokenOf( float key ) {
			if( _buckets == null ) return INVALID_TOKEN; // Return invalid token if buckets are not initialized
			
			int hash = Array.hash( key );
			int i    = ( _buckets[ bucketIndex( hash ) ] ) - 1; // Get the index of the first entry in the bucket
			
			// Traverse the collision chain in the bucket
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key )
					return token( i ); // Key found, return its token
				i = nexts[ i ]; // Move to the next entry in the chain
				if( nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." ); // Fail-fast for concurrent modifications
			}
			return INVALID_TOKEN; // Key not found
		}
		
		/**
		 * Checks if the given token is valid for the current version of the set.
		 * A token becomes invalid if the set is modified after the token was obtained.
		 *
		 * @param token the token to validate
		 * @return {@code true} if the token is valid, {@code false} otherwise
		 */
		public boolean isValid( long token ) {
			return token != INVALID_TOKEN && version( token ) == _version;
		}
		
		/**
		 * Returns a token representing the first element in the set for iteration.
		 * If the set is empty, it returns {@link #INVALID_TOKEN}.
		 *
		 * @return a token for the first element or {@link #INVALID_TOKEN} if empty
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i ); // Find the first non-free element
			return hasNullKey ?
					token( _count ) :
					// Token for null key is at _count index (conceptually)
					INVALID_TOKEN;
		}
		
		/**
		 * Returns a token representing the next element in the set after the element associated with the given token.
		 * If the given token is the last element or invalid, it returns {@link #INVALID_TOKEN}.
		 *
		 * @param token the token of the current element
		 * @return a token for the next element or {@link #INVALID_TOKEN} if no next element exists or the token is invalid
		 * @throws ConcurrentModificationException if the collection was modified and the token is no longer valid.
		 */
		public long token( long token ) {
			if( !isValid( token ) )
				throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." ); // Fail-fast if token is invalid
			for( int i = index( token ) + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i ); // Find the next non-free element after the current index
			return hasNullKey && index( token ) < _count ?
					token( _count ) :
					// If null key exists and we haven't reached its conceptual index, return null key token
					INVALID_TOKEN;
		}
		
		/**
		 * Checks if the element associated with the given token represents a null key.
		 *
		 * @param token the token to check
		 * @return {@code true} if the token represents a null key and is valid, {@code false} otherwise
		 */
		boolean isKeyNull( long token ) { return isValid( token ) && index( token ) == _count; }
		
		/**
		 * Returns the integer key associated with the given token.
		 *
		 * @param token the token of the element
		 * @return the integer key associated with the token
		 * @throws ConcurrentModificationException if the collection was modified and the token is no longer valid.
		 */
		public float key( long token ) {
			if( isValid( token ) )
				return  ( hasNullKey && index( token ) == _count ?
						0 :
						keys[ index( token ) ] );
			throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." ); // Fail-fast if token is invalid
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
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				final int h = Array.hash( key( token ) ); // Hash each key
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
		 * Seed value used in hashCode calculation. Initialized with the hashCode of the class.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this set to the specified object for equality.
		 * Returns {@code true} if the specified object is also a {@code R} instance and represents the same set of keys.
		 *
		 * @param obj the object to compare with
		 * @return {@code true} if the objects are equal
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Compares this set to another {@code R} instance for equality.
		 * Two sets are considered equal if they have the same size, the same null key presence, and contain the same keys.
		 *
		 * @param other the {@code R} instance to compare with
		 * @return {@code true} if the sets are equal
		 */
		public boolean equals( R other ) {
			if( other == this ) return true; // Same instance check
			if( other == null || other.size() != size() || other.hasNullKey != hasNullKey ) return false; // Size and null key presence check
			for( long token = token(); index( token ) < _count; token = token( token ) )
				if( !other.contains( key( token ) ) ) return false; // Check if each key in this set is present in the other set
			return true; // All keys match
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code R} instance.
		 * The copy includes the internal arrays (_buckets, nexts, keys) but not the elements themselves.
		 *
		 * @return a clone of this {@code R} instance
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone(); // Perform shallow clone
				if( _buckets != null ) {
					dst._buckets = _buckets.clone(); // Clone buckets array
					dst.nexts    = nexts.clone();    // Clone nexts array
					dst.keys     = keys.clone();     // Clone keys array
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace(); // Handle clone exception
				return null;
			}
		}
		
		/**
		 * Writes the set's content to a {@link JsonWriter} as a JSON array.
		 *
		 * @param json the JsonWriter to write to
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			int size = size();
			json.enterArray(); // Start JSON array
			if( hasNullKey ) json.value(); // Write null value if null key is present
			if( size > 0 ) {
				json.preallocate( size * 10 ); // Pre-allocate buffer for efficiency
				for( long t = token(); isValid( t ); t = token( t ) ) json.value( key( t ) ); // Write each key as a JSON value
			}
			json.exitArray(); // End JSON array
		}
		
		/**
		 * Returns a JSON string representation of this set.
		 *
		 * @return a JSON string representation of this set
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Calculates the bucket index for a given hash value.
		 *
		 * @param hash the hash value of the key
		 * @return the bucket index
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an index and the current version.
		 *
		 * @param index the index of the element
		 * @return the generated token
		 */
		protected long token( int index ) {
			return ( ( long ) _version << VERSION_SHIFT ) | ( index & INDEX_MASK );
		}
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token the token
		 * @return the index extracted from the token
		 */
		protected int index( long token ) {
			return ( int ) ( token & INDEX_MASK );
		}
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token the token
		 * @return the version extracted from the token
		 */
		protected int version( long token ) {
			return ( int ) ( token >>> VERSION_SHIFT );
		}
	}
	
	/**
	 * {@code RW} is a read-write implementation of {@code IntSet}, extending the read-only base class {@link R}.
	 * It provides methods for modifying the set, such as adding and removing elements, as well as clearing and trimming the set's capacity.
	 * This class is suitable for scenarios where both read and write operations are frequently performed on the set.
	 */
	class RW extends R {
		
		/**
		 * Constructs an empty {@code RW} set with a default initial capacity.
		 */
		public RW() { this( 0 ); }
		
		/**
		 * Constructs an empty {@code RW} set with the specified initial capacity.
		 *
		 * @param capacity the initial capacity of the set
		 */
		public RW( int capacity ) {
			if( capacity > 0 ) initialize( capacity ); // Initialize if capacity is greater than 0
		}
		
		/**
		 * Adds the specified key to this set if it is not already present. Handles null keys.
		 *
		 * @param key the key to add to this set
		 * @return {@code true} if this set did not already contain the specified key
		 */
		public boolean add(  Float     key ) {
			return key == null ?
					addNullKey() :
					// Handle null key
					add( key. floatValue     () ); // Add integer key
		}
		
		/**
		 * Adds the specified integer key to this set if it is not already present.
		 *
		 * @param key the integer key to add to this set
		 * @return {@code true} if this set did not already contain the specified integer key
		 */
		public boolean add( float key ) {
			if( _buckets == null ) initialize( 0 ); // Initialize buckets if not already initialized
			int[] _nexts         = nexts;
			int           hash           = Array.hash( key );
			int           collisionCount = 0;
			int           bucketIndex    = bucketIndex( hash );
			int           i              = ( _buckets[ bucketIndex ] ) - 1; // Get starting index of the bucket
			
			// Check for key existence in the collision chain
			while( ( i & 0x7FFF_FFFF ) < _nexts.length ) {
				if( keys[ i ] == key ) return false; // Key already exists
				i = _nexts[ i ]; // Move to the next element in collision chain
				if( _nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." ); // Fail-fast for concurrent modification
			}
			
			int index;
			if( 0 < _freeCount ) {
				// Reuse a free slot if available
				index     = _freeList;
				_freeList = StartOfFreeList - _nexts[ _freeList ]; // Update free list pointer
				_freeCount--;
			}
			else {
				// Allocate a new slot
				if( _count == _nexts.length ) {
					resize( Array.prime( _count * 2 ) ); // Resize if full, using next prime capacity
					i = ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1; // Re-calculate bucket index after resize
				}
				index = _count++; // Increment count and get new index
			}
			
			nexts[ index ]          = ( int ) i; // Set 'next' pointer for the new element
			keys[ index ]           = ( float ) key; // Store the key
			_buckets[ bucketIndex ] = ( int ) ( index + 1 ); // Update bucket to point to the new element
			_version++; // Increment version to invalidate tokens
			
			return true; // Key added successfully
		}
		
		/**
		 * Adds a null key to the set if it is not already present.
		 *
		 * @return {@code true} if the null key was added, {@code false} if already present
		 */
		private boolean addNullKey() {
			if( hasNullKey ) return false; // Null key already exists
			hasNullKey = true; // Set null key flag
			_version++; // Increment version to invalidate tokens
			return true; // Null key added
		}
		
		
		/**
		 * Removes the specified key from this set if it is present. Handles null keys.
		 *
		 * @param key the key to remove from this set
		 * @return {@code true} if this set contained the key
		 */
		public boolean remove(  Float     key ) {
			return key == null ?
					removeNullKey() :
					// Handle null key removal
					remove( key. floatValue     () ); // Remove integer key
		}
		
		/**
		 * Removes the specified integer key from this set if it is present.
		 *
		 * @param key the integer key to remove from this set
		 * @return {@code true} if this set contained the integer key
		 */
		public boolean remove( float key ) {
			if( _buckets == null || _count == 0 ) return false; // Set is empty or not initialized
			
			int collisionCount = 0;
			int last           = -1; // Index of the previous element in the collision chain
			int hash           = Array.hash( key );
			int bucketIndex    = bucketIndex( hash );
			int i              = ( _buckets[ bucketIndex ] ) - 1; // Start index of the bucket
			
			// Traverse the collision chain
			while( -1 < i ) {
				int next = nexts[ i ];
				if( keys[ i ] == key ) {
					// Key found, remove it
					if( last < 0 ) _buckets[ bucketIndex ] = ( int ) ( next + 1 ); // If it's the first in the bucket, update bucket to point to the next
					else nexts[ last ] = next; // Otherwise, update the 'next' pointer of the previous element
					nexts[ i ] = ( int ) ( StartOfFreeList - _freeList ); // Add the removed element to the free list
					_freeList  = i; // Update free list head
					_freeCount++; // Increment free count
					_version++; // Increment version to invalidate tokens
					return true; // Key removed
				}
				last = i; // Update 'last' to the current element
				i    = next; // Move to the next element in the chain
				if( nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." ); // Fail-fast for concurrent modification
			}
			return false; // Key not found
		}
		
		/**
		 * Removes the null key from this set if it is present.
		 *
		 * @return {@code true} if the null key was removed, {@code false} if not present
		 */
		private boolean removeNullKey() {
			if( !hasNullKey ) return false; // Null key not present
			hasNullKey = false; // Clear null key flag
			_version++; // Increment version to invalidate tokens
			return true; // Null key removed
		}
		
		
		/**
		 * Removes all elements from this set.
		 */
		public void clear() {
			if( _count < 1 && !hasNullKey ) return; // Set is already empty
			Arrays.fill( _buckets, ( int ) 0 ); // Clear buckets
			Arrays.fill( nexts, 0, _count, ( int ) 0 ); // Clear 'next' pointers for used slots
			Arrays.fill( keys, 0, _count, ( float ) 0 ); // Clear keys for used slots
			_count     = 0; // Reset element count
			_freeList  = -1; // Reset free list
			_freeCount = 0; // Reset free count
			hasNullKey = false; // Clear null key flag
			_version++; // Increment version to invalidate tokens
		}
		
		
		/**
		 * Returns an array containing all of the keys in this set.
		 *
		 * @param dst the array into which the elements of this set are to be stored, if it is big enough; otherwise, a new array of the same runtime type is allocated for this purpose.
		 * @return an array containing all the keys in this set
		 */
		public float[] toArray( float[] dst ) {
			int index = 0;
			for( long token = token(); isValid( token ); token = token( token ) )
			     dst[ index++ ] = ( float ) key( token ); // Copy keys to the array using token iteration
			return dst;
		}
		
		
		/**
		 * Reduces the capacity of this set to be the set's current size.
		 * This method can be used to minimize the storage of a set instance.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Reduces the capacity of this set to be at least as large as the set's current size or the given capacity.
		 * If the given capacity is less than the current size, an {@link IllegalArgumentException} is thrown.
		 *
		 * @param capacity the desired new capacity
		 * @throws IllegalArgumentException if the capacity is less than the current size of the set.
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." ); // Capacity must be at least the current size
			int currentCapacity = nexts != null ?
					nexts.length :
					0;
			int new_size = Array.prime( capacity ); // Get the next prime capacity
			if( currentCapacity <= new_size ) return; // No need to trim if current capacity is already sufficient
			
			int[] old_next  = nexts;
			float[] old_keys  = keys;
			int           old_count = _count;
			_version++; // Increment version as structure changes
			initialize( new_size ); // Initialize with the new trimmed capacity
			copy( old_next, old_keys, old_count ); // Copy existing data to the new arrays
		}
		
		/**
		 * Initializes the internal data structures of the set with the specified capacity.
		 *
		 * @param capacity the initial capacity for the set
		 * @return the prime capacity that was actually used after rounding up to the nearest prime
		 */
		private int initialize( int capacity ) {
			capacity  = Array.prime( capacity ); // Get the next prime capacity
			_buckets  = new int[ capacity ]; // Initialize buckets array
			nexts     = new int[ capacity ]; // Initialize 'next' pointers array
			keys      = new float[ capacity ]; // Initialize keys array
			_freeList = -1; // Initialize free list as empty
			return capacity; // Return the actual capacity used
		}
		
		/**
		 * Resizes the internal arrays to a new capacity.
		 *
		 * @param newSize the new capacity for the set
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Float    .BYTES * 8 ); // Limit max size based on Integer size
			_version++; // Increment version as structure changes
			int[] new_next = Arrays.copyOf( nexts, newSize ); // Create new 'next' array with new size, copying old data
			float[] new_keys = Arrays.copyOf( keys, newSize ); // Create new 'keys' array with new size, copying old data
			final int     count    = _count;
			
			_buckets = new int[ newSize ]; // Create new buckets array with new size
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) { // Only re-hash non-free elements
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) ); // Re-calculate bucket index for each key
					new_next[ i ]           = ( int ) ( ( _buckets[ bucketIndex ] ) - 1 ); // Update 'next' pointer to the old bucket head
					_buckets[ bucketIndex ] = ( int ) ( i + 1 ); // Set new bucket head to the current element's index
				}
			
			nexts = new_next; // Replace old 'next' array with the new one
			keys  = new_keys; // Replace old 'keys' array with the new one
		}
		
		/**
		 * Copies elements from old arrays to the newly initialized arrays during resizing or trimming.
		 * Re-hashes and re-buckets the elements into the new hash table.
		 *
		 * @param old_next  the old 'next' pointers array
		 * @param old_keys  the old keys array
		 * @param old_count the number of elements in the old arrays
		 */
		private void copy( int[] old_next, float[] old_keys, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue; // Skip free slots
				nexts[ new_count ] = old_next[ i ]; // Copy 'next' pointer (though it will be overwritten in re-hashing)
				keys[ new_count ]  = old_keys[ i ]; // Copy the key
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) ); // Re-calculate bucket index in the new capacity
				nexts[ new_count ]      = ( int ) ( ( _buckets[ bucketIndex ] ) - 1 ); // Update 'next' pointer to the old bucket head (in new buckets)
				_buckets[ bucketIndex ] = ( int ) ( new_count + 1 ); // Set new bucket head to the current element's index
				new_count++; // Increment new count
			}
			_count     = new_count; // Update count to the number of copied elements
			_freeCount = 0; // Reset free count as we are starting fresh in new arrays
		}
		
	}
}