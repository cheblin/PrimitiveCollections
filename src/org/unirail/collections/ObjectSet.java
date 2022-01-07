package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.Array.hash;

public interface ObjectSet {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static <K> int token(R<K> src, int token) {
			for (; ; )
				if (++token == src.keys.length) return INIT;
				else if (src.keys[token] != null) return token;
		}
		
		static <K> K key(R<K> src, int token) {return src.keys[token];}
	}
	
	
	abstract class R<K> implements Cloneable {
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNullKey;
		
		protected double loadFactor;
		
		protected final Array.Of<K> array;
		private final   char        s;
		
		protected R(Class<K> clazz) {
			array = Array.get(clazz);
			s     = clazz == String.class ? '"' : '\0';
		}
		
		public K[] keys;
		
		
		public boolean contains(K key) {
			if (key == null) return hasNullKey;
			int slot = array.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key)) return true;
			
			return false;
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (hasNullKey ? 1 : 0);}
		
		public int hashCode() {
			int hash = hash(hasNullKey ? 10153331 : 888888883);
			for (int token = NonNullKeysIterator.INIT, h = 888888883; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
			     hash = (h++) + hash(hash, array.hashCode(NonNullKeysIterator.key(this, token)));
			return hash;
		}
		
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		public boolean equals(R<K> other) {
			if (other == null || other.assigned != assigned || other.hasNullKey != hasNullKey) return false;
			
			for (K k : keys) if (k != null && !other.contains(k)) return false;
			
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
		
		public Array.ISort.Objects<K> getK = null;
		
		public void build(Array.ISort.Anything.Index dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get(int index) {return keys[index];}
			} : getK;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		
		public void build(Array.ISort.Anything.Index2 dst) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = getK == null ? getK = new Array.ISort.Objects<K>() {
				@Override K get(int index) {return keys[index];}
			} : getK;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != null) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		
		public String toString() {
			final JsonWriter        json   = JsonWriter.get();
			final JsonWriter.Config config = json.enter();
			json.enterArray();
			
			if (hasNullKey) json.value();
			
			int size = size(), token = ObjectObjectMap.NonNullKeysIterator.INIT, i = 0;
			;
			if (0 < size)
			{
				json.preallocate(size * 10);
				
				if (json.orderByKey())
					for (build(json.anythingIndex); i < json.anythingIndex.size; i++)
					     json.value(NonNullKeysIterator.key(this, json.anythingIndex.dst[i]));
				else
					while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
						json.value(NonNullKeysIterator.key(this, token));
			}
			
			json.exitArray();
			return json.exit(config);
		}
	}
	
	class RW<K> extends R<K> {
		
		
		public RW(Class<K> clazz, int expectedItems) {this(clazz, expectedItems, 0.75f);}
		
		public RW(Class<K> clazz, int expectedItems, double loadFactor) {
			super(clazz);
			
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			long length = (long) Math.ceil(expectedItems / loadFactor);
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max(4, org.unirail.collections.Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys = array.copyOf(null, size);
		}
		
		@SafeVarargs public RW(Class<K> clazz, K... items) {
			this(clazz, items.length);
			for (K key : items) add(key);
		}
		
		public boolean add(K key) {
			if (key == null) return !hasNullKey && (hasNullKey = true);
			
			int slot = array.hashCode(key) & mask;
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key)) return false;
			
			keys[slot] = key;
			if (assigned == resizeAt) this.allocate(mask + 1 << 1);
			
			assigned++;
			return true;
		}
		
		public void clear() {
			assigned   = 0;
			hasNullKey = false;
			for (int i = keys.length - 1; i >= 0; i--) keys[i] = null;
		}
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length < size) keys = array.copyOf(null, size);
				return;
			}
			
			final K[] k = keys;
			keys = array.copyOf(null, size);
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = array.hashCode(key) & mask;
					
					while (keys[slot] != null) slot = slot + 1 & mask;
					
					keys[slot] = key;
				}
			
		}
		
		
		public boolean remove(K key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = array.hashCode(key) & mask;
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key))
				{
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, slot1; (kk = keys[slot1 = gapSlot + ++distance & mask]) != null; )
						if ((slot1 - array.hashCode(kk) & mask) >= distance)
						{
							keys[gapSlot] = kk;
							                gapSlot = slot1;
							                distance = 0;
						}
					
					keys[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW<K> clone() {return (RW<K>) super.clone();}
		
	}
}
	