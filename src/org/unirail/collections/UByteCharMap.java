package org.unirail.collections;


import org.unirail.collections.Array.HashEqual;

import java.util.Arrays;

public interface UByteCharMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {return ByteSet.NonNullKeysIterator.token(src.keys, token);}
		
		static char key(R src, int token) {return (char) (ByteSet.NonNullKeysIterator.key(null, token));}
		
		static char value(R src, int token) {return (char) src.values[ByteSet.NonNullKeysIterator.index(null, token)];}
	}
	
	
	abstract class R implements Cloneable {
		
		ByteSet.RW    keys = new ByteSet.RW();
		char[] values;
		
		
		public int size()                         {return keys.size();}
		
		public boolean isEmpty()                  {return keys.isEmpty();}
		
		public boolean contains( Character key) {return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          {return keys.contains((byte) key);}
		
		public char value( Character key) {return key == null ? NullKeyValue : value(key + 0);}
		
		public char  value(int key) {return  (char)  values[keys.rank((byte) key)];}
		
		char NullKeyValue = 0;
		
		public int hashCode() {return HashEqual.hash(HashEqual.hash(contains(null) ? HashEqual.hash(NullKeyValue) : 77415193, keys), values);}
		
		
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
	
	class RW extends R implements Array {
		
		public RW(int length) {values = new char[265 < length ? 256 : length];}
		
		public void clear() {
			keys.clear();
		}
		
		
		public boolean put( Character key, char value) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains(null);
				keys.add(null);
				return !ret;
			}
			
			return put((char) (key + 0), value);
		}
		
		public boolean put(char key, char value) {
			boolean ret = keys.add((byte) key);
			values[keys.rank((byte) key) - 1] = (char) value;
			return ret;
		}
		
		public boolean remove( Character  key) {return key == null ? keys.remove(null) : remove((char) (key + 0));}
		
		public boolean remove(char key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			Array.resize( this, size(), keys.rank(k) - 1, -1 );
			keys.remove(k);
			
			return true;
		}
		@Override public Object array() {return values;}
		@Override public Object length(int length) {
			if (0 < length)
			{
				
				return Arrays.copyOf(values, length);
			}
			return values = length == 0 ? Array.chars0      : new char[-length];
		}
		@Override public int length() {return values.length;}
		@Override public RW clone()   {return (RW) super.clone();}
	}
}
