// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// 1. The above copyright notice and this permission notice shall be included in all
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
// FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.ConcurrentModificationException;

/**
 * {@code ByteIntNullMap} is an interface defining a map that associates byte keys with integer values.
 * <p>
 * This map supports null keys and null values. It is built upon {@link ByteSet.RW} for efficient key management.
 * The implementation is optimized for performance and low memory footprint.
 */
public interface ByteIntNullMap {
	
	
	/**
	 * {@code R} is an abstract base class providing read-only functionality for {@link ByteIntNullMap}.
	 * <p>
	 * It extends {@link ByteSet.R} for key storage and management.
	 * Values are stored in an {@link Array.FF} based {@code int[]} array, supporting null values.
	 * This class is designed to be lightweight and efficient for read operations.
	 */
	abstract class R extends ByteSet.R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Manages null value flags for each key.
		 * <p>
		 * If {@code nulls.get(key)} is true, it indicates that the value associated with the key is not null,
		 * and the actual value can be found in the {@link #values} array at the index derived from {@code nulls.rank(key)}.
		 * If false, the value associated with the key is considered null.
		 */
		protected Array.FF               nulls        = new Array.FF() { };
		/**
		 * Stores integer values associated with the keys.
		 * <p>
		 * The index of the value in this array corresponds to the rank of the key in the {@link #nulls} structure.
		 */
		protected int[]          values;
		/**
		 * Default value returned when retrieving a null key's value if no value has been explicitly set for the null key.
		 */
		protected int            nullKeyValue = 0;
		/**
		 * Flag indicating whether the {@link #nullKeyValue} is explicitly set to null.
		 */
		protected boolean                nullKeyValueNull;
		/**
		 * Flag indicating if a non-null value has been explicitly associated with the null key.
		 * <p>
		 * When true, {@link #nullKeyValue} holds the non-null value. When false, the null key is considered to have a null value unless {@link #hasNullKey} is false.
		 */
		protected boolean                nullKeyHasValue; // Indicates if the null key has a non-null value
		
		
		/**
		 * Checks if this map contains a specific value (boxed Integer).
		 *
		 * @param value The {@link Integer} value to search for in the map. Null values are also supported.
		 * @return {@code true} if the map contains the specified value, {@code false} otherwise.
		 */
		public boolean containsValue(  Integer   value ) {
			if( size() == 0 ) return false;
			
			return value == null ?
					hasNullKey && !nullKeyHasValue || 0 < nulls.first1() :
					containsValue( ( int ) ( value + 0 ) );
		}
		
		/**
		 * Checks if this map contains a specific primitive integer value.
		 *
		 * @param value The primitive {@code int} value to search for in the map.
		 * @return {@code true} if the map contains the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( int value ) { return Array.indexOf( values, ( int ) value, 0, nulls.size ) != -1; }
		
		
		/**
		 * Checks if the entry associated with the given token has a non-null value.
		 *
		 * @param token The token representing a key in the map.
		 * @return {@code true} if the entry corresponding to the token has a non-null value, {@code false} otherwise.
		 * @throws ConcurrentModificationException If the token is invalid due to concurrent modification of the map.
		 */
		public boolean hasValue( long token ) {
			return isKeyNull( token ) ?
					nullKeyHasValue :
					nulls.get( ( byte ) ( token & KEY_MASK ) );
		}
		
		/**
		 * Retrieves the integer value associated with the given token.
		 *
		 * @param token The token representing a key in the map.
		 * @return The integer value associated with the token. Returns {@link #nullKeyValue} if the token represents the null key.
		 * @throws ConcurrentModificationException If the token is invalid due to concurrent modification of the map.
		 */
		public int value( long token ) {
			return ( isKeyNull( token ) ?
					nullKeyValue :
					values[ nulls.rank( ( byte ) ( token & KEY_MASK ) ) - 1 ] );
		}
		
		/**
		 * Computes the hash code for this map based on its keys and values.
		 * <p>
		 * The hash code is calculated to be consistent with the {@link #equals(Object)} method.
		 * It includes the hash codes of the keys, values, and the null key status.
		 *
		 * @return A hash code value for this map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			{
				int h = Array.mix( seed, super.hashCode() );
				h = Array.mix( h, Array.hash( h, values, 0, nulls.size ) );
				h = Array.mix( h, nulls.hashCode() );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			if( hasNullKey ) {
				int h = nullKeyHasValue ?
						Array.mix( seed, nullKeyValueNull ?
								22633363 :
								Array.hash( nullKeyValue ) ) :
						Array.hash( seed );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this map to the specified object for equality.
		 * <p>
		 * The comparison is based on the class type and the content of the maps, including keys and values.
		 *
		 * @param obj The object to compare to.
		 * @return {@code true} if the given object is also a {@code ByteIntNullMap.R} and the two maps are equal, {@code false} otherwise.
		 */
		@Override
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( ( R ) obj );
		}
		
