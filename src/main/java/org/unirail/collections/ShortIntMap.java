package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;
import java.util.ConcurrentModificationException;


/**
 * A specialized map for mapping 2 bytes primitive keys (65,536 distinct values) to a primitive values.
 * Implements a HYBRID strategy:
 * 1. Starts as a hash map with separate chaining, optimized for sparse data.
 * 2. Automatically transitions to a direct-mapped flat array when the hash map is full and reaches its capacity 0x7FFF entries (exclude nullKey entity) and needs to grow further.
 * This approach balances memory efficiency for sparse maps with guaranteed O(1) performance and full key range support for dense maps.
 */
public interface ShortIntMap {
	
	/**
	 * Abstract base class providing read-only operations for the map.
	 * Handles the underlying structure which can be either a hash map or a flat array.
	 */
	abstract class R implements Cloneable, JsonWriter.Source {
		
		protected boolean           hasNullKey;          // Indicates if the map contains a null key
		protected int       nullKeyValue;        // Value for the null key, stored separately.
		protected char[]            _buckets;            // Hash table buckets array (1-based indices to chain heads).
		protected short[]           nexts;               // Links within collision chains (-1 termination, -2 unused, <-2 free list link).
		protected short[]    keys; // Keys array.
		protected int[]     values;              // Values array.
		protected int               _count;              // Hash mode: Total slots used (entries + free slots). Flat mode: Number of set bits (actual entries).
		protected int               _freeList;           // Index of the first entry in the free list (-1 if empty).
		protected int               _freeCount;          // Number of free entries in the free list.
		protected int               _version;            // Version counter for concurrent modification detection.
		
		protected static final int StartOfFreeList = -3; // Marks the start of the free list in 'nexts' field.
		protected static final int VERSION_SHIFT   = 32; // Bits to shift version in token.
		// Special index used in tokens to represent the null key. Outside valid array index ranges.
		protected static final int NULL_KEY_INDEX  = 0x1_FFFF; // 65537
		
