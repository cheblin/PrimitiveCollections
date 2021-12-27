package org.unirail.collections;


import static org.unirail.collections.Array.HashEqual.hash;

public interface ObjectUShortNullMap {
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K> int token(R<K> src, int token) {
			for (token++, token &= Integer.MAX_VALUE; ; token++)
				if (token == src.keys.array.length) return INIT;
				else if (src.keys.array[token] != null) return src.values.hasValue(token) ? token : token | Integer.MIN_VALUE;
		}
		
		static <K> K key(R<K> src, int token)            {return src.keys.array[token & Integer.MAX_VALUE];}
		
		static <K> boolean hasValue(R<K> src, int token) {return -1 < token;}
		
		static <K> char value(R<K> src, int token) {return src.values.get(token);}
	}
	
	abstract class R<K> implements Cloneable {
		
		
		ObjectList.RW<K>       keys;
		UShortNullList.RW values;
		
		protected int    assigned;
		protected int    mask;
		protected int    resizeAt;
		protected double loadFactor;
		
		
		public int size()        {return assigned + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		public boolean isEmpty() {return size() == 0;}
		
	
		
		public boolean contains(K key) {return !hasNone(token(key));}
		
		public @Positive_Values int token(K key) {
			
			if (key == null) return hasNullKey;
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (keys.equals(key)) return (values.hasValue(slot)) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;
		}
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public boolean hasNull(int token)  {return token == Positive_Values.NULL;}
		
		public char value(@Positive_ONLY int token) {return token == Positive_Values.VALUE ? NullKeyValue : values.get(token);}
		
		
		public int hashCode() {
			int hash = hash(hasNullKey == Positive_Values.NONE ? 719281 : hasNullKey == Positive_Values.NULL ? 401101 : hash(NullKeyValue));
			
			for (int token = NonNullKeysIterator.INIT, h = 719281; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				hash = (h++) + hash(hash, hash(keys.hash_equal.hashCode(NonNullKeysIterator.key(this, token)),
						NonNullKeysIterator.hasValue(this, token) ? NonNullKeysIterator.value(this, token) : h++));
			
			return hash;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));
		}
		
		public boolean equals(R<K> other) {
			if (other == null || hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && NullKeyValue != other.NullKeyValue || size() != other.size()) return false;
			
			
			K key;
			for (int i = keys.array.length, token; -1 < --i; )
				if ((key = keys.array[i]) != null)
					if (values.nulls.get(i))
					{
						if (( token = other.token(key)) == -1 || values.get(i) != other.value(token)) return false;
					}
					else if (-1 < other.token(key)) return false;
			
			return true;
		}
		
		@Override @SuppressWarnings("unchecked")
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				
				dst.keys = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		protected @Positive_Values int         hasNullKey   = Positive_Values.NONE;
		protected                  char NullKeyValue = 0;
		
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			switch (hasNullKey)
			{
				case Positive_Values.NULL:
					dst.append("null -> null\n");
					break;
				case Positive_Values.VALUE:
					dst.append("null -> ").append(NullKeyValue).append('\n');
			}
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; dst.append('\n'))
			{
				dst.append(NonNullKeysIterator.key(this, token)).append(" -> ");
				
				if (NonNullKeysIterator.hasValue(this, token)) dst.append(NonNullKeysIterator.value(this, token));
				else dst.append("null");
			}
			
			return dst;
		}
	}
	
	class RW<K> extends R<K> {
		
		public RW(Class<K[]> clazz, int expectedItems) {this(clazz, expectedItems, 0.75);}
		
		public RW(Class<K[]> clazz, int expectedItems, double loadFactor) {
			
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			long length = (long) Math.ceil(expectedItems / loadFactor);
			
			int size = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys = new ObjectList.RW<>(clazz, size);
			values= new UShortNullList.RW(size);
		}
		
		@Override public RW<K> clone() {return (RW<K>) super.clone();}
		
		
		public boolean put(K key,  Character value) {
			if (value != null) put(key, (char) value);
			
			if (key == null)
			{
				hasNullKey = Positive_Values.NULL;
				return true;
			}
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (keys.equals(key))
				{
					values.set(slot, ( Character) null);
					return true;
				}
			
			keys.array[slot] = key;
			values.set(slot, ( Character) null);
			
			if (++assigned == resizeAt) this.allocate(mask + 1 << 1);
			
			return true;
		}
		
		public boolean put(K key, char value) {
			
			if (key == null)
			{
				int h = hasNullKey;
				hasNullKey = Positive_Values.VALUE;
				NullKeyValue = value;
				return h != Positive_Values.VALUE;
			}
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (keys.equals(key))
				{
					values.set(slot, value);
					return true;
				}
			
			keys.array[slot] = key;
			values.set(slot, value);
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		public void clear() {
			assigned = 0;
			hasNullKey = Positive_Values.NONE;
			keys.clear();
			values.clear();
		}
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				
				if (values.nulls.length() < size) values.nulls.length(-size);
				
				if (values.values.length() < size) values.values.length(-size);
				
				return;
			}
			
			final K[]              k    = keys.array;
			UShortNullList.RW vals = values;
			
			keys.length(-size);
			values = new UShortNullList.RW(-size);
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = keys.hash_equal.hashCode(key) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot] = key;
					
					if (vals.hasValue(i)) values.set(slot, vals.get(i));
					else values.set(slot, ( Character) null);
				}
			
		}
		
		
		public boolean remove(K key) {
			if (key == null)
			{
				int h = hasNullKey;
				hasNullKey = Positive_Values.NONE;
				return h != Positive_Values.NONE;
			}
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			final K[] ks = keys.array;
			for (K k; (k = ks[slot]) != null; slot = slot + 1 & mask)
				if (keys.equals(key))
				{
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = ks[s = gapSlot + ++distance & mask]) != null; )
						if ((s - keys.hash_equal.hashCode(kk) & mask) >= distance)
						{
							ks[gapSlot] = kk;
							
							if (values.nulls.get(s))
								values.set(gapSlot, values.get(s));
							else
								values.set(gapSlot, ( Character) null);
							
							gapSlot = s;
							distance = 0;
						}
					
					ks[gapSlot] = null;
					values.set(gapSlot, ( Character) null);
					assigned--;
					return true;
				}
			
			return false;
		}
		
		
	}
	
}
	
	