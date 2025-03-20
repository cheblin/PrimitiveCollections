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
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.unirail.collections;

import org.unirail.JsonWriter;

import java.util.ConcurrentModificationException;

/**
 * {@code ByteObjectNullMap} is an interface defining a map that uses primitive {@code byte} values as keys and stores objects as values.
 * <p>
 * It extends the functionality of a standard map by explicitly supporting {@code null} keys and {@code null} values.
 * Implementations of this interface provide efficient storage and retrieval of object values based on byte keys,
 * with specific considerations for handling null keys and values.
 */
public interface ByteObjectNullMap {
	
	/**
	 * {@code R} (Read-only) is an abstract base class that provides the foundation for read-only operations on a byte-keyed object map.
	 * <p>
	 * It manages the underlying storage for keys and values, and implements a token-based mechanism with versioning to ensure safe and consistent iteration
	 * even when concurrent modifications might occur.
	 * <p>
	 * This class is designed to be extended by concrete implementations that require read-only or read-write functionalities.
	 *
	 * @param <V> The type of values stored in this map.
	 */
	abstract class R< V > extends ByteSet.R implements Cloneable, JsonWriter.Source {
		
		/**
		 * {@code nulls} is an internal array used to track the presence and order of non-null byte keys.
		 * It is a {@link Array.FF} (Fixed-size Flag Array) which is optimized for boolean flags associated with byte keys.
		 */
		protected       Array.FF               nulls = new Array.FF() { };
		/**
		 * {@code values} is the array storing the object values associated with the byte keys.
		 * The index of a value in this array corresponds to the rank of its key as determined by the {@code nulls} array.
		 * <p>
		 * Values are stored in the same order as their corresponding keys are tracked in {@code nulls}.
		 */
		public          V[]                    values;
		/**
		 * {@code equal_hash_V} is a helper object responsible for determining equality and generating hash codes for values of type {@code V}.
		 * It is an instance of {@link Array.EqualHashOf}, which provides a strategy for handling potentially complex value types.
		 */
		protected final Array.EqualHashOf< V > equal_hash_V;
		/**
		 * {@code nullKeyValue} stores the value associated with the null key in this map.
		 * Since primitive bytes cannot be null, a separate mechanism is needed to handle a null key.
		 * This field holds the value that is returned when a null key is queried.
		 */
		protected       V                      nullKeyValue;
		
		public V nullKeyValue() { return  nullKeyValue; }
		
		/**
		 * Protected constructor for {@code R}. Initializes the read-only map with a value type class.
		 * <p>
		 * This constructor is intended for use by subclasses. It sets up the {@link Array.EqualHashOf} strategy based on the provided class of the value type {@code V}.
		 *
		 * @param clazz The class of the value type {@code V}. Used to determine equality and hashing strategies for values.
		 */
		protected R( Class< V > clazz ) {
			this.equal_hash_V = Array.get( clazz );
		}
		
		/**
		 * Constructor for {@code R} that accepts a custom equality and hashing strategy for values.
		 * <p>
		 * This constructor allows for more control over how values of type {@code V} are compared and hashed,
		 * by providing a custom {@link Array.EqualHashOf} implementation.
		 *
		 * @param equal_hash_V Custom {@link Array.EqualHashOf} instance to be used for value type {@code V}.
		 */
		public R( Array.EqualHashOf< V > equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
		}
		
		
		/**
		 * Retrieves the value associated with a given token.
		 * <p>
		 * Tokens are used as an efficient and versioned way to access elements within the map, especially during iteration.
		 *
		 * @param token The token representing a key-value pair in the map.
		 * @return The value associated with the token. Returns {@code nullKeyValue} if the token represents the null key. May return {@code null} if the key exists but has a null value.
		 * @throws ConcurrentModificationException If the token is no longer valid due to structural modifications of the map since the token was obtained.
		 */
		public V value( long token ) {
			int i;
			return isKeyNull( token ) ?
					nullKeyValue :
					// Return nullKeyValue if it's the null key token
					nulls.get( ( byte ) ( i = ( int ) ( token & KEY_MASK ) ) ) ?
							values.length == 256 ?
									values[ i ] :
									values[ nulls.rank( ( byte ) i ) - 1 ] :
							// If key exists, get the value from values array using key's rank
							null; // Key not found (or was removed after token was issued)
		}
		
