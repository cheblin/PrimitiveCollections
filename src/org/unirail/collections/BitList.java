package org.unirail.collections;


import org.unirail.JsonWriter;

public interface BitList {
	
	
	abstract class R implements Cloneable {
		
		protected int size;
		
		public int size() {return size;}
		
		long[] values = Array.Of.longs.O;
		
		static int len4bits(int bits) {return 1 + (bits >> LEN);}
		
		static final int LEN  = 6;
		static final int BITS = 1 << LEN;
		static final int MASK = BITS - 1;
		
		static int index(int item_X_bits) {return item_X_bits >> LEN;}
		
		static long mask(int bits)        {return (1L << bits) - 1;}
		
		static final long FFFFFFFFFFFFFFFF = ~0L;
		static final int  OI               = Integer.MAX_VALUE;
		static final int  IO               = Integer.MIN_VALUE;
		
		
		int used = 0;
		
		int used() {
			if (-1 < used) return used;
			
			used &= OI;
			
			int i = used - 1;
			while (-1 < i && values[i] == 0) i--;
			
			return used = i + 1;
		}
		
		int used(int bit) {
			if (size() <= bit) size = bit + 1;
			
			final int index = bit >> LEN;
			if (index < used()) return index;
			
			if (values.length < (used = index + 1)) values = Array.copyOf(values, Math.max(2 * values.length, used));
			
			return index;
		}
		
		
		public boolean get(int bit) {
			final int index = bit >> LEN;
			return index < used() && (values[index] & 1L << bit) != 0;
		}
		
		public int get(int bit, int FALSE, int TRUE) {
			final int index = bit >> LEN;
			return index < used() && (values[index] & 1L << bit) != 0 ? TRUE : FALSE;
		}
		
		public int get(long[] dst, int from_bit, int to_bit) {
			
			final int ret = (to_bit - from_bit - 1 >> LEN) + 1;
			
			int index = from_bit >> LEN;
			
			if ((from_bit & MASK) == 0) System.arraycopy(values, index, dst, 0, ret - 1);
			else
				for (int i = 0; i < ret - 1; i++, index++)
				     dst[i] = values[index] >>> from_bit | values[index + 1] << -from_bit;
			
			
			long mask = FFFFFFFFFFFFFFFF >>> -to_bit;
			dst[ret - 1] =
					(to_bit - 1 & MASK) < (from_bit & MASK) ?
					values[index] >>> from_bit | (values[index + 1] & mask) << -from_bit
					                                        :
					(values[index] & mask) >>> from_bit;
			
			return ret;
		}
		
		
		public int next1(int bit) {
			
			int index = bit >> LEN;
			if (used() <= index) return -1;
			
			for (long i = values[index] & FFFFFFFFFFFFFFFF << bit; ; i = values[index])
			{
				if (i != 0) return index * BITS + Long.numberOfTrailingZeros(i);
				if (++index == used) return -1;
			}
		}
		
		
		public int next0(int bit) {
			
			int index = bit >> LEN;
			if (used() <= index) return bit;
			
			for (long i = ~values[index] & FFFFFFFFFFFFFFFF << bit; ; i = ~values[index])
			{
				if (i != 0) return index * BITS + Long.numberOfTrailingZeros(i);
				if (++index == used) return used * BITS;
			}
		}
		
