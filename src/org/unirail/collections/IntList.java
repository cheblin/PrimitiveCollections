package org.unirail.collections;


import java.util.Arrays;

public interface IntList {
	
	interface IDst {
		void add(int value);
		
		void write(int size);
	}
	
	interface ISrc {
		int size();
		
		int  get(int index);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			for (int i = 0; i < size; i++)
			{
				dst.append(get(i)).append('\t');
				if (i % 10 == 0) dst.append('\t').append(i).append('\n');
			}
			return dst;
		}
	}
	
	
	abstract class R implements Comparable<R>, ISrc {
		int[] array = Array.ints0     ;
		
		public int length() { return array.length; }
		
		int size = 0;
		
		public int size()                           { return size; }
		
		public boolean isEmpty()                    { return size == 0; }
		
		public boolean contains( int value) {return -1 < indexOf(value);}
		
		
		public int[] toArray(int index, int len, int[] dst) {
			if (size == 0) return null;
			if (dst == null) dst = new int[len];
			for (int i = 0; i < len; i++) dst[index + i] =  array[i];
			
			return dst;
		}
		
		public boolean containsAll(ISrc src) {
			for (int i = src.size(); -1 < --i; )
				if (!contains(src.get(i))) return false;
			
			return true;
		}
		
		public int get(int index) {return   array[index]; }
		
		public int indexOf( int value) {
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf( int value) {
			for (int i = size - 1; -1 < i; i--)
				if (array[i] == value) return i;
			return -1;
		}
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R other) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			for (int i = 0; i < size; i++)
				if (array[i] != other.array[i]) return 1;
			return 0;
		}
		
		public int hashCode() {
			int hashCode = 1;
			for (int i = 0; i < size; i++) hashCode = 31 * hashCode + Array.hash(get(i));
			
			return hashCode;
		}
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				
				dst.array = array.clone();
				dst.size = size;
				
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
			
		}
		public String toString() { return toString(null).toString();}
	}
	
	
	class RW extends R implements Array, IDst {
		
		public RW(int length) { if (0 < length) array = new int[length]; }
		
		public RW(int... items) {
			this(items == null ? 0 : items.length);
			if (items != null)
			{
				size = items.length;
				for (int i = 0; i < size; i++)
					array[i] = (int) items[i];
			}
		}
		
		public RW(R src, int fromIndex, int toIndex) {
			this(toIndex - fromIndex);
			System.arraycopy(src.array, fromIndex, array, 0, toIndex - fromIndex);
		}
		
		public int[] array() {return array;}
		
		
		public int[] length(int length) {
			if (0 < length)
			{
				if (length < size) size = length;
				return array = Arrays.copyOf(array, length);
			}
			size = 0;
			return array = length == 0 ? Array.ints0      : new int[-length];
		}
		
		public void add(int value) {
			size = Array.resize(this, size, size, 1);
			array[size - 1] = (int) value;
		}
		
		public void add(int index, int value) {
			if (index < size)
			{
				size = Array.resize(this, size, index, 1);
				array[index] = (int) value;
			}
			else set(index, value);
			
		}
		
		public void remove() { remove(size - 1);}
		
		public void remove(int index) {
			if (size < 1 || size < index) return;
			if (index == size - 1) array[--size] = (int) 0;
			else size = Array.resize(this, size, index, -1);
		}
		
		public void remove_fast(int index) {
			if (size < 1 || size <= index) return;
			array[index] = array[--size];
		}
		
		
		public void set(int index, int... values) {
			int len = values.length;
			
			if (size <= index + len) size = Array.resize(this, size, size, index + len - size);
			
			for (int i = 0; i < len; i++)
				array[index + i] = (int) values[i];
		}
		
		public void set(int value) { set(size, value);}
		
		public void set(int index, int value) {
			
			if (size <= index) size = Array.resize(this, size, index, 1);
			array[index] = (int) value;
		}
		
		public void swap(int index1, int index2) {
			final int tmp = array[index1];
			array[index1] = array[index2];
			array[index2] = tmp;
			
		}
		
		public void addAll(ISrc src) {
			int s = src.size();
			write(s);
			for (int i = 0; i < s; i++) array[size + i] = (int) src.get(i);
			size += s;
		}
		
		public boolean addAll(ISrc src, int index) {
			int s = src.size();
			size = Array.resize(this, size, index, s);
			for (int i = 0; i < s; i++) array[index + i] = (int) src.get(i);
			return true;
		}
		
		
		public int removeAll(ISrc src) {
			int fix = size;
			
			for (int i = 0, k, src_size = src.size(); i < src_size; i++)
				if (-1 < (k = indexOf(src.get(i)))) remove(k);
			return fix - size;
		}
		
		public int removeAll(int src) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf(src)); ) remove(k);
			return fix - size;
		}
		
		public int removeAll_fast(int src) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf(src)); ) remove_fast(k);
			return fix - size;
		}
		
		public boolean retainAll(R chk) {
			
			final int   fix = size;
			int v;
			for (int index = 0; index < size; index++)
				if (!chk.contains(v = get(index)))
					remove(indexOf(v));
			
			return fix != size;
		}
		
		public void clear() {
			if (size < 1) return;
			Arrays.fill(array, 0, size - 1, (int) 0);
			size = 0;
		}
		
		//region  IDst
		@Override public void write(int size) {
			if (array.length < size) length(-size);
			else clear();
			
			this.size = size;
		}
		//endregion
		
		
		public RW clone() { return (RW) super.clone(); }
		
	}
}
