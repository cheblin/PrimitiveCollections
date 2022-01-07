package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.Array.hash;

public interface UShortLongNullMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {
			int len = src.keys.length;
			for (token++; ; token++)
				if (token == len) return src.has0Key == Positive_Values.NONE ? INIT : len;
				else if (token == len + 1) return INIT;
				else if (src.keys[token] != 0) return token;
		}
		
		static char key(R src, int token) {return token == src.keys.length ? 0 : src.keys[token];}
		
		static boolean hasValue(R src, int token) {return src.values.hasValue(token);}
		
		static long value(R src, int token) {return token == src.keys.length ? src.OKeyValue :    src.values.get(token);}
	}
	
	abstract class R implements Cloneable {
		
		
		public char[]          keys   = Array.Of.chars     .O;
		public LongNullList.RW values = new LongNullList.RW(0);
		
		int assigned;
		int mask;
		int resizeAt;
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		long NullKeyValue = 0;
		
		
		@Positive_Values int has0Key = Positive_Values.NONE;
		long       OKeyValue;
		
		
		protected double loadFactor;
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (has0Key == Positive_Values.NONE ? 0 : 1) + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		
		public int hashCode() {
			int hash = hasNullKey == Positive_Values.VALUE ? hash(NullKeyValue) : hasNullKey == Positive_Values.NONE ? 436195789 : 22121887;
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
			{
				hash = hash(hash, NonNullKeysIterator.key(this, token));
				if (NonNullKeysIterator.hasValue(this, token)) hash = hash(hash, hash((NonNullKeysIterator.value(this, token))));
			}
			
			return hash;
		}
		
		public boolean contains( Character key)           {return !hasNone(token(key));}
		
		public boolean contains(char key)               {return !hasNone(token(key));}
		
		public @Positive_Values int token( Character key) {return key == null ? hasNullKey : token((char) (key + 0));}
		
		public @Positive_Values int token(char key) {
			
			if (key == 0) return has0Key == Positive_Values.VALUE ? keys.length : has0Key;
			
			final char key_ =  key;
			
			int slot = hash(key) & mask;
			
			for (char k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return (values.hasValue(slot)) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;//the key is not present
		}
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public boolean hasNull(int token)  {return token == Positive_Values.NULL;}
		
		public long value(@Positive_ONLY int token) {
			if (token == keys.length + 1) return NullKeyValue;
			if (token == keys.length) return OKeyValue;
			return values.get(token);
		}
		
		
		public @Positive_Values int hasNullKey() {return hasNullKey;}
		
		public long nullKeyValue() {return NullKeyValue;}
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R other) {
			if (other == null || hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && NullKeyValue != other.NullKeyValue
			    || has0Key != other.has0Key || has0Key == Positive_Values.VALUE && OKeyValue != other.OKeyValue || size() != other.size()) return false;
			
			
			char           key;
			for (int i = keys.length; -1 < --i; )
				if ((key =  keys[i]) != 0)
					if (values.nulls.get(i))
					{
						int ii = other.token(key);
						if (ii < 0 || values.get(i) != other.value(ii)) return false;
					}
					else if (-1 < other.token(key)) return false;
			
			return true;
		}
		
		
		public R clone() {
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
			          getK == null ? getK = index ->  keys[index] : getK :
			          getV == null ? getV = index -> values.hasValue(index) ?  value(index) : 0 : getV;
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != 0) dst.dst[k++] = (char) i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public void build(Array.ISort.Primitives.Index2 dst, boolean K) {
			if (dst.dst == null || dst.dst.length < assigned) dst.dst = new int[assigned];
			dst.size = assigned;
			
			dst.src = K ?
			          getK == null ? getK = index ->  keys[index] : getK :
			          getV == null ? getV = index -> values.hasValue(index) ?  value(index) : 0 : getV;
			
			
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != 0) dst.dst[k++] = i;
			Array.ISort.sort(dst, 0, assigned - 1);
		}
		
		public String toString() {
			final JsonWriter        json   = JsonWriter.get();
			final JsonWriter.Config config = json.enter();
			json.enterObject();
			
			switch (hasNullKey)
			{
				case Positive_Values.VALUE:
					json.name(null).value(NullKeyValue);
					break;
				case Positive_Values.NULL:
					json.name(null).value();
			}
			
			int size = size(), i=0,token = NonNullKeysIterator.INIT;
			if (0 < size)
			{
				json.preallocate(size * 10);
				
				if (json.orderByKey())
					for (build(json.primitiveIndex, true); i < json.primitiveIndex.size; i++)
					{
						json.name(NonNullKeysIterator.key(this, token = json.primitiveIndex.dst[i]));
						if (NonNullKeysIterator.hasValue(this, token)) json.value(NonNullKeysIterator.value(this, token));
						else json.value();
					}
				else
					while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
					{
						json.name(NonNullKeysIterator.key(this, token));
						if (NonNullKeysIterator.hasValue(this, token)) json.value(NonNullKeysIterator.value(this, token));
						else json.value();
					}
			}
			
			json.exitObject();
			return json.exit(config);
		}
		
	}
	
	class RW extends R {
		
		
		public RW(int expectedItems) {this(expectedItems, 0.75);}
		
		
		public RW(int expectedItems, double loadFactor) {
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys = new char[size];
			values.length(size);
		}
		
		
		public boolean put( Character key, long value) {
			if (key != null) return put((char) (key + 0), value);
			
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			NullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put( Character key,  Long      value) {
			if (key != null) return put((char) (key + 0), value);
			
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
		
		public boolean put(char key,  Long      value) {
			if (value != null) return put(key, (long) value);
			
			if (key == 0)
			{
				int h = has0Key;
				has0Key = Positive_Values.NULL;
				return h == Positive_Values.NONE;
			}
			
			int               slot = hash(key) & mask;
			final char key_ =  key ;
			
			for (char k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set(slot, ( Long     ) null);
					return false;
				}
			
			keys[slot] = key_;
			values.set(slot, ( Long     ) null);
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean put(char key, long value) {
			if (key == 0)
			{
				int h = has0Key;
				has0Key   = Positive_Values.VALUE;
				OKeyValue = value;
				return h == Positive_Values.NONE;
			}
			
			int slot = hash(key) & mask;
			
			final char key_ =  key;
			
			for (char k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set(slot, value);
					return true;
				}
			
			keys[slot] = key_;
			values.set(slot, value);
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean remove() {return has0Key != Positive_Values.NONE && (has0Key = Positive_Values.NONE) == Positive_Values.NONE;}
		
		public boolean remove(char key) {
			if (key == 0) return remove();
			
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
							
							keys[gapSlot] = kk;
							
							if (values.nulls.get(s))
								values.set(gapSlot, values.get(s));
							else
								values.set(gapSlot, ( Long     ) null);
							
							gapSlot  = s;
							distance = 0;
						}
					
					keys[gapSlot] = 0;
					values.set(gapSlot, ( Long     ) null);
					assigned--;
					return true;
				}
			return false;
		}
		
		void allocate(int size) {
			
			if (assigned < 1)
			{
				resizeAt = Math.min(size - 1, (int) Math.ceil(size * loadFactor));
				mask     = size - 1;
				
				if (keys.length < size) keys = new char[size];
				if (values.nulls.length() < size || values.values.values.length < size) values.length(size);
				
				return;
			}
			
			RW tmp = new RW(size - 1, loadFactor);
			
			char[] k = keys;
			char   key;
			
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
					if (values.nulls.get(i)) tmp.put( key, values.get(i));
					else tmp.put( key, null);
			
			keys   = tmp.keys;
			values = tmp.values;
			
			assigned = tmp.assigned;
			mask     = tmp.mask;
			resizeAt = tmp.resizeAt;
		}
		
		public void clear() {
			assigned = 0;
			has0Key  = Positive_Values.NONE;
			values.clear();
		}
		
		public RW clone() {return (RW) super.clone();}
	}
}
