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

public interface ByteDoubleNullMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static int token( R src, int token )        { return ByteSet.NonNullKeysIterator.token( src.keys, token ); }
		
		static byte key( R src, int token ) { return (byte) (ByteSet.NonNullKeysIterator.key( src.keys, token )); }
		
		static boolean hasValue( R src, int token ) { return src.values.hasValue( ByteSet.NonNullKeysIterator.index( null, token ) ); }
		
		static double value( R src, int token ) { return  src.values.get( ByteSet.NonNullKeysIterator.index( null, token ) ); }
	}
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		ByteSet.RW             keys = new ByteSet.RW();
		DoubleNullList.RW values;
		
		
		public int size()                                     { return keys.size(); }
		
		public boolean isEmpty()                              { return keys.isEmpty(); }
		
		
		public boolean contains(  Byte      key )           { return !hasNone( token( key ) ); }
		
		public boolean contains( byte key )           { return !hasNone( token( key ) ); }
		
		
		public @Positive_Values int token(  Byte      key ) { return key == null ? hasNullKey == Positive_Values.VALUE ? 256 : hasNullKey : token( (byte) (key + 0) ); }
		
		public @Positive_Values int token( byte key ) {
			if( keys.contains( (byte) key ) )
			{
				int i = keys.rank( (byte) key ) - 1;
				return values.hasValue( i ) ? i : Positive_Values.NULL;
			}
			return Positive_Values.NONE;
		}
		
		public boolean hasValue( int token ) { return -1 < token; }
		
		public boolean hasNone( int token )  { return token == Positive_Values.NONE; }
		
		public boolean hasNull( int token )  { return token == Positive_Values.NULL; }
		
		public double value( @Positive_ONLY int token ) { return token == 256 ? NullKeyValue :    values.get( (byte) token ); }
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		double NullKeyValue = 0;
		
		public int hashCode() {
			return Array.finalizeHash( Array.hash( Array.hash( hasNullKey == Positive_Values.NULL ? 553735009 : hasNullKey == Positive_Values.NONE ? 10019689 : Array.hash( NullKeyValue ), keys ), values ), size() );
		}
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			return other != null && hasNullKey == other.hasNullKey &&
			       (hasNullKey != Positive_Values.VALUE || NullKeyValue == other.NullKeyValue)
			       && other.keys.equals( keys ) && other.values.equals( values );
		}
		
		@Override public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e )
			{
				e.printStackTrace();
			}
			return null;
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			json.enterObject();
			
			switch( hasNullKey )
			{
				case Positive_Values.NULL:
					json.name().value();
					break;
				case Positive_Values.VALUE:
					if( keys.hasNullKey ) json.name().value( NullKeyValue );
			}
			
			int size = keys.size;
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				for( int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
				{
					json.name( NonNullKeysIterator.key( this, token ) );
					if( NonNullKeysIterator.hasValue( this, token ) ) json.value();
					else json.value( NonNullKeysIterator.value( this, token ) );
				}
			}
			
			json.exitObject();
		}
	}
	interface Interface {
		int size();
		boolean contains(  Byte      key );
		boolean contains( byte key );
		@Positive_Values int token(  Byte      key );
		@Positive_Values int token( byte key );
		default boolean hasValue( int token ) { return -1 < token; }
		boolean put(  Byte      key, double value );
		boolean put(  Byte      key,  Double    value );
		boolean put( byte key,  Double    value );
		boolean put( byte key, double value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length ) { values = new DoubleNullList.RW( 265 < length ? 256 : length ); }
		
		
		public RW clear() {
			keys.clear();
			values.clear();
			return this;
		}
		
		
		public boolean put(  Byte      key, double value ) {
			if( key != null ) return put( (byte) (key + 0), value );
			
			keys.add( null );
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			NullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put(  Byte      key,  Double    value ) {
			if( key != null ) return put( (byte) (key + 0), value );
			
			keys.add( null );
			int h = hasNullKey;
			
			if( value == null )
			{
				hasNullKey = Positive_Values.NULL;
				return h == Positive_Values.NULL;
			}
			
			hasNullKey   = Positive_Values.VALUE;
			NullKeyValue = value;
			return h == Positive_Values.VALUE;
		}
		
		
		public boolean put( byte key,  Double    value ) {
			if( value != null ) return put( key, (double) value );
			
			
			if( keys.add( (byte) key ) )
			{
				values.add1( keys.rank( (byte) key ) - 1, ( Double   ) null );
				return true;
			}
			values.set1( keys.rank( (byte) key ) - 1, ( Double   ) null );
			
			return false;
		}
		
		public boolean put( byte key, double value ) {
			
			if( keys.add( (byte) key ) )
			{
				values.add1( keys.rank( (byte) key ) - 1, value );
				return true;
			}
			
			values.set1( keys.rank( (byte) key ) - 1, value );
			return false;
		}
		
		public boolean remove(  Byte       key ) {
			if( key == null )
			{
				hasNullKey = Positive_Values.NONE;
				keys.remove( null );
			}
			
			return remove( (byte) (key + 0) );
		}
		
		public boolean remove( byte key ) {
			final byte k = (byte) key;
			if( !keys.contains( k ) ) return false;
			
			values.remove( keys.rank( k ) - 1 );
			keys.remove( k );
			return true;
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
