package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

import java.util.Arrays;


public interface FloatList {
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		float[] values = Array.Of.floats     .O;
		
		public float[] array() { return values; }
		
		int size = 0;
		
		public int size()                            { return size; }
		
		public boolean isEmpty()                     { return size == 0; }
		
		public boolean contains( float value ) { return -1 < indexOf( value ); }
		
		
		public float[] toArray( int index, int len, float[] dst ) {
			if( size == 0 ) return null;
			if( dst == null ) dst = new float[len];
			for( int i = 0; i < len; i++ ) dst[index + i] =  values[i];
			
			return dst;
		}
		
		public boolean containsAll( R src ) {
			for( int i = src.size(); -1 < --i; )
				if( !contains( src.get( i ) ) ) return false;
			
			return true;
		}
		
		public float get( int index ) { return   values[index]; }
		
		public int get(float[] dst, int dst_index, int src_index, int len ) {
			len = Math.min( Math.min( size - src_index, len ), dst.length - dst_index );
			if( len < 1 ) return 0;
			
			for( int i = 0; i < len; i++ )
			     dst[dst_index++] =  values[src_index++];
			
			return len;
		}
		
		public int indexOf( float value ) {
			for( int i = 0; i < size; i++ )
				if( values[i] == value ) return i;
			return -1;
		}
		
		public int lastIndexOf( float value ) {
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
		
		float get( int index );
		
		void add( float value );
		
		void set( int index, float value );
	}
	
	class RW extends R implements Interface {
		
		public RW( int length ) { if( 0 < length ) values = new float[length]; }
		
		public RW( float... items ) {
			this( items == null ? 0 : items.length );
			if( items != null )
			{
				size = items.length;
				for( int i = 0; i < size; i++ )
				     values[i] = (float) items[i];
			}
		}
		
		public RW( float default_value, int size ) {
			this( size );
			this.size = size;
			if( default_value == 0 ) return;
			
			while( -1 < --size ) values[size] = (float) default_value;
		}
		
		public RW( R src, int fromIndex, int toIndex ) {
			this( toIndex - fromIndex );
			System.arraycopy( src.values, fromIndex, values, 0, toIndex - fromIndex );
		}
		
		
		public void add( float value ) { add( size, value ); }
		
		public void add( int index, float value ) {
			
			int max = Math.max( index, size + 1 );
			
			size          = Array.resize( values, values.length <= max ? values = new float[max + max / 2] : values, index, size, 1 );
			values[index] = (float) value;
		}
		
		public void add( int index, float[] src, int src_index, int len ) {
			int max = Math.max( index, size ) + len;
			
			size = Array.resize( values, values.length < max ? values = new float[max + max / 2] : values, index, size, len );
			
			for( int i = 0; i < len; i++ ) values[index + i] = (float) src[src_index + i];
		}
		
		public void remove() { remove( size - 1 ); }
		
		public void remove( int index ) {
			if( size < 1 || size < index ) return;
			if( index == size - 1 ) values[--size] = (float) 0;
			else size = Array.resize( values, values, index, size, -1 );
		}
		
		public void remove_fast( int index ) {
			if( size < 1 || size <= index ) return;
			values[index] = values[--size];
		}
		
		
		public void set( int index, float... src ) {
			int len = src.length;
			int max = index + len;
			
			if( size < max )
			{
				if( values.length < max ) Array.copy( values, index, len, size, values = new float[max + max / 2] );
				size = max;
			}
			
			for( int i = 0; i < len; i++ )
			     values[index + i] = (float) src[i];
		}
		
		public void set( float value ) { set( size, value ); }
		
		public void set( int index, float value ) {
			
			if( size <= index )
			{
				if( values.length <= index ) values = Arrays.copyOf( values, index + index / 2 );
				size = index + 1;
			}
			
			values[index] = (float) value;
		}
		
		public int set( float[] src, int src_index, int dst_index, int len ) {
			len = Math.min( src.length - src_index, len );
			if( len < 1 ) return 0;
			
			for( int i = 0; i < len; i++ )
			     set( dst_index++, src[src_index++] );
			
			return len;
		}
		
		
		public void swap( int index1, int index2 ) {
			final float tmp = values[index1];
			values[index1] = values[index2];
			values[index2] = tmp;
			
		}
		
		public int removeAll( R src ) {
			int fix = size;
			
			for( int i = 0, k, src_size = src.size(); i < src_size; i++ )
				if( -1 < (k = indexOf( src.get( i ) )) ) remove( k );
			return fix - size;
		}
		
		public int removeAll( float src ) {
			int fix = size;
			
			for( int k; -1 < (k = indexOf( src )); ) remove( k );
			return fix - size;
		}
		
		public int removeAll_fast( float src ) {
			int fix = size;
			
			for( int k; -1 < (k = indexOf( src )); ) remove_fast( k );
			return fix - size;
		}
		
		public boolean retainAll( R chk ) {
			
			final int   fix = size;
			float v;
			for( int index = 0; index < size; index++ )
				if( !chk.contains( v = get( index ) ) )
					remove( indexOf( v ) );
			
			return fix != size;
		}
		
		public void clear() {
			if( size < 1 ) return;
			Arrays.fill( values, 0, size - 1, (float) 0 );
			size = 0;
		}
		
		
		public RW clone() { return (RW) super.clone(); }
		
	}
}
