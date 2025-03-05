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
		
		
		protected boolean hasNullKey; // A flag indicating whether the set contains a null key.
		
		// Constants and fields for token-based iteration. Tokens are used for safe iteration and modification detection.
		protected static final long KEY_MASK      = 0x1_FFL; // Bits 0-9 for key
		protected static final long RANK_MASK     = 0x3_FE_00L; // Bits 9-18 for rank
		protected static final int  RANK_SHIFT    = 9;                      // Shift for key position
		protected static final int  VERSION_SHIFT = 32;                    // Number of bits to shift to get the version from an iteration token.
		protected static final long VERSION_MASK  = 0xFFFF_FFFF_0000_0000L;                    // Number of bits to shift to get the version from an iteration token.
		
		protected static final long INVALID_TOKEN = -1L;                   // Special token value indicating an invalid iteration state or end of iteration.
		
		
		/**
		 * Returns the total number of elements in the set.
		 * If the set contains a null key, it is included in the count.
		 *
		 * @return The total size of the set, including the null key if present.
		 */
		public int size() {
			return hasNullKey ?
					size + 1 :
					size;
		}
		
		/**
		 * Checks if the set is empty.
		 * A set is considered empty if it contains no elements and does not have a null key.
		 *
		 * @return {@code true} if the set is empty, {@code false} otherwise.
		 */
		public boolean isEmpty() { return size == 0 && !hasNullKey; }
		
		/**
		 * Checks if the set contains the specified byte value, which can be a boxed {@code Byte} or null.
		 * Handles null key checking if the implementation supports null keys.
		 *
		 * @param key The {@code Byte} value to check for presence in the set. Can be null.
		 * @return {@code true} if the set contains the specified value or null key (if {@code key} is null), {@code false} otherwise.
		 */
		public boolean contains(  Byte      key ) {
			return key == null ?
					hasNullKey :
					contains( ( byte ) ( key + 0 ) ); // Unbox Byte and call primitive contains method. Adding 0 ensures byte conversion.
		}
		
		/**
		 * Checks if the set contains the specified primitive byte value.
		 * This method is optimized for primitive byte values and does not handle null keys.
		 *
		 * @param key The primitive byte value to check for presence in the set (valid range is 0-255).
		 * @return {@code true} if the set contains the specified byte value, {@code false} otherwise.
		 */
		protected boolean contains( byte key ) { return get( ( byte ) key ); }
		
		/**
		 * Returns the first valid iteration token for traversing the set.
		 * This token points to the smallest byte value present in the set or the null key if present and no byte values exist.
		 * Returns {@link #INVALID_TOKEN} if the set is empty (no elements and no null key).
		 *
		 * @return The first iteration token, or {@link #INVALID_TOKEN} if the set is empty.
		 */
		public long token() {
			return 0 < size ?
					token( 1, first1() ) :
					hasNullKey ?
							// If no byte values but null key is present.
							token_nullKey() :
							INVALID_TOKEN;// If the set is not empty (contains byte values).
		}
		
		/**
		 * Returns the next iteration token in the sequence, starting from the given {@code token}.
		 * This method is used to iterate through the elements of the set in ascending order of byte values, followed by the null key (if present).
		 * Throws {@link ConcurrentModificationException} if the provided {@code token} is no longer valid due to modifications made to the set since the token was obtained.
		 *
		 * @param token The current iteration token.
		 * @return The next iteration token, or {@link #INVALID_TOKEN} if there are no more elements in the set.
		 * @throws ConcurrentModificationException if the provided {@code token} is invalid.
		 */
		public long token( long token ) {
			if( !isValid( token ) ) // Check if the provided token is valid for the current version of the set.
				throw new ConcurrentModificationException( "Collection was modified; token is no longer valid." );
			
			int key = ( int ) ( token & KEY_MASK ); // Extract the index (byte value or 256 for null) from the token.
			if( 0xFF < key ) return INVALID_TOKEN; // If the current token is for the null key, no next element exists.
			
			if( key + 1 < size ) return token_next_existing_key( token, next1( key ) );
			
			return hasNullKey ?
					// If no more byte values, check for null key.
					token_nullKey() :
					// Return token for null key.
					INVALID_TOKEN; // No more elements (neither byte values nor null key).
		}
		
		/**
		 * Checks if the given iteration {@code token} represents the null key.
		 *
		 * @param token The iteration token to check.
		 * @return {@code true} if the token is valid and represents the null key, {@code false} otherwise.
		 */
		boolean isKeyNull( long token ) { return isValid( token ) && 0xFF < ( KEY_MASK & token ); }
		
		/**
		 * Gets the byte value associated with the given iteration {@code token}.
		 * Throws {@link ConcurrentModificationException} if the token is invalid.
		 * For null key tokens, it returns 0 (as byte). Use {@link #isKeyNull(long)} to differentiate between byte value 0 and null key.
		 *
		 * @param token The iteration token from which to extract the byte value.
		 * @return The byte value represented by the token. Returns 0 if the token is for a null key.
		 * @throws ConcurrentModificationException if the provided {@code token} is invalid.
		 */
		public byte key( long token ) {
			// Check token validity and if it's a null key token.
			if( isKeyNull( token ) ) throw new RuntimeException( "The key of this token is null" );
			return ( byte )  ( KEY_MASK & token );    // Return the extracted index as byte value.
		}
		
		protected long token_nullKey()                    { return ( long ) _version << VERSION_SHIFT | 0x100; }
		
		protected long token( int key )                   { return ( long ) _version << VERSION_SHIFT | key & 0xFF; }
		
		protected long token_next_existing_key( long token, int key ) { return token & VERSION_MASK | ( token & KEY_MASK ) + 1L << RANK_SHIFT | key; }//step on next existing key
		
		protected long token( int rank, int key )         { return ( long ) _version << VERSION_SHIFT | ( long ) rank << 9 | key; }
		
		protected int rank( long token )                  { return ( int ) ( token & RANK_MASK ) >>> RANK_SHIFT; }
		
		
		/**
		 * Checks if the given iteration {@code token} is valid for the current version of the set.
		 * A token is valid if it's not {@link #INVALID_TOKEN} and its embedded version matches the current set's version.
		 *
		 * @param token The iteration token to validate.
		 * @return {@code true} if the token is valid, {@code false} otherwise.
		 */
		public boolean isValid( long token ) { return token != INVALID_TOKEN && token >>> VERSION_SHIFT == _version; }
		
		/**
		 * Retrieves the iteration token for a boxed {@code Byte} value.
		 * Handles null keys.
		 *
		 * @param key The boxed {@code Byte} value to find the token for (can be null).
		 * @return The iteration token for the given value, or {@link #INVALID_TOKEN} if the value is not in the set.
		 */
		public long tokenOf( Byte key ) {
			return key == null ?
					hasNullKey ?
							// Check if null key is present in the set.
							token_nullKey() :
							// Return token for null key.
							INVALID_TOKEN :
					// Null key not present, return invalid token.
					tokenOf( ( byte ) ( key + 0 ) ); // Unbox Byte and call primitive tokenOf method. Adding 0 ensures byte conversion.
		}
		
		/**
		 * Retrieves the iteration token for a primitive byte value.
		 *
		 * @param key The primitive byte value to find the token for.
		 * @return The iteration token for the given value, or {@link #INVALID_TOKEN} if the value is not in the set.
		 */
		public long tokenOf( byte key ) {
			return contains(  key ) ?
					// Check if the set contains the byte value.
					token( key ) :
					// Create and return a token for the byte value.
					INVALID_TOKEN; // Byte value not present, return invalid token.
		}
		
		
		/**
		 * Checks if this set contains all elements of another {@code ByteSet.R} set (subset relationship).
		 *
		 * @param subset The {@code ByteSet.R} set to check if it is a subset of this set.
		 * @return {@code true} if this set contains all elements of the {@code subset}, {@code false} otherwise.
		 */
		public boolean containsAll( R subset ) {
			return subset == null ||
			       subset.size() <= size() &&
			       ( hasNullKey || !subset.hasNullKey ) &&
			       ( _1 & subset._1 ) == subset._1 &&
			       ( _2 & subset._2 ) == subset._2 &&
			       ( _3 & subset._3 ) == subset._3 &&
			       ( _4 & subset._4 ) == subset._4;  // If 'subset' is larger, or if 'subset' has null key but this doesn't, it cannot be a subset.
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
			if( size != other.size || hasNullKey != other.hasNullKey ) return false; // Quick check for size and null key status.
			return _4 == other._4 && _3 == other._3 && _2 == other._2 && _1 == other._1; // Compare the four long segments for equality.
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
			if( 0 < size ) { // If there are byte values in the set.
				json.preallocate( size * 10 ); // Pre-allocate buffer for JSON string to improve performance.
				for( long token = token(); ( token & KEY_MASK ) < 0x100; token = token( token ) ) // Iterate through the set using tokens.
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
					_add( ( byte ) ( key + 0 ) );
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
					_remove( ( byte ) ( key + 0 ) );
		}
		protected boolean _remove(  ) {
		if( hasNullKey ) return false;
				
					hasNullKey = true;
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
		public RW(  Byte     ... items ) { for(  Byte      key : items ) add( key ); } // Add each Byte value from the array to the set.
		
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
		public boolean add( final byte key ) { return _add( key ); }
		
		
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
		public boolean remove( byte key ) { return _remove( key ); }
		
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
			boolean ret = false; // Flag to track if any modification occurred.
			if( _1 != src._1 ) { _1 &= src._1; ret = true; } // Intersect _1 with src._1, set modification flag if changed.
			if( _2 != src._2 ) { _2 &= src._2; ret = true; } // Intersect _2 with src._2, set modification flag if changed.
			if( _3 != src._3 ) { _3 &= src._3; ret = true; } // Intersect _3 with src._3, set modification flag if changed.
			if( _4 != src._4 ) { _4 &= src._4; ret = true; } // Intersect _4 with src._4, set modification flag if changed.
			if( ret ) {
				size = Math.max( size, src.size ); // Size might change after intersection.
				_version++; // Increment version if modified.
			}
			return ret; // Return modification status.
		}
		
		/**
		 * Adds all elements from the specified source set to this set.
		 * This operation performs a union with the source set.
		 *
		 * @param src The source {@code ByteSet.R} set to add elements from.
		 * @return This {@code RW} instance after adding all elements from the source set.
		 */
		public RW addAll( R src ) {
			_1 |= src._1;
			_2 |= src._2;
			_3 |= src._3;
			_4 |= src._4; // Perform bitwise OR to union the bitsets.
			size = Math.max( src.size, size ); // Update size to the maximum of current and source size.
			if( src.hasNullKey ) hasNullKey = true; // If source has null key, add null key to this set.
			return this; // Return instance for chaining.
		}
		
		/**
		 * Creates and returns a shallow copy of this mutable {@code RW} ByteSet.
		 *
		 * @return A shallow copy of this {@code RW} set.
		 */
		public RW clone() { return ( RW ) super.clone(); } // Return shallow copy using super.clone().
	}
}