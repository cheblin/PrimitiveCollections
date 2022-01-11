package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.Array.hash;

public interface UByteObjectMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static <V> int token(R<V> src, int token) {return ByteSet.NonNullKeysIterator.token(src.keys, token);}
		
		static <V> char key(R<V> src, int token) {return (char) ByteSet.NonNullKeysIterator.key(src.keys, token);}
		
		static <V> V value(R<V> src, int token)   {return src.values[ByteSet.NonNullKeysIterator.index(src.keys, token)];}
	}
	
	abstract class R<V> implements Cloneable , JsonWriter.Source {
		
		public          ByteSet.RW  keys = new ByteSet.RW();
		public          V[]         values;
		protected final Array.Of<V> array;
		private final   char        s;
		protected R(Class<V> clazz) {
			array = Array.get(clazz);
			s     = clazz == String.class ? '"' : '\0';
		}
		
		public int size()                         {return keys.size();}
		
		public boolean isEmpty()                  {return keys.isEmpty();}
		
		
		public boolean contains( Character key) {return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          {return keys.contains((byte) key);}
		
		public V value( Character key)          {return key == null ? NullKeyValue : value(key + 0);}
		
		public V value(int key)                   {return values[keys.rank((byte) key)];}
		
		V NullKeyValue = null;
		
		public int hashCode()                                            {return hash(Array.hash(Array.hash(keys.contains(null) ? hash(NullKeyValue) : 29399999), keys), array.hashCode(values, size()));}
		
		@SuppressWarnings("unchecked") public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals(getClass().cast(obj));}
		
		public boolean equals(R<V> other)                                {return other != null && keys.equals(other.keys) && array.equals(values, other.values, size()) && (!keys.hasNullKey || NullKeyValue == other.NullKeyValue);}
		
		
		@SuppressWarnings("unchecked") public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			return null;
		}
		
		public Array.ISort.Objects<V> getV = null;
		
		
		public void build(Array.ISort.Anything.Index dst) {
			if (dst.dst == null || dst.dst.length < size()) dst.dst = new char[size()];
			dst.size = size();
			
			dst.src = getV == null ? getV = new Array.ISort.Objects<V>() {
				@Override V get(int index) {return values[index];}
			} : getV;
			
			for (int token = NonNullKeysIterator.INIT, k = 0; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
			     dst.dst[k++] = (char) token;
			
			Array.ISort.sort(dst, 0, dst.size - 1);
		}
		
		public void build(Array.ISort.Anything.Index2 dst) {
			if (dst.dst == null || dst.dst.length < size()) dst.dst = new int[size()];
			dst.size = size();
			
			dst.src = getV == null ? getV = new Array.ISort.Objects<V>() {
				@Override V get(int index) {return values[index];}
			} : getV;
			
			for (int token = NonNullKeysIterator.INIT, k = 0; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
			     dst.dst[k++] = (char) token;
			
			Array.ISort.sort(dst, 0, dst.size - 1);
		}
		
		
		public String toString() {return toJSON();}
		@Override public void toJSON(JsonWriter json) {
			json.enterObject();
			
			int size = keys.size();
			if (0 < size)
			{
				json.preallocate(size * 10);
				
				if (keys.hasNullKey) json.name().value(NullKeyValue);
				
				for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				     json.
						     name(NonNullKeysIterator.key(this, token)).
						     value(NonNullKeysIterator.value(this, token));
			}
			
			json.exitObject();
		}
	}
	
	class RW<V> extends R<V> {
		
		public RW(Class<V> clazz, int length) {
			super(clazz);
			values = array.copyOf(null, 265 < length ? 256 : length);
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
			
			org.unirail.collections.Array.resize(values, values, keys.rank(k) - 1, size(), -1);
			
			keys.remove(k);
			
			return true;
		}
		
		
		public RW<V> clone() {return (RW<V>) super.clone();}
		
	}
	
	
}
