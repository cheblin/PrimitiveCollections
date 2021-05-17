package org.unirail.collections;

import java.util.Arrays;

public interface ObjectList {
	interface Writer<V extends Comparable<? super V>> {
		V add(V value);
		
		void write(int size);
	}
	
	interface Reader<V extends Comparable<? super V>> {
		int size();
		
		V value(int index);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			for (int i = 0; i < size; i++)
				dst.append(value(i)).append('\n');
			
			return dst;
		}
	}
	
	
	class R<V extends Comparable<? super V>> implements Comparable<R<V>>, Reader<V> {
		
		@SuppressWarnings("unchecked")
		protected R(int length) { if (0 < length) array = (V[]) new Comparable[length]; }
		
		@SafeVarargs protected R(V... items) {
			this(0);
			if (items == null) return;
			array = items.clone();
			size = items.length;
		}
		
		protected V[] array;
		
		public int length() { return array == null ? 0 : array.length; }
		
		int size = 0;
		
		public int size() { return size; }
		
		public V[] toArray(int index, int len, V[] dst) {
			if (size == 0) return null;
			if (dst == null || dst.length < len) return Arrays.copyOfRange(array, index, len);
			System.arraycopy(array, index, dst, 0, len);
			return dst;
		}
		
		public boolean containsAll(Reader<V> src) {
			
			for (int i = 0, s = src.size(); i < s; i++)
				if (-1 < indexOf(src.value(i))) return false;
			return true;
		}
		
		
		public V value(int index) {return array[index]; }
		
		
		public int indexOf(V value) {
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf(V value) {
			for (int i = size - 1; -1 < i; i--)
				if (array[i] == value) return i;
			return -1;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R<V> other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R<V> other) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			for (int i = 0, diff; i < size; i++)
				if (array[i] != null)
					if (other.array[i] == null)
					{
						if (other.array[i] != null) return -1;
					}
					else if ((diff = array[i].compareTo(other.array[i])) != 0) return diff;
			
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
		
		
		public String toString() { return toString(null).toString();}
	}
	
	
	class RW<V extends Comparable<? super V>> extends R<V> implements Array, Writer<V> {
		
		public RW(int length) { super(length); }
		
		public RW(V... items) { super(items); }
		
		public V[] array()    {return array;}
		
		@SuppressWarnings("unchecked")
		public V[] length(int length) {
			if (0 < length)
			{
				if (length < size) size = length;
				return array = array == null ? (V[]) new Comparable[length] : Arrays.copyOf(array, length);
			}
			size = 0;
			return array = length == 0 ? null : (V[]) new Comparable[-length];
		}
		
		public void clear() {
			Arrays.fill(array, null);
			size = 0;
		}
		
		//region  writer
		@Override public void write(int size) {
			this.size = 0;
			if (array.length < size) length(-this.size);
			else clear();
		}
		//endregion
		
		
		public V add(V value) {
			size = Array.resize(this, size, size, 1);
			return array[size - 1] = value;
		}
		
		public V add(int index, V value) {
			if (index < size)
			{
				size = Array.resize(this, size, index, 1);
				return array[index] = value;
			}
			return set(index, value);
		}
		
		public void remove() { remove(size - 1);}
		
		public void remove(int index) {
			if (size < 1 || size <= index) return;
			size = Array.resize(this, size, index, -1);
		}
		
		public void remove_fast(int index) {
			if (size < 1 || size <= index) return;
			size--;
			if (index < size) array[index] = array[size];
			array[size] = null;
		}
		
		public V set(V value) {return set(size, value);}
		
		
		@SafeVarargs
		public final void set(int index, V... values) {
			int len = values.length;
			
			if (size <= index + len)
			{
				int    fix = size;
				Object obj = array;
				
				size = Array.resize(this, size, index, len);
				if (obj == array) Arrays.fill(array, fix, size - 1, null);
			}
			
			System.arraycopy(values, 0, array, index, len);
		}
		
		
		public V set(int index, V value) {
			if (size <= index)
			{
				int    fix = size;
				Object obj = array;
				
				size = Array.resize(this, size, index, 1);
				if (obj == array) Arrays.fill(array, fix, size - 1, null);
			}
			
			return array[index] = value;
		}
		
		public boolean addAll(Reader<V> src, int count) {
			int s = size;
			size = Array.resize(this, size, size, count);
			
			for (int i = 0, size = src.size(); i < size; i++)
				array[s++] = src.value(i);
			
			return true;
		}
		
		public boolean addAll(Reader<V> src, int index, int count) {
			count = Math.min(src.size(), count);
			size = Array.resize(this, size, index, count);
			for (int i = 0; i < count; i++) array[index + i] = src.value(i);
			return true;
		}
		
		public boolean removeAll(Reader<V> src) {
			final int s = size;
			
			for (int k = 0, src_size = src.size(); k < src_size; k++)
				for (int i = size - 1; i >= 0; i--) if (array[i] == src.value(k)) size = Array.resize(this, size, i, -1);
			return size != s;
		}
		
		public int removeAll(V src) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf(src)); ) remove(k);
			return fix - size;
		}
		
		public int removeAll_fast(V src) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf(src)); ) remove_fast(k);
			return fix - size;
		}
		
		public boolean retainAll(R<V> chk) {
			
			final int s = size;
			
			for (int i = 0, max = size; i < max; i++)
				if (chk.indexOf(array[i]) == -1)
				{
					final V val = array[i];
					for (int j = size; j > 0; j--) if (array[j] == val) size = Array.resize(this, size, j, -1);
					max = size;
				}
			
			return s != size;
		}
		
		
		public RW<V> clone() { return (RW<V>) super.clone(); }
		
	}
}
	