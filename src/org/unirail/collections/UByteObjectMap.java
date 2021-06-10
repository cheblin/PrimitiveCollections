package org.unirail.collections;


public interface UByteObjectMap {
	interface IDst<V extends Comparable<? super V>> {
		boolean put(char key, V value);
		
		boolean put( Character key, V value);
		
		void write(int size);
	}
	
	
	interface ISrc<V extends Comparable<? super V>> {
		
		int size();
		
		boolean read_has_null_key();
		
		V read_null_key_val();
		
		int read(int info);
		
		char read_key(int info);
		
		V read_val(int info);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (read_has_null_key())
			{
				dst.append("null -> ").append(read_null_key_val()).append('\n');
				size--;
			}
			
			for (int p = -1, i = 0; i < size; i++)
				dst.append(read_key(p = read(p)))
						.append(" -> ")
						.append(read_val(p))
						.append('\n');
			
			return dst;
		}
	}
	
	abstract class R<V extends Comparable<? super V>> implements Cloneable, Comparable<R<V>>, ISrc<V> {
		
		ByteSet.RW       keys = new ByteSet.RW();
		ObjectList.RW<V> values;
		
		@Override public int size()               { return keys.size(); }
		
		public boolean isEmpty()                  { return keys.isEmpty();}
		
		
		public boolean contains( Character key) { return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key)          { return keys.contains((byte) key);}
		
		public V value( Character key)          { return key == null ? NullKeyValue : value(key + 0);}
		
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
		
		//region  ISrc
		
		@Override public int read(int info) {
			int i = (info & ~0xFF) + (1 << 8);
			return i | keys.read(info);
		}
		
		@Override public boolean read_has_null_key() { return keys.hasNullKey; }
		
		@Override public V read_null_key_val()       { return NullKeyValue; }
		
		@Override public char read_key(int info) {return (char) (info & 0xFF);}
		
		@Override public V read_val(int info)        { return values.array[info >> 8]; }
		
		//endregion
		
		public String toString() { return toString(null).toString();}
	}
	
	class RW<V extends Comparable<? super V>> extends R<V> implements IDst<V> {
		
		
		public RW(int length)  { values = new ObjectList.RW<>(265 < length ? 256 : length); }
		
		
		public void clear() {
			NullKeyValue = null;
			values.clear();
			keys.clear();
		}
		
		//region  IDst
		@Override public void write(int size) {
			NullKeyValue = null;
			keys.clear();
			if (values.length() < size) values.length(-size);
			else values.clear();
		}
		//endregion
		
		public boolean put( Character key, V value) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains(null);
				keys.add(null);
				return !ret;
			}
			
			return put((char) (key + 0), value);
		}
		
		public boolean put(char key, V value) {
			boolean ret = keys.add((byte) key);
			values.array[keys.rank((byte) key) - 1] = value;
			return ret;
		}
		
		public boolean remove( Character  key) { return key == null ? keys.remove(null) : remove((char) (key + 0)); }
		
		public boolean remove(char key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			values.remove(keys.rank(k) - 1);
			keys.remove(k);
			
			return true;
		}
		
		
		public RW<V> clone() { return (RW<V>) super.clone(); }
	}
}
