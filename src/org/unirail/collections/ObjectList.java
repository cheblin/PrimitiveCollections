package org.unirail.collections;

import java.util.Arrays;

public interface ObjectList {
	
	interface Producer<V extends Comparable<? super V>> {
		int tag();
		
		int tag( int tag );
		
		default boolean ok( int tag ) {return tag != -1;}
		
		V value( int tag );
	}
	
	interface Consumer<V extends Comparable<? super V>> {
		boolean add( V value );
	}
	
	class R<V extends Comparable<? super V>> implements Array, Comparable<R<V>> {
		
		
		public V[] array;
		
		public Object array() {return array;}
		
		@SuppressWarnings("unchecked")
		@Override public Object allocate( int size ) {
			return array = (V[]) new Comparable[size];
		}
		
		public int length() { return array == null ? 0 : array.length; }
		
		
		public int resize( int index, int resize, boolean fit ) {
			final Object src        = array();
			final int    fix_length = length();
			final int    fix_size   = size();
			
			size = Array.super.resize( index, resize, fit );
			
			if (fix_length < 1) return size;
			
			if (0 < resize)
			{
				if (0 < fix_size && index < fix_size && src == array())
					Arrays.fill( array, index, resize, null );
			}
			else if (!fit) Arrays.fill( array, size, fix_size - size, null );
			return size;
		}
		
		
		public R( int length ) {
			if (0 < length) allocate( length );
		}
		
		public R( V... items ) {
			this( items.length );
			size = items.length;
			if (0 < size) System.arraycopy( items, 0, array, 0, size );
		}
		
		
		int size = 0;
		
		public int size()                  { return size; }
		
		public boolean isEmpty()           { return size == 0; }
		
		public boolean contains( V value ) { return -1 < indexOf( value ); }
		
		
		private Producer<V> producer;
		
		public Producer<V> producer() {
			return producer == null ? producer = new Producer<>() {
				
				public int tag() { return size - 1; }
				
				public int tag( int tag ) { return --tag; }
				
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
			
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag ))
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
		
		@SuppressWarnings("unchecked")
		R<V> shell() {
			try
			{
				return (R<V>) super.clone();
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			
			return null;
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
	
	class RW<V extends Comparable<? super V>> extends R<V> implements Consumer<V> {
		
		public boolean add( V value ) {
			resize( size, 1, false );
			array[size - 1] = value;
			return true;
		}
		
		public boolean remove( int index ) {
			if (!(index < size)) return false;
			resize( index, -1, false );
			return true;
		}
		
		public boolean addAll( Producer<V> src, int count ) {
			int i = size;
			resize( size, count, false );
			
			for (int tag = src.tag(); tag != -1 && i < size; tag = src.tag( tag ))
			     array[i++] = src.value( tag );
			
			return true;
		}
		
		public boolean addAll( Producer<V> src, int index, int count ) {
			
			resize( index, count, false );
			for (int tag = src.tag(); tag != -1 && index < size; tag = src.tag( tag ))
			     array[index++] = src.value( tag );
			return true;
		}
		
		public boolean removeAll( Producer<V> src ) {
			final int s = size;
			
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag ))
				for (int i = size - 1; i >= 0; i--) if (array[i] == src.value( tag )) resize( i, -1, false );
			return size != s;
		}
		
		public boolean retainAll( Consumer<V> chk ) {
			
			final int s = size;
			
			for (int i = 0, max = size; i < max; i++)
				if (!chk.add( array[i] ))
				{
					final V val = array[i];
					for (int j = size; j > 0; j--) if (array[j] == val) resize( j, -1, false );
					max = size;
				}
			
			return s != size;
		}
		
		public void clear() { Arrays.fill( array, null ); size = 0;}
		
		public V set( int index, V value ) {
			
			V ret = null;
			if (index < size) ret = array[index];
			else resize( index = size, 1, false );
			
			array[index] = value;
			
			return ret;
		}
		
		public void add( int index, V value ) {
			if (size < index) index = size;
			
			resize( index, 1, false );
			array[index] = value;
		}
		
		public RW<V> clone()          { return (RW<V>) super.clone(); }
		
		RW<V> shell()                 { return (RW<V>) super.shell(); }
		
		public Consumer<V> consumer() {return this; }
		
		
	}
}
	