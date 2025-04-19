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

import java.util.*;
import java.util.function.Function;

/**
 * A generic Set implementation providing efficient storage and operations for keys.
 * Supports null keys and provides token-based iteration for safe and unsafe traversal.
 * The implementation uses a hash table with open addressing for efficient key storage.
 */
public interface ObjectSet {
	
	/**
	 * Read-only base class providing core functionality and state management for the set.
	 * Implements {@link java.util.Set} and {@link JsonWriter.Source} for JSON serialization.
	 */
	abstract class R< K > implements java.util.Set< K >, JsonWriter.Source, Cloneable {
		protected boolean                hasNullKey;    // True if the set contains a null key
		protected int[]                  _buckets;       // Hash table buckets
		protected long[]                 hash_nexts;    // Set entries: hashCode | next
		protected K[]                    keys;             // Set elements (keys)
		protected int                    _count;           // Number of elements excluding free list
		protected int                    _freeList;        // Index of the first free list element
		protected int                    _freeCount;       // Number of free list elements
		protected int                    _version;         // Version for modification detection
		protected Array.EqualHashOf< K > equal_hash_K; // Equality and hash provider
		
		protected static final int  StartOfFreeList = -3;
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		protected static final int  NULL_KEY_INDEX  = 0x7FFF_FFFF;
		protected static final int  VERSION_SHIFT   = 32;
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Checks if the set contains a null key.
		 *
		 * @return {@code true} if the set contains a null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() {
			return hasNullKey;
		}
		
