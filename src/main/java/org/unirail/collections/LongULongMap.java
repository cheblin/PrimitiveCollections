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
import java.util.Objects;

/**
 * A specialized Map interface for storing integer key-value pairs with efficient operations.
 * Supports null keys and implements a hash table with separate chaining for collision resolution.
 */
public interface LongULongMap {
	
	
	/**
	 * Abstract base class providing read-only operations for the map.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		protected boolean           hasNullKey;          // Indicates if the map contains a null key.
		protected long       nullKeyValue;        // Value for the null key, stored separately.
		protected int[]             _buckets;            // Hash table buckets array (1-based indices to chain heads).
		protected int[]             nexts;          // Packed entries: hashCode (upper 32 bits) | next index (lower 32 bits).
		protected long[]     keys = Array.EqualHashOf.longs     .O; // Keys array.
		protected long[]     values;              // Values array.
		protected int               _count;              // Total number of entries in arrays (including free slots).
		protected int               _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int               _freeCount;          // Number of free entries in the free list.
		protected int               _version;            // Version counter for concurrent modification detection.
		
		protected static final int  StartOfFreeList = -3; // Marks the start of the free list in 'next' field.
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL; // Mask for index in token.
		protected static final int  VERSION_SHIFT   = 32; // Bits to shift version in token.
		
		protected static final long INVALID_TOKEN = -1L; // Invalid token constant.
		
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return True if the map contains no key-value mappings.
		 */
		public boolean isEmpty() {
			return size() == 0;
		}
		
