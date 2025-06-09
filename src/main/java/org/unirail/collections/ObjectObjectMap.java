// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// 1. The above copyright notice and this permission notice must be included in all
//    copies or substantial portions of the Software.
//
// 2. Users of the Software must provide a clear acknowledgment in their user
//    documentation or other materials that their solution includes or is based on
//    this Software. This acknowledgment should be prominent and easily visible,
//    and can be formatted as follows:
//    "This product includes software developed by Chikirev Sirguy and the Unirail Group
//    (https://github.com/AdHoc-Protocol)."
//
// 3. If you modify the Software and distribute it, you must include a prominent notice
//    stating that you have changed the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.*;
import java.util.function.Function;

/**
 * {@code ObjectObjectMap} is a generic interface for a hash map that stores key-value pairs where both keys and values are objects.
 * It provides efficient methods for common map operations, supporting both read-only and read-write implementations.
 * The map supports null keys and provides token-based access for efficient iteration and lookup.
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
		protected boolean hasNullKey;
		
		/**
		 * Stores the value associated with the null key, if present.
		 */
		protected V nullKeyValue;
		
		/**
		 * Array of buckets for the hash table, used for efficient key lookups.
		 */
		protected int[] _buckets;
		
		/**
		 * Array storing hash codes for each entry. (Replaces part of hash_nexts)
		 */
		protected int[] hash;
		
		/**
		 * Array storing the 'next' index in collision chains (0-based indices). (Replaces part of hash_nexts)
		 */
		protected int[] links;
		
		/**
		 * Array of keys stored in the map.
		 */
		protected K[] keys;
		
		/**
		 * Array of values corresponding to the keys in the map.
		 */
		protected V[] values;
		
		// Replaced _count, _freeList, _freeCount
		/**
		 * Number of active entries in the low region (0 to _lo_Size-1).
		 */
		protected int _lo_Size;
		/**
		 * Number of active entries in the high region (keys.length - _hi_Size to keys.length-1).
		 */
		protected int _hi_Size;
		
		/**
		 * Version number used for detecting concurrent modifications. Incremented on structural changes.
		 */
		protected int _version;
		
		/**
		 * Strategy for determining equality and hash code of keys.
		 */
		protected Array.EqualHashOf< K > equal_hash_K;
		
		/**
		 * Strategy for determining equality and hash code of values.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		/**
		 * Lazily initialized set view of the keys contained in this map.
		 */
		protected KeyCollection _keys;
		
		/**
		 * Lazily initialized collection view of the values contained in this map.
		 */
		protected ValueCollection _values;
		
		/**
		 * Lazily initialized set view of the entries contained in this map.
		 */
		protected EntrySet _entrySet;
		
		// Removed StartOfFreeList, HASH_CODE_MASK, NEXT_MASK - no longer needed with split hash/links
		
		/**
		 * Mask to extract the index from a token.
		 */
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		/**
		 * Bit shift for extracting the version from a token.
		 */
		protected static final int VERSION_SHIFT = 32;
		
		/**
		 * Represents an invalid token, typically returned when a key is not found or iteration has completed. Value is -1.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Constructs a read-only {@code ObjectObjectMap.R} with the specified value equality and hash strategy.
		 *
		 * @param equal_hash_V The strategy for value equality and hash code calculation.
		 */
		protected R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		/**
		 * Returns the total number of active non-null entries in the map.
		 * This is the sum of entries in the low and high regions.
		 *
		 * @return The current count of non-null entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * Checks if this map is empty.
		 *
		 * @return {@code true} if the map contains no key-value mappings, {@code false} otherwise.
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
		 * Returns the total capacity (number of slots) of the internal hash table, including both occupied and free slots.
		 *
		 * @return The capacity of the internal hash table.
		 */
		public int length() {
			return keys == null ?
			       0 :
			       keys.length;
		}
		
		/**
		 * Checks if this map contains a mapping for the specified key.
		 *
		 * @param key The key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean containsKey( Object key ) { return contains( ( K ) key ); }
		
		/**
		 * Checks if this map contains a mapping for the specified key.
		 *
		 * @param key The key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean contains( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this map contains a mapping with the specified value.
		 *
		 * @param value The value whose presence in this map is to be tested.
		 * @return {@code true} if this map contains at least one mapping with the specified value, {@code false} otherwise.
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
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		public V nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Returns a token associated with the given key, if present in the map.
		 * A token is a unique identifier combining the entry index and map version for efficient access to the key-value pair.
		 *
		 * @param key The key to find the token for.
		 * @return A valid token if the key is found, or {@link #INVALID_TOKEN} (-1) if the key is not found.
		 * @throws ConcurrentModificationException If excessive collisions indicate concurrent modification.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
			                         token( NULL_KEY_INDEX ) :
			                         INVALID_TOKEN;
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN;
			
			int hash  = equal_hash_K.hashCode( key );
			int index = _buckets[ bucketIndex( hash ) ] - 1; // 0-based index from bucket
			if( index < 0 ) return INVALID_TOKEN; // Bucket is empty
			
			// If the first entry is in the hi Region (it's the only one for this bucket)
			if( _lo_Size <= index ) return ( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) ?
			                               token( index ) :
			                               INVALID_TOKEN;
			
			// Traverse collision chain (entry is in lo Region)
			for( int collisions = 0; ; ) {
				if( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) return token( index );
				if( _lo_Size <= index ) break; // Reached a terminal node that might be in hi Region (no more links)
				index = links[ index ]; // Directly use links array
				if( _lo_Size + 1 < ++collisions ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the first valid token for iteration.
		 *
		 * @return A token for iteration, or {@link #INVALID_TOKEN} if the map is empty.
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
		 * Returns the next valid token for iteration.
		 *
		 * @param token The current token.
		 * @return The next token, or {@link #INVALID_TOKEN} if no further entries exist or the token is invalid.
		 * @throws IllegalArgumentException        If the provided token is {@link #INVALID_TOKEN}.
		 * @throws ConcurrentModificationException If the map has been modified since the token was obtained.
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
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
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
		 * Checks if the key associated with the given token is null.
		 *
		 * @param token The token of the entry.
		 * @return {@code true} if the key is null, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the key associated with the given token.
		 *
		 * @param token The token of the entry.
		 * @return The key associated with the token, or {@code null} if the token represents the null key.
		 * @throws ConcurrentModificationException If the map has been modified since the token was obtained.
		 */
		public K key( long token ) {
			return isKeyNull( token ) ?
			       null :
			       keys[ index( token ) ];
		}
		
		/**
		 * Returns the value associated with the given token.
		 *
		 * @param token The token of the entry.
		 * @return The value associated with the token.
		 * @throws ConcurrentModificationException If the map has been modified since the token was obtained.
		 */
		public V value( long token ) {
			return isKeyNull( token ) ?
			       // Use isKeyNull for clarity
			       nullKeyValue :
			       values[ index( token ) ];
		}
		
		/**
		 * Returns the value to which the specified key is mapped, or {@code null} if no mapping exists.
		 *
		 * @param key The key whose associated value is to be returned.
		 * @return The value associated with the key, or {@code null} if no mapping exists.
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
		 * Returns the value to which the specified key is mapped, or the default value if no mapping exists.
		 *
		 * @param key          The key whose associated value is to be returned.
		 * @param defaultValue The default value to return if no mapping exists.
		 * @return The value associated with the key, or {@code defaultValue} if no mapping exists.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public V getOrDefault( Object key, V defaultValue ) { return get( ( K ) key, defaultValue ); }
		
		/**
		 * Returns the value to which the specified key is mapped, or the default value if no mapping exists.
		 *
		 * @param key          The key whose associated value is to be returned.
		 * @param defaultValue The default value to return if no mapping exists.
		 * @return The value associated with the key, or {@code defaultValue} if no mapping exists.
		 */
		public V get( K key, V defaultValue ) {
			long token = tokenOf( key );
			return token == INVALID_TOKEN ?
			       defaultValue :
			       value( token );
		}
		
		/**
		 * Computes the hash code for this map based on its key-value mappings.
		 *
		 * @return The hash code for this map.
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
		 * Checks if this map is equal to another object.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R< K, V > ) obj );
		}
		
		/**
		 * Compares this map with another read-only map for equality.
		 * Two maps are equal if they contain the same key-value mappings.
		 *
		 * @param other The other map to compare with.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
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
		 * Creates a shallow copy of this map.
		 *
		 * @return A cloned instance of this map.
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
		 * @return A JSON string representing the map.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the map's contents to a JSON writer.
		 *
		 * @param json The JSON writer to write to.
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
		 * Calculates the bucket index for a given hash code.
		 *
		 * @param hash The hash code of the key.
		 * @return The bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		// Removed static methods hash, next, hash_next, next(long[],...), hash(long[],...) as hash_nexts is gone
		
		/**
		 * Creates a token from an index and the current version.
		 *
		 * @param index The index of the entry.
		 * @return The created token.
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); }
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token The token.
		 * @return The extracted index.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token The token.
		 * @return The extracted version.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		/**
		 * Returns a set view of the keys contained in this map.
		 *
		 * @return A set view of the keys.
		 */
		@Override
		public Set< K > keySet() { return keys(); }
		
		/**
		 * Returns a read-only {@link KeyCollection} view of the keys contained in this map.
		 * The collection is backed by the map, so changes to the map are reflected in the collection.
		 * The collection does not support modifications.
		 *
		 * @return A set view of the keys contained in this map.
		 */
		public KeyCollection keys() {
			return _keys == null ?
			       ( _keys = new KeyCollection( this ) ) :
			       _keys;
		}
		
		/**
		 * Returns a collection view of the values contained in this map.
		 *
		 * @return A collection view of the values.
		 */
		@Override
		public Collection< V > values() {
			return _values == null ?
			       ( _values = new ValueCollection( this ) ) :
			       _values;
		}
		
		/**
		 * Returns a set view of the entries contained in this map.
		 *
		 * @return A set view of the entries.
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
		 * @return An iterator over the entries of this map.
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
			 * @param map The read-only map.
			 */
			public KeyCollection( R< K, V > map ) { _map = map; }
			
			/**
			 * Returns the number of keys in this set.
			 *
			 * @return The number of keys.
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * Checks if this set contains the specified key.
			 *
			 * @param item The key to check for.
			 * @return {@code true} if the set contains the key, {@code false} otherwise.
			 */
			@Override
			public boolean contains( Object item ) { return _map.containsKey( item ); }
			
			/**
			 * Returns an iterator over the keys in this set.
			 *
			 * @return An iterator over the keys.
			 */
			@Override
			public java.util.Iterator< K > iterator() { return new KeyIterator( _map ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public boolean add( K item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Copies the keys from this collection to the specified array, starting at the specified index.
			 *
			 * @param array The array to copy the keys into.
			 * @param index The starting index in the array.
			 * @throws IndexOutOfBoundsException If the index is out of range.
			 * @throws IllegalArgumentException  If the array is too small to contain all the keys.
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
			 * @param map The read-only map to iterate over.
			 */
			KeyIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * Checks if there are more keys to iterate over.
			 *
			 * @return {@code true} if there are more keys, {@code false} otherwise.
			 * @throws ConcurrentModificationException If the map has been modified during iteration.
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
			 * @throws ConcurrentModificationException If the map has been modified during iteration.
			 * @throws NoSuchElementException          If there are no more keys to iterate over.
			 */
			@Override
			public K next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentKey = _map.key( _currentToken );
				return _currentKey;
			}
			
			/**
			 * Throws an exception as this is a read-only iterator.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
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
			 * @param map The read-only map.
			 */
			public ValueCollection( R< K, V > map ) { _map = map; }
			
			/**
			 * Returns the number of values in this collection.
			 *
			 * @return The number of values.
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * Checks if this collection contains the specified value.
			 *
			 * @param item The value to check for.
			 * @return {@code true} if the collection contains the value, {@code false} otherwise.
			 */
			@Override
			public boolean contains( Object item ) { return _map.containsValue( item ); }
			
			/**
			 * Returns an iterator over the values in this collection.
			 *
			 * @return An iterator over the values.
			 */
			@Override
			public java.util.Iterator< V > iterator() { return new ValueIterator( _map ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public boolean add( V item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public boolean remove( Object item ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Copies the values from this collection to the specified array, starting at the specified index.
			 *
			 * @param array The array to copy the values into.
			 * @param index The starting index in the array.
			 * @throws NullPointerException      If the array is null.
			 * @throws IndexOutOfBoundsException If the index is out of range.
			 * @throws IllegalArgumentException  If the array is too small to contain all the values.
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
			 * @param map The read-only map to iterate over.
			 */
			ValueIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * Checks if there are more values to iterate over.
			 *
			 * @return {@code true} if there are more values, {@code false} otherwise.
			 * @throws ConcurrentModificationException If the map has been modified during iteration.
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
			 * @throws ConcurrentModificationException If the map has been modified during iteration.
			 * @throws NoSuchElementException          If there are no more values to iterate over.
			 */
			@Override
			public V next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentValue = _map.value( _currentToken );
				return _currentValue;
			}
			
			/**
			 * Throws an exception as this is a read-only iterator.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
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
			 * @param map The read-only map.
			 */
			public EntrySet( R< K, V > map ) { _map = map; }
			
			/**
			 * Returns the number of entries in this set.
			 *
			 * @return The number of entries.
			 */
			@Override
			public int size() { return _map.size(); }
			
			/**
			 * Checks if this set contains the specified entry's key.
			 *
			 * @param o The object to check for (expected to be a key).
			 * @return {@code true} if the set contains an entry with the specified key, {@code false} otherwise.
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
			 * Returns an iterator over the entries in this set.
			 *
			 * @return An iterator over the entries.
			 */
			@Override
			public java.util.Iterator< Entry< K, V > > iterator() { return new EntryIterator( _map ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public void clear() { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public boolean add( Entry< K, V > entry ) { throw new UnsupportedOperationException( "Read-only collection" ); }
			
			/**
			 * Throws an exception as this is a read-only collection.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
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
			 * @param map The read-only map to iterate over.
			 */
			EntryIterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * Checks if there are more entries to iterate over.
			 *
			 * @return {@code true} if there are more entries, {@code false} otherwise.
			 * @throws ConcurrentModificationException If the map has been modified during iteration.
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
			 * @throws ConcurrentModificationException If the map has been modified during iteration.
			 * @throws NoSuchElementException          If there are no more entries to iterate over.
			 */
			@Override
			public Entry< K, V > next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_currentEntry = new AbstractMap.SimpleEntry<>( _map.key( _currentToken ), _map.value( _currentToken ) );
				return _currentEntry;
			}
			
			/**
			 * Throws an exception as this is a read-only iterator.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		/**
		 * {@code Iterator} is a read-only iterator for the entries of a {@code ObjectObjectMap.R}.
		 * It allows iteration over the key-value entries in the map.
		 */
		public class Iterator implements java.util.Iterator< Entry< K, V > > {
			private final R< K, V >     _map;
			private final int           _version;
			protected     long          _currentToken;
			private       Entry< K, V > _current;
			
			/**
			 * Constructs an {@code Iterator} for the entries of the given read-only map.
			 *
			 * @param map The read-only map to iterate over.
			 */
			Iterator( R< K, V > map ) {
				_map          = map;
				_version      = map._version;
				_currentToken = INVALID_TOKEN;
			}
			
			/**
			 * Checks if there are more entries to iterate over.
			 *
			 * @return {@code true} if there are more entries, {@code false} otherwise.
			 * @throws ConcurrentModificationException If the map has been modified during iteration.
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
			 * @throws ConcurrentModificationException If the map has been modified during iteration.
			 * @throws NoSuchElementException          If there are no more entries to iterate over.
			 */
			@Override
			public Entry< K, V > next() {
				if( _version != _map._version ) throw new ConcurrentModificationException();
				if( _currentToken == INVALID_TOKEN ) throw new NoSuchElementException();
				_current = new AbstractMap.SimpleEntry<>( _map.key( _currentToken ), _map.value( _currentToken ) );
				return _current;
			}
			
			/**
			 * Throws an exception as this is a read-only iterator.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "Read-only iterator" ); }
		}
		
		// Unsupported write operations
		
		/**
		 * Throws an exception as this is a read-only map.
		 *
		 * @throws UnsupportedOperationException Always thrown.
		 */
		@Override
		public V put( K key, V value ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * Throws an exception as this is a read-only map.
		 *
		 * @throws UnsupportedOperationException Always thrown.
		 */
		@Override
		public V remove( Object key ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * Throws an exception as this is a read-only map.
		 *
		 * @throws UnsupportedOperationException Always thrown.
		 */
		@Override
		public void putAll( Map< ? extends K, ? extends V > m ) { throw new UnsupportedOperationException( "Read-only map" ); }
		
		/**
		 * Throws an exception as this is a read-only map.
		 *
		 * @throws UnsupportedOperationException Always thrown.
		 */
		@Override
		public void clear() { throw new UnsupportedOperationException( "Read-only map" ); }
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
		 * Threshold for hash collision count, used to trigger rehashing for performance optimization.
		 */
		private static final int HashCollisionThreshold = 100; // Moved from R
		
		/**
		 * Function to force new hash codes for keys, used in collision resolution strategies. Can be set externally.
		 */
		public Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes = null;
		
		/**
		 * Constructs a read-write {@code ObjectObjectMap.RW} with default capacity (7).
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
		 * Constructs a read-write {@code ObjectObjectMap.RW} with default capacity (7).
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
		 * Constructs a read-write {@code ObjectObjectMap.RW} with default capacity (7).
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
			if( capacity < 0 ) throw new IllegalArgumentException( "capacity < 0" );
			this.equal_hash_K = equal_hash_K;
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
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
		 * Copies all mappings from the specified map to this map.
		 *
		 * @param m The map containing mappings to be copied.
		 */
		@Override
		public void putAll( Map< ? extends K, ? extends V > m ) {
			K key;
			ensureCapacity( size() + m.size() );
			for( Entry< ? extends K, ? extends V > src : m.entrySet() )
				copy( key = src.getKey(), equal_hash_K.hashCode( key ), src.getValue() );
		}
		
		/**
		 * Removes all mappings from this map.
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
		 * Removes the mapping for the specified key from this map, if present.
		 *
		 * @param key The key whose mapping is to be removed.
		 * @return The previous value associated with the key, or {@code null} if no mapping existed.
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public V remove( Object key ) { return remove_( ( K ) key ); }
		
		/**
		 * Removes the mapping for the specified key from this map, if present.
		 *
		 * @param key The key whose mapping is to be removed.
		 * @return The previous value associated with the key, or {@code null} if no mapping existed.
		 * @throws ConcurrentModificationException If excessive collisions indicate concurrent modification.
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
		 * Relocates an entry's key, hash, link, and value from a source index ({@code src}) to a destination index ({@code dst})
		 * within the internal arrays. This method is crucial for compaction during removal operations.
		 * <p>
		 * After moving the data, this method updates any existing pointers (either from a hash bucket in
		 * {@code _buckets} or from a {@code links} link in a collision chain) that previously referenced
		 * {@code src} to now correctly reference {@code dst}.
		 *
		 * @param src The index of the entry to be moved (its current location).
		 * @param dst The new index where the entry's data will be placed.
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
		 * Ensures the map's capacity is at least the specified minimum capacity.
		 * If the current capacity is less, it is increased to the next prime number greater than or equal to the specified capacity.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity of the map.
		 * @throws IllegalArgumentException If the capacity is negative.
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
		 * Reduces the map's capacity to the minimum size that can hold the current number of entries.
		 */
		public void trim() { trim( _count() ); } // Use _count()
		
		/**
		 * Reduces the map's capacity to the minimum size that can hold the specified capacity.
		 *
		 * @param capacity The desired capacity to trim to.
		 * @throws IllegalArgumentException If the capacity is less than the current number of entries.
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than Count." ); // Use _count()
			int currentCapacity = length();
			capacity = Array.prime( Math.max( capacity, _count() ) ); // Ensure capacity is at least current count and prime. Use _count()
			if( currentCapacity <= capacity ) return; // No need to trim if current capacity is already smaller or equal
			
			resize( capacity, false ); // Resize to the new smaller size
		}
		
		/**
		 * Associates the specified value with the specified key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 * @return The previous value associated with the key, or {@code null} if no mapping existed.
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
		 * Inserts or updates a key-value pair in the map.
		 * Handles null keys, hash collisions, and resizing as needed.
		 *
		 * @param key   The key to insert or update.
		 * @param value The value to associate with the key.
		 * @return {@code true} if a new mapping was inserted, {@code false} if an existing mapping was updated.
		 * @throws ConcurrentModificationException If excessive collisions indicate concurrent modification.
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
				// Bucket is not empty, 'index' points to an existing entry
				if( _lo_Size <= index ) { // Existing entry is in {@code hi Region}
					if( this.hash[ index ] == hash && equal_hash_K.equals( keys[ index ], key ) ) { // Key matches existing {@code hi Region} entry
						values[ index ] = value; // Update value
						_version++;
						return false; // Key was not new
					}
				}
				else  // Existing entry is in {@code lo Region} (collision chain)
				{
					int collisions = 0;
					for( int next = index; ; ) {
						if( this.hash[ next ] == hash && equal_hash_K.equals( keys[ next ], key ) ) {
							values[ next ] = value;// Update value
							_version++; // Increment version as value was modified
							return false;// Key was not new
						}
						if( _lo_Size <= next ) break; // Reached a terminal node in hi Region
						next = links[ next ]; // Move to next in chain
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
				}
				
				
				// Key is new, and a collision occurred (bucket was not empty). Place new entry in {@code lo Region}.
				if( links.length == ( dst_index = _lo_Size++ ) ) // If links array needs resize, and assign new index
					// Resize links array, cap at keys.length to avoid unnecessary large array
					links = Arrays.copyOf( links, Math.min( keys.length, links.length * 2 ) );
				
				links[ dst_index ] = ( int ) index; // Link new entry to the previous head of the chain
			}
			
			
			this.hash[ dst_index ]  = hash; // Store hash code
			keys[ dst_index ]       = key;     // Store key
			values[ dst_index ]     = value; // Store value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to point to the new entry (1-based)
			_version++; // Increment version
			
			return true; // New mapping added
		}
		
		/**
		 * Resizes the internal hash table to the specified size.
		 * Optionally recalculates hash codes for all keys if {@code forceNewHashCodes} is true.
		 *
		 * @param newSize           The new size of the hash table.
		 * @param forceNewHashCodes If {@code true}, recalculates hash codes for all keys.
		 */
		private int resize( int newSize, boolean forceNewHashCodes ) {
			_version++;
			
			// Store old data before re-initializing
			K[]   old_keys    = keys;
			int[] old_hash    = hash;
			V[]   old_values  = values; // No clone needed for Objects, they are referenced.
			int   old_lo_Size = _lo_Size;
			int   old_hi_Size = _hi_Size;
			
			// Re-initialize with new capacity (this clears _buckets, resets _lo_Size, _hi_Size)
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
		 * Initializes the internal hash table with the specified capacity.
		 *
		 * @param capacity The initial capacity of the hash table.
		 * @return The prime number capacity used.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets = new int[ capacity ];
			hash     = new int[ capacity ]; // Initialize hash array
			links    = new int[ Math.min( 16, capacity ) ]; // Initialize links with a small, reasonable capacity (from ObjectBitsMap)
			keys     = equal_hash_K.copyOf( null, capacity );
			values   = equal_hash_V.copyOf( null, capacity ); // Initialize values array
			_lo_Size = 0;
			_hi_Size = 0;
			return capacity;
		}
		
		/**
		 * Internal helper method used during resizing to efficiently copy an
		 * existing key-value pair into the new hash table structure. It re-hashes the key
		 * and places it into the correct bucket and region (lo or hi) in the new arrays.
		 * This method does not check for existing keys, assuming all keys are new in the
		 * target structure during a resize operation.
		 *
		 * @param key   The key to copy.
		 * @param hash  The hash code of the key.
		 * @param value The value associated with the key.
		 */
		private void copy( K key, int hash, V value ) {
			int bucketIndex = bucketIndex( hash );
			int index       = _buckets[ bucketIndex ] - 1; // 0-based index from the bucket
			
			int dst_index; // Destination index for the key
			
			if( index == -1 ) // Bucket is empty: place new entry in {@code hi Region}
				dst_index = keys.length - 1 - _hi_Size++;
			else {
				// Collision occurred. Place new entry in {@code lo Region}
				if( links.length == _lo_Size ) // If lo_Size exceeds links array capacity
					links = Arrays.copyOf( links, Math.min( _lo_Size * 2, keys.length ) ); // Resize links
				
				links[ dst_index = _lo_Size++ ] = index; // New entry points to the old head
			}
			
			keys[ dst_index ]       = key; // Store the key
			this.hash[ dst_index ]  = hash; // Store the hash
			values[ dst_index ]     = value; // Store the value
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to new head (1-based)
		}
		
		/**
		 * Copies the entries from this map to the specified array of {@link Entry} objects, starting at the specified index.
		 *
		 * @param array The array to copy the entries into.
		 * @param index The starting index in the array.
		 * @throws IndexOutOfBoundsException If the index is out of range.
		 * @throws IllegalArgumentException  If the array is too small to contain all the entries.
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
		 * {@code KeyCollection} for {@code ObjectObjectMap.RW} extends the read-only version to support write operations.
		 * It allows adding and removing keys, and clearing the collection, which modifies the underlying map.
		 */
		public final class KeyCollection extends R< K, V >.KeyCollection {
			RW< K, V > _map;
			
			/**
			 * Constructs a {@code KeyCollection} associated with the given read-write map.
			 *
			 * @param map The read-write map.
			 */
			public KeyCollection( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Adds a key with a null value to the map.
			 *
			 * @param key The key to add.
			 * @return {@code true} if the key was added, {@code false} if it already existed.
			 */
			@Override
			public boolean add( K key ) { boolean ret = !_map.contains( key ); _map.put_( key, null ); return ret; }
			
			/**
			 * Removes the specified key from the map.
			 *
			 * @param o The key to remove.
			 * @return {@code true} if the key was removed, {@code false} if it did not exist.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) { return _map.remove_( ( K ) o ) != null; } // Use remove_
			
			/**
			 * Clears all mappings from the map.
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code KeyIterator} for {@code ObjectObjectMap.RW} extends the read-only version to support removal of keys during iteration.
		 * It allows removing the current key from the underlying map.
		 */
		public class KeyIterator extends R< K, V >.KeyIterator {
			RW< K, V > _map;
			
			/**
			 * Constructs a {@code KeyIterator} for the keys of the given read-write map.
			 *
			 * @param map The read-write map to iterate over.
			 */
			KeyIterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Removes the current key from the map.
			 *
			 * @throws IllegalStateException If there is no current element.
			 */
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove_( _map.key( _currentToken ) ); // Use remove_
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * {@code ValueCollection} for {@code ObjectObjectMap.RW} extends the read-only version to support write operations.
		 * It allows adding values (with a null key) and removing values, which modifies the underlying map.
		 */
		public final class ValueCollection extends R< K, V >.ValueCollection {
			RW< K, V > _map;
			
			/**
			 * Constructs a {@code ValueCollection} associated with the given read-write map.
			 *
			 * @param map The read-write map.
			 */
			public ValueCollection( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Adds a value with a null key to the map.
			 *
			 * @param item The value to add.
			 * @return {@code true} if the value was added, {@code false} if a null key already existed.
			 */
			@Override
			public boolean add( V item ) { return _map.put_( null, item ); } // Use put_
			
			/**
			 * Removes the first entry with the specified value from the map.
			 *
			 * @param value The value to remove.
			 * @return {@code true} if an entry was removed, {@code false} if no matching value was found.
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
			 * Clears all mappings from the map.
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code ValueIterator} for {@code ObjectObjectMap.RW} extends the read-only version.
		 * Removal is not supported as it is ambiguous which key to remove when values are not unique.
		 */
		public class ValueIterator extends R< K, V >.ValueIterator {
			ValueIterator( R< K, V > map ) { super( map ); }
			
			/**
			 * Throws an exception as removal by value is not supported.
			 *
			 * @throws UnsupportedOperationException Always thrown.
			 */
			@Override
			public void remove() { throw new UnsupportedOperationException( "remove by value is not supported" ); }
		}
		
		/**
		 * {@code EntrySet} for {@code ObjectObjectMap.RW} extends the read-only version to support write operations.
		 * It allows adding and removing entries, and clearing the set, which modifies the underlying map.
		 */
		public final class EntrySet extends R< K, V >.EntrySet {
			RW< K, V > _map;
			
			/**
			 * Constructs an {@code EntrySet} associated with the given read-write map.
			 *
			 * @param map The read-write map.
			 */
			public EntrySet( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Adds an entry to the map.
			 *
			 * @param entry The entry to add.
			 * @return {@code true} if the entry was added, {@code false} if it already existed.
			 * @throws NullPointerException If the entry is null.
			 */
			@Override
			public boolean add( Entry< K, V > entry ) {
				if( entry == null ) throw new NullPointerException( "Entry is null" );
				boolean ret = !_map.contains( entry.getKey() );
				_map.put_( entry.getKey(), entry.getValue() ); // Use put_
				return ret;
			}
			
			/**
			 * Removes the entry with the specified key from the map.
			 *
			 * @param o The object (expected to be an Entry) whose key is to be removed.
			 * @return {@code true} if the entry was removed, {@code false} if it did not exist.
			 */
			@Override
			@SuppressWarnings( "unchecked" )
			public boolean remove( Object o ) {
				if( !( o instanceof Entry ) ) return false;
				Entry< ?, ? > entry = ( Entry< ?, ? > ) o;
				return _map.remove_( ( K ) entry.getKey() ) != null; // Use remove_
			}
			
			/**
			 * Clears all mappings from the map.
			 */
			@Override
			public void clear() { _map.clear(); }
		}
		
		/**
		 * {@code EntryIterator} for {@code ObjectObjectMap.RW} extends the read-only version to support removal of entries during iteration.
		 * It allows removing the current entry from the underlying map.
		 */
		public class EntryIterator extends R< K, V >.EntryIterator {
			RW< K, V > _map;
			
			/**
			 * Constructs an {@code EntryIterator} for the entries of the given read-write map.
			 *
			 * @param map The read-write map to iterate over.
			 */
			EntryIterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Removes the current entry from the map.
			 *
			 * @throws IllegalStateException If there is no current element.
			 */
			@Override
			public void remove() {
				if( _currentToken == INVALID_TOKEN ) throw new IllegalStateException( "No current element" );
				_map.remove_( _map.key( _currentToken ) ); // Use remove_
				_currentToken = INVALID_TOKEN;
			}
		}
		
		/**
		 * Returns an iterator over the entries in this map, supporting removal.
		 *
		 * @return An iterator over the entries.
		 */
		@Override
		public Iterator iterator() { return new Iterator( this ); }
		
		/**
		 * {@code Iterator} for {@code ObjectObjectMap.RW} extends the read-only version to support removal of entries during iteration.
		 * It allows removing the current entry from the underlying map.
		 */
		public class Iterator extends R< K, V >.Iterator {
			RW< K, V > _map;
			
			/**
			 * Constructs an {@code Iterator} for the entries of the given read-write map.
			 *
			 * @param map The read-write map to iterate over.
			 */
			Iterator( RW< K, V > map ) {
				super( map );
				_map = map;
			}
			
			/**
			 * Removes the current entry from the map.
			 *
			 * @throws IllegalStateException If there is no current element.
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
		 * Creates a shallow copy of this map.
		 *
		 * @return A cloned instance of this map.
		 */
		@Override
		public RW< K, V > clone() { return ( RW< K, V > ) super.clone(); }
	}
	
	/**
	 * Returns the equality and hash strategy for {@code RW} instances.
	 *
	 * @param <K> The type of keys.
	 * @param <V> The type of values.
	 * @return The equality and hash strategy.
	 */
	@SuppressWarnings( "unchecked" )
	static < K, V > Array.EqualHashOf< RW< K, V > > equal_hash() { return ( Array.EqualHashOf< RW< K, V > > ) RW.OBJECT; }
}