package org.unirail.collections;


import static org.unirail.collections.Array.HashEqual.hash;

public interface ObjectByteMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K> int token(R<K> src, int token) {
			for (; ; )
				if (++token == src.keys.array.length) return INIT;
				else if (src.keys.array[token] != null) return token;
		}
		
		static <K> K key(R<K> src, int token) {return src.keys.array[token];}
		
		static <K> byte value(R<K> src, int token) {return src.values.get(token);}
	}
	
	abstract class R<K> implements Cloneable {
		
		public ObjectList.RW<K>   keys;
		public ByteList.RW values;
		
		protected int     assigned;
		protected int     mask;
		protected int     resizeAt;
		protected boolean hasNullKey;
		
		byte NullKeyValue = 0;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token(K key) {
			if (key == null) return hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE;
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (keys.equals(key)) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains(K key)    {return !hasNone(token(key));}
		
		public byte value(@Positive_ONLY int token) {return token == Positive_Values.VALUE ? NullKeyValue :  (byte) values.array[token];}
		
		public boolean hasNone(int token) {return token == Positive_Values.NONE;}
		
		
		public int size()                 {return assigned + (hasNullKey ? 1 : 0);}
		
		public boolean isEmpty()          {return size() == 0;}
		
		
		public int hashCode() {
			int hash = hash(hasNullKey ? hash(NullKeyValue) : 719717, keys);
			for (int token = NonNullKeysIterator.INIT, h = 719717; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				hash = (h++) + hash(hash, keys.hash_equal.hashCode(NonNullKeysIterator.key(this, token)));
			
			return hash;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		public boolean equals(R<K> other) {
			if (other == null || hasNullKey != other.hasNullKey || hasNullKey && NullKeyValue != other.NullKeyValue || size() != other.size()) return false;
			
			K key;
			for (int i = keys.array.length, token; -1 < --i; )
				if ((key = keys.array[i]) != null && ((token = other.token(key)) == Positive_Values.NONE || values.array[i] != other.value(token))) return false;
			
			return true;
		}
		
		@SuppressWarnings("unchecked")
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
		
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (hasNullKey) dst.append("null -> ").append(NullKeyValue).append('\n');
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				dst.append(NonNullKeysIterator.key(this, token))
						.append(" -> ")
						.append(NonNullKeysIterator.value(this, token))
						.append('\n');
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
			values = new ByteList.RW(0);
		}
		
		public boolean put(K key, byte value) {
			
			if (key == null)
			{
				NullKeyValue = value;
				return !hasNullKey && (hasNullKey = true);
			}
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (keys.equals(key))
				{
					values.array[slot] = (byte) value;
					return false;
				}
			
			keys.array[slot] = key;
			values.array[slot] = (byte) value;
			
			if (assigned++ == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean remove(K key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = keys.hash_equal.hashCode(key) & mask;
			
			final K[] array = keys.array;
			
			for (K k; (k = array[slot]) != null; slot = slot + 1 & mask)
				if (keys.equals(key))
				{
					byte[] vals = values.array;
					
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != null; )
						if ((s - keys.hash_equal.hashCode(kk) & mask) >= distance)
						{
							vals[gapSlot] = vals[s];
							array[gapSlot] = kk;
							gapSlot = s;
							distance = 0;
						}
					
					array[gapSlot] = null;
					vals[gapSlot] = (byte) 0;
					assigned--;
					return true;
				}
			
			return false;
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
			
			final K[]           k = keys.array;
			final byte[] v = values.array;
			
			keys.length(-size);
			values.length(-size);
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = keys.hash_equal.hashCode(key) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot] = key;
					values.array[slot] = v[i];
				}
		}
		
		public RW<K> clone() {return (RW<K>) super.clone();}
	}
}
	