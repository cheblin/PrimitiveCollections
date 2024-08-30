//  MIT License
//
//  Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//  For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//  GitHub Repository: https://github.com/AdHoc-Protocol
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to use,
//  copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
//  the Software, and to permit others to do so, under the following conditions:
//
//  1. The above copyright notice and this permission notice must be included in all
//     copies or substantial portions of the Software.
//
//  2. Users of the Software must provide a clear acknowledgment in their user
//     documentation or other materials that their solution includes or is based on
//     this Software. This acknowledgment should be prominent and easily visible,
//     and can be formatted as follows:
//     "This product includes software developed by Chikirev Sirguy and the Unirail Group
//     (https://github.com/AdHoc-Protocol)."
//
//  3. If you modify the Software and distribute it, you must include a prominent notice
//     stating that you have changed the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
//  OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.

package org.unirail.collections;


import org.unirail.JsonWriter;

/**
 Interface for an ObjectSet that can hold objects of type K.
 This interface provides the structure for implementing sets with custom behavior.
 */
public interface ObjectSet {
	
	/**
	 Nested interface for iterating over non-null keys in the set.
	 This interface provides methods to traverse the set efficiently.
	 */
	interface NonNullKeysIterator {
		
		/** Constant representing the initial state of the iterator. */
		int INIT = -1;
		
		/**
		 Finds the next non-null key in the set.
		 
		 @param <K>   The type of keys in the set.
		 @param src   The source set to iterate over.
		 @param token The current position in the iteration.
		 @return The index of the next non-null key, or INIT if the end is reached.
		 */
		static <K> int token( R<K> src, int token ) {
			for( ; ; )
				if( ++token == src.keys.length ) return INIT;
				else if( src.keys[token] != null ) return token;
		}
		
		/**
		 Retrieves a key from the set given a token.
		 
		 @param <K>   The type of keys in the set.
		 @param src   The source set to retrieve from.
		 @param token The index of the key to retrieve.
		 @return The key at the given index.
		 */
		static <K> K key( R<K> src, int token ) { return src.keys[token]; }
	}
	
	/**
	 Abstract base class for implementing an ObjectSet.
	 This class provides common functionality and fields for ObjectSet implementations.
	 
	 @param <K> The type of keys in the set.
	 */
	abstract class R<K> implements Cloneable, JsonWriter.Source {
		
		/** The number of non-null elements in the set. */
		protected int assigned;
		
		/** The bitmask used for indexing into the internal array. */
		protected int mask;
		
		/** The threshold at which the set should be resized. */
		protected int resizeAt;
		
		/** Flag indicating whether the set contains a null key. */
		protected boolean hasNullKey;
		
		/** The load factor used to determine when to resize the set. */
		protected double loadFactor;
		
		/** The object used for equality checks and hash code generation for keys. */
		protected final Array.EqualHashOf<K> equal_hash_K;
		
		/** Flag indicating whether the key type is String. */
		private final boolean K_is_string;
		/**
		 Constructor for the R class with a Class object representing the type of K.
		 
		 @param clazzK The Class object for type K.
		 */
		protected R( Class<K> clazzK ) {
			equal_hash_K = Array.get( clazzK );
			K_is_string  = clazzK == String.class;
		}
		
		/**
		 Constructor for the R class with an EqualHashOf object for K.
		 
		 @param equal_hash_K The EqualHashOf object for type K.
		 */
		public R( Array.EqualHashOf<K> equal_hash_K ) {
			this.equal_hash_K = equal_hash_K;
			K_is_string       = false;
		}
		
		/** Array to store the keys of the set. */
		public K[] keys;
		
		/**
		 Checks if the set contains a specific key.
		 
		 @param key The key to check for.
		 @return true if the set contains the key, false otherwise.
		 */
		public boolean contains( K key ) {
			if( key == null ) return hasNullKey;
			int slot = equal_hash_K.hashCode( key ) & mask;
			
			for( K k; (k = keys[slot]) != null; slot = slot + 1 & mask )
				if( equal_hash_K.equals( k, key ) ) return true;
			
			return false;
		}
		
		/**
		 Checks if the set is empty.
		 
		 @return true if the set is empty, false otherwise.
		 */
		public boolean isEmpty() { return size() == 0; }
		
		/**
		 Returns the number of elements in the set.
		 
		 @return The number of elements in the set.
		 */
		public int size() { return assigned + (hasNullKey ? 1 : 0); }
		
