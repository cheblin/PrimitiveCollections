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
 * {@code ObjectObjectNullMap} is an interface for a map that allows null keys and values.
 * It provides a contract for map implementations that need to handle null keys and values efficiently.
 */
public interface ObjectObjectNullMap {
	
	/**
	 * {@code R} is an abstract base class providing a read-only implementation of a {@link java.util.Map}
	 * that supports null keys and values.
	 * <p>
	 * It uses an open-addressing hash table for efficient storage and retrieval.
	 * {@code R} is designed for scenarios where read operations are frequent and mutations are either rare or not allowed.
	 * <p>
	 * This implementation does not support concurrent modifications from different threads.
	 * If concurrent access is required, external synchronization mechanisms must be used.
	 *
	 * @param <K> the type of the keys in this map
	 * @param <V> the type of the values in this map
	 */
	abstract class R< K, V > implements java.util.Map< K, V >, JsonWriter.Source, Cloneable {
		/**
		 * Flag indicating whether a null key is present in the map.
		 */
		protected       boolean                hasNullKey;
		/**
		 * Value associated with the null key, if present.
		 */
		protected       V                      nullKeyValue;
		/**
		 * Array of bucket indices for the hash table.
		 * The size of this array determines the capacity of the hash table.
		 */
		protected       int[]                  _buckets;
		/**
		 * Array storing hash codes and next entry indices for collision resolution.
		 * Each element packs the hash code and index of the next entry in the collision chain.
		 */
		protected       long[]                 hash_nexts;
		/**
		 * Array of keys stored in the map.
		 */
		protected       K[]                    keys;
		/**
		 * List of values corresponding to the keys.
		 * Uses {@link ObjectNullList.RW} to efficiently manage null values.
		 */
		protected       ObjectNullList.RW< V > values;
		/**
		 * The number of key-value mappings in this map, excluding free slots.
		 */
		protected       int                    _count;
		/**
		 * Index of the first free slot in the {@code hash_nexts}, {@code keys}, and {@code values} arrays, forming a free list.
		 */
		protected       int                    _freeList;
		/**
		 * The number of free slots available in the map.
		 */
		protected       int                    _freeCount;
		/**
		 * Version number for detecting concurrent modifications.
		 * Incremented on structural modifications to the map.
		 */
		protected       int                    _version;
		/**
		 * Strategy for comparing keys and calculating their hash codes.
		 * Defaults to {@link Array.EqualHashOf} based on the key type.
		 */
		protected       Array.EqualHashOf< K > equal_hash_K;
		/**
		 * Strategy for comparing values and calculating their hash codes.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		/**
		 * Cache for the key set view of the map.
		 */
		protected       KeyCollection          _keys;
		/**
		 * Cache for the value collection view of the map.
		 */
		protected       ValueCollection        _values;
		/**
		 * Cache for the entry set view of the map.
		 */
		protected       EntrySet               _entrySet;
		
		/**
		 * Constant indicating the start of the free list in the {@code hash_nexts} array.
		 */
		protected static final int  StartOfFreeList = -3;
		/**
		 * Mask to extract the hash code from a packed entry in {@code hash_nexts}.
		 */
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		/**
		 * Mask to extract the next entry index from a packed entry in {@code hash_nexts}.
		 */
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		/**
		 * Mask to extract the index from a token.
		 */
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		/**
		 * Number of bits to shift to extract the version from a token.
		 */
		protected static final int  VERSION_SHIFT   = 32;
		/**
		 * Constant representing an invalid token (-1).
		 */
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Constructs a new read-only map with the specified value equality and hash strategy.
		 *
		 * @param equal_hash_V the equality and hash strategy for values.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public int size() {
			return _count - _freeCount + (
					hasNullKey ?
							1 :
							0 );
		}
		
		/**
		 * Returns the number of key-value mappings in this map.
		 * This is an alias for {@link #size()}.
		 *
		 * @return the number of key-value mappings in this map
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the allocated capacity of the hash table.
		 *
		 * @return the length of the internal hash table arrays, or 0 if not initialized.
		 */
		public int length() {
			return hash_nexts == null ?
					0 :
					hash_nexts.length;
		}
		