		/**
		 * Gets the value associated with a boxed {@link Byte} key.
		 * <p>
		 * Allows retrieval of a value using a {@link Byte} object as the key. Handles null keys by returning the {@code nullKeyValue}.
		 *
		 * @param key The boxed {@link Byte} key to look up. Can be {@code null}.
		 * @return The associated value, or {@code null} if the key is not found (or associated with a null value, or if the key itself is null and {@code nullKeyValue} is null).
		 */
		public V value(  Byte      key ) {
			return key == null ?
					nullKeyValue :
					// Handle null key case
					value( ( byte ) ( key + 0 ) ); // Key not found
		}
		
		/**
		 * Gets the value associated with a primitive {@code byte} key.
		 *
		 * @param key The primitive {@code byte} key to look up.
		 * @return The associated value, or {@code null} if the key is not found (or associated with a null value).
		 */
		public V value( byte key ) {
			return nulls.get( ( byte ) ( key + 0 ) ) ?
					values.length == 256 ?
							values[ key & 0xFF ] :
							values[ nulls.rank( ( byte ) ( key + 0 ) ) - 1 ] :
					// Retrieve value by key's rank if key exists
					null; // Key not found
		}
		
		/**
		 * Checks if the map contains a specific value.
		 * <p>
		 * Iterates through the values in the map to determine if the given value is present.
		 * Handles null values correctly by comparing with {@code nullKeyValue} and values in the {@code values} array.
		 *
		 * @param value The value to search for (can be {@code null}).
		 * @return {@code true} if the value is found in the map, {@code false} otherwise.
		 */
		public boolean containsValue( V value ) {
			return value == null ?
					hasNullKey && equal_hash_V.equals( value, nullKeyValue ) :
					// Check for null value against nullKeyValue
					Array.indexOf( values, value, 0, nulls.cardinality ) != -1; // Search for non-null value in the values array
		}
		
		/**
		 * Computes the hash code for this map.
		 * <p>
		 * The hash code is calculated based on the keys and values in the map, as well as the map's structure and versioning information inherited from {@link ByteSet.R}.
		 * This ensures that maps with the same key-value pairs (and null key status) will have the same hash code.
		 *
		 * @return The hash code of this map.
		 */
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			{
				int h = Array.mix( seed, super.hashCode() ); // Mix seed with superclass hashCode (ByteSet.R)
				h = Array.mix( h, nulls.hashCode() ); // Mix with hashCode of nulls array
				h = Array.mix( h, Array.hash( h, values, 0, nulls.cardinality ) ); // Mix with hash of values array
				h = Array.finalizeHash( h, 2 ); // Finalize hash for this part
				a += h; b ^= h; c *= h | 1; // Accumulate hash components
			}
			
			if( hasNullKey ) {
				int h = Array.hash( seed ); // Start hash for null key
				h = Array.mix( h, Array.hash( nullKeyValue != null ?
						                              // Mix with hash of nullKeyValue (or seed if nullKeyValue is null)
						                              equal_hash_V.hashCode( nullKeyValue ) :
						                              seed ) );
				h = Array.finalizeHash( h, 2 ); // Finalize hash for null key part
				a += h; b ^= h; c *= h | 1; // Accumulate hash components
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() ); // Finalize overall hash with size and accumulated components
		}
		
		private static final int seed = IntObjectMap.R.class.hashCode(); // Seed for hash calculations
		
		/**
		 * Checks if this map is equal to another object.
		 * <p>
		 * Equality is determined by comparing the class, the underlying {@link ByteSet.R} state, the {@code nulls} array,
		 * the values array (up to the size of the map), and the {@code nullKeyValue}.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R< V > ) obj ); }
		
		/**
		 * Compares this map to another {@code R} instance for equality.
		 * <p>
		 * Performs a deep comparison to check if both maps contain the same key-value mappings and null key status.
		 *
		 * @param other The other {@code R} instance to compare with.
		 * @return {@code true} if the maps are equal, {@code false} otherwise.
		 */
		public boolean equals( R< V > other ) {
			return other != null &&
			       super.equals( other ) && // Compare ByteSet.R part
			       nulls.equals( other.nulls ) && // Compare nulls arrays
			       equal_hash_V.equals( values, other.values, nulls.cardinality ) && // Compare values arrays (up to map size) using equal_hash_V strategy
			       ( !hasNullKey || equal_hash_V.equals( nullKeyValue, other.nullKeyValue ) ); // Compare null key status and nullKeyValues
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code R} instance.
		 * <p>
		 * The clone will contain shallow copies of the {@code values} and {@code nulls} arrays.
		 * The values themselves are not deep-copied, meaning if values are mutable objects, changes to them will be reflected in both the original and the cloned map.
		 *
		 * @return A shallow copy of this map. Returns {@code null} if cloning is not supported or fails.
		 */
		@SuppressWarnings( "unchecked" )
		public R< V > clone() {
			
			try {
				R< V > dst = ( R< V > ) super.clone(); // Clone the ByteSet.R part
				dst.values = values.clone(); // Shallow copy of values array
				dst.nulls  = ( Array.FF ) nulls.clone(); // Shallow copy of nulls array
				return dst;
			} catch( CloneNotSupportedException e ) { }
			return null; // Cloning failed
		}
		
