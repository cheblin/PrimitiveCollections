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

/**
 * Defines interfaces and classes for lists that efficiently store boolean values,
 * supporting a tri-state logic: true, false, and null.
 * <p>
 * This structure is built upon {@link BitsList} and uses 2 bits per element to represent
 * boolean values along with an explicit null state.
 */
public interface BoolNullList {
	
	/**
	 * Abstract base class for {@link BitsList} implementations tailored for tri-state boolean lists (true, false, null).
	 * <p>
	 * Extends {@link BitsList.R} and sets up the foundation for handling boolean lists with null values,
	 * using 2 bits per boolean element.
	 */
	abstract class R extends BitsList.R {
		
		/**
		 * Constructs a new BoolNullList.R with a specified initial length.
		 * Uses 2 bits per item to represent boolean (true, false, null) states.
		 *
		 * @param length The initial length of the boolean list.
		 */
		protected R( int length ) { super( 2, length ); }
		
		/**
		 * Constructs a new BoolNullList.R with a default boolean value and a specified size.
		 * Uses 2 bits per item.
		 *
		 * @param default_value The default Boolean value to initialize the list with.
		 *                      {@code null} represents the null state, {@code true} for true, and {@code false} for false.
		 * @param size          The initial size of the boolean list. If negative, the absolute value is used.
		 *                      If size is positive, initialize with the default value.
		 */
		protected R( Boolean default_value, int size ) {
			super( 2, default_value == null ?
					2 :
					default_value ?
							1 :
							0, size );
		}
		
		/**
		 * Checks if the boolean value at the given index is not null.
		 *
		 * @param index The index to check.
		 * @return {@code true} if the value at the index is either true or false (not null), {@code false} otherwise (null).
		 */
		public boolean hasValue( int index ) { return get( index ) != 2; }
		
		/**
		 * Gets the boolean value at the specified index as a {@link Boolean} object.
		 *
		 * @param index The index of the boolean value to retrieve.
		 * @return {@link Boolean#TRUE} if the value is true, {@link Boolean#FALSE} if false, and {@code null} if the value is null.
		 */
		public Boolean get_Boolean( int index ) {
			switch( get( index ) ) {
				case 1:
					return Boolean.TRUE;
				case 0:
					return Boolean.FALSE;
			}
			return null;
		}
		
		/**
		 * Writes the boolean list to a JSON writer.
		 * Represents boolean true as JSON true, boolean false as JSON false, and null as JSON null.
		 *
		 * @param json The {@link JsonWriter} to write the JSON representation to.
		 */
		@Override public void toJSON( JsonWriter json ) {
			json.enterArray();
			
			json.preallocate( size * 4 ); // Estimate buffer size for JSON output
			if( 0 < size ) {
				long src = values[ 0 ];
				
				for( int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++ ) {
					final int bit = BitsList.R.bit( bp );
					long value = ( BitsList.R.BITS < bit + bits ?
							BitsList.R.value( src, src = values[ BitsList.R.index( bp ) + 1 ], bit, bits, mask ) :
							BitsList.R.value( src, bit, mask ) );
					
					if( ( value & 2 ) == 2 ) json.value(); // Write JSON null for null state (represented by 2)
					else json.value( value == 1 );       // Write JSON true for 1, JSON false for 0
				}
			}
			json.exitArray();
		}
		
		/**
		 * Creates and returns a shallow copy of this BoolNullList.R instance.
		 *
		 * @return A clone of this instance.
		 */
		@Override public R clone() { return ( R ) super.clone(); }
	}
	
	/**
	 * Interface defining read and write operations for BoolNullList implementations.
	 */
	interface Interface {
		/**
		 * Returns the number of elements in the list.
		 *
		 * @return The size of the list.
		 */
		int size();
		
		/**
		 * Gets the boolean value at the specified index as a {@link Boolean} object.
		 *
		 * @param index The index of the boolean value to retrieve.
		 * @return {@link Boolean#TRUE} if the value is true, {@link Boolean#FALSE} if false, and {@code null} if the value is null.
		 */
		Boolean get_Boolean( int index );
		
		/**
		 * Checks if the boolean value at the given index is not null.
		 *
		 * @param index The index to check.
		 * @return {@code true} if the value at the index is either true or false (not null), {@code false} otherwise (null).
		 */
		boolean hasValue( int index );
		
		/**
		 * Sets the boolean value at the end of the list to the given boolean value.
		 *
		 * @param value The boolean value to set (true or false).
		 * @return The RW instance for method chaining.
		 */
		RW set1( boolean value );
		
		/**
		 * Sets the boolean value at the end of the list to the given {@link Boolean} value.
		 *
		 * @param value The {@link Boolean} value to set ({@code true}, {@code false}, or {@code null}).
		 * @return The RW instance for method chaining.
		 */
		RW set1( Boolean value );
		
		/**
		 * Adds a new boolean value to the end of the list.
		 *
		 * @param value The boolean value to add (true or false).
		 * @return The RW instance for method chaining.
		 */
		RW add( boolean value );
		
		/**
		 * Adds a new {@link Boolean} value to the end of the list.
		 *
		 * @param value The {@link Boolean} value to add ({@code true}, {@code false}, or {@code null}).
		 * @return The RW instance for method chaining.
		 */
		RW add( Boolean value );
	}
	
	/**
	 * Concrete implementation of {@link BoolNullList} that allows read and write operations for tri-state boolean lists.
	 * <p>
	 * Extends {@link BoolNullList.R} and implements {@link BoolNullList.Interface}, providing a mutable list
	 * capable of storing boolean true, boolean false, and null values efficiently using bits.
	 */
	class RW extends R implements Interface {
		
