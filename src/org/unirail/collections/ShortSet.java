package org.unirail.collections;


import static org.unirail.collections.Array.HashEqual.hash;

public interface ShortSet {
	
	interface NonNullKeysIterator {
		int INIT = -1;
		
		static int token(R src, int token) {
			for (int len = src.keys.length; ; )
				if (++token == len) return src.has0Key ? -2 : INIT;
				else if (token == 0x7FFF_FFFF) return INIT;
				else if (src.keys[token] != 0) return token;
		}
		
		static short key(R src, int token) {return token == -2 ? 0 : (short)  src.keys[token];}
	}
	
	abstract class R implements Cloneable {
		
		public short[] keys = new short[0];
		
		int assigned;
		
		int mask;
		
		int resizeAt;
		
		boolean has0Key;
		boolean hasNullKey;
		
		protected double loadFactor;
		
		
		public boolean contains( Short     key) {return key == null ? hasNullKey : contains((short) (key + 0));}
		
		public boolean contains(short key) {
			if (key == 0) return has0Key;
			
			int slot = hash(key) & mask;
			
			for (short k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return true;
			return false;
		}
		
		
		public boolean isEmpty() {return size() == 0;}
		
		
		public int size()        {return assigned + (has0Key ? 1 : 0) + (hasNullKey ? 1 : 0);}
		
		
		public int hashCode() {
			int hash = hasNullKey ? 10910099 : 97654321;
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				hash = hash(hash, NonNullKeysIterator.key(this, token));
			
			return hash(hash, keys);
		}
		
		
		public short[] toArray(short[] dst) {
			final int size = size();
			if (dst == null || dst.length < size) dst = new short[size];
			
			for (int i = keys.length, ii = 0; -1 < --i; )
				if (keys[i] != 0) dst[ii++] = keys[i];
			
			return dst;
		}
		
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public boolean equals(R other) {
			if (other == null || other.assigned != assigned) return false;
			
			for (short k : keys) if (k != 0 && !other.contains(k)) return false;
			
			return true;
		}
		
		
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				
				dst.keys = keys.clone();
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
			
			if (hasNullKey) dst.append("null\n");
			
			for (int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT; )
				dst.append(NonNullKeysIterator.key(this, token)).append('\n');
			return dst;
		}
	}
	
	
	class RW extends R {
		
		public RW(int expectedItems) {this(expectedItems, 0.75);}
		
		public RW(double loadFactor) {this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);}
		
		
		public RW(int expectedItems, double loadFactor) {
			this(loadFactor);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys = new short[size];
		}
		
		public RW(short... items) {
			this(items.length);
			for (short key : items) add(key);
		}
		
		public RW( Short    ... items) {
			this(items.length);
			for ( Short     key : items) add(key);
		}
		
		public boolean add( Short     key) {return key == null ? !hasNullKey && (hasNullKey = true) : add((short) key);}
		
		public boolean add(short  key) {
			
			if (key == 0) return !has0Key && (has0Key = true);
			
			int slot = hash(key) & mask;
			
			for (short k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return false;
			
			keys[slot] =  key;
			
			if (assigned++ == resizeAt) allocate(mask + 1 << 1);
			return true;
		}
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length < size)keys =  new short[size] ;
				return;
			}
			
			final short[] k = keys;
			
			keys =  new short[size] ;
			
			short key;
			for (int i = k.length; -1 < --i; )
				if ((key = k[i]) != 0)
				{
					int slot = hash(key) & mask;
					while (keys[slot] != 0) slot = slot + 1 & mask;
					keys[slot] = key;
				}
		}
		
		
		public boolean remove( Short     key) {return key == null ? hasNullKey && !(hasNullKey = false) : remove((short) key);}
		
		public boolean remove(short key) {
			
			if (key == 0) return has0Key && !(has0Key = false);
			
			int slot = hash(key) & mask;
			
			for (short k; (k = keys[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					int gapSlot = slot;
					
					short kk;
					for (int distance = 0, s; (kk = keys[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - hash(kk) & mask) >= distance)
						{
							keys[gapSlot] =  kk;
							gapSlot = s;
							distance = 0;
						}
					
					keys[gapSlot] = 0;
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
		
		
		public void retainAll(RW chk) {
			short key;
			
			for (int i = keys.length; -1 < --i; )
				if ((key = keys[i]) != 0 && !chk.add(key)) remove(key);
			
			if (has0Key && !chk.add((short) 0)) has0Key = false;
		}
		
		public int removeAll(R src) {
			int fix = size();
			
			for (int i = 0, s = src.size(), token = NonNullKeysIterator.INIT; i < s; i++) remove(NonNullKeysIterator.key(src, token = NonNullKeysIterator.token(src, token)));
			
			return fix - size();
		}
		
		@Override public RW clone() {return (RW) super.clone();}
	}
}