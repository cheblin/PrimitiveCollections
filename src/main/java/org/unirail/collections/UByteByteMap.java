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
 * {@code ByteIntMap} is an interface defining a map that associates byte keys with primitive values.
 * <p>
 * This map is specifically designed for scenarios requiring efficient storage and retrieval of primitive values
 * based on byte keys. It supports null values, allowing the map to explicitly store and represent the absence
 * of a value or a value that is intentionally set to null. This feature distinguishes it from maps that might
 * implicitly treat the absence of a key as a null value.
 * <p>
 * Implementations of this interface are optimized for performance and memory efficiency, especially when dealing
 * with a limited range of byte keys (0-255).
 */
public interface UByteByteMap {
	
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
	abstract class R extends UByteSet.R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Array to store primitive values associated with byte keys.
		 * <p>
		 * In sparse mode (less than 128 keys), the array is dynamically resized, and each index corresponds to the
		 * rank of a key in the underlying {@code ByteSet} minus 1 (since ranks start at 1). In flat mode (128 or more
		 * keys), the array is fixed at 256 elements, and each index directly corresponds to a byte key treated as an
		 * unsigned value (0 to 255, computed as {@code key & 0xFF}). This dual-mode design optimizes memory usage for
		 * small maps and lookup speed for larger ones.
		 */
		protected byte[] values;
		/**
		 * Stores the primitive value associated with the null key.
		 * Default value is 0.
		 */
		protected byte   nullKeyValue = 0;
		
		/**
		 * Retrieves the value associated with the null key.
		 *
		 * @return The value associated with the null key, or 0 if no null key is present.
		 */
		public byte nullKeyValue() { return (byte) nullKeyValue; }
		
