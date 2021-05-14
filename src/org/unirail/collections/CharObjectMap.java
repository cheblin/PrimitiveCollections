package org.unirail.collections;

public interface CharObjectMap {
	
	interface Consumer<V extends Comparable<? super V>> {
		boolean put(char key, V value);//return false to interrupt
		
		boolean put( Character key, V value);
		
		int consume(int items);
	}
	
	
	interface Producer<V extends Comparable<? super V>> {
		
		int size();
		
		boolean produce_has_null_key();
		
		V produce_null_key_val();
		
		boolean produce_has_0key();
		
		V produce_0key_val();
		
		int produce(int state);
		
		char produce_key(int state);
		
		V produce_val(int state);
		
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			
			if (produce_has_null_key())
			{
				dst.append("null -> ").append(produce_null_key_val()).append('\n');
				size--;
			}
			
			if (produce_has_0key())
			{
				dst.append("0 -> ").append(produce_0key_val()).append('\n');
				size--;
			}
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(produce_key(p = produce(p)))
						.append(" -> ")
						.append(produce_val(p))
						.append('\n');
			
			return dst;
		}
	}
	
	
	class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>>, Producer<V> {
		
		
		public CharList.RW keys = new CharList.RW(0);
		
		public ObjectList.RW<V> values = new ObjectList.RW<>(0);
		
		
		int assigned;
		
		
		int mask;
		
		
		int resizeAt;
		
		boolean hasNullKey;
		V       nullKeyValue;
		
		
		boolean hasO;
		V       OkeyValue;
		
		
		protected double loadFactor;
		
		
		public R(double loadFactor) { this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D); }
		
		public R(int expectedItems) { this(expectedItems, 0.75); }
		
		
		public R(int expectedItems, double loadFactor) {
			this(loadFactor);
			
			final long length = (long) Math.ceil(expectedItems / loadFactor);
			int        size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
			values.length(size);
		}
		
		
		public @Positive_Values int info( Character key) { return key == null ? hasNullKey ? Integer.MAX_VALUE : Positive_Values.NONE : info((char)(key + 0));}
		
		public @Positive_Values int info(char key) {
			if (key == 0) return hasO ? Positive_Values.VALUE - 1 : Positive_Values.NONE;
			
			int slot = Array.hash(key) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key) return slot;
			
			return Positive_Values.NONE;
		}
		
		public V value(@Positive_ONLY int info) {
			if (info == Positive_Values.VALUE) return nullKeyValue;
			if (info == Positive_Values.VALUE - 1) return OkeyValue;
			
			return values.array[info];
		}
		
		
		public boolean isEmpty() { return size() == 0; }
		
		public int size()        { return assigned + (hasO ? 1 : 0) + (hasNullKey ? 1 : 0); }
		
		
		public int hashCode() {
			int         h = hasO ? 0xDEADBEEF : 0;
			char k;
			
			for (int i = mask; 0 <= i; i--)
				if ((k = keys.array[i]) != 0)
					h += Array.hash(k) + Array.hash(values.array[i]);
			
			return h;
		}
		
		
		@SuppressWarnings("unchecked") public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R<V> other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R<V> other) {
			
			if (other == null) return -1;
			if (assigned != other.assigned) return assigned - other.assigned;
			if (hasO != other.hasO || hasNullKey != other.hasNullKey) return 1;
			
			int diff;
			if (hasO && (diff = OkeyValue.compareTo(other.OkeyValue)) != 0) return diff;
			if (hasNullKey && (diff = nullKeyValue.compareTo(other.nullKeyValue)) != 0) return diff;
			
			
			V           v;
			char k;
			
			for (int i = keys.array.length - 1, c; -1 <= --i; )
				if ((k = keys.array[i]) != 0)
				{
					if ((c = other.info(k)) < 0) return 3;
					v = other.value(c);
					
					if (values.array[i] != null && v != null && (diff = v.compareTo(values.array[i])) != 0) return diff;
					else if (values.array[i] != v) return 8;
				}
			return 0;
		}
		
		
		@SuppressWarnings("unchecked")
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
				dst.keys = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		//region  producer
		
		@Override public int produce(int state)         { for (; ; ) if (keys.array[++state] != 0) return state; }
		
		@Override public boolean produce_has_null_key() {return hasNullKey;}
		
		@Override public V produce_null_key_val()       {return nullKeyValue;}
		
		@Override public boolean produce_has_0key()     {return hasO; }
		
		@Override public V produce_0key_val()           {return OkeyValue;}
		
		@Override public char produce_key(int state) {return  (char) keys.array[state]; }
		
		@Override public V produce_val(int state)       {return  values.array[state]; }
		
		//endregion
		
		
		public String toString() { return toString(null).toString();}
	}
	
	class RW<V extends Comparable<? super V>> extends R<V> implements Consumer<V> {
		
		
		public RW(double loadFactor) {
			super(loadFactor);
		}
		
		public RW(int expectedItems) {
			super(expectedItems);
		}
		
		public RW(int expectedItems, double loadFactor) {
			super(expectedItems, loadFactor);
		}
		
		public Consumer<V> consumer() {return this; }
		
		
		public boolean put( Character key, V value) {
			if (key != null) return put((char) key, value);
			
			hasNullKey = true;
			nullKeyValue = value;
			
			return true;
		}
		
		public boolean put(char key, V value) {
			if (key == 0)
			{
				hasO = true;
				OkeyValue = value;
				return true;
			}
			
			
			int slot = Array.hash(key) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					values.array[slot] = value;
					return true;
				}
			
			keys.array[slot] =  key;
			values.array[slot] = value;
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		
		public boolean remove( Character key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			return remove((char) key);
		}
		
		public boolean remove(char key) {
			
			if (key == 0) return hasO && !(hasO = false);
			
			int slot = Array.hash(key) & mask;
			
			for (char k; (k = keys.array[slot]) != 0; slot = slot + 1 & mask)
				if (k == key)
				{
					final V v       = values.array[slot];
					int     gapSlot = slot;
					
					char kk;
					for (int distance = 0, s; (kk = keys.array[s = gapSlot + ++distance & mask]) != 0; )
						if ((s - Array.hash(kk) & mask) >= distance)
						{
							
							keys.array[gapSlot] =  kk;
							values.array[gapSlot] = values.array[s];
							gapSlot = s;
							distance = 0;
						}
					
					keys.array[gapSlot] = 0;
					values.array[gapSlot] = null;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public void clear() {
			assigned = 0;
			
			hasO = false;
			OkeyValue = null;
			
			hasNullKey = false;
			nullKeyValue = null;
			
			keys.clear();
			values.clear();
		}
		
		@Override public int consume(int items) {
			
			assigned = 0;
			hasNullKey = false;
			hasO = false;
			OkeyValue = null;
			nullKeyValue = null;
			
			allocate((int) Array.nextPowerOf2(items));
			return items;
		}
		
		void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				else keys.clear();
				
				if (values.length() < size) values.length(-size);
				else values.clear();
				
				return;
			}
			
			
			final char[] k = keys.array;
			final V[]           v = values.array;
			
			keys.length(-size);
			values.length(-size);
			
			char key;
			for (int from = k.length - 1; 0 <= --from; )
				if ((key = k[from]) != 0)
				{
					int slot = Array.hash(key) & mask;
					while (keys.array[slot] != 0) slot = slot + 1 & mask;
					keys.array[slot] =  key;
					values.array[slot] = v[from];
				}
		}
		
		
		public RW<V> clone() { return (RW<V>) super.clone(); }
	}
}
