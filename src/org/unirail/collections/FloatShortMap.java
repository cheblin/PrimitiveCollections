package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.Array.hash;

public interface FloatShortMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {
			for (int len = src.keys.length; ; )
				if (++token == len) return src.has0Key ? len : INIT;
				else if (token == len + 1) return INIT;
				else if (src.keys[token] != 0) return token;
		}
		
		static float key(R src, int token) {return token == src.keys.length ? 0 :   src.keys[token];}
		
		static short value(R src, int token) {return token == src.keys.length ? src.OKeyValue :  (short) src.values[token];}
	}
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		float[] keys   = Array.Of.floats     .O;
		short[] values = Array.Of.shorts     .O;
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNullKey;
		short       nullKeyValue;
		
		boolean has0Key;
		short       OKeyValue;
		
		protected double loadFactor;
		
		
		public boolean isEmpty()                               {return size() == 0;}
		
		public int size()                                      {return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		public boolean contains( Float     key)           {return !hasNone(token(key));}
		
		public boolean contains(float key)               {return !hasNone(token(key));}
		
		public @Positive_Values int token( Float     key) {return key == null ? hasNullKey ? keys.length + 1 : Positive_Values.NONE : token((float) (key + 0));}
		
		public @Positive_Values int token(float key) {
			if (key == 0) return has0Key ? keys.length : Positive_Values.NONE;
			
			int slot = hash(key) & mask;
			
			for (float key_ =  key , k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return Positive_Values.NONE;
		}
		
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public short value(@Positive_ONLY int token) {
			if (token == keys.length + 1) return nullKeyValue;
			if (token == keys.length) return OKeyValue;
			return (short) values[token];
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
			
			float           key;
			for (int i = keys.length, c; -1 < --i; )
				if ((key =  keys[i]) != 0)
				{
					if ((c = other.token(key)) < 0) return false;
					if (other.value(c) !=  (short) values[i]) return false;
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
			          getK == null ? getK = (Array.ISort.Primitives.floats) index  ->  keys[index] : getK :
			          getV == null ? getV = index -> (short) values[index] : getV;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != 0) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public void build(Array.ISort.Primitives.Index2 dst, boolean K) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = K ?
			          getK == null ? getK = (Array.ISort.Primitives.floats) index  ->  keys[index] : getK :
			          getV == null ? getV = index -> (short) values[index] : getV;
			
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != 0) dst.dst[k++] = i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		
		public String toString() {return toJSON();}
		@Override public void toJSON(JsonWriter json) {
			json.enterObject();
			
			int size = size(), token = NonNullKeysIterator.INIT;
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
			
			keys   = new float[size];
			values = new short[size];
		}
		
		
		public boolean put( Float     key, short value) {
			if (key != null) return put((float) key, value);
			
			hasNullKey   = true;
			nullKeyValue = value;
			
			return true;
		}
		
		public boolean put(float key, short value) {
			
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
			
			final float key_ =  key;
			
			for (float k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values[slot] = (short) value;
					return true;
				}
			
			keys[slot]   = key_;
			values[slot] = (short) value;
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return false;
		}
		
		
		public boolean remove( Float     key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			return remove((float) key);
		}
		
		public boolean remove(float key) {
			
			if (key == 0) return has0Key && !(has0Key = false);
			
			int slot = hash(key) & mask;
			
			final float key_ =  key;
			
			for (float k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					float kk;
					
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
				if (keys.length < size) keys = new float[size];
				if (values.length < size) values = new short[size];
				return;
			}
			
			final float[] k = keys;
			final short[] v = values;
			
			keys   = new float[size];
			values = new short[size];
			
			float key;
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
