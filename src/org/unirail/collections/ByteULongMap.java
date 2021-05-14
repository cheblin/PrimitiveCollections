package org.unirail.collections;


public interface ByteULongMap {
	
	interface Consumer {
		boolean put(byte key, long value);
		
		boolean put( Byte      key, long value);
		
		void consume(int size);
	}
	
	interface Producer {
		int size();
		
		boolean produce_has_null_key();
		
		long produce_null_key_val();
		
		int produce( int info);
		
		byte produce_key( int info);
		
		long produce_val( int info);
		
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
	
	
	class R implements Cloneable, Comparable<R>, Producer {
		
		ByteSet.RW         keys = new ByteSet.RW();
		ULongList.RW values;
		
		
		protected R(int length)                   { values = new ULongList.RW(265 < length ? 256 : length); }
		
		@Override public int size()               { return keys.size(); }
		
		public boolean isEmpty()                  { return keys.isEmpty();}
		
		public boolean contains( Byte      key) { return key == null ? keys.contains(null) : keys.contains((byte) (key + 0));}
		
		public boolean contains(int key) { return keys.contains((byte) key);}
		
		public long value( Byte      key) { return key == null ? NullKeyValue : value(key + 0);}
		
		public long  value(int key) {return    values.array[keys.rank((byte) key)];}
		
		long NullKeyValue = 0;
		
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
		
		
		//region  producer
		
		@Override public int produce(int info) {
			int i = (info & ~0xFF) + (1 << 8);
			return i | keys.produce(info);
		}
		
		@Override public boolean produce_has_null_key() { return keys.hasNullKey; }
		
		@Override public long produce_null_key_val() { return NullKeyValue; }
		
		@Override public byte produce_key(int info) {return (byte) (info & 0xFF);}
		
		@Override public long produce_val(int info) { return  values.array[info >> 8]; }
		
		//endregion
		public String toString() { return toString(null).toString();}
	}
	
	class RW extends R implements Consumer {
		
		public RW(int length) { super(length); }
		
		
		public void clear() {
			if (keys.size() < 1) return;
			
			keys.clear();
			values.clear();
		}
		
		@Override public void consume(int size) {
			keys.consume(size);
			values.consume(size);
		}
		
		
		@Override public boolean put( Byte      key, long value) {
			if (key == null)
			{
				NullKeyValue = value;
				boolean ret = keys.contains(null);
				keys.add(null);
				return !ret;
			}
			
			return put((byte) (key + 0), value);
		}
		
		@Override public boolean put(byte key, long value) {
			boolean ret = keys.add((byte) key);
			values.array[keys.rank((byte) key) - 1] = (long) value;
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
		
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
