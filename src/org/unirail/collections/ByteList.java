package org.unirail.collections;


import java.util.Arrays;

public interface ByteList {
	
	interface IDst {
		void add(byte value);
		
		void write(int size);
	}
	
	interface ISrc {
		int size();
		
		byte  get(int index);
		
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
		
		byte[] array = Array.bytes0     ;
		
		
		
		public int length() { return array == null ? 0 : array.length; }
		
		int size = 0;
		
		public int size()                           { return size; }
		
		public boolean isEmpty()                    { return size == 0; }
		
		public boolean contains( byte value) {return -1 < indexOf(value);}
		
		
		public byte[] toArray(int index, int len, byte[] dst) {
			if (size == 0) return null;
			if (dst == null) dst = new byte[len];
			for (int i = 0; i < len; i++) dst[index + i] = (byte) array[i];
			
			return dst;
		}
		
		public boolean containsAll(ISrc src) {
			for (int i = src.size(); -1 < --i; )
				if (!contains(src.get(i))) return false;
			
			return true;
		}
		
		
		public byte get(int index) {return  (byte) array[index]; }
		
		
		public int indexOf( byte value) {
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf( byte value) {
			for (int i = size - 1; -1 < i; i--)
				if (array[i] == (byte) value) return i;
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
		
		public RW(int length) { if (0 < length) array = new byte[length]; }
		
		public RW(byte... items) {
			this(items == null ? 0 : items.length);
			if (items != null)
			{
				size = items.length;
				for (int i = 0; i < size; i++)
					array[i] = (byte) items[i];
			}
		}
		
		public RW(R src, int fromIndex, int toIndex) {
			this(toIndex - fromIndex);
			System.arraycopy(src.array, fromIndex, array, 0, toIndex - fromIndex);
		}
		
		public byte[] array()                 {return array;}
		
		
		public byte[] length(int length) {
			if (0 < length)
			{
				if (length < size) size = length;
				return array = Arrays.copyOf(array, length);
			}
			size = 0;
			return array = length == 0 ? Array.bytes0      : new byte[-length];
		}
		
		public void add(byte value) {
			size = Array.resize(this, size, size, 1);
			array[size - 1] = (byte) value;
		}
		
		public void add(int index, byte value) {
			if (index < size)
			{
				size = Array.resize(this, size, index, 1);
				array[index] = (byte) value;
			}
			else set(index, value);
			
		}
		
		public void remove() { remove(size - 1);}
		
		public void remove(int index) {
			if (size < 1 || size <= index) return;
			size = Array.resize(this, size, index, -1);
		}
		
		public void remove_fast(int index) {
			if (size < 1 || size <= index) return;
			size--;
			if (index < size ) array[index] = array[size];
		}
		
		
		public void set(int index, byte... values) {
			int len = values.length;
			
			if (size <= index + len)
			{
				int    fix = size;
				Object obj = array;
				
				size = Array.resize(this, size, index, len);
				if (obj == array) Arrays.fill(array, fix, size - 1, (byte) 0);
			}
			
			for (int i = 0; i < len; i++)
				array[index + i] = (byte) values[i];
		}
		
		public void set(byte value) { set(size, value);}
		
		public void set(int index, byte value) {
			if (size <= index)
			{
				int    fix = size;
				Object obj = array;
				
				size = Array.resize(this, size, index, 1);
				if (obj == array) Arrays.fill(array, fix, size - 1, (byte) 0);
			}
			
			array[index] = (byte) value;
		}
		
		public void swap(int index1, int index2) {
			final byte tmp = array[index1];
			array[index1] = array[index2];
			array[index2] = tmp;
			
		}
		
		public void addAll(ISrc src) {
			int s = src.size();
			write(s);
			for (int i = 0; i < s; i++) array[size + i] = (byte) src.get(i);
			size += s;
		}
		
		public boolean addAll(ISrc src, int index) {
			int s = src.size();
			size = Array.resize(this, size, index, s);
			for (int i = 0; i < s; i++) array[index + i] = (byte) src.get(i);
			return true;
		}
		
		
		public int removeAll(ISrc src) {
			int fix = size;
			
			for (int i = 0, k, src_size = src.size(); i < src_size; i++)
				if (-1 < (k = indexOf(src.get(i)))) remove(k);
			return fix - size;
		}
		
		public int removeAll(byte src) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf(src)); ) remove(k);
			return fix - size;
		}
		
		public int removeAll_fast(byte src) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf(src)); ) remove_fast(k);
			return fix - size;
		}
		
		public boolean retainAll(R chk) {
			
			final int   fix = size;
			byte v;
			for (int index = 0; index < size; index++)
				if (!chk.contains(v = get(index)))
					remove(indexOf(v));
			
			return fix != size;
		}
		
		public void clear() { size = 0;}
		
		//region  IDst
		@Override public void write(int size) {
			if (array.length < size) length(-size);
			this.size = 0;
		}
		//endregion
		
		
		public RW clone()   { return (RW) super.clone(); }
		
	}
}