		/**
		 * Returns the number of key-value mappings in the map.
		 *
		 * @return The total number of mappings, including null key if present.
		 */
		public int size() {
			return _count - _freeCount +
			       ( hasNullKey ?
					       1 :
					       0 );
		}
		
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
		 * Checks if the map contains a mapping for the specified key.
		 *
		 * @param key The key to check (boxed Integer).
		 * @return True if the key exists in the map.
		 */
		public boolean contains(  Long      key ) {
			return key == null ?
					hasNullKey :
					contains( key.longValue     () );
		}
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive int key.
		 * @return True if the key exists in the map.
		 */
		public boolean contains( long key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 *
		 * @param value The value to search for.
		 * @return True if the value exists in the map.
		 */
		public boolean containsValue( long value ) {
			if( hasNullKey && nullKeyValue == value ) return true;
			if( _count == 0 ) return false;
			
			for( int i = 0; i < nexts.length; i++ )
				if( -2 < nexts[ i ] && values[ i ] == value ) return true;
			return false;
		}
		
		/**
		 * Returns a token for the specified key (boxed Integer).
		 *
		 * @param key The key to find (can be null).
		 * @return A token representing the key's location if found, or -1 (INVALID_TOKEN) if not found.
		 */
		public long tokenOf(  Long      key ) {
			return key == null ?
					( hasNullKey ?
							token( _count ) :
							INVALID_TOKEN ) :
					tokenOf( key.longValue     () );
		}
		
		/**
		 * Returns a token for the specified primitive key.
		 *
		 * @param key The primitive int key.
		 * @return A token representing the key's location if found, or -1 (INVALID_TOKEN) if not found.
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
		 * Returns the initial token for iteration.
		 *
		 * @return The first valid token to begin iteration, or -1 (INVALID_TOKEN) if the map is empty.
		 */
		public long token() {
			
			if( 0 < _count )
				for( int i = 0; ; i++ )
					if( -2 < nexts[ i ] ) return token( i );
			
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next token in iteration.
		 *
		 * @param token The current token.
		 * @return The next valid token for iteration, or -1 (INVALID_TOKEN) if there are no more entries or the token is invalid due to structural modification.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN || version( token ) != _version ) return INVALID_TOKEN;
			
			for( int i = index( token ) + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i );
			return hasNullKey && index( token ) < _count ?
					token( _count ) :
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
		
		
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the null key. Behavior undefined if null key doesn't exist.
		 */
		public long nullKeyValue() { return  nullKeyValue; }
		
		public boolean hasKey( long token ) { return index( token ) < _count || hasNullKey; }
		
		/**
		 * Retrieves the key associated with a token.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The integer key associated with the token, or undefined if the token is -1 (INVALID_TOKEN) or invalid due to structural modification.
		 */
		public long key( long token ) { return  ( keys[ index( token ) ] ); }
		
		/**
		 * Retrieves the value associated with a token.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The integer value associated with the token, or undefined if the token is -1 (INVALID_TOKEN) or invalid due to structural modification.
		 */
		public long value( long token ) {
			return ( index( token ) == _count ?
					nullKeyValue :
					values[ index( token ) ] );
		}
		
		/**
		 * Computes an order-independent hash code.
		 *
		 * @return The hash code of the map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
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
		 * Compares this map with another R instance for equality.
		 *
		 * @param other The other map to compare.
		 * @return True if the maps are equal.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    ( hasNullKey && Objects.equals( nullKeyValue, other.nullKeyValue ) ) || size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || value( token ) != other.value( t ) ) return false;
			}
			
			return true;
		}
		
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
		
		@Override
		public String toString() { return toJSON(); }
		
		
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
		
		protected long token( int index )   { return ( long ) _version << VERSION_SHIFT | index & INDEX_MASK; }
		
		protected int index( long token )   { return ( int ) ( token & INDEX_MASK ); }
		
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * Read-write implementation of the map, extending read-only functionality.
	 */
	class RW extends R {
		
		/**
		 * Constructs an empty map with default capacity.
		 */
		public RW() {
			this( 0 );
		}
		
		/**
		 * Constructs an empty map with specified initial capacity.
		 *
		 * @param capacity The initial capacity.
		 */
		public RW( int capacity ) {
			if( capacity > 0 ) initialize( capacity );
		}
		
		/**
		 * Initializes the internal arrays with a specified capacity.
		 *
		 * @param capacity The desired capacity.
		 * @return The initialized capacity (prime number).
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets  = new int[ capacity ];
			nexts     = new int[ capacity ];
			keys      = new long[ capacity ];
			values    = new long[ capacity ];
			_freeList = -1;
			return capacity;
		}
		
		/**
		 * Associates a value with a key (boxed Integer).
		 *
		 * @param key   The key (can be null).
		 * @param value The value.
		 * @return True if the map was modified structurally.
		 */
		public boolean put(  Long      key, long value ) {
			return key == null ?
					this.put( value ) :
					put( key, value );
		}
		
		
		/**
		 * Associates a value with a primitive key.
		 *
		 * @param key   The primitive int key.
		 * @param value The value.
		 * @return True if the map was modified structurally.
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
					bucket = ( ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1 );
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
		 * Inserts a value for the null key.
		 *
		 * @param value The value.
		 * @return True if the null key was newly inserted.
		 */
		public boolean put( long value ) {
			boolean b = !hasNullKey;
			
			hasNullKey   = true;
			nullKeyValue = ( long ) value;
			_version++;
			return b;
		}
		
		/**
		 * Removes a key-value pair (boxed Integer key).
		 *
		 * @param key The key to remove.
		 * @return The token of the removed entry if found and removed, or -1 (INVALID_TOKEN) if not found.
		 */
		public long remove(  Long      key ) {
			return key == null ?
					removeNullKey() :
					remove( key.longValue     () );
		}
		
		/**
		 * Removes the null key mapping.
		 *
		 * @return The token of the removed entry if found and removed, or -1 (INVALID_TOKEN) if not present.
		 */
		private long removeNullKey() {
			
			if( !hasNullKey ) return INVALID_TOKEN;
			hasNullKey = false;
			_version++;
			return token( _count );
		}
		
		/**
		 * Removes a key-value pair (primitive key).
		 *
		 * @param key The primitive key to remove.
		 * @return The token of the removed entry if found and removed, or -1 (INVALID_TOKEN) if not found.
		 */
		public long remove( long key ) {
			if( _buckets == null || _count == 0 ) return INVALID_TOKEN;
			
			int collisionCount = 0;
			int last           = -1;
			int hash           = Array.hash( key );
			int bucketIndex    = bucketIndex( hash );
			int i              = _buckets[ bucketIndex ] - 1;
			
			while( -1 < i ) {
				int next = nexts[ i ];
				if( keys[ i ] == key ) {
					if( last < 0 ) _buckets[ bucketIndex ] = ( next + 1 );
					else nexts[ last ] = next;
					nexts[ i ] = ( int ) ( StartOfFreeList - _freeList );
					_freeList  = i;
					_freeCount++;
					_version++;
					return token( i );
				}
				last = i;
				i    = next;
				if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Clears all mappings from the map.
		 */
		public void clear() {
			hasNullKey = false;
			if( _count == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( nexts, ( int ) 0 );
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
			_version++;
		}
		
		/**
		 * Ensures the map’s capacity is at least the specified value.
		 *
		 * @param capacity The minimum capacity.
		 * @return The new capacity.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			_version++;
			if( _buckets == null ) return initialize( capacity );
			int newSize = Array.prime( capacity );
			resize( newSize );
			return newSize;
		}
		
		/**
		 * Trims the capacity to the current number of entries.
		 */
		public void trim() {
			trim( size() );
		}
		
		/**
		 * Trims the capacity to the specified value, at least the current count.
		 *
		 * @param capacity The desired capacity.
		 */
		public void trim( int capacity ) {
			int currentCapacity = length();
			int newSize         = Array.prime( capacity );
			if( currentCapacity <= newSize ) return;
			
			int[]         old_next   = nexts;
			long[] old_keys   = keys;
			long[] old_values = values;
			int           old_count  = _count;
			_version++;
			initialize( newSize );
			copy( old_next, old_keys, old_values, old_count );
		}
		
		/**
		 * Resizes the map to a new prime capacity.
		 *
		 * @param newSize The new size (prime number).
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
					_buckets[ bucketIndex ] = ( i + 1 );
				}
			
			nexts  = new_next;
			keys   = new_keys;
			values = new_values;
		}
		
		/**
		 * Copies entries during trimming.
		 *
		 * @param old_next   Old hash_nexts array.
		 * @param old_keys   Old keys array.
		 * @param old_values Old values array.
		 * @param old_count  Old count.
		 */
		private void copy( int[] old_next, long[] old_keys, long[] old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue;
				keys[ new_count ]   = old_keys[ i ];
				values[ new_count ] = old_values[ i ];
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) );
				nexts[ new_count ]      = ( int ) ( _buckets[ bucketIndex ] - 1 );
				_buckets[ bucketIndex ] = ( new_count + 1 );
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		
		@Override
		public RW clone() {
			return ( RW ) super.clone();
		}
	}
}