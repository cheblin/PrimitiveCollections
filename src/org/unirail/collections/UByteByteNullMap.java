package org.unirail.collections;


import org.unirail.JsonWriter;

public interface UByteByteNullMap {
	
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static int token(R src, int token)        {return ByteSet.NonNullKeysIterator.token(src.keys, token);}
		
		static char key(R src, int token) {return (char) (ByteSet.NonNullKeysIterator.key(src.keys, token));}
		
		static boolean hasValue(R src, int token) {return src.values.hasValue(ByteSet.NonNullKeysIterator.index(null, token));}
		
		static byte value(R src, int token) {return  src.values.get(ByteSet.NonNullKeysIterator.index(null, token));}
	}
	
	abstract class R implements Cloneable, JsonWriter.Source {
		
		ByteSet.RW             keys = new ByteSet.RW();
		ByteNullList.RW values;
		
		
		public int size()                                   {return keys.size();}
		
		public boolean isEmpty()                            {return keys.isEmpty();}
		
		
		public boolean contains( Character key)           {return !hasNone(token(key));}
		
		public boolean contains(char key)           {return !hasNone(token(key));}
		
		
		public @Positive_Values int token( Character key) {return key == null ? hasNullKey == Positive_Values.VALUE ? 256 : hasNullKey : token((char) (key + 0));}
		
		public @Positive_Values int token(char key) {
			if (keys.contains((byte) key))
			{
				int i = keys.rank((byte) key) - 1;
				return values.hasValue(i) ? i : Positive_Values.NULL;
			}
			return Positive_Values.NONE;
		}
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public boolean hasNull(int token)  {return token == Positive_Values.NULL;}
		
		public byte value(@Positive_ONLY int token) {return token == 256 ? NullKeyValue :    values.get((byte) token);}
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		byte NullKeyValue = 0;
		
		public int hashCode() {
			return Array.hash(Array.hash(hasNullKey == Positive_Values.NULL ? 553735009 : hasNullKey == Positive_Values.NONE ? 10019689 : Array.hash(NullKeyValue), keys), values);
		}
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R other) {
			return other != null && hasNullKey == other.hasNullKey &&
			       (hasNullKey != Positive_Values.VALUE || NullKeyValue == other.NullKeyValue)
			       && other.keys.equals(keys) && other.values.equals(values);
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
			
			switch (hasNullKey)
			{
				case Positive_Values.NULL:
					json.name().value();
					break;
				case Positive_Values.VALUE:
					if (keys.hasNullKey) json.name().value(NullKeyValue);
			}
			
			int size = keys.size;
			if (0 < size)
			{
				json.preallocate(size * 10);
				for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				{
					json.name(NonNullKeysIterator.key(this, token));
					if (NonNullKeysIterator.hasValue(this, token)) json.value();
					else json.value(NonNullKeysIterator.value(this, token));
				}
			}
			
			json.exitObject();
		}
	}
	
	class RW extends R {
		
		public RW(int length) {values = new ByteNullList.RW(265 < length ? 256 : length);}
		
		
		public void clear() {
			keys.clear();
			values.clear();
		}
		
		
		public boolean put( Character key, byte value) {
			if (key != null) return put((char) (key + 0), value);
			
			keys.add(null);
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			NullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put( Character key,  Byte      value) {
			if (key != null) return put((char) (key + 0), value);
			
			keys.add(null);
			int h = hasNullKey;
			
			if (value == null)
			{
				hasNullKey = Positive_Values.NULL;
				return h == Positive_Values.NULL;
			}
			
			hasNullKey   = Positive_Values.VALUE;
			NullKeyValue = value;
			return h == Positive_Values.VALUE;
		}
		
		
		public boolean put(char key,  Byte      value) {
			if (value != null) return put(key, (byte) value);
			
			
			if (keys.add((byte) key))
			{
				values.add(keys.rank((byte) key) - 1, ( Byte     ) null);
				return true;
			}
			values.set(keys.rank((byte) key) - 1, ( Byte     ) null);
			
			return false;
		}
		
		public boolean put(char key, byte value) {
			
			if (keys.add((byte) key))
			{
				values.add(keys.rank((byte) key) - 1, value);
				return true;
			}
			
			values.set(keys.rank((byte) key) - 1, value);
			return false;
		}
		
		public boolean remove( Character  key) {
			if (key == null)
			{
				hasNullKey = Positive_Values.NONE;
				keys.remove(null);
			}
			
			return remove((char) (key + 0));
		}
		
		public boolean remove(char key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			values.remove(keys.rank(k) - 1);
			keys.remove(k);
			return true;
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
