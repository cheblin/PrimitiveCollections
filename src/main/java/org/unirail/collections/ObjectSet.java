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

import java.util.*;
import java.util.function.Function;

/**
 * A generic Set implementation providing key storage with efficient operations.
 */
public interface ObjectSet {
	
	/**
	 * Read-only base class providing core functionality and state management
	 */
	abstract class R< K > implements java.util.Set< K >, JsonWriter.Source, Cloneable {
		protected boolean hasNullKey;    // True if Set contains null key
		
		public boolean hasNullKey() { return hasNullKey; }
		
		protected int[]                  _buckets;     // Hash table buckets
		protected long[]                 hash_nexts;   // Set entries: hashCode | next
		protected K[]                    keys;        // Set elements (keys)
		protected int                    _count;      // Number of elements excluding free list
		protected int                    _freeList;   // Index of first free list element
		protected int                    _freeCount;  // Number of free list elements
		protected int                    _version;   // Version for modification detection
		protected Array.EqualHashOf< K > equal_hash_K; // Equality and hash provider
		
		protected static final int  StartOfFreeList = -3;
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		protected static final int  VERSION_SHIFT   = 32;
		protected static final long INVALID_TOKEN   = -1L;
		
		@Override public int size() {
			return _count - _freeCount + (
					hasNullKey ?
							1 :
							0 );
		}
		
		public int count()                 { return size(); }
		
		@Override public boolean isEmpty() { return size() == 0; }
		
		@SuppressWarnings( "unchecked" )
		@Override public boolean contains( Object key ) { return tokenOf( ( K ) key ) != INVALID_TOKEN; }
		
