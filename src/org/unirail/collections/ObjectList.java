package org.unirail.collections;

import org.unirail.Hash;

import java.util.Arrays;

public interface ObjectList {
	
	abstract class R<V extends Comparable<? super V>> implements Comparable<R<V>> {
		
		
		protected V[] array;
		
		public int length() {return array == null ? 0 : array.length;}
		
		int size = 0;
		
		public int size() {return size;}
		
		public V[] toArray( int index, int len, V[] dst ) {
			if (size == 0) return null;
			if (dst == null || dst.length < len) return Arrays.copyOfRange( array, index, len );
			System.arraycopy( array, index, dst, 0, len );
			return dst;
		}
		
		public boolean containsAll( R<V> src ) {
			
			for (int i = 0, s = src.size(); i < s; i++)
				if (-1 < indexOf( src.get( i ) )) return false;
			return true;
		}
		
		public boolean hasValue( int index ) {return index < size && array[index] != null;}
		
		public V get( int index )            {return array[index];}
		
		
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
		
		public boolean equals( R<V> other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R<V> other ) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			for (int i = 0, diff; i < size; i++)
				if (array[i] != null && other.array[i] != null)
				{
					if ((diff = array[i].compareTo( other.array[i] )) != 0) return diff;
				}
				else if (array[i] != other.array[i]) return -1;
			
			return 0;
		}
		
		public int hashCode() {
			int hash = 93321341;
			for (int i = size(); -1 < --i; ) hash = Hash.code( hash, array[i] );
			return hash;
		}
		
		@SuppressWarnings("unchecked")
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
				if (dst.array != null) dst.array = array.clone();
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			
			return null;
		}
		
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			for (int i = 0; i < size; i++)
			     dst.append( get( i ) ).append( '\n' );
			
			return dst;
		}
	}
	
	
	class RW<V extends Comparable<? super V>> extends R<V> implements Array {
		@SuppressWarnings("unchecked")
		public RW( int length ) {if (0 < length) array = (V[]) new Comparable[length];}
		
		@SafeVarargs public RW( V... items ) {
			this( 0 );
			if (items == null) return;
			array = items.clone();
			size  = items.length;
		}
		
		public RW( V fill_value, int size ) {
			this( size );
			this.size = size;
			if (fill_value == null) return;
			while (-1 < --size) array[size] = fill_value;
		}
		
		public Comparable[] array() {return array;}
		
		@SuppressWarnings("unchecked")
		public Comparable[] length( int length ) {
			if (0 < length)
			{
				if (length < size) size = length;
				return array = array == null ? (V[]) new Comparable[length] : Arrays.copyOf( array, length );
			}
			size = 0;
			return array = length == 0 ? null : (V[]) new Comparable[-length];
		}
		
		public void clear() {
			if (size < 1) return;
			Arrays.fill( array, 0, size - 1, null );
			size = 0;
		}
		
		
		public V add( V value ) {
			size = Array.resize( this, size, size, 1 );
			return array[size - 1] = value;
		}
		
		public V add( int index, V value ) {
			if (index < size)
			{
				size = Array.resize( this, size, index, 1 );
				return array[index] = value;
			}
			return set( index, value );
		}
		
		public void remove()            {remove( size - 1 );}
		
		public void remove( int index ) {size = Array.resize( this, size, index, -1 );}
		
		public void remove_fast( int index ) {
			if (size < 1 || size <= index) return;
			size--;
			array[index] = array[size];
			array[size]  = null;
		}
		
		public V set( V value ) {return set( size, value );}
		
		
		@SafeVarargs
		public final void set( int index, V... values ) {
			int len = values.length;
			
			if (size <= index + len) size = Array.resize( this, size, size, index + len - size );
			
			System.arraycopy( values, 0, array, index, len );
		}
		
		
		public V set( int index, V value ) {
			if (size <= index) size = Array.resize( this, size, index, 1 );
			return array[index] = value;
		}
		
		public void addAll( R<V> src, int count ) {
			final int s = size;
			size = Array.resize( this, size, size, count );
			
			for (int i = 0, max = src.size(); i < max; i++)
			     array[s + i] = src.get( i );
		}
		
		public void addAll( R<V> src, int index, int count ) {
			count = Math.min( src.size(), count );
			size  = Array.resize( this, size, index, count );
			for (int i = 0; i < count; i++) array[index + i] = src.get( i );
		}
		
		public void removeAll( R<V> src )   {for (int i = 0, max = src.size(), p; i < max; i++) removeAll( src.get( i ) );}
		
		public void removeAll( V src )      {for (int k; -1 < (k = indexOf( src )); ) remove( k );}
		
		public void removeAll_fast( V src ) {for (int k; -1 < (k = indexOf( src )); ) remove_fast( k );}
		
		public void retainAll( R<V> chk ) {
			for (int i = 0; i < size; i++)
			{
				final V val = array[i];
				if (chk.indexOf( val ) == -1) removeAll( val );
			}
			
		}
		
		
		public RW<V> clone() {return (RW<V>) super.clone();}
		
	}
}
	