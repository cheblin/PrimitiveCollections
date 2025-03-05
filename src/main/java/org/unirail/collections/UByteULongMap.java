//  MIT License
//
//  Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//  For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//  GitHub Repository: https://github.com/AdHoc-Protocol
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in all
//  copies or substantial portions of the Software.
//
//  Users of the Software must provide a clear acknowledgment in their user
//  documentation or other materials that their solution includes or is based on
//  this Software. This acknowledgment should be prominent and easily visible,
//  and can be formatted as follows:
//  "This product includes software developed by Chikirev Sirguy and the Unirail Group
//  (https://github.com/AdHoc-Protocol)."
//
//  If you modify the Software and distribute it, you must include a prominent notice
//  stating that you have changed the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

/**
 * {@code ByteIntMap} is an interface defining a map that associates byte keys with integer values.
 * <p>
 * This map is specifically designed for scenarios requiring efficient storage and retrieval of integer values
 * based on byte keys. It supports null values, allowing the map to explicitly store and represent the absence
 * of a value or a value that is intentionally set to null. This feature distinguishes it from maps that might
 * implicitly treat the absence of a key as a null value.
 * <p>
 * Implementations of this interface are optimized for performance and memory efficiency, especially when dealing
 * with a limited range of byte keys (0-255).
 */
public interface UByteULongMap {

	/**
	 * {@code R} is an abstract base class providing a skeletal implementation of the {@code ByteIntMap} interface.
	 * <p>
	 * It extends {@code ByteSet.R} to manage the set of byte keys and introduces an integer array to store the
	 * corresponding values. This class handles the core logic for storing, retrieving, and managing key-value pairs,
	 * including support for a null key and its associated value.
	 * <p>
	 * Subclasses of {@code R} are expected to provide concrete implementations for specific use cases, such as
	 * mutable or immutable maps.
	 */
	abstract class R  extends UByteSet .R implements Cloneable, JsonWriter.Source {

		/**
		 * Array to store integer values associated with the byte keys.
		 * The index in this array corresponds to the rank of the key in the underlying {@code ByteSet}.
		 */
		protected long[] values;
		/**
		 * Stores the integer value associated with the null key.
		 * Default value is 0.
		 */
		protected long   nullKeyValue = 0;

		/**
		 * Retrieves the integer value associated with the given token.
		 * <p>
		 * A token is an encoded representation of a key that efficiently identifies its position within the map.
		 *
		 * @param token The token representing the key to look up.
		 * @return The integer value associated with the key represented by the token.
		 *         Returns {@code nullKeyValue} if the token represents a null key.
		 *         Returns the value from the {@code values} array if the token represents a byte key.
		 */
		public long value( long token ) {
			int r;
			return ( isKeyNull( token ) ?
					nullKeyValue :
					values[ ( ( r = ( int ) ( token & RANK_MASK ) ) == 0 ?
							rank( ( byte ) ( token & KEY_MASK ) ) :
							r >>> RANK_SHIFT ) - 1 ] );
		}

