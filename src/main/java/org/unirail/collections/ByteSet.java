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

/**
 * {@code ByteSet} interface defines the contract for a set that efficiently stores byte values.
 * Implementations of this interface are designed to be memory-compact and optimized for byte data.
 * Some implementations may optionally support the inclusion of a single null element.
 */
public interface ByteSet {
	
	
	/**
	 * {@code R} is an abstract base class that provides a common implementation for {@code ByteSet} interfaces.
	 * It utilizes a bitset approach for efficient storage of byte values (0-255).
	 * The implementation uses four 64-bit long fields to represent the bitset, each covering a range of 64 byte values.
	 * This class also manages set size, null key presence, and token-based iteration for thread-safety and modification tracking.
	 */
	abstract class R extends Array.FF implements Cloneable, JsonWriter.Source {
		
		public boolean hasNullKey() { return hasNullKey; }
		
		protected boolean hasNullKey; // A flag indicating whether the set contains a null key.
		
		// Constants and fields for token-based iteration. Tokens are used for safe iteration and modification detection.
		protected static final int  KEY_MASK      = 0x1_FF; // Bits 0-9 for key
		protected static final int  KEY_LEN       = 9; // Bits 0-9 for key
		protected static final int  VERSION_SHIFT = 32;                    // Number of bits to shift to get the version from an iteration token.
		protected static final long INVALID_TOKEN = -1L;                   // Special token value (-1) indicating an invalid iteration state or end of iteration.
		
		/**
		 * Returns the total number of elements in the set.
		 * If the set contains a null key, it is included in the count.
		 *
		 * @return The total size of the set, including the null key if present.
		 */
		public int size() {
			return hasNullKey ?
			       cardinality + 1 :
			       cardinality;
		}
		
