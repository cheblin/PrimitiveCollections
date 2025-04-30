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
 * {@code IntSet} is a specialized Set interface optimized for storing and manipulating integer keys.
 * It provides efficient operations for adding, removing, checking existence, and iterating over integer elements.
 * Implementations of this interface are designed to outperform general-purpose Set implementations when working with integers.
 */
public interface LongSet {
	
	/**
	 * {@code R} is a read-only abstract base class that implements core functionality and state management for {@code IntSet}.
	 * It serves as the foundation for read-write implementations, offering lightweight and efficient methods for querying the set.
	 * This class supports JSON serialization via {@link JsonWriter.Source} and cloning via {@link Cloneable}.
	 */
	abstract class R implements JsonWriter.Source, Cloneable {
		/**
		 * Indicates whether the set contains a null key, which is handled separately from integer keys.
		 */
		protected boolean hasNullKey;
		
		/**
		 * Returns whether the set contains a null key.
		 *
		 * @return {@code true} if the set contains a null key, {@code false} otherwise
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		/**
		 * Array of buckets for the hash table, where each bucket stores the index of the first entry in its chain.
		 */
		protected int[]         _buckets;
		/**
		 * Array storing the 'next' index for each entry, used for collision resolution via chaining.
		 */
		protected int[]         nexts;
		/**
		 * Array storing the integer keys of the set.
		 */
		protected long[] keys;
		/**
		 * The current number of elements in the set, excluding elements in the free list.
		 */
		protected int           _count;
		/**
		 * Index of the first element in the free list, or -1 if the free list is empty.
		 */
		protected int           _freeList;
		/**
		 * Number of elements in the free list, available for reuse.
		 */
		protected int           _freeCount;
		/**
		 * Version number for detecting modifications during iteration, enabling fail-fast behavior.
		 */
		protected int           _version;
		
		/**
		 * Constant marking the start of the free list chain.
		 */
		protected static final int StartOfFreeList = -3;
		
		/**
		 * Constant representing the index for a null key.
		 */
		protected static final int  NULL_KEY_INDEX = 0x7FFF_FFFF;
		/**
		 * Bit shift for extracting the version from a token.
		 */
		protected static final int  VERSION_SHIFT  = 32;
		/**
		 * Represents an invalid token, equal to -1.
		 */
		protected static final long INVALID_TOKEN  = -1L;
		
		/**
		 * Returns the number of elements in the set, including the null key if present.
		 *
		 * @return the number of elements in the set
		 */
		public int size() {
			return _count - _freeCount + (
					hasNullKey ?
							1 :
							0 );
		}
		
		/**
		 * Alias for {@link #size()}.
		 *
		 * @return the number of elements in the set
		 */
		public int count() { return size(); }
		
