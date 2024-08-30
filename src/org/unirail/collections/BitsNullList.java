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

public interface BitsNullList {
	
	// Abstract class that extends BitsList.R to handle lists with a specific null value representation
	abstract class R extends BitsList.R {
		
		// The value representing null in this list
		public final int null_val;
		
		// Constructor initializing bits per item and null value
		protected R( int bits_per_item, int null_val ) {
			super( bits_per_item );
			this.null_val = null_val;
		}
		
		// Constructor initializing bits per item, null value, and size
		protected R( int bits_per_item, int null_val, int size ) {
			super( bits_per_item, null_val, size );
			this.null_val = null_val;
		}
		
		// Constructor initializing bits per item, null value, default value, and size
		protected R( int bits_per_item, int null_val, int default_value, int size ) {
			super( bits_per_item, default_value, size );
			this.null_val = null_val;
		}
		
		// Checks if the value at the given index is not the null value
		public boolean hasValue( int index ) { return get( index ) != null_val; }
		// Checks if the list contains the specified item
		public boolean contains( long item ) { return 0 < indexOf( item ); }
		// Clones the current instance
		@Override public R clone() { return (R) super.clone(); }
		
		// Converts the list to a JSON string representation
		public String toString() { return toJSON(); }
		// Writes the list to a JSON writer
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterArray();
			
			final int size = size();
			if( 0 < size )
			{
				json.preallocate( size * 4 );
				
				long src = values[0];
				for( int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++ )
				{
					final int bit   = BitsList.bit( bp );
					long      value = (BitsList.BITS < bit + bits ?
					                   BitsList.value( src, src = values[BitsList.index( bp ) + 1], bit, bits, mask ) :
					                   BitsList.value( src, bit, mask ));
					
					if( value == null_val ) json.value();
					else json.value( value );
				}
			}
			json.exitArray();
		}
	}
	
	// Interface for a list with bits and a null value
	interface Interface {
		int size();
		
		byte get( int item );
		
		boolean hasValue( int index );
		
		RW set1( int item, long value );
		
		RW set1( int item, Long value );
		
		RW add1( long value );
		
		RW add1( Long value );
	}
	
	// Class representing a read-write list with bits and a null value
	class RW extends R implements Interface {
		/**
		 Constructor initializing bits per item and null value.
		 
		 @param bits_per_item Number of bits used to represent each item.
		 @param null_val      Value used to represent null.
		 */
		public RW( int bits_per_item, int null_val ) { super( null_val, bits_per_item ); }
		/**
		 Constructor initializing the data structure with bits per item, null value, and size.
		 
		 @param bits_per_item Number of bits used to represent each item.
		 @param null_val      Value used to represent null.
		 @param size          The initial size of the data structure. If negative, the absolute value is used.
		 If size is positive, initialize with the default value.
		 */
		// If size is positive, initialize with the default value.
		public RW( int bits_per_item, int null_val, int size ) { super( bits_per_item, null_val, size ); }
		/**
		 Constructor initializing the data structure with bits per item, null value, default value, and size.
		 
		 @param bits_per_item Number of bits used to represent each item.
		 @param null_val      Value used to represent null.
		 @param default_value The default value to initialize elements with.
		 @param size          The initial size of the data structure. If negative, the absolute value is used.
		 If size is positive, initialize with the default value.
		 */
		public RW( int bits_per_item, int null_val, int default_value, int size ) { super( bits_per_item, null_val, default_value, size ); }
		/**
		 Constructor initializing the data structure with bits per item, null value, nullable default value, and size.
		 
		 @param bits_per_item Number of bits used to represent each item.
		 @param null_val      Value used to represent null.
		 @param default_value The default value to initialize elements with. If null, null_val is used.
		 @param size          The initial size of the data structure. If negative, the absolute value is used.
		 If size is positive, initialize with the default value.
		 */
		public RW( int bits_per_item, int null_val, Integer default_value, int size ) { super( bits_per_item, null_val, default_value == null ? null_val : default_value, size ); }
		
		// Methods to add values to the list, converting to null value if necessary
		public RW add1( Byte value ) { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( Character value )            { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( Short value )                { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( Integer value )              { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( Long value )                 { return add1( size, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Byte value )      { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Character value ) { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Short value )     { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Integer value )   { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( int index, Long value )      { return add1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW add1( long value )                 { return add1( size, value & 0xFF ); }
		// Method to add a value at a specific index
		public RW add1( int index, long src ) {
			if( index < size ) add( this, index, src );
			else set1( index, src );
			return this;
		}
		
		// Methods to remove values from the list, converting to null value if necessary
		public RW remove( Byte value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( Character value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( Short value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( Integer value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( Long value ) {
			remove( this, value == null ? null_val : value & 0xFF );
			return this;
		}
		
		public RW remove( int value ) {
			remove( this, value );
			return this;
		}
		
		// Method to remove a value at a specific index
		public RW removeAt( int item ) {
			removeAt( this, item );
			return this;
		}
		
		// Methods to set values in the list, converting to null value if necessary
		public RW set1( int index, Byte value ) { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, Character value ) { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, Short value )     { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, Integer value )   { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, Long value )      { return set1( index, value == null ? null_val : value & 0xFF ); }
		
		public RW set1( int index, long value ) {
			set1( this, index, value );
			return this;
		}
		
		// Methods to set multiple values in the list
		public RW set( int index, Byte... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int index, Byte[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, values[src_index + i] == null ? null_val : values[src_index + i] & 0xFF );
			return this;
		}
		
		public RW set( int item, Character... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, item + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int index, Short... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int index, Integer... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[i] == null ? null_val : values[i] & 0xFF );
			return this;
		}
		
		public RW set( int index, Long... values ) {
			
			for( int i = values.length; -1 < --i; )
			     set1( this, index + i, values[i] == null ? null_val : (int) (values[i] & 0xFF) );
			return this;
		}
		
		public RW set( int index, byte... values ) {
			set( this, index, values );
			return this;
		}
		
		public RW set( int index, byte[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( this, index + i, src[src_index + i] );
			return this;
		}
		
		public RW set( int index, char... values ) {
			set( this, index, values );
			return this;
		}
		
		public RW set( int index, short... values ) {
			set( this, index, values );
			return this;
		}
		
		public RW set( int index, int... values ) {
			set( this, index, values );
			return this;
		}
		
		public RW set( int index, long... values ) {
			set( this, index, values );
			return this;
		}
		// Method to adjust the internal length to fit the current size.
		public RW fit() { return length( size ); }
		
		public RW length( int items ) {
			if( items < 1 )
			{
				values = Array.EqualHashOf.longs.O;
				size   = 0;
			}
			else length_( items );
			return this;
		}
		// Method to set the size of the list
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( this.size < size ) set1( size - 1, default_value );
			else this.size = size;
			return this;
		}
		// Clones the current instance
		@Override public RW clone() { return (RW) super.clone(); }
	}
	
}
