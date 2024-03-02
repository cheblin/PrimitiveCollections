package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

import java.util.Arrays;


public interface CharList {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		public final char default_value;
		protected R( char default_value ) { this.default_value = default_value; }
		
		char[] values = Array.EqualHashOf.chars     .O;
		
		public char[] array() { return values; }
		
		int size = 0;
		
		public int size()                            { return size; }
		
		public boolean isEmpty()                     { return size == 0; }
		
		public boolean contains( char value ) { return -1 < indexOf( value ); }
		
		
		public char[] toArray( int index, int len, char[] dst ) {
			if( size == 0 ) return null;
			if( dst == null ) dst = new char[len];
			for( int i = 0; i < len; i++ ) dst[index + i] = (char) values[i];
			
			return dst;
		}
		
		public boolean containsAll( R src ) {
			for( int i = src.size(); -1 < --i; )
				if( !contains( src.get( i ) ) ) return false;
			
			return true;
		}
		
		public char get( int index ) { return  (char) values[index]; }
		
		public int get(char[] dst, int dst_index, int src_index, int len ) {
			len = Math.min( Math.min( size - src_index, len ), dst.length - dst_index );
			if( len < 1 ) return 0;
			
			for( int i = 0; i < len; i++ )
			     dst[dst_index++] = (char) values[src_index++];
			
			return len;
		}
		
		public int indexOf( char value ) {
			for( int i = 0; i < size; i++ )
				if( values[i] == value ) return i;
			return -1;
		}
		
		public int lastIndexOf( char value ) {
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
		
		char get( int index );
		
		RW add1( char value );
		
		RW set1( int index, char value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length ) {
			super( (char) 0 );
			values = 0 < length ? new char[length] : Array.EqualHashOf.chars     .O;
		}
		
		public RW( char default_value, int size ) {
			super( default_value );
			
			values = 0 < size ? new char[size] : Array.EqualHashOf.chars     .O;
			
			this.size = size;
			if( default_value == 0 ) return;
			
			while( -1 < --size ) values[size] = (char) default_value;
		}
		
		public RW add1( char value ) { return add1( size, value ); }
		
		public RW add1( int index, char value ) {
			
			int max = Math.max( index, size + 1 );
			
			size          = Array.resize( values, values.length <= max ? values = new char[max + max / 2] : values, index, size, 1 );
			values[index] = (char) value;
			return this;
		}
		
		public RW add( char... src ) { return add( size(), src, 0, src.length ); }
		public RW add( int index, char[] src, int src_index, int len ) {
			int max = Math.max( index, size ) + len;
			
			size = Array.resize( values, values.length < max ? values = new char[max + max / 2] : values, index, size, len );
			
			for( int i = 0; i < len; i++ ) values[index + i] = (char) src[src_index + i];
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
		
		public RW set1( char value ) { return set1( size, value ); }
		
		public RW set1( int index, char value ) {
			
			if( size <= index )
			{
				if( values.length <= index ) values = Arrays.copyOf( values, index + index / 2 );
				
				if( default_value != 0 ) Arrays.fill( values, size, index, (char) default_value );
				
				size = index + 1;
			}
			
			values[index] = (char) value;
			return this;
		}
		
		public RW set( int index, char... src ) { return set( index, src, 0, src.length ); }
		
		
		public RW set( int index, char[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( index + i,  src[src_index + i] );
			return this;
		}
		
		public RW swap( int index1, int index2 ) {
			final char tmp = values[index1];
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
		
		public int removeAll( char src ) {
			int fix = size;
			
			for( int k; -1 < (k = indexOf( src )); ) remove( k );
			return fix - size;
		}
		
		//remove with change order
		public int removeAll_fast( char src ) {
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
			
			final int   fix = size;
			
			for( int index = 0; index < size; index++ )
				if( !chk.contains( get( index ) ) )
					remove( index );
			
			return fix != size;
		}
		
		public RW clear() {
			if( size < 1 ) return this;
			Arrays.fill( values, 0, size - 1, (char) default_value );
			size = 0;
			return this;
		}
		
		public RW fit() {
			length( size() );
			return this;
		}
		
		public RW length( int length ) {
			if( values.length != length )
				if( length < 1 )
				{
					values = Array.EqualHashOf.chars     .O;
					size   = 0;
				}
				else
				{
					Arrays.copyOf( values, length );
					if( length < size ) size = length;
				}
			
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
