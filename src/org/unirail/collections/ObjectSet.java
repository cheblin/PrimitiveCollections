package org.unirail.collections;

public interface ObjectSet {
	
	interface IDst<V extends Comparable<? super V>> {
		boolean add(V key);
		
		void write(int size);
	}
	
	interface ISrc<K extends Comparable<? super K>> {
		
		int size();
		
		boolean read_has_null_key();
		
		int read(int info);
		
		K read_key(int info);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (read_has_null_key())
			{
				dst.append("null\n");
				size--;
			}
			
			for (int p = -1, i = 0; i < size; dst.append('\n'), i++)
				dst.append(read_key(p = read(p)));
			
			return dst;
		}
	}
	
	abstract class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>>, ISrc<K> {
		
		public ObjectList.RW<K> keys = new ObjectList.RW<>(4);
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNullKey;
		
		protected double loadFactor;
		
		
		public boolean contains(K key) {
			if (key == null) return hasNullKey;
			int slot = Array.hash(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0) return true;
			
			return false;
		}
		
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + (hasNullKey ? 1 : 0); }
		
		public int hashCode() {
			int h = hasNullKey ? 0xDEADBEEF : 0;
			K   key;
			
			for (int slot = mask; 0 <= slot; slot--)
				if ((key = keys.array[slot]) != null) h += Array.hash(key);
			
			return h;
		}
		
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R<K> other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R<K> other) {
			if (other == null) return -1;
			if (other.assigned != assigned) return other.assigned - assigned;
			if (other.hasNullKey != hasNullKey) return 1;
			
			for (K k : keys.array) if (k != null && !other.contains(k)) return 1;
			
			return 0;
		}
		
		@SuppressWarnings("unchecked")
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				dst.keys = keys.clone();
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			
			return null;
		}
		
		//region  ISrc
		
		@Override public boolean read_has_null_key() { return hasNullKey; }
		
		@Override public int read(int info)          { for (; ; ) if (keys.array[++info] != null) return info; }
		
		@Override public K read_key(int info)        {return keys.array[info]; }
		
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> implements IDst<K> {
		public RW()                  { this(4, 0.75f); }
		
		
		public RW(double loadFactor) { this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D); }
		
		
		public RW(int expectedItems) { this(expectedItems, 0.75f); }
		
		public RW(int expectedItems, double loadFactor) {
			this(loadFactor);
			
			long length = (long) Math.ceil(expectedItems / loadFactor);
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
			keys.length(size);
		}
		
		
		public boolean add(K key) {
			if (key == null) return !hasNullKey && (hasNullKey = true);
			
			int slot = Array.hash(key) & mask;
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0) return false;
			
			keys.array[slot] = key;
			if (assigned == resizeAt) this.allocate(mask + 1 << 1);
			
			assigned++;
			return true;
		}
		
		public void clear() {
			assigned = 0;
			hasNullKey = false;
			keys.clear();
		}
		
		//region  IDst
		@Override public void write(int size) {
			assigned = 0;
			hasNullKey = false;
			keys.write((int) Array.nextPowerOf2(size));
		}
		//endregion
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				return;
			}
			
			final K[] k = keys.array;
			keys.length(-size);
			
			K kk;
			for (int i = k.length - 1; --i >= 0; )
				if ((kk = k[i]) != null)
				{
					int slot = Array.hash(kk) & mask;
					
					while (keys.array[slot] != null)
						slot = slot + 1 & mask;
					
					keys.array[slot] = kk;
				}
			
		}
		
		
		public boolean remove(K key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = Array.hash(key) & mask;
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0)
				{
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys.array[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - Array.hash(kk) & mask) >= distance)
						{
							keys.array[gapSlot] = kk;
							gapSlot = slot1;
							distance = 0;
						}
					
					keys.array[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW<K> clone() { return (RW<K>) super.clone(); }
		
	}
}
	