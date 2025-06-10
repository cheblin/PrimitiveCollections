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

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.function.Function;

/**
 * <p>A high-performance, generic map implementation that stores object keys and primitive {@code int} values,
 * with explicit support for {@code null} values. It is built on a hash table that uses a specialized
 * collision handling strategy to optimize memory layout and performance.</p>
 *
 * <p>This map divides its internal storage into two regions:
 * <ul>
 *     <li><b>Hi Region:</b> Stores entries that do not cause hash collisions upon insertion.
 *     It grows from the end of the internal arrays inward.</li>
 *     <li><b>Lo Region:</b> Stores entries that cause hash collisions. These entries form collision chains
 *     using a separate {@code links} array. This region grows from the start of the arrays forward.</li>
 * </ul>
 * This design aims to keep non-colliding entries compact and separate from chained entries, improving cache efficiency.
 * </p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *     <li><b>Generic Keys:</b> Supports any object type as keys, with customizable equality and hashing via {@link Array.EqualHashOf}.</li>
 *     <li><b>Primitive Int Values with Null Support:</b> Stores primitive {@code int} values efficiently, using an {@link IntNullList} to explicitly track which entries are {@code null}.</li>
 *     <li><b>Optimized Collision Handling:</b> Uses a hybrid approach of open addressing (for the first entry in a bucket) and separate chaining for subsequent collisions, managed through distinct "lo" and "hi" memory regions.</li>
 *     <li><b>Dynamic Resizing:</b> Automatically grows capacity and can be manually trimmed. It supports collision-driven rehashing with a custom function to mitigate poor hash distributions.</li>
 *     <li><b>Null Key Support:</b> Allows a single {@code null} key with dedicated methods for access.</li>
 *     <li><b>Token-Based Iteration:</b> Provides a safe and efficient iteration mechanism using tokens, which are stable handles to map entries. An {@link R#unsafe_token(int) unsafe} method is also available for high-performance iteration when no concurrent modifications are expected.</li>
 *     <li><b>Memory Management:</b> Includes methods to {@link RW#trim()} and {@link RW#ensureCapacity(int)} to control memory usage.</li>
 *     <li><b>Cloneable:</b> Supports creating a shallow copy of the map structure.</li>
 * </ul>
 */
public interface ObjectUIntNullMap {
	
	abstract class R< K > implements JsonWriter.Source, Cloneable {
		protected boolean                hasNullKey;
		protected boolean                nullKeyHasValue;
		protected int            nullKeyValue;
		protected int[]                  _buckets;
		protected int[]                  hash; // Replaced hash_nexts
		protected int[]                  links; // Replaced hash_nexts
		protected K[]                    keys;
		protected UIntNullList.RW values;
		
		public int valuesFlatStrategyThreshold() { return values.flatStrategyThreshold(); }
		
		// Replaced _count, _freeList, _freeCount
		protected int _lo_Size;
		protected int _hi_Size;
		
		protected int                    _version;
		protected Array.EqualHashOf< K > equal_hash_K;
		
		protected static final int NULL_KEY_INDEX = 0x7FFF_FFFF;
		
		protected static final int  VERSION_SHIFT = 32;
		protected static final long INVALID_TOKEN = -1L;
		// HashCollisionThreshold is only used in RW, moving it there
		
		/**
		 * Returns the number of non-null key-value mappings in this map.
		 * This count excludes the null key, if present.
		 *
		 * @return The number of non-null key entries.
		 */
		protected int _count() { return _lo_Size + _hi_Size; }
		
		/**
		 * Returns {@code true} if this map contains no key-value mappings.
		 *
		 * @return {@code true} if this map is empty.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the total number of key-value mappings in this map, including the null key if present.
		 *
		 * @return The total number of entries in the map.
		 */
		public int size() {
			return _count() + ( hasNullKey ?
			                    1 :
			                    0 );
		}
		
		/**
		 * Returns the total number of key-value mappings in this map. Alias for {@link #size()}.
		 *
		 * @return The total number of entries in the map.
		 */
		public int count() { return size(); }
		
