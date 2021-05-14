package org.unirail.collections;

public interface LongDoubleNullMap {
	interface Consumer {
		boolean put(long key, double value);
		
		boolean put(long key,  Double    value);
		
		boolean put( Long      key,  Double    value);
		
		boolean put( Long      key, double value);
		
		int consume(int items);
	}
	
	
	interface Producer {
		
		int size();
		
		@Positive_Values int produce_has_null_key();
		
		double produce_null_key_val();
		
		@Positive_Values int produce_has_0key();
		
		double produce_0key_val();
		
		
		long produce_key(int info);
		
		@Positive_YES int produce_has_val(int info);
		
		double produce_val(@Positive_ONLY int info);
		
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			switch (produce_has_null_key())
			{
				case Positive_Values.VALUE:
					dst.append("null -> ").append(produce_null_key_val()).append('\n');
					size--;
					break;
				case Positive_Values.NULL:
					dst.append("null -> null\n");
					size--;
			}
			
			switch (produce_has_0key())
			{
				case Positive_Values.VALUE:
					dst.append("0 -> ").append(produce_0key_val()).append('\n');
					size--;
					break;
				case Positive_Values.NULL:
					dst.append("0 -> null\n");
					size--;
			}
			
			for (int p = -1, i = 0; i < size; i++, dst.append('\n'))
			{
				dst.append(produce_key(p = produce_has_val(p))).append(" -> ");
				
				if (p < 0) dst.append("null");
				else dst.append(produce_val(p));
			}
			
