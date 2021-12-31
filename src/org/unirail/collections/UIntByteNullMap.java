package org.unirail.collections;


import static org.unirail.collections.Array.hash;

public interface UIntByteNullMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {
			int len = src.keys.length;
			for (token++, token &= Integer.MAX_VALUE; ; token++)
				if (token == len) return src.has0key == Positive_Values.NONE ? INIT : -2;
				else if (token == 0x7FFF_FFFF) return INIT;
				else if (src.keys[token] != 0) return src.values.hasValue(token) ? token : token | Integer.MIN_VALUE;
		}
		
		static long key(R src, int token) {return token == -2 ? 0 : src.keys[token & Integer.MAX_VALUE];}
		
		static boolean hasValue(R src, int token) {return (token == -2 && src.has0key == Positive_Values.VALUE) || -1 < token;}
		
		static byte value(R src, int token) {return token == -2 ? src.OkeyValue :    src.values.get(token);}
	}
	
	abstract class R implements Cloneable {
		
		
		public int[]          keys   = Array.ints0     ;
		public ByteNullList.RW values = new ByteNullList.RW(0);
		
		int assigned;
		int mask;
		int resizeAt;
		
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		byte nullKeyValue = 0;
		
		
		@Positive_Values int has0key = Positive_Values.NONE;
		byte       OkeyValue;
		
		
		protected double loadFactor;
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (has0key == Positive_Values.NONE ? 0 : 1) + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		
		public int hashCode() {
			int hash = hasNullKey == Positive_Values.VALUE ? hash(nullKeyValue) : hasNullKey == Positive_Values.NONE ? 436195789 : 22121887;
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
			{
				hash = hash(hash, NonNullKeysIterator.key(this, token));
				if (NonNullKeysIterator.hasValue(this, token)) hash = hash(hash, hash((NonNullKeysIterator.value(this, token))));
			}
			
			return hash;
		}
		
		public boolean contains( Integer   key)           {return !hasNone(token(key));}
		
		public boolean contains(long key)               {return !hasNone(token(key));}
		
		public @Positive_Values int token( Integer   key) {return key == null ? hasNullKey : token((long) (key + 0));}
		
		public @Positive_Values int token(long key) {
			
			if (key == 0) return has0key == Positive_Values.VALUE ? Positive_Values.VALUE - 1 : has0key;
			
			final int key_ = (int) key ;
			
			int slot = hash(key) & mask;
			
			for (int k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return (values.hasValue(slot)) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;//the key is not present
		}
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public boolean hasNull(int token)  {return token == Positive_Values.NULL;}
		
		public byte value(@Positive_ONLY int token) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			if (token == Positive_Values.VALUE - 1) return OkeyValue;
			return values.get(token);
		}
		
		
		public @Positive_Values int hasNullKey() {return hasNullKey;}
		
		public byte nullKeyValue() {return nullKeyValue;}
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R other) {
			if (other == null || hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && nullKeyValue != other.nullKeyValue
			    || has0key != other.has0key || has0key == Positive_Values.VALUE && OkeyValue != other.OkeyValue || size() != other.size()) return false;
			
			
			long           key;
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
			
			switch (hasNullKey)
			{
				case Positive_Values.VALUE:
					dst.append("null -> ").append(nullKeyValue).append('\n');
					break;
				case Positive_Values.NULL:
					dst.append("null -> null\n");
			}
			
			switch (has0key)
			{
				case Positive_Values.VALUE:
					dst.append("0 -> ").append(OkeyValue).append('\n');
					break;
				case Positive_Values.NULL:
					dst.append("0 -> null\n");
			}
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; dst.append('\n'))
			{
				dst.append(NonNullKeysIterator.key(this, token)).append(" -> ");
				if (NonNullKeysIterator.hasValue(this, token)) dst.append(NonNullKeysIterator.value(this, token));
				else dst.append("null");
			}
			
			return dst;
		}
	}
	
	class RW extends R {
		
		
		public RW(int expectedItems) {this(expectedItems, 0.75);}
		
		
		public RW(int expectedItems, double loadFactor) {
			this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys = new int[size];
			values.length(size);
		}
		
		
		public boolean put( Integer   key, byte value) {
			if (key != null) return put((long) (key + 0), value);
			
			int h = hasNullKey;
			hasNullKey = Positive_Values.VALUE;
			nullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put( Integer   key,  Byte      value) {
			if (key != null) return put((long) (key + 0), value);
			
			int h = hasNullKey;
			
			if (value == null)
			{
				hasNullKey = Positive_Values.NULL;
				return h == Positive_Values.NULL;
			}
			
			hasNullKey = Positive_Values.VALUE;
			nullKeyValue = value;
			return h == Positive_Values.VALUE;
		}
		
		public boolean put(long key,  Byte      value) {
			if (value != null) return put(key, (byte) value);
			
			if (key == 0)
			{
				int h = has0key;
				has0key = Positive_Values.NULL;
				return h == Positive_Values.NONE;
			}
			
			int               slot = hash(key) & mask;
			final int key_ = (int) key ;
			
			for (int k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					values.set(slot, ( Byte     ) null);
					return false;
				}
			
			keys[slot] = key_;
			values.set(slot, ( Byte     ) null);
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean put(long key, byte value) {
			if (key == 0)
			{
				int h = has0key;
				has0key = Positive_Values.VALUE;
				OkeyValue = value;
				return h == Positive_Values.NONE;
			}
			
			int slot = hash(key) & mask;
			
			final int key_ = (int) key ;
			
			for (int k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
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
		
		
		public boolean remove() {return has0key != Positive_Values.NONE && (has0key = Positive_Values.NONE) == Positive_Values.NONE;}
		
		public boolean remove(long key) {
			if (key == 0) return remove();
			
			int slot = hash(key) & mask;
			
			final int key_ = (int) key ;
			
			for (int k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_)
				{
					int gapSlot = slot;
					
					int kk;
					
					for (int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hash(kk) & mask) >= distance)
						{
							
							keys[gapSlot] = kk;
							
							if (values.nulls.get(s))
								values.set(gapSlot, values.get(s));
							else
								values.set(gapSlot, ( Byte     ) null);
							
							gapSlot = s;
							distance = 0;
						}
					
					keys[gapSlot] = 0;
					values.set(gapSlot, ( Byte     ) null);
					assigned--;
					return true;
				}
			return false;
		}
		
		void allocate(int size) {
			
			if (assigned < 1)
			{
				resizeAt = Math.min(size - 1, (int) Math.ceil(size * loadFactor));
				mask = size - 1;
				
				if (keys.length < size) keys = new int[size];
				if (values.nulls.length() < size|| values.values.values.length < size) values.length (size);
				
				return;
			}
			
			RW tmp = new RW(size - 1, loadFactor);
			
			int[] k = keys;
			int   key;
			
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
					if (values.nulls.get(i)) tmp.put( key, values.get(i));
					else tmp.put( key, null);
			
			keys = tmp.keys;
			values = tmp.values;
			
			assigned = tmp.assigned;
			mask = tmp.mask;
			resizeAt = tmp.resizeAt;
		}
		
		public void clear() {
			assigned = 0;
			has0key = Positive_Values.NONE;
			values.clear();
		}
		
		public RW clone() {return (RW) super.clone();}
	}
}
