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
 * {@code IntSet} is a specialized Set interface designed for efficient storage and manipulation of integer keys.
 * It provides optimized operations for adding, removing, checking for existence, and iterating over integer elements.
 * Implementations of this interface offer performance advantages over general-purpose Set implementations when dealing specifically with integers.
 */
public interface DoubleSet {
	
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
		protected boolean hasNullKey;
		
		public boolean hasNullKey() { return hasNullKey; }
		
		
		/**
		 * Array of buckets for the hash table. Each bucket stores the index of the first entry in its chain.
		 */
		protected int[]         _buckets;
		/**
		 * Array storing the 'next' index for each entry in the hash table, used for collision resolution (chaining).
		 */
		protected int[]         nexts;
		/**
		 * Array storing the integer keys of the Set.
		 */
		protected double[] keys = Array.EqualHashOf.doubles     .O;
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
		 * Represents an invalid token, equal to -1.
		 */
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Returns the number of elements in this set (excluding any elements in the free list, but including a potential null key).
		 *
		 * @return the number of elements in this set
		 */
		public int size() {
			return _count - _freeCount + (
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
		 * Checks if this set contains the specified key. Handles null keys.
		 *
		 * @param key the key to check for in this set
		 * @return {@code true} if this set contains the specified key
		 */
		public boolean contains(  Double    key ) {
			return key == null ?
					hasNullKey :
					contains( key. doubleValue     () );
		}
		
		/**
		 * Checks if this set contains the specified integer key.
		 *
		 * @param key the integer key to check for in this set
		 * @return {@code true} if this set contains the specified integer key
		 */
		public boolean contains( double key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Returns a token for the specified key if it exists in the set, otherwise returns {@link #INVALID_TOKEN}.
		 * Tokens are used for efficient iteration and element access. Handles null keys.
		 *
		 * @param key the key to get the token for (can be null)
		 * @return a valid token if the key is in the set, -1 ({@link #INVALID_TOKEN}) if not found
		 */
		public long tokenOf(  Double    key ) {
			return key == null ?
					( hasNullKey ?
							token( _count ) :
							INVALID_TOKEN ) :
					tokenOf( key. doubleValue     () );
		}
		
		/**
		 * Returns a token for the specified integer key if it exists in the set, otherwise returns {@link #INVALID_TOKEN}.
		 * Tokens are used for efficient iteration and element access.
		 *
		 * @param key the integer key to get the token for
		 * @return a valid token if the key is in the set, -1 ({@link #INVALID_TOKEN}) if not found
		 */
		public long tokenOf( double key ) {
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
		 * Returns a token representing the first element in the set for iteration.
		 *
		 * @return a token for the first element, or -1 ({@link #INVALID_TOKEN}) if the set is empty
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
		 *
		 * @param token the token of the current element
		 * @return a token for the next element, or -1 ({@link #INVALID_TOKEN}) if no next element exists or if the token is invalid due to structural modification
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN || version( token ) != _version ) return INVALID_TOKEN;
			for( int i = index( token ) + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i ); // Find the next non-free element after the current index
			return hasNullKey && index( token ) < _count ?
					token( _count ) :
					// If null key exists and we haven't reached its conceptual index, return null key token
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #key(long)} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #key(long) To get the key associated with a token.
		 */
		public int unsafe_token( int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return i;
			return -1;
		}
		
		/**
		 * Checks if the element associated with the given token represents a null key.
		 *
		 * @param token the token to check
		 * @return {@code true} if the token represents a null key, {@code false} otherwise
		 */
		boolean isKeyNull( long token ) { return index( token ) == _count; }
		
		/**
		 * Returns the integer key associated with the given token.
		 * The result is undefined if the token is -1 ({@link #INVALID_TOKEN}) or invalid due to structural modification.
		 *
		 * @param token the token of the element
		 * @return the integer key associated with the token, or 0 if the token represents a null key
		 */
		public double key( long token ) {
			return  ( hasNullKey && index( token ) == _count ?
					0 :
					keys[ index( token ) ] );
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
		 *
		 * @param obj the object to compare with
		 * @return {@code true} if the specified object is an {@code R} instance with the same keys
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Compares this set to another {@code R} instance for equality.
		 *
		 * @param other the {@code R} instance to compare with
		 * @return {@code true} if the sets contain the same keys
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
		 * Returns a JSON string representation of this set.
		 *
		 * @return a JSON string representation of this set
		 */
		@Override
		public String toString() { return toJSON(); }
		
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
				for( long t = token(); t != INVALID_TOKEN; t = token( t ) ) json.value( key( t ) ); // Write each key as a JSON value
			}
			json.exitArray(); // End JSON array
		}
		
		
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
		public RW( int capacity ) { if( capacity > 0 ) initialize( capacity ); }
		
		/**
		 * Adds the specified key to this set if it is not already present. Handles null keys.
		 *
		 * @param key the key to add to this set
		 * @return {@code true} if this set did not already contain the specified key
		 */
		public boolean add(  Double    key ) {
			return key == null ?
					addNullKey() :
					add( key. doubleValue     () ); // Add integer key
		}
		
		/**
		 * Adds the specified integer key to this set if it is not already present.
		 *
		 * @param key the integer key to add to this set
		 * @return {@code true} if this set did not already contain the specified integer key
		 */
		public boolean add( double key ) {
			if( _buckets == null ) initialize( 7 ); // Initialize buckets if not already initialized
			int[] _nexts         = nexts;
			int   hash           = Array.hash( key );
			int   collisionCount = 0;
			int   bucketIndex    = bucketIndex( hash );
			int   bucket         = _buckets[ bucketIndex ] - 1; // Get starting index of the bucket
			
			// Check for key existence in the collision chain
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) return false; // Key already exists
				next = _nexts[ next ]; // Move to the next element in collision chain
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
					bucket = ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1; // Re-calculate bucket index after resize
				}
				index = _count++; // Increment count and get new index
			}
			
			nexts[ index ]          = ( int ) bucket; // Set 'next' pointer for the new element
			keys[ index ]           = ( double ) key; // Store the key
			_buckets[ bucketIndex ] = index + 1; // Update bucket to point to the new element
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
		public boolean remove(  Double    key ) {
			return key == null ?
					removeNullKey() :
					// Handle null key removal
					remove( key. doubleValue     () ); // Remove integer key
		}
		
		/**
		 * Removes the specified integer key from this set if it is present.
		 *
		 * @param key the integer key to remove from this set
		 * @return {@code true} if this set contained the integer key
		 */
		public boolean remove( double key ) {
			if( _buckets == null || _count == 0 ) return false; // Set is empty or not initialized
			
			int collisionCount = 0;
			int last           = -1; // Index of the previous element in the collision chain
			int hash           = Array.hash( key );
			int bucketIndex    = bucketIndex( hash );
			int i              = _buckets[ bucketIndex ] - 1; // Start index of the bucket
			
			// Traverse the collision chain
			while( -1 < i ) {
				int next = nexts[ i ];
				if( keys[ i ] == key ) {
					// Key found, remove it
					if( last < 0 ) _buckets[ bucketIndex ] = ( next + 1 ); // If it's the first in the bucket, update bucket to point to the next
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
			Arrays.fill( _buckets, 0 ); // Clear buckets
			Arrays.fill( nexts, 0, _count, ( int ) 0 ); // Clear 'next' pointers for used slots
			Arrays.fill( keys, 0, _count, ( double ) 0 ); // Clear keys for used slots
			_count     = 0; // Reset element count
			_freeList  = -1; // Reset free list
			_freeCount = 0; // Reset free count
			hasNullKey = false; // Clear null key flag
			_version++; // Increment version to invalidate tokens
		}
		
		
		/**
		 * Returns an array containing all of the keys in this set.
		 *
		 * @param dst the array into which the elements of this set are to be stored, if it is big enough; otherwise, a new array is allocated
		 * @return an array containing all the keys in this set
		 */
		public double[] toArray( double[] dst ) {
			int index = 0;
			for( long token = token(); token != INVALID_TOKEN; token = token( token ) )
			     dst[ index++ ] = ( double ) key( token ); // Copy keys to the array using token iteration
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
			
			int[]         old_next  = nexts;
			double[] old_keys  = keys;
			int           old_count = _count;
			_version++; // Increment version as structure changes
			initialize( new_size ); // Initialize with the new trimmed capacity
			copy( old_next, old_keys, old_count ); // Copy existing data to the new arrays
		}
		
		/**
		 * Initializes the internal data structures of the set with the specified capacity.
		 *
		 * @param capacity the initial capacity for the set
		 * @return the prime capacity used
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets  = new int[ capacity ]; // Initialize buckets array
			nexts     = new int[ capacity ]; // Initialize 'next' pointers array
			keys      = new double[ capacity ]; // Initialize keys array
			_freeList = -1; // Initialize free list as empty
			return capacity; // Return the actual capacity used
		}
		
		/**
		 * Resizes the internal arrays to a new capacity.
		 *
		 * @param newSize the new capacity for the set
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Double   .BYTES * 8 ); // Limit max size based on Integer size
			_version++; // Increment version as structure changes
			int[]         new_next = Arrays.copyOf( nexts, newSize ); // Create new 'next' array with new size, copying old data
			double[] new_keys = Arrays.copyOf( keys, newSize ); // Create new 'keys' array with new size, copying old data
			final int     count    = _count;
			
			_buckets = new int[ newSize ]; // Create new buckets array with new size
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) { // Only re-hash non-free elements
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) ); // Re-calculate bucket index for each key
					new_next[ i ]           = ( int ) ( _buckets[ bucketIndex ] - 1 ); // Update 'next' pointer to the old bucket head
					_buckets[ bucketIndex ] = ( i + 1 ); // Set new bucket head to the current element's index
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
		private void copy( int[] old_next, double[] old_keys, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue; // Skip free slots
				keys[ new_count ] = old_keys[ i ]; // Copy the key
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) ); // Re-calculate bucket index in the new capacity
				nexts[ new_count ]      = ( int ) ( _buckets[ bucketIndex ] - 1 ); // Update 'next' pointer to the old bucket head (in new buckets)
				_buckets[ bucketIndex ] = ( new_count + 1 ); // Set new bucket head to the current element's index
				new_count++; // Increment new count
			}
			_count     = new_count; // Update count to the number of copied elements
			_freeCount = 0; // Reset free count as we are starting fresh in new arrays
		}
		
	}
}