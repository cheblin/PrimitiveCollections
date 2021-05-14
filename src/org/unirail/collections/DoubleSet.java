package org.unirail.collections;

public interface DoubleSet {
	
	interface Consumer {
		boolean add(double value);
		
		boolean add( Double    key);
		
		void consume(int items);
	}
	
	interface Producer {
		boolean produce_has_null_key();
		
		boolean produce_has_0key();
		
		int size();
		
		int produce(int state);
		
		double  produce_key(@Positive_Values int state);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (produce_has_null_key())
			{
				dst.append("null\n");
				size--;
			}
			
			if (produce_has_0key())
			{
				dst.append("0\n");
				size--;
			}
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(produce_key(p = produce(p))).append('\n');
			return dst;
		}
	}
	
	class R implements Cloneable, Comparable<R>, Producer {
		
		public DoubleList.RW keys = new DoubleList.RW(0);
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean hasOkey;
		boolean hasNullKey;
		
		protected double loadFactor;
		
		
		protected R(int expectedItems) { this(expectedItems, 0.75); }
		
		protected R(double loadFactor) { this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D); }
		
		
		protected R(int expectedItems, double loadFactor) {
			this(loadFactor);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
		}
		
		public R(double... items) {
			this(items.length);
			for (double i : items) add(this, i);
		}
		
		protected static boolean add(R src, double key) {
			
			if (key == 0) return !src.hasOkey && (src.hasOkey = true);
			
			int slot = Array.hash(key) & src.mask;
			
			for (double k; (k = src.keys.array[slot]) != 0; slot = slot + 1 & src.mask)
				if (k == key) return false;
			
			src.keys.array[slot] = Double.doubleToLongBits( key);
			
			if (src.assigned++ == src.resizeAt) src.allocate(src.mask + 1 << 1);
			return true;
		}
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				else keys.clear();
				
				return;
			}
			
			final double[] k = keys.array;
			
			keys.length(-size);
			
			double key;
			for (int from = k.length - 1; 0 <= --from; )
				if ((key = k[from]) != 0)
				{
					int slot = Array.hash(key) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] =Double.doubleToLongBits( key);
				}
		}
		
		public boolean contains( Double    key) { return key == null ? hasNullKey : contains((double) (key + 0)); }
		
		public boolean contains(double key) {
			if (key == 0) return hasOkey;
			
			int slot = Array.hash(key) & mask;
			
			for (double k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty()    { return size() == 0; }
		
		
		@Override public int size() { return assigned + (hasOkey ? 1 : 0) + (hasNullKey ? 1 : 0); }
		
		
		public int hashCode() {
			int         h = hasOkey ? 0xDEADBEEF : 0;
			double k;
			
			for (int slot = mask; slot >= 0; slot--)
				if ((k = keys.array[slot]) != 0)
					h += Array.hash(k);
			
			return h;
		}
		
		
		
		public double[] toArray(double[] dst) {
			final int size = size();
			if (dst == null || dst.length < size) dst = new double[size];
			
			for (int i = keys.array.length - 1, ii = 0; 0 <= i; i--)
				if (keys.array[i] != 0) dst[ii++] = keys.array[i];
			
			return dst;
		}
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		@Override public int compareTo(R other) {
			if (other == null) return -1;
			if (other.assigned != assigned) return other.assigned - assigned;
			
			for (double k : keys.array) if (k != 0 && !other.contains(k)) return 1;
			
			return 0;
		}
		
		
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				
				dst.keys = keys.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		//region  producer
		
		@Override public boolean produce_has_null_key() { return hasNullKey; }
		
		@Override public boolean produce_has_0key()     { return hasOkey; }
		
		@Override public int produce(int state)         { for (; ; ) if (keys.array[++state] != 0) return state; }
		
		@Override public double produce_key(int state) { return   keys.array[state]; }
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	
	class RW extends R implements Consumer {
		
		public RW(int expectedItems)                      { super(expectedItems); }
		
		public RW(double loadFactor)                      { super(loadFactor); }
		
		public RW(int expectedItems, double loadFactor)   { super(expectedItems, loadFactor); }
		
		public RW(double... items)                   { super(items); }
		
		@Override public boolean add( Double    key) { return key == null ? !hasNullKey && (hasNullKey = true) : add((double) key);}
		
		@Override public boolean add(double  value)  { return R.add(this, value); }
		
		@Override public void consume(int items) {
			assigned = 0;
			hasOkey = false;
			hasNullKey = false;
			allocate((int) Array.nextPowerOf2(items));
		}
		
		public boolean remove( Double    key) { return key == null ? hasNullKey && !(hasNullKey = false) : remove((double) key); }
		
		public boolean remove(double key) {
			
			if (key == 0) return hasOkey && !(hasOkey = false);
			
			int slot = Array.hash(key) & mask;
			
			for (double k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					double kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Array.hash(kk) & mask) >= distance)
						{
							keys.array[gapSlot] = Double.doubleToLongBits( kk);
							gapSlot = s;
							distance = 0;
						}
					
					keys.array[gapSlot] = 0;
					assigned--;
					return true;
				}
			return false;
		}
		
		public void clear() {
			assigned = 0;
			hasOkey = false;
			hasNullKey = false;
			
			keys.clear();
		}
		
		public void retainAll(Consumer chk) {
			double key;
			
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = keys.array[i]) != 0 && !chk.add(key)) remove(key);
			
			if (hasOkey && !chk.add((double) 0)) hasOkey = false;
		}
		
		public int removeAll(DoubleList.Producer src) {
			int fix = size();
			
			for (int i = 0, s = src.size(); i < s; i++) remove(src.get(i));
			
			return fix - size();
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}