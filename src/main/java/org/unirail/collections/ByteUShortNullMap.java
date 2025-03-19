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
 * This map supports null keys and nullable integer values for byte keys. It is built upon {@link ByteSet.RW} for efficient key management.
 * The implementation employs two strategies for storing values to optimize for both space and performance depending on the density of non-null values.
 * Initially, a compressed, rank-based strategy is used for memory efficiency when the map is sparse.
 * As the map becomes denser (the number of non-null values increases), and the cost of rank calculations in the compressed strategy becomes noticeable,
 * it transitions to a flat, one-to-one mapping strategy for faster lookups.
 */
public interface ByteUShortNullMap {
	
	/**
	 * {@code R} is an abstract base class providing read-only functionality for {@link ByteIntNullMap}.
	 * <p>
	 * It extends {@link ByteSet.R} for key storage and management.
	 * Values are stored in an {@link Array.FF} based {@code int[]} array, supporting nullable values.
	 * This class is designed to be lightweight and efficient for read operations.
	 * <p>
	 * **Value Storage Strategies:**
	 * This class employs two strategies for storing integer values associated with byte keys to balance memory efficiency and performance:
	 * <ol>
	 *     <li><b>Compressed (Rank-Based) Strategy (Initial - Sparse Map Optimized):</b>
	 *         - Used when the map is relatively sparse (fewer non-null values).
	 *         - Only non-null values are stored in the {@link #values} array, maximizing memory efficiency for sparse data.
	 *         - {@link #nulls} bitset tracks which keys have non-null values.
	 *         - Value lookup involves calculating the rank of the key in {@link #nulls} to find the corresponding value in the compact {@link #values} array.
	 *         - Memory-efficient as it avoids storing null values explicitly in the `values` array.
	 *         - Value access has a performance overhead due to rank calculation, which becomes more significant as the number of non-null values increases.
	 *     </li>
	 *     <li><b>Flat (One-to-One) Strategy (Transitioned - Dense Map Optimized):</b>
	 *         - Used when the map becomes denser (the number of non-null values reaches a threshold, specifically when adding the 128th non-null entry in {@link RW}).
	 *         - The {@link #values} array is resized to a fixed size of 256 (covering all possible byte values).
	 *         - The index in the {@link #values} array directly corresponds to the byte key's unsigned value (0-255), enabling very fast direct access.
	 *         - {@link #nulls} is still used to quickly check if a key has a non-null value before accessing {@link #values}.
	 *         - Value lookup is extremely fast due to direct array indexing, eliminating the rank calculation overhead.
	 *         - Uses a fixed 256-integer array regardless of the number of non-null values after transition, trading some potential memory for consistent high performance lookups, even if the map becomes sparse again later.
	 *     </li>
	 * </ol>
	 * The map automatically transitions from the Compressed to the Flat strategy to optimize performance as the density of non-null values increases and rank calculation overhead grows.
	 */
	abstract class R extends ByteSet.R implements Cloneable, JsonWriter.Source {
		
		/**
		 * Manages null value flags for each key.
		 * <p>
		 * In **Compressed (Rank-Based) Strategy:**
		 * If {@code nulls.get(key)} is true, it indicates that the value associated with the key is not null,
		 * and the actual value can be found in the {@link #values} array at the index derived from {@code nulls.rank(key)}.
		 * If false, the value associated with the key is considered null.
		 * <p>
		 * In **Flat (One-to-One) Strategy:**
		 * {@code nulls.get(key)} is still used to quickly check if a key has a non-null value. If true, the value is in {@link #values} at index `key & 0xFF`.
		 */
		protected Array.FF               nulls        = new Array.FF() { };
		/**
		 * Stores integer values associated with the keys.
		 * <p>
		 * In **Compressed (Rank-Based) Strategy:**
		 * The index of the value in this array corresponds to the rank of the key in the {@link #nulls} structure. Only non-null values are stored compactly.
		 * <p>
		 * In **Flat (One-to-One) Strategy:**
		 * This array is resized to length 256. The index of the value in this array directly corresponds to the key's unsigned value (0-255) for direct access.
		 */
		protected char[]          values;
		/**
		 * Default value returned when retrieving a null key's value if no value has been explicitly set for the null key.
		 */
		protected char            nullKeyValue = 0;
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
		public boolean containsValue(  Character value ) {
			if( size() == 0 ) return false;
			
			return value == null ?
					hasNullKey && !nullKeyHasValue || 0 < nulls.first1() :
					containsValue( ( char ) ( value + 0 ) );
		}
		
