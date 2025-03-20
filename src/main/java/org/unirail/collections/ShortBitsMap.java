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
 * A specialized Map interface for storing integer key-value pairs with efficient operations.
 * Supports null keys and implements a hash table with separate chaining for collision resolution.
 */
public interface ShortBitsMap {
	
	
	/**
	 * Abstract base class providing read-only operations for the map.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		protected boolean       hasNullKey;          // Indicates if the map contains a null key.
		protected byte          nullKeyValue;        // Value for the null key, stored separately.
		protected int[] _buckets;            // Hash table buckets array (1-based indices to chain heads).
		protected int[] nexts;          // Packed entries: hashCode (upper 32 bits) | next index (lower 32 bits).
		protected short[] keys = Array.EqualHashOf.shorts     .O; // Keys array.
		protected BitsList.RW   values;              // Values array.
		protected int           _count;              // Total number of entries in arrays (including free slots).
		protected int           _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int           _freeCount;          // Number of free entries in the free list.
		protected int           _version;            // Version counter for concurrent modification detection.
		
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
			return _count - _freeCount + ( hasNullKey ?
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
		public boolean contains(  Short     key ) {
			return key == null ?
					hasNullKey :
					contains( key.shortValue     () );
		}
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive int key.
		 * @return True if the key exists in the map.
		 */
		public boolean contains( short key ) {
			return tokenOf( key ) != INVALID_TOKEN;
		}
		
		/**
		 * Checks if the map contains the specified value.
		 *
		 * @param value The value to search for.
		 * @return True if the value exists in the map.
		 */
        public boolean containsValue(long value) { return _count != 0 || hasNullKey && values.contains(value); }
		
		/**
		 * Returns a token for the specified key (boxed Integer).
		 *
         * @param key The key to find (can be null).
         * @return A token representing the key's location if found, or {@link #INVALID_TOKEN} (-1) if the key is not present.
		 */
		public long tokenOf(  Short     key ) {
			return key == null ?
					( hasNullKey ?
							token( _count ) :
							INVALID_TOKEN ) :
					tokenOf( key.shortValue     () );
		}
		
		/**
		 * Returns a token for the specified primitive key.
		 *
		 * @param key The primitive int key.
         * @return A token representing the key's location if found, or {@link #INVALID_TOKEN} (-1) if the key is not present.
		 */
		public long tokenOf( short key ) {
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
		 * Returns the initial token for iteration.
		 *
         * @return The first valid token to begin iteration, or {@link #INVALID_TOKEN} (-1) if the map is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i );
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next token in iteration.
		 *
		 * @param token The current token.
         * @return The next valid token for iteration, or {@link #INVALID_TOKEN} (-1) if there are no more entries or if the map was modified since the token was obtained.
		 */
		public long token( long token ) {
            if (token == INVALID_TOKEN || version(token) != _version) return INVALID_TOKEN;
			for( int i = index( token ) + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i );
            return hasNullKey && index(token) < _count ? token(_count) : INVALID_TOKEN;
		}
		
		/**
		 * Checks if the map contains a mapping for the null key.
		 *
		 * @return True if the map contains a null key, false otherwise.
		 */
		public boolean hasNullKey() {
			return hasNullKey;
		}
		
		public short nullKeyValue() { return (short) nullKeyValue; }
		
	
		
		/**
		 * Checks if the map contains a key associated with the given token.
		 *
		 * @param token The token to check.
         * @return True if a key is associated with the token, false if the token is {@link #INVALID_TOKEN} (-1) or invalid due to map modifications.
		 */
		public boolean hasKey( long token ) {
            return  (index(token) < _count || hasNullKey);
		}
		
		/**
		 * Retrieves the key associated with a token.
		 *
		 * @param token The token representing the key-value pair.
         * @return The integer key associated with the token, or undefined behavior if the token is {@link #INVALID_TOKEN} (-1) or invalid due to map modifications.
		 */
		public short key( long token ) { return (short)  keys[ index( token ) ]; }
		
		/**
		 * Retrieves the value associated with a token.
		 *
		 * @param token The token representing the key-value pair.
         * @return The byte value associated with the token, or undefined behavior if the token is {@link #INVALID_TOKEN} (-1) or invalid due to map modifications.
		 */
		public byte value( long token ) {
				return index( token ) == _count ?
						nullKeyValue :
						values.get( index( token ) ); // Handle null key value.
		}
		
		/**
		 * Computes an order-independent hash code for the map.
		 * This hash code is consistent as long as the map's contents remain unchanged.
		 *
		 * @return The hash code of the map.
		 */
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			{
				int h = Array.mix( seed, values.hashCode() );
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
		
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Compares this map with another R instance for equality.
		 * Two maps are considered equal if they contain the same key-value mappings.
		 *
		 * @param other The other map to compare.
		 * @return True if the maps are equal, false otherwise.
		 */
		public boolean equals( R other ) {
			if( other == null || hasNullKey != other.hasNullKey ||
			    ( hasNullKey && nullKeyValue != other.nullKeyValue ) || size() != other.size() )
				return false;
			
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || value( token ) != other.value( t ) ) return false;
			}
			return true;
		}
		
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
		
		public String toString() {
			return toJSON();
		}
		
		public String toJSON() {
			return JsonWriter.Source.super.toJSON();
		}
		
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey ) json.name( "null" ).value( nullKeyValue ); // Represent null key explicitly in JSON
			
			for( long token = token(); index( token ) < _count; token = token( token ) )
			     json.name( String.valueOf( keys[ index( token ) ] ) ).value( value( token ) );
			
			json.exitObject();
		}
		
