package org.unirail.collections;


import java.util.Arrays;

public interface BitsList {
	
	interface Consumer {
		void consume(int index, long src);
		
		void consume(int items, int bits);
	}
	
	interface Producer {
		int size();
		
		int bits();
		
		long produce(int index);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			int bits = bits();
			int mask = (int) mask(bits);
			
			if (dst == null) dst = new StringBuilder(size * 2);
			else dst.ensureCapacity(dst.length() + size * 2);
			
			long src = produce(0);
			for (int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++)
			{
				final int bit   = bit(bp);
				short     value = (short) (Base.BITS < bit + bits ? value(src, src = produce(index(bp) + 1), bit, bits, mask) : value(src, bit, mask));
				
				dst.append(value).append('\t');
				
				if (i % 10 == 0) dst.append('\t').append(i / 10 * 10).append('\n');
			}
			
			return dst;
		}
	}
	
	static final long FFFFFFFFFFFFFFFF = ~0L;
	
	static long mask(int bits)                                           {return (1L << bits) - 1;}
	
	static int size(int src)                                             {return src >>> 3;}
	
	static int index(int item_X_bits)                                    {return item_X_bits >> Base.LEN;}
	
	static int bit(int item_X_bits)                                      {return item_X_bits & Base.MASK;}
	
	static long value(long src, int bit, int mask)                       {return src >>> bit & mask; }
	
	static long value(long prev, long next, int bit, int bits, int mask) {return ((next & mask(bit + bits - Base.BITS)) << Base.BITS - bit | prev >>> bit) & mask; }
	
	
	static long set(int src, int item, long buff, int mask, int bits, Consumer dst) {
		
		final long v   = src & mask;
		final int  bit = bit(item *= bits);
		
		final long k = Base.BITS - bit;
		
		if (bits <= k) return buff << k >>> k | v << bit | buff >>> bit + bits << bit + bits;
		
		dst.consume(index(item), buff << k | v << bit);
		return k << bits + k | v >> k;
	}
	
	
	abstract class Base implements Cloneable, Comparable<Base>, Producer {
		
		
		static final int BITS = 64;
		static final int MASK = BITS - 1;
		static final int LEN  = 6;
		
		protected long[] array = Array.longs0;
		protected int    size  = 0;
		
		
		protected int bits;
		protected int mask;
		
		protected void add(int src) {
			long v = src & mask;
			
			final int p = bits * ++size;
			if (length() * BITS < p) length(Math.max(length() + length() / 2, len4bits(p)));
			
			final int index = index(p);
			final int bit   = bit(p);
			
			if (bit == 0) array[index] = v;
			else
			{
				array[index] |= v << bit;
				if (BITS < bit + bits) array[index + 1] = v >> BITS - bit;
			}
			
		}
		
		protected void add(int index, int value) {
			
			if (size <= index)
			{
				set(this, index, value);
				return;
			}
			
			long v = value & mask;
			
			int p = index * bits;
			
			 index = index(p);
			
			final long[] src  = array;
			long[]       dst_ = array;
			
			if (length() * BITS < p) length(Math.max(length() + length() / 2, len4bits(p)));
			
			
			final int bit = bit(p);
			if (0 < bit)
			{
				final long i = src[index];
				final int  k = BITS - bit;
				if (bit + bits < BITS)
				{
					dst_[index] = i << k >>> k | v << bit | i >>> bit << bit + bits;
					v = i >>> bit + bits | src[index + 1] << k - bits & mask;
				}
				else
				{
					dst_[index] = i << k >>> k | v << bit;
					v = v >> k | i >> bit << bits - k;
				}
				index++;
			}
			
			size++;
			
			for (final int max = len4bits(size * bits); ; )
			{
				final long i = src[index];
				dst_[index] = i << bits | v;
				if (max < ++index) break;
				v = i >>> BITS - bits;
			}
		}
		
		protected void clear() {
			for (int i = index(bits * size) + 1; -1 < --i; )
				array[i] = 0;
			
			size = 0;
		}
		
		protected void remove(int value) {
			for (int i = size; -1 < (i = lastIndexOf(i, value)); )
				removeAt(this, i);
		}
		
		protected int bits(int bits) {
			mask = (int) mask(bits);
			return this.bits = bits;
		}
		
		public int bits()                 {return bits;}
		
		protected Base(int bits_per_item) {bits(bits_per_item); }
		
		protected Base(int bits_per_item, int items) {
			bits(bits_per_item);
			array = new long[len4bits(items * bits)];
		}
		
		protected static void set(Base dst, int from, char... src) {
			
			final int bits = dst.bits;
			for (char c : src)
			{
				final int item  = bits * dst.size++;
				final int index = from + index(item);
				final int bit   = bit(item);
				
				final long v = c & dst.mask;
				
				dst.array[index] |= v << bit;
				if (BITS < bit + bits) dst.array[index + 1] = v >> BITS - bit;
			}
		}
		
		protected static void set(Base dst, int from, int... src) {
			
			final int bits = dst.bits;
			for (int i : src)
			{
				final int item  = bits * dst.size++;
				final int index = from + index(item);
				final int bit   = bit(item);
				
				final long v = i & dst.mask;
				
				dst.array[index] |= v << bit;
				if (BITS < bit + bits) dst.array[index + 1] = v >> BITS - bit;
			}
		}
		
		protected static void consume(Base dst, int size, int bits) {
			if (dst.length() < len4bits((dst.size = size) * bits)) dst.length(-size * bits);
			else Arrays.fill(dst.array, 0);
		}
		
		public int length() { return array.length * BITS / bits; }
		
		protected void length(int items) {
			if (0 < items)
			{
				if (items < size) size = items;
				
				array = Arrays.copyOf(array, len4bits(items * bits));
				return;
			}
			
			size = 0;
			
			array = items == 0 ? Array.longs0 : new long[len4bits(-items * bits)];
		}
		
		public int size()                   { return size; }
		
		public boolean isEmpty()            { return size == 0; }
		
		public boolean contains(int value)  { return -1 < indexOf(value); }
		
		public boolean contains(char value) { return -1 < indexOf(value); }
		
		static int len4bits(int bits)       {return (bits + BITS) >>> LEN;}
		
		public byte[] toArray(byte[] dst) {
			if (size == 0) return null;
			if (dst == null || dst.length < size) dst = new byte[size];
			
			for (int item = 0, max = size * bits; item < max; item += bits)
				dst[item / bits] = (byte) value(item);
			
			return dst;
		}
		
		public char value(int item) {
			final int index = index(item *= bits);
			final int bit   = bit(item);
			
			return (char) (BITS < bit + bits ? BitsList.value(array[index], array[index + 1], bit, bits, mask) : BitsList.value(array[index], bit, mask));
		}
		
		public int indexOf(char value) { return indexOf((int) value); }
		
		public int indexOf(int value) {
			for (int item = 0, max = size * bits; item < max; item += bits)
				if (value == value(item)) return item / bits;
			
			return -1;
		}
		
		public int lastIndexOf(char value) {return lastIndexOf((int) value); }
		
		public int lastIndexOf(int value)  {return lastIndexOf(size, value);}
		
		public int lastIndexOf(int from, int value) {
			for (int i = size; -1 < --i; )
				if (value == value(i)) return i;
			
			return -1;
		}
		
		
		public boolean equals(Object other) {
			
			return other != null &&
			       getClass() == other.getClass() &&
			       compareTo(getClass().cast(other)) == 0;
		}
		
		public boolean equals(Base other) { return other != null && compareTo(other) == 0; }
		
		@Override public int compareTo(Base other) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			int i = index(size);
			
			long m = (1L << bit(size)) - 1;
			if ((array[i] & m) != (other.array[i] & m)) return 2;
			
			while (-1 < --i)
				if (array[i] != other.array[i]) return (int) (array[i] - other.array[i]);
			
			return 0;
		}
		
		
		public boolean containsAll(Producer src) {
			return false;
		}
		
		
		public Base subList(int fromIndex, int toIndex) {
			return null;
		}
		
		protected static void set(Base dst, int item, int src) {
			
			final long v = src & dst.mask;
			final int
					p = item * dst.bits,
					index = index(p),
					bit = bit(p),
					k = BITS - bit;
			
			if (dst.size <= item)
			{
				if (dst.length() * BITS <= p) dst.length(Math.max(dst.length() + dst.length() / 2, len4bits(p)));
				
				dst.array[index] = index == index(dst.size * dst.bits) ? dst.array[index] & FFFFFFFFFFFFFFFF >> BITS - bit | v << bit : v << bit;
				
				dst.size = item + 1;
				return;
			}
			
			final long i = dst.array[index];
			
			if (k < dst.bits)
			{
				dst.array[index] = i << k | v << bit;
				dst.array[index + 1] = dst.array[index + 1] >>> dst.bits + k << dst.bits + k | v >> k;
			}
			else dst.array[index] = i << k >>> k | v << bit | i >>> bit + dst.bits << bit + dst.bits;
		}
		
		protected static void removeAt(Base dst, int item) {
			
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
		
		
		protected static boolean retainAll(Base dst, Base chk) {
			final int fix = dst.size;
			
			for (int item = 0, v; item < dst.size; item++)
				if (!chk.contains(v = dst.value(item))) dst.remove(v);
			
			return fix != dst.size;
		}
		
		
		@Override public Base clone() {
			try
			{
				Base dst = (Base) super.clone();
				dst.array = dst.length() == 0 ? array : array.clone();
				return dst;
				
			} catch (CloneNotSupportedException e) { e.printStackTrace(); }
			return null;
		}
		
		//region  producer
		@Override public long produce(int index) { return array[index];}
		
		//endregion
		public String toString() { return toString(null).toString();}
	}
	
	class R extends Base {
		
		protected R(int bits_per_item)            { super(bits_per_item); }
		
		protected R(int bits_per_item, int items) { super(bits_per_item, items); }
		
		public R(int bits_per_item, char... values) {
			super(bits_per_item, values.length);
			set(this, 0, values);
		}
		
		public R(int bits_per_item, int... values) {
			super(bits_per_item, values.length);
			set(this, 0, values);
		}
		
	}
	
	
	class RW extends R implements Consumer {
		
		public RW(int bits_per_item)                       { super(bits_per_item); }
		
		public RW(int bits_per_item, int items)            { super(bits_per_item, items); }
		
		public RW(int bits_per_item, char... values)       { super(bits_per_item, values); }
		
		public RW(int bits_per_item, int... values)        { super(bits_per_item, values); }
		
		@Override public void consume(int index, long src) { array[index] = src; }
		
		@Override public void consume(int items, int bits) { Base.consume(this, items, bits);}
		
		public void add(int value)                         { super.add(value); }
		
		public void add(char src)                          { super.add(src); }
		
		public void add(int index, int src)                { super.add(index, src); }
		
		public void add(int index, char src)               { super.add(index, src); }
		
		public void remove(int value)                      { super.remove(value); }
		
		public void remove(char value)                     { super.remove(value); }
		
		public void removeAt(int item)                     { removeAt(this, item); }
		
		public void set(int value)                         { set(this, size, value); }
		
		public void set(char value)                        { set(this, size, value); }
		
		public void set(int item, int value)               { set(this, item, value); }
		
		public void set(int item, char value)              { set(this, item, value); }
		
		public void set(int index, int... values)          { set(this, index, values); }
		
		public void set(int index, char... values)         { set(this, index, values); }
		
		public boolean retainAll(R chk)                    { return retainAll(this, chk); }
		
		public void clear()                                { super.clear(); }
		
		public void fit()                                  { length(size()); }
		
		@Override public RW clone()                        { return (RW) super.clone(); }
	}
}