		/**
		 * Checks if the set is empty.
		 *
		 * @return {@code true} if the set contains no elements, {@code false} otherwise
		 */
		public boolean isEmpty() { return size() == 0; }
		
		
		/**
		 * Checks if the set contains the specified key, handling null keys.
		 *
		 * @param key the key to check (may be null)
		 * @return {@code true} if the key is in the set, {@code false} otherwise
		 */
		public boolean contains(  Long      key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the set contains the specified integer key.
		 *
		 * @param key the integer key to check
		 * @return {@code true} if the key is in the set, {@code false} otherwise
		 */
		public boolean contains( long key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Returns a token for the specified key if it exists, or {@link #INVALID_TOKEN} if not.
		 * Tokens enable efficient iteration and element access. Handles null keys.
		 *
		 * @param key the key to get the token for (may be null)
		 * @return a valid token if the key exists, or {@link #INVALID_TOKEN} if not
		 */
		public long tokenOf(  Long      key ) {
			return key == null ?
					hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN :
					tokenOf( key. longValue     () );
		}
		
		/**
		 * Returns a token for the specified integer key if it exists, or {@link #INVALID_TOKEN} if not.
		 *
		 * @param key the integer key to get the token for
		 * @return a valid token if the key exists, or {@link #INVALID_TOKEN} if not
		 */
		public long tokenOf( long key ) {
			if( _buckets == null ) return INVALID_TOKEN; // Return invalid token if buckets are not initialized
			
			int hash = Array.hash( key );
			int i    = _buckets[ bucketIndex( hash ) ] - 1; // Get the index of the first entry in the bucket
			
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
		 * Returns a token for the first element in the set for iteration.
		 *
		 * @return a token for the first element, or {@link #INVALID_TOKEN} if the set is empty
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i ); // Find the first non-free element
			return hasNullKey ?
					token( NULL_KEY_INDEX ) :
					// Token for null key is at _count index (conceptually)
					INVALID_TOKEN;
		}
		
		/**
		 * Returns a token for the next element after the given token.
		 *
		 * @param token the current token
		 * @return a token for the next element, or {@link #INVALID_TOKEN} if none exists or the token is invalid
		 * @throws IllegalArgumentException        if the token is {@link #INVALID_TOKEN}
		 * @throws ConcurrentModificationException if the set was modified since the token was issued
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			int i = index( token );
			if( i == NULL_KEY_INDEX ) return INVALID_TOKEN;
			
			if( 0 < _count - _freeCount )
				for( i++; i < _count; i++ )
					if( -2 < nexts[ i ] ) return token( i ); // Find the next non-free element after the current index
			
			return hasNullKey && index( token ) < _count ?
					token( NULL_KEY_INDEX ) :
					// If null key exists and we haven't reached its conceptual index, return null key token
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over non-null keys, skipping concurrency checks.
		 * Start with {@code unsafe_token(-1)} and pass the returned token back. Iteration ends when {@code -1} is returned.
		 * The null key is excluded; use {@link #hasNullKey()} and {@link #key(long)} to handle it.
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster but risks undefined behavior if the set is modified during iteration.
		 *
		 * @param token the previous token, or -1 to start iteration
		 * @return the next token for a non-null key, or -1 if no more keys exist
		 */
		public int unsafe_token( int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return i;
			return -1;
		}
		
		/**
		 * Checks if the given token represents a null key.
		 *
		 * @param token the token to check
		 * @return {@code true} if the token represents a null key, {@code false} otherwise
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the integer key for the given token.
		 * Ensure {@link #isKeyNull(long)} is false before calling.
		 *
		 * @param token the token
		 * @return the integer key, or 0 if the token represents a null key
		 */
		public long key( long token ) { return   keys[ index( token ) ]; }
		
		/**
		 * Computes the hash code for the set based on its keys and size.
		 *
		 * @return the hash code
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
		
		/**
		 * Seed value used in hash code calculation.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Checks if this set equals another object.
		 *
		 * @param obj the object to compare with
		 * @return {@code true} if the object is an {@code R} instance with the same keys
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Checks if this set equals another {@code R} instance.
		 *
		 * @param other the {@code R} instance to compare with
		 * @return {@code true} if the sets contain the same keys
		 */
		public boolean equals( R other ) {
			if( other == this ) return true; // Same instance check
			if( other == null || other.size() != size() || other.hasNullKey != hasNullKey ) return false; // Size and null key presence check
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( !other.contains( key( token ) ) ) return false; // Check if each key in this set is present in the other set
			return true; // All keys match
		}
		
		/**
		 * Creates a shallow copy of this set.
		 *
		 * @return a cloned {@code R} instance
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.nexts    = nexts.clone();
					dst.keys     = keys.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
				return null;
			}
		}
		
		/**
		 * Returns a JSON string representation of the set.
		 *
		 * @return the JSON string
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the set as a JSON array to a {@link JsonWriter}.
		 *
		 * @param json the {@link JsonWriter} to write to
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			int size = size();
			json.enterArray();
			if( hasNullKey ) json.value();
			if( size > 0 ) {
				json.preallocate( size * 10 );
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.value( key( token ) );
			}
			json.exitArray();
		}
		
		
		/**
		 * Calculates the bucket index for a given hash.
		 *
		 * @param hash the hash value
		 * @return the bucket index
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an index and the current version.
		 *
		 * @param index the element index
		 * @return the generated token
		 */
		protected long token( int index ) {
			return ( long ) _version << VERSION_SHIFT | index;
		}
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token the token
		 * @return the index
		 */
		protected int index( long token ) {
			return ( int ) token;
		}
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token the token
		 * @return the version
		 */
		protected int version( long token ) {
			return ( int ) ( token >>> VERSION_SHIFT );
		}
	}
	
	/**
	 * {@code RW} is a read-write implementation of {@code IntSet}, extending {@link R}.
	 * It supports modifying the set through operations like adding, removing, clearing, and trimming capacity.
	 * Suitable for scenarios requiring frequent read and write operations.
	 */
	class RW extends R {
		
		/**
		 * Constructs an empty set with default initial capacity.
		 */
		public RW() { this( 0 ); }
		
		/**
		 * Constructs an empty set with the specified initial capacity.
		 *
		 * @param capacity the initial capacity
		 */
		public RW( int capacity ) { if( capacity > 0 ) initialize( Array.prime( capacity ) ); }
		
		/**
		 * Adds a key to the set if not already present, handling null keys.
		 *
		 * @param key the key to add (may be null)
		 * @return {@code true} if the key was added, {@code false} if already present
		 */
		public boolean add(  Long      key ) {
			return key == null ?
					addNullKey() :
					add( key. longValue     () ); // Add integer key
		}
		
		/**
		 * Adds an integer key to the set if not already present.
		 *
		 * @param key the integer key to add
		 * @return {@code true} if the key was added, {@code false} if already present
		 */
		public boolean add( long key ) {
			if( _buckets == null ) initialize( 7 );
			int[] _nexts         = nexts;
			int   hash           = Array.hash( key );
			int   collisionCount = 0;
			int   bucketIndex    = bucketIndex( hash );
			int   bucket         = _buckets[ bucketIndex ] - 1;
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) return false;
				next = _nexts[ next ];
				if( _nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			
			int index;
			if( 0 < _freeCount ) {
				index     = _freeList;
				_freeList = StartOfFreeList - _nexts[ _freeList ];
				_freeCount--;
			}
			else {
				if( _count == _nexts.length ) {
					resize( Array.prime( _count * 2 ) );
					bucket = _buckets[ bucketIndex = bucketIndex( hash ) ] - 1;
				}
				index = _count++;
			}
			nexts[ index ]          = bucket;
			keys[ index ]           = ( long ) key;
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			return true;
		}
		
		/**
		 * Adds a null key to the set if not already present.
		 *
		 * @return {@code true} if added, {@code false} if already present
		 */
		private boolean addNullKey() {
			if( hasNullKey ) return false;
			hasNullKey = true;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes a key from the set if present, handling null keys.
		 *
		 * @param key the key to remove (may be null)
		 * @return {@code true} if the key was removed, {@code false} if not present
		 */
		public boolean remove(  Long      key ) {
			return key == null ?
					removeNullKey() :
					remove( key. longValue     () );
		}
		
		/**
		 * Removes an integer key from the set if present.
		 *
		 * @param key the integer key to remove
		 * @return {@code true} if the key was removed, {@code false} if not present
		 */
		public boolean remove( long key ) {
			if( _count - _freeCount == 0 ) return false;
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int i           = _buckets[ bucketIndex ] - 1;
			if( i < 0 ) return false;
			
			int next = nexts[ i ];
			if( keys[ i ] == key ) _buckets[ bucketIndex ] =  next + 1 ;
			else
				for( int last = i, collisionCount = 0; ; ) {
					if( ( i = next ) < 0 ) return false;
					next = nexts[ i ];
					if( keys[ i ] == key ) {
						nexts[ last ] = next;
						break;
					}
					last = i;
					if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
			
			nexts[ i ] = StartOfFreeList - _freeList ;
			_freeList  = i;
			_freeCount++;
			_version++;
			return true;
		}
		
		/**
		 * Removes the null key from the set if present.
		 *
		 * @return {@code true} if removed, {@code false} if not present
		 */
		private boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey = false;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes all elements from the set.
		 */
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( nexts, 0, _count, 0 );
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
		}
		
		
		/**
		 * Returns an array of all keys in the set.
		 *
		 * @param dst the destination array, or a new array if too small
		 * @return an array containing all keys
		 */
		public long[] toArray( long[] dst, long null_substitute ) {
			int s = size();
			if( dst.length < s ) dst = new long[ s ];
			
			int index = 0;
			if( hasNullKey ) dst[ index++ ] = null_substitute; // Convention: represent null key as char 0
			
			for( int token = -1; ( token = unsafe_token( token ) ) != INVALID_TOKEN; )
			     dst[ index++ ] = ( long ) key( token ); // Copy keys to the array using token iteration
			return dst;
		}
		
		
		/**
		 * Trims the set's capacity to its current size.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Trims the set's capacity to at least the specified capacity or the current size.
		 *
		 * @param capacity the desired capacity
		 * @throws IllegalArgumentException if capacity is less than the current size
		 */
		public void trim( int capacity ) {
			int currentCapacity = nexts != null ?
					nexts.length :
					0;
			int new_size = Array.prime( capacity ); // Get the next prime capacity
			if( currentCapacity <= new_size ) return; // No need to trim if current capacity is already sufficient
			
			int[]         old_next  = nexts;
			long[] old_keys  = keys;
			int           old_count = _count;
			_version++; // Increment version as structure changes
			initialize( new_size ); // Initialize with the new trimmed capacity
			for( int i = 0; i < old_count; i++ )
				if( -2 < old_next[ i ] ) add(  old_keys[ i ]  );
			
		}
		
		/**
		 * Initializes the set's data structures with the specified capacity.
		 *
		 * @param capacity the initial capacity
		 * @return the actual capacity used
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets   = new int[ capacity ]; // Initialize buckets array
			nexts      = new int[ capacity ]; // Initialize 'next' pointers array
			keys       = new long[ capacity ]; // Initialize keys array
			_freeList  = -1;
			_count     = 0;
			_freeCount = 0;
			return capacity;
		}
		
		/**
		 * Resizes the set's internal arrays to a new capacity.
		 *
		 * @param newSize the new capacity
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Long     .BYTES * 8 ); // Limit max size based on Integer size
			_version++; // Increment version as structure changes
			int[]         new_next = Arrays.copyOf( nexts, newSize ); // Create new 'next' array with new size, copying old data
			long[] new_keys = Arrays.copyOf( keys, newSize ); // Create new 'keys' array with new size, copying old data
			final int     count    = _count;
			
			_buckets = new int[ newSize ]; // Create new buckets array with new size
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) { // Only re-hash non-free elements
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) ); // Re-calculate bucket index for each key
					new_next[ i ]           = ( int ) ( _buckets[ bucketIndex ] - 1 ); // Update 'next' pointer to the old bucket head
					_buckets[ bucketIndex ] = i + 1; // Set new bucket head to the current element's index
				}
			
			nexts = new_next; // Replace old 'next' array with the new one
			keys  = new_keys; // Replace old 'keys' array with the new one
		}
		
		@Override public RW clone() { return ( RW ) super.clone(); }
	}
}