		/**
		 * Checks if the set is empty.
		 * A set is considered empty if it contains no elements and does not have a null key.
		 *
		 * @return {@code true} if the set is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() { return cardinality == 0 && !hasNullKey; }
		
		/**
		 * Checks if the set contains the specified byte value, which can be a boxed {@code Byte} or null.
		 * Handles null key checking if the implementation supports null keys.
		 *
		 * @param key The {@code Byte} value to check for presence in the set. Can be null.
		 * @return {@code true} if the set contains the specified value or null key (if {@code key} is null), {@code false} otherwise.
		 */
		public boolean contains(  Byte      key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the set contains the specified primitive byte value.
		 * This method is optimized for primitive byte values and does not handle null keys.
		 *
		 * @param key The primitive byte value to check for presence in the set (valid range is 0-255).
		 * @return {@code true} if the set contains the specified byte value, {@code false} otherwise.
		 */
		public boolean contains( byte key ) { return is1( ( byte ) key ); }
		
		/**
		 * Returns the first valid iteration token for traversing the set.
		 * This token points to the smallest byte value present in the set or the null key if present and no byte values exist.
		 * May return {@link #INVALID_TOKEN} (-1) if the set is empty (no elements and no null key).
		 *
		 * @return The first iteration token, or {@link #INVALID_TOKEN} (-1) if the set is empty.
		 */
		public long token() {
			return 0 < cardinality ?
			       tokenOf( next1( -1 ) ) :
			       hasNullKey ?
			       token( KEY_MASK ) :
			       INVALID_TOKEN;
		}
		
		/**
		 * Returns the next iteration token in the sequence, starting from the given {@code token}.
		 * This method is used to iterate through the elements of the set in ascending order of byte values,
		 * followed by the null key (if present). May return {@link #INVALID_TOKEN} (-1) if there are no more
		 * elements to iterate or if the provided token is invalid (e.g., due to set modification).
		 *
		 * @param token The current iteration token.
		 * @return The next iteration token, or {@link #INVALID_TOKEN} (-1) if there are no more elements or the token is invalid.
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN || token >>> VERSION_SHIFT != _version ) return INVALID_TOKEN;
			
			int key = ( int ) ( token & KEY_MASK ); // Extract the key (byte value or 256 for null) from the token.
			return 0xFF < key ?
			       INVALID_TOKEN :
			       ( key = next1( key ) ) == -1 ?
			       hasNullKey ?
			       token( KEY_MASK ) :
			       INVALID_TOKEN :
			       token( token, key );
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
		public int unsafe_token( final int token ) { return next1( token ); }
		
		/**
		 * Checks if the given iteration {@code token} represents the null key.
		 *
		 * @param token The iteration token to check.
		 * @return {@code true} if the token is valid and represents the null key, {@code false} otherwise.
		 */
		public boolean isKeyNull( long token ) { return ( token & KEY_MASK ) == KEY_MASK; }
		
		/**
		 * Gets the byte value associated with the given iteration {@code token}.  Before calling this method ensure that this token is not point to the isKeyNull
		 *
		 * @param token The iteration token from which to extract the byte value.
		 * @return The byte value represented by the token, or 0 if the token is for a null key or invalid.
		 */
		public byte key( long token ) { return ( byte )  ( KEY_MASK & token ); }
		
		public long token( int key )                { return ( long ) _version << VERSION_SHIFT | key; }
		
		protected long token( long token, int key ) { return ( long ) _version << VERSION_SHIFT | key; }
		
		protected long tokenOf( int key )           { return ( long ) _version << VERSION_SHIFT | key; }
		
		/**
		 * Retrieves the iteration token for a boxed {@code Byte} value.
		 * Handles null keys. May return {@link #INVALID_TOKEN} (-1) if the value is not in the set.
		 *
		 * @param key The boxed {@code Byte} value to find the token for (can be null).
		 * @return The iteration token for the given value, or {@link #INVALID_TOKEN} (-1) if the value is not in the set.
		 */
		public long tokenOf( Byte key ) {
			return key == null ?
			       hasNullKey ?
			       // Check if null key is present in the set.
			       token( KEY_MASK ) :
			       // Return token for null key.
			       INVALID_TOKEN :
			       tokenOf( ( byte ) ( key + 0 ) ); // Unbox Byte and call primitive tokenOf method. Adding 0 ensures byte conversion.
		}
		
		/**
		 * Retrieves the iteration token for a primitive byte value.
		 * May return {@link #INVALID_TOKEN} (-1) if the value is not in the set.
		 *
		 * @param key The primitive byte value to find the token for.
		 * @return The iteration token for the given value, or {@link #INVALID_TOKEN} (-1) if the value is not in the set.
		 */
		public long tokenOf( byte key ) {
			return is1( ( byte ) key ) ?
			       tokenOf( key & 0xFF ) :
			       INVALID_TOKEN; // Byte value not present, return invalid token.
		}
		
		
		/**
		 * Checks if this set contains all elements of another {@code ByteSet.R} set (subset relationship).
		 *
		 * @param subset The {@code ByteSet.R} set to check if it is a subset of this set.
		 * @return {@code true} if this set contains all elements of the {@code subset}, {@code false} otherwise.
		 */
		public boolean containsAll( R subset ) {
			long b;
			return subset == null ||
			       subset.size() <= size() &&
			       ( hasNullKey || !subset.hasNullKey ) &&
			       ( segments[ 0 ] & ( b = subset.segments[ 0 ] ) ) == b &&
			       ( segments[ 1 ] & ( b = subset.segments[ 1 ] ) ) == b &&
			       ( segments[ 2 ] & ( b = subset.segments[ 2 ] ) ) == b &&
			       ( segments[ 3 ] & ( b = subset.segments[ 3 ] ) ) == b;
		}
		
		/**
		 * Generates a hash code for this {@code ByteSet.R}.
		 * The hash code is calculated based on the bitset representation and the presence of a null key, ensuring that sets with the same elements have the same hash code.
		 *
		 * @return The hash code for this set.
		 */
		public int hashCode() {
			return Array.finalizeHash( hasNullKey ?
			                           184889743 :
			                           22633363, super.hashCode() );
		}
		
		/**
		 * Checks if this {@code ByteSet.R} is equal to another object.
		 * It performs type checking and then delegates to {@link #equals(R)} for content comparison.
		 *
		 * @param obj The object to compare with.
		 * @return {@code true} if the objects are equal, {@code false} otherwise.
		 */
		public boolean equals( Object obj ) {
			return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) ); // Type check and then content equality check.
		}
		
		/**
		 * Performs a deep equality check with another {@code ByteSet.R}.
		 * Two sets are considered equal if they have the same size, contain the same byte values, and have the same null key status.
		 * The comparison is optimized by directly comparing the underlying long fields.
		 *
		 * @param other The {@code ByteSet.R} to compare with.
		 * @return {@code true} if the sets are equal, {@code false} otherwise.
		 */
		public boolean equals( R other ) {
			return other == this || other != null && cardinality == other.cardinality && hasNullKey == other.hasNullKey && super.equals( other );
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code ByteSet.R} instance.
		 * The copy will have the same elements and null key status as the original set.
		 *
		 * @return A shallow copy of this set.
		 */
		public R clone() {
			try {
				return ( R ) super.clone(); // Use Object's clone method for shallow copy.
			} catch( CloneNotSupportedException e ) {
				e.printStackTrace(); // Handle exception if cloning is not supported (shouldn't happen for Cloneable).
			}
			return null; // Return null in case of exception (though unlikely).
		}
		
