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
 * {@code CharSet} is a specialized Set interface designed for efficient storage and manipulation of char/short keys (65,536 distinct values).
 * It provides optimized operations for adding, removing, checking for existence, and iterating over char elements.
 * Implements a HYBRID strategy:
 * 1. Starts as a hash set with separate chaining, optimized for sparse data.
 * Uses short[] for next pointers, limiting this phase's capacity.
 * 2. Automatically transitions to a direct-mapped flat bitset when the hash set
 * reaches its capacity limit (~32,749 entries) and needs to grow further.
 * This approach balances memory efficiency for sparse sets with guaranteed O(1)
 * performance and full key range support for dense sets.
 */
public interface ShortSet {
	
	/**
	 * {@code R} is a read-only abstract base class that implements the core functionalities and state management for {@code CharSet}.
	 * It handles the underlying structure which can be either a hash set or a flat bitset.
	 * It serves as a foundation for read-write implementations and provides methods for querying the set without modification.
	 * This class is designed to be lightweight and efficient for read-heavy operations.
	 * <p>
	 * It implements {@link JsonWriter.Source} to support JSON serialization and {@link Cloneable} for creating copies of the set.
	 */
	abstract class R implements JsonWriter.Source, Cloneable {
		/**
		 * Indicates whether the Set contains a null key. Null keys are handled separately.
		 */
		
		public boolean hasNullKey() { return hasNullKey; }
		
		protected boolean        hasNullKey;          // Indicates if the map contains a null key
		protected char[]         _buckets;            // Hash table buckets array (1-based indices to chain heads).
		protected short[]        nexts;               // Links within collision chains (-1 termination, -2 unused, <-2 free list link).
		protected short[] keys; // Keys array.
		protected int            _count;              // Hash mode: Total slots used (entries + free slots). Flat mode: Number of set bits (actual entries).
		protected int            _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int            _freeCount;          // Number of free entries in the free list.
		protected int            _version;            // Version counter for concurrent modification detection.
		
		protected static final int StartOfFreeList = -3; // Marks the start of the free list in 'nexts' field.
		protected static final int VERSION_SHIFT   = 32; // Bits to shift version in token.
		// Special index used in tokens to represent the null key. Outside valid array index ranges.
		protected static final int NULL_KEY_INDEX  = 0x1_FFFF; // 65537
		
		protected static final long INVALID_TOKEN = -1L; // Invalid token constant.
		
		
		/**
		 * Flag indicating if the set is operating in flat bitset mode.
		 */
		protected boolean isFlatStrategy() { return nulls != null; }
		
		/**
		 * Bitset to track presence of keys in flat mode. Size 1024 longs = 65536 bits.
		 */
		protected long[] nulls; // Size: 65536 / 64 = 1024
		
		// Constants for Flat Mode
		protected static final int FLAT_ARRAY_SIZE = 0x10000;
		protected static final int NULLS_SIZE      = FLAT_ARRAY_SIZE / 64; // 1024
		
		
		/**
		 * Returns the number of elements in this set (its cardinality).
		 * Corresponds to {@code Set.size()}.
		 *
		 * @return the number of elements in this set
		 */
		public int size() {
			// Hash Mode: _count includes free slots, subtract _freeCount for actual entries.
			// Flat Mode: _count is the number of set bits (actual entries).
			// Add 1 if the null key is present in either mode.
			return (
					       isFlatStrategy() ?
							       _count :
							       _count - _freeCount ) + (
					       hasNullKey ?
							       1 :
							       0 );
		}
		
		/**
		 * Returns the number of elements in this set. This is an alias for {@link #size()}.
		 *
		 * @return the number of elements in this set
		 */
		public int count() { return size(); }
		
