package org.unirail.collections;


import java.util.Arrays;

public interface BitsList {
	
	interface IDst {
		void write(int index, long src);
		
		void write(long shift, int size, int bits);
	}
	
	interface ISrc {
		int size();
		
		int bits();
		
		long shift();
		
		long read(int index);
		
		default StringBuilder toString(StringBuilder dst) {
			int  size  = size();
			int  bits  = bits();
			long shift = shift();
			int  mask  = (int) mask(bits);
			
			if (dst == null) dst = new StringBuilder(size * 4);
			else dst.ensureCapacity(dst.length() + size * 4);
			
			long src = read(0);
			for (int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++)
			{
				final int bit   = bit(bp);
				long      value = (BITS < bit + bits ? value(src, src = read(index(bp) + 1), bit, bits, mask) : value(src, bit, mask)) + shift;
				
				dst.append(value).append('\t');
				
				if (i % 10 == 0) dst.append('\t').append(i / 10 * 10).append('\n');
			}
			
			return dst;
		}
		
		default boolean equals(ISrc other) {
			int i = size();
			if (i != other.size() || bits() != other.bits() || shift() != other.shift()) return false;
			if (i != other.size()) return false;
			for (i--, i >>>= 6; -1 < i; i--) if (read(i) != other.read(i)) return false;
			return true;
		}
	}
	
	long FFFFFFFFFFFFFFFF = ~0L;
	
	static long mask(int bits)                                            {return (1L << bits) - 1;}
	
	static int size(int src)                                              {return src >>> 3;}
	
	static int index(int item_X_bits)                                     {return item_X_bits >> LEN;}
	
	static int bit(int item_X_bits)                                       {return item_X_bits & MASK;}
	
	static long value(long src, int bit, long mask)                       {return src >>> bit & mask; }
	
	static long value(long prev, long next, int bit, int bits, long mask) {return ((next & mask(bit + bits - BITS)) << BITS - bit | prev >>> bit) & mask; }
	
	int BITS = 64;
	int MASK = BITS - 1;
	int LEN  = 6;
	
	static int len4bits(int bits) {return (bits + BITS) >>> LEN;}
	
	static long set(int src, int item, long buff, int mask, int bits, IDst dst) {
		
		final long v   = src & mask;
		final int  bit = bit(item *= bits);
		
		final long k = BITS - bit;
		
		if (bits <= k) return buff << k >>> k | v << bit | buff >>> bit + bits << bit + bits;
		
		dst.write(index(item), buff << k | v << bit);
		return k << bits + k | v >> k;
	}
	
	abstract class R implements Cloneable, Comparable<R>, ISrc {
		
		protected long[] array = Array.longs0;
		protected int    size  = 0;
		
		public int size()        { return size; }
		
		public boolean isEmpty() { return size == 0; }
		
		protected    long mask;
		public final int  bits;
		
		public int bits() {return bits;}
		
		public final long shift;
		
		public long shift() { return shift; }
		
		protected R(long shift, int bits_per_item) {
			mask = mask(bits_per_item);
			bits = bits_per_item;
			this.shift = shift;
		}
		
		protected R(long shift, int bits_per_item, int items) {
			mask = mask(bits_per_item);
			bits = bits_per_item;
			this.shift = shift;
			array = new long[len4bits(items * bits)];
		}
		
		protected void clear() {
			for (int i = index(bits * size) + 1; -1 < --i; ) array[i] = 0;
			size = 0;
		}
		
		public boolean equals(Object other) {
			return other != null &&
			       getClass() == other.getClass() &&
			       ISrc.super.equals(getClass().cast(other));
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		@Override public int compareTo(R other) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			int i = index(size);
			
			long m = (1L << bit(size)) - 1;
			if ((array[i] & m) != (other.array[i] & m)) return 2;
			
			while (-1 < --i)
				if (array[i] != other.array[i]) return (int) (array[i] - other.array[i]);
			
			return 0;
		}
		
		public int length()                          { return array.length * BITS / bits; }
		
		protected static void write(R dst, int size) { if (dst.length() < len4bits((dst.size = size) * dst.bits)) R.length(dst, -size * dst.bits); }
		
