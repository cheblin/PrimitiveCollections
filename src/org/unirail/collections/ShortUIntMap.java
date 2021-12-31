package org.unirail.collections;


import static org.unirail.collections.Array.hash;

public interface ShortUIntMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {
			for (int len = src.keys.length; ; )
				if (++token == len) return src.has0Key ? -2 : INIT;
				else if (token == 0x7FFF_FFFF) return INIT;
				else if (src.keys[token] != 0) return token;
		}
		
		static short key(R src, int token) {return token == -2 ? 0 :  (short) src.keys[token];}
		
		static long value(R src, int token) {return token == -2 ? src.OkeyValue :  (0xFFFFFFFFL &  src.values[token]);}
	}
	
	
	abstract class R implements Cloneable {
		short[] keys   = Array.shorts0     ;
		int[] values = Array.ints0     ;
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNullKey;
		long       nullKeyValue;
		
		boolean has0Key;
		long       OkeyValue;
		
		protected double loadFactor;
		
		
		public boolean isEmpty()                               {return size() == 0;}
		
		public int size()                                      {return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		public boolean contains( Short     key)           {return !hasNone(token(key));}
		
		public boolean contains(short key)               {return !hasNone(token(key));}
		
		public @Positive_Values int token( Short     key) {return key == null ? hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE : token((short) (key + 0));}
		
		public @Positive_Values int token(short key) {
			if (key == 0) return has0Key ? Positive_Values.VALUE - 1 : Positive_Values.NONE;
			
			int slot = hash(key) & mask;
			
			for (short key_ =  key , k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return Positive_Values.NONE;
		}
		
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public long value(@Positive_ONLY int token) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			if (token == Positive_Values.VALUE - 1) return OkeyValue;
			
			return (0xFFFFFFFFL &  values[token]);
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
			    hasNullKey != other.hasNullKey || has0Key && OkeyValue != other.OkeyValue
			    || hasNullKey && nullKeyValue != other.nullKeyValue || size() != other.size()) return false;
			
			short           key;
			for (int i = keys.length, c; -1 < --i; )
				if ((key = (short) keys[i]) != 0)
				{
					if ((c = other.token(key)) < 0) return false;
					if (other.value(c) !=  (0xFFFFFFFFL &  values[i])) return false;
				}
			
			return true;
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
		
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (hasNullKey) dst.append("null -> ").append(nullKeyValue).append('\n');
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				dst.append(NonNullKeysIterator.key(this, token))
						.append(" -> ")
						.append(NonNullKeysIterator.value(this, token))
						.append('\n');
			return dst;
		}
	}
	
	
	class RW extends R {
		
		public RW(int expectedItems) {this(expectedItems, 0.75);}
		
		
		public RW(int expectedItems, double loadFactor) {
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(size - 1, (int) Math.ceil(size * loadFactor));
			mask = size - 1;
			
			keys = new short[size];
			values = new int[size];
		}
		
		
		public boolean put( Short     key, long value) {
			if (key != null) return put((short) key, value);
			
			hasNullKey = true;
			nullKeyValue = value;
			
			return true;
		}
		
		public boolean put(short key, long value) {
			
			if (key == 0)
			{
				if (has0Key)
				{
					OkeyValue = value;
					return true;
				}
				has0Key = true;
				OkeyValue = value;
				return false;
			}
			
			
			int slot = hash(key) & mask;
			
			final short key_ =  key;
			
			for (short k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values[slot] = (int) value;
					return true;
				}
			
			keys[slot] = key_;
			values[slot] = (int) value;
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return false;
		}
		
		
		public boolean remove( Short     key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			return remove((short) key);
		}
		
		public boolean remove(short key) {
			
			if (key == 0) return has0Key && !(has0Key = false);
			
			int slot = hash(key) & mask;
			
			final short key_ =  key;
			
			for (short k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					short kk;
					
					for (int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hash(kk) & mask) >= distance)
						{
							
							keys[gapSlot] = kk;
							values[gapSlot] = values[s];
							gapSlot = s;
							distance = 0;
						}
					
					keys[gapSlot] = 0;
					values[gapSlot] = 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			has0Key = false;
			hasNullKey = false;
		}
		
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length < size) keys = new short[size];
				if (values.length < size) values = new int[size];
				return;
			}
			
			final short[] k = keys;
			final int[] v = values;
			
			keys = new short[size];
			values = new int[size];
			
			short key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = hash(key) & mask;
					while (keys[slot] != 0) slot = slot + 1 & mask;
					keys[slot] = key;
					values[slot] = v[i];
				}
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}