		/**
		 * {@inheritDoc}
		 *
		 * @throws ClassCastException if the key is of an inappropriate type for this map
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean containsKey( Object key ) { return contains( ( K ) key ); }
		
		/**
		 * Checks if this map contains a mapping for the specified key.
		 *
		 * @param key key whose presence in this map is to be tested
		 * @return {@code true} if this map contains a mapping for the specified key
		 */
		public boolean contains( K key ) {
			return key == null ?
					hasNullKey :
					tokenOf( key ) != INVALID_TOKEN;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean containsValue( Object value ) {
			if( _count == 0 && !hasNullKey ) return false;
			if( hasNullKey && Objects.equals( nullKeyValue, value ) ) return true;
			for( int i = 0; i < _count; i++ )
				if( next( hash_nexts[ i ] ) >= -1 && Objects.equals( values.get( i ), value ) ) return true;
			return false;
		}
		
		/**
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		public V nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Returns a token associated with the given key, or {@link #INVALID_TOKEN} (-1) if the key is not found.
		 * <p>
		 * Tokens are used as stable identifiers for entries in the map, allowing iteration and access
		 * even if the map's internal structure changes due to resizing (in read-write implementations).
		 *
		 * @param key the key to find the token for
		 * @return a token representing the key-value mapping, or {@link #INVALID_TOKEN} (-1) if not found
		 * @throws ConcurrentModificationException if the map is modified during the search
		 */
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
		 * Returns a token for the first entry in the map, or {@link #INVALID_TOKEN} (-1) if the map is empty.
		 * <p>
		 * This is used to start iterating through the map using tokens.
		 *
		 * @return a token for the first entry, or {@link #INVALID_TOKEN} (-1) if empty
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return token( i );
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns a token for the next entry in the map after the entry represented by the given token.
		 * Returns {@link #INVALID_TOKEN} (-1) if there are no more entries or if the provided token is invalid or from a different version.
		 * <p>
		 * This is used to iterate through the map using tokens.
		 *
		 * @param token the current token
		 * @return a token for the next entry, or {@link #INVALID_TOKEN} (-1) if no more entries or token is invalid
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN || version( token ) != _version ) return INVALID_TOKEN;
			for( int i = index( token ) + 1; i < _count; i++ )
				if( next( hash_nexts[ i ] ) >= -1 ) return token( i );
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
		
		/**
		 * Returns the key associated with the given token.
		 *
		 * @param token the token of the entry
		 * @return the key associated with the token, or null if the token represents the null key entry
		 */
		public K key( long token ) {
			return hasNullKey && index( token ) == _count ?
					null :
					keys[ index( token ) ];
		}
		
		/**
		 * Checks if the entry associated with the given token has a value (i.e., is not a removed entry).
		 *
		 * @param token the token of the entry
		 * @return {@code true} if the entry has a value, {@code false} otherwise
		 */
		public boolean hasValue( long token ) {
			return index( token ) == _count ?
					nullKeyValue != null :
					values.hasValue( index( token ) );
		}
		
		/**
		 * Returns the value associated with the given token.
		 *
		 * @param token the token of the entry
		 * @return the value associated with the token
		 */
		public V value( long token ) {
			return hasNullKey && index( token ) == _count ?
					nullKeyValue :
					values.get( index( token ) );
		}
		
		/**
		 * {@inheritDoc}
		 *
		 * @throws ClassCastException if the key is of an inappropriate type for this map
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public V get( Object key ) {
			long token = tokenOf( ( K ) key );
			return token == INVALID_TOKEN ?
					null :
					value( token );
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public V getOrDefault( Object key, V defaultValue ) { return getOrDefault_( ( K ) key, defaultValue ); }
		
		/**
		 * Returns the value to which the specified key is mapped, or {@code defaultValue} if this map contains no mapping for the key.
		 *
		 * @param key          the key whose associated value is to be returned
		 * @param defaultValue the default value to be returned if this map does not contain a mapping for the key
		 * @return the value to which the specified key is mapped, or {@code defaultValue} if no mapping for the key is found
		 */
		public V getOrDefault_( K key, V defaultValue ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
					defaultValue :
					value( token );
		}
		
		/**
		 * {@inheritDoc}
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
		 * Seed value used in hashCode calculation.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * {@inheritDoc}
		 *
		 * @throws ClassCastException if the object is not of type {@code R} with compatible key and value types.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< K, V > ) obj );
		}
		
		/**
		 * Compares this map with another map for equality.
		 *
		 * @param other the other map to compare with
		 * @return {@code true} if the maps are equal, {@code false} otherwise
		 */
		public boolean equals( R< K, V > other ) {
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
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public R< K, V > clone() {
			try {
				R< K, V > dst = ( R< K, V > ) super.clone();
				if( _buckets != null ) {
					dst._buckets   = _buckets.clone();
					dst.hash_nexts = hash_nexts.clone();
					dst.keys       = keys.clone();
					dst.values     = values.clone(); // Clone ObjectNullList.RW
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace();
				return null;
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * {@inheritDoc}
		 */
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
		
		/**
		 * Calculates the bucket index for a given hash code.
		 *
		 * @param hash the hash code
		 * @return the bucket index
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Extracts the hash code from a packed entry value.
		 *
		 * @param packedEntry the packed entry value
		 * @return the hash code
		 */
		protected static int hash( long packedEntry ) { return ( int ) ( packedEntry >> 32 ); }
		
		/**
		 * Extracts the next entry index from a packed entry value.
		 *
		 * @param hash_next the packed entry value
		 * @return the next entry index
		 */
		protected static int next( long hash_next ) { return ( int ) ( hash_next & NEXT_MASK ); }
		
		/**
		 * Packs a hash code and next entry index into a long value.
		 *
		 * @param hash the hash code
		 * @param next the next entry index
		 * @return the packed entry value
		 */
		protected static long hash_next( int hash, int next ) { return ( ( long ) hash << 32 ) | ( next & NEXT_MASK ); }
		
		/**
		 * Sets the next entry index in a packed entry value.
		 *
		 * @param dst   the array to modify
		 * @param index the index in the array
		 * @param next  the next entry index to set
		 */
		protected static void next( long[] dst, int index, int next ) { dst[ index ] = ( dst[ index ] & HASH_CODE_MASK ) | ( next & NEXT_MASK ); }
		
		/**
		 * Sets the hash code in a packed entry value.
		 *
		 * @param dst   the array to modify
		 * @param index the index in the array
		 * @param hash  the hash code to set
		 */
		protected static void hash( long[] dst, int index, int hash ) { dst[ index ] = ( dst[ index ] & NEXT_MASK ) | ( ( long ) hash << 32 ); }
		
		/**
		 * Creates a token from a given index and the current version.
		 *
		 * @param index the index of the entry
		 * @return the token
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index & INDEX_MASK ); }
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token the token
		 * @return the index of the entry
		 */
		protected int index( long token ) { return ( int ) ( token & INDEX_MASK ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token the token
		 * @return the version number
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Set< K > keySet() { return keys(); }
		
		/**
		 * Returns a read-only {@link KeyCollection} view of the keys contained in this map.
		 * The collection is backed by the map, so changes to the map will be reflected in the collection.
		 *
		 * @return a set view of the keys contained in this map
		 */
		public KeyCollection keys() {
			return _keys == null ?
					_keys = new KeyCollection( this ) :
					_keys;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Collection< V > values() {
			return _values == null ?
					_values = new ValueCollection( this ) :
					_values;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Set< Entry< K, V > > entrySet() {
			return _entrySet == null ?
					_entrySet = new EntrySet( this ) :
					_entrySet;
		}
		
		/**
		 * Returns a read-only {@link Iterator} over the entries in this map.
		 *
		 * @return an iterator over the entries in this map
		 */
		public Iterator iterator() { return new Iterator( this ); }
		
		/**
		 * {@code KeyCollection} is a read-only set view of the keys in the map.
		 * It implements the {@link Set} interface and provides methods for accessing and iterating over keys.
		 * <p>
		 * Modification operations are not supported and will throw {@link UnsupportedOperationException}.
		 */
		public class KeyCollection extends AbstractSet< K > implements Set< K > {
			private final R< K, V > _map;
			
			/**
			 * Constructs a new KeyCollection backed by the given map.
			 *
			 * @param map the map to back this collection
			 */
			public KeyCollection( R< K, V > map ) { _map = map; }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean contains( Object item ) { return _map.containsKey( item ); }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public java.util.Iterator< K > iterator() { return new KeyIterator( _map ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public boolean add( K item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Copies the keys from this collection to an array, starting at the specified index.
			 *
			 * @param array the array to copy to
			 * @param index the starting index in the array
			 * @throws IndexOutOfBoundsException if the index is out of range
			 * @throws IllegalArgumentException  if the array is too small to hold all keys
			 */
			public void CopyTo( K[] array, int index ) {
				if( index < 0 || index > array.length ) throw new IndexOutOfBoundsException( "Index out of range" );
				if( array.length - index < _map.count() ) throw new IllegalArgumentException( "Arg_ArrayPlusOffTooSmall" );
				int    count   = _map._count;
				long[] entries = _map.hash_nexts;
				for( int i = 0; i < count; i++ )
					if( next( entries[ i ] ) >= -1 )
						array[ index++ ] = _map.keys[ i ];
			}
		}
		
		/**
		 * {@code KeyIterator} is a read-only iterator over the keys in the map.
		 * It implements the {@link java.util.Iterator} interface and provides methods for iterating over keys.
		 * <p>
		 * Modification operations are not supported and will throw {@link UnsupportedOperationException}.
		 */
		public class KeyIterator implements java.util.Iterator< K > {
			private final R< K, V > _map;
			private final int       _version;
			protected     long      _currentToken;
			private       K         _currentKey;
			
			/**
			 * Constructs a new KeyIterator for the given map.
			 *
			 * @param map the map to iterate over
			 */
			KeyIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws ConcurrentModificationException if the map is modified during iteration
			 */
			@Override
			public boolean hasNext() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				_currentToken = _currentToken == INVALID_TOKEN ?
						_map.token() :
						_map.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws ConcurrentModificationException if the map is modified during iteration
			 * @throws NoSuchElementException          if there are no more elements to return
			 */
			@Override
			public K next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentKey = _map.key( _currentToken );
				return _currentKey;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * {@code ValueCollection} is a read-only collection view of the values in the map.
		 * It implements the {@link Collection} interface and provides methods for accessing and iterating over values.
		 * <p>
		 * Modification operations are not supported and will throw {@link UnsupportedOperationException}.
		 */
		public class ValueCollection extends AbstractCollection< V > implements Collection< V > {
			private final R< K, V > _map;
			
			/**
			 * Constructs a new ValueCollection backed by the given map.
			 *
			 * @param map the map to back this collection
			 */
			public ValueCollection( R< K, V > map ) { _map = map; }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean contains( Object item ) { return _map.containsValue( item ); }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public java.util.Iterator< V > iterator() { return new ValueIterator( _map ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public boolean add( V item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public boolean remove( Object item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Copies the values from this collection to an array, starting at the specified index.
			 *
			 * @param array the array to copy to
			 * @param index the starting index in the array
			 * @throws NullPointerException      if the array is null
			 * @throws IndexOutOfBoundsException if the index is out of range
			 * @throws IllegalArgumentException  if the array is too small to hold all values
			 */
			public void CopyTo( V[] array, int index ) {
				if( array == null ) throw new NullPointerException( "array is null" );
				if( index < 0 || index > array.length ) throw new IndexOutOfBoundsException( "Index out of range" );
				if( array.length - index < _map.count() ) throw new IllegalArgumentException( "Arg_ArrayPlusOffTooSmall" );
				int    count   = _map._count;
				long[] entries = _map.hash_nexts;
				for( int i = 0; i < count; i++ )
					if( next( entries[ i ] ) >= -1 )
						array[ index++ ] = _map.values.get( i ); // Adjusted to use get(i)
			}
		}
		
		/**
		 * {@code ValueIterator} is a read-only iterator over the values in the map.
		 * It implements the {@link java.util.Iterator} interface and provides methods for iterating over values.
		 * <p>
		 * Modification operations are not supported and will throw {@link UnsupportedOperationException}.
		 */
		public class ValueIterator implements java.util.Iterator< V > {
			private final R< K, V > _map;
			private final int       _version;
			private       long      _currentToken;
			private       V         _currentValue;
			
			/**
			 * Constructs a new ValueIterator for the given map.
			 *
			 * @param map the map to iterate over
			 */
			ValueIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws ConcurrentModificationException if the map is modified during iteration
			 */
			@Override
			public boolean hasNext() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				_currentToken = _currentToken == INVALID_TOKEN ?
						_map.token() :
						_map.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws ConcurrentModificationException if the map is modified during iteration
			 * @throws NoSuchElementException          if there are no more elements to return
			 */
			@Override
			public V next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentValue = _map.value( _currentToken );
				return _currentValue;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * {@code EntrySet} is a read-only set view of the entries in the map.
		 * It implements the {@link Set} interface and provides methods for accessing and iterating over entries.
		 * <p>
		 * Modification operations are not supported and will throw {@link UnsupportedOperationException}.
		 */
		public class EntrySet extends AbstractSet< Entry< K, V > > implements Set< Entry< K, V > > {
			private final R< K, V > _map;
			
			/**
			 * Constructs a new EntrySet backed by the given map.
			 *
			 * @param map the map to back this set
			 */
			public EntrySet( R< K, V > map ) { _map = map; }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean contains( Object o ) { return _map.containsKey( o ); }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public java.util.Iterator< Entry< K, V > > iterator() { return new EntryIterator( _map ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public boolean add( Entry< K, V > entry ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public boolean remove( Object o ) { throw new UnsupportedOperationException( "Read-only collection" ); }
		}
		
		/**
		 * {@code EntryIterator} is a read-only iterator over the entries in the map.
		 * It implements the {@link java.util.Iterator} interface and provides methods for iterating over entries.
		 * <p>
		 * Modification operations are not supported and will throw {@link UnsupportedOperationException}.
		 */
		public class EntryIterator implements java.util.Iterator< Entry< K, V > > {
			private final R< K, V >     _map;
			private final int           _version;
			protected     long          _currentToken;
			private       Entry< K, V > _currentEntry;
			
			/**
			 * Constructs a new EntryIterator for the given map.
			 *
			 * @param map the map to iterate over
			 */
			EntryIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws ConcurrentModificationException if the map is modified during iteration
			 */
			@Override
			public boolean hasNext() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				_currentToken = _currentToken == INVALID_TOKEN ?
						_map.token() :
						_map.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws ConcurrentModificationException if the map is modified during iteration
			 * @throws NoSuchElementException          if there are no more elements to return
			 */
			@Override
			public Entry< K, V > next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentEntry = new AbstractMap.SimpleEntry<>( _map.key( _currentToken ), _map.value( _currentToken ) );
				return _currentEntry;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * {@code Iterator} is a read-only iterator over the entries in the map.
		 * It is an alias for {@link EntryIterator}.
		 * <p>
		 * Modification operations are not supported and will throw {@link UnsupportedOperationException}.
		 */
		public class Iterator implements java.util.Iterator< Entry< K, V > > {
			private final R< K, V >     _map;
			private final int           _version;
			protected     long          _currentToken;
			private       Entry< K, V > _current;
			
			/**
			 * Constructs a new Iterator for the given map.
			 *
			 * @param map the map to iterate over
			 */
			Iterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws ConcurrentModificationException if the map is modified during iteration
			 */
			@Override
			public boolean hasNext() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				_currentToken = _currentToken == INVALID_TOKEN ?
						_map.token() :
						_map.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws ConcurrentModificationException if the map is modified during iteration
			 * @throws NoSuchElementException          if there are no more elements to return
			 */
			@Override
			public Entry< K, V > next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_current = new AbstractMap.SimpleEntry<>( _map.key( _currentToken ), _map.value( _currentToken ) );
				return _current;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * {@inheritDoc}
		 *
		 * @throws UnsupportedOperationException always
		 */
		@Override
		public V put( K key, V value ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * {@inheritDoc}
		 *
		 * @throws UnsupportedOperationException always
		 */
		@Override
		public V remove( Object key ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * {@inheritDoc}
		 *
		 * @throws UnsupportedOperationException always
		 */
		@Override
		public void putAll( Map< ? extends K, ? extends V > m ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * {@inheritDoc}
		 *
		 * @throws UnsupportedOperationException always
		 */
		@Override
		public void clear() { throw new UnsupportedOperationException( "Read-only map" ); }
	}
	
	/**
	 * {@code RW} is a read-write implementation of {@code ObjectObjectMap}.
	 * It extends {@link R} and provides mutable map operations like {@code put}, {@code remove}, and {@code clear}.
	 * <p>
	 * It uses the same open-addressing hash table structure as {@code R} but allows modifications.
	 * {@code RW} is suitable for general-purpose map usage where both read and write operations are required.
	 * <p>
	 * Like {@code R}, this implementation is not thread-safe. External synchronization is needed for concurrent access.
	 *
	 * @param <K> the type of the keys in this map
	 * @param <V> the type of the values in this map
	 */
	class RW< K, V > extends R< K, V > {
		/**
		 * Threshold for hash collision count. If collisions exceed this threshold, resizing with forced rehash might be triggered.
		 */
		private static final int                                                        HashCollisionThreshold = 100;
		/**
		 * Optional function to force new hash codes for keys during resize operations, potentially to mitigate hash collisions.
		 * Applied when {@code HashCollisionThreshold} is exceeded and key type is String.
		 */
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs a new read-write map with default capacity.
		 *
		 * @param clazzK the class of keys
		 * @param clazzV the class of values
		 */
		public RW( Class< K > clazzK, Class< V > clazzV ) { this( clazzK, clazzV, 0 ); }
		
		/**
		 * Constructs a new read-write map with the specified initial capacity.
		 *
		 * @param clazzK   the class of keys
		 * @param clazzV   the class of values
		 * @param capacity the initial capacity
		 */
		public RW( Class< K > clazzK, Class< V > clazzV, int capacity ) { this( Array.get( clazzK ), Array.get( clazzV ), capacity ); }
		
		/**
		 * Constructs a new read-write map with default capacity and specified value equality/hash strategy.
		 *
		 * @param clazzK       the class of keys
		 * @param equal_hash_V the equality and hash strategy for values
		 */
		public RW( Class< K > clazzK, Array.EqualHashOf< V > equal_hash_V ) { this( Array.get( clazzK ), equal_hash_V, 0 ); }
		
		/**
		 * Constructs a new read-write map with specified capacity and value equality/hash strategy.
		 *
		 * @param clazzK       the class of keys
		 * @param equal_hash_V the equality and hash strategy for values
		 * @param capacity     the initial capacity
		 */
		public RW( Class< K > clazzK, Array.EqualHashOf< V > equal_hash_V, int capacity ) { this( Array.get( clazzK ), equal_hash_V, capacity ); }
		
		/**
		 * Constructs a new read-write map with default capacity and specified key and value equality/hash strategies.
		 *
		 * @param equal_hash_K the equality and hash strategy for keys
		 * @param equal_hash_V the equality and hash strategy for values
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V ) { this( equal_hash_K, equal_hash_V, 0 ); }
		
		/**
		 * Constructs a new read-write map with specified capacity and key and value equality/hash strategies.
		 *
		 * @param equal_hash_K the equality and hash strategy for keys
		 * @param equal_hash_V the equality and hash strategy for values
		 * @param capacity     the initial capacity
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V, int capacity ) {
			super( equal_hash_V );
			this.equal_hash_K = equal_hash_K;
			if( capacity > 0 ) initialize( capacity );
		}
		
		/**
		 * Constructs a new read-write map initialized with the mappings from the given map.
		 *
		 * @param equal_hash_K the equality and hash strategy for keys
		 * @param equal_hash_V the equality and hash strategy for values
		 * @param map          the map whose mappings are to be placed in this map
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V, java.util.Map< ? extends K, ? extends V > map ) {
			super( equal_hash_V );
			this.equal_hash_K = equal_hash_K;
			putAll( map );
		}
		
		/**
		 * Constructs a new read-write map initialized with the entries from the given collection.
		 *
		 * @param equal_hash_K the equality and hash strategy for keys
		 * @param equal_hash_V the equality and hash strategy for values
		 * @param collection   the collection of entries to initialize the map with
		 */
		@SuppressWarnings( "unchecked" )
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V, Iterable< Entry< K, V > > collection ) {
			super( equal_hash_V );
			this.equal_hash_K = equal_hash_K;
			if( collection instanceof RW ) {
				RW< K, V > source = ( RW< K, V > ) collection;
				if( source.count() > 0 ) copy( source.hash_nexts, source.keys, source.values, source._count );
			}
			else {
				for( Entry< K, V > pair : collection ) put( pair.getKey(), pair.getValue() );
			}
		}
		
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void putAll( Map< ? extends K, ? extends V > m ) {
			for( Entry< ? extends K, ? extends V > entry : m.entrySet() ) put( entry.getKey(), entry.getValue() );
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear() {
			if( _count == 0 && !hasNullKey ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( hash_nexts, 0, _count, 0L );
			Arrays.fill( keys, 0, _count, null );
			values.clear(); // Adjusted to use clear() from ObjectNullList.RW
			_count       = 0;
			_freeList    = -1;
			_freeCount   = 0;
			hasNullKey   = false;
			nullKeyValue = null;
			_version++;
		}
		
		/**
		 * {@inheritDoc}
		 *
		 * @throws ClassCastException if the key is of an inappropriate type for this map
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public V remove( Object key ) { return remove_( ( K ) key ); }
		
		/**
		 * Removes the mapping for a key from this map if it is present.
		 *
		 * @param key key whose mapping is to be removed from the map
		 * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}
		 */
		public V remove_( K key ) {
			if( key == null ) {
				if( !hasNullKey ) return null;
				hasNullKey = false;
				V oldValue = nullKeyValue;
				nullKeyValue = null;
				_version++;
				return oldValue;
			}
			
			if( _buckets == null || _count == 0 ) return null;
			
			int hash           = equal_hash_K.hashCode( key );
			int bucketIndex    = bucketIndex( hash );
			int last           = -1;
			int i              = _buckets[ bucketIndex ] - 1;
			int collisionCount = 0;
			
			while( -1 < i ) {
				long hash_next = hash_nexts[ i ];
				if( hash( hash_next ) == hash && equal_hash_K.equals( keys[ i ], key ) ) {
					V oldValue = values.get( i ); // Adjusted to use get(i)
					
					if( last < 0 ) _buckets[ bucketIndex ] = next( hash_next ) + 1;
					else next( hash_nexts, last, next( hash_next ) );
					
					final int     lastNonNullValue = values.nulls.last1();
					final boolean hasValue         = values.hasValue( i );
					
					if( i != lastNonNullValue && hasValue ) {
						int BucketOf_KeyToMove = bucketIndex( Array.hash( keys[ lastNonNullValue ] ) );
						keys[ i ] = keys[ lastNonNullValue ];
						next( hash_nexts, i, next( hash_nexts[ lastNonNullValue ] ) );
						values.set1( i, values.get( lastNonNullValue ) );
						
						int prev = -1;
						collisionCount = 0;
						for( int current = _buckets[ BucketOf_KeyToMove ] - 1; current != lastNonNullValue; prev = current, current = next( hash_nexts[ current ] ) )
							if( hash_nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
						
						if( prev < 0 ) _buckets[ BucketOf_KeyToMove ] = i + 1;
						else next( hash_nexts, prev, next( hash_nexts[ lastNonNullValue ] ) );
						
						values.set1( lastNonNullValue, null );
						i = lastNonNullValue;
					}
					else if( hasValue ) values.set1( i, null );
					
					next( hash_nexts, i, StartOfFreeList - _freeList );
					keys[ i ] = null;
					_freeList = i;
					_freeCount++;
					_version++;
					return oldValue;
				}
				last = i;
				i    = next( hash_next );
				if( hash_nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return null;
		}
		
		/**
		 * Ensures that the capacity of this map is at least the specified capacity.
		 * If the current capacity is less than {@code capacity}, a resize operation is performed.
		 *
		 * @param capacity the desired minimum capacity
		 * @return the new capacity of the map after ensuring the capacity
		 * @throws IllegalArgumentException if the capacity is negative
		 */
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
		
		/**
		 * Reduces the capacity of this map to be equal to the current size, if possible.
		 * If the current capacity is already at or below the current size, this method does nothing.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Reduces the capacity of this map to be at least as large as the specified capacity.
		 * If the current capacity is already at or below the specified capacity, this method does nothing.
		 *
		 * @param capacity the desired capacity to trim to
		 * @throws IllegalArgumentException if the capacity is less than the current size of the map
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			int new_size        = Array.prime( capacity );
			if( currentCapacity <= new_size ) return;
			
			long[]                 old_hash_next = hash_nexts;
			K[]                    old_keys      = keys;
			ObjectNullList.RW< V > old_values    = values.clone(); // Adjusted to clone ObjectNullList.RW
			int                    old_count     = _count;
			_version++;
			initialize( new_size );
			copy( old_hash_next, old_keys, old_values, old_count );
		}
		
		/**
		 * Tries to insert a new key-value mapping into the map.
		 * Handles different insertion behaviors based on the {@code behavior} parameter.
		 *
		 * @param key   the key to insert
		 * @param value the value to associate with the key
		 * @return {@code true} if insertion was successful, {@code false} otherwise (depending on behavior)
		 * @throws ConcurrentModificationException if the map is modified during insertion
		 */
		public V put( K key, V value ) {
			if( key == null ) {
				_version++;
				if( hasNullKey ) {
					V ret = nullKeyValue;
					nullKeyValue = value;
					return ret;
				}
				hasNullKey   = true;
				nullKeyValue = value;
				return null;
			}
			
			if( _buckets == null ) initialize( 7 );
			long[] _hash_nexts    = hash_nexts;
			int    hash           = equal_hash_K.hashCode( key );
			int    collisionCount = 0;
			int    bucketIndex    = bucketIndex( hash );
			int    bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _hash_nexts.length; ) {
				if( hash( _hash_nexts[ next ] ) == hash && equal_hash_K.equals( keys[ next ], key ) ) {
					_version++;
					
					V ret = values.get( next );
					values.set1( next, value ); // Adjusted to use set1(i, value)
					return ret;
				}
				
				next = next( _hash_nexts[ next ] );
				if( _hash_nexts.length < collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			
			int index;
			if( 0 < _freeCount ) {
				index     = _freeList;
				_freeList = StartOfFreeList - next( hash_nexts[ _freeList ] );
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
			values.set1( index, value ); // Adjusted to use set1(index, value)
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			if( HashCollisionThreshold < collisionCount && this.forceNewHashCodes != null && key instanceof String )
				resize( hash_nexts.length, true );
			return null;
		}
		
		/**
		 * Resizes the internal hash table to a new capacity.
		 * Optionally forces rehashing of keys if {@code forceNewHashCodes} is enabled and collisions are high.
		 *
		 * @param new_size          the new capacity of the hash table
		 * @param forceNewHashCodes whether to force rehashing of keys
		 * @throws ConcurrentModificationException if the map is modified during resize
		 */
		private void resize( int new_size, boolean forceNewHashCodes ) {
			_version++;
			long[]                 new_hash_next = Arrays.copyOf( hash_nexts, new_size );
			K[]                    new_keys      = Arrays.copyOf( keys, new_size );
			ObjectNullList.RW< V > new_values    = values.clone(); // Adjusted to clone ObjectNullList.RW
			new_values.length( new_size ); // Adjust capacity
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
			values     = new_values;
		}
		
		/**
		 * Initializes the hash table with the given capacity.
		 * Allocates bucket, hash_nexts, keys, and values arrays.
		 *
		 * @param capacity the initial capacity of the hash table
		 * @return the initialized capacity
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets   = new int[ capacity ];
			hash_nexts = new long[ capacity ];
			keys       = equal_hash_K.copyOf( null, capacity );
			values     = new ObjectNullList.RW<>( equal_hash_V, capacity ); // Initialize ObjectNullList.RW
			_freeList  = -1;
			return capacity;
		}
		
		/**
		 * Copies entries from old arrays to the newly resized arrays.
		 *
		 * @param old_hash_next old hash_nexts array
		 * @param old_keys      old keys array
		 * @param old_values    old values list
		 * @param old_count     old entry count
		 */
		private void copy( long[] old_hash_next, K[] old_keys, ObjectNullList.RW< V > old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				final long hn = old_hash_next[ i ];
				if( next( hn ) < -1 ) continue;
				keys[ new_count ] = old_keys[ i ];
				values.set1( new_count, old_values.get( i ) ); // Adjusted to use set1 and get
				int bucketIndex = bucketIndex( hash( hn ) );
				hash_nexts[ new_count ] = hn & 0xFFFF_FFFF_0000_0000L | _buckets[ bucketIndex ] - 1;
				_buckets[ bucketIndex ] = new_count + 1;
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		/**
		 * Copies entries from this map to an array of {@link Entry} objects.
		 *
		 * @param array the array to copy to
		 * @param index the starting index in the array
		 * @throws IndexOutOfBoundsException if the index is out of range
		 * @throws IllegalArgumentException  if the array is too small to hold all entries
		 */
		private void copyTo( Entry< K, V >[] array, int index ) {
			if( index < 0 || index > array.length ) throw new IndexOutOfBoundsException( "Index out of range" );
			if( array.length - index < count() ) throw new IllegalArgumentException( "Arg_ArrayPlusOffTooSmall" );
			int    count   = _count;
			long[] entries = hash_nexts;
			for( int i = 0; i < count; i++ )
				if( next( entries[ i ] ) >= -1 )
					array[ index++ ] = new AbstractMap.SimpleEntry<>( keys[ i ], values.get( i ) ); // Adjusted to use get(i)
		}
		
		// Override inner classes to support mutation
		
		/**
		 * {@code KeyCollection} for {@link RW} is a mutable set view of the keys in the map.
		 * It extends {@link R.KeyCollection} and overrides modification operations to support adding and removing keys.
		 */
		public final class KeyCollection extends R< K, V >.KeyCollection {
			RW< K, V > _map;
			
			/**
			 * Constructs a new KeyCollection backed by the given read-write map.
			 *
			 * @param map the read-write map to back this collection
			 */
			public KeyCollection( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @return {@code true} if the key was successfully added, {@code false} if the key was already present.
			 */
			@Override
			public boolean add( K item ) { return _map.put( item, null ) != null; }
			
			/**
			 * {@inheritDoc}
			 *
			 * @return {@code true} if a key was removed, {@code false} otherwise.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) { return _map.remove( o ) != null; }
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code KeyIterator} for {@link RW} is a mutable iterator over the keys in the map.
		 * It extends {@link R.KeyIterator} and overrides the {@code remove()} operation to support removing keys during iteration.
		 */
		public class KeyIterator extends R< K, V >.KeyIterator {
			RW< K, V > _map;
			
			/**
			 * Constructs a new KeyIterator for the given read-write map.
			 *
			 * @param map the read-write map to iterate over
			 */
			KeyIterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws IllegalStateException if {@code next()} has not yet been called, or {@code remove()} has already been called after the last call to {@code next()}.
			 */
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove( _map.key( _currentToken ) );
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * {@code ValueCollection} for {@link RW} is a mutable collection view of the values in the map.
		 * It extends {@link R.ValueCollection} and overrides modification operations to support adding and removing values.
		 */
		public final class ValueCollection extends R< K, V >.ValueCollection {
			RW< K, V > _map;
			
			/**
			 * Constructs a new ValueCollection backed by the given read-write map.
			 *
			 * @param map the read-write map to back this collection
			 */
			public ValueCollection( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @return {@code true} if the value was successfully added, which is always true as null key is used.
			 */
			@Override
			public boolean add( V item ) { return _map.put( null, item ) != null; }
			
			/**
			 * {@inheritDoc}
			 * Removes the first occurrence of the specified value from this collection, if it is present.
			 *
			 * @return {@code true} if a value was removed, {@code false} otherwise.
			 */
			@Override
			public boolean remove( Object item ) {
				for( long token = _map.token(); token != INVALID_TOKEN; token = _map.token( token ) ) {
					if( Objects.equals( _map.value( token ), item ) ) {
						_map.remove( _map.key( token ) );
						return true;
					}
				}
				return false;
			}
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code ValueIterator} for {@link RW} is a read-only iterator over the values in the map.
		 * It extends {@link R.ValueIterator}. Removal by value is not supported.
		 */
		public class ValueIterator extends R< K, V >.ValueIterator {
			/**
			 * Constructs a new ValueIterator for the given read-write map.
			 *
			 * @param map the read-write map to iterate over
			 */
			ValueIterator( RW< K, V > map ) { super( map ); }
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws UnsupportedOperationException always, remove by value is not supported.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "remove by value is not supported" ); }
		}
		
		/**
		 * {@code EntrySet} for {@link RW} is a mutable set view of the entries in the map.
		 * It extends {@link R.EntrySet} and overrides modification operations to support adding and removing entries.
		 */
		public final class EntrySet extends R< K, V >.EntrySet {
			RW< K, V > _map;
			
			/**
			 * Constructs a new EntrySet backed by the given read-write map.
			 *
			 * @param map the read-write map to back this set
			 */
			public EntrySet( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @return {@code true} if the entry was successfully added, {@code false} if an entry with the same key was already present.
			 * @throws NullPointerException if the specified entry is null
			 */
			@Override
			public boolean add( Entry< K, V > entry ) {
				if( entry == null ) throw new NullPointerException( "Entry is null" );
				return _map.put( entry.getKey(), entry.getValue() ) != null;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @return {@code true} if an entry was removed, {@code false} otherwise.
			 * @throws ClassCastException if the object is not of type {@link Entry}
			 */
			@Override
			public boolean remove( Object o ) {
				if( !( o instanceof Entry ) ) return false;
				Entry< ?, ? > entry = ( Entry< ?, ? > ) o;
				return _map.remove( entry.getKey() ) != null;
			}
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code EntryIterator} for {@link RW} is a mutable iterator over the entries in the map.
		 * It extends {@link R.EntryIterator} and overrides the {@code remove()} operation to support removing entries during iteration.
		 */
		public class EntryIterator extends R< K, V >.EntryIterator {
			RW< K, V > _map;
			
			/**
			 * Constructs a new EntryIterator for the given read-write map.
			 *
			 * @param map the read-write map to iterate over
			 */
			EntryIterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws IllegalStateException if {@code next()} has not yet been called, or {@code remove()} has already been called after the last call to {@code next()}.
			 */
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove( _map.key( _currentToken ) );
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator iterator() { return new Iterator( this ); }
		
		/**
		 * {@code Iterator} for {@link RW} is a mutable iterator over the entries in the map.
		 * It extends {@link R.Iterator} and is an alias for {@link RW.EntryIterator}.
		 */
		public class Iterator extends R< K, V >.Iterator {
			RW< K, V > _map;
			
			/**
			 * Constructs a new Iterator for the given read-write map.
			 *
			 * @param map the read-write map to iterate over
			 */
			Iterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * {@inheritDoc}
			 *
			 * @throws IllegalStateException if {@code next()} has not yet been called, or {@code remove()} has already been called after the last call to {@code next()}.
			 */
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove( _map.key( _currentToken ) );
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * Static instance of {@link Array.EqualHashOf} for {@link RW} class, used for default equality and hash operations.
		 */
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Returns a default {@link Array.EqualHashOf} instance for {@link RW} class.
	 *
	 * @param <K> the key type of the map
	 * @param <V> the value type of the map
	 * @return a default {@link Array.EqualHashOf} instance for {@link RW}
	 */
	@SuppressWarnings( "unchecked" )
	static < K, V > Array.EqualHashOf< RW< K, V > > equal_hash() { return ( Array.EqualHashOf< RW< K, V > > ) RW.OBJECT; }
}