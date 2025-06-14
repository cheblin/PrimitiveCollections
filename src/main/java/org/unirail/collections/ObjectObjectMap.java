// Copyright 2025 Chikirev Sirguy, Unirail Group
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.*;
import java.util.function.Function;

/**
 * A generic interface for a hash map that stores object-to-object key-value pairs.
 * It provides efficient methods for common map operations and supports token-based access
 * for high-performance iteration and lookups. The interface accommodates both read-only
 * and read-write implementations and supports {@code null} keys.
 */
public interface ObjectObjectMap {
	
	/**
	 * An abstract read-only base class for {@code ObjectObjectMap} implementations.
	 * <p>
	 * This class provides the core, non-modifying functionality for a hash map. It uses a
	 * specialized internal structure with two distinct regions for storing entries to optimize
	 * memory layout and access patterns. It serves as the foundation for both read-only and
	 * read-write map implementations.
	 *
	 * @param <K> The type of keys maintained by this map.
	 * @param <V> The type of mapped values.
	 */
	abstract class R< K, V > implements java.util.Map< K, V >, JsonWriter.Source, Cloneable {
		/**
		 * A flag indicating whether the map contains a mapping for the {@code null} key.
		 */
		protected boolean hasNullKey;
		
		/**
		 * The value associated with the {@code null} key, if {@link #hasNullKey} is true.
		 */
		protected V nullKeyValue;
		
		/**
		 * An array of buckets for the hash table. Each element stores a 1-based index
		 * pointing to the head of a collision chain in the {@link #keys} array. A value of 0
		 * indicates an empty bucket.
		 */
		protected int[] _buckets;
		
		/**
		 * An array storing the cached hash code for each entry, parallel to the {@link #keys} array.
		 */
		protected int[] hash;
		
		/**
		 * An array storing 0-based indices to link entries in a collision chain. This array is used
		 * for entries in the "low" region of the map.
		 */
		protected int[] links= Array.EqualHashOf._ints.O;
		
		/**
		 * The array storing the keys of the map entries.
		 */
		protected K[] keys;
		
		/**
		 * The array storing the values of the map entries, parallel to the {@link #keys} array.
		 */
		protected V[] values;
		
		/**
		 * The number of active entries stored in the "low" region of the backing arrays,
		 * occupying indices from {@code 0} to {@code _lo_Size - 1}.
		 */
		protected int _lo_Size;
		/**
		 * The number of active entries stored in the "high" region of the backing arrays,
		 * occupying indices from {@code keys.length - _hi_Size} to {@code keys.length - 1}.
		 */
		protected int _hi_Size;
		
		/**
		 * A version counter used to detect concurrent modifications. It is incremented
		 * on every structural change to the map, ensuring fail-fast behavior for iterators.
		 */
		protected int _version;
		
		/**
		 * The strategy object used for calculating hash codes and checking equality of keys.
		 */
		protected Array.EqualHashOf< K > equal_hash_K;
		
		/**
		 * The strategy object used for calculating hash codes and checking equality of values.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		/**
		 * A lazily initialized set view of the keys contained in this map.
		 */
		protected KeyCollection _keys;
		
		/**
		 * A lazily initialized collection view of the values contained in this map.
		 */
		protected ValueCollection _values;
		
		/**
		 * A lazily initialized set view of the map entries.
		 */
		protected EntrySet _entrySet;
		
		/**
		 * A special index used within a token to represent the {@code null} key.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * The number of bits to shift to extract the version from a token.
		 */
		protected static final int VERSION_SHIFT = 32;
		
		/**
		 * A constant representing an invalid or non-existent token, typically returned when
		 * a key is not found or an iteration has completed. Its value is {@code -1L}.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Constructs a read-only map with a specified strategy for value equality and hashing.
		 *
		 * @param equal_hash_V The strategy for value equality and hash code calculation.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		/**
		 * Returns the total number of active non-null entries in the map.
		 * This is the sum of entries in both the low and high regions.
		 *
		 * @return The current count of non-null key-value mappings.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * Returns {@code true} if this map contains no key-value mappings.
		 *
		 * @return {@code true} if this map is empty.
		 */
		@Override
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value mappings in this map.
		 *
		 * @return The number of key-value mappings in this map.
		 */
		@Override
		public int size() {
			return _count() + ( hasNullKey ?
			                    1 :
			                    0 );
		}
		
		/**
		 * Returns the number of key-value mappings in this map. Alias for {@link #size()}.
		 *
		 * @return The number of key-value mappings in this map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the total capacity of the internal arrays, which is the maximum number
		 * of entries the map can hold before resizing.
		 *
		 * @return The capacity of the internal data structures.
		 */
		public int length() {
			return keys == null ?
			       0 :
			       keys.length;
		}
		
