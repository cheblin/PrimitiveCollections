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

public interface BoolNullList {
	
	abstract class R extends BitsList.R {
		
		protected R( int length )                        { super( 2, length ); }
		
		protected R( Boolean default_value, int size ) { super( 2, default_value == null ? 2 : default_value ? 1 : 0, size ); }
		
		public boolean hasValue( int index )            { return get( index ) != 2; }
		public Boolean get_Boolean( int index ) {
			switch( get( index ) )
			{
				case 1:
					return Boolean.TRUE;
				case 0:
					return Boolean.FALSE;
			}
			return null;
		}
		
		@Override public void toJSON( JsonWriter json ) {
			json.enterArray();
			
			json.preallocate( size * 4 );
			if( 0 < size )
			{
				long src = values[0];
				
				for( int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++ )
				{
					final int bit   = BitsList.bit( bp );
					long      value = (BitsList.BITS < bit + bits ? BitsList.value( src, src = values[BitsList.index( bp ) + 1], bit, bits, mask ) : BitsList.value( src, bit, mask ));
					
					if( (value & 2) == 2 ) json.value();
					else json.value( value == 1 );
				}
			}
			json.exitArray();
		}
		
		public R clone() { return (R) super.clone(); }
	}
	
	interface Interface {
		int size();
		Boolean get_Boolean( int index );
		
		boolean hasValue( int index );
		
		RW set1( boolean value );
		
		RW set1( Boolean value );
		
		RW add( boolean value );
		
		RW add( Boolean value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length )                        { super( length ); }
		// Constructor initializing with default value and size.
		// If size is positive, initialize with the default value.
		public RW( Boolean default_value, int size ) { super( default_value, size ); }
		
		
		public RW add( boolean value )           { add( this, value ? 1 : 0 ); return this; }
		
		public RW add( Boolean value )           { add( this, value == null ? 2 : value ? 1 : 0 ); return this;}
		
		
		public RW remove( Boolean value )        { remove( this, value == null ? 2 : value ? 1 : 0 );return this; }
		
		public RW remove( boolean value )        { remove( this, value ? 1 : 0 );return this; }
		
		
		public RW removeAt( int item )           { removeAt( this, item );return this; }
		
		
		public RW set1( boolean value )           { set1( this, size, value ? 1 : 0 ); return this;}
		
		public RW set1( Boolean value )           { set1( this, size, value == null ? 2 : value ? 1 : 0 );return this; }
		
		
		public RW set1( int item, boolean value ) { set1( this, item, value ? 1 : 0 ); return this;}
		
		public RW set1( int item, Boolean value ) { set1( this, item, value == null ? 2 : value ? 1 : 0 );return this; }
		
		
		public RW set( int index, boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set1( index + i, values[i] );
			return this;
		}
		
		public RW set( int index, Boolean... values ) {
			for( int i = 0, max = values.length; i < max; i++ )
			     set1( index + i, values[i] );
			return this;
		}
		
		
		// Method to adjust the internal length to fit the current size.
		public RW fit() { return length( size() ); }
		
		public RW length( int items ) {
			if( items < 1 ) {
				values = Array.EqualHashOf.longs.O;
				size=0;
			}
			else length_( items );
			return this;
		}
		
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( this.size < size ) set1(this, size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		
		public RW clone()   { return (RW) super.clone(); }
	}
}