		/**
		 * Checks if the map contains the specified integer value.
		 * <p>
		 * This method iterates through all the values in the map (including the null key value if present)
		 * to determine if the given value exists.
		 *
		 * @param value The integer value to search for.
		 * @return {@code true} if the map contains the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( long value ) { return Array.indexOf( values, ( long ) value, 0, size ) != -1; }

		/**
		 * Calculates the hash code for this {@code ByteIntMap}.
		 * <p>
		 * The hash code is computed based on the keys and values present in the map, including the null key and its value.
		 * It uses a mixing function to combine the hash codes of individual key-value pairs to produce a final hash code
		 * for the entire map. This ensures that maps with the same key-value mappings have the same hash code.
		 *
		 * @return The hash code for this {@code ByteIntMap}.
		 */
		public int hashCode() {
			int a = 0, b = 0, c = 1;

			int key;
			for( long token = token(); ( key = ( int ) ( token & KEY_MASK ) ) < 0x100; token = token( token ) ) {
				int h = Array.mix( seed, Array.hash( values[ rank( token ) - 1 ] ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}

			if( hasNullKey ) {
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( nullKeyValue ) );
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}

			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( super.hashCode(), a ), b ), c ), size() );
		}

		/**
		 * Seed value used in hash code calculation.
		 * It's derived from the class hash code to provide a consistent starting point for mixing.
		 */
		private static final int seed = IntIntMap.R.class.hashCode();

		/**
		 * Checks if this {@code ByteIntMap} is equal to another object.
		 * <p>
		 * Equality is defined as having the same class and the same key-value mappings.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) ); }

		/**
		 * Checks if this {@code ByteIntMap} is equal to another {@code R} instance.
		 * <p>
		 * Equality is determined by comparing the underlying {@code ByteSet} (keys), the {@code nullKeyValue},
		 * and the {@code values} array. Two {@code ByteIntMap} instances are considered equal if they have the
		 * same keys and the corresponding values are also equal.
		 *
		 * @param other The {@code R} instance to compare with.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			if( other == null ||
			    super.equals( other ) || // Compare ByteSet part
			    ( hasNullKey && nullKeyValue != other.nullKeyValue ) || // Compare null key value
			    size() != other.size() ) // Compare sizes. Important to check size before array iteration
				return false;

			for( int i = 0; i < size; i++ )
				if( values[ i ] != other.values[ i ] ) return false; // Compare value arrays

			return true;
		}

		/**
		 * Creates and returns a shallow copy of this {@code ByteIntMap} instance.
		 * <p>
		 * The clone will contain copies of the keys and values. The {@code ByteSet} and {@code values} array
		 * are cloned, but the integer values themselves are not deep-copied (if they were objects).
		 *
		 * @return A clone of this {@code ByteIntMap} instance.
		 */
		@Override
		public R clone() {
			R dst = ( R ) super.clone(); // Clone ByteSet part
			dst.values = values.clone();  // Clone values array
			return dst;
		}

		/**
		 * Returns a string representation of this {@code ByteIntMap} in JSON format.
		 *
		 * @return A JSON string representation of the map.
		 */
		public String toString() { return toJSON(); }

		/**
		 * Writes this {@code ByteIntMap} to a {@code JsonWriter}.
		 * <p>
		 * This method serializes the map into a JSON object. Byte keys are converted to strings for JSON representation.
		 * The null key, if present, is represented by a JSON null key name.
		 *
		 * @param json The {@code JsonWriter} to write the JSON output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 ); // Pre-allocate buffer for better performance
			json.enterObject();              // Start JSON object
			if( hasNullKey ) json.name().value( nullKeyValue ); // Write null key value
			int key;
			for( long token = token(); ( key = ( int ) ( token & KEY_MASK ) ) < 0x100; token = token( token ) ) // Iterate over byte keys
			     json.name( String.valueOf( key ) ).value( values[ rank( token ) - 1 ] ); // Write key-value pair
			json.exitObject();               // End JSON object
		}

	}


	/**
	 * {@code RW} is a concrete, mutable implementation of the {@code ByteIntMap} interface, extending the abstract
	 * class {@code ByteIntMap.R}.
	 * <p>
	 * It provides read and write operations for managing byte-to-integer mappings. This class is designed for
	 * scenarios where the map needs to be modified after creation.
	 */
	class RW extends R {
		/**
		 * Constructs a new {@code RW} instance with an initial capacity.
		 * <p>
		 * The initial capacity determines the starting size of the internal arrays. It is capped at 256 (the maximum
		 * number of distinct byte keys) and defaults to a minimum of 16 for efficiency.
		 *
		 * @param length The desired initial capacity of the map.
		 */
		public RW( int length ) { values = new long[ Math.max( Math.min( length, 0x100 ), 16 ) ]; }

		/**
		 * Clears all key-value mappings from this {@code ByteIntMap}.
		 * <p>
		 * This method resets the map to an empty state, removing all byte keys and their associated integer values,
		 * including the null key and its value.
		 *
		 * @return This {@code RW} instance to allow for method chaining.
		 */
		public RW clear() {
			_clear(); // Clear ByteSet part
			return this;
		}

		/**
		 * Associates the specified integer value with the specified key in this map.
		 * <p>
		 * If the map previously contained a mapping for the key, the old value is replaced by the specified value.
		 *
		 * @param key   The byte key with which the specified value is to be associated. A {@code null} key is permitted.
		 * @param value The integer value to be associated with the specified key.
		 * @return {@code true} if a new key-value mapping was added (i.e., the key was not previously in the map),
		 *         {@code false} if an existing mapping was updated.
		 */
		public boolean put(  Character key, long value ) {
			if( key == null ) {
				nullKeyValue = (long)value;
				return _add(); // Add null key to ByteSet if not already present
			}
			return put( ( char ) ( key + 0 ), value ); // Unbox Byte and call byte version of put
		}


		/**
		 * Associates the specified integer value with the specified byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 *
		 * @param key   The byte key with which the specified value is to be associated.
		 * @param value The integer value to be associated with the specified key.
		 * @return {@code true} if a new key-value mapping was added (i.e., the key was not previously in the map),
		 *         {@code false} if an existing mapping was updated.
		 */
		public boolean put( char key, long value ) {
			int     i     = rank( ( byte ) key ) - 1; // Get the rank/index for the key
			boolean added = _add( ( byte ) key );      // Try to add the key to the ByteSet
			if( added )  // If the key was newly added to the ByteSet (not already present)
				Array.resize( values, i < values.length ? // Resize values array if needed
						values : // if enough space, use current array
						( values = new long[ Math.min( values.length * 2, 0x100 ) ] ), // otherwise create a new array, doubling the size but not exceeding 256
						i, size, 1 ); // Resize the 'values' array to accommodate the new value at index 'i'

			values[ i ] =  (long)value; // Set or update the value at the calculated index

			return added; // Return true if key was added (new mapping), false otherwise (existing mapping updated)
		}

		/**
		 * Removes the mapping for the specified key from this {@code ByteIntMap} if present.
		 *
		 * @param key The byte key whose mapping is to be removed from the map. Null key is permitted.
		 * @return {@code true} if a mapping was removed as a result of this call, {@code false} if no mapping existed for the key.
		 */
		public boolean remove(  Character key ) {
			if( key == null ) remove( null ); // Handle null key removal
			return remove( ( char ) ( key + 0 ) ); // Unbox Byte and call byte version of remove
		}

		/**
		 * Removes the mapping for the specified byte key from this {@code ByteIntMap} if present.
		 *
		 * @param key The byte key whose mapping is to be removed from the map.
		 * @return {@code true} if a mapping was removed as a result of this call, {@code false} if no mapping existed for the key.
		 */
		public boolean remove( char key ) {
			if( !_remove( ( byte ) key ) ) return false; // Remove key from ByteSet, return false if key not found
			Array.resize( values, values, rank( key ) - 1, size, -1 ); // Resize values array to remove the value at the index of removed key
			return true; // Return true if key was successfully removed
		}

		/**
		 * Creates and returns a shallow copy of this {@code RW} instance.
		 * <p>
		 * The clone will contain copies of the keys and values. The underlying arrays are cloned, but the integer
		 * values themselves are not deep-copied.
		 *
		 * @return A clone of this {@code RW} instance.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); } // Call super.clone() to clone the base class part
	}
}