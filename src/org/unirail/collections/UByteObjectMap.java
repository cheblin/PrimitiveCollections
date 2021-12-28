package org.unirail.collections;


import org.unirail.collections.Array.HashEqual;

import static org.unirail.collections.Array.HashEqual.hash;

public interface UByteObjectMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static <V> int token(R<V> src, int token) {return ByteSet.NonNullKeysIterator.token(src.keys, token);}
		
		static <V> char key(R<V> src, int token) {return (char) ByteSet.NonNullKeysIterator.key(src.keys, token);}
		
		static <V> V value(R<V> src, int token)   {return src.values[ByteSet.NonNullKeysIterator.index(src.keys, token)];}
	}
	
	abstract class R<V> implements Cloneable {
		
		public          ByteSet.RW   keys = new ByteSet.RW();
		public          V[]          values;
		protected final HashEqual<V> hash_equal;
		
		protected R(Class<V> clazz)               {hash_equal = HashEqual.get(clazz);}
		
		public int size()                         {return keys.size();}
		
		public boolean isEmpty()                  {return keys.isEmpty();}
		
		
		public boolean contains( Character key) {return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          {return keys.contains((byte) key);}
		
		public V value( Character key)          {return key == null ? NullKeyValue : value(key + 0);}
		
		public V value(int key)                   {return values[keys.rank((byte) key)];}
		
		V NullKeyValue = null;
		
		public int hashCode() {return hash(HashEqual.hash(HashEqual.hash(keys.contains(null) ? hash(NullKeyValue) : 29399999), keys), hash_equal.hashCode(values, size()));}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		public boolean equals(R<V> other) {return other != null && keys.equals(other.keys) && hash_equal.equals(values, other.values, size()) && (!keys.hasNullKey || NullKeyValue == other.NullKeyValue);}
		
		
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
	
	class RW<V> extends R<V> implements Array {
		
		public RW(Class<V> clazz, int length) {
			super(clazz);
			values = hash_equal.copyOf(null, 265 < length ? 256 : length);
		}
		
		public void clear() {
			NullKeyValue = null;
			java.util.Arrays.fill(values, 0, size() - 1, null);
			keys.clear();
		}
		
		public boolean put( Character key, V value) {
			if (key == null)
			{
				NullKeyValue = value;
				return keys.add(null);
			}
			
			return put((char) (key + 0), value);
		}
		
		public boolean put(char key, V value) {
			boolean ret = keys.add((byte) key);
			values[keys.rank((byte) key) - 1] = value;
			return ret;
		}
		
		public boolean remove( Character  key) {
			if (key == null)
			{
				NullKeyValue = null;
				return keys.remove(null);
			}
			return remove((char) (key + 0));
		}
		
		public boolean remove(char key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			Array.resize(this, size(), keys.rank(k) - 1, -1);
			keys.remove(k);
			
			return true;
		}
		
		
		public RW<V> clone() {return (RW<V>) super.clone();}
		
		@SuppressWarnings("unchecked")
		public Object[] length(int length) {
			if (0 < length) return values = hash_equal.copyOf(values, length);
			
			return values = hash_equal.copyOf(null, -length);
		}
		
		@Override public int length() {return values.length;}
		@Override public Object array() {
			return values;
		}
	}
	
	
}