		// Helper methods
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
        protected long token(int index) { return (long) _version << VERSION_SHIFT | index & INDEX_MASK; }
		
        protected int index(long token) { return (int) (token & INDEX_MASK); }
		
		protected int version( long token ) {			return ( int ) ( token >>> VERSION_SHIFT );		}
	}
	
	/**
	 * Read-write implementation of the map, extending read-only functionality provided by {@link R}.
	 * Allows modification operations such as put, remove, and clear.
	 */
	class RW extends R {
		
		
		/**
		 * Constructs an empty map with default capacity and a specified number of bits per value item.
		 *
		 * @param bits_per_item The number of bits to allocate for each value item in the internal {@link BitsList}.
		 */
		public RW( int bits_per_item ) {
			this( 0, bits_per_item );
		}
		
		/**
		 * Constructs an empty map with a specified initial capacity and bits per value item.
		 *
		 * @param capacity      The initial capacity of the map. It will be rounded up to the nearest prime number.
		 * @param bits_per_item The number of bits per value item.
		 */
		public RW( int capacity, int bits_per_item ) { this( capacity, bits_per_item, 0 ); }
		
		/**
		 * Constructs an empty map with a specified initial capacity, bits per value item, and default value.
		 *
		 * @param capacity      The initial capacity of the map. Rounded up to the nearest prime number.
		 * @param bits_per_item The number of bits per value item.
		 * @param defaultValue  The default value used when initializing the {@link BitsList} and for reset operations.
		 */
		public RW( int capacity, int bits_per_item, int defaultValue ) {
			if( capacity > 0 ) initialize( capacity );
			values = new BitsList.RW( bits_per_item, defaultValue, capacity );
		}
		
		/**
		 * Initializes the internal hash table arrays.
		 *
		 * @param capacity The initial capacity of the hash table. Will be adjusted to the nearest prime number greater than or equal to capacity.
		 * @return The actual capacity after adjusting to a prime number.
		 */
		private int initialize( int capacity ) {
			capacity  = Array.prime( capacity );
			_buckets  = new int[ capacity ];
			nexts     = new int[ capacity ];
			keys      = new short[ capacity ];
			_freeList = -1;
			return capacity;
		}
		
		/**
		 * Associates a value with a key (boxed Integer).
		 * If the key already exists, the value is updated.
		 *
		 * @param key   The key (can be null).
		 * @param value The value to be associated with the key.
		 * @return True if the map was modified structurally (key was newly inserted or value was updated), false otherwise.
		 */
		public boolean put(  Short     key, long value ) {
			return key == null ?
					tryInsert( value, 1 ) :
					tryInsert( key, value, 1 );
		}
		
		/**
		 * Associates a value with a primitive key.
		 * If the key already exists, the value is updated.
		 *
		 * @param key   The primitive int key.
		 * @param value The value to be associated with the key.
		 * @return True if the map was modified structurally (key was newly inserted or value was updated), false otherwise.
		 */
		public boolean put( short key, long value ) {
			return tryInsert( key, value, 1 );
		}
		
		/**
		 * Associates a value with the null key.
		 * If the null key already exists, the value is updated.
		 *
		 * @param value The value to be associated with the null key.
		 * @return True if the map was modified structurally (null key was newly inserted or value was updated), false otherwise.
		 */
		public boolean put( long value ) {
			return tryInsert( value, 1 );
		}
		
		/**
		 * Inserts a key-value pair only if the key doesn’t already exist in the map.
		 *
		 * @param key   The key to insert.
		 * @param value The value to associate with the key.
		 * @return True if the key-value pair was inserted because the key did not exist, false if the key already existed.
		 */
		public boolean putNotExist( short key, long value ) {
			return tryInsert( key, value, 2 );
		}
		
		/**
		 * Inserts a key-value pair only if the key doesn’t already exist in the map (boxed Integer key).
		 *
		 * @param key   The key to insert (boxed Integer, can be null).
		 * @param value The value to associate with the key.
		 * @return True if the key-value pair was inserted, false if the key already existed.
		 */
		public boolean putNotExist(  Short     key, long value ) {
			return key == null ?
					tryInsert( value, 2 ) :
					tryInsert( key.shortValue     (), value, 2 );
		}
		
		/**
		 * Inserts a value for the null key only if the null key doesn't already exist.
		 *
		 * @param value The value to associate with the null key.
		 * @return True if the null key-value pair was inserted, false if the null key already existed.
		 */
		public boolean putNotExist( long value ) {
			return tryInsert( value, 2 );
		}
		
		/**
		 * Inserts a key-value pair, throwing an exception if the key already exists.
		 *
		 * @param key   The key to insert.
		 * @param value The value to associate with the key.
		 * @throws IllegalArgumentException If a mapping for the specified key already exists in the map.
		 */
		public void putTry( short key, long value ) {
			tryInsert( key, value, 0 );
		}
		
		/**
		 * Inserts a key-value pair (boxed Integer key), throwing an exception if the key already exists.
		 *
		 * @param key   The key to insert (boxed Integer, can be null).
		 * @param value The value to associate with the key.
		 * @throws IllegalArgumentException If a mapping for the specified key already exists in the map.
		 */
		public void putTry(  Short     key, long value ) {
			if( key == null )
				tryInsert( value, 0 );
			else
				tryInsert( key.shortValue     (), value, 0 );
		}
		
		/**
		 * Inserts a value for the null key, throwing an exception if the null key already exists.
		 *
		 * @param value The value to associate with the null key.
		 * @throws IllegalArgumentException If a mapping for the null key already exists in the map.
		 */
		public void putTry( long value ) {
			tryInsert( value, 0 );
		}
		
		/**
		 * Core insertion logic with behavior control.
		 * Handles insertion, update, and "put if not exists" semantics for integer keys.
		 *
		 * @param key      The primitive key to insert or update.
		 * @param value    The value to associate with the key.
		 * @param behavior 0=throw if exists, 1=put (update/insert), 2=put if not exists.
		 * @return True if the map was structurally modified (insertion or update), false otherwise (only for behavior 2 if key exists).
		 * @throws IllegalArgumentException        if behavior is 0 and the key already exists.
		 * @throws ConcurrentModificationException if concurrent modification is detected during collision resolution.
		 */
		private boolean tryInsert( short key, long value, int behavior ) {
			if( _buckets == null ) initialize( 0 );
			int[] _nexts         = nexts;
			int           hash           = Array.hash( key );
			int           collisionCount = 0;
			int           bucketIndex    = bucketIndex( hash );
			int           bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key )
					switch( behavior ) {
						case 1:
							values.set1( next, ( byte ) value );
							_version++;
							return true;
						case 0:
							throw new IllegalArgumentException( "An item with the same key has already been added. Key: " + key );
						default:
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
					bucket =  ( ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1 );
				}
				index = _count++;
			}
			
			nexts[ index ] = ( int ) bucket;
			keys[ index ]  = ( short ) key;
			values.set1( index, ( byte ) value );
			_buckets[ bucketIndex ] =   index + 1;
			_version++;
			
			return true;
		}
		
		/**
		 * Handles null key insertion.
		 *
		 * @param value    The value for the null key.
		 * @param behavior 0=throw if exists, 1=put (update/insert), 2=put if not exists.
		 * @return True if the map was structurally modified (insertion or update), false otherwise (only for behavior 2 if key exists).
		 * @throws IllegalArgumentException if behavior is 0 and the null key already exists.
		 */
		private boolean tryInsert( long value, int behavior ) {
			if( hasNullKey ) switch( behavior ) {
				case 1:
                    break;
				case 0:
					throw new IllegalArgumentException( "An item with the same key has already been added. Key: null" );
				default:
					return false;
			}
			hasNullKey   = true;
			nullKeyValue = ( byte ) value;
			_version++;
			return true;
		}
		
		/**
		 * Removes a key-value pair associated with the given key (boxed Integer).
		 *
		 * @param key The key to remove (boxed Integer, can be null).
         * @return The token of the removed entry if the key was found and removed, or {@link #INVALID_TOKEN} (-1) if the key was not found.
		 */
		public long remove(  Short     key ) {
			return key == null ?
					removeNullKey() :
					remove( key.shortValue     () );
		}
		
		/**
		 * Removes the mapping for the null key.
		 *
         * @return The token of the removed entry if the null key was found and removed, or {@link #INVALID_TOKEN} (-1) if the null key was not present.
		 */
		private long removeNullKey() {
			if( !hasNullKey ) return INVALID_TOKEN;
			hasNullKey = false;
			_version++;
            return token(_count);
		}
		
		/**
		 * Removes a key-value pair associated with the given primitive key.
		 *
		 * @param key The primitive key to remove.
         * @return The token of the removed entry if the key was found and removed, or {@link #INVALID_TOKEN} (-1) if the key was not found.
		 * @throws ConcurrentModificationException if concurrent modification is detected during collision resolution.
		 */
		public long remove( short key ) {
			if( _buckets == null || _count == 0 ) return INVALID_TOKEN;
			
			int collisionCount = 0;
			int last           = -1;
			int hash           = Array.hash( key );
			int bucketIndex    = bucketIndex( hash );
			int i              = _buckets[ bucketIndex ] - 1;
			
			while( -1 < i ) {
				int next = nexts[ i ];
				if( keys[ i ] == key ) {
					if( last < 0 ) _buckets[ bucketIndex ] =  ( next + 1 ); // Update bucket head if removing the head of the chain
					else nexts[ last ] = next; // Update the 'next' pointer of the previous entry in the chain
					nexts[ i ] = ( int ) ( StartOfFreeList - _freeList ); // Mark the removed entry as free and point to the old free list head
					values.set1( i, values.default_value ); // Reset value to default
					_freeList = i; // Update free list head to the newly freed entry
					_freeCount++; // Increment free count
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
		 * Clears all mappings from the map, including the null key mapping.
		 * Resets the map to its initial empty state.
		 */
		public void clear() {
			if( _count == 0 && !hasNullKey ) return;
			Arrays.fill( _buckets,  0 );
			Arrays.fill( nexts, 0, _count, ( int ) 0 );
			Arrays.fill( keys, 0, _count, ( short ) 0 );
			values.clear();
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
			hasNullKey = false;
			_version++;
		}
		
		/**
		 * Ensures the map’s capacity is at least the specified value.
		 * If the current capacity is less than the specified capacity, the map is resized to accommodate more entries.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The new capacity of the map after ensuring it meets the minimum requirement.
		 * @throws IllegalArgumentException if the capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity is less than 0." );
			int currentCapacity = length();
			if( capacity <= currentCapacity ) return currentCapacity;
			_version++;
			if( _buckets == null ) return initialize( capacity );
			int newSize = Array.prime( capacity );
			resize( newSize );
			return newSize;
		}
		
		/**
		 * Trims the capacity of the map to the current number of entries, effectively reducing memory usage.
		 * If the current capacity is already at or below the size, no action is taken.
		 */
		public void trim() {
			trim( size() );
		}
		
		/**
		 * Trims the capacity of the map to the specified value, but only if it's not less than the current size.
		 * If the specified capacity is less than the current size, an IllegalArgumentException is thrown.
		 *
		 * @param capacity The desired capacity to trim to. Must be at least the current size of the map.
		 * @throws IllegalArgumentException if the capacity is less than the current size of the map.
		 */
		public void trim( int capacity ) {
			if( capacity < size() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			int newSize         = Array.prime( capacity );
			if( currentCapacity <= newSize ) return;
			
			int[] old_next  = nexts;
			short[] old_keys  = keys;
			int           old_count = _count;
			_version++;
			initialize( newSize );
			copy( old_next, old_keys, old_count );
		}
		
		/**
		 * Resizes the internal hash table to a new prime capacity.
		 * Rehashes and redistributes existing key-value pairs into the new buckets.
		 *
		 * @param newSize The new size of the hash table (must be a prime number).
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Short    .BYTES * 8 ); // Limit max size to avoid integer overflow
			_version++; // Increment version before and after array operations for invalidation safety during resize
			_version++;
			int[] new_next = Arrays.copyOf( nexts, newSize );
			short[] new_keys = Arrays.copyOf( keys, newSize );
			final int     count    = _count;
			
			_buckets = new int[ newSize ]; // Create new bucket array with the new size
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) { // Skip free list entries
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) ); // Recalculate bucket index in the new bucket array size
					new_next[ i ]           = ( int ) ( _buckets[ bucketIndex ] - 1 ); // Prepend current entry to the new bucket's chain
					_buckets[ bucketIndex ] =  ( i + 1 ); // Update bucket head to the current entry's index
				}
			
			nexts = new_next; // Replace old arrays with new resized arrays
			keys  = new_keys;
		}
		
		/**
		 * Copies entries from old arrays to the newly resized arrays during trimming.
		 * Only copies live entries, skipping entries marked as free in the old arrays.
		 *
		 * @param old_next  Old hash_nexts array.
		 * @param old_keys  Old keys array.
		 * @param old_count Old count of entries (including free slots).
		 */
		private void copy( int[] old_next, short[] old_keys, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue; // Skip free list entries
				nexts[ new_count ] = old_next[ i ]; // Copy 'next' value
				keys[ new_count ]  = old_keys[ i ];  // Copy key
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) ); // Calculate new bucket index for the resized bucket array
				nexts[ new_count ]      = ( int ) ( _buckets[ bucketIndex ] - 1 ); // Prepend to new bucket chain
				_buckets[ bucketIndex ] =  ( new_count + 1 ); // Update bucket head
				new_count++; // Increment count for the new compacted array
			}
			_count     = new_count; // Update the main count to reflect only live entries
			_freeCount = 0; // Reset free count as all entries are now compacted
		}
		
		
		@Override
		public RW clone() {
			return ( RW ) super.clone();
		}
	}
}