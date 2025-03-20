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
 * {@code ObjectObjectMap} is a generic interface for a Map that stores key-value pairs where both keys and values are Objects.
 * It provides efficient methods for common map operations.
 */
public interface ObjectObjectMap {
	
	
	/**
	 * {@code R} is an abstract read-only base class for {@code ObjectObjectMap} implementations.
	 * It provides core functionalities for map operations without modification capabilities.
	 * Subclasses can extend this class to implement specific read-only or read-write map behaviors.
	 *
	 * @param <K> The type of keys in the map.
	 * @param <V> The type of values in the map.
	 */
	abstract class R< K, V > implements java.util.Map< K, V >, JsonWriter.Source, Cloneable {
		/**
		 * Indicates whether the map contains a null key.
		 */
		protected       boolean                hasNullKey;
		/**
		 * Stores the value associated with the null key, if present.
		 */
		protected       V                      nullKeyValue;
		/**
		 * Array of buckets for the hash table, used for efficient key lookups.
		 */
		protected       int[]                  _buckets;
		/**
		 * Array storing hash codes and next entry indices for collision resolution in the hash table.
		 */
		protected       long[]                 hash_nexts;
		/**
		 * Array of keys stored in the map.
		 */
		protected       K[]                    keys;
		/**
		 * Array of values corresponding to the keys in the map.
		 */
		protected       V[]                    values;
		/**
		 * The current number of entries in the map (including free slots).
		 */
		protected       int                    _count;
		/**
		 * Index of the first free entry in the {@code hash_nexts}, {@code keys}, and {@code values} arrays, forming a free list for reuse.
		 */
		protected       int                    _freeList;
		/**
		 * The number of free entries available in the map's internal arrays.
		 */
		protected       int                    _freeCount;
		/**
		 * Version number used for detecting concurrent modifications. Incremented on structural changes.
		 */
		protected       int                    _version;
		/**
		 * Strategy for determining equality and hash code of keys.
		 */
		protected       Array.EqualHashOf< K > equal_hash_K;
		/**
		 * Strategy for determining equality and hash code of values.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		/**
		 * Lazily initialized set view of the keys contained in this map.
		 */
		protected       KeyCollection          _keys;
		/**
		 * Lazily initialized collection view of the values contained in this map.
		 */
		protected       ValueCollection        _values;
		/**
		 * Lazily initialized set view of the entries contained in this map.
		 */
		protected       EntrySet               _entrySet;
		
		/**
		 * Constant indicating the start of the free list in the {@code hash_nexts} array.
		 */
		protected static final int  StartOfFreeList = -3;
		/**
		 * Mask to extract the hash code from a packed entry in the {@code hash_nexts} array.
		 */
		protected static final long HASH_CODE_MASK  = 0xFFFFFFFF00000000L;
		/**
		 * Mask to extract the next index from a packed entry in the {@code hash_nexts} array.
		 */
		protected static final long NEXT_MASK       = 0x00000000FFFFFFFFL;
		/**
		 * Mask to extract the index from a token.
		 */
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		/**
		 * Bit shift for extracting the version from a token.
		 */
		protected static final int  VERSION_SHIFT   = 32;
		/**
         * Represents an invalid token, typically returned when a key is not found or iteration has completed. Value is -1.
		 */
		protected static final long INVALID_TOKEN   = -1L;
		
		/**
		 * Constructs a read-only {@code ObjectObjectMap.R} with the specified value equality and hash strategy.
		 *
		 * @param equal_hash_V The strategy for value equality and hash code calculation.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		@Override
		public boolean isEmpty() { return size() == 0; }
		
		@Override
		public int size() {
			return _count - _freeCount + ( hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the number of key-value mappings in this map.
		 * Alias for {@link #size()}.
		 *
		 * @return the number of key-value mappings in this map
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the total capacity (number of slots) of the internal hash table.
		 * This includes both occupied and free slots.
		 *
		 * @return the capacity of the internal hash table.
		 */
		public int length() {
			return hash_nexts == null ?
					0 :
					hash_nexts.length;
		}
		
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
		
