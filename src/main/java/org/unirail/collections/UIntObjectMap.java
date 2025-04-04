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
 * {@code IntObjectMap} is a generic interface for a map that stores key-value pairs,
 * where keys are primitive integers ({@code int}) and values are objects of a specified type {@code V}.
 * <p>
 * This interface is designed for efficient storage and retrieval of object values based on integer keys.
 */
public interface UIntObjectMap {
	
	/**
	 * {@code R} is an abstract, read-only base class that provides the fundamental implementation
	 * for {@code IntObjectMap}. It manages the core data structures and read operations for the map.
	 * <p>
	 * This class is intended to be extended by concrete implementations that require either read-only
	 * or read-write capabilities. It uses a hash table for efficient key-based lookups and manages
	 * collisions using separate chaining.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	abstract class R< V > implements JsonWriter.Source, Cloneable {
		/**
		 * Indicates whether the map contains an entry with a null key.
		 * Null keys are handled separately from integer keys in this implementation.
		 */
		protected       boolean                hasNullKey;
		/**
		 * Stores the value associated with the null key, if {@link #hasNullKey} is true.
		 */
		protected       V                      nullKeyValue;
		/**
		 * Array of bucket indices for the hash table. Each index corresponds to a hash bucket.
		 * Stores indices into the {@link #nexts}, {@link #keys}, and {@link #values} arrays,
		 * or 0 if the bucket is empty.
		 */
		protected       int[]                  _buckets;
		/**
		 * Array of 'next' indices for collision chaining in the hash table.
		 * For each entry, it stores the index of the next entry in the same hash bucket,
		 * or a special value indicating the end of the chain or a free entry.
		 */
		protected       int[]                  nexts;
		/**
		 * Array of integer keys stored in the map.
		 */
		protected       int[]          keys = Array.EqualHashOf.ints     .O;
		/**
		 * Array of values associated with the keys in the {@link #keys} array.
		 */
		protected       V[]                    values;
		/**
		 * The total number of entries currently in use in the {@link #nexts}, {@link #keys}, and {@link #values} arrays.
		 * This includes both occupied entries and entries marked as free in the free list.
		 */
		protected       int                    _count;
		/**
		 * Index of the first free entry in the {@link #nexts}, {@link #keys}, and {@link #values} arrays, forming a free list.
		 * -1 indicates that there are no free entries available.
		 */
		protected       int                    _freeList;
		/**
		 * The number of free entries available in the free list.
		 */
		protected       int                    _freeCount;
		/**
		 * Version number of the map, incremented on structural modifications.
		 * Used for invalidating tokens and detecting concurrent modifications.
		 */
		protected       int                    _version;
		/**
		 * Strategy for comparing values and calculating their hash codes.
		 * This allows the map to handle different types of values and equality semantics.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		/**
		 * Special value indicating the start of the free list chain in the {@link #nexts} array.
		 */
		protected static final int  StartOfFreeList = -3;
		/**
		 * Mask to extract the index part from a token.
		 */
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		/**
		 * Number of bits to shift for extracting the version part from a token.
		 */
		protected static final int  VERSION_SHIFT   = 32;
		/**
		 * Represents an invalid token value (-1).
		 */
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Constructs a new read-only {@code IntObjectMap.R} with the specified value equality and hash strategy.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating their hash codes.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		/**
		 * Checks if the map is empty (contains no key-value pairs).
		 *
		 * @return {@code true} if the map is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value pairs in the map.
		 *
		 * @return The number of key-value pairs in the map.
		 */
		public int size() {
			return _count - _freeCount + (
					hasNullKey ?
							1 :
							0 );
		}
		
		/**
		 * Returns the number of key-value pairs in the map. This is an alias for {@link #size()}.
		 *
		 * @return The number of key-value pairs in the map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the total capacity of the map's internal arrays (number of allocated slots).
		 * This is not the same as the number of key-value pairs ({@link #size()}).
		 *
		 * @return The capacity of the map's internal arrays.
		 */
		public int length() {
			return nexts == null ?
					0 :
					nexts.length;
		}
		
