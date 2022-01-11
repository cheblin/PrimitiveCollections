package org.unirail.collections;


import org.unirail.JsonWriter;

public interface ByteByteMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {return ByteSet.NonNullKeysIterator.token(src.keys, token);}
		
		static byte key(R src, int token) {return (byte) (ByteSet.NonNullKeysIterator.key(null, token));}
		
		static byte value(R src, int token) {return (byte) src.values[ByteSet.NonNullKeysIterator.index(null, token)];}
	}
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		ByteSet.RW    keys = new ByteSet.RW();
		byte[] values;
		
		
		public int size()                         {return keys.size();}
		
		public boolean isEmpty()                  {return keys.isEmpty();}
		
		public boolean contains( Byte      key) {return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          {return keys.contains((byte) key);}
		
		public byte value( Byte      key) {return key == null ? NullKeyValue : value(key + 0);}
		
		public byte  value(int key) {return  (byte)  values[keys.rank((byte) key)];}
		
		byte NullKeyValue = 0;
		
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
		
		
		public boolean put( Byte      key, byte value) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains(null);
				keys.add(null);
				return !ret;
			}
			
			return put((byte) (key + 0), value);
		}
		
		public boolean put(byte key, byte value) {
			boolean ret = keys.add((byte) key);
			values[keys.rank((byte) key) - 1] = (byte) value;
			return ret;
		}
		
		public boolean remove( Byte       key) {return key == null ? keys.remove(null) : remove((byte) (key + 0));}
		
		public boolean remove(byte key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			org.unirail.collections.Array.resize(values, values, keys.rank(k) - 1, size(), -1);
			
			keys.remove(k);
			
			return true;
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