		@Override
		public boolean containsValue( Object value ) {
			if( _count == 0 && !hasNullKey ) return false;
			if( hasNullKey && Objects.equals( nullKeyValue, value ) ) return true;
			for( int i = 0; i < _count; i++ )
				if( next( hash_nexts[ i ] ) >= -1 && Objects.equals( values[ i ], value ) ) return true;
			return false;
		}
		/**
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		
		public V nullKeyValue() { return  nullKeyValue; }
		/**
		 * Returns a token associated with the given key, if present in the map.
		 * A token is a unique identifier that can be used to efficiently access the key-value pair.
		 *
		 * @param key the key to find the token for
         * @return a valid token if the key is found, or {@link #INVALID_TOKEN} (-1) if the key is not found
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
		 * Returns a token for the first entry in the map.
		 * Useful for iterating through the map using tokens.
		 *
         * @return a token for the first entry, or {@link #INVALID_TOKEN} (-1) if the map is empty
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < next( hash_nexts[ i ] ) ) return token( i );
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns a token for the next entry in the map after the entry associated with the given token.
		 *
		 * @param token the token of the current entry
         * @return a token for the next entry, or {@link #INVALID_TOKEN} (-1) if there are no more entries or the token is invalid
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
		 * Returns the key associated with the given token.
		 *
		 * @param token the token of the entry
		 * @return the key associated with the token
		 * @throws ConcurrentModificationException if the map has been modified since the token was obtained
		 */
		public K key( long token ) {
				return hasNullKey && index( token ) == _count ?
						null :
						keys[ index( token ) ];
		}
		
		/**
		 * Returns the value associated with the given token.
		 *
		 * @param token the token of the entry
		 * @return the value associated with the token
		 * @throws ConcurrentModificationException if the map has been modified since the token was obtained
		 */
		public V value( long token ) {
				return hasNullKey && index( token ) == _count ?
						nullKeyValue :
						values[ index( token ) ];
		}
		
		@Override
		@SuppressWarnings( "unchecked" )
		public V get( Object key ) {
			long token = tokenOf( ( K ) key );
			return token == INVALID_TOKEN ?
					null :
					value( token );
		}
		
		@Override
		@SuppressWarnings( "unchecked" )
		public V getOrDefault( Object key, V defaultValue ) { return getOrDefault_( ( K ) key, defaultValue ); }
		
