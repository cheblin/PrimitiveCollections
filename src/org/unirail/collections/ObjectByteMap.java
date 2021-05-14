package org.unirail.collections;


public interface ObjectByteMap {
	
	interface Consumer<K extends Comparable<? super K>> {
		boolean put(K key, byte value);
		
		void consume(int items);
	}
	
	
	interface Producer<K extends Comparable<? super K>> {
		
		int size();
		
		boolean produce_has_null_key();
		
		byte produce_null_key_val();
		
		int produce(int state);
		
		K produce_key(int state);
		
		byte  produce_val(int state);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (produce_has_null_key())
			{
				dst.append("null -> ").append(produce_null_key_val()).append('\n');
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
	
	class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>>, Producer<K> {
		
		public ObjectList.RW<K> keys = new ObjectList.RW<>(0);
		
		public ByteList.RW values = new ByteList.RW(0);
		
		protected int assigned;
		
		protected int mask;
		
		protected int resizeAt;
		
		protected boolean hasNullKey;
		
		byte NullKeyValue = 0;
		
		protected double loadFactor;
		
		
		protected R(double loadFactor) { this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D); }
		
		protected R(int expectedItems) { this(expectedItems, 0.75); }
		
		
		protected R(int expectedItems, double loadFactor) {
			this(loadFactor);
			
			long length = (long) Math.ceil(expectedItems / loadFactor);
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
			values.length(size);
		}
		
		
		public @Positive_Values int info(K key) {
			if (key == null) return hasNullKey ? Positive_Values.VALUE : Positive_Values.NONE;
			
			int slot = Array.hash(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0) return slot;
			
			return Positive_Values.NONE;
		}
		
		public boolean contains(K key)    {return -1 < info(key);}
		
		public boolean contains(int info) {return -1 < info;}
		
		public byte value(@Positive_ONLY int contains) {return contains == Positive_Values.VALUE ? NullKeyValue :  (byte) values.array[contains]; }
		
		
		public int size()                 { return assigned + (hasNullKey ? 1 : 0); }
		
		public boolean isEmpty()          { return size() == 0; }
		
		
		public int hashCode() {
			int h = hasNullKey ? 0xDEADBEEF : 0;
			K   k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != null)
					h += Array.hash(k) + Array.hash(values.array[i]);
			return h;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R<K> other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R<K> other) {
			if (other == null) return -1;
			
			if (hasNullKey != other.hasNullKey ||
			    hasNullKey && NullKeyValue != other.NullKeyValue) return 1;
			
			int diff;
			if ((diff = size() - other.size()) != 0) return diff;
			
			
			K key;
			for (int i = keys.array.length - 1, info; -1 < i; i--)
				if ((key = keys.array[i]) != null)
					if ((info = other.info(key)) == Positive_Values.NONE || values.array[i] != other.value(info)) return 1;
			
			return 0;
		}
		
		@SuppressWarnings("unchecked")
		public R<K> clone() {
			try
			{
				R<K> dst = (R<K>) super.clone();
				
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
		
		@Override public boolean produce_has_null_key() { return hasNullKey; }
		
		@Override public byte produce_null_key_val()     { return NullKeyValue; }
		
		@Override public int produce(int state)         { for (; ; ) if (keys.array[++state] != null) return state; }
		
		@Override public K produce_key(int state)       {return keys.array[state]; }
		
		@Override public byte produce_val(int state) {return values.get(state); }
		
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> implements Consumer<K> {
		
		
		public RW(double loadFactor) {
			super(loadFactor);
		}
		
		public RW(int expectedItems) {
			super(expectedItems);
		}
		
		public RW(int expectedItems, double loadFactor) {
			super(expectedItems, loadFactor);
		}
		
		public void clear() {
			assigned = 0;
			hasNullKey = false;
			keys.clear();
			values.clear();
		}
		
		public boolean put(K key, byte value) {
			
			if (key == null)
			{
				NullKeyValue = value;
				return !hasNullKey && (hasNullKey = true);
			}
			
			int slot = Array.hash(key) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0)
				{
					values.array[slot] = (byte) value;
					return false;
				}
			
			keys.array[slot] = key;
			values.array[slot] = (byte) value;
			
			if (assigned++ == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		@Override public void consume(int items) {
			assigned = 0;
			hasNullKey = false;
			
			allocate((int) Array.nextPowerOf2(items));
		}
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				else keys.clear();
				
				if (values.length() < size) values.length(-size);
				else values.clear();
				return;
			}
			
			final K[]           k = keys.array;
			final byte[] v = values.array;
			
			keys.length(-size);
			values.length(-size);
			
			K kk;
			for (int i = k.length - 1; 0 <= --i; )
				if ((kk = k[i]) != null)
				{
					int slot = Array.hash(kk.hashCode()) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot] = kk;
					values.array[slot] = v[i];
				}
		}
		
		public boolean remove(K key) {
			if (key == null) return hasNullKey && !(hasNullKey = false);
			
			int slot = Array.hash(key) & mask;
			
			final K[] array = keys.array;
			
			for (K k; (k = array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0)
				{
					byte[] vals = values.array;
					
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != null; )
						if ((s - Array.hash(kk.hashCode()) & mask) >= distance)
						{
							vals[gapSlot] = vals[s];
							array[gapSlot] = kk;
							gapSlot = s;
							distance = 0;
						}
					
					array[gapSlot] = null;
					vals[gapSlot] = (byte) 0;
					assigned--;
					return true;
				}
			
			return false;
		}
		
		public RW<K> clone() { return (RW<K>) super.clone(); }
	}
}
	