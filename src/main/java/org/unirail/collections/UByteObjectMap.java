// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit others to do so, subject to the following conditions:
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
// FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.ConcurrentModificationException;

/**
 * {@code ByteObjectMap} is an interface for a map that uses primitive {@code byte} values as keys
 * and stores object references as values.
 * <p>
 * It supports {@code null} keys and {@code null} values.
 * <p>
 * Implementations are expected to provide efficient storage and retrieval of key-value pairs
 * where keys are bytes.
 */
public interface UByteObjectMap {
	
	/**
	 * {@code R} (Read-only) is an abstract base class that provides read-only operations for {@code ByteObjectMap}.
	 * <p>
	 * It extends {@link ByteSet.R} to manage byte keys and adds functionality for storing and retrieving values
	 * associated with these keys.
	 * <p>
	 * Key-value pairs are stored using a rank-based indexing system, allowing for efficient access.
	 * Versioning via tokens is employed to ensure safe iteration and detect concurrent modifications.
	 *
	 * @param <V> The type of values stored in this map.
	 */
	abstract class R< V > extends UByteSet.R implements Cloneable, JsonWriter.Source {
		/**
		 * Array to store values. The index in this array corresponds to the rank of the key.
		 * The rank is determined by the order of insertion of the keys.
		 */
		public          V[]                    values;
		/**
		 * Helper object responsible for determining equality and generating hash codes for values of type {@code V}.
		 * This allows for customization of value comparison logic if needed.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		/**
		 * Stores the value associated with a {@code null} key.
		 * If there is no mapping for a {@code null} key, this field will typically be {@code null}.
		 */
		protected       V                      nullKeyValue;
		
		public V nullKeyValue() { return nullKeyValue; }
		
		/**
		 * Protected constructor to initialize a read-only {@code ByteObjectMap}.
		 * <p>
		 * It initializes the {@link #equal_hash_V} using {@link Array#get(Class)} based on the provided value class.
		 *
		 * @param clazz The {@code Class} object representing the type of values ({@code V}) to be stored in the map.
		 *              This is used to obtain an appropriate {@link Array.EqualHashOf} instance for value handling.
		 */
		protected R( Class< V > clazz ) {
			this.equal_hash_V = Array.get( clazz );
		}
		
		/**
		 * Constructor to initialize a read-only {@code ByteObjectMap} with a custom equality and hash strategy.
		 * <p>
		 * Allows specifying a custom {@link Array.EqualHashOf} instance for handling values of type {@code V}.
		 * This is useful when specific equality or hashing rules are required for the value type.
		 *
		 * @param equal_hash_V Custom {@link Array.EqualHashOf} implementation to be used for values of type {@code V}.
		 */
		public R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		
		/**
		 * Retrieves the value associated with the key represented by the given token.
		 * <p>
		 * Tokens are used for efficient and safe iteration. This method decodes the token to find the corresponding value.
		 *
		 * @param token The token representing a key in the map.
		 * @return The value associated with the key of the token,
		 * or {@link #nullKeyValue} if the token represents the {@code null} key.
		 * @throws ConcurrentModificationException If the token is found to be invalid,
		 *                                         which indicates that the map has been structurally modified since the token was obtained.
		 */
		public V value( long token ) {
			int r;
			return isKeyNull( token ) ?
					nullKeyValue :
					values[ ( ( r = ( int ) ( token & RANK_MASK ) ) == 0 ?
							rank( ( byte ) ( token & KEY_MASK ) ) :
							r >>> RANK_SHIFT ) - 1 ];
		}
		
		/**
		 * Retrieves the value associated with the specified boxed {@code Byte} key.
		 * <p>
		 * Handles {@code null} keys by returning the value associated with the {@code null} key if {@code key} is {@code null}.
		 *
		 * @param key The boxed {@code Byte} key whose associated value is to be returned. A {@code null} key is permitted.
		 * @return The value associated with the given key, or {@code null} if the key is not found in the map
		 * (unless {@code null} itself is stored as a value).
		 */
		public V value(  Character key ) {
			return key == null ?
					nullKeyValue :
					super.contains( (char)( 0xFF &  ( ( byte ) ( key + 0 ) )) ) ?
							value( rank( key ) - 1 ) :
							null;
		}
		
		/**
		 * Retrieves the value associated with the specified primitive {@code byte} key.
		 *
		 * @param key The primitive {@code byte} key whose associated value is to be returned.
		 * @return The value associated with the given key, or {@code null} if the key is not found in the map
		 * (unless {@code null} itself is stored as a value).
		 */
		public V value( char key ) {
			return super.contains( key ) ?
					value( rank( key ) - 1 ) :
					null;
		}
		
