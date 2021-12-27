package org.unirail.collections;


import static org.unirail.collections.Array.HashEqual.hash;

public interface ObjectSet {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static <K> int token(R<K> src, int token) {
			for (; ; )
				if (++token == src.keys.array.length) return INIT;
				else if (src.keys.array[token] != null) return token;
		}
		
		static <K> K key(R<K> src, int token) {return src.keys.array[token];}
	}
	
	
	abstract class R<K> implements Cloneable {
		
		public ObjectList.RW<K> keys;
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNullKey;
		
		protected double loadFactor;
		
		protected final Array.HashEqual<K> hash_equal;
		protected R(Class<K[]> clazz) {hash_equal = Array.HashEqual.get(clazz);}
		
		
		public boolean contains(K key) {
			if (key == null) return hasNullKey;
			int slot = hash_equal.hashCode(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (hash_equal.equals(k, key)) return true;
			
			return false;
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (hasNullKey ? 1 : 0);}
		
		public int hashCode() {
			int hash = hash(hasNullKey ? 10153331 : 888888883);
			for (int token = NonNullKeysIterator.INIT, h = 888888883; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				hash = hash(hash, hash_equal.hashCode(NonNullKeysIterator.key(this, token)));
			return hash;
		}
		
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		public boolean equals(R<K> other) {
			if (other == null || other.assigned != assigned || other.hasNullKey != hasNullKey) return false;
			
			for (K k : keys.array) if (k != null && !other.contains(k)) return false;
			
			return true;
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
		
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (hasNullKey) dst.append("null\n");
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; dst.append('\n'))
				dst.append(NonNullKeysIterator.key(this, token));
			
			return dst;
		}
	}
	
	class RW<K> extends R<K> {
		
		
		public RW(Class<K[]> clazz, int expectedItems) {this(clazz, expectedItems, 0.75f);}
		
		public RW(Class<K[]> clazz, int expectedItems, double loadFactor) {
			super(clazz);
			
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			long length = (long) Math.ceil(expectedItems / loadFactor);
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys = new ObjectList.RW<>(clazz,size);
		}
		
		@SafeVarargs public RW(Class<K[]> clazz, K... items) {
			this(clazz, items.length);
			for (K key : items) add(key);
		}
		
		public boolean add(K key) {
			if (key == null) return !hasNullKey && (hasNullKey = true);
			
			int slot = hash_equal.hashCode(key) & mask;
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (!hash_equal.equals(k, key)) return false;
			
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
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				return;
			}
			
			final K[] k = keys.array;
			keys.length(-size);
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = hash_equal.hashCode(key) & mask;
					
					while (keys.array[slot] != null) slot = slot + 1 & mask;
					
					keys.array[slot] = key;
				}
			
		}
		
		
		public boolean remove(K key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = hash_equal.hashCode(key) & mask;
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (hash_equal.equals(k, key))
				{
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys.array[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - hash_equal.hashCode(kk) & mask) >= distance)
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
		
		public RW<K> clone() {return (RW<K>) super.clone();}
		
	}
}
	