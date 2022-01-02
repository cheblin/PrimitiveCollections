package org.unirail.collections;


import java.util.Arrays;

import static org.unirail.collections.Array.hash;

public interface ObjectFloatNullMap {
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K> int token(R<K> src, int token) {
			for (token++, token &= Integer.MAX_VALUE; ; token++)
				if (token == src.keys.length) return INIT;
				else if (src.keys[token] != null) return src.values.hasValue(token) ? token : token | Integer.MIN_VALUE;
		}
		
		static <K> K key(R<K> src, int token)            {return src.keys[token & Integer.MAX_VALUE];}
		
		static <K> boolean hasValue(R<K> src, int token) {return -1 < token;}
		
		static <K> float value(R<K> src, int token) {return src.values.get(token);}
	}
	
	abstract class R<K> implements Cloneable {
		
		protected final Array<K> array;
		
		protected R(Class<K> clazz) {array = Array.get(clazz);}
		
		K[]                    keys;
		FloatNullList.RW values;
		
		protected int    assigned;
		protected int    mask;
		protected int    resizeAt;
		protected double loadFactor;
		
		
		public int size()              {return assigned + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		public boolean isEmpty()       {return size() == 0;}
		
		
		public boolean contains(K key) {return !hasNone(token(key));}
		
		public @Positive_Values int token(K key) {
			
			if (key == null) return hasNullKey;
			
			int slot = array.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key)) return (values.hasValue(slot)) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;
		}
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public boolean hasNull(int token)  {return token == Positive_Values.NULL;}
		
		public float value(@Positive_ONLY int token) {return token == Positive_Values.VALUE ? NullKeyValue : values.get(token);}
		
		
		public int hashCode() {
			int hash = hash(hasNullKey == Positive_Values.NONE ? 719281 : hasNullKey == Positive_Values.NULL ? 401101 : hash(NullKeyValue));
			
			for (int token = NonNullKeysIterator.INIT, h = 719281; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
			     hash = (h++) + hash(hash, hash(array.hashCode(NonNullKeysIterator.key(this, token)),
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
			for (int i = keys.length, token; -1 < --i; )
				if ((key = keys[i]) != null)
					if (values.nulls.get(i))
					{
						if ((token = other.token(key)) == -1 || values.get(i) != other.value(token)) return false;
					}
					else if (-1 < other.token(key)) return false;
			
			return true;
		}
		
		@Override @SuppressWarnings("unchecked")
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		protected @Positive_Values int         hasNullKey   = Positive_Values.NONE;
		protected                  float NullKeyValue = 0;
		
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			switch (hasNullKey)
			{
				case Positive_Values.NULL:
					dst.append("Ø -> Ø\n");
					break;
				case Positive_Values.VALUE:
					dst.append("Ø -> ").append(NullKeyValue).append('\n');
			}
			
			final int[] indexes = new int[assigned];
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) indexes[k++] = i;
			
			Array.ISort sorter = new Array.ISort() {
				
				int more = 1, less = -1;
				@Override public void asc() {less = -(more = 1);}
				@Override public void desc() {more = -(less = 1);}
				
				@Override public int compare(int ia, int ib) {
					final int x = keys[indexes[ia]].hashCode(), y = keys[indexes[ib]].hashCode();
					return x < y ? less : x == y ? 0 : more;
				}
				@Override public void swap(int ia, int ib) {
					final int t = indexes[ia];
					indexes[ia] = indexes[ib];
					indexes[ib] = t;
				}
				@Override public void set(int idst, int isrc) {indexes[idst] = indexes[isrc];}
				@Override public int compare(int isrc) {
					final int x = fix, y = keys[indexes[isrc]].hashCode();
					return x < y ? less : x == y ? 0 : more;
				}
				
				int fix = 0;
				int fixi = 0;
				@Override public void get(int isrc) {fix = keys[fixi = indexes[isrc]].hashCode();}
				@Override public void set(int idst) {indexes[idst] = fixi;}
			};
			
			Array.ISort.sort(sorter, 0, assigned - 1);
			
			for (int i = 0, j; i < assigned; i++)
			{
				dst.append(keys[j = indexes[i]]).append(" -> ");
				
				if (values.hasValue(j)) dst.append(values.get(j));
				else dst.append('Ø');
				dst.append('\n');
			}
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
			
			keys   = array.copyOf(null, size);
			values = new FloatNullList.RW(size);
		}
		
		@Override public RW<K> clone() {return (RW<K>) super.clone();}
		
		
		public boolean put(K key,  Float     value) {
			if (value != null) put(key, (float) value);
			
			if (key == null)
			{
				hasNullKey = Positive_Values.NULL;
				return true;
			}
			
			int slot = array.hashCode(key) & mask;
			
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key))
				{
					values.set(slot, ( Float    ) null);
					return true;
				}
			
			keys[slot] = key;
			values.set(slot, ( Float    ) null);
			
			if (++assigned == resizeAt) this.allocate(mask + 1 << 1);
			
			return true;
		}
		
		public boolean put(K key, float value) {
			
			if (key == null)
			{
				int h = hasNullKey;
				hasNullKey   = Positive_Values.VALUE;
				NullKeyValue = value;
				return h != Positive_Values.VALUE;
			}
			
			int slot = array.hashCode(key) & mask;
			
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key))
				{
					values.set(slot, value);
					return true;
				}
			
			keys[slot] = key;
			values.set(slot, value);
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		public void clear() {
			assigned   = 0;
			hasNullKey = Positive_Values.NONE;
			for (int i = keys.length - 1; i >= 0; i--) keys[i] = null;
			values.clear();
		}
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length < size) keys = array.copyOf(null, size);
				
				if (values.nulls.length() < size) values.nulls.length(-size);
				if (values.values.values.length < size) values.values.values = Arrays.copyOf(values.values.values, size);
				
				return;
			}
			
			final K[]              k    = keys;
			FloatNullList.RW vals = values;
			
			keys   = array.copyOf(null, size);
			values = new FloatNullList.RW(-size);
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = array.hashCode(key) & mask;
					while (!(keys[slot] == null)) slot = slot + 1 & mask;
					
					keys[slot] = key;
					
					if (vals.hasValue(i)) values.set(slot, vals.get(i));
					else values.set(slot, ( Float    ) null);
				}
		}
		
		
		public boolean remove(K key) {
			if (key == null)
			{
				int h = hasNullKey;
				hasNullKey = Positive_Values.NONE;
				return h != Positive_Values.NONE;
			}
			
			int slot = array.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key))
				{
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != null; )
						if ((s - array.hashCode(kk) & mask) >= distance)
						{
							keys[gapSlot] = kk;
							
							if (values.nulls.get(s))
								values.set(gapSlot, values.get(s));
							else
								values.set(gapSlot, ( Float    ) null);
							
							gapSlot  = s;
							distance = 0;
						}
					
					keys[gapSlot] = null;
					values.set(gapSlot, ( Float    ) null);
					assigned--;
					return true;
				}
			
			return false;
		}
	}
}
	
	