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


public interface UIntList {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		public final long default_value;
		protected R( long default_value ) { this.default_value = default_value; }
		
		int[] values = Array.EqualHashOf.ints     .O;
		
		public int[] array() { return values; }
		
		int size = 0;
		
		public int size()                            { return size; }
		
		public boolean isEmpty()                     { return size == 0; }
		
		public boolean contains( long value ) { return -1 < indexOf( value ); }
		
		
		public long[] toArray( int index, int len, long[] dst ) {
			if( size == 0 ) return null;
			if( dst == null ) dst = new long[len];
			for( int i = 0; i < len; i++ ) dst[index + i] = (0xFFFFFFFFL &  values[i]);
			
			return dst;
		}
		
		public boolean containsAll( R src ) {
			for( int i = src.size(); -1 < --i; )
				if( !contains( src.get( i ) ) ) return false;
			
			return true;
		}
		
		public long get( int index ) { return  (0xFFFFFFFFL &  values[index]); }
		
		public int get(long[] dst, int dst_index, int src_index, int len ) {
			len = Math.min( Math.min( size - src_index, len ), dst.length - dst_index );
			if( len < 1 ) return 0;
			
			for( int i = 0; i < len; i++ )
			     dst[dst_index++] = (0xFFFFFFFFL &  values[src_index++]);
			
			return len;
		}
		
		public int indexOf( long value ) {
			for( int i = 0; i < size; i++ )
				if( values[i] == value ) return i;
			return -1;
		}
		
		public int lastIndexOf( long value ) {
			for( int i = size - 1; -1 < i; i-- )
				if( values[i] == value ) return i;
			return -1;
		}
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			if( other == null || other.size != size ) return false;
			
