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

import java.util.Arrays;


public interface BitsList {
	
	// Calculates a mask for the given number of bits.
	static long mask( int bits ) { return (1L << bits) - 1; }
	
	// Calculates the size of the storage needed for the given number of bits.
	static int size( int src ) { return src >>> 3; }
	
	// Determines the index in the array where the given item's bits start.
	static int index( int item_X_bits ) { return item_X_bits >> LEN; }
	
	// Determines the bit position within the long value at the index for the item.
	static int bit( int item_X_bits ) { return item_X_bits & MASK; }
	
	// Retrieves the byte value from the source long starting at the specified bit.
	static byte value( long src, int bit, long mask ) { return (byte) (src >>> bit & mask); }
	
	// Retrieves a byte value that spans across two long values.
	static byte value( long prev, long next, int bit, int bits, long mask ) { return (byte) (((next & mask( bit + bits - BITS )) << BITS - bit | prev >>> bit) & mask); }
	
	// Constants for bit manipulation.
	int BITS = 64;
	int MASK = BITS - 1;
	int LEN  = 6;
	
	// Calculates the length of the array needed to store the given number of bits.
	static int len4bits( int bits ) { return (bits + BITS) >>> LEN; }
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		// Array to store the bits.
		protected long[] values = Array.EqualHashOf.longs.O;
		// Number of items stored.
		protected int    size   = 0;
		
		// Returns the number of items stored.
		public int size() { return size; }
		
		// Mask for the bits per item and the default value for new items.
		protected final long mask;
		public final    int  bits;
		public final    int  default_value;
		
		// Constructor specifying the number of bits per item.
		protected R( int bits_per_item ) {
			mask          = mask( bits = bits_per_item );
			default_value = 0;
		}
		
		// Constructor specifying the number of bits per item and the initial capacity.
		protected R( int bits_per_item, int length ) {
			mask          = mask( bits = bits_per_item );
			values        = new long[len4bits( length * bits )];
			default_value = 0;
		}
		
		// Constructor specifying the number of bits per item, default value, and size.
		protected R( int bits_per_item, int default_value, int size ) {
			mask = mask( bits = bits_per_item );
			
			values = new long[len4bits( (this.size = size < 0 ? -size : size) * bits )];
			
			if( (this.default_value = (int) (default_value & mask)) != 0 && 0 < size )
				for( int i = 0; i < size; i++ ) append( this, i, this.default_value );
		}
		
		// Returns the total length of the storage array.
		public int length() { return values.length * BITS / bits; }
		
		// Adjusts the length of the storage array based on the number of items.
		// If 0 < items , it adjusts the storage space according to the 'items' parameter.
		// If items < 0, it cleans up and allocates -items space.
		protected void length_( int items ) {
			
			if( 0 < items )// If positive, adjust the array size to fit the specified number of items.
			{
				if( items < size ) size = items;// Adjust the size if items are less than the current size.
				final int new_values_length = len4bits( items * bits );
				
				if( values.length != new_values_length ) values = Array.copyOf( values, new_values_length );
				return;
			}
			
			// If negative, clear the array and allocate space for the absolute value of items.
			final int new_values_length = len4bits( -items * bits );
			
			if( values.length != new_values_length )
			{
				// Allocate new space or set it to an empty array if new length is 0.
				values = new_values_length == 0 ?
				         Array.EqualHashOf.longs.O :
				         new long[new_values_length];
				
				size = 0;
				return;
			}
			
			
			clear();// Clear the array.
		}
		
		// Clears the array, setting all bits to zero.
		protected void clear() {
			Arrays.fill( values, 0, Math.min( index( bits * size ), values.length - 1 ), 0 );
			size = 0;
		}
		
		// Checks if the list is empty.
		public boolean isEmpty() { return size == 0; }
		
		// Generates a hash code for the list.
		public int hashCode() {
			int i = index( size );
			
			int hash = Array.hash( 149989999, (values[i] & (1L << bit( size )) - 1) );
			
			while( -1 < --i ) hash = Array.hash( hash, values[i] );
			
			return Array.finalizeHash( hash, size() );
		}
		
		// Compares this list with another object for equality.
		public boolean equals( Object other ) {
			return other != null &&
			       getClass() == other.getClass() &&
			       equals( getClass().cast( other ) );
		}
		
		// Compares this list with another list of the same type for equality.
		public boolean equals( R other ) {
			if( other == null || other.size != size ) return false;
			
			int i = index( size );
			
			long mask = (1L << bit( size )) - 1;
			if( (values[i] & mask) != (other.values[i] & mask) ) return false;
			
			while( -1 < --i )
				if( values[i] != other.values[i] ) return false;
			
			return true;
		}
		