		/**
		 * Returns the current capacity of the internal arrays.
		 *
		 * @return The current capacity.
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
		public boolean containsKey( K key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Returns {@code true} if this map maps one or more keys to the specified value.
		 * This operation may be slow as it requires a full scan of all values.
		 *
		 * @param value The value whose presence in this map is to be tested. Can be {@code null}.
		 * @return {@code true} if one or more keys are mapped to the specified value.
		 */
		public boolean containsValue(  Long      value ) {
			return
					value == null ?
					hasNullKey && !nullKeyHasValue ||
					values.nextNullIndex( -1 ) != -1 :
					hasNullKey && nullKeyHasValue && nullKeyValue == value ||
					values.indexOf( value ) != -1;
		}
		
		
		/**
		 * Returns {@code true} if this map maps one or more keys to the specified primitive value.
		 * This operation may be slow as it requires a full scan of all values.
		 *
		 * @param value The primitive {@code int} value whose presence in this map is to be tested.
		 * @return {@code true} if one or more keys are mapped to the specified value.
		 */
		public boolean containsValue( long value ) { return tokenOfValue( value ) != -1; }
		
		/**
		 * Returns the token for the first occurrence of the specified value.
		 *
		 * @param value The value to find. Can be {@code null}.
		 * @return A token for the entry, or {@link #INVALID_TOKEN} if the value is not found.
		 */
		public long tokenOfValue(  Long      value ) {
			if( value != null )
				return tokenOfValue( value.longValue     () );
			
			if( hasNullKey && !nullKeyHasValue ) return token( NULL_KEY_INDEX );
			else
				for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) if( !hasValue( token( t ) ) ) return t; // Changed to token(t) for hasValue
			
			return -1;
		}
		
