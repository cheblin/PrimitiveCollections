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
 * A generic Map implementation for storing key-value pairs with efficient operations.
 * Implements a hash table with separate chaining for collision resolution and dynamic resizing.
 * </p>
 * <p>
 * Supports null keys and values, designed for high-performance and customization.
 * </p>
 * <p>
 * **Key Features:**
 * <ul>
 *     <li>**Generic Keys:** Supports any object type as keys.</li>
 *     <li>**Integer Values:** Optimized for storing integer values.</li>
 *     <li>**Separate Chaining:** Efficiently handles hash collisions.</li>
 *     <li>**Dynamic Resizing:** Maintains performance as the map grows.</li>
 *     <li>**Null Key Support:** Allows a single null key.</li>
 *     <li>**Customizable Equality and Hashing:** Uses {@link Array.EqualHashOf} for key handling.</li>
 *     <li>**Token-Based Iteration:** Safe and efficient traversal, even with concurrent reads.</li>
 *     <li>**Cloneable:** Implements {@link Cloneable} for shallow copies.</li>
 * </ul>
 * </p>
 */
public interface ObjectDoubleMap {
	
	/**
	 * Read-only base class providing core functionality and state management
	 */
	abstract class R< K > implements JsonWriter.Source, Cloneable {
		protected boolean                hasNullKey;        // Indicates if the Map contains a null key
		protected double            nullKeyValue;      // Value for the null key
		protected int[]                  _buckets;         // Hash table buckets array
		protected long[]                 hash_nexts;       // Array of entries: hashCode | next index
		protected K[]                    keys;            // Array of keys
		protected double[]          values;          // Array of values
		protected int                    _count;          // Total number of entries
		protected int                    _freeList;       // Index of first free entry
		protected int                    _freeCount;      // Number of free entries
		protected int                    _version;        // Version for modification detection
		protected Array.EqualHashOf< K > equal_hash_K; // Key equality and hash strategy
		
		protected static final int  StartOfFreeList = -3;
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		protected static final int  VERSION_SHIFT   = 32;
		protected static final long INVALID_TOKEN   = -1L;
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size() {
			return _count - _freeCount + ( hasNullKey ?
					1 :
					0 );
		}
		
		public int count() { return size(); }
		
		public int length() {
			return hash_nexts == null ?
					0 :
					hash_nexts.length;
		}
		
		public boolean contains( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		public boolean containsValue( double value ) {
			if( _count == 0 ) return false;
			if( hasNullKey && nullKeyValue == value ) return true;
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) && values[ i ] == value ) return true;
			return false;
		}
		
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
			if( _buckets == null ) return INVALID_TOKEN;
			int hash = equal_hash_K.hashCode( key );
			int next = _buckets[ bucketIndex( hash ) ] - 1;
			
