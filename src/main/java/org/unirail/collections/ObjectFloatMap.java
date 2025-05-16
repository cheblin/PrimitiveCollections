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

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.function.Function;

/**
 * A generic Map implementation for storing key-value pairs with efficient operations.
 * Implements a hash table with separate chaining for collision resolution and dynamic resizing.
 *
 * <p>Supports null keys and values, designed for high-performance and customization.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *     <li><b>Generic Keys:</b> Supports any object type as keys.</li>
 *     <li><b>Integer Values:</b> Optimized for storing primitive values.</li>
 *     <li><b>Separate Chaining:</b> Efficiently handles hash collisions.</li>
 *     <li><b>Dynamic Resizing:</b> Maintains performance as the map grows.</li>
 *     <li><b>Null Key Support:</b> Allows a single null key.</li>
 *     <li><b>Customizable Equality and Hashing:</b> Uses {@link Array.EqualHashOf} for key handling.</li>
 *     <li><b>Token-Based Iteration:</b> Safe and efficient traversal, even with concurrent reads.</li>
 *     <li><b>Cloneable:</b> Implements {@link Cloneable} for shallow copies.</li>
 * </ul>
 */
public interface ObjectFloatMap {
	
	/**
	 * Read-only base class providing core functionality and state management for the map.
	 */
	abstract class R< K > implements JsonWriter.Source, Cloneable {
		protected boolean                hasNullKey;        // Indicates if the Map contains a null key
		protected float            nullKeyValue;      // Value for the null key
		protected int[]                  _buckets;         // Hash table buckets array
		protected long[]                 hash_nexts;       // Array of entries: hashCode | next index
		protected K[]                    keys;            // Array of keys
		protected float[]          values;          // Array of values
		protected int                    _count;          // Total number of entries
		protected int                    _freeList;       // Index of first free entry
		protected int                    _freeCount;      // Number of free entries
		protected int                    _version;        // Version for modification detection
		protected Array.EqualHashOf< K > equal_hash_K;    // Key equality and hash strategy
		
