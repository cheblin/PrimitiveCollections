package org.unirail.collections;


import java.util.Arrays;

public interface ShortShiftList {
	
	interface IDst {
		void add(long value);
		
		void write(long shift, int size);
	}
	
	interface ISrc {
		int size();
		
		long shift();
		
		long get(int index);
		
		default StringBuilder toString(StringBuilder dst) {
			int  size  = size();
			long shift = shift();
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
		
		short[] array = Array.shorts0     ;
		
		public final long shift;
		@Override public long shift() { return shift; }
		protected R(long shift)       {this.shift = shift;}
		
		public int length()           { return array.length; }
		
		protected int size = 0;
		
		public int size()                   { return size; }
		
		public boolean isEmpty()            { return size == 0; }
		
		public boolean contains(long value) {return -1 < indexOf(value);}
		
		
		public long[] toArray(int index, int len, long[] dst) {
			if (size == 0) return null;
			if (dst == null) dst = new long[len];
			
			for (int i = 0; i < len; i++) dst[index + i] = array[i] + shift;
			
			return dst;
		}
		
		public boolean containsAll(ISrc src) {
			for (int i = src.size(); -1 < --i; )
				if (!contains(src.get(i))) return false;
			
			return true;
		}
		
		
		public long get(int index) {return array[index] + shift; }
		
		
		public int indexOf(long value) {
			value -= shift;
			for (int i = 0; i < size; i++)
				if (array[i] == value) return i;
			return -1;
		}
		
		public int lastIndexOf(long value) {
			value -= shift;
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
			if (other.size != size || other.shift != shift) return other.size - size;
			
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
		
		public RW(long shift, int length) {
			super(shift);
			if (0 < length) array = new short[length];
		}
		
		public RW(long shift, long... items) {
			super(shift);
			if (items != null)
			{
				size = items.length;
				for (int i = 0; i < size; i++)
					array[i] = (short) (items[i] - shift);
			}
		}
		
		public RW(R src, int fromIndex, int toIndex) {
			this(toIndex - fromIndex);
			System.arraycopy(src.array, fromIndex, array, 0, toIndex - fromIndex);
		}
		
		
		public short[] array() {return array;}
		
		
		public short[] length(int length) {
			if (0 < length)
			{
				if (length < size) size = length;
				return array = Arrays.copyOf(array, length);
			}
			size = 0;
			return array = length == 0 ? Array.shorts0      : new short[-length];
		}
		
		public void add(long value) {
			size = Array.resize(this, size, size, 1);
			array[size - 1] = (short) (value - shift);
		}
		
		public void add(int index, long value) {
			if (index < size)
			{
				size = Array.resize(this, size, index, 1);
				array[index] = (short) (value - shift);
			}
			else set(index, value);
			
		}
		
		public void remove() { remove(size - 1);}
		
		public void remove(int index) {size = Array.resize(this, size, index, -1);}
		
		public void remove_fast(int index) {
			if (size < 1 || size <= index) return;
			array[index] = array[--size];
		}
		
		
		public void set(int index, long... values) {
			int len = values.length;
			
			if (size <= index + len) size = Array.resize(this, size, size, index + len - size);
			
			for (int i = 0; i < len; i++)
				array[index + i] = (short) (values[i] - shift);
		}
		
		public void set(long value) { set(size, value);}
		
		public void set(int index, long value) {
			if (size <= index) size = Array.resize(this, size, index, 1);
			array[index] = (short) (value - shift);
		}
		
		public void swap(int index1, int index2) {
			final short tmp = array[index1];
			array[index1] = array[index2];
			array[index2] = tmp;
		}
		
		public void addAll(ISrc src) { addAll(src, 0); }
		
		public void addAll(ISrc src, int index) {
			int s = src.size();
			size = Array.resize(this, size, index, s);
			for (int i = 0; i < s; i++) array[index + i] = (short) (src.get(i) - shift);
		}
		
		
		public int removeAll(ISrc src) {
			int fix = size;
			
			for (int i = 0, k, src_size = src.size(); i < src_size; i++)
				if (-1 < (k = indexOf(src.get(i)))) remove(k);
			return fix - size;
		}
		
		public int removeAll(short src) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf(src)); ) remove(k);
			return fix - size;
		}
		
		public int removeAll_fast(short src) {
			int fix = size;
			
			for (int k; -1 < (k = indexOf(src)); ) remove_fast(k);
			return fix - size;
		}
		
		public boolean retainAll(R chk) {
			
			final int fix = size;
			long      v;
			for (int index = 0; index < size; index++)
				if (!chk.contains(v = get(index)))
					remove(indexOf(v));
			
			return fix != size;
		}
		
		public void clear() {
			if (size < 1) return;
			Arrays.fill(array, 0, size - 1, (short) 0);
			size = 0;
		}
		
		//region  IDst
		
		
		@Override public void write(long shift, int size) {
			if (array.length < size) length(-size);
			else clear();
			this.size = size;
		}
		
		//endregion
		
		public RW clone() { return (RW) super.clone(); }
	}
}