		/**
		 * Returns the value to which the specified key is mapped, or {@code defaultValue} if this map contains no mapping for the key.
		 *
		 * @param key          the key whose associated value is to be returned
		 * @param defaultValue the default value to return if this map does not contain a mapping for the key
		 * @return the value to which the specified key is mapped, or {@code defaultValue} if this map contains no mapping for the key
		 */
		public V getOrDefault_( K key, V defaultValue ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
					defaultValue :
					value( token );
		}
		
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) );
				h = Array.mix( h, Array.hash( value( token ) == null ?
						                              seed :
						                              equal_hash_V.hashCode( value( token ) ) ) );
				h = Array.finalizeHash( h, 2 );
				a += h; b ^= h; c *= h | 1;
			}
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue != null ?
						                              equal_hash_V.hashCode( nullKeyValue ) :
						                              seed ) );
				h = Array.finalizeHash( h, 2 );
				a += h; b ^= h; c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< K, V > ) obj );
		}
		
		/**
		 * Compares this read-only map with another read-only map for equality.
		 * Two maps are considered equal if they contain the same mappings.
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
		
		@Override
		@SuppressWarnings( "unchecked" )
		public R< K, V > clone() {
			try {
				R< K, V > dst = ( R< K, V > ) super.clone();
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
		 * @param hash the hash code of the key
		 * @return the bucket index
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Extracts the hash code from a packed entry in the {@code hash_nexts} array.
		 *
		 * @param packedEntry the packed entry (hash code and next index)
		 * @return the extracted hash code
		 */
		protected static int hash( long packedEntry ) { return ( int ) ( packedEntry >> 32 ); }
		
		/**
		 * Extracts the next index from a packed entry in the {@code hash_nexts} array.
		 *
		 * @param hash_next the packed entry (hash code and next index)
		 * @return the extracted next index
		 */
		protected static int next( long hash_next ) { return ( int ) ( hash_next & NEXT_MASK ); }
		
		/**
		 * Packs a hash code and a next index into a long value for storage in the {@code hash_nexts} array.
		 *
		 * @param hash the hash code
		 * @param next the next index
		 * @return the packed hash code and next index
		 */
		protected static long hash_next( int hash, int next ) { return ( ( long ) hash << 32 ) | ( next & NEXT_MASK ); }
		
		/**
		 * Sets the next index part of a packed entry in the {@code hash_nexts} array.
		 *
		 * @param dst   the {@code hash_nexts} array
		 * @param index the index of the entry to modify
		 * @param next  the new next index value
		 */
		protected static void next( long[] dst, int index, int next ) { dst[ index ] = ( dst[ index ] & HASH_CODE_MASK ) | ( next & NEXT_MASK ); }
		
		/**
		 * Sets the hash code part of a packed entry in the {@code hash_nexts} array.
		 *
		 * @param dst   the {@code hash_nexts} array
		 * @param index the index of the entry to modify
		 * @param hash  the new hash code value
		 */
		protected static void hash( long[] dst, int index, int hash ) { dst[ index ] = ( dst[ index ] & NEXT_MASK ) | ( ( long ) hash << 32 ); }
		
		/**
		 * Creates a token from an index and the current version.
		 *
		 * @param index the index of the entry
		 * @return the created token
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index & INDEX_MASK ); }
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token the token
		 * @return the extracted index
		 */
		protected int index( long token ) { return ( int ) ( token & INDEX_MASK ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token the token
		 * @return the extracted version
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		@Override
		public Set< K > keySet() { return keys(); }
		
		/**
		 * Returns a read-only {@link KeyCollection} view of the keys contained in this map.
		 * The collection is backed by the map, so changes to the map will be reflected in the collection.
		 * However, the collection itself does not support modifications.
		 *
		 * @return a set view of the keys contained in this map
		 */
		public KeyCollection keys() {
			return _keys == null ?
					_keys = new KeyCollection( this ) :
					_keys;
		}
		
		@Override
		public Collection< V > values() {
			return _values == null ?
					_values = new ValueCollection( this ) :
					_values;
		}
		
		@Override
		public Set< Entry< K, V > > entrySet() {
			return _entrySet == null ?
					_entrySet = new EntrySet( this ) :
					_entrySet;
		}
		
		/**
		 * Returns a read-only {@link Iterator} over the entries in this map.
		 *
		 * @return an iterator over the entries of this map
		 */
		public Iterator iterator() { return new Iterator( this ); }
		
		// Inner classes
		
		/**
		 * {@code KeyCollection} is a read-only set view of the keys contained in the {@code ObjectObjectMap.R}.
		 * It implements the {@link Set} interface and provides read-only access to the map's keys.
		 */
		public class KeyCollection extends AbstractSet< K > implements Set< K > {
			private final R< K, V > _map;
			
			/**
			 * Constructs a {@code KeyCollection} associated with the given read-only map.
			 *
			 * @param map the read-only map
			 */
			public KeyCollection( R< K, V > map ) { _map = map; }
			
			@Override
			public int size() { return _map.size(); }
			
			@Override
			public boolean contains( Object item ) { return _map.containsKey( item ); }
			
			@Override
			public java.util.Iterator< K > iterator() { return new KeyIterator( _map ); }
			
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			@Override
			public boolean add( K item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Copies the keys from this collection to the specified array, starting at the specified index.
			 *
			 * @param array the array to copy the keys into
			 * @param index the starting index in the array
			 * @throws IndexOutOfBoundsException if the index is out of range
			 * @throws IllegalArgumentException  if the array is too small to contain all the keys
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
		 * {@code KeyIterator} is a read-only iterator for the keys of a {@code ObjectObjectMap.R}.
		 * It allows iteration over the keys in the map.
		 */
		public class KeyIterator implements java.util.Iterator< K > {
			private final R< K, V > _map;
			private final int       _version;
			protected     long      _currentToken;
			private       K         _currentKey;
			
			/**
			 * Constructs a {@code KeyIterator} for the keys of the given read-only map.
			 *
			 * @param map the read-only map to iterate over
			 */
			KeyIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			@Override
			public boolean hasNext() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				_currentToken = _currentToken == INVALID_TOKEN ?
						_map.token() :
						_map.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			@Override
			public K next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentKey = _map.key( _currentToken );
				return _currentKey;
			}
			
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * {@code ValueCollection} is a read-only collection view of the values contained in the {@code ObjectObjectMap.R}.
		 * It implements the {@link Collection} interface and provides read-only access to the map's values.
		 */
		public class ValueCollection extends AbstractCollection< V > implements Collection< V > {
			private final R< K, V > _map;
			
			/**
			 * Constructs a {@code ValueCollection} associated with the given read-only map.
			 *
			 * @param map the read-only map
			 */
			public ValueCollection( R< K, V > map ) { _map = map; }
			
			@Override
			public int size() { return _map.size(); }
			
			@Override
			public boolean contains( Object item ) { return _map.containsValue( item ); }
			
			@Override
			public java.util.Iterator< V > iterator() { return new ValueIterator( _map ); }
			
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			@Override
			public boolean add( V item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			@Override
			public boolean remove( Object item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Copies the values from this collection to the specified array, starting at the specified index.
			 *
			 * @param array the array to copy the values into
			 * @param index the starting index in the array
			 * @throws NullPointerException      if the array is null
			 * @throws IndexOutOfBoundsException if the index is out of range
			 * @throws IllegalArgumentException  if the array is too small to contain all the values
			 */
			public void CopyTo( V[] array, int index ) {
				if( array == null ) throw new NullPointerException( "array is null" );
				if( index < 0 || index > array.length ) throw new IndexOutOfBoundsException( "Index out of range" );
				if( array.length - index < _map.count() ) throw new IllegalArgumentException( "Arg_ArrayPlusOffTooSmall" );
				int    count   = _map._count;
				long[] entries = _map.hash_nexts;
				for( int i = 0; i < count; i++ )
					if( next( entries[ i ] ) >= -1 )
						array[ index++ ] = _map.values[ i ];
			}
		}
		
		/**
		 * {@code ValueIterator} is a read-only iterator for the values of a {@code ObjectObjectMap.R}.
		 * It allows iteration over the values in the map.
		 */
		public class ValueIterator implements java.util.Iterator< V > {
			private final R< K, V > _map;
			private final int       _version;
			private       long      _currentToken;
			private       V         _currentValue;
			
			/**
			 * Constructs a {@code ValueIterator} for the values of the given read-only map.
			 *
			 * @param map the read-only map to iterate over
			 */
			ValueIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			@Override
			public boolean hasNext() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				_currentToken = _currentToken == INVALID_TOKEN ?
						_map.token() :
						_map.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			@Override
			public V next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentValue = _map.value( _currentToken );
				return _currentValue;
			}
			
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * {@code EntrySet} is a read-only set view of the entries contained in the {@code ObjectObjectMap.R}.
		 * It implements the {@link Set} interface and provides read-only access to the map's entries.
		 */
		public class EntrySet extends AbstractSet< Entry< K, V > > implements Set< Entry< K, V > > {
			private final R< K, V > _map;
			
			/**
			 * Constructs an {@code EntrySet} associated with the given read-only map.
			 *
			 * @param map the read-only map
			 */
			public EntrySet( R< K, V > map ) { _map = map; }
			
			@Override
			public int size() { return _map.size(); }
			
			@Override public boolean contains( Object o ) { return _map.containsKey( o ); }
			
			@Override
			public java.util.Iterator< Entry< K, V > > iterator() { return new EntryIterator( _map ); }
			
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			@Override
			public boolean add( Entry< K, V > entry ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			@Override
			public boolean remove( Object o ) { throw new UnsupportedOperationException( "Read-only collection" ); }
		}
		
		/**
		 * {@code EntryIterator} is a read-only iterator for the entries of a {@code ObjectObjectMap.R}.
		 * It allows iteration over the key-value entries in the map.
		 */
		public class EntryIterator implements java.util.Iterator< Entry< K, V > > {
			private final R< K, V >     _map;
			private final int           _version;
			protected     long          _currentToken;
			private       Entry< K, V > _currentEntry;
			
			/**
			 * Constructs an {@code EntryIterator} for the entries of the given read-only map.
			 *
			 * @param map the read-only map to iterate over
			 */
			EntryIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			@Override
			public boolean hasNext() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				_currentToken = _currentToken == INVALID_TOKEN ?
						_map.token() :
						_map.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			@Override
			public Entry< K, V > next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentEntry = new AbstractMap.SimpleEntry<>( _map.key( _currentToken ), _map.value( _currentToken ) );
				return _currentEntry;
			}
			
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * {@code Iterator} is a read-only iterator for the entries of a {@code ObjectObjectMap.R}.
		 * It's similar to {@link EntryIterator} but named more generically as 'Iterator'.
		 */
		public class Iterator implements java.util.Iterator< Entry< K, V > > {
			private final R< K, V >     _map;
			private final int           _version;
			protected     long          _currentToken;
			private       Entry< K, V > _current;
			
			/**
			 * Constructs an {@code Iterator} for the entries of the given read-only map.
			 *
			 * @param map the read-only map to iterate over
			 */
			Iterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			@Override
			public boolean hasNext() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				_currentToken = _currentToken == INVALID_TOKEN ?
						_map.token() :
						_map.token( _currentToken );
				return _currentToken != INVALID_TOKEN;
			}
			
			@Override
			public Entry< K, V > next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_current = new AbstractMap.SimpleEntry<>( _map.key( _currentToken ), _map.value( _currentToken ) );
				return _current;
			}
			
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		// Unsupported write operations
		@Override public V put( K key, V value ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		@Override public V remove( Object key )                           { throw new UnsupportedOperationException( "Read-only map" ); }
		
		@Override public void putAll( Map< ? extends K, ? extends V > m ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		@Override public void clear()                                     { throw new UnsupportedOperationException( "Read-only map" ); }
		
	}
	
	/**
	 * {@code RW} is a read-write implementation of {@code ObjectObjectMap}, extending the read-only base class {@link R}.
	 * It provides full map functionalities, including adding, removing, and modifying key-value pairs.
	 *
	 * @param <K> The type of keys in the map.
	 * @param <V> The type of values in the map.
	 */
	class RW< K, V > extends R< K, V > {
		/**
		 * Threshold for hash collision count, used to potentially trigger rehashing for performance optimization.
		 */
		private static final int                                                        HashCollisionThreshold = 100;
		/**
		 * Function to force new hash codes for keys, used in collision resolution strategies. Can be set externally.
		 */
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} with default capacity.
		 * Uses default equality and hash strategies for both keys and values based on their classes.
		 *
		 * @param clazzK The class of keys.
		 * @param clazzV The class of values.
		 */
		public RW( Class< K > clazzK, Class< V > clazzV ) { this( clazzK, clazzV, 0 ); }
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} with a specified initial capacity.
		 * Uses default equality and hash strategies for both keys and values based on their classes.
		 *
		 * @param clazzK   The class of keys.
		 * @param clazzV   The class of values.
		 * @param capacity The initial capacity of the map.
		 */
		public RW( Class< K > clazzK, Class< V > clazzV, int capacity ) { this( Array.get( clazzK ), Array.get( clazzV ), capacity ); }
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} with default capacity.
		 * Uses a default equality and hash strategy for keys and a custom strategy for values.
		 *
		 * @param clazzK       The class of keys.
		 * @param equal_hash_V Custom equality and hash strategy for values.
		 */
		public RW( Class< K > clazzK, Array.EqualHashOf< V > equal_hash_V ) { this( Array.get( clazzK ), equal_hash_V, 0 ); }
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} with a specified initial capacity.
		 * Uses a default equality and hash strategy for keys and a custom strategy for values.
		 *
		 * @param clazzK       The class of keys.
		 * @param equal_hash_V Custom equality and hash strategy for values.
		 * @param capacity     The initial capacity of the map.
		 */
		public RW( Class< K > clazzK, Array.EqualHashOf< V > equal_hash_V, int capacity ) { this( Array.get( clazzK ), equal_hash_V, capacity ); }
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} with default capacity.
		 * Uses custom equality and hash strategies for both keys and values.
		 *
		 * @param equal_hash_K Custom equality and hash strategy for keys.
		 * @param equal_hash_V Custom equality and hash strategy for values.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V ) { this( equal_hash_K, equal_hash_V, 0 ); }
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} with a specified initial capacity.
		 * Uses custom equality and hash strategies for both keys and values.
		 *
		 * @param equal_hash_K Custom equality and hash strategy for keys.
		 * @param equal_hash_V Custom equality and hash strategy for values.
		 * @param capacity     The initial capacity of the map.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V, int capacity ) {
			super( equal_hash_V );
			this.equal_hash_K = equal_hash_K;
			if( capacity > 0 ) initialize( capacity );
		}
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} initialized with the mappings from the specified map.
		 * Uses custom equality and hash strategies for both keys and values.
		 *
		 * @param equal_hash_K Custom equality and hash strategy for keys.
		 * @param equal_hash_V Custom equality and hash strategy for values.
		 * @param map          The map whose mappings are to be placed in this map.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V, java.util.Map< ? extends K, ? extends V > map ) {
			super( equal_hash_V );
			this.equal_hash_K = equal_hash_K;
			putAll( map );
		}
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} initialized with the mappings from the specified collection of entries.
		 * Uses custom equality and hash strategies for both keys and values.
		 *
		 * @param equal_hash_K Custom equality and hash strategy for keys.
		 * @param equal_hash_V Custom equality and hash strategy for values.
		 * @param collection   The collection of entries to initialize the map with.
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
		
		@Override
		public V put( K key, V value ) {
			long token = tokenOf( key );
			V ret = token == INVALID_TOKEN ?
					null :
					value( token );
			tryInsert( key, value, 1 );
			return ret;
		}
		
		/**
		 * Associates the specified key with the specified value in this map if the key is not already present.
		 *
		 * @param key   key with which the specified value is to be associated
		 * @param value value to be associated with the specified key
		 * @return {@code true} if a new key-value mapping was added, {@code false} if the key was already present
		 */
		public boolean putNotExist( K key, V value ) { return tryInsert( key, value, 2 ); }
		
		/**
		 * Associates the specified key with the specified value in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * If insertion fails (e.g., due to duplicate key exception when behavior flag is set), it throws {@link IllegalArgumentException}.
		 *
		 * @param key   key with which the specified value is to be associated
		 * @param value value to be associated with the specified key
		 * @throws IllegalArgumentException if an item with the same key already exists and behavior is set to throw exception.
		 */
		public void putTry( K key, V value ) { tryInsert( key, value, 0 ); }
		
		@Override
		public void putAll( Map< ? extends K, ? extends V > m ) {
			for( Entry< ? extends K, ? extends V > entry : m.entrySet() ) put( entry.getKey(), entry.getValue() );
		}
		
		@Override
		public void clear() {
			if( _count == 0 && !hasNullKey ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( hash_nexts, 0, _count, 0L );
			Arrays.fill( keys, 0, _count, null );
			Arrays.fill( values, 0, _count, null );
			_count       = 0;
			_freeList    = -1;
			_freeCount   = 0;
			hasNullKey   = false;
			nullKeyValue = null;
			_version++;
		}
		
		@Override
		@SuppressWarnings( "unchecked" )
		public V remove( Object key ) { return remove_( ( K ) key ); }
		
		/**
		 * Removes the mapping for a key from this map if it is present.
		 *
		 * @param key key whose mapping is to be removed from the map
		 * @return the previous value associated with the key, or {@code null} if there was no mapping for the key
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
					V oldValue = values[ i ];
					values[ i ] = null;
					_freeList   = i;
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
		 * Ensures that the capacity of this map is at least the specified minimum capacity.
		 * If the current capacity is less than the minimum capacity, it increases the capacity to the next prime number greater than or equal to {@code capacity}.
		 *
		 * @param capacity the desired minimum capacity
		 * @return the new capacity of the map after ensuring capacity
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
		 * Reduces the capacity of this map to be the minimum size that can hold the current number of entries.
		 * This method trims the capacity to {@link #count()}.
		 */
		public void trim() { trim( count() ); }
		
		/**
		 * Reduces the capacity of this map to be the minimum size that can hold the specified capacity.
		 *
		 * @param capacity the desired capacity to trim to
		 * @throws IllegalArgumentException if the capacity is less than the current number of entries
		 */
		public void trim( int capacity ) {
			if( capacity < count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			int new_size        = Array.prime( capacity );
			if( currentCapacity <= new_size ) return;
			
			long[] old_hash_next = hash_nexts;
			K[]    old_keys      = keys;
			V[]    old_values    = values;
			int    old_count     = _count;
			_version++;
			initialize( new_size );
			copy( old_hash_next, old_keys, old_values, old_count );
		}
		
		/**
		 * Tries to insert a key-value pair into the map.
		 * Handles null keys, hash collisions, and resizing.
		 *
		 * @param key      The key to insert.
		 * @param value    The value to associate with the key.
		 * @param behavior Flag to control insertion behavior.
		 *                 0: Throw exception if key exists.
		 *                 1: Update value if key exists, insert if not.
		 *                 2: Insert only if key does not exist.
		 * @return {@code true} if insertion occurred (or value updated), {@code false} if insertion was prevented by behavior flag.
		 * @throws IllegalArgumentException        if behavior is 0 and key already exists.
		 * @throws ConcurrentModificationException if concurrent operations are detected.
		 */
		private boolean tryInsert( K key, V value, int behavior ) {
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
				nullKeyValue = value;
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
							values[ next ] = value;
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
			
			hash_nexts[ index ]     = hash_next( hash, bucket );
			keys[ index ]           = key;
			values[ index ]         = value;
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			if( HashCollisionThreshold < collisionCount && this.forceNewHashCodes != null && key instanceof String ) resize( hash_nexts.length, true );
			return true;
		}
		
		/**
		 * Resizes the internal hash table to a new size.
		 * Optionally forces recalculation of hash codes for all keys if {@code forceNewHashCodes} is true.
		 *
		 * @param new_size          The new size of the hash table.
		 * @param forceNewHashCodes If {@code true}, recalculates hash codes for all keys.
		 */
		private void resize( int new_size, boolean forceNewHashCodes ) {
			_version++;
			long[]    new_hash_next = Arrays.copyOf( hash_nexts, new_size );
			K[]       new_keys      = Arrays.copyOf( keys, new_size );
			V[]       new_values    = Arrays.copyOf( values, new_size );
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
			values     = new_values;
		}
		
		/**
		 * Initializes the internal hash table with a given capacity.
		 *
		 * @param capacity The initial capacity of the hash table.
		 * @return The prime number capacity that is actually used.
		 */
		private int initialize( int capacity ) {
			capacity   = Array.prime( capacity );
			_buckets   = new int[ capacity ];
			hash_nexts = new long[ capacity ];
			keys       = equal_hash_K.copyOf( null, capacity );
			values     = equal_hash_V.copyOf( null, capacity );
			_freeList  = -1;
			return capacity;
		}
		
		/**
		 * Copies entries from old arrays to the newly resized arrays during resizing or trimming.
		 *
		 * @param old_hash_next The old hash_nexts array.
		 * @param old_keys      The old keys array.
		 * @param old_values    The old values array.
		 * @param old_count     The number of elements in the old arrays.
		 */
		private void copy( long[] old_hash_next, K[] old_keys, V[] old_values, int old_count ) {
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
		
		/**
		 * Copies the entries from this map to the specified array of {@link Entry} objects, starting at the specified index.
		 *
		 * @param array the array to copy the entries into
		 * @param index the starting index in the array
		 * @throws IndexOutOfBoundsException if the index is out of range
		 * @throws IllegalArgumentException  if the array is too small to contain all the entries
		 */
		private void copyTo( Entry< K, V >[] array, int index ) {
			if( index < 0 || index > array.length ) throw new IndexOutOfBoundsException( "Index out of range" );
			if( array.length - index < count() ) throw new IllegalArgumentException( "Arg_ArrayPlusOffTooSmall" );
			int    count   = _count;
			long[] entries = hash_nexts;
			for( int i = 0; i < count; i++ )
				if( next( entries[ i ] ) >= -1 )
					array[ index++ ] = new AbstractMap.SimpleEntry<>( keys[ i ], values[ i ] );
		}
		
		// Override inner classes to support mutation
		
		/**
		 * {@code KeyCollection} for {@code ObjectObjectMap.RW} extends the read-only version to support write operations.
		 * It allows adding and removing keys, and clearing the collection, which modifies the underlying map.
		 */
		public final class KeyCollection extends R< K, V >.KeyCollection {
			RW< K, V > _map;
			
			/**
			 * Constructs a {@code KeyCollection} associated with the given read-write map.
			 *
			 * @param map the read-write map
			 */
			public KeyCollection( RW< K, V > map ) { super( map ); _map = map; }
			
			@Override
			public boolean add( K item ) { return _map.put( item, null ) != null; }
			
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) { return _map.remove( o ) != null; }
			
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code KeyIterator} for {@code ObjectObjectMap.RW} extends the read-only version to support removal of keys during iteration.
		 * It allows removing the current key during iteration from the underlying map.
		 */
		public class KeyIterator extends R< K, V >.KeyIterator {
			RW< K, V > _map;
			
			/**
			 * Constructs a {@code KeyIterator} for the keys of the given read-write map.
			 *
			 * @param map the read-write map to iterate over
			 */
			KeyIterator( RW< K, V > map ) { super( map ); _map = map; }
			
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove( _map.key( _currentToken ) );
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * {@code ValueCollection} for {@code ObjectObjectMap.RW} extends the read-only version to support write operations.
		 * It allows adding and removing values and clearing the collection, which modifies the underlying map.
		 * Note: Adding a value adds it with a null key. Removing a value removes the first entry found with that value.
		 */
		public final class ValueCollection extends R< K, V >.ValueCollection {
			RW< K, V > _map;
			
			/**
			 * Constructs a {@code ValueCollection} associated with the given read-write map.
			 *
			 * @param map the read-write map
			 */
			public ValueCollection( RW< K, V > map ) { super( map ); _map = map; }
			
			@Override
			public boolean add( V item ) { return _map.put( null, item ) != null; }
			
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
			
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code ValueIterator} for {@code ObjectObjectMap.RW} extends the read-only version.
		 * Note: remove operation is not supported for ValueIterator as it is ambiguous which key to remove when values are not unique.
		 */
		public class ValueIterator extends R< K, V >.ValueIterator {
			ValueIterator( R< K, V > map ) { super( map ); }
			
			@Override
			public void remove() { throw new UnsupportedOperationException( "remove by value is not supported" ); }
		}
		
		/**
		 * {@code EntrySet} for {@code ObjectObjectMap.RW} extends the read-only version to support write operations.
		 * It allows adding and removing entries and clearing the set, which modifies the underlying map.
		 */
		public final class EntrySet extends R< K, V >.EntrySet {
			RW< K, V > _map;
			
			/**
			 * Constructs an {@code EntrySet} associated with the given read-write map.
			 *
			 * @param map the read-write map
			 */
			public EntrySet( RW< K, V > map ) { super( map ); _map = map; }
			
			@Override
			public boolean add( Entry< K, V > entry ) {
				if( entry == null ) throw new NullPointerException( "Entry is null" );
				return _map.put( entry.getKey(), entry.getValue() ) != null;
			}
			
			@Override
			public boolean remove( Object o ) {
				if( !( o instanceof Entry ) ) return false;
				Entry< ?, ? > entry = ( Entry< ?, ? > ) o;
				return _map.remove( entry.getKey() ) != null;
			}
			
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code EntryIterator} for {@code ObjectObjectMap.RW} extends the read-only version to support removal of entries during iteration.
		 * It allows removing the current entry during iteration from the underlying map.
		 */
		public class EntryIterator extends R< K, V >.EntryIterator {
			RW< K, V > _map;
			
			/**
			 * Constructs an {@code EntryIterator} for the entries of the given read-write map.
			 *
			 * @param map the read-write map to iterate over
			 */
			EntryIterator( RW< K, V > map ) { super( map ); _map = map; }
			
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove( _map.key( _currentToken ) );
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * {@code Iterator} for {@code ObjectObjectMap.RW} extends the read-only version to support removal of entries during iteration.
		 * It's a generic iterator that allows removing the current entry during iteration from the underlying map.
		 */
		public Iterator iterator() { return new Iterator( this ); }
		
		
		/**
		 * {@code Iterator} for {@code ObjectObjectMap.RW} extends the read-only version to support removal of entries during iteration.
		 * It's a generic iterator that allows removing the current entry during iteration from the underlying map.
		 */
		public class Iterator extends R< K, V >.Iterator {
			RW< K, V > _map;
			
			/**
			 * Constructs an {@code Iterator} for the entries of the given read-write map.
			 *
			 * @param map the read-write map to iterate over
			 */
			Iterator( RW< K, V > map ) { super( map ); _map = map; }
			
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove( _map.key( _currentToken ) );
				_currentToken = INVALID_TOKEN;
			}
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	@SuppressWarnings( "unchecked" )
	static < K, V > Array.EqualHashOf< RW< K, V > > equal_hash() { return ( Array.EqualHashOf< RW< K, V > > ) RW.OBJECT; }
}