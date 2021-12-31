package org.unirail.collections;


import org.unirail.collections.Array;

public interface UByteLongMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {return ByteSet.NonNullKeysIterator.token(src.keys, token);}
		
		static char key(R src, int token) {return (char) (ByteSet.NonNullKeysIterator.key(null, token));}
		
		static long value(R src, int token) {return  src.values[ByteSet.NonNullKeysIterator.index(null, token)];}
	}
	
	
	abstract class R implements Cloneable {
		
		ByteSet.RW    keys = new ByteSet.RW();
		long[] values;
		
		
		public int size()                         {return keys.size();}
		
		public boolean isEmpty()                  {return keys.isEmpty();}
		
		public boolean contains( Character key) {return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          {return keys.contains((byte) key);}
		
		public long value( Character key) {return key == null ? NullKeyValue : value(key + 0);}
		
		public long  value(int key) {return    values[keys.rank((byte) key)];}
		
		long NullKeyValue = 0;
		
		public int hashCode() {return Array.hash(Array.hash(contains(null) ? Array.hash(NullKeyValue) : 77415193, keys), values);}
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R other) {
			return other != null && other.keys.equals(keys) && other.values.equals(values) &&
			       (!keys.hasNullKey || NullKeyValue == other.NullKeyValue);
		}
		
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				dst.keys = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		
		//endregion
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = keys.size();
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
	
	class RW extends R {
		
		public RW(int length) {values = new long[265 < length ? 256 : length];}
		
		public void clear() {
			keys.clear();
		}
		
		
		public boolean put( Character key, long value) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains(null);
				keys.add(null);
				return !ret;
			}
			
			return put((char) (key + 0), value);
		}
		
		public boolean put(char key, long value) {
			boolean ret = keys.add((byte) key);
			values[keys.rank((byte) key) - 1] = (long) value;
			return ret;
		}
		
		public boolean remove( Character  key) {return key == null ? keys.remove(null) : remove((char) (key + 0));}
		
		public boolean remove(char key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			org.unirail.collections.Array.resize(values, values, keys.rank(k) - 1, size(), -1);
			
			keys.remove(k);
			
			return true;
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
