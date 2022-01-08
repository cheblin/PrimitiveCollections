package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.Array.hash;

public interface ObjectObjectMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K, V> int token(R<K, V> src, int token) {
			for (; ; )
				if (++token == src.keys.length) return INIT;
				else if (src.keys[token] != null) return token;
		}
		
		static <K, V> K key(R<K, V> src, int token)   {return src.keys[token];}
		
		static <K, V> V value(R<K, V> src, int token) {return src.values[token];}
	}
	
	abstract class R<K, V> implements Cloneable {
		
		public          K[]         keys;
		public          V[]         values;
		protected final Array.Of<K> hash_equalK;
		protected final Array.Of<V> hash_equalV;
		private final   boolean     K_is_string;
		private final   boolean     V_is_string;
		
		protected R(Class<K> clazzK, Class<V> clazzV) {
			
			hash_equalK = Array.get(clazzK);
			hash_equalV = Array.get(clazzV);
			K_is_string = clazzK == String.class;
			V_is_string = clazzV == String.class;
		}
		
		protected int assigned;
		
		protected int mask;
		
		
		protected int resizeAt;
		
		
		protected boolean hasNullKey;
		
		V NullKeyValue = null;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token(K key) {
			if (key == null) return hasNullKey ? keys.length : Positive_Values.NONE;
			
			int slot = hash_equalK.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (k.equals(key)) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains(K key)           {return !hasNone(token(key));}
		
		public boolean hasNone(int token)        {return token == Positive_Values.NONE;}
		
		public V value(@Positive_ONLY int token) {return token == keys.length ? NullKeyValue : values[token];}
		
		
		public int size()                        {return assigned + (hasNullKey ? 1 : 0);}
		
		
		public boolean isEmpty()                 {return size() == 0;}
		
		
		public int hashCode() {
			int hash = hash(hasNullKey ? hash_equalV.hashCode(NullKeyValue) : 10100011);
			
			for (int token = NonNullKeysIterator.INIT, h = 10100011; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
			     hash = (h++) + hash(hash(hash, hash_equalK.hashCode(NonNullKeysIterator.key(this, token))), hash_equalV.hashCode(NonNullKeysIterator.value(this, token)));
			return hash;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		public boolean equals(R<K, V> other) {
			if (other == null || size() != other.size() || hasNullKey != other.hasNullKey ||
			    (hasNullKey && NullKeyValue != other.NullKeyValue && (NullKeyValue == null || other.NullKeyValue == null || !NullKeyValue.equals(other.NullKeyValue)))) return false;
			
			
			K key;
			for (int i = keys.length, token = 0; -1 < --i; )
				if ((key = keys[i]) != null &&
				    ((token = other.token(key)) == -1 || !hash_equalV.equals(values[i], other.value(token)))) return false;
			
			return true;
		}
		
		
		@SuppressWarnings("unchecked")
		public R<K, V> clone() {
			
			try
			{
				R<K, V> dst = (R<K, V>) super.clone();
				
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		public Array.ISort.Objects<K> getK = null;
		public Array.ISort.Objects<V> getV = null;
		
		
		public void build_K(Array.ISort.Anything.Index dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get(int index) {return keys[index];}
			} : getK;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public void build_V(Array.ISort.Anything.Index dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = new Array.ISort.Objects<V>() {
				@Override V get(int index) {return values[index];}
			} : getV;
			
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
		
		public void build_V(Array.ISort.Anything.Index2 dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = getV == null ? getV = new Array.ISort.Objects<V>() {
				@Override V get(int index) {return values[index];}
			} : getV;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public String toString() {
			final JsonWriter        json   = JsonWriter.get();
			final JsonWriter.Config config = json.enter();
			
			
			if (0 < assigned)
			{
				json.preallocate(assigned * 10);
				int token = NonNullKeysIterator.INIT, i = 0;
				
				if (K_is_string)
				{
					json.enterObject();
					
					if (hasNullKey) json.name(null).value(NullKeyValue);
					
					if (json.orderByKey())
						for (build_K(json.anythingIndex); i < json.anythingIndex.size; i++)
						     json.
								     name(NonNullKeysIterator.key(this, token = json.anythingIndex.dst[i]).toString()).
								     value(NonNullKeysIterator.value(this, token));
					else
						while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
							json.
									name(NonNullKeysIterator.key(this, token).toString()).
									value(NonNullKeysIterator.value(this, token));
					json.exitObject();
				}
				else
				{
					json.enterArray();
					
					if (hasNullKey)
						json.
								enterObject()
								.name("Key").value()
								.name("Value").value(NullKeyValue).
								exitObject();
					
					if (json.orderByKey())
						for (build_K(json.anythingIndex); i < json.anythingIndex.size; i++)
						     json.
								     enterObject()
								     .name("Key").value(NonNullKeysIterator.key(this, token = json.anythingIndex.dst[i]))
								     .name("Value").value(NonNullKeysIterator.value(this, token)).
								     exitObject();
					else
						while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
							json.
									enterObject()
									.name("Key").value(NonNullKeysIterator.key(this, token))
									.name("Value").value(NonNullKeysIterator.value(this, token)).
									exitObject();
					json.exitArray();
					
				}
			}
			else
			{
				json.enterObject();
				if (hasNullKey) json.name(null).value(NullKeyValue);
				json.exitObject();
			}
			
			return json.exit(config);
		}
	}
	
	class RW<K, V> extends R<K, V> {
		
		
		public RW(Class<K> k, Class<V> v, int expectedItems) {this(k, v, expectedItems, 0.75);}
		
		
		public RW(Class<K> k, Class<V> v, int expectedItems, double loadFactor) {
			super(k, v);
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			long length = (long) Math.ceil(expectedItems / this.loadFactor);
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max(4, org.unirail.collections.Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys   = hash_equalK.copyOf(null, size);
			values = hash_equalV.copyOf(null, size);
		}
		
		
		public boolean put(K key, V value) {
			
			if (key == null)
			{
				boolean h = hasNullKey;
				hasNullKey   = true;
				NullKeyValue = value;
				return h != hasNullKey;
			}
			
			
			int slot = hash_equalK.hashCode(key) & mask;
			
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (hash_equalK.equals(k, key))
				{
					values[slot] = value;
					return true;
				}
			
			keys[slot]   = key;
			values[slot] = value;
			
			if (assigned++ == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			
			for (int i = keys.length - 1; i >= 0; i--, keys[i] = null, values[i] = null) ;
		}
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length < size) keys = hash_equalK.copyOf(null, size);
				if (values.length < size) values = hash_equalV.copyOf(null, size);
				return;
			}
			
			final K[] ks = this.keys;
			final V[] vs = this.values;
			
			keys   = hash_equalK.copyOf(null, size);
			values = hash_equalV.copyOf(null, size);
			
			K k;
			for (int i = ks.length; -1 < --i; )
				if ((k = ks[i]) != null)
				{
					int slot = hash_equalK.hashCode(k) & mask;
					while (!(keys[slot] == null)) slot = slot + 1 & mask;
					
					keys[slot]   = k;
					values[slot] = vs[i];
				}
		}
		
		public boolean remove(K key) {
			if (key == null)
			{
				boolean h = hasNullKey;
				hasNullKey = false;
				return h != hasNullKey;
			}
			
			int slot = hash_equalK.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (hash_equalK.equals(k, key))
				{
					final V v       = values[slot];
					int     gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - hash_equalK.hashCode(kk) & mask) >= distance)
						{
							values[gapSlot] = values[slot1];
							keys[gapSlot] = kk;
							                gapSlot = slot1;
							                distance = 0;
						}
					
					keys[gapSlot]   = null;
					values[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW<K, V> clone() {return (RW<K, V>) super.clone();}
	}
}
	