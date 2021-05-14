package org.unirail.collections;

public interface ByteSet {
	
	interface Consumer {
		
		default boolean add(int value) { return add((byte) (value & 0xFF)); }
		
		boolean add(byte value);
		
		boolean add( Byte      key);
		
		void consume(int items);
	}
	
	interface Producer {
		boolean produce_has_null_key();
		
		int size();
		
		int produce(int info);
		
		byte  produce_key(int info);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (produce_has_null_key())
			{
				dst.append("null\n");
				size--;
			}
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(produce_key(p = produce(p))).append('\n');
			
			return dst;
		}
	}
	
	class R implements Cloneable, Comparable<R>, Producer {
		long
				_1,
				_2,
				_3,
				_4;
		
		int size = 0;
		
		
		protected boolean hasNullKey;
		
		
		public R(byte... items) { for (byte i : items) add(this, i); }
		
		protected static boolean add(ByteSet.R dst, final byte value) {
			
			final int val = value & 0xFF;
			
			if (val < 128)
				if (val < 64)
					if ((dst._1 & 1L << val) == 0) dst._1 |= 1L << val;
					else return false;
				else if ((dst._2 & 1L << val - 64) == 0) dst._2 |= 1L << val - 64;
				else return false;
			else if (val < 192)
				if ((dst._3 & 1L << val - 128) == 0) dst._3 |= 1L << val - 128;
				else return false;
			else if ((dst._4 & 1L << val - 192) == 0) dst._4 |= 1L << val - 192;
			else return false;
			
			dst.size++;
			return true;
		}
		
		
		public int size()                         { return hasNullKey ? size + 1 : size; }
		
		public boolean isEmpty()                  { return size < 1; }
		
		public boolean contains( Byte      key) { return key == null ? hasNullKey : contains((byte) (key + 0)); }
		
		public boolean contains(int key) {
			if (size() == 0) return false;
			
			final int val = key & 0xFF;
			return
					(
							val < 128 ?
							val < 64 ? _1 & 1L << val : _2 & 1L << val - 64 :
							val < 192 ? _3 & 1L << val - 128 : _4 & 1L << val - 192
					) != 0;
		}
		
		
		//Rank returns the number of integers that are smaller or equal to x
		//return inversed value if key does not exists
		protected int rank(byte key) {
			final int val = key & 0xFF;
			
			int  ret  = 0;
			long l    = _1;
			int  base = 0;
a:
			{
				if (val < 64) break a;
				if (l != 0) ret += Long.bitCount(l);
				base = 64;
				l = _2;
				if (val < 128) break a;
				if (l != 0) ret += Long.bitCount(l);
				base = 128;
				l = _3;
				if (val < 192) break a;
				if (l != 0) ret += Long.bitCount(l);
				base = 192;
				l = _4;
			}
			
			while (l != 0)
			{
				final int s = Long.numberOfTrailingZeros(l);
				
				if ((base += s) == val) return ret + 1;
				if (val < base) return ~ret;
				
				ret++;
				l >>>= s + 1;
			}
			
			return ~ret;
		}
		
		public boolean containsAll(R ask) {
			
			if (hasNullKey != ask.hasNullKey) return false;
			
			int  i;
			long l;
			
			for (i = 0, l = _1; l != 0; l >>>= ++i)
				if (!ask.contains((byte) (i += Long.numberOfTrailingZeros(l)))) return false;
			
			for (i = 0, l = _2; l != 0; l >>>= ++i)
				if (!ask.contains((byte) (i += Long.numberOfTrailingZeros(l) + 64))) return false;
			
			for (i = 0, l = _3; l != 0; l >>>= ++i)
				if (!ask.contains((byte) (i += Long.numberOfTrailingZeros(l) + 128))) return false;
			
			for (i = 0, l = _4; l != 0; l >>>= ++i)
				if (!ask.contains((byte) (i += Long.numberOfTrailingZeros(l) + 192))) return false;
			
			return true;
		}
		
		public boolean containsAll(Producer src) {
			if (src.produce_has_null_key() != hasNullKey || size() != src.size()) return false;
			
			for (int p = src.produce(-1), i = 0, size = src.size(); i < size; i++, p = src.produce(p))
				if (!contains(src.produce_key(p))) return false;
			return true;
		}
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R other) {
			
			return
					size == other.size ?
					(int) (_1 == other._1 ?
					       _2 == other._2 ?
					       _3 == other._3 ?
					       _4 == other._4 ? 0 : _4 - other._4 : _3 - other._3 : _2 - other._2 : _1 - other._1) : size - other.size;
		}
		
		public R clone() {
			try
			{
				return (R) super.clone();
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			return null;
		}
		
		//region  producer
		@Override public boolean produce_has_null_key() { return hasNullKey; }
		
		public byte produce_key(int info) { return (byte) (info & 0xFF); }
		
		public int produce(int info) {
			info++;
			info &= 0xFF;
			long l;
			if (info < 128)
			{
				if (info < 64)
				{
					if ((l = _1 >>> info) != 0) return (info + Long.numberOfTrailingZeros(l));
					info = 0;
				}
				else info -= 64;
				
				if ((l = _2 >>> info) != 0) return ((info + Long.numberOfTrailingZeros(l) + 64));
				info = 128;
			}
			
			if (info < 192)
			{
				if ((l = _3 >>> (info - 128)) != 0) return ((info + Long.numberOfTrailingZeros(l) + 128));
				
				info = 0;
			}
			else info -= 192;
			
			if ((l = _4 >>> info) != 0) return ((info + Long.numberOfTrailingZeros(l) + 192));
			
			return -1;
		}
		
		//endregion
		public String toString() { return toString(null).toString(); }
	}
	
	class RW extends R implements Consumer {
		
		@Override public void consume(int size) { clear(); }
		
		public boolean add( Byte      key)    { return key == null ? !hasNullKey && (hasNullKey = true) : R.add(this, (byte) (key + 0));}
		
		public boolean add(byte value)  { return R.add(this, value); }
		
		public boolean retainAll(R src) {
			boolean ret = false;
			
			if (_1 != src._1)
			{
				_1 &= src._1;
				ret = true;
			}
			if (_2 != src._2)
			{
				_2 &= src._2;
				ret = true;
			}
			if (_3 != src._3)
			{
				_3 &= src._3;
				ret = true;
			}
			if (_4 != src._4)
			{
				_4 &= src._4;
				ret = true;
			}
			
			if (ret) size = Long.bitCount(_1) + Long.bitCount(_2) + Long.bitCount(_3) + Long.bitCount(_4);
			return ret;
		}
		
		
		public boolean remove( Byte      key) { return key == null ? hasNullKey && !(hasNullKey = false) : remove((byte) (key + 0)); }
		
		public boolean remove(byte value) {
			if (size == 0) return false;
			
			final int val = value & 0xFF;
			
			if (val < 128)
				if (val < 64)
					if ((_1 & 1L << val) == 0) return false;
					else _1 &= ~(1L << val);
				else if ((_2 & 1L << val - 64) == 0) return false;
				else _2 &= ~(1L << val - 64);
			else if (val < 192)
				if ((_3 & 1L << val - 128) == 0) return false;
				else _3 &= ~(1L << val - 128);
			else if ((_4 & 1L << val - 192) == 0) return false;
			else _4 &= ~(1L << val - 192);
			
			size--;
			return true;
		}
		
		
		public boolean retainAll(Producer src) {
			long
					_1 = 0,
					_2 = 0,
					_3 = 0,
					_4 = 0;
			
			for (int p = src.produce(-1), i = 0, size = src.size(); i < size; i++, p = src.produce(p))
			{
				final int val = src.produce_key(p) & 0xFF;
				
				if (val < 128)
					if (val < 64) _1 |= 1L << val;
					else _2 |= 1L << val - 64;
				else if (val < 192) _3 |= 1L << val - 128;
				else _4 |= 1L << val - 192;
			}
			
			boolean ret = false;
			
			if (_1 != this._1)
			{
				this._1 &= _1;
				ret = true;
			}
			if (_2 != this._2)
			{
				this._2 &= _2;
				ret = true;
			}
			if (_3 != this._3)
			{
				this._3 &= _3;
				ret = true;
			}
			if (_4 != this._4)
			{
				this._4 &= _4;
				ret = true;
			}
			
			if (ret) size = (_1 == 0 ? 0 : Long.bitCount(_1)) + (_2 == 0 ? 0 : Long.bitCount(_2)) + (_3 == 0 ? 0 : Long.bitCount(_3)) + (_4 == 0 ? 0 : Long.bitCount(_4));
			return ret;
		}
		
		public boolean removeAll(Producer src) {
			boolean ret = false;
			
			for (int p = src.produce(-1), i = 0, size = src.size(); i < size; i++, p = src.produce(p))
			{
				remove(src.produce_key(p));
				ret = true;
			}
			return ret;
		}
		
		public void clear() {
			_1 = 0;
			_2 = 0;
			_3 = 0;
			_4 = 0;
			size = 0;
			hasNullKey = false;
		}
		
		public boolean addAll(Producer src) {
			boolean      ret = false;
			byte val;
			
			for (int p = src.produce(-1), i = 0, size = src.size(); i < size; i++, p = src.produce(p))
				if (!contains(val = src.produce_key(p)))
				{
					ret = true;
					R.add(this, val);
				}
			
			return ret;
		}
		
		public RW clone() { return (RW) super.clone(); }
	}
}
