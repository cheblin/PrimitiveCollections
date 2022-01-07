package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;

import static org.unirail.collections.Array.hash;

public interface ObjectLongNullMap {
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K> int token(R<K> src, int token) {
			for (token++; ; token++)
				if (token == src.keys.length) return INIT;
				else if (src.keys[token] != null) return token;
		}
		
		static <K> K key(R<K> src, int token)            {return src.keys[token];}
		
		static <K> boolean hasValue(R<K> src, int token) {return src.values.hasValue(token);}
		
		static <K> long value(R<K> src, int token) {return src.values.get(token);}
	}
	
	abstract class R<K> implements Cloneable {
		
		protected final Array.Of<K> array;
		private final   boolean     K_is_string;
		
		protected R(Class<K> clazzK) {
			array       = Array.get(clazzK);
			K_is_string = clazzK == String.class;
		}
		
		K[]                    keys;
		LongNullList.RW values;
		
		protected int    assigned;
		protected int    mask;
		protected int    resizeAt;
		protected double loadFactor;
		
		
		public int size()              {return assigned + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		public boolean isEmpty()       {return size() == 0;}
		
		
		public boolean contains(K key) {return !hasNone(token(key));}
		
		public @Positive_Values int token(K key) {
			
			if (key == null) return hasNullKey == Positive_Values.NONE ? Positive_Values.NONE : keys.length;
			
			int slot = array.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key)) return (values.hasValue(slot)) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;
		}
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public boolean hasNull(int token)  {return token == keys.length;}
		
		public long value(@Positive_ONLY int token) {return token == keys.length ? NullKeyValue : values.get(token);}
		
		
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
		protected                  long NullKeyValue = 0;
		
		
		public Array.ISort.Objects<K> getK = null;
		public Array.ISort.Primitives getV = null;
		
		public void build_K(Array.ISort.Anything.Index dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get(int index) {return keys[index];}
			} : getK;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public void build_V(Array.ISort.Primitives.Index dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = index -> values.hasValue(index) ?  value(index) : 0 : getV;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public void build_K(Array.ISort.Anything.Index2 dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get(int index) {return keys[index];}
			} : getK;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public void build_V(Array.ISort.Primitives.Index2 dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = index -> values.hasValue(index) ?  value(index) : 0 : getV;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public String toString() {
			final JsonWriter        json   = JsonWriter.get();
			final JsonWriter.Config config = json.enter();
			
			
			if (0 < assigned)
			{
				json.preallocate(assigned * 10);
				int token = NonNullKeysIterator.INIT, i=0;
				
				if (K_is_string)
				{
					json.enterObject();
					
					switch (hasNullKey)
					{
						case Positive_Values.NULL:
							json.name(null).value();
							break;
						case Positive_Values.VALUE:
							json.name(null).value(NullKeyValue);
					}
					
					
					if (json.orderByKey())
						for (build_K(json.anythingIndex); i < json.anythingIndex.size; i++)
						{
							json.name(NonNullKeysIterator.key(this, token = json.anythingIndex.dst[i]).toString());
							if (NonNullKeysIterator.hasValue(this, token)) json.value(NonNullKeysIterator.value(this, token));
							else json.value();
						}
					else
						while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
						{
							json.name(NonNullKeysIterator.key(this, token).toString());
							if (NonNullKeysIterator.hasValue(this, token)) json.value(NonNullKeysIterator.value(this, token));
							else json.value();
						}
					
					json.exitObject();
				}
				else
				{
					json.enterArray();
					
					switch (hasNullKey)
					{
						case Positive_Values.NULL:
							json.name(null).value();
							json.
									name("Key").value().
									name("Value").value();
							
							break;
						case Positive_Values.VALUE:
							json.
									name("Key").value().
									name("Value").value(NullKeyValue);
					}
					
					if (json.orderByKey())
						for (build_K(json.anythingIndex); i < json.anythingIndex.size; i++)						{
							json.
									name("Key").value(NonNullKeysIterator.key(this, token = json.anythingIndex.dst[i])).
									name("Value");
							if (NonNullKeysIterator.hasValue(this, token)) json.value(NonNullKeysIterator.value(this, token));
							else json.value();
						}
					else
						while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
						{
							json.
									name("Key").value(NonNullKeysIterator.key(this, token)).
									name("Value");
							if (NonNullKeysIterator.hasValue(this, token)) json.value(NonNullKeysIterator.value(this, token));
							else json.value();
						}
					json.exitArray();
				}
			}
			else
			{
				json.enterObject();
				switch (hasNullKey)
				{
					case Positive_Values.NULL:
						json.name(null).value();
						break;
					case Positive_Values.VALUE:
						json.name(null).value(NullKeyValue);
				}
				json.exitObject();
			}
			
			return json.exit(config);
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
			values = new LongNullList.RW(size);
		}
		
		@Override public RW<K> clone() {return (RW<K>) super.clone();}
		
		
		public boolean put(K key,  Long      value) {
			if (value != null) put(key, (long) value);
			
			if (key == null)
			{
				hasNullKey = Positive_Values.NULL;
				return true;
			}
			
			int slot = array.hashCode(key) & mask;
			
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key))
				{
					values.set(slot, ( Long     ) null);
					return true;
				}
			
			keys[slot] = key;
			values.set(slot, ( Long     ) null);
			
			if (++assigned == resizeAt) this.allocate(mask + 1 << 1);
			
			return true;
		}
		
		public boolean put(K key, long value) {
			
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
			LongNullList.RW vals = values;
			
			keys   = array.copyOf(null, size);
			values = new LongNullList.RW(-size);
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = array.hashCode(key) & mask;
					while (!(keys[slot] == null)) slot = slot + 1 & mask;
					
					keys[slot] = key;
					
					if (vals.hasValue(i)) values.set(slot, vals.get(i));
					else values.set(slot, ( Long     ) null);
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
								values.set(gapSlot, ( Long     ) null);
							
							gapSlot  = s;
							distance = 0;
						}
					
					keys[gapSlot] = null;
					values.set(gapSlot, ( Long     ) null);
					assigned--;
					return true;
				}
			
			return false;
		}
	}
}
	
	