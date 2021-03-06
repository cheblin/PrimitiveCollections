package org.unirail.collections;


import java.util.Arrays;

public interface ObjectFloatNullMap {
	interface IDst<K extends Comparable<? super K>> {
		boolean put(K key, float value);
		
		boolean put(K key,  Float     value);
		
		void write(int size);
	}
	
	
	interface ISrc<K extends Comparable<? super K>> {
		
		int size();
		
		@Positive_Values int read_has_null_key();
		
		float read_null_key_val();
		
		@Positive_YES int read_has_val(int info);
		
		K read_key(int info);
		
		float  read_val(int info);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			switch (read_has_null_key())
			{
				case Positive_Values.NULL:
					dst.append("null -> null\n");
					size--;
					break;
				case Positive_Values.VALUE:
					dst.append("null -> ").append(read_null_key_val()).append('\n');
					size--;
			}
			
			for (int p = -1, i = 0; i < size; dst.append('\n'), i++)
			{
				dst.append(read_key(p = read_has_val(p))).append(" -> ");
				
				if (p < 0) dst.append("null");
				else dst.append(read_val(p));
			}
			
			return dst;
		}
	}
	
	
	abstract class R<K extends Comparable<? super K>> implements Cloneable, Comparable<R<K>>, ISrc<K> {
		
		
		ObjectList.RW<K>       keys   = new ObjectList.RW<>(0);
		FloatNullList.RW values = new FloatNullList.RW(0);
		
		protected int    assigned;
		protected int    mask;
		protected int    resizeAt;
		protected double loadFactor;
		
		
		public int size()        { return assigned + (hasNullKey == Positive_Values.NONE ? 0 : 1); }
		
		public boolean isEmpty() { return size() == 0; }
		
		public int hashCode() {
			int h = hasNullKey == Positive_Values.VALUE ? 0xDEADBEEF : 0;
			K   k;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((k = keys.array[i]) != null)
					h += Array.hash(k) + Array.hash(values.hashCode());
			return h;
		}
		
		
		public @Positive_Values int info(K key) {
			
			if (key == null) return hasNullKey;
			
			int slot = Array.hash(key) & mask;
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0) return (values.hasValue(slot)) ? slot : Positive_Values.NULL;
			
			return Positive_Values.NONE;
		}
		
		public boolean hasValue(int info) {return -1 < info;}
		
		public boolean hasNone(int info)  {return info == Positive_Values.NONE;}
		
		public boolean hasNull(int info)  {return info == Positive_Values.NULL;}
		
		public float value(@Positive_ONLY int contains) { return contains == Positive_Values.VALUE ? NullKeyValue : values.get(contains); }
		
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			return obj != null && getClass() == obj.getClass() && compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R<K> other) { return other != null && compareTo(other) == 0; }
		
		@Override public int compareTo(R<K> other) {
			if (other == null) return -1;
			
			if (hasNullKey != other.hasNullKey ||
			    hasNullKey == Positive_Values.VALUE && NullKeyValue != other.NullKeyValue) return 1;
			
			int diff = size() - other.size();
			if (diff != 0) return diff;
			
			
			K key;
			for (int i = keys.array.length - 1; 0 <= i; i--)
				if ((key = keys.array[i]) != null)
					if (values.nulls.get(i))
					{
						int tag = other.info(key);
						if (tag == -1 || values.get(i) != other.value(tag)) return 1;
					}
					else if (-1 < other.info(key)) return 1;
			
			return 0;
		}
		
		@Override @SuppressWarnings("unchecked")
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
		
		protected @Positive_Values int         hasNullKey;
		protected                  float NullKeyValue = 0;
		
		
		//region  ISrc
		
		@Override public int read_has_null_key() { return hasNullKey; }
		
		@Override public float read_null_key_val() { return NullKeyValue; }
		
		
		@Override public @Positive_YES int read_has_val(int info) {
			for (info++, info &= Integer.MAX_VALUE; keys.array[info] == null; info++) ;
			return values.hasValue(info) ? info : info | Integer.MIN_VALUE;
		}
		
		@Override public K read_key(int info) {return keys.array[info & Integer.MAX_VALUE]; }
		
		@Override public float read_val(@Positive_ONLY int info) {return values.get(info); }
		
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	class RW<K extends Comparable<? super K>> extends R<K> implements IDst<K> {
		
		public RW(double loadFactor) {this.loadFactor = Math.min(Math.max(loadFactor, 1 / 100.0D), 99 / 100.0D);}
		
		public RW(int expectedItems) {this(expectedItems, 0.75);}
		
		public RW(int expectedItems, double loadFactor) {
			this(loadFactor);
			
			long length = (long) Math.ceil(expectedItems / loadFactor);
			int  size   = (int) (length == expectedItems ? length + 1 : Math.max(4, Array.nextPowerOf2(length)));
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			keys.length(size);
			values.nulls.length(size);
			values.values.length(size);
		}
		
		@Override public RW<K> clone()                  { return (RW<K>) super.clone(); }
		
		
		
		@Override public boolean put(K key,  Float     value) {
			if (value != null) put(key, (float) value);
			
			if (key == null)
			{
				hasNullKey = Positive_Values.NULL;
				return true;
			}
			
			int slot = Array.hash(key) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0)
				{
					values.set(slot, ( Float    ) null);
					return true;
				}
			
			keys.array[slot] = key;
			values.set(slot, ( Float    ) null);
			
			if (++assigned == resizeAt) this.allocate(mask + 1 << 1);
			
			return true;
		}
		
		@Override public boolean put(K key, float value) {
			
			if (key == null)
			{
				int h = hasNullKey;
				hasNullKey = Positive_Values.VALUE;
				NullKeyValue = value;
				return h != Positive_Values.VALUE;
			}
			
			int slot = Array.hash(key) & mask;
			
			
			for (K k; (k = keys.array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0)
				{
					values.set(slot, value);
					return true;
				}
			
			keys.array[slot] = key;
			values.set(slot, value);
			
			if (++assigned == resizeAt) allocate(mask + 1 << 1);
			
			return true;
		}
		
		public void clear() {
			assigned = 0;
			hasNullKey = Positive_Values.NONE;
			keys.clear();
			values.clear();
		}
		
		//region  IDst
		@Override public void write(int size) {
			assigned = 0;
			hasNullKey = Positive_Values.NONE;
			keys.write(size = (int) Array.nextPowerOf2(size));
			if (values.length() < size) values.length(-size);
			else values.clear();
		}
		//endregion
	
		
		protected void allocate(int size) {
			
			resizeAt = Math.min(mask = size - 1, (int) Math.ceil(size * loadFactor));
			
			if (assigned < 1)
			{
				if (keys.length() < size) keys.length(-size);
				
				if (values.nulls.length() < size) values.nulls.length(-size);
				
				if (values.values.length() < size) values.values.length(-size);
				
				return;
			}
			
			final K[]              k    = keys.array;
			FloatNullList.RW vals = values;
			
			keys.length(-size);
			values = new FloatNullList.RW(-size);
			
			K kk;
			for (int i = k.length - 1; 0 <= --i; )
				if ((kk = k[i]) != null)
				{
					int slot = Array.hash(kk.hashCode()) & mask;
					while (!(keys.array[slot] == null)) slot = slot + 1 & mask;
					
					keys.array[slot] = kk;
					
					if (vals.hasValue(i)) values.set(slot, vals.get(i));
					else values.set(slot, ( Float    ) null);
				}
			
		}
		
		
		public boolean remove(K key) {
			if (key == null)
			{
				int h = hasNullKey;
				hasNullKey = Positive_Values.NONE;
				return h != Positive_Values.NONE;
			}
			
			int slot = Array.hash(key) & mask;
			
			final K[] array = keys.array;
			for (K k; (k = array[slot]) != null; slot = slot + 1 & mask)
				if (k.compareTo(key) == 0)
				{
					
					int gapSlot = slot;
					
					K kk;
					for (int distance = 0, s; (kk = array[s = gapSlot + ++distance & mask]) != null; )
						if ((s - Array.hash(kk.hashCode()) & mask) >= distance)
						{
							array[gapSlot] = kk;
							
							if (values.nulls.get(s))
								values.set(gapSlot, values.get(s));
							else
								values.set(gapSlot, ( Float    ) null);
							
							gapSlot = s;
							distance = 0;
						}
					
					array[gapSlot] = null;
					values.set(gapSlot, ( Float    ) null);
					assigned--;
					return true;
				}
			
			return false;
		}
		
		
	}
	
}
	
	