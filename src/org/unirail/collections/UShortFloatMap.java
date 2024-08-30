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


public interface UShortFloatMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token( R src, int token ) {
			for( int len = src.keys.length; ; )
				if( ++token == len ) return src.has0Key ? len : INIT;
				else if( token == len + 1 ) return INIT;
				else if( src.keys[token] != 0 ) return token;
		}
		
		static char key( R src, int token ) { return token == src.keys.length ? 0 :   src.keys[token]; }
		
		static float value( R src, int token ) { return token == src.keys.length ? src.OKeyValue :   src.values[token]; }
	}
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		char[] keys   = Array.EqualHashOf.chars     .O;
		float[] values = Array.EqualHashOf.floats     .O;
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNullKey;
		float       NullKeyValue;
		
		boolean has0Key;
		float       OKeyValue;
		
		protected double loadFactor;
		
		
		public boolean isEmpty()                                 { return size() == 0; }
		
		public int size()                                        { return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0); }
		
		public boolean contains(  Character key )           { return !hasNone( token( key ) ); }
		
		public boolean contains( char key )               { return !hasNone( token( key ) ); }
		
		public @Positive_Values int token(  Character key ) { return key == null ? hasNullKey ? keys.length + 1 : Positive_Values.NONE : token( key. charValue      () ); }
		
		public @Positive_Values int token( char key ) {
			if( key == 0 ) return has0Key ? keys.length : Positive_Values.NONE;
			
			int slot = Array.hash( key ) & mask;
			
			for( char key_ =  key , k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key_ ) return slot;
			
			return Positive_Values.NONE;
		}
		
		
		public boolean hasValue( int token ) { return -1 < token; }
		
		public boolean hasNone( int token )  { return token == Positive_Values.NONE; }
		
		public float value( @Positive_ONLY int token ) {
			if( token == keys.length + 1 ) return NullKeyValue;
			if( token == keys.length ) return OKeyValue;
			return  values[token];
		}
		
		
		//Compute a hash that is symmetric in its arguments - that is a hash
		//where the order of appearance of elements does not matter.
		//This is useful for hashing sets, for example.
		
		public int hashCode() {
			int a = 0;
			int b = 0;
			int c = 1;
			
			for( int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
			{
				int h = Array.mix( seed, Array.hash( NonNullKeysIterator.key( this, token ) ) );
				h = Array.mix( h, Array.hash( NonNullKeysIterator.value( this, token ) ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey )
			{
				final int h = Array.finalizeHash( Array.mix( Array.hash( seed ), Array.hash( NullKeyValue ) ), 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			if( other == null ||
			    has0Key != other.has0Key ||
			    hasNullKey != other.hasNullKey || has0Key && OKeyValue != other.OKeyValue
			    || hasNullKey && NullKeyValue != other.NullKeyValue || size() != other.size() ) return false;
			
			char           key;
			for( int i = keys.length, c; -1 < --i; )
				if( (key =  keys[i]) != 0 )
				{
					if( (c = other.token( key )) < 0 ) return false;
					if( other.value( c ) !=   values[i] ) return false;
				}
			
			return true;
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
		
		private Array.ISort.Primitives getK = null;
		private Array.ISort.Primitives getV = null;
		
		public void build( Array.ISort.Primitives.Index dst, boolean K ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = K ?
			          getK == null ? getK = index ->  keys[index] : getK :
			          getV == null ? getV = (Array.ISort.Primitives.floats) index  ->  values[index] : getV;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != 0 ) dst.dst[k++] = (char) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build( Array.ISort.Primitives.Index2 dst, boolean K ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = K ?
			          getK == null ? getK = index ->  keys[index] : getK :
			          getV == null ? getV = (Array.ISort.Primitives.floats) index  ->  values[index] : getV;
			
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != 0 ) dst.dst[k++] = i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			json.enterObject();
			
			int size = size(), token = NonNullKeysIterator.INIT;
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				while( (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT )
					json.
							name( NonNullKeysIterator.key( this, token ) ).
							value( NonNullKeysIterator.value( this, token ) );
			}
			
			json.exitObject();
		}
	}
	
	interface Interface {
		int size();
		
		boolean contains(  Character key );
		
		boolean contains( char key );
		
		boolean hasValue( int token );
		
		boolean hasNone( int token );
		
		@Positive_Values int token(  Character key );
		
		@Positive_Values int token( char key );
		
		boolean put(  Character key, float value );
		
		boolean put( char key, float value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int expectedItems ) { this( expectedItems, 0.75 ); }
		
		
		public RW( int expectedItems, double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
			mask     = size - 1;
			
			keys   = new char[size];
			values = new float[size];
		}
		
		
		public boolean put(  Character key, float value ) {
			if( key != null ) return put( (char) key, value );
			
			hasNullKey   = true;
			NullKeyValue = value;
			
			return true;
		}
		
		public boolean put( char key, float value ) {
			
			if( key == 0 )
			{
				if( has0Key )
				{
					OKeyValue = value;
					return false;
				}
				has0Key   = true;
				OKeyValue = value;
				return true;
			}
			
			
			int slot = Array.hash( key ) & mask;
			
			final char key_ =  key;
			
			for( char k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key_ )
				{
					values[slot] = (float) value;
					return true;
				}
			
			keys[slot]   = key_;
			values[slot] = (float) value;
			
			if( ++assigned == resizeAt ) allocate( mask + 1 << 1 );
			
			return false;
		}
		
		
		public boolean remove(  Character key ) {
			if( key == null ) return hasNullKey && !(hasNullKey = false);
			return remove( (char) key );
		}
		
		public boolean remove( char key ) {
			
			if( key == 0 ) return has0Key && !(has0Key = false);
			
			int slot = Array.hash( key ) & mask;
			
			final char key_ =  key;
			
			for( char k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key_ )
				{
					int gapSlot = slot;
					
					char kk;
					
					for( int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != 0; )
						if( (s - Array.hash( kk ) & mask) >= distance )
						{
							
							keys[gapSlot]   = kk;
							values[gapSlot] = values[s];
							                  gapSlot = s;
							                  distance = 0;
						}
					
					keys[gapSlot]   = 0;
					values[gapSlot] = 0;
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
		
		
		protected void allocate( int size ) {
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			if( assigned < 1 )
			{
				if( keys.length < size ) keys = new char[size];
				if( values.length < size ) values = new float[size];
				return;
			}
			
			final char[] k = keys;
			final float[] v = values;
			
			keys   = new char[size];
			values = new float[size];
			
			char key;
			for( int i = k.length; -1 < --i; )
				if( (key = k[i]) != 0 )
				{
					int slot = Array.hash( key ) & mask;
					while( keys[slot] != 0 ) slot = slot + 1 & mask;
					keys[slot]   = key;
					values[slot] = v[i];
				}
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