		/**
		 * Returns {@code true} if this map contains a mapping for the specified key.
		 *
		 * @param key The key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean containsKey( Object key ) { return contains( ( K ) key ); }
		
		/**
		 * Returns {@code true} if this map contains a mapping for the specified key.
		 *
		 * @param key The key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key.
		 */
		public boolean contains( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Returns {@code true} if this map maps one or more keys to the specified value.
		 *
		 * @param value The value whose presence in this map is to be tested.
		 * @return {@code true} if this map contains at least one mapping to the specified value.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean containsValue( Object value ) {
			V v;
			try { v = ( V ) value; } catch( ClassCastException e ) { return false; } // Handle type mismatch gracefully
			
			if( hasNullKey && equal_hash_V.equals( nullKeyValue, v ) ) return true;
			
			if( _count() == 0 ) return false;
			
			// Iterate active entries in the low region
			for( int i = 0; i < _lo_Size; i++ ) if( equal_hash_V.equals( values[ i ], v ) ) return true;
			// Iterate active entries in the high region
			for( int i = keys.length - _hi_Size; i < keys.length; i++ ) if( equal_hash_V.equals( values[ i ], v ) ) return true;
			return false;
		}
		
		/**
		 * Returns {@code true} if this map contains a mapping for the {@code null} key.
		 *
		 * @return {@code true} if a mapping for the {@code null} key exists.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the {@code null} key.
		 *
		 * @return The value mapped to the {@code null} key, or {@code null} if no such mapping exists.
		 */
		public V nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Returns a token for the specified key. A token is a long integer that encodes both
		 * the entry's index and the map's current version, enabling efficient, version-aware
		 * access to the entry.
		 *
		 * @param key The key whose token is to be retrieved.
		 * @return A valid token if the key is found; otherwise, {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if an excessive number of collisions is detected,
		 *                                         suggesting a concurrent modification issue.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
			                         token( NULL_KEY_INDEX ) :
			                         INVALID_TOKEN;
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			
			int hash  = equal_hash_K.hashCode( key );
			int index = _buckets[ bucketIndex( hash ) ] - 1; // 0-based index from bucket
			if( index < 0 ) return INVALID_TOKEN; // Bucket is empty
			
			// Traverse collision chain (entry is in lo Region)
			for( int collisions = 0; ; ) {
				if( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) return token( index );
				if( _lo_Size <= index ) return INVALID_TOKEN; // Reached a terminal node that might be in hi Region (no more links)
				index = links[ index ]; // Directly use links array
				if( _lo_Size + 1 < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
		}
		
		/**
		 * Returns the first token for iterating through the map.
		 * The iteration order is not guaranteed.
		 *
		 * @return A token corresponding to the first entry for iteration, or
		 * {@link #INVALID_TOKEN} if the map is empty.
		 */
		public long token() {
			int index = unsafe_token( -1 ); // Get the first non-null key token
			return index == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       // If no non-null keys, return null key token if present
			       INVALID_TOKEN :
			       token( index ); // Return token for found non-null key
		}
		
		/**
		 * Returns the token for the next entry in an iteration.
		 *
		 * @param token The token of the current entry.
		 * @return The token for the next entry, or {@link #INVALID_TOKEN} if this is the last entry.
		 * @throws IllegalArgumentException        if the provided token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException if the map was structurally modified after the token was obtained.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN; // If current token is null key, no more elements
			
			index = unsafe_token( index ); // Get next unsafe token (non-null key)
			
			return index == -1 ?
			       hasNullKey ?
			       token( NULL_KEY_INDEX ) :
			       // If no more non-null keys, check for null key
			       INVALID_TOKEN :
			       token( index ); // Return token for found non-null key
		}
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 * <p>
		 * Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #key(long)} to handle it separately.
		 * <p>
		 * <strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token (index), or {@code -1} to begin iteration.
		 * @return The next token (index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #key(long) To get the key associated with a token.
		 */
		public int unsafe_token( int token ) {
			if( _buckets == null || _count() == 0 ) return -1;
			
			int i         = token + 1;
			int lowest_hi = keys.length - _hi_Size; // Start of hi region (inclusive)
			
			// Check lo region first
			if( i < _lo_Size ) return i; // Return the current index in lo region
			
			// If we've exhausted lo region or started beyond it
			if( i < lowest_hi ) { // If 'i' is in the gap between lo and hi regions
				// If hi region is empty, no more entries
				if( _hi_Size == 0 ) return -1;
				return lowest_hi; // Return the start of hi region
			}
			
			// If 'i' is already in or past the start of the hi region
			// Iterate through hi region
			if( i < keys.length ) return i;
			
			return -1; // No more entries
		}
		
		/**
		 * Checks if the key associated with the given token is {@code null}.
		 *
		 * @param token The token of the entry to check.
		 * @return {@code true} if the token corresponds to the {@code null} key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the key corresponding to the given token.
		 *
		 * @param token The token of the entry.
		 * @return The key for the entry. Returns {@code null} if the token represents the null key mapping.
		 * @throws ConcurrentModificationException if the map was structurally modified after the token was obtained.
		 */
		public K key( long token ) {
			return isKeyNull( token ) ?
			       null :
			       keys[ index( token ) ];
		}
		
		/**
		 * Returns the value corresponding to the given token.
		 *
		 * @param token The token of the entry.
		 * @return The value for the entry.
		 * @throws ConcurrentModificationException if the map was structurally modified after the token was obtained.
		 */
		public V value( long token ) {
			return isKeyNull( token ) ?
			       // Use isKeyNull for clarity
			       nullKeyValue :
			       values[ index( token ) ];
		}
		
		/**
		 * Returns the value to which the specified key is mapped, or {@code null} if this map
		 * contains no mapping for the key.
		 *
		 * @param key The key whose associated value is to be returned.
		 * @return The value to which the specified key is mapped, or {@code null} if no mapping exists.
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
		 * Returns the value to which the specified key is mapped, or {@code defaultValue} if
		 * this map contains no mapping for the key.
		 *
		 * @param key          The key whose associated value is to be returned.
		 * @param defaultValue The default value to return if no mapping for the key is found.
		 * @return The value for the key, or {@code defaultValue} if no mapping exists.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public V getOrDefault( Object key, V defaultValue ) { return get( ( K ) key, defaultValue ); }
		
		/**
		 * Returns the value to which the specified key is mapped, or {@code defaultValue} if
		 * this map contains no mapping for the key.
		 *
		 * @param key          The key whose associated value is to be returned.
		 * @param defaultValue The default value to return if no mapping for the key is found.
		 * @return The value for the key, or {@code defaultValue} if no mapping exists.
		 */
		public V get( K key, V defaultValue ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
			       defaultValue :
			       value( token );
		}
		
		/**
		 * Computes the hash code for this map. The hash code is derived from the hash codes
		 * of its key-value pairs.
		 *
		 * @return The hash code value for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( key( token( token ) ) ) ); // Pass token(token)
				h = Array.mix( h, value( token( token ) ) == null ?
				                  // Pass token(token)
				                  seed :
				                  equal_hash_V.hashCode( value( token( token ) ) ) ); // Pass token(token)
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, nullKeyValue != null ?
				                  equal_hash_V.hashCode( nullKeyValue ) :
				                  seed );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares the specified object with this map for equality.
		 *
		 * @param obj The object to be compared for equality with this map.
		 * @return {@code true} if the specified object is equal to this map.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< K, V > ) obj );
		}
		
		/**
		 * Compares this map with another {@code ObjectObjectMap.R} for equality.
		 * Two maps are considered equal if they contain the same key-value mappings.
		 *
		 * @param other The other map to compare with.
		 * @return {@code true} if the two maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R< K, V > other ) {
			if( other == this ) return true;
			if( other == null ||
			    hasNullKey != other.hasNullKey ||
			    ( hasNullKey && !equal_hash_V.equals( nullKeyValue, other.nullKeyValue ) ) ||
			    size() != other.size() ) return false;
			
			long t;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				t = other.tokenOf( key( token( token ) ) ); // Pass token(token)
				if( t == INVALID_TOKEN ||
				    !equal_hash_V.equals( value( token( token ) ), other.value( t ) ) ) // Pass token(token)
					return false;
			}
			return true;
		}
		
		/**
		 * Creates and returns a shallow copy of this map. The keys and values themselves are not cloned.
		 *
		 * @return A clone of this map instance.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public R< K, V > clone() {
			try {
				R< K, V > dst = ( R< K, V > ) super.clone();
				if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.hash     = hash.clone(); // Cloned hash
					dst.links    = links.clone(); // Cloned links
					dst.keys     = keys.clone();
					dst.values   = values.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				throw new InternalError( e ); // Should not happen as R implements Cloneable
			}
		}
		
		/**
		 * Returns a JSON string representation of this map.
		 *
		 * @return A JSON formatted string representing the map's contents.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Serializes the map's contents into JSON format using the provided {@link JsonWriter}.
		 * If keys are strings, it generates a JSON object. Otherwise, it generates a JSON array of key-value objects.
		 *
		 * @param json The {@link JsonWriter} to write the JSON output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			
			if( equal_hash_K == Array.string ) {
				json.enterObject();
				
				if( hasNullKey ) json.name().value( nullKeyValue );
				
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( key( token( token ) ) == null ?
				                // Pass token(token)
				                null :
				                key( token( token ) ).toString() ) // Pass token(token)
				         .value( value( token( token ) ) ); // Pass token(token)
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
				         .name( "Key" ).value( key( token( token ) ) ) // Pass token(token)
				         .name( "Value" ).value( value( token( token ) ) ) // Pass token(token)
				         .exitObject();
				json.exitArray();
			}
		}
		
		/**
		 * Calculates the bucket index for a given key hash code.
		 *
		 * @param hash The hash code of the key.
		 * @return The corresponding bucket index in the {@link #_buckets} array.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an entry index and the current map version.
		 *
		 * @param index The 0-based index of the entry.
		 * @return A long integer token combining the index and version.
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); }
		
		/**
		 * Extracts the 0-based entry index from a token.
		 *
		 * @param token The token from which to extract the index.
		 * @return The entry index.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts the map version from a token.
		 *
		 * @param token The token from which to extract the version.
		 * @return The map version encoded in the token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		/**
		 * Returns a {@link Set} view of the keys contained in this map.
		 *
		 * @return A set view of the keys in this map.
		 */
		@Override
		public Set< K > keySet() { return keys(); }
		
		/**
		 * Returns a read-only {@link KeyCollection} view of the keys contained in this map.
		 * The collection is backed by the map, so changes to the map are reflected in the collection.
		 * The collection does not support modification operations.
		 *
		 * @return A read-only set view of the keys contained in this map.
		 */
		public KeyCollection keys() {
			return _keys == null ?
			       ( _keys = new KeyCollection( this ) ) :
			       _keys;
		}
		
		/**
		 * Returns a {@link Collection} view of the values contained in this map.
		 *
		 * @return A collection view of the values in this map.
		 */
		@Override
		public Collection< V > values() {
			return _values == null ?
			       ( _values = new ValueCollection( this ) ) :
			       _values;
		}
		
		/**
		 * Returns a {@link Set} view of the mappings contained in this map.
		 *
		 * @return A set view of the entries in this map.
		 */
		@Override
		public Set< Entry< K, V > > entrySet() {
			return _entrySet == null ?
			       _entrySet = new EntrySet( this ) :
			       _entrySet;
		}
		
		/**
		 * Returns a read-only iterator over the entries in this map.
		 *
		 * @return An iterator over the map's entries.
		 */
		public Iterator iterator() { return new Iterator( this ); }
		
		// Inner classes
		
		/**
		 * A read-only set view of the keys contained in a {@code ObjectObjectMap.R}.
		 * This class implements the {@link Set} interface but throws
		 * {@link UnsupportedOperationException} for all modification methods.
		 */
		public class KeyCollection extends AbstractSet< K > implements Set< K > {
			private final R< K, V > _map;
			
			/**
			 * Constructs a new key collection backed by the specified read-only map.
			 *
			 * @param map The backing map.
			 */
			public KeyCollection( R< K, V > map ) { _map = map; }
			
			/**
			 * Returns the number of keys in this collection, which is the size of the backing map.
			 *
			 * @return The number of keys.
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * Checks if the specified object is a key in the backing map.
			 *
			 * @param item The object to check for containment.
			 * @return {@code true} if the key is in the map.
			 */
			@Override
			public boolean contains( Object item ) { return _map.containsKey( item ); }
			
			/**
			 * Returns a read-only iterator over the keys in this collection.
			 *
			 * @return A {@link KeyIterator}.
			 */
			@Override
			public java.util.Iterator< K > iterator() { return new KeyIterator( _map ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public boolean add( K item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Copies the keys from this collection into a destination array.
			 *
			 * @param array The destination array.
			 * @param index The starting position in the destination array.
			 * @throws IndexOutOfBoundsException if the index is out of bounds.
			 * @throws IllegalArgumentException  if the destination array is too small.
			 */
			public void CopyTo( K[] array, int index ) {
				if( index < 0 || index > array.length ) throw new IndexOutOfBoundsException( "Index out of range" );
				if( array.length - index < _map._count() ) throw new IllegalArgumentException( "Arg_ArrayPlusOffTooSmall" );
				
				// Iterate active entries in the low region
				for( int i = 0; i < _map._lo_Size; i++ ) array[ index++ ] = _map.keys[ i ];
				// Iterate active entries in the high region
				for( int i = _map.keys.length - _map._hi_Size; i < _map.keys.length; i++ ) array[ index++ ] = _map.keys[ i ];
			}
		}
		
		/**
		 * A read-only iterator over the keys of a {@code ObjectObjectMap.R}.
		 * This iterator is fail-fast and will throw a {@link ConcurrentModificationException}
		 * if the map is structurally modified during iteration.
		 */
		public class KeyIterator implements java.util.Iterator< K > {
			private final R< K, V > _map;
			private final int       _version;
			protected     long      _currentToken;
			private       K         _currentKey;
			
			/**
			 * Constructs a key iterator for the specified read-only map.
			 *
			 * @param map The map whose keys are to be iterated.
			 */
			KeyIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * Returns {@code true} if the iteration has more keys.
			 *
			 * @return {@code true} if there is a next key.
			 * @throws ConcurrentModificationException if the map has been modified.
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
			 * Returns the next key in the iteration.
			 *
			 * @return The next key.
			 * @throws ConcurrentModificationException if the map has been modified.
			 * @throws NoSuchElementException          if the iteration has no more keys.
			 */
			@Override
			public K next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentKey = _map.key( _currentToken );
				return _currentKey;
			}
			
			/**
			 * This operation is not supported by this read-only iterator.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * A read-only collection view of the values contained in a {@code ObjectObjectMap.R}.
		 * This class implements the {@link Collection} interface but throws
		 * {@link UnsupportedOperationException} for all modification methods.
		 */
		public class ValueCollection extends AbstractCollection< V > implements Collection< V > {
			private final R< K, V > _map;
			
			/**
			 * Constructs a new value collection backed by the specified read-only map.
			 *
			 * @param map The backing map.
			 */
			public ValueCollection( R< K, V > map ) { _map = map; }
			
			/**
			 * Returns the number of values in this collection, which is the size of the backing map.
			 *
			 * @return The number of values.
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * Checks if the specified object is a value in the backing map.
			 *
			 * @param item The object to check for containment.
			 * @return {@code true} if the value is in the map.
			 */
			@Override
			public boolean contains( Object item ) { return _map.containsValue( item ); }
			
			/**
			 * Returns a read-only iterator over the values in this collection.
			 *
			 * @return A {@link ValueIterator}.
			 */
			@Override
			public java.util.Iterator< V > iterator() { return new ValueIterator( _map ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public boolean add( V item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public boolean remove( Object item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Copies the values from this collection into a destination array.
			 *
			 * @param array The destination array.
			 * @param index The starting position in the destination array.
			 * @throws NullPointerException      if the destination array is null.
			 * @throws IndexOutOfBoundsException if the index is out of bounds.
			 * @throws IllegalArgumentException  if the destination array is too small.
			 */
			public void CopyTo( V[] array, int index ) {
				if( array == null ) throw new NullPointerException( "array is null" );
				if( index < 0 || index > array.length ) throw new IndexOutOfBoundsException( "Index out of range" );
				if( array.length - index < _map._count() ) throw new IllegalArgumentException( "Arg_ArrayPlusOffTooSmall" );
				
				// Iterate active entries in the low region
				for( int i = 0; i < _map._lo_Size; i++ ) array[ index++ ] = _map.values[ i ];
				// Iterate active entries in the high region
				for( int i = _map.keys.length - _map._hi_Size; i < _map.keys.length; i++ ) array[ index++ ] = _map.values[ i ];
			}
		}
		
		/**
		 * A read-only iterator over the values of a {@code ObjectObjectMap.R}.
		 * This iterator is fail-fast and will throw a {@link ConcurrentModificationException}
		 * if the map is structurally modified during iteration.
		 */
		public class ValueIterator implements java.util.Iterator< V > {
			private final R< K, V > _map;
			private final int       _version;
			private       long      _currentToken;
			private       V         _currentValue;
			
			/**
			 * Constructs a value iterator for the specified read-only map.
			 *
			 * @param map The map whose values are to be iterated.
			 */
			ValueIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * Returns {@code true} if the iteration has more values.
			 *
			 * @return {@code true} if there is a next value.
			 * @throws ConcurrentModificationException if the map has been modified.
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
			 * Returns the next value in the iteration.
			 *
			 * @return The next value.
			 * @throws ConcurrentModificationException if the map has been modified.
			 * @throws NoSuchElementException          if the iteration has no more values.
			 */
			@Override
			public V next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentValue = _map.value( _currentToken );
				return _currentValue;
			}
			
			/**
			 * This operation is not supported by this read-only iterator.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * A read-only set view of the entries contained in a {@code ObjectObjectMap.R}.
		 * This class implements the {@link Set} interface but throws
		 * {@link UnsupportedOperationException} for all modification methods.
		 */
		public class EntrySet extends AbstractSet< Entry< K, V > > implements Set< Entry< K, V > > {
			private final R< K, V > _map;
			
			/**
			 * Constructs a new entry set backed by the specified read-only map.
			 *
			 * @param map The backing map.
			 */
			public EntrySet( R< K, V > map ) { _map = map; }
			
			/**
			 * Returns the number of entries in this set, which is the size of the backing map.
			 *
			 * @return The number of entries.
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * Checks if the specified object, which must be a {@link Map.Entry}, is present in the backing map.
			 *
			 * @param o The entry to check for containment.
			 * @return {@code true} if the entry is in the map.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean contains( Object o ) {
				if( !( o instanceof Map.Entry ) ) return false;
				Map.Entry< ?, ? > entry = ( Map.Entry< ?, ? > ) o;
				long              token = _map.tokenOf( ( K ) entry.getKey() );
				return token != INVALID_TOKEN && _map.equal_hash_V.equals( _map.value( token ), ( V ) entry.getValue() );
			}
			
			/**
			 * Returns a read-only iterator over the entries in this set.
			 *
			 * @return An {@link EntryIterator}.
			 */
			@Override
			public java.util.Iterator< Entry< K, V > > iterator() { return new EntryIterator( _map ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public boolean add( Entry< K, V > entry ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * This operation is not supported by this read-only collection.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public boolean remove( Object o ) { throw new UnsupportedOperationException( "Read-only collection" ); }
		}
		
		/**
		 * A read-only iterator over the entries of a {@code ObjectObjectMap.R}.
		 * This iterator is fail-fast and will throw a {@link ConcurrentModificationException}
		 * if the map is structurally modified during iteration.
		 */
		public class EntryIterator implements java.util.Iterator< Entry< K, V > > {
			private final R< K, V >     _map;
			private final int           _version;
			protected     long          _currentToken;
			private       Entry< K, V > _currentEntry;
			
			/**
			 * Constructs an entry iterator for the specified read-only map.
			 *
			 * @param map The map whose entries are to be iterated.
			 */
			EntryIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * Returns {@code true} if the iteration has more entries.
			 *
			 * @return {@code true} if there is a next entry.
			 * @throws ConcurrentModificationException if the map has been modified.
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
			 * Returns the next entry in the iteration.
			 *
			 * @return The next entry.
			 * @throws ConcurrentModificationException if the map has been modified.
			 * @throws NoSuchElementException          if the iteration has no more entries.
			 */
			@Override
			public Entry< K, V > next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentEntry = new AbstractMap.SimpleEntry<>( _map.key( _currentToken ), _map.value( _currentToken ) );
				return _currentEntry;
			}
			
			/**
			 * This operation is not supported by this read-only iterator.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * A read-only iterator over the entries of a {@code ObjectObjectMap.R}.
		 * This iterator is fail-fast and will throw a {@link ConcurrentModificationException}
		 * if the map is structurally modified during iteration.
		 */
		public class Iterator implements java.util.Iterator< Entry< K, V > > {
			private final R< K, V >     _map;
			private final int           _version;
			protected     long          _currentToken;
			private       Entry< K, V > _current;
			
			/**
			 * Constructs an iterator for the specified read-only map.
			 *
			 * @param map The map whose entries are to be iterated.
			 */
			Iterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * Returns {@code true} if the iteration has more entries.
			 *
			 * @return {@code true} if there is a next entry.
			 * @throws ConcurrentModificationException if the map has been modified.
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
			 * Returns the next entry in the iteration.
			 *
			 * @return The next entry.
			 * @throws ConcurrentModificationException if the map has been modified.
			 * @throws NoSuchElementException          if the iteration has no more entries.
			 */
			@Override
			public Entry< K, V > next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_current = new AbstractMap.SimpleEntry<>( _map.key( _currentToken ), _map.value( _currentToken ) );
				return _current;
			}
			
			/**
			 * This operation is not supported by this read-only iterator.
			 *
			 * @throws UnsupportedOperationException Always.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		// Unsupported write operations
		
		/**
		 * This operation is not supported by the read-only map.
		 *
		 * @throws UnsupportedOperationException Always.
		 */
		@Override
		public V put( K key, V value ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * This operation is not supported by the read-only map.
		 *
		 * @throws UnsupportedOperationException Always.
		 */
		@Override
		public V remove( Object key ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * This operation is not supported by the read-only map.
		 *
		 * @throws UnsupportedOperationException Always.
		 */
		@Override
		public void putAll( Map< ? extends K, ? extends V > m ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * This operation is not supported by the read-only map.
		 *
		 * @throws UnsupportedOperationException Always.
		 */
		@Override
		public void clear() { throw new UnsupportedOperationException( "Read-only map" ); }
	}
	
	/**
	 * A read-write implementation of a hash map for object-to-object key-value pairs.
	 * It extends the read-only base class {@link R} to provide a full suite of
	 * modification methods, including adding, removing, and updating entries.
	 *
	 * @param <K> The type of keys maintained by this map.
	 * @param <V> The type of mapped values.
	 */
	class RW< K, V > extends R< K, V > {
		/**
		 * The number of collisions in a chain to tolerate before considering a resize
		 * with re-hashing.
		 */
		private static final int HashCollisionThreshold = 100; // Moved from R
		
		/**
		 * An optional function that can be set to provide a new hashing strategy for keys.
		 * This is used to resolve states with an unusually high number of hash collisions.
		 * When {@code resize} is called with {@code forceNewHashCodes = true}, this function
		 * is applied to {@link #equal_hash_K}.
		 */
		public Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes = null;
		
		/**
		 * Constructs an empty read-write map with a default initial capacity, using default
		 * hashing and equality strategies for keys and values based on their class types.
		 *
		 * @param clazzK The {@link Class} of the keys.
		 * @param clazzV The {@link Class} of the values.
		 */
		public RW( Class< K > clazzK, Class< V > clazzV ) { this( clazzK, clazzV, 0 ); }
		
		/**
		 * Constructs an empty read-write map with a specified initial capacity, using default
		 * hashing and equality strategies for keys and values based on their class types.
		 *
		 * @param clazzK   The {@link Class} of the keys.
		 * @param clazzV   The {@link Class} of the values.
		 * @param capacity The initial capacity of the map.
		 */
		public RW( Class< K > clazzK, Class< V > clazzV, int capacity ) { this( Array.get( clazzK ), Array.get( clazzV ), capacity ); }
		
		/**
		 * Constructs an empty read-write map with a default initial capacity, using a default
		 * key strategy and a custom value strategy.
		 *
		 * @param clazzK       The {@link Class} of the keys.
		 * @param equal_hash_V A custom strategy for value equality and hashing.
		 */
		public RW( Class< K > clazzK, Array.EqualHashOf< V > equal_hash_V ) { this( Array.get( clazzK ), equal_hash_V, 0 ); }
		
		/**
		 * Constructs an empty read-write map with a specified initial capacity, using a default
		 * key strategy and a custom value strategy.
		 *
		 * @param clazzK       The {@link Class} of the keys.
		 * @param equal_hash_V A custom strategy for value equality and hashing.
		 * @param capacity     The initial capacity of the map.
		 */
		public RW( Class< K > clazzK, Array.EqualHashOf< V > equal_hash_V, int capacity ) { this( Array.get( clazzK ), equal_hash_V, capacity ); }
		
		/**
		 * Constructs an empty read-write map with a default initial capacity, using custom
		 * strategies for both keys and values.
		 *
		 * @param equal_hash_K A custom strategy for key equality and hashing.
		 * @param equal_hash_V A custom strategy for value equality and hashing.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V ) { this( equal_hash_K, equal_hash_V, 0 ); }
		
		/**
		 * Constructs an empty read-write map with a specified initial capacity and custom
		 * strategies for both keys and values.
		 *
		 * @param equal_hash_K A custom strategy for key equality and hashing.
		 * @param equal_hash_V A custom strategy for value equality and hashing.
		 * @param capacity     The initial capacity of the map.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V, int capacity ) {
			
			super( equal_hash_V );
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity < 0" );
			this.equal_hash_K = equal_hash_K;
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Constructs a new read-write map with the same mappings as the specified {@link Map},
		 * using custom strategies for keys and values.
		 *
		 * @param equal_hash_K A custom strategy for key equality and hashing.
		 * @param equal_hash_V A custom strategy for value equality and hashing.
		 * @param map          The map whose mappings are to be placed in this map.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V, java.util.Map< ? extends K, ? extends V > map ) {
			super( equal_hash_V );
			this.equal_hash_K = equal_hash_K;
			putAll( map );
		}
		
		/**
		 * Constructs a new read-write map initialized from a collection of entries,
		 * using custom strategies for keys and values.
		 *
		 * @param equal_hash_K A custom strategy for key equality and hashing.
		 * @param equal_hash_V A custom strategy for value equality and hashing.
		 * @param collection   An iterable collection of entries to populate the map.
		 */
		@SuppressWarnings( "unchecked" )
		public RW( Array.EqualHashOf< K > equal_hash_K, Array.EqualHashOf< V > equal_hash_V, Iterable< Entry< K, V > > collection ) {
			super( equal_hash_V );
			this.equal_hash_K = equal_hash_K;
			if( collection instanceof RW ) {
				RW< K, V > src = ( RW< K, V > ) collection;
				K          key;
				for( int t = -1; ( t = src.unsafe_token( t ) ) != -1; )
				     copy( key = src.key( t ), equal_hash_K.hashCode( key ), src.value( t ) );
			}
			else
				for( Entry< K, V > src : collection )
					put( src.getKey(), src.getValue() );
		}
		
		
		/**
		 * Copies all of the mappings from the specified map to this map.
		 *
		 * @param m Mappings to be stored in this map.
		 */
		@Override
		public void putAll( Map< ? extends K, ? extends V > m ) {
			K key;
			ensureCapacity( size() + m.size() );
			for( Entry< ? extends K, ? extends V > src : m.entrySet() )
				copy( key = src.getKey(), equal_hash_K.hashCode( key ), src.getValue() );
		}
		
		/**
		 * Removes all of the mappings from this map. The map will be empty after this call returns.
		 */
		@Override
		public void clear() {
			_version++;
			hasNullKey   = false;
			nullKeyValue = null; // Clear null key value
			if( _count() == 0 ) return; // Use _count()
			Arrays.fill( _buckets, 0 );
			Arrays.fill( values, null );
			Arrays.fill( keys, null );
			_lo_Size = 0;
			_hi_Size = 0;
		}
		
		/**
		 * Removes the mapping for a key from this map if it is present.
		 *
		 * @param key key whose mapping is to be removed from the map.
		 * @return the previous value associated with the key, or {@code null} if there was no mapping for the key.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public V remove( Object key ) { return remove_( ( K ) key ); }
		
		/**
		 * Removes the mapping for the specified key from this map if present. This method performs
		 * the core removal logic, handling compaction of the internal arrays by moving elements
		 * to fill the freed slot.
		 *
		 * @param key The key whose mapping is to be removed.
		 * @return The previous value associated with the key, or {@code null} if no mapping existed.
		 * @throws ConcurrentModificationException if an excessively long collision chain is detected,
		 *                                         suggesting a corrupted state.
		 */
		public V remove_( K key ) {
			if( key == null ) {
				if( !hasNullKey ) return null;
				hasNullKey = false;
				V oldValue = nullKeyValue;
				nullKeyValue = null; // Clear null key value
				_version++;
				return oldValue;
			}
			
			if( _count() == 0 ) return null; // Use _count()
			
			int hash              = equal_hash_K.hashCode( key );
			int removeBucketIndex = bucketIndex( hash );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1; // 0-based index from bucket
			if( removeIndex < 0 ) return null; // Key not in this bucket
			
			// Case 1: Entry to be removed is in the hi Region (cannot be part of a chain from _lo_Size)
			if( _lo_Size <= removeIndex ) {
				if( this.hash[ removeIndex ] != hash || !equal_hash_K.equals( keys[ removeIndex ], key ) ) return null;
				
				V oldValue = values[ removeIndex ]; // Get old value before moving
				
				// Move the last element of hi region to the removed slot
				move( keys.length - _hi_Size, removeIndex );
				
				_hi_Size--; // Decrement hi_Size
				_buckets[ removeBucketIndex ] = 0; // Clear the bucket reference (it was the only one)
				_version++;
				return oldValue;
			}
			
			// Case 2: Entry to be removed is in the lo Region
			// Finding the key in the chain and updating links
			int next = links[ removeIndex ]; // Get the link from the first element in the chain
			
			// Key found at the head of the chain (removeIndex)
			if( this.hash[ removeIndex ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) _buckets[ removeBucketIndex ] = ( next + 1 ); // Update bucket to point to the next element
			else {
				// Key is NOT at the head of the chain. Traverse.
				int last = removeIndex; // 'last' tracks the node *before* 'removeIndex' (the element we are currently checking)
				
				// This block is from ObjectBitsMap, it handles the 2nd element in the chain.
				// 'removeIndex' is reassigned here to be the 'next' element.
				// The key is found at the 'SecondNode'
				// 'SecondNode' is in 'lo Region', relink 'last' to bypass 'SecondNode'
				if( this.hash[ removeIndex = next ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
				else {  // 'SecondNode' is in the hi Region (it's a terminal node)
					// This block performs a specific compaction for hi-region terminal nodes.
					keys[ removeIndex ]      = keys[ last ]; // Copies `keys[last]` to `keys[removeIndex]`
					this.hash[ removeIndex ] = this.hash[ last ];
					values[ removeIndex ]    = values[ last ]; // Copies `values[last]` to `values[removeIndex]`
					
					// 'removeBucketIndex' is the hash bucket for the original 'key'.
					// By pointing it to 'removeIndex' (which now contains data from 'last'),
					// we make 'last' (now at 'removeIndex') the new sole head of the bucket.
					_buckets[ removeBucketIndex ] = ( removeIndex + 1 );
					removeIndex                   = last; // Mark original 'last' (lo-region entry) for removal/compaction
				}
				else // Loop for 3rd+ elements in the chain
					// If 'removeIndex' (now the second element) is in hi-region and didn't match
					if( _lo_Size <= removeIndex ) return null; // Key not found in this chain
					else for( int collisionCount = 0; ; ) {
						int prev_for_loop = last; // This 'prev_for_loop' is the element *before* 'last' in this inner loop context
						
						// Advance 'last' and 'removeIndex' (current element being checked)
						if( this.hash[ removeIndex = links[ last = removeIndex ] ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) {
							// Found in lo-region: relink 'last' to bypass 'removeIndex'
							if( removeIndex < _lo_Size ) links[ last ] = links[ removeIndex ];
							else { // Found in hi-region (terminal node): Special compaction
								// Copy data from 'removeIndex' (hi-region node) to 'last' (lo-region node)
								keys[ removeIndex ]      = keys[ last ];
								this.hash[ removeIndex ] = this.hash[ last ];
								values[ removeIndex ]    = values[ last ]; // Adapted type
								
								// Relink 'prev_for_loop' (the element before 'last') to 'removeIndex'
								links[ prev_for_loop ] = removeIndex;
								                         removeIndex = last; // Mark original 'last' (lo-region entry) for removal/compaction
							}
							break; // Key found and handled, break from loop.
						}
						if( _lo_Size <= removeIndex ) return null; // Reached hi-region terminal node, key not found
						// Safeguard against excessively long or circular chains (corrupt state)
						if( _lo_Size + 1 < ++collisionCount )
							throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
			}
			
			V oldValue = values[ removeIndex ]; // Get old value before moving
			// At this point, 'removeIndex' holds the final index of the element to be removed.
			// This 'removeIndex' might have been reassigned multiple times within the if-else blocks.
			move( _lo_Size - 1, removeIndex ); // Move the last lo-region element to the spot of the removed entry
			_lo_Size--; // Decrement lo-region size
			_version++; // Structural modification
			return oldValue;
		}
		
		/**
		 * Relocates an entry's data from a source index to a destination index within the
		 * internal arrays. This method is a key part of the compaction process during removal.
		 * It moves the entry's data and crucially updates the bucket or link that pointed to the
		 * source index, ensuring the hash table's integrity.
		 *
		 * @param src The index of the entry to move.
		 * @param dst The index to move the entry to.
		 */
		private void move( int src, int dst ) {
			if( src == dst ) return;
			
			int bucketIndex = bucketIndex( this.hash[ src ] );
			int index       = _buckets[ bucketIndex ] - 1;
			
			if( index == src ) _buckets[ bucketIndex ] = ( dst + 1 ); // Update bucket head if src was the head
			else {
				// Find the link pointing to src
				// This loop iterates through the chain for the bucket to find the predecessor of `src`
				while( links[ index ] != src ) {
					index = links[ index ];
					// Defensive break for corrupted chain or if `src` is somehow not in this chain (shouldn't happen if logic is correct)
					if( index == -1 || index >= keys.length ) break;
				}
				if( links[ index ] == src ) // Ensure we found it before updating
					links[ index ] = dst; // Update the link to point to dst
			}
			
			// If src was in lo region, copy its link. THIS IS THE CORRECT LOGIC FROM ObjectBitsMap.
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			this.hash[ dst ] = this.hash[ src ]; // Copy hash
			keys[ dst ]      = keys[ src ];   // Copy key
			values[ dst ]    = values[ src ]; // Copy value
			keys[ src ]      = null;          // Clear source slot for memory management and to prevent stale references
			values[ src ]    = null;          // Clear source slot for memory management
		}
		
		/**
		 * Ensures that the map can hold at least the given number of entries without
		 * needing to resize. If the current capacity is insufficient, it is increased.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity of the map (which may be larger than requested).
		 * @throws IllegalArgumentException if the specified capacity is negative.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity is less than 0." );
			int currentCapacity = length();
			if( capacity <= currentCapacity ) return currentCapacity; // Already sufficient capacity
			_version++; // Increment version as structural change is about to occur
			if( _buckets == null ) return initialize( Array.prime( capacity ) ); // Initialize if buckets are null
			int newSize = Array.prime( capacity ); // Get next prime size
			resize( newSize, false );                // Resize to the new size
			return newSize;
		}
		
		/**
		 * Reduces the map's capacity to the minimum size required to hold the current number of entries.
		 */
		public void trim() { trim( _count() ); } // Use _count()
		
		/**
		 * Trims the capacity of the map to be closer to the specified capacity,
		 * without going below the current number of entries.
		 *
		 * @param capacity The desired capacity to trim to.
		 * @throws IllegalArgumentException if the specified capacity is less than the current size of the map.
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			if( length() <= ( capacity = Array.prime( Math.max( capacity, size() ) ) ) ) return;
			
			resize( capacity,false );
			
			if( _lo_Size < links.length )
				links = _lo_Size == 0 ?
				        Array.EqualHashOf._ints.O :
				        Array.copyOf( links, _lo_Size );
		}
		
		/**
		 * Associates the specified value with the specified key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 * @return The previous value associated with the key, or {@code null} if there was no mapping for the key.
		 */
		@Override
		public V put( K key, V value ) {
			long token = tokenOf( key );
			V ret = token == INVALID_TOKEN ?
			        null :
			        value( token );
			
			put_( key, value ); // Call the internal put_ method
			return ret;
		}
		
		/**
		 * The core logic for inserting or updating a key-value pair. If the key is new, it is added.
		 * If the key already exists, its value is updated. This method handles null keys,
		 * hash collisions, and resizing.
		 *
		 * @param key   The key to insert or update.
		 * @param value The value to associate with the key.
		 * @return {@code true} if a new mapping was created, {@code false} if an existing mapping was updated.
		 * @throws ConcurrentModificationException if an excessive number of collisions is detected,
		 *                                         suggesting a corrupted state.
		 */
		public boolean put_( K key, V value ) {
			if( key == null ) {
				boolean b = !hasNullKey;
				hasNullKey   = true;
				nullKeyValue = value;
				_version++;
				return b;
			}
			
			if( _buckets == null ) initialize( 7 ); // Initial capacity if not yet initialized
			else // If backing array is full, resize
				if( _count() == keys.length ) resize( Array.prime( keys.length * 2 ), false ); // Resize to double capacity
			
			int hash        = equal_hash_K.hashCode( key );
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from bucket
			int dst_index; // Destination index for the new/updated entry
			
			// If bucket is empty, new entry goes into hi Region
			if( index == -1 ) dst_index = keys.length - 1 - _hi_Size++; // Calculate new position in hi region
			else {
				
				int collisions = 0;
				for( int i = index; ; ) {
					if( this.hash[ i ] == hash && equal_hash_K.equals( keys[ i ], key ) ) {
						values[ i ] = value;// Update value
						_version++; // Increment version as value was modified
						return false;// Key was not new
					}
					if( _lo_Size <= i ) break; // Reached a terminal node in hi Region
					i = links[ i ]; // Move to next in chain
					// Safeguard against excessively long or circular chains (corrupt state)
					if( _lo_Size + 1 < collisions++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
				// Check for high collision and potential rehashing for string keys, adapted from original put()
				if( HashCollisionThreshold < collisions && this.forceNewHashCodes != null && key instanceof String ) // Check for high collision and potential rehashing for string keys
				{
					resize( keys.length, true ); // Resize to potentially trigger new hash codes
					hash        = equal_hash_K.hashCode( key );
					bucketIndex = bucketIndex( hash );
					index       = _buckets[ bucketIndex ] - 1;
				}
				
				( links.length == ( dst_index = _lo_Size++ ) ?
				  links = Arrays.copyOf( links, Math.max( 16, Math.min( _lo_Size * 2, keys.length ) ) ) :
				  links )[ dst_index ] = index; // New entry points to the old head
			}
			
			
			this.hash[ dst_index ]  = hash; // Store hash code
			keys[ dst_index ]       = key;     // Store key
			values[ dst_index ]     = value; // Store value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to point to the new entry (1-based)
			_version++; // Increment version
			
			return true; // New mapping added
		}
		
		/**
		 * Resizes the internal data structures to a new capacity. All existing entries are
		 * re-hashed and re-inserted into the new arrays.
		 *
		 * @param newSize           The new capacity for the map.
		 * @param forceNewHashCodes If {@code true}, applies the {@link #forceNewHashCodes} function
		 *                          to get a new key hashing strategy before re-inserting elements.
		 * @return The new capacity of the map.
		 */
		private int resize( int newSize, boolean forceNewHashCodes ) {
			_version++;
			
			// Store old data before re-initializing
			K[]   old_keys    = keys;
			int[] old_hash    = hash;
			V[]   old_values  = values; // No clone needed for Objects, they are referenced.
			int   old_lo_Size = _lo_Size;
			int   old_hi_Size = _hi_Size;
			
			if( links.length < 0xFF && links.length < _buckets.length ) links = _buckets;//reuse buckets as links
			initialize( newSize );
			
			// If forceNewHashCodes is set, apply it to the equal_hash_K provider BEFORE re-hashing elements.
			if( forceNewHashCodes ) {
				equal_hash_K = this.forceNewHashCodes.apply( equal_hash_K ); // Apply new hashing strategy
				
				// Copy elements from old structure to new structure by re-inserting
				K key;
				// Iterate through old lo region
				for( int i = 0; i < old_lo_Size; i++ ) {
					key = old_keys[ i ];
					copy( key, equal_hash_K.hashCode( key ), old_values[ i ] );
				}
				
				// Iterate through old hi region
				for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) {
					key = old_keys[ i ];
					copy( key, equal_hash_K.hashCode( key ), old_values[ i ] );
				}
				
				return keys.length; // Return actual new capacity
			}
			
			// Copy elements from old structure to new structure by re-inserting (original hash codes)
			
			// Iterate through old lo region
			for( int i = 0; i < old_lo_Size; i++ ) copy( old_keys[ i ], old_hash[ i ], old_values[ i ] );
			
			// Iterate through old hi region
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) copy( old_keys[ i ], old_hash[ i ], old_values[ i ] );
			
			return keys.length; // Return actual new capacity
		}
		
		/**
		 * Initializes the internal data structures of the map with a given capacity.
		 *
		 * @param capacity The initial capacity for the map's arrays.
		 * @return The capacity used for initialization.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets = new int[ capacity ];
			hash     = new int[ capacity ]; // Initialize hash array
			keys     = equal_hash_K.copyOf( null, capacity );
			values   = equal_hash_V.copyOf( null, capacity ); // Initialize values array
			_lo_Size = 0;
			_hi_Size = 0;
			return capacity;
		}
		
		/**
		 * Internal helper method to copy an entry into the map during a resize.
		 * It places the entry directly without checking for duplicates, as it assumes
		 * it's operating on a fresh, empty set of arrays.
		 *
		 * @param key   The key to copy.
		 * @param hash  The pre-computed hash code of the key.
		 * @param value The value to copy.
		 */
		private void copy( K key, int hash, V value ) {
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from the bucket
			
			int dst_index; // Destination index for the key
			
			if( index == -1 ) // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++;
			else ( links.length == _lo_Size ?
				  links = Arrays.copyOf( links, Math.max( 16, Math.min( _lo_Size * 2, keys.length ) ) ) :
				  links )[ dst_index = _lo_Size++ ] = ( char ) ( index );
			
			keys[ dst_index ]       = key; // Store the key
			this.hash[ dst_index ]  = hash; // Store the hash
			values[ dst_index ]     = value; // Store the value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to new head (1-based)
		}
		
		/**
		 * Copies the entries of this map into a destination array of {@link Entry} objects.
		 *
		 * @param array The destination array.
		 * @param index The starting position in the destination array.
		 * @throws IndexOutOfBoundsException if the index is out of bounds.
		 * @throws IllegalArgumentException  if the destination array is too small.
		 */
		private void copyTo( Entry< K, V >[] array, int index ) {
			if( index < 0 || index > array.length ) throw new IndexOutOfBoundsException( "Index out of range" );
			if( array.length - index < count() ) throw new IllegalArgumentException( "Arg_ArrayPlusOffTooSmall" );
			
			// Iterate active entries in the low region
			for( int i = 0; i < _lo_Size; i++ ) array[ index++ ] = new AbstractMap.SimpleEntry<>( keys[ i ], values[ i ] );
			// Iterate active entries in the high region
			for( int i = keys.length - _hi_Size; i < keys.length; i++ ) array[ index++ ] = new AbstractMap.SimpleEntry<>( keys[ i ], values[ i ] );
		}
		
		// Override inner classes to support mutation
		
		/**
		 * A read-write set view of the keys in a {@code ObjectObjectMap.RW}.
		 * It supports adding, removing, and clearing keys, with changes reflected
		 * in the backing map.
		 */
		public final class KeyCollection extends R< K, V >.KeyCollection {
			RW< K, V > _map;
			
			/**
			 * Constructs a new key collection backed by the specified read-write map.
			 *
			 * @param map The backing map.
			 */
			public KeyCollection( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Adds the specified key to the map, associated with a {@code null} value.
			 *
			 * @param key The key to add.
			 * @return {@code true} if the map did not already contain the key.
			 */
			@Override
			public boolean add( K key ) { boolean ret = !_map.contains( key ); _map.put_( key, null ); return ret; }
			
			/**
			 * Removes the specified key from the map.
			 *
			 * @param o The key to remove.
			 * @return {@code true} if the key was present and removed.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) { return _map.remove_( ( K ) o ) != null; } // Use remove_
			
			/**
			 * Removes all keys from the map.
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * An iterator over the keys of a {@code ObjectObjectMap.RW} that supports the
		 * {@code remove} operation.
		 */
		public class KeyIterator extends R< K, V >.KeyIterator {
			RW< K, V > _map;
			
			/**
			 * Constructs a key iterator for the specified read-write map.
			 *
			 * @param map The map whose keys are to be iterated.
			 */
			KeyIterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Removes the last key returned by {@link #next()} from the underlying map.
			 *
			 * @throws IllegalStateException if {@code next} has not yet been called, or {@code remove} has
			 *                               already been called after the last call to {@code next}.
			 */
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove_( _map.key( _currentToken ) ); // Use remove_
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * A read-write collection view of the values in a {@code ObjectObjectMap.RW}.
		 * It supports clearing the map and removing values. Adding a value will
		 * associate it with a {@code null} key.
		 */
		public final class ValueCollection extends R< K, V >.ValueCollection {
			RW< K, V > _map;
			
			/**
			 * Constructs a new value collection backed by the specified read-write map.
			 *
			 * @param map The backing map.
			 */
			public ValueCollection( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Adds a mapping from a {@code null} key to the specified value.
			 * If a {@code null} key already exists, its value is updated.
			 *
			 * @param item The value to add.
			 * @return {@code true} if a new mapping was created for the {@code null} key.
			 */
			@Override
			public boolean add( V item ) { return _map.put_( null, item ); } // Use put_
			
			/**
			 * Removes the first occurrence of a mapping with the specified value.
			 *
			 * @param value The value to remove.
			 * @return {@code true} if a mapping was found and removed.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object value ) {
				V v;
				try { v = ( V ) value; } catch( ClassCastException e ) { return false; } // Handle type mismatch gracefully
				
				for( long token = _map.token(); token != INVALID_TOKEN; token = _map.token( token ) )
					if( _map.equal_hash_V.equals( _map.value( token ), v ) ) {
						_map.remove_( _map.key( token ) ); // Use remove_
						return true;
					}
				return false;
			}
			
			/**
			 * Removes all values (and their associated keys) from the map.
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * An iterator over the values of a {@code ObjectObjectMap.RW}. The {@code remove}
		 * operation is not supported because it is ambiguous which key to remove if
		 * multiple keys map to the same value.
		 */
		public class ValueIterator extends R< K, V >.ValueIterator {
			ValueIterator( R< K, V > map ) { super( map ); }
			
			/**
			 * This operation is not supported by this iterator.
			 *
			 * @throws UnsupportedOperationException Always, as removing by value is ambiguous.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "remove by value is not supported" ); }
		}
		
		/**
		 * A read-write set view of the entries in a {@code ObjectObjectMap.RW}.
		 * It supports adding, removing, and clearing entries, with changes reflected
		 * in the backing map.
		 */
		public final class EntrySet extends R< K, V >.EntrySet {
			RW< K, V > _map;
			
			/**
			 * Constructs a new entry set backed by the specified read-write map.
			 *
			 * @param map The backing map.
			 */
			public EntrySet( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Adds the specified entry to the map.
			 *
			 * @param entry The entry to add.
			 * @return {@code true} if the map did not already contain the entry's key.
			 * @throws NullPointerException if the specified entry is null.
			 */
			@Override
			public boolean add( Entry< K, V > entry ) {
				if( entry == null ) throw new NullPointerException( "Entry is null" );
				boolean ret = !_map.contains( entry.getKey() );
				_map.put_( entry.getKey(), entry.getValue() ); // Use put_
				return ret;
			}
			
			/**
			 * Removes the entry corresponding to the key of the specified object.
			 *
			 * @param o The object (expected to be a {@link Map.Entry}) whose key's mapping is to be removed.
			 * @return {@code true} if an entry was removed.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) {
				if( !( o instanceof Entry ) ) return false;
				Entry< ?, ? > entry = ( Entry< ?, ? > ) o;
				return _map.remove_( ( K ) entry.getKey() ) != null; // Use remove_
			}
			
			/**
			 * Removes all entries from the map.
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * An iterator over the entries of a {@code ObjectObjectMap.RW} that supports the
		 * {@code remove} operation.
		 */
		public class EntryIterator extends R< K, V >.EntryIterator {
			RW< K, V > _map;
			
			/**
			 * Constructs an entry iterator for the specified read-write map.
			 *
			 * @param map The map whose entries are to be iterated.
			 */
			EntryIterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Removes the last entry returned by {@link #next()} from the underlying map.
			 *
			 * @throws IllegalStateException if {@code next} has not yet been called, or {@code remove} has
			 *                               already been called after the last call to {@code next}.
			 */
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove_( _map.key( _currentToken ) ); // Use remove_
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * Returns an iterator over the entries in this map that supports removal.
		 *
		 * @return An iterator over the map's entries.
		 */
		@Override
		public Iterator iterator() { return new Iterator( this ); }
		
		/**
		 * An iterator over the entries of a {@code ObjectObjectMap.RW} that supports the
		 * {@code remove} operation.
		 */
		public class Iterator extends R< K, V >.Iterator {
			RW< K, V > _map;
			
			/**
			 * Constructs an iterator for the specified read-write map.
			 *
			 * @param map The map whose entries are to be iterated.
			 */
			Iterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Removes the last entry returned by {@link #next()} from the underlying map.
			 *
			 * @throws IllegalStateException if {@code next} has not yet been called, or {@code remove} has
			 *                               already been called after the last call to {@code next}.
			 */
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove_( _map.key( _currentToken ) ); // Use remove_
				_currentToken = INVALID_TOKEN;
			}
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
		/**
		 * Creates and returns a shallow copy of this map. The keys and values themselves are not cloned.
		 *
		 * @return A clone of this map instance.
		 */
		@Override
		public RW< K, V > clone() { return ( RW< K, V > ) super.clone(); }
	}
	
	/**
	 * Returns a singleton {@link Array.EqualHashOf} strategy for comparing and hashing {@code RW} maps.
	 *
	 * @param <K> The key type of the map.
	 * @param <V> The value type of the map.
	 * @return The singleton equality and hash strategy instance.
	 */
	@SuppressWarnings( "unchecked" )
	static < K, V > Array.EqualHashOf< RW< K, V > > equal_hash() { return ( Array.EqualHashOf< RW< K, V > > ) RW.OBJECT; }
}