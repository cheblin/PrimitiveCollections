package org.unirail.collections;


import static org.unirail.collections.Array.hash;

public interface ObjectFloatMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K> int token(R<K> src, int token) {
			for (; ; )
				if (++token == src.keys.length) return INIT;
				else if (src.keys[token] != null) return token;
		}
		
		static <K> K key(R<K> src, int token) {return src.keys[token];}
		
		static <K> float value(R<K> src, int token) {return   src.values[token];}
	}
	
	abstract class R<K> implements Cloneable {
		
		protected final Array<K> array;
		
		protected R(Class<K> clazz) {array = Array.get(clazz);}
		
		K[] keys;
		public float[] values;
		
		protected int     assigned;
		protected int     mask;
		protected int     resizeAt;
		protected boolean hasNullKey;
		
		float NullKeyValue = 0;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token(K key) {
			if (key == null) return hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE;
			
			int slot = array.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key)) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains(K key)    {return !hasNone(token(key));}
		
		public float value(@Positive_ONLY int token) {return token == Positive_Values.VALUE ? NullKeyValue :   values[token];}
		
		public boolean hasNone(int token) {return token == Positive_Values.NONE;}
		
		
		public int size()                 {return assigned + (hasNullKey ? 1 : 0);}
		
		public boolean isEmpty()          {return size() == 0;}
		
		
		public int hashCode() {
			int hash = hash(hasNullKey ? hash(NullKeyValue) : 719717, keys);
			for (int token = NonNullKeysIterator.INIT, h = 719717; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				hash = (h++) + hash(hash, array.hashCode(NonNullKeysIterator.key(this, token)));
			
			return hash;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		public boolean equals(R<K> other) {
			if (other == null || hasNullKey != other.hasNullKey || hasNullKey && NullKeyValue != other.NullKeyValue || size() != other.size()) return false;
			
			K key;
			for (int i = keys.length, token; -1 < --i; )
				if ((key = keys[i]) != null && ((token = other.token(key)) == Positive_Values.NONE || values[i] != other.value(token))) return false;
			
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
		
		public RW(Class<K> clazz, int expectedItems) {this(clazz, expectedItems, 0.75);}
		
		public RW(Class<K> clazz, int expectedItems, double loadFactor) {
			super(clazz);
			
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			long length = (long) Math.ceil(expectedItems / loadFactor);
			
			int size = (int) (length == expectedItems ? length + 1 : Math.max(4, org.unirail.collections.Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys = array.copyOf(null, size);
			values = new float[size];
		}
		
		public boolean put(K key, float value) {
			
			if (key == null)
			{
				NullKeyValue = value;
				return !hasNullKey && (hasNullKey = true);
			}
			
			int slot = array.hashCode(key) & mask;
			
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key))
				{
					values[slot] = (float) value;
					return false;
				}
			
			keys[slot] = key;
			values[slot] = (float) value;
			
			if (assigned++ == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean remove(K key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = array.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k,key))
				{
					float[] vals = values;
					
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != null; )
						if ((s - array.hashCode(kk) & mask) >= distance)
						{
							vals[gapSlot] = vals[s];
							keys[gapSlot] = kk;
							gapSlot = s;
							distance = 0;
						}
					
					keys[gapSlot] = null;
					vals[gapSlot] = (float) 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			hasNullKey = false;
			for (int i = keys.length - 1; i >= 0; i--) keys[i] = null;
		}
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length < size) keys = array.copyOf(null, size);
				if (values.length < size) values =new float[size];
				return;
			}
			
			final K[]           k = keys;
			final float[] v = values;
			
			keys = array.copyOf(null, size);
			values =new float[size];
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = array.hashCode(key) & mask;
					while (!(keys[slot] == null)) slot = slot + 1 & mask;
					
					keys[slot] = key;
					values[slot] = v[i];
				}
		}
		
		public RW<K> clone() {return (RW<K>) super.clone();}
	}
}
	