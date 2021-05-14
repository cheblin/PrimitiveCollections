package org.unirail.collections;


import java.util.Arrays;

public interface ByteObjectMap {
	interface Consumer<V extends Comparable<? super V>> {
		boolean put(byte key, V value);
		
		boolean put( Byte      key, V value);
		
		void consume(int items);
	}
	
	
	interface Producer<V extends Comparable<? super V>> {
		
		int size();
		
		boolean produce_has_null_key();
		
		V produce_null_key_val();
		
		int produce(int state);
		
		byte produce_key(int state);
		
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
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(produce_key(p = produce(p)))
						.append(" -> ")
						.append(produce_val(p))
						.append('\n');
			
			return dst;
		}
	}
	
	class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>>, Producer<V> {
		
		ByteSet.RW       keys = new ByteSet.RW();
		ObjectList.RW<V> values;
		
		protected R(int length)                   { values = new ObjectList.RW<>(265 < length ? 256 : length); }
		
		@Override public int size()               { return keys.size(); }
		
		public boolean isEmpty()                  { return keys.isEmpty();}
		
		
		public boolean contains( Byte      key) { return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          { return keys.contains((byte) key);}
		
		public V value( Byte      key)          { return key == null ? NullKeyValue : value(key + 0);}
		
		public V value(int key)                   {return values.array[keys.rank((byte) key)];}
		
		V NullKeyValue = null;
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R<V> other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R<V> other) {
			if (other == null) return -1;
			int diff;
			if ((diff = keys.compareTo(other.keys)) != 0 || (diff = values.compareTo(other.values)) != 0) return diff;
			if (keys.hasNullKey && NullKeyValue != other.NullKeyValue) return 1;
			
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
				
			} catch (CloneNotSupportedException e) { e.printStackTrace(); }
			return null;
		}
		
		//region  producer
		
		@Override public int produce(int state) {
			int i = (state & ~0xFF) + (1 << 8);
			return i | keys.produce(state);
		}
		
		@Override public boolean produce_has_null_key() { return keys.hasNullKey; }
		
		@Override public V produce_null_key_val()       { return NullKeyValue; }
		
		@Override public byte produce_key(int state) {return (byte) (state & 0xFF);}
		
		@Override public V produce_val(int state)       { return values.array[state >> 8]; }
		
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	class RW<V extends Comparable<? super V>> extends R<V> implements Consumer<V> {
		
		
		public RW(int length) { super(length); }
		
		
		public void clear() {
			if (keys.size() < 1) return;
			NullKeyValue = null;
			values.clear();
			keys.clear();
		}
		
		@Override public void consume(int items) {
			NullKeyValue = null;
			keys.consume(items);
			values.consume(items);
		}
		
		public boolean put( Byte      key, V value) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains(null);
				keys.add(null);
				return !ret;
			}
			
			return put((byte) (key + 0), value);
		}
		
		public boolean put(byte key, V value) {
			boolean ret = keys.add((byte) key);
			values.array[keys.rank((byte) key) - 1] = value;
			return ret;
		}
		
		public boolean remove( Byte       key) { return key == null ? keys.remove(null) : remove((byte) (key + 0)); }
		
		public boolean remove(byte key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			values.remove(keys.rank(k) - 1);
			keys.remove(k);
			
			return true;
		}
		
		
		public RW<V> clone() { return (RW<V>) super.clone(); }
	}
}