		/**
		 * Retrieves the primitive value associated with the given token.
		 * <p>
		 * A token is an encoded representation of a key that efficiently identifies its position within the map.
		 *
		 * @param token The token representing the key to look up.
		 * @return The primitive value associated with the key.
		 */
		public byte value( long token ) {
			return (byte) ( isKeyNull( token ) ?
					nullKeyValue :
					values[ ( int ) token >>> KEY_LEN ] );
		}
	
		
		/**
		 * Checks if the map contains the specified primitive value.
		 * <p>
		 * This method iterates through all the values in the map (including the null key value if present)
		 * to determine if the given value exists.
		 *
		 * @param value The primitive value to search for.
		 * @return {@code true} if the map contains the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( byte value ) { return tokenOfValue( value ) != -1; }
		
		
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
			int h =  seed;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				h = Array.mix( h, Array.hash( values[ token >>> KEY_LEN ] ) );
				
				h = Array.finalizeHash( h, 2 );
				a += h;
				b ^= h;
				c *= h | 1;
			}
		
			
			if( hasNullKey ) {
				h = Array.hash( seed );
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
			if( other == this ) return true;
			if( other == null ||
			    !super.equals( other ) || // Compare ByteSet part
			    hasNullKey && nullKeyValue != other.nullKeyValue || // Compare null key value
			    size() != other.size() ) // Compare sizes. Important to check size before array iteration
				return false;
			long t;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( ( t = other.tokenOf( key( token ) ) ) == -1 || other.value( t ) != values[ token >>> KEY_LEN ] ) return false;
			return true;
		}
		
		
		protected long token( long token, int key ) {
			return ( long ) _version << VERSION_SHIFT | (
					values.length == 256 ?
							( long ) key << KEY_LEN :
							( token & ( long ) ~KEY_MASK ) + ( 1 << KEY_LEN )
			) | key;
		}
		
		protected long tokenOf( int key ) {
			return ( long ) _version << VERSION_SHIFT | (
					                                            values.length == 256 ?
							                                            ( long ) key :
							                                            rank( ( byte ) key ) - 1
			                                            ) << KEY_LEN | key;
		}
		
		
		/**
		 * Finds the first token associated with the specified primitive value.
		 * <p>
		 * Searches {@code nullKeyValue} first (if the null key exists), then iterates through the {@code values} array
		 * using tokens to find a matching value.
		 *
		 * @param value The primitive value to search for.
		 * @return The token of the key associated with the value, or -1 if not found.
		 */
		public long tokenOfValue( byte value ) {
			if( hasNullKey && nullKeyValue == value ) return token( KEY_MASK );
			
			for( int t = -1; ( t = unsafe_token( t ) ) != -1; ) if( values[ t >>> KEY_LEN ] == value ) return t;
			return -1;
		}
		
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()}
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token, or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 */
		@Override public int unsafe_token( int token ) {
			if( token == -1 ) {
				
				int ret = next1( -1 );
				
				return ret == -1 ?
						-1 :
						( int ) tokenOf( ret );
			}
			
			int next = next1( token & KEY_MASK );
			
			return next == -1 ?
					-1 :
					( int ) token( token, next );
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code ByteIntMap} instance.
		 * <p>
		 * The clone will contain copies of the keys and values. The {@code ByteSet} and {@code values} array
		 * are cloned, but the primitive values themselves are not deep-copied (if they were objects).
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
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) // Iterate over byte keys
			     json.name( String.valueOf( key( token ) ) ).value( values[ token >>> KEY_LEN ] ); // Write key-value pair
			json.exitObject();
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
		public RW( int length ) { values = new byte[ ( int ) Math.max( Math.min( Array.nextPowerOf2( length ), 0x100 ), 16 ) ]; }
		
		/**
		 * Clears all key-value mappings from this {@code ByteIntMap}.
		 * <p>
		 * This method resets the map to an empty state, removing all byte keys and their associated primitive values,
		 * including the null key and its value.
		 *
		 * @return This {@code RW} instance to allow for method chaining.
		 */
		public RW clear() {
			_clear(); // Clear ByteSet part
			return this;
		}
		
		
		/**
		 * Associates the specified primitive value with the specified key in this map.
		 * <p>
		 * If the map previously contained a mapping for the key, the old value is replaced by the specified value.
		 *
		 * @param key   The byte key with which the specified value is to be associated. A {@code null} key is permitted.
		 * @param value The primitive value to be associated with the specified key.
		 * @return {@code true} if a new key-value mapping was added (i.e., the key was not previously in the map),
		 * {@code false} if an existing mapping was updated.
		 */
		public boolean put(  Character key, byte value ) {
			if( key != null ) return put( ( char ) ( key + 0 ), value );
			nullKeyValue = ( byte ) value;
			return _add();
		}
		
		
		/**
		 * Associates the specified primitive value with the specified byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 *
		 * @param key   The byte key with which the specified value is to be associated.
		 * @param value The primitive value to be associated with the specified key.
		 * @return {@code true} if a new key-value mapping was added (i.e., the key was not previously in the map),
		 * {@code false} if an existing mapping was updated.
		 */
		public boolean put( char key, byte value ) {
			if( get_( ( byte ) key ) ) {
				values[ values.length == 256 ?
						key & 0xFF :
						rank( ( byte ) key ) - 1 ] = ( byte ) value; // Set or update the value at the calculated index
				return false;
			}
			
			if( values.length == 256 ) values[ key & 0xFF ] = ( byte ) value;
			else if( cardinality == 128 ) {//switch to flat mode
				byte[] values_ = new byte[ 256 ];
				for( int token = -1, ii = 0; ( token = unsafe_token( token ) ) != -1; )
				     values_[ token & KEY_MASK ] = values[ ii++ ];
				
				( values = values_ )[ key & 0xFF ] = ( byte ) value;
			}
			else {
				
				int r = rank( ( byte ) key ); // Get the rank for the key
				
				Array.resize( values,
				              cardinality < values.length ?
						              values :
						              ( values = new byte[ values.length * 2 ] ), r, cardinality, 1 ); // Resize the 'values' array to accommodate the new value at index 'i'
				values[ r ] = ( byte ) value;
			}
			
			return set1( ( byte ) key );
		}
		
		/**
		 * Removes the mapping for the specified key from this {@code ByteIntMap} if present.
		 *
		 * @param key The byte key whose mapping is to be removed from the map. Null key is permitted.
		 * @return {@code true} if a mapping was removed as a result of this call, {@code false} if no mapping existed for the key.
		 */
		public boolean remove(  Character key ) {
			return key == null ?
					_remove() :
					remove( ( char ) ( key + 0 ) ); // Handle null key removal
			
		}
		
		/**
		 * Removes the mapping for the specified byte key from this {@code ByteIntMap} if present.
		 *
		 * @param key The byte key whose mapping is to be removed from the map.
		 * @return {@code true} if a mapping was removed as a result of this call, {@code false} if no mapping existed for the key.
		 */
		public boolean remove( char key ) {
			if( !set0( ( byte ) key ) ) return false;
			if( values.length == 256 ) return true;
			
			Array.resize( values, values, rank( ( byte ) key ), cardinality + 1, -1 ); // Resize values array to remove the value at the index of removed key
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
		public RW clone() {
			
			return ( RW ) super.clone();
		} // Call super.clone() to clone the base class part
	}
}