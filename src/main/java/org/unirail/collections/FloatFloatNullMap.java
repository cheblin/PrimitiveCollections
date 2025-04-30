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
 * {@code IntIntNullMap} is a specialized map interface designed for storing primitive keys and primitive values.
 * <p>
 * It provides efficient operations for managing key-value pairs where both keys and values are primitives.
 * The map supports null keys and utilizes a hash table with separate chaining to resolve hash collisions,
 * ensuring good performance even with a large number of entries.
 * <p>
 * This interface defines the contract for maps that need to handle primitive-to-primitive mappings with null key support,
 * offering methods for common map operations like insertion, retrieval, deletion, and checking for existence.
 */
public interface FloatFloatNullMap {
	
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
		
		protected boolean hasNullKey;          // Indicates if the map contains a null key
		protected boolean nullKeyHasValue;
		
		protected float            nullKeyValue;        // Value for the null key, stored separately.
		protected int[]                  _buckets;            // Hash table buckets array (1-based indices to chain heads).
		protected int[]                  nexts;               // Links within collision chains (-1 termination, -2 unused, <-2 free list link).
		protected float[]          keys; // Keys array.
		protected FloatNullList.RW values;             // Values array.
		protected int                    _count;              // Hash mode: Total slots used (entries + free slots). Flat mode: Number of set bits (actual entries).
		protected int                    _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int                    _freeCount;          // Number of free entries in the free list.
		protected int                    _version;            // Version counter for concurrent modification detection.
		