		/**
		 * Returns a JSON string representation of this map.
		 * <p>
		 * Uses the {@link #toJSON(JsonWriter)} method to generate the JSON output.
		 *
		 * @return A JSON string representing the map's contents.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the contents of this map in JSON format to the provided {@link JsonWriter}.
		 * <p>
		 * The JSON output will be an object where keys are string representations of the byte keys (or "null" for the null key) and values are JSON representations of the object values.
		 *
		 * @param json The {@link JsonWriter} to write the JSON output to.
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 ); // Pre-allocate buffer space for JSON output (heuristic size)
			json.enterObject(); // Begin JSON object
			if( hasNullKey ) json.name( "null" ).value( nullKeyValue ); // Write null key entry if present
			int key;
			for( long token = token(); ( key = ( int ) ( token & KEY_MASK ) ) < 0x100; token = token( token ) ) // Iterate through tokens for byte keys
			     json.name( String.valueOf( key ) ).value( values[ nulls.rank( ( byte ) key ) - 1 ] ); // Write each key-value pair
			json.exitObject(); // End JSON object
		}
	}
	
	/**
	 * {@code RW} (Read-Write) is a concrete implementation of {@link ByteObjectNullMap} that extends the read-only base class {@link R}
	 * and provides full read and write capabilities.
	 * <p>
	 * It allows modification of the map, including adding, updating, and removing key-value pairs.
	 *
	 * @param <V> The type of values stored in this map.
	 */
	class RW< V > extends R< V > {
		
		/**
		 * Constructs a new {@code RW} map with a specified initial capacity and using the default equality/hashing strategy for the value type.
		 *
		 * @param clazz  The class of the value type {@code V}. Used to determine the default equality and hashing strategy.
		 * @param length The initial capacity of the map. This is a hint and the actual initial capacity might be adjusted.
		 */
		public RW( Class< V > clazz, int length ) {
			this( Array.get( clazz ), length );
		}
		
		/**
		 * Constructs a new {@code RW} map with a specified initial capacity and a custom equality/hashing strategy for the value type.
		 *
		 * @param equal_hash_V The custom {@link Array.EqualHashOf} instance to be used for value type {@code V}.
		 * @param length       The initial capacity of the map. This is a hint and the actual initial capacity might be adjusted.
		 */
		public RW( Array.EqualHashOf< V > equal_hash_V, int length ) {
			super( equal_hash_V );
			values = this.equal_hash_V.copyOf( null, Math.max( Math.min( length, 0x100 ), 16 ) ); // Initialize values array with a minimum size and up to max byte value range
		}
		
		/**
		 * Clears all key-value mappings from this map.
		 * <p>
		 * Resets the map to an empty state, removing all keys and values, including the null key mapping.
		 *
		 * @return This {@code RW} instance, allowing for method chaining.
		 */
		public RW< V > clear() {
			_clear(); // Clear ByteSet.R state
			java.util.Arrays.fill( values, 0, cardinality, null ); // Clear values array
			nullKeyValue = null; // Reset null key value
			nulls._clear(); // Clear nulls array
			return this;
		}
		
		/**
		 * Associates the specified value with the specified boxed {@link Byte} key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced. Allows {@code null} keys.
		 *
		 * @param key   The boxed {@link Byte} key with which the specified value is to be associated. Can be {@code null}.
		 * @param value The value to be associated with the specified key. Can be {@code null}.
		 * @return {@code true} if a new key-value mapping was added, {@code false} if the key already existed and the value was updated.
		 */
		public boolean put(  Byte      key, V value ) {
			if( key == null ) {
				nullKeyValue = value; // Set null key value
				return _add(); // Add null key entry (ByteSet.R logic)
			}
			return put( ( byte ) ( key + 0 ), value ); // Delegate to primitive byte key put method
		}
		