		protected static void length(R dst, int items) {
			if (0 < items)
			{
				if (items < dst.size) dst.size = items;
				
				dst.array = Arrays.copyOf(dst.array, len4bits(items * dst.bits));
				return;
			}
			
			dst.size = 0;
			dst.array = items == 0 ? Array.longs0 : new long[len4bits(-items * dst.bits)];
		}
		
		
		protected static void add(R dst, int item, long value) {
			
			if (dst.size <= item)
			{
				set(dst, item, value);
				return;
			}
			
			int p = item * dst.bits;
			
			item = index(p);
			
			final long[] src  = dst.array;
			long[]       dst_ = dst.array;
			
			if (dst.length() * BITS < p) R.length(dst, Math.max(dst.length() + dst.length() / 2, len4bits(p)));
			
			long      v   = (value - dst.shift) & dst.mask;
			final int bit = bit(p);
			if (0 < bit)
			{
				final long i = src[item];
				final int  k = BITS - bit;
				if (bit + dst.bits < BITS)
				{
					dst_[item] = i << k >>> k | v << bit | i >>> bit << bit + dst.bits;
					v = i >>> bit + dst.bits | src[item + 1] << k - dst.bits & dst.mask;
				}
				else
				{
					dst_[item] = i << k >>> k | v << bit;
					v = v >> k | i >> bit << dst.bits - k;
				}
				item++;
			}
			
			dst.size++;
			
			for (final int max = len4bits(dst.size * dst.bits); ; )
			{
				final long i = src[item];
				dst_[item] = i << dst.bits | v;
				if (max < ++item) break;
				v = i >>> BITS - dst.bits;
			}
		}
		
		protected static void set(R dst, int from, byte... src)  { for (int i = src.length; -1 < --i; ) set(dst, from + i, src[i]);}
		protected static void set(R dst, int from, char... src)  { for (int i = src.length; -1 < --i; ) set(dst, from + i, src[i]);}
		protected static void set(R dst, int from, short... src) { for (int i = src.length; -1 < --i; ) set(dst, from + i, src[i]);}
		protected static void set(R dst, int from, int... src)   { for (int i = src.length; -1 < --i; ) set(dst, from + i, src[i]);}
		protected static void set(R dst, int from, long... src)  { for (int i = src.length; -1 < --i; ) set(dst, from + i, src[i]);}
		
		protected static void set(R dst, int item, long src) {
			final long v = (src - dst.shift) & dst.mask;
			final int
					p = item * dst.bits,
					index = index(p),
					bit = bit(p);
			if (dst.size <= item)
			{
				if (dst.length() <= item) R.length(dst, Math.max(dst.length() + dst.length() / 2, len4bits(p)));
				dst.array[index] = index == index(dst.size * dst.bits) ? dst.array[index] & FFFFFFFFFFFFFFFF >> BITS - bit | v << bit : v << bit;
				dst.size = item + 1;
				return;
			}
			final long i = dst.array[index];
			final int  k = BITS - 1 - bit;
			if (k < dst.bits)
			{
				dst.array[index] = i << k | v << bit;
				dst.array[index + 1] = dst.array[index + 1] >>> dst.bits + k << dst.bits + k | v >> k;
			}
			else dst.array[index] = i >>> bit + dst.bits << bit + dst.bits | v << bit | i << k >>> k;
		}
		protected static void add(R dst, long src) {set(dst, dst.size, src);}
		
		protected static void removeAt(R dst, int item) {
			
			if (item + 1 == dst.size)
			{
				dst.size--;
				return;
			}
			
			int       index = index(item *= dst.bits);
			final int bit   = bit(item);
			
			final int k = BITS - bit;
			long      i = dst.array[index];
			
			if (index + 1 == dst.length())
			{
				if (bit == 0) dst.array[index] = i >>> dst.bits;
				else if (k < dst.bits) dst.array[index] = i << k >>> k;
				else if (dst.bits < k) dst.array[index] = i << k >>> k | i >>> bit + dst.bits << bit;
				
				dst.size--;
				return;
			}
			
			if (bit == 0) dst.array[index] = i >>>= dst.bits;
			else if (k < dst.bits)
			{
				long ii = dst.array[index + 1];
				
				dst.array[index] = i << k >>> k | ii >>> bit + dst.bits - BITS << bit;
				dst.array[++index] = i = ii >>> dst.bits;
			}
			else if (dst.bits < k)
			{
				long ii = dst.array[index + 1];
				
				dst.array[index] = i << k >>> k | i >>> bit + dst.bits << bit | ii << BITS - dst.bits;
				dst.array[++index] = i = ii >>> dst.bits;
			}
			
			for (final int max = dst.size * dst.bits; index * BITS < max; )
			{
				long ii = dst.array[index + 1];
				dst.array[index] = i << dst.bits >>> dst.bits | ii << BITS - dst.bits;
				dst.array[++index] = i = ii >>> dst.bits;
			}
			
			dst.size--;
		}
		