		protected static final int  StartOfFreeList = -3;
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		protected static final int  NULL_KEY_INDEX  = 0x7FFF_FFFF;
		protected static final int  VERSION_SHIFT   = 32;
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Checks if the map is empty.
		 *
		 * @return {@code true} if the map contains no key-value mappings, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value mappings in the map.
		 *
		 * @return The number of key-value mappings, including the null key if present.
		 */
		public int size() {
			return _count - _freeCount + ( hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Alias for {@link #size()}.
		 *
		 * @return The number of key-value mappings in the map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the capacity of the internal arrays.
		 *
		 * @return The length of the internal arrays, or 0 if uninitialized.
		 */
		public int length() {
			return hash_nexts == null ?
					0 :
					hash_nexts.length;
		}
		
		/**
		 * Checks if the map contains a mapping for the specified key.
		 *
		 * @param key The key to check.
		 * @return {@code true} if the map contains the key, {@code false} otherwise.
		 */
		public boolean containsKey( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 *
		 * @param value The value to check.
		 * @return {@code true} if the map contains the value, {@code false} otherwise.
		 */
		public boolean containsValue( float value ) {
			
			if( hasNullKey && nullKeyValue == value ) return true;
			
			if( _count - _freeCount == 0 ) return false;
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) && values[ i ] == value ) return true;
			return false;
		}
		
		/**
		 * Checks if the map contains a mapping for the null key.
		 *
		 * @return {@code true} if the map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the null key.
		 *
		 * @return The value associated with the null key, or undefined if no null key exists.
		 */
		public float nullKeyValue() { return  nullKeyValue; }
		
		/**
		 * Returns the token associated with the specified key.
		 *
		 * @param key The key to look up.
		 * @return A token for iteration, or {@link #INVALID_TOKEN} if the key is not found.
		 * @throws ConcurrentModificationException if excessive collisions are detected.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN;
			if( _buckets == null ) return INVALID_TOKEN;
			int hash = equal_hash_K.hashCode( key );
			int next = _buckets[ bucketIndex( hash ) ] - 1;
			
			for( int collisionCount = 0; ( next & 0x7FFF_FFFF ) < hash_nexts.length; ) {
				final long hash_next = hash_nexts[ next ];
				if( hash( hash_next ) == hash && equal_hash_K.equals( keys[ next ], key ) )
					return token( next );
				next = next( hash_next );
				if( hash_nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the first valid token for iteration.
		 *
		 * @return A token for iteration, or {@link #INVALID_TOKEN} if the map is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return token( i );
			return hasNullKey ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next valid token for iteration.
		 *
		 * @param token The current token.
		 * @return The next token, or {@link #INVALID_TOKEN} if no further entries exist or the token is invalid.
		 * @throws IllegalArgumentException        if the token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the map was modified since the token was issued.
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
		 * Returns the next token for fast, <b>unsafe</b> iteration over <b>non-null keys only</b>.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #key(long)} to handle it separately.</p>
		 *
		 * <p><b>WARNING: UNSAFE.</b> This method is faster than {@link #token(long)} but risky if the map is
		 * structurally modified during iteration. Such changes may cause skipped entries, exceptions, or undefined
		 * behavior. Use only when no modifications will occur.</p>
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 */
		public int unsafe_token( int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return i;
			return -1;
		}
		
		/**
		 * Checks if the token corresponds to the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the key associated with the given token.
		 *
		 * @param token The token to query.
		 * @return The key, or {@code null} if the token corresponds to the null key entry.
		 */
		public K key( long token ) {
			return isKeyNull( token ) ?
					null :
					keys[ index( token ) ];
		}
		
		/**
		 * Returns the value associated with the given token.
		 *
		 * @param token The token to query.
		 * @return The value associated with the token.
		 */
		public float value( long token ) {
			return ( index( token ) == NULL_KEY_INDEX ?
					nullKeyValue :
					values[ index( token ) ] );
		}
		
		/**
		 * Computes the hash code for the map based on its key-value pairs.
		 *
		 * @return The hash code for the map.
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
		
		/**
		 * Checks if this map is equal to another object.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the object is a map with the same key-value mappings, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Checks if this map is equal to another map of the same type.
		 *
		 * @param other The other map to compare with.
		 * @return {@code true} if the maps have the same key-value mappings, {@code false} otherwise.
		 */
		public boolean equals( R< K > other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    hasNullKey && nullKeyValue != other.nullKeyValue ||
			    size() != other.size() ) return false;
			
			long t;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( ( t = other.tokenOf( key( token ) ) ) == INVALID_TOKEN ||
				    value( token ) != other.value( t ) ) return false;
			return true;
		}
		
		/**
		 * Creates a shallow copy of this map.
		 *
		 * @return A new map with the same key-value mappings, or {@code null} if cloning fails.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public R< K > clone() {
			try {
				R dst = ( R ) super.clone();
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
		
		/**
		 * Returns a JSON string representation of this map.
		 *
		 * @return A JSON string representing the map's contents.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Serializes the map's contents to JSON.
		 *
		 * @param json The {@link JsonWriter} to write to.
		 */
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
				         .value( value( token ) );
				json.exitObject();
			}
			else {
				json.enterArray();
				
				if( hasNullKey )
					json.enterObject()
					    .name( "Key" ).value()
					    .name( "Value" ).value( nullKeyValue )
					    .exitObject();
				
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.enterObject()
				         .name( "Key" ).value( key( token ) )
				         .name( "Value" ).value( value( token ) )
				         .exitObject();
				json.exitArray();
			}
		}
		
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		protected static int hash( long packedEntry ) {
			return ( int ) ( packedEntry >> 32 );
		}
		
