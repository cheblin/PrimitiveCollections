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
 * A specialized map implementation for storing integer key-value pairs with efficient operations.
 * Supports null keys and uses a hash table with separate chaining for collision resolution.
 * Provides read-only operations in the abstract base class {@code R} and read-write operations in the subclass {@code RW}.
 */
public interface LongLongMap {
	
	
	/**
	 * Abstract base class providing read-only operations for the map.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		protected boolean           hasNullKey;          // Indicates if the map contains a null key.
		protected long       nullKeyValue;        // Value for the null key, stored separately.
		protected int[]             _buckets;            // Hash table buckets array (1-based indices to chain heads).
		protected int[]             nexts;               // Packed entries: next index in collision chain.
		protected long[]     keys; // Keys array.
		protected long[]     values;              // Values array.
		protected int               _count;              // Total number of entries in arrays (including free slots).
		protected int               _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int               _freeCount;          // Number of free entries in the free list.
		protected int               _version;            // Version counter for concurrent modification detection.
		
		protected static final int  StartOfFreeList = -3; // Marks the start of the free list in 'nexts' field.
		protected static final int  VERSION_SHIFT   = 32; // Bits to shift version in token.
		protected static final int  NULL_KEY_INDEX  = 0x7FFF_FFFF; // Index representing the null key.
		protected static final long INVALID_TOKEN   = -1L; // Invalid token constant.
		
		
		/**
		 * Checks if the map contains no key-value mappings.
		 *
		 * @return {@code true} if the map is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() {
			return size() == 0;
		}
		
		/**
		 * Returns the number of key-value mappings in the map, including the null key if present.
		 *
		 * @return The total number of mappings.
		 */
		public int size() {
			return _count - _freeCount +
			       ( hasNullKey ?
					       1 :
					       0 );
		}
		
		/**
		 * Alias for {@link #size()}.
		 *
		 * @return The total number of mappings.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the allocated capacity of the internal arrays.
		 *
		 * @return The length of the internal arrays, or 0 if uninitialized.
		 */
		public int length() {
			return nexts == null ?
					0 :
					nexts.length;
		}
		
