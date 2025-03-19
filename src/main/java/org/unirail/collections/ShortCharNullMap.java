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
 * {@code IntIntNullMap} is a specialized map interface designed for storing integer keys and integer values.
 * <p>
 * It provides efficient operations for managing key-value pairs where both keys and values are integers.
 * The map supports null keys and utilizes a hash table with separate chaining to resolve hash collisions,
 * ensuring good performance even with a large number of entries.
 * <p>
 * This interface defines the contract for maps that need to handle integer-to-integer mappings with null key support,
 * offering methods for common map operations like insertion, retrieval, deletion, and checking for existence.
 */
public interface ShortCharNullMap {
	
	/**
	 * {@code R} is an abstract base class that provides read-only operations for implementations of the {@code IntIntNullMap} interface.
	 * <p>
	 * It serves as a foundation for read-write map implementations by encapsulating the common read operations
	 * and data storage mechanisms. This class includes functionalities for checking map size, emptiness,
	 * containment of keys and values, and iteration through map entries using tokens.
	 * <p>
	 * {@code R} also implements {@code Cloneable} and {@code JsonWriter.Source}, allowing map instances to be cloned
	 * and serialized to JSON format, respectively.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		/**
		 * Indicates whether the map contains a null key.
		 */
		protected boolean                hasNullKey;
		/**
		 * Indicates whether the null key has an associated value.
		 */
		protected boolean                nullKeyHasValue;
		/**
		 * Stores the value associated with the null key.
		 */
		protected char            nullKeyValue;
		/**
		 * Array of buckets for the hash table. Each element is a 1-based index pointing to the head of a collision chain in the {@code nexts} array.
		 */
		protected int[]          _buckets;
		/**
		 * Array of next indices for collision chains. Each element points to the next entry in the chain or contains special markers like {@code StartOfFreeList}.
		 */
		protected int[]          nexts;
		/**
		 * Array of keys. Holds the integer keys for each entry in the map.
		 */
		protected short[]          keys = Array.EqualHashOf.shorts     .O;
		/**
		 * List of values. Manages integer values and null values, associated with keys in the {@code keys} array.
		 */
		protected CharNullList.RW values;
		/**
		 * Total number of entries in the internal arrays, including both active entries and free slots.
		 */
		protected int                    _count;
		/**
		 * Index of the first entry in the free list. -1 indicates that the free list is empty.
		 */
		protected int                    _freeList;
		/**
		 * Number of free entries available in the free list.
		 */
		protected int                    _freeCount;
		/**
		 * Version counter used for detecting concurrent modifications. Incremented on structural changes to the map.
		 */
		protected int                    _version;
		
		/**
		 * Constant to mark the start of the free list in the 'next' field.
		 */
		protected static final int  StartOfFreeList = -3;
		/**
		 * Mask to extract the index from a token.
		 */
		protected static final long INDEX_MASK      = 0x0000_0000_7FFF_FFFFL;
		/**
		 * Number of bits to shift the version in a token.
		 */
		protected static final int  VERSION_SHIFT   = 32;
		
		/**
		 * Constant representing an invalid token.
		 */
		protected static final long INVALID_TOKEN = -1L;
		
		/**
		 * Checks if this map is empty (contains no key-value mappings).
		 *
		 * @return {@code true} if this map is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() {
			return size() == 0;
		}
		
		/**
		 * Returns the number of key-value mappings in this map.
		 * <p>
		 * This count includes the mapping for the null key if present.
		 *
		 * @return the number of key-value mappings in this map.
		 */
		public int size() {
			return _count - _freeCount + ( hasNullKey ?
					1 :
					0 );
		}
		
		/**
		 * Returns the number of key-value mappings in this map.
		 * <p>
		 * This is an alias for {@link #size()}.
		 *
		 * @return the number of key-value mappings in this map.
		 * @see #size()
		 */
		public int count() {
			return size(); // Alias for size()
		}
		
		/**
		 * Returns the allocated capacity of the internal arrays used by this map.
		 * <p>
		 * This is the maximum number of entries the map can hold without resizing.
		 *
		 * @return the length of the internal arrays, or 0 if the map is uninitialized.
		 */
		public int length() {
			return nexts == null ?
					0 :
					nexts.length;
		}
		
