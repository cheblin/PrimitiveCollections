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
import java.util.function.Function;

/**
 * <p>
 * A generic Map implementation providing key-value pair storage with efficient retrieval, insertion, and deletion operations.
 * This Map uses a hash table with separate chaining for collision resolution and dynamic resizing to maintain performance as the number of entries grows.
 * </p>
 * <p>
 * Supports null keys and values, optimized for primitive values with explicit null value handling. The Map is designed for high-performance, customizable use cases where flexibility in key types and null handling is required.
 * </p>
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *     <li><b>Generic Keys:</b> Supports any object type as keys, with customizable equality and hashing via {@link Array.EqualHashOf}.</li>
 *     <li><b>Integer Values with Null Support:</b> Stores primitive values, with explicit support for null values using {@link IntNullList}, ideal for use cases where null is a valid value.</li>
 *     <li><b>Hash Table with Separate Chaining:</b> Resolves hash collisions by chaining entries in buckets, ensuring efficient performance even with high load factors.</li>
 *     <li><b>Dynamic Resizing:</b> Automatically adjusts hash table capacity based on entry count, with support for collision-driven rehashing using a customizable {@code forceNewHashCodes} function.</li>
 *     <li><b>Null Key Support:</b> Allows a single null key, with dedicated methods to check and access its value.</li>
 *     <li><b>Token-Based Iteration:</b> Provides safe iteration using tokens, which act as stable pointers to entries, reducing overhead compared to iterators. Includes an <b>unsafe</b> iteration method ({@link R#unsafe_token(int)}) for faster traversal of non-null keys, with caveats for concurrent modifications.</li>
 *     <li><b>Memory Management:</b> Supports capacity trimming ({@link RW#trim()}) and minimum capacity enforcement ({@link RW#ensureCapacity(int)}) to optimize memory usage.</li>
 *     <li><b>Cloneable:</b> Implements {@link Cloneable} for creating shallow copies of the map. Keys and values are not deep-copied.</li>
 * </ul>
 * </p>
 */
public interface ObjectUShortNullMap {
	
	abstract class R< K > implements JsonWriter.Source, Cloneable {
		protected boolean                hasNullKey;
		protected boolean                nullKeyHasValue;
		protected char            nullKeyValue;
		protected int[]                  _buckets;
		protected long[]                 hash_nexts;
		protected K[]                    keys;
		protected UShortNullList.RW values;
		protected int                    _count;
		protected int                    _freeList;
		protected int                    _freeCount;
		protected int                    _version;
		protected Array.EqualHashOf< K > equal_hash_K;
		
		protected static final int  StartOfFreeList = -3;
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		protected static final int  NULL_KEY_INDEX  = 0x7FFF_FFFF;
		
		protected static final int  VERSION_SHIFT          = 32;
		protected static final long INVALID_TOKEN          = -1L;
		protected static final int  HashCollisionThreshold = 100;
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return true if the map contains no key-value mappings, false otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value mappings in this map.
		 *
		 * @return The number of key-value mappings in this map.
		 */
		public int size() {
			return _count - _freeCount + (
					hasNullKey ?
							1 :
							0 );
		}
		
		/**
		 * Returns the number of key-value mappings in this map.
		 * Alias for {@link #size()}.
		 *
		 * @return The number of key-value mappings in this map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the hash map (the length of the internal arrays).
		 *
		 * @return The capacity of the hash map. Returns 0 if not initialized.
		 */
		public int length() {
			return hash_nexts == null ?
					0 :
					hash_nexts.length;
		}
		