		/**
		 * Associates the specified value with the specified primitive {@code byte} key in this map.
		 * <p>
		 * If the map previously contained a mapping for this key, the old value is replaced.
		 * If the provided value is {@code null}, it effectively removes the mapping for the given key.
		 *
		 * @param key   The primitive {@code byte} key with which the specified value is to be associated.
		 * @param value The value to be associated with the specified key. If {@code null}, the mapping for the key is removed.
		 * @return {@code true} if a new key-value mapping was added, {@code false} if the key already existed and the value was updated.
		 */
		public boolean put( byte key, V value ) {
			
			if( value == null ) {
				if( nulls.set0( ( byte ) key ) )// if in the nulls exists it means the key is present
				{
					if( values.length == 256 ) return false;
					Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.cardinality + 1, -1 ); // Remove key from nulls array and resize values array if needed
					return false;
				}
				//maybe the key does not present
				return set1( ( byte ) key ); // return true if key was added, false otherwise (ByteSet.R logic, tracks key presence regardless of value)
			}
			
			if( !nulls.set1( ( byte ) key ) ) {
				values[ values.length == 256 ?
						key & 0xFF :
						nulls.rank( ( byte ) key ) - 1 ] = value;
				
				return false;
			}
			
			int i;
			if( values.length == 256 ) i = key & 0xFF;
			else if( nulls.cardinality == 128 ) {
				V[] newValues = equal_hash_V.copyOf( null, 256 );
				i = key & 0xFF;
				
				for( int ii = nulls.first1(), k = 0; -1 < ii; ii = nulls.next1( ii ), k++ )
				     newValues[ ii ] = values[ k ];
				values = newValues;
			}
			else {
				i = nulls.rank( ( byte ) key ) - 1;
				
				if( i + 1 < nulls.cardinality || values.length < nulls.cardinality )
					Array.resize( values, values.length < nulls.cardinality ?
							( values = equal_hash_V.copyOf( null, Math.min( values.length * 2, 0x100 ) ) ) :
							values, i, nulls.cardinality - 1, 1 );
			}
			values[ i ] = value;
			return set1( ( byte ) key ); // return true if key was added, false otherwise
		}
		
		/**
		 * Removes the mapping for a boxed {@link Byte} key from this map if it is present.
		 * <p>
		 * Also handles the removal of the null key mapping if the provided key is {@code null}.
		 *
		 * @param key The boxed {@link Byte} key whose mapping is to be removed from the map. Can be {@code null}.
		 * @return {@code true} if a mapping was removed as a result of this call, {@code false} if no mapping existed for the key.
		 */
		public boolean remove(  Byte      key ) {
			if( key != null ) return remove( ( byte ) ( key + 0 ) ); // Delegate to primitive byte key remove if key is not null
			
			nullKeyValue = null; // Reset null key value
			return _remove(); // Remove null key entry (ByteSet.R logic)
		}
		
		/**
		 * Removes the mapping for a primitive {@code byte} key from this map if it is present.
		 *
		 * @param key The primitive {@code byte} key whose mapping is to be removed from the map.
		 * @return {@code true} if a mapping was removed as a result of this call, {@code false} if no mapping existed for the key.
		 */
		public boolean remove( byte key ) {
			if( !set0( ( byte ) key ) ) return false; // Remove key from ByteSet.R, return false if key wasn't present
			
			if( nulls.set0( ( byte ) key ) ) // Remove key from nulls array if present
				if( values.length == 256 )
					values[ key & 0xFF ] = null;
				else
					Array.resize( values, values, nulls.rank( ( byte ) key ), nulls.cardinality + 1, -1 ); // Resize values array to remove the gap
			return true; // Mapping removed
		}
		
		/**
		 * Creates and returns a deep copy of this {@code RW} instance.
		 * <p>
		 * The clone will be a new {@code RW} map with the same key-value mappings as this map.
		 * The values themselves are still shallow-copied, similar to {@link R#clone()}.
		 *
		 * @return A cloned instance of this map.
		 */
		public RW< V > clone() { return ( RW< V > ) super.clone(); } // Simply cast the shallow clone from superclass to RW<V>
		
		// Static helper for equality/hashing of RW instances
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class ); // Static instance used for default EqualHashOf<RW<V>>
	}
	
	/**
	 * Provides a static method to obtain an {@link Array.EqualHashOf} instance for {@code RW<V>}.
	 * <p>
	 * This allows for custom equality and hashing comparisons of {@code RW<V>} map instances.
	 *
	 * @param <V> The value type of the {@code RW} map.
	 * @return An {@link Array.EqualHashOf} instance for {@code RW<V>}.
	 */
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() {
		return ( Array.EqualHashOf< RW< V > > ) RW.OBJECT; // Return the static OBJECT instance
	}
}