			for( int collisionCount = 0; ( next & 0xFFFF_FFFFL ) < hash_nexts.length; ) {
				final long hash_next = hash_nexts[ next ];
				if( hash( hash_next ) == hash && equal_hash_K.equals( keys[ next ], key ) )
					return token( next );
				next = next( hash_next );
				if( hash_nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		public boolean isValid( long token ) {
			return token != INVALID_TOKEN && version( token ) == _version;
		}
		
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return token( i );
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		public long token( final long token ) {
			if( !isValid( token ) )
				throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
			for( int i = index( token ) + 1; i < _count; i++ )
				if( next( hash_nexts[ i ] ) >= -1 ) return token( i );
			return hasNullKey && index( token ) < _count ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		public K key( long token ) {
			if( isValid( token ) )
				return hasNullKey && index( token ) == _count ?
						null :
						keys[ index( token ) ];
			throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
		}
		
		public double value( long token ) {
			if( isValid( token ) )
				return ( index( token ) == _count ?
						nullKeyValue :
						values[ index( token ) ] );
			throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
		}
		
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) );
				h = Array.mix( h, Array.hash( value( token ) ) );
				h = Array.finalizeHash( h, 2 );
				a += h; b ^= h; c *= h | 1;
			}
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue ) );
				h = Array.finalizeHash( h, 2 );
				a += h; b ^= h; c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		@SuppressWarnings( "unchecked" )
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		public boolean equals( R< K > other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    ( hasNullKey && nullKeyValue != other.nullKeyValue ) ||
			    size() != other.size() ) return false;
			
			for( long token = token(), t; index( token ) < _count; token = token( token ) )
				if( ( t = other.tokenOf( key( token ) ) ) == INVALID_TOKEN ||
				    value( token ) != other.value( t ) ) return false;
			return true;
		}
		
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
		 * <p>
		 * Uses the {@link #toJSON(JsonWriter)} method to generate the JSON output.
		 *
		 * @return A JSON string representing the map's contents.
		 */
		public String toString() { return toJSON(); }
		
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			long token = token();
			
			
			if( equal_hash_K == Array.string ) {
				json.enterObject();
				if( hasNullKey ) json.name().value( nullKeyValue );
				for( ; index( token ) < _count; token = token( token ) )
				     json.name( key( token ) == null ?
						                null :
						                key( token ).toString() )
				         .value( value( token ) );
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
				
				for( ; index( token ) < _count; token = token( token ) )
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
		
		protected long token( int index ) {
			return ( long ) _version << VERSION_SHIFT | index & INDEX_MASK;
		}
		
		protected int index( long token ) {
			return ( int ) ( token & INDEX_MASK );
		}
		
		protected int version( long token ) {
			return ( int ) ( token >>> VERSION_SHIFT );
		}
		
		
	}
	
	/**
	 * Read-write implementation extending the read-only base class
	 */
	class RW< K > extends R< K > {
		private static final int                                                        HashCollisionThreshold = 100;
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		public RW( Class< K > clazzK )                   { this( clazzK, 0 ); }
		
		public RW( Class< K > clazzK, int capacity )     { this( Array.get( clazzK ), capacity ); }
		
		public RW( Array.EqualHashOf< K > equal_hash_K ) { this( equal_hash_K, 0 ); }
		
		public RW( Array.EqualHashOf< K > equal_hash_K, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			if( capacity > 0 ) initialize( capacity );
		}
		
		public boolean put( K key, double value )         { return tryInsert( key, value, 1 ); }
		
		public boolean putNotExist( K key, double value ) { return tryInsert( key, value, 2 ); }
		
		public void putTry( K key, double value )         { tryInsert( key, value, 0 ); }
		
		public void clear() {
			if( _count == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( hash_nexts, 0, _count, 0L );
			Arrays.fill( keys, 0, _count, null );
			Arrays.fill( values, 0, _count, ( double ) 0 );
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
			hasNullKey = false;
			_version++;
		}
		
		public long remove( K key ) {
			if( key == null ) {
				if( !hasNullKey ) return INVALID_TOKEN;
				hasNullKey = false;
				_version++;
				return token( _count );
			}
			
			if( _buckets == null || _count == 0 ) return INVALID_TOKEN;
			
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
					return token( i );
				}
				last = i;
				i    = next( hash_next );
				if( hash_nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity is less than 0." );
			int currentCapacity = length();
			if( capacity <= currentCapacity ) return currentCapacity;
			_version++;
			if( _buckets == null ) return initialize( capacity );
			int newSize = Array.prime( capacity );
			resize( newSize, false );
			return newSize;
		}
		
		public void trim() { trim( count() ); }
		
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			int new_size        = Array.prime( capacity );
			if( currentCapacity <= new_size ) return;
			
			long[]        old_hash_next = hash_nexts;
			K[]           old_keys      = keys;
			double[] old_values    = values;
			int           old_count     = _count;
			_version++;
			initialize( new_size );
			copy( old_hash_next, old_keys, old_values, old_count );
		}
		
		private boolean tryInsert( K key, double value, int behavior ) {
			if( key == null ) {
				if( hasNullKey )
					switch( behavior ) {
						case 1:
							break;
						case 0:
							throw new IllegalArgumentException( "An item with the same key has already been added. Key: " + key );
						default:
							return false;
					}
				hasNullKey   = true;
				nullKeyValue = ( double ) value;
				_version++;
				return true;
			}
			
			if( _buckets == null ) initialize( 0 );
			long[] _hash_nexts    = hash_nexts;
			int    hash           = equal_hash_K.hashCode( key );
			int    collisionCount = 0;
			int    bucketIndex    = bucketIndex( hash );
			int    bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _hash_nexts.length; ) {
				if( hash( _hash_nexts[ next ] ) == hash && equal_hash_K.equals( keys[ next ], key ) )
					switch( behavior ) {
						case 1:
							values[ next ] = ( double ) value;
							_version++;
							return true;
						case 0:
							throw new IllegalArgumentException( "An item with the same key has already been added. Key: " + key );
						default:
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
			values[ index ]         = ( double ) value;
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			if( HashCollisionThreshold < collisionCount && this.forceNewHashCodes != null && key instanceof String ) resize( hash_nexts.length, true );
			return true;
		}
		
		private void resize( int new_size, boolean forceNewHashCodes ) {
			_version++;
			long[]        new_hash_next = Arrays.copyOf( hash_nexts, new_size );
			K[]           new_keys      = Arrays.copyOf( keys, new_size );
			double[] new_values    = Arrays.copyOf( values, new_size );
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
			capacity   = Array.prime( capacity );
			_buckets   = new int[ capacity ];
			hash_nexts = new long[ capacity ];
			keys       = equal_hash_K.copyOf( null, capacity );
			values     = new double[ capacity ];
			_freeList  = -1;
			return capacity;
		}
		
		private void copy( long[] old_hash_next, K[] old_keys, double[] old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( next( old_hash_next[ i ] ) < -1 ) continue;
				hash_nexts[ new_count ] = old_hash_next[ i ];
				keys[ new_count ]       = old_keys[ i ];
				values[ new_count ]     = old_values[ i ];
				int bucketIndex = bucketIndex( hash( old_hash_next[ i ] ) );
				next( hash_nexts, new_count, _buckets[ bucketIndex ] - 1 );
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