		/**
		 * Checks if this map contains a mapping for the specified key.
		 *
		 * @param key key whose presence in this map is to be tested
		 * @return true if this map contains a mapping for the specified key, false otherwise.
		 */
		public boolean containsKey( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this map contains the specified value.
		 * Note: This operation is less efficient than {@link #containsKey(Object)} as it requires iterating through the values.
		 *
		 * @param value value whose presence in this map is to be tested (can be null)
		 * @return true if this map contains a mapping with the specified value, false otherwise.
		 */
		public boolean containsValue(  Character value ) {
			return
					value == null ?
							hasNullKey && !nullKeyHasValue ||
							values.nextNullIndex( -1 ) != -1 :
							hasNullKey && nullKeyHasValue && nullKeyValue == value ||
							values.indexOf( value ) != -1;
		}
		
		
		/**
		 * Checks if this map contains the specified int value.
		 * Note: This operation is less efficient than {@link #containsKey(Object)} as it requires iterating through the values.
		 *
		 * @param value int value whose presence in this map is to be tested
		 * @return true if this map contains a mapping with the specified value, false otherwise.
		 */
		public boolean containsValue( char value ) { return tokenOfValue( value ) != -1; }
		
		/**
		 * Returns a token associated with the specified value, if the value exists in the map.
		 * Supports null values. Tokens are used for efficient iteration and access.
		 *
		 * @param value the value to find the token for (can be null)
		 * @return a valid token if the value is found, -1 otherwise
		 */
		public long tokenOfValue(  Character value ) {
			if( value != null )
				return tokenOfValue( value.charValue     () );
			
			if( hasNullKey && !nullKeyHasValue ) return token( NULL_KEY_INDEX );
			else
				for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) if( !hasValue( t ) ) return t;
			
			return -1;
		}
		
		/**
		 * Returns a token associated with the specified int value, if the value exists in the map.
		 * Tokens are used for efficient iteration and access.
		 *
		 * @param value the int value to find the token for
		 * @return a valid token if the value is found, -1 otherwise
		 */
		public long tokenOfValue( char value ) {
			if( hasNullKey && nullKeyValue == value ) return token( NULL_KEY_INDEX );
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) if( hasValue( t ) && value( t ) == value ) return t;
			return -1;
		}
		
		/**
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		public boolean nullKeyHasValue() { return nullKeyHasValue; }
		
		public char nullKeyValue() { return  nullKeyValue; }
		
		/**
		 * Returns a token associated with the given key, if the key exists in the map.
		 * Tokens are used for efficient and safe iteration.
		 *
		 * @param key the key to find the token for
		 * @return a valid token if the key is found, {@link #INVALID_TOKEN} (-1) otherwise
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
		 * Returns a token for the first entry in the map.
		 * Used to start token-based iteration.
		 *
		 * @return A token for the first entry, or {@link #INVALID_TOKEN} (-1) if the map is empty
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return token( i );
			return hasNullKey ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns a token for the next entry in the map, starting from the entry represented by the given token.
		 * Used for token-based iteration.
		 *
		 * @param token The current token
		 * @return A token for the next entry, or {@link #INVALID_TOKEN} (-1) if there are no more entries or if the token is invalid due to map modification
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			int i = index( token );
			if( i == NULL_KEY_INDEX ) return INVALID_TOKEN;
			
			if( 0 < _count - _freeCount )
				for( i++; i < _count; i++ )
					if( next( hash_nexts[ i ] ) >= -1 ) return token( i );
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
		 * use {@link #nullKeyHasValue()} and {@link #nullKeyValue()} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #nullKeyHasValue() To check if the null key has a value.
		 * @see #nullKeyValue() To get the null key’s value.
		 */
		public int unsafe_token( int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return i;
			return -1;
		}
		
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the key associated with the given token.
		 *
		 * @param token The token
		 * @return The key associated with the token, or null if the token represents the null key or is invalid
		 */
		public K key( long token ) {
			return isKeyNull( token ) ?
					null :
					keys[ index( token ) ];
		}
		
		/**
		 * Checks if the entry associated with the given token has a value (not null in null-value context).
		 *
		 * @param token The token
		 * @return true if the entry has a value, false if it represents a null value or the token is invalid
		 */
		public boolean hasValue( long token ) {
			return index( token ) == NULL_KEY_INDEX ?
					nullKeyHasValue :
					values.hasValue( index( token ) );
		}
		