		protected static final int  StartOfFreeList = -3; // Marks the start of the free list in 'nexts' field.
		protected static final int  VERSION_SHIFT   = 32; // Bits to shift version in token.
		protected static final long INVALID_TOKEN   = -1L; // Invalid token constant.
		protected static final int  NULL_KEY_INDEX  = 0x7FFF_FFFF;
		
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
			return _count - _freeCount + (
					hasNullKey ?
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
		public int count() { return size(); }
		
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
		 * Checks if this map contains a mapping for the specified key (boxed).
		 *
		 * @param key the key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean contains(  Float     key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this map contains a mapping for the specified key (primitive int).
		 *
		 * @param key the key whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping for the specified key, {@code false} otherwise.
		 */
		public boolean contains( float key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this map contains the specified value (boxed).
		 *
		 * @param value the value whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping with the specified value, {@code false} otherwise.
		 */
		public boolean containsValue(  Float     value ) {
			int i;
			return value == null ?
					hasNullKey && !nullKeyHasValue || ( i = values.nextNullIndex( 1 ) ) != -1 &&
					                                  i < _count - _freeCount :
					hasNullKey && nullKeyHasValue && nullKeyValue == value || values.indexOf( value ) != -1;
		}
		
		/**
		 * Checks if this map contains the specified value (primitive int).
		 *
		 * @param value the value whose presence in this map is to be tested.
		 * @return {@code true} if this map contains a mapping with the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( float value ) { return hasNullKey && nullKeyHasValue && nullKeyValue == value || values.indexOf( value ) != -1; }
		
		/**
		 * Returns a token associated with the specified key (boxed).
		 * <p>
		 * Tokens are used for efficient iteration and access to map entries.
		 *
		 * @param key the key for which to find a token (can be null).
		 * @return a token if the key is found, -1 ({@link #INVALID_TOKEN}) if not found.
		 */
		public long tokenOf(  Float     key ) {
			return key == null ?
					hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN :
					tokenOf( key.floatValue     () );
		}
		
		/**
		 * Returns a token associated with the specified key (primitive int).
		 * <p>
		 * Tokens are used for efficient iteration and access to map entries.
		 *
		 * @param key the key for which to find a token.
		 * @return a token if the key is found, -1 ({@link #INVALID_TOKEN}) if not found.
		 */
		public long tokenOf( float key ) {
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
		 * @return the first valid token for iteration, or -1 ({@link #INVALID_TOKEN}) if the map is empty.
		 */
		public long token() {
			for( int i = 0; i < _count; i++ )
				if( -2 < nexts[ i ] ) return token( i );
			return hasNullKey ?
					token( NULL_KEY_INDEX ) :
					INVALID_TOKEN;
		}
		
		/**
		 * Returns the next token in the iteration sequence, starting from the given token.
		 * <p>
		 * This method allows iterating through the map entries in the order they are stored internally.
		 *
		 * @param token the current token in the iteration.
		 * @return the next valid token in the iteration, or -1 ({@link #INVALID_TOKEN}) if there are no more entries or if the token is invalid due to structural modification.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			
			int i = index( token );
			if( i == NULL_KEY_INDEX ) return INVALID_TOKEN;
			
			if( 0 < _count - _freeCount )
				for( i++; i < _count; i++ )
					if( -2 < nexts[ i ] ) return token( i );
			
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
		 * @see #nullKeyHasValue()  To check for a null key has a valur.
		 * @see #nullKeyValue() To get the null key’s value.
		 */
		public int unsafe_token( final int token ) {
			for( int i = token + 1; i < _count; i++ )
				if( -2 < nexts[ i ] ) return i;
			return -1;
		}
		
		/**
		 * Checks if this map contains a mapping for the null key.
		 *
		 * @return {@code true} if this map contains a mapping for the null key, {@code false} otherwise.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		public boolean nullKeyHasValue()       { return nullKeyHasValue; }
		
		public float nullKeyValue() { return  nullKeyValue; }
		
		
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Checks if the entry associated with the given token has a value (not null in the context of null-value support).
		 *
		 * @param token the token to check.
		 * @return {@code true} if the entry has a value and the token is valid, {@code false} otherwise.
		 */
		public boolean hasValue( long token ) {
			return isKeyNull( token ) ?
					nullKeyHasValue :
					values.hasValue( index( token ) );
		}
		
		/**
		 * Returns the key associated with the specified token.  Before calling this method ensure that this token is not point to the isKeyNull
		 *
		 * @param token the token for which to retrieve the key.
		 * @return the key associated with the token.
		 */
		public float key( long token ) { return  keys[ index( token ) ]; }
		
		/**
		 * Returns the value associated with the specified token.
		 * <p>
		 * <b>Precondition:</b> This method should only be called if {@link #hasValue(long)} returns {@code true} for the given token.
		 * Calling it for a token associated with a {@code null} value results in undefined behavior.
		 * <p>
		 *
		 * @param token the token for which to retrieve the value.
		 * @return the value associated with the token, or 0 if the value is null in null-value context.
		 */
		public float value( long token ) {
			return (
					isKeyNull( token ) ?
							nullKeyValue :
							values.get( index( token ) ) );
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
			if( other == this ) return true;
			if( other == null || hasNullKey != other.hasNullKey ||
			    hasNullKey && ( nullKeyHasValue != other.nullKeyHasValue || nullKeyValue != other.nullKeyValue ) ||
			    size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || hasValue( token ) != other.hasValue( t ) ||
				    hasValue( token ) && value( token ) != other.value( t ) )
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
		public String toString() { return toJSON(); }
		
		
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
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
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
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | index; }
		
		protected int index( long token ) { return ( int ) ( ( int ) token ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token the token.
		 * @return the version extracted from the token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
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
		public RW() { this( 0 ); }
		
		/**
		 * Constructs an empty {@code RW} map with the specified initial capacity.
		 *
		 * @param capacity the initial capacity of the map.
		 */
		public RW( int capacity ) {
			values = new FloatNullList.RW( 0 );
			if( capacity > 0 )initialize( Array.prime( capacity ) );
		}
		
		/**
		 * Sets the threshold that determines when to switch the values collection from
		 * <b>Compressed Strategy</b> to <b>Flat Strategy</b>.
		 * <p>
		 * The switch is typically based on the density of null values. The default value is 1024.
		 *
		 * @param interleavedBits the threshold value for switching strategies.
		 */
		public void flatStrategyThreshold( int interleavedBits ) { values.flatStrategyThreshold = interleavedBits; }
		
		/**
		 * Initializes the internal arrays of the map with a given capacity.
		 * <p>
		 * The capacity is adjusted to the nearest prime number greater than or equal to the given capacity.
		 *
		 * @param capacity the desired capacity.
		 * @return the initialized capacity (which is a prime number).
		 */
		private int initialize( int capacity ) {
			_version++;
			_buckets  = new int[ capacity ];
			nexts     = new int[ capacity ];
			keys      = new float[ capacity ];
			values    = new FloatNullList.RW( capacity );
			_freeList  = -1;
			_count     = 0;
			_freeCount = 0;
			return capacity;
		}
		
		public boolean put(  Float     key,  Float     value ) {
			return key == null ?
					value == null ?
							put() :
							putNullKey( value ) :
					value == null ?
							putNullValue( key ) :
							put( key.floatValue     (), value.floatValue     () );
		}
		
		public boolean put( float key,  Float     value ) {
			return value == null ?
					putNullValue( key ) :
					put( key, value.floatValue     () );
		}
		
		public boolean put(  Float     key, float value ) {
			return key == null ?
					putNullKey( value ) :
					put( key.floatValue     (), value );
		}
		
		/**
		 * Associates the specified value with the specified key in this map value.
		 *
		 * @param key   the key with which the specified value is to be associated.
		 * @param value the value to be associated with the specified key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 */
		public boolean put( float key, float value ) { return put( key, value, true ); }
		
		public boolean putNullKey( float  value ) { return put( value, true ); }
		
		public boolean putNullValue( float key )  { return put( key, ( float ) 0, false ); }
		
		/**
		 * Associates the specified value with the specified key in this map (primitive int key and primitive int value).
		 * <p>
		 * If the map previously contained a mapping for the key, the old value is replaced by the specified value.
		 *
		 * @param key   the key with which the specified value is to be associated.
		 * @param value the value to be associated with the specified key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 */
		private boolean put( float key, float value, boolean hasValue ) {
			if( _buckets == null ) initialize( 7 );
			int[] _nexts         = nexts;
			int   hash           = Array.hash( key );
			int   collisionCount = 0;
			int   bucketIndex    = bucketIndex( hash );
			int   bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) {
					if( hasValue ) values.set1( next, value );
					else values.set1( next, null );
					_version++;
					return false;
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
					
					bucket = _buckets[ bucketIndex = bucketIndex( hash ) ] - 1;
				}
				index = _count++;
			}
			
			nexts[ index ] = ( int ) bucket;
			keys[ index ]  = ( float ) key;
			
			if( hasValue ) values.set1( index, value );
			else values.set1( index, null );
			
			_buckets[ bucketIndex ] = index + 1;
			_version++;
			
			return true;
		}
		
		/**
		 * Associates the specified value with the null key in this map (boxed value).
		 * <p>
		 * If the map previously contained a mapping for the null key, the old value is replaced by the specified value.
		 *
		 * @param value the value to be associated with the null key.
		 * @return {@code true} if the map was structurally modified as a result of this operation, {@code false} otherwise.
		 */
		private boolean put( float value, boolean hasValue ) {
			boolean b = !hasNullKey;
			
			hasNullKey = true;
			if( nullKeyHasValue = hasValue ) nullKeyValue = ( float ) value;
			_version++;
			return b;
		}
		
		public boolean put() { return put( ( float ) 0, false ); }
		
		
		/**
		 * Removes the mapping for the null key from this map if present.
		 *
		 * @return true if remove entry or false no mapping was found for the null key.
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey      = false;
			nullKeyHasValue = false;
			_version++;
			return true;
		}
		
		
		/**
		 * Removes the mapping for the specified key from this map if present (primitive int key).
		 * <p>
		 * If a non-null value is being removed and it's not the last non-null value in the values list,
		 * this method optimizes removal by swapping the entry with the last non-null entry to avoid shifting in the values list.
		 *
		 * @param key key whose mapping is to be removed from the map.
		 * @return true if remove entry or false no mapping was found for the key.
		 */
		public boolean remove( float key ) {
			// Handle edge cases: if map is uninitialized or empty, nothing to remove
			if( _buckets == null || _count == 0 )
				return false; // Return invalid token indicating no removal
			
			// Compute hash and bucket index for the key to locate its chain
			int bucketIndex    = bucketIndex( Array.hash( key ) );       // Map hash to bucket index
			int last           = -1;                             // Previous index in chain (-1 if 'i' is head)
			int i              = _buckets[ bucketIndex ] - 1;         // Head of chain (convert 1-based to 0-based)
			int collisionCount = 0;                    // Counter to detect infinite loops or concurrency issues
			
			// Traverse the linked list in the bucket to find the key
			
			while( -1 < i ) {
				int next = nexts[ i ];                   // Get next index in the chain
				if( keys[ i ] == key ) {                  // Found the key at index 'i'
					
					if( values.isFlatStrategy ) {
						values.nulls.set0( i );//optional
						
						if( last < 0 ) _buckets[ bucketIndex ] = next + 1;
						else nexts[ last ] = next;
					}
					else // values used compressedStrategy
					{
						// Step 1: Unlink the entry at 'i' from its chain
						if( last < 0 )
							// If 'i' is the head, update bucket to point to the next entry
							_buckets[ bucketIndex ] = next + 1;
						
						else
							// Otherwise, link the previous entry to the next, bypassing 'i'
							nexts[ last ] = ( int ) next;
						
						// Step 2: Optimize removal if value is non-null and not the last non-null
						final int lastNonNullValue = values.nulls.last1(); // Index of last non-null value
						
						if( values.hasValue( i ) )
							if( i != lastNonNullValue ) {
								// Optimization applies: swap with last non-null entry
								// Step 2a: Copy key, next, and value from lastNonNullValue to i
								float   keyToMove = keys[ lastNonNullValue ];
								int           bucket    = bucketIndex( Array.hash( keyToMove ) );
								int           _next     = nexts[ lastNonNullValue ];
								
								keys[ i ]  = keyToMove;                         // Copy the key to the entry being removed
								nexts[ i ] = _next;         // Copy the next to the entry being removed
								values.set1( i, values.get( lastNonNullValue ) ); // Copy the value to the entry being removed
								
								// Step 2b: Update the chain containing keyToMove to point to 'i'
								int prev = -1;                     // Previous index in keyToMove’s chain
								collisionCount = 0;// Reset collision counter
								
								// Start at chain head
								for( int current = _buckets[ bucket ] - 1; current != lastNonNullValue; prev = current, current = nexts[ current ] )
									if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
								
								if( -1 < prev ) nexts[ prev ] = i;
								else _buckets[ bucket ] = i + 1;// If 'lastNonNullValue' the head, update bucket to the position of keyToMove
								
								
								values.set1( lastNonNullValue, null );        // Clear value (O(1) since it’s last non-null)
								i = lastNonNullValue;
							}
							else values.set1( i, null );                       // Clear value (may shift if not last)
					}
					
					nexts[ i ] = StartOfFreeList - _freeList; // Mark 'i' as free
					
					_freeList = i;
					_freeCount++;       // Increment count of free entries
					_version++;         // Increment version for concurrency control
					return true;    // Return token for removed/overwritten entry
				}
				
				// Move to next entry in chain
				last = i;
				i    = next;
				if( collisionCount++ > nexts.length ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				
			}
			
			return false; // Key not found
		}
		
		
		/**
		 * Removes all mappings from this map.
		 * <p>
		 * The map will be empty after this call.
		 */
		public void clear() {
			_version++;
			hasNullKey = false;
			if( _count == 0 ) return;
			Arrays.fill( _buckets, 0 );
			Arrays.fill( nexts, 0, _count, ( int ) 0 );
			values.clear();
			_count     = 0;
			_freeList  = -1;
			_freeCount = 0;
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
			return capacity <= length() ?
					length() :
					_buckets == null ?
							initialize( capacity ) :
							resize( Array.prime( capacity ) );
			
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
		 * Reduces the capacity of this map to be at least as great as the given capacity.
		 * <p>
		 * If the given capacity is less than the current size of the map, the capacity is trimmed to the current size.
		 *
		 * @param capacity the desired capacity to trim to.
		 * @throws IllegalArgumentException if the specified capacity is less than the current size of the map.
		 */
		public void trim( int capacity ) {
			int newSize = Array.prime( capacity );
			if( length() <= newSize ) return;
			
			int[]                  old_next   = nexts;
			float[]          old_keys   = keys;
			FloatNullList.RW old_values = values;
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
		private int resize( int newSize ) {
			newSize = Math.min( newSize, 0x7FFF_FFFF & -1 >>> 32 -  Float    .BYTES * 8 );
			_version++;
			int[]         new_next = Arrays.copyOf( nexts, newSize );
			float[] new_keys = Arrays.copyOf( keys, newSize );
			final int     count    = _count;
			
			_buckets = new int[ newSize ];
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = _buckets[ bucketIndex ] - 1; //relink chain
					_buckets[ bucketIndex ] = i + 1;
				}
			
			nexts = new_next;
			keys  = new_keys;
			return newSize;
		}
		
		/**
		 * Copies entries from old arrays to new arrays during trimming or resizing.
		 *
		 * @param old_next   the old next indices array.
		 * @param old_keys   the old keys array.
		 * @param old_values the old values list.
		 * @param old_count  the number of entries in the old arrays.
		 */
		private void copy( int[] old_next, float[] old_keys, FloatNullList.RW old_values, int old_count ) {
			
			int ii = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_next[ i ] < -1 ) continue;
				keys[ ii ] = old_keys[ i ];
				if( old_values.hasValue( i ) ) values.set1( ii, old_values.get( i ) );
				else values.set1( ii, null );
				
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) );
				nexts[ ii ]             = ( int ) ( _buckets[ bucketIndex ] - 1 );
				_buckets[ bucketIndex ] = ii + 1;
				ii++;
			}
			_count     = ii;
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