		/**
		 * Compares this read-only map to another {@code ByteIntNullMap.R} for equality.
		 * <p>
		 * Equality is determined by comparing the presence of a null key, the associated value for the null key (if any),
		 * the size of the maps, and the key-value pairs for non-null keys.
		 *
		 * @param other The {@code ByteIntNullMap.R} to compare to.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == null || hasNullKey != other.hasNullKey ||
			    hasNullKey && ( nullKeyHasValue != other.nullKeyHasValue || nullKeyValue != other.nullKeyValue ) ||
			    size() != other.size() ||
			    !super.equals( other ) )
				return false;
			
			
			for( int i = 0; i < nulls.size; i++ ) if( values[ i ] != other.values[ i ] ) return false;
			
			return true;
		}
		
		/**
		 * Creates and returns a deep copy of this read-only map.
		 * <p>
		 * The clone will contain the same mappings as the original map.
		 *
		 * @return A clone of this {@code ByteIntNullMap.R} instance.
		 */
		@Override
		public R clone() {
			
			try {
				R dst = ( R ) super.clone();
				dst.values = values.clone(); // Shallow copy; deep copy may be needed for mutable V
				dst.nulls  = ( Array.FF ) nulls.clone();
				return dst;
			} catch( CloneNotSupportedException e ) { }
			return null;
		}
		
		/**
		 * Returns a string representation of this map in JSON format.
		 *
		 * @return A JSON string representation of the map.
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the content of this map to a {@link JsonWriter}.
		 * <p>
		 * The map is represented as a JSON object where keys are string representations of bytes
		 * and values are the corresponding integer values or null if the value is null.
		 *
		 * @param json The {@link JsonWriter} to write the JSON representation to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey )
				json.name().value( nullKeyHasValue ?
						                   nullKeyValue :
						                   null );
			
			for( long token = token(), key; ( key = token & KEY_MASK ) < 0x100; token = token( token ) ) {
				json.name( String.valueOf( key ) );
				if( nulls.get( ( byte ) key ) )
					json.value(    values[ nulls.rank( ( byte ) key ) - 1 ]  );
				else
					json.value();
			}
			json.exitObject();
		}
	}
	
	/**
	 * {@code RW} (Read-Write) class provides a mutable implementation of {@link ByteIntNullMap}.
	 * <p>
	 * It extends {@link ByteIntNullMap.R} and allows modification of the map, including adding, updating, and removing key-value pairs.
	 * This class is suitable for scenarios where the map needs to be dynamically changed.
	 */
	class RW extends R {
		
		/**
		 * Constructs a new {@code RW} map with an initial capacity.
		 *
		 * @param length The initial capacity of the map. The actual capacity will be at least 16 and at most 256.
		 */
		public RW( int length ) {
			values = new int[ Math.max( Math.min( length, 0x100 ), 16 ) ];
		}
		
		/**
		 * Clears all mappings from this map.
		 * <p>
		 * This operation removes all key-value pairs, including the null key mapping if present, and resets the map to an empty state.
		 *
		 * @return This {@code RW} instance to allow for method chaining.
		 */
		public RW clear() {
			_clear();
			nulls._clear();
			return this;
		}
		