		/**
		 * Checks if this map contains a mapping for the specified key (boxed Integer).
		 *
		 * @param key the key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean contains(  Short     key ) {
			return key == null ?
					hasNullKey :
					contains( key.shortValue     () );
		}
		
		/**
		 * Checks if this map contains a mapping for the specified key (primitive int).
		 *
		 * @param key the key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean contains( short key ) {
			return tokenOf( key ) != INVALID_TOKEN;
		}
		
		/**
		 * Checks if this map contains the specified value.
		 * <p>
		 *
		 * @param value the value whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping with the specified value, {@code false} otherwise.
		 */
		public boolean containsValue(  Character value ) {
			if( _count == 0 && !hasNullKey ) return false;
			return value == null ?
					!nullKeyHasValue :
					values.indexOf( value ) != -1;
		}
		
		/**
		 * Checks if this map contains the specified value (primitive int).
		 * <p>
		 *
		 * @param value the value whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping with the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( char value ) {
			if( _count == 0 ) return false;
			return values.indexOf( value ) != -1;
		}
		
		/**
		 * Returns a token associated with the specified key (boxed Integer).
		 * <p>
		 * Tokens are used for efficient iteration and access to map entries.
		 *
		 * @param key the key for which to find a token.
		 * @return a token if the key is found, {@link #INVALID_TOKEN} otherwise.
		 */
		public long tokenOf(  Short     key ) {
			return key == null ?
					( hasNullKey ?
							token( _count ) :
							INVALID_TOKEN ) :
					tokenOf( key.shortValue     () );
		}
		