		// Retrieves the last byte value added to the list.
		public byte get() { return get( size - 1 ); }
		
		// Retrieves the byte value at the specified item index.
		public byte get( int item ) {
			final int index = index( item *= bits );
			final int bit   = bit( item );
			
			return BITS < bit + bits ? value( values[index], values[index + 1], bit, bits, mask ) : value( values[index], bit, mask );
		}
		
		// Adds a new value to the list.
		protected static void add( R dst, long src ) { set1( dst, dst.size, src ); }
		// Adds a new value at the specified item index.
		protected static void add( R dst, int item, long value ) {
			// If adding at the end of the list, simply append the value.
			if( dst.size == item )
			{
				append( dst, item, value );
				return;
			}
			// If adding beyond the current size, set the value at the specified index.
			if( dst.size < item )
			{
				set1( dst, item, value );
				return;
			}
			
			// Insert the value into the list at the specified index.
			int p = item * dst.bits;
			item = index( p );
			
			final long[] src  = dst.values;
			long[]       dst_ = dst.values;
			if( dst.length() * BITS < p ) dst.length_( Math.max( dst.length() + dst.length() / 2, len4bits( p ) ) );
			long      v   = value & dst.mask;
			final int bit = bit( p );
			if( 0 < bit )
			{
				final long i = src[item];
				final int  k = BITS - bit;
				if( k < dst.bits )
				{
					dst_[item] = i << k >>> k | v << bit;
					v          = v >> k | i >> bit << dst.bits - k;
				}
				else
				{
					dst_[item] = i << k >>> k | v << bit | i >>> bit << bit + dst.bits;
					v          = i >>> bit + dst.bits | src[item + 1] << k - dst.bits & dst.mask;
				}
				item++;
			}
			
			dst.size++;
			
			for( final int max = len4bits( dst.size * dst.bits ); ; )
			{
				final long i = src[item];
				dst_[item] = i << dst.bits | v;
				if( max < ++item ) break;
				v = i >>> BITS - dst.bits;
			}
		}
		