		/**
		 * Associates the specified integer value with the specified boxed Byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * Supports null keys.
		 *
		 * @param key   The boxed {@link Byte} key with which the specified value is to be associated.
		 *              Null is a valid key.
		 * @param value The primitive {@code int} value to be associated with the specified key.
		 * @return {@code true} if the key was newly added to the map, {@code false} if the key already existed.
		 */
		public boolean put(  Byte      key, int value ) {
			if( key != null ) return put( ( byte ) ( key + 0 ), value );
			nullKeyValue = ( int ) value;
			return _add();
		}
		
		/**
		 * Associates the specified boxed Integer value with the specified boxed Byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * Supports null keys and null values.
		 *
		 * @param key   The boxed {@link Byte} key with which the specified value is to be associated.
		 *              Null is a valid key.
		 * @param value The boxed {@link Integer} value to be associated with the specified key.
		 *              Null is a valid value.
		 * @return {@code true} if the key was newly added to the map, {@code false} if the key already existed.
		 */
		public boolean put(  Byte      key,  Integer   value ) {
			if( key != null ) return put( ( byte ) ( key + 0 ), value );
			
			nullKeyValue = ( int ) ( ( nullKeyValueNull = ( value == null ) ) ?
					0 :
					value + 0 );
			
			return _add();
		}
		
		/**
		 * Associates the specified boxed Integer value with the specified primitive byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * Supports null values.
		 *
		 * @param key   The primitive {@code byte} key with which the specified value is to be associated.
		 * @param value The boxed {@link Integer} value to be associated with the specified key.
		 *              Null is a valid value.
		 * @return {@code true} if the key was newly added to the map, {@code false} if the key already existed.
		 */
		public boolean put( byte key,  Integer   value ) {
			
			if( value != null ) return put( key, ( int ) ( value + 0 ) );
			
			if( nulls._remove( ( byte ) key ) ) Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.size + 1, -1 );
			
			return _add( ( byte ) key ); // return true if key was added, false otherwise
		}
		
		/**
		 * Associates the specified primitive integer value with the specified primitive byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 *
		 * @param key   The primitive {@code byte} key with which the specified value is to be associated.
		 * @param value The primitive {@code int} value to be associated with the specified key.
		 * @return {@code true} if the key was newly added to the map, {@code false} if the key already existed.
		 */
		public boolean put( byte key, int value ) {
			
			if( nulls._add( ( byte ) key ) ) {
				int i = nulls.rank( ( byte ) key ) - 1;
				
				if( i + 1 < nulls.size || values.length < nulls.size )
					Array.resize( values, values.length < nulls.size ?
							( values = new int[ Math.min( values.length * 2, 0x100 ) ] ) :
							values, i, nulls.size - 1, 1 );
				
				values[ i ] = ( int ) value;
				return _add( ( byte ) key ); // return true if key was added, false otherwise
			}
			
			values[ nulls.rank( ( byte ) key ) - 1 ] = ( int ) value;
			
			return false;
		}
		
		/**
		 * Removes the mapping for the specified boxed Byte key from this map if present.
		 * <p>
		 * If a mapping exists for the key, it is removed. Null keys are supported.
		 *
		 * @param key The boxed {@link Byte} key whose mapping is to be removed from the map.
		 *            Null is a valid key.
		 * @return {@code true} if a mapping was removed for the key, {@code false} otherwise.
		 */
		public boolean remove(  Byte      key ) {
			if( key != null ) return remove( ( byte ) ( key + 0 ) );
			
			nullKeyHasValue = false;
			return _remove();
		}
		
		/**
		 * Removes the mapping for the specified primitive byte key from this map if present.
		 * <p>
		 * If a mapping exists for the key, it is removed.
		 *
		 * @param key The primitive {@code byte} key whose mapping is to be removed from the map.
		 * @return {@code true} if a mapping was removed for the key, {@code false} otherwise.
		 */
		public boolean remove( byte key ) {
			if( !_remove( ( byte ) key ) ) return false;
			
			if( nulls._remove( ( byte ) key ) )
				Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.size + 1, -1 );
			return true;
		}
		
		/**
		 * Creates and returns a deep copy of this read-write map.
		 * <p>
		 * The clone will contain the same mappings as the original map and is also mutable.
		 *
		 * @return A clone of this {@code ByteIntNullMap.RW} instance.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
	}
}