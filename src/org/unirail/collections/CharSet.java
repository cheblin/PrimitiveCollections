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

public interface CharSet {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token( R src, int token ) {
			for( int len = src.keys.length; ; )
				if( ++token == len ) return src.has0Key ? len : INIT;
				else if( token == len + 1 ) return INIT;
				else if( src.keys[token] != 0 ) return token;
		}
		
		static char key( R src, int token ) { return token == src.keys.length ? 0 : (char)  src.keys[token]; }
	}
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		public char[] keys = new char[0];
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean has0Key;
		boolean hasNullKey;
		
		protected double loadFactor;
		
		
		public boolean contains(  Character key ) { return key == null ? hasNullKey : contains( key. charValue      () ); }
		
		public boolean contains( char key ) {
			if( key == 0 ) return has0Key;
			
			int slot = Array.hash( key ) & mask;
			
			for( char k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key ) return true;
			return false;
		}
		
		
		public boolean isEmpty() { return size() == 0; }
		
		
		public int size()        { return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0); }
		
		//Compute a hash that is symmetric in its arguments - that is a hash
		//where the order of appearance of elements does not matter.
		//This is useful for hashing sets, for example.
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
		private static final int seed = R.class.hashCode();
		
		public char[] toArray( char[] dst ) {
			final int size = size();
			if( dst == null || dst.length < size ) dst = new char[size];
			
			for( int i = keys.length, ii = 0; -1 < --i; )
				if( keys[i] != 0 ) dst[ii++] = keys[i];
			
			return dst;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			if( other == null || other.assigned != assigned ) return false;
			
			for( char k : keys ) if( k != 0 && !other.contains( k ) ) return false;
			
			return true;
		}
		
		
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				
				dst.keys = keys.clone();
				return dst;
				
			} catch( CloneNotSupportedException e )
			{
				e.printStackTrace();
			}
			return null;
		}
		
		private Array.ISort.Primitives getK = null;
		
		public void build( Array.ISort.Primitives.Index dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = index -> (char) keys[index] : getK;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != 0 ) dst.dst[k++] = (char) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build( Array.ISort.Primitives.Index2 dst ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = index -> (char) keys[index] : getK;
			
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != 0 ) dst.dst[k++] = i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			json.enterObject();
			
			if( hasNullKey ) json.name().value();
			
			int i = keys.length;
			if( 0 < assigned )
			{
				json.preallocate( assigned * 10 );
				char v;
				while( -1 < --i )
					if( (v = keys[i]) != 0 ) json.name( v ).value();
			}
			
			json.exitObject();
		}
	}
	
	interface Interface {
		int size();
		
		boolean contains(  Character key );
		
		boolean contains( char key );
		
		boolean add(  Character key );
		
		boolean add( char  key );
	}
	
	class RW extends R implements Interface {
		
		public RW( int expectedItems ) { this( expectedItems, 0.75 ); }
		
		public RW( double loadFactor ) { this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D ); }
		
		
		public RW( int expectedItems, double loadFactor ) {
			this( loadFactor );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys = new char[size];
		}
		
		public RW( char... items ) {
			this( items.length );
			for( char key : items ) add( key );
		}
		
		public RW(  Character... items ) {
			this( items.length );
			for(  Character key : items ) add( key );
		}
		
		public boolean add(  Character key ) { return key == null ? !hasNullKey && (hasNullKey = true) : add( (char) key ); }
		
		public boolean add( char  key ) {
			
			if( key == 0 ) return !has0Key && (has0Key = true);
			
			int slot = Array.hash( key ) & mask;
			
			for( char k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key ) return false;
			
			keys[slot] =  key;
			
			if( assigned++ == resizeAt ) allocate( mask + 1 << 1 );
			return true;
		}
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if( assigned < 1 )
			{
				if( keys.length < size ) keys = new char[size];
				return;
			}
			
			final char[] k = keys;
			
			keys = new char[size];
			
			char key;
			for( int i = k.length; -1 < --i; )
				if( (key = k[i]) != 0 )
				{
					int slot = Array.hash( key ) & mask;
					while( keys[slot] != 0 ) slot = slot + 1 & mask;
					keys[slot] = key;
				}
		}
		
		
		public boolean remove(  Character key ) { return key == null ? hasNullKey && !(hasNullKey = false) : remove( (char) key ); }
		
		public boolean remove( char key ) {
			
			if( key == 0 ) return has0Key && !(has0Key = false);
			
			int slot = Array.hash( key ) & mask;
			
			for( char k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key )
				{
					int gapSlot = slot;
					
					char kk;
					for( int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != 0; )
						if( (s - Array.hash( kk ) & mask) >= distance )
						{
							keys[gapSlot] =  kk;
							                           gapSlot = s;
							                           distance = 0;
						}
					
					keys[gapSlot] = 0;
					assigned--;
					return true;
				}
			return false;
		}
		
		public RW clear() {
			assigned   = 0;
			has0Key    = false;
			hasNullKey = false;
			return this;
		}
		
		
		public RW retainAll( RW chk ) {
			char key;
			
			for( int i = keys.length; -1 < --i; )
				if( (key = keys[i]) != 0 && !chk.add( key ) ) remove( key );
			
			if( has0Key && !chk.add( (char) 0 ) ) has0Key = false;
			return this;
		}
		
		public int removeAll( R src ) {
			int fix = size();
			
			for( int i = 0, s = src.size(), token = NonNullKeysIterator.INIT; i < s; i++ ) remove( NonNullKeysIterator.key( src, token = NonNullKeysIterator.token( src, token ) ) );
			
			return fix - size();
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}