		// Sets multiple byte values starting from the specified index.
		protected static void set( R dst, int from, byte... src ) { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		// Sets multiple char values starting from the specified index.
		protected static void set( R dst, int from, char... src ) { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		// Sets multiple short values starting from the specified index.
		protected static void set( R dst, int from, short... src ) { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		// Sets multiple int values starting from the specified index.
		protected static void set( R dst, int from, int... src ) { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		// Sets multiple long values starting from the specified index.
		protected static void set( R dst, int from, long... src ) { for( int i = src.length; -1 < --i; ) set1( dst, from + i, src[i] ); }
		
		protected static void set1( R dst, int item, long src ) {
			// Calculate the total number of bits for the item.
			final int total_bits = item * dst.bits;
			
			// If the item is within the current size, update its value.
			if( item < dst.size )
			{
				// Calculate the index and bit position for the item.
				final int
						index = index( total_bits ),
						bit = bit( total_bits ),
						k = BITS - bit;
				
				// Retrieve the current value and apply the mask.
				final long i = dst.values[index], v = src & dst.mask;
				// If the value spans across two indices, split it accordingly.
				if( k < dst.bits )
				{
					dst.values[index]     = i << k >>> k | v << bit;
					dst.values[index + 1] = dst.values[index + 1] >>> dst.bits - k << dst.bits - k | v >> k;
				}
				else dst.values[index] = ~(~0L >>> BITS - dst.bits << bit) & i | v << bit;
				return;
			}
			
			// If the item is beyond the current length, expand the list.
			
			if( dst.length() <= item ) dst.length_( Math.max( dst.length() + dst.length() / 2, len4bits( total_bits + dst.bits ) ) );
			
			// If there's a default value, fill up to the new item with it.
			
			if( dst.default_value != 0 )
				for( int i = dst.size; i < item; i++ ) append( dst, i, dst.default_value );
			
			// Append the new value to the list.
			append( dst, item, src );
			
			// Update the size to reflect the new item.
			dst.size = item + 1;
		}
		
		// Appends a value to the list at a specific item index.
		private static void append( R dst, int item, long src ) {
			// Apply the mask to the source value.
			final long v = src & dst.mask;
			// Calculate the position and index for the item.
			final int
					p = item * dst.bits,
					index = index( p ),
					bit = bit( p );
			
			// Calculate the shift needed based on the bit position.
			final int k = BITS - bit;
			// Retrieve the current value at the index.
			final long i = dst.values[index];
			
			// If the value spans across two indices, split it accordingly.
			if( k < dst.bits )
			{
				dst.values[index]     = i << k >>> k | v << bit;
				dst.values[index + 1] = v >> k;
			}
			else
				dst.values[index] = ~(~0L << bit) & i | v << bit;
		}
		
		// Removes an item at a specific index from the list.
		protected static void removeAt( R dst, int item ) {
			// If removing the last item, just decrement the size.
			
			if( item + 1 == dst.size )
			{
				if( dst.default_value == 0 ) append( dst, item, 0 );// Zero out the last place.
				dst.size--;
				return;
			}
			
			// Calculate the index and bit position for the item.
			int       index = index( item *= dst.bits );
			final int bit   = bit( item );
			
			// Calculate the shift needed based on the bit position.
			final int k = BITS - bit;
			// Retrieve the current value at the index.
			long i = dst.values[index];
			
			// If the item is at the end of the list, handle it accordingly.
			
			if( index + 1 == dst.length() )
			{
				if( bit == 0 ) dst.values[index] = i >>> dst.bits;
				else if( k < dst.bits ) dst.values[index] = i << k >>> k;
				else if( dst.bits < k ) dst.values[index] = i << k >>> k | i >>> bit + dst.bits << bit;
				
				dst.size--;
				return;
			}
			
			// Shift the values to remove the item.
			
			if( bit == 0 ) dst.values[index] = i >>>= dst.bits;
			else if( k < dst.bits )
			{
				long ii = dst.values[index + 1];
				
				dst.values[index]   = i << k >>> k | ii >>> bit + dst.bits - BITS << bit;
				dst.values[++index] = i = ii >>> dst.bits;
			}
			else if( dst.bits < k )
				if( index + 1 == dst.values.length )
				{
					dst.values[index] = i << k >>> k | i >>> bit + dst.bits << bit;
					dst.size--;
					return;
				}
				else
				{
					long ii = dst.values[index + 1];
					
					dst.values[index]   = i << k >>> k | i >>> bit + dst.bits << bit | ii << BITS - dst.bits;
					dst.values[++index] = i = ii >>> dst.bits;
				}
			
			// Continue shifting values until the item is fully removed.
			for( final int max = dst.size * dst.bits >>> LEN; index < max; )
			{
				long ii = dst.values[index + 1];
				dst.values[index]   = i << dst.bits >>> dst.bits | ii << BITS - dst.bits;
				dst.values[++index] = i = ii >>> dst.bits;
			}
			
			dst.size--;
		}
		
		
		// Overrides the clone method to create a copy of the R object.
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				if( 0 < dst.length() ) dst.values = values.clone(); // Clone the values array if it's not empty.
				return dst;
				
			} catch( CloneNotSupportedException e ) { e.printStackTrace(); }// Print the stack trace if cloning is not supported.
			return null; // Return null if cloning fails.
		}
		
		// Converts the bits list to a JSON array representation.
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterArray(); // Start the JSON array.
			
			if( 0 < size )
			{
				json.preallocate( size * 4 ); // Preallocate space for efficiency.
				long src = values[0]; // Start with the first value.
				
				for( int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++ )
				{
					final int bit   = bit( bp );// Calculate the bit position.
					long      value = (BITS < bit + bits ? value( src, src = values[index( bp ) + 1], bit, bits, mask ) : value( src, bit, mask ));// Get the value, spanning across indices if necessary.
					json.value( value );// Add the value to the JSON array.
				}
			}
			json.exitArray();// End the JSON array
		}
		
		// Finds the index of the first occurrence of the specified value.
		public int indexOf( long value ) {
			for( int item = 0, max = size * bits; item < max; item += bits )
				if( value == get( item ) ) return item / bits;
			
			return -1;
		}
		
		// Finds the index of the last occurrence of the specified value.
		public int lastIndexOf( long value ) { return lastIndexOf( size, value ); }
		
		// Finds the index of the last occurrence of the specified value, starting from a specified index.
		public int lastIndexOf( int from, long value ) {
			for( int i = Math.min( from, size ); -1 < --i; )
				if( value == get( i ) ) return i; // Return the index if the value is found.
			
			return -1;// Return -1 if the value is not found.
		}
		
		// Removes all occurrences of the specified value from the list.
		protected static void remove( R dst, long value ) {
			for( int i = dst.size; -1 < (i = dst.lastIndexOf( i, value )); )
			     removeAt( dst, i );// Remove the value at the found index.
		}
		
		// Checks if the list contains the specified value.
		public boolean contains( long value ) { return -1 < indexOf( value ); }
		
		// Converts the list of bits to an array of bytes.
		public byte[] toArray( byte[] dst ) {
			if( size == 0 ) return null;// Return null if the list is empty.
			if( dst == null || dst.length < size ) dst = new byte[size];// Create a new array if necessary.
			
			for( int item = 0, max = size * bits; item < max; item += bits )
			     dst[item / bits] = (byte) get( item ); // Convert each item to a byte and add it to the array.
			
			return dst;// Return the array.
		}
	}
	