		/**
		 * Returns the token for the first occurrence of the specified primitive value.
		 *
		 * @param value The primitive {@code int} value to find.
		 * @return A token for the entry, or {@link #INVALID_TOKEN} if the value is not found.
		 */
		public long tokenOfValue( long value ) {
			if( hasNullKey && nullKeyValue == value ) return token( NULL_KEY_INDEX );
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) if( hasValue( token( t ) ) && value( token( t ) ) == value ) return t; // Changed to token(t) for hasValue/value
			return -1;
		}
		
		/**
		 * Returns {@code true} if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if the null key is present.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns {@code true} if the null key exists and has a non-null value.
		 *
		 * @return {@code true} if the null key has a non-null value.
		 */
		public boolean nullKeyHasValue() { return nullKeyHasValue; }
		
		/**
		 * Returns the value to which the null key is mapped.
		 * <p><b>Precondition:</b> This method should be called only if {@link #nullKeyHasValue()} is {@code true}.</p>
		 *
		 * @return The value of the null key entry.
		 */
		public long nullKeyValue() { return (0xFFFFFFFFL &  nullKeyValue); }
		
		/**
		 * Returns a stable token for the specified key, or {@link #INVALID_TOKEN} if the key is not in the map.
		 * A token is a long value that encodes both the entry's index and a version snapshot,
		 * ensuring safe access even if the map is modified.
		 *
		 * @param key The key to find.
		* @return A valid token if the key is found, otherwise {@link #INVALID_TOKEN}.
		 */
		public long tokenOf( K key ) {
			if( key == null ) return hasNullKey ?
			                         token( NULL_KEY_INDEX ) :
			                         INVALID_TOKEN;
			
			if( _buckets == null || _count() == 0 ) return INVALID_TOKEN; // Uses _count()
			
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
				if( _lo_Size + 1 < ++collisions ) // Collision count check for lo_Size region
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		/**
		 * Returns the first token for iterating through the map.
		 * The iteration order is not guaranteed. It will start with non-null keys and end with the null key, if present.
		 *
		 * @return The first token, or {@link #INVALID_TOKEN} if the map is empty.
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
		 * Returns the next token for iteration, given the current token.
		 *
		 * @param token The current valid token.
		 * @return The next token, or {@link #INVALID_TOKEN} if this is the last entry.
		 * @throws IllegalArgumentException if the token is invalid.
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
		 * Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #nullKeyHasValue()} and {@link #nullKeyValue()} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 * <p>
		 * Iteration order: First, entries in the {@code lo Region} (0 to {@code _lo_Size-1}),
		 * then entries in the {@code hi Region} ({@code keys.length - _hi_Size} to {@code keys.length-1}).
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #nullKeyHasValue() To check if the null key has a value.
		 * @see #nullKeyValue() To get the null key’s value.
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
		 * Checks if the given token corresponds to the null key.
		 *
		 * @param token The token to check.
		 * @return {@code true} if the token represents the null key.
		 */
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the key corresponding to the given token.
		 *
		 * @param token A valid token.
		 * @return The key for the entry. Returns {@code null} if the token represents the null key.
		 */
		public K key( long token ) {
			return isKeyNull( token ) ?
			       null :
			       keys[ index( token ) ];
		}
		
		/**
		 * Checks if the entry for the given token has a non-null value.
		 *
		 * @param token A valid token.
		 * @return {@code true} if the entry has a non-null value.
		 */
		public boolean hasValue( long token ) {
			return index( token ) == NULL_KEY_INDEX ?
			       nullKeyHasValue :
			       values.hasValue( index( token ) );
		}
		
		/**
		 * Returns the value for the given token.
		 * <p>
		 * <b>Precondition:</b> This method should only be called if {@link #hasValue(long)} returns {@code true} for the given token.
		 * Calling it for an entry with a {@code null} value will result in undefined behavior.
		 * </p>
		 *
		 * @param token A valid token for an entry with a non-null value.
		 * @return The primitive {@code int} value for the entry.
		 */
		public long value( long token ) {
			return (0xFFFFFFFFL &  ( index( token ) == NULL_KEY_INDEX ?
			                    nullKeyValue :
			                    values.get( index( token ) ) ));
		}
		
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( key( token ) ) );
				h = Array.mix( h, hasValue( token( token ) ) ?
				                  // Pass token(token)
				                  Array.hash( value( token( token ) ) ) :
				                  // Pass token(token)
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
		 * Compares the specified map with this map for equality.
		 * Two maps are equal if they have the same size and contain the same key-value mappings.
		 *
		 * @param other The map to be compared for equality with this map.
		 * @return {@code true} if the specified map is equal to this map.
		 */
		public boolean equals( R< K > other ) {
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    hasNullKey && ( nullKeyHasValue != other.nullKeyHasValue || nullKeyHasValue && nullKeyValue != other.nullKeyValue ) ||
			    size() != other.size() ) return false;
			
			long t;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || hasValue( token( token ) ) != other.hasValue( t ) || // Pass token(token)
				    hasValue( token( token ) ) && value( token( token ) ) != other.value( t ) ) // Pass token(token)
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
				         .value( hasValue( token( token ) ) ?
				                 // Pass token(token)
				                 value( token( token ) ) :
				                 // Pass token(token)
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
				         .name( "Value" ).value( hasValue( token( token ) ) ?
				                                 // Pass token(token)
				                                 value( token( token ) ) :
				                                 // Pass token(token)
				                                 null )
				         .exitObject();
				json.exitArray();
			}
		}
		
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		// Removed hash(long) and next(long) as hash_nexts is gone
		
		protected long token( int index )   { return ( ( long ) _version << VERSION_SHIFT ) | index; }
		
		protected int index( long token )   { return ( int ) ( token ); }
		
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	class RW< K > extends R< K > {
		private static final int                                                        HashCollisionThreshold = 100; // Moved from R
		/**
		 * A function that, when provided, can generate a new hashing strategy for keys.
		 * This is invoked during a resize operation triggered by a high number of hash collisions (see {@link #HashCollisionThreshold}),
		 * allowing the map to adapt to problematic key distributions (e.g., by using a different seed for String hashing).
		 * If null, the hashing strategy is never changed.
		 */
		public               Function< Array.EqualHashOf< K >, Array.EqualHashOf< K > > forceNewHashCodes      = null;
		
		/**
		 * Constructs an empty map with an initial capacity of 0.
		 * The map will be initialized on the first insertion.
		 *
		 * @param clazzK The class of the keys, used to select a default {@link Array.EqualHashOf}.
		 */
		public RW( Class< K > clazzK ) { this( clazzK, 0 ); }
		
		/**
		 * Constructs an empty map with the specified initial capacity.
		 *
		 * @param clazzK   The class of the keys, used to select a default {@link Array.EqualHashOf}.
		 * @param capacity The initial capacity. The actual capacity will be the next prime number greater than or equal to this value.
		 */
		public RW( Class< K > clazzK, int capacity ) { this( Array.get( clazzK ), capacity ); }
		
		/**
		 * Constructs an empty map with a custom key handler and an initial capacity of 0.
		 * The map will be initialized on the first insertion.
		 *
		 * @param equal_hash_K The custom provider for key hashing and equality checks.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K ) { this( equal_hash_K, 0 ); }
		
		/**
		 * Constructs an empty map with a custom key handler and the specified initial capacity.
		 *
		 * @param equal_hash_K The custom provider for key hashing and equality checks.
		 * @param capacity     The initial capacity. The actual capacity will be the next prime number greater than or equal to this value.
		 */
		public RW( Array.EqualHashOf< K > equal_hash_K, int capacity ) {
			this.equal_hash_K = equal_hash_K;
			values            = new UIntNullList.RW( 0 ); // Initial capacity for values. Will be resized in initialize.
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Sets the threshold that determines when the underlying {@link IntNullList}
		 * should switch its storage strategy to optimize for dense or sparse null values.
		 *
		 * @param interleavedBits The threshold value.
		 */
		public void valuesFlatStrategyThreshold( int interleavedBits ) { values.flatStrategyThreshold = interleavedBits; }
		
		/**
		 * Associates the specified value with the specified key in this map.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 *
		 * @param key   The key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key. Can be {@code null}.
		 * @return {@code true} if a new key-value pair was inserted, {@code false} if an existing key's value was updated.
		 */
		public boolean put( K key,  Long      value ) {
			return value == null ?
			       put( key, (long ) 0, false ) :
			       put( key, value, true );
		}
		
		/**
		 * Removes all of the mappings from this map. The map will be empty after this call returns.
		 * This operation does not shrink the underlying arrays; use {@link #trim()} for that.
		 */
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count() == 0 ) return; // Uses _count() for current entries
			Arrays.fill( _buckets, 0 );
			Arrays.fill( keys, null ); // Clear object references
			// hash, links, values arrays are implicitly cleared as _lo_Size and _hi_Size are reset.
			_lo_Size = 0;
			_hi_Size = 0;
			values.clear(); // Clear the underlying IntNullList
		}
		
		/**
		 * Removes the mapping for a key from this map if it is present.
		 *
		 * @param key The key whose mapping is to be removed from the map.
		 * @return {@code true} if an entry was removed, {@code false} if no mapping was found for the key.
		 */
		public boolean remove( K key ) {
			if( key == null ) { // Handle null key removal
				if( !hasNullKey ) return false; // Null key not present
				hasNullKey      = false;
				nullKeyHasValue = false; // Null key no longer has a value
				_version++;                             // Increment version
				return true;
			}
			
			if( _count() == 0 ) return false; // Map is empty
			
			int hash              = equal_hash_K.hashCode( key );
			int removeBucketIndex = bucketIndex( hash );
			int removeIndex       = _buckets[ removeBucketIndex ] - 1; // 0-based index from bucket
			if( removeIndex < 0 ) return false; // Key not in this bucket
			
			// Case 1: Entry to be removed is in the hi Region (cannot be part of a chain from _lo_Size)
			if( _lo_Size <= removeIndex ) {
				if( this.hash[ removeIndex ] != hash || !equal_hash_K.equals( keys[ removeIndex ], key ) ) return false;
				
				// Move the last element of hi region to the removed slot
				move( keys.length - _hi_Size, removeIndex );
				
				_hi_Size--; // Decrement hi_Size
				_buckets[ removeBucketIndex ] = 0; // Clear the bucket reference (it was the only one)
				_version++;
				return true;
			}
			
			// Case 2: Entry to be removed is in the lo Region
			// Finding the key in the chain and updating links
			int next = links[ removeIndex ]; // Get the link from the first element in the chain
			
			if( this.hash[ removeIndex ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) {
				// Key found at the head of the chain (removeIndex)
				_buckets[ removeBucketIndex ] = ( next + 1 ); // Update bucket to point to the next element
			}
			else {
				// Key is NOT at the head of the chain. Traverse.
				int last = removeIndex; // 'last' tracks the node *before* 'removeIndex' (the element we are currently checking)
				
				// This block is from ObjectBitsMap, it handles the 2nd element in the chain.
				// 'removeIndex' is reassigned here to be the 'next' element.
				if( this.hash[ removeIndex = next ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) { // The key is found at the 'SecondNode'
					if( removeIndex < _lo_Size ) { // 'SecondNode' is in 'lo Region', relink 'last' to bypass 'SecondNode'
						links[ last ] = links[ removeIndex ];
					}
					else {  // 'SecondNode' is in the hi Region (it's a terminal node)
						// This block performs a specific compaction for hi-region terminal nodes.
						keys[ removeIndex ]      = keys[ last ]; // Copies `keys[last]` to `keys[removeIndex]`
						this.hash[ removeIndex ] = this.hash[ last ];
						// Correctly copy value and hasValue status using IntNullList methods
						if( values.hasValue( last ) ) values.set1( removeIndex, values.get( last ) );
						else values.set1( removeIndex, null );
						
						// 'removeBucketIndex' is the hash bucket for the original 'key'.
						// By pointing it to 'removeIndex' (which now contains data from 'last'),
						// we make 'last' (now at 'removeIndex') the new sole head of the bucket.
						_buckets[ removeBucketIndex ] = ( removeIndex + 1 );
						removeIndex                   = last; // Mark original 'last' (lo-region entry) for removal/compaction
					}
				}
				else if( _lo_Size <= removeIndex ) { // If 'removeIndex' (now the second element) is in hi-region and didn't match
					return false; // Key not found in this chain
				}
				else {
					// Loop for 3rd+ elements in the chain
					for( int collisionCount = 0; ; ) {
						int prev_for_loop = last; // This 'prev_for_loop' is the element *before* 'last' in this inner loop context
						
						// Advance 'last' and 'removeIndex' (current element being checked)
						if( this.hash[ removeIndex = links[ last = removeIndex ] ] == hash && equal_hash_K.equals( keys[ removeIndex ], key ) ) {
							if( removeIndex < _lo_Size ) { // Found in lo-region: relink 'last' to bypass 'removeIndex'
								links[ last ] = links[ removeIndex ];
							}
							else { // Found in hi-region (terminal node): Special compaction
								// Copy data from 'removeIndex' (hi-region node) to 'last' (lo-region node)
								keys[ removeIndex ]      = keys[ last ];
								this.hash[ removeIndex ] = this.hash[ last ];
								// Correctly copy value and hasValue status
								if( values.hasValue( last ) ) values.set1( removeIndex, values.get( last ) );
								else values.set1( removeIndex, null );
								
								// Relink 'prev_for_loop' (the element before 'last') to 'removeIndex'
								links[ prev_for_loop ] = removeIndex;
								                         removeIndex = last; // Mark original 'last' (lo-region entry) for removal/compaction
							}
							break; // Key found and handled, break from loop.
						}
						if( _lo_Size <= removeIndex ) return false; // Reached hi-region terminal node, key not found
						// Safeguard against excessively long or circular chains (corrupt state)
						if( _lo_Size + 1 < ++collisionCount )
							throw new ConcurrentModificationException( "Concurrent operations not supported." );
					}
				}
			}
			
			// At this point, 'removeIndex' holds the final index of the element to be removed.
			// This 'removeIndex' might have been reassigned multiple times within the if-else blocks.
			move( _lo_Size - 1, removeIndex ); // Move the last lo-region element to the spot of the removed entry
			_lo_Size--; // Decrement lo-region size
			_version++; // Structural modification
			return true;
		}
		
		/**
		 * Relocates an entry's data from a source index to a destination index and updates
		 * all internal pointers (from buckets or collision chain links) to reflect the move.
		 * This is a key internal operation for compacting storage after a removal.
		 *
		 * @param src The source index of the entry to move.
		 * @param dst The destination index.
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
			
			// If src was in lo region, copy its link.
			// THIS IS THE CORRECT LOGIC FROM ObjectBitsMap, NO ELSE CLAUSE
			if( src < _lo_Size ) links[ dst ] = links[ src ];
			
			this.hash[ dst ] = this.hash[ src ]; // Copy hash
			keys[ dst ]      = keys[ src ];   // Copy key
			keys[ src ]      = null;          // Clear source slot for memory management and to prevent stale references
			
			// Correctly copy value and hasValue status using IntNullList methods
			if( values.hasValue( src ) ) values.set1( dst, values.get( src ) );
			else values.set1( dst, null );
		}
		
		/**
		 * Ensures that the map has at least the specified capacity. If the current capacity is smaller,
		 * the map is resized, and all existing entries are rehashed.
		 *
		 * @param capacity The desired minimum capacity.
		 * @return The new capacity of the map.
		 * @throws IllegalArgumentException if capacity is negative.
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
		 * Trims the capacity of the map to be its current size, minimizing memory usage.
		 * This operation rehashes all entries.
		 */
		public void trim() { trim( _count() ); } // Uses _count()
		
		/**
		 * Trims the capacity of the map to the specified value, if it's smaller than the current capacity.
		 * The final capacity will be at least the current number of entries. This operation rehashes all entries.
		 *
		 * @param capacity The desired maximum capacity.
		 * @throws IllegalArgumentException if capacity is less than the current number of entries.
		 */
		public void trim( int capacity ) {
			if( capacity < _count() ) throw new IllegalArgumentException( "capacity is less than Count." );
			int currentCapacity = length();
			capacity = Array.prime( Math.max( capacity, _count() ) ); // Ensure capacity is at least current count and prime
			if( currentCapacity <= capacity ) return; // No need to trim if current capacity is already smaller or equal
			
			resize( capacity, false ); // Resize to the new smaller size
		}
		
		/**
		 * Associates the specified key with the specified value. This is the core insertion method.
		 * If the map previously contained a mapping for the key, the old value is replaced.
		 * <p>
		 * If a high number of collisions is detected (exceeding {@link #HashCollisionThreshold}) and a
		 * {@code forceNewHashCodes} function is provided, this method may trigger a full rehash of the map
		 * with a new hashing strategy to improve performance.
		 *
		 * @param key      The key to insert or update.
		 * @param value    The primitive {@code int} value to associate with the key.
		 * @param hasValue {@code true} if the value is a valid integer, {@code false} if it represents a {@code null} value.
		 * @return {@code true} if a new key-value pair was inserted, {@code false} if an existing key's value was updated.
		 */
		public boolean put( K key, long value, boolean hasValue ) {
			if( key == null ) {
				boolean b = !hasNullKey;
				
				hasNullKey      = true;
				nullKeyHasValue = hasValue;
				nullKeyValue    = ( int ) value;
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
						
						if( hasValue ) values.set1( index, value );
						else values.set1( index, null );
						_version++;
						return false; // Key was not new
					}
				}
				else  // Existing entry is in {@code lo Region} (collision chain)
				{
					int collisions = 0;
					for( int next = index; ; ) {
						if( this.hash[ next ] == hash && equal_hash_K.equals( keys[ next ], key ) ) {
							if( hasValue ) values.set1( next, value );
							else values.set1( next, null );
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
			
			
			this.hash[ dst_index ] = hash; // Store hash code
			keys[ dst_index ]      = key;     // Store key
			if( hasValue ) values.set1( dst_index, value );
			else values.set1( dst_index, null );
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to point to the new entry (1-based)
			_version++; // Increment version
			
			return true; // New mapping added
			
			
		}
		
		/**
		 * Resizes the internal arrays to the specified new size and rehashes all existing entries.
		 * This is a structural modification that invalidates all previously obtained tokens.
		 *
		 * @param newSize           The new capacity for the map.
		 * @param forceNewHashCodes If {@code true}, attempts to use a new hashing strategy via the {@link #forceNewHashCodes} function.
		 * @return The new capacity of the map.
		 */
		private int resize( int newSize, boolean forceNewHashCodes ) {
			_version++;
			
			// Store old data before re-initializing
			K[]                    old_keys    = keys;
			int[]                  old_hash    = hash;
			UIntNullList.RW old_values  = values.clone(); // Clone values to copy them over
			int                    old_lo_Size = _lo_Size;
			int                    old_hi_Size = _hi_Size;
			
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
					copy( key, equal_hash_K.hashCode( key ), old_values.get( i ), old_values.hasValue( i ) );
				}
				
				// Iterate through old hi region
				for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) {
					key = old_keys[ i ];
					copy( key, equal_hash_K.hashCode( key ), old_values.get( i ), old_values.hasValue( i ) );
				}
				
				return keys.length; // Return actual new capacity
			}
			
			// Copy elements from old structure to new structure by re-inserting
			
			// Iterate through old lo region
			for( int i = 0; i < old_lo_Size; i++ ) {
				copy( old_keys[ i ], old_hash[ i ], old_values.get( i ), old_values.hasValue( i ) );
			}
			
			// Iterate through old hi region
			for( int i = old_keys.length - old_hi_Size; i < old_keys.length; i++ ) {
				copy( old_keys[ i ], old_hash[ i ], old_values.get( i ), old_values.hasValue( i ) );
			}
			
			return keys.length; // Return actual new capacity
		}
		
		/**
		 * Initializes or re-initializes the internal data structures to the specified capacity.
		 *
		 * @param capacity The desired capacity for the internal arrays.
		 * @return The provided capacity.
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets = new int[ capacity ];     // Initialize buckets array
			hash     = new int[ capacity ];     // Initialize hash array
			links    = new int[ Math.min( 16, capacity ) ]; // Initialize links with a small, reasonable capacity (from ObjectBitsMap)
			keys     = equal_hash_K.copyOf( null, capacity ); // Initialize keys array
			values   = new UIntNullList.RW( capacity, values == null ?
			                                                 64 :
			                                                 values.flatStrategyThreshold() ); // Re-initialize values with new capacity
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
		 * @param key      The key to copy.
		 * @param hash     The hash code of the key.
		 * @param value    The value associated with the key.
		 * @param hasValue True if the value is valid, false if it represents null.
		 */
		private void copy( K key, int hash, long value, boolean hasValue ) {
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
			
			keys[ dst_index ]      = key; // Store the key
			this.hash[ dst_index ] = hash; // Store the hash
			if( hasValue ) values.set1( dst_index, value );
			else values.set1( dst_index, null );
			_buckets[ bucketIndex ] = dst_index + 1; // Update bucket to new head (1-based)
		}
		
		// Removed obsolete static hash_next, next, hash methods
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
		
		@Override public RW< K > clone() { return ( RW< K > ) super.clone(); }
	}
	
	@SuppressWarnings( "unchecked" )
	static < K > Array.EqualHashOf< RW< K > > equal_hash() { return ( Array.EqualHashOf< RW< K > > ) RW.OBJECT; }
}