		/**
		 * Returns a JSON string representation of this {@code ByteSet.R}.
		 *
		 * @return A JSON string representing this set.
		 */
		public String toString() { return toJSON(); } // Convert to JSON and then to string.
		
		@Override
		public void toJSON( JsonWriter json ) {
			json.enterObject(); // Start JSON object representation.
			
			if( hasNullKey ) json.name().value(); // If null key present, add a null entry (name is omitted for null key).
			
			if( 0 < cardinality ) { // If there are byte values in the set.
				json.preallocate( cardinality * 10 ); // Pre-allocate buffer for JSON string to improve performance.
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) // Iterate through the set using tokens.
				     json.name( key( token ) ).value(); // For each element, add a name-value pair to the JSON object, using byte value as name.
			}
			json.exitObject(); // End JSON object representation.
		}
		
		/**
		 * Adds a boxed {@code Byte} value to the set.
		 * If the value is null, it adds a null key to the set (if supported by the implementation).
		 *
		 * @param key The {@code Byte} value to add to the set. Can be null.
		 * @return {@code true} if the set was modified as a result of this operation (i.e., the value was not already present), {@code false} otherwise.
		 */
		protected boolean _add(  Byte      key ) {
			return key == null ?
			       _add() :
			       set1( ( byte ) ( key + 0 ) );
		}
		
		protected boolean _add() {
			if( hasNullKey ) return false; // If null key already present, no modification.
			hasNullKey = true; // Set null key flag.
			_version++; // Increment version to invalidate tokens.
			return true; // Set was modified by adding null key.
		}
		
		
		/**
		 * Removes a boxed {@code Byte} value from the set.
		 * If the value is null, it removes the null key from the set (if present).
		 *
		 * @param key The {@code Byte} value to remove from the set. Can be null.
		 * @return {@code true} if the set was modified as a result of this operation (i.e., the value was present), {@code false} otherwise.
		 */
		protected boolean _remove(  Byte      key ) {
			return key == null ?
			       _remove() :
			       set0( ( byte ) ( key + 0 ) );
		}
		
		protected boolean _remove() {
			if( !hasNullKey ) return false; // Corrected logic: if no null key, no removal.
			hasNullKey = false;
			_version++;
			return true;
			
		}
		
		/**
		 * Clears all byte values and the null key (if present) from the set, making it empty.
		 * Returns the instance itself for method chaining.
		 *
		 * @return This {@code RW} instance after clearing all elements.
		 */
		protected void _clear() {
			hasNullKey = false;
			super._clear();
		}
		
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
	}
	
	/**
	 * {@code RW} is a mutable implementation of {@code ByteSet} based on the abstract class {@code R}.
	 * It provides read and write operations for modifying the set, including adding, removing, and clearing elements.
	 * It supports null keys and inherits the efficient bitset representation from {@code R}.
	 */
	class RW extends R {
		/**
		 * Constructs an empty {@code RW} ByteSet.
		 */
		public RW() { }
		
		/**
		 * Constructs a {@code RW} ByteSet initialized with the given primitive byte values.
		 *
		 * @param items An array of primitive byte values to initialize the set with.
		 */
		public RW( byte... items ) { for( byte i : items ) add( i ); } // Add each byte value from the array to the set.
		
		/**
		 * Constructs a {@code RW} ByteSet initialized with the given boxed {@code Byte} values.
		 * Handles null values in the input array by adding a null key to the set if a null value is encountered.
		 *
		 * @param items An array of boxed {@code Byte} values to initialize the set with. Can contain null values.
		 */
		public RW(  Byte     [] items ) { for(  Byte      key : items ) add( key ); } // Add each Byte value from the array to the set.
		
		/**
		 * Adds a boxed {@code Byte} value to the set.
		 * If the value is null, it adds a null key to the set (if supported by the implementation).
		 *
		 * @param key The {@code Byte} value to add to the set. Can be null.
		 * @return {@code true} if the set was modified as a result of this operation (i.e., the value was not already present), {@code false} otherwise.
		 */
		public boolean add(  Byte      key ) { return _add( key ); }
		
		/**
		 * Adds a primitive byte value to the set.
		 *
		 * @param key The primitive byte value to add to the set (valid range is 0-255).
		 * @return {@code true} if the set was modified as a result of this operation (i.e., the value was not already present), {@code false} otherwise.
		 */
		public boolean add( final byte key ) { return set1( ( byte ) key ); }
		
		
		/**
		 * Removes a boxed {@code Byte} value from the set.
		 * If the value is null, it removes the null key from the set (if present).
		 *
		 * @param key The {@code Byte} value to remove from the set. Can be null.
		 * @return {@code true} if the set was modified as a result of this operation (i.e., the value was present), {@code false} otherwise.
		 */
		public boolean remove(  Byte      key ) { return _remove( key ); }
		
		/**
		 * Removes a primitive byte key from the set.
		 *
		 * @param key The primitive byte key to remove from the set (valid range is 0-255).
		 * @return {@code true} if the set was modified as a result of this operation (i.e., the key was present), {@code false} otherwise.
		 */
		public boolean remove( byte key ) { return set0( ( byte ) key ); }
		
		/**
		 * Clears all byte values and the null key (if present) from the set, making it empty.
		 * Returns the instance itself for method chaining.
		 *
		 * @return This {@code RW} instance after clearing all elements.
		 */
		public RW clear() {
			_clear();
			return this; // Return instance for chaining.
		}
		
		/**
		 * Retains only the elements in this set that are also present in the specified source set.
		 * In other words, this operation performs an intersection with the source set.
		 *
		 * @param src The source {@code ByteSet.R} set to retain elements from.
		 * @return {@code true} if this set was modified as a result of this operation, {@code false} otherwise.
		 */
		public boolean retainAll( R src ) {
			boolean modified = false; // Flag to track if any modification occurred.
			long    _0, _1, _2, _3, b;
			
			if( ( _0 = segments[ 0 ] ) != ( b = src.segments[ 0 ] ) ) {
				segments[ 0 ] = _0 &= b;
				                modified = true;
			}
			if( ( _1 = segments[ 1 ] ) != ( b = src.segments[ 1 ] ) ) {
				segments[ 1 ] = _1 &= b;
				                modified = true;
			}
			if( ( _2 = segments[ 2 ] ) != ( b = src.segments[ 2 ] ) ) {
				segments[ 2 ] = _2 &= b;
				                modified = true;
			}
			if( ( _3 = segments[ 3 ] ) != ( b = src.segments[ 3 ] ) ) {
				segments[ 3 ] = _3 &= b;
				                modified = true;
			}
			
			if( modified ) {
				
				cardinality = Long.bitCount( _0 ) +
				              Long.bitCount( _1 ) +
				              Long.bitCount( _2 ) +
				              Long.bitCount( _3 );
				_version++;
			}
			
			if( !hasNullKey || src.hasNullKey ) return modified;
			hasNullKey = false;
			
			_version++;
			
			return true; // Return modification status.
		}
		
		/**
		 * Adds all elements from the specified source set to this set.
		 * This operation performs a union with the source set.
		 *
		 * @param src The source {@code ByteSet.R} set to add elements from.
		 * @return This {@code RW} instance after adding all elements from the source set.
		 */
		public RW addAll( R src ) {
			boolean modified = false; // Flag to track if any modification occurred.
			long    _0, _1, _2, _3, b;
			
			if( ( _0 = segments[ 0 ] ) != ( b = src.segments[ 0 ] ) ) {
				segments[ 0 ] = _0 |= b;
				                modified = true;
			}
			if( ( _1 = segments[ 1 ] ) != ( b = src.segments[ 1 ] ) ) {
				segments[ 1 ] = _1 |= b;
				                modified = true;
			}
			if( ( _2 = segments[ 2 ] ) != ( b = src.segments[ 2 ] ) ) {
				segments[ 2 ] = _2 |= b;
				                modified = true;
			}
			if( ( _3 = segments[ 3 ] ) != ( b = src.segments[ 3 ] ) ) {
				segments[ 3 ] = _3 |= b;
				                modified = true;
			}
			
			if( modified ) {
				
				cardinality = Long.bitCount( _0 ) +
				              Long.bitCount( _1 ) +
				              Long.bitCount( _2 ) +
				              Long.bitCount( _3 );
				_version++;
			}
			
			if( hasNullKey || !src.hasNullKey ) return this;
			hasNullKey = true;
			_version++;
			
			return this;
		}
		
		/**
		 * Creates and returns a shallow copy of this mutable {@code RW} ByteSet.
		 *
		 * @return A shallow copy of this {@code RW} set.
		 */
		public RW clone() { return ( RW ) super.clone(); } // Return shallow copy using super.clone().
	}
}