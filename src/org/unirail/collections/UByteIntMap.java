package org.unirail.collections;


public interface UByteIntMap {
	
	interface IDst {
		boolean put(char key, int value);
		
		boolean put( Character key, int value);
		
		void write(int size);
	}
	
	interface ISrc {
		int size();
		
		boolean read_has_null_key();
		
		int read_null_key_val();
		
		int read( int info);
		
		char read_key( int info);
		
		int read_val( int info);
		
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
	
	
	 abstract class R implements Cloneable, Comparable<R>, ISrc {
		
		ByteSet.RW         keys = new ByteSet.RW();
		IntList.RW values;
		
		
		
		
		@Override public int size()               { return keys.size(); }
		
		public boolean isEmpty()                  { return keys.isEmpty();}
		
		public boolean contains( Character key) { return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key) { return keys.contains((byte) key);}
		
		public int value( Character key) { return key == null ? NullKeyValue : value(key + 0);}
		
		public int  value(int key) {return    values.array[keys.rank((byte) key)];}
		
		int NullKeyValue = 0;
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		@Override public int compareTo(R other) {
			if (other == null) return -1;
			
			int diff;
			if ((diff = other.keys.compareTo(keys)) != 0 || (diff = other.values.compareTo(values)) != 0) return diff;
			if (keys.hasNullKey && NullKeyValue != other.NullKeyValue) return 1;
			
			return 0;
		}
		
		@Override public R clone() {
			
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
		
		
		//region  ISrc
		
		@Override public int read(int info) {
			int i = (info & ~0xFF) + (1 << 8);
			return i | keys.read(info);
		}
		
		@Override public boolean read_has_null_key() { return keys.hasNullKey; }
		
		@Override public int read_null_key_val() { return NullKeyValue; }
		
		@Override public char read_key(int info) {return (char) (info & 0xFF);}
		
		@Override public int read_val(int info) { return  values.array[info >> 8]; }
		
		//endregion
		public String toString() { return toString(null).toString();}
	}
	
	class RW extends R implements IDst {
		
		public RW(int length)                   { values = new IntList.RW(265 < length ? 256 : length); }
		
		public void clear() {
			if (keys.size() < 1) return;
			
			keys.clear();
			values.clear();
		}
		
		//region  IDst
		@Override public void write(int size) {
			keys.write(size);
			values.write(size);
		}
		
		//endregion
		
		
		@Override public boolean put( Character key, int value) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains(null);
				keys.add(null);
				return !ret;
			}
			
			return put((char) (key + 0), value);
		}
		
		@Override public boolean put(char key, int value) {
			boolean ret = keys.add((byte) key);
			values.array[keys.rank((byte) key) - 1] = (int) value;
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
		
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