		/**
		 Computes a symmetric hash where the order of elements does not affect the result.
		 This method is useful for hashing sets, as it ensures that sets with the same elements
		 in different orders will have the same hash code.
		 
		 @return The computed hash code for the set.
		 */
		public int hashCode() {
			int a = 0;
			int b = 0;
			int c = 1;
			
			for( int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
			{
				final int h = Array.hash( NonNullKeysIterator.key( this, token ) );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			if( hasNullKey )
			{
				final int h = Array.hash( seed );
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		/** Seed value for hash computation, based on the hash code of the R class. */
		private static final int seed = R.class.hashCode();
		/**
		 Checks if this set is equal to another object.
		 
		 @param obj The object to compare with this set.
		 @return true if the objects are equal, false otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) ); }
		
		/**
		 Checks if this set is equal to another set of the same type.
		 
		 @param other The other set to compare with this set.
		 @return true if the sets are equal, false otherwise.
		 */
		public boolean equals( R<K> other ) {
			if( other == null || other.assigned != assigned || other.hasNullKey != hasNullKey ) return false;
			
			for( K k : keys ) if( k != null && !other.contains( k ) ) return false;
			
			return true;
		}
		
		/**
		 Creates a clone of this set.
		 
		 @return A clone of this set, or null if cloning fails.
		 */
		@SuppressWarnings( "unchecked" )
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				dst.keys = keys.clone();
				return dst;  // Return the cloned object
			} catch( CloneNotSupportedException e )
			{
				e.printStackTrace();
			}
			
			return null;
		}
		
		/** Lazy-initialized getter for keys. */
		public Array.ISort.Objects<K> getK = null;
		
		/**
		 Builds a sort index for the keys in the set.
		 
		 @param dst The destination index to build.
		 */
		public void build( Array.ISort.Anything.Index dst ) {
			// Ensure the destination array is large enough
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[assigned];
			dst.size = assigned;
			// Initialize the source for sorting if not already done
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get( int index ) { return keys[index]; }
			} : getK;
			// Populate the destination array with indices of non-null keys
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != null ) dst.dst[k++] = (char) i;
			// Sort the indices based on the keys they point to
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		// Method to build a sort index for the keys in the set using Integers instead of chars
		public void build( Array.ISort.Anything.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get( int index ) { return keys[index]; }
			} : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != null ) dst.dst[k++] = (char) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		/**
		 Converts the set to a string representation.
		 
		 @return A JSON string representation of the set.
		 */
		public String toString() { return toJSON(); }
		
		/**
		 Converts the set to a JSON representation.
		 
		 @param json The JsonWriter to write the JSON representation to.
		 */
		@Override public void toJSON( JsonWriter json ) {
			int i = keys.length;
			if( K_is_string )
			{
				json.enterObject();  // If the keys are strings, represent the set as a JSON object
				
				if( hasNullKey ) json.name().value(); // Handle null key if present
				
				if( 0 < assigned )
				{
					json.preallocate( assigned * 10 );// Preallocate space for efficiency
					String[] strs = (String[]) keys;
					String   str;
					while( -1 < --i ) if( (str = strs[i]) != null ) json.name( str ).value();
				}
				
				json.exitObject();
				return;
			}
			
			json.enterArray(); // If the keys are not strings, represent the set as a JSON array
			
			if( hasNullKey ) json.value(); // Handle null key if present
			
			if( 0 < assigned )
			{
				json.preallocate( assigned * 10 );// Preallocate space for efficiency
				Object obj;
				while( -1 < --i ) if( (obj = keys[i]) != null ) json.value( obj );
			}
			
			json.exitArray();
		}
	}
	
	/**
	 Interface for an ObjectSet that supports adding and removing elements.
	 This interface defines the core operations for a mutable set of objects.
	 
	 @param <K> The type of elements in the set.
	 */
	interface Interface<K> {
		
		/**
		 Returns the number of elements in the set.
		 
		 @return The number of elements in the set.
		 */
		int size();
		
		/**
		 Checks whether the set contains the specified element.
		 
		 @param key The element to check for.
		 @return true if the set contains the specified element, false otherwise.
		 */
		boolean contains( K key );
		
		/**
		 Adds the specified element to the set if it's not already present.
		 
		 @param key The element to add to the set.
		 @return true if the set did not already contain the specified element,
		 false otherwise.
		 */
		boolean add( K key );
	}
	
	/**
	 Concrete implementation of the ObjectSet interface that allows adding and removing elements.
	 
	 @param <K> The type of elements in the set.
	 */
	class RW<K> extends R<K> implements Interface<K> {
		
		/**
		 Constructs a new RW with the specified class and expected number of items.
		 
		 @param clazz         The class of the elements.
		 @param expectedItems The expected number of items in the set.
		 */
		
