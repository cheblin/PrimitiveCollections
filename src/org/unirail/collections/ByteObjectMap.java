package org.unirail.collections;


import org.unirail.collections.Array.HashEqual;

import static org.unirail.collections.Array.HashEqual.hash;
public interface ByteObjectMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static <V> int token(R<V> src, int token) {return ByteSet.NonNullKeysIterator.token(src.keys, token);}
		
		static <V> byte key(R<V> src, int token) {return (byte) ByteSet.NonNullKeysIterator.key(src.keys, token);}
		
		static <V> V value(R<V> src, int token)   {return src.values.array[ByteSet.NonNullKeysIterator.index(src.keys, token)];}
	}
	
	abstract class R<V> implements Cloneable {
		
		public ByteSet.RW       keys = new ByteSet.RW();
		public ObjectList.RW<V> values;
		
		public int size()                         {return keys.size();}
		
		public boolean isEmpty()                  {return keys.isEmpty();}
		
		
		public boolean contains( Byte      key) {return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          {return keys.contains((byte) key);}
		
		public V value( Byte      key)          {return key == null ? NullKeyValue : value(key + 0);}
		
		public V value(int key)                   {return values.array[keys.rank((byte) key)];}
		
		V NullKeyValue = null;
		
		public int hashCode() {return hash(HashEqual.hash(HashEqual.hash(keys.contains(null) ? hash(NullKeyValue) : 29399999), keys), values);}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R<V> other) {return other != null && keys.equals(other.keys) && values.equals(other.values) && (!keys.hasNullKey || NullKeyValue == other.NullKeyValue);}
		
		
		@SuppressWarnings("unchecked")
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
				dst.keys = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			return null;
		}
		
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (keys.hasNullKey) dst.append("null -> ").append(NullKeyValue).append('\n');
			
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				dst.append(NonNullKeysIterator.key(this, token))
						.append(" -> ")
						.append(NonNullKeysIterator.value(this, token))
						.append('\n');
			
			return dst;
		}
	}
	
	class RW<V> extends R<V> {
		
		public RW(Class<V[]> clazz, int length) {values = new ObjectList.RW<>(clazz, 265 < length ? 256 : length);}
		
		public void clear() {
			NullKeyValue = null;
			values.clear();
			keys.clear();
		}
		
		public boolean put( Byte      key, V value) {
			if (key == null)
			{
				NullKeyValue = value;
				return keys.add(null);
			}
			
			return put((byte) (key + 0), value);
		}
		
		public boolean put(byte key, V value) {
			boolean ret = keys.add((byte) key);
			values.array[keys.rank((byte) key) - 1] = value;
			return ret;
		}
		
		public boolean remove( Byte       key) {
			if (key == null)
			{
				NullKeyValue = null;
				return keys.remove(null);
			}
			return remove((byte) (key + 0));
		}
		
		public boolean remove(byte key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			values.remove(keys.rank(k) - 1);
			keys.remove(k);
			
			return true;
		}
		
		
		public RW<V> clone() {return (RW<V>) super.clone();}
	}
}