			for( int i = size(); -1 < --i; )
				if( values[i] != other.values[i] ) return false;
			return true;
		}
		
		public final int hashCode() {
			switch( size )
			{
				case 0:
					return Array.finalizeHash( seed, 0 );
				case 1:
					return Array.finalizeHash( Array.mix( seed, Array.hash( values[0] ) ), 1 );
			}
			
			final int initial   = Array.hash( values[0] );
			int       prev      = Array.hash( values[1] );
			final int rangeDiff = prev - initial;
			int       h         = Array.mix( seed, initial );
			
			for( int i = 2; i < size; ++i )
			{
				h = Array.mix( h, prev );
				final int hash = Array.hash( values[i] );
				if( rangeDiff != hash - prev )
				{
					for( h = Array.mix( h, hash ), ++i; i < size; ++i )
					     h = Array.mix( h, Array.hash( values[i] ) );
					
					return Array.finalizeHash( h, size );
				}
				prev = hash;
			}
			
			return Array.avalanche( Array.mix( Array.mix( h, rangeDiff ), prev ) );
		}
		private static final int seed = R.class.hashCode();
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				
				dst.values = values.clone();
				dst.size   = size;
				
				return dst;
				
			} catch( CloneNotSupportedException e )
			{
				e.printStackTrace();
			}
			return null;
			
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			json.enterArray();
			int size = size();
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				for( int i = 0; i < size; i++ ) json.value( get( i ) );
			}
			json.exitArray();
		}
	}
	
	interface Interface {
		int size();
		
		long get( int index );
		
		RW add1( long value );
		
		RW set1( int index, long value );
	}
	
	class RW extends R implements Interface {
		
		/**
		 Constructor initializing with a specified length.
		 
		 @param length The length of the array to be created.
		 */
		public RW( int length ) {
			super( (long) 0 );
			// Create a new array if length > 0, otherwise use an empty array
			values = 0 < length ? new int[length] : Array.EqualHashOf.ints     .O;
		}
		
		/**
		 Constructor initializing with default value and size.
		 
		 @param default_value The default int value to initialize elements with.
		 @param size          The initial size of the array. If negative, the absolute value is used.
		 If size is positive, initialize with the default value.
		 */
		public RW( long default_value, int size ) {
			super( default_value );
			
			values = size == 0 ? Array.EqualHashOf.ints     .O : new int[ this.size = size < 0 ? -size :  size];// Use absolute value of size
			
			if( size < 1 || default_value == 0 ) return;// Skip initialization if size was negative or if default_value is 0
			while( -1 < --size ) values[size] = (int) default_value;// Initialize all elements with the default value
		}
		
		public RW add1( long value ) { return add1( size, value ); }
		
		public RW add1( int index, long value ) {
			
			int max = Math.max( index, size + 1 );
			
			size          = Array.resize( values, values.length <= max ? values = new int[max + max / 2] : values, index, size, 1 );
			values[index] = (int) value;
			return this;
		}
		
		public RW add( int... src ) { return add( size(), src, 0, src.length ); }
		public RW add( int index, int[] src, int src_index, int len ) {
			int max = Math.max( index, size ) + len;
			
			size = Array.resize( values, values.length < max ? values = new int[max + max / 2] : values, index, size, len );
			
			for( int i = 0; i < len; i++ ) values[index + i] = (int) src[src_index + i];
			return this;
		}
		
		
		public RW remove() { return remove( size - 1 ); }
		
		public RW remove( int index ) {
			if( size < 1 || size < index ) return this;
			if( index == size - 1 ) size--;
			else size = Array.resize( values, values, index, size, -1 );
			
			return this;
		}
		
		public RW remove( int index, int len ) {
			if( size < 1 || size < index ) return this;
			int s = size;
			if( index == size - 1 ) size--;
			else size = Array.resize( values, values, index, size, -len );
			
			return this;
		}
		
		public RW set1( long value ) { return set1( size, value ); }
		
		public RW set1( int index, long value ) {
			
			if( size <= index )
			{
				if( values.length <= index ) values = Arrays.copyOf( values, index + index / 2 );
				
				if( default_value != 0 ) Arrays.fill( values, size, index, (int) default_value );
				
				size = index + 1;
			}
			
			values[index] = (int) value;
			return this;
		}
		
		public RW set( int index, long... src ) { return set( index, src, 0, src.length ); }
		
		
		public RW set( int index, long[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( index + i, src[src_index + i] );
			return this;
		}
		
		public RW swap( int index1, int index2 ) {
			final int tmp = values[index1];
			values[index1] = values[index2];
			values[index2] = tmp;
			return this;
		}
		
		public int removeAll( R src ) {
			int fix = size;
			
			for( int i = 0, k, src_size = src.size(); i < src_size; i++ )
				if( -1 < (k = indexOf( src.get( i ) )) ) remove( k );
			return fix - size;
		}
		
		public int removeAll( long src ) {
			int fix = size;
			
			for( int k; -1 < (k = indexOf( src )); ) remove( k );
			return fix - size;
		}
		
		//remove with change order
		public int removeAll_fast( long src ) {
			int fix = size;
			
			for( int k; -1 < (k = indexOf( src )); ) remove_fast( k );
			return fix - size;
		}
		
		//remove with change order
		public RW remove_fast( int index ) {
			if( size < 1 || size <= index ) return this;
			values[index] = values[--size];
			return this;
		}
		
		public boolean retainAll( R chk ) {
			
			final int fix = size;
			
			for( int index = 0; index < size; index++ )
				if( !chk.contains( get( index ) ) )
					remove( index );
			
			return fix != size;
		}
		
		public RW clear() {
			size = 0;
			return this;
		}
		
		// Method to adjust the internal length to fit the current size.
		public RW fit() { return length( size() ); }
		
		public RW length( int length ) {
			if( values.length != length )
				if( length < 1 )
				{
					values = Array.EqualHashOf.ints     .O;
					size   = 0;
					return this;
				}
			
			Arrays.copyOf( values, length );
			if( length < size ) size = length;
			
			return this;
		}
		
		public RW size( int size ) {
			if( size < 1 ) clear();
			else if( size() < size ) set1( size - 1, default_value );
			else this.size = size;
			return this;
		}
		
		
		public RW clone() { return (RW) super.clone(); }
		
	}
}