		/**
		 * Returns the number of elements in the set, including the null key if present.
		 *
		 * @return The size of the set.
		 */
		@Override
		public int size() {
			return _count - _freeCount + ( hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the number of elements in the set (alias for {@link #size()}).
		 *
		 * @return The number of elements in the set.
		 */
		public int count() {
			return size();
		}
		
		/**
		 * Checks if the set is empty.
		 *
		 * @return {@code true} if the set contains no elements, {@code false} otherwise.
		 */
		@Override
		public boolean isEmpty() {
			return size() == 0;
		}
		
		/**
		 * Checks if the set contains the specified key.
		 *
		 * @param key The key to check for.
		 * @return {@code true} if the set contains the key, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean contains( Object key ) {
			return tokenOf( ( K ) key ) != INVALID_TOKEN;
		}
		
		/**
		 * Checks if the set contains the specified key (type-safe version).
		 *
		 * @param key The key to check for.
		 * @return {@code true} if the set contains the key, {@code false} otherwise.
		 */
		public boolean contains_( K key ) {
			return tokenOf( key ) != INVALID_TOKEN;
		}
		
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
			if( _buckets == null ) return INVALID_TOKEN;
			
			int hash = equal_hash_K.hashCode( key );
			int i    = _buckets[ bucketIndex( hash ) ] - 1;
			
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < hash_nexts.length; ) {
				final long hash_next = hash_nexts[ i ];
				if( hash( hash_next ) == hash && equal_hash_K.equals( keys[ i ], key ) )
					return token( i );
				i = next( hash_next );
				if( hash_nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the first valid iteration token for traversing the set.
		 * The token points to the first valid element or the null key if present and no other elements exist.
		 * Returns {@link #INVALID_TOKEN} if the set is empty.
		 *
		 * @return The first iteration token, or {@link #INVALID_TOKEN} if the set is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( next( hash_nexts[ i ] ) >= -1 ) return token( i );
			return hasNullKey ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN;
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
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			int i = index( token );
			if( i == NULL_KEY_INDEX ) return INVALID_TOKEN;
			if( 0 < _count - _freeCount )
				for( i++; i < _count; i++ )
					if( -2 < next( hash_nexts[ i ] ) ) return token( i );
			return hasNullKey && index( token ) < _count ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN;
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
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #key(long) To get the key associated with a token.
		 */
		public int unsafe_token( int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return i;
			return -1;
		}
		
		/**
		 * Checks if the given token corresponds to the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) {
			return index( token ) == NULL_KEY_INDEX;
		}
		
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
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< K > ) obj );
		}
		
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
					dst._buckets   = _buckets.clone();
					dst.hash_nexts = hash_nexts.clone();
					dst.keys       = keys.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
				return null;
			}
		}
		
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
					json.preallocate( size * 10 );
					for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
					     json.name( key( token ).toString() ).value();
				}
				json.exitObject();
			}
			else {
				json.enterArray();
				if( hasNullKey ) json.value();
				if( size > 0 ) {
					json.preallocate( size * 10 );
					for( long t = token(); t != INVALID_TOKEN; t = token( t ) ) json.value( key( t ) );
				}
				json.exitArray();
			}
		}
		
		/**
		 * Returns a JSON string representation of the set.
		 *
		 * @return The JSON string representation.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Computes the bucket index for a given hash code.
		 *
		 * @param hash The hash code.
		 * @return The bucket index.
		 */
		protected int bucketIndex( int hash ) {
			return ( hash & 0x7FFF_FFFF ) % _buckets.length;
		}
		
		/**
		 * Extracts the hash code from a hash_next entry.
		 *
		 * @param hash_next The hash_next entry.
		 * @return The hash code.
		 */
		protected static int hash( long hash_next ) {
			return ( int ) ( hash_next >> 32 );
		}
		
		/**
		 * Extracts the next index from a hash_next entry.
		 *
		 * @param hash_next The hash_next entry.
		 * @return The next index.
		 */
		protected static int next( long hash_next ) {
			return ( int ) ( hash_next & NEXT_MASK );
		}
		
		/**
		 * Creates a token from an index.
		 *
		 * @param index The index.
		 * @return The token combining the version and index.
		 */
		protected long token( int index ) {
			return ( ( long ) _version << VERSION_SHIFT ) | ( index );
		}
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token The token.
		 * @return The index.
		 */
		protected int index( long token ) {
			return ( int ) ( token );
		}
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token The token.
		 * @return The version.
		 */
		protected int version( long token ) {
			return ( int ) ( token >>> VERSION_SHIFT );
		}
	}
	
	/**
	 * Read-write implementation extending the read-only base class.
	 * Provides methods to add, remove, and modify elements in the set.
	 */
	class RW< K > extends R< K > {
		private static final int                                                        HashCollisionThreshold = 100;
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs an empty set for the specified key class.
		 *
		 * @param clazzK The class of the keys.
		 */
		public RW( Class< K > clazzK ) {
			this( clazzK, 0 );
		}
		
		/**
		 * Constructs an empty set with the specified initial capacity for the key class.
		 *
		 * @param clazzK   The class of the keys.
		 * @param capacity The initial capacity.
		 */
		public RW( Class< K > clazzK, int capacity ) {
			this( Array.get( clazzK ), capacity );
		}
		
		/**
		 * Constructs an empty set with the specified equality and hash provider.
		 *
		 * @param equal_hash_K The equality and hash provider.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K ) {
			this( equal_hash_K, 0 );
		}
		
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
			
			if( _buckets == null ) initialize( 7 );
			long[] _hash_nexts    = hash_nexts;
			int    hash           = equal_hash_K.hashCode( key );
			int    collisionCount = 0;
			int    bucketIndex    = bucketIndex( hash );
			int    bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _hash_nexts.length; ) {
				if( hash( _hash_nexts[ next ] ) == hash && equal_hash_K.equals( keys[ next ], key ) )
					return false;
				next = next( _hash_nexts[ next ] );
				if( _hash_nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			
			int index;
			if( 0 < _freeCount ) {
				index     = _freeList;
				_freeList = StartOfFreeList - next( _hash_nexts[ _freeList ] );
				_freeCount--;
			}
			else {
				if( _count == _hash_nexts.length ) {
					resize( Array.prime( _count * 2 ), false );
					bucket = _buckets[ bucketIndex = bucketIndex( hash ) ] - 1;
				}
				index = _count++;
			}
			
			hash_nexts[ index ]     = hash_next( hash, bucket );
			keys[ index ]           = key;
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			if( HashCollisionThreshold < collisionCount && this.forceNewHashCodes != null && key instanceof String )
				resize( hash_nexts.length, true );
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
		public boolean remove( Object key ) {
			return remove_( ( K ) key );
		}
		
		/**
		 * Removes the specified key from the set if it is present (type-safe version).
		 *
		 * @param key The key to remove.
		 * @return {@code true} if the key was removed, {@code false} if it was not present.
		 */
		public boolean remove_( K key ) {
			if( key == null ) {
				if( !hasNullKey ) return false;
				hasNullKey = false;
				_version++;
				return true;
			}
			
			if( _buckets == null || _count == 0 ) return false;
			
			int collisionCount = 0;
			int last           = -1;
			int hash           = equal_hash_K.hashCode( key );
			int bucketIndex    = bucketIndex( hash );
			int i              = _buckets[ bucketIndex ] - 1;
			
			while( -1 < i ) {
				long hash_next = hash_nexts[ i ];
				if( hash( hash_next ) == hash && equal_hash_K.equals( keys[ i ], key ) ) {
					if( last < 0 ) _buckets[ bucketIndex ] = next( hash_next ) + 1;
					else next( hash_nexts, last, next( hash_next ) );
					next( hash_nexts, i, StartOfFreeList - _freeList );
					keys[ i ] = null;
					_freeList = i;
					_freeCount++;
					_version++;
					return true;
				}
				last = i;
				i    = next( hash_next );
				if( hash_nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return false;
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
			
			// Handle non-null keys
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) {
				if( keys.contains( key( t ) ) ) {
					remove( key( t ) );
					t = unsafe_token( -1 ); // Reset iteration after removal
				}
			}
			
			// Handle null key if present
			if( hasNullKey && keys.contains( null ) ) {
				remove( null );
			}
			
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
			
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; )
				if( !keys.contains( key( t ) ) ) {
					remove( key( t ) );
					t = unsafe_token( -1 );
				}
			
			return v != _version;
		}
		
		/**
		 * Removes all elements from the set.
		 */
		@Override
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count < 1 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( hash_nexts, 0, _count, 0L );
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
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
		 *
		 * @param capacity The desired capacity.
		 * @throws IllegalArgumentException If the capacity is less than the current size.
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = hash_nexts != null ?
					hash_nexts.length :
					0;
			int new_size = Array.prime( capacity );
			if( currentCapacity <= new_size ) return;
			
			long[] old_hash_next = hash_nexts;
			K[]    old_keys      = keys;
			int    old_count     = _count;
			_version++;
			initialize( new_size );
			copy( old_hash_next, old_keys, old_count );
		}
		
		/**
		 * Initializes the set's internal arrays with the specified capacity.
		 *
		 * @param capacity The capacity to initialize.
		 * @return The initialized capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets   = new int[ capacity ];
			hash_nexts = new long[ capacity ];
			keys       = equal_hash_K.copyOf( null, capacity );
			_freeList  = -1;
			_count     = 0;
			_freeCount = 0;
			return capacity;
		}
		
		/**
		 * Resizes the set to the specified size, optionally forcing new hash codes.
		 *
		 * @param new_size          The new size.
		 * @param forceNewHashCodes Whether to force new hash codes.
		 */
		private void resize( int new_size, boolean forceNewHashCodes ) {
			_version++;
			long[]    new_hash_next = Arrays.copyOf( hash_nexts, new_size );
			K[]       new_keys      = Arrays.copyOf( keys, new_size );
			final int count         = _count;
			
			if( forceNewHashCodes && this.forceNewHashCodes != null ) {
				equal_hash_K = this.forceNewHashCodes.apply( equal_hash_K );
				for( int i = 0; i < count; i++ )
					if( next( new_hash_next[ i ] ) >= -2 )
						hash( new_hash_next, i, equal_hash_K.hashCode( keys[ i ] ) );
			}
			
			_buckets = new int[ new_size ];
			for( int i = 0; i < count; i++ )
				if( next( new_hash_next[ i ] ) > -2 ) {
					int bucketIndex = bucketIndex( hash( new_hash_next[ i ] ) );
					next( new_hash_next, i, _buckets[ bucketIndex ] - 1 );
					_buckets[ bucketIndex ] = i + 1;
				}
			
			hash_nexts = new_hash_next;
			keys       = new_keys;
		}
		
		/**
		 * Copies elements from old arrays to new ones, maintaining the set's structure.
		 *
		 * @param old_hash_next The old hash_next array.
		 * @param old_keys      The old keys array.
		 * @param old_count     The old count of elements.
		 */
		private void copy( long[] old_hash_next, K[] old_keys, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				final long hn = old_hash_next[ i ];
				if( next( hn ) < -1 ) continue;
				
				keys[ new_count ] = old_keys[ i ];
				int h           = hash( hn );
				int bucketIndex = bucketIndex( h );
				hash_nexts[ new_count ] = hash_next( h, _buckets[ bucketIndex ] - 1 );
				
				_buckets[ bucketIndex ] = new_count + 1;
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		/**
		 * Creates a hash_next entry from a hash code and next index.
		 *
		 * @param hash The hash code.
		 * @param next The next index.
		 * @return The hash_next entry.
		 */
		private static long hash_next( int hash, int next ) {
			return ( ( long ) hash << 32 ) | ( next & NEXT_MASK );
		}
		
		/**
		 * Sets the next index in a hash_next entry.
		 *
		 * @param dst   The hash_next array.
		 * @param index The index to modify.
		 * @param next  The new next index.
		 */
		private static void next( long[] dst, int index, int next ) {
			dst[ index ] = ( dst[ index ] & HASH_CODE_MASK ) | ( next & NEXT_MASK );
		}
		
		/**
		 * Sets the hash code in a hash_next entry.
		 *
		 * @param dst   The hash_next array.
		 * @param index The index to modify.
		 * @param hash  The new hash code.
		 */
		private static void hash( long[] dst, int index, int hash ) {
			dst[ index ] = ( dst[ index ] & NEXT_MASK ) | ( ( long ) hash << 32 );
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
				_currentToken = INVALID_TOKEN;
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
				if( _version != _set._version ) throw new ConcurrentModificationException( "Collection was modified; enumeration operation may not execute." );
				_currentToken =
						_currentToken == INVALID_TOKEN ?
								_set.token() :
								_set.token( _currentToken );
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
				_currentKey = _set.key( _currentToken );
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
				if( _version != _set._version ) throw new ConcurrentModificationException( "Collection was modified; enumeration operation may not execute." );
				
				_set.remove_( _currentKey );
				_currentKey = null;
				_version    = _set._version;
			}
		}
		
		@Override public RW< K > clone() { return ( RW< K > ) super.clone(); }
		
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