		/**
		 * Returns {@code true} if this set contains no elements.
		 *
		 * @return {@code true} if this set contains no elements
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the allocated capacity of the internal structure.
		 * In hash set mode, it's the length of the internal arrays.
		 * In flat mode, it's the fixed size (65536).
		 *
		 * @return The capacity.
		 */
		public int length() {
			return isFlatStrategy() ?
					FLAT_ARRAY_SIZE :
					( nexts == null ?
							0 :
							nexts.length );
		}
		
		
		/**
		 * Checks if this set contains the specified key. Handles null keys.
		 *
		 * @param key the key to check for in this set (boxed Character).
		 * @return {@code true} if this set contains the specified key
		 */
		public boolean contains(  Short     key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if this set contains the specified primitive char key.
		 *
		 * @param key the primitive char key (0 to 65535) to check for in this set
		 * @return {@code true} if this set contains the specified char key
		 */
		public boolean contains( short key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		
		/**
		 * Returns a token for the specified key if it exists in the set, otherwise returns {@link #INVALID_TOKEN}.
		 * Tokens are used for efficient iteration and element access. Handles null keys.
		 *
		 * @param key the key to get the token for (can be null, boxed Character)
		 * @return a valid token if the key is in the set, -1 ({@link #INVALID_TOKEN}) if not found
		 */
		public long tokenOf(  Short     key ) {
			return key == null ?
					( hasNullKey ?
							token( NULL_KEY_INDEX ) :
							// Use special index for null key token
							INVALID_TOKEN ) :
					tokenOf( ( short ) ( key + 0 ) );
		}
		
		
		/**
		 * Returns a token for the specified primitive char key if it exists in the set, otherwise returns {@link #INVALID_TOKEN}.
		 * Tokens are used for efficient iteration and element access.
		 *
		 * @param key the primitive char key to get the token for
		 * @return a valid token if the key is in the set, -1 ({@link #INVALID_TOKEN}) if not found
		 */
		public long tokenOf( short key ) {
			if( isFlatStrategy() )
				return exists( ( char ) key ) ?
						token( ( char ) key ) :
						INVALID_TOKEN;
			
			if( _buckets == null || size() == 0 ) return INVALID_TOKEN; // Check size() to account for only null key present
			
			int hash = Array.hash( key );
			int i    = ( _buckets[ bucketIndex( hash ) ] ) - 1;
			
			for( int collisionCount = 0; ( i & 0xFFFF_FFFFL ) < nexts.length; ) {
				if( keys[ i ] == key ) return token( i );
				i = nexts[ i ];
				if( nexts.length < ++collisionCount ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			return INVALID_TOKEN;
		}
		
		
		/**
		 * Returns a token representing the first element in the set for iteration. Starts with the first non-null key.
		 * If only the null key exists, returns the null key token.
		 *
		 * @return a token for the first element, or -1 ({@link #INVALID_TOKEN}) if the set is empty
		 */
		public long token() {
			int index = unsafe_token( -1 );
			
			return index == -1 ?
					hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN :
					token( index );
		}
		
		
		/**
		 * Returns a token representing the next element in the set after the element associated with the given token.
		 *
		 * @param token the token of the current element
		 * @return a token for the next element, or -1 ({@link #INVALID_TOKEN}) if no next element exists or if the token is invalid due to structural modification
		 */
		public long token( final long token ) {
			if( token == INVALID_TOKEN ) throw new IllegalArgumentException( "Invalid token argument: INVALID_TOKEN" );
			if( version( token ) != _version ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			
			int index = index( token );
			if( index == NULL_KEY_INDEX ) return INVALID_TOKEN;
			index = unsafe_token( index );
			
			return index == -1 ?
					hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN :
					token( index );
		}
		
		
		/**
		 * Returns the next token for fast, <strong>unsafe</strong> iteration over <strong>non-null keys only</strong>,
		 * skipping concurrency and modification checks.
		 *
		 * <p>Start iteration with {@code unsafe_token(-1)}, then pass the returned token back to get the next one.
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()}.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * set is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token (index), or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #key(long) To get the key associated with a token (use carefully with null key token).
		 */
		public int unsafe_token( final int token ) {
			if( isFlatStrategy() )
				return next1( index( token ) );
			else
				for( int i = token + 1; i < _count; i++ )
					if( -2 < nexts[ i ] ) return i;
			
			return -1; // No more entries
		}
		
		
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Returns the char key associated with the given token.  Before calling this method ensure that this token is not point to the isKeyNull
		 *
		 * @param token the token of the element
		 * @return the char key associated with the token, or `(char) 0` if the token represents a null key
		 */
		public short key( long token ) {
			return ( short ) (short) (
					isFlatStrategy() ?
							index( token ) :
							keys[ index( token ) ] );
		}
		
		/**
		 * Computes the hash code for this set.
		 * The hash code is calculated based on the keys in the set and the set's size.
		 * It iterates through the set using tokens and accumulates hash values using a mixing function.
		 *
		 * @return the hash code value for this set
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			// Use unsafe iteration for potentially better performance, assuming no concurrent modification during hashCode calculation.
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				final int h = Array.hash( isFlatStrategy() ?
						                          token :
						                          // Key is the index itself in flat mode
						                          keys[ token ] ); // Key from array in hash mode
				a += h;
				b ^= h;
				c *= h | 1;
			}
			if( hasNullKey ) {
				final int h = Array.hash( seed ); // Hash the seed for the null key presence
				a += h;
				b ^= h;
				c *= h | 1;
			}
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() ); // Finalize the hash with size and seed
		}
		
		/**
		 * Seed value used in hashCode calculation. Initialized with the hashCode of the class.
		 */
		private static final int seed = R.class.hashCode();
		
		/**
		 * Compares this set to the specified object for equality.
		 *
		 * @param obj the object to compare with
		 * @return {@code true} if the specified object is an {@code R} instance with the same keys
		 */
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		/**
		 * Compares this set to another {@code R} instance for equality.
		 *
		 * @param other the {@code R} instance to compare with
		 * @return {@code true} if the sets contain the same keys
		 */
		public boolean equals( R other ) {
			if( other == this ) return true; // Same instance check
			if( other == null || other.size() != size() || other.hasNullKey != hasNullKey ) return false; // Size and null key presence check
			
			// Use unsafe iteration for potentially better performance
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				if( !other.contains(
						( short ) (short)( isFlatStrategy() ?
								token :
								keys[ token ] ) ) ) return false; // Check if each key in this set is present in the other set
			return true; // All keys match
		}
		
		/**
		 * Creates and returns a shallow copy of this {@code R} instance.
		 * The underlying data arrays (or bitset) are cloned.
		 *
		 * @return a clone of this {@code R} instance
		 */
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				if( isFlatStrategy() ) {
					dst.nulls = nulls.clone();
				}
				else if( _buckets != null ) {
					dst._buckets = _buckets.clone();
					dst.nexts    = nexts.clone();
					dst.keys     = keys.clone();
				}
				return dst;
			} catch( CloneNotSupportedException e ) {
				// This should not happen as R implements Cloneable
				throw new InternalError( e ); // Re-throw as InternalError
			}
		}
		
		/**
		 * Returns a JSON string representation of this set.
		 *
		 * @return a JSON string representation of this set
		 */
		@Override
		public String toString() { return toJSON(); }
		
		/**
		 * Writes the set's content to a {@link JsonWriter} as a JSON array.
		 * Null key is represented as JSON null. Char keys are written as strings or numbers depending on context.
		 *
		 * @param json the JsonWriter to write to
		 */
		@Override
		public void toJSON( JsonWriter json ) {
			int size = size();
			json.enterArray(); // Start JSON array
			if( hasNullKey ) json.value(); // Write null value if null key is present
			
			if( size > 0 ) {
				json.preallocate( size * 5 ); // Pre-allocate buffer (estimate size)
				// Use unsafe iteration
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
					json.value( String.valueOf(
							(short)( isFlatStrategy() ?
									token :
									keys[ token ] ) ) );
				}
			}
			json.exitArray(); // End JSON array
		}
		
		
		/**
		 * Calculates the bucket index for a given hash value in hash mode.
		 * Ensures non-negative index within the bounds of the buckets array.
		 *
		 * @param hash the hash value of the key
		 * @return the bucket index
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token from an index and the current version.
		 * The index can be a regular array index or a special value like NULL_KEY_INDEX.
		 *
		 * @param index the index of the element or special marker
		 * @return the generated token
		 */
		protected long token( int index ) { return ( ( long ) _version << VERSION_SHIFT ) | ( index ); }
		
		/**
		 * Extracts the index from a token.
		 *
		 * @param token the token
		 * @return the index extracted from the token
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts the version from a token.
		 *
		 * @param token the token
		 * @return the version extracted from the token
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
		/**
		 * Checks if a key is present in flat mode using the bitset. Assumes nulls is not null.
		 * Safe check: returns false if key is out of bounds for char (shouldn't happen with char).
		 */
		protected final boolean exists( char key ) { return ( nulls[ key >>> 6 ] & 1L << key ) != 0; }
		
		
		/**
		 * Finds the index of the next set bit (1) in the bitset, starting from or after 'bit'.
		 */
		public int next1( int bit ) { return next1( bit, nulls ); }
		
		public static int next1( int bit, long[] nulls ) {
			
			if( 0xFFFF < ++bit ) return -1;
			int  index = bit >>> 6;
			long value = nulls[ index ] & -1L << ( bit & 63 );
			
			while( value == 0 )
				if( ++index == NULLS_SIZE ) return -1;
				else value = nulls[ index ];
			
			return ( index << 6 ) + Long.numberOfTrailingZeros( value );
		}
	}
	
	/**
	 * {@code RW} is a read-write implementation of {@code CharSet}, extending the read-only base class {@link R}.
	 * It provides methods for modifying the set, such as adding and removing elements, as well as clearing and managing the set's capacity (including switching between hash and flat modes).
	 * This class is suitable for scenarios where both read and write operations are frequently performed on the set.
	 */
	class RW extends R {
		// The threshold capacity determining the switch to flat strategy.
		// Set to the max capacity of the hash phase (due to short[] nexts).
		protected static int flatStrategyThreshold = 0x7FFF; // 32767
		
		/**
		 * Constructs an empty {@code RW} set with a default initial capacity for hash mode.
		 */
		public RW() { this( 0 ); } // Default initial capacity
		
		/**
		 * Constructs an empty {@code RW} set with the specified initial capacity hint.
		 * If capacity exceeds the {@link #flatStrategyThreshold}, starts in flat mode.
		 *
		 * @param capacity the initial capacity hint
		 */
		public RW( int capacity ) { if( capacity > 0 ) initialize( Array.prime( capacity ) ); }
		
		
		/**
		 * Initializes the internal data structures of the set with the specified capacity.
		 * Selects between hash mode and flat mode based on the capacity.
		 *
		 * @param capacity the initial capacity hint for the set
		 * @return the actual capacity used (prime for hash mode, fixed for flat mode)
		 */
		private int initialize( int capacity ) {
			_version++;
			_count = 0; // Flat mode _count tracks actual entries
			if( flatStrategyThreshold < capacity ) {
				
				nulls    = new long[ NULLS_SIZE ]; // 1024 longs
				_buckets = null;
				nexts    = null;
				keys     = null;
				return FLAT_ARRAY_SIZE;
			}
			nulls      = null;
			_buckets   = new char[ capacity ];
			nexts      = new short[ capacity ];
			keys       = new short[ capacity ];
			_freeList  = -1;
			_freeCount = 0;
			return length();
		}
		
		
		/**
		 * Adds the specified key to this set if it is not already present. Handles null keys.
		 *
		 * @param key the key (boxed Character) to add to this set
		 * @return {@code true} if this set did not already contain the specified key
		 */
		public boolean add(  Short     key ) {
			return key == null ?
					addNullKey() :
					add( ( short ) ( key + 0 ) ); // Add primitive char key
		}
		
		
		/**
		 * Adds the specified primitive char key to this set if it is not already present.
		 *
		 * @param key the primitive char key to add to this set
		 * @return {@code true} if this set did not already contain the specified char key
		 */
		public boolean add( short key ) {
			if( isFlatStrategy() ) {
				if( exists( ( char ) key ) ) return false; // Already exists
				exists1( ( char ) key ); // Set the bit
				_count++; // Increment count of set bits
				_version++;
				return true;
			}
			
			// --- Hash Mode Logic ---
			if( _buckets == null ) initialize( 7 ); // Initialize if first add
			
			short[] _nexts      = nexts;
			int     hash        = Array.hash( key );
			int     bucketIndex = bucketIndex( hash );
			int     bucket      = _buckets[ bucketIndex ] - 1; // Get 0-based index
			
			// Check for key existence in the collision chain
			for( int next = bucket, collisionCount = 0; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) return false; // Key already exists
				next = _nexts[ next ];
				if( _nexts.length <= collisionCount++ )
					throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			
			// Key not found, add it
			int index;
			if( 0 < _freeCount ) {
				// Reuse a free slot
				index     = _freeList;
				_freeList = StartOfFreeList - _nexts[ _freeList ]; // Update free list pointer
				_freeCount--;
			}
			else {
				// Allocate a new slot
				if( _count == _nexts.length ) {
					// Resize needed
					int i = Array.prime( _count * 2 );
					if( flatStrategyThreshold < i && _count < flatStrategyThreshold ) i = flatStrategyThreshold;
					
					resize( i ); // Resize might switch to flat mode
					
					if( isFlatStrategy() ) return add( key );
					
					bucketIndex = bucketIndex( hash );
					bucket      = _buckets[ bucketIndex ] - 1;
				}
				index = _count++; // Use next available slot and increment total slot count
			}
			
			nexts[ index ]          = ( short ) bucket; // Link new entry to previous bucket head
			keys[ index ]           = ( short ) key; // Store the key
			_buckets[ bucketIndex ] = ( char ) ( index + 1 ); // Update bucket head (1-based index)
			_version++; // Increment version
			
			return true; // Key added successfully
		}
		
		
		/**
		 * Adds a null key to the set if it is not already present.
		 *
		 * @return {@code true} if the null key was added, {@code false} if already present
		 */
		public boolean addNullKey() {
			if( hasNullKey ) return false; // Null key already exists
			hasNullKey = true; // Set null key flag
			_version++; // Increment version
			return true; // Null key added
		}
		
		
		/**
		 * Removes the specified key from this set if it is present. Handles null keys.
		 *
		 * @param key the key (boxed Character) to remove from this set
		 * @return {@code true} if this set contained the key
		 */
		public boolean remove(  Short     key ) {
			return key == null ?
					removeNullKey() :
					remove( ( short ) ( key + 0 ) ); // Remove primitive char key
		}
		
		
		/**
		 * Removes the specified primitive char key from this set if it is present.
		 *
		 * @param key the primitive char key to remove from this set
		 * @return {@code true} if this set contained the char key
		 */
		public boolean remove( short key ) {
			
			if( isFlatStrategy() ) {
				if( _count == 0  || !exists( ( char ) key ) ) return false;
				exists0( ( char ) key );
				_count--;
				_version++;
				return true;
			}
			if( _count - _freeCount == 0 ) return false;
			
			int bucketIndex = bucketIndex( Array.hash( key ) );
			int i           = _buckets[ bucketIndex ] - 1;
			if( i < 0 ) return false;
			
			short next = nexts[ i ];
			if( keys[ i ] == key ) _buckets[ bucketIndex ] = ( char ) ( next + 1 );
			else
				for( int last = i, collisionCount = 0; ; ) {
					if( ( i = next ) < 0 ) return false;
					next = nexts[ i ];
					if( keys[ i ] == key ) {
						nexts[ last ] = next;
						break;
					}
					last = i;
					if( nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
				}
			
			nexts[ i ] = ( short ) ( StartOfFreeList - _freeList );
			_freeList  = i;
			_freeCount++;
			_version++;
			return true;
		}
		
		/**
		 * Removes the null key from this set if it is present.
		 *
		 * @return {@code true} if the null key was removed, {@code false} if not present
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false; // Null key not present
			hasNullKey = false; // Clear null key flag
			_version++; // Increment version
			return true; // Null key removed
		}
		
		
		/**
		 * Removes all elements from this set. The set will be empty after this call returns.
		 * Resets the mode if necessary (stays in flat mode if it was already there, hash mode resets arrays).
		 */
		public void clear() {
			_version++;
			
			hasNullKey = false;
			if( _count == 0 ) return;
			if( isFlatStrategy() )
				Array.fill( nulls, 0 );
			else {
				Arrays.fill( _buckets, ( char ) 0 );
				Arrays.fill( nexts, ( short ) 0 );
				_freeList  = -1;
				_freeCount = 0;
			}
			_count = 0;
		}
		
		
		/**
		 * Returns an array containing all of the keys in this set.
		 * The order of keys is not guaranteed.
		 *
		 * @param dst the array into which the elements of this set are to be stored, if it is big enough; otherwise, a new array of the same runtime type is allocated for this purpose.
		 * @return an array containing all the keys in this set
		 * @throws ArrayStoreException  if the runtime type of the specified array is not a supertype of the runtime type of every element in this set (should not happen with primitive char).
		 * @throws NullPointerException if the specified array is null.
		 */
		public short[] toArray( short[] dst, short null_substitute ) {
			int s = size();
			if( dst.length < s ) dst = new short[ s ];
			
			int index = 0;
			if( hasNullKey ) dst[ index++ ] = null_substitute;
			
			// Use unsafe iteration for performance
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				dst[ index++ ] = ( short ) (short)( isFlatStrategy() ?
						token :
						keys[ token ] );
			}
			
			// If dst was larger than needed, set the element after the last one to 0 (optional, standard for Collection.toArray(T[]))
			// if( dst.length > s ) dst[ s ] = 0; // Not strictly necessary for primitive arrays
			
			return dst;
		}
		
		
		/**
		 * Ensures the map’s capacity is at least the specified value.
		 * May trigger resize or switch to flat mode.
		 * Does nothing if the requested capacity is already met or if in flat mode (fixed capacity).
		 *
		 * @param capacity The minimum desired capacity (number of elements).
		 * @return The actual capacity after ensuring (might be larger than requested).
		 */
		public int ensureCapacity( int capacity ) {
			if( isFlatStrategy() || capacity <= length() ) return length(); // No change needed in flat mode or if capacity sufficient
			return _buckets == null ?
					initialize( capacity ) :
					// Initialize if not already done
					resize( Array.prime( capacity ) ); // Resize hash table
		}
		
		/**
		 * Reduces the capacity of this set to be the set's current size (number of elements).
		 * This method can be used to minimize the storage of a set instance.
		 * May switch from flat mode back to hash mode if size is below threshold.
		 */
		public void trim() { trim( size() ); }
		
		
		/**
		 * Reduces the capacity of this set to be at least as large as the set's current size or the given capacity hint, whichever is larger.
		 * If the current mode is flat, this might switch back to hash mode if the target capacity is below the threshold.
		 * If the current mode is hash, the internal arrays are resized to a prime number close to the target capacity.
		 *
		 * @param capacity the desired new capacity hint (will use at least `size()`)
		 * @throws IllegalArgumentException if the capacity hint is negative.
		 */
		public void trim( int capacity ) {
			capacity = Array.prime( capacity );
			if( length() <= capacity ) return;
			if( isFlatStrategy() ) {
				if( capacity <= flatStrategyThreshold ) resize( capacity );
				return;
			}
			
			short[]        old_next  = nexts;
			short[] old_keys  = keys;
			int            old_count = _count;
			_version++;
			initialize( capacity );
			copy( old_next, old_keys, old_count );
		}
		
		/**
		 * Resizes the internal arrays (in hash mode) or switches between modes.
		 *
		 * @param newSize the desired new capacity (for hash mode) or a trigger capacity for mode switching.
		 * @return the new actual capacity.
		 */
		private int resize( int newSize ) {
			newSize = Math.min( newSize, FLAT_ARRAY_SIZE ); ; ;
			
			if( isFlatStrategy() ) {
				if( flatStrategyThreshold < newSize ) return length();
				
				_version++;
				long[] _nulls = nulls;
				
				initialize( newSize );
				for( int token = -1; ( token = next1( token, _nulls ) ) != -1; )
				     add( ( short ) token );
				
				return length();
			}
			
			_version++;
			if( flatStrategyThreshold < newSize ) {
				
				long[] nulls = new long[ NULLS_SIZE ];
				
				for( int i = -1; ( i = unsafe_token( i ) ) != -1; )
				     exists1( ( char ) keys[ i ], nulls );
				
				this.nulls = nulls;
				
				_buckets = null;
				nexts    = null;
				keys     = null;
				
				_count -= _freeCount;
				
				_freeList  = -1;
				_freeCount = 0;
				return FLAT_ARRAY_SIZE;
			}
			
			
			short[]        new_next = Arrays.copyOf( nexts, newSize );
			short[] new_keys = Arrays.copyOf( keys, newSize );
			final int      count    = _count;
			
			_buckets = new char[ newSize ];
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = ( short ) ( _buckets[ bucketIndex ] - 1 ); //relink chain
					_buckets[ bucketIndex ] = ( char ) ( i + 1 );
				}
			
			nexts = new_next;
			keys  = new_keys;
			return length();
		}
		
		
		/**
		 * Copies elements from old arrays to the newly initialized arrays during hash mode trimming.
		 * Re-hashes and re-buckets the elements into the new hash table.
		 * Assumes the target arrays (`_buckets`, `nexts`, `keys`) are already initialized with the new capacity.
		 *
		 * @param old_nexts the old 'next' pointers array
		 * @param old_keys  the old keys array
		 * @param old_count the number of slots used in the old arrays (_count value)
		 */
		private void copy( short[] old_nexts, short[] old_keys, int old_count ) {
			int new_count = 0;
			for( int i = 0; i < old_count; i++ ) {
				if( old_nexts[ i ] < -1 ) continue;
				
				keys[ new_count ] = old_keys[ i ];
				
				int bucketIndex = bucketIndex( Array.hash( old_keys[ i ] ) );
				nexts[ new_count ]      = ( short ) ( _buckets[ bucketIndex ] - 1 );
				_buckets[ bucketIndex ] = ( char ) ( new_count + 1 );
				new_count++;
			}
			_count     = new_count;
			_freeCount = 0;
			_freeList  = -1; // Reset free list
		}
		
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		
		/**
		 * Sets a key as present in flat mode using the bitset.
		 */
		protected final void exists1( char key ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		protected static void exists1( char key, long[] nulls ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		/**
		 * Clears a key's presence in flat mode using the bitset.
		 */
		protected final void exists0( char key ) { nulls[ key >>> 6 ] &= ~( 1L << key ); }
	}
}