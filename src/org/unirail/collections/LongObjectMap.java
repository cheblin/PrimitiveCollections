package org.unirail.collections;


import static org.unirail.collections.Array.HashEqual.hash;

public interface LongObjectMap {
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static <V> int token(R<V> src, int token) {
			for (int len = src.keys.array.length; ; )
				if (++token == len) return src.has0Key ? -2 : INIT;
				else if (token == 0x7FFF_FFFF) return INIT;
				else if (src.keys.array[token] != 0) return token;
		}
		
		static <V> long key(R<V> src, int token) {return token == -2 ? 0 :  src.keys.array[token];}
		
		static <V> V value(R<V> src, int token) {return token == -2 ? src.OkeyValue : src.values.array[token];}
	}
	
	abstract class R<V> implements Cloneable {
		
		
		public LongList.RW keys;
		public ObjectList.RW<V>   values;
		
		protected R(Class<V[]> clazz, int size) {
			values = new ObjectList.RW<>(clazz, size);
			keys = new LongList.RW(size);
		}
		
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		boolean hasNullKey;
		V       nullKeyValue;
		
		
		boolean has0Key;
		V       OkeyValue;
		
		
		protected double loadFactor;
		
		public boolean contains( Long      key)           {return !hasNone(token(key));}
		
		public boolean contains(long key)               {return !hasNone(token(key));}
		
		public boolean hasNone(int token)                      {return token == Positive_Values.NONE;}
		
		
		public @Positive_Values int token( Long      key) {return key == null ? hasNullKey ? Integer.MAX_VALUE : Positive_Values.NONE : token((long) (key + 0));}
		
		public @Positive_Values int token(long key) {
			if (key == 0) return has0Key ? Positive_Values.VALUE - 1 : Positive_Values.NONE;
			
			int slot = hash(key) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return slot;
			
			return Positive_Values.NONE;
		}
		
		public V value(@Positive_ONLY int token) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			if (token == Positive_Values.VALUE - 1) return OkeyValue;
			
			return values.array[token];
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		
		public int hashCode() {
			int hash = hasNullKey ? hash(nullKeyValue) : 997651;
			
			for (int token = NonNullKeysIterator.INIT, h = 0xc2b2ae35; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				hash = (h++) + hash(hash(hash, NonNullKeysIterator.key(this, token)), values.hash_equal.hashCode(NonNullKeysIterator.value(this, token)));
			return hash;
		}
		
		
		@SuppressWarnings("unchecked") public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R<V> other) {
			
			if (other == null ||
			    assigned != other.assigned ||
			    has0Key != other.has0Key ||
			    hasNullKey != other.hasNullKey ||
			    has0Key && !values.hash_equal.equals(OkeyValue, other.OkeyValue) ||
			    hasNullKey && nullKeyValue != other.nullKeyValue &&
			    (nullKeyValue == null || other.nullKeyValue == null || !values.hash_equal.equals(nullKeyValue, other.nullKeyValue))) return false;
			
			V           v;
			long key;
			
			for (int i = keys.array.length, c; -1 < --i; )
				if ((key = keys.array[i]) != 0)
				{
					if ((c = other.token(key)) < 0) return false;
					v = other.value(c);
					
					if (values.array[i] != v)
						if (v == null || values.array[i] == null || !values.hash_equal.equals(v, values.array[i])) return false;
				}
			return true;
		}
		
		
		@SuppressWarnings("unchecked")
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
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
			
			if (hasNullKey) dst.append("null -> ").append(nullKeyValue).append('\n');
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				dst.append(NonNullKeysIterator.key(this, token))
						.append(" -> ")
						.append(NonNullKeysIterator.value(this, token))
						.append('\n');
			
			return dst;
		}
	}
	
	class RW<V> extends R<V> {
		
		public RW(Class<V[]> clazz, double loadFactor) { super(clazz, 0); this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);}
		
		public RW(Class<V[]> clazz, int expectedItems) {this(clazz, expectedItems, 0.75);}
		
		
		public RW(Class<V[]> clazz, int expectedItems, double loadFactor) {
			this(clazz, loadFactor);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
			values.length(size);
		}
		
		
		public boolean put( Long      key, V value) {
			if (key != null) return put((long) key, value);
			
			hasNullKey = true;
			nullKeyValue = value;
			
			return true;
		}
		
		public boolean put(long key, V value) {
			if (key == 0)
			{
				has0Key = true;
				OkeyValue = value;
				return true;
			}
			
			
			int slot = hash(key) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					values.array[slot] = value;
					return true;
				}
			
			keys.array[slot] =  key;
			values.array[slot] = value;
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean remove( Long      key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			return remove((long) key);
		}
		
		public boolean remove(long key) {
			
			if (key == 0) return has0Key && !(has0Key = false);
			
			int slot =hash(key) & mask;
			
			for (long k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					long kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hash(kk) & mask) >= distance)
						{
							
							keys.array[gapSlot] =  kk;
							values.array[gapSlot] = values.array[s];
							gapSlot = s;
							distance = 0;
						}
					
					keys.array[gapSlot] = 0;
					values.array[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			
			has0Key = false;
			OkeyValue = null;
			
			hasNullKey = false;
			nullKeyValue = null;
			
			keys.clear();
			values.clear();
		}
		
		
		void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				
				if (values.length() < size) values.length(-size);
				return;
			}
			
			
			final long[] k = keys.array;
			final V[]           v = values.array;
			
			keys.length(-size);
			values.length(-size);
			
			long key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = hash(key) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] =  key;
					values.array[slot] = v[i];
				}
		}
		
		
		public RW<V> clone() {return (RW<V>) super.clone();}
	}
}
