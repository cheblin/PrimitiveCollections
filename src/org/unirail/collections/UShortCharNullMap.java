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

public interface UShortCharNullMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token( R src, int token ) {
			final int len = src.keys.length;
			for( token++; ; token++ )
				if( token == len ) return src.has0Key == Positive_Values.NONE ? INIT : len;
				else if( token == len + 1 ) return INIT;
				else if( src.keys[token] != 0 ) return token;
		}
		
		static char key( R src, int token ) { return token == src.keys.length ? 0 : src.keys[token]; }
		
		static boolean hasValue( R src, int token ) { return token == src.keys.length ? src.has0Key == Positive_Values.VALUE : src.values.hasValue( token ); }
		
		static char value( R src, int token ) { return token == src.keys.length ? src.OKeyValue :    src.values.get( token ); }
	}
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		
		public char[]          keys   = Array.EqualHashOf.chars     .O;
		public CharNullList.RW values = new CharNullList.RW( 0 );
		
		int assigned;
		int mask;
		int resizeAt;
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		char NullKeyValue = 0;
		
		
		@Positive_Values int has0Key = Positive_Values.NONE;
		char       OKeyValue;
		
		
		protected double loadFactor;
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + (has0Key == Positive_Values.NONE ? 0 : 1) + (hasNullKey == Positive_Values.NONE ? 0 : 1); }
		
		
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
				h = Array.mix( h, Array.hash( NonNullKeysIterator.hasValue( this, token ) ? NonNullKeysIterator.value( this, token ) : seed ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			if( hasNullKey != Positive_Values.NONE )
			{
				int h = Array.hash( seed );
				h = Array.mix( h, Array.hash( hasNullKey == Positive_Values.VALUE ? Array.hash( NullKeyValue ) : seed ) );
				h = Array.finalizeHash( h, 2 );
				
				a += h;
				b ^= h;
				c *= h | 1;
			}
			
			return Array.finalizeHash( Array.mixLast( Array.mix( Array.mix( seed, a ), b ), c ), size() );
		}
		
		private static final int seed = R.class.hashCode();
		
		public boolean contains(  Character key )           { return !hasNone( token( key ) ); }
		
		public boolean contains( char key )               { return !hasNone( token( key ) ); }
		
		public @Positive_Values int token(  Character key ) { return key == null ? hasNullKey == Positive_Values.VALUE ? keys.length + 1 : hasNullKey : token( key. charValue      () ); }
		
		public @Positive_Values int token( char key ) {
			
			if( key == 0 ) return has0Key == Positive_Values.VALUE ? keys.length : has0Key;
			
			final char key_ =  key;
			
			int slot = Array.hash( key ) & mask;
			
			for( char k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key_ ) return (values.hasValue( slot )) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;//the key is not present
		}
		
		public boolean hasValue( int token )     { return -1 < token; }
		
		public boolean hasNone( int token )      { return token == Positive_Values.NONE; }
		
		public boolean hasNull( int token )      { return token == Positive_Values.NULL; }
		
		public char value( @Positive_ONLY int token ) { return token == keys.length + 1 ? NullKeyValue : token == keys.length ? OKeyValue : values.get( token ); }
		
		
		public @Positive_Values int hasNullKey() { return hasNullKey; }
		
		public char nullKeyValue() { return NullKeyValue; }
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			if( other == null || hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && NullKeyValue != other.NullKeyValue
			    || has0Key != other.has0Key || has0Key == Positive_Values.VALUE && OKeyValue != other.OKeyValue || size() != other.size() ) return false;
			
			
			char           key;
			for( int i = keys.length; -1 < --i; )
				if( (key =  keys[i]) != 0 )
					if( values.nulls.get( i ) )
					{
						int ii = other.token( key );
						if( ii < 0 || values.get( i ) != other.value( ii ) ) return false;
					}
					else if( -1 < other.token( key ) ) return false;
			
			return true;
		}
		
		
		public R clone() {
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
			          getV == null ? getV = index -> values.hasValue( index ) ?  value( index ) : 0 : getV;
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != 0 ) dst.dst[k++] = (char) i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		public void build( Array.ISort.Primitives.Index2 dst, boolean K ) {
			if( dst.dst == null || dst.dst.length < assigned ) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = K ?
			          getK == null ? getK = index ->  keys[index] : getK :
			          getV == null ? getV = index -> values.hasValue( index ) ?  value( index ) : 0 : getV;
			
			
			for( int i = 0, k = 0; i < keys.length; i++ ) if( keys[i] != 0 ) dst.dst[k++] = i;
			Array.ISort.sort( dst, 0, assigned - 1 );
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			json.enterObject();
			
			switch( hasNullKey )
			{
				case Positive_Values.VALUE:
					json.name().value( NullKeyValue );
					break;
				case Positive_Values.NULL:
					json.name().value();
			}
			
			int size = size(), token = NonNullKeysIterator.INIT;
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				
				while( (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT )
				{
					json.name( NonNullKeysIterator.key( this, token ) );
					if( NonNullKeysIterator.hasValue( this, token ) ) json.value( NonNullKeysIterator.value( this, token ) );
					else json.value();
				}
			}
			
			json.exitObject();
		}
	}
	
	interface Interface {
		int size();
		
		boolean contains(  Character key );
		
		boolean contains( char key );
		
		@Positive_Values int token(  Character key );
		
		@Positive_Values int token( char key );
		
		boolean hasValue( int token );
		
		boolean hasNone( int token );
		
		boolean hasNull( int token );
		
		char value( @Positive_ONLY int token );
		
		@Positive_Values int hasNullKey();
		
		char nullKeyValue();
		
		boolean put(  Character key, char value );
		
		boolean put(  Character key,  Character value );
		
		boolean put( char key,  Character value );
		
		boolean put( char key, char value );
	}
	
	class RW extends R implements Interface {
		
		
		public RW( int expectedItems ) { this( expectedItems, 0.75 ); }
		
		
		public RW( int expectedItems, double loadFactor ) {
			this.loadFactor = Math.min( Math.max( loadFactor, 1 / 100.0D ), 99 / 100.0D );
			
			final long length = (long) Math.ceil( expectedItems / loadFactor );
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max( 4, Array.nextPowerOf2( length ) ));
			
			resizeAt = Math.min( mask = size - 1, (int) Math.ceil( size * loadFactor ) );
			
			keys = new char[size];
			values.length( size );
		}
		
		
		public boolean put(  Character key, char value ) {
			if( key != null ) return put( key. charValue      (), value );
			
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			NullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put(  Character key,  Character value ) {
			if( key != null ) return put( key. charValue      (), value );
			
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
		
		public boolean put( char key,  Character value ) {
			if( value != null ) return put( key, (char) value );
			
			if( key == 0 )
			{
				int h = has0Key;
				has0Key = Positive_Values.NULL;
				return h == Positive_Values.NONE;
			}
			
			int               slot = Array.hash( key ) & mask;
			final char key_ =  key ;
			
			for( char k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key_ )
				{
					values.set1( slot, ( Character) null );
					return false;
				}
			
			keys[slot] = key_;
			values.set1( slot, ( Character) null );
			
			if( ++assigned == resizeAt ) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean put( char key, char value ) {
			if( key == 0 )
			{
				int h = has0Key;
				has0Key   = Positive_Values.VALUE;
				OKeyValue = value;
				return h == Positive_Values.NONE;
			}
			
			int slot = Array.hash( key ) & mask;
			
			final char key_ =  key;
			
			for( char k; (k = keys[slot]) != 0; slot = slot + 1 & mask )
				if( k == key_ )
				{
					values.set1( slot, value );
					return true;
				}
			
			keys[slot] = key_;
			values.set1( slot, value );
			
			if( ++assigned == resizeAt ) allocate( mask + 1 << 1 );
			
			return true;
		}
		
		
		public boolean remove() { return has0Key != Positive_Values.NONE && (has0Key = Positive_Values.NONE) == Positive_Values.NONE; }
		
		public boolean remove( char key ) {
			if( key == 0 ) return remove();
			
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
							
							keys[gapSlot] = kk;
							
							if( values.nulls.get( s ) )
								values.set1( gapSlot, values.get( s ) );
							else
								values.set1( gapSlot, ( Character) null );
							
							gapSlot  = s;
							distance = 0;
						}
					
					keys[gapSlot] = 0;
					values.set1( gapSlot, ( Character) null );
					assigned--;
					return true;
				}
			return false;
		}
		
		void allocate( int size ) {
			
			if( assigned < 1 )
			{
				resizeAt = Math.min( size - 1, (int) Math.ceil( size * loadFactor ) );
				mask     = size - 1;
				
				if( keys.length < size ) keys = new char[size];
				if( values.nulls.length() < size || values.values.values.length < size ) values.length( size );
				
				return;
			}
			
			RW tmp = new RW( size - 1, loadFactor );
			
			char[] k = keys;
			char   key;
			
			for( int i = k.length; -1 < --i; )
				if( (key = k[i]) != 0 )
					if( values.nulls.get( i ) ) tmp.put( key, values.get( i ) );
					else tmp.put( key, null );
			
			keys   = tmp.keys;
			values = tmp.values;
			
			assigned = tmp.assigned;
			mask     = tmp.mask;
			resizeAt = tmp.resizeAt;
		}
		
		public RW clear() {
			assigned = 0;
			has0Key  = Positive_Values.NONE;
			values.clear();
			return this;
		}
		
		public RW clone() { return (RW) super.clone(); }
	}
}
