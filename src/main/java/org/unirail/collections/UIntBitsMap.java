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
 * A specialized map for storing integer key-value pairs with bit-packed values.
 * Uses a hash table with separate chaining for collision resolution and supports null keys.
 * Values are stored in a {@link BitsList} with configurable bits per value (1 to 7).
 */
public interface UIntBitsMap {
	
	
	/**
	 * Abstract base class providing read-only operations for the map.
	 * Implements core functionality for key-value access and iteration.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		protected boolean       hasNullKey;          // Indicates if the map contains a null key.
		protected byte          nullKeyValue;        // Value associated with the null key, stored separately.
		protected int[]         _buckets;            // Hash table buckets (1-based indices to chain heads).
		protected int[]         nexts;               // Packed entries: next index (lower 32 bits) or free list pointer.
		protected int[] keys;               // Array of integer keys.
		protected BitsList.RW   values;              // Bit-packed values array.
		protected int           _count;              // Total number of entries in arrays (including free slots).
		protected int           _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int           _freeCount;          // Number of free entries in the free list.
		protected int           _version;            // Version counter for concurrent modification detection.
		
		protected static final int  StartOfFreeList = -3; // Marks the start of the free list in 'nexts' field.
		protected static final int  VERSION_SHIFT   = 32; // Bits to shift version in token.
		protected static final int  NULL_KEY_INDEX  = 0x7FFF_FFFF; // Index representing the null key.
		protected static final long INVALID_TOKEN   = -1L; // Invalid token constant.
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return {@code true} if the map contains no key-value mappings, {@code false} otherwise.
		 */
		public boolean isEmpty() {
			return size() == 0;
		}
		
		/**
		 * Returns the number of key-value mappings in the map.
		 *
		 * @return The total number of mappings, including the null key if present.
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
		 * Checks if the map contains a mapping for the specified boxed key.
		 *
		 * @param key The key to check (boxed {@link Integer}, may be {@code null}).
		 * @return {@code true} if the key exists in the map, {@code false} otherwise.
		 */
		public boolean contains(  Long      key ) {
			return key == null ?
					hasNullKey :
					contains( key.longValue     () );
		}
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive {@code int} key.
		 * @return {@code true} if the key exists in the map, {@code false} otherwise.
		 */
		public boolean contains( long key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 *
		 * @param value The value to search for.
		 * @return {@code true} if the value exists in the map, {@code false} otherwise.
		 */
		public boolean containsValue( long value ) { return hasNullKey && nullKeyValue == value || values.contains( value ); }
		
		/**
		 * Returns a token for the specified boxed key.
		 *
		 * @param key The key to find (boxed {@link Integer}, may be {@code null}).
		 * @return A token representing the key's location if found, or {@link #INVALID_TOKEN} (-1) if not present.
		 */
		public long tokenOf(  Long      key ) {
			return key == null ?
					( hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN ) :
					tokenOf( key.longValue     () );
		}
		
		/**
		 * Returns a token for the specified primitive key.
		 *
		 * @param key The primitive {@code int} key.
		 * @return A token representing the key's location if found, or {@link #INVALID_TOKEN} (-1) if not present.
		 * @throws ConcurrentModificationException if excessive collisions are detected, indicating concurrent modification.
		 */
		public long tokenOf( long key ) {
			if( _buckets == null ) return INVALID_TOKEN;
			int hash = Array.hash( key );
			int i    = _buckets[ bucketIndex( hash ) ] - 1;
			
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key ) return token( i );
				i = nexts[ i ];
				if( nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the initial token for iteration over all keys.
		 *
		 * @return The first valid token to begin iteration, or {@link #INVALID_TOKEN} (-1) if the map is empty.
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
		 * Returns the next token for safe iteration over all keys.
		 *
		 * @param token The current token.
		 * @return The next valid token, or {@link #INVALID_TOKEN} (-1) if no more entries exist or the map was modified.
		 * @throws IllegalArgumentException        if the token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the map was modified since the token was obtained.
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
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>.
		 * Skips concurrency and modification checks for performance.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token to continue. Iteration ends when
		 * {@code -1} is returned. The null key is excluded; use {@link #hasNullKey()} and {@link #nullKeyValue()} to handle it.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but may cause skipped entries,
		 * exceptions, or undefined behavior if the map is structurally modified during iteration. Use only when modifications
		 * are guaranteed not to occur.
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #nullKeyValue() To get the null key's value.
		 */
		public int unsafe_token( final int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return i;
			return -1;
		}
		
		/**
		 * Checks if the map contains a null key.
		 *
		 * @return {@code true} if the map contains a null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the null key.
		 *
		 * @return The value associated with the null key, or undefined if no null key exists.
		 */
		public long nullKeyValue() { return (0xFFFFFFFFL &  nullKeyValue); }
		
		
		/**
		 * Checks if the token represents the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token corresponds to the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Retrieves the key associated with a token.
		 * Ensure the token does not represent the null key using {@link #isKeyNull(long)} before calling.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The integer key associated with the token.
		 * @throws ArrayIndexOutOfBoundsException if the token is invalid or represents the null key.
		 */
		public long key( long token ) { return   keys[ index( token ) ]; }
		
		/**
		 * Retrieves the value associated with a token.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The byte value associated with the token, or the null key's value if the token represents the null key.
		 */
		public byte value( long token ) {
			return isKeyNull( token ) ?
					nullKeyValue :
					values.get( index( token ) );
		}
		
		/**
		 * Returns the number of bits allocated per value.
		 *
		 * @return The number of bits per value item (1 to 7).
		 */
		public int bits_per_value() { return values.bits_per_item(); }
		
		/**
		 * Computes an order-independent hash code for the map.
		 * Consistent as long as the map's contents remain unchanged.
		 *
		 * @return The hash code of the map.
		 */
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
		 * Checks if this map equals another object.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the object is a map with the same key-value mappings, {@code false} otherwise.
		 */
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Compares this map with another {@link R} instance for equality.
		 * Two maps are equal if they contain the same key-value mappings.
		 *
		 * @param other The other map to compare.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    ( hasNullKey && nullKeyValue != other.nullKeyValue ) || size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || value( token ) != other.value( t ) ) return false;
			}
			return true;
		}
		
		/**
		 * Creates a shallow copy of the map.
		 *
		 * @return A cloned instance of the map with copied internal arrays.
		 */
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
		 * Returns a JSON representation of the map.
		 *
		 * @return A string containing the JSON representation.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Serializes the map to JSON using a {@link JsonWriter}.
		 *
		 * @param json The {@link JsonWriter} to write to.
		 */
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey ) json.name( "null" ).value( nullKeyValue );
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
			     json.name( String.valueOf( keys[ token ] ) ).value( value( token ) );
			
			json.exitObject();
		}
		
