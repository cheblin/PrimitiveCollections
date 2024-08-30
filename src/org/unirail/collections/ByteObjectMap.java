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

public interface ByteObjectMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static < V > int token( R< V > src, int token ) { return ByteSet.NonNullKeysIterator.token( src.keys, token ); }
		
		static < V > byte key( R< V > src, int token ) { return ( byte ) ByteSet.NonNullKeysIterator.key( src.keys, token ); }
		
		static < V > V value( R< V > src, int token )   { return src.values[ ByteSet.NonNullKeysIterator.index( src.keys, token ) ]; }
	}
	
	abstract class R< V > implements Cloneable, JsonWriter.Source {
		
		public          ByteSet.RW    keys = new ByteSet.RW();
		public          V[]                          values;
		protected final Array.EqualHashOf< V > equal_hash_V;
		
		protected R( Class< V > clazz )                             { equal_hash_V = Array.get( clazz ); }
		
		public R( Array.EqualHashOf< V > equal_hash_V ) { this.equal_hash_V = equal_hash_V; }
		
		public int size()                           { return keys.size(); }
		
		public boolean isEmpty()                    { return keys.isEmpty(); }
		
		
		public boolean contains(  Byte      key ) { return key == null ? keys.contains( null ) : keys.contains( ( byte ) ( key + 0 ) ); }
		
		public boolean contains( int key )          { return keys.contains( ( byte ) key ); }
		
		public V value(  Byte      key )          { return key == null ? NullKeyValue : value( key + 0 ); }
		
		public V value( int key )                   { return values[ keys.rank( ( byte ) key ) ]; }
		
		V NullKeyValue = null;
		
		public int hashCode()                                                { return Array.finalizeHash( Array.hash( Array.hash( Array.hash( keys.contains( null ) ? Array.hash( NullKeyValue ) : 29399999 ), keys ) ), size() ); }
		
		@SuppressWarnings( "unchecked" ) public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( getClass().cast( obj ) ); }
		
		public boolean equals( R< V > other )                                { return other != null && keys.equals( other.keys ) && equal_hash_V.equals( values, other.values, size() ) && (!keys.hasNullKey || NullKeyValue == other.NullKeyValue ); }
		
		@SuppressWarnings( "unchecked" ) public R< V > clone() {
			try {
				R< V > dst = ( R< V > ) super.clone();
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e ) { e.printStackTrace(); }
			return null;
		}
		
		public Array.ISort.Objects< V > getV = null;
		
		
		public void build( Array.ISort.Anything.Index dst ) {
			if( dst.dst == null || dst.dst.length < size() ) dst.dst = new char[ size() ];
			dst.size = size();
			
			dst.src = getV == null ? getV = new Array.ISort.Objects< V >() {
				@Override V get( int index ) { return values[ index ]; }
			} : getV;
			
			for( int token = NonNullKeysIterator.INIT, k = 0; ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT; )
			     dst.dst[ k++ ] = ( char ) token;
			
			Array.ISort.sort( dst, 0, dst.size - 1 );
		}
		
		public void build( Array.ISort.Anything.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < size() ) dst.dst = new int[ size() ];
			dst.size = size();
			
			dst.src = getV == null ? getV = new Array.ISort.Objects< V >() {
				@Override V get( int index ) { return values[ index ]; }
			} : getV;
			
			for( int token = NonNullKeysIterator.INIT, k = 0; ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT; )
			     dst.dst[ k++ ] = ( char ) token;
			
			Array.ISort.sort( dst, 0, dst.size - 1 );
		}
		
		
		public String toString() { return toJSON(); }
		
		@Override public void toJSON( JsonWriter json ) {
			json.enterObject();
			
			int size = keys.size();
			if( 0 < size ) {
				json.preallocate( size * 10 );
				
				if( keys.hasNullKey ) json.name().value( NullKeyValue );
				
				for( int token = NonNullKeysIterator.INIT; ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT; )
				     json.
						     name( NonNullKeysIterator.key( this, token ) ).
						     value( NonNullKeysIterator.value( this, token ) );
			}
			
			json.exitObject();
		}
	}
	
	interface Interface< V > {
		int size();
		
		boolean contains(  Byte      key );
		
		boolean contains( int key );
		
		V value(  Byte      key );
		
		V value( int key );
		
		boolean put(  Byte      key, V value );
		
		boolean put( byte key, V value );
	}
	
	class RW< V > extends R< V > implements Interface< V > {
		
		public RW( Class< V > clazz, int length ) { this( Array.get( clazz ), length ); }
		
		public RW( Array.EqualHashOf< V > equal_hash_V, int length ) {
			super( equal_hash_V );
			values = this.equal_hash_V.copyOf( null, 265 < length ? 256 : length );
		}
		
		public RW clear() {
			NullKeyValue = null;
			java.util.Arrays.fill( values, 0, size() - 1, null );
			keys.clear();
			return this;
		}
		
		public boolean put(  Byte      key, V value ) {
			if( key == null ) {
				NullKeyValue = value;
				return keys.add( null );
			}
			
			return put( ( byte ) ( key + 0 ), value );
		}
		
		public boolean put( byte key, V value ) {
			boolean ret = keys.add( ( byte ) key );
			values[ keys.rank( ( byte ) key ) - 1 ] = value;
			return ret;
		}
		
		public boolean remove(  Byte       key ) {
			if( key == null ) {
				NullKeyValue = null;
				return keys.remove( null );
			}
			return remove( ( byte ) ( key + 0 ) );
		}
		
		public boolean remove( byte key ) {
			final byte k = ( byte ) key;
			if( !keys.contains( k ) ) return false;
			
			Array.resize( values, values, keys.rank( k ) - 1, size(), -1 );
			
			keys.remove( k );
			
			return true;
		}
		
		public RW< V > clone() { return ( RW< V > ) super.clone(); }
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	@SuppressWarnings( "unchecked" )
	static < V > Array.EqualHashOf< RW< V > > equal_hash() { return (Array.EqualHashOf< RW< V > >) RW.OBJECT; }
}