		public int prev1(int bit) {
			
			int index = bit >> LEN;
			if (used() <= index) return last1() - 1;
			
			
			for (long i = values[index] & FFFFFFFFFFFFFFFF >>> -(bit + 1); ; i = values[index])
			{
				if (i != 0) return (index + 1) * BITS - 1 - Long.numberOfLeadingZeros(i);
				if (index-- == 0) return -1;
			}
		}
		
		
		public int prev0(int bit) {
			int index = bit >> LEN;
			if (used() <= index) return bit;
			
			for (long i = ~values[index] & FFFFFFFFFFFFFFFF >>> -(bit + 1); ; i = ~values[index])
			{
				if (i != 0) return (index + 1) * BITS - 1 - Long.numberOfLeadingZeros(i);
				if (index-- == 0) return -1;
			}
		}
		
		
		public int last1()       {return used() == 0 ? 0 : BITS * (used - 1) + BITS - Long.numberOfLeadingZeros(values[used - 1]);}
		
		
		public boolean isEmpty() {return used == 0;}
		
		
		public int rank(int bit) {
			final int max = bit >> LEN;
			
			if (max < used())
				for (int i = 0, sum = 0; ; i++)
					if (i < max) sum += Long.bitCount(values[i]);
					else return sum + Long.bitCount(values[i] & FFFFFFFFFFFFFFFF >>> BITS - (bit + 1));
			
			return cardinality();
		}
		
		
		public int cardinality() {
			for (int i = 0, sum = 0; ; i++)
				if (i < used()) sum += Long.bitCount(values[i]);
				else return sum;
		}
		
		public int bit(int cardinality) {
			
			int i = 0, c = 0;
			while ((c += Long.bitCount(values[i])) < cardinality) i++;
			
			long v = values[i];
			int  z = Long.numberOfLeadingZeros(v);
			
			for (long p = 1L << BITS - 1; cardinality < c; z++) if ((v & p >>> z) != 0) c--;
			
			return i * 32 + BITS - z;
		}
		
		
		public int hashCode() {
			int hash = 197;
			for (int i = used; --i >= 0; )
			     hash = Array.hash(hash, values[i]);
			
			return hash;
		}
		