		/**
		 * Checks if this map contains a value equal to the specified value.
		 * <p>
		 * Uses the {@link #equal_hash_V} strategy to compare values.
		 *
		 * @param value The value whose presence in this map is to be tested. {@code null} values are permitted.
		 * @return {@code true} if a value equal to {@code value} is present in this map; {@code false} otherwise.
		 */
		public boolean containsValue( V value ) {
			return value == null ?
					hasNullKey && equal_hash_V.equals( value, nullKeyValue ) :
					Array.indexOf( values, value, 0, cardinality ) != -1;
		}
		
		/**
		 * Returns the hash code for this map.
		 * <p>
		 * The hash code is computed based on the hash codes of the keys and values in the map,
		 * as well as the presence and value associated with the null key.
		 *
		 * @return The hash code value for this map.
		 */
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			{
				int h = Array.mix( seed, super.hashCode() );
				h = Array.mix( h, Array.hash( h, values, 0, cardinality ) );
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
		
		private static final int seed = IntObjectMap.R.class.hashCode();
		
		/**
		 * Compares this map to the specified object for equality.
		 * <p>
		 * Returns {@code true} if the given object is also a {@code ByteObjectMap.R} and the two maps represent the same mappings.
		 * More formally, two maps are considered equal if they contain the same keys and corresponding values are equal
		 * according to their {@code equals} method (using {@link #equal_hash_V} for value comparison).
		 *
		 * @param obj The object to be compared for equality with this map.
		 * @return {@code true} if the specified object is equal to this map; {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj ); }
		
		/**
		 * Compares this read-only map to another read-only map of the same type for equality.
		 * <p>
		 * Two maps are equal if they have the same size, the same key-value mappings, and the same null key status and value.
		 * Value equality is determined using the {@link #equal_hash_V} strategy.
		 *
		 * @param other The other read-only map to be compared with this map.
		 * @return {@code true} if the {@code other} map is equal to this map; {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) {
			if( other == this ) return true;
			return other != null &&
			       super.equals( other ) &&
			       equal_hash_V.equals( values, other.values, cardinality ) &&
			       ( !hasNullKey || equal_hash_V.equals( nullKeyValue, other.nullKeyValue ) );
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code ByteObjectMap.R} instance.
		 * <p>
		 * The keys and values array are cloned, but the value objects themselves are not deep-copied.
		 * If the values are mutable objects, changes to the values in the original map will be reflected in the cloned map, and vice versa.
		 *
		 * @return A clone of this {@code ByteObjectMap.R} instance.
		 */
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			
			R< V > dst = ( R< V > ) super.clone();
			dst.values = values.clone(); // Shallow copy: value references are copied, not the value objects themselves.
			return dst;
		}
		
		/**
		 * Returns a string representation of this map in JSON format.
		 * <p>
		 * Delegates to {@link #toJSON()} to generate the JSON string.
		 *
		 * @return A JSON string representing the content of this map.
		 */
		public String toString() {
			return toJSON();
		}
		
		/**
		 * Writes the content of this map as a JSON object to the provided {@link JsonWriter}.
		 * <p>
		 * Iterates over the key-value pairs and writes them as name-value pairs in the JSON object.
		 * Byte keys are converted to strings for JSON representation.
		 *
		 * @param json The {@link JsonWriter} to which the JSON representation of this map should be written.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			if( hasNullKey ) json.name().value( nullKeyValue ); // Represent null key as "null" in JSON
			int key;
			for( long token = token(); ( key = ( int ) ( token & KEY_MASK ) ) < 0x100; token = token( token ) )
			     json.name( String.valueOf( key ) ).value( values[ rank( token ) - 1 ] );
			json.exitObject();
		}
	}
	
	/**
	 * {@code RW} (Read-Write) is a concrete implementation of {@code ByteObjectMap} that allows modification of the map.
	 * <p>
	 * It extends {@link ByteObjectMap.R} and provides methods for adding, updating, and removing key-value pairs.
	 *
	 * @param <V> The type of values stored in this map.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs a new, empty read-write {@code ByteObjectMap} with the specified initial capacity and value type.
		 *
		 * @param clazz  The {@code Class} object representing the type of values ({@code V}) to be stored.
		 * @param length The initial capacity of the map. The map will be internally resized if more elements are added.
		 *               The capacity is clamped to be at least 16 and at most 256 (the maximum number of unique bytes).
		 */
		public RW( Class< V > clazz, int length ) {
			this( Array.get( clazz ), length );
		}
		