		public boolean contains_( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
					token( _count ) :
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
		 * This token points to the smallest byte value present in the set or the null key if present and no byte values exist.
		 * May return {@link #INVALID_TOKEN} (-1) if the set is empty (no elements and no null key).
		 *
		 * @return The first iteration token, or {@link #INVALID_TOKEN} (-1) if the set is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( next( hash_nexts[ i ] ) >= -1 ) return token( i );
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next iteration token in the sequence, starting from the given {@code token}.
		 * This method is used to iterate through the elements of the set in ascending order of byte values,
		 * followed by the null key (if present). May return {@link #INVALID_TOKEN} (-1) if there are no more
		 * elements to iterate or if the provided token is invalid (e.g., due to set modification).
		 *
		 * @param token The current iteration token.
		 * @return The next iteration token, or {@link #INVALID_TOKEN} (-1) if there are no more elements or the token is invalid.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN || version( token ) != _version ) return INVALID_TOKEN;
			for( int i = index( token ) + 1; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return token( i );
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
		 * use {@link #key(long)} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
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
		
		public K key( long token ) {
			return hasNullKey && index( token ) == _count ?
					null :
					keys[ index( token ) ];
		}
		
		@Override public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
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
		
		@SuppressWarnings( "unchecked" )
		@Override public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< K > ) obj );
		}
		
		@SuppressWarnings( "unchecked" )
		public boolean equals( R< K > other ) {
			if( other == this ) return true;
			if( other == null || other.size() != size() || other.hasNullKey != hasNullKey ) return false;
			for( long token = token(); index( token ) < _count; token = token( token ) )
				if( !other.contains_( key( token ) ) ) return false;
			return true;
		}
		
		@SuppressWarnings( "unchecked" )
		@Override public R< K > clone() {
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
		
		@Override public void toJSON( JsonWriter json ) {
			int size = size();
			if( equal_hash_K == Array.string ) {
				json.enterObject();
				if( hasNullKey ) json.name().value();
				if( size > 0 ) {
					json.preallocate( size * 10 );
					for( long token = token(); index( token ) < _count; token = token( token ) )
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
		
		@Override public String toString()    { return toJSON(); } // Added missing method
		
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		protected static int hash( long hash_next ) {
			return ( int ) ( hash_next >> 32 );
		}
		
		protected static int next( long hash_next ) { return ( int ) ( hash_next & NEXT_MASK ); }
		
		protected long token( int index ) {
			return ( ( long ) _version << VERSION_SHIFT ) | ( index & INDEX_MASK );
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
		
		public RW( Array.EqualHashOf< K > equal_hash_K, Collection< ? extends K > collection ) {
			this.equal_hash_K = equal_hash_K;
			addAll( collection );
		}
		
		@Override public boolean add( K key ) {
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
			
			if( HashCollisionThreshold < collisionCount && this.forceNewHashCodes != null && key instanceof String ) resize( hash_nexts.length, true );
			return true;
		}
		
		@Override public boolean remove( Object key ) {
			@SuppressWarnings( "unchecked" )
			K k = ( K ) key;
			return remove_( k );
		}
		
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
		
		@Override public boolean addAll( Collection< ? extends K > keys ) {
			boolean modified = false;
			for( K key : keys ) if( add( key ) ) modified = true;
			return modified;
		}
		
		@Override public boolean removeAll( Collection< ? > keys ) {
			boolean modified = false;
			for( Object key : keys ) if( remove( key ) ) modified = true;
			return modified;
		}
		
		@Override public boolean retainAll( Collection< ? > keys ) {
			Objects.requireNonNull( keys );
			int                     v  = _version;
			java.util.Iterator< K > it = iterator();
			while( it.hasNext() )
				if( !keys.contains( it.next() ) ) it.remove();
			return v != _version;
		}
		
		@Override public void clear() {
			if( _count < 1 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( hash_nexts, 0, _count, 0L );
			Arrays.fill( keys, 0, _count, null );
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
			hasNullKey = false;
			_version++;
		}
		
		@Override public java.util.Iterator< K > iterator() {
			return new Iterator( this );
		}
		
		@Override public boolean containsAll( Collection< ? > src ) {
			for( Object element : src )
				if( !contains( element ) ) return false;
			return true;
		}
		
		@Override public Object[] toArray() {
			Object[] array = new Object[ size() ];
			int      index = 0;
			for( long token = token(); token != INVALID_TOKEN; token = token( token ) )
			     array[ index++ ] = key( token );
			return array;
		}
		
		@SuppressWarnings( "unchecked" )
		@Override public < T > T[] toArray( T[] a ) {
			int size = size();
			if( a.length < size ) return ( T[] ) Arrays.copyOf( toArray(), size, a.getClass() );
			System.arraycopy( toArray(), 0, a, 0, size );
			if( a.length > size ) a[ size ] = null;
			return a;
		}
		
		public void trim() { trim( count() ); }
		
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
		
		private int initialize( int capacity ) {
			_version++;
			
			_buckets   = new int[ capacity ];
			hash_nexts = new long[ capacity ];
			keys       = equal_hash_K.copyOf( null, capacity );
			_freeList  = -1;
			return capacity;
		}
		
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
		
		private void copy( long[] old_hash_next, K[] old_keys, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				final long hn = old_hash_next[ i ];
				if( next( hn ) < -1 ) continue;
				
				keys[ new_count ] = old_keys[ i ];
				int bucketIndex = bucketIndex( hash( hn ) );
				hash_nexts[ i ] = hn & 0xFFFF_FFFF_0000_0000L | _buckets[ bucketIndex ] - 1;
				
				_buckets[ bucketIndex ] = new_count + 1;
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		private static long hash_next( int hash, int next ) {
			return ( ( long ) hash << 32 ) | ( next & NEXT_MASK );
		}
		
		private static void next( long[] dst, int index, int next ) {
			dst[ index ] = ( dst[ index ] & HASH_CODE_MASK ) | ( next & NEXT_MASK );
		}
		
		private static void hash( long[] dst, int index, int hash ) {
			dst[ index ] = ( dst[ index ] & NEXT_MASK ) | ( ( long ) hash << 32 );
		}
		
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
			
			@Override public boolean hasNext() {
				if( _version != _set._version )
					throw new ConcurrentModificationException( "Collection was modified; enumeration operation may not execute." );
				_currentToken = _currentToken == INVALID_TOKEN ?
						_set.token() :
						_set.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			@Override public K next() {
				if( _version != _set._version )
					throw new ConcurrentModificationException( "Collection was modified; enumeration operation may not execute." );
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentKey = _set.key( _currentToken );
				return _currentKey;
			}
			
			@Override public void remove() {
				if( _version != _set._version )
					throw new ConcurrentModificationException( "Collection was modified; enumeration operation may not execute." );
				if( _currentKey == null ) throw new IllegalStateException();
				_set.remove_( _currentKey );
				_currentKey = null;
				_version    = _set._version;
			}
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() { return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT; }
}