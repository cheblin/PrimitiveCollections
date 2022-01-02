package org.unirail.collections;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class Array<T> {
	
	public static int resize(Object src, Object dst, int index, int size, final int resize) {
		if (resize < 0)
		{
			if (index + (-resize) < size)
			{
				System.arraycopy(src, index + (-resize), dst, index, size - (index + (-resize)));
				if (src != dst) System.arraycopy(src, 0, dst, 0, index);
				return size + resize;
			}
			return index;
		}
		final int new_size = Math.max(index, size) + resize;
		if (0 < size)
			if (index < size)
				if (index == 0)
					System.arraycopy(src, 0, dst, resize, size);
				else
				{
					if (src != dst) System.arraycopy(src, 0, dst, 0, index);
					System.arraycopy(src, index, dst, index + resize, size - index);
				}
			else if (src != dst) System.arraycopy(src, 0, dst, 0, size);
		return new_size;
	}
	
	public static void copy(Object src, int index, final int skip, int size, Object dst) {
		if (0 < index) System.arraycopy(src, 0, dst, 0, index);
		if ((index += skip) < size) System.arraycopy(src, index, dst, index, size - index);
	}
	
	
	public static long nextPowerOf2(long v) {
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
	
	
	public static final byte[]   bytes0   = new byte[0];
	public static final char[]   chars0   = new char[0];
	public static final short[]  shorts0  = new short[0];
	public static final int[]    ints0    = new int[0];
	public static final long[]   longs0   = new long[0];
	public static final float[]  floats0  = new float[0];
	public static final double[] doubles0 = new double[0];
	
	
	public static int hash(int hash, boolean[] val) {return hash ^ Arrays.hashCode(val);}
	public static int hash(int hash, byte[] val)    {return hash ^ Arrays.hashCode(val);}
	public static int hash(int hash, char[] val)    {return hash ^ Arrays.hashCode(val);}
	public static int hash(int hash, short[] val)   {return hash ^ Arrays.hashCode(val);}
	public static int hash(int hash, int[] val)     {return hash ^ Arrays.hashCode(val);}
	public static int hash(int hash, long[] val)    {return hash ^ Arrays.hashCode(val);}
	public static int hash(int hash, float[] val)   {return hash ^ Arrays.hashCode(val);}
	public static int hash(int hash, double[] val)  {return hash ^ Arrays.hashCode(val);}
	public static int hash(int hash, Object[] val)  {return hash ^ Arrays.hashCode(val);}
	
	public static int hash(boolean[] val)           {return Arrays.hashCode(val);}
	public static int hash(byte[] val)              {return Arrays.hashCode(val);}
	public static int hash(char[] val)              {return Arrays.hashCode(val);}
	public static int hash(short[] val)             {return Arrays.hashCode(val);}
	public static int hash(int[] val)               {return Arrays.hashCode(val);}
	public static int hash(long[] val)              {return Arrays.hashCode(val);}
	public static int hash(float[] val)             {return Arrays.hashCode(val);}
	public static int hash(double[] val)            {return Arrays.hashCode(val);}
	public static int hash(Object[] val)            {return Arrays.hashCode(val);}
	
	public static int hash(int hash, Object val)    {return hash ^ hash(val);}
	public static int hash(int hash, double val)    {return hash ^ hash(val);}
	public static int hash(int hash, float val)     {return hash ^ hash(val);}
	public static int hash(int hash, int val)       {return hash ^ hash(val);}
	public static int hash(int hash, long val)      {return hash ^ hash(val);}
	
	
	public static int hash(Object val)              {return val == null ? 0x85ebca6b : hash(val.hashCode());}
	public static int hash(double val)              {return hash(Double.doubleToLongBits(val));}
	public static int hash(float val)               {return hash(Float.floatToIntBits(val));}
	public static int hash(int val) {
		val = (val ^ (val >>> 16)) * 0x85ebca6b;
		val = (val ^ (val >>> 13)) * 0xc2b2ae35;
		return val ^ (val >>> 16);
	}
	public static int hash(long val) {
		val = (val ^ (val >>> 32)) * 0x4cd6944c5cc20b6dL;
		val = (val ^ (val >>> 29)) * 0xfc12c5b19d3259e9L;
		return (int) (val ^ (val >>> 32));
	}
	
	
	public int hashCode(T src)        {return hash(src);}
	
	public boolean equals(T v1, T v2) {return Objects.equals(v1, v2);}
	
	public int hashCode(T[] src, int len) {
		int hash = Array.class.hashCode();
		while (-1 < --len) hash = (len + 10153331) + hash(hash, hashCode(src[len]));
		return hash;
	}
	
	public boolean equals(T[] O, T[] X, int len) {
		if (O.length < len || X.length < len) return false;
		for (T o, x; -1 < --len; )
			if ((o = O[len]) != (x = X[len]))
				if (o == null || x == null || !equals(o, x)) return false;
		return true;
	}
	
	public final T[] zeroid;
	@SuppressWarnings("unchecked")
	private Array(Class<T> clazz) {zeroid = (T[]) java.lang.reflect.Array.newInstance(clazz, 0);}
	
	public T[] copyOf(T[] current, int len) {return len < 1 ? zeroid : Arrays.copyOf(current == null ? zeroid : current, len);}
	
	private static final Array<boolean[]> booleans = new Array<boolean[]>(boolean[].class) {
		@Override public int hashCode(boolean[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(boolean[] v1, boolean[] v2) {return Arrays.equals(v1, v2);}
	};
	private static final Array<byte[]>    bytes    = new Array<byte[]>(byte[].class) {
		@Override public int hashCode(byte[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(byte[] v1, byte[] v2) {return Arrays.equals(v1, v2);}
	};
	private static final Array<short[]>   shorts   = new Array<short[]>(short[].class) {
		@Override public int hashCode(short[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(short[] v1, short[] v2) {return Arrays.equals(v1, v2);}
	};
	private static final Array<char[]>    chars    = new Array<char[]>(char[].class) {
		@Override public int hashCode(char[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(char[] v1, char[] v2) {return Arrays.equals(v1, v2);}
	};
	private static final Array<int[]>     ints     = new Array<int[]>(int[].class) {
		@Override public int hashCode(int[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(int[] v1, int[] v2) {return Arrays.equals(v1, v2);}
	};
	private static final Array<long[]>    longs    = new Array<long[]>(long[].class) {
		@Override public int hashCode(long[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(long[] v1, long[] v2) {return Arrays.equals(v1, v2);}
	};
	private static final Array<float[]>   floats   = new Array<float[]>(float[].class) {
		@Override public int hashCode(float[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(float[] v1, float[] v2) {return Arrays.equals(v1, v2);}
	};
	private static final Array<double[]>  doubles  = new Array<double[]>(double[].class) {
		@Override public int hashCode(double[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(double[] v1, double[] v2) {return Arrays.equals(v1, v2);}
	};
	
	private static final Array<Object[]> objects = new Array<Object[]>(Object[].class) {
		@Override public int hashCode(Object[] src) {return Arrays.hashCode(src);}
		@Override public boolean equals(Object[] v1, Object[] v2) {return Arrays.equals(v1, v2);}
	};
	
	private static final HashMap<Class<?>, Object> pool = new HashMap<>(8);
	
	@SuppressWarnings("unchecked")
	public static <T> Array<T> get(Class<T> clazz) {
		final Object c = clazz;
		if (c == boolean[].class) return (Array<T>) booleans;
		if (c == byte[].class) return (Array<T>) bytes;
		if (c == short[].class) return (Array<T>) shorts;
		if (c == int[].class) return (Array<T>) ints;
		if (c == long[].class) return (Array<T>) longs;
		if (c == char[].class) return (Array<T>) chars;
		if (c == float[].class) return (Array<T>) floats;
		if (c == double[].class) return (Array<T>) doubles;
		if (c == Object[].class) return (Array<T>) objects;
		if (pool.containsKey(clazz)) return (Array<T>) pool.get(clazz);
		synchronized (pool)
		{
			if (pool.containsKey(clazz)) return (Array<T>) pool.get(clazz);
			Array<T> ret = new Array<>(clazz);
			pool.put(clazz, ret);
			return ret;
		}
	}
	
	
	public interface ISort {
		
		int compare(int ia, int ib);//array[ia] < array[ib] ? -1 : array[ia] == array[ib] ? 0 : 1;
		
		/**
		 var t = indexes[ia];
		 indexes[ia] = indexes[ib];
		 indexes[ib] = t;
		 */
		void swap(int ia, int ib);
		
		void set(int dst_index, int src_index);//replace element at dst_index with element at src_index
		
		void asc();//ascending sort order
		
		void desc();//descending order
		
		int compare(int src_index);// compare fixed element with element at index;
		
		void get(int src_index);//fix a value at index
		
		void set(int dst_index);//put fixed value at index
		
		//binary search in sorted array index of the element that value is equal to the fixed element
		static int search(ISort dst, int lo, int hi) {
			while (lo <= hi)
			{
				final int o = lo + (hi - lo >> 1), dir = dst.compare(o);
				
				if (dir == 0) return o;
				if (0 < dir) lo = o + 1;
				else hi = o - 1;
			}
			return ~lo;
		}
		
		
		static void sort(ISort dst, int lo, int hi) {
			
			int len = hi - lo + 1;
			if (len < 2) return;
			
			int pow2 = 0;
			while (0 < len)
			{
				pow2++;
				len >>>= 1;
			}
			
			sort(dst, lo, hi, 2 * pow2);
		}
		
		static void sort(ISort dst, final int lo, int hi, int limit) {
			
			while (hi > lo)
			{
				int size = hi - lo + 1;
				
				if (size < 17)
					switch (size)
					{
						case 1:
							return;
						case 2:
							if (dst.compare(lo, hi) > 0) dst.swap(lo, hi);
							return;
						case 3:
							if (dst.compare(lo, hi - 1) > 0) dst.swap(lo, hi - 1);
							if (dst.compare(lo, hi) > 0) dst.swap(lo, hi);
							if (dst.compare(hi - 1, hi) > 0) dst.swap(hi - 1, hi);
							return;
						default:
							for (int i = lo; i < hi; i++)
							{
								dst.get(i + 1);
								
								int j = i;
								while (lo <= j && dst.compare(j) < 0) dst.set(j + 1, j--);
								
								dst.set(j + 1);
							}
							return;
					}
				
				if (limit == 0)
				{
					final int w = hi - lo + 1;
					for (int i = w >>> 1; 0 < i; i--) heapify(dst, i, w, lo);
					
					for (int i = w; 1 < i; i--)
					{
						dst.swap(lo, lo + i - 1);
						heapify(dst, 1, i - 1, lo);
					}
					return;
				}
				
				limit--;
				
				final int o = lo + (hi - lo >> 1);
				
				if (dst.compare(lo, o) > 0) dst.swap(lo, o);
				if (dst.compare(lo, hi) > 0) dst.swap(lo, hi);
				if (dst.compare(o, hi) > 0) dst.swap(o, hi);
				
				dst.get(o);
				dst.swap(o, hi - 1);
				int l = lo, h = hi - 1;
				
				while (l < h)
				{
					while (-1 < dst.compare(++l)) ;
					while (dst.compare(--h) < 0) ;
					
					if (h <= l) break;
					
					dst.swap(l, h);
				}
				
				if (l != hi - 1) dst.swap(l, hi - 1);
				sort(dst, l + 1, hi, limit);
				hi = l - 1;
			}
		}
		
		static void heapify(ISort dst, int i, final int w, int lo) {
			dst.get(--lo + i);
			
			while (i <= w >>> 1)
			{
				int child = i << 1;
				if (child < w && dst.compare(lo + child, lo + child + 1) < 0) child++;
				
				if (-1 < dst.compare(lo + child)) break;
				
				dst.set(lo + i, lo + child);
				i = child;
			}
			
			dst.set(lo + i);
		}
	}
}
