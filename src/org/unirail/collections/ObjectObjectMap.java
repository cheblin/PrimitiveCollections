package org.unirail.collections;


public interface ObjectObjectMap {
	
	interface Consumer<K extends Comparable<? super K>, V extends Comparable<? super V>> {
		boolean put(K key, V value);
		
		void write(int size);
	}
	
	interface Producer<K extends Comparable<? super K>, V extends Comparable<? super V>> {
		
		int size();
		
		boolean read_has_null_key();
		
		V read_null_key_val();
		
		
		int read(int info);
		
		K read_key(int info);
		
		V read_val(int info);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (read_has_null_key())
			{
				dst.append("null -> ").append(read_null_key_val());
				size--;
			}
			
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(read_key(p = read(p))).append(" -> ").append(read_val(p));
			return dst;
		}
	}
	
	class R<K extends Comparable<? super K>, V extends Comparable<? super V>> implements Cloneable, Comparable<R<K, V>>, Producer<K, V> {
		
		public ObjectList.RW<K> keys   = new ObjectList.RW<>(0);
		public ObjectList.RW<V> values = new ObjectList.RW<>(0);
		
		
		protected int assigned;
		
		
		protected int mask;
		
		
		protected int resizeAt;
		
		
		protected boolean hasNullKey;
		
		V NullKeyValue = null;
		
		protected double loadFactor;
		
		
		protected R(double loadFactor) {
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
		}
		
		protected R(int expectedItems) { this(expectedItems, 0.75); }
		
		
		protected R(int expectedItems, double loadFactor) {
			this(loadFactor);
			
			long length = (long) Math.ceil(expectedItems / this.loadFactor);
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
			values.length(size);
		}
		
		public @Positive_Values int info(K key) {
			if (key == null) return hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE;
			
			int slot = Array.hash(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains(K key)          {return -1 < info(key);}
		
		public boolean contains(int info)       {return -1 < info;}
		
		public V value(@Positive_ONLY int info) {return info == Integer.MAX_VALUE ? NullKeyValue : values.array[info]; }
		
		
		public int size()                       { return assigned + (hasNullKey ? 1 : 0); }
		
		
		public boolean isEmpty()                { return size() == 0; }
		
		
		public int hashCode() {
			int h = hasNullKey ? 0xDEADBEEF : 0;
			K   k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != null)
					h += Array.hash(k) + Array.hash(values.array[i]);
			return h;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R<K, V> other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R<K, V> other) {
			if (other == null) return -1;
			if (size() != other.size()) return size() - other.size();
			
			if (hasNullKey != other.hasNullKey) return 1;
			
			int diff;
			if (hasNullKey)
				if (NullKeyValue == null) {if (other.NullKeyValue != null) return -1;}
				else if ((diff = NullKeyValue.compareTo(other.NullKeyValue)) != 0) return diff;
			
			K key;
			
			for (int i = keys.array.length - 1, tag; -1 < i; i--)
				if ((key = keys.array[i]) != null)
					if ((tag = other.info(key)) == -1 || values.array[i] != other.value(tag)) return 1;
			
			return 0;
		}
		
		
		@SuppressWarnings("unchecked")
		public R<K, V> clone() {
			try
			{
				R<K, V> dst = (R<K, V>) super.clone();
				
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
		
		@Override public int read(int info)          { for (; ; ) if (keys.array[++info] != null) return info; }
		
		@Override public boolean read_has_null_key() { return hasNullKey; }
		
		@Override public V read_null_key_val()       { return NullKeyValue; }
		
		@Override public K read_key(int info)        {return keys.array[info]; }
		
		@Override public V read_val(int info)        {return values.value(info); }
		
		//endregion
		
		
		public String toString() { return toString(null).toString();}
	}
	
	class RW<K extends Comparable<? super K>, V extends Comparable<? super V>> extends R<K, V> implements Consumer<K, V> {
		
		
		public RW(double loadFactor)                    { super(loadFactor); }
		
		public RW(int expectedItems)                    { super(expectedItems); }
		
		public RW(int expectedItems, double loadFactor) { super(expectedItems, loadFactor); }
		
		
		public boolean put(K key, V value) {
			
			if (key == null)
			{
				boolean h = hasNullKey;
				hasNullKey = true;
				NullKeyValue = value;
				return h != hasNullKey;
			}
			
			
			int slot = Array.hash(key) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0)
				{
					values.array[slot] = value;
					return true;
				}
			
			keys.array[slot] = key;
			values.array[slot] = value;
			
			if (assigned++ == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public void clear() {
			assigned = 0;
			hasNullKey = false;
			
			keys.clear();
			values.clear();
		}
		
		//region  consumer
		@Override public void write(int size) {
			assigned = 0;
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
			
			final K[] k = this.keys.array;
			final V[] v = this.values.array;
			
			keys.length(-size);
			values.length(-size);
			
			K kk;
			for (int from = k.length - 1; 0 <= --from; )
				if ((kk = k[from]) != null)
				{
					int slot = Array.hash(kk.hashCode()) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot] = kk;
					values.array[slot] = v[from];
				}
		}
		
		public boolean remove(K key) {
			if (key == null)
			{
				boolean h = hasNullKey;
				hasNullKey = false;
				return h != hasNullKey;
			}
			
			int slot = Array.hash(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0)
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys.array[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - Array.hash(kk.hashCode()) & mask) >= distance)
						{
							values.array[gapSlot] = values.array[slot1];
							keys.array[gapSlot] = kk;
							gapSlot = slot1;
							distance = 0;
						}
					
					keys.array[gapSlot] = null;
					values.array[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW<K, V> clone() { return (RW<K, V>) super.clone(); }
	}
}
	