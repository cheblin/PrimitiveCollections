package org.unirail.collections;

public interface UShortShortMap {
	
	interface Consumer {
		boolean put(char key, short value);
		
		boolean put( Character key, short value);
		
		void consume(int size);
	}
	
	interface Producer {
		
		int size();
		
		boolean produce_has_null_key();
		
		short produce_null_key_val();
		
		boolean produce_has_0key();
		
		short produce_0key_val();
		
		int produce(int state);
		
		char produce_key(int state);
		
		short produce_val(int state);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (produce_has_null_key())
			{
				dst.append("null -> ").append(produce_null_key_val()).append('\n');
				size--;
			}
			
			if (produce_has_0key())
			{
				dst.append("0 -> ").append(produce_0key_val()).append('\n');
				size--;
			}
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(produce_key(p = produce(p)))
						.append(" -> ")
						.append(produce_val(p))
						.append('\n');
			return dst;
		}
	}
	
	
	class R implements Cloneable, Comparable<R>, Producer {
		public UShortList.RW keys   = new UShortList.RW(0);
		public ShortList.RW values = new ShortList.RW(0);
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNullKey;
		short       nullKeyValue;
		
		boolean hasO;
		short       OkeyValue;
		
		protected double loadFactor;
		
		public R()                  { this(4); }
		
		
		public R(int expectedItems) { this(expectedItems, 0.75); }
		
		
		public R(int expectedItems, double loadFactor) {
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
		
		
		public @Positive_Values int info( Character key) { return key == null ? hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE : info((char) (key + 0));}
		
		public @Positive_Values int info(char key) {
			if (key == 0) return hasO ? Positive_Values.VALUE - 1 : Positive_Values.NONE;
			
			int slot = Array.hash(key) & mask;
			
			for (char key_ =  key , k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean hasValue(int info) {return -1 < info;}
		
		public boolean hasNone(int info)  {return info == Positive_Values.NONE;}
		
		public short value(@Positive_ONLY int info) {
			if (info == Positive_Values.VALUE) return nullKeyValue;
			if (info == Positive_Values.VALUE - 1) return OkeyValue;
			
			return (short) values.get(info);
		}
		
		public int hashCode() {
			int h = hasO ? 0xDEADBEEF : 0;
			
			char k;
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
			
			char           k;
			for (int i = keys.array.length - 1, c; -1 < --i; )
				if ((k =  keys.array[i]) != 0)
				{
					if ((c = other.info(k)) < 0) return 3;
					if (other.value(c) !=  (short) values.array[i]) return 1;
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
		
		@Override public boolean produce_has_null_key() {return hasNullKey;}
		
		@Override public short produce_null_key_val() {return nullKeyValue;}
		
		@Override public boolean produce_has_0key()     {return hasO; }
		
		@Override public short produce_0key_val() {return OkeyValue;}
		
		
		@Override public int produce(int state)         { for (; ; ) if (keys.array[++state] != 0) return state; }
		
		@Override public char produce_key(int state) {return   keys.array[state]; }
		
		@Override public short produce_val(int state) {return  (short) values.array[state]; }
		
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	
	class RW extends R implements Consumer {
		
		@Override public void consume(int size) {
			assigned = 0;
			hasO = false;
			hasNullKey = false;
			
			allocate((int) Array.nextPowerOf2(size));
		}
		
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
			
			final char[] k = keys.array;
			final short[] v = values.array;
			
			keys.length(-size);
			values.length(-size);
			
			char key;
			for (int i = k.length - 1; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = Array.hash(key) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] = key;
					values.array[slot] = v[i];
				}
		}
		
		@Override public boolean put( Character key, short value) {
			if (key != null) return put((char) key, value);
			
			hasNullKey = true;
			nullKeyValue = value;
			
			return true;
		}
		
		@Override public boolean put(char key, short value) {
			
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
			
			final char key_ =  key;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.array[slot] = (short) value;
					return true;
				}
			
			keys.array[slot] = key_;
			values.array[slot] = (short) value;
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return false;
		}
		
		
		public boolean remove( Character key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			return remove((char) key);
		}
		
		public boolean remove(char key) {
			
			if (key == 0) return hasO && !(hasO = false);
			
			int slot = Array.hash(key) & mask;
			
			final char key_ =  key;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					char kk;
					
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
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
