package org.unirail.collections;


import static org.unirail.collections.Array.HashEqual.hash;

public interface ObjectObjectMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K, V> int token(R<K, V> src, int token) {
			for (; ; )
				if (++token == src.keys.array.length) return INIT;
				else if (src.keys.array[token] != null) return token;
		}
		
		static <K, V> K key(R<K, V> src, int token)   {return src.keys.array[token];}
		
		static <K, V> V value(R<K, V> src, int token) {return src.values.get(token);}
	}
	
	abstract class R<K, V> implements Cloneable {
		
		public ObjectList.RW<K> keys;
		public ObjectList.RW<V> values;
		
		
		protected int assigned;
		
		
		protected int mask;
		
		
		protected int resizeAt;
		
		
		protected boolean hasNullKey;
		
		V NullKeyValue = null;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token(K key) {
			if (key == null) return hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE;
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.equals(key)) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains(K key)           {return !hasNone(token(key));}
		
		public boolean hasNone(int token)        {return token == Positive_Values.NONE;}
		
		public V value(@Positive_ONLY int token) {return token == Positive_Values.VALUE ? NullKeyValue : values.array[token];}
		
		
		public int size()                        {return assigned + (hasNullKey ? 1 : 0);}
		
		
		public boolean isEmpty()                 {return size() == 0;}
		
		
		public int hashCode() {
			int hash = hash(hasNullKey ? values.hash_equal.hashCode(NullKeyValue) : 10100011);
			
			for (int token = NonNullKeysIterator.INIT, h = 10100011; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				hash = (h++) + hash(hash(hash, keys.hash_equal.hashCode(NonNullKeysIterator.key(this, token))), values.hash_equal.hashCode(NonNullKeysIterator.value(this, token)));
			return hash;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		public boolean equals(R<K, V> other) {
			if (other == null || size() != other.size() || hasNullKey != other.hasNullKey ||
			    (hasNullKey && NullKeyValue != other.NullKeyValue && (NullKeyValue == null || other.NullKeyValue == null || !NullKeyValue.equals(other.NullKeyValue)))) return false;
			
			
			K key;
			
			for (int i = keys.array.length, token = 0; -1 < --i; )
				if ((key = keys.array[i]) != null && (token = other.token(key)) == -1 ||
				    !values.hash_equal.equals(values.array[i], other.value(token))) return false;
			
			return true;
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
		
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (hasNullKey) dst.append("null -> ").append(NullKeyValue);
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				dst.append(NonNullKeysIterator.key(this, token)).append(" -> ").append(NonNullKeysIterator.value(this, token));
			return dst;
		}
	}
	
	class RW<K, V> extends R<K, V> {
		
		
		public RW(Class<K[]> k, Class<V[]> v, int expectedItems) {this(k, v, expectedItems, 0.75);}
		
		
		public RW(Class<K[]> k, Class<V[]> v, int expectedItems, double loadFactor) {
			
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			long length = (long) Math.ceil(expectedItems / this.loadFactor);
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys = new ObjectList.RW<>(k, size);
			values = new ObjectList.RW<>(v, size);
		}
		
		
		public boolean put(K key, V value) {
			
			if (key == null)
			{
				boolean h = hasNullKey;
				hasNullKey = true;
				NullKeyValue = value;
				return h != hasNullKey;
			}
			
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (keys.hash_equal.equals(k, key))
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
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				if (values.length() < size) values.length(-size);
				return;
			}
			
			final K[] ks = this.keys.array;
			final V[] vs = this.values.array;
			
			keys.length(-size);
			values.length(-size);
			
			K k;
			for (int i = ks.length; -1 < --i; )
				if ((k = ks[i]) != null)
				{
					int slot = keys.hash_equal.hashCode(k) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot] = k;
					values.array[slot] = vs[i];
				}
		}
		
		public boolean remove(K key) {
			if (key == null)
			{
				boolean h = hasNullKey;
				hasNullKey = false;
				return h != hasNullKey;
			}
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (keys.hash_equal.equals(k, key))
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys.array[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - keys.hash_equal.hashCode(kk) & mask) >= distance)
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
		
		public RW<K, V> clone() {return (RW<K, V>) super.clone();}
	}
}
	