		// Helper methods
		
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
		 * @return A token combining the version and index.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index; }
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token The token.
		 * @return The index.
		 */
		protected int index( long token ) { return ( int ) ( ( int ) token ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token The token.
		 * @return The version.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * Read-write implementation of the map, extending read-only functionality from {@link R}.
	 * Supports modification operations such as put, remove, clear, and capacity management.
	 */
	class RW extends R {
		
		
		/**
		 * Constructs an empty map with default capacity and specified bits per value.
		 *
		 * @param bits_per_item The number of bits per value (1 to 7 inclusive).
		 * @throws IllegalArgumentException if bits_per_item is out of range.
		 */
		public RW( int bits_per_item ) {
			this( 0, bits_per_item );
		}
		
		/**
		 * Constructs an empty map with specified initial capacity and bits per value.
		 *
		 * @param capacity      The initial capacity (rounded to the nearest prime).
		 * @param bits_per_item The number of bits per value (1 to 7 inclusive).
		 * @throws IllegalArgumentException if bits_per_item is out of range or capacity is negative.
		 */
		public RW( int capacity, int bits_per_item ) { this( capacity, bits_per_item, 0 ); }
		
		/**
		 * Constructs an empty map with specified initial capacity, bits per value, and default value.
		 *
		 * @param capacity      The initial capacity (rounded to the nearest prime).
		 * @param bits_per_item The number of bits per value (1 to 7 inclusive).
		 * @param defaultValue  The default value for the {@link BitsList} and reset operations.
		 */
		public RW( int capacity, int bits_per_item, int defaultValue ) {
			if( capacity < 0 ) throw new NegativeArraySizeException( "capacity" );
			if( capacity > 0 )initialize( Array.prime( capacity ) );
			values = new BitsList.RW( bits_per_item, defaultValue, capacity );
		}
		
		/**
		 * Initializes the internal hash table arrays.
		 *
		 * @param capacity The initial capacity (adjusted to the nearest prime).
		 * @return The actual capacity after adjustment.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets  = new int[ capacity ];
			nexts     = new int[ capacity ];
			keys      = new int[ capacity ];
			_freeList  = -1;
			_count     = 0;
			_freeCount = 0;
			return capacity;
		}
		
		/**
		 * Associates a value with a boxed key.
		 * Updates the value if the key already exists.
		 *
		 * @param key   The key (boxed {@link Integer}, may be {@code null}).
		 * @param value The value to associate.
		 * @return {@code true} if the map was modified (new key inserted or value updated), {@code false} otherwise.
		 */
		public boolean put(  Long      key, long value ) {
			return key == null ?
					put( value ) :
					put( key.longValue     (), value );
		}
		
		
		/**
		 * Associates a value with a primitive key.
		 * Updates the value if the key already exists.
		 *
		 * @param key   The primitive {@code int} key.
		 * @param value The value to associate.
		 * @return {@code true} if the map was modified (new key inserted or value updated), {@code false} otherwise.
		 * @throws ConcurrentModificationException if excessive collisions are detected.
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
					values.set1( next, ( byte ) value );
					_version++;
					return false;
				}
				
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
					bucket = (int) ( ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1 );
				}
				index = _count++;
			}
			
			nexts[ index ] = ( int ) bucket;
			keys[ index ]  = ( int ) key;
			values.set1( index, ( byte ) value );
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			return true;
		}
		
		/**
		 * Associates a value with the null key.
		 * Updates the value if the null key already exists.
		 *
		 * @param value The value to associate with the null key.
		 * @return {@code true} if the map was modified (null key inserted or value updated), {@code false} otherwise.
		 */
		public boolean put( long value ) {
			boolean b = !hasNullKey;
			
			hasNullKey   = true;
			nullKeyValue = ( byte ) ( value & values.mask );
			_version++;
			return b;
		}
		
		/**
		 * Removes the mapping for a boxed key.
		 *
		 * @param key The key to remove (boxed {@link Integer}, may be {@code null}).
		 * @return The token of the removed entry, or {@link #INVALID_TOKEN} (-1) if the key was not found.
		 */
		public long remove(  Long      key ) {
			return key == null ?
					removeNullKey() :
					remove( key.longValue     () );
		}
		
		/**
		 * Removes the mapping for the null key.
		 *
		 * @return The token of the removed entry, or {@link #INVALID_TOKEN} (-1) if the null key was not present.
		 */
		private long removeNullKey() {
			if( !hasNullKey ) return INVALID_TOKEN;
			hasNullKey = false;
			_version++;
			return token( NULL_KEY_INDEX );
		}
		
		/**
		 * Removes the mapping for a primitive key.
		 *
		 * @param key The primitive {@code int} key to remove.
		 * @return The token of the removed entry, or {@link #INVALID_TOKEN} (-1) if the key was not found.
		 * @throws ConcurrentModificationException if excessive collisions are detected.
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
					values.set1( i, values.default_value );
					_freeList = i;
					_freeCount++;
					_version++;
					return token( i );
				}
				last = i;
				i    = next;
				if( nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Clears all mappings from the map, including the null key.
		 * Resets the map to its initial empty state.
		 */
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( nexts, 0, _count, ( int ) 0 );
			values.clear();
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
		}
		
		/**
		 * Ensures the map's capacity is at least the specified value.
		 * Resizes the map if necessary to accommodate more entries.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new capacity after adjustment.
		 * @throws IllegalArgumentException if capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity is less than 0." );
			int currentCapacity = length();
			if( capacity <= currentCapacity ) return currentCapacity;
			_version++;
			if( _buckets == null ) return initialize( Array.prime( capacity ) );
			int newSize = Array.prime( capacity );
			resize( newSize );
			return newSize;
		}
		
		/**
		 * Trims the map's capacity to the current number of entries.
		 * Reduces memory usage by compacting internal arrays.
		 */
		public void trim() { trim( size() ); }
		
		/**
		 * Trims the map's capacity to the specified value, if not less than the current size.
		 *
		 * @param capacity The desired capacity (must be at least the current size).
		 * @throws IllegalArgumentException if capacity is less than the current size.
		 */
		public void trim( int capacity ) {
			int newSize = Array.prime( capacity );
			if( length() <= newSize ) return;
			
			int[]         old_next  = nexts;
			int[] old_keys  = keys;
			int           old_count = _count;
			_version++;
			initialize( newSize );
			copy( old_next, old_keys, old_count );
		}
		
		/**
		 * Resizes the internal hash table to a new prime capacity.
		 * Rehashes and redistributes existing key-value pairs.
		 *
		 * @param newSize The new capacity (must be prime).
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Long     .BYTES * 8 );
			_version++;
			_version++;
			int[]         new_next = Arrays.copyOf( nexts, newSize );
			int[] new_keys = Arrays.copyOf( keys, newSize );
			final int     count    = _count;
			
			_buckets = new int[ newSize ];
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = ( int ) ( _buckets[ bucketIndex ] - 1 );
					_buckets[ bucketIndex ] = ( i + 1 );
				}
			
			nexts = new_next;
			keys  = new_keys;
		}
		
		/**
		 * Copies live entries from old arrays to new arrays during trimming.
		 * Skips free entries to compact the map.
		 *
		 * @param old_next  The old nexts array.
		 * @param old_keys  The old keys array.
		 * @param old_count The old count of entries (including free slots).
		 */
		private void copy( int[] old_next, int[] old_keys, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue;
				keys[ new_count ] = old_keys[ i ];
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) );
				nexts[ new_count ]      = ( int ) ( _buckets[ bucketIndex ] - 1 );
				_buckets[ bucketIndex ] = ( new_count + 1 );
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		/**
		 * Creates a shallow copy of the map.
		 *
		 * @return A cloned instance of the map with copied internal arrays.
		 */
		@Override
		public RW clone() {
			return ( RW ) super.clone();
		}
	}
}