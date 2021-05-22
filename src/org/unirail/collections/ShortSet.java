package org.unirail.collections;

public interface ShortSet {
	
	interface IDst {
		boolean add(short key);
		
		boolean add( Short     key);
		
		void write(int size);
	}
	
	interface ISrc {
		boolean read_has_null_key();
		
		boolean read_has_0key();
		
		int size();
		
		int read(int info);
		
		short  read_key(int info);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (read_has_null_key())
			{
				dst.append("null\n");
				size--;
			}
			
			if (read_has_0key())
			{
				dst.append("0\n");
				size--;
			}
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(read_key(p = read(p))).append('\n');
			return dst;
		}
	}
	
	abstract class R implements Cloneable, Comparable<R>, ISrc {
		
		public ShortList.RW keys = new ShortList.RW(0);
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean hasOkey;
		boolean hasNullKey;
		
		protected double loadFactor;
		
		
		public boolean contains( Short     key) { return key == null ? hasNullKey : contains((short) (key + 0)); }
		
		public boolean contains(short key) {
			if (key == 0) return hasOkey;
			
			int slot = Array.hash(key) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty()    { return size() == 0; }
		
		
		@Override public int size() { return assigned + (hasOkey ? 1 : 0) + (hasNullKey ? 1 : 0); }
		
		
		public int hashCode() {
			int         h = hasOkey ? 0xDEADBEEF : 0;
			short k;
			
			for (int slot = mask; slot >= 0; slot--)
				if ((k = keys.array[slot]) != 0)
					h += Array.hash(k);
			
			return h;
		}
		
		
		public short[] toArray(short[] dst) {
			final int size = size();
			if (dst == null || dst.length < size) dst = new short[size];
			
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
			
			for (short k : keys.array) if (k != 0 && !other.contains(k)) return 1;
			
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
		
		//region  ISrc
		
		@Override public boolean read_has_null_key() { return hasNullKey; }
		
		@Override public boolean read_has_0key()     { return hasOkey; }
		
		@Override public int read(int info)          { for (; ; ) if (keys.array[++info] != 0) return info; }
		
		@Override public short read_key(int info) { return (short)  keys.array[info]; }
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	
	class RW extends R implements IDst {
		
		public RW(int expectedItems) { this(expectedItems, 0.75); }
		
		public RW(double loadFactor) { this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D); }
		
		
		public RW(int expectedItems, double loadFactor) {
			this(loadFactor);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
		}
		
		public RW(short... items) {
			this(items.length);
			for (short i : items) add(i);
		}
		
		@Override public boolean add( Short     key) { return key == null ? !hasNullKey && (hasNullKey = true) : add((short) key);}
		
		@Override public boolean add(short  key) {
			
			if (key == 0) return !hasOkey && (hasOkey = true);
			
			int slot = Array.hash(key) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return false;
			
			keys.array[slot] =  key;
			
			if (assigned++ == resizeAt) allocate(mask + 1 << 1);
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
			
			final short[] k = keys.array;
			
			keys.length(-size);
			
			short key;
			for (int from = k.length - 1; 0 <= --from; )
				if ((key = k[from]) != 0)
				{
					int slot = Array.hash(key) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] = key;
				}
		}
		
		
		public boolean remove( Short     key) { return key == null ? hasNullKey && !(hasNullKey = false) : remove((short) key); }
		
		public boolean remove(short key) {
			
			if (key == 0) return hasOkey && !(hasOkey = false);
			
			int slot = Array.hash(key) & mask;
			
			for (short k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					short kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Array.hash(kk) & mask) >= distance)
						{
							keys.array[gapSlot] =  kk;
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
		
		//region  IDst
		@Override public void write(int size) {
			assigned = 0;
			hasOkey = false;
			hasNullKey = false;
			keys.write((int) Array.nextPowerOf2(size));
		}
		//endregion
		
		public void retainAll(IDst chk) {
			short key;
			
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = keys.array[i]) != 0 && !chk.add(key)) remove(key);
			
			if (hasOkey && !chk.add((short) 0)) hasOkey = false;
		}
		
		public int removeAll(ISrc src) {
			int fix = size();
			
			for (int i = 0, s = src.size(), p = -1; i < s; i++) remove(src.read_key(p = src.read(p)));
			
			return fix - size();
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}