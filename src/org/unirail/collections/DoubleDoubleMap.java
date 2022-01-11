package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.Array.hash;

public interface DoubleDoubleMap {
	
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {
			for (int len = src.keys.length; ; )
				if (++token == len) return src.has0Key ? len : INIT;
				else if (token == len + 1) return INIT;
				else if (src.keys[token] != 0) return token;
		}
		
		static double key(R src, int token) {return token == src.keys.length ? 0 :   src.keys[token];}
		
		static double value(R src, int token) {return token == src.keys.length ? src.OKeyValue :   src.values[token];}
	}
	
	
	abstract class R implements Cloneable, JsonWriter.Source {
		double[] keys   = Array.Of.doubles     .O;
		double[] values = Array.Of.doubles     .O;
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		
		boolean hasNullKey;
		double       nullKeyValue;
		
		boolean has0Key;
		double       OKeyValue;
		
		protected double loadFactor;
		
		
		public boolean isEmpty()                               {return size() == 0;}
		
		public int size()                                      {return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		public boolean contains( Double    key)           {return !hasNone(token(key));}
		
		public boolean contains(double key)               {return !hasNone(token(key));}
		
		public @Positive_Values int token( Double    key) {return key == null ? hasNullKey ? keys.length + 1 : Positive_Values.NONE : token((double) (key + 0));}
		
		public @Positive_Values int token(double key) {
			if (key == 0) return has0Key ? keys.length : Positive_Values.NONE;
			
			int slot = hash(key) & mask;
			
			for (double key_ =  key , k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return slot;
			
			return Positive_Values.NONE;
		}
		
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public double value(@Positive_ONLY int token) {
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
			
			double           key;
			for (int i = keys.length, c; -1 < --i; )
				if ((key =  keys[i]) != 0)
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
			          getK == null ? getK = (Array.ISort.Primitives.doubles) index  ->  keys[index] : getK :
			          getV == null ? getV = (Array.ISort.Primitives.doubles) index  ->  values[index] : getV;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != 0) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public void build(Array.ISort.Primitives.Index2 dst, boolean K) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = K ?
			          getK == null ? getK = (Array.ISort.Primitives.doubles) index  ->  keys[index] : getK :
			          getV == null ? getV = (Array.ISort.Primitives.doubles) index  ->  values[index] : getV;
			
			
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
			
			keys   = new double[size];
			values = new double[size];
		}
		
		
		public boolean put( Double    key, double value) {
			if (key != null) return put((double) key, value);
			
			hasNullKey   = true;
			nullKeyValue = value;
			
			return true;
		}
		
		public boolean put(double key, double value) {
			
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
			
			final double key_ =  key;
			
			for (double k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values[slot] = (double) value;
					return true;
				}
			
			keys[slot]   = key_;
			values[slot] = (double) value;
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return false;
		}
		
		
		public boolean remove( Double    key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			return remove((double) key);
		}
		
		public boolean remove(double key) {
			
			if (key == 0) return has0Key && !(has0Key = false);
			
			int slot = hash(key) & mask;
			
			final double key_ =  key;
			
			for (double k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					double kk;
					
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
				if (keys.length < size) keys = new double[size];
				if (values.length < size) values = new double[size];
				return;
			}
			
			final double[] k = keys;
			final double[] v = values;
			
			keys   = new double[size];
			values = new double[size];
			
			double key;
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