		/**
		 * Checks if the map contains a mapping for the specified boxed Integer key.
		 *
		 * @param key The key to check (may be {@code null}).
		 * @return {@code true} if the key exists in the map, {@code false} otherwise.
		 */
		public boolean containsKey(  Long      key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a mapping for the specified primitive int key.
		 *
		 * @param key The primitive int key to check.
		 * @return {@code true} if the key exists in the map, {@code false} otherwise.
		 */
		public boolean containsKey( long key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 *
		 * @param value The value to search for.
		 * @return {@code true} if the value exists in the map, {@code false} otherwise.
		 */
		public boolean containsValue( long value ) {
			if( hasNullKey && nullKeyValue == value ) return true;
			if( _count == 0 ) return false;
			
			for( int i = 0; i < nexts.length; i++ )
				if( -2 < nexts[ i ] && values[ i ] == value ) return true;
			return false;
		}
		
		/**
		 * Returns a token for the specified boxed Integer key.
		 *
		 * @param key The key to find (may be {@code null}).
		 * @return A token representing the key's location if found, or {@code INVALID_TOKEN} if not found.
		 */
		public long tokenOf(  Long      key ) {
			return key == null ?
					hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN :
					tokenOf( key.longValue     () );
		}
		
		/**
		 * Returns a token for the specified primitive int key.
		 *
		 * @param key The primitive int key to find.
		 * @return A token representing the key's location if found, or {@code INVALID_TOKEN} if not found.
		 * @throws ConcurrentModificationException if the map is structurally modified during lookup.
		 */
		public long tokenOf( long key ) {
			if( _buckets == null ) return INVALID_TOKEN;
			int hash = Array.hash( key );
			int i    = _buckets[ bucketIndex( hash ) ] - 1;
			
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key ) return token( i );
				i = nexts[ i ];
				if( nexts.length < ++collisionCount ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the initial token for iterating over the map's entries.
		 *
		 * @return The first valid token to begin iteration, or {@code INVALID_TOKEN} if the map is empty.
		 */
		public long token() {
			
			if( 0 < _count - _freeCount )
				for( int i = 0; ; i++ )
					if( -2 < nexts[ i ] ) return token( i );
			
			return hasNullKey ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next token in the iteration sequence.
		 *
		 * @param token The current token.
		 * @return The next valid token, or {@code INVALID_TOKEN} if no more entries exist or the token is invalid.
		 * @throws ConcurrentModificationException if the map is structurally modified during iteration.
		 * @throws IllegalArgumentException        if the token is {@code INVALID_TOKEN}.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			int i = index( token );
			if( i == NULL_KEY_INDEX ) return INVALID_TOKEN;
			
			if( 0 < _count - _freeCount )
				for( i++; i < _count; i++ )
					if( -2 < nexts[ i ] ) return token( i );
			
			return hasNullKey && index( token ) < _count ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #nullKeyValue()} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #nullKeyValue() To get the null key’s value.
		 */
		public int unsafe_token( final int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return i;
			return -1;
		}
		
		/**
		 * Checks if the map contains a null key.
		 *
		 * @return {@code true} if the null key is present, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the null key.
		 *
		 * @return The value for the null key; behavior is undefined if no null key exists (check {@link #hasNullKey()} first).
		 */
		public long nullKeyValue() { return  nullKeyValue; }
		
		/**
		 * Checks if the specified token corresponds to the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Retrieves the key associated with the specified token.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The key associated with the token; behavior is undefined if the token is {@code INVALID_TOKEN} or invalid.
		 * @throws IllegalStateException if the token points to the null key (use {@link #isKeyNull(long)} to check).
		 */
		public long key( long token ) { return  keys[ index( token ) ]; }
		
		/**
		 * Retrieves the value associated with the specified token.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The value associated with the token; behavior is undefined if the token is {@code INVALID_TOKEN} or invalid.
		 */
		public long value( long token ) {
			return  (isKeyNull( token ) ?
					nullKeyValue :
					values[ index( token ) ]);
		}
		
		/**
		 * Computes an order-independent hash code for the map.
		 *
		 * @return The hash code based on all key-value pairs.
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
		
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		/**
		 * Compares this map with another {@code R} instance for equality.
		 *
		 * @param other The other map to compare with.
		 * @return {@code true} if the maps contain the same key-value pairs, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null ||
			    hasNullKey != other.hasNullKey || hasNullKey && nullKeyValue != other.nullKeyValue ||
			    size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || value( token ) != other.value( t ) ) return false;
			}
			
			return true;
		}
		
		/**
		 * Creates a shallow copy of this map.
		 * <p>
		 * enem @return A cloned instance of the map, or {@code null} if cloning fails.
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.nexts    = nexts.clone();
					dst.keys     = keys.clone();
					dst.values   = values.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		/**
		 * Returns a JSON string representation of the map.
		 *
		 * @return The map as a JSON string.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the map’s contents to a {@code JsonWriter}.
		 *
		 * @param json The {@code JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     json.name( String.valueOf( keys[ index( token ) ] ) ).value( value( token ) );
			
			json.exitObject();
		}
		
		// Helper methods
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		protected long token( int index )   { return ( long ) _version << VERSION_SHIFT | index; }
		
		protected int index( long token )   { return ( int ) ( int ) token; }
		
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * Read-write implementation of the map, extending the read-only functionality of {@code R}.
	 */
	class RW extends R {
		
		/**
		 * Constructs an empty map with default capacity (0, initialized on first put).
		 */
		public RW() {
			this( 0 );
		}
		
		/**
		 * Constructs an empty map with the specified initial capacity.
		 *
		 * @param capacity The initial capacity (will be adjusted to a prime number if greater than 0).
		 */
		public RW( int capacity ) {
			if( capacity > 0 )initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Initializes the internal arrays with the specified capacity, adjusted to a prime number.
		 *
		 * @param capacity The desired initial capacity.
		 * @return The actual initialized capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets  = new int[ capacity ];
			nexts     = new int[ capacity ];
			keys      = new long[ capacity ];
			values    = new long[ capacity ];
			_freeList = -1;
			_count    = 0;
			_freeCount =0;
			return capacity;
		}
		
		/**
		 * Associates a value with the specified boxed Integer key, handling null keys.
		 *
		 * @param key   The key to associate (may be {@code null}).
		 * @param value The value to store as an integer.
		 * @return {@code true} if the map was structurally modified (new entry added), {@code false} if an existing key’s value was updated.
		 */
		public boolean put(  Long      key, long value ) {
			return key == null ?
					this.put( value ) :
					put( (long)(key + 0), value );
		}
		
		
		/**
		 * Associates a value with the specified primitive int key.
		 *
		 * @param key   The primitive int key to associate.
		 * @param value The value to store as an integer.
		 * @return {@code true} if the map was structurally modified (new entry added), {@code false} if an existing key’s value was updated.
		 * @throws ConcurrentModificationException if the map is structurally modified during insertion.
		 */
		public boolean put( long key, long value ) {
			if( _buckets == null ) initialize( 7 );
			int[] _nexts         = nexts;
			int   hash           = Array.hash( key );
			int   collisionCount = 0;
			int   bucketIndex    = bucketIndex( hash );
			int   bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) {
					values[ next ] = ( long ) value;
					_version++;
					return false;
				}
				
				next = _nexts[ next ];
				if( _nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
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
			
			nexts[ index ]          = ( int ) bucket;
			keys[ index ]           = ( long ) key;
			values[ index ]         = ( long ) value;
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			return true;
		}
		
		/**
		 * Associates a value with the null key.
		 *
		 * @param value The value to store as an integer for the null key.
		 * @return {@code true} if the null key was newly inserted, {@code false} if its value was updated.
		 */
		public boolean put( long value ) {
			boolean b = !hasNullKey;
			
			hasNullKey   = true;
			nullKeyValue = ( long ) value;
			_version++;
			return b;
		}
		
		/**
		 * Removes the mapping for the specified boxed Integer key.
		 *
		 * @param key The key to remove (may be {@code null}).
		 * @return The token of the removed entry if found and removed, or {@code INVALID_TOKEN} if not found.
		 */
		public boolean remove(  Long      key ) {
			return key == null ?
					removeNullKey() :
					remove( key.longValue     () );
		}
		
		/**
		 * Removes the mapping for the null key.
		 *
		 * @return The token of the removed null key entry if it existed, or {@code INVALID_TOKEN} if not present.
		 */
		private boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey = false;
			_version++;
			return true;
		}
		
		/**
		 * Removes the mapping for the specified primitive int key.
		 *
		 * @param key The primitive int key to remove.
		 * @return The token of the removed entry if found and removed, or {@code INVALID_TOKEN} if not found.
		 * @throws ConcurrentModificationException if the map is structurally modified during removal.
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
		 * Removes all mappings from the map.
		 */
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( nexts, ( int ) 0 );
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
		}
		
		/**
		 * Ensures the map’s capacity is at least the specified value, resizing if necessary.
		 *
		 * @param capacity The minimum capacity required.
		 * @return The new capacity of the internal arrays.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			_version++;
			if( _buckets == null ) return initialize( Array.prime( capacity ) );
			int newSize = Array.prime( capacity );
			resize( newSize );
			return newSize;
		}
		
		/**
		 * Trims the map’s capacity to match the current number of entries.
		 */
		public void trim() {
			trim( size() );
		}
		
		/**
		 * Trims the map’s capacity to the specified value, ensuring it is at least the current number of entries.
		 *
		 * @param capacity The desired capacity (will be adjusted to a prime number).
		 */
		public void trim( int capacity ) {
			int currentCapacity = length();
			int newSize         = Array.prime( capacity );
			if( currentCapacity <= newSize ) return;
			
			int[]         old_next   = nexts;
			long[] old_keys   = keys;
			long[] old_values = values;
			int           old_count  = _count;
			initialize( newSize );
			for( int i = 0; i < old_count; i++ )
				if( -2 < old_next[ i ] ) put(  old_keys[ i ], old_values[ i ] );
		}
		
		/**
		 * Resizes the map to a new capacity, adjusted to a prime number.
		 *
		 * @param newSize The desired new capacity; capped at a maximum based on key type constraints.
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Long     .BYTES * 8 );
			_version++;
			int[]         new_next   = Arrays.copyOf( nexts, newSize );
			long[] new_keys   = Arrays.copyOf( keys, newSize );
			long[] new_values = Arrays.copyOf( values, newSize );
			final int     count      = _count;
			
			_buckets = new int[ newSize ];
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = ( int ) ( _buckets[ bucketIndex ] - 1 ); //relink chain
					_buckets[ bucketIndex ] = i + 1;
				}
			
			nexts  = new_next;
			keys   = new_keys;
			values = new_values;
		}
		
		
		/**
		 * Creates a shallow copy of this map.
		 *
		 * @return A cloned instance of the map.
		 */
		@Override
		public RW clone() {
			return ( RW ) super.clone();
		}
	}
}