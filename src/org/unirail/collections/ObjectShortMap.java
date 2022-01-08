package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.Array.hash;

public interface ObjectShortMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static <K> int token(R<K> src, int token) {
			for (; ; )
				if (++token == src.keys.length) return INIT;
				else if (src.keys[token] != null) return token;
		}
		
		static <K> K key(R<K> src, int token) {return src.keys[token];}
		
		static <K> short value(R<K> src, int token) {return  (short) src.values[token];}
	}
	
	abstract class R<K> implements Cloneable {
		
		protected final Array.Of<K> array;
		private final   boolean     K_is_string;
		
		protected R(Class<K> clazzK) {
			array       = Array.get(clazzK);
			K_is_string = clazzK == String.class;
		}
		
		K[] keys;
		public short[] values;
		
		protected int     assigned;
		protected int     mask;
		protected int     resizeAt;
		protected boolean hasNullKey;
		
		short NullKeyValue = 0;
		
		protected double loadFactor;
		
		
		public @Positive_Values int token(K key) {
			if (key == null) return hasNullKey ? keys.length + 1 : Positive_Values.NONE;
			
			int slot = array.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key)) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains(K key)    {return !hasNone(token(key));}
		
		public short value(@Positive_ONLY int token) {return token == keys.length + 1 ? NullKeyValue :  (short) values[token];}
		
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
			
			dst.src = getV == null ? getV = index -> (short) values[index] : getV;
			
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
			
			dst.src = getV == null ? getV = index -> (short) values[index] : getV;
			
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
								.name("Key").value(NonNullKeysIterator.key(this, token))
								.name("Value").value(NonNullKeysIterator.value(this, token)).
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
	
	class RW<K> extends R<K> {
		
		public RW(Class<K> clazz, int expectedItems) {this(clazz, expectedItems, 0.75);}
		
		public RW(Class<K> clazz, int expectedItems, double loadFactor) {
			super(clazz);
			
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			long length = (long) Math.ceil(expectedItems / loadFactor);
			
			int size = (int) (length == expectedItems ? length + 1 : Math.max(4, org.unirail.collections.Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys   = array.copyOf(null, size);
			values = new short[size];
		}
		
		public boolean put(K key, short value) {
			
			if (key == null)
			{
				NullKeyValue = value;
				return !hasNullKey && (hasNullKey = true);
			}
			
			int slot = array.hashCode(key) & mask;
			
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key))
				{
					values[slot] = (short) value;
					return false;
				}
			
			keys[slot]   = key;
			values[slot] = (short) value;
			
			if (assigned++ == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean remove(K key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = array.hashCode(key) & mask;
			
			for (K k; (k = keys[slot]) != null; slot = slot + 1 & mask)
				if (array.equals(k, key))
				{
					short[] vals = values;
					
					
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
					vals[gapSlot] = (short) 0;
					assigned--;
					return true;
				}
			
			return false;
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
				if (values.length < size) values = new short[size];
				return;
			}
			
			final K[]           k = keys;
			final short[] v = values;
			
			keys   = array.copyOf(null, size);
			values = new short[size];
			
			K key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != null)
				{
					int slot = array.hashCode(key) & mask;
					while (!(keys[slot] == null)) slot = slot + 1 & mask;
					
					keys[slot]   = key;
					values[slot] = v[i];
				}
		}
		
		public RW<K> clone() {return (RW<K>) super.clone();}
	}
}
	