		protected static final long INVALID_TOKEN = -1L; // Invalid token constant.
		
		
		/**
		 * Flag indicating if the map is operating in flat array mode.
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
		 * Checks if the map is empty.
		 *
		 * @return True if the map contains no key-value mappings.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 * Returns the number of key-value mappings in the map.
		 * Calculation depends on the current mode (hash map or flat array).
		 *
		 * @return The total number of mappings, including the conceptual null key if present.
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
		
		
		public int count() { return size(); }
		
		/**
		 * Returns the allocated capacity of the internal structure.
		 * In hash map mode, it's the length of the internal arrays.
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
		 * Checks if the map contains a mapping for the specified key (boxed Character).
		 *
		 * @param key The key to check.
		 * @return True if the key exists in the map.
		 */
		public boolean contains(  Short     key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains a mapping for the specified primitive key.
		 *
		 * @param key The primitive char key (0 to 65535).
		 * @return True if the key exists in the map.
		 */
		public boolean contains( short key ) { return tokenOf( key ) != INVALID_TOKEN; }
		
		/**
		 * Checks if the map contains the specified value.
		 * This operation can be slow (O(N)).
		 *
		 * @param value The value to search for.
		 * @return True if the value exists in the map.
		 */
		public boolean containsValue( int value ) {
			if( hasNullKey && nullKeyValue == value ) return true;
			if( isFlatStrategy() )
				for( int i = -1; ( i = next1( i ) ) != -1; ) {
					if( values[ i ] == value ) return true;
				}
			else if( nexts != null )
				for( int i = 0; i < nexts.length; i++ )
					if( -2 < nexts[ i ] && values[ i ] == value ) return true;
			
			return false;
		}
		
		/**
		 * Returns a token for the specified key (boxed Character).
		 *
		 * @param key The key.
		 * @return A token representing the key's location if found, or INVALID_TOKEN if not found.
		 */
		public long tokenOf(  Short     key ) {
			return key == null ?
					( hasNullKey ?
							token( NULL_KEY_INDEX ) :
							INVALID_TOKEN ) :
					tokenOf( ( short ) ( key + 0 ) );
		}
		
		/**
		 * Returns a token for the specified primitive key.
		 *
		 * @param key The primitive char key.
		 * @return A token representing the key's location if found, or INVALID_TOKEN if not found.
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
		 * Returns the initial token for iteration. Starts with the first non-null key.
		 * If only the null key exists, returns the null key token.
		 *
		 * @return The first valid token to begin iteration, or INVALID_TOKEN if the map is empty.
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
		 * Returns the next token in iteration.
		 *
		 * @param token The current token.
		 * @return The next valid token for iteration, or -1 (INVALID_TOKEN) if there are no more entries or the token is invalid due to structural modification.
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
		 * Iteration ends when {@code -1} is returned. The null key is excluded; check {@link #hasNullKey()} and
		 * use {@link #nullKeyValue()} to handle it separately.
		 *
		 * <p><strong>WARNING: UNSAFE.</strong> This method is faster than {@link #token(long)} but risky if the
		 * map is structurally modified (e.g., via add, remove, or resize) during iteration. Such changes may
		 * cause skipped entries, exceptions, or undefined behavior. Use only when no modifications will occur.
		 *
		 * @param token The previous token (index), or {@code -1} to begin iteration.
		 * @return The next token (an index) for a non-null key, or {@code -1} if no more entries exist.
		 * @see #token(long) For safe iteration including the null key.
		 * @see #hasNullKey() To check for a null key.
		 * @see #nullKeyValue() To get the null key’s value.
		 */
		public int unsafe_token( final int token ) {
			if( isFlatStrategy() )
				return next1( index( token ) );
			else
				for( int i = token + 1; i < _count; i++ )
					if( -2 < nexts[ i ] ) return i;
			
			return -1; // No more entries
		}
		
		/**
		 * Checks if the map contains the conceptual null key.
		 */
		public boolean hasNullKey() { return hasNullKey; }
		
		/**
		 * Returns the value associated with the conceptual null key. Behavior undefined if null key doesn't exist.
		 */
		public int nullKeyValue() { return  nullKeyValue; }
		
		
		public boolean isKeyNull( long token ) { return index( token ) == NULL_KEY_INDEX; }
		
		/**
		 * Retrieves the key associated with a token. Before calling this method ensure that this token is not point to the isKeyNull
		 *
		 * @param token The token representing a non-null key-value pair.
		 * @return The char key associated with the token.
		 * @throws ArrayIndexOutOfBoundsException if the token is for the null key or invalid.
		 */
		public short key( long token ) {
			return ( short ) (short) (
					isFlatStrategy() ?
							index( token ) :
							keys[ index( token ) ] );
		}
		
		
		/**
		 * Retrieves the value associated with a token.
		 *
		 * @param token The token representing the key-value pair.
		 * @return The primitive value associated with the token, or undefined if the token is -1 (INVALID_TOKEN) or invalid due to structural modification.
		 */
		public int value( long token ) {
			return (
					isKeyNull( token ) ?
							nullKeyValue :
							values[ index( token ) ] );
		}
		
		/**
		 * Computes an order-independent hash code.
		 *
		 * @return The hash code of the map.
		 */
		@Override
		public int hashCode() {
			int a = 0, b = 0, c = 1;
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				int h = Array.mix( seed, Array.hash( isFlatStrategy() ?
						                                     token :
						                                     keys[ token ] ) );
				h = Array.mix( h, Array.hash( value( token ) ) );
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
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		@Override
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( ( R ) obj ); }
		
		/**
		 * Compares this map with another R instance for equality.
		 *
		 * @param other The other map to compare.
		 * @return True if the maps are equal.
		 */
		public boolean equals( R other ) {
			if( other == this ) return true;
			if( other == null ||
			    hasNullKey != other.hasNullKey ||
			    ( hasNullKey && nullKeyValue != other.nullKeyValue ) || size() != other.size() )
				return false;
			
			for( int token = -1; ( token = unsafe_token( token ) ) != -1; ) {
				long t = other.tokenOf( key( token ) );
				if( t == INVALID_TOKEN || value( token ) != other.value( t ) ) return false;
			}
			return true;
		}
		
		
		@Override
		public R clone() {
			try {
				R dst = ( R ) super.clone();
				if( isFlatStrategy() ) {
					dst.nulls  = nulls.clone();
					dst.values = values.clone();
				}
				else if( _buckets != null ) {
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
		
		@Override
		public String toString() { return toJSON(); }
		
		
		@Override
		public void toJSON( JsonWriter json ) {
			json.preallocate( size() * 10 );
			json.enterObject();
			
			if( hasNullKey ) json.name().value( nullKeyValue );
			
			if( isFlatStrategy() )
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( token ).value( values[ token ] );
			else
				for( int token = -1; ( token = unsafe_token( token ) ) != -1; )
				     json.name( keys[ token ] ).value( values[ token ] );
			
			
			json.exitObject();
		}
		
		// --- Helper methods ---
		
		/**
		 * Calculates bucket index in hash map mode. Ensures non-negative index.
		 */
		protected int bucketIndex( int hash ) { return ( hash & 0x7FFF_FFFF ) % _buckets.length; }
		
		/**
		 * Creates a token combining version and index.
		 */
		protected long token( int index ) { return ( long ) _version << VERSION_SHIFT | ( index ); }
		
		/**
		 * Extracts index from a token.
		 */
		protected int index( long token ) { return ( int ) ( token ); }
		
		/**
		 * Extracts version from a token.
		 */
		protected int version( long token ) { return ( int ) ( token >>> VERSION_SHIFT ); }
		
		
		/**
		 * Checks if a key is present in flat mode using the bitset. Assumes nulls is not null.
		 */
		protected final boolean exists( char key ) { return ( nulls[ key >>> 6 ] & 1L << key ) != 0; }
		
		
		public int next1( int key ) { return next1( key, nulls ); }
		
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
	 * Read-write implementation of the map, extending read-only functionality.
	 */
	class RW extends R {
		// The threshold capacity determining the switch to flat strategy.
		// Set to the max capacity of the hash phase (due to short[] nexts).
		protected static final int flatStrategyThreshold = 0x7FFF;
		
		/**
		 * Constructs an empty map with default initial capacity for hash map mode.
		 */
		public RW() { this( 0 ); } // Default initial capacity
		
		/**
		 * Constructs an empty map with specified initial capacity.
		 * If capacity exceeds the threshold, starts in flat mode.
		 *
		 * @param capacity The initial capacity hint.
		 */
		public RW( int capacity ) {
			if( capacity > 0 ) initialize( Array.prime( capacity ) );
		}
		
		
		/**
		 * Initializes the internal arrays with a specified capacity.
		 *
		 * @param capacity The desired capacity.
		 * @return The initialized capacity (prime number).
		 */
		private int initialize( int capacity ) {
			_version++;
			_count = 0; // Flat mode _count tracks actual entries
			if( flatStrategyThreshold < capacity ) {
				nulls    = new long[ NULLS_SIZE ]; // 1024 longs
				values   = new int[ FLAT_ARRAY_SIZE ];  // 65536
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
			values     = new int[ capacity ];
			return length();
		}
		
		
		/**
		 * Associates a value with a key (boxed Character).
		 *
		 * @param key   The key.
		 * @param value The value.
		 * @return True if the map was modified (key inserted or updated).
		 */
		public boolean put(  Short     key, int value ) {
			return key == null ?
					put( value ) :
					put( key, value );
		}
		
		/**
		 * Associates a value with a primitive key.
		 *
		 * @param key   The primitive char key.
		 * @param value The value.
		 * @return True if the map was modified (key inserted or updated).
		 */
		public boolean put( short key, int value ) {
			if( isFlatStrategy() ) {
				boolean b;
				if( b = !exists( ( char ) key ) ) {
					exists1( ( char ) key );
					_count++;
				}
				values[ ( char ) key ] = ( int ) value;
				_version++;
				return b;
			}
			
			
			if( _buckets == null ) initialize( 7 );
			short[] _nexts         = nexts;
			int     hash           = Array.hash( key );
			int     collisionCount = 0;
			int     bucketIndex    = bucketIndex( hash );
			int     bucket         = _buckets[ bucketIndex ] - 1;
			
			for( int next = bucket; ( next & 0x7FFF_FFFF ) < _nexts.length; ) {
				if( keys[ next ] == key ) {
					values[ next ] = ( int ) value;
					_version++;
					return false;
				}
				
				next = _nexts[ next ];
				if( _nexts.length < collisionCount++ ) throw new ConcurrentModificationException( "Concurrent operations not supported." );
			}
			
			int index;
			if( 0 < _freeCount ) {
				index     = _freeList;
				_freeList = StartOfFreeList - _nexts[ _freeList ];
				_freeCount--;
			}
			else {
				if( _count == _nexts.length ) {
					int i = Array.prime( _count * 2 );
					if( flatStrategyThreshold < i && _count < flatStrategyThreshold ) i = flatStrategyThreshold;
					
					resize( i );
					if( isFlatStrategy() ) return put( key, value );
					
					bucket = ( ( _buckets[ bucketIndex = bucketIndex( hash ) ] ) - 1 );
				}
				index = _count++;
			}
			
			nexts[ index ]          = ( short ) bucket;
			keys[ index ]           = ( short ) key;
			values[ index ]         = ( int ) value;
			_buckets[ bucketIndex ] = ( char ) ( index + 1 );
			_version++;
			
			return true;
		}
		
		/**
		 * Inserts or updates the value for the conceptual null key.
		 *
		 * @param value The value.
		 * @return True if the null key was newly inserted or updated.
		 */
		public boolean put( int value ) {
			boolean b = !hasNullKey;
			hasNullKey   = true;
			nullKeyValue = ( int ) value;
			_version++;
			return b;
		}
		
		/**
		 * Removes a key-value pair (boxed Integer key).
		 *
		 * @param key The key to remove.
		 * @return The token of the removed entry if found and removed, or -1 (INVALID_TOKEN) if not found.
		 */
		public boolean remove(  Short     key ) {
			return key == null ?
					removeNullKey() :
					remove( ( short ) ( key + 0 ) );
		}
		
		/**
		 * Removes the conceptual null key mapping.
		 *
		 * @return true if remove entry or false no mapping was found for the null  key.
		 */
		public boolean removeNullKey() {
			if( !hasNullKey ) return false;
			hasNullKey = false;
			_version++;
			// Return a token representing the null key conceptually
			return true;
		}
		
		/**
		 * Removes a key-value pair (primitive key).
		 *
		 * @param key The primitive char key to remove.
		 * @return The token of the removed entry if found and removed, or INVALID_TOKEN if not found.
		 */
		public boolean remove( short key ) {
			
			if( isFlatStrategy() ) {
				if( _count == 0  || !exists( ( char ) key ) ) return false;
				exists0( ( char ) key );
				_count--;
				_version++;
				return true;
			}
			
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
		 * Clears all mappings from the map.
		 */
		public void clear() {
			_version++;
			
			hasNullKey = false;
			if( _count == 0 ) return;
			_count = 0;
			
			
			if( isFlatStrategy() )
				Array.fill( nulls, 0 );
			else {
				if( _buckets != null ) Arrays.fill( _buckets, ( char ) 0 );
				Arrays.fill( nexts, ( short ) 0 );
				_freeList  = -1;
				_freeCount = 0;
			}
		}
		
		/**
		 * Ensures the map’s capacity is at least the specified value.
		 * May trigger resize or switch to flat mode.
		 *
		 * @param capacity The minimum desired capacity.
		 * @return The actual capacity after ensuring.
		 */
		public int ensureCapacity( int capacity ) {
			if( capacity <= length() ) return length();
			return !isFlatStrategy() && _buckets == null ?
					initialize( capacity ) :
					resize( Array.prime( capacity ) );
		}
		
		/**
		 * Trims the capacity to the current number of entries.
		 */
		public void trim() { trim( size() ); }
		
		/**
		 * Trims the capacity to the specified value (at least the current size).
		 * May switch from flat mode back to hash mode if trimmed below threshold.
		 *
		 * @param capacity The desired capacity.
		 */
		public void trim( int capacity ) {
			capacity = Array.prime( capacity );
			if( length() <= capacity ) return;
			if( isFlatStrategy() ) {
				if( capacity <= flatStrategyThreshold ) resize( capacity );
				return;
			}
			
			short[]        old_next   = nexts;
			short[] old_keys   = keys;
			int[]  old_values = values;
			int            old_count  = _count;
			initialize( capacity );
			for( int i = 0; i < old_count; i++ )
				if( -2 < old_next[ i ] ) put( (short) old_keys[ i ], old_values[ i ] );
		}
		
		/**
		 * Resizes the map to a new prime capacity.
		 *
		 * @param newSize The new size (prime number).
		 */
		private int resize( int newSize ) {
			newSize = Math.min( newSize, FLAT_ARRAY_SIZE ); ; ;
			
			if( isFlatStrategy() ) {
				if( flatStrategyThreshold < newSize ) return length();
				
				_version++;
				int[] _values = values;
				long[]        _nulls  = nulls;
				
				initialize( newSize );
				for( int token = -1; ( token = next1( token, _nulls ) ) != -1; )
				     put( ( short ) token,  _values[ token ] );
				
				return length();
			}
			
			_version++;
			if( flatStrategyThreshold < newSize ) {
				
				int[] _values = values;
				
				values = new int[ FLAT_ARRAY_SIZE ];
				long[] nulls = new long[ NULLS_SIZE ];
				
				for( int i = -1; ( i = unsafe_token( i ) ) != -1; ) {
					char key = ( char ) keys[ i ];
					exists1( key, nulls );
					values[ key ] = _values[ i ];
				}
				
				this.nulls = nulls;
				
				_buckets = null;
				nexts    = null;
				keys     = null;
				
				_count -= _freeCount;
				
				_freeList  = -1;
				_freeCount = 0;
				return FLAT_ARRAY_SIZE;
			}
			
			
			short[]        new_next   = Arrays.copyOf( nexts, newSize );
			short[] new_keys   = Arrays.copyOf( keys, newSize );
			int[]  new_values = Arrays.copyOf( values, newSize );
			final int      count      = _count;
			
			_buckets = new char[ newSize ];
			for( int i = 0; i < count; i++ )
				if( -2 < new_next[ i ] ) {
					int bucketIndex = bucketIndex( Array.hash( keys[ i ] ) );
					new_next[ i ]           = ( short ) ( _buckets[ bucketIndex ] - 1 ); //relink chain
					_buckets[ bucketIndex ] = ( char ) ( i + 1 );
				}
			
			nexts  = new_next;
			keys   = new_keys;
			values = new_values;
			return length();
		}
		
		
		@Override
		public RW clone() { return ( RW ) super.clone(); }
		
		/**
		 * Sets a key as present in flat mode using the bitset.
		 */
		protected final void exists1( char key, long[] nulls ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		protected final void exists1( char key ) { nulls[ key >>> 6 ] |= 1L << key; }
		
		/**
		 * Clears a key's presence in flat mode using the bitset.
		 */
		protected final void exists0( char key ) { nulls[ key >>> 6 ] &= ~( 1L << key ); }
	}
}