		/**
		 * Returns the value associated with the given token.
		 * <p>
		 * <b>Precondition:</b> This method should only be called if {@link #hasValue(long)} returns {@code true} for the given token.
		 * Calling it for a token associated with a {@code null} value results in undefined behavior.
		 * <p>
		 *
		 * @param token The token
		 * @return The value associated with the token, or 0 if the value is null or the token is invalid
		 */
		public char value( long token ) {
			return  ( index( token ) == NULL_KEY_INDEX ?
					nullKeyValue :
					values.get( index( token ) ) );
		}
		
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) );
				h = Array.mix( h, hasValue( token ) ?
						Array.hash( value( token ) ) :
						seed );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			if( hasNullKey ) {
				int h = nullKeyHasValue ?
						Array.mix( seed, Array.hash( nullKeyValue ) ) :
						Array.hash( seed );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< K > ) obj );
		}
		
		/**
		 * Compares this map to the specified R for equality.
		 *
		 * @param other The other R to compare to.
		 * @return true if the maps are equal, false otherwise.
		 */
		public boolean equals( R< K > other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    hasNullKey && ( nullKeyHasValue != other.nullKeyHasValue || nullKeyHasValue && nullKeyValue != other.nullKeyValue ) ||
			    size() != other.size() ) return false;
			
			long t;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || hasValue( token ) != other.hasValue( t ) ||
				    hasValue( token ) && value( token ) != other.value( t ) )
					return false;
			}
			return true;
		}
		
		@SuppressWarnings( "unchecked" )
		@Override
		public R< K > clone() {
			try {
				R< K > dst = ( R< K > ) super.clone();
				if( _buckets != null ) {
					dst._buckets   = _buckets.clone();
					dst.hash_nexts = hash_nexts.clone();
					dst.keys       = keys.clone();
					dst.values     = values.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		public String toString() { return toJSON(); }
		
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
				         .value( hasValue( token ) ?
						                 value( token ) :
						                 null );
				json.exitObject();
			}
			else {
				json.enterArray();
				
				if( hasNullKey )
					json
							.enterObject()
							.name( "Key" ).value()
							.name( "Value" ).value( nullKeyValue )
							.exitObject();
				
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.enterObject()
				         .name( "Key" ).value( key( token ) )
				         .name( "Value" ).value( hasValue( token ) ?
						                                 value( token ) :
						                                 null )
				         .exitObject();
				json.exitArray();
			}
		}
		
		protected int bucketIndex( int hash )         { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		protected static int hash( long packedEntry ) { return ( int ) ( packedEntry >> 32 ); }
		
		protected static int next( long hash_next )   { return ( int ) ( hash_next & NEXT_MASK ); }
		
		protected long token( int index )             { return ( long ) _version << VERSION_SHIFT | index; }
		
		protected int index( long token )             { return ( int ) ( token ); }
		
		protected int version( long token )           { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	class RW< K > extends R< K > {
		public Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes = null;
		
		/**
		 * Constructs an empty RW with the default initial capacity.
		 * Uses the default {@link Array.EqualHashOf} for key equality and hashing based on the class K.
		 *
		 * @param clazzK The class of the keys.
		 */
		public RW( Class< K > clazzK ) { this( clazzK, 0 ); }
		
		/**
		 * Constructs an empty RW with the specified initial capacity.
		 * Uses the default {@link Array.EqualHashOf} for key equality and hashing based on the class K.
		 *
		 * @param clazzK   The class of the keys.
		 * @param capacity The initial capacity of the hash map.
		 */
		public RW( Class< K > clazzK, int capacity ) { this( Array.get( clazzK ), capacity ); }
		
		/**
		 * Constructs an empty RW with the default initial capacity.
		 * Uses the provided {@link Array.EqualHashOf} for key equality and hashing.
		 *
		 * @param equal_hash_K Custom implementation of {@link Array.EqualHashOf} for keys of type K.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K ) { this( equal_hash_K, 0 ); }
		
		/**
		 * Constructs an empty RW with the specified initial capacity and custom key equality and hashing.
		 *
		 * @param equal_hash_K Custom implementation of {@link Array.EqualHashOf} for keys of type K.
		 * @param capacity     The initial capacity of the hash map.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			values            = new UShortNullList.RW( 0 );
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Associates the specified value with the specified key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * Supports null values, which are stored explicitly to distinguish from unset values.
		 *
		 * @param key   key with which the specified value is to be associated
		 * @param value value to be associated with the specified key (can be null)
		 * @return true if the key was newly inserted, false if an existing key was updated
		 */
		public boolean put( K key,  Character value ) {
			return value == null ?
					put( key, (char ) 0, false ) :
					put( key, value, true );
		}
		
		/**
		 * Removes all mappings from this map.
		 * The map will be empty after this call.
		 */
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( hash_nexts, 0, _count, 0L );
			values.clear();
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
		}
		
		/**
		 * Removes the mapping for the specified key from this map if present.
		 *
		 * @param key key whose mapping is to be removed from the map
		 * @return true if remove entry or false no mapping was found for the  key.
		 */
		public boolean remove( K key ) {
			if( key == null ) {
				if( !hasNullKey ) return false;
				hasNullKey      = false;
				nullKeyHasValue = false;
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
					values.set1( i, null );
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
		 * Ensures the map's capacity is at least the specified value.
		 * If the current capacity is less than the specified capacity, it is increased to the next prime number
		 * greater than or equal to the specified capacity, optimizing memory usage and performance.
		 *
		 * @param capacity the desired minimum capacity
		 * @return the new capacity after ensuring the minimum capacity
		 * @throws IllegalArgumentException if capacity is negative
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity is less than 0." );
			int currentCapacity = length();
			if( capacity <= currentCapacity ) return currentCapacity;
			_version++;
			if( _buckets == null ) return initialize( Array.prime( capacity ) );
			int newSize = Array.prime( capacity );
			resize( newSize, false );
			return newSize;
		}
		
		/**
		 * Trims the map's capacity to match its current size.
		 * Reduces memory usage by removing unused capacity.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Trims the map's capacity to be at least its current size but no larger than the specified capacity.
		 * Reduces memory usage if the current capacity exceeds the specified capacity.
		 *
		 * @param capacity the desired maximum capacity, must be at least the current size
		 * @throws IllegalArgumentException if capacity is less than the current size
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			int new_size        = Array.prime( capacity );
			if( currentCapacity <= new_size ) return;
			
			long[]                 old_hash_next = hash_nexts;
			K[]                    old_keys      = keys;
			UShortNullList.RW old_values    = values;
			int                    old_count     = _count;
			_version++;
			initialize( new_size );
			copy( old_hash_next, old_keys, old_values, old_count );
		}
		
		/**
		 * Associates the specified int value with the specified key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * The hasValue parameter indicates whether the value is a valid integer or represents a null value.
		 *
		 * <p>If high collision rates are detected and {@code forceNewHashCodes} is set, the map may rehash keys
		 * to improve performance, particularly for String keys.</p>
		 *
		 * @param key      key with which the specified value is to be associated
		 * @param value    int value to be associated with the specified key
		 * @param hasValue true if the value is a valid integer, false if it represents a null value
		 * @return true if the key was newly inserted, false if an existing key was updated
		 */
		public boolean put( K key, char value, boolean hasValue ) {
			if( key == null ) {
				boolean b = !hasNullKey;
				
				hasNullKey      = true;
				nullKeyHasValue = hasValue;
				nullKeyValue    = ( char ) value;
				_version++;
				return b;
			}
			
			if( _buckets == null ) initialize( 7 );
			long[] _hash_nexts    = hash_nexts;
			int    hash           = equal_hash_K.hashCode( key );
			int    collisionCount = 0;
			int    bucketIndex    = bucketIndex( hash );
			int    bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _hash_nexts.length; ) {
				if( hash( _hash_nexts[ next ] ) == hash && equal_hash_K.equals( keys[ next ], key ) ) {
					if( hasValue ) values.set1( next, value );
					else values.set1( next, null );
					_version++;
					return false;
				}
				
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
			
			hash_nexts[ index ] = hash_next( hash, bucket );
			keys[ index ]       = key;
			if( hasValue ) values.set1( index, value );
			else values.set1( index, null );
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			if( HashCollisionThreshold < collisionCount && this.forceNewHashCodes != null && key instanceof String ) resize( hash_nexts.length, true );
			return true;
		}
		
		private void resize( int new_size, boolean forceNewHashCodes ) {
			_version++;
			long[] new_hash_next = Arrays.copyOf( hash_nexts, new_size );
			K[]    new_keys      = Arrays.copyOf( keys, new_size );
			
			final int count = _count;
			
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
		
		private int initialize( int capacity ) {
			_version++;
			_buckets   = new int[ capacity ];
			hash_nexts = new long[ capacity ];
			keys       = equal_hash_K.copyOf( null, capacity );
			values     = new UShortNullList.RW( capacity );
			_freeList  = -1;
			_count     = 0;
			_freeCount = 0;
			return capacity;
		}
		
		private void copy( long[] old_hash_next, K[] old_keys, UShortNullList.RW old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				final long hn = old_hash_next[ i ];
				if( next( hn ) < -1 ) continue;
				
				keys[ new_count ] = old_keys[ i ];
				if( old_values.hasValue( i ) ) values.set1( new_count, old_values.get( i ) );
				
				int h           = hash( hn );
				int bucketIndex = bucketIndex( h );
				hash_nexts[ new_count ] = hash_next( h, _buckets[ bucketIndex ] - 1 );
				_buckets[ bucketIndex ] = new_count + 1;
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		private static long hash_next( int hash, int next ) {
			return ( long ) hash << 32 | next & NEXT_MASK;
		}
		
		private static void hash_next( long[] dst, int index, int hash, int next ) { dst[ index ] = ( long ) hash << 32 | next & NEXT_MASK; }
		
		private static void next( long[] dst, int index, int next )                { dst[ index ] = dst[ index ] & HASH_CODE_MASK | next & NEXT_MASK; }
		
		private static void hash( long[] dst, int index, int hash )                { dst[ index ] = dst[ index ] & NEXT_MASK | ( long ) hash << 32; }
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
		@Override public RW< K > clone() { return ( RW< K > ) super.clone(); }
	}
	
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() { return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT; }
}