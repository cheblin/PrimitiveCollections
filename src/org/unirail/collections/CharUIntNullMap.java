package org.unirail.collections;


import static org.unirail.collections.Array.hash;

public interface CharUIntNullMap {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {
			int len = src.keys.length;
			for (token++, token &= Integer.MAX_VALUE; ; token++)
				if (token == len) return src.has0Key == Positive_Values.NONE ? INIT : Positive_Values.VALUE - 1;
				else if (token == Positive_Values.VALUE) return INIT;
				else if (src.keys[token] != 0) return src.values.hasValue(token) ? token : token | Integer.MIN_VALUE;
		}
		
		static char key(R src, int token) {return token == Positive_Values.VALUE - 1 ? 0 :(char) src.keys[token & Integer.MAX_VALUE];}
		
		static boolean hasValue(R src, int token) {return -1 < token;}
		
		static long value(R src, int token) {return token == Positive_Values.VALUE - 1 ? src.OKeyValue :    src.values.get(token);}
	}
	
	abstract class R implements Cloneable {
		
		
		public char[]          keys   = Array.chars0     ;
		public UIntNullList.RW values = new UIntNullList.RW(0);
		
		int assigned;
		int mask;
		int resizeAt;
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		long nullKeyValue = 0;
		
		
		@Positive_Values int has0Key = Positive_Values.NONE;
		long       OKeyValue;
		
		
		protected double loadFactor;
		
		public boolean isEmpty() {return size() == 0;}
		
		public int size()        {return assigned + (has0Key == Positive_Values.NONE ? 0 : 1) + (hasNullKey == Positive_Values.NONE ? 0 : 1);}
		
		
		public int hashCode() {
			int hash = hasNullKey == Positive_Values.VALUE ? hash(nullKeyValue) : hasNullKey == Positive_Values.NONE ? 436195789 : 22121887;
			
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
			
			if (key == 0) return has0Key == Positive_Values.VALUE ? Positive_Values.VALUE - 1 : has0Key;
			
			final char key_ =  key ;
			
			int slot = hash(key) & mask;
			
			for (char k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key_) return (values.hasValue(slot)) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;//the key is not present
		}
		
		public boolean hasValue(int token) {return -1 < token;}
		
		public boolean hasNone(int token)  {return token == Positive_Values.NONE;}
		
		public boolean hasNull(int token)  {return token == Positive_Values.NULL;}
		
		public long value(@Positive_ONLY int token) {
			if (token == Positive_Values.VALUE) return nullKeyValue;
			if (token == Positive_Values.VALUE - 1) return OKeyValue;
			return values.get(token);
		}
		
		
		public @Positive_Values int hasNullKey() {return hasNullKey;}
		
		public long nullKeyValue() {return nullKeyValue;}
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R other) {
			if (other == null || hasNullKey != other.hasNullKey || hasNullKey == Positive_Values.VALUE && nullKeyValue != other.nullKeyValue
			    || has0Key != other.has0Key || has0Key == Positive_Values.VALUE && OKeyValue != other.OKeyValue || size() != other.size()) return false;
			
			
			char           key;
			for (int i = keys.length; -1 < --i; )
				if ((key = (char) keys[i]) != 0)
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
		
		
		public String toString() {return toString(null).toString();}
		
		public StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			switch (hasNullKey)
			{
				case Positive_Values.VALUE:
					dst.append("Ø -> ").append(nullKeyValue).append('\n');
					break;
				case Positive_Values.NULL:
					dst.append("Ø -> Ø\n");
			}
			
			switch (has0Key)
			{
				case Positive_Values.VALUE:
					dst.append("0 -> ").append(OKeyValue).append('\n');
					break;
				case Positive_Values.NULL:
					dst.append("0 -> Ø\n");
			}
			
			final int[] indexes = new int[assigned];
			for (int i = 0, k = 0; i < keys.length; i++) if (keys[i] != 0) indexes[k++] = i;
			
			Array.ISort sorter = new Array.ISort() {
				
				int more = 1, less = -1;
				@Override public void asc() {less = -(more = 1);}
				@Override public void desc() {more = -(less = 1);}
				
				@Override public int compare(int ia, int ib) {
					final char x = keys[indexes[ia]], y = keys[indexes[ib]];
					return x < y ? less : x == y ? 0 : more;
				}
				@Override public void swap(int ia, int ib) {
					final int t = indexes[ia];
					indexes[ia] = indexes[ib];
					indexes[ib] = t;
				}
				@Override public void set(int idst, int isrc) {indexes[idst] = indexes[isrc];}
				@Override public int compare(int isrc) {
					final char x = fix, y = keys[indexes[isrc]];
					return x < y ? less : x == y ? 0 : more;
				}
				
				char fix = 0;
				int fixi = 0;
				@Override public void get(int isrc) {fix = keys[fixi = indexes[isrc]];}
				@Override public void set(int idst) {indexes[idst] = fixi;}
			};
			
			Array.ISort.sort(sorter, 0, assigned - 1);
			
			for (int i = 0, j; i < assigned; i++)
			{
				dst.append(keys[j = indexes[i]]).append(" -> ");
				if (values.hasValue(j)) dst.append(values.get(j));
				else dst.append('Ø');
				
				dst.append('\n');
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
			
			keys = new char[size];
			values.length(size);
		}
		
		
		public boolean put( Character key, long value) {
			if (key != null) return put((char) (key + 0), value);
			
			int h = hasNullKey;
			hasNullKey   = Positive_Values.VALUE;
			nullKeyValue = value;
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
			nullKeyValue = value;
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
			
			final char key_ =  key ;
			
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
			
			final char key_ =  key ;
			
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
					if (values.nulls.get(i)) tmp.put((char) key, values.get(i));
					else tmp.put((char) key, null);
			
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