		public long get(int item) {
			final int index = index(item *= bits);
			final int bit   = bit(item);
			
			return BITS < bit + bits ? value(array[index], array[index + 1], bit, bits, mask) : value(array[index], bit, mask);
		}
		
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				if (0 < dst.length()) dst.array = array.clone();
				return dst;
				
			} catch (CloneNotSupportedException e) { e.printStackTrace(); }
			return null;
		}
		
		//region  ISrc
		@Override public long read(int index) { return array[index];}
		
		//endregion
		public String toString() { return toString(null).toString();}
		
		public int indexOf(long value) {
			for (int item = 0, max = size * bits; item < max; item += bits)
				if (value == get(item)) return item / bits;
			
			return -1;
		}
		
		public int lastIndexOf(long value) {return lastIndexOf(size, value);}
		
		public int lastIndexOf(int from, long value) {
			for (int i = from; -1 < --i; )
				if (value == get(i)) return i;
			
			return -1;
		}
		
		
		public boolean containsAll(ISrc src) {
			return false;
		}
		
		
		public R subList(int fromIndex, int toIndex) {
			return null;
		}
		
		
		protected static void remove(R dst, long value) {
			for (int i = dst.size; -1 < (i = dst.lastIndexOf(i, value)); )
				removeAt(dst, i);
		}
		
		public boolean contains(long value) { return -1 < indexOf(value); }
		
		public byte[] toArray(byte[] dst) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new byte[size];
			
			for (int item = 0, max = size * bits; item < max; item += bits)
				dst[item / bits] = (byte) get(item);
			
			return dst;
		}
	}
	
	
	class RW extends R implements IDst {
		
		public RW(int bits_per_item)                        { super(0, bits_per_item); }
		public RW(long shift, int bits_per_item)            { super(shift, bits_per_item); }
		
		public RW(int bits_per_item, int items)             { super(0, bits_per_item, items); }
		public RW(int bits_per_item, long shift, int items) { super(shift, bits_per_item, items); }
		
		public RW(int bits_per_item, byte... values) {
			super(0, bits_per_item, values.length);
			set(this, 0, values);
		}
		public RW(long shift, int bits_per_item, byte... values) {
			super(shift, bits_per_item, values.length);
			set(this, 0, values);
		}
		
		public RW(int bits_per_item, char... values) {
			super(0, bits_per_item, values.length);
			set(this, 0, values);
		}
		public RW(long shift, int bits_per_item, char... values) {
			super(shift, bits_per_item, values.length);
			set(this, 0, values);
		}
		
		public RW(int bits_per_item, short... values) {
			super(0, bits_per_item, values.length);
			set(this, 0, values);
		}
		public RW(long shift, int bits_per_item, short... values) {
			super(shift, bits_per_item, values.length);
			set(this, 0, values);
		}
		
		public RW(int bits_per_item, int... values) {
			super(0, bits_per_item, values.length);
			set(this, 0, values);
		}
		public RW(long shift, int bits_per_item, int... values) {
			super(shift, bits_per_item, values.length);
			set(this, 0, values);
		}
		
		public RW(int bits_per_item, long... values) {
			super(0, bits_per_item, values.length);
			set(this, 0, values);
		}
		
		public RW(long shift, int bits_per_item, long... values) {
			super(shift, bits_per_item, values.length);
			set(this, 0, values);
		}
		
		
		//region  IDst
		@Override public void write(int index, long src) { array[index] = src; }
		
		@Override public void write(long shift, int size, int bits) { R.write(this, size);}
		
		//endregion
		
		public void add(long value)                { add(this, value); }
		
		public void add(int index, long src)       { add(this, index, src); }
		
		public void remove(long value)             { remove(this, value); }
		
		public void remove(char value)             { remove(this, value); }
		
		public void removeAt(int item)             { removeAt(this, item); }
		
		public void set(int value)                 { set(this, size, value); }
		
		public void set(char value)                { set(this, size, value); }
		
		public void set(int item, int value)       { set(this, item, value); }
		
		public void set(int item, char value)      { set(this, item, value); }
		
		public void set(int index, char... values) { set(this, index, values); }
		
		public boolean retainAll(R chk) {
			final int fix = size;
			
			long v;
			for (int item = 0; item < size; item++)
				if (!chk.contains(v = get(item))) remove(this, v);
			
			return fix != size;
		}
		
		public void clear()         { super.clear(); }
		
		public void fit()           { length(this, size()); }
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
	
}
