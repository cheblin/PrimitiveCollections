package org.unirail.collections;


import java.util.Arrays;

public interface UByteList {
	
	interface Consumer {
		boolean add( char value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return -1 < tag;}
		
		char  value( int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( value( tag ) ).append( '\n' );
			return dst;
		}
	}
	
	
	class R implements Array, Comparable<R> {
		
		public byte[] array;
		
		public Object array()              {return array;}
		
		public Object allocate( int size ) { return array = size == 0 ? null : new byte[size];}
		
		public void fit()                  {if (0 < length() && size < length()) array = Arrays.copyOf( array, size ); }
		
		public int length()                { return array == null ? 0 : array.length; }
		
		public R( int length ) {
			if (0 < length) allocate( length );
		}
		
		public R( char... items ) {
			this( items.length );
			size = items.length;
			
			for (int i = 0; i < size; i++)
			     array[i] = (byte)items[i];
		}
		
		int size = 0;
		
		public int size() { return size; }
		
		public int resize(int size, int index, int resize, boolean fit ) {
			final Object src        = array();
			final int    fix_length = length();
			final int    fix_size   = size;
			
			this.size = Array.super.resize( size, index, resize, fit );
			
			if (fix_length < 1) return size;
			
			if (0 < resize &&
			    0 < fix_size &&
			    index < fix_size && src == array())
				Arrays.fill( array, index, resize, (byte) 0 );
			
			return size;
		}
		
		public boolean isEmpty()                     { return size == 0; }
		
		public boolean contains( char value ) {return -1 < indexOf( value );}
		
		
		public char[] toArray( char[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new char[size];
			for (int i = 0; i < size; i++) dst[i] = (char)( 0xFFFF &  array[i]);
			
			return dst;
		}
		
		public boolean containsAll( Producer src ) {
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
				if (!contains( src.value( tag ) )) return false;
			
			return true;
		}
		
		
		public char get( int index ) {return  (char)( 0xFFFF &  array[index]); }
		
		
		public int indexOf( char value ) {
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf( char value ) {
			for (int i = size - 1; -1 < i; i--)
				if (array[i] == (byte)value) return i;
			return -1;
		}
		
		public R subList( int fromIndex, int toIndex, R dst ) {
			if (size <= fromIndex) return null;
			if (size - 1 < toIndex) toIndex = size - 1;
			if (toIndex == fromIndex) return null;
			
			if (dst == null) dst = new R( toIndex - fromIndex );
			if (dst.length() < toIndex - fromIndex) dst.allocate( toIndex - fromIndex );
			
			System.arraycopy( array, fromIndex, dst.array, 0, toIndex - fromIndex );
			return dst;
		}
		
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R other ) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			for (int i = 0; i < size; i++)
				if (array[i] != other.array[i]) return 1;
			return 0;
		}
		
		
		public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				
				dst.array = array.clone();
				dst.size  = size;
				
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
			
		}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				
				public int tag() { return 0 < size ? 0 : -1; }
				
				public int tag( int tag ) { return ++tag < size ? tag : -1; }
				
				public char value( int tag ) {return (char)( 0xFFFF &  array[tag]); }
				
			} : producer;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
		
	}
	
	class RW extends R implements Consumer {
		
		public Consumer consumer() {return this; }
		
		public RW( int length ) {
			super( length );
		}
		
		public RW( char... items ) {
			super( items );
		}
		
		public void swap( int item1, int item2 ) {
			final byte tmp = array[item1];
			array[item1] = array[item2];
			array[item2] = tmp;
			
		}
		
		public boolean add( char value ) {
			resize(size, size, 1, false );
			array[size - 1] = (byte)value;
			return true;
		}
		
		public boolean remove() { return 0 < size && remove( size - 1 );}
		
		public boolean remove( int index ) {
			if (size < 1 || !(index < size)) return false;
			
			resize(size, index, -1, false );
			return true;
		}
		
		
		public void addAll( Producer src ) {
			for (int tag = src.tag(), i = size; src.ok( tag ); tag = src.tag( tag )) array[i++] = (byte) src.value( tag );
		}
		
		public boolean addAll( Producer src, int index ) {
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) array[index++] = (byte) src.value( tag );
			return true;
		}
		
		
		public int removeAll( Producer src ) {
			int fix = size;
			for (int tag = src.tag(), i; src.ok( tag ); tag = src.tag( tag ))
				if (-1 < (i = indexOf( src.value( tag ) ))) remove( i );
			return fix - size;
		}
		
		public boolean retainAll( Consumer chk ) {
			
			final int   fix = size;
			char v;
			for (int index = 0; index < size; index++)
				if (!chk.add( v = get( index ) ))
					remove( indexOf( v ) );
			
			return fix != size;
		}
		
		public void clear() { size = 0;}
		
		
		public boolean set( int index, char value ) {
			
			boolean resize = !(index < size);
			if (resize) resize(size, index = size, 1, false );
			
			array[index] = (byte)value;
			
			return resize;
		}
		
		public void add( int index, char value ) {
			if (size < index) index = size;
			
			resize(size, index, 1, false );
			array[index] = (byte)value;
		}
		
		public RW clone() { return (RW) super.clone(); }
		
	}
}