		/**
		 * Checks if this map contains a specific primitive integer value.
		 *
		 * @param value The primitive {@code int} value to search for in the map.
		 * @return {@code true} if the map contains the specified value, {@code false} otherwise.
		 */
		public boolean containsValue( char value ) { return Array.indexOf( values, ( char ) value, 0, nulls.cardinality ) != -1; }
		
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
		 * <p>
		 * The retrieval strategy depends on whether the map is using the **Compressed (Rank-Based)** or **Flat (One-to-One)** strategy.
		 *
		 * @param token The token representing a key in the map.
		 * @return The integer value associated with the token. Returns {@link #nullKeyValue} if the token represents the null key.
		 * @throws ConcurrentModificationException If the token is invalid due to concurrent modification of the map.
		 */
		public char value( long token ) {
			int i;
			return  ( isKeyNull( token ) ?
					nullKeyValue :
					nulls.get( ( byte ) ( i = ( int ) ( token & KEY_MASK ) ) ) ?
							values.length == 256 ?
									values[ i ] :
									values[ nulls.rank( ( byte ) i ) - 1 ] :
							0 ); // Default value (null) if nulls.get(key) is false (null value)
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
				
				if( values.length == 256 )
					for( int i = nulls.first1(); i != -1; i = nulls.next1( i ) ) h = Array.mix( h, Array.hash( values[ i ] ) );
				else
					h = Array.mix( h, Array.hash( h, values, 0, nulls.cardinality ) );
				
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
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
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
			
			if( values.length == 256 ) {
				for( int i = nulls.first1(); i != -1; i = nulls.next1( i ) ) if( values[ i ] != other.values[ i ] ) return false;
			}
			else for( int i = 0; i < nulls.cardinality; i++ ) if( values[ i ] != other.values[ i ] ) return false;
			
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
					json.value(    ( values.length == 256 ?
							values[ ( int ) key ] :
							values[ nulls.rank( ( byte ) key ) - 1 ] )  ); // Compressed (Rank-Based) Strategy - Rank calculation for sparse access
				else
					json.value(); // Output null for null value
			}
			json.exitObject();
		}
	}
	
	/**
	 * {@code RW} (Read-Write) class provides a mutable implementation of {@link ByteIntNullMap}.
	 * <p>
	 * It extends {@link ByteIntNullMap.R} and allows modification of the map, including adding, updating, and removing key-value pairs.
	 * This class is suitable for scenarios where the map needs to be dynamically changed.
	 * <p>
	 * It inherits and utilizes the two value storage strategies from {@link R}: Compressed (Rank-Based) and Flat (One-to-One),
	 * transitioning between them automatically as the map's density of non-null values increases and the overhead of rank calculations in the Compressed strategy grows.
	 */
	class RW extends R {
		
		/**
		 * Constructs a new {@code RW} map with an initial capacity.
		 * <p>
		 * Initially, the map starts with the **Compressed (Rank-Based) Strategy**, optimized for sparse maps.
		 * The {@link #values} array is initialized with a size that is at least 16 and at most 256, based on the provided {@code length} hint.
		 *
		 * @param length The initial capacity hint for the map. The actual capacity will be at least 16 and at most 256.
		 */
		public RW( int length ) { values = new char[ ( int ) Math.max( Math.min( Array.nextPowerOf2( length ), 0x100 ), 16 ) ]; }
		
