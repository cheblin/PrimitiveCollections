package org.unirail.collections;




import java.util.Arrays;

import static org.unirail.collections.Array.HashEqual.hash;

public interface ShortList {
	
	
	abstract class R  {
		short[] array = Array.shorts0     ;
		
		public int length() {return array.length;}
		
		int size = 0;
		
		public int size()                            {return size;}
		
		public boolean isEmpty()                     {return size == 0;}
		
		public boolean contains( short value ) {return -1 < indexOf( value );}
		
		
		public short[] toArray( int index, int len, short[] dst ) {
			if (size == 0) return null;
			if (dst == null) dst = new short[len];
			for (int i = 0; i < len; i++) dst[index + i] = (short) array[i];
			
			return dst;
		}
		
		public boolean containsAll( R src ) {
			for (int i = src.size(); -1 < --i; )
				if (!contains( src.get( i ) )) return false;
			
			return true;
		}
		
		public short get( int index ) {return  (short) array[index];}
		
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
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			if (other == null || other.size != size) return false;
			
			for (int i = size(); -1 < --i; )
				if (array[i] != other.array[i]) return false;
			return true;
		}
		
		public int hashCode() {
			int hash = 999197497;
			for (int i = size(); -1 < --i; ) hash = hash( hash, array[i] );
			return hash;
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
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			for (int i = 0; i < size; i++)
			{
				dst.append( get( i ) ).append( '\t' );
				if (i % 10 == 0) dst.append( '\t' ).append( i ).append( '\n' );
			}
			return dst;
		}
		
	}
	
	
	class RW extends R implements Array {
		
		public RW( int length ) {if (0 < length) array = new short[length];}
		
		public RW( short... items ) {
			this( items == null ? 0 : items.length );
			if (items != null)
			{
				size = items.length;
				for (int i = 0; i < size; i++)
				     array[i] = (short) items[i];
			}
		}
		
		public RW( short fill_value, int size ) {
			this( size );
			this.size = size;
			if (fill_value == 0) return;
			
			while (-1 < --size) array[size] = (short) fill_value;
		}
		
		public RW( R src, int fromIndex, int toIndex ) {
			this( toIndex - fromIndex );
			System.arraycopy( src.array, fromIndex, array, 0, toIndex - fromIndex );
		}
		
		public short[] array() {return array;}
		
		
		public short[] length( int length ) {
			if (0 < length)
			{
				if (length < size) size = length;
				return array = Arrays.copyOf( array, length );
			}
			size = 0;
			return array = length == 0 ? Array.shorts0      : new short[-length];
		}
		
		public void add( short value ) {
			size            = Array.resize( this, size, size, 1 );
			array[size - 1] = (short) value;
		}
		
		public void add( int index, short value ) {
			if (index < size)
			{
				size         = Array.resize( this, size, index, 1 );
				array[index] = (short) value;
			}
			else set( index, value );
			
		}
		
		public void remove() {remove( size - 1 );}
		
		public void remove( int index ) {
			if (size < 1 || size < index) return;
			if (index == size - 1) array[--size] = (short) 0;
			else size = Array.resize( this, size, index, -1 );
		}
		
		public void remove_fast( int index ) {
			if (size < 1 || size <= index) return;
			array[index] = array[--size];
		}
		
		
		public void set( int index, short... values ) {
			int len = values.length;
			
			if (size <= index + len) size = Array.resize( this, size, size, index + len - size );
			
			for (int i = 0; i < len; i++)
			     array[index + i] = (short) values[i];
		}
		
		public void set( short value ) {set( size, value );}
		
		public void set( int index, short value ) {
			
			if (size <= index) size = Array.resize( this, size, index, 1 );
			array[index] = (short) value;
		}
		
		public void swap( int index1, int index2 ) {
			final short tmp = array[index1];
			array[index1] = array[index2];
			array[index2] = tmp;
			
		}
		
		public void addAll( R src ) {
			int s = src.size();
			for (int i = 0; i < s; i++) array[size + i] = (short) src.get( i );
			size += s;
		}
		
		public boolean addAll( R src, int index ) {
			int s = src.size();
			size = Array.resize( this, size, index, s );
			for (int i = 0; i < s; i++) array[index + i] = (short) src.get( i );
			return true;
		}
		
		
		public int removeAll( R src ) {
			int fix = size;
			
			for (int i = 0, k, src_size = src.size(); i < src_size; i++)
				if (-1 < (k = indexOf( src.get( i ) ))) remove( k );
			return fix - size;
		}
		
		public int removeAll( short src ) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf( src )); ) remove( k );
			return fix - size;
		}
		
		public int removeAll_fast( short src ) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf( src )); ) remove_fast( k );
			return fix - size;
		}
		
		public boolean retainAll( R chk ) {
			
			final int   fix = size;
			short v;
			for (int index = 0; index < size; index++)
				if (!chk.contains( v = get( index ) ))
					remove( indexOf( v ) );
			
			return fix != size;
		}
		
		public void clear() {
			if (size < 1) return;
			Arrays.fill( array, 0, size - 1, (short) 0 );
			size = 0;
		}
		
		
		public RW clone() {return (RW) super.clone();}
		
	}
}