		public RW( Class<K> clazz, int expectedItems ) { this( clazz, expectedItems, 0.75f ); }
		
		/**
		 Constructs a new RW with the specified equality/hash function and expected number of items.
		 
		 @param equal_hash_K  The equality/hash function for the elements.
		 @param expectedItems The expected number of items in the set.
		 */
		public RW( Array.EqualHashOf<K> equal_hash_K, int expectedItems ) { this( equal_hash_K, expectedItems, 0.75f ); }
		/**
		 Constructs a new RW with the specified class, expected number of items, and load factor.
		 
		 @param clazz         The class of the elements.
		 @param expectedItems The expected number of items in the set.
		 @param loadFactor    The load factor for the set.
		 */
		public RW( Class<K> clazz, int expectedItems, double loadFactor ) { this( Array.get( clazz ), expectedItems, loadFactor ); }
		
		
		/**
		 Main constructor for RW.
		 
		 @param equal_hash_K  The equality/hash function for the elements.
		 @param expectedItems The expected number of items in the set.
		 @param loadFactor    The load factor for the set.
		 */
		public RW( Array.EqualHashOf<K> equal_hash_K, int expectedItems, double loadFactor ) {
			super( equal_hash_K );
			
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			long length = (long) Math.ceil( expectedItems / loadFactor );
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys = equal_hash_K.copyOf( null, size );
		}
		
		/**
		 Constructs a new RW with the specified class and initial items.
		 
		 @param clazz The class of the elements.
		 @param items The initial items to add to the set.
		 */
		@SafeVarargs public RW( Class<K> clazz, K... items ) {
			this( clazz, items.length );
			for( K key : items ) add( key );
		}
		
		/**
		 Adds an element to the set.
		 
		 @param key The element to add.
		 @return true if the element was added, false if it was already present.
		 */
		public boolean add( K key ) {
			if( key == null ) return !hasNullKey && (hasNullKey = true);
			
			int slot = equal_hash_K.hashCode( key ) & mask;
			for( K k; (k = keys[slot]) != null; slot = slot + 1 & mask )
				if( equal_hash_K.equals( k, key ) ) return false;
			
			keys[slot] = key;
			if( assigned == resizeAt ) this.allocate( mask + 1 << 1 );
			
			assigned++;
			return true;
		}
		
		/**
		 Clears all elements from the set.
		 
		 @return This RW instance.
		 */
		public RW clear() {
			assigned   = 0;
			hasNullKey = false;
			for( int i = keys.length - 1; i >= 0; i-- ) keys[i] = null;
			return this;
		}
		
		/**
		 Allocates memory for the set.
		 
		 @param size The new size of the set.
		 */
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if( assigned < 1 )
			{
				if( keys.length < size ) keys = equal_hash_K.copyOf( null, size );
				return;
			}
			
			final K[] k = keys;
			keys = equal_hash_K.copyOf( null, size );
			
			K key;
			for( int i = k.length; -1 < --i; )
				if( (key = k[i]) != null )
				{
					int slot = equal_hash_K.hashCode( key ) & mask;
					
					while( keys[slot] != null ) slot = slot + 1 & mask;
					
					keys[slot] = key;
				}
			
		}
		
		/**
		 Removes an element from the set.
		 
		 @param key The element to remove.
		 @return true if the element was removed, false if it was not present.
		 */
		public boolean remove( K key ) {
			if( key == null ) return hasNullKey && !(hasNullKey = false);
			
			int slot = equal_hash_K.hashCode( key ) & mask;
			for( K k; (k = keys[slot]) != null; slot = slot + 1 & mask )
				if( equal_hash_K.equals( k, key ) )
				{
					int gapSlot = slot;
					
					K kk;
					for( int distance = 0, slot1; (kk = keys[slot1 = gapSlot + ++distance & mask]) != null; )
						if( (slot1 - equal_hash_K.hashCode( kk ) & mask) >= distance )
						{
							keys[gapSlot] = kk;
							                gapSlot = slot1;
							                distance = 0;
						}
					
					keys[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		/**
		 Creates a clone of this RW instance.
		 
		 @return A clone of this RW instance.
		 */
		public RW<K> clone() { return (RW<K>) super.clone(); }
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	/**
	 Creates singleton of an EqualHashOf object for RW<K>.
	 
	 @param <K> The type of elements in the RW.
	 @return An EqualHashOf object for RW<K>.
	 */
	@SuppressWarnings( "unchecked" )
	static <K> Array.EqualHashOf<RW<K>> equal_hash() { return (Array.EqualHashOf<RW<K>>) RW.OBJECT; }
}
	