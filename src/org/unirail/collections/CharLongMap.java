package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.Array.hash;

public interface CharLongMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {
			for (int len = src.keys.length; ; )
				if (++token == len) return src.has0Key ? len : INIT;
				else if (token == len + 1) return INIT;
				else if (src.keys[token] != 0) return token;
		}
		
		static char key(R src, int token) {return token == src.keys.length ? 0 :  (char) src.keys[token];}
		
		static long value(R src, int token) {return token == src.keys.length ? src.OKeyValue :   src.values[token];}
	}
	
	
	abstract class R implements Cloneable , JsonWriter.Client{
		char[] keys   = Array.Of.chars     .O;
		long[] values = Array.Of.longs     .O;
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNullKey;
		long       nullKeyValue;
		
		boolean has0Key;
		long       OKeyValue;
		
		protected double loadFactor;
		
		
		public boolean isEmpty()                               {return size() == 0;}
		
		public int size()                                      {return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		public boolean contains( Character key)           {return !hasNone(token(key));}
		
		public boolean contains(char key)               {return !hasNone(token(key));}
		
		public @Positive_Values int token( Character key) {return key == null ? hasNullKey ? keys.length + 1 : Positive_Values.NONE : token((char) (key + 0));}
		
		public @Positive_Values int token(char key) {
			if (key == 0) return has0Key ? keys.length : Positive_Values.NONE;
			
			int slot = hash(key) & mask;
			
			for (char key_ =  key , k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return Positive_Values.NONE;
		}
		
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public long value(@Positive_ONLY int token) {
			if (token == keys.length + 1) return nullKeyValue;
			if (token == keys.length) return OKeyValue;
			return  values[token];
		}
		
		public int hashCode() {
			int hash = hasNullKey ? hash(nullKeyValue) : 331997;
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
			     hash = hash(hash(hash, NonNullKeysIterator.key(this, token)), NonNullKeysIterator.value(this, token));
			return hash;
		}
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R other) {
			if (other == null ||
			    has0Key != other.has0Key ||
			    hasNullKey != other.hasNullKey || has0Key && OKeyValue != other.OKeyValue
			    || hasNullKey && nullKeyValue != other.nullKeyValue || size() != other.size()) return false;
			
			char           key;
			for (int i = keys.length, c; -1 < --i; )
				if ((key = (char) keys[i]) != 0)
				{
					if ((c = other.token(key)) < 0) return false;
					if (other.value(c) !=   values[i]) return false;
				}
			
			return true;
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
		
		private Array.ISort.Primitives getK = null;
		private Array.ISort.Primitives getV = null;
		
		public void build(Array.ISort.Primitives.Index dst, boolean K) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new char[assigned];
			dst.size = assigned;
			
			dst.src = K ?
			          getK == null ? getK = index -> (char) keys[index] : getK :
			          getV == null ? getV = index ->  values[index] : getV;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != 0) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public void build(Array.ISort.Primitives.Index2 dst, boolean K) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = K ?
			          getK == null ? getK = index -> (char) keys[index] : getK :
			          getV == null ? getV = index ->  values[index] : getV;
			
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != 0) dst.dst[k++] = i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		
public String toString() {return toJSON();}
		@Override public void toJSON(JsonWriter json)  {
			json.enterObject();
			
			int size = size(),  token = NonNullKeysIterator.INIT;
			if (0 < size)
			{
				json.preallocate(size * 10);
				while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
					json.
							name(NonNullKeysIterator.key(this, token)).
							value(NonNullKeysIterator.value(this, token));
			}
			
			json.exitObject();
		}
	}
	
	
	class RW extends R {
		
		public RW(int expectedItems) {this(expectedItems, 0.75);}
		
		
		public RW(int expectedItems, double loadFactor) {
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(size - 1, (int) Math.ceil(size * loadFactor));
			mask     = size - 1;
			
			keys   = new char[size];
			values = new long[size];
		}
		
		
		public boolean put( Character key, long value) {
			if (key != null) return put((char) key, value);
			
			hasNullKey   = true;
			nullKeyValue = value;
			
			return true;
		}
		
		public boolean put(char key, long value) {
			
			if (key == 0)
			{
				if (has0Key)
				{
					OKeyValue = value;
					return false;
				}
				has0Key   = true;
				OKeyValue = value;
				return true;
			}
			
			
			int slot = hash(key) & mask;
			
			final char key_ =  key;
			
			for (char k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values[slot] = (long) value;
					return true;
				}
			
			keys[slot]   = key_;
			values[slot] = (long) value;
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return false;
		}
		
		
		public boolean remove( Character key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			return remove((char) key);
		}
		
		public boolean remove(char key) {
			
			if (key == 0) return has0Key && !(has0Key = false);
			
			int slot = hash(key) & mask;
			
			final char key_ =  key;
			
			for (char k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					char kk;
					
					for (int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hash(kk) & mask) >= distance)
						{
							
							keys[gapSlot]   = kk;
							values[gapSlot] = values[s];
							                  gapSlot = s;
							                  distance = 0;
						}
					
					keys[gapSlot]   = 0;
					values[gapSlot] = 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned   = 0;
			has0Key    = false;
			hasNullKey = false;
		}
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length < size) keys = new char[size];
				if (values.length < size) values = new long[size];
				return;
			}
			
			final char[] k = keys;
			final long[] v = values;
			
			keys   = new char[size];
			values = new long[size];
			
			char key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = hash(key) & mask;
					while (keys[slot] != 0) slot = slot + 1 & mask;
					keys[slot]   = key;
					values[slot] = v[i];
				}
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
