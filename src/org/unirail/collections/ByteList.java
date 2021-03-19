package org.unirail.collections;


import java.util.Arrays;

public interface ByteList {
	
	interface Consumer {
		boolean add( byte value );
	}
	
	interface Producer {
		int tag();
		
		int tag( int tag );
		
		byte  value( int tag );
	}
	
	
	class R implements Array,  Comparable<R> {
		
		public byte[] array;
		
		public Object array()              {return array;}
		
		public Object allocate( int size ) { return array = size == 0 ? null : new byte[size];}
		
		public int length()                { return array == null ? 0 : array.length; }
		
		public R( int length ) {
			if (0 < length) allocate( length );
		}
		
		public R( byte... items ) {
			this( items.length );
			size = items.length;
			
			for (int i = 0; i < size; i++)
			     array[i] =items[i];
		}
		
		int size = 0;
		
		public int size() { return size; }
		
		public int resize( int index, int resize, boolean fit ) {
			final Object src        = array();
			final int    fix_length = length();
			final int    fix_size   = size();
			
			size = Array.super.resize( index, resize, fit );
			
			if (fix_length < 1) return size;
			
			if (0 < resize &&
			    0 < fix_size &&
			    index < fix_size && src == array())
				Arrays.fill( array, index, resize, (byte) 0 );
			
			return size;
		}
		
		public boolean isEmpty() { return size == 0; }
		
		public boolean contains( byte value ) {
			
			for (int i = size - 1; -1 < i; i--) if (array[i] == value) return true;
			return false;
		}
		
		
		public byte[] toArray( byte[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new byte[size];
			for (int i = 0; i < size; i++) dst[i] =  array[i];
			
			return dst;
		}
		
		public boolean containsAll( Producer src ) {
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag ))
				if (!contains( src.value( tag ) )) return false;
			
			return true;
		}
		
		
		public byte get( int index ) {return   array[index]; }
		
		
		public int indexOf( byte value ) {
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf( byte value ) {
			for (int i = size - 1; -1 < i; i--)
				if (array[i] == value) return i;
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
				
				public int tag()          { return size - 1; }
				
				public int tag( int tag ) { return --tag; }
				
				public byte value( int tag ) {return  array[tag]; }
				
			} : producer;
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			for (int i = 0; i < size; dst.append( '\n' ), i++)
			     dst.append( get( i ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	
	}
	
	class RW extends R implements Consumer {
		
		public Consumer consumer() {return this; }
		
		public RW( int length ) {
			super( length );
		}
		
		public RW( byte... items ) {
			super( items );
		}
		
		public void swap( int item1, int item2 ) {
			final byte tmp = array[item1];
			array[item1] = array[item2];
			array[item2] = tmp;
			
		}
		
		public boolean add( byte value ) {
			resize( size, 1, false );
			array[size - 1] = value;
			return true;
		}
		
		public boolean remove() { return 0 < size && remove( size - 1 );}
		
		public boolean remove( int index ) {
			if (size < 1 || !(index < size)) return false;
			
			resize( index, -1, false );
			return true;
		}
		
		
		public void addAll( Producer src ) {
			for (int tag = src.tag(), i = size; tag != -1; tag = src.tag( tag )) array[i++] =  src.value( tag );
		}
		
		public boolean addAll( Producer src, int index ) {
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag )) array[index++] =  src.value( tag );
			return true;
		}
		
		
		public int removeAll( Producer src ) {
			int fix = size;
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag )) remove( src.value( tag ) );
			return fix - size;
		}
		
		public boolean retainAll( Consumer chk ) {
			
			final int fix = size;
			
			for (int index = 0, v; index < size; index++)
				if (!chk.add( v = get( index ) )) remove( v );
			
			return fix != size;
		}
		
		public void clear() { size = 0;}
		
		
		public boolean set( int index, byte value ) {
			
			boolean resize = !(index < size);
			if (resize) resize( index = size, 1, false );
			
			array[index] = value;
			
			return resize;
		}
		
		public void add( int index, byte value ) {
			if (size < index) index = size;
			
			resize( index, 1, false );
			array[index] = value;
		}
		
		public RW clone() { return (RW) super.clone(); }
		
	}
}
