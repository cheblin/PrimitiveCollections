package org.unirail.collections;


import java.util.Arrays;

import static org.unirail.collections.Array.hash;

public interface ByteList {
	
	
	abstract class R {
		byte[] values = Array.bytes0     ;
		
		int size = 0;
		
		public int size()                           {return size;}
		
		public boolean isEmpty()                    {return size == 0;}
		
		public boolean contains( byte value) {return -1 < indexOf(value);}
		
		
		public byte[] toArray(int index, int len, byte[] dst) {
			if (size == 0) return null;
			if (dst == null) dst = new byte[len];
			for (int i = 0; i < len; i++) dst[index + i] = (byte) values[i];
			
			return dst;
		}
		
		public boolean containsAll(R src) {
			for (int i = src.size(); -1 < --i; )
				if (!contains(src.get(i))) return false;
			
			return true;
		}
		
		public byte get(int index) {return  (byte) values[index];}
		
		public int indexOf( byte value) {
			for (int i = 0; i < size; i++)
				if (values[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf( byte value) {
			for (int i = size - 1; -1 < i; i--)
				if (values[i] == value) return i;
			return -1;
		}
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R other) {
			if (other == null || other.size != size) return false;
			
			for (int i = size(); -1 < --i; )
				if (values[i] != other.values[i]) return false;
			return true;
		}
		
		public int hashCode() {
			int hash = 999197497;
			for (int i = size(); -1 < --i; ) hash = hash(hash, values[i]);
			return hash;
		}
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				
				dst.values = values.clone();
				dst.size = size;
				
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
			
		}
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
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
	
	
	class RW extends R {
		
		public RW(int length) {if (0 < length) values = new byte[length];}
		
		public RW(byte... items) {
			this(items == null ? 0 : items.length);
			if (items != null)
			{
				size = items.length;
				for (int i = 0; i < size; i++)
					values[i] = (byte) items[i];
			}
		}
		
		public RW(byte fill_value, int size) {
			this(size);
			this.size = size;
			if (fill_value == 0) return;
			
			while (-1 < --size) values[size] = (byte) fill_value;
		}
		
		public RW(R src, int fromIndex, int toIndex) {
			this(toIndex - fromIndex);
			System.arraycopy(src.values, fromIndex, values, 0, toIndex - fromIndex);
		}
		
		
		public void add(byte value) {add(size, value);}
		
		public void add(int index, byte value) {
			
			int max = Math.max(index, size + 1);
			
			size = Array.resize(values, values.length <= max ? values = new byte[max + max / 2] : values, index, size, 1);
			values[index] = (byte) value;
		}
		
		public void add(int index, byte[] src, int src_index, int len) {
			int max = Math.max(index, size) + len;
			
			size = Array.resize(values, values.length < max ? values = new byte[max + max / 2] : values, index, size, len);
			
			for (int i = 0; i < len; i++) values[index + i] = (byte) src[src_index + i];
		}
		
		public void remove() {remove(size - 1);}
		
		public void remove(int index) {
			if (size < 1 || size < index) return;
			if (index == size - 1) values[--size] = (byte) 0;
			else size = Array.resize(values, values, index, size, -1);
		}
		
		public void remove_fast(int index) {
			if (size < 1 || size <= index) return;
			values[index] = values[--size];
		}
		
		
		public void set(int index, byte... src) {
			int len = src.length;
			int max = index + len;
			
			if (size < max)
			{
				if (values.length < max) Array.copy(values, index, len, size, values = new byte[max + max / 2]);
				size = max;
			}
			
			for (int i = 0; i < len; i++)
				values[index + i] = (byte) src[i];
		}
		
		public void set(byte value) {set(size, value);}
		
		public void set(int index, byte value) {
			
			if (size <= index)
			{
				if (values.length <= index) values = Arrays.copyOf(values, index + index / 2);
				size = index + 1;
			}
			
			values[index] = (byte) value;
		}
		
		public void swap(int index1, int index2) {
			final byte tmp = values[index1];
			values[index1] = values[index2];
			values[index2] = tmp;
			
		}
		
		public int removeAll(R src) {
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
		
		public void clear() {
			if (size < 1) return;
			Arrays.fill(values, 0, size - 1, (byte) 0);
			size = 0;
		}
		
		
		public RW clone() {return (RW) super.clone();}
		
	}
}