		/**
		 * Returns a token associated with the specified key (primitive int).
		 * <p>
		 * Tokens are used for efficient iteration and access to map entries.
		 *
		 * @param key the key for which to find a token.
		 * @return a token if the key is found, {@link #INVALID_TOKEN} otherwise.
		 */
		public long tokenOf( short key ) {
			if( _buckets == null ) return INVALID_TOKEN;
			int hash = Array.hash( key );
			int i    = _buckets[ bucketIndex( hash ) ]/**/ - 1;
			
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key ) return token( i );
				i = nexts[ i ];
				if( nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the initial token for iteration over the map entries.
		 * <p>
		 * This token points to the first entry in the map. Subsequent entries can be accessed using {@link #token(long)}.
		 *
		 * @return the first valid token for iteration, or {@link #INVALID_TOKEN} if the map is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i );
			return hasNullKey ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next token in the iteration sequence, starting from the given token.
		 * <p>
		 * This method allows iterating through the map entries in the order they are stored internally.
		 *
		 * @param token the current token in the iteration.
		 * @return the next valid token in the iteration, or {@link #INVALID_TOKEN} if there are no more entries.
		 * @throws ConcurrentModificationException if the collection was modified while iterating.
		 */
		public long token( long token ) {
			if( !isValid( token ) )
				throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
			for( int i = index( token ) + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i );
			return hasNullKey && index( token ) < _count ?
					token( _count ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Checks if the given token is valid for the current state of the map.
		 * <p>
		 * A token becomes invalid if the map has been structurally modified since the token was obtained.
		 *
		 * @param token the token to validate.
		 * @return {@code true} if the token is valid, {@code false} otherwise.
		 */
		public boolean isValid( long token ) {
			return token != INVALID_TOKEN && version( token ) == _version;
		}
		
		/**
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() {
			return hasNullKey;
		}
		
		/**
		 * Checks if the entry associated with the given token has a key.
		 *
		 * @param token the token to check.
		 * @return {@code true} if the token is valid and associated with a key, {@code false} otherwise.
		 * @throws ConcurrentModificationException if the collection was modified while validating the token.
		 */
		public boolean hasKey( long token ) {
			if( isValid( token ) ) return index( token ) < _count || hasNullKey;
			throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
		}
		
		/**
		 * Checks if the entry associated with the given token has a value (not null in the context of null-value support).
		 *
		 * @param token the token to check.
		 * @return {@code true} if the entry has a value, {@code false} if it represents a null value.
		 * @throws ConcurrentModificationException if the collection was modified while validating the token.
		 */
		public boolean hasValue( long token ) {
			if( isValid( token ) )
				return index( token ) == _count ?
						nullKeyHasValue :
						values.hasValue( index( token ) );
			throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
		}
		
		/**
		 * Returns the key associated with the specified token.
		 *
		 * @param token the token for which to retrieve the key.
		 * @return the key associated with the token.
		 * @throws ConcurrentModificationException if the collection was modified while validating the token.
		 */
		public short key( long token ) {
			if( isValid( token ) ) return (short) ( keys[ index( token ) ] );
			throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
		}
		
		/**
		 * Returns the value associated with the specified token.
		 * <p>
		 * If the entry represents a null value (in null-value context), returns 0.
		 *
		 * @param token the token for which to retrieve the value.
		 * @return the value associated with the token, or 0 if the value is null in null-value context.
		 * @throws ConcurrentModificationException if the collection was modified while validating the token.
		 */
		public char value( long token ) {
			if( isValid( token ) )
				return (char)( index( token ) == _count ?
						nullKeyValue :
						values.get( index( token ) ) );
			throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
		}
		
		/**
		 * Computes an order-independent hash code for this map.
		 * <p>
		 * This hash code is based on the key-value pairs in the map and is not affected by the order of entries.
		 *
		 * @return the hash code of this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
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
		
		/**
		 * Seed value for hash code calculation.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map to the specified object for equality.
		 * <p>
		 * Returns {@code true} if the given object is also an {@code IntIntNullMap.R} and the two maps represent the same mappings.
		 *
		 * @param obj the object to be compared for equality with this map.
		 * @return {@code true} if the specified object is equal to this map, {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Compares this map to another {@code R} instance for equality.
		 *
		 * @param other the other map to compare with.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == null || hasNullKey != other.hasNullKey ||
			    ( hasNullKey && ( nullKeyHasValue != other.nullKeyHasValue || nullKeyValue != other.nullKeyValue ) ) ||
			    size() != other.size() )
				return false;
			
			for( long token = token(); index( token ) < _count; token = token( token ) ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || hasValue( token ) != other.hasValue( t ) ||
				    ( hasValue( token ) && value( token ) != other.value( t ) ) )
					return false;
			}
			return true;
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code IntIntNullMap.R} instance.
		 * <p>
		 * The cloned map will contain the same mappings as the original map.
		 *
		 * @return a clone of this map instance.
		 */
		@Override
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
		
		/**
		 * Returns a string representation of this map in JSON format.
		 *
		 * @return a JSON string representing this map.
		 */
		@Override
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Returns a JSON string representation of this map.
		 *
		 * @return a JSON string representing this map.
		 */
		@Override
		public String toJSON() {
			return JsonWriter.Source.super.toJSON();
		}
		
		/**
		 * Writes this map to a {@code JsonWriter}.
		 *
		 * @param json the {@code JsonWriter} to write to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey )
				json.name().value( nullKeyHasValue ?
						                   nullKeyValue :
						                   null );
			
			for( long token = token(); index( token ) < _count; token = token( token ) )
			     json
					     .name( String.valueOf( keys[ index( token ) ] ) )
					     .value( hasValue( token ) ?
							             value( token ) :
							             null );
			
			json.exitObject();
		}
		
		/**
		 * Calculates the bucket index for a given hash.
		 *
		 * @param hash the hash value.
		 * @return the bucket index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an index and the current version.
		 *
		 * @param index the index of the entry.
		 * @return the created token.
		 */
		protected long token( int index ) {
			return ( long ) _version << VERSION_SHIFT | index & INDEX_MASK;
		}
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token the token.
		 * @return the index extracted from the token.
		 */
		protected int index( long token ) {
			return ( int ) ( token & INDEX_MASK );
		}
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token the token.
		 * @return the version extracted from the token.
		 */
		protected int version( long token ) {
			return ( int ) ( token >>> VERSION_SHIFT );
		}
	}
	
	/**
	 * {@code RW} is a read-write implementation of the {@code IntIntNullMap} interface, extending the read-only functionality provided by {@link R}.
	 * <p>
	 * This class provides methods to modify the map, such as inserting, updating, and removing key-value pairs.
	 * It inherits the read operations from {@code R} and adds functionalities for structural modifications,
	 * including resizing and clearing the map.
	 */
	class RW extends R {
		
		
		/**
		 * Constructs an empty {@code RW} map with the default initial capacity.
		 */
		public RW() {
			this( 0 );
		}
		
		/**
		 * Constructs an empty {@code RW} map with the specified initial capacity.
		 *
		 * @param capacity the initial capacity of the map.
		 */
		public RW( int capacity ) {
			values = new CharNullList.RW( 0 );
			if( capacity > 0 ) initialize( capacity );
		}
		/**
		 * {@code flatStrategyThreshold} is the threshold that determines when to switch values collection from
		 * <b>Compressed Strategy</b> to <b>Flat Strategy</b>. The switch is typically based on the
		 * density of null values. The default value is 512.
		 */
		public void flatStrategyThreshold( int interleavedBits ) {
			values. flatStrategyThreshold = interleavedBits;
		}
		/**
		 * Initializes the internal arrays of the map with a given capacity.
		 * <p>
		 * The capacity is adjusted to the nearest prime number greater than or equal to the given capacity.
		 *
		 * @param capacity the desired capacity.
		 * @return the initialized capacity (which is a prime number).
		 */
		private int initialize( int capacity ) {
			capacity  = Array.prime( capacity );
			_buckets  = new int[ capacity ];
			nexts     = new int[ capacity ];
			keys      = new short[ capacity ];
			values    = new CharNullList.RW( capacity );
			_freeList = -1;
			return capacity;
		}
		
		/**
		 * Associates the specified value with the specified key in this map (boxed Integer key and boxed Integer value).
		 * <p>
		 * If the map previously contained a mapping for the key, the old value is replaced by the specified value.
		 *
		 * @param key   the key with which the specified value is to be associated.
		 * @param value the value to be associated with the specified key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 */
		public boolean put(  Short     key,  Character value ) {
			return key == null ?
					tryInsert( value == null ?
							           0 :
							           value, value != null, 1 ) :
					tryInsert( key.shortValue     (), value == null ?
							0 :
							value, value != null, 1 );
		}
		
		/**
		 * Associates the specified value with the specified key in this map (primitive int key and primitive int value).
		 * <p>
		 * If the map previously contained a mapping for the key, the old value is replaced by the specified value.
		 *
		 * @param key   the key with which the specified value is to be associated.
		 * @param value the value to be associated with the specified key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 */
		public boolean put( short key, char value ) { return tryInsert( key, value, true, 1 ); }
		
		/**
		 * Associates the specified value with the null key in this map (boxed Integer value).
		 * <p>
		 * If the map previously contained a mapping for the null key, the old value is replaced by the specified value.
		 *
		 * @param value the value to be associated with the null key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 */
		public boolean put(  Character value ) {
			return tryInsert( value == null ?
					                  0 :
					                  value, value != null, 1 );
		}
		
		/**
		 * Associates the specified value with the specified key only if the key is not already present in this map (primitive int key and primitive int value).
		 * <p>
		 * This is equivalent to put if absent.
		 *
		 * @param key   the key with which the specified value is to be associated.
		 * @param value the value to be associated with the specified key.
		 * @return {@code true} if a new mapping was added, {@code false} if the key was already present.
		 */
		public boolean putNotExist( short key, char value ) {
			return tryInsert( key, value, true, 2 );
		}
		
		/**
		 * Associates the specified value with the specified key only if the key is not already present in this map (boxed Integer key and boxed Integer value).
		 * <p>
		 * This is equivalent to put if absent.
		 *
		 * @param key   the key with which the specified value is to be associated.
		 * @param value the value to be associated with the specified key.
		 * @return {@code true} if a new mapping was added, {@code false} if the key was already present.
		 */
		public boolean putNotExist(  Short     key,  Character value ) {
			return key == null ?
					tryInsert( value == null ?
							           0 :
							           value, value != null, 2 ) :
					tryInsert( key, value == null ?
							0 :
							value, value != null, 2 );
		}
		
		/**
		 * Associates the specified value with the null key only if the null key is not already present in this map (boxed Integer value).
		 * <p>
		 * This is equivalent to put if absent for the null key.
		 *
		 * @param value the value to be associated with the null key.
		 * @return {@code true} if a new mapping was added, {@code false} if the null key was already present.
		 */
		public boolean putNotExist(  Character value ) {
			return tryInsert( value == null ?
					                  0 :
					                  value, value != null, 2 );
		}
		
		/**
		 * Associates the specified value with the specified key, attempting to add only and throwing an exception if the key already exists (primitive int key and primitive int value).
		 *
		 * @param key   the key with which the specified value is to be associated.
		 * @param value the value to be associated with the specified key.
		 * @throws IllegalArgumentException if a mapping for the specified key already exists in this map.
		 */
		public void putTry( short key, char value ) {
			tryInsert( key, value, true, 0 );
		}
		
		/**
		 * Associates the specified value with the specified key, attempting to add only and throwing an exception if the key already exists (boxed Integer key and boxed Integer value).
		 *
		 * @param key   the key with which the specified value is to be associated.
		 * @param value the value to be associated with the specified key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 * @throws IllegalArgumentException if a mapping for the specified key already exists in this map.
		 */
		public boolean putTry(  Short     key,  Character value ) {
			return key == null ?
					tryInsert( value == null ?
							           0 :
							           value, value != null, 0 ) :
					tryInsert( key, value == null ?
							0 :
							value, value != null, 0 );
		}
		
		/**
		 * Associates the specified value with the null key, attempting to add only and throwing an exception if the null key already exists (boxed Integer value).
		 *
		 * @param value the value to be associated with the null key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 * @throws IllegalArgumentException if a mapping for the null key already exists in this map.
		 */
		public boolean putTry(  Character value ) {
			return tryInsert( value == null ?
					                  0 :
					                  value, value != null, 0 );
		}
		
		/**
		 * Core insertion logic for primitive integer keys.
		 * <p>
		 * Handles insertion, update, and "put if not exists" operations based on the behavior parameter.
		 *
		 * @param key      the key to insert or update.
		 * @param value    the value to associate with the key.
		 * @param hasValue {@code true} if the value is not null, {@code false} otherwise.
		 * @param behavior 0=throw if exists, 1=put (update/insert), 2=put if not exists.
		 * @return {@code true} if the map was structurally modified, {@code false} otherwise (depending on behavior).
		 * @throws IllegalArgumentException        if behavior is 0 and the key already exists.
		 * @throws ConcurrentModificationException if concurrent operations are detected.
		 */
		private boolean tryInsert( short key, char value, boolean hasValue, int behavior ) {
			if( _buckets == null ) initialize( 0 );
			int[] _nexts         = nexts;
			int           hash           = Array.hash( key );
			int           collisionCount = 0;
			int           bucketIndex    = bucketIndex( hash );
			int           bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) {
					switch( behavior ) {
						case 1:
							if( hasValue ) values.set1( next, value );
							else values.set1( next, null );
							_version++;
							return true;
						case 0:
							throw new IllegalArgumentException( "An item with the same key has already been added. Key: " + key );
						default:
							return false;
					}
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
					bucket = ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1;
				}
				index = _count++;
			}
			
			nexts[ index ] = ( int ) bucket;
			keys[ index ]  = ( short ) key;
			
			if( hasValue ) values.set1( index, value );
			else values.set1( index, null );
			
			_buckets[ bucketIndex ] =   index + 1;
			_version++;
			
			return true;
		}
		
		/**
		 * Handles insertion and update logic for the null key.
		 *
		 * @param value    the value to associate with the null key.
		 * @param hasValue {@code true} if the value is not null, {@code false} otherwise.
		 * @param behavior 0=throw if exists, 1=put (update/insert), 2=put if not exists.
		 * @return {@code true} if the map was structurally modified, {@code false} otherwise (depending on behavior).
		 * @throws IllegalArgumentException if behavior is 0 and the null key already exists.
		 */
		private boolean tryInsert( char value, boolean hasValue, int behavior ) {
			if( hasNullKey ) {
				switch( behavior ) {
					case 1:
						break; // Update allowed.
					case 0:
						throw new IllegalArgumentException( "An item with the same key has already been added. Key: null" );
					default:
						return false;
				}
			}
			hasNullKey = true;
			if( ( nullKeyHasValue = hasValue ) ) nullKeyValue = ( char ) value;
			_version++;
			return true;
		}
		
		/**
		 * Removes the mapping for the specified key from this map if present (boxed Integer key).
		 *
		 * @param key key whose mapping is to be removed from the map.
		 * @return the token of the removed entry, or {@link #INVALID_TOKEN} if no mapping was found for the key.
		 */
		public long remove(  Short     key ) {
			return key == null ?
					remove() :
					remove( key.shortValue     () );
		}
		
		/**
		 * Removes the mapping for the null key from this map if present.
		 *
		 * @return the token of the removed entry, or {@link #INVALID_TOKEN} if no mapping was found for the null key.
		 */
		public long remove() {
			if( !hasNullKey ) return INVALID_TOKEN;
			hasNullKey      = false;
			nullKeyHasValue = false;
			_version++;
			return token( _count );
		}
		
		
		/**
		 * Removes the mapping for the specified key from this map if present (primitive int key).
		 * <p>
		 * If a non-null value is being removed and it's not the last non-null value in the values list,
		 * this method optimizes removal by swapping the entry with the last non-null entry to avoid shifting in the values list.
		 *
		 * @param key key whose mapping is to be removed from the map.
		 * @return the token of the removed entry, or {@link #INVALID_TOKEN} if no mapping was found for the key.
		 */
		public long remove( short key ) {
			// Handle edge cases: if map is uninitialized or empty, nothing to remove
			if( _buckets == null || _count == 0 )
				return INVALID_TOKEN; // Return invalid token indicating no removal
			
			// Compute hash and bucket index for the key to locate its chain
			int hash           = Array.hash( key );                // Hash the key using Array.hash
			int bucketIndex    = bucketIndex( hash );       // Map hash to bucket index
			int last           = -1;                             // Previous index in chain (-1 if 'i' is head)
			int i              = _buckets[ bucketIndex ] - 1;         // Head of chain (convert 1-based to 0-based)
			int collisionCount = 0;                    // Counter to detect infinite loops or concurrency issues
			
			// Traverse the linked list in the bucket to find the key
			while( -1 < i ) {
				int next = nexts[ i ];                   // Get next index in the chain
				if( keys[ i ] == key ) {                  // Found the key at index 'i'
					
					// Step 1: Unlink the entry at 'i' from its chain
					if( last < 0 )
						// If 'i' is the head, update bucket to point to the next entry
						_buckets[ bucketIndex ] =  ( next + 1 );
					
					else
						// Otherwise, link the previous entry to the next, bypassing 'i'
						nexts[ last ] = ( int ) next;
					
					// Step 2: Optimize removal if value is non-null and not the last non-null
					final int     lastNonNullValue = values.nulls.last1(); // Index of last non-null value
					final boolean hasValue         = values.hasValue( i );
					
					// Update free list head
					if( i != lastNonNullValue && hasValue ) {
						// Optimization applies: swap with last non-null entry
						// Step 2a: Copy key, next, and value from lastNonNullValue to i
						short   keyToMove          = keys[ lastNonNullValue ];
						int           BucketOf_KeyToMove = bucketIndex( Array.hash( keyToMove ) );
						int   _next              = nexts[ lastNonNullValue ];
						
						keys[ i ]  = keyToMove;                         // Copy the key to the entry being removed
						nexts[ i ] = _next;         // Copy the next to the entry being removed
						values.set1( i, values.get( lastNonNullValue ) ); // Copy the value to the entry being removed
						
						// Step 2b: Update the chain containing keyToMove to point to 'i'
						int prev = -1;                     // Previous index in keyToMove’s chain
						collisionCount = 0;// Reset collision counter
						
						// Start at chain head
						for( int current = _buckets[ BucketOf_KeyToMove ] - 1; current != lastNonNullValue; prev = current, current = nexts[ current ] )
							if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
						
						_buckets[ BucketOf_KeyToMove ] =  ( i + 1 );// If 'lastNonNullValue' the head, update bucket to the position of keyToMove
						
						if( -1 < prev ) nexts[ prev ] = _next;
						
						values.set1( lastNonNullValue, null );        // Clear value (O(1) since it’s last non-null)
						i = lastNonNullValue;
					}
					else if( hasValue ) values.set1( i, null );                       // Clear value (may shift if not last)
					
					nexts[ i ] = ( int ) ( StartOfFreeList - _freeList ); // Mark 'i' as free
					_freeList  = i;
					
					_freeCount++;       // Increment count of free entries
					_version++;         // Increment version for concurrency control
					return token( i );    // Return token for removed/overwritten entry
				}
				
				// Move to next entry in chain
				last = i;
				i    = next;
				if( collisionCount++ > nexts.length ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				
			}
			
			return INVALID_TOKEN; // Key not found
		}
		
		
		/**
		 * Removes all mappings from this map.
		 * <p>
		 * The map will be empty after this call.
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
		 * Ensures that the capacity of this map is at least equal to the specified minimum capacity.
		 * <p>
		 * If the current capacity is less than the specified capacity, the internal arrays are resized to a new capacity
		 * that is the smallest prime number greater than or equal to the specified capacity.
		 *
		 * @param capacity the desired minimum capacity.
		 * @return the new capacity after ensuring the minimum capacity.
		 * @throws IllegalArgumentException if the specified capacity is negative.
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
		 * Reduces the capacity of this map to be the map's current size.
		 * <p>
		 * If the map is empty, the capacity is trimmed to the default initial capacity.
		 */
		public void trim() {
			trim( size() );
		}
		
		/**
		 * Reduces the capacity of this map to be at least as great as the
		 * given capacity.
		 * <p>
		 * If the given capacity is less than the current size of the map, the capacity is trimmed to the current size.
		 *
		 * @param capacity the desired capacity to trim to.
		 * @throws IllegalArgumentException if the specified capacity is less than the current size of the map.
		 */
		public void trim( int capacity ) {
			if( capacity < size() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			int newSize         = Array.prime( capacity );
			if( currentCapacity <= newSize ) return;
			
			int[]          old_next   = nexts;
			short[]          old_keys   = keys;
			CharNullList.RW old_values = values;
			int                    old_count  = _count;
			_version++;
			initialize( newSize );
			copy( old_next, old_keys, old_values, old_count );
		}
		
		/**
		 * Resizes the internal hash table to a new capacity.
		 * <p>
		 * Rehashes all existing keys to distribute them in the new buckets.
		 *
		 * @param newSize the new capacity (must be a prime number).
		 */
		private void resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Short    .BYTES * 8 );
			_version++;
			int[] new_next = Arrays.copyOf( nexts, newSize );
			short[] new_keys = Arrays.copyOf( keys, newSize );
			final int     count    = _count;
			
			_buckets = new int[ newSize ];
			for( int i = 0; i < count; i++ ) {
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = ( int ) ( _buckets[ bucketIndex ] - 1 );
					_buckets[ bucketIndex ] =  ( i + 1 );
				}
			}
			
			nexts = new_next;
			keys  = new_keys;
		}
		
		/**
		 * Copies entries from old arrays to new arrays during trimming or resizing.
		 *
		 * @param old_next   the old next indices array.
		 * @param old_keys   the old keys array.
		 * @param old_values the old values list.
		 * @param old_count  the number of entries in the old arrays.
		 */
		private void copy( int[] old_next, short[] old_keys, CharNullList.RW old_values, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue;
				nexts[ new_count ] = old_next[ i ];
				keys[ new_count ]  = old_keys[ i ];
				if( old_values.hasValue( i ) ) values.set1( new_count, old_values.get( i ) );
				else values.set1( new_count, null );
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) );
				nexts[ new_count ]      = ( int ) ( _buckets[ bucketIndex ] - 1 );
				_buckets[ bucketIndex ] =  ( new_count + 1 );
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code IntIntNullMap.RW} instance.
		 * <p>
		 * The cloned map will contain the same mappings as the original map.
		 *
		 * @return a clone of this map instance.
		 */
		@Override
		public RW clone() {
			return ( RW ) super.clone();
		}
	}
}