			return dst;
		}
	}
	
	class R implements Cloneable, Comparable<R>, Producer {
		
		
		public LongList.RW     keys   = new LongList.RW(0);
		public DoubleNullList.RW values = new DoubleNullList.RW(0);
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		double nullKeyValue = 0;
		
		
		@Positive_Values int hasOkey = Positive_Values.NONE;
		double       OkeyValue;
		
		
		protected double loadFactor;
		
		
		protected R(int expectedItems) { this(expectedItems, 0.75); }
		
		
		protected R(int expectedItems, double loadFactor) {
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
			values.nulls.length(size);
			values.values.length(size);
		}
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + (hasOkey == Positive_Values.NONE ? 0 : 1) + (hasNullKey == Positive_Values.NONE ? 0 : 1); }
		
		
		public int hashCode() {
			int h = hasOkey == Positive_Values.NONE ? 0xDEADBEEF : 0;
			
			long k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash(  k ) + Array.hash(values.hashCode());
			
			return h;
		}
		
		public @Positive_Values int info( Long      key) {return key == null ? hasNullKey : info((long) (key + 0));}
		
		public @Positive_Values int info(long key) {
			
			if (key == 0) return hasOkey == Positive_Values.VALUE ? Positive_Values.VALUE - 1 : hasOkey;
			
			final long key_ =  key ;
			
			int slot = Array.hash(key) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return (values.hasValue(slot)) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;//the key is not present
		}
		
		public boolean hasValue(int info) {return -1 < info;}
		
		public boolean hasNone(int info)  {return info == Positive_Values.NONE;}
		
		public boolean hasNull(int info)  {return info == Positive_Values.NULL;}
		
		public double value(@Positive_ONLY int info) {
			if (info == Positive_Values.VALUE) return nullKeyValue;
			if (info == Positive_Values.VALUE - 1) return OkeyValue;
			return values.value(info);
		}
		
		
		public @Positive_Values int hasNullKey() { return hasNullKey; }
		
		public double nullKeyValue() { return nullKeyValue; }
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R other) {
			if (other == null) return -1;
			
			if (hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && nullKeyValue != other.nullKeyValue) return 1;
			
			if (hasOkey != other.hasOkey || hasOkey == Positive_Values.VALUE && OkeyValue != other.OkeyValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			long           key;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key =  keys.array[i]) != 0)
					if (values.nulls.value(i))
					{
						int ii = other.info(key);
						if (ii < 0 || values.value(i) != other.value(ii)) return 1;
					}
					else if (-1 < other.info(key)) return 1;
			
			return 0;
		}
		
		
		public R clone() {
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
		
		@Override public @Positive_Values int produce_has_null_key() {return hasNullKey;}
		
		@Override public double produce_null_key_val() {return nullKeyValue;}
		
		@Override public @Positive_Values int produce_has_0key()     {return hasOkey; }
		
		@Override public double produce_0key_val() {return OkeyValue;}
		
		@Override public long produce_key(int info) {return   keys.array[info & Integer.MAX_VALUE]; }
		
		@Override public @Positive_YES int produce_has_val(int info) {
			for (info++, info &= Integer.MAX_VALUE; keys.array[info] == 0; info++) ;
			return values.hasValue(info) ? info : info | Integer.MIN_VALUE;
		}
		
		@Override public double produce_val(@Positive_ONLY int info) {return   values.value(info); }
		
		//endregion
		
		
		public String toString() { return toString(null).toString();}
	}
	
	class RW extends R implements Consumer {
		
		
		public RW(int expectedItems)                    { super(expectedItems); }
		
		public RW(int expectedItems, double loadFactor) { super(expectedItems, loadFactor); }
		
		
		public boolean put( Long      key, double value) {
			if (key != null) return put((long) (key + 0), value);
			
			hasNullKey = Positive_Values.VALUE;
			nullKeyValue = value;
			return true;
		}
		
		public boolean put( Long      key,  Double    value) {
			if (key != null) return put((long) (key + 0), value);
			
			int h = hasNullKey;
			
			if (value == null)
				hasNullKey = Positive_Values.NULL;
			else
			{
				hasNullKey = Positive_Values.VALUE;
				nullKeyValue = value;
			}
			
			return h == Positive_Values.NONE;
		}
		
		public boolean put(long key,  Double    value) {
			if (value != null) return put(key, (double) value);
			
			if (key == 0)
			{
				int h = hasOkey;
				hasOkey = Positive_Values.NULL;
				return h == Positive_Values.NONE;
			}
			
			int               slot = Array.hash(key) & mask;
			final long key_ =  key ;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set(slot, ( Double   ) null);
					return false;
				}
			
			keys.array[slot] = key_;
			values.set(slot, ( Double   ) null);
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean put(long key, double value) {
			if (key == 0)
			{
				int h = hasOkey;
				hasOkey = Positive_Values.VALUE;
				OkeyValue = value;
				return h == Positive_Values.NONE;
			}
			
			int slot = Array.hash(key) & mask;
			
			final long key_ =  key ;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set(slot, value);
					return true;
				}
			
			keys.array[slot] = key_;
			values.set(slot, value);
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean remove() { return hasOkey != Positive_Values.NONE && (hasOkey = Positive_Values.NONE) == Positive_Values.NONE; }
		
		public boolean remove(long key) {
			if (key == 0) return remove();
			
			int slot = Array.hash(key) & mask;
			
			final long key_ =  key ;
			
			final long[] array = keys.array;
			for (long k; (k = array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					long kk;
					
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Array.hash(kk) & mask) >= distance)
						{
							
							array[gapSlot] = kk;
							
							if (values.nulls.value(s))
								values.set(gapSlot, values.value(s));
							else
								values.set(gapSlot, ( Double   ) null);
							
							gapSlot = s;
							distance = 0;
						}
					
					array[gapSlot] = 0;
					values.set(gapSlot, ( Double   ) null);
					assigned--;
					return true;
				}
			return false;
		}
		
		@Override public int consume(int items) {
			items = (int) Array.nextPowerOf2(items);
			if (keys.length() < items) allocate(items);
			return items;
		}
		
		void allocate(int size) {
			
			if (assigned < 1)
			{
				resizeAt = Math.min(size - 1, (int) Math.ceil(size * loadFactor));
				mask = size - 1;
				
				if (keys.length() < size) keys.length(-size);
				else keys.clear();
				
				if (values.nulls.length() < size) values.nulls.length(-size);
				else values.nulls.clear();
				
				if (values.values.length() < size) values.values.length(-size);
				else values.values.clear();
				
				return;
			}
			
			RW tmp = new RW(size - 1, loadFactor);
			
			long[] array = keys.array;
			long   key;
			
			for (int i = array.length - 1; -1 < --i; )
				if ((key = array[i]) != 0)
					if (values.nulls.value(i)) tmp.put( key, values.value(i));
					else tmp.put( key, null);
			
			keys = tmp.keys;
			values = tmp.values;
			
			assigned = tmp.assigned;
			mask = tmp.mask;
			resizeAt = tmp.resizeAt;
		}
		
		public void clear() {
			assigned = 0;
			hasOkey = Positive_Values.NONE;
			keys.clear();
			values.clear();
		}
		
		public RW clone() { return (RW) super.clone(); }
	}
}
