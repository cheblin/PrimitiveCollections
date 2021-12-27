package org.unirail.collections;


import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

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
	
	static <T> boolean equals(T[] a, T[] a2, Comparator<? super T> cmp) {
		
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
	
	long[] longs0 = new long[0];
	
	float[] floats0 = new float[0];
	
	double[] doubles0 = new double[0];
	
	
	class HashEqual<V> {
		
		public static int hash(int hash, Object val) {return hash ^ hash(val);}
		
		public static int hash(int hash, double val) {return hash ^ hash(val);}
		
		public static int hash(int hash, float val)  {return hash ^ hash(val);}
		
		public static int hash(int hash, long val)   {return hash ^ hash(val);}
		
		public static int hash(int hash, int val)    {return hash ^ hash(val);}
		
		public static int hash(Object val) {return val == null ? 0x85ebca6b : hash(val.hashCode());}
		
		public static int hash(double val)           {return hash(Double.doubleToLongBits(val));}
		
		public static int hash(float val)           {return hash(Float.floatToIntBits(val));}
		
		public static int hash(long val) {
			val = (val ^ (val >>> 32)) * 0x4cd6944c5cc20b6dL;
			val = (val ^ (val >>> 29)) * 0xfc12c5b19d3259e9L;
			return (int) (val ^ (val >>> 32));
		}
		
		public static int hash(int val) {
			val = (val ^ (val >>> 16)) * 0x85ebca6b;
			val = (val ^ (val >>> 13)) * 0xc2b2ae35;
			return val ^ (val >>> 16);
		}
		
		public int hashCode(V src)        {return hash(src);}
		
		public boolean equals(V v1, V v2) {return Objects.equals(v1, v2);}
		
		public int hashCode(V[] src, int len) {
			int hash = HashEqual.class.hashCode();
			while (-1 < --len) hash =(len + 10153331) + hash(hash  , hashCode(src[len]));
			return hash;
		}
		
		public boolean equals(V[] O, V[] X, int len) {
			for (V o, x; -1 < --len; )
				if ((o = O[len]) != (x = X[len]))
					if (o == null || x == null || !equals(o, x)) return false;
			
			return true;
		}
		
		private final V[] zeroid;
		@SuppressWarnings("unchecked")
		private HashEqual(Class<V[]> clazz) {zeroid = (V[]) java.lang.reflect.Array.newInstance(clazz.getComponentType(), 0);}
		
		public V[] copyOf(V[] current, int len) {return len < 1 ? zeroid : Arrays.copyOf(current == null ? zeroid : current, len);}
		
		private static final HashEqual<boolean[]> booleans = new HashEqual<boolean[]>(boolean[][].class) {
			@Override public int hashCode(boolean[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(boolean[] v1, boolean[] v2) {return Arrays.equals(v1, v2);}
		};
		private static final HashEqual<byte[]>    bytes    = new HashEqual<byte[]>(byte[][].class) {
			@Override public int hashCode(byte[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(byte[] v1, byte[] v2) {return Arrays.equals(v1, v2);}
		};
		private static final HashEqual<short[]>   shorts   = new HashEqual<short[]>(short[][].class) {
			@Override public int hashCode(short[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(short[] v1, short[] v2) {return Arrays.equals(v1, v2);}
		};
		private static final HashEqual<char[]>    chars    = new HashEqual<char[]>(char[][].class) {
			@Override public int hashCode(char[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(char[] v1, char[] v2) {return Arrays.equals(v1, v2);}
			
		};
		private static final HashEqual<int[]>     ints     = new HashEqual<int[]>(int[][].class) {
			@Override public int hashCode(int[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(int[] v1, int[] v2) {return Arrays.equals(v1, v2);}
			
		};
		private static final HashEqual<long[]>    longs    = new HashEqual<long[]>(long[][].class) {
			@Override public int hashCode(long[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(long[] v1, long[] v2) {return Arrays.equals(v1, v2);}
			
		};
		private static final HashEqual<float[]>   floats   = new HashEqual<float[]>(float[][].class) {
			@Override public int hashCode(float[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(float[] v1, float[] v2) {return Arrays.equals(v1, v2);}
		};
		private static final HashEqual<double[]>  doubles  = new HashEqual<double[]>(double[][].class) {
			@Override public int hashCode(double[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(double[] v1, double[] v2) {return Arrays.equals(v1, v2);}
		};
		
		private static final HashEqual<Object[]> objects = new HashEqual<Object[]>(Object[][].class) {
			@Override public int hashCode(Object[] src) {return Arrays.hashCode(src);}
			@Override public boolean equals(Object[] v1, Object[] v2) {return Arrays.equals(v1, v2);}
		};
		
		static final HashMap<Class<?>, Object> pool = new HashMap<>(8);
		
		@SuppressWarnings("unchecked")
		public static <R> HashEqual<R> get(Class<R[]> clazz) {
			final Object c = clazz;
			
			if (c == boolean[][].class) return (HashEqual<R>) booleans;
			if (c == byte[][].class) return (HashEqual<R>) bytes;
			if (c == short[][].class) return (HashEqual<R>) shorts;
			if (c == int[][].class) return (HashEqual<R>) ints;
			if (c == long[][].class) return (HashEqual<R>) longs;
			if (c == char[][].class) return (HashEqual<R>) chars;
			if (c == float[][].class) return (HashEqual<R>) floats;
			if (c == double[][].class) return (HashEqual<R>) doubles;
			if (c == Object[][].class) return (HashEqual<R>) objects;
			
			if (pool.containsKey(clazz)) return (HashEqual<R>) pool.get(clazz);
			
			HashEqual<R> ret = new HashEqual<>(clazz);
			
			pool.put(clazz, ret);
			return ret;
		}
	}
}
