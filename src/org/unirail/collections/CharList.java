package org.unirail.collections;


import java.util.Arrays;

public interface CharList {
	
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
	
	
	class R implements Comparable<R> {
		
		R( int length ) {
			if (0 < length) array = new char[length];
		}
		
		public static R of( char... values ) {
			R dst = new R( values.length );
			fill( dst, values );
			return dst;
		}
		
		
		static void fill( R dst, char... items ) {
			dst.array = new char[dst.size = items.length];
			
			for (int i = 0; i < dst.size; i++)
			     dst.array[i] = (char) items[i];
		}
		
		char[] array;
		
		public int length() { return array == null ? 0 : array.length; }
		
		int size = 0;
		
		public int size()                            { return size; }
		
		public boolean isEmpty()                     { return size == 0; }
		
		public boolean contains( char value ) {return -1 < indexOf( value );}
		
		
		public char[] toArray( char[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new char[size];
			for (int i = 0; i < size; i++) dst[i] = (char) array[i];
			
			return dst;
		}
		
		public boolean containsAll( Producer src ) {
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
				if (!contains( src.value( tag ) )) return false;
			
			return true;
		}
		
		
		public char get( int index ) {return  (char) array[index]; }
		
		
		public int indexOf( char value ) {
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf( char value ) {
			for (int i = size - 1; -1 < i; i--)
				if (array[i] == (char) value) return i;
			return -1;
		}
		
		public R subList( int fromIndex, int toIndex, R dst ) {
			if (size <= fromIndex) return null;
			if (size - 1 < toIndex) toIndex = size - 1;
			if (toIndex == fromIndex) return null;
			
			if (dst == null) dst = new R( toIndex - fromIndex );
			else if (dst.length() < toIndex - fromIndex) dst.array = new char[toIndex - fromIndex];
			
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
				
				public char value( int tag ) {return (char) array[tag]; }
				
			} : producer;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
		
		
	}
	
	class Rsize extends R {
		
		public Rsize( int items ) {
			super( items );
			size = items;
		}
		
		public static Rsize of( char... values ) {
			Rsize dst = new Rsize( values.length );
			fill( dst, values );
			return dst;
		}
		
		public void set( char value ) { set( size, value );}
		
		public void set( int index, char value ) {
			if (size <= index) return;
			
			array[index] = (char) value;
		}
		
		
		public void set( int index, char... values ) {
			int max = Math.min( values.length, size - index );
			
			
			for (int i = 0; i < max; i++)
			     array[index + i] = (char) values[i];
		}
	}
	
	class RW extends Rsize implements Array, Consumer {
		
		
		public RW( int items ) { super( items ); size = 0; }
		
		public static RW of( char... values ) {
			RW dst = new RW( values.length );
			fill( dst, values );
			return dst;
		}
		
		public Consumer consumer()               {return this; }
		
		public char[] array()             {return array;}
		
		public char[] length( int items ) { return array = items == 0 ? null : new char[items];}
		
		public void fit()                        {if (0 < length() && size < length()) array = Arrays.copyOf( array, size ); }
		
		public boolean add( char value ) {
			size            = Array.resize( this, size, size, 1, false );
			array[size - 1] = (char) value;
			return true;
		}
		
		public void add( int index, char value ) {
			if (index < size)
			{
				size         = Array.resize( this, size, index, 1, false );
				array[index] = (char) value;
			}
			else set( index, value );
			
		}
		
		public void remove() { remove( size - 1 );}
		
		public void remove( int index ) {
			if (size < 1 || size <= index) return;
			size = Array.resize( this, size, index, -1, false );
		}
		
		
		public void set( int index, char... values ) {
			int len = values.length;
			
			if (size <= index + len)
			{
				int    fix = size;
				Object obj = array;
				
				size = Array.resize( this, size, index, len, false );
				if (obj == array) Arrays.fill( array, fix, size - 1, (char) 0 );
			}
			
			for (int i = 0; i < len; i++)
			     array[index + i] = (char) values[i];
		}
		
		public void set( char value ) { set( size, value );}
		
		public void set( int index, char value ) {
			if (size <= index)
			{
				int    fix = size;
				Object obj = array;
				
				size = Array.resize( this, size, index, 1, false );
				if (obj == array) Arrays.fill( array, fix, size - 1, (char) 0 );
			}
			
			array[index] = (char) value;
		}
		
		public void swap( int index1, int index2 ) {
			final char tmp = array[index1];
			array[index1] = array[index2];
			array[index2] = tmp;
			
		}
		
		public void addAll( Producer src ) {
			for (int tag = src.tag(), i = size; src.ok( tag ); tag = src.tag( tag )) array[i++] = (char) src.value( tag );
		}
		
		public boolean addAll( Producer src, int index ) {
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) array[index++] = (char) src.value( tag );
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
		
		
		public RW clone()   { return (RW) super.clone(); }
		
	}
}