		/**
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		public V nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Checks if the map contains a key-value pair with the specified integer key.
		 *
		 * @param key The integer key to search for.
		 * @return {@code true} if the map contains a key-value pair with the specified key, {@code false} otherwise.
		 */
		public boolean containsKey( long key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a key-value pair with the specified key (boxed Integer).
		 * Handles null keys as well.
		 *
		 * @param key The key (Integer) to search for (can be null).
		 * @return {@code true} if the map contains a key-value pair with the specified key, {@code false} otherwise.
		 */
		public boolean containsKey(  Long      key ) {
			return key == null ?
					hasNullKey :
					tokenOf( key ) != INVALID_TOKEN;
		}
		
		/**
		 * Checks if the map contains a key-value pair with the specified value.
		 * This operation iterates through all values in the map and performs an equality check.
		 *
		 * @param value The value to search for.
		 * @return {@code true} if the map contains a key-value pair with the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( Object value ) {
			if( _count == 0 && !hasNullKey ) return false;
			if( hasNullKey && Objects.equals( nullKeyValue, value ) ) return true;
			for( int i = 0; i < _count; i++ )
				if( -2 < nexts[ i ] && Objects.equals( values[ i ], value ) ) return true;
			return false;
		}
		
		/**
		 * Returns a token associated with the specified key (boxed Integer), if present in the map.
		 * A token is a long value that uniquely identifies a key-value pair within a specific version of the map.
		 * Tokens are used for efficient iteration and access, and may become invalid if the map is modified.
		 * Handles null keys.
		 *
		 * @param key The key (Integer) to get the token for (can be null).
		 * @return A valid token if the key is found, {@code INVALID_TOKEN} (-1) otherwise.
		 */
		public long tokenOf(  Long      key ) {
			return key == null ?
					hasNullKey ?
							token( _count ) :
							INVALID_TOKEN :
					tokenOf( key. longValue     () );
		}
		
		/**
		 * Returns a token associated with the specified integer key, if present in the map.
		 * A token is a long value that uniquely identifies a key-value pair within a specific version of the map.
		 * Tokens are used for efficient iteration and access, and may become invalid if the map is modified.
		 *
		 * @param key The integer key to get the token for.
		 * @return A valid token if the key is found, {@code INVALID_TOKEN} (-1) otherwise.
		 */
		public long tokenOf( long key ) {
			
			if( _buckets == null ) return INVALID_TOKEN;
			int hash = Array.hash( key );
			int i    = ((int) _buckets[ bucketIndex( hash ) ] ) - 1;
			
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key )
					return token( i );
				i = nexts[ i ];
				if( nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns a token for the "first" key-value pair in the map for iteration purposes.
		 * The order is not guaranteed to be consistent across different versions or implementations.
		 * If the map is empty, returns {@code INVALID_TOKEN} (-1). If a null key exists, its token will be returned last by subsequent {@link #token(long)} calls.
		 *
		 * @return A token for the first key-value pair, or {@code INVALID_TOKEN} (-1) if the map is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i );
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns a token for the "next" key-value pair in the map, following the given token, for iteration purposes.
		 * If the given token is the last valid token, or is invalid (e.g., due to map modification), returns {@code INVALID_TOKEN} (-1).
		 * If a null key exists, its token will be returned last.
		 *
		 * @param token The current token.
		 * @return A token for the next key-value pair, or {@code INVALID_TOKEN} (-1) if there is no next pair or the token is invalid.
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
		
		/**
		 * Checks if the key associated with the given token is the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the key for the token is null, {@code false} otherwise.
		 */
		boolean isKeyNull( long token ) { return index( token ) == _count; }
		
		/**
		 * Returns the integer key associated with the given token.
		 *
		 * @param token The token to get the key for.
		 * @return The integer key associated with the token, or 0 if the token represents the null key or is invalid.
		 */
		public long key( long token ) {
			return  ( hasNullKey && index( token ) == _count ?
					0 :
					keys[ index( token ) ] );
		}
		
		/**
		 * Returns the value associated with the given token.
		 *
		 * @param token The token to get the value for.
		 * @return The value associated with the token, or {@code null} if the token is invalid.
		 */
		public V value( long token ) {
			return hasNullKey && index( token ) == _count ?
					nullKeyValue :
					values[ index( token ) ];
		}
		
		/**
		 * Returns the value associated with the specified key (boxed Integer), or {@code null} if the key is not found.
		 * Handles null keys.
		 *
		 * @param key The key (Integer) to get the value for (can be null).
		 * @return The value associated with the key, or {@code null} if the key is not found.
		 */
		@SuppressWarnings( "unchecked" )
		public V get(  Long      key ) {
			long token = tokenOf( (  Long      ) key );
			return token == INVALID_TOKEN ?
					null :
					value( token );
		}
		
		
		/**
		 * Returns the value associated with the specified integer key, or {@code defaultValue} if the key is not found.
		 *
		 * @param key          The integer key to get the value for.
		 * @param defaultValue The default value to return if the key is not found.
		 * @return The value associated with the key, or {@code defaultValue} if the key is not found.
		 */
		public V getOrDefault( long key, V defaultValue ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
					defaultValue :
					value( token );
		}
		
		/**
		 * Returns the value associated with the specified key (boxed Integer), or {@code defaultValue} if the key is not found.
		 * Handles null keys.
		 *
		 * @param key          The key (Integer) to get the value for (can be null).
		 * @param defaultValue The default value to return if the key is not found.
		 * @return The value associated with the key, or {@code defaultValue} if the key is not found.
		 */
		public V getOrDefault(  Long      key, V defaultValue ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
					defaultValue :
					value( token );
		}
		
		/**
		 * Calculates the hash code for this map. The hash code is based on the keys and values of all entries.
		 * It iterates through all key-value pairs and combines their hash codes using a mixing function.
		 *
		 * @return The hash code for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) );
				h = Array.mix( h, Array.hash( value( token ) == null ?
						                              seed :
						                              equal_hash_V.hashCode( value( token ) ) ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue != null ?
						                              equal_hash_V.hashCode( nullKeyValue ) :
						                              seed ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		/**
		 * Seed value used in the hash code calculation.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map to the specified object for equality.
		 * Two maps are considered equal if they are of the same class, have the same size,
		 * contain the same key-value pairs, and have the same null key status and null key value (if applicable).
		 *
		 * @param obj The object to compare to.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj );
		}
		
		/**
		 * Compares this map to another map of the same type for equality.
		 *
		 * @param other The other map to compare to.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    ( hasNullKey && !Objects.equals( nullKeyValue, other.nullKeyValue ) ) ||
			    size() != other.size() ) return false;
			
			for( long token = token(), t; index( token ) < _count; token = token( token ) )
				if( ( t = other.tokenOf( key( token ) ) ) == INVALID_TOKEN ||
				    !Objects.equals( value( token ), other.value( t ) ) ) return false;
			return true;
		}
		
		/**
		 * Creates and returns a shallow copy of this read-only map.
		 * The copy will contain references to the same keys and values as the original map.
		 *
		 * @return A shallow copy of this map.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.nexts    = nexts.clone();
					dst.keys     = keys.clone();
					dst.values   = values.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
				return null;
			}
		}
		
		/**
		 * Returns a string representation of this map in JSON format.
		 *
		 * @return A JSON string representation of this map.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the content of this map as a JSON object to the provided {@link JsonWriter}.
		 *
		 * @param json The {@link JsonWriter} to write the JSON output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			long token = token();
			
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			for( ; index( token ) < _count; token = token( token ) )
			     json.name( String.valueOf( key( token ) ) ).value( value( token ) );
			
			
			json.exitObject();
		}
		
		/**
		 * Calculates the bucket index in the {@link #_buckets} array for a given hash value.
		 *
		 * @param hash The hash value to calculate the bucket index for.
		 * @return The bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an index and the current version of the map.
		 *
		 * @param index The index of the entry in the internal arrays.
		 * @return The generated token.
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index & INDEX_MASK ); }
		
		/**
		 * Extracts the index part from a token.
		 *
		 * @param token The token to extract the index from.
		 * @return The index part of the token.
		 */
		protected int index( long token ) { return ( int ) ( token & INDEX_MASK ); }
		
		/**
		 * Extracts the version part from a token.
		 *
		 * @param token The token to extract the version from.
		 * @return The version part of the token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * {@code RW} is a read-write implementation of {@code IntObjectMap}, extending the read-only base class {@link R}.
	 * It provides methods for modifying the map, such as adding, updating, and removing key-value pairs.
	 * <p>
	 * This class uses the same underlying hash table structure as {@link R} but adds functionality for write operations,
	 * including resizing the internal arrays as needed to accommodate new entries.
	 *
	 * @param <V> The type of values stored in the map.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs a new read-write {@code IntObjectMap.RW} with a default initial capacity.
		 *
		 * @param clazzV The class of the value type {@code V}. Used for array creation.
		 */
		public RW( Class< V > clazzV ) { this( clazzV, 0 ); }
		
		/**
		 * Constructs a new read-write {@code IntObjectMap.RW} with the specified initial capacity.
		 *
		 * @param clazzV   The class of the value type {@code V}. Used for array creation.
		 * @param capacity The initial capacity of the map.
		 */
		public RW( Class< V > clazzV, int capacity ) { this( Array.get( clazzV ), capacity ); }
		
		/**
		 * Constructs a new read-write {@code IntObjectMap.RW} with a default initial capacity,
		 * using the provided value equality and hash strategy.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating their hash codes.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V ) { this( equal_hash_V, 0 ); }
		
		/**
		 * Constructs a new read-write {@code IntObjectMap.RW} with the specified initial capacity,
		 * using the provided value equality and hash strategy.
		 *
		 * @param equal_hash_V The strategy for comparing values and calculating their hash codes.
		 * @param capacity     The initial capacity of the map.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int capacity ) {
			super( equal_hash_V );
			if( capacity > 0 ) initialize( capacity );
		}
		
		
		/**
		 * Removes all key-value pairs from this map, making it empty.
		 */
		public void clear() {
			if( _count == 0 && !hasNullKey ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( nexts, 0, _count, ( int ) 0 );
			Arrays.fill( keys, 0, _count, ( int ) 0 );
			Arrays.fill( values, 0, _count, null );
			_count       = 0;
			_freeList    = -1;
			_freeCount   = 0;
			hasNullKey   = false;
			nullKeyValue = null;
			_version++;
		}
		
		/**
		 * Removes the key-value pair with the specified integer key from this map, if present.
		 *
		 * @param key The integer key of the key-value pair to remove.
		 * @return The value that was associated with the key, or {@code null} if the key was not found.
		 */
		public V remove( long key ) {
			if( _buckets == null || _count == 0 ) return null;
			
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
					V oldValue = values[ i ];
					values[ i ] = null;
					_freeList   = i;
					_freeCount++;
					_version++;
					return oldValue;
				}
				last = i;
				i    = next;
				if( nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return null;
			
		}
		
		/**
		 * Removes the key-value pair with the specified key (boxed Integer) from this map, if present.
		 * Handles null keys.
		 *
		 * @param key The key (Integer) of the key-value pair to remove (can be null).
		 * @return The value that was associated with the key, or {@code null} if the key was not found.
		 */
		public V remove(  Long      key ) {
			if( key == null ) {
				if( !hasNullKey ) return null;
				hasNullKey = false;
				V oldValue = nullKeyValue;
				nullKeyValue = null;
				_version++;
				return oldValue;
			}
			
			return remove( key. longValue     () );
		}
		
		/**
		 * Ensures that the capacity of the map's internal arrays is at least the specified capacity.
		 * If the current capacity is less than the specified capacity, the arrays are resized to a new capacity
		 * that is a prime number greater than or equal to the specified capacity.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity of the map's internal arrays after ensuring capacity.
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
		 * Trims the capacity of the map's internal arrays to be equal to the current size of the map,
		 * if the current capacity is larger than the size. This can reduce memory usage.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Trims the capacity of the map's internal arrays to be at least the specified capacity.
		 * If the current capacity is larger than the specified capacity, the arrays are resized to a new capacity
		 * that is a prime number greater than or equal to the specified capacity, but not less than the current size of the map.
		 *
		 * @param capacity The desired capacity after trimming. Must be at least the current size of the map.
		 * @throws IllegalArgumentException if the capacity is less than the current size of the map.
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			int new_size        = Array.prime( capacity );
			if( currentCapacity <= new_size ) return;
			
			int[]         old_next   = nexts;
			int[] old_keys   = keys;
			V[]           old_values = values;
			int           old_count  = _count;
			_version++;
			initialize( new_size );
			copy( old_next, old_keys, old_values, old_count );
		}
		
		/**
		 * Associates the specified value with the specified key (boxed Integer) in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Handles null keys.
		 *
		 * @param key   The key (Integer) with which the specified value is to be associated (can be null).
		 * @param value The value to be associated with the specified key.
		 * @return The previous value associated with the key, or {@code null} if there was no mapping for the key.
		 */
		public boolean put(  Long      key, V value ) {
			return key == null ?
					put( value ) :
					put( key.longValue     (), value );
		}
		
		
		/**
		 * Tries to insert a value associated with a null key into the map.
		 *
		 * @param value The value to insert.
		 * @return {@code true} if insertion occurred, {@code false} otherwise (depending on behavior).
		 * @throws IllegalArgumentException if behavior is 0 and the key already exists.
		 */
		public boolean put( V value ) {
			_version++;
			boolean b = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = value;
			return b;
		}
		
		
		/**
		 * Tries to insert a key-value pair with the specified integer key and value into the map.
		 * Handles hash collisions and resizing if necessary.
		 *
		 * @param key   The integer key to insert.
		 * @param value The value to insert.
		 * @return {@code true} if insertion occurred, {@code false} otherwise (depending on behavior).
		 * @throws IllegalArgumentException if behavior is 0 and the key already exists.
		 */
		public boolean put( long key, V value ) {
			if( _buckets == null ) initialize( 7 );
			int[] _nexts         = nexts;
			int   hash           = Array.hash( key );
			int   collisionCount = 0;
			int   bucketIndex    = bucketIndex( hash );
			int   bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) {
					values[ next ] = value;
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
					bucket = ((int) _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1;
				}
				index = _count++;
			}
			
			nexts[ index ]          = ( int ) bucket;
			keys[ index ]           = ( int ) key;
			values[ index ]         = value;
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			return true;
		}
		
		/**
		 * Resizes the internal arrays of the map to a new capacity.
		 * Rehashes existing key-value pairs into the new buckets after resizing.
		 *
		 * @param newSize The new capacity for the arrays (should be a prime number).
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Long     .BYTES * 8 ); // Limit size to avoid potential issues
			_version++; // Increment version before and after resize operation to ensure token invalidation
			int[]         new_next   = Arrays.copyOf( nexts, newSize );
			int[] new_keys   = Arrays.copyOf( keys, newSize );
			V[]           new_values = Arrays.copyOf( values, newSize );
			final int     count      = _count;
			
			_buckets = new int[ newSize ];
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = ( int ) ( _buckets[ bucketIndex ] - 1 );
					_buckets[ bucketIndex ] = ( i + 1 );
				}
			
			nexts  = new_next;
			keys   = new_keys;
			values = new_values;
		}
		
		/**
		 * Initializes the internal arrays of the map with the specified capacity.
		 *
		 * @param capacity The initial capacity for the arrays (will be adjusted to the next prime number).
		 * @return The actual capacity after adjusting to a prime number.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets  = new int[ capacity ];
			nexts     = new int[ capacity ];
			keys      = new int[ capacity ];
			values    = equal_hash_V.copyOf( null, capacity );
			_freeList = -1;
			return capacity;
		}
		
		/**
		 * Copies key-value pairs from old arrays to the newly resized arrays.
		 * Used during resizing to preserve existing data.
		 *
		 * @param old_next   The old 'next' array.
		 * @param old_keys   The old 'keys' array.
		 * @param old_values The old 'values' array.
		 * @param old_count  The number of entries in the old arrays.
		 */
		private void copy( int[] old_next, int[] old_keys, V[] old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue; // Skip free list entries from old array
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
		
		
		/**
		 * Static instance used for obtaining the {@link Array.EqualHashOf} implementation for {@code RW<V>}.
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
	}
	
	/**
	 * Returns the {@link Array.EqualHashOf} implementation for {@code RW<V>}.
	 *
	 * @param <V> The type of values in the map.
	 * @return The {@link Array.EqualHashOf} instance for {@code RW<V>}.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() { return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; }
}