package org.unirail.collections;

public interface FloatSet {
	
	interface Consumer {
		boolean add(float key);
		
		boolean add( Float     key);
		
		void consume(int items);
	}
	
	interface Producer {
		boolean produce_has_null_key();
		
		boolean produce_has_0key();
		
		int size();
		
		int produce(int info);
		
		float  produce_key( int info);
		
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
		
		public FloatList.RW keys = new FloatList.RW(0);
		
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
		
		public R(float... items) {
			this(items.length);
			for (float i : items) add(this, i);
		}
		
		protected static boolean add(R src, float key) {
			
			if (key == 0) return !src.hasOkey && (src.hasOkey = true);
			
			int slot = Array.hash(key) & src.mask;
			
			for (float k; (k = src.keys.array[slot]) != 0; slot = slot + 1 & src.mask)
				if (k == key) return false;
			
			src.keys.array[slot] = Float.floatToIntBits( key);
			
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
			
			final float[] k = keys.array;
			
			keys.length(-size);
			
			float key;
			for (int from = k.length - 1; 0 <= --from; )
				if ((key = k[from]) != 0)
				{
					int slot = Array.hash(key) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] =Float.floatToIntBits( key);
				}
		}
		
		public boolean contains( Float     key) { return key == null ? hasNullKey : contains((float) (key + 0)); }
		
		public boolean contains(float key) {
			if (key == 0) return hasOkey;
			
			int slot = Array.hash(key) & mask;
			
			for (float k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty()    { return size() == 0; }
		
		
		@Override public int size() { return assigned + (hasOkey ? 1 : 0) + (hasNullKey ? 1 : 0); }
		
		
		public int hashCode() {
			int         h = hasOkey ? 0xDEADBEEF : 0;
			float k;
			
			for (int slot = mask; slot >= 0; slot--)
				if ((k = keys.array[slot]) != 0)
					h += Array.hash(k);
			
			return h;
		}
		
		
		
		public float[] toArray(float[] dst) {
			final int size = size();
			if (dst == null || dst.length < size) dst = new float[size];
			
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
			
			for (float k : keys.array) if (k != 0 && !other.contains(k)) return 1;
			
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
		
		@Override public int produce(int info)          { for (; ; ) if (keys.array[++info] != 0) return info; }
		
		@Override public float produce_key(int info) { return   keys.array[info]; }
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	
	class RW extends R implements Consumer {
		
		public RW(int expectedItems)                      { super(expectedItems); }
		
		public RW(double loadFactor)                      { super(loadFactor); }
		
		public RW(int expectedItems, double loadFactor)   { super(expectedItems, loadFactor); }
		
		public RW(float... items)                   { super(items); }
		
		@Override public boolean add( Float     key) { return key == null ? !hasNullKey && (hasNullKey = true) : add((float) key);}
		
		@Override public boolean add(float  key)    { return R.add(this, key); }
		
		@Override public void consume(int items) {
			assigned = 0;
			hasOkey = false;
			hasNullKey = false;
			allocate((int) Array.nextPowerOf2(items));
		}
		
		public boolean remove( Float     key) { return key == null ? hasNullKey && !(hasNullKey = false) : remove((float) key); }
		
		public boolean remove(float key) {
			
			if (key == 0) return hasOkey && !(hasOkey = false);
			
			int slot = Array.hash(key) & mask;
			
			for (float k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					float kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Array.hash(kk) & mask) >= distance)
						{
							keys.array[gapSlot] = Float.floatToIntBits( kk);
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
			float key;
			
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = keys.array[i]) != 0 && !chk.add(key)) remove(key);
			
			if (hasOkey && !chk.add((float) 0)) hasOkey = false;
		}
		
		public int removeAll(FloatList.Producer src) {
			int fix = size();
			
			for (int i = 0, s = src.size(); i < s; i++) remove(src.value(i));
			
			return fix - size();
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}