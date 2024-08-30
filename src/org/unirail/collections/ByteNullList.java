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

public interface ByteNullList {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		
		BitList.RW         nulls;
		ByteList.RW values;
		
		
		public int length()                                 { return nulls.length(); }
		
		public int size()                                   { return nulls.size; }
		
		public boolean isEmpty()                            { return size() < 1; }
		
		public boolean hasValue( int index )                { return nulls.get( index ); }
		
		@Positive_OK public int nextValueIndex( int index ) { return nulls.next1( index ); }
		
		@Positive_OK public int prevValueIndex( int index ) { return nulls.prev1( index ); }
		
		@Positive_OK public int nextNullIndex( int index )  { return nulls.next0( index ); }
		
		@Positive_OK public int prevNullIndex( int index )  { return nulls.prev0( index ); }
		
		public byte get( @Positive_ONLY int index ) { return (byte) values.get( nulls.rank( index ) - 1 ); }
		
		
		public int indexOf( byte value ) {
			int i = values.indexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		
		public int lastIndexOf( byte value ) {
			int i = values.lastIndexOf( value );
			return i < 0 ? i : nulls.bit( i );
		}
		
		
		public boolean equals( Object obj ) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public int hashCode()            { return Array.finalizeHash( Array.hash( Array.hash( nulls ), values ), size() ); }
		
		
		public boolean equals( R other ) { return other != null && other.size() == size() && values.equals( other.values ) && nulls.equals( other.nulls ); }
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				dst.values = values.clone();
				dst.nulls  = nulls.clone();
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
				for( int i = 0, ii; i < size; )
					if( (ii = nextValueIndex( i )) == i ) json.value( get( i++ ) );
					else if( ii == -1 || size <= ii )
					{
						while( i++ < size ) json.value();
						break;
					}
					else for( ; i < ii; i++ ) json.value();
			}
			json.exitArray();
		}
		
		
		protected static void set( R dst, int index,  Byte      value ) {
			
			if( value == null )
			{
				if( dst.size() <= index ) dst.nulls.set0( index );//resize
				else if( dst.nulls.get( index ) )
				{
					dst.values.remove( dst.nulls.rank( index ) );
					dst.nulls.set0( index );
				}
			}
			else set( dst, index, value. byteValue     () );
		}
		
		protected static void set( R dst, int index, byte value ) {
			
			if( dst.nulls.get( index ) ) dst.values.set1( dst.nulls.rank( index ) - 1, value );
			else
			{
				dst.nulls.set1( index );
				dst.values.add1( dst.nulls.rank( index ) - 1, value );
			}
		}
	}
	
	interface Interface {
		int size();
		
		boolean hasValue( int index );
		
		byte get( @Positive_ONLY int index );
		
		RW add1(  Byte      value );
		
		RW add1( int index,  Byte      value );
		
		RW add1( int index, byte value );
		
		RW set1( int index,  Byte      value );
		
		RW set1( int index, byte value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length ) {
			super();
			
			nulls  = new BitList.RW( length );
			values = new ByteList.RW( length );
		}
		
		/**
		 Constructor initializing with a boxed Integer default value and size.
		 
		 @param default_value The default Integer value to initialize elements with.
		 @param size          The initial size of the array.
		 If size is positive, initialize with the default value.
		 */
		public RW(  Byte      default_value, int size ) {
			super();
			
			// Initialize nulls BitList: true if default_value is not null
			nulls = new BitList.RW( default_value != null, size );
			
			// Initialize values IntList: use 0 if default_value is null, otherwise use its int value
			values = new ByteList.RW( default_value == null ? 0 : default_value. byteValue     (), size );
			if( size < 0 ) values.clear();
		}
		
		/**
		 Constructor initializing with a primitive int default value and size.
		 
		 @param default_value The default int value to initialize elements with.
		 @param size          The initial size of the array.
		 If size is positive, initialize with the default value.
		 */
		public RW( byte default_value, int size ) {
			super();
			
			// Initialize nulls BitList: always false since primitive int cannot be null
			nulls = new BitList.RW( false, size );
			
			// Initialize values IntList with the given default value
			values = new ByteList.RW( default_value, size );
		}
		
		public RW clone()  { return (RW) super.clone(); }
		
		public RW remove() { return remove( size() - 1 ); }
		
		public RW remove( int index ) {
			if( size() < 1 || size() <= index ) return this;
			
			if( nulls.get( index ) ) values.remove( nulls.rank( index ) - 1 );
			nulls.remove( index );
			return this;
		}
		
		public RW set1(  Byte      value ) {
			set( this, size() - 1, value );
			return this;
		}
		
		public RW set1( byte value ) {
			set( this, size() - 1, value );
			return this;
		}
		
		public RW set1( int index,  Byte      value ) {
			set( this, index, value );
			return this;
		}
		
		public RW set1( int index, byte value ) {
			set( this, index, value );
			return this;
		}
		public RW set( int index,  Byte     ... values ) {
			for( int i = values.length; -1 < --i; )
			     set( this, index + i, values[i] );
			return this;
		}
		
		public RW set( int index, byte... values ) { return set( index, values, 0, values.length ); }
		
		public RW set( int index, byte[] values, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set( this, index + i, (byte) values[src_index + i] );
			return this;
		}
		
		public RW set( int index,  Byte     [] values, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set( this, index + i, values[src_index + i] );
			return this;
		}
		
		
		public RW add1(  Byte      value ) {
			if( value == null ) nulls.add( false );
			else add1( value. byteValue     () );
			return this;
		}
		
		public RW add1( byte value ) {
			values.add1( value );
			nulls.add( true );
			return this;
		}
		
		
		public RW add1( int index,  Byte      value ) {
			if( value == null ) nulls.add( index, false );
			else add1( index, value. byteValue     () );
			return this;
		}
		
		public RW add1( int index, byte value ) {
			if( index < size() )
			{
				nulls.add( index, true );
				values.add1( nulls.rank( index ) - 1, value );
			}
			else set1( index, value );
			return this;
		}
		
		public RW add( byte... items ) {
			int size = size();
			set1( size() + items.length - 1, values.default_value );
			return set( size, items );
		}
		
		
		public RW add(  Byte     ... items ) {
			int size = size();
			set1( size() + items.length - 1, values.default_value );
			return set( size, items );
		}
		
		public RW addAll( R src ) {
			
			for( int i = 0, s = src.size(); i < s; i++ )
				if( src.hasValue( i ) ) add1( src.get( i ) );
				else nulls.add( false );
			return this;
		}
		
		public RW clear() {
			values.clear();
			nulls.clear();
			return this;
		}
		
		public RW length( int length ) {
			nulls.length( length );
			values.length( length );
			values.size( nulls.cardinality() );
			return this;
		}
		
		public RW size( int size ) {
			nulls.size( size );
			values.size( nulls.cardinality() );
			return this;
		}
		
		// Method to adjust the internal length to fit the current size.
		public RW fit() { return length( size() ); }
		
		public RW swap( int index1, int index2 ) {
			
			int exist, empty;
			if( nulls.get( index1 ) )
				if( nulls.get( index2 ) )
				{
					values.swap( nulls.rank( index1 ) - 1, nulls.rank( index2 ) - 1 );
					return this;
				}
				else
				{
					exist = nulls.rank( index1 ) - 1;
					empty = nulls.rank( index2 );
					nulls.set0( index1 );
					nulls.set1( index2 );
				}
			else if( nulls.get( index2 ) )
			{
				exist = nulls.rank( index2 ) - 1;
				empty = nulls.rank( index1 );
				
				nulls.set1( index1 );
				nulls.set0( index2 );
			}
			else return this;
			
			byte v = values.get( exist );
			values.remove( exist );
			values.add1( empty, v );
			return this;
		}
	}
}