		/**
		 * Constructs a new, empty read-write {@code ByteObjectMap} with the specified initial capacity and custom value handling.
		 *
		 * @param equal_hash_V The custom {@link Array.EqualHashOf} implementation for values of type {@code V}.
		 * @param length       The initial capacity of the map. The map will be resized as needed.
		 *                     The capacity is clamped to be at least 16 and at most 256.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int length ) {
			super( equal_hash_V );
			values = this.equal_hash_V.copyOf( null, Math.max( Math.min( length, 0x100 ), 16 ) );
		}
		
		/**
		 * Removes all mappings from this map.
		 * <p>
		 * The map will be empty after this operation. Resets the size and clears internal data structures.
		 *
		 * @return This {@code RW} instance to allow for method chaining.
		 */
		public RW< V > clear() {
			_clear();
			java.util.Arrays.fill( values, 0, cardinality, null ); // Clear value array
			nullKeyValue = null;                              // Clear null key value
			return this;
		}
		
		/**
		 * Associates the specified value with the specified boxed {@code Byte} key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * Handles {@code null} keys.
		 *
		 * @param key   The boxed {@code Byte} key with which the specified value is to be associated. {@code null} key is permitted.
		 * @param value The value to be associated with the specified key.
		 * @return {@code true} if a new key-value mapping was added, {@code false} if the key already existed and the value was updated.
		 */
		public boolean put(  Character key, V value ) {
			if( key == null ) {
				nullKeyValue = value;
				return _add();
			}
			return put( ( char ) ( key + 0 ), value );
		}
		
		/**
		 * Associates the specified value with the specified primitive {@code byte} key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 *
		 * @param key   The primitive {@code byte} key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key.
		 * @return {@code true} if a new key-value mapping was added, {@code false} if the key already existed and the value was updated.
		 */
		public boolean put( char key, V value ) {
			int     i     = rank( ( byte ) key ) - 1;
			boolean added = set1( ( byte ) key ); // Try to add the key, returns true if key was new
			if( added )  // if key was newly added, we need to resize the values array if necessary
				Array.resize( values, i < values.length ?
						values :
						( values = equal_hash_V.copyOf( null, Math.min( values.length * 2, 0x100 ) ) ), i, cardinality, 1 );
			
			values[ i ] = value;
			
			return added; // return true if key was added (new mapping), false otherwise (value updated for existing key)
		}
		
		/**
		 * Removes the mapping for a boxed {@code Byte} key from this map if it is present.
		 * <p>
		 * Handles {@code null} keys.
		 *
		 * @param key The boxed {@code Byte} key whose mapping is to be removed from the map. {@code null} key is permitted.
		 * @return {@code true} if a mapping was removed as a result of this call, {@code false} if no mapping existed for the key.
		 */
		public boolean remove(  Character key ) {
			if( key == null ) {
				nullKeyValue = null;
				_remove();
			}
			return remove( ( char ) ( key + 0 ) );
		}
		
		/**
		 * Removes the mapping for a primitive {@code byte} key from this map if it is present.
		 *
		 * @param key The primitive {@code byte} key whose mapping is to be removed from the map.
		 * @return {@code true} if a mapping was removed as a result of this call, {@code false} if no mapping existed for the key.
		 */
		public boolean remove( char key ) {
			if( !set0( ( byte ) key ) ) return false; // _remove returns false if key was not present
			Array.resize( values, values, rank( key ) - 1, cardinality, -1 ); // Shift values array to fill the gap
			return true; // Mapping was removed
		}
		
		/**
		 * Creates and returns a deep copy of this read-write {@code ByteObjectMap} instance.
		 * <p>
		 * The clone is a new map with the same mappings as this map. If the values are mutable objects,
		 * they are still shared between the original and the cloned map (shallow copy of values).
		 *
		 * @return A clone of this {@code RW} instance.
		 */
		public RW< V > clone() { return ( RW< V > ) super.clone(); }
		
		// Static helper for equality/hashing of RW instances. Used by equal_hash() method.
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 * Returns an {@link Array.EqualHashOf} instance suitable for comparing {@code RW<V>} maps for equality and hashing.
	 * <p>
	 * This provides a way to treat {@code RW<V>} maps as values themselves for use in other collections or algorithms that require equality checks and hash codes.
	 *
	 * @param <V> The value type of the {@code RW} map.
	 * @return An {@link Array.EqualHashOf} instance for {@code RW<V>}.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() {
		return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT;
	}
}