		/**
		 * Clears all mappings from this map.
		 * <p>
		 * This operation removes all key-value pairs, including the null key mapping if present, and resets the map to an empty state.
		 * The map reverts to its initial state using the **Compressed (Rank-Based) Strategy** with an empty (or initial size) {@link #values} array, ready to efficiently store sparse data again.
		 *
		 * @return This {@code RW} instance to allow for method chaining.
		 */
		public RW clear() {
			_clear();
			nulls._clear();
			// values array is implicitly reset to initial size upon next put operation if needed, or remains as is (no explicit reset here for optimization).
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
		public boolean put(  Byte      key, char value ) {
			if( key != null ) return put( ( byte ) ( key + 0 ), value );
			nullKeyValue    = ( char ) value;
			nullKeyHasValue = true;
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
		public boolean put(  Byte      key,  Character value ) {
			if( key != null ) return put( ( byte ) ( key + 0 ), value );
			
			nullKeyValue    = ( char ) ( ( nullKeyValueNull = ( value == null ) ) ?
					0 :
					value + 0 );
			nullKeyHasValue = !nullKeyValueNull;
			
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
		public boolean put( byte key,  Character value ) {
			if( value != null ) return put( key, ( char ) ( value + 0 ) );
			
			// Handling null value: Remove the key from nulls bitset if it was previously associated with a non-null value.
			if( nulls.set0( ( byte ) key ) ) {
				// If using Compressed (Rank-Based) Strategy, resize values array to remove the entry and maintain compactness.
				if( values.length < 256 )
					Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.cardinality + 1, -1 );
				// In Flat (One-to-One) Strategy, no resizing is needed as all 256 slots are pre-allocated; the value slot will implicitly become a default int value (0).
			}
			
			return set1( ( byte ) key ); // return true if key was added (as a key with null value), false otherwise (key already present, value changed to null)
		}
		
		/**
		 * Associates the specified primitive integer value with the specified primitive byte key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced by the specified value.
		 * <p>
		 * This method intelligently handles both **Compressed (Rank-Based)** and **Flat (One-to-One)** strategies and manages the transition from the former to the latter when the map becomes dense.
		 * The transition occurs when adding the 128th non-null entry, aiming to optimize for performance as rank calculation becomes less efficient with denser data.
		 *
		 * @param key   The primitive {@code byte} key with which the specified value is to be associated.
		 * @param value The primitive {@code int} value to be associated with the specified key.
		 * @return {@code true} if the key was newly added to the map, {@code false} if the key already existed.
		 */
		public boolean put( byte key, char value ) {
			if( values.length == 256 ) {
				values[ key & 0xFF ] = ( char ) value;
				return nulls.set1( ( byte ) key ) && set1( ( byte ) key );
			}
			
			if( nulls.cardinality == 127 && !nulls.get( ( byte ) key ) ) { // Threshold reached for transition to Flat Strategy: Adding the 128th non-null entry triggers the switch.
				char[] newValues = new char[ 256 ]; // Create a new array of size 256 to accommodate all byte values for direct indexing.
				
				// Copy existing values from the compressed 'values' array to the new 256 array, using 'nulls' to efficiently iterate through existing non-null keys and values.
				for( int ii = nulls.first1(), k = 0; -1 < ii; ii = nulls.next1( ii ), k++ )
				     newValues[ ii ] = values[ k ];
				
				( values = newValues )[ key & 0xFF ] = ( char ) value; // Store the value at the calculated index, either rank-based (Compressed) or direct (Flat).
				return set1( ( byte ) key ); // return true if key was newly added to the base ByteSet (key was not present before), false otherwise (shouldn't happen in this path after nulls._add)
			}
			
			
			if( !nulls.set1( ( byte ) key ) ) {
				values[ nulls.rank( ( byte ) key ) - 1 ] = ( char ) value; // Rank-based index in Compressed Strategy - Slower for dense maps, efficient for sparse.
				return false; // Key already existed, value updated.
			}
			
			int i = nulls.rank( ( byte ) key ) - 1; // Calculate rank-based index to find the correct position in the compact 'values' array.
			
			// Resize 'values' array if needed to accommodate the new non-null value while maintaining compactness.
			if( i + 1 < nulls.cardinality || values.length < nulls.cardinality )
				Array.resize( values, values.length < nulls.cardinality ?
						( values = new char[ Math.min( values.length * 2, 0x100 ) ] ) :
						// Double size (up to 256 max) if growing to maintain amortized insertion cost.
						values, i, nulls.cardinality - 1, 1 ); // Resize to fit, shifting elements to make space for the new value at the rank-based index.
			
			values[ i ] = ( char ) value; // Store the value at the calculated index, either rank-based (Compressed) or direct (Flat).
			return set1( ( byte ) key ); // return true if key was newly added to the base ByteSet (key was not present before), false otherwise (shouldn't happen in this path after nulls._add)
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
		 * <p>
		 * This method handles removal in both **Compressed (Rank-Based)** and **Flat (One-to-One)** strategies, ensuring consistent behavior regardless of the current strategy.
		 *
		 * @param key The primitive {@code byte} key whose mapping is to be removed from the map.
		 * @return {@code true} if a mapping was removed for the key, {@code false} otherwise.
		 */
		public boolean remove( byte key ) {
			if( !set0( ( byte ) key ) ) return false; // Remove key from base ByteSet (handling key presence).
			
			if( nulls.set0( ( byte ) key ) ) { // Remove the non-null flag from nulls bitset if it was set, indicating a non-null value was associated.
				if( values.length == 256 ) // Flat (One-to-One) Strategy: Just clear the value slot (optional, but good practice for consistency and potential debugging).
					values[ key & 0xFF ] = 0; // Optional: Reset value slot in Flat Strategy for removed key to default int value (0).
				else // Compressed (Rank-Based) Strategy: Resize values array to remove the now-null entry and maintain compactness, shifting subsequent elements.
					Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.cardinality + 1, -1 ); // Resize to remove the value, shifting elements to fill the gap.
			}
			return true; // Mapping removed successfully (or was not present initially).
		}
		
		/**
		 * Creates and returns a deep copy of this read-write map.
		 * <p>
		 * The clone will contain the same mappings as the original map and is also mutable, allowing independent modifications without affecting the original map.
		 *
		 * @return A clone of this {@code ByteIntNullMap.RW} instance.
		 */
		@Override
		public RW clone() { return ( RW ) super.clone(); }
	}
}