		/**
		 * Constructs a new RW boolean list with a specified initial length.
		 *
		 * @param length The initial length of the list.
		 */
		public RW( int length ) { super( length ); }
		
		/**
		 * Constructs a new RW boolean list with a default boolean value and a specified size.
		 * If size is positive, the list is initialized with the default value.
		 * If size is negative, the absolute value is used for size, and the list is not initialized with default values.
		 *
		 * @param default_value The default {@link Boolean} value for the list.
		 *                      {@code null} represents the null state, {@code true} for true, and {@code false} for false.
		 * @param size          The initial size of the list.
		 */
		public RW( Boolean default_value, int size ) { super( default_value, size ); }
		
		
		/**
		 * Adds a boolean value to the end of the list.
		 *
		 * @param value The boolean value to add (true or false).
		 * @return This RW instance for method chaining.
		 */
		public RW add( boolean value ) {
			add( this, value ?
					1 :
					0 ); return this;
		}
		
		/**
		 * Adds a {@link Boolean} value to the end of the list.
		 *
		 * @param value The {@link Boolean} value to add ({@code true}, {@code false}, or {@code null}).
		 * @return This RW instance for method chaining.
		 */
		public RW add( Boolean value ) {
			add( this, value == null ?
					2 :
					value ?
							1 :
							0 ); return this;
		}
		
		
		/**
		 * Removes the first occurrence of the given {@link Boolean} value from the list.
		 *
		 * @param value The {@link Boolean} value to remove ({@code true}, {@code false}, or {@code null}).
		 * @return This RW instance for method chaining.
		 */
		public RW remove( Boolean value ) {
			remove( this, value == null ?
					2 :
					value ?
							1 :
							0 ); return this;
		}
		
		/**
		 * Removes the first occurrence of the given boolean value from the list.
		 *
		 * @param value The boolean value to remove (true or false).
		 * @return This RW instance for method chaining.
		 */
		public RW remove( boolean value ) {
			remove( this, value ?
					1 :
					0 ); return this;
		}
		
		
		/**
		 * Removes the element at the specified index from the list.
		 *
		 * @param item The index of the element to remove.
		 * @return This RW instance for method chaining.
		 */
		public RW removeAt( int item ) { removeAt( this, item ); return this; }
		
		
		/**
		 * Sets the boolean value at the end of the list. Effectively appends a value.
		 *
		 * @param value The boolean value to set (true or false).
		 * @return This RW instance for method chaining.
		 */
		public RW set1( boolean value ) {
			set1( this, size, value ?
					1 :
					0 ); return this;
		}
		
		/**
		 * Sets the {@link Boolean} value at the end of the list. Effectively appends a value.
		 *
		 * @param value The {@link Boolean} value to set ({@code true}, {@code false}, or {@code null}).
		 * @return This RW instance for method chaining.
		 */
		public RW set1( Boolean value ) {
			set1( this, size, value == null ?
					2 :
					value ?
							1 :
							0 ); return this;
		}
		
		
		/**
		 * Sets the boolean value at the specified index.
		 *
		 * @param item  The index to set the value at.
		 * @param value The boolean value to set (true or false).
		 * @return This RW instance for method chaining.
		 */
		public RW set1( int item, boolean value ) {
			set1( this, item, value ?
					1 :
					0 ); return this;
		}
		
		/**
		 * Sets the {@link Boolean} value at the specified index.
		 *
		 * @param item  The index to set the value at.
		 * @param value The {@link Boolean} value to set ({@code true}, {@code false}, or {@code null}).
		 * @return This RW instance for method chaining.
		 */
		public RW set1( int item, Boolean value ) {
			set1( this, item, value == null ?
					2 :
					value ?
							1 :
							0 ); return this;
		}
		
		
		/**
		 * Sets a range of boolean values starting at the specified index.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of boolean values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set1( index + i, values[ i ] );
			return this;
		}
		
		/**
		 * Sets a range of {@link Boolean} values starting at the specified index.
		 *
		 * @param index  The starting index to set values.
		 * @param values An array of {@link Boolean} values to set.
		 * @return This RW instance for method chaining.
		 */
		public RW set( int index, Boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set1( index + i, values[ i ] );
			return this;
		}
		
		
		/**
		 * Adjusts the internal array length to fit the current size, potentially saving memory.
		 *
		 * @return This RW instance for method chaining.
		 */
		public RW fit() { return length( size() ); }
		
		/**
		 * Sets the internal array length to accommodate the specified number of items.
		 * If {@code items} is less than 1, the internal array is cleared.
		 *
		 * @param items The desired number of items the internal array should accommodate.
		 * @return This RW instance for method chaining.
		 */
		public RW length( int items ) {
			if( items < 1 ) {
				values = Array.EqualHashOf._longs.O; // Clear the array if items is less than 1
				size   = 0;
			}
			else length_( items ); // Adjust length if items is positive
			return this;
		}
		
		/**
		 * Sets the size of the list.
		 * <p>
		 * If the new {@code size} is smaller than the current size, elements at the end are effectively removed.
		 * If the new {@code size} is larger, new elements are added and initialized with the default value.
		 * If {@code size} is less than 1, the list is cleared.
		 *
		 * @param size The new size of the list.
		 * @return This RW instance for method chaining.
		 */
		public RW size( int size ) {
			if( size < 1 ) clear(); // Clear if size is less than 1
			else if( this.size < size ) set1( this, size - 1, default_value ); // Extend with default value if size increases
			else this.size = size; // Just update size if it's within bounds or decreasing
			return this;
		}
		
		
		/**
		 * Creates and returns a shallow copy of this RW instance.
		 *
		 * @return A clone of this instance.
		 */
		@Override public RW clone() { return ( RW ) super.clone(); }
	}
}