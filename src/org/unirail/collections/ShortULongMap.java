package org.unirail.collections;

public interface ShortULongMap {
	
	interface Consumer {
		boolean put(short key, long value);
		
		boolean put( Short     key, long value);
		
		void write(int size);
	}
	
	interface Producer {
		
		int size();
		
		boolean read_has_null_key();
		
		long read_null_key_val();
		
		boolean read_has_0key();
		
		long read_0key_val();
		
		int read(int info);
		
		short read_key(int info);
		
		long read_val(int info);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (read_has_null_key())
			{
				dst.append("null -> ").append(read_null_key_val()).append('\n');
				size--;
			}
			
			if (read_has_0key())
			{
				dst.append("0 -> ").append(read_0key_val()).append('\n');
				size--;
			}
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(read_key(p = read(p)))
						.append(" -> ")
						.append(read_val(p))
						.append('\n');
			return dst;
		}
	}
	
	
	class R implements Cloneable, Comparable<R>, Producer {
		public ShortList.RW keys   = new ShortList.RW(0);
		public ULongList.RW values = new ULongList.RW(0);
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNullKey;
		long       nullKeyValue;
		
		boolean hasO;
		long       OkeyValue;
		
		protected double loadFactor;
		
		
		protected R(int expectedItems) { this(expectedItems, 0.75); }
		
		
		protected R(int expectedItems, double loadFactor) {
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(size - 1, (int) Math.ceil(size * loadFactor));
			mask = size - 1;
			
			keys.length(size);
			values.length(size);
		}
		
		public boolean isEmpty()                              { return size() == 0; }
		
		@Override public int size()                           { return assigned + (hasO ? 1 : 0) + (hasNullKey ? 1 : 0); }
		
		
		public @Positive_Values int info( Short     key) { return key == null ? hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE : info((short) (key + 0));}
		
		public @Positive_Values int info(short key) {
			if (key == 0) return hasO ? Positive_Values.VALUE - 1 : Positive_Values.NONE;
			
			int slot = Array.hash(key) & mask;
			
			for (short key_ =  key , k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean hasValue(int info) {return -1 < info;}
		
		public boolean hasNone(int info)  {return info == Positive_Values.NONE;}
		
		public long value(@Positive_ONLY int info) {
			if (info == Positive_Values.VALUE) return nullKeyValue;
			if (info == Positive_Values.VALUE - 1) return OkeyValue;
			
			return  values.value(info);
		}
		
		public int hashCode() {
			int h = hasO ? 0xDEADBEEF : 0;
			
			short k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash(k) + Array.hash(values.array[i]);
			
			return h;
		}
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		@Override public int compareTo(R other) {
			if (other == null) return -1;
			
			if (hasO != other.hasO || hasNullKey != other.hasNullKey) return 1;
			
			int diff;
			if (hasO && (OkeyValue - other.OkeyValue) != 0) return 7;
			if (hasNullKey && (nullKeyValue - other.nullKeyValue) != 0) return 8;
			
			if ((diff = size() - other.size()) != 0) return diff;
			
			short           k;
			for (int i = keys.array.length - 1, c; -1 < --i; )
				if ((k = (short) keys.array[i]) != 0)
				{
					if ((c = other.info(k)) < 0) return 3;
					if (other.value(c) !=   values.array[i]) return 1;
				}
			
			return 0;
		}
		
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				
				dst.keys = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		//region  producer
		
		@Override public boolean read_has_null_key() {return hasNullKey;}
		
		@Override public long read_null_key_val() {return nullKeyValue;}
		
		@Override public boolean read_has_0key()     {return hasO; }
		
		@Override public long read_0key_val() {return OkeyValue;}
		
		
		@Override public int read(int info)          { for (; ; ) if (keys.array[++info] != 0) return info; }
		
		@Override public short read_key(int info) {return  (short) keys.array[info]; }
		
		@Override public long read_val(int info) {return   values.array[info]; }
		
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	
	class RW extends R implements Consumer {
		
		public RW(int expectedItems)                    { super(expectedItems); }
		
		public RW(int expectedItems, double loadFactor) { super(expectedItems, loadFactor); }
		
		
		
		
		@Override public boolean put( Short     key, long value) {
			if (key != null) return put((short) key, value);
			
			hasNullKey = true;
			nullKeyValue = value;
			
			return true;
		}
		
		@Override public boolean put(short key, long value) {
			
			if (key == 0)
			{
				if (hasO)
				{
					OkeyValue = value;
					return true;
				}
				hasO = true;
				OkeyValue = value;
				return false;
			}
			
			
			int slot = Array.hash(key) & mask;
			
			final short key_ =  key;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.array[slot] = (long) value;
					return true;
				}
			
			keys.array[slot] = key_;
			values.array[slot] = (long) value;
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return false;
		}
		
		
		public boolean remove( Short     key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			return remove((short) key);
		}
		
		public boolean remove(short key) {
			
			if (key == 0) return hasO && !(hasO = false);
			
			int slot = Array.hash(key) & mask;
			
			final short key_ =  key;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					short kk;
					
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Array.hash(kk) & mask) >= distance)
						{
							
							keys.array[gapSlot] = kk;
							values.array[gapSlot] = values.array[s];
							gapSlot = s;
							distance = 0;
						}
					
					keys.array[gapSlot] = 0;
					values.array[gapSlot] = 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			hasO = false;
			hasNullKey = false;
			
			keys.clear();
			values.clear();
		}
		//region  consumer
		@Override public void write(int size) {
			assigned = 0;
			hasO = false;
			hasNullKey = false;
			
			keys.write(size = (int) Array.nextPowerOf2(size));
			values.write(size);
		}
		
		//endregion
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				
				if (keys.length() < size) keys.length(-size);
				else keys.clear();
				
				if (values.length() < size) values.length(-size);
				else values.clear();
				
				return;
			}
			
			final short[] k = keys.array;
			final long[] v = values.array;
			
			keys.length(-size);
			values.length(-size);
			
			short key;
			for (int i = k.length - 1; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = Array.hash(key) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] = key;
					values.array[slot] = v[i];
				}
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