		public int length() {return values.length * BITS;}
		
		
		public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				dst.values = values.length == 0 ? values : values.clone();
				return dst;
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			
			return null;
		}
		
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		
		public boolean equals(R other) {
			int i = size();
			if (i != other.size()) return false;
			for (i >>>= 6; -1 < i; i--) if (values[i] != other.values[i]) return false;
			return true;
		}
		
		
		public String toString() {
			JsonWriter        json  = JsonWriter.get();
			JsonWriter.Config config = json.enter();
			
			int size = size();
			
			int max = size >> LEN;
			
			json.preallocate((max + 1) * 68);
			
			json.enterArray();
			
			for (int i = 0; i < max; i++)
			{
				final long v = values[i];
				for (int s = 0; s < 64; s++)
				     json.value((v & 1L << s) == 0 ? 0 : 1);
			}
			
			if (0 < (size &= 63))
			{
				final long v = values[max];
				for (int s = 0; s < size; s++)
				     json.value((v & 1L << s) == 0 ? 0 : 1);
			}
			
			json.exitArray();
			
			return json.exit(config);
		}
	}
	
	
	class RW extends R {
		
		public RW(int length) {if (0 < length) values = new long[len4bits(length)];}
		
		public RW(boolean fill_value, int size) {
			
			int len = len4bits(this.size = size);
			values = new long[len];
			
			used = len | IO;
			
			if (fill_value) set1(0, size - 1);
		}
		
		public RW(R src, int from_bit, int to_bit) {
			
			if (src.size() <= from_bit) return;
			size = Math.min(to_bit, src.size() - 1) - from_bit;
			
			int i2 = src.get(to_bit) ? to_bit : src.prev1(to_bit);
			
			if (i2 == -1) return;
			
			values = new long[(i2 - 1 >> LEN) + 1];
			used   = values.length | IO;
			
			int
					i1 = src.get(from_bit) ? from_bit : src.next1(from_bit),
					index = i1 >>> LEN,
					max = (i2 >>> LEN) + 1,
					i = 0;
			
			for (long v = src.values[index] >>> i1; ; v >>>= i1, i++)
				if (index + 1 < max)
					values[i] = v | (v = src.values[index + i]) << BITS - i1;
				else
				{
					values[i] = v;
					return;
				}
		}
		
		
		public void and(R and) {
			if (this == and) return;
			
			if (and.used() < used())
				while (used > and.used) values[--used] = 0;
			
			for (int i = 0; i < used; i++) values[i] &= and.values[i];
			
			used |= IO;
		}
		
		
		public void or(R or) {
			if (or.used() < 1 || this == or) return;
			
			int u = used;
			if (used() < or.used())
			{
				if (values.length < or.used) values = Array.copyOf(values, Math.max(2 * values.length, or.used));
				used = or.used;
			}
			
			int min = Math.min(u, or.used);
			
			for (int i = 0; i < min; i++)
			     values[i] |= or.values[i];
			
			if (min < or.used) System.arraycopy(or.values, min, values, min, or.used - min);
			else if (min < u) System.arraycopy(values, min, or.values, min, u - min);
		}
		
		
		public void xor(R xor) {
			if (xor.used() < 1 || xor == this) return;
			
			int u = used;
			if (used() < xor.used())
			{
				if (values.length < xor.used) values = Array.copyOf(values, Math.max(2 * values.length, xor.used));
				used = xor.used;
			}
			
			final int min = Math.min(u, xor.used);
			for (int i = 0; i < min; i++)
			     values[i] ^= xor.values[i];
			
			if (min < xor.used) System.arraycopy(xor.values, min, values, min, xor.used - min);
			else if (min < u) System.arraycopy(values, min, xor.values, min, u - min);
			
			used |= IO;
		}
		
		public void andNot(R not) {
			for (int i = Math.min(used(), not.used()) - 1; -1 < i; i--) values[i] &= ~not.values[i];
			
			used |= IO;
		}
		
		public boolean intersects(R set) {
			for (int i = Math.min(used, set.used) - 1; i >= 0; i--)
				if ((values[i] & set.values[i]) != 0) return true;
			
			return false;
		}
		
		public void fit() {length(size());}
		
		void length(int bits) {
			if (0 < bits)
			{
				if (bits < size)
				{
					set0(bits, size + 1);
					size = bits;
				}
				values = Array.copyOf(values, index(bits) + 1);
				
				used |= IO;
				return;
			}
			
			size   = 0;
			used   = 0;
			values = Array.copyOf(values, index(-bits) + 1);
		}
		
		public void flip(int bit) {
			final int index = used(bit);
			if ((values[index] ^= 1L << bit) == 0 && index + 1 == used) used |= IO;
		}
		
		
		public void flip(int from_bit, int to_bit) {
			if (from_bit == to_bit) return;
			
			int from_index = from_bit >> LEN;
			int to_index   = used(to_bit - 1);
			
			final long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			final long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if (from_index == to_index)
			{
				if ((values[from_index] ^= from_mask & to_mask) == 0 && from_index + 1 == used) used |= IO;
			}
			else
			{
				values[from_index] ^= from_mask;
				
				for (int i = from_index + 1; i < to_index; i++) values[i] ^= FFFFFFFFFFFFFFFF;
				
				values[to_index] ^= to_mask;
				                    used |= IO;
			}
		}
		
		public void set(int index, boolean... values) {
			for (int i = 0, max = values.length; i < max; i++)
				if (values[i]) set1(index + i);
				else set0(index + i);
		}
		
		
		public void set1(int bit) {
			final int index = used(bit);//!!!
			values[index] |= 1L << bit;
		}
		
		
		public void add(boolean value) {set(size, value);}
		
		public void set(int bit, boolean value) {
			if (value)
				set1(bit);
			else
				set0(bit);
		}
		
		public void set(int bit, int value) {
			if (value == 0)
				set0(bit);
			else
				set1(bit);
		}
		
		public void set(int bit, int value, int TRUE) {
			if (value == TRUE)
				set1(bit);
			else
				set0(bit);
		}
		
		
		public void set1(int from_bit, int to_bit) {
			
			if (from_bit == to_bit) return;
			
			int from_index = from_bit >> LEN;
			int to_index   = used(to_bit - 1);
			
			long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if (from_index == to_index) values[from_index] |= from_mask & to_mask;
			else
			{
				values[from_index] |= from_mask;
				
				for (int i = from_index + 1; i < to_index; i++)
				     values[i] = FFFFFFFFFFFFFFFF;
				
				values[to_index] |= to_mask;
			}
		}
		
		
		public void set(int from_bit, int to_bit, boolean value) {
			if (value)
				set1(from_bit, to_bit);
			else
				set0(from_bit, to_bit);
		}
		
		
		public void set0(int bit) {
			if (size() <= bit) size = bit + 1;
			
			final int index = bit >> LEN;
			
			if (index < used())
				if (index + 1 == used && (values[index] &= ~(1L << bit)) == 0) used |= IO;
				else
					values[index] &= ~(1L << bit);
		}
		
		
		public void set0(int from_bit, int to_bit) {
			if (size() <= to_bit) size = to_bit + 1;
			
			if (from_bit == to_bit) return;
			
			int from_index = from_bit >> LEN;
			if (used() <= from_index) return;
			
			int to_index = to_bit - 1 >> LEN;
			if (used <= to_index)
			{
				to_bit   = last1();
				to_index = used - 1;
			}
			
			long from_mask = FFFFFFFFFFFFFFFF << from_bit;
			long to_mask   = FFFFFFFFFFFFFFFF >>> -to_bit;
			
			if (from_index == to_index)
			{
				if ((values[from_index] &= ~(from_mask & to_mask)) == 0) if (from_index + 1 == used) used |= IO;
			}
			else
			{
				values[from_index] &= ~from_mask;
				
				for (int i = from_index + 1; i < to_index; i++) values[i] = 0;
				
				values[to_index] &= ~to_mask;
				
				used |= IO;
			}
		}
		
		public void add(long src) {add(src, 64);}
		
		public void add(long src, int bits) {
			if (64 < bits) bits = 64;
			
			int _size = size;
			size += bits;
			
			if ((src &= ~(1L << bits - 1)) == 0) return;
			
			used(_size + BITS - Long.numberOfLeadingZeros(src));
			
			int bit = _size & 63;
			
			if (bit == 0) values[index(size)] = src;
			else
			{
				values[index(_size)] &= src << bit | mask(bit);
				if (index(_size) < index(size)) values[index(size)] = src >> bit;
			}
		}
		
		public void add(int key, boolean value) {
			if (key < last1())
			{
				int index = key >> LEN;
				
				long m = FFFFFFFFFFFFFFFF << key, v = values[index];
				
				m = (v & m) << 1 | v & ~m;
				
				if (value) m |= 1L << key;
				
				while (++index < used)
				{
					values[index - 1] = m;
					final int t = (int) (v >>> BITS - 1);
					v = values[index];
					m = v << 1 | t;
				}
				values[index - 1] = m;
				                    used |= IO;
			}
			else if (value)
			{
				final int index = used(key);  //!!!
				values[index] |= 1L << key;
			}
			
			size++;
		}
		
		public void clear() {
			for (used(); used > 0; ) values[--used] = 0;
			size = 0;
		}
		
		
		public void remove(int bit) {
			if (size <= bit) return;
			
			size--;
			
			int index = bit >> LEN;
			if (used() <= index) return;
			
			
			final int last = last1();
			if (bit == last) set0(bit);
			else if (bit < last)
			{
				long m = FFFFFFFFFFFFFFFF << bit, v = values[index];
				
				v = v >>> 1 & m | v & ~m;
				
				while (++index < used)
				{
					m = values[index];
					
					values[index - 1] = (m & 1) << BITS - 1 | v;
					v                 = m >>> 1;
				}
				values[index - 1] = v;
				                    used |= IO;
			}
		}
		
		
		public RW clone() {return (RW) super.clone();}
	}
}

