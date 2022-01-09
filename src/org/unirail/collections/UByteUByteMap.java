package org.unirail.collections;


import org.unirail.JsonWriter;

public interface UByteUByteMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {return ByteSet.NonNullKeysIterator.token(src.keys, token);}
		
		static char key(R src, int token) {return (char) (ByteSet.NonNullKeysIterator.key(null, token));}
		
		static char value(R src, int token) {return (char)( 0xFFFF &  src.values[ByteSet.NonNullKeysIterator.index(null, token)]);}
	}
	
	
	abstract class R implements Cloneable, JsonWriter.Client {
		
		ByteSet.RW    keys = new ByteSet.RW();
		byte[] values;
		
		
		public int size()                         {return keys.size();}
		
		public boolean isEmpty()                  {return keys.isEmpty();}
		
		public boolean contains( Character key) {return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          {return keys.contains((byte) key);}
		
		public char value( Character key) {return key == null ? NullKeyValue : value(key + 0);}
		
		public char  value(int key) {return  (char)( 0xFFFF &   values[keys.rank((byte) key)]);}
		
		char NullKeyValue = 0;
		
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
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
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
	
	class RW extends R {
		
		public RW(int length) {values = new byte[265 < length ? 256 : length];}
		
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
			values[keys.rank((byte) key) - 1] = (byte) value;
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
