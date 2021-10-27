package org.unirail.collections;


import java.util.Comparator;

public interface Array extends Cloneable {
	
	
	Object array();
	
	Object length(int length);
	
	int length();
	
	static int resize(Array array, int size, int index, final int resize) {
		
		if (size < 0) size = 0;
		if (resize == 0) return size;
		
		
		if (index < 0) index = 0;
		
		
		if (resize < 0)
		{
			if (index == 0 && size <= -resize) return 0;
			
			
			if (size <= index) return size;
			
			if (index + (-resize) < size)//есть хвост который надо перенести
			{
				final Object tmp = array.array();
				System.arraycopy(tmp, index + (-resize), tmp, index, size - (index + (-resize)));
				
				size += resize;
			}
			else
				size = index + 1;
			
			
			return size;
		}
		
		final int new_size = Math.max(index, size) + resize;
		
		final int length = array.length();
		
		if (length < 1)
		{
			array.length(-new_size);
			return new_size;
		}
		
		Object src = array.array();
		Object dst = new_size < length ? src : array.length(-Math.max(new_size, length + length / 2));
		
		if (0 < size)
			if (index < size)
				if (index == 0) System.arraycopy(src, 0, dst, resize, size);
				else
				{
					if (src != dst) System.arraycopy(src, 0, dst, 0, index);
					System.arraycopy(src, index, dst, index + resize, size - index);
				}
			else if (src != dst) System.arraycopy(src, 0, dst, 0, size);
		
		return new_size;
	}
	
	
	static long nextPowerOf2(long v) {
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v |= v >> 32;
		v++;
		return v;
	}
	
	static <T extends Comparable<T>> boolean equals(T[] a, T[] a2, Comparator<? super T> cmp) {
		
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;
		
		int length = a.length;
		if (a2.length != length)
			return false;
		
		for (int i = 0; i < length; i++)
		{
			if (cmp.compare(a[i], a2[i]) != 0)
				return false;
		}
		
		return true;
	}
	
	
	byte[] bytes0 = new byte[0];
	
	char[] chars0 = new char[0];
	
	short[] shorts0 = new short[0];
	
	int[] ints0 = new int[0];
	
	float[] floats0 = new float[0];
	
	long[] longs0 = new long[0];
	
	double[] doubles0 = new double[0];
}
