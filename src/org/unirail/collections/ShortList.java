package org.unirail.collections;


import java.util.Arrays;

public interface ShortList {
	
	interface Consumer {
		boolean add( short value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return -1 < tag;}
		
		short  value( int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( value( tag ) ).append( '\n' );
			return dst;
		}
	}
	
	
	class R implements Comparable<R> {
		
		R( int length ) {
			if (0 < length) array = new short[length];
		}
		
		public static R of( short... values ) {
			R dst = new R( 0 );
			fill( dst, values );
			return dst;
		}
		
		
		static void fill( R dst, short... items ) {
			dst.array = new short[dst.size = items.length];
			
			for (int i = 0; i < dst.size; i++)
			     dst.array[i] = (short) items[i];
		}
		
		short[] array;
		
		public int length() { return array == null ? 0 : array.length; }
		
		int size = 0;
		
		public int size()                            { return size; }
		
		public boolean isEmpty()                     { return size == 0; }
		
		public boolean contains( short value ) {return -1 < indexOf( value );}
		
		
		public short[] toArray( short[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new short[size];
			for (int i = 0; i < size; i++) dst[i] = (short) array[i];
			
			return dst;
		}
		
		public boolean containsAll( Producer src ) {
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
				if (!contains( src.value( tag ) )) return false;
			
			return true;
		}
		
		
		public short get( int index ) {return  (short) array[index]; }
		
		
		public int indexOf( short value ) {
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf( short value ) {
			for (int i = size - 1; -1 < i; i--)
				if (array[i] == value) return i;
			return -1;
		}
		
		public R subList( int fromIndex, int toIndex, R dst ) {
			if (size <= fromIndex) return null;
			if (size - 1 < toIndex) toIndex = size - 1;
			if (toIndex == fromIndex) return null;
			
			if (dst == null) dst = new R( toIndex - fromIndex );
			else if (dst.length() < toIndex - fromIndex) dst.array = new short[toIndex - fromIndex];
			
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
				
				public short value( int tag ) {return (short) array[tag]; }
				
			} : producer;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
		
	}
	
	class RW extends R implements Array, Consumer {
		
		
		public RW( int length )    { super( length ); }
		
		public static RW of( short... values ) {
			RW dst = new RW( 0 );
			fill( dst, values );
			return dst;
		}
		
		public Consumer consumer() {return this; }
		
		public short[] array()             {return array;}
		
		public short[] length( int items ) { return array = items == 0 ? null : new short[items];}
		
		public void fit()                        {if (0 < length() && size < length()) array = Arrays.copyOf( array, size ); }
		
		public boolean add( short value ) {
			size            = Array.resize( this, size, size, 1, false );
			array[size - 1] = value;
			return true;
		}
		
		public void add( int index, short value ) {
			if (index < size)
			{
				size         = Array.resize( this, size, index, 1, false );
				array[index] = value;
			}
			else set( index, value );
			
		}
		
		public void remove() { remove( size - 1 );}
		
		public void remove( int index ) {
			if (size < 1 || size <= index) return;
			size = Array.resize( this, size, index, -1, false );
		}
		
		public void set( int index, short value ) {
			if (size <= index)
			{
				int    fix = size;
				Object obj = array;
				
				size = Array.resize( this, size, index, 1, false );
				if (obj == array) Arrays.fill( array, fix, size - 1, (short) 0 );
			}
			
			array[index] = value;
		}
		
		public void swap( int index1, int index2 ) {
			final short tmp = array[index1];
			array[index1] = array[index2];
			array[index2] = tmp;
			
		}
		
		public void addAll( Producer src ) {
			for (int tag = src.tag(), i = size; src.ok( tag ); tag = src.tag( tag )) array[i++] =  src.value( tag );
		}
		
		public boolean addAll( Producer src, int index ) {
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) array[index++] =  src.value( tag );
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
			short v;
			for (int index = 0; index < size; index++)
				if (!chk.add( v = get( index ) ))
					remove( indexOf( v ) );
			
			return fix != size;
		}
		
		public void clear() { size = 0;}
		
		
		public RW clone()   { return (RW) super.clone(); }
		
	}
}