	// Interface defining basic operations for the data structure.
	interface Interface {
		int size(); // Returns the number of items in the collection.
		
		byte get( int item ); // Retrieves the value at the specified item index.
		
		RW set1( int item, int value ); // Sets the value at the specified item index.
		
		RW add1( long value ); // Adds a value to the collection.
	}
	
	// RW class extends R and implements the Interface.
	class RW extends R implements Interface {
		
		/**
		 Constructor initializing the data structure with bits per item.
		 
		 @param bits_per_item Number of bits used to represent each item.
		 */
		public RW( int bits_per_item ) { super( bits_per_item ); }
		
		/**
		 Constructor initializing the data structure with bits per item and length.
		 
		 @param bits_per_item Number of bits used to represent each item.
		 @param length        The initial length of the data structure.
		 */
		public RW( int bits_per_item, int length ) { super( bits_per_item, length ); }
		
		/**
		 Constructor initializing the data structure with bits per item, default value, and size.
		 
		 @param bits_per_item Number of bits used to represent each item.
		 @param defaultValue  The default value to initialize elements with.
		 @param size          The initial size of the data structure. If negative, the absolute value is used.
		 If size is positive, initialize with the default value.
		 */
		public RW( int bits_per_item, int defaultValue, int size ) { super( bits_per_item, defaultValue, size ); }
		
		// Method to add a value to the collection.
		public RW add1( long value ) {
			add( this, value );
			return this;
		}
		
		// Method to add a value at a specific index in the collection.
		public RW add1( int index, long src ) {
			add( this, index, src );
			return this;
		}
		
		// Method to remove a specific value from the collection.
		public RW remove( long value ) {
			remove( this, value );
			return this;
		}
		
		// Method to remove a value at a specified index in the collection.
		public RW removeAt( int item ) {
			removeAt( this, item );
			return this;
		}
		
		// Method to remove the last item from the collection.
		public RW remove() {
			removeAt( this, size - 1 );
			return this;
		}
		
		// Method to set the last item in the collection to a specific value.
		public RW set1( int value ) {
			set1( this, size - 1, value );
			return this;
		}
		
		// Method to set the last item in the collection to a specific char value.
		public RW set1( char value ) {
			set1( this, size - 1, value );
			return this;
		}
		
		// Method to set a specific item in the collection to a value.
		public RW set1( int item, int value ) {
			set1( this, item, value );
			return this;
		}
		
		// Method to set a specific item in the collection to a char value.
		public RW set1( int item, char value ) {
			set1( this, item, value );
			return this;
		}
		
		// Method to set multiple values starting from a specified index.
		public RW set( int index, byte... values ) {
			set( this, index, values );
			return this;
		}
		
		// Method to set a range of values starting from a specified index.
		public RW set( int index, byte[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, src[src_index + i] );
			return this;
		}
		
		// Method to set multiple char values starting from a specified index.
		public RW set( int index, char... values ) {
			set( this, index, values );
			return this;
		}
		
		// Method to set multiple short values starting from a specified index.
		public RW set( int index, short... values ) {
			set( this, index, values );
			return this;
		}
		
		// Method to set multiple int values starting from a specified index.
		public RW set( int index, int... values ) {
			set( this, index, values );
			return this;
		}
		
		// Method to set multiple long values starting from a specified index.
		public RW set( int index, long... values ) {
			set( this, index, values );
			return this;
		}
		
		// Method to retain only the items that are present in another collection.
		public boolean retainAll( R chk ) {
			final int fix = size;
			
			int v;
			for( int item = 0; item < size; item++ )
				if( !chk.contains( v = get( item ) ) ) remove( this, v );
			
			return fix != size;
		}
		
		// Method to adjust the internal length to fit the current size.
		public RW fit() { return length( size ); }
		
		// Method to set the length of the collection.
		public RW length( int items ) {
			if( items < 1 )
			{
				values = Array.EqualHashOf.longs.O;
				size   = 0;
			}
			else length_( items );
			return this;
		}
		
		// Method to set the size of the collection.
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( this.size < size ) set1( size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		// Override the clone method to create a deep copy of the RW object.
		@Override public RW clone() { return (RW) super.clone(); }
	}
	
}