		protected static int next( long hash_next ) {
			return ( int ) ( hash_next & NEXT_MASK );
		}
		
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index; }
		
		protected int index( long token ) {
			return ( int ) token;
		}
		
		protected int version( long token ) {
			return ( int ) ( token >>> VERSION_SHIFT );
		}
	}
	
	/**
	 * Read-write implementation extending the read-only base class, providing methods to modify the map.
	 */
	class RW< K > extends R< K > {
		private static final int                                                        HashCollisionThreshold = 100;
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs an empty map for the specified key class.
		 *
		 * @param clazzK The class of the keys.
		 */
		public RW( Class< K > clazzK ) { this( clazzK, 0 ); }
		
		/**
		 * Constructs an empty map for the specified key class with the given initial capacity.
		 *
		 * @param clazzK   The class of the keys.
		 * @param capacity The initial capacity.
		 */
		public RW( Class< K > clazzK, int capacity ) { this( Array.get( clazzK ), capacity ); }
		
		/**
		 * Constructs an empty map with the specified equality and hash strategy.
		 *
		 * @param equal_hash_K The equality and hash strategy for keys.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K ) { this( equal_hash_K, 0 ); }
		
		/**
		 * Constructs an empty map with the specified equality and hash strategy and initial capacity.
		 *
		 * @param equal_hash_K The equality and hash strategy for keys.
		 * @param capacity     The initial capacity.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Removes all mappings from the map.
		 */
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( hash_nexts, 0, _count, 0L );
			Arrays.fill( keys, 0, _count, null );
			Arrays.fill( values, 0, _count, ( float ) 0 );
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
		}
		
		/**
		 * Removes the mapping for the specified key, if present.
		 *
		 * @param key The key to remove.
		 * @return true if remove entry or false no mapping was found for the  key.
		 * @throws ConcurrentModificationException if excessive collisions are detected.
		 */
		public boolean remove( K key ) {
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
		 * Ensures the map has at least the specified capacity.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity of the map.
		 * @throws IllegalArgumentException if capacity is negative.
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
		 * Trims the map's capacity to fit its current size.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Trims the map's capacity to the specified minimum capacity.
		 *
		 * @param capacity The desired minimum capacity.
		 * @throws IllegalArgumentException if capacity is less than the current size.
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int new_size        = Array.prime( capacity );
			if( length() <= new_size ) return;
			
			long[]        old_hash_next = hash_nexts;
			K[]           old_keys      = keys;
			float[] old_values    = values;
			int           old_count     = _count;
			_version++;
			initialize( new_size );
			copy( old_hash_next, old_keys, old_values, old_count );
		}
		
		/**
		 * Associates the specified value with the specified key.
		 *
		 * @param key   The key to associate the value with.
		 * @param value The value to associate.
		 * @return {@code true} if the key was not previously mapped, {@code false} if the key's value was updated.
		 * @throws ConcurrentModificationException if excessive collisions are detected.
		 */
		public boolean put( K key, float value ) {
			if( key == null ) {
				boolean b = !hasNullKey;
				hasNullKey   = true;
				nullKeyValue = ( float ) value;
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
					values[ next ] = ( float ) value;
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
			
			hash_nexts[ index ]     = hash_next( hash, bucket );
			keys[ index ]           = key;
			values[ index ]         = ( float ) value;
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			if( HashCollisionThreshold < collisionCount && this.forceNewHashCodes != null && key instanceof String )
				resize( hash_nexts.length, true );
			return true;
		}
		
		private void resize( int new_size, boolean forceNewHashCodes ) {
			_version++;
			long[]        new_hash_next = Arrays.copyOf( hash_nexts, new_size );
			K[]           new_keys      = Arrays.copyOf( keys, new_size );
			float[] new_values    = Arrays.copyOf( values, new_size );
			final int     count         = _count;
			
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
			values     = new_values;
		}
		
		private int initialize( int capacity ) {
			_version++;
			_buckets   = new int[ capacity ];
			hash_nexts = new long[ capacity ];
			keys       = equal_hash_K.copyOf( null, capacity );
			values     = new float[ capacity ];
			_freeList  = -1;
			_count     = 0;
			_freeCount = 0;
			return capacity;
		}
		
		private void copy( long[] old_hash_next, K[] old_keys, float[] old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				final long hn = old_hash_next[ i ];
				if( next( hn ) < -1 ) continue;
				
				keys[ new_count ]   = old_keys[ i ];
				values[ new_count ] = old_values[ i ];
				int h           = hash( hn );
				int bucketIndex = bucketIndex( h );
				hash_nexts[ new_count ] = hash_next( h, _buckets[ bucketIndex ] - 1 );
				_buckets[ bucketIndex ] = new_count + 1;
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		private static long hash_next( int hash, int next ) { return ( long ) hash << 32 | next & NEXT_MASK; }
		
		private static void next( long[] dst, int index, int next ) {
			dst[ index ] = dst[ index ] & HASH_CODE_MASK | next & NEXT_MASK;
		}
		
		private static void hash( long[] dst, int index, int hash ) {
			dst[ index ] = dst[ index ] & NEXT_MASK | ( long ) hash << 32;
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() { return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT; }
}