package org.unirail.collections;

import java.util.Arrays;

public interface ObjectList {
	
	interface Producer<V extends Comparable<? super V>> {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return -1 < tag;}
		
		V value( int tag );
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( value( tag ) ).append( '\n' );
			
			return dst;
		}
	}
	
	interface Consumer<V extends Comparable<? super V>> {
		boolean add( V value );
	}
	
	class R<V extends Comparable<? super V>> implements Array, Comparable<R<V>> {
		
		
		V[] array;
		
		public V[] array() {return array;}
		
		@SuppressWarnings("unchecked")
		public V[] length( int items ) { return array = (V[]) new Comparable[items]; }
		
		public void fit()   {if (0 < length() && size < length()) array = Arrays.copyOf( array, size ); }
		
		public int length() { return array == null ? 0 : array.length; }
		
		
		public R( int length ) {
			if (0 < length) length( length );
		}
		
		
		int size = 0;
		
		public int size()                  { return size; }
		
		public boolean isEmpty()           { return size == 0; }
		
		public boolean contains( V value ) { return -1 < indexOf( value ); }
		
		
		private Producer<V> producer;
		
		public Producer<V> producer() {
			return producer == null ? producer = new Producer<>() {
				
				public int tag() { return 0 < size ? 0 : -1; }
				
				public int tag( int tag ) { return ++tag < size ? tag : -1; }
				
				public V value( int tag ) {return array[tag]; }
				
			} : producer;
		}
		
		
		public V[] toArray( V[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < size()) return Arrays.copyOfRange( array, 0, size );
			System.arraycopy( array, 0, dst, 0, size );
			return dst;
		}
		
		
		public boolean containsAll( Producer<V> src ) {
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
				if (!contains( src.value( tag ) )) return false;
			return true;
		}
		
		
		public V get( int index ) {return array[index]; }
		
		
		public int indexOf( V value ) {
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf( V value ) {
			for (int i = size - 1; -1 < i; i--)
				if (array[i] == value) return i;
			return -1;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
		public int compareTo( R<V> other ) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			for (int i = 0, diff; i < size; i++)
				if (array[i] != null)
					if (other.array[i] == null)
					{
						if (other.array[i] != null) return -1;
					}
					else if ((diff = array[i].compareTo( other.array[i] )) != 0) return diff;
			
			return 0;
		}
		
		
		@SuppressWarnings("unchecked")
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
				dst.array = array.clone();
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			
			return null;
		}
		
		
		public StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW<V extends Comparable<? super V>> extends R<V> implements Consumer<V> {
		public RW( int length ) {
			super( length );
		}
		
		
		public boolean add( V value ) {
			size            = Array.resize( this, size, size, 1, false );
			array[size - 1] = value;
			return true;
		}
		
		public void add( int index, V value ) {
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
		
		public void set( int index, V value ) {
			if (size <= index)
			{
				int    fix = size;
				Object obj = array;
				
				size = Array.resize( this, size, index, 1, false );
				if (obj == array) Arrays.fill( array, fix, size - 1, null );
			}
			
			array[index] = value;
		}
		
		public boolean addAll( Producer<V> src, int count ) {
			int i = size;
			size = Array.resize( this, size, size, count, false );
			
			for (int tag = src.tag(); src.ok( tag ) && i < size; tag = src.tag( tag ))
			     array[i++] = src.value( tag );
			
			return true;
		}
		
		public boolean addAll( Producer<V> src, int index, int count ) {
			
			size = Array.resize( this, size, index, count, false );
			for (int tag = src.tag(); src.ok( tag ) && index < size; tag = src.tag( tag ))
			     array[index++] = src.value( tag );
			return true;
		}
		
		public boolean removeAll( Producer<V> src ) {
			final int s = size;
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
				for (int i = size - 1; i >= 0; i--) if (array[i] == src.value( tag )) size = Array.resize( this, size, i, -1, false );
			return size != s;
		}
		
		public boolean retainAll( Consumer<V> chk ) {
			
			final int s = size;
			
			for (int i = 0, max = size; i < max; i++)
				if (!chk.add( array[i] ))
				{
					final V val = array[i];
					for (int j = size; j > 0; j--) if (array[j] == val) size = Array.resize( this, size, j, -1, false );
					max = size;
				}
			
			return s != size;
		}
		
		public void clear() { Arrays.fill( array, null ); size = 0;}
		
		public RW<V> clone()          { return (RW<V>) super.clone(); }
		
		public Consumer<V> consumer